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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.yaml.snakeyaml.error.YAMLException;

import de.dlr.proseo.ui.backend.BackendUserManager;
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
@EnableJpaRepositories(basePackages = { "de.dlr.proseo.model.dao" })
public class CommandLineInterface implements CommandLineRunner {

	private static final String CMD_EXIT = "exit";
	/* Message ID constants */
	private static final int MSG_ID_SYNTAX_FILE_NOT_FOUND = 2920;
	private static final int MSG_ID_SYNTAX_FILE_ERROR = 2921;
	private static final int MSG_ID_COMMAND_LINE_PROMPT_SUPPRESSED = 2922;
	private static final int MSG_ID_COMMAND_NAME_NULL = 2923;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_SYNTAX_FILE_NOT_FOUND = "(E%d) Syntax file %s not found";
	private static final String MSG_SYNTAX_FILE_ERROR = "(E%d) Parsing error in syntax file %s (cause: %s)";
	private static final String MSG_COMMAND_LINE_PROMPT_SUPPRESSED = "(I%d) Command line prompt suppressed by proseo.cli.start parameter";
	private static final String MSG_COMMAND_NAME_NULL = "(E%d) Command name must not be null";
	private static final String MSG_NOT_IMPLEMENTED = "(E%d) Command %s not implemented";
	
	/* Other string constants */
	private static final String PROSEO_COMMAND_PROMPT = "prosEO> ";
	private static final String CMD_INGEST = "ingest";
	private static final String CMD_PRODUCT = "product";
	private static final String CMD_PRODUCT_CLASS = "productClass";
	private static final String CMD_CONFIGURATION = "configuration";
	private static final String CMD_PROCESSOR = "processor";
	private static final String CMD_ORBIT = "orbit";
	private static final String CMD_MISSION = "mission";
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
	private BackendUserManager backendUserMgr;
	
	/* Classes for the various top-level commands */
	@Autowired
	private OrderCommandRunner orderCommandRunner;
	@Autowired
	private IngestorCommandRunner ingestorCommandRunner;
	@Autowired
	private ProcessorCommandRunner processorCommandRunner;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(CLIParser.class);
	
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
			throw new NullPointerException(String.format(MSG_COMMAND_NAME_NULL, MSG_ID_COMMAND_NAME_NULL));
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
			case CMD_LOGIN:
				String username = null, password = null, mission = null;
				for (ParsedOption option: command.getOptions()) {
					if ("user".equals(option.getName())) username = option.getValue();
					if ("password".equals(option.getName())) password = option.getValue();
				}
				if (0 < command.getParameters().size()) {
					mission = command.getParameters().get(0).getValue();
				}
				backendUserMgr.doLogin(username, password, mission);
				break;
			case CMD_LOGOUT:
				backendUserMgr.doLogout();
				break;
			case CMD_HELP:
				parser.getSyntax().printHelp(System.out);
				break;
			case OrderCommandRunner.CMD_ORDER:
				orderCommandRunner.executeCommand(command);
				break;
			case CMD_PRODUCT:
			case CMD_INGEST:
				ingestorCommandRunner.executeCommand(command);
				break;
			case CMD_PROCESSOR:
			case CMD_CONFIGURATION:
				processorCommandRunner.executeCommand(command);
				break;
			case CMD_MISSION:
			case CMD_ORBIT:
			case CMD_PRODUCT_CLASS:
			default:
				String message = String.format(MSG_NOT_IMPLEMENTED, MSG_ID_NOT_IMPLEMENTED, command.getName());
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
			logger.error(String.format(MSG_SYNTAX_FILE_NOT_FOUND, MSG_ID_SYNTAX_FILE_NOT_FOUND, config.getCliSyntaxFile()));
			throw e;
		} catch (YAMLException e) {
			logger.error(String.format(MSG_SYNTAX_FILE_ERROR, MSG_ID_SYNTAX_FILE_ERROR, config.getCliSyntaxFile(), e.getMessage()));
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
			logger.info(String.format(MSG_COMMAND_LINE_PROMPT_SUPPRESSED, MSG_ID_COMMAND_LINE_PROMPT_SUPPRESSED));
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
			if (CMD_EXIT.equals(command.getName())) {
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
