/**
 * ProseoHttp.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.UIMessage;

/**
 * @author Katharina Bassler
 *
 */
public class ProseoHttp {

	/** A logger for this class */
	private final ProseoLogger logger;

	/** A prefix indicating the service */
	private final HttpPrefix prefix;

	/**
	 * @param logger The calling class's logger
	 * @param prefix A prefix indicatimg the service
	 */
	public ProseoHttp(ProseoLogger logger, HttpPrefix prefix) {
		this.logger = logger;
		this.prefix = prefix;
	}

	/**
	 * prosEO message format, e. g. "[Warning: 199 proseo-processor-mgr (E2205)
	 * Product type L2________ invalid for mission NM4T]"
	 */
	private static final Pattern PROSEO_MESSAGE_TEMPLATE_A = Pattern
		.compile("\\[Warning:\"199 +\\S+ +(?<message>\\([IWEF]\\d+\\) .*)\"\\]");

	/**
	 * prosEO message format, e. g. "199 proseo-processor-mgr (E2205) Product type
	 * L2________ invalid for mission NM4T"
	 */
	private static final Pattern PROSEO_MESSAGE_TEMPLATE_B = Pattern.compile("199 +\\S+ +(?<message>\\([IWEF]\\d+\\) .*)");

	// Method taken from ui/backend/ServiceConnection.java and adapted
	/**
	 * Create an HTTP "Warning" header with the given text message
	 *
	 * @param loggedMessage the message text
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

		Matcher m = PROSEO_MESSAGE_TEMPLATE_A.matcher(warningHeader);
		if (m.matches()) {
			return m.group("message");
		}

		m = PROSEO_MESSAGE_TEMPLATE_B.matcher(warningHeader);
		if (m.matches()) {
			return m.group("message");
		}

		if (logger.isTraceEnabled())
			logger.trace("... no prosEO message found: [" + warningHeader + "]");

		return null;
	}

	// Method taken from ui/backend/ServiceConnection.java and adapted
	/**
	 * Checks whether the error message from the "Warning" header is
	 * prosEO-compliant; if so, returns the error message from the header, otherwise
	 * generates a generic error message
	 *
	 * @param httpStatus  the HTTP status returned by the REST call
	 * @param httpHeaders the HTTP headers returned by the REST call
	 * @return a formatted error message
	 */
	public String createMessageFromHeaders(HttpStatus httpStatus, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createMessageFromHeaders({}, httpHeaders)", httpStatus);

		if (httpStatus == null | httpHeaders == null) {
			if (logger.isTraceEnabled())
				logger.trace("... no prosEO message found: [header = " + httpHeaders + ", status =" + httpStatus + "]");
			return null;
		}

		String warningHeader = httpHeaders.getFirst(HttpHeaders.WARNING);
		String warningMessage = extractProseoMessage(warningHeader);

		return (null == warningMessage
				? ProseoLogger.format(UIMessage.SERVICE_REQUEST_FAILED, httpStatus.value(), httpStatus.toString(), warningHeader)
				: warningMessage);
	}
}
