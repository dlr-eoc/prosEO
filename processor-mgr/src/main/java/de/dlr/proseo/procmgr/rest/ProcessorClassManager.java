/**
 * ProcessorClassManager.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProcessorMgrMessage;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.procmgr.ProcessorManagerConfiguration;
import de.dlr.proseo.procmgr.rest.model.ProcessorClassUtil;
import de.dlr.proseo.procmgr.rest.model.RestProcessorClass;

/**
 * Service methods required to manage processor classes.
 *
 * @author Dr. Thomas Bassler
 *
 */
@Component
@Transactional
public class ProcessorClassManager {

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
	private static ProseoLogger logger = new ProseoLogger(ProcessorClassManager.class);

	/**
	 * Get processor classes by mission and name
	 *
	 * @param mission       the mission code (optional)
	 * @param processorName the processor name (optional)
	 * @param recordFrom          first record of filtered and ordered result to return
	 * @param recordTo            last record of filtered and ordered result to return
	 * @return a list of Json objects representing processor classes satisfying the search criteria
	 * @throws NoResultException if no processor classes matching the given search criteria could be found
	 * @throws SecurityException if a cross-mission data access was attempted
	 */
	public List<RestProcessorClass> getProcessorClasses(String mission, String processorName, Integer recordFrom, Integer recordTo)
			throws NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessorClasses({}, {})", mission, processorName);

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

