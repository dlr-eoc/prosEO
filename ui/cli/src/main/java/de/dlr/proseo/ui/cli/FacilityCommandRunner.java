/**
 * FacilityCommandRunner.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
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

import de.dlr.proseo.model.rest.model.RestProcessingFacility;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;
import de.dlr.proseo.ui.cli.parser.ParsedParameter;

/**
 * Run commands for managing prosEO processing facilities (create, read, update, delete). 
 * All methods assume that before invocation a syntax check of the command has been performed, so no extra checks are performed.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class FacilityCommandRunner {

	/* General string constants */
	public static final String CMD_FACILITY = "facility";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";
	
	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_FACILITY_NAME = "Facility name (empty field cancels): ";
	private static final String PROMPT_PROCENG_URL = "Processing engine URL (empty field cancels): ";
	private static final String PROMPT_STORAGEMGR_URL = "Storage manager URL (empty field cancels): ";
	private static final String PROMPT_STORAGEMGR_USER = "Storage manager username (empty field cancels): ";
	private static final String PROMPT_STORAGEMGR_PASSWD = "Storage manager password (empty field cancels): ";
	private static final String PROMPT_LOCAL_STORAGEMGR_URL = "Kubernetes-local storage manager URL (empty field cancels): ";
	private static final String PROMPT_STORAGE_TYPE = "Default storage type (empty field cancels): ";
	
	private static final String URI_PATH_FACILITIES = "/facilities";
	
	private static final String FACILITIES = "facilities";
	
	private static final String PWD_PLACEHOLDER = "********";

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
	private static Logger logger = LoggerFactory.getLogger(FacilityCommandRunner.class);

	/**
	 * Retrieve the processing facility with the given name, notifying the user of any errors occurring
	 * 
	 * @param facilityName the facility name
	 * @return the requested facility or null, if the facility does not exist
	 */
	private RestProcessingFacility retrieveFacilityByName(String facilityName) {
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getFacilityManagerUrl(),
					URI_PATH_FACILITIES + "?name=" + URLEncoder.encode(facilityName, Charset.defaultCharset()), List.class, loginManager.getUser(), loginManager.getPassword());
			if (resultList.isEmpty()) {
				String message = uiMsg(MSG_ID_FACILITY_NOT_FOUND, facilityName);
				logger.error(message);
				System.err.println(message);
				return null;
			} else {
				ObjectMapper mapper = new ObjectMapper();
				try {
					return mapper.convertValue(resultList.get(0), RestProcessingFacility.class);
				} catch (Exception e) {
					String message = uiMsg(MSG_ID_FACILITY_NOT_READABLE, facilityName, e.getMessage());
					logger.error(message);
					System.err.println(message);
					return null;
				}
			}
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_FACILITY_NOT_FOUND, facilityName);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), FACILITIES, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, "(" + e.getRawStatusCode() + ") " + e.getMessage());
			}
			logger.error(message);
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			String message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			logger.error(message);
			System.err.println(message);
			e.printStackTrace(System.err);
			return null;
		}
	}

	/**
	 * Create a new processing facility in prosEO; if the input is not from a file, the user will be prompted for mandatory 
	 * attributes not given on the command line
	 * 
	 * @param createCommand the parsed "facility create" command
	 */
	private void createFacility(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createFacility({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File facilityFile = null;
		String facilityFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				facilityFile = new File(option.getValue());
				break;
			case "format":
				facilityFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read processing facility file, if any */
		RestProcessingFacility restFacility = null;
		if (null == facilityFile) {
			restFacility = new RestProcessingFacility();
		} else {
			try {
				restFacility = CLIUtil.parseObjectFile(facilityFile, facilityFileFormat, RestProcessingFacility.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from processing facility file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is facility name
				restFacility.setName(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restFacility, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restFacility.getName() || restFacility.getName().isBlank()) {
			System.out.print(PROMPT_FACILITY_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restFacility.setName(response);
		}
		if (null == restFacility.getProcessingEngineUrl() || restFacility.getProcessingEngineUrl().isBlank()) {
			System.out.print(PROMPT_PROCENG_URL);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restFacility.setProcessingEngineUrl(response);
		}
		if (null == restFacility.getStorageManagerUrl() || restFacility.getStorageManagerUrl().isBlank()) {
			System.out.print(PROMPT_STORAGEMGR_URL);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restFacility.setStorageManagerUrl(response);
		}
		if (null == restFacility.getStorageManagerUser() || restFacility.getStorageManagerUser().isBlank()) {
			System.out.print(PROMPT_STORAGEMGR_USER);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restFacility.setStorageManagerUser(response);
		}
		if (null == restFacility.getStorageManagerPassword() || restFacility.getStorageManagerPassword().isBlank()) {
			System.out.print(PROMPT_STORAGEMGR_PASSWD);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restFacility.setStorageManagerPassword(response);
		}
		if (null == restFacility.getLocalStorageManagerUrl() || restFacility.getLocalStorageManagerUrl().isBlank()) {
			System.out.print(PROMPT_LOCAL_STORAGEMGR_URL);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restFacility.setLocalStorageManagerUrl(response);
		}
		if (null == restFacility.getDefaultStorageType() || restFacility.getDefaultStorageType().isBlank()) {
			System.out.print(PROMPT_STORAGE_TYPE);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restFacility.setDefaultStorageType(response);
		}
		
		/* Create processing facility */
		try {
			restFacility = serviceConnection.postToService(serviceConfig.getFacilityManagerUrl(), URI_PATH_FACILITIES, 
					restFacility, RestProcessingFacility.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_FACILITY_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), FACILITIES, loginManager.getMission());
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

		/* Report success, giving newly assigned processing facility ID */
		String message = uiMsg(MSG_ID_FACILITY_CREATED,
				restFacility.getName(), restFacility.getId());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Show the processing facility specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "facility show" command
	 */
	@SuppressWarnings("unchecked")
	private void showFacility(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showFacility({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Check command options */
		String facilityOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		Boolean isVerbose = false, showPasswords = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case "format":
				facilityOutputFormat = option.getValue().toUpperCase();
				break;
			case "verbose":
				isVerbose = true;
				break;
			case "showPasswords":
				showPasswords = true;
				break;
			}
		}
		
		/* If facility name is set, show just the requested facility */
		if (!showCommand.getParameters().isEmpty()) {
			// Only facility name allowed as parameter
			RestProcessingFacility restFacility = retrieveFacilityByName(showCommand.getParameters().get(0).getValue());
			if (null != restFacility) {
				try {
					CLIUtil.printObject(System.out, restFacility, facilityOutputFormat);
				} catch (IllegalArgumentException e) {
					System.err.println(e.getMessage());
				} catch (IOException e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				}
			}
			return;
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_FACILITIES;
		
		/* Get the facility information from the Facility Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getFacilityManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_FACILITIES_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), FACILITIES, loginManager.getMission());
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
		
		// Remove passwords unless explicitly requested
		if (!showPasswords) {
			// Must be a list of processing facilities
			for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					((Map<String, Object>) resultObject).put("processingEnginePassword", PWD_PLACEHOLDER);
					((Map<String, Object>) resultObject).put("storageManagerPassword", PWD_PLACEHOLDER);
				}
			}
		}
		
		/* Display the facility(s) found */
		if (isVerbose) {
			// Print facility details
			try {
				CLIUtil.printObject(System.out, resultList, facilityOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			} 
		} else {
			// Print facility names only; resultList must be a list of processing facilities
			for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					System.out.println(((Map<?, ?>) resultObject).get("name"));
				}
			}
		}
	}
	
	/**
	 * Update a processing facility from a processing facility file or from "attribute=value" pairs
	 * (overriding any processing facility file entries)
	 * 
	 * @param updateCommand the parsed "facility update" command
	 */
	private void updateFacility(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateFacility({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File facilityFile = null;
		String facilityFileFormat = CLIUtil.FILE_FORMAT_JSON;
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				facilityFile = new File(option.getValue());
				break;
			case "format":
				facilityFileFormat = option.getValue().toUpperCase();
				break;
			case "delete-attributes":
				isDeleteAttributes = true;
				break;
			}
		}
		
		/* Read processing facility file, if any */
		RestProcessingFacility updatedFacility = null;
		if (null == facilityFile) {
			updatedFacility = new RestProcessingFacility();
		} else {
			try {
				updatedFacility = CLIUtil.parseObjectFile(facilityFile, facilityFileFormat, RestProcessingFacility.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from processing facility file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is facility name
				updatedFacility.setName(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedFacility, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Read original facility from Facility Manager service */
		if (null == updatedFacility.getName() || 0 == updatedFacility.getName().length()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_FACILITY_NAME_GIVEN));
			return;
		}
		RestProcessingFacility restFacility = retrieveFacilityByName(updatedFacility.getName());
		if (null == restFacility) {
			return;
		}

		/* Compare attributes of database facility with updated facility */
		// No modification of ID, version and facility name allowed
		if (null != updatedFacility.getName() && 0 != updatedFacility.getName().length()) { // mandatory, must not be empty
			restFacility.setName(updatedFacility.getName());
		}
		if (isDeleteAttributes ||
				null != updatedFacility.getDescription() && !updatedFacility.getDescription().isBlank()) {
			restFacility.setDescription(updatedFacility.getDescription());
		}
		if (null != updatedFacility.getProcessingEngineUrl() && !updatedFacility.getProcessingEngineUrl().isBlank()) {
			restFacility.setProcessingEngineUrl(updatedFacility.getProcessingEngineUrl());
		}
		if (null != updatedFacility.getProcessingEngineUser() && !updatedFacility.getProcessingEngineUser().isBlank()) {
			restFacility.setProcessingEngineUser(updatedFacility.getProcessingEngineUser());
		}
		if (null != updatedFacility.getProcessingEnginePassword() && !updatedFacility.getProcessingEnginePassword().isBlank()) {
			restFacility.setProcessingEnginePassword(updatedFacility.getProcessingEnginePassword());
		}
		if (null != updatedFacility.getStorageManagerUrl() && !updatedFacility.getStorageManagerUrl().isBlank()) {
			restFacility.setStorageManagerUrl(updatedFacility.getStorageManagerUrl());
		}
		if (null != updatedFacility.getLocalStorageManagerUrl() && !updatedFacility.getLocalStorageManagerUrl().isBlank()) {
			restFacility.setLocalStorageManagerUrl(updatedFacility.getLocalStorageManagerUrl());
		}
		if (null != updatedFacility.getStorageManagerUser() && !updatedFacility.getStorageManagerUser().isBlank()) {
			restFacility.setStorageManagerUser(updatedFacility.getStorageManagerUser());
		}
		if (null != updatedFacility.getStorageManagerPassword() && !updatedFacility.getStorageManagerPassword().isBlank()) {
			restFacility.setStorageManagerPassword(updatedFacility.getStorageManagerPassword());
		}
		if (null != updatedFacility.getDefaultStorageType() && !updatedFacility.getDefaultStorageType().isBlank()) {
			restFacility.setDefaultStorageType(updatedFacility.getDefaultStorageType());
		}
		
		/* Update processing facility using Facility Manager service */
		try {
			restFacility = serviceConnection.patchToService(serviceConfig.getFacilityManagerUrl(),
					URI_PATH_FACILITIES + "/" + restFacility.getId(),
					restFacility, RestProcessingFacility.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(uiMsg(MSG_ID_NOT_MODIFIED));
				return;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_FACILITY_NOT_FOUND_BY_ID, restFacility.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_FACILITY_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), FACILITIES, loginManager.getMission());
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
		String message = uiMsg(MSG_ID_FACILITY_UPDATED, restFacility.getId(), restFacility.getVersion());
		logger.info(message);
		System.out.println(message);
	}

	/**
	 * Delete the given processing facility
	 * 
	 * @param deleteCommand the parsed "facility delete" command
	 */
	private void deleteFacility(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteFacility({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get processing facility name from command parameters */
		if (1 > deleteCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_FACILITY_NAME_GIVEN));
			return;
		}
		String facilityName = deleteCommand.getParameters().get(0).getValue();
		
		/* Retrieve the processing facility using Facility Manager service */
		RestProcessingFacility restFacility = retrieveFacilityByName(facilityName);
		if (null == restFacility) {
			return;
		}
		
		/* Delete facility using Facility Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getFacilityManagerUrl(),
					URI_PATH_FACILITIES + "/" + restFacility.getId(), 
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_FACILITY_NOT_FOUND_BY_ID, restFacility.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), FACILITIES, loginManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = uiMsg(MSG_ID_FACILITY_DELETE_FAILED, facilityName, e.getMessage());
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
		String message = uiMsg(MSG_ID_FACILITY_DELETED, restFacility.getId());
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
		
		/* Check argument */
		if (!CMD_FACILITY.equals(command.getName())) {
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
				
		/* Execute the (sub-command */
		switch (subcommand.getName()) {
		case CMD_CREATE:	createFacility(subcommand); break;
		case CMD_SHOW:		showFacility(subcommand); break;
		case CMD_UPDATE:	updateFacility(subcommand); break;
		case CMD_DELETE:	deleteFacility(subcommand); break;
		default:
			System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
			return;
		}
	}
}
