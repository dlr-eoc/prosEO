/**
 * ProductClassControllerTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest;

import static org.junit.Assert.*;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.SimplePolicy.PolicyType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.prodclmgr.ProductClassManager;
import de.dlr.proseo.prodclmgr.ProductClassSecurityConfig;
import de.dlr.proseo.prodclmgr.ProductClassTestConfiguration;
import de.dlr.proseo.prodclmgr.rest.model.RestProductClass;

/**
 * Test class for the REST API of ProductClassControllerImpl
 * 
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProductClassManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
//@DirtiesContext
//@Transactional
@AutoConfigureTestEntityManager
public class ProductClassControllerTest {
	

	/* The base URI of the Ingestor */
	private static String PRODUCT_CLASS_BASE_URI = "/proseo/productclass-mgr/v0.1/productclasses";

	/* Various static test data */
	private static final String TEST_CODE = "ABC";
	private static final String TEST_PRODUCT_TYPE = "FRESCO";
	private static final String TEST_MISSION_TYPE = "L2__FRESCO_";
	private static final String TEST_PARAM_VALUE = "01";
	private static final String TEST_PARAM_TYPE = "STRING";
	private static final String TEST_PARAM_KEY = "revision";
	private static final String TEST_MODE = "OFFL";
	private static final String TEST_NEW_MISSION_TYPE = "$L2__AAI___$";
	private static final String TEST_NEW_PRODUCT_TYPE = "$AAI$";
	
	/** Test configuration */
	@Autowired
	ProductClassTestConfiguration config;
	
	/** The security environment for this test */
	@Autowired
	ProductClassSecurityConfig ingestorSecurityConfig;
	
	/** The (random) port on which the Product Class Manager was started */
	@LocalServerPort
	private int port;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductClassControllerTest.class);

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

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#getRestProductClass(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testGetRestProductClass() {
		// TODO
		logger.warn("Test not implemented for getRestProductClass");

		logger.info("Test OK: Read all product classes");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#createRestProductClass(de.dlr.proseo.prodclmgr.rest.model.RestProductClass)}.
	 */
	@Test
	public final void testCreateRestProductClass() {
		
		// Make sure a mission and a base product class to derive the new one from exist
		Mission mission = RepositoryService.getMissionRepository().findByCode(TEST_CODE);
		if (null == mission) {
			mission = new Mission();
			mission.setCode(TEST_CODE);
			mission.getProcessingModes().add(TEST_MODE);
			mission = RepositoryService.getMissionRepository().save(mission);
		}
		logger.info("Using mission " + mission.getCode() + " with id " + mission.getId());
		
		ProductClass prodClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(TEST_CODE, TEST_PRODUCT_TYPE);
		if (null == prodClass) {
			prodClass = new ProductClass();
			prodClass.setMission(mission);
			prodClass.setProductType(TEST_PRODUCT_TYPE);
			prodClass.setMissionType(TEST_MISSION_TYPE);
			prodClass = RepositoryService.getProductClassRepository().save(prodClass);
			//mission.getProductClasses().add(prodClass);
			//mission = RepositoryService.getMissionRepository().save(mission);
		}
		logger.info("Using product class " + prodClass.getProductType() + " with id " + prodClass.getId());
		
		// Create a REST object for the new product class
		RestProductClass restProductClass = new RestProductClass();
		restProductClass.setMissionCode(TEST_CODE);
		restProductClass.setProductType(TEST_NEW_PRODUCT_TYPE);
		restProductClass.setMissionType(TEST_NEW_MISSION_TYPE);
		// TODO We could add a configured processor here, if one was created beforehand
		
		// Create a REST selection rule for the new product class
		de.dlr.proseo.prodclmgr.rest.model.SimpleSelectionRule newSelectionRule = new de.dlr.proseo.prodclmgr.rest.model.SimpleSelectionRule();
		newSelectionRule.setMode(TEST_MODE);
		newSelectionRule.setIsMandatory(true);
		newSelectionRule.getFilterConditions().add(
				new de.dlr.proseo.prodclmgr.rest.model.Parameter(TEST_PARAM_KEY, TEST_PARAM_TYPE, TEST_PARAM_VALUE));
		newSelectionRule.setTargetProductClass(TEST_NEW_PRODUCT_TYPE);
		newSelectionRule.setSourceProductClass(TEST_PRODUCT_TYPE);
		de.dlr.proseo.prodclmgr.rest.model.SimplePolicy newSimplePolicy = new de.dlr.proseo.prodclmgr.rest.model.SimplePolicy();
		newSimplePolicy.setPolicyType(PolicyType.LatestValCover.toString());
		newSimplePolicy.setDeltaTimeT0(
				new de.dlr.proseo.prodclmgr.rest.model.DeltaTimeT0(4L, TimeUnit.HOURS.toString()));
		newSimplePolicy.setDeltaTimeT1(
				new de.dlr.proseo.prodclmgr.rest.model.DeltaTimeT1(180L, TimeUnit.MINUTES.toString()));
		newSelectionRule.getSimplePolicies().add(newSimplePolicy);
		restProductClass.getSelectionRule().add(newSelectionRule);
		
		// Call the REST API
		String testUrl = "http://localhost:" + port + PRODUCT_CLASS_BASE_URI + "/";
		logger.info("Testing URL {} / POST", testUrl);

		ResponseEntity<RestProductClass> postEntity = new TestRestTemplate(config.getUserName(), config.getUserPassword())
				.postForEntity(testUrl, restProductClass, RestProductClass.class);
		assertEquals("Unexpected HTTP status code: ", HttpStatus.CREATED, postEntity.getStatusCode());
		
		// Check the result
		RestProductClass responseProductClass = postEntity.getBody();
		assertNotNull("Product class missing", responseProductClass);
		assertNotEquals("Database ID should be set: ", 0L, responseProductClass.getId().longValue());
		assertEquals("Unexpected mission code: ", restProductClass.getMissionCode(), responseProductClass.getMissionCode());
		assertEquals("Unexpected product type: ", restProductClass.getProductType(), responseProductClass.getProductType());
		assertEquals("Unexpected mission type: ", restProductClass.getMissionType(), responseProductClass.getMissionType());
		assertNotNull("List of selection rules missing", responseProductClass.getSelectionRule());
		assertEquals("Unexpected number of selection rules: ", restProductClass.getSelectionRule().size(), responseProductClass.getSelectionRule().size());
		de.dlr.proseo.prodclmgr.rest.model.SimpleSelectionRule responseSelectionRule = responseProductClass.getSelectionRule().get(0);
		assertEquals("Unexpected selection rule mode: ", newSelectionRule.getMode(), responseSelectionRule.getMode());
		assertEquals("Unexpected mandatory value: ", newSelectionRule.getIsMandatory(), responseSelectionRule.getIsMandatory());
		assertNotNull("List of filter conditions missing", responseSelectionRule.getFilterConditions());
		assertEquals("Unexpected number of filter conditions: ", newSelectionRule.getFilterConditions().size(), responseSelectionRule.getFilterConditions().size());
		assertEquals("Unexpected filter condition: ", newSelectionRule.getFilterConditions().get(0), responseSelectionRule.getFilterConditions().get(0));
		assertNotNull("List of simple policies missing", responseSelectionRule.getSimplePolicies());
		assertEquals("Unexpected number of simple policies: ", newSelectionRule.getSimplePolicies().size(), responseSelectionRule.getSimplePolicies().size());
		de.dlr.proseo.prodclmgr.rest.model.SimplePolicy responsePolicy = responseSelectionRule.getSimplePolicies().get(0);
		assertEquals("Unexpected policy type: ", newSimplePolicy.getPolicyType(), responsePolicy.getPolicyType());
		assertEquals("Unexpected delta time T0: ", newSimplePolicy.getDeltaTimeT0(), responsePolicy.getDeltaTimeT0());
		assertEquals("Unexpected delta time T1: ", newSimplePolicy.getDeltaTimeT1(), responsePolicy.getDeltaTimeT1());
		
		Optional<ProductClass> dbProductClass = RepositoryService.getProductClassRepository().findById(responseProductClass.getId());
		assertFalse("Product class not in database", dbProductClass.isEmpty());
		
		logger.info("Test OK: Insert a single product class");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#getRestProductClassById(java.lang.Long)}.
	 */
	@Test
	public final void testGetRestProductClassById() {
		// TODO
		logger.warn("Test not implemented for getRestProductClassById");

		logger.info("Test OK: Read a single product class");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#modifyRestProductClass(java.lang.Long, de.dlr.proseo.prodclmgr.rest.model.RestProductClass)}.
	 */
	@Test
	public final void testModifyRestProductClass() {
		// TODO
		logger.warn("Test not implemented for modifyRestProductClass");

		logger.info("Test OK: Update a single product class");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#deleteProductclassById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteProductclassById() {
		// TODO
		logger.warn("Test not implemented for deleteProductclassById");

		logger.info("Test OK: Delete a single product class");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#getSelectionRuleStrings(java.lang.Long, java.lang.String)}.
	 */
	@Test
	public final void testGetSelectionRuleStrings() {
		// TODO
		logger.warn("Test not implemented for getSelectionRuleStrings");

		logger.info("Test OK: Get selection rule strings");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#createSelectionRuleString(java.lang.Long, java.util.List)}.
	 */
	@Test
	public final void testCreateSelectionRuleString() {
		// TODO
		logger.warn("Test not implemented for createSelectionRuleString");

		logger.info("Test OK: Create selection rule from string");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#getSelectionRuleString(java.lang.Long, java.lang.Long)}.
	 */
	@Test
	public final void testGetSelectionRuleString() {
		// TODO
		logger.warn("Test not implemented for getSelectionRuleString");

		logger.info("Test OK: Get selection rule by ID");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#modifySelectionRuleString(java.lang.Long, java.lang.Long, de.dlr.proseo.prodclmgr.rest.model.SelectionRuleString)}.
	 */
	@Test
	public final void testModifySelectionRuleString() {
		// TODO
		logger.warn("Test not implemented for modifySelectionRuleString");

		logger.info("Test OK: Update selection rule by ID");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#deleteSelectionrule(java.lang.Long, java.lang.Long)}.
	 */
	@Test
	public final void testDeleteSelectionrule() {
		// TODO
		logger.warn("Test not implemented for deleteSelectionrule");

		logger.info("Test OK: Delete selection rule by ID");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#addProcessorToRule(java.lang.String, java.lang.Long, java.lang.Long)}.
	 */
	@Test
	public final void testAddProcessorToRule() {
		// TODO
		logger.warn("Test not implemented for addProcessorToRule");

		logger.info("Test OK: Add configured processor to selection rule");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#removeProcessorFromRule(java.lang.String, java.lang.Long, java.lang.Long)}.
	 */
	@Test
	public final void testRemoveProcessorFromRule() {
		// TODO
		logger.warn("Test not implemented for removeProcessorFromRule");

		logger.info("Test OK: Remove configured processor from selection rule");
	}

}
