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
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.rest.model.Configuration;
import de.dlr.proseo.procmgr.rest.model.ConfigurationInputFile;
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

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ConfigurationControllerImpl.class);

	/** Single TransactionTemplate shared amongst all methods in this instance */
	private final TransactionTemplate transactionTemplate;

	/**
	 * Constructor using constructor-injection to supply the PlatformTransactionManager
	 * 
	 * @param transactionManager the platform transaction manager
	 */
	public ConfigurationControllerImpl(PlatformTransactionManager transactionManager) {
		Assert.notNull(transactionManager, "The 'transactionManager' argument must not be null.");
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}
	
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
	 * Get configurations by mission, processor name and configuration version
	 * 
	 * @param mission the mission code
	 * @param processorName the processor name
	 * @param configurationVersion the configuration version
	 * @return a list of Json objects representing configurations satisfying the search criteria
	 */
	@Override
	public ResponseEntity<List<Configuration>> getConfigurations(String mission, String processorName,
			String configurationVersion) {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfigurations({}, {}, {})", mission, processorName, configurationVersion);
		
		List<Configuration> result = new ArrayList<>();
		
		if (null != mission && null != processorName && null != configurationVersion) {
			de.dlr.proseo.model.Configuration processor = RepositoryService.getConfigurationRepository()
					.findByMissionCodeAndProcessorNameAndConfigurationVersion(mission, processorName, configurationVersion);
			if (null == processor) {
				return new ResponseEntity<>(
						errorHeaders(MSG_CONFIGURATION_NOT_FOUND, MSG_ID_CONFIGURATION_NOT_FOUND, mission, processorName, configurationVersion),
						HttpStatus.NOT_FOUND);
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
				return new ResponseEntity<>(
						errorHeaders(MSG_CONFIGURATION_NOT_FOUND, MSG_ID_CONFIGURATION_NOT_FOUND, mission, processorName, configurationVersion),
						HttpStatus.NOT_FOUND);
			}
		}
		logInfo(MSG_CONFIGURATION_LIST_RETRIEVED, MSG_ID_CONFIGURATION_LIST_RETRIEVED, mission, processorName, configurationVersion);
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	/**
     * Create a new configuration
     * 
     * @param configuration a Json representation of the new configuration
	 * @return a Json representation of the configuration after creation (with ID and version number)
	 */
	@Override
	public ResponseEntity<Configuration> createConfiguration(@Valid Configuration configuration) {
		if (logger.isTraceEnabled()) logger.trace(">>> createConfiguration({})", (null == configuration ? "MISSING" : configuration.getProcessorName()));

		if (null == configuration) {
			return new ResponseEntity<>(
					errorHeaders(MSG_CONFIGURATION_MISSING, MSG_ID_CONFIGURATION_MISSING),
					HttpStatus.BAD_REQUEST);
		}
		
		return transactionTemplate.execute(new TransactionCallback<>() {

			@Override
			public ResponseEntity<Configuration> doInTransaction(TransactionStatus txStatus) {
				de.dlr.proseo.model.Configuration modelConfiguration = ConfigurationUtil.toModelConfiguration(configuration);
				
				modelConfiguration.setProcessorClass(RepositoryService.getProcessorClassRepository()
						.findByMissionCodeAndProcessorName(configuration.getMissionCode(), configuration.getProcessorName()));
				if (null == modelConfiguration.getProcessorClass()) {
					txStatus.setRollbackOnly();
					return new ResponseEntity<>(
							errorHeaders(MSG_PROCESSOR_CLASS_INVALID, MSG_ID_PROCESSOR_CLASS_INVALID,
									configuration.getProcessorName(), configuration.getMissionCode()),
							HttpStatus.BAD_REQUEST);
				}
				
				for (ConfigurationInputFile staticInputFile: configuration.getStaticInputFiles()) {
					de.dlr.proseo.model.ConfigurationInputFile modelInputFile = new de.dlr.proseo.model.ConfigurationInputFile();
					if (!ALLOWED_FILENAME_TYPES.contains(staticInputFile.getFileNameType())) {
						txStatus.setRollbackOnly();
						return new ResponseEntity<>(
								errorHeaders(MSG_FILENAME_TYPE_INVALID, MSG_ID_FILENAME_TYPE_INVALID, staticInputFile.getFileNameType()),
								HttpStatus.BAD_REQUEST);
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
				
				return new ResponseEntity<>(ConfigurationUtil.toRestConfiguration(modelConfiguration), HttpStatus.CREATED);
			}
		});
	}

	/**
	 * Get a configuration by ID
	 * 
	 * @param id the configuration ID
	 * @return a Json object corresponding to the configuration found and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no configuration with the given ID exists
	 */
	@Override
	public ResponseEntity<Configuration> getConfigurationById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfigurationById({})", id);
		
		if (null == id) {
			return new ResponseEntity<>(
					errorHeaders(MSG_CONFIGURATION_ID_MISSING, MSG_ID_CONFIGURATION_ID_MISSING, id), 
					HttpStatus.BAD_REQUEST);
		}
		
		Optional<de.dlr.proseo.model.Configuration> modelConfiguration = RepositoryService.getConfigurationRepository().findById(id);
		
		if (modelConfiguration.isEmpty()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_CONFIGURATION_ID_NOT_FOUND, MSG_ID_CONFIGURATION_ID_NOT_FOUND, id), 
					HttpStatus.NOT_FOUND);
		}

		logInfo(MSG_CONFIGURATION_RETRIEVED, MSG_ID_CONFIGURATION_RETRIEVED, id);
		
		return new ResponseEntity<>(ConfigurationUtil.toRestConfiguration(modelConfiguration.get()), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Configuration> updateConfiguration(Long id, @Valid Configuration configuration) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders("PATCH for configuration not implemented", MSG_ID_NOT_IMPLEMENTED, id), 
				HttpStatus.NOT_IMPLEMENTED);
	}

	@Override
	public ResponseEntity<?> deleteConfigurationById(Long id) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders("DELETE for configuration not implemented", MSG_ID_NOT_IMPLEMENTED, id), 
				HttpStatus.NOT_IMPLEMENTED);
	}

}
