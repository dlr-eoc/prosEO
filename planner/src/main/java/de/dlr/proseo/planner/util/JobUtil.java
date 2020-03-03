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
	private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd'_'HHmmssSSSSSS").withZone(ZoneId.of("UTC"));

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	@Transactional
	public Messages suspend(Job job) {
		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
				answer = Messages.JOB_HASTOBE_RELEASED;
				break;
			case RELEASED:
				// no job step is running
				// supend all of them
				for (JobStep js : job.getJobSteps()) {
					UtilService.getJobStepUtil().suspend(js);
				}
				job.setJobState(de.dlr.proseo.model.Job.JobState.INITIAL);
				RepositoryService.getJobRepository().save(job);
				answer = Messages.JOB_SUSPENDED;
				break;
			case ON_HOLD:
				answer = Messages.JOB_ALREADY_HOLD;
				break;
			case STARTED:
				// try to suspend job steps not running
				Boolean allSuspended = true;
				for (JobStep js : job.getJobSteps()) {
					allSuspended = UtilService.getJobStepUtil().suspend(js).isTrue() & allSuspended;
				}
				if (allSuspended) {
					job.setJobState(de.dlr.proseo.model.Job.JobState.INITIAL);
					RepositoryService.getJobRepository().save(job);
					answer = Messages.JOB_HOLD;
				} else {
					job.setJobState(de.dlr.proseo.model.Job.JobState.ON_HOLD);
					RepositoryService.getJobRepository().save(job);
					answer = Messages.JOB_SUSPENDED;
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
		}
		return answer;
	}

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
		}
		return answer;
	}

	@Transactional
	public Messages resume(Job job) {
		Messages answer = Messages.FALSE;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
				for (JobStep js : job.getJobSteps()) {
					UtilService.getJobStepUtil().resume(js);
				}
				job.setJobState(de.dlr.proseo.model.Job.JobState.RELEASED);
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
		}
		return answer;
	}
	
	@Transactional
	public Boolean startJob(Job job) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case ON_HOLD:
				break;
			case RELEASED:
				UtilService.getOrderUtil().startOrder(job.getProcessingOrder());
				job.setJobState(de.dlr.proseo.model.Job.JobState.STARTED);
				RepositoryService.getJobRepository().save(job);
				answer = true;
				break;
			case STARTED:
				answer = true;
				break;
			case COMPLETED:
			case FAILED:
			default:
				break;
			}
		}
		return answer;
	}

	@Transactional
	public Boolean delete(Job job) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case RELEASED:
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
				job.getOutputParameters().clear();
				answer = true;
				break;
			case ON_HOLD:
			case STARTED:
			case COMPLETED:
			case FAILED:
			default:
				break;
			}
		}
 		return answer;
	}

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

}
