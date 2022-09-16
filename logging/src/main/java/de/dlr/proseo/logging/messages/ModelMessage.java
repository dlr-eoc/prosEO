/**
 * ModelMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the facility model.
 *
 * @author Katharina Bassler
 */
public enum ModelMessage implements ProseoMessage {

	SLICING_DURATION_NOT_ALLOWED		(0000, Level.ERROR, "Setting of slicing duration not allowed for slicing type ", ""),
	SLICING_OVERLAP_NOT_ALLOWED			(0000, Level.ERROR, "Setting of slicing overlap not allowed for slicing type ", ""),
	NO_ITEM_FOUND						(0000, Level.ERROR, "No item found or not enough time coverage for selection rule '{0}' and time interval ({1}, {2})", ""),
	INVALID_ITEM_TYPE					(0000, Level.ERROR, "Item with different item type found ", ""),
	CANNOT_CREATE_QUERY					(0000, Level.ERROR, "Cannot create query (cause: {0})", ""),
	POLICY_TYPE_NOT_IMPLEMENTED			(0000, Level.ERROR, "Policy type {0} not implemented", ""),
	CANNOT_ACCESS_PRODUCT_FIELD			(0000, Level.ERROR, "Cannot access product field {0} (cause: {1})", ""),	
	FILENAME_TEMPLATE_NOT_FOUND			(0000, Level.ERROR, "Product filename template for mission not found", ""),
	INVALID_PARAMETER_VALUE_FOR_TYPE	(0000, Level.ERROR, "Invalid parameter value {0} for type {1}", ""),
	PARAMETER_CANNOT_BE_CONVERTED		(0000, Level.ERROR, "Parameter of type {0} cannot be converted to {1}", ""),

	;

	private final int code;
	private final Level level;
	private final String message;
	private final String description;

	private ModelMessage(int code, Level level, String message, String description) {
		this.level = level;
		this.code = code;
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
}
