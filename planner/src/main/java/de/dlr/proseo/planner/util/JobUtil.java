/**
 * JobUtil.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Messages;

/**
 * Handle jobs
 * 
 * @author Ernst Melchinger
 *
 */
@Component
@Transactional
public class JobUtil {
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(JobUtil.class);

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	/**
	 * Suspend the job and its job steps. If force ist true, running Kubernetes jobs of are killed.
	 *  
	 * @param job The job
	 * @param force 
	 * @return Result message
	 */
	@Transactional
	public Messages suspend(Job job, Boolean force) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspend({}, {})", (null == job ? "null" : job.getId()), force);

		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
				answer = Messages.JOB_INITIAL;
				break;
			case RELEASED:
				// no job step is running
				// supend all of them
				for (JobStep js : job.getJobSteps()) {
					UtilService.getJobStepUtil().suspend(js, force);
				}
				job.setJobState(de.dlr.proseo.model.Job.JobState.PLANNED);
				job.incrementVersion();
				RepositoryService.getJobRepository().save(job);
				answer = Messages.JOB_SUSPENDED;
				break;
			case ON_HOLD:
			case STARTED:
				// try to suspend job steps not running
				Boolean allSuspended = true;
				for (JobStep js : job.getJobSteps()) {
					allSuspended = UtilService.getJobStepUtil().suspend(js, force).isTrue() && allSuspended;
				}
				job.incrementVersion();
				job.setJobState(de.dlr.proseo.model.Job.JobState.ON_HOLD);
				answer = Messages.JOB_HOLD;
				if (allSuspended) {
					job.setJobState(de.dlr.proseo.model.Job.JobState.PLANNED); // direct transition to INITIAL not allowed
					answer = Messages.JOB_SUSPENDED;
				}
				RepositoryService.getJobRepository().save(job);
				break;
			case COMPLETED:
				answer = Messages.JOB_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.JOB_ALREADY_FAILED;
			case CLOSED:
				answer = Messages.JOB_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, String.valueOf(job.getId()));
		}
		return answer;
	}


	/**
	 * Close a job and its job steps.
	 * 
	 * @param job The job
	 * @return Result message
	 */
	@Transactional
	public Messages close(Job job) {
		if (logger.isTraceEnabled()) logger.trace(">>> close({})", (null == job ? "null" : job.getId()));

		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case PLANNED:
			case RELEASED:
			case STARTED:
			case ON_HOLD:
				answer = Messages.JOB_COULD_NOT_CLOSE;
				break;
			case COMPLETED:
			case FAILED:
				job.setJobState(JobState.CLOSED);
				for (JobStep js : job.getJobSteps()) {
					UtilService.getJobStepUtil().close(js);
				}		
				job.incrementVersion();
				RepositoryService.getJobRepository().save(job);
				em.merge(job);
				answer = Messages.JOB_CLOSED;
				break;
			case CLOSED:
				answer = Messages.JOB_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, String.valueOf(job.getId()));
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
	public Messages retry(Job job) {
		if (logger.isTraceEnabled()) logger.trace(">>> retry({})", (null == job ? "null" : job.getId()));

		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case RELEASED:
			case STARTED:
				answer = Messages.JOB_COULD_NOT_RETRY;
				break;
			case COMPLETED:
				answer = Messages.JOB_ALREADY_COMPLETED;
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
					if (allCompleted) {
						job.setJobState(de.dlr.proseo.model.Job.JobState.PLANNED);
						job.setJobState(de.dlr.proseo.model.Job.JobState.RELEASED);
						job.setJobState(de.dlr.proseo.model.Job.JobState.STARTED);
						job.setJobState(de.dlr.proseo.model.Job.JobState.COMPLETED);
						job.incrementVersion();
						RepositoryService.getJobRepository().save(job);
						em.merge(job);
						answer = Messages.JOB_COMPLETED;
					} else {
						job.setJobState(de.dlr.proseo.model.Job.JobState.PLANNED);
						job.incrementVersion();
						RepositoryService.getJobRepository().save(job);
						em.merge(job);
						answer = Messages.JOB_RETRIED;
					}
				} else {
					answer = Messages.JOB_COULD_NOT_RETRY;
				}
				break;
			case CLOSED:
				answer = Messages.JOB_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, String.valueOf(job.getId()));
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
	public Messages cancel(Job job) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancel({})", (null == job ? "null" : job.getId()));

		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
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
				answer = Messages.JOB_CANCELED;
				break;
			case RELEASED:
				answer = Messages.JOB_ALREADY_RELEASED;
				break;
			case ON_HOLD:
				answer = Messages.JOB_ALREADY_HOLD;
				break;
			case STARTED:
				answer = Messages.JOB_ALREADY_STARTED;
				break;
			case COMPLETED:
				answer = Messages.JOB_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.JOB_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.JOB_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, String.valueOf(job.getId()));
		}
		return answer;
	}

	/**
	 * Resume a job and its job steps.
	 * 
	 * @param job The job
	 * @return Result message
	 */
	@Transactional
	public Messages resume(long jobId) {
		if (logger.isTraceEnabled()) logger.trace(">>> resume({})", jobId);

		Messages answer = Messages.FALSE;
		Job job = null;
		Optional<Job> jo = RepositoryService.getJobRepository().findById(jobId);
		if (jo.isPresent()) {
			job = jo.get();
		}
		if (job != null) {
			if (job.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
				Messages.FACILITY_NOT_AVAILABLE.log(logger, job.getProcessingFacility().getName(),
						job.getProcessingFacility().getFacilityState().toString());

		    	return Messages.FACILITY_NOT_AVAILABLE;
			}
		}
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
				answer = Messages.JOB_HASTOBE_PLANNED;
				break;
			case PLANNED:
				job.setJobState(de.dlr.proseo.model.Job.JobState.RELEASED);
				job.incrementVersion();
				RepositoryService.getJobRepository().save(job);
				for (JobStep js : job.getJobSteps()) {
					UtilService.getJobStepUtil().resume(js, false);
				}
				answer = Messages.JOB_RELEASED;
				break;
			case RELEASED:
				answer = Messages.JOB_RELEASED;
				break;
			case ON_HOLD:
				answer = Messages.JOB_ALREADY_HOLD;
				break;
			case STARTED:
				answer = Messages.JOB_ALREADY_STARTED;
				break;
			case COMPLETED:
				answer = Messages.JOB_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.JOB_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.JOB_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, String.valueOf(job.getId()));
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
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
				Messages.JOB_INITIAL.log(logger, String.valueOf(job.getId()));
				break;
			case ON_HOLD:
				Messages.JOB_HOLD.log(logger, String.valueOf(job.getId()));
				break;
			case RELEASED:
				UtilService.getOrderUtil().startOrder(job.getProcessingOrder());
				job.setJobState(de.dlr.proseo.model.Job.JobState.STARTED);
				job.incrementVersion();
				RepositoryService.getJobRepository().save(job);
				Messages.JOB_STARTED.log(logger, String.valueOf(job.getId()));
				answer = true;
				break;
			case STARTED:
				Messages.JOB_ALREADY_STARTED.log(logger, String.valueOf(job.getId()));
				answer = true;
				break;
			case COMPLETED:
				Messages.JOB_ALREADY_COMPLETED.log(logger, String.valueOf(job.getId()));
				break;
			case FAILED:
				Messages.JOB_ALREADY_FAILED.log(logger, String.valueOf(job.getId()));
				break;
			case CLOSED:
				Messages.JOB_ALREADY_CLOSED.log(logger, String.valueOf(job.getId()));
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
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
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
						toRem.add(js);
					} else {
						js.setJob(null);
					}
				}
				for (JobStep js : toRem) {
					job.getJobSteps().remove(js);
				}
				job.setProcessingOrder(null);
				Messages.JOB_DELETED.log(logger, String.valueOf(job.getId()));
				answer = true;
				break;
			case ON_HOLD:
				Messages.JOB_ALREADY_HOLD.log(logger, String.valueOf(job.getId()));
				break;
			case STARTED:
				Messages.JOB_ALREADY_STARTED.log(logger, String.valueOf(job.getId()));
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
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
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
				Messages.JOB_DELETED.log(logger, String.valueOf(job.getId()));
				answer = true;
				break;
			case STARTED:
				Messages.JOB_ALREADY_STARTED.log(logger, String.valueOf(job.getId()));
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
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case RELEASED:
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
	 * Update the job state depending on job step state
	 * TODO
	 * 
	 * @param job The job
	 * @param jsState The job step state
	 */
	@Transactional
	public void updateState(Job jobOrig, JobStepState jsState) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateState({}, {})", (null == jobOrig ? "null" : jobOrig.getId()), jsState);

		Job job = null;
		Optional<Job> oJob = RepositoryService.getJobRepository().findById(jobOrig.getId());
		if (oJob.isPresent()) {
			job = oJob.get();
		}
		// first implementation for retry
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
				if (jsState == JobStepState.READY || jsState == JobStepState.WAITING_INPUT) {
					job.setJobState(JobState.RELEASED);
					job.incrementVersion();
					RepositoryService.getJobRepository().save(job);
					em.merge(job);
				} else if (jsState == JobStepState.FAILED) {
					Boolean allState = true;
					this.setHasFailedJobSteps(job,  true);
					for (JobStep js : job.getJobSteps()) {
						if (js.getJobStepState() != JobStepState.FAILED && js.getJobStepState() != JobStepState.COMPLETED) {
							allState = false;
							break;
						}
					}
					if (allState) {
						job.setJobState(JobState.FAILED);
						job.incrementVersion();
						RepositoryService.getJobRepository().save(job);
						em.merge(job);
					}
				}
				break;
			case RELEASED:
				if (jsState == JobStepState.RUNNING) {
					job.setJobState(JobState.STARTED);
					job.incrementVersion();
					RepositoryService.getJobRepository().save(job);
					em.merge(job);
				}
				break;
			case STARTED:
				// fall through intended
			case ON_HOLD:
				if (jsState == JobStepState.PLANNED) {
					Boolean allState = true;
					for (JobStep js : job.getJobSteps()) {
						if (js.getJobStepState() != JobStepState.PLANNED) {
							allState = false;
							break;
						}
					}
					if (allState) {
						job.setJobState(JobState.PLANNED);
						job.setHasFailedJobSteps(false);
						job.incrementVersion();
						RepositoryService.getJobRepository().save(job);
						em.merge(job);
					}
				} else if (jsState == JobStepState.FAILED) {
					Boolean allState = true;
					this.setHasFailedJobSteps(job,  true);
					for (JobStep js : job.getJobSteps()) {
						if (js.getJobStepState() != JobStepState.FAILED && js.getJobStepState() != JobStepState.COMPLETED) {
							allState = false;
							break;
						}
					}
					if (allState) {
						job.setJobState(JobState.FAILED);
						job.incrementVersion();
						RepositoryService.getJobRepository().save(job);
						em.merge(job);
					}
				}
				break;
			case COMPLETED:
				break;
			case FAILED:
				if (jsState == JobStepState.PLANNED || jsState == JobStepState.WAITING_INPUT) {
					job.setJobState(JobState.PLANNED);
					job.incrementVersion();
					RepositoryService.getJobRepository().save(job);
					em.merge(job);
				}
				break;
			case CLOSED:
				break;
			default:
				break;
			}	
			Boolean hasFailed = false;
			for (JobStep js : job.getJobSteps()) {
				if (js.getJobStepState() == JobStepState.FAILED) {
					hasFailed = true;
					break;
				}
			}
			job.setHasFailedJobSteps(hasFailed);
			// check the states of job steps and update job state
			Boolean allHasFinished = true;
			for (JobStep js : job.getJobSteps()) {
				if (js.getJobStepState() != JobStepState.FAILED && js.getJobStepState() != JobStepState.COMPLETED) {
					allHasFinished = false;
					break;
				}
			}
			if (allHasFinished) {
				if (hasFailed) {
					job.setJobState(JobState.FAILED);
				} else {
					if (job.getJobState() == JobState.FAILED || job.getJobState() == JobState.PLANNED) {
						job.setJobState(JobState.PLANNED);
						job.setJobState(JobState.RELEASED);
						job.setJobState(JobState.STARTED);
					}
					job.setJobState(JobState.COMPLETED);
				}
				job.incrementVersion();
				RepositoryService.getJobRepository().save(job);
				em.merge(job);
			}
			UtilService.getOrderUtil().updateState(job.getProcessingOrder(), job.getJobState());
		}
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
