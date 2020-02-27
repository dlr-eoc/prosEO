/**
 * LoginControllerImpl.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Spring MVC controller for the prosEO User Manager; implements the login service.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class LoginControllerImpl implements LoginController {

	/* Message ID constants */
	private static final int MSG_ID_MISSION_NOT_FOUND = 2700;
	private static final int MSG_ID_USER_NOT_AUTHORIZED = 2701;
	private static final int MSG_ID_MISSION_MISSING = 2702;
	
	/* Message string constants */
	private static final String MSG_USER_NOT_AUTHORIZED = "(E%d) User %s has no authorities for mission %s";
	private static final String MSG_MISSION_NOT_FOUND = "(E%d) Access control list for mission %s not found";
	private static final String MSG_MISSION_MISSING = "(E%d) No mission given for login";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-user-mgr ";

	/* Other string constants */
	private static final String ROLE_ROOT = "ROLE_ROOT";
	
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
	 * @return a list of authorities the user holds for this mission (directly or indirectly)
	 */
	@Override
	public ResponseEntity<List<String>> login(String mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> login({})", mission);
		
		// Get the user requesting authentication
		// Since successful authentication is required for accessing "login", we trust that the authentication object is filled
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (logger.isTraceEnabled()) {
			logger.trace("Found authentication: " + auth + " with username " + auth.getName() + " and password " + 
					(((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]" ) );
		}
		String username = auth.getName();
		Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();  // TODO Where do the group authorities come from?
		
		// Query the user management database
		List<String> authoritiesFound = new ArrayList<>();
		
		// Collect authorities and handle root user: No check against missions required
		boolean isRootUser = false;
		for (GrantedAuthority authority: authorities) {
			if (logger.isTraceEnabled()) logger.trace("... checking granted authority " + authority.getAuthority());
			authoritiesFound.add(authority.getAuthority());
			if (ROLE_ROOT.equals(authority.getAuthority())) {
				isRootUser = true;
			}
		}
		
		// Check whether mission is set
		if (!isRootUser && (null == mission || "".equals(mission))) {
			String message = String.format(MSG_MISSION_MISSING, MSG_ID_MISSION_MISSING);
			logger.error(message);
			return new ResponseEntity<>(errorHeaders(message), HttpStatus.BAD_REQUEST);
		}
		
		// Check whether user is authorized for this mission
		if (isRootUser || username.startsWith(mission + "-")) {
			return new ResponseEntity<>(authoritiesFound, HttpStatus.OK);
		} else {
			String message = String.format(MSG_USER_NOT_AUTHORIZED, MSG_ID_USER_NOT_AUTHORIZED, username, mission);
			logger.error(message);
			return new ResponseEntity<>(errorHeaders(message), HttpStatus.UNAUTHORIZED);
		}
	}

}
