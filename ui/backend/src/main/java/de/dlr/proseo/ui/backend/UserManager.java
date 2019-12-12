/**
 * UserManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.backend;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Management of user login and logout (thread safe)
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class UserManager {

	/* General string constants */
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
	private ServiceConfiguration backendConfig;
	
	/** The connector service to the prosEO backend services prosEO */
	@Autowired
	private ServiceConnection backendConnector;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(UserManager.class);
	
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
		} catch (RestClientResponseException e) {
			if (logger.isTraceEnabled()) logger.trace("Caught HttpClientErrorException " + e.getMessage());
			String message = null;
			switch (e.getRawStatusCode()) {
			case org.apache.http.HttpStatus.SC_NOT_FOUND:
				message = uiMsg(MSG_ID_MISSION_NOT_FOUND, mission);
				break;
			case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
				message = uiMsg(MSG_ID_NOT_AUTHORIZED_FOR_MISSION, username, mission);
				break;
			default:
				message = uiMsg(MSG_ID_EXCEPTION, e.getMessage());
			}
			System.err.println(message);
			return false;
		} catch (RuntimeException e) {
			System.err.println(uiMsg(MSG_ID_HTTP_CONNECTION_FAILURE, e.getMessage()));
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
			System.err.println(uiMsg(MSG_ID_LOGIN_CANCELLED));
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
			String message = uiMsg(MSG_ID_LOGGED_IN, username);
			logger.info(message);
			System.out.println(message);
			if (logger.isTraceEnabled()) logger.trace("<<< doLogin()");
			return true;
		} else {
			// Report failure
			String message = uiMsg(MSG_ID_LOGIN_FAILED, username);
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
		
		String message = uiMsg(MSG_ID_LOGGED_OUT, oldUser);
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
