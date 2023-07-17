/**
 * CadipStatusControllerImpl.java
 * 
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.cadipmon.rest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.api.cadipmon.CadipMonitor;
import de.dlr.proseo.api.cadipmon.CadipMonitorConfiguration;
import de.dlr.proseo.api.cadipmon.rest.model.RestInterfaceStatus;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ApiMonitorMessage;

/**
 * Spring MVC controller for the prosEO CADIP Monitor; implements the services required to inquire about the interface status
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class CadipStatusControllerImpl implements StatusController {

	/** The CADIP Monitor configuration to use */
	@Autowired
	private CadipMonitorConfiguration config;

	/** The CADIP Monitor to use */
	@Autowired
	private CadipMonitor monitor;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(CadipStatusControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.CADIP_MONITOR);
	
    /**
     * Get the interface status for the given CADIP Monitor
     * 
     * @param cadipid the CADIP Monitor identifier
     * @param httpHeaders the HTTP request headers (injected)
     * @return HTTP status "OK" and the Json representation of the interface status information, or
	 *         HTTP status "FORBIDDEN" and an error message, if an invalid CADIP Monitor identifier was passed 
     */
	@Override
	public ResponseEntity<RestInterfaceStatus> getRestInterfaceStatusByCadipid(String cadipid, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestInterfaceStatusByCadipid({})", cadipid);
		
		if (!config.getCadipId().equals(cadipid)) {
			return new ResponseEntity<>(
				http.errorHeaders(logger.log(ApiMonitorMessage.INVALID_CADIP_ID, cadipid)), HttpStatus.FORBIDDEN);
		}

		RestInterfaceStatus restInterfaceStatus = new RestInterfaceStatus();
		restInterfaceStatus.setId(config.getCadipId());
		
		if (monitor.checkStatus()) {
			restInterfaceStatus.setAvailable(true);
			restInterfaceStatus.setPerformance(monitor.getLastCopyPerformance());
		} else {
			restInterfaceStatus.setAvailable(false);
			restInterfaceStatus.setPerformance(0.0);
		}
		
		return new ResponseEntity<>(restInterfaceStatus, HttpStatus.OK);
	}

}
