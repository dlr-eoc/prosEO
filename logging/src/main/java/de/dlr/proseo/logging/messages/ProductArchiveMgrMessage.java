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
	
	 DELETION_UNSUCCESSFUL		(4800, Level.ERROR, false, "Product Archive deletion unsuccessful for ID {0}", ""),
	 DUPLICATED_ARCHIVE			(4801, Level.ERROR, false, "Product Archive {0} exists already", ""),
	 ARCHIVE_CREATED			(4802, Level.INFO,  true,  "Product Archive with identifier {0} created", ""),
	 ARCHIVE_DELETED			(4803, Level.INFO,  true,  "Product Archive with id {0} deleted", ""),	 
	 ARCHIVE_HAS_PRODUCTS		(4804, Level.ERROR, false, "Cannot delete Product Archive {0} due to existing products", ""),
	 ARCHIVE_ID_MISSING			(4805, Level.ERROR, false, "Product Archive ID not set", ""),
	 ARCHIVE_LIST_EMPTY			(4806, Level.ERROR, false, "No Product Archives found for search criteria", ""),
	 ARCHIVE_LIST_RETRIEVED		(4807, Level.INFO,  true,  "Product Archive list of size {0} retrieved for facility ''{1}''", ""),
	 ARCHIVE_MISSING			(4808, Level.ERROR, false, "Product Archive not set", ""),
	 ARCHIVE_MODIFIED			(4809, Level.INFO,  true,  "Product Archive with id {0} modified", ""),
	 ARCHIVE_NOT_FOUND			(4810, Level.ERROR, false, "No Product Archive found for ID {0}", ""),
	 ARCHIVE_NOT_MODIFIED		(4811, Level.INFO,  true,  "Product Archive with id {0} not modified (no changes)", ""),
	 ARCHIVE_RETRIEVED			(4812, Level.INFO,  true,  "Product Archive with ID {0} retrieved", ""),
	 ARCHIVE_TYPE_WRONG			(4813, Level.ERROR, false, "Product Archive Type is wrong: {0}", ""),
	 DUPLICATED_PRODUCT_CLASS	(4814, Level.ERROR, false, "Duplicated Product Class {0} in Product Archive {1}", ""),
	 PRODUCT_CLASS_NOT_FOUND    (4815, Level.ERROR, false, "Product Class with code {0} and type {1} not found", ""),
	 
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
