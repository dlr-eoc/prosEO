/**
 * DatatakeRepositoryTest.java
 * 
 * (c) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Payload;
import de.dlr.proseo.model.RadarDatatake;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import jakarta.persistence.EntityManager;

/**
 * Unit test cases for DatatakeRepository
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
public class DatatakeRepositoryTest {

	private static final String TEST_MISSIONCODE = "xyz567";
	private static final String TEST_SC_CODE = "XYZ";
	private static final String TEST_PAYLOAD = "XYZ_PL1";
	private static final String TEST_ID = "$Datatake 4711$";
	private static final String TEST_ID_RADAR = "$RadarDatatake 4711$";
	private static final Instant TEST_START_TIME = Instant.from(OrbitTimeFormatter.parse("2018-06-13T09:23:45.637000"));
	private static final Instant TEST_START_TIME_RADAR = Instant.from(OrbitTimeFormatter.parse("2020-02-29T09:23:45.637000"));
	private static final Duration TEST_DURATION = Duration.ofSeconds(10);
	private static final String TEST_PARAM_KEY = "key1";
	private static final String TEST_PARAM_VALUE = "value1";
	
	@Autowired
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(DatatakeRepositoryTest.class);
	
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
		
		// Basic test: Create a datatake and find it by datatake ID
		Datatake datatake = new Datatake();
		datatake.setDatatakeId(TEST_ID);
		datatake.setSpacecraft(spacecraft);
		datatake.setPayload(spacecraft.getPayloads().get(0));
		datatake.setSensingStartTime(TEST_START_TIME);
		datatake.setDatatakeDuration(TEST_DURATION);
		Parameter dtParam = new Parameter().init(ParameterType.STRING, TEST_PARAM_VALUE);
		datatake.getParameters().put(TEST_PARAM_KEY, dtParam);
		datatake = RepositoryService.getDatatakeRepository().save(datatake);
		
		Optional<Datatake> newDatatakeOpt = RepositoryService.getDatatakeRepository().findByMissionAndPayloadAndId(TEST_MISSIONCODE, TEST_SC_CODE, TEST_PAYLOAD, TEST_ID);
		assertFalse(newDatatakeOpt.isEmpty(), "Find by datatake ID failed");
		
		Datatake newDatatake = newDatatakeOpt.get();
		assertEquals(datatake.getDatatakeId(), newDatatake.getDatatakeId(), "Datatake ID wrong");
		assertEquals(spacecraft, newDatatake.getSpacecraft(), "Spacecraft wrong");
		assertEquals(datatake.getPayload(), newDatatake.getPayload(), "Payload wrong");
		assertEquals(datatake.getSensingStartTime(), newDatatake.getSensingStartTime(), "Sensing start time wrong");
		assertEquals(datatake.getDatatakeDuration(), newDatatake.getDatatakeDuration(), "Duration wrong");
		assertEquals(datatake.getParameters().get(TEST_PARAM_KEY), newDatatake.getParameters().get(TEST_PARAM_KEY), "Parameter wrong");
		
		logger.info("OK: Test for findByMissionAndPayloadAndId completed");
		
		// Subclassing test: Create a Radar datatake and find it by sensing start time
		RadarDatatake radarDatatake = new RadarDatatake();
		radarDatatake.setDatatakeId(TEST_ID_RADAR);
		radarDatatake.setSpacecraft(spacecraft);
		radarDatatake.setPayload(spacecraft.getPayloads().get(0));
		radarDatatake.setSensingStartTime(TEST_START_TIME_RADAR);
		radarDatatake.setDatatakeDuration(TEST_DURATION);
		radarDatatake.setInstrumentSwath("WS");
		radarDatatake.setPolarization("HH");
		radarDatatake = RepositoryService.getDatatakeRepository().save(radarDatatake);
		
		List<Datatake> newRadarDatatakes = RepositoryService.getDatatakeRepository().findByMissionAndPayloadAndSensingStartBetween(
				TEST_MISSIONCODE, TEST_SC_CODE, TEST_PAYLOAD, TEST_START_TIME_RADAR.minusSeconds(5), TEST_START_TIME_RADAR);
		assertEquals(1, newRadarDatatakes.size(), "Unexpected number of list objects");
		
		newDatatake = newRadarDatatakes.get(0);
		assertTrue(newDatatake instanceof RadarDatatake, "Datatake type wrong (expecting RadarDatatype)");
		
		logger.info("Status of RADAR datatake after read from database: " + ((RadarDatatake) newDatatake));
		
		assertEquals(TEST_ID_RADAR, newDatatake.getDatatakeId(), "Datatake ID wrong for RadarDatatake");
		assertEquals(radarDatatake.getInstrumentSwath(), ((RadarDatatake) newDatatake).getInstrumentSwath(), "Swath wrong");
		assertEquals(radarDatatake.getPolarization(), ((RadarDatatake) newDatatake).getPolarization(), "Polarization wrong");
		
		logger.info("OK: Test for findByMissionAndPayloadAndSensingStartBetween and RadarDatatake completed");
		
		// Segment list test: Add two segments to the first datatake and check the ordering
		DataSegment ds1 = new DataSegment();
		ds1.setSegmentNumber(2); // adding in wrong order!
		ds1.setDatatake(datatake);
		ds1.setSensingStartTime(TEST_START_TIME.plusSeconds(TEST_DURATION.dividedBy(2).getSeconds()));
		ds1.setSegmentDuration(TEST_DURATION.dividedBy(2));
		em.persist(ds1);
		datatake.getDataSegments().add(ds1);
		
		DataSegment ds2 = new DataSegment();
		ds2.setSegmentNumber(1); // adding in wrong order!
		ds2.setDatatake(datatake);
		ds2.setSensingStartTime(TEST_START_TIME);
		ds2.setSegmentDuration(TEST_DURATION.dividedBy(2));
		em.persist(ds2);
		datatake.getDataSegments().add(ds2);
		
		datatake = RepositoryService.getDatatakeRepository().save(datatake);

		newDatatakeOpt = RepositoryService.getDatatakeRepository().findById(datatake.getId());
		assertFalse(newDatatakeOpt.isEmpty(), "Find by database ID failed");

		logger.info("Status of datatake with segments after read from database: " + newDatatakeOpt.get());
		
		newDatatake = newDatatakeOpt.get();
		assertEquals(2, newDatatake.getDataSegments().size(), "Unexpected number of data segments");
		assertEquals(1, newDatatake.getDataSegments().iterator().next().getSegmentNumber(), "Unexpected ordering of data segments");
		assertEquals(TEST_START_TIME.plusSeconds(TEST_DURATION.dividedBy(2).getSeconds()), 
				((DataSegment) newDatatake.getDataSegments().toArray()[1]).getSensingStartTime(), "Segment start time wrong");
		assertEquals(TEST_DURATION.dividedBy(2), 
				newDatatake.getDataSegments().iterator().next().getSegmentDuration(), "Wrong duration");
		
		logger.info("OK: Test for data segment ordering completed");
		
	}

}
