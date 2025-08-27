/**
 * UserControllerImplTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import de.dlr.proseo.usermgr.rest.model.RestUser;

/**
 * Testing UserControllerImpl.class.
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

	/** The GroupControllerImpl under test */
	@Autowired
	private GroupControllerImpl gci;

	/** Repository for group members */
	@Autowired
	GroupMemberRepository groupMemberRepository;

	/** Repository for User group objects */
	@Autowired
	GroupRepository groupRepository;

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
		user.setUsername("UTM-janedoe");
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
		// Nothing to do, test data will be deleted by automatic rollback of test transaction
	}

	@Test
	public final void testAddGroupMember() {
		logger.trace(">>> testAddGroupMember()");

		User newUser = new User();
		newUser.setUsername("UTM-johndoe");
		newUser.setPassword("$2a$04$nXMQTg2ZMY6k8yDvL5jD2.lthiKrmWZpOVgyu0l7tbm.JKKzyRpQW");
		newUser.setEnabled(true);
		newUser.setExpirationDate(Date.from(Instant.now()));
		newUser.setExpirationDate(Date.from(Instant.now()));

		Authority authority = new Authority();
		authority.setAuthority("ROLE_USER");
		newUser.getAuthorities().add(authority);

		newUser = userRepository.save(newUser);

		ResponseEntity<List<RestUser>> response = gci.addGroupMember(groupRepository.findAll().get(0).getId(), "UTM-johndoe");
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, response.getStatusCode());

		List<RestUser> updatedMembers = response.getBody();
		assertNotNull("Updated group members should not be null", updatedMembers);
		assertTrue("There should be at least one group member", updatedMembers.size() > 0);
		assertTrue("New user should be a group member",
				updatedMembers.stream().anyMatch(user -> "UTM-johndoe".equals(user.getUsername())));
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

	@Test
	public final void testCreateGroup() {
		logger.trace(">>> testCreateGroup()");

		RestGroup restGroup = new RestGroup();
		restGroup.setGroupname("NewGroup");

		ResponseEntity<RestGroup> response = gci.createGroup(restGroup);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, response.getStatusCode());

		RestGroup createdGroup = response.getBody();
		assertNotNull("Created group should not be null", createdGroup);
		assertEquals("Groupname should match", "NewGroup", createdGroup.getGroupname());
	}

	@Test
	public final void testDeleteGroupById() {
		logger.trace(">>> testDeleteGroupById()");

		// Create new group with no users
		Group group = new Group();
		group.setGroupName("UTM-todelete");

		GroupAuthority groupAuthority = new GroupAuthority();
		groupAuthority.setAuthority("ROLE_USER");
		group.getGroupAuthorities().add(groupAuthority);

		group = groupRepository.save(group);

		ResponseEntity<?> response = gci.deleteGroupById(group.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	public final void testGetGroupById() {
		logger.trace(">>> testGetGroupById()");

		Long id = groupRepository.findAll().get(0).getId();

		ResponseEntity<RestGroup> response = gci.getGroupById(id);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());

		RestGroup group = response.getBody();
		assertNotNull("Retrieved group should not be null", group);
		assertEquals("Id should match: ", id, group.getId());
	}

	@Test
	public final void testGetGroupMembers() {
		logger.trace(">>> testGetGroupMembers()");

		ResponseEntity<List<RestUser>> response = gci.getGroupMembers(groupRepository.findByGroupName("UTM-testname").getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());

		List<RestUser> members = response.getBody();
		assertNotNull("Group members should not be null", members);
		assertTrue("There should be at least one group member", members.size() > 0);

		groupRepository.findByGroupName("UTM-testname").getGroupMembers().forEach(member -> {
			member.getGroup().getGroupMembers().clear();
			member.getUser().getGroupMemberships().clear();
			groupMemberRepository.deleteById(member.getId());
		});

		response = gci.getGroupMembers(groupRepository.findByGroupName("UTM-testname").getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#getGroups(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer)}.
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
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#ModifyGroup(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer)}.
	 */
	@Test
	public final void testModifyGroup() {
		logger.trace(">>> testModifyGroup()");

		// Attempt change in name (not allowed)
		RestGroup restGroup = new RestGroup();
		restGroup.setGroupname("ModifiedGroup");

		ResponseEntity<RestGroup> response = gci.modifyGroup(groupRepository.findByGroupName("UTM-testname").getId(), restGroup);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());

		RestGroup modifiedGroup = response.getBody();
		assertNotNull("Modified group should not be null", modifiedGroup);
		// No name change allowed
		assertEquals("Groupname should match", "UTM-testname", modifiedGroup.getGroupname());

		// Change authorities
		restGroup.setGroupname("UTM-testname");
		restGroup.getAuthorities().add("ROLE_ORDER_MGR");
		response = gci.modifyGroup(groupRepository.findByGroupName("UTM-testname").getId(), restGroup);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, response.getStatusCode());

		modifiedGroup = response.getBody();
		assertNotNull("Modified group should not be null", modifiedGroup);
		assertTrue("New authority should be included", modifiedGroup.getAuthorities().contains("ROLE_ORDER_MGR"));

		// Add illegal authority
		restGroup.setGroupname("UTM-testname");
		restGroup.getAuthorities().add("ANY");
		response = gci.modifyGroup(groupRepository.findByGroupName("UTM-testname").getId(), restGroup);
		assertEquals("Wrong HTTP status: ", HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#removeGroupMember(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer)}.
	 */
	@Test
	public final void testRemoveGroupMember() {
		logger.trace(">>> testRemoveGroupMember()");

		ResponseEntity<List<RestUser>> response = gci.removeGroupMember(groupRepository.findAll().get(0).getId(), "UTM-janedoe");
		assertEquals("Wrong HTTP status: ", HttpStatus.NO_CONTENT, response.getStatusCode());
	}

}