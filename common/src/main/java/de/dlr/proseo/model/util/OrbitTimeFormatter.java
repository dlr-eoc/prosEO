/**
 * OrbitTimeFormatter.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Master time format for orbit times (ISO-formatted UTC-STS timestamps with microsecond fraction and without time zone)
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class OrbitTimeFormatter {

	private static final String MSG_CANNOT_PARSE_DATE_TIME_STRING = "Cannot parse date/time string %s at index %d";
	
	/** Lenient date/time parsing pattern */
	private static Pattern dateTimePattern = Pattern.compile(
			"(?<year>\\d\\d\\d\\d)-(?<month>\\d\\d)-(?<day>\\d\\d)" + 	// date (groups 1-3)
			"(?:T(?<hour>\\d\\d)\\:(?<minute>\\d\\d)" + 				// optional hour and minute (groups 4 and 5)
				"(?:\\:(?<second>\\d\\d)" + 							// optional second (group 6)
					"(?:\\.(?<frac>\\d{1,6})" + 							// optional fraction of a second (group 7)
					")?" +
				")?" +
			")?" +
			"(?<zone>[GZz+-].*)?"										// optional time zone (group 8)
			);

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
		try {
			return orbitTimeFormatter.parse(timestamp);
		} catch (DateTimeParseException e) {
			return parseDateTime(timestamp);
		}
	}

	/**
	 * Parse a date and time string in the format "yyyy-MM-dd'T'HH:mm:ss.SSSSSS[zZX]", whereby after the following variants 
	 * are allowed:
	 * <ul>
	 *   <li>yyyy-MM-dd</li>
	 *   <li>yyyy-MM-ddTHH:mm</li>
	 *   <li>yyyy-MM-ddTHH:mm:ss</li>
	 *   <li>yyyy-MM-ddTHH:mm:ss.S[SSSSS]</li>
	 *   <li>any of the above plus a time zone in general, RFC 822 or ISO 8601 format</li>
	 * </ul>
	 * Missing parts are set to zero, a missing time zone is set to UTC.
	 * 
	 * @param dateTime the date and time string to parse
	 * @return the parsed point in time
	 * @throws DateTimeException if the given string cannot be parsed according to the format given above
	 */
	public static Instant parseDateTime(String dateTime) throws DateTimeParseException {
		// Check the format of the input string
		Matcher m = dateTimePattern.matcher(dateTime);
		
		if (!m.matches()) {
			throw new DateTimeParseException(String.format(MSG_CANNOT_PARSE_DATE_TIME_STRING, dateTime, 0), dateTime, 0);
		}
		
		// Check which parts of the date/time string are available
		int year = Integer.parseInt(m.group("year"));
		int month = Integer.parseInt(m.group("month"));
		int day = Integer.parseInt(m.group("day"));
		int hour = ( null == m.group("hour") ? 0 : Integer.parseInt(m.group("hour")) );
		int minute =  (null == m.group("minute") ? 0 : Integer.parseInt(m.group("minute")) );
		int second = ( null == m.group("second") ? 0 : Integer.parseInt(m.group("second")) );
		// Nanoseconds need to padded with trailing zeroes
		int nano = 0;
		if (null != m.group("frac")) {
			StringBuilder milliString = new StringBuilder(m.group("frac"));
			for (int i = milliString.length(); i < 6; ++i) {
				milliString.append('0');
			}
			nano = Integer.parseInt(milliString.toString()) * 1000;
		}
		
		// Check the time zone
		TimeZone tz = TimeZone.getTimeZone("UTC");
		if (null != m.group("zone")) {
			String tzString = m.group("zone");
			if ("Z".equals(tzString.toUpperCase())) {
				// do nothing, that's the default
			} else if (tzString.matches("GMT[+-]\\d\\d?:?\\d?\\d?")) {
				// Java time zone
				tz = TimeZone.getTimeZone(tzString);
			} else if (tzString.matches("[+-]\\d\\d?:?\\d?\\d?")) {
				// ISO 8601 or RFC 822 time zone
				tz = TimeZone.getTimeZone("GMT" + tzString);
			} else {
				throw new DateTimeParseException(
						String.format(MSG_CANNOT_PARSE_DATE_TIME_STRING, dateTime, m.start("zone")), dateTime, m.start("zone"));
			}
		}
		
		// Try to create an instant from the given numbers
		LocalDate date = LocalDate.of(year, month, day);
		LocalTime time = LocalTime.of(hour, minute, second, nano);
		ZonedDateTime zonedDateTime = ZonedDateTime.of(LocalDateTime.of(date, time), tz.toZoneId());
		Instant result = Instant.from(zonedDateTime);
		result.minusNanos(0); // force nano component of Instant
		
		return result;
	}
	
}
