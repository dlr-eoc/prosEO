package de.dlr.proseo.notification.service;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OdipMessage;
import de.dlr.proseo.notification.NotificationConfiguration;
import de.dlr.proseo.notification.NotificationHttp;
/**
 * The HTTP service to post the message
 * 
 * @author Ernst Melchinger
 *
 */
@Service
public class ServiceConnection {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ServiceConnection.class);
	
	private static String HTTP_PREFIX = "199 proseo-notification ";
	private static NotificationHttp http = new NotificationHttp(logger, HTTP_PREFIX);


	/** REST template builder */
	@Autowired
	private RestTemplateBuilder rtb;

	/**
	 * The configuration
	 */
	@Autowired
	NotificationConfiguration config;

	/**
	 * Calls a prosEO service at the given location with HTTP Post
	 * 
	 * @param endpoint The HTTP address
	 * @param user The user name for basic HTTP authentication (optional)
	 * @param password The password for basic HTTP authentication (optional)
	 * @param subject The message subject
	 * @param mediaType The media type to send the message
	 * @param messageCode The message code
	 * @param message The message body
	 * @param sender The message sender
	 * @return The response entity
	 * @throws RestClientException if an error (HTTP status code 4xx or 5xx) occurred in the communication to the service
	 * @throws RuntimeException if the service returned an HTTP status different from OK (200)
	 */
	
	public ResponseEntity<?> postToService(String endpoint, String user, String password, String subject, MediaType mediaType, 
			String messageCode, String message, String sender)
			throws RestClientException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> postToService({}, {})", endpoint, message);
		
		// Attempt connection to service
		ResponseEntity<?> entity = null;
		try {
			RestTemplate restTemplate = ( null == user ? rtb : rtb.basicAuthentication(user, password) )
					.setReadTimeout(Duration.ofSeconds(config.getHttpTimeout()))
					.build();
			URI requestUrl = URI.create(endpoint);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(mediaType);
			headers.setOrigin(sender);
			HttpEntity<String> httpEntity = new HttpEntity<>(message, headers);
			if (logger.isTraceEnabled()) logger.trace("... calling with POST: {}", restTemplate.toString());
			entity = restTemplate.postForEntity(requestUrl, httpEntity, String.class);
		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
			String response = http.createMessageFromHeaders(e.getStatusCode(), e.getResponseHeaders());
			logger.log(OdipMessage.EXTRACTED_MESSAGE, response);
			throw new HttpClientErrorException(e.getStatusCode(), response);
		} catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
			String warningMessage = http.extractProseoMessage(e.getResponseHeaders().getFirst(HttpHeaders.WARNING));
			if (warningMessage == null) 
				logger.log(OdipMessage.EXTRACTED_MESSAGE, warningMessage);
				else
					logger.log(OdipMessage.NOT_AUTHORIZED_FOR_SERVICE, user);
			throw new HttpClientErrorException(e.getStatusCode(), warningMessage);
		} catch (RestClientException e) {
			String response = logger.log(OdipMessage.HTTP_REQUEST_FAILED, e);
			throw new RestClientException(response, e);
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);
			throw new RuntimeException(e);
		}
		
		// Check for NOT_MODIFIED (POST used for creating sub-objects)
		if (HttpStatus.NOT_MODIFIED.equals(entity.getStatusCode())) {
			throw new RestClientResponseException(entity.getHeaders().getFirst(HttpHeaders.WARNING), HttpStatus.NOT_MODIFIED.value(),
					HttpStatus.NOT_MODIFIED.toString(), entity.getHeaders(), "".getBytes(), Charset.defaultCharset());
		}
		
		// All successful POST requests should return HTTP status CREATED
		if (!entity.getStatusCode().is2xxSuccessful()) {
			String response = http.createMessageFromHeaders(entity.getStatusCode(), entity.getHeaders());
			logger.log(OdipMessage.EXTRACTED_MESSAGE, response);
			throw new RuntimeException(response);
		}
		
		// Check connection result
		if (logger.isTraceEnabled()) logger.trace("<<< postToService()");
		return entity;
	}
	
}
