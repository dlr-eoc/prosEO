/**
 * PlannerMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the planner.
 *
 * @author Katharina Bassler
 */
public enum PlannerMessage implements ProseoMessage {
	
	ORDERS_RETRIEVED					(3000, Level.INFO, true, "List of processing orders retrieved", ""), 
	ORDER_RETRIEVED						(3001, Level.INFO, true, "Processing order '{0}' retrieved", ""), 
	ORDER_APPROVED						(3002, Level.INFO, true, "Processing order '{0}' is approved", ""), 
	ORDER_PLANNING						(3003, Level.INFO, true, "Processing order '{0}' is planning", ""), 
	ORDER_PLANNED						(3004, Level.INFO, true, "Processing order '{0}' is planned", ""), 
	ORDER_RELEASING						(3005, Level.INFO, true, "Processing order '{0}' is releasing", ""), 
	ORDER_RELEASED						(3006, Level.INFO, true, "Processing order '{0}' is released", ""), 
	ORDER_RUNNING						(3007, Level.INFO, true, "Processing order '{0}' is running", ""), 
	ORDER_SUSPENDED						(3008, Level.INFO, true, "Processing order '{0}' is suspended", ""), 
	ORDER_SUSPEND_PREPARED				(3009, Level.INFO, true, "Processing order '{0}' is prepared for suspend", ""), 
	ORDER_COULD_NOT_INTERRUPT			(3010, Level.ERROR, false, "Processing order '{0}' could not onterrpt release thread", ""), 
	ORDER_CANCELED						(3011, Level.INFO, true, "Processing order '{0}' is canceled", ""), 
	ORDER_RESET							(3012, Level.INFO, true, "Processing order '{0}' is reset", ""), 
	ORDER_DELETED						(3013, Level.INFO, true, "Processing order '{0}' deleted", ""), 
	ORDER_CLOSED						(3014, Level.INFO, true, "Processing order '{0}' is closed", ""), 
	ORDER_SAVE  						(3015, Level.INFO, true, "Saving processing order '{0}'", ""), 
	ORDER_COMPLETED						(3016, Level.INFO, true, "Processing order '{0}' is completed", ""), 
	ORDER_PLANNING_INTERRUPTED			(3017, Level.INFO, true, "Processing order planning thread '{0}' ({1}) is interrupted", ""), 
	ORDER_PLANNING_EXCEPTION			(3018, Level.ERROR, false, "Processing order planning thread '{0}' ({1}) with exception", ""), 
	ORDER_PLANNING_FAILED				(3019, Level.ERROR, false, "Processing order planning thread '{0}' ({1}) is failed", ""), 
	ORDER_RELEASING_INTERRUPTED			(3020, Level.INFO, true, "Processing order releasing thread '{0}' ({1}) is interrupted", ""), 
	ORDER_RELEASING_EXCEPTION			(3021, Level.ERROR, false, "Processing order releasing thread '{0}' ({1}) with exception", ""), 
	ORDER_RETRIED						(3022, Level.WARN, true, "Processing order '{0}' is retried", ""), 
	ORDER_HASTOBE_APPROVED				(3023, Level.ERROR, false, "Processing order '{0}' has to be approved", ""), 
	ORDER_ALREADY_APPROVED				(3024, Level.ERROR, false, "Processing order '{0}' already approved", ""), 
	ORDER_HASTOBE_PLANNED				(3025, Level.ERROR, false, "Processing order '{0}' has to be planned", ""), 
	ORDER_ALREADY_PLANNED				(3026, Level.ERROR, false, "Processing order '{0}' already planned", ""), 
	ORDER_HASTOBE_RELEASED				(3027, Level.ERROR, false, "Processing order '{0}' has to be released", ""), 
	ORDER_ALREADY_RELEASING				(3028, Level.ERROR, false, "Processing order '{0}' already releasing", ""), 
	ORDER_ALREADY_RELEASED				(3029, Level.ERROR, false, "Processing order '{0}' already released", ""), 
	ORDER_ALREADY_RUNNING				(3030, Level.ERROR, false, "Processing order '{0}' already running", ""), 
	ORDER_ALREADY_SUSPENDING			(3031, Level.ERROR, false, "Processing order '{0}' already suspending", ""), 
	ORDER_ALREADY_COMPLETED				(3032, Level.ERROR, false, "Processing order '{0}' already completed", ""), 
	ORDER_PRODUCT_EXIST 				(3033, Level.INFO, true, "Processing order '{0}' requested product already exist", ""), 
	ORDER_ALREADY_FAILED				(3034, Level.WARN, false, "Processing order '{0}' already failed", ""), 
	ORDER_ALREADY_CLOSED				(3035, Level.WARN, false, "Processing order '{0}' already closed", ""), 
	ORDER_COULD_NOT_RETRY				(3036, Level.ERROR, false, "Processing order '{0}' has to be in state FAILED to retry", ""), 
	ORDER_HASTOBE_FINISHED				(3037, Level.ERROR, false, "Processing order '{0}' has to be finished (completed or failed)", ""), 
	ORDER_NOT_EXIST						(3038, Level.ERROR, false, "Processing order '{0}' does not exist in the current mission", ""),
	ORDER_FACILITY_NOT_EXIST			(3039, Level.ERROR, false, "Processing order '{0}' and processing facility '{1}' does not exist", ""),
	ORDER_SLICING_TYPE_NOT_SET			(3040, Level.ERROR, false, "Processing order '{0}' slicing type not set", ""),
	ORDER_MISSION_NOT_SET				(3041, Level.ERROR, false, "Processing order '{0}' mission not set", ""),
	ORDER_REQ_PROC_NOT_SET				(3042, Level.ERROR, false, "Processing order '{0}' requested processor(s) not set", ""),
	ORDER_REQ_ORBIT_NOT_SET				(3043, Level.ERROR, false, "Processing order '{0}' requested orbit(s) not set", ""),
	ORDER_REQ_DAY_NOT_SET				(3044, Level.ERROR, false, "Processing order '{0}' requested calendar day not set", ""),
	ORDER_REQ_TIMESLICE_NOT_SET			(3045, Level.ERROR, false, "Processing order '{0}' requested time slice not set", ""),
	ORDER_REQ_ORBIT_OR_TIME_NOT_SET		(3046, Level.ERROR, false, "Processing order '{0}' requested orbit or start/stop time not set", ""),
	ORDERDISP_NO_CONF_PROC				(3047, Level.ERROR, false, "OrderDispatcher: no configured processor found for product class '{0}'", ""),
	ORDER_REQ_CON_PROC_NOT_SET			(3048, Level.ERROR, false, "Processing order '{0}' requested configured processor not set", ""),
	ORDER_REQ_PROD_CLASS_NOT_SET		(3049, Level.ERROR, false, "Processing order '{0}' requested product class(es) not set", ""),
	ORDER_WAIT_FOR_RELEASE				(3050, Level.INFO, true, "Processing order '{0}' has state '{1}', wait for release", ""),
	ORDER_NOTHING_TO_PUBLISH			(3051, Level.INFO, true, "Processing order '{0}' has state '{1}', nothing to publish", ""),
	FACILITY_NOT_EXIST					(3052, Level.ERROR, false, "Processing facility '{0}' does not exist", ""),
	FACILITY_NOT_DEFINED				(3053, Level.ERROR, false, "No processing facility defined", ""),
	JOBS_RETRIEVED						(3054, Level.INFO, true, "Jobs for processing order '{0}' retrieved", ""), 
	JOBCOUNT_RETRIEVED					(3055, Level.INFO, true, "Job count for processing order '{0}' retrieved", ""), 
	JOBGRAPH_RETRIEVED					(3056, Level.INFO, true, "Dependency graph for job '{0}' retrieved", ""), 
	JOB_RETRIEVED						(3057, Level.INFO, true, "Job '{0}' retrieved", ""), 
	JOB_RELEASED						(3058, Level.INFO, true, "Job '{0}' is released", ""), 
	JOB_CANCELED						(3059, Level.INFO, true, "Job '{0}' is cancelled", ""), 
	JOB_DELETED 						(3060, Level.INFO, true, "Job '{0}' is deleted", ""), 
	JOB_SUSPENDED						(3061, Level.INFO, true, "Job '{0}' is suspended", ""), 
	JOB_HOLD							(3062, Level.INFO, true, "Job '{0}' is on hold", ""), 
	JOB_PLANNED							(3063, Level.INFO, true, "Job '{0}' is planned", ""), 
	JOB_INITIAL							(3064, Level.INFO, true, "Job '{0}' is initial", ""), 
	JOB_RETRIED							(3065, Level.INFO, true, "Job '{0}' set to inital", ""), 
	JOB_STARTED							(3066, Level.INFO, true, "Job '{0}' is started", ""), 
	JOB_COMPLETED						(3067, Level.INFO, true, "Job '{0}' is completed", ""), 
	JOB_CLOSED							(3068, Level.INFO, true, "Job '{0}' is closed", ""), 
	JOB_ALREADY_RELEASED				(3069, Level.WARN, false, "Job '{0}' is already released", ""), 
	JOB_HASTOBE_RELEASED				(3070, Level.ERROR, false, "Job '{0}' has to be released", ""), 
	JOB_HASTOBE_PLANNED					(3071, Level.ERROR, false, "Job '{0}' has to be planned", ""), 
	JOB_ALREADY_EXIST					(3072, Level.WARN, false, "Job '{0}' already exist", ""), 
	JOB_ALREADY_HOLD					(3073, Level.WARN, false, "Job '{0}' is on hold", ""), 
	JOB_ALREADY_STARTED					(3074, Level.WARN, false, "Job '{0}' is already started", ""), 
	JOB_ALREADY_COMPLETED				(3075, Level.WARN, false, "Job '{0}' already completed", ""), 
	JOB_ALREADY_FAILED					(3076, Level.WARN, false, "Job '{0}' already failed", ""), 
	JOB_ALREADY_CLOSED					(3077, Level.WARN, false, "Job '{0}' already closed", ""), 
	JOB_COULD_NOT_RETRY					(3078, Level.ERROR, false, "Job '{0}' has to be in state FAILED to retry", ""), 
	JOB_COULD_NOT_CLOSE					(3079, Level.ERROR, false, "Job '{0}' has to be in state COMPLETED or FAILED to close", ""), 
	JOB_NOT_EXIST						(3080, Level.ERROR, false, "Job '{0}' does not exist", ""),
	JOBSTEPS_RETRIEVED					(3081, Level.INFO, true, "Job steps of status {0} retrieved for mission {1}", ""), 
	JOBSTEP_RETRIEVED					(3082, Level.INFO, true, "Job step '{0}' retrieved", ""), 
	JOBSTEP_WAITING						(3083, Level.INFO, true, "Job step '{0}' is waiting for input", ""), 
	JOBSTEP_READY						(3084, Level.INFO, true, "Job step '{0}' is ready to run", ""), 
	JOBSTEP_CANCELED					(3085, Level.INFO, true, "Job step '{0}' is canceled", ""),  
	JOBSTEP_CLOSED						(3086, Level.INFO, true, "Job step '{0}' is closed", ""), 
	JOBSTEP_DELETED 					(3087, Level.INFO, true, "Job step '{0}' is deleted", ""), 
	JOBSTEP_SPQ_DELETED 				(3088, Level.INFO, true, "Satisfied product queries of job step '{0}' deleted", ""), 
	JOBSTEP_SUSPENDED					(3089, Level.INFO, true, "Job step '{0}' is suspended", ""), 
	JOBSTEP_STARTED 					(3090, Level.INFO, true, "Job step '{0}' is started", ""), 
	JOBSTEP_RETRIED						(3091, Level.INFO, true, "Job step '{0}' set to inital", ""), 
	JOBSTEP_RETRIED_COMPLETED			(3092, Level.INFO, true, "Job step '{0}' set to completed cause product exists", ""), 
	JOBSTEP_FAILED						(3093, Level.INFO, true, "Job step '{0}' is failed", ""), 
	JOBSTEP_COMPLETED					(3094, Level.INFO, true, "Job step '{0}' is completed", ""), 
	JOBSTEP_ALREADY_RUNNING				(3095, Level.WARN, false, "Job step '{0}' is already running", ""), 
	JOBSTEP_ALREADY_COMPLETED			(3096, Level.WARN, false, "Job step '{0}' already completed", ""), 
	JOBSTEP_ALREADY_CLOSED				(3097, Level.WARN, false, "Job step '{0}' already closed", ""), 
	JOBSTEP_ALREADY_FAILED				(3098, Level.WARN, false, "Job step '{0}' already failed", ""), 
	JOBSTEP_COULD_NOT_RETRY				(3099, Level.ERROR, false, "Job step '{0}' has to be in state FAILED to retry", ""), 
	JOBSTEP_COULD_NOT_CLOSE				(3100, Level.ERROR, false, "Job step '{0}' has to be in state COMPLETED or FAILED to close", ""), 
	JOBSTEP_NOT_EXIST					(3101, Level.ERROR, false, "Job step '{0}' does not exist", ""),
	JOF_DELETED							(3102, Level.INFO, true, "Job Order File '{0}' deleted", ""), 
	JOF_DELETING_ERROR					(3103, Level.ERROR, false, "Error deleting Job Order File '{0}' from processing facility '{1}' (cause: {1})", ""),
	JOBS_FOR_ORDER_NOT_EXIST			(3104, Level.ERROR, false, "Job(s) for processing order '{0}' do not exist", ""),
	PARAM_ID_FACILITY_NOT_SET			(3105, Level.ERROR, false, "Parameter id and facility are not set", ""),
	PARAM_FACILITY_NOT_SET				(3106, Level.ERROR, false, "Parameter facility is not set", ""),
	PARAM_ID_NOT_SET					(3107, Level.ERROR, false, "Parameter id is not set", ""),
	PLANNER_FACILITY_CONNECTED 			(3108, Level.INFO, true, "ProcessingFacility '{0}' connected to '{1}'", ""),
	PLANNER_FACILITY_WORKER_CNT			(3109, Level.INFO, true, "{0} worker nodes found", ""),
	PLANNER_FACILITY_DISCONNECTED		(3110, Level.INFO, true, "ProcessingFacility '{0}' disconnected", ""),
	PLANNER_FACILITY_NOT_CONNECTED		(3111, Level.ERROR, false, "ProcessingFacility '{0}' could not be connected to '{1}'", ""),
	PLANNER_AUTH_DATASOURCE				(3112, Level.INFO, true, "Initializing authentication from datasource '{0}'", ""),
	KUBEDISPATCHER_CONFIG_NOT_SET		(3113, Level.ERROR, false, "KubeDispatcherRunOnce: KubeConfig not set", ""),
	KUBEDISPATCHER_RUN_ONCE				(3114, Level.INFO, true, "KubeDispatcher run once and finish", ""),
	KUBEDISPATCHER_CYCLE				(3115, Level.INFO, true, "KubeDispatcher cycle started", ""),
	KUBEDISPATCHER_SLEEP				(3116, Level.INFO, true, "KubeDispatcher cycle completed, sleeping for {0} ms", ""),
	KUBEDISPATCHER_INTERRUPT			(3117, Level.INFO, true, "KubeDispatcher interrupt", ""),
	KUBEDISPATCHER_PLANNER_NOT_SET		(3118, Level.ERROR, false, "KubeDispatcher: Production planner not set", ""),
	KUBECONFIG_JOB_NOT_FOUND			(3119, Level.ERROR, false, "Job '{0}' not found, is it already finished?", ""),
	KUBEJOB_CREATED						(3120, Level.INFO, true, "Kubernetes job '{0}/{1}' created", ""),
	KUBEJOB_FINISHED					(3121, Level.INFO, true, "Kubernetes job '{0}/{1}' finished", ""),
	KUBEJOB_FINISH_TRIGGERED			(3122, Level.INFO, true, "Finishing of Kubernetes job '{0}/{1}' triggered", ""),
	KUBERNETES_NOT_CONNECTED     	 	(3123, Level.ERROR, false, "Kubernetes configuration {0} not connected", ""),
	JOB_STEP_NOT_FOUND              	(3124, Level.ERROR, false, "No job step found for id {0}", ""),
	CONFIG_PROC_DISABLED           		(3125, Level.WARN, false, "Configured processor {0} is disabled", ""), 
	PLANNING_CHECK_COMPLETE				(3126, Level.INFO, true, "Planning check complete for product with ID {0}", ""),
	PLANNING_CHECK_FAILED				(3127, Level.ERROR, false, "Planning check failed for product with ID {0} (cause: {1})", ""),
	PLANNING_INTERRUPTED				(3128, Level.WARN, false, "Planning order {0} interrupted", ""),
	INSUFFICIENT_ORDER_DATA				(3129, Level.ERROR, false, "Insufficient data for sending job order to Storage Manager", ""),
	HTTP_REQUEST						(3130, Level.INFO, true, "HTTP-Request: {0}", ""),
	HTTP_RESPONSE						(3131, Level.INFO, true, "... response is {0}", ""),
	SENDING_JOB_EXCEPTION				(3132, Level.ERROR, false, "Exception sending job order to Storage Manager: {0}", ""),
	JOB_CREATION_FAILED					(3133, Level.ERROR, false, "Job creation failed with exception: ", ""),
	FACILITY_CONNECTION_FAILED			(3134, Level.ERROR, false, "Could not connect with facility {0}", ""),
	CONFIGURATION_ACCESS_FAILED			(3135, Level.INFO, true, "Cannot access Kubernetes Configuration file: {0}", ""),
	MALFORMED_HOST_ALIAS				(3136, Level.WARN, true, "Found malformed host alias parameter {0}", ""),
	JOB_CREATED							(3137, Level.INFO, true, "Job {0} created with status {1}", ""),
	JOB_STEP_CREATION_FAILED			(3138, Level.ERROR, false, "Creation of job order for job step {0} failed", ""),
	JOB_STEP_CREATION_FAILED_EXCEPTION	(3139, Level.ERROR, false, "Creation of job order for job step {0} failed with exception {1}", ""),
	SENDING_JOB_STEP_FAILED				(3140, Level.ERROR, false, "Sending of job order to Storage Manager failed for job step {0}", ""),
	JOB_STEP_CREATION_EXCEPTION			(3141, Level.ERROR, false, "General exception creating job for job step {0}: {1}", ""),
	KUBERNETES_API_EXCEPTION			(3142, Level.ERROR, false, "Kubernetes API exception creating job for job step {0}: {1}" + "\n" +
			"  Status code: {2}" + "\n" +
			"  Reason: {3}" + "\n" +
			"  Response headers: {4}", ""),
	NO_INPUT_QUERIES					(3143, Level.WARN, true, "Job Step '{}' has no input product queries", "");
	
	;

	private final int code;
	private final Level level;
	private final boolean success;
	private final String message;
	private final String description;

	private PlannerMessage(int code, Level level, boolean success, String message, String description) {
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

	/**
	 * Get the message's success.
	 * 
	 * @return The message's success.
	 */
	public boolean getSuccess() {
		return success;
	}

}
