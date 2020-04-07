/**
 * Messages.java
 * 
 * © 2019 Prophos Informatik GmbH
 */

package de.dlr.proseo.planner;

/**
 * Messages, codes and states
 * 
 * @author Ernst Melchinger
 *
 */
public enum Messages {
	TRUE							(true, "true"),
	FALSE							(false, "false"),
	OK								(true, "Okay"),
	UNDEFINED						(false, "Undefined"),
	HTTP_HEADER_WARNING				(true, "Warning"),
	HTTP_HEADER_SUCCESS				(true, "Success"),
	MSG_PREFIX						(true, "199 proseo-planner "), 
	ORDER_APPROVED					(true, "Processing order '%s' is approved (%d)"), 
	ORDER_PLANNED					(true, "Processing order '%s' is planned (%d)"), 
	ORDER_RELEASED					(true, "Processing order '%s' is released (%d)"), 
	ORDER_RUNNING					(true, "Processing order '%s' is running (%d)"), 
	ORDER_SUSPENDED					(true, "Processing order '%s' is suspended (%d)"), 
	ORDER_CANCELED					(true, "Processing order '%s' is canceled (%d)"), 
	ORDER_RESET						(true, "Processing order '%s' is reset (%d)"), 
	ORDER_DELETED					(true, "Processing order '%s' deleted (%d)"), 
	ORDER_CLOSED					(true, "Processing order '%s' is closed (%d)"), 
	ORDER_COMPLETED					(true, "Processing order '%s' is completed (%d)"), 
	ORDER_RETRIED					(true, "Processing order '%s' set to planned (%d)"), 
	ORDER_HASTOBE_APPROVED			(false, "Processing order '%s' has to be approved (%d)"), 
	ORDER_ALREADY_APPROVED			(false, "Processing order '%s' already approved (%d)"), 
	ORDER_HASTOBE_PLANNED			(false, "Processing order '%s' has to be planned (%d)"), 
	ORDER_ALREADY_PLANNED			(false, "Processing order '%s' already planned (%d)"), 
	ORDER_HASTOBE_RELEASED			(false, "Processing order '%s' has to be released (%d)"), 
	ORDER_ALREADY_RELEASED			(false, "Processing order '%s' already released (%d)"), 
	ORDER_ALREADY_RUNNING			(false, "Processing order '%s' already running (%d)"), 
	ORDER_ALREADY_SUSPENDING		(false, "Processing order '%s' already suspending (%d)"), 
	ORDER_ALREADY_COMPLETED			(false, "Processing order '%s' already completed (%d)"), 
	ORDER_ALREADY_FAILED			(false, "Processing order '%s' already failed (%d)"), 
	ORDER_ALREADY_CLOSED			(false, "Processing order '%s' already closed (%d)"), 
	ORDER_COULD_NOT_RETRY			(false, "Processing order '%s' has to be in state FAILED to retry (%d)"), 
	ORDER_HASTOBE_FINISHED			(false, "Processing order '%s' has to be finished (completed or failed) (%d)"), 
	ORDER_NOT_EXIST					(false, "Processing order '%s' does not exist (%d)"),
	ORDER_FACILITY_NOT_EXIST		(false, "Processing order '%s' and processing facility '%s' does not exist (%d)"),
	ORDER_SLICING_TYPE_NOT_SET		(false, "Processing order '%s' slicing type not set (%d)"),
	ORDER_MISSION_NOT_SET			(false, "Processing order '%s' mission not set (%d)"),
	ORDER_REQ_PROC_NOT_SET			(false, "Processing order '%s' requested processor(s) not set (%d)"),
	ORDER_REQ_ORBIT_NOT_SET			(false, "Processing order '%s' requested orbit(s) not set (%d)"),
	ORDER_REQ_DAY_NOT_SET			(false, "Processing order '%s' requested calendar day not set (%d)"),
	ORDER_REQ_TIMESLICE_NOT_SET		(false, "Processing order '%s' requested time slice not set (%d)"),
	ORDER_REQ_ORBIT_OR_TIME_NOT_SET	(false, "Processing order '%s' requested orbit or start/stop time not set (%d)"),
	ORDERDISP_NO_CONF_PROC			(false, "OrderDipatcher: no configured processor found for product class '%s' (%d)"),
	ORDER_REQ_CON_PROC_NOT_SET		(false, "Processing order '%s' requested configured processor not set (%d)"),
	ORDER_REQ_PROD_CLASS_NOT_SET	(false, "Processing order '%s' requested product class(es) not set (%d)"),
	ORDER_WAIT_FOR_RELEASE			(true, "Processing order '%s' has state '%s', wait for release (%d)"),
	ORDER_NOTHING_TO_PUBLISH		(true, "Processing order '%s' has state '%s', nothing to publish (%d)"),
	FACILITY_NOT_EXIST				(false, "Processing facility '%s' does not exist (%d)"),
	FACILITY_NOT_DEFINED			(false, "No processing facility defined (%d)"),
	JOB_RELEASED					(true, "Job '%s' is released (%d)"), 
	JOB_CANCELED					(true, "Job '%s' is canceled (%d)"), 
	JOB_SUSPENDED					(true, "Job '%s' is suspended (%d)"), 
	JOB_HOLD						(true, "Job '%s' is on hold (%d)"), 
	JOB_INITIAL						(true, "Job '%s' is initial (%d)"), 
	JOB_RETRIED						(true, "Job '%s' set to inital (%d)"), 
	JOB_ALREADY_RELEASED			(false, "Job '%s' is already released (%d)"), 
	JOB_HASTOBE_RELEASED			(false, "Job '%s' has to be released (%d)"), 
	JOB_ALREADY_HOLD				(false, "Job '%s' is on hold (%d)"), 
	JOB_ALREADY_STARTED				(false, "Job '%s' is already started (%d)"), 
	JOB_ALREADY_COMPLETED			(false, "Job '%s' already completed (%d)"), 
	JOB_ALREADY_FAILED				(false, "Job '%s' already failed (%d)"), 
	JOB_COULD_NOT_RETRY				(false, "Job '%s' has to be in state FAILED to retry (%d)"), 
	JOB_NOT_EXIST					(false, "Job '%s' does not exist (%d)"),
	JOBSTEP_WAITING					(true, "Job step '%s' is waiting for input (%d)"), 
	JOBSTEP_READY					(true, "Job step '%s' is ready to run (%d)"), 
	JOBSTEP_CANCELED				(true, "Job step '%s' is canceled (%d)"), 
	JOBSTEP_SUSPENDED				(true, "Job step '%s' is suspended (%d)"), 
	JOBSTEP_RETRIED					(true, "Job step '%s' set to inital (%d)"), 
	JOBSTEP_FAILED					(true, "Job step '%s' is failed (%d)"), 
	JOBSTEP_COMPLETED				(true, "Job step '%s' is completed (%d)"), 
	JOBSTEP_ALREADY_RUNNING			(false, "Job step '%s' is already running (%d)"), 
	JOBSTEP_ALREADY_COMPLETED		(false, "Job step '%s' already completed (%d)"), 
	JOBSTEP_ALREADY_FAILED			(false, "Job step '%s' already failed (%d)"), 
	JOBSTEP_COULD_NOT_RETRY			(false, "Job step '%s' has to be in state FAILED to retry (%d)"), 
	JOBSTEP_NOT_EXIST				(false, "Job step '%s' does not exist (%d)"),
	JOBS_FOR_ORDER_NOT_EXIST		(false, "Jobs for processing order '%s' does not exist (%d)"),
	PARAM_ID_FACILITY_NOT_SET		(false, "Parameter id and facility are not set (%d)"),
	PARAM_FACILITY_NOT_SET			(false, "Parameter facility is not set (%d)"),
	PARAM_ID_NOT_SET				(false, "Parameter id is not set (%d)"),
	PLANNER_FACILITY_CONNECTED 		(true, "ProcessingFacility '%s' connected to '%s' (%d)"),
	PLANNER_FACILITY_WORKER_CNT		(true, "%s worker nodes found (%d)"),
	PLANNER_FACILITY_DISCONNECTED	(true, "ProcessingFacility '%s' disconnected (%d)"),
	PLANNER_FACILITY_NOT_CONNECTED	(false, "ProcessingFacility '%s' could not be connected to '%s' (%d)"),
	PLANNER_AUTH_DATASOURCE			(true, "Initializing authentication from datasource '%s' (%d)"),
	KUBEDISPATCHER_CONFIG_NOT_SET	(false, "KubeDispatcherRunOnce: KubeConfig not set (%d)"),
	KUBEDISPATCHER_RUN_ONCE			(true, "KubeDispatcher run once and finish (%d)"),
	KUBEDISPATCHER_CYCLE			(true, "KubeDispatcher cycle (%d)"),
	KUBEDISPATCHER_INTERRUPT		(true, "KubeDispatcher interrupt (%d)"),
	KUBEDISPATCHER_PLANNER_NOT_SET	(false, "KubeDispatcher: Production planner not set (%d)"),
	KUBECONFIG_JOB_NOT_FOUND		(false, "Job '%s' not found, is it already finished?"),
	KUBEJOB_CREATED					(true, "Kubernetes job '%s/%s' created (%d)"),
	KUBEJOB_FINISHED				(true, "Kubernetes job '%s/%s' finished (%d)"),
	
