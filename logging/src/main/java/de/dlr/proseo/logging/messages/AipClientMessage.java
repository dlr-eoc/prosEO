/**
 * AipClientMessage.java
 * 
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the AIP Client.
 *
 * @author Dr. THomas Bassler
 */
public enum AipClientMessage implements ProseoMessage {

	INVALID_FACILITY					(6801, Level.ERROR, false, "Invalid processing facility {0} for input data storage", ""),
	INPUT_FILE_NOT_FOUND	 			(6802, Level.ERROR, false, "Input file {0} not found locally or in any external archive", ""),
	INVALID_ODATA_RESPONSE	 			(6803, Level.ERROR, false, "Invalid OData response ''{0}'' from external archive {1}", ""),
	PRODUCT_NOT_FOUND_BY_NAME	 		(6804, Level.INFO, true, "Product file {0} not found in external archive {1}", ""),
	MULTIPLE_PRODUCTS_FOUND_BY_NAME	 	(6805, Level.WARN, true, "Multiple files with name {0} found in external archive {1}, using first one", ""),
	CHECKSUM_INVALID				 	(6806, Level.WARN, true, "Checksum invalid in OData response {0}", ""),
	FILESIZE_INVALID				 	(6807, Level.WARN, true, "File size invalid in OData response {0}", ""),
	MANDATORY_ELEMENT_MISSING			(6808, Level.ERROR, false, "Mandatory element missing in OData response {0}", ""),
	DATE_NOT_PARSEABLE					(6809, Level.ERROR, false, "Date attribute not parseable in OData response {0}", ""),
	WAITING_FOR_PRODUCT_ORDER			(6810, Level.INFO, true, "Product order {0} in state {1}, waiting for completion ...", ""),
	ORDER_WAIT_INTERRUPTED				(6811, Level.ERROR, false, "Wait for product order {0} interrupted, product download failed", ""),
	PRODUCT_ORDER_COMPLETED				(6812, Level.INFO, true, "Product order {0} completed", ""),
	FILE_NOT_WRITABLE					(6813, Level.ERROR, false, "Cannot write product file {0}", ""),
	PRODUCT_DOWNLOAD_FAILED				(6814, Level.ERROR, false, "Download of product file {0} failed (cause: {1})", ""),
	FILE_SIZE_MISMATCH					(6815, Level.ERROR, false, "File size mismatch for product file {0} (expected: {1} Bytes, got {2} Bytes)", ""),
	CHECKSUM_MISMATCH					(6816, Level.ERROR, false, "Checksum mismatch for product file {0} (expected: {1}, got {2})", ""),
	PRODUCT_TRANSFER_COMPLETED			(6817, Level.INFO, true, "Transfer completed: |{0}|{1}|{2}|{3}|{4}|", ""),
	ERROR_CONVERTING_INGESTOR_PRODUCT	(6818, Level.ERROR, false, "Error converting ingestor product of class {0} to JSON (cause: {1})", ""),
	ERROR_REGISTERING_PRODUCT			(6819, Level.ERROR, false, "Error registering product of class {0} with Ingestor (HTTP status code: {1}, message: {2})", ""),
	PRODUCT_REGISTERED					(6820, Level.INFO, true, "Product {0} registered with Ingestor", ""),
	ODATA_REQUEST_ABORTED				(6821, Level.ERROR, false, "OData request {0} aborted (cause: {1} / {2})", ""),
	ODATA_REQUEST_FAILED				(6822, Level.ERROR, false, "OData request {0} failed with HTTP status code {1}, message:\n{2}\n", ""),
	ODATA_RESPONSE_UNREADABLE			(6823, Level.ERROR, false, "OData response not readable", ""),
	RETRIEVAL_RESULT					(6824, Level.INFO, true, "Retrieval request returned {0} products out of {1} available", ""),
	PRODUCT_UUID_MISSING				(6825, Level.ERROR, false, "Product list entry {0} does not contain product UUID ('Id' element)", ""),
	PRODUCT_FILENAME_MISSING			(6826, Level.ERROR, false, "Product list entry {0} does not contain product filename ('Name' element)", ""),
	PRODUCT_SIZE_MISSING				(6827, Level.ERROR, false, "Product list entry {0} does not contain product size ('ContentLength' element)", ""),
	PRODUCT_HASH_MISSING				(6828, Level.ERROR, false, "Product list entry {0} does not contain product checksum ('Checksum/Value' element)", ""),
	PRODUCT_VAL_START_MISSING			(6829, Level.ERROR, false, "Product list entry {0} does not contain product validity start ('ContentDate/Start' element)", ""),
	PRODUCT_VAL_STOP_MISSING			(6830, Level.ERROR, false, "Product list entry {0} does not contain product validity end ('ContentDate/End' element)", ""),
	PRODUCT_PUBLICATION_MISSING			(6831, Level.ERROR, false, "Product list entry {0} does not contain valid publication time ('PublicationDate' element)", ""),
	PRODUCT_EVICTION_MISSING			(6832, Level.ERROR, false, "Product list entry {0} does not contain valid eviction time ('EvictionDate' element)", ""),
	PRODUCT_ATTRIBUTES_MISSING			(6833, Level.ERROR, false, "Product list entry {0} does not contain attribute list ('Attributes' element)", ""),
	ORDER_DATA_MISSING					(6834, Level.ERROR, false, "Order list entry {0} does not contain mandatory values ('Id' and/or 'Status' elements)", ""),
	ORDER_REQUEST_FAILED				(6835, Level.ERROR, false, "Product order request to external API {0} failed", ""),
	PRODUCT_DOWNLOAD_ONGOING			(6836, Level.INFO, true, "Download for product {0} already ongoing, skipping download and ingestion", ""),
	INVALID_PRODUCT_TYPE				(6837, Level.ERROR, false, "Invalid product type {1} for mission {0}", ""),
	INVALID_SENSING_TIME				(6838, Level.ERROR, false, "Invalid sensing time: {0}", ""),
	PRODUCT_NOT_FOUND_BY_TIME	 		(6839, Level.INFO, true, "Product file of type {0} with sensing time interval {1} – {2} not found in external archive {3}", ""),
	MULTIPLE_PRODUCTS_FOUND_BY_TIME	 	(6840, Level.WARN, true, "Multiple files of type {0} with sensing time interval {1} – {2} found in external archive {3}, using first one", ""),
	INPUT_FILE_NOT_FOUND_BY_TIME		(6841, Level.ERROR, false, "No input file of type {0} with sensing time interval {1} – {2} found locally or in any external archive", ""),
	NO_PRODUCTS_FOUND_BY_TIME	 		(6842, Level.INFO, true, "No product files of type {0} intersecting sensing time interval {1} – {2} found in external archive {3}", ""),
	PRODUCT_TYPE_MISMATCH				(6843, Level.WARN, true, "Product type {0} of archive product does not match requested type {1}, will be replaced", "")
	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private AipClientMessage(int code, Level level, boolean success, String message, String description) {
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
	 * Get the message''s success.
	 * 
	 * @return The message's success.
	 */
	public boolean getSuccess() {
		return success;
	}

}
