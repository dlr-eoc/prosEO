/**
 * SimpleSelectionRuleTest.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.util.SelectionRule;

/**
 * Test class for SimpleSelectionRule
 * 
 * @author Dr. Thomas Bassler
 */
public class SimpleSelectionRuleTest {
	
	/** Required date format for JPQL and SQL queries */
    private static final DateTimeFormatter DATEFORMAT_SQL = DateTimeFormatter.ofPattern("yyyy'-'MM'-'dd' 'HH:mm:ss.SSSSSS").withZone(ZoneId.of("UTC"));
	
	/* Static test data */
	private static final String TEST_MISSION_CODE = "S5P";
	private static final String TEST_PRODUCT_TYPE = "AUX_CH4";
	private static final Long TEST_PRODUCT_CLASS_ID = 4711L;
	private static final String TEST_PRODUCT_TYPE_IR = "L1B_IR/fileClass:UVN,revision:2.0";
	private static final Long TEST_PRODUCT_CLASS_IR_ID = 815L;
	private static final String[] selectionRuleStrings = {
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(1 H, 1 H) MANDATORY",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValIntersect(1 H, 1 H) MANDATORY",
			"FOR " + TEST_PRODUCT_TYPE_IR + " SELECT LatestValidity MANDATORY",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersect(1 H, 1 H) OR LatestValidity OPTIONAL",
			"FOR " + TEST_PRODUCT_TYPE_IR + " SELECT ValIntersect(1 H, 1 H) OR LatestValIntersect(1 H, 1 H) OPTIONAL",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValidityClosest(1 H, 1 H)",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT LatestValCover(1 H, 1 H)",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ClosestStartValidity(1 H, 1 H)",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ClosestStopValidity(1 H, 1 H)",
			"FOR " + TEST_PRODUCT_TYPE_IR + " SELECT LatestStartValidity",
			"FOR " + TEST_PRODUCT_TYPE_IR + " SELECT LatestStopValidity",
			"FOR " + TEST_PRODUCT_TYPE + " SELECT ValIntersectWithoutDuplicates(1 H, 1 H)",
			"FOR " + TEST_PRODUCT_TYPE_IR + " SELECT LastCreated"
	};
	private static final Instant TEST_START_TIME = Instant.parse("2016-11-02T00:00:00Z");
	private static final Instant TEST_STOP_TIME = Instant.parse("2016-11-02T00:00:05Z");

	private static final String EXPECTED_START_TIME = DATEFORMAT_SQL.format(TEST_START_TIME.minusSeconds(3600));
	private static final String EXPECTED_STOP_TIME = DATEFORMAT_SQL.format(TEST_STOP_TIME.plusSeconds(3600));
	private static final String EXPECTED_CENTRE_TIME = DATEFORMAT_SQL.format(
			TEST_START_TIME.minusSeconds(3600).plusSeconds(Duration.between(TEST_START_TIME.minusSeconds(3600), TEST_STOP_TIME.plusSeconds(3600)).getSeconds() / 2));
	
