/**
 * ProductionInterfaceSecurity.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.api.prip.odata.ProductEntityProcessor;
import de.dlr.proseo.model.enums.UserRole;

/**
 * Security utility methods for PRIP API
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class ProductionInterfaceSecurity {

	// Message IDs
	private static final int MSG_ID_HTTP_REQUEST_FAILED = 5003;
	private static final int MSG_ID_AUTH_MISSING_OR_INVALID = 5006;
	private static final int MSG_ID_NOT_AUTHORIZED_FOR_MISSION = 5008;
	
	// Message strings
	private static final String MSG_HTTP_REQUEST_FAILED = "(E%d) HTTP request failed (cause: %s)";
	private static final String MSG_AUTH_MISSING_OR_INVALID = "(E%d) Basic authentication missing or invalid: %s";
	private static final String MSG_NOT_AUTHORIZED_FOR_MISSION = "(E%d) User %s not authorized for PRIP API in mission %s";
	
	/** The logged in user (as used for authentication, i. e. including mission prefix) */
	private ThreadLocal<String> username = new ThreadLocal<>();
	/** The user's password */
	private ThreadLocal<String> password = new ThreadLocal<>();
	/** The mission to which the user has logged in */
	private ThreadLocal<String> mission = new ThreadLocal<>();
	/** The authorities granted to the user */
	private ThreadLocal<List<String>> authorities = new ThreadLocal<>();

	/** REST template builder */
	@Autowired
	private RestTemplateBuilder rtb;
	
	/** The configuration for the PRIP API */
	@Autowired
	private ProductionInterfaceConfiguration config;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductEntityProcessor.class);

	/**
	 * Create and log a formatted message at the given level
	 * 
	 * @param level the logging level to use
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private String log(Level level, String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		if (Level.ERROR.equals(level)) {
			logger.error(message);
		} else if (Level.WARN.equals(level)) {
			logger.warn(message);
		} else {
			logger.info(message);
		}

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
		return log(Level.ERROR, messageFormat, messageId, messageParameters);
	}

	/**
	 * Parse an HTTP authentication header into mission, username and password and set the respective thread-local attributes
	 * 
	 * @param authHeader the authentication header to parse, expected format: "Basic base64(mission&bsol;username:password)"
	 * @throws IllegalArgumentException if the authentication header cannot be parsed into the three parts expected
	 */
	public void parseAuthenticationHeader(String authHeader) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> parseAuthenticationHeader({})", authHeader);

		if (null == authHeader) {
			String message = logError(MSG_AUTH_MISSING_OR_INVALID, MSG_ID_AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException(message);
		}
		String[] authParts = authHeader.split(" ");
		if (2 != authParts.length || !"Basic".equals(authParts[0])) {
			String message = logError(MSG_AUTH_MISSING_OR_INVALID, MSG_ID_AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException(message);
		}
		String[] missionUserPassword = (new String(Base64.getDecoder().decode(authParts[1]))).split("\\\\"); // --> regex "\\" --> matches "\"
		if (2 != missionUserPassword.length) {
			String message = logError(MSG_AUTH_MISSING_OR_INVALID, MSG_ID_AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException(message);
		}
		String[] userPassword = missionUserPassword[1].split(":"); // guaranteed to work as per BasicAuth specification
		
		mission.set(missionUserPassword[0]);
		username.set(userPassword[0]);
		password.set(userPassword[1]);
	}
	
	/**
	 * Log in to prosEO
	 * 
	 * @param request the HTTP request containing the authentication header
	 * @return true, if the login was successful, false otherwise
	 * @throws SecurityException if the login failed for any reason
	 */
	public void doLogin(HttpServletRequest request) throws SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> doLogin(HttpServletRequest)");
		
		// Reset all authentication attributes for this thread
		username.remove();
		password.remove();
		mission.remove();
		authorities.set(new ArrayList<String>());
		
		// Analyse authentication header
		try {
			parseAuthenticationHeader(request.getHeader(HttpHeaders.AUTHORIZATION));
		} catch (IllegalArgumentException e) {
			throw new SecurityException(e.getMessage());
		}

		// Attempt connection to User Manager
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = null;
		try {
			RestTemplate restTemplate = rtb.basicAuthentication(mission.get() + "-" + username.get(), password.get()).build();
			String requestUrl = config.getUserMgrUrl() + "/login?mission=" + mission.get();
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with GET", requestUrl);
			entity = restTemplate.getForEntity(requestUrl, List.class);
		} catch (HttpClientErrorException.Unauthorized e) {
			String message = String.format(MSG_NOT_AUTHORIZED_FOR_MISSION, MSG_ID_NOT_AUTHORIZED_FOR_MISSION, username.get(), mission.get());
			logger.error(message);
			throw new SecurityException(message);
		} catch (Exception e) {
			String message = String.format(MSG_HTTP_REQUEST_FAILED, MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
			logger.error(message, e);
			throw new SecurityException(message);
		}

		for (Object authority: entity.getBody()) {
			if (authority instanceof String) {
				authorities.get().add((String) authority);
			}
		}
		
		// Check whether user is authorized to use the PRIP API
		if (!hasRole(UserRole.PRIP_USER)) {
			String message = String.format(MSG_NOT_AUTHORIZED_FOR_MISSION, MSG_ID_NOT_AUTHORIZED_FOR_MISSION, username.get(), mission.get());
			logger.error(message);
			throw new SecurityException(message);
		}
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
	
	/**
	 * Checks whether the logged in user has the given role
	 * 
	 * @return true, if the respective authority was granted, false otherwise
	 */
	public boolean hasRole(UserRole role) {
		return authorities.get().contains(role.asRoleString());
	}
}
