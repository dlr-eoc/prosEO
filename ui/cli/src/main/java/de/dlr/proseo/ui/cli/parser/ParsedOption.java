/**
 * ParsedOption.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

import static de.dlr.proseo.ui.backend.UIMessages.*;

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
			throw new IllegalArgumentException(uiMsg(MSG_ID_ILLEGAL_OPTION_TYPE, type, CLISyntax.allowedTypes.toString()));
		}
	}
	/**
	 * @return the description
	 */
	public String getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}
	/**
	 * @return the syntaxOption
	 */
	public CLIOption getSyntaxOption() {
		return syntaxOption;
	}
	/**
	 * @param syntaxOption the syntaxOption to set
	 */
	public void setSyntaxOption(CLIOption syntaxOption) {
		this.syntaxOption = syntaxOption;
	}
	
	@Override
	public String toString() {
		return "ParsedOption [\n  name=" + name + ",\n  type=" + type + ",\n  value=" + value + "\n]";
	}
	
	
}
