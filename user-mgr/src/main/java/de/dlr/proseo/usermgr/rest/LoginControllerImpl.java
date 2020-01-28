/**
 * LoginControllerImpl.java
 */
package de.dlr.proseo.usermgr.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.usermgr.rest.model.RestAuthority;

/**
 * @author thomas
 *
 */
@Component
public class LoginControllerImpl implements LoginController {

	@Override
	public ResponseEntity<List<RestAuthority>> login(String mission) {
		// TODO Auto-generated method stub
		return null;
	}

}
