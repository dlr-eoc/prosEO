/**
 * TriggerControllerImpl.java
 *
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordergen.rest;

import java.util.List;

import javax.validation.Valid;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.rest.TriggerController;
import de.dlr.proseo.model.rest.model.RestTrigger;

/**
 * Implementation of the TriggerController
 *
 * @author Ernst Melchinger
 *
 */
@Component
public class TriggerControllerImpl implements TriggerController {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(TriggerControllerImpl.class);
	
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.ORDERGEN);

	@Autowired
	private TriggerManager triggerManager;
	
	/**
	 * Get a List of OrderTriggers filtered by mission, name and type.
	 * 
	 * @param mission 			the mission code
	 * @param name				the trigger name
	 * @param type				the trigger type
	 * @param httpHeaders       the HTTP request headers (injected)
	 * @return HTTP status "OK" and the list of Json representation of the triggers found 
	 * 		   or HTTP status "FORBIDDEN" and an error message, if a
	 *         cross-mission data access was attempted
	 */
	@Override
	public ResponseEntity<List<RestTrigger>> getTriggers(String mission, String name, String type,
			String workflow, String inputProductClass, String outputProductClass, Integer recordFrom, 
			Integer recordTo, String[] orderBy, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getTriggers({}, {}, {}, {}, {}, {})", mission, name, type, workflow, inputProductClass, outputProductClass);

		try {
			return new ResponseEntity<>(triggerManager.getTriggers(mission, name, type, workflow, inputProductClass, outputProductClass,
					 recordFrom, recordTo, orderBy), 
					HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	@Override
	public ResponseEntity<?> countTriggers(String mission, String name, String type, String workflow,
			String inputProductClass, String outputProductClass, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countTriggers({}, {}, {}, {}, {}, {})", mission, name, type, workflow, inputProductClass, outputProductClass);

		try {
			return new ResponseEntity<>(triggerManager.countTriggers(mission, name, type, workflow, inputProductClass, outputProductClass), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}
	
	/**
	 * Create a trigger from the given Json object
	 * 
	 * @param restTrigger the Json object to create the trigger from
	 * @param httpHeaders HTTP Authentication header
	 * @return HTTP status "CREATED" and a response containing a Json object
	 *         corresponding to the trigger after persistence (with ID and version
	 *         for all contained objects) or HTTP status "FORBIDDEN" and an error
	 *         message, if a cross-mission data access was attempted, or HTTP status
	 *         "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestTrigger> createTrigger(@Valid RestTrigger restTrigger, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createTrigger({})", (null == restTrigger ? "MISSING" : restTrigger.getName()));

		try {
			return new ResponseEntity<>(triggerManager.createTrigger(restTrigger), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}   
	
	/**
     * Delete all triggers filtered by mission, name, type.
     * 
	 * @param mission 			the mission code
	 * @param name				the trigger name
	 * @param type				the trigger type
	 * @param httpHeaders       the HTTP request headers (injected)
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was
	 *         successful, or HTTP status "NOT_FOUND", if the trigger did not exist,
	 *         or HTTP status "FORBIDDEN" and an error message,
	 *         if a cross-mission data access was attempted, or HTTP status
	 *         "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
    public ResponseEntity<Object> deleteTrigger(String mission, String name, String type, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteTrigger({}, {}, {})", mission, name, type);

		try {
			triggerManager.deleteTrigger(mission, name, type);
			return new ResponseEntity<Object>(HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

    /**
     * Update a trigger from the given JSON object
     * 
	 * @param restTrigger the Json object to create the trigger from
	 * @param httpHeaders HTTP Authentication header
	 * @return HTTP status "OK" and a response containing a Json object
	 *         corresponding to the trigger after persistence (with ID and version
	 *         for all contained objects) or HTTP status "FORBIDDEN" and an error
	 *         message, if a cross-mission data access was attempted, or HTTP status
	 *         "BAD_REQUEST", if any of the input data was invalid
     */
	@Override
    public ResponseEntity<RestTrigger> updateTrigger(RestTrigger restTrigger, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateTrigger({})", (null == restTrigger ? "MISSING" : restTrigger.getName()));

		try {
			RestTrigger changedTrigger = triggerManager.modifyTrigger(restTrigger);
			HttpStatus httpStatus = (restTrigger.getVersion() == changedTrigger.getVersion() ? HttpStatus.NOT_MODIFIED
					: HttpStatus.OK);
			return new ResponseEntity<>(changedTrigger, httpStatus);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Get all data driven triggers for workflows having the given product class as input product class
	 * 
	 * @param mission the mission code
	 * @param productType the product type of the requested product class
	 * @return HTTP status "OK" and the list of Json representation of the triggers found 
	 *         or HTTP status "BAD_REQUEST" and an error message, if the given product type is invalid,
	 * 		   or HTTP status "FORBIDDEN" and an error message, if a
	 *         cross-mission data access was attempted
	 */
	@Override
	public ResponseEntity<List<RestTrigger>> getByProductType(String mission, String productType, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getByProductType({}, {})", mission, productType);

		try {
			return new ResponseEntity<>(triggerManager.getByProductType(mission, productType), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Reload and restart all triggers (mission is not used)
	 */
	@Override
	public ResponseEntity<Object> reloadTriggers(String mission, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> reloadTriggers({})", mission);

		try {
			triggerManager.reloadTriggers(mission);
			return new ResponseEntity<Object>(HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (SchedulerException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
