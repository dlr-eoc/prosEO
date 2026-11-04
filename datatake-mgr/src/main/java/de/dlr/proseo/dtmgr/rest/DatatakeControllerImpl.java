/**
 * S1DatatakeControllerImpl
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.dtmgr.rest;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.dtmgr.rest.model.RestDatatake;
import jakarta.persistence.EntityNotFoundException;

/**
 * Spring MVC controller for the prosEO Mission Planning Monitor; implements the services required to handle datatakes and segments
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class DatatakeControllerImpl implements DatatakeController {

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-datatake-mgr ";

	/** The datatake manager */
	@Autowired
	DatatakeManager datatakeManager;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(DatatakeControllerImpl.class);
	
	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + (null == message ? "null" : message.replaceAll("\n", " ")));
		return responseHeaders;
	}
	
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
	//public ResponseEntity<List<RestDatatake>> getRestDatatakes(String spacecraft, Long from, Long to, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getRestDatatakes({}, {}, {}, {}, {})", spacecraftCode, payload, datatakeId, startTimeFrom, startTimeTo);
		
		try {
			return new ResponseEntity<>(
					datatakeManager.getRestDatatakes(spacecraftCode, payload, datatakeId, startTimeFrom, startTimeTo, 
							recordFrom, recordTo, orderBy), 
					HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Mark the datatake with the given datatake ID as completed (sets processing time)
	 * 
	 * @param id the database ID of the datatake
     * @return HTTP status "OK" and the JSON representation of the updated datatake, or
	 *         HTTP status "NOT_FOUND" and an error message, if the spacecraft code or the datatake ID cannot be found
	 */
	@Override
	public ResponseEntity<RestDatatake> markProcessed(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> markProcessed({})", id);
		
		try {
			return new ResponseEntity<>(
					datatakeManager.markProcessed(id), HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	@Override
	public ResponseEntity<RestDatatake> createDatatake(RestDatatake restDatatake) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> countDatatakes(String spacecraftCode, String payload, String datatakeId, String startTimeFrom,
			String startTimeTo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestDatatake> getDatatakeById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteDatatakeById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestDatatake> modifyDatatake(Long id, RestDatatake restDatatake) {
		// TODO Auto-generated method stub
		return null;
	}

}
