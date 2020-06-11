/**
 * JobControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
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
import de.dlr.proseo.model.rest.JobController;
import de.dlr.proseo.model.rest.model.RestJob;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.OrderDispatcher;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.rest.model.RestUtil;
import de.dlr.proseo.planner.util.JobStepUtil;
import de.dlr.proseo.planner.util.JobUtil;
import de.dlr.proseo.planner.util.UtilService;

/**
 * Spring MVC controller for the prosEO planner; implements the services required to plan
 * and handle jobs.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class JobControllerImpl implements JobController {
	
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
			responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), Messages.OK.getDescription());
			return new ResponseEntity<>(restJobs, responseHeaders, HttpStatus.OK);
		}
		Messages.JOBS_FOR_ORDER_NOT_EXIST.log(logger, String.valueOf(orderId));
		String message = Messages.JOBS_FOR_ORDER_NOT_EXIST.formatWithPrefix(String.valueOf(orderId));
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	
	@Transactional
	@Override
    public ResponseEntity<RestJob> getJob(String jobId) {
		Job job = this.findJobById(jobId);
		if (job != null) {
			RestJob rj = RestUtil.createRestJob(job);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), Messages.OK.getDescription());
			return new ResponseEntity<>(rj, responseHeaders, HttpStatus.OK);
		}
		Messages.JOB_NOT_EXIST.log(logger, jobId);
		String message = Messages.JOB_NOT_EXIST.formatWithPrefix(jobId);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Transactional
	@Override 
	public ResponseEntity<RestJob> resumeJob(String jobId) {
		Job job = this.findJobById(jobId);
		if (job != null) {
			Messages msg = jobUtil.resume(job);
			if (msg.isTrue()) {
				if (job.getProcessingFacility() != null) {
					KubeConfig kc = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
					if (kc != null) {
						UtilService.getJobStepUtil().checkJobToRun(kc, job);
					}
				}
				RestJob rj = RestUtil.createRestJob(job);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(jobId));
				return new ResponseEntity<>(rj, responseHeaders, HttpStatus.OK);
			} else {
				RestJob rj = RestUtil.createRestJob(job);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(jobId));
				return new ResponseEntity<>(rj, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		Messages.JOB_NOT_EXIST.log(logger, jobId);
		String message = Messages.JOB_NOT_EXIST.formatWithPrefix(jobId);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	
	@Transactional
	@Override 
	public ResponseEntity<RestJob> cancelJob(String jobId){
		Job job = this.findJobById(jobId);
		if (job != null) {
			Messages msg = jobUtil.cancel(job);
			if (msg.isTrue()) {
				RestJob rj = RestUtil.createRestJob(job);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(jobId));
				return new ResponseEntity<>(rj, responseHeaders, HttpStatus.OK);
			} else {
				RestJob rj = RestUtil.createRestJob(job);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(jobId));
				return new ResponseEntity<>(rj, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		Messages.JOB_NOT_EXIST.log(logger, jobId);
		String message = Messages.JOB_NOT_EXIST.formatWithPrefix(jobId);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Transactional
	@Override 
	public ResponseEntity<RestJob> suspendJob(String jobId, Boolean force) {
		Job j = this.findJobById(jobId);
		if (j != null) {
			Messages msg = jobUtil.suspend(j, force);
			if (msg.isTrue()) {
				RestJob pj = RestUtil.createRestJob(j);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(jobId));
				return new ResponseEntity<>(pj, responseHeaders, HttpStatus.OK);
			} else {
				RestJob pj = RestUtil.createRestJob(j);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(jobId));
				return new ResponseEntity<>(pj, responseHeaders, HttpStatus.OK);
			}
		}
		Messages.JOB_NOT_EXIST.log(logger, jobId);
		String message = Messages.JOB_NOT_EXIST.formatWithPrefix(jobId);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
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

	@Transactional
	@Override
	public ResponseEntity<RestJob> retryJob(String id) {
		Job j = this.findJobById(id);
		if (j != null) {
			Messages msg = jobUtil.retry(j);
			if (msg.isTrue()) {
				RestJob pj = RestUtil.createRestJob(j);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(id));
				return new ResponseEntity<>(pj, responseHeaders, HttpStatus.OK);
			} else {
				RestJob pj = RestUtil.createRestJob(j);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(id));
				return new ResponseEntity<>(pj, responseHeaders, HttpStatus.OK);
			}
		}
		Messages.JOB_NOT_EXIST.log(logger, id);
		String message = Messages.JOB_NOT_EXIST.formatWithPrefix(id);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	
}
