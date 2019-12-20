/**
 * ConfigurationControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ConcurrentModificationException;
import java.util.List;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import de.dlr.proseo.procmgr.rest.model.RestConfiguration;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage configuration versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ConfigurationControllerImpl implements ConfigurationController {
	
	/* Message ID constants */
	// private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-processor-mgr ";
	
	/** The configuration manager */
	@Autowired
	private ConfigurationManager configurationManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ConfigurationControllerImpl.class);

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
	 * Get configurations by mission, processor name and configuration version
	 * 
	 * @param mission the mission code
	 * @param processorName the processor name
	 * @param configurationVersion the configuration version
	 * @return HTTP status "OK" and a list of Json objects representing configurations satisfying the search criteria or
	 *         HTTP status "NOT_FOUND" and an error message, if no configurations matching the search criteria were found
	 */
	@Override
	public ResponseEntity<List<RestConfiguration>> getConfigurations(String mission, String processorName,
			String configurationVersion) {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfigurations({}, {}, {})", mission, processorName, configurationVersion);
		
		try {
			return new ResponseEntity<>(configurationManager.getConfigurations(mission, processorName, configurationVersion), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
     * Create a new configuration
     * 
     * @param configuration a Json representation of the new configuration
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the configuration after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestConfiguration> createConfiguration(@Valid RestConfiguration configuration) {
		if (logger.isTraceEnabled()) logger.trace(">>> createConfiguration({})", (null == configuration ? "MISSING" : configuration.getProcessorName()));

		try {
			return new ResponseEntity<>(configurationManager.createConfiguration(configuration), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Get a configuration by ID
	 * 
	 * @param id the configuration ID
	 * @return HTTP status "OK" and a Json object corresponding to the configuration found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no configuration ID was given, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no configuration with the given ID exists
	 */
	@Override
	public ResponseEntity<RestConfiguration> getConfigurationById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfigurationById({})", id);
		
		try {
			return new ResponseEntity<>(configurationManager.getConfigurationById(id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
		
	}

	/**
	 * Update a configuration by ID
	 * 
	 * @param id the ID of the configuration to update
	 * @param processorClass a Json object containing the modified (and unmodified) attributes
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the configuration after modification
	 *             (with ID and version for all contained objects) or 
	 * 		   HTTP status "NOT_FOUND" and an error message, if no configuration with the given ID exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "CONFLICT"and an error message, if the configuration has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestConfiguration> modifyConfiguration(Long id, @Valid RestConfiguration configuration) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyConfiguration({}, {})", id, (null == configuration ? "MISSING" : configuration.getProcessorName() + " " + configuration.getConfigurationVersion()));
		
		try {
			return new ResponseEntity<>(configurationManager.modifyConfiguration(id, configuration), HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		}
	}

	/**
	 * Delete a configuration by ID
	 * 
	 * @param the ID of the configuration to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the configuration did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteConfigurationById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteConfigurationById({})", id);
		
		try {
			configurationManager.deleteConfigurationById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

}