		Long numberOfResults = Long.parseLong(this.countProcessorClasses(mission, processorName));
		Integer maxResults = config.getMaxResults();
		if (numberOfResults > maxResults && (recordTo - recordFrom) > maxResults && (numberOfResults - recordFrom) > maxResults) {
			throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS,
					logger.log(GeneralMessage.TOO_MANY_RESULTS, "workflows", numberOfResults, config.getMaxResults()));
		}

		List<RestProcessorClass> result = new ArrayList<>();

		String jpqlQuery = "select pc from ProcessorClass pc where mission.code = :missionCode";
		if (null != processorName) {
			jpqlQuery += " and processorName = :processorName";
		}
		Query query = em.createQuery(jpqlQuery);
		query.setParameter("missionCode", mission);
		if (null != processorName) {
			query.setParameter("processorName", processorName);
		}
		query.setFirstResult(recordFrom);
		query.setMaxResults(recordTo - recordFrom);

		for (Object resultObject : query.getResultList()) {
			if (resultObject instanceof de.dlr.proseo.model.ProcessorClass) {
				result.add(ProcessorClassUtil.toRestProcessorClass((de.dlr.proseo.model.ProcessorClass) resultObject));
			}
		}
		if (result.isEmpty()) {
			throw new NoResultException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_NOT_FOUND, mission, processorName));
		}

		logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_LIST_RETRIEVED, mission, processorName);

		return result;
	}

	/**
	 * Create a new processor class
	 *
	 * @param processorClass a Json representation of the new processor class
	 * @return a Json representation of the processor class after creation (with ID and version number)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public RestProcessorClass createProcessorClass(@Valid RestProcessorClass processorClass)
			throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createProcessorClass({})", (null == processorClass ? "MISSING" : processorClass.getProcessorName()));

		if (null == processorClass) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_MISSING));
		}

		// Ensure user is authorized for the mission of the processor class
		if (!securityService.isAuthorizedForMission(processorClass.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, processorClass.getMissionCode(),
					securityService.getMission()));
		}

		// Ensure mandatory attributes are set
		if (null == processorClass.getProcessorName() || processorClass.getProcessorName().isBlank()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "processorName", "processorClass creation"));
		}

		// If list attributes were set to null explicitly, initialize with empty lists
		if (null == processorClass.getProductClasses()) {
			processorClass.setProductClasses(new ArrayList<>());
		}

		ProcessorClass modelProcessorClass = ProcessorClassUtil.toModelProcessorClass(processorClass);

		// Make sure a processor class with the same name does not yet exist for the mission
		if (null != RepositoryService.getProcessorClassRepository()
			.findByMissionCodeAndProcessorName(processorClass.getMissionCode(), processorClass.getProcessorName())) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.DUPLICATE_PROCESSOR_CLASS,
					processorClass.getMissionCode(), processorClass.getProcessorName()));
		}

		modelProcessorClass = RepositoryService.getProcessorClassRepository().save(modelProcessorClass);
		if (logger.isTraceEnabled())
			logger.trace("... creating processor class with database ID = " + modelProcessorClass.getId());

		modelProcessorClass.setMission(RepositoryService.getMissionRepository().findByCode(processorClass.getMissionCode()));
		if (null == modelProcessorClass.getMission()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.MISSION_CODE_INVALID, processorClass.getMissionCode()));
		}

		for (String productType : processorClass.getProductClasses()) {
			ProductClass productClass = RepositoryService.getProductClassRepository()
				.findByMissionCodeAndProductType(processorClass.getMissionCode(), productType);
			if (null == productClass) {
				throw new IllegalArgumentException(
						logger.log(ProcessorMgrMessage.PRODUCT_CLASS_INVALID, productType, processorClass.getMissionCode()));
			}
			productClass.setProcessorClass(modelProcessorClass);
			modelProcessorClass.getProductClasses().add(productClass);
		}

		logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_CREATED, modelProcessorClass.getProcessorName(),
				modelProcessorClass.getMission().getCode());

		return ProcessorClassUtil.toRestProcessorClass(modelProcessorClass);
	}

	/**
	 * Get a processor class by ID
	 *
	 * @param id the processor class ID
	 * @return a Json object corresponding to the processor class found
	 * @throws IllegalArgumentException if no processor class ID was given
	 * @throws NoResultException        if no processor class with the given ID exists
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public RestProcessorClass getProcessorClassById(Long id) throws IllegalArgumentException, NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessorClassById({})", id);

		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_ID_MISSING));
		}

		Optional<ProcessorClass> modelProcessorClass = RepositoryService.getProcessorClassRepository().findById(id);

		if (modelProcessorClass.isEmpty()) {
			throw new NoResultException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_ID_NOT_FOUND, id));
		}

		// Ensure user is authorized for the mission of the processor class
		if (!securityService.isAuthorizedForMission(modelProcessorClass.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProcessorClass.get().getMission().getCode(), securityService.getMission()));
		}

		logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_RETRIEVED, id);

		return ProcessorClassUtil.toRestProcessorClass(modelProcessorClass.get());
	}

	/**
	 * Update a processor class by ID
	 *
	 * @param id             the ID of the processor class to update
	 * @param processorClass a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the processor class after modification (with ID and version for
	 *         all contained objects)
	 * @throws EntityNotFoundException         if no processor class with the given ID exists
	 * @throws IllegalArgumentException        if any of the input data was invalid
	 * @throws SecurityException               if a cross-mission data access was attempted
	 * @throws ConcurrentModificationException if the processor class has been modified since retrieval by the client
	 */
	public RestProcessorClass modifyProcessorClass(Long id, @Valid RestProcessorClass processorClass)
			throws EntityNotFoundException, IllegalArgumentException, SecurityException, ConcurrentModificationException {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyProcessorClass({}, {})", id,
					(null == processorClass ? "MISSING" : processorClass.getProcessorName()));

		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_ID_MISSING));
		}
		if (null == processorClass) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_DATA_MISSING));
		}

		// Ensure user is authorized for the mission of the processor class
		if (!securityService.isAuthorizedForMission(processorClass.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, processorClass.getMissionCode(),
					securityService.getMission()));
		}

		Optional<ProcessorClass> optProcessorClass = RepositoryService.getProcessorClassRepository().findById(id);

		if (optProcessorClass.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_ID_NOT_FOUND, id));
		}
		ProcessorClass modelProcessorClass = optProcessorClass.get();

		// Make sure we are allowed to change the processor class (no intermediate update)
		if (modelProcessorClass.getVersion() != processorClass.getVersion().intValue()) {
			throw new ConcurrentModificationException(logger.log(ProcessorMgrMessage.CONCURRENT_UPDATE, id));
		}

		// Ensure mandatory attributes are set
		if (null == processorClass.getProcessorName() || processorClass.getProcessorName().isBlank()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "processorName", "processorClass modification"));
		}

		// If list attributes were set to null explicitly, initialize with empty lists
		if (null == processorClass.getProductClasses()) {
			processorClass.setProductClasses(new ArrayList<>());
		}

		// Apply changed attributes
		ProcessorClass changedProcessorClass = ProcessorClassUtil.toModelProcessorClass(processorClass);

		boolean processorClassChanged = false;
		if (!modelProcessorClass.getProcessorName().equals(changedProcessorClass.getProcessorName())) {
			processorClassChanged = true;
			modelProcessorClass.setProcessorName(changedProcessorClass.getProcessorName());
		}

		// Check changes in associated product classes
		Set<ProductClass> newProductClasses = new HashSet<>();
		for (String productType : processorClass.getProductClasses()) {
			ProductClass productClass = RepositoryService.getProductClassRepository()
				.findByMissionCodeAndProductType(processorClass.getMissionCode(), productType);
			if (null == productClass) {
				throw new IllegalArgumentException(
						logger.log(ProcessorMgrMessage.PRODUCT_CLASS_INVALID, productType, processorClass.getMissionCode()));
			}
			productClass.setProcessorClass(modelProcessorClass);
			productClass = RepositoryService.getProductClassRepository().save(productClass);
			newProductClasses.add(productClass);
			if (!modelProcessorClass.getProductClasses().contains(productClass)) {
				processorClassChanged = true;
			}
		}
		// Check removed product classes
		for (ProductClass productClass : modelProcessorClass.getProductClasses()) {
			if (!newProductClasses.contains(productClass)) {
				processorClassChanged = true;
				productClass.setProcessorClass(null);
				RepositoryService.getProductClassRepository().save(productClass);
			}
		}

		// Save processor class only if anything was actually changed
		if (processorClassChanged) {
			modelProcessorClass.incrementVersion();
			modelProcessorClass.getProductClasses().clear();
			modelProcessorClass.getProductClasses().addAll(newProductClasses);
			modelProcessorClass = RepositoryService.getProcessorClassRepository().save(modelProcessorClass);
			logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_MODIFIED, id);
		} else {
			logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_NOT_MODIFIED, id);
		}

		return ProcessorClassUtil.toRestProcessorClass(modelProcessorClass);
	}

	/**
	 * Delete a processor class by ID
	 *
	 * @param id the ID of the processor class to delete
	 * @throws EntityNotFoundException  if the processor class to delete does not exist in the database
	 * @throws RuntimeException         if the deletion was not performed as expected
	 * @throws IllegalArgumentException if the ID of the processor class to delete was not given, or if dependent objects exist
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public void deleteProcessorClassById(Long id)
			throws EntityNotFoundException, RuntimeException, IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteProcessorClassById({})", id);

		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_ID_MISSING));
		}

		// Test whether the product id is valid
		Optional<ProcessorClass> modelProcessorClass = RepositoryService.getProcessorClassRepository().findById(id);
		if (modelProcessorClass.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_ID_NOT_FOUND, id));
		}

		// Ensure user is authorized for the mission of the processor class
		if (!securityService.isAuthorizedForMission(modelProcessorClass.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProcessorClass.get().getMission().getCode(), securityService.getMission()));
		}

		// Check whether there are processors or configurations for this processor class
		if (!modelProcessorClass.get().getProcessors().isEmpty()) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_HAS_PROC,
					modelProcessorClass.get().getMission().getCode(), modelProcessorClass.get().getProcessorName()));
		}

		String jpqlQuery = "select c from Configuration c where processorClass.mission.code = :missionCode"
				+ " and processorClass.processorName = :processorName";
		Query query = em.createQuery(jpqlQuery);
		query.setParameter("missionCode", modelProcessorClass.get().getMission().getCode());
		query.setParameter("processorName", modelProcessorClass.get().getProcessorName());

		if (!query.getResultList().isEmpty()) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_HAS_CONF,
					modelProcessorClass.get().getMission().getCode(), modelProcessorClass.get().getProcessorName()));
		}

		// Remove processor class from product classes
		for (ProductClass productClass : modelProcessorClass.get().getProductClasses()) {
			productClass.setProcessorClass(null);
			RepositoryService.getProductClassRepository().save(productClass);
		}

		// Delete the processor class
		RepositoryService.getProcessorClassRepository().deleteById(id);

		// Test whether the deletion was successful
		modelProcessorClass = RepositoryService.getProcessorClassRepository().findById(id);
		if (!modelProcessorClass.isEmpty()) {
			throw new RuntimeException(logger.log(ProcessorMgrMessage.DELETION_UNSUCCESSFUL, id));
		}

		logger.log(ProcessorMgrMessage.PROCESSOR_CLASS_DELETED, id);
	}

	/**
	 * Count the processor classes matching the specified mission or processorName.
	 *
	 * @param missionCode   the mission code
	 * @param processorName the processor name
	 * @return the number of processor classes found as string
	 * @throws SecurityException if a cross-mission data access was attempted
	 */
	public String countProcessorClasses(String missionCode, String processorName) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countProcessorClasses({}, {})", missionCode, processorName);

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
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = cb.createQuery(Long.class);
		Root<ProcessorClass> rootProcessorClass = query.from(ProcessorClass.class);

		List<Predicate> predicates = new ArrayList<>();

		predicates.add(cb.equal(rootProcessorClass.get("mission").get("code"), missionCode));
		if (processorName != null)
			predicates.add(cb.equal(rootProcessorClass.get("processorName"), processorName));
		query.select(cb.count(rootProcessorClass)).where(predicates.toArray(new Predicate[predicates.size()]));

		Long result = em.createQuery(query).getSingleResult();

		logger.log(ProcessorMgrMessage.CONFIGURATIONS_COUNTED, result, missionCode, processorName);

		return result.toString();
	}
}