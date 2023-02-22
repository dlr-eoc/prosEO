/**
 * ProductClassControllerTest.java
 *
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.SimplePolicy;
import de.dlr.proseo.model.SimplePolicy.DeltaTime;
import de.dlr.proseo.model.SimplePolicy.PolicyType;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.enums.ProductVisibility;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.prodclmgr.ProductClassManagerApplication;
import de.dlr.proseo.prodclmgr.rest.model.ProductClassUtil;
import de.dlr.proseo.prodclmgr.rest.model.RestParameter;
import de.dlr.proseo.prodclmgr.rest.model.RestProductClass;
import de.dlr.proseo.prodclmgr.rest.model.RestSimplePolicy;
import de.dlr.proseo.prodclmgr.rest.model.RestSimpleSelectionRule;
import de.dlr.proseo.prodclmgr.rest.model.SelectionRuleString;

/**
 * Test class for the REST API of ProductClassControllerImpl
 *
 * @author Dr. Thomas Bassler
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProductClassManagerApplication.class)
@WithMockUser(username = "UTM-testuser", roles = {})
@AutoConfigureTestEntityManager
@Transactional
public class ProductClassControllerTest {

	// Test data
	private static String[] testMissionData =
			// code, name, processing_mode, file_class, product_file_template
			{ "UTM", "ABCD Testing", "OFFL", "OPER", "test_file_temp" };
	private static final String TEST_PRODUCT_TYPE = "L2__FRESCO_";
	private static final String TEST_PARAM_VALUE = "01";
	private static final String TEST_PARAM_TYPE = "STRING";
	private static final String TEST_PARAM_KEY = "revision";
	private static final String TEST_NEW_PRODUCT_TYPE = "$L2__AAI___$";
	private static final String TEST_SELECTION_RULE = "FOR " + TEST_PRODUCT_TYPE + "/" + TEST_PARAM_KEY + ":"
			+ TEST_PARAM_VALUE + " SELECT LatestValIntersect(180 M, 180 M) OR LatestValidity OPTIONAL";
	private static final ProductVisibility TEST_VISIBILITY = ProductVisibility.PUBLIC;

	/** The ProductClassControllerImpl under test */
	@Autowired
	private ProductClassControllerImpl pci;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductClassControllerTest.class);

	/**
	 * @throws java.lang.Exception if any error occurs
	 */
	@Before
	public void setUp() throws Exception {
		fillDatabase();
	}

	/**
	 * @throws java.lang.Exception if any error occurs
	 */
	@After
	public void tearDown() throws Exception {
		RepositoryService.getProductClassRepository().deleteAll();
		RepositoryService.getMissionRepository().deleteAll();
	}

	/**
	 * Filling the database with some initial data for testing purposes
	 */
	private static void fillDatabase() {
		logger.trace("... creating testMission {}", testMissionData[0]);
		Mission testMission = new Mission();
		testMission.setCode(testMissionData[0]);
		testMission.setName(testMissionData[1]);
		testMission.getProcessingModes().add(testMissionData[2]);
		testMission.getFileClasses().add(testMissionData[3]);
		testMission.setProductFileTemplate(testMissionData[4]);
		testMission = RepositoryService.getMissionRepository().save(testMission);

		logger.trace("... adding a test product class");
		ProductClass testProductClass = new ProductClass();
		testProductClass.setProductType(TEST_NEW_PRODUCT_TYPE);
		testProductClass.setMission(testMission);
		testProductClass.setVisibility(TEST_VISIBILITY);
		testProductClass = RepositoryService.getProductClassRepository().save(testProductClass);

		// Create a selection rule for the new product class
		SimpleSelectionRule testSelectionRule = new SimpleSelectionRule();
		testSelectionRule.setMode(testMissionData[2]);
		testSelectionRule.setIsMandatory(true);
		testSelectionRule.getFilterConditions().put(TEST_PARAM_KEY,
				new Parameter().init(ParameterType.valueOf(TEST_PARAM_TYPE), TEST_PARAM_VALUE));

		testSelectionRule.setTargetProductClass(testProductClass);

		ProductClass sourceProductClass = new ProductClass();
		sourceProductClass.setProductType(TEST_PRODUCT_TYPE);
		sourceProductClass.setMission(testMission);
		sourceProductClass = RepositoryService.getProductClassRepository().save(sourceProductClass);
		testSelectionRule.setSourceProductClass(sourceProductClass);

		// Create a simple policy for the new product class
		SimplePolicy testSimplePolicy = new SimplePolicy();
		testSimplePolicy.setPolicyType(PolicyType.LatestValCover);
		DeltaTime deltaTimeT0 = new DeltaTime();
		deltaTimeT0.duration = 4L;
		testSimplePolicy.setDeltaTimeT0(deltaTimeT0);
		DeltaTime deltaTimeT1 = new DeltaTime();
		deltaTimeT1.duration = 180L;
		testSimplePolicy.setDeltaTimeT1(deltaTimeT1);

		testSelectionRule.getSimplePolicies().add(testSimplePolicy);
		testProductClass.getRequiredSelectionRules().add(testSelectionRule);

		testProductClass = RepositoryService.getProductClassRepository().save(testProductClass);

		testMission.getProductClasses().add(sourceProductClass);
		testMission.getProductClasses().add(testProductClass);
		RepositoryService.getMissionRepository().save(testMission);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#getRestProductClass(java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testGetRestProductClass() {
		// TODO
		logger.trace("Test not implemented for getRestProductClass");

		logger.trace("Test OK: Read all product classes");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#createRestProductClass(de.dlr.proseo.prodclmgr.rest.model.RestProductClass)}.
	 */
	@Test
	public final void testCreateRestProductClass() {
		logger.trace(">>> testCreateRestProductClass()");

		//
		RestProductClass testProductClass = ProductClassUtil
				.toRestProductClass(RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(testMissionData[0], TEST_NEW_PRODUCT_TYPE));
		RestSimpleSelectionRule testSelectionRule = testProductClass.getSelectionRule().get(0);
		RestSimplePolicy restSimplePolicy = testSelectionRule.getSimplePolicies().get(0);
		RepositoryService.getProductClassRepository().deleteById(testProductClass.getId());

		//
		ResponseEntity<RestProductClass> postEntity = pci.createRestProductClass(testProductClass);

		assertEquals("Unexpected HTTP status code: ", HttpStatus.CREATED, postEntity.getStatusCode());

		// Check the result
		RestProductClass responseProductClass = postEntity.getBody();
		assertNotNull("Product class missing", responseProductClass);
		assertNotEquals("Database ID should be set: ", 0L, responseProductClass.getId().longValue());
		assertEquals("Unexpected mission code: ", testProductClass.getMissionCode(),
				responseProductClass.getMissionCode());
		assertEquals("Unexpected product type: ", testProductClass.getProductType(),
				responseProductClass.getProductType());
		assertNotNull("List of selection rules missing", responseProductClass.getSelectionRule());
		assertEquals("Unexpected number of selection rules: ", testProductClass.getSelectionRule().size(),
				responseProductClass.getSelectionRule().size());
		RestSimpleSelectionRule responseSelectionRule = responseProductClass.getSelectionRule().get(0);
		assertEquals("Unexpected selection rule mode: ", testSelectionRule.getMode(), responseSelectionRule.getMode());
		assertEquals("Unexpected mandatory value: ", testSelectionRule.getIsMandatory(),
				responseSelectionRule.getIsMandatory());
		assertNotNull("List of filter conditions missing", responseSelectionRule.getFilterConditions());
		assertEquals("Unexpected number of filter conditions: ", testSelectionRule.getFilterConditions().size(),
				responseSelectionRule.getFilterConditions().size());
		assertEquals("Unexpected filter condition: ", testSelectionRule.getFilterConditions().get(0),
				responseSelectionRule.getFilterConditions().get(0));
		assertNotNull("List of simple policies missing", responseSelectionRule.getSimplePolicies());
		assertEquals("Unexpected number of simple policies: ", testSelectionRule.getSimplePolicies().size(),
				responseSelectionRule.getSimplePolicies().size());
		RestSimplePolicy responsePolicy = responseSelectionRule.getSimplePolicies().get(0);
		assertEquals("Unexpected policy type: ", restSimplePolicy.getPolicyType(), responsePolicy.getPolicyType());
		assertEquals("Unexpected delta time T0: ", restSimplePolicy.getDeltaTimeT0(), responsePolicy.getDeltaTimeT0());
		assertEquals("Unexpected delta time T1: ", restSimplePolicy.getDeltaTimeT1(), responsePolicy.getDeltaTimeT1());

		Optional<ProductClass> dbProductClass = RepositoryService.getProductClassRepository()
				.findById(responseProductClass.getId());
		assertFalse("Product class not in database", dbProductClass.isEmpty());

		logger.trace("Test OK: Insert a single product class");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#getRestProductClassById(java.lang.Long)}.
	 */
	@Test
	public final void testGetRestProductClassById() {
		// TODO
		logger.trace("Test not implemented for getRestProductClassById");

		logger.trace("Test OK: Read a single product class");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#modifyRestProductClass(java.lang.Long, de.dlr.proseo.prodclmgr.rest.model.RestProductClass)}.
	 */
	@Test
	public final void testModifyRestProductClass() {
		// TODO
		logger.trace("Test not implemented for modifyRestProductClass");

		logger.trace("Test OK: Update a single product class");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#deleteProductclassById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteProductclassById() {
		// TODO
		logger.trace("Test not implemented for deleteProductclassById");

		logger.trace("Test OK: Delete a single product class");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#getSelectionRuleStrings(java.lang.Long, java.lang.String)}.
	 */
	@Test
	public final void testGetSelectionRuleStrings() {
		// TODO
		logger.trace("Test not implemented for getSelectionRuleStrings");

		logger.trace("Test OK: Get selection rule strings");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#createSelectionRuleString(java.lang.Long, java.util.List)}.
	 */
	@Test
	public final void testCreateSelectionRuleString() {
		logger.trace(">>> testCreateSelectionRuleString()");

		// Retrieve a test product class from the repository and remove the selection rule
		ProductClass testProductClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(testMissionData[0], TEST_NEW_PRODUCT_TYPE);
		testProductClass.getRequiredSelectionRules().clear();
		logger.trace("Success? " + RepositoryService.getProductClassRepository().findByMissionCodeAndProductType("UTM", TEST_PRODUCT_TYPE));

		// Now create a selection rule for this product class
		SelectionRuleString ruleString = new SelectionRuleString();
		ruleString.setMode("OFFL");
		ruleString.setSelectionRule(TEST_SELECTION_RULE);
		// TODO We could add a configured processor here, if one was created beforehand
		List<SelectionRuleString> ruleStrings = new ArrayList<>();
		ruleStrings.add(ruleString);

		ResponseEntity<RestProductClass> postEntity = pci.createSelectionRuleString(testProductClass.getId(), ruleStrings);
		assertEquals("Unexpected HTTP status code: ", HttpStatus.CREATED, postEntity.getStatusCode());

		RestProductClass restProductClass = postEntity.getBody();

		// Check result
		assertNotNull("Product class missing", restProductClass);
		assertNotNull("List of selection rules missing", restProductClass.getSelectionRule());
		assertEquals("Unexpected number of selection rules:", ruleStrings.size(),
				restProductClass.getSelectionRule().size());

		RestSimpleSelectionRule responseRule = restProductClass.getSelectionRule().get(0);
		assertEquals("Unexpected mode:", testMissionData[2], responseRule.getMode());
		assertEquals("Unexpected mandatory value:", false, responseRule.getIsMandatory());
		assertEquals("Unexpected target product class:", TEST_NEW_PRODUCT_TYPE, responseRule.getTargetProductClass());
		assertEquals("Unexpected source product class:", TEST_PRODUCT_TYPE, responseRule.getSourceProductClass());
		assertNotNull("List of configured processors missing", responseRule.getApplicableConfiguredProcessors());
		assertEquals("Unexpected number of configured processors:", 0,
				responseRule.getApplicableConfiguredProcessors().size());

		assertNotNull("List of filter conditions missing", responseRule.getFilterConditions());
		assertEquals("Unexpected number of filter conditions:", 1, responseRule.getFilterConditions().size());

		RestParameter filterParameter = responseRule.getFilterConditions().get(0);
		assertEquals("Unexpected filter condition key:", TEST_PARAM_KEY, filterParameter.getKey());
		assertEquals("Unexpected filter condition type:", TEST_PARAM_TYPE, filterParameter.getParameterType());
		assertEquals("Unexpected filter condition value:", TEST_PARAM_VALUE, filterParameter.getParameterValue());

		assertNotNull("List of simple policies missing", responseRule.getSimplePolicies());
		assertEquals("Unexpected number of simple policies:", 2, responseRule.getSimplePolicies().size());

		for (RestSimplePolicy responsePolicy : responseRule.getSimplePolicies()) {
			if ("LatestValIntersect".equals(responsePolicy.getPolicyType())) {
				// 3 hours are expected instead of 180 minutes, as the delta time is normalized
				// during selection rule creation
				assertEquals("Unexpected LatestValIntersect delta time 0 duration:", 3L,
						responsePolicy.getDeltaTimeT0().getDuration().longValue());
				assertEquals("Unexpected LatestValIntersect delta time 0 unit:", TimeUnit.HOURS.toString(),
						responsePolicy.getDeltaTimeT0().getUnit());
				assertEquals("Unexpected LatestValIntersect delta time 1 duration:", 3L,
						responsePolicy.getDeltaTimeT1().getDuration().longValue());
				assertEquals("Unexpected LatestValIntersect delta time 1 unit:", TimeUnit.HOURS.toString(),
						responsePolicy.getDeltaTimeT1().getUnit());
			} else if ("LatestValidity".equals(responsePolicy.getPolicyType())) {
				assertEquals("Unexpected LatestValidity delta time 0 duration:", 0,
						responsePolicy.getDeltaTimeT0().getDuration().longValue());
				assertEquals("Unexpected LatestValidity delta time 0 unit:", TimeUnit.DAYS.toString(),
						responsePolicy.getDeltaTimeT0().getUnit());
				assertEquals("Unexpected LatestValidity delta time 1 duration:", 0,
						responsePolicy.getDeltaTimeT1().getDuration().longValue());
				assertEquals("Unexpected LatestValidity delta time 1 unit:", TimeUnit.DAYS.toString(),
						responsePolicy.getDeltaTimeT1().getUnit());
			} else {
				fail("Unexpected policy type: " + responsePolicy.getPolicyType());
			}
		}

		logger.trace("Test OK: Create selection rule from string");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#getSelectionRuleString(java.lang.Long, java.lang.Long)}.
	 */
	@Test
	public final void testGetSelectionRuleString() {
		// TODO
		logger.trace("Test not implemented for getSelectionRuleString");

		logger.trace("Test OK: Get selection rule by ID");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#modifySelectionRuleString(java.lang.Long, java.lang.Long, de.dlr.proseo.prodclmgr.rest.model.SelectionRuleString)}.
	 */
	@Test
	public final void testModifySelectionRuleString() {
		// TODO
		logger.trace("Test not implemented for modifySelectionRuleString");

		logger.trace("Test OK: Update selection rule by ID");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#deleteSelectionrule(java.lang.Long, java.lang.Long)}.
	 */
	@Test
	public final void testDeleteSelectionrule() {
		// TODO
		logger.trace("Test not implemented for deleteSelectionrule");

		logger.trace("Test OK: Delete selection rule by ID");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#addProcessorToRule(java.lang.String, java.lang.Long, java.lang.Long)}.
	 */
	@Test
	public final void testAddProcessorToRule() {
		// TODO
		logger.trace("Test not implemented for addProcessorToRule");

		logger.trace("Test OK: Add configured processor to selection rule");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#removeProcessorFromRule(java.lang.String, java.lang.Long, java.lang.Long)}.
	 */
	@Test
	public final void testRemoveProcessorFromRule() {
		// TODO
		logger.trace("Test not implemented for removeProcessorFromRule");

		logger.trace("Test OK: Remove configured processor from selection rule");
	}

}
