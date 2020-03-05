package de.dlr.proseo.facmgr.rest;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import de.dlr.proseo.facmgr.rest.model.RestFacility;


/**
 * Spring MVC controller for the prosEO Facility Manager; implements the services required to manage processing facilities
 * 
 * @author Ranjitha Vignesh
 *
 */
@Component
public class FacmgrControllerImpl implements FacilityController{
	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-facmgr-facmgrcontroller ";

	private static Logger logger = LoggerFactory.getLogger(FacmgrControllerImpl.class);
	
	/** The processing facility manager */
	@Autowired
	private FacmgrManager procFacilityManager;
	
	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + message.replaceAll("\n", " "));
		return responseHeaders;
	}
	
	/**
	 * Create a facility from the given Json object 
	 * 
	 * @param order the Json object to create the order from
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the facility after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */

	@Override
	public ResponseEntity<RestFacility> createFacility(RestFacility facility) {	
		if (logger.isTraceEnabled()) logger.trace(">>> createFacility({})", (null == facility ? "MISSING" : facility.getName()));
		
		try {
			return new ResponseEntity<>(procFacilityManager.createFacility(facility), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}	
	}
	
	/**
	 * List of all facilities with no search criteria
	 * @param mission the mission code
	 * @param name the unique facility name
	 * @return a response entity with either a list of facilities and HTTP status OK or an error message and an HTTP status indicating failure
	 */
	@Override
	public ResponseEntity<List<RestFacility>> getFacilities(String name) {
		if (logger.isTraceEnabled()) logger.trace(">>> getFacilitys( {})",  name);
		
		try {
			return new ResponseEntity<>(procFacilityManager.getFacility(name), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}
	/**
	 * Find the facility with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a response entity corresponding to the found facility and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no facility with the given ID exists
	 */
	@Override
	public ResponseEntity<RestFacility> getFacilityById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getFacilityById({})", id);
		try {
			return new ResponseEntity<>(procFacilityManager.getFacilityById(id), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
		
	}
	/**
	 * Delete a facility by ID
	 * 
	 * @param the ID of the facility to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, "NOT_FOUND", if the facility did not
	 *         exist, or "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteFacilityById(Long id) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> deleteFacilityById({})", id);

		try {
			procFacilityManager.deleteFacilityById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}
	/**
	 * Update the facility with the given ID with the attribute values of the given Json object. 
	 * @param id the ID of the facility to update
	 * @param restFacility a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the facility after modification (with ID and version for all 
	 * 		   contained objects) and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no facility with the given ID exists
	 */
	@Override
	public ResponseEntity<RestFacility> modifyFacility(Long id, RestFacility restFacility) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyOrder({})", id);
		try {
			RestFacility changedOrder = procFacilityManager.modifyFacility(id, restFacility);
			HttpStatus httpStatus = (restFacility.getVersion() == changedOrder.getVersion() ? HttpStatus.NOT_MODIFIED : HttpStatus.OK);
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
