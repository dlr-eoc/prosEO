/**
 * JobControllerImpl.java
 */
package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.OrderDispatcher;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.rest.JobController;
import de.dlr.proseo.planner.rest.model.RestJob;
import de.dlr.proseo.planner.rest.model.RestOrder;
import de.dlr.proseo.planner.rest.model.RestUtil;
import de.dlr.proseo.planner.util.JobStepUtil;
import de.dlr.proseo.planner.util.JobUtil;

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
    private JobUtil jobUtil;
    
    @Autowired
    private JobStepUtil jobStepUtil;
    
    
    /**
     * Get production planner jobs by id
     * 
     */
	@Transactional
	@Override
    public ResponseEntity<List<RestJob>> getJobs(String state, Long orderId) {
		List<Job> jobs = this.findJobsByStateAndOrderId(state, orderId);
		List<RestJob> restJobs = new ArrayList<RestJob>();
		if (jobs != null && !jobs.isEmpty()) {
			for (Job job : jobs) { 
				RestJob rj = RestUtil.createRestJob(job);
				if (rj != null) {
					restJobs.add(rj);
				}
			}
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_SUCCESS, "Job deleted");
			return new ResponseEntity<>(restJobs, responseHeaders, HttpStatus.OK);
		}
		String message = String.format(MSG_PREFIX + "Jobs with id %s does not exist (%d)", orderId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Transactional
	@Override
    public ResponseEntity<RestJob> getJob(String jobId) {
		Job job = this.findJobById(jobId);
		if (job != null) {
			RestJob rj = RestUtil.createRestJob(job);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_SUCCESS, "Job found");
			return new ResponseEntity<>(rj, responseHeaders, HttpStatus.OK);
		}
		String message = String.format(MSG_PREFIX + "Job with id %s does not exist (%d)", jobId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Transactional
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
					jobStepUtil.searchForJobStepsToRun(pf);
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
	@Transactional
	@Override
    public ResponseEntity<RestJob> deleteJob(String jobId) {
		Job job = this.findJobById(jobId);
		if (job != null) {
			if (jobUtil.delete(job)) {
				RestJob rj = RestUtil.createRestJob(job);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Job deleted");
				return new ResponseEntity<>(rj, responseHeaders, HttpStatus.OK);
			} else {
				RestJob rj = RestUtil.createRestJob(job);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Job could not be deleted");
				return new ResponseEntity<>(rj, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		String message = String.format(MSG_PREFIX + "Job with id %s does not exist (%d)", jobId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Transactional
	@Override 
	public ResponseEntity<RestJob> resumeJob(String jobId) {
		Job job = this.findJobById(jobId);
		if (job != null) {
			if (jobUtil.resume(job)) {
				RestJob rj = RestUtil.createRestJob(job);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Job resumed");
				return new ResponseEntity<>(rj, responseHeaders, HttpStatus.OK);
			} else {
				RestJob rj = RestUtil.createRestJob(job);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Job could not be resumed");
				return new ResponseEntity<>(rj, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		String message = String.format(MSG_PREFIX + "Job with id %s does not exist (%d)", jobId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	@Transactional
	@Override 
	public ResponseEntity<RestJob> cancelJob(String jobId){
		Job job = this.findJobById(jobId);
		if (job != null) {
			if (jobUtil.cancel(job)) {
				RestJob rj = RestUtil.createRestJob(job);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Job canceled");
				return new ResponseEntity<>(rj, responseHeaders, HttpStatus.OK);
			} else {
				RestJob rj = RestUtil.createRestJob(job);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Job could not be canceled");
				return new ResponseEntity<>(rj, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		String message = String.format(MSG_PREFIX + "Job with id %s does not exist (%d)", jobId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Transactional
	@Override 
	public ResponseEntity<RestJob> suspendJob(String jobId) {
		Job j = this.findJobById(jobId);
		if (j != null) {
			if (jobUtil.suspend(j)) {
				RestJob pj = RestUtil.createRestJob(j);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Job suspended");
				return new ResponseEntity<>(pj, responseHeaders, HttpStatus.OK);
			} else {
				RestJob pj = RestUtil.createRestJob(j);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Job already running or at end");
				return new ResponseEntity<>(pj, responseHeaders, HttpStatus.OK);
			}
		}
		String message = String.format(MSG_PREFIX + "Job with id %s does not exist (%d)", jobId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Transactional
	private Job findJobById(String idStr) {
		Job j = null;
		Long id = null;
		if (idStr != null) {
			if (idStr.matches("[0-9]+")) {
				id = Long.valueOf(idStr);
			}
			if (id != null) {
				Optional<Job> jo = RepositoryService.getJobRepository().findById(id);
				if (jo.isPresent()) {
					j = jo.get();
				}
			}
		}
		return j;
	}
	
	@Transactional
	private List<Job> findJobsByStateAndOrderId(String state, Long orderId) {
		JobState jobState = null;
		if (state != null) {
			jobState = JobState.valueOf(state);
		}
		if (jobState != null && orderId != null) {
			return RepositoryService.getJobRepository().findAllByJobStateAndProcessingOrder(jobState, orderId);
		} else if (jobState != null) {
			return RepositoryService.getJobRepository().findAllByJobState(jobState);
		} else if (orderId != null) {
			return RepositoryService.getJobRepository().findAllByProcessingOrder(orderId);
		}
		return RepositoryService.getJobRepository().findAll();
	}
	
}
