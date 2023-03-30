/**
 * WorkflowMgrTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.WorkflowOption;
import de.dlr.proseo.model.WorkflowOption.WorkflowOptionType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.ProcessorManagerApplication;
import de.dlr.proseo.procmgr.rest.model.RestWorkflow;
import de.dlr.proseo.procmgr.rest.model.RestWorkflowOption;
import de.dlr.proseo.procmgr.rest.model.WorkflowUtil;

/**
 * Testing the service methods required to create, modify and delete workflows
 * in the prosEO database, and to query the database about such workflows
 *
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProcessorManagerApplication.class)
@WithMockUser(username = "UTM-testuser", roles = {})
@AutoConfigureTestEntityManager
@Transactional
public class WorkflowMgrTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(WorkflowMgrTest.class);

	/** The workflow manager under test */
	@Autowired
	WorkflowMgr workflowMgr;

	/** A REST template builder for this class */
	@MockBean
	RestTemplateBuilder rtb;

	// Test data
	private static String[] testMissionData =
			// code, name, processing_mode, file_class, product_file_template
			{ "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" };
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
	 * Create a test mission, a test spacecraft and test orders in the database.
	 *
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		logger.trace(">>> Starting to create test data in the database");

		fillDatabase();

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
	 * Filling the database with some initial data for testing purposes
	 *
	 * @param mission the mission to be referenced by the data filled in the
	 *                database
	 */
	private static void fillDatabase() {
		logger.trace("... creating testMission {}", testMissionData[0]);
		Mission testMission = new Mission();
		testMission.setCode(testMissionData[0]);
		testMission.setName(testMissionData[1]);
		testMission.getProcessingModes().add(testMissionData[2]);
		testMission.getFileClasses().add(testMissionData[3]);
		testMission.setProductFileTemplate(testMissionData[4]);
		testMission.setId(RepositoryService.getMissionRepository().save(testMission).getId());
		
		logger.debug("... adding input product classes");
		ProductClass productClass0 = new ProductClass();
		productClass0.setProductType(testWorkflowData[0][3]);
		productClass0.setMission(testMission);
		RepositoryService.getProductClassRepository().save(productClass0);

		ProductClass productClass1 = new ProductClass();
		productClass1.setProductType(testWorkflowData[1][3]);
		productClass1.setMission(testMission);
		RepositoryService.getProductClassRepository().save(productClass1);

		logger.debug("... adding a processor class and output product classes");
		ProcessorClass processorClass = new ProcessorClass();
		processorClass.setMission(testMission);
		processorClass.setProcessorName("randomName");
		processorClass.setId(RepositoryService.getProcessorClassRepository().save(processorClass).getId());

		ProductClass productClass2 = new ProductClass();
		productClass2.setProductType(testWorkflowData[0][4]);
		productClass2.setMission(testMission);
		productClass2.setProcessorClass(processorClass);
		productClass2.setId(RepositoryService.getProductClassRepository().save(productClass2).getId());

		ProductClass productClass3 = new ProductClass();
		productClass3.setProductType(testWorkflowData[1][4]);
		productClass3.setMission(testMission);
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
	 * {@link de.dlr.proseo.procmgr.rest.WorkflowMgr#countWorkflows(de.dlr.proseo.procmgr.rest.model.RestWorkflow)}.
	 */
	@Test
	public final void testCountWorkflows() {
		logger.trace(">>> testCountWorkflows()");
		
		// Count workflows and assert success.
		assertEquals("Wrong workflow count.", "2",
				workflowMgr.countWorkflows("UTM", null, null, null, null));
		assertEquals("Wrong workflow count.", "1",
				workflowMgr.countWorkflows("UTM", testWorkflowData[0][0], null, null, null));
		assertEquals("Wrong workflow count.", "1",
				workflowMgr.countWorkflows("UTM", null, testWorkflowData[0][2], null, null));
		assertEquals("Wrong workflow count.", "1",
				workflowMgr.countWorkflows("UTM", null, null, testWorkflowData[0][4], null));
		assertEquals("Wrong workflow count.", "1",
				workflowMgr.countWorkflows("UTM", null, null, null, testWorkflowData[0][5]));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.WorkflowMgr#createWorkflow(de.dlr.proseo.procmgr.rest.model.RestWorkflow)}.
	 */
	@Test
	public final void testCreateWorkflow() {
		logger.trace(">>> testCreateWorkflow()");

		// Get a valid sample workflow and workflow option from which deviations can be
		// tested.
		RestWorkflow testWorkflow = WorkflowUtil
				.toRestWorkflow(RepositoryService.getWorkflowRepository().findByName(testWorkflowData[0][0]));
		RestWorkflowOption testWorkflowOption = testWorkflow.getWorkflowOptions().get(0);

		logger.trace("testWorkflow is " + testWorkflow);
		logger.trace("testWorkflowOption is " + testWorkflowOption);

		RepositoryService.getWorkflowOptionRepository().deleteById(testWorkflowOption.getId());
		RepositoryService.getWorkflowRepository().deleteById(testWorkflow.getId());

		// The RestWorkflow parameter must not be null.
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(null));

		// It is not allowed to create a workflow without a mission.
		testWorkflow.setMissionCode(null);
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflow.setMissionCode(testMissionData[0]);

		// The user is only allowed to create workflows for the mission they are logged
		// into (here UTM).
		testWorkflow.setMissionCode("PTM");
		assertThrows(SecurityException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflow.setMissionCode(testMissionData[0]);

		// No two workflows can have the same name and workflow version.
		testWorkflow.setName(testWorkflowData[1][0]);
		testWorkflow.setWorkflowVersion(testWorkflowData[1][2]);
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflow.setName(testWorkflowData[0][0]);
		testWorkflow.setWorkflowVersion(testWorkflowData[0][2]);

		// No two workflows can have the same UUID.
		testWorkflow.setUuid(
				RepositoryService.getWorkflowRepository().findByName(testWorkflowData[1][0]).getUuid().toString());
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflow.setUuid(UUID.randomUUID().toString());

		// The workflowVersion must be provided.
		testWorkflow.setWorkflowVersion(null);
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflow.setWorkflowVersion(testWorkflowData[0][2]);

		// A valid inputProductClass must be provided
		testWorkflow.setInputProductClass(null);
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflow.setInputProductClass(RepositoryService.getProductClassRepository()
				.findByMissionCodeAndProductType(testMissionData[0], testWorkflowData[0][3]).getProductType());

		// A valid outputProductClass must be provided, i.e., the processor class must
		// be able to generate it.
		testWorkflow.setOutputProductClass(null);
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflow.setOutputProductClass(RepositoryService.getProductClassRepository()
				.findByMissionCodeAndProductType(testMissionData[0], testWorkflowData[0][3]).getProductType());
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflow.setOutputProductClass(RepositoryService.getProductClassRepository()
				.findByMissionCodeAndProductType(testMissionData[0], testWorkflowData[0][4]).getProductType());

		// A valid configuredProcessor must be provided.
		testWorkflow.setConfiguredProcessor(null);
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflow.setConfiguredProcessor(testWorkflowData[0][5]);

		// If provided, workflowOptions must be valid.

		// The missionCode must match both the respective workflow's
		// mission code and the mission to which the user is logged in.
		testWorkflowOption.setMissionCode("PTM");
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflowOption.setMissionCode(testMissionData[0]);

		// A valid workflowName must be provided, matching the workflow to which the
		// option is assigned.
		testWorkflowOption.setWorkflowName(null);
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));

		testWorkflowOption.setWorkflowName("invalid");
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflowOption.setWorkflowName(testWorkflow.getName());

		// The workflow option must have a name.
		testWorkflowOption.setName(null);
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflowOption.setName("optionName");

		// A valid optionType must be provided.
		testWorkflowOption.setOptionType(null);
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));

		testWorkflowOption.setOptionType("invalid");
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflowOption.setOptionType(WorkflowOptionType.NUMBER.toString());

		// A valueRange must be provided.
		testWorkflowOption.setValueRange(null);
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		// Empty value ranges are allowed, so a non-existing value range is quietly corrected
//		assertThrows(IllegalArgumentException.class, () -> workflowMgr.createWorkflow(testWorkflow));
		testWorkflowOption.setValueRange(new ArrayList<String>());
		testWorkflowOption.getValueRange().add("someValue");
		
		// No exception is thrown for correct input.
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		workflowMgr.createWorkflow(testWorkflow);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.WorkflowMgr#deleteWorkflowById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteWorkflowById() {
		logger.trace(">>> testDeleteWorkflowById()");

		// Get a test workflow to delete.
		RestWorkflow testWorkflow = WorkflowUtil
				.toRestWorkflow(RepositoryService.getWorkflowRepository().findAll().get(0));

		// Delete workflow and assert success.
		workflowMgr.deleteWorkflowById(testWorkflow.getId());
		assertTrue("The workflow was not deleted.",
				RepositoryService.getWorkflowRepository().findById(testWorkflow.getId()).isEmpty());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.WorkflowMgr#getWorkflowById(java.lang.Long)}.
	 */
	@Test
	public final void testGetWorkflowById() {
		logger.trace(">>> testGetWorkflowById()");

		// Get a test workflow to retrieve.
		RestWorkflow testWorkflow = WorkflowUtil
				.toRestWorkflow(RepositoryService.getWorkflowRepository().findAll().get(0));

		// Retrieve workflow and assert success.
		assertNotNull("No workflow was retrieved.", workflowMgr.getWorkflowById(testWorkflow.getId()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.WorkflowMgr#modifyWorkflow(java.lang.Long, de.dlr.proseo.procmgr.rest.model.RestWorkflow)}.
	 */
	@Test
	public final void testModifyWorkflow() {
		logger.trace(">>> testModifyWorkflow()");

		// Get a valid sample workflow and workflow option to modify.
		RestWorkflow originalWorkflow = WorkflowUtil
				.toRestWorkflow(RepositoryService.getWorkflowRepository().findAll().get(0));
		RestWorkflow testWorkflow = WorkflowUtil
				.toRestWorkflow(RepositoryService.getWorkflowRepository().findById(originalWorkflow.getId()).get());
		RestWorkflowOption testWorkflowOption = testWorkflow.getWorkflowOptions().get(0);

		logger.trace("testWorkflow is " + testWorkflow);
		logger.trace("testWorkflowOption is " + testWorkflowOption);

		// The function parameters must not be null.
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.modifyWorkflow(null, null));
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.modifyWorkflow(testWorkflow.getId(), null));
		assertThrows(IllegalArgumentException.class, () -> workflowMgr.modifyWorkflow(null, testWorkflow));

		// Version is updated automatically and may not be set manually. The workflow
		// version may be changed.
		testWorkflow.setVersion(1111L);
		assertThrows(ConcurrentModificationException.class,
				() -> workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow));
		testWorkflow.setVersion(originalWorkflow.getVersion());
		testWorkflow.setWorkflowVersion("someOtherVersion");
		assertTrue("Version was not incremented.", workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow)
				.getVersion() == 1 + originalWorkflow.getVersion());
		testWorkflow.setVersion(testWorkflow.getVersion() + 1);

		//TODO Mandatory fields may not be null
		
		// If changed, configured processor must be valid.
		testWorkflow.setConfiguredProcessor("invalid");
		assertThrows(IllegalArgumentException.class,
				() -> workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow));
		testWorkflow.setConfiguredProcessor(testWorkflowData[0][5]);

		// If changed, the new input product class must be valid.
		testWorkflow.setInputProductClass("invalid");
		assertThrows(IllegalArgumentException.class,
				() -> workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow));
		testWorkflow.setInputProductClass(testWorkflowData[0][3]);

		// If changed, the new output product class must be valid.
		testWorkflow.setOutputProductClass("invalid");
		assertThrows(IllegalArgumentException.class,
				() -> workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow));
		// ... and match the configured processor. (Here, an input product class is
		// used,
		// which the specified processor cannot produce.)
		testWorkflow.setOutputProductClass(testWorkflowData[0][3]);
		assertThrows(IllegalArgumentException.class,
				() -> workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow));
		testWorkflow.setOutputProductClass(testWorkflowData[0][4]);

		// Deleting workflowOptions is allowed.
		testWorkflow.getWorkflowOptions().clear();
		workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow);
		testWorkflow.setVersion(testWorkflow.getVersion() + 1);

		// Added workflow options must be valid, cf. above:

		// The workflow option must belong to the workflow by name.
		testWorkflowOption.setWorkflowName("wrongName");
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		assertThrows(IllegalArgumentException.class,
				() -> workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow));
		testWorkflowOption.setWorkflowName(testWorkflow.getName());

		// The missionCode, if provided, must match both the respective workflow's
		// mission code and the mission to which the user is logged in.
		testWorkflowOption.setMissionCode("PTM");
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		assertThrows(IllegalArgumentException.class,
				() -> workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow));
		testWorkflowOption.setMissionCode(testMissionData[0]);

		// A valid workflowName must be provided.
		testWorkflowOption.setWorkflowName(null);
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		assertThrows(IllegalArgumentException.class,
				() -> workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow));

		testWorkflowOption.setWorkflowName("invalid");
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		assertThrows(IllegalArgumentException.class,
				() -> workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow));
		testWorkflowOption.setWorkflowName(testWorkflow.getName());

		// The workflow option must have a name.
		testWorkflowOption.setName(null);
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		assertThrows(IllegalArgumentException.class,
				() -> workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow));
		testWorkflowOption.setName("optionName");

		// A valid optionType must be provided.
