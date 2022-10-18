/**
 * ParsedParameter.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;

/**
 * Class representing a parsed command parameter
 * 
 * @author Dr. Thomas Bassler
 */
public class ParsedParameter {

	/** Parameter name */
	private String name;
	/** Parameter type (see allowedTypes) */
	private String type;
	/** Parameter value */
	private String value;
	/** Reference to syntax element */
	private CLIParameter syntaxParameter;
	
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
				UIMessage.ILLEGAL_PARAMETER_TYPE, type, CLISyntax.allowedTypes.toString()));
		}
	}
	/**
	 * @return the value
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
	 * @return the syntaxParameter
	 */
	public CLIParameter getSyntaxParameter() {
		return syntaxParameter;
	}
	
	/**
	 * @param syntaxParameter the syntaxParameter to set
	 */
	public void setSyntaxParameter(CLIParameter syntaxParameter) {
		this.syntaxParameter = syntaxParameter;
	}
	
	@Override
	public String toString() {
		return "CLIParameter [\n  name=" + name + ",\n  type=" + type + ",\n  value=" + value + "\n]";
	}


}
