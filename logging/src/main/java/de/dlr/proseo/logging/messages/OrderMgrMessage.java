/**
 * OrderMgrMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the order manager.
 *
 * @author Katharina Bassler
 */
public enum OrderMgrMessage implements ProseoMessage {

	JOBCOUNT_RETRIEVED					(1065, Level.INFO, true, "Job count for processing order {0} retrieved", ""),
	JOBINDEX_RETRIEVED					(1066, Level.INFO, true, "Job index for processing order {0} retrieved", ""),
	JOB_RETRIED							(1067, Level.INFO, true, "Job {0} set to inital", ""),
	JOB_NOT_EXIST						(1068, Level.ERROR, false, "Job {0} does not exist", ""),
	JOBSTEP_NOT_EXIST					(1069, Level.ERROR, false, "Job step {0} does not exist", ""),
	JOBSTEP_RETRIEVED					(1070, Level.INFO, true, "Job step {0} retrieved", ""),
	JOBSTEPS_RETRIEVED					(1071, Level.INFO, true, "Job steps of status {0} retrieved for mission {1}", ""),
	JOBS_FOR_ORDER_NOT_EXIST			(1072, Level.ERROR, false, "Job(s) for processing order {0} do not exist", ""),
	JOBS_RETRIEVED						(1073, Level.INFO, true, "Jobs for processing order {0} retrieved", ""),
	NO_MISSIONS_FOUND					(1002, Level.ERROR, false, "No missions found", ""),
	MISSION_NOT_FOUND					(1001, Level.ERROR, false, "No mission found for ID {0}", ""),
	MISSION_DELETION_UNSUCCESSFUL		(1004, Level.ERROR, false, "Mission deletion unsuccessful for ID {0}", ""),
	DELETE_PRODUCTS_WITHOUT_FORCE		(1005, Level.ERROR, false, "Option 'delete-products' not valid without option 'force'", ""),
	PRODUCTS_EXIST						(1006, Level.ERROR, false, "Cannot delete mission {0} due to existing products", ""),
	PRODUCTCLASSES_EXIST				(1006, Level.ERROR, false, "Cannot delete mission {0} due to existing product classes", ""),
	PROCESSORCLASSES_EXIST				(1008, Level.ERROR, false, "Cannot delete mission {0} due to existing processor classes", ""),
	MISSION_EXISTS						(1015, Level.ERROR, false, "Mission with mission code {0} already exists", ""),
	SPACECRAFT_EXISTS					(1016, Level.ERROR, false, "Spacecraft with spacecraft code {0} already exists for mission {1}", ""),
	MISSION_CODE_MISSING				(1017, Level.ERROR, false, "No mission code given", ""),
	MISSION_DELETED						(1010, Level.INFO, true, "Mission with database ID {0} deleted", ""),
	MISSION_UPDATED						(1011, Level.INFO, true, "Mission {0} updated", ""),
	MISSION_RETRIEVED					(1012, Level.INFO, true, "Mission {0} retrieved", ""),
	MISSION_CREATED						(1013, Level.INFO, true, "Mission {0} created", ""),
	MISSIONS_RETRIEVED					(1014, Level.INFO, true, "All missions retrieved", ""),
	MISSION_NOT_MODIFIED				(1018, Level.INFO, true, "Mission with id {0} not modified (no changes)", ""),
	ORBIT_NOT_FOUND						(1050, Level.ERROR, false, "No orbit found for ID {0}", ""),
	ORBIT_DELETION_UNSUCCESSFUL			(1051, Level.ERROR, false, "Orbit deletion unsuccessful for ID {0}", ""),
	ORBIT_MISSING						(1052, Level.ERROR, false, "Orbit not set", ""),
	ORBIT_INCOMPLETE					(1053, Level.ERROR, false, "Spacecraft code not set in the search", ""),
	NO_ORBITS_FOUND						(1054, Level.ERROR, false, "No orbits found for given search criteria", ""),
	SPACECRAFT_NOT_FOUND				(1055, Level.ERROR, false, "Spacecraft {0} not found in mission {1}", ""),
	ORBITS_RETRIEVED					(1056, Level.INFO, true, "{0} orbits retrieved", ""),
	ORBITS_CREATED						(1057, Level.INFO, true, "{0} orbits created or updated", ""),
	ORBIT_RETRIEVED						(1058, Level.INFO, true, "Orbit {0} retrieved", ""),
	ORBIT_UPDATED						(1059, Level.INFO, true, "Orbit {0} updated", ""),
	ORBIT_DELETED						(1060, Level.INFO, true, "Orbit {0} deleted", ""),
	ORBIT_NOT_MODIFIED					(1061, Level.INFO, true, "Mission with id {0} not modified (no changes)", ""),
	ORDER_NOT_FOUND						(1100, Level.ERROR, false, "No order found for ID {0}", ""),
	DELETION_UNSUCCESSFUL				(1101, Level.ERROR, false, "Order deletion unsuccessful for ID {0}", ""),
	ORDER_MISSING						(1102, Level.ERROR, false, "Order not set", ""),
	ORDER_ID_MISSING					(1103, Level.ERROR, false, "Order ID not set", ""),
	DUPLICATE_ORDER_UUID				(1104, Level.ERROR, false, "Order UUID {0} already exists", ""),
	INVALID_REQUESTED_CLASS				(1105, Level.ERROR, false, "Requested product class {0} is not defined for mission {1}", ""),
	INVALID_INPUT_CLASS					(1106, Level.ERROR, false, "Input product class {0} is not defined for mission {1}", ""),
	INVALID_FILE_CLASS					(1107, Level.ERROR, false, "Output file class {0} is not defined for mission {1}", ""),
	INVALID_PROCESSING_MODE				(1108, Level.ERROR, false, "Processing mode {0} is not defined for mission {1}", ""),
	INVALID_CONFIGURED_PROCESSOR		(1109, Level.ERROR, false, "Configured processor {0} not found", ""),
	INVALID_ORBIT_RANGE					(1110, Level.ERROR, false, "No orbits defined between orbit number {0} and {1} for spacecraft {2}", ""),
	ORDER_IDENTIFIER_MISSING			(1111, Level.ERROR, false, "Order identifier not set", ""),
	DUPLICATE_ORDER_IDENTIFIER			(1112, Level.ERROR, false, "Order identifier {0} already exists within mission {1}", ""),
	ORDER_TIME_INTERVAL_MISSING			(1113, Level.ERROR, false, "Time interval (orbit or time range) missing for order {1}", ""),
	REQUESTED_PRODUCTCLASSES_MISSING	(1114, Level.ERROR, false, "Requested product classes missing for order {0}", ""),
	ORDER_LIST_EMPTY					(1115, Level.ERROR, false, "No processing order found for search criteria", ""),
	INVALID_MISSION_CODE				(1116, Level.ERROR, false, "No mission found for mission code {0}", ""),
	INVALID_OUTPUT_CLASS				(1117, Level.ERROR, false, "Output product class {0} is not defined for mission {1}", ""),
	ILLEGAL_STATE_TRANSITION			(1118, Level.ERROR, false, "Illegal order state transition from {0} to {1}", ""),
	STATE_TRANSITION_FORBIDDEN			(1119, Level.ERROR, false, "Order state transition from {0} to {1} not allowed for user {2}", ""),
	ORDER_MODIFICATION_FORBIDDEN		(1120, Level.ERROR, false, "Order modification other than state change not allowed for user {0}", ""),
	ILLEGAL_ORDER_STATE					(1121, Level.ERROR, false, "Order update only allowed in INITIAL state", ""),
	ILLEGAL_CREATION_STATE				(1122, Level.ERROR, false, "Orders must be created in INITIAL state (found state {0})", ""),
	SLICE_DURATION_MISSING				(1123, Level.ERROR, false, "Time slice duration missing for order {0} of slicing type TIME_SLICE", ""),
	INVALID_SLICE_OVERLAP				(1124, Level.ERROR, false, "Order {0} with slicing type NONE has invalid slice overlap {1} (no overlap allowed)", ""),
	NEGATIVE_DURATION					(1125, Level.ERROR, false, "Order {0} has start time {1} after stop time {2}", ""),
	ORDER_LIST_RETRIEVED				(1126, Level.INFO, true, "Order list of size {0} retrieved for mission {1}, order {2}, start time {3}, stop time {4}", ""),
	ORDER_RETRIEVED						(1127, Level.INFO, true, "Order with ID {0} retrieved", ""),
	ORDER_MODIFIED						(1128, Level.INFO, true, "Order with id {0} modified", ""),
	ORDER_CREATED						(1129, Level.INFO, true, "Order with identifier {0} created for mission {1}", ""),
	ORDER_DELETED						(1130, Level.INFO, true, "Order with id {0} deleted", ""),
	NUMBER_ORDERS_DELETED				(1131, Level.INFO, true, "{0} orders deleted", ""),
	ORDER_NOT_MODIFIED					(1132, Level.INFO, true, "Order with id {0} not modified (no changes)", ""),
	JOF_DELETED							(1133, Level.INFO, true, "Job Order File {0} deleted from processing facility {1}", ""),
	JOF_DELETING_ERROR					(1134, Level.ERROR, false, "Error deleting Job Order File {0} from processing facility {1} (cause: {2})", ""),
	MODEL_ORDER_MISSIONCODE				(1135, Level.INFO, true, "Model order missioncode {0}", ""),
	
	;

	private final int code;
	private final Level level;
	private final boolean success;
	private final String message;
	private final String description;

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
