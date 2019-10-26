/**
 * ProcessorControllerImpl.java
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
import de.dlr.proseo.procmgr.rest.model.Processor;
import de.dlr.proseo.procmgr.rest.model.ProcessorClass;
import de.dlr.proseo.procmgr.rest.model.ProcessorClassUtil;
import de.dlr.proseo.procmgr.rest.model.ProcessorUtil;
import de.dlr.proseo.procmgr.rest.model.Task;
import de.dlr.proseo.procmgr.rest.model.TaskUtil;

/**
 * Spring MVC controller for the prosEO Processor Manager; implements the services required to manage processor versions.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProcessorControllerImpl implements ProcessorController {
	
	/* Message ID constants */
	private static final int MSG_ID_PROCESSOR_NOT_FOUND = 2250;
	private static final int MSG_ID_PROCESSOR_LIST_RETRIEVED = 2251;
	private static final int MSG_ID_PROCESSOR_RETRIEVED = 2252;
	private static final int MSG_ID_PROCESSOR_MISSING = 2253;
	private static final int MSG_ID_PROCESSOR_CLASS_INVALID = 2254;
	private static final int MSG_ID_PROCESSOR_CREATED = 2255;
	private static final int MSG_ID_PROCESSOR_ID_MISSING = 2256;
	private static final int MSG_ID_PROCESSOR_ID_NOT_FOUND = 2257;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_PROCESSOR_NOT_FOUND = "(E%d) No processor found for mission %s, processor name %s and processor version %s";
	private static final String MSG_PROCESSOR_LIST_RETRIEVED = "(I%d) Processor(s) for mission %s, processor name %s and processor version %s retrieved";
	private static final String MSG_PROCESSOR_RETRIEVED = "(I%d) Processor with ID %d retrieved";
	private static final String MSG_PROCESSOR_MISSING = "(E%d) Processor not set";
	private static final String MSG_PROCESSOR_ID_MISSING = "(E%d) Processor ID not set";
	private static final String MSG_PROCESSOR_ID_NOT_FOUND = "(E%d) No processor found with ID %d";
	private static final String MSG_PROCESSOR_CLASS_INVALID = "(E%d) Processor class %s invalid for mission %s";
	private static final String MSG_PROCESSOR_CREATED = "(I%d) Processor %s, version %s created for mission %s";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-processor-mgr ";

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorControllerImpl.class);

	/** Single TransactionTemplate shared amongst all methods in this instance */
	private final TransactionTemplate transactionTemplate;

	/**
	 * Constructor using constructor-injection to supply the PlatformTransactionManager
	 * 
	 * @param transactionManager the platform transaction manager
	 */
	public ProcessorControllerImpl(PlatformTransactionManager transactionManager) {
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
	 * Create a new processor (version)
	 * 
	 * @param processor a Json representation of the new processor
	 * @return a Json representation of the processor after creation (with ID and version number)
	 */
	@Override
	public ResponseEntity<Processor> createProcessor(Processor processor) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProcessor({})", (null == processor ? "MISSING" : processor.getProcessorName()));

		if (null == processor) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PROCESSOR_MISSING, MSG_ID_PROCESSOR_MISSING),
					HttpStatus.BAD_REQUEST);
		}
		
		return transactionTemplate.execute(new TransactionCallback<>() {

			@Override
			public ResponseEntity<Processor> doInTransaction(TransactionStatus txStatus) {
				de.dlr.proseo.model.Processor modelProcessor = ProcessorUtil.toModelProcessor(processor);
				
				modelProcessor.setProcessorClass(RepositoryService.getProcessorClassRepository()
						.findByMissionCodeAndProcessorName(processor.getMissionCode(), processor.getProcessorName()));
				if (null == modelProcessor.getProcessorClass()) {
					txStatus.setRollbackOnly();
					return new ResponseEntity<>(
							errorHeaders(MSG_PROCESSOR_CLASS_INVALID, MSG_ID_PROCESSOR_CLASS_INVALID,
									processor.getProcessorName(), processor.getMissionCode()),
							HttpStatus.BAD_REQUEST);
				}
				
				modelProcessor = RepositoryService.getProcessorRepository().save(modelProcessor);
				
				for (Task task: processor.getTasks()) {
					de.dlr.proseo.model.Task modelTask = TaskUtil.toModelTask(task);
					modelTask.setProcessor(modelProcessor);
					modelTask = RepositoryService.getTaskRepository().save(modelTask);
				}

				logInfo(MSG_PROCESSOR_CREATED, MSG_ID_PROCESSOR_CREATED, 
						modelProcessor.getProcessorClass().getProcessorName(),
						modelProcessor.getProcessorVersion(), 
						modelProcessor.getProcessorClass().getMission().getCode());
				
				return new ResponseEntity<>(ProcessorUtil.toRestProcessor(modelProcessor), HttpStatus.CREATED);
			}
		});
	}

	/**
	 * Get processors by mission, name and version (user-defined version, not database version)
	 * 
	 * @param mission the mission code
	 * @param processorName the name of the processor (class)
	 * @param processorVersion the processor version
	 * @return a list of Json objects representing processors satisfying the search criteria
	 */
	@Override
	public ResponseEntity<List<Processor>> getProcessors(String mission, String processorName, String processorVersion) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessors({}, {}, {})", mission, processorName, processorVersion);
		
		List<Processor> result = new ArrayList<>();
		
		if (null != mission && null != processorName && null != processorVersion) {
			de.dlr.proseo.model.Processor processor = RepositoryService.getProcessorRepository()
					.findByMissionCodeAndProcessorNameAndProcessorVersion(mission, processorName, processorVersion);
			if (null == processor) {
				return new ResponseEntity<>(
						errorHeaders(MSG_PROCESSOR_NOT_FOUND, MSG_ID_PROCESSOR_NOT_FOUND, mission, processorName, processorVersion),
						HttpStatus.NOT_FOUND);
			}
			result.add(ProcessorUtil.toRestProcessor(processor));
		} else {
			String jpqlQuery = "select p from Processor p where 1 = 1";
			if (null != mission) {
				jpqlQuery += " and processorClass.mission.code = :missionCode";
			}
			if (null != processorName) {
				jpqlQuery += " and processorClass.processorName = :processorName";
			}
			if (null != processorVersion) {
				jpqlQuery += " and processorVersion = :processorVersion";
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
			for (Object resultObject: query.getResultList()) {
				if (resultObject instanceof de.dlr.proseo.model.Processor) {
					result.add(ProcessorUtil.toRestProcessor((de.dlr.proseo.model.Processor) resultObject));
				}
			}
			if (result.isEmpty()) {
				return new ResponseEntity<>(
						errorHeaders(MSG_PROCESSOR_NOT_FOUND, MSG_ID_PROCESSOR_NOT_FOUND, mission, processorName, processorVersion),
						HttpStatus.NOT_FOUND);
			}
		}
		logInfo(MSG_PROCESSOR_LIST_RETRIEVED, MSG_ID_PROCESSOR_LIST_RETRIEVED, mission, processorName, processorVersion);
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	/**
	 * Get a processor by ID
	 * 
	 * @param id the processor ID
	 * @return a Json object corresponding to the processor found and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no processor with the given ID exists
	 */
	@Override
	public ResponseEntity<Processor> getProcessorById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessorById({})", id);
		
		if (null == id) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PROCESSOR_ID_MISSING, MSG_ID_PROCESSOR_ID_MISSING, id), 
					HttpStatus.BAD_REQUEST);
		}
		
		Optional<de.dlr.proseo.model.Processor> modelProcessor = RepositoryService.getProcessorRepository().findById(id);
		
		if (modelProcessor.isEmpty()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PROCESSOR_ID_NOT_FOUND, MSG_ID_PROCESSOR_ID_NOT_FOUND, id), 
					HttpStatus.NOT_FOUND);
		}

		logInfo(MSG_PROCESSOR_RETRIEVED, MSG_ID_PROCESSOR_RETRIEVED, id);
		
		return new ResponseEntity<>(ProcessorUtil.toRestProcessor(modelProcessor.get()), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Processor> updateProcessor(Long id, @Valid Processor processor) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders("PATCH for processor not implemented", MSG_ID_NOT_IMPLEMENTED, id), 
				HttpStatus.NOT_IMPLEMENTED);
	}

	@Override
	public ResponseEntity<?> deleteProcessorById(Long id) {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(
				errorHeaders("DELETE for processor not implemented", MSG_ID_NOT_IMPLEMENTED, id), 
				HttpStatus.NOT_IMPLEMENTED);
	}

}
