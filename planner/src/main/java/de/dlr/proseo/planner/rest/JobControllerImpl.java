/**
 * JobControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.planner.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.planner.rest.JobController;
import de.dlr.proseo.planner.rest.model.PlannerJob;

/**
 * Spring MVC controller for the prosEO Production Planner; TODO
 *
 * @author NN
 *
 */
@Component
public class JobControllerImpl implements JobController {

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-planner ";
	
	private static Logger logger = LoggerFactory.getLogger(JobControllerImpl.class);

	/* (non-Javadoc)
	 * @see de.dlr.proseo.planner.rest.JobController#getPlannerJobsById(java.lang.String)
	 */
	@Override
	public ResponseEntity<List<PlannerJob>> getPlannerJobsById(String id) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "GET for id %s not implemented (%d)", id, 2000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.planner.rest.JobController#createPlannerJob(de.dlr.proseo.ingestor.rest.model.PlannerJob)
	 */
	@Override
	public ResponseEntity<PlannerJob> createPlannerJob(PlannerJob plannerJob) {
		// TODO Auto-generated method stub
		String message = String.format(MSG_PREFIX + "POST not implemented (%d)", 2001);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

}
