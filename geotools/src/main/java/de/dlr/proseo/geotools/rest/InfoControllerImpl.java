package de.dlr.proseo.geotools.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.geotools.GeotoolsConfiguration;

// TODO Add file, class and method comments

@Component
public class InfoControllerImpl implements InfoController {
	
	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(InfoControllerImpl.class);

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
	