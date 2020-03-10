/**
 * GroupManager.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import de.dlr.proseo.usermgr.dao.GroupMemberRepository;
import de.dlr.proseo.usermgr.dao.GroupRepository;
import de.dlr.proseo.usermgr.dao.UserRepository;
import de.dlr.proseo.usermgr.model.Group;
import de.dlr.proseo.usermgr.model.GroupAuthority;
import de.dlr.proseo.usermgr.model.GroupMember;
import de.dlr.proseo.usermgr.model.User;
import de.dlr.proseo.usermgr.rest.model.RestGroup;
import de.dlr.proseo.usermgr.rest.model.RestUser;

/**
 * Service methods required to manage user group groups.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
@Transactional
public class GroupManager {

	/* Message ID constants */
	private static final int MSG_ID_GROUP_NOT_FOUND = 2770;
	private static final int MSG_ID_GROUP_LIST_RETRIEVED = 2771;
	private static final int MSG_ID_GROUP_RETRIEVED = 2772;
	private static final int MSG_ID_GROUP_MISSING = 2773;
	private static final int MSG_ID_GROUP_CREATED = 2775;
	private static final int MSG_ID_GROUPNAME_MISSING = 2776;
	private static final int MSG_ID_GROUPNAME_NOT_FOUND = 2777;
	private static final int MSG_ID_GROUP_DATA_MISSING = 2778;
	private static final int MSG_ID_GROUP_MODIFIED = 2779;
	private static final int MSG_ID_GROUP_NOT_MODIFIED = 2780;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 2781;
	private static final int MSG_ID_GROUP_DELETED = 2782;
	private static final int MSG_ID_DELETE_FAILURE = 2784;
	private static final int MSG_ID_MISSION_MISSING = 2785;
	private static final int MSG_ID_GROUP_ID_MISSING = 2786;
	private static final int MSG_ID_GROUP_ID_NOT_FOUND = 2787;
	private static final int MSG_ID_GROUP_EMPTY = 2788;
	private static final int MSG_ID_GROUP_MEMBERS_RETRIEVED = 2789;
	private static final int MSG_ID_GROUP_MEMBER_ADDED = 2790;
	private static final int MSG_ID_MEMBER_REMOVAL_UNSUCCESSFUL = 2791;
	private static final int MSG_ID_GROUP_MEMBER_REMOVED = 2792;
	private static final int MSG_ID_USERNAME_MISSING = 2756;
	private static final int MSG_ID_USERNAME_NOT_FOUND = 2757;
