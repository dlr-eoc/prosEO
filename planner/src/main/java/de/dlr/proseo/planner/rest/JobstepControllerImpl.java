package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.dao.JobRepository;
import de.dlr.proseo.model.dao.JobStepRepository;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.JobDispatcher;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import de.dlr.proseo.planner.rest.model.PlannerJob;
import de.dlr.proseo.planner.rest.model.PlannerJobstep;
import de.dlr.proseo.planner.rest.model.Status;



@Component
public class JobstepControllerImpl implements JobstepController {

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_HEADER_SUCCESS = "Success";
	private static final String MSG_PREFIX = "199 proseo-planner ";
	
	private static Logger logger = LoggerFactory.getLogger(JobControllerImpl.class);

    /**
     * Get production planner jobsteps by id
     * 
     */
	@Override
    public ResponseEntity<List<PlannerJobstep>> getPlannerJobstepsByStatus(
            @Valid
            Status status) {
		
		List<PlannerJobstep> list = new ArrayList<PlannerJobstep>(); 
		Iterable<JobStep> it;
		if (status == null || status.value().equalsIgnoreCase("NONE")) {
			it = RepositoryService.getJobStepRepository().findAll();
		} else {
			JobStepState state = JobStepState.valueOf(status.toString());
			it = RepositoryService.getJobStepRepository().findAllByJobStepState(state);
		}
		for (JobStep js : it) {
			PlannerJobstep pjs = new PlannerJobstep();
			pjs.setId(String.valueOf(js.getId()));
			pjs.setName(String.valueOf(ProductionPlanner.jobNamePrefix + js.getId()));
			if (js.getJobStepState() != null) {
				pjs.setState(js.getJobStepState().toString());
			}
			pjs.setVersion((long) js.getVersion());
			pjs.setProcessingmode(js.getProcessingMode());
			list.add(pjs);			
		}
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_SUCCESS, "");
		return new ResponseEntity<>(list, responseHeaders, HttpStatus.FOUND);
	}

    /**
     * Get production planner jobstep
     * 
     */
	@Override
    public ResponseEntity<PlannerJobstep> getPlannerJobstepByName(String name) {
		Long id = null;
		JobStep jst = new JobStep();
		jst.setProcessingMode("nix"); 
		RepositoryService.getJobStepRepository().save(jst);
		JobDispatcher jd = new JobDispatcher();
		jd.createJobOrder(jst);
		if (name.matches("[0-9]+")) {
			id = Long.valueOf(name);
		} else if (name.startsWith(ProductionPlanner.jobNamePrefix)) {
			id = Long.valueOf(name.substring(ProductionPlanner.jobNamePrefix.length()));
		}
		if (id != null) {
			Optional<JobStep> jso = RepositoryService.getJobStepRepository().findById(id);
			if (jso.isPresent()) {
				JobStep js = jso.get();
				PlannerJobstep pjs = new PlannerJobstep();
				pjs.setId(String.valueOf(js.getId()));
				pjs.setName(String.valueOf(ProductionPlanner.jobNamePrefix + js.getId()));
				if (js.getJobStepState() != null) {
					pjs.setState(js.getJobStepState().toString());
				}
				pjs.setVersion((long) js.getVersion());
				pjs.setProcessingmode(js.getProcessingMode());

				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "");
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.FOUND);
			} 
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
    public ResponseEntity<PlannerJobstep> updateJobsteps(String name) {
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
    public ResponseEntity<PlannerJobstep> deleteJobstepByName(String name) {
		// TODO Auto-generated method stub
    	boolean result = ProductionPlanner.getKubeConfig(null).deleteJob(name);
    	if (result) {
    		String message = String.format(MSG_PREFIX + "job deleted (%s)", name);
    		logger.error(message);
    		HttpHeaders responseHeaders = new HttpHeaders();
    		responseHeaders.set(HTTP_HEADER_SUCCESS, "");
    		return new ResponseEntity<>(responseHeaders, HttpStatus.FOUND);
    	}
    	String message = String.format(MSG_PREFIX + "DELETE not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
}
