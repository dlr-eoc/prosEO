/**
 * DatasegmentControllerImpl.java
 */
package de.dlr.proseo.dtmgr.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.dtmgr.rest.model.RestDataSegment;
import de.dlr.proseo.dtmgr.rest.model.RestDatatake;

/**
 * 
 */
@Component
public class DatasegmentControllerImpl implements DatasegmentController {

	@Override
	public ResponseEntity<RestDatatake> addDataSegment(RestDataSegment restDataSegment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteDataSegmentById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestDatatake> modifyDataSegment(Long id, RestDataSegment restDataSegment) {
		// TODO Auto-generated method stub
		return null;
	}

}
