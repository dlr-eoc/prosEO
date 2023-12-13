/**
 * ProcessingOrderMgrTest.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityNotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
import de.dlr.proseo.model.rest.model.RestClassOutputParameter;
import de.dlr.proseo.model.rest.model.RestInputReference;
import de.dlr.proseo.model.rest.model.RestOrbitQuery;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.model.util.OrderUtil;

/**
 * Testing the service methods required to create, modify and delete processing order in the prosEO database, and to query the
 * database about such orders
 *
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DataJpaTest
@AutoConfigureTestEntityManager
public class ProcessingOrderMgrTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProcessingOrderMgrTest.class);

	/** The processing order manager under test */
	@Autowired
	ProcessingOrderMgr pom;

	/** A REST template builder for this class */
	@MockBean
	RestTemplateBuilder rtb;

	// Test data
	private static String[][] testMissionData = {
			// id, version, code, name, processing_mode, file_class, product_file_template
			{ "1", "0", "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" },
			{ "2", "0", "PTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" } };
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
			{ "111", "0", "2019-11-17T22:49:21.000000", "XYZ", "RUNNING", "NRTI", "PT20.345S", "TIME_SLICE", "0",
					"2019-08-29T22:49:21.000000", "2019-08-29T22:49:21.000000" },
			{ "112", "0", "2019-11-18T20:04:20.000000", "ABCDE", "PLANNED", "OFFL", null, "ORBIT", "0",
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
		createMissionAndSpacecraft(testMissionData[0], testSpacecraftData);
		createMissionAndSpacecraft(testMissionData[1], testSpacecraftData);
		fillDatabase(RepositoryService.getMissionRepository().findByCode(testMissionData[0][2]));
		logger.debug("<<< Finished creating test data in the database");

		logger.debug(">>> Starting to create orders in database");
		this.createRestOrderInDatabase(testMissionData[0][2], testOrderData[0]);
		this.createRestOrderInDatabase(testMissionData[0][2], testOrderData[1]);
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
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.ProcessingOrderMgr#createOrder(de.dlr.proseo.model.rest.model.RestOrder)}.
	 */
	@Test
	@WithMockUser(username = "UTM-testuser", roles = {})
	public final void testCreateOrder() {
		logger.debug(">>> testCreateOrder()");

		// Get a valid sample order from which deviations can be tested.
		RestOrder testOrder = this.createRestOrderInDatabase(testMissionData[0][2], testOrderData[2]);
		RepositoryService.getOrderRepository().deleteById(testOrder.getId());
		RestOrder successfullyCreated = pom.createOrder(testOrder);
		RepositoryService.getOrderRepository().deleteById(successfullyCreated.getId());

		// The RestOrder parameter must not be null.
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(null));

		// It is not allowed to create an order without a mission.
		testOrder.setMissionCode(null);
		assertThrows(SecurityException.class, () -> pom.createOrder(testOrder));
		testOrder.setMissionCode("UTM");

		// The user is only allowed the mission they are logged into (here UTM).
		testOrder.setMissionCode("notUTM");
		assertThrows(SecurityException.class, () -> pom.createOrder(testOrder));
		testOrder.setMissionCode("UTM");

		// No two orders can have the same UUID.
		testOrder.setUuid(RepositoryService.getOrderRepository().findAll().get(0).getUuid().toString());
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setUuid(UUID.randomUUID().toString());

		// Identifiers must be unique.
		testOrder.setIdentifier(RepositoryService.getOrderRepository().findAll().get(0).getIdentifier());
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setIdentifier(testOrderData[2][3]);

		// Orders must be in state INITIAL.
		testOrder.setOrderState(OrderState.APPROVED.toString());
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setOrderState(OrderState.INITIAL.toString());

		// Time interval must be given
		testOrder.setStartTime(null);
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setStartTime("");
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setStartTime("wrongFormat");
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setStartTime(testOrderData[2][9]);

		testOrder.setStopTime(null);
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setStopTime("");
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setStopTime("wrongFormat");
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setStopTime(testOrderData[2][10]);

		// Stop time must not precede start time.
		testOrder.setStartTime(testOrderData[2][10]);
		testOrder.setStopTime(testOrderData[2][9]);
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setStartTime(testOrderData[2][9]);
		testOrder.setStopTime(testOrderData[2][10]);

		// Slice duration is obligatory for slicing type TIME_SLICE.
		testOrder.setSlicingType(OrderSlicingType.TIME_SLICE.toString());
		testOrder.setSliceDuration(0L);
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setSliceDuration(null);

		// If orbits are provided, they must be valid.
		RestOrbitQuery restOrbitQuery = new RestOrbitQuery();
		restOrbitQuery.setOrbitNumberFrom(0L);
		restOrbitQuery.setOrbitNumberTo(10L);
		testOrder.getOrbits().add(restOrbitQuery);
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.getOrbits().clear();

		// If an input product class is provided, it must be valid.
		testOrder.getInputProductClasses().add("invalidInputProductClass");
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.getInputProductClasses().clear();
		for (ProductClass prodClass : RepositoryService.getProductClassRepository().findByProductType(testInputProdClass)) {
			testOrder.getInputProductClasses().add(prodClass.toString());
		}

		// If a workflow is provided, is must be valid.
		testOrder.setWorkflowName("wrongName");
		testOrder.setUuid(testWorkflow[0][1]);
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setWorkflowName(testWorkflow[0][0]);
		testOrder.setWorkflowUuid(UUID.randomUUID().toString());
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setWorkflowUuid(testWorkflow[1][1]);
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setWorkflowUuid(testWorkflow[0][1]);

		// Output parameters must be provided and valid.
		testOrder.getOutputParameters().clear();
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		RestParameter outputParameter = new RestParameter();
		outputParameter.setKey("invalidKey");
		outputParameter.setParameterType("invalidType");
		outputParameter.setParameterValue("invalidValue");
		testOrder.getOutputParameters().add(outputParameter);
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.getOutputParameters().clear();
		for (String[] element : testOutputParam) {
			RestParameter outputParam = new RestParameter();
			outputParam.setKey(element[0]);
			outputParam.setParameterType(element[1]);
			outputParam.setParameterValue(element[2]);
			testOrder.getOutputParameters().add(outputParam);
		}

		// Provided class output parameters must be valid.
		testOrder.getClassOutputParameters().clear();
		RestClassOutputParameter restClassOutputParameter = new RestClassOutputParameter();
		restClassOutputParameter.getOutputParameters().add(outputParameter);
		testOrder.getClassOutputParameters().add(restClassOutputParameter);
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.getClassOutputParameters().clear();

		// Requested product classes must be set.
		testOrder.getRequestedProductClasses().clear();
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		for (String element : testReqProdClass) {
			testOrder.getRequestedProductClasses().add(element);
		}

		// A valid processing mode must be provided.
		testOrder.setProcessingMode(null);
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setProcessingMode("invalidProcessingMode");
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setProcessingMode(testOrderData[2][5]);

		// A valid output file class must be provided.
		testOrder.setOutputFileClass(null);
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setOutputFileClass("invalidOutputFileClass");
		assertThrows(IllegalArgumentException.class, () -> pom.createOrder(testOrder));
		testOrder.setOutputFileClass(testOutputFileClass);
	}

	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.ProcessingOrderMgr#deleteOrderById(java.lang.Long)}.
	 */
	@Test
	@WithMockUser(username = "UTM-testuser", roles = {})
	public final void testDeleteOrderById() {
		logger.debug(">>> testDeleteOrderById()");

		assertTrue(RepositoryService.getOrderRepository().findById(Long.valueOf(testOrderData[0][0])).isPresent());
		pom.deleteOrderById(Long.valueOf(testOrderData[0][0]));
		assertTrue(RepositoryService.getOrderRepository().findById(Long.valueOf(testOrderData[0][0])).isEmpty());
	}

	/**
	 * Test method for {@link de.dlr.proseo.ordermgr.rest.ProcessingOrderMgr#getOrderById(java.lang.Long)}.
	 */
	@Test
	@WithMockUser(username = "UTM-testuser", roles = {})
	public final void testGetOrderById() {
		logger.debug(">>> testGetOrderById()");

		assertEquals(testOrderData[0][3], pom.getOrderById(Long.valueOf(testOrderData[0][0])).getIdentifier());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.ProcessingOrderMgr#modifyOrder(java.lang.Long, de.dlr.proseo.model.rest.model.RestOrder)}.
	 * without authorities
	 */
	@Test
	@WithMockUser(username = "UTM-testuser", roles = {})
	public final void testModifyOrderWithoutAuthorities() {
		logger.debug(">>> testModifyOrderWithoutAuthorities()");

		RestOrder testOrder = this.createRestOrderInDatabase(testMissionData[0][2], testOrderData[2]);

		// Orders can only be approved by ORDER_APPROVERS
		testOrder.setOrderState(OrderState.APPROVED.toString());
		assertThrows(SecurityException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));

		// Changes other than state changes are only allowed with Role_ORDER_MGR
		testOrder.setIdentifier("any");
		assertThrows(SecurityException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.ProcessingOrderMgr#modifyOrder(java.lang.Long, de.dlr.proseo.model.rest.model.RestOrder)}.
	 */
	@Test
	@WithMockUser(username = "UTM-testuser", roles = { "ORDER_APPROVER", "ORDER_MGR" })
	public final void testModifyOrder() {
		logger.debug(">>> testModifyOrder()");

		RestOrder testOrder = createLocalTestOrder();

		// ID is mandatory
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(null, testOrder));

		// User must be authorized
		testOrder.setMissionCode("PTM");
		assertThrows(SecurityException.class, () -> pom.modifyOrder(0L, testOrder));
		testOrder.setMissionCode("UTM");

		// Order must already be in database
		assertThrows(EntityNotFoundException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));

		// UUID cannot be changed
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());
		testOrder.setUuid(UUID.randomUUID().toString());
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));

		// Mission cannot be changed
		testOrder.setMissionCode("PTM");
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());
		testOrder.setMissionCode("UTM");
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));

		// Order must be in state INITIAL
		testOrder.setOrderState(OrderState.COMPLETED.toString());
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
		testOrder.setOrderState(OrderState.INITIAL.toString());
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());

		// Orders can only be approved by ORDER_APPROVERS, no exception expected
		testOrder.setOrderState(OrderState.APPROVED.toString());
		pom.modifyOrder(testOrder.getId(), testOrder);
		testOrder.setOrderState(OrderState.INITIAL.toString());
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());

		// Stop time must not precede start time
		testOrder.setStopTime(Instant.now().toString());
		testOrder.setStartTime(Instant.now().toString());
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
		testOrder.setStartTime(Instant.now().toString());
		testOrder.setStopTime(Instant.now().toString());
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());

		// Updated input product classes must be valid
		testOrder.getInputProductClasses().add("invalid");
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
		testOrder.getInputProductClasses().remove("invalid");
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());

		// Updated requested product classes must be valid
		testOrder.getRequestedProductClasses().add("invalid");
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
		testOrder.getRequestedProductClasses().remove("invalid");
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());

		// Updated class output parameters must be valid
		RestClassOutputParameter invalidParam = new RestClassOutputParameter();
		invalidParam.setProductClass("invalid");
		invalidParam.getOutputParameters().clear();
		testOrder.getClassOutputParameters().add(invalidParam);
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
		testOrder.getClassOutputParameters().clear();
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());

		// Updated output file classes must be valid
		testOrder.setOutputFileClass("invalid");
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
		testOrder.setOutputFileClass(testOutputFileClass);
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());

		// Updated processing mode must be valid
		testOrder.setProcessingMode("invalid");
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
		testOrder.setProcessingMode(testOrderData[2][5]);
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());

		// Updated configured processors must be valid
		testOrder.getConfiguredProcessors().add("invalid");
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
		testOrder.getConfiguredProcessors().remove("invalid");
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());

		// Updated orbit range must be valid
		testOrder.getOrbits().add(new RestOrbitQuery());
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
		testOrder.getOrbits().clear();
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());
		RestOrbitQuery invalidQuery = new RestOrbitQuery();
		invalidQuery.setOrbitNumberFrom(0L);
		invalidQuery.setOrbitNumberTo(0L);
		invalidQuery.setSpacecraftCode("invalid");
		testOrder.getOrbits().add(invalidQuery);
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
		testOrder.getOrbits().clear();
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());

		// Input product reference may not be updated
		testOrder.setInputProductReference(new RestInputReference());
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());
		testOrder.setInputProductReference(null);
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
		testOrder.setInputProductReference(null);
		testOrder.setId(RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(testOrder)).getId());

		// Workflow may not be updated
		ProcessingOrder po = OrderUtil.toModelOrder(testOrder);
		po.setWorkflow(RepositoryService.getWorkflowRepository()
				.findByMissionCodeAndNameAndVersion("UTM", testWorkflow[0][0], testWorkflowVersion));
		testOrder.setId(RepositoryService.getOrderRepository().save(po).getId());
		testOrder.setWorkflowName(testWorkflow[1][0]);
		testOrder.setWorkflowUuid(testWorkflow[1][1]);
		assertThrows(IllegalArgumentException.class, () -> pom.modifyOrder(testOrder.getId(), testOrder));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.ProcessingOrderMgr#getOrders(java.lang.String, java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
	 */
	@Test
	@WithMockUser(username = "UTM-testuser", roles = {})
	public final void testGetOrders() {
		logger.debug(">>> testGetOrders()");

		Instant start = ZonedDateTime.of(2010, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant();
		Instant stop = ZonedDateTime.of(2030, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant();

		// User must be authorized for the mission
		assertThrows(SecurityException.class, () -> pom.getOrders("PTM", null, null, null, null, null, null));

		/*
		 * Using values from the test order data which was used to initialize the order repository means that the query must return
		 * at least one order. If no mission was specified, it is acquired from the security service. Not specifying additional
		 * parameters returns all orders for the given mission.
		 */
		assertTrue(pom.getOrders(null, null, null, null, null, null, null).size() >= 1);
		assertTrue(pom.getOrders("UTM", null, null, null, null, null, null).size() >= 1);
		assertTrue(pom.getOrders("UTM", testOrderData[0][3], null, null, null, null, null).size() >= 1);
		assertTrue(pom.getOrders("UTM", null, testReqProdClass, null, null, null, null).size() >= 1);
		assertTrue(pom.getOrders("UTM", null, null, Date.from(start), null, null, null).size() >= 1);
		assertTrue(pom.getOrders("UTM", null, null, null, Date.from(stop), null, null).size() >= 1);
		assertTrue(pom.getOrders("UTM", null, null, null, null, Date.from(start), null).size() >= 1);
		assertTrue(pom.getOrders("UTM", null, null, null, null, null, Date.from(stop)).size() >= 1);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.ProcessingOrderMgr#getAndSelectOrders(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[], java.lang.String, java.lang.String, java.lang.Long, java.lang.Long, java.lang.String[])}.
	 */
	@Test
	@WithMockUser(username = "UTM-testuser", roles = {})
	public final void testGetAndSelectOrders() {
		logger.debug(">>> testGetAndSelectOrders()");

		RestOrder differentMission = createLocalTestOrder();
		differentMission.setMissionCode("PTM");
		RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(differentMission));

		Instant start = ZonedDateTime.of(2010, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant();
		Instant stop = ZonedDateTime.of(2030, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant();

		/*
		 * Using values from the test order data which was used to initialize the order repository means that the query must return
		 * at least one order. Mission code is obligatory.
		 */
		assertThrows(IllegalArgumentException.class,
				() -> pom.getAndSelectOrders(null, null, null, null, null, null, null, null, null));
		assertTrue(pom.getAndSelectOrders("PTM", null, null, null, null, null, null, null, null).size() >= 1);
		assertTrue(pom.getAndSelectOrders("UTM", null, null, null, null, null, null, null, null).size() >= 1);
		assertTrue(pom.getAndSelectOrders("UTM", testOrderData[0][3], null, null, null, null, null, null, null).size() >= 1);
		assertTrue(pom.getAndSelectOrders("UTM", null, new String[] { "RUNNING", "PLANNED" }, null, null, null, null, null, null)
			.size() >= 1);
		assertTrue(pom.getAndSelectOrders("UTM", null, null, testReqProdClass, null, null, null, null, null).size() >= 1);
		assertTrue(pom.getAndSelectOrders("UTM", null, null, null, start.toString(), null, null, null, null).size() >= 1);
		assertTrue(pom.getAndSelectOrders("UTM", null, null, null, null, stop.toString(), null, null, null).size() >= 1);
		assertTrue(pom.getAndSelectOrders("UTM", null, null, null, null, null, 0L, null, null).size() >= 1);
		assertTrue(pom.getAndSelectOrders("UTM", null, null, null, null, null, null, Long.MAX_VALUE, null).size() >= 1);
		assertTrue(pom.getAndSelectOrders("UTM", null, null, null, null, null, null, null, null).size() >= 1);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.ordermgr.rest.ProcessingOrderMgr#countSelectOrders(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[], java.lang.String, java.lang.String, java.lang.Long, java.lang.Long, java.lang.String[])}.
	 */
	@Test
	@WithMockUser(username = "UTM-testuser", roles = {})
	public final void testCountSelectOrders() {
		logger.debug(">>> testCountSelectOrders()");

		RestOrder differentMission = createLocalTestOrder();
		differentMission.setMissionCode("PTM");
		RepositoryService.getOrderRepository().save(OrderUtil.toModelOrder(differentMission));

		Instant start = ZonedDateTime.of(2010, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant();
		Instant stop = ZonedDateTime.of(2030, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant();

		/*
		 * Using values from the test order data which was used to initialize the order repository means that the query must return
		 * at least one order. Mission code is obligatory.
		 */
		assertThrows(IllegalArgumentException.class,
				() -> pom.countSelectOrders(null, null, null, null, null, null, null, null, null));
		assertTrue(Long.valueOf(pom.countSelectOrders("PTM", null, null, null, null, null, null, null, null)) >= 1);
		assertTrue(Long.valueOf(pom.countSelectOrders("UTM", null, null, null, null, null, null, null, null)) >= 1);
		assertTrue(Long.valueOf(pom.countSelectOrders("UTM", testOrderData[0][3], null, null, null, null, null, null, null)) >= 1);
		assertTrue(Long.valueOf(pom.countSelectOrders("UTM", null, new String[] { "RUNNING", "PLANNED" }, null, null, null, null,
				null, null)) >= 1);
		assertTrue(Long.valueOf(pom.countSelectOrders("UTM", null, null, testReqProdClass, null, null, null, null, null)) >= 1);
		assertTrue(Long.valueOf(pom.countSelectOrders("UTM", null, null, null, start.toString(), null, null, null, null)) >= 1);
		assertTrue(Long.valueOf(pom.countSelectOrders("UTM", null, null, null, null, stop.toString(), null, null, null)) >= 1);
		assertTrue(Long.valueOf(pom.countSelectOrders("UTM", null, null, null, null, null, 0L, null, null)) >= 1);
		assertTrue(Long.valueOf(pom.countSelectOrders("UTM", null, null, null, null, null, null, Long.MAX_VALUE, null)) >= 1);
		assertTrue(Long.valueOf(pom.countSelectOrders("UTM", null, null, null, null, null, null, null, null)) >= 1);
	}
}