	DUMMY							(true, "(%d)")
	;
	
	

	private final int code;
	private final String description;
	private final boolean success;
	private static int nextCode = 3000;

	/**
	 * Get the next code number, used to generate codes automatically
	 * 
	 * @return code
	 */
	private static int getNextCode() {
		if (nextCode == 0) {
			nextCode = 3000;
		}
		return ++nextCode; 
	}

	/**
	 * Get the message with corresponding code.
	 * 
	 * @param code of message to retrieve
	 * @return message 
	 */
	public static Messages getValueOfCode(int code) {
		int index = code - 3000;
		if (index < 0 || index >= Messages.values().length) {
			return Messages.UNDEFINED;
		} else {
			return Messages.values()[code - 3000];
		}
	}
	
	/**
	 * Create Messages element with success flag and description.
	 * 
	 * @param success true if positive, else false
	 * @param description message string
	 */
	private Messages(boolean success, String description) {
		this.code = Messages.getNextCode();
		this.description = description;
		this.success = success;
	}

	/**
	 * Get the success state of the message.
	 * 
	 * @return success state
	 */
	public boolean isTrue() {
		return success;
	}
	
	/**
	 * Get the message string.
	 * 
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Format the message, use description and code.
	 * 
	 * @return Formatted string
	 */
	public String format() {
		return String.format(description, code);
	}

