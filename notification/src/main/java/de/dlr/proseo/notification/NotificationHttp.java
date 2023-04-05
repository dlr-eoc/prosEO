package de.dlr.proseo.notification;

import org.springframework.http.HttpHeaders;

import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * HTTP warning header
 * 
 * @author Ernst Melchinger
 *
 */
public class NotificationHttp extends ProseoHttp {
	private String stringPrefix;
	
	public NotificationHttp(ProseoLogger logger, String prefix) {
		super(logger, null);
		stringPrefix = prefix;
	}		

	/**
	 * Create an HTTP "Warning" header with the given text message
	 *
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	public HttpHeaders errorHeaders(String loggedMessage) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HttpHeaders.WARNING, stringPrefix + (null == loggedMessage ? "null" : loggedMessage));
		return responseHeaders;
	}
}
