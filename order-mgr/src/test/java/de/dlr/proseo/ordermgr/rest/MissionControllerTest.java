/**
 * MissionControllerTest.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.transaction.Transactional;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.OrderManager;
import de.dlr.proseo.ordermgr.rest.model.MissionUtil;

/**
 * Testing MissionControllerImpl.class.
 *
 * TODO test invalid REST requests
 *
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = OrderManager.class)
@WithMockUser(username = "UTM-testuser", roles = {})
@AutoConfigureTestEntityManager
@Transactional
public class MissionControllerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(MissionControllerTest.class);

	/** The MissionControllerImpl under test */
	@Autowired
	private MissionControllerImpl mci;

	// Test data
	private static String[][] testMissionData = {
			// id, version, code, name, processing_mode, file_class,
			// product_file_template
			{ "1", "0", "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" },
			{ "2", "0", "PTM", "EFGH Testing", "NRTI", "OPER", "test_file_temp" },
			{ "3", "0", "XTM", "IJKL Testing", "NRTI", "OPER", "test_file_temp" } };
	private static String[] testSpacecraftData =
			// version, code,name,
			{ "1", "S_TDX1", "Tandom-X" };

	/** Database transaction manager */
	@Autowired
	private PlatformTransactionManager txManager;

	/**
	 *
	 * Create test missions and test spacecrafts in the database.
	 *
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		logger.trace(">>> Starting to create test data in the database");
		createMissionAndSpacecraft(testMissionData[0], testSpacecraftData);
		createMissionAndSpacecraft(testMissionData[1], testSpacecraftData);
		logger.trace("<<< Finished creating test data in the database");
	}

	/**
	 *
	 * Deleting test data from the database
	 *
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		logger.trace(">>> Starting to delete test data in database");
		RepositoryService.getSpacecraftRepository().deleteAll();
		RepositoryService.getMissionRepository().deleteAll();
		logger.trace("<<< Finished deleting test data in database");
	}

	/**
	 * Create a test mission and a test spacecraft in the database
	 *
	 * @param missionData
	 *            The data from which to create the mission
	 * @param spacecraftData
	 *            The data from which to create the spacecraft
	 */
	private static void createMissionAndSpacecraft(String[] missionData, String[] spacecraftData) {
		if (null != RepositoryService.getMissionRepository().findByCode(missionData[2])) {
			return;
		}

		Mission testMission = new Mission();
		Spacecraft testSpacecraft = new Spacecraft();

		logger.trace("... creating mission {}", missionData[2]);

		// adding mission parameters
		testMission.setCode(missionData[2]);
		testMission.setName(missionData[3]);
		testMission.getProcessingModes().clear();
		testMission.getProcessingModes().add(missionData[4]);
		testMission.getFileClasses().clear();
		testMission.getFileClasses().add(missionData[5]);
		testMission.setProductFileTemplate(missionData[6]);
		testMission = RepositoryService.getMissionRepository().save(testMission);

		logger.trace("... creating spacecraft {}", spacecraftData[2]);

		// adding spacecraft parameters
		testSpacecraft.setMission(testMission);
		testSpacecraft.setCode(spacecraftData[1]);
		testSpacecraft.setName(spacecraftData[2]);
		testSpacecraft = RepositoryService.getSpacecraftRepository().save(testSpacecraft);

		testMission.getSpacecrafts().clear();
		testMission.getSpacecrafts().add(testSpacecraft);

		RepositoryService.getMissionRepository().save(testMission);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.MissionControllerImpl#getMissions(java.lang.String)}.
	 */
	@Test
	public final void testGetMissions() {
		logger.trace(">>> testGetMissions()");

		// Get missions with MissionControllerImpl
		ResponseEntity<List<RestMission>> entity = mci.getMissions("UTM");
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());

		// Assert one mission with code UTM was retrieved
		RepositoryService.getMissionRepository().findByCode("UTM");
		assertTrue("More or less missions than expected were found.", entity.getBody().size() == 1);
		assertTrue("An unexpected mission was found.", "UTM" == entity.getBody().get(0).getCode());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.MissionControllerImpl#createMission(de.dlr.proseo.model.rest.model.RestMission)}.
	 */
	@Test
	public final void testCreateMission() {
		logger.trace(">>> testCreateMissions()");

		List<Mission> beforeCreation = RepositoryService.getMissionRepository().findAll();

		// Create a mission with MissionControllerImpl
		RestMission mission = new RestMission();
		mission.setCode(testMissionData[2][2]);
		mission.setName(testMissionData[2][3]);
		mission.getProcessingModes().add(testMissionData[2][4]);
		mission.getFileClasses().add(testMissionData[2][5]);
		mission.setProductFileTemplate(testMissionData[2][6]);

		ResponseEntity<RestMission> createdMission = mci.createMission(mission);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, createdMission.getStatusCode());

		// Assert that the mission was created
		List<Mission> afterCreation = RepositoryService.getMissionRepository().findAll();
		assertTrue("Mission repository does not contain more missions after creation.",
				afterCreation.size() > beforeCreation.size());
		assertTrue("Created mission is not present in repository.",
				null != RepositoryService.getMissionRepository().findByCode(createdMission.getBody().getCode()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.MissionControllerImpl#getMissionById(java.lang.Long)}.
	 */
	@Test
	public final void testGetMissionById() {
		logger.trace(">>> testGetMissionsById()");

		// Retrieve a mission directly from the database
		Mission missionToRetrieve = RepositoryService.getMissionRepository().findAll().get(0);

		// Retrieve a mission by MissionControllerImpl
		ResponseEntity<RestMission> retrievedMission = mci.getMissionById(missionToRetrieve.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedMission.getStatusCode());

		// Assert that expected equals retrieved mission
		assertEquals("Wrong mission retrieved: ", missionToRetrieve.getCode(), retrievedMission.getBody().getCode());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.MissionControllerImpl#modifyMission(java.lang.Long, de.dlr.proseo.model.rest.model.RestMission)}.
	 */
	@Test
	public final void testModifyMission() {
		logger.trace(">>> testModifyMissions()");

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		transactionTemplate.execute(status -> {
			// Retrieve and modify a mission from the database
			List<Mission> beforeModification = RepositoryService.getMissionRepository().findAll();
			RestMission missionToModify = MissionUtil.toRestMission(beforeModification.get(0));
			missionToModify.setName("MNOP Testing");

			// Modify a mission with MissionControllerImpl
			ResponseEntity<RestMission> entity = mci.modifyMission(missionToModify.getId(), missionToModify);
			assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());

			// Assert that the modification had the expected effect
			assertEquals("Mission was not modified as expected.", MissionUtil.toModelMission(missionToModify),
					RepositoryService.getMissionRepository().findByCode(missionToModify.getCode()));

			return true;
		});

	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.MissionControllerImpl#deleteMissionById(java.lang.Long, java.lang.Boolean, java.lang.Boolean)}.
	 */
	@Test
	public final void testDeleteMissionById() {
		logger.trace(">>> testDeleteMissions()");

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		transactionTemplate.execute(status -> {
			List<Mission> beforeDeletion = RepositoryService.getMissionRepository().findAll();
			Mission missionToDelete = beforeDeletion.get(0);

			// Delete spacecraft referencing mission
			missionToDelete.getSpacecrafts()
					.forEach(spacecraft -> RepositoryService.getSpacecraftRepository().deleteById(spacecraft.getId()));

			// Delete a mission with MissionControllerImpl
			mci.deleteMissionById(missionToDelete.getId(), false, false);

			// Assert that the mission was deleted
			List<Mission> afterDeletion = RepositoryService.getMissionRepository().findAll();
			assertTrue("After deletion, repository does not contain less missions.", afterDeletion.size() < beforeDeletion.size());
			assertFalse("Deleted mission is still in the repository.", afterDeletion.contains(missionToDelete));

			return true;
		});

	}

}
