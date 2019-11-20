/**
 * OrderControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.rest.model.OrderUtil;
import de.dlr.proseo.ordermgr.rest.model.RestOrder;

/**
 * Spring MVC controller for the prosEO Order Manager; implements the services required to manage processing orders
 * 
 * @author Ranjitha Vignesh
 *
 */
@Component
public class OrderControllerImpl implements OrderController {
	
	/* Message ID constants */
	private static final int MSG_ID_ORDER_NOT_FOUND = 1007;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 1004;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	private static final int MSG_ID_ORDER_MISSING = 1008;


	/* Message string constants */
	private static final String MSG_ORDER_NOT_FOUND = "No order found for ID %d (%d)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "Order deletion unsuccessful for ID %d (%d)";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-ordermgr-ordercontroller ";
	private static final String MSG_ORDER_MISSING = "(E%d) Order not set";

	private static Logger logger = LoggerFactory.getLogger(OrderControllerImpl.class);
	
	/** The product manager */
	@Autowired
	private ProcessingOrderMgr procOrderManager;
	
	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + message);
		return responseHeaders;
	}
	/**
	 * Create a order from the given Json object 
	 * 
	 * @param order the Json object to create the order from
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the product after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */

	@SuppressWarnings("unchecked")
	@Override
	public ResponseEntity<RestOrder> createOrder(RestOrder order) {	
		if (logger.isTraceEnabled()) logger.trace(">>> createOrder({})", (null == order ? "MISSING" : order.getIdentifier()));
		
		try {
			return new ResponseEntity<>(procOrderManager.createOrder(order), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}	
	}
	/**
	 * List of all order filtered by mission, identifier, productClasses, starttime range
	 * 
	 * @param mission the mission code
	 * @param identifier the unique order identifier string
	 * @param productClass an array of product types
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo latest sensing start time
	 * @return HTTP status "OK" and a list of products or
	 *         HTTP status "NOT_FOUND" and an error message, if no products matching the search criteria were found
	 */
	@Override
	public ResponseEntity<List<RestOrder>> getOrders(String mission, String identifier, String[] productclasses, @DateTimeFormat Date starttimefrom,
			@DateTimeFormat Date starttimeto) {
		if (logger.isTraceEnabled()) logger.trace(">>> getOrders({}, {}, {}, {}, {})", mission, identifier, productclasses, starttimefrom, starttimeto);
		
		try {
			return new ResponseEntity<>(
					procOrderManager.getOrders(mission, identifier, productclasses, starttimefrom, starttimeto), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}
	/**
	 * Find the order with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a Json object corresponding to the found order and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no orbit with the given ID exists
	 */
	@Override
	public ResponseEntity<RestOrder> getOrderById(Long id) {
		// TODO Auto-generated method stub
		if (logger.isTraceEnabled()) logger.trace(">>> getOrderById({})", id);
		try {
			return new ResponseEntity<>(procOrderManager.getOrderById(id), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}
	/**
	 * Delete an order by ID
	 * 
	 * @param the ID of the order to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, "NOT_FOUND", if the orbit did not
	 *         exist, or "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteOrderById(Long id) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> deleteOrderById({})", id);

		try {
			procOrderManager.deleteOrderById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}
	/**
	 * Update the order with the given ID with the attribute values of the given Json object. 
	 * @param id the ID of the order to update
	 * @param orbit a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the order after modification (with ID and version for all 
	 * 		   contained objects) and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no order with the given ID exists
	 */

	// To be Tested
	@Override
	public ResponseEntity<RestOrder> modifyOrder(Long id, @Valid RestOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyOrder({})", id);
		try {
			RestOrder changedOrder = procOrderManager.modifyOrder(id, order);
			HttpStatus httpStatus = (order.getVersion() == changedOrder.getVersion() ? HttpStatus.NOT_MODIFIED : HttpStatus.OK);
			return new ResponseEntity<>(changedOrder, httpStatus);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		}
	
	}
	

}
