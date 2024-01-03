/**
 * StorageMgrMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the storage manager.
 *
 * @author Katharina Bassler
 */
public enum StorageMgrMessage implements ProseoMessage {
	
	INTERNAL_ERROR 	  					  (5501, Level.ERROR, false, "Internal Storage Manager  Error: {0}", ""),
	INVALID_PATH 	  					  (5502, Level.ERROR, false, "Invalid path: {0}", ""),
	SECRET_TOO_SHORT  					  (5503, Level.ERROR, false, "Secret length is shorter than the minimum 256-bit requirement, Error: {0}", ""),
	FILE_NOT_FOUND 	  					  (5504, Level.ERROR, false, "File not found: {0}", ""),
	PATH_IS_NULL 	  					  (5505, Level.ERROR, false, "The path is null", ""),
	FILE_NOT_DELETED  					  (5506, Level.ERROR, false, "File {0} cannot be deleted: {1}", ""),
	
	FILE_NOT_DOWNLOADED 			      (5507, Level.ERROR, false, "Cannot download file/folder from {0} to {1} : {2}", ""), 
	FILE_NOT_UPLOADED 					  (5508, Level.ERROR, false, "Cannot upload file/folder from {0} to {1} : {2}" , ""), 

	REST_INFO_GOT 						  (5511, Level.INFO,  true,  "Rest Info Information got", ""),
	STRING_NOT_BASE64_ENCODED   		  (5512, Level.ERROR, false, "Attribute jobOrderStringBase64 is not Base64-encoded", ""),
	
	JOB_ORDER_FILE_UPLOADED 			  (5521, Level.INFO,  true,  "Received & Uploaded joborder-file: {0}", ""),
	JOB_ORDER_CREATION_ERROR 			  (5522, Level.ERROR, false, "Cannot create job order file {0}. Unexpected error", ""), 
	JOB_ORDER_FILE_CANNOT_BE_GOT 		  (5523, Level.ERROR, false, "Cannot get job order file: {0}, {1}",  ""), 
	JOB_ORDER_FILE_GOT 				      (5524, Level.INFO,  true,  "Job order file got: {0}", ""),
	
	TOKEN_MISSING  						  (5531, Level.ERROR, false, "Authentication token missing", ""),
	TOKEN_INVALID 						  (5532, Level.ERROR, false, "Authentication token invalid: {0}, Error: {1}", ""),
	TOKEN_EXPIRED  						  (5533, Level.ERROR, false, "Authentication token expired at: {0}", ""),
	TOKEN_MISMATCH 						  (5534, Level.ERROR, false, "Authentication token not valid for file: {0}", ""),
	
	TOKEN_PAYLOAD_INVALID	  			  (5541, Level.ERROR, false, "The payload of the JWT doesn't represent a valid JSON object and a JWT claims set, Error {0}", ""),
	TOKEN_NOT_VERIFIABLE 	 			  (5542, Level.ERROR, false, "The JWS object couldn't be verified, Error {0}", ""),
	TOKEN_STATE_INVALID 	 			  (5543, Level.ERROR, false, "The JWS object is not in a signed or verified state, actual state: {0}, Error: {1}", ""),
	TOKEN_VERIFICATION_FAILED 		 	  (5544, Level.ERROR, false, "Verification of the JWT failed", ""),
	TOKEN_NOT_PARSEABLE 	  			  (5545, Level.ERROR, false, "Token not parseable, Error: {0}", ""),
	
	/** Uploaded = registered */
	PRODUCTS_UPLOADED_TO_STORAGE 	  	  (5551, Level.INFO,  true,  "Product(s) uploaded to storage. Amount: {0}, uploaded products: {1}", ""), 
	/** Listed = got */
	PRODUCT_FILES_LISTED		      	  (5552, Level.INFO,  true,  "Product files listed: {0}", ""), 
	/** Downloaded = retrieved */
	PRODUCT_FILE_DOWNLOADED 	      	  (5553, Level.INFO,  true,  "Product file downloaded: {0}", ""),  
	/** Uploaded = registered */
	PRODUCT_FILE_UPLOADED 		 		  (5554, Level.INFO,  true,  "Product file {0} uploaded for product {1}", ""),  

	PRODUCT_FILE_CANNOT_BE_DOWNLOADED     (5555, Level.ERROR, false, "Product file cannot be downloaded: {0}", ""),  
	PRODUCT_FILE_PARTIALLY_DOWNLOADED 	  (5556, Level.INFO,  true,  "Product file {0} partially downloaded from byte {1} to byte {2}, {3} bytes transferred", ""),  
	PRODUCT_FILE_DELETED 			  	  (5557, Level.INFO,  true,  "Product file deleted: {0}", ""),
	
	READ_TIME_OUT 				      	  (5561, Level.ERROR, false, "Read for file {0} timed out after {1} seconds. Error {2}", ""), 
	EXCEPTION_IS_NULL 			 	  	  (5562, Level.ERROR, false, "Exception is null", ""),
	ATTEMPTS_WERE_NOT_SUCCESSFUL 	  	  (5563, Level.ERROR, false, "All {0} attempts were not successful: {1}", ""),
	
	CACHE_NO_FILE_FOR_PUTTING_TO_CACHE 	  (5571, Level.ERROR, false, "File can't be put to cache, it does not exist: {0}", ""),
	CACHE_TEMPORARY_FILE_DELETED 		  (5572, Level.INFO,  true,  "Temporary file has been deleted: {0}", ""),
	CACHE_CLEANUP_REPORT				  (5573, Level.INFO,  true,  "Cache cleanup removed {0} entries from file cache in {1} ms", ""),
	CACHE_NOT_ENOUGH_SPACE_AFTER_CLEANING (5574, Level.ERROR, false, "Disk usage {0} exceeds maximum usage {1} after emptying cache", ""),
	CACHE_FILE_NOT_DELETED 				  (5575, Level.WARN,  false, "Cache File was not deleted: {0}", ""),
	
	/** Downloaded = retrieved */
	PRODUCT_FILE_DOWNLOADED_FROM_EXTERNAL_TO_CACHE 	(5581, Level.INFO,  true,  "Product file downloaded from external storage to cache: {0}", ""),  
	/** Uploaded = registered */
	PRODUCT_FILE_UPLOADED_FROM_CACHE_TO_STORAGE     (5582, Level.INFO,  true,  "Product file {0} uploaded from cache to storage for product {1}", ""),  
	
	;

	private final int code;
	private final Level level;
	private final boolean success;
	private final String message;
	private final String description;

	private StorageMgrMessage(int code, Level level, boolean success, String message, String description) {
		this.level = level;
		this.code = code;
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
