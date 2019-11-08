/**
 * ConfiguredProcessorControllerImpl.java
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
import de.dlr.proseo.procmgr.rest.model.RestConfiguredProcessor;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage configured processor versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ConfiguredProcessorControllerImpl implements ConfiguredprocessorController {
	
	/* Message ID constants */
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-processor-mgr ";

	/** The configured processor manager */
	@Autowired
	private ConfiguredProcessorManager configuredProcessorManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ConfiguredProcessorControllerImpl.class);

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
	 * Get configured processors by mission, processor name, processor version and configuration version
	 * 
	 * @param mission the mission code
	 * @param processorName the processor name
	 * @param processorVersion the processor version
	 * @param configurationVersion the configuration version
	 * @return HTTP status "OK" and a list of Json objects representing configured processors satisfying the search criteria or
	 *         HTTP status "NOT_FOUND" and an error message, if no configured processors matching the search criteria were found
	 */
	@Override
	public ResponseEntity<List<RestConfiguredProcessor>> getConfiguredProcessors(String mission, String processorName,
			String processorVersion, String configurationVersion) {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfiguredProcessors({}, {}, {}, {})", 
				mission, processorName, processorVersion, configurationVersion);
		
		try {
			return new ResponseEntity<>(
					configuredProcessorManager.getConfiguredProcessors(mission, processorName, processorVersion, configurationVersion),
					HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
     * Create a new configured processor
     * 
     * @param configuredProcessor a Json representation of the new configured processor
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the configured processor after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestConfiguredProcessor> createConfiguredProcessor(@Valid RestConfiguredProcessor configuredProcessor) {
		if (logger.isTraceEnabled()) logger.trace(">>> createConfiguredProcessor({})", (null == configuredProcessor ? "MISSING" : configuredProcessor.getProcessorName()));

		try {
			return new ResponseEntity<>(configuredProcessorManager.createConfiguredProcessor(configuredProcessor), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Get a configured processor by ID
	 * 
	 * @param id the configured processor ID
	 * @return HTTP status "OK" and a Json object corresponding to the configured processor found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no configured processor ID was given, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no configured processor with the given ID exists
	 */
	@Override
	public ResponseEntity<RestConfiguredProcessor> getConfiguredProcessorById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfiguredProcessorById({})", id);
		
		try {
			return new ResponseEntity<>(configuredProcessorManager.getConfiguredProcessorById(id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Update a configured processor by ID
	 * 
	 * @param id the ID of the configured processor to update
	 * @param processorClass a Json object containing the modified (and unmodified) attributes
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the configured processor after modification
	 *             (with ID and version for all contained objects) or 
	 * 		   HTTP status "NOT_FOUND" and an error message, if no configured processor with the given ID exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "CONFLICT"and an error message, if the configured processor has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestConfiguredProcessor> modifyConfiguredProcessor(Long id, @Valid RestConfiguredProcessor configuredProcessor) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders(logError("PATCH for configured processor not implemented", MSG_ID_NOT_IMPLEMENTED, id)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

	/**
	 * Delete a configured processor by ID
	 * 
	 * @param the ID of the configured processor to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the configured processor did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteConfiguredProcessorById(Long id) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders(logError("DELETE for configured processor not implemented", MSG_ID_NOT_IMPLEMENTED, id)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

}
