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
	 * Logging for levels info, warn, and error with automatic formatting including
	 * level, code, and message.
	 * 
	 * @param type          The enum specifying the type of the message.
	 * @param msgParameters The message's parameters.
	 * @return Returns the logged message.
	 */
	public String log(ProseoMessage type, Object... msgParameters) {
		if (type == null) {
			throw new IllegalArgumentException("Please specify the type of the message.");
		}
		String logged = "";
		switch (type.getLevel()) {
		case INFO:
			if (logger.isInfoEnabled()) {
				logged = MessageFormat.format("(I" + type.getCode() + ") " + type.getMessage(), msgParameters);
				logger.info(logged);
			}
			break;
		case WARN:
			if (logger.isWarnEnabled()) {
				logged = MessageFormat.format("(W" + type.getCode() + ") " + type.getMessage(), msgParameters);
				logger.warn(logged);
			}
			break;
		case ERROR:
			if (logger.isErrorEnabled()) {
				logged = MessageFormat.format("(E" + type.getCode() + ") " + type.getMessage(), msgParameters);
				logger.error(logged);
				
			}
			break;
		case DEBUG:
			if (logger.isDebugEnabled()) {
				logged = MessageFormat.format("(D" + type.getCode() + ") " + type.getMessage(), msgParameters);
				logger.debug(logged);
			}
			break;
		case TRACE:
			if (logger.isTraceEnabled()) {
				logged = MessageFormat.format("(T" + type.getCode() + ") " + type.getMessage(), msgParameters);
				logger.trace(logged);
			}
			break;
		}
		return logged;
	}

	/**
	 * Logging logic provided by Logback.
	 * 
	 * @param msg
	 */
	public void debug(String msg) {
		logger.debug(msg);
	}

	/**
	 * Logging logic provided by Logback.
	 * 
	 * @param format
	 * @param arguments
	 */
	public void debug(String format, Object... arguments) {
		logger.debug(format, arguments);
	}

	/**
	 * Logging logic provided by Logback.
	 * 
	 * @param msg
	 * @param t
	 */
	public void debug(String msg, Throwable t) {
		logger.debug(msg, t);
	}

	/**
	 * Logging logic provided by Logback.
	 * 
	 * @param msg
	 */
	public void trace(String msg) {
		logger.trace(msg);
	}

	/**
	 * Logging logic provided by Logback.
	 * 
	 * @param format
	 * @param arguments
	 */
	public void trace(String format, Object... arguments) {
		logger.trace(format, arguments);
	}

	/**
	 * Logging logic provided by Logback.
	 * 
	 * @param msg
	 * @param t
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
