/**
 * JobstepControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
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
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import de.dlr.proseo.planner.rest.model.RestJobStep;
import de.dlr.proseo.planner.rest.model.RestUtil;
import de.dlr.proseo.planner.rest.model.Status;
import de.dlr.proseo.planner.util.JobStepUtil;


/**
 * Spring MVC controller for the prosEO planner; implements the services required to plan
 * and handle job steps.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class JobstepControllerImpl implements JobstepController {
	
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
		responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), Messages.OK.getDescription());
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
			if (js.getJobStepState() == JobStepState.RUNNING && js.getJob() != null) {
				if (js.getJob().getProcessingFacility() != null) {
					KubeConfig kc = productionPlanner.getKubeConfig(js.getJob().getProcessingFacility().getName());
					if (kc != null) {
						KubeJob kj = kc.getKubeJob(ProductionPlanner.jobNamePrefix + js.getId());
						if (kj != null) {
							kj.getInfo(ProductionPlanner.jobNamePrefix + js.getId());
						}
					}
				}
			}
			RestJobStep pjs = RestUtil.createRestJobStep(js);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), Messages.OK.getDescription());
			return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
		}
    	String message = Messages.JOBSTEP_NOT_EXIST.formatWithPrefix(name);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		
	}

	@Override 
	@Transactional
	public ResponseEntity<RestJobStep> resumeJobStep(String jobstepId) {
		JobStep js = this.findJobStepByNameOrId(jobstepId);
		if (js != null) {
			Messages msg = jobStepUtil.resume(js);
			if (msg.isTrue()) {
				// canceled
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(jobstepId));
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(jobstepId));
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			}
		}
		String message =  Messages.JOBSTEP_NOT_EXIST.formatWithPrefix(jobstepId);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Override 
	@Transactional
	public ResponseEntity<RestJobStep> cancelJobStep(String jobstepId) {
		JobStep js = this.findJobStepByNameOrId(jobstepId);
		if (js != null) {
			Messages msg = jobStepUtil.cancel(js);
			if (msg.isTrue()) {
				// canceled
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(jobstepId));
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(jobstepId));
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			}
		}
		String message =  Messages.JOBSTEP_NOT_EXIST.formatWithPrefix(jobstepId);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Override 
	@Transactional
	public ResponseEntity<RestJobStep> suspendJobStep(String jobstepId) {
		JobStep js = this.findJobStepByNameOrId(jobstepId);
		if (js != null) {
			Messages msg = jobStepUtil.suspend(js); 
			if (msg.isTrue()) {
				// suspended
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(jobstepId));
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(jobstepId));
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			}
		}
		String message =  Messages.JOBSTEP_NOT_EXIST.formatWithPrefix(jobstepId);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Transactional
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
