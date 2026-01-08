/**
 * OdipMessage.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

public enum OrderGenMessage implements ProseoMessage {

	EXCEPTION								(4700, Level.ERROR, false, "Exception encountered (cause: {0})", ""),
	ORDER_DATA_INVALID						(4701, Level.ERROR, false, "Order data invalid (cause: {0})", ""),
	NOT_AUTHORIZED							(4702, Level.ERROR, false, "User {0} not authorized to manage {1} for mission {2}", ""),
	TRIGGER_MISSING							(4703, Level.ERROR, false, "Trigger not set", ""),
	DUPLICATE_TRIGGER						(4704, Level.ERROR, false, "Duplicate trigger for mission {0}, name {1} and type {2}", ""),
	TRIGGER_CREATED							(4705, Level.INFO, true, "Trigger {0}, type {1} created for mission {2}", ""),
	TRIGGER_NAME_MISSING					(4706, Level.ERROR, false, "Trigger name not set", ""),
	TRIGGER_MISSION_MISSING					(4707, Level.ERROR, false, "Trigger mission not set", ""),
	TRIGGER_TYPE_MISSING					(4708, Level.ERROR, false, "Trigger type not set", ""),
	TRIGGER_NOT_EXIST						(4709, Level.ERROR, false, "Trigger not exist for mission {0}, name {1} and type {2}", ""),
	TRIGGER_DELETED							(4710, Level.INFO, false, "Trigger deleted for mission {0}, name {1} and type {2}", ""),
	TRIGGER_MODIFIED						(4711, Level.INFO, true, "Trigger {0}, type {1} modified for mission {2}", ""),
	TRIGGER_NOT_MODIFIED					(4712, Level.INFO, true, "Trigger {0}, type {1} not modified for mission {2}", ""),
	TRIGGER_TYPE_DIFFER						(4713, Level.ERROR, false, "Trigger types are different", ""),
	INVALID_CRON_EXPRESSION					(4714, Level.ERROR, false, "Invalid cron expression ''{0}'' of trigger with name {1} and type {2}", ""),
	SPACECRAFT_NOT_SET						(4715, Level.ERROR, false, "Spacecraft not defined for trigger with name {0} and type {1}", ""),
	INTERVAL_NOT_SET						(4716, Level.ERROR, false, "Time interval not defined for trigger with name {0} and type {1}", ""),
	TRIGGERS_LOADED							(4717, Level.INFO, true, "Triggers of mission {0} (re-)loaded", ""),
	ONLY_BY_NAME_NOT_IMPLEMENTED			(4718, Level.WARN, true, "Find trigger only by name is not implemented", ""),
	MISSION_OR_PRODUCT_TYPE_MISSING			(4719, Level.ERROR, false, "Incomplete parameters: mission ''{0}'', product type ''{1}''", ""),
	PRODUCT_TYPE_INVALID					(4720, Level.ERROR, false, "Product type {1} invalid for mission {0}", ""),
	PRODUCT_NOT_FOUND						(4721, Level.ERROR, false, "Product with ID {0} not found in database", ""),
	ORDER_GENERATED							(4722, Level.INFO, true, "Processing Order {0} generated from trigger {1}", ""),
	NO_ORDER_GENERATED						(4723, Level.WARN, true, "No processing orders generated for product with database ID {0} (no trigger found with enabled workflows)", ""),
	TRIGGERS_RELOADED						(4724, Level.INFO, true, "Triggers of all missions (re-)loaded", ""),
	NO_TRIGGER 								(4725, Level.ERROR, false, "Trigger is not defined (NULL)", ""),
	WORKFLOW_NOT_ENABLED    				(4726, Level.ERROR, false, "Workflow referenced by name {0} is disabled (not useable)", ""),
	WORKFLOW_NOT_FOUND	    				(4727, Level.ERROR, false, "Workflow referenced by id {0} not found", ""),
	MSG_WORKFLOW_OPTION_NO_TYPE_MATCH 		(4728, Level.ERROR, false, "Workflow option {0} does not match type: {1}, value: {2}", ""),
	
	;
	
	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private OrderGenMessage(int code, Level level, boolean success, String message, String description) {
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
	 * Get a more detailed description of the message's purpose.
	 * 
	 * @return A description of the message.
	 */
	public String getDescription() {
		return description;
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
	 * Get the message's success.
	 * 
	 * @return The message's success.
	 */
	public boolean getSuccess() {
		return success;
	}
}
