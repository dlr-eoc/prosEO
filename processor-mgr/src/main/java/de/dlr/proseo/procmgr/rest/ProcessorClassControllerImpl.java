/**
 * ProcessorClassControllerImpl.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ConcurrentModificationException;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.procmgr.rest.model.RestProcessorClass;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage processor classes.
 *
 * @author Dr. Thomas Bassler
 */
@Component
public class ProcessorClassControllerImpl implements ProcessorclassController {

	/** The processor class manager */
	@Autowired
	private ProcessorClassManager processorClassManager;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProcessorClassControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PROCESSOR_MGR);

	/**
	 * Get processor classes by mission and name
	 *
	 * @param mission       the mission code (optional)
	 * @param processorName the processor name (optional)
	 * @param recordFrom    first record of filtered and ordered result to return
	 * @param recordTo      last record of filtered and ordered result to return
	 * @param orderBy		an array of strings containing a column name and an optional sort direction (ASC/DESC), separated by white space
	 * @return HTTP status "OK" and a list of Json objects representing processor classes satisfying the search criteria or HTTP
	 *         status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or HTTP status "NOT_FOUND" and
	 *         an error message, if no processor classes matching the search criteria were found HTTP status "TOO MANY REQUESTS" if
	 *         the result list exceeds a configured maximum
	 */
	@Override
	public ResponseEntity<List<RestProcessorClass>> getProcessorClasses(String mission, Long id, String[] productClass, String processorName, Integer recordFrom,
			Integer recordTo, String[] orderBy) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessorClass({}, {}, {}, {})", mission, id, productClass, processorName);

		try {
			return new ResponseEntity<>(processorClassManager.getProcessorClasses(mission, id, productClass, processorName, recordFrom, recordTo, orderBy),
					HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Create a new processor class
	 *
	 * @param processorClass a Json representation of the new processor class
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the processor class after persistence
	 *         (with ID and version for all contained objects) or HTTP status "FORBIDDEN" and an error message, if a cross-mission
	 *         data access was attempted, or HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestProcessorClass> createProcessorClass(@Valid RestProcessorClass processorClass) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createProcessorClass({})", (null == processorClass ? "MISSING" : processorClass.getProcessorName()));

		try {
			return new ResponseEntity<>(processorClassManager.createProcessorClass(processorClass), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Get a processor class by ID
	 *
	 * @param id the processor class ID
	 * @return HTTP status "OK" and a Json object corresponding to the processor class found or HTTP status "BAD_REQUEST" and an
	 *         error message, if no processor class ID was given, or HTTP status "FORBIDDEN" and an error message, if a
	 *         cross-mission data access was attempted, or HTTP status "NOT_FOUND" and an error message, if no processor class with
	 *         the given ID exists
	 */
	@Override
	public ResponseEntity<RestProcessorClass> getProcessorClassById(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessorClassById({})", id);

		try {
			return new ResponseEntity<>(processorClassManager.getProcessorClassById(id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Update a processor class by ID
	 *
	 * @param id             the ID of the processor class to update
	 * @param processorClass a Json object containing the modified (and unmodified) attributes
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the processor class after modification
	 *         (with ID and version for all contained objects) or HTTP status "NOT_MODIFIED" and the unchanged product, if no
	 *         attributes were actually changed, or HTTP status "NOT_FOUND" and an error message, if no processor class with the
	 *         given ID exists, or HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or HTTP
	 *         status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or HTTP status "CONFLICT"and
	 *         an error message, if the processor class has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestProcessorClass> modifyProcessorClass(Long id, @Valid RestProcessorClass processorClass) {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyProcessorClass({}, {})", id,
					(null == processorClass ? "MISSING" : processorClass.getProcessorName()));

		try {
			RestProcessorClass changedProcessorClass = processorClassManager.modifyProcessorClass(id, processorClass);
			HttpStatus httpStatus = (processorClass.getVersion() == changedProcessorClass.getVersion() ? HttpStatus.NOT_MODIFIED
					: HttpStatus.OK);
			return new ResponseEntity<>(changedProcessorClass, httpStatus);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		}
	}

	/**
	 * Delete a processor class by ID
	 *
	 * @param id the ID of the processor class to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or HTTP status "NOT_FOUND", if the
	 *         processor class did not exist, or HTTP status "NOT_MODIFIED", if the deletion was unsuccessful, or HTTP status
	 *         "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or HTTP status "BAD_REQUEST", if the
	 *         processor class ID was not given, or if dependent objects exist
	 */
	@Override
	public ResponseEntity<?> deleteProcessorClassById(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteProcessorClassById({})", id);

		try {
			processorClassManager.deleteProcessorClassById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * Count the processor classes matching the specified mission and processor name.
	 *
	 * @param missionCode          the mission code
	 * @param processorName        the processor name
	 * @return the number of matching configurations as a String (may be zero) or HTTP status "FORBIDDEN" and an error message, if a
	 *         cross-mission data access was attempted
	 */
	@Override
	public ResponseEntity<String> countProcessorClasses(String missionCode, Long id, String[] productClass, String processorName) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countProcessorClasses({}, {}, {}, {})", missionCode, id, productClass, processorName);

		try {
			return new ResponseEntity<>(processorClassManager.countProcessorClasses(missionCode, id, productClass, processorName), HttpStatus.OK);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}