/**
 * FaciltiyMgrMessage.java
 * 
 * (C) 10 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the product archive manager.
 *
 * @author Katharina Bassler
 * @author Denys Chaykovskiy
 * 
 */
public enum ProductArchiveMgrMessage implements ProseoMessage {
	
	 DELETION_UNSUCCESSFUL		(7500, Level.ERROR, false, "Product Archive deletion unsuccessful for ID {0}", ""),
	 DUPLICATED_ARCHIVE			(7501, Level.ERROR, false, "Product Archive {0} exists already", ""),
	 ARCHIVE_CREATED			(7502, Level.INFO,  true,  "Product Archive with identifier {0} created", ""),
	 ARCHIVE_DELETED			(7503, Level.INFO,  true,  "Product Archive with id {0} deleted", ""),	 
	 ARCHIVE_HAS_PRODUCTS		(7504, Level.ERROR, false, "Cannot delete Product Archive {0} due to existing products", ""),
	 ARCHIVE_ID_MISSING			(7505, Level.ERROR, false, "Product Archive ID not set", ""),
	 ARCHIVE_LIST_EMPTY			(7506, Level.ERROR, false, "No Product Archives found for search criteria", ""),
	 ARCHIVE_LIST_RETRIEVED		(7507, Level.INFO,  true,  "Product Archive list of size {0} retrieved for archive ''{1}''", ""),
	 ARCHIVE_MISSING			(7508, Level.ERROR, false, "Product Archive not set", ""),
	 ARCHIVE_MODIFIED			(7509, Level.INFO,  true,  "Product Archive with id {0} modified", ""),
	 ARCHIVE_NOT_FOUND			(7510, Level.ERROR, false, "No Product Archive found for ID {0}", ""),
	 ARCHIVE_NOT_MODIFIED		(7511, Level.INFO,  true,  "Product Archive with id {0} not modified (no changes)", ""),
	 ARCHIVE_RETRIEVED			(7512, Level.INFO,  true,  "Product Archive with ID {0} retrieved", ""),
	 ARCHIVE_TYPE_WRONG			(7513, Level.ERROR, false, "Product Archive Type is wrong: {0}", ""),
	 DUPLICATED_PRODUCT_CLASS	(7514, Level.ERROR, false, "Duplicated Product Class {0} in Product Archive {1}", ""),
	 PRODUCT_CLASS_NOT_FOUND    (7515, Level.ERROR, false, "Product Class with code {0} and type {1} not found", ""),
	 ARCHIVE_CODE_MISSING		(7516, Level.ERROR, false, "Product Archive code not set", ""),
	 ARCHIVE_MISSION_MISSING	(7517, Level.ERROR, false, "Mission for Product Archive not set", ""),

	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private ProductArchiveMgrMessage(int code, Level level, boolean success, String message, String description) {
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
