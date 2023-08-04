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
	
	EXCEPTION_ENCOUNTERED				(9001, Level.ERROR, false, "Exception encountered: {0}", ""),
	FACILITY_NOT_AVAILABLE				(9002, Level.WARN, true, "Processing facility {0} not available (cause: {1})", ""),
	FALSE								(9003, Level.ERROR, false, "false", ""),
	ILLEGAL_CROSS_MISSION_ACCESS		(9004, Level.ERROR, false, "Illegal cross-mission access to mission {0} (logged in to {1})", ""),
	ILLEGAL_FACILITY_STATE_TRANSITION 	(9005, Level.ERROR, false, "Illegal facility state transition from {0} to {1}", ""),
	ILLEGAL_JOB_STATE_TRANSITION 		(9006, Level.ERROR, false, "Illegal job state transition from {0} to {1}", ""),
	ILLEGAL_ORDER_STATE_TRANSITION 		(9007, Level.ERROR, false, "Illegal order state transition from {0} to {1}", ""),
	INITIALIZING_AUTHENTICATION			(9008, Level.INFO, true, "Initializing authentication from user details service", ""),
	INITIALIZING_USER_DETAILS_SERVICE	(9009, Level.INFO, true, "Initializing user details service from datasource {0}", ""),
	INVALID_PROCESSING_MODE 			(9010, Level.ERROR, false, "Processing mode {0} not defined for mission {1}", ""),
	OK									(9011, Level.INFO, true, "Okay", ""),
	RUNTIME_EXCEPTION_ENCOUNTERED		(9012, Level.WARN, true, "Exception encountered: {0}", ""),
	TRUE								(9013, Level.INFO, true, "true", ""),
	UNDEFINED							(9014, Level.ERROR, false, "An unknown error occured.", ""),
	TOO_MANY_RESULTS					(9015, Level.ERROR, false, "The number of {0} matching the search criteria ({1}) exceeds the configured maximum number of results ({2})", ""),
	FIELD_NOT_SET						(9016, Level.ERROR, false, "Specification of {0} is mandatory for {1}", ""),
	NO_UUID_MODIFICATION				(9017, Level.ERROR, false, "UUID may not be modified", ""),
	INVALID_PARAMETER_FORMAT			(9018, Level.ERROR, false, "\"{0}\" does not conform to parameter format", ""),
	CONCURRENT_MODIFICATION			(9019, Level.ERROR, false, "Entity of type \"{0}\" with id {1} was modified concurrently", ""),
	
	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	public final boolean success;

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
