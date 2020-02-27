/**
 * CLIParser.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.error.YAMLException;

import de.dlr.proseo.ui.cli.CLIConfiguration;

/**
 * Parser for the prosEO Command Line Interface
 * 
 * Command line syntax is based on a YAML specification.
 * The general command syntax is:  "proseo" command [subcommand...] [option...] [parameter...]
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class CLIParser {

	/** The name of the top-level command */
	public static final String TOP_LEVEL_COMMAND_NAME = "proseo";
	
	/** The configuration object for the prosEO CLI */
	@Autowired
	private CLIConfiguration config;
	
	/** The command line syntax to use */
	private CLISyntax syntax;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(CLIParser.class);
	
	/**
	 * Load the command line syntax from the configured syntax file
	 * 
	 * @throws FileNotFoundException if the given file could not be read
	 * @throws YAMLException if the given file could not be parsed
	 */
	public void loadSyntax() throws FileNotFoundException, YAMLException {
		syntax = CLISyntax.fromSyntaxFile(config.getCliSyntaxFile());		
	}
	
	/**
	 * Return the currently applicable syntax
	 * 
	 * @return the syntax
	 */
	public CLISyntax getSyntax() {
		return syntax;
	}

	/**
	 * Check whether the given value conforms to the given type
	 * 
	 * @param type the type to test against
	 * @param value the value to test
	 * @return true, if the value is acceptable for the type, false otherwise
	 */
	private boolean isTypeOK(String type, String value) {
		if ("string".equals(type) && null != value && !"".equals(value))
			return true;
		if ("integer".equals(type)) {
			try {
				Integer.parseInt(value);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		if ("datetime".equals(type)) {
			try {
				Instant.parse(value);
				return true;
			} catch (IllegalArgumentException e) {
				return false;
			}
		}
		if ("boolean".equals(type)) {
			if (null == value || "true".equals(value.toLowerCase()) || "false".equals(value.toLowerCase()))
				return true;
		}
		
		return false;
	}
	
	private List<ParsedOption> parseMultipleModalOptions(String optionString, CLICommand syntaxCommand) {
		if (logger.isTraceEnabled()) logger.trace(">>> parseMultipleModalOptions({}, {})",
				optionString, (null == syntaxCommand ? TOP_LEVEL_COMMAND_NAME : syntaxCommand.getName()));
		
		List<ParsedOption> modalOptions = new ArrayList<>();
		
		OPTION_LOOP:
		for (int i = 1; i < optionString.length(); ++i) {
			Character optionShortForm = optionString.charAt(i);
			
			List<CLIOption> applicableOptions = new ArrayList<>((null == syntaxCommand ? syntax.getOptions() : syntaxCommand.getOptions()));
			applicableOptions.addAll(syntax.getGlobalOptions());
			for (CLIOption syntaxOption: applicableOptions) {
				if (null != syntaxOption.getShortForm() && syntaxOption.getShortForm().equals(optionShortForm)
						&& syntaxOption.getType().equals("boolean")) {
					ParsedOption option = new ParsedOption();
					option.setName(syntaxOption.getName());
					option.setType(syntaxOption.getType());
					option.setValue("true");
					modalOptions.add(option);
					continue OPTION_LOOP;
				}
			}
			// One option not found or not a modal option: return empty list
			return new ArrayList<>();
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< parseMultipleModalOptions()");
		return modalOptions;
	}
	
	/**
	 * Parse a single command line token as a command option
	 * 
	 * @param optionString the token to parse
	 * @param syntaxCommand
	 * @return the parsed option
	 * @throws ParseException if the option is not parseable or not acceptable for the command
	 */
	private List<ParsedOption> parseOption(String optionString, CLICommand syntaxCommand) throws ParseException {
		if (logger.isTraceEnabled()) logger.trace(">>> parseOption({}, {})",
				optionString, (null == syntaxCommand ? TOP_LEVEL_COMMAND_NAME : syntaxCommand.getName()));
		
		// Split option string in option name and value
		String optionLongForm = null;
		Character optionShortForm = null;
		String optionValue = null;
		if (optionString.startsWith("--")) {
			// Long version of option name
			if (optionString.contains("=")) {
				// Extract option value
				optionValue = optionString.split("=")[1];
			}
			optionLongForm = optionString.split("=")[0].substring(2);
		} else {
			// Short version of option name - test special case of multiple modal options first
			List<ParsedOption> multipleModalOptions = parseMultipleModalOptions(optionString, syntaxCommand);
			if (!multipleModalOptions.isEmpty()) {
				if (logger.isTraceEnabled()) logger.trace("<<< parseOption()");
				return multipleModalOptions;
			}
			// Find option value (if any) and option short form
			if (2 < optionString.length()) {
				optionValue = optionString.substring(2);
			}
			if (1 < optionString.length()) {
				optionShortForm = optionString.charAt(1);
			}
		}
		
		// Check for existence of option name
		if (null == optionLongForm && null == optionShortForm) {
			throw new ParseException(uiMsg(MSG_ID_INVALID_COMMAND_OPTION, optionString), 0);
		}
		
		// Check whether this option is allowed for the current command
		CLIOption currentSyntaxOption = null;
		List<CLIOption> applicableOptions = new ArrayList<>((null == syntaxCommand ? syntax.getOptions() : syntaxCommand.getOptions()));
		applicableOptions.addAll(syntax.getGlobalOptions());
		for (CLIOption syntaxOption: applicableOptions) {
			if (syntaxOption.getName().equals(optionLongForm) 
					|| (null != syntaxOption.getShortForm() && syntaxOption.getShortForm().equals(optionShortForm))) {
				currentSyntaxOption = syntaxOption;
				break;
			}
		}
		if (null == currentSyntaxOption) {
			throw new ParseException(uiMsg(MSG_ID_ILLEGAL_OPTION, optionString, syntaxCommand.getName()), 0);
		}

		// Check the type conformity of the option value
		if (!isTypeOK(currentSyntaxOption.getType(), optionValue)) {
			throw new ParseException(uiMsg(MSG_ID_ILLEGAL_OPTION_VALUE, optionValue,
					currentSyntaxOption.getName(), currentSyntaxOption.getType()), 0);
		}
		
		// Prepare and return the parsed option
		ParsedOption option = new ParsedOption();
		option.setName(currentSyntaxOption.getName());
		option.setType(currentSyntaxOption.getType());
		option.setValue(optionValue);
		option.setSyntaxOption(currentSyntaxOption);
		
		if (logger.isTraceEnabled()) logger.trace("<<< parseOption()");
		return Arrays.asList(option);
	}
	
	/**
	 * Parse a single command line token as a command parameter
	 * 
	 * @param parameterString the parameter to parse
	 * @param parameterPosition the position of the parameter in the list of parameters
	 * @param syntaxCommand the command syntax to check against (must not be null - no parameters for top-level proseo command)
	 * @return the parsed parameter
	 * @throws ParseException if the given parameter does not conform to the type expected at the given position, or if the number
	 * 		   number of positional parameters is exhausted
	 */
	private ParsedParameter parseParameter(String parameterString, int parameterPosition, CLICommand syntaxCommand) throws ParseException {
		if (logger.isTraceEnabled()) logger.trace(">>> parseParameter({}, {}, {})",
				parameterString, parameterPosition, syntaxCommand.getName());
				
		// Determine the correct parameter in the command syntax
		CLIParameter syntaxParameter = null;
		List<CLIParameter> syntaxParameters = syntaxCommand.getParameters();
		int maxParam = syntaxParameters.size() - 1;
		if (parameterPosition > maxParam) {
			// Check, whether last parameter is a repeatable parameter
			if (syntaxParameters.get(maxParam).getRepeatable()) {
				syntaxParameter = syntaxParameters.get(maxParam);
			} else {
				throw new ParseException(uiMsg(MSG_ID_TOO_MANY_PARAMETERS, syntaxCommand.getName()), 0);
			}
		} else {
			// Use parameter at given position
			syntaxParameter = syntaxParameters.get(parameterPosition);
		}
		
		// Check, whether parameter value conforms to the expected parameter type
		boolean isAttribute = "attribute".equals(syntaxParameter.getName());
		if (isAttribute && !parameterString.contains("=")) {
			throw new ParseException(uiMsg(MSG_ID_ATTRIBUTE_PARAMETER_EXPECTED, parameterPosition, syntaxCommand.getName()), 0);
		}
		String parameterValue = (isAttribute ? parameterString.split("=", 2)[1] : parameterString ); // 2-argument split to enable empty strings as attribute value
		if (!isTypeOK(syntaxParameter.getType(), parameterValue)) {
			throw new ParseException(uiMsg(MSG_ID_ATTRIBUTE_PARAMETER_EXPECTED, parameterPosition, syntaxCommand.getName()), 0);
		}
		
		// Prepare and return parsed parameter
		ParsedParameter parameter = new ParsedParameter();
		parameter.setName(syntaxParameter.getName());
		parameter.setType(syntaxParameter.getType());
		parameter.setValue(parameterString);
		parameter.setSyntaxParameter(syntaxParameter);
		
		if (logger.isTraceEnabled()) logger.trace("<<< parseParameter()");
		return parameter;
	}
	
	/**
	 * Parse a command string using the given syntax structure
	 * 
	 * @param commandString the command string to parse
	 * @return a command structure for execution
	 * @throws ParseException if an error is detected in the command syntax
	 */
	public ParsedCommand parse(String commandString) throws ParseException {
		if (logger.isTraceEnabled()) logger.trace(">>> parse({})", commandString);

		ParsedCommand command = new ParsedCommand();
		command.setName(TOP_LEVEL_COMMAND_NAME);
		
		// Split command string into separate tokens
		String[] commandTokens = commandString.split(" ");
		
		// Analyze command structure
		ParsedCommand currentCommand = command;
		CLICommand currentSyntaxCommand = null;
		boolean optionFound = false;
		boolean parameterFound = false;
		int parameterPosition = 0;
		
		for (String token: commandTokens) {
			if (token.isBlank()) {
				// Ignore multiple white space resulting in empty or blank tokens
				continue;
			}
			if (token.startsWith("-")) {
				// Handle token as option
				if (parameterFound) {
					// Option not allowed after parameters
					throw new ParseException(uiMsg(MSG_ID_OPTION_NOT_ALLOWED, token), 0);
				}
				optionFound = true;
				currentCommand.getOptions().addAll(parseOption(token, currentSyntaxCommand));
			} else if (optionFound || parameterFound) {
				// Handle token as parameter
				parameterFound = true;
				currentCommand.getParameters().add(parseParameter(token, parameterPosition, currentSyntaxCommand));
				++parameterPosition;
			} else {
				// Is token a valid command at the current command level?
				boolean commandFound = false;
				if (null == currentSyntaxCommand) {
					// Check token against second-level commands
					for (CLICommand syntaxCommand: syntax.getCommands()) {
						if (syntaxCommand.getName().equals(token)) {
							// Second-level command found
							currentSyntaxCommand = syntaxCommand;
							currentCommand.setName(currentSyntaxCommand.getName());
							commandFound = true;
							break;
						}
					}
				} else {
					// Check token against subcommands of current syntax command
					for (CLICommand subCommand: currentSyntaxCommand.getSubcommands()) {
						if (subCommand.getName().equals(token)) {
							// Subcommand to current command found
							currentSyntaxCommand = subCommand;
							ParsedCommand newSubCommand = new ParsedCommand();
							newSubCommand.setName(currentSyntaxCommand.getName());
							currentCommand.setSubcommand(newSubCommand);
							currentCommand = newSubCommand;
							commandFound = true;
							break;
						}
					}
				}
				if (commandFound) {
					// Handle token as command
					currentCommand.setSyntaxCommand(currentSyntaxCommand);
				} else if (null == currentSyntaxCommand) {
					// Illegal command (because no parameters are expected without a command)
					throw new ParseException(uiMsg(MSG_ID_ILLEGAL_COMMAND, token), 0);
				} else if (currentSyntaxCommand.getParameters().isEmpty()) {
					// Illegal command (because no parameters are expected without a command)
					throw new ParseException(uiMsg(MSG_ID_ILLEGAL_SUBCOMMAND, token), 0);
				} else {
					// Handle token as parameter (no more commands or options allowed)
					optionFound = true;
					parameterFound = true;
					currentCommand.getParameters().add(parseParameter(token, parameterPosition, currentSyntaxCommand));
					++parameterPosition;
				}
			}
		}
		
		// Check completeness of command parameters, except if help was invoked
		if (null != currentSyntaxCommand && !currentCommand.isHelpRequested()) {
			// Test parameters
			PARAMETER_LOOP: for (CLIParameter syntaxParameter : currentSyntaxCommand.getParameters()) {
				if (syntaxParameter.getOptional())
					continue; // Don't bother about optional parameters
				for (ParsedParameter parameter : currentCommand.getParameters()) {
					if (syntaxParameter.getName().equals(parameter.getName())) {
						continue PARAMETER_LOOP;
					}
				}
				// Required parameter not found
				throw new ParseException(uiMsg(MSG_ID_PARAMETER_MISSING, syntaxParameter.getName(), currentCommand.getName()), 0);
			} 
		}
		if (logger.isTraceEnabled()) logger.trace("<<< parse()");
		return command;
	}
}
