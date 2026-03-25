/**
 * GroupControllerImpl.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import java.util.ConcurrentModificationException;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.usermgr.rest.model.RestGroup;
import de.dlr.proseo.usermgr.rest.model.RestUser;

/**
 * Spring MVC controller for the prosEO User Manager; implements the services
 * required to manage user groups.
 *
 * @author Dr. Thomas Bassler
 */
@Component
public class GroupControllerImpl implements GroupController {

	/** The user manager */
	@Autowired
	private GroupManager groupManager;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GroupControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.USER_MGR);

	/**
	 * Get user groups by mission and optionally by group name
	 *
	 * @param mission    the mission code and optionally by group name
	 * @param groupName  the group name (optional)
	 * @param recordFrom first record of filtered and ordered result to return
	 * @param recordTo   last record of filtered and ordered result to return
	 * @return HTTP status "OK" and a list of Json objects representing groups
	 *         authorized for the given mission or HTTP status "TOO MANY REQUESTS"
	 *         if the result list exceeds a configured maximum HTTP status
	 *         "NOT_FOUND" and an error message, if no groups matching the search
	 *         criteria were found
	 */
	@Override
	public ResponseEntity<List<RestGroup>> getGroups(String mission, String groupName, Integer recordFrom, Integer recordTo) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getGroups({}, {})", mission, groupName);

		try {
			return new ResponseEntity<>(groupManager.getGroups(mission, groupName, recordFrom, recordTo), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (HttpClientErrorException.TooManyRequests e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.TOO_MANY_REQUESTS);
		}
	}

	/**
	 * Create a new user group
	 *
	 * @param restGroup a Json representation of the new user group
	 * @return HTTP status "CREATED" and a response containing a Json object
	 *         corresponding to the group after persistence (with ACL security
	 *         identity ID) or HTTP status "BAD_REQUEST", if any of the input data
	 *         was invalid
	 */
	@Override
	public ResponseEntity<RestGroup> createGroup(@Valid RestGroup restGroup) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createGroup({})", (null == restGroup ? "MISSING" : restGroup.getGroupname()));

		try {
			return new ResponseEntity<>(groupManager.createGroup(restGroup), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Get a user group by ID
	 *
	 * @param id the group ID
	 * @return HTTP status "OK" and a Json object corresponding to the group found
	 *         or HTTP status "BAD_REQUEST" and an error message, if no group ID was
	 *         given, or HTTP status "NOT_FOUND" and an error message, if no group
	 *         with the given ID exists
	 */
	@Override
	public ResponseEntity<RestGroup> getGroupById(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getGroupById({})", id);

		try {
			return new ResponseEntity<>(groupManager.getGroupById(id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Delete a user group by ID
	 *
	 * @param id the group ID
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was
	 *         successful, or HTTP status "NOT_FOUND", if the group did not exist,
	 *         or HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteGroupById(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteGroupById({})", id);

		try {
			groupManager.deleteGroupById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * Update a user group by ID
	 *
	 * @param id        the ID of the group to update
	 * @param restGroup a Json object containing the modified (and unmodified)
	 *                  attributes
	 * @return HTTP status "OK" and a response containing a Json object
	 *         corresponding to the user after modification or HTTP status
	 *         "NOT_MODIFIED" and a warning message, if the input date was the same
	 *         as the database data, or HTTP status "NOT_FOUND" and an error
	 *         message, if no user group with the given ID exists, or HTTP status
	 *         "BAD_REQUEST" and an error message, if any of the input data was
	 *         invalid, or HTTP status "CONFLICT" and an error message, if the user
	 *         has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestGroup> modifyGroup(Long id, RestGroup restGroup) {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyGroup({}, {})", id, (null == restGroup ? "MISSING" : restGroup.getGroupname()));

		try {
			return new ResponseEntity<>(groupManager.modifyGroup(id, restGroup), HttpStatus.OK);
		} catch (GroupManager.NotModifiedException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		}
	}

	/**
	 * Get all members of the given user group
	 *
	 * @param id the ID of the user group
	 * @return HTTP status "OK" and a list of Json objects representing users, which
	 *         are members of the given group or HTTP status "NOT_FOUND" and an
	 *         error message, if the group has no members
	 */
	@Override
	public ResponseEntity<List<RestUser>> getGroupMembers(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getGroupMembers({})", id);

		try {
			return new ResponseEntity<>(groupManager.getGroupMembers(id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Add a member to the given user group
	 *
	 * @param id       the ID of the group to update
	 * @param username the name of the user to add
	 * @return HTTP status "OK" and a response containing a Json object
	 *         corresponding to the list of users after addition or HTTP status
	 *         "NOT_MODIFIED" and a warning message, if the user is already a member
	 *         of the group, or HTTP status "NOT_FOUND" and an error message, if no
	 *         user group with the given ID or no user with the given name exists,
	 *         or HTTP status "BAD_REQUEST" and an error message, if any of the
	 *         input data was invalid
	 */
	@Override
	public ResponseEntity<List<RestUser>> addGroupMember(Long id, String username) {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyGroup({}, {})", id, username);

		try {
			return new ResponseEntity<>(groupManager.addGroupMember(id, username), HttpStatus.CREATED);
		} catch (GroupManager.NotModifiedException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Delete a member from the given user group
	 *
	 * @param id       the group ID
	 * @param username the name of the user to remove
	 * @return HTTP status "OK" and a response containing a Json object
	 *         corresponding to the list of users after removal or HTTP status
	 *         "NOT_MODIFIED" and a warning message, if the user is not a member of
	 *         the group, or HTTP status "NOT_FOUND", if the group did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<List<RestUser>> removeGroupMember(Long id, String username) {
		if (logger.isTraceEnabled())
			logger.trace(">>> removeGroupMember({}, {})", id, username);

		try {
			groupManager.removeGroupMember(id, username);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (GroupManager.NotModifiedException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * Count the groups matching the specified mission.
	 *
	 * @param missionCode the mission code
	 * @return the number of matching groups as a String (may be zero) or HTTP
	 *         status "BAD_REQUEST" if the request was not made by the root user or
	 *         no mission was provided
	 */
	@Override
	public ResponseEntity<String> countGroups(String missionCode) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countGroups({})", missionCode);

		try {
			return new ResponseEntity<>(groupManager.countGroups(missionCode), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

}