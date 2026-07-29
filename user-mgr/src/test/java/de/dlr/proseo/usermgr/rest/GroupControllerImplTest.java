/**
 * UserControllerImplTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@SpringBootTest(classes = UserManagerApplication.class)
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
	@BeforeEach
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
	@AfterEach
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
		assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Wrong HTTP status: ");

		List<RestUser> updatedMembers = response.getBody();
		assertNotNull(updatedMembers, "Updated group members should not be null");
		assertTrue(updatedMembers.size() > 0, "There should be at least one group member");
		assertTrue(updatedMembers.stream().anyMatch(user -> "UTM-johndoe".equals(user.getUsername())),
				"New user should be a group member");
	}

	/**
	 * Test method for {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#countGroups(java.lang.String)}.
	 */
	@Test
	public final void testCountGroups() {
		logger.trace(">>> testCountGroups()");

		ResponseEntity<String> response = gci.countGroups("UTM");
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(groupRepository.count() + "", response.getBody(), "Wrong number of groups retrieved: ");
	}

	@Test
	public final void testCreateGroup() {
		logger.trace(">>> testCreateGroup()");

		RestGroup restGroup = new RestGroup();
		restGroup.setGroupname("NewGroup");

		ResponseEntity<RestGroup> response = gci.createGroup(restGroup);
		assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Wrong HTTP status: ");

		RestGroup createdGroup = response.getBody();
		assertNotNull(createdGroup, "Created group should not be null");
		assertEquals("NewGroup", createdGroup.getGroupname(), "Groupname should match");
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
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), "Wrong HTTP status: ");
	}

	@Test
	public final void testGetGroupById() {
		logger.trace(">>> testGetGroupById()");

		Long id = groupRepository.findAll().get(0).getId();

		ResponseEntity<RestGroup> response = gci.getGroupById(id);
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");

		RestGroup group = response.getBody();
		assertNotNull(group, "Retrieved group should not be null");
		assertEquals(id, group.getId(), "Id should match: ");
	}

	@Test
	public final void testGetGroupMembers() {
		logger.trace(">>> testGetGroupMembers()");

		ResponseEntity<List<RestUser>> response = gci.getGroupMembers(groupRepository.findByGroupName("UTM-testname").getId());
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");

		List<RestUser> members = response.getBody();
		assertNotNull(members, "Group members should not be null");
		assertTrue(members.size() > 0, "There should be at least one group member");

		groupRepository.findByGroupName("UTM-testname").getGroupMembers().forEach(member -> {
			member.getGroup().getGroupMembers().clear();
			member.getUser().getGroupMemberships().clear();
			groupMemberRepository.deleteById(member.getId());
		});

		response = gci.getGroupMembers(groupRepository.findByGroupName("UTM-testname").getId());
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Wrong HTTP status: ");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#getGroups(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer)}.
	 */
	@Test
	public final void testGetGroups() {
		logger.trace(">>> testGetGroups()");

		ResponseEntity<List<RestGroup>> response = gci.getGroups("UTM", null, null, null);
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");

		response = gci.getGroups("UTM", "UTM-testname", null, null);
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");
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
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");

		RestGroup modifiedGroup = response.getBody();
		assertNotNull(modifiedGroup, "Modified group should not be null");
		// No name change allowed
		assertEquals("UTM-testname", modifiedGroup.getGroupname(), "Groupname should match");

		// Change authorities
		restGroup.setGroupname("UTM-testname");
		restGroup.getAuthorities().add("ROLE_ORDER_MGR");
		response = gci.modifyGroup(groupRepository.findByGroupName("UTM-testname").getId(), restGroup);
		assertEquals(HttpStatus.OK, response.getStatusCode(), "Wrong HTTP status: ");

		modifiedGroup = response.getBody();
		assertNotNull(modifiedGroup, "Modified group should not be null");
		assertTrue(modifiedGroup.getAuthorities().contains("ROLE_ORDER_MGR"), "New authority should be included");

		// Add illegal authority
		restGroup.setGroupname("UTM-testname");
		restGroup.getAuthorities().add("ANY");
		response = gci.modifyGroup(groupRepository.findByGroupName("UTM-testname").getId(), restGroup);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Wrong HTTP status: ");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.usermgr.rest.GroupControllerImpl#removeGroupMember(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Integer)}.
	 */
	@Test
	public final void testRemoveGroupMember() {
		logger.trace(">>> testRemoveGroupMember()");

		ResponseEntity<List<RestUser>> response = gci.removeGroupMember(groupRepository.findAll().get(0).getId(), "UTM-janedoe");
		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), "Wrong HTTP status: ");
	}
}