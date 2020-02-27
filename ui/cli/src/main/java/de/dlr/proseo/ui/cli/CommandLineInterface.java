/**
 * CommandLineInterface.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ui.cli;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.yaml.snakeyaml.error.YAMLException;

import de.dlr.proseo.ui.backend.LoginManager;
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
	private static final String PROSEO_COMMAND_PROMPT = "prosEO> ";
	private static final String CMD_HELP = "help";
	private static final String CMD_LOGOUT = "logout";
	private static final String CMD_LOGIN = "login";
	
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
	private IngestorCommandRunner ingestorCommandRunner;
	@Autowired
	private ProcessorCommandRunner processorCommandRunner;
	@Autowired
	private ProductclassCommandRunner productclassCommandRunner;
	@Autowired
	private UserCommandRunner userCommandRunner;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(CLIParser.class);
	
	/**
	 * Check the program invocation arguments (-u/--user, -p/--password, -m/--mission) and remove them from the command line
	 * 
	 * @param args the program invocation arguments
	 * @return a list of strings containing:
	 *   <ol>
	 *     <li>the prosEO command after removal of the invocation arguments</li>
	 *     <li>the username</li>
	 *     <li>the user's password</li>
	 *     <li>the mission to execute the command for</li>
	 *  </ol>
	 */
	private List<String> checkArguments(String[] args) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkArguments({}, out username, out password, out mission)", Arrays.toString(args));
		
		StringBuilder commandBuilder = new StringBuilder();
		String username = null;
		String password = null;
		String mission = null;
		
		for (String arg: args) {
			if (arg.startsWith("-u")) {
				// Short form user argument
				username = arg.substring(2);
			} else if (arg.startsWith("--user=")) {
				// Long form user argument
				username = arg.substring(7);
			} else if (arg.startsWith("-p")) {
				// Short form password argument
				password = arg.substring(2);
			} else if (arg.startsWith("--password=")) {
				// Long form password argument
				password = arg.substring(11);
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
		
		return Arrays.asList(commandBuilder.toString(), username, password, mission);
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
			throw new NullPointerException(uiMsg(MSG_ID_COMMAND_NAME_NULL));
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
			// Handle --version option
			Package classPackage = getClass().getPackage();
			System.out.println(classPackage.getImplementationTitle() + " (v" + classPackage.getImplementationVersion() + ")");
		} else {
			// Hand command down to appropriate command executor class
			switch (command.getName()) {
			case CMD_LOGIN:
				String username = null, password = null, mission = null;
				for (ParsedOption option: command.getOptions()) {
					if ("user".equals(option.getName())) username = option.getValue();
					if ("password".equals(option.getName())) password = option.getValue();
				}
				if (0 < command.getParameters().size()) {
					mission = command.getParameters().get(0).getValue();
				}
				loginManager.doLogin(username, password, mission);
				break;
			case CMD_LOGOUT:
				loginManager.doLogout();
				break;
			case CMD_HELP:
				parser.getSyntax().printHelp(System.out);
				break;
			case OrderCommandRunner.CMD_ORDER:
				orderCommandRunner.executeCommand(command);
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
			case UserCommandRunner.CMD_GROUP:
				userCommandRunner.executeCommand(command);
				break;
			default:
				String message = uiMsg(MSG_ID_NOT_IMPLEMENTED, command.getName());
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
			logger.error(uiMsg(MSG_ID_SYNTAX_FILE_NOT_FOUND, config.getCliSyntaxFile()));
			throw e;
		} catch (YAMLException e) {
			logger.error(uiMsg(MSG_ID_SYNTAX_FILE_ERROR, config.getCliSyntaxFile(), e.getMessage()));
			throw e;
		}
		
		// If command is given, execute it and terminate
		if (0 < args.length) {
			// Handle command line parameters for login!
			List<String> proseoCommand = checkArguments(args);
			
			// Log in to prosEO, if a username was given
			String username = proseoCommand.get(1);
			String password = proseoCommand.get(2);
			String mission = proseoCommand.get(3);
			if (null != username) {
				if (null == password || password.isBlank()) {
					String message = uiMsg(MSG_ID_PASSWORD_MISSING, username);
					logger.error(message);
					System.err.println(message);
					return;
				}
				if (!loginManager.doLogin(username, password, mission)) {
					// Already logged
					return;
				}
			}
			
			// Run the command (for the logged in user, if any)
			executeCommand(parser.parse(proseoCommand.get(0)));
			if (logger.isTraceEnabled()) logger.trace("<<< run()");
			return;
		};
		
		// Check whether the command line prompt shall be started
		if (!config.getCliStart()) {
			logger.info(uiMsg(MSG_ID_COMMAND_LINE_PROMPT_SUPPRESSED));
			if (logger.isTraceEnabled()) logger.trace("<<< run()");
			return;
		}
		
		// If no command is given, enter command prompt loop
		LineReader userInput = LineReaderBuilder.builder().build();
		while (true) {
			ParsedCommand command;
			try {
				String commandLine = userInput.readLine(PROSEO_COMMAND_PROMPT);
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
