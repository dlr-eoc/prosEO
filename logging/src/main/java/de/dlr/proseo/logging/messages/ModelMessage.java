/**
 * ModelMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the model.
 *
 * @author Katharina Bassler
 */
public enum ModelMessage implements ProseoMessage {

	SLICING_DURATION_NOT_ALLOWED		(2500, Level.ERROR, false, "Setting of slicing duration not allowed for slicing type ", ""),
	SLICING_OVERLAP_NOT_ALLOWED			(2501, Level.ERROR, false, "Setting of slicing overlap not allowed for slicing type ", ""),
	NO_ITEM_FOUND						(2502, Level.ERROR, false, "No item found or not enough time coverage for selection rule '{0}' and time interval ({1}, {2})", ""),
	INVALID_ITEM_TYPE					(2503, Level.ERROR, false, "Item with different item type found ", ""),
	CANNOT_CREATE_QUERY					(2504, Level.ERROR, false, "Cannot create query (cause: {0})", ""),
	POLICY_TYPE_NOT_IMPLEMENTED			(2505, Level.ERROR, false, "Policy type {0} not implemented", ""),
	CANNOT_ACCESS_PRODUCT_FIELD			(2506, Level.ERROR, false, "Cannot access product field {0} (cause: {1})", ""),	
	FILENAME_TEMPLATE_NOT_FOUND			(2507, Level.ERROR, false, "Product filename template for mission not found", ""),
	INVALID_PARAMETER_VALUE_FOR_TYPE	(2508, Level.ERROR, false, "Invalid parameter value {0} for type {1}", ""),
	PARAMETER_CANNOT_BE_CONVERTED		(2509, Level.ERROR, false, "Parameter of type {0} cannot be converted to {1}", ""),
	PROPERTY_COLUMNS_FOUND				(2510, Level.WARN, true, "Found {0} columns for property {1}", ""),
	ATTRIBUTE_COLUMN_MAP_NOT_GENERATED	(2511, Level.ERROR, false, "Cannot generate attribute/column map (cause: {0})", ""),
	INCOMPLETE_PRODUCT_QUERY			(2512, Level.ERROR, false, "Incomplete product query {0}", ""),
	
	;

	private final int code;
	private final Level level;
	private final boolean success;
	private final String message;
	private final String description;

	private ModelMessage(int code, Level level, boolean success, String message, String description) {
		this.level = level;
		this.code = code;
		this.success = success;
		this.message = message;
		this.description = description;
	}

	/**
	 * Get the message's code.
	 * 
	 * @return The message code.
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get the message's level.
	 * 
	 * @return The message level.
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * Get the message.
	 * 
	 * @return The message.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Get a more detailed description of the message's purpose.
	 * 
	 * @return A description of the message.
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Get the message's success.
	 * 
	 * @return The message's success.
	 */
	public boolean getSuccess() {
		return success;
	}
}
