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
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		if (CLISyntax.allowedTypes.contains(type)) {
			this.type = type;
		} else {
			throw new IllegalArgumentException(ProseoLogger.format(
					UIMessage.ILLEGAL_PARAMETER_TYPE, 
					type, CLISyntax.allowedTypes.toString()));
		}
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
	 * @return the optional
	 */
	public Boolean getOptional() {
		return optional;
	}
	/**
	 * @param optional the optional to set
	 */
	public void setOptional(Boolean optional) {
		this.optional = optional;
	}
	/**
	 * @return the repeatable
	 */
	public Boolean getRepeatable() {
		return repeatable;
	}
	/**
	 * @param repeatable the repeatable to set
	 */
	public void setRepeatable(Boolean repeatable) {
		this.repeatable = repeatable;
	}
	
	@Override
	public String toString() {
		return "CLIParameter [\n  name=" + name + ",\n  type=" + type + ",\n  description=" + description + ",\n  optional=" + optional
				+ ",\n  repeatable=" + repeatable + "\n]";
	}

	/**
	 * Appends the StringBuilder provided by the caller with HTML code that prints a
	 * table with the parameter's name and a description. If applicable, the
	 * parameter is marked as optional and/or repeatable.
	 * 
	 * @param htmlDoc A StringBuilder that may already contain information.
	 * @return The same StringBuilder, appended information on the option.
	 */
	public StringBuilder printHTML(StringBuilder htmlDoc) {

		htmlDoc.append(
				"<table>" + "<tr>" + "<td>" + "Parameter" + "</td>" + "<td>" + "<strong>" + this.name + "</strong>");

		if (this.optional != null)
			if (this.repeatable != null)
				htmlDoc.append(" (optional, repeatable)");
			else
				htmlDoc.append(" (optional)");
		else if (this.repeatable != null)
			htmlDoc.append(" (repeatable)");

		htmlDoc.append("</td>" + "</tr>")
				.append("<tr>" + "<td>" + "Type" + "</td>" + "<td>" + this.type + "</td>" + "</tr>").append("<tr>"
						+ "<td>" + "Description" + "</td>" + "<td>" + this.description + "." + "</td>" + "</tr>");

		htmlDoc.append("</table>").append("<br>");

		return htmlDoc;
	}

}
