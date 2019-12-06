/**
 * BackendConnectionService.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.backend;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AUTH;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	private static final int MSG_ID_SERIALIZATION_FAILED = 2807;
	private static final int MSG_ID_INVALID_URL = 2808;
	
	/* Message string constants */
	private static final String MSG_HTTP_REQUEST_FAILED = "(E%d) HTTP request failed (cause: %s)";
	private static final String MSG_SERVICE_REQUEST_FAILED = "(E%d) Service request failed with status %d (%s), cause: %s";
	private static final String MSG_NOT_AUTHORIZED = "(E%d) User %s not authorized for requested service";
	private static final String MSG_SERIALIZATION_FAILED = "(E%d) Cannot convert object to Json (cause: %s)";
	private static final String MSG_INVALID_URL = "(E%d) Invalid request URL %s (cause: %s)";

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
	 * @return the body of the HTTP response converted into an object of the expected class
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
			String message = String.format(MSG_HTTP_REQUEST_FAILED, MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
			logger.error(message, e);
			throw new RestClientException(message, e);
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
	 * @return the body of the HTTP response converted into an object of the expected class
	 * @throws RestClientException if an error (HTTP status code 4xx or 5xx) occurred in the communication to the service
	 * @throws RuntimeException if the service returned an HTTP status different from OK (200)
	 */
	public <T> T postToService(String serviceUrl, String requestPath, Object restObject, Class<T> clazz, String username, String password)
			throws RestClientException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> postToService({}, {}, object, user, password)", serviceUrl, requestPath);
		
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
			String message = String.format(MSG_HTTP_REQUEST_FAILED, MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
			logger.error(message, e);
			throw new RestClientException(message, e);
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
	
	/**
	 * Calls a prosEO service at the given location with HTTP Patch
	 * 
	 * @param serviceUrl the base URL of the service (protocol, hostname, port, base URI)
	 * @param requestPath the specific request path including request parameters
	 * @param restObject the object to patch to the service
	 * @param clazz the class of the expected result object
	 * @param username the username for basic HTTP authentication (optional)
	 * @param password the password for basic HTTP authentication (optional)
	 * @return the body of the HTTP response converted into an object of the expected class
	 * @throws RestClientException if an error (HTTP status code 4xx or 5xx) occurred in the communication to the service
	 * @throws RuntimeException if the service returned an HTTP status different from OK (200) or another unrecoverable error occurred
	 */
	public <T> T patchToService(String serviceUrl, String requestPath, Object restObject, Class<T> clazz, String username, String password)
			throws RestClientException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> patchToService({}, {}, object, user, password)", serviceUrl, requestPath);
		
		// Implementation does not rely on RestTemplate, as this seems to be buggy for PATCH!
		
		// Build an HTTP request
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
		HttpClient httpclient = HttpClientBuilder.create().build();
		HttpPatch req = new HttpPatch();
		try {
			req.setURI(new URI(serviceUrl + requestPath));
		} catch (URISyntaxException e) {
			String message = String.format(MSG_INVALID_URL, MSG_ID_INVALID_URL, serviceUrl + requestPath, e.getMessage());
			logger.error(message);
			throw new RuntimeException(message, e);
		}
		req.addHeader(new BasicHeader(AUTH.WWW_AUTH_RESP, AuthSchemes.BASIC + " " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes())));
		req.addHeader(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()));
		try {
			String jsonObject = mapper.writeValueAsString(restObject);
			if (logger.isTraceEnabled()) logger.trace("... serialized Json object: " + jsonObject);
			req.setEntity(new StringEntity(jsonObject));
		} catch (Exception e) {
			String message = String.format(MSG_SERIALIZATION_FAILED, MSG_ID_SERIALIZATION_FAILED, e.getMessage());
			logger.error(message);
			throw new RuntimeException(message, e);
		}
		// Execute the HTTP request
		try {
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with PATCH", serviceUrl + requestPath);
			HttpResponse response =  httpclient.execute(req);
			// Convert Json response to object of requested class
			return mapper.readValue(response.getEntity().getContent(), clazz);
		} catch (HttpResponseException e) {
			if (HttpStatus.UNAUTHORIZED.value() == e.getStatusCode()) {
				logger.error(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED, e.getMessage()), e);
				throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, e.getMessage());
			} else if (HttpStatus.NOT_FOUND.value() == e.getStatusCode()) {
				logger.error(String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED,
						e.getStatusCode(), e.getReasonPhrase(), "See service log"));
				throw new HttpClientErrorException(HttpStatus.NOT_FOUND, e.getMessage());
			} else {
				String message = String.format(MSG_HTTP_REQUEST_FAILED, MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
				logger.error(message, e);
				throw new RestClientException(message, e);
			}
		} catch (IOException e) {
			String message = String.format(MSG_HTTP_REQUEST_FAILED, MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
			logger.error(message, e);
			throw new RestClientException(message, e);
		}
	}
	
	/**
	 * Calls a prosEO service at the given location with HTTP DELETE
	 * 
	 * @param serviceUrl the base URL of the service (protocol, hostname, port, base URI)
	 * @param requestPath the specific request path including request parameters
	 * @param username the username for basic HTTP authentication (optional)
	 * @param password the password for basic HTTP authentication (optional)
	 * @return the body of the HTTP response converted into an object of the expected class
	 * @throws RestClientException if an error (HTTP status code 4xx or 5xx) occurred in the communication to the service
	 */
	public void deleteFromService(String serviceUrl, String requestPath, String username, String password) throws RestClientException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteFromService({}, {}, user, password)", serviceUrl, requestPath);
		
		// Attempt connection to service
		try {
			RestTemplate restTemplate = ( null == username ? rtb.build() : rtb.basicAuthentication(username, password).build() );
			String requestUrl = serviceUrl + requestPath;
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with DELETE", requestUrl);
			restTemplate.delete(requestUrl);
		} catch (HttpClientErrorException.NotFound e) {
			logger.error(String.format(MSG_SERVICE_REQUEST_FAILED, MSG_ID_SERVICE_REQUEST_FAILED,
					e.getStatusCode().value(), e.getStatusCode().toString(), e.getResponseHeaders().getFirst("Warning")));
			throw e;
		} catch (HttpClientErrorException.Unauthorized e) {
			logger.error(String.format(MSG_NOT_AUTHORIZED, MSG_ID_NOT_AUTHORIZED, e.getMessage()), e);
			throw e;
		} catch (RestClientException e) {
			String message = String.format(MSG_HTTP_REQUEST_FAILED, MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
			logger.error(message, e);
			throw new RestClientException(message, e);
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< deleteFromService()");
	}
	
}
