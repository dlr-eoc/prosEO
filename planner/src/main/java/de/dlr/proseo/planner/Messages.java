/**
 * Messages.java
 * 
 * © 2019 Prophos Informatik GmbH
 */

package de.dlr.proseo.planner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;

/**
 * Messages, codes and states
 * 
 * @author Ernst Melchinger
 *
 */
public enum Messages {
	TRUE							(true, MessageType.I, "true"),
	FALSE							(false, MessageType.E, "false"),
	OK								(true, MessageType.I, "Okay"),
	UNDEFINED						(false, MessageType.E, "Undefined"),
	HTTP_HEADER_WARNING				(true, MessageType.W, "Warning"),
	HTTP_HEADER_SUCCESS				(true, MessageType.I, "Success"),
	MSG_PREFIX						(true, MessageType.I, "199 proseo-planner "), 
	ORDER_APPROVED					(true, MessageType.I, "Processing order '%s' is approved"), 
	ORDER_PLANNED					(true, MessageType.I, "Processing order '%s' is planned"), 
	ORDER_RELEASED					(true, MessageType.I, "Processing order '%s' is released"), 
	ORDER_RUNNING					(true, MessageType.I, "Processing order '%s' is running"), 
	ORDER_SUSPENDED					(true, MessageType.I, "Processing order '%s' is suspended"), 
	ORDER_CANCELED					(true, MessageType.I, "Processing order '%s' is canceled"), 
	ORDER_RESET						(true, MessageType.I, "Processing order '%s' is reset"), 
	ORDER_DELETED					(true, MessageType.I, "Processing order '%s' deleted"), 
	ORDER_CLOSED					(true, MessageType.I, "Processing order '%s' is closed"), 
	ORDER_COMPLETED					(true, MessageType.I, "Processing order '%s' is completed"), 
	ORDER_RETRIED					(true, MessageType.I, "Processing order '%s' set to planned"), 
	ORDER_HASTOBE_APPROVED			(false, MessageType.E, "Processing order '%s' has to be approved"), 
	ORDER_ALREADY_APPROVED			(false, MessageType.W, "Processing order '%s' already approved"), 
	ORDER_HASTOBE_PLANNED			(false, MessageType.E, "Processing order '%s' has to be planned"), 
	ORDER_ALREADY_PLANNED			(false, MessageType.W, "Processing order '%s' already planned"), 
	ORDER_HASTOBE_RELEASED			(false, MessageType.E, "Processing order '%s' has to be released"), 
	ORDER_ALREADY_RELEASED			(false, MessageType.W, "Processing order '%s' already released"), 
	ORDER_ALREADY_RUNNING			(false, MessageType.W, "Processing order '%s' already running"), 
	ORDER_ALREADY_SUSPENDING		(false, MessageType.W, "Processing order '%s' already suspending"), 
	ORDER_ALREADY_COMPLETED			(false, MessageType.W, "Processing order '%s' already completed"), 
	ORDER_PRODUCT_EXIST 			(true, MessageType.I, "Processing order '%s' requested product already exist"), 
	ORDER_ALREADY_FAILED			(false, MessageType.W, "Processing order '%s' already failed"), 
	ORDER_ALREADY_CLOSED			(false, MessageType.W, "Processing order '%s' already closed"), 
	ORDER_COULD_NOT_RETRY			(false, MessageType.E, "Processing order '%s' has to be in state FAILED to retry"), 
	ORDER_HASTOBE_FINISHED			(false, MessageType.E, "Processing order '%s' has to be finished (completed or failed)"), 
	ORDER_NOT_EXIST					(false, MessageType.E, "Processing order '%s' does not exist in the current mission"),
	ORDER_FACILITY_NOT_EXIST		(false, MessageType.E, "Processing order '%s' and processing facility '%s' does not exist"),
	ORDER_SLICING_TYPE_NOT_SET		(false, MessageType.E, "Processing order '%s' slicing type not set"),
	ORDER_MISSION_NOT_SET			(false, MessageType.E, "Processing order '%s' mission not set"),
	ORDER_REQ_PROC_NOT_SET			(false, MessageType.E, "Processing order '%s' requested processor(s) not set"),
	ORDER_REQ_ORBIT_NOT_SET			(false, MessageType.E, "Processing order '%s' requested orbit(s) not set"),
	ORDER_REQ_DAY_NOT_SET			(false, MessageType.E, "Processing order '%s' requested calendar day not set"),
	ORDER_REQ_TIMESLICE_NOT_SET		(false, MessageType.E, "Processing order '%s' requested time slice not set"),
	ORDER_REQ_ORBIT_OR_TIME_NOT_SET	(false, MessageType.E, "Processing order '%s' requested orbit or start/stop time not set"),
	ORDERDISP_NO_CONF_PROC			(false, MessageType.E, "OrderDispatcher: no configured processor found for product class '%s'"),
	ORDER_REQ_CON_PROC_NOT_SET		(false, MessageType.E, "Processing order '%s' requested configured processor not set"),
	ORDER_REQ_PROD_CLASS_NOT_SET	(false, MessageType.E, "Processing order '%s' requested product class(es) not set"),
	ORDER_WAIT_FOR_RELEASE			(true, MessageType.I, "Processing order '%s' has state '%s', wait for release"),
	ORDER_NOTHING_TO_PUBLISH		(true, MessageType.I, "Processing order '%s' has state '%s', nothing to publish"),
	FACILITY_NOT_EXIST				(false, MessageType.E, "Processing facility '%s' does not exist"),
	FACILITY_NOT_DEFINED			(false, MessageType.E, "No processing facility defined"),
	JOB_RELEASED					(true, MessageType.I, "Job '%s' is released"), 
	JOB_CANCELED					(true, MessageType.I, "Job '%s' is canceled"), 
	JOB_DELETED 					(true, MessageType.I, "Job '%s' is canceled"), 
	JOB_SUSPENDED					(true, MessageType.I, "Job '%s' is suspended"), 
	JOB_HOLD						(true, MessageType.I, "Job '%s' is on hold"), 
	JOB_INITIAL						(true, MessageType.I, "Job '%s' is initial"), 
	JOB_RETRIED						(true, MessageType.I, "Job '%s' set to inital"), 
	JOB_STARTED						(true, MessageType.I, "Job '%s' is started"), 
	JOB_COMPLETED					(true, MessageType.I, "Job '%s' completed"), 
	JOB_ALREADY_RELEASED			(false, MessageType.W, "Job '%s' is already released"), 
	JOB_HASTOBE_RELEASED			(false, MessageType.E, "Job '%s' has to be released"), 
	JOB_ALREADY_HOLD				(false, MessageType.W, "Job '%s' is on hold"), 
	JOB_ALREADY_STARTED				(false, MessageType.W, "Job '%s' is already started"), 
	JOB_ALREADY_COMPLETED			(false, MessageType.W, "Job '%s' already completed"), 
	JOB_ALREADY_FAILED				(false, MessageType.W, "Job '%s' already failed"), 
	JOB_COULD_NOT_RETRY				(false, MessageType.E, "Job '%s' has to be in state FAILED to retry"), 
	JOB_NOT_EXIST					(false, MessageType.E, "Job '%s' does not exist"),
	JOBSTEP_WAITING					(true, MessageType.I, "Job step '%s' is waiting for input"), 
	JOBSTEP_READY					(true, MessageType.I, "Job step '%s' is ready to run"), 
	JOBSTEP_CANCELED				(true, MessageType.I, "Job step '%s' is canceled"), 
	JOBSTEP_DELETED 				(true, MessageType.I, "Job step '%s' is deleted"), 
	JOBSTEP_SUSPENDED				(true, MessageType.I, "Job step '%s' is suspended"), 
	JOBSTEP_STARTED 				(true, MessageType.I, "Job step '%s' is started"), 
	JOBSTEP_RETRIED					(true, MessageType.I, "Job step '%s' set to inital"), 
	JOBSTEP_RETRIED_COMPLETED		(true, MessageType.I, "Job step '%s' set to completed cause product exists"), 
	JOBSTEP_FAILED					(true, MessageType.I, "Job step '%s' is failed"), 
	JOBSTEP_COMPLETED				(true, MessageType.I, "Job step '%s' is completed"), 
	JOBSTEP_ALREADY_RUNNING			(false, MessageType.W, "Job step '%s' is already running"), 
	JOBSTEP_ALREADY_COMPLETED		(false, MessageType.W, "Job step '%s' already completed"), 
	JOBSTEP_ALREADY_FAILED			(false, MessageType.W, "Job step '%s' already failed"), 
	JOBSTEP_COULD_NOT_RETRY			(false, MessageType.E, "Job step '%s' has to be in state FAILED to retry"), 
	JOBSTEP_NOT_EXIST				(false, MessageType.E, "Job step '%s' does not exist"),
	JOF_DELETED						(true, MessageType.I, "Job Order File '%s' deleted"), 
	JOF_DELETING_ERROR				(false, MessageType.E, "Error deleting Job Order File '%s' from processing facility '%s' (cause: %s)"),
	JOBS_FOR_ORDER_NOT_EXIST		(false, MessageType.E, "Job(s) for processing order '%s' do not exist"),
	PARAM_ID_FACILITY_NOT_SET		(false, MessageType.E, "Parameter id and facility are not set"),
	PARAM_FACILITY_NOT_SET			(false, MessageType.E, "Parameter facility is not set"),
	PARAM_ID_NOT_SET				(false, MessageType.E, "Parameter id is not set"),
	PLANNER_FACILITY_CONNECTED 		(true, MessageType.I, "ProcessingFacility '%s' connected to '%s'"),
	PLANNER_FACILITY_WORKER_CNT		(true, MessageType.I, "%s worker nodes found"),
	PLANNER_FACILITY_DISCONNECTED	(true, MessageType.I, "ProcessingFacility '%s' disconnected"),
	PLANNER_FACILITY_NOT_CONNECTED	(false, MessageType.E, "ProcessingFacility '%s' could not be connected to '%s'"),
	PLANNER_AUTH_DATASOURCE			(true, MessageType.I, "Initializing authentication from datasource '%s'"),
	KUBEDISPATCHER_CONFIG_NOT_SET	(false, MessageType.E, "KubeDispatcherRunOnce: KubeConfig not set"),
	KUBEDISPATCHER_RUN_ONCE			(true, MessageType.I, "KubeDispatcher run once and finish"),
	KUBEDISPATCHER_CYCLE			(true, MessageType.I, "KubeDispatcher cycle"),
	KUBEDISPATCHER_INTERRUPT		(true, MessageType.I, "KubeDispatcher interrupt"),
	KUBEDISPATCHER_PLANNER_NOT_SET	(false, MessageType.E, "KubeDispatcher: Production planner not set"),
	KUBECONFIG_JOB_NOT_FOUND		(false, MessageType.E, "Job '%s' not found, is it already finished?"),
	KUBEJOB_CREATED					(true, MessageType.I, "Kubernetes job '%s/%s' created"),
	KUBEJOB_FINISHED				(true, MessageType.I, "Kubernetes job '%s/%s' finished"),
	KUBERNETES_NOT_CONNECTED        (false, MessageType.E, "Kubernetes configuration %s not connected"),
	JOB_STEP_NOT_FOUND              (false, MessageType.E, "No job step found for id %d"),
	CONFIG_PROC_DISABLED            (false, MessageType.W, "Configured processor %s is disabled"), 
	PLANNING_CHECK_COMPLETE			(true, MessageType.I, "Planning check complete for product with ID %d"),
	PLANNING_CHECK_FAILED			(false, MessageType.E, "Planning check failed for product with ID %d (cause: %s)"),

