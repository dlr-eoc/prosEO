/**
 * OrbitTimeFormatter.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

/**
 * Master time format for orbit times (ISO-formatted UTC-STS timestamps with microsecond fraction and without time zone)
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class OrbitTimeFormatter {

	private static final DateTimeFormatter orbitTimeFormatter =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS").withZone(ZoneId.of("UTC"));
	
	/**
	 * Format a timestamp object in orbit time format
	 * 
	 * @param instant the timestamp to format
	 * @return a string with the formatted timestamp
	 * @throws DateTimeException if an error occurs during formatting
	 */
	public static String format(TemporalAccessor instant) throws DateTimeException {
		return orbitTimeFormatter.format(instant);
	}
	
	/**
	 * Parse a timestamp string in orbit time format
	 * 
	 * @param timestamp the timestamp string to parse
	 * @return a timestamp object representing the time
	 * @throws DateTimeParseException if unable to parse the requested result
	 */
	public static TemporalAccessor parse(String timestamp) throws DateTimeParseException {
		return orbitTimeFormatter.parse(timestamp);
	}

}
