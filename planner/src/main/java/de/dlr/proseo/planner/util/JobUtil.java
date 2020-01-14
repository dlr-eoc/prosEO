package de.dlr.proseo.planner.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.util.JobStepUtil;


@Component
public class JobUtil {
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(JobUtil.class);
	private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd'_'HHmmssSSSSSS").withZone(ZoneId.of("UTC"));
		

    @Autowired
    private JobStepUtil jobStepUtil;
		
	@Transactional
	public Boolean suspend(Job job) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
				answer = true;
				break;
			case ON_HOLD:
				break;
			case RELEASED:
				// no job step is running
				// supend all of them
				for (JobStep js : job.getJobSteps()) {
					jobStepUtil.suspend(js);
				}
				job.setJobState(de.dlr.proseo.model.Job.JobState.INITIAL);
				RepositoryService.getJobRepository().save(job);
				answer = true;
				break;
			case STARTED:
				// try to suspend job steps not running
				Boolean oneNotSuspended = true;
				for (JobStep js : job.getJobSteps()) {
					oneNotSuspended = jobStepUtil.suspend(js) & oneNotSuspended;
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
					jobStepUtil.cancel(js);
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
					jobStepUtil.resume(js);
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
	public Boolean delete(Job job) {
		Boolean answer = false;
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
		if (job != null) {
			switch (job.getJobState()) {
			case INITIAL:
			case RELEASED:
				for (JobStep js : job.getJobSteps()) {
					jobStepUtil.delete(js);
				}
				job.setJobState(de.dlr.proseo.model.Job.JobState.FAILED);
				RepositoryService.getJobRepository().delete(job);
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

}
