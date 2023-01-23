/**
 * WorkflowControllerTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
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
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.WorkflowOption;
import de.dlr.proseo.model.WorkflowOption.WorkflowOptionType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.ProcessorManagerApplication;
import de.dlr.proseo.procmgr.rest.model.RestWorkflow;
import de.dlr.proseo.procmgr.rest.model.WorkflowUtil;

/**
 * Testing WorkflowControllerImpl.class.
 *
 * TODO test invalid REST requests
 *
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProcessorManagerApplication.class)
@WithMockUser(username = "UTM-testuser", roles = {})
@AutoConfigureTestEntityManager
@Transactional
public class WorkflowControllerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(WorkflowControllerTest.class);

	/** The WorkflowControllerImpl under test */
	@Autowired
	private WorkflowControllerImpl wci;

	/** A REST template builder for this class */
	@MockBean
	RestTemplateBuilder rtb;

	// Test data
	private static String[] testMissionData =
			// code, name, processing_mode, file_class, product_file_template
			{ "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" };
	private static String[] testSpacecraftData =
			// code, name
			{ "S_TDX1", "Tandom-X" };
	private static String[][] testWorkflowData = {
			// name, uuid, workflowVersion, inputProductClass, outputProductClass,
			// configuredProcessor
			{ "CLOUD_to_O3", UUID.randomUUID().toString(), "1.0", "L2__CLOUD_", "L2__O3____",
					"UPAS 02.04.01 2022-11-02" },
			{ "AER_to_SO2", UUID.randomUUID().toString(), "1.3", "L2__AER_AI", "L2__SO2___", "NL-L2 02.04.00" }, };
	private static String[][] testWorkflowOptions = {
			// workflowName, name, optionType, valueRange
			{ "CLOUD_to_O3", "someName", "NUMBER", "someValue" }, 
			{ "AER_to_SO2", "someOtherName", "NUMBER", "someValue" } };

	/**
	 *
	 * Create a test mission, a test spacecraft and test workflows in the database.
	 *
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		logger.trace(">>> Starting to create test data in the database");

		createMissionAndSpacecraft(testMissionData, testSpacecraftData);
		fillDatabase(RepositoryService.getMissionRepository().findByCode(testMissionData[0]));

		createTestWorkflow(testWorkflowData[0], testWorkflowOptions[0]);
		createTestWorkflow(testWorkflowData[1], testWorkflowOptions[1]);

		logger.trace("<<< Finished creating test data in database");
	}

	/**
	 *
	 * Deleting test data from the database
	 *
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		logger.trace(">>> Starting to delete test data in database");
		RepositoryService.getWorkflowOptionRepository().deleteAll();
		RepositoryService.getWorkflowRepository().deleteAll();
		RepositoryService.getProductClassRepository().deleteAll();
		RepositoryService.getConfiguredProcessorRepository().deleteAll();
		RepositoryService.getProcessorRepository().deleteAll();
		RepositoryService.getProcessorClassRepository().deleteAll();
		RepositoryService.getSpacecraftRepository().deleteAll();
		RepositoryService.getMissionRepository().deleteAll();
		logger.trace("<<< Finished deleting test data in database");
	}

	/**
	 * Create a test workflow in the database
	 *
	 * @param workflowData The data from which to create the workflow
	 * @returns the created workflow
	 */
	private static Workflow createTestWorkflow(String[] workflowData, String[] workflowOptionData) {
		logger.trace("... creating a test workflow in the database");

		Workflow workflow = new Workflow();

		// set workflow attributes
		workflow.setName(workflowData[0]);
		workflow.setUuid(UUID.fromString(workflowData[1]));
		workflow.setWorkflowVersion(workflowData[2]);
		workflow.setInputProductClass(
				RepositoryService.getProductClassRepository().findByProductType(workflowData[3]).get(0));
		workflow.setOutputProductClass(
				RepositoryService.getProductClassRepository().findByProductType(workflowData[4]).get(0));
		workflow.setConfiguredProcessor(RepositoryService.getConfiguredProcessorRepository()
				.findByMissionCodeAndIdentifier(testMissionData[0], workflowData[5]));

		// save workflow in database
		workflow = RepositoryService.getWorkflowRepository().save(workflow);

		// set workflow options
		WorkflowOption workflowOption = new WorkflowOption();
		workflowOption.setWorkflow(workflow);
		workflowOption.setName(workflowOptionData[1]);
		workflowOption.setType(WorkflowOptionType.get(workflowOptionData[2].toLowerCase()));
		workflowOption.getValueRange().add(workflowOptionData[3]);

		// save workflow option in database
		workflowOption = RepositoryService.getWorkflowOptionRepository().save(workflowOption);
		workflow.getWorkflowOptions().add(workflowOption);

		return workflow;
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

		logger.trace("... creating mission {}", missionData[0]);

		// adding mission attributes
		testMission.setCode(missionData[0]);
		testMission.setName(missionData[1]);
		testMission.getProcessingModes().add(missionData[2]);
		testMission.getFileClasses().add(missionData[3]);
		testMission.setProductFileTemplate(missionData[4]);
		
		// saving mission in the database
		testMission = RepositoryService.getMissionRepository().save(testMission);

		logger.trace("... creating spacecraft {}", spacecraftData[1]);

		// adding spacecraft attributes
		testSpacecraft.setMission(testMission);
		testSpacecraft.setCode(spacecraftData[0]);
		testSpacecraft.setName(spacecraftData[1]);
		
		// saving spacecraft in the database
		testSpacecraft = RepositoryService.getSpacecraftRepository().save(testSpacecraft);

		// assigning the spacecraft to the mission 
		testMission.getSpacecrafts().clear();
		testMission.getSpacecrafts().add(testSpacecraft);

		RepositoryService.getMissionRepository().save(testMission);
	}

	/**
	 * Filling the database with some initial data for testing purposes
	 *
	 * @param mission the mission to be referenced by the data filled in the
	 *                database
	 */
	private static void fillDatabase(Mission mission) {
		logger.debug("... adding input product classes");
		ProductClass productClass0 = new ProductClass();
		productClass0.setProductType(testWorkflowData[0][3]);
		productClass0.setMission(mission);
		RepositoryService.getProductClassRepository().save(productClass0);

		ProductClass productClass1 = new ProductClass();
		productClass1.setProductType(testWorkflowData[1][3]);
		productClass1.setMission(mission);
		RepositoryService.getProductClassRepository().save(productClass1);

		logger.debug("... adding a processor class and output product classes");
		ProcessorClass processorClass = new ProcessorClass();
		processorClass.setMission(mission);
		processorClass.setProcessorName("randomName");
		processorClass.setId(RepositoryService.getProcessorClassRepository().save(processorClass).getId());

		ProductClass productClass2 = new ProductClass();
		productClass2.setProductType(testWorkflowData[0][4]);
		productClass2.setMission(mission);
		productClass2.setProcessorClass(processorClass);
		productClass2.setId(RepositoryService.getProductClassRepository().save(productClass2).getId());

		ProductClass productClass3 = new ProductClass();
		productClass3.setProductType(testWorkflowData[1][4]);
		productClass3.setMission(mission);
		productClass3.setProcessorClass(processorClass);
		productClass3.setId(RepositoryService.getProductClassRepository().save(productClass3).getId());

		processorClass.getProductClasses().add(productClass2);
		processorClass.getProductClasses().add(productClass3);
		RepositoryService.getProcessorClassRepository().save(processorClass);

		logger.debug("... adding a processor");
		Processor processor = new Processor();
		processor.setProcessorClass(processorClass);
		processor.setId(RepositoryService.getProcessorRepository().save(processor).getId());

		logger.debug("... adding configured processors");
		ConfiguredProcessor configProc0 = new ConfiguredProcessor();
		configProc0.setProcessor(processor);
		configProc0.setIdentifier(testWorkflowData[0][5]);

		RepositoryService.getConfiguredProcessorRepository().save(configProc0);
		ConfiguredProcessor configProc1 = new ConfiguredProcessor();
		configProc1.setProcessor(processor);
		configProc1.setIdentifier(testWorkflowData[1][5]);
		RepositoryService.getConfiguredProcessorRepository().save(configProc1);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.workflowmgr.rest.WorkflowControllerImpl#createWorkflow(de.dlr.proseo.model.rest.model.RestWorkflow)}.
	 */
	@Test
	public final void testCreateWorkflow() {
		logger.trace(">>> testCreateWorkflow()");

		// retrieve and delete the test workflow from the database
		RestWorkflow toBeCreated = WorkflowUtil
				.toRestWorkflow(RepositoryService.getWorkflowRepository().findAll().get(0));
		RepositoryService.getWorkflowRepository().deleteById(toBeCreated.getId());

		// testing workflow creation with the workflow controller
		ResponseEntity<RestWorkflow> created = wci.createWorkflow(toBeCreated);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, created.getStatusCode());
		assertEquals("Error during workflow creation.", toBeCreated.getName(), created.getBody().getName());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.workflowmgr.rest.WorkflowControllerImpl#getWorkflows(java.lang.String, java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
	 */
	@Test
	public final void testGetWorkflows() {
		logger.trace(">>> testGetWorkflows()");

		// retrieve all workflows from the database, as all were created with the same
		// mission
		List<Workflow> expectedWorkflows = RepositoryService.getWorkflowRepository().findAll();

		// retrieve all workflows with the same mission as the test workflows from the
		// database via the workflow controller
		ResponseEntity<List<RestWorkflow>> retrievedWorkflows = wci.getWorkflows(testMissionData[0], null, null, null,
				null, null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedWorkflows.getStatusCode());
		assertTrue("Wrong number of workflows retrieved.",
				expectedWorkflows.size() == retrievedWorkflows.getBody().size());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.workflowmgr.rest.WorkflowControllerImpl#getWorkflowById(java.lang.Long)}.
	 */
	@Test
	public final void testGetWorkflowById() {
		logger.trace(">>> testGetWorkflowById()");

		// retrieve a test workflow from the database
		Workflow expectedWorkflow = RepositoryService.getWorkflowRepository().findAll().get(0);

		// retrieve a workflow with the workflow controller by using the id from the
		// test workflow
		ResponseEntity<RestWorkflow> retrievedWorkflow = wci.getWorkflowById(expectedWorkflow.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedWorkflow.getStatusCode());
		assertTrue("Wrong workflow retrieved.",
				expectedWorkflow.getName().equals(retrievedWorkflow.getBody().getName()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.workflowmgr.rest.WorkflowControllerImpl#deleteWorkflowById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteWorkflowById() {
		logger.trace(">>> testDeleteWorkflowById()");

		// chose one workflow from the database for deletion
		Workflow toBeDeleted = RepositoryService.getWorkflowRepository().findAll().get(0);

		// delete the chosen workflow via the workflow controller
		ResponseEntity<?> entity = wci.deleteWorkflowById(toBeDeleted.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.NO_CONTENT, entity.getStatusCode());

		// assert that the workflow was deleted
		assertTrue("Workflow not deleted.",
				RepositoryService.getWorkflowRepository().findById(toBeDeleted.getId()).isEmpty());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.workflowmgr.rest.WorkflowControllerImpl#modifyWorkflow(java.lang.Long, de.dlr.proseo.model.rest.model.RestWorkflow)}.
	 */
	@Test
	public final void testModifyWorkflow() {
		logger.trace(">>> testModifyWorkflow()");

		Workflow inRepository = RepositoryService.getWorkflowRepository().findAll().get(0);
		RestWorkflow toBeModified = WorkflowUtil.toRestWorkflow(inRepository);
		String previousWorkflowVersion = toBeModified.getWorkflowVersion();
		toBeModified.setWorkflowVersion("10.1");

		ResponseEntity<RestWorkflow> entity = wci.modifyWorkflow(toBeModified.getId(), toBeModified);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());
		assertTrue("Modification unsuccessfull", toBeModified.getVersion() + 1 == entity.getBody().getVersion());
		assertNotEquals("Modification unsuccessfull", previousWorkflowVersion, entity.getBody().getWorkflowVersion());
	}

}
