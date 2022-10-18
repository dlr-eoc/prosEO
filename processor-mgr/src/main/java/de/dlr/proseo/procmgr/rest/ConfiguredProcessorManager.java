/**
 * ConfiguredProcessorManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProcessorMgrMessage;
import de.dlr.proseo.model.Configuration;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.procmgr.rest.model.ConfiguredProcessorUtil;
import de.dlr.proseo.procmgr.rest.model.RestConfiguredProcessor;

/**
 * Service methods required to manage configured processor versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
@Transactional
public class ConfiguredProcessorManager {

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ConfiguredProcessorManager.class);
	
	/**
	 * Get configured processors, filtered by mission, identifier, processor name, processor version and/or configuration version
	 * 
	 * @param mission the mission code
	 * @param identifier the identifier for the configured processor
	 * @param processorName the processor name
	 * @param processorVersion the processor version
	 * @param configurationVersion the configuration version
	 * @param uuid the UUID of the configured processor
	 * @return a list of Json objects representing configured processors satisfying the search criteria
	 * @throws NoResultException if no configured processors matching the given search criteria could be found
     * @throws SecurityException if a cross-mission data access was attempted
	 */
	public List<RestConfiguredProcessor> getConfiguredProcessors(String mission, String identifier,
			String processorName, String processorVersion, String configurationVersion, String uuid)
					throws NoResultException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfiguredProcessors({}, {}, {}, {}, {}, {})", 
				mission, identifier, processorName, processorVersion, configurationVersion, uuid);
		
		if (null == mission) {
			mission = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(mission)) {
				throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
						mission, securityService.getMission()));
			} 
		}
		
		List<RestConfiguredProcessor> result = new ArrayList<>();
		
		String jpqlQuery = "select c from ConfiguredProcessor c where c.processor.processorClass.mission.code = :missionCode";
		if (null != identifier) {
			jpqlQuery += " and c.identifier = :identifier";
		}
		if (null != processorName) {
			jpqlQuery += " and c.processor.processorClass.processorName = :processorName";
		}
		if (null != processorVersion) {
			jpqlQuery += " and c.processor.processorVersion = :processorVersion";
		}
		if (null != configurationVersion) {
			jpqlQuery += " and c.configuration.configurationVersion = :configurationVersion";
		}
		if (null != uuid) {
			jpqlQuery += " and c.uuid = :uuid";
		}
		Query query = em.createQuery(jpqlQuery);
		query.setParameter("missionCode", mission);
		if (null != identifier) {
			query.setParameter("identifier", identifier);
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
		if (null != uuid) {
			query.setParameter("uuid", uuid);
		}
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof ConfiguredProcessor) {
				result.add(ConfiguredProcessorUtil.toRestConfiguredProcessor((ConfiguredProcessor) resultObject));
			}
		}
		if (result.isEmpty()) {
			throw new NoResultException(logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_NOT_FOUND,
					mission, identifier, processorName, processorVersion, configurationVersion));
		}

		logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_LIST_RETRIEVED,
				mission, identifier, processorName, processorVersion, configurationVersion);
		
		return result;
	}

	/**
     * Create a new configured processor
     * 
     * @param configuredProcessor a Json representation of the new configured processor
	 * @return a Json representation of the configured processor after creation (with ID and version number)
	 * @throws IllegalArgumentException if any of the input data was invalid
     * @throws SecurityException if a cross-mission data access was attempted
	 */
	public RestConfiguredProcessor createConfiguredProcessor(@Valid RestConfiguredProcessor configuredProcessor)
			throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> createConfiguredProcessor({})",
				(null == configuredProcessor ? "MISSING" : configuredProcessor.getProcessorName()));

		if (null == configuredProcessor) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_MISSING));
		}
		
		// Ensure user is authorized for the mission of the configured processor
		if (!securityService.isAuthorizedForMission(configuredProcessor.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					configuredProcessor.getMissionCode(), securityService.getMission()));			
		}
		
		ConfiguredProcessor modelConfiguredProcessor = ConfiguredProcessorUtil.toModelConfiguredProcessor(configuredProcessor);
		// Make sure configured processor has a UUID
		if (null == modelConfiguredProcessor.getUuid()) {
			modelConfiguredProcessor.setUuid(UUID.randomUUID());
		} else {
			// Test if given UUID is not yet in use
			if (null != RepositoryService.getConfiguredProcessorRepository().findByUuid(modelConfiguredProcessor.getUuid())) {
				throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.DUPLICATE_CONFPROC_UUID, 
						modelConfiguredProcessor.getUuid()));
			}
		}
		
		// Make sure a configured processor with the same identifier does not yet exist for the mission
		if (null != RepositoryService.getConfiguredProcessorRepository()
				.findByMissionCodeAndIdentifier(configuredProcessor.getMissionCode(), configuredProcessor.getIdentifier())) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.DUPLICATE_CONFPROC_ID,
					configuredProcessor.getIdentifier()));
		}

		modelConfiguredProcessor.setProcessor(RepositoryService.getProcessorRepository()
				.findByMissionCodeAndProcessorNameAndProcessorVersion(
						configuredProcessor.getMissionCode(),
						configuredProcessor.getProcessorName(),
						configuredProcessor.getProcessorVersion()));
		if (null == modelConfiguredProcessor.getProcessor()) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_INVALID,
					configuredProcessor.getProcessorName(),
					configuredProcessor.getProcessorVersion(),
					configuredProcessor.getMissionCode()));
		}
		
		modelConfiguredProcessor.setConfiguration(RepositoryService.getConfigurationRepository()
				.findByMissionCodeAndProcessorNameAndConfigurationVersion(
						configuredProcessor.getMissionCode(),
						configuredProcessor.getProcessorName(),
						configuredProcessor.getConfigurationVersion()));
		if (null == modelConfiguredProcessor.getConfiguration()) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURATION_INVALID,
					configuredProcessor.getProcessorName(),
					configuredProcessor.getConfigurationVersion(),
					configuredProcessor.getMissionCode()));
		}
		
		modelConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository().save(modelConfiguredProcessor);
		
		logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_CREATED, 
				modelConfiguredProcessor.getProcessor().getProcessorClass().getProcessorName(),
				modelConfiguredProcessor.getProcessor().getProcessorVersion(),
				modelConfiguredProcessor.getConfiguration().getConfigurationVersion(), 
				modelConfiguredProcessor.getProcessor().getProcessorClass().getMission().getCode());
		
		return ConfiguredProcessorUtil.toRestConfiguredProcessor(modelConfiguredProcessor);
	}

	/**
	 * Get a configured processor by ID
	 * 
	 * @param id the configured processor ID
	 * @return a Json object corresponding to the configured processor found
	 * @throws IllegalArgumentException if no configured processor ID was given
	 * @throws NoResultException if no configured processor with the given ID exists
     * @throws SecurityException if a cross-mission data access was attempted
	 */
	public RestConfiguredProcessor getConfiguredProcessorById(Long id)
			throws IllegalArgumentException, NoResultException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getConfiguredProcessorById({})", id);
		
		if (null == id) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_ID_MISSING, id));
		}
		
		Optional<de.dlr.proseo.model.ConfiguredProcessor> modelConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository().findById(id);
		
		if (modelConfiguredProcessor.isEmpty()) {
			throw new NoResultException(logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_ID_NOT_FOUND, id));
		}

		// Ensure user is authorized for the mission of the configured processor
		if (!securityService.isAuthorizedForMission(modelConfiguredProcessor.get().getProcessor().getProcessorClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelConfiguredProcessor.get().getProcessor().getProcessorClass().getMission().getCode(), securityService.getMission()));			
		}
		
		logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_RETRIEVED, id);
		
		return ConfiguredProcessorUtil.toRestConfiguredProcessor(modelConfiguredProcessor.get());
	}

	/**
	 * Update a configured processor by ID
	 * 
	 * @param id the ID of the configured processor to update
	 * @param configuredProcessor a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the configured processor after modification (with ID and version for all 
	 * 		   contained objects)
	 * @throws EntityNotFoundException if no configured processor with the given ID exists
	 * @throws IllegalArgumentException if any of the input data was invalid
     * @throws SecurityException if a cross-mission data access was attempted
	 * @throws ConcurrentModificationException if the configured processor has been modified since retrieval by the client
	 */
	public RestConfiguredProcessor modifyConfiguredProcessor(Long id, @Valid RestConfiguredProcessor configuredProcessor) throws
			EntityNotFoundException, IllegalArgumentException, SecurityException, ConcurrentModificationException {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyConfiguredProcessor({}, {})", id, (null == configuredProcessor ? "MISSING" : configuredProcessor.getIdentifier()));

		// Check arguments
		if (null == id) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_ID_MISSING, id));
		}
		if (null == configuredProcessor) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_DATA_MISSING));
		}
		
		// Ensure user is authorized for the mission of the configured processor
		if (!securityService.isAuthorizedForMission(configuredProcessor.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					configuredProcessor.getMissionCode(), securityService.getMission()));			
		}
		
		Optional<de.dlr.proseo.model.ConfiguredProcessor> optConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository().findById(id);
		
		if (optConfiguredProcessor.isEmpty()) {
			throw new NoResultException(logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_ID_NOT_FOUND, id));
		}
		ConfiguredProcessor modelConfiguredProcessor = optConfiguredProcessor.get();
		
		// Make sure we are allowed to change the configured processor (no intermediate update)
		if (modelConfiguredProcessor.getVersion() != configuredProcessor.getVersion().intValue()) {
			throw new ConcurrentModificationException(logger.log(ProcessorMgrMessage.CONCURRENT_UPDATE, id));
		}
		
		// Apply changed attributes
		ConfiguredProcessor changedConfiguredProcessor = ConfiguredProcessorUtil.toModelConfiguredProcessor(configuredProcessor);
		
		boolean configuredProcessorChanged = false;
		if (!modelConfiguredProcessor.getIdentifier().equals(changedConfiguredProcessor.getIdentifier())) {
			configuredProcessorChanged = true;
			modelConfiguredProcessor.setIdentifier(changedConfiguredProcessor.getIdentifier());
		}
		if (!modelConfiguredProcessor.getEnabled().equals(changedConfiguredProcessor.getEnabled())) {
			configuredProcessorChanged = true;
			modelConfiguredProcessor.setEnabled(changedConfiguredProcessor.getEnabled());
		}

		Processor changedProcessor = RepositoryService.getProcessorRepository()
				.findByMissionCodeAndProcessorNameAndProcessorVersion(
						configuredProcessor.getMissionCode(),
						configuredProcessor.getProcessorName(),
						configuredProcessor.getProcessorVersion());
		if (null == changedProcessor) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_INVALID,
					configuredProcessor.getProcessorName(),
					configuredProcessor.getProcessorVersion(),
					configuredProcessor.getMissionCode()));
		}
		if (!changedProcessor.equals(modelConfiguredProcessor.getProcessor())) {
			configuredProcessorChanged = true;
			modelConfiguredProcessor.getProcessor().getConfiguredProcessors().remove(modelConfiguredProcessor);
			RepositoryService.getProcessorRepository().save(modelConfiguredProcessor.getProcessor());
			modelConfiguredProcessor.setProcessor(changedProcessor);
		}
		
		Configuration changedConfiguration = RepositoryService.getConfigurationRepository()
				.findByMissionCodeAndProcessorNameAndConfigurationVersion(
						configuredProcessor.getMissionCode(),
						configuredProcessor.getProcessorName(),
						configuredProcessor.getConfigurationVersion());
		if (null == changedConfiguration) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURATION_INVALID,
					configuredProcessor.getProcessorName(),
					configuredProcessor.getProcessorVersion(),
					configuredProcessor.getMissionCode()));
		}
		if (!changedConfiguration.equals(modelConfiguredProcessor.getConfiguration())) {
			configuredProcessorChanged = true;
			modelConfiguredProcessor.getConfiguration().getConfiguredProcessors().remove(modelConfiguredProcessor);
			RepositoryService.getConfigurationRepository().save(modelConfiguredProcessor.getConfiguration());
			modelConfiguredProcessor.setConfiguration(changedConfiguration);
		}

		// Save configured processor only if anything was actually changed
		if (configuredProcessorChanged) {
			modelConfiguredProcessor.incrementVersion();
			modelConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository().save(modelConfiguredProcessor);
			logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_MODIFIED, id);
		} else {
			logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_NOT_MODIFIED, id);
		}
		
		return ConfiguredProcessorUtil.toRestConfiguredProcessor(modelConfiguredProcessor);
	}

	/**
	 * Delete a configured processor by ID
	 * 
	 * @param id the ID of the configured processor to delete
	 * @throws EntityNotFoundException if the configured processor to delete does not exist in the database
     * @throws SecurityException if a cross-mission data access was attempted
	 * @throws RuntimeException if the deletion was not performed as expected
	 */
	public void deleteConfiguredProcessorById(Long id) throws EntityNotFoundException, SecurityException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteConfiguredProcessorById({})", id);

		// Check arguments
		if (null == id) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_ID_MISSING, id));
		}
		
		Optional<de.dlr.proseo.model.ConfiguredProcessor> modelConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository().findById(id);
		
		if (modelConfiguredProcessor.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_ID_NOT_FOUND, id));
		}
		
		// Ensure user is authorized for the mission of the configured processor
		if (!securityService.isAuthorizedForMission(modelConfiguredProcessor.get().getProcessor().getProcessorClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelConfiguredProcessor.get().getProcessor().getProcessorClass().getMission().getCode(), securityService.getMission()));			
		}
		
		// Check whether there are still products referencing this configured processor
		// (restricted to the product classes processable by the processor class for efficiency)
		ProcessorClass processorClass = modelConfiguredProcessor.get().getProcessor().getProcessorClass();
		if (!processorClass.getProductClasses().isEmpty()) {
			String jpqlQuery = "select count(p) from Product p where p.productClass in :productClasses and p.configuredProcessor = :configuredProcessor";
			Query query = em.createQuery(jpqlQuery);
			query.setParameter("productClasses", processorClass.getProductClasses());
			query.setParameter("configuredProcessor", modelConfiguredProcessor.get());
			Object result = query.getSingleResult();
			if (!(result instanceof Number) || 0 != ((Number) result).intValue()) {
				throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_HAS_PRODUCTS, modelConfiguredProcessor.get().getIdentifier(), result));
			} 
		}
		// Check whether there are selection rules referencing this configured processor
		String sqlQuery = "SELECT COUNT(*) FROM simple_selection_rule_applicable_configured_processors WHERE applicable_configured_processors_id = :id";
		Query query = em.createNativeQuery(sqlQuery);
		query.setParameter("id", modelConfiguredProcessor.get().getId());
		Object result = query.getSingleResult();
		if (!(result instanceof Number) || 0 != ((Number) result).intValue()) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_HAS_SELECTION_RULES,
					modelConfiguredProcessor.get().getIdentifier(), result));
		}
		
		// Delete the configured processor
		RepositoryService.getConfiguredProcessorRepository().deleteById(id);

		// Test whether the deletion was successful
		modelConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository().findById(id);
		if (!modelConfiguredProcessor.isEmpty()) {
			throw new RuntimeException(logger.log(ProcessorMgrMessage.DELETION_UNSUCCESSFUL, id));
		}
		
		logger.log(ProcessorMgrMessage.CONFIGURED_PROCESSOR_DELETED, id);
	}

}
