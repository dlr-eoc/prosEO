/**
 * LoginManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.backend;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

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
	
	/** The logged in user (as used for authentication, i. e. including mission prefix) */
	private ThreadLocal<String> username = new ThreadLocal<>();
	/** The user's password */
	private ThreadLocal<String> password = new ThreadLocal<>();
	/** The mission to which the user has logged in */
	private ThreadLocal<String> mission = new ThreadLocal<>();
	/** The authorities granted to the user */
	private ThreadLocal<List<String>> authorities = new ThreadLocal<>();

	/** The configuration object for the prosEO User Interface */
	@Autowired
	private ServiceConfiguration backendConfig;
	
	/** The connector service to the prosEO backend services prosEO */
	@Autowired
	private ServiceConnection backendConnector;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(LoginManager.class);
	
	/**
	 * Login to prosEO for the given mission with the given username and password
	 * 
	 * @param username the username to login with (including mission prefix)
	 * @param password the password to login with
	 * @param mission the mission to log in to (may be null for user with prosEO Administrator privileges)
	 * @return a list of strings denoting authorities granted to the user for the given mission (may be empty)
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
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED_FOR_MISSION, username, mission);
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_HTTP_CONNECTION_FAILURE, e.getMessage()));
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
			String message = uiMsg(MSG_ID_LOGIN_FAILED, username);
			logger.error(message);
			System.err.println(message);
			if (logger.isTraceEnabled()) logger.trace("<<< doLogin()");
			return false;
		}
		
		// Ask for username, if not set
		if (null == username || "".equals(username)) {
			System.out.print(PROSEO_USERNAME_PROMPT);
			username = new String(System.console().readLine());
		}
		if (null == username || "".equals(username)) {
			System.err.println(uiMsg(MSG_ID_LOGIN_CANCELLED));
			return false;
		}
		
		// Ask for password, if not set
		if (null == password || "".equals(password)) {
			System.out.print(String.format(PROSEO_PASSWORD_PROMPT, username));
			password = new String(System.console().readPassword());
		}
		
		// Login to prosEO via the User Manager Service
		String missionUsername = (null == mission ? "" : mission + "-") + username;
		List<String> grantedAuthorities = login(missionUsername, password, mission);
		if (grantedAuthorities.isEmpty()) {
			// Report failure
			String message = uiMsg(MSG_ID_LOGIN_FAILED, username);
			logger.error(message);
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
			String message = uiMsg(MSG_ID_LOGGED_IN, username);
			logger.info(message);
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
		
		String message = uiMsg(MSG_ID_LOGGED_OUT, oldUser);
		logger.info(message);
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
	 * Gets the authorities granted to the user after login
	 * 
	 * @return the granted authorities
	 */
	public List<String> getAuthorities() {
		return authorities.get();
	}
}
