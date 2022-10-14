/**
 * UIMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the UIs.
 *
 * @author Katharina Bassler
 */
public enum UIMessage implements ProseoMessage {

	ALREADY_MEMBER							(6000, Level.ERROR, false, "User {0} is already a member of group {1}", ""),
	ATTRIBUTE_PARAMETER_EXPECTED			(6001, Level.ERROR, false, "Parameter of format '<attribute name>=<attribute value>' expected at position {0} for command {1}", ""),
	AUTHORITIES_GRANTED						(6002, Level.INFO, true, "Authorities {0} granted to user {1}", ""),
	AUTHORITIES_REVOKED						(6003, Level.INFO, true, "Authorities {0} revoked from user {1}", ""),
	CLI_NOT_AUTHORIZED						(6004, Level.ERROR, false, "User {0} not authorized for Command Line Interface", ""),
	CLI_TERMINATED							(6005, Level.INFO, true, "'exit' command received, prosEO Command Line Interface terminates", ""),
	COMMAND_LINE_PROMPT_SUPPRESSED			(6006, Level.ERROR, false, "Command line prompt suppressed by proseo.cli.start parameter", ""),
	COMMAND_NAME_NULL						(6007, Level.ERROR, false, "Command name must not be null", ""),
	COMMAND_NOT_IMPLEMENTED					(6008, Level.ERROR, false, "Command {0} not implemented", ""),
	CONFIGURATION_CREATED					(6009, Level.INFO, true, "Configuration {0} with version {0} created (database ID {1})", ""),
	CONFIGURATION_DATA_INVALID				(6010, Level.ERROR, false, "Configuration data invalid (cause: {0})", ""),
	CONFIGURATION_DELETE_FAILED				(6011, Level.ERROR, false, "Deletion of processor {0} with version {1} failed (cause: {2})", ""),
	CONFIGURATION_DELETED					(6012, Level.INFO, true, "Configuration with database ID {0} deleted", ""),
	CONFIGURATION_NOT_FOUND					(6013, Level.ERROR, false, "Configuration for processor {0} with configuration version {1} not found", ""),
	CONFIGURATION_NOT_FOUND_BY_ID			(6014, Level.ERROR, false, "Configuration with database ID {0} not found", ""),
	CONFIGURATION_UPDATED					(6015, Level.INFO, true, "Configuration with database ID {0} updated (new version {1})", ""),
	CONFIGUREDPROCESSOR_CREATED				(6016, Level.INFO, true, "Configured processor {0} for processor {1}, version {2} and configuration version {3} created (database ID {4})", ""),
	CONFIGUREDPROCESSOR_DATA_INVALID		(6017, Level.ERROR, false, "Configuration data invalid (cause: {0})", ""),
	CONFIGUREDPROCESSOR_DELETE_FAILED		(6018, Level.ERROR, false, "Deletion of configured processor {0} failed (cause: {1})", ""),
	CONFIGUREDPROCESSOR_DELETED				(6019, Level.INFO, true, "Configured processor with database ID {0} deleted", ""),
	CONFIGUREDPROCESSOR_NOT_FOUND			(6020, Level.ERROR, false, "Configured processor {0} not found", ""),
	CONFIGUREDPROCESSOR_NOT_FOUND_BY_ID		(6021, Level.ERROR, false, "Configured processor with database ID {0} not found", ""),
	CONFIGUREDPROCESSOR_UPDATED				(6022, Level.INFO, true, "Configured processor with database ID {0} updated (new version {1})", ""),
	CREDENTIALS_INSECURE					(6023, Level.ERROR, false, "Credential file {0} ignored, because it is insecure (group or world readable)", ""),
	CREDENTIALS_NOT_FOUND					(6024, Level.ERROR, false, "Credential file {0} not found", ""),
	CREDENTIALS_NOT_READABLE				(6025, Level.ERROR, false, "Credential file {0} not readable (cause: {1})", ""),
	DELETE_PRODUCTS_WITHOUT_FORCE			(6026, Level.ERROR, false, "Option 'delete-products' not valid without option 'force'", ""),
	END_OF_FILE								(6027, Level.INFO, true, "End of input reached, prosEO Command Line Interface terminates", ""),
	EXCEPTION								(6028, Level.ERROR, false, "Command failed (cause: {0})", ""),
	EXTRACTED_MESSAGE						(6029, Level.ERROR, false, "Extraced message: {0}", ""),
	FACILITY_CREATED						(6030, Level.INFO, true, "Processing facility {0} created (database ID {1})", ""),
	FACILITY_DATA_INVALID					(6031, Level.ERROR, false, "Processing facility data invalid (cause: {0})", ""),
	FACILITY_DELETE_FAILED					(6032, Level.ERROR, false, "Deletion of processing facility {0} failed (cause: {1})", ""),
	FACILITY_DELETED						(6033, Level.INFO, true, "Processing facility with database ID {0} deleted", ""),
	FACILITY_MISSING						(6034, Level.ERROR, false, "Processing facility missing in parameters", ""),
	FACILITY_NOT_FOUND						(6035, Level.ERROR, false, "Processing facility {0} not found", ""),
	FACILITY_NOT_FOUND_BY_ID				(6036, Level.ERROR, false, "Processing facility with database ID {0} not found", ""),
	FACILITY_NOT_READABLE					(6037, Level.ERROR, false, "Processing facility {0} not readable (cause: {1})", ""),
	FACILITY_UPDATED						(6038, Level.INFO, true, "Processing facility with database ID {0} updated (new version {1})", ""),
	FILE_NOT_FOUND							(6039, Level.ERROR, false, "Selection rule file {0} not found or not readable", ""),
	GENERATION_EXCEPTION					(6040, Level.ERROR, false, "Write exception serializing object {0} to format {1} (cause: {2})", ""),
	GROUP_AUTHORITIES_GRANTED				(6041, Level.INFO, true, "Authorities {0} granted to group {1}", ""),
	GROUP_AUTHORITIES_REVOKED				(6042, Level.INFO, true, "Authorities {0} revoked from group {1}", ""),
	GROUP_CREATED							(6043, Level.INFO, true, "User group {0} created", ""),
	GROUP_DATA_INVALID						(6044, Level.ERROR, false, "User group data invalid (cause: {0})", ""),
	GROUP_DELETE_FAILED						(6045, Level.ERROR, false, "Deletion of user group {0} failed (cause: {1})", ""),
	GROUP_DELETED							(6046, Level.INFO, true, "User group {0} deleted", ""),
	GROUP_NOT_FOUND_BY_ID					(6047, Level.ERROR, false, "Group with database ID {0} not found", ""),
	GROUP_NOT_FOUND_BY_NAME					(6048, Level.ERROR, false, "User group {0} not found for mission {1}", ""),
	GROUP_UPDATED							(6049, Level.INFO, true, "User group {0} updated", ""),
	HTTP_CONNECTION_FAILURE					(6050, Level.ERROR, false, "HTTP connection failure (cause: {0})", ""),
	HTTP_REQUEST_FAILED						(6051, Level.ERROR, false, "HTTP request failed (cause: {0})", ""),
	ILLEGAL_COMMAND							(6052, Level.ERROR, false, "Illegal command {0}", ""),
	ILLEGAL_OPTION							(6053, Level.ERROR, false, "Option {0} not allowed for command {1}", ""),
	ILLEGAL_OPTION_TYPE						(6054, Level.ERROR, false, "Illegal option type {0}, expected one of {2}", ""),
	ILLEGAL_OPTION_VALUE					(6055, Level.ERROR, false, "Illegal option value {0} for option {1} of type {2}", ""),
	ILLEGAL_PARAMETER_TYPE					(6056, Level.ERROR, false, "Illegal parameter type {0}, expected one of {1}", ""),
	ILLEGAL_SUBCOMMAND						(6057, Level.ERROR, false, "Illegal subcommand {0}", ""),
	INGESTION_FILE_MISSING					(6058, Level.ERROR, false, "No file for product ingestion given", ""),
	INPUT_NOT_NUMERIC						(6059, Level.ERROR, false, "Input {0} not numeric", ""),
	INPUT_OUT_OF_BOUNDS						(6060, Level.ERROR, false, "Input {0} invalid, please select a number between {0} and {1}", ""),
	INSUFFICIENT_CREDENTIALS				(6061, Level.ERROR, false, "Insufficient credentials given for non-interactive login", ""),
	INVALID_ATTRIBUTE_NAME					(6062, Level.ERROR, false, "Invalid attribute name {0}", ""),
	INVALID_ATTRIBUTE_TYPE					(6063, Level.ERROR, false, "Attribute {0} cannot be converted to type {1}", ""),
	INVALID_COMMAND_NAME					(6064, Level.ERROR, false, "Invalid command name {0}", ""),
	INVALID_COMMAND_OPTION					(6065, Level.ERROR, false, "Invalid command option {0} found", ""),
	INVALID_CRITICALITY_LEVEL				(6066, Level.ERROR, false, "Invalid criticality level {0} (expected integer > 1)", ""),
	INVALID_DATABASE_ID						(6067, Level.ERROR, false, "Database ID {0} not numeric", ""),
	INVALID_FILE_STRUCTURE					(6068, Level.ERROR, false, "{0} content of file {1} invalid for object generation (cause: {2})", ""),
	INVALID_FILE_SYNTAX						(6069, Level.ERROR, false, "File {0} contains invalid {1} content (cause: {2})", ""),
	INVALID_FILE_TYPE						(6070, Level.ERROR, false, "Invalid file format {0}", ""),
	INVALID_IDENT_FILE						(6071, Level.ERROR, false, "Credentials file {0} invalid (does not contain username and password)", ""),
	INVALID_JOB_STATE						(6072, Level.ERROR, false, "Operation {0} not allowed for job state {1} (must be {2})", ""),
	INVALID_JOB_STATE_VALUE					(6073, Level.ERROR, false, "Invalid job state {0}", ""),
	INVALID_JOBSTEP_STATE					(6074, Level.ERROR, false, "Operation {0} not allowed for job step state {1} (must be {2})", ""),
	INVALID_JOBSTEP_STATE_VALUE				(6075, Level.ERROR, false, "Invalid job step state {0}", ""),
	INVALID_ORBIT_NUMBER					(6076, Level.ERROR, false, "Orbit number {0} not numeric", ""),
	INVALID_ORDER_STATE						(6077, Level.ERROR, false, "Operation {0} not allowed for order state {1} (must be {2})", ""),
	INVALID_SLICE_DURATION					(6078, Level.ERROR, false, "Slice duration {0} not numeric", ""),
	INVALID_SLICING_TYPE					(6079, Level.ERROR, false, "Invalid order slicing type {0}", ""),
	INVALID_TIME							(6080, Level.ERROR, false, "Time format {0} not parseable", ""),
	INVALID_URL								(6081, Level.ERROR, false, "Invalid request URL {0} (cause: {1})", ""),
	INVALID_USERNAME						(6082, Level.ERROR, false, "Invalid Username (mission missing?): {0} {1} {2} {3} ...",""),
	INVALID_VISIBILITY						(6083, Level.ERROR, false, "Invalid product visibility {0}", ""),
	JOB_CANCELLED							(6084, Level.INFO, true, "Job with database ID {0} cancelled (new version {1})", ""),
	JOB_DATA_INVALID						(6085, Level.ERROR, false, "Job data invalid (cause: {0})", ""),
	JOB_NOT_FOUND							(6086, Level.ERROR, false, "Job with database ID {0} not found", ""),
	JOB_RESUMED								(6087, Level.INFO, true, "Job with database ID {0} resumed (new version {1})", ""),
	JOB_SUSPENDED							(6088, Level.INFO, true, "Job with database ID {0} suspended (new version {1})", ""),
	JOBSTEP_CANCELLED						(6089, Level.INFO, true, "Job step with database ID {0} cancelled (new version {1})", ""),
	JOBSTEP_DATA_INVALID					(6090, Level.ERROR, false, "Job step data invalid (cause: {0})", ""),
	JOBSTEP_NOT_FOUND						(6091, Level.ERROR, false, "Job step with database ID {0} not found", ""),
	JOBSTEP_RESUMED							(6092, Level.INFO, true, "Job step with database ID {0} resumed (new version {1})", ""),
	JOBSTEP_SUSPENDED						(6093, Level.INFO, true, "Job step with database ID {0} suspended (new version {1})", ""),
	LOGGED_IN								(6094, Level.INFO, true, "User {0} logged in", ""),
	LOGGED_IN_TO_MISSION					(6095, Level.ERROR, false, "Operation not allowed, when already logged in to a mission (currently logged in to {0})", ""),
	LOGGED_OUT								(6096, Level.INFO, true, "User {0} logged out", ""),
	LOGGING_IN								(6097, Level.INFO, true, "Logging in to prosEO with user {0}", ""),
	LOGIN_CANCELLED							(6098, Level.INFO, true, "No username given, login cancelled", ""),
	LOGIN_FAILED							(6099, Level.ERROR, false, "Login for user {0} failed", ""),
	LOGIN_WITHOUT_MISSION_FAILED			(6100, Level.ERROR, false, "User {0} not authorized to login without a mission", ""),
	MANDATORY_ATTRIBUTE_MISSING				(6101, Level.ERROR, false, "Mandatory attribute '{0}' missing", ""),
	MAPPING_EXCEPTION						(6102, Level.ERROR, false, "Exception mapping object {0} to format {1} (cause: {2})", ""),
	MISSION_ALREADY_SET						(6103, Level.ERROR, false, "Already logged in to mission {0}, use of '--mission' option not allowed", ""),
	MISSION_CREATED							(6104, Level.INFO, true, "Mission {0} created (database ID {1})", ""),
	MISSION_DATA_INVALID					(6105, Level.ERROR, false, "Mission data invalid (cause: {0})", ""),
	MISSION_DELETE_FAILED					(6106, Level.ERROR, false, "Deletion of mission {0} failed (cause: {1})", ""),
	MISSION_DELETED							(6107, Level.INFO, true, "Mission {0} deleted", ""),
	MISSION_NOT_FOUND						(6108, Level.ERROR, false, "Mission {0} not found", ""),
	MISSION_NOT_FOUND_BY_ID					(6109, Level.ERROR, false, "Mission with database ID {0} not found", ""),
	MISSION_NOT_READABLE					(6110, Level.ERROR, false, "Mission {0} not readable (cause: {1})", ""),
	MISSION_UPDATED							(6111, Level.INFO, true, "Mission {0} updated (new version {1})", ""),
	NO_AUTHORITIES_GIVEN					(6112, Level.ERROR, false, "No valid authorities given in command", ""),
	NO_CONFIGURATION_IDENTIFIER_GIVEN		(6113, Level.ERROR, false, "No processor name and/or configuration version given", ""),
	NO_CONFIGURATIONS_FOUND					(6114, Level.ERROR, false, "No configurations found for given search criteria", ""),
	NO_CONFIGUREDPROCESSOR_IDENTIFIER_GIVEN	(6115, Level.ERROR, false, "No configured processor identifier given", ""),
	NO_CONFIGUREDPROCESSORS_FOUND			(6116, Level.ERROR, false, "No processors found for given search criteria", ""),
	NO_FACILITIES_FOUND						(6117, Level.ERROR, false, "No processing facilities found", ""),
	NO_FACILITY_NAME_GIVEN					(6118, Level.ERROR, false, "No processing facility name given", ""),
	NO_FILECLASSES_FOUND					(6119, Level.ERROR, false, "No file classes found for given search criteria", ""),
	NO_GROUPNAME_GIVEN						(6120, Level.ERROR, false, "No group name given", ""),
	NO_GROUPS_FOUND							(6121, Level.ERROR, false, "No user groups found for mission {0}", ""),
	NO_IDENTIFIER_GIVEN						(6122, Level.ERROR, false, "No order identifier or database ID given", ""),
	NO_JOB_DBID_GIVEN						(6123, Level.ERROR, false, "No job database ID given", ""),
	NO_JOBS_FOUND							(6124, Level.ERROR, false, "No jobs found for given search criteria", ""),
	NO_JOBSTEP_DBID_GIVEN					(6125, Level.ERROR, false, "No job step database ID given", ""),
	NO_JOBSTEPS_FOUND						(6126, Level.ERROR, false, "No job steps found for given search criteria", ""),
	NO_MISSION_CODE_GIVEN					(6127, Level.ERROR, false, "Mission code missing", ""),
	NO_MISSIONS_FOUND						(6128, Level.ERROR, false, "No missions found for given search criteria", ""),
	NO_ORBIT_NUMBER_GIVEN					(6129, Level.ERROR, false, "No orbit number given", ""),
	NO_ORBITS_FOUND							(6130, Level.ERROR, false, "No orbits found for given search criteria", ""),
	NO_ORDERS_FOUND							(6131, Level.ERROR, false, "No orders found for given search criteria", ""),
	NO_PROCCLASS_IDENTIFIER_GIVEN			(6132, Level.ERROR, false, "No processor class name given", ""),
	NO_PROCESSINGMODES_FOUND				(6133, Level.ERROR, false, "No processing modes found for given search criteria", ""),
	NO_PROCESSOR_IDENTIFIER_GIVEN			(6134, Level.ERROR, false, "No processor name and/or version given", ""),
	NO_PROCESSORCLASSES_FOUND				(6135, Level.ERROR, false, "No processor classes found for given search criteria", ""),
	NO_PROCESSORS_FOUND						(6136, Level.ERROR, false, "No processors found for given search criteria", ""),
	NO_PRODCLASS_NAME_GIVEN					(6137, Level.ERROR, false, "No product class name given", ""),
	NO_PRODUCT_DBID_GIVEN					(6138, Level.ERROR, false, "No product database ID given", ""),
	NO_PRODUCTCLASSES_FOUND					(6139, Level.ERROR, false, "No product classes found for given search criteria", ""),
	NO_PRODUCTS_FOUND						(6140, Level.ERROR, false, "No products found for given search criteria", ""),
	NO_RULEID_GIVEN							(6141, Level.ERROR, false, "No selection rule database ID given", ""),
	NO_SELECTION_RULES_FOUND				(6142, Level.ERROR, false, "No selection rules found for product class {0}", ""),
	NO_SELECTION_RULES_FOUND_FOR_SOURCE		(6143, Level.ERROR, false, "No selection rules found for target product class {0} and source product class {2}", ""),
	NO_SPACECRAFT_CODE_GIVEN				(6145, Level.ERROR, false, "No spacecraft code given", ""),
	NO_SPACECRAFTS_FOUND					(6146, Level.ERROR, false, "No spacecrafts found for given search criteria", ""),
	NO_USERNAME_GIVEN						(6147, Level.ERROR, false, "No username given", ""),
	NO_USERS_FOUND							(6148, Level.ERROR, false, "No user accounts found for mission {0}", ""),
	NO_USERS_FOUND_IN_GROUP					(6149, Level.ERROR, false, "No user accounts found for user group {0}", ""),
	NO_USERS_GIVEN							(6150, Level.ERROR, false, "No users given in command", ""),
	NOT_AUTHORIZED							(6151, Level.ERROR, false, "User {0} not authorized to manage {1} for mission {2}", ""),
	NOT_AUTHORIZED_FOR_MISSION				(6152, Level.ERROR, false, "User {0} not authorized for mission {1}", ""),
	NOT_AUTHORIZED_FOR_SERVICE				(6153, Level.ERROR, false, "User {0} not authorized for requested service", ""),
	NOT_MEMBER								(6154, Level.ERROR, false, "User {0} is not a member of group {1}", ""),
	NOT_MODIFIED							(6155, Level.INFO, true, "Data not modified", ""),
	OPERATION_CANCELLED						(6156, Level.INFO, true, "Operation cancelled", ""),
	OPTION_NOT_ALLOWED						(6157, Level.ERROR, false, "Option {0} not allowed after command parameter", ""),
	ORBIT_DATA_INVALID						(6158, Level.ERROR, false, "Orbit data invalid (cause: {0})", ""),
	ORBIT_DELETE_FAILED						(6159, Level.ERROR, false, "Deletion of orbit {0} for spacecraft {1} failed (cause: {2})", ""),
	ORBIT_NOT_FOUND							(6160, Level.ERROR, false, "Orbit number {0} not found for spacecraft {1}", ""),
	ORBIT_NOT_FOUND_BY_ID					(6161, Level.ERROR, false, "Orbit with database ID {0} not found", ""),
	ORBIT_NUMBER_INVALID					(6162, Level.ERROR, false, "Orbit number {0} not numeric", ""),
	ORBITS_CREATED							(6163, Level.INFO, true, "{0} orbits created", ""),
	ORBITS_DELETED							(6164, Level.INFO, true, "{0} orbits deleted", ""),
	ORBITS_UPDATED							(6165, Level.INFO, true, "{0} orbits updated", ""),
	ORDER_APPROVED							(6166, Level.INFO, true, "Order with identifier {0} approved (new version {1})", ""),
	ORDER_CANCELLED							(6167, Level.INFO, true, "Order with identifier {0} cancelled (new version {1})", ""),
	ORDER_CREATED							(6168, Level.INFO, true, "Order with identifier {0} created (database ID {0})", ""),
	ORDER_DATA_INVALID						(6169, Level.ERROR, false, "Order data invalid (cause: {0})", ""),
	ORDER_DELETED							(6170, Level.INFO, true, "Order with identifier {0} deleted", ""),
	ORDER_JOBS_NOT_FOUND					(6171, Level.ERROR, false, "No jobs found for order with identifier {0}", ""),
	ORDER_NOT_FOUND							(6172, Level.ERROR, false, "Order with identifier {0} not found", ""),
	ORDER_PLANNED							(6173, Level.INFO, true, "Order with identifier {0} planned (new version {1})", ""),
	ORDER_RELEASED							(6174, Level.INFO, true, "Order with identifier {0} released (new version {1})", ""),
	ORDER_RELEASING							(6175, Level.INFO, true, "Order with identifier {0} released (new version {1})", ""),
	ORDER_RESET								(6176, Level.INFO, true, "Order with identifier {0} reset (new version {1})", ""),
	ORDER_SUSPENDED							(6177, Level.INFO, true, "Order with identifier {0} suspended (new version {1})", ""),
	ORDER_UPDATED							(6178, Level.INFO, true, "Order with identifier {0} updated (new version {1})", ""),
	PARAMETER_MISSING						(6179, Level.ERROR, false, "Required parameter {0} not found for command {1}", ""),
	PARAMETER_TYPE_MISMATCH					(6180, Level.ERROR, false, "Parameter of type {0} expected at position {1} for command {2}", ""),
	PASSWORD_CHANGE_NOT_ALLOWED				(6181, Level.ERROR, false, "Password change not allowed in non-interactive mode", ""),
	PASSWORD_CHANGED						(6182, Level.INFO, true, "Password changed for user {0}", ""),
	PASSWORD_MISMATCH						(6183, Level.ERROR, false, "Passwords do not match", ""),
	PASSWORD_MISSING						(6184, Level.ERROR, false, "No password given for user {0}", ""),
	PASSWORD_STRENGTH_INSUFFICIENT			(6185, Level.ERROR, false, "Password strength insufficient (min. length {0} characters, min. {1} types of the four element groups lowercase letters, uppercase letters, digits and special characters '{2}' required)", ""),
	PASSWORDS_MUST_DIFFER					(6186, Level.ERROR, false, "Old and new password must be different", ""),
	PROCESSING_FACILITY_MISSING				(6187, Level.ERROR, false, "No processing facility to ingest to given", ""),
	PROCESSOR_CREATED						(6188, Level.INFO, true, "Processor {0} with version {1} created (database ID {2})", ""),
	PROCESSOR_DATA_INVALID					(6189, Level.ERROR, false, "Processor data invalid (cause: {0})", ""),
	PROCESSOR_DELETE_FAILED					(6190, Level.ERROR, false, "Deletion of processor {0} with version {1} failed (cause: {2})", ""),
	PROCESSOR_DELETED						(6191, Level.INFO, true, "Processor with database ID {0} deleted", ""),
	PROCESSOR_NOT_FOUND						(6192, Level.ERROR, false, "Processor {0} with version {1} not found", ""),
	PROCESSOR_NOT_FOUND_BY_ID				(6193, Level.ERROR, false, "Processor with database ID {0} not found", ""),
	PROCESSOR_UPDATED						(6194, Level.INFO, true, "Processor with database ID {0} updated (new version {1})", ""),
	PROCESSORCLASS_CREATED					(6195, Level.INFO, true, "Processor class {0} created (database ID {1})", ""),
	PROCESSORCLASS_DATA_INVALID				(6196, Level.ERROR, false, "Processor class data invalid (cause: {0})", ""),
	PROCESSORCLASS_DELETE_FAILED			(6197, Level.ERROR, false, "Deletion of processor class {0} failed (cause: {1})", ""),
	PROCESSORCLASS_DELETED					(6198, Level.INFO, true, "Processor class with database ID {0} deleted", ""),
	PROCESSORCLASS_NOT_FOUND				(6199, Level.ERROR, false, "Processor class {0} not found", ""),
	PROCESSORCLASS_NOT_FOUND_BY_ID			(6200, Level.ERROR, false, "Processor class with database ID {0} not found", ""),
	PROCESSORCLASS_UPDATED					(6201, Level.INFO, true, "Processor class with database ID {0} updated (new version {1})", ""),
	PRODUCT_CLASS_MISMATCH					(6202, Level.ERROR, false, "Product with database ID {0} is not of requested class {2}", ""),
	PRODUCT_CREATED							(6203, Level.INFO, true, "Product of class {0} created (database ID {1}, UUID {2})", ""),
	PRODUCT_DATA_INVALID					(6204, Level.ERROR, false, "Product data invalid (cause: {0})", ""),
	PRODUCT_DELETED							(6205, Level.INFO, true, "Product with database ID {0} deleted", ""),
	PRODUCT_HAS_NO_FILES					(6206, Level.ERROR, false, "Product with database ID {0} has no files", ""),
	PRODUCT_ID_OR_FACILITY_MISSING			(6207, Level.ERROR, false, "Product database ID or processing facility missing", ""),
	PRODUCT_NOT_FOUND						(6208, Level.ERROR, false, "Product with database ID {0} not found", ""),
	PRODUCT_UPDATED							(6209, Level.INFO, true, "Product with database ID {0} updated (new version {1})", ""),
	PRODUCTCLASS_CREATED					(6210, Level.INFO, true, "Product class {0} created (database ID {1})", ""),
	PRODUCTCLASS_DATA_INVALID				(6211, Level.ERROR, false, "Product class data invalid (cause: {0})", ""),
	PRODUCTCLASS_DELETE_FAILED				(6212, Level.ERROR, false, "Deletion of product class {0} failed (cause: {1})", ""),
	PRODUCTCLASS_DELETED					(6213, Level.INFO, true, "Product class with database ID {0} deleted", ""),
	PRODUCTCLASS_NOT_FOUND					(6214, Level.ERROR, false, "Product class {0} not found", ""),
	PRODUCTCLASS_NOT_FOUND_BY_ID			(6215, Level.ERROR, false, "Product class with database ID {0} not found", ""),
	PRODUCTCLASS_UPDATED					(6216, Level.INFO, true, "Product class with database ID {0} updated (new version {1})", ""),
	PRODUCTFILE_DELETED						(6217, Level.INFO, true, "Product file for product database ID {0} and processing facility {2} deleted", ""),
	PRODUCTFILE_NOT_FOUND					(6218, Level.ERROR, false, "No product file found for product database ID {0} and processing facility {1}", ""),
	PRODUCTS_INGESTED						(6219, Level.INFO, true, "{0} products ingested to processing facility {1}", ""),
	REFLECTION_EXCEPTION					(6220, Level.ERROR, false, "Reflection exception setting attribute {0} (cause: {2})", ""),
	RETRYING_JOB							(6221, Level.INFO, true, "Retrying job with database ID {0} (new version {1})", ""),
	RETRYING_JOBSTEP						(6222, Level.INFO, true, "Retrying job step with database ID {0} (new version {1})", ""),
	RETRYING_ORDER							(6223, Level.INFO, true, "Retrying order with identifier {0} (new version {1})", ""),
	RULEID_NOT_NUMERIC						(6224, Level.ERROR, false, "Database ID {0} for selection rule not numeric", ""),
	SELECTION_RULE_DATA_INVALID				(6225, Level.ERROR, false, "Selection rule data invalid (cause: {0})", ""),
	SELECTION_RULE_DELETE_FAILED			(6226, Level.ERROR, false, "Deletion of selection rule with database ID {0} from product class {1} failed (cause: {2})", ""),
	SELECTION_RULE_DELETED					(6227, Level.INFO, true, "Selection rule with database ID {0} deleted", ""),
	SELECTION_RULE_NOT_FOUND_BY_ID			(6228, Level.ERROR, false, "Selection rule with database ID {0} not found", ""),
	SELECTION_RULE_UPDATED					(6229, Level.INFO, true, "Selection rule with database ID {0} updated (new version {1})", ""),
	SELECTION_RULES_CREATED					(6230, Level.INFO, true, "{0} selection rules created for product class {1}", ""),
	SERIALIZATION_FAILED					(6231, Level.ERROR, false, "Cannot convert object to Json (cause: {0})", ""),
	SERVICE_REQUEST_FAILED					(6232, Level.ERROR, false, "Service request failed with status {0} ({1}), cause: {2}", ""),
	SKIPPING_INVALID_AUTHORITY				(6233, Level.ERROR, false, "Skipping invalid authority {0}", ""),
	SPACECRAFT_ADDED						(6234, Level.INFO, true, "Spacecraft {0} added (database ID {1})", ""),
	SPACECRAFT_EXISTS						(6235, Level.ERROR, false, "Spacecraft {0} exists in mission {1}", ""),
	SPACECRAFT_NOT_FOUND					(6236, Level.ERROR, false, "Spacecraft {0} not found in mission {1}", ""),
	SPACECRAFT_REMOVED						(6237, Level.INFO, true, "Spacecraft {0} removed from mission {1}", ""),
	SUBCOMMAND_MISSING						(6238, Level.ERROR, false, "Subcommand missing for command {0}", ""),
	SYNTAX_FILE_ERROR						(6239, Level.ERROR, false, "Parsing error in syntax file {0} (cause: {1})", ""),
	SYNTAX_FILE_NOT_FOUND					(6240, Level.ERROR, false, "Syntax file {0} not found", ""),
	SYNTAX_LOADED							(6241, Level.INFO, true, "Command line syntax loaded from syntax file {0}", ""),
	TOO_MANY_PARAMETERS						(6242, Level.ERROR, false, "Too many parameters for command {0}", ""),
	UNCAUGHT_EXCEPTION						(6243, Level.ERROR, false, "prosEO Command Line Interface terminated by exception: {0}", ""),
	UNEXPECTED_STATUS						(6244, Level.ERROR, false, "Unexpected HTTP status {0} received", ""),
	UNKNOWN_AUTHENTICATION_TYPE				(6245, Level.ERROR, false, "Unknown authentication type: {0}", ""),
	USER_CREATED							(6246, Level.INFO, true, "User account {0} created", ""),
	USER_DATA_INVALID						(6247, Level.ERROR, false, "User account data invalid (cause: {0})", ""),
	USER_DELETE_FAILED						(6248, Level.ERROR, false, "Deletion of user account {0} failed (cause: {1})", ""),
	USER_DELETED							(6249, Level.INFO, true, "User account {0} deleted", ""),
	USER_DISABLED							(6250, Level.INFO, true, "User account {0} disabled", ""),
	USER_ENABLED							(6251, Level.INFO, true, "User account {0} enabled", ""),
	USER_INTERRUPT							(6252, Level.ERROR, false, "prosEO Command Line Interface exiting due to user interrupt", ""),
	USER_NOT_FOUND_BY_NAME					(6253, Level.ERROR, false, "User account {0} not found for mission {1}", ""),
	USER_NOT_LOGGED_IN						(6254, Level.ERROR, false, "User not logged in", ""),
	USER_NOT_LOGGED_IN_TO_MISSION			(6255, Level.ERROR, false, "User not logged in to any mission", ""),
	USER_UPDATED							(6256, Level.INFO, true, "User account {0} updated", ""),
	USERNAME_MISMATCH						(6257, Level.ERROR, false, "Username {0} to update does not match username {1} from credentials file {2}", ""),
	USERS_ADDED								(6258, Level.INFO, true, "Users {0} added to group {1}", ""),
	USERS_REMOVED							(6259, Level.INFO, true, "Users {0} removed from group {1}", ""),
	WARN_CREDENTIALS_INSECURE				(6260, Level.WARN, false, "Credential file {0} ignored, because it is insecure (group or world readable)", ""),
	WARN_UNEXPECTED_STATUS					(6261, Level.WARN, false, "Unexpected HTTP status {0} received", ""),
	WEBCLIENT_ERROR							(6262, Level.ERROR, false, "Error from WebClient - Status {0}, Body {1}, Exception {2}", ""),
	
	
	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private UIMessage(int code, Level level, boolean success, String message, String description) {
		this.level = level;
		this.code = code;
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
