/**
 * WorkflowCommandRunner.java
 * 
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
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
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.model.rest.model.RestWorkflow;
import de.dlr.proseo.model.rest.model.RestWorkflowOption;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;
import de.dlr.proseo.ui.cli.parser.ParsedParameter;

/**
 * Run commands for managing prosEO workflows (create, read, update, delete
 * etc.). All methods assume that before invocation a syntax check of the
 * command has been performed, so no extra checks are performed.
 * 
 * @author Katharina Bassler
 */
@Component
public class WorkflowCommandRunner {

	/* General string constants */
	public static final String CMD_WORKFLOW = "workflow";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";

	private static final String OPTION_DELETE_ATTRIBUTES = "delete-attributes";
	private static final String OPTION_VERBOSE = "verbose";
	private static final String OPTION_FORMAT = "format";
	private static final String OPTION_FILE = "file";

	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_WORKFLOW_NAME = "Workflow class name (empty field cancels): ";
	private static final String PROMPT_WORKFLOW_VERSION = "Workflow version (empty field cancels): ";
	private static final String PROMPT_WORKFLOW_OUTPUT = "Workflow output product class (empty field cancels): ";
	private static final String PROMPT_WORKFLOW_CONFIGURED_PROCESSOR = "Workflow configured processor (empty field cancels): ";
	private static final String PROMPT_WORKFLOW_INPUT = "Workflow input product class (empty field cancels): ";
	private static final String PROMPT_WORKFLOW_OPTIONS = "WorkflowOption names (comma-separated list; empty field cancels): ";
	private static final String PROMPT_WORKFLOW_OPTION_TYPE = "WorkflowOption type (STRING, NUMBER, or DATENUMBER) for %s (empty field cancels): ";
	private static final String PROMPT_WORKFLOW_OPTION_DEFAULT = "WorkflowOption default value for %s (empty field means no default): ";
	private static final String PROMPT_WORKFLOW_OPTION_RANGE = "WorkflowOption value range for %s (comma-separated list): ";

	private static final String URI_PATH_WORKFLOWS = "/workflows";
//	private static final String URI_PATH_WORKFLOW_OPTIONS = "/workflowoptions";

	private static final String WORKFLOWS = "workflows";

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
	private static ProseoLogger logger = new ProseoLogger(WorkflowCommandRunner.class);

	/**
	 * Create a new workflow ; if the input is not from a file, the user will be
	 * prompted for mandatory attributes not given on the command line
	 * 
	 * @param createCommand the parsed "workflow create" command
	 */
	private void createWorkflow(ParsedCommand createCommand) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createWorkflow({})", (null == createCommand ? "null" : createCommand.getName()));

