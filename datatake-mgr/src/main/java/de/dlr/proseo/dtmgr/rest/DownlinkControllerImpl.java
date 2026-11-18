/**
 * DownlinkControllerImpl.java
 */
package de.dlr.proseo.dtmgr.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.dtmgr.rest.model.RestDatatakeGroup;
import de.dlr.proseo.dtmgr.rest.model.RestDownlink;

/**
 * 
 */
@Component
public class DownlinkControllerImpl implements DownlinkController {

	@Override
	public ResponseEntity<List<RestDownlink>> getDownlinks(String spacecraftCode, String stationId, String downlinkId,
			String startTimeFrom, String startTimeTo, Long recordFrom, Long recordTo, String[] orderBy) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestDownlink> createDownlink(RestDownlink restDownlink) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> countDownlinks(String spacecraftCode, String stationId, String downlinkId, String startTimeFrom,
			String startTimeTo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestDatatakeGroup> getDownlinkById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestDownlink> addDataSegment(Long id, String spacecraftCode, String payload, String datatakeId,
			Long segmentNumber) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteDownlinkById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestDownlink> modifyDownlink(Long id, RestDownlink restDownlink) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestDownlink> removeDataSegment(Long dtid, String dsid, Long id) {
		// TODO Auto-generated method stub
		return null;
	}

}
