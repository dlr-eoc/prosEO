/**
 * JobControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.rest.JobController;
import de.dlr.proseo.model.rest.model.RestJob;
import de.dlr.proseo.model.rest.model.RestJobGraph;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
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
	
	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;

    @Autowired
    private JobUtil jobUtil;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

    
    /**
     * Get production planner jobs, optionally filtered by job state and/or order ID
     * 
	 * @param state the job state to filter by (optional)
	 * @param orderId the order ID to filter by (optional)
	 * @param recordFrom first record of filtered and ordered result to return (optional; mandatory if "recordTo" is given)
	 * @param recordTo last record of filtered and ordered result to return (optional)
	 * @param orderBy an array of strings containing a column name and an optional sort direction (ASC/DESC), separated by white space
	 * @return a list of JSON objects describing jobs
     */
	@Transactional
	@Override
    public ResponseEntity<List<RestJob>> getJobs(String state, Long orderId,
			Long recordFrom, Long recordTo, Boolean logs, String[] orderBy) {
		if (logger.isTraceEnabled()) logger.trace(">>> getJobs({}, {}, {}, {}, {}, {})",
				state, orderId, recordFrom, recordTo, (null == orderBy ? "null" : Arrays.asList(orderBy)));
		
		try {
			if (logs == null) {
				logs = true;
			}
			List<RestJob> resultList = new ArrayList<>();

			Query query = createJobsQuery(state, orderId, recordFrom, recordTo, orderBy, false);
			
			for (Object resultObject: query.getResultList()) {
				if (resultObject instanceof Job) {
					Job job = (Job) resultObject;					
					resultList.add(RestUtil.createRestJob(job, logs));
				}
			}		

			if (resultList.isEmpty()) {
				String message = Messages.JOBS_FOR_ORDER_NOT_EXIST.log(logger, String.valueOf(orderId));

				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			}
			
			Messages.JOBS_RETRIEVED.log(logger, orderId);

			return new ResponseEntity<>(resultList, HttpStatus.OK);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
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
		if (logger.isTraceEnabled()) logger.trace(">>> countJobs({}, {})", state, orderId);
		
		try {
			// Find using search parameters
			Query query = createJobsQuery(state, orderId, null, null, null, true);
			Object resultObject = query.getSingleResult();

			Messages.JOBCOUNT_RETRIEVED.log(logger, orderId);

			if (resultObject instanceof Long) {
				return new ResponseEntity<>(((Long)resultObject).toString(), HttpStatus.OK);	
			}
			if (resultObject instanceof String) {
				return new ResponseEntity<>((String) resultObject, HttpStatus.OK);	
			}
			return new ResponseEntity<>("0", HttpStatus.OK);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}	
	}
	
	/* 
	 * Get a job by job id
	 * 
	 * @param jobId
	 */
	@Transactional
	@Override
    public ResponseEntity<RestJob> getJob(String jobId) {
		if (logger.isTraceEnabled()) logger.trace(">>> getJob({})", jobId);
		
		try {
			Job job = this.findJobById(jobId);
			if (job != null) {
				RestJob rj = RestUtil.createRestJob(job, true);

				Messages.JOB_RETRIEVED.log(logger, jobId);

				return new ResponseEntity<>(rj, HttpStatus.OK);
			}
			String message = Messages.JOB_NOT_EXIST.log(logger, jobId);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/* 
	 * Create a graph of job step dependencies within a job
	 * 
	 * @param jobId
	 */
	@Transactional
	@Override
    public ResponseEntity<RestJobGraph> graphJobSteps(String jobId) {
		if (logger.isTraceEnabled()) logger.trace(">>> graphJobSteps({})", jobId);
		
		try {
			Job job = this.findJobById(jobId);
			if (job != null) {
				RestJobGraph rj = null;
				rj = RestUtil.createRestJobGraph(job);

				Messages.JOBGRAPH_RETRIEVED.log(logger, jobId);

				return new ResponseEntity<>(rj, HttpStatus.OK);
			}
			String message = Messages.JOB_NOT_EXIST.log(logger, jobId);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/* 
	 * Resume the job with job id
	 * 
	 * @param jobId
	 */
	@Transactional
	@Override 
	public ResponseEntity<RestJob> resumeJob(String jobId) {
		if (logger.isTraceEnabled()) logger.trace(">>> resumeJob({})", jobId);
		
		try {
			Job job = this.findJobById(jobId);
			if (job != null) {
				if (job.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
					String message = Messages.FACILITY_NOT_AVAILABLE.log(logger, job.getProcessingFacility().getName(),
							job.getProcessingFacility().getFacilityState().toString());

			    	return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}

				Messages msg = jobUtil.resume(job);
				// Already logged
				
				if (msg.isTrue()) {
					UtilService.getOrderUtil().updateState(job.getProcessingOrder(), job.getJobState());
					if (job.getProcessingFacility() != null) {
						KubeConfig kc = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
						if (kc != null) {
							UtilService.getJobStepUtil().checkJobToRun(kc, job);
						}
					}
					RestJob rj = RestUtil.createRestJob(job, false);

					return new ResponseEntity<>(rj, HttpStatus.OK);
				} else {
					String message = msg.format(jobId);

					return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = Messages.JOB_NOT_EXIST.log(logger, jobId);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/* 
	 * Cancel a job by job id
	 * 
	 * @param jobId
	 */
	@Transactional
	@Override 
	public ResponseEntity<RestJob> cancelJob(String jobId){
		if (logger.isTraceEnabled()) logger.trace(">>> cancelJob({})", jobId);
		
		try {
			Job job = this.findJobById(jobId);
			if (job != null) {
				Messages msg = jobUtil.cancel(job);
				if (msg.isTrue()) {
					UtilService.getOrderUtil().updateState(job.getProcessingOrder(), job.getJobState());
					RestJob rj = RestUtil.createRestJob(job, false);

					return new ResponseEntity<>(rj, HttpStatus.OK);
				} else {
					String message = msg.format(jobId);

					return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = Messages.JOB_NOT_EXIST.log(logger, jobId);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
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
		if (logger.isTraceEnabled()) logger.trace(">>> suspendJob({}, force: {})", jobId, force);
		
		if (null == force) {
			force = false;
		}
		
		try {
			Job job = this.findJobById(jobId);
			if (job != null) {
				
				// "Suspend force" is only allowed, if the processing facility is available
				if (force && job.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
					String message = Messages.FACILITY_NOT_AVAILABLE.log(logger, job.getProcessingFacility().getName(),
							job.getProcessingFacility().getFacilityState().toString());

			    	return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}

				Messages msg = jobUtil.suspend(job, force);
				// Already logged
				
				if (msg.isTrue()) {
					UtilService.getOrderUtil().updateState(job.getProcessingOrder(), job.getJobState());
					RestJob pj = RestUtil.createRestJob(job, false);

					return new ResponseEntity<>(pj, HttpStatus.OK);
				} else {
					String message = msg.format(jobId);

					return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.OK);
				}
			}
			String message = Messages.JOB_NOT_EXIST.log(logger, jobId);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Find a job by an id string, the contents could be the job DB id or the job name
	 * @param idStr
	 * @return The job found or null
	 */
	@Transactional
	private Job findJobById(String idStr) {
		if (logger.isTraceEnabled()) logger.trace(">>> findJobById({})", idStr);
		
		Job job = null;
		Long id = null;
		if (idStr != null) {
			if (idStr.matches("[0-9]+")) {
				id = Long.valueOf(idStr);
			}
			if (id != null) {
				Optional<Job> jo = RepositoryService.getJobRepository().findById(id);
				if (jo.isPresent()) {
					job = jo.get();
				}
			}
		}

		if (null != job) {
			// Ensure user is authorized for the mission of the job
			String missionCode = securityService.getMission();
			String orderMissionCode = job.getProcessingOrder().getMission().getCode();
			if (!missionCode.equals(orderMissionCode)) {
				Messages.ILLEGAL_CROSS_MISSION_ACCESS.log(logger, orderMissionCode, missionCode);
				return null;
			} 
		}
		return job;
	}
	
	/* 
	 * Retry a job by job id
	 * 
	 * @param jobId
	 */
	@Transactional
	@Override
	public ResponseEntity<RestJob> retryJob(String id) {
		if (logger.isTraceEnabled()) logger.trace(">>> retryJob({})", id);
		
		try {
			Job j = this.findJobById(id);
			if (j != null) {
				Messages msg = jobUtil.retry(j);
				// Already logged
				
				if (msg.isTrue()) {
					UtilService.getOrderUtil().updateState(j.getProcessingOrder(), j.getJobState());
					RestJob pj = RestUtil.createRestJob(j, false);

					return new ResponseEntity<>(pj, HttpStatus.OK);
				} else {
					String message = msg.format(id);

					return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = Messages.JOB_NOT_EXIST.log(logger, id);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Create a JPQL query for jobs, filtering by the mission the user is logged in to, and optionally by job state and/or order ID
	 * 
	 * @param state the job state to filter by (optional)
	 * @param orderId the order ID to filter by (optional)
	 * @param recordFrom first record of filtered and ordered result to return
	 * @param recordTo last record of filtered and ordered result to return
	 * @param orderBy an array of strings containing a column name and an optional sort direction (ASC/DESC), separated by white space
	 * @param count indicates whether just a count of the orders shall be retrieved or the orders as such
	 * @return a JPQL query object
	 */
	private Query createJobsQuery(String state, Long orderId,
			Long recordFrom, Long recordTo, String[] orderBy, Boolean count) {
		if (logger.isTraceEnabled()) logger.trace(">>> createJobsQuery({}, {}, {}, {}, {}, {}, count: {})",
				state, orderId, recordFrom, recordTo, (null == orderBy ? "null" : Arrays.asList(orderBy)), count);
		
		// Find using search parameters
		String jpqlQuery = null;
		if (count) {
			jpqlQuery = "select count(x) from Job x";
		} else {
			jpqlQuery = "select x from Job x";
		}
		jpqlQuery += " where x.processingOrder.mission.code = :missionCode";
		if (null != state) {
			jpqlQuery += " and x.jobState = :state";
		}
		if (null != orderId) {
			jpqlQuery += " and x.processingOrder.id = :orderId";
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

		query.setParameter("missionCode", securityService.getMission());
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