	/**
	 * Format the message, use description and code, insert string parameters into description.
	 * 
	 * @param s1
	 * @return Formatted string
	 */
	public String format(String s1) {
		return String.format(description, s1, code);
	}

	/**
	 * Format the message, use description and code, insert string parameters into description.
	 * 
	 * @param s1
	 * @param s2
	 * @return Formatted string
	 */
	public String format(String s1, String s2) {
		return String.format(description, s1, s2, code);
	}

	/**
	 * Format the message, use description and code, insert string parameters into description.
	 * 
	 * @param s1
	 * @param s2
	 * @param s3
	 * @return Formatted string
	 */
	public String format(String s1, String s2, String s3) {
		return String.format(description, s1, s2, s3, code);
	}

	/**
	 * Format the message, use prefix (MSG_PREFIX) description and code.
	 * 
	 * @return Formatted string
	 */
	public String formatWithPrefix() {
		return Messages.MSG_PREFIX.getDescription() + String.format(description, code);
	}

	/**
	 * Format the message, use prefix (MSG_PREFIX) description and code, insert string parameters into description.
	 * 
	 * @param s1
	 * @return Formatted string
	 */
	public String formatWithPrefix(String s1) {
		return Messages.MSG_PREFIX.getDescription() + String.format(description, s1, code);
	}

	/**
	 * Format the message, use prefix (MSG_PREFIX) description and code, insert string parameters into description.
	 * 
	 * @param s1
	 * @param s2
	 * @return Formatted string
	 */
	public String formatWithPrefix(String s1, String s2) {
		return Messages.MSG_PREFIX.getDescription() + String.format(description, s1, s2, code);
	}

	/**
	 * Format the message, use prefix (MSG_PREFIX) description and code, insert string parameters into description.
	 * 
	 * @param s1
	 * @param s2
	 * @param s3
	 * @return Formatted string
	 */
	public String formatWithPrefix(String s1, String s2, String s3) {
		return Messages.MSG_PREFIX.getDescription() + String.format(description, s1, s2, s3, code);
	}

	/**
	 * Get the message code.
	 * 
	 * @return code
	 */
	public int getCode() {
		return code;
	}

	/* (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return description;
	}
}
