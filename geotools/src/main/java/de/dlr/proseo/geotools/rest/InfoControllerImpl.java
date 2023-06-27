/**
 * InfoControllerImpl.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.geotools.rest;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * Controller to handle the request for information on the regions provided in
 * application.yml.
 *
 * @author Ernst Melchinger
 */
@Component
public class InfoControllerImpl implements InfoController {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(InfoControllerImpl.class);

	/** HTTP service methods */
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.GEOTOOLS);

	/** The geotools utilities */
	@Autowired
	private GeotoolsUtil geotools;

	/**
	 * Return information on the known regions.
	 *
	 * @return HttpStatus 200 and the requested information OR HttpStatus 400 and an
	 *         error message if the input was invalid OR HttpStatus 500 and an error
	 *         message if the input was valid but the implementation is pending or a
	 *         problem occurred while trying to process a shape file
	 */
	@Override
	public ResponseEntity<?> getInfo() {
		try {
			String info = geotools.getInfo();
			return new ResponseEntity<>(info, HttpStatus.OK);
		} catch (UnsupportedOperationException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (IOException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getClass().getName() + " / " + e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
