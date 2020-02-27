package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.rest.model.RestJobStep;
import de.dlr.proseo.planner.rest.model.RestUtil;
import de.dlr.proseo.planner.rest.model.Status;
import de.dlr.proseo.planner.util.JobStepUtil;



@Component
public class JobstepControllerImpl implements JobstepController {

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_HEADER_SUCCESS = "Success";
	private static final String MSG_PREFIX = "199 proseo-planner ";
	
	private static Logger logger = LoggerFactory.getLogger(JobControllerImpl.class);

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;
    @Autowired
    private JobStepUtil jobStepUtil;
    
    /**
     * Get production planner jobsteps by status
     * 
     */
	@Override
	@Transactional
    public ResponseEntity<List<RestJobStep>> getJobSteps(
            @Valid
            Status status) {
		
		List<RestJobStep> list = new ArrayList<RestJobStep>(); 
		Iterable<JobStep> it;
		jobStepUtil.searchForJobStepsToRun(null);
		if (status == null || status.value().equalsIgnoreCase("NONE")) {
			it = RepositoryService.getJobStepRepository().findAll();
		} else {
			JobStepState state = JobStepState.valueOf(status.toString());
			it = RepositoryService.getJobStepRepository().findAllByJobStepState(state);
		}
		for (JobStep js : it) {
			RestJobStep pjs = RestUtil.createRestJobStep(js);
			list.add(pjs);			
		}
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_SUCCESS, "");
		return new ResponseEntity<>(list, responseHeaders, HttpStatus.OK);
	}

    /**
     * Get production planner jobstep
     * 
     */
	@Override
	@Transactional
	public ResponseEntity<RestJobStep> getJobStep(String name) {
		JobStep js = this.findJobStepByNameOrId(name);
		if (js != null) {
			RestJobStep pjs = RestUtil.createRestJobStep(js);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_SUCCESS, "");
			return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
		}
    	String message = String.format(MSG_PREFIX + "JobStep element '%s' not found", 2001, name);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		
	}

    /**
     * Create production planner jobstep
     * 
     */
	@Override
    public ResponseEntity<RestJobStep> createJobStep(String name) {
		// TODO Auto-generated method stub
    	String message = String.format(MSG_PREFIX + "CREATE not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

    /**
     * Delete production planner jobstep
     * 
     */
	@Override
    public ResponseEntity<RestJobStep> deleteJobStep(String name) {
		// TODO Auto-generated method stub
    	boolean result = productionPlanner.getKubeConfig(null).deleteJob(name);
    	if (result) {
    		String message = String.format(MSG_PREFIX + "job deleted (%s)", name);
    		logger.error(message);
    		HttpHeaders responseHeaders = new HttpHeaders();
    		responseHeaders.set(HTTP_HEADER_SUCCESS, "");
    		return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
    	}
    	String message = String.format(MSG_PREFIX + "DELETE not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	

	@Override 
	public ResponseEntity<RestJobStep> resumeJobStep(String resumeId) {
		JobStep js = this.findJobStepByNameOrId(resumeId);
		if (js != null) {
			if (jobStepUtil.resume(js)) {
				// canceled
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "JobStep resumed");
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "JobStep already resumed");
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			}
		}
		String message = String.format(MSG_PREFIX + "JobStep with name or id %s does not exist (%d)", resumeId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Override 
	public ResponseEntity<RestJobStep> cancelJobStep(String cancelId) {
		JobStep js = this.findJobStepByNameOrId(cancelId);
		if (js != null) {
			if (jobStepUtil.cancel(js)) {
				// canceled
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "JobStep canceled");
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "JobStep already running or at end");
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			}
		}
		String message = String.format(MSG_PREFIX + "JobStep with name or id %s does not exist (%d)", cancelId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Override 
	public ResponseEntity<RestJobStep> suspendJobStep(String suspendId) {
		JobStep js = this.findJobStepByNameOrId(suspendId);
		if (js != null) {
			if (jobStepUtil.suspend(js)) {
				// suspended
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "JobStep suspended");
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "JobStep already running or at end");
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			}
		}
		String message = String.format(MSG_PREFIX + "JobStep with name or id %s does not exist (%d)", suspendId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
		
	private JobStep findJobStepByNameOrId(String nameOrId) {
		JobStep js = null;
		Long id = null;
		if (nameOrId != null) {
			if (nameOrId.matches("[0-9]+")) {
				id = Long.valueOf(nameOrId);
			} else if (nameOrId.startsWith(ProductionPlanner.jobNamePrefix)) {
				id = Long.valueOf(nameOrId.substring(ProductionPlanner.jobNamePrefix.length()));
			}
			if (id != null) {
				Optional<JobStep> jso = RepositoryService.getJobStepRepository().findById(id);
				if (jso.isPresent()) {
					js = jso.get();
				}
			}
		}
		return js;
	}

}
