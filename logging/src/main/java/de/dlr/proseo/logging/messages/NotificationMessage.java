/**
 * MonitorMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the monitor.
 *
 * @author Ernst Melchinger
 */
public enum NotificationMessage implements ProseoMessage {

	MSG_ENDPOINT_NOT_SET 		(1700, Level.ERROR, false, "No endpoint defined", ""),
	MSG_ENDPOINT_TYPE_UNKNOWN	(1701, Level.ERROR, false, "The endpint type {0} is unknown", ""),
	MSG_USER_PASSWORD_NOT_SET	(1702, Level.ERROR, false, "HTTP(S) endpoints need a user and password", ""), 
	MSG_MISSING_MESSAGE_CONTENT	(1703, Level.ERROR, false, "Message content is missed", ""),
	MSG_INVALID_CONTENT_TYPE	(1704, Level.WARN, false, "Invalid content type, set to {0}", ""),
	MESSAGING_EXCEPTION			(1705, Level.ERROR, false, "Error sending MIME message: {0}", ""),
	UNKNOWN_MEDIATYPE			(1706, Level.ERROR, false, "The MediaType {0} is unknown, use simple message", ""),
	MESSAGE_SENT				(1707, Level.INFO, true, "Notification message to {0} successfully sent", ""),
	;
	
	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private NotificationMessage(int code, Level level, boolean success, String message, String description) {
		this.code = code;
		this.level = level;
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
	 * Get the message''s success.
	 * 
	 * @return The message's success.
	 */
	public boolean getSuccess() {
		return success;
	}

}
