/**
 * ProcessorClassControllerImpl.java
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

import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.rest.model.ProcessorClass;
import de.dlr.proseo.procmgr.rest.model.ProcessorClassUtil;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage processor classes.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProcessorClassControllerImpl implements ProcessorclassController {
	
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
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_PROCESSOR_CLASS_NOT_FOUND = "(E%d) No processor class found for mission %s and processor name %s";
	private static final String MSG_PROCESSOR_CLASS_LIST_RETRIEVED = "(I%d) Processor class(es) for mission %s and processor name %s retrieved";
	private static final String MSG_PROCESSOR_CLASS_RETRIEVED = "(I%d) Processor class with ID %d retrieved";
	private static final String MSG_PROCESSOR_CLASS_MISSING = "(E%d) Processor class not set";
	private static final String MSG_PROCESSOR_CLASS_ID_MISSING = "(E%d) Processor class ID not set";
	private static final String MSG_PROCESSOR_CLASS_ID_NOT_FOUND = "(E%d) No processor class found with ID %d";
	private static final String MSG_MISSION_CODE_INVALID = "(E%d) Mission code %s invalid";
	private static final String MSG_PRODUCT_CLASS_INVALID = "(E%d) Product type %s invalid for mission %s";
	private static final String MSG_PROCESSOR_CLASS_CREATED = "(I%d) Processor class %s created for mission %s";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-processor-mgr ";

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorClassControllerImpl.class);

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
	 * Get processor classes by mission and name
	 * 
	 * @param mission the mission code (optional)
	 * @param processorName the processor name (optional)
	 * @return a list of Json objects representing processor classes satisfying the search criteria
	 */
	@Override
	public ResponseEntity<List<ProcessorClass>> getProcessorClass(String mission, String processorName) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClass({}, {})", mission, processorName);
		
		List<ProcessorClass> result = new ArrayList<>();
		
		if (null != mission && null != processorName) {
			de.dlr.proseo.model.ProcessorClass processorClass = RepositoryService.getProcessorClassRepository().findByMissionCodeAndProcessorName(mission,
					processorName);
			if (null == processorClass) {
				return new ResponseEntity<>(
						errorHeaders(MSG_PROCESSOR_CLASS_NOT_FOUND, MSG_ID_PROCESSOR_CLASS_NOT_FOUND, mission, processorName),
						HttpStatus.NOT_FOUND);
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
				return new ResponseEntity<>(
						errorHeaders(MSG_PROCESSOR_CLASS_NOT_FOUND, MSG_ID_PROCESSOR_CLASS_NOT_FOUND, mission, processorName),
						HttpStatus.NOT_FOUND);
			}
		}
		logInfo(MSG_PROCESSOR_CLASS_LIST_RETRIEVED, MSG_ID_PROCESSOR_CLASS_LIST_RETRIEVED, mission, processorName);
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	/**
	 * Create a new processor class
	 * 
	 * @param processorClass a Json representation of the new processor class
	 * @return a Json representation of the processor class after creation (with ID and version number)
	 */
	@Override
	public ResponseEntity<ProcessorClass> createProcessorClass(@Valid ProcessorClass processorClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProcessorClass({})", (null == processorClass ? "MISSING" : processorClass.getProcessorName()));
		
		if (null == processorClass) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PROCESSOR_CLASS_MISSING, MSG_ID_PROCESSOR_CLASS_MISSING),
					HttpStatus.BAD_REQUEST);
		}
		
		de.dlr.proseo.model.ProcessorClass modelProcessorClass = ProcessorClassUtil.toModelProcessorClass(processorClass);
		
		modelProcessorClass.setMission(RepositoryService.getMissionRepository().findByCode(processorClass.getMissionCode()));
		if (null == modelProcessorClass.getMission()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_MISSION_CODE_INVALID, MSG_ID_MISSION_CODE_INVALID, processorClass.getMissionCode()),
					HttpStatus.BAD_REQUEST);
		}
		
		for (String productType: processorClass.getProductClasses()) {
			ProductClass productClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(processorClass.getMissionCode(), productType);
			if (null == productClass) {
				return new ResponseEntity<>(
						errorHeaders(MSG_PRODUCT_CLASS_INVALID, MSG_ID_PRODUCT_CLASS_INVALID,
								productType, processorClass.getMissionCode()),
						HttpStatus.BAD_REQUEST);
			}
		}
		
		modelProcessorClass = RepositoryService.getProcessorClassRepository().save(modelProcessorClass);
		
		logInfo(MSG_PROCESSOR_CLASS_CREATED, MSG_ID_PROCESSOR_CLASS_CREATED, 
				modelProcessorClass.getProcessorName(), modelProcessorClass.getMission().getCode());
		
		return new ResponseEntity<>(ProcessorClassUtil.toRestProcessorClass(modelProcessorClass), HttpStatus.CREATED);
	}

	/**
	 * Get a processor class by ID
	 * 
	 * @param id the processor class ID
	 * @return a Json object corresponding to the processor class found and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no processor class with the given ID exists
	 */
	@Override
	public ResponseEntity<ProcessorClass> getProcessorClassById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorClassById({})", id);
		
		if (null == id) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PROCESSOR_CLASS_ID_MISSING, MSG_ID_PROCESSOR_CLASS_ID_MISSING, id), 
					HttpStatus.BAD_REQUEST);
		}
		
		Optional<de.dlr.proseo.model.ProcessorClass> modelProcessorClass = RepositoryService.getProcessorClassRepository().findById(id);
		
		if (modelProcessorClass.isEmpty()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PROCESSOR_CLASS_ID_NOT_FOUND, MSG_ID_PROCESSOR_CLASS_ID_NOT_FOUND, id), 
					HttpStatus.NOT_FOUND);
		}

		logInfo(MSG_PROCESSOR_CLASS_RETRIEVED, MSG_ID_PROCESSOR_CLASS_RETRIEVED, id);
		
		return new ResponseEntity<>(ProcessorClassUtil.toRestProcessorClass(modelProcessorClass.get()), HttpStatus.OK);
	}

	/**
	 * Update a processor class by ID
	 * 
	 * @param id the ID of the processor class to update
	 * @param processorClass a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the processor class after modification (with ID and version for all 
	 * 		   contained objects) and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no processor class with the given ID exists
	 */
	@Override
	public ResponseEntity<ProcessorClass> updateProcessorClass(Long id, @Valid ProcessorClass processorClass) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders("PATCH for processor class not implemented", MSG_ID_NOT_IMPLEMENTED, id), 
				HttpStatus.NOT_IMPLEMENTED);
	}

	/**
	 * Delete a processor class by ID
	 * 
	 * @param the ID of the processor class to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, "NOT_FOUND", if the processor class did not
	 *         exist, or "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteProcessorclassById(Long id) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders("DELETE for processor class not implemented", MSG_ID_NOT_IMPLEMENTED, id), 
				HttpStatus.NOT_IMPLEMENTED);
	}

}
