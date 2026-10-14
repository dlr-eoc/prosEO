/**
 * DownlinkRepositoryTest.java
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.DataSegment;
import de.dlr.proseo.model.Datatake;
import de.dlr.proseo.model.Downlink;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Payload;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import jakarta.persistence.EntityManager;

/**
 * Unit test cases for DownlinkRepository
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
public class DownlinkRepositoryTest {

	private static final String TEST_MISSIONCODE = "xyz567";
	private static final String TEST_SC_CODE = "XYZ";
	private static final String TEST_PAYLOAD = "XYZ_PL1";
	private static final String TEST_DL_ID = "$Downlink 0815$";
	private static final String TEST_DT_ID = "$Datatake 4711$";
	private static final Instant TEST_START_TIME = Instant.from(OrbitTimeFormatter.parse("2018-06-13T09:23:45.637000"));
	private static final Duration TEST_DURATION = Duration.ofSeconds(10);
	private static final String TEST_PARAM_KEY = "key1";
	private static final String TEST_PARAM_VALUE = "value1";
	
	@Autowired
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(DownlinkRepositoryTest.class);
	
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
		
		// Basic test: Create a downlink and find it by downlink ID
		Downlink downlink = new Downlink();
		downlink.setDownlinkId(TEST_DL_ID);
		downlink.setSpacecraft(spacecraft);
		downlink = RepositoryService.getDownlinkRepository().save(downlink);
		
		Optional<Downlink> newDownlinkOpt = RepositoryService.getDownlinkRepository()
				.findByMissionAndSpacecraftAndId(TEST_MISSIONCODE, TEST_SC_CODE, TEST_DL_ID);
		assertFalse(newDownlinkOpt.isEmpty(), "Downlink not found by downlink ID");
		
		Downlink newDownlink = newDownlinkOpt.get();
		assertEquals(downlink, newDownlink, "Downlink does not match");
		
		logger.info("OK: Test for findByMissionAndSpacecraftAndId completed");
		
		// Add data segments to the downlink
		
		// Create a datatake
		Datatake datatake = new Datatake();
		datatake.setDatatakeId(TEST_DT_ID);
		datatake.setSpacecraft(spacecraft);
		datatake.setPayload(spacecraft.getPayloads().get(0));
		datatake.setSensingStartTime(TEST_START_TIME);
		datatake.setDatatakeDuration(TEST_DURATION);
		Parameter dtParam = new Parameter().init(ParameterType.STRING, TEST_PARAM_VALUE);
		datatake.getParameters().put(TEST_PARAM_KEY, dtParam);
		datatake = RepositoryService.getDatatakeRepository().save(datatake);

		DataSegment ds1 = new DataSegment();
		ds1.setSegmentNumber(2); // adding in wrong order!
		ds1.setDatatake(datatake);
		ds1.setSensingStartTime(TEST_START_TIME.plusSeconds(TEST_DURATION.dividedBy(2).getSeconds()));
		ds1.setSegmentDuration(TEST_DURATION.dividedBy(2));
		ds1.setDownlink(downlink);
		em.persist(ds1);
		datatake.getDataSegments().add(ds1);
		
		DataSegment ds2 = new DataSegment();
		ds2.setSegmentNumber(1); // adding in wrong order!
		ds2.setDatatake(datatake);
		ds2.setSensingStartTime(TEST_START_TIME);
		ds2.setSegmentDuration(TEST_DURATION.dividedBy(2));
		ds2.setDownlink(downlink);
		em.persist(ds2);
		datatake.getDataSegments().add(ds2);
		
		datatake = RepositoryService.getDatatakeRepository().save(datatake);

		downlink.getDataSegments().add(ds1);
		downlink.getDataSegments().add(ds2);
		
		downlink = RepositoryService.getDownlinkRepository().save(downlink);
		
		em.flush();
		
		newDownlinkOpt = RepositoryService.getDownlinkRepository()
				.findByMissionAndSpacecraftAndId(TEST_MISSIONCODE, TEST_SC_CODE, TEST_DL_ID);
		assertFalse(newDownlinkOpt.isEmpty(), "Downlink not found by downlink ID");
		
		newDownlink = newDownlinkOpt.get();
		assertEquals(downlink, newDownlink, "Downlink does not match after adding segments");
		assertEquals(2, newDownlink.getDataSegments().size(), "Number of data segments wrong");
		
		assertEquals(downlink, datatake.getDataSegments().iterator().next().getDownlink(),
				"Navigation from datatake to downlink fails");
		
		logger.info("OK: Test for downlink with data segments completed");
		
	}

}
