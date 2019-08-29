package de.dlr.proseo.planner.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import de.dlr.proseo.planner.rest.model.PlannerJob;
import de.dlr.proseo.planner.rest.model.PlannerJobstep;



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
    public ResponseEntity<List<PlannerJobstep>> getPlannerJobsteps() {
		return null;
	}

    /**
     * Get production planner jobstep
     * 
     */
	@Override
    public ResponseEntity<PlannerJobstep> getPlannerJobstepByName(String name) {
		// TODO Auto-generated method stub
		String message = String.format(MSG_PREFIX + "GET not implemented (%d)", 2001);
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
