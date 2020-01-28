/**
 * GroupControllerImpl.java
 */
package de.dlr.proseo.usermgr.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.usermgr.rest.model.RestGroup;

/**
 * @author thomas
 *
 */
@Component
public class GroupControllerImpl implements GroupController {

	@Override
	public ResponseEntity<List<RestGroup>> getGroups(String mission) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestGroup> createGroup(@Valid RestGroup restGroup) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestGroup> getGroupById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteGroupById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestGroup> modifyGroup(Long id, RestGroup restGroup) {
		// TODO Auto-generated method stub
		return null;
	}

}
