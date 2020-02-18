/**
 * UserCommandRunner.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

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
	 * Create a new user account; if the input is not from a file, the user will be prompted for mandatory attributes
	 * not given on the command line
	 * 
	 * @param command the parsed "user create" command
	 */
	private void createUser(ParsedCommand createCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> createUser({})", (null == createCommand ? "null" : createCommand));
		
		/* Check command options */
		File userAccountFile = null;
		String userAccountFileFormat = CLIUtil.FILE_FORMAT_JSON;
		for (ParsedOption option: createCommand.getOptions()) {
			switch(option.getName()) {
			case "file":
				userAccountFile = new File(option.getValue());
				break;
			case "format":
				userAccountFileFormat = option.getValue().toUpperCase();
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
				restUser.setUsername(param.getValue());
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
			restUser.setUsername(loginManager.getMission() + "-" + response);
		}
		if (null == restUser.getPassword() || restUser.getPassword().isEmpty()) {
			System.out.print(PROMPT_PASSWORD);
			String response = new String(System.console().readPassword());
			if ("".equals(response)) {
				System.out.println(uiMsg(MSG_ID_OPERATION_CANCELLED));
				return;
			}
			restUser.setPassword(passwordEncoder.encode(response));
		}
		
		/* Create user account */
		try {
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
		for (ParsedOption option: showCommand.getOptions()) {
			switch(option.getName()) {
			case "format":
				userAccountOutputFormat = option.getValue().toUpperCase();
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
			requestURI += "/" + loginManager.getMission() + "-" + username;

			/* Get the user account information from the User Manager service */
			try {
				result = serviceConnection.getFromService(serviceConfig.getUserManagerUrl(),
						requestURI, RestUser.class, loginManager.getUser(), loginManager.getPassword());
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
				return;
			} catch (RuntimeException e) {
				System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
				return;
			}
		}
		
		
		/* Display the user account(s) found */
		try {
			CLIUtil.printObject(System.out, result, userAccountOutputFormat);
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			return;
		} catch (IOException e) {
			System.err.println(uiMsg(MSG_ID_EXCEPTION, e.getMessage()));
			return;
		}
	}

	private void updateUser(ParsedCommand command) {
		// TODO Auto-generated method stub
		
	}

	private void deleteUser(ParsedCommand command) {
		// TODO Auto-generated method stub
		
	}

	private void enableUser(ParsedCommand command) {
		// TODO Auto-generated method stub
		
	}

	private void disableUser(ParsedCommand command) {
		// TODO Auto-generated method stub
		
	}

	private void grantAuthority(ParsedCommand command) {
		// TODO Auto-generated method stub
		
	}

	private void revokeAuthority(ParsedCommand command) {
		// TODO Auto-generated method stub
		
	}

	private void createGroup(ParsedCommand command) {
		// TODO Auto-generated method stub
		
	}

	private void showGroup(ParsedCommand command) {
		// TODO Auto-generated method stub
		
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
//			case CMD_UPDATE:	updateUser(subcommand); break COMMAND;
//			case CMD_DELETE:	deleteUser(subcommand); break COMMAND;
//			case CMD_ENABLE:	enableUser(subcommand); break COMMAND;
//			case CMD_DISABLE:	disableUser(subcommand); break COMMAND;
//			case CMD_GRANT:		grantAuthority(subcommand); break COMMAND;
//			case CMD_REVOKE:	revokeAuthority(subcommand); break COMMAND;
			default:
				System.err.println(uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName() + " " + subcommand.getName()));
				return;
			}
		case CMD_GROUP:
			switch (subcommand.getName()) {
//			case CMD_CREATE:	createGroup(subcommand); break COMMAND;
//			case CMD_SHOW:		showGroup(subcommand); break COMMAND;
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
