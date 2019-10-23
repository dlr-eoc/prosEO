/**
 * JobControllerImpl.java
 */
package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.mapping.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.dao.JobStepRepository;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import de.dlr.proseo.planner.rest.JobController;
import de.dlr.proseo.planner.rest.model.PlannerJob;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;

/**
 * Spring MVC controller for the prosEO Production Planner; TODO
 *
 * @author NN
 *
 */
@Component
public class JobControllerImpl implements JobController {

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_HEADER_SUCCESS = "Success";
	private static final String MSG_PREFIX = "199 proseo-planner ";
	
	private static Logger logger = LoggerFactory.getLogger(JobControllerImpl.class);
	
    /**
     * Get production planner jobs by id
     * 
     */
	@Override
    public ResponseEntity<List<PlannerJob>> getPlannerJobsByState(String state) {
		// todo remove test start

		if (ProductionPlanner.getKubeConfig(null).isConnected()) {
	    	KubeJob aJob = ProductionPlanner.getKubeConfig(null).createJob("test");
	    	if (aJob != null) {
	    		ProductionPlanner.getKubeConfig(null).deleteJob(aJob);
	    	}
		}
		return null;
	}
	@Override
    public ResponseEntity<PlannerJob> getPlannerJobByName(String name) {
		// TODO Auto-generated method stub
		String message = String.format(MSG_PREFIX + "GET not implemented (%d)", 2001);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Override
    public ResponseEntity<PlannerJob> updateJobs(String name) {
		// TODO Auto-generated method stub
		if (name != null) {
			Optional<ProcessingOrder> orderOpt = null;
			ProcessingOrder order = null;
			try {
				Long id = Long.valueOf(name);
				orderOpt = RepositoryService.getOrderRepository().findById(id);
				if (orderOpt.isPresent()) {
					order = orderOpt.get();
				}
			} catch (NumberFormatException nfe) {
				// use name as identifier
			}
			if (order == null) {
				order = RepositoryService.getOrderRepository().findByIdentifier(name);
			}
			
		}
		
    	String message = String.format(MSG_PREFIX + "CREATE not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Override
    public ResponseEntity<PlannerJob> deleteJobByName(String name) {
		// TODO Auto-generated method stub
    	boolean result = ProductionPlanner.getKubeConfig(null).deleteJob(name);
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
}
