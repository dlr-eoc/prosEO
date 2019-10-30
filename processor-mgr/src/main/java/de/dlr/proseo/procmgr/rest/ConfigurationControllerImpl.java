/**
 * ConfigurationControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import de.dlr.proseo.model.Configuration;
import de.dlr.proseo.model.ConfigurationInputFile;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.rest.model.RestConfiguration;
import de.dlr.proseo.procmgr.rest.model.RestConfigurationInputFile;
import de.dlr.proseo.procmgr.rest.model.ConfigurationUtil;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage configuration versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ConfigurationControllerImpl implements ConfigurationController {
	
	/* Message ID constants */
	private static final int MSG_ID_CONFIGURATION_NOT_FOUND = 2300;
	private static final int MSG_ID_CONFIGURATION_LIST_RETRIEVED = 2301;
	private static final int MSG_ID_CONFIGURATION_RETRIEVED = 2302;
	private static final int MSG_ID_CONFIGURATION_MISSING = 2303;
	private static final int MSG_ID_PROCESSOR_CLASS_INVALID = 2304;
	private static final int MSG_ID_CONFIGURATION_CREATED = 2305;
	private static final int MSG_ID_CONFIGURATION_ID_MISSING = 2306;
	private static final int MSG_ID_CONFIGURATION_ID_NOT_FOUND = 2307;
	private static final int MSG_ID_FILENAME_TYPE_INVALID = 2308;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_CONFIGURATION_NOT_FOUND = "(E%d) No configuration found for mission %s, processor name %s and configuration version %s";
	private static final String MSG_CONFIGURATION_LIST_RETRIEVED = "(I%d) Configuration(s) for mission %s, processor name %s and configuration version %s retrieved";
	private static final String MSG_CONFIGURATION_RETRIEVED = "(I%d) Configuration with ID %d retrieved";
	private static final String MSG_CONFIGURATION_MISSING = "(E%d) Configuration not set";
	private static final String MSG_CONFIGURATION_ID_MISSING = "(E%d) Configuration ID not set";
	private static final String MSG_CONFIGURATION_ID_NOT_FOUND = "(E%d) No Configuration found with ID %d";
	private static final String MSG_PROCESSOR_CLASS_INVALID = "(E%d) Processor class %s invalid for mission %s";
	private static final String MSG_FILENAME_TYPE_INVALID = "(E%d) Input filename type %s invalid";
	private static final String MSG_CONFIGURATION_CREATED = "(I%d) Configuration for processor %s with version %s created for mission %s";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-processor-mgr ";
	
	/** Allowed filename types for static input files (in lower case for easier comparation) */
	private static final List<String> ALLOWED_FILENAME_TYPES = Arrays.asList("physical", "logical", "stem", "regexp", "directory");

	/** The configuration manager */
	@Autowired
	private ConfigurationManager configurationManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ConfigurationControllerImpl.class);

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
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders(logError("PATCH for configuration not implemented", MSG_ID_NOT_IMPLEMENTED, id)), 
				HttpStatus.NOT_IMPLEMENTED);
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
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders(logError("DELETE for configuration not implemented", MSG_ID_NOT_IMPLEMENTED, id)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

}
