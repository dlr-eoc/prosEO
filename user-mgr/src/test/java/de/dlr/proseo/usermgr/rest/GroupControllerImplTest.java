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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.usermgr.UserManagerApplication;
import de.dlr.proseo.usermgr.dao.GroupMemberRepository;
import de.dlr.proseo.usermgr.dao.GroupRepository;
import de.dlr.proseo.usermgr.dao.UserRepository;
import de.dlr.proseo.usermgr.model.Authority;
import de.dlr.proseo.usermgr.model.Group;
import de.dlr.proseo.usermgr.model.GroupAuthority;
import de.dlr.proseo.usermgr.model.GroupMember;
import de.dlr.proseo.usermgr.model.User;
import de.dlr.proseo.usermgr.rest.model.RestGroup;


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
public class GroupControllerImplTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GroupControllerImplTest.class);

	/** The WorkflowControllerImpl under test */
	@Autowired
	private GroupControllerImpl gci;

	/** Repository for User group objects */
	@Autowired
	GroupRepository groupRepository;

	/** Repository for User objects */
	@Autowired
	UserRepository userRepository;

	/** Repository for group members */
	@Autowired
	GroupMemberRepository groupMemberRepository;

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

		logger.trace("... adding a test group to the database");

		Group group = new Group();
		group.setGroupName("UTM-testname");

		GroupAuthority groupAuthority = new GroupAuthority();
		groupAuthority.setAuthority("ROLE_USER");
		group.getGroupAuthorities().add(groupAuthority);

		group = groupRepository.save(group);

		logger.trace("... adding a group member to the database");

		GroupMember member = new GroupMember();
		group.getGroupMembers().add(member);
		user.getGroupMemberships().add(member);

		member.setGroup(group);
		member.setUser(user);
		member = groupMemberRepository.save(member);

		userRepository.save(user);
		groupRepository.save(group);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		groupMemberRepository.deleteAll();
		groupRepository.deleteAll();
		userRepository.deleteAll();
	}

	/**
	 * Test method for {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#getGroups(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer)}.
	 */
	@Test
	public final void testGetGroups() {
		logger.trace(">>> testGetGroups()");

		ResponseEntity<List<RestGroup>> response = gci.getGroups("UTM", null, null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());

		response = gci.getGroups("UTM", "UTM-testname", null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
	}

	/**
	 * Test method for {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#createGroup(de.dlr.proseo.usermgr.rest.model.RestGroup)}.
	 */
	@Test
	public final void testCreateGroup() {
		// TODO
	}

	/**
	 * Test method for {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#getGroupById(java.lang.Long)}.
	 */
	@Test
	public final void testGetGroupById() {
		// TODO
	}

	/**
	 * Test method for {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#deleteGroupById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteGroupById() {
		// TODO
	}

	/**
	 * Test method for {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#modifyGroup(java.lang.Long, de.dlr.proseo.usermgr.rest.model.RestGroup)}.
	 */
	@Test
	public final void testModifyGroup() {
		// TODO
	}

	/**
	 * Test method for {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#getGroupMembers(java.lang.Long)}.
	 */
	@Test
	public final void testGetGroupMembers() {
		// TODO
	}

	/**
	 * Test method for {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#addGroupMember(java.lang.Long, java.lang.String)}.
	 */
	@Test
	public final void testAddGroupMember() {
		// TODO
	}

	/**
	 * Test method for {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#removeGroupMember(java.lang.Long, java.lang.String)}.
	 */
	@Test
	public final void testRemoveGroupMember() {
		// TODO
	}

	/**
	 * Test method for {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#countGroups(java.lang.String)}.
	 */
	@Test
	public final void testCountGroups() {
		logger.trace(">>> testCountGroups()");

		ResponseEntity<String> response = gci.countGroups("UTM");
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());
		assertEquals("Wrong number of groups retrieved: ", groupRepository.count() + "", response.getBody());
	}

}
