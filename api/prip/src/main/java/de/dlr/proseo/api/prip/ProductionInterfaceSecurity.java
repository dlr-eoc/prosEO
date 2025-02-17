/**
 * ProductionInterfaceSecurity.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.api.prip.OAuth2TokenManager.UserInfo;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.PripMessage;
import de.dlr.proseo.model.enums.UserRole;

/**
 * Security utility methods for PRIP API
 *
 * @author Dr. Thomas Bassler
 */
@Component
public class ProductionInterfaceSecurity {

	private static final String AUTH_TYPE_BASIC = "Basic";
	private static final String AUTH_TYPE_BEARER = "Bearer";

	/** Information about the current user in this thread */
	private ThreadLocal<UserInfo> userInfo = new ThreadLocal<>();

	/** REST template builder */
	@Autowired
	private RestTemplateBuilder rtb;

	/** The configuration for the PRIP API */
	@Autowired
	private ProductionInterfaceConfiguration config;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductionInterfaceSecurity.class);

	/**
	 * Parse an HTTP authentication header into mission, username and password and set the respective thread-local attributes
	 *
	 * @param authHeader the authentication header to parse, expected format: "Basic base64(mission&#92;username:password)"
	 * @throws IllegalArgumentException if the authentication header cannot be parsed into the three parts expected
	 * @return the parsed UserInfo
	 */
	/* package */ UserInfo parseAuthenticationHeader(String authHeader) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> parseAuthenticationHeader({})", authHeader);

		if (null == authHeader) {
			String message = logger.log(PripMessage.MSG_AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException(message);
		}
		String[] authParts = authHeader.split(" ");
		if (2 != authParts.length || !AUTH_TYPE_BASIC.equals(authParts[0])) {
			String message = logger.log(PripMessage.MSG_AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException(message);
		}
		String[] missionUserPassword = (new String(Base64.getDecoder().decode(authParts[1]))).split("\\\\"); // --> regex "\\" -->
																												// matches "\"
		if (2 != missionUserPassword.length) {
			String message = logger.log(PripMessage.MSG_AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException(message);
		}
		// The following is guaranteed to work as per BasicAuth specification (but split limited, because password may contain ':')
		String[] userPassword = missionUserPassword[1].split(":", 2);

		return new UserInfo(missionUserPassword[0], userPassword[0], userPassword[1], new ArrayList<>());
	}

	/**
	 * Log in to prosEO
	 *
	 * @param request the HTTP request containing the authentication header
	 * @param tokenManager the token manager to use
	 * @throws SecurityException if the login failed for any reason
	 */
	public void doLogin(HttpServletRequest request, OAuth2TokenManager tokenManager) throws SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> doLogin(HttpServletRequest)");

		// Reset all authentication attributes for this thread
		userInfo.remove();

		// Check authentication type
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (null == authHeader) {
			String message = logger.log(PripMessage.MSG_AUTH_MISSING_OR_INVALID, authHeader);
			throw new SecurityException(message);
		}

		// Delegate Bearer token authentication to OAuth2 Token Manager
		if (authHeader.startsWith(AUTH_TYPE_BEARER + " ")) {
			userInfo.set(tokenManager.getUserInfoFromToken(authHeader.substring((AUTH_TYPE_BEARER + " ").length())));
			return;
		}

		// Analyse authentication header
		UserInfo tmpUserInfo = null;
		try {
			tmpUserInfo = parseAuthenticationHeader(authHeader);
		} catch (IllegalArgumentException e) {
			throw new SecurityException(e.getMessage());
		}

		userInfo.set(authenticateUser(tmpUserInfo));
	}

	/**
	 * Check user credentials and authorities with User Manager
	 *
	 * @param userInfo a UserInfo object containing the user credentials
	 * @return the updated UserInfo object including authorities, if the authentication was successful and PRIP access is granted
	 * @throws SecurityException if the user could not be authenticated or is not authorized to access the PRIP API
	 */
	/* package */ UserInfo authenticateUser(UserInfo userInfo) throws SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> authenticateUser(userInfo)");

		// Attempt connection to User Manager
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = null;
		try {
			RestTemplate restTemplate = rtb.basicAuthentication(userInfo.missionCode + "-" + userInfo.username, userInfo.password)
				.build();
			String requestUrl = config.getUserMgrUrl() + "/login?mission=" + userInfo.missionCode;
			if (logger.isTraceEnabled())
				logger.trace("... calling service URL {} with GET", requestUrl);
			entity = restTemplate.getForEntity(requestUrl, List.class);
		} catch (HttpClientErrorException.Unauthorized e) {
			String message = logger.log(PripMessage.MSG_NOT_AUTHORIZED_FOR_PRIP, userInfo.missionCode, userInfo.username);
			throw new SecurityException(message);
		} catch (Exception e) {
			String message = logger.log(PripMessage.MSG_HTTP_REQUEST_FAILED, e.getMessage());
			throw new SecurityException(message);
		}
		if (logger.isTraceEnabled())
			logger.trace("... Authentication succeeded for user " + userInfo.missionCode + "\\" + userInfo.username);

		for (Object authority : entity.getBody()) {
			if (authority instanceof String) {
				if (logger.isTraceEnabled())
					logger.trace("... Adding authority " + authority);
				userInfo.authorities.add((String) authority);
			}
		}

		// Check whether user is authorized to use the PRIP API
		if (!userInfo.authorities.contains(UserRole.PRIP_USER.asRoleString())) {
			String message = logger.log(PripMessage.MSG_NOT_AUTHORIZED_FOR_PRIP, userInfo.missionCode, userInfo.username);
			throw new SecurityException(message);
		}

		return userInfo;
	}

	/**
	 * Gets the name of the logged in user for service authentication (including mission prefix)
	 *
	 * @return the user name or null, if no user is logged in
	 */
	public String getUser() {
		return userInfo.get().username;
	}

	/**
	 * Gets the password of the logged in user
	 *
	 * @return the password or null, if no user is logged in
	 */
	public String getPassword() {
		return userInfo.get().password;
	}

	/**
	 * Gets the code of the mission the current user is logged in to
	 *
	 * @return the mission code or null, if no user is logged in
	 */
	public String getMission() {
		return userInfo.get().missionCode;
	}

	/**
	 * Gets the authorities granted to the user after login
	 *
	 * @return the granted authorities
	 */
	public List<String> getAuthorities() {
		return userInfo.get().authorities;
	}

	/**
	 * Checks whether the logged in user has the given role
	 *
	 * @param role the user role to check
	 * @return true, if the respective authority was granted, false otherwise
	 */
	public boolean hasRole(UserRole role) {
		if (logger.isTraceEnabled())
			logger.trace("... Checking authorities " + userInfo.get().authorities + " for authority " + role.asRoleString());
		return userInfo.get().authorities.contains(role.asRoleString());
	}

}