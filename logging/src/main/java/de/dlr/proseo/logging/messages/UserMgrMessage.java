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
	
	USER_NOT_AUTHORIZED			(2701, Level.ERROR, false, "User {0} has no authorities for mission {1}", ""),
	MISSION_MISSING				(2702, Level.ERROR, false, "Mission not set", ""),
	ACCOUNT_EXPIRED				(2703, Level.ERROR, false, "Account expired for user {0}", ""),
	PASSWORD_EXPIRED			(2704, Level.ERROR, false, "Password expired for user {0}", ""),
	USER_NOT_FOUND				(2750, Level.ERROR, false, "No user found for mission {0}", ""),
	USER_LIST_RETRIEVED			(2751, Level.INFO, true, "User(s) for mission {0} retrieved", ""),
	USER_RETRIEVED				(2752, Level.INFO, true, "User {0} retrieved", ""),
	PASSWORD_MISSING			(2753, Level.ERROR, false, "Password not set", ""),
	USER_MISSING				(2754, Level.ERROR, false, "User not set", ""),
	USER_CREATED				(2755, Level.INFO, true, "User {0} created", ""),
	USERNAME_MISSING			(2756, Level.ERROR, false, "User name not set", ""),
	USERNAME_NOT_FOUND			(2757, Level.ERROR, false, "User {0} not found", ""),
	DUPLICATE_GROUP				(2758, Level.ERROR, false, "Duplicate user group {0}", ""),
	USER_MODIFIED				(2759, Level.INFO, true, "User {0} modified", ""),
	USER_NOT_MODIFIED			(2760, Level.INFO, true, "User {0} not modified (no changes)", ""),
	DELETION_UNSUCCESSFUL		(2761, Level.ERROR, false, "Deletion unsuccessful for user {0}", ""),
	USER_DELETED				(2762, Level.INFO, true, "User {0} deleted", ""),
	DELETE_FAILURE				(2764, Level.ERROR, false, "Deletion failed for user {0} (cause: {1})", ""),
	DUPLICATE_USER				(2766, Level.ERROR, false, "Duplicate user {0}", ""),
	ILLEGAL_DATA_ACCESS			(2767, Level.ERROR, false, "User {0} not authorized to access data for user {1}", ""),
	ILLEGAL_DATA_MODIFICATION	(2768, Level.ERROR, false, "Only change of password allowed for user {0}", ""),
	ILLEGAL_AUTHORITY			(2769, Level.ERROR, false, "Illegal authority value {0}", ""),
	GROUP_NOT_FOUND				(2770, Level.ERROR, false, "No user group found for mission {0}", ""),
	GROUP_LIST_RETRIEVED		(2771, Level.INFO, true, "User(s) for mission {0} retrieved", ""),
	GROUP_NOT_MODIFIED			(2772, Level.INFO, true, "User group {0} not modified (no changes)", ""),
	GROUP_MISSING				(2773, Level.ERROR, false, "User group not set", ""),
	GROUP_RETRIEVED				(2774, Level.INFO, true, "User group {0} retrieved", ""),
	GROUP_CREATED				(2775, Level.INFO, true, "User group {0} created", ""),
	GROUPNAME_MISSING			(2776, Level.ERROR, false, "User group name not set", ""),
	GROUPNAME_NOT_FOUND			(2777, Level.ERROR, false, "User group {0} not found", ""),
	GROUP_DATA_MISSING			(2778, Level.ERROR, false, "User group data not set", ""),
	GROUP_MODIFIED				(2779, Level.INFO, true, "User group {0} modified", ""),
	USER_DATA_MISSING			(2780, Level.ERROR, false, "User data not set", ""),
	DELETION_UNSUCCESSFUL_GROUP	(2781, Level.ERROR, false, "Deletion unsuccessful for user group {0}", ""),
	GROUP_DELETED				(2782, Level.INFO, true, "User group {0} deleted", ""),
	DELETE_FAILURE_GROUP		(2784, Level.ERROR, false, "Deletion failed for user group {0} (cause: {1})", ""),
	GROUP_ID_MISSING			(2786, Level.ERROR, false, "User group ID not set", ""),
	GROUP_ID_NOT_FOUND			(2787, Level.ERROR, false, "User group with ID {0} not found", ""),
	GROUP_EMPTY					(2788, Level.ERROR, false, "Group with ID {0} has no members", ""),
	GROUP_MEMBERS_RETRIEVED		(2789, Level.INFO, true, "Members for user group with ID {0} retrieved", ""),
	GROUP_MEMBER_ADDED			(2790, Level.INFO, true, "Member {0} added to user group with ID {1}", ""),
	GROUP_MEMBER_REMOVED		(2792, Level.INFO, true, "Member {0} removed from user group with ID {1}", ""),
	CREATE_BOOTSTRAP_USER		(2793, Level.INFO, true, "Creating bootstrap user", ""),
	CREATE_BOOTSTRAP_FAILED		(2794, Level.INFO, true, "Creation of bootstrap user failed, cause: {0}", ""),

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