	private static final String[] expectedJpqlQueries = {
			"select p from Product p where (p.productClass.id = 4711 and "
				+ "p.sensingStartTime <= '" + EXPECTED_STOP_TIME + "' and "
				+ "p.sensingStopTime >= '" + EXPECTED_START_TIME + "')",
			"select p from Product p where (p.productClass.id = 4711 and "
				+ "p.sensingStartTime <= '" + EXPECTED_STOP_TIME + "' and "
				+ "p.sensingStopTime >= '" + EXPECTED_START_TIME + "' and "
				+ "p.generationTime >= "
					+ "(select max(p2.generationTime) from Product p2 where "
					+ "p2.productClass.id = 4711 and "
					+ "p2.sensingStartTime <= '" + EXPECTED_STOP_TIME + "' and "
					+ "p2.sensingStopTime >= '" + EXPECTED_START_TIME + "'))",
			"select p from Product p "
				+ "join p.parameters pp0 "
				+ "where (p.productClass.id = 815 and "
				+ "p.sensingStartTime >= "
					+ "(select max(p2.sensingStartTime) from Product p2 "
					+ "join p2.parameters pp20 "
					+ "where p2.productClass.id = 815 and "
					+ "p2.fileClass = 'UVN' and "
					+ "key(pp20) = 'revision' and pp20.parameterValue = '2.0') "
				+ "and "
				+ "p.fileClass = 'UVN' and "
				+ "key(pp0) = 'revision' and pp0.parameterValue = '2.0')",
			"select p from Product p where (p.productClass.id = 4711 and "
				+ "(p.sensingStartTime <= '" + EXPECTED_STOP_TIME + "' and "
				+ "p.sensingStopTime >= '" + EXPECTED_START_TIME + "' or "
				+ "p.sensingStartTime >= "
					+ "(select max(p2.sensingStartTime) from Product p2 "
					+ "where p2.productClass.id = 4711)))",
			"select p from Product p "
				+ "join p.parameters pp0 "
				+ "where (p.productClass.id = 815 and "
				+ "(p.sensingStartTime <= '" + EXPECTED_STOP_TIME + "' and "
				+ "p.sensingStopTime >= '" + EXPECTED_START_TIME + "' or "
				+ "p.sensingStartTime <= '" + EXPECTED_STOP_TIME + "' and "
				+ "p.sensingStopTime >= '" + EXPECTED_START_TIME + "' and "
				+ "p.generationTime >= "
					+ "(select max(p2.generationTime) from Product p2 "
					+ "join p2.parameters pp20 "
					+ "where p2.productClass.id = 815 and "
					+ "p2.sensingStartTime <= '" + EXPECTED_STOP_TIME + "' and "
					+ "p2.sensingStopTime >= '" + EXPECTED_START_TIME + "' and "
					+ "p2.fileClass = 'UVN' and "
					+ "key(pp20) = 'revision' and pp20.parameterValue = '2.0')) "
				+ "and "
				+ "p.fileClass = 'UVN' and "
				+ "key(pp0) = 'revision' and pp0.parameterValue = '2.0')",
			"select p from Product p where (p.productClass.id = 4711 and "
				+ "(p.sensingStartTime <= '" + EXPECTED_CENTRE_TIME + "' and "
				+ "p.sensingStartTime >= (select max(p2.sensingStartTime) from Product p2 where p2.productClass.id = 4711 and "
				+ "p2.sensingStartTime <= '" + EXPECTED_CENTRE_TIME + "') or "
				+ "p.sensingStartTime > '" + EXPECTED_CENTRE_TIME + "' and "
				+ "p.sensingStartTime < "
					+ "(select min(p2.sensingStartTime) from Product p2 "
					+ "where p2.productClass.id = 4711 and "
					+ "p2.sensingStartTime > '" + EXPECTED_CENTRE_TIME + "')))",
			"select p from Product p where (p.productClass.id = 4711 and "
				+ "p.sensingStartTime <= '" + EXPECTED_START_TIME + "' and "
				+ "p.sensingStopTime >= '" + EXPECTED_STOP_TIME + "' and "
				+ "p.generationTime >= "
					+ "(select max(p2.generationTime) from Product p2 where "
					+ "p2.productClass.id = 4711 and "
					+ "p2.sensingStartTime <= '" + EXPECTED_START_TIME + "' and "
					+ "p2.sensingStopTime >= '" + EXPECTED_STOP_TIME + "'))",
			"select p from Product p where (p.productClass.id = 4711 and "
				+ "(p.sensingStartTime <= '" + EXPECTED_CENTRE_TIME + "' and "
				+ "p.sensingStartTime >= "
					+ "(select max(p2.sensingStartTime) from Product p2 where p2.productClass.id = 4711 and "
					+ "p2.sensingStartTime <= '" + EXPECTED_CENTRE_TIME + "') "
				+ "or "
				+ "p.sensingStartTime > '" + EXPECTED_CENTRE_TIME + "' and "
				+ "p.sensingStartTime < "
					+ "(select min(p2.sensingStartTime) from Product p2 where p2.productClass.id = 4711 and "
					+ "p2.sensingStartTime > '" + EXPECTED_CENTRE_TIME + "')))",
			"select p from Product p where (p.productClass.id = 4711 and "
				+ "(p.sensingStopTime <= '" + EXPECTED_CENTRE_TIME + "' and "
				+ "p.sensingStopTime >= "
					+ "(select max(p2.sensingStopTime) from Product p2 where p2.productClass.id = 4711 and "
					+ "p2.sensingStopTime <= '" + EXPECTED_CENTRE_TIME + "') "
				+ "or "
				+ "p.sensingStopTime > '" + EXPECTED_CENTRE_TIME + "' and "
				+ "p.sensingStopTime < "
					+ "(select min(p2.sensingStopTime) from Product p2 where p2.productClass.id = 4711 and "
					+ "p2.sensingStopTime > '" + EXPECTED_CENTRE_TIME + "')))",
			"select p from Product p "
				+ "join p.parameters pp0 "
				+ "where (p.productClass.id = 815 and "
				+ "p.sensingStartTime >= "
					+ "(select max(p2.sensingStartTime) from Product p2 "
					+ "join p2.parameters pp20 "
					+ "where p2.productClass.id = 815 and "
					+ "p2.fileClass = 'UVN' and "
					+ "key(pp20) = 'revision' and pp20.parameterValue = '2.0') "
				+ "and "
				+ "p.fileClass = 'UVN' and "
				+ "key(pp0) = 'revision' and pp0.parameterValue = '2.0')",
			"select p from Product p "
				+ "join p.parameters pp0 "
				+ "where (p.productClass.id = 815 and "
				+ "p.sensingStopTime >= "
					+ "(select max(p2.sensingStopTime) from Product p2 "
					+ "join p2.parameters pp20 "
					+ "where p2.productClass.id = 815 and "
					+ "p2.fileClass = 'UVN' and "
					+ "key(pp20) = 'revision' and pp20.parameterValue = '2.0') "
				+ "and "
				+ "p.fileClass = 'UVN' and "
				+ "key(pp0) = 'revision' and pp0.parameterValue = '2.0')",
			"select p from Product p where (p.productClass.id = 4711 and "
				+ "p.sensingStartTime <= '" + EXPECTED_STOP_TIME + "' and "
				+ "p.sensingStopTime >= '" + EXPECTED_START_TIME + "')",
			"select p from Product p "
				+ "join p.parameters pp0 "
				+ "where (p.productClass.id = 815 and "
				+ "p.generationTime >= "
					+ "(select max(p2.generationTime) from Product p2 "
					+ "join p2.parameters pp20 "
					+ "where p2.productClass.id = 815 and "
					+ "p2.fileClass = 'UVN' and "
					+ "key(pp20) = 'revision' and pp20.parameterValue = '2.0') "
				+ "and "
				+ "p.fileClass = 'UVN' and "
				+ "key(pp0) = 'revision' and pp0.parameterValue = '2.0')"
	};

