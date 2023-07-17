/**
 * ParsedOption.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;

/**
 * Class representing a parsed command option
 *
 * @author Dr. Thomas Bassler
 */
public class ParsedOption {

	/** Option name */
	private String name;
	/** Option type (see allowedTypes) */
	private String type;
	/** Option value */
	private String value;
	/** Reference to syntax element */
	private CLIOption syntaxOption;

	/**
	 * Gets the parsed option's name
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the parsed option's name
	 *
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the parsed option's type
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the parsed option's type
	 *
	 * @param type the type to set
	 */
	public void setType(String type) {
		if (CLISyntax.allowedTypes.contains(type)) {
			this.type = type;
		} else {
			throw new IllegalArgumentException(
					ProseoLogger.format(UIMessage.ILLEGAL_OPTION_TYPE, type, CLISyntax.allowedTypes.toString()));
		}
	}

	/**
	 * Gets the parsed option's description
	 *
	 * @return the description
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets the parsed option's description
	 *
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Gets the parsed option's syntax option
	 *
	 * @return the syntaxOption
	 */
	public CLIOption getSyntaxOption() {
		return syntaxOption;
	}

	/**
	 * Sets the parsed option's syntax option
	 *
	 * @param syntaxOption the syntaxOption to set
	 */
	public void setSyntaxOption(CLIOption syntaxOption) {
		this.syntaxOption = syntaxOption;
	}

	/**
	 * Returns a String representation of the parsed option
	 *
	 * @return a String representation of the parsed option
	 */
	@Override
	public String toString() {
		return "ParsedOption [\n  name=" + name + ",\n  type=" + type + ",\n  value=" + value + "\n]";
	}

}