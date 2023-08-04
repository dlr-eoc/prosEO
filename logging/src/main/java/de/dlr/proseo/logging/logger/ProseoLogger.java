/**
 * ProseoLogger.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.logger;

import java.text.MessageFormat;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import de.dlr.proseo.logging.messages.ProseoMessage;

/**
 * A centralized logging mechanism for ProsEO.
 *
 * @author Katharina Bassler
 */
public final class ProseoLogger {

	private final Logger logger;

	/**
	 * Generates a new logger named as the fully qualified class name.
	 *
	 * @param clazz The fully qualified class name.
	 */
	public ProseoLogger(Class<?> clazz) {
		this.logger = (Logger) LoggerFactory.getLogger(clazz);
	}

	/**
	 * Logging with automatic formatting including level, code, and message.
	 *
	 * @param type          The enum specifying the type of the message.
	 * @param msgParameters The message's parameters.
	 * @return Returns the logged message.
	 */
	public String log(ProseoMessage type, Object... msgParameters) {
		if (type == null) {
			throw new IllegalArgumentException("Please specify the type of the message.");
		}

		String logged = format(type, msgParameters);

		switch (type.getLevel()) {
		case INFO:
			if (logger.isInfoEnabled())
				logger.info(logged);
			break;
		case WARN:
			if (logger.isWarnEnabled())
				logger.warn(logged);
			break;
		case ERROR:
			if (logger.isErrorEnabled())
				logger.error(logged);
			break;
		case DEBUG:
			if (logger.isDebugEnabled())
				logger.debug(logged);
			break;
		case TRACE:
			if (logger.isTraceEnabled())
				logger.trace(logged);
			break;
		}
		return logged;
	}

	/**
	 * Automatic formatting including level, code, and message.
	 *
	 * @param type          The enum specifying the type of the message.
	 * @param msgParameters The message's parameters.
	 * @return Returns the logged message.
	 */
	public static String format(ProseoMessage type, Object... msgParameters) {
		if (type == null) {
			throw new IllegalArgumentException("Please specify the type of the message.");
		}

		String formatted = "";

		try {

			switch (type.getLevel()) {
			case INFO:
				formatted = MessageFormat.format("(I" + type.getCode() + ") " + type.getMessage(), msgParameters);
				break;
			case WARN:
				formatted = MessageFormat.format("(W" + type.getCode() + ") " + type.getMessage(), msgParameters);
				break;
			case ERROR:
				formatted = MessageFormat.format("(E" + type.getCode() + ") " + type.getMessage(), msgParameters);
				break;
			case DEBUG:
				formatted = MessageFormat.format("(D" + type.getCode() + ") " + type.getMessage(), msgParameters);
				break;
			case TRACE:
				formatted = MessageFormat.format("(T" + type.getCode() + ") " + type.getMessage(), msgParameters);
				break;
			}

		} catch (Exception e) {
			Logger errorLogger = (Logger) LoggerFactory.getLogger(ProseoLogger.class);
			errorLogger.error("A message format error occured with message type " + type);
			return "A message format error occured with message type " + type;
		}

		return formatted;
	}

	/**
	 * Logging logic provided by Logback.
	 *
	 * @param msg the message to log
	 */
	public void debug(String msg) {
		logger.debug(msg);
	}

	/**
	 * Logging logic provided by Logback.
	 *
	 * @param format    the format string
	 * @param arguments the arguments to insert
	 */
	public void debug(String format, Object... arguments) {
		logger.debug(format, arguments);
	}

	/**
	 * Logging logic provided by Logback.
	 *
	 * @param msg the message to log
	 * @param t   the exception (throwable) to log
	 */
	public void debug(String msg, Throwable t) {
		logger.debug(msg, t);
	}

	/**
	 * Logging logic provided by Logback.
	 *
	 * @param msg the message to log
	 */
	public void trace(String msg) {
		logger.trace(msg);
	}

	/**
	 * Logging logic provided by Logback.
	 *
	 * @param format    the format string
	 * @param arguments the arguments to insert
	 */
	public void trace(String format, Object... arguments) {
		logger.trace(format, arguments);
	}

	/**
	 * Logging logic provided by Logback.
	 *
	 * @param msg the message to log
	 * @param t   the exception (throwable) to log
	 */
	public void trace(String msg, Throwable t) {
		logger.trace(msg, t);
	}

	/**
	 * Returns the logger's name.
	 *
	 * @return The logger's name as a String.
	 */
	public String getName() {
		return logger.getName();
	}

	/**
	 * Indicates whether the logger is trace enabled.
	 *
	 * @return The logging level.
	 */
	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	/**
	 * Indicates whether the logger is debug enabled.
	 *
	 * @return The logging level.
	 */
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	/**
	 * Indicates whether the logger is info enabled.
	 *
	 * @return The logging level.
	 */
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	/**
	 * Indicates whether the logger is warn enabled.
	 *
	 * @return The logging level.
	 */
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	/**
	 * Indicates whether the logger is error enabled.
	 *
	 * @return The logging level.
	 */
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

}
