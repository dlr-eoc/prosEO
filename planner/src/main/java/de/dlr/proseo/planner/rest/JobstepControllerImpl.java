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
 * Spring MVC controller for the prosEO planner; implements the services required to plan
 * and handle job steps.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class JobstepControllerImpl implements JobstepController {
	
	/**
	 * Logger of this class
	 */
	private static ProseoLogger logger = new ProseoLogger(JobstepControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PLANNER);

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;
    
    @Autowired
    private JobStepUtil jobStepUtil;
    
   
    /**
     * Get production planner job steps by status
     * 
     * @param status TODO
     * @param mission TODO
     * @param last TODO
     * @param httpHeaders TODO
     * @return TODO
     */
	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public ResponseEntity<List<RestJobStep>> getJobSteps(Status status, String mission, Long last, HttpHeaders httpHeaders) {		
		if (logger.isTraceEnabled()) logger.trace(">>> getJobSteps({}, {}, {})", status, mission, last);
		
		if (null == mission || mission.isBlank()) {
			mission = securityService.getMission();
		} else if (!mission.equals(securityService.getMission())) {
			String message = logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, mission, securityService.getMission());
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.FORBIDDEN);
		}
		
		try {
			List<RestJobStep> list = new ArrayList<RestJobStep>(); 
			List<JobStep> jsList;
			if (status == null || status.value().equalsIgnoreCase("NONE")) {
				List<JobStep> allJobSteps = RepositoryService.getJobStepRepository().findAll();
				jsList = new ArrayList<>();
				for (JobStep js: allJobSteps) {
					if (js.getJob().getProcessingOrder().getMission().getCode().equals(mission)) {
						jsList.add(js);
					}
				}
			} else {
				JobStepState state = JobStepState.valueOf(status.toString());

				if (last != null && last > 0) {
					List<JobStep> jsListAll = jobStepUtil.findOrderedByJobStepStateAndMission(state, mission, last.intValue());
					if (last < jsListAll.size()) {
						jsList = jsListAll.subList(0, last.intValue());
					} else {
						jsList = jsListAll;
					}
				} else {
					jsList = RepositoryService.getJobStepRepository().findAllByJobStepStateAndMissionOrderByDate(state, mission);
				}
			}
			for (JobStep js : jsList) {
				RestJobStep pjs = RestUtil.createRestJobStep(js, false);
				list.add(pjs);			
			}
			
			logger.log(PlannerMessage.JOBSTEPS_RETRIEVED, status, mission);

			return new ResponseEntity<>(list, HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Get production planner job step identified by name or id
     * 
     */
	@Override
	public ResponseEntity<RestJobStep> getJobStep(String name, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getJobStep({})", name);
		
		try {
			JobStep js = this.findJobStepByNameOrId(name);
			if (js != null) {
				try {
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					transactionTemplate.execute((status) -> {
						JobStep jsx = this.findJobStepByNameOrIdPrim(name);
						Job job = jsx.getJob();
						
						// TODO Check whether loading/updating of log info is still necessary
						if (jsx.getJobStepState() == JobStepState.RUNNING && job != null) {
							if (job.getProcessingFacility() != null) {
								KubeConfig kc = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
								if (kc != null) {
									KubeJob kj = kc.getKubeJob(ProductionPlanner.jobNamePrefix + jsx.getId());
									if (kj != null) {
										kj.updateInfo(ProductionPlanner.jobNamePrefix + jsx.getId());
									}
								}
							}
						}
						return null;
					});
				} catch (Exception e) {
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
					
					if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				RestJobStep pjs = getRestJobStep(js.getId(), true);

				logger.log(PlannerMessage.JOBSTEP_RETRIEVED, name);

				return new ResponseEntity<>(pjs, HttpStatus.OK);
			}
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, name);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}

    /**
     * Get production planner job step log string by name or id
     * 
     */
	@Override
	public ResponseEntity<String> getJobStepLog(String name, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getJobStep({})", name);
		try {
			JobStep js = this.findJobStepByNameOrIdPrim(name);
			if (js != null) {
				String logx = null;
				try {
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					transactionTemplate.setReadOnly(true);
					logx = transactionTemplate.execute((status) -> {
						String log = null;
						JobStep jsx = this.findJobStepByNameOrIdPrim(name);
						Job job = jsx.getJob();
						if (jsx.getJobStepState() == JobStepState.RUNNING && job != null) {
							if (job.getProcessingFacility() != null) {
								KubeConfig kc = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
								if (kc != null && kc.isConnected()) {
									KubeJob kj = kc.getKubeJob(ProductionPlanner.jobNamePrefix + jsx.getId());
									if (kj != null) {
										V1Pod aPod = kc.getV1Pod(kj.getPodNames().get(kj.getPodNames().size()-1));
										log = kj.getJobStepLogPrim(aPod);
									}
								}
							}
						} else {
							log = jsx.getProcessingStdOut();
						}
						return log;
					});
				} catch (Exception e) {
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
					
					if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				if (logx == null) {
					return new ResponseEntity<>("", HttpStatus.OK);
				}else {
					return new ResponseEntity<>(logx, HttpStatus.OK);
				}
			}
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, name);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}

    /**
     * Resume a production planner job step identified by name or id
     * 
     */
	@Override 
	public ResponseEntity<RestJobStep> resumeJobStep(String jobstepId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> resumeJobStep({})", jobstepId);
		
		try {
			// wait until finish of concurrent createJob
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

			JobStep js = this.findJobStepByNameOrId(jobstepId);
			
			if (js != null) {
				// Check the status of the requested processing facility
				final ResponseEntity<RestJobStep> response = transactionTemplate.execute((status) -> {
					ProcessingFacility pf = this.findJobStepByNameOrId(jobstepId).getJob().getProcessingFacility();
					KubeConfig kc = productionPlanner.updateKubeConfig(pf.getName());
					if (null == kc) {
						String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, pf.getName());

						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
					}
					if (pf.getFacilityState() != FacilityState.RUNNING && pf.getFacilityState() != FacilityState.STARTING) {
						String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, pf.getName(), pf.getFacilityState().toString());
						if (pf.getFacilityState() == FacilityState.DISABLED) {
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
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							final ResponseEntity<RestJobStep> msgF = transactionTemplate.execute((status) -> {
								JobStep jsx = this.findJobStepByNameOrIdPrim(jobstepId);
								Job job = jsx.getJob();
								if (job.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
									String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, job.getProcessingFacility().getName(),
											job.getProcessingFacility().getFacilityState().toString());

									return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
								} else {
									msg.add(jobStepUtil.resume(jsx, true));	
									return null;
								}
							});
							if (msgF != null) {
								return msgF;
							}
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}
				} catch (Exception e) {
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
					
					if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				// Already logged
				
				if (msg.get(0).getSuccess()) {
					final ResponseEntity<RestJobStep> msgS = transactionTemplate.execute((status) -> {
						JobStep jsx = this.findJobStepByNameOrId(jobstepId);
						Job job = jsx.getJob();
						if (job != null && job.getProcessingFacility() != null) {
							KubeConfig kc = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
							if (kc != null) {
								try {
									UtilService.getJobStepUtil().checkJobStepToRun(kc, jsx.getId());
								} catch (Exception e) {
									String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
									
									if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

									return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
								}
							}
						}
						// resumed

						RestJobStep pjs = RestUtil.createRestJobStep(jsx, false);

						return new ResponseEntity<>(pjs, HttpStatus.OK);
					});
					return msgS;
				} else {
					// illegal state for resume
					String message = logger.log(msg.get(0).getMessage(), jobstepId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, jobstepId);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Cancel a production planner job step identified by name or id
     * 
     */
	@Override 
	public ResponseEntity<RestJobStep> cancelJobStep(String jobstepId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancelJobStep({})", jobstepId);

		try {
			// wait until finish of concurrent createJob
			JobStep js = this.findJobStepByNameOrId(jobstepId);
			if (js != null) {
				Job job = js.getJob();
				PlannerResultMessage msg = new PlannerResultMessage(null);
				try {
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							msg = transactionTemplate.execute((status) -> {
								JobStep jsx = this.findJobStepByNameOrIdPrim(jobstepId);
								return jobStepUtil.cancel(jsx);
							});
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}
				} catch (Exception e) {
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
					
					if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				if (msg.getSuccess()) {
					if (job != null && job.getProcessingFacility() != null) {
						KubeConfig kc = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
						if (kc != null) {
							try {
								UtilService.getJobStepUtil().checkJobStepToRun(kc, js.getId());
							} catch (Exception e) {
								String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
								
								if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

								return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
							}
						}
					}
					// cancelled
					RestJobStep pjs = getRestJobStep(js.getId(), false);

					return new ResponseEntity<>(pjs, HttpStatus.OK);
				} else {
					// illegal state for cancel
					String message = logger.log(msg.getMessage(), jobstepId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, jobstepId);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Cancel a production planner job step identified by name or id.
     * Kill the job step if force equals true, otherwise wait until end of Kubernetes job.
     * 
     */
	@Override 
	public ResponseEntity<RestJobStep> suspendJobStep(String jobstepId, Boolean forceP, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspendJobStep({}, force: {})", jobstepId, forceP);

		final Boolean force = (null == forceP ? false : forceP);

		try {
			JobStep js = this.findJobStepByNameOrId(jobstepId);
			if (js != null) {
				// Check the status of the requested processing facility
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
				final ResponseEntity<RestJobStep> response = transactionTemplate.execute((status) -> {
					ProcessingFacility pf = this.findJobStepByNameOrId(jobstepId).getJob().getProcessingFacility();
					KubeConfig kc = productionPlanner.updateKubeConfig(pf.getName());
					if (null == kc) {
						String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, pf.getName());

						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
					}
					if (pf.getFacilityState() != FacilityState.RUNNING && pf.getFacilityState() != FacilityState.STARTING) {
						String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, pf.getName(), pf.getFacilityState().toString());
						if (pf.getFacilityState() == FacilityState.DISABLED) {
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
							JobStep jsx = this.findJobStepByNameOrIdPrim(jobstepId);
							Job job = jsx.getJob();
							if (job.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
								String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, job.getProcessingFacility().getName(),
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
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					} catch (Exception e) {
						String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			

						if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}
				PlannerResultMessage msg = new PlannerResultMessage(GeneralMessage.FALSE); 
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						msg = transactionTemplate.execute((status) -> {
							JobStep jsx = this.findJobStepByNameOrIdPrim(jobstepId);
							return jobStepUtil.suspend(jsx, force);
						});
						break;
					} catch (CannotAcquireLockException e) {
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}

					} catch (Exception e) {	
						String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

						if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}
				if (msg.getSuccess()) {
					// suspended
					RestJobStep pjs = getRestJobStep(js.getId(), false);

					return new ResponseEntity<>(pjs, HttpStatus.OK);
				} else {
					// illegal state for suspend
					String message = logger.log(msg.getMessage(), jobstepId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, jobstepId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			e.printStackTrace();
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Get job step identified by name or id.
	 * @param nameOrId
	 * @return Job step found
	 */
	private JobStep findJobStepByNameOrIdPrim(String nameOrId) {
		if (logger.isTraceEnabled()) logger.trace(">>> findJobStepByNameOrIdPrim({})", nameOrId);
		
		JobStep js = null;
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);
		js = transactionTemplate.execute((status) -> {
			JobStep jsx = null;
			Long id = null;
			if (nameOrId != null) {
				if (nameOrId.matches("[0-9]+")) {
					id = Long.valueOf(nameOrId);
				} else if (nameOrId.startsWith(ProductionPlanner.jobNamePrefix)) {
					id = Long.valueOf(nameOrId.substring(ProductionPlanner.jobNamePrefix.length()));
				}
				if (id != null) {
					Optional<JobStep> jso = RepositoryService.getJobStepRepository().findById(id);
					if (jso.isPresent()) {
						jsx = jso.get();
					}
				}
			}

			if (null != jsx) {
				// Ensure user is authorized for the mission of the order
				String missionCode = securityService.getMission();
				String orderMissionCode = jsx.getJob().getProcessingOrder().getMission().getCode();
				if (!missionCode.equals(orderMissionCode)) {
					logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, orderMissionCode, missionCode);
					return null;
				} 
			}
			return jsx;
		});
		return js;
	}

	private JobStep findJobStepByNameOrId(String nameOrId) {
		if (logger.isTraceEnabled()) logger.trace(">>> findJobStepByNameOrId({})", nameOrId);
		JobStep js = null;
		try {
			js = this.findJobStepByNameOrIdPrim(nameOrId);
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());	
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
		}
		return js;
	}

	private RestJobStep getRestJobStep(long id, Boolean logs) {
		RestJobStep answer = null;
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
			transactionTemplate.setReadOnly(true);
			answer = transactionTemplate.execute((status) -> {
				RestJobStep rj = null;
				JobStep js = null;
				Optional<JobStep> opt = RepositoryService.getJobStepRepository().findById(id);
				if (opt.isPresent()) {
					js = opt.get();
					rj = RestUtil.createRestJobStep(js, logs);
				}
				return rj;
			});
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());	
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
		}
		return answer;
	}
    /**
     * Retry a production planner job step identified by name or id.
     * 
     */
	@Override
	public ResponseEntity<RestJobStep> retryJobStep(String jobstepId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> retryJobStep({})", jobstepId);
		
		try {
			JobStep js = this.findJobStepByNameOrId(jobstepId);
			if (js != null) {
				PlannerResultMessage msg = new PlannerResultMessage(null);
				try {
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							msg = transactionTemplate.execute((status) -> {
								JobStep jsx = this.findJobStepByNameOrIdPrim(jobstepId);
								return jobStepUtil.retry(jsx);
							});
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}
				} catch (Exception e) {
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
					
					if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				
				if (msg.getSuccess()) {
					RestJobStep pjs = getRestJobStep(js.getId(), false);

					return new ResponseEntity<>(pjs, HttpStatus.OK);
				} else {
					String message = logger.log(msg.getMessage(), jobstepId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, jobstepId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
