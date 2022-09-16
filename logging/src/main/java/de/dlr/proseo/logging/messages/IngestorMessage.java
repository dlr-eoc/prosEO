/**
 * IngestorMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the ingestor.
 *
 * @author Katharina Bassler
 */
public enum IngestorMessage implements ProseoMessage {

	INVALID_FACILITY		(2051, Level.INFO, "Invalid processing facility {0} for ingestion", ""),
	PRODUCTS_INGESTED		(2058, Level.INFO, "{0} products ingested in processing facility {1}", ""),
	AUTH_MISSING_OR_INVALID	(2056, Level.INFO, "Basic authentication missing or invalid: {0}", ""),
	NOTIFICATION_FAILED		(2091, Level.INFO, "Notification of Production Planner failed (cause: {0})", ""),
	ERROR_ACQUIRE_SEMAPHORE	(2092, Level.ERROR,	"Error to acquire semaphore from prosEO Production Planner (cause: {0})", ""),
	ERROR_RELEASE_SEMAPHORE	(2093, Level.ERROR,	"Error to release semaphore from prosEO Production Planner (cause: {0})", "")

	;

	private final int code;
	private final Level level;
	private final String message;
	private final String description;

	private IngestorMessage(int code, Level level, String message, String description) {
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
