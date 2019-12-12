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
	// General
	public static final int MSG_ID_INVALID_COMMAND_NAME = 2800;
	public static final int MSG_ID_SUBCOMMAND_MISSING = 2801;
	public static final int MSG_ID_USER_NOT_LOGGED_IN = 2802;
	public static final int MSG_ID_NOT_AUTHORIZED = 2803;
	public static final int MSG_ID_OPERATION_CANCELLED = 2804;
	public static final int MSG_ID_INVALID_TIME = 2805;
	public static final int MSG_ID_EXCEPTION = 2806;
	public static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	// Service connection
	public static final int MSG_ID_HTTP_REQUEST_FAILED = 2810;
	public static final int MSG_ID_SERVICE_REQUEST_FAILED = 2811;
	public static final int MSG_ID_NOT_AUTHORIZED_FOR_SERVICE = 2812;
	public static final int MSG_ID_SERIALIZATION_FAILED = 2813;
	public static final int MSG_ID_INVALID_URL = 2814;
	public static final int MSG_ID_UNEXPECTED_STATUS = 2815;
	
	// User Manager
	public static final int MSG_ID_HTTP_CONNECTION_FAILURE = 2820;
	public static final int MSG_ID_LOGGED_IN = 2821;
	public static final int MSG_ID_LOGIN_FAILED = 2822;
	public static final int MSG_ID_LOGGED_OUT = 2823;
	public static final int MSG_ID_LOGIN_CANCELLED = 2824;
	public static final int MSG_ID_MISSION_NOT_FOUND = 2825;
	public static final int MSG_ID_NOT_AUTHORIZED_FOR_MISSION = 2826;
	
	// CLIUtil
	public static final int MSG_ID_INVALID_FILE_TYPE = 2830;
	public static final int MSG_ID_INVALID_FILE_STRUCTURE = 2831;
	public static final int MSG_ID_INVALID_FILE_SYNTAX = 2832;
	public static final int MSG_ID_INVALID_ATTRIBUTE_NAME = 2833;
	public static final int MSG_ID_INVALID_ATTRIBUTE_TYPE = 2834;
	public static final int MSG_ID_REFLECTION_EXCEPTION = 2835;

	// CLI Parser
	public static final int MSG_ID_SYNTAX_LOADED = 2900;
	public static final int MSG_ID_ILLEGAL_PARAMETER_TYPE = 2901;
	public static final int MSG_ID_ILLEGAL_OPTION_TYPE = 2902;
	public static final int MSG_ID_INVALID_COMMAND_OPTION = 2910;
	public static final int MSG_ID_OPTION_NOT_ALLOWED = 2911;
	public static final int MSG_ID_ILLEGAL_OPTION = 2912;
	public static final int MSG_ID_ILLEGAL_OPTION_VALUE = 2913;
	public static final int MSG_ID_TOO_MANY_PARAMETERS = 2914;
	public static final int MSG_ID_ATTRIBUTE_PARAMETER_EXPECTED = 2915;
	public static final int MSG_ID_ILLEGAL_COMMAND = 2916;
	public static final int MSG_ID_ILLEGAL_SUBCOMMAND = 2917;
	public static final int MSG_ID_PARAMETER_MISSING = 2918;
	
	// CLI Main
	public static final int MSG_ID_SYNTAX_FILE_NOT_FOUND = 2920;
	public static final int MSG_ID_SYNTAX_FILE_ERROR = 2921;
	public static final int MSG_ID_COMMAND_LINE_PROMPT_SUPPRESSED = 2922;
	public static final int MSG_ID_COMMAND_NAME_NULL = 2923;
	
	// Order CLI
	public static final int MSG_ID_NO_ORDERS_FOUND = 2930;
	public static final int MSG_ID_INVALID_SLICING_TYPE = 2931;
	public static final int MSG_ID_INVALID_SLICE_DURATION = 2932;
	public static final int MSG_ID_INVALID_ORBIT_NUMBER = 2933;
	public static final int MSG_ID_ORDER_CREATED = 2934;
	public static final int MSG_ID_ORDER_NOT_FOUND = 2935;
	public static final int MSG_ID_NO_IDENTIFIER_GIVEN = 2936;
	public static final int MSG_ID_INVALID_ORDER_STATE = 2937;
	public static final int MSG_ID_ORDER_UPDATED = 2938;
	public static final int MSG_ID_ORDER_DELETED = 2939;
	public static final int MSG_ID_ORDER_DATA_INVALID = 2940;

	// Ingestor/product CLI
	public static final int MSG_ID_NO_PRODUCTS_FOUND = 2950;
	public static final int MSG_ID_PRODUCT_CREATED = 2951;
	public static final int MSG_ID_INVALID_DATABASE_ID = 2952;
	public static final int MSG_ID_NO_PRODUCT_DBID_GIVEN = 2953;
	public static final int MSG_ID_PRODUCT_NOT_FOUND = 2954;
	public static final int MSG_ID_PRODUCT_UPDATED = 2955;
	public static final int MSG_ID_PRODUCT_DELETED = 2956;
	public static final int MSG_ID_INGESTION_FILE_MISSING = 2957;
	public static final int MSG_ID_PROCESSING_FACILITY_MISSING = 2958;
	public static final int MSG_ID_PRODUCTS_INGESTED = 2959;
	public static final int MSG_ID_PRODUCT_DATA_INVALID = 2960;
	
	// Processor CLI
	public static final int MSG_ID_NO_PROCESSORCLASSES_FOUND = 2970;
	public static final int MSG_ID_PROCESSORCLASS_CREATED = 2971;
	public static final int MSG_ID_NO_PROCCLASS_IDENTIFIER_GIVEN = 2973;
	public static final int MSG_ID_PROCESSORCLASS_NOT_FOUND = 2974;
	public static final int MSG_ID_PROCESSORCLASS_UPDATED = 2975;
	public static final int MSG_ID_PROCESSORCLASS_DELETED = 2976;
	public static final int MSG_ID_INVALID_CRITICALITY_LEVEL = 2977;
	public static final int MSG_ID_PROCESSOR_CREATED = 2978;
	public static final int MSG_ID_NO_PROCESSORS_FOUND = 2979;
	public static final int MSG_ID_NO_PROCESSOR_IDENTIFIER_GIVEN = 2980;
	public static final int MSG_ID_PROCESSOR_NOT_FOUND = 2981;
	public static final int MSG_ID_PROCESSOR_NOT_FOUND_BY_ID = 2982;
	public static final int MSG_ID_PROCESSOR_UPDATED = 2983;
	public static final int MSG_ID_PROCESSORCLASS_NOT_FOUND_BY_ID = 2984;
	public static final int MSG_ID_PROCESSOR_DELETED = 2985;
	public static final int MSG_ID_PROCESSORCLASS_DATA_INVALID = 2986;
	public static final int MSG_ID_PROCESSOR_DATA_INVALID = 2987;
	public static final int MSG_ID_PROCESSOR_DELETE_FAILED = 2988;
	public static final int MSG_ID_PROCESSORCLASS_DELETE_FAILED = 2989;

	private static Map<Integer, String> uiMessages = new HashMap<>();
	
	
	/* Message strings (private, may be moved to database or localized properties file some day) */
	private enum UIMessage {
		/* --- Error messages --- */
		// General
		MSG_INVALID_COMMAND_NAME ("(E%d) Invalid command name %s", MSG_ID_INVALID_COMMAND_NAME),
		MSG_SUBCOMMAND_MISSING ("(E%d) Subcommand missing for command %s", MSG_ID_SUBCOMMAND_MISSING),
		MSG_USER_NOT_LOGGED_IN ("(E%d) User not logged in", MSG_ID_USER_NOT_LOGGED_IN),
		MSG_NOT_AUTHORIZED ("(E%d) User %s not authorized to manage %s for mission %s", MSG_ID_NOT_AUTHORIZED),
		MSG_EXCEPTION ("(E%d) Command failed (cause: %s)", MSG_ID_EXCEPTION),
		MSG_INVALID_TIME ("(E%d) Time format %s not parseable", MSG_ID_INVALID_TIME),
		MSG_NOT_IMPLEMENTED ("(E%d) Command %s not implemented", MSG_ID_NOT_IMPLEMENTED),

		// User Manager
		MSG_HTTP_CONNECTION_FAILURE ("(E%d) HTTP connection failure (cause: %s)", MSG_ID_HTTP_CONNECTION_FAILURE),
		MSG_MISSION_NOT_FOUND ("(E%d) Mission %s not found", MSG_ID_MISSION_NOT_FOUND),
		MSG_LOGIN_FAILED ("(E%d) Login for user %s failed", MSG_ID_LOGIN_FAILED),
		MSG_NOT_AUTHORIZED_FOR_MISSION ("(E%d) User %s not authorized for mission %s", MSG_ID_NOT_AUTHORIZED_FOR_MISSION),
		
		// Service connection
		MSG_HTTP_REQUEST_FAILED ("(E%d) HTTP request failed (cause: %s)", MSG_ID_HTTP_REQUEST_FAILED),
		MSG_SERVICE_REQUEST_FAILED ("(E%d) Service request failed with status %d (%s), cause: %s", MSG_ID_SERVICE_REQUEST_FAILED),
		MSG_NOT_AUTHORIZED_FOR_SERVICE ("(E%d) User %s not authorized for requested service", MSG_ID_NOT_AUTHORIZED_FOR_SERVICE),
		MSG_SERIALIZATION_FAILED ("(E%d) Cannot convert object to Json (cause: %s)", MSG_ID_SERIALIZATION_FAILED),
		MSG_INVALID_URL ("(E%d) Invalid request URL %s (cause: %s)", MSG_ID_INVALID_URL),
		MSG_UNEXPECTED_STATUS ("(E%d) Unexpected HTTP status %s received", MSG_ID_UNEXPECTED_STATUS),
		
		// Order CLI
		MSG_NO_ORDERS_FOUND ("(E%d) No orders found for given search criteria", MSG_ID_NO_ORDERS_FOUND),
		MSG_INVALID_SLICING_TYPE ("(E%d) Invalid order slicing type %s", MSG_ID_INVALID_SLICING_TYPE),
		MSG_INVALID_SLICE_DURATION ("(E%d) Slice duration %s not numeric", MSG_ID_INVALID_SLICE_DURATION),
		MSG_INVALID_ORBIT_NUMBER ("(E%d) Orbit number %s not numeric", MSG_ID_INVALID_ORBIT_NUMBER),
		MSG_ORDER_NOT_FOUND ("(E%d) Order with identifier %s not found", MSG_ID_ORDER_NOT_FOUND),
		MSG_INVALID_ORDER_STATE ("(E%d) Operation %s not allowed for order state %s (must be %s)", MSG_ID_INVALID_ORDER_STATE),
		MSG_NO_IDENTIFIER_GIVEN ("(E%d) No order identifier or database ID given", MSG_ID_NO_IDENTIFIER_GIVEN),
		MSG_ORDER_DATA_INVALID ("(E%d) Order data invalid (cause: %s)", MSG_ID_ORDER_DATA_INVALID),
		
		// Ingestor/product CLI
		MSG_NO_PRODUCTS_FOUND ("(E%d) No products found for given search criteria", MSG_ID_NO_PRODUCTS_FOUND),
		MSG_INVALID_DATABASE_ID ("(E%d) Database ID %s not numeric", MSG_ID_INVALID_DATABASE_ID),
		MSG_NO_PRODUCT_DBID_GIVEN ("(E%d) No product database ID given", MSG_ID_NO_IDENTIFIER_GIVEN),
		MSG_PRODUCT_NOT_FOUND ("(E%d) Product with database ID %d not found", MSG_ID_PRODUCT_NOT_FOUND),
		MSG_INGESTION_FILE_MISSING ("(E%d) No file for product ingestion given", MSG_ID_INGESTION_FILE_MISSING),
		MSG_PROCESSING_FACILITY_MISSING ("(E%d) No processing facility to ingest to given", MSG_ID_PROCESSING_FACILITY_MISSING),
		MSG_PRODUCT_DATA_INVALID ("(E%d) Product data invalid (cause: %s)", MSG_ID_PRODUCT_DATA_INVALID),
		
		// Processor CLI
		MSG_NO_PROCESSORCLASSES_FOUND ("(E%d) No processor classes found for given search criteria", MSG_ID_NO_PROCESSORCLASSES_FOUND),
		MSG_NO_PROCCLASS_IDENTIFIER_GIVEN ("(E%d) No processor class name given", MSG_ID_NO_PROCCLASS_IDENTIFIER_GIVEN),
		MSG_PROCESSORCLASS_NOT_FOUND ("(E%d) Processor class %s not found", MSG_ID_PROCESSORCLASS_NOT_FOUND),
		MSG_PROCESSORCLASS_NOT_FOUND_BY_ID ("(E%d) Processor class with database ID %d not found", MSG_ID_PROCESSORCLASS_NOT_FOUND_BY_ID),
		MSG_PROCESSORCLASS_DATA_INVALID ("(E%d) Processor class data invalid (cause: %s)", MSG_ID_PROCESSORCLASS_DATA_INVALID),
		MSG_PROCESSORCLASS_DELETE_FAILED ("(E%d) Deletion of processor class %s failed (cause: %s)", MSG_ID_PROCESSORCLASS_DELETE_FAILED),
		MSG_INVALID_CRITICALITY_LEVEL ("(E%d) Invalid criticality level %s (expected integer > 1)", MSG_ID_INVALID_CRITICALITY_LEVEL),
		MSG_NO_PROCESSORS_FOUND ("(E%d) No processors found for given search criteria", MSG_ID_NO_PROCESSORS_FOUND),
		MSG_NO_PROCESSOR_IDENTIFIER_GIVEN ("(E%d) No processor name and/or version given", MSG_ID_NO_PROCESSOR_IDENTIFIER_GIVEN),
		MSG_PROCESSOR_NOT_FOUND ("(E%d) Processor %s with version %s not found", MSG_ID_PROCESSOR_NOT_FOUND),
		MSG_PROCESSOR_NOT_FOUND_BY_ID ("(E%d) Processor with database ID %d not found", MSG_ID_PROCESSOR_NOT_FOUND_BY_ID),
		MSG_PROCESSOR_DATA_INVALID ("(E%d) Processor data invalid (cause: %s)", MSG_ID_PROCESSOR_DATA_INVALID),
		MSG_PROCESSOR_DELETE_FAILED ("(E%d) Deletion of processor %s with version %s failed (cause: %s)", MSG_ID_PROCESSOR_DELETE_FAILED),

		// CLIUtil
		MSG_INVALID_FILE_TYPE ("(E%d) Invalid order file type %s", MSG_ID_INVALID_FILE_TYPE),
		MSG_INVALID_FILE_STRUCTURE ("(E%d) %s content of order file %s invalid for order generation (cause: %s)", MSG_ID_INVALID_FILE_STRUCTURE),
		MSG_INVALID_FILE_SYNTAX ("(E%d) Order file %s contains invalid %s content (cause: %s)", MSG_ID_INVALID_FILE_SYNTAX),
		MSG_INVALID_ATTRIBUTE_NAME ("(E%d) Invalid attribute name %s", MSG_ID_INVALID_ATTRIBUTE_NAME),
		MSG_INVALID_ATTRIBUTE_TYPE ("(E%d) Attribute %s cannot be converted to type %s", MSG_ID_INVALID_ATTRIBUTE_TYPE),
		MSG_REFLECTION_EXCEPTION ("(E%d) Reflection exception setting attribute %s (cause: %s)", MSG_ID_REFLECTION_EXCEPTION),
		
		// CLI Parser
		MSG_ILLEGAL_PARAMETER_TYPE ("(E%d) Illegal parameter type %s, expected one of %s", MSG_ID_ILLEGAL_PARAMETER_TYPE),
		MSG_ILLEGAL_OPTION_TYPE ("(E%d) Illegal option type %s, expected one of %s", MSG_ID_ILLEGAL_OPTION_TYPE),
		MSG_INVALID_COMMAND_OPTION ("(E%d) Invalid command option %s found", MSG_ID_INVALID_COMMAND_OPTION),
		MSG_OPTION_NOT_ALLOWED ("(E%d) Option %s not allowed after command parameter", MSG_ID_OPTION_NOT_ALLOWED),
		MSG_ILLEGAL_OPTION ("(E%d) Option %s not allowed for command %s", MSG_ID_ILLEGAL_OPTION),
		MSG_ILLEGAL_OPTION_VALUE ("(E%d) Illegal option value %s for option %s of type %s", MSG_ID_ILLEGAL_OPTION_VALUE),
		MSG_TOO_MANY_PARAMETERS ("(E%d) Too many parameters for command %s", MSG_ID_TOO_MANY_PARAMETERS),
		MSG_ATTRIBUTE_PARAMETER_EXPECTED ("(E%d) Parameter of format '<attribute name>=<attribute value>' expected at position %d for command %s", MSG_ID_ATTRIBUTE_PARAMETER_EXPECTED),
		MSG_ILLEGAL_COMMAND ("(E%d) Illegal command %s", MSG_ID_ILLEGAL_COMMAND),
		MSG_ILLEGAL_SUBCOMMAND ("(E%d) Illegal subcommand %s", MSG_ID_ILLEGAL_SUBCOMMAND),
		MSG_PARAMETER_MISSING ("(E%d) Required parameter %s not found for command %s", MSG_ID_PARAMETER_MISSING),
		
		// CLI Main
		MSG_SYNTAX_FILE_NOT_FOUND ("(E%d) Syntax file %s not found", MSG_ID_SYNTAX_FILE_NOT_FOUND),
		MSG_SYNTAX_FILE_ERROR ("(E%d) Parsing error in syntax file %s (cause: %s)", MSG_ID_SYNTAX_FILE_ERROR),
		MSG_COMMAND_LINE_PROMPT_SUPPRESSED ("(I%d) Command line prompt suppressed by proseo.cli.start parameter", MSG_ID_COMMAND_LINE_PROMPT_SUPPRESSED),
		MSG_COMMAND_NAME_NULL ("(E%d) Command name must not be null", MSG_ID_COMMAND_NAME_NULL),
		
		/* --- Info messages -- */
		// General
		MSG_OPERATION_CANCELLED ("(I%d) Operation cancelled", MSG_ID_OPERATION_CANCELLED),
		
		// User Manager
		MSG_LOGGED_IN ("(I%d) User %s logged in", MSG_ID_LOGGED_IN),
		MSG_LOGGED_OUT ("(I%d) User %s logged out", MSG_ID_LOGGED_OUT),
		MSG_LOGIN_CANCELLED ("(I%d) No username given, login cancelled", MSG_ID_LOGIN_CANCELLED),
		
		// CLI Parser
		MSG_SYNTAX_LOADED ("(I%d) Command line syntax loaded from syntax file %s", MSG_ID_SYNTAX_LOADED),
		
		// Order CLI
		MSG_ORDER_CREATED ("(I%d) Order with identifier %s created (database ID %d)", MSG_ID_ORDER_CREATED),
		MSG_ORDER_UPDATED ("(I%d) Order with identifier %s updated (new version %d)", MSG_ID_ORDER_UPDATED),
		MSG_ORDER_DELETED ("(I%d) Order with identifier %s deleted", MSG_ID_ORDER_DELETED),
		
		// Ingestor/product CLI
		MSG_PRODUCT_CREATED ("(I%d) Product of class %s created (database ID %d, UUID %s)", MSG_ID_PRODUCT_CREATED),
		MSG_PRODUCT_UPDATED ("(I%d) Product with database ID %d updated (new version %d)", MSG_ID_PRODUCT_UPDATED),
		MSG_PRODUCT_DELETED ("(I%d) Product with database ID %d deleted", MSG_ID_PRODUCT_DELETED),
		MSG_PRODUCTS_INGESTED ("(I%d) %d products ingested to processing facility %s", MSG_ID_PRODUCTS_INGESTED),
		
		// Processor CLI
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
