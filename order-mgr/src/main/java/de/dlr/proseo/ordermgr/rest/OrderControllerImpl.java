/**
 * OrderControllerImpl.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.rest.OrderController;
import de.dlr.proseo.model.rest.model.RestOrder;

/**
 * Spring MVC controller for the prosEO Order Manager; implements the services required to manage processing orders
 *
 * @author Ranjitha Vignesh
 */
@Component
public class OrderControllerImpl implements OrderController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderControllerImpl.class);

	/** HTTP utility class */
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.ORDER_MGR);

	/** The processing order manager */
	@Autowired
	private ProcessingOrderMgr procOrderManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/**
	 * Create an order from the given JSON object
	 *
	 * @param restOrder the JSON object to create the order from
	 * @return HTTP status "CREATED" and a response containing a JSON object corresponding to the order after persistence (with ID
	 *         and version for all contained objects) or HTTP status "FORBIDDEN" and an error message, if a cross-mission data
	 *         access was attempted, or HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestOrder> createOrder(RestOrder restOrder) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createOrder({})", (null == restOrder ? "MISSING" : restOrder.getIdentifier()));

		try {
			return new ResponseEntity<>(procOrderManager.createOrder(restOrder), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * List of all orders filtered by mission, identifier, productClasses, starttime range
	 *
	 * @param mission           the mission code
	 * @param identifier        the unique order identifier string
	 * @param productClasses    an array of product types
	 * @param startTimeFrom     earliest sensing start time
	 * @param startTimeTo       latest sensing start time
	 * @param executionTimeFrom earliest order execution time
	 * @param executionTimeTo   latest order execution time
	 * @return HTTP status "OK" and a list of products or HTTP status "NOT_FOUND" and an error message, if no products matching the
	 *         search criteria were found, or HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was
	 *         attempted
	 */
	@Override
	public ResponseEntity<List<RestOrder>> getOrders(String mission, String identifier, String[] productClasses,
			@DateTimeFormat Date startTimeFrom, @DateTimeFormat Date startTimeTo, @DateTimeFormat Date executionTimeFrom,
			@DateTimeFormat Date executionTimeTo) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getOrders({}, {}, {}, {}, {})", mission, identifier, productClasses, startTimeFrom, startTimeTo,
					executionTimeFrom, executionTimeTo);

		try {
			return new ResponseEntity<>(procOrderManager.getOrders(mission, identifier, productClasses, startTimeFrom, startTimeTo,
					executionTimeFrom, executionTimeTo), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Retrieve a list of orders satisfying the selection parameters
	 *
	 * @param mission       the mission code
	 * @param identifier    the unique order identifier string
	 * @param state         an array of order states
	 * @param productClass  an array of product types
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo   latest sensing start time
	 * @param recordFrom    first record of filtered and ordered result to return
	 * @param recordTo      last record of filtered and ordered result to return
	 * @param orderBy       an array of strings containing a column name and an optional sort direction (ASC/DESC), separated by
	 *                      white space
	 * @return the result list
	 */
	@Override
	public ResponseEntity<List<RestOrder>> getAndSelectOrders(String mission, String identifier, String[] state,
			String[] productClass, String startTimeFrom, String startTimeTo, Long recordFrom, Long recordTo, String[] orderBy) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getAndSelectOrders({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})", mission, identifier, state,
					productClass, startTimeFrom, startTimeTo, recordFrom, recordTo, orderBy);

		try {
			List<RestOrder> list = procOrderManager.getAndSelectOrders(mission, identifier, state, productClass, startTimeFrom,
					startTimeTo, recordFrom, recordTo, orderBy);

			return new ResponseEntity<>(list, HttpStatus.OK);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (Exception e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Calculate the amount of orders satisfying the selection parameters. Mission code is mandatory.
	 *
	 * @param mission       the mission code
	 * @param identifier    the unique order identifier string
	 * @param state         an array of order states
	 * @param productClass  an array of product types
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo   latest sensing start time
	 * @param recordFrom    first record of filtered and ordered result to return
	 * @param recordTo      last record of filtered and ordered result to return
	 * @param orderBy       an array of strings containing a column name and an optional sort direction (ASC/DESC),separated by
	 *                      white space
	 * @return The order count
	 */
	@Override
	public ResponseEntity<String> countSelectOrders(String mission, String identifier, String[] state, String[] productClass,
			String startTimeFrom, String startTimeTo, Long recordFrom, Long recordTo, String[] orderBy) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getAndSelectOrders({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})", mission, identifier, state,
					productClass, startTimeFrom, startTimeTo, recordFrom, recordTo, orderBy);

		try {
			String count = procOrderManager.countSelectOrders(mission, identifier, state, productClass, startTimeFrom, startTimeTo,
					recordFrom, recordTo, orderBy);

			return new ResponseEntity<>(count, HttpStatus.OK);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (Exception e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Find the order with the given ID
	 *
	 * @param id the ID to look for
	 * @return HTTP status "OK" and a JSON object corresponding to the found order or HTTP status "FORBIDDEN" and an error message,
	 *         if a cross-mission data access was attempted, or HTTP status "NOT_FOUND", if no orbit with the given ID exists
	 */
	@Override
	public ResponseEntity<RestOrder> getOrderById(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getOrderById({})", id);
		try {
			return new ResponseEntity<>(procOrderManager.getOrderById(id), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Delete an order by ID
	 *
	 * @param id the ID of the order to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or HTTP status "NOT_FOUND" and an
	 *         error message, if the orbit did not exist, or HTTP status "FORBIDDEN" and an error message, if a cross-mission data
	 *         access was attempted, or HTTP status "NOT_MODIFIED" and an error message, if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteOrderById(Long id) {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteOrderById({})", id);

		try {
			procOrderManager.deleteOrderById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * Update the order with the given ID with the attribute values of the given JSON object.
	 *
	 * @param id        the ID of the order to update
	 * @param restOrder a JSON object containing the modified (and unmodified) attributes
	 * @return a response containing HTTP status "OK" and a JSON object corresponding to the order after modification (with ID and
	 *         version for all contained objects) or HTTP status "NOT_MODIFIED" and the unchanged order, if no attributes were
	 *         actually changed, or HTTP status "NOT_FOUND" and an error message, if no order with the given ID exists, or HTTP
	 *         status "FORBIDDEN" and an error message, if a cross-mission data access was attempted
	 */
	// TODO To be Tested
	@Override
	public ResponseEntity<RestOrder> modifyOrder(Long id, @Valid RestOrder restOrder) {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyOrder({})", id);
		try {
			RestOrder changedOrder = procOrderManager.modifyOrder(id, restOrder);
			HttpStatus httpStatus = (restOrder.getVersion() == changedOrder.getVersion() ? HttpStatus.NOT_MODIFIED : HttpStatus.OK);
			return new ResponseEntity<>(changedOrder, httpStatus);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Count orders filtered by mission, identifier and id not equal nid.
	 *
	 * @param mission    The mission code
	 * @param identifier The unique order identifier string
	 * @param nid        The ids of orbit(s) found has to be unequal to nid
	 * @return The number of orders found
	 */
	@Transactional
	@Override
	public ResponseEntity<String> countOrders(String mission, String identifier, Long nid) {
		if (logger.isTraceEnabled())
			logger.trace(">>> contOrders{}");

		// Find using search parameters
		String jpqlQuery = "select count(x) from ProcessingOrder x ";
		String divider = " where ";
		if (mission != null) {
			jpqlQuery += divider + " x.mission.code = :mission";
			divider = " and ";
		}
		if (null != identifier) {
			jpqlQuery += divider + " x.identifier = :identifier";
			divider = " and ";
		}
		if (null != nid) {
			jpqlQuery += divider + " x.id <> :nid";
			divider = " and ";
		}

		Query query = em.createQuery(jpqlQuery);
		if (null != mission) {
			query.setParameter("mission", mission);
		}
		if (null != identifier) {
			query.setParameter("identifier", identifier);
		}
		if (null != identifier) {
			query.setParameter("nid", nid);
		}

		Object resultObject = query.getSingleResult();
		if (resultObject instanceof Long) {
			return new ResponseEntity<>(((Long) resultObject).toString(), HttpStatus.OK);
		}
		if (resultObject instanceof String) {
			return new ResponseEntity<>((String) resultObject, HttpStatus.OK);
		}

		return new ResponseEntity<>("0", HttpStatus.OK);
	}

}