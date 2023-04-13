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
 * The send manager prepares the message and send it as HTTP request or mail
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class SendManager {
	/**
	 * Logger of this class
	 */
	private static ProseoLogger logger = new ProseoLogger(SendManager.class);

	private enum SendType {
			HTTP,
			HTTPS,
			MAIL,
			UNKNOWN
	};

	/** The configuration */
	@Autowired
	private NotificationConfiguration config;
	
	/** The HTTP service */
	@Autowired
	private ServiceConnection serviceConnection;
	
	/**
	 * The mail service
	 */
	@Autowired
	private ServiceMail serviceMail;
	
	/**
	 * Evaluate the rest message attributes
	 * 
	 * @param restMessage The rest message
	 * @return The response entity
	 */
	public ResponseEntity<?> sendNotification(RestMessage restMessage) {
		if (logger.isTraceEnabled()) logger.trace(">>> sendNotification({})", restMessage);
		
		// Analyze content of message
		String endpoint = null;
		SendType type = SendType.UNKNOWN;
		Boolean raw = false;
		String message = "";
		String messageCode = null;
		String subject = config.getSubject();
		String contentType = config.getContentType();
		String user = null;
		String password = null;
		String sender = config.getSender();
		
		if (restMessage.getEndpoint() != null) {
			endpoint = restMessage.getEndpoint();
			// Detect send type, supported are http(s) and mailto
			if (endpoint.toLowerCase().startsWith("https:")) {
				type = SendType.HTTPS;
			} else if (endpoint.toLowerCase().startsWith("http:")) {
				type = SendType.HTTP;
			} else if (endpoint.toLowerCase().startsWith("mailto:")) {
				type = SendType.MAIL;
			}
			if (type == SendType.UNKNOWN) {
				throw new IllegalArgumentException(
						logger.log(NotificationMessage.MSG_ENDPOINT_TYPE_UNKNOWN, restMessage.getEndpoint()));
			}
		} else {
			throw new IllegalArgumentException(
					logger.log(NotificationMessage.MSG_ENDPOINT_NOT_SET));
		}
		if (type == SendType.HTTPS || type == SendType.HTTP) {
			// user and password required
			if (restMessage.getUser() == null || restMessage.getPassword() == null) {
				// error, user and password required but not exist
				throw new IllegalArgumentException(
						logger.log(NotificationMessage.MSG_USER_PASSWORD_NOT_SET));
			}
		}
		if (restMessage.getContentType() != null) {
			contentType = restMessage.getContentType();
		}
		MediaType mediaType = null;
		try {
			mediaType = MediaType.parseMediaType(contentType);			
		} catch (InvalidMediaTypeException e) {
			try {
				mediaType = MediaType.parseMediaType(config.getContentType());		
			} catch (InvalidMediaTypeException e2) {
				mediaType = MediaType.TEXT_PLAIN;
			}
			logger.log(NotificationMessage.MSG_INVALID_CONTENT_TYPE, mediaType);
		}
		if (restMessage.getMessage() != null) {
			message = restMessage.getMessage();
			// Detect the content type of the message
			if (raw) {
				// TODO: small check whether contentType fits or correct it
			} else {
				// format the message if necessary 
			}
		} else {
			// mandatory, return with error
			throw new IllegalArgumentException(
					logger.log(NotificationMessage.MSG_MISSING_MESSAGE_CONTENT));
			
		}
		if (restMessage.getSubject() != null && !restMessage.getSubject().isBlank()) {
			subject = restMessage.getSubject();
		}
		if (restMessage.getSender() != null && !restMessage.getSender().isBlank()) {
			sender = restMessage.getSender();
		}
		
		String result = null;
		switch (type) {
		case HTTPS:
		case HTTP:
			return serviceConnection.postToService(endpoint, user, password, subject, mediaType, messageCode, message, sender);
		case MAIL:
			return serviceMail.sendMail(endpoint, user, password, subject, mediaType, messageCode, message, sender);
		default:
			break;
		}

		throw new IllegalArgumentException(
				logger.log(NotificationMessage.MSG_ENDPOINT_TYPE_UNKNOWN, restMessage.getEndpoint()));
	}
}
