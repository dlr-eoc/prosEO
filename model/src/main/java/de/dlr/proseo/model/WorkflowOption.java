/**
 * WorkflowOption.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Description of possible options, which can be set as "Dynamic Processing Parameters" in Job Orders
 * created using the owning Workflow
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(indexes = {
		@Index(unique = true, columnList = "workflow_id, name") 
	})
public class WorkflowOption extends PersistentObject {
	
	/** The owning workflow */
	@ManyToOne
	private Workflow workflow;

	/** Short name of the option */
	private String name;
	
	/** Textual description of the option, defaults to an empty string */
	private String description = "";
	
	/** Data type of the option */
	@Enumerated(EnumType.STRING)
	private WorkflowOptionType type;
	
	/** The default value of the option (if a value range is given, then the default value must be contained in that range) */
	private String defaultValue;
	
	/** List of all possible values of the option (if any are given, the list is considered exhaustive) */
	@ElementCollection
	private List<String> valueRange = new ArrayList<>();
	
	/**
	 * Allowed types for workflow options
	 */
	public enum WorkflowOptionType {

		/** The option value may be any string */
		STRING("string"),

		/** The option value must be numeric, but may integer or rational */
		NUMBER("number"),

		/** Assumption is that this type means the day of year, i. e. it must be an integer number in the range 1..366 */
		DATENUMBER("datenumber");

		/** The string value associated with this enum */
		private String value;

		/** A lookup table from string value to enum */
		private static Map<String, WorkflowOptionType> valueMap = new HashMap<>();

		/**
		 * Populate the lookup table on loading time
		 */
		static
		{
			for(WorkflowOptionType type: WorkflowOptionType.values())
			{
				valueMap.put(type.getValue(), type);
			}
		}

		/**
		 * Reverse lookup of enums from their value
		 * 
		 * @param value the value to look for
		 * @return the enum associated with the value
		 */
		public static WorkflowOptionType get(String value) 
		{
			return valueMap.get(value);
		}

		/**
		 * Constructor with value string parameter
		 * 
		 * @param value the String value to associate with this enum
		 */
		WorkflowOptionType(String value) {
			this.value = value;
		}

		/**
		 * Returns the string value associated with this enum
		 * 
		 * @return a string value
		 */
		public String getValue() {
			return value;
		}
	}

	/**
	 * Gets the owning workflow
	 * 
	 * @return the workflow
	 */
	public Workflow getWorkflow() {
		return workflow;
	}

	/**
	 * Sets the owning workflow
	 * 
	 * @param workflow the workflow to set
	 */
	public void setWorkflow(Workflow workflow) {
		this.workflow = workflow;
	}

	/**
	 * Gets the option name
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the option name
	 * 
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the option description
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the option description
	 * 
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the option type
	 * 
	 * @return the type
	 */
	public WorkflowOptionType getType() {
		return type;
	}

	/**
	 * Sets the option type
	 * 
	 * @param type the type to set
	 */
	public void setType(WorkflowOptionType type) {
		this.type = type;
	}

	/**
	 * Gets the default value for the option
	 * 
	 * @return the default value if set, null otherwise
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Sets the default value for the option
	 * 
	 * @param defaultValue the default value to set
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Gets the available option values
	 * 
	 * @return the value range (empty list if not specified)
	 */
	public List<String> getValueRange() {
		return valueRange;
	}

	/**
	 * Sets the available option values
	 * 
	 * @param valueRange the value range to set
	 */
	public void setValueRange(List<String> valueRange) {
		this.valueRange = valueRange;
	}

	@Override
	public int hashCode() {
		return Objects.hash(workflow, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof WorkflowOption))
			return false;
		WorkflowOption other = (WorkflowOption) obj;
		return Objects.equals(workflow, other.getWorkflow()) && Objects.equals(name, other.getName());
	}

	@Override
	public String toString() {
		return "WorkflowOption [name=" + name + ", description=" + description + ", type=" + type + ", defaultValue=" + defaultValue
				+ ", valueRange=" + valueRange + "]";
	}

}
