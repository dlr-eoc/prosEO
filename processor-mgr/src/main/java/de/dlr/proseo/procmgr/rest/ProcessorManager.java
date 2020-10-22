/**
 * ProcessorManager.java
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.Task;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.procmgr.rest.model.RestProcessor;
import de.dlr.proseo.procmgr.rest.model.ProcessorUtil;
import de.dlr.proseo.procmgr.rest.model.RestTask;
import de.dlr.proseo.procmgr.rest.model.TaskUtil;

/**
 * Service methods required to manage processor versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
@Transactional
public class ProcessorManager {
	
	/* Message ID constants */
	private static final int MSG_ID_PROCESSOR_NOT_FOUND = 2250;
	private static final int MSG_ID_PROCESSOR_LIST_RETRIEVED = 2251;
	private static final int MSG_ID_PROCESSOR_RETRIEVED = 2252;
	private static final int MSG_ID_PROCESSOR_MISSING = 2253;
	private static final int MSG_ID_PROCESSOR_CLASS_INVALID = 2254;
	private static final int MSG_ID_PROCESSOR_CREATED = 2255;
	private static final int MSG_ID_PROCESSOR_ID_MISSING = 2256;
	private static final int MSG_ID_PROCESSOR_ID_NOT_FOUND = 2257;
	private static final int MSG_ID_PROCESSOR_DATA_MISSING = 2258;
	private static final int MSG_ID_PROCESSOR_MODIFIED = 2259;
	private static final int MSG_ID_PROCESSOR_NOT_MODIFIED = 2260;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 2261;
	private static final int MSG_ID_PROCESSOR_DELETED = 2262;
	private static final int MSG_ID_CONCURRENT_UPDATE = 2263;
	private static final int MSG_ID_DELETE_FAILURE = 2264;
	private static final int MSG_ID_DUPLICATE_PROCESSOR = 2265;
	private static final int MSG_ID_PROCESSOR_HAS_CONFIG = 2266;
	private static final int MSG_ID_PROCESSOR_NAME_MISSING = 2267;
	
	// Same as in other services
	private static final int MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS = 2028;
	//private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_PROCESSOR_NOT_FOUND = "(E%d) No processor found for mission %s, processor name %s and processor version %s";
	private static final String MSG_PROCESSOR_MISSING = "(E%d) Processor not set";
	private static final String MSG_PROCESSOR_ID_MISSING = "(E%d) Processor ID not set";
	private static final String MSG_PROCESSOR_ID_NOT_FOUND = "(E%d) No processor found with ID %d";
	private static final String MSG_PROCESSOR_NAME_MISSING = "(E%d) Processor name not set";
	private static final String MSG_PROCESSOR_CLASS_INVALID = "(E%d) Processor class %s invalid for mission %s";
	private static final String MSG_PROCESSOR_DATA_MISSING = "(E%d) Processor data not set";
	private static final String MSG_DELETE_FAILURE = "(E%d) Processor deletion failed for ID %d (cause: %s)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Processor deletion unsuccessful for ID %d";
	private static final String MSG_CONCURRENT_UPDATE = "(E%d) The processor with ID %d has been modified since retrieval by the client";
	private static final String MSG_DUPLICATE_PROCESSOR = "(E%d) Duplicate processor for mission %s, processor name %s and processor version %s";
	private static final String MSG_PROCESSOR_HAS_CONFIG = "(E%d) Processor for mission %s with processor name %s and processor version %s cannot be deleted, because it has configured processors";

	private static final String MSG_PROCESSOR_LIST_RETRIEVED = "(I%d) Processor(s) for mission %s, processor name %s and processor version %s retrieved";
	private static final String MSG_PROCESSOR_RETRIEVED = "(I%d) Processor with ID %d retrieved";
	private static final String MSG_PROCESSOR_CREATED = "(I%d) Processor %s, version %s created for mission %s";
	private static final String MSG_PROCESSOR_MODIFIED = "(I%d) Processor with id %d modified";
	private static final String MSG_PROCESSOR_NOT_MODIFIED = "(I%d) Processor with id %d not modified (no changes)";
	private static final String MSG_PROCESSOR_DELETED = "(I%d) Processor with id %d deleted";

	// Same as in other services
	private static final String MSG_ILLEGAL_CROSS_MISSION_ACCESS = "(E%d) Illegal cross-mission access to mission %s (logged in to %s)";
	
	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorManager.class);

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
	 * Create a new processor (version)
	 * 
	 * @param processor a Json representation of the new processor
	 * @return a Json representation of the processor after creation (with ID and version number)
	 * @throws IllegalArgumentException if any of the input data was invalid
     * @throws SecurityException if a cross-mission data access was attempted
	 */
	public RestProcessor createProcessor(RestProcessor processor) throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> createProcessor({})", (null == processor ? "MISSING" : processor.getProcessorName()));

		if (null == processor) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_MISSING, MSG_ID_PROCESSOR_MISSING));
		}
		
		// Make sure processor class name is set
		if (null == processor.getProcessorName() || processor.getProcessorName().isBlank()) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_NAME_MISSING, MSG_ID_PROCESSOR_NAME_MISSING));
		}
		
		// Ensure user is authorized for the mission of the processor
		if (!securityService.isAuthorizedForMission(processor.getMissionCode())) {
			throw new SecurityException(logError(MSG_ILLEGAL_CROSS_MISSION_ACCESS, MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS,
					processor.getMissionCode(), securityService.getMission()));			
		}
		
		Processor modelProcessor = ProcessorUtil.toModelProcessor(processor);
		
		// Make sure a processor with the same processor class name and processor version does not yet exist
		if (null != RepositoryService.getProcessorRepository().findByMissionCodeAndProcessorNameAndProcessorVersion(
				processor.getMissionCode(), processor.getProcessorName(), processor.getProcessorVersion())) {
			throw new IllegalArgumentException(logError(MSG_DUPLICATE_PROCESSOR, MSG_ID_DUPLICATE_PROCESSOR,
					processor.getMissionCode(),
					processor.getProcessorName(),
					processor.getProcessorVersion()));
		}
		
		modelProcessor.setProcessorClass(RepositoryService.getProcessorClassRepository()
				.findByMissionCodeAndProcessorName(processor.getMissionCode(), processor.getProcessorName()));
		if (null == modelProcessor.getProcessorClass()) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_CLASS_INVALID, MSG_ID_PROCESSOR_CLASS_INVALID,
							processor.getProcessorName(), processor.getMissionCode()));
		}
		
		modelProcessor = RepositoryService.getProcessorRepository().save(modelProcessor);
		
		for (RestTask task: processor.getTasks()) {
			Task modelTask = TaskUtil.toModelTask(task);
			modelTask.setProcessor(modelProcessor);
			modelTask = RepositoryService.getTaskRepository().save(modelTask);
		}

		logInfo(MSG_PROCESSOR_CREATED, MSG_ID_PROCESSOR_CREATED, 
				modelProcessor.getProcessorClass().getProcessorName(),
				modelProcessor.getProcessorVersion(), 
				modelProcessor.getProcessorClass().getMission().getCode());
		
		return ProcessorUtil.toRestProcessor(modelProcessor);
	}

	/**
	 * Get processors by mission, name and version (user-defined version, not database version)
	 * 
	 * @param mission the mission code
	 * @param processorName the name of the processor (class)
	 * @param processorVersion the processor version
	 * @return a list of Json objects representing processors satisfying the search criteria
	 * @throws NoResultException if no processors matching the given search criteria could be found
     * @throws SecurityException if a cross-mission data access was attempted
	 */
	public List<RestProcessor> getProcessors(String mission, String processorName, String processorVersion)
			throws NoResultException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessors({}, {}, {})", mission, processorName, processorVersion);
		
		if (null == mission) {
			mission = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(mission)) {
				throw new SecurityException(logError(MSG_ILLEGAL_CROSS_MISSION_ACCESS, MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS,
						mission, securityService.getMission()));
			} 
		}
		
		List<RestProcessor> result = new ArrayList<>();
		
		String jpqlQuery = "select p from Processor p where processorClass.mission.code = :missionCode";
		if (null != processorName) {
			jpqlQuery += " and processorClass.processorName = :processorName";
		}
		if (null != processorVersion) {
			jpqlQuery += " and processorVersion = :processorVersion";
		}
		Query query = em.createQuery(jpqlQuery);
		query.setParameter("missionCode", mission);
		if (null != processorName) {
			query.setParameter("processorName", processorName);
		}
		if (null != processorVersion) {
			query.setParameter("processorVersion", processorVersion);
		}
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Processor) {
				result.add(ProcessorUtil.toRestProcessor((Processor) resultObject));
			}
		}
		if (result.isEmpty()) {
			throw new NoResultException(logError(MSG_PROCESSOR_NOT_FOUND, MSG_ID_PROCESSOR_NOT_FOUND,
					mission, processorName, processorVersion));
		}

		logInfo(MSG_PROCESSOR_LIST_RETRIEVED, MSG_ID_PROCESSOR_LIST_RETRIEVED, mission, processorName, processorVersion);
		
		return result;
	}

	/**
	 * Get a processor by ID
	 * 
	 * @param id the processor ID
	 * @return a Json object corresponding to the processor found
	 * @throws IllegalArgumentException if no processor ID was given
	 * @throws NoResultException if no processor with the given ID exists
     * @throws SecurityException if a cross-mission data access was attempted
	 */
	public RestProcessor getProcessorById(Long id) throws IllegalArgumentException, NoResultException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorById({})", id);
		
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_ID_MISSING, MSG_ID_PROCESSOR_ID_MISSING, id));
		}
		
		Optional<Processor> modelProcessor = RepositoryService.getProcessorRepository().findById(id);
		
		if (modelProcessor.isEmpty()) {
			throw new NoResultException(logError(MSG_PROCESSOR_ID_NOT_FOUND, MSG_ID_PROCESSOR_ID_NOT_FOUND, id));
		}

		// Ensure user is authorized for the mission of the processor
		if (!securityService.isAuthorizedForMission(modelProcessor.get().getProcessorClass().getMission().getCode())) {
			throw new SecurityException(logError(MSG_ILLEGAL_CROSS_MISSION_ACCESS, MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS,
					modelProcessor.get().getProcessorClass().getMission().getCode(), securityService.getMission()));			
		}
		
		logInfo(MSG_PROCESSOR_RETRIEVED, MSG_ID_PROCESSOR_RETRIEVED, id);
		
		return ProcessorUtil.toRestProcessor(modelProcessor.get());
	}

	/**
	 * Update a processor by ID
	 * 
	 * @param id the ID of the processor to update
	 * @param processor a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the processor after modification (with ID and version for all 
	 * 		   contained objects)
	 * @throws EntityNotFoundException if no processor with the given ID exists
	 * @throws IllegalArgumentException if any of the input data was invalid
     * @throws SecurityException if a cross-mission data access was attempted
	 * @throws ConcurrentModificationException if the processor has been modified since retrieval by the client
	 */
	public RestProcessor modifyProcessor(Long id, @Valid RestProcessor processor) throws
			EntityNotFoundException, IllegalArgumentException, SecurityException, ConcurrentModificationException {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProcessor({}, {})", id, (null == processor ? "MISSING" : processor.getProcessorName()));

		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_ID_MISSING, MSG_ID_PROCESSOR_ID_MISSING));
		}
		if (null == processor) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_DATA_MISSING, MSG_ID_PROCESSOR_DATA_MISSING));
		}
		
		// Ensure user is authorized for the mission of the processor
		if (!securityService.isAuthorizedForMission(processor.getMissionCode())) {
			throw new SecurityException(logError(MSG_ILLEGAL_CROSS_MISSION_ACCESS, MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS,
					processor.getMissionCode(), securityService.getMission()));			
		}
		
		Optional<Processor> optProcessor = RepositoryService.getProcessorRepository().findById(id);
		
		if (optProcessor.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_PROCESSOR_ID_NOT_FOUND, MSG_ID_PROCESSOR_ID_NOT_FOUND, id));
		}
		Processor modelProcessor = optProcessor.get();
		
		// Make sure we are allowed to change the processor (no intermediate update)
		if (modelProcessor.getVersion() != processor.getVersion().intValue()) {
			throw new ConcurrentModificationException(logError(MSG_CONCURRENT_UPDATE, MSG_ID_CONCURRENT_UPDATE, id));
		}
		
		// Apply changed attributes
		Processor changedProcessor = ProcessorUtil.toModelProcessor(processor);
		
		boolean processorChanged = false;
		
		if (!modelProcessor.getProcessorVersion().equals(changedProcessor.getProcessorVersion())) {
			processorChanged = true;
			modelProcessor.setProcessorVersion(changedProcessor.getProcessorVersion());
		}
		if (!modelProcessor.getIsTest().equals(changedProcessor.getIsTest())) {
			processorChanged = true;
			modelProcessor.setIsTest(changedProcessor.getIsTest());
		}
		if (!modelProcessor.getMinDiskSpace().equals(changedProcessor.getMinDiskSpace())) {
			processorChanged = true;
			modelProcessor.setMinDiskSpace(changedProcessor.getMinDiskSpace());
		}
		if (!modelProcessor.getMaxTime().equals(changedProcessor.getMaxTime())) {
			processorChanged = true;
			modelProcessor.setMaxTime(changedProcessor.getMaxTime());
		}
		if (!modelProcessor.getSensingTimeFlag().equals(changedProcessor.getSensingTimeFlag())) {
			processorChanged = true;
			modelProcessor.setSensingTimeFlag(changedProcessor.getSensingTimeFlag());
		}
		if (!modelProcessor.getDockerImage().equals(changedProcessor.getDockerImage())) {
			processorChanged = true;
			modelProcessor.setDockerImage(changedProcessor.getDockerImage());
		}
		if (null == modelProcessor.getDockerRunParameters() && null != changedProcessor.getDockerRunParameters()
				|| null != modelProcessor.getDockerRunParameters() && !modelProcessor.getDockerRunParameters().equals(changedProcessor.getDockerRunParameters())) {
			processorChanged = true;
			modelProcessor.setDockerRunParameters(changedProcessor.getDockerRunParameters());
		}
		
		// Check task changes (modifications in sequence also count as changes)
		List<Task> newTasks = new ArrayList<>();
		for (int i = 0; i < processor.getTasks().size(); ++i) {
			Task changedTask = TaskUtil.toModelTask(processor.getTasks().get(i));
			changedTask.setProcessor(changedProcessor);
			Task modelTask = null;
			if (modelProcessor.getTasks().size() <= i) {
				// More tasks in new list
				processorChanged = true;
				modelTask = RepositoryService.getTaskRepository().save(changedTask);
			} else {
				// Compare tasks
				modelTask = modelProcessor.getTasks().get(i);
				if (!modelTask.getTaskName().equals(changedTask.getTaskName())) {
					processorChanged = true;
					modelTask.setTaskName(changedTask.getTaskName());
				}
				if (!modelTask.getTaskVersion().equals(changedTask.getTaskVersion())) {
					processorChanged = true;
					modelTask.setTaskVersion(changedTask.getTaskVersion());
				}
				if (!modelTask.getIsCritical().equals(changedTask.getIsCritical())) {
					processorChanged = true;
					modelTask.setIsCritical(changedTask.getIsCritical());
				}
				if (!modelTask.getCriticalityLevel().equals(changedTask.getCriticalityLevel())) {
					processorChanged = true;
					modelTask.setCriticalityLevel(changedTask.getCriticalityLevel());
				}
				if (null == modelTask.getNumberOfCpus() && null != changedTask.getNumberOfCpus()
						|| null != modelTask.getNumberOfCpus() && !modelTask.getNumberOfCpus().equals(changedTask.getNumberOfCpus())) {
					processorChanged = true;
					modelTask.setNumberOfCpus(changedTask.getNumberOfCpus());
				}
				if (!modelTask.getBreakpointFileNames().equals(changedTask.getBreakpointFileNames())) {
					processorChanged = true;
					modelTask.setBreakpointFileNames(changedTask.getBreakpointFileNames());
				}
			}
			newTasks.add(modelTask);
		}
		
		// Save processor only if anything was actually changed
		if (processorChanged)	{
			modelProcessor.incrementVersion();
			modelProcessor.getTasks().clear();
			modelProcessor.getTasks().addAll(newTasks);
			modelProcessor = RepositoryService.getProcessorRepository().save(modelProcessor);
			logInfo(MSG_PROCESSOR_MODIFIED, MSG_ID_PROCESSOR_MODIFIED, id);
		} else {
			logInfo(MSG_PROCESSOR_NOT_MODIFIED, MSG_ID_PROCESSOR_NOT_MODIFIED, id);
		}
		
		return ProcessorUtil.toRestProcessor(modelProcessor);
	}

	/**
	 * Delete a processor by ID
	 * 
	 * @param id the ID of the processor to delete
	 * @throws EntityNotFoundException if the processor to delete does not exist in the database
	 * @throws RuntimeException if the deletion was not performed as expected
	 * @throws IllegalArgumentException if the ID of the processor to delete was not given, or if dependent objects exist
     * @throws SecurityException if a cross-mission data access was attempted
	 */
	public void deleteProcessorById(Long id)
			throws EntityNotFoundException, RuntimeException, IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProcessorById({})", id);
		
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_ID_MISSING, MSG_ID_PROCESSOR_ID_MISSING));
		}
		
		// Test whether the processor id is valid
		Optional<Processor> modelProcessor = RepositoryService.getProcessorRepository().findById(id);
		if (modelProcessor.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_PROCESSOR_ID_NOT_FOUND, MSG_ID_PROCESSOR_ID_NOT_FOUND, id));
		}
		
		// Ensure user is authorized for the mission of the processor
		if (!securityService.isAuthorizedForMission(modelProcessor.get().getProcessorClass().getMission().getCode())) {
			throw new SecurityException(logError(MSG_ILLEGAL_CROSS_MISSION_ACCESS, MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS,
					modelProcessor.get().getProcessorClass().getMission().getCode(), securityService.getMission()));			
		}
		
		// Check whether there are configured processors for this processor
		if (!modelProcessor.get().getConfiguredProcessors().isEmpty()) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_HAS_CONFIG, MSG_ID_PROCESSOR_HAS_CONFIG,
					modelProcessor.get().getProcessorClass().getMission().getCode(),
					modelProcessor.get().getProcessorClass().getProcessorName(),
					modelProcessor.get().getProcessorVersion()));
		}
		
		// Remove the processor from the processor class
		modelProcessor.get().getProcessorClass().getProcessors().remove(modelProcessor.get());
		
		// Delete the processor
		try {
			RepositoryService.getProcessorRepository().deleteById(id);
		} catch (Exception e) {
			throw new RuntimeException(logError(MSG_DELETE_FAILURE, MSG_ID_DELETE_FAILURE, id, e.getMessage()));
		}

		// Test whether the deletion was successful
		modelProcessor = RepositoryService.getProcessorRepository().findById(id);
		if (!modelProcessor.isEmpty()) {
			throw new RuntimeException(logError(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, id));
		}
		
		logInfo(MSG_PROCESSOR_DELETED, MSG_ID_PROCESSOR_DELETED, id);
	}

}
