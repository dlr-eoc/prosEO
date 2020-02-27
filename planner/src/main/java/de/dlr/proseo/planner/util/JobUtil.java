package de.dlr.proseo.planner.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
import de.dlr.proseo.planner.util.JobStepUtil;


@Component
@Transactional
public class JobUtil {
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(JobUtil.class);
	private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd'_'HHmmssSSSSSS").withZone(ZoneId.of("UTC"));
		
	@Transactional
	public Boolean suspend(Job job) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case RELEASED:
				// no job step is running
				// supend all of them
				for (JobStep js : job.getJobSteps()) {
					UtilService.getJobStepUtil().suspend(js);
				}
				job.setJobState(de.dlr.proseo.model.Job.JobState.INITIAL);
				RepositoryService.getJobRepository().save(job);
				answer = true;
				break;
			case ON_HOLD:
			case STARTED:
				// try to suspend job steps not running
				Boolean oneNotSuspended = true;
				for (JobStep js : job.getJobSteps()) {
					oneNotSuspended = UtilService.getJobStepUtil().suspend(js) & oneNotSuspended;
				}
				if (oneNotSuspended) {
					job.setJobState(de.dlr.proseo.model.Job.JobState.ON_HOLD);
					RepositoryService.getJobRepository().save(job);
					answer = false;
				} else {
					job.setJobState(de.dlr.proseo.model.Job.JobState.INITIAL);
					RepositoryService.getJobRepository().save(job);
					answer = true;
				}
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
	public Boolean cancel(Job job) {
		Boolean answer = false;
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
				answer = true;
				break;
			case RELEASED:
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
	public Boolean resume(Job job) {
		Boolean answer = false;
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
				answer = true;
				break;
			case RELEASED:
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
				// RepositoryService.getJobRepository().delete(job);
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
			case ON_HOLD:
				break;
			case STARTED:
				Boolean all = true;
				Boolean completed = true;
				for (JobStep js : job.getJobSteps()) {
					switch (js.getJobStepState()) {
					case COMPLETED:
						break;
					case FAILED:
						completed = false;
						break;
					default:
						all = false;
						break;
					}
				}
				if (all) {
					if (completed) {
						job.setJobState(JobState.COMPLETED);
					} else {
						job.setJobState(JobState.FAILED);
					}
					UtilService.getOrderUtil().checkFinish(job.getProcessingOrder());
					RepositoryService.getJobRepository().save(job);
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
