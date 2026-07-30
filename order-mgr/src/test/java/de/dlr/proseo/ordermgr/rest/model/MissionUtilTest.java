/**
 * MissionUtilTest.java
 *
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.rest.model.RestMission;

/**
 * @author Ranjitha Vignesh
 */
public class MissionUtilTest {

	/* Test Missions */
	private static String[][] testMissionData = {
			// id, version, mission_code, mission_name,fileClass, processinMode, spacecraft_version,spacecraft_code,spacecraft_name
			{ "0", "0", "ABCe", "ABCD Testing", "TEST", "NRTI", "1", "S_TDX1", "Tandom-X" },
			{ "11", "11", "DEFg", "DefrostMission", "OPER", "OFFL", "2", "S_TDX2", "Tandom-X" },
			{ "12", "12", "XY1Z", "XYZ Testing", "TEST", "OFFL", "3", "S_TDX3", "Tandom-X" } };

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(MissionUtilTest.class);

	/**
	 * Create a mission from a data array
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
		testMission.getFileClasses().clear();
		testMission.getFileClasses().add(testData[4]);
		testMission.getProcessingModes().add(testData[5]);

		// adding Spacecraft parameters
		testSpacecraft.setMission(testMission);
		testSpacecraft.incrementVersion();
		testSpacecraft.setCode(testData[6]);
		testSpacecraft.setName(testData[7]);

		logger.info("Created test mission {}", testMission.getId());
		return testMission;
	}

	@Test
	public final void test() {
		// Create an empty product
		Mission modelmission = new Mission();
		RestMission restMission = MissionUtil.toRestMission(modelmission);
		assertNull(restMission.getName(), "Unexpected name for new mission: ");
		assertNull(restMission.getCode(), "Unexpected code for new mission: ");
		logger.info("Test copy empty mission OK");

		// Copy a mission from model to REST
		modelmission = createMission(testMissionData[0]);
		restMission = MissionUtil.toRestMission(modelmission);
		assertEquals(modelmission.getId(), restMission.getId(), "Unexpected ID: ");
		assertEquals(modelmission.getCode(), restMission.getCode(), "Unexpected mission code: ");
		assertEquals(modelmission.getName(), restMission.getName(), "Unexpected mission name: ");

		for (Iterator<String> it = modelmission.getProcessingModes().iterator(); it.hasNext();) {
			if (!(restMission.getProcessingModes().contains((it.next())))) {
				logger.info("Unexpected Processing Modes  ");

			}
		}
		for (Iterator<String> it = modelmission.getFileClasses().iterator(); it.hasNext();) {
			if (!(restMission.getFileClasses().contains((it.next())))) {
				logger.info("Unexpected File Classes : ");

			}
		}
		assertEquals(modelmission.getProductFileTemplate(), restMission.getProductFileTemplate(),
				"Unexpected ProductFile Template : ");

		assertEquals(modelmission.getSpacecrafts().size(), restMission.getSpacecrafts().size(), "Unexpected Spacecrafts length: ");

		logger.info("Test copy model to REST OK");

		// Copy a mission from REST to model
		Mission copiedModelMission = MissionUtil.toModelMission(restMission);
//				assertEquals(modelmission.getId(), copiedModelMission.getId(), "ID not preserved: ");
		assertEquals(modelmission.getCode(), copiedModelMission.getCode(), "Code not preserved: ");
		assertEquals(modelmission.getName(), copiedModelMission.getName(), "Name not preserved: ");
		assertEquals(modelmission.getSpacecrafts().size(), copiedModelMission.getSpacecrafts().size(),
				"Number of Spacecrafts not preserved: ");
		assertEquals(modelmission.getProductFileTemplate(), copiedModelMission.getProductFileTemplate(),
				"Unexpected ProductFile Template : ");

		logger.info("Test copy REST to model OK");
	}

}
