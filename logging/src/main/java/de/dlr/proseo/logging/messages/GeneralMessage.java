/**
 * GeneralMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed across services.
 *
 * @author Katharina Bassler
 */
public enum GeneralMessage implements ProseoMessage {
	
	TRUE								(9000, Level.INFO, true, "true", ""),
	FALSE								(9001, Level.ERROR, false, "false", ""),
	OK									(9002, Level.INFO, true, "Okay", ""),
	UNDEFINED							(9003, Level.ERROR, false, "An unknown error occured.", ""),
	ILLEGAL_CROSS_MISSION_ACCESS		(9004, Level.ERROR, false, "Illegal cross-mission access to mission {0} (logged in to {1})", ""),
	EXCEPTION_ENCOUNTERED				(9005, Level.ERROR, false, "Exception encountered: {0}", ""),
	RUNTIME_EXCEPTION_ENCOUNTERED		(9006, Level.WARN, true, "Exception encountered: {0}", ""),
	ILLEGAL_ORDER_STATE_TRANSITION 		(9007, Level.ERROR, false, "Illegal order state transition from {0} to {1}", ""),
	ILLEGAL_JOB_STATE_TRANSITION 		(9008, Level.ERROR, false, "Illegal job state transition from {0} to {1}", ""),
	ILLEGAL_FACILITY_STATE_TRANSITION 	(9009, Level.ERROR, false, "Illegal facility state transition from {0} to {1}", ""),
	INVALID_PROCESSING_MODE 			(9010, Level.ERROR, false, "Processing mode {0} not defined for mission {1}", ""),
	INITIALIZING_AUTHENTICATION			(1136, Level.INFO, true, "Initializing authentication from user details service", ""),
	INITIALIZING_USER_DETAILS_SERVICE	(1137, Level.INFO, true, "Initializing user details service from datasource {0}", "")

	;

	private final int code;
	private final Level level;
	public final boolean success;
	private final String message;
	private final String description;

	private GeneralMessage(int code, Level level, boolean success, String message, String description) {
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
