/**
 * JobUtil.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.util.JobStepUtil;

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
	/**
	 * Date time formatter for this class
	 */
	private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd'_'HHmmssSSSSSS").withZone(ZoneId.of("UTC"));

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
				job.setJobState(de.dlr.proseo.model.Job.JobState.INITIAL);
				job.incrementVersion();
				RepositoryService.getJobRepository().save(job);
				answer = Messages.JOB_SUSPENDED;
				break;
			case ON_HOLD:
			case STARTED:
				// try to suspend job steps not running
				Boolean allSuspended = true;
				for (JobStep js : job.getJobSteps()) {
					allSuspended = UtilService.getJobStepUtil().suspend(js, force).isTrue() & allSuspended;
				}
				job.incrementVersion();
				if (allSuspended) {
					job.setJobState(de.dlr.proseo.model.Job.JobState.INITIAL);
					RepositoryService.getJobRepository().save(job);
					answer = Messages.JOB_SUSPENDED;
				} else {
					job.setJobState(de.dlr.proseo.model.Job.JobState.ON_HOLD);
					RepositoryService.getJobRepository().save(job);
					answer = Messages.JOB_HOLD;
				}
				break;
			case COMPLETED:
				answer = Messages.JOB_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.JOB_ALREADY_FAILED;
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
				for (JobStep js : job.getJobSteps()) {
					UtilService.getJobStepUtil().retry(js);
				}
				Boolean all = true;
				Boolean allCompleted = true;
				for (JobStep js : job.getJobSteps()) {
					if (!(   js.getJobStepState() == de.dlr.proseo.model.JobStep.JobStepState.INITIAL
						  || js.getJobStepState() == de.dlr.proseo.model.JobStep.JobStepState.COMPLETED)) {
						all = false;
						if (js.getJobStepState() != de.dlr.proseo.model.JobStep.JobStepState.COMPLETED) {
							allCompleted = false;
						}
						
					}
				}
				if (all) {
					if (allCompleted) {
						job.setJobState(de.dlr.proseo.model.Job.JobState.COMPLETED);
						job.incrementVersion();
						RepositoryService.getJobRepository().save(job);
						answer = Messages.JOB_COMPLETED;
					} else {
						job.setJobState(de.dlr.proseo.model.Job.JobState.INITIAL);
						job.incrementVersion();
						RepositoryService.getJobRepository().save(job);
						answer = Messages.JOB_RETRIED;
					}
				} else {
					answer = Messages.JOB_COULD_NOT_RETRY;
				}
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
		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
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
	public Messages resume(Job job) {
		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
				for (JobStep js : job.getJobSteps()) {
					UtilService.getJobStepUtil().resume(js, false);
				}
				job.setJobState(de.dlr.proseo.model.Job.JobState.RELEASED);
				job.incrementVersion();
				RepositoryService.getJobRepository().save(job);
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
		Boolean answer = false;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case RELEASED:
			case COMPLETED:
			case FAILED:
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
		Boolean answer = false;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case RELEASED:
			case ON_HOLD:
			case COMPLETED:
			case FAILED:
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
	public Boolean checkFinish(Job job) {
		Boolean answer = false;	
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
						job.setJobState(JobState.INITIAL);
						job.incrementVersion();
						RepositoryService.getJobRepository().save(job);
						em.merge(job);
						UtilService.getOrderUtil().checkFinish(job.getProcessingOrder());
						break;	
					}					
				} else {
					break;
				}
			case STARTED:
				Boolean all = RepositoryService.getJobStepRepository().countJobStepNotFinishedByJobId(job.getId()) == 0;
				if (all) {
					Boolean completed = RepositoryService.getJobStepRepository().countJobStepFailedByJobId(job.getId()) == 0;
					if (completed) {
						job.setJobState(JobState.COMPLETED);
					} else {
						job.setJobState(JobState.FAILED);
					}
					job.incrementVersion();
					RepositoryService.getJobRepository().save(job);
					em.merge(job);
					UtilService.getOrderUtil().checkFinish(job.getProcessingOrder());					
				}
				answer = true;
				break;
			case COMPLETED:
			case FAILED:
				answer = true;
				break;
			default:
				break;
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
	public void updateState(Job job, JobStepState jsState) {
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
					UtilService.getOrderUtil().updateState(job.getProcessingOrder(), job.getJobState());
				}
				break;
			case RELEASED:
				break;
			case STARTED:
				break;
			case ON_HOLD:
				break;
			case COMPLETED:
				break;
			case FAILED:
				if (jsState == JobStepState.INITIAL || jsState == JobStepState.WAITING_INPUT) {
					job.setJobState(JobState.INITIAL);
					job.incrementVersion();
					RepositoryService.getJobRepository().save(job);
					em.merge(job);
					UtilService.getOrderUtil().updateState(job.getProcessingOrder(), job.getJobState());
				}
				break;
			default:
				break;
			}	
		}
	}
}
