package de.dlr.proseo.api.odip;

import org.springframework.http.HttpHeaders;

import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;

public class OdipHttp extends ProseoHttp {
	private String stringPrefix;
	
	public OdipHttp(ProseoLogger logger, String prefix) {
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
