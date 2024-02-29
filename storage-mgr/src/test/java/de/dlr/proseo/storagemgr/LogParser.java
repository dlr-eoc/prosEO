package de.dlr.proseo.storagemgr;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * LogParser Class
 * 
 * - Intercepted: 2024-02-22 20:23:08.000 TRACE 14224 --- [ Thread-6]
 * d.dlr.proseo.storagemgr.utils.FileUtils : >>>
 * createFile(2024-02-22T19:23:08.000469Z)
 * 
 * @author Denys Chaykovskiy
 * 
 */

public class LogParser {

	private String logString;

	public static void main(String[] args) {

		String logString = "Intercepted: 2024-02-22 20:23:08.000 TRACE 14224 --- [       Thread-6] d.dlr.proseo.storagemgr.utils.FileUtils  : >>> createFile(2024-02-22T19:23:08.000469Z)";

		LogParser logParser = new LogParser(logString);

		logParser.parse().print();
	}

	/**
	 * Constructor
	 * 
	 * @param logString
	 */
	public LogParser(String logString) {

		this.logString = logString;
	}

	/**
	 * Checks if the string is a log string
	 * 
	 * @return
	 */
	public boolean isLogString() {

		String[] sublines = logString.split("\\s+");

		if (sublines.length < 11)
			return false;

		// TODO: add additional conditions like thread name has "thread"

		return true;
	}

	/**
	 * Parses a log record
	 * 
	 * @return
	 */
	public LogRecord parse() {

		String[] sublines = logString.split("\\s+");

//		for (String subline : sublines) {
//			System.out.println(subline);
//		}

		Instant timestamp = parseTimeStamp(sublines[1] + " " + sublines[2]);

		String threadName = parseThreadName(sublines[7]);

		String className = parseClassName(sublines[8]);

		String content = sublines[10];

		for (int i = 11; i < sublines.length; i++) {

			content += " " + sublines[i];
		}

		content = parseContent(content);

		LogRecord logRecord = new LogRecord(timestamp, threadName, className, content);

		return logRecord;
	}

	/**
	 * Parses a timestamp
	 * 
	 * @param timestamp
	 * @return
	 */
	private static Instant parseTimeStamp(String timestamp) {

		// String timestampString = "2024-02-22 20:23:08.000";

		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
					.withZone(ZoneId.systemDefault());
			return Instant.from(formatter.parse(timestamp));

		} catch (Exception e) {
			System.err.println("Error parsing Instant: " + e.getMessage());

			throw new DateTimeException(e.getMessage(), e);
		}
	}

	/**
	 * Parses a thread name
	 * 
	 * @param threadName
	 * @return
	 */
	private static String parseThreadName(String threadName) {

		return threadName.replace("]", "");
	}

	/**
	 * Parses a class name
	 * 
	 * @param className
	 * @return
	 */
	private static String parseClassName(String className) {

		return className;
	}

	/**
	 * Parses a content
	 * 
	 * @param content
	 * @return
	 */
	private static String parseContent(String content) {

		return content.replace(">>> ", "");
	}
}
