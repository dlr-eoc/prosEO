/**
 * UserManager.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.enums.UserRole;
import de.dlr.proseo.usermgr.UsermgrConfiguration;
import de.dlr.proseo.usermgr.dao.UserRepository;
import de.dlr.proseo.usermgr.model.Authority;
import de.dlr.proseo.usermgr.model.Quota;
import de.dlr.proseo.usermgr.model.User;
import de.dlr.proseo.usermgr.rest.model.RestQuota;
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
	private static final int MSG_ID_PASSWORD_MISSING = 2753;
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
	private static final int MSG_ID_DUPLICATE_USER = 2766;
	private static final int MSG_ID_ILLEGAL_DATA_ACCESS = 2767;
	private static final int MSG_ID_ILLEGAL_DATA_MODIFICATION = 2768;
	private static final int MSG_ID_ILLEGAL_AUTHORITY = 2769;
	
	/* Message string constants */
	private static final String MSG_USER_NOT_FOUND = "(E%d) No user found for mission %s";
	private static final String MSG_USER_MISSING = "(E%d) User not set";
	private static final String MSG_PASSWORD_MISSING = "(E%d) Password not set";
	private static final String MSG_USERNAME_MISSING = "(E%d) User name not set";
	private static final String MSG_USERNAME_NOT_FOUND = "(E%d) User %s not found";
	private static final String MSG_USER_DATA_MISSING = "(E%d) User data not set";
	private static final String MSG_DELETE_FAILURE = "(E%d) Deletion failed for user %s (cause: %s)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Deletion unsuccessful for user %s";
	private static final String MSG_MISSION_MISSING = "(E%d) Mission not set";
	private static final String MSG_DUPLICATE_USER = "(E%d) Duplicate user %s";
	private static final String MSG_ILLEGAL_DATA_ACCESS = "(E%d) User %s not authorized to access data for user %s";
	private static final String MSG_ILLEGAL_DATA_MODIFICATION = "(E%d) Only change of password allowed for user %s";
	private static final String MSG_ILLEGAL_AUTHORITY = "(E%d) Illegal authority value %s";
	
	private static final String MSG_USER_LIST_RETRIEVED = "(I%d) User(s) for mission %s retrieved";
	private static final String MSG_USER_RETRIEVED = "(I%d) User %s retrieved";
	private static final String MSG_USER_CREATED = "(I%d) User %s created";
	private static final String MSG_USER_MODIFIED = "(I%d) User %s modified";
	private static final String MSG_USER_NOT_MODIFIED = "(I%d) User %s not modified (no changes)";
	private static final String MSG_USER_DELETED = "(I%d) User %s deleted";

	/* Other string constants */
	private static final String ROLE_ROOT = UserRole.ROOT.asRoleString();
	private static final String ROLE_USERMGR = UserRole.USERMGR.asRoleString();

	/** Repository for User objects */
	@Autowired
	UserRepository userRepository;
	
	/** The User Manager configuration */
	@Autowired
	private UsermgrConfiguration config;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(UserManager.class);
	
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
	 * Create and log a formatted informational message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private static String logInfo(String messageFormat, int messageId, Object... messageParameters) {
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
	private static String logError(String messageFormat, int messageId, Object... messageParameters) {
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
	 * @throws IllegalArgumentException if an invalid authority value was given
	 */
	/* package */ static User toModelUser(RestUser restUser) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> toModelUser({})", (null == restUser ? "MISSING" : restUser.getUsername()));
		
		User modelUser = new User();
		modelUser.setUsername(restUser.getUsername());
		if (null != restUser.getPassword()) {
			modelUser.setPassword(restUser.getPassword());
		}
		modelUser.setEnabled(restUser.getEnabled());
		if (null != restUser.getExpirationDate()) {
			modelUser.setExpirationDate(restUser.getExpirationDate());
		}
		if (null != restUser.getPasswordExpirationDate()) {
			modelUser.setPasswordExpirationDate(restUser.getPasswordExpirationDate());
		}
		if (null != restUser.getQuota()) {
			RestQuota restQuota = restUser.getQuota();
			Quota modelQuota = new Quota();
			modelQuota.setAssigned(restQuota.getAssigned().intValue());
			modelQuota.setUsed(restQuota.getUsed().intValue());
			modelQuota.setLastAccessDate(restQuota.getLastAccessDate());
			modelUser.setQuota(modelQuota);
		}
		for (String restAuthority: restUser.getAuthorities()) {
			// Test whether authority is legal
			try {
				UserRole.asRole(restAuthority);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(logError(MSG_ILLEGAL_AUTHORITY, MSG_ID_ILLEGAL_AUTHORITY, restAuthority));
			}
			
			Authority modelAuthority = new Authority();
			modelAuthority.setAuthority(restAuthority);
			modelAuthority.setUser(modelUser);
			modelUser.getAuthorities().add(modelAuthority);
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< toModelUser()");
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
		// Never return the password hash
		restUser.setEnabled(modelUser.getEnabled());
		if (null != modelUser.getExpirationDate()) {
			restUser.setExpirationDate(modelUser.getExpirationDate());
		}
		if (null != modelUser.getPasswordExpirationDate()) {
			restUser.setPasswordExpirationDate(modelUser.getPasswordExpirationDate());
		}
		if (null != modelUser.getQuota()) {
			Quota modelQuota = modelUser.getQuota();
			restUser.setQuota(new RestQuota(modelQuota.getAssigned().longValue(), modelQuota.getUsed().longValue(), modelQuota.getLastAccessDate()));
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
		if (null == restUser.getUsername() || restUser.getUsername().isBlank()) {
			throw new IllegalArgumentException(logError(MSG_USERNAME_MISSING, MSG_ID_USERNAME_MISSING));
		}
		if (null == restUser.getPassword() || restUser.getPassword().isBlank()) {
			throw new IllegalArgumentException(logError(MSG_PASSWORD_MISSING, MSG_ID_PASSWORD_MISSING));
		}
		
		// Make sure user does not exist already
		if (null != userRepository.findByUsername(restUser.getUsername())) {
			throw new IllegalArgumentException(logError(MSG_DUPLICATE_USER, MSG_ID_DUPLICATE_USER, restUser.getUsername()));
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
	 * @throws NoResultException if no user is found for the given mission
	 */
	public List<RestUser> getUsers(String mission) throws NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getUsers({})", mission);
		
		// Check whether principal has ROOT role
		boolean isRootUser = false;
		Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
		for (GrantedAuthority authority: authorities) {
			if (logger.isTraceEnabled()) logger.trace("... checking granted authority " + authority.getAuthority());
			if (ROLE_ROOT.equals(authority.getAuthority())) {
				isRootUser = true;
			}
		}
		
		// Check parameter
		if (!isRootUser && (null == mission || mission.isBlank())) {
			throw new IllegalArgumentException(logError(MSG_MISSION_MISSING, MSG_ID_MISSION_MISSING));
		}
		
		// Collect all users whose user name starts with the mission code (or all users, if a root user starts the request without a mission)
		List<User> userList = null;
		if (isRootUser && (null == mission || mission.isBlank())) {
			userList = userRepository.findAll();
		} else {
			userList = userRepository.findByMissionCode(mission);
		}

		if (userList.isEmpty()) {
			throw new NoResultException(logError(MSG_USER_NOT_FOUND, MSG_ID_USER_NOT_FOUND, mission));
		}
		
		List<RestUser> result = new ArrayList<>();
		for (User modelUser: userList) {
			result.add(toRestUser(modelUser));
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
		
		if (null == username || username.isBlank()) {
			throw new IllegalArgumentException(logError(MSG_USERNAME_MISSING, MSG_ID_USERNAME_MISSING));
		}
		
		// Check permission to read the user data (only ROOT and USERMGR may read all user data, regular users may only read their own data)
		
		// Since successful authentication is required for accessing "login", we trust that the authentication object is filled
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String loginUsername = auth.getName();
		Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();  // Includes group authorities
		
		// Collect authorities and handle root user: No check against missions required
		boolean isUserManager = false;
		for (GrantedAuthority authority: authorities) {
			if (ROLE_ROOT.equals(authority.getAuthority()) || ROLE_USERMGR.equals(authority.getAuthority())) {
				isUserManager = true;
			}
		}
		
		if (!isUserManager && !username.equals(loginUsername)) {
			throw new SecurityException(logError(MSG_ILLEGAL_DATA_ACCESS, MSG_ID_ILLEGAL_DATA_ACCESS, loginUsername, username));
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
		
		if (null == username || username.isBlank()) {
			throw new IllegalArgumentException(logError(MSG_USERNAME_MISSING, MSG_ID_USERNAME_MISSING));
		}
		
		// Test whether the user name is valid
		User modelUser = userRepository.findByUsername(username);
		
		if (null == modelUser) {
			throw new NoResultException(logError(MSG_USERNAME_NOT_FOUND, MSG_ID_USERNAME_NOT_FOUND, username));
		}
		
		// Delete the user
		try {
			modelUser.getAuthorities().clear();
			userRepository.save(modelUser);
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
	 * If the password is changed, the password expiration date will be updated according to the password expiration period
	 * configured.
	 * 
	 * Note: This method cannot detect, whether a password was actually changed, because due to the BCrypt algorithm used
	 * the same password may yield different salted hashes with each encryption run. It is in the responsibility of the
	 * calling component to make sure that the password was indeed altered (and that it conforms to any applicable password
	 * policy).
	 * 
	 * @param username the name of the user to update
	 * @param restUser a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the user after modification
	 * @throws EntityNotFoundException if no user with the given user name exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws NotModifiedException if the user data was not modified (input data same as database data)
	 */
	public RestUser modifyUser(String username, RestUser restUser) throws
			EntityNotFoundException, IllegalArgumentException, NotModifiedException {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyUser({}, {})", username, (null == restUser ? "MISSING" : restUser.getUsername()));
		
		// Check arguments
		if (null == username || username.isBlank()) {
			throw new IllegalArgumentException(logError(MSG_USERNAME_MISSING, MSG_ID_USERNAME_MISSING));
		}
		if (null == restUser) {
			throw new IllegalArgumentException(logError(MSG_USER_DATA_MISSING, MSG_ID_USER_DATA_MISSING));
		}
		
		// Check permission to change the user (only ROOT and USERMGR may change all users, regular users may only change
		// their password (which entails an update of the password expiration date)
		
		// Since successful authentication is required for accessing "login", we trust that the authentication object is filled
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String loginUsername = auth.getName();
		Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();  // Includes group authorities
		
		// Collect authorities and handle root user: No check against missions required
		boolean isUserManager = false;
		for (GrantedAuthority authority: authorities) {
			if (ROLE_ROOT.equals(authority.getAuthority()) || ROLE_USERMGR.equals(authority.getAuthority())) {
				isUserManager = true;
			}
		}
		
		if (!isUserManager && !username.equals(loginUsername)) {
			throw new SecurityException(logError(MSG_ILLEGAL_DATA_ACCESS, MSG_ID_ILLEGAL_DATA_ACCESS, loginUsername, username));
		}
		
		// Get the user to modify
		User modelUser = userRepository.findByUsername(username);
		
		if (null == modelUser) {
			throw new NoResultException(logError(MSG_USERNAME_NOT_FOUND, MSG_ID_USERNAME_NOT_FOUND, username));
		}
		
		// Apply changed attributes (no change of the username allowed)
		User changedUser = toModelUser(restUser);
		
		boolean userChanged = false;
		
		if (!modelUser.getEnabled().equals(changedUser.getEnabled())) {
			if (logger.isTraceEnabled()) logger.trace("Enabled changed from {} to {}", modelUser.getEnabled(), changedUser.getEnabled());
			if (!isUserManager) {
				throw new SecurityException(logError(MSG_ILLEGAL_DATA_MODIFICATION, MSG_ID_ILLEGAL_DATA_MODIFICATION, loginUsername));
			}
			userChanged = true;
			modelUser.setEnabled(changedUser.getEnabled());
		}
		// Change expiration dates only if modified by at least one day
		if (24*60*60*1000 <= Math.abs(modelUser.getExpirationDate().getTime() - changedUser.getExpirationDate().getTime()))	{
			if (logger.isTraceEnabled()) logger.trace("Expiration changed from {} to {}", modelUser.getExpirationDate(), changedUser.getExpirationDate());
			if (!isUserManager) {
				throw new SecurityException(logError(MSG_ILLEGAL_DATA_MODIFICATION, MSG_ID_ILLEGAL_DATA_MODIFICATION, loginUsername));
			}
			userChanged = true;
			modelUser.setExpirationDate(changedUser.getExpirationDate());
		}
		if (24*60*60*1000 <= Math.abs(modelUser.getPasswordExpirationDate().getTime() - changedUser.getPasswordExpirationDate().getTime())) {
			if (logger.isTraceEnabled()) logger.trace("Passwd expiration changed from {} to {}", modelUser.getPasswordExpirationDate(), changedUser.getPasswordExpirationDate());
			if (!isUserManager) {
				throw new SecurityException(logError(MSG_ILLEGAL_DATA_MODIFICATION, MSG_ID_ILLEGAL_DATA_MODIFICATION, loginUsername));
			}
			userChanged = true;
			modelUser.setPasswordExpirationDate(changedUser.getPasswordExpirationDate());
		}
		if (null != changedUser.getPassword() && !modelUser.getPassword().equals(changedUser.getPassword())) {
			userChanged = true;
			modelUser.setPassword(changedUser.getPassword());
			
			// Update password expiration date, if needed
			Date newPasswordExpirationDate = 
					Date.from(Instant.now().plus(Long.parseLong(config.getPasswordExpirationTime()), ChronoUnit.DAYS));
			if (modelUser.getPasswordExpirationDate().before(newPasswordExpirationDate)) {
				// Do not reduce longer expiration dates, if they have been granted (esp. those amounting to "never expires")
				modelUser.setPasswordExpirationDate(newPasswordExpirationDate);
			}
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
				if (!isUserManager) {
					throw new SecurityException(logError(MSG_ILLEGAL_DATA_MODIFICATION, MSG_ID_ILLEGAL_DATA_MODIFICATION, loginUsername));
				}
				userChanged = true;
				if (logger.isTraceEnabled()) logger.trace("Authority revoked: {}", modelAuthority.getAuthority());
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
				if (!isUserManager) {
					throw new SecurityException(logError(MSG_ILLEGAL_DATA_MODIFICATION, MSG_ID_ILLEGAL_DATA_MODIFICATION, loginUsername));
				}
				userChanged = true;
				Authority newAuthority = new Authority();
				newAuthority.setAuthority(restAuthority);
				newAuthority.setUser(modelUser);
				newAuthorities.add(newAuthority);
				if (logger.isTraceEnabled()) logger.trace("Authority granted: {}", restAuthority);
			}
		}
		modelUser.setAuthorities(newAuthorities);
		
		// Save user only if anything was actually changed
		if (userChanged) {
			modelUser = userRepository.save(modelUser);
			logInfo(MSG_USER_MODIFIED, MSG_ID_USER_MODIFIED, username);
		} else {
			throw new NotModifiedException(logInfo(MSG_USER_NOT_MODIFIED, MSG_ID_USER_NOT_MODIFIED, username));
		}
		
		// Return the changed user
		return toRestUser(modelUser);
	}

}
