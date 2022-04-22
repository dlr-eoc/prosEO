package de.dlr.proseo.storagemgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;


public class Msg {

	// C:\Base\projects\git\prosEO\planner\src\main\java\de\dlr\proseo\planner\Messages.java
	// UIMessages.java

	// throw new ParseException(uiMsg(MSG_ID_TOO_MANY_PARAMETERS,
	// syntaxCommand.getName()), 0);
	
	public static final String WARNING = "Warning"; 
	public static final String MSG_PREFIX = "199 storage-manager "; 

	public static final int INVALID_COMMAND_NAME = 2800;
		
	private enum SMMessage {
		/* --- Error messages --- */
		// General
		MSG_INVALID_COMMAND_NAME ("(E%d) Invalid command name %s", INVALID_COMMAND_NAME);
		private final String msgText;
		private final int msgId;
		
		SMMessage(String text, int id) {
			this.msgText = text;
			this.msgId = id;
		}
	};
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(Msg.class);
	
	private static Map<Integer, String> smMessages = new HashMap<>();
	
	/*
	 * Static initializer to fill message map from enum values
	 * (might read from a properties file or from a database later on)
	 */
	static
	{
		if (logger.isTraceEnabled()) logger.trace(">>> UIMessages::<init>");

		for (SMMessage msg: SMMessage.values()) {
			smMessages.put(msg.msgId, msg.msgText);
		}
		
		if (logger.isTraceEnabled()) logger.trace("... number of messages found: " + smMessages.size());
	}

	public static String str(int messageId, Object... messageParameters) {

		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Format the message
		return String.format(smMessages.get(messageId), messageParamList.toArray());
	}
	
	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	public static HttpHeaders errorHeaders(String message) {
		
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(WARNING, MSG_PREFIX + message.replaceAll("\n", " "));
		return responseHeaders;
	}
}
