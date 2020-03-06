/**
 * UserCommandRunner.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.model.rest.model.RestGroup;
import de.dlr.proseo.model.rest.model.RestProcessor;
import de.dlr.proseo.model.rest.model.RestProcessorClass;
import de.dlr.proseo.model.rest.model.RestUser;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;
import de.dlr.proseo.ui.cli.parser.ParsedParameter;

/**
 * Run commands for managing prosEO user accounts and user groups (create, read, update, delete etc.). 
 * All methods assume that before invocation a syntax check of the command has been performed, so no extra checks are performed.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class UserCommandRunner {

	/* General string constants */
	public static final String CMD_USER = "user";
	public static final String CMD_GROUP = "group";
	private static final String CMD_ADD = "add";
	private static final String CMD_REMOVE = "remove";
	private static final String CMD_GRANT = "grant";
	private static final String CMD_REVOKE = "revoke";
	private static final String CMD_ENABLE = "enable";
	private static final String CMD_DISABLE = "disable";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";

	private static final String MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES = "Checking for missing mandatory attributes ...";
	private static final String PROMPT_USER_NAME = "User name (empty field cancels): ";
	private static final String PROMPT_GROUP_NAME = "Group name (empty field cancels): ";
	private static final String PROMPT_PASSWORD = "Password (unencrypted; empty field cancels): ";
	private static final String PROMPT_PASSWORD_REPEAT = "Repeat Password: ";
	
	private static final String URI_PATH_USERS = "/users";
	private static final String URI_PATH_GROUPS = "/groups";
	
	private static final String USERS = "user accounts";
	private static final String GROUPS = "user groups";

	
	/** The user manager used by all command runners */
	@Autowired
	private LoginManager loginManager;
	
	/** The configuration object for the prosEO backend services */
	@Autowired
	private ServiceConfiguration serviceConfig;
	
	/** The connector service to the prosEO backend services */
	@Autowired
	private ServiceConnection serviceConnection;
	
	/** A BCrypt password encoder (used only in this class) */
	private static BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(UserCommandRunner.class);


	/**
	 * Read a user by username from the User Manager
	 * 
	 * @param username the username to use as key
	 * @return a user object or null, if no user with the given username could be found
	 * @throws IllegalArgumentException if the conversion of the response body to a user object failed
	 *         (probably an error of the underlying service)
	 */
	private RestUser readUser(String username) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> readUser({})", username);
		
		if (null == username || username.isBlank()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_USERNAME_GIVEN));
			return null;
		}
		RestUser restUser = null;
		try {
			restUser = serviceConnection.getFromService(serviceConfig.getUserManagerUrl(),
					URI_PATH_USERS + "/" + username,
					RestUser.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_USER_NOT_FOUND_BY_NAME, username, loginManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return null;
		}

		return restUser;
	}

	/**
	 * Update the user with the given values
	 * 
	 * @param restUser the user data to update
	 * @return the modified user object or null, if an error occurred
	 */
	private RestUser modifyUser(RestUser restUser) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyUser({})", (null == restUser ? "null" : restUser.getUsername()));
		
		try {
			restUser = serviceConnection.patchToService(serviceConfig.getUserManagerUrl(),
					URI_PATH_USERS + "/" + restUser.getUsername(),
					restUser, RestUser.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_USER_NOT_FOUND_BY_NAME, restUser.getUsername(), loginManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_USER_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return null;
		}
		return restUser;
	}

	/**
	 * Read a group by group name from the User Manager
	 * 
	 * @param groupName the group name to use as key
	 * @return a group object or null, if no group with the given name could be found
	 * @throws IllegalArgumentException if the conversion of the response body to a group object failed
	 *         (probably an error of the underlying service)
	 */
	private RestGroup readGroup(String groupName) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> readGroup({})", groupName);
		
		if (null == groupName || groupName.isBlank()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_GROUPNAME_GIVEN));
			return null;
		}
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getUserManagerUrl(),
					URI_PATH_GROUPS + "?mission=" + loginManager.getMission() + "&groupName=" + groupName,
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_GROUP_NOT_FOUND_BY_NAME, groupName,  loginManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), GROUPS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return null;
		}
		if (resultList.isEmpty()) {
			String message = uiMsg(MSG_ID_GROUP_NOT_FOUND_BY_NAME, groupName,  loginManager.getMission());
			logger.error(message);
			System.err.println(message);
			return null;
		}

		return (new ObjectMapper()).convertValue(resultList.get(0), RestGroup.class);
	}

	/**
	 * Update the group with the given values
	 * 
	 * @param restGroup the group data to update
	 * @return the modified group object or null, if an error occurred
	 */
	private RestGroup modifyGroup(RestGroup restGroup) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyGroup({})", (null == restGroup ? "null" : restGroup.getGroupname()));
		
		try {
			restGroup = serviceConnection.patchToService(serviceConfig.getUserManagerUrl(),
					URI_PATH_GROUPS + "/" + restGroup.getId(),
					restGroup, RestGroup.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_GROUP_NOT_FOUND_BY_ID, restGroup.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_USER_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), GROUPS, loginManager.getMission());
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return null;
		}
		return restGroup;
	}

	/**
	 * Create a new user account; if the input is not from a file, the user will be prompted for mandatory attributes
	 * not given on the command line
	 * 
	 * @param createCommand the parsed "user create" command
	 */
	private void createUser(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createUser({})", (null == createCommand ? "null" : createCommand));
		
		/* Check command options */
		File userAccountFile = null;
		String userAccountFileFormat = CLIUtil.FILE_FORMAT_JSON;
		String missionCode = loginManager.getMission();
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				userAccountFile = new File(option.getValue());
				break;
			case "format":
				userAccountFileFormat = option.getValue().toUpperCase();
				break;
			case "mission":
				if (null == missionCode) {
					missionCode = option.getValue().toUpperCase();
				} else {
					System.err.println(uiMsg(MSG_ID_MISSION_ALREADY_SET, missionCode));
					return;
				}
				break;
			}
		}
		
		/* Read user account file, if any */
		RestUser restUser = null;
		if (null == userAccountFile) {
			restUser = new RestUser();
		} else {
			try {
				restUser = CLIUtil.parseObjectFile(userAccountFile, userAccountFileFormat, RestUser.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from processor class file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is user account name
				restUser.setUsername((null == missionCode ? "" : missionCode + "-") + param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restUser, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Set missing attributes to default values where possible */
		if (null == restUser.getEnabled()) {
			restUser.setEnabled(true);
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);

		if (null == restUser.getUsername() || restUser.getUsername().isEmpty()) {
			System.out.print(PROMPT_USER_NAME);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restUser.setUsername((null == missionCode ? "" : missionCode + "-") + response);
		}
		while (null == restUser.getPassword() || restUser.getPassword().isEmpty()) {
			System.out.print(PROMPT_PASSWORD);
			String response = new String(System.console().readPassword());
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			System.out.print(PROMPT_PASSWORD_REPEAT);
			String response2 = new String(System.console().readPassword());
			if (!response.equals(response2)) {
				System.out.println(uiMsg(MSG_ID_PASSWORD_MISMATCH));
				continue;
			}
			restUser.setPassword(passwordEncoder.encode(response));
		}
		
		/* Create user account */
		try {
			if (logger.isTraceEnabled()) {
				try {
					logger.trace("... creating user from REST data: " + (new ObjectMapper()).writeValueAsString(restUser));
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			restUser = serviceConnection.postToService(serviceConfig.getUserManagerUrl(), URI_PATH_USERS, 
					restUser, RestUser.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_USER_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission());
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

		/* Report success */
		String message = uiMsg(MSG_ID_USER_CREATED, restUser.getUsername());
		logger.info(message);
		System.out.println(message);
	}

	/**
	 * Show the user account specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "user show" command
	 */
	private void showUser(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showUser({})", (null == showCommand ? "null" : showCommand));
		
		/* Check command options */
		String userAccountOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case "format":
				userAccountOutputFormat = option.getValue().toUpperCase();
				break;
			case "verbose":
				isVerbose = true;
				break;
			}
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_USERS;
		Object result = null;
		
		if (showCommand.getParameters().isEmpty()) {
			requestURI += "?mission=" + loginManager.getMission();

			/* Get the user account information from the User Manager service */
			try {
				result = serviceConnection.getFromService(serviceConfig.getUserManagerUrl(),
						requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
			} catch (RestClientResponseException e) {
				String message = null;
				switch (e.getRawStatusCode()) {
				case org.apache.http.HttpStatus.SC_NOT_FOUND:
					message = uiMsg(MSG_ID_NO_USERS_FOUND, loginManager.getMission());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
					message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission());
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
		} else {
			// Only user name allowed as parameter
			String username = showCommand.getParameters().get(0).getValue();
			isVerbose = true; // implied, if only a single user is requested
			result = readUser(loginManager.getMission() + "-" + username);
			if (null == result)	{
				// Error handled by called method
				return;
			}
		}
		
		
		/* Display the user account(s) found */
		if (isVerbose) {
			try {
				CLIUtil.printObject(System.out, result, userAccountOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		} else {
			// Must be a list of users
			for (Object resultObject: (new ObjectMapper()).convertValue(result, List.class)) {
				if (resultObject instanceof Map) {
					System.out.println(((Map<String, String>) resultObject).get("username"));
				}
			}
		}
	}

	/**
	 * Update a user from a user file or from "attribute=value" pairs (overriding any user file entries)
	 * 
	 * @param updateCommand the parsed "user update" command
	 */
	private void updateUser(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateUser({})", (null == updateCommand ? "null" : updateCommand.getName()));
		
		/* Check command options */
		File userFile = null;
		String userFileFormat = CLIUtil.FILE_FORMAT_JSON;
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				userFile = new File(option.getValue());
				break;
			case "format":
				userFileFormat = option.getValue().toUpperCase();
				break;
			case "delete-attributes":
				isDeleteAttributes = true;
				break;
			}
		}
		
		/* Read user file, if any */
		RestUser updatedUser = null;
		if (null == userFile) {
			updatedUser = new RestUser();
		} else {
			try {
				updatedUser = CLIUtil.parseObjectFile(userFile, userFileFormat, RestUser.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from user file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is processor class name
				updatedUser.setUsername(loginManager.getMission() + "-" + param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					// Handle special case: Password is unencrypted on command line
					if (param.getValue().startsWith("password=")) {
						// Due to parsing guaranteed to be "password=value"
						param.setValue("password=" + passwordEncoder.encode(param.getValue().split("=")[1]));
					}
					CLIUtil.setAttribute(updatedUser, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Read original user from User service */
		RestUser restUser = readUser(updatedUser.getUsername());
		if (null == restUser) {
			// Error handled by called method
			return;
		}

		/* Compare attributes of database user with updated user */
		// No modification of username allowed
		if (null != updatedUser.getPassword() && !updatedUser.getPassword().isBlank()) {
			restUser.setPassword(updatedUser.getPassword());
		}
		if (null != updatedUser.getEnabled()) {
			restUser.setEnabled(updatedUser.getEnabled());
		}
		if (null != updatedUser.getAuthorities() && (isDeleteAttributes || !updatedUser.getAuthorities().isEmpty())) {
			restUser.getAuthorities().clear();
			restUser.getAuthorities().addAll(updatedUser.getAuthorities());
		}
		if (isDeleteAttributes || null != updatedUser.getExpirationDate()) {
			if (null == updatedUser.getExpirationDate()) {
				restUser.setExpirationDate(Date.from(Instant.now().plus(100, ChronoUnit.YEARS)));
			} else {
				restUser.setExpirationDate(updatedUser.getExpirationDate());
			}
		}
		if (isDeleteAttributes || null != updatedUser.getPasswordExpirationDate()) {
			if (null == updatedUser.getPasswordExpirationDate()) {
				restUser.setPasswordExpirationDate(Date.from(Instant.now().plus(100, ChronoUnit.YEARS)));
			} else {
				restUser.setPasswordExpirationDate(updatedUser.getPasswordExpirationDate());
			}
		}
		
		/* Update user using User Manager service */
		restUser = modifyUser(restUser);
		if (null == restUser) {
			// Error handled by called method;
			return;
		}
		
		/* Report success */
		String message = uiMsg(MSG_ID_USER_UPDATED, restUser.getUsername());
		logger.info(message);
		System.out.println(message);
	}

	/**
	 * Delete the given user from the current mission
	 * 
	 * @param deleteCommand the parsed "user delete" command
	 */
	private void deleteUser(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteUser({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Check command options */
		String missionCode = loginManager.getMission();
		for (ParsedOption option: deleteCommand.getOptions()) {
			switch(option.getName()) {
			case "mission":
				if (null == missionCode) {
					missionCode = option.getValue().toUpperCase();
				} else {
					System.err.println(uiMsg(MSG_ID_MISSION_ALREADY_SET, missionCode));
					return;
				}
				break;
			}
		}
		
		/* Get username from command parameters */
		if (1 > deleteCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_USERNAME_GIVEN));
			return;
		}
		String username = (null == missionCode ? "" : missionCode + "-") + deleteCommand.getParameters().get(0).getValue();
		
		/* Retrieve the user using User Manager service */
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getProcessorManagerUrl(), 
					URI_PATH_USERS + "/" + username, 
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_USER_NOT_FOUND_BY_NAME, username);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission());
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
			String message = uiMsg(MSG_ID_USER_NOT_FOUND_BY_NAME, username);
			logger.error(message);
			System.err.println(message);
			return;
		}
		ObjectMapper mapper = new ObjectMapper();
		RestUser restUser = mapper.convertValue(resultList.get(0), RestUser.class);
		
		/* Delete user using User Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getProcessorManagerUrl(),
					URI_PATH_USERS + "/" + restUser.getUsername(), 
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_USER_NOT_FOUND_BY_NAME, restUser.getUsername());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = uiMsg(MSG_ID_USER_DELETE_FAILED, username, e.getMessage());
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
		String message = uiMsg(MSG_ID_USER_DELETED, restUser.getUsername());
		logger.info(message);
		System.out.println(message);
	}

	/**
	 * Enable the given user for the current mission
	 *
	 * @param command the "user enable" command
	 */
	private void enableUser(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> enableUser({})", (null == command ? "null" : command.getName()));

		/* Get username from command parameters */
		if (1 > command.getParameters().size()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_USERNAME_GIVEN));
			return;
		}
		String username = loginManager.getMission() + "-" + command.getParameters().get(0).getValue();
		
		/* Read original user from User service */
		RestUser restUser = readUser(username);
		if (null == restUser) {
			// Error handled by called method
			return;
		}
		
		/* Set the user account to enabled */
		restUser.setEnabled(true);

		/* Update user using User Manager service */
		restUser = modifyUser(restUser);
		if (null == restUser) {
			// Error handled by called method;
			return;
		}
		
		/* Report success */
		String message = uiMsg(MSG_ID_USER_ENABLED, restUser.getUsername());
		logger.info(message);
		System.out.println(message);
	}

	/**
	 * Disable the given user for the current mission
	 *
	 * @param command the "user disable" command
	 */
	private void disableUser(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> enableUser({})", (null == command ? "null" : command.getName()));

		/* Get username from command parameters */
		if (1 > command.getParameters().size()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_USERNAME_GIVEN));
			return;
		}
		String username = loginManager.getMission() + "-" + command.getParameters().get(0).getValue();
		
		/* Read original user from User service */
		RestUser restUser = readUser(username);
		if (null == restUser) {
			// Error handled by called method
			return;
		}
		
		/* Set the user account to disabled */
		restUser.setEnabled(false);

		/* Update user using User Manager service */
		restUser = modifyUser(restUser);
		if (null == restUser) {
			// Error handled by called method;
			return;
		}
		
		/* Report success */
		String message = uiMsg(MSG_ID_USER_DISABLED, restUser.getUsername());
		logger.info(message);
		System.out.println(message);
	}

	/**
	 * Grant the given authorities to the given user for the current mission
	 *
	 * @param command the "user grant" command
	 */
	private void grantAuthority(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> grantAuthority({})", (null == command ? "null" : command.getName()));

		/* Get username from command parameters */
		if (1 > command.getParameters().size()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_USERNAME_GIVEN));
			return;
		}
		String username = loginManager.getMission() + "-" + command.getParameters().get(0).getValue();
		
		/* Get granted authorities from command parameters */
		List<String> authorities = new ArrayList<>();
		for (int i = 1; i < command.getParameters().size(); ++i) {
			authorities.add(command.getParameters().get(i).getValue());
		}
		if (authorities.isEmpty()) {
			// No authorities to grant given
			System.err.println(uiMsg(MSG_ID_NO_AUTHORITIES_GIVEN));
			return;
		}
		
		/* Read original user from User service */
		RestUser restUser = readUser(username);
		if (null == restUser) {
			// Error handled by called method
			return;
		}
		
		/* Add the given authorities */
		for (String authority: authorities) {
			if (!restUser.getAuthorities().contains(authority)) {
				restUser.getAuthorities().add(authority);
			}
		}

		/* Update user using User Manager service */
		restUser = modifyUser(restUser);
		if (null == restUser) {
			// Error handled by called method;
			return;
		}
		
		/* Report success */
		String message = uiMsg(MSG_ID_AUTHORITIES_GRANTED, Arrays.toString(authorities.toArray()), restUser.getUsername());
		logger.info(message);
		System.out.println(message);
	}

	/**
	 * Revoke the given authorities from the given user for the current mission
	 *
	 * @param command the "user revoke" command
	 */
	private void revokeAuthority(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> revokeAuthority({})", (null == command ? "null" : command.getName()));

		/* Get username from command parameters */
		if (1 > command.getParameters().size()) {
			// No identifying value given
			System.err.println(uiMsg(MSG_ID_NO_USERNAME_GIVEN));
			return;
		}
		String username = loginManager.getMission() + "-" + command.getParameters().get(0).getValue();
		
		/* Get granted authorities from command parameters */
		List<String> authorities = new ArrayList<>();
		for (int i = 1; i < command.getParameters().size(); ++i) {
			authorities.add(command.getParameters().get(i).getValue());
		}
		if (authorities.isEmpty()) {
			// No authorities to grant given
			System.err.println(uiMsg(MSG_ID_NO_AUTHORITIES_GIVEN));
			return;
		}
		
		/* Read original user from User service */
		RestUser restUser = readUser(username);
		if (null == restUser) {
			// Error handled by called method
			return;
		}
		
		/* Remove the given authorities */
		restUser.getAuthorities().removeAll(authorities);

		/* Update user using User Manager service */
		restUser = modifyUser(restUser);
		if (null == restUser) {
			// Error handled by called method;
			return;
		}
		
		/* Report success */
		String message = uiMsg(MSG_ID_AUTHORITIES_REVOKED, Arrays.toString(authorities.toArray()), restUser.getUsername());
		logger.info(message);
		System.out.println(message);
	}

	/**
	 * Create a new user group; if the input is not from a file, the user will be prompted for mandatory attributes
	 * not given on the command line
	 * 
	 * @param createCommand the parsed "group create" command
	 */
	private void createGroup(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createGroup({})", (null == createCommand ? "null" : createCommand));
		
		/* Check command options */
		File groupAccountFile = null;
		String groupAccountFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				groupAccountFile = new File(option.getValue());
				break;
			case "format":
				groupAccountFileFormat = option.getValue().toUpperCase();
				break;
			}
		}
		
		/* Read user group file, if any */
		RestGroup restGroup = null;
		if (null == groupAccountFile) {
			restGroup = new RestGroup();
		} else {
			try {
				restGroup = CLIUtil.parseObjectFile(groupAccountFile, groupAccountFileFormat, RestGroup.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from processor class file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is user group name
				restGroup.setGroupname(param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restGroup, param.getValue());
				} catch (Exception e) {
					System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Prompt user for missing mandatory attributes */
		System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);

		if (null == restGroup.getGroupname() || restGroup.getGroupname().isEmpty()) {
			System.out.print(PROMPT_GROUP_NAME);
			String response = System.console().readLine();
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restGroup.setGroupname(loginManager.getMission() + "-" + response);
		}
		
		/* Create user group */
		try {
			restGroup = serviceConnection.postToService(serviceConfig.getUserManagerUrl(), URI_PATH_GROUPS, 
					restGroup, RestGroup.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = uiMsg(MSG_ID_GROUP_DATA_INVALID, e.getMessage());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission());
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

		/* Report success */
		String message = uiMsg(MSG_ID_GROUP_CREATED, restGroup.getGroupname());
		logger.info(message);
		System.out.println(message);
	}

	/**
	 * Show the user group(s) specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "group show" command
	 */
	private void showGroup(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showUser({})", (null == showCommand ? "null" : showCommand));
		
		/* Check command options */
		String userAccountOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case "format":
				userAccountOutputFormat = option.getValue().toUpperCase();
				break;
			case "verbose":
				isVerbose = true;
				break;
			}
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_GROUPS;
		Object result = null;
		
		if (showCommand.getParameters().isEmpty()) {
			requestURI += "?mission=" + loginManager.getMission();

			/* Get the user account information from the User Manager service */
			try {
				result = serviceConnection.getFromService(serviceConfig.getUserManagerUrl(),
						requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
			} catch (RestClientResponseException e) {
				String message = null;
				switch (e.getRawStatusCode()) {
				case org.apache.http.HttpStatus.SC_NOT_FOUND:
					message = uiMsg(MSG_ID_NO_GROUPS_FOUND, loginManager.getMission());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
					message = uiMsg(MSG_ID_NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission());
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
		} else {
			// Only user name allowed as parameter
			String groupName = showCommand.getParameters().get(0).getValue();
			isVerbose = true; // implied, if only a single user is requested
			
			result = readGroup(loginManager.getMission() + "-" + groupName);
			if (null == result)	{
				// Error handled by called method
				return;
			}
		}
		
		/* Display the user group(s) found */
		if (isVerbose) {
			try {
				CLIUtil.printObject(System.out, result, userAccountOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		} else {
			// Must be a list of users
			for (Object resultObject: (new ObjectMapper()).convertValue(result, List.class)) {
				if (resultObject instanceof Map) {
					System.out.println(((Map<String, String>) resultObject).get("groupname"));
				}
			}
		}
	}

	private void updateGroup(ParsedCommand command) {
		// TODO Auto-generated method stub
		
	}

	private void deleteGroup(ParsedCommand command) {
		// TODO Auto-generated method stub
		
	}

	private void addUser(ParsedCommand command) {
		// TODO Auto-generated method stub
		
	}

	private void removeUser(ParsedCommand command) {
		// TODO Auto-generated method stub
		
	}

	private void grantGroupAuthority(ParsedCommand command) {
		// TODO Auto-generated method stub
		
	}

	private void revokeGroupAuthority(ParsedCommand command) {
		// TODO Auto-generated method stub
		
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
		if (!CMD_USER.equals(command.getName()) && !CMD_GROUP.equals(command.getName())) {
			System.err.println(uiMsg(MSG_ID_INVALID_COMMAND_NAME, command.getName()));
			return;
		}
		
		/* Make sure a subcommand is given */
		if (null == command.getSubcommand() || null == command.getSubcommand().getName()) {
			System.err.println(uiMsg(MSG_ID_SUBCOMMAND_MISSING, command.getName()));
			return;
		}
		
		/* Check for subcommand help request */
		ParsedCommand subcommand = command.getSubcommand();
		if (subcommand.isHelpRequested()) {
			subcommand.getSyntaxCommand().printHelp(System.out);
			return;
		}
				
		/* Execute the subcommand */
		COMMAND:
		switch (command.getName()) {
		case CMD_USER:
			switch (subcommand.getName()) {
			case CMD_CREATE:	createUser(subcommand); break COMMAND;
			case CMD_SHOW:		showUser(subcommand); break COMMAND;
			case CMD_UPDATE:	updateUser(subcommand); break COMMAND;
			case CMD_DELETE:	deleteUser(subcommand); break COMMAND;
			case CMD_ENABLE:	enableUser(subcommand); break COMMAND;
			case CMD_DISABLE:	disableUser(subcommand); break COMMAND;
			case CMD_GRANT:		grantAuthority(subcommand); break COMMAND;
			case CMD_REVOKE:	revokeAuthority(subcommand); break COMMAND;
			default:
				System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		case CMD_GROUP:
			switch (subcommand.getName()) {
			case CMD_CREATE:	createGroup(subcommand); break COMMAND;
			case CMD_SHOW:		showGroup(subcommand); break COMMAND;
//			case CMD_UPDATE:	updateGroup(subcommand); break COMMAND;
//			case CMD_DELETE:	deleteGroup(subcommand); break COMMAND;
//			case CMD_ADD:		addUser(subcommand); break COMMAND;
//			case CMD_REMOVE:	removeUser(subcommand); break COMMAND;
//			case CMD_GRANT:		grantGroupAuthority(subcommand); break COMMAND;
//			case CMD_REVOKE:	revokeGroupAuthority(subcommand); break COMMAND;
			default:
				System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		}
	}
}
