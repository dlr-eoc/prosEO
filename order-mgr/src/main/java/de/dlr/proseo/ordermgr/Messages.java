package de.dlr.proseo.ordermgr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;

/**
 * Messages, codes and states
 * 
 * @author Ernst Melchinger
 *
 */
public enum Messages {
	TRUE							(true, MessageType.I, "true"),
	FALSE							(false, MessageType.E, "false"),
	OK								(true, MessageType.I, "Okay"),
	UNDEFINED					(false, MessageType.E, "Undefined"),
	HTTP_HEADER_WARNING				(true, MessageType.W, "Warning"),
	HTTP_HEADER_SUCCESS				(true, MessageType.I, "Success"),
	MSG_PREFIX						(true, MessageType.I, "199 order-mgr "), 
	JOBCOUNT_RETRIEVED				(true, MessageType.I, "Job count for processing order '%s' retrieved"), 
	JOBINDEX_RETRIEVED				(true, MessageType.I, "Job index for processing order '%s' retrieved"), 
	JOB_RETRIED						(true, MessageType.I, "Job '%s' set to inital"), 
	JOB_NOT_EXIST					(false, MessageType.E, "Job '%s' does not exist"),
	JOBSTEP_NOT_EXIST				(false, MessageType.E, "Job step '%s' does not exist"),
	JOBSTEP_RETRIEVED				(true, MessageType.I, "Job step '%s' retrieved"), 
	JOBSTEPS_RETRIEVED				(true, MessageType.I, "Job steps of status %s retrieved for mission %s"), 
	JOBS_FOR_ORDER_NOT_EXIST		(false, MessageType.E, "Job(s) for processing order '%s' do not exist"),
	JOBS_RETRIEVED					(true, MessageType.I, "Jobs for processing order '%s' retrieved"), 
	// Same as in other services
	ILLEGAL_CROSS_MISSION_ACCESS 	(false, MessageType.E, "Illegal cross-mission access to mission %s (logged in to %s)"),
	RUNTIME_EXCEPTION				(false, MessageType.E, "Exception encountered: %s"),

	DUMMY							(true, MessageType.I, "(%d)")
	;
	
	private static final int CODE_START = 1065;
	
	public enum MessageType {
		I, // Information
		W, // Warning
		E; // Error
	}

	
	/**
	 * The message code
	 */
	private final int code;
	
	/**
	 * The message type
	 */
	private final MessageType type;
	
	/**
	 * The message description 
	 */
	private final String description;
	
	/**
	 * The message success, true or false
	 */
	private final boolean success;
	
	/**
	 * The next message code.
	 * At the moment the code is generated as a sequence starting at 3000 
	 */
	private static int nextCode = CODE_START;

	/**
	 * Get the next code number, used to generate codes automatically
	 * 
	 * @return code
	 */
	private static int getNextCode() {
		if (nextCode == 0) {
			nextCode = CODE_START;
		}
		return ++nextCode; 
	}
	

	/**
	 * @return the type
	 */
	public MessageType getType() {
		return type;
	}


	/**
	 * Get the message with corresponding code.
	 * 
	 * @param code of message to retrieve
	 * @return message 
	 */
	public static Messages getValueOfCode(int code) {
		int index = code - CODE_START;
		if (index < 0 || index >= Messages.values().length) {
			return Messages.UNDEFINED;
		} else {
			return Messages.values()[code - CODE_START];
		}
	}
	
	/**
	 * Create Messages element with success flag and description.
	 * 
	 * @param success true if positive, else false
	 * @param description message string
	 */
	private Messages(boolean success, MessageType type, String description) {
		this.code = Messages.getNextCode();
		this.type = type;
		this.description = description;
		this.success = success;
	}

	/**
	 * Get the success state of the message.
	 * 
	 * @return success state
	 */
	public boolean isTrue() {
		return success;
	}
	
	/**
	 * Get the message string.
	 * 
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Format the message, use description and code.
	 * 
	 * @return Formatted string
	 */
	public String format(Object... messageParameters) {
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, code);
		messageParamList.add(0, type.toString());

		return String.format("(%s%d) " + description, messageParamList.toArray());
	}
	public String log(Logger logger, Object... messageParameters) {
		return logPrim(logger, this.format(messageParameters));
	}

	/**
	 * Format the message, use prefix (MSG_PREFIX) description and code.
	 * 
	 * @return Formatted string
	 */
	public String formatWithPrefix(Object... messageParameters) {
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, code);
		messageParamList.add(0, type.toString());
		return Messages.MSG_PREFIX.getDescription() + String.format("(%s%d) " + description, messageParamList.toArray());
	}
	public String logWithPrefix(Logger logger, Object... messageParameters) {
		return logPrim(logger, this.formatWithPrefix(messageParameters));
	}

	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	public static HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING.getDescription(),
				MSG_PREFIX.getDescription() + (null == message ? "null" : message.replaceAll("\n", " ")));
		return responseHeaders;
	}
	
	/**
	 * Get the message code.
	 * 
	 * @return code
	 */
	public int getCode() {
		return code;
	}

	private String logPrim(Logger logger, String msg) {
		if (logger != null && msg != null) {
			switch (this.type) {
			case E:
				logger.error(msg);
				break;
			case W:
				logger.warn(msg);
				break;
			case I:
			default:
				logger.info(msg);
				break;
			}
			return msg;
		} else {
			return "";
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return description;
	}
}
