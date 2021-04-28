/**
 * ProcessorCommandRunner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.rest.model.RestConfiguration;
import de.dlr.proseo.model.rest.model.RestConfiguredProcessor;
import de.dlr.proseo.model.rest.model.RestProcessor;
import de.dlr.proseo.model.rest.model.RestProcessorClass;
import de.dlr.proseo.model.rest.model.RestTask;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;
import de.dlr.proseo.ui.cli.parser.ParsedParameter;

/**
 * Run commands for managing prosEO processor classes, versions and configurations (create, read, update, delete etc.). 
 * All methods assume that before invocation a syntax check of the command has been performed, so no extra checks are performed.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class ProcessorCommandRunner {

	/* General string constants */
	public static final String CMD_PROCESSOR = "processor";
	private static final String CMD_CLASS = "class";
	public static final String CMD_CONFIGURATION = "configuration";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";

	private static final String OPTION_DELETE_ATTRIBUTES = "delete-attributes";
	private static final String OPTION_VERBOSE = "verbose";
	private static final String OPTION_FORMAT = "format";
	private static final String OPTION_FILE = "file";
	
	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_PROCESSOR_NAME = "Processor class name (empty field cancels): ";
	private static final String PROMPT_PROCESSOR_VERSION = "Processor version (empty field cancels): ";
	private static final String PROMPT_TASKS = "Task names (comma-separated list; empty field cancels): ";
	private static final String PROMPT_TASK_VERSION = "Task version for %s (empty field cancels): ";
	private static final String PROMPT_CRITICALITY_LEVEL = "Criticality level for %s (empty field cancels): ";
	private static final String PROMPT_DOCKER_IMAGE = "Docker image (empty field cancels): ";
	private static final String PROMPT_CONFIGURATION_VERSION = "Configuration version (empty field cancels): ";
	private static final String PROMPT_CONFIGUREDPROCESSOR_IDENTIFIER = "Configured processor identifier (empty field cancels): ";
	
	private static final String URI_PATH_PROCESSORCLASSES = "/processorclasses";
	private static final String URI_PATH_PROCESSORS = "/processors";
	private static final String URI_PATH_CONFIGURATIONS = "/configurations";
	private static final String URI_PATH_CONFIGUREDPROCESSORS = "/configuredprocessors";
	
	private static final String PROCESSORCLASSES = "processor classes";
	private static final String PROCESSORS = "processors";
	private static final String CONFIGURATIONS = "configurations";
	private static final String CONFIGUREDPROCESSORS = "configured processors";
	
	/** The user manager used by all command runners */
	@Autowired
	private LoginManager loginManager;
	
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
		String processorClassFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				processorClassFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
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
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
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
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Set missing attributes to default values where possible */
		if (null == restProcessorClass.getMissionCode() || 0 == restProcessorClass.getMissionCode().length()) {
			restProcessorClass.setMissionCode(loginManager.getMission());
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restProcessorClass.getProcessorName() || 0 == restProcessorClass.getProcessorName().length()) {
			System.out.print(PROMPT_PROCESSOR_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProcessorClass.setProcessorName(response);
		}
		
		/* Create processor class */
		try {
			restProcessorClass = serviceConnection.postToService(serviceConfig.getProcessorManagerUrl(), URI_PATH_PROCESSORCLASSES, 
					restProcessorClass, RestProcessorClass.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_PROCESSORCLASS_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PROCESSORCLASSES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}

		/* Report success, giving newly assigned processor class ID */
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
		
		/* Check command options */
		String processorClassOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORMAT:
				processorClassOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
				break;
			}
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_PROCESSORCLASSES + "?mission=" + loginManager.getMission();
		
		if (!showCommand.getParameters().isEmpty()) {
			// Only processor name allowed as parameter
			requestURI += "&processorName=" + URLEncoder.encode(showCommand.getParameters().get(0).getValue(), Charset.defaultCharset());
		}
		
		/* Get the processor class information from the Processor Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_PROCESSORCLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PROCESSORCLASSES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		if (isVerbose) {
			/* Display the processor class(es) found */
			try {
				CLIUtil.printObject(System.out, resultList, processorClassOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			} 
		} else {
			// Must be a list of processor classes
			for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					Map<?, ?> resultMap = (Map<?, ?>) resultObject;
					System.out.println(resultMap.get("processorName"));
				}
			}
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
		String processorClassFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				processorClassFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
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
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
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
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
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
					URI_PATH_PROCESSORCLASSES + "?mission=" + loginManager.getMission() + "&processorName=" 
							+ URLEncoder.encode(updatedProcessorClass.getProcessorName(), Charset.defaultCharset()),
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PROCESSORCLASS_NOT_FOUND, updatedProcessorClass.getProcessorName());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PROCESSORCLASSES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
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

		/* Compare attributes of database processor class with updated processor class */
		// No modification of ID, version, mission code or processor class name allowed
		if (!updatedProcessorClass.getProductClasses().isEmpty()) { // mandatory, must not be empty
			restProcessorClass.setProductClasses(updatedProcessorClass.getProductClasses());
		}
		
		/* Update processor class using Processor Manager service */
		try {
			restProcessorClass = serviceConnection.patchToService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_PROCESSORCLASSES + "/" + restProcessorClass.getId(),
					restProcessorClass, RestProcessorClass.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(uiMsg(MSG_ID_NOT_MODIFIED));
				return;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PROCESSORCLASS_NOT_FOUND_BY_ID, restProcessorClass.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_PROCESSORCLASS_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PROCESSORCLASSES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
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
					URI_PATH_PROCESSORCLASSES + "?mission=" + loginManager.getMission() + "&processorName=" 
							+ URLEncoder.encode(processorName, Charset.defaultCharset()), 
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PROCESSORCLASS_NOT_FOUND, processorName);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PROCESSORCLASSES, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
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
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PROCESSORCLASS_NOT_FOUND_BY_ID, restProcessorClass.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PROCESSORCLASSES, loginManager.getMission()) :
						e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = uiMsg(MSG_ID_PROCESSORCLASS_DELETE_FAILED, processorName, e.getMessage());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
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
		String processorFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				processorFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
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
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
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
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Set missing attributes to default values where possible */
		if (null == restProcessor.getMissionCode() || 0 == restProcessor.getMissionCode().length()) {
			restProcessor.setMissionCode(loginManager.getMission());
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restProcessor.getProcessorName() || 0 == restProcessor.getProcessorName().length()) {
			System.out.print(PROMPT_PROCESSOR_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProcessor.setProcessorName(response);
		}
		if (null == restProcessor.getProcessorVersion() || 0 == restProcessor.getProcessorVersion().length()) {
			System.out.print(PROMPT_PROCESSOR_VERSION);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProcessor.setProcessorVersion(response);
		}
		if (null == restProcessor.getTasks() || restProcessor.getTasks().isEmpty()) {
			System.out.print(PROMPT_TASKS);
			String response = System.console().readLine();
			if (response.isBlank()) {
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
				if (response.isBlank()) {
					System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
					return;
				}
				restTask.setTaskVersion(response);
				if (1 < tasks.length) {
					while (null == restTask.getCriticalityLevel()) {
						System.out.print(String.format(PROMPT_CRITICALITY_LEVEL, task));
						response = System.console().readLine();
						if (response.isBlank()) {
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
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProcessor.setDockerImage(response);
		}
		
		/* Create processor */
		try {
			restProcessor = serviceConnection.postToService(serviceConfig.getProcessorManagerUrl(), URI_PATH_PROCESSORS, 
					restProcessor, RestProcessor.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_PROCESSOR_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PROCESSORS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}

		/* Report success, giving newly assigned processor ID and version */
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
		
		/* Check command options */
		String processorOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORMAT:
				processorOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
				break;
			}
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_PROCESSORS + "?mission=" + loginManager.getMission();
		
		for (int i = 0; i < showCommand.getParameters().size(); ++i) {
			String paramValue = showCommand.getParameters().get(i).getValue();
			if (0 == i) {
				// First parameter is processor name
				requestURI += "&processorName=" + URLEncoder.encode(paramValue, Charset.defaultCharset());
			} else if (1 == i) {
				// Second parameter is processor version
				requestURI += "&processorVersion=" + URLEncoder.encode(paramValue, Charset.defaultCharset());
			}
		}
		
		/* Get the processor information from the Processor Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_PROCESSORS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PROCESSORS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		if (isVerbose) {
			/* Display the processor class(es) found */
			try {
				CLIUtil.printObject(System.out, resultList, processorOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			} 
		} else {
			// Must be a list of processors
			String listFormat = "%-20s %s";
			System.out.println(String.format(listFormat, "Processor Name", "Version"));
			for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					Map<?, ?> resultMap = (Map<?, ?>) resultObject;
					System.out.println(String.format(listFormat, resultMap.get("processorName"), resultMap.get("processorVersion")));
				}
			}
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
		String processorFileFormat = CLIUtil.FILE_FORMAT_JSON;
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				processorFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				processorFileFormat = option.getValue().toUpperCase();
				break;
			case OPTION_DELETE_ATTRIBUTES:
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
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
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
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
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
					URI_PATH_PROCESSORS + "?mission=" + loginManager.getMission() 
						+ "&processorName=" + URLEncoder.encode(updatedProcessor.getProcessorName(), Charset.defaultCharset()) 
						+ "&processorVersion=" + URLEncoder.encode(updatedProcessor.getProcessorVersion(), Charset.defaultCharset()),
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PROCESSOR_NOT_FOUND, 
						updatedProcessor.getProcessorName(), updatedProcessor.getProcessorVersion());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PROCESSORS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		if (resultList.isEmpty()) {
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
		if (null != updatedProcessor.getJobOrderVersion()) { // not null
			restProcessor.setJobOrderVersion(updatedProcessor.getJobOrderVersion());
		}
		if (null != updatedProcessor.getUseInputFileTimeIntervals()) { // not null
			restProcessor.setUseInputFileTimeIntervals(updatedProcessor.getUseInputFileTimeIntervals());
		}
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
		if (isDeleteAttributes || (null != updatedProcessor.getDockerRunParameters() && !updatedProcessor.getDockerRunParameters().isEmpty())) {
			restProcessor.setDockerRunParameters(updatedProcessor.getDockerRunParameters());
		}
		
		/* Update processor using Processor Manager service */
		try {
			restProcessor = serviceConnection.patchToService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_PROCESSORS + "/" + restProcessor.getId(),
					restProcessor, RestProcessor.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(uiMsg(MSG_ID_NOT_MODIFIED));
				return;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PROCESSOR_NOT_FOUND_BY_ID, restProcessor.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_PROCESSOR_DATA_INVALID,  e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PROCESSORS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
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
					URI_PATH_PROCESSORS + "?mission=" + loginManager.getMission()
						+ "&processorName=" + URLEncoder.encode(processorName, Charset.defaultCharset()) 
						+ "&processorVersion=" + URLEncoder.encode(processorVersion, Charset.defaultCharset()), 
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PROCESSOR_NOT_FOUND, processorName, processorVersion);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PROCESSORS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
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
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PROCESSOR_NOT_FOUND_BY_ID, restProcessor.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PROCESSORS, loginManager.getMission()) :
						e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = uiMsg(MSG_ID_PROCESSOR_DELETE_FAILED, processorName, processorVersion, e.getMessage());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success */
		String message = uiMsg(MSG_ID_PROCESSOR_DELETED, restProcessor.getId());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Create a new configuration; if the input is not from a file, the user will be prompted for mandatory attributes
	 * not given on the command line
	 * 
	 * @param createCommand the parsed "configuration create" command
	 */
	private void createConfiguration(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createConfiguration({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File configurationFile = null;
		String configurationFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				configurationFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				configurationFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read configuration file, if any */
		RestConfiguration restConfiguration = null;
		if (null == configurationFile) {
			restConfiguration = new RestConfiguration();
		} else {
			try {
				restConfiguration = CLIUtil.parseObjectFile(configurationFile, configurationFileFormat, RestConfiguration.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from configuration class file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is processor class name
				restConfiguration.setProcessorName(param.getValue());
			} else if (1 == i) {
				// Second parameter is configuration version
				restConfiguration.setConfigurationVersion(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restConfiguration, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Set missing attributes to default values where possible */
		if (null == restConfiguration.getMissionCode() || 0 == restConfiguration.getMissionCode().length()) {
			restConfiguration.setMissionCode(loginManager.getMission());
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restConfiguration.getProcessorName() || 0 == restConfiguration.getProcessorName().length()) {
			System.out.print(PROMPT_PROCESSOR_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restConfiguration.setProcessorName(response);
		}
		if (null == restConfiguration.getConfigurationVersion() || 0 == restConfiguration.getConfigurationVersion().length()) {
			System.out.print(PROMPT_CONFIGURATION_VERSION);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restConfiguration.setConfigurationVersion(response);
		}
		
		/* Create configuration */
		try {
			restConfiguration = serviceConnection.postToService(serviceConfig.getProcessorManagerUrl(), URI_PATH_CONFIGURATIONS, 
					restConfiguration, RestConfiguration.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_CONFIGURATION_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), CONFIGURATIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}

		/* Report success, giving newly assigned configuration ID and version */
		String message = uiMsg(MSG_ID_CONFIGURATION_CREATED,
				restConfiguration.getProcessorName(), restConfiguration.getConfigurationVersion(), restConfiguration.getId());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Show the configuration specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "configuration show" command
	 */
	private void showConfiguration(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showConfiguration({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Check command options */
		String configurationOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORMAT:
				configurationOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
				break;
			}
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_CONFIGURATIONS + "?mission=" + loginManager.getMission();
		
		for (int i = 0; i < showCommand.getParameters().size(); ++i) {
			String paramValue = showCommand.getParameters().get(i).getValue();
			if (0 == i) {
				// First parameter is processor name
				requestURI += "&processorName=" + URLEncoder.encode(paramValue, Charset.defaultCharset());
			} else if (1 == i) {
				// Second parameter is configuration version
				requestURI += "&configurationVersion=" + URLEncoder.encode(paramValue, Charset.defaultCharset());
			}
		}
		
		/* Get the configuration information from the Processor Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_CONFIGURATIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), CONFIGURATIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		if (isVerbose) {
			/* Display the configuration class(es) found */
			try {
				CLIUtil.printObject(System.out, resultList, configurationOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			} 
		} else {
			// Must be a list of configurations
			String listFormat = "%-20s %s";
			System.out.println(String.format(listFormat, "Processor Name", "Configuration Version"));
			for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					Map<?, ?> resultMap = (Map<?, ?>) resultObject;
					System.out.println(String.format(listFormat, resultMap.get("processorName"), resultMap.get("configurationVersion")));
				}
			}
		}
	}
	
	/**
	 * Update a configuration from a configuration file or from "attribute=value" pairs (overriding any configuration file entries)
	 * 
	 * @param updateCommand the parsed "configuration update" command
	 */
	private void updateConfiguration(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateConfiguration({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File configurationFile = null;
		String configurationFileFormat = CLIUtil.FILE_FORMAT_JSON;
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				configurationFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				configurationFileFormat = option.getValue().toUpperCase();
				break;
			case OPTION_DELETE_ATTRIBUTES:
				isDeleteAttributes = true;
				break;
			}
		}
		
		/* Read configuration file, if any */
		RestConfiguration updatedConfiguration = null;
		if (null == configurationFile) {
			updatedConfiguration = new RestConfiguration();
		} else {
			try {
				updatedConfiguration = CLIUtil.parseObjectFile(configurationFile, configurationFileFormat, RestConfiguration.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from configuration class file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is processor class name
				updatedConfiguration.setProcessorName(param.getValue());
			} else if (1 == i) {
				// Second parameter is configuration version
				updatedConfiguration.setConfigurationVersion(param.getValue());;
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedConfiguration, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Read original configuration from Processor Manager service */
		if (null == updatedConfiguration.getProcessorName() || 0 == updatedConfiguration.getProcessorName().length()
				|| null == updatedConfiguration.getConfigurationVersion() || 0 == updatedConfiguration.getConfigurationVersion().length()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_CONFIGURATION_IDENTIFIER_GIVEN));
			return;
		}
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_CONFIGURATIONS + "?mission=" + loginManager.getMission() 
						+ "&processorName=" + URLEncoder.encode(updatedConfiguration.getProcessorName(), Charset.defaultCharset()) 
						+ "&configurationVersion=" + URLEncoder.encode(updatedConfiguration.getConfigurationVersion(), Charset.defaultCharset()),
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_CONFIGURATION_NOT_FOUND, 
						updatedConfiguration.getProcessorName(), updatedConfiguration.getConfigurationVersion());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), CONFIGURATIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		if (resultList.isEmpty()) {
			String message = uiMsg(MSG_ID_CONFIGURATION_NOT_FOUND, 
					updatedConfiguration.getProcessorName(), updatedConfiguration.getConfigurationVersion());
			logger.error(message);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestConfiguration restConfiguration = mapper.convertValue(resultList.get(0), RestConfiguration.class);

		/* Compare attributes of database configuration with updated configuration */
		// No modification of ID, version, mission code, processor class name, configuration version or configured processors allowed
		if (isDeleteAttributes || (null != updatedConfiguration.getMode() && !updatedConfiguration.getMode().isBlank())) {
			restConfiguration.setMode(updatedConfiguration.getMode());
		}
		if (null != updatedConfiguration.getDynProcParameters() && (isDeleteAttributes || !updatedConfiguration.getDynProcParameters().isEmpty())) {
			restConfiguration.getDynProcParameters().clear();
			restConfiguration.getDynProcParameters().addAll(updatedConfiguration.getDynProcParameters());
		}
		if (isDeleteAttributes || (null != updatedConfiguration.getProductQuality() && !updatedConfiguration.getProductQuality().isBlank())) {
			restConfiguration.setProductQuality(updatedConfiguration.getProductQuality());
		}
		if (null != updatedConfiguration.getConfigurationFiles() && (isDeleteAttributes || !updatedConfiguration.getConfigurationFiles().isEmpty())) {
			restConfiguration.getConfigurationFiles().clear();
			restConfiguration.getConfigurationFiles().addAll(updatedConfiguration.getConfigurationFiles());
		}
		if (null != updatedConfiguration.getStaticInputFiles() && (isDeleteAttributes || !updatedConfiguration.getStaticInputFiles().isEmpty())) {
			restConfiguration.getStaticInputFiles().clear();
			restConfiguration.getStaticInputFiles().addAll(updatedConfiguration.getStaticInputFiles());
		}
		if (isDeleteAttributes || (null != updatedConfiguration.getDockerRunParameters() && !updatedConfiguration.getDockerRunParameters().isEmpty())) {
			restConfiguration.setDockerRunParameters(updatedConfiguration.getDockerRunParameters());
		}
		
		/* Update configuration using Processor Manager service */
		try {
			restConfiguration = serviceConnection.patchToService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_CONFIGURATIONS + "/" + restConfiguration.getId(),
					restConfiguration, RestConfiguration.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(uiMsg(MSG_ID_NOT_MODIFIED));
				return;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_CONFIGURATION_NOT_FOUND_BY_ID, restConfiguration.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_CONFIGURATION_DATA_INVALID,  e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), CONFIGURATIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new configuration version */
		String message = uiMsg(MSG_ID_CONFIGURATION_UPDATED, restConfiguration.getId(), restConfiguration.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Delete the given configuration
	 * 
	 * @param deleteCommand the parsed "configuration delete" command
	 */
	private void deleteConfiguration(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteConfiguration({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get processor name and configuration version from command parameters */
		if (2 > deleteCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_PROCESSOR_IDENTIFIER_GIVEN));
			return;
		}
		String processorName = deleteCommand.getParameters().get(0).getValue();
		String configurationVersion = deleteCommand.getParameters().get(1).getValue();
		
		/* Retrieve the configuration using Processor Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(), 
					URI_PATH_CONFIGURATIONS + "?mission=" + loginManager.getMission()
						+ "&processorName=" + URLEncoder.encode(processorName, Charset.defaultCharset()) 
						+ "&configurationVersion=" + URLEncoder.encode(configurationVersion, Charset.defaultCharset()), 
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_CONFIGURATION_NOT_FOUND, processorName, configurationVersion);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), CONFIGURATIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		if (resultList.isEmpty()) {
			String message = uiMsg(MSG_ID_CONFIGURATION_NOT_FOUND, processorName, configurationVersion);
			logger.error(message);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestConfiguration restConfiguration = mapper.convertValue(resultList.get(0), RestConfiguration.class);
		
		/* Delete configuration using Processor Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_CONFIGURATIONS + "/" + restConfiguration.getId(), 
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_CONFIGURATION_NOT_FOUND_BY_ID, restConfiguration.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), CONFIGURATIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = uiMsg(MSG_ID_CONFIGURATION_DELETE_FAILED, processorName, configurationVersion, e.getMessage());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success */
		String message = uiMsg(MSG_ID_CONFIGURATION_DELETED, restConfiguration.getId());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Create a new configured processor; if the input is not from a file, the user will be prompted for mandatory attributes
	 * not given on the command line
	 * 
	 * @param createCommand the parsed "processor configuration create" command
	 */
	private void createConfiguredProcessor(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createConfiguredProcessor({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File configuredProcessorFile = null;
		String configuredProcessorFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				configuredProcessorFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				configuredProcessorFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read configured processor file, if any */
		RestConfiguredProcessor restConfiguredProcessor = null;
		if (null == configuredProcessorFile) {
			restConfiguredProcessor = new RestConfiguredProcessor();
		} else {
			try {
				restConfiguredProcessor = CLIUtil.parseObjectFile(configuredProcessorFile, configuredProcessorFileFormat, RestConfiguredProcessor.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from configured processor class file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			switch (i) {
			case 0:
				// First parameter is identifier of configured processor
				restConfiguredProcessor.setIdentifier(param.getValue());
				break;
			case 1:
				// Second parameter is processor class name
				restConfiguredProcessor.setProcessorName(param.getValue());
				break;
			case 2:
				// Third parameter is processor version
				restConfiguredProcessor.setProcessorVersion(param.getValue());
				break;
			case 3:
				// Fourth parameter is configuration version
				restConfiguredProcessor.setConfigurationVersion(param.getValue());
				break;
			default:
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restConfiguredProcessor, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Set missing attributes to default values where possible */
		if (null == restConfiguredProcessor.getMissionCode() || 0 == restConfiguredProcessor.getMissionCode().length()) {
			restConfiguredProcessor.setMissionCode(loginManager.getMission());
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restConfiguredProcessor.getIdentifier() || 0 == restConfiguredProcessor.getIdentifier().length()) {
			System.out.print(PROMPT_CONFIGUREDPROCESSOR_IDENTIFIER);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restConfiguredProcessor.setIdentifier(response);
		}
		if (null == restConfiguredProcessor.getProcessorName() || 0 == restConfiguredProcessor.getProcessorName().length()) {
			System.out.print(PROMPT_PROCESSOR_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restConfiguredProcessor.setProcessorName(response);
		}
		if (null == restConfiguredProcessor.getProcessorVersion() || 0 == restConfiguredProcessor.getProcessorVersion().length()) {
			System.out.print(PROMPT_PROCESSOR_VERSION);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restConfiguredProcessor.setProcessorVersion(response);
		}
		if (null == restConfiguredProcessor.getConfigurationVersion() || 0 == restConfiguredProcessor.getConfigurationVersion().length()) {
			System.out.print(PROMPT_CONFIGURATION_VERSION);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restConfiguredProcessor.setConfigurationVersion(response);
		}
		
		/* Create configured processor */
		try {
			restConfiguredProcessor = serviceConnection.postToService(serviceConfig.getProcessorManagerUrl(), URI_PATH_CONFIGUREDPROCESSORS, 
					restConfiguredProcessor, RestConfiguredProcessor.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_CONFIGUREDPROCESSOR_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), CONFIGUREDPROCESSORS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}

		/* Report success, giving newly assigned configured processor ID and version */
		String message = uiMsg(MSG_ID_CONFIGUREDPROCESSOR_CREATED,
				restConfiguredProcessor.getIdentifier(),
				restConfiguredProcessor.getProcessorName(),
				restConfiguredProcessor.getProcessorVersion(),
				restConfiguredProcessor.getConfigurationVersion(),
				restConfiguredProcessor.getId());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Show the configured processor specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "processor configuration show" command
	 */
	private void showConfiguredProcessor(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showConfiguredProcessor({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Check command options */
		String configuredProcessorOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORMAT:
				configuredProcessorOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
				break;
			}
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_CONFIGUREDPROCESSORS + "?mission=" + loginManager.getMission();
		
		for (int i = 0; i < showCommand.getParameters().size(); ++i) {
			String paramValue = showCommand.getParameters().get(i).getValue();
			if (0 == i) {
				// First parameter is configured processor identifier
				requestURI += "&identifier=" + URLEncoder.encode(paramValue, Charset.defaultCharset());
			}
		}
		
		/* Get the configured processor information from the Processor Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_CONFIGUREDPROCESSORS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), CONFIGUREDPROCESSORS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		if (isVerbose) {
			/* Display the configured processor class(es) found */
			try {
				CLIUtil.printObject(System.out, resultList, configuredProcessorOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			} 
		} else {
			// Must be a list of configured processors
			String listFormat = "%-30s %-38s %-20s %-16s %s";
			System.out.println(String.format(listFormat, "Identifier", "UUID", "Processor Name", "Processor Version", "Configuration Version"));
			for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					Map<?, ?> resultMap = (Map<?, ?>) resultObject;
					System.out.println(String.format(listFormat,
							resultMap.get("identifier"),
							resultMap.get("uuid"),
							resultMap.get("processorName"),
							resultMap.get("processorVersion"),
							resultMap.get("configurationVersion")));
				}
			}
		}
	}
	
	/**
	 * Update a configured processor from a configured processor file or from "attribute=value" pairs
	 * (overriding any configured processor file entries)
	 * 
	 * @param updateCommand the parsed "processor configuration update" command
	 */
	private void updateConfiguredProcessor(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateConfiguredProcessor({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File configuredProcessorFile = null;
		String configuredProcessorFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				configuredProcessorFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				configuredProcessorFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read configured processor file, if any */
		RestConfiguredProcessor updatedConfiguredProcessor = null;
		if (null == configuredProcessorFile) {
			updatedConfiguredProcessor = new RestConfiguredProcessor();
		} else {
			try {
				updatedConfiguredProcessor = CLIUtil.parseObjectFile(configuredProcessorFile, configuredProcessorFileFormat, RestConfiguredProcessor.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from configured processor class file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is configured processor identifier
				updatedConfiguredProcessor.setIdentifier(param.getValue());;
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedConfiguredProcessor, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Read original configured processor from Processor Manager service */
		if (null == updatedConfiguredProcessor.getIdentifier() || 0 == updatedConfiguredProcessor.getIdentifier().length()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_CONFIGUREDPROCESSOR_IDENTIFIER_GIVEN));
			return;
		}
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_CONFIGUREDPROCESSORS + "?mission=" + loginManager.getMission() 
						+ "&identifier=" + URLEncoder.encode(updatedConfiguredProcessor.getIdentifier(), Charset.defaultCharset()),
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_CONFIGUREDPROCESSOR_NOT_FOUND, 
						updatedConfiguredProcessor.getIdentifier(), updatedConfiguredProcessor.getProcessorName());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), CONFIGUREDPROCESSORS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		if (resultList.isEmpty()) {
			String message = uiMsg(MSG_ID_CONFIGUREDPROCESSOR_NOT_FOUND, 
					updatedConfiguredProcessor.getIdentifier(), updatedConfiguredProcessor.getProcessorName());
			logger.error(message);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestConfiguredProcessor restConfiguredProcessor = mapper.convertValue(resultList.get(0), RestConfiguredProcessor.class);

		/* Compare attributes of database configured processor with updated configured processor */
		// No modification of ID, version, mission code or processor class name allowed
		if (null != updatedConfiguredProcessor.getIdentifier() && 0 != updatedConfiguredProcessor.getIdentifier().length()) {
			restConfiguredProcessor.setIdentifier(updatedConfiguredProcessor.getIdentifier());
		}
		if (null != updatedConfiguredProcessor.getEnabled()) {
			restConfiguredProcessor.setEnabled(updatedConfiguredProcessor.getEnabled());
		}
		if (null != updatedConfiguredProcessor.getProcessorVersion() && 0 != updatedConfiguredProcessor.getProcessorVersion().length()) {
			restConfiguredProcessor.setProcessorVersion(updatedConfiguredProcessor.getProcessorVersion());
		}
		if (null != updatedConfiguredProcessor.getConfigurationVersion() && 0 != updatedConfiguredProcessor.getConfigurationVersion().length()) {
			restConfiguredProcessor.setConfigurationVersion(updatedConfiguredProcessor.getConfigurationVersion());
		}
		
		/* Update configured processor using Processor Manager service */
		try {
			restConfiguredProcessor = serviceConnection.patchToService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_CONFIGUREDPROCESSORS + "/" + restConfiguredProcessor.getId(),
					restConfiguredProcessor, RestConfiguredProcessor.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(uiMsg(MSG_ID_NOT_MODIFIED));
				return;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_CONFIGUREDPROCESSOR_NOT_FOUND_BY_ID, restConfiguredProcessor.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_CONFIGUREDPROCESSOR_DATA_INVALID,  e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), CONFIGUREDPROCESSORS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new configured processor version */
		String message = uiMsg(MSG_ID_CONFIGUREDPROCESSOR_UPDATED, restConfiguredProcessor.getId(), restConfiguredProcessor.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Delete the given configured processor
	 * 
	 * @param deleteCommand the parsed "processor configuration delete" command
	 */
	private void deleteConfiguredProcessor(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteConfiguredProcessor({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get configured processor identifier from command parameters */
		if (1 > deleteCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_CONFIGUREDPROCESSOR_IDENTIFIER_GIVEN));
			return;
		}
		String identifier = deleteCommand.getParameters().get(0).getValue();
		
		/* Retrieve the configured processor using Processor Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(), 
					URI_PATH_CONFIGUREDPROCESSORS + "?mission=" + loginManager.getMission()
						+ "&identifier=" + URLEncoder.encode(identifier, Charset.defaultCharset()), 
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_CONFIGUREDPROCESSOR_NOT_FOUND, identifier);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), CONFIGUREDPROCESSORS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		if (resultList.isEmpty()) {
			String message = uiMsg(MSG_ID_CONFIGUREDPROCESSOR_NOT_FOUND, identifier);
			logger.error(message);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestConfiguredProcessor restConfiguredProcessor = mapper.convertValue(resultList.get(0), RestConfiguredProcessor.class);
		
		/* Delete configured processor using Processor Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_CONFIGUREDPROCESSORS + "/" + restConfiguredProcessor.getId(), 
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_CONFIGUREDPROCESSOR_NOT_FOUND_BY_ID, restConfiguredProcessor.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = (null == e.getStatusText() ?
						uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), CONFIGUREDPROCESSORS, loginManager.getMission()) :
						e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = uiMsg(MSG_ID_CONFIGUREDPROCESSOR_DELETE_FAILED, identifier, e.getMessage());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success */
		String message = uiMsg(MSG_ID_CONFIGUREDPROCESSOR_DELETED, restConfiguredProcessor.getId());
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
		if (null == loginManager.getUser()) {
			System.err.println(uiMsg(MSG_ID_USER_NOT_LOGGED_IN, command.getName()));
			return;
		}
		if (null == loginManager.getMission()) {
			System.err.println(uiMsg(MSG_ID_USER_NOT_LOGGED_IN_TO_MISSION, command.getName()));
			return;
		}
		
		/* Check argument */
		if (!CMD_PROCESSOR.equals(command.getName()) && !CMD_CONFIGURATION.equals(command.getName())) {
			System.err.println(uiMsg(MSG_ID_INVALID_COMMAND_NAME, command.getName()));
			return;
		}

		/* Make sure a subcommand is given */
		ParsedCommand subcommand = command.getSubcommand();

		if (null == subcommand) {
			System.err.println(uiMsg(MSG_ID_SUBCOMMAND_MISSING, command.getName()));
			return;
		}

		/* Check for subcommand help request */
		if (subcommand.isHelpRequested()) {
			subcommand.getSyntaxCommand().printHelp(System.out);
			return;
		}
				
		/* Make sure a sub-subcommand is given for "class" and "configuration" */
		ParsedCommand subsubcommand = subcommand.getSubcommand();
		if ((CMD_CLASS.equals(subcommand.getName()) || CMD_CONFIGURATION.equals(subcommand.getName()))
				&& null == subcommand.getSubcommand()) {
			System.err.println(uiMsg(MSG_ID_SUBCOMMAND_MISSING, subcommand.getName()));
			return;
		}

		/* Check for sub-subcommand help request */
		if (null != subsubcommand && subsubcommand.isHelpRequested()) {
			subsubcommand.getSyntaxCommand().printHelp(System.out);
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
				case CMD_CREATE:	createConfiguredProcessor(subsubcommand); break COMMAND;
				case CMD_SHOW:		showConfiguredProcessor(subsubcommand); break COMMAND;
				case CMD_UPDATE:	updateConfiguredProcessor(subsubcommand); break COMMAND;
				case CMD_DELETE:	deleteConfiguredProcessor(subsubcommand); break COMMAND;
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
			case CMD_CREATE:	createConfiguration(subcommand); break COMMAND;
			case CMD_SHOW:		showConfiguration(subcommand); break COMMAND;
			case CMD_UPDATE:	updateConfiguration(subcommand); break COMMAND;
			case CMD_DELETE:	deleteConfiguration(subcommand); break COMMAND;
			default:
				System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		}
	}
}
