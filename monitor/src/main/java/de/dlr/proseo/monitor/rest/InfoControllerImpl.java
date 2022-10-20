package de.dlr.proseo.monitor.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.logger.ProseoLogger;

@Component
public class InfoControllerImpl implements InfoController {
	
	/**
	 * Logger of this class
	 */
	private static ProseoLogger logger = new ProseoLogger(InfoControllerImpl.class);

	@Override
	public ResponseEntity<?> getInfo() {
		// TODO Auto-generated method stub
		return null;
	}
}
	