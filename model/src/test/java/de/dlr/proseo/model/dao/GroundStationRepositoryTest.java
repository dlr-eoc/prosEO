/**
 * GroundStationRepositoryTest.java
 * 
 * (c) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

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

import de.dlr.proseo.model.Downlink;
import de.dlr.proseo.model.GroundStation;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Payload;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for GroundStationRepository
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
public class GroundStationRepositoryTest {

	private static final String TEST_MISSIONCODE = "xyz567";
	private static final String TEST_SC_CODE = "XYZ";
	private static final String TEST_PAYLOAD = "XYZ_PL1";
	private static final String TEST_DL_ID = "$Downlink 0815$";
	private static final String TEST_ID = "GS1";
	private static final String TEST_NAME = "Ground Station 1";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(GroundStationRepositoryTest.class);
	
	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeAll
	public static void setUpBeforeAll() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterAll
	public static void tearDownAfterAll() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeEach
	public void setUpBeforeEach() throws Exception {
		Mission mission = new Mission();
		mission.setCode(TEST_MISSIONCODE);
		mission = RepositoryService.getMissionRepository().save(mission);
		
		Spacecraft spacecraft = new Spacecraft();
		spacecraft.setCode(TEST_SC_CODE);
		spacecraft.setMission(mission);
		Payload payload = new Payload();
		payload.setName(TEST_PAYLOAD);
		spacecraft.getPayloads().add(payload);
		spacecraft = RepositoryService.getSpacecraftRepository().save(spacecraft);
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterEach
	public void tearDownAfterEach() throws Exception {
	}

	/**
	 * Test the additional repository methods
	 */
	@Test
	public final void test() {
		
		// Basic test: Create a ground station and find it by station name
		GroundStation groundStation = new GroundStation();
		groundStation.setStationId(TEST_ID);
		groundStation.setStationName(TEST_NAME);
		
		groundStation = RepositoryService.getGroundStationRepository().save(groundStation);
		
		Optional<GroundStation> newGroundStationOpt = RepositoryService.getGroundStationRepository().findByStationId(TEST_ID);
		assertFalse(newGroundStationOpt.isEmpty(), "Ground station not found by station ID");
		
		GroundStation newGroundStation = newGroundStationOpt.get();
		assertEquals(groundStation, newGroundStation, "Ground stations do not match");
		assertEquals(TEST_NAME, newGroundStation.getStationName(), "Ground station name wrong");
		
		logger.info("OK: Test for findByStationId completed");
		
		// Add link from downlink
		Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByMissionAndCode(TEST_MISSIONCODE, TEST_SC_CODE);
		
		Downlink downlink = new Downlink();
		downlink.setDownlinkId(TEST_DL_ID);
		downlink.setSpacecraft(spacecraft);
		downlink.setGroundStation(groundStation);
		downlink = RepositoryService.getDownlinkRepository().save(downlink);
		
		Optional<Downlink> newDownlinkOpt = RepositoryService.getDownlinkRepository()
				.findByMissionAndSpacecraftAndId(TEST_MISSIONCODE, TEST_SC_CODE, TEST_DL_ID);
		assertFalse(newDownlinkOpt.isEmpty(), "Downlink not found by downlink ID");
		
		Downlink newDownlink = newDownlinkOpt.get();
		assertEquals(groundStation, newDownlink.getGroundStation(), "Ground stations in downlink do not match");
		
		logger.info("OK: Test for linking from downlink completed");
		
	}

}
