/**
 * ConfiguredProcessorControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.rest.model.Configuration;
import de.dlr.proseo.procmgr.rest.model.ConfigurationUtil;
import de.dlr.proseo.procmgr.rest.model.ConfiguredProcessor;
import de.dlr.proseo.procmgr.rest.model.ConfiguredProcessorUtil;
import de.dlr.proseo.procmgr.rest.model.Processor;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage configured processor versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ConfiguredProcessorControllerImpl implements ConfiguredprocessorController {
	
	/* Message ID constants */
	private static final int MSG_ID_CONFIGURED_PROCESSOR_NOT_FOUND = 2350;
	private static final int MSG_ID_CONFIGURED_PROCESSOR_LIST_RETRIEVED = 2351;
	private static final int MSG_ID_CONFIGURED_PROCESSOR_RETRIEVED = 2352;
	private static final int MSG_ID_CONFIGURED_PROCESSOR_MISSING = 2353;
	private static final int MSG_ID_PROCESSOR_INVALID = 2354;
	private static final int MSG_ID_CONFIGURED_PROCESSOR_CREATED = 2355;
	private static final int MSG_ID_CONFIGURED_PROCESSOR_ID_MISSING = 2356;
	private static final int MSG_ID_CONFIGURED_PROCESSOR_ID_NOT_FOUND = 2357;
	private static final int MSG_ID_CONFIGURATION_INVALID = 2358;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_CONFIGURED_PROCESSOR_NOT_FOUND = "(E%d) No configured processors found for mission %s, processor name %s, processor version %s and configuration version %s";
	private static final String MSG_CONFIGURED_PROCESSOR_LIST_RETRIEVED = "(I%d) Configuration(s) for mission %s, processor name %s, processor version %s and configuration version %s retrieved";
	private static final String MSG_CONFIGURED_PROCESSOR_RETRIEVED = "(I%d) Configuration with ID %d retrieved";
	private static final String MSG_CONFIGURED_PROCESSOR_MISSING = "(E%d) Configuration not set";
	private static final String MSG_CONFIGURED_PROCESSOR_ID_MISSING = "(E%d) Configuration ID not set";
	private static final String MSG_CONFIGURED_PROCESSOR_ID_NOT_FOUND = "(E%d) No Configuration found with ID %d";
	private static final String MSG_PROCESSOR_INVALID = "(E%d) Processor %s with version %s invalid for mission %s";
	private static final String MSG_CONFIGURATION_INVALID = "(E%d) Configuration %s with version %s invalid for mission %s";
	private static final String MSG_CONFIGURED_PROCESSOR_CREATED = "(I%d) Configuration for processor %s with version %s created for mission %s";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-processor-mgr ";

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ConfiguredProcessorControllerImpl.class);

	/**
	 * Log an informational message with the prosEO message prefix
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 */
	private void logInfo(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		logger.info(String.format(messageFormat, messageParamList.toArray()));
	}
	
	/**
	 * Log an error and return the corresponding HTTP message header
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return an HttpHeaders object with a formatted error message
	 */
	private HttpHeaders errorHeaders(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.error(message);
		
		// Create an HTTP "Warning" header
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
	 * @return a list of Json objects representing configured processors satisfying the search criteria
	 */
	@Override
	public ResponseEntity<List<ConfiguredProcessor>> getConfiguredProcessors(String mission, String processorName,
			String processorVersion, String configurationVersion) {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfiguredProcessors({}, {}, {}, {})", 
				mission, processorName, processorVersion, configurationVersion);
		
		List<ConfiguredProcessor> result = new ArrayList<>();
		
		String jpqlQuery = "select c from ConfiguredProcessor where 1 = 1";
		if (null != mission) {
			jpqlQuery += " and processor.productClass.mission.code = :missionCode";
		}
		if (null != processorName) {
			jpqlQuery += " and processor.productClass.processorName = :processorName";
		}
		if (null != processorVersion) {
			jpqlQuery += " and processor.processorVersion = :processorVersion";
		}
		if (null != configurationVersion) {
			jpqlQuery += " and configuration.configurationVersion = :configurationVersion";
		}
		Query query = em.createQuery(jpqlQuery);
		if (null != mission) {
			query.setParameter("missionCode", mission);
		}
		if (null != processorName) {
			query.setParameter("processorName", processorName);
		}
		if (null != processorVersion) {
			query.setParameter("processorVersion", processorVersion);
		}
		if (null != configurationVersion) {
			query.setParameter("configurationVersion", configurationVersion);
		}
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof de.dlr.proseo.model.ConfiguredProcessor) {
				result.add(ConfiguredProcessorUtil.toRestConfiguredProcessor((de.dlr.proseo.model.ConfiguredProcessor) resultObject));
			}
		}
		if (result.isEmpty()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_CONFIGURED_PROCESSOR_NOT_FOUND, MSG_ID_CONFIGURED_PROCESSOR_NOT_FOUND, mission, processorName, processorVersion, configurationVersion),
					HttpStatus.NOT_FOUND);
		}

		logInfo(MSG_CONFIGURED_PROCESSOR_LIST_RETRIEVED, MSG_ID_CONFIGURED_PROCESSOR_LIST_RETRIEVED, mission, processorName, processorVersion, configurationVersion);
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	/**
     * Create a new configured processor
     * 
     * @param configuredProcessor a Json representation of the new configured processor
	 * @return a Json representation of the configured processor after creation (with ID and version number)
	 */
	@Override
	public ResponseEntity<ConfiguredProcessor> createConfiguredProcessor(@Valid ConfiguredProcessor configuredProcessor) {
		if (logger.isTraceEnabled()) logger.trace(">>> createConfiguredProcessor({})", (null == configuredProcessor ? "MISSING" : configuredProcessor.getProcessorName()));

		if (null == configuredProcessor) {
			return new ResponseEntity<>(
					errorHeaders(MSG_CONFIGURED_PROCESSOR_MISSING, MSG_ID_CONFIGURED_PROCESSOR_MISSING),
					HttpStatus.BAD_REQUEST);
		}
	
		de.dlr.proseo.model.ConfiguredProcessor modelConfiguredProcessor = ConfiguredProcessorUtil.toModelConfiguredProcessor(configuredProcessor);
		
		modelConfiguredProcessor.setProcessor(RepositoryService.getProcessorRepository()
				.findByMissionCodeAndProcessorNameAndProcessorVersion(configuredProcessor.getMissionCode(), configuredProcessor.getProcessorName(), configuredProcessor.getProcessorVersion()));
		if (null == modelConfiguredProcessor.getProcessor()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PROCESSOR_INVALID, MSG_ID_PROCESSOR_INVALID,
							configuredProcessor.getProcessorName(), configuredProcessor.getProcessorVersion(), configuredProcessor.getMissionCode()),
					HttpStatus.BAD_REQUEST);
		}
		
		modelConfiguredProcessor.setConfiguration(RepositoryService.getConfigurationRepository()
				.findByMissionCodeAndProcessorNameAndConfigurationVersion(configuredProcessor.getMissionCode(), configuredProcessor.getProcessorName(), configuredProcessor.getConfigurationVersion()));
		if (null == modelConfiguredProcessor.getConfiguration()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_CONFIGURATION_INVALID, MSG_ID_CONFIGURATION_INVALID,
							configuredProcessor.getProcessorName(), configuredProcessor.getProcessorVersion(), configuredProcessor.getMissionCode()),
					HttpStatus.BAD_REQUEST);
		}
		
		modelConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository().save(modelConfiguredProcessor);
		
		logInfo(MSG_CONFIGURED_PROCESSOR_CREATED, MSG_ID_CONFIGURED_PROCESSOR_CREATED, 
				modelConfiguredProcessor.getProcessor().getProcessorClass().getProcessorName(),
				modelConfiguredProcessor.getProcessor().getProcessorVersion(),
				modelConfiguredProcessor.getConfiguration().getConfigurationVersion(), 
				modelConfiguredProcessor.getProcessor().getProcessorClass().getMission().getCode());
		
		return new ResponseEntity<>(ConfiguredProcessorUtil.toRestConfiguredProcessor(modelConfiguredProcessor), HttpStatus.CREATED);
	}

	/**
	 * Get a configured processor by ID
	 * 
	 * @param id the configured processor ID
	 * @return a Json object corresponding to the configured processor found and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no configured processor with the given ID exists
	 */
	@Override
	public ResponseEntity<ConfiguredProcessor> getConfiguredProcessorById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfiguredProcessorById({})", id);
		
		if (null == id) {
			return new ResponseEntity<>(
					errorHeaders(MSG_CONFIGURED_PROCESSOR_ID_MISSING, MSG_ID_CONFIGURED_PROCESSOR_ID_MISSING, id), 
					HttpStatus.BAD_REQUEST);
		}
		
		Optional<de.dlr.proseo.model.ConfiguredProcessor> modelConfiguration = RepositoryService.getConfiguredProcessorRepository().findById(id);
		
		if (modelConfiguration.isEmpty()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_CONFIGURED_PROCESSOR_ID_NOT_FOUND, MSG_ID_CONFIGURED_PROCESSOR_ID_NOT_FOUND, id), 
					HttpStatus.NOT_FOUND);
		}

		logInfo(MSG_CONFIGURED_PROCESSOR_RETRIEVED, MSG_ID_CONFIGURED_PROCESSOR_RETRIEVED, id);
		
		return new ResponseEntity<>(ConfiguredProcessorUtil.toRestConfiguredProcessor(modelConfiguration.get()), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ConfiguredProcessor> updateConfiguredProcessor(Long id, @Valid ConfiguredProcessor configuredProcessor) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders("PATCH for configured processor not implemented", MSG_ID_NOT_IMPLEMENTED, id), 
				HttpStatus.NOT_IMPLEMENTED);
	}

	@Override
	public ResponseEntity<?> deleteConfiguredprocessorById(Long id) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders("DELETE for configured processor not implemented", MSG_ID_NOT_IMPLEMENTED, id), 
				HttpStatus.NOT_IMPLEMENTED);
	}

}