	private static final String[] expectedSqlQueries = {
			"SELECT * FROM product p WHERE (p.product_class_id = 4711 AND "
				+ "p.sensing_start_time <= '" + EXPECTED_STOP_TIME + "' AND "
				+ "p.sensing_stop_time >= '" + EXPECTED_START_TIME + "')",
			"SELECT * FROM product p WHERE (p.product_class_id = 4711 AND "
				+ "p.sensing_start_time <= '" + EXPECTED_STOP_TIME + "' AND "
				+ "p.sensing_stop_time >= '" + EXPECTED_START_TIME + "' AND "
				+ "p.generation_time >= "
					+ "(SELECT MAX(p2.generation_time) FROM product p2 WHERE "
					+ "p2.product_class_id = 4711 AND "
					+ "p2.sensing_start_time <= '" + EXPECTED_STOP_TIME + "' AND "
					+ "p2.sensing_stop_time >= '" + EXPECTED_START_TIME + "'))",
			"SELECT * FROM product p "
				+ "JOIN product_parameters pp0 ON p.id = pp0.product_id "
				+ "WHERE (p.product_class_id = 815 AND "
				+ "p.sensing_start_time >= "
					+ "(SELECT MAX(p2.sensing_start_time) FROM product p2 "
					+ "JOIN product_parameters pp20 ON p2.id = pp20.product_id "
					+ "WHERE p2.product_class_id = 815 AND "
					+ "p2.file_class = 'UVN' AND "
					+ "pp20.parameters_key = 'revision' AND pp20.parameter_value = '2.0') "
				+ "AND "
				+ "p.file_class = 'UVN' AND "
				+ "pp0.parameters_key = 'revision' AND pp0.parameter_value = '2.0')",
			"SELECT * FROM product p WHERE (p.product_class_id = 4711 AND "
				+ "(p.sensing_start_time <= '" + EXPECTED_STOP_TIME + "' AND "
				+ "p.sensing_stop_time >= '" + EXPECTED_START_TIME + "' OR "
				+ "p.sensing_start_time >= "
					+ "(SELECT MAX(p2.sensing_start_time) FROM product p2 WHERE "
					+ "p2.product_class_id = 4711)))",
			"SELECT * FROM product p "
				+ "JOIN product_parameters pp0 ON p.id = pp0.product_id "
				+ "WHERE (p.product_class_id = 815 AND "
				+ "(p.sensing_start_time <= '" + EXPECTED_STOP_TIME + "' AND "
				+ "p.sensing_stop_time >= '" + EXPECTED_START_TIME + "' OR "
				+ "p.sensing_start_time <= '" + EXPECTED_STOP_TIME + "' AND "
				+ "p.sensing_stop_time >= '" + EXPECTED_START_TIME + "' AND "
				+ "p.generation_time >= "
					+ "(SELECT MAX(p2.generation_time) FROM product p2 "
					+ "JOIN product_parameters pp20 ON p2.id = pp20.product_id "
					+ "WHERE p2.product_class_id = 815 AND "
					+ "p2.sensing_start_time <= '" + EXPECTED_STOP_TIME + "' AND "
					+ "p2.sensing_stop_time >= '" + EXPECTED_START_TIME + "' AND "
					+ "p2.file_class = 'UVN' AND "
					+ "pp20.parameters_key = 'revision' AND pp20.parameter_value = '2.0')) "
				+ "AND "
				+ "p.file_class = 'UVN' AND "
				+ "pp0.parameters_key = 'revision' AND pp0.parameter_value = '2.0')",
			"SELECT * FROM product p WHERE (p.product_class_id = 4711 AND "
				+ "(p.sensing_start_time <= '" + EXPECTED_CENTRE_TIME + "' AND "
				+ "p.sensing_start_time >= "
					+ "(SELECT MAX(p2.sensing_start_time) FROM product p2 WHERE p2.product_class_id = 4711 AND "
					+ "p2.sensing_start_time <= '" + EXPECTED_CENTRE_TIME + "') "
				+ "OR "
				+ "p.sensing_start_time > '" + EXPECTED_CENTRE_TIME + "' AND "
				+ "p.sensing_start_time < "
					+ "(SELECT MIN(p2.sensing_start_time) FROM product p2 WHERE p2.product_class_id = 4711 AND "
					+ "p2.sensing_start_time > '" + EXPECTED_CENTRE_TIME + "')))",
			"SELECT * FROM product p WHERE (p.product_class_id = 4711 AND "
				+ "p.sensing_start_time <= '" + EXPECTED_START_TIME + "' AND "
				+ "p.sensing_stop_time >= '" + EXPECTED_STOP_TIME + "' AND "
				+ "p.generation_time >= "
					+ "(SELECT MAX(p2.generation_time) FROM product p2 WHERE "
					+ "p2.product_class_id = 4711 AND "
					+ "p2.sensing_start_time <= '" + EXPECTED_START_TIME + "' AND "
					+ "p2.sensing_stop_time >= '" + EXPECTED_STOP_TIME + "'))",
			"SELECT * FROM product p WHERE (p.product_class_id = 4711 AND "
				+ "(p.sensing_start_time <= '" + EXPECTED_CENTRE_TIME + "' AND "
				+ "p.sensing_start_time >= "
					+ "(SELECT MAX(p2.sensing_start_time) FROM product p2 WHERE p2.product_class_id = 4711 AND "
					+ "p2.sensing_start_time <= '" + EXPECTED_CENTRE_TIME + "') "
				+ "OR "
				+ "p.sensing_start_time > '" + EXPECTED_CENTRE_TIME + "' AND "
				+ "p.sensing_start_time < "
					+ "(SELECT MIN(p2.sensing_start_time) FROM product p2 WHERE p2.product_class_id = 4711 AND "
					+ "p2.sensing_start_time > '" + EXPECTED_CENTRE_TIME + "')))",
			"SELECT * FROM product p WHERE (p.product_class_id = 4711 AND "
				+ "(p.sensing_stop_time <= '" + EXPECTED_CENTRE_TIME + "' AND "
				+ "p.sensing_stop_time >= "
					+ "(SELECT MAX(p2.sensing_stop_time) FROM product p2 WHERE p2.product_class_id = 4711 AND "
					+ "p2.sensing_stop_time <= '" + EXPECTED_CENTRE_TIME + "') "
				+ "OR "
				+ "p.sensing_stop_time > '" + EXPECTED_CENTRE_TIME + "' AND "
				+ "p.sensing_stop_time < "
					+ "(SELECT MIN(p2.sensing_stop_time) FROM product p2 WHERE p2.product_class_id = 4711 AND "
					+ "p2.sensing_stop_time > '" + EXPECTED_CENTRE_TIME + "')))",
			"SELECT * FROM product p "
				+ "JOIN product_parameters pp0 ON p.id = pp0.product_id "
				+ "WHERE (p.product_class_id = 815 AND "
				+ "p.sensing_start_time >= "
					+ "(SELECT MAX(p2.sensing_start_time) FROM product p2 "
					+ "JOIN product_parameters pp20 ON p2.id = pp20.product_id "
					+ "WHERE p2.product_class_id = 815 AND "
					+ "p2.file_class = 'UVN' AND "
					+ "pp20.parameters_key = 'revision' AND pp20.parameter_value = '2.0') "
				+ "AND "
				+ "p.file_class = 'UVN' AND "
				+ "pp0.parameters_key = 'revision' AND pp0.parameter_value = '2.0')",
			"SELECT * FROM product p "
				+ "JOIN product_parameters pp0 ON p.id = pp0.product_id "
				+ "WHERE (p.product_class_id = 815 AND "
				+ "p.sensing_stop_time >= "
					+ "(SELECT MAX(p2.sensing_stop_time) FROM product p2 "
					+ "JOIN product_parameters pp20 ON p2.id = pp20.product_id "
					+ "WHERE p2.product_class_id = 815 AND "
					+ "p2.file_class = 'UVN' AND "
					+ "pp20.parameters_key = 'revision' AND pp20.parameter_value = '2.0') "
				+ "AND "
				+ "p.file_class = 'UVN' AND "
				+ "pp0.parameters_key = 'revision' AND pp0.parameter_value = '2.0')",
			"SELECT * FROM product p WHERE (p.product_class_id = 4711 AND "
				+ "p.sensing_start_time <= '" + EXPECTED_STOP_TIME + "' AND "
				+ "p.sensing_stop_time >= '" + EXPECTED_START_TIME + "')",
			"SELECT * FROM product p "
				+ "JOIN product_parameters pp0 ON p.id = pp0.product_id "
				+ "WHERE (p.product_class_id = 815 AND "
				+ "p.generation_time >= "
					+ "(SELECT MAX(p2.generation_time) FROM product p2 "
					+ "JOIN product_parameters pp20 ON p2.id = pp20.product_id "
					+ "WHERE p2.product_class_id = 815 AND "
					+ "p2.file_class = 'UVN' AND "
					+ "pp20.parameters_key = 'revision' AND pp20.parameter_value = '2.0') "
				+ "AND "
				+ "p.file_class = 'UVN' AND "
				+ "pp0.parameters_key = 'revision' AND pp0.parameter_value = '2.0')"
	};
	
