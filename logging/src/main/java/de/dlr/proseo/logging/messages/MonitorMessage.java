/**
 * MonitorMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the monitor.
 *
 * @author Katharina Bassler
 */
public enum MonitorMessage implements ProseoMessage {

	ILLEGAL_CONFIG_VALUE		(0000, Level.WARN, "Illegal config value productAggregationStart: {0}", ""),
	DUPLICATE_ENTRIES			(0000, Level.WARN, "Duplicate entries in {0} at {1}", ""),
	WRONG_PRODUCTION_LATENCY	(0000, Level.ERROR, "productionLatencyAvg: {0} >= Integer.MAX_VALUE", ""),
	WRONG_TOTAL_LATENCY			(0000, Level.ERROR, "totalLatencyAvg: {0} >= Integer.MAX_VALUE", ""),
	WRONG_DOWNLOAD_SIZE			(0000, Level.ERROR, "downloadSize: {0} >= Integer.MAX_VALUE", ""),
	
	;

	private final int code;
	private final Level level;
	private final String message;
	private final String description;

	private MonitorMessage(int code, Level level, String message, String description) {
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
