/**
 * OAuthMessage.java
 * 
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed for OAuth2 authentication.
 *
 * @author Dr. Thomas Bassler
 */
public enum OAuthMessage implements ProseoMessage {

	TOKEN_REQUEST_FAILED				(5901, Level.ERROR, false, "Token request to external API {0} failed", ""),
	TOKEN_RESPONSE_INVALID				(5902, Level.ERROR, false, "Token response {0} from external API {1} invalid (cause: {2})", ""),
	ACCESS_TOKEN_MISSING				(5903, Level.ERROR, false, "Token response {0} from external API {1} does not contain access token", ""),
	TOKEN_RESPONSE_EMPTY				(5904, Level.ERROR, false, "Token response {0} from external API {1} is empty", ""),
	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private OAuthMessage(int code, Level level, boolean success, String message, String description) {
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
