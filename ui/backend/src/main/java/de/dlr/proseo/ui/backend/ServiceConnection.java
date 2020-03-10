/**
 * ServiceConnection.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.backend;

import static de.dlr.proseo.ui.backend.UIMessages.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.auth.AUTH;
import org.apache.http.client.HttpClient;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service class to connect to the prosEO backend services from the user interface
 * 
 * @author Dr. Thomas Bassler
 */
@Service
public class ServiceConnection {
	
	/* Message ID constants */
	
	/* Message string constants */

	/** REST template builder */
	@Autowired
	private RestTemplateBuilder rtb;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ServiceConnection.class);
	
	/**
	 * Calls a prosEO service at the given location with HTTP GET
	 * 
	 * @param <T> the class of the REST object to return
	 * @param serviceUrl the base URL of the service (protocol, hostname, port, base URI)
	 * @param requestPath the specific request path including request parameters
	 * @param clazz the class of the return object
	 * @param username the username for basic HTTP authentication (optional)
	 * @param password the password for basic HTTP authentication (optional)
	 * @return the body of the HTTP response converted into an object of the expected class
	 * @throws RestClientException if an error (HTTP status code 4xx or 5xx) occurred in the communication to the service
	 * @throws RuntimeException if the service returned an HTTP status different from OK (200)
	 */
	public <T> T getFromService(String serviceUrl, String requestPath, Class<T> clazz, String username, String password) throws RestClientException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> getFromService({}, {}, {}, user, password)", serviceUrl, requestPath, clazz);
		
		// Attempt connection to service
		ResponseEntity<T> entity = null;
		try {
			RestTemplate restTemplate = ( null == username ? rtb.build() : rtb.basicAuthentication(username, password).build() );
			String requestUrl = serviceUrl + requestPath;
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with GET", requestUrl);
			entity = restTemplate.getForEntity(requestUrl, clazz);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			logger.error(uiMsg(MSG_ID_SERVICE_REQUEST_FAILED,
					e.getStatusCode().value(), e.getStatusCode().toString(), e.getResponseHeaders().getFirst("Warning")));
			throw new HttpClientErrorException(e.getStatusCode(), e.getResponseHeaders().getFirst("Warning"));
		} catch (HttpClientErrorException.Unauthorized e) {
			logger.error(uiMsg(MSG_ID_NOT_AUTHORIZED_FOR_SERVICE, e.getMessage()), e);
			throw e;
		} catch (RestClientException e) {
			String message = uiMsg(MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
			logger.error(message, e);
			throw new RestClientException(message, e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		
		// All GET requests should return HTTP status OK
		if (!HttpStatus.OK.equals(entity.getStatusCode())) {
			String message = uiMsg(MSG_ID_SERVICE_REQUEST_FAILED, 
					entity.getStatusCodeValue(), entity.getStatusCode().toString(), entity.getHeaders().getFirst("Warning"));
			logger.error(message);
			throw new RuntimeException(message);
		}
		
		// Check connection result
		if (logger.isTraceEnabled()) logger.trace("<<< getFromService()");
		return entity.getBody();
	}
	
	/**
	 * Calls a prosEO service at the given location with HTTP PUT
	 * 
	 * @param <T> the class of the REST object to use as PUT data
	 * @param serviceUrl the base URL of the service (protocol, hostname, port, base URI)
	 * @param requestPath the specific request path including request parameters
	 * @param clazz the class of the return object
	 * @param username the username for basic HTTP authentication (optional)
	 * @param password the password for basic HTTP authentication (optional)
	 * @return the body of the HTTP response converted into an object of the expected class
	 * @throws RestClientException if an error (HTTP status code 4xx or 5xx) occurred in the communication to the service
	 * @throws RuntimeException if the service returned an HTTP status different from OK (200)
	 */
	public <T> T putToService(String serviceUrl, String requestPath, Class<T> clazz, String username, String password) throws RestClientException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> putToService({}, {}, {}, user, password)", serviceUrl, requestPath, clazz);
		
		// Attempt connection to service
		ResponseEntity<T> entity = null;
		try {
			RestTemplate restTemplate = ( null == username ? rtb.build() : rtb.basicAuthentication(username, password).build() );
			RequestEntity<Void> requestEntity = RequestEntity.put(new URI(serviceUrl + requestPath)).build();
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with GET", requestEntity.getUrl());
			entity = restTemplate.exchange(requestEntity, clazz);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			logger.error(uiMsg(MSG_ID_SERVICE_REQUEST_FAILED,
					e.getStatusCode().value(), e.getStatusCode().toString(), e.getResponseHeaders().getFirst("Warning")));
			throw new HttpClientErrorException(e.getStatusCode(), e.getResponseHeaders().getFirst("Warning"));
		} catch (HttpClientErrorException.Unauthorized e) {
			logger.error(uiMsg(MSG_ID_NOT_AUTHORIZED_FOR_SERVICE, e.getMessage()), e);
			throw e;
		} catch (RestClientException e) {
			String message = uiMsg(MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
			logger.error(message, e);
			throw new RestClientException(message, e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		
		// All PUT requests should return HTTP status OK
		if (!HttpStatus.OK.equals(entity.getStatusCode())) {
			String message = uiMsg(MSG_ID_SERVICE_REQUEST_FAILED, 
					entity.getStatusCodeValue(), entity.getStatusCode().toString(), entity.getHeaders().getFirst("Warning"));
			logger.error(message);
			throw new RuntimeException(message);
		}
		
		// Check connection result
		if (logger.isTraceEnabled()) logger.trace("<<< putToService()");
		return entity.getBody();
	}
	
	/**
	 * Calls a prosEO service at the given location with HTTP Post
	 * 
	 * @param <T> the class of the REST object to use as POST data
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
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			logger.error(uiMsg(MSG_ID_SERVICE_REQUEST_FAILED,
					e.getStatusCode().value(), e.getStatusCode().toString(), e.getResponseHeaders().getFirst("Warning")));
			throw HttpClientErrorException.create(e.getStatusCode(), e.getResponseHeaders().getFirst("Warning"), e.getResponseHeaders(), e.getResponseBodyAsByteArray(), null);
		} catch (HttpClientErrorException.Unauthorized e) {
			logger.error(uiMsg(MSG_ID_NOT_AUTHORIZED_FOR_SERVICE, e.getMessage()), e);
			throw e;
		} catch (RestClientException e) {
			String message = uiMsg(MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
			logger.error(message, e);
			throw new RestClientException(message, e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		
		// All POST requests should return HTTP status CREATED
		if (!HttpStatus.CREATED.equals(entity.getStatusCode())) {
			String message = uiMsg(MSG_ID_SERVICE_REQUEST_FAILED, 
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
	 * @param <T> the class of the REST object to use as PATCH data
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
			String message = uiMsg(MSG_ID_INVALID_URL, serviceUrl + requestPath, e.getMessage());
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
			String message = uiMsg(MSG_ID_SERIALIZATION_FAILED, e.getMessage());
			logger.error(message);
			throw new RuntimeException(message, e);
		}
		// Execute the HTTP request
		try {
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with PATCH", serviceUrl + requestPath);
			try {
				String responseContent =  httpclient.execute(req, httpResponse -> {
					int httpStatusCode = httpResponse.getStatusLine().getStatusCode();
					Header warningHeader = httpResponse.getFirstHeader("Warning");
					String warningMessage = (null == warningHeader ? "no message" : warningHeader.getValue());
					if (HttpStatus.UNAUTHORIZED.value() == httpStatusCode) {
						if (null != httpResponse.getEntity())
							httpResponse.getEntity().getContent().close();
						logger.error(uiMsg(MSG_ID_NOT_AUTHORIZED_FOR_SERVICE, warningMessage));
						throw HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, warningMessage, null, null, null);
					} else if (HttpStatus.NOT_FOUND.value() == httpStatusCode || HttpStatus.BAD_REQUEST.value() == httpStatusCode) {
						String reasonPhrase = httpResponse.getStatusLine().getReasonPhrase();
						if (null != httpResponse.getEntity())
							httpResponse.getEntity().getContent().close();
						logger.error(uiMsg(MSG_ID_SERVICE_REQUEST_FAILED,
								httpStatusCode, reasonPhrase, warningMessage));
						throw HttpClientErrorException.create(HttpStatus.valueOf(httpStatusCode), warningMessage, null, null, null);
					} else if (300 <= httpStatusCode){
						String reasonPhrase = httpResponse.getStatusLine().getReasonPhrase();
						if (null != httpResponse.getEntity())
							httpResponse.getEntity().getContent().close();
						logger.error(uiMsg(MSG_ID_HTTP_REQUEST_FAILED, reasonPhrase));
						throw new HttpClientErrorException(HttpStatus.valueOf(httpStatusCode), reasonPhrase);
					}
					return IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8);
				});
				// Convert Json response to object of requested class
				return mapper.readValue(responseContent, clazz);
			} catch (Exception e) {
				throw e;
			}
		} catch (IOException e) {
			String message = uiMsg(MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
			logger.error(message, e);
			throw new RestClientException(message, e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Calls a prosEO service at the given location with HTTP DELETE
	 * 
	 * @param serviceUrl the base URL of the service (protocol, hostname, port, base URI)
	 * @param requestPath the specific request path including request parameters
	 * @param username the username for basic HTTP authentication (optional)
	 * @param password the password for basic HTTP authentication (optional)
	 * @throws RestClientException if an error (HTTP status code 304, 4xx or 5xx) occurred in the communication to the service
	 */
	public void deleteFromService(String serviceUrl, String requestPath, String username, String password) throws RestClientException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteFromService({}, {}, user, password)", serviceUrl, requestPath);
		
		// Attempt connection to service
		ResponseEntity<Object> entity = null;
		try {
			RestTemplate restTemplate = ( null == username ? rtb.build() : rtb.basicAuthentication(username, password).build() );
			String requestUrl = serviceUrl + requestPath;
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with DELETE", requestUrl);
			//restTemplate.delete(requestUrl);
			entity = restTemplate.exchange(new URI(requestUrl), HttpMethod.DELETE, null, Object.class);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			String message = e.getResponseHeaders().getFirst("Warning");
			logger.error(uiMsg(MSG_ID_SERVICE_REQUEST_FAILED,
					e.getStatusCode().value(), e.getStatusCode().toString(), message));
			throw new RestClientResponseException(message, e.getRawStatusCode(), e.getStatusCode().getReasonPhrase(),
					e.getResponseHeaders(), e.getResponseBodyAsByteArray(), StandardCharsets.UTF_8);
		} catch (HttpClientErrorException.Unauthorized e) {
			logger.error(uiMsg(MSG_ID_NOT_AUTHORIZED_FOR_SERVICE, e.getMessage()), e);
			throw e;
		} catch (RestClientException e) {
			String message = uiMsg(MSG_ID_HTTP_REQUEST_FAILED, e.getMessage());
			logger.error(message, e);
			throw new RestClientException(message, e);
		} catch (URISyntaxException e) {
			String message = uiMsg(MSG_ID_INVALID_URL, serviceUrl + requestPath, e.getMessage());
			logger.error(message);
			throw new RuntimeException(message, e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		
		// Check for deletion failure indicated by 304 NOT_MODIFIED
		if (HttpStatus.NOT_MODIFIED.equals(entity.getStatusCode())) {
			String message = entity.getHeaders().getFirst("Warning");
			logger.error(uiMsg(MSG_ID_SERVICE_REQUEST_FAILED,
					entity.getStatusCodeValue(), entity.getStatusCode().getReasonPhrase(), message));
			throw new RestClientResponseException(message, entity.getStatusCodeValue(), entity.getStatusCode().getReasonPhrase(),
					entity.getHeaders(), null, StandardCharsets.UTF_8);
		} else if (!HttpStatus.NO_CONTENT.equals(entity.getStatusCode())) {
			// Ignore unexpected status code, but log it as warning
			logger.warn(uiMsg(MSG_ID_UNEXPECTED_STATUS, entity.getStatusCode().getReasonPhrase()));
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< deleteFromService()");
	}
	
}
