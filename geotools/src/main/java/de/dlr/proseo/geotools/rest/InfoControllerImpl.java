package de.dlr.proseo.geotools.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.geotools.GeotoolsConfiguration;
import de.dlr.proseo.logging.logger.ProseoLogger;

// TODO Add file, class and method comments

@Component
public class InfoControllerImpl implements InfoController {
	
	/**
	 * Logger of this class
	 */
	private static ProseoLogger logger = new ProseoLogger(InfoControllerImpl.class);

	/** Geotools configuration */
	@Autowired
	private GeotoolsConfiguration geotoolsConfig;

	@Autowired
	private GeotoolsUtil geotools;
	
	@Override
	public ResponseEntity<?> getInfo() {
		// TODO Auto-generated method stub
		return new ResponseEntity<>(geotools.getInfo(), HttpStatus.OK);
	}
}
	