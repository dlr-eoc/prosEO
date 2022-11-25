/**
 * JobUtil.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ProseoMessage;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;

/**
 * Handle jobs
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class JobUtil {
	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(JobUtil.class);

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	@Autowired
	private ProductionPlanner productionPlanner;
	
	/**
	 * Suspend the job and its job steps. If force is true, running Kubernetes jobs of are killed.
	 *  
	 * @param job The job
	 * @param force 
	 * @return Result message
	 */
	@Transactional
	public ProseoMessage suspend(Job job, Boolean force) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspend({}, {})", (null == job ? "null" : job.getId()), force);

		ProseoMessage answer = GeneralMessage.FALSE;
		// check current state for possibility to be suspended
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
				answer = PlannerMessage.JOB_INITIAL;
				break;
			case PLANNED:
			case RELEASED:
			case ON_HOLD:
			case STARTED:
				// try to suspend job steps not running
				Boolean allSuspended = true;
				for (JobStep js : job.getJobSteps()) {
					allSuspended = UtilService.getJobStepUtil().suspend(js, force).getSuccess() && allSuspended;
				}
				job.incrementVersion();
				if (allSuspended) {
					if (job.getJobState() == JobState.STARTED) {
						job.setJobState(de.dlr.proseo.model.Job.JobState.ON_HOLD);
						job.setJobState(de.dlr.proseo.model.Job.JobState.PLANNED);
					} else if (job.getJobState() == JobState.RELEASED) {
						job.setJobState(de.dlr.proseo.model.Job.JobState.PLANNED);
					} else if (job.getJobState() == JobState.ON_HOLD) {
						job.setJobState(de.dlr.proseo.model.Job.JobState.PLANNED);
					}
					answer = PlannerMessage.JOB_SUSPENDED;
				} else {
					job.setJobState(de.dlr.proseo.model.Job.JobState.ON_HOLD);
					answer = PlannerMessage.JOB_HOLD;
				}
				RepositoryService.getJobRepository().save(job);
				em.merge(job);
				break;
			case COMPLETED:
				answer = PlannerMessage.JOB_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = PlannerMessage.JOB_ALREADY_FAILED;
			case CLOSED:
				answer = PlannerMessage.JOB_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			logger.log(answer, job.getId());
		}
		return answer;
	}


	/**
	 * Delete satisfied product queries of its job steps.
	 * 
	 * @param job The job
	 * @return Result message
	 */
	public ProseoMessage close(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> close({})", (null == id ? "null" : id));

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		List<Long> jobStepIds = new ArrayList<Long>();

		final JobState jobState = transactionTemplate.execute((status) -> {
			String sqlQuery = "select job_state from job where id = " + id + ";";
			Query query = em.createNativeQuery(sqlQuery);
			Object o = query.getSingleResult();
			return JobState.valueOf((String)o);			
		});
		final Object dummy = transactionTemplate.execute((status) -> {
			String sqlQuery = "select id from job_step where job_id = " + id + ";";
			Query query = em.createNativeQuery(sqlQuery);
			List<?> ol = query.getResultList();
			for (Object o : ol) {
				if (o instanceof BigInteger) {
					jobStepIds.add(((BigInteger)o).longValue());
				}
			}
			return null;
		});
		ProseoMessage answer = GeneralMessage.FALSE;
		// check current state for possibility to be suspended
		if (id != null) {
			switch (jobState) {
			case INITIAL:
			case PLANNED:
			case RELEASED:
			case STARTED:
			case ON_HOLD:
				answer = PlannerMessage.JOB_COULD_NOT_CLOSE;
				break;
			case COMPLETED:
			case FAILED:
				for (Long jsId : jobStepIds) {
					UtilService.getJobStepUtil().close(jsId);
				}		
				final Object dummy2 = transactionTemplate.execute((status) -> {
					String sqlQuery = "select version from job where id = " + id + ";";
					Query query = em.createNativeQuery(sqlQuery);
					Object o = query.getSingleResult();
					Integer version = (Integer)o;
					sqlQuery = "update job set job_state = 'CLOSED', version = " + (version + 1) + " where id = " + id + ";";
					query = em.createNativeQuery(sqlQuery);
					query.executeUpdate();
					return null;
				});
				answer = PlannerMessage.JOB_CLOSED;
				break;
			case CLOSED:
				answer = PlannerMessage.JOB_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			logger.log(answer, id);
		}
		return answer;
	}

	/**
	 * Retry a job and its job steps.
	 * 
	 * @param job The job
	 * @return Result message
	 */
	@Transactional
	public ProseoMessage retry(Job job) {
		if (logger.isTraceEnabled()) logger.trace(">>> retry({})", (null == job ? "null" : job.getId()));

		ProseoMessage answer = GeneralMessage.FALSE;
		// check current state for possibility to be suspended
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case RELEASED:
			case STARTED:
				answer = PlannerMessage.JOB_COULD_NOT_RETRY;
				break;
			case COMPLETED:
				answer = PlannerMessage.JOB_ALREADY_COMPLETED;
				break;
			case PLANNED:
				answer = PlannerMessage.JOB_RETRIED;
				break;
			case ON_HOLD:
			case FAILED:
				job.setHasFailedJobSteps(false);
				for (JobStep js : job.getJobSteps()) {
					UtilService.getJobStepUtil().retry(js);
					if (js.getJobStepState() == JobStepState.FAILED) {
						job.setHasFailedJobSteps(true);
					}
				}
				Boolean all = true;
				Boolean allCompleted = true;
				for (JobStep js : job.getJobSteps()) {
					if (!(   js.getJobStepState() == de.dlr.proseo.model.JobStep.JobStepState.PLANNED
						  || js.getJobStepState() == de.dlr.proseo.model.JobStep.JobStepState.COMPLETED)) {
						all = false;
						
					}
					if (js.getJobStepState() != de.dlr.proseo.model.JobStep.JobStepState.COMPLETED) {
						allCompleted = false;
					}
				}
				if (all) {
					if (job.getJobState() == JobState.COMPLETED) {
						answer = PlannerMessage.JOB_COMPLETED;
					} else if (allCompleted) {
						job.setJobState(de.dlr.proseo.model.Job.JobState.PLANNED);
						job.setJobState(de.dlr.proseo.model.Job.JobState.RELEASED);
						job.setJobState(de.dlr.proseo.model.Job.JobState.STARTED);
						job.setJobState(de.dlr.proseo.model.Job.JobState.COMPLETED);
						job.incrementVersion();
						RepositoryService.getJobRepository().save(job);
						em.merge(job);
						answer = PlannerMessage.JOB_COMPLETED;
					} else {
						if (job.getJobState() == JobState.STARTED) {
							job.setJobState(de.dlr.proseo.model.Job.JobState.ON_HOLD);
						}
						job.setJobState(de.dlr.proseo.model.Job.JobState.PLANNED);
						job.incrementVersion();
						RepositoryService.getJobRepository().save(job);
						em.merge(job);
						answer = PlannerMessage.JOB_RETRIED;
					}
				} else {
					answer = PlannerMessage.JOB_COULD_NOT_RETRY;
				}
				break;
			case CLOSED:
				answer = PlannerMessage.JOB_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			logger.log(answer, job.getId());
		}
		return answer;
	}

	/**
	 * Cancel a job and its job steps.
	 * 
	 * @param job The job
	 * @return Result message
	 */
	@Transactional
	public ProseoMessage cancel(Job job) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancel({})", (null == job ? "null" : job.getId()));

		ProseoMessage answer = GeneralMessage.FALSE;
		// check current state for possibility to be suspended
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case PLANNED:
				for (JobStep js : job.getJobSteps()) {
					UtilService.getJobStepUtil().cancel(js);
				}
				job.setJobState(de.dlr.proseo.model.Job.JobState.FAILED);
				job.incrementVersion();
				RepositoryService.getJobRepository().save(job);
				answer = PlannerMessage.JOB_CANCELED;
				break;
			case RELEASED:
				answer = PlannerMessage.JOB_ALREADY_RELEASED;
				break;
			case ON_HOLD:
				answer = PlannerMessage.JOB_ALREADY_HOLD;
				break;
			case STARTED:
				answer = PlannerMessage.JOB_ALREADY_STARTED;
				break;
			case COMPLETED:
				answer = PlannerMessage.JOB_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = PlannerMessage.JOB_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = PlannerMessage.JOB_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			logger.log(answer, job.getId());
		}
		return answer;
	}

	/**
	 * Resume a job and its job steps.
	 * 
	 * @param jobId The job id
	 * @return Result message
	 */
	public ProseoMessage resume(Job job) {
		if (logger.isTraceEnabled()) logger.trace(">>> resume({})", job.getId());

		ProseoMessage answer = GeneralMessage.FALSE;
//		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
//
//		final Job job = transactionTemplate.execute((status) -> {
//			Optional<Job> opt = RepositoryService.getJobRepository().findById(jobId);
//			if (opt.isPresent()) {
//				if (opt.get().getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
//					PlannerMessage.FACILITY_NOT_AVAILABLE.log(logger, opt.get().getProcessingFacility().getName(),
//							opt.get().getProcessingFacility().getFacilityState().toString());
//
//			    	return null;
//				} else {
//					return opt.get();
//				}
//			}
//			return null;
//		});

		// check current state for possibility to be suspended
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
				answer = PlannerMessage.JOB_HASTOBE_PLANNED;
				break;
			case PLANNED:
				try {
					job.setJobState(de.dlr.proseo.model.Job.JobState.RELEASED);
					job.incrementVersion();
					RepositoryService.getJobRepository().save(job);


					for (JobStep js : job.getJobSteps()) {
						UtilService.getJobStepUtil().resume(js, false);
					}
					answer = PlannerMessage.JOB_RELEASED;
				} catch (Exception e) {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
				}
				break;
			case RELEASED:
				answer = PlannerMessage.JOB_RELEASED;
				break;
			case ON_HOLD:
				answer = PlannerMessage.JOB_ALREADY_HOLD;
				break;
			case STARTED:
				answer = PlannerMessage.JOB_ALREADY_STARTED;
				break;
			case COMPLETED:
				answer = PlannerMessage.JOB_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = PlannerMessage.JOB_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = PlannerMessage.JOB_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			logger.log(answer, job.getId());
		}
		return answer;
	}

	/**
	 * Start a job and its job steps.
	 * 
	 * @param job The job
	 * @return true after success
	 */
	@Transactional
	public Boolean startJob(Job job) {
		if (logger.isTraceEnabled()) logger.trace(">>> startJob({})", (null == job ? "null" : job.getId()));

		Boolean answer = false;
		// check current state for possibility to be suspended
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
				logger.log(PlannerMessage.JOB_INITIAL, String.valueOf(job.getId()));
				break;
			case ON_HOLD:
				logger.log(PlannerMessage.JOB_HOLD, String.valueOf(job.getId()));
				break;
			case PLANNED:
				logger.log(PlannerMessage.JOB_PLANNED, String.valueOf(job.getId()));
				break;
			case RELEASED:
				UtilService.getOrderUtil().startOrder(job.getProcessingOrder());
				job.setJobState(de.dlr.proseo.model.Job.JobState.STARTED);
				job.incrementVersion();
				RepositoryService.getJobRepository().save(job);
				logger.log(PlannerMessage.JOB_STARTED, String.valueOf(job.getId()));
				answer = true;
				break;
			case STARTED:
				logger.log(PlannerMessage.JOB_ALREADY_STARTED, String.valueOf(job.getId()));
				answer = true;
				break;
			case COMPLETED:
				logger.log(PlannerMessage.JOB_ALREADY_COMPLETED, String.valueOf(job.getId()));
				break;
			case FAILED:
				logger.log(PlannerMessage.JOB_ALREADY_FAILED, String.valueOf(job.getId()));
				break;
			case CLOSED:
				logger.log(PlannerMessage.JOB_ALREADY_CLOSED, String.valueOf(job.getId()));
				break;
			default:
				break;
			}
		}
		return answer;
	}

	/**
	 * Delete a job and its job steps.
	 * 
	 * @param job The job
	 * @return true after success
	 */
	@Transactional
	public Boolean delete(Job job) {
		if (logger.isTraceEnabled()) logger.trace(">>> delete({})", (null == job ? "null" : job.getId()));

		Boolean answer = false;
		// check current state for possibility to be suspended
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case PLANNED:
			case RELEASED:
			case COMPLETED:
			case FAILED:
			case CLOSED:
				List<JobStep> toRem = new ArrayList<JobStep>();
				for (JobStep js : job.getJobSteps()) {
					if (UtilService.getJobStepUtil().delete(js)) {
						js.setJob(null);
						toRem.add(js);
					} else {
						js.setJob(null);
					}
				}
				for (JobStep js : toRem) {
					job.getJobSteps().remove(js);
				}
				job.setProcessingOrder(null);
				logger.log(PlannerMessage.JOB_DELETED, String.valueOf(job.getId()));
				answer = true;
				break;
			case ON_HOLD:
				logger.log(PlannerMessage.JOB_ALREADY_HOLD, String.valueOf(job.getId()));
				break;
			case STARTED:
				logger.log(PlannerMessage.JOB_ALREADY_STARTED, String.valueOf(job.getId()));
				break;
			default:
				break;
			}
		}
 		return answer;
	}

	/**
	 * Delete a job and its job steps except it is running.
	 * 
	 * @param job The job
	 * @return true after success
	 */
	@Transactional
	public Boolean deleteForced(Job job) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteForced({})", (null == job ? "null" : job.getId()));

		Boolean answer = false;
		// check current state for possibility to be suspended
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case PLANNED:
			case RELEASED:
			case ON_HOLD:
			case COMPLETED:
			case FAILED:
			case CLOSED:
				List<JobStep> toRem = new ArrayList<JobStep>();
				for (JobStep js : job.getJobSteps()) {
					if (UtilService.getJobStepUtil().deleteForced(js)) {
						js.setJob(null);
						toRem.add(js);
					} else {
						js.setJob(null);
					}
				}
				for (JobStep js : toRem) {
					job.getJobSteps().remove(js);
					RepositoryService.getJobStepRepository().delete(js);
				}
				job.setProcessingOrder(null);
				logger.log(PlannerMessage.JOB_DELETED, String.valueOf(job.getId()));
				answer = true;
				break;
			case STARTED:
				logger.log(PlannerMessage.JOB_ALREADY_STARTED, String.valueOf(job.getId()));
			default:
				break;
			}
		}
 		return answer;
	}

	/**
	 * Check whether a job and its job steps are finished.
	 * 
	 * @param job The job
	 * @return true after success
	 */
	@Transactional
	public Boolean checkFinish(Long jobId) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkFinish({})", jobId);

		Boolean answer = false;	
		Boolean checkFurther = false;
		Boolean hasChanged = false;
		Job job = null;
		Optional<Job> oJob = RepositoryService.getJobRepository().findById(jobId);
		if (oJob.isPresent()) {
			job = oJob.get();
		}
		// check current state for possibility to be suspended
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case RELEASED:
			case PLANNED:
				break;
			case ON_HOLD:
				Boolean running = RepositoryService.getJobStepRepository().countJobStepRunningByJobId(job.getId()) > 0;
				if (!running) {
					Boolean all = RepositoryService.getJobStepRepository().countJobStepNotFinishedByJobId(job.getId()) == 0;
					if (!all) {
						job.setJobState(JobState.PLANNED);
						RepositoryService.getJobRepository().save(job);
						em.merge(job);
						hasChanged = true;
						checkFurther = true;
						break;	
					}					
				} else {
					break;
				}
			case STARTED:
				Boolean all = RepositoryService.getJobStepRepository().countJobStepNotFinishedByJobId(job.getId()) == 0;
				if (all) {
					Boolean completed = RepositoryService.getJobStepRepository().countJobStepFailedByJobId(job.getId()) == 0;
					if (job.getJobState() == JobState.ON_HOLD) {
						job.setJobState(JobState.PLANNED);
						job.setJobState(JobState.RELEASED);
						job.setJobState(JobState.STARTED);
					}
					if (completed) {
						job.setJobState(JobState.COMPLETED);
					} else {
						job.setJobState(JobState.FAILED);
					}
					RepositoryService.getJobRepository().save(job);
					em.merge(job);
					hasChanged = true;
					checkFurther = true;
				}
				answer = true;
				break;
			case COMPLETED:
				UtilService.getOrderUtil().checkFinish(job.getProcessingOrder().getId());	
				answer = true;
			case FAILED:
			case CLOSED:
				answer = true;
				break;
			default:
				break;
			}	
			if (checkFurther) {
				Boolean hasFailed = false;
				for (JobStep js : job.getJobSteps()) {
					if (js.getJobStepState() == JobStepState.FAILED) {
						hasFailed = true;
						break;
					}
				}
				if (job.getHasFailedJobSteps() != hasFailed) {
					job.setHasFailedJobSteps(hasFailed);
					RepositoryService.getJobRepository().save(job);
					em.merge(job);
					hasChanged = true;
				}
				// check the states of job steps and update job state
				Boolean allHasFinished = true;
				for (JobStep js : job.getJobSteps()) {
					if (js.getJobStepState() != JobStepState.FAILED && js.getJobStepState() != JobStepState.COMPLETED) {
						allHasFinished = false;
						break;
					}
				}
				if (allHasFinished) {
					if (job.getJobState() == JobState.ON_HOLD) {
						job.setJobState(JobState.PLANNED);
						job.setJobState(JobState.RELEASED);
						job.setJobState(JobState.STARTED);
					}
					if (hasFailed) {
						job.setJobState(JobState.FAILED);
					} else {
						job.setJobState(JobState.COMPLETED);
					}
					RepositoryService.getJobRepository().save(job);
					em.merge(job);
					hasChanged = true;
				}
			}
			if (hasChanged) {
				job.incrementVersion();
				RepositoryService.getJobRepository().save(job);
				em.merge(job);
			}
			if (checkFurther) {
				UtilService.getOrderUtil().checkFinish(job.getProcessingOrder().getId());	
			}
		}
 		return answer;
	}
	
	/**
	 * Set the job to failed
	 * 
	 * @param job The job
	 * @param failed
	 */
	@Transactional
	public void setHasFailedJobSteps(Job job, Boolean failed) {
		if (logger.isTraceEnabled()) logger.trace(">>> setHasFailedJobSteps({}, {})", (null == job ? "null" : job.getId()), failed);

		if (failed && !job.getHasFailedJobSteps()) {
			job.setHasFailedJobSteps(failed);
			job.incrementVersion();
			RepositoryService.getJobRepository().save(job);
			em.merge(job);
			UtilService.getOrderUtil().setHasFailedJobSteps(job.getProcessingOrder(), failed);
		}
	}
}
