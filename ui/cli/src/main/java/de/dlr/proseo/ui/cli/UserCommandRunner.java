/**
 * UserCommandRunner.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.model.enums.UserRole;
import de.dlr.proseo.model.rest.model.RestGroup;
import de.dlr.proseo.model.rest.model.RestUser;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.backend.ServiceConfiguration;
import de.dlr.proseo.ui.backend.ServiceConnection;
import de.dlr.proseo.ui.cli.CLIUtil.Credentials;
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
	public static final String CMD_PASSWORD = "password";
	public static final String CMD_GROUP = "group";
	private static final String CMD_ADD = "add";
	private static final String CMD_REMOVE = "remove";
	private static final String CMD_MEMBERS = "members";
	private static final String CMD_GRANT = "grant";
	private static final String CMD_REVOKE = "revoke";
	private static final String CMD_ENABLE = "enable";
	private static final String CMD_DISABLE = "disable";
	private static final String CMD_SHOW = "show";
	private static final String CMD_CREATE = "create";
	private static final String CMD_UPDATE = "update";
	private static final String CMD_DELETE = "delete";

	private static final String OPTION_DELETE_ATTRIBUTES = "delete-attributes";
	private static final String OPTION_VERBOSE = "verbose";
	private static final String OPTION_IDENT_FILE = "identFile";
	private static final String OPTION_MISSION = "mission";
	private static final String OPTION_FORMAT = "format";
	private static final String OPTION_FILE = "file";

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
	private static ProseoLogger logger = new ProseoLogger(UserCommandRunner.class);


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
			System.err.println(ProseoLogger.format(UIMessage.NO_USERNAME_GIVEN));
			return null;
		}
		RestUser restUser = null;
		try {
			restUser = serviceConnection.getFromService(serviceConfig.getUserManagerUrl(),
					URI_PATH_USERS + "/" + UriUtils.encodePathSegment(username, Charset.defaultCharset()),
					RestUser.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.USER_NOT_FOUND_BY_NAME, username, loginManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
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
					URI_PATH_USERS + "/" + UriUtils.encodePathSegment(restUser.getUsername(), Charset.defaultCharset()),
					restUser, RestUser.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(ProseoLogger.format(UIMessage.NOT_MODIFIED));
				return null;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.USER_NOT_FOUND_BY_NAME, restUser.getUsername(), loginManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.USER_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
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
			System.err.println(ProseoLogger.format(UIMessage.NO_GROUPNAME_GIVEN));
			return null;
		}
		List<?> resultList = null;
		try {
			resultList = serviceConnection.getFromService(serviceConfig.getUserManagerUrl(),
					URI_PATH_GROUPS + "?mission=" + loginManager.getMission() + "&groupName=" + groupName,
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.GROUP_NOT_FOUND_BY_NAME, groupName,  loginManager.getMission());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), GROUPS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
			return null;
		}
		if (resultList.isEmpty()) {
			String message = logger.log(UIMessage.GROUP_NOT_FOUND_BY_NAME, groupName,  loginManager.getMission());
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
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				System.out.println(ProseoLogger.format(UIMessage.NOT_MODIFIED));
				return null;
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.GROUP_NOT_FOUND_BY_ID, restGroup.getId());
				break;
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.GROUP_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), GROUPS, loginManager.getMission()) :
						e.getStatusText());
				break;
			default:
				message = ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return null;
		} catch (RuntimeException e) {
			System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
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
		String missionPrefix = loginManager.getMissionPrefix();
		String identFile = null;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				userAccountFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				userAccountFileFormat = option.getValue().toUpperCase();
				break;
			case OPTION_MISSION:
				if (missionPrefix.isBlank()) {
					missionPrefix = option.getValue().toUpperCase() + LoginManager.MISSION_PREFIX_CHAR;
				} else {
					System.err.println(ProseoLogger.format(UIMessage.MISSION_ALREADY_SET, loginManager.getMission()));
					return;
				}
				break;
			case OPTION_IDENT_FILE:
				identFile = option.getValue();
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
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from user file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is user account name
				restUser.setUsername(missionPrefix + param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restUser, param.getValue());
				} catch (Exception e) {
					// Already logged and printed
					return;
				}
			}
		}
		
		/* Set missing attributes to default values where possible */
		if (null == restUser.getEnabled()) {
			restUser.setEnabled(true);
		}
		
		/* Prompt user for missing mandatory attributes */
		if (null != System.console()) System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);

		if (null == restUser.getUsername() || restUser.getUsername().isEmpty()) {
			if (null == System.console()) {
				logger.log(UIMessage.MANDATORY_ATTRIBUTE_MISSING, "username");
				return;
			}
			System.out.print(PROMPT_USER_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restUser.setUsername(missionPrefix + response);
		}
		// Handle safe password setting for user creation (--identFile ?)
		restUser.setPassword(null); // Never mind what the account file said!
		if (null != identFile) {
			try {
				Credentials credentials = CLIUtil.readIdentFile(identFile);
				String bareUserName = (restUser.getUsername().startsWith(missionPrefix) ?
						restUser.getUsername().substring(missionPrefix.length()) :
						restUser.getUsername());
				if (!credentials.username.equals(bareUserName)) {
					String message = logger.log(UIMessage.USERNAME_MISMATCH, bareUserName, credentials.username, identFile);
					if (null != System.console()) System.err.println(message);
					return;
				}
				restUser.setPassword(passwordEncoder.encode(credentials.password));
			} catch (Exception e) {
				// Error already handled
				return;
			}
		}
		while (null == restUser.getPassword() || restUser.getPassword().isEmpty()) {
			if (null == System.console()) {
				logger.log(UIMessage.MANDATORY_ATTRIBUTE_MISSING, "password");
				return;
			}
			System.out.print(PROMPT_PASSWORD);
			String response = new String(System.console().readPassword());
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			if (!loginManager.isPasswordStrengthOk(response)) {
				// Error handled in called method
				continue;
			}
			System.out.print(PROMPT_PASSWORD_REPEAT);
			String response2 = new String(System.console().readPassword());
			if (!response.equals(response2)) {
				System.out.println(ProseoLogger.format(UIMessage.PASSWORD_MISMATCH));
				continue;
			}
			restUser.setPassword(passwordEncoder.encode(response));
		}
		
		/* Create user account */
		try {
			restUser = serviceConnection.postToService(serviceConfig.getUserManagerUrl(), URI_PATH_USERS, 
					restUser, RestUser.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.USER_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission()) :
						e.getStatusText());
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

		/* Report success */
		String message = logger.log(UIMessage.USER_CREATED, restUser.getUsername());
		System.out.println(message);
	}

	/**
	 * Show the user account specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "user show" command
	 */
	@SuppressWarnings("rawtypes")
	private void showUser(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showUser({})", (null == showCommand ? "null" : showCommand));
		
		/* Check command options */
		String userAccountOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORMAT:
				userAccountOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
				break;
			}
		}
		
		/* Prepare request URI */
		String requestURI = URI_PATH_USERS;
		Object result = null;
		
		if (showCommand.getParameters().isEmpty()) {
			if (null != loginManager.getMission()) {
				requestURI += "?mission=" + loginManager.getMission();
			}
			/* Get the user account information from the User Manager service */
			try {
				result = serviceConnection.getFromService(serviceConfig.getUserManagerUrl(),
						requestURI, List.class, loginManager.getUser(), loginManager.getPassword());
			} catch (RestClientResponseException e) {
				String message = null;
				switch (e.getStatusCode().value()) {
				case org.apache.http.HttpStatus.SC_NOT_FOUND:
					message = ProseoLogger.format(UIMessage.NO_USERS_FOUND, loginManager.getMission());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				case org.apache.http.HttpStatus.SC_FORBIDDEN:
					message = (null == e.getStatusText() ?
							ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission()) :
							e.getStatusText());
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
		} else {
			// Only user name allowed as parameter
			String username = showCommand.getParameters().get(0).getValue();
			isVerbose = true; // implied, if only a single user is requested
			result = readUser(loginManager.getMissionPrefix() + username);
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
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		} else {
			// Must be a list of users
			for (Object resultObject: (new ObjectMapper()).convertValue(result, List.class)) {
				if (resultObject instanceof Map) {
					System.out.println(((Map) resultObject).get("username"));
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
			case OPTION_FILE:
				userFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				userFileFormat = option.getValue().toUpperCase();
				break;
			case OPTION_DELETE_ATTRIBUTES:
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
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from user file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is username
				updatedUser.setUsername(loginManager.getMissionPrefix() + param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedUser, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
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
		// No modification of username or password allowed
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
		if (isDeleteAttributes || null != updatedUser.getQuota()) {
			restUser.setQuota(updatedUser.getQuota());
		}
		
		/* Update user using User Manager service */
		restUser = modifyUser(restUser);
		if (null == restUser) {
			// Error handled by called method;
			return;
		}
		
		/* Report success */
		String message = logger.log(UIMessage.USER_UPDATED, restUser.getUsername());
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
		String missionPrefix = loginManager.getMissionPrefix();
		for (ParsedOption option: deleteCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_MISSION:
				if (missionPrefix.isBlank()) {
					missionPrefix = option.getValue().toUpperCase() + LoginManager.MISSION_PREFIX_CHAR;
				} else {
					System.err.println(ProseoLogger.format(UIMessage.MISSION_ALREADY_SET, missionPrefix));
					return;
				}
				break;
			}
		}
		
		/* Get username from command parameters */
		if (1 > deleteCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_USERNAME_GIVEN));
			return;
		}
		String username = missionPrefix + deleteCommand.getParameters().get(0).getValue();
		
		/* Retrieve the user using User Manager service */
		RestUser restUser = readUser(username);
		
		/* Delete user using User Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getUserManagerUrl(),
					URI_PATH_USERS + "/" + UriUtils.encodePathSegment(restUser.getUsername(), Charset.defaultCharset()), 
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.USER_NOT_FOUND_BY_NAME, restUser.getUsername());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission()) :
						e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = ProseoLogger.format(UIMessage.USER_DELETE_FAILED, username, e.getMessage());
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
		String message = logger.log(UIMessage.USER_DELETED, restUser.getUsername());
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
			System.err.println(ProseoLogger.format(UIMessage.NO_USERNAME_GIVEN));
			return;
		}
		String username = loginManager.getMissionPrefix() + command.getParameters().get(0).getValue();
		
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
		String message = logger.log(UIMessage.USER_ENABLED, restUser.getUsername());
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
			System.err.println(ProseoLogger.format(UIMessage.NO_USERNAME_GIVEN));
			return;
		}
		String username = loginManager.getMissionPrefix() + command.getParameters().get(0).getValue();
		
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
		String message = logger.log(UIMessage.USER_DISABLED, restUser.getUsername());
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
			System.err.println(ProseoLogger.format(UIMessage.NO_USERNAME_GIVEN));
			return;
		}
		String username = loginManager.getMissionPrefix() + command.getParameters().get(0).getValue();
		
		/* Get granted authorities from command parameters */
		List<String> authorities = new ArrayList<>();
		for (int i = 1; i < command.getParameters().size(); ++i) {
			String authority = command.getParameters().get(i).getValue();
			try {
				UserRole.asRole(authority);
			} catch (IllegalArgumentException e) {
				System.err.println(ProseoLogger.format(UIMessage.SKIPPING_INVALID_AUTHORITY, authority));
				continue;
			}
			authorities.add(authority);
		}
		if (authorities.isEmpty()) {
			// No authorities to grant given
			System.err.println(ProseoLogger.format(UIMessage.NO_AUTHORITIES_GIVEN));
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
		String message = logger.log(UIMessage.AUTHORITIES_GRANTED, Arrays.toString(authorities.toArray()), restUser.getUsername());
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
			System.err.println(ProseoLogger.format(UIMessage.NO_USERNAME_GIVEN));
			return;
		}
		String username = loginManager.getMissionPrefix() + command.getParameters().get(0).getValue();
		
		/* Get granted authorities from command parameters */
		List<String> authorities = new ArrayList<>();
		for (int i = 1; i < command.getParameters().size(); ++i) {
			String authority = command.getParameters().get(i).getValue();
			try {
				UserRole.asRole(authority);
			} catch (IllegalArgumentException e) {
				System.err.println(ProseoLogger.format(UIMessage.SKIPPING_INVALID_AUTHORITY, authority));
				continue;
			}
			authorities.add(authority);
		}
		if (authorities.isEmpty()) {
			// No authorities to grant given
			System.err.println(ProseoLogger.format(UIMessage.NO_AUTHORITIES_GIVEN));
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
		String message = logger.log(UIMessage.AUTHORITIES_REVOKED, Arrays.toString(authorities.toArray()), restUser.getUsername());
		System.out.println(message);
	}

	/**
	 * Interactively change the password of the logged in user,
	 * or of the named user, if executed by a user with user manager permissions
	 * 
	 * @param passwordCommand the parsed "password" command
	 */
	private void changePassword(ParsedCommand passwordCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> changePassword({})", (null == passwordCommand ? "null" : passwordCommand));
		
		if (null == System.console()) {
			logger.log(UIMessage.PASSWORD_CHANGE_NOT_ALLOWED);
			return;
		}

		/* Check command parameters */
		String userName = loginManager.getUser();
		if (0 < passwordCommand.getParameters().size()) {
			userName = loginManager.getMissionPrefix() + passwordCommand.getParameters().get(0).getValue();
		}
		
		/* Find the given user */
		RestUser restUser = readUser(userName);
		if (null == restUser) {
			// Error handled by called method
			return;
		}
		
		/* Prompt user for new password */
		restUser.setPassword(null);
		String newPassword = null;
		while (null == restUser.getPassword() || restUser.getPassword().isEmpty()) {
			System.out.print(PROMPT_PASSWORD);
			newPassword = new String(System.console().readPassword());
			if (newPassword.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			// Password must be different from previous password (as far as we can tell, i.e. only for the logged-in user)
			if (userName.equals(loginManager.getUser()) && newPassword.equals(loginManager.getPassword())) {
				System.out.println(ProseoLogger.format(UIMessage.PASSWORDS_MUST_DIFFER));
				continue;
			}
			// Ensure password strength
			if (!loginManager.isPasswordStrengthOk(newPassword)) {
				// Error handled in called method
				continue;
			}
			// Repeat password
			System.out.print(PROMPT_PASSWORD_REPEAT);
			String response2 = new String(System.console().readPassword());
			if (!newPassword.equals(response2)) {
				System.out.println(ProseoLogger.format(UIMessage.PASSWORD_MISMATCH));
				continue;
			}
			restUser.setPassword(passwordEncoder.encode(newPassword));
		}
		
		/* Update user using User Manager service */
		restUser = modifyUser(restUser);
		if (null == restUser) {
			// Error handled by called method;
			return;
		}
		
		/* If the password of the logged-in user was changed, notify the Login Manager */
		if (userName.equals(loginManager.getUser())) {
			String bareUserName = (userName.startsWith(loginManager.getMissionPrefix()) ? 
					userName.substring(loginManager.getMissionPrefix().length()) : 
					userName);
			loginManager.doLogin(bareUserName, newPassword, loginManager.getMission(), false);
		}
		
		/* Report success */
		String message = logger.log(UIMessage.PASSWORD_CHANGED, restUser.getUsername());
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
			case OPTION_FILE:
				groupAccountFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
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
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from group file) */
		for (int i = 0; i < createCommand.getParameters().size(); ++i) {
			ParsedParameter param = createCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is user group name
				restGroup.setGroupname(loginManager.getMissionPrefix() + param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(restGroup, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Prompt user for missing mandatory attributes */
		if (null != System.console()) System.out.println(MSG_CHECKING_FOR_MISSING_MANDATORY_ATTRIBUTES);

		if (null == restGroup.getGroupname() || restGroup.getGroupname().isEmpty()) {
			if (null == System.console()) {
				logger.log(UIMessage.MANDATORY_ATTRIBUTE_MISSING, "groupname");
				return;
			}
			System.out.print(PROMPT_GROUP_NAME);
			String response = System.console().readLine();
			if (response.isBlank()) {
				System.out.println(ProseoLogger.format(UIMessage.OPERATION_CANCELLED));
				return;
			}
			restGroup.setGroupname(loginManager.getMissionPrefix() + response);
		}
		
		/* Create user group */
		try {
			restGroup = serviceConnection.postToService(serviceConfig.getUserManagerUrl(), URI_PATH_GROUPS, 
					restGroup, RestGroup.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_BAD_REQUEST:
				message = ProseoLogger.format(UIMessage.GROUP_DATA_INVALID, e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission()) :
						e.getStatusText());
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

		/* Report success */
		String message = logger.log(UIMessage.GROUP_CREATED, restGroup.getGroupname());
		System.out.println(message);
	}

	/**
	 * Show the user group(s) specified in the command parameters or options
	 * 
	 * @param showCommand the parsed "group show" command
	 */
	@SuppressWarnings("rawtypes")
	private void showGroup(ParsedCommand showCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> showUser({})", (null == showCommand ? "null" : showCommand));
		
		/* Check command options */
		String userAccountOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORMAT:
				userAccountOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
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
				switch (e.getStatusCode().value()) {
				case org.apache.http.HttpStatus.SC_NOT_FOUND:
					message = ProseoLogger.format(UIMessage.NO_GROUPS_FOUND, loginManager.getMission());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				case org.apache.http.HttpStatus.SC_FORBIDDEN:
					message = (null == e.getStatusText() ?
							ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), USERS, loginManager.getMission()) :
							e.getStatusText());
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
		} else {
			// Only user name allowed as parameter
			String groupName = showCommand.getParameters().get(0).getValue();
			isVerbose = true; // implied, if only a single user is requested
			
			result = readGroup(loginManager.getMissionPrefix() + groupName);
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
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		} else {
			// Must be a list of groups as map
			for (Object resultObject: (new ObjectMapper()).convertValue(result, List.class)) {
				if (resultObject instanceof Map) {
					System.out.println(((Map) resultObject).get("groupname"));
				}
			}
		}
	}

	/**
	 * Update a group from a group file or from "attribute=value" pairs (overriding any group file entries)
	 * 
	 * @param updateCommand the parsed "group update" command
	 */
	private void updateGroup(ParsedCommand updateCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateGroup({})", (null == updateCommand ? "null" : updateCommand.getName()));
		
		/* Check command options */
		File groupFile = null;
		String groupFileFormat = CLIUtil.FILE_FORMAT_JSON;
		boolean isDeleteAttributes = false;
		for (ParsedOption option: updateCommand.getOptions()) {
			switch(option.getName()) {
			case OPTION_FILE:
				groupFile = new File(option.getValue());
				break;
			case OPTION_FORMAT:
				groupFileFormat = option.getValue().toUpperCase();
				break;
			case OPTION_DELETE_ATTRIBUTES:
				isDeleteAttributes = true;
				break;
			}
		}
		
		/* Read group file, if any */
		RestGroup updatedGroup = null;
		if (null == groupFile) {
			updatedGroup = new RestGroup();
		} else {
			try {
				updatedGroup = CLIUtil.parseObjectFile(groupFile, groupFileFormat, RestGroup.class);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		/* Check command parameters (overriding values from group file) */
		for (int i = 0; i < updateCommand.getParameters().size(); ++i) {
			ParsedParameter param = updateCommand.getParameters().get(i);
			if (0 == i) {
				// First parameter is group name
				updatedGroup.setGroupname(loginManager.getMissionPrefix() + param.getValue());
			} else {
				// Remaining parameters are "attribute=value" parameters
				try {
					CLIUtil.setAttribute(updatedGroup, param.getValue());
				} catch (Exception e) {
					System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
					return;
				}
			}
		}
		
		/* Read original group from User service */
		RestGroup restGroup = readGroup(updatedGroup.getGroupname());
		if (null == restGroup) {
			// Error handled by called method
			return;
		}

		/* Compare attributes of database user with updated user */
		// No modification of username allowed
		if (null != updatedGroup.getAuthorities() && (isDeleteAttributes || !updatedGroup.getAuthorities().isEmpty())) {
			restGroup.getAuthorities().clear();
			restGroup.getAuthorities().addAll(updatedGroup.getAuthorities());
		}
		
		/* Update user using User Manager service */
		restGroup = modifyGroup(restGroup);
		if (null == restGroup) {
			// Error handled by called method;
			return;
		}
		
		/* Report success */
		String message = logger.log(UIMessage.GROUP_UPDATED, restGroup.getGroupname());
		System.out.println(message);
	}

	/**
	 * Delete the given group from the current mission
	 * 
	 * @param deleteCommand the parsed "group delete" command
	 */
	private void deleteGroup(ParsedCommand deleteCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteGroup({})", (null == deleteCommand ? "null" : deleteCommand.getName()));

		/* Get group name from command parameters */
		if (1 > deleteCommand.getParameters().size()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_GROUPNAME_GIVEN));
			return;
		}
		String groupname = loginManager.getMissionPrefix() + deleteCommand.getParameters().get(0).getValue();
		
		/* Retrieve the group using User Manager service */
		RestGroup restGroup = readGroup(groupname);
		
		/* Delete user using User Manager service */
		try {
			serviceConnection.deleteFromService(serviceConfig.getUserManagerUrl(),
					URI_PATH_GROUPS + "/" + restGroup.getId(), 
					loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.GROUP_NOT_FOUND_BY_ID, restGroup.getId());
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), GROUPS, loginManager.getMission()) :
						e.getStatusText());
				break;
			case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
				message = ProseoLogger.format(UIMessage.GROUP_DELETE_FAILED, groupname, e.getMessage());
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
		String message = logger.log(UIMessage.GROUP_DELETED, restGroup.getGroupname());
		System.out.println(message);
	}

	/**
	 * Add the named user(s) to the user group
	 * 
	 * @param command the "group add" command
	 */
	private void addUser(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> addUser({})", (null == command ? "null" : command.getName()));

		/* Get group name from command parameters */
		if (1 > command.getParameters().size()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_GROUPNAME_GIVEN));
			return;
		}
		String groupname = loginManager.getMissionPrefix() + command.getParameters().get(0).getValue();
		
		/* Get user name(s) from command parameters */
		List<String> usernames = new ArrayList<>();
		for (int i = 1; i < command.getParameters().size(); ++i) {
			usernames.add(loginManager.getMissionPrefix() + command.getParameters().get(i).getValue());
		}
		if (usernames.isEmpty()) {
			// No users to add given
			System.err.println(ProseoLogger.format(UIMessage.NO_USERS_GIVEN));
			return;
		}
		
		/* Read original group from User service */
		RestGroup restGroup = readGroup(groupname);
		if (null == restGroup) {
			// Error handled by called method
			return;
		}
		
		/* Check that the users exist and add each one to the group's list of members */
		List<String> addedUsers = new ArrayList<>();
		for (String username: usernames) {
			RestUser restUser = readUser(username);
			if (null == restUser) {
				// Invalid user name (at least for the selected mission) - already logged
				continue;
			}
			
			/* Add user to group using User Manager service */
			try {
				serviceConnection.postToService(serviceConfig.getUserManagerUrl(),
						URI_PATH_GROUPS + "/" + restGroup.getId() + "/members?username=" + username,
						restGroup, List.class, loginManager.getUser(), loginManager.getPassword());
				addedUsers.add(username);
			} catch (RestClientResponseException e) {
				String message = null;
				switch (e.getStatusCode().value()) {
				case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
					System.out.println(ProseoLogger.format(UIMessage.ALREADY_MEMBER, username, restGroup.getGroupname()));
					continue;
				case org.apache.http.HttpStatus.SC_NOT_FOUND:
					message = ProseoLogger.format(UIMessage.GROUP_NOT_FOUND_BY_ID, restGroup.getId());
					break;
				case org.apache.http.HttpStatus.SC_BAD_REQUEST:
					message = ProseoLogger.format(UIMessage.GROUP_DATA_INVALID, e.getStatusText());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				case org.apache.http.HttpStatus.SC_FORBIDDEN:
					message = (null == e.getStatusText() ?
							ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), GROUPS, loginManager.getMission()) :
							e.getStatusText());
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
		}
		
		/* Report success, if valid users were added */
		if (0 < addedUsers.size()) {
			String message = logger.log(UIMessage.USERS_ADDED, Arrays.toString(addedUsers.toArray()), restGroup.getGroupname());
			System.out.println(message);
		}
	}

	/**
	 * Remove the named user(s) to the user group
	 * 
	 * @param command the "group remove" command
	 */
	private void removeUser(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> removeUser({})", (null == command ? "null" : command.getName()));

		/* Get group name from command parameters */
		if (1 > command.getParameters().size()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_GROUPNAME_GIVEN));
			return;
		}
		String groupname = loginManager.getMissionPrefix() + command.getParameters().get(0).getValue();
		
		/* Get user name(s) from command parameters */
		List<String> usernames = new ArrayList<>();
		for (int i = 1; i < command.getParameters().size(); ++i) {
			usernames.add(loginManager.getMissionPrefix() + command.getParameters().get(i).getValue());
		}
		if (usernames.isEmpty()) {
			// No users to add given
			System.err.println(ProseoLogger.format(UIMessage.NO_USERS_GIVEN));
			return;
		}
		
		/* Read original group from User service */
		RestGroup restGroup = readGroup(groupname);
		if (null == restGroup) {
			// Error handled by called method
			return;
		}
		
		/* Check that the users exist and remove each one from the group's list of members */
		List<String> removedUsers = new ArrayList<>();
		for (String username: usernames) {
			RestUser restUser = readUser(username);
			if (null == restUser) {
				// Invalid user name (at least for the selected mission) - already logged
				continue;
			}
			
			/* Remove user from group using User Manager service */
			try {
				serviceConnection.deleteFromService(serviceConfig.getUserManagerUrl(),
						URI_PATH_GROUPS + "/" + restGroup.getId() + "/members?username=" + username,
						loginManager.getUser(), loginManager.getPassword());
				removedUsers.add(username);
			} catch (RestClientResponseException e) {
				String message = null;
				switch (e.getStatusCode().value()) {
				case org.apache.http.HttpStatus.SC_NOT_MODIFIED:
					System.out.println(ProseoLogger.format(UIMessage.NOT_MEMBER, username, restGroup.getGroupname()));
					continue;
				case org.apache.http.HttpStatus.SC_NOT_FOUND:
					message = ProseoLogger.format(UIMessage.GROUP_NOT_FOUND_BY_ID, restGroup.getId());
					break;
				case org.apache.http.HttpStatus.SC_BAD_REQUEST:
					message = ProseoLogger.format(UIMessage.GROUP_DATA_INVALID, e.getStatusText());
					break;
				case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				case org.apache.http.HttpStatus.SC_FORBIDDEN:
					message = (null == e.getStatusText() ?
							ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), GROUPS, loginManager.getMission()) :
							e.getStatusText());
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
		}
		
		/* Report success, if valid users were removed */
		if (0 < removedUsers.size()) {
			String message = logger.log(UIMessage.USERS_REMOVED, Arrays.toString(removedUsers.toArray()), restGroup.getGroupname());
			System.out.println(message);
		}
	}

	/**
	 * Show all members of the given group
	 * 
	 * @param command the "group members" command
	 */
	@SuppressWarnings("rawtypes")
	private void showGroupMembers(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> revokeGroupAuthority({})", (null == command ? "null" : command.getName()));
		
		/* Check command options */
		String userAccountOutputFormat = CLIUtil.FILE_FORMAT_YAML;
		boolean isVerbose = false;
		for (ParsedOption option: command.getOptions()) {
			switch(option.getName()) {
			case OPTION_FORMAT:
				userAccountOutputFormat = option.getValue().toUpperCase();
				break;
			case OPTION_VERBOSE:
				isVerbose = true;
				break;
			}
		}
		
		/* Get group name from command parameters */
		if (1 > command.getParameters().size()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_GROUPNAME_GIVEN));
			return;
		}
		String groupname = loginManager.getMissionPrefix() + command.getParameters().get(0).getValue();
		
		/* Read original group from User service */
		RestGroup restGroup = readGroup(groupname);
		if (null == restGroup) {
			// Error handled by called method
			return;
		}
		
		/* Read the users for the given group */
		Object result = null;
		
		/* Get the user account information from the User Manager service */
		try {
			result = serviceConnection.getFromService(serviceConfig.getUserManagerUrl(),
					URI_PATH_GROUPS + "/" + restGroup.getId() + "/members",
					List.class, loginManager.getUser(), loginManager.getPassword());
		} catch (RestClientResponseException e) {
			String message = null;
			switch (e.getStatusCode().value()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = ProseoLogger.format(UIMessage.NO_USERS_FOUND_IN_GROUP, groupname);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
			case org.apache.http.HttpStatus.SC_FORBIDDEN:
				message = (null == e.getStatusText() ?
						ProseoLogger.format(UIMessage.NOT_AUTHORIZED, loginManager.getUser(), GROUPS, loginManager.getMission()) :
						e.getStatusText());
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

		/* Display the user account(s) found */
		if (isVerbose) {
			try {
				CLIUtil.printObject(System.out, result, userAccountOutputFormat);
			} catch (IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			} catch (IOException e) {
				System.err.println(ProseoLogger.format(UIMessage.EXCEPTION, e.getMessage()));
				return;
			}
		} else {
			// Must be a list of users
			for (Object resultObject: (new ObjectMapper()).convertValue(result, List.class)) {
				if (resultObject instanceof Map) {
					System.out.println(((Map) resultObject).get("username"));
				}
			}
		}
	}
	
	/**
	 * Grant the given authorities to the given user group for the current mission
	 *
	 * @param command the "group grant" command
	 */
	private void grantGroupAuthority(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> grantGroupAuthority({})", (null == command ? "null" : command.getName()));

		/* Get group name from command parameters */
		if (1 > command.getParameters().size()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_GROUPNAME_GIVEN));
			return;
		}
		String groupname = loginManager.getMissionPrefix() + command.getParameters().get(0).getValue();
		
		/* Get granted authorities from command parameters */
		List<String> authorities = new ArrayList<>();
		for (int i = 1; i < command.getParameters().size(); ++i) {
			String authority = command.getParameters().get(i).getValue();
			try {
				UserRole.asRole(authority);
			} catch (IllegalArgumentException e) {
				System.err.println(ProseoLogger.format(UIMessage.SKIPPING_INVALID_AUTHORITY, authority));
				continue;
			}
			authorities.add(authority);
		}
		if (authorities.isEmpty()) {
			// No authorities to grant given
			System.err.println(ProseoLogger.format(UIMessage.NO_AUTHORITIES_GIVEN));
			return;
		}
		
		/* Read original group from User service */
		RestGroup restGroup = readGroup(groupname);
		if (null == restGroup) {
			// Error handled by called method
			return;
		}
		
		/* Add the given authorities */
		for (String authority: authorities) {
			if (!restGroup.getAuthorities().contains(authority)) {
				restGroup.getAuthorities().add(authority);
			}
		}

		/* Update group using User Manager service */
		restGroup = modifyGroup(restGroup);
		if (null == restGroup) {
			// Error handled by called method;
			return;
		}
		
		/* Report success */
		String message = logger.log(UIMessage.GROUP_AUTHORITIES_GRANTED, Arrays.toString(authorities.toArray()), restGroup.getGroupname());
		System.out.println(message);
	}

	/**
	 * Revoke the given authorities from the given group for the current mission
	 *
	 * @param command the "group revoke" command
	 */
	private void revokeGroupAuthority(ParsedCommand command) {
		if (logger.isTraceEnabled()) logger.trace(">>> revokeGroupAuthority({})", (null == command ? "null" : command.getName()));

		/* Get group name from command parameters */
		if (1 > command.getParameters().size()) {
			// No identifying value given
			System.err.println(ProseoLogger.format(UIMessage.NO_GROUPNAME_GIVEN));
			return;
		}
		String groupname = loginManager.getMissionPrefix() + command.getParameters().get(0).getValue();
		
		/* Get granted authorities from command parameters */
		List<String> authorities = new ArrayList<>();
		for (int i = 1; i < command.getParameters().size(); ++i) {
			String authority = command.getParameters().get(i).getValue();
			try {
				UserRole.asRole(authority);
			} catch (IllegalArgumentException e) {
				System.err.println(ProseoLogger.format(UIMessage.SKIPPING_INVALID_AUTHORITY, authority));
				continue;
			}
			authorities.add(authority);
		}
		if (authorities.isEmpty()) {
			// No authorities to grant given
			System.err.println(ProseoLogger.format(UIMessage.NO_AUTHORITIES_GIVEN));
			return;
		}
		
		/* Read original group from User service */
		RestGroup restGroup = readGroup(groupname);
		if (null == restGroup) {
			// Error handled by called method
			return;
		}
		
		/* Remove the given authorities */
		restGroup.getAuthorities().removeAll(authorities);

		/* Update group using User Manager service */
		restGroup = modifyGroup(restGroup);
		if (null == restGroup) {
			// Error handled by called method;
			return;
		}
		
		/* Report success */
		String message = logger.log(UIMessage.GROUP_AUTHORITIES_REVOKED, Arrays.toString(authorities.toArray()), restGroup.getGroupname());
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
			System.err.println(ProseoLogger.format(UIMessage.USER_NOT_LOGGED_IN, command.getName()));
			return;
		}
		if (null == loginManager.getMission()) {
			if (CMD_USER.equals(command.getName()) && null != command.getSubcommand() && CMD_CREATE.equals(command.getSubcommand().getName()) ) {
				// OK, "user create" allowed without login to a specific mission (for administrator only)
			} else if (CMD_PASSWORD.equals(command.getName())) {
				// OK, "password" allowed without login to a specific mission (for administrator only)
			} else {
				System.err.println(ProseoLogger.format(UIMessage.USER_NOT_LOGGED_IN_TO_MISSION, command.getName()));
				return;
			}
		}
		
		/* Check argument */
		if (!CMD_USER.equals(command.getName()) && !CMD_PASSWORD.equals(command.getName()) && !CMD_GROUP.equals(command.getName())) {
			System.err.println(ProseoLogger.format(UIMessage.INVALID_COMMAND_NAME, command.getName()));
			return;
		}
		
		/* Make sure a subcommand is given for "user" and "group" */
		if (!CMD_PASSWORD.equals(command.getName()) &&
				(null == command.getSubcommand() || null == command.getSubcommand().getName())) {
			System.err.println(ProseoLogger.format(UIMessage.SUBCOMMAND_MISSING, command.getName()));
			return;
		}
		
		/* Check for subcommand help request */
		ParsedCommand subcommand = command.getSubcommand();
		if (!CMD_PASSWORD.equals(command.getName()) && subcommand.isHelpRequested()) {
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
				System.err.println(ProseoLogger.format(UIMessage.COMMAND_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		case CMD_PASSWORD:
			changePassword(command); break COMMAND;
		case CMD_GROUP:
			switch (subcommand.getName()) {
			case CMD_CREATE:	createGroup(subcommand); break COMMAND;
			case CMD_SHOW:		showGroup(subcommand); break COMMAND;
			case CMD_UPDATE:	updateGroup(subcommand); break COMMAND;
			case CMD_DELETE:	deleteGroup(subcommand); break COMMAND;
			case CMD_ADD:		addUser(subcommand); break COMMAND;
			case CMD_REMOVE:	removeUser(subcommand); break COMMAND;
			case CMD_MEMBERS:	showGroupMembers(subcommand); break COMMAND;
			case CMD_GRANT:		grantGroupAuthority(subcommand); break COMMAND;
			case CMD_REVOKE:	revokeGroupAuthority(subcommand); break COMMAND;
			default:
				System.err.println(ProseoLogger.format(UIMessage.COMMAND_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		}
	}

}
