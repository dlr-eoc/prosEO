/**
 * CLIOption.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

/**
 * Class representing a prosEO CLI command option
 * 
 * @author Dr. Thomas Bassler
 */
public class CLIOption {

	/* Message ID constants */
	private static final int MSG_ID_ILLEGAL_OPTION_TYPE = 2902;

	/* Message strings */
	private static final String MSG_ILLEGAL_OPTION_TYPE = "(E%d) Illegal option type %s, expected one of %s";

	/** Option name */
	private String name = "";
	/** Option type (see allowedTypes) */
	private String type = "string";
	/** Option description (help text) */
	private String description = "";
	/** Option short form */
	private Character shortForm = null;
	
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
			throw new IllegalArgumentException(String.format(MSG_ILLEGAL_OPTION_TYPE,
					MSG_ID_ILLEGAL_OPTION_TYPE, type, CLISyntax.allowedTypes.toString()));
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
	 * @return the shortForm
	 */
	public Character getShortForm() {
		return shortForm;
	}
	/**
	 * @param shortForm the shortForm to set
	 */
	public void setShortForm(Character shortForm) {
		this.shortForm = shortForm;
	}
	
	@Override
	public String toString() {
		return "CLIOption [\n  name=" + name + ",\n  type=" + type + ",\n  description=" + description + ",\n  shortForm=" + shortForm + "\n]";
	}
	
	/**
	 * Appends the StringBuilder provided by the caller with HTML code that prints a
	 * table with the option's name, short form (if applicable) and description.
	 * 
	 * @param htmlDoc A StringBuilder that may already contain information.
	 * @return The same StringBuilder, appended information on the option.
	 */
	public StringBuilder printHTML(StringBuilder htmlDoc) {

		htmlDoc.append("<table>").append("<tr>" + "<td>" + "Option" + "</td>" + "<td>" + "<strong>" + this.name
				+ "</strong>" + "</td>" + "</tr>");

		if (this.shortForm != null) {
			htmlDoc.append("<tr>" + "<td>" + "Short form" + "</td>" + "<td>" + "<em>" + this.shortForm + "</em>"
					+ "</td>" + "</tr>");
		}

		htmlDoc.append("<tr>" + "<td>" + "Type" + "</td>" + "<td>" + this.type + "</td>" + "</tr>")
				.append("<tr>" + "<td>" + "Description" + "</td>" + "<td>" + this.description + "." + "</td>" + "</tr>")
				.append("</table>").append("<br>");

		return htmlDoc;
	}
	
}
