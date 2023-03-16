/**
 * UserControllerImplTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.usermgr.UserManagerApplication;
import de.dlr.proseo.usermgr.dao.UserRepository;
import de.dlr.proseo.usermgr.model.Authority;
import de.dlr.proseo.usermgr.model.User;
import de.dlr.proseo.usermgr.rest.model.RestUser;

/**
 * Testing UserControllerImpl.class.
 *
 * TODO test invalid REST requests
 *
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = UserManagerApplication.class)
@AutoConfigureTestEntityManager
@Transactional
@WithMockUser(username = "UTM-testuser", roles = {"USERMGR"})
public class UserControllerImplTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(UserControllerImplTest.class);

	/** The WorkflowControllerImpl under test */
	@Autowired
	private UserControllerImpl gci;

	/** Repository for User objects */
	@Autowired
	UserRepository userRepository;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		logger.trace("... adding a test user to the database");

		User user = new User();
		user.setUsername("UTM-johndoe");
		user.setPassword("$2a$04$nXMQTg2ZMY6k8yDvL5jD2.lthiKrmWZpOVgyu0l7tbm.JKKzyRpQW");
		user.setEnabled(true);
		user.setExpirationDate(Date.from(Instant.now()));
		user.setExpirationDate(Date.from(Instant.now()));

		Authority authority = new Authority();
		authority.setAuthority("ROLE_USER");
		user.getAuthorities().add(authority);

		user = userRepository.save(user);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		userRepository.deleteAll();
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.UserControllerImpl#getUsers(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer)}.
	 */
	@Test
	public final void testGetUsers() {
		logger.trace(">>> testGetUsers()");

		ResponseEntity<List<RestUser>> response = gci.getUsers("UTM", null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.UserControllerImpl#createUser(de.dlr.proseo.usermgr.rest.model.RestUser)}.
	 */
	@Test
	public final void testCreateUser() {
		// TODO
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.UserControllerImpl#getUserById(java.lang.Long)}.
	 */
	@Test
	public final void testGetUserById() {
		// TODO
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.UserControllerImpl#deleteUserById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteUserById() {
		// TODO
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.UserControllerImpl#modifyUser(java.lang.Long, de.dlr.proseo.usermgr.rest.model.RestUser)}.
	 */
	@Test
	public final void testModifyUser() {
		// TODO
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.UserControllerImpl#getUserMembers(java.lang.Long)}.
	 */
	@Test
	public final void testGetUserMembers() {
		// TODO
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.UserControllerImpl#addUserMember(java.lang.Long, java.lang.String)}.
	 */
	@Test
	public final void testAddUserMember() {
		// TODO
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.UserControllerImpl#removeUserMember(java.lang.Long, java.lang.String)}.
	 */
	@Test
	public final void testRemoveUserMember() {
		// TODO
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.UserControllerImpl#countUsers(java.lang.String)}.
	 */
	@Test
	public final void testCountUsers() {
		logger.trace(">>> testCountUsers()");
		
		ResponseEntity<String> response = gci.countUsers("UTM");
		
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertEquals("Wrong number of users retrieved: ", "1", response.getBody());
	}

}
