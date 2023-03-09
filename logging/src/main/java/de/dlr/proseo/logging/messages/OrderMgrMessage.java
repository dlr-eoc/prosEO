/**
 * OrderMgrMessage.java
 * 
 * (C) 35 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the order manager.
 *
 * @author Katharina Bassler
 */
public enum OrderMgrMessage implements ProseoMessage {

	DELETE_PRODUCTS_WITHOUT_FORCE		(3500, Level.ERROR, false, "Option 'delete-products' not valid without option 'force'", ""),
	DELETION_UNSUCCESSFUL				(3501, Level.ERROR, false, "Order deletion unsuccessful for ID {0}", ""),
	DUPLICATE_ORDER_IDENTIFIER			(3502, Level.ERROR, false, "Order identifier {0} already exists within mission {1}", ""),
	DUPLICATE_ORDER_UUID				(3503, Level.ERROR, false, "Order UUID {0} already exists", ""),
	ILLEGAL_CREATION_STATE				(3504, Level.ERROR, false, "Orders must be created in INITIAL state (found state {0})", ""),
	ILLEGAL_ORDER_STATE					(3505, Level.ERROR, false, "Order update only allowed in INITIAL state", ""),
	ILLEGAL_STATE_TRANSITION			(3506, Level.ERROR, false, "Illegal order state transition from {0} to {1}", ""),
	INVALID_CONFIGURED_PROCESSOR		(3507, Level.ERROR, false, "Configured processor {0} not found", ""),
	INVALID_FILE_CLASS					(3508, Level.ERROR, false, "Output file class {0} is not defined for mission {1}", ""),
	INVALID_INPUT_CLASS					(3509, Level.ERROR, false, "Input product class {0} is not defined for mission {1}", ""),
	INVALID_MISSION_CODE				(3510, Level.ERROR, false, "No mission found for mission code {0}", ""),
	INVALID_ORBIT_RANGE					(3511, Level.ERROR, false, "No orbits defined between orbit number {0} and {1} for spacecraft {2}", ""),
	INVALID_OUTPUT_CLASS				(3512, Level.ERROR, false, "Output product class {0} is not defined for mission {1}", ""),
	INVALID_PROCESSING_MODE				(3513, Level.ERROR, false, "Processing mode {0} is not defined for mission {1}", ""),
	INVALID_REQUESTED_CLASS				(3514, Level.ERROR, false, "Requested product class {0} is not defined for mission {1}", ""),
	INVALID_SLICE_OVERLAP				(3515, Level.ERROR, false, "Order {0} with slicing type NONE has invalid slice overlap {1} (no overlap allowed)", ""),
	JOB_NOT_EXIST						(3516, Level.ERROR, false, "Job {0} does not exist", ""),
	JOB_RETRIED							(3517, Level.INFO, true, "Job {0} set to inital", ""),
	JOBCOUNT_RETRIEVED					(3518, Level.INFO, true, "Job count for processing order {0} retrieved", ""),
	JOBINDEX_RETRIEVED					(3519, Level.INFO, true, "Job index for processing order {0} retrieved", ""),
	JOBS_FOR_ORDER_NOT_EXIST			(3520, Level.ERROR, false, "Job(s) for processing order {0} do not exist", ""),
	JOBS_RETRIEVED						(3521, Level.INFO, true, "Jobs for processing order {0} retrieved", ""),
	JOBSTEP_NOT_EXIST					(3522, Level.ERROR, false, "Job step {0} does not exist", ""),
	JOBSTEP_RETRIEVED					(3523, Level.INFO, true, "Job step {0} retrieved", ""),
	JOBSTEPS_RETRIEVED					(3524, Level.INFO, true, "Job steps of status {0} retrieved for mission {1}", ""),
	JOF_DELETED							(3525, Level.INFO, true, "Job Order File {0} deleted from processing facility {1}", ""),
	JOF_DELETING_ERROR					(3526, Level.ERROR, false, "Error deleting Job Order File {0} from processing facility {1} (cause: {2})", ""),
	MISSION_CODE_MISSING				(3527, Level.ERROR, false, "No mission code given", ""),
	MISSION_CREATED						(3528, Level.INFO, true, "Mission {0} created", ""),
	MISSION_DELETED						(3529, Level.INFO, true, "Mission with database ID {0} deleted", ""),
	MISSION_DELETION_UNSUCCESSFUL		(3530, Level.ERROR, false, "Mission deletion unsuccessful for ID {0}", ""),
	MISSION_EXISTS						(3531, Level.ERROR, false, "Mission with mission code {0} already exists", ""),
	MISSION_NOT_FOUND					(3532, Level.ERROR, false, "No mission found for ID {0}", ""),
	MISSION_NOT_MODIFIED				(3533, Level.INFO, true, "Mission with id {0} not modified (no changes)", ""),
	MISSION_RETRIEVED					(3534, Level.INFO, true, "Mission {0} retrieved", ""),
	MISSION_UPDATED						(3535, Level.INFO, true, "Mission {0} updated", ""),
	MISSIONS_RETRIEVED					(3536, Level.INFO, true, "All missions retrieved", ""),
	MODEL_ORDER_MISSIONCODE				(3537, Level.INFO, true, "Model order missioncode {0}", ""),
	NEGATIVE_DURATION					(3538, Level.ERROR, false, "Order {0} has start time {1} after stop time {2}", ""),
	NO_MISSIONS_FOUND					(3539, Level.ERROR, false, "No missions found", ""),
	NO_ORBITS_FOUND						(3540, Level.ERROR, false, "No orbits found for given search criteria", ""),
	NUMBER_ORDERS_DELETED				(3541, Level.INFO, true, "{0} orders deleted", ""),
	ORBIT_DELETED						(3542, Level.INFO, true, "Orbit {0} deleted", ""),
	ORBIT_DELETION_UNSUCCESSFUL			(3543, Level.ERROR, false, "Orbit deletion unsuccessful for ID {0}", ""),
	ORBIT_INCOMPLETE					(3545, Level.ERROR, false, "Spacecraft code not set in the search", ""),
	ORBIT_MISSING						(3546, Level.ERROR, false, "Orbit not set", ""),
	ORBIT_NOT_FOUND						(3547, Level.ERROR, false, "No orbit found for ID {0}", ""),
	ORBIT_NOT_MODIFIED					(3548, Level.INFO, true, "Mission with id {0} not modified (no changes)", ""),
	ORBIT_RETRIEVED						(3549, Level.INFO, true, "Orbit {0} retrieved", ""),
	ORBIT_UPDATED						(3550, Level.INFO, true, "Orbit {0} updated", ""),
	ORBITS_CREATED						(3551, Level.INFO, true, "{0} orbits created or updated", ""),
	ORBITS_RETRIEVED					(3552, Level.INFO, true, "{0} orbits retrieved", ""),
	ORDER_CREATED						(3553, Level.INFO, true, "Order with identifier {0} created for mission {1}", ""),
	ORDER_DELETED						(3554, Level.INFO, true, "Order with id {0} deleted", ""),
	ORDER_ID_MISSING					(3555, Level.ERROR, false, "Order ID not set", ""),
	ORDER_IDENTIFIER_MISSING			(3556, Level.ERROR, false, "Order identifier not set", ""),
	ORDER_LIST_EMPTY					(3557, Level.ERROR, false, "No processing order found for search criteria", ""),
	ORDER_LIST_RETRIEVED				(3558, Level.INFO, true, "Order list of size {0} retrieved for mission {1}, order {2}, sensing start time between {3} and {4}", ""),
	ORDER_MISSING						(3559, Level.ERROR, false, "Order not set", ""),
	ORDER_MODIFICATION_FORBIDDEN		(3560, Level.ERROR, false, "Order modification other than state change not allowed for user {0}", ""),
	ORDER_MODIFIED						(3561, Level.INFO, true, "Order with id {0} modified", ""),
	ORDER_NOT_FOUND						(3562, Level.ERROR, false, "No order found for ID {0}", ""),
	ORDER_NOT_MODIFIED					(3563, Level.INFO, true, "Order with id {0} not modified (no changes)", ""),
	ORDER_RETRIEVED						(3564, Level.INFO, true, "Order with ID {0} retrieved", ""),
	ORDER_TIME_INTERVAL_MISSING			(3565, Level.ERROR, false, "Time interval (orbit or time range) missing for order {1}", ""),
	PROCESSORCLASSES_EXIST				(3566, Level.ERROR, false, "Cannot delete mission {0} due to existing processor classes", ""),
	PRODUCTCLASSES_EXIST				(3567, Level.ERROR, false, "Cannot delete mission {0} due to existing product classes", ""),
	PRODUCTS_EXIST						(3568, Level.ERROR, false, "Cannot delete mission {0} due to existing products", ""),
	REQUESTED_PRODUCTCLASSES_MISSING	(3569, Level.ERROR, false, "Requested product classes missing for order {0}", ""),
	SLICE_DURATION_MISSING				(3570, Level.ERROR, false, "Time slice duration missing for order {0} of slicing type TIME_SLICE", ""),
	SPACECRAFT_EXISTS					(3571, Level.ERROR, false, "Spacecraft with spacecraft code {0} already exists for mission {1}", ""),
	SPACECRAFT_NOT_FOUND				(3572, Level.ERROR, false, "Spacecraft {0} not found in mission {1}", ""),
	STATE_TRANSITION_FORBIDDEN			(3573, Level.ERROR, false, "Order state transition from {0} to {1} not allowed for user {2}", ""),
	ORDER_CLEANUP_CYCLE					(3574, Level.INFO, true, "Order cleanup cycle started", ""),
	ORDER_CLEANUP_SLEEP					(3575, Level.INFO, true, "Order cleanup cycle completed, sleeping for {0} ms", ""),
	ORDER_CLEANUP_TERMINATE				(3576, Level.INFO, true, "Order cleanup cycle interrupted – terminating‚", ""),
	ORDER_NOT_EVICTABLE					(3577, Level.ERROR, false, "Eviction time {1} of order {0} not before requested cutoff time {2}", ""),
	
	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private OrderMgrMessage(int code, Level level, boolean success, String message, String description) {
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
