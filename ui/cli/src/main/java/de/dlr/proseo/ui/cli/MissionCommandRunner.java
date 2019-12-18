/**
 * MissionCommandRunner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.model.rest.model.RestSpacecraft;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;
import de.dlr.proseo.ui.cli.parser.ParsedParameter;

/**
 * Run commands for managing prosEO missions, spacecrafts and orbits (create, read, update, delete etc.). 
 * All methods assume that before invocation a syntax check of the command has been performed, so no extra checks are performed.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class MissionCommandRunner {

	/* General string constants */
	public static final String CMD_MISSION = "mission";
	private static final String CMD_SPACECRAFT = "spacecraft";
	private static final String CMD_ORBIT = "orbit";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";
	private static final String CMD_ADD = "add";
	private static final String CMD_REMOVE = "remove";
	
	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_SPACECRAFT_CODE = "Spacecraft code (empty field cancels): ";
	private static final String PROMPT_SPACECRAFT_NAME = "Spacecraft name (empty field cancels): ";
	
	private static final String URI_PATH_MISSIONS = "/missions";
	private static final String URI_PATH_ORBITS = "/orbits";
	
	private static final String MISSIONS = "missions";
	private static final String ORBITS = "orbits";

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
	private static Logger logger = LoggerFactory.getLogger(MissionCommandRunner.class);

	/**
	 * Retrieve the mission with the given code, notifying the user of any errors occurring
	 * 
	 * @param missionCode the mission code
	 * @return the requested mission or null, if the mission does not exist
	 */
	private RestMission retrieveMissionByCode(String missionCode) {
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					URI_PATH_MISSIONS, List.class, loginManager.getUser(), loginManager.getPassword());
			for (Object result: resultList) {
				if (logger.isTraceEnabled()) logger.trace("Found result object of class " + result.getClass());
				if (result instanceof Map && missionCode.equals(((Map<?, ?>) result).get("code"))) {
					ObjectMapper mapper = new ObjectMapper();
					try {
						return mapper.convertValue(result, RestMission.class);
					} catch (Exception e) {
						String message = uiMsg(MSG_ID_MISSION_NOT_READABLE, loginManager.getMission(), e.getMessage());
						logger.error(message);
						System.err.println(message);
						return null;
					}
				}
			}
			String message = uiMsg(MSG_ID_MISSION_NOT_FOUND, missionCode);
			logger.error(message);
			System.err.println(message);
			return null;
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_MISSION_NOT_FOUND, loginManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, loginManager.getMission());
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
	 * Show the mission specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "mission show" command
	 */
	private void showMission(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showMission({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Check command options */
		String missionOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case "format":
				missionOutputFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* If mission code is set, show just the requested mission */
		if (!showCommand.getParameters().isEmpty()) {
			// Only mission code allowed as parameter
			RestMission restMission = retrieveMissionByCode(showCommand.getParameters().get(0).getValue());
			if (null != restMission) {
				try {
					CLIUtil.printObject(System.out, restMission, missionOutputFormat);
				} catch (IllegalArgumentException e) {
					System.err.println(e.getMessage());
				} catch (IOException e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				}
			}
			return;
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_MISSIONS;
		
		/* Get the mission information from the Order Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, loginManager.getMission());
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
		
		/* Display the mission(s) found */
		try {
			CLIUtil.printObject(System.out, resultList, missionOutputFormat);
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			return;
		} catch (IOException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
	}
	
	/**
	 * Update a mission from a mission file or from "attribute=value" pairs (overriding any mission file entries)
	 * 
	 * @param updateCommand the parsed "mission update" command
	 */
	private void updateMission(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateMission({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File missionFile = null;
		String missionFileFormat = null;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				missionFile = new File(option.getValue());
				break;
			case "format":
				missionFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read mission file, if any */
		RestMission updatedMission = null;
		if (null == missionFile) {
			updatedMission = new RestMission();
		} else {
			try {
				updatedMission = CLIUtil.parseObjectFile(missionFile, missionFileFormat, RestMission.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from mission file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			// All parameters are "attribute=value" parameters
			try {
				CLIUtil.setAttribute(updatedMission, param.getValue());
			} catch (Exception e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Read original mission from Order Manager service */
		RestMission restMission = retrieveMissionByCode(loginManager.getMission());

		/* Compare attributes of database mission with updated mission */
		// No modification of ID, version, mission code and spacecrafts allowed
		if (null != updatedMission.getName() && 0 != updatedMission.getName().length()) { // mandatory, must not be empty
			restMission.setName(updatedMission.getName());
		}
		if (!updatedMission.getFileClasses().isEmpty()) {
			restMission.setFileClasses(updatedMission.getFileClasses());
		}
		if (!updatedMission.getProcessingModes().isEmpty()) {
			restMission.setProcessingModes(updatedMission.getProcessingModes());
		}
		if (null != updatedMission.getProductFileTemplate()) {
			restMission.setProductFileTemplate(updatedMission.getProductFileTemplate());
		}
		
		/* Update mission using Order Manager service */
		try {
			restMission = serviceConnection.patchToService(serviceConfig.getOrderManagerUrl(),
					URI_PATH_MISSIONS + "/" + restMission.getId(),
					restMission, RestMission.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_MISSION_NOT_FOUND_BY_ID, restMission.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_MISSION_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, loginManager.getMission());
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
		String message = uiMsg(MSG_ID_MISSION_UPDATED, restMission.getId(), restMission.getVersion());
		logger.info(message);
		System.out.println(message);
	}

	/**
	 * Add a new spacecraft to a mission; if the input is not from a file, the user will be prompted for mandatory attributes
	 * not given on the command line
	 * 
	 * @param addCommand the parsed "mission spacecraft add" command
	 */
	private void addSpacecraft(ParsedCommand addCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> addSpacecraft({})", (null == addCommand ? "null" : addCommand.getName()));
		
		/* Check command options */
		File processorFile = null;
		String processorFileFormat = null;
		for (ParsedOption option: addCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				processorFile = new File(option.getValue());
				break;
			case "format":
				processorFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read spacecraft file, if any */
		RestSpacecraft restSpacecraft = null;
		if (null == processorFile) {
			restSpacecraft = new RestSpacecraft();
		} else {
			try {
				restSpacecraft = CLIUtil.parseObjectFile(processorFile, processorFileFormat, RestSpacecraft.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from spacecraft file) */
		for (int i = 0; i < addCommand.getParameters().size(); ++i) {
			ParsedParameter param = addCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is spacecraft code
				restSpacecraft.setCode(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restSpacecraft, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restSpacecraft.getCode() || 0 == restSpacecraft.getCode().length()) {
			System.out.print(PROMPT_SPACECRAFT_CODE);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restSpacecraft.setCode(response);
		}
		if (null == restSpacecraft.getName() || 0 == restSpacecraft.getName().length()) {
			System.out.print(PROMPT_SPACECRAFT_NAME);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restSpacecraft.setName(response);
		}
		
		/* Find mission and add spacecraft */
		RestMission restMission = retrieveMissionByCode(loginManager.getMission());
		if (null == restMission) {
			// Already handled
			return;
		}
		
		for (RestSpacecraft oldSpacecraft: restMission.getSpacecrafts()) {
			if (restSpacecraft.getCode().equals(oldSpacecraft.getCode())) {
				String message = uiMsg(MSG_ID_SPACECRAFT_EXISTS,
						restSpacecraft.getCode(), loginManager.getMission());
				logger.error(message);
				System.err.println(message);
				return;
			}
		}
		restMission.getSpacecrafts().add(restSpacecraft);

		/* Update mission using Order Manager service */
		try {
			restMission = serviceConnection.patchToService(serviceConfig.getOrderManagerUrl(),
					URI_PATH_MISSIONS + "/" + restMission.getId(),
					restMission, RestMission.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_MISSION_NOT_FOUND_BY_ID, restMission.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_MISSION_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, "(" + e.getRawStatusCode() + ") " + e.getMessage());
			}
			logger.error(message);
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			e.printStackTrace(System.err);
			return;
		}

		/* Report success, giving newly assigned processor ID and version */
		for (RestSpacecraft missionSpacecraft: restMission.getSpacecrafts()) {
			if (restSpacecraft.getCode().equals(missionSpacecraft.getCode())) {
				String message = uiMsg(MSG_ID_SPACECRAFT_ADDED,
						missionSpacecraft.getCode(), missionSpacecraft.getId());
				logger.info(message);
				System.out.println(message);
				break;
			}
		}
	}
	
	/**
	 * Remove a spacecraft from a mission
	 * 
	 * @param removeCommand the parsed "mission spacecraft remove" command
	 */
	private void removeSpacecraft(ParsedCommand removeCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> removeSpacecraft({})", (null == removeCommand ? "null" : removeCommand.getName()));
		
		/* Get spacecraft code from command parameters */
		if (1 > removeCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_SPACECRAFT_CODE_GIVEN));
			return;
		}
		String spacecraftCode = removeCommand.getParameters().get(0).getValue();
		
		/* Find mission and remove spacecraft */
		RestMission restMission = retrieveMissionByCode(loginManager.getMission());
		if (null == restMission) {
			// Already handled
			return;
		}
		
		RestSpacecraft spacecraftToDelete = null;
		for (RestSpacecraft oldSpacecraft: restMission.getSpacecrafts()) {
			if (spacecraftCode.equals(oldSpacecraft.getCode())) {
				spacecraftToDelete = oldSpacecraft;
				break;
			}
		}
		if (null == spacecraftToDelete) {
			String message = uiMsg(MSG_ID_SPACECRAFT_NOT_FOUND, spacecraftCode, loginManager.getMission());
			logger.info(message);
			System.err.println(message);
			return;
		}
		restMission.getSpacecrafts().remove(spacecraftToDelete);

		/* Update mission using Order Manager service */
		try {
			restMission = serviceConnection.patchToService(serviceConfig.getOrderManagerUrl(),
					URI_PATH_MISSIONS + "/" + restMission.getId(),
					restMission, RestMission.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_MISSION_NOT_FOUND_BY_ID, restMission.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_MISSION_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, loginManager.getMission());
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
		String message = uiMsg(MSG_ID_SPACECRAFT_REMOVED, spacecraftCode, restMission.getCode());
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
		if (!CMD_MISSION.equals(command.getName()) && !CMD_ORBIT.equals(command.getName())) {
			System.err.println(uiMsg(MSG_ID_INVALID_COMMAND_NAME, command.getName()));
			return;
		}
		
		/* Make sure a subcommand is given */
		ParsedCommand subcommand = command.getSubcommand();
		if (null == subcommand 
				|| (CMD_SPACECRAFT.equals(subcommand.getName()) && null == subcommand.getSubcommand())) {
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
		case CMD_MISSION:
			switch (subcommand.getName()) {
			case CMD_SPACECRAFT:
				// Handle commands for spacecrafts
				switch (subsubcommand.getName()) {
				case CMD_ADD:		addSpacecraft(subsubcommand); break COMMAND;
				case CMD_REMOVE:	removeSpacecraft(subsubcommand); break COMMAND;
				default:
					System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, 
							command.getName() + " " + subcommand.getName() + " " + subsubcommand.getName()));
					return;
				}
			// Handle commands for processors
			case CMD_SHOW:		showMission(subcommand); break COMMAND;
			case CMD_UPDATE:	updateMission(subcommand); break COMMAND;
			default:
				System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		case CMD_ORBIT:
			// Handle commands for configurations
			switch (subcommand.getName()) {
//			case CMD_CREATE:	createOrbits(subcommand); break COMMAND;
//			case CMD_SHOW:		showOrbits(subcommand); break COMMAND;
//			case CMD_UPDATE:	createOrbits(subcommand); break COMMAND;
//			case CMD_DELETE:	deleteOrbits(subcommand); break COMMAND;
			default:
				System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		}
	}
}
