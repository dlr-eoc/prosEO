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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.logging.messages.ProseoMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.rest.JobController;
import de.dlr.proseo.model.rest.model.RestJob;
import de.dlr.proseo.model.rest.model.RestJobGraph;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
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
	private static ProseoLogger logger = new ProseoLogger(JobControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PLANNER);
	
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
				String message = logger.log(PlannerMessage.JOBS_FOR_ORDER_NOT_EXIST, String.valueOf(orderId));

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			}
			
			logger.log(PlannerMessage.JOBS_RETRIEVED, orderId);

			return new ResponseEntity<>(resultList, HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
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

			logger.log(PlannerMessage.JOBCOUNT_RETRIEVED, orderId);

			if (resultObject instanceof Long) {
				return new ResponseEntity<>(((Long)resultObject).toString(), HttpStatus.OK);	
			}
			if (resultObject instanceof String) {
				return new ResponseEntity<>((String) resultObject, HttpStatus.OK);	
			}
			return new ResponseEntity<>("0", HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
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
				RestJob rj = getRestJob(job.getId(), true);

				logger.log(PlannerMessage.JOB_RETRIEVED, jobId);

				return new ResponseEntity<>(rj, HttpStatus.OK);
			}
			String message = logger.log(PlannerMessage.JOB_NOT_EXIST, jobId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
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

				logger.log(PlannerMessage.JOBGRAPH_RETRIEVED, jobId);

				return new ResponseEntity<>(rj, HttpStatus.OK);
			}
			String message = logger.log(PlannerMessage.JOB_NOT_EXIST, jobId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/* 
	 * Resume the job with job id
	 * 
	 * @param jobId
	 */
	@Override 
	public ResponseEntity<RestJob> resumeJob(String jobId) {
		if (logger.isTraceEnabled()) logger.trace(">>> resumeJob({})", jobId);
		
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());

			Job job = this.findJobById(jobId);
			if (job != null) {

				ProseoMessage msg = null;
				try {
					productionPlanner.acquireThreadSemaphore("resumeJob");
					final ResponseEntity<RestJob> answer = transactionTemplate.execute((status) -> {
						Job jobx = this.findJobByIdPrim(jobId);
						if (jobx.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
							String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, jobx.getProcessingFacility().getName(),
									jobx.getProcessingFacility().getFacilityState().toString());

							return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
						}
						jobUtil.resume(jobx);
						return null;
					});
					if (answer != null) {
						productionPlanner.releaseThreadSemaphore("resumeJob");	
						return answer;
					}

					productionPlanner.releaseThreadSemaphore("resumeJob");	
				} catch (Exception e) {
					productionPlanner.releaseThreadSemaphore("resumeJob");	
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				if (msg.getSuccess()) {
					final KubeConfig kc = transactionTemplate.execute((status) -> {
						if (job.getProcessingFacility() != null) {
							return productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
						} else {
							return null;
						}
					});
					if (kc != null) {
						try {
							productionPlanner.acquireThreadSemaphore("resumeJob2");
							UtilService.getJobStepUtil().checkJobToRun(kc, job.getId());
							productionPlanner.releaseThreadSemaphore("resumeJob2");	
						} catch (Exception e) {
							productionPlanner.releaseThreadSemaphore("resumeJob2");	
							String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
							return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
						}
					}
					RestJob rj = getRestJob(job.getId(), false);
					return new ResponseEntity<>(rj, HttpStatus.OK);
				} else {
					String message = logger.log(msg, jobId);
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOB_NOT_EXIST, jobId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/* 
	 * Cancel a job by job id
	 * 
	 * @param jobId
	 */
	@Override 
	public ResponseEntity<RestJob> cancelJob(String jobId){
		if (logger.isTraceEnabled()) logger.trace(">>> cancelJob({})", jobId);
		
		try {
			Job job = this.findJobById(jobId);
			if (job != null) {
				ProseoMessage msg = null;
				try {
					productionPlanner.acquireThreadSemaphore("cancelJob");
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					msg = transactionTemplate.execute((status) -> {
						Job jobx = this.findJobByIdPrim(jobId);
						return jobUtil.cancel(jobx);
					});
					productionPlanner.releaseThreadSemaphore("cancelJob");	
				} catch (Exception e) {
					productionPlanner.releaseThreadSemaphore("cancelJob");	
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				if (msg.getSuccess()) {
					RestJob rj = getRestJob(job.getId(), false);

					return new ResponseEntity<>(rj, HttpStatus.OK);
				} else {
					String message = logger.log(msg, jobId);
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOB_NOT_EXIST, jobId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/* 
	 * Suspend a job by job id
	 * 
	 * @param jobId
	 * @param force If true, kill job, otherweise wait until end of job
	 */
	@Override 
	public ResponseEntity<RestJob> suspendJob(String jobId, Boolean forceP) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspendJob({}, force: {})", jobId, forceP);
		
		final Boolean force = (null == forceP ? false : forceP);
		
		try {
			Job job = this.findJobById(jobId);
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			if (job != null) {

				ProseoMessage msg = null;
				try {
					productionPlanner.acquireThreadSemaphore("suspendJob");
					final ResponseEntity<RestJob> answer = transactionTemplate.execute((status) -> {
						Job jobx = this.findJobByIdPrim(jobId);
						if (jobx.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
							// "Suspend force" is only allowed, if the processing facility is available
							if (force && jobx.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
								String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, jobx.getProcessingFacility().getName(),
										jobx.getProcessingFacility().getFacilityState().toString());
						    	return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
							}
						}
						return null;
					});
					if (answer != null) {
						productionPlanner.releaseThreadSemaphore("suspendJob");	
						return answer;
					}

					productionPlanner.releaseThreadSemaphore("suspendJob");	
				} catch (Exception e) {
					productionPlanner.releaseThreadSemaphore("suspendJob");	
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				try {
					productionPlanner.acquireThreadSemaphore("suspendJob");
					msg = transactionTemplate.execute((status) -> {
						Job jobx = this.findJobByIdPrim(jobId);
						return jobUtil.suspend(jobx, force);
					});
					productionPlanner.releaseThreadSemaphore("suspendJob");	
				} catch (Exception e) {
					productionPlanner.releaseThreadSemaphore("suspendJob");	
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				if (msg.getSuccess()) {
					RestJob pj = getRestJob(job.getId(), false);

					return new ResponseEntity<>(pj, HttpStatus.OK);
				} else {
					String message = logger.log(msg, jobId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_ACCEPTABLE);
				}
			}
			String message = logger.log(PlannerMessage.JOB_NOT_EXIST, jobId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Find a job by an id string, the contents could be the job DB id or the job name
	 * @param idStr
	 * @return The job found or null
	 */
	private Job findJobByIdPrim(String idStr) {
		if (logger.isTraceEnabled()) logger.trace(">>> findJobByIdPrim({})", idStr);
		
		Job job = null;
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		job = transactionTemplate.execute((status) -> {
			Job jobx = null;
			Long id = null;
			if (idStr != null) {
				if (idStr.matches("[0-9]+")) {
					id = Long.valueOf(idStr);
				}
				if (id != null) {
					Optional<Job> jo = RepositoryService.getJobRepository().findById(id);
					if (jo.isPresent()) {
						jobx = jo.get();
					}
				}
			}

			if (null != jobx) {
				// Ensure user is authorized for the mission of the job
				String missionCode = securityService.getMission();
				String orderMissionCode = jobx.getProcessingOrder().getMission().getCode();
				if (!missionCode.equals(orderMissionCode)) {
					logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, orderMissionCode, missionCode);
					return null;
				} 
			}
			return jobx;
		});
		return job;
	}

	private Job findJobById(String nameOrId) {
		if (logger.isTraceEnabled()) logger.trace(">>> findJobById({})", nameOrId);
		Job j = null;
		try {
			productionPlanner.acquireThreadSemaphore("findJobById");
			j = this.findJobByIdPrim(nameOrId);
			productionPlanner.releaseThreadSemaphore("findJobById");	
		} catch (Exception e) {
			productionPlanner.releaseThreadSemaphore("findJobById");	
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());	
		}
		return j;
	}

	private RestJob getRestJob(long id, Boolean logs) {
		RestJob answer = null;
		try {
			productionPlanner.acquireThreadSemaphore("getRestJob");
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			answer = transactionTemplate.execute((status) -> {
				RestJob rj = null;
				Job job = null;
				Optional<Job> opt = RepositoryService.getJobRepository().findById(id);
				if (opt.isPresent()) {
					job = opt.get();
					rj = RestUtil.createRestJob(job, logs);
				}
				return rj;
			});
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());	
		} finally {
			productionPlanner.releaseThreadSemaphore("getRestJob");
		}
		return answer;
	}
	/* 
	 * Retry a job by job id
	 * 
	 * @param jobId
	 */
	@Override
	public ResponseEntity<RestJob> retryJob(String id) {
		if (logger.isTraceEnabled()) logger.trace(">>> retryJob({})", id);
		
		try {
			Job j = this.findJobById(id);
			if (j != null) {
				ProseoMessage msg = null;
				try {
					productionPlanner.acquireThreadSemaphore("retryJob");
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					msg = transactionTemplate.execute((status) -> {
						Job jobx = this.findJobByIdPrim(id);
						return jobUtil.retry(jobx);
					});
					productionPlanner.releaseThreadSemaphore("retryJob");	
				} catch (Exception e) {
					productionPlanner.releaseThreadSemaphore("retryJob");	
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				if (msg.getSuccess()) {
					RestJob rj = getRestJob(j.getId(), false);

					return new ResponseEntity<>(rj, HttpStatus.OK);
				} else {
					String message = logger.log(msg, id);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOB_NOT_EXIST, id);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
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
			query.setParameter("state", JobState.valueOf(state));
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
