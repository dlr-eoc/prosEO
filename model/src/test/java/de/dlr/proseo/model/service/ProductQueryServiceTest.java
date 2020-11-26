/**
 * ProductQueryServiceTest.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.service;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.time.Instant;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.InputFilter;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.model.util.SelectionRule;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;

/**
 * Test class for ProductQueryService
 * 
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class ProductQueryServiceTest {

	/* Various static test data */
	private static final String TEST_CODE = "ABC";
	private static final String TEST_TARGET_PRODUCT_TYPE = "L2__FRESCO_";
	private static final String TEST_SOURCE_PRODUCT_TYPE = "L1B________";
	private static final String TEST_MODE = "OFFL";
	private static final String TEST_SELECTION_RULE = "FOR L1B________/revision:1,mode:OFFL SELECT ValIntersect(0, 0)";
	private static final String TEST_SELECTION_RULE_MINCOVER = "FOR L1B________ SELECT ValIntersect(0, 0) MINCOVER(70)";
	private static final Instant TEST_START_TIME_EARLY = Instant.parse("2009-08-29T23:00:00Z");
	private static final Instant TEST_STOP_TIME_EARLY = Instant.parse("2009-08-30T01:00:00Z");
	private static final Instant TEST_START_TIME_LATE = Instant.parse("2009-08-30T01:00:00Z");
	private static final Instant TEST_STOP_TIME_LATE = Instant.parse("2009-08-30T03:00:00Z");
	private static final String TEST_FACILITY = "Test Facility";

	/* Test products */
	private static String[][] testProductData = {
		// id, version, mission code, product class, mode, sensing start, sensing stop, generation, revision (parameter)
		{ "0", "1", TEST_CODE, TEST_SOURCE_PRODUCT_TYPE, TEST_MODE, "2009-08-29T22:49:21.074395", "2009-08-30T00:19:33.946628", "2009-10-05T10:12:39.000000", "01" },
		{ "0", "1", TEST_CODE, TEST_SOURCE_PRODUCT_TYPE, TEST_MODE, "2009-08-30T00:19:33.946628", "2009-08-30T01:49:46.482753", "2009-10-05T10:13:22.000000", "01" },
		{ "0", "1", "TDM", "DEM", null, "2009-08-30T00:19:33.946628", "2009-08-30T01:49:46.482753", "2009-10-05T10:13:22.000000", "02" }
	};

	@Autowired
	private ProductQueryService queryService;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductQueryServiceTest.class);
	
	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	    TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Create a product from a data array
	 * 
	 * @param testData an array of Strings representing the product to create
	 * @param facility the processing facility to search the product file in
	 * @return a Product with its attributes set to the input data
	 */
	private Product createProduct(String[] testData, ProcessingFacility facility) {
		Product testProduct = new Product();
		
		testProduct.setProductClass(
				RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(testData[2], testData[3]));

		logger.info("... creating product with product type {}", (null == testProduct.getProductClass() ? null : testProduct.getProductClass().getProductType()));
		testProduct.setUuid(UUID.randomUUID());
		testProduct.setMode(testData[4]);
		testProduct.setSensingStartTime(Instant.from(OrbitTimeFormatter.parse(testData[5])));
		testProduct.setSensingStopTime(Instant.from(OrbitTimeFormatter.parse(testData[6])));
		testProduct.setGenerationTime(Instant.from(OrbitTimeFormatter.parse(testData[7])));
		testProduct.getParameters().put(
				"revision", new Parameter().init(ParameterType.INTEGER, Integer.parseInt(testData[8])));
		ProductFile testProductFile = new ProductFile();
		testProductFile.setProcessingFacility(facility);
		testProduct.getProductFile().add(testProductFile);
		testProduct = RepositoryService.getProductRepository().save(testProduct);
		
		logger.info("Created test product {} with start time = {} and stop time = {}", testProduct.getId(), testProduct.getSensingStartTime().toString(), testProduct.getSensingStopTime().toString());
		return testProduct;
	}
	
	/**
	 * Test method for {@link de.dlr.proseo.model.service.ProductQueryService#executeQuery(de.dlr.proseo.model.ProductQuery, boolean)}.
	 */
	@Test
	public final void testExecuteQuery() {
		
		// Create test data: mission, product class, product, selection rules (with and without MINCOVER), order, job, job step
		Mission mission = RepositoryService.getMissionRepository().findByCode(TEST_CODE);
		if (null == mission) {
			logger.trace("Creating mission ...");
			mission = new Mission();
			mission.setCode(TEST_CODE);
			mission.getProcessingModes().add(TEST_MODE);
			mission = RepositoryService.getMissionRepository().save(mission);
		}
		logger.info("Using mission " + mission.getCode() + " with id " + mission.getId());
		
		ProductClass targetProdClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(TEST_CODE, TEST_TARGET_PRODUCT_TYPE);
		if (null == targetProdClass) {
			logger.trace("Creating target product class ...");
			targetProdClass = new ProductClass();
			targetProdClass.setMission(mission);
			targetProdClass.setProductType(TEST_TARGET_PRODUCT_TYPE);
			targetProdClass = RepositoryService.getProductClassRepository().save(targetProdClass);
			mission.getProductClasses().add(targetProdClass);
			//mission = RepositoryService.getMissionRepository().save(mission);
		}
		logger.info("Using target product class " + targetProdClass.getProductType() + " with id " + targetProdClass.getId());
		
		ProductClass sourceProdClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(TEST_CODE, TEST_SOURCE_PRODUCT_TYPE);
		if (null == sourceProdClass) {
			logger.trace("Creating source product class ...");
			sourceProdClass = new ProductClass();
			sourceProdClass.setMission(mission);
			sourceProdClass.setProductType(TEST_SOURCE_PRODUCT_TYPE);
			sourceProdClass = RepositoryService.getProductClassRepository().save(sourceProdClass);
			mission.getProductClasses().add(sourceProdClass);
			//mission = RepositoryService.getMissionRepository().save(mission);
		}
		logger.info("Using source product class " + sourceProdClass.getProductType() + " with id " + sourceProdClass.getId());
		
		ProcessingFacility facility = RepositoryService.getFacilityRepository().findByName(TEST_FACILITY);
		if (null == facility) {
			logger.trace("Creating processing facility ...");
			facility = new ProcessingFacility();
			facility.setName(TEST_FACILITY);
			facility = RepositoryService.getFacilityRepository().save(facility);
		}
		logger.info("Using processing facility " + facility.getName());
		
		createProduct(testProductData[0], facility);
		createProduct(testProductData[1], facility);
		
		logger.trace("Number of products in database: " + RepositoryService.getProductRepository().count());
		
		ProcessingOrder order = new ProcessingOrder();
		InputFilter inputFilter = new InputFilter();
		inputFilter.getFilterConditions().put("revision", (new Parameter()).init(ParameterType.INTEGER, 1));
		order.getInputFilters().put(sourceProdClass, inputFilter);
		
		Job jobEarly = new Job();
		jobEarly.setProcessingOrder(order);
		jobEarly.setProcessingFacility(facility);
		jobEarly.setStartTime(TEST_START_TIME_EARLY);
		jobEarly.setStopTime(TEST_STOP_TIME_EARLY);
		JobStep jobStepEarly = new JobStep();
		jobStepEarly.setJob(jobEarly);
		jobStepEarly.setProcessingMode(TEST_MODE);
		
		Job jobLate = new Job();
		jobLate.setProcessingOrder(order);
		jobLate.setProcessingFacility(facility);
		jobLate.setStartTime(TEST_START_TIME_LATE);
		jobLate.setStopTime(TEST_STOP_TIME_LATE);
		JobStep jobStepLate = new JobStep();
		jobStepLate.setJob(jobLate);
		jobStepLate.setProcessingMode(TEST_MODE);
		
		// Test first product query without MINCOVER --> satisfied
		SelectionRule selectionRule = null;
		try {
			selectionRule = SelectionRule.parseSelectionRule(targetProdClass, TEST_SELECTION_RULE);
		} catch (IllegalArgumentException | ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception when parsing selection rule " + TEST_SELECTION_RULE + " (cause: " + e.getMessage() + ")");
		}
		assertTrue("List of selection rules is empty", !selectionRule.getSimpleRules().isEmpty());

		SimpleSelectionRule simpleSelectionRule = selectionRule.getSimpleRules().iterator().next();
		ProductQuery query = ProductQuery.fromSimpleSelectionRule(simpleSelectionRule, jobStepLate);
		logger.trace("Starting test for product query 1 based on " + simpleSelectionRule);
		assertTrue("Product query 1 fails unexpectedly for JPQL", queryService.executeQuery(query, true));
		assertTrue("Product query 1 fails unexpectedly for SQL", queryService.executeSqlQuery(query, true));
		
		// Test first product query with additional filter condition "revision:2" --> fails
		inputFilter.getFilterConditions().clear();
		inputFilter.getFilterConditions().put("revision", (new Parameter()).init(ParameterType.INTEGER, 2));
		query = ProductQuery.fromSimpleSelectionRule(simpleSelectionRule, jobStepLate);
		logger.trace("Starting test for product query 1 with filters " + query.getFilterConditions());
		assertTrue("Product query 1 succeeds unexpectedly for filter 'revision:2'", !queryService.executeQuery(query, true));
		inputFilter.getFilterConditions().clear();
		inputFilter.getFilterConditions().put("revision", (new Parameter()).init(ParameterType.INTEGER, 1));
		
		// Test second product query with MINCOVER --> satisfied for early interval, not satisfied for late interval
		try {
			selectionRule = SelectionRule.parseSelectionRule(targetProdClass, TEST_SELECTION_RULE_MINCOVER);
		} catch (IllegalArgumentException | ParseException e) {
			e.printStackTrace();
			fail("Unexpected exception when parsing selection rule " + TEST_SELECTION_RULE + " (cause: " + e.getMessage() + ")");
		}
		assertTrue("List of selection rules is empty", !selectionRule.getSimpleRules().isEmpty());

		simpleSelectionRule = selectionRule.getSimpleRules().iterator().next();
		query = ProductQuery.fromSimpleSelectionRule(simpleSelectionRule, jobStepEarly);
		logger.trace("Starting test for product query 2 and early interval based on " + simpleSelectionRule);
		assertTrue("Product query 2 fails unexpectedly for early interval and JPQL", queryService.executeQuery(query, true));
		assertTrue("Product query 2 fails unexpectedly for early interval and SQL", queryService.executeSqlQuery(query, true));

		query = ProductQuery.fromSimpleSelectionRule(simpleSelectionRule, jobStepLate);
		logger.trace("Starting test for product query 2 and late interval based on " + simpleSelectionRule);
		assertTrue("Product query 2 succeeds unexpectedly for late interval and JPQL", !queryService.executeQuery(query, true));
		assertTrue("Product query 2 succeeds unexpectedly for late interval and SQL", !queryService.executeSqlQuery(query, true));
		
		logger.info("OK: Test for executeQuery completed");
	}

}
