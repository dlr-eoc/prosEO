/**
 * OrderControllerTest.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.model.util.OrderUtil;
import de.dlr.proseo.ordermgr.OrderManager;

/**
 * Testing OrderControllerImpl.class. planner
 *
 * TODO test invalid REST requests
 *
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = OrderManager.class)
@WithMockUser(username = "UTM-testuser", roles = { "ORDER_APPROVER", "ORDER_MGR" })
@AutoConfigureTestEntityManager
@Transactional
public class OrderControllerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderControllerTest.class);

	/** The OrderControllerImpl under test */
	@Autowired
	private OrderControllerImpl oci;

	/** A REST template builder for this class */
	@MockBean
	RestTemplateBuilder rtb;

	// Test data
	private static String[] testMissionData =
			// id, version, code, name, processing_mode, file_class, product_file_template
			{ "1", "0", "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" };
	private static String[] testSpacecraftData =
			// version, code,name,
			{ "1", "S_TDX1", "Tandom-X" };
	private static String[][] testFilterConditions = {
			// filter_conditions_key, parameter_type, parameter_value
			{ "copernicusCollection", "STRING", "99" }, { "revision", "INTEGER", "1" }, { "productColour", "STRING", "blue" } };
	private static String[][] testOutputParam = {
			// filter_conditions_key, parameter_type, parameter_value
			{ "copernicusCollection", "STRING", "99" }, { "copernicusCollection1", "INTEGER", "9" },
			{ "copernicusCollection2", "STRING", "999" }, };
	private static String[] testConfProc =
			// identifier
			{ "O3_TPR 02.00.00 OFFL 2019-10-11", "TROPICALIPF 01.00.00 SIR 2019-10-11" };
	private static String[] testReqProdClass = { "O3", "CLOUD" };
	private static String testInputProdClass = "L1B";
	private static String testOutputFileClass = "RPRO";
	private static String[][] testWorkflow =
			// name, UUID
			{ { "testWorkflow", UUID.randomUUID().toString() }, { "otherTestWorkflow", UUID.randomUUID().toString() } };
	private static String testWorkflowVersion = "1.0";
	private static String[][] testReqOrbits = {
			// spacecraft_code, orbitNumber from, orbitNumber to
			{ "S5P", "8132", "8138" }, { "S5P", "8136", "8141" } };
	private static String[][] testOrderData = {
			// order_id, order_version, execution_time, identifier, order_state,
			// processing_mode, slice_duration, slice_type, slice_overlap, start_time,
			// stop_time
			{ "111", "0", "2019-11-17T22:49:21.000000", "XYZ", "INITIAL", "NRTI", "PT20.345S", "TIME_SLICE", "0",
					"2019-08-29T22:49:21.000000", "2019-08-29T22:49:21.000000" },
			{ "112", "0", "2019-11-18T20:04:20.000000", "ABCDE", "INITIAL", "OFFL", null, "ORBIT", "0",
					"2019-02-20T22:49:21.000000", "2019-05-29T20:29:11.000000" },
			{ "113", "0", "2019-10-31T20:49:02.000000", "XYZ1234", "INITIAL", "NRTI", null, "ORBIT", "0",
					"2019-01-02T02:40:21.000000", "2019-04-29T18:29:10.000000" } };

	/**
	 *
	 * Create a test mission, a test spacecraft and test orders in the database.
	 *
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		logger.debug(">>> Starting to create test data in the database");
		createMissionAndSpacecraft(testMissionData, testSpacecraftData);
		fillDatabase(RepositoryService.getMissionRepository().findByCode(testMissionData[2]));
		logger.debug("<<< Finished creating test data in the database");

		logger.debug(">>> Starting to create orders in database");
		this.createRestOrderInDatabase(testMissionData[2], testOrderData[0]);
		this.createRestOrderInDatabase(testMissionData[2], testOrderData[1]);
		logger.debug("<<< Finished creating orders in database");
	}

	/**
	 *
	 * Deleting test data from the database
	 *
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		logger.debug(">>> Starting to delete test data in database");
		RepositoryService.getOrderRepository().deleteAll();
		RepositoryService.getProductClassRepository().deleteAll();
		RepositoryService.getConfiguredProcessorRepository().deleteAll();
		RepositoryService.getProcessorRepository().deleteAll();
		RepositoryService.getProcessorClassRepository().deleteAll();
		RepositoryService.getWorkflowRepository().deleteAll();
		RepositoryService.getSpacecraftRepository().deleteAll();
		RepositoryService.getMissionRepository().deleteAll();
		logger.debug("<<< Finished deleting test data in database");
	}

	/**
	 * Create a test order in the database from a data array
	 *
	 * @param missionCode the code of the test mission
	 * @param testData    an array of Strings representing the order to create
	 * @return a order with its attributes set to the input data
	 */
	private RestOrder createRestOrderInDatabase(String missionCode, String[] testData) {
		logger.debug("... creating order ");

		ProcessingOrder testOrder = new ProcessingOrder();

		if (null != RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(missionCode, testData[3])) {
			return OrderUtil
				.toRestOrder(RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(missionCode, testData[3]));
		} else {

			// Adding processing order parameters
			testOrder.setMission(RepositoryService.getMissionRepository().findByCode(missionCode));
			testOrder.setUuid(UUID.randomUUID());
			testOrder.setExecutionTime(Instant.from(OrbitTimeFormatter.parse(testData[2])));
			testOrder.setIdentifier(testData[3]);
			testOrder.setOrderState(OrderState.valueOf(testData[4]));
			testOrder.setProcessingMode(testData[5]);
			testOrder.setSlicingType(OrderSlicingType.valueOf(testData[7]));
			testOrder.setOutputFileClass(testOutputFileClass);

			if (testOrder.getSlicingType().toString().equals("TIME_SLICE")) {
				testOrder.setSliceDuration(Duration.parse(testData[6]));
			}

			// If slice_type is ORBIT then slice duration can be null
			testOrder.setSliceOverlap(Duration.ZERO);
			testOrder.setStartTime(Instant.from(OrbitTimeFormatter.parse(testData[9])));
			testOrder.setStopTime(Instant.from(OrbitTimeFormatter.parse(testData[10])));

			for (String[] testFilterCondition : testFilterConditions) {
				Parameter filterCondition = new Parameter();
				filterCondition.init(ParameterType.valueOf(testFilterCondition[1]), testFilterCondition[2]);
				// TODO upgrade to new DB model
				// testOrder.getFilterConditions().put(testFilterConditions[i][0],
				// filterCondition);
			}

			for (String[] element : testOutputParam) {
				Parameter outputParam = new Parameter();
				outputParam.init(ParameterType.valueOf(element[1]), element[2]);
				testOrder.getOutputParameters().put(element[0], outputParam);
			}

			for (String element : testConfProc) {
				ConfiguredProcessor reqProc = RepositoryService.getConfiguredProcessorRepository()
					.findByMissionCodeAndIdentifier(missionCode, element);
				if (null != reqProc) {
					testOrder.getRequestedConfiguredProcessors().add(reqProc);
				}
			}

			for (ProductClass prodClass : RepositoryService.getProductClassRepository().findByProductType(testInputProdClass)) {
				testOrder.getInputProductClasses().add(prodClass);
			}

			for (String element : testReqProdClass) {
				Set<ProductClass> set = new HashSet<>(RepositoryService.getProductClassRepository().findByProductType(element));
				testOrder.setRequestedProductClasses(set);
			}

			for (String[] testReqOrbit : testReqOrbits) {
				List<Orbit> orbits = RepositoryService.getOrbitRepository()
					.findByMissionCodeAndSpacecraftCodeAndOrbitNumberBetween(missionCode, testReqOrbit[0],
							Integer.valueOf(testReqOrbit[1]), Integer.valueOf(testReqOrbit[2]));
				testOrder.setRequestedOrbits(orbits);
			}

			testOrder = RepositoryService.getOrderRepository().save(testOrder);
			testData[0] = String.valueOf(testOrder.getId());
		}

		logger.debug("Created test order {}", testOrder.getId());

		return OrderUtil.toRestOrder(testOrder);
	}

	/**
	 * Create an local test order from a data array
	 *
	 * @return a RestOrder with some initial data for testing purposes
	 */
	private RestOrder createLocalTestOrder() {
		logger.debug(">>> Creating local test RestOrder");

		RestOrder testOrder = new RestOrder();

		testOrder.getConfiguredProcessors().add(testConfProc[0]);
		testOrder.setExecutionTime(Date.from(Instant.from(OrbitTimeFormatter.parse(testOrderData[2][2]))));
		testOrder.setIdentifier(testOrderData[2][3]);
		testOrder.getInputProductClasses().add(testInputProdClass);
		testOrder.setMissionCode("UTM");
		testOrder.setOrderState(OrderState.INITIAL.toString());
		testOrder.setOutputFileClass(testOutputFileClass);
		testOrder.setProcessingMode(testOrderData[2][5]);
		testOrder.getRequestedProductClasses().add(testReqProdClass[0]);
		testOrder.setSlicingType(OrderSlicingType.NONE.toString());
		testOrder.setStartTime(testOrderData[2][9]);
		testOrder.setStopTime(testOrderData[2][10]);
		testOrder.setUuid(UUID.randomUUID().toString());

		for (String[] element : testOutputParam) {
			RestParameter outputParam = new RestParameter();
			outputParam.setKey(element[0]);
			outputParam.setParameterType(element[1]);
			outputParam.setParameterValue(element[2]);
			testOrder.getOutputParameters().add(outputParam);
		}

		return testOrder;
	}

	/**
	 * Create a test mission and a test spacecraft in the database
	 *
	 * @param missionData    The data from which to create the mission
	 * @param spacecraftData The data from which to create the spacecraft
	 */
	private static void createMissionAndSpacecraft(String[] missionData, String[] spacecraftData) {
		if (null != RepositoryService.getMissionRepository().findByCode(missionData[2])) {
			return;
		}

		Mission testMission = new Mission();
		Spacecraft testSpacecraft = new Spacecraft();

		logger.debug("... creating mission {}", missionData[2]);

		// adding mission parameters
		testMission.setCode(missionData[2]);
		testMission.setName(missionData[3]);
		testMission.getProcessingModes().clear();
		testMission.getProcessingModes().add(missionData[4]);
		testMission.getFileClasses().clear();
		testMission.getFileClasses().add(missionData[5]);
		testMission.getFileClasses().add(testOutputFileClass);
		testMission.setProductFileTemplate(missionData[6]);
		testMission = RepositoryService.getMissionRepository().save(testMission);

		logger.debug("... creating spacecraft {}", spacecraftData[2]);

		// adding spacecraft parameters
		testSpacecraft.setMission(testMission);
		testSpacecraft.setCode(spacecraftData[1]);
		testSpacecraft.setName(spacecraftData[2]);
		testSpacecraft = RepositoryService.getSpacecraftRepository().save(testSpacecraft);

		testMission.getSpacecrafts().clear();
		testMission.getSpacecrafts().add(testSpacecraft);

		RepositoryService.getMissionRepository().save(testMission);
	}

	/**
	 * Filling the database with some initial data for testing purposes
	 *
	 * @param mission The mission that is referenced by the data filled in the database
	 */
	private static void fillDatabase(Mission mission) {
		logger.debug("... adding product classes");
		ProductClass productClass0 = new ProductClass();
		productClass0.setProductType(testInputProdClass);
		productClass0.setMission(mission);
		RepositoryService.getProductClassRepository().save(productClass0);

		ProductClass productClass1 = new ProductClass();
		productClass1.setProductType(testReqProdClass[0]);
		productClass1.setMission(mission);
		RepositoryService.getProductClassRepository().save(productClass1);

		ProductClass productClass2 = new ProductClass();
		productClass2.setProductType(testReqProdClass[1]);
		productClass2.setMission(mission);
		RepositoryService.getProductClassRepository().save(productClass2);

		logger.debug("... adding workflows");
		Workflow workflow0 = new Workflow();
		workflow0.setMission(mission);
		workflow0.setName(testWorkflow[0][0]);
		workflow0.setWorkflowVersion(testWorkflowVersion);
		workflow0.setUuid(UUID.fromString(testWorkflow[0][1]));
		RepositoryService.getWorkflowRepository().save(workflow0);
		Workflow workflow1 = new Workflow();
		workflow1.setMission(mission);
		workflow1.setName(testWorkflow[1][0]);
		workflow1.setWorkflowVersion(testWorkflowVersion);
		workflow1.setUuid(UUID.fromString(testWorkflow[1][1]));
		RepositoryService.getWorkflowRepository().save(workflow1);

		logger.debug("... adding a processor class");
		ProcessorClass processorClass = new ProcessorClass();
		processorClass.setMission(mission);
		processorClass.setProcessorName("randomName");
		processorClass.setId(RepositoryService.getProcessorClassRepository().save(processorClass).getId());

		logger.debug("... adding a processor");
		Processor processor = new Processor();
		processor.setProcessorClass(processorClass);
		processor.setId(RepositoryService.getProcessorRepository().save(processor).getId());

		logger.debug("... adding configured processors");
		ConfiguredProcessor configProc0 = new ConfiguredProcessor();
		configProc0.setProcessor(processor);
		configProc0.setIdentifier(testConfProc[0]);
		RepositoryService.getConfiguredProcessorRepository().save(configProc0);
		ConfiguredProcessor configProc1 = new ConfiguredProcessor();
		configProc0.setProcessor(processor);
		configProc1.setIdentifier(testConfProc[1]);
		RepositoryService.getConfiguredProcessorRepository().save(configProc1);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl#createOrder(de.dlr.proseo.model.rest.model.RestOrder)}.
	 */
	@Test
	public final void testCreateOrder() {
		logger.trace(">>> testOrders()");

		RestOrder toBeCreated = createLocalTestOrder();
		ResponseEntity<RestOrder> created = oci.createOrder(toBeCreated);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, created.getStatusCode());
		assertEquals("Error during order creation.", toBeCreated.getIdentifier(), created.getBody().getIdentifier());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl#getOrders(java.lang.String, java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
	 */
	@Test
	public final void testGetOrders() {
		logger.trace(">>> testGetOrders()");

		List<ProcessingOrder> expectedOrders = RepositoryService.getOrderRepository().findAll();
		expectedOrders.removeIf(po -> "UTM" != po.getMission().getCode());
		ResponseEntity<List<RestOrder>> retrievedOrders = oci.getOrders("UTM", null, null, null, null, null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedOrders.getStatusCode());
		assertTrue("Wrong number of orders retrieved.", expectedOrders.size() == retrievedOrders.getBody().size());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl#getAndSelectOrders(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[], java.lang.String, java.lang.String, java.lang.Long, java.lang.Long, java.lang.String[])}.
	 */
	@Test
	public final void testGetAndSelectOrders() {
		logger.trace(">>> testGetAndSelectOrders()");

		List<ProcessingOrder> expectedOrders = RepositoryService.getOrderRepository().findAll();
		expectedOrders.removeIf(po -> "UTM" != po.getMission().getCode());
		ResponseEntity<List<RestOrder>> retrievedOrders = oci.getAndSelectOrders("UTM", null, null, null, null, null, null, null,
				null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedOrders.getStatusCode());
		assertTrue("Wrong number of orders retrieved.", expectedOrders.size() == retrievedOrders.getBody().size());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl#countSelectOrders(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[], java.lang.String, java.lang.String, java.lang.Long, java.lang.Long, java.lang.String[])}.
	 */
	@Test
	public final void testCountSelectOrders() {
		logger.trace(">>> testCountSelectOrders()");

		List<ProcessingOrder> expectedOrders = RepositoryService.getOrderRepository().findAll();
		expectedOrders.removeIf(po -> "UTM" != po.getMission().getCode());
		ResponseEntity<String> retrievedOrders = oci.countSelectOrders("UTM", null, null, null, null, null, null, null, null);

		logger.trace("Expected amount is " + expectedOrders.size());
		logger.trace("Retrieved size is " + retrievedOrders.getBody());

		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedOrders.getStatusCode());
		assertTrue("Wrong number of orders retrieved.", expectedOrders.size() == Long.valueOf(retrievedOrders.getBody()));
	}

	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl#getOrderById(java.lang.Long)}.
	 */
	@Test
	public final void testGetOrderById() {
		logger.trace(">>> testGetOrderById()");

		RestOrder expectedOrder = OrderUtil.toRestOrder(RepositoryService.getOrderRepository().findAll().get(0));

		ResponseEntity<RestOrder> retrievedOrder = oci.getOrderById(expectedOrder.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedOrder.getStatusCode());
		assertTrue("Wrong order retrieved.", expectedOrder.getIdentifier().equals(retrievedOrder.getBody().getIdentifier()));
	}

	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl#deleteOrderById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteOrderById() {
		logger.trace(">>> testDeleteOrderById()");

		List<ProcessingOrder> beforeDeletion = RepositoryService.getOrderRepository().findAll();
		ProcessingOrder toBeDeleted = beforeDeletion.get(0);

		ResponseEntity<?> entity = oci.deleteOrderById(toBeDeleted.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.NO_CONTENT, entity.getStatusCode());

		List<ProcessingOrder> afterDeletion = RepositoryService.getOrderRepository().findAll();
		assertTrue("Order not deleted.", !afterDeletion.contains(toBeDeleted));

	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl#modifyOrder(java.lang.Long, de.dlr.proseo.model.rest.model.RestOrder)}.
	 */
	@Test
	public final void testModifyOrder() {
		logger.trace(">>> testModifyOrder()");

		RestOrder toBeModified = OrderUtil.toRestOrder(RepositoryService.getOrderRepository().findAll().get(0));
		toBeModified.setOrderState(OrderState.APPROVED.toString());

		ResponseEntity<RestOrder> entity = oci.modifyOrder(toBeModified.getId(), toBeModified);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());
		assertEquals("Modification unsuccessfull.", toBeModified.getIdentifier(), entity.getBody().getIdentifier());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.OrderControllerImpl#countOrders(java.lang.String, java.lang.String, java.lang.Long)}.
	 */
	@Test
	public final void testCountOrders() {
		logger.trace(">>> testCountOrders()");

		List<ProcessingOrder> expectedOrders = RepositoryService.getOrderRepository().findAll();
		expectedOrders.removeIf(po -> "UTM" != po.getMission().getCode());

		ResponseEntity<String> retrievedOrders = oci.countOrders("UTM", null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedOrders.getStatusCode());
		assertTrue("Wrong number of orders retrieved.", expectedOrders.size() == Long.valueOf(retrievedOrders.getBody()));

	}

}
