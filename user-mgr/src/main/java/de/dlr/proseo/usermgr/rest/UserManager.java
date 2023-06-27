/**
 * UserManager.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.UserMgrMessage;
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
	private static ProseoLogger logger = new ProseoLogger(UserManager.class);

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
	 * Convert a user from REST format to the prosEO data model format (including
	 * directly assigned authorities)
	 *
	 * @param restUser the REST user to convert
	 * @return the converted model user
	 * @throws IllegalArgumentException if an invalid authority value was given
	 */
	/* package */ static User toModelUser(RestUser restUser) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> toModelUser({})", (null == restUser ? "MISSING" : restUser.getUsername()));

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
			modelQuota.setAssigned(restQuota.getAssigned());
			modelQuota.setUsed(restQuota.getUsed());
			modelQuota.setLastAccessDate(restQuota.getLastAccessDate());
			modelUser.setQuota(modelQuota);
		}
		for (String restAuthority : restUser.getAuthorities()) {
			// Test whether authority is legal
			try {
				UserRole.asRole(restAuthority);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(logger.log(UserMgrMessage.ILLEGAL_AUTHORITY, restAuthority));
			}

			Authority modelAuthority = new Authority();
			modelAuthority.setAuthority(restAuthority);
			modelAuthority.setUser(modelUser);
			modelUser.getAuthorities().add(modelAuthority);
		}

		if (logger.isTraceEnabled())
			logger.trace("<<< toModelUser()");
		return modelUser;
	}

	/**
	 * Convert a user from prosEO data model format to REST format (including
	 * directly assigned authorities)
	 *
	 * @param modelUser the model user to convert
	 * @return the converted REST user
	 */
	/* package */ static RestUser toRestUser(User modelUser) {
		if (logger.isTraceEnabled())
			logger.trace(">>> toRestUser({})", (null == modelUser ? "MISSING" : modelUser.getUsername()));

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
			restUser.setQuota(new RestQuota(modelQuota.getAssigned(), modelQuota.getUsed(), modelQuota.getLastAccessDate()));
		}
		for (Authority modelAuthority : modelUser.getAuthorities()) {
			restUser.getAuthorities().add(modelAuthority.getAuthority());
		}

		return restUser;
	}

	/**
	 * Create a user (optionally with direct authorities)
	 *
	 * @param restUser a Json representation of the new user
	 * @return a Json representation of the user after creation (with ACL security
	 *         identity ID)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 */
	public RestUser createUser(RestUser restUser) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createUser({})", (null == restUser ? "MISSING" : restUser.getUsername()));

		// Check parameter
		if (null == restUser) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.USER_MISSING));
		}
		if (null == restUser.getUsername() || restUser.getUsername().isBlank()) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.USERNAME_MISSING));
		}
		if (null == restUser.getPassword() || restUser.getPassword().isBlank()) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.PASSWORD_MISSING));
		}
		if (null == restUser.getAuthorities()) {
			restUser.setAuthorities(new ArrayList<String>());
		}

		// Make sure user does not exist already
		if (null != userRepository.findByUsername(restUser.getUsername())) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.DUPLICATE_USER, restUser.getUsername()));
		}

		// Create user
		User modelUser = userRepository.save(toModelUser(restUser));

		logger.log(UserMgrMessage.USER_CREATED, modelUser.getUsername());

		return toRestUser(modelUser);
	}

	/**
	 * Get users by mission
	 *
	 * @param mission    the mission code
	 * @param recordFrom first record of filtered and ordered result to return
	 * @param recordTo   last record of filtered and ordered result to return
	 * @return a list of Json objects representing the users authorized for the
	 *         given mission
	 * @throws NoResultException        if no user is found for the given mission
	 * @throws HttpClientErrorException if the result list exceeds a configured
	 *                                  maximum
	 */
	public List<RestUser> getUsers(String mission, Integer recordFrom, Integer recordTo)
			throws NoResultException, HttpClientErrorException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getUsers({})", mission);

		// Check whether principal has ROOT role
		boolean isRootUser = false;
		Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext()
			.getAuthentication()
			.getAuthorities();
		for (GrantedAuthority authority : authorities) {
			if (logger.isTraceEnabled())
				logger.trace("... checking granted authority " + authority.getAuthority());
			if (ROLE_ROOT.equals(authority.getAuthority())) {
				isRootUser = true;
			}
		}

		// Check parameter
		if (!isRootUser && (null == mission || mission.isBlank())) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.MISSION_MISSING));
		}

		// Collect all users within the configured range whose user name starts with the
		// mission code (or all users, if a root user starts the request without a
		// mission)
		if (recordFrom == null) {
			recordFrom = 0;
		}
		if (recordTo == null) {
			recordTo = Integer.MAX_VALUE;
		}

		Long numberOfResults = Long.parseLong(this.countUsers(mission));
		Integer maxResults = config.getMaxResults();
		if (numberOfResults > maxResults && (recordTo - recordFrom) > maxResults && (numberOfResults - recordFrom) > maxResults) {
			throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS,
					logger.log(GeneralMessage.TOO_MANY_RESULTS, "users", numberOfResults, config.getMaxResults()));
		}

		List<RestUser> result = new ArrayList<>();

		String jpqlQuery = "select u from users u";

		if (!isRootUser || !(null == mission || mission.isBlank())) {
			jpqlQuery += " where u.username like concat(:missionCode, '-%')";
		}

		Query query = em.createQuery(jpqlQuery);
		if (!isRootUser || !(null == mission || mission.isBlank())) {
			query.setParameter("missionCode", mission);
		}
		query.setFirstResult(recordFrom);
		query.setMaxResults(recordTo - recordFrom);

		for (Object resultObject : query.getResultList()) {
			if (resultObject instanceof User) {
				result.add(toRestUser((User) resultObject));
			}
		}

		if (result.isEmpty()) {
			throw new NoResultException(logger.log(UserMgrMessage.USER_NOT_FOUND, mission));
		}

		logger.log(UserMgrMessage.USER_LIST_RETRIEVED, mission);

		return result;
	}

	/**
	 * Get a user by name
	 *
	 * @param username the user name
	 * @return a Json object corresponding to the user found
	 * @throws IllegalArgumentException if no user name was given
	 * @throws NoResultException        if no user with the given name exists
	 */
	public RestUser getUserByName(String username) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getUserByName({})", username);

		if (null == username || username.isBlank()) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.USERNAME_MISSING));
		}

		// Check permission to read the user data (only ROOT and USERMGR may read all
		// user data, regular users may only read their own data)

		// Since successful authentication is required for accessing "login", we trust
		// that the authentication object is filled
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String loginUsername = auth.getName();
		Collection<? extends GrantedAuthority> authorities = auth.getAuthorities(); // Includes group authorities

		// Collect authorities and handle root user: No check against missions required
		boolean isUserManager = false;
		for (GrantedAuthority authority : authorities) {
			if (ROLE_ROOT.equals(authority.getAuthority()) || ROLE_USERMGR.equals(authority.getAuthority())) {
				isUserManager = true;
			}
		}

		if (!isUserManager && !username.equals(loginUsername)) {
			throw new SecurityException(logger.log(UserMgrMessage.ILLEGAL_DATA_ACCESS, loginUsername, username));
		}

		User modelUser = userRepository.findByUsername(username);

		if (null == modelUser) {
			throw new NoResultException(logger.log(UserMgrMessage.USERNAME_NOT_FOUND, username));
		}

		logger.log(UserMgrMessage.USER_RETRIEVED, username);

		return toRestUser(modelUser);
	}

	/**
	 * Delete a user by user name
	 *
	 * @param username the name of the user to delete
	 * @throws EntityNotFoundException if the user to delete does not exist in the
	 *                                 database
	 * @throws RuntimeException        if the deletion was not performed as expected
	 */
	public void deleteUserByName(String username) throws EntityNotFoundException, RuntimeException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteUserByName({})", username);

		if (null == username || username.isBlank()) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.USERNAME_MISSING));
		}

		// Test whether the user name is valid
		User modelUser = userRepository.findByUsername(username);

		if (null == modelUser) {
			throw new NoResultException(logger.log(UserMgrMessage.USERNAME_NOT_FOUND, username));
		}

		// Delete the user
		try {
			modelUser.getAuthorities().clear();
			userRepository.save(modelUser);
			userRepository.delete(modelUser);
		} catch (Exception e) {
			throw new RuntimeException(logger.log(UserMgrMessage.DELETE_FAILURE, username, e.getMessage()));
		}

		// Test whether the deletion was successful
		modelUser = userRepository.findByUsername(username);

		if (null != modelUser) {
			throw new RuntimeException(logger.log(UserMgrMessage.DELETION_UNSUCCESSFUL, username));
		}

		logger.log(UserMgrMessage.USER_DELETED, username);
	}

	/**
	 * Update a user by user name
	 *
	 * If the password is changed, the password expiration date will be updated
	 * according to the password expiration period configured.
	 *
	 * Note: This method cannot detect, whether a password was actually changed,
	 * because due to the BCrypt algorithm used the same password may yield
	 * different salted hashes with each encryption run. It is in the responsibility
	 * of the calling component to make sure that the password was indeed altered
	 * (and that it conforms to any applicable password policy).
	 *
	 * @param username the name of the user to update
	 * @param restUser a Json object containing the modified (and unmodified)
	 *                 attributes
	 * @return a response containing a Json object corresponding to the user after
	 *         modification
	 * @throws EntityNotFoundException  if no user with the given user name exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws NotModifiedException     if the user data was not modified (input
	 *                                  data same as database data)
	 */
	public RestUser modifyUser(String username, RestUser restUser)
			throws EntityNotFoundException, IllegalArgumentException, NotModifiedException {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyUser({}, {})", username, (null == restUser ? "MISSING" : restUser.getUsername()));

		// Check arguments
		if (null == username || username.isBlank()) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.USERNAME_MISSING));
		}
		if (null == restUser) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.USER_DATA_MISSING));
		}
		if (null == restUser.getAuthorities()) {
			restUser.setAuthorities(new ArrayList<String>());
		}

		// Check permission to change the user (only ROOT and USERMGR may change all
		// users, regular users may only change
		// their password (which entails an update of the password expiration date)

		// Since successful authentication is required for accessing "login", we trust
		// that the authentication object is filled
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String loginUsername = auth.getName();
		Collection<? extends GrantedAuthority> authorities = auth.getAuthorities(); // Includes group authorities

		// Collect authorities and handle root user: No check against missions required
		boolean isUserManager = false;
		for (GrantedAuthority authority : authorities) {
			if (ROLE_ROOT.equals(authority.getAuthority()) || ROLE_USERMGR.equals(authority.getAuthority())) {
				isUserManager = true;
			}
		}

		if (!isUserManager && !username.equals(loginUsername)) {
			throw new SecurityException(logger.log(UserMgrMessage.ILLEGAL_DATA_ACCESS, loginUsername, username));
		}

		// Get the user to modify
		User modelUser = userRepository.findByUsername(username);

		if (null == modelUser) {
			throw new NoResultException(logger.log(UserMgrMessage.USERNAME_NOT_FOUND, username));
		}

		// Apply changed attributes (no change of the username allowed)
		User changedUser = toModelUser(restUser);

		boolean userChanged = false;

		if (!modelUser.getEnabled().equals(changedUser.getEnabled())) {
			if (logger.isTraceEnabled())
				logger.trace("Enabled changed from {} to {}", modelUser.getEnabled(), changedUser.getEnabled());
			if (!isUserManager) {
				throw new SecurityException(logger.log(UserMgrMessage.ILLEGAL_DATA_MODIFICATION, loginUsername));
			}
			userChanged = true;
			modelUser.setEnabled(changedUser.getEnabled());
		}
		// Change expiration dates only if modified by at least one day
		if (24 * 60 * 60 * 1000 <= Math.abs(modelUser.getExpirationDate().getTime() - changedUser.getExpirationDate().getTime())) {
			if (logger.isTraceEnabled())
				logger.trace("Expiration changed from {} to {}", modelUser.getExpirationDate(), changedUser.getExpirationDate());
			if (!isUserManager) {
				throw new SecurityException(logger.log(UserMgrMessage.ILLEGAL_DATA_MODIFICATION, loginUsername));
			}
			userChanged = true;
			modelUser.setExpirationDate(changedUser.getExpirationDate());
		}
		if (24 * 60 * 60 * 1000 <= Math
			.abs(modelUser.getPasswordExpirationDate().getTime() - changedUser.getPasswordExpirationDate().getTime())) {
			if (logger.isTraceEnabled())
				logger.trace("Passwd expiration changed from {} to {}", modelUser.getPasswordExpirationDate(),
						changedUser.getPasswordExpirationDate());
			if (!isUserManager) {
				throw new SecurityException(logger.log(UserMgrMessage.ILLEGAL_DATA_MODIFICATION, loginUsername));
			}
			userChanged = true;
			modelUser.setPasswordExpirationDate(changedUser.getPasswordExpirationDate());
		}
		if (null != changedUser.getPassword() && !modelUser.getPassword().equals(changedUser.getPassword())) {
			userChanged = true;
			modelUser.setPassword(changedUser.getPassword());

			// Update password expiration date, if needed
			Date newPasswordExpirationDate = Date
				.from(Instant.now().plus(Long.parseLong(config.getPasswordExpirationTime()), ChronoUnit.DAYS));
			if (modelUser.getPasswordExpirationDate().before(newPasswordExpirationDate)) {
				// Do not reduce longer expiration dates, if they have been granted (esp. those
				// amounting to "never expires")
				modelUser.setPasswordExpirationDate(newPasswordExpirationDate);
			}
		}
		if (null == modelUser.getQuota() && null != changedUser.getQuota() || null != modelUser.getQuota()
				&& !modelUser.getQuota().getAssigned().equals(changedUser.getQuota().getAssigned())) {
			userChanged = true;
			modelUser.getQuota().setAssigned(changedUser.getQuota().getAssigned());
			// "used" and "lastAccessDate" cannot be changed, because they are managed
			// automatically
		}

		// Apply changed authorities
		Set<Authority> newAuthorities = new HashSet<>();
		for (Authority modelAuthority : modelUser.getAuthorities()) {
			boolean authorityChanged = true;
			for (String restAuthority : restUser.getAuthorities()) {
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
					throw new SecurityException(logger.log(UserMgrMessage.ILLEGAL_DATA_MODIFICATION, loginUsername));
				}
				userChanged = true;
				if (logger.isTraceEnabled())
					logger.trace("Authority revoked: {}", modelAuthority.getAuthority());
			}
		}
		for (String restAuthority : restUser.getAuthorities()) {
			boolean authorityChanged = true;
			for (Authority modelAuthority : modelUser.getAuthorities()) {
				if (modelAuthority.getAuthority().equals(restAuthority)) {
					// Unchanged authority
					authorityChanged = false;
					break;
				}
			}
			if (authorityChanged) {
				// This authority was added
				if (!isUserManager) {
					throw new SecurityException(logger.log(UserMgrMessage.ILLEGAL_DATA_MODIFICATION, loginUsername));
				}
				userChanged = true;
				Authority newAuthority = new Authority();
				newAuthority.setAuthority(restAuthority);
				newAuthority.setUser(modelUser);
				newAuthorities.add(newAuthority);
				if (logger.isTraceEnabled())
					logger.trace("Authority granted: {}", restAuthority);
			}
		}
		modelUser.setAuthorities(newAuthorities);

		// Save user only if anything was actually changed
		if (userChanged) {
			modelUser = userRepository.save(modelUser);
			logger.log(UserMgrMessage.USER_MODIFIED, username);
		} else {
			throw new NotModifiedException(logger.log(UserMgrMessage.USER_NOT_MODIFIED, username));
		}

		// Return the changed user
		return toRestUser(modelUser);
	}

	/**
	 * Count the users matching the specified mission, if any
	 *
	 * @param mission the mission code
	 * @return the number of users found as string
	 */
	public String countUsers(String mission) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countUsers({})", mission);

		// Check whether principal has ROOT role
		boolean isRootUser = false;
		Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext()
			.getAuthentication()
			.getAuthorities();
		for (GrantedAuthority authority : authorities) {
			if (logger.isTraceEnabled())
				logger.trace("... checking granted authority " + authority.getAuthority());
			if (ROLE_ROOT.equals(authority.getAuthority())) {
				isRootUser = true;
			}
		}

		// Check parameter
		if (!isRootUser && (null == mission || mission.isBlank())) {
			throw new IllegalArgumentException(logger.log(UserMgrMessage.MISSION_MISSING));
		}

		// build query
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = cb.createQuery(Long.class);
		Root<User> userType = query.from(User.class);

		List<Predicate> predicates = new ArrayList<>();

		if (!isRootUser || !(null == mission || mission.isBlank())) {
			predicates.add(cb.equal(cb.substring(userType.get("username"), 0, mission.length()), mission));
		}
		query.select(cb.count(userType)).where(predicates.toArray(new Predicate[predicates.size()]));

		Long result = em.createQuery(query).getSingleResult();

		logger.log(UserMgrMessage.USERS_COUNTED, result, mission);

		return result.toString();
	}

}