/**
 * OrderjobControllerImpl.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OrderMgrMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.rest.OrderjobController;
import de.dlr.proseo.model.rest.model.RestJob;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.ordermgr.OrdermgrConfiguration;
import de.dlr.proseo.ordermgr.rest.model.RestUtil;

/**
 * Controller for the prosEO Order Manager, implements the services required to manage jobs. Provides methods for retrieving and
 * counting jobs, as well as finding the index of a job within an ordered list. The class also includes utility methods for creating
 * JPA queries.
 *
 * @author Ernst Melchinger
 */
@Component
public class OrderjobControllerImpl implements OrderjobController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderjobControllerImpl.class);

	/** HTTP utility class */
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.ORDER_MGR);

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** The order manager configuration */
	@Autowired
	private OrdermgrConfiguration config;

	/**
	 * Retrieves production planner jobs, optionally filtered by job state and/or order ID.
	 *
	 * @param states     The job states to filter by.
	 * @param orderId    The order ID to filter by.
	 * @param recordFrom The first record of the filtered and ordered result to return.
	 * @param recordTo   The last record of the filtered and ordered result to return.
	 * @param logs       Whether or not logs are included in the REST job step.
	 * @param orderBy    An array of strings containing a column name and an optional sort direction (ASC/DESC), separated by
	 *                   whitespace.
	 * @return A list of JSON objects describing jobs.
	 */
	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public ResponseEntity<List<RestJob>> getJobs(Long orderId, Integer recordFrom, Integer recordTo, Boolean logs, String[] states,
			String[] orderBy) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getJobs({}, {}, {}, {}, {}, {})", states, orderId, recordFrom, recordTo,
					(null == orderBy ? "null" : Arrays.asList(orderBy)));

		try {
			if (logs == null) {
				logs = true;
			}
			List<RestJob> resultList = new ArrayList<>();

			if (recordFrom == null) {
				recordFrom = 0;
			}
			if (recordTo == null) {
				recordTo = Integer.MAX_VALUE;
			}

			Long numberOfResults = Long.parseLong(this.countJobs(states, orderId).getBody());
			Integer maxResults = config.getMaxResults();
			if (numberOfResults > maxResults && (recordTo - recordFrom) > maxResults
					&& (numberOfResults - recordFrom) > maxResults) {
				throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS,
						logger.log(GeneralMessage.TOO_MANY_RESULTS, "jobs", numberOfResults, config.getMaxResults()));
			}

			Query query = createJobsQuery(states, orderId, recordFrom, recordTo, orderBy, false);

			for (Object resultObject : query.getResultList()) {
				if (resultObject instanceof Job) {
					Job job = (Job) resultObject;
					resultList.add(RestUtil.createRestJob(job, logs));
				}
			}

			if (resultList.isEmpty()) {
				String message = logger.log(OrderMgrMessage.JOBS_FOR_ORDER_NOT_EXIST, String.valueOf(orderId));
				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			}

			logger.log(OrderMgrMessage.JOBS_RETRIEVED, orderId);

			return new ResponseEntity<>(resultList, HttpStatus.OK);
		} catch (HttpClientErrorException.TooManyRequests e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.TOO_MANY_REQUESTS);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Retrieves the number of production planner jobs based on the specified states and order ID.
	 *
	 * @param states  The permitted job states.
	 * @param orderId The order ID of the jobs.
	 * @return The number of jobs.
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	@Override
	public ResponseEntity<String> countJobs(String[] states, Long orderId) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countJobs({}, {})", states, orderId);

		try {
			// Find using search parameters
			Query query = createJobsQuery(states, orderId, null, null, null, true);
			Object resultObject = query.getSingleResult();

			logger.log(OrderMgrMessage.JOBCOUNT_RETRIEVED, orderId);

			if (resultObject instanceof Long) {
				return new ResponseEntity<>(((Long) resultObject).toString(), HttpStatus.OK);
			}
			if (resultObject instanceof String) {
				return new ResponseEntity<>((String) resultObject, HttpStatus.OK);
			}
			return new ResponseEntity<>("0", HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e)),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Retrieves the index of a job in an ordered list of all jobs of an order.
	 *
	 * @param states    The permitted job states (COMPLETED, NON-COMPLETED).
	 * @param orderId   The persistent id of the processing order.
	 * @param jobId     The persistent id of the job.
	 * @param jobStepId The persistent id of the job step.
	 * @param orderBy   An array of strings containing a column name and an optional sort direction (ASC/DESC), separated by
	 *                  whitespace.
	 * @return The index of the job in the ordered list (0 based).
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	@Override
	public ResponseEntity<String> indexOfJob(Long orderId, Long jobId, Long jobStepId, String[] states, String[] orderBy) {
		if (logger.isTraceEnabled())
			logger.trace(">>> indexOfJob({}, {}, {})", orderId, jobId, jobStepId);

		try {
			// Find using search parameters
			String jpqlQuery = null;
			jpqlQuery = "select j.id from Job j where j.processingOrder.id = ";
			if (orderId != null) {
				jpqlQuery += orderId;
			} else if (jobId != null) {
				jpqlQuery += "select jx.processingOrder.id from Job jx where jx.id = " + jobId;
			} else if (jobStepId != null) {
				jpqlQuery += "select js.job.processingOrder.id from JobStep js where js.id = " + jobStepId;
			} else {
				return new ResponseEntity<>("0", HttpStatus.OK);
			}
			if (null != states && states.length == 1) {
				if (states[0].equalsIgnoreCase("COMPLETED")) {
					jpqlQuery += " and j.jobState in ('COMPLETED', 'CLOSED')";
				} else if (states[0].equalsIgnoreCase("NON-COMPLETED")) {
					jpqlQuery += " and j.jobState not in ('COMPLETED', 'CLOSED')";
				}
			}
			// order by
			if (null != orderBy && 0 < orderBy.length) {
				jpqlQuery += " order by ";
				for (int i = 0; i < orderBy.length; ++i) {
					if (0 < i)
						jpqlQuery += ", ";
					jpqlQuery += "j.";
					jpqlQuery += orderBy[i];
				}
			}
			if (jobId == null && jobStepId != null) {
				Object res = em.createQuery("select js.job.id from JobStep js where js.id = " + jobStepId).getSingleResult();
				if (res instanceof Long) {
					jobId = (Long) res;
				}
			}
			if (jobId == null) {
				return new ResponseEntity<>("0", HttpStatus.OK);
			}

			Object resultObject = (long) em.createQuery(jpqlQuery).getResultList().indexOf(jobId);

			logger.log(OrderMgrMessage.JOBINDEX_RETRIEVED, orderId);

			if (resultObject instanceof Long) {
				return new ResponseEntity<>(((Long) resultObject).toString(), HttpStatus.OK);
			}
			if (resultObject instanceof String) {
				return new ResponseEntity<>((String) resultObject, HttpStatus.OK);
			}
			return new ResponseEntity<>("0", HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Creates a JPQL query for jobs, filtering by the mission the user is logged in to, and optionally by job state and/or order
	 * ID.
	 *
	 * @param states     The job states to filter by.
	 * @param orderId    The order ID to filter by.
	 * @param recordFrom The first record of the filtered and ordered result to return.
	 * @param recordTo   The last record of the filtered and ordered result to return.
	 * @param orderBy    An array of strings containing a column name and an optional sort direction (ASC/DESC), separated by
	 *                   whitespace.
	 * @param count      Indicates whether just a count of the orders shall be retrieved or the orders as such.
	 * @return A JPQL query object.
	 */
	private Query createJobsQuery(String[] states, Long orderId, Integer recordFrom, Integer recordTo, String[] orderBy,
			Boolean count) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJobsQuery({}, {}, {}, {}, {}, {}, count: {})", states, orderId, recordFrom, recordTo,
					(null == orderBy ? "null" : Arrays.asList(orderBy)), count);

		// Find using search parameters
		String jpqlQuery = null;
		if (count) {
			jpqlQuery = "select count(x) from Job x";
		} else {
			jpqlQuery = "select x from Job x";
		}
		jpqlQuery += " where x.processingOrder.mission.code = :missionCode";
		if (null != states && states.length == 1) {
			if (states[0].equalsIgnoreCase("COMPLETED")) {
				jpqlQuery += " and x.jobState in ('COMPLETED', 'CLOSED')";
			} else if (states[0].equalsIgnoreCase("NON-COMPLETED")) {
				jpqlQuery += " and x.jobState not in ('COMPLETED', 'CLOSED')";
			}
		}
		if (null != orderId) {
			jpqlQuery += " and x.processingOrder.id = :orderId";
		}
		// order by
		if (null != orderBy && 0 < orderBy.length) {
			jpqlQuery += " order by ";
			for (int i = 0; i < orderBy.length; ++i) {
				if (0 < i)
					jpqlQuery += ", ";
				jpqlQuery += "x.";
				jpqlQuery += orderBy[i];
			}
		}

		Query query = em.createQuery(jpqlQuery);

		query.setParameter("missionCode", securityService.getMission());
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