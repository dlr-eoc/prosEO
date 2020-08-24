/**
 * JobstepControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.rest.JobstepController;
import de.dlr.proseo.model.rest.model.RestJobStep;
import de.dlr.proseo.model.rest.model.Status;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import de.dlr.proseo.planner.rest.model.RestUtil;
import de.dlr.proseo.planner.util.JobStepUtil;
import de.dlr.proseo.planner.util.JobUtil;
import de.dlr.proseo.planner.util.UtilService;


/**
 * Spring MVC controller for the prosEO planner; implements the services required to plan
 * and handle job steps.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class JobstepControllerImpl implements JobstepController {
	
	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(JobControllerImpl.class);

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;
    
    @Autowired
    private JobStepUtil jobStepUtil;
    
   
    /**
     * Get production planner job steps by status
     * 
     */
	@Override
	@Transactional
    public ResponseEntity<List<RestJobStep>> getJobSteps(Status status, String mission, Long last) {		
		List<RestJobStep> list = new ArrayList<RestJobStep>(); 
		List<JobStep> it ;
		if (status == null || status.value().equalsIgnoreCase("NONE")) {
			it = RepositoryService.getJobStepRepository().findAll();
		} else if (mission != null) {
			JobStepState state = JobStepState.valueOf(status.toString());
			//it = new ArrayList<JobStep>();
			if (last != null && last > 0) {
				List<JobStep> itall = jobStepUtil.findOrderedByJobStepStateAndMission(state, mission, last.intValue());
				if (last < itall.size()) {
					it = itall.subList(0, last.intValue());
				} else {
					it = itall;
				}
			} else {
				it = RepositoryService.getJobStepRepository().findAllByJobStepStateAndMissionOrderByDate(state, mission);
			}
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
     * Get production planner job step identified by name or id
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

    /**
     * Resume a production planner job step identified by name or id
     * 
     */
	@Override 
	@Transactional
	public ResponseEntity<RestJobStep> resumeJobStep(String jobstepId) {
		JobStep js = this.findJobStepByNameOrId(jobstepId);
		if (js != null) {
			Messages msg = jobStepUtil.resume(js, true);
			if (msg.isTrue()) {
				UtilService.getJobUtil().updateState(js.getJob(), js.getJobStepState());
				if (js.getJob() != null && js.getJob().getProcessingFacility() != null) {
					KubeConfig kc = productionPlanner.getKubeConfig(js.getJob().getProcessingFacility().getName());
					if (kc != null) {
						UtilService.getJobStepUtil().checkJobStepToRun(kc, js);
					}
				}
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
		Messages.JOBSTEP_NOT_EXIST.log(logger, jobstepId);
		String message =  Messages.JOBSTEP_NOT_EXIST.formatWithPrefix(jobstepId);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

    /**
     * Cancel a production planner job step identified by name or id
     * 
     */
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
		Messages.JOBSTEP_NOT_EXIST.log(logger, jobstepId);
		String message =  Messages.JOBSTEP_NOT_EXIST.formatWithPrefix(jobstepId);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

    /**
     * Cancel a production planner job step identified by name or id.
     * Kill the job step if force equals true, otherwise wait until end of Kubernetes job.
     * 
     */
	@Override 
	@Transactional
	public ResponseEntity<RestJobStep> suspendJobStep(String jobstepId, Boolean force) {
		JobStep js = this.findJobStepByNameOrId(jobstepId);
		if (js != null) {
			Messages msg = jobStepUtil.suspend(js, force); 
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
		Messages.JOBSTEP_NOT_EXIST.log(logger, jobstepId);
		String message =  Messages.JOBSTEP_NOT_EXIST.formatWithPrefix(jobstepId);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	/**
	 * Get job step identified by name or id.
	 * @param nameOrId
	 * @return Job step found
	 */
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

    /**
     * Retry a production planner job step identified by name or id.
     * 
     */
	@Transactional
	@Override
	public ResponseEntity<RestJobStep> retryJobStep(String jobstepId) {
		JobStep js = this.findJobStepByNameOrId(jobstepId);
		if (js != null) {
			Messages msg = jobStepUtil.retry(js);
			if (msg.isTrue()) {
				UtilService.getJobUtil().updateState(js.getJob(), js.getJobStepState());
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(jobstepId));
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			} else {
				RestJobStep pjs = RestUtil.createRestJobStep(js);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(jobstepId));
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
			}
		}
		Messages.JOBSTEP_NOT_EXIST.log(logger, jobstepId);
		String message =  Messages.JOBSTEP_NOT_EXIST.formatWithPrefix(jobstepId);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

}
