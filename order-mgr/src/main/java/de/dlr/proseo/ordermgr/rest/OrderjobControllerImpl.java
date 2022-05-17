package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.rest.OrderjobController;
import de.dlr.proseo.model.rest.model.RestJob;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.ordermgr.Messages;
import de.dlr.proseo.ordermgr.rest.model.RestUtil;

/**
 * Controller for the prosEO Order Manager, implements the services required to manage jobs.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class OrderjobControllerImpl implements OrderjobController {
	private static Logger logger = LoggerFactory.getLogger(OrderjobControllerImpl.class);

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;
	
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
	@Override
	@Transactional(readOnly = true)
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
	@Transactional(readOnly = true)
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
			query.setParameter("state", JobStepState.valueOf(state));
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