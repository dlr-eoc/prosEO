/**
 * GroupControllerImpl.java
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

import de.dlr.proseo.usermgr.rest.model.RestGroup;

/**
 * Spring MVC controller for the prosEO User Manager; implements the services required to manage user groups.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class GroupControllerImpl implements GroupController {

	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-user-mgr ";

	/** The user manager */
	@Autowired
	private GroupManager groupManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GroupControllerImpl.class);

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
	 * Get user groups by mission
	 * 
	 * @param mission the mission code
	 * @return HTTP status "OK" and a list of Json objects representing groups authorized for the given mission or
	 *         HTTP status "NOT_FOUND" and an error message, if no groups matching the search criteria were found
	 */
	@Override
	public ResponseEntity<List<RestGroup>> getGroups(String mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> getGroups({})", mission);
		
		try {
			return new ResponseEntity<>(groupManager.getGroups(mission), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Create a new user group
	 * 
	 * @param restGroup a Json representation of the new user group
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the group after persistence
	 *             (with ACL security identity ID) or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestGroup> createGroup(@Valid RestGroup restGroup) {
		if (logger.isTraceEnabled()) logger.trace(">>> createGroup({})", (null == restGroup ? "MISSING" : restGroup.getGroupname()));

		try {
			return new ResponseEntity<>(groupManager.createGroup(restGroup), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Get a user group by ID
	 * 
	 * @param id the group ID
	 * @return HTTP status "OK" and a Json object corresponding to the group found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no group ID was given, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no group with the given ID exists
	 */
	@Override
	public ResponseEntity<RestGroup> getGroupById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getGroupById({})", id);
		
		try {
			return new ResponseEntity<>(groupManager.getGroupById(id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Delete a user group by ID
	 * 
	 * @param id the group ID
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the group did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteGroupById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteGroupById({})", id);
		
		try {
			groupManager.deleteGroupById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * Update a user group by ID
	 * 
	 * @param id the ID of the group to update
	 * @param restGroup a Json object containing the modified (and unmodified) attributes
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the user after modification or 
	 * 		   HTTP status "NOT_FOUND" and an error message, if no user with the given ID exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "CONFLICT"and an error message, if the user has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestGroup> modifyGroup(Long id, RestGroup restGroup) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyGroup({}, {})", id, (null == restGroup ? "MISSING" : restGroup.getGroupname()));
		
		try {
			return new ResponseEntity<>(groupManager.modifyGroup(id, restGroup), HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		}
	}

}
