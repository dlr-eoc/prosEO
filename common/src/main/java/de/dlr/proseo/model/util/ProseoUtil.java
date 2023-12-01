/**
 * ProseoUtil.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * Class to hold general utility methods
 *
 * @author Ernst Melchinger
 */
public class ProseoUtil {

	private static ProseoLogger logger = new ProseoLogger(ProseoUtil.class);
	/**
	 * prosEO message format, e. g. "199 proseo-processor-mgr (E2205) Product type
	 * L2________ invalid for mission NM4T"
	 */
	private static final Pattern PROSEO_MESSAGE_TEMPLATE = Pattern.compile("199 +\\S+ +(?<message>\\([IWEF]\\d+\\) .*)");
	
	/** Maximum number of retries for database concurrency issues */
	public static final int DB_MAX_RETRY = 10;
	/** Wait interval in ms before retrying database operation */
	public static final int DB_WAIT = 1000;

	/** Maximum number of retries for Kubernetes API issues */
	public static final int K8S_MAX_RETRY = 10;
	/** Wait interval in ms before retrying Kubernetes API operation */
	public static final int K8S_WAIT = 1000;

	/**
	 * Escape a give String to make it safe to be printed or stored.
	 *
	 * @param s The input String.
	 * @return The output String.
	 **/
	public static String escape(String s) {
		return s.replace("\\", "\\\\")
			.replace("\t", "\\t")
			.replace("\b", "\\b")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\f", "\\f")
			.replace("\'", "\\'")
			.replace("\"", "\\\"");
	}

	/**
	 * Extracts the prosEO-compliant message from the "Warning" header, if any
	 *
	 * @param warningHeader the HTTP Warning header to extract the message from
	 * @return the prosEO-compliant message, if there is one, or the unchanged
	 *         warning header, if there is one, or null otherwise
	 */
	public static String extractProseoMessage(String warningHeader) {

		String result = warningHeader;

		if (null != warningHeader) {
			Matcher m = PROSEO_MESSAGE_TEMPLATE.matcher(warningHeader);

			if (m.matches()) {
				result = m.group("message");
			}
		}

		return result;
	}

	/**
	 * Wait a slightly randomized amount of time, increasing progressively with the number of attempts
	 * 
	 * @param attempt the retry attempt count
	 * @param waitInterval the base wait interval to apply
	 */
	public static void randomWait(int attempt, int waitInterval) {
		long factor = (long)((Math.random() * attempt * attempt * waitInterval) + waitInterval);
		try {
			if (logger.isDebugEnabled()) logger.debug("... retrying in {} ms!", factor);
			Thread.sleep(factor);
		} catch (InterruptedException e) {
			throw new RuntimeException("Random Wait interrupted", e);
		}
	}

	/**
	 * Wait a slightly randomized amount of time based on database wait parameters
	 */
	public static void dbWait() {
		randomWait(1, DB_WAIT);
	}

	/**
	 * Wait a slightly randomized amount of time based on database wait parameters
	 * (increasing progressively with the number of attempts)
	 * 
	 * @param attempt the retry attempt count
	 */
	public static void dbWait(int attempt) {
		randomWait(attempt, DB_WAIT);
	}
	
	/**
	 * Wait a slightly randomized amount of time based on Kubernetes wait parameters
	 */
	public static void kubeWait() {
		randomWait(1, K8S_WAIT);
	}

	/**
	 * Wait a slightly randomized amount of time based on Kubernetes wait parameters
	 * (increasing progressively with the number of attempts)
	 * 
	 * @param attempt the retry attempt count
	 */
	public static void kubeWait(int attempt) {
		randomWait(attempt, K8S_WAIT);
	}
}