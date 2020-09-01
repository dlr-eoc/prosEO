/**
 * MissionCommandRunner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.model.rest.model.RestOrbit;
import de.dlr.proseo.model.rest.model.RestSpacecraft;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
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
	public static final String CMD_ORBIT = "orbit";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";
	private static final String CMD_ADD = "add";
	private static final String CMD_REMOVE = "remove";
	
	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_MISSION_NAME = "Mission name (empty field cancels): ";
	private static final String PROMPT_FILE_TEMPLATE = "Product file name template (empty field cancels): ";
	private static final String PROMPT_SPACECRAFT_CODE = "Spacecraft code (empty field cancels): ";
	private static final String PROMPT_SPACECRAFT_NAME = "Spacecraft name (empty field cancels): ";
	private static final String PROMPT_ORBIT_NUMBER = "Orbit number (empty field cancels): ";
	private static final String PROMPT_START_TIME = "Orbit start time (yyyy-mm-ddThh:mm:ss.SSSSSS; empty field cancels): ";
	private static final String PROMPT_STOP_TIME = "Orbit stop time (yyyy-mm-ddThh:mm:ss.SSSSSS; empty field cancels): ";
	
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
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
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
	 * Create a new earth observation mission in prosEO; if the input is not from a file, the user will be prompted for mandatory 
	 * attributes not given on the command line
	 * 
	 * @param createCommand the parsed "mission create" command
	 */
	private void createMission(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createMission({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File missionFile = null;
		String missionFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
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
		RestMission restMission = null;
		if (null == missionFile) {
			restMission = new RestMission();
		} else {
			try {
				restMission = CLIUtil.parseObjectFile(missionFile, missionFileFormat, RestMission.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from mission file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is mission code
				restMission.setCode(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restMission, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restMission.getName() || restMission.getName().isBlank()) {
			System.out.print(PROMPT_MISSION_NAME);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restMission.setName(response);
		}
		if (null == restMission.getProductFileTemplate() || restMission.getProductFileTemplate().isBlank()) {
			System.out.print(PROMPT_FILE_TEMPLATE);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restMission.setProductFileTemplate(response);
		}
		
		/* Create mission */
		try {
			restMission = serviceConnection.postToService(serviceConfig.getOrderManagerUrl(), URI_PATH_MISSIONS, 
					restMission, RestMission.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_MISSION_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
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

		/* Report success, giving newly assigned processor class ID */
		String message = uiMsg(MSG_ID_MISSION_CREATED,
				restMission.getCode(), restMission.getId());
		logger.info(message);
		System.out.println(message);
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
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
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
		String missionFileFormat = CLIUtil.FILE_FORMAT_JSON;
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
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
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
	 * Delete the given mission (optionally removing all configured items and all products, too)
	 * 
	 * @param deleteCommand the parsed "mission delete" command
	 */
	private void deleteMission(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteMission({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Check command options */
		boolean forcedDelete = false;
		boolean deleteProducts = false;
		for (ParsedOption option: deleteCommand.getOptions()) {
			switch(option.getName()) {
			case "force":
				forcedDelete = true;
				break;
			case "delete-products":
				deleteProducts = true;
				break;
			}
		}
		if (deleteProducts && !forcedDelete) {
			// "delete-products" only allowed with "force" to avoid unintentional data loss
			System.err.println(uiMsg(MSG_ID_DELETE_PRODUCTS_WITHOUT_FORCE));
			return;
		}
		
		/* Get mission code from command parameters */
		if (deleteCommand.getParameters().isEmpty()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_MISSION_CODE_GIVEN));
			return;
		}
		String missionCode = deleteCommand.getParameters().get(0).getValue();
		
		/* Retrieve the mission using Order Manager service */
		RestMission mission = retrieveMissionByCode(missionCode);
		if (null == mission) {
			return;
		}
		
		/* Delete mission using Order Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getOrderManagerUrl(), 
					URI_PATH_MISSIONS + "/" + mission.getId() +
					(forcedDelete ? "?force=true" : "") +
					(deleteProducts ? "&delete-products=true" : ""), 
				loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_MISSION_NOT_FOUND, missionCode);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, missionCode);
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = uiMsg(MSG_ID_MISSION_DELETE_FAILED, missionCode, e.getMessage());
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
		String message = uiMsg(MSG_ID_MISSION_DELETED, missionCode);
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
		String processorFileFormat = CLIUtil.FILE_FORMAT_JSON;
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
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
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
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
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
	 * Create a new set of orbits; if the input is not from a file, the user will be prompted for mandatory attributes 
	 * not given on the command line (for a single orbit)
	 * 
	 * @param createCommand the parsed "orbit create" command
	 */
	@SuppressWarnings("unchecked")
	private void createOrbit(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createOrbit({})", (null == createCommand ? "null" : createCommand.getName()));
		
		/* Check command options */
		File orbitFile = null;
		String orbitFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				orbitFile = new File(option.getValue());
				break;
			case "format":
				orbitFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read orbit file, if any */
		List<Object> orbitList = null;
		if (null == orbitFile) {
			orbitList = new ArrayList<>();
		} else {
			try {
				orbitList = CLIUtil.parseObjectFile(orbitFile, orbitFileFormat, List.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* If the orbit list is empty, we create a single orbit from user input */
		if (orbitList.isEmpty()) {
			RestOrbit restOrbit = new RestOrbit();
			restOrbit.setMissionCode(loginManager.getMission());
			/* Check command parameters (overriding values from processor class file) */
			for (int i = 0; i < createCommand.getParameters().size(); ++i) {
				ParsedParameter param = createCommand.getParameters().get(i);
				if (0 == i) {
					// First parameter is spacecraft code
					restOrbit.setSpacecraftCode(param.getValue());
				} else {
					// Remaining parameters are "attribute=value" parameters
					try {
						CLIUtil.setAttribute(restOrbit, param.getValue());
					} catch (Exception e) {
						System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
						return;
					}
				}
			}
			/* Prompt user for missing mandatory attributes */
			System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
			if (null == restOrbit.getSpacecraftCode() || 0 == restOrbit.getSpacecraftCode().length()) {
				System.out.print(PROMPT_SPACECRAFT_CODE);
				String response = System.console().readLine();
				if ("".equals(response)) {
					System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
					return;
				}
				restOrbit.setSpacecraftCode(response);
			}
			while (null == restOrbit.getOrbitNumber() || 0 == restOrbit.getOrbitNumber().longValue()) {
				System.out.print(PROMPT_ORBIT_NUMBER);
				String response = System.console().readLine();
				if ("".equals(response)) {
					System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
					return;
				}
				try {
					restOrbit.setOrbitNumber(Long.parseLong(response));
				} catch (NumberFormatException e) {
					System.err.println(uiMsg(MSG_ID_ORBIT_NUMBER_INVALID, response));
				}
			}
			while (null == restOrbit.getStartTime() || 0 == restOrbit.getStartTime().length()) {
				System.out.print(PROMPT_START_TIME);
				String response = System.console().readLine();
				if ("".equals(response)) {
					System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
					return;
				}
				try {
					restOrbit.setStartTime(OrbitTimeFormatter.format(CLIUtil.parseDateTime(response))); // no time zone in input expected
				} catch (DateTimeParseException e) {
					System.err.println(uiMsg(MSG_ID_INVALID_TIME, response));
				}
			}
			while (null == restOrbit.getStopTime() || 0 == restOrbit.getStopTime().length()) {
				System.out.print(PROMPT_STOP_TIME);
				String response = System.console().readLine();
				if ("".equals(response)) {
					System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
					return;
				}
				try {
					restOrbit.setStopTime(OrbitTimeFormatter.format(CLIUtil.parseDateTime(response))); // no time zone in input expected
				} catch (DateTimeParseException e) {
					System.err.println(uiMsg(MSG_ID_INVALID_TIME, response));
				}
			}
			orbitList.add(restOrbit);
		}
		
		/* Create orbit(s) */
		try {
			orbitList = serviceConnection.postToService(serviceConfig.getOrderManagerUrl(), URI_PATH_ORBITS, 
					orbitList, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_ORBIT_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), ORBITS, loginManager.getMission());
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

		/* Report success, giving number of orbits created */
		String message = uiMsg(MSG_ID_ORBITS_CREATED, orbitList.size());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Show the orbits specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "orbit show" command
	 */
	private void showOrbit(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showOrbit({})", (null == showCommand ? "null" : showCommand.getName()));
		
		/* Check command options */
		String orbitOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		Integer fromOrbit = null;
		Integer toOrbit = null;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case "from":
				fromOrbit = Integer.parseInt(option.getValue());
				break;
			case "to":
				toOrbit = Integer.parseInt(option.getValue());
				break;
			case "format":
				orbitOutputFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Prepare request URI */
		if (1 > showCommand.getParameters().size()) {
			System.err.println(uiMsg(MSG_ID_NO_SPACECRAFT_CODE_GIVEN));
			return;
		}
		String requestURI = URI_PATH_ORBITS + "?spacecraftCode=" + showCommand.getParameters().get(0).getValue();
		if (null != fromOrbit) {
			requestURI += "&orbitNumberFrom=" + fromOrbit;
		}
		if (null != toOrbit) {
			requestURI += "&orbitNumberTo=" + toOrbit;
		}
		
		/* Get the orbit information from the Order Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
					requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_ORBITS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), ORBITS, loginManager.getMission());
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
		
		/* Display the orbits found */
		try {
			CLIUtil.printObject(System.out, resultList, orbitOutputFormat);
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			return;
		} catch (IOException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
	}
	
	/**
	 * Update a set of orbits from an orbit file or from "attribute=value" pairs (if no orbit file is given)
	 * 
	 * @param updateCommand the parsed "orbit update" command
	 */
	@SuppressWarnings("unchecked")
	private void updateOrbit(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateOrbit({})", (null == updateCommand ? "null" : updateCommand.getName()));

		/* Check command options */
		File orbitFile = null;
		String orbitFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				orbitFile = new File(option.getValue());
				break;
			case "format":
				orbitFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read orbit file, if any */
		List<Object> orbitList = null;
		if (null == orbitFile) {
			orbitList = new ArrayList<>();
		} else {
			try {
				orbitList = CLIUtil.parseObjectFile(orbitFile, orbitFileFormat, List.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Update a single orbit from user input, if no file is given (or the file is empty) */
		if (orbitList.isEmpty()) {
			RestOrbit restOrbit = new RestOrbit();
			restOrbit.setMissionCode(loginManager.getMission());
			/* Check command parameters */
			for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
				ParsedParameter param = updateCommand.getParameters().get(i);
				if (0 == i) {
					// First parameter is spacecraft code
					restOrbit.setSpacecraftCode(param.getValue());
				} else if (1 == i) {
					// Second parameter is orbit number
					try {
						restOrbit.setOrbitNumber(Long.parseLong(param.getValue()));
					} catch (NumberFormatException e) {
						System.err.println(uiMsg(MSG_ID_ORBIT_NUMBER_INVALID, param.getValue()));
						return;
					}
					;
				} else {
					// Remaining parameters are "attribute=value" parameters
					try {
						CLIUtil.setAttribute(restOrbit, param.getValue());
					} catch (Exception e) {
						System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
						return;
					}
				}
			}
			orbitList.add(restOrbit);
		}
		
		/* Update orbits one by one */
		ObjectMapper mapper = new ObjectMapper();
		for (Object listObject: orbitList) {
			RestOrbit updatedOrbit = mapper.convertValue(listObject, RestOrbit.class);
			
			/* Read original orbit from Order Manager service */
			if ((null == updatedOrbit.getId() || 0 == updatedOrbit.getId().longValue())
					&& (null == updatedOrbit.getSpacecraftCode() || 0 == updatedOrbit.getSpacecraftCode().length()
					|| null == updatedOrbit.getOrbitNumber() || 0 == updatedOrbit.getOrbitNumber().longValue())) {
				// No identifying value given
				System.err.println(uiMsg(MSG_ID_NO_ORBIT_IDENTIFIER_GIVEN));
				return;
			}
			RestOrbit restOrbit = null;
			try {
				if (null == updatedOrbit.getId() || 0 == updatedOrbit.getId().longValue()) {
					List<?> resultList = null;
					resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
							URI_PATH_ORBITS + "?spacecraftCode=" + updatedOrbit.getSpacecraftCode() 
								+ "&orbitNumberFrom=" + updatedOrbit.getOrbitNumber() 
								+ "&orbitNumberTo=" + updatedOrbit.getOrbitNumber(),
							List.class, loginManager.getUser(), loginManager.getPassword());
					if (resultList.isEmpty()) {
						String message = uiMsg(MSG_ID_ORBIT_NOT_FOUND,
								updatedOrbit.getOrbitNumber(), updatedOrbit.getSpacecraftCode());
						logger.error(message);
						System.err.println(message);
						return;
					}
					restOrbit = mapper.convertValue(resultList.get(0), RestOrbit.class);
				} else {
					restOrbit = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(),
							URI_PATH_ORBITS + "/" + updatedOrbit.getId(),
							RestOrbit.class, loginManager.getUser(), loginManager.getPassword());
				}
			} catch (RestClientResponseException e) {
				String message = null;
				switch (e.getRawStatusCode()) {
				case org.apache.http.HttpStatus.SC_NOT_FOUND:
					message = uiMsg(MSG_ID_ORBIT_NOT_FOUND,
							updatedOrbit.getOrbitNumber(), updatedOrbit.getSpacecraftCode());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				case org.apache.http.HttpStatus.SC_FORBIDDEN:
					message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), ORBITS, loginManager.getMission());
					break;
				default:
					message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
				}
				logger.error(message);
				System.err.println(message);
				return;
			} catch (RuntimeException e) {
				String message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
				logger.error(message);
				System.err.println(message);
				return;
			}

			/* Compare attributes of database orbit with updated orbit */
			// No modification of ID, version, mission code, spacecraft code or orbit number allowed
			if (null != updatedOrbit.getStartTime()) { // not null
				restOrbit.setStartTime(updatedOrbit.getStartTime());
			}
			if (null != updatedOrbit.getStopTime()) { // not null
				restOrbit.setStopTime(updatedOrbit.getStopTime());
			}
			
			/* Update orbit using Order Manager service */
			try {
				restOrbit = serviceConnection.patchToService(serviceConfig.getOrderManagerUrl(),
						URI_PATH_ORBITS + "/" + restOrbit.getId(), restOrbit, RestOrbit.class,
						loginManager.getUser(), loginManager.getPassword());
			} catch (RestClientResponseException e) {
				String message = null;
				switch (e.getRawStatusCode()) {
				case org.apache.http.HttpStatus.SC_NOT_FOUND:
					message = uiMsg(MSG_ID_ORBIT_NOT_FOUND_BY_ID, restOrbit.getId());
					break;
				case org.apache.http.HttpStatus.SC_BAD_REQUEST:
					message = uiMsg(MSG_ID_ORBIT_DATA_INVALID, e.getMessage());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				case org.apache.http.HttpStatus.SC_FORBIDDEN:
					message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), ORBITS, loginManager.getMission());
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
		}
		
		/* Report success, giving number of orbits updated */
		String message = uiMsg(MSG_ID_ORBITS_UPDATED, orbitList.size());
		logger.info(message);
		System.out.println(message);
	}
	
	/**
	 * Delete the given orbit range
	 * 
	 * @param deleteCommand the parsed "orbit delete" command
	 */
	private void deleteOrbit(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteOrbit({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get spacecraft code from command parameters */
		if (1 > deleteCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_ORBIT_IDENTIFIER_GIVEN));
			return;
		}
		String spacecraftCode = deleteCommand.getParameters().get(0).getValue();
		
		/* Get orbit range from command options */
		Integer fromOrbit = null;
		Integer toOrbit = null;
		for (ParsedOption option: deleteCommand.getOptions()) {
			switch(option.getName()) {
			case "from":
				fromOrbit = Integer.parseInt(option.getValue());
				break;
			case "to":
				toOrbit = Integer.parseInt(option.getValue());
				break;
			}
		}
		if (null == fromOrbit || null == toOrbit) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_ORBIT_IDENTIFIER_GIVEN));
			return;
		}
		
		/* Retrieve the orbits using Order Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getOrderManagerUrl(), 
					URI_PATH_ORBITS + "?spacecraftCode=" + spacecraftCode 
					+ "&orbitNumberFrom=" + fromOrbit 
					+ "&orbitNumberTo=" + toOrbit, 
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_NO_ORBITS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), ORBITS, loginManager.getMission());
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
			String message = uiMsg(MSG_ID_NO_ORBITS_FOUND);
			logger.error(message);
			System.err.println(message);
			return;
		}
		
		ObjectMapper mapper = new ObjectMapper();
		
		for (int i = 0; i < resultList.size(); i++) {
			RestOrbit restOrbit = mapper.convertValue(resultList.get(i), RestOrbit.class);
			/* Delete processor using Processor Manager service */
			try {
				serviceConnection.deleteFromService(serviceConfig.getOrderManagerUrl(),
						URI_PATH_ORBITS + "/" + restOrbit.getId(), loginManager.getUser(), loginManager.getPassword());
			} catch (RestClientResponseException e) {
				String message = null;
				switch (e.getRawStatusCode()) {
				case org.apache.http.HttpStatus.SC_NOT_FOUND:
					message = uiMsg(MSG_ID_ORBIT_NOT_FOUND_BY_ID, restOrbit.getId());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
					message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), ORBITS, loginManager.getMission());
					break;
				case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
					message = uiMsg(MSG_ID_ORBIT_DELETE_FAILED, restOrbit.getOrbitNumber(), spacecraftCode, e.getMessage());
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
		}
		/* Report success */
		String message = uiMsg(MSG_ID_ORBITS_DELETED, resultList.size());
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
			if (CMD_MISSION.equals(command.getName()) && null != command.getSubcommand() && CMD_SHOW.equals(command.getSubcommand().getName()) ) {
				// OK, "mission show" allowed without login
			} else {
				System.err.println(uiMsg(MSG_ID_USER_NOT_LOGGED_IN, command.getName()));
				return;
			}
		}
		if (null == loginManager.getMission()) {
			if (CMD_MISSION.equals(command.getName()) && null != command.getSubcommand() && 
					(CMD_SHOW.equals(command.getSubcommand().getName()) || CMD_CREATE.equals(command.getSubcommand().getName())
							|| CMD_DELETE.equals(command.getSubcommand().getName())) ) {
				// OK, "mission show", "mission create" and "mission delete" allowed without login to a specific mission
			} else {
				System.err.println(uiMsg(MSG_ID_USER_NOT_LOGGED_IN_TO_MISSION, command.getName()));
				return;
			}
		}
		
		/* Check argument */
		if (!CMD_MISSION.equals(command.getName()) && !CMD_ORBIT.equals(command.getName())) {
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
				
		/* Make sure a sub-subcommand is given for "spacecraft" */
		ParsedCommand subsubcommand = subcommand.getSubcommand();
		if (CMD_SPACECRAFT.equals(subcommand.getName()) && null == subcommand.getSubcommand()) {
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
			case CMD_CREATE:	createMission(subcommand); break COMMAND;
			case CMD_SHOW:		showMission(subcommand); break COMMAND;
			case CMD_UPDATE:	updateMission(subcommand); break COMMAND;
			case CMD_DELETE:	deleteMission(subcommand); break COMMAND;
			default:
				System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		case CMD_ORBIT:
			// Handle commands for configurations
			switch (subcommand.getName()) {
			case CMD_CREATE:	createOrbit(subcommand); break COMMAND;
			case CMD_SHOW:		showOrbit(subcommand); break COMMAND;
			case CMD_UPDATE:	updateOrbit(subcommand); break COMMAND;
			case CMD_DELETE:	deleteOrbit(subcommand); break COMMAND;
			default:
				System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		}
	}
}
