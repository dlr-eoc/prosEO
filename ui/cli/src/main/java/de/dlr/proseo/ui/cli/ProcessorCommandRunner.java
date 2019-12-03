/**
 * ProcessorCommandRunner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.rest.model.RestProcessorClass;
import de.dlr.proseo.model.rest.model.RestProduct;
import de.dlr.proseo.ui.backend.BackendConfiguration;
import de.dlr.proseo.ui.backend.BackendConnectionService;
import de.dlr.proseo.ui.backend.BackendUserManager;
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

	/* Message ID constants */
	// Same as in OrderCommandRunner
	private static final int MSG_ID_INVALID_COMMAND_NAME = 2930;
	private static final int MSG_ID_SUBCOMMAND_MISSING = 2931;
	private static final int MSG_ID_USER_NOT_LOGGED_IN = 2932;
	private static final int MSG_ID_NOT_AUTHORIZED = 2933;
	private static final int MSG_ID_OPERATION_CANCELLED = 2934;
	
	// Specific to ProcessorCommandRunner
	private static final int MSG_ID_NO_PROCESSORCLASSES_FOUND = 2960;
	private static final int MSG_ID_PROCESSORCLASS_CREATED = 2961;
	private static final int MSG_ID_INVALID_DATABASE_ID = 2962;
	private static final int MSG_ID_NO_PROCCLASS_IDENTIFIER_GIVEN = 2963;
	private static final int MSG_ID_PROCESSORCLASS_NOT_FOUND = 2964;
	private static final int MSG_ID_PROCESSORCLASS_UPDATED = 2965;
	private static final int MSG_ID_PROCESSORCLASS_DELETED = 2966;
	private static final int MSG_ID_INGESTION_FILE_MISSING = 2967;
	private static final int MSG_ID_PROCESSING_FACILITY_MISSING = 2968;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;

	/* Message string constants */
	private static final String MSG_INVALID_COMMAND_NAME = "(E%d) Invalid command name %s";
	private static final String MSG_SUBCOMMAND_MISSING = "(E%d) Subcommand missing for command %s";
	private static final String MSG_USER_NOT_LOGGED_IN = "(E%d) User not logged in";
	private static final String MSG_NOT_AUTHORIZED = "(E%d) User %s not authorized to manage orders for mission %s";
	private static final String MSG_NO_PROCESSORCLASSES_FOUND = "(E%d) No products found for given search criteria";
	private static final String MSG_INVALID_DATABASE_ID = "(E%d) Database ID %s not numeric";
	private static final String MSG_NO_PROCCLASS_IDENTIFIER_GIVEN = "(E%d) No processor class name given";
	private static final String MSG_PROCESSORCLASS_NOT_FOUND = "(E%d) Processor class %d not found";
	private static final String MSG_INGESTION_FILE_MISSING = "(E%d) No file for product ingestion given";
	private static final String MSG_PROCESSING_FACILITY_MISSING = "(E%d) No processing facility to ingest to given";
	private static final String MSG_NOT_IMPLEMENTED = "(E%d) Command %s not implemented";

	private static final String MSG_OPERATION_CANCELLED = "(I%d) Operation cancelled";
	private static final String MSG_PROCESSORCLASS_CREATED = "(I%d) Processor class %s created (database ID %d)";
	private static final String MSG_PROCESSORCLASS_UPDATED = "(I%d) Processor class with database ID %d updated (new version %d)";
	private static final String MSG_PROCESSORCLASS_DELETED = "(I%d) Processor class with database ID %d deleted";

	/* Other string constants */
	public static final String CMD_PROCESSOR = "processor";
	private static final String CMD_CLASS = "class";
	private static final String CMD_CONFIGURATION = "configuration";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";

	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_PROCESSOR_NAME = "Processor name (empty field cancels): ";
	private static final String PROMPT_PRODUCT_CLASSES = "Processible product classes (comma-separated list; empty field cancels): ";
	
	private static final String URI_PATH_PROCESSORCLASSES = "/processorclasses";
	private static final String URI_PATH_PROCESSORS = "/processors";
	private static final String URI_PATH_CONFIGURATIONS = "/configurations";
	private static final String URI_PATH_CONFIGUREDPROCESSORS = "/configuredprocessors";
	
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss").withZone(ZoneId.of("UTC"));

	/** The configuration object for the prosEO User Interface */
	@Autowired
	private BackendConfiguration backendConfig;
	
	/** The user manager used by all command runners */
	@Autowired
	private BackendUserManager backendUserMgr;
	
	/** The connector service to the prosEO backend services prosEO */
	@Autowired
	private BackendConnectionService backendConnector;
	
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
			restProcessorClass.setMissionCode(backendUserMgr.getMission());
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restProcessorClass.getProcessorName() || 0 == restProcessorClass.getProcessorName().length()) {
			System.out.print(PROMPT_PROCESSOR_NAME);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProcessorClass.setProcessorName(response);
		}
		if (null == restProcessorClass.getProductClasses() || restProcessorClass.getProductClasses().isEmpty()) {
			System.out.print(PROMPT_PRODUCT_CLASSES);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(String.format(MSG_OPERATION_CANCELLED, MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProcessorClass.setProductClasses(Arrays.asList(response.split(",")));
		}
		
		/* Create product */
		try {
			restProcessorClass = backendConnector.postToService(backendConfig.getProcessorManagerUrl(), URI_PATH_PROCESSORCLASSES, 
					restProcessorClass, RestProcessorClass.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}

		/* Report success, giving newly assigned product ID and UUID */
		String message = String.format(MSG_PROCESSORCLASS_CREATED, MSG_ID_PROCESSORCLASS_CREATED,
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
		String requestURI = URI_PATH_PROCESSORCLASSES + "?mission=" + backendUserMgr.getMission();
		
		if (!showCommand.getParameters().isEmpty()) {
			// Only processor name allowed as parameter
			requestURI += "&processorName=" + showCommand.getParameters().get(0).getValue();
		}
		
		/* Get the processor class information from the Processor Manager service */
		List<?> resultList = null;
		try {
			resultList = backendConnector.getFromService(backendConfig.getProcessorManagerUrl(),
					requestURI, List.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_NO_PROCESSORCLASSES_FOUND, MSG_ID_NO_PROCESSORCLASSES_FOUND);
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
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
		RestProcessorClass restProcessorClass = null;
		if (null == updatedProcessorClass.getProcessorName() || 0 == updatedProcessorClass.getProcessorName().length()) {
			// No identifying value given
			System.err.println(String.format(MSG_NO_PROCCLASS_IDENTIFIER_GIVEN, MSG_ID_NO_PROCCLASS_IDENTIFIER_GIVEN));
			return;
		}
		try {
			restProcessorClass = backendConnector.getFromService(backendConfig.getIngestorUrl(),
					URI_PATH_PROCESSORCLASSES + "?mission=" + backendUserMgr.getMission() + "&processorName=" + updatedProcessorClass.getProcessorName(),
					RestProcessorClass.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_PROCESSORCLASS_NOT_FOUND, MSG_ID_PROCESSORCLASS_NOT_FOUND, restProcessorClass.getId());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}

		/* Compare attributes of database product with updated product */
		// No modification of ID, version, mission code or processor class name allowed
		if (!updatedProcessorClass.getProductClasses().isEmpty()) { // mandatory, must not be empty
			restProcessorClass.setProductClasses(updatedProcessorClass.getProductClasses());
		}
		
		/* Update processor class using Processor Manager service */
		try {
			restProcessorClass = backendConnector.patchToService(backendConfig.getProcessorManagerUrl(),
					URI_PATH_PROCESSORCLASSES + "/" + restProcessorClass.getId(),
					restProcessorClass, RestProcessorClass.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_PROCESSORCLASS_NOT_FOUND, MSG_ID_PROCESSORCLASS_NOT_FOUND, restProcessorClass.getId());
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (RuntimeException e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success, giving new processor class version */
		String message = String.format(MSG_PROCESSORCLASS_UPDATED, MSG_ID_PROCESSORCLASS_UPDATED, restProcessorClass.getId(), restProcessorClass.getVersion());
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
			System.err.println(String.format(MSG_NO_PROCCLASS_IDENTIFIER_GIVEN, MSG_ID_NO_PROCCLASS_IDENTIFIER_GIVEN));
			return;
		}
		String processorName = deleteCommand.getParameters().get(0).getValue();
		
		/* Retrieve the processor class using Processor Manager service */
		RestProcessorClass restProcessorClass = null;
		try {
			restProcessorClass = backendConnector.getFromService(backendConfig.getProcessorManagerUrl(), 
					URI_PATH_PROCESSORCLASSES + "?mission=" + backendUserMgr.getMission() + "&processorName=" + processorName, 
					RestProcessorClass.class, backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_PROCESSORCLASS_NOT_FOUND, MSG_ID_PROCESSORCLASS_NOT_FOUND, processorName);
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (Exception e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Delete processor class using Processor Manager service */
		try {
			backendConnector.deleteFromService(backendConfig.getProcessorManagerUrl(),
					URI_PATH_PROCESSORCLASSES + "/" + restProcessorClass.getId(), 
					backendUserMgr.getUser(), backendUserMgr.getPassword());
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_PROCESSORCLASS_NOT_FOUND, MSG_ID_PROCESSORCLASS_NOT_FOUND, processorName);
			logger.error(message);
			System.err.println(message);
			return;
		} catch (HttpClientErrorException.Unauthorized e) {
			// Already logged
			System.err.println(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED,  backendUserMgr.getUser(), backendUserMgr.getMission()));
			return;
		} catch (Exception e) {
			// Already logged
			System.err.println(e.getMessage());
			return;
		}
		
		/* Report success */
		String message = String.format(MSG_PROCESSORCLASS_DELETED, MSG_ID_PROCESSORCLASS_DELETED, restProcessorClass.getId());
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
		if (null == backendUserMgr.getUser()) {
			System.err.println(String.format(MSG_USER_NOT_LOGGED_IN, MSG_ID_USER_NOT_LOGGED_IN, command.getName()));
		}
		
		/* Check argument */
		if (!CMD_PROCESSOR.equals(command.getName()) && !CMD_CONFIGURATION.equals(command.getName())) {
			System.err.println(String.format(MSG_INVALID_COMMAND_NAME, MSG_ID_INVALID_COMMAND_NAME, command.getName()));
			return;
		}
		
		/* Make sure a subcommand is given */
		ParsedCommand subcommand = command.getSubcommand();
		if (null == subcommand 
				|| ((CMD_CLASS.equals(subcommand.getName()) || CMD_CONFIGURATION.equals(subcommand.getName()))
						&& null == subcommand.getSubcommand())) {
			System.err.println(String.format(MSG_SUBCOMMAND_MISSING, MSG_ID_SUBCOMMAND_MISSING, command.getName()));
			return;
		}
		ParsedCommand subsubcommand = subcommand.getSubcommand();
		
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
					System.err.println(String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, 
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
					System.err.println(String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, 
							command.getName() + " " + subcommand.getName() + " " + subsubcommand.getName()));
					return;
				}
			// Handle commands for processors
//			case CMD_CREATE:	createProcessor(subcommand); break COMMAND;
//			case CMD_SHOW:		showProcessor(subcommand); break COMMAND;
//			case CMD_UPDATE:	updateProcessor(subcommand); break COMMAND;
//			case CMD_DELETE:	deleteProcessor(subcommand); break COMMAND;
			default:
				System.err.println(String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, 
						command.getName() + " " + subcommand.getName()));
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
				System.err.println(String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, 
						command.getName() + " " + subcommand.getName()));
				return;
			}
		}
	}
}
