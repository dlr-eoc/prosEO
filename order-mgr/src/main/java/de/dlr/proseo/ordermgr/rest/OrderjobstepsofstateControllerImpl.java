/**
 * OrderjobstepControllerImpl.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OrderMgrMessage;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.rest.OrderjobstepsofstateController;
import de.dlr.proseo.model.rest.model.RestJobStep;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.ordermgr.rest.model.RestUtil;

/**
 * Controller for the prosEO Order Manager, implements the services required to manage job steps.
 *
 * @author Ernst Melchinger
 */
@Component
public class OrderjobstepsofstateControllerImpl implements OrderjobstepsofstateController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderjobstepControllerImpl.class);

	/** HTTP utility class */
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.ORDER_MGR);

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public ResponseEntity<List<RestJobStep>> getJobStepsOfStates(String[] status, String mission, Long last) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getJobSteps({}, {}, {})", status, mission, last);

		if (null == mission || mission.isBlank()) {
			mission = securityService.getMission();
		} else if (!mission.equals(securityService.getMission())) {
			String message = logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, mission, securityService.getMission());
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.FORBIDDEN);
		}

		try {
			List<RestJobStep> list = new ArrayList<>();
			List<JobStep> it = new ArrayList<>();
			if (status == null || (status.length == 0 ? true : status[0].equalsIgnoreCase("NONE"))) {
				List<JobStep> allJobSteps = RepositoryService.getJobStepRepository().findAll();
				it = new ArrayList<>();
				for (JobStep js : allJobSteps) {
					if (js.getJob().getProcessingOrder().getMission().getCode().equals(mission)) {
						it.add(js);
					}
				}
			} else {
				// it = new ArrayList<JobStep>();
				if (last > 0) {
					List<JobStep> itall = findOrderedByJobStepStatesAndMission(status, mission, last.intValue());
					if (last < itall.size()) {
						it = itall.subList(0, last.intValue());
					} else {
						it = itall;
					}
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
	 * Find job steps of a specific job step state. The result is ordered by processingCompletionTime descending and returns the
	 * first 'limit' entries.
	 *
	 * @param state   The job step state
	 * @param mission The mission code
	 * @param limit   The length of the result entry list
	 * @return The found job steps
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	private List<JobStep> findOrderedByJobStepStatesAndMission(String[] states, String mission, int limit) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findOrderedByJobStepStateAndMission({}, {}, {})", states, mission, limit);

		String statesString = "(";
		int i = 0;
		for (String state : states) {
			statesString += ("'" + state + "'");
			i++;
			if (i < states.length) {
				statesString += ", ";
			}
		}
		statesString += ")";
		String query = "select js from JobStep js " + "inner join Job j on js.job.id = j.id "
				+ "inner join ProcessingOrder o on j.processingOrder.id = o.id " + "inner join Mission m on o.mission.id = m.id "
				+ "where js.processingStartTime is not null and js.jobStepState in " + statesString + " and m.code = '" + mission
				+ "' order by js.processingCompletionTime desc";
		// em.createNativeQ
		return em.createQuery(query, JobStep.class).setMaxResults(limit).getResultList();
	}

}