/**
 * CommandLineInterface.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ui.cli;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.error.YAMLException;

import de.dlr.proseo.ui.backend.BackendConfiguration;
import de.dlr.proseo.ui.backend.BackendConnectionService;
import de.dlr.proseo.ui.cli.parser.CLIParser;
import de.dlr.proseo.ui.cli.parser.ParsedCommand;
import de.dlr.proseo.ui.cli.parser.ParsedOption;

/**
 * prosEO Command Line Interface application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
public class CommandLineInterface implements CommandLineRunner {

	/* Message ID constants */
	private static final int MSG_ID_SYNTAX_FILE_NOT_FOUND = 2920;
	private static final int MSG_ID_SYNTAX_FILE_ERROR = 2921;
	private static final int MSG_ID_COMMAND_LINE_PROMPT_SUPPRESSED = 2922;
	private static final int MSG_ID_COMMAND_NAME_NULL = 2923;
	private static final int MSG_ID_LOGGED_IN = 2924;
	private static final int MSG_ID_LOGIN_FAILED = 2925;
	private static final int MSG_ID_LOGGED_OUT = 2926;
	private static final int MSG_ID_LOGIN_CANCELLED = 2927;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_SYNTAX_FILE_NOT_FOUND = "(E%d) Syntax file %s not found";
	private static final String MSG_SYNTAX_FILE_ERROR = "(E%d) Parsing error in syntax file %s (cause: %s)";
	private static final String MSG_COMMAND_LINE_PROMPT_SUPPRESSED = "(I%d) Command line prompt suppressed by proseo.cli.start parameter";
	private static final String MSG_COMMAND_NAME_NULL = "(E%d) Command name must not be null";
	private static final String MSG_LOGGED_IN = "(I%d) User %s logged in";
	private static final String MSG_LOGIN_FAILED = "(E%d) Login for user %s failed";
	private static final String MSG_LOGGED_OUT = "(I%d) User %s logged out";
	private static final String MSG_LOGIN_CANCELLED = "(I%d) No username given, login cancelled";
	private static final String MSG_NOT_IMPLEMENTED = "(E%d) Command %s not implemented";
	private static final String MSG_PREFIX = "199 proseo-ui-cli ";
	
	/* Other string constants */
	private static final String PROSEO_COMMAND_PROMPT = "prosEO> ";
	private static final String PROSEO_USERNAME_PROMPT = "Username: ";
	private static final String PROSEO_PASSWORD_PROMPT = "Password for user %s: ";
	
	/** The configuration object for the prosEO User Interface */
	@Autowired
	private BackendConfiguration backendConfig;
	
	/** The configuration object for the prosEO CLI */
	@Autowired
	private CLIConfiguration config;
	
	/** The connector service to the prosEO backend services prosEO */
	@Autowired
	private BackendConnectionService backendConnector;
	
	/** The command line parser */
	@Autowired
	private CLIParser parser;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(CLIParser.class);
	
	/**
	 * Log in to prosEO
	 * 
	 * @param username the user name for login (optional, will be requested from standard input, if not set)
	 * @param password the password for login (optional, will be requested from standard input, if not set)
	 * @return true, if the login was successful, false otherwise
	 */
	private boolean doLogin(String username, String password) {
		if (logger.isTraceEnabled()) logger.trace(">>> doLogin({}, PWD)", username);
		
		// Ask for username, if not set
		if (null == username || "".equals(username)) {
			System.out.print(PROSEO_USERNAME_PROMPT);
			username = new String(System.console().readPassword());
		}
		if (null == username || "".equals(username)) {
			System.err.println(String.format(MSG_PREFIX + MSG_LOGIN_CANCELLED, MSG_ID_LOGIN_CANCELLED));
		}
		
		// Ask for password, if not set
		if (null == password || "".equals(password)) {
			System.out.print(String.format(PROSEO_PASSWORD_PROMPT, username));
			password = new String(System.console().readPassword());
		}
		
		// Test connection to some backend service
		if (backendConnector.testConnectionProcessorManager(username, password)) {
			backendConfig.setProseoUser(username);
			backendConfig.setProseoPassword(password);
			
			String message = String.format(MSG_PREFIX + MSG_LOGGED_IN, MSG_ID_LOGGED_IN, username);
			logger.info(message);
			System.out.println(message);
			if (logger.isTraceEnabled()) logger.trace("<<< doLogin()");
			return true;
		} else {
			backendConfig.setProseoUser(null);
			backendConfig.setProseoPassword(null);
			
			String message = String.format(MSG_PREFIX + MSG_LOGIN_FAILED, MSG_ID_LOGIN_FAILED, username);
			logger.info(message);
			System.err.println(message);
			if (logger.isTraceEnabled()) logger.trace("<<< doLogin()");
			return false;
		}
		
	}
	
	/**
	 * Log the logged in user out from prosEO
	 */
	private void doLogout() {
		if (logger.isTraceEnabled()) logger.trace(">>> doLogout()");
		String username = backendConfig.getProseoUser();
		backendConfig.setProseoUser(null);
		backendConfig.setProseoPassword(null);
		
		String message = String.format(MSG_PREFIX + MSG_LOGGED_OUT, MSG_ID_LOGGED_OUT, username);
		logger.info(message);
		System.out.println(message);
		if (logger.isTraceEnabled()) logger.trace("<<< doLogout()");
	}
	
	/**
	 * Execute the given command (may result in just evaluating the top-level options; "exit" is handled in main command loop)
	 * 
	 * @param command the command to execute
	 * @throws Exception on any unrecoverable failure
	 */
	private void executeCommand(ParsedCommand command) throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> executeCommand({})", (null == command ? "null" : command.getName()));
		
		// Check argument
		if (null == command) {
			throw new NullPointerException(String.format(MSG_PREFIX + MSG_COMMAND_NAME_NULL, MSG_ID_COMMAND_NAME_NULL));
		}
		
		// If help is requested, show help and skip execution
		if (command.isHelpRequested()) {
			if (null == command.getSyntaxCommand()) {
				parser.getSyntax().printHelp(System.out);
			} else {
				command.getSyntaxCommand().printHelp(System.out);
			}
			if (logger.isTraceEnabled()) logger.trace("<<< executeCommand({})");
			return;
		}
		
		// Evaluate command
		if (CLIParser.TOP_LEVEL_COMMAND_NAME.equals(command.getName())) {
			// Handle top-level "proseo" command
			// TODO Handle --version option
		} else {
			// Hand command down to appropriate command executor class
			switch (command.getName()) {
			case "login":
				String username = null, password = null;
				for (ParsedOption option: command.getOptions()) {
					if ("user".equals(option.getName())) username = option.getValue();
					if ("password".equals(option.getName())) password = option.getValue();
				}
				doLogin(username, password);
				break;
			case "logout":
				doLogout();
				break;
			case "help":
				parser.getSyntax().printHelp(System.out);
				break;
			case "mission":
			case "orbit":
			case "order":
			case "processor":
			case "configuration":
			case "productClass":
			case "product":
			case "ingest":
			default:
				String message = String.format(MSG_PREFIX + MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, command.getName());
				System.err.println(message);
				break;
			}
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< executeCommand({})");
	}
	
	/**
	 * Main command loop after initialization of Spring environment
	 * 
	 * @param args the command line arguments
	 * @throws Exception on any unrecoverable failure
	 */
	@Override
	public void run(String... args) throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> run({})", (Object[]) args);
		
		// Initialize the CLI
		try {
			parser.loadSyntax();
		} catch (FileNotFoundException e) {
			logger.error(String.format(MSG_PREFIX + MSG_SYNTAX_FILE_NOT_FOUND, MSG_ID_SYNTAX_FILE_NOT_FOUND, config.getCliSyntaxFile()));
			throw e;
		} catch (YAMLException e) {
			logger.error(String.format(MSG_PREFIX + MSG_SYNTAX_FILE_ERROR, MSG_ID_SYNTAX_FILE_ERROR, config.getCliSyntaxFile()));
			throw e;
		}
		
		// If command is given, execute it and terminate
		if (0 < args.length) {
			// TODO Handle command line parameters for login!!
			executeCommand(parser.parse(String.join(" ", args)));
			if (logger.isTraceEnabled()) logger.trace("<<< run()");
			return;
		};
		
		// Check whether the command line prompt shall be started
		if (!config.getCliStart()) {
			logger.info(String.format(MSG_PREFIX + MSG_COMMAND_LINE_PROMPT_SUPPRESSED, MSG_ID_COMMAND_LINE_PROMPT_SUPPRESSED));
			if (logger.isTraceEnabled()) logger.trace("<<< run()");
			return;
		}
		
		// If no command is given, enter command prompt loop
		BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			System.out.print(PROSEO_COMMAND_PROMPT);
			ParsedCommand command;
			try {
				command = parser.parse(userInput.readLine());
			} catch (ParseException e) {
				System.err.println(e.getMessage());
				continue;
			}
			if (logger.isTraceEnabled()) logger.trace("... received command '{}'", (null == command ? "null" : command.getName()));
			if ("exit".equals(command.getName())) {
				break;
			}
			executeCommand(command);
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< run()");
	}

	/**
	 * Main function to start the Spring application; exits with return code 0 on normal termination, with return code 1 otherwise
	 * 
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		try {
			SpringApplication.run(CommandLineInterface.class, args);
			System.exit(0);
		} catch (Exception e) {
			System.err.println("prosEO Command Line Interface terminated by exception!");
			System.exit(1);
		}
	}

}
