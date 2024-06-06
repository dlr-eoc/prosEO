/**
 * JobstepControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.planner.PlannerResultMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.rest.JobstepController;
import de.dlr.proseo.model.rest.model.RestJobStep;
import de.dlr.proseo.model.rest.model.Status;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import de.dlr.proseo.planner.rest.model.RestUtil;
import de.dlr.proseo.planner.util.JobStepUtil;
import de.dlr.proseo.planner.util.UtilService;
import io.kubernetes.client.openapi.models.V1Pod;

/**
 * Spring MVC controller for the prosEO planner; implements the services required to plan and handle job steps.
 * 
 * @author Ernst Melchinger
 */
@Component
public class JobstepControllerImpl implements JobstepController {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(JobstepControllerImpl.class);

	/** Utility class for handling HTTP headers */
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PLANNER);

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** The Production Planner instance */
	@Autowired
	private ProductionPlanner productionPlanner;

	/** Utility class to handle job steps */
	@Autowired
	private JobStepUtil jobStepUtil;

	/**
	 * Retrieves a list of job steps based on the provided status, mission, and optional last parameter.
	 * 
	 * @param status      The status of the job steps to retrieve.
	 * @param mission     The mission code for which the job steps are retrieved. If null or blank, uses the mission from security
	 *                    context.
	 * @param last        An optional parameter indicating the index of the last job step retrieved.
	 * @param httpHeaders HttpHeaders object containing HTTP headers for the response.
	 * @return ResponseEntity containing a list of REST representations of the retrieved job steps.
	 */
	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public ResponseEntity<List<RestJobStep>> getJobSteps(Status status, String mission, Long last, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getJobSteps({}, {}, {})", status, mission, last);

		// If mission is null or blank, retrieve mission from security context
		if (null == mission || mission.isBlank()) {
			mission = securityService.getMission();
		} else if (!mission.equals(securityService.getMission())) {
			// Ensure that the requested mission matches the mission from security context to prevent cross-mission access
			String message = logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, mission, securityService.getMission());
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.FORBIDDEN);
		}

		try {
			List<RestJobStep> list = new ArrayList<RestJobStep>();
			List<JobStep> jobStepList;
			
			if (status == null || status.value().equalsIgnoreCase("NONE")) {
				// If status is null or "NONE", retrieve all job steps for the specified mission
				List<JobStep> allJobSteps = RepositoryService.getJobStepRepository().findAll();
				jobStepList = new ArrayList<>();
				
				for (JobStep jobStep : allJobSteps) {
					if (jobStep.getJob().getProcessingOrder().getMission().getCode().equals(mission)) {
						jobStepList.add(jobStep);
					}
				}
			} else {
				// If a specific status is provided, retrieve job steps with that status and optional last parameter
				JobStepState state = JobStepState.valueOf(status.toString());

				if (last != null && last > 0) {
					List<JobStep> jobStepListAll = jobStepUtil.findOrderedByJobStepStateAndMission(state, mission, last.intValue());
					
					if (last < jobStepListAll.size()) {
						jobStepList = jobStepListAll.subList(0, last.intValue());
					} else {
						jobStepList = jobStepListAll;
					}
				} else {
					// Retrieve job steps based on status and mission
					jobStepList = RepositoryService.getJobStepRepository()
						.findAllByJobStepStateAndMissionOrderByDate(state, mission);
				}
			}

			// Convert retrieved job steps to REST representations and add them to the response list
			for (JobStep jobStep : jobStepList) {
				RestJobStep restJobStep = RestUtil.createRestJobStep(jobStep, false);
				list.add(restJobStep);
			}

			logger.log(PlannerMessage.JOBSTEPS_RETRIEVED, status, mission);

			return new ResponseEntity<>(list, HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Get production planner job step identified by name or id
	 * 
	 * @param name        The name or id of the job step to retrieve
	 * @param httpHeaders HttpHeaders object containing HTTP headers for the response.
	 * @return ResponseEntity containing the RestJobStep object
	 */
	@Override
	public ResponseEntity<RestJobStep> getJobStep(String name, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getJobStep({})", name);

		try {
			// Find the job step by name or id
			JobStep jobStep = this.findJobStepByNameOrId(name);

			if (jobStep != null) {
				try {
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					transactionTemplate.execute((status) -> {
						// Re-fetch the job step within the transaction context
						JobStep jobStepX = this.findJobStepByNameOrIdPrim(name);
						Job job = jobStepX.getJob();

						// TODO Check whether loading/updating of log info is still necessary
						if (jobStepX.getJobStepState() == JobStepState.RUNNING && job != null) {
							// Update log information if the job step is running and associated with a job
							if (job.getProcessingFacility() != null) {
								KubeConfig kubeConfig = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
								if (kubeConfig != null) {
									KubeJob kubeJob = kubeConfig.getKubeJob(ProductionPlanner.jobNamePrefix + jobStepX.getId());
									if (kubeJob != null) {
										kubeJob.updateInfo(ProductionPlanner.jobNamePrefix + jobStepX.getId());
									}
								}
							}
						}
						return null;
					});
				} catch (Exception e) {
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}

				// Return the job step step in its REST representation
				RestJobStep restJobStep = getRestJobStep(jobStep.getId(), true);

				logger.log(PlannerMessage.JOBSTEP_RETRIEVED, name);

				return new ResponseEntity<>(restJobStep, HttpStatus.OK);
			}
			// Return a not found response if the job step does not exist
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, name);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	/**
	 * Get production planner job step log string by name or id
	 * 
	 * @param name        The name or id of the job step to retrieve the log for
	 * @param httpHeaders HttpHeaders object containing HTTP headers for the response.
	 * @return ResponseEntity containing the log string
	 */
	@Override
	public ResponseEntity<String> getJobStepLog(String name, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getJobStep({})", name);
		try {
			// Find the job step by name or id
			JobStep jobStep = this.findJobStepByNameOrIdPrim(name);
			if (jobStep != null) {
				String logx = null;
				try {
					// Retrieve the log within a transaction
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					transactionTemplate.setReadOnly(true);
					logx = transactionTemplate.execute((status) -> {
						String log = null;
						// Re-fetch the job step within the transaction context
						JobStep jobStepX = this.findJobStepByNameOrIdPrim(name);
						Job job = jobStepX.getJob();
						// If the job step is running and associated with a job, attempt to fetch logs from the processing facility
						if (jobStepX.getJobStepState() == JobStepState.RUNNING && job != null) {
							if (job.getProcessingFacility() != null) {
								KubeConfig kubeConfig = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
								if (kubeConfig != null && kubeConfig.isConnected()) {
									KubeJob kubeJob = kubeConfig.getKubeJob(ProductionPlanner.jobNamePrefix + jobStepX.getId());
									if (kubeJob != null) {
										// Fetch logs from the running job
										V1Pod aPod = kubeConfig
											.getV1Pod(kubeJob.getPodNames().get(kubeJob.getPodNames().size() - 1));
										log = kubeJob.getJobStepLogPrim(aPod);
									}
								}
							}
						} else {
							// If the job step is not running, retrieve logs from the stored processing stdout
							log = jobStepX.getProcessingStdOut();
						}
						return log;
					});
				} catch (Exception e) {
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}

				// Return an OK response with the retrieved log string
				if (logx == null) {
					return new ResponseEntity<>("", HttpStatus.OK);
				} else {
					return new ResponseEntity<>(logx, HttpStatus.OK);
				}
			}
			// Return a not found response if the job step does not exist
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, name);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	/**
	 * Resume a production planner job step identified by name or id
	 * 
	 * @param jobstepId   The id of the job step to resume
	 * @param httpHeaders HttpHeaders object containing HTTP headers for the response.
	 * @return ResponseEntity containing the resumed RestJobStep object
	 */
	@Override
	public ResponseEntity<RestJobStep> resumeJobStep(String jobstepId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> resumeJobStep({})", jobstepId);

		try {
			// wait until finish of concurrent createJob
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

			// Find the job step by name or id
			JobStep jobStep = this.findJobStepByNameOrId(jobstepId);

			if (jobStep != null) {
				// Check the status of the requested processing facility
				final ResponseEntity<RestJobStep> response = transactionTemplate.execute((status) -> {
					ProcessingFacility processingFacility = this.findJobStepByNameOrId(jobstepId).getJob().getProcessingFacility();
					KubeConfig kubeConfig = productionPlanner.updateKubeConfig(processingFacility.getName());
					if (null == kubeConfig) {
						String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, processingFacility.getName());

						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
					}
					if (processingFacility.getFacilityState() != FacilityState.RUNNING
							&& processingFacility.getFacilityState() != FacilityState.STARTING) {
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

				if (response != null) {
					return response;
				}

				List<PlannerResultMessage> msg = new ArrayList<PlannerResultMessage>();
				try {
					// Retry database operations in case of concurrency issues
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							final ResponseEntity<RestJobStep> msgF = transactionTemplate.execute((status) -> {
								JobStep jobStepX = this.findJobStepByNameOrIdPrim(jobstepId);
								Job job = jobStepX.getJob();
								if (job.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
									String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE,
											job.getProcessingFacility().getName(),
											job.getProcessingFacility().getFacilityState().toString());

									return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
								} else {
									// Attempt to resume the job step
									msg.add(jobStepUtil.resume(jobStepX, true));
									return null;
								}
							});
							if (msgF != null) {
								return msgF;
							}
							break;
						} catch (CannotAcquireLockException e) {
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
				// Already logged

				if (msg.get(0).getSuccess()) {
					final ResponseEntity<RestJobStep> msgS = transactionTemplate.execute((status) -> {
						JobStep jobStepX = this.findJobStepByNameOrId(jobstepId);
						Job job = jobStepX.getJob();
						if (job != null && job.getProcessingFacility() != null) {
							KubeConfig kubeConfig = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
							if (kubeConfig != null) {
								try {
									// Check if the job step can be run
									UtilService.getJobStepUtil().checkJobStepToRun(kubeConfig, jobStepX.getId());
								} catch (Exception e) {
									String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

									if (logger.isDebugEnabled())
										logger.debug("... exception stack trace: ", e);

									return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
								}
							}
						}

						// Job step resumed successfully
						RestJobStep restJobStep = RestUtil.createRestJobStep(jobStepX, false);

						return new ResponseEntity<>(restJobStep, HttpStatus.OK);
					});
					return msgS;
				} else {
					// Illegal state for resuming the job step
					String message = logger.log(msg.get(0).getMessage(), jobstepId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			// Return a not found response if the job step does not exist
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, jobstepId);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Cancel a production planner job step identified by name or id
	 * 
	 * @param jobstepId   The id of the job step to cancel
	 * @param httpHeaders HttpHeaders object containing HTTP headers for the response.
	 * @return ResponseEntity containing the canceled RestJobStep object
	 */
	@Override
	public ResponseEntity<RestJobStep> cancelJobStep(String jobstepId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> cancelJobStep({})", jobstepId);

		try {
			// Wait until finish of concurrent createJob
			JobStep jobStep = this.findJobStepByNameOrId(jobstepId);
			if (jobStep != null) {
				Job job = jobStep.getJob();
				PlannerResultMessage msg = new PlannerResultMessage(null);
				try {
					// Execute the cancellation within a transaction
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							// Attempt to cancel the job step
							msg = transactionTemplate.execute((status) -> {
								JobStep jobStepX = this.findJobStepByNameOrIdPrim(jobstepId);
								return jobStepUtil.cancel(jobStepX);
							});
							break;
						} catch (CannotAcquireLockException e) {
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
					if (job != null && job.getProcessingFacility() != null) {
						KubeConfig kubeConfig = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
						if (kubeConfig != null) {
							try {
								// Check if the job step can be run after cancellation
								UtilService.getJobStepUtil().checkJobStepToRun(kubeConfig, jobStep.getId());
							} catch (Exception e) {
								String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

								if (logger.isDebugEnabled())
									logger.debug("... exception stack trace: ", e);

								return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
							}
						}
					}
					// Job step successfully cancelled
					RestJobStep restJobStep = getRestJobStep(jobStep.getId(), false);

					return new ResponseEntity<>(restJobStep, HttpStatus.OK);
				} else {
					// Illegal state for cancelling the job step
					String message = logger.log(msg.getMessage(), jobstepId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			// Return a not found response if the job step does not exist
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, jobstepId);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Cancel a production planner job step identified by name or id. Kill the job step if force equals true, otherwise wait until
	 * end of Kubernetes job.
	 * 
	 * @param jobstepId   The id of the job step to suspend
	 * @param forceP      Whether to force suspension or not
	 * @param httpHeaders HttpHeaders object containing HTTP headers for the response.
	 * @return ResponseEntity containing the suspended RestJobStep object
	 */
	@Override
	public ResponseEntity<RestJobStep> suspendJobStep(String jobstepId, Boolean forceP, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> suspendJobStep({}, force: {})", jobstepId, forceP);

		// Set force to false if forceP is null
		final Boolean force = (null == forceP ? false : forceP);

		try {
			// Find the job step by name or id
			JobStep jobStep = this.findJobStepByNameOrId(jobstepId);
			if (jobStep != null) {
				// Check the status of the requested processing facility
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
				final ResponseEntity<RestJobStep> response = transactionTemplate.execute((status) -> {
					ProcessingFacility processingFacility = this.findJobStepByNameOrId(jobstepId).getJob().getProcessingFacility();
					KubeConfig kubeConfig = productionPlanner.updateKubeConfig(processingFacility.getName());
					if (null == kubeConfig) {
						String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, processingFacility.getName());

						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
					}
					if (processingFacility.getFacilityState() != FacilityState.RUNNING
							&& processingFacility.getFacilityState() != FacilityState.STARTING) {
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
				if (response != null) {
					return response;
				}

				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						final ResponseEntity<RestJobStep> msgF = transactionTemplate.execute((status) -> {
							JobStep jobStepX = this.findJobStepByNameOrIdPrim(jobstepId);
							Job job = jobStepX.getJob();
							if (job.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
								String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE,
										job.getProcessingFacility().getName(),
										job.getProcessingFacility().getFacilityState().toString());

								return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
							} else {
								return null;
							}
						});
						if (msgF != null) {
							return msgF;
						}

						break;
					} catch (CannotAcquireLockException e) {
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

				// Attempt to suspend the job step within a transaction
				PlannerResultMessage msg = new PlannerResultMessage(GeneralMessage.FALSE);
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						msg = transactionTemplate.execute((status) -> {
							JobStep jobStepX = this.findJobStepByNameOrIdPrim(jobstepId);
							return jobStepUtil.suspend(jobStepX, force);
						});
						break;
					} catch (CannotAcquireLockException e) {
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
					// Job step successfully suspended
					RestJobStep pjs = getRestJobStep(jobStep.getId(), false);

					return new ResponseEntity<>(pjs, HttpStatus.OK);
				} else {
					// Illegal state for suspending the job step
					String message = logger.log(msg.getMessage(), jobstepId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			// Return a not found response if the job step does not exist
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, jobstepId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			e.printStackTrace();
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Get job step identified by name or id.
	 * 
	 * @param nameOrId The name or id of the job step to retrieve
	 * @return Job step found based on the provided name or id.
	 */
	private JobStep findJobStepByNameOrIdPrim(String nameOrId) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findJobStepByNameOrIdPrim({})", nameOrId);

		JobStep jobStep = null;
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);
		jobStep = transactionTemplate.execute((status) -> {
			JobStep jobStepX = null;
			Long id = null;
			// Check if the nameOrId is a numeric id or starts with the jobNamePrefix
			if (nameOrId != null) {
				if (nameOrId.matches("[0-9]+")) {
					id = Long.valueOf(nameOrId);
				} else if (nameOrId.startsWith(ProductionPlanner.jobNamePrefix)) {
					id = Long.valueOf(nameOrId.substring(ProductionPlanner.jobNamePrefix.length()));
				}
				// If id is not null, retrieve the job step from the repository
				if (id != null) {
					Optional<JobStep> jso = RepositoryService.getJobStepRepository().findById(id);
					if (jso.isPresent()) {
						jobStepX = jso.get();
					}
				}
			}

			if (null != jobStepX) {
				// Ensure user is authorized for the mission of the order
				String missionCode = securityService.getMission();
				String orderMissionCode = jobStepX.getJob().getProcessingOrder().getMission().getCode();
				if (!missionCode.equals(orderMissionCode)) {
					logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, orderMissionCode, missionCode);
					return null;
				}
			}
			return jobStepX;
		});
		return jobStep;
	}

	/**
	 * Finds a job step by its name or ID.
	 * 
	 * @param nameOrId The name or ID of the job step to find.
	 * @return The found job step, or null if not found or an exception occurs.
	 */
	private JobStep findJobStepByNameOrId(String nameOrId) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findJobStepByNameOrId({})", nameOrId);
		JobStep jobStep = null;
		try {
			jobStep = this.findJobStepByNameOrIdPrim(nameOrId);
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);
		}
		return jobStep;
	}

	/**
	 * Retrieves a REST representation of a job step identified by its ID.
	 * 
	 * @param id   The ID of the job step to retrieve.
	 * @param logs Boolean indicating whether to include logs in the REST representation.
	 * @return A REST representation of the specified job step, or null if not found or an exception occurs.
	 */
	private RestJobStep getRestJobStep(long id, Boolean logs) {
		RestJobStep answer = null;
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
			transactionTemplate.setReadOnly(true);
			answer = transactionTemplate.execute((status) -> {
				RestJobStep restJobStep = null;
				JobStep jobStep = null;
				Optional<JobStep> opt = RepositoryService.getJobStepRepository().findById(id);
				if (opt.isPresent()) {
					jobStep = opt.get();
					restJobStep = RestUtil.createRestJobStep(jobStep, logs);
				}
				return restJobStep;
			});
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);
		}
		return answer;
	}

	/**
	 * Retry a production planner job step identified by name or id
	 * 
	 * @param jobstepId   The id of the job step to retry
	 * @param httpHeaders HttpHeaders object containing HTTP headers for the response.
	 * @return ResponseEntity containing the retried RestJobStep object
	 */
	@Override
	public ResponseEntity<RestJobStep> retryJobStep(String jobstepId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> retryJobStep({})", jobstepId);

		try {
			// Find the job step using the provided id or name
			JobStep jobStep = this.findJobStepByNameOrId(jobstepId);
			if (jobStep != null) {
				PlannerResultMessage msg = new PlannerResultMessage(null);
				try {
					// Execute the retry operation within a transaction, handling concurrency issues
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							msg = transactionTemplate.execute((status) -> {
								JobStep jobStepX = this.findJobStepByNameOrIdPrim(jobstepId);
								return jobStepUtil.retry(jobStepX);
							});
							break;
						} catch (CannotAcquireLockException e) {
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

				// If retry operation is successful, return the retried job step
				if (msg.getSuccess()) {
					RestJobStep restJobStep = getRestJobStep(jobStep.getId(), false);

					return new ResponseEntity<>(restJobStep, HttpStatus.OK);
				} else {
					// If retry operation fails, return an error response
					String message = logger.log(msg.getMessage(), jobstepId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			// If the job step does not exist, return a not found error response
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, jobstepId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
