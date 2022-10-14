/**
 * UserMgrMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the user manager.
 *
 * @author Katharina Bassler
 */
public enum UserMgrMessage implements ProseoMessage {
	
	USER_NOT_AUTHORIZED			(6501, Level.ERROR, false, "User {0} has no authorities for mission {1}", ""),
	MISSION_MISSING				(6502, Level.ERROR, false, "Mission not set", ""),
	ACCOUNT_EXPIRED				(6503, Level.ERROR, false, "Account expired for user {0}", ""),
	PASSWORD_EXPIRED			(6504, Level.ERROR, false, "Password expired for user {0}", ""),
	USER_NOT_FOUND				(6550, Level.ERROR, false, "No user found for mission {0}", ""),
	USER_LIST_RETRIEVED			(6551, Level.INFO, true, "User(s) for mission {0} retrieved", ""),
	USER_RETRIEVED				(6552, Level.INFO, true, "User {0} retrieved", ""),
	PASSWORD_MISSING			(6553, Level.ERROR, false, "Password not set", ""),
	USER_MISSING				(6554, Level.ERROR, false, "User not set", ""),
	USER_CREATED				(6555, Level.INFO, true, "User {0} created", ""),
	USERNAME_MISSING			(6556, Level.ERROR, false, "User name not set", ""),
	USERNAME_NOT_FOUND			(6557, Level.ERROR, false, "User {0} not found", ""),
	DUPLICATE_GROUP				(6558, Level.ERROR, false, "Duplicate user group {0}", ""),
	USER_MODIFIED				(6559, Level.INFO, true, "User {0} modified", ""),
	USER_NOT_MODIFIED			(6560, Level.INFO, true, "User {0} not modified (no changes)", ""),
	DELETION_UNSUCCESSFUL		(6561, Level.ERROR, false, "Deletion unsuccessful for user {0}", ""),
	USER_DELETED				(6562, Level.INFO, true, "User {0} deleted", ""),
	DELETE_FAILURE				(6564, Level.ERROR, false, "Deletion failed for user {0} (cause: {1})", ""),
	DUPLICATE_USER				(6566, Level.ERROR, false, "Duplicate user {0}", ""),
	ILLEGAL_DATA_ACCESS			(6567, Level.ERROR, false, "User {0} not authorized to access data for user {1}", ""),
	ILLEGAL_DATA_MODIFICATION	(6568, Level.ERROR, false, "Only change of password allowed for user {0}", ""),
	ILLEGAL_AUTHORITY			(6569, Level.ERROR, false, "Illegal authority value {0}", ""),
	GROUP_NOT_FOUND				(6570, Level.ERROR, false, "No user group found for mission {0}", ""),
	GROUP_LIST_RETRIEVED		(6571, Level.INFO, true, "User(s) for mission {0} retrieved", ""),
	GROUP_NOT_MODIFIED			(6572, Level.INFO, true, "User group {0} not modified (no changes)", ""),
	GROUP_MISSING				(6573, Level.ERROR, false, "User group not set", ""),
	GROUP_RETRIEVED				(6574, Level.INFO, true, "User group {0} retrieved", ""),
	GROUP_CREATED				(6575, Level.INFO, true, "User group {0} created", ""),
	GROUPNAME_MISSING			(6576, Level.ERROR, false, "User group name not set", ""),
	GROUPNAME_NOT_FOUND			(6577, Level.ERROR, false, "User group {0} not found", ""),
	GROUP_DATA_MISSING			(6578, Level.ERROR, false, "User group data not set", ""),
	GROUP_MODIFIED				(6579, Level.INFO, true, "User group {0} modified", ""),
	USER_DATA_MISSING			(6580, Level.ERROR, false, "User data not set", ""),
	DELETION_UNSUCCESSFUL_GROUP	(6581, Level.ERROR, false, "Deletion unsuccessful for user group {0}", ""),
	GROUP_DELETED				(6582, Level.INFO, true, "User group {0} deleted", ""),
	DELETE_FAILURE_GROUP		(6584, Level.ERROR, false, "Deletion failed for user group {0} (cause: {1})", ""),
	GROUP_ID_MISSING			(6586, Level.ERROR, false, "User group ID not set", ""),
	GROUP_ID_NOT_FOUND			(6587, Level.ERROR, false, "User group with ID {0} not found", ""),
	GROUP_EMPTY					(6588, Level.ERROR, false, "Group with ID {0} has no members", ""),
	GROUP_MEMBERS_RETRIEVED		(6589, Level.INFO, true, "Members for user group with ID {0} retrieved", ""),
	GROUP_MEMBER_ADDED			(6590, Level.INFO, true, "Member {0} added to user group with ID {1}", ""),
	GROUP_MEMBER_REMOVED		(6592, Level.INFO, true, "Member {0} removed from user group with ID {1}", ""),
	CREATE_BOOTSTRAP_USER		(6593, Level.INFO, true, "Creating bootstrap user", ""),
	CREATE_BOOTSTRAP_FAILED		(6594, Level.INFO, true, "Creation of bootstrap user failed, cause: {0}", ""),

	;
	
	private final int code;
	private final String description;
	private final boolean success;
	private final Level level;
	private final String message;

	private UserMgrMessage(int code, Level level, boolean success, String message, String description) {
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
