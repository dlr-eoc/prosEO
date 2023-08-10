/**
 * UserControllerImplTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import de.dlr.proseo.usermgr.rest.model.RestQuota;
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
@WithMockUser(username = "UTM-testuser", roles = { "USERMGR" })
public class UserControllerImplTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(UserControllerImplTest.class);

	/** Test password */
	private static final String TEST_PASSWORD = "$2a$04$nXMQTg2ZMY6k8yDvL5jD2.lthiKrmWZpOVgyu0l7tbm.JKKzyRpQW";

	/** The UserControllerImpl under test */
	@Autowired
	private UserControllerImpl uci;

	/** Repository for User objects */
	@Autowired
	UserRepository userRepository;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		logger.trace("... adding a test user to the database");

		Authority authority = new Authority();
		authority.setAuthority("ROLE_USER");

		User user1 = new User();
		user1.setUsername("UTM-janedoe");
		user1.setPassword(TEST_PASSWORD);
		user1.setEnabled(true);
		user1.setExpirationDate(Date.from(Instant.now()));
		user1.setExpirationDate(Date.from(Instant.now()));
		user1.getAuthorities().add(authority);
		user1 = userRepository.save(user1);

		User user2 = new User();
		user2.setUsername("UTM-johndoe");
		user2.setPassword(TEST_PASSWORD);
		user2.setEnabled(true);
		user2.setExpirationDate(Date.from(Instant.now()));
		user2.setExpirationDate(Date.from(Instant.now()));
		user2.getAuthorities().add(authority);
		user2 = userRepository.save(user2);

		User user3 = new User();
		user3.setUsername("PTM-janedoe");
		user3.setPassword(TEST_PASSWORD);
		user3.setEnabled(true);
		user3.setExpirationDate(Date.from(Instant.now()));
		user3.setExpirationDate(Date.from(Instant.now()));
		user3.getAuthorities().add(authority);
		user3 = userRepository.save(user3);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		logger.trace("... clearing test data from the database");

		userRepository.deleteAll();
	}

	/**
	 * Test method for {@link de.dlr.proseo.usermgr.rest.UserControllerImpl#countUsers(java.lang.String)}.
	 */
	@Test
	public final void testCountUsers() {
		logger.trace(">>> testCountUsers()");

		// Count users for a mission with known users
		ResponseEntity<String> response = uci.countUsers("UTM");

		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertNotNull("Count not returned: ", response.getBody());
		assertEquals("Wrong number of users retrieved: ", "2", response.getBody());

		// Count users for a mission with no known users
		response = uci.countUsers("NOT");

		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertNotNull("Count not returned: ", response.getBody());
		assertEquals("Wrong number of users retrieved: ", "0", response.getBody());
	}

	@Test
	public final void testCreateUser() {
		logger.trace(">>> testCreateUser()");

		// Create a new user
		RestUser newUser = new RestUser();
		newUser.setUsername("UTM-newuser");
		newUser.setPassword("password");
		newUser.setEnabled(true);

		ResponseEntity<RestUser> response = uci.createUser(newUser);

		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, response.getStatusCode());
		assertNotNull("User not returned: ", response.getBody());
		assertEquals("Unexpected username: ", "UTM-newuser", response.getBody().getUsername());
		assertNull("Password should not be returned: ", response.getBody().getPassword());

		// Attempt to create a new user with an existing name
		RestUser existingUser = new RestUser();
		existingUser.setUsername("UTM-newuser");
		existingUser.setPassword("password");
		existingUser.setEnabled(true);

		response = uci.createUser(existingUser);

		assertEquals("Wrong HTTP status: ", HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	public final void testDeleteUserById() {
		logger.trace(">>> testDeleteUserById()");

		ResponseEntity<?> response = uci.deleteUserByName("UTM-johndoe");

		assertEquals("Wrong HTTP status: ", HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public final void testGetUserByName() {
		logger.trace(">>> testGetUserById()");

		// Retrieve a known user
		ResponseEntity<RestUser> response = uci.getUserByName("UTM-janedoe");

		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertNotNull("User not returned: ", response.getBody());
		assertEquals("Unexpected username: ", "UTM-janedoe", response.getBody().getUsername());
		assertNull("Password should not be returned: ", response.getBody().getPassword());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.UserControllerImpl#getUsers(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer)}.
	 */
	@Test
	public final void testGetUsers() {
		logger.trace(">>> testGetUsers()");

		// Retrieve by mission code only
		ResponseEntity<List<RestUser>> response = uci.getUsers("UTM", null, null);

		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertEquals("Wrong number of results: ", 2, response.getBody().size());

		// Retrieve selected records
		response = uci.getUsers("UTM", 0, 1);

		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertEquals("Wrong number of results: ", 1, response.getBody().size());
	}

	@Test
	public final void testModifyUser() {
		logger.trace(">>> testModifyUser()");

		// Modify enabled status
		RestUser modifiedUser = new RestUser();
		modifiedUser.setUsername("UTM-johndoe");
		modifiedUser.setPassword(TEST_PASSWORD);
		modifiedUser.setEnabled(false);

		ResponseEntity<RestUser> response = uci.modifyUser("UTM-johndoe", modifiedUser);

		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertNotNull("User not returned: ", response.getBody());
		assertEquals("Unexpected username: ", "UTM-johndoe", response.getBody().getUsername());
		assertNull("Password should not be returned: ", response.getBody().getPassword());
		assertFalse("User should be disabled: ", response.getBody().getEnabled());

		// Modify name (as user manager, see test annotation), not allowed
		modifiedUser.setUsername("UTM-jonadoe");

		response = uci.modifyUser("UTM-johndoe", modifiedUser);

		assertEquals("Wrong HTTP status: ", HttpStatus.NOT_MODIFIED, response.getStatusCode());

		// Modify password
		modifiedUser.setUsername("UTM-johndoe");
		modifiedUser.setPassword("newPassword");
		;

		response = uci.modifyUser("UTM-johndoe", modifiedUser);

		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertNotNull("User not returned: ", response.getBody());
		assertEquals("Unexpected username: ", "UTM-johndoe", response.getBody().getUsername());
		assertNull("Password should not be returned: ", response.getBody().getPassword());
		assertFalse("User should be disabled: ", response.getBody().getEnabled());

//		// Incorrectly modify authority
//		modifiedUser.getAuthorities().add("WRONG");
//		
//		assertEquals("Wrong HTTP status: ", HttpStatus.BAD_REQUEST, response.getStatusCode());

		// Add quota
		modifiedUser.setQuota(new RestQuota(10l, 0l, Date.from(Instant.now())));

		response = uci.modifyUser("UTM-johndoe", modifiedUser);

		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertNotNull("User not returned: ", response.getBody());
		assertEquals("Unexpected quota: ", Long.valueOf(10), response.getBody().getQuota().getAssigned());
		assertEquals("Unexpected quota: ", Long.valueOf(0), response.getBody().getQuota().getUsed());
	}

}