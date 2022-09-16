/**
 * OrderMgrMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the order manager.
 *
 * @author Katharina Bassler
 */
public enum OrderMgrMessage implements ProseoMessage {

	JOBCOUNT_RETRIEVED			(1065, Level.INFO, true, "Job count for processing order '{0}' retrieved", ""),
	JOBINDEX_RETRIEVED			(1066, Level.INFO, true, "Job index for processing order '{0}' retrieved", ""),
	JOB_RETRIED					(1067, Level.INFO, true, "Job '{0}' set to inital", ""),
	JOB_NOT_EXIST				(1068, Level.ERROR, false, "Job '{0}' does not exist", ""),
	JOBSTEP_NOT_EXIST			(1069, Level.ERROR, false, "Job step '{0}' does not exist", ""),
	JOBSTEP_RETRIEVED			(1070, Level.INFO, true, "Job step '{0}' retrieved", ""),
	JOBSTEPS_RETRIEVED			(1071, Level.INFO, true, "Job steps of status {0} retrieved for mission {0}", ""),
	JOBS_FOR_ORDER_NOT_EXIST	(1072, Level.ERROR, false, "Job(s) for processing order '{0}' do not exist", ""),
	JOBS_RETRIEVED				(1073, Level.INFO, true, "Jobs for processing order '{0}' retrieved", ""),

	;

	private final int code;
	private final Level level;
	private final boolean success;
	private final String message;
	private final String description;

	private OrderMgrMessage(int code, Level level, boolean success, String message, String description) {
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
