/**
 * OAuth2TokenController.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.api.odip.OAuth2TokenManager;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.OdipMessage;

/**
 * Spring MVC controller for requesting an access token for the prosEO ODIP API; implements the services required to provide a RESTful API
 * according to OAuth2 "Resource Owner Password Credentials Grant" flow and "Client Credentials Grant" flow as requested in
 * ESA's Production Interface Delivery Point (ODIP) API ICD (ESA-EOPG-EOPGC-IF-3, issue 1.5) and its associated 
 * "Technical Note for the Interface Delivery Points Access and Authentication" (issue 1.0).
 * 
 * @author Dr. Thomas Bassler
 */
@RestController
@Validated
@RequestMapping(value = "/proseo/odip/v1/token", produces = "application/json")
public class OAuth2TokenController {
	
	/* OAuth2 message handling */
	private static final String ERROR_INTERNAL_SERVER_ERROR = "internal_server_error";
	private static final String ERROR_UNAUTHORIZED_CLIENT = "unauthorized_client";
	private static final String ERROR_UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
	private static final String ERROR_INVALID_REQUEST = "invalid_request";
	private static final String ERROR_RESPONSE_FORMAT = "{ \"error\" : \"%s\", \"error_description\" : \"%s\" }";
	
	/* Message string constants */
	private static final String HTTP_MSG_PREFIX = "199 proseo-api-prip ";
	
	/** The OAuth2 token manager */
	@Autowired
	private OAuth2TokenManager tokenManager;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OAuth2TokenController.class);
	
	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HttpHeaders.WARNING, HTTP_MSG_PREFIX + (null == message ? "null" : message.replaceAll("\n", " ")));
		return responseHeaders;
	}
	
    /**
     * Create an authentication token and a refresh token for the user identified in the Basic Auth header
     * 
     * @param grantType Type of grant requested (value must be set to "password" for "Resource Owner Password Credentials Grant" flow
     *    or to "client_credentials" for "Client Credentials Grant" flow as per RFC 6749; REQUIRED)
     * @param username The ODIP username (as per RFC 6749; REQUIRED for "Resource Owner Password Credentials Grant" flow, must not be set otherwise)
     * @param password The ODIP password (as per RFC 6749; REQUIRED for "Resource Owner Password Credentials Grant" flow, must not be set otherwise)
     * @param scope The scope of the access request (as per RFC 6749; OPTIONAL, will be ignored if set)
     * @param httpHeaders the HTTP request headers
     * @return HTTP status OK and an OAuth2 token grant response, or
     *         HTTP status BAD_REQUEST and an OAuth2 error response, if any failure occurred (additionally the Warning header is set), or
     *         HTTP status INTERNAL_SERVER_ERROR and an OAuth2 error response, if any other unrecoverable failure occurred
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
	public ResponseEntity<String> getToken(
	        @RequestParam(name = "grant_type") String grantType,
	        @RequestParam(required = false) String username,
	        @RequestParam(required = false) String password,
	        @RequestParam(required = false) String scope,
	        @RequestHeader HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getToken({}, {}, ********, {}, HttpHeaders)", grantType, username, scope);
		
		ObjectMapper om = new ObjectMapper();
		
		try {
			String responseBody = om.writeValueAsString(tokenManager.getToken(grantType, username, password, httpHeaders));
			
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HttpHeaders.CACHE_CONTROL, "no-store");
			responseHeaders.set(HttpHeaders.PRAGMA, "no-cache");
			
			return ResponseEntity.status(HttpStatus.OK).headers(responseHeaders).body(responseBody);
		} catch (JsonProcessingException e) {
			String errorString = String.format(ERROR_RESPONSE_FORMAT, ERROR_INTERNAL_SERVER_ERROR, e.getMessage());
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(errorHeaders(e.getMessage())).body(errorString);
		} catch (Exception e) {
			logger.log(OdipMessage.MSG_EXCEPTION, e.getMessage(), e);
			
			String errorCode = null;
			HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
			if (e instanceof IllegalArgumentException) {
				errorCode = ERROR_INVALID_REQUEST;
			} else if (e instanceof UnsupportedOperationException) {
				errorCode = ERROR_UNSUPPORTED_GRANT_TYPE;
			} else if (e instanceof SecurityException) {
				errorCode = ERROR_UNAUTHORIZED_CLIENT;
			} else {
				httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
				errorCode = ERROR_INTERNAL_SERVER_ERROR;
			}
			String errorString = String.format(ERROR_RESPONSE_FORMAT, errorCode, e.getMessage());
						
			return ResponseEntity.status(httpStatus).headers(errorHeaders(e.getMessage())).body(errorString);
		}
	}

}
