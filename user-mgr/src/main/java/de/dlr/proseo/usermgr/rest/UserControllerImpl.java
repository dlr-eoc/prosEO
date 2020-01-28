/**
 * UserControllerImpl.java
 */
package de.dlr.proseo.usermgr.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.usermgr.rest.model.RestUser;

/**
 * @author thomas
 *
 */
@Component
public class UserControllerImpl implements UserController {

	@Override
	public ResponseEntity<List<RestUser>> getUsers(String mission) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestUser> createUser(@Valid RestUser restUser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestUser> getUserByName(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteUserByName(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<RestUser> modifyUser(String username, RestUser restUser) {
		// TODO Auto-generated method stub
		return null;
	}

}
