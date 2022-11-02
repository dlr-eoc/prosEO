/**
 * CommandLineInterface.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ui.cli;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.error.YAMLException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.model.enums.UserRole;
import de.dlr.proseo.ui.backend.LoginManager;
import de.dlr.proseo.ui.cli.CLIUtil.Credentials;
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

	private static final String CMD_EXIT = "exit";
	/* Message ID constants */
	
	/* Message string constants */
	
	/* Other string constants */
	private static final String PROSEO_COMMAND_PROMPT = "prosEO (%s)> ";
	private static final String CMD_CLEAR = "clear";
	private static final String CMD_HELP = "help";
	private static final String CMD_LOGOUT = "logout";
	private static final String CMD_LOGIN = "login";

	private static final String CLEAR_SCREEN_SEQUENCE = "\033[H\033[J"; // ANSI command sequence to clear the screen
	
	/** The configuration object for the prosEO CLI */
	@Autowired
	private CLIConfiguration config;
	
	/** The command line parser */
	@Autowired
	private CLIParser parser;
	
	/** The user manager used by all command runners */
	@Autowired
	private LoginManager loginManager;
	
	/* Classes for the various top-level commands */
	@Autowired
	private MissionCommandRunner missionCommandRunner;
	@Autowired
	private OrderCommandRunner orderCommandRunner;
	@Autowired
	private JobCommandRunner jobCommandRunner;
	@Autowired
	private IngestorCommandRunner ingestorCommandRunner;
	@Autowired
	private ProcessorCommandRunner processorCommandRunner;
	@Autowired
	private ProductclassCommandRunner productclassCommandRunner;
	@Autowired
	private UserCommandRunner userCommandRunner;
	@Autowired
	private FacilityCommandRunner facilityCommandRunner;
	
	/** Indicator for interactive mode vs. running from input redirection */
	public static boolean isInteractiveMode = true;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(CommandLineInterface.class);
	
	/**
	 * Check the program invocation arguments (-i/--identFile, -m/--mission) and remove them from the command line
	 * 
	 * @param args the program invocation arguments
	 * @return a list of strings containing:
	 *   <ol>
	 *     <li>the prosEO command after removal of the invocation arguments</li>
	 *     <li>the path to the ident file, if given</li>
	 *     <li>the mission to execute the command for, if given</li>
	 *  </ol>
	 */
	private static List<String> checkArguments(String[] args) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkArguments({})", Arrays.toString(args));
		
		StringBuilder commandBuilder = new StringBuilder();
		String identFile = null;
		String mission = null;
		
		for (String arg: args) {
			if (arg.startsWith("-i")) {
				// Short form ident file argument
				identFile = arg.substring(2);
			} else if (arg.startsWith("--identFile=")) {
				// Long form ident file argument
				identFile = arg.substring(12);
			} else if (arg.startsWith("-m")) {
				// Short form mission argument
				mission = arg.substring(2);
			} else if (arg.startsWith("--mission=")) {
				// Long form mission argument
				mission = arg.substring(10);
			} else {
				// Part of prosEO command line
				commandBuilder.append(' ').append(arg);
			}
		}
		
		return Arrays.asList(commandBuilder.toString(), identFile, mission);
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
			throw new NullPointerException(ProseoLogger.format(UIMessage.COMMAND_NAME_NULL));
		}
		
		// If help is requested, show help and skip execution
		if (command.isHelpRequested()) {
			if (null == command.getSyntaxCommand()) {
				parser.getSyntax().printHelp(System.out);
			} else {
				command.getSyntaxCommand().printHelp(System.out);
			}
			if (logger.isTraceEnabled()) logger.trace("<<< executeCommand()");
			return;
		}
		
		// Evaluate command
		if (CLIParser.TOP_LEVEL_COMMAND_NAME.equals(command.getName())) {
			// Handle top-level "proseo" command
			// Handle --version option
			Package classPackage = getClass().getPackage();
			System.out.println(classPackage.getImplementationTitle() + " (v" + classPackage.getImplementationVersion() + ")");
		} else {
			// Hand command down to appropriate command executor class
			switch (command.getName()) {
			case CMD_LOGIN:
				String username = null, password = null, mission = null;
				for (ParsedOption option: command.getOptions()) {
					if ("identFile".equals(option.getName())) {
						try {
							Credentials credentials = CLIUtil.readIdentFile(option.getValue());
							username = credentials.username;
							password = credentials.password;
						} catch (Exception e) {
							System.err.println(ProseoLogger.format(UIMessage.USER_NOT_LOGGED_IN));
							break;
						}
					}
				}
				if (0 < command.getParameters().size()) {
					mission = command.getParameters().get(0).getValue();
				}
				boolean loggedIn = loginManager.doLogin(username, password, mission, true);
				if (loggedIn && !loginManager.hasRole(UserRole.CLI_USER)) {
					String message = logger.log(UIMessage.CLI_NOT_AUTHORIZED, loginManager.getUser());
					System.err.println(message);
					loginManager.doLogout();
				}
				break;
			case CMD_LOGOUT:
				loginManager.doLogout();
				break;
			case CMD_HELP:
				parser.getSyntax().printHelp(System.out);
				break;
			case CMD_CLEAR:
				if (System.getProperty("os.name").toLowerCase().contains("windows")) {
					new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
				} else {
					System.out.print(CLEAR_SCREEN_SEQUENCE);
				}
				break;
			case OrderCommandRunner.CMD_ORDER:
				orderCommandRunner.executeCommand(command);
				break;
			case JobCommandRunner.CMD_JOB:
				jobCommandRunner.executeCommand(command);
				break;
			case IngestorCommandRunner.CMD_PRODUCT:
			case IngestorCommandRunner.CMD_INGEST:
				ingestorCommandRunner.executeCommand(command);
				break;
			case ProcessorCommandRunner.CMD_PROCESSOR:
			case ProcessorCommandRunner.CMD_CONFIGURATION:
				processorCommandRunner.executeCommand(command);
				break;
			case MissionCommandRunner.CMD_MISSION:
			case MissionCommandRunner.CMD_ORBIT:
				missionCommandRunner.executeCommand(command);
				break;
			case ProductclassCommandRunner.CMD_PRODUCTCLASS:
				productclassCommandRunner.executeCommand(command);
				break;
			case UserCommandRunner.CMD_USER:
			case UserCommandRunner.CMD_PASSWORD:
			case UserCommandRunner.CMD_GROUP:
				userCommandRunner.executeCommand(command);
				break;
			case FacilityCommandRunner.CMD_FACILITY:
				facilityCommandRunner.executeCommand(command);
				break;
			default:
				String message = ProseoLogger.format(UIMessage.COMMAND_NOT_IMPLEMENTED, command.getName());
				System.err.println(message);
				break;
			}
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< executeCommand()");
	}
	
	/**
	 * Main command loop after initialization of Spring environment
	 * 
	 * @param args the command line arguments
	 * @throws Exception on any unrecoverable failure
	 */
	@Override
	public void run(String... args) throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> run({})", args.toString());
		
		// Initialize the CLI
		try {
			parser.loadSyntax();
		} catch (FileNotFoundException e) {
			logger.log(UIMessage.SYNTAX_FILE_NOT_FOUND, config.getCliSyntaxFile());
			throw e;
		} catch (YAMLException e) {
			logger.log(UIMessage.SYNTAX_FILE_ERROR, config.getCliSyntaxFile(), e.getMessage());
			throw e;
		}
		
		// Process command line arguments: Log in and optionally execute command and terminate
		if (0 < args.length) {
			// Handle command line parameters for login!
			List<String> proseoCommand = checkArguments(args);
			
			// Log in to prosEO, if a username was given
			String identFile = proseoCommand.get(1);
			String mission = proseoCommand.get(2);
			String username = null, password = null;
			if (null != identFile) {
				try {
					Credentials credentials = CLIUtil.readIdentFile(identFile);
					username = credentials.username;
					password = credentials.password;
				} catch (SecurityException | IOException e) {
					System.err.println(ProseoLogger.format(UIMessage.USER_NOT_LOGGED_IN));
				} catch (Exception e) {
					String message = logger.log(UIMessage.EXCEPTION, e);
					System.err.println(message);
					System.err.println(ProseoLogger.format(UIMessage.USER_NOT_LOGGED_IN));
				}
			}
			if (null != username) {
				if (null == password || password.isBlank()) {
					String message = logger.log(UIMessage.PASSWORD_MISSING, username);
					System.err.println(message);
					return;
				}
				System.out.println(ProseoLogger.format(UIMessage.LOGGING_IN, username));
				if (!loginManager.doLogin(username, password, mission, isInteractiveMode)) {
					// Already logged
					return;
				}
				if (!loginManager.hasRole(UserRole.CLI_USER)) {
					String message = logger.log(UIMessage.CLI_NOT_AUTHORIZED, username);
					System.err.println(message);
					return;
				}
			}
			
			// If command is given, execute it and terminate
			if (!proseoCommand.get(0).isBlank()) {
				ParsedCommand command = null;
				try {
					command = parser.parse(proseoCommand.get(0));
				} catch (ParseException e) {
					System.err.println(e.getMessage());
					return;
				}
				executeCommand(command);
				if (logger.isTraceEnabled())
					logger.trace("<<< run()");
				return;
			}
		};
		
		// Check whether the command line prompt shall be started (only required for unit tests)
		if (!config.getCliStart()) {
			logger.log(UIMessage.COMMAND_LINE_PROMPT_SUPPRESSED);
			if (logger.isTraceEnabled()) logger.trace("<<< run()");
			return;
		}
		
		// If no command is given, enter command prompt loop
		LineReader userInput = LineReaderBuilder.builder().build();
		while (true) {
			ParsedCommand command;
			try {
				String commandLine;
				try {
					commandLine = userInput.readLine(isInteractiveMode ?
						String.format(PROSEO_COMMAND_PROMPT, null == loginManager.getMission() ? "no mission" : loginManager.getMission())
						: "");
				} catch (UserInterruptException e) {
					String message = logger.log(UIMessage.USER_INTERRUPT);
					System.err.println(message);
					break;
				} catch (EndOfFileException e) {
					// End of file reached for redirected input
					logger.log(UIMessage.END_OF_FILE);
					// No logging to standard output
					break;
				}
				if (commandLine.isBlank()) {
					// Silently ignore empty input lines
					continue;
				}
				command = parser.parse(commandLine);
			} catch (ParseException e) {
				System.err.println(e.getMessage());
				continue;
			}
			if (logger.isTraceEnabled()) logger.trace("... received command '{}'", (null == command ? "null" : command.getName()));
			if (CMD_EXIT.equals(command.getName()) && !command.isHelpRequested()) {
				// Terminate CLI execution
				logger.log(UIMessage.CLI_TERMINATED);
				// No logging to standard output
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
		// Check availability of console or prosEO command to determine interactive mode
		Banner.Mode bannerMode = Banner.Mode.CONSOLE;
		
		if (null == System.console() || !checkArguments(args).get(0).isBlank()) {
			isInteractiveMode = false;
			bannerMode = Banner.Mode.OFF;
		}
		
		try {
			SpringApplication cli = new SpringApplication(CommandLineInterface.class);
			cli.setBannerMode(bannerMode);
			cli.run(args);
			System.exit(0);
		} catch (Exception e) {
			String message = logger.log(UIMessage.UNCAUGHT_EXCEPTION, e);
			System.err.println(message);
			System.exit(1);
		}
	}

}
