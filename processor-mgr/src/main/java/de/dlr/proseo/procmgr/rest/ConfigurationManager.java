/**
 * ConfigurationManager.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProcessorMgrMessage;
import de.dlr.proseo.model.Configuration;
import de.dlr.proseo.model.ConfigurationFile;
import de.dlr.proseo.model.ConfigurationInputFile;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.enums.ProductQuality;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.procmgr.ProcessorManagerConfiguration;
import de.dlr.proseo.procmgr.rest.model.ConfigurationUtil;
import de.dlr.proseo.procmgr.rest.model.RestConfiguration;
import de.dlr.proseo.procmgr.rest.model.RestConfigurationInputFile;

/**
 * Service methods required to manage configuration versions.
 *
 * @author Dr. Thomas Bassler
 */
@Component
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class ConfigurationManager {

	/** Allowed filename types for static input files (in lower case for easier comparation) */
	private static final List<String> ALLOWED_FILENAME_TYPES = Arrays.asList("physical", "logical", "stem", "regexp", "directory");

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** The processor manager configuration */
	@Autowired
	ProcessorManagerConfiguration config;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ConfigurationManager.class);

	/**
	 * Get configurations by mission, processor name and configuration version
	 *
	 * @param mission              the mission code
	 * @param processorName        the name of the processor class this configuration belongs to
	 * @param configurationVersion the configuration version
	 * @param recordFrom           first record of filtered and ordered result to return
	 * @param recordTo             last record of filtered and ordered result to return
	 * @param orderBy		an array of strings containing a column name and an optional sort direction (ASC/DESC), separated by white space
	 * @return a list of Json objects representing configurations satisfying the search criteria
	 * @throws NoResultException if no processor classes matching the given search criteria could be found
	 * @throws SecurityException if a cross-mission data access was attempted
	 */
	public List<RestConfiguration> getConfigurations(String mission, Long id, String processorName[], String configurationVersion, 
			String productQuality[], String processingMode[],
			Integer recordFrom, Integer recordTo, String[] orderBy) throws NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getConfigurations({}, {}, {})", mission, processorName, configurationVersion);

		if (null == mission) {
			mission = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(mission)) {
				throw new SecurityException(
						logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, mission, securityService.getMission()));
			}
		}

		if (recordFrom == null) {
			recordFrom = 0;
		}
		if (recordTo == null) {
			recordTo = Integer.MAX_VALUE;
		}

		Long numberOfResults = Long.parseLong(this.countConfigurations(mission, id, processorName, configurationVersion, productQuality, processingMode));
		Integer maxResults = config.getMaxResults();
		if (numberOfResults > maxResults && (recordTo - recordFrom) > maxResults && (numberOfResults - recordFrom) > maxResults) {
			throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS,
					logger.log(GeneralMessage.TOO_MANY_RESULTS, "workflows", numberOfResults, config.getMaxResults()));
		}

		List<RestConfiguration> result = new ArrayList<>();
		
		Query query = createConfigurationsQuery(mission, id, processorName, configurationVersion, 
				productQuality, processingMode, orderBy, false);

		query.setFirstResult(recordFrom);
		query.setMaxResults(recordTo - recordFrom);

		for (Object resultObject : query.getResultList()) {
			if (resultObject instanceof de.dlr.proseo.model.Configuration) {
				result.add(ConfigurationUtil.toRestConfiguration((de.dlr.proseo.model.Configuration) resultObject));
			}
		}
		if (result.isEmpty()) {
			throw new NoResultException(
					logger.log(ProcessorMgrMessage.CONFIGURATION_NOT_FOUND, mission, processorName, configurationVersion));
		}

		logger.log(ProcessorMgrMessage.CONFIGURATION_LIST_RETRIEVED, mission, processorName, configurationVersion);

		return result;
	}

	/**
	 * Create a new configuration
	 *
	 * @param configuration a Json representation of the new configuration
	 * @return a Json representation of the configuration after creation (with ID and version number)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public RestConfiguration createConfiguration(@Valid RestConfiguration configuration)
			throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createConfiguration({})", (null == configuration ? "MISSING" : configuration.getProcessorName()));

		if (null == configuration) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURATION_MISSING));
		}

		// Ensure user is authorized for the mission of the configuration
		if (!securityService.isAuthorizedForMission(configuration.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, configuration.getMissionCode(),
					securityService.getMission()));
		}

		// Ensure mandatory attributes are set
		if (null == configuration.getProcessorName() || configuration.getProcessorName().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "processorName", "configuration creation"));
		}
		if (null == configuration.getConfigurationVersion() || configuration.getConfigurationVersion().isBlank()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "configurationVersion", "configuration creation"));
		}

		// If list attributes were set to null explicitly, initialize with empty lists
		if (null == configuration.getDynProcParameters()) {
			configuration.setDynProcParameters(new ArrayList<>());
		}
		if (null == configuration.getConfigurationFiles()) {
			configuration.setConfigurationFiles(new ArrayList<>());
		}
		if (null == configuration.getStaticInputFiles()) {
			configuration.setStaticInputFiles(new ArrayList<>());
		}
		if (null == configuration.getConfiguredProcessors()) {
			configuration.setConfiguredProcessors(new ArrayList<>());
		}
		if (null == configuration.getDockerRunParameters()) {
			configuration.setDockerRunParameters(new ArrayList<>());
		}

		Configuration modelConfiguration = ConfigurationUtil.toModelConfiguration(configuration);

		// Make sure a configuration with the same processor class name and configuration version does not yet exist
		if (null != RepositoryService.getConfigurationRepository()
			.findByMissionCodeAndProcessorNameAndConfigurationVersion(configuration.getMissionCode(),
					configuration.getProcessorName(), configuration.getConfigurationVersion())) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.DUPLICATE_CONFIGURATION,
					configuration.getMissionCode(), configuration.getProcessorName(), configuration.getConfigurationVersion()));
		}

		modelConfiguration.setProcessorClass(RepositoryService.getProcessorClassRepository()
			.findByMissionCodeAndProcessorName(configuration.getMissionCode(), configuration.getProcessorName()));
		if (null == modelConfiguration.getProcessorClass()) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_INVALID,
					configuration.getProcessorName(), configuration.getMissionCode()));
		}

		// Make sure the processing mode, if set, is valid
		if (null != modelConfiguration.getMode() && !modelConfiguration.getProcessorClass()
			.getMission()
			.getProcessingModes()
			.contains(modelConfiguration.getMode())) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.INVALID_PROCESSING_MODE, modelConfiguration.getMode(),
					modelConfiguration.getProcessorClass().getMission().getCode()));
		}

		for (RestConfigurationInputFile staticInputFile : configuration.getStaticInputFiles()) {
			if (!ALLOWED_FILENAME_TYPES.contains(staticInputFile.getFileNameType().toLowerCase())) {
				throw new IllegalArgumentException(
						logger.log(ProcessorMgrMessage.FILENAME_TYPE_INVALID, staticInputFile.getFileNameType()));
			}
			ConfigurationInputFile modelInputFile = new ConfigurationInputFile();
			modelInputFile.setFileType(staticInputFile.getFileType());
			modelInputFile.setFileNameType(staticInputFile.getFileNameType());
			modelInputFile.getFileNames().addAll(staticInputFile.getFileNames());
			modelConfiguration.getStaticInputFiles().add(modelInputFile);
		}

		modelConfiguration = RepositoryService.getConfigurationRepository().save(modelConfiguration);

		logger.log(ProcessorMgrMessage.CONFIGURATION_CREATED, modelConfiguration.getProcessorClass().getProcessorName(),
				modelConfiguration.getConfigurationVersion(), modelConfiguration.getProcessorClass().getMission().getCode());

		return ConfigurationUtil.toRestConfiguration(modelConfiguration);
	}

	/**
	 * Get a configuration by ID
	 *
	 * @param id the configuration ID
	 * @return a Json object corresponding to the configuration found
	 * @throws IllegalArgumentException if no configuration ID was given
	 * @throws NoResultException        if no configuration with the given ID exists
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public RestConfiguration getConfigurationById(Long id) throws IllegalArgumentException, NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getConfigurationById({})", id);

		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURATION_ID_MISSING, id));
		}

		Optional<de.dlr.proseo.model.Configuration> modelConfiguration = RepositoryService.getConfigurationRepository()
			.findById(id);

		if (modelConfiguration.isEmpty()) {
			throw new NoResultException(logger.log(ProcessorMgrMessage.CONFIGURATION_ID_NOT_FOUND, id));
		}

		// Ensure user is authorized for the mission of the configuration
		if (!securityService.isAuthorizedForMission(modelConfiguration.get().getProcessorClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelConfiguration.get().getProcessorClass().getMission().getCode(), securityService.getMission()));
		}

		logger.log(ProcessorMgrMessage.CONFIGURATION_RETRIEVED, id);

		return ConfigurationUtil.toRestConfiguration(modelConfiguration.get());
	}

	/**
	 * Update a configuration by ID
	 *
	 * @param id            the ID of the configuration to update
	 * @param configuration a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the configuration after modification (with ID and version for
	 *         all contained objects)
	 * @throws EntityNotFoundException         if no configuration with the given ID exists
	 * @throws IllegalArgumentException        if any of the input data was invalid
	 * @throws ConcurrentModificationException if the configuration has been modified since retrieval by the client
	 * @throws SecurityException               if a cross-mission data access was attempted
	 */
	public RestConfiguration modifyConfiguration(Long id, @Valid RestConfiguration configuration)
			throws EntityNotFoundException, IllegalArgumentException, SecurityException, ConcurrentModificationException {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyConfiguration({}, {})", id, (null == configuration ? "MISSING"
					: configuration.getProcessorName() + " " + configuration.getConfigurationVersion()));

		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURATION_ID_MISSING));
		}
		if (null == configuration) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURATION_DATA_MISSING));
		}

		// Ensure user is authorized for the mission of the configuration
		if (!securityService.isAuthorizedForMission(configuration.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, configuration.getMissionCode(),
					securityService.getMission()));
		}

		Optional<Configuration> optConfiguration = RepositoryService.getConfigurationRepository().findById(id);

		if (optConfiguration.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProcessorMgrMessage.CONFIGURATION_ID_NOT_FOUND, id));
		}
		Configuration modelConfiguration = optConfiguration.get();

		// Ensure mandatory attributes are set
		if (null == configuration.getProcessorName() || configuration.getProcessorName().isBlank()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "processorName", "configuration modification"));
		}
		if (null == configuration.getConfigurationVersion() || configuration.getConfigurationVersion().isBlank()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "configurationVersion", "configuration modification"));
		}

		// If list attributes were set to null explicitly, initialize with empty lists
		if (null == configuration.getDynProcParameters()) {
			configuration.setDynProcParameters(new ArrayList<>());
		}
		if (null == configuration.getConfigurationFiles()) {
			configuration.setConfigurationFiles(new ArrayList<>());
		}
		if (null == configuration.getStaticInputFiles()) {
			configuration.setStaticInputFiles(new ArrayList<>());
		}
		if (null == configuration.getConfiguredProcessors()) {
			configuration.setConfiguredProcessors(new ArrayList<>());
		}
		if (null == configuration.getDockerRunParameters()) {
			configuration.setDockerRunParameters(new ArrayList<>());
		}

		// Make sure we are allowed to change the configuration (no intermediate update)
		if (modelConfiguration.getVersion() != configuration.getVersion().intValue()) {
			throw new ConcurrentModificationException(logger.log(ProcessorMgrMessage.CONCURRENT_UPDATE, id));
		}

		// Apply changed attributes
		Configuration changedConfiguration = ConfigurationUtil.toModelConfiguration(configuration);

		boolean configurationChanged = false;
		if (!modelConfiguration.getConfigurationVersion().equals(changedConfiguration.getConfigurationVersion())) {
			configurationChanged = true;
			modelConfiguration.setConfigurationVersion(changedConfiguration.getConfigurationVersion());
		}
		if (!Objects.equals(modelConfiguration.getMode(), changedConfiguration.getMode())) {
			if (null == changedConfiguration.getMode() || modelConfiguration.getProcessorClass()
				.getMission()
				.getProcessingModes()
				.contains(changedConfiguration.getMode())) {
				configurationChanged = true;
				modelConfiguration.setMode(changedConfiguration.getMode());
			} else {
				throw new IllegalArgumentException(logger.log(GeneralMessage.INVALID_PROCESSING_MODE,
						changedConfiguration.getMode(), modelConfiguration.getProcessorClass().getMission().getCode()));
			}
		}
		if (!modelConfiguration.getProductQuality().equals(changedConfiguration.getProductQuality())) {
			configurationChanged = true;
			modelConfiguration.setProductQuality(changedConfiguration.getProductQuality());
		}
		if (null == modelConfiguration.getDockerRunParameters() && null != changedConfiguration.getDockerRunParameters()
				|| null != modelConfiguration.getDockerRunParameters()
						&& !modelConfiguration.getDockerRunParameters().equals(changedConfiguration.getDockerRunParameters())) {
			configurationChanged = true;
			modelConfiguration.setDockerRunParameters(changedConfiguration.getDockerRunParameters());
		}

		// Check for new or changed parameters
		Map<String, Parameter> newParameters = new HashMap<>();
		for (String paramKey : changedConfiguration.getDynProcParameters().keySet()) {
			Parameter changedParameter = changedConfiguration.getDynProcParameters().get(paramKey);
			Parameter modelParameter = modelConfiguration.getDynProcParameters().get(paramKey);
			if (null == modelParameter || !modelParameter.equals(changedParameter)) {
				configurationChanged = true;
				newParameters.put(paramKey, changedParameter);
			} else {
				newParameters.put(paramKey, modelParameter);
			}
		}
		// Check for removed parameters
		if (!newParameters.keySet().equals(modelConfiguration.getDynProcParameters().keySet())) {
			configurationChanged = true;
		}

		// Check for new or changed configuration files
		Set<ConfigurationFile> newFiles = new HashSet<>();
		for (ConfigurationFile changedFile : changedConfiguration.getConfigurationFiles()) {
			if (!modelConfiguration.getConfigurationFiles().contains(changedFile)) {
				configurationChanged = true;
			}
			newFiles.add(changedFile);
		}
		// Check for removed configuration files
		if (!newFiles.equals(modelConfiguration.getConfigurationFiles())) {
			configurationChanged = true;
		}

		// Check for new or changed static input files
		Set<ConfigurationInputFile> newInputFiles = new HashSet<>();
		for (RestConfigurationInputFile restInputFile : configuration.getStaticInputFiles()) {
			if (null == restInputFile.getId() || 0 == restInputFile.getId()) {
				// New static input file
				configurationChanged = true;
				ConfigurationInputFile changedInputFile = new ConfigurationInputFile();
				changedInputFile.setFileType(restInputFile.getFileType());
				changedInputFile.setFileNameType(restInputFile.getFileNameType());
				changedInputFile.getFileNames().addAll(restInputFile.getFileNames());
				newInputFiles.add(changedInputFile);
				continue;
			}
			ConfigurationInputFile modelInputFile = null;
			for (ConfigurationInputFile inputFile : modelConfiguration.getStaticInputFiles()) {
				if (inputFile.getId() == restInputFile.getId().longValue()) {
					modelInputFile = inputFile;
					break;
				}
			}
			if (null == modelInputFile) {
				throw new EntityNotFoundException(logger.log(ProcessorMgrMessage.INPUT_FILE_ID_NOT_FOUND, id));
			}
			if (!modelInputFile.getFileType().equals(restInputFile.getFileType())) {
				configurationChanged = true;
				modelInputFile.setFileType(restInputFile.getFileType());
			}
			if (!modelInputFile.getFileNameType().equals(restInputFile.getFileNameType())) {
				configurationChanged = true;
				modelInputFile.setFileNameType(restInputFile.getFileNameType());
			}
			if (!modelInputFile.getFileNames().equals(restInputFile.getFileNames())) {
				configurationChanged = true;
				modelInputFile.setFileNames(restInputFile.getFileNames());
			}
			newInputFiles.add(modelInputFile);
		}
		// Check for removed static input files
		for (ConfigurationInputFile inputFile : modelConfiguration.getStaticInputFiles()) {
			if (!newInputFiles.contains(inputFile)) {
				configurationChanged = true;
			}
		}

		// Save configuration only if anything was actually changed
		if (configurationChanged) {
			modelConfiguration.incrementVersion();
			modelConfiguration.getDynProcParameters().clear();
			modelConfiguration.getDynProcParameters().putAll(newParameters);
			modelConfiguration.getConfigurationFiles().clear();
			modelConfiguration.getConfigurationFiles().addAll(newFiles);
			modelConfiguration.getStaticInputFiles().clear();
			modelConfiguration.getStaticInputFiles().addAll(newInputFiles);
			modelConfiguration = RepositoryService.getConfigurationRepository().save(modelConfiguration);
			logger.log(ProcessorMgrMessage.CONFIGURATION_MODIFIED, id);
		} else {
			logger.log(ProcessorMgrMessage.CONFIGURATION_NOT_MODIFIED, id);
		}

		return ConfigurationUtil.toRestConfiguration(modelConfiguration);
	}

	/**
	 * Delete a configuration by ID
	 *
	 * @param id the ID of the configuration to delete
	 * @throws EntityNotFoundException  if the configuration to delete does not exist in the database
	 * @throws RuntimeException         if the deletion was not performed as expected
	 * @throws IllegalArgumentException if the ID of the processor class to delete was not given, or if dependent objects exist
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public void deleteConfigurationById(Long id)
			throws EntityNotFoundException, RuntimeException, IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteConfigurationById({})", id);

		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURATION_ID_MISSING));
		}

		// Test whether the configuration id is valid
		Optional<Configuration> modelConfiguration = RepositoryService.getConfigurationRepository().findById(id);
		if (modelConfiguration.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProcessorMgrMessage.CONFIGURATION_ID_NOT_FOUND, id));
		}

		// Ensure user is authorized for the mission of the configuration
		if (!securityService.isAuthorizedForMission(modelConfiguration.get().getProcessorClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelConfiguration.get().getProcessorClass().getMission().getCode(), securityService.getMission()));
		}

		// Check whether there are configured processors for this configuration
		if (!modelConfiguration.get().getConfiguredProcessors().isEmpty()) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURATION_HAS_PROC,
					modelConfiguration.get().getProcessorClass().getMission().getCode(),
					modelConfiguration.get().getProcessorClass().getProcessorName(),
					modelConfiguration.get().getConfigurationVersion()));
		}

		// Delete the configuration
		RepositoryService.getConfigurationRepository().deleteById(id);

		// Test whether the deletion was successful
		modelConfiguration = RepositoryService.getConfigurationRepository().findById(id);
		if (!modelConfiguration.isEmpty()) {
			throw new RuntimeException(logger.log(ProcessorMgrMessage.DELETION_UNSUCCESSFUL, id));
		}

		logger.log(ProcessorMgrMessage.CONFIGURATION_DELETED, id);
	}

	/**
	 * Count the configurations matching the specified mission, processorName, or configurationVersion
	 *
	 * @param missionCode          the mission code
	 * @param processorName        the name of the processor class this configuration belongs to
	 * @param configurationVersion the configuration version
	 * @return the number of configurations found as string
	 * @throws SecurityException if a cross-mission data access was attempted
	 */
	public String countConfigurations(String missionCode, Long id, String processorName[], String configurationVersion, 
			String productQuality[], String processingMode[]) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countConfigurations({}, {}, {})", missionCode, processorName, configurationVersion);

		if (null == missionCode) {
			missionCode = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(missionCode)) {
				throw new SecurityException(
						logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, missionCode, securityService.getMission()));
			}
		}

		// build query

		Query query = createConfigurationsQuery(missionCode, id, processorName, configurationVersion, 
				productQuality, processingMode, null, true);

		Object resultObject = query.getSingleResult();
		if (resultObject instanceof Long) {
			return ((Long) resultObject).toString();
		}
		if (resultObject instanceof String) {
			return (String) resultObject;
		}
		return "0";
	}
	
	private Query createConfigurationsQuery(String mission, Long id, String[] processorName, String configurationVersion, 
			String productQuality[], String processingMode[], String[] orderBy, Boolean count) {
		String jpqlQuery = "";
		if (count) {
			jpqlQuery = "select count(pc) from Configuration  pc ";
		} else {
			jpqlQuery = "select pc from Configuration pc ";
		}
		jpqlQuery += " where pc.processorClass.mission.code = :missionCode";
		if (null != configurationVersion) {
			jpqlQuery += "  and upper(configurationVersion) like :configurationVersion";;
		}
		if (null != id && id > 0) {
			jpqlQuery += " and pc.id = :id";
		}
		if (null != processorName && 0 < processorName.length) {
			jpqlQuery += " and pc.processorClass.processorName in (";
			for (int i = 0; i < processorName.length; ++i) {
				if (0 < i)
					jpqlQuery += ", ";
				jpqlQuery += ":processorName" + i;
			}
			jpqlQuery += ") ";
			
		}
		if (null != productQuality && 0 < productQuality.length) {
			jpqlQuery += " and pc.productQuality in (";
			for (int i = 0; i < productQuality.length; ++i) {
				if (0 < i)
					jpqlQuery += ", ";
				jpqlQuery += ":productQuality" + i;
			}
			jpqlQuery += ") ";
			
		}
		if (null != processingMode && 0 < processingMode.length) {
			jpqlQuery += " and pc.mode in (";
			for (int i = 0; i < processingMode.length; ++i) {
				if (0 < i)
					jpqlQuery += ", ";
				jpqlQuery += ":processingMode" + i;
			}
			jpqlQuery += ") ";
			
		}
		if (!count) {
			// order by
			if (null != orderBy && 0 < orderBy.length) {
				jpqlQuery += " order by ";
				for (int i = 0; i < orderBy.length; ++i) {
					if (0 < i)
						jpqlQuery += ", ";
					jpqlQuery += "pc.";
					jpqlQuery += orderBy[i];
				}
			}
			
		}
		Query query = em.createQuery(jpqlQuery);
		query.setParameter("missionCode", mission);
		if (null != id && id > 0) {
			query.setParameter("id", id);
		}
		if (null != configurationVersion) {
			query.setParameter("configurationVersion", configurationVersion);
		}
		if (null != processorName && 0 < processorName.length) {
			for (int i = 0; i < processorName.length; ++i) {
				query.setParameter("processorName" + i, processorName[i]);
			}
		}
		if (null != productQuality && 0 < productQuality.length) {
			for (int i = 0; i < productQuality.length; ++i) {
				query.setParameter("productQuality" + i, ProductQuality.valueOf(productQuality[i]));
			}
		}
		if (null != processingMode && 0 < processingMode.length) {
			for (int i = 0; i < processingMode.length; ++i) {
				query.setParameter("processingMode" + i, processingMode[i]);
			}
		}
		
		return query;
	}
}