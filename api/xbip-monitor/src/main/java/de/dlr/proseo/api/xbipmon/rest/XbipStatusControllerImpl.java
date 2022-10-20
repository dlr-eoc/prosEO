/**
 * XbipStatusControllerImpl.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.xbipmon.rest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.api.xbipmon.XbipMonitor;
import de.dlr.proseo.api.xbipmon.XbipMonitorConfiguration;
import de.dlr.proseo.api.xbipmon.rest.model.RestInterfaceStatus;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ApiMonitorMessage;

/**
 * Spring MVC controller for the prosEO XBIP Monitor; implements the services required to inquire about the interface status
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class XbipStatusControllerImpl implements StatusController {

	/** The XBIP Monitor configuration to use */
	@Autowired
	private XbipMonitorConfiguration config;

	/** The XBIP Monitor to use */
	@Autowired
	private XbipMonitor monitor;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(XbipStatusControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.XBIP_MONITOR);
	
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
			return new ResponseEntity<>(
					http.errorHeaders(logger.log(ApiMonitorMessage.INVALID_XBIP_ID, xbipid)), HttpStatus.FORBIDDEN);
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
