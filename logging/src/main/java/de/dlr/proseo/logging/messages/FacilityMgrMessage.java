/**
 * FaciltiyMgrMessage.java
 * 
 * (C) 10 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the facility manager.
 *
 * @author Katharina Bassler
 */
public enum FacilityMgrMessage implements ProseoMessage {
	
	 DELETION_UNSUCCESSFUL		(1000, Level.ERROR, false, "Facility deletion unsuccessful for ID {0}", ""),
	 DUPLICATE_FACILITY			(1001, Level.ERROR, false, "Facility {0} exists already", ""),
	 FACILITY_CREATED			(1002, Level.INFO, true, "Facility with identifier {0} created", ""),
	 FACILITY_DELETED			(1003, Level.INFO, true, "Facility with id {0} deleted", ""),	 
	 FACILITY_HAS_PRODUCTS		(1004, Level.ERROR, false, "Cannot delete facility {0} due to existing products", ""),
	 FACILITY_ID_MISSING		(1005, Level.ERROR, false, "Facility ID not set", ""),
	 FACILITY_LIST_EMPTY		(1006, Level.ERROR, false, "No facilities found for search criteria", ""),
	 FACILITY_LIST_RETRIEVED	(1007, Level.INFO, true, "Facility list of size {0} retrieved for facility ''{1}''", ""),
	 FACILITY_MISSING			(1008, Level.ERROR, false, "Facility not set", ""),
	 FACILITY_MODIFIED			(1009, Level.INFO, true, "Facility with id {0} modified", ""),
	 FACILITY_NOT_FOUND			(1010, Level.ERROR, false, "No facility found for ID {0}", ""),
	 FACILITY_NOT_MODIFIED		(1011, Level.INFO, true, "Facility with id {0} not modified (no changes)", ""),
	 FACILITY_RETRIEVED			(1012, Level.INFO, true, "Facility with ID {0} retrieved", ""),
	
	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private FacilityMgrMessage(int code, Level level, boolean success, String message, String description) {
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
