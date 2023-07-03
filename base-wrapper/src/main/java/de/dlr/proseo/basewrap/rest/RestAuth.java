/**
 * RestAuth.java
 *
 *  (C) 2019 Hubert Asamer, DLR
 *  (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.basewrap.rest;

import java.io.IOException;
import java.util.Base64;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Provide REST authentication.
 * 
 * @author Hubert Asamer
 */
public class RestAuth implements ClientRequestFilter {

	private final String user;
	private final String password;

	public RestAuth(String user, String password) {
		this.user = user;
		this.password = password;
	}

	/**
	 * Adds an HTTP Authorization header to the given client request context
	 *
	 * @param requestContext the client request context
	 * @throws IOException if Base64 encoding of the authorization header text fails
	 */
	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {

		// Encode username and password in Base64
		String basicAuthentication = this.user + ":" + this.password;
		basicAuthentication = "Basic " + Base64.getEncoder().encodeToString(basicAuthentication.getBytes());

		// Add the authorization string to the request headers
		MultivaluedMap<String, Object> headers = requestContext.getHeaders();
		headers.add("Authorization", basicAuthentication);
	}
}