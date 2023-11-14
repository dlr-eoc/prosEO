/**
 * SendManager.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.notification.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.NotificationMessage;
import de.dlr.proseo.notification.NotificationConfiguration;
import de.dlr.proseo.notification.rest.model.RestMessage;
import de.dlr.proseo.notification.service.ServiceConnection;
import de.dlr.proseo.notification.service.ServiceMail;

/**
 * This SendManager class is responsible for preparing and sending messages either as HTTP requests or emails. It analyzes the
 * attributes of a given REST message and determines the appropriate sending protocol based on the message's endpoint. The class
 * supports sending messages via HTTP, HTTPS, or email (MAIL).
 *
 * @author Ernst Melchinger
 */
@Component
public class SendManager {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(SendManager.class);

	/** Allowed sending protocols, i.e. HTTP, HTTPS, MAIL, as well as UNKNOWN */
	private enum SendType {
		HTTP, HTTPS, MAIL, UNKNOWN
	}

	/** The configuration */
	@Autowired
	private NotificationConfiguration config;

	/** The service responsible for handling HTTP/HTTPS */
	@Autowired
	private ServiceConnection serviceConnection;

	/** The service responsible for handling mails */
	@Autowired
	private ServiceMail serviceMail;

	/**
	 * Evaluate the REST message attributes, parses and validates the message content, formats it where necessary, and delegates the
	 * sending either to the ServiceConnection class (HTTP/HTTPs) or the ServiceMail class (MAIL)
	 *
	 * @param restMessage a REST message
	 * @return a ResponseEntity representing the response from the sending operation
	 */
	public ResponseEntity<?> sendNotification(RestMessage restMessage) {
		if (logger.isTraceEnabled())
			logger.trace(">>> sendNotification({})", restMessage);

		// Analyze content of message
		String endpoint = null;
		SendType type = SendType.UNKNOWN;
		boolean raw = false;
		String message = "";
		String messageCode = null;
		String subject = config.getSubject();
		String contentType = config.getContentType();
		String user = null;
		String password = null;
		String sender = config.getMailSender();

		// Check if the endpoint is provided
		if (restMessage.getEndpoint() != null) {
			endpoint = restMessage.getEndpoint();

			// Detect the send type (HTTP, HTTPS, or MAIL)
			if (endpoint.toLowerCase().startsWith("https:")) {
				type = SendType.HTTPS;
			} else if (endpoint.toLowerCase().startsWith("http:")) {
				type = SendType.HTTP;
			} else if (endpoint.toLowerCase().startsWith("mailto:")) {
				type = SendType.MAIL;
			}

			// Throw an exception if the send type is unknown
			if (type == SendType.UNKNOWN) {
				throw new IllegalArgumentException(
						logger.log(NotificationMessage.MSG_ENDPOINT_TYPE_UNKNOWN, restMessage.getEndpoint()));
			}
		} else {
			// Throw an exception no send tyoe is set
			throw new IllegalArgumentException(logger.log(NotificationMessage.MSG_ENDPOINT_NOT_SET));
		}

		// Check user and password for HTTP-based protocols
		if (type == SendType.HTTPS || type == SendType.HTTP) {
			if (restMessage.getUser() == null || restMessage.getPassword() == null) {
				throw new IllegalArgumentException(logger.log(NotificationMessage.MSG_USER_PASSWORD_NOT_SET));
			}
			user = restMessage.getUser();
			password = restMessage.getPassword();
		}

		// Check if a content type is provided, otherwise use the default from the configuration
		if (restMessage.getContentType() != null) {
			contentType = restMessage.getContentType();
		}

		// Parse the media type
		MediaType mediaType = null;
		try {
			mediaType = MediaType.parseMediaType(contentType);
		} catch (InvalidMediaTypeException e) {
			try {
				// If the provided media type is invalid, fall back to the default from the configuration
				mediaType = MediaType.parseMediaType(config.getContentType());
			} catch (InvalidMediaTypeException e2) {
				// If both provided and default media types are invalid, use TEXT_PLAIN as a fallback
				mediaType = MediaType.TEXT_PLAIN;
			}
			logger.log(NotificationMessage.MSG_INVALID_CONTENT_TYPE, mediaType);
		}

		// Check if the message content is provided
		if (restMessage.getMessage() != null) {
			message = restMessage.getMessage();

			// Detect the content type of the message and format it if necessary
			// TODO Right now, raw always evaluates to false
			if (raw) {
				// TODO Check whether contentType fits or correct it
			} else {
				// TODO Format the message if necessary
			}
		} else {
			// If the message content is missing, throw an exception
			throw new IllegalArgumentException(logger.log(NotificationMessage.MSG_MISSING_MESSAGE_CONTENT));
		}

		// Check if a custom subject is provided, otherwise use the default from the configuration
		if (restMessage.getSubject() != null && !restMessage.getSubject().isBlank()) {
			subject = restMessage.getSubject();
		}

		// Check if a custom sender is provided, otherwise use the default from the configuration
		if (type != SendType.MAIL && (restMessage.getSender() != null && !restMessage.getSender().isBlank())) {
			sender = restMessage.getSender();
		}

		// Send the message based on the detected send type
		switch (type) {
		case HTTPS:
		case HTTP:
			return serviceConnection.postToService(endpoint, user, password, subject, mediaType, messageCode, message, sender);
		case MAIL:
			return serviceMail.sendMail(endpoint, user, password, subject, mediaType, messageCode, message, sender);
		default:
			break;
		}

		// Throw an exception if the send type is unknown
		throw new IllegalArgumentException(logger.log(NotificationMessage.MSG_ENDPOINT_TYPE_UNKNOWN, restMessage.getEndpoint()));
	}
}