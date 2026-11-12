/**
 * DownlinkUtilTest.java
 *
 * (c) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.dtmgr.rest.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.DataSegment;
import de.dlr.proseo.model.Datatake;
import de.dlr.proseo.model.Downlink;
import de.dlr.proseo.model.GroundStation;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.OrbitalPass;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Payload;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test class for DownlinkUtil (implicitly tests DataSegmentUtil, too)
 *
 * @author Dr. Thomas Bassler
 */
class DownlinkUtilTest {

	private static final long TEST_GS_DBID = 4710L;
	private static final long TEST_DOWNLINK_DBID = 4711L;
	private static final long TEST_SEGMENT_DBID = 4712L;
	private static final String TEST_CHANNEL_2 = "CH-2";
	private static final String TEST_CHANNEL_1 = "CH-1";
	private static final String TEST_MISSIONCODE = "xyz567";
	private static final String TEST_SC_CODE = "XYZ";
	private static final String TEST_PAYLOAD = "XYZ_PL1";
	private static final String TEST_DATATAKE_ID = "$Datatake 4711$";
	private static final String TEST_DOWNLINK_ID = "$Downlink 0815$";
	private static final Instant TEST_START_TIME = Instant.from(OrbitTimeFormatter.parse("2018-06-13T09:23:45.637000"));
	private static final Duration TEST_DURATION = Duration.ofSeconds(10);
	private static final String TEST_PARAM_KEY = "key1";
	private static final String TEST_PARAM_VALUE = "value1";
	private static final String TEST_GS_ID = "GS1";
	private static final String TEST_GS_NAME = "Ground Station 1";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(DownlinkUtilTest.class);
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link de.dlr.proseo.dtmgr.rest.model.DownlinkUtil#toRestDownlinnk(de.dlr.proseo.model.Downlink)}.
	 */
	@Test
	final void testToRestDownlink() {
		Mission mission = new Mission();
		mission.setCode(TEST_MISSIONCODE);
		
		Spacecraft spacecraft = new Spacecraft();
		spacecraft.setCode(TEST_SC_CODE);
		spacecraft.setMission(mission);
		Payload payload = new Payload();
		payload.setName(TEST_PAYLOAD);
		spacecraft.getPayloads().add(payload);
		
		Datatake datatake = new Datatake();
		datatake.setDatatakeId(TEST_DATATAKE_ID);
		datatake.setSpacecraft(spacecraft);
		datatake.setPayload(spacecraft.getPayloads().get(0));
		datatake.setSensingStartTime(TEST_START_TIME);
		datatake.setDatatakeDuration(TEST_DURATION);
		Parameter dtParam = new Parameter().init(ParameterType.STRING, TEST_PARAM_VALUE);
		datatake.getParameters().put(TEST_PARAM_KEY, dtParam);

		DataSegment ds1 = new DataSegment();
		ds1.setSegmentNumber(1);
		ds1.setDatatake(datatake);
		ds1.setSensingStartTime(TEST_START_TIME);
		ds1.setSegmentDuration(TEST_DURATION.dividedBy(2));
		ds1.setChannel(TEST_CHANNEL_1);
		datatake.getDataSegments().add(ds1);
		
		DataSegment ds2 = new DataSegment();
		ds2.setSegmentNumber(2);
		ds2.setDatatake(datatake);
		ds2.setSensingStartTime(TEST_START_TIME.plus(TEST_DURATION.dividedBy(2)));
		ds2.setSegmentDuration(TEST_DURATION.dividedBy(2));
		ds2.setChannel(TEST_CHANNEL_2);
		datatake.getDataSegments().add(ds2);

		Downlink downlink = new Downlink();
		downlink.setDownlinkId(TEST_DOWNLINK_ID);
		downlink.setSpacecraft(spacecraft);
		ds1.setDownlink(downlink);
		ds2.setDownlink(downlink);
		downlink.getDataSegments().add(ds1);
		downlink.getDataSegments().add(ds2);
		downlink.getChannels().addAll(Arrays.asList(TEST_CHANNEL_1, TEST_CHANNEL_2));
		
		GroundStation groundStation = new GroundStation();
		groundStation.setStationId(TEST_GS_ID);
		groundStation.setStationName(TEST_GS_NAME);

		OrbitalPass pass = new OrbitalPass();
		pass.setGroundStation(groundStation);
		pass.setAcquisitionStartTime(TEST_START_TIME);
		downlink.getPasses().add(pass);
		
		RestDownlink restDownlink = DownlinkUtil.toRestDownlink(downlink);
		
		assertNotNull(restDownlink, "toRestDownlink returns null");

		ObjectMapper mapper = JsonMapper.builder().configure(SerializationFeature.INDENT_OUTPUT, true).build();
		logger.info("... REST downlink:\n{}", mapper.writeValueAsString(restDownlink));
		
		assertEquals(downlink.getSpacecraft().getMission().getCode(), restDownlink.getMissionCode(), "Mission code mismatch");
		assertEquals(downlink.getDownlinkId(), restDownlink.getDownlinkId(), "Downlink ID mismatch");
		assertEquals(downlink.getDataSegments().size(), restDownlink.getDataSegments().size(), "Number of data segments does not match");
		assertEquals(downlink.getChannels(), restDownlink.getChannels(), "Channel mismatch");
		
		// Check data segment order is preserved
		int expectedSegmentNumber = 1;
		for (RestDataSegment restDataSegment: restDownlink.getDataSegments()) {
			assertEquals(expectedSegmentNumber, restDataSegment.getSegmentNumber(), "Wrong sequence in data segments");
			assertEquals(datatake.getDatatakeId(), restDataSegment.getDatatakeId(), 
					"Datatake ID mismatch in segment " + expectedSegmentNumber);
			assertEquals(TEST_DURATION.dividedBy(2).toMillis(), restDataSegment.getSegmentDuration(),
					"Segment duration mismatch in segment " + expectedSegmentNumber);
			switch (expectedSegmentNumber) {
			case 1:
				assertEquals(ds1.getSensingStartTime(), 
						Instant.from(OrbitTimeFormatter.parse(restDataSegment.getSensingStartTime())));
				break;
			case 2:
				assertEquals(ds2.getSensingStartTime(), 
						Instant.from(OrbitTimeFormatter.parse(restDataSegment.getSensingStartTime())));
				break;
			default:
				fail("Unexpected data segment number: " + restDataSegment.getSegmentNumber());
			}
			++expectedSegmentNumber;
		}
		
		// Check parameters
		//assertEquals()
		
		logger.info("OK: Test for conversion to REST downlink completed");
	}

