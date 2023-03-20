/**
 * ProcessorManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProcessorMgrMessage;
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
	
	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProcessorManager.class);
	
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
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_MISSING));
		}
		
		// Ensure user is authorized for the mission of the processor
		if (!securityService.isAuthorizedForMission(processor.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					processor.getMissionCode(), securityService.getMission()));			
		}
		
		// Ensure mandatory attributes are set
		if (null == processor.getProcessorName() || processor.getProcessorName().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "processorName", "processor creation"));
		}
		if (null == processor.getProcessorVersion() || processor.getProcessorVersion().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "processorVersion", "processor creation"));
		}
		if (null == processor.getTasks() || processor.getTasks().isEmpty()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "tasks", "processor creation"));
		}
		if (null == processor.getDockerImage() || processor.getDockerImage().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "dockerImage", "processor creation"));
		}
		
		// If list attributes were set to null explicitly, initialize with empty lists
		if (null == processor.getConfiguredProcessors()) {
			processor.setConfiguredProcessors(new ArrayList<>());
		}
		if (null == processor.getDockerRunParameters()) {
			processor.setDockerRunParameters(new ArrayList<>());
		}

		Processor modelProcessor = ProcessorUtil.toModelProcessor(processor);
		
		// Make sure a processor with the same processor class name and processor version does not yet exist
		if (null != RepositoryService.getProcessorRepository().findByMissionCodeAndProcessorNameAndProcessorVersion(
				processor.getMissionCode(), processor.getProcessorName(), processor.getProcessorVersion())) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.DUPLICATE_PROCESSOR,
					processor.getMissionCode(),
					processor.getProcessorName(),
					processor.getProcessorVersion()));
		}
		
		modelProcessor.setProcessorClass(RepositoryService.getProcessorClassRepository()
				.findByMissionCodeAndProcessorName(processor.getMissionCode(), processor.getProcessorName()));
		if (null == modelProcessor.getProcessorClass()) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_INVALID,
							processor.getProcessorName(), processor.getMissionCode()));
		}
		
		modelProcessor = RepositoryService.getProcessorRepository().save(modelProcessor);
		
		for (RestTask task: processor.getTasks()) {
			Task modelTask = TaskUtil.toModelTask(task);
			if (modelTask.getIsCritical() && null == modelTask.getCriticalityLevel()) {
				throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CRITICALITY_LEVEL_MISSING, modelTask.getTaskName()));
			}
			modelTask.setProcessor(modelProcessor);
			modelTask = RepositoryService.getTaskRepository().save(modelTask);
		}

		logger.log(ProcessorMgrMessage.PROCESSOR_CREATED, 
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
	public List<RestProcessor> getProcessors(String mission, String processorName, String processorVersion,
			Integer recordFrom, Integer recordTo)
			throws NoResultException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessors({}, {}, {})", mission, processorName, processorVersion);
		
		if (null == mission) {
			mission = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(mission)) {
				throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
						mission, securityService.getMission()));
			} 
		}
				
		if (recordFrom == null) {
			recordFrom = 0;
		}
		if (recordTo == null) {
			recordTo = Integer.MAX_VALUE;
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
		query.setFirstResult(recordFrom);
		query.setMaxResults(recordTo - recordFrom);

		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Processor) {
				result.add(ProcessorUtil.toRestProcessor((Processor) resultObject));
			}
		}
		if (result.isEmpty()) {
			throw new NoResultException(logger.log(ProcessorMgrMessage.PROCESSOR_NOT_FOUND,
					mission, processorName, processorVersion));
		}

		logger.log(ProcessorMgrMessage.PROCESSOR_LIST_RETRIEVED, mission, processorName, processorVersion);
		
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
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_ID_MISSING, id));
		}
		
		Optional<Processor> modelProcessor = RepositoryService.getProcessorRepository().findById(id);
		
		if (modelProcessor.isEmpty()) {
			throw new NoResultException(logger.log(ProcessorMgrMessage.PROCESSOR_ID_NOT_FOUND, id));
		}

		// Ensure user is authorized for the mission of the processor
		if (!securityService.isAuthorizedForMission(modelProcessor.get().getProcessorClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProcessor.get().getProcessorClass().getMission().getCode(), securityService.getMission()));			
		}
		
		logger.log(ProcessorMgrMessage.PROCESSOR_RETRIEVED, id);
		
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
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_ID_MISSING));
		}
		if (null == processor) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_DATA_MISSING));
		}
		
		// Ensure user is authorized for the mission of the processor
		if (!securityService.isAuthorizedForMission(processor.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					processor.getMissionCode(), securityService.getMission()));			
		}
		
		// Ensure mandatory attributes are set
		if (null == processor.getProcessorName() || processor.getProcessorName().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "processorName", "processor modification"));
		}
		if (null == processor.getProcessorVersion() || processor.getProcessorVersion().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "processorVersion", "processor modification"));
		}
		if (null == processor.getTasks() || processor.getTasks().isEmpty()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "tasks", "processor modification"));
		}
		if (null == processor.getDockerImage() || processor.getDockerImage().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "dockerImage", "processor modification"));
		}
		
		// If list attributes were set to null explicitly, initialize with empty lists
		if (null == processor.getConfiguredProcessors()) {
			processor.setConfiguredProcessors(new ArrayList<>());
		}
		if (null == processor.getDockerRunParameters()) {
			processor.setDockerRunParameters(new ArrayList<>());
		}
		
		Optional<Processor> optProcessor = RepositoryService.getProcessorRepository().findById(id);
		
		if (optProcessor.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProcessorMgrMessage.PROCESSOR_ID_NOT_FOUND, id));
		}
		Processor modelProcessor = optProcessor.get();
		
		// Make sure we are allowed to change the processor (no intermediate update)
		if (modelProcessor.getVersion() != processor.getVersion().intValue()) {
			throw new ConcurrentModificationException(logger.log(ProcessorMgrMessage.CONCURRENT_UPDATE, id));
		}
		
		// Apply changed attributes
		Processor changedProcessor = ProcessorUtil.toModelProcessor(processor);
		
		boolean processorChanged = false;
		
		if (!modelProcessor.getProcessorVersion().equals(changedProcessor.getProcessorVersion())) {
			processorChanged = true;
			modelProcessor.setProcessorVersion(changedProcessor.getProcessorVersion());
		}
		if (!modelProcessor.getJobOrderVersion().equals(changedProcessor.getJobOrderVersion())) {
			processorChanged = true;
			modelProcessor.setJobOrderVersion(changedProcessor.getJobOrderVersion());
		}
		if (!modelProcessor.getUseInputFileTimeIntervals().equals(changedProcessor.getUseInputFileTimeIntervals())) {
			processorChanged = true;
			modelProcessor.setUseInputFileTimeIntervals(changedProcessor.getUseInputFileTimeIntervals());
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
		if (!Objects.equals(modelProcessor.getDockerRunParameters(), changedProcessor.getDockerRunParameters())) {
			processorChanged = true;
			modelProcessor.setDockerRunParameters(changedProcessor.getDockerRunParameters());
		}
		if (logger.isTraceEnabled()) logger.trace("... scalar attributes for processor have changed: " + processorChanged);
		
		// Check task changes (modifications in sequence also count as changes)
		List<Task> newTasks = new ArrayList<>();
		for (int i = 0; i < processor.getTasks().size(); ++i) {
			Task changedTask = TaskUtil.toModelTask(processor.getTasks().get(i));
			changedTask.setProcessor(changedProcessor);
			Task modelTask = null;
			if (modelProcessor.getTasks().size() <= i) {
				// More tasks in new list
				if (logger.isTraceEnabled()) logger.trace("... new task added");
				processorChanged = true;
				modelTask = RepositoryService.getTaskRepository().save(changedTask);
			} else {
				// Compare tasks
				modelTask = modelProcessor.getTasks().get(i);
				if (!modelTask.getTaskName().equals(changedTask.getTaskName())) {
					if (logger.isTraceEnabled()) logger.trace(String.format("... task name changed from %s to %s", modelTask.getTaskName(), changedTask.getTaskName()));
					processorChanged = true;
					modelTask.setTaskName(changedTask.getTaskName());
				}
				if (!modelTask.getTaskVersion().equals(changedTask.getTaskVersion())) {
					if (logger.isTraceEnabled()) logger.trace(String.format("... task version changed from %s to %s", modelTask.getTaskVersion(), changedTask.getTaskVersion()));
					processorChanged = true;
					modelTask.setTaskVersion(changedTask.getTaskVersion());
				}
				
				if (!modelTask.getIsCritical().equals(changedTask.getIsCritical())) {
					if (logger.isTraceEnabled()) logger.trace(String.format("... task criticality changed from %s to %s", modelTask.getIsCritical().toString(), changedTask.getIsCritical().toString()));
					processorChanged = true;
					modelTask.setIsCritical(changedTask.getIsCritical());
				}
				if (!Objects.equals(modelTask.getCriticalityLevel(), changedTask.getCriticalityLevel())) {
					if (logger.isTraceEnabled()) logger.trace(String.format("... task criticality level changed from %d to %d", modelTask.getCriticalityLevel(), changedTask.getCriticalityLevel()));
					processorChanged = true;
					modelTask.setCriticalityLevel(changedTask.getCriticalityLevel());
				}
				if (modelTask.getIsCritical() && null == modelTask.getCriticalityLevel()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.CRITICALITY_LEVEL_MISSING, modelTask.getTaskName()));
				}
				
				if (!Objects.equals(modelTask.getNumberOfCpus(), changedTask.getNumberOfCpus())) {
					if (logger.isTraceEnabled()) logger.trace(String.format("... task number of cpus changed from %s to %s", String.valueOf(modelTask.getNumberOfCpus()), String.valueOf(changedTask.getNumberOfCpus())));
					processorChanged = true;
					modelTask.setNumberOfCpus(changedTask.getNumberOfCpus());
				}
				if (!Objects.equals(modelTask.getMinMemory(), changedTask.getMinMemory())) {
					if (logger.isTraceEnabled()) logger.trace(String.format("... task minimum memory changed from %s to %s", String.valueOf(modelTask.getMinMemory()), String.valueOf(changedTask.getMinMemory())));
					processorChanged = true;
					modelTask.setMinMemory(changedTask.getMinMemory());
				}
				if (!modelTask.getBreakpointFileNames().equals(changedTask.getBreakpointFileNames())) {
					if (modelTask.getBreakpointFileNames().isEmpty() && changedTask.getBreakpointFileNames().isEmpty()) {
						if (logger.isTraceEnabled()) logger.trace("... task breakpoint files 'not equal', although both are empty");
					} else {
						if (logger.isTraceEnabled()) logger.trace(String.format("... task breakpoint files changed from %s to %s", modelTask.getBreakpointFileNames().toString(), changedTask.getBreakpointFileNames().toString()));
						processorChanged = true;
						modelTask.setBreakpointFileNames(changedTask.getBreakpointFileNames());
					}
				}
				if (logger.isTraceEnabled()) logger.trace("... processor has changed after task " + i + ": " + processorChanged);
			}
			newTasks.add(modelTask);
		}
		
		// Save processor only if anything was actually changed
		if (processorChanged)	{
			modelProcessor.incrementVersion();
			modelProcessor.getTasks().clear();
			modelProcessor.getTasks().addAll(newTasks);
			modelProcessor = RepositoryService.getProcessorRepository().save(modelProcessor);
			logger.log(ProcessorMgrMessage.PROCESSOR_MODIFIED, id);
		} else {
			logger.log(ProcessorMgrMessage.PROCESSOR_NOT_MODIFIED, id);
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
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_ID_MISSING));
		}
		
		// Test whether the processor id is valid
		Optional<Processor> modelProcessor = RepositoryService.getProcessorRepository().findById(id);
		if (modelProcessor.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProcessorMgrMessage.PROCESSOR_ID_NOT_FOUND, id));
		}
		
		// Ensure user is authorized for the mission of the processor
		if (!securityService.isAuthorizedForMission(modelProcessor.get().getProcessorClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProcessor.get().getProcessorClass().getMission().getCode(), securityService.getMission()));			
		}
		
		// Check whether there are configured processors for this processor
		if (!modelProcessor.get().getConfiguredProcessors().isEmpty()) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_HAS_CONFIG,
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
			throw new RuntimeException(logger.log(ProcessorMgrMessage.DELETE_FAILURE, id, e.getMessage()));
		}

		// Test whether the deletion was successful
		modelProcessor = RepositoryService.getProcessorRepository().findById(id);
		if (!modelProcessor.isEmpty()) {
			throw new RuntimeException(logger.log(ProcessorMgrMessage.DELETION_UNSUCCESSFUL, id));
		}
		
		logger.log(ProcessorMgrMessage.PROCESSOR_DELETED, id);
	}

	/**
	 * Count the processors matching the specified mission, processorName, or
	 * processorVersion
	 * 
	 * @param missionCode      the mission code
	 * @param processorName    the processor name
	 * @param processorVersion the processor version
	 * @return the number of processors found as string
	 * @throws SecurityException if a cross-mission data access was attempted
	 */
	public String countProcessors(String missionCode, String processorName, String processorVersion) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countProcessors({}, {}, {})", missionCode, processorName, processorVersion);

		if (null == missionCode) {
			missionCode = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(missionCode)) {
				throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, missionCode,
						securityService.getMission()));
			}
		}

		// build query
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = cb.createQuery(Long.class);
		Root<Processor> rootProcessor = query.from(Processor.class);

		List<Predicate> predicates = new ArrayList<>();

		predicates.add(cb.equal(rootProcessor.get("processorClass").get("mission").get("code"), missionCode));
		if (processorName != null)
			predicates.add(cb.equal(rootProcessor.get("processorClass").get("processorName"), processorName));
		if (processorVersion != null)
			predicates.add(cb.equal(rootProcessor.get("processorVersion"), processorVersion));
		query.select(cb.count(rootProcessor)).where(predicates.toArray(new Predicate[predicates.size()]));

		Long result = em.createQuery(query).getSingleResult();

		logger.log(ProcessorMgrMessage.PROCESSORS_COUNTED, result, missionCode, processorName, processorVersion);

		return result.toString();
	}

}
