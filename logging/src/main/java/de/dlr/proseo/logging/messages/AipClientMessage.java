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

	INVALID_FACILITY					(5401, Level.ERROR, false, "Invalid processing facility {0} for input data storage", ""),
	INPUT_FILE_NOT_FOUND	 			(5402, Level.ERROR, false, "Input file {0} not found locally or in any external archive", ""),
	INVALID_ODATA_RESPONSE	 			(5403, Level.ERROR, false, "Invalid OData response ''{0}'' from external archive {1}", ""),
	PRODUCT_NOT_FOUND_BY_NAME	 		(5404, Level.INFO, true, "Product file {0} not found in external archive {1}", ""),
	MULTIPLE_PRODUCTS_FOUND_BY_NAME	 	(5405, Level.WARN, true, "Multiple files with name {0} found in external archive {1}, using first one", ""),
	CHECKSUM_INVALID				 	(5406, Level.WARN, true, "Checksum invalid in OData response {0}", ""),
	FILESIZE_INVALID				 	(5407, Level.WARN, true, "File size invalid in OData response {0}", ""),
	MANDATORY_ELEMENT_MISSING			(5408, Level.ERROR, false, "Mandatory element missing in OData response {0}", ""),
	DATE_NOT_PARSEABLE					(5409, Level.ERROR, false, "Date attribute not parseable in OData response {0}", ""),
	WAITING_FOR_PRODUCT_ORDER			(5410, Level.INFO, true, "Product order {0} in state {1}, waiting for completion ...", ""),
	ORDER_WAIT_INTERRUPTED				(5411, Level.ERROR, false, "Wait for product order {0} interrupted, product download failed", ""),
	PRODUCT_ORDER_COMPLETED				(5412, Level.INFO, true, "Product order {0} completed", ""),
	FILE_NOT_WRITABLE					(5413, Level.ERROR, false, "Cannot write product file {0}", ""),
	PRODUCT_DOWNLOAD_FAILED				(5414, Level.ERROR, false, "Download of product file {0} failed (cause: {1})", ""),
	FILE_SIZE_MISMATCH					(5415, Level.ERROR, false, "File size mismatch for product file {0} (expected: {1} Bytes, got {2} Bytes)", ""),
	CHECKSUM_MISMATCH					(5416, Level.ERROR, false, "Checksum mismatch for product file {0} (expected: {1}, got {2})", ""),
	PRODUCT_TRANSFER_COMPLETED			(5417, Level.INFO, true, "Transfer completed: |{0}|{1}|{2}|{3}|{4}|", ""),
	ERROR_CONVERTING_INGESTOR_PRODUCT	(5418, Level.ERROR, false, "Error converting ingestor product of class {0} to JSON (cause: {1})", ""),
	ERROR_REGISTERING_PRODUCT			(5419, Level.ERROR, false, "Error registering product of class {0} with Ingestor (HTTP status code: {1}, message: {2})", ""),
	PRODUCT_REGISTERED					(5420, Level.INFO, true, "Product {0} registered with Ingestor", ""),
	ODATA_REQUEST_ABORTED				(5421, Level.ERROR, false, "OData request {0} aborted (cause: {1} / {2})", ""),
	ODATA_REQUEST_FAILED				(5422, Level.ERROR, false, "OData request {0} failed with HTTP status code {1}, message:\n{2}\n", ""),
	ODATA_RESPONSE_UNREADABLE			(5423, Level.ERROR, false, "OData response not readable", ""),
	RETRIEVAL_RESULT					(5424, Level.INFO, true, "Retrieval request returned {0} products out of {1} available", ""),
	PRODUCT_UUID_MISSING				(5425, Level.ERROR, false, "Product list entry {0} does not contain product UUID ('Id' element)", ""),
	PRODUCT_FILENAME_MISSING			(5426, Level.ERROR, false, "Product list entry {0} does not contain product filename ('Name' element)", ""),
	PRODUCT_SIZE_MISSING				(5427, Level.ERROR, false, "Product list entry {0} does not contain product size ('ContentLength' element)", ""),
	PRODUCT_HASH_MISSING				(5428, Level.ERROR, false, "Product list entry {0} does not contain product checksum ('Checksum/Value' element)", ""),
	PRODUCT_VAL_START_MISSING			(5429, Level.ERROR, false, "Product list entry {0} does not contain product validity start ('ContentDate/Start' element)", ""),
	PRODUCT_VAL_STOP_MISSING			(5430, Level.ERROR, false, "Product list entry {0} does not contain product validity end ('ContentDate/End' element)", ""),
	PRODUCT_PUBLICATION_MISSING			(5431, Level.ERROR, false, "Product list entry {0} does not contain valid publication time ('PublicationDate' element)", ""),
	PRODUCT_EVICTION_MISSING			(5432, Level.ERROR, false, "Product list entry {0} does not contain valid eviction time ('EvictionDate' element)", ""),
	PRODUCT_ATTRIBUTES_MISSING			(5433, Level.ERROR, false, "Product list entry {0} does not contain attribute list ('Attributes' element)", ""),
	ORDER_DATA_MISSING					(5434, Level.ERROR, false, "Order list entry {0} does not contain mandatory values ('Id' and/or 'Status' elements)", ""),
	ORDER_REQUEST_FAILED				(5435, Level.ERROR, false, "Product order request to external API {0} failed", ""),
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
