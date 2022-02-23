/**
 * CLIParameter.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli.parser;

import static de.dlr.proseo.ui.backend.UIMessages.*;

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
			throw new IllegalArgumentException(uiMsg(MSG_ID_ILLEGAL_PARAMETER_TYPE, type, CLISyntax.allowedTypes.toString()));
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


}
