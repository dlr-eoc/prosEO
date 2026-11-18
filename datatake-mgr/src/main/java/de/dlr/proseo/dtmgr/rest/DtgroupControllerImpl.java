/**
 * DtgroupControllerImpl.java
 */
package de.dlr.proseo.dtmgr.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.dtmgr.rest.model.RestDatatakeGroup;

/**
 * 
 */
@Component
public class DtgroupControllerImpl implements DtgroupController {

	@Override
	public ResponseEntity<List<RestDatatakeGroup>> getDatatakeGroups(String dtGroupIdentifier, String startTimeFrom,
			String startTimeTo, Long recordFrom, Long recordTo, String[] orderBy) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestDatatakeGroup> createDatatakeGroup(RestDatatakeGroup restDatatakeGroup) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> countDatatakeGroups(String dtGroupIdentifier, String startTimeFrom, String startTimeTo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestDatatakeGroup> getDatatakeGroupById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestDatatakeGroup> addDatatake(Long id, String spacecraftCode, String payload, String datatakeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteDatatakeGroupById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestDatatakeGroup> removeDatatake(Long dtid, Long id) {
		// TODO Auto-generated method stub
		return null;
	}

}
