/**
 * GroupManager.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
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
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.usermgr.dao.AclSidRepository;
import de.dlr.proseo.usermgr.dao.GroupRepository;
import de.dlr.proseo.usermgr.model.AclSid;
import de.dlr.proseo.usermgr.model.Authority;
import de.dlr.proseo.usermgr.model.Group;
import de.dlr.proseo.usermgr.model.GroupAuthority;
import de.dlr.proseo.usermgr.model.GroupMember;
import de.dlr.proseo.usermgr.model.User;
import de.dlr.proseo.usermgr.rest.model.RestAuthority;
import de.dlr.proseo.usermgr.rest.model.RestGroup;

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
	private static final int MSG_ID_MISSION_NOT_FOUND = 2786;
	private static final int MSG_ID_GROUP_ID_MISSING = 2787;
	private static final int MSG_ID_GROUP_ID_NOT_FOUND = 2788;
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
	private static final String MSG_MISSION_NOT_FOUND = "(E%d) Mission %s not found";

	private static final String MSG_GROUP_LIST_RETRIEVED = "(I%d) User(s) for mission %s retrieved";
	private static final String MSG_GROUP_RETRIEVED = "(I%d) User group %s retrieved";
	private static final String MSG_GROUP_CREATED = "(I%d) User group %s created with security identity ID %d";
	private static final String MSG_GROUP_MODIFIED = "(I%d) User group %s modified";
	private static final String MSG_GROUP_NOT_MODIFIED = "(I%d) User group %s not modified (no changes)";
	private static final String MSG_GROUP_DELETED = "(I%d) User group %s deleted";

	/** Repository for User group objects */
	@Autowired
	GroupRepository groupRepository;
	/** Repository for ACL security identity objects */
	@Autowired
	AclSidRepository aclSidRepository;
	/** The ACL service */
	@Autowired
	MutableAclService aclService;
	
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
	 * Convert a user group from REST format to the prosEO data model format
	 * 
	 * @param restGroup the REST user group to convert
	 * @return the converted model user group
	 */
	private Group toModelGroup(RestGroup restGroup) {
		if (logger.isTraceEnabled()) logger.trace(">>> toModelGroup({})", (null == restGroup ? "MISSING" : restGroup.getGroupname()));
		
		Group modelGroup = new Group();
		if (null != restGroup.getId()) {
			modelGroup.setId(restGroup.getId());
		}
		modelGroup.setGroupName(restGroup.getGroupname());
		
		return modelGroup;
	}
	
	/**
	 * Convert a user group from prosEO data model format to REST format (without authorities)
	 * 
	 * @param modelGroup the model user group to convert
	 * @param sid the ACL security identity representing this user group (optional)
	 * @return the converted REST user
	 */
	private RestGroup toRestGroup(Group modelGroup, AclSid sid) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestGroup({})", (null == modelGroup ? "MISSING" : modelGroup.getGroupName()));
		
		RestGroup restGroup = new RestGroup();
		restGroup.setId(Long.valueOf(modelGroup.getId()));
		restGroup.setGroupname(modelGroup.getGroupName());
		if (null != sid) {
			restGroup.setSidId(sid.getId());
		}
		
		return restGroup;
	}
	
	/**
	 * Create an ACL security identity (SID), if none exists yet for the given authority,
	 * and authorize it for the object identity given in the authority
	 * 
	 * @param restAuthority the authority, for which a SID shall be generated
	 */
	private void conditionallyCreateSid(RestAuthority restAuthority) {
		if (logger.isTraceEnabled()) logger.trace(">>> conditionallyCreateSid({})", restAuthority.getAuthority());

		// Create ACL security identity, if it does not exist
		AclSid sid = aclSidRepository.findBySid(restAuthority.getAuthority());
		if (null == sid) {
			sid = new AclSid();
			sid.setPrincipal(false);
			sid.setSid(restAuthority.getAuthority());
			aclSidRepository.save(sid);
		}
		
		// Authorize for all operations on the given object identity (if any)
		if (Mission.class.getCanonicalName().equals(restAuthority.getObjectClass())) {
			Mission modelMission = RepositoryService.getMissionRepository().findByCode(restAuthority.getObjectIdentifier());
			if (null == modelMission) {
				throw new IllegalArgumentException(logError(MSG_MISSION_NOT_FOUND, MSG_ID_MISSION_NOT_FOUND, restAuthority.getObjectIdentifier()));
			}
			ObjectIdentity objectIdentity = new ObjectIdentityImpl(Mission.class, modelMission.getId());

			MutableAcl acl = (MutableAcl) aclService.readAclById(objectIdentity);
			Sid authoritySid = new GrantedAuthoritySid(sid.getSid());
			acl.insertAce(acl.getEntries().size(), BasePermission.ADMINISTRATION, authoritySid, true);
			aclService.updateAcl(acl);
		}
	}

	/**
	 * Delete the ACL security identity (SID) for the given authority, if no other user group exists with this authority
	 * 
	 * @param groupAuthority the authority to (conditionally) delete
	 */
	private void deleteOrphanedSid(GroupAuthority groupAuthority) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteOrphanedSid({})", groupAuthority.getAuthority());

		List<Group> groupList = groupRepository.findByAuthority(groupAuthority.getAuthority());
		if (1 < groupList.size() || 1 == groupList.size() && !groupAuthority.getGroup().equals(groupList.get(0))) {
			// Other users left with this authority
			return;
		}
		
		AclSid sid = aclSidRepository.findBySid(groupAuthority.getAuthority());
		if (null != sid) {
			aclSidRepository.delete(sid);
		}
	}
	/**
	 * Create a user group (optionally with direct authorities)
	 * 
	 * @param restGroup a Json representation of the new user group
	 * @return a Json representation of the user group after creation (with ACL security identity ID)
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
		
		// Create user
		Group modelGroup = toModelGroup(restGroup);
		
		modelGroup = groupRepository.save(modelGroup);
		
		for (RestAuthority restAuthority: restGroup.getAuthorities()) {
			// Create new authority
			GroupAuthority authority = new GroupAuthority();
			authority.setAuthority(restAuthority.getAuthority());
			authority.setGroup(modelGroup);
			modelGroup.getGroupAuthorities().add(authority);
			
			// Create ACL security identity for new authority
			conditionallyCreateSid(restAuthority);
		}
		
		// Create ACL security identity for user
		AclSid sid = new AclSid();
		sid.setPrincipal(true);
		sid.setSid(modelGroup.getGroupName());
		
		sid = aclSidRepository.save(sid);
		
		logInfo(MSG_GROUP_CREATED, MSG_ID_GROUP_CREATED, modelGroup.getGroupName(), sid.getId());
		
		return toRestGroup(modelGroup, sid);
	}

	/**
	 * Get user groups by mission
	 * 
	 * @param mission the mission code
	 * @return a list of Json objects representing the user groups authorized for the given mission
	 * @throws NoResultException
	 */
	public List<RestGroup> getGroups(String mission) throws NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getGroups({})", mission);
		
		// Check parameter
		if (null == mission || "".equals(mission)) {
			throw new IllegalArgumentException(logError(MSG_MISSION_MISSING, MSG_ID_MISSION_MISSING));
		}
		
		// Get the ACL object identity for the mission
		Mission modelMission = RepositoryService.getMissionRepository().findByCode(mission);
		if (null == modelMission) {
			throw new IllegalArgumentException(logError(MSG_MISSION_NOT_FOUND, MSG_ID_MISSION_NOT_FOUND, mission));
		}
		ObjectIdentity missionIdentity = new ObjectIdentityImpl(Mission.class, modelMission.getId());
		
		// Collect all user groups connected to the ACL entries of the mission
		List<RestGroup> result = new ArrayList<>();
		for (Group modelGroup: groupRepository.findAll()) {
			List<Sid> sids = new ArrayList<>();
			sids.add(new PrincipalSid(modelGroup.getGroupName()));
			
			// Create sids for all directly granted authorities
			for (GroupAuthority authority: modelGroup.getGroupAuthorities()) {
				sids.add(new GrantedAuthoritySid(authority.getAuthority()));
			}
			
			// Find the ACL entries for this mission
			Acl acl = null;
			try {
				acl = aclService.readAclById(missionIdentity, sids);
			} catch (NotFoundException e) {
				// No authorities for this user
				continue;
			}
			if (acl.getEntries().isEmpty()) {
				// ACL loaded, but no ACL entries for the given sids
				continue;
			}
			
			// OK, this user group has some authorization(s) for the given mission
			RestGroup restUser = toRestGroup(modelGroup, null);
			
			for (AccessControlEntry entry: acl.getEntries()) {
				Sid entrySid = entry.getSid();
				if (entrySid instanceof GrantedAuthoritySid) {
					RestAuthority restAuthority = new RestAuthority();
					restAuthority.setAuthority(((GrantedAuthoritySid) entrySid).getGrantedAuthority());
					restAuthority.setObjectClass(Mission.class.getCanonicalName());
					restAuthority.setObjectIdentifier(missionIdentity.getIdentifier().toString());
					restUser.getAuthorities().add(restAuthority);
				}
			}

			result.add(restUser);
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
		
		if (null == id || "".equals(id)) {
			throw new IllegalArgumentException(logError(MSG_GROUPNAME_MISSING, MSG_ID_GROUPNAME_MISSING));
		}
		
		Optional<Group> modelGroup = groupRepository.findById(id);
		
		if (modelGroup.isEmpty()) {
			throw new NoResultException(logError(MSG_GROUPNAME_NOT_FOUND, MSG_ID_GROUPNAME_NOT_FOUND, id));
		}
		
		RestGroup restUser = toRestGroup(modelGroup.get(), null);
		
		// Add authorities granted directly
		for (GroupAuthority modelAuthority: modelGroup.get().getGroupAuthorities()) {
			RestAuthority restAuthority = new RestAuthority();
			restAuthority.setAuthority(modelAuthority.getAuthority());
			restUser.getAuthorities().add(restAuthority);
		}
		
		logInfo(MSG_GROUP_RETRIEVED, MSG_ID_GROUP_RETRIEVED, id);
		
		return restUser;
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
			throw new IllegalArgumentException(logError(MSG_GROUP_ID_MISSING, MSG_ID_GROUPNAME_MISSING));
		}
		
		// Test whether the user group name is valid
		Optional<Group> modelGroup = groupRepository.findById(id);
		
		if (modelGroup.isEmpty()) {
			throw new NoResultException(logError(MSG_GROUP_ID_NOT_FOUND, MSG_ID_GROUP_ID_NOT_FOUND, id));
		}
		
		// Delete the ACL security identities for the user group and their authorities
		AclSid sid = aclSidRepository.findBySid(modelGroup.get().getGroupName());
		if (null != sid) {
			aclSidRepository.delete(sid);
		}
		for (GroupAuthority authority: modelGroup.get().getGroupAuthorities()) {
			deleteOrphanedSid(authority);
		}
		
		// Delete the user group
		try {
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
		if (null == id || "".equals(id)) {
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
			for (RestAuthority restAuthority: restGroup.getAuthorities()) {
				if (modelAuthority.getAuthority().equals(restAuthority.getAuthority())) {
					// Unchanged authority
					newAuthorities.add(modelAuthority);
					authorityChanged = false;
					break;
				}
			}
			if (authorityChanged) {
				// This authority was revoked
				deleteOrphanedSid(modelAuthority);
				userChanged = true;
			}
		}
		for (RestAuthority restAuthority: restGroup.getAuthorities()) {
			boolean authorityChanged = true;
			for (GroupAuthority modelAuthority: modelGroup.getGroupAuthorities()) {
				if (modelAuthority.getAuthority().equals(restAuthority.getAuthority())) {
					// Unchanged authority
					authorityChanged = false;
					break;
				}
			}
			if (authorityChanged) {
				// This authority was added
				userChanged = true;
				GroupAuthority newAuthority = new GroupAuthority();
				newAuthority.setAuthority(restAuthority.getAuthority());
				newAuthority.setGroup(modelGroup);
				newAuthorities.add(newAuthority);
				
				// Create ACL security identity for new authority
				conditionallyCreateSid(restAuthority);
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
		
		// Return the changed user
		restGroup = toRestGroup(modelGroup, null);
		
		// Add authorities granted directly
		for (GroupAuthority modelAuthority: modelGroup.getGroupAuthorities()) {
			RestAuthority restAuthority = new RestAuthority();
			restAuthority.setAuthority(modelAuthority.getAuthority());
			restGroup.getAuthorities().add(restAuthority);
		}
		
		return restGroup;
	}

}
