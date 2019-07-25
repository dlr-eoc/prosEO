/**
 * MissionControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.util.List;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.ordermgr.rest.model.Mission;

/**
 * Spring MVC controller for the prosEO Order Manager; implements the services required to manage missions
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class MissionControllerImpl implements MissionController {
	
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-order-mgr ";

	private static Logger logger = LoggerFactory.getLogger(MissionControllerImpl.class);

	@Override
	public ResponseEntity<List<Mission>> getMissions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<Mission> createMission(@Valid Mission mission) {
		// TODO Auto-generated method stub

		String message = String.format(MSG_PREFIX + "POST not implemented (%d)", 2001);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<Mission> getMissionById(Long id) {
		// TODO Auto-generated method stub

		String message = String.format(MSG_PREFIX + "GET for id %s not implemented (%d)", id, 2000);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<Mission> updateMission(Long id, @Valid Mission mission) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteMissionById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

}
