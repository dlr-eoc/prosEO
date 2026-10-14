/**
 * DatatakeGroupRepositoryTest.java
 * 
 * (c) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
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

import de.dlr.proseo.model.Datatake;
import de.dlr.proseo.model.DatatakeGroup;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Payload;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Unit test cases for DatatakeGroupRepository
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
public class DatatakeGroupRepositoryTest {

	private static final String TEST_MISSIONCODE = "xyz567";
	private static final String TEST_SC_CODE = "XYZ";
	private static final String TEST_PAYLOAD = "XYZ_PL1";
	private static final String TEST_ID = "$Datatake 4711$";
	private static final Instant TEST_START_TIME = Instant.from(OrbitTimeFormatter.parse("2018-06-13T09:23:45.637000"));
	private static final Duration TEST_DURATION = Duration.ofSeconds(10);
	private static final String TEST_PARAM_KEY = "key1";
	private static final String TEST_PARAM_VALUE = "value1";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(DatatakeGroupRepositoryTest.class);
	
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
		
		Spacecraft spacecraft = RepositoryService.getSpacecraftRepository().findByMissionAndCode(TEST_MISSIONCODE, TEST_SC_CODE);
		
		// Basic test: Create a datatake group and find it by datatake group identifier
		DatatakeGroup dtGroup = new DatatakeGroup();
		dtGroup.setMission(spacecraft.getMission());
		dtGroup.setDtGroupIdentifier(TEST_ID);
		
		// Add a datatake to the group
		Datatake datatake = new Datatake();
		datatake.setDatatakeId(TEST_ID);
		datatake.setSpacecraft(spacecraft);
		datatake.setPayload(spacecraft.getPayloads().get(0));
		datatake.setSensingStartTime(TEST_START_TIME);
		datatake.setDatatakeDuration(TEST_DURATION);
		Parameter dtParam = new Parameter().init(ParameterType.STRING, TEST_PARAM_VALUE);
		datatake.getParameters().put(TEST_PARAM_KEY, dtParam);
		datatake = RepositoryService.getDatatakeRepository().save(datatake);
		
		dtGroup.getDatatakes().add(datatake);
		dtGroup = RepositoryService.getDatatakeGroupRepository().save(dtGroup);
		
		Optional<DatatakeGroup> newDtGroupOpt = RepositoryService.getDatatakeGroupRepository().findByMissionCodeAndIdentifier(TEST_MISSIONCODE, TEST_ID);
		assertFalse(newDtGroupOpt.isEmpty(), "Datatake group not found");
		
		DatatakeGroup newDtGroup = newDtGroupOpt.get();
		assertEquals(dtGroup, newDtGroup, "Datatake group does not match");
		assertEquals(datatake, newDtGroup.getDatatakes().iterator().next(), "Datatakes in group do not match");
		
		logger.info("OK: Test for findByMissionCodeAndIdentifier completed");
		
	}

}
