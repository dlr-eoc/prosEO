/**
 * ApiMonitorMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the ESA API Monitor services.
 *
 * @author Dr. Thomas Bassler
 */
public enum ApiMonitorMessage implements ProseoMessage {
	
	// -- Base Monitor --
	INTERRUPTED					(7000, Level.INFO, true, "Interrupt received while waiting for next check of pickup point", ""),
	TRANSFER_FAILED				(7001, Level.ERROR, false, "Transfer of object {0} failed", ""),
	FOLLOW_ON_ACTION_FAILED		(7002, Level.ERROR, false, "Follow-on action for object {0} failed", ""),
	HISTORY_READ_FAILED			(7003, Level.ERROR, false, "Failed to read transfer history file {0} (cause: {1})", ""),	 
	HISTORY_WRITE_FAILED		(7004, Level.ERROR, false, "Failed to write transfer history file {0} (cause: {1})", ""),
	ABORTING_MONITOR			(7005, Level.ERROR, false, "Aborting monitor due to IOException (cause: {0})", ""),
	ILLEGAL_HISTORY_ENTRY_FORMAT (7006, Level.ERROR, false, "Transfer history entry ''{0}'' has illegal format", ""),
	ILLEGAL_HISTORY_ENTRY_DATE	(7007, Level.ERROR, false, "Transfer history entry date ''{0}'' has illegal format", ""),
	HISTORY_ENTRIES_READ		(7008, Level.INFO, true, "{0} history entries read from history file {1}, reference time for next pickup point lookup is {2}", ""),
	HISTORY_ENTRIES_TRUNCATED	(7009, Level.INFO, true, "{0} entries truncated from transfer history file {1}", ""),
	TASK_WAIT_INTERRUPTED		(7010, Level.ERROR, false, "Wait for task completion interrupted, monitoring loop aborted", ""),
	SUBTASK_TIMEOUT				(7011, Level.ERROR, false, "Timeout after {0} s during wait for task completion, task cancelled", ""),
	ABORTING_TASK				(7012, Level.ERROR, false, "Aborting download task due to exception (cause: {0})", ""),
	EXCEPTION_CHECKING_DOWNLOADS (7013, Level.ERROR, false, "Exception during check for available downloads (cause: {0} / {1})", ""),
	EXCEPTION_IN_TRANSFER_OR_ACTION (7014, Level.ERROR, false, "Exception during data transfer or follow-on action (cause: {0} / {1})", ""),

	// -- AUXIP/CADIP Monitor --
	INVALID_AUXIP_ID			(7100, Level.ERROR, false, "Invalid AUXIP Monitor identifier {0} passed", ""),
	ODATA_REQUEST_ABORTED		(7101, Level.ERROR, false, "OData request for reference time {0} aborted (cause: {1} / {2})", ""),
	ODATA_REQUEST_FAILED		(7102, Level.ERROR, false, "OData request for reference time {0} failed with HTTP status code {1}, message:\n{2}\n", ""),
	ODATA_RESPONSE_UNREADABLE	(7103, Level.ERROR, false, "OData response not readable", ""),
	RETRIEVAL_RESULT			(7104, Level.INFO, true, "Retrieval request returned {0} products out of {1} available", ""),
	PRODUCT_UUID_MISSING		(7105, Level.ERROR, false, "Product list entry {0} does not contain product UUID ('Id' element)", ""),
	PRODUCT_FILENAME_MISSING	(7106, Level.ERROR, false, "Product list entry {0} does not contain product filename ('Name' element)", ""),
	PRODUCT_SIZE_MISSING		(7107, Level.ERROR, false, "Product list entry {0} does not contain product size ('ContentLength' element)", ""),
	PRODUCT_HASH_MISSING		(7108, Level.ERROR, false, "Product list entry {0} does not contain product checksum ('Checksum/Value' element)", ""),
	PRODUCT_VAL_START_MISSING	(7109, Level.ERROR, false, "Product list entry {0} does not contain product validity start ('ContentDate/Start' element)", ""),
	PRODUCT_VAL_STOP_MISSING	(7110, Level.ERROR, false, "Product list entry {0} does not contain product validity end ('ContentDate/End' element)", ""),
	PRODUCT_PUBLICATION_MISSING	(7111, Level.ERROR, false, "Product list entry {0} does not contain valid publication time ('PublicationDate' element)", ""),
	PRODUCT_EVICTION_MISSING	(7112, Level.ERROR, false, "Product list entry {0} does not contain valid eviction time ('EvictionDate' element)", ""),
	INVALID_CADIP_ID			(7113, Level.ERROR, false, "Invalid CADIP Monitor identifier {0} passed", ""),
	
