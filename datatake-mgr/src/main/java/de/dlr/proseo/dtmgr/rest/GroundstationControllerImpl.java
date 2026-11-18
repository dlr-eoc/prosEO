/**
 * GroundstationControllerImpl.java
 */
package de.dlr.proseo.dtmgr.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.dtmgr.rest.model.RestGroundStation;

/**
 * 
 */
@Component
public class GroundstationControllerImpl implements GroundstationController {

	@Override
	public ResponseEntity<List<RestGroundStation>> getGroundStations(String stationId, String stationName, Long recordFrom,
			Long recordTo, String[] orderBy) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestGroundStation> createGroundStation(RestGroundStation restGroundStation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> countGroundStations(String stationId, String stationName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestGroundStation> getGroundStationById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteGroundStationById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestGroundStation> modifyGroundStation(Long id, RestGroundStation restGroundStation) {
		// TODO Auto-generated method stub
		return null;
	}

}
