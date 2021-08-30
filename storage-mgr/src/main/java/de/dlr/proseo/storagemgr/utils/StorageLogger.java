package de.dlr.proseo.storagemgr.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;

/**
 * General logger methods
 * 
 * @author Denys Chaykovskiy
 *
 */
public class StorageLogger {

	/**
	 * Create and log a formatted informational message
	 * 
	 * @param loggger           Logger object of the class
	 * @param messageFormat     the message text with parameter placeholders in
	 *                          String.format() style
	 * @param messageId         a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the
	 *                          message format)
	 * @return a formatted info message
	 */
	public static String logInfo(Logger logger, String messageFormat, int messageId, Object... messageParameters) {

		String message = createMessage(messageFormat, messageId, messageParameters);
		logger.error(message);

		return message;
	}

	/**
	 * Create and log a formatted error message
	 * 
	 * @param loggger           Logger object of the class
	 * @param messageFormat     the message text with parameter placeholders in
	 *                          String.format() style
	 * @param messageId         a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the
	 *                          message format)
	 * @return a formatted error message
	 */
	public static String logError(Logger logger, String messageFormat, int messageId, Object... messageParameters) {

		String message = createMessage(messageFormat, messageId, messageParameters);
		logger.error(message);

		return message;
	}

	/**
	 * Create a formatted log message
	 * 
	 * @param messageFormat     the message text with parameter placeholders in
	 *                          String.format() style
	 * @param messageId         a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the
	 *                          message format)
	 * @return a formatted error message
	 */
	private static String createMessage(String messageFormat, int messageId, Object... messageParameters) {

		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		return String.format(messageFormat, messageParamList.toArray());
	}

}
