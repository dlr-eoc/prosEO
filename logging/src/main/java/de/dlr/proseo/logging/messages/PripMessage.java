/**
 * PripMessage.java
 * 
 * (C) 35 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the prip.
 *
 * @author Katharina Bassler
 */
public enum PripMessage implements ProseoMessage {

	MSG_INVALID_ENTITY_TYPE 		(5001, Level.ERROR, false, "Invalid entity type {0} referenced in service request", ""),
	MSG_URI_GENERATION_FAILED 		(5002, Level.ERROR, false, "URI generation from product UUID failed (cause: {0})", ""),
	MSG_HTTP_REQUEST_FAILED 		(5003, Level.ERROR, false, "HTTP request failed (cause: {0})", ""),
	MSG_SERVICE_REQUEST_FAILED 		(5004, Level.ERROR, false, "Service request failed with status {0} ({1}), cause: {2}", ""),
	MSG_NOT_AUTHORIZED_FOR_SERVICE 	(5005, Level.ERROR, false, "User {0} not authorized for requested service", ""),
	MSG_PRODUCT_NOT_FOUND 			(5006, Level.ERROR, false, "No product found with UUID {0}", ""),
	MSG_NOT_AUTHORIZED_FOR_PRODUCT 	(5007, Level.ERROR, false, "User {0} not authorized to access requested product {1}", ""),
	MSG_EXCEPTION 					(5008, Level.ERROR, false, "Request failed (cause {0}: {1})", ""),
	MSG_FORBIDDEN 					(5009, Level.ERROR, false, "Creation, update and deletion of products not allowed through PRIP", ""),
	MSG_PRODUCT_NOT_AVAILABLE 		(5010, Level.ERROR, false, "Product {0} not available on any Processing Facility", ""),
	MSG_UNSUPPORTED_FORMAT 			(5011, Level.ERROR, false, "Unsupported response format {0}", ""),
	MSG_INVALID_RANGE_HEADER 		(5012, Level.WARN, 	true, "Ignoring invalid HTTP range header {0}", ""),
	MSG_REDIRECT 					(5013, Level.INFO, 	true, "Redirecting download request to Storage Manger URL {0}", ""),
	MSG_TOKEN_CREATED 				(5014, Level.INFO, 	true, "OAuth2 token created for user {0}", ""),
	MSG_TOKEN_INVALID 				(5015, Level.ERROR, false, "Authentication token {0} invalid (cause: {1})", ""),
	MSG_TOKEN_EXPIRED 				(5016, Level.ERROR, false, "Authentication token expired at {0}", ""),
	MSG_GRANT_TYPE_INVALID 			(5017, Level.ERROR, false, "Invalid grant type {0}", ""),
	MSG_USERNAME_INVALID 			(5018, Level.ERROR, false, "Invalid username parameter {0}", ""),
	MSG_CREDENTIAL_MISMATCH 		(5019, Level.ERROR, false, "Username and password do not match Authorization header", ""),
	MSG_SUPERFLUOUS_PARAMETERS 		(5020, Level.ERROR, false, "Superfluous query parameter(s) found (no credentials allowed for 'client_credentials' flow)", ""),
	MSG_INVALID_QUERY_CONDITION 	(5021, Level.ERROR, false, "Invalid query condition (cause: {0})", ""),
	MSG_INVALID_QUERY_RESULT 		(5022, Level.ERROR, false, "Invalid result for 'count(*)' query: {0}", ""),
	MSG_QUOTA_EXCEEDED 				(5023, Level.ERROR, false, "Result set exceeds maximum quota of {0} products", ""),
	MSG_USER_LOGGED_IN 				(5024, Level.INFO, false, "User {0}\\{1} logged in to PRIP API", ""),
	MSG_AUTH_MISSING_OR_INVALID 	(5025, Level.ERROR, false, "Basic authentication missing or invalid: {0}", ""),
	MSG_NOT_AUTHORIZED_FOR_PRIP 	(5026, Level.ERROR, false, "User {0}\\{1} not authorized for PRIP API", ""),
	MSG_INVALID_OPERAND_TYPE	 	(5027, Level.ERROR, false, "Both operands for binary operator must be AttributeCondition objects in Attribute lambda expression", ""),
	MSG_INVALID_OPERATOR_TYPE	 	(5028, Level.ERROR, false, "Only binary operator 'eq' allowed for 'Name' in Attribute lambda expression", ""),
	MSG_MISSING_OPERAND_NAME	 	(5029, Level.ERROR, false, "One operand for binary operator must be named in Attribute lambda expression ('Name' and 'Value' allowed)", ""),
	MSG_UNEXPECTED_URI			 	(5030, Level.ERROR, false, "Unexpected URI resource of kind {0} in Attribute lambda expression (only lambda variable allowed)", ""),
	MSG_UNEXPECTED_URI_VAR		 	(5031, Level.ERROR, false, "Lambda variable {0} not allowed in Attribute lambda expression for {1}", ""),
	MSG_UNEXPECTED_SUB_URI			(5032, Level.ERROR, false, "Unexpected URI sub-resource of kind {0} in Attribute lambda expression (only primitive property allowed)", ""),
	MSG_UNEXPECTED_PROPERTY		 	(5033, Level.ERROR, false, "Unexpected property {0} in Attribute lambda expression (only 'Name' and 'Value' allowed)", ""),
	MSG_CANNOT_CONVERT_COORD 		(5034, Level.WARN, 	true, "Cannot convert coordinate string '{}' to footprint", ""),
	MSG_EXCEPTION_SET_RESP			(5035, Level.ERROR, false, "Exception setting response content (cause {0}: {1})", ""),
	MSG_EXCEPTION_PIS				(5036, Level.ERROR, false, "Server Error occurred in ProductionInterfaceSecurity (cause {0}: {1})", ""),
	MSG_EXCEPTION_PQC				(5037, Level.ERROR, false, "Server Error occurred in ProductQueryController (cause {0}: {1})", ""),
	
	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private PripMessage(int code, Level level, boolean success, String message, String description) {
		this.code = code;
		this.level = level;
		this.success = success;
		this.message = message;
		this.description = description;
	}

	/**
	 * Get the message's code.
	 * 
	 * @return The message code.
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get a more detailed description of the message's purpose.
	 * 
	 * @return A description of the message.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get the message's level.
	 * 
	 * @return The message level.
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * Get the message.
	 * 
	 * @return The message.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Get the message's success.
	 * 
	 * @return The message's success.
	 */
	public boolean getSuccess() {
		return success;
	}

}
