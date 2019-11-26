/**
 * BackendConnectionService.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.backend;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Service class to connect to the prosEO backend services from the user interface
 * 
 * @author Dr. Thomas Bassler
 */
@Service
public class BackendConnectionService {
	
	/* Message ID constants */
	private static final int MSG_ID_HTTP_REQUEST_FAILED = 2804;
	private static final int MSG_ID_SERVICE_REQUEST_FAILED = 2805;
	private static final int MSG_ID_NOT_AUTHORIZED = 2806;
	
	/* Message string constants */
	private static final String MSG_HTTP_REQUEST_FAILED = "(E%d) HTTP request failed (cause: %s)";
	private static final String MSG_SERVICE_REQUEST_FAILED = "(E%d) Service request failed with status %d (%s), cause: %s";
	private static final String MSG_NOT_AUTHORIZED = "(E%d) User %s not authorized for requested service";

	/** The configuration object for the prosEO User Interface */
	@Autowired
	private BackendConfiguration backendConfig;
	
	/** REST template builder */
	@Autowired
	private RestTemplateBuilder rtb;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(BackendConnectionService.class);
	
	/**
	 * Calls a prosEO service at the given location with HTTP GET
	 * 
	 * @param serviceUrl the base URL of the service (protocol, hostname, port, base URI)
	 * @param requestPath the specific request path including request parameters
	 * @param username the username for basic HTTP authentication (optional)
	 * @param password the password for basic HTTP authentication (optional)
	 * @return the body of the HTTP response
	 * @throws RestClientException if an error (HTTP status code 4xx or 5xx) occurred in the communication to the service
	 * @throws RuntimeException if the service returned an HTTP status different from OK (200)
	 */
	public <T> T getFromService(String serviceUrl, String requestPath, Class<T> clazz, String username, String password) throws RestClientException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> getFromService({}, {})", serviceUrl, requestPath);
		
		// Attempt connection to service
		ResponseEntity<T> entity = null;
		try {
			RestTemplate restTemplate = ( null == username ? rtb.build() : rtb.basicAuthentication(username, password).build() );
			String requestUrl = serviceUrl + requestPath;
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with GET", requestUrl);
			entity = restTemplate.getForEntity(requestUrl, clazz);
		} catch (HttpClientErrorException.Unauthorized e) {
			logger.error(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED, e.getMessage()), e);
			throw e;
		} catch (RestClientException e) {
			logger.error(String.format(MSG_HTTP_REQUEST_FAILED, MSG_ID_HTTP_REQUEST_FAILED, e.getMessage()), e);
			throw e;
		}
		
		// All GET requests should return HTTP status OK
		if (!HttpStatus.OK.equals(entity.getStatusCode())) {
			String message = String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED, 
					entity.getStatusCodeValue(), entity.getStatusCode().toString(), entity.getHeaders().getFirst("Warning"));
			logger.error(message);
			throw new RuntimeException(message);
		}
		
		// Check connection result
		if (logger.isTraceEnabled()) logger.trace("<<< getFromService()");
		return entity.getBody();
	}
	
	/**
	 * Calls a prosEO service at the given location with HTTP Post
	 * 
	 * @param serviceUrl the base URL of the service (protocol, hostname, port, base URI)
	 * @param requestPath the specific request path including request parameters
	 * @param restObject the object to post to the service
	 * @param clazz the class of the expected result object
	 * @param username the username for basic HTTP authentication (optional)
	 * @param password the password for basic HTTP authentication (optional)
	 * @return the body of the HTTP response
	 * @throws RestClientException if an error (HTTP status code 4xx or 5xx) occurred in the communication to the service
	 * @throws RuntimeException if the service returned an HTTP status different from OK (200)
	 */
	public <T> T postToService(String serviceUrl, String requestPath, Object restObject, Class<T> clazz, String username, String password)
			throws RestClientException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> getFromService({}, {})", serviceUrl, requestPath);
		
		// Attempt connection to service
		ResponseEntity<T> entity = null;
		try {
			RestTemplate restTemplate = ( null == username ? rtb.build() : rtb.basicAuthentication(username, password).build() );
			String requestUrl = serviceUrl + requestPath;
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with POST", requestUrl);
			entity = restTemplate.postForEntity(requestUrl, restObject, clazz);
		} catch (HttpClientErrorException.NotFound e) {
			logger.error(String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED,
					e.getStatusCode().value(), e.getStatusCode().toString(), e.getResponseHeaders().getFirst("Warning")));
			throw e;
		} catch (HttpClientErrorException.Unauthorized e) {
			logger.error(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED, e.getMessage()), e);
			throw e;
		} catch (RestClientException e) {
			logger.error(String.format(MSG_HTTP_REQUEST_FAILED, MSG_ID_HTTP_REQUEST_FAILED, e.getMessage()), e);
			throw e;
		}
		
		// All POST requests should return HTTP status CREATED
		if (!HttpStatus.CREATED.equals(entity.getStatusCode())) {
			String message = String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED, 
					entity.getStatusCodeValue(), entity.getStatusCode().toString(), entity.getHeaders().getFirst("Warning"));
			logger.error(message);
			throw new RuntimeException(message);
		}
		
		// Check connection result
		if (logger.isTraceEnabled()) logger.trace("<<< getFromService()");
		return entity.getBody();
	}
	
}
