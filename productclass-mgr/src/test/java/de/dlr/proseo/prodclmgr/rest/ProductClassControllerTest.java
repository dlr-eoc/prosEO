/**
 * ProductClassControllerTest.java
 *
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.SimplePolicy;
import de.dlr.proseo.model.SimplePolicy.DeltaTime;
import de.dlr.proseo.model.SimplePolicy.PolicyType;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.enums.OrderSlicingType;
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
@SpringBootTest(classes = ProductClassManagerApplication.class)
@WithMockUser(username = "UTM-testuser", roles = {})
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
	private static final String TEST_NEW_PRODUCT_TYPE_2 = "$SUB_FRESCO_$";
	private static final String TEST_SELECTION_RULE = "FOR " + TEST_PRODUCT_TYPE + "/" + TEST_PARAM_KEY + ":" + TEST_PARAM_VALUE
			+ " SELECT LatestValIntersect(180 M, 180 M) OR LatestValidity OPTIONAL";
	private static final String TEST_SELECTION_RULE_2 = "FOR " + TEST_NEW_PRODUCT_TYPE + " SELECT LatestValidity";
	private static final ProductVisibility TEST_VISIBILITY = ProductVisibility.PUBLIC;

	/** The ProductClassControllerImpl under test */
	@Autowired
	private ProductClassControllerImpl pci;

	/** Database transaction manager */
	@Autowired
	private PlatformTransactionManager txManager;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductClassControllerTest.class);

	/**
	 * @throws java.lang.Exception if any error occurs
	 */
	@BeforeEach
	public void setUp() throws Exception {
		fillDatabase();
	}

	/**
	 * @throws java.lang.Exception if any error occurs
	 */
	@AfterEach
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
		testSelectionRule.getFilterConditions()
			.put(TEST_PARAM_KEY, new Parameter().init(ParameterType.valueOf(TEST_PARAM_TYPE), TEST_PARAM_VALUE));

		testSelectionRule.setTargetProductClass(testProductClass);

		logger.trace("... adding another test product class");
		ProductClass sourceProductClass = new ProductClass();
		sourceProductClass.setProductType(TEST_PRODUCT_TYPE);
		sourceProductClass.setMission(testMission);
		sourceProductClass.setVisibility(TEST_VISIBILITY);
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
	 * Delete a product class and re-create it
	 *
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#createRestProductClass(de.dlr.proseo.prodclmgr.rest.model.RestProductClass)}
	 * and
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#deleteProductclassById(java.lang.Long)}.
	 */
	@Test
	public final void testCreateRestProductClass() {
		logger.trace(">>> testCreateRestProductClass()");

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		transactionTemplate.execute(status -> {
			// Wrap in transaction to ensure that either all database modifications succeed or all fail

			// Delete on of the product classes
			RestProductClass testProductClass = ProductClassUtil.toRestProductClass(RepositoryService.getProductClassRepository()
				.findByMissionCodeAndProductType(testMissionData[0], TEST_NEW_PRODUCT_TYPE));
			RestSimpleSelectionRule testSelectionRule = testProductClass.getSelectionRule().get(0);
			RestSimplePolicy restSimplePolicy = testSelectionRule.getSimplePolicies().get(0);
			RepositoryService.getProductClassRepository().deleteById(testProductClass.getId());

			// Recreate the product class
			testProductClass.setId(null);
			ResponseEntity<RestProductClass> postEntity = pci.createRestProductClass(testProductClass);

			assertEquals(HttpStatus.CREATED, postEntity.getStatusCode(), "Unexpected HTTP status code: ");

			logger.trace("Test OK: Delete a single product class"); // ... because we could create a product class with the same name

			// Check the result
			RestProductClass responseProductClass = postEntity.getBody();
			assertNotNull(responseProductClass, "Product class missing");
			assertNotEquals(0L, responseProductClass.getId().longValue(), "Database ID should be set: ");
			assertEquals(testProductClass.getMissionCode(), responseProductClass.getMissionCode(), "Unexpected mission code: ");
			assertEquals(testProductClass.getProductType(), responseProductClass.getProductType(), "Unexpected product type: ");
			assertNotNull(responseProductClass.getSelectionRule(), "List of selection rules missing");
			assertEquals(testProductClass.getSelectionRule().size(), responseProductClass.getSelectionRule().size(),
					"Unexpected number of selection rules: ");
			RestSimpleSelectionRule responseSelectionRule = responseProductClass.getSelectionRule().get(0);
			assertEquals(testSelectionRule.getMode(), responseSelectionRule.getMode(), "Unexpected selection rule mode: ");
			assertEquals(testSelectionRule.getIsMandatory(), responseSelectionRule.getIsMandatory(),
					"Unexpected mandatory value: ");
			assertNotNull(responseSelectionRule.getFilterConditions(), "List of filter conditions missing");
			assertEquals(testSelectionRule.getFilterConditions().size(), responseSelectionRule.getFilterConditions().size(),
					"Unexpected number of filter conditions: ");
			assertEquals(testSelectionRule.getFilterConditions().get(0), responseSelectionRule.getFilterConditions().get(0),
					"Unexpected filter condition: ");
			assertNotNull(responseSelectionRule.getSimplePolicies(), "List of simple policies missing");
			assertEquals(testSelectionRule.getSimplePolicies().size(), responseSelectionRule.getSimplePolicies().size(),
					"Unexpected number of simple policies: ");
			RestSimplePolicy responsePolicy = responseSelectionRule.getSimplePolicies().get(0);
			assertEquals(restSimplePolicy.getPolicyType(), responsePolicy.getPolicyType(), "Unexpected policy type: ");
			assertEquals(restSimplePolicy.getDeltaTimeT0(), responsePolicy.getDeltaTimeT0(), "Unexpected delta time T0: ");
			assertEquals(restSimplePolicy.getDeltaTimeT1(), responsePolicy.getDeltaTimeT1(), "Unexpected delta time T1: ");

			Optional<ProductClass> dbProductClass = RepositoryService.getProductClassRepository()
				.findById(responseProductClass.getId());
			assertFalse(dbProductClass.isEmpty(), "Product class not in database");
			return true;
		});

		logger.trace("Test OK: Insert a single product class");
	}

	/**
	 * Show that adding a component product class for a product class having a selection rule is invalid.
	 *
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#createRestProductClass(de.dlr.proseo.prodclmgr.rest.model.RestProductClass)}.
	 */
	@Test
	public final void testCreateRestProductClassInvalid() {
		logger.trace(">>> testCreateRestProductClassInvalid()");

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		try {
			transactionTemplate.execute(status -> {
				// Wrap in transaction to ensure that either all product creates succeed or all fail

				Mission testMission = RepositoryService.getMissionRepository().findByCode(testMissionData[0]);

				ProductClass enclosingClass = RepositoryService.getProductClassRepository()
						.findByMissionCodeAndProductType(testMissionData[0], TEST_PRODUCT_TYPE);

				ProductClass testProductClass = new ProductClass();
				testProductClass.setProductType(TEST_NEW_PRODUCT_TYPE_2);
				testProductClass.setMission(testMission);
				testProductClass.setVisibility(TEST_VISIBILITY);
				testProductClass.setEnclosingClass(enclosingClass);

				RestProductClass testRestProductClass = ProductClassUtil.toRestProductClass(testProductClass);

				ResponseEntity<RestProductClass> postEntity = pci.createRestProductClass(testRestProductClass);
				assertEquals(HttpStatus.BAD_REQUEST, postEntity.getStatusCode(), "Unexpected HTTP status code: ");

				return true;
			});
		} catch (UnexpectedRollbackException e) {
			// Success!
		}

		logger.trace("Test OK: Insert a single product class");
	}

	/**
	 * Test method for {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#getRestProductClassById(java.lang.Long)}.
	 */
	@Test
	public final void testGetRestProductClassById() {
		logger.trace(">>> testGetRestProductClassById()");

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		transactionTemplate.execute(status -> {
			// Wrap in transaction to ensure that either all product creates succeed or all fail

			// Find some product class
			RestProductClass testProductClass = ProductClassUtil.toRestProductClass(RepositoryService.getProductClassRepository()
				.findByMissionCodeAndProductType(testMissionData[0], TEST_PRODUCT_TYPE));

			// Perform the update under test
			ResponseEntity<RestProductClass> postEntity = pci.getRestProductClassById(testProductClass.getId());

			assertEquals(HttpStatus.OK, postEntity.getStatusCode(), "Unexpected HTTP status code: ");

			// Check the result
			RestProductClass responseProductClass = postEntity.getBody();
			assertNotNull(responseProductClass, "Product class missing");
			assertEquals(testProductClass.getProductType(), responseProductClass.getProductType(), "Unexpected product type: ");

			return true;
		});

		logger.trace("Test OK: Read a single product class");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#modifyRestProductClass(java.lang.Long, de.dlr.proseo.prodclmgr.rest.model.RestProductClass)}.
	 */
	@Test
	public final void testModifyRestProductClass() {
		logger.trace(">>> testModifyRestProductClass()");

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		transactionTemplate.execute(status -> {
			// Wrap in transaction to ensure that either all product creates succeed or all fail

			// Modify some product class attributes
			RestProductClass testProductClass = ProductClassUtil.toRestProductClass(RepositoryService.getProductClassRepository()
				.findByMissionCodeAndProductType(testMissionData[0], TEST_PRODUCT_TYPE));

			testProductClass.setDefaultSlicingType(OrderSlicingType.CALENDAR_DAY.name());
			testProductClass.setProcessingLevel("L3");

			// Perform the update under test
			ResponseEntity<RestProductClass> postEntity = pci.modifyRestProductClass(testProductClass.getId(), testProductClass);

			assertEquals(HttpStatus.OK, postEntity.getStatusCode(), "Unexpected HTTP status code: ");

			// Check the result
			RestProductClass responseProductClass = postEntity.getBody();
			assertNotNull(responseProductClass, "Product class missing");
			assertEquals(OrderSlicingType.CALENDAR_DAY.name(), responseProductClass.getDefaultSlicingType(), "Unexpected slicing type: ");
			assertEquals("L3", responseProductClass.getProcessingLevel(), "Unexpected processing level: ");

			return true;
		});

		logger.trace("Test OK: Update a single product class");
	}

	/**
	 * Show that it is invalid to add component classes to a product class supporting selection rules
	 *
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#modifyRestProductClass(java.lang.Long, de.dlr.proseo.prodclmgr.rest.model.RestProductClass)}.
	 */
	@Test
	public final void testModifyRestProductClassInvalid() {
		logger.trace(">>> testModifyRestProductClassInvalid()");

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		try {
			transactionTemplate.execute(status -> {
				// Wrap in transaction to ensure that either all product creates succeed or all fail

				//
				RestProductClass testProductClass = ProductClassUtil.toRestProductClass(RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(testMissionData[0], TEST_PRODUCT_TYPE));

				testProductClass.getComponentClasses().add(TEST_NEW_PRODUCT_TYPE);

				ResponseEntity<RestProductClass> postEntity = pci.modifyRestProductClass(testProductClass.getId(), testProductClass);
				assertEquals(HttpStatus.BAD_REQUEST, postEntity.getStatusCode(), "Unexpected HTTP status code: ");

				return true;
			});
		} catch (UnexpectedRollbackException e) {
			// Success!
		}

		logger.trace("Test OK: Reject component classes for class supporting selection rules");
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

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		transactionTemplate.execute(status -> {
			// Retrieve a test product class from the repository and remove the
			// selection rule
			ProductClass testProductClass = RepositoryService.getProductClassRepository()
				.findByMissionCodeAndProductType(testMissionData[0], TEST_NEW_PRODUCT_TYPE);
			testProductClass.getRequiredSelectionRules().clear();
			logger.trace("Success? "
					+ RepositoryService.getProductClassRepository().findByMissionCodeAndProductType("UTM", TEST_PRODUCT_TYPE));

			// Now create a selection rule for this product class
			SelectionRuleString ruleString = new SelectionRuleString();
			ruleString.setMode("OFFL");
			ruleString.setSelectionRule(TEST_SELECTION_RULE);
			// TODO We could add a configured processor here, if one was created
			// beforehand
			List<SelectionRuleString> ruleStrings = new ArrayList<>();
			ruleStrings.add(ruleString);

			ResponseEntity<RestProductClass> postEntity = pci.createSelectionRuleString(testProductClass.getId(), ruleStrings);
			assertEquals(HttpStatus.CREATED, postEntity.getStatusCode(), "Unexpected HTTP status code: ");

			RestProductClass restProductClass = postEntity.getBody();

			// Check result
			assertNotNull(restProductClass, "Product class missing");
			assertNotNull(restProductClass.getSelectionRule(), "List of selection rules missing");
			assertEquals(ruleStrings.size(), restProductClass.getSelectionRule().size(), "Unexpected number of selection rules:");

			RestSimpleSelectionRule responseRule = restProductClass.getSelectionRule().get(0);
			assertEquals(testMissionData[2], responseRule.getMode(), "Unexpected mode:");
			assertEquals(false, responseRule.getIsMandatory(), "Unexpected mandatory value:");
			assertEquals(TEST_NEW_PRODUCT_TYPE, responseRule.getTargetProductClass(), "Unexpected target product class:");
			assertEquals(TEST_PRODUCT_TYPE, responseRule.getSourceProductClass(), "Unexpected source product class:");
			assertNotNull(responseRule.getConfiguredProcessors(), "List of configured processors missing");
			assertEquals(0, responseRule.getConfiguredProcessors().size(), "Unexpected number of configured processors:");

			assertNotNull(responseRule.getFilterConditions(), "List of filter conditions missing");
			assertEquals(1, responseRule.getFilterConditions().size(), "Unexpected number of filter conditions:");

			RestParameter filterParameter = responseRule.getFilterConditions().get(0);
			assertEquals(TEST_PARAM_KEY, filterParameter.getKey(), "Unexpected filter condition key:");
			assertEquals(TEST_PARAM_TYPE, filterParameter.getParameterType(), "Unexpected filter condition type:");
			assertEquals(TEST_PARAM_VALUE, filterParameter.getParameterValue(), "Unexpected filter condition value:");

			assertNotNull(responseRule.getSimplePolicies(), "List of simple policies missing");
			assertEquals(2, responseRule.getSimplePolicies().size(), "Unexpected number of simple policies:");

			for (RestSimplePolicy responsePolicy : responseRule.getSimplePolicies()) {
				if ("LatestValIntersect".equals(responsePolicy.getPolicyType())) {
					// 3 hours are expected instead of 180 minutes, as the delta
					// time is normalized
					// during selection rule creation
					assertEquals(3L, responsePolicy.getDeltaTimeT0().getDuration().longValue(),
							"Unexpected LatestValIntersect delta time 0 duration:");
					assertEquals(TimeUnit.HOURS.toString(), responsePolicy.getDeltaTimeT0().getUnit(),
							"Unexpected LatestValIntersect delta time 0 unit:");
					assertEquals(3L, responsePolicy.getDeltaTimeT1().getDuration().longValue(),
							"Unexpected LatestValIntersect delta time 1 duration:");
					assertEquals(TimeUnit.HOURS.toString(), responsePolicy.getDeltaTimeT1().getUnit(),
							"Unexpected LatestValIntersect delta time 1 unit:");
				} else if ("LatestValidity".equals(responsePolicy.getPolicyType())) {
					assertEquals(0, responsePolicy.getDeltaTimeT0().getDuration().longValue(),
							"Unexpected LatestValidity delta time 0 duration:");
					assertEquals(TimeUnit.DAYS.toString(), responsePolicy.getDeltaTimeT0().getUnit(),
							"Unexpected LatestValidity delta time 0 unit:");
					assertEquals(0, responsePolicy.getDeltaTimeT1().getDuration().longValue(),
							"Unexpected LatestValidity delta time 1 duration:");
					assertEquals(TimeUnit.DAYS.toString(), responsePolicy.getDeltaTimeT1().getUnit(),
							"Unexpected LatestValidity delta time 1 unit:");
				} else {
					fail("Unexpected policy type: " + responsePolicy.getPolicyType());
				}
			}

			return true;
		});

		logger.trace("Test OK: Create selection rule from string");
	}

	/**
	 * Show that adding a supported selection rule (i. e. product class is source class in rule)
	 * to a product class with component classes is invalid
	 *
	 * Test method for
	 * {@link de.dlr.proseo.prodclmgr.rest.ProductClassControllerImpl#createSelectionRuleString(java.lang.Long, java.util.List)}.
	 */
	@Test
	public final void testCreateSelectionRuleStringInvalid() {
		logger.trace(">>> testCreateSelectionRuleStringInvalid()");

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		try {
			transactionTemplate.execute(status -> {
				// Retrieve a test product class from the repository and remove the selection rule
				ProductClass testProductClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(testMissionData[0], TEST_NEW_PRODUCT_TYPE);
				ProductClass componentProductClass = RepositoryService.getProductClassRepository()
						.findByMissionCodeAndProductType(testMissionData[0], TEST_PRODUCT_TYPE);

				// Add a component product class (doubles as target product class for the selection rule)
				testProductClass.getComponentClasses().add(componentProductClass);
				testProductClass = RepositoryService.getProductClassRepository().save(testProductClass);

				RestProductClass restProductClass = ProductClassUtil.toRestProductClass(componentProductClass);

				// Now create a selection rule having the enclosing product class as source class
				SelectionRuleString ruleString = new SelectionRuleString();
				ruleString.setSelectionRule(TEST_SELECTION_RULE_2);
				List<SelectionRuleString> ruleStrings = new ArrayList<>();
				ruleStrings.add(ruleString);

				ResponseEntity<RestProductClass> postEntity = pci.createSelectionRuleString(restProductClass.getId(), ruleStrings);
				assertEquals(HttpStatus.BAD_REQUEST, postEntity.getStatusCode(), "Unexpected HTTP status code: ");

				return true;
			});
		} catch (UnexpectedRollbackException e) {
			// Success!
		}

		logger.trace("Test OK: Reject invalid selection rule");
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
