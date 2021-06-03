/**
 * LogUtil.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataServerError;
import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * Utility methods for logging and error reporting
 * 
 * @author Dr. Thomas Bassler
 */
public class LogUtil {

	// prosEO message format, e. g. "(E2205) Product type L2________ invalid for mission NM4T"
	private static final Pattern PROSEO_MESSAGE_TEMPLATE = Pattern.compile("\\((?<messageCode>[IWEF]\\d+)\\) .*");

	/**
	 * Create and log a formatted message at the given level
	 * 
	 * @param level the logging level to use
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	public static String log(Logger logger, Level level, String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		if (Level.ERROR.equals(level)) {
			logger.error(message);
		} else if (Level.WARN.equals(level)) {
			logger.warn(message);
		} else {
			logger.info(message);
		}

		return message;
	}

	/**
	 * Create and log a formatted error message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted error message
	 */
	public static String logError(Logger logger, String messageFormat, int messageId, Object... messageParameters) {
		return log(logger, Level.ERROR, messageFormat, messageId, messageParameters);
	}

	/**
	 * Create an ODataServerError object for the error response body from an HTTP status code and a message
	 * 
	 * @param statusCode the HTTP status code to set
	 * @param message the message to set
	 * @return
	 */
	public static ODataServerError oDataServerError(int statusCode, String message) {
		ODataServerError serverError = new ODataServerError();

		serverError.setStatusCode(statusCode);
		
		Matcher m = PROSEO_MESSAGE_TEMPLATE.matcher(message);
		if (m.matches()) {
			serverError.setCode(m.group("messageCode"));
		} else {
			serverError.setCode(HttpStatusCode.fromStatusCode(statusCode).toString());
		}
		
		serverError.setMessage(message);
		serverError.setLocale(Locale.ROOT);
		
		return serverError;
	}
}
