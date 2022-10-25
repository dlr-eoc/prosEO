/**
 * LogUtil.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip.odata;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataServerError;

/**
 * Utility methods for logging and error reporting
 * 
 * @author Dr. Thomas Bassler
 */
public class LogUtil {

	// prosEO message format, e. g. "(E2205) Product type L2________ invalid for mission NM4T"
	private static final Pattern PROSEO_MESSAGE_TEMPLATE = Pattern.compile("\\((?<messageCode>[IWEF]\\d+)\\) (?<message>.*)");

	/**
	 * Create an ODataServerError object for the error response body from an HTTP status code and a message
	 * 
	 * @param statusCode the HTTP status code to set
	 * @param message the message to set
	 * @return
	 */
	public static ODataServerError oDataServerError(int statusCode, String message) {
		ODataServerError serverError = new ODataServerError();

		serverError.setStatusCode(statusCode);
		
		Matcher m = PROSEO_MESSAGE_TEMPLATE.matcher(message);
		if (m.matches()) {
			serverError.setCode(m.group("messageCode"));
			serverError.setMessage(m.group("message"));
		} else {
			serverError.setCode(HttpStatusCode.fromStatusCode(statusCode).toString());
			serverError.setMessage(message);
		}
		
		serverError.setLocale(Locale.ROOT);
		
		return serverError;
	}
}
