/**
 * ConfigurationManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import de.dlr.proseo.model.Configuration;
import de.dlr.proseo.model.ConfigurationInputFile;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.rest.model.RestConfiguration;
import de.dlr.proseo.procmgr.rest.model.RestConfigurationInputFile;
import de.dlr.proseo.procmgr.rest.model.ConfigurationUtil;

/**
 * Service methods required to manage configuration versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
@Transactional
public class ConfigurationManager {
	
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
	
	/** Allowed filename types for static input files (in lower case for easier comparation) */
	private static final List<String> ALLOWED_FILENAME_TYPES = Arrays.asList("physical", "logical", "stem", "regexp", "directory");

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

	/**
	 * Create and log a formatted informational message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private String logInfo(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.info(message);
		
		return message;
	}
	
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
	 * Get configurations by mission, processor name and configuration version
	 * 
	 * @param mission the mission code
	 * @param processorName the processor name
	 * @param configurationVersion the configuration version
	 * @return a list of Json objects representing configurations satisfying the search criteria
	 * @throws NoResultException if no processor classes matching the given search criteria could be found
	 */
	public List<RestConfiguration> getConfigurations(String mission, String processorName,
			String configurationVersion) throws NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfigurations({}, {}, {})", mission, processorName, configurationVersion);
		
		List<RestConfiguration> result = new ArrayList<>();
		
		if (null != mission && null != processorName && null != configurationVersion) {
			de.dlr.proseo.model.Configuration processor = RepositoryService.getConfigurationRepository()
					.findByMissionCodeAndProcessorNameAndConfigurationVersion(mission, processorName, configurationVersion);
			if (null == processor) {
				throw new NoResultException(logError(MSG_CONFIGURATION_NOT_FOUND, MSG_ID_CONFIGURATION_NOT_FOUND,
						mission, processorName, configurationVersion));
			}
			result.add(ConfigurationUtil.toRestConfiguration(processor));
		} else {
			String jpqlQuery = "select c from Configuration c where 1 = 1";
			if (null != mission) {
				jpqlQuery += " and processorClass.mission.code = :missionCode";
			}
			if (null != processorName) {
				jpqlQuery += " and processorClass.processorName = :processorName";
			}
			if (null != configurationVersion) {
				jpqlQuery += " and configurationVersion = :configurationVersion";
			}
			Query query = em.createQuery(jpqlQuery);
			if (null != mission) {
				query.setParameter("missionCode", mission);
			}
			if (null != processorName) {
				query.setParameter("processorName", processorName);
			}
			if (null != configurationVersion) {
				query.setParameter("configurationVersion", configurationVersion);
			}
			for (Object resultObject: query.getResultList()) {
				if (resultObject instanceof de.dlr.proseo.model.Configuration) {
					result.add(ConfigurationUtil.toRestConfiguration((de.dlr.proseo.model.Configuration) resultObject));
				}
			}
			if (result.isEmpty()) {
				throw new NoResultException(logError(MSG_CONFIGURATION_NOT_FOUND, MSG_ID_CONFIGURATION_NOT_FOUND,
						mission, processorName, configurationVersion));
			}
		}
		logInfo(MSG_CONFIGURATION_LIST_RETRIEVED, MSG_ID_CONFIGURATION_LIST_RETRIEVED, mission, processorName, configurationVersion);
		
		return result;
	}

	/**
     * Create a new configuration
     * 
     * @param configuration a Json representation of the new configuration
	 * @return a Json representation of the configuration after creation (with ID and version number)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 */
	public RestConfiguration createConfiguration(@Valid RestConfiguration configuration) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> createConfiguration({})", (null == configuration ? "MISSING" : configuration.getProcessorName()));

		if (null == configuration) {
			throw new IllegalArgumentException(logError(MSG_CONFIGURATION_MISSING, MSG_ID_CONFIGURATION_MISSING));
		}
		
		Configuration modelConfiguration = ConfigurationUtil.toModelConfiguration(configuration);
		
		modelConfiguration.setProcessorClass(RepositoryService.getProcessorClassRepository()
				.findByMissionCodeAndProcessorName(configuration.getMissionCode(), configuration.getProcessorName()));
		if (null == modelConfiguration.getProcessorClass()) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_CLASS_INVALID, MSG_ID_PROCESSOR_CLASS_INVALID,
							configuration.getProcessorName(), configuration.getMissionCode()));
		}
		
		for (RestConfigurationInputFile staticInputFile: configuration.getStaticInputFiles()) {
			ConfigurationInputFile modelInputFile = new ConfigurationInputFile();
			if (!ALLOWED_FILENAME_TYPES.contains(staticInputFile.getFileNameType())) {
				throw new IllegalArgumentException(logError(MSG_FILENAME_TYPE_INVALID, MSG_ID_FILENAME_TYPE_INVALID,
						staticInputFile.getFileNameType()));
			}
			modelInputFile.setFileType(staticInputFile.getFileType());
			modelInputFile.setFileNameType(staticInputFile.getFileNameType());
			modelInputFile.getFileNames().addAll(staticInputFile.getFileNames());
			modelConfiguration.getStaticInputFiles().add(modelInputFile);
		}
		
		modelConfiguration = RepositoryService.getConfigurationRepository().save(modelConfiguration);
		
		logInfo(MSG_CONFIGURATION_CREATED, MSG_ID_CONFIGURATION_CREATED, 
				modelConfiguration.getProcessorClass().getProcessorName(),
				modelConfiguration.getConfigurationVersion(), 
				modelConfiguration.getProcessorClass().getMission().getCode());
		
		return ConfigurationUtil.toRestConfiguration(modelConfiguration);
	}

	/**
	 * Get a configuration by ID
	 * 
	 * @param id the configuration ID
	 * @return a Json object corresponding to the configuration found
	 * @throws IllegalArgumentException if no configuration ID was given
	 * @throws NoResultException if no configuration with the given ID exists
	 */
	public RestConfiguration getConfigurationById(Long id) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfigurationById({})", id);
		
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_CONFIGURATION_ID_MISSING, MSG_ID_CONFIGURATION_ID_MISSING, id));
		}
		
		Optional<de.dlr.proseo.model.Configuration> modelConfiguration = RepositoryService.getConfigurationRepository().findById(id);
		
		if (modelConfiguration.isEmpty()) {
			throw new NoResultException(logError(MSG_CONFIGURATION_ID_NOT_FOUND, MSG_ID_CONFIGURATION_ID_NOT_FOUND, id));
		}

		logInfo(MSG_CONFIGURATION_RETRIEVED, MSG_ID_CONFIGURATION_RETRIEVED, id);
		
		return ConfigurationUtil.toRestConfiguration(modelConfiguration.get());
	}

	/**
	 * Update a configuration by ID
	 * 
	 * @param id the ID of the configuration to update
	 * @param configuration a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the configuration after modification (with ID and version for all 
	 * 		   contained objects)
	 * @throws EntityNotFoundException if no configuration with the given ID exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws ConcurrentModificationException if the configuration has been modified since retrieval by the client
	 */
	public RestConfiguration modifyConfiguration(Long id, @Valid RestConfiguration configuration) throws
			EntityNotFoundException, IllegalArgumentException, ConcurrentModificationException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(logError("PATCH for configuration not implemented", MSG_ID_NOT_IMPLEMENTED, id));
	}

	/**
	 * Delete a configuration by ID
	 * 
	 * @param the ID of the configuration to delete
	 * @throws EntityNotFoundException if the configuration to delete does not exist in the database
	 * @throws RuntimeException if the deletion was not performed as expected
	 */
	public void deleteConfigurationById(Long id) throws EntityNotFoundException, RuntimeException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(logError("DELETE for configuration not implemented", MSG_ID_NOT_IMPLEMENTED, id));
	}

}
