/**
 * ProductclassCommandRunner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.rest.model.RestProductClass;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;
import de.dlr.proseo.ui.cli.parser.ParsedParameter;

/**
 * Run commands for managing prosEO product classes and selection rules (create, read, update, delete etc.). 
 * All methods assume that before invocation a syntax check of the command has been performed, so no extra checks are performed.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class ProductclassCommandRunner {

	/* General string constants */
	public static final String CMD_PRODUCTCLASS = "productclass";
	private static final String CMD_RULE = "rule";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";

	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_PRODUCT_TYPE = "Product class name (empty field cancels): ";
	private static final String PROMPT_MISSION_TYPE = "Mission product type (empty field cancels): ";

	private static final String URI_PATH_PRODUCTCLASSES = "/productclasses";
	
	private static final String PRODUCTCLASSES = "product classes";

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
	 * Create a new product class; if the input is not from a file, the user will be prompted for mandatory attributes
	 * not given on the command line
	 * 
	 * @param createCommand the parsed "productclass create" command
	 */
	private void createProductClass(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProductClass({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File productClassFile = null;
		String productClassFileFormat = null;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				productClassFile = new File(option.getValue());
				break;
			case "format":
				productClassFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read product class file, if any */
		RestProductClass restProductClass = null;
		
		if (null == productClassFile) {
			restProductClass = new RestProductClass();
		} else {
			try {
				restProductClass = CLIUtil.parseObjectFile(productClassFile, productClassFileFormat, RestProductClass.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from product class file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is product class name
				restProductClass.setProductType(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restProductClass, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Set missing attributes to default values where possible */
		if (null == restProductClass.getMissionCode() || 0 == restProductClass.getMissionCode().length()) {
			restProductClass.setMissionCode(loginManager.getMission());
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restProductClass.getProductType() || 0 == restProductClass.getProductType().length()) {
			System.out.print(PROMPT_PRODUCT_TYPE);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProductClass.setProductType(response);
		}
		if (null == restProductClass.getMissionType() || restProductClass.getMissionType().isEmpty()) {
			System.out.print(PROMPT_MISSION_TYPE);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restProductClass.setMissionType(response);
		}
		
		/* Create product class */
		try {
			restProductClass = serviceConnection.postToService(serviceConfig.getProductClassManagerUrl(), URI_PATH_PRODUCTCLASSES, 
					restProductClass, RestProductClass.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_PRODUCTCLASS_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission());
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

		/* Report success, giving newly assigned product class ID */
		String message = uiMsg(MSG_ID_PRODUCTCLASS_CREATED,
				restProductClass.getProductType(), restProductClass.getId());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Show the product class specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "productclass show" command
	 */
	private void showProductClass(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showProductClass({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Check command options */
		String productClassOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case "format":
				productClassOutputFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_PRODUCTCLASSES + "?mission=" + loginManager.getMission();
		
		if (!showCommand.getParameters().isEmpty()) {
			// Only product class name allowed as parameter
			requestURI += "&productType=" + showCommand.getParameters().get(0).getValue();
		}
		
		/* Get the product class information from the Product Class Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_PRODUCTCLASSES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission());
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
		
		/* Display the product class(es) found */
		try {
			CLIUtil.printObject(System.out, resultList, productClassOutputFormat);
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			return;
		} catch (IOException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
	}
	
	/**
	 * Update a product class from a product class file or from "attribute=value" pairs (overriding any product class file entries)
	 * 
	 * @param updateCommand the parsed "productclass update" command
	 */
	private void updateProductClass(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateProductClass({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File productClassFile = null;
		String productClassFileFormat = null;
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				productClassFile = new File(option.getValue());
				break;
			case "format":
				productClassFileFormat = option.getValue().toUpperCase();
				break;
			case "delete-attributes":
				isDeleteAttributes = true;
				break;
			}
		}
		
		/* Read product class file, if any */
		RestProductClass updatedProductClass = null;
		if (null == productClassFile) {
			updatedProductClass = new RestProductClass();
		} else {
			try {
				updatedProductClass = CLIUtil.parseObjectFile(productClassFile, productClassFileFormat, RestProductClass.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from product class file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is product class name
				updatedProductClass.setProductType(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedProductClass, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Read original product class from Product Manager service */
		if (null == updatedProductClass.getProductType() || 0 == updatedProductClass.getProductType().length()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_PRODCLASS_IDENTIFIER_GIVEN));
			return;
		}
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(),
					URI_PATH_PRODUCTCLASSES + "?mission=" + loginManager.getMission() + "&productType=" + updatedProductClass.getProductType(),
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PRODUCTCLASS_NOT_FOUND, updatedProductClass.getProductType());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission());
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
			String message = uiMsg(MSG_ID_PRODUCTCLASS_NOT_FOUND, updatedProductClass.getProductType());
			logger.error(message);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestProductClass restProductClass = mapper.convertValue(resultList.get(0), RestProductClass.class);

		/* Compare attributes of database product class with updated product class */
		// No modification of ID, version, mission code or product class name allowed
		if (null != updatedProductClass.getMissionType() && 0 != updatedProductClass.getMissionType().length()) { // mandatory, must not be empty
			restProductClass.setMissionType(updatedProductClass.getMissionType());
		}
		if (isDeleteAttributes || (null != updatedProductClass.getTypeDescription() && 0 != updatedProductClass.getTypeDescription().length())) {
			restProductClass.setTypeDescription(updatedProductClass.getTypeDescription());
		}
		if (isDeleteAttributes || (null != updatedProductClass.getComponentClasses() && !updatedProductClass.getComponentClasses().isEmpty())) {
			restProductClass.getComponentClasses().clear();
			restProductClass.getComponentClasses().addAll(updatedProductClass.getComponentClasses());
		}
		if (isDeleteAttributes || (null != updatedProductClass.getEnclosingClass() && 0 != updatedProductClass.getEnclosingClass().length())) {
			restProductClass.setEnclosingClass(updatedProductClass.getEnclosingClass());
		}
		if (isDeleteAttributes || (null != updatedProductClass.getProcessorClass() && 0 != updatedProductClass.getProcessorClass().length())) {
			restProductClass.setProcessorClass(updatedProductClass.getProcessorClass());
		}
		
		/* Update product class using Product Class Manager service */
		try {
			restProductClass = serviceConnection.patchToService(serviceConfig.getProductClassManagerUrl(),
					URI_PATH_PRODUCTCLASSES + "/" + restProductClass.getId(),
					restProductClass, RestProductClass.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PRODUCTCLASS_NOT_FOUND_BY_ID, restProductClass.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_PROCESSORCLASS_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission());
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
		
		/* Report success, giving new product class version */
		String message = uiMsg(MSG_ID_PRODUCTCLASS_UPDATED, restProductClass.getId(), restProductClass.getVersion());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Delete the given product class
	 * 
	 * @param deleteCommand the parsed "productclass delete" command
	 */
	private void deleteProductClass(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductClass({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get product class name from command parameters */
		if (deleteCommand.getParameters().isEmpty()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_PRODCLASS_IDENTIFIER_GIVEN));
			return;
		}
		String productType = deleteCommand.getParameters().get(0).getValue();
		
		/* Retrieve the product class using Product Class Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProductClassManagerUrl(), 
					URI_PATH_PRODUCTCLASSES + "?mission=" + loginManager.getMission() + "&productType=" + productType, 
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PRODUCTCLASS_NOT_FOUND, productType);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission());
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
			String message = uiMsg(MSG_ID_PRODUCTCLASS_NOT_FOUND, productType);
			logger.error(message);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestProductClass restProductClass = mapper.convertValue(resultList.get(0), RestProductClass.class);
		
		/* Delete processor class using Product Class Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getProductClassManagerUrl(),
					URI_PATH_PRODUCTCLASSES + "/" + restProductClass.getId(), 
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_PRODUCTCLASS_NOT_FOUND_BY_ID, restProductClass.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), PRODUCTCLASSES, loginManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = uiMsg(MSG_ID_PRODUCTCLASS_DELETE_FAILED, productType, e.getMessage());
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
		String message = uiMsg(MSG_ID_PRODUCTCLASS_DELETED, restProductClass.getId());
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
		}
		
		/* Check argument */
		if (!CMD_PRODUCTCLASS.equals(command.getName())) {
			System.err.println(uiMsg(MSG_ID_INVALID_COMMAND_NAME, command.getName()));
			return;
		}
		
		/* Make sure a subcommand is given */
		ParsedCommand subcommand = command.getSubcommand();
		if (null == subcommand 
				|| (CMD_RULE.equals(subcommand.getName())
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
		switch (subcommand.getName()) {
		// Handle commands for product classes
		case CMD_CREATE:	createProductClass(subcommand); break COMMAND;
		case CMD_SHOW:		showProductClass(subcommand); break COMMAND;
		case CMD_UPDATE:	updateProductClass(subcommand); break COMMAND;
		case CMD_DELETE:	deleteProductClass(subcommand); break COMMAND;
		case CMD_RULE:
			// Handle commands for selection rules
			switch (subsubcommand.getName()) {
//			case CMD_CREATE:	createConfiguredProcessor(subsubcommand); break COMMAND;
//			case CMD_SHOW:		showConfiguredProcessor(subsubcommand); break COMMAND;
//			case CMD_UPDATE:	updateConfiguredProcessor(subsubcommand); break COMMAND;
//			case CMD_DELETE:	deleteConfiguredProcessor(subsubcommand); break COMMAND;
			default:
				System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, 
						command.getName() + " " + subcommand.getName() + " " + subsubcommand.getName()));
				return;
			}
		default:
			System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
			return;
		}
	}
}
