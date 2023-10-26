/**
 * OdipMessage.java
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
public enum OdipMessage implements ProseoMessage {

	MSG_INVALID_ENTITY_TYPE 				(7201, Level.ERROR, false, "Invalid entity type {0} referenced in service request", ""),
	MSG_URI_GENERATION_FAILED 				(7202, Level.ERROR, false, "URI generation from product UUID failed (cause: {0})", ""),
	MSG_HTTP_REQUEST_FAILED 				(7203, Level.ERROR, false, "HTTP request failed (cause: {0})", ""),
	MSG_SERVICE_REQUEST_FAILED 				(7204, Level.ERROR, false, "Service request failed with status {0} ({1}), cause: {2}", ""),
	MSG_NOT_AUTHORIZED_FOR_SERVICE 			(7205, Level.ERROR, false, "User {0} not authorized for requested service", ""),
	MSG_PRODUCTIONORDER_NOT_FOUND			(7206, Level.ERROR, false, "No production order found with UUID {0}", ""),
	MSG_NOT_AUTHORIZED_FOR_PRODUCTIONORDER 	(7207, Level.ERROR, false, "User {0} not authorized to access requested production order {1}", ""),
	MSG_EXCEPTION 							(7208, Level.ERROR, false, "Request failed (cause {0}: {1})", ""),
	MSG_FORBIDDEN 							(7209, Level.ERROR, false, "Creation, update and deletion of products not allowed through PRIP", ""),
	MSG_PRODUCT_NOT_AVAILABLE 				(7210, Level.ERROR, false, "Product {0} not available on any Processing Facility", ""),
	MSG_UNSUPPORTED_FORMAT 					(7211, Level.ERROR, false, "Unsupported response format {0}", ""),
	MSG_INVALID_RANGE_HEADER 				(7212, Level.WARN, 	true, "Ignoring invalid HTTP range header {0}", ""),
	MSG_REDIRECT 							(7213, Level.INFO, 	true, "Redirecting download request to Storage Manger URL {0}", ""),
	MSG_TOKEN_CREATED 						(7214, Level.INFO, 	true, "OAuth2 token created for user {0}", ""),
	MSG_TOKEN_INVALID 						(7215, Level.ERROR, false, "Authentication token {0} invalid (cause: {1})", ""),
	MSG_TOKEN_EXPIRED 						(7216, Level.ERROR, false, "Authentication token expired at {0}", ""),
	MSG_GRANT_TYPE_INVALID 					(7217, Level.ERROR, false, "Invalid grant type {0}", ""),
	MSG_USERNAME_INVALID 					(7218, Level.ERROR, false, "Invalid username parameter {0}", ""),
	MSG_CREDENTIAL_MISMATCH 				(7219, Level.ERROR, false, "Username and password do not match Authorization header", ""),
	MSG_SUPERFLUOUS_PARAMETERS 				(7220, Level.ERROR, false, "Superfluous query parameter(s) found (no credentials allowed for ''client_credentials'' flow)", ""),
	MSG_INVALID_QUERY_CONDITION 			(7221, Level.ERROR, false, "Invalid query condition (cause: {0})", ""),
	MSG_INVALID_QUERY_RESULT 				(7222, Level.ERROR, false, "Invalid result for ''count(*)'' query: {0}", ""),
	MSG_QUOTA_EXCEEDED 						(7223, Level.ERROR, false, "Result set exceeds maximum quota of {0} products", ""),
	MSG_USER_LOGGED_IN 						(7224, Level.INFO, false, "User {0}\\{1} logged in to PRIP API", ""),
	MSG_AUTH_MISSING_OR_INVALID 			(7225, Level.ERROR, false, "Basic authentication missing or invalid: {0}", ""),
	MSG_NOT_AUTHORIZED_FOR_PRIP 			(7226, Level.ERROR, false, "User {0}\\{1} not authorized for PRIP API", ""),
	MSG_INVALID_OPERAND_TYPE	 			(7227, Level.ERROR, false, "Both operands for binary operator must be AttributeCondition objects in Attribute lambda expression", ""),
	MSG_INVALID_OPERATOR_TYPE	 			(7228, Level.ERROR, false, "Only binary operator ''eq'' allowed for ''Name'' in Attribute lambda expression", ""),
	MSG_MISSING_OPERAND_NAME	 			(7229, Level.ERROR, false, "One operand for binary operator must be named in Attribute lambda expression (''Name'' and ''Value'' allowed)", ""),
	MSG_UNEXPECTED_URI			 			(7230, Level.ERROR, false, "Unexpected URI resource of kind {0} in Attribute lambda expression (only lambda variable allowed)", ""),
	MSG_UNEXPECTED_URI_VAR		 			(7231, Level.ERROR, false, "Lambda variable {0} not allowed in Attribute lambda expression for {1}", ""),
	MSG_UNEXPECTED_SUB_URI					(7232, Level.ERROR, false, "Unexpected URI sub-resource of kind {0} in Attribute lambda expression (only primitive property allowed)", ""),
	MSG_UNEXPECTED_PROPERTY		 			(7233, Level.ERROR, false, "Unexpected property {0} in Attribute lambda expression (only ''Name'' and ''Value'' allowed)", ""),
	MSG_CANNOT_CONVERT_COORD 				(7234, Level.WARN, 	true, "Cannot convert coordinate string ''{}'' to footprint", ""),
	MSG_EXCEPTION_SET_RESP					(7235, Level.ERROR, false, "Exception setting response content (cause {0}: {1})", ""),
	MSG_EXCEPTION_PIS						(7236, Level.ERROR, false, "Server Error occurred in ProductionInterfaceSecurity (cause {0}: {1})", ""),
	MSG_EXCEPTION_PQC						(7237, Level.ERROR, false, "Server Error occurred in ProductQueryController (cause {0}: {1})", ""),
	MSG_WORKFLOW_REFERENCE_MISSING  		(7238, Level.ERROR, false, "Workflow UUID and name missing", ""),
	MSG_WORKFLOW_REF_NOT_FOUND  			(7239, Level.ERROR, false, "Workflow referenced by (UUID/name) {0}/{1} not found", ""),
	MSG_WORKFLOW_OPTION_NOT_DEF  			(7240, Level.ERROR, false, "Workflow option {0} not  defined in workflow {1}", ""),
	MSG_WORKFLOW_OPTION_NO_TYPE_MATCH 		(7241, Level.ERROR, false, "Workflow option {0} does not match type: {1}, value: {2}", ""),
	MSG_WORKFLOW_OPTION_VALUE_NOT_IN_RANGE 	(7242, Level.ERROR, false, "Workflow option {0}: value not in value range {1}", ""),
	EXCEPTION								(7243, Level.ERROR, false, "Command failed (cause: {0})", ""),
	EXTRACTED_MESSAGE						(7244, Level.ERROR, false, "Extraced message: {0}", ""),
	HTTP_REQUEST_FAILED						(7245, Level.ERROR, false, "HTTP request failed (cause: {0})", ""),
	INVALID_URL								(7246, Level.ERROR, false, "Invalid request URL {0} (cause: {1})", ""),
	NOT_AUTHORIZED							(7247, Level.ERROR, false, "User {0} not authorized to manage {1} for mission {2}", ""),
	NOT_AUTHORIZED_FOR_SERVICE				(7248, Level.ERROR, false, "User {0} not authorized for requested service", ""),
	NOT_MODIFIED							(7249, Level.INFO, true, "Data not modified", ""),
	ORBIT_DATA_INVALID						(7250, Level.ERROR, false, "Orbit data invalid (cause: {0})", ""),
	ORDER_DATA_INVALID						(7251, Level.ERROR, false, "Order data invalid (cause: {0})", ""),
	SERIALIZATION_FAILED					(7252, Level.ERROR, false, "Cannot convert object to Json (cause: {0})", ""),
	WARN_UNEXPECTED_STATUS					(7253, Level.ERROR, false, "Unexpected HTTP status {0} received", ""),
	MSG_STARTSTOP_MISSING					(7254, Level.ERROR, false, "Sensing start/stop time missing", ""),
	MSG_INPUTREF_INVALID					(7255, Level.ERROR, false, "Invalid input reference", ""),
	MSG_PRODUCTCLASS_NOT_DEF				(7256, Level.ERROR, false, "Product class {0} not defined for mission {1}", ""),
	MSG_PARAMETER_NOT_FOUND					(7257, Level.ERROR, false, "Parameter not found: {0}", ""),
	MSG_INPUTREF_NOT_FOUND					(7258, Level.ERROR, false, "No product file named {0} found on any archive", ""),
	MSG_NO_INPUTPRODUCT						(7259, Level.ERROR, false, "No product of type {0} found on any archive", ""),
	MSG_WORKFLOW_NOT_FOUND					(7260, Level.ERROR, false, "No workflow found with UUID {0}", ""),
	MSG_NOT_AUTHORIZED_FOR_WORKFLOW 		(7261, Level.ERROR, false, "User {0} not authorized to access requested production order {1}", ""),
	MSG_JSON_PARSE_ERROR					(7262, Level.ERROR, false, "JSON parse error: {0}", ""),
	MSG_PRODUCTIONORDERPRODUCT_NOT_FOUND	(7263, Level.ERROR, false, "No product found for production order with UUID {0}", ""),
	MSG_PRODUCTIONORDERWORKFLOW_NOT_FOUND	(7264, Level.ERROR, false, "No workflow found for production order with UUID {0}", ""),
	MSG_PRODUCTIONORDERPRODUCT_NO_FILES 	(7265, Level.ERROR, false, "Requested product of production order with UUID {0} has no files", ""),
	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private OdipMessage(int code, Level level, boolean success, String message, String description) {
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
	 * Get the message''s success.
	 * 
	 * @return The message's success.
	 */
	public boolean getSuccess() {
		return success;
	}

}
