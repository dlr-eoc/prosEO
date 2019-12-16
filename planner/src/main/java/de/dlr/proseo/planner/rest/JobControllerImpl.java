/**
 * JobControllerImpl.java
 */
package de.dlr.proseo.planner.rest;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.ProductQueryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.JobStepDispatcher;
import de.dlr.proseo.planner.dispatcher.OrderDispatcher;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import de.dlr.proseo.planner.rest.JobController;
import de.dlr.proseo.planner.rest.model.RestJob;

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
	
	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;

    @Autowired
    private OrderDispatcher orderDispatcher;

    @Autowired
    private JobStepDispatcher jobStepDispatcher;
    
    /**
     * Get production planner jobs by id
     * 
     */
	@Override
    public ResponseEntity<List<RestJob>> getJobs(String state) {
		// todo remove test start

		if (productionPlanner.getKubeConfig(null).isConnected()) {
	    	KubeJob aJob = productionPlanner.getKubeConfig(null).createJob("test", "INFO", "INFO");
	    	if (aJob != null) {
	    		productionPlanner.getKubeConfig(null).deleteJob(aJob);
	    	}
		}
		return null;
	}
	@Override
    public ResponseEntity<RestJob> getJob(String name) {
		// TODO Auto-generated method stub
		String message = String.format(MSG_PREFIX + "GET not implemented (%d)", 2001);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Override
    public ResponseEntity<RestJob> createJob(String name, String facility) {
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
			if (order != null) {
				ProcessingFacility pf = null;
				if (facility != null) {
					KubeConfig kc = productionPlanner.getKubeConfig(facility);
					if (kc != null) {
						pf = kc.getProcessingFacility();
					}
				}
				if (pf == null) {
					productionPlanner.getKubeConfig("Lerchenhof").getProcessingFacility();
				}
				if (orderDispatcher.publishOrder(order, pf)) {
					jobStepDispatcher.searchForJobStepsToRun(pf);
					String message = String.format(MSG_PREFIX + "CREATE jobs for order '%s' created (%d)", order.getIdentifier(), 2000);
					logger.error(message);
					HttpHeaders responseHeaders = new HttpHeaders();
					responseHeaders.set(HTTP_HEADER_WARNING, message);
					return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
				} else {
					String message = String.format(MSG_PREFIX + "CREATE jobs for order '%s' not created (%d)", order.getIdentifier(), 2000);
					logger.error(message);
					HttpHeaders responseHeaders = new HttpHeaders();
					responseHeaders.set(HTTP_HEADER_WARNING, message);
					return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
				}
			}
		} else {
			String message = String.format(MSG_PREFIX + "CREATE order '%s' not found (%d)", name, 2000);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		
    	String message = String.format(MSG_PREFIX + "CREATE parameter name missing (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
    	return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Override
    public ResponseEntity<RestJob> deleteJob(String name) {
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
	public ResponseEntity<RestJob> resumeJob(String resumeId) {
		// TODO Auto-generated method stub
    	String message = String.format(MSG_PREFIX + "Resume not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Override 
	public ResponseEntity<RestJob> cancelJob(String cancelId){
		// TODO Auto-generated method stub

    	String message = String.format(MSG_PREFIX + "Cancel not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Override 
	public ResponseEntity<RestJob> suspendJob(String suspendId) {
		// TODO Auto-generated method stub
    	String message = String.format(MSG_PREFIX + "Suspend not implemented (%d)", 2001);
    	logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
}
