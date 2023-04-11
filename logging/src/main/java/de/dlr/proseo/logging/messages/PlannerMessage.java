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
	
	ORDERS_RETRIEVED					(4000, Level.INFO, true, "List of processing orders retrieved", ""), 
	ORDER_RETRIEVED						(4001, Level.INFO, true, "Processing order ''{0}'' retrieved", ""), 
	ORDER_APPROVED						(4002, Level.INFO, true, "Processing order ''{0}'' is approved", ""), 
	ORDER_PLANNING						(4003, Level.INFO, true, "Processing order ''{0}'' is planning", ""), 
	ORDER_PLANNED						(4004, Level.INFO, true, "Processing order ''{0}'' is planned", ""), 
	ORDER_RELEASING						(4005, Level.INFO, true, "Processing order ''{0}'' is releasing", ""), 
	ORDER_RELEASED						(4006, Level.INFO, true, "Processing order ''{0}'' is released", ""), 
	ORDER_RUNNING						(4007, Level.INFO, true, "Processing order ''{0}'' is running", ""), 
	ORDER_SUSPENDED						(4008, Level.INFO, true, "Processing order ''{0}'' is suspended", ""), 
	ORDER_SUSPEND_PREPARED				(4009, Level.INFO, true, "Processing order ''{0}'' is prepared for suspend", ""), 
	ORDER_COULD_NOT_INTERRUPT			(4010, Level.ERROR, false, "Processing order ''{0}'' could not onterrpt release thread", ""), 
	ORDER_CANCELED						(4011, Level.INFO, true, "Processing order ''{0}'' is canceled", ""), 
	ORDER_RESET							(4012, Level.INFO, true, "Processing order ''{0}'' is reset", ""), 
	ORDER_DELETED						(4013, Level.INFO, true, "Processing order ''{0}'' deleted", ""), 
	ORDER_CLOSED						(4014, Level.INFO, true, "Processing order ''{0}'' is closed", ""), 
	ORDER_SAVE  						(4015, Level.INFO, true, "Saving processing order ''{0}''", ""), 
	ORDER_COMPLETED						(4016, Level.INFO, true, "Processing order ''{0}'' is completed", ""), 
	ORDER_PLANNING_INTERRUPTED			(4017, Level.INFO, true, "Processing order planning thread ''{0}'' ({1}) is interrupted", ""), 
	ORDER_PLANNING_EXCEPTION			(4018, Level.ERROR, false, "Processing order planning thread ''{0}'' ({1}) with exception", ""), 
	ORDER_PLANNING_FAILED				(4019, Level.ERROR, false, "Processing order planning thread ''{0}'' ({1}) is failed", ""), 
	ORDER_RELEASING_INTERRUPTED			(4020, Level.INFO, true, "Processing order releasing thread ''{0}'' ({1}) is interrupted", ""), 
	ORDER_RELEASING_EXCEPTION			(4021, Level.ERROR, false, "Processing order releasing thread ''{0}'' ({1}) with exception", ""), 
	ORDER_RETRIED						(4022, Level.WARN, true, "Processing order ''{0}'' is retried", ""), 
	ORDER_HASTOBE_APPROVED				(4023, Level.ERROR, false, "Processing order ''{0}'' has to be approved", ""), 
	ORDER_ALREADY_APPROVED				(4024, Level.ERROR, false, "Processing order ''{0}'' already approved", ""), 
	ORDER_HASTOBE_PLANNED				(4025, Level.ERROR, false, "Processing order ''{0}'' has to be planned", ""), 
	ORDER_ALREADY_PLANNED				(4026, Level.ERROR, false, "Processing order ''{0}'' already planned", ""), 
	ORDER_HASTOBE_RELEASED				(4027, Level.ERROR, false, "Processing order ''{0}'' has to be released", ""), 
	ORDER_ALREADY_RELEASING				(4028, Level.ERROR, false, "Processing order ''{0}'' already releasing", ""), 
	ORDER_ALREADY_RELEASED				(4029, Level.ERROR, false, "Processing order ''{0}'' already released", ""), 
	ORDER_ALREADY_RUNNING				(4030, Level.ERROR, false, "Processing order ''{0}'' already running", ""), 
	ORDER_ALREADY_SUSPENDING			(4031, Level.ERROR, false, "Processing order ''{0}'' already suspending", ""), 
	ORDER_ALREADY_COMPLETED				(4032, Level.ERROR, false, "Processing order ''{0}'' already completed", ""), 
	ORDER_PRODUCT_EXIST 				(4033, Level.INFO, true, "Processing order ''{0}'' requested product already exist", ""), 
	ORDER_ALREADY_FAILED				(4034, Level.WARN, false, "Processing order ''{0}'' already failed", ""), 
	ORDER_ALREADY_CLOSED				(4035, Level.WARN, false, "Processing order ''{0}'' already closed", ""), 
	ORDER_COULD_NOT_RETRY				(4036, Level.ERROR, false, "Processing order ''{0}'' has to be in state FAILED to retry", ""), 
	ORDER_HASTOBE_FINISHED				(4037, Level.ERROR, false, "Processing order ''{0}'' has to be finished (completed or failed)", ""), 
	ORDER_NOT_EXIST						(4038, Level.ERROR, false, "Processing order ''{0}'' does not exist in the current mission", ""),
	ORDER_FACILITY_NOT_EXIST			(4039, Level.ERROR, false, "Processing order ''{0}'' and processing facility ''{1}'' does not exist", ""),
	ORDER_SLICING_TYPE_NOT_SET			(4040, Level.ERROR, false, "Processing order ''{0}'' slicing type not set", ""),
	ORDER_MISSION_NOT_SET				(4041, Level.ERROR, false, "Processing order ''{0}'' mission not set", ""),
	ORDER_REQ_PROC_NOT_SET				(4042, Level.ERROR, false, "Processing order ''{0}'' requested processor(s) not set", ""),
	ORDER_REQ_ORBIT_NOT_SET				(4043, Level.ERROR, false, "Processing order ''{0}'' requested orbit(s) not set", ""),
	ORDER_REQ_DAY_NOT_SET				(4044, Level.ERROR, false, "Processing order ''{0}'' requested calendar day not set", ""),
	ORDER_REQ_TIMESLICE_NOT_SET			(4045, Level.ERROR, false, "Processing order ''{0}'' requested time slice not set", ""),
	ORDER_REQ_ORBIT_OR_TIME_NOT_SET		(4046, Level.ERROR, false, "Processing order ''{0}'' requested orbit or start/stop time not set", ""),
	ORDERDISP_NO_CONF_PROC				(4047, Level.ERROR, false, "OrderDispatcher: no configured processor found for product class ''{0}''", ""),
	ORDER_REQ_CON_PROC_NOT_SET			(4048, Level.ERROR, false, "Processing order ''{0}'' requested configured processor not set", ""),
	ORDER_REQ_PROD_CLASS_NOT_SET		(4049, Level.ERROR, false, "Processing order ''{0}'' requested product class(es) not set", ""),
	ORDER_WAIT_FOR_RELEASE				(4050, Level.INFO, true, "Processing order ''{0}'' has state ''{1}'', wait for release", ""),
	ORDER_NOTHING_TO_PUBLISH			(4051, Level.INFO, true, "Processing order ''{0}'' has state ''{1}'', nothing to publish", ""),
	FACILITY_NOT_EXIST					(4052, Level.ERROR, false, "Processing facility ''{0}'' does not exist", ""),
	FACILITY_NOT_DEFINED				(4053, Level.ERROR, false, "No processing facility defined", ""),
	JOBS_RETRIEVED						(4054, Level.INFO, true, "Jobs for processing order ''{0}'' retrieved", ""), 
	JOBCOUNT_RETRIEVED					(4055, Level.INFO, true, "Job count for processing order ''{0}'' retrieved", ""), 
	JOBGRAPH_RETRIEVED					(4056, Level.INFO, true, "Dependency graph for job ''{0}'' retrieved", ""), 
	JOB_RETRIEVED						(4057, Level.INFO, true, "Job ''{0}'' retrieved", ""), 
	JOB_RELEASED						(4058, Level.INFO, true, "Job ''{0}'' is released", ""), 
	JOB_CANCELED						(4059, Level.INFO, true, "Job ''{0}'' is cancelled", ""), 
	JOB_DELETED 						(4060, Level.INFO, true, "Job ''{0}'' is deleted", ""), 
	JOB_SUSPENDED						(4061, Level.INFO, true, "Job ''{0}'' is suspended", ""), 
	JOB_HOLD							(4062, Level.INFO, true, "Job ''{0}'' is on hold", ""), 
	JOB_PLANNED							(4063, Level.INFO, true, "Job ''{0}'' is planned", ""), 
	JOB_INITIAL							(4064, Level.INFO, true, "Job ''{0}'' is initial", ""), 
	JOB_RETRIED							(4065, Level.INFO, true, "Job ''{0}'' set to inital", ""), 
	JOB_STARTED							(4066, Level.INFO, true, "Job ''{0}'' is started", ""), 
	JOB_COMPLETED						(4067, Level.INFO, true, "Job ''{0}'' is completed", ""), 
	JOB_CLOSED							(4068, Level.INFO, true, "Job ''{0}'' is closed", ""), 
	JOB_ALREADY_RELEASED				(4069, Level.WARN, false, "Job ''{0}'' is already released", ""), 
	JOB_HASTOBE_RELEASED				(4070, Level.ERROR, false, "Job ''{0}'' has to be released", ""), 
	JOB_HASTOBE_PLANNED					(4071, Level.ERROR, false, "Job ''{0}'' has to be planned", ""), 
	JOB_ALREADY_EXIST					(4072, Level.WARN, false, "Job ''{0}'' already exist", ""), 
	JOB_ALREADY_HOLD					(4073, Level.WARN, false, "Job ''{0}'' is on hold", ""), 
	JOB_ALREADY_STARTED					(4074, Level.WARN, false, "Job ''{0}'' is already started", ""), 
	JOB_ALREADY_COMPLETED				(4075, Level.WARN, false, "Job ''{0}'' already completed", ""), 
	JOB_ALREADY_FAILED					(4076, Level.WARN, false, "Job ''{0}'' already failed", ""), 
	JOB_ALREADY_CLOSED					(4077, Level.WARN, false, "Job ''{0}'' already closed", ""), 
	JOB_COULD_NOT_RETRY					(4078, Level.ERROR, false, "Job ''{0}'' has to be in state FAILED to retry", ""), 
	JOB_COULD_NOT_CLOSE					(4079, Level.ERROR, false, "Job ''{0}'' has to be in state COMPLETED or FAILED to close", ""), 
	JOB_NOT_EXIST						(4080, Level.ERROR, false, "Job ''{0}'' does not exist", ""),
	JOBSTEPS_RETRIEVED					(4081, Level.INFO, true, "Job steps of status {0} retrieved for mission {1}", ""), 
	JOBSTEP_RETRIEVED					(4082, Level.INFO, true, "Job step ''{0}'' retrieved", ""), 
	JOBSTEP_WAITING						(4083, Level.INFO, true, "Job step ''{0}'' is waiting for input", ""), 
	JOBSTEP_READY						(4084, Level.INFO, true, "Job step ''{0}'' is ready to run", ""), 
	JOBSTEP_CANCELED					(4085, Level.INFO, true, "Job step ''{0}'' is canceled", ""),  
	JOBSTEP_CLOSED						(4086, Level.INFO, true, "Job step ''{0}'' is closed", ""), 
	JOBSTEP_DELETED 					(4087, Level.INFO, true, "Job step ''{0}'' is deleted", ""), 
	JOBSTEP_SPQ_DELETED 				(4088, Level.INFO, true, "Satisfied product queries of job step ''{0}'' deleted", ""), 
	JOBSTEP_SUSPENDED					(4089, Level.INFO, true, "Job step ''{0}'' is suspended", ""), 
	JOBSTEP_STARTED 					(4090, Level.INFO, true, "Job step ''{0}'' is started", ""), 
	JOBSTEP_RETRIED						(4091, Level.INFO, true, "Job step ''{0}'' set to inital", ""), 
	JOBSTEP_RETRIED_COMPLETED			(4092, Level.INFO, true, "Job step ''{0}'' set to completed cause product exists", ""), 
	JOBSTEP_FAILED						(4093, Level.INFO, true, "Job step ''{0}'' is failed", ""), 
	JOBSTEP_COMPLETED					(4094, Level.INFO, true, "Job step ''{0}'' is completed", ""), 
	JOBSTEP_ALREADY_RUNNING				(4095, Level.WARN, false, "Job step ''{0}'' is already running", ""), 
	JOBSTEP_ALREADY_COMPLETED			(4096, Level.WARN, false, "Job step ''{0}'' already completed", ""), 
	JOBSTEP_ALREADY_CLOSED				(4097, Level.WARN, false, "Job step ''{0}'' already closed", ""), 
	JOBSTEP_ALREADY_FAILED				(4098, Level.WARN, false, "Job step ''{0}'' already failed", ""), 
	JOBSTEP_COULD_NOT_RETRY				(4099, Level.ERROR, false, "Job step ''{0}'' has to be in state FAILED to retry", ""), 
	JOBSTEP_COULD_NOT_CLOSE				(3100, Level.ERROR, false, "Job step ''{0}'' has to be in state COMPLETED or FAILED to close", ""), 
	JOBSTEP_NOT_EXIST					(4101, Level.ERROR, false, "Job step ''{0}'' does not exist", ""),
	JOF_DELETED							(4102, Level.INFO, true, "Job Order File ''{0}'' deleted", ""), 
	JOF_DELETING_ERROR					(4103, Level.ERROR, false, "Error deleting Job Order File ''{0}'' from processing facility ''{1}'' (cause: {1})", ""),
	JOBS_FOR_ORDER_NOT_EXIST			(4104, Level.ERROR, false, "Job(s) for processing order ''{0}'' do not exist", ""),
	PARAM_ID_FACILITY_NOT_SET			(4105, Level.ERROR, false, "Parameter id and facility are not set", ""),
	PARAM_FACILITY_NOT_SET				(4106, Level.ERROR, false, "Parameter facility is not set", ""),
	PARAM_ID_NOT_SET					(4107, Level.ERROR, false, "Parameter id is not set", ""),
	PLANNER_FACILITY_CONNECTED 			(4108, Level.INFO, true, "ProcessingFacility ''{0}'' connected to ''{1}''", ""),
	PLANNER_FACILITY_WORKER_CNT			(4109, Level.INFO, true, "{0} worker nodes found", ""),
	PLANNER_FACILITY_DISCONNECTED		(4110, Level.INFO, true, "ProcessingFacility ''{0}'' disconnected", ""),
	PLANNER_FACILITY_NOT_CONNECTED		(4111, Level.ERROR, false, "ProcessingFacility ''{0}'' could not be connected to ''{1}''", ""),
	PLANNER_AUTH_DATASOURCE				(4112, Level.INFO, true, "Initializing authentication from datasource ''{0}''", ""),
	KUBEDISPATCHER_CONFIG_NOT_SET		(4113, Level.ERROR, false, "KubeDispatcherRunOnce: KubeConfig not set", ""),
	KUBEDISPATCHER_RUN_ONCE				(4114, Level.INFO, true, "KubeDispatcher run once and finish", ""),
	KUBEDISPATCHER_CYCLE				(4115, Level.INFO, true, "KubeDispatcher cycle started", ""),
	KUBEDISPATCHER_SLEEP				(4116, Level.INFO, true, "KubeDispatcher cycle completed, sleeping for {0} ms", ""),
	KUBEDISPATCHER_INTERRUPT			(4117, Level.INFO, true, "KubeDispatcher interrupt", ""),
	KUBEDISPATCHER_PLANNER_NOT_SET		(4118, Level.ERROR, false, "KubeDispatcher: Production planner not set", ""),
	KUBECONFIG_JOB_NOT_FOUND			(4119, Level.ERROR, false, "Job ''{0}'' not found, is it already finished?", ""),
	KUBEJOB_CREATED						(4120, Level.INFO, true, "Kubernetes job ''{0}/{1}'' created", ""),
	KUBEJOB_FINISHED					(4121, Level.INFO, true, "Kubernetes job ''{0}/{1}'' finished", ""),
	KUBEJOB_FINISH_TRIGGERED			(4122, Level.INFO, true, "Finishing of Kubernetes job ''{0}/{1}'' triggered", ""),
	KUBERNETES_NOT_CONNECTED     	 	(4123, Level.ERROR, false, "Kubernetes configuration {0} not connected", ""),
	JOB_STEP_NOT_FOUND              	(4124, Level.ERROR, false, "No job step found for id {0}", ""),
	CONFIG_PROC_DISABLED           		(4125, Level.WARN, false, "Configured processor {0} is disabled", ""), 
	PLANNING_CHECK_COMPLETE				(4126, Level.INFO, true, "Planning check complete for product with ID {0}", ""),
	PLANNING_CHECK_FAILED				(4127, Level.ERROR, false, "Planning check failed for product with ID {0} (cause: {1})", ""),
	PLANNING_INTERRUPTED				(4128, Level.WARN, false, "Planning order {0} interrupted", ""),
	INSUFFICIENT_ORDER_DATA				(4129, Level.ERROR, false, "Insufficient data for sending job order to Storage Manager", ""),
	HTTP_REQUEST						(4130, Level.INFO, true, "HTTP-Request: {0}", ""),
	HTTP_RESPONSE						(4131, Level.INFO, true, "... response is {0}", ""),
	SENDING_JOB_EXCEPTION				(4132, Level.ERROR, false, "Exception sending job order to Storage Manager: {0}", ""),
	JOB_CREATION_FAILED					(4133, Level.ERROR, false, "Job creation failed with exception: ", ""),
	FACILITY_CONNECTION_FAILED			(4134, Level.ERROR, false, "Could not connect with facility {0}", ""),
	CONFIGURATION_ACCESS_FAILED			(4135, Level.INFO, true, "Cannot access Kubernetes Configuration file: {0}", ""),
	MALFORMED_HOST_ALIAS				(4136, Level.WARN, true, "Found malformed host alias parameter {0}", ""),
	JOB_CREATED							(4137, Level.INFO, true, "Job {0} created with status {1}", ""),
	JOB_STEP_CREATION_FAILED			(4138, Level.ERROR, false, "Creation of job order for job step {0} failed", ""),
	JOB_STEP_CREATION_FAILED_EXCEPTION	(4139, Level.ERROR, false, "Creation of job order for job step {0} failed with exception {1}", ""),
	SENDING_JOB_STEP_FAILED				(4140, Level.ERROR, false, "Sending of job order to Storage Manager failed for job step {0}", ""),
	JOB_STEP_CREATION_EXCEPTION			(4141, Level.ERROR, false, "General exception creating job for job step {0}: {1}", ""),
	KUBERNETES_API_EXCEPTION			(4142, Level.ERROR, false, "Kubernetes API exception creating job for job step {0}: {1}" + "\n" +
			"  Status code: {2}" + "\n" +
			"  Reason: {3}" + "\n" +
			"  Response headers: {4}", ""),
	NO_INPUT_QUERIES					(4143, Level.WARN, true, "Job Step ''{0}'' has no input product queries", ""),
	MSG_NO_INPUTPRODUCT					(4144, Level.ERROR, false, "No input product(s) found for order {0}", ""),
	NOTIFY_FAILED						(4145, Level.ERROR, false, "Notification to {0} failed: {1}", ""),
	;
	
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
