package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.Date;
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
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.JobDispatcher;
import de.dlr.proseo.planner.dispatcher.JobStepDispatcher;
import de.dlr.proseo.planner.rest.model.RestJob;
import de.dlr.proseo.planner.rest.model.RestJobStep;
import de.dlr.proseo.planner.rest.model.Status;
import de.dlr.proseo.planner.rest.model.StderrLogLevel;
import de.dlr.proseo.planner.rest.model.StdoutLogLevel;



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
    private JobStepDispatcher jobStepDispatcher;
    
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
		jobStepDispatcher.searchForJobStepsToRun(null);
		if (status == null || status.value().equalsIgnoreCase("NONE")) {
			it = RepositoryService.getJobStepRepository().findAll();
		} else {
			JobStepState state = JobStepState.valueOf(status.toString());
			it = RepositoryService.getJobStepRepository().findAllByJobStepState(state);
		}
		for (JobStep js : it) {
			RestJobStep pjs = createAndCopyToRestJobStep(js);
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
		Long id = null;
		if (name.matches("[0-9]+")) {
			id = Long.valueOf(name);
		} else if (name.startsWith(ProductionPlanner.jobNamePrefix)) {
			id = Long.valueOf(name.substring(ProductionPlanner.jobNamePrefix.length()));
		}
		if (id != null) {
			Optional<JobStep> jso = RepositoryService.getJobStepRepository().findById(id);
			if (jso.isPresent()) {
				JobStep js = jso.get();
				
				RestJobStep pjs = createAndCopyToRestJobStep(js);
				
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "");
				return new ResponseEntity<>(pjs, responseHeaders, HttpStatus.OK);
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
	

	private RestJobStep createAndCopyToRestJobStep(JobStep js) {
		RestJobStep pjs = new RestJobStep();
		if (js != null) {
			pjs = new RestJobStep();
			pjs.setId(Long.valueOf(js.getId()));
			pjs.setName(ProductionPlanner.jobNamePrefix + js.getId());
			if (js.getJobStepState() != null) {
				pjs.setJobStepState(de.dlr.proseo.planner.rest.model.JobStepState.valueOf(js.getJobStepState().toString()));
			}
			pjs.setVersion((long) js.getVersion());
			pjs.setProcessingMode(js.getProcessingMode());
			if (js.getProcessingStartTime() != null) { 
				pjs.setProcessingStartTime(Date.from(js.getProcessingStartTime()));
			}
			if (js.getProcessingCompletionTime() != null) { 
				pjs.setProcessingCompletionTime(Date.from(js.getProcessingCompletionTime()));
			}
			pjs.setProcessingStdOut(js.getProcessingStdOut());
			pjs.setProcessingStdErr(js.getProcessingStdErr());
			pjs.setStderrLogLevel(StderrLogLevel.fromValue(js.getStderrLogLevel().toString()));
			pjs.setStdoutLogLevel(StdoutLogLevel.fromValue(js.getStdoutLogLevel().toString()));
			pjs.setJobId(js.getJob() == null ? null : js.getJob().getId());
			if (js.getOutputProduct() != null && js.getOutputProduct().getProductClass() != null && js.getOutputProduct().getProductClass().getProductType() != null) {
				pjs.setOutputProductClass(js.getOutputProduct().getProductClass().getProductType());
			}
			for (ProductQuery pq : js.getInputProductQueries()) {
				String pt = pq.getRequestedProductClass().getProductType();
				if (!pjs.getInputProductClasses().contains(pt)) {
					pjs.getInputProductClasses().add(pt);
				}
			}
		} 
		return pjs;
	}
	@Override 
	public ResponseEntity<RestJobStep> resumeJobStep(String resumeId) {
		// TODO Auto-generated method stub
    	String message = String.format(MSG_PREFIX + "Resume not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Override 
	public ResponseEntity<RestJobStep> cancelJobStep(String cancelId){
		// TODO Auto-generated method stub

    	String message = String.format(MSG_PREFIX + "Cancel not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Override 
	public ResponseEntity<RestJobStep> suspendJobStep(String suspendId) {
		// TODO Auto-generated method stub
    	String message = String.format(MSG_PREFIX + "Suspend not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

}
