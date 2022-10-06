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

	SHAPE_FILE_INITIALIZED		(0000, Level.INFO, "Shape file {0} for type {1} initialized", ""),
	NO_SHAPE_FILES_FOUND		(0000, Level.ERROR, "No shape files found for type {0}. Known types: {1}",""),
	POINT_INSIDE_AREAS			(0000, Level.INFO, "Point {0}/{1} is inside areas {2}", ""),
	POINT_NOT_INSIDE_AREAS		(0000, Level.INFO, "Point {0}/{1} is NOT inside areas {2}", ""),
	POLYGON_INSIDE_AREAS		(0000, Level.INFO, "Polygon {0} is inside areas {1}", ""),
	POLYGON_NOT_INSIDE_AREAS	(0000, Level.INFO, "Polygon {0} is NOT inside areas {1}", ""),
	POLYGON_OVERLAPS			(0000, Level.INFO, "Polygon {0} overlaps areas {1}", ""),
	POLYGON_NO_OVERLAP			(0000, Level.INFO, "Polygon {0} does NOT overlap areas {1}", ""),
	
	;

	private final int code;
	private final Level level;
	private final String message;
	private final String description;

	private GeotoolsMessage(int code, Level level, String message, String description) {
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