//		testWorkflowOption.setOptionType(null);
//		testWorkflow.getWorkflowOptions().clear();
//		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
//		assertThrows(IllegalArgumentException.class,
//				() -> workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow));

		testWorkflowOption.setOptionType("invalid");
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);
		assertThrows(IllegalArgumentException.class,
				() -> workflowMgr.modifyWorkflow(testWorkflow.getId(), testWorkflow));
		testWorkflowOption.setOptionType(WorkflowOptionType.NUMBER.toString());

		// Empty value ranges are allowed, so a non-existing value range is quietly corrected
		testWorkflowOption.setValueRange(null);
		testWorkflow.getWorkflowOptions().clear();
		testWorkflow.getWorkflowOptions().add(testWorkflowOption);

		// No exception is thrown if nothing was modified.
		workflowMgr.modifyWorkflow(testWorkflow.getId(), WorkflowUtil
				.toRestWorkflow(RepositoryService.getWorkflowRepository().findById(originalWorkflow.getId()).get()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.WorkflowMgr#getWorkflows(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testGetWorkflows() {
		logger.trace(">>> testGetWorkflows()");

		/*
		 * Using values from the test workflow data which was used to initialize the
		 * workflow repository means that the query must return at least one workflow.
		 * If no mission was specified, it is acquired from the security service. Not
		 * specifying additional parameters returns all workflows for the given mission.
		 */
		assertTrue("More or less workflows retrieved than expected.",
				workflowMgr.getWorkflows(null, null, null, null, null, 0, 10).size() == 2);
		assertTrue("More or less workflows retrieved than expected.",
				workflowMgr.getWorkflows(testMissionData[0], null, null, null, null, 0, 100).size() == 2);
		assertTrue("More or less workflows retrieved than expected.",
				workflowMgr.getWorkflows(testMissionData[0], testWorkflowData[0][0], null, null, null, null, null).size() == 1);
		assertTrue("More or less workflows retrieved than expected.",
				workflowMgr.getWorkflows(testMissionData[0], null, testWorkflowData[0][2], null, null, null, null).size() == 1);
		assertTrue("More or less workflows retrieved than expected.",
				workflowMgr.getWorkflows(testMissionData[0], null, null, testWorkflowData[0][4], null, null, null).size() == 1);
		assertTrue("More or less workflows retrieved than expected.",
				workflowMgr.getWorkflows(testMissionData[0], null, null, null, testWorkflowData[0][5], null, null).size() == 1);
	}

}