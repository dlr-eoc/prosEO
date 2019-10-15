/**
 * ProcessorControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.rest.model.Processor;
import de.dlr.proseo.procmgr.rest.model.ProcessorClassUtil;
import de.dlr.proseo.procmgr.rest.model.ProcessorUtil;
import de.dlr.proseo.procmgr.rest.model.Task;

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
	private static final int MSG_ID_MISSION_CODE_INVALID = 2254;
	private static final int MSG_ID_PROCESSOR_CLASS_INVALID = 2255;
	private static final int MSG_ID_PROCESSOR_CREATED = 2258;
	private static final int MSG_ID_PROCESSOR_ID_MISSING = 2259;
	private static final int MSG_ID_PROCESSOR_ID_NOT_FOUND = 2250;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_PROCESSOR_NOT_FOUND = "(E%d) No processor found for mission %s and processor name %s";
	private static final String MSG_PROCESSOR_LIST_RETRIEVED = "(I%d) Processor for mission %s and processor name %s retrieved";
	private static final String MSG_PROCESSOR_RETRIEVED = "(I%d) Processor with ID %d retrieved";
	private static final String MSG_PROCESSOR_MISSING = "(E%d) Processor not set";
	private static final String MSG_PROCESSOR_ID_MISSING = "(E%d) Processor ID not set";
	private static final String MSG_PROCESSOR_ID_NOT_FOUND = "(E%d) No processor found with ID %d";
	private static final String MSG_MISSION_CODE_INVALID = "(E%d) Mission code %s invalid";
	private static final String MSG_PROCESSOR_CLASS_INVALID = "(E%d) Processor class %s invalid for mission %s";
	private static final String MSG_PROCESSOR_CREATED = "(I%d) Processor %s, version %s created for mission %s";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-ingestor ";

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorControllerImpl.class);

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
	
		de.dlr.proseo.model.Processor modelProcessor = ProcessorUtil.toModelProcessor(processor);
		
		modelProcessor.setProcessorClass(RepositoryService.getProcessorClassRepository()
				.findByMissionCodeAndProcessorName(processor.getMissionCode(), processor.getProcessorName()));
		if (null == modelProcessor.getProcessorClass()) {
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

	@Override
	public ResponseEntity<List<Processor>> getProcessors(String mission, String processorName, String processorVersion) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<Processor> getProcessorById(Long id) {
		// TODO Auto-generated method stub

		String message = String.format(MSG_PREFIX + "GET for id %s not implemented (%d)", id, 2000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<Processor> updateProcessor(Long id, @Valid Processor processor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteProcessorById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

}
