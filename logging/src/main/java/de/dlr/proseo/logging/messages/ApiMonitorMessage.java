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
	INTERRUPTED						(7000, Level.INFO, true, "Interrupt received while waiting for next check of pickup point", ""),
	TRANSFER_FAILED					(7001, Level.ERROR, false, "Transfer of object {0} failed", ""),
	FOLLOW_ON_ACTION_FAILED			(7002, Level.ERROR, false, "Follow-on action for object {0} failed", ""),
	HISTORY_READ_FAILED				(7003, Level.ERROR, false, "Failed to read transfer history file {0} (cause: {1})", ""),	 
	HISTORY_WRITE_FAILED			(7004, Level.ERROR, false, "Failed to write transfer history file {0} (cause: {1})", ""),
	ABORTING_MONITOR				(7005, Level.ERROR, false, "Aborting monitor due to IOException (cause: {0})", ""),
	ILLEGAL_HISTORY_ENTRY_FORMAT 	(7006, Level.ERROR, false, "Transfer history entry ''{0}'' has illegal format", ""),
	ILLEGAL_HISTORY_ENTRY_DATE		(7007, Level.ERROR, false, "Transfer history entry date ''{0}'' has illegal format", ""),
	HISTORY_ENTRIES_READ			(7008, Level.INFO, true, "{0} history entries read from history file {1}, reference time for next pickup point lookup is {2}", ""),
	HISTORY_ENTRIES_TRUNCATED		(7009, Level.INFO, true, "{0} entries truncated from transfer history file {1}", ""),
	TASK_WAIT_INTERRUPTED			(7010, Level.ERROR, false, "Wait for task completion interrupted, monitoring loop aborted", ""),
	SUBTASK_TIMEOUT					(7011, Level.ERROR, false, "Timeout after {0} s during wait for task completion, task cancelled", ""),
	ABORTING_TASK					(7012, Level.ERROR, false, "Aborting download task due to exception (cause: {0})", ""),
	EXCEPTION_CHECKING_DOWNLOADS	(7013, Level.ERROR, false, "Exception during check for available downloads (cause: {0} / {1})", ""),
	EXCEPTION_IN_TRANSFER_OR_ACTION (7014, Level.ERROR, false, "Exception during data transfer or follow-on action (cause: {0} / {1})", ""),

	// -- AUXIP/CADIP Monitor --
	INVALID_AUXIP_ID			(7015, Level.ERROR, false, "Invalid AUXIP Monitor identifier {0} passed", ""),
	ODATA_REQUEST_ABORTED		(7016, Level.ERROR, false, "OData request for reference time {0} aborted (cause: {1} / {2})", ""),
	ODATA_REQUEST_FAILED		(7017, Level.ERROR, false, "OData request for reference time {0} failed with HTTP status code {1}, message:\n{2}\n", ""),
	ODATA_RESPONSE_UNREADABLE	(7018, Level.ERROR, false, "OData response not readable", ""),
	RETRIEVAL_RESULT			(7019, Level.INFO, true, "Retrieval request returned {0} products out of {1} available", ""),
	PRODUCT_UUID_MISSING		(7020, Level.ERROR, false, "Product list entry {0} does not contain product UUID ('Id' element)", ""),
	PRODUCT_FILENAME_MISSING	(7021, Level.ERROR, false, "Product list entry {0} does not contain product filename ('Name' element)", ""),
	PRODUCT_SIZE_MISSING		(7022, Level.ERROR, false, "Product list entry {0} does not contain product size ('ContentLength' element)", ""),
	PRODUCT_HASH_MISSING		(7023, Level.ERROR, false, "Product list entry {0} does not contain product checksum ('Checksum/Value' element)", ""),
	PRODUCT_VAL_START_MISSING	(7024, Level.ERROR, false, "Product list entry {0} does not contain product validity start ('ContentDate/Start' element)", ""),
	PRODUCT_VAL_STOP_MISSING	(7025, Level.ERROR, false, "Product list entry {0} does not contain product validity end ('ContentDate/End' element)", ""),
	PRODUCT_PUBLICATION_MISSING	(7026, Level.ERROR, false, "Product list entry {0} does not contain valid publication time ('PublicationDate' element)", ""),
	PRODUCT_EVICTION_MISSING	(7027, Level.ERROR, false, "Product list entry {0} does not contain valid eviction time ('EvictionDate' element)", ""),
	TARGET_DIR_NOT_WRITABLE		(7028, Level.ERROR, false, "Target directory {0} not writable", ""),
	ODATA_SESSION_REQ_ABORTED	(7029, Level.ERROR, false, "OData request for files of session {0} aborted (cause: {1} / {2})", ""),
	ODATA_SESSION_REQ_FAILED	(7030, Level.ERROR, false, "OData request for files of session {0} failed with HTTP status code {1}, message:\n{2}\n", ""),
	SESSION_RETRIEVAL_RESULT	(7031, Level.INFO, true, "Session file list request returned {0} files out of {1} available", ""),
	
	// -- EDIP Monitor --
	EDIP_NOT_READABLE			(7050, Level.ERROR, false, "EDIP directory {0} not readable (cause: {1})", ""),
	EDIP_ENTRY_MALFORMED		(7051, Level.WARN, true, "Malformed EDIP directory entry {0} found - skipped", ""),
	INVALID_EDIP_ID				(7052, Level.ERROR, false, "Invalid EDIP Monitor identifier {0} passed", ""),
	
	EDIP_START_MESSAGE			(7099, Level.INFO, true, "------  Starting EDIP Monitor  ------\n"
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
	
	// -- XBIP/CADIP Monitor --
	XBIP_NOT_READABLE			(7100, Level.ERROR, false, "XBIP directory {0} not readable (cause: {1})", ""),
	TRANSFER_OBJECT_IS_NULL		(7101, Level.ERROR, false, "Transfer object is null - skipped", ""),
	INVALID_TRANSFER_OBJECT_TYPE (7102, Level.ERROR, false, "Transfer object {0} of invalid type found - skipped", ""),
	CANNOT_CREATE_TARGET_DIR	(7103, Level.ERROR, false, "Cannot create channel directory in target directory {0} - skipped", ""),
	COPY_FAILED					(7104, Level.ERROR, false, "Copying of session directory {0} failed (cause: {1})", ""),
	COPY_FILE_FAILED			(7105, Level.ERROR, false, "Copying of session data file {0} failed (cause: {1})", ""),
	COPY_INTERRUPTED			(7106, Level.ERROR, false, "Copying of session directory {0} failed due to interrupt", ""),
	COMMAND_START_FAILED		(7107, Level.ERROR, false, "Start of L0 processing command ''{0}'' failed (cause: {1})", ""),
	CANNOT_READ_DSIB_FILE		(7108, Level.ERROR, false, "Cannot read DSIB file {0} (cause: {1})", ""),
	DATA_SIZE_MISMATCH			(7109, Level.ERROR, false, "Data size mismatch copying session directory {0}: "
															+ "expected size {1}, actual size {2}", ""),
	COPY_TIMEOUT				(7110, Level.ERROR, false, "Timeout after {0} s during wait for download of file {1}, download cancelled", ""),
	XBIP_ENTRY_MALFORMED		(7111, Level.WARN, true, "Malformed XBIP directory entry {0} found - skipped", ""),
	SKIPPING_SESSION_DIRECTORY	(7112, Level.WARN, true, "Skipping inaccessible session directory {0}", ""),
	AVAILABLE_DOWNLOADS_FOUND	(7113, Level.INFO, true, "{0} session entries found for download (unfiltered)", ""),
	SESSION_TRANSFER_INCOMPLETE	(7114, Level.INFO, true, "Transfer for session {0} still incomplete - skipped", ""),
	SESSION_TRANSFER_COMPLETED	(7115, Level.INFO, true, "Transfer for session {0} completed with result {1}", ""),
	SKIPPING_SESSION_FOR_DELAY	(7116, Level.INFO, true, "Waiting for retrieval delay for session {0} to expire - skipped", ""),
	FOLLOW_ON_ACTION_STARTED	(7117, Level.INFO, true, "Follow-on action for session {0} started with command {1}", ""),
	INVALID_XBIP_ID				(7118, Level.ERROR, true, "Invalid XBIP Monitor identifier {0} passed", ""),
	INVALID_CADIP_ID			(7119, Level.ERROR, false, "Invalid CADIP Monitor identifier {0} passed", ""),
	SESSION_ELEMENT_MISSING		(7120, Level.ERROR, false, "Session list entry {0} does not contain ''{1}'' element", ""),
	DOWNLOAD_INTERRUPTED		(7124, Level.ERROR, false, "Download of session {0} failed due to interrupt", ""),
	DOWNLOAD_TIMEOUT			(7125, Level.ERROR, false, "Timeout after {0} s during wait for download of session {1}, download cancelled", ""),
	DOWNLOAD_FAILED				(7126, Level.ERROR, false, "Download of DSDB file {0} failed (cause: {1})", ""),
	FILE_ELEMENT_MISSING		(7127, Level.ERROR, false, "CADU file entry {0} does not contain ''{1}'' element", ""),
	FILE_NOT_WRITABLE			(7128, Level.ERROR, false, "Cannot write CADU file {0} (cause: {1})", ""),
	FILE_DOWNLOAD_FAILED		(7129, Level.ERROR, false, "Download of CADU file {0} failed (cause: {1})", ""),
	FILE_SIZE_MISMATCH			(7130, Level.ERROR, false, "File size mismatch for CADU file {0} (expected: {1} Bytes, got {2} Bytes)", ""),
	SESSION_DOWNLOAD_TIMEOUT	(7131, Level.ERROR, false, "Timeout after {0} s during wait for completion of session {1}, download incomplete", ""),
	FILE_TRANSFER_COMPLETED		(7132, Level.INFO, true, "Session {0}: Transfer for CADU file {1} completed (size {2} bytes, published {3})", ""),
	
	CADIP_START_MESSAGE			(7198, Level.INFO, true, "------  Starting CADIP Monitor  ------\n"
															+ "CADIP base URI . . . . . . : {0}\n"
															+ "CADIP context. . . . . . . : {1}\n"
															+ "Use token-based auth . . . : {2}\n"
															+ "Satellite  . . . . . . . . : {3}\n"
															+ "Transfer history file  . . : {4}\n"
															+ "CADIP check interval . . . : {5}\n"
															+ "Session file check interval: {6}\n"
															+ "Session download timeout . : {7}\n"
															+ "History truncation interval: {8}\n"
															+ "History retention period . : {9}\n"
															+ "CADU target directory  . . : {10}\n"
															+ "L0 processor command . . . : {11}\n"
															+ "Max. transfer sessions . . : {12}\n"
															+ "Transfer session wait time : {13}\n"
															+ "Max. session wait cycles . : {14}\n"
															+ "Max. file download threads : {15}\n"
															+ "File download wait time  . : {16}\n"
															+ "Max. file wait cycles  . . : {17}"
															,""),
	
	XBIP_START_MESSAGE			(7199, Level.INFO, true, "------  Starting XBIP Monitor  ------\n"
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