	// Same as in other services
	ILLEGAL_CROSS_MISSION_ACCESS 	(false, MessageType.E, "Illegal cross-mission access to mission %s (logged in to %s)"),
	
	FACILITY_NOT_AVAILABLE			(false, MessageType.W, "Processing facility %s is not available cause it is: %s"),
	
	DUMMY							(true, MessageType.I, "(%d)")
	;
	
	public enum MessageType {
		I, // Information
		W, // Warning
		E; // Error
	}

	
	/**
	 * The message code
	 */
	private final int code;
	
	/**
	 * The message type
	 */
	private final MessageType type;
	
	/**
	 * The message description 
	 */
	private final String description;
	
	/**
	 * The message success, true or false
	 */
	private final boolean success;
	
	/**
	 * The next message code.
	 * At the moment the code is generated as a sequence starting at 3000 
	 */
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
	private Messages(boolean success, MessageType type, String description) {
		this.code = Messages.getNextCode();
		this.type = type;
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
	public String format(Object... messageParameters) {
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, code);
		messageParamList.add(0, type.toString());

		return String.format("(%s%d) " + description, messageParamList.toArray());
	}
	public String log(Logger logger, Object... messageParameters) {
		return logPrim(logger, this.format(messageParameters));
	}

	/**
	 * Format the message, use prefix (MSG_PREFIX) description and code.
	 * 
	 * @return Formatted string
	 */
	public String formatWithPrefix(Object... messageParameters) {
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, code);
		messageParamList.add(0, type.toString());
		return Messages.MSG_PREFIX.getDescription() + String.format("(%s%d) " + description, messageParamList.toArray());
	}
	public String logWithPrefix(Logger logger, Object... messageParameters) {
		return logPrim(logger, this.formatWithPrefix(messageParameters));
	}

	/**
	 * Get the message code.
	 * 
	 * @return code
	 */
	public int getCode() {
		return code;
	}

	private String logPrim(Logger logger, String msg) {
		if (logger != null && msg != null) {
			switch (this.type) {
			case E:
				logger.error(msg);
				break;
			case W:
				logger.warn(msg);
				break;
			case I:
			default:
				logger.info(msg);
				break;
			}
			return msg;
		} else {
			return "";
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return description;
	}
}
