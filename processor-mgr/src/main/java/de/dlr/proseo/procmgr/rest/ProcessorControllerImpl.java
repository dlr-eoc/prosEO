/**
 * ProcessorControllerImpl.java
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
import de.dlr.proseo.procmgr.rest.model.RestProcessor;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage processor versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProcessorControllerImpl implements ProcessorController {

	/** The processor manager */
	@Autowired
	private ProcessorManager processorManager;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProcessorControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PROCESSOR_MGR);
	
	/**
	 * Create a new processor (version)
	 * 
	 * @param processor a Json representation of the new processor
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the processor after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestProcessor> createProcessor(RestProcessor processor) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProcessor({})", (null == processor ? "MISSING" : processor.getProcessorName()));

		try {
			return new ResponseEntity<>(processorManager.createProcessor(processor), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Get processors by mission, name and version (user-defined version, not database version)
	 * 
	 * @param mission the mission code
	 * @param processorName the name of the processor (class)
	 * @param processorVersion the processor version
	 * @return HTTP status "OK" and a list of Json objects representing processors satisfying the search criteria or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "NOT_FOUND" and an error message, if no processors matching the search criteria were found, or
	 *         HTTP status "TOO MANY REQUESTS" if the result list exceeds a configured maximum
	 */
	@Override
	public ResponseEntity<List<RestProcessor>> getProcessors(String mission, String processorName,
			String processorVersion, Integer recordFrom, Integer recordTo) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessors({}, {}, {})", mission, processorName, processorVersion);

		try {
			return new ResponseEntity<>(
					processorManager.getProcessors(mission, processorName, processorVersion, recordFrom, recordTo),
					HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Get a processor by ID
	 * 
	 * @param id the processor ID
	 * @return HTTP status "OK" and a Json object corresponding to the processor found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no processor ID was given, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no processor with the given ID exists
	 */
	@Override
	public ResponseEntity<RestProcessor> getProcessorById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorById({})", id);
		
		try {
			return new ResponseEntity<>(processorManager.getProcessorById(id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Update a processor by ID
	 * 
	 * @param id the ID of the processor to update
	 * @param processor a Json object containing the modified (and unmodified) attributes
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the processor after modification
	 *             (with ID and version for all contained objects) or 
	 * 		   HTTP status "NOT_FOUND" and an error message, if no processor with the given ID exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "CONFLICT"and an error message, if the processor has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestProcessor> modifyProcessor(Long id, @Valid RestProcessor processor) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProcessor({}, {})", id, (null == processor ? "MISSING" : processor.getProcessorName()));
		
		try {
			RestProcessor changedProcessor = processorManager.modifyProcessor(id, processor); 
			HttpStatus httpStatus = (processor.getVersion() == changedProcessor.getVersion() ? HttpStatus.NOT_MODIFIED : HttpStatus.OK);
			return new ResponseEntity<>(changedProcessor, httpStatus);
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
	 * Delete a processor by ID
	 * 
	 * @param id the ID of the processor to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the processor did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "BAD_REQUEST", if the processor class ID was not given, or if dependent objects exist
	 */
	@Override
	public ResponseEntity<?> deleteProcessorById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProcessorById({})", id);
		
		try {
			processorManager.deleteProcessorById(id);
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
	 * Count the processors matching the specified mission, processor name, and
	 * processor version.
	 * 
	 * @param missionCode      the mission code
	 * @param processorName    the processor name
	 * @param processorVersion the processor version
	 * @return the number of matching processors as a String (may be zero) or HTTP
	 *         status "FORBIDDEN" and an error message, if a cross-mission data
	 *         access was attempted
	 */
	@Override
	public ResponseEntity<String> countProcessors(String missionCode, String processorName, String processorVersion) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countProcessors({}, {}, {})", missionCode, processorName, processorVersion);

		try {
			return new ResponseEntity<>(processorManager.countProcessors(missionCode, processorName, processorVersion),
					HttpStatus.OK);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}
