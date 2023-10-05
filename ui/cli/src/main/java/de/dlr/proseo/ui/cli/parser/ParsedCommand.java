/**
 * ParsedCommand.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a parsed command line command
 *
 * @author Dr. Thomas Bassler
 */
public class ParsedCommand {

	/** Command name */
	private String name;
	/** Applicable subcommand */
	private ParsedCommand subcommand;
	/** Command options */
	private List<ParsedOption> options = new ArrayList<>();
	/** Command parameters */
	private List<ParsedParameter> parameters = new ArrayList<>();
	/** Reference to syntax element */
	private CLICommand syntaxCommand;

	/**
	 * Gets the parsed command's name
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the parsed command's name
	 *
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the parsed command's subcommand
	 *
	 * @return the subcommand
	 */
	public ParsedCommand getSubcommand() {
		return subcommand;
	}

	/**
	 * Sets the parsed command's subcommand
	 *
	 * @param subcommand the subcommand to set
	 */
	public void setSubcommand(ParsedCommand subcommand) {
		this.subcommand = subcommand;
	}

	/**
	 * Gets the parsed command's options
	 *
	 * @return the options
	 */
	public List<ParsedOption> getOptions() {
		return options;
	}

	/**
	 * Sets the parsed command's options
	 *
	 * @param options the options to set
	 */
	public void setOptions(List<ParsedOption> options) {
		this.options = options;
	}

	/**
	 * Check, whether the "help" option was set
	 *
	 * @return true, if help was requested, false otherwise
	 */
	public boolean isHelpRequested() {
		for (ParsedOption option : options) {
			if ("help".equals(option.getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets the parsed command's parameters
	 *
	 * @return the parameters
	 */
	public List<ParsedParameter> getParameters() {
		return parameters;
	}

	/**
	 * Sets the parsed command's parameters
	 *
	 * @param parameters the parameters to set
	 */
	public void setParameters(List<ParsedParameter> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Gets the parsed command's syntax command
	 *
	 * @return the syntaxCommand
	 */
	public CLICommand getSyntaxCommand() {
		return syntaxCommand;
	}

	/**
	 * Sets the parsed command's syntax command
	 *
	 * @param syntaxCommand the syntaxCommand to set
	 */
	public void setSyntaxCommand(CLICommand syntaxCommand) {
		this.syntaxCommand = syntaxCommand;
	}

	/**
	 * Returns a String representation of the parsed command
	 *
	 * @return a String representation of the parsed command
	 */
	@Override
	public String toString() {
		return "CLICommand [\n  name=" + name + ",\n  subcommand=" + subcommand + ",\n  options=" + options + ",\n  parameters="
				+ parameters + "\n]";
	}

}