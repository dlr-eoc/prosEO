package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OrderMgrMessage;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.rest.OrderjobstepController;
import de.dlr.proseo.model.rest.model.RestJobStep;
import de.dlr.proseo.model.rest.model.Status;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.ordermgr.OrderManager;
import de.dlr.proseo.ordermgr.rest.model.RestUtil;

/**
 * Controller for the prosEO Order Manager, implements the services required to manage job steps.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class OrderjobstepControllerImpl implements OrderjobstepController {
	/**
	 * Logger of this class
	 */
	private static ProseoLogger logger = new ProseoLogger(OrderjobstepControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.ORDER_MGR);

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;
    
	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
    /**
     * Get production planner job steps by status, mission and latest of size "last"
     * 
     */
	@Override
	@Transactional(readOnly = true)
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
					List<JobStep> itall = findOrderedByJobStepStateAndMission(state, mission, last.intValue());
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
			
			logger.log(OrderMgrMessage.JOBSTEPS_RETRIEVED, status, mission);

			return new ResponseEntity<>(list, HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);
			
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Get production planner job step identified by name or id
     * 
     */
	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<RestJobStep> getJobStep(String name) {
		if (logger.isTraceEnabled()) logger.trace(">>> getJobStep({})", name);
		
		try {
			JobStep js = this.findJobStepByNameOrId(name);
			if (js != null) {
				RestJobStep pjs = getRestJobStep(js.getId(), true);

				logger.log(OrderMgrMessage.JOBSTEP_RETRIEVED, name);

				return new ResponseEntity<>(pjs, HttpStatus.OK);
			}
			String message = logger.log(OrderMgrMessage.JOBSTEP_NOT_EXIST, name);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
	}


	/**
	 * Get job step identified by name or id.
	 * @param nameOrId
	 * @return Job step found
	 */
	@Transactional(readOnly = true)
	private JobStep findJobStepByNameOrIdPrim(String nameOrId) {
		if (logger.isTraceEnabled()) logger.trace(">>> findJobStepByNameOrIdPrim({})", nameOrId);

		JobStep jsx = null;
		Long id = null;
		if (nameOrId != null) {
			if (nameOrId.matches("[0-9]+")) {
				id = Long.valueOf(nameOrId);
			} else if (nameOrId.startsWith(OrderManager.jobNamePrefix)) {
				id = Long.valueOf(nameOrId.substring(OrderManager.jobNamePrefix.length()));
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
	}

	private JobStep findJobStepByNameOrId(String nameOrId) {
		if (logger.isTraceEnabled()) logger.trace(">>> findJobStepByNameOrId({})", nameOrId);
		JobStep js = null;
		try {
			js = this.findJobStepByNameOrIdPrim(nameOrId);
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);	
		}
		return js;
	}

	@Transactional(readOnly = true)
	private RestJobStep getRestJobStep(long id, Boolean logs) {
		RestJobStep rj = null;
		JobStep js = null;
		Optional<JobStep> opt = RepositoryService.getJobStepRepository().findById(id);
		if (opt.isPresent()) {
			js = opt.get();
			rj = RestUtil.createRestJobStep(js, logs);
		}
		return rj;
	}

	/**
	 * Find job steps of specific job step state. The result is ordered by processingCompletionTime descending and returns the first 'limit' entries.
	 *  
	 * @param state The job step state
	 * @param mission The mission code
	 * @param limit The length of result entry list
	 * @return The found job steps
	 */
	@Transactional(readOnly = true)
	private List<JobStep> findOrderedByJobStepStateAndMission(JobStepState state, String mission, int limit) {
		if (logger.isTraceEnabled()) logger.trace(">>> findOrderedByJobStepStateAndMission({}, {}, {})", state, mission, limit);

		String query = "select js from JobStep js " + 
				" inner join Job j on js.job.id = j.id " + 
				" inner join ProcessingOrder o on j.processingOrder.id = o.id" + 
				" inner join Mission m on o.mission.id = m.id " + 
				" where js.processingStartTime is not null and js.jobStepState = '" + state + "' and m.code = '" + mission + 
				"' order by js.processingCompletionTime desc";
		// em.createNativeQ
		return em.createQuery(query,
			JobStep.class)
				.setMaxResults(limit)
				.getResultList();
	}
}
