/**
 * LoggingDemo.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging;

import org.springframework.http.HttpHeaders;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OrderMgrMessage;

/**
 * A demonstration of the prosEO logging mechanism.
 * 
 * @author Katharina Bassler
 *
 */
public class LoggingDemo {

	// the class logger
	private static ProseoLogger logger = new ProseoLogger(LoggingDemo.class);

	// pretending to be within order manager module
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.ORDER_MGR);

	public static void main(String[] args) {

		// trace and debug: placeholders as used by Logback, i.e. {}
		if (logger.isTraceEnabled())
			logger.trace("msg");

		if (logger.isDebugEnabled()) {
			logger.debug("just a message");
			logger.debug("a message with {} {}", "two", "parameters");
			logger.debug("a message with a throwable", new Exception());
		}

		// info, warn and error
		logger.log(OrderMgrMessage.JOBCOUNT_RETRIEVED, "01");
		logger.log(OrderMgrMessage.JOB_NOT_EXIST, "n/a");

		// generate error header
		HttpHeaders header = http.errorHeaders(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, "a", "b"));
		System.out.println(header);
		
		// extract message from error header
		// Change method parameter to HttpHeaders?
		String extractedMessage = http.extractProseoMessage(header.toString());
		System.out.println(extractedMessage);
	}

}
