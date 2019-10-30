/**
 * ProcessorControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.NoResultException;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import de.dlr.proseo.procmgr.rest.model.RestProcessor;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage processor versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProcessorControllerImpl implements ProcessorController {
	
	/* Message ID constants */
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-processor-mgr ";

	/** The processor manager */
	@Autowired
	private ProcessorManager processorManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorControllerImpl.class);

	/**
	 * Create and log a formatted error message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted error message
	 */
	private String logError(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.error(message);
		
		return message;
	}
	
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
	 * Create a new processor (version)
	 * 
	 * @param processor a Json representation of the new processor
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the processor after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestProcessor> createProcessor(RestProcessor processor) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProcessor({})", (null == processor ? "MISSING" : processor.getProcessorName()));

		try {
			return new ResponseEntity<>(processorManager.createProcessor(processor), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Get processors by mission, name and version (user-defined version, not database version)
	 * 
	 * @param mission the mission code
	 * @param processorName the name of the processor (class)
	 * @param processorVersion the processor version
	 * @return HTTP status "OK" and a list of Json objects representing processors satisfying the search criteria or
	 *         HTTP status "NOT_FOUND" and an error message, if no products matching the search criteria were found
	 */
	@Override
	public ResponseEntity<List<RestProcessor>> getProcessors(String mission, String processorName, String processorVersion) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessors({}, {}, {})", mission, processorName, processorVersion);
		
		try {
			return new ResponseEntity<>(processorManager.getProcessors(mission, processorName, processorVersion), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Get a processor by ID
	 * 
	 * @param id the processor ID
	 * @return HTTP status "OK" and a Json object corresponding to the processor found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no processor ID was given, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no processor with the given ID exists
	 */
	@Override
	public ResponseEntity<RestProcessor> getProcessorById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorById({})", id);
		
		try {
			return new ResponseEntity<>(processorManager.getProcessorById(id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Update a processor by ID
	 * 
	 * @param id the ID of the processor to update
	 * @param processorClass a Json object containing the modified (and unmodified) attributes
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the processor after modification
	 *             (with ID and version for all contained objects) or 
	 * 		   HTTP status "NOT_FOUND" and an error message, if no processor with the given ID exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "CONFLICT"and an error message, if the processor has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestProcessor> modifyProcessor(Long id, @Valid RestProcessor processor) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders(logError("PATCH for processor not implemented", MSG_ID_NOT_IMPLEMENTED, id)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

	/**
	 * Delete a processor by ID
	 * 
	 * @param the ID of the processor to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the processor did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteProcessorById(Long id) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders(logError("DELETE for processor not implemented", MSG_ID_NOT_IMPLEMENTED, id)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

}
