/**
 * UserControllerImplTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.usermgr.UserManagerApplication;
import de.dlr.proseo.usermgr.UsermgrSecurityConfig;
import de.dlr.proseo.usermgr.dao.UserRepository;
import de.dlr.proseo.usermgr.model.Authority;
import de.dlr.proseo.usermgr.model.User;

/**
 * Testing LoginControllerImpl.class.
 *
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = UserManagerApplication.class)
@AutoConfigureTestEntityManager
@Transactional
public class LoginControllerImplTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(UserControllerImplTest.class);

	/** Test password */
	private static final String TEST_PASSWORD = "$2a$04$nXMQTg2ZMY6k8yDvL5jD2.lthiKrmWZpOVgyu0l7tbm.JKKzyRpQW";

	/** The LoginControllerImpl under test */
	@Autowired
	private LoginControllerImpl loginController;

	/** The user manager security configuration */
	@Autowired
	UsermgrSecurityConfig securityConfig;

	/** Repository for User objects */
	@Autowired
	UserRepository userRepository;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		logger.trace("... adding test users to the database");

		Authority authority = new Authority();
		authority.setAuthority("ROLE_USER");

		Authority rootAuthority = new Authority();
		rootAuthority.setAuthority("ROLE_ROOT");

		User user1 = new User();
		user1.setUsername("UTM-janedoe");
		user1.setPassword(TEST_PASSWORD);
		user1.setEnabled(true);
		user1.getAuthorities().add(authority);
		user1 = userRepository.save(user1);

		User user2 = new User();
		user2.setUsername("UTM-janeroot");
		user2.setPassword(TEST_PASSWORD);
		user2.setEnabled(true);
		user2.getAuthorities().add(rootAuthority);
		user2 = userRepository.save(user2);

		User user3 = new User();
		user3.setUsername("PTM-expired");
		user3.setPassword(TEST_PASSWORD);
		user3.setEnabled(true);
		user3.setExpirationDate(new Date(System.currentTimeMillis() - 86400000L)); // Expired yesterday
		user3.getAuthorities().add(authority);
		user3 = userRepository.save(user3);

		User user4 = new User();
		user4.setUsername("PTM-expiredpw");
		user4.setPassword(TEST_PASSWORD);
		user4.setEnabled(true);
		user4.setPasswordExpirationDate(new Date(System.currentTimeMillis() - 86400000L)); // Expired yesterday
		user4.getAuthorities().add(authority);
		user4 = userRepository.save(user4);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		logger.trace("... clearing test data from the database");

		userRepository.deleteAll();
	}

	@Test
	public void testLoginCrossMissionAccess() {
		// Mock the user details with Spring security UserBuilder
		UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("UTM-janedoe")
			.password(TEST_PASSWORD)
			.roles("ORDER_MGR")
			.build();

		// Set up the authentication
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, TEST_PASSWORD,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_ORDER_MGR")));
		SecurityContextHolder.getContext().setAuthentication(auth);

		// Test the login
		ResponseEntity<List<String>> response = loginController.login("PTM");
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	public void testLoginMissingMission() {
		// Mock the user details with Spring security UserBuilder
		UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("UTM-janedoe")
			.password(TEST_PASSWORD)
			.roles("ORDER_MGR")
			.build();

		// Set up the authentication
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, TEST_PASSWORD,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_ORDER_MGR")));
		SecurityContextHolder.getContext().setAuthentication(auth);

		// Test the login
		ResponseEntity<List<String>> response = loginController.login("");
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	public void testLoginNonRootUserWithValidMission() {
		// Mock the user details with Spring security UserBuilder
		UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("UTM-janedoe")
			.password(TEST_PASSWORD)
			.roles("ORDER_MGR")
			.build();

		// Set up the authentication
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, TEST_PASSWORD,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_ORDER_MGR")));
		SecurityContextHolder.getContext().setAuthentication(auth);

		// Test the login
		ResponseEntity<List<String>> response = loginController.login("UTM");
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1, response.getBody().size());
		assertTrue(response.getBody().contains("ROLE_ORDER_MGR"));
	}

	@Test
	public void testLoginRootUser() {
		// Mock the user details with Spring security UserBuilder
		UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("UTM-janeroot")
			.password(TEST_PASSWORD)
			.roles("ORDER_MGR")
			.build();

		// Set up the authentication
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, TEST_PASSWORD,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_ROOT")));
		SecurityContextHolder.getContext().setAuthentication(auth);

		// Test the login
		ResponseEntity<List<String>> response = loginController.login("");
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1, response.getBody().size());
		assertTrue(response.getBody().contains("ROLE_ROOT"));
	}

	@Test
	public void testLoginUnauthorizedUser() {
		// Mock the user details with Spring security UserBuilder
		UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("UTM-unauthorized")
			.password(TEST_PASSWORD)
			.roles("ORDER_MGR")
			.build();

		// Set up the authentication
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, TEST_PASSWORD,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_ORDER_MGR")));
		SecurityContextHolder.getContext().setAuthentication(auth);

		// Test the login
		assertThrows(NoResultException.class, () -> loginController.login("UTM"));
	}

	@Test
	public void testLoginUserWithExpiredAccount() {
		// Mock the user details with Spring security UserBuilder
		UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("PTM-expired")
			.password(TEST_PASSWORD)
			.roles("ORDER_MGR")
			.build();

		// Set up the authentication
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, TEST_PASSWORD,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_ORDER_MGR")));
		SecurityContextHolder.getContext().setAuthentication(auth);

		// Test the login
		ResponseEntity<List<String>> response = loginController.login("PTM");
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	public void testLoginUserWithExpiredPassword() {
		// Mock the user details with Spring security UserBuilder
		UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("PTM-expiredpw")
			.password(TEST_PASSWORD)
			.roles("ORDER_MGR")
			.build();

		// Set up the authentication
		Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, TEST_PASSWORD,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_ORDER_MGR")));
		SecurityContextHolder.getContext().setAuthentication(auth);

		// Test the login
		ResponseEntity<List<String>> response = loginController.login("PTM");
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
	}

}