/**
 * EdipStatusControllerImpl.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.edipmon.rest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.api.edipmon.EdipMonitor;
import de.dlr.proseo.api.edipmon.EdipMonitorConfiguration;
import de.dlr.proseo.api.edipmon.rest.model.RestInterfaceStatus;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ApiMonitorMessage;

/**
 * Spring MVC controller for the prosEO EDIP Monitor; implements the services required to inquire about the interface status
 *
 * @author Dr. Thomas Bassler
 */
@Component
public class EdipStatusControllerImpl implements StatusController {

	/** The EDIP Monitor configuration to use */
	@Autowired
	private EdipMonitorConfiguration config;

	/** The EDIP Monitor to use */
	@Autowired
	private EdipMonitor monitor;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(EdipStatusControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.EDIP_MONITOR);

	/**
	 * Get the interface status for the given EDIP Monitor
	 *
	 * @param edipid      the EDIP Monitor identifier
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return HTTP status "OK" and the Json representation of the interface status information, or HTTP status "FORBIDDEN" and an
	 *         error message, if an invalid EDIP Monitor identifier was passed
	 */
	@Override
	public ResponseEntity<RestInterfaceStatus> getRestInterfaceStatusByEdipid(String edipid, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getRestInterfaceStatusByEdipid({})", edipid);

		if (!config.getEdipId().equals(edipid)) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(ApiMonitorMessage.INVALID_EDIP_ID, edipid)),
					HttpStatus.FORBIDDEN);
		}

		RestInterfaceStatus restInterfaceStatus = new RestInterfaceStatus();
		restInterfaceStatus.setId(config.getEdipId());

		Path edipSatelliteDirectory = Paths.get(config.getEdipDirectoryPath()).resolve(config.getEdipSatellite());
		if (Files.isDirectory(edipSatelliteDirectory) && Files.isReadable(edipSatelliteDirectory)) {
			restInterfaceStatus.setAvailable(true);
			restInterfaceStatus.setPerformance(monitor.getLastCopyPerformance());
		} else {
			restInterfaceStatus.setAvailable(false);
			restInterfaceStatus.setPerformance(0.0);
		}

		return new ResponseEntity<>(restInterfaceStatus, HttpStatus.OK);
	}

}