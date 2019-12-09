/**
 * BackendUserManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Management of user login and logout (thread safe)
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class BackendUserManager {

	/* Message ID constants */
	private static final int MSG_ID_HTTP_CONNECTION_FAILURE = 2820;
	private static final int MSG_ID_LOGGED_IN = 2823;
	private static final int MSG_ID_LOGIN_FAILED = 2824;
	private static final int MSG_ID_LOGGED_OUT = 2825;
	private static final int MSG_ID_LOGIN_CANCELLED = 2826;
	private static final int MSG_ID_MISSION_NOT_FOUND = 2827;
	private static final int MSG_ID_NOT_AUTHORIZED = 2828;

	/* Message string constants */
	private static final String MSG_HTTP_CONNECTION_FAILURE = "(E%d) HTTP connection failure (cause: %s)";
	private static final String MSG_MISSION_NOT_FOUND = "(E%d) Mission %s not found";
	private static final String MSG_LOGIN_FAILED = "(E%d) Login for user %s failed";
	private static final String MSG_NOT_AUTHORIZED = "(E%d) User %s not authorized for mission %s";

	private static final String MSG_LOGGED_IN = "(I%d) User %s logged in";
	private static final String MSG_LOGGED_OUT = "(I%d) User %s logged out";
	private static final String MSG_LOGIN_CANCELLED = "(I%d) No username given, login cancelled";

	/* Other string constants */
	private static final String PROSEO_USERNAME_PROMPT = "Username: ";
	private static final String PROSEO_PASSWORD_PROMPT = "Password for user %s: ";
	
	/** The logged in user */
	private ThreadLocal<String> username = new ThreadLocal<>();
	/** The user's password */
	private ThreadLocal<String> password = new ThreadLocal<>();
	/** The mission to which the user has logged in */
	private ThreadLocal<String> mission = new ThreadLocal<>();

	/** The configuration object for the prosEO User Interface */
	@Autowired
	private BackendConfiguration backendConfig;
	
	/** The connector service to the prosEO backend services prosEO */
	@Autowired
	private BackendConnectionService backendConnector;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(BackendUserManager.class);
	
	/**
	 * Test whether the given user can connect to the Processor Manager with the given password
	 * 
	 * @param username the username to login with
	 * @param password the password to login with
	 * @param mission the mission to log in to
	 * @return true, if the connection can be established, false otherwise
	 */
	private boolean testLogin(String username, String password, String mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> login({}, ********, {})", username, mission);
		
		// TODO Create a real login to a User Manager service!

		// Attempt connection to Processor Manager (as a substitute)
		try {
			backendConnector.getFromService(backendConfig.getProcessorManagerUrl(), "/processorclasses?mission=" + mission, Object.class, username, password);
		} catch (HttpClientErrorException.NotFound e) {
			String message = String.format(MSG_MISSION_NOT_FOUND, MSG_ID_MISSION_NOT_FOUND, mission);
			logger.error(message);
			System.err.println(message);
			return false;
		} catch (HttpClientErrorException.Unauthorized e) {
			String message = String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED, mission);
			logger.error(message);
			System.err.println(message);
			return false;
		} catch (RestClientException e) {
			String message = String.format(MSG_HTTP_CONNECTION_FAILURE, MSG_ID_HTTP_CONNECTION_FAILURE, e.getMessage());
			logger.error(message, e);
			System.err.println(message);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Log in to prosEO
	 * 
	 * @param username the user name for login (optional, will be requested from standard input, if not set)
	 * @param password the password for login (optional, will be requested from standard input, if not set)
	 * @param mission the mission to log in to
	 * @return true, if the login was successful, false otherwise
	 */
	public boolean doLogin(String username, String password, String mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> doLogin({}, ********, {})", username, mission);
		
		// Ask for username, if not set
		if (null == username || "".equals(username)) {
			System.out.print(PROSEO_USERNAME_PROMPT);
			username = new String(System.console().readLine());
		}
		if (null == username || "".equals(username)) {
			System.err.println(String.format(MSG_LOGIN_CANCELLED, MSG_ID_LOGIN_CANCELLED));
		}
		
		// Ask for password, if not set
		if (null == password || "".equals(password)) {
			System.out.print(String.format(PROSEO_PASSWORD_PROMPT, username));
			password = new String(System.console().readPassword());
		}
		
		// Test connection to some backend service
		if (testLogin(username, password, mission)) {
			// Record login information
			this.username.set(username);
			this.password.set(password);
			this.mission.set(mission);
			// Report success
			String message = String.format(MSG_LOGGED_IN, MSG_ID_LOGGED_IN, username);
			logger.info(message);
			System.out.println(message);
			if (logger.isTraceEnabled()) logger.trace("<<< doLogin()");
			return true;
		} else {
			// Report failure
			String message = String.format(MSG_LOGIN_FAILED, MSG_ID_LOGIN_FAILED, username);
			logger.error(message);
			System.err.println(message);
			if (logger.isTraceEnabled()) logger.trace("<<< doLogin()");
			return false;
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
		
		String message = String.format(MSG_LOGGED_OUT, MSG_ID_LOGGED_OUT, oldUser);
		logger.info(message);
		System.out.println(message);
		if (logger.isTraceEnabled()) logger.trace("<<< doLogout()");
	}
	
	/**
	 * Gets the name of the logged in user
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
}
