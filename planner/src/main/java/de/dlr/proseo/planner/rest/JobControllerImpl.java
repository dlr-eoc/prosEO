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
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.planner.PlannerResultMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.rest.JobController;
import de.dlr.proseo.model.rest.model.RestJob;
import de.dlr.proseo.model.rest.model.RestJobGraph;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.rest.model.RestUtil;
import de.dlr.proseo.planner.util.JobUtil;
import de.dlr.proseo.planner.util.UtilService;

/**
 * Spring MVC controller for the prosEO planner; implements the services required to plan and handle jobs.
 * 
 * This class manages the endpoints for job management, including retrieval, creation, modification, and cancellation of jobs.
 * 
 * @author Ernst Melchinger
 */
@Component
public class JobControllerImpl implements JobController {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(JobControllerImpl.class);

	/** Utility class for handling HTTP headers */
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PLANNER);

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** The production planner instance */
	@Autowired
	private ProductionPlanner productionPlanner;

	/** Utility class for handling jobs */
	@Autowired
	private JobUtil jobUtil;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/**
	 * Retrieves a list of jobs based on provided filtering criteria.
	 * 
	 * @param state       The job state to filter by (optional).
	 * @param orderId     The order ID to filter by (optional).
	 * @param recordFrom  First record of filtered and ordered result to return (optional; mandatory if "recordTo" is given).
	 * @param recordTo    Last record of filtered and ordered result to return (optional).
	 * @param logs        Indicates whether to include logs in the response.
	 * @param orderBy     An array of strings containing a column name and an optional sort direction (ASC/DESC), separated by white
	 *                    space.
	 * @param httpHeaders HTTP headers.
	 * @return A ResponseEntity containing a list of JSON objects describing jobs.
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	@Override
	public ResponseEntity<List<RestJob>> getJobs(String state, Long orderId, Long recordFrom, Long recordTo, Boolean logs,
			String[] orderBy, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getJobs({}, {}, {}, {}, {}, {})", state, orderId, recordFrom, recordTo,
					(null == orderBy ? "null" : Arrays.asList(orderBy)));

		try {
			if (logs == null) {
				logs = true;
			}
			List<RestJob> resultList = new ArrayList<>();

			// Create a JPQL query to fetch jobs based on provided criteria
			Query query = createJobsQuery(state, orderId, recordFrom, recordTo, orderBy, false);

			for (Object resultObject : query.getResultList()) {
				if (resultObject instanceof Job) {
					Job job = (Job) resultObject;
					resultList.add(RestUtil.createRestJob(job, logs));
				}
			}

			// If no jobs were found for the provided criteria, log and return HTTP 404
			if (resultList.isEmpty()) {
				String message = logger.log(PlannerMessage.JOBS_FOR_ORDER_NOT_EXIST, String.valueOf(orderId));

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			}

			// Else, log and return HTTP 200
			logger.log(PlannerMessage.JOBS_RETRIEVED, orderId);

			return new ResponseEntity<>(resultList, HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Retrieves the number of jobs by order ID and state.
	 * 
	 * @param state       State of jobs.
	 * @param orderId     Order ID of jobs.
	 * @param httpHeaders HTTP headers.
	 * @return The number of jobs as a string.
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	@Override
	public ResponseEntity<String> countJobs(String state, Long orderId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countJobs({}, {})", state, orderId);

		try {
			// Retrieve job count based on search parameters
			Query query = createJobsQuery(state, orderId, null, null, null, true);
			Object resultObject = query.getSingleResult();

			logger.log(PlannerMessage.JOBCOUNT_RETRIEVED, orderId);

			// Check the type of the result object and return an appropriate response entity
			if (resultObject instanceof Long) {
				return new ResponseEntity<>(((Long) resultObject).toString(), HttpStatus.OK);
			}
			if (resultObject instanceof String) {
				return new ResponseEntity<>((String) resultObject, HttpStatus.OK);
			}

			// Else, return a result of zero
			return new ResponseEntity<>("0", HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Get a job by job id
	 * 
	 * @param jobId
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	@Override
	public ResponseEntity<RestJob> getJob(String jobId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getJob({})", jobId);

		try {
			Job job = this.findJobById(jobId);

			// If the job exists, return the job and HTTP 200
			if (job != null) {
				RestJob restJob = getRestJob(job.getId(), true);

				logger.log(PlannerMessage.JOB_RETRIEVED, jobId);

				return new ResponseEntity<>(restJob, HttpStatus.OK);
			}

			// If the job was not found, log and return HTTP 404
			String message = logger.log(PlannerMessage.JOB_NOT_EXIST, jobId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Create a graph of job step dependencies within a job
	 * 
	 * @param jobId
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	@Override
	public ResponseEntity<RestJobGraph> graphJobSteps(String jobId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> graphJobSteps({})", jobId);

		try {
			Job job = this.findJobById(jobId);

			// If found, create and return the graph for the job with HTTP 200
			if (job != null) {
				RestJobGraph restJob = null;
				restJob = RestUtil.createRestJobGraph(job);

				logger.log(PlannerMessage.JOBGRAPH_RETRIEVED, jobId);

				return new ResponseEntity<>(restJob, HttpStatus.OK);
			}

			// If the job was not found, log and return HTTP 404
			String message = logger.log(PlannerMessage.JOB_NOT_EXIST, jobId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Resume the job with job id
	 * 
	 * @param jobId
	 */
	@Override
	public ResponseEntity<RestJob> resumeJob(String jobId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> resumeJob({})", jobId);

		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

			Job job = this.findJobById(jobId);
			// Check the status of the requested processing facility
			final ResponseEntity<RestJob> response = transactionTemplate.execute((status) -> {

				// Get the processing facility associated with the job and update its Kubernetes configuration
				ProcessingFacility processingFacility = job.getProcessingFacility();
				KubeConfig kubeConfig = productionPlanner.updateKubeConfig(processingFacility.getName());

				// Check if the Kubernetes configuration is null, indicating the non-existence of the facility
				if (null == kubeConfig) {
					String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, processingFacility.getName());

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
				}

				// Check the state of the processing facility
				if (processingFacility.getFacilityState() != FacilityState.RUNNING && processingFacility.getFacilityState() != FacilityState.STARTING) {
					String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, processingFacility.getName(),
							processingFacility.getFacilityState().toString());
					if (processingFacility.getFacilityState() == FacilityState.DISABLED) {
						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
					} else {
						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.SERVICE_UNAVAILABLE);
					}
				}
				return null;
			});

			// If an error occurred, return
			if (response != null) {
				return response;
			}
			if (job != null) {

				PlannerResultMessage msg = new PlannerResultMessage(null);
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						final ResponseEntity<RestJob> answer = transactionTemplate.execute((status) -> {
							Job jobx = this.findJobByIdPrim(jobId);

							// Check if the processing facility associated with the job is running
							if (jobx.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
								String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE,
										jobx.getProcessingFacility().getName(),
										jobx.getProcessingFacility().getFacilityState().toString());

								return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
							}
							jobUtil.resume(jobx);
							return null;
						});

						// If an error occurred, return
						if (answer != null) {
							return answer;
						}

						break;
					} catch (CannotAcquireLockException e) {
						// Handle database concurrency issues
						if (logger.isDebugEnabled())
							logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled())
								logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					} catch (Exception e) {
						String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

						if (logger.isDebugEnabled())
							logger.debug("... exception stack trace: ", e);

						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}

				// Check the success status of the planner message
				if (msg.getSuccess()) {
					final KubeConfig kubeConfig = transactionTemplate.execute((status) -> {
						if (job.getProcessingFacility() != null) {
							// Get the Kubernetes configuration for the processing facility
							return productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
						} else {
							return null;
						}
					});
					if (kubeConfig != null) {
						try {
							// Check for unsatisfied job step queries
							UtilService.getJobStepUtil().checkJobToRun(kubeConfig, job.getId());
						} catch (Exception e) {
							String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

							if (logger.isDebugEnabled())
								logger.debug("... exception stack trace: ", e);

							return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
						}
					}

					// Return the rest job and HTTP status 200
					RestJob restJob = getRestJob(job.getId(), false);
					return new ResponseEntity<>(restJob, HttpStatus.OK);
				} else {
					String message = logger.log(msg.getMessage(), jobId);
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOB_NOT_EXIST, jobId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Cancel a job by job id
	 * 
	 * @param jobId
	 */
	@Override
	public ResponseEntity<RestJob> cancelJob(String jobId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> cancelJob({})", jobId);

		try {
			// Find the job by its ID
			Job job = this.findJobById(jobId);

			if (job != null) {
				PlannerResultMessage msg = new PlannerResultMessage(null);
				try {
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							// Execute a transaction to cancel the job
							msg = transactionTemplate.execute((status) -> {
								Job jobx = this.findJobByIdPrim(jobId);
								return jobUtil.cancel(jobx);
							});
							break;
						} catch (CannotAcquireLockException e) {
							// Handle database concurrency issues
							if (logger.isDebugEnabled())
								logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled())
									logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}
				} catch (Exception e) {
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				if (msg.getSuccess()) {
					// If the cancellation was successful, return the job and HTTP 200
					RestJob restJob = getRestJob(job.getId(), false);

					return new ResponseEntity<>(restJob, HttpStatus.OK);
				} else {
					// Else, log and return HTTP 400
					String message = logger.log(msg.getMessage(), jobId);
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			// If the job does not exist, return a 404 Not Found response
			String message = logger.log(PlannerMessage.JOB_NOT_EXIST, jobId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Suspend a job by job id
	 * 
	 * @param jobId
	 * @param force If true, kill job, otherwise wait until end of job
	 */
	@Override
	public ResponseEntity<RestJob> suspendJob(String jobId, Boolean forceP, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> suspendJob({}, force: {})", jobId, forceP);

		final Boolean force = (null == forceP ? false : forceP);

		try {
			Job job = this.findJobById(jobId);
			// Check the status of the requested processing facility
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
			final ResponseEntity<RestJob> response = transactionTemplate.execute((status) -> {
				// Check the status of the processing facility associated with the job
				ProcessingFacility processingFacility = job.getProcessingFacility();
				KubeConfig kubeConfig = productionPlanner.updateKubeConfig(processingFacility.getName());

				// If the processing facility does not exist, return HTTP 404
				if (null == kubeConfig) {
					String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, processingFacility.getName());

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
				}

				// If the processing facility is not in a runnable state, return an appropriate HTTP status
				if (processingFacility.getFacilityState() != FacilityState.RUNNING && processingFacility.getFacilityState() != FacilityState.STARTING) {
					String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, processingFacility.getName(),
							processingFacility.getFacilityState().toString());
					if (processingFacility.getFacilityState() == FacilityState.DISABLED) {
						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
					} else {
						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.SERVICE_UNAVAILABLE);
					}
				}
				return null;
			});

			// If an error occurred, return
			if (response != null) {
				return response;
			}
			if (job != null) {

				PlannerResultMessage msg = new PlannerResultMessage(null);
				try {
					final ResponseEntity<RestJob> answer = transactionTemplate.execute((status) -> {
						Job jobx = this.findJobByIdPrim(jobId);
						if (jobx.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
							// "Suspend force" is only allowed if the processing facility is available
							if (force && jobx.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
								String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE,
										jobx.getProcessingFacility().getName(),
										jobx.getProcessingFacility().getFacilityState().toString());
								return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
							}
						}
						return null;
					});
					if (answer != null) {
						return answer;
					}

				} catch (Exception e) {
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						msg = transactionTemplate.execute((status) -> {
							Job jobx = this.findJobByIdPrim(jobId);
							return jobUtil.suspend(jobx, force);
						});
						break;
					} catch (CannotAcquireLockException e) {
						// Handle database concurrency issues
						if (logger.isDebugEnabled())
							logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled())
								logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					} catch (Exception e) {
						String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

						if (logger.isDebugEnabled())
							logger.debug("... exception stack trace: ", e);

						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}
				if (msg.getSuccess()) {
					// If the suspension was successful, return the job and HTTP 200
					RestJob restJob = getRestJob(job.getId(), false);

					return new ResponseEntity<>(restJob, HttpStatus.OK);
				} else {
					// Else, log and return HTTP 406
					String message = logger.log(msg.getMessage(), jobId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_ACCEPTABLE);
				}
			}
			String message = logger.log(PlannerMessage.JOB_NOT_EXIST, jobId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Find a job by an ID string (database id or job name).
	 * 
	 * @param idStr The ID string.
	 * @return The job found, or null if not found.
	 */
	private Job findJobByIdPrim(String idStr) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findJobByIdPrim({})", idStr);

		Job job = null;
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		job = transactionTemplate.execute((status) -> {
			Job jobx = null;
			Long id = null;

			// Parse the ID string to Long if it's not null and numeric
			if (idStr != null) {
				if (idStr.matches("[0-9]+")) {
					id = Long.valueOf(idStr);
				}

				// If ID is successfully parsed, retrieve the job from the repository
				if (id != null) {
					Optional<Job> opt = RepositoryService.getJobRepository().findById(id);
					if (opt.isPresent()) {
						jobx = opt.get();
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

			// Return the retrieved job
			return jobx;
		});
		return job;
	}

	/**
	 * Find a job by an ID string.
	 * 
	 * @param nameOrId The name or ID string.
	 * @return The job found, or null if not found.
	 */
	private Job findJobById(String nameOrId) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findJobById({})", nameOrId);
		Job j = null;
		try {
			j = this.findJobByIdPrim(nameOrId);
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);
		}
		return j;
	}

	/**
	 * Get a REST representation of a job.
	 * 
	 * @param id   The ID of the job.
	 * @param logs Indicates whether to include logs in the representation.
	 * @return A REST representation of the job.
	 */
	private RestJob getRestJob(long id, Boolean logs) {
		RestJob answer = null;
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
			answer = transactionTemplate.execute((status) -> {
				RestJob restJob = null;
				Job job = null;
				Optional<Job> opt = RepositoryService.getJobRepository().findById(id);
				if (opt.isPresent()) {
					job = opt.get();
					restJob = RestUtil.createRestJob(job, logs);
				}

				// Return the job, if present
				return restJob;
			});
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);
		}
		return answer;
	}

	/**
	 * Retry a job by job id
	 * 
	 * @param jobId
	 */
	@Override
	public ResponseEntity<RestJob> retryJob(String id, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> retryJob({})", id);

		try {
			Job j = this.findJobById(id);
			if (j != null) {
				PlannerResultMessage msg = new PlannerResultMessage(null);
				try {
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							msg = transactionTemplate.execute((status) -> {
								// Find the job by its ID
								Job jobx = this.findJobByIdPrim(id);
								return jobUtil.retry(jobx);
							});
							break;
						} catch (CannotAcquireLockException e) {
							// Handle database concurrency issue
							if (logger.isDebugEnabled())
								logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled())
									logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}
				} catch (Exception e) {
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				if (msg.getSuccess()) {
					// If successful, retrieve the updated job details and return them and HTTP 200
					RestJob restJob = getRestJob(j.getId(), false);

					return new ResponseEntity<>(restJob, HttpStatus.OK);
				} else {
					// Else, log and return HTTP 400
					String message = logger.log(msg.getMessage(), id);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOB_NOT_EXIST, id);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Create a JPQL query for jobs, filtering by the mission the user is logged in to, and optionally by job state and/or order ID.
	 * 
	 * @param state      The job state to filter by (optional).
	 * @param orderId    The order ID to filter by (optional).
	 * @param recordFrom First record of filtered and ordered result to return.
	 * @param recordTo   Last record of filtered and ordered result to return.
	 * @param orderBy    An array of strings containing a column name and an optional sort direction (ASC/DESC), separated by white
	 *                   space.
	 * @param count      Indicates whether just a count of the orders shall be retrieved or the orders as such.
	 * @return A JPQL query object.
	 */
	private Query createJobsQuery(String state, Long orderId, Long recordFrom, Long recordTo, String[] orderBy, Boolean count) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJobsQuery({}, {}, {}, {}, {}, {}, count: {})", state, orderId, recordFrom, recordTo,
					(null == orderBy ? "null" : Arrays.asList(orderBy)), count);

		// Construct JPQL query based on search parameters
		String jpqlQuery = null;

		// If counting is desired, count jobs, else select all
		if (count) {
			jpqlQuery = "select count(x) from Job x";
		} else {
			jpqlQuery = "select x from Job x";
		}
		// Filter by mission code
		jpqlQuery += " where x.processingOrder.mission.code = :missionCode";

		// Filter by job state and order id if desired
		if (null != state) {
			jpqlQuery += " and x.jobState = :state";
		}
		if (null != orderId) {
			jpqlQuery += " and x.processingOrder.id = :orderId";
		}

		// Specify order if desired
		if (null != orderBy && 0 < orderBy.length) {
			jpqlQuery += " order by ";
			for (int i = 0; i < orderBy.length; ++i) {
				if (0 < i)
					jpqlQuery += ", ";
				jpqlQuery += "x.";
				jpqlQuery += orderBy[i];
			}
		}

		// Create a query object from the constructed JPQL query
		Query query = em.createQuery(jpqlQuery);
		query.setParameter("missionCode", securityService.getMission());
		if (null != state) {
			query.setParameter("state", JobState.valueOf(state));
		}
		if (null != orderId) {
			query.setParameter("orderId", orderId);
		}

		// Set pagination parameters for the record list
		if (recordFrom != null && recordFrom >= 0) {
			query.setFirstResult(recordFrom.intValue());
		}
		if (recordTo != null && recordTo >= 0) {
			query.setMaxResults(recordTo.intValue() - recordFrom.intValue());
		}
		
		return query;
	}
}