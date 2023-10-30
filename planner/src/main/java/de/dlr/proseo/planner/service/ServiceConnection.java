package de.dlr.proseo.planner.service;


import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.auth.AUTH;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
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

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.planner.ProductionPlannerConfiguration;

/**
 * Service class to connect to the prosEO backend services from planner
 * 
 * @author Dr. Thomas Bassler, Ernst Melchinger
 */
@Service
public class ServiceConnection {
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ServiceConnection.class);
	
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PLANNER);


	/** REST template builder */
	@Autowired
	private RestTemplateBuilder rtb;

	/**
	 * MonitorServices configuration
	 */
	@Autowired
	ProductionPlannerConfiguration config;
	
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
			RestTemplate restTemplate = ( null == username ? rtb : rtb.basicAuthentication(username, password) )
					.setReadTimeout(Duration.ofSeconds(config.getHttpTimeout()))
					.build();
			URI requestUrl = URI.create(serviceUrl + requestPath);
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with GET", requestUrl);
			entity = restTemplate.getForEntity(requestUrl, clazz);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			String message = http.createMessageFromHeaders(e.getStatusCode(), e.getResponseHeaders());
			logger.log(PlannerMessage.EXTRACTED_MESSAGE, message);
			throw new HttpClientErrorException(e.getStatusCode(), message);
		} catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
			String warningMessage = http.extractProseoMessage(e.getResponseHeaders().getFirst(HttpHeaders.WARNING));
			if (warningMessage == null) 
				logger.log(PlannerMessage.EXTRACTED_MESSAGE, warningMessage);
				else
					logger.log(PlannerMessage.NOT_AUTHORIZED_FOR_SERVICE, username);
			throw new HttpClientErrorException(e.getStatusCode(), warningMessage);
		} catch (RestClientResponseException e) {
			logger.log(PlannerMessage.HTTP_REQUEST_FAILED, e.getMessage());
			logger.log(PlannerMessage.HTTP_REQUEST_FAILED, e.getResponseBodyAsString());
			throw e;
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			throw new RuntimeException(e);
		}
		
		// All GET requests should return HTTP status OK
		if (!HttpStatus.OK.equals(entity.getStatusCode())) {
			String message = http.createMessageFromHeaders(entity.getStatusCode(), entity.getHeaders());
			logger.log(PlannerMessage.EXTRACTED_MESSAGE, message);
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
			RestTemplate restTemplate = ( null == username ? rtb : rtb.basicAuthentication(username, password) )
					.setReadTimeout(Duration.ofSeconds(config.getHttpTimeout()))
					.build();
			RequestEntity<Void> requestEntity = RequestEntity.put(URI.create(serviceUrl + requestPath)).build();
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with GET", requestEntity.getUrl());
			entity = restTemplate.exchange(requestEntity, clazz);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			String message = http.createMessageFromHeaders(e.getStatusCode(), e.getResponseHeaders());
			logger.log(PlannerMessage.EXTRACTED_MESSAGE, message);
			throw new HttpClientErrorException(e.getStatusCode(), message);
		} catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
			String warningMessage = http.extractProseoMessage(e.getResponseHeaders().getFirst(HttpHeaders.WARNING));
			if (warningMessage == null) 
				logger.log(PlannerMessage.EXTRACTED_MESSAGE, warningMessage);
				else
					logger.log(PlannerMessage.NOT_AUTHORIZED_FOR_SERVICE, username);
			throw new HttpClientErrorException(e.getStatusCode(), warningMessage);
		} catch (RestClientResponseException e) {
			logger.log(PlannerMessage.HTTP_REQUEST_FAILED, e.getMessage());
			logger.log(PlannerMessage.HTTP_REQUEST_FAILED, e.getResponseBodyAsString());
			throw e;
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			throw new RuntimeException(e);
		}
		
		// All PUT requests should return HTTP status OK (for updates) or CREATED (for newly created items)
		if (!HttpStatus.OK.equals(entity.getStatusCode()) && !HttpStatus.CREATED.equals(entity.getStatusCode())) {
			String message = http.createMessageFromHeaders(entity.getStatusCode(), entity.getHeaders());
			logger.log(PlannerMessage.EXTRACTED_MESSAGE, message);
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
			RestTemplate restTemplate = ( null == username ? rtb : rtb.basicAuthentication(username, password) )
					.setReadTimeout(Duration.ofSeconds(config.getHttpTimeout()))
					.build();
			URI requestUrl = URI.create(serviceUrl + requestPath);
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with POST", requestUrl);
			entity = restTemplate.postForEntity(requestUrl, restObject, clazz);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			String message = http.createMessageFromHeaders(e.getStatusCode(), e.getResponseHeaders());
			logger.log(PlannerMessage.EXTRACTED_MESSAGE, message);
			throw new HttpClientErrorException(e.getStatusCode(), message);
		} catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
			String warningMessage = http.extractProseoMessage(e.getResponseHeaders().getFirst(HttpHeaders.WARNING));
			if (warningMessage == null) 
				logger.log(PlannerMessage.EXTRACTED_MESSAGE, warningMessage);
				else
					logger.log(PlannerMessage.NOT_AUTHORIZED_FOR_SERVICE, username);
			throw new HttpClientErrorException(e.getStatusCode(), warningMessage);
		} catch (RestClientResponseException e) {
			logger.log(PlannerMessage.HTTP_REQUEST_FAILED, e.getMessage());
			logger.log(PlannerMessage.HTTP_REQUEST_FAILED, e.getResponseBodyAsString());
			throw e;
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			throw new RuntimeException(e);
		}
		
		// Check for NOT_MODIFIED (POST used for creating sub-objects)
		if (HttpStatus.NOT_MODIFIED.equals(entity.getStatusCode())) {
			throw new RestClientResponseException(entity.getHeaders().getFirst(HttpHeaders.WARNING), HttpStatus.NOT_MODIFIED.value(),
					HttpStatus.NOT_MODIFIED.toString(), entity.getHeaders(), "".getBytes(), Charset.defaultCharset());
		}
		
		// All successful POST requests should return HTTP status CREATED
		if (!HttpStatus.CREATED.equals(entity.getStatusCode())) {
			String message = http.createMessageFromHeaders(entity.getStatusCode(), entity.getHeaders());
			logger.log(PlannerMessage.EXTRACTED_MESSAGE, message);
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
		req.setConfig(RequestConfig.custom().setConnectTimeout(config.getHttpTimeout().intValue()).build());
		try {
			req.setURI(URI.create(serviceUrl + requestPath));
		} catch (IllegalArgumentException e) {
			String message = logger.log(PlannerMessage.INVALID_URL, serviceUrl + requestPath, e.getMessage());
			throw new RuntimeException(message, e);
		}
		req.addHeader(new BasicHeader(AUTH.WWW_AUTH_RESP, AuthSchemes.BASIC + " " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes())));
		req.addHeader(new BasicHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()));
		try {
			String jsonObject = mapper.writeValueAsString(restObject);
			if (logger.isTraceEnabled()) logger.trace("... serialized Json object: " + jsonObject);
			req.setEntity(new StringEntity(jsonObject));
		} catch (Exception e) {
			String message = logger.log(PlannerMessage.SERIALIZATION_FAILED, e.getMessage());
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			throw new RuntimeException(message, e);
		}
		// Execute the HTTP request
		try {
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with PATCH", serviceUrl + requestPath);
			
			String responseContent =  httpclient.execute(req, httpResponse -> {
				int httpStatusCode = httpResponse.getStatusLine().getStatusCode();
				Header warningHeader = httpResponse.getFirstHeader(HttpHeaders.WARNING);
				
				String message = null;
				String proseoMessage = null;
				if (null == warningHeader) {
					message = "no message";
				} else {
					proseoMessage = http.extractProseoMessage(warningHeader.getValue());
					if (null == proseoMessage) {
						HttpHeaders messageHeaders = new HttpHeaders();
						messageHeaders.add(HttpHeaders.WARNING, warningHeader.getValue());
						message = http.createMessageFromHeaders(HttpStatus.valueOf(httpStatusCode), messageHeaders);
					} else {
						message = proseoMessage;
					}
				}
				
				if (HttpStatus.UNAUTHORIZED.value() == httpStatusCode || HttpStatus.FORBIDDEN.value() == httpStatusCode) {
					if (null != httpResponse.getEntity())
						httpResponse.getEntity().getContent().close();
					if (proseoMessage == null) 
						logger.log(PlannerMessage.EXTRACTED_MESSAGE, proseoMessage);
						else
							logger.log(PlannerMessage.NOT_AUTHORIZED_FOR_SERVICE, username);
					throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, proseoMessage);
				} else if (HttpStatus.NOT_FOUND.value() == httpStatusCode || HttpStatus.BAD_REQUEST.value() == httpStatusCode) {
					if (null != httpResponse.getEntity())
						httpResponse.getEntity().getContent().close();
					logger.log(PlannerMessage.EXTRACTED_MESSAGE, message);
					throw new HttpClientErrorException(HttpStatus.valueOf(httpStatusCode), message);
				} else if (HttpStatus.NOT_MODIFIED.value() == httpStatusCode) {
					if (null != httpResponse.getEntity())
						httpResponse.getEntity().getContent().close();
					logger.log(PlannerMessage.NOT_MODIFIED);
					throw new RestClientResponseException(message, httpStatusCode, 
							HttpStatus.NOT_MODIFIED.getReasonPhrase(), null, null, null);
				} else if (300 <= httpStatusCode){
					String reasonPhrase = httpResponse.getStatusLine().getReasonPhrase();
					if (null != httpResponse.getEntity())
						httpResponse.getEntity().getContent().close();
					logger.log(PlannerMessage.HTTP_REQUEST_FAILED, reasonPhrase);
					throw new HttpClientErrorException(HttpStatus.valueOf(httpStatusCode), reasonPhrase);
				}
				return IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8);
			});
			
			// Convert Json response to object of requested class
			return mapper.readValue(responseContent, clazz);
			
		} catch (IOException e) {
			String message = logger.log(PlannerMessage.HTTP_REQUEST_FAILED, e.getMessage());
			logger.log(PlannerMessage.HTTP_REQUEST_FAILED, e);
			throw new RestClientException(message, e);
		} catch (RestClientResponseException e) {
			logger.log(PlannerMessage.HTTP_REQUEST_FAILED, e.getMessage());
			logger.log(PlannerMessage.HTTP_REQUEST_FAILED, e.getResponseBodyAsString());
			throw e;
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

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
			RestTemplate restTemplate = ( null == username ? rtb : rtb.basicAuthentication(username, password) )
					.setReadTimeout(Duration.ofSeconds(config.getHttpTimeout()))
					.build();
			URI requestUrl = URI.create(serviceUrl + requestPath);
			if (logger.isTraceEnabled()) logger.trace("... calling service URL {} with DELETE", requestUrl);
			//restTemplate.delete(requestUrl);
			entity = restTemplate.exchange(requestUrl, HttpMethod.DELETE, null, Object.class);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			String message = http.createMessageFromHeaders(e.getStatusCode(), e.getResponseHeaders());
			logger.log(PlannerMessage.EXTRACTED_MESSAGE, message);
			throw new RestClientResponseException(message, e.getRawStatusCode(), e.getStatusCode().getReasonPhrase(),
					e.getResponseHeaders(), e.getResponseBodyAsByteArray(), StandardCharsets.UTF_8);
		} catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
			String warningMessage = http.extractProseoMessage(e.getResponseHeaders().getFirst(HttpHeaders.WARNING));
			if (warningMessage == null) 
				logger.log(PlannerMessage.EXTRACTED_MESSAGE, warningMessage);
				else
					logger.log(PlannerMessage.NOT_AUTHORIZED_FOR_SERVICE, username);
			throw new HttpClientErrorException(e.getStatusCode(), warningMessage);
		} catch (RestClientResponseException e) {
			logger.log(PlannerMessage.HTTP_REQUEST_FAILED, e.getMessage());
			logger.log(PlannerMessage.HTTP_REQUEST_FAILED, e.getResponseBodyAsString());
			throw e;
		} catch (IllegalArgumentException e) {
			String message = logger.log(PlannerMessage.INVALID_URL, serviceUrl + requestPath, e.getMessage());
			throw new RuntimeException(message, e);
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			throw new RuntimeException(e);
		}
		
		// Check for deletion failure indicated by 304 NOT_MODIFIED
		if (HttpStatus.NOT_MODIFIED.equals(entity.getStatusCode())) {
			String message = http.createMessageFromHeaders(entity.getStatusCode(), entity.getHeaders());
			logger.log(PlannerMessage.EXTRACTED_MESSAGE, message);
			throw new RestClientResponseException(message, entity.getStatusCodeValue(), entity.getStatusCode().getReasonPhrase(),
					entity.getHeaders(), null, StandardCharsets.UTF_8);
		} else if (!HttpStatus.NO_CONTENT.equals(entity.getStatusCode())) {
			// Ignore unexpected status code, but log it as warning
			logger.log(PlannerMessage.WARN_UNEXPECTED_STATUS, entity.getStatusCode().getReasonPhrase());
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< deleteFromService()");
	}
	
}

