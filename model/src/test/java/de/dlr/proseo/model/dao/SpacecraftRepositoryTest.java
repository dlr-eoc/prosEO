/**
 * SpacecraftRepositoryTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for OrbitRepository
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
public class SpacecraftRepositoryTest {

	private static final String TEST_MISSIONCODE = "qwertz";

	private static final String TEST_SC_CODE = "$XYZ$";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(SpacecraftRepositoryTest.class);
	
	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeEach
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterEach
	public void tearDown() throws Exception {
	}

	/**
	 * Test the additional repository methods
	 */
	@Test
	public final void test() {
		Mission mission = new Mission();
		mission.setCode(TEST_MISSIONCODE);
		mission = RepositoryService.getMissionRepository().save(mission);
		
		Spacecraft spacecraft = new Spacecraft();
		spacecraft.setMission(mission);
		spacecraft.setCode(TEST_SC_CODE);
		spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
		mission.getSpacecrafts().add(spacecraft);
		
		// Test findByCode
		spacecraft = RepositoryService.getSpacecraftRepository().findByMissionAndCode(TEST_MISSIONCODE, TEST_SC_CODE);
		assertNotNull(spacecraft, "Find by spacecraft code failed for Spacecraft");
		
		logger.info("OK: Test for findByMissionAndCode completed");
		
	}

}
