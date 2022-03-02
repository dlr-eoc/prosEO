/**
 * JobstepControllerImpl.java
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.rest.JobstepController;
import de.dlr.proseo.model.rest.model.RestJobStep;
import de.dlr.proseo.model.rest.model.Status;
import de.dlr.proseo.planner.Messages;
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
	private static Logger logger = LoggerFactory.getLogger(JobstepControllerImpl.class);

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
			String message = Messages.ILLEGAL_CROSS_MISSION_ACCESS.log(logger, mission, securityService.getMission());
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.FORBIDDEN);
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
			
			Messages.JOBSTEPS_RETRIEVED.log(logger, status, mission);

			return new ResponseEntity<>(list, HttpStatus.OK);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Get production planner job step identified by name or id
     * 
     */
	@Override
	@Transactional
	public ResponseEntity<RestJobStep> getJobStep(String name) {
		if (logger.isTraceEnabled()) logger.trace(">>> getJobStep({})", name);
		
		try {
			JobStep js = this.findJobStepByNameOrId(name);
			if (js != null) {
				Job job = js.getJob();
				if (js.getJobStepState() == JobStepState.RUNNING && job != null) {
					if (job.getProcessingFacility() != null) {
						KubeConfig kc = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
						if (kc != null) {
							KubeJob kj = kc.getKubeJob(ProductionPlanner.jobNamePrefix + js.getId());
							if (kj != null) {
								kj.updateInfo(ProductionPlanner.jobNamePrefix + js.getId());
							}
						}
					}
				}
				RestJobStep pjs = RestUtil.createRestJobStep(js, true);

				Messages.JOBSTEP_RETRIEVED.log(logger, name);

				return new ResponseEntity<>(pjs, HttpStatus.OK);
			}
			String message = Messages.JOBSTEP_NOT_EXIST.log(logger, name);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}

    /**
     * Resume a production planner job step identified by name or id
     * 
     */
	@Override 
	@Transactional
	public ResponseEntity<RestJobStep> resumeJobStep(String jobstepId) {
		if (logger.isTraceEnabled()) logger.trace(">>> resumeJobStep({})", jobstepId);
		
		try {
			// wait until finish of concurrent createJob
			JobStep js = this.findJobStepByNameOrId(jobstepId);
			if (js != null) {
				Job job = js.getJob();
				if (job.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
					String message = Messages.FACILITY_NOT_AVAILABLE.log(logger, job.getProcessingFacility().getName(),
							job.getProcessingFacility().getFacilityState().toString());

			    	return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}

				Messages msg = jobStepUtil.resume(js, true);
				// Already logged
				
				if (msg.isTrue()) {
					UtilService.getJobUtil().updateState(job, js.getJobStepState());
					if (job != null && job.getProcessingFacility() != null) {
						KubeConfig kc = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
						if (kc != null) {
							productionPlanner.acquireReleaseSemaphore("resumeJobStep");
							try {
								UtilService.getJobStepUtil().checkJobStepToRun(kc, js);
								productionPlanner.releaseReleaseSemaphore("resumeJobStep");
							} catch (Exception e) {
								String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
								productionPlanner.releaseReleaseSemaphore("resumeJobStep");
								return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
							}
						}
					}
					// resumed
					RestJobStep pjs = RestUtil.createRestJobStep(js, false);

					return new ResponseEntity<>(pjs, HttpStatus.OK);
				} else {
					// illegal state for resume
					String message = msg.format(jobstepId);

					return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message =  Messages.JOBSTEP_NOT_EXIST.log(logger, jobstepId);
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Cancel a production planner job step identified by name or id
     * 
     */
	@Override 
	@Transactional
	public ResponseEntity<RestJobStep> cancelJobStep(String jobstepId) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancelJobStep({})", jobstepId);

		try {
			// wait until finish of concurrent createJob
			JobStep js = this.findJobStepByNameOrId(jobstepId);
			if (js != null) {
				Job job = js.getJob();
				Messages msg = jobStepUtil.cancel(js);
				if (msg.isTrue()) {
					UtilService.getJobUtil().updateState(job, js.getJobStepState());
					if (job != null && job.getProcessingFacility() != null) {
						KubeConfig kc = productionPlanner.getKubeConfig(job.getProcessingFacility().getName());
						if (kc != null) {
							productionPlanner.acquireReleaseSemaphore("cancelJobStep");
							try {
								UtilService.getJobStepUtil().checkJobStepToRun(kc, js);
								productionPlanner.releaseReleaseSemaphore("cancelJobStep");
							} catch (Exception e) {
								String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
								productionPlanner.releaseReleaseSemaphore("cancelJobStep");
								return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
							}
						}
					}
					// cancelled
					RestJobStep pjs = RestUtil.createRestJobStep(js, false);

					return new ResponseEntity<>(pjs, HttpStatus.OK);
				} else {
					// illegal state for cancel
					String message = msg.format(jobstepId);

					return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message =  Messages.JOBSTEP_NOT_EXIST.log(logger, jobstepId);
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Cancel a production planner job step identified by name or id.
     * Kill the job step if force equals true, otherwise wait until end of Kubernetes job.
     * 
     */
	@Override 
	@Transactional
	public ResponseEntity<RestJobStep> suspendJobStep(String jobstepId, Boolean force) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspendJobStep({}, force: {})", jobstepId, force);
		
		if (null == force) {
			force = false;
		}
		
		try {
			JobStep js = this.findJobStepByNameOrId(jobstepId);
			if (js != null) {
				Job job = js.getJob();
				
				// "Suspend force" is only allowed, if the processing facility is available
				if (force && job.getProcessingFacility().getFacilityState() != FacilityState.RUNNING) {
					String message = Messages.FACILITY_NOT_AVAILABLE.log(logger, job.getProcessingFacility().getName(),
							job.getProcessingFacility().getFacilityState().toString());

			    	return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}

				Messages msg = jobStepUtil.suspend(js, force); 
				if (msg.isTrue()) {
					// suspended
					UtilService.getJobUtil().updateState(job, js.getJobStepState());
					RestJobStep pjs = RestUtil.createRestJobStep(js, false);

					return new ResponseEntity<>(pjs, HttpStatus.OK);
				} else {
					// illegal state for suspend
					String message = msg.format(jobstepId);

					return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message =  Messages.JOBSTEP_NOT_EXIST.log(logger, jobstepId);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			e.printStackTrace();
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Get job step identified by name or id.
	 * @param nameOrId
	 * @return Job step found
	 */
	@Transactional
	private JobStep findJobStepByNameOrId(String nameOrId) {
		if (logger.isTraceEnabled()) logger.trace(">>> findJobStepByNameOrId({})", nameOrId);
		
		JobStep js = null;
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
					js = jso.get();
				}
			}
		}

		if (null != js) {
			// Ensure user is authorized for the mission of the order
			String missionCode = securityService.getMission();
			String orderMissionCode = js.getJob().getProcessingOrder().getMission().getCode();
			if (!missionCode.equals(orderMissionCode)) {
				Messages.ILLEGAL_CROSS_MISSION_ACCESS.log(logger, orderMissionCode, missionCode);
				return null;
			} 
		}
		return js;
	}

    /**
     * Retry a production planner job step identified by name or id.
     * 
     */
	@Transactional
	@Override
	public ResponseEntity<RestJobStep> retryJobStep(String jobstepId) {
		if (logger.isTraceEnabled()) logger.trace(">>> retryJobStep({})", jobstepId);
		
		try {
			JobStep js = this.findJobStepByNameOrId(jobstepId);
			if (js != null) {
				Job job = js.getJob();
				Messages msg = jobStepUtil.retry(js);
				// Already logged
				
				if (msg.isTrue()) {
					UtilService.getJobUtil().updateState(job, js.getJobStepState());
					RestJobStep pjs = RestUtil.createRestJobStep(js, false);

					return new ResponseEntity<>(pjs, HttpStatus.OK);
				} else {
					String message = msg.format(jobstepId);

					return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
			String message =  Messages.JOBSTEP_NOT_EXIST.log(logger, jobstepId);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
