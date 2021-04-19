/**
 * JobControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.rest.JobController;
import de.dlr.proseo.model.rest.model.RestJob;
import de.dlr.proseo.model.rest.model.RestJobGraph;
import de.dlr.proseo.model.rest.model.RestOrbit;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.rest.model.RestUtil;
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
	
	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(JobControllerImpl.class);
	
	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;

    @Autowired
    private JobUtil jobUtil;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

    
    /**
     * Get production planner jobs by order id
     * 
	 * @param state state of jobs
	 * @param orderId order id of jobs
	 * @param recordFrom first record of filtered and ordered result to return
	 * @param recordTo last record of filtered and ordered result to return
	 * @param orderBy an array of strings containing a column name and an optional sort direction (ASC/DESC), separated by white space
	 * @return a list of jobs
     */
	@Transactional
	@Override
    public ResponseEntity<List<RestJob>> getJobs(String state, Long orderId,
			Long recordFrom, Long recordTo, String[] orderBy) {

		List<RestJob> resultList = new ArrayList<>();
		// Find using search parameters
		// if job is set, look for page containing it
//		if (jobId != null && jobId > 0) {
//			Boolean found = false;
//			Long pageSize = recordTo - recordFrom;
//			Long from = (long) 0;
//			Long to = pageSize;
//			List<Job> jobList = new ArrayList<Job>();	
//			while (found == false) {
//				jobList.clear();
//				Query query = createJobsQuery(state, orderId, from, to, orderBy, false);
//				for (Object resultObject: query.getResultList()) {
//					if (resultObject instanceof Job) {
//						// Filter depending on product visibility and user authorization
//						Job job = (Job) resultObject;			
//						jobList.add(job);
//						if (job.getId() == jobId) {
//							found = true;
//						}
//					}
//				}	
//				if (jobList.isEmpty()) {
//					// no page found, use original page
//					break;
//				}
//			}
//			if (found) {
//				for (Job job: jobList) {			
//					resultList.add(RestUtil.createRestJob(job));
//				}	
//			}
//		} 
		if (resultList.isEmpty()) {
			Query query = createJobsQuery(state, orderId, recordFrom, recordTo, orderBy, false);
			
			for (Object resultObject: query.getResultList()) {
				if (resultObject instanceof Job) {
					// Filter depending on product visibility and user authorization
					Job job = (Job) resultObject;					
					resultList.add(RestUtil.createRestJob(job));
				}
			}		
		}
		if (resultList.isEmpty()) {
			Messages.JOBS_FOR_ORDER_NOT_EXIST.log(logger, String.valueOf(orderId));
			String message = Messages.JOBS_FOR_ORDER_NOT_EXIST.formatWithPrefix(String.valueOf(orderId));
	    	HttpHeaders responseHeaders = new HttpHeaders();
	    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), Messages.OK.getDescription());
		return new ResponseEntity<>(resultList, responseHeaders, HttpStatus.OK);
	}

    /**
     * Get production planner number of jobs by order id and state
     * 
	 * @param state state of jobs
	 * @param orderId order id of jobs
	 * @return number of jobs
     */
	@Transactional
	@Override
    public ResponseEntity<String> countJobs(String state, Long orderId) {

		List<RestJob> resultList = new ArrayList<>();

		// Find using search parameters
		Query query = createJobsQuery(state, orderId, null, null, null, true);
		Object resultObject = query.getSingleResult();
		if (resultObject instanceof Long) {
			return new ResponseEntity<>(((Long)resultObject).toString(), HttpStatus.OK);	
		}
		if (resultObject instanceof String) {
			return new ResponseEntity<>((String) resultObject, HttpStatus.OK);	
		}
		return new ResponseEntity<>("0", HttpStatus.OK);	
	}
	
	/* 
	 * Get a job by job id
	 * 
	 * @param jobId
	 */
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
	
	/* 
	 * Get a job by job id
	 * 
	 * @param jobId
	 */
	@Transactional
	@Override
    public ResponseEntity<RestJobGraph> graphJobSteps(String jobId) {
		Job job = this.findJobById(jobId);
		if (job != null) {
			RestJobGraph rj = null;
			rj = RestUtil.createRestJobGraph(job);
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

	/* 
	 * Resume the job with job id
	 * 
	 * @param jobId
	 */
	@Transactional
	@Override 
	public ResponseEntity<RestJob> resumeJob(String jobId) {
		Job job = this.findJobById(jobId);
		if (job != null) {
			@SuppressWarnings("unchecked")
			ResponseEntity<RestJob> re = (ResponseEntity<RestJob>) productionPlanner.checkFacility(job.getProcessingFacility()); 
			if (re != null) {
				return re;
			}
			Messages msg = jobUtil.resume(job);
			if (msg.isTrue()) {
				UtilService.getOrderUtil().updateState(job.getProcessingOrder(), job.getJobState());
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

	/* 
	 * Cancel a job by job id
	 * 
	 * @param jobId
	 */
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

	/* 
	 * Suspend a job by job id
	 * 
	 * @param jobId
	 * @param force If true, kill job, otherweise wait until end of job
	 */
	@Transactional
	@Override 
	public ResponseEntity<RestJob> suspendJob(String jobId, Boolean force) {
		Job j = this.findJobById(jobId);
		if (j != null) {
			@SuppressWarnings("unchecked")
			ResponseEntity<RestJob> re = (ResponseEntity<RestJob>) productionPlanner.checkFacility(j.getProcessingFacility()); 
			if (re != null) {
				return re;
			}
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

	/**
	 * Find a job by an id string, the contents could be the job DB id or the job name
	 * @param idStr
	 * @return The job found or null
	 */
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
	
	/**
	 * Find jobs of an order with a state.
	 * 
	 * @param state Job state
	 * @param orderId Order id
	 * @return The jobs found
	 */
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

	/* 
	 * Retry a job by job id
	 * 
	 * @param jobId
	 */
	@Transactional
	@Override
	public ResponseEntity<RestJob> retryJob(String id) {
		Job j = this.findJobById(id);
		if (j != null) {
			Messages msg = jobUtil.retry(j);
			if (msg.isTrue()) {
				UtilService.getOrderUtil().updateState(j.getProcessingOrder(), j.getJobState());
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

	private Query createJobsQuery(String state, Long orderId,
			Long recordFrom, Long recordTo, String[] orderBy, Boolean count) {

		// Find using search parameters
		String jpqlQuery = null;
		String div = " where";
		if (count) {
			jpqlQuery = "select count(x) from Job x";
		} else {
			jpqlQuery = "select x from Job x";
		}
		if (null != state) {
			jpqlQuery += div + " x.jobState = :state";
			div = " and";
		}
		if (null != orderId) {
			jpqlQuery += div + " x.processingOrder.id = :orderId";
			div = " and";
		}
		// order by
		if (null != orderBy && 0 < orderBy.length) {
			jpqlQuery += " order by ";
			for (int i = 0; i < orderBy.length; ++i) {
				if (0 < i) jpqlQuery += ", ";
				jpqlQuery += "x.";
				jpqlQuery += orderBy[i];
			}
		}

		Query query = em.createQuery(jpqlQuery);

		if (null != state) {
			query.setParameter("state",state);
		}
		if (null != orderId) {
			query.setParameter("orderId", orderId);
		}
		
		// length of record list
		if (recordFrom != null && recordFrom >= 0) {
			query.setFirstResult(recordFrom.intValue());
		}
		if (recordTo != null && recordTo >= 0) {
			query.setMaxResults(recordTo.intValue() - recordFrom.intValue());
		}
		return query;
	}
}
