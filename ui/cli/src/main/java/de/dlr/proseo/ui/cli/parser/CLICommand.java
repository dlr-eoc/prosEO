/**
 * CLICommand.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a prosEO CLI command
 * 
 * @author thomas
 *
 */
public class CLICommand {

	/** Command name */
	private String name;
	/** Command description (help text) */
	private String description;
	/** Available subcommands */
	private List<CLICommand> subcommands = new ArrayList<>();
	/** Command options */
	private List<CLIOption> options = new ArrayList<>();
	/** Command parameters */
	private List<CLIParameter> parameters = new ArrayList<>();
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the subcommands
	 */
	public List<CLICommand> getSubcommands() {
		return subcommands;
	}
	/**
	 * @param subcommands the subcommands to set
	 */
	public void setSubcommands(List<CLICommand> subcommands) {
		this.subcommands = subcommands;
	}
	/**
	 * @return the options
	 */
	public List<CLIOption> getOptions() {
		return options;
	}
	/**
	 * @param options the options to set
	 */
	public void setOptions(List<CLIOption> options) {
		this.options = options;
	}
	/**
	 * @return the parameters
	 */
	public List<CLIParameter> getParameters() {
		return parameters;
	}
	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters(List<CLIParameter> parameters) {
		this.parameters = parameters;
	}
	
	@Override
	public String toString() {
		return "CLICommand [\n  name=" + name + ",\n  description=" + description + ",\n  subcommands=" + subcommands + ",\n  options=" + options
				+ ",\n  parameters=" + parameters + "\n]";
	}


}
