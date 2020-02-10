/**
 * AclobjectControllerImpl.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.AlreadyExistsException;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.usermgr.rest.model.RestAclObject;

/**
 * Spring MVC controller for the prosEO User Manager; implements the services required to manage ACL object identities.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class AclobjectControllerImpl implements AclobjectController {

	/* Message ID constants */
	private static final int MSG_ID_MISSION_NOT_FOUND = 2700;
	private static final int MSG_ID_USER_NOT_AUTHORIZED = 2701;
	private static final int MSG_ID_MISSION_MISSING = 2702;
	private static final int MSG_ID_NO_MISSIONS_FOUND = 2710;
	private static final int MSG_ID_NO_ACLS_FOUND = 2711;
	private static final int MSG_ID_ACLOBJECT_LIST_RETRIEVED = 2712;
	private static final int MSG_ID_ACLOBJECT_EXISTS = 2713;
	private static final int MSG_ID_ACLOBJECT_CREATED = 2714;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 2715;
	private static final int MSG_ID_ACLOBJECT_DELETED = 2716;
	
	/* Message string constants */
	private static final String MSG_USER_NOT_AUTHORIZED = "(E%d) User %s has no authorities for mission %s";
	private static final String MSG_MISSION_NOT_FOUND = "(E%d) Access control list for mission %s not found";
	private static final String MSG_MISSION_MISSING = "(E%d) No mission given for login";
	private static final String MSG_NO_MISSIONS_FOUND = "(E%d) No missions found in database";
	private static final String MSG_NO_ACLS_FOUND = "(E%d) No mission ACLs found in database";
	private static final String MSG_ACLOBJECT_EXISTS = "(E%d) ACL object identity of class %s with ID %s already exists";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) ACL object identity deletion unsuccessful for class %s and ID %s";

	private static final String MSG_ACLOBJECT_LIST_RETRIEVED = "(I%d) ACL object identities retrieved";
	private static final String MSG_ACLOBJECT_CREATED = "(I%d) ACL object identity of class %s with ID %s created";
	private static final String MSG_ACLOBJECT_DELETED = "(I%d) ACL object identity of class %s with ID %s deleted";

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-user-mgr ";

	/* Other string constants */
	private static final String ROLE_ROOT = "ROLE_ROOT";
	
	/** The ACL service */
	@Autowired
	MutableAclService aclService;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(LoginControllerImpl.class);

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
	 * Get all ACL object identities
	 * 
	 * @return HTTP status "OK" and a list of Json objects representing ACL object identities satisfying the search criteria or
	 *         HTTP status "NOT_FOUND" and an error message, if no ACL object identities were found
	 */
	@Override
	public ResponseEntity<List<RestAclObject>> getAclObjects() {
		if (logger.isTraceEnabled()) logger.trace(">>> getAclObjects()");
		
		// Find all missions
		List<Mission> missions = RepositoryService.getMissionRepository().findAll();
		if (missions.isEmpty()) {
			return new ResponseEntity<>(errorHeaders(logError(MSG_NO_MISSIONS_FOUND, MSG_ID_NO_MISSIONS_FOUND)), HttpStatus.NOT_FOUND);
		}
		
		// Select the missions that actually have ACLs
		List<ObjectIdentity> objectIdentities = new ArrayList<>();
		for (Mission mission: missions) {
			objectIdentities.add(new ObjectIdentityImpl(Mission.class, mission.getId()));
		}
		Map<ObjectIdentity, Acl> acls = null;
		try {
			acls = aclService.readAclsById(objectIdentities);
		} catch (NotFoundException e1) {
			return new ResponseEntity<>(errorHeaders(logError(MSG_NO_ACLS_FOUND, MSG_ID_NO_ACLS_FOUND)), HttpStatus.NOT_FOUND);
		}
		
		// Return all object identities with ACLs
		List<RestAclObject> result = new ArrayList<>();
		for (ObjectIdentity objectIdentity: acls.keySet()) {
			RestAclObject restAclObject = new RestAclObject();
			restAclObject.setObjectClass(objectIdentity.getType());
			restAclObject.setObjectIdentifier(String.valueOf(objectIdentity.getIdentifier()));
			// TODO set ID and owner sid or remove attributes from RestAclObject
			result.add(restAclObject);
		}

		logInfo(MSG_ACLOBJECT_LIST_RETRIEVED, MSG_ID_ACLOBJECT_LIST_RETRIEVED);
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	/**
	 * Create a new ACL object identity
	 * 
	 * @param restAclObject a Json representation of the new ACL object identity
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the ACL object identity after persistence or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestAclObject> createAclObject(@Valid RestAclObject restAclObject) {
		if (logger.isTraceEnabled()) logger.trace(">>> createAclObject({})", restAclObject.getObjectIdentifier());
		
		ObjectIdentity objectIdentity = new ObjectIdentityImpl(restAclObject.getObjectClass(), Long.parseLong(restAclObject.getObjectIdentifier()));
		
		try {
			aclService.createAcl(objectIdentity);
		} catch (AlreadyExistsException e) {
			return new ResponseEntity<>(errorHeaders(logError(
					MSG_ACLOBJECT_EXISTS, MSG_ID_ACLOBJECT_EXISTS, restAclObject.getObjectClass(), restAclObject.getObjectIdentifier())), HttpStatus.BAD_REQUEST);
		}
		
		logInfo(MSG_ACLOBJECT_CREATED, MSG_ID_ACLOBJECT_CREATED, restAclObject.getObjectClass(), restAclObject.getObjectIdentifier());
		
		return new ResponseEntity<>(restAclObject, HttpStatus.CREATED);
	}

	/**
	 * Delete an ACL object identity by identifier
	 * 
	 * @param clazz the canonical class name of the object identity
	 * @param identifier the (database) identifier for the object identity
	 * @return a response entity with HTTP status "NO_CONTENT"
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteAclObjectById(String clazz, Long identifier) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteAclObjectById({}, {})", clazz, identifier);
		
		// Find and delete the ACL of the given class and ID
		ObjectIdentity objectIdentity = new ObjectIdentityImpl(clazz, identifier);
		
		aclService.deleteAcl(objectIdentity, true);
		
		// Make sure the ACL is gone
		try {
			aclService.readAclById(objectIdentity);
		} catch (NotFoundException e) {
			// OK, deletion successful
			logInfo(MSG_ACLOBJECT_DELETED, MSG_ID_ACLOBJECT_DELETED, clazz, identifier);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		}
		
		// Failure: The ACL is still there
		return new ResponseEntity<>(
				errorHeaders(logError(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, clazz, identifier)),
				HttpStatus.NOT_MODIFIED);
	}

}
