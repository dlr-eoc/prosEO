/**
 * SampleMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the ingestor.
 *
 * @author Katharina Bassler
 */
public enum SamplesMessage implements ProseoMessage {

	LEAVING_SAMPLE_PROCESSOR						(0000, Level.ERROR, "Leaving sample-processor with exit code {0} ({1})", ""),
	STARTING_SAMPLE_PROCESSOR						(0000, Level.ERROR, "Starting sample-processor V00.00.02 with JobOrder file {0}", ""),
	INVALID_NUMBER_OF_ARGUMENTS						(0000, Level.ERROR, "Invalid number of invocation arguments: {0} (only 1 allowed)", ""),
	INVALID_NUMBER_OF_OUTPUT_FILES					(0000, Level.ERROR, "Invalid number of output files: {0} (1 - 10 expected)", ""),
	INVALID_NUMBER_OF_OUTPUT_FILE_TYPES				(0000, Level.ERROR, "Invalid number of output file types: {0} (exactly 1 expected)", ""),
	INVALID_OUTPUT_FILE_TYPE						(0000, Level.ERROR, "Invalid output file type: {0}", ""),
	INVALID_NUMBER_OF_INPUT_FILE_TYPES				(0000, Level.ERROR, "Invalid number of input file types: {0} (exactly 1 expected)", ""),
	INVALID_NUMBER_OF_INPUT_FILE_NAMES				(0000, Level.ERROR, "Invalid number of input file names: {0} (exactly 1 expected)", ""),
	INVALID_NUMBER_OF_TASK_NAMES					(0000, Level.ERROR, "Invalid number of task names: {0} (exactly 1 expected)", ""),
	INVALID_TASK_NAME_IN_JOF						(0000, Level.ERROR, "Invalid task name {0} in JobOrder file (expected {1})", ""),
	INVALID_NUMBER_OF_CONFIGURATION_FILES			(0000, Level.ERROR, "Invalid number of configuration files for output file type {0}: {1} (exactly 1 expected)", ""),
	INVALID_NUMBER_OF_CONFIGURATION_FILE_NAMES		(0000, Level.ERROR, "Invalid number of configuration file names: {0} (exactly 1 expected)", ""),
	NO_VALID_INPUT_FILE_IN_JOF						(0000, Level.ERROR, "No valid input file in Job Order file for output file type {0}", ""),
	INVALID_FILE_CONTENT							(0000, Level.ERROR, "Invalid content in file {0} (expecting 'id|type|startTime|stopTime|revision')", ""),
	FILE_NOT_READABLE								(0000, Level.ERROR, "File {0} is not readable", ""),
	FILE_NOT_CLOSABLE								(0000, Level.ERROR, "Cannot close file {0}", ""),
	FILE_NOT_WRITABLE								(0000, Level.ERROR, "File {0} is not writable", ""),
	ERROR_INSTANTIATING_DOCUMENT_BUILDER			(0000, Level.ERROR, "Error instantiating DocumentBuilder: {0}", ""),
	JOF_NOT_PARSEABLE								(0000, Level.ERROR, "JobOrder file {0} not parseable ({1})", ""),
	JOF_TAG_MISSING									(0000, Level.ERROR, "JobOrder file does not contain element with tag {0}", ""),
	CONFIGURATION_DOC_NOT_PARSEABLE					(0000, Level.ERROR, "Configuration file {0} not parseable ({1})", ""),
	PARAMETER_NAME_MISSING							(0000, Level.ERROR, "Configuration file contains PARAM element without name attribute", ""),
	TASK_NAME_MISSING_IN_CONF_FILE					(0000, Level.ERROR, "PARAM element with name 'task_name' missing in configuration file {0}", ""),

	;

	private final int code;
	private final Level level;
	private final String message;
	private final String description;

	private SamplesMessage(int code, Level level, String message, String description) {
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
