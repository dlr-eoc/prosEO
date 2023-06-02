package de.dlr.proseo.facmgr.rest;

import java.util.ConcurrentModificationException;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.facmgr.rest.model.RestProcessingFacility;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * Spring MVC controller for the prosEO Facility Manager; implements the
 * services required to manage processing facilities
 *
 * @author Ranjitha Vignesh
 *
 */
@Component
public class FacmgrControllerImpl implements FacilityController {

	private static ProseoLogger logger = new ProseoLogger(FacmgrControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.FACILTY_MGR);

	/** The processing facility manager */
	@Autowired
	private FacmgrManager procFacilityManager;

	/**
	 * Create a facility from the given Json object
	 *
	 * @param restFacility the Json object to create the facility from
	 * @return HTTP status "CREATED" and a response containing a Json object
	 *         corresponding to the facility after persistence (with ID and version
	 *         for all contained objects) or HTTP status "BAD_REQUEST", if any of
	 *         the input data was invalid
	 */
	@Override
	public ResponseEntity<RestProcessingFacility> createFacility(RestProcessingFacility restFacility) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createFacility({})", (null == restFacility ? "MISSING" : restFacility.getName()));

		try {
			return new ResponseEntity<>(procFacilityManager.createFacility(restFacility), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Retrieve a list of facilities filtered by mission and name.
	 *
	 * @param name the name of the facility
	 * @return a response entity with either a list of facilities and HTTP status
	 *         "OK" or an error message and HTTP status "NOT_FOUND" if no matching
	 *         facilities were found
	 */
	@Override
	public ResponseEntity<List<RestProcessingFacility>> getFacilities(String name) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getFacilities( {})", name);

		try {
			return new ResponseEntity<>(procFacilityManager.getFacility(name), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Find the facility with the given ID
	 *
	 * @param id the ID to look for
	 * @return a response entity corresponding to the found facility and HTTP status
	 *         "OK" or an error message and HTTP status "NOT_FOUND", if no facility
	 *         with the given ID exists
	 */
	@Override
	public ResponseEntity<RestProcessingFacility> getFacilityById(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getFacilityById({})", id);

		try {
			return new ResponseEntity<>(procFacilityManager.getFacilityById(id), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Delete a facility by ID
	 *
	 * @param id the ID of the facility to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was
	 *         successful, "BAD_REQUEST", if the facility still has stored products,
	 *         "NOT_FOUND", if the facility did not exist, or "NOT_MODIFIED", if the
	 *         deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteFacilityById(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteFacilityById({})", id);

		try {
			procFacilityManager.deleteFacilityById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * Update the facility with the given ID with the attribute values of the given
	 * Json object. Both modified and unmodified attributes need to be provided.
	 *
	 * @param id           the ID of the facility to update
	 * @param restFacility a Json object containing the modified and unmodified
	 *                     attributes
	 * @return a response containing HTTP status "OK" and a Json object
	 *         corresponding to the facility after modification (with ID and version
	 *         for all contained objects) or HTTP status "NOT_MODIFIED" and the
	 *         unchanged facility, if no attributes were actually changed, or HTTP
	 *         status "NOT_FOUND" and an error message, if no facility with the
	 *         given ID exists
	 */
	@Override
	public ResponseEntity<RestProcessingFacility> modifyFacility(Long id, RestProcessingFacility restFacility) {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyFacility({})", id);

		try {
			RestProcessingFacility changedFacility = procFacilityManager.modifyFacility(id, restFacility);
			HttpStatus httpStatus = (restFacility.getVersion() == changedFacility.getVersion() ? HttpStatus.NOT_MODIFIED
					: HttpStatus.OK);
			return new ResponseEntity<>(changedFacility, httpStatus);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		}
	}

}