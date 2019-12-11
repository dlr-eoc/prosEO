/**
 * UIMessages.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.backend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message IDs and message strings for the prosEO User Interface
 * 
 * @author Dr. Thomas Bassler
 */
public class UIMessages {

	/* Message IDs (publicly accessible) */
	public static final int MSG_ID_INVALID_COMMAND_NAME = 2930;
	public static final int MSG_ID_SUBCOMMAND_MISSING = 2931;
	public static final int MSG_ID_USER_NOT_LOGGED_IN = 2932;
	public static final int MSG_ID_NOT_AUTHORIZED = 2933;
	public static final int MSG_ID_OPERATION_CANCELLED = 2934;
	public static final int MSG_ID_NO_PROCESSORCLASSES_FOUND = 2980;
	public static final int MSG_ID_PROCESSORCLASS_CREATED = 2981;
	public static final int MSG_ID_NO_PROCCLASS_IDENTIFIER_GIVEN = 2983;
	public static final int MSG_ID_PROCESSORCLASS_NOT_FOUND = 2984;
	public static final int MSG_ID_PROCESSORCLASS_UPDATED = 2985;
	public static final int MSG_ID_PROCESSORCLASS_DELETED = 2986;
	public static final int MSG_ID_INVALID_CRITICALITY_LEVEL = 2987;
	public static final int MSG_ID_PROCESSOR_CREATED = 2988;
	public static final int MSG_ID_NO_PROCESSORS_FOUND = 2989;
	public static final int MSG_ID_NO_PROCESSOR_IDENTIFIER_GIVEN = 2970;
	public static final int MSG_ID_PROCESSOR_NOT_FOUND = 2971;
	public static final int MSG_ID_PROCESSOR_NOT_FOUND_BY_ID = 2972;
	public static final int MSG_ID_PROCESSOR_UPDATED = 2973;
	public static final int MSG_ID_PROCESSORCLASS_NOT_FOUND_BY_ID = 2974;
	public static final int MSG_ID_PROCESSOR_DELETED = 2975;
	public static final int MSG_ID_PROCESSORCLASS_DATA_INVALID = 2976;
	public static final int MSG_ID_PROCESSOR_DATA_INVALID = 2977;
	public static final int MSG_ID_PROCESSOR_DELETE_FAILED = 2978;
	public static final int MSG_ID_EXCEPTION = 2979;
	public static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	private static Map<Integer, String> uiMessages = new HashMap<>();
	
	
	/* Message strings (private, may be moved to database or localized properties file some day) */
	private enum UIMessage {
		MSG_INVALID_COMMAND_NAME ("(E%d) Invalid command name %s", MSG_ID_INVALID_COMMAND_NAME),
		MSG_SUBCOMMAND_MISSING ("(E%d) Subcommand missing for command %s", MSG_ID_SUBCOMMAND_MISSING),
		MSG_USER_NOT_LOGGED_IN ("(E%d) User not logged in", MSG_ID_USER_NOT_LOGGED_IN),
		MSG_NOT_AUTHORIZED ("(E%d) User %s not authorized to manage orders for mission %s", MSG_ID_NOT_AUTHORIZED),
		MSG_EXCEPTION ("(E%d) Command failed (cause: %s)", MSG_ID_EXCEPTION),
		MSG_NO_PROCESSORCLASSES_FOUND ("(E%d) No processor classes found for given search criteria", MSG_ID_NO_PROCESSORCLASSES_FOUND),
		MSG_NO_PROCCLASS_IDENTIFIER_GIVEN ("(E%d) No processor class name given", MSG_ID_NO_PROCCLASS_IDENTIFIER_GIVEN),
		MSG_PROCESSORCLASS_NOT_FOUND ("(E%d) Processor class %s not found", MSG_ID_PROCESSORCLASS_NOT_FOUND),
		MSG_PROCESSORCLASS_NOT_FOUND_BY_ID ("(E%d) Processor class with database ID %d not found", MSG_ID_PROCESSORCLASS_NOT_FOUND_BY_ID),
		MSG_PROCESSORCLASS_DATA_INVALID ("(E%d) Processor class data invalid (cause: %s)", MSG_ID_PROCESSORCLASS_DATA_INVALID),
		MSG_INVALID_CRITICALITY_LEVEL ("(E%d) Invalid criticality level %s (expected integer > 1)", MSG_ID_INVALID_CRITICALITY_LEVEL),
		MSG_NO_PROCESSORS_FOUND ("(E%d) No processors found for given search criteria", MSG_ID_NO_PROCESSORS_FOUND),
		MSG_NO_PROCESSOR_IDENTIFIER_GIVEN ("(E%d) No processor name and/or version given", MSG_ID_NO_PROCESSOR_IDENTIFIER_GIVEN),
		MSG_PROCESSOR_NOT_FOUND ("(E%d) Processor %s with version %s not found", MSG_ID_PROCESSOR_NOT_FOUND),
		MSG_PROCESSOR_NOT_FOUND_BY_ID ("(E%d) Processor with database ID %d not found", MSG_ID_PROCESSOR_NOT_FOUND_BY_ID),
		MSG_PROCESSOR_DATA_INVALID ("(E%d) Processor data invalid (cause: %s)", MSG_ID_PROCESSOR_DATA_INVALID),
		MSG_PROCESSOR_DELETE_FAILED ("(E%d) Deletion of processor %s with version %s failed (cause: %s)", MSG_ID_PROCESSOR_DELETE_FAILED),
		MSG_NOT_IMPLEMENTED ("(E%d) Command %s not implemented", MSG_ID_NOT_IMPLEMENTED),

		MSG_OPERATION_CANCELLED ("(I%d) Operation cancelled", MSG_ID_OPERATION_CANCELLED),
		MSG_PROCESSORCLASS_CREATED ("(I%d) Processor class %s created (database ID %d)", MSG_ID_PROCESSORCLASS_CREATED),
		MSG_PROCESSORCLASS_UPDATED ("(I%d) Processor class with database ID %d updated (new version %d)", MSG_ID_PROCESSORCLASS_UPDATED),
		MSG_PROCESSORCLASS_DELETED ("(I%d) Processor class with database ID %d deleted", MSG_ID_PROCESSORCLASS_DELETED),
		MSG_PROCESSOR_CREATED ("(I%d) Processor %s with version %s created (database ID %d)", MSG_ID_PROCESSOR_CREATED),
		MSG_PROCESSOR_UPDATED ("(I%d) Processor with database ID %d updated (new version %d)", MSG_ID_PROCESSOR_UPDATED),
		MSG_PROCESSOR_DELETED ("(I%d) Processor with database ID %d deleted", MSG_ID_PROCESSOR_DELETED);
		
		private final String msgText;
		private final int msgId;
		
		UIMessage(String text, int id) {
			this.msgText = text;
			this.msgId = id;
		}
	};
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(UIMessages.class);

	/*
	 * Static initializer to fill message map from enum values
	 * (might read from a properties file or from a database later on)
	 */
	static
	{
		if (logger.isTraceEnabled()) logger.trace(">>> UIMessages::<init>");

		for (UIMessage msg: UIMessage.values()) {
			uiMessages.put(msg.msgId, msg.msgText);
		}
		
		if (logger.isTraceEnabled()) logger.trace("... number of messages found: " + uiMessages.size());
	}
	
	
	/**
	 * Retrieve a message string by message ID (as a template for String.format())
	 */
	public static String uiMsg(int messageId, Object... messageParameters) {
		if (logger.isTraceEnabled()) logger.trace(">>> uiMsg({}, {})", messageId, messageParameters);

		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Format the message
		return String.format(uiMessages.get(messageId), messageParamList.toArray());
	}
}
