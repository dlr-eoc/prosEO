package de.dlr.proseo.ui.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import de.dlr.proseo.ui.backend.LoginManager;

@Component
public class GUIAuthenticationProvider implements AuthenticationProvider {
	@Autowired
	private LoginManager loginManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GUIAuthenticationProvider.class);

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		Object principal = authentication.getPrincipal();
		if (principal instanceof String) {
			String[] userNameParts = ((String) principal).split("\\\\"); // --> regex "\\" --> matches "\"
			if (userNameParts.length != 2) {
				logger.error("Invalid Username (mission missing?): " + userNameParts);
				throw new BadCredentialsException("Invalid Username (mission missing?)");
			}
			String mission = userNameParts[0];
			String userName = userNameParts[1];
			String password = (String) authentication.getCredentials();

			if (loginManager.doLogin(userName, password, mission)) {
				GUIAuthenticationToken newAuthentication = new GUIAuthenticationToken();
				UserDetails newPrincipal = new User(userName, password, authentication.getAuthorities());
				newAuthentication.setPrincipal(newPrincipal);
				newAuthentication.setCredentials(authentication.getCredentials());
				newAuthentication.setDetails(mission);
				newAuthentication.setAuthenticated(true);
				return newAuthentication;
			} else {
				logger.error("Login failed for user: " + userName);
				throw new BadCredentialsException("Login failed");
			}

		} else {
			logger.error("Unknown authentication Type: " + authentication);
			throw new BadCredentialsException("Unknown authentication Type");
		}
	}

	@Override
	public boolean supports(Class<?> authentication) {
		if (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication)) {
			return true;
		} else {
			return false;
		}
	}

}
