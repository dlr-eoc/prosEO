/**
 * CLICommand.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a prosEO CLI command
 *
 * @author Thomas Bassler
 */
public class CLICommand {

	/** Message indicating that a parameter is optional */
	private static final String MSG_OPTIONAL = "(optional) ";
	/** Message indicating that a parameter is mandatory */
	private static final String MSG_MANDATORY = "(mandatory) ";

	/** Command name */
	private String name = "";
	/** Command description (help text) */
	private String description = "";
	/** Available subcommands */
	private List<CLICommand> subcommands = new ArrayList<>();
	/** Command options */
	private List<CLIOption> options = new ArrayList<>();
	/** Command parameters */
	private List<CLIParameter> parameters = new ArrayList<>();

	/**
	 * Gets the command name
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the command name
	 *
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the command description
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the command description
	 *
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the command's subcommands
	 *
	 * @return the subcommands
	 */
	public List<CLICommand> getSubcommands() {
		return subcommands;
	}

	/**
	 * Sets the command's subcommands
	 *
	 * @param subcommands the subcommands to set
	 */
	public void setSubcommands(List<CLICommand> subcommands) {
		this.subcommands = subcommands;
	}

	/**
	 * Gets the command's options
	 *
	 * @return the options
	 */
	public List<CLIOption> getOptions() {
		return options;
	}

	/**
	 * Sets the command's options
	 *
	 * @param options the options to set
	 */
	public void setOptions(List<CLIOption> options) {
		this.options = options;
	}

	/**
	 * Gets the command's parameters
	 *
	 * @return the parameters
	 */
	public List<CLIParameter> getParameters() {
		return parameters;
	}

	/**
	 * Sets the command's parameters
	 *
	 * @param parameters the parameters to set
	 */
	public void setParameters(List<CLIParameter> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Print help information for this command on the given print stream
	 *
	 * @param out the print stream to write to
	 */
	public void printHelp(PrintStream out) {
		out.println(description);

		out.println("Options:");
		List<CLIOption> applicableOptions = new ArrayList<>(options);
		applicableOptions.addAll(CLISyntax.inputSyntax.getGlobalOptions());
		for (CLIOption option : applicableOptions) {
			out.println(
					String.format("    %s --%-10s  %s", (null == option.getShortForm() ? "   " : "-" + option.getShortForm() + ","),
							option.getName(), option.getDescription().replace('\n', ' ')));
		}

		out.println("Positional parameters:");
		if (parameters.isEmpty()) {
			out.println("    -- none --");
		} else {
			for (CLIParameter parameter : parameters) {
				out.println(String.format("    %-16s  %s%s", parameter.getName(),
						(parameter.getOptional() ? MSG_OPTIONAL : MSG_MANDATORY), parameter.getDescription().replace('\n', ' ')));
			}
		}

		out.println("Subcommands:");
		if (subcommands.isEmpty()) {
			out.println("    -- none --");
		} else {
			for (CLICommand subcommand : subcommands) {
				out.println(String.format("    %-16s  %s", subcommand.getName(), subcommand.getDescription().replace('\n', ' ')));
			}
		}
	}

	@Override
	public String toString() {
		return "CLICommand [\n  name=" + name + ",\n  description=" + description + ",\n  subcommands=" + subcommands
				+ ",\n  options=" + options + ",\n  parameters=" + parameters + "\n]";
	}

	/**
	 * Appends the StringBuilder provided by the caller with HTML code that prints a table with the command's name and description
	 * and calls - if applicable - the printHTML() methods of subcommands, options and parameters.
	 *
	 * @param htmlDoc A StringBuilder that may already contain information.
	 * @return The same StringBuilder, appended information on the command.
	 */
	public StringBuilder printHTML(StringBuilder htmlDoc) {

		htmlDoc.append("<table>")
			.append("<tr>" + "<td>" + "Command" + "</td>" + "<td id=\"" + this.name + "\">" + "<strong>" + this.name + "</strong>"
					+ "</td>" + "</tr>")
			.append("<tr>" + "<td>" + "Description" + "</td>" + "<td>" + this.description + "." + "</td>" + "</tr>");

		// Subcommands:
		if (this.subcommands != null) {
			htmlDoc.append("<tr><td>Subcommands</td><td>");

			for (CLICommand subcommand : this.subcommands) {
				subcommand.printHTML(htmlDoc);
			}

			htmlDoc.append("</td></tr>");
		}

		// Options:
		if (this.options != null) {
			htmlDoc.append("<tr><td>Options</td><td>");

			for (CLIOption option : this.options) {
				option.printHTML(htmlDoc);
			}

			htmlDoc.append("</td></tr>");
		}

		// Parameters:
		if (this.parameters != null) {
			htmlDoc.append("<tr><td>Parameters</td><td>");

			for (CLIParameter parameter : this.parameters) {
				parameter.printHTML(htmlDoc);
			}

			htmlDoc.append("</td></tr>");
		}

		htmlDoc.append("</table>").append("<br>");
		return htmlDoc;
	}

}