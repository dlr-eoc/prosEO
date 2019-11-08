/**
 * ProcessorClassManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
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
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
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
	
	/* Message ID constants */
	private static final int MSG_ID_PROCESSOR_CLASS_NOT_FOUND = 2200;
	private static final int MSG_ID_PROCESSOR_CLASS_LIST_RETRIEVED = 2201;
	private static final int MSG_ID_PROCESSOR_CLASS_RETRIEVED = 2202;
	private static final int MSG_ID_PROCESSOR_CLASS_MISSING = 2203;
	private static final int MSG_ID_MISSION_CODE_INVALID = 2204;
	private static final int MSG_ID_PRODUCT_CLASS_INVALID = 2205;
	private static final int MSG_ID_PROCESSOR_CLASS_CREATED = 2208;
	private static final int MSG_ID_PROCESSOR_CLASS_ID_MISSING = 2209;
	private static final int MSG_ID_PROCESSOR_CLASS_ID_NOT_FOUND = 2200;
	private static final int MSG_ID_PROCESSOR_CLASS_DATA_MISSING = 2210;
	private static final int MSG_ID_PROCESSOR_CLASS_MODIFIED = 2211;
	private static final int MSG_ID_PROCESSOR_CLASS_NOT_MODIFIED = 2212;
	private static final int MSG_ID_PROCESSOR_CLASS_DELETED = 2213;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 2213;
//	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_PROCESSOR_CLASS_NOT_FOUND = "(E%d) No processor class found for mission %s and processor name %s";
	private static final String MSG_PROCESSOR_CLASS_MISSING = "(E%d) Processor class not set";
	private static final String MSG_PROCESSOR_CLASS_ID_MISSING = "(E%d) Processor class ID not set";
	private static final String MSG_PROCESSOR_CLASS_ID_NOT_FOUND = "(E%d) No processor class found with ID %d";
	private static final String MSG_MISSION_CODE_INVALID = "(E%d) Mission code %s invalid";
	private static final String MSG_PRODUCT_CLASS_INVALID = "(E%d) Product type %s invalid for mission %s";
	private static final String MSG_PROCESSOR_CLASS_DATA_MISSING = "(E%d) Processor class data not set";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Processor class deletion unsuccessful for ID %d";

	private static final String MSG_PROCESSOR_CLASS_LIST_RETRIEVED = "(I%d) Processor class(es) for mission %s and processor name %s retrieved";
	private static final String MSG_PROCESSOR_CLASS_CREATED = "(I%d) Processor class %s created for mission %s";
	private static final String MSG_PROCESSOR_CLASS_RETRIEVED = "(I%d) Processor class with ID %d retrieved";
	private static final String MSG_PROCESSOR_CLASS_MODIFIED = "(I%d) Processor class with id %d modified";
	private static final String MSG_PROCESSOR_CLASS_NOT_MODIFIED = "(I%d) Processor class with id %d not modified (no changes)";
	private static final String MSG_PROCESSOR_CLASS_DELETED = "(I%d) Processor class with id %d deleted";

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorClassManager.class);

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
	 * Get processor classes by mission and name
	 * 
	 * @param mission the mission code (optional)
	 * @param processorName the processor name (optional)
	 * @return a list of Json objects representing processor classes satisfying the search criteria
	 * @throws NoResultException if no processor classes matching the given search criteria could be found
	 */
	public List<RestProcessorClass> getProcessorClasses(String mission, String processorName) throws NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClasses({}, {})", mission, processorName);
		
		List<RestProcessorClass> result = new ArrayList<>();
		
		if (null != mission && null != processorName) {
			ProcessorClass processorClass = RepositoryService.getProcessorClassRepository().findByMissionCodeAndProcessorName(mission,
					processorName);
			if (null == processorClass) {
				throw new NoResultException(logError(MSG_PROCESSOR_CLASS_NOT_FOUND, MSG_ID_PROCESSOR_CLASS_NOT_FOUND, 
						mission, processorName));
			}
			result.add(ProcessorClassUtil.toRestProcessorClass(processorClass));
		} else {
			String jpqlQuery = "select pc from ProcessorClass pc where 1 = 1";
			if (null != mission) {
				jpqlQuery += " and mission.code = :missionCode";
			}
			if (null != processorName) {
				jpqlQuery += " and processorName = :processorName";
			}
			Query query = em.createQuery(jpqlQuery);
			if (null != mission) {
				query.setParameter("missionCode", mission);
			}
			if (null != processorName) {
				query.setParameter("processorName", processorName);
			}
			for (Object resultObject: query.getResultList()) {
				if (resultObject instanceof de.dlr.proseo.model.ProcessorClass) {
					result.add(ProcessorClassUtil.toRestProcessorClass((de.dlr.proseo.model.ProcessorClass) resultObject));
				}
			}
			if (result.isEmpty()) {
				throw new NoResultException(logError(MSG_PROCESSOR_CLASS_NOT_FOUND, MSG_ID_PROCESSOR_CLASS_NOT_FOUND, 
						mission, processorName));
			}
		}
		logInfo(MSG_PROCESSOR_CLASS_LIST_RETRIEVED, MSG_ID_PROCESSOR_CLASS_LIST_RETRIEVED, mission, processorName);
		
		return result;
	}

	/**
	 * Create a new processor class
	 * 
	 * @param processorClass a Json representation of the new processor class
	 * @return a Json representation of the processor class after creation (with ID and version number)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 */
	public RestProcessorClass createProcessorClass(@Valid RestProcessorClass processorClass) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> createProcessorClass({})", (null == processorClass ? "MISSING" : processorClass.getProcessorName()));
		
		if (null == processorClass) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_CLASS_MISSING, MSG_ID_PROCESSOR_CLASS_MISSING));
		}
		
		ProcessorClass modelProcessorClass = ProcessorClassUtil.toModelProcessorClass(processorClass);
		
		modelProcessorClass.setMission(RepositoryService.getMissionRepository().findByCode(processorClass.getMissionCode()));
		if (null == modelProcessorClass.getMission()) {
			throw new IllegalArgumentException(logError(MSG_MISSION_CODE_INVALID, MSG_ID_MISSION_CODE_INVALID,
					processorClass.getMissionCode()));
		}
		
		for (String productType: processorClass.getProductClasses()) {
			ProductClass productClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(processorClass.getMissionCode(), productType);
			if (null == productClass) {
				throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_INVALID, MSG_ID_PRODUCT_CLASS_INVALID,
								productType, processorClass.getMissionCode()));
			}
		}
		
		modelProcessorClass = RepositoryService.getProcessorClassRepository().save(modelProcessorClass);
		
		logInfo(MSG_PROCESSOR_CLASS_CREATED, MSG_ID_PROCESSOR_CLASS_CREATED, 
				modelProcessorClass.getProcessorName(), modelProcessorClass.getMission().getCode());
		
		return ProcessorClassUtil.toRestProcessorClass(modelProcessorClass);
	}

	/**
	 * Get a processor class by ID
	 * 
	 * @param id the processor class ID
	 * @return a Json object corresponding to the processor class found
	 * @throws IllegalArgumentException if no processor class ID was given
	 * @throws NoResultException if no processor class with the given ID exists
	 */
	public RestProcessorClass getProcessorClassById(Long id) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClassById({})", id);
		
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_CLASS_ID_MISSING, MSG_ID_PROCESSOR_CLASS_ID_MISSING));
		}
		
		Optional<ProcessorClass> modelProcessorClass = RepositoryService.getProcessorClassRepository().findById(id);
		
		if (modelProcessorClass.isEmpty()) {
			throw new NoResultException(logError(MSG_PROCESSOR_CLASS_ID_NOT_FOUND, MSG_ID_PROCESSOR_CLASS_ID_NOT_FOUND, id));
		}

		logInfo(MSG_PROCESSOR_CLASS_RETRIEVED, MSG_ID_PROCESSOR_CLASS_RETRIEVED, id);
		
		return ProcessorClassUtil.toRestProcessorClass(modelProcessorClass.get());
	}

	/**
	 * Update a processor class by ID
	 * 
	 * @param id the ID of the processor class to update
	 * @param processorClass a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the processor class after modification (with ID and version for all 
	 * 		   contained objects)
	 * @throws EntityNotFoundException if no processor class with the given ID exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws ConcurrentModificationException if the processor class has been modified since retrieval by the client
	 */
	public RestProcessorClass modifyProcessorClass(Long id, @Valid RestProcessorClass processorClass) throws
			EntityNotFoundException, IllegalArgumentException, ConcurrentModificationException {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProcessorClass({}, {})", id, (null == processorClass ? "MISSING" : processorClass.getProcessorName()));

		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_CLASS_ID_MISSING, MSG_ID_PROCESSOR_CLASS_ID_MISSING));
		}
		if (null == processorClass) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_CLASS_DATA_MISSING, MSG_ID_PROCESSOR_CLASS_DATA_MISSING));
		}
		
		Optional<ProcessorClass> optProcessorClass = RepositoryService.getProcessorClassRepository().findById(id);
		
		if (optProcessorClass.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_PROCESSOR_CLASS_ID_NOT_FOUND, MSG_ID_PROCESSOR_CLASS_ID_NOT_FOUND, id));
		}
		ProcessorClass modelProcessorClass = optProcessorClass.get();
		
		// Apply changed attributes
		ProcessorClass changedProcessorClass = ProcessorClassUtil.toModelProcessorClass(processorClass);
		
		boolean processorClassChanged = false;
		if (!modelProcessorClass.getProcessorName().equals(changedProcessorClass.getProcessorName())) {
			processorClassChanged = true;
			modelProcessorClass.setProcessorName(changedProcessorClass.getProcessorName());
		}
		
		// Check changes in associated product classes
		Set<ProductClass> newProductClasses = new HashSet<>();
		for (String productType: processorClass.getProductClasses()) {
			ProductClass productClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(processorClass.getMissionCode(), productType);
			if (null == productClass) {
				throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_INVALID, MSG_ID_PRODUCT_CLASS_INVALID,
						productType, processorClass.getMissionCode()));
			}
			newProductClasses.add(productClass);
			if (!modelProcessorClass.getProductClasses().contains(productClass)) {
				processorClassChanged = true;
			}
		}
		// Check removed product classes
		for (ProductClass productClass: modelProcessorClass.getProductClasses()) {
			if (!newProductClasses.contains(productClass)) {
				processorClassChanged = true;
			}
		}

		// Save processor class only if anything was actually changed
		if (processorClassChanged)	{
			modelProcessorClass.incrementVersion();
			modelProcessorClass.setProductClasses(newProductClasses);
			modelProcessorClass = RepositoryService.getProcessorClassRepository().save(modelProcessorClass);
			logInfo(MSG_PROCESSOR_CLASS_MODIFIED, MSG_ID_PROCESSOR_CLASS_MODIFIED, id);
		} else {
			logInfo(MSG_PROCESSOR_CLASS_NOT_MODIFIED, MSG_ID_PROCESSOR_CLASS_NOT_MODIFIED, id);
		}
		
		return ProcessorClassUtil.toRestProcessorClass(modelProcessorClass);
	}

	/**
	 * Delete a processor class by ID
	 * 
	 * @param the ID of the processor class to delete
	 * @throws EntityNotFoundException if the processor class to delete does not exist in the database
	 * @throws RuntimeException if the deletion was not performed as expected
	 */
	public void deleteProcessorClassById(Long id) throws EntityNotFoundException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClassById({})", id);
		
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_CLASS_ID_MISSING, MSG_ID_PROCESSOR_CLASS_ID_MISSING));
		}
		
		// Test whether the product id is valid
		Optional<ProcessorClass> modelProcessorClass = RepositoryService.getProcessorClassRepository().findById(id);
		if (modelProcessorClass.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_PROCESSOR_CLASS_NOT_FOUND, MSG_ID_PROCESSOR_CLASS_NOT_FOUND));
		}
		
		// Delete the processor class
		RepositoryService.getProcessorClassRepository().deleteById(id);

		// Test whether the deletion was successful
		modelProcessorClass = RepositoryService.getProcessorClassRepository().findById(id);
		if (!modelProcessorClass.isEmpty()) {
			throw new RuntimeException(logError(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, id));
		}
		
		logInfo(MSG_PROCESSOR_CLASS_DELETED, MSG_ID_PROCESSOR_CLASS_DELETED, id);
	}

}