	// -- EDIP Monitor --
	EDIP_NOT_READABLE			(7150, Level.ERROR, false, "EDIP directory {0} not readable (cause: {1})", ""),
	EDIP_ENTRY_MALFORMED		(7151, Level.WARN, true, "Malformed EDIP directory entry {0} found - skipped", ""),
	INVALID_EDIP_ID				(7152, Level.ERROR, false, "Invalid EDIP Monitor identifier {0} passed", ""),
	
	EDIP_START_MESSAGE			(7199, Level.INFO, true, "------  Starting EDIP Monitor  ------\n"
															+ "EDIP directory . . . . . . : {0}\n"
															+ "Satellite  . . . . . . . . : {1}\n"
															+ "Transfer history file  . . : {2}\n"
															+ "EDIP check interval  . . . : {3}\n"
															+ "History truncation interval: {4}\n"
															+ "History retention period . : {5}\n"
															+ "CADU target directory  . . : {6}\n"
															+ "L0 processor command . . . : {7}\n"
															+ "Max. transfer sessions . . : {8}\n"
															+ "Transfer session wait time : {9}\n"
															+ "Max. session wait cycles . : {10}\n"
															+ "Max. file download threads : {11}\n"
															+ "File download wait time  . : {12}\n"
															+ "Max. file wait cycles  . . : {13}"
															,""),
	
	// -- XBIP Monitor --
	XBIP_NOT_READABLE			(7200, Level.ERROR, false, "XBIP directory {0} not readable (cause: {1})", ""),
	TRANSFER_OBJECT_IS_NULL		(7201, Level.ERROR, false, "Transfer object is null - skipped", ""),
	INVALID_TRANSFER_OBJECT_TYPE (7202, Level.ERROR, false, "Transfer object {0} of invalid type found - skipped", ""),
	CANNOT_CREATE_TARGET_DIR	(7203, Level.ERROR, false, "Cannot create channel directory in target directory {0} - skipped", ""),
	COPY_FAILED					(7204, Level.ERROR, false, "Copying of session directory {0} failed (cause: {1})", ""),
	COPY_FILE_FAILED			(7205, Level.ERROR, false, "Copying of session data file {0} failed (cause: {1})", ""),
	COPY_INTERRUPTED			(7206, Level.ERROR, false, "Copying of session directory {0} failed due to interrupt", ""),
	COMMAND_START_FAILED		(7207, Level.ERROR, false, "Start of L0 processing command ''{0}'' failed (cause: {1})", ""),
	CANNOT_READ_DSIB_FILE		(7208, Level.ERROR, false, "Cannot read DSIB file {0} (cause: {1})", ""),
	DATA_SIZE_MISMATCH			(7209, Level.ERROR, false, "Data size mismatch copying session directory {0}: "
															+ "expected size {1}, actual size {2}", ""),
	COPY_TIMEOUT				(7210, Level.ERROR, false, "Timeout after {0} s during wait for download of file {1}, download cancelled", ""),
	XBIP_ENTRY_MALFORMED		(7211, Level.WARN, true, "Malformed XBIP directory entry {0} found - skipped", ""),
	SKIPPING_SESSION_DIRECTORY	(7212, Level.WARN, true, "Skipping inaccessible session directory {0}", ""),
	AVAILABLE_DOWNLOADS_FOUND	(7213, Level.INFO, true, "{0} session entries found for download (unfiltered)", ""),
	SESSION_TRANSFER_INCOMPLETE	(7214, Level.INFO, true, "Transfer for session {0} still incomplete - skipped", ""),
	SESSION_TRANSFER_COMPLETED	(7215, Level.INFO, true, "Transfer for session {0} completed with result {1}", ""),
	SKIPPING_SESSION_FOR_DELAY	(7216, Level.INFO, true, "Waiting for retrieval delay for session {0} to expire - skipped", ""),
	FOLLOW_ON_ACTION_STARTED	(7217, Level.INFO, true, "Follow-on action for session {0} started with command {1}", ""),
	INVALID_XBIP_ID				(7218, Level.ERROR, true, "Invalid XBIP Monitor identifier {0} passed", ""),
	
	XBIP_START_MESSAGE			(7299, Level.INFO, true, "------  Starting XBIP Monitor  ------\n"
															+ "XBIP directory . . . . . . : {0}\n"
															+ "Satellite  . . . . . . . . : {1}\n"
															+ "Transfer history file  . . : {2}\n"
															+ "XBIP check interval  . . . : {3}\n"
															+ "History truncation interval: {4}\n"
															+ "History retention period . : {5}\n"
															+ "CADU target directory  . . : {6}\n"
															+ "L0 processor command . . . : {7}\n"
															+ "Max. transfer sessions . . : {8}\n"
															+ "Transfer session wait time : {9}\n"
															+ "Max. session wait cycles . : {10}\n"
															+ "Max. file download threads : {11}\n"
															+ "File download wait time  . : {12}\n"
															+ "Max. file wait cycles  . . : {13}"
															,""),
	
	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private ApiMonitorMessage(int code, Level level, boolean success, String message, String description) {
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
