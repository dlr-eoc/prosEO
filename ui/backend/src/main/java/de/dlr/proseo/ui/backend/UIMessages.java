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
	public static final int MSG_ID_USER_NOT_LOGGED_IN_TO_MISSION = 2807;
	public static final int MSG_ID_NOT_MODIFIED = 2808;
	public static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	// Service connection
	public static final int MSG_ID_HTTP_REQUEST_FAILED = 2810;
	public static final int MSG_ID_SERVICE_REQUEST_FAILED = 2811;
	public static final int MSG_ID_NOT_AUTHORIZED_FOR_SERVICE = 2812;
	public static final int MSG_ID_SERIALIZATION_FAILED = 2813;
	public static final int MSG_ID_INVALID_URL = 2814;
	public static final int MSG_ID_UNEXPECTED_STATUS = 2815;
	// 2816, 2817, 2818 used for CLI Main
	
	// Login Manager
	public static final int MSG_ID_HTTP_CONNECTION_FAILURE = 2820;
	public static final int MSG_ID_LOGGED_IN = 2821;
	public static final int MSG_ID_LOGIN_FAILED = 2822;
	public static final int MSG_ID_LOGGED_OUT = 2823;
	public static final int MSG_ID_LOGIN_CANCELLED = 2824;
	public static final int MSG_ID_MISSION_NOT_FOUND = 2825;
	public static final int MSG_ID_NOT_AUTHORIZED_FOR_MISSION = 2826;
	public static final int MSG_ID_INSUFFICIENT_CREDENTIALS = 2827;
	public static final int MSG_ID_LOGIN_WITHOUT_MISSION_FAILED = 2828;
	public static final int MSG_ID_CLI_NOT_AUTHORIZED = 2829;
	
	// CLIUtil
	public static final int MSG_ID_INVALID_FILE_TYPE = 2830;
	public static final int MSG_ID_INVALID_FILE_STRUCTURE = 2831;
	public static final int MSG_ID_INVALID_FILE_SYNTAX = 2832;
	public static final int MSG_ID_INVALID_ATTRIBUTE_NAME = 2833;
	public static final int MSG_ID_INVALID_ATTRIBUTE_TYPE = 2834;
	public static final int MSG_ID_REFLECTION_EXCEPTION = 2835;
	public static final int MSG_ID_GENERATION_EXCEPTION = 2836;
	public static final int MSG_ID_MAPPING_EXCEPTION = 2837;

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
	public static final int MSG_ID_PARAMETER_TYPE_MISMATCH = 2919;
	
	// CLI Main
	public static final int MSG_ID_SYNTAX_FILE_NOT_FOUND = 2920;
	public static final int MSG_ID_SYNTAX_FILE_ERROR = 2921;
	public static final int MSG_ID_COMMAND_LINE_PROMPT_SUPPRESSED = 2922;
	public static final int MSG_ID_COMMAND_NAME_NULL = 2923;
	public static final int MSG_ID_PASSWORD_MISSING = 2924;
	public static final int MSG_ID_UNCAUGHT_EXCEPTION = 2925;
	public static final int MSG_ID_USER_INTERRUPT = 2926;
	public static final int MSG_ID_END_OF_FILE = 2927;
	public static final int MSG_ID_CLI_TERMINATED = 2928;
	public static final int MSG_ID_CREDENTIALS_UNSAFE = 2816;
	public static final int MSG_ID_CREDENTIALS_NOT_FOUND = 2817;
	public static final int MSG_ID_CREDENTIALS_NOT_READABLE = 2818;
	
	// Mission CLI
	public static final int MSG_ID_NO_MISSIONS_FOUND = 2840;
	public static final int MSG_ID_MISSION_NOT_FOUND_BY_ID = 2841;
	public static final int MSG_ID_MISSION_DATA_INVALID = 2842;
	public static final int MSG_ID_MISSION_UPDATED = 2843;
	public static final int MSG_ID_MISSION_NOT_READABLE = 2844;
	public static final int MSG_ID_SPACECRAFT_EXISTS = 2845;
	public static final int MSG_ID_SPACECRAFT_ADDED = 2846;
	public static final int MSG_ID_SPACECRAFT_NOT_FOUND = 2846;
	public static final int MSG_ID_SPACECRAFT_REMOVED = 2847;
	public static final int MSG_ID_NO_SPACECRAFT_CODE_GIVEN = 2848;
	public static final int MSG_ID_ORBIT_NUMBER_INVALID = 2849;
	public static final int MSG_ID_ORBIT_DATA_INVALID = 2850;
	public static final int MSG_ID_ORBITS_CREATED = 2851;
	public static final int MSG_ID_NO_ORBITS_FOUND = 2852;
	public static final int MSG_ID_NO_ORBIT_NUMBER_GIVEN = 2853;
	public static final int MSG_ID_ORBIT_NOT_FOUND = 2854;
	public static final int MSG_ID_ORBIT_NOT_FOUND_BY_ID = 2855;
	public static final int MSG_ID_ORBITS_UPDATED = 2856;
	public static final int MSG_ID_ORBIT_DELETE_FAILED = 2857;
	public static final int MSG_ID_ORBITS_DELETED = 2858;
	public static final int MSG_ID_MISSION_CREATED = 2859;
	public static final int MSG_ID_NO_MISSION_CODE_GIVEN = 2780;
	public static final int MSG_ID_MISSION_DELETED = 2781;
	public static final int MSG_ID_DELETE_PRODUCTS_WITHOUT_FORCE = 2782;
	public static final int MSG_ID_MISSION_DELETE_FAILED = 2783;
	public static final int MSG_ID_LOGGED_IN_TO_MISSION = 2784;
	
	// Product class CLI
	public static final int MSG_ID_PRODUCTCLASS_DATA_INVALID = 2860;
	public static final int MSG_ID_PRODUCTCLASS_CREATED = 2861;
	public static final int MSG_ID_NO_PRODUCTCLASSES_FOUND = 2862;
	public static final int MSG_ID_NO_PRODCLASS_NAME_GIVEN = 2863;
	public static final int MSG_ID_PRODUCTCLASS_NOT_FOUND = 2864;
	public static final int MSG_ID_PRODUCTCLASS_NOT_FOUND_BY_ID = 2865;
	public static final int MSG_ID_PRODUCTCLASS_UPDATED = 2866;
	public static final int MSG_ID_PRODUCTCLASS_DELETE_FAILED = 2867;
	public static final int MSG_ID_PRODUCTCLASS_DELETED = 2868;
	public static final int MSG_ID_FILE_NOT_FOUND = 2869;
	public static final int MSG_ID_SELECTION_RULE_DATA_INVALID = 2870;
	public static final int MSG_ID_SELECTION_RULES_CREATED = 2871;
	public static final int MSG_ID_NO_SELECTION_RULES_FOUND = 2872;
	public static final int MSG_ID_NO_SELECTION_RULES_FOUND_FOR_SOURCE = 2755;
	public static final int MSG_ID_INPUT_OUT_OF_BOUNDS = 2873;
	public static final int MSG_ID_INPUT_NOT_NUMERIC = 2874;
	public static final int MSG_ID_SELECTION_RULE_NOT_FOUND_BY_ID = 2875;
	public static final int MSG_ID_SELECTION_RULE_UPDATED = 2876;
	public static final int MSG_ID_RULEID_NOT_NUMERIC = 2877;
	public static final int MSG_ID_SELECTION_RULE_DELETE_FAILED = 2878;
	public static final int MSG_ID_SELECTION_RULE_DELETED = 2879;
	public static final int MSG_ID_INVALID_VISIBILITY = 2880;
	public static final int MSG_ID_NO_RULEID_GIVEN = 2881;
	
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
	public static final int MSG_ID_FACILITY_MISSING = 2941;
	public static final int MSG_ID_ORDER_APPROVED = 2942;
	public static final int MSG_ID_ORDER_PLANNED = 2943;
	public static final int MSG_ID_ORDER_RELEASED = 2944;
	public static final int MSG_ID_ORDER_SUSPENDED = 2945;
	public static final int MSG_ID_ORDER_CANCELLED = 2946;
	public static final int MSG_ID_ORDER_RESET = 2947;
	public static final int MSG_ID_ORDER_JOBS_NOT_FOUND = 2948;
	public static final int MSG_ID_RETRYING_ORDER = 2759;

	// Job/job step CLI
	public static final int MSG_ID_NO_JOBS_FOUND = 2760;
	public static final int MSG_ID_INVALID_JOB_STATE = 2761;
	public static final int MSG_ID_INVALID_JOB_STATE_VALUE = 2762;
	public static final int MSG_ID_NO_JOB_DBID_GIVEN = 2763;
	public static final int MSG_ID_JOB_NOT_FOUND = 2764;
	public static final int MSG_ID_JOB_DATA_INVALID = 2765;
	public static final int MSG_ID_JOB_SUSPENDED = 2766;
	public static final int MSG_ID_JOB_RESUMED = 2767;
	public static final int MSG_ID_JOB_CANCELLED = 2768;
	public static final int MSG_ID_RETRYING_JOB = 2769;
	public static final int MSG_ID_INVALID_JOBSTEP_STATE = 2770;
	public static final int MSG_ID_INVALID_JOBSTEP_STATE_VALUE = 2771;
	public static final int MSG_ID_NO_JOBSTEP_DBID_GIVEN = 2772;
	public static final int MSG_ID_JOBSTEP_NOT_FOUND = 2773;
	public static final int MSG_ID_JOBSTEP_DATA_INVALID = 2774;
	public static final int MSG_ID_JOBSTEP_SUSPENDED = 2775;
	public static final int MSG_ID_JOBSTEP_RESUMED = 2776;
	public static final int MSG_ID_JOBSTEP_CANCELLED = 2777;
	public static final int MSG_ID_RETRYING_JOBSTEP = 2778;
	
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
	public static final int MSG_ID_PRODUCT_ID_OR_FACILITY_MISSING = 2961;
	public static final int MSG_ID_PRODUCTFILE_NOT_FOUND = 2962;
	public static final int MSG_ID_PRODUCTFILE_DELETED = 2963;
	public static final int MSG_ID_PRODUCT_HAS_NO_FILES = 2964;
	public static final int MSG_ID_PRODUCT_CLASS_MISMATCH = 2965;
	
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
	public static final int MSG_ID_CONFIGURATION_DATA_INVALID = 2990;
	public static final int MSG_ID_CONFIGURATION_CREATED = 2991;
	public static final int MSG_ID_CONFIGURATION_UPDATED = 2992;
	public static final int MSG_ID_CONFIGURATION_DELETED = 2993;
	public static final int MSG_ID_NO_CONFIGURATION_IDENTIFIER_GIVEN = 2994;
	public static final int MSG_ID_CONFIGURATION_NOT_FOUND = 2995;
	public static final int MSG_ID_CONFIGURATION_NOT_FOUND_BY_ID = 2996;
	public static final int MSG_ID_CONFIGURATION_DELETE_FAILED = 2997;
	public static final int MSG_ID_NO_CONFIGURATIONS_FOUND = 2998;
	public static final int MSG_ID_CONFIGUREDPROCESSOR_DATA_INVALID = 2890;
	public static final int MSG_ID_CONFIGUREDPROCESSOR_CREATED = 2891;
	public static final int MSG_ID_CONFIGUREDPROCESSOR_UPDATED = 2892;
	public static final int MSG_ID_CONFIGUREDPROCESSOR_DELETED = 2893;
	public static final int MSG_ID_NO_CONFIGUREDPROCESSOR_IDENTIFIER_GIVEN = 2894;
	public static final int MSG_ID_CONFIGUREDPROCESSOR_NOT_FOUND = 2895;
	public static final int MSG_ID_CONFIGUREDPROCESSOR_NOT_FOUND_BY_ID = 2896;
	public static final int MSG_ID_CONFIGUREDPROCESSOR_DELETE_FAILED = 2897;
	public static final int MSG_ID_NO_CONFIGUREDPROCESSORS_FOUND = 2898;
	
	// User/group CLI
	public static final int MSG_ID_USER_DATA_INVALID = 2700;
	public static final int MSG_ID_USER_CREATED = 2701;
	public static final int MSG_ID_NO_USERS_FOUND = 2702;
	public static final int MSG_ID_USER_NOT_FOUND_BY_NAME = 2703;
	public static final int MSG_ID_PASSWORD_MISMATCH = 2704;
	public static final int MSG_ID_NO_USERNAME_GIVEN = 2705;
	public static final int MSG_ID_USER_UPDATED = 2705;
	public static final int MSG_ID_USER_DELETE_FAILED = 2706;
	public static final int MSG_ID_USER_DELETED = 2707;
	public static final int MSG_ID_USER_ENABLED = 2708;
	public static final int MSG_ID_USER_DISABLED = 2709;
	public static final int MSG_ID_AUTHORITIES_GRANTED = 2711;
	public static final int MSG_ID_AUTHORITIES_REVOKED = 2712;
	public static final int MSG_ID_NO_AUTHORITIES_GIVEN = 2713;
	public static final int MSG_ID_NO_GROUPNAME_GIVEN = 2714;
	public static final int MSG_ID_GROUP_NOT_FOUND_BY_ID = 2715;
	public static final int MSG_ID_GROUP_DATA_INVALID = 2716;
	public static final int MSG_ID_GROUP_CREATED = 2717;
	public static final int MSG_ID_NO_GROUPS_FOUND = 2718;
	public static final int MSG_ID_GROUP_NOT_FOUND_BY_NAME = 2719;
	public static final int MSG_ID_MISSION_ALREADY_SET = 2720;
	public static final int MSG_ID_GROUP_UPDATED = 2721;
	public static final int MSG_ID_GROUP_DELETED = 2722;
	public static final int MSG_ID_GROUP_DELETE_FAILED = 2723;
	public static final int MSG_ID_GROUP_AUTHORITIES_GRANTED = 2724;
	public static final int MSG_ID_GROUP_AUTHORITIES_REVOKED = 2725;
	public static final int MSG_ID_NO_USERS_GIVEN = 2726;
	public static final int MSG_ID_USERS_ADDED = 2727;
	public static final int MSG_ID_USERS_REMOVED = 2728;
	public static final int MSG_ID_NO_USERS_FOUND_IN_GROUP = 2729;

	// Facility CLI
	public static final int MSG_ID_NO_FACILITIES_FOUND = 2740;
	public static final int MSG_ID_FACILITY_NOT_FOUND = 2741;
	public static final int MSG_ID_FACILITY_NOT_FOUND_BY_ID = 2742;
	public static final int MSG_ID_FACILITY_DATA_INVALID = 2743;
	public static final int MSG_ID_FACILITY_UPDATED = 2744;
	public static final int MSG_ID_FACILITY_NOT_READABLE = 2745;
	public static final int MSG_ID_FACILITY_CREATED = 2746;
	public static final int MSG_ID_FACILITY_DELETED = 2747;
	public static final int MSG_ID_NO_FACILITY_NAME_GIVEN = 2748;
	public static final int MSG_ID_FACILITY_DELETE_FAILED = 2749;
	
	private static Map<Integer, String> uiMessages = new HashMap<>();
	
	
	/* Message strings (private, may be moved to database or localized properties file some day) */
	private enum UIMessage {
		/* --- Error messages --- */
		// General
		MSG_INVALID_COMMAND_NAME ("(E%d) Invalid command name %s", MSG_ID_INVALID_COMMAND_NAME),
		MSG_SUBCOMMAND_MISSING ("(E%d) Subcommand missing for command %s", MSG_ID_SUBCOMMAND_MISSING),
		MSG_USER_NOT_LOGGED_IN ("(E%d) User not logged in", MSG_ID_USER_NOT_LOGGED_IN),
		MSG_USER_NOT_LOGGED_IN_TO_MISSION ("(E%d) User not logged in to any mission", MSG_ID_USER_NOT_LOGGED_IN_TO_MISSION),
		MSG_NOT_AUTHORIZED ("(E%d) User %s not authorized to manage %s for mission %s", MSG_ID_NOT_AUTHORIZED),
		MSG_EXCEPTION ("(E%d) Command failed (cause: %s)", MSG_ID_EXCEPTION),
		MSG_INVALID_TIME ("(E%d) Time format %s not parseable", MSG_ID_INVALID_TIME),
		MSG_NOT_IMPLEMENTED ("(E%d) Command %s not implemented", MSG_ID_NOT_IMPLEMENTED),

		// Login Manager
		MSG_HTTP_CONNECTION_FAILURE ("(E%d) HTTP connection failure (cause: %s)", MSG_ID_HTTP_CONNECTION_FAILURE),
		MSG_MISSION_NOT_FOUND ("(E%d) Mission %s not found", MSG_ID_MISSION_NOT_FOUND),
		MSG_LOGIN_FAILED ("(E%d) Login for user %s failed", MSG_ID_LOGIN_FAILED),
		MSG_NOT_AUTHORIZED_FOR_MISSION ("(E%d) User %s not authorized for mission %s", MSG_ID_NOT_AUTHORIZED_FOR_MISSION),
		MSG_INSUFFICIENT_CREDENTIALS ("(E%d) Insufficient credentials given for non-interactive login", MSG_ID_INSUFFICIENT_CREDENTIALS),
		MSG_LOGIN_WITHOUT_MISSION_FAILED ("(E%d) User %s not authorized to login without a mission", MSG_ID_LOGIN_WITHOUT_MISSION_FAILED),
		MSG_CLI_NOT_AUTHORIZED ("(E%d) User %s not authorized for Command Line Interface", MSG_ID_CLI_NOT_AUTHORIZED),
		
		// Service connection
		MSG_HTTP_REQUEST_FAILED ("(E%d) HTTP request failed (cause: %s)", MSG_ID_HTTP_REQUEST_FAILED),
		MSG_SERVICE_REQUEST_FAILED ("(E%d) Service request failed with status %d (%s), cause: %s", MSG_ID_SERVICE_REQUEST_FAILED),
		MSG_NOT_AUTHORIZED_FOR_SERVICE ("(E%d) User %s not authorized for requested service", MSG_ID_NOT_AUTHORIZED_FOR_SERVICE),
		MSG_SERIALIZATION_FAILED ("(E%d) Cannot convert object to Json (cause: %s)", MSG_ID_SERIALIZATION_FAILED),
		MSG_INVALID_URL ("(E%d) Invalid request URL %s (cause: %s)", MSG_ID_INVALID_URL),
		MSG_UNEXPECTED_STATUS ("(E%d) Unexpected HTTP status %s received", MSG_ID_UNEXPECTED_STATUS),
		
		// Mission CLI
		MSG_NO_MISSIONS_FOUND ("(E%d) No missions found for given search criteria", MSG_ID_NO_MISSIONS_FOUND),
		MSG_MISSION_NOT_FOUND_BY_ID ("(E%d) Mission with database ID %d not found", MSG_ID_MISSION_NOT_FOUND_BY_ID),
		MSG_MISSION_DATA_INVALID ("(E%d) Mission data invalid (cause: %s)", MSG_ID_MISSION_DATA_INVALID),
		MSG_NO_MISSION_CODE_GIVEN ("(E%d) Mission code missing", MSG_ID_NO_MISSION_CODE_GIVEN),
		MSG_DELETE_PRODUCTS_WITHOUT_FORCE ("(E%d) Option 'delete-products' not valid without option 'force'", MSG_ID_DELETE_PRODUCTS_WITHOUT_FORCE),
		MSG_MISSION_NOT_READABLE ("(E%d) Mission %s not readable (cause: %s)", MSG_ID_MISSION_NOT_READABLE),
		MSG_MISSION_DELETE_FAILED ("(E%d) Deletion of mission %s failed (cause: %s)", MSG_ID_MISSION_DELETE_FAILED),
		MSG_SPACECRAFT_EXISTS ("(E%d) Spacecraft %s exists in mission %s", MSG_ID_SPACECRAFT_EXISTS),
		MSG_NO_SPACECRAFT_CODE_GIVEN ("(E%d) No spacecraft code given", MSG_ID_NO_SPACECRAFT_CODE_GIVEN),
		MSG_SPACECRAFT_NOT_FOUND ("(E%d) Spacecraft %s not found in mission %s", MSG_ID_SPACECRAFT_NOT_FOUND),
		MSG_ORBIT_NUMBER_INVALID ("(E%d) Orbit number %s not numeric", MSG_ID_ORBIT_NUMBER_INVALID),
		MSG_ORBIT_DATA_INVALID ("(E%d) Orbit data invalid (cause: %s)", MSG_ID_ORBIT_DATA_INVALID),
		MSG_NO_ORBITS_FOUND ("(E%d) No orbits found for given search criteria", MSG_ID_NO_ORBITS_FOUND),
		MSG_NO_ORBIT_NUMBER_GIVEN ("(E%d) No orbit number given", MSG_ID_NO_ORBIT_NUMBER_GIVEN),
		MSG_ORBIT_NOT_FOUND ("(E%d) Orbit number %d not found for spacecraft %s", MSG_ID_ORBIT_NOT_FOUND),
		MSG_ORBIT_NOT_FOUND_BY_ID ("(E%d) Orbit with database ID %d not found", MSG_ID_ORBIT_NOT_FOUND_BY_ID),
		MSG_ORBIT_DELETE_FAILED ("(E%d) Deletion of orbit %d for spacecraft %s failed (cause: %s)", MSG_ID_ORBIT_DELETE_FAILED),
		MSG_LOGGED_IN_TO_MISSION ("(E%d) Operation not allowed, when already logged in to a mission (currently logged in to %s)", MSG_ID_LOGGED_IN_TO_MISSION),
				
		// Order CLI
		MSG_NO_ORDERS_FOUND ("(E%d) No orders found for given search criteria", MSG_ID_NO_ORDERS_FOUND),
		MSG_INVALID_SLICING_TYPE ("(E%d) Invalid order slicing type %s", MSG_ID_INVALID_SLICING_TYPE),
		MSG_INVALID_SLICE_DURATION ("(E%d) Slice duration %s not numeric", MSG_ID_INVALID_SLICE_DURATION),
		MSG_INVALID_ORBIT_NUMBER ("(E%d) Orbit number %s not numeric", MSG_ID_INVALID_ORBIT_NUMBER),
		MSG_ORDER_NOT_FOUND ("(E%d) Order with identifier %s not found", MSG_ID_ORDER_NOT_FOUND),
		MSG_INVALID_ORDER_STATE ("(E%d) Operation %s not allowed for order state %s (must be %s)", MSG_ID_INVALID_ORDER_STATE),
		MSG_NO_IDENTIFIER_GIVEN ("(E%d) No order identifier or database ID given", MSG_ID_NO_IDENTIFIER_GIVEN),
		MSG_ORDER_DATA_INVALID ("(E%d) Order data invalid (cause: %s)", MSG_ID_ORDER_DATA_INVALID),
		MSG_FACILITY_MISSING ("(E%d) Processing facility missing in parameters", MSG_ID_FACILITY_MISSING),
		MSG_ORDER_JOBS_NOT_FOUND ("(E%d) No jobs found for order with identifier %s", MSG_ID_ORDER_JOBS_NOT_FOUND),
		
		// Job and job step CLI
		MSG_NO_JOBS_FOUND ("(E%d) No jobs found for given search criteria", MSG_ID_NO_JOBS_FOUND),
		MSG_INVALID_JOB_STATE ("(E%d) Operation %s not allowed for job state %s (must be %s)", MSG_ID_INVALID_JOB_STATE),
		MSG_INVALID_JOB_STATE_VALUE ("(E%d) Invalid job state %s", MSG_ID_INVALID_JOB_STATE_VALUE),
		MSG_NO_JOB_DBID_GIVEN ("(E%d) No job database ID given", MSG_ID_NO_JOB_DBID_GIVEN),
		MSG_JOB_NOT_FOUND ("(E%d) Job with database ID %s not found", MSG_ID_JOB_NOT_FOUND),
		MSG_JOB_DATA_INVALID ("(E%d) Job data invalid (cause: %s)", MSG_ID_JOB_DATA_INVALID),
		MSG_INVALID_JOBSTEP_STATE ("(E%d) Operation %s not allowed for job state %s (must be %s)", MSG_ID_INVALID_JOBSTEP_STATE),
		MSG_INVALID_JOBSTEP_STATE_VALUE ("(E%d) Invalid job step state %s", MSG_ID_INVALID_JOBSTEP_STATE_VALUE),
		MSG_NO_JOBSTEP_DBID_GIVEN ("(E%d) No job database ID given", MSG_ID_NO_JOBSTEP_DBID_GIVEN),
		MSG_JOBSTEP_NOT_FOUND ("(E%d) Job with database ID %s not found", MSG_ID_JOBSTEP_NOT_FOUND),
		MSG_JOBSTEP_DATA_INVALID ("(E%d) Job data invalid (cause: %s)", MSG_ID_JOBSTEP_DATA_INVALID),
		
		// Product class CLI
		MSG_PRODUCTCLASS_DATA_INVALID ("(E%d) Product class data invalid (cause: %s)", MSG_ID_PRODUCTCLASS_DATA_INVALID),
		MSG_NO_PRODUCTCLASSES_FOUND ("(E%d) No product classes found for given search criteria", MSG_ID_NO_PRODUCTCLASSES_FOUND),
		MSG_NO_PRODCLASS_NAME_GIVEN ("(E%d) No product class name given", MSG_ID_NO_PRODCLASS_NAME_GIVEN),
		MSG_PRODUCTCLASS_NOT_FOUND ("(E%d) Product class %s not found", MSG_ID_PRODUCTCLASS_NOT_FOUND),
		MSG_PRODUCTCLASS_NOT_FOUND_BY_ID ("(E%d) Product class with database ID %d not found", MSG_ID_PRODUCTCLASS_NOT_FOUND_BY_ID),
		MSG_PRODUCTCLASS_DELETE_FAILED ("(E%d) Deletion of product class %s failed (cause: %s)", MSG_ID_PRODUCTCLASS_DELETE_FAILED),
		MSG_FILE_NOT_FOUND ("(E%d) Selection rule file %s not found or not readable", MSG_ID_FILE_NOT_FOUND),
		MSG_SELECTION_RULE_DATA_INVALID ("(E%d) Selection rule data invalid (cause: %s)", MSG_ID_SELECTION_RULE_DATA_INVALID),
		MSG_NO_SELECTION_RULES_FOUND ("(E%d) No selection rules found for product class %s", MSG_ID_NO_SELECTION_RULES_FOUND),
		MSG_NO_SELECTION_RULES_FOUND_FOR_SOURCE ("(E%d) No selection rules found for target product class %s and source product class %s", MSG_ID_NO_SELECTION_RULES_FOUND_FOR_SOURCE),
		MSG_INPUT_OUT_OF_BOUNDS ("(E%d) Input %d invalid, please select a number between %d and %d", MSG_ID_INPUT_OUT_OF_BOUNDS),
		MSG_INPUT_NOT_NUMERIC ("(E%d) Input %s not numeric", MSG_ID_INPUT_NOT_NUMERIC),
		MSG_SELECTION_RULE_NOT_FOUND_BY_ID ("(E%d) Selection rule with database ID %d not found", MSG_ID_SELECTION_RULE_NOT_FOUND_BY_ID),
		MSG_NO_RULEID_GIVEN ("(E%d) No selection rule database ID given", MSG_ID_NO_RULEID_GIVEN),
		MSG_RULEID_NOT_NUMERIC ("(E%d) Database ID %d for selection rule not numeric", MSG_ID_RULEID_NOT_NUMERIC),
		MSG_SELECTION_RULE_DELETE_FAILED ("(E%d) Deletion of selection rule with database ID %d from product class %s failed (cause: %s)", MSG_ID_SELECTION_RULE_DELETE_FAILED),
		MSG_INVALID_VISIBILITY ("(E%d) Invalid product visibility %s", MSG_ID_INVALID_VISIBILITY),
		
		// Ingestor/product CLI
		MSG_NO_PRODUCTS_FOUND ("(E%d) No products found for given search criteria", MSG_ID_NO_PRODUCTS_FOUND),
		MSG_INVALID_DATABASE_ID ("(E%d) Database ID %s not numeric", MSG_ID_INVALID_DATABASE_ID),
		MSG_NO_PRODUCT_DBID_GIVEN ("(E%d) No product database ID given", MSG_ID_NO_PRODUCT_DBID_GIVEN),
		MSG_PRODUCT_NOT_FOUND ("(E%d) Product with database ID %d not found", MSG_ID_PRODUCT_NOT_FOUND),
		MSG_INGESTION_FILE_MISSING ("(E%d) No file for product ingestion given", MSG_ID_INGESTION_FILE_MISSING),
		MSG_PROCESSING_FACILITY_MISSING ("(E%d) No processing facility to ingest to given", MSG_ID_PROCESSING_FACILITY_MISSING),
		MSG_PRODUCT_DATA_INVALID ("(E%d) Product data invalid (cause: %s)", MSG_ID_PRODUCT_DATA_INVALID),
		MSG_PRODUCT_ID_OR_FACILITY_MISSING ("(E%d) Product database ID or processing facility missing", MSG_ID_PRODUCT_ID_OR_FACILITY_MISSING),
		MSG_PRODUCTFILE_NOT_FOUND ("(E%d) No product file found for product database ID %d and processing facility %s", MSG_ID_PRODUCTFILE_NOT_FOUND),
		MSG_PRODUCT_HAS_NO_FILES ("(E%d) Product with database ID %d has no files", MSG_ID_PRODUCT_HAS_NO_FILES),
		MSG_PRODUCT_CLASS_MISMATCH ("(E%d) Product with database ID %d is not of requested class %s", MSG_ID_PRODUCT_CLASS_MISMATCH),
		
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
		MSG_CONFIGURATION_DATA_INVALID ("(E%d) Configuration data invalid (cause: %s)", MSG_ID_CONFIGURATION_DATA_INVALID),
		MSG_NO_CONFIGURATIONS_FOUND ("(E%d) No processors found for given search criteria", MSG_ID_NO_CONFIGURATIONS_FOUND),
		MSG_NO_CONFIGURATION_IDENTIFIER_GIVEN ("(E%d) No processor name and/or configuration version given", MSG_ID_NO_CONFIGURATION_IDENTIFIER_GIVEN),
		MSG_CONFIGURATION_NOT_FOUND ("(E%d) Configuration for processor %s with configuration version %s not found", MSG_ID_CONFIGURATION_NOT_FOUND),
		MSG_CONFIGURATION_NOT_FOUND_BY_ID ("(E%d) Configuration with database ID %d not found", MSG_ID_CONFIGURATION_NOT_FOUND_BY_ID),
		MSG_CONFIGURATION_DELETE_FAILED ("(E%d) Deletion of processor %s with version %s failed (cause: %s)", MSG_ID_CONFIGURATION_DELETE_FAILED),
		MSG_CONFIGUREDPROCESSOR_DATA_INVALID ("(E%d) Configuration data invalid (cause: %s)", MSG_ID_CONFIGUREDPROCESSOR_DATA_INVALID),
		MSG_NO_CONFIGUREDPROCESSORS_FOUND ("(E%d) No processors found for given search criteria", MSG_ID_NO_CONFIGUREDPROCESSORS_FOUND),
		MSG_NO_CONFIGUREDPROCESSOR_IDENTIFIER_GIVEN ("(E%d) No configured processor identifier given", MSG_ID_NO_CONFIGUREDPROCESSOR_IDENTIFIER_GIVEN),
		MSG_CONFIGUREDPROCESSOR_NOT_FOUND ("(E%d) Configured processor %s not found", MSG_ID_CONFIGUREDPROCESSOR_NOT_FOUND),
		MSG_CONFIGUREDPROCESSOR_NOT_FOUND_BY_ID ("(E%d) Configured processor with database ID %d not found", MSG_ID_CONFIGUREDPROCESSOR_NOT_FOUND_BY_ID),
		MSG_CONFIGUREDPROCESSOR_DELETE_FAILED ("(E%d) Deletion of configured processor %s failed (cause: %s)", MSG_ID_CONFIGUREDPROCESSOR_DELETE_FAILED),

		// User/group CLI
		MSG_USER_DATA_INVALID ("(E%d) User account data invalid (cause: %s)", MSG_ID_USER_DATA_INVALID),
		MSG_NO_USERS_FOUND ("(E%d) No user accounts found for mission %s", MSG_ID_NO_USERS_FOUND),
		MSG_USER_NOT_FOUND_BY_NAME ("(E%d) User account %s not found for mission %s", MSG_ID_USER_NOT_FOUND_BY_NAME),
		MSG_PASSWORD_MISMATCH ("(E%d) Passwords do not match", MSG_ID_PASSWORD_MISMATCH),
		MSG_NO_USERNAME_GIVEN ("(E%d) No username given", MSG_ID_NO_USERNAME_GIVEN),
		MSG_USER_DELETE_FAILED ("(E%d) Deletion of user account %s failed (cause: %s)", MSG_ID_USER_DELETE_FAILED),
		MSG_NO_AUTHORITIES_GIVEN ("(E%d) No authorities given in command", MSG_ID_NO_AUTHORITIES_GIVEN),
		MSG_NO_GROUPNAME_GIVEN ("(E%d) No group name given", MSG_ID_NO_GROUPNAME_GIVEN),
		MSG_GROUP_NOT_FOUND_BY_ID ("(E%d) Group with database ID %d not found", MSG_ID_GROUP_NOT_FOUND_BY_ID),
		MSG_GROUP_DATA_INVALID ("(E%d) User group data invalid (cause: %s)", MSG_ID_GROUP_DATA_INVALID),
		MSG_NO_GROUPS_FOUND ("(E%d) No user groups found for mission %s", MSG_ID_NO_GROUPS_FOUND),
		MSG_GROUP_NOT_FOUND_BY_NAME ("(E%d) User group %s not found for mission %s", MSG_ID_GROUP_NOT_FOUND_BY_NAME),
		MSG_MISSION_ALREADY_SET ("(E%d) Already logged in to mission %s, use of '--mission' option not allowed", MSG_ID_MISSION_ALREADY_SET),
		MSG_GROUP_DELETE_FAILED ("(E%d) Deletion of user group %s failed (cause: %s)", MSG_ID_GROUP_DELETE_FAILED),
		MSG_NO_USERS_GIVEN ("(E%d) No users given in command", MSG_ID_NO_USERS_GIVEN),
		MSG_NO_USERS_FOUND_IN_GROUP ("(E%d) No user accounts found for user group %s", MSG_ID_NO_USERS_FOUND_IN_GROUP),

		// Facility CLI
		MSG_NO_FACILITIES_FOUND ("(E%d) No processing facilities found", MSG_ID_NO_FACILITIES_FOUND),
		MSG_FACILITY_NOT_FOUND ("(E%d) Processing facility %s not found", MSG_ID_FACILITY_NOT_FOUND),
		MSG_FACILITY_NOT_FOUND_BY_ID ("(E%d) Processing facility with database ID %d not found", MSG_ID_FACILITY_NOT_FOUND_BY_ID),
		MSG_FACILITY_NOT_READABLE ("(E%d) Processing facility %s not readable (cause: %s)", MSG_ID_FACILITY_NOT_READABLE),
		MSG_FACILITY_DATA_INVALID ("(E%d) Processing facility data invalid (cause: %s)", MSG_ID_FACILITY_DATA_INVALID),
		MSG_NO_FACILITY_NAME_GIVEN ("(E%d) No processing facility name given", MSG_ID_NO_FACILITY_NAME_GIVEN),
		MSG_FACILITY_DELETE_FAILED ("(E%d) Deletion of processing facility %s failed (cause: %s)", MSG_ID_FACILITY_DELETE_FAILED),
		
		// CLIUtil
		MSG_INVALID_FILE_TYPE ("(E%d) Invalid file format %s", MSG_ID_INVALID_FILE_TYPE),
		MSG_INVALID_FILE_STRUCTURE ("(E%d) %s content of file %s invalid for object generation (cause: %s)", MSG_ID_INVALID_FILE_STRUCTURE),
		MSG_INVALID_FILE_SYNTAX ("(E%d) File %s contains invalid %s content (cause: %s)", MSG_ID_INVALID_FILE_SYNTAX),
		MSG_INVALID_ATTRIBUTE_NAME ("(E%d) Invalid attribute name %s", MSG_ID_INVALID_ATTRIBUTE_NAME),
		MSG_INVALID_ATTRIBUTE_TYPE ("(E%d) Attribute %s cannot be converted to type %s", MSG_ID_INVALID_ATTRIBUTE_TYPE),
		MSG_REFLECTION_EXCEPTION ("(E%d) Reflection exception setting attribute %s (cause: %s)", MSG_ID_REFLECTION_EXCEPTION),
		MSG_GENERATION_EXCEPTION ("(E%d) Write exception serializing object %s to format %s (cause: %s)", MSG_ID_GENERATION_EXCEPTION),
		MSG_MAPPING_EXCEPTION ("(E%d) Exception mapping object %s to format %s (cause: %s)", MSG_ID_MAPPING_EXCEPTION),
		
		// CLI Parser
		MSG_ILLEGAL_PARAMETER_TYPE ("(E%d) Illegal parameter type %s, expected one of %s", MSG_ID_ILLEGAL_PARAMETER_TYPE),
		MSG_ILLEGAL_OPTION_TYPE ("(E%d) Illegal option type %s, expected one of %s", MSG_ID_ILLEGAL_OPTION_TYPE),
		MSG_INVALID_COMMAND_OPTION ("(E%d) Invalid command option %s found", MSG_ID_INVALID_COMMAND_OPTION),
		MSG_OPTION_NOT_ALLOWED ("(E%d) Option %s not allowed after command parameter", MSG_ID_OPTION_NOT_ALLOWED),
		MSG_ILLEGAL_OPTION ("(E%d) Option %s not allowed for command %s", MSG_ID_ILLEGAL_OPTION),
		MSG_ILLEGAL_OPTION_VALUE ("(E%d) Illegal option value %s for option %s of type %s", MSG_ID_ILLEGAL_OPTION_VALUE),
		MSG_TOO_MANY_PARAMETERS ("(E%d) Too many parameters for command %s", MSG_ID_TOO_MANY_PARAMETERS),
		MSG_ATTRIBUTE_PARAMETER_EXPECTED ("(E%d) Parameter of format '<attribute name>=<attribute value>' expected at position %d for command %s", MSG_ID_ATTRIBUTE_PARAMETER_EXPECTED),
		MSG_PARAMETER_TYPE_MISMATCH ("(E%d) Parameter of type %s expected at position %d for command %s", MSG_ID_PARAMETER_TYPE_MISMATCH),
		MSG_ILLEGAL_COMMAND ("(E%d) Illegal command %s", MSG_ID_ILLEGAL_COMMAND),
		MSG_ILLEGAL_SUBCOMMAND ("(E%d) Illegal subcommand %s", MSG_ID_ILLEGAL_SUBCOMMAND),
		MSG_PARAMETER_MISSING ("(E%d) Required parameter %s not found for command %s", MSG_ID_PARAMETER_MISSING),
		
		// CLI Main
		MSG_SYNTAX_FILE_NOT_FOUND ("(E%d) Syntax file %s not found", MSG_ID_SYNTAX_FILE_NOT_FOUND),
		MSG_SYNTAX_FILE_ERROR ("(E%d) Parsing error in syntax file %s (cause: %s)", MSG_ID_SYNTAX_FILE_ERROR),
		MSG_COMMAND_LINE_PROMPT_SUPPRESSED ("(I%d) Command line prompt suppressed by proseo.cli.start parameter", MSG_ID_COMMAND_LINE_PROMPT_SUPPRESSED),
		MSG_COMMAND_NAME_NULL ("(E%d) Command name must not be null", MSG_ID_COMMAND_NAME_NULL),
		MSG_PASSWORD_MISSING ("(E%d) No password given for user %s", MSG_ID_PASSWORD_MISSING),
		MSG_UNCAUGHT_EXCEPTION ("(E%d) prosEO Command Line Interface terminated by exception: %s", MSG_ID_UNCAUGHT_EXCEPTION),
		MSG_USER_INTERRUPT("(E%d) prosEO Command Line Interface exiting due to user interrupt", MSG_ID_USER_INTERRUPT),
		MSG_CREDENTIALS_UNSAFE ("(E%d) Credential file %s ignored, because it is unsafe (group or world readable)", MSG_ID_CREDENTIALS_UNSAFE),
		MSG_CREDENTIALS_NOT_FOUND ("(E%d) Credential file %s not found", MSG_ID_CREDENTIALS_NOT_FOUND),
		MSG_CREDENTIALS_NOT_READABLE ("(E%d) Credential file %s not readable (cause: %s)", MSG_ID_CREDENTIALS_NOT_READABLE),
		
		/* --- Info messages -- */
		// General
		MSG_OPERATION_CANCELLED ("(I%d) Operation cancelled", MSG_ID_OPERATION_CANCELLED),
		MSG_NOT_MODIFIED ("(I%d) Data not modified", MSG_ID_NOT_MODIFIED),
		
		// Login Manager
		MSG_LOGGED_IN ("(I%d) User %s logged in", MSG_ID_LOGGED_IN),
		MSG_LOGGED_OUT ("(I%d) User %s logged out", MSG_ID_LOGGED_OUT),
		MSG_LOGIN_CANCELLED ("(I%d) No username given, login cancelled", MSG_ID_LOGIN_CANCELLED),
		
		// CLI Parser
		MSG_SYNTAX_LOADED ("(I%d) Command line syntax loaded from syntax file %s", MSG_ID_SYNTAX_LOADED),

		// CLI Main
		MSG_END_OF_FILE("(I%d) End of input reached, prosEO Command Line Interface terminates", MSG_ID_END_OF_FILE),
		MSG_CLI_TERMINATED("(I%d) 'exit' command received, prosEO Command Line Interface terminates", MSG_ID_CLI_TERMINATED),
		
		// Mission CLI
		MSG_MISSION_CREATED ("(I%d) Mission %s created (database ID %d)", MSG_ID_MISSION_CREATED),
		MSG_MISSION_UPDATED ("(I%d) Mission %s updated (new version %d)", MSG_ID_MISSION_UPDATED),
		MSG_MISSION_DELETED ("(I%d) Mission %s deleted", MSG_ID_MISSION_DELETED),
		MSG_SPACECRAFT_ADDED ("(I%d) Spacecraft %s added (database ID %d)", MSG_ID_SPACECRAFT_ADDED),
		MSG_SPACECRAFT_REMOVED ("(I%d) Spacecraft %s removed from mission %s", MSG_ID_SPACECRAFT_REMOVED),
		MSG_ORBITS_CREATED ("(I%d) %d orbits created", MSG_ID_ORBITS_CREATED),
		MSG_ORBITS_UPDATED ("(I%d) %d orbits updated", MSG_ID_ORBITS_UPDATED),
		MSG_ORBITS_DELETED ("(I%d) %d orbits deleted", MSG_ID_ORBITS_DELETED),
	
		// Order CLI
		MSG_ORDER_CREATED ("(I%d) Order with identifier %s created (database ID %d)", MSG_ID_ORDER_CREATED),
		MSG_ORDER_UPDATED ("(I%d) Order with identifier %s updated (new version %d)", MSG_ID_ORDER_UPDATED),
		MSG_ORDER_APPROVED ("(I%d) Order with identifier %s approved (new version %d)", MSG_ID_ORDER_APPROVED),
		MSG_ORDER_PLANNED ("(I%d) Order with identifier %s planned (new version %d)", MSG_ID_ORDER_PLANNED),
		MSG_ORDER_RELEASED ("(I%d) Order with identifier %s released (new version %d)", MSG_ID_ORDER_RELEASED),
		MSG_ORDER_SUSPENDED ("(I%d) Order with identifier %s suspended (new version %d)", MSG_ID_ORDER_SUSPENDED),
		MSG_ORDER_CANCELLED ("(I%d) Order with identifier %s cancelled (new version %d)", MSG_ID_ORDER_CANCELLED),
		MSG_ORDER_RESET ("(I%d) Order with identifier %s reset (new version %d)", MSG_ID_ORDER_RESET),
		MSG_ORDER_DELETED ("(I%d) Order with identifier %s deleted", MSG_ID_ORDER_DELETED),
		MSG_RETRYING_ORDER ("(I%d) Retrying order with identifier %s (new version %d)", MSG_ID_RETRYING_ORDER),
		
		// Job and job step CLI
		MSG_JOB_SUSPENDED ("(I%d) Job with database ID %s suspended (new version %d)", MSG_ID_JOB_SUSPENDED),
		MSG_JOB_RESUMED ("(I%d) Job with database ID %s resumed (new version %d)", MSG_ID_JOB_RESUMED),
		MSG_JOB_CANCELLED ("(I%d) Job with database ID %s cancelled (new version %d)", MSG_ID_JOB_CANCELLED),
		MSG_RETRYING_JOB ("(I%d) Retrying job with database ID %s (new version %d)", MSG_ID_RETRYING_JOB),
		MSG_JOBSTEP_SUSPENDED ("(I%d) Job step with database ID %s suspended (new version %d)", MSG_ID_JOBSTEP_SUSPENDED),
		MSG_JOBSTEP_RESUMED ("(I%d) Job step with database ID %s resumed (new version %d)", MSG_ID_JOBSTEP_RESUMED),
		MSG_JOBSTEP_CANCELLED ("(I%d) Job step with database ID %s cancelled (new version %d)", MSG_ID_JOBSTEP_CANCELLED),
		MSG_RETRYING_JOBSTEP ("(I%d) Retrying job step with database ID %s (new version %d)", MSG_ID_RETRYING_JOBSTEP),
		
		// Product class CLI
		MSG_PRODUCTCLASS_CREATED ("(I%d) Product class %s created (database ID %d)", MSG_ID_PRODUCTCLASS_CREATED),
		MSG_PRODUCTCLASS_UPDATED ("(I%d) Product class with database ID %d updated (new version %d)", MSG_ID_PRODUCTCLASS_UPDATED),
		MSG_PRODUCTCLASS_DELETED ("(I%d) Product class with database ID %d deleted", MSG_ID_PRODUCTCLASS_DELETED),
		MSG_SELECTION_RULES_CREATED ("(I%d) %d selection rules created for product class %s", MSG_ID_SELECTION_RULES_CREATED),
		MSG_SELECTION_RULE_UPDATED ("(I%d) Selection rule with database ID %d updated (new version %d)", MSG_ID_SELECTION_RULE_UPDATED),
		MSG_SELECTION_RULE_DELETED ("(I%d) Selection rule with database ID %d deleted", MSG_ID_SELECTION_RULE_DELETED),
		
		// Ingestor/product CLI
		MSG_PRODUCT_CREATED ("(I%d) Product of class %s created (database ID %d, UUID %s)", MSG_ID_PRODUCT_CREATED),
		MSG_PRODUCT_UPDATED ("(I%d) Product with database ID %d updated (new version %d)", MSG_ID_PRODUCT_UPDATED),
		MSG_PRODUCT_DELETED ("(I%d) Product with database ID %d deleted", MSG_ID_PRODUCT_DELETED),
		MSG_PRODUCTS_INGESTED ("(I%d) %d products ingested to processing facility %s", MSG_ID_PRODUCTS_INGESTED),
		MSG_PRODUCTFILE_DELETED ("(I%d) Product file for product database ID %d and processing facility %s deleted", MSG_ID_PRODUCTFILE_DELETED),

		// User/group CLI
		MSG_USER_CREATED ("(I%d) User account %s created", MSG_ID_USER_CREATED),
		MSG_USER_UPDATED ("(I%d) User account %s updated", MSG_ID_USER_UPDATED),
		MSG_USER_DELETED ("(I%d) User account %s deleted", MSG_ID_USER_DELETED),
		MSG_USER_ENABLED ("(I%d) User account %s enabled", MSG_ID_USER_ENABLED),
		MSG_USER_DISABLED ("(I%d) User account %s disabled", MSG_ID_USER_DISABLED),
		MSG_AUTHORITIES_GRANTED ("(I%d) Authorities %s granted to user %s", MSG_ID_AUTHORITIES_GRANTED),
		MSG_AUTHORITIES_REVOKED ("(I%d) Authorities %s revoked from user %s", MSG_ID_AUTHORITIES_REVOKED),
		MSG_GROUP_CREATED ("(I%d) User group %s created", MSG_ID_GROUP_CREATED),
		MSG_GROUP_UPDATED ("(I%d) User group %s updated", MSG_ID_GROUP_UPDATED),
		MSG_GROUP_DELETED ("(I%d) User group %s deleted", MSG_ID_GROUP_DELETED),
		MSG_GROUP_AUTHORITIES_GRANTED ("(I%d) Authorities %s granted to group %s", MSG_ID_GROUP_AUTHORITIES_GRANTED),
		MSG_GROUP_AUTHORITIES_REVOKED ("(I%d) Authorities %s revoked from group %s", MSG_ID_GROUP_AUTHORITIES_REVOKED),
		MSG_USERS_ADDED ("(I%d) Users %s added to group %s", MSG_ID_USERS_ADDED),
		MSG_USERS_REMOVED ("(I%d) Users %s removed from group %s", MSG_ID_USERS_REMOVED),
		
		// Facility CLI
		MSG_FACILITY_CREATED ("(I%d) Processing facility %s created (database ID %d)", MSG_ID_FACILITY_CREATED),
		MSG_FACILITY_UPDATED ("(I%d) Processing facility %s updated (database ID %d)", MSG_ID_FACILITY_UPDATED),
		MSG_FACILITY_DELETED ("(I%d) Processing facility %s deleted (database ID %d)", MSG_ID_FACILITY_DELETED),
		
		// Processor CLI
		MSG_PROCESSORCLASS_CREATED ("(I%d) Processor class %s created (database ID %d)", MSG_ID_PROCESSORCLASS_CREATED),
		MSG_PROCESSORCLASS_UPDATED ("(I%d) Processor class with database ID %d updated (new version %d)", MSG_ID_PROCESSORCLASS_UPDATED),
		MSG_PROCESSORCLASS_DELETED ("(I%d) Processor class with database ID %d deleted", MSG_ID_PROCESSORCLASS_DELETED),
		MSG_PROCESSOR_CREATED ("(I%d) Processor %s with version %s created (database ID %d)", MSG_ID_PROCESSOR_CREATED),
		MSG_PROCESSOR_UPDATED ("(I%d) Processor with database ID %d updated (new version %d)", MSG_ID_PROCESSOR_UPDATED),
		MSG_PROCESSOR_DELETED ("(I%d) Processor with database ID %d deleted", MSG_ID_PROCESSOR_DELETED),
		MSG_CONFIGURATION_CREATED ("(I%d) Configuration %s with version %s created (database ID %d)", MSG_ID_CONFIGURATION_CREATED),
		MSG_CONFIGURATION_UPDATED ("(I%d) Configuration with database ID %d updated (new version %d)", MSG_ID_CONFIGURATION_UPDATED),
		MSG_CONFIGURATION_DELETED ("(I%d) Configuration with database ID %d deleted", MSG_ID_CONFIGURATION_DELETED),
		MSG_CONFIGUREDPROCESSOR_CREATED ("(I%d) Configured processor %s for processor %s, version %s and configuration version %s created (database ID %d)", MSG_ID_CONFIGUREDPROCESSOR_CREATED),
		MSG_CONFIGUREDPROCESSOR_UPDATED ("(I%d) Configured processor with database ID %d updated (new version %d)", MSG_ID_CONFIGUREDPROCESSOR_UPDATED),
		MSG_CONFIGUREDPROCESSOR_DELETED ("(I%d) Configured processor with database ID %d deleted", MSG_ID_CONFIGUREDPROCESSOR_DELETED);
		
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
	 * 
	 * @param messageId the ID of the message to return
	 * @param messageParameters an arbitrary number of parameters for the message template
	 * @return a fully formatted message
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
