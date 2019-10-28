/**
 * ProductTest.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import static org.junit.Assert.*;

import java.time.Instant;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.dlr.proseo.model.Parameter.ParameterType;

/**
 * Tests the Product class (except for getter and setter methods)
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class ProductTest {
	
	/* Test data */
	private final String TEST_MISSION_CODE = "S5P";
	private final String TEST_PRODUCT_TYPE = "NPP";
	private final String TEST_MISSION_TYPE = "L2__NPP___";
	private final Instant TEST_START_TIME = Instant.parse("2019-10-24T22:09:01.234567Z");
	private final Instant TEST_STOP_TIME = Instant.parse("2019-10-24T23:39:12.345678Z");
	private final Instant TEST_GENERATION_TIME = Instant.parse("2019-10-25T00:01:02Z");
	private final String TEST_FILE_CLASS = "RPRO";
	private final String TEST_MODE = "OFFL";
	private final String TEST_PROCESSOR_VERSION = "01.02.03";
	private final String TEST_COLLECTION_NUMBER = "99";
	private final Integer TEST_ORBIT_NUMBER = 5432;
	private final String TEST_FILENAME_TEMPLATE =
			"S5P_${fileClass}_${productClass.missionType}_" +
			"${T(java.time.format.DateTimeFormatter).ofPattern(\"uuuuMMdd'T'HHmmss\").withZone(T(java.time.ZoneId).of(\"UTC\")).format(sensingStartTime)}_" +
			"${T(java.time.format.DateTimeFormatter).ofPattern(\"uuuuMMdd'T'HHmmss\").withZone(T(java.time.ZoneId).of(\"UTC\")).format(sensingStopTime)}_" +
			"${(new java.text.DecimalFormat(\"00000\")).format(orbit.orbitNumber)}_" +
			"${parameters.get(\"copernicusCollection\").getParameterValue()}_" +
			"${configuredProcessor.processor.processorVersion.replaceAll(\"\\.\", \"\")}_" +
			"${T(java.time.format.DateTimeFormatter).ofPattern(\"uuuuMMdd'T'HHmmss\").withZone(T(java.time.ZoneId).of(\"UTC\")).format(generationTime)}.nc";
	private final String TEST_SIMPLE_TEMPLATE = "S5P_${fileClass}_${productClass.missionType}";
	private final String TEST_EXPRESSION_ONLY_TEMPLATE = "${productClass.mission.code}${productClass.missionType}${fileClass}${mode}";
	
	private final String EXPECTED_FILENAME = "S5P_RPRO_L2__NPP____20191024T220901_20191024T233912_05432_99_010203_20191025T000102.nc";
	private final String EXPECTED_SIMPLENAME = "S5P_RPRO_L2__NPP___";
	private final String EXPECTED_EXPRESSION_ONLY_NAME = "S5PL2__NPP___RPROOFFL";
	
	/** The logger for this class */
	private static final Logger logger = LoggerFactory.getLogger(ProductTest.class);
	

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
	public final void testGenerateFilename() {
		Mission mission = new Mission();
		mission.setCode(TEST_MISSION_CODE);
		
		ProductClass productClass = new ProductClass();
		productClass.setMission(mission);
		productClass.setProductType(TEST_PRODUCT_TYPE);
		productClass.setMissionType(TEST_MISSION_TYPE);
		
		Orbit orbit = new Orbit();
		orbit.setOrbitNumber(TEST_ORBIT_NUMBER);
		
		Processor processor = new Processor();
		processor.setProcessorVersion(TEST_PROCESSOR_VERSION);
		
		ConfiguredProcessor configuredProcessor = new ConfiguredProcessor();
		configuredProcessor.setProcessor(processor);
		
		Product product = new Product();
		product.setProductClass(productClass);
		product.setFileClass(TEST_FILE_CLASS);
		product.setMode(TEST_MODE);
		product.setSensingStartTime(TEST_START_TIME);
		product.setSensingStopTime(TEST_STOP_TIME);
		product.setGenerationTime(TEST_GENERATION_TIME);
		product.getParameters().put("copernicusCollection", (new Parameter()).init(ParameterType.STRING, TEST_COLLECTION_NUMBER));
		product.setConfiguredProcessor(configuredProcessor);
		product.setOrbit(orbit);
		
		mission.setProductFileTemplate(TEST_SIMPLE_TEMPLATE);
		assertEquals("Unexpected resolution of simple template", EXPECTED_SIMPLENAME, product.generateFilename());
		
		mission.setProductFileTemplate(TEST_FILENAME_TEMPLATE);
		assertEquals("Unexpected resolution of filename template", EXPECTED_FILENAME, product.generateFilename());
		
		mission.setProductFileTemplate(TEST_EXPRESSION_ONLY_TEMPLATE);
		assertEquals("Unexpected resolution of filename template", EXPECTED_EXPRESSION_ONLY_NAME, product.generateFilename());
		
		logger.info("OK: Test for Product#generateFileName() completed");
	}

}
