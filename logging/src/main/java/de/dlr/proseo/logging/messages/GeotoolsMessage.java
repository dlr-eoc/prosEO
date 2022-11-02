/**
 * GeoToolsMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the geotools service.
 *
 * @author Katharina Bassler
 */
public enum GeotoolsMessage implements ProseoMessage {

	SHAPE_FILE_INITIALIZED		(1500, Level.INFO, true, "Shape file {0} for type {1} initialized", ""),
	NO_SHAPE_FILES_FOUND		(1501, Level.ERROR, false, "No shape files found for type {0}. Known types: {1}",""),
	POINT_INSIDE_AREAS			(1502, Level.INFO, true, "Point {0}/{1} is inside areas {2}", ""),
	POINT_NOT_INSIDE_AREAS		(1503, Level.INFO, true, "Point {0}/{1} is NOT inside areas {2}", ""),
	POLYGON_INSIDE_AREAS		(1504, Level.INFO, true, "Polygon {0} is inside areas {1}", ""),
	POLYGON_NOT_INSIDE_AREAS	(1505, Level.INFO, true, "Polygon {0} is NOT inside areas {1}", ""),
	POLYGON_OVERLAPS			(1506, Level.INFO, true, "Polygon {0} overlaps areas {1}", ""),
	POLYGON_NO_OVERLAP			(1507, Level.INFO, true, "Polygon {0} does NOT overlap areas {1}", ""),
	
	;

	private final int code;
	private final Level level;
	private final boolean success;
	private final String message;
	private final String description;

	private GeotoolsMessage(int code, Level level, boolean success, String message, String description) {
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
