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

	/* --- Error messages --- */
	// General
	INVALID_COMMAND_NAME(2800, Level.ERROR, "Invalid command name {0}", ""),
	SUBCOMMAND_MISSING(2801, Level.ERROR, "Subcommand missing for command {0}", ""),
	USER_NOT_LOGGED_IN(2802, Level.ERROR, "User not logged in", ""),
	USER_NOT_LOGGED_IN_TO_MISSION(2807, Level.ERROR, "User not logged in to any mission", ""),
	NOT_AUTHORIZED(2803, Level.ERROR, "User {0} not authorized to manage {1} for mission {2}", ""),
	EXCEPTION(2806, Level.ERROR, "Command failed (cause: {0})", ""),
	INVALID_TIME(2805, Level.ERROR, "Time format {0} not parseable", ""),
	MANDATORY_ATTRIBUTE_MISSING(2809, Level.ERROR, "Mandatory attribute '{0}' missing", ""),
	COMMAND_NOT_IMPLEMENTED(2999, Level.ERROR, "Command {0} not implemented", ""),
		
	// Login Manager
	HTTP_CONNECTION_FAILURE(2820, Level.ERROR, "HTTP connection failure (cause: {0})", ""),
	MISSION_NOT_FOUND(2825, Level.ERROR, "Mission {0} not found", ""),
	LOGIN_FAILED(2822, Level.ERROR, "Login for user {0} failed", ""),
	NOT_AUTHORIZED_FOR_MISSION(2826, Level.ERROR, "User {0} not authorized for mission {1}", ""),
	INSUFFICIENT_CREDENTIALS(2827, Level.ERROR, "Insufficient credentials given for non-interactive login", ""),
	LOGIN_WITHOUT_MISSION_FAILED(2828, Level.ERROR, "User {0} not authorized to login without a mission", ""),
	CLI_NOT_AUTHORIZED(2829, Level.ERROR, "User {0} not authorized for Command Line Interface", ""),
	
	// Service connection
	HTTP_REQUEST_FAILED(2810, Level.ERROR, "HTTP request failed (cause: {0})", ""),
	SERVICE_REQUEST_FAILED(2811, Level.ERROR, "Service request failed with status {0} ({1}), cause: {2}", ""),
	NOT_AUTHORIZED_FOR_SERVICE(2812, Level.ERROR, "User {0} not authorized for requested service", ""),
	SERIALIZATION_FAILED(2813, Level.ERROR, "Cannot convert object to Json (cause: {0})", ""),
	INVALID_URL(2814, Level.ERROR, "Invalid request URL {0} (cause: {1})", ""),
	UNEXPECTED_STATUS(2815, Level.ERROR, "Unexpected HTTP status {0} received", ""),
	
	// Mission CLI
	NO_MISSIONS_FOUND(2840, Level.ERROR, "No missions found for given search criteria", ""),
	MISSION_NOT_FOUND_BY_ID(2841, Level.ERROR, "Mission with database ID {0} not found", ""),
	MISSION_DATA_INVALID(2842, Level.ERROR, "Mission data invalid (cause: {0})", ""),
	NO_MISSION_CODE_GIVEN(2780, Level.ERROR, "Mission code missing", ""),
	DELETE_PRODUCTS_WITHOUT_FORCE(2782, Level.ERROR, "Option 'delete-products' not valid without option 'force'", ""),
	MISSION_NOT_READABLE(2844, Level.ERROR, "Mission {0} not readable (cause: {1})", ""),
	MISSION_DELETE_FAILED(2783, Level.ERROR, "Deletion of mission {0} failed (cause: {1})", ""),
	SPACECRAFT_EXISTS(2845, Level.ERROR, "Spacecraft {0} exists in mission {1}", ""),
	NO_SPACECRAFT_CODE_GIVEN(2848, Level.ERROR, "No spacecraft code given", ""),
	SPACECRAFT_NOT_FOUND(2785, Level.ERROR, "Spacecraft {0} not found in mission {1}", ""),
	ORBIT_NUMBER_INVALID(2849, Level.ERROR, "Orbit number {0} not numeric", ""),
	ORBIT_DATA_INVALID(2850, Level.ERROR, "Orbit data invalid (cause: {0})", ""),
	NO_ORBITS_FOUND(2852, Level.ERROR, "No orbits found for given search criteria", ""),
	NO_ORBIT_NUMBER_GIVEN(2853, Level.ERROR, "No orbit number given", ""),
	ORBIT_NOT_FOUND(2854, Level.ERROR, "Orbit number {0} not found for spacecraft {1}", ""),
	ORBIT_NOT_FOUND_BY_ID(2855, Level.ERROR, "Orbit with database ID {0} not found", ""),
	ORBIT_DELETE_FAILED(2857, Level.ERROR, "Deletion of orbit {0} for spacecraft {1} failed (cause: {2})", ""),
	LOGGED_IN_TO_MISSION(2784, Level.ERROR, "Operation not allowed, when already logged in to a mission (currently logged in to {0})", ""),
	
	// Order CLI
	NO_ORDERS_FOUND(2930, Level.ERROR, "No orders found for given search criteria", ""),
	INVALID_SLICING_TYPE(2931, Level.ERROR, "Invalid order slicing type {0}", ""),
	INVALID_SLICE_DURATION(2932, Level.ERROR, "Slice duration {0} not numeric", ""),
	INVALID_ORBIT_NUMBER(2933, Level.ERROR, "Orbit number {0} not numeric", ""),
	ORDER_NOT_FOUND(2935, Level.ERROR, "Order with identifier {0} not found", ""),
	INVALID_ORDER_STATE(2937, Level.ERROR, "Operation {0} not allowed for order state {1} (must be {2})", ""),
	NO_IDENTIFIER_GIVEN(2936, Level.ERROR, "No order identifier or database ID given", ""),
	ORDER_DATA_INVALID(2940, Level.ERROR, "Order data invalid (cause: {0})", ""),
	FACILITY_MISSING(2941, Level.ERROR, "Processing facility missing in parameters", ""),
	ORDER_JOBS_NOT_FOUND(2948, Level.ERROR, "No jobs found for order with identifier {0}", ""),
	
	// Job and job step CLI
	NO_JOBS_FOUND(2760, Level.ERROR, "No jobs found for given search criteria", ""),
	INVALID_JOB_STATE(2761, Level.ERROR, "Operation {0} not allowed for job state {1} (must be {2})", ""),
	INVALID_JOB_STATE_VALUE(2762, Level.ERROR, "Invalid job state {0}", ""),
	NO_JOB_DBID_GIVEN(2763, Level.ERROR, "No job database ID given", ""),
	JOB_NOT_FOUND(2764, Level.ERROR, "Job with database ID {0} not found", ""),
	JOB_DATA_INVALID(2765, Level.ERROR, "Job data invalid (cause: {0})", ""),
	INVALID_JOBSTEP_STATE(2770, Level.ERROR, "Operation {0} not allowed for job step state {1} (must be {2})", ""),
	INVALID_JOBSTEP_STATE_VALUE(2771, Level.ERROR, "Invalid job step state {0}", ""),
	NO_JOBSTEP_DBID_GIVEN(2772, Level.ERROR, "No job step database ID given", ""),
	JOBSTEP_NOT_FOUND(2773, Level.ERROR, "Job step with database ID {0} not found", ""),
	JOBSTEP_DATA_INVALID(2774, Level.ERROR, "Job step data invalid (cause: {0})", ""),
	NO_JOBSTEPS_FOUND(2779, Level.ERROR, "No job steps found for given search criteria", ""),
	
	// Product class CLI
	PRODUCTCLASS_DATA_INVALID(2860, Level.ERROR, "Product class data invalid (cause: {0})", ""),
	NO_PRODUCTCLASSES_FOUND(2862, Level.ERROR, "No product classes found for given search criteria", ""),
	NO_PRODCLASS_NAME_GIVEN(2863, Level.ERROR, "No product class name given", ""),
	PRODUCTCLASS_NOT_FOUND(2864, Level.ERROR, "Product class {0} not found", ""),
	PRODUCTCLASS_NOT_FOUND_BY_ID(2865, Level.ERROR, "Product class with database ID {0} not found", ""),
	PRODUCTCLASS_DELETE_FAILED(2867, Level.ERROR, "Deletion of product class {0} failed (cause: {1})", ""),
	FILE_NOT_FOUND(2869, Level.ERROR, "Selection rule file {0} not found or not readable", ""),
	SELECTION_RULE_DATA_INVALID(2870, Level.ERROR, "Selection rule data invalid (cause: {0})", ""),
	NO_SELECTION_RULES_FOUND(2872, Level.ERROR, "No selection rules found for product class {0}", ""),
	NO_SELECTION_RULES_FOUND_FOR_SOURCE(2755, Level.ERROR, "No selection rules found for target product class {0} and source product class {2}", ""),
	INPUT_OUT_OF_BOUNDS(2873, Level.ERROR, "Input {0} invalid, please select a number between {0} and {1}", ""),
	INPUT_NOT_NUMERIC(2874, Level.ERROR, "Input {0} not numeric", ""),
	SELECTION_RULE_NOT_FOUND_BY_ID(2875, Level.ERROR, "Selection rule with database ID {0} not found", ""),
	NO_RULEID_GIVEN(2881, Level.ERROR, "No selection rule database ID given", ""),
	RULEID_NOT_NUMERIC(2877, Level.ERROR, "Database ID {0} for selection rule not numeric", ""),
	SELECTION_RULE_DELETE_FAILED(2878, Level.ERROR, "Deletion of selection rule with database ID {0} from product class {1} failed (cause: {2})", ""),
	INVALID_VISIBILITY(2880, Level.ERROR, "Invalid product visibility {0}", ""),
	
	// Ingestor/product CLI
	NO_PRODUCTS_FOUND(2950, Level.ERROR, "No products found for given search criteria", ""),
	INVALID_DATABASE_ID(2952, Level.ERROR, "Database ID {0} not numeric", ""),
	NO_PRODUCT_DBID_GIVEN(2953, Level.ERROR, "No product database ID given", ""),
	PRODUCT_NOT_FOUND(2954, Level.ERROR, "Product with database ID {0} not found", ""),
	INGESTION_FILE_MISSING(2957, Level.ERROR, "No file for product ingestion given", ""),
	PROCESSING_FACILITY_MISSING(2958, Level.ERROR, "No processing facility to ingest to given", ""),
	PRODUCT_DATA_INVALID(2960, Level.ERROR, "Product data invalid (cause: {0})", ""),
	PRODUCT_ID_OR_FACILITY_MISSING(2961, Level.ERROR, "Product database ID or processing facility missing", ""),
	PRODUCTFILE_NOT_FOUND(2962, Level.ERROR, "No product file found for product database ID {0} and processing facility {1}", ""),
	PRODUCT_HAS_NO_FILES(2964, Level.ERROR, "Product with database ID {0} has no files", ""),
	PRODUCT_CLASS_MISMATCH(2965, Level.ERROR, "Product with database ID {0} is not of requested class {2}", ""),
	
	// Processor CLI
	NO_PROCESSORCLASSES_FOUND(2970, Level.ERROR, "No processor classes found for given search criteria", ""),
	NO_PROCCLASS_IDENTIFIER_GIVEN(2973, Level.ERROR, "No processor class name given", ""),
	PROCESSORCLASS_NOT_FOUND(2974, Level.ERROR, "Processor class {0} not found", ""),
	PROCESSORCLASS_NOT_FOUND_BY_ID(2984, Level.ERROR, "Processor class with database ID {0} not found", ""),
	PROCESSORCLASS_DATA_INVALID(2986, Level.ERROR, "Processor class data invalid (cause: {0})", ""),
	PROCESSORCLASS_DELETE_FAILED(2989, Level.ERROR, "Deletion of processor class {0} failed (cause: {1})", ""),
	INVALID_CRITICALITY_LEVEL(2977, Level.ERROR, "Invalid criticality level {0} (expected integer > 1)", ""),
	NO_PROCESSORS_FOUND(2979, Level.ERROR, "No processors found for given search criteria", ""),
	NO_PROCESSOR_IDENTIFIER_GIVEN(2980, Level.ERROR, "No processor name and/or version given", ""),
	PROCESSOR_NOT_FOUND(2981, Level.ERROR, "Processor {0} with version {1} not found", ""),
	PROCESSOR_NOT_FOUND_BY_ID(2982, Level.ERROR, "Processor with database ID {0} not found", ""),
	PROCESSOR_DATA_INVALID(2987, Level.ERROR, "Processor data invalid (cause: {0})", ""),
	PROCESSOR_DELETE_FAILED(2988, Level.ERROR, "Deletion of processor {0} with version {1} failed (cause: {2})", ""),
	CONFIGURATION_DATA_INVALID(2990, Level.ERROR, "Configuration data invalid (cause: {0})", ""),
	NO_CONFIGURATIONS_FOUND(2998, Level.ERROR, "No configurations found for given search criteria", ""),
	NO_CONFIGURATION_IDENTIFIER_GIVEN(2994, Level.ERROR, "No processor name and/or configuration version given", ""),
	CONFIGURATION_NOT_FOUND(2995, Level.ERROR, "Configuration for processor {0} with configuration version {1} not found", ""),
	CONFIGURATION_NOT_FOUND_BY_ID(2996, Level.ERROR, "Configuration with database ID {0} not found", ""),
	CONFIGURATION_DELETE_FAILED(2997, Level.ERROR, "Deletion of processor {0} with version {1} failed (cause: {2})", ""),
	CONFIGUREDPROCESSOR_DATA_INVALID(2890, Level.ERROR, "Configuration data invalid (cause: {0})", ""),
	NO_CONFIGUREDPROCESSORS_FOUND(2898, Level.ERROR, "No processors found for given search criteria", ""),
	NO_CONFIGUREDPROCESSOR_IDENTIFIER_GIVEN(2894, Level.ERROR, "No configured processor identifier given", ""),
	CONFIGUREDPROCESSOR_NOT_FOUND(2895, Level.ERROR, "Configured processor {0} not found", ""),
	CONFIGUREDPROCESSOR_NOT_FOUND_BY_ID(2896, Level.ERROR, "Configured processor with database ID {0} not found", ""),
	CONFIGUREDPROCESSOR_DELETE_FAILED(2897, Level.ERROR, "Deletion of configured processor {0} failed (cause: {1})", ""),
	
	// User/group CLI
	USER_DATA_INVALID(2700, Level.ERROR, "User account data invalid (cause: {0})", ""),
	NO_USERS_FOUND(2702, Level.ERROR, "No user accounts found for mission {0}", ""),
	USER_NOT_FOUND_BY_NAME(2703, Level.ERROR, "User account {0} not found for mission {1}", ""),
	PASSWORD_MISMATCH(2704, Level.ERROR, "Passwords do not match", ""),
	NO_USERNAME_GIVEN(2705, Level.ERROR, "No username given", ""),
	USER_DELETE_FAILED(2707, Level.ERROR, "Deletion of user account {0} failed (cause: {1})", ""),
	NO_AUTHORITIES_GIVEN(2713, Level.ERROR, "No valid authorities given in command", ""),
	NO_GROUPNAME_GIVEN(2714, Level.ERROR, "No group name given", ""),
	GROUP_NOT_FOUND_BY_ID(2715, Level.ERROR, "Group with database ID {0} not found", ""),
	GROUP_DATA_INVALID(2716, Level.ERROR, "User group data invalid (cause: {0})", ""),
	NO_GROUPS_FOUND(2718, Level.ERROR, "No user groups found for mission {0}", ""),
	GROUP_NOT_FOUND_BY_NAME(2719, Level.ERROR, "User group {0} not found for mission {1}", ""),
	MISSION_ALREADY_SET(2720, Level.ERROR, "Already logged in to mission {0}, use of '--mission' option not allowed", ""),
	GROUP_DELETE_FAILED(2723, Level.ERROR, "Deletion of user group {0} failed (cause: {1})", ""),
	NO_USERS_GIVEN(2726, Level.ERROR, "No users given in command", ""),
	NO_USERS_FOUND_IN_GROUP(2729, Level.ERROR, "No user accounts found for user group {0}", ""),
	PASSWORD_STRENGTH_INSUFFICIENT(2731, Level.ERROR, "Password strength insufficient (min. length {0} characters, min. {1} types of the four element groups lowercase letters, uppercase letters, digits and special characters '{2}' required)", ""),
	INVALID_IDENT_FILE(2732, Level.ERROR, "Credentials file {0} invalid (does not contain username and password)", ""),
	USERNAME_MISMATCH(2733, Level.ERROR, "Username {0} to update does not match username {1} from credentials file {2}", ""),
	PASSWORD_CHANGE_NOT_ALLOWED(2734, Level.ERROR, "Password change not allowed in non-interactive mode", ""),
	PASSWORDS_MUST_DIFFER(2735, Level.ERROR, "Old and new password must be different", ""),
	SKIPPING_INVALID_AUTHORITY(2736, Level.ERROR, "Skipping invalid authority {0}", ""),
	ALREADY_MEMBER(2737, Level.ERROR, "User {0} is already a member of group {1}", ""),
	NOT_MEMBER(2738, Level.ERROR, "User {0} is not a member of group {1}", ""),
	
	// Facility CLI
	NO_FACILITIES_FOUND(2740, Level.ERROR, "No processing facilities found", ""),
	FACILITY_NOT_FOUND(2741, Level.ERROR, "Processing facility {0} not found", ""),
	FACILITY_NOT_FOUND_BY_ID(2742, Level.ERROR, "Processing facility with database ID {0} not found", ""),
	FACILITY_NOT_READABLE(2745, Level.ERROR, "Processing facility {0} not readable (cause: {1})", ""),
	FACILITY_DATA_INVALID(2743, Level.ERROR, "Processing facility data invalid (cause: {0})", ""),
	NO_FACILITY_NAME_GIVEN(2748, Level.ERROR, "No processing facility name given", ""),
	FACILITY_DELETE_FAILED(2749, Level.ERROR, "Deletion of processing facility {0} failed (cause: {1})", ""),
	
	// CLIUtil
	INVALID_FILE_TYPE(2830, Level.ERROR, "Invalid file format {0}", ""),
	INVALID_FILE_STRUCTURE(2831, Level.ERROR, "{0} content of file {1} invalid for object generation (cause: {2})", ""),
	INVALID_FILE_SYNTAX(2832, Level.ERROR, "File {0} contains invalid {1} content (cause: {2})", ""),
	INVALID_ATTRIBUTE_NAME(2833, Level.ERROR, "Invalid attribute name {0}", ""),
	INVALID_ATTRIBUTE_TYPE(2834, Level.ERROR, "Attribute {0} cannot be converted to type {1}", ""),
	REFLECTION_EXCEPTION(2835, Level.ERROR, "Reflection exception setting attribute {0} (cause: {2})", ""),
	GENERATION_EXCEPTION(2836, Level.ERROR, "Write exception serializing object {0} to format {1} (cause: {2})", ""),
	MAPPING_EXCEPTION(2837, Level.ERROR, "Exception mapping object {0} to format {1} (cause: {2})", ""),
	CREDENTIALS_INSECURE(2816, Level.ERROR, "Credential file {0} ignored, because it is insecure (group or world readable)", ""),
	CREDENTIALS_NOT_FOUND(2817, Level.ERROR, "Credential file {0} not found", ""),
	CREDENTIALS_NOT_READABLE(2818, Level.ERROR, "Credential file {0} not readable (cause: {1})", ""),
	
	// CLI Parser
	ILLEGAL_PARAMETER_TYPE(2901, Level.ERROR, "Illegal parameter type {0}, expected one of {1}", ""),
	ILLEGAL_OPTION_TYPE(2902, Level.ERROR, "Illegal option type {0}, expected one of {2}", ""),
	INVALID_COMMAND_OPTION(2910, Level.ERROR, "Invalid command option {0} found", ""),
	OPTION_NOT_ALLOWED(2911, Level.ERROR, "Option {0} not allowed after command parameter", ""),
	ILLEGAL_OPTION(2912, Level.ERROR, "Option {0} not allowed for command {1}", ""),
	ILLEGAL_OPTION_VALUE(2913, Level.ERROR, "Illegal option value {0} for option {1} of type {2}", ""),
	TOO_MANY_PARAMETERS(2914, Level.ERROR, "Too many parameters for command {0}", ""),
	ATTRIBUTE_PARAMETER_EXPECTED(2915, Level.ERROR, "Parameter of format '<attribute name>=<attribute value>' expected at position {0} for command {1}", ""),
	PARAMETER_TYPE_MISMATCH(2919, Level.ERROR, "Parameter of type {0} expected at position {1} for command {2}", ""),
	ILLEGAL_COMMAND(2916, Level.ERROR, "Illegal command {0}", ""),
	ILLEGAL_SUBCOMMAND(2917, Level.ERROR, "Illegal subcommand {0}", ""),
	PARAMETER_MISSING(2918, Level.ERROR, "Required parameter {0} not found for command {1}", ""),
	
	// CLI Main
	SYNTAX_FILE_NOT_FOUND(2920, Level.ERROR, "Syntax file {0} not found", ""),
	SYNTAX_FILE_ERROR(2921, Level.ERROR, "Parsing error in syntax file {0} (cause: {1})", ""),
	COMMAND_LINE_PROMPT_SUPPRESSED(2922, Level.ERROR, "Command line prompt suppressed by proseo.cli.start parameter", ""),
	COMMAND_NAME_NULL(2923, Level.ERROR, "Command name must not be null", ""),
	PASSWORD_MISSING(2924, Level.ERROR, "No password given for user {0}", ""),
	UNCAUGHT_EXCEPTION(2925, Level.ERROR, "prosEO Command Line Interface terminated by exception: {0}", ""),
	USER_INTERRUPT(2926, Level.ERROR, "prosEO Command Line Interface exiting due to user interrupt", ""),
	
	/* --- Info messages -- */
	// General
	OPERATION_CANCELLED(2804, Level.INFO, "Operation cancelled", ""),
	NOT_MODIFIED(2808, Level.INFO, "Data not modified", ""),
	
	// Login Manager
	LOGGING_IN(2838, Level.INFO, "Logging in to prosEO with user {0}", ""),
	LOGGED_IN(2821, Level.INFO, "User {0} logged in", ""),
	LOGGED_OUT(2823, Level.INFO, "User {0} logged out", ""),
	LOGIN_CANCELLED(2824, Level.INFO, "No username given, login cancelled", ""),
	
	// CLI Parser
	SYNTAX_LOADED(2900, Level.INFO, "Command line syntax loaded from syntax file {0}", ""),
	
	// CLI Main
	END_OF_FILE(2927, Level.INFO, "End of input reached, prosEO Command Line Interface terminates", ""),
	CLI_TERMINATED(2928, Level.INFO, "'exit' command received, prosEO Command Line Interface terminates", ""),
	
	// Mission CLI
	MISSION_CREATED(2859, Level.INFO, "Mission {0} created (database ID {1})", ""),
	MISSION_UPDATED(2843, Level.INFO, "Mission {0} updated (new version {1})", ""),
	MISSION_DELETED(2781, Level.INFO, "Mission {0} deleted", ""),
	SPACECRAFT_ADDED(2846, Level.INFO, "Spacecraft {0} added (database ID {1})", ""),
	SPACECRAFT_REMOVED(2847, Level.INFO, "Spacecraft {0} removed from mission {1}", ""),
	ORBITS_CREATED(2851, Level.INFO, "{0} orbits created", ""),
	ORBITS_UPDATED(2856, Level.INFO, "{0} orbits updated", ""),
	ORBITS_DELETED(2858, Level.INFO, "{0} orbits deleted", ""),
	
	// Order CLI
	ORDER_CREATED(2934, Level.INFO, "Order with identifier {0} created (database ID {0})", ""),
	ORDER_UPDATED(2938, Level.INFO, "Order with identifier {0} updated (new version {1})", ""),
	ORDER_APPROVED(2942, Level.INFO, "Order with identifier {0} approved (new version {1})", ""),
	ORDER_PLANNED(2943, Level.INFO, "Order with identifier {0} planned (new version {1})", ""),
	ORDER_RELEASING(2929, Level.INFO, "Order with identifier {0} released (new version {1})", ""),
	ORDER_RELEASED(2944, Level.INFO, "Order with identifier {0} released (new version {1})", ""),
	ORDER_SUSPENDED(2945, Level.INFO, "Order with identifier {0} suspended (new version {1})", ""),
	ORDER_CANCELLED(2946, Level.INFO, "Order with identifier {0} cancelled (new version {1})", ""),
	ORDER_RESET(2947, Level.INFO, "Order with identifier {0} reset (new version {1})", ""),
	ORDER_DELETED(2939, Level.INFO, "Order with identifier {0} deleted", ""),
	RETRYING_ORDER(2759, Level.INFO, "Retrying order with identifier {0} (new version {1})", ""),
	
	// Job and job step CLI
	JOB_SUSPENDED(2766, Level.INFO, "Job with database ID {0} suspended (new version {1})", ""),
	JOB_RESUMED(2767, Level.INFO, "Job with database ID {0} resumed (new version {1})", ""),
	JOB_CANCELLED(2768, Level.INFO, "Job with database ID {0} cancelled (new version {1})", ""),
	RETRYING_JOB(2769, Level.INFO, "Retrying job with database ID {0} (new version {1})", ""),
	JOBSTEP_SUSPENDED(2775, Level.INFO, "Job step with database ID {0} suspended (new version {1})", ""),
	JOBSTEP_RESUMED(2776, Level.INFO, "Job step with database ID {0} resumed (new version {1})", ""),
	JOBSTEP_CANCELLED(2777, Level.INFO, "Job step with database ID {0} cancelled (new version {1})", ""),
	RETRYING_JOBSTEP(2778, Level.INFO, "Retrying job step with database ID {0} (new version {1})", ""),
	
	// Product class CLI
	PRODUCTCLASS_CREATED(2861, Level.INFO, "Product class {0} created (database ID {1})", ""),
	PRODUCTCLASS_UPDATED(2866, Level.INFO, "Product class with database ID {0} updated (new version {1})", ""),
	PRODUCTCLASS_DELETED(2868, Level.INFO, "Product class with database ID {0} deleted", ""),
	SELECTION_RULES_CREATED(2871, Level.INFO, "{0} selection rules created for product class {1}", ""),
	SELECTION_RULE_UPDATED(2876, Level.INFO, "Selection rule with database ID {0} updated (new version {1})", ""),
	SELECTION_RULE_DELETED(2879, Level.INFO, "Selection rule with database ID {0} deleted", ""),
	
	// Ingestor/product CLI
	PRODUCT_CREATED(2951, Level.INFO, "Product of class {0} created (database ID {1}, UUID {2})", ""),
	PRODUCT_UPDATED(2955, Level.INFO, "Product with database ID {0} updated (new version {1})", ""),
	PRODUCT_DELETED(2956, Level.INFO, "Product with database ID {0} deleted", ""),
	PRODUCTS_INGESTED(2959, Level.INFO, "{0} products ingested to processing facility {1}", ""),
	PRODUCTFILE_DELETED(2963, Level.INFO, "Product file for product database ID {0} and processing facility {2} deleted", ""),

	// User/group CLI
	USER_CREATED(2701, Level.INFO, "User account {0} created", ""),
	USER_UPDATED(2706, Level.INFO, "User account {0} updated", ""),
	USER_DELETED(2708, Level.INFO, "User account {0} deleted", ""),
	USER_ENABLED(2709, Level.INFO, "User account {0} enabled", ""),
	USER_DISABLED(2710, Level.INFO, "User account {0} disabled", ""),
	AUTHORITIES_GRANTED(2711, Level.INFO, "Authorities {0} granted to user {1}", ""),
	AUTHORITIES_REVOKED(2712, Level.INFO, "Authorities {0} revoked from user {1}", ""),
	GROUP_CREATED(2717, Level.INFO, "User group {0} created", ""),
	GROUP_UPDATED(2721, Level.INFO, "User group {0} updated", ""),
	GROUP_DELETED(2722, Level.INFO, "User group {0} deleted", ""),
	GROUP_AUTHORITIES_GRANTED(2724, Level.INFO, "Authorities {0} granted to group {1}", ""),
	GROUP_AUTHORITIES_REVOKED(2725, Level.INFO, "Authorities {0} revoked from group {1}", ""),
	USERS_ADDED(2727, Level.INFO, "Users {0} added to group {1}", ""),
	USERS_REMOVED(2728, Level.INFO, "Users {0} removed from group {1}", ""),
	PASSWORD_CHANGED(2730, Level.INFO, "Password changed for user {0}", ""),

	// Facility CLI
	FACILITY_CREATED(2746, Level.INFO, "Processing facility {0} created (database ID {1})", ""),
	FACILITY_UPDATED(2744, Level.INFO, "Processing facility with database ID {0} updated (new version {1})", ""),
	FACILITY_DELETED(2747, Level.INFO, "Processing facility with database ID {0} deleted", ""),

	// Processor CLI
	PROCESSORCLASS_CREATED(2971, Level.INFO, "Processor class {0} created (database ID {1})", ""),
	PROCESSORCLASS_UPDATED(2975, Level.INFO, "Processor class with database ID {0} updated (new version {1})", ""),
	PROCESSORCLASS_DELETED(2976, Level.INFO, "Processor class with database ID {0} deleted", ""),
	PROCESSOR_CREATED(2978, Level.INFO, "Processor {0} with version {1} created (database ID {2})", ""),
	PROCESSOR_UPDATED(2983, Level.INFO, "Processor with database ID {0} updated (new version {1})", ""),
	PROCESSOR_DELETED(2985, Level.INFO, "Processor with database ID {0} deleted", ""),
	CONFIGURATION_CREATED(2991, Level.INFO, "Configuration {0} with version {0} created (database ID {1})", ""),
	CONFIGURATION_UPDATED(2992, Level.INFO, "Configuration with database ID {0} updated (new version {1})", ""),
	CONFIGURATION_DELETED(2993, Level.INFO, "Configuration with database ID {0} deleted", ""),
	CONFIGUREDPROCESSOR_CREATED(2891, Level.INFO, "Configured processor {0} for processor {1}, version {2} and configuration version {3} created (database ID {4})", ""),
	CONFIGUREDPROCESSOR_UPDATED(2892, Level.INFO, "Configured processor with database ID {0} updated (new version {1})", ""),
	CONFIGUREDPROCESSOR_DELETED(2893, Level.INFO, "Configured processor with database ID {0} deleted", ""),
	
	// GUI specific
	NO_FILECLASSES_FOUND(2770, Level.ERROR, "No file classes found for given search criteria", ""),
	NO_PROCESSINGMODES_FOUND(2771, Level.ERROR, "No processing modes found for given search criteria", ""),
	NO_SPACECRAFTS_FOUND(2771, Level.ERROR, "No spacecrafts found for given search criteria", ""),

	
	;

	private final int code;
	private final Level level;
	private final String message;
	private final String description;

	private UIMessage(int code, Level level, String message, String description) {
		this.level = level;
		this.code = code;
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
	 * Get a more detailed description of the message's purpose.
	 * 
	 * @return A description of the message.
	 */
	public String getDescription() {
		return description;
	}

}
