/**
 * AuxipStatusControllerImpl.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.auxipmon.rest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.api.auxipmon.AuxipMonitor;
import de.dlr.proseo.api.auxipmon.AuxipMonitorConfiguration;
import de.dlr.proseo.api.auxipmon.rest.model.RestInterfaceStatus;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ApiMonitorMessage;

/**
 * Spring MVC controller for the prosEO AUXIP Monitor; implements the services required to inquire about the interface status
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class AuxipStatusControllerImpl implements StatusController {

	/** The AUXIP Monitor configuration to use */
	@Autowired
	private AuxipMonitorConfiguration config;

	/** The AUXIP Monitor to use */
	@Autowired
	private AuxipMonitor monitor;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(AuxipStatusControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.AUXIP_MONITOR);
	
    /**
     * Get the interface status for the given AUXIP Monitor
     * 
     * @param auxipid the AUXIP Monitor identifier
     * @param httpHeaders the HTTP request headers (injected)
     * @return HTTP status "OK" and the Json representation of the interface status information, or
	 *         HTTP status "FORBIDDEN" and an error message, if an invalid AUXIP Monitor identifier was passed 
     */
	@Override
	public ResponseEntity<RestInterfaceStatus> getRestInterfaceStatusByAuxipid(String auxipid, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestInterfaceStatusByAuxipid({})", auxipid);
		
		if (!config.getAuxipId().equals(auxipid)) {
			return new ResponseEntity<>(
				http.errorHeaders(logger.log(ApiMonitorMessage.INVALID_AUXIP_ID, auxipid)), HttpStatus.FORBIDDEN);
		}

		RestInterfaceStatus restInterfaceStatus = new RestInterfaceStatus();
		restInterfaceStatus.setId(config.getAuxipId());
		
		Path auxipSatelliteDirectory = Paths.get(config.getAuxipBaseUri()).resolve(config.getAuxipContext());
		if (Files.isDirectory(auxipSatelliteDirectory) && Files.isReadable(auxipSatelliteDirectory)) {
			restInterfaceStatus.setAvailable(true);
			restInterfaceStatus.setPerformance(monitor.getLastCopyPerformance());
		} else {
			restInterfaceStatus.setAvailable(false);
			restInterfaceStatus.setPerformance(0.0);
		}
		
		return new ResponseEntity<>(restInterfaceStatus, HttpStatus.OK);
	}

}
