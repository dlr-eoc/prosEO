/**
 * GroupManager.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.UserMgrMessage;
import de.dlr.proseo.model.enums.UserRole;
import de.dlr.proseo.usermgr.UsermgrConfiguration;
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

	/** Repository for User group objects */
	@Autowired
	GroupRepository groupRepository;

	/** Repository for User objects */
	@Autowired
	UserRepository userRepository;

	/** Repository for group members */
	@Autowired
	GroupMemberRepository groupMemberRepository;

	/** The User Manager configuration */
	@Autowired
	private UsermgrConfiguration config;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GroupManager.class);

	/**
	 * Exception to indicate unmodified data to caller
	 */
	public static class NotModifiedException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public NotModifiedException(String message) {
			super(message);
		}
	}

	/**
	 * Convert a user group from REST format to the prosEO data model format
	 * (including authorities)
	 *
	 * @param restGroup the REST user group to convert
	 * @return the converted model user group
	 * @throws IllegalArgumentException if an invalid authority value was given
	 */
	/* package */ static Group toModelGroup(RestGroup restGroup) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> toModelGroup({})", (null == restGroup ? "MISSING" : restGroup.getGroupname()));

		Group modelGroup = new Group();
		if (null != restGroup.getId()) {
			modelGroup.setId(restGroup.getId());
		}
		modelGroup.setGroupName(restGroup.getGroupname());
		for (String restAuthority : restGroup.getAuthorities()) {
			// Test whether authority is legal
			try {
				UserRole.asRole(restAuthority);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(logger.log(UserMgrMessage.ILLEGAL_AUTHORITY, restAuthority));
			}

			GroupAuthority modelAuthority = new GroupAuthority();
			modelAuthority.setAuthority(restAuthority);
			modelAuthority.setGroup(modelGroup);
			modelGroup.getGroupAuthorities().add(modelAuthority);
		}

		return modelGroup;
	}

	/**
	 * Convert a user group from prosEO data model format to REST format (including
	 * authorities)
	 *
	 * @param modelGroup the model user group to convert
	 * @return the converted REST user
	 */
	/* package */ static RestGroup toRestGroup(Group modelGroup) {
		if (logger.isTraceEnabled())
			logger.trace(">>> toRestGroup({})", (null == modelGroup ? "MISSING" : modelGroup.getGroupName()));

		RestGroup restGroup = new RestGroup();
		restGroup.setId(Long.valueOf(modelGroup.getId()));
		restGroup.setGroupname(modelGroup.getGroupName());
		for (GroupAuthority modelAuthority : modelGroup.getGroupAuthorities()) {
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
		if (logger.isTraceEnabled())
			logger.trace(">>> createGroup({})", (null == restGroup ? "MISSING" : restGroup.getGroupname()));

		// Check parameter
		if (null == restGroup) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.GROUP_MISSING));
		}
		if (null == restGroup.getGroupname() || restGroup.getGroupname().isBlank()) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.GROUPNAME_MISSING));
		}
		if (null == restGroup.getAuthorities()) {
			restGroup.setAuthorities(new ArrayList<String>());
		}

		// Make sure the group does not exist already
		if (null != groupRepository.findByGroupName(restGroup.getGroupname())) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.DUPLICATE_GROUP, restGroup.getGroupname()));
		}

		// Create user group
		Group modelGroup = groupRepository.save(toModelGroup(restGroup));

		logger.log(UserMgrMessage.GROUP_CREATED, modelGroup.getGroupName());

		return toRestGroup(modelGroup);
	}

	/**
	 * Get user groups by mission and optionally by group name
	 *
	 * @param mission    the mission code
	 * @param groupName  the group name (optional)
	 * @param recordFrom first record of filtered and ordered result to return
	 * @param recordTo   last record of filtered and ordered result to return
	 * @return a list of Json objects representing the user groups authorized for
	 *         the given mission
	 * @throws HttpClientErrorException if the result list exceeds a configured
	 *                                  maximum
	 * @throws NoResultException        if no groups matching the search criteria
	 *                                  can be found
	 */
	public List<RestGroup> getGroups(String mission, String groupName, Integer recordFrom, Integer recordTo)
			throws NoResultException, HttpClientErrorException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getGroups({}, {})", mission, groupName);

		// Check parameter
		if (null == mission || mission.isBlank()) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.MISSION_MISSING));
		}

		List<RestGroup> result = new ArrayList<>();

		// If specified, find group by name
		if (null != groupName) {
			Group group = groupRepository.findByGroupName(groupName);

			if (null == group) {
				throw new NoResultException(logger.log(UserMgrMessage.GROUPNAME_NOT_FOUND, groupName));
			} else {
				logger.log(UserMgrMessage.GROUP_RETRIEVED, groupName);

				result.add(toRestGroup(group));
				return result;
			}
		}

		// Collect all user groups for the mission within the configured range
		if (recordFrom == null) {
			recordFrom = 0;
		}
		if (recordTo == null) {
			recordTo = Integer.MAX_VALUE;
		}

		Long numberOfResults = Long.parseLong(this.countGroups(mission));
		Integer maxResults = config.getMaxResults();
		if (numberOfResults > maxResults && (recordTo - recordFrom) > maxResults && (numberOfResults - recordFrom) > maxResults) {
			throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS,
					logger.log(GeneralMessage.TOO_MANY_RESULTS, "groups", numberOfResults, config.getMaxResults()));
		}

		Query query = em.createQuery("select g from groups g where g.groupName like concat(:missionCode, '-%')");
		query.setParameter("missionCode",  mission);
		query.setFirstResult(recordFrom);
		query.setMaxResults(recordTo - recordFrom);

		for (Object resultObject : query.getResultList()) {
			if (resultObject instanceof Group) {
				result.add(toRestGroup((Group) resultObject));
			}
		}

		if (result.isEmpty()) {
			throw new NoResultException(logger.log(UserMgrMessage.GROUP_NOT_FOUND, mission));
		}

		logger.log(UserMgrMessage.GROUP_LIST_RETRIEVED, mission);

		return result;
	}

	/**
	 * Get a user group by ID
	 *
	 * @param id the user group ID
	 * @return a Json object corresponding to the user group found
	 * @throws IllegalArgumentException if no user group ID was given
	 * @throws NoResultException        if no user group with the given ID exists
	 */
	public RestGroup getGroupById(Long id) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getGroupById({})", id);

		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.GROUP_ID_MISSING));
		}

		Optional<Group> modelGroup = groupRepository.findById(id);

		if (modelGroup.isEmpty()) {
			throw new NoResultException(logger.log(UserMgrMessage.GROUPNAME_NOT_FOUND, id));
		}

		logger.log(UserMgrMessage.GROUP_RETRIEVED, id);

		return toRestGroup(modelGroup.get());
	}

	/**
	 * Delete a user group by ID
	 *
	 * @param id the ID of the user group to delete
	 * @throws EntityNotFoundException if the user group to delete does not exist in
	 *                                 the database
	 * @throws RuntimeException        if the deletion was not performed as expected
	 */
	public void deleteGroupById(Long id) throws EntityNotFoundException, RuntimeException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteGroupById({})", id);

		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.GROUP_ID_MISSING));
		}

		// Test whether the user group name is valid
		Optional<Group> modelGroup = groupRepository.findById(id);

		if (modelGroup.isEmpty()) {
			throw new NoResultException(logger.log(UserMgrMessage.GROUP_ID_NOT_FOUND, id));
		}

		// Delete the user group
		try {
			modelGroup.get().getGroupAuthorities().clear();
			groupRepository.save(modelGroup.get());
			groupRepository.delete(modelGroup.get());
		} catch (Exception e) {
			throw new RuntimeException(logger.log(UserMgrMessage.DELETE_FAILURE, id, e.getMessage()));
		}

		// Test whether the deletion was successful
		modelGroup = groupRepository.findById(id);

		if (!modelGroup.isEmpty()) {
			throw new RuntimeException(logger.log(UserMgrMessage.DELETION_UNSUCCESSFUL, id));
		}

		logger.log(UserMgrMessage.GROUP_DELETED, id);
	}

	/**
	 * Update a user group by user group ID
	 *
	 * @param id        the ID of the user group to update
	 * @param restGroup a Json object containing the modified (and unmodified)
	 *                  attributes
	 * @return a response containing a Json object corresponding to the user group
	 *         after modification
	 * @throws EntityNotFoundException  if no user group with the given user group
	 *                                  ID exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws NotModifiedException     if the group data was not modified (input
	 *                                  data same as database data)
	 */
	public RestGroup modifyGroup(Long id, RestGroup restGroup)
			throws EntityNotFoundException, IllegalArgumentException, de.dlr.proseo.usermgr.rest.GroupManager.NotModifiedException {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyUser({}, {})", id, (null == restGroup ? "MISSING" : restGroup.getGroupname()));

		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.GROUPNAME_MISSING));
		}
		if (null == restGroup) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.GROUP_DATA_MISSING));
		}
		if (null == restGroup.getGroupname() || restGroup.getGroupname().isBlank()) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.GROUPNAME_MISSING));
		}
		if (null == restGroup.getAuthorities()) {
			restGroup.setAuthorities(new ArrayList<String>());
		}

		// Get the user group to modify
		Optional<Group> optionalGroup = groupRepository.findById(id);

		if (optionalGroup.isEmpty()) {
			throw new NoResultException(logger.log(UserMgrMessage.GROUPNAME_NOT_FOUND, id));
		}
		Group modelGroup = optionalGroup.get();

		// Apply changed attributes (no change of the group name allowed)
		// -- No modifiable attributes --
		toModelGroup(restGroup); // Just check for valid input data

		boolean groupChanged = false;

		// Apply changed authorities
		Set<GroupAuthority> newAuthorities = new HashSet<>();
		for (GroupAuthority modelAuthority : modelGroup.getGroupAuthorities()) {
			boolean authorityChanged = true;
			for (String restAuthority : restGroup.getAuthorities()) {
				if (modelAuthority.getAuthority().equals(restAuthority)) {
					// Unchanged authority
					newAuthorities.add(modelAuthority);
					authorityChanged = false;
					break;
				}
			}
			if (authorityChanged) {
				// This authority was revoked
				groupChanged = true;
			}
		}
		for (String restAuthority : restGroup.getAuthorities()) {
			boolean authorityChanged = true;
			for (GroupAuthority modelAuthority : modelGroup.getGroupAuthorities()) {
				if (modelAuthority.getAuthority().equals(restAuthority)) {
					// Unchanged authority
					authorityChanged = false;
					break;
				}
			}
			if (authorityChanged) {
				// This authority was added
				groupChanged = true;
				GroupAuthority newAuthority = new GroupAuthority();
				newAuthority.setAuthority(restAuthority);
				newAuthority.setGroup(modelGroup);
				newAuthorities.add(newAuthority);
			}
		}
		modelGroup.setGroupAuthorities(newAuthorities);

		// Save user group only if anything was actually changed
		if (groupChanged) {
			modelGroup = groupRepository.save(modelGroup);
			logger.log(UserMgrMessage.GROUP_MODIFIED, id);
		} else {
			throw new NotModifiedException(logger.log(UserMgrMessage.GROUP_NOT_MODIFIED, id));
		}

		// Return the changed user group
		return toRestGroup(modelGroup);
	}

	/**
	 * Get all members of the given user group
	 *
	 * @param id the ID of the user group
	 * @return a list of Json objects representing the users, which are members of
	 *         the given group
	 * @throws NoResultException if the group has no members
	 */
	public List<RestUser> getGroupMembers(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getGroups({})", id);

		// Check parameter
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.GROUP_ID_MISSING));
		}

		// Get the user group to modify
		Optional<Group> optionalGroup = groupRepository.findById(id);

		if (optionalGroup.isEmpty()) {
			throw new NoResultException(logger.log(UserMgrMessage.GROUPNAME_NOT_FOUND, id));
		}
		Group modelGroup = optionalGroup.get();

		if (modelGroup.getGroupMembers().isEmpty()) {
			throw new NoResultException(logger.log(UserMgrMessage.GROUP_EMPTY, id));
		}

		// Create a list of all users in the group
		List<RestUser> result = new ArrayList<>();
		for (GroupMember member : modelGroup.getGroupMembers()) {
			result.add(UserManager.toRestUser(member.getUser()));
		}

		logger.log(UserMgrMessage.GROUP_MEMBERS_RETRIEVED, id);

		return result;
	}

	/**
	 * Add a member to the given user group
	 *
	 * @param id       the ID of the group to update
	 * @param username the name of the user to add
	 * @return a Json object corresponding to the list of users after addition
	 * @throws EntityNotFoundException  if no user group with the given ID or no
	 *                                  user with the given name exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws NotModifiedException     if the user is already a member of the group
	 */
	public List<RestUser> addGroupMember(Long id, String username)
			throws EntityNotFoundException, IllegalArgumentException, de.dlr.proseo.usermgr.rest.GroupManager.NotModifiedException {
		if (logger.isTraceEnabled())
			logger.trace(">>> addGroupMember({}, {})", id, username);

		// Check parameter
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.GROUP_ID_MISSING));
		}
		if (null == username || username.isBlank()) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.USERNAME_MISSING));
		}

		// Get the user group to modify
		Optional<Group> optionalGroup = groupRepository.findById(id);

		if (optionalGroup.isEmpty()) {
			throw new NoResultException(logger.log(UserMgrMessage.GROUPNAME_NOT_FOUND, id));
		}
		Group modelGroup = optionalGroup.get();

		// Get the user to add
		User modelUser = userRepository.findByUsername(username);

		if (null == modelUser) {
			throw new NoResultException(logger.log(UserMgrMessage.USERNAME_NOT_FOUND, username));
		}

		// Add the user to the user group
		GroupMember newMember = new GroupMember();
		newMember.setGroup(modelGroup);
		newMember.setUser(modelUser);

		if (modelGroup.getGroupMembers().contains(newMember)) {
			throw new NotModifiedException(logger.log(UserMgrMessage.GROUP_NOT_MODIFIED, id));
		}

		newMember = groupMemberRepository.save(newMember);

		modelGroup.getGroupMembers().add(newMember);
		modelUser.getGroupMemberships().add(newMember);

		// Create a list of all users in the group
		List<RestUser> result = new ArrayList<>();
		for (GroupMember member : modelGroup.getGroupMembers()) {
			result.add(UserManager.toRestUser(member.getUser()));
		}

		logger.log(UserMgrMessage.GROUP_MEMBER_ADDED, username, id);

		return result;
	}

	/**
	 * Delete a member from the given user group
	 *
	 * @param id       the group ID
	 * @param username the name of the user to remove
	 * @throws EntityNotFoundException if the group did not exist
	 * @throws RuntimeException        if the deletion was unsuccessful
	 * @throws NotModifiedException    if the user is not a member of the group
	 */
	public void removeGroupMember(Long id, String username)
			throws EntityNotFoundException, RuntimeException, de.dlr.proseo.usermgr.rest.GroupManager.NotModifiedException {
		if (logger.isTraceEnabled())
			logger.trace(">>> removeGroupMember({}, {})", id, username);

		// Check parameter
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.GROUP_ID_MISSING));
		}
		if (null == username || username.isBlank()) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.USERNAME_MISSING));
		}

		// Get the user group to modify
		Optional<Group> optionalGroup = groupRepository.findById(id);

		if (optionalGroup.isEmpty()) {
			throw new NoResultException(logger.log(UserMgrMessage.GROUPNAME_NOT_FOUND, id));
		}
		Group modelGroup = optionalGroup.get();

		// Remove the user from the user group
		Iterator<GroupMember> memberIterator = modelGroup.getGroupMembers().iterator();
		boolean memberFound = false;
		while (memberIterator.hasNext()) {
			GroupMember member = memberIterator.next();
			if (username.equals(member.getUser().getUsername())) {
				User userToRemove = member.getUser();
				userToRemove.getGroupMemberships().remove(member);
				userRepository.save(userToRemove);
				memberIterator.remove();
				memberFound = true;
				break;
			}
		}

		if (!memberFound) {
			throw new NotModifiedException(logger.log(UserMgrMessage.GROUP_NOT_MODIFIED, id));
		}

		logger.log(UserMgrMessage.GROUP_MEMBER_REMOVED, username, id);
	}

	/**
	 * Count the groups matching the specified mission
	 *
	 * @param mission the mission code
	 * @return the number of groups found as string
	 */
	public String countGroups(String mission) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countGroups({})", mission);

		// Check mission
		if (null == mission || mission.isBlank()) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.MISSION_MISSING));
		}

		// Build query with long results and parameters as in the group class
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = cb.createQuery(Long.class);
		Root<Group> groupType = query.from(Group.class);

		// Only count groups of the given mission
		List<Predicate> predicates = new ArrayList<>();
		// Compare the group's mission prefix with the provided mission string
		predicates.add(cb.equal(cb.substring(groupType.get("groupName"), 0, mission.length()), mission));
		query.select(cb.count(groupType)).where(predicates.toArray(new Predicate[predicates.size()]));

		Long result = em.createQuery(query).getSingleResult();

		logger.log(UserMgrMessage.GROUPS_COUNTED, result, mission);

		return result.toString();
	}

}