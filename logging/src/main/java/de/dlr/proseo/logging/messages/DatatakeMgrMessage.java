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

	INVALID_SPACECRAFT_CODE				(3201, Level.ERROR, false, "Invalid spacecraft code {0} for mission {1}", "")

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
