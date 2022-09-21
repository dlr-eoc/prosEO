/**
 * ProseoHttp.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;

import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * @author Katharina Bassler
 *
 */
public class ProseoHttp {
	
	private final ProseoLogger logger;
	private final HttpPrefix prefix;
		
	public ProseoHttp(ProseoLogger logger, HttpPrefix prefix) {
		this.logger = logger;
		this.prefix = prefix;
	}

	// prosEO message format, e. g. "199 proseo-processor-mgr (E2205) Product type
	// L2________ invalid for mission NM4T"
	private static final Pattern PROSEO_MESSAGE_TEMPLATE = Pattern
			.compile("\\[Warning:\"199 +\\S+ +(?<message>\\([IWEF]\\d+\\) .*)\"\\]");

	// Method taken from ui/backend/ServiceConnection.java and adapted
	/**
	 * Create an HTTP "Warning" header with the given text message
	 *
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	public HttpHeaders errorHeaders(String loggedMessage) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HttpHeaders.WARNING, prefix.getPrefix() + (null == loggedMessage ? "null" : loggedMessage));
		return responseHeaders;
	}

	// Method taken from ui/backend/ServiceConnection.java and adapted
	/**
	 * Extracts the prosEO-compliant message from the "Warning" header, if any
	 *
	 * @param warningHeader the warning header to check (may be null)
	 * @return the prosEO-compliant message, if there is one, or null otherwise
	 */
	public String extractProseoMessage(String warningHeader) {
		if (logger.isTraceEnabled())
			logger.trace(">>> extractProseoMessage({})", warningHeader);

		if (null == warningHeader)
			return null;

		Matcher m = PROSEO_MESSAGE_TEMPLATE.matcher(warningHeader);
		if (m.matches()) {
			return m.group("message");
		}
		if (logger.isTraceEnabled())
			logger.trace("... no prosEO message found: [" + warningHeader + "]");

		return null;
	}

}
