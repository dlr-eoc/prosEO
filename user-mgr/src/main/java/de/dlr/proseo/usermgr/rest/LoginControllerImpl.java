/**
 * LoginControllerImpl.java
 */
package de.dlr.proseo.usermgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.usermgr.rest.model.RestAuthority;

/**
 * @author thomas
 *
 */
@Component
public class LoginControllerImpl implements LoginController {

	/* Message ID constants */
	private static final int MSG_ID_MISSION_NOT_FOUND = 2700;
	private static final int MSG_ID_USER_NOT_AUTHORIZED = 2701;
	
	/* Message string constants */
	private static final String MSG_USER_NOT_AUTHORIZED = "(E%d) User %s has no authorities for mission %s";
	private static final String MSG_MISSION_NOT_FOUND = "(E%d) Access control list for mission %s not found";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-user-mgr ";

	/* Other string constants */
	private static final String ROLE_ROOT = "ROOT";
	
	/** The ACL service */
	@Autowired
	AclService aclService;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(LoginControllerImpl.class);

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
	 * Let a user log in to a specific mission (the user is retrieved from the basic authentication information)
	 * 
	 * @param mission the code of the mission to log in to
	 * @return a list of authorities the user holds for this mission
	 */
	@Override
	public ResponseEntity<List<RestAuthority>> login(String mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> login({})", mission);
		
		// Get the user requesting authentication
		// Since successful authentication is required for accessing "login", we trust that the authentication object is filled
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (logger.isTraceEnabled()) {
			logger.trace("Found authentication: " + auth + " with username " + auth.getName() + " and password " + 
					(((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]" ) );
		}
		final String username = auth.getName();
		final Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
		
		// Query the user management database
		List<RestAuthority> authoritiesFound = new ArrayList<>();
		
		// Check whether the user is authenticated for the given mission
		ObjectIdentity missionIdentity = new ObjectIdentityImpl(Mission.class, mission);
		
		Acl acl;
		try {
			acl = aclService.readAclById(missionIdentity);
		} catch (NotFoundException e) {
			String message = String.format(MSG_MISSION_NOT_FOUND, MSG_ID_MISSION_NOT_FOUND, mission);
			logger.error(message);
			return new ResponseEntity<>(errorHeaders(message), HttpStatus.UNAUTHORIZED);
		}
		
		// Collect authorities for this user and mission
		for (GrantedAuthority authority: authorities) {
			Sid sid = new GrantedAuthoritySid(authority.getAuthority());
			if (acl.isSidLoaded(Arrays.asList(sid)) || ROLE_ROOT.equals(authority.getAuthority())) {
				// Actual permissions are not of interest here!
				RestAuthority restAuthority = new RestAuthority();
				restAuthority.setAuthority(authority.getAuthority());
				restAuthority.setObjectClass(Mission.class.getCanonicalName());
				restAuthority.setObjectIdentifier(mission);
				authoritiesFound.add(restAuthority);
			}
		}
		
		if (authorities.isEmpty()) {
			String message = String.format(MSG_USER_NOT_AUTHORIZED, MSG_ID_USER_NOT_AUTHORIZED, username, mission);
			logger.error(message);
			return new ResponseEntity<>(errorHeaders(message), HttpStatus.UNAUTHORIZED);
		}
		
		return new ResponseEntity<>(authoritiesFound, HttpStatus.OK);
	}

}
