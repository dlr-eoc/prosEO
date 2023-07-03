/**
 * ConfiguredProcessorControllerImpl.java
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
import de.dlr.proseo.procmgr.rest.model.RestConfiguredProcessor;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage configured processor versions.
 *
 * @author Dr. Thomas Bassler
 */
@Component
public class ConfiguredProcessorControllerImpl implements ConfiguredprocessorController {

	/** The configured processor manager */
	@Autowired
	private ConfiguredProcessorManager configuredProcessorManager;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ConfiguredProcessorControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PROCESSOR_MGR);

	/**
	 * Get configured processors, filtered by mission, identifier, processor name, processor version and/or configuration version
	 *
	 * @param mission              the mission code
	 * @param identifier           the identifier for the configured processor
	 * @param processorName        the processor name
	 * @param processorVersion     the processor version
	 * @param configurationVersion the configuration version
	 * @param uuid                 the UUID of the configured processor
	 * @param recordFrom           first record of filtered and ordered result to return
	 * @param recordTo             last record of filtered and ordered result to return
	 * @return HTTP status "OK" and a list of JSON objects representing configured processors satisfying the search criteria or HTTP
	 *         status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or HTTP status "NOT_FOUND" and
	 *         an error message, if no configurations matching the search criteria were found, or HTTP status "TOO MANY REQUESTS" if
	 *         the result list exceeds a configured maximum
	 */
	@Override
	public ResponseEntity<List<RestConfiguredProcessor>> getConfiguredProcessors(String mission, String identifier,
			String processorName, String processorVersion, String configurationVersion, String uuid, Integer recordFrom,
			Integer recordTo) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getConfiguredProcessors({}, {}, {}, {}, {}, {})", mission, identifier, processorName,
					processorVersion, configurationVersion, uuid);

		try {
			return new ResponseEntity<>(configuredProcessorManager.getConfiguredProcessors(mission, identifier, processorName,
					processorVersion, configurationVersion, uuid, recordFrom, recordTo), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (HttpClientErrorException.TooManyRequests e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.TOO_MANY_REQUESTS);
		}
	}

	/**
	 * Create a new configured processor
	 *
	 * @param configuredProcessor a Json representation of the new configured processor
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the configured processor after
	 *         persistence (with ID and version for all contained objects) or HTTP status "FORBIDDEN" and an error message, if a
	 *         cross-mission data access was attempted, or HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestConfiguredProcessor> createConfiguredProcessor(@Valid RestConfiguredProcessor configuredProcessor) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createConfiguredProcessor({})",
					(null == configuredProcessor ? "MISSING" : configuredProcessor.getProcessorName()));

		try {
			return new ResponseEntity<>(configuredProcessorManager.createConfiguredProcessor(configuredProcessor),
					HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Get a configured processor by ID
	 *
	 * @param id the configured processor ID
	 * @return HTTP status "OK" and a Json object corresponding to the configured processor found or HTTP status "BAD_REQUEST" and
	 *         an error message, if no configured processor ID was given, or HTTP status "FORBIDDEN" and an error message, if a
	 *         cross-mission data access was attempted, or HTTP status "NOT_FOUND" and an error message, if no configured processor
	 *         with the given ID exists
	 */
	@Override
	public ResponseEntity<RestConfiguredProcessor> getConfiguredProcessorById(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getConfiguredProcessorById({})", id);

		try {
			return new ResponseEntity<>(configuredProcessorManager.getConfiguredProcessorById(id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Update a configured processor by ID
	 *
	 * @param id                  the ID of the configured processor to update
	 * @param configuredProcessor a Json object containing the modified (and unmodified) attributes
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the configured processor after modification
	 *         (with ID and version for all contained objects) or HTTP status "NOT_MODIFIED" and the unchanged configured processor,
	 *         if no attributes were actually changed, or HTTP status "NOT_FOUND" and an error message, if no configured processor
	 *         with the given ID exists, or HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or HTTP status
	 *         "CONFLICT"and an error message, if the configured processor has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestConfiguredProcessor> modifyConfiguredProcessor(Long id,
			@Valid RestConfiguredProcessor configuredProcessor) {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyConfiguredProcessor({}, {})", id,
					(null == configuredProcessor ? "MISSING" : configuredProcessor.getIdentifier()));

		try {
			RestConfiguredProcessor changedConfiguredProcessor = configuredProcessorManager.modifyConfiguredProcessor(id,
					configuredProcessor);
			HttpStatus httpStatus = (configuredProcessor.getVersion() == changedConfiguredProcessor.getVersion()
					? HttpStatus.NOT_MODIFIED
					: HttpStatus.OK);
			return new ResponseEntity<>(changedConfiguredProcessor, httpStatus);
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
	 * Delete a configured processor by ID
	 *
	 * @param id the ID of the configured processor to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or HTTP status "NOT_FOUND", if the
	 *         configured processor did not exist, or HTTP status "FORBIDDEN" and an error message, if a cross-mission data access
	 *         was attempted, or HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteConfiguredProcessorById(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteConfiguredProcessorById({})", id);

		try {
			configuredProcessorManager.deleteConfiguredProcessorById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * Count the configuredProcessors matching the specified mission, processor name, and configuredProcessor version.
	 *
	 * @param missionCode          the mission code
	 * @param processorName        the processor name
	 * @param processorVersion     the processor version
	 * @param configurationVersion the configuration version
	 * @return the number of matching configuredProcessors as a String (may be zero) or HTTP status "FORBIDDEN" and an error
	 *         message, if a cross-mission data access was attempted
	 */
	@Override
	public ResponseEntity<String> countConfiguredProcessors(String missionCode, String processorName, String processorVersion,
			String configurationVersion) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countConfiguredProcessors({}, {}, {}, {})", missionCode, processorName, processorVersion,
					configurationVersion);

		try {
			return new ResponseEntity<>(configuredProcessorManager.countConfiguredProcessors(missionCode, processorName,
					processorVersion, configurationVersion), HttpStatus.OK);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}
}