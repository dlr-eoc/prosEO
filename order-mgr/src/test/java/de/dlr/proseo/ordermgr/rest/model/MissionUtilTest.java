/**
 * ProductUtilTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest.model;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.dlr.proseo.ordermgr.rest.model.Mission;
import de.dlr.proseo.ordermgr.rest.model.Spacecraft;

import de.dlr.proseo.ordermgr.rest.MissionControllerTest;

/**
 * @author Ranjitha Vignesh
 *
 */
public class MissionUtilTest {

	/* Test Missions */
	private static String[][] testMissionData = {
			// id, version, mission_code, mission_name,spacecraft_version,spacecraft_code,spacecraft_name
			{ "0", "0", "ABCe", "ABCD Testing", "1","S_TDX1","Tandom-X"},
			{ "11", "11", "DEFg", "DefrostMission", "2","S_TDX2","Tandom-X"},
			{ "12", "12", "XY1Z", "XYZ Testing", "3","S_TDX3","Tandom-X" }
		};

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(MissionControllerTest.class);
	
	/**
	 * Create a product from a data array
	 * 
	 * @param testData an array of Strings representing the mission to create
	 * @return a Mission with its attributes set to the input data
	 */
	private de.dlr.proseo.model.Mission createMission(String[] testData) {
		de.dlr.proseo.model.Mission testMission = new de.dlr.proseo.model.Mission();
		de.dlr.proseo.model.Spacecraft testSpacecraft = new de.dlr.proseo.model.Spacecraft();

		testMission.setId(Long.parseLong(testData[0]));
		testMission.setCode(testData[2]);
		testMission.setName(testData[3]);

		//adding Spacecraft parameters
		testSpacecraft.setMission(testMission);
		testSpacecraft.incrementVersion();
		testSpacecraft.setCode(testData[5]);
		testSpacecraft.setName(testData[6]);
		
		logger.info("Created test mission {}", testMission.getId());
		return testMission;
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void test() {
		// Create an empty product
		de.dlr.proseo.model.Mission modelmission = new de.dlr.proseo.model.Mission();
		Mission restMission = MissionUtil.toRestMission(modelmission);
		assertNull("Unexpected name for new mission: ",  restMission.getName());
		assertNull("Unexpected code for new mission: ", restMission.getCode());
		logger.info("Test copy empty product OK");
		
		// Copy a mission from model to REST
		modelmission = createMission(testMissionData[0]);
		restMission = MissionUtil.toRestMission(modelmission);
		assertEquals("Unexpected ID: ", modelmission.getId(), restMission.getId().longValue());
		assertEquals("Unexpected mission code: ", modelmission.getCode(),restMission.getCode());
		assertEquals("Unexpected mission name: ", modelmission.getName(),restMission.getName());
		assertEquals("Unexpected Spacecrafts length: ", modelmission.getSpacecrafts().size(),restMission.getSpacecrafts().size());

		logger.info("Test copy model to REST OK");
		
		// Copy a product from REST to model
		de.dlr.proseo.model.Mission copiedModelMission = MissionUtil.toModelMission(restMission);
		assertEquals("ID not preserved: ", modelmission.getId(), copiedModelMission.getId());
		assertEquals("Code not preserved: ", modelmission.getCode(), copiedModelMission.getCode());
		assertEquals("Name not preserved: ", modelmission.getName(), copiedModelMission.getName());
		assertEquals("Number of Spacecrafts not preserved: ", modelmission.getSpacecrafts().size(),copiedModelMission.getSpacecrafts().size());

		logger.info("Test copy REST to model OK");
	}

}
