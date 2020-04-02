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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.procmgr.rest.model.RestProcessorClass;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage processor classes.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProcessorClassControllerImpl implements ProcessorclassController {
	
	/* Message ID constants */
//	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-processor-mgr ";

	/** The processor class manager */
	@Autowired
	private ProcessorClassManager processorClassManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorClassControllerImpl.class);

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
	 * Get processor classes by mission and name
	 * 
	 * @param mission the mission code (optional)
	 * @param processorName the processor name (optional)
	 * @return HTTP status "OK" and a list of Json objects representing processor classes satisfying the search criteria or
	 *         HTTP status "NOT_FOUND" and an error message, if no processor classes matching the search criteria were found
	 */
	@Override
	public ResponseEntity<List<RestProcessorClass>> getProcessorClasses(String mission, String processorName) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClass({}, {})", mission, processorName);
		
		try {
			return new ResponseEntity<>(processorClassManager.getProcessorClasses(mission, processorName), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Create a new processor class
	 * 
	 * @param processorClass a Json representation of the new processor class
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the processor class after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestProcessorClass> createProcessorClass(@Valid RestProcessorClass processorClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProcessorClass({})", (null == processorClass ? "MISSING" : processorClass.getProcessorName()));
		
		try {
			return new ResponseEntity<>(processorClassManager.createProcessorClass(processorClass), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Get a processor class by ID
	 * 
	 * @param id the processor class ID
	 * @return HTTP status "OK" and a Json object corresponding to the processor class found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no processor class ID was given, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no processor class with the given ID exists
	 */
	@Override
	public ResponseEntity<RestProcessorClass> getProcessorClassById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClassById({})", id);
		
		try {
			return new ResponseEntity<>(processorClassManager.getProcessorClassById(id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Update a processor class by ID
	 * 
	 * @param id the ID of the processor class to update
	 * @param processorClass a Json object containing the modified (and unmodified) attributes
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the processor class after modification
	 *             (with ID and version for all contained objects) or 
	 * 		   HTTP status "NOT_FOUND" and an error message, if no processor class with the given ID exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "CONFLICT"and an error message, if the processor class has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestProcessorClass> modifyProcessorClass(Long id, @Valid RestProcessorClass processorClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProcessorClass({}, {})", id, (null == processorClass ? "MISSING" : processorClass.getProcessorName()));

		try {
			return new ResponseEntity<>(processorClassManager.modifyProcessorClass(id, processorClass), HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		}
	}

	/**
	 * Delete a processor class by ID
	 * 
	 * @param id the ID of the processor class to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the processor class did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful, or
	 *         HTTP status "BAD_REQUEST", if the processor class ID was not given, or if dependent objects exist
	 */
	@Override
	public ResponseEntity<?> deleteProcessorClassById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProcessorClassById({})", id);
		
		try {
			processorClassManager.deleteProcessorClassById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

}
