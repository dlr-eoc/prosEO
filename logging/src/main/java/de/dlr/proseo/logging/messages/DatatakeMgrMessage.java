/**
 * DatatakeMgrMessage.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the datatake manager.
 *
 * @author Katharina Bassler
 */
public enum DatatakeMgrMessage implements ProseoMessage {

	INVALID_SPACECRAFT_CODE				(3201, Level.ERROR, false, "Invalid spacecraft code {0} for mission {1}", ""),
	BOUNDS_MISMATCH						(3202, Level.ERROR, false, "Upper record number {0} is below lower record number {1}", ""),
	SPACECRAFT_NOT_FOUND				(3203, Level.ERROR, false, "Spacecraft {0}s not found in mission {1}", ""),
	DATATAKE_NOT_FOUND					(3204, Level.ERROR, false, "Datatake with database ID {0} not found for spacecraft {1} and mission {2}", ""),
	DATATAKE_ALREADY_PROCESSED			(3205, Level.WARN, false, "Datatake {0} for spacecraft {1} has already been processed", ""),
	DATATAKES_RETRIEVED					(3205, Level.INFO, false, "{0} datatakes retrieved for spacecraft {1}", ""),
	DATATAKE_PROCESSED					(3206, Level.INFO, false, "Datatake {0} for spacecraft {1} marked as processed", ""),
	INVALID_START_TIMES					(3207, Level.ERROR, false, "Sensing start time {0} or stop time {1} invalid for datatake {2}", "")

	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private DatatakeMgrMessage(int code, Level level, boolean success, String message, String description) {
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
	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get a more detailed description of the message's purpose.
	 *
	 * @return A description of the message.
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Get the message's level.
	 *
	 * @return The message level.
	 */
	@Override
	public Level getLevel() {
		return level;
	}

	/**
	 * Get the message.
	 *
	 * @return The message.
	 */
	@Override
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