//	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_GROUP_NOT_FOUND = "(E%d) No user group found for mission %s";
	private static final String MSG_GROUP_MISSING = "(E%d) User group not set";
	private static final String MSG_GROUPNAME_MISSING = "(E%d) User group name not set";
	private static final String MSG_GROUP_ID_MISSING = "(E%d) User group ID not set";
	private static final String MSG_GROUPNAME_NOT_FOUND = "(E%d) User group %s not found";
	private static final String MSG_GROUP_ID_NOT_FOUND = "(E%d) User group with ID %d not found";
	private static final String MSG_GROUP_DATA_MISSING = "(E%d) User group data not set";
	private static final String MSG_DELETE_FAILURE = "(E%d) Deletion failed for user group %s (cause: %s)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Deletion unsuccessful for user group %s";
	private static final String MSG_MISSION_MISSING = "(E%d) Mission not set";
	private static final String MSG_GROUP_EMPTY = "(E%d) Group with ID %d has no members";
	private static final String MSG_USERNAME_MISSING = "(E%d) User name not set";
	private static final String MSG_USERNAME_NOT_FOUND = "(E%d) User %s not found";
	private static final String MSG_MEMBER_REMOVAL_UNSUCCESSFUL = "(E%d) Removal of member %s unsuccessful for user group with ID %d";
	
	private static final String MSG_GROUP_LIST_RETRIEVED = "(I%d) User(s) for mission %s retrieved";
	private static final String MSG_GROUP_RETRIEVED = "(I%d) User group %s retrieved";
	private static final String MSG_GROUP_CREATED = "(I%d) User group %s created";
	private static final String MSG_GROUP_MODIFIED = "(I%d) User group %s modified";
	private static final String MSG_GROUP_NOT_MODIFIED = "(I%d) User group %s not modified (no changes)";
	private static final String MSG_GROUP_DELETED = "(I%d) User group %s deleted";
	private static final String MSG_GROUP_MEMBERS_RETRIEVED = "(I%d) Members for user group with ID %d retrieved";
	private static final String MSG_GROUP_MEMBER_ADDED = "(I%d) Member %s added to user group with ID %d";
	private static final String MSG_GROUP_MEMBER_REMOVED = "(I%d) Member %s removed from user group with ID %d";

	/** Repository for User group objects */
	@Autowired
	GroupRepository groupRepository;
	
	/** Repository for User objects */
	@Autowired
	UserRepository userRepository;
	
	/** Repository for group members */
	@Autowired
	GroupMemberRepository groupMemberRepository;
	
	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GroupManager.class);

	/**
	 * Create and log a formatted informational message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private String logInfo(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.info(message);
		
		return message;
	}
	
	/**
	 * Create and log a formatted error message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted error message
	 */
	private String logError(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.error(message);
		
		return message;
	}
	
	/**
	 * Convert a user group from REST format to the prosEO data model format (including authorities)
	 * 
	 * @param restGroup the REST user group to convert
	 * @return the converted model user group
	 */
	/* package */ static Group toModelGroup(RestGroup restGroup) {
		if (logger.isTraceEnabled()) logger.trace(">>> toModelGroup({})", (null == restGroup ? "MISSING" : restGroup.getGroupname()));
		
		Group modelGroup = new Group();
		if (null != restGroup.getId()) {
			modelGroup.setId(restGroup.getId());
		}
		modelGroup.setGroupName(restGroup.getGroupname());
		for (String restAuthority: restGroup.getAuthorities()) {
			GroupAuthority modelAuthority = new GroupAuthority();
			modelAuthority.setAuthority(restAuthority);
			modelAuthority.setGroup(modelGroup);
			modelGroup.getGroupAuthorities().add(modelAuthority);
		}
		
		return modelGroup;
	}
	
	/**
	 * Convert a user group from prosEO data model format to REST format (including authorities)
	 * 
	 * @param modelGroup the model user group to convert
	 * @return the converted REST user
	 */
	/* package */ static RestGroup toRestGroup(Group modelGroup) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestGroup({})", (null == modelGroup ? "MISSING" : modelGroup.getGroupName()));
		
		RestGroup restGroup = new RestGroup();
		restGroup.setId(Long.valueOf(modelGroup.getId()));
		restGroup.setGroupname(modelGroup.getGroupName());
		for (GroupAuthority modelAuthority: modelGroup.getGroupAuthorities()) {
			restGroup.getAuthorities().add(modelAuthority.getAuthority());
		}
		
		return restGroup;
	}
	
	/**
	 * Create a user group (optionally with direct authorities)
	 * 
	 * @param restGroup a Json representation of the new user group
	 * @return a Json representation of the user group after creation
	 * @throws IllegalArgumentException if any of the input data was invalid
	 */
	public RestGroup createGroup(RestGroup restGroup) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> createGroup({})", (null == restGroup ? "MISSING" : restGroup.getGroupname()));
		
		// Check parameter
		if (null == restGroup) {
			throw new IllegalArgumentException(logError(MSG_GROUP_MISSING, MSG_ID_GROUP_MISSING));
		}
		if (null == restGroup.getGroupname() || "".equals(restGroup.getGroupname())) {
			throw new IllegalArgumentException(logError(MSG_GROUPNAME_MISSING, MSG_ID_GROUPNAME_MISSING));
		}
		
		// Create user group
		Group modelGroup = groupRepository.save(toModelGroup(restGroup));
		
		logInfo(MSG_GROUP_CREATED, MSG_ID_GROUP_CREATED, modelGroup.getGroupName());
		
		return toRestGroup(modelGroup);
	}

	/**
	 * Get user groups by mission and optionally by group name
	 * 
	 * @param mission the mission code
	 * @param groupName the group name (optional)
	 * @return a list of Json objects representing the user groups authorized for the given mission
	 * @throws NoResultException
	 */
	public List<RestGroup> getGroups(String mission, String groupName) throws NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getGroups({}, {})", mission, groupName);
		
		// Check parameter
		if (null == mission || "".equals(mission)) {
			throw new IllegalArgumentException(logError(MSG_MISSION_MISSING, MSG_ID_MISSION_MISSING));
		}
		
		// Collect all user groups for the mission
		List<RestGroup> result = new ArrayList<>();
		if (null == groupName) {
			for (Group modelGroup : groupRepository.findByMissionCode(mission)) {
				result.add(toRestGroup(modelGroup));
			} 
		} else {
			Group modelGroup = groupRepository.findByGroupName(groupName);
			if (null != modelGroup) {
				result.add(toRestGroup(modelGroup));
			}
		}
		if (result.isEmpty()) {
			throw new NoResultException(logError(MSG_GROUP_NOT_FOUND, MSG_ID_GROUP_NOT_FOUND, mission));
		}
		
		logInfo(MSG_GROUP_LIST_RETRIEVED, MSG_ID_GROUP_LIST_RETRIEVED, mission);
		
		return result;
	}

	/**
	 * Get a user group by ID
	 * 
	 * @param id the user group ID
	 * @return a Json object corresponding to the user group found
	 * @throws IllegalArgumentException if no user group ID was given
	 * @throws NoResultException if no user group with the given ID exists
	 */
	public RestGroup getGroupById(Long id) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getGroupById({})", id);
		
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_GROUP_ID_MISSING, MSG_ID_GROUP_ID_MISSING));
		}
		
		Optional<Group> modelGroup = groupRepository.findById(id);
		
		if (modelGroup.isEmpty()) {
			throw new NoResultException(logError(MSG_GROUPNAME_NOT_FOUND, MSG_ID_GROUPNAME_NOT_FOUND, id));
		}
				
		logInfo(MSG_GROUP_RETRIEVED, MSG_ID_GROUP_RETRIEVED, id);
		
		return toRestGroup(modelGroup.get());
	}

	/**
	 * Delete a user group by ID
	 * 
	 * @param id the ID of the user group to delete
	 * @throws EntityNotFoundException if the user group to delete does not exist in the database
	 * @throws RuntimeException if the deletion was not performed as expected
	 */
	public void deleteGroupById(Long id) throws EntityNotFoundException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteGroupById({})", id);
		
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_GROUP_ID_MISSING, MSG_ID_GROUP_ID_MISSING));
		}
		
		// Test whether the user group name is valid
		Optional<Group> modelGroup = groupRepository.findById(id);
		
		if (modelGroup.isEmpty()) {
			throw new NoResultException(logError(MSG_GROUP_ID_NOT_FOUND, MSG_ID_GROUP_ID_NOT_FOUND, id));
		}
		
		// Delete the user group
		try {
			modelGroup.get().getGroupAuthorities().clear();
			groupRepository.save(modelGroup.get());
			groupRepository.delete(modelGroup.get());
		} catch (Exception e) {
			throw new RuntimeException(logError(MSG_DELETE_FAILURE, MSG_ID_DELETE_FAILURE, id, e.getMessage()));
		}
		
		// Test whether the deletion was successful
		modelGroup = groupRepository.findById(id);
		
		if (null != modelGroup) {
			throw new RuntimeException(logError(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, id));
		}
		
		logInfo(MSG_GROUP_DELETED, MSG_ID_GROUP_DELETED, id);
	}

	/**
	 * Update a user group by user group ID
	 * 
	 * @param id the ID of the user group to update
	 * @param restGroup a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the user group after modification
	 * @throws EntityNotFoundException if no user group with the given user group ID exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 */
	public RestGroup modifyGroup(Long id, RestGroup restGroup) throws
			EntityNotFoundException, IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyUser({}, {})", id, (null == restGroup ? "MISSING" : restGroup.getGroupname()));
		
		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_GROUPNAME_MISSING, MSG_ID_GROUPNAME_MISSING));
		}
		if (null == restGroup) {
			throw new IllegalArgumentException(logError(MSG_GROUP_DATA_MISSING, MSG_ID_GROUP_DATA_MISSING));
		}
		
		// Get the user group to modify
		Optional<Group> optionalGroup = groupRepository.findById(id);
		
		if (optionalGroup.isEmpty()) {
			throw new NoResultException(logError(MSG_GROUPNAME_NOT_FOUND, MSG_ID_GROUPNAME_NOT_FOUND, id));
		}
		Group modelGroup = optionalGroup.get();
		
		// Apply changed attributes (no change of the group name allowed)
		// -- No modifiable attributes --
		//Group changedUser = toModelGroup(restGroup);
		
		boolean userChanged = false;
		
		// Apply changed authorities
		Set<GroupAuthority> newAuthorities = new HashSet<>();
		for (GroupAuthority modelAuthority: modelGroup.getGroupAuthorities()) {
			boolean authorityChanged = true;
			for (String restAuthority: restGroup.getAuthorities()) {
				if (modelAuthority.getAuthority().equals(restAuthority)) {
					// Unchanged authority
					newAuthorities.add(modelAuthority);
					authorityChanged = false;
					break;
				}
			}
			if (authorityChanged) {
				// This authority was revoked
				userChanged = true;
			}
		}
		for (String restAuthority: restGroup.getAuthorities()) {
			boolean authorityChanged = true;
			for (GroupAuthority modelAuthority: modelGroup.getGroupAuthorities()) {
				if (modelAuthority.getAuthority().equals(restAuthority)) {
					// Unchanged authority
					authorityChanged = false;
					break;
				}
			}
			if (authorityChanged) {
				// This authority was added
				userChanged = true;
				GroupAuthority newAuthority = new GroupAuthority();
				newAuthority.setAuthority(restAuthority);
				newAuthority.setGroup(modelGroup);
				newAuthorities.add(newAuthority);
			}
		}
		modelGroup.setGroupAuthorities(newAuthorities);
		
		// Save user group only if anything was actually changed
		if (userChanged) {
			modelGroup = groupRepository.save(modelGroup);
			logInfo(MSG_GROUP_MODIFIED, MSG_ID_GROUP_MODIFIED, id);
		} else {
			logInfo(MSG_GROUP_NOT_MODIFIED, MSG_ID_GROUP_NOT_MODIFIED, id);
		}
		
		// Return the changed user group
		return toRestGroup(modelGroup);
	}

	/**
	 * Get all members of the given user group
	 * 
	 * @param id the ID of the user group
	 * @return a list of Json objects representing the users, which are members of the given group
	 * @throws NoResultException if the group has no members
	 */
	public List<RestUser> getGroupMembers(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getGroups({})", id);
		
		// Check parameter
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_GROUP_ID_MISSING, MSG_ID_GROUP_ID_MISSING));
		}
		
		// Get the user group to modify
		Optional<Group> optionalGroup = groupRepository.findById(id);
		
		if (optionalGroup.isEmpty()) {
			throw new NoResultException(logError(MSG_GROUPNAME_NOT_FOUND, MSG_ID_GROUPNAME_NOT_FOUND, id));
		}
		Group modelGroup = optionalGroup.get();
		
		if (modelGroup.getGroupMembers().isEmpty()) {
			throw new NoResultException(logError(MSG_GROUP_EMPTY, MSG_ID_GROUP_EMPTY, id));
		}
		
		// Create a list of all users in the group
		List<RestUser> result = new ArrayList<>();
		for (GroupMember member: modelGroup.getGroupMembers()) {
			result.add(UserManager.toRestUser(member.getUser()));
		}
		
		logInfo(MSG_GROUP_MEMBERS_RETRIEVED, MSG_ID_GROUP_MEMBERS_RETRIEVED, id);
		
		return result;
	}

	/**
	 * Add a member to the given user group
	 * 
	 * @param id the ID of the group to update
	 * @param username the name of the user to add
	 * @return a Json object corresponding to the list of users after addition
	 * @throws EntityNotFoundException if no user group with the given ID or no user with the given name exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 */
	public List<RestUser> addGroupMember(Long id, String username) throws EntityNotFoundException, IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> addGroupMember({}, {})", id, username);
		
		// Check parameter
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_GROUP_ID_MISSING, MSG_ID_GROUP_ID_MISSING));
		}
		if (null == username || "".equals(username)) {
			throw new IllegalArgumentException(logError(MSG_USERNAME_MISSING, MSG_ID_USERNAME_MISSING));
		}
		
		// Get the user group to modify
		Optional<Group> optionalGroup = groupRepository.findById(id);
		
		if (optionalGroup.isEmpty()) {
			throw new NoResultException(logError(MSG_GROUPNAME_NOT_FOUND, MSG_ID_GROUPNAME_NOT_FOUND, id));
		}
		Group modelGroup = optionalGroup.get();
		
		// Get the user to add
		User modelUser = userRepository.findByUsername(username);
		
		if (null == modelUser) {
			throw new NoResultException(logError(MSG_USERNAME_NOT_FOUND, MSG_ID_USERNAME_NOT_FOUND, username));
		}
		
		// Add the user to the user group
		GroupMember newMember = new GroupMember();
		newMember.setGroup(modelGroup);
		newMember.setUser(modelUser);
		newMember = groupMemberRepository.save(newMember);
		
		modelGroup.getGroupMembers().add(newMember);
		modelUser.getGroupMemberships().add(newMember);
		
		// Create a list of all users in the group
		List<RestUser> result = new ArrayList<>();
		for (GroupMember member: modelGroup.getGroupMembers()) {
			result.add(UserManager.toRestUser(member.getUser()));
		}
		
		logInfo(MSG_GROUP_MEMBER_ADDED, MSG_ID_GROUP_MEMBER_ADDED, username, id);
		
		return result;
	}

	/**
	 * Delete a member from the given user group
	 * 
	 * @param id the group ID
	 * @param username the name of the user to remove
	 * @throws EntityNotFoundException if the group did not exist
	 * @throws RuntimeException if the deletion was unsuccessful
	 */
	public void removeGroupMember(Long id, String username) throws EntityNotFoundException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> removeGroupMember({}, {})", id, username);
		
		// Check parameter
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_GROUP_ID_MISSING, MSG_ID_GROUP_ID_MISSING));
		}
		if (null == username || "".equals(username)) {
			throw new IllegalArgumentException(logError(MSG_USERNAME_MISSING, MSG_ID_USERNAME_MISSING));
		}
		
		// Get the user group to modify
		Optional<Group> optionalGroup = groupRepository.findById(id);
		
		if (optionalGroup.isEmpty()) {
			throw new NoResultException(logError(MSG_GROUPNAME_NOT_FOUND, MSG_ID_GROUPNAME_NOT_FOUND, id));
		}
		Group modelGroup = optionalGroup.get();
		
		// Remove the user from the user group
		Iterator<GroupMember> memberIterator = modelGroup.getGroupMembers().iterator();
		while (memberIterator.hasNext()) {
			GroupMember member = memberIterator.next();
			if (username.equals(member.getUser().getUsername())) {
				User userToRemove = member.getUser();
				userToRemove.getGroupMemberships().remove(member);
				userRepository.save(userToRemove);
				memberIterator.remove();
				break;
			}
		}
		
		logInfo(MSG_GROUP_MEMBER_REMOVED, MSG_ID_GROUP_MEMBER_REMOVED, username, id);
	}

}
