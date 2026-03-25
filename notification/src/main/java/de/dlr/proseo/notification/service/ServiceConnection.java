/**
 * ServiceConnection.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
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

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OdipMessage;
import de.dlr.proseo.notification.NotificationConfiguration;

/**
 * The HTTP service to post the message. This service is responsible for sending an HTTP POST request to a specified endpoint,
 * providing the necessary authentication, headers, and message body.
 *
 * @author Ernst Melchinger
 */
@Service
public class ServiceConnection {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ServiceConnection.class);

	/** HTTP service methods */
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.NOTIFICATION);

	/** REST template builder */
	@Autowired
	private RestTemplateBuilder rtb;

	/** The configuration of the notification service */
	@Autowired
	NotificationConfiguration config;

	/**
	 * Constructs and sends an HTTP POST request to the specified endpoint, including the necessary authentication, headers, and
	 * message body. It handles exceptions and returns appropriate response entities based on the HTTP status codes received.
	 *
	 * @param endpoint    The HTTP address
	 * @param user        The user name for basic HTTP authentication
	 * @param password    The password for basic HTTP authentication
	 * @param subject     The message subject
	 * @param mediaType   The media type of the message body
	 * @param messageCode The message code
	 * @param message     The message body
	 * @param sender      The message sender
	 * @return The response entity
	 * @throws RestClientException if an error (HTTP status code 4xx or 5xx) occurred in the communication to the service
	 * @throws RuntimeException    if the service returned an HTTP status different from OK (200)
	 */
	public Object postToService(String endpoint, String user, String password, String subject, MediaType mediaType,
			String messageCode, String message, String sender) throws RestClientException, RuntimeException {
		if (logger.isTraceEnabled())
			logger.trace(">>> postToService({}, {})", endpoint, message);

		// Attempt connection to the service
		ResponseEntity<?> entity = null;
		try {

			// Build the RestTemplate for making the HTTP POST request
			RestTemplate restTemplate = (null == user ? rtb : rtb.basicAuthentication(user, password))
				.readTimeout(Duration.ofSeconds(config.getHttpTimeout()))
				.build();

			// Create the request URL from the provided endpoint
			URI requestUrl = URI.create(endpoint);

			// Set the headers for the HTTP request
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(mediaType);
			headers.setOrigin(sender);

			// Create the HttpEntity with the message body and headers
			HttpEntity<String> httpEntity = new HttpEntity<>(message, headers);

			// Make the HTTP POST request using RestTemplate
			if (logger.isTraceEnabled())
				logger.trace("... calling with POST: {}", restTemplate.toString());
			entity = restTemplate.postForEntity(requestUrl, httpEntity, String.class);

		} catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {

			// Handle HTTP 400 Bad Request or HTTP 404 Not Found errors
			String response = http.createMessageFromHeaders(e.getStatusCode(), e.getResponseHeaders());

			logger.log(OdipMessage.EXTRACTED_MESSAGE, response);

			throw new HttpClientErrorException(e.getStatusCode(), response);
		} catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {

			// Handle HTTP 401 Unauthorized or HTTP 403 Forbidden errors
			String warningMessage = http.extractProseoMessage(e.getResponseHeaders().getFirst(HttpHeaders.WARNING));

			if (warningMessage == null)
				logger.log(OdipMessage.EXTRACTED_MESSAGE, warningMessage);
			else
				logger.log(OdipMessage.NOT_AUTHORIZED_FOR_SERVICE, user);

			throw new HttpClientErrorException(e.getStatusCode(), warningMessage);
		} catch (RestClientException e) {

			// Handle other RestClientExceptions (e.g., network errors)
			String response = logger.log(OdipMessage.HTTP_REQUEST_FAILED, e);

			throw new RestClientException(response, e);
		} catch (Exception e) {

			// Handle general exceptions
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);

			throw new RuntimeException(e);
		}

		// Check for NOT_MODIFIED (POST used for creating sub-objects)
		if (HttpStatus.NOT_MODIFIED.equals(entity.getStatusCode())) {
			throw new RestClientResponseException(entity.getHeaders().getFirst(HttpHeaders.WARNING),
					HttpStatus.NOT_MODIFIED.value(), HttpStatus.NOT_MODIFIED.toString(), entity.getHeaders(), "".getBytes(),
					Charset.defaultCharset());
		}

		// All successful POST requests should return HTTP status CREATED
		if (!entity.getStatusCode().is2xxSuccessful()) {
			String response = http.createMessageFromHeaders(entity.getStatusCode(), entity.getHeaders());
			logger.log(OdipMessage.EXTRACTED_MESSAGE, response);
			throw new RuntimeException(response);
		}

		// Check connection result
		if (logger.isTraceEnabled())
			logger.trace("<<< postToService()");
		return entity.getBody();
	}

}
