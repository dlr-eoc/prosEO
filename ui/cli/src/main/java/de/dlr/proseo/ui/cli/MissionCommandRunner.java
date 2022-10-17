/**
 * MissionCommandRunner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import java.io.File;
import java.io.IOException;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
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

	private static final String OPTION_DELETE_ATTRIBUTES = "delete-attributes";
	private static final String OPTION_DELETE_PRODUCTS = "delete-products";
	private static final String OPTION_FORCE = "force";
	private static final String OPTION_ORBIT_TO = "to";
	private static final String OPTION_ORBIT_FROM = "from";
	private static final String OPTION_VERBOSE = "verbose";
	private static final String OPTION_FORMAT = "format";
	private static final String OPTION_FILE = "file";

	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_MISSION_CODE = "Mission code (empty field cancels): ";
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
	private static ProseoLogger logger = new ProseoLogger(MissionCommandRunner.class);

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
						String message = logger.log(UIMessage.MISSION_NOT_READABLE, loginManager.getMission(), e.getMessage());
						System.err.println(message);
						return null;
					}
				}
			}
			String message = logger.log(UIMessage.MISSION_NOT_FOUND, missionCode);
			System.err.println(message);
			return null;
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = logger.log(UIMessage.MISSION_NOT_FOUND, loginManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						logger.log(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, "(" + e.getRawStatusCode() + ") " + e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			String message = logger.log(UIMessage.EXCEPTION, e.getMessage());
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
		
		/* Only allowed when not logged in to a mission! */
		if (null != loginManager.getMission()) {
			System.err.println(ProseoLogger.format(UIMessage.LOGGED_IN_TO_MISSION, loginManager.getMission()));
			return;
		}
		
		/* Check command options */
		File missionFile = null;
		String missionFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				missionFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
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
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
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
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restMission.getCode() || restMission.getCode().isBlank()) {
			System.out.print(PROMPT_MISSION_CODE);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restMission.setCode(response);
		}
		if (null == restMission.getName() || restMission.getName().isBlank()) {
			System.out.print(PROMPT_MISSION_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restMission.setName(response);
		}
		if (null == restMission.getProductFileTemplate() || restMission.getProductFileTemplate().isBlank()) {
			System.out.print(PROMPT_FILE_TEMPLATE);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
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
				message = logger.log(UIMessage.MISSION_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}

		/* Report success, giving newly assigned processor class ID */
		String message = logger.log(UIMessage.MISSION_CREATED,
				restMission.getCode(), restMission.getId());
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
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORMAT:
				missionOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
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
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
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
				message = logger.log(UIMessage.NO_MISSIONS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		if (isVerbose) {
			/* Display the mission(s) found */
			try {
				CLIUtil.printObject(System.out, resultList, missionOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			} 
		} else {
			// Must be a list of missions
			String listFormat = "%-6s %s";
			System.out.println(String.format(listFormat, "Code", "Name"));
			for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					Map<?, ?> resultMap = (Map<?, ?>) resultObject;
					System.out.println(String.format(listFormat, resultMap.get("code"), resultMap.get("name")));
				}
			}
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
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				missionFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				missionFileFormat = option.getValue().toUpperCase();
				break;
			case OPTION_DELETE_ATTRIBUTES:
				isDeleteAttributes = true;
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
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
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
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
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
		if (isDeleteAttributes || null != updatedMission.getProcessingCentre()) {
			restMission.setProcessingCentre(updatedMission.getProcessingCentre());
		}
		if (isDeleteAttributes || null != updatedMission.getProductRetentionPeriod()) {
			restMission.setProductRetentionPeriod(updatedMission.getProductRetentionPeriod());
		}
		if (isDeleteAttributes || null != updatedMission.getOrderRetentionPeriod()) {
			restMission.setOrderRetentionPeriod(updatedMission.getOrderRetentionPeriod());
		}
		
		/* Update mission using Order Manager service */
		try {
			restMission = serviceConnection.patchToService(serviceConfig.getOrderManagerUrl(),
					URI_PATH_MISSIONS + "/" + restMission.getId(),
					restMission, RestMission.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(ProseoLogger.format(UIMessage.NOT_MODIFIED));
				return;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = logger.log(UIMessage.MISSION_NOT_FOUND_BY_ID, restMission.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = logger.log(UIMessage.MISSION_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success, giving new processor class version */
		String message = logger.log(UIMessage.MISSION_UPDATED, restMission.getId(), restMission.getVersion());
		System.out.println(message);
	}

	/**
	 * Delete the given mission (optionally removing all configured items and all products, too)
	 * 
	 * @param deleteCommand the parsed "mission delete" command
	 */
	private void deleteMission(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteMission({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Only allowed when not logged in to a mission! */
		if (null != loginManager.getMission()) {
			System.err.println(ProseoLogger.format(UIMessage.LOGGED_IN_TO_MISSION, loginManager.getMission()));
			return;
		}
		
		/* Check command options */
		boolean forcedDelete = false;
		boolean deleteProducts = false;
		for (ParsedOption option: deleteCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORCE:
				forcedDelete = true;
				break;
			case OPTION_DELETE_PRODUCTS:
				deleteProducts = true;
				break;
			}
		}
		if (deleteProducts && !forcedDelete) {
			// "delete-products" only allowed with "force" to avoid unintentional data loss
			System.err.println(ProseoLogger.format(UIMessage.DELETE_PRODUCTS_WITHOUT_FORCE));
			return;
		}
		
		/* Get mission code from command parameters */
		if (deleteCommand.getParameters().isEmpty()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_MISSION_CODE_GIVEN));
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
				message = logger.log(UIMessage.MISSION_NOT_FOUND, missionCode);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = logger.log(UIMessage.MISSION_DELETE_FAILED, missionCode, e.getMessage());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		/* Report success */
		String message = logger.log(UIMessage.MISSION_DELETED, missionCode);
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
			case OPTION_FILE:
				processorFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
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
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
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
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
		if (null == restSpacecraft.getCode() || 0 == restSpacecraft.getCode().length()) {
			System.out.print(PROMPT_SPACECRAFT_CODE);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restSpacecraft.setCode(response);
		}
		if (null == restSpacecraft.getName() || 0 == restSpacecraft.getName().length()) {
			System.out.print(PROMPT_SPACECRAFT_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
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
				String message = logger.log(UIMessage.SPACECRAFT_EXISTS,
						restSpacecraft.getCode(), loginManager.getMission());
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
				message = logger.log(UIMessage.MISSION_NOT_FOUND_BY_ID, restMission.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = logger.log(UIMessage.MISSION_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						logger.log(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, "(" + e.getRawStatusCode() + ") " + e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			e.printStackTrace(System.err);
			return;
		}

		/* Report success, giving newly assigned processor ID and version */
		for (RestSpacecraft missionSpacecraft: restMission.getSpacecrafts()) {
			if (restSpacecraft.getCode().equals(missionSpacecraft.getCode())) {
				String message = logger.log(UIMessage.SPACECRAFT_ADDED,
						missionSpacecraft.getCode(), missionSpacecraft.getId());
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
			System.err.println(ProseoLogger.format(UIMessage.NO_SPACECRAFT_CODE_GIVEN));
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
			String message = logger.log(UIMessage.SPACECRAFT_NOT_FOUND, spacecraftCode, loginManager.getMission());
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
				message = logger.log(UIMessage.MISSION_NOT_FOUND_BY_ID, restMission.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = logger.log(UIMessage.MISSION_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), MISSIONS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}

		/* Report success, giving newly assigned processor ID and version */
		String message = logger.log(UIMessage.SPACECRAFT_REMOVED, spacecraftCode, restMission.getCode());
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
			case OPTION_FILE:
				orbitFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
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
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Retrieve the mission to get valid spacecraft codes */
		RestMission mission = retrieveMissionByCode(loginManager.getMission());
		if (null == mission) {
			// Already handled
			return;
		}
		List<String> validSpacecraftCodes = new ArrayList<>();
		for (RestSpacecraft spacecraft: mission.getSpacecrafts()) {
			validSpacecraftCodes.add(spacecraft.getCode());
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
					String spacecraftCode = param.getValue();
					if (!validSpacecraftCodes.contains(spacecraftCode)) {
						System.err.println(ProseoLogger.format(UIMessage.SPACECRAFT_NOT_FOUND, spacecraftCode, loginManager.getMission()));
						return;
					}
					restOrbit.setSpacecraftCode(spacecraftCode);
				} else {
					// Remaining parameters are "attribute=value" parameters
					try {
						CLIUtil.setAttribute(restOrbit, param.getValue());
					} catch (Exception e) {
						System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
						return;
					}
				}
			}
			/* Prompt user for missing mandatory attributes */
			System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);
			while (null == restOrbit.getSpacecraftCode() || restOrbit.getSpacecraftCode().isBlank()) {
				System.out.print(PROMPT_SPACECRAFT_CODE);
				String response = System.console().readLine();
				if (response.isBlank()) {
					System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
					return;
				}
				if (!validSpacecraftCodes.contains(response)) {
					System.err.println(ProseoLogger.format(UIMessage.SPACECRAFT_NOT_FOUND, response, loginManager.getMission()));
					continue;
				}
				restOrbit.setSpacecraftCode(response);
			}
			while (null == restOrbit.getOrbitNumber() || 0 == restOrbit.getOrbitNumber().longValue()) {
				System.out.print(PROMPT_ORBIT_NUMBER);
				String response = System.console().readLine();
				if (response.isBlank()) {
					System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
					return;
				}
				try {
					restOrbit.setOrbitNumber(Long.parseLong(response));
				} catch (NumberFormatException e) {
					System.err.println(ProseoLogger.format(UIMessage.ORBIT_NUMBER_INVALID, response));
				}
			}
			while (null == restOrbit.getStartTime() || restOrbit.getStartTime().isBlank()) {
				System.out.print(PROMPT_START_TIME);
				String response = System.console().readLine();
				if (response.isBlank()) {
					System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
					return;
				}
				try {
					restOrbit.setStartTime(OrbitTimeFormatter.format(CLIUtil.parseDateTime(response))); // no time zone in input expected
				} catch (DateTimeException e) {
					System.err.println(ProseoLogger.format(UIMessage.INVALID_TIME, response));
				}
			}
			while (null == restOrbit.getStopTime() || restOrbit.getStopTime().isBlank()) {
				System.out.print(PROMPT_STOP_TIME);
				String response = System.console().readLine();
				if (response.isBlank()) {
					System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
					return;
				}
				try {
					restOrbit.setStopTime(OrbitTimeFormatter.format(CLIUtil.parseDateTime(response))); // no time zone in input expected
				} catch (DateTimeException e) {
					System.err.println(ProseoLogger.format(UIMessage.INVALID_TIME, response));
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
				message = logger.log(UIMessage.ORBIT_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ORBITS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}

		/* Report success, giving number of orbits created */
		String message = logger.log(UIMessage.ORBITS_CREATED, orbitList.size());
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
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_ORBIT_FROM:
				fromOrbit = Integer.parseInt(option.getValue());
				break;
			case OPTION_ORBIT_TO:
				toOrbit = Integer.parseInt(option.getValue());
				break;
			case OPTION_FORMAT:
				orbitOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
				break;
			}
		}
		
		/* Retrieve the mission to get valid spacecraft codes */
		RestMission mission = retrieveMissionByCode(loginManager.getMission());
		if (null == mission) {
			// Already handled
			return;
		}
		List<String> validSpacecraftCodes = new ArrayList<>();
		for (RestSpacecraft spacecraft: mission.getSpacecrafts()) {
			validSpacecraftCodes.add(spacecraft.getCode());
		}
		
		// First parameter is spacecraft code
		String spacecraftCode = showCommand.getParameters().get(0).getValue();
		if (!validSpacecraftCodes.contains(spacecraftCode)) {
			System.err.println(ProseoLogger.format(UIMessage.SPACECRAFT_NOT_FOUND, spacecraftCode, loginManager.getMission()));
			return;
		}

		/* Prepare request URI */
		if (1 > showCommand.getParameters().size()) {
			System.err.println(ProseoLogger.format(UIMessage.NO_SPACECRAFT_CODE_GIVEN));
			return;
		}
		String requestURI = URI_PATH_ORBITS + "?spacecraftCode=" + spacecraftCode;
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
				message = logger.log(UIMessage.NO_ORBITS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ORBITS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		
		if (isVerbose) {
			/* Display the orbits found */
			try {
				CLIUtil.printObject(System.out, resultList, orbitOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			} 
		} else {
			// Must be a list of orbits
			String listFormat = "%-6s %05d %-26s %-26s";
			System.out.println(String.format("%-6s %-5s %-26s %-26s", "S/C", "Orb-#", "Sensing Start", "Sensing Stop"));
			for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
				if (resultObject instanceof Map) {
					Map<?, ?> resultMap = (Map<?, ?>) resultObject;
					System.out.println(String.format(listFormat, resultMap.get("spacecraftCode"), resultMap.get("orbitNumber"),
							resultMap.get("startTime"), resultMap.get("stopTime")));
				}
			}
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
			case OPTION_FILE:
				orbitFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
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
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Retrieve the mission to get valid spacecraft codes */
		RestMission mission = retrieveMissionByCode(loginManager.getMission());
		if (null == mission) {
			// Already handled
			return;
		}
		List<String> validSpacecraftCodes = new ArrayList<>();
		for (RestSpacecraft spacecraft: mission.getSpacecrafts()) {
			validSpacecraftCodes.add(spacecraft.getCode());
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
					String spacecraftCode = param.getValue();
					if (!validSpacecraftCodes.contains(spacecraftCode)) {
						System.err.println(ProseoLogger.format(UIMessage.SPACECRAFT_NOT_FOUND, spacecraftCode, loginManager.getMission()));
						return;
					}
					restOrbit.setSpacecraftCode(spacecraftCode);
				} else if (1 == i) {
					// Second parameter is orbit number
					try {
						restOrbit.setOrbitNumber(Long.parseLong(param.getValue()));
					} catch (NumberFormatException e) {
						System.err.println(ProseoLogger.format(UIMessage.ORBIT_NUMBER_INVALID, param.getValue()));
						return;
					}
					;
				} else {
					// Remaining parameters are "attribute=value" parameters
					try {
						CLIUtil.setAttribute(restOrbit, param.getValue());
					} catch (Exception e) {
						System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
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
			if (null == updatedOrbit.getId() || 0 == updatedOrbit.getId().longValue()) {
				// No database ID given, check for natural keys
				if (null == updatedOrbit.getSpacecraftCode() || updatedOrbit.getSpacecraftCode().isBlank()) {
					// No spacecraft code given
					System.err.println(ProseoLogger.format(UIMessage.NO_SPACECRAFT_CODE_GIVEN));
					return;
				} else if (null == updatedOrbit.getOrbitNumber() || 0 == updatedOrbit.getOrbitNumber().longValue()) {
					// No orbit number given
					System.err.println(ProseoLogger.format(UIMessage.NO_ORBIT_NUMBER_GIVEN));
					return;
				}
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
						String message = logger.log(UIMessage.ORBIT_NOT_FOUND,
								updatedOrbit.getOrbitNumber(), updatedOrbit.getSpacecraftCode());
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
					message = logger.log(UIMessage.ORBIT_NOT_FOUND,
							updatedOrbit.getOrbitNumber(), updatedOrbit.getSpacecraftCode());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				case org.apache.http.HttpStatus.SC_FORBIDDEN:
					message = (null == e.getStatusText() ?
							logger.log(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ORBITS, loginManager.getMission()) :
							e.getStatusText());
					break;
				default:
					message = logger.log(UIMessage.EXCEPTION, e.getMessage());
				}
				System.err.println(message);
				return;
			} catch (RuntimeException e) {
				String message = logger.log(UIMessage.EXCEPTION, e.getMessage());
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
				case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
					System.out.println(ProseoLogger.format(UIMessage.NOT_MODIFIED));
					return;
				case org.apache.http.HttpStatus.SC_NOT_FOUND:
					message = logger.log(UIMessage.ORBIT_NOT_FOUND_BY_ID, restOrbit.getId());
					break;
				case org.apache.http.HttpStatus.SC_BAD_REQUEST:
					message = logger.log(UIMessage.ORBIT_DATA_INVALID, e.getStatusText());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				case org.apache.http.HttpStatus.SC_FORBIDDEN:
					message = (null == e.getStatusText() ?
							ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ORBITS, loginManager.getMission()) :
							e.getStatusText());
					break;
				default:
					message = logger.log(UIMessage.EXCEPTION, e.getMessage());
				}
				System.err.println(message);
				return;
			} catch (RuntimeException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			} 
		}
		
		/* Report success, giving number of orbits updated */
		String message = logger.log(UIMessage.ORBITS_UPDATED, orbitList.size());
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
			System.err.println(ProseoLogger.format(UIMessage.NO_SPACECRAFT_CODE_GIVEN));
			return;
		}

		/* Retrieve the mission to get valid spacecraft codes */
		RestMission mission = retrieveMissionByCode(loginManager.getMission());
		if (null == mission) {
			// Already handled
			return;
		}
		List<String> validSpacecraftCodes = new ArrayList<>();
		for (RestSpacecraft spacecraft: mission.getSpacecrafts()) {
			validSpacecraftCodes.add(spacecraft.getCode());
		}
		
		// First parameter is spacecraft code
		String spacecraftCode = deleteCommand.getParameters().get(0).getValue();
		if (!validSpacecraftCodes.contains(spacecraftCode)) {
			System.err.println(ProseoLogger.format(UIMessage.SPACECRAFT_NOT_FOUND, spacecraftCode, loginManager.getMission()));
			return;
		}

		/* Get "from" orbit from command parameters */
		if (2 > deleteCommand.getParameters().size()) {
			System.err.println(ProseoLogger.format(UIMessage.NO_ORBIT_NUMBER_GIVEN));
			return;
		}
		Integer fromOrbit = Integer.parseInt(deleteCommand.getParameters().get(1).getValue());
		/* Check for "to" orbit in command parameters (optional) */
		Integer toOrbit = null;
		if (3 == deleteCommand.getParameters().size()) {
			toOrbit = Integer.parseInt(deleteCommand.getParameters().get(2).getValue());
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
				message = logger.log(UIMessage.NO_ORBITS_FOUND);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ORBITS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = logger.log(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return;
		} catch (Exception e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return;
		}
		if (resultList.isEmpty()) {
			String message = logger.log(UIMessage.NO_ORBITS_FOUND);
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
					message = logger.log(UIMessage.ORBIT_NOT_FOUND_BY_ID, restOrbit.getId());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
					message = (null == e.getStatusText() ?
							ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), ORBITS, loginManager.getMission()) :
							e.getStatusText());
					break;
				case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
					message = logger.log(UIMessage.ORBIT_DELETE_FAILED, restOrbit.getOrbitNumber(), spacecraftCode, e.getMessage());
					break;
				default:
					message = logger.log(UIMessage.EXCEPTION, e.getMessage());
				}
				System.err.println(message);
				return;
			} catch (Exception e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			} 
		}
		/* Report success */
		String message = logger.log(UIMessage.ORBITS_DELETED, resultList.size());
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
				System.err.println(ProseoLogger.format(UIMessage.USER_NOT_LOGGED_IN, command.getName()));
				return;
			}
		}
		if (null == loginManager.getMission()) {
			if (CMD_MISSION.equals(command.getName()) && null != command.getSubcommand() && 
					(CMD_SHOW.equals(command.getSubcommand().getName()) || CMD_CREATE.equals(command.getSubcommand().getName())
							|| CMD_DELETE.equals(command.getSubcommand().getName())) ) {
				// OK, "mission show", "mission create" and "mission delete" allowed without login to a specific mission
			} else {
				System.err.println(ProseoLogger.format(UIMessage.USER_NOT_LOGGED_IN_TO_MISSION, command.getName()));
				return;
			}
		}
		
		/* Check argument */
		if (!CMD_MISSION.equals(command.getName()) && !CMD_ORBIT.equals(command.getName())) {
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
				
		/* Make sure a sub-subcommand is given for "spacecraft" */
		ParsedCommand subsubcommand = subcommand.getSubcommand();
		if (CMD_SPACECRAFT.equals(subcommand.getName()) && null == subcommand.getSubcommand()) {
			System.err.println(ProseoLogger.format(UIMessage.SUBCOMMAND_MISSING, subcommand.getName()));
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
					System.err.println(ProseoLogger.format(UIMessage.COMMAND_NOT_IMPLEMENTED, 
							command.getName() + " " + subcommand.getName() + " " + subsubcommand.getName()));
					return;
				}
			// Handle commands for processors
			case CMD_CREATE:	createMission(subcommand); break COMMAND;
			case CMD_SHOW:		showMission(subcommand); break COMMAND;
			case CMD_UPDATE:	updateMission(subcommand); break COMMAND;
			case CMD_DELETE:	deleteMission(subcommand); break COMMAND;
			default:
				System.err.println(ProseoLogger.format(UIMessage.COMMAND_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
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
				System.err.println(ProseoLogger.format(UIMessage.COMMAND_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		}
	}
}
