/**
 * GUIAuthenticationProvider.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.model.enums.UserRole;
import de.dlr.proseo.ui.backend.LoginManager;

/**
 * Implementation of the AuthenticationProvider interface provided by Spring Security, handling the authentication process for the
 * GUI users.
 * 
 * @author David Mazo
 */
@Component
public class GUIAuthenticationProvider implements AuthenticationProvider {

	/** The login manager */
	@Autowired
	private LoginManager loginManager;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GUIAuthenticationProvider.class);

	/**
	 * Authenticates the user based on the provided credentials.
	 *
	 * @param authentication the authentication object containing the user's credentials
	 * @return the authenticated authentication object
	 * @throws AuthenticationException if authentication fails
	 */
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		Object principal = authentication.getPrincipal();

		if (principal instanceof String) {
			
			// Split the username into mission and username parts
			String[] userNameParts = ((String) principal).split("/");

			if (userNameParts.length != 2) {
				logger.log(UIMessage.INVALID_USERNAME, (Object[]) userNameParts);
				throw new BadCredentialsException("Invalid Username (mission missing?)");
			}

			String mission = userNameParts[0];
			String userName = userNameParts[1];
			String password = (String) authentication.getCredentials();

			// Perform login and role validation
			if (loginManager.doLogin(userName, password, mission, false) && loginManager.hasRole(UserRole.GUI_USER)) {
				
				// Create a new authenticated GUIAuthenticationToken
				GUIAuthenticationToken newAuthentication = new GUIAuthenticationToken();
				UserDetails newPrincipal = new User(userName, password, authentication.getAuthorities());
				newAuthentication.setPrincipal(newPrincipal);
				newAuthentication.setCredentials(authentication.getCredentials());
				newAuthentication.setDetails(mission);
				newAuthentication.setNewLogin(true);
				newAuthentication.setAuthenticated(true);

				// Retrieve user roles and sort them alphabetically
				List<String> roles = new ArrayList<>();
				for (String role : loginManager.getRoles()) {
					roles.add(role.replaceFirst("ROLE_", ""));
				}
				Comparator<String> c = Comparator.comparing((String x) -> x);
				roles.sort(c);
				newAuthentication.setUserRoles(roles);

				return newAuthentication;
			} else {
				logger.log(UIMessage.LOGIN_FAILED, userName);
				throw new BadCredentialsException("Login failed");
			}
		} else {
			logger.log(UIMessage.UNKNOWN_AUTHENTICATION_TYPE, authentication);
			throw new BadCredentialsException("Unknown authentication Type");
		}
	}

	/**
	 * Checks if the authentication provider supports the specified authentication class.
	 *
	 * @param authentication the authentication class to check
	 * @return true if the authentication provider supports the class, false otherwise
	 */
	@Override
	public boolean supports(Class<?> authentication) {
		if (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication)) {
			return true;
		} else {
			return false;
		}
	}

}