/**
 * UserControllerImpl.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import java.util.ConcurrentModificationException;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.usermgr.rest.model.RestUser;

/**
 * Spring MVC controller for the prosEO User Manager; implements the services required to manage user accounts.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class UserControllerImpl implements UserController {

	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-user-mgr ";

	/** The user manager */
	@Autowired
	private UserManager userManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(UserControllerImpl.class);

	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + message.replaceAll("\n", " "));
		return responseHeaders;
	}
	
	/**
	 * Get users by mission
	 * 
	 * @param mission the mission code
	 * @return HTTP status "OK" and a list of Json objects representing users authorized for the given mission or
	 *         HTTP status "NOT_FOUND" and an error message, if no users matching the search criteria were found
	 */
	@Override
	public ResponseEntity<List<RestUser>> getUsers(String mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> getUsers({})", mission);
		
		try {
			return new ResponseEntity<>(userManager.getUsers(mission), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Create a new user
	 * 
	 * @param restUser a Json representation of the new user
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the user after persistence
	 *             (with ACL security identity ID) or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestUser> createUser(@Valid RestUser restUser) {
		if (logger.isTraceEnabled()) logger.trace(">>> createUser({})", (null == restUser ? "MISSING" : restUser.getUsername()));

		try {
			return new ResponseEntity<>(userManager.createUser(restUser), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Get a user by name
	 * 
	 * @param username the user name
	 * @return HTTP status "OK" and a Json object corresponding to the user found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no user name was given, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no user with the given name exists
	 */
	@Override
	public ResponseEntity<RestUser> getUserByName(String username) {
		if (logger.isTraceEnabled()) logger.trace(">>> getUserByName({})", username);
		
		try {
			return new ResponseEntity<>(userManager.getUserByName(username), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Delete a user by user name
	 * 
	 * @param username the name of the user to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the user did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteUserByName(String username) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteUserByName({})", username);
		
		try {
			userManager.deleteUserByName(username);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * Update a user by name
	 * 
	 * @param username the name of the user to update
	 * @param restUser a Json object containing the modified (and unmodified) attributes
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the user after modification or 
	 * 		   HTTP status "NOT_FOUND" and an error message, if no user with the given name exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "CONFLICT"and an error message, if the user has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestUser> modifyUser(String username, RestUser restUser) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyUser({}, {})", username, (null == restUser ? "MISSING" : restUser.getUsername()));
		
		try {
			return new ResponseEntity<>(userManager.modifyUser(username, restUser), HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		}
	}

}
