/**
 * AclobjectControllerImpl.java
 */
package de.dlr.proseo.usermgr.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.usermgr.rest.model.RestAclObject;

/**
 * @author thomas
 *
 */
@Component
public class AclobjectControllerImpl implements AclobjectController {

	@Override
	public ResponseEntity<List<RestAclObject>> getAclObjects() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestAclObject> createAclObject(@Valid RestAclObject restAclObject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestAclObject> getAclObjectById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteAclObjectById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestAclObject> modifyAclObject(Long id, RestAclObject restAclObject) {
		// TODO Auto-generated method stub
		return null;
	}

}
