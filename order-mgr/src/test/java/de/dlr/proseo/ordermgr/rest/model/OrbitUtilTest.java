package de.dlr.proseo.ordermgr.rest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.rest.model.RestOrbit;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * @author Ranjitha Vignesh
 */
public class OrbitUtilTest {

	/* Test orbits */
	private static String[][] testOrbitData = {
			// mission_id, mission_version, mission_code, mission_name,spacecraft_id,
			// spacecraft_version,spacecraft_code,spacecraft_name, orbit_id, orbit_version, orbit_number, start_time, stop_time,
			{ "0", "0", "ABCe", "ABCD Testing", "13", "1", "S_TDX1", "Tandem-X", "0", "0", "14", "2019-08-29T22:49:21.074395",
					"2019-08-29T22:49:21.074395" },
			{ "11", "11", "DEFg", "DefrostMission", "14", "2", "S_TDX2", "Tandem-X", "0", "0", "12", "2019-08-12T22:49:21.070095",
					"2019-08-13T22:49:21.074395" },
			{ "12", "12", "XY1Z", "XYZ Testing", "15", "3", "S_TDX3", "Terrasar-X", "0", "0", "13", "2019-08-09T22:49:21.074395",
					"2019-08-10T22:49:21.074395" }, };

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrbitUtilTest.class);

	/**
	 * Create an orbit from a data array
	 *
	 * @param testData an array of Strings representing the orbit to create
	 * @return a Orbit with its attributes set to the input data
	 */
	private Orbit createOrbit(String[] testData) {
		logger.info("... creating orbit ");
		Mission testMission = new Mission();
		Spacecraft testSpacecraft = new Spacecraft();
		Orbit testOrbit = new Orbit();

		// Adding mission parameters
		testMission.setId(Long.parseLong(testData[0]));
		testMission.setCode(testData[2]);
		testMission.setName(testData[3]);

		// adding Spacecraft parameters
		testSpacecraft.setId(Long.parseLong(testData[4]));
		testSpacecraft.setMission(testMission);
		testSpacecraft.setCode(testData[6]);
		testSpacecraft.setName(testData[7]);

		// Adding orbit parameters
		testOrbit.setOrbitNumber(Integer.valueOf(testData[10]));
		testOrbit.setStartTime(Instant.from(OrbitTimeFormatter.parse(testData[11])));
		testOrbit.setStopTime(Instant.from(OrbitTimeFormatter.parse(testData[12])));
		testOrbit.setSpacecraft(testSpacecraft);

		logger.info("Created test orbit {}", testOrbit.getId());
		return testOrbit;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
	}

	@Test
	public final void test() {
		// Create an empty product
		Orbit modelorbit = new Orbit();
		RestOrbit restOrbit = new RestOrbit();

		assertNull(restOrbit.getOrbitNumber(), "Unexpected number for new orbit: ");
		assertNull(restOrbit.getSpacecraftCode(), "Unexpected Spacecraft code for new orbit: ");
		logger.info("Test copy empty orbit OK");

		// Copy a orbit from model to REST
		modelorbit = createOrbit(testOrbitData[0]);
		restOrbit = OrbitUtil.toRestOrbit(modelorbit);
		assertEquals(modelorbit.getId(), restOrbit.getId(), "Unexpected ID: ");
		assertEquals(Long.valueOf(modelorbit.getOrbitNumber()), restOrbit.getOrbitNumber(), "Unexpected orbit number: ");
		// assertEquals(modelorbit.getSpacecraft().getCode(), restOrbit.getSpacecraftCode(), "Unexpected Spacecrafts: ");
		assertEquals(OrbitTimeFormatter.format(modelorbit.getStartTime()), restOrbit.getStartTime(), "Unexpected start time: ");
		assertEquals(OrbitTimeFormatter.format(modelorbit.getStopTime()), restOrbit.getStopTime(), "Unexpected stop time: ");

		logger.info("Test copy model to REST OK");

		// Copy a product from REST to model

		de.dlr.proseo.model.Orbit copiedModelOrbit = OrbitUtil.toModelOrbit(restOrbit);

		assertEquals(modelorbit.getId(), copiedModelOrbit.getId(), "ID not preserved: ");
		assertEquals(modelorbit.getOrbitNumber(), copiedModelOrbit.getOrbitNumber(), "Orbit number not preserved: ");
		// assertEquals(modelorbit.getSpacecraft().getCode(), copiedModelOrbit.getSpacecraft().getCode(), "Spacecraft code not
		// preserved: ");
		logger.info("Test copy REST to model OK");
	}
}
