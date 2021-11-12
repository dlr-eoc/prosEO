/**
 * XbipStatusControllerImpl.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.xbipmon.rest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.api.xbipmon.XbipMonitor;
import de.dlr.proseo.api.xbipmon.XbipMonitorConfiguration;
import de.dlr.proseo.api.xbipmon.rest.model.RestInterfaceStatus;

/**
 * Spring MVC controller for the prosEO XBIP Monitor; implements the services required to inquire about the interface status
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class XbipStatusControllerImpl implements StatusController {

	/* Message ID constants */
	private static final int MSG_ID_INVALID_XBIP_ID = 2051;

	/* Message string constants */
	private static final String MSG_INVALID_XBIP_ID = "(E%d) Invalid XBIP Monitor identifier %s passed";
	
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-ingestor ";

	
	/** The XBIP Monitor configuration to use */
	@Autowired
	private XbipMonitorConfiguration config;

	/** The XBIP Monitor to use */
	@Autowired
	private XbipMonitor monitor;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(XbipStatusControllerImpl.class);
	
	/**
	 * Create and log a formatted message at the given level
	 * 
	 * @param level the logging level to use
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private String log(Level level, String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		if (Level.ERROR.equals(level)) {
			logger.error(message);
		} else if (Level.WARN.equals(level)) {
			logger.warn(message);
		} else {
			logger.info(message);
		}

		return message;
	}

	/**
	 * Create and log a formatted error message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted error message
	 */
	private String logError(String messageFormat, int messageId, Object... messageParameters) {
		return log(Level.ERROR, messageFormat, messageId, messageParameters);
	}
	
	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + (null == message ? "null" : message.replaceAll("\n", " ")));
		return responseHeaders;
	}
	
    /**
     * Get the interface status for the given XBIP Monitor
     * 
     * @param xbipid the XBIP Monitor identifier
     * @param httpHeaders the HTTP request headers (injected)
     * @return HTTP status "OK" and the Json representation of the interface status information, or
	 *         HTTP status "FORBIDDEN" and an error message, if an invalid XBIP Monitor identifier was passed 
     */
	@Override
	public ResponseEntity<RestInterfaceStatus> getRestInterfaceStatusByXbipid(String xbipid, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestInterfaceStatusByXbipid({})", xbipid);
		
		if (!config.getXbipId().equals(xbipid)) {
			return new ResponseEntity<>(errorHeaders(logError(MSG_INVALID_XBIP_ID, MSG_ID_INVALID_XBIP_ID, xbipid)), HttpStatus.FORBIDDEN);
		}

		RestInterfaceStatus restInterfaceStatus = new RestInterfaceStatus();
		restInterfaceStatus.setId(config.getXbipId());
		
		Path xbipSatelliteDirectory = Paths.get(config.getXbipDirectoryPath()).resolve(config.getXbipSatellite());
		if (Files.isDirectory(xbipSatelliteDirectory) && Files.isReadable(xbipSatelliteDirectory)) {
			restInterfaceStatus.setAvailable(true);
			restInterfaceStatus.setPerformance(monitor.getLastCopyPerformance());
		} else {
			restInterfaceStatus.setAvailable(false);
			restInterfaceStatus.setPerformance(0.0);
		}
		
		return new ResponseEntity<>(restInterfaceStatus, HttpStatus.OK);
	}

}
