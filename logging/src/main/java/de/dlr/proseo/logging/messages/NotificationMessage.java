package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

public enum NotificationMessage implements ProseoMessage {

	MSG_ENDPOINT_NOT_SET 		(5500, Level.ERROR, false, "No endpoint defined", ""),
	MSG_ENDPOINT_TYPE_UNKNOWN	(5501, Level.ERROR, false, "The endpint type {} is unknown", ""),
	MSG_USER_PASSWORD_NOT_SET	(5502, Level.ERROR, false, "HTTP(S) endpoints need a user and password", ""), 
	MSG_MISSING_MESSAGE_CONTENT	(5503, Level.ERROR, false, "Message content is missed", ""),
	MSG_INVALID_CONTENT_TYPE	(5504, Level.WARN, false, "Invalid content type, set to {}", ""),
	MESSAGING_EXCEPTION			(5505, Level.ERROR, false, "Error sending MIME message: {}", ""),
	
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