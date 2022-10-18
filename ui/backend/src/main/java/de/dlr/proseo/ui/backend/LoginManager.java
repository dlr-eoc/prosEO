/**
 * LoginManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.backend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.UIMessage;
import de.dlr.proseo.model.enums.UserRole;

/**
 * Management of user login and logout (thread safe)
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class LoginManager {

	/* General string constants */
	private static final String PROSEO_USERNAME_PROMPT = "Username (empty field cancels): ";
	private static final String PROSEO_PASSWORD_PROMPT = "Password for user %s: ";
	public static final String MISSION_PREFIX_CHAR = "-";
	
	/** The logged in user (as used for authentication, i. e. including mission prefix) */
	private ThreadLocal<String> username = new ThreadLocal<>();
	/** The user's password */
	private ThreadLocal<String> password = new ThreadLocal<>();
	/** The mission to which the user has logged in */
	private ThreadLocal<String> mission = new ThreadLocal<>();
	/** The authorities granted to the user */
	private ThreadLocal<List<String>> authorities = new ThreadLocal<>();
	
	/** Minimum password length */
	private static Integer passwordMinLength = null;
	/** Minimum number of character elements */
	private static Integer passwordMinElements = null;
	/** Characters counting as "special" */
	private static String passwordSpecialChars = null;

	/** The configuration object for the prosEO User Interface */
	@Autowired
	private ServiceConfiguration backendConfig;
	
	/** The connector service to the prosEO backend services prosEO */
	@Autowired
	private ServiceConnection backendConnector;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(LoginManager.class);
	
	/**
	 * Login to prosEO for the given mission with the given username and password
	 * 
	 * @param username the username to login with (including mission prefix)
	 * @param password the password to login with
	 * @param mission the mission to log in to (may be null for user with prosEO Administrator privileges)
	 * @return a list of strings denoting authorities granted to the user for the given mission
	 * 		   (may be empty, meaning access is denied)
	 */
	private List<String> login(String username, String password, String mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> login({}, ********, {})", username, mission);
		
		// Attempt connection to Processor Manager (as a substitute)
		List<String> authorities = new ArrayList<>();
		try {
			for (Object authority: backendConnector.getFromService(backendConfig.getUserManagerUrl(),
					"/login" + (null == mission ? "" : "?mission=" + mission), 
					List.class, username, password)) {
				if (authority instanceof String) {
					authorities.add((String) authority);
				}
			};
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled())
				logger.trace("Caught HttpClientErrorException " + e.getMessage());
			if (!(HttpStatus.NOT_FOUND.value() == e.getRawStatusCode())
					&& !(HttpStatus.UNAUTHORIZED.value() == e.getRawStatusCode())) {
				System.err.println(logger.format(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getMessage()));
			}
		} catch (RuntimeException e) {
			System.err.println(logger.format(UIMessage.HTTP_CONNECTION_FAILURE, e.getMessage()));
		}
		
		return authorities;
	}
	
	/**
	 * Log in to prosEO
	 * 
	 * @param username the user name for login (without mission prefix; optional, will be requested from standard input, if not set)
	 * @param password the password for login (optional, will be requested from standard input, if not set)
	 * @param mission the mission to log in to (may be null for user with prosEO Administrator privileges)
	 * @param showLoginMessage if true, show a message upon successful login
	 * @return true, if the login was successful, false otherwise
	 */
	public boolean doLogin(String username, String password, String mission, boolean showLoginMessage) {
		if (logger.isTraceEnabled()) logger.trace(">>> doLogin({}, ********, {})", username, mission);
		
		// Catch missing arguments in non-interactive mode
		if (null == System.console() && (null == username || username.isBlank() || null == password || password.isBlank())) {
			String message = logger.log(UIMessage.INSUFFICIENT_CREDENTIALS);
			System.err.println(message);
			if (logger.isTraceEnabled()) logger.trace("<<< doLogin()");
			return false;
		}
		
		// Ask for username, if not set
		if (null == username || username.isBlank()) {
			System.out.print(PROSEO_USERNAME_PROMPT);
			username = new String(System.console().readLine());
		}
		if (null == username || username.isBlank()) {
			
			String message = "(E" + UIMessage.LOGIN_CANCELLED.getCode() + ") "
					+ UIMessage.LOGIN_CANCELLED.getMessage();
			
			System.err.println(message);

			return false;
		}
		
		// Ask for password, if not set
		if (null == password || password.isBlank()) {
			System.out.print(String.format(PROSEO_PASSWORD_PROMPT, username));
			password = new String(System.console().readPassword());
		}
		
		// Login to prosEO via the User Manager Service
		String missionUsername = (null == mission ? "" : mission + MISSION_PREFIX_CHAR) + username;
		List<String> grantedAuthorities = login(missionUsername, password, mission);
		if (grantedAuthorities.isEmpty()) {
			// Report failure
			String message = null;
			if (message == mission)
				message = logger.log(UIMessage.LOGIN_WITHOUT_MISSION_FAILED, username);
			else
				message = logger.log(UIMessage.NOT_AUTHORIZED_FOR_MISSION, username, mission);
			System.err.println(message);
			if (logger.isTraceEnabled()) logger.trace("<<< doLogin()");
			return false;
		} else {
			// Record login information
			this.username.set(missionUsername);
			this.password.set(password);
			this.mission.set(mission);
			this.authorities.set(grantedAuthorities);
			// Report success
			String message = logger.log(UIMessage.LOGGED_IN, username);
			if (logger.isDebugEnabled()) logger.debug("... with authorities: " + Arrays.toString(grantedAuthorities.toArray()));
			if (showLoginMessage) {
				System.out.println(message);
			}
			if (logger.isTraceEnabled()) logger.trace("<<< doLogin()");
			return true;
		}
		
	}
	
	/**
	 * Log the logged in user out from prosEO
	 */
	public void doLogout() {
		if (logger.isTraceEnabled()) logger.trace(">>> doLogout()");
		String oldUser = username.get();
		
		// Remove login information
		username.remove();
		password.remove();
		mission.remove();
		authorities.remove();
		
		String message = null;
		if (message == oldUser) 
			logger.log(UIMessage.USER_NOT_LOGGED_IN);
			else logger.log(UIMessage.LOGGED_OUT, oldUser);
		System.out.println(message);
		if (logger.isTraceEnabled()) logger.trace("<<< doLogout()");
	}
	
	/**
	 * Gets the name of the logged in user for service authentication (including mission prefix)
	 * 
	 * @return the user name or null, if no user is logged in
	 */
	public String getUser() {
		return username.get();
	}
	
	/**
	 * Gets the password of the logged in user
	 * 
	 * @return the password or null, if no user is logged in
	 */
	public String getPassword() {
		return password.get();
	}
	
	/**
	 * Gets the code of the mission the current user is logged in to
	 * 
	 * @return the mission code or null, if no user is logged in
	 */
	public String getMission() {
		return mission.get();
	}

	/**
	 * Gets the mission prefix consisting of the code of the mission the current user is logged in to appended with "-"
	 * 
	 * @return the mission prefix or an empty string, if no user is logged in, or if the current user is not logged in to any mission
	 */
	public String getMissionPrefix() {
		return (null == mission.get() ? "" : mission.get() + MISSION_PREFIX_CHAR);
	}

	/**
	 * Gets the authorities granted to the user after login
	 * 
	 * @return the granted authorities
	 */
	public List<String> getAuthorities() {
		return authorities.get();
	}

	/**
	 * Checks whether the logged in user has the given role
	 * 
	 * @param role the role to check
	 * @return true, if the respective authority was granted, false otherwise
	 */
	public boolean hasRole(UserRole role) {
		return authorities.get().contains(role.asRoleString());
	}
	
	/**
	 * Get user roles
	 * 
	 * @return string list with user roles
	 */
	public List<String> getRoles() {
		return authorities.get();
	}
	
	/**
	 * Retrieve the configured password strength parameters from the User Manager
	 */
	private static void retrievePasswordParams() {
		
		// TODO Actually retrieve parameters from User Manager
		
		passwordMinLength = 10;
		passwordMinElements = 3;
		passwordSpecialChars = ",.-;:_!§$%&/()=?+#*<>";
	}
	
	/**
	 * Test the given password against the configured password strength parameters (length, element groups used)
	 * 
	 * @param password the password to test
	 * @return true, if the password conforms to the password rules, false otherwise
	 */
	public boolean isPasswordStrengthOk(String password) {
		boolean ok = true;
		
		// Check parameter
		if (null == password)
			return false;
		
		// Check whether strength parameters are already available
		if (null == passwordMinLength) {
			retrievePasswordParams();
		}
		
		// Check password length
		ok = ok && passwordMinLength <= password.length();
		
		// Check element groups
		int elementGroupCount = 0;
		if (password.matches(".*\\p{Upper}.*")) ++elementGroupCount;
		if (password.matches(".*\\p{Lower}.*")) ++elementGroupCount;
		if (password.matches(".*\\p{Digit}.*")) ++elementGroupCount;
		if (password.matches(".*[" + passwordSpecialChars + "].*")) ++elementGroupCount;
		ok = ok && passwordMinElements <= elementGroupCount;
		
		// Log and (in interactive mode) print error message, if strength check failed
		if (!ok) {
			String message = logger.log(UIMessage.PASSWORD_STRENGTH_INSUFFICIENT, 
					passwordMinLength, passwordMinElements, passwordSpecialChars);
			if (null != System.console()) System.err.println(message);
		}
		return ok;
	}
}
