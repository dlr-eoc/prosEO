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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.logging.messages.ProseoMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.rest.JobstepController;
import de.dlr.proseo.model.rest.model.RestJob;
import de.dlr.proseo.model.rest.model.RestJobStep;
import de.dlr.proseo.model.rest.model.Status;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.kubernetes.KubeJob;
import de.dlr.proseo.planner.rest.model.RestUtil;
import de.dlr.proseo.planner.util.JobStepUtil;
import de.dlr.proseo.planner.util.UtilService;


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
     */
	@Override
	@Transactional
    public ResponseEntity<List<RestJobStep>> getJobSteps(Status status, String mission, Long last) {		
		if (logger.isTraceEnabled()) logger.trace(">>> getJobSteps({}, {}, {})", status, mission, last);
		
		if (null == mission || mission.isBlank()) {
			mission = securityService.getMission();
		} else if (!mission.equals(securityService.getMission())) {
			String message = logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, mission, securityService.getMission());
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.FORBIDDEN);
		}
		
		try {
			List<RestJobStep> list = new ArrayList<RestJobStep>(); 
			List<JobStep> it;
			if (status == null || status.value().equalsIgnoreCase("NONE")) {
				List<JobStep> allJobSteps = RepositoryService.getJobStepRepository().findAll();
				it = new ArrayList<>();
				for (JobStep js: allJobSteps) {
					if (js.getJob().getProcessingOrder().getMission().getCode().equals(mission)) {
						it.add(js);
					}
				}
			} else {
				JobStepState state = JobStepState.valueOf(status.toString());
				//it = new ArrayList<JobStep>();
				if (last != null && last > 0) {
					List<JobStep> itall = jobStepUtil.findOrderedByJobStepStateAndMission(state, mission, last.intValue());
					if (last < itall.size()) {
						it = itall.subList(0, last.intValue());
					} else {
						it = itall;
					}
				} else {
					it = RepositoryService.getJobStepRepository().findAllByJobStepStateAndMissionOrderByDate(state, mission);
				}
			}
			for (JobStep js : it) {
				RestJobStep pjs = RestUtil.createRestJobStep(js, false);
				list.add(pjs);			
			}
			
			logger.log(PlannerMessage.JOBSTEPS_RETRIEVED, status, mission);

			return new ResponseEntity<>(list, HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Get production planner job step identified by name or id
     * 
     */
	@Override
	public ResponseEntity<RestJobStep> getJobStep(String name) {
		if (logger.isTraceEnabled()) logger.trace(">>> getJobStep({})", name);
		
		try {
			JobStep js = this.findJobStepByNameOrId(name);
			if (js != null) {
				ProseoMessage msg = null;
				try {
					productionPlanner.acquireThreadSemaphore("getJobStep");
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.execute((status) -> {
						JobStep jsx = this.findJobStepByNameOrIdPrim(name);
						Job job = jsx.getJob();
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
					productionPlanner.releaseThreadSemaphore("getJobStep");	
				} catch (Exception e) {
					productionPlanner.releaseThreadSemaphore("getJobStep");	
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
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
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}

    /**
     * Resume a production planner job step identified by name or id
     * 
     */
	@Override 
	public ResponseEntity<RestJobStep> resumeJobStep(String jobstepId) {
		if (logger.isTraceEnabled()) logger.trace(">>> resumeJobStep({})", jobstepId);
		
		try {
			// wait until finish of concurrent createJob
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());

			JobStep js = this.findJobStepByNameOrId(jobstepId);
			if (js != null) {
				List<ProseoMessage> msg = new ArrayList<ProseoMessage>();
				try {
					productionPlanner.acquireThreadSemaphore("resumeJobStep");
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
					productionPlanner.releaseThreadSemaphore("resumeJobStep");
					if (msgF != null) {
						return msgF;
					}
				} catch (Exception e) {
					productionPlanner.releaseThreadSemaphore("resumeJobStep");	
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
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
									productionPlanner.acquireThreadSemaphore("resumeJobStep");
									UtilService.getJobStepUtil().checkJobStepToRun(kc, jsx.getId());
									productionPlanner.releaseThreadSemaphore("resumeJobStep");
								} catch (Exception e) {
									String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
									productionPlanner.releaseThreadSemaphore("resumeJobStep");
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
					String message = logger.log(msg.get(0), jobstepId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, jobstepId);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Cancel a production planner job step identified by name or id
     * 
     */
	@Override 
	public ResponseEntity<RestJobStep> cancelJobStep(String jobstepId) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancelJobStep({})", jobstepId);

		try {
			// wait until finish of concurrent createJob
			JobStep js = this.findJobStepByNameOrId(jobstepId);
			if (js != null) {
				Job job = js.getJob();
				ProseoMessage msg = null;
				try {
					productionPlanner.acquireThreadSemaphore("cancelJobStep");
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					msg = transactionTemplate.execute((status) -> {
						JobStep jsx = this.findJobStepByNameOrIdPrim(jobstepId);
						return jobStepUtil.cancel(jsx);
					});
					productionPlanner.releaseThreadSemaphore("cancelJobStep");	
				} catch (Exception e) {
					productionPlanner.releaseThreadSemaphore("cancelJobStep");	
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				if (msg.getSuccess()) {
					if (job != null && job.getProcessingFacility() != null) {
						KubeConfig kc = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
						if (kc != null) {
							try {
								productionPlanner.acquireThreadSemaphore("cancelJobStep");
								UtilService.getJobStepUtil().checkJobStepToRun(kc, js.getId());
								productionPlanner.releaseThreadSemaphore("cancelJobStep");
							} catch (Exception e) {
								String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
								productionPlanner.releaseThreadSemaphore("cancelJobStep");
								return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
							}
						}
					}
					// cancelled
					RestJobStep pjs = getRestJobStep(js.getId(), false);

					return new ResponseEntity<>(pjs, HttpStatus.OK);
				} else {
					// illegal state for cancel
					String message = logger.log(msg, jobstepId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, jobstepId);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Cancel a production planner job step identified by name or id.
     * Kill the job step if force equals true, otherwise wait until end of Kubernetes job.
     * 
     */
	@Override 
	public ResponseEntity<RestJobStep> suspendJobStep(String jobstepId, Boolean forceP) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspendJobStep({}, force: {})", jobstepId, forceP);

		final Boolean force = (null == forceP ? false : forceP);
		
		try {
			JobStep js = this.findJobStepByNameOrId(jobstepId);
			if (js != null) {
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				try {
					productionPlanner.acquireThreadSemaphore("suspendJobStep");
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
					productionPlanner.releaseThreadSemaphore("suspendJobStep");

				} catch (Exception e) {
					productionPlanner.releaseThreadSemaphore("suspendJobStep");	
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				ProseoMessage msg = GeneralMessage.FALSE; 
				try {
					productionPlanner.acquireThreadSemaphore("suspendJobStep");
					msg = transactionTemplate.execute((status) -> {
						JobStep jsx = this.findJobStepByNameOrIdPrim(jobstepId);
						return jobStepUtil.suspend(jsx, force);
					});
					productionPlanner.releaseThreadSemaphore("suspendJobStep");
				} catch (Exception e) {
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
					productionPlanner.releaseThreadSemaphore("suspendJobStep");
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				if (msg.getSuccess()) {
					// suspended
					RestJobStep pjs = getRestJobStep(js.getId(), false);

					return new ResponseEntity<>(pjs, HttpStatus.OK);
				} else {
					// illegal state for suspend
					String message = logger.log(msg, jobstepId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, jobstepId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			e.printStackTrace();
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
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
			productionPlanner.acquireThreadSemaphore("findJobStepByNameOrId");
			js = this.findJobStepByNameOrIdPrim(nameOrId);
			productionPlanner.releaseThreadSemaphore("findJobStepByNameOrId");	
		} catch (Exception e) {
			productionPlanner.releaseThreadSemaphore("findJobStepByNameOrId");	
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());	
		}
		return js;
	}

	private RestJobStep getRestJobStep(long id, Boolean logs) {
		RestJobStep answer = null;
		try {
			productionPlanner.acquireThreadSemaphore("getRestJobStep");
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
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
		} finally {
			productionPlanner.releaseThreadSemaphore("getRestJobStep");
		}
		return answer;
	}
    /**
     * Retry a production planner job step identified by name or id.
     * 
     */
	@Override
	public ResponseEntity<RestJobStep> retryJobStep(String jobstepId) {
		if (logger.isTraceEnabled()) logger.trace(">>> retryJobStep({})", jobstepId);
		
		try {
			JobStep js = this.findJobStepByNameOrId(jobstepId);
			if (js != null) {
				ProseoMessage msg = null;
				try {
					productionPlanner.acquireThreadSemaphore("retryJobStep");
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					msg = transactionTemplate.execute((status) -> {
						JobStep jsx = this.findJobStepByNameOrIdPrim(jobstepId);
						return jobStepUtil.retry(jsx);
					});
					productionPlanner.releaseThreadSemaphore("retryJobStep");	
				} catch (Exception e) {
					productionPlanner.releaseThreadSemaphore("retryJobStep");	
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());			
					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				
				if (msg.getSuccess()) {
					RestJobStep pjs = getRestJobStep(js.getId(), false);

					return new ResponseEntity<>(pjs, HttpStatus.OK);
				} else {
					String message = logger.log(msg, jobstepId);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message = logger.log(PlannerMessage.JOBSTEP_NOT_EXIST, jobstepId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
