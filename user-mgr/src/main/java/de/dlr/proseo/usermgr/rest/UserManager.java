/**
 * UserManager.java
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
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.usermgr.dao.UserRepository;
import de.dlr.proseo.usermgr.model.Authority;
import de.dlr.proseo.usermgr.model.GroupAuthority;
import de.dlr.proseo.usermgr.model.GroupMember;
import de.dlr.proseo.usermgr.model.User;
import de.dlr.proseo.usermgr.rest.model.RestUser;

/**
 * Service methods required to manage user accounts.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
@Transactional
public class UserManager {

	/* Message ID constants */
	private static final int MSG_ID_USER_NOT_FOUND = 2750;
	private static final int MSG_ID_USER_LIST_RETRIEVED = 2751;
	private static final int MSG_ID_USER_RETRIEVED = 2752;
	private static final int MSG_ID_USER_MISSING = 2753;
	private static final int MSG_ID_USER_CREATED = 2755;
	private static final int MSG_ID_USERNAME_MISSING = 2756;
	private static final int MSG_ID_USERNAME_NOT_FOUND = 2757;
	private static final int MSG_ID_USER_DATA_MISSING = 2758;
	private static final int MSG_ID_USER_MODIFIED = 2759;
	private static final int MSG_ID_USER_NOT_MODIFIED = 2760;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 2761;
	private static final int MSG_ID_USER_DELETED = 2762;
	private static final int MSG_ID_DELETE_FAILURE = 2764;
	private static final int MSG_ID_MISSION_MISSING = 2765;
	
	/* Message string constants */
	private static final String MSG_USER_NOT_FOUND = "(E%d) No user found for mission %s";
	private static final String MSG_USER_MISSING = "(E%d) User not set";
	private static final String MSG_USERNAME_MISSING = "(E%d) User name not set";
	private static final String MSG_USERNAME_NOT_FOUND = "(E%d) User %s not found";
	private static final String MSG_USER_DATA_MISSING = "(E%d) User data not set";
	private static final String MSG_DELETE_FAILURE = "(E%d) Deletion failed for user %s (cause: %s)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Deletion unsuccessful for user %s";
	private static final String MSG_MISSION_MISSING = "(E%d) Mission not set";
	private static final String MSG_USER_LIST_RETRIEVED = "(I%d) User(s) for mission %s retrieved";
	private static final String MSG_USER_RETRIEVED = "(I%d) User %s retrieved";
	private static final String MSG_USER_CREATED = "(I%d) User %s created";
	private static final String MSG_USER_MODIFIED = "(I%d) User %s modified";
	private static final String MSG_USER_NOT_MODIFIED = "(I%d) User %s not modified (no changes)";
	private static final String MSG_USER_DELETED = "(I%d) User %s deleted";

	/** Repository for User objects */
	@Autowired
	UserRepository userRepository;
	
	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(UserManager.class);

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
	 * Convert a user from REST format to the prosEO data model format (including directly assigned authorities)
	 * 
	 * @param restUser the REST user to convert
	 * @return the converted model user
	 */
	/* package */ static User toModelUser(RestUser restUser) {
		if (logger.isTraceEnabled()) logger.trace(">>> toModelUser({})", (null == restUser ? "MISSING" : restUser.getUsername()));
		
		User modelUser = new User();
		modelUser.setUsername(restUser.getUsername());
		modelUser.setPassword(restUser.getPassword());
		modelUser.setEnabled(restUser.getEnabled());
		if (null == restUser.getExpirationDate()) {
			modelUser.setExpirationDate(Date.from(Instant.MAX));
		} else {
			modelUser.setExpirationDate(restUser.getExpirationDate());
		}
		if (null == restUser.getPasswordExpirationDate()) {
			modelUser.setPasswordExpirationDate(Date.from(Instant.MAX));
		} else {
			modelUser.setPasswordExpirationDate(restUser.getPasswordExpirationDate());
		}
		for (String restAuthority: restUser.getAuthorities()) {
			Authority modelAuthority = new Authority();
			modelAuthority.setAuthority(restAuthority);
			modelAuthority.setUser(modelUser);
			modelUser.getAuthorities().add(modelAuthority);
		}
		
		return modelUser;
	}
	
	/**
	 * Convert a user from prosEO data model format to REST format (including directly assigned authorities)
	 * 
	 * @param modelUser the model user to convert
	 * @return the converted REST user
	 */
	/* package */ static RestUser toRestUser(User modelUser) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestUser({})", (null == modelUser ? "MISSING" : modelUser.getUsername()));
		
		RestUser restUser = new RestUser();
		restUser.setUsername(modelUser.getUsername());
		restUser.setPassword(modelUser.getPassword());
		restUser.setEnabled(modelUser.getEnabled());
		if (null != modelUser.getExpirationDate()) {
			restUser.setExpirationDate(modelUser.getExpirationDate());
		}
		if (null != modelUser.getPasswordExpirationDate()) {
			restUser.setPasswordExpirationDate(modelUser.getPasswordExpirationDate());
		}
		for (Authority modelAuthority: modelUser.getAuthorities()) {
			restUser.getAuthorities().add(modelAuthority.getAuthority());
		}
		
		return restUser;
	}
	
	/**
	 * Create a user (optionally with direct authorities)
	 * 
	 * @param restUser a Json representation of the new user
	 * @return a Json representation of the user after creation (with ACL security identity ID)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 */
	public RestUser createUser(RestUser restUser) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> createUser({})", (null == restUser ? "MISSING" : restUser.getUsername()));
		
		// Check parameter
		if (null == restUser) {
			throw new IllegalArgumentException(logError(MSG_USER_MISSING, MSG_ID_USER_MISSING));
		}
		if (null == restUser.getUsername() || "".equals(restUser.getUsername())) {
			throw new IllegalArgumentException(logError(MSG_USERNAME_MISSING, MSG_ID_USERNAME_MISSING));
		}
		
		// Create user
		User modelUser = userRepository.save(toModelUser(restUser));
		
		logInfo(MSG_USER_CREATED, MSG_ID_USER_CREATED, modelUser.getUsername());
		
		return toRestUser(modelUser);
	}

	/**
	 * Get users by mission
	 * 
	 * @param mission the mission code
	 * @return a list of Json objects representing the users authorized for the given mission
	 * @throws NoResultException
	 */
	public List<RestUser> getUsers(String mission) throws NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getUsers({})", mission);
		
		// Check parameter
		if (null == mission || "".equals(mission)) {
			throw new IllegalArgumentException(logError(MSG_MISSION_MISSING, MSG_ID_MISSION_MISSING));
		}
		
		// Collect all users connected to the ACL entries of the mission (either directly or indirectly)
		List<RestUser> result = new ArrayList<>();
		for (User modelUser: userRepository.findByMissionCode(mission)) {
			result.add(toRestUser(modelUser));
		}

		if (result.isEmpty()) {
			throw new NoResultException(logError(MSG_USER_NOT_FOUND, MSG_ID_USER_NOT_FOUND, mission));
		}
		
		logInfo(MSG_USER_LIST_RETRIEVED, MSG_ID_USER_LIST_RETRIEVED, mission);
		
		return result;
	}

	/**
	 * Get a user by name
	 * 
	 * @param username the user name
	 * @return a Json object corresponding to the user found
	 * @throws IllegalArgumentException if no user name was given
	 * @throws NoResultException if no user with the given name exists
	 */
	public RestUser getUserByName(String username) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getUserByName({})", username);
		
		if (null == username || "".equals(username)) {
			throw new IllegalArgumentException(logError(MSG_USERNAME_MISSING, MSG_ID_USERNAME_MISSING));
		}
		
		User modelUser = userRepository.findByUsername(username);
		
		if (null == modelUser) {
			throw new NoResultException(logError(MSG_USERNAME_NOT_FOUND, MSG_ID_USERNAME_NOT_FOUND, username));
		}
		
		logInfo(MSG_USER_RETRIEVED, MSG_ID_USER_RETRIEVED, username);
		
		return toRestUser(modelUser);
	}

	/**
	 * Delete a user by user name
	 * 
	 * @param username the name of the user to delete
	 * @throws EntityNotFoundException if the user to delete does not exist in the database
	 * @throws RuntimeException if the deletion was not performed as expected
	 */
	public void deleteUserByName(String username) throws EntityNotFoundException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteUserByName({})", username);
		
		if (null == username || "".equals(username)) {
			throw new IllegalArgumentException(logError(MSG_USERNAME_MISSING, MSG_ID_USERNAME_MISSING));
		}
		
		// Test whether the user name is valid
		User modelUser = userRepository.findByUsername(username);
		
		if (null == modelUser) {
			throw new NoResultException(logError(MSG_USERNAME_NOT_FOUND, MSG_ID_USERNAME_NOT_FOUND, username));
		}
		
		// Delete the user
		try {
			userRepository.delete(modelUser);
		} catch (Exception e) {
			throw new RuntimeException(logError(MSG_DELETE_FAILURE, MSG_ID_DELETE_FAILURE, username, e.getMessage()));
		}
		
		// Test whether the deletion was successful
		modelUser = userRepository.findByUsername(username);
		
		if (null != modelUser) {
			throw new RuntimeException(logError(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, username));
		}
		
		logInfo(MSG_USER_DELETED, MSG_ID_USER_DELETED, username);
	}

	/**
	 * Update a user by user name
	 * 
	 * @param username the name of the user to update
	 * @param restUser a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the user after modification
	 * @throws EntityNotFoundException if no user with the given user name exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 */
	public RestUser modifyUser(String username, RestUser restUser) throws
			EntityNotFoundException, IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyUser({}, {})", username, (null == restUser ? "MISSING" : restUser.getUsername()));
		
		// Check arguments
		if (null == username || "".equals(username)) {
			throw new IllegalArgumentException(logError(MSG_USERNAME_MISSING, MSG_ID_USERNAME_MISSING));
		}
		if (null == restUser) {
			throw new IllegalArgumentException(logError(MSG_USER_DATA_MISSING, MSG_ID_USER_DATA_MISSING));
		}
		
		// Get the user to modify
		User modelUser = userRepository.findByUsername(username);
		
		if (null == modelUser) {
			throw new NoResultException(logError(MSG_USERNAME_NOT_FOUND, MSG_ID_USERNAME_NOT_FOUND, username));
		}
		
		// Apply changed attributes (no change of the username allowed)
		User changedUser = toModelUser(restUser);
		
		boolean userChanged = false;
		
		if (!modelUser.getPassword().equals(changedUser.getPassword())) {
			userChanged = true;
			modelUser.setPassword(changedUser.getPassword());
		}
		if (!modelUser.getEnabled().equals(changedUser.getEnabled())) {
			userChanged = true;
			modelUser.setPassword(changedUser.getPassword());
		}
		if (!modelUser.getExpirationDate().equals(changedUser.getExpirationDate()))	{
			userChanged = true;
			modelUser.setExpirationDate(changedUser.getExpirationDate());
		}
		if (!modelUser.getPasswordExpirationDate().equals(changedUser.getPasswordExpirationDate())) {
			userChanged = true;
			modelUser.setPasswordExpirationDate(changedUser.getPasswordExpirationDate());
		}
		
		// Apply changed authorities
		Set<Authority> newAuthorities = new HashSet<>();
		for (Authority modelAuthority: modelUser.getAuthorities()) {
			boolean authorityChanged = true;
			for (String restAuthority: restUser.getAuthorities()) {
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
		for (String restAuthority: restUser.getAuthorities()) {
			boolean authorityChanged = true;
			for (Authority modelAuthority: modelUser.getAuthorities()) {
				if (modelAuthority.getAuthority().equals(restAuthority)) {
					// Unchanged authority
					authorityChanged = false;
					break;
				}
			}
			if (authorityChanged) {
				// This authority was added
				userChanged = true;
				Authority newAuthority = new Authority();
				newAuthority.setAuthority(restAuthority);
				newAuthority.setUser(modelUser);
				newAuthorities.add(newAuthority);
			}
		}
		modelUser.setAuthorities(newAuthorities);
		
		// Save user only if anything was actually changed
		if (userChanged) {
			modelUser = userRepository.save(modelUser);
			logInfo(MSG_USER_MODIFIED, MSG_ID_USER_MODIFIED, username);
		} else {
			logInfo(MSG_USER_NOT_MODIFIED, MSG_ID_USER_NOT_MODIFIED, username);
		}
		
		// Return the changed user
		return toRestUser(modelUser);
	}

}
