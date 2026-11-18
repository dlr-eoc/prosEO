/**
 * DatatakeControllerImpl
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.dtmgr.rest;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.dtmgr.rest.model.RestDatatake;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import jakarta.persistence.EntityNotFoundException;

/**
 * Spring MVC controller for the prosEO Datatake Manager; implements the services required to handle datatakes and segments
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class DatatakeControllerImpl implements DatatakeController {

	/** The datatake manager */
	@Autowired
	DatatakeManager datatakeManager;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(DatatakeControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.DATATAKE_MGR);
	
	/**
	 * Get the datatakes in the database (optionally using a lower and/or upper bound of the datatake ID)
	 * 
	 * @param spacecraftCode the spacecraft code (optional)
	 * @param payload the spacecraft payload name (optional)
	 * @param datatakeId the (external) datatake ID (optional)
	 * @param startTimeFrom earliest sensing start time (optional)
	 * @param startTimeTo latest sensing start time (optional)
	 * @param recordFrom first record of filtered and ordered result to return (optional, default first)
	 * @param recordTo last record of filtered and ordered result to return (optional, default last)
	 * @param orderBy array of strings containing a column name and an optional sort direction (ASC/DESC), may be empty
     * @return HTTP status "OK" and the Json representation of a datatake list, or
	 *         HTTP status "BAD_REQUEST" and an error message, if the last record number is below the first record number, or
	 *         HTTP status "NOT_FOUND" and an error message, if no datatakes with the given search criteria can be found
	 */
	@Override
	public ResponseEntity<List<RestDatatake>> getDatatakes(String spacecraftCode, String payload, String datatakeId,
			String startTimeFrom, String startTimeTo, Long recordFrom, Long recordTo, String[] orderBy) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getDatatakes({}, {}, {}, {}, {})", spacecraftCode, payload, datatakeId, startTimeFrom, startTimeTo);
		
		try {
			return new ResponseEntity<>(
					datatakeManager.getDatatakes(spacecraftCode, payload, datatakeId, startTimeFrom, startTimeTo, 
							recordFrom, recordTo, orderBy), 
					HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Mark the datatake with the given datatake ID as completed (sets processing time for L0 product)
	 * 
	 * @param id the database ID of the datatake
     * @return HTTP status "OK" and the JSON representation of the updated datatake, or
	 *         HTTP status "NOT_FOUND" and an error message, if no datatake with the given database ID can be found, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted
	 */
	@Override
	public ResponseEntity<RestDatatake> markProcessed(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> markProcessed({})", id);
		
		try {
			return new ResponseEntity<>(
					datatakeManager.markProcessed(id), HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Create a datatake from the given JSON object
	 * 
	 * @param restDatatake a JSON representation of a datatake
	 * @return HTTP status "CREATED" and the JSON representation of the new datatake, or
	 *         HTTP status "BAD_REQUEST" and an error message, if the request is not processable, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted
	 */
	@Override
	public ResponseEntity<RestDatatake> createDatatake(RestDatatake restDatatake) {
		if (logger.isTraceEnabled()) logger.trace(">>> createDatatake({})", 
				null == restDatatake ? null : restDatatake.getDatatakeId());
		
		try {
			return new ResponseEntity<>(
					/* TODO */ HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Count datatakes filtered by spacecraft, payload, datatake ID, start time range;
	 * only datatakes belonging to the mission the user is logged in to will be counted
	 * 
	 * @param spacecraftCode the spacecraft code (optional)
	 * @param payload the spacecraft payload name (optional)
	 * @param datatakeId the (external) datatake ID (optional)
	 * @param startTimeFrom earliest sensing start time (optional)
	 * @param startTimeTo latest sensing start time (optional)
	 * @return HTTP status "OK" and the number of datatakes as plain text, or
	 *         HTTP status "NOT_FOUND" and an error message, if no datatakes with the given search criteria can be found
	 */
	@Override
	public ResponseEntity<?> countDatatakes(String spacecraftCode, String payload, String datatakeId, String startTimeFrom,
			String startTimeTo) {
		if (logger.isTraceEnabled()) logger.trace(">>> countDatatakes({}, {}, {}, {}, {})", 
				spacecraftCode, payload, datatakeId, startTimeFrom, startTimeTo);
		
		try {
			return new ResponseEntity<>(
					/* TODO */ HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Find the datatake with the given database ID
	 * 
	 * @param id the database ID of the datatake
     * @return HTTP status "OK" and the JSON representation of the requested datatake, or
	 *         HTTP status "NOT_FOUND" and an error message, if no datatake with the given database ID can be found, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted
	 */
	@Override
	public ResponseEntity<RestDatatake> getDatatakeById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getDatatakeById({})", id);
		
		try {
			return new ResponseEntity<>(
					/* TODO */ HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Delete a datatake by database ID
	 * 
	 * @param id the database ID of the datatake
     * @return HTTP status "NO_CONTENT" and an empty body on success, or
	 *         HTTP status "NOT_FOUND" and an error message, if no datatake with the given database ID can be found, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted
	 */
	@Override
	public ResponseEntity<?> deleteDatatakeById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteDatatakeById({})", id);
		
		try {
			return new ResponseEntity<>(
					/* TODO */ HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Update the datatake with the given database ID from the given JSON object
	 * 
	 * @param id the database ID of the datatake
	 * @param restDatatake a JSON object containing the modified (and unmodified) attributes
     * @return HTTP status "OK" and the JSON representation of the updated datatake, or
	 *         HTTP status "NOT_FOUND" and an error message, if no datatake with the given database ID can be found, or
	 *         HTTP status "BAD_REQUEST" and an error message, if the request is not processable, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted
	 */
	@Override
	public ResponseEntity<RestDatatake> modifyDatatake(Long id, RestDatatake restDatatake) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyDatatake({}, {})", id,
				null == restDatatake ? null : restDatatake.getDatatakeId());
		
		try {
			return new ResponseEntity<>(
					/* TODO */ HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}