	/* Mapping from Product attributes to SQL column names */
	private static Map<String, String> productColumnMapping = new HashMap<>();

	/* Test objects */
	private Mission mission = new Mission();
	private ProductClass productClassCH4 = new ProductClass();
	private ProductClass productClassIR = new ProductClass();

	/** The logger for this class */
	private static final Logger logger = LoggerFactory.getLogger(SimpleSelectionRuleTest.class);
	
	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		productColumnMapping.put("fileClass", "file_class");
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Create the necessary reference objects
	 * 
	 * @throws java.lang.Exception if an error occurs
	 */
	@Before
	public void setUp() throws Exception {
		// Create required mission and product classes
		mission.setCode(TEST_MISSION_CODE);
		mission.getProductClasses().addAll(Arrays.asList(productClassCH4, productClassIR));

		productClassCH4.setId(TEST_PRODUCT_CLASS_ID);
		productClassCH4.setProductType(TEST_PRODUCT_TYPE);
		productClassCH4.setMission(mission);

		productClassIR.setId(TEST_PRODUCT_CLASS_IR_ID);
		productClassIR.setProductType(TEST_PRODUCT_TYPE_IR.split("/")[0]);
		productClassIR.setMission(mission);
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link de.dlr.proseo.model.SimpleSelectionRule#asJpqlQuery(java.time.Instant, java.time.Instant)}.
	 */
	@Test
	public final void testAsJpqlQuery() {
		for (int i = 0; i < selectionRuleStrings.length; ++i) {
			try {
				SelectionRule selectionRule = SelectionRule.parseSelectionRule(productClassCH4, selectionRuleStrings[i]);
				List<SimpleSelectionRule> simpleRules = selectionRule.getSimpleRules();
				for (SimpleSelectionRule simpleSelectionRule: simpleRules) {
					
					assertEquals("Unexpected JPQL query for selection rule string " + i, expectedJpqlQueries[i], 
							simpleSelectionRule.asJpqlQuery(TEST_START_TIME, TEST_STOP_TIME, null));
				}
			} catch (IllegalArgumentException | ParseException e) {
				e.printStackTrace();
				fail("Unexpected exception in SelectionRule#parseSelectionRule(ProductClass, String)");
			}
		}
		
		logger.info("OK: Test for asJpqlQuery completed");
	}

	/**
	 * Test method for {@link de.dlr.proseo.model.SimpleSelectionRule#asSqlQuery(java.time.Instant, java.time.Instant)}.
	 */
	@Test
	public final void testAsSqlQuery() {
		for (int i = 0; i < selectionRuleStrings.length; ++i) {
			try {
				SelectionRule selectionRule = SelectionRule.parseSelectionRule(productClassCH4, selectionRuleStrings[i]);
				List<SimpleSelectionRule> simpleRules = selectionRule.getSimpleRules();
				for (SimpleSelectionRule simpleSelectionRule: simpleRules) {
					
					assertEquals("Unexpected SQL query for selection rule string " + i, expectedSqlQueries[i], 
							simpleSelectionRule.asSqlQuery(TEST_START_TIME, TEST_STOP_TIME, null, productColumnMapping, null, null));
				}
			} catch (IllegalArgumentException | ParseException e) {
				e.printStackTrace();
				fail("Unexpected exception in SelectionRule#parseSelectionRule(ProductClass, String)");
			}
		}
		
		logger.info("OK: Test for asSqlQuery completed");
	}

}