		/* Check command options */
		File workflowFile = null;
		String workflowFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option : createCommand.getOptions()) {
			switch (option.getName()) {
			case OPTION_FILE:
				workflowFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				workflowFileFormat = option.getValue().toUpperCase();
				break;
			}
		}

		/* Read workflow file, if any */
		RestWorkflow restWorkflow = null;
		if (null == workflowFile) {
			restWorkflow = new RestWorkflow();
		} else {
			try {
				restWorkflow = CLIUtil.parseObjectFile(workflowFile, workflowFileFormat, RestWorkflow.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}

		/* Check command parameters (overriding values from workflow class file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is workflow name
				restWorkflow.setName(param.getValue());
			} else if (1 == i) {
				// Second parameter is workflow version
				restWorkflow.setWorkflowVersion(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restWorkflow, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}

		/* Set missing attributes to default values where possible */
		if (null == restWorkflow.getMissionCode() || 0 == restWorkflow.getMissionCode().length()) {
			restWorkflow.setMissionCode(loginManager.getMission());
		}

		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restWorkflow.getName() || 0 == restWorkflow.getName().length()) {
			System.out.print(PROMPT_WORKFLOW_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restWorkflow.setName(response);
		}
		if (null == restWorkflow.getWorkflowVersion() || 0 == restWorkflow.getWorkflowVersion().length()) {
			System.out.print(PROMPT_WORKFLOW_VERSION);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restWorkflow.setWorkflowVersion(response);
		}
		if (null == restWorkflow.getConfiguredProcessor() || 0 == restWorkflow.getConfiguredProcessor().length()) {
			System.out.print(PROMPT_WORKFLOW_CONFIGURED_PROCESSOR);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restWorkflow.setConfiguredProcessor(response);
		}
		if (null == restWorkflow.getInputProductClass() || 0 == restWorkflow.getInputProductClass().length()) {
			System.out.print(PROMPT_WORKFLOW_INPUT);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restWorkflow.setInputProductClass(response);
		}
		if (null == restWorkflow.getOutputProductClass() || 0 == restWorkflow.getOutputProductClass().length()) {
			System.out.print(PROMPT_WORKFLOW_OUTPUT);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restWorkflow.setOutputProductClass(response);
		}
		if (null == restWorkflow.getWorkflowOptions() || restWorkflow.getWorkflowOptions().isEmpty()) {
			System.out.print(PROMPT_WORKFLOW_OPTIONS);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			String[] workflowOptions = response.split(",");
			for (String workflowOption : workflowOptions) {
				RestWorkflowOption restWorkflowOption = new RestWorkflowOption();
				restWorkflowOption.setName(workflowOption);

				/* Set mission and workflow name */
				restWorkflowOption.setMissionCode(loginManager.getMission());
				restWorkflowOption.setWorkflowName(restWorkflow.getName());

				/* Prompt user for workflow option attributes */
				System.out.print(String.format(PROMPT_WORKFLOW_OPTION_TYPE, workflowOption));
				response = System.console().readLine();
				if (response.isBlank()) {
					System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
					return;
				}
				restWorkflowOption.setOptionType(response);

				System.out.print(String.format(PROMPT_WORKFLOW_OPTION_DEFAULT, workflowOption));
				response = System.console().readLine();
				if (response.isBlank()) {
					response = null;
				}
				restWorkflowOption.setDefaultValue(response);

				System.out.print(String.format(PROMPT_WORKFLOW_OPTION_RANGE, workflowOption));
				response = System.console().readLine();
				if (!response.isBlank()) {
					String[] range = response.split(",");
					for (String element : range) {
						restWorkflowOption.getValueRange().add(element);
					}
				}

				restWorkflow.getWorkflowOptions().add(restWorkflowOption);
			}
		}
		
		/* Create workflow */
		try {
			restWorkflow = serviceConnection.postToService(serviceConfig.getProcessorManagerUrl(), URI_PATH_WORKFLOWS,
					restWorkflow, RestWorkflow.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.WORKFLOW_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), WORKFLOWS,
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

		/* Report success, giving newly assigned workflow ID and version */
		String message = logger.log(UIMessage.WORKFLOW_CREATED, restWorkflow.getName(),
				restWorkflow.getWorkflowVersion(), restWorkflow.getId());
		System.out.println(message);
	}

	/**
	 * Show the workflow specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "workflow show" command
	 */
	private void showWorkflow(ParsedCommand showCommand) {
		if (logger.isTraceEnabled())
			logger.trace(">>> showWorkflow({})", (null == showCommand ? "null" : showCommand.getName()));

		/* Check command options */
		String workflowOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option : showCommand.getOptions()) {
			switch (option.getName()) {
			case OPTION_FORMAT:
				workflowOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
				break;
			}
		}

		/* Prepare request URI */
		String requestURI = URI_PATH_WORKFLOWS + "?mission=" + loginManager.getMission();

		for (int i = 0; i < showCommand.getParameters().size(); ++i) {
			String paramValue = showCommand.getParameters().get(i).getValue();
			if (0 == i) {
				// First parameter is workflow name
				requestURI += "&workflowName=" + URLEncoder.encode(paramValue, Charset.defaultCharset());
			} else if (1 == i) {
				// Second parameter is workflow version
				requestURI += "&workflowVersion=" + URLEncoder.encode(paramValue, Charset.defaultCharset());
			} else if (2 == i) {
				// Third parameter is output product class
				requestURI += "&outputProductClass=" + URLEncoder.encode(paramValue, Charset.defaultCharset());
			} else if (3 == i) {
				// Fourth parameter is configured processor
				requestURI += "&configuredProcessor=" + URLEncoder.encode(paramValue, Charset.defaultCharset());
			}
		}

		/* Get the workflow information from the Workflow Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(), requestURI,
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.NO_WORKFLOWS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), WORKFLOWS,
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
			/* Display the workflows found */
			try {
				CLIUtil.printObject(System.out, resultList, workflowOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		} else {
			// Must be a list of workflows
			String listFormat = "%s %s %s %s";
			System.out.println(String.format(listFormat, "Workflow name", "Version", "Output product class",
					"Configured processor"));
			for (Object resultObject : (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					Map<?, ?> resultMap = (Map<?, ?>) resultObject;
					System.out.println(
							String.format(listFormat, resultMap.get("workflowName"), resultMap.get("workflowVersion"),
									resultMap.get("outputProductClass"), resultMap.get("configuredProcessor")));
				}
			}
		}
	}

	/**
	 * Update a workflow from a workflow file or from "attribute=value" pairs
	 * (overriding any workflow file entries)
	 * 
	 * @param updateCommand the parsed "workflow update" command
	 */
	private void updateWorkflow(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateWorkflow({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File workflowFile = null;
		String workflowFileFormat = CLIUtil.FILE_FORMAT_JSON;
		boolean isDeleteAttributes = false;
		for (ParsedOption option : updateCommand.getOptions()) {
			switch (option.getName()) {
			case OPTION_FILE:
				workflowFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				workflowFileFormat = option.getValue().toUpperCase();
				break;
			case OPTION_DELETE_ATTRIBUTES:
				isDeleteAttributes = true;
				break;
			}
		}

		/* Read workflow file, if any */
		RestWorkflow updatedWorkflow = null;
		if (null == workflowFile) {
			updatedWorkflow = new RestWorkflow();
		} else {
			try {
				updatedWorkflow = CLIUtil.parseObjectFile(workflowFile, workflowFileFormat, RestWorkflow.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}

		/* Check command parameters (overriding values from workflow class file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is workflow class name
				updatedWorkflow.setName(param.getValue());
			} else if (1 == i) {
				// Second parameter is workflow version
				updatedWorkflow.setWorkflowVersion(param.getValue());
				;
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedWorkflow, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}

		/* Read original workflow from Workflow Manager service */
		if (null == updatedWorkflow.getName() || 0 == updatedWorkflow.getName().length()
				|| null == updatedWorkflow.getWorkflowVersion() || 0 == updatedWorkflow.getWorkflowVersion().length()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_WORKFLOW_IDENTIFIER_GIVEN));
			return;
		}
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_WORKFLOWS + "?mission=" + loginManager.getMission() + "&workflowName="
							+ URLEncoder.encode(updatedWorkflow.getName(), Charset.defaultCharset())
							+ "&workflowVersion="
							+ URLEncoder.encode(updatedWorkflow.getWorkflowVersion(), Charset.defaultCharset()),
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.WORKFLOW_NOT_FOUND, updatedWorkflow.getName(),
						updatedWorkflow.getWorkflowVersion());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), WORKFLOWS,
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
		if (resultList.isEmpty()) {
			String message = logger.log(UIMessage.WORKFLOW_NOT_FOUND, updatedWorkflow.getName(),
					updatedWorkflow.getWorkflowVersion());
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestWorkflow restWorkflow = mapper.convertValue(resultList.get(0), RestWorkflow.class);

		/* Update attributes of database workflow */
		// No modification of ID, version, mission code, workflow name or uuid
		if (null != updatedWorkflow.getWorkflowVersion()) {
			restWorkflow.setWorkflowVersion(updatedWorkflow.getWorkflowVersion());
		}
		if (null != updatedWorkflow.getConfiguredProcessor()) {
			restWorkflow.setConfiguredProcessor(updatedWorkflow.getConfiguredProcessor());
		}
		if (null != updatedWorkflow.getInputProductClass()) {
			restWorkflow.setInputProductClass(updatedWorkflow.getInputProductClass());
		}
		if (null != updatedWorkflow.getOutputProductClass()) {
			restWorkflow.setOutputProductClass(updatedWorkflow.getOutputProductClass());
		}
		if (isDeleteAttributes
				|| (null != updatedWorkflow.getWorkflowOptions() && !updatedWorkflow.getWorkflowOptions().isEmpty())) {
			restWorkflow.getWorkflowOptions().clear();
			restWorkflow.getWorkflowOptions().addAll(updatedWorkflow.getWorkflowOptions());
		}

		/* Update workflow using Workflow Manager service */
		try {
			restWorkflow = serviceConnection.patchToService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_WORKFLOWS + "/" + restWorkflow.getId(), restWorkflow, RestWorkflow.class,
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(ProseoLogger.format(UIMessage.NOT_MODIFIED));
				return;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.WORKFLOW_NOT_FOUND_BY_ID, restWorkflow.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.WORKFLOW_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), WORKFLOWS,
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

		/* Report success, giving new workflow version */
		String message = logger.log(UIMessage.WORKFLOW_UPDATED, restWorkflow.getId(), restWorkflow.getVersion());
		System.out.println(message);
	}

	/**
	 * Delete the given workflow
	 * 
	 * @param deleteCommand the parsed "workflow delete" command
	 */
	private void deleteWorkflow(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteWorkflow({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get workflow name from command parameters */
		if (2 > deleteCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_WORKFLOW_IDENTIFIER_GIVEN));
			return;
		}
		String workflowName = deleteCommand.getParameters().get(0).getValue();
		String workflowVersion = deleteCommand.getParameters().get(1).getValue();

		/* Retrieve the workflow using Workflow Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_WORKFLOWS + "?mission=" + loginManager.getMission() + "&workflowName="
							+ URLEncoder.encode(workflowName, Charset.defaultCharset()) + "&workflowVersion="
							+ URLEncoder.encode(workflowVersion, Charset.defaultCharset()),
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.WORKFLOW_NOT_FOUND, workflowName, workflowVersion);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), WORKFLOWS,
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
			String message = logger.log(UIMessage.WORKFLOW_NOT_FOUND, workflowName, workflowVersion);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestWorkflow restWorkflow = mapper.convertValue(resultList.get(0), RestWorkflow.class);

		/* Delete workflow using Workflow Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_WORKFLOWS + "/" + restWorkflow.getId(), loginManager.getUser(),
					loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.WORKFLOW_NOT_FOUND_BY_ID, restWorkflow.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = (null == e.getStatusText()
						? ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), WORKFLOWS,
								loginManager.getMission())
						: e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = ProseoLogger.format(UIMessage.WORKFLOW_DELETE_FAILED, workflowName, workflowVersion,
						e.getMessage());
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
		String message = logger.log(UIMessage.WORKFLOW_DELETED, restWorkflow.getId());
		System.out.println(message);
	}

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
		if (!CMD_WORKFLOW.equals(command.getName())) {
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
		COMMAND: if (command.getName().equals(CMD_WORKFLOW)) {
			switch (subcommand.getName()) {
			// Handle commands for workflows
			case CMD_CREATE:
				createWorkflow(subcommand);
				break COMMAND;
			case CMD_SHOW:
				showWorkflow(subcommand);
				break COMMAND;
			case CMD_UPDATE:
				updateWorkflow(subcommand);
				break COMMAND;
			case CMD_DELETE:
				deleteWorkflow(subcommand);
				break COMMAND;
			default:
				System.err.println(ProseoLogger.format(UIMessage.COMMAND_NOT_IMPLEMENTED,
						command.getName() + " " + subcommand.getName()));
				return;
			}

		}
	}
}
