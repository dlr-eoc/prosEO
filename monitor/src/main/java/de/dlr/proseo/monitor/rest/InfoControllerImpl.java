package de.dlr.proseo.monitor.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.rest.InfoController;

@Component
public class InfoControllerImpl implements InfoController {
	
	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(InfoControllerImpl.class);

	@Override
	public ResponseEntity<?> getInfo() {
		// TODO Auto-generated method stub
		return null;
	}
}
	