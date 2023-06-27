/**
 * LoginControllerImpl.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UserMgrMessage;
import de.dlr.proseo.model.enums.UserRole;
import de.dlr.proseo.usermgr.rest.model.RestUser;

/**
 * Spring MVC controller for the prosEO User Manager; implements the login
 * service.
 *
 * @author Dr. Thomas Bassler
 */
@Component
public class LoginControllerImpl implements LoginController {

	/* Other string constants */
	private static final String ROLE_ROOT = UserRole.ROOT.asRoleString();

	/* The user manager component */
	@Autowired
	private UserManager userManager;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(LoginControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.USER_MGR);

	/**
	 * Let a user log in to a specific mission (the user is retrieved from the basic
	 * authentication information). Mission may be empty for the "root" user.
	 *
	 * @param mission the code of the mission to log in to
	 * @return a list of authorities the user holds for this mission (directly or
	 *         indirectly)
	 */
	@Override
	public ResponseEntity<List<String>> login(String mission) {
		if (logger.isTraceEnabled())
			logger.trace(">>> login({})", mission);

		// Get the user requesting authentication
		// Since successful authentication is required for accessing "login", we trust
		// that the authentication object is filled
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (logger.isTraceEnabled()) {
			logger.trace("Found authentication: " + auth + " with username " + auth.getName() + " and password "
					+ (((UserDetails) auth.getPrincipal()).getPassword() == null ? "null" : "[protected]"));
		}
		String username = auth.getName();
		Collection<? extends GrantedAuthority> authorities = auth.getAuthorities(); // Includes group authorities

		// Query the user management database
		List<String> authoritiesFound = new ArrayList<>();

		// Collect authorities and handle root user: No check against missions required
		boolean isRootUser = false;
		for (GrantedAuthority authority : authorities) {
			if (logger.isTraceEnabled())
				logger.trace("... checking granted authority " + authority.getAuthority());
			authoritiesFound.add(authority.getAuthority());
			if (ROLE_ROOT.equals(authority.getAuthority())) {
				isRootUser = true;
			}
		}

		// Check whether mission is set
		if (!isRootUser && (null == mission || mission.isBlank())) {
			String message = logger.log(UserMgrMessage.MISSION_MISSING);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
		}

		// Check whether user is authorized for this mission
		if (!isRootUser && !username.startsWith(mission + "-")) {
			String message = logger.log(UserMgrMessage.USER_NOT_AUTHORIZED, username, mission);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.UNAUTHORIZED);
		}

		// Check account expiration
		RestUser loginUser = userManager.getUserByName(username);
		Date now = new Date();
		if (loginUser.getExpirationDate().before(now)) {
			String message = logger.log(UserMgrMessage.ACCOUNT_EXPIRED, username);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.UNAUTHORIZED);
		}

		// Check password expiration
		if (loginUser.getPasswordExpirationDate().before(now)) {
			String message = logger.log(UserMgrMessage.PASSWORD_EXPIRED, username);
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.UNAUTHORIZED);
		}

		return new ResponseEntity<>(authoritiesFound, HttpStatus.OK);
	}

}