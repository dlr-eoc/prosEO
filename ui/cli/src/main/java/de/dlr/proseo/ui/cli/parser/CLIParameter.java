/**
 * CLIParameter.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;

/**
 * Class representing a CLI command parameter
 *
 * @author Dr. Thomas Bassler
 */
public class CLIParameter {

	/** Parameter name */
	private String name = "";
	/** Parameter type (see allowedTypes) */
	private String type = "string";
	/** Parameter description (help text) */
	private String description = "";
	/** Flag, whether parameter is optional */
	private Boolean optional = false;
	/** Flag, whether parameter can occur multiple times */
	private Boolean repeatable = false;

	/**
	 * Gets the parameter's name
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the parameter's name
	 *
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the parameter's type
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the parameter's type
	 *
	 * @param type the type to set
	 */
	public void setType(String type) {
		if (CLISyntax.allowedTypes.contains(type)) {
			this.type = type;
		} else {
			throw new IllegalArgumentException(
					ProseoLogger.format(UIMessage.ILLEGAL_PARAMETER_TYPE, type, CLISyntax.allowedTypes.toString()));
		}
	}

	/**
	 * Gets the parameter's description
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the parameter's description
	 *
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Return whether the parameter is optional
	 *
	 * @return true if the parameter is optional, false otherwise
	 */
	public Boolean getOptional() {
		return optional;
	}

	/**
	 * Set whether the parameter is optional
	 *
	 * @param optional true if the parameter is to be optional, false otherwise
	 */
	public void setOptional(Boolean optional) {
		this.optional = optional;
	}

	/**
	 * Return whether the parameter is repeatable
	 *
	 * @return true if the parameter is repeatable, false otherwise
	 */
	public Boolean getRepeatable() {
		return repeatable;
	}

	/**
	 * Set whether the parameter is repeatable
	 *
	 * @param repeatable true if the parameter is to be repeatable, false otherwise
	 */
	public void setRepeatable(Boolean repeatable) {
		this.repeatable = repeatable;
	}

	/**
	 * Returns a String representation of the parameter
	 *
	 * @return a String representation of the parameter
	 */
	@Override
	public String toString() {
		return "CLIParameter [\n  name=" + name + ",\n  type=" + type + ",\n  description=" + description + ",\n  optional="
				+ optional + ",\n  repeatable=" + repeatable + "\n]";
	}

	/**
	 * Appends the StringBuilder provided by the caller with HTML code that prints a table with the parameter's name and a
	 * description. If applicable, the parameter is marked as optional and/or repeatable.
	 *
	 * @param htmlDoc A StringBuilder that may already contain information.
	 * @return The same StringBuilder, appended information on the option.
	 */
	public StringBuilder printHTML(StringBuilder htmlDoc) {

		htmlDoc.append("<table>" + "<tr>" + "<td>" + "Parameter" + "</td>" + "<td>" + "<strong>" + this.name + "</strong>");

		if (this.optional)
			if (this.repeatable)
				htmlDoc.append(" (optional, repeatable)");
			else
				htmlDoc.append(" (optional)");
		else if (this.repeatable)
			htmlDoc.append(" (repeatable)");

		htmlDoc.append("</td>" + "</tr>")
			.append("<tr>" + "<td>" + "Type" + "</td>" + "<td>" + this.type + "</td>" + "</tr>")
			.append("<tr>" + "<td>" + "Description" + "</td>" + "<td>" + this.description + "." + "</td>" + "</tr>");

		htmlDoc.append("</table>").append("<br>");

		return htmlDoc;
	}

}