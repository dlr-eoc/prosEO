/**
 * SpringAuthenticationFilter.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Custom authentication filter that handles authentication based on the provided username, password, and mission values in an HTTP
 * request. Extends the UsernamePasswordAuthenticationFilter from the Spring Security framework.
 *
 * @author Ernst Melchinger
 */
public class SpringAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

	/** The name of the mission parameter in the request */
	private String missionParameter = "mission";

	/** Flag indicating if only POST requests are allowed for authentication */
	private boolean postOnly = true;

	/**
	 * Performs authentication by checking the request method and creating an Authentication object with the username, password, and
	 * mission. Then, it delegates the authentication to the configured AuthenticationManager and returns the authenticated
	 * Authentication object.
	 *
	 * @param request  the HTTP request
	 * @param response the HTTP response
	 * @return the authenticated Authentication object
	 * @throws AuthenticationException if an authentication error occurs
	 */
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {

		if (postOnly && !request.getMethod().equals("POST")) {
			throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
		}

		UsernamePasswordAuthenticationToken authRequest = getAuthRequest(request);
		setDetails(request, authRequest);

		return this.getAuthenticationManager().authenticate(authRequest);
	}

	/**
	 * Creates the UsernamePasswordAuthenticationToken object with the username, password, and mission retrieved from the request.
	 * The username is formatted as "mission/username".
	 *
	 * @param request the HTTP request
	 * @return the UsernamePasswordAuthenticationToken object
	 */
	private UsernamePasswordAuthenticationToken getAuthRequest(HttpServletRequest request) {

		String username = obtainUsername(request);
		String password = obtainPassword(request);
		String mission = request.getParameter(missionParameter);
		String usernameDomain = String.format("%s%s%s", mission, "/", username.trim());

		return new UsernamePasswordAuthenticationToken(usernameDomain, password);
	}

	/**
	 * Determines if authentication is required for the given request. It checks if the request method is POST and delegates to the
	 * parent class's requiresAuthentication method for additional checks.
	 *
	 * @param request  the HTTP request
	 * @param response the HTTP response
	 * @return true if authentication is required, false otherwise
	 */
	@Override
	protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
		return (request.getMethod().equals("POST") && super.requiresAuthentication(request, response));
	}
}