	/**
	 * Test method for {@link de.dlr.proseo.dtmgr.rest.model.DownlinkUtil#toModelDownlink(de.dlr.proseo.dtmgr.rest.model.RestDownlink)}.
	 */
	@Test
	final void testToModelDownlink() {
		RestDownlink restDownlink = new RestDownlink();
		restDownlink.setId(TEST_DOWNLINK_DBID);
		restDownlink.setVersion(1L);
		restDownlink.setMissionCode(TEST_MISSIONCODE);
		restDownlink.setSpacecraftCode(TEST_SC_CODE);
		restDownlink.setDownlinkId(TEST_DOWNLINK_ID);
		restDownlink.getChannels().addAll(Arrays.asList(TEST_CHANNEL_1, TEST_CHANNEL_2));
		
		RestGroundStation restGroundStation = new RestGroundStation(TEST_GS_DBID, 1L, TEST_GS_ID, TEST_GS_NAME);
		RestOrbitalPass restOrbitalPass = new RestOrbitalPass();
		restOrbitalPass.setGroundStation(restGroundStation);
		restOrbitalPass.setAcquisitionStartTime(OrbitTimeFormatter.format(TEST_START_TIME));
		
		restDownlink.getPasses().add(restOrbitalPass);
		
		RestDataSegment restDataSegment = new RestDataSegment();
		restDataSegment.setId(TEST_SEGMENT_DBID);
		restDataSegment.setVersion(1L);
		restDataSegment.setMissionCode(TEST_MISSIONCODE);
		restDataSegment.setSpacecraftCode(TEST_SC_CODE);
		restDataSegment.setPayloadName(TEST_PAYLOAD);
		restDataSegment.setDatatakeId(TEST_DATATAKE_ID);
		restDataSegment.setSegmentNumber(1L);
		restDataSegment.setSensingStartTime(OrbitTimeFormatter.format(TEST_START_TIME));
		restDataSegment.setSegmentDuration(TEST_DURATION.toMillis());
		
		restDownlink.getDataSegments().add(restDataSegment);
		
		ObjectMapper mapper = JsonMapper.builder().configure(SerializationFeature.INDENT_OUTPUT, true).build();
		logger.info("... REST downlink:\n{}", mapper.writeValueAsString(restDownlink));
		
		Downlink downlink = DownlinkUtil.toModelDownlink(restDownlink);
		assertNotNull(downlink, "toModelDownlink returns null");
		
		assertEquals(TEST_DOWNLINK_DBID, downlink.getId(), "DBID mismatch");
		assertEquals(1L, downlink.getVersion(), "Version mismatch");
		assertNull(downlink.getSpacecraft(), "Spacecraft exists??");
		assertEquals(TEST_DOWNLINK_ID, downlink.getDownlinkId(), "Downlink ID mismatch");
		assertEquals(restDownlink.getChannels(), downlink.getChannels(), "Channel list mismatch");
		assertEquals(1, downlink.getPasses().size(), "Unexpected number of passes");
		assertEquals(1, downlink.getDataSegments().size(), "Unexpected number of segments");
		
		OrbitalPass pass = downlink.getPasses().get(0);
		assertNotNull(pass.getGroundStation(), "Ground station is null");
		assertEquals(TEST_GS_ID, pass.getGroundStation().getStationId(), "Ground station ID mismatch");
		assertEquals(TEST_START_TIME, pass.getAcquisitionStartTime(), "Start time mismatch");
		
		DataSegment dataSegment = downlink.getDataSegments().get(0);
		assertNull(dataSegment.getDatatake(), "Datatake exists??");
		assertEquals(1, dataSegment.getSegmentNumber(), "Segment number mismatch");
		assertEquals(TEST_START_TIME, dataSegment.getSensingStartTime(), "Start time mismatch in segment");
		assertEquals(TEST_DURATION, dataSegment.getSegmentDuration(), "Duration mismatch in segment");
		
		logger.info("OK: Test for conversion to model downlink completed");
	}

}
