/**
 * ProcessorCommandRunner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.rest.model.RestProcessor;
import de.dlr.proseo.model.rest.model.RestProcessorClass;
import de.dlr.proseo.model.rest.model.RestTask;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.backend.UserManager;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;
import de.dlr.proseo.ui.cli.parser.ParsedParameter;

/**
 * Run commands for managing prosEO processor classes, versions and configurations (create, read, update, delete etc.). 
 * All methods assume that before invocation a syntax check of the command has been performed, so no extra checks are performed.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProcessorCommandRunner {

	/* General string constants */
	public static final String CMD_PROCESSOR = "processor";
	private static final String CMD_CLASS = "class";
	private static final String CMD_CONFIGURATION = "configuration";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";

	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_PROCESSOR_NAME = "Processor name (empty field cancels): ";
	private static final String PROMPT_PROCESSOR_VERSION = "Processor version (empty field cancels): ";
	private static final String PROMPT_PRODUCT_CLASSES = "Processible product classes (comma-separated list; empty field cancels): ";
	private static final String PROMPT_TASKS = "Task names (comma-separated list; empty field cancels): ";
	private static final String PROMPT_TASK_VERSION = "Task version for %s (empty field cancels): ";
	private static final String PROMPT_CRITICALITY_LEVEL = "Criticality level for %s (empty field cancels): ";
	private static final String PROMPT_DOCKER_IMAGE = "Docker image (empty field cancels): ";
	
	private static final String URI_PATH_PROCESSORCLASSES = "/processorclasses";
	private static final String URI_PATH_PROCESSORS = "/processors";
	private static final String URI_PATH_CONFIGURATIONS = "/configurations";
	private static final String URI_PATH_CONFIGUREDPROCESSORS = "/configuredprocessors";
	
	private static final String PROCESSORS = "processors";
	
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss").withZone(ZoneId.of("UTC"));

	/** The user manager used by all command runners */
	@Autowired
	private UserManager userManager;
	
	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;
	
	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorCommandRunner.class);

	/**
	 * Create a new processor class; if the input is not from a file, the user will be prompted for mandatory attributes
	 * not given on the command line
	 * 
	 * @param createCommand the parsed "processor class create" command
	 */
	private void createProcessorClass(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProcessorClass({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File processorClassFile = null;
		String processorClassFileFormat = null;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				processorClassFile = new File(option.getValue());
				break;
			case "format":
				processorClassFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read processor class file, if any */
		RestProcessorClass restProcessorClass = null;
		if (null == processorClassFile) {
			restProcessorClass = new RestProcessorClass();
		} else {
			try {
				restProcessorClass = CLIUtil.parseObjectFile(processorClassFile, processorClassFileFormat, RestProcessorClass.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(e.getMessage());
				return;
			}
		}
		
		/* Check command parameters (overriding values from processor class file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is processor class name
				restProcessorClass.setProcessorName(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restProcessorClass, param.getValue());
				} catch (Exception e) {
					System.err.println(e.getMessage());
					return;
				}
			}
		}
		
		/* Set missing attributes to default values where possible */
		if (null == restProcessorClass.getMissionCode() || 0 == restProcessorClass.getMissionCode().length()) {
			restProcessorClass.setMissionCode(userManager.getMission());
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restProcessorClass.getProcessorName() || 0 == restProcessorClass.getProcessorName().length()) {
			System.out.print(PROMPT_PROCESSOR_NAME);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProcessorClass.setProcessorName(response);
		}
		if (null == restProcessorClass.getProductClasses() || restProcessorClass.getProductClasses().isEmpty()) {
			System.out.print(PROMPT_PRODUCT_CLASSES);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProcessorClass.setProductClasses(Arrays.asList(response.split(",")));
		}
		
		/* Create product */
		try {
			restProcessorClass = serviceConnection.postToService(serviceConfig.getProcessorManagerUrl(), URI_PATH_PROCESSORCLASSES, 
					restProcessorClass, RestProcessorClass.class, userManager.getUser(), userManager.getPassword());
		} catch (HttpClientErrorException.BadRequest e) {
			// Already logged
			System.err.println(uiMsg(MSG_ID_PROCESSORCLASS_DATA_INVALID,  e.getMessage()));
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(uiMsg(MSG_ID_NOT_AUTHORIZED, userManager.getUser(), PROCESSORS, userManager.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}

		/* Report success, giving newly assigned product ID and UUID */
		String message = uiMsg(MSG_ID_PROCESSORCLASS_CREATED,
				restProcessorClass.getProcessorName(), restProcessorClass.getId());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Show the processor class specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "processor class show" command
	 */
	private void showProcessorClass(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showProcessorClass({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Prepare request URI */
		String requestURI = URI_PATH_PROCESSORCLASSES + "?mission=" + userManager.getMission();
		
		if (!showCommand.getParameters().isEmpty()) {
			// Only processor name allowed as parameter
			requestURI += "&processorName=" + showCommand.getParameters().get(0).getValue();
		}
		
		/* Get the processor class information from the Processor Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					requestURI, List.class, userManager.getUser(), userManager.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = uiMsg(MSG_ID_NO_PROCESSORCLASSES_FOUND);
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(uiMsg(MSG_ID_NOT_AUTHORIZED,  userManager.getUser(), PROCESSORS, userManager.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Display the processor class(es) found */
		ObjectMapper mapper = new ObjectMapper();
		for (Object result: resultList) {
			RestProcessorClass restProcessorClass = mapper.convertValue(result, RestProcessorClass.class);
			
			// TODO Format output
			System.out.println(restProcessorClass);
		}
	}
	
	/**
	 * Update a processor class from a processor class file or from "attribute=value" pairs (overriding any processor class file entries)
	 * 
	 * @param updateCommand the parsed "processor class update" command
	 */
	private void updateProcessorClass(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateProcessorClass({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File processorClassFile = null;
		String processorClassFileFormat = null;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				processorClassFile = new File(option.getValue());
				break;
			case "format":
				processorClassFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read processor class file, if any */
		RestProcessorClass updatedProcessorClass = null;
		if (null == processorClassFile) {
			updatedProcessorClass = new RestProcessorClass();
		} else {
			try {
				updatedProcessorClass = CLIUtil.parseObjectFile(processorClassFile, processorClassFileFormat, RestProcessorClass.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(e.getMessage());
				return;
			}
		}
		
		/* Check command parameters (overriding values from processor class file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is processor class name
				updatedProcessorClass.setProcessorName(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedProcessorClass, param.getValue());
				} catch (Exception e) {
					System.err.println(e.getMessage());
					return;
				}
			}
		}
		
		/* Read original processor class from Processor Manager service */
		if (null == updatedProcessorClass.getProcessorName() || 0 == updatedProcessorClass.getProcessorName().length()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_PROCCLASS_IDENTIFIER_GIVEN));
			return;
		}
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_PROCESSORCLASSES + "?mission=" + userManager.getMission() + "&processorName=" + updatedProcessorClass.getProcessorName(),
					List.class, userManager.getUser(), userManager.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = uiMsg(MSG_ID_PROCESSORCLASS_NOT_FOUND, updatedProcessorClass.getProcessorName());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.BadRequest e) {
			// Already logged
			System.err.println(uiMsg(MSG_ID_PROCESSORCLASS_DATA_INVALID,  e.getMessage()));
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(uiMsg(MSG_ID_NOT_AUTHORIZED,  userManager.getUser(), PROCESSORS, userManager.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		if (resultList.isEmpty()) {
			String message = uiMsg(MSG_ID_PROCESSORCLASS_NOT_FOUND, updatedProcessorClass.getProcessorName());
			logger.error(message);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestProcessorClass restProcessorClass = mapper.convertValue(resultList.get(0), RestProcessorClass.class);

		/* Compare attributes of database product with updated product */
		// No modification of ID, version, mission code or processor class name allowed
		if (!updatedProcessorClass.getProductClasses().isEmpty()) { // mandatory, must not be empty
			restProcessorClass.setProductClasses(updatedProcessorClass.getProductClasses());
		}
		
		/* Update processor class using Processor Manager service */
		try {
			restProcessorClass = serviceConnection.patchToService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_PROCESSORCLASSES + "/" + restProcessorClass.getId(),
					restProcessorClass, RestProcessorClass.class, userManager.getUser(), userManager.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = uiMsg(MSG_ID_PROCESSORCLASS_NOT_FOUND_BY_ID, restProcessorClass.getId());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(uiMsg(MSG_ID_NOT_AUTHORIZED,  userManager.getUser(), PROCESSORS, userManager.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success, giving new processor class version */
		String message = uiMsg(MSG_ID_PROCESSORCLASS_UPDATED, restProcessorClass.getId(), restProcessorClass.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Delete the given processor class
	 * 
	 * @param deleteCommand the parsed "processor class delete" command
	 */
	private void deleteProcessorClass(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProcessorClass({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get processor class name from command parameters */
		if (deleteCommand.getParameters().isEmpty()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_PROCCLASS_IDENTIFIER_GIVEN));
			return;
		}
		String processorName = deleteCommand.getParameters().get(0).getValue();
		
		/* Retrieve the processor class using Processor Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(), 
					URI_PATH_PROCESSORCLASSES + "?mission=" + userManager.getMission() + "&processorName=" + processorName, 
					List.class, userManager.getUser(), userManager.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = uiMsg(MSG_ID_PROCESSORCLASS_NOT_FOUND, processorName);
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(uiMsg(MSG_ID_NOT_AUTHORIZED,  userManager.getUser(), PROCESSORS, userManager.getMission()));
			return;
		} catch (Exception e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		if (resultList.isEmpty()) {
			String message = uiMsg(MSG_ID_PROCESSORCLASS_NOT_FOUND, processorName);
			logger.error(message);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestProcessorClass restProcessorClass = mapper.convertValue(resultList.get(0), RestProcessorClass.class);
		
		/* Delete processor class using Processor Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_PROCESSORCLASSES + "/" + restProcessorClass.getId(), 
					userManager.getUser(), userManager.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = uiMsg(MSG_ID_PROCESSORCLASS_NOT_FOUND_BY_ID, restProcessorClass.getId());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(uiMsg(MSG_ID_NOT_AUTHORIZED,  userManager.getUser(), PROCESSORS, userManager.getMission()));
			return;
		} catch (Exception e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success */
		String message = uiMsg(MSG_ID_PROCESSORCLASS_DELETED, restProcessorClass.getId());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Create a new processor ; if the input is not from a file, the user will be prompted for mandatory attributes
	 * not given on the command line
	 * 
	 * @param createCommand the parsed "processor create" command
	 */
	private void createProcessor(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProcessor({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File processorFile = null;
		String processorFileFormat = null;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				processorFile = new File(option.getValue());
				break;
			case "format":
				processorFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read processor file, if any */
		RestProcessor restProcessor = null;
		if (null == processorFile) {
			restProcessor = new RestProcessor();
		} else {
			try {
				restProcessor = CLIUtil.parseObjectFile(processorFile, processorFileFormat, RestProcessor.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(e.getMessage());
				return;
			}
		}
		
		/* Check command parameters (overriding values from processor class file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is processor class name
				restProcessor.setProcessorName(param.getValue());
			} else if (1 == i) {
				// Second parameter is processor version
				restProcessor.setProcessorVersion(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restProcessor, param.getValue());
				} catch (Exception e) {
					System.err.println(e.getMessage());
					return;
				}
			}
		}
		
		/* Set missing attributes to default values where possible */
		if (null == restProcessor.getMissionCode() || 0 == restProcessor.getMissionCode().length()) {
			restProcessor.setMissionCode(userManager.getMission());
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restProcessor.getProcessorName() || 0 == restProcessor.getProcessorName().length()) {
			System.out.print(PROMPT_PROCESSOR_NAME);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProcessor.setProcessorName(response);
		}
		if (null == restProcessor.getProcessorVersion() || 0 == restProcessor.getProcessorVersion().length()) {
			System.out.print(PROMPT_PROCESSOR_VERSION);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProcessor.setProcessorVersion(response);
		}
		if (null == restProcessor.getTasks() || restProcessor.getTasks().isEmpty()) {
			System.out.print(PROMPT_TASKS);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			String[] tasks = response.split(",");
			for (String task: tasks) {
				RestTask restTask = new RestTask();
				restProcessor.getTasks().add(restTask);
				restTask.setTaskName(task);
				System.out.print(String.format(PROMPT_TASK_VERSION, task));
				response = System.console().readLine();
				if ("".equals(response)) {
					System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
					return;
				}
				restTask.setTaskVersion(response);
				if (1 < tasks.length) {
					while (null == restTask.getCriticalityLevel()) {
						System.out.print(String.format(PROMPT_CRITICALITY_LEVEL, task));
						response = System.console().readLine();
						if ("".equals(response)) {
							System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
							return;
						}
						try {
							restTask.setCriticalityLevel(Long.parseLong(response));
						} catch (NumberFormatException e) {
							System.err.println(uiMsg(MSG_ID_INVALID_CRITICALITY_LEVEL, response));
							continue;
						}
						if (2 > restTask.getCriticalityLevel()) {
							System.err.println(uiMsg(MSG_ID_INVALID_CRITICALITY_LEVEL, response));
							restTask.setCriticalityLevel(null);
							continue;
						}
					}
				}
			}
		}
		if (null == restProcessor.getDockerImage() || 0 == restProcessor.getDockerImage().length()) {
			System.out.print(PROMPT_DOCKER_IMAGE);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProcessor.setDockerImage(response);
		}
		
		/* Create product */
		try {
			restProcessor = serviceConnection.postToService(serviceConfig.getProcessorManagerUrl(), URI_PATH_PROCESSORS, 
					restProcessor, RestProcessor.class, userManager.getUser(), userManager.getPassword());
		} catch (HttpClientErrorException.BadRequest e) {
			// Already logged
			System.err.println(uiMsg(MSG_ID_PROCESSOR_DATA_INVALID,  e.getMessage()));
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(uiMsg(MSG_ID_NOT_AUTHORIZED,  userManager.getUser(), PROCESSORS, userManager.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}

		/* Report success, giving newly assigned product ID and UUID */
		String message = uiMsg(MSG_ID_PROCESSOR_CREATED,
				restProcessor.getProcessorName(), restProcessor.getProcessorVersion(), restProcessor.getId());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Show the processor specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "processor show" command
	 */
	private void showProcessor(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showProcessor({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Prepare request URI */
		String requestURI = URI_PATH_PROCESSORS + "?mission=" + userManager.getMission();
		
		for (int i = 0; i < showCommand.getParameters().size(); ++i) {
			String paramValue = showCommand.getParameters().get(i).getValue();
			if (0 == i) {
				// First parameter is processor name
				requestURI += "&processorName=" + paramValue;
			} else if (1 == i) {
				// Second parameter is processor version
				requestURI += "&processorVersion=" + paramValue;
			}
		}
		
		/* Get the processor information from the Processor Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					requestURI, List.class, userManager.getUser(), userManager.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = uiMsg(MSG_ID_NO_PROCESSORS_FOUND);
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(uiMsg(MSG_ID_NOT_AUTHORIZED,  userManager.getUser(), PROCESSORS, userManager.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Display the processor class(es) found */
		ObjectMapper mapper = new ObjectMapper();
		for (Object result: resultList) {
			RestProcessor restProcessor = mapper.convertValue(result, RestProcessor.class);
			
			// TODO Format output
			System.out.println(restProcessor);
		}
	}
	
	/**
	 * Update a processor from a processor file or from "attribute=value" pairs (overriding any processor file entries)
	 * 
	 * @param updateCommand the parsed "processor update" command
	 */
	private void updateProcessor(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateProcessor({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File processorFile = null;
		String processorFileFormat = null;
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				processorFile = new File(option.getValue());
				break;
			case "format":
				processorFileFormat = option.getValue().toUpperCase();
				break;
			case "delete-attributes":
				isDeleteAttributes = true;
				break;
			}
		}
		
		/* Read processor file, if any */
		RestProcessor updatedProcessor = null;
		if (null == processorFile) {
			updatedProcessor = new RestProcessor();
		} else {
			try {
				updatedProcessor = CLIUtil.parseObjectFile(processorFile, processorFileFormat, RestProcessor.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(e.getMessage());
				return;
			}
		}
		
		/* Check command parameters (overriding values from processor class file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is processor class name
				updatedProcessor.setProcessorName(param.getValue());
			} else if (1 == i) {
				// Second parameter is processor version
				updatedProcessor.setProcessorVersion(param.getValue());;
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedProcessor, param.getValue());
				} catch (Exception e) {
					System.err.println(e.getMessage());
					return;
				}
			}
		}
		
		/* Read original processor from Processor Manager service */
		if (null == updatedProcessor.getProcessorName() || 0 == updatedProcessor.getProcessorName().length()
				|| null == updatedProcessor.getProcessorVersion() || 0 == updatedProcessor.getProcessorVersion().length()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_PROCESSOR_IDENTIFIER_GIVEN));
			return;
		}
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_PROCESSORS + "?mission=" + userManager.getMission() 
						+ "&processorName=" + updatedProcessor.getProcessorName() + "&processorVersion=" + updatedProcessor.getProcessorVersion(),
					List.class, userManager.getUser(), userManager.getPassword());
		} catch (HttpClientErrorException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
				message = uiMsg(MSG_ID_PROCESSOR_NOT_FOUND, 
						updatedProcessor.getProcessorName(), updatedProcessor.getProcessorVersion());
			} else if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
				message = uiMsg(MSG_ID_NOT_AUTHORIZED,  userManager.getUser(), PROCESSORS, userManager.getMission());
			} else {
				message = e.getMessage();
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught RuntimeException " + e.getMessage());
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		if (resultList.isEmpty()) {
			if (logger.isTraceEnabled()) logger.trace("Got empty result list");
			String message = uiMsg(MSG_ID_PROCESSOR_NOT_FOUND, 
					updatedProcessor.getProcessorName(), updatedProcessor.getProcessorVersion());
			logger.error(message);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestProcessor restProcessor = mapper.convertValue(resultList.get(0), RestProcessor.class);

		/* Compare attributes of database processor with updated processor */
		// No modification of ID, version, mission code, processor class name or version allowed
		if (null != updatedProcessor.getIsTest()) { // not null
			restProcessor.setIsTest(updatedProcessor.getIsTest());
		}
		if (null != updatedProcessor.getMinDiskSpace()) { // not null
			restProcessor.setMinDiskSpace(updatedProcessor.getMinDiskSpace());
		}
		if (null != updatedProcessor.getMaxTime()) { // not null
			restProcessor.setMaxTime(updatedProcessor.getMaxTime());
		}
		if (null != updatedProcessor.getSensingTimeFlag()) { // not null
			restProcessor.setSensingTimeFlag(updatedProcessor.getSensingTimeFlag());
		}
		if (null != updatedProcessor.getTasks() && (isDeleteAttributes || !updatedProcessor.getTasks().isEmpty())) {
			restProcessor.getTasks().clear();
			restProcessor.getTasks().addAll(updatedProcessor.getTasks());
		}
		if (null != updatedProcessor.getDockerImage()) { // not null
			restProcessor.setDockerImage(updatedProcessor.getDockerImage());
		}
		if (isDeleteAttributes || (null != updatedProcessor.getDockerRunParameters() && 0 != updatedProcessor.getDockerRunParameters().length())) {
			restProcessor.setDockerRunParameters(updatedProcessor.getDockerRunParameters());
		}
		
		/* Update processor using Processor Manager service */
		try {
			restProcessor = serviceConnection.patchToService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_PROCESSORS + "/" + restProcessor.getId(),
					restProcessor, RestProcessor.class, userManager.getUser(), userManager.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException.NotFound " + e.getMessage());
			String message = uiMsg(MSG_ID_PROCESSOR_NOT_FOUND_BY_ID, restProcessor.getId());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.BadRequest e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException.BadRequest " + e.getMessage());
			// Already logged
			System.err.println(uiMsg(MSG_ID_PROCESSOR_DATA_INVALID,  e.getMessage()));
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException.Unauthorized " + e.getMessage());
			// Already logged
			System.err.println(uiMsg(MSG_ID_NOT_AUTHORIZED,  userManager.getUser(), PROCESSORS, userManager.getMission()));
			return;
		} catch (RuntimeException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught RuntimeException " + e.getMessage());
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success, giving new processor version */
		String message = uiMsg(MSG_ID_PROCESSOR_UPDATED, restProcessor.getId(), restProcessor.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Delete the given processor
	 * 
	 * @param deleteCommand the parsed "processor delete" command
	 */
	private void deleteProcessor(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProcessor({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get processor name from command parameters */
		if (2 > deleteCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_PROCESSOR_IDENTIFIER_GIVEN));
			return;
		}
		String processorName = deleteCommand.getParameters().get(0).getValue();
		String processorVersion = deleteCommand.getParameters().get(1).getValue();
		
		/* Retrieve the processor using Processor Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(), 
					URI_PATH_PROCESSORS + "?mission=" + userManager.getMission()
						+ "&processorName=" + processorName + "&processorVersion=" + processorVersion, 
					List.class, userManager.getUser(), userManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PROCESSOR_NOT_FOUND, processorName, processorVersion);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, userManager.getUser(), PROCESSORS, userManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = uiMsg(MSG_ID_PROCESSOR_DELETE_FAILED, processorName, processorVersion, e.getMessage());
				break;
			default:
				message = e.getMessage();
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		if (resultList.isEmpty()) {
			String message = uiMsg(MSG_ID_PROCESSOR_NOT_FOUND, processorName, processorVersion);
			logger.error(message);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestProcessor restProcessor = mapper.convertValue(resultList.get(0), RestProcessor.class);
		
		/* Delete processor using Processor Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_PROCESSORS + "/" + restProcessor.getId(), 
					userManager.getUser(), userManager.getPassword());
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PROCESSOR_NOT_FOUND_BY_ID, restProcessor.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, userManager.getUser(), PROCESSORS, userManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = uiMsg(MSG_ID_PROCESSOR_DELETE_FAILED, processorName, processorVersion, e.getMessage());
				break;
			default:
				message = e.getMessage();
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			// Already logged
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success */
		String message = uiMsg(MSG_ID_PROCESSOR_DELETED, restProcessor.getId());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Run the given command
	 * 
	 * @param command the command to execute
	 */
	void executeCommand(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> executeCommand({})", (null == command ? "null" : command.getName()));
		
		/* Check that user is logged in */
		if (null == userManager.getUser()) {
			System.err.println(uiMsg(MSG_ID_USER_NOT_LOGGED_IN, command.getName()));
		}
		
		/* Check argument */
		if (!CMD_PROCESSOR.equals(command.getName()) && !CMD_CONFIGURATION.equals(command.getName())) {
			System.err.println(uiMsg(MSG_ID_INVALID_COMMAND_NAME, command.getName()));
			return;
		}
		
		/* Make sure a subcommand is given */
		ParsedCommand subcommand = command.getSubcommand();
		if (null == subcommand 
				|| ((CMD_CLASS.equals(subcommand.getName()) || CMD_CONFIGURATION.equals(subcommand.getName()))
						&& null == subcommand.getSubcommand())) {
			System.err.println(uiMsg(MSG_ID_SUBCOMMAND_MISSING, command.getName()));
			return;
		}
		ParsedCommand subsubcommand = subcommand.getSubcommand();
		
		/* Check for subcommand help request */
		if (null != subsubcommand && subsubcommand.isHelpRequested()) {
			subsubcommand.getSyntaxCommand().printHelp(System.out);
			return;
		} else if (subcommand.isHelpRequested()) {
			subcommand.getSyntaxCommand().printHelp(System.out);
			return;
		}
		
		/* Execute the (sub-)sub-command */
		COMMAND:
		switch (command.getName()) {
		case CMD_PROCESSOR:
			switch (subcommand.getName()) {
			case CMD_CLASS:
				// Handle commands for processor classes
				switch (subsubcommand.getName()) {
				case CMD_CREATE:	createProcessorClass(subsubcommand); break COMMAND;
				case CMD_SHOW:		showProcessorClass(subsubcommand); break COMMAND;
				case CMD_UPDATE:	updateProcessorClass(subsubcommand); break COMMAND;
				case CMD_DELETE:	deleteProcessorClass(subsubcommand); break COMMAND;
				default:
					System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, 
							command.getName() + " " + subcommand.getName() + " " + subsubcommand.getName()));
					return;
				}
			case CMD_CONFIGURATION:
				// Handle commands for configured processors
				switch (subsubcommand.getName()) {
//				case CMD_CREATE:	createConfiguredProcessor(subsubcommand); break COMMAND;
//				case CMD_SHOW:		showConfiguredProcessor(subsubcommand); break COMMAND;
//				case CMD_UPDATE:	updateConfiguredProcessor(subsubcommand); break COMMAND;
//				case CMD_DELETE:	deleteConfiguredProcessors(subsubcommand); break COMMAND;
				default:
					System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, 
							command.getName() + " " + subcommand.getName() + " " + subsubcommand.getName()));
					return;
				}
			// Handle commands for processors
			case CMD_CREATE:	createProcessor(subcommand); break COMMAND;
			case CMD_SHOW:		showProcessor(subcommand); break COMMAND;
			case CMD_UPDATE:	updateProcessor(subcommand); break COMMAND;
			case CMD_DELETE:	deleteProcessor(subcommand); break COMMAND;
			default:
				System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		case CMD_CONFIGURATION:
			// Handle commands for configurations
			switch (subcommand.getName()) {
//			case CMD_CREATE:	createConfiguration(subcommand); break COMMAND;
//			case CMD_SHOW:		showConfiguration(subcommand); break COMMAND;
//			case CMD_UPDATE:	updateConfiguration(subcommand); break COMMAND;
//			case CMD_DELETE:	deleteConfiguration(subcommand); break COMMAND;
			default:
				System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		}
	}
}
