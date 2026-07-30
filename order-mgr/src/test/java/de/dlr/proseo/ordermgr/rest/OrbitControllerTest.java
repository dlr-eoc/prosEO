/**
 * OrbitControllerTest.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.rest.model.RestOrbit;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.ordermgr.OrderManager;
import de.dlr.proseo.ordermgr.rest.model.OrbitUtil;

/**
 * Testing OrbitControllerImpl.class.
 *
 * @author Katharina Bassler
 */
@SpringBootTest(classes = OrderManager.class)
@WithMockUser(username = "UTM-testuser", roles = {})
@Transactional
public class OrbitControllerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrbitControllerTest.class);

	/** The OrbitControllerImpl under test */
	@Autowired
	private OrbitControllerImpl oci;

	// Test data
	private static String[] testMissionData =
			// id, version, code, name, processing_mode, file_class, product_file_template
			{ "1", "0", "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" };
	private static String[] testSpacecraftData =
			// version, code,name,
			{ "1", "S_TDX1", "Tandom-X" };
	private static String[][] testOrbitData = {
			// orbit_id, orbit_version, orbit_number, start_time, stop_time,
			{ "0", "0", "14", "2019-08-29T22:49:21.074395", "2019-10-05T10:12:39.000000" },
			{ "0", "0", "12", "2019-08-30T00:19:33.946628", "2019-10-06T10:13:22.000000" },
			{ "0", "0", "13", "2019-08-30T00:19:33.946628", "2019-10-10T10:13:22.000000" }, };

	/**
	 *
	 * Create test missions and test spacecrafts in the database.
	 *
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		logger.trace(">>> Starting to create test data in the database");
		createMissionAndSpacecraft(testMissionData, testSpacecraftData);
		createOrbit(testOrbitData[0]);
		createOrbit(testOrbitData[1]);
		createOrbit(testOrbitData[2]);
		logger.trace("<<< Finished creating test data in the database");
	}

	/**
	 *
	 * Deleting test data from the database
	 *
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
		logger.trace(">>> Starting to delete test data in database");
		RepositoryService.getOrbitRepository().deleteAll();
		RepositoryService.getSpacecraftRepository().deleteAll();
		RepositoryService.getMissionRepository().deleteAll();
		logger.trace("<<< Finished deleting test data in database");
	}

	/**
	 * Create a test mission and a test spacecraft in the database
	 *
	 * @param missionData    The data from which to create the mission
	 * @param spacecraftData The data from which to create the spacecraft
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
	 * Create an orbit from a data array
	 *
	 * @param orbitData an array of Strings representing the orbit to create
	 * @return an Orbit with its attributes set to the input data
	 */

	private Orbit createOrbit(String[] orbitData) {
		logger.trace("... creating orbit no. {}", orbitData[2]);
		Orbit testOrbit = new Orbit();

		if (null != RepositoryService.getOrbitRepository()
			.findByMissionCodeAndSpacecraftCodeAndOrbitNumber(testMissionData[2], testSpacecraftData[1],
					Integer.valueOf(orbitData[2]))) {
			return RepositoryService.getOrbitRepository()
				.findByMissionCodeAndSpacecraftCodeAndOrbitNumber(testMissionData[2], testSpacecraftData[1],
						Integer.valueOf(orbitData[2]));
		}

		// adding orbit parameters
		testOrbit.setOrbitNumber(Integer.valueOf(orbitData[2]));
		testOrbit.setStartTime(Instant.from(OrbitTimeFormatter.parse(orbitData[3])));
		testOrbit.setStopTime(Instant.from(OrbitTimeFormatter.parse(orbitData[4])));
		testOrbit.setSpacecraft(
				RepositoryService.getSpacecraftRepository().findByMissionAndCode(testMissionData[2], testSpacecraftData[1]));

		testOrbit = RepositoryService.getOrbitRepository().save(testOrbit);

		return testOrbit;
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.OrbitControllerImpl#getOrbits(java.lang.String, java.lang.Long, java.lang.Long, java.lang.String, java.lang.String, java.lang.Long, java.lang.Long, java.lang.String[])}.
	 */
	@Test
	public final void testGetOrbits() {
		logger.trace(">>> testGetOrbits()");
		ResponseEntity<List<RestOrbit>> retrievedOrbits = oci.getOrbits(testSpacecraftData[1], null, null, null, null, null, null,
				null);
		assertEquals(HttpStatus.OK, retrievedOrbits.getStatusCode(), "Wrong HTTP status: ");
		assertTrue(retrievedOrbits.getBody().size() == testOrbitData.length, "Not all orbits found.");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.OrbitControllerImpl#countOrbits(java.lang.String, java.lang.Long, java.lang.Long, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testCountOrbits() {
		logger.trace(">>> testCountOrbits()");
		ResponseEntity<String> orbitCount = oci.countOrbits(testSpacecraftData[1], null, null, null, null);
		assertEquals(HttpStatus.OK, orbitCount.getStatusCode(), "Wrong HTTP status: ");
		assertTrue(Long.valueOf(orbitCount.getBody()) == testOrbitData.length, "Wrong orbit count.");
	}

	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrbitControllerImpl#createOrbits(java.util.List)}.
	 */
	@Test
	public final void testCreateOrbits() {
		logger.trace(">>> testCreateOrbits()");

		RestOrbit toCreate = new RestOrbit();
		toCreate.setMissionCode(testMissionData[2]);
		toCreate.setSpacecraftCode(testSpacecraftData[1]);
		toCreate.setOrbitNumber(100L);
		toCreate.setStartTime(testOrbitData[0][3]);
		toCreate.setStopTime(testOrbitData[0][4]);
		List<RestOrbit> orbitsToCreate = new ArrayList<>();
		orbitsToCreate.add(toCreate);

		ResponseEntity<List<RestOrbit>> createdOrbits = oci.createOrbits(orbitsToCreate);
		assertEquals(HttpStatus.CREATED, createdOrbits.getStatusCode(), "Wrong HTTP status: ");
		assertTrue(createdOrbits.getBody().get(0).getOrbitNumber() == toCreate.getOrbitNumber(), "Error during orbit creation.");
	}

	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrbitControllerImpl#getOrbitById(java.lang.Long)}.
	 */
	@Test
	public final void testGetOrbitById() {
		logger.trace(">>> testGetOrbitById()");

		Orbit expectedOrbit = RepositoryService.getOrbitRepository().findAll().get(0);

		ResponseEntity<RestOrbit> retrievedOrbit = oci.getOrbitById(expectedOrbit.getId());
		assertEquals(HttpStatus.OK, retrievedOrbit.getStatusCode(), "Wrong HTTP status: ");
		assertTrue(retrievedOrbit.getBody().getOrbitNumber() == Long.valueOf(expectedOrbit.getOrbitNumber()),
				"Wrong orbit retrieved.");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.OrbitControllerImpl#modifyOrbit(java.lang.Long, de.dlr.proseo.model.rest.model.RestOrbit)}.
	 */
	@Test
	public final void testModifyOrbit() {
		logger.trace(">>> testModifyOrbit()");

		RestOrbit toBeModified = OrbitUtil.toRestOrbit(RepositoryService.getOrbitRepository().findAll().get(0));
		toBeModified.setOrbitNumber(200L);

		ResponseEntity<RestOrbit> modifiedOrbit = oci.modifyOrbit(toBeModified.getId(), toBeModified);
		assertEquals(HttpStatus.OK, modifiedOrbit.getStatusCode(), "Wrong HTTP status: ");
		assertEquals(toBeModified.getOrbitNumber(), modifiedOrbit.getBody().getOrbitNumber(), "Wrong orbit retrieved.");
	}

	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrbitControllerImpl#deleteOrbitById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteOrbitById() {
		logger.trace(">>> testDeleteOrbit()");
		List<Orbit> beforeDeletion = RepositoryService.getOrbitRepository().findAll();
		Orbit toBeDeleted = beforeDeletion.get(0);

		ResponseEntity<?> deletion = oci.deleteOrbitById(toBeDeleted.getId());
		assertEquals(HttpStatus.NO_CONTENT, deletion.getStatusCode(), "Wrong HTTP status: ");

		List<Orbit> afterDeletion = RepositoryService.getOrbitRepository().findAll();
		assertTrue(afterDeletion.size() < beforeDeletion.size(), "After deletion, repository does not contain less orbits.");
	}
}
