/**
 * TriggerCommandRunner.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.model.enums.TriggerType;
import de.dlr.proseo.model.rest.model.RestTrigger;
import de.dlr.proseo.model.util.StringUtils;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;
import de.dlr.proseo.ui.cli.parser.ParsedParameter;

/**
 * Run commands for managing prosEO triggers (create, read, update, delete etc.). 
 * All methods assume that before invocation a syntax check of the command has been performed, so no extra checks are performed.
 * 
 * @author Ernst Melchinger
 */
@Component
public class TriggerCommandRunner {

	/* General string constants */
	public static final String CMD_TRIGGER = "trigger";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";
	private static final String CMD_RELOAD = "reload";

	private static final String OPTION_DELETE_ATTRIBUTES = "delete-attributes";
	private static final String OPTION_VERBOSE = "verbose";
	private static final String OPTION_FORMAT = "format";
	private static final String OPTION_FILE = "file";
	

	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_TRIGGER_TYPE = "Trigger type (empty field cancels): ";
	private static final String PROMPT_TRIGGER_NAME = "Trigger name (empty field cancels): ";
	private static final String PROMPT_TRIGGER_CRON_EXP = "Trigger cron expression (empty field cancels): ";
	private static final String PROMPT_TRIGGER_SPACECRAFT = "Trigger spacecarft code (empty field cancels): ";
	private static final String PROMPT_TRIGGER_LAST_ORBIT = "Trigger last orbit number (empty field cancels): ";
	private static final String PROMPT_TRIGGER_DELTA_TIME = "Trigger delta time (empty field cancels): ";
	private static final String PROMPT_TRIGGER_TIMEINTERVAL = "Trigger time interval (empty field cancels): ";
	private static final String PROMPT_WORKFLOW_NAME = "Trigger workflow name (empty field cancels): ";
	private static final String PROMPT_WORKFLOW_VERSION = "Trigger workflow version (empty field cancels): ";
	private static final String URI_PATH_TRIGGERS = "/triggers";
	private static final String URI_PATH_RELOAD = "/reload";
	private static final String TRIGGERS = "triggers";

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
	private static ProseoLogger logger = new ProseoLogger(TriggerCommandRunner.class);
	
	/**
	 * Run the given command
	 *
	 * @param command the command to execute
	 */
	void executeCommand(ParsedCommand command) {
		if (logger.isTraceEnabled())
			logger.trace(">>> executeCommand({})", (null == command ? "null" : command.getName()));

		/* Check that user is logged in */
		if (null == loginManager.getUser()) {
			System.err.println(ProseoLogger.format(UIMessage.USER_NOT_LOGGED_IN, command.getName()));
			return;
		}
		if (null == loginManager.getMission()) {
			System.err.println(ProseoLogger.format(UIMessage.USER_NOT_LOGGED_IN_TO_MISSION, command.getName()));
			return;
		}

		/* Check argument */
		if (!CMD_TRIGGER.equals(command.getName())) {
			System.err.println(ProseoLogger.format(UIMessage.INVALID_COMMAND_NAME, command.getName()));
			return;
		}

		/* Make sure a subcommand is given */
		ParsedCommand subcommand = command.getSubcommand();

		if (null == subcommand) {
			System.err.println(ProseoLogger.format(UIMessage.SUBCOMMAND_MISSING, command.getName()));
			return;
		}

		/* Check for subcommand help request */
		if (subcommand.isHelpRequested()) {
			subcommand.getSyntaxCommand().printHelp(System.out);
			return;
		}

		/* Execute the sub-command */
		COMMAND: if (command.getName().equals(CMD_TRIGGER)) {
			switch (subcommand.getName()) {
			// Handle commands for triggers
			case CMD_CREATE:
				createTrigger(subcommand);
				break COMMAND;
			case CMD_SHOW:
				showTrigger(subcommand);
				break COMMAND;
			case CMD_UPDATE:
				updateTrigger(subcommand);
				break COMMAND;
			case CMD_DELETE:
				deleteTrigger(subcommand);
				break COMMAND;
			case CMD_RELOAD:
				reloadTrigger(subcommand);
				break COMMAND;
			default:
				System.err.println(
						ProseoLogger.format(UIMessage.COMMAND_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}

		}
	}

	/**
	 * Create a new trigger ; if the input is not from a file, the user will be prompted for mandatory attributes not given on the
	 * command line
	 *
	 * @param createCommand the parsed "trigger create" command
	 */
	private void createTrigger(ParsedCommand createCommand) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createTrigger({})", (null == createCommand ? "null" : createCommand.getName()));

		/* Check command options */
		File triggerFile = null;
		String triggerFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option : createCommand.getOptions()) {
			switch (option.getName()) {
			case OPTION_FILE:
				triggerFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				triggerFileFormat = option.getValue().toUpperCase();
				break;
			}
		}

		/* Read trigger file, if any */
		RestTrigger restTrigger = null;
		TriggerType type = null;
		if (null == triggerFile) {
			restTrigger = new RestTrigger();
		} else {
			try {
				restTrigger = CLIUtil.parseObjectFile(triggerFile, triggerFileFormat, RestTrigger.class);
				try {
					type = TriggerType.valueOf(restTrigger.getType());
				} catch (Exception e) {
					// unknown type
					System.err.println(ProseoLogger.format(UIMessage.INVALID_TRIGGER_TYPE, restTrigger.getType()));
					return;
				}
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}

		/* Check command parameters (overriding values from trigger class file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is trigger type
				String val = param.getValue();
				try {
					type = TriggerType.valueOf(val);
				} catch (Exception e) {
					// unknown type
					System.err.println(ProseoLogger.format(UIMessage.INVALID_TRIGGER_TYPE, val));
					return;
				}
				if (type != null) {
					restTrigger.setType(val);
				}
			} else if (1 == i) {
				// Second parameter is trigger name
				restTrigger.setName(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restTrigger, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}

		/* Set missing attributes to default values where possible */
		if (null == restTrigger.getMissionCode() || 0 == restTrigger.getMissionCode().length()) {
			restTrigger.setMissionCode(loginManager.getMission());
		}

		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (StringUtils.isNullOrEmpty(restTrigger.getType())) {
			System.out.print(PROMPT_TRIGGER_TYPE);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			try {
				type = TriggerType.valueOf(response);
			} catch (Exception e) {
				// unknown type
				System.err.println(ProseoLogger.format(UIMessage.INVALID_TRIGGER_TYPE, response));
				return;
			}
			restTrigger.setType(response);
		}
		if (StringUtils.isNullOrEmpty(restTrigger.getName())) {
			System.out.print(PROMPT_TRIGGER_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restTrigger.setName(response);
		}
		if (StringUtils.isNullOrEmpty(restTrigger.getWorkflowName())) {
			System.out.print(PROMPT_WORKFLOW_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restTrigger.setWorkflowName(response);
		}
		if (StringUtils.isNullOrEmpty(restTrigger.getWorkflowVersion())) {
			System.out.print(PROMPT_WORKFLOW_VERSION);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restTrigger.setWorkflowVersion(response);
		}
		switch (type) {
		case Calendar:
			if (StringUtils.isNullOrEmpty(restTrigger.getCronExpression())) {
				System.out.print(PROMPT_TRIGGER_CRON_EXP);
				String response = System.console().readLine();
				if (response.isBlank()) {
					System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
					return;
				}
				restTrigger.setCronExpression(response);
			}
			break;
		case DataDriven:
			// no special mandatory attributes
			break;
		case Datatake:

			break;
		case Orbit:
			if (StringUtils.isNullOrEmpty(restTrigger.getSpacecraftCode())) {
				System.out.print(PROMPT_TRIGGER_SPACECRAFT);
				String response = System.console().readLine();
				if (response.isBlank()) {
					System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
					return;
				}
				restTrigger.setSpacecraftCode(response);
			}
			if (null == restTrigger.getLastOrbitNumber()) {
				System.out.print(PROMPT_TRIGGER_LAST_ORBIT);
				String response = System.console().readLine();
				if (response.isBlank()) {
					System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
					return;
				}
				Long orbitNumber = 0L;
				try {
					orbitNumber = Long.valueOf(response);
				} catch (NumberFormatException e) {
					System.out.println(ProseoLogger.format(UIMessage.INVALID_NUMBER_FORMAT));
					return;					
				}
				restTrigger.setLastOrbitNumber(orbitNumber);
			}
			if (null == restTrigger.getDeltaTime()) {
				System.out.print(PROMPT_TRIGGER_DELTA_TIME);
				String response = System.console().readLine();
				if (response.isBlank()) {
					System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
					return;
				}
				Long deltaTime = 0L;
				try {
					deltaTime = Long.valueOf(response);
				} catch (NumberFormatException e) {
					System.out.println(ProseoLogger.format(UIMessage.INVALID_NUMBER_FORMAT));
					return;					
				}
				restTrigger.setDeltaTime(deltaTime);
			}
			break;
		case TimeInterval:
			if (null == restTrigger.getTriggerInterval()) {
				System.out.print(PROMPT_TRIGGER_TIMEINTERVAL);
				String response = System.console().readLine();
				if (response.isBlank()) {
					System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
					return;
				}
				Long interval = null;
				try {
					interval = Long.valueOf(response);
				} catch (NumberFormatException e) {
					System.out.println(ProseoLogger.format(UIMessage.INVALID_NUMBER_FORMAT));
					return;					
				}
				restTrigger.setTriggerInterval(interval);
			}
			break;
		default:
			break;
		}

		/* Create trigger */
		try {
			restTrigger = serviceConnection.postToService(serviceConfig.getOrderGenUrl(), URI_PATH_TRIGGERS, restTrigger,
					RestTrigger.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.TRIGGER_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), TRIGGERS,
								loginManager.getMission())
						: e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}

		/* Report success, giving newly assigned trigger ID and version */
		String message = logger.log(UIMessage.TRIGGER_CREATED, restTrigger.getName(),
				restTrigger.getType(), restTrigger.getId());
		System.out.println(message);
	}

	/**
	 * Show the trigger specified in the command parameters or options
	 *
	 * @param showCommand the parsed "trigger show" command
	 */
	private void showTrigger(ParsedCommand showCommand) {
		if (logger.isTraceEnabled())
			logger.trace(">>> showTrigger({})", (null == showCommand ? "null" : showCommand.getName()));

		/* Check command options */
		String triggerOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option : showCommand.getOptions()) {
			switch (option.getName()) {
			case OPTION_FORMAT:
				triggerOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
				break;
			}
		}

		/* Prepare request URI */
		String requestURI = URI_PATH_TRIGGERS + "?mission=" + loginManager.getMission();


		/* Check command parameters (overriding values from trigger class file) */
		for (int i = 0; i < showCommand.getParameters().size(); ++i) {
			String paramValue = showCommand.getParameters().get(i).getValue();
			if (0 == i) {
				// First parameter is trigger type
				try {
					TriggerType.valueOf(paramValue);
				} catch (Exception e) {
					// unknown type
					System.err.println(ProseoLogger.format(UIMessage.INVALID_TRIGGER_TYPE, paramValue));
					return;
				}
				requestURI += "&type=" + URLEncoder.encode(paramValue, Charset.defaultCharset());
			} else if (1 == i) {
				// Second parameter is trigger name
				requestURI += "&name=" + URLEncoder.encode(paramValue, Charset.defaultCharset());
			}
		}
		requestURI += "&orderBy=type%20ASC,name%20ASC";

		/* Get the trigger information from the Trigger Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderGenUrl(), requestURI, List.class,
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.NO_TRIGGERS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), TRIGGERS,
								loginManager.getMission())
						: e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}

		if (isVerbose) {
			/* Display the triggers found */
			try {
				CLIUtil.printObject(System.out, resultList, triggerOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		} else {
			// Must be a list of triggers
			String listFormat = "%-16s %-22s %-8s %-22s %-18s";
			System.out
				.println(String.format(listFormat, "Type", "Trigger name", "Priority", "Workflow", "Workflow version"));
			for (Object resultObject : (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					Map<?, ?> resultMap = (Map<?, ?>) resultObject;
					System.out.println(String.format(listFormat, resultMap.get("type"), resultMap.get("name"),
							resultMap.get("priority"), resultMap.get("workflowName"), resultMap.get("workflowVersion")));
				}
			}
		}
	}

	/**
	 * Update a trigger from a trigger file or from "attribute=value" pairs (overriding any trigger file entries)
	 *
	 * @param updateCommand the parsed "trigger update" command
	 */
	private void updateTrigger(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateTrigger({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File triggerFile = null;
		String triggerFileFormat = CLIUtil.FILE_FORMAT_JSON;
		boolean isDeleteAttributes = false;
		for (ParsedOption option : updateCommand.getOptions()) {
			switch (option.getName()) {
			case OPTION_FILE:
				triggerFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				triggerFileFormat = option.getValue().toUpperCase();
				break;
			case OPTION_DELETE_ATTRIBUTES:
				isDeleteAttributes = true;
				break;
			}
		}

		/* Read trigger file, if any */
		RestTrigger updatedTrigger = null;
		TriggerType type = null;
		if (null == triggerFile) {
			updatedTrigger = new RestTrigger();
		} else {
			try {
				updatedTrigger = CLIUtil.parseObjectFile(triggerFile, triggerFileFormat, RestTrigger.class);
				try {
					type = TriggerType.valueOf(updatedTrigger.getType());
				} catch (Exception e) {
					// unknown type
					System.err.println(ProseoLogger.format(UIMessage.INVALID_TRIGGER_TYPE, updatedTrigger.getType()));
					return;
				}
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}

		/* Check command parameters (overriding values from trigger class file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is trigger type
				String val = param.getValue();
				type = null;
				try {
					type= TriggerType.valueOf(val);
				} catch (Exception e) {
					// unknown type
					System.err.println(ProseoLogger.format(UIMessage.INVALID_TRIGGER_TYPE, val));
					return;
				}
				if (type != null) {
					updatedTrigger.setType(val);
				}
			} else if (1 == i) {
				// Second parameter is trigger name
				updatedTrigger.setName(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedTrigger, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}

		/* Read original trigger from Trigger Manager service */
		if (null == updatedTrigger.getName() || 0 == updatedTrigger.getName().length()
				|| null == updatedTrigger.getType() || 0 == updatedTrigger.getType().length()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_TRIGGER_IDENTIFIER_GIVEN));
			return;
		}
		String name = updatedTrigger.getName();
		String typeString = updatedTrigger.getType();
		/* Retrieve the trigger using Trigger Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderGenUrl(),
					URI_PATH_TRIGGERS + "?mission=" + loginManager.getMission() + "&name="
							+ URLEncoder.encode(name, Charset.defaultCharset()) + "&type="
							+ URLEncoder.encode(typeString, Charset.defaultCharset()),
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.TRIGGER_NOT_FOUND, name, typeString);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), TRIGGERS,
								loginManager.getMission())
						: e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		if (resultList.isEmpty()) {
			String message = logger.log(UIMessage.TRIGGER_NOT_FOUND, name, typeString);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestTrigger restTrigger = mapper.convertValue(resultList.get(0), RestTrigger.class);

		/* Update attributes of database trigger */
		// No modification of ID, version, mission code, trigger name or type
		if (null != updatedTrigger.getWorkflowName()) {
			restTrigger.setWorkflowName(updatedTrigger.getWorkflowName());
		}
		if (null != updatedTrigger.getWorkflowVersion()) {
			restTrigger.setWorkflowVersion(updatedTrigger.getWorkflowVersion());
		}
		if (isDeleteAttributes || null != updatedTrigger.getExecutionDelay()) {
			restTrigger.setExecutionDelay(updatedTrigger.getExecutionDelay());
		}
		if (null != updatedTrigger.getPriority()) {
			restTrigger.setPriority(updatedTrigger.getPriority());
		}
		if (isDeleteAttributes || null != updatedTrigger.getNextTriggerTime()) {
			restTrigger.setNextTriggerTime(updatedTrigger.getNextTriggerTime());
		}
		if (null != updatedTrigger.getTriggerInterval()) {
			restTrigger.setTriggerInterval(updatedTrigger.getTriggerInterval());
		}
		if (null != updatedTrigger.getCronExpression()) {
			restTrigger.setCronExpression(updatedTrigger.getCronExpression());
		}
		if (null != updatedTrigger.getSpacecraftCode()) {
			restTrigger.setSpacecraftCode(updatedTrigger.getSpacecraftCode());
		}
		if (null != updatedTrigger.getLastOrbitNumber()) {
			restTrigger.setLastOrbitNumber(updatedTrigger.getLastOrbitNumber());
		}
		if (null != updatedTrigger.getDatatakeType()) {
			restTrigger.setDatatakeType(updatedTrigger.getDatatakeType());
		}
		if (null != updatedTrigger.getLastDatatakeStartTime()) {
			restTrigger.setLastDatatakeStartTime(updatedTrigger.getLastDatatakeStartTime());
		}
		if (null != updatedTrigger.getDeltaTime()) {
			restTrigger.setDeltaTime(updatedTrigger.getDeltaTime());
		}
		if (null != updatedTrigger.getParametersToCopy()) {
			restTrigger.setParametersToCopy(updatedTrigger.getParametersToCopy());
		} else if (isDeleteAttributes) {
			restTrigger.getParametersToCopy().clear();
		}

//
//		/* Update trigger using Trigger Manager service */
//		try {
//			restTrigger = serviceConnection.patchToService(serviceConfig.getProcessorManagerUrl(),
//					URI_PATH_TRIGGERS + "/" + restTrigger.getId(), restTrigger, RestTrigger.class, loginManager.getUser(),
//					loginManager.getPassword());
//		} catch (RestClientResponseException e) {
//			String message = null;
//			switch (e.getStatusCode().value()) {
//			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
//				System.out.println(ProseoLogger.format(UIMessage.NOT_MODIFIED));
//				return;
//			case org.apache.http.HttpStatus.SC_NOT_FOUND:
//				message = ProseoLogger.format(UIMessage.TRIGGER_NOT_FOUND_BY_ID, restTrigger.getId());
//				break;
//			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
//				message = ProseoLogger.format(UIMessage.TRIGGER_DATA_INVALID, e.getStatusText());
//				break;
//			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
//			case org.apache.http.HttpStatus.SC_FORBIDDEN:
//				message = (null == e.getStatusText()
//						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), TRIGGERS,
//								loginManager.getMission())
//						: e.getStatusText());
//				break;
//			default:
//				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
//			}
//			System.err.println(message);
//			return;
//		} catch (RuntimeException e) {
//			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
//			return;
//		}
//
//		/* Report success, giving new trigger version */
//		String message = logger.log(UIMessage.TRIGGER_UPDATED, restTrigger.getId(), restTrigger.getVersion());
//		System.out.println(message);

		try {
			restTrigger = serviceConnection.patchToService(serviceConfig.getOrderGenUrl(), URI_PATH_TRIGGERS, restTrigger,
					RestTrigger.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				/* Report not modified, giving newly assigned trigger ID and version */
				message = logger.log(UIMessage.NOT_MODIFIED);
				System.out.println(message);
				return;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.TRIGGER_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), TRIGGERS,
								loginManager.getMission())
						: e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}

		/* Report success, giving newly assigned trigger ID and version */
		String message = logger.log(UIMessage.TRIGGER_UPDATED, restTrigger.getName(),
				restTrigger.getType(), restTrigger.getVersion());
		System.out.println(message);
	}
	

	/**
	 * Delete the given trigger
	 *
	 * @param deleteCommand the parsed "trigger delete" command
	 */
	private void deleteTrigger(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteTrigger({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get trigger name from command parameters */
		if (2 > deleteCommand.getParameters().size()) {
			// No identifying value given
//			System.err.println(ProseoLogger.format(UIMessage.NO_TRIGGER_IDENTIFIER_GIVEN));
			return;
		}
		String name = deleteCommand.getParameters().get(1).getValue();
		String val = deleteCommand.getParameters().get(0).getValue();
		String type = null;
		TriggerType triggerType = null;
		try {
			triggerType= TriggerType.valueOf(val);
		} catch (Exception e) {
			// unknown type
			System.err.println(ProseoLogger.format(UIMessage.INVALID_TRIGGER_TYPE, val));
			return;
		}
		if (triggerType != null) {
			type = val;
		}

		/* Retrieve the trigger using Trigger Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderGenUrl(),
					URI_PATH_TRIGGERS + "?mission=" + loginManager.getMission() + "&name="
							+ URLEncoder.encode(name, Charset.defaultCharset()) + "&type="
							+ URLEncoder.encode(type, Charset.defaultCharset()),
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.TRIGGER_NOT_FOUND, name, type);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), TRIGGERS,
								loginManager.getMission())
						: e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		if (resultList.isEmpty()) {
			String message = logger.log(UIMessage.TRIGGER_NOT_FOUND, name, type);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestTrigger restTrigger = mapper.convertValue(resultList.get(0), RestTrigger.class);

		/* Delete trigger using Trigger Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getOrderGenUrl(),
					URI_PATH_TRIGGERS + "?mission=" + loginManager.getMission() + "&name="
							+ URLEncoder.encode(name, Charset.defaultCharset()) + "&type="
							+ URLEncoder.encode(type, Charset.defaultCharset()),
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.TRIGGER_NOT_FOUND_BY_ID, restTrigger.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), TRIGGERS,
								loginManager.getMission())
						: e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = ProseoLogger.format(UIMessage.TRIGGER_DELETE_FAILED, name, type, e.getMessage());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}

		/* Report success */
		String message = logger.log(UIMessage.TRIGGER_DELETED, restTrigger.getName());
		System.out.println(message);
	}

	/**
	 * Delete the given trigger
	 *
	 * @param deleteCommand the parsed "trigger delete" command
	 */
	private void reloadTrigger(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled())
			logger.trace(">>> reloadTrigger({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* reload and restart */
		List<?> resultList = null;
		try {
			serviceConnection.getFromService(serviceConfig.getOrderGenUrl(),
					URI_PATH_TRIGGERS + URI_PATH_RELOAD + "?mission=" + loginManager.getMission(),
					List.class,
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), TRIGGERS,
								loginManager.getMission())
						: e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), TRIGGERS,
								loginManager.getMission())
						: e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}

		/* Report success */
		String message = logger.log(UIMessage.TRIGGERS_RELOADED);
		System.out.println(message);
	}

}
