/**
 * DatatakeUtilTest.java
 *
 * (c) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.dtmgr.rest.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.DataSegment;
import de.dlr.proseo.model.Datatake;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Payload;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test class for DatatakeUtil (implicitly tests DataSegmentUtil, too)
 *
 * @author Dr. Thomas Bassler
 */
class DatatakeUtilTest {

	private static final long TEST_SEGMENT_DBID = 4712L;
	private static final long TEST_DATATAKE_DBID = 4711L;
	private static final String TEST_MISSIONCODE = "xyz567";
	private static final String TEST_SC_CODE = "XYZ";
	private static final String TEST_PAYLOAD = "XYZ_PL1";
	private static final String TEST_DATATAKE_ID = "$Datatake 4711$";
	private static final Instant TEST_START_TIME = Instant.from(OrbitTimeFormatter.parse("2018-06-13T09:23:45.637000"));
	private static final Duration TEST_DURATION = Duration.ofSeconds(10);
	private static final String TEST_PARAM_KEY = "key1";
	private static final String TEST_PARAM_VALUE = "value1";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(DatatakeUtilTest.class);
	
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
	 * Test method for {@link de.dlr.proseo.dtmgr.rest.model.DatatakeUtil#toRestDatatake(de.dlr.proseo.model.Datatake)}.
	 */
	@Test
	final void testToRestDatatake() {
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
		ds1.setSegmentNumber(2); // adding in wrong order!
		ds1.setDatatake(datatake);
		ds1.setSensingStartTime(TEST_START_TIME.plus(TEST_DURATION.dividedBy(2)));
		ds1.setSegmentDuration(TEST_DURATION.dividedBy(2));
		datatake.getDataSegments().add(ds1);
		
		DataSegment ds2 = new DataSegment();
		ds2.setSegmentNumber(1); // adding in wrong order!
		ds2.setDatatake(datatake);
		ds2.setSensingStartTime(TEST_START_TIME);
		ds2.setSegmentDuration(TEST_DURATION.dividedBy(2));
		datatake.getDataSegments().add(ds2);
		
		RestDatatake restDatatake = DatatakeUtil.toRestDatatake(datatake);
		
		assertNotNull(restDatatake, "toRestDatatake returns null");
		
		ObjectMapper mapper = JsonMapper.builder().configure(SerializationFeature.INDENT_OUTPUT, true).build();
		logger.info("... REST datatake:\n{}", mapper.writeValueAsString(restDatatake));
		
		assertEquals(datatake.getSpacecraft().getMission().getCode(), restDatatake.getMissionCode(), "Mission code mismatch");
		assertEquals(datatake.getPayload().getName(), restDatatake.getPayloadName(), "Payload name mismatch");
		assertEquals(datatake.getDatatakeId(), restDatatake.getDatatakeId(), "Datatake ID mismatch");
		assertEquals(datatake.getDataSegments().size(), restDatatake.getDataSegments().size(), "Number of data segments does not match");
		
		// Check data segments in ascending order
		int expectedSegmentNumber = 1;
		for (RestDataSegment restDataSegment: restDatatake.getDataSegments()) {
			assertEquals(expectedSegmentNumber, restDataSegment.getSegmentNumber(), "Wrong sequence in data segments");
			assertEquals(datatake.getDatatakeId(), restDataSegment.getDatatakeId(), 
					"Datatake ID mismatch in segment " + expectedSegmentNumber);
			assertEquals(TEST_DURATION.dividedBy(2).toMillis(), restDataSegment.getSegmentDuration(),
					"Segment duration mismatch in segment " + expectedSegmentNumber);
			switch (expectedSegmentNumber) {
			case 1:
				assertEquals(ds2.getSensingStartTime(), 
						Instant.from(OrbitTimeFormatter.parse(restDataSegment.getSensingStartTime())));
				break;
			case 2:
				assertEquals(ds1.getSensingStartTime(), 
						Instant.from(OrbitTimeFormatter.parse(restDataSegment.getSensingStartTime())));
				break;
			default:
				fail("Unexpected data segment number: " + restDataSegment.getSegmentNumber());
			}
			++expectedSegmentNumber;
		}
		
		// Check parameters
		//assertEquals()
		
		logger.info("OK: Test for conversion to REST datatake completed");
	}

	/**
	 * Test method for {@link de.dlr.proseo.dtmgr.rest.model.DatatakeUtil#toModelDatatake(de.dlr.proseo.dtmgr.rest.model.RestDatatake)}.
	 */
	@Test
	final void testToModelDatatake() {
		RestDatatake restDatatake = new RestDatatake();
		restDatatake.setId(TEST_DATATAKE_DBID);
		restDatatake.setVersion(1L);
		restDatatake.setMissionCode(TEST_MISSIONCODE);
		restDatatake.setSpacecraftCode(TEST_SC_CODE);
		restDatatake.setPayloadName(TEST_PAYLOAD);
		restDatatake.setDatatakeId(TEST_DATATAKE_ID);
		restDatatake.setSensingStartTime(OrbitTimeFormatter.format(TEST_START_TIME));
		restDatatake.setDatatakeDuration(TEST_DURATION.toMillis());
		restDatatake.setIsComplete(false);
		restDatatake.getParameters().add(new RestParameter(TEST_PARAM_KEY, ParameterType.STRING.toString(), TEST_PARAM_VALUE));
		
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
		
		restDatatake.getDataSegments().add(restDataSegment);
		
		ObjectMapper mapper = JsonMapper.builder().configure(SerializationFeature.INDENT_OUTPUT, true).build();
		logger.info("... REST datatake:\n{}", mapper.writeValueAsString(restDatatake));

		Datatake datatake = DatatakeUtil.toModelDatatake(restDatatake);
		assertNotNull(datatake, "toModelDatatake returns null");
		
		assertEquals(TEST_DATATAKE_DBID, datatake.getId(), "DBID mismatch");
		assertEquals(1L, datatake.getVersion(), "Version mismatch");
		assertNull(datatake.getSpacecraft(), "Spacecraft exists??");
		assertNull(datatake.getPayload(), "Payload exists??");
		assertEquals(TEST_DATATAKE_ID, datatake.getDatatakeId(), "Datatake ID mismatch");
		assertEquals(TEST_START_TIME, datatake.getSensingStartTime(), "Start time mismatch");
		assertEquals(TEST_DURATION, datatake.getDatatakeDuration(), "Duration mismatch");
		
		assertNotNull(datatake.getParameters().get(TEST_PARAM_KEY), "Parameter missing");
		assertEquals(TEST_PARAM_VALUE, datatake.getParameters().get(TEST_PARAM_KEY).getParameterValue(), "Parameter value mismatch");
		
		assertEquals(1, datatake.getDataSegments().size(), "Unexpected number of segments");
		
		DataSegment ds1 = datatake.getDataSegments().iterator().next();
		assertEquals(datatake, ds1.getDatatake(), "Datatake mismatch in segment");
		assertEquals(1, ds1.getSegmentNumber(), "Segment number mismatch");
		assertEquals(TEST_START_TIME, ds1.getSensingStartTime(), "Start time mismatch in segment");
		assertEquals(TEST_DURATION, ds1.getSegmentDuration(), "Duration mismatch in segment");
		
		logger.info("OK: Test for conversion to model datatake completed");
	}

}
