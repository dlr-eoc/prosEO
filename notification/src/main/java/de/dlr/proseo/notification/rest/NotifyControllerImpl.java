/**
 * NotifyControllerImpl.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.notification.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.notification.rest.model.RestMessage;

/**
 * REST controller that receives and processes notifications by delegating the task of sending messages to the SendManager class,
 * and it handles exceptions and returns appropriate responses to the client
 *
 * @author Ernst Melchinger
 */
@Component
public class NotifyControllerImpl implements NotifyController {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(NotifyControllerImpl.class);

	/** HTTP service methods */
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.NOTIFICATION);

	/** The send manager */
	@Autowired
	private SendManager sendManager;

	/**
	 * Process and send the REST message
	 *
	 * @param restMessage the REST message to send
	 * @return The response entity
	 */
	@Override
	public ResponseEntity<?> notifyx(RestMessage restMessage) {
		if (logger.isTraceEnabled())
			logger.trace(">>> notify({})", restMessage);

		try {
			ResponseEntity<?> result = sendManager.sendNotification(restMessage);
			return new ResponseEntity<>(result, HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (HttpClientErrorException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), e.getStatusCode());
		} catch (Exception e) {
			String msg = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}