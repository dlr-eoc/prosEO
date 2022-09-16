/**
 * FaciltiyMgrMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the facility manager.
 *
 * @author Katharina Bassler
 */
public enum FacilityMgrMessage implements ProseoMessage {
	
	 FACILITY_NOT_FOUND			(1014, Level.ERROR, "No facility found for ID {0}", ""),
	 DELETION_UNSUCCESSFUL		(1015, Level.ERROR, "Facility deletion unsuccessful for ID {0}", ""),
	 FACILITY_MISSING			(1016, Level.ERROR, "Facility not set", ""),
	 FACILITY_DELETED			(1017, Level.INFO, "Facility with id {0} deleted", ""),	 
	 FACILITY_RETRIEVED			(1018, Level.INFO, "Facility with ID {0} retrieved", ""),
	 FACILITY_MODIFIED			(1019, Level.INFO, "Facility with id {0} modified", ""),
	 FACILITY_NOT_MODIFIED		(1020, Level.INFO, "Facility with id {0} not modified (no changes)", ""),
	 FACILITY_CREATED			(1021, Level.INFO, "Facility with identifier {0} created", ""),
	 FACILITY_LIST_EMPTY		(1022, Level.ERROR, "No facilities found for search criteria", ""),
	 FACILITY_LIST_RETRIEVED	(1023, Level.INFO, "Facility list of size {0} retrieved for facility '{1}'", ""),
	 DUPLICATE_FACILITY			(1024, Level.ERROR, "Facility {0} exists already", ""),
	 FACILITY_HAS_PRODUCTS		(1025, Level.ERROR, "Cannot delete facility {0} due to existing products", ""),
	 FACILITY_ID_MISSING		(1026, Level.ERROR, "Facility ID not set", ""),
	
	;

	private final int code;
	private final Level level;
	private final String message;
	private final String description;

	private FacilityMgrMessage(int code, Level level, String message, String description) {
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
