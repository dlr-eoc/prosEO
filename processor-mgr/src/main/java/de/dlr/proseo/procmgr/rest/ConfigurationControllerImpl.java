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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.procmgr.rest.model.RestConfiguration;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage configuration versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ConfigurationControllerImpl implements ConfigurationController {
	
	/** The configuration manager */
	@Autowired
	private ConfigurationManager configurationManager;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ConfigurationControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PROCESSOR_MGR);
	
	/**
	 * Get configurations by mission, processor name and configuration version
	 * 
	 * @param mission              the mission code
	 * @param processorName        the processor name
	 * @param configurationVersion the configuration version
	 * @param recordFrom           first record of filtered and ordered result to return
	 * @param recordTo             last record of filtered and ordered result to return
	 * @return HTTP status "OK" and a list of JSON objects representing configurations satisfying the search criteria or 
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or 
	 *         HTTP status "NOT_FOUND" and an error message, if no configurations matching the search criteria were found, or 
	 *         HTTP status "TOO MANY REQUESTS" if the result list exceeds a configured maximum
	 */
	@Override
	public ResponseEntity<List<RestConfiguration>> getConfigurations(String mission, String processorName,
			String configurationVersion, Integer recordFrom, Integer recordTo) {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfigurations({}, {}, {})", mission, processorName, configurationVersion);
		
		try {
			return new ResponseEntity<>(configurationManager.getConfigurations(mission, processorName, configurationVersion, recordFrom, recordTo), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (HttpClientErrorException.TooManyRequests e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.TOO_MANY_REQUESTS);
		}
	}

	/**
     * Create a new configuration
     * 
     * @param configuration a Json representation of the new configuration
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the configuration after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestConfiguration> createConfiguration(@Valid RestConfiguration configuration) {
		if (logger.isTraceEnabled()) logger.trace(">>> createConfiguration({})", (null == configuration ? "MISSING" : configuration.getProcessorName()));

		try {
			return new ResponseEntity<>(configurationManager.createConfiguration(configuration), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Get a configuration by ID
	 * 
	 * @param id the configuration ID
	 * @return HTTP status "OK" and a Json object corresponding to the configuration found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no configuration ID was given, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no configuration with the given ID exists, or 
	 * 		   HTTP status "TOO MANY REQUESTS" if the result list exceeds a configured maximum
	 */
	@Override
	public ResponseEntity<RestConfiguration> getConfigurationById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfigurationById({})", id);
		
		try {
			return new ResponseEntity<>(configurationManager.getConfigurationById(id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (HttpClientErrorException.TooManyRequests e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.TOO_MANY_REQUESTS);
		}
	}

	/**
	 * Update a configuration by ID
	 * 
	 * @param id the ID of the configuration to update
	 * @param configuration a Json object containing the modified (and unmodified) attributes
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the configuration after modification
	 *             (with ID and version for all contained objects) or 
	 *         HTTP status "NOT_MODIFIED" and the unchanged configuration, if no attributes were actually changed, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no configuration with the given ID exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "CONFLICT" and an error message, if the configuration has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestConfiguration> modifyConfiguration(Long id, @Valid RestConfiguration configuration) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyConfiguration({}, {})", id, (null == configuration ? "MISSING" : configuration.getProcessorName() + " " + configuration.getConfigurationVersion()));
		
		try {
			RestConfiguration changedConfiguration = configurationManager.modifyConfiguration(id, configuration); 
			HttpStatus httpStatus = (configuration.getVersion() == changedConfiguration.getVersion() ? HttpStatus.NOT_MODIFIED : HttpStatus.OK);
			return new ResponseEntity<>(changedConfiguration, httpStatus);
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
	 * Delete a configuration by ID
	 * 
	 * @param id the ID of the configuration to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the configuration did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "BAD_REQUEST", if the processor class ID was not given, or if dependent objects exist
	 */
	@Override
	public ResponseEntity<?> deleteConfigurationById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteConfigurationById({})", id);
		
		try {
			configurationManager.deleteConfigurationById(id);
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
	 * Count the configurations matching the specified mission, processor name, and
	 * configuration version.
	 * 
	 * @param missionCode          the mission code
	 * @param processorName        the processor name
	 * @param configurationVersion the configuration version
	 * @return the number of matching configurations as a String (may be zero) or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data
	 *         access was attempted
	 */
	@Override
	public ResponseEntity<String> countConfigurations(String missionCode, String processorName,
			String configurationVersion) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countConfigurations({}, {}, {})", missionCode, processorName, configurationVersion);

		try {
			return new ResponseEntity<>(configurationManager.countConfigurations(missionCode, processorName, configurationVersion), HttpStatus.OK);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}
