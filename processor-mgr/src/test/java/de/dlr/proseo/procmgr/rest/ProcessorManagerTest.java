/**
 * ProcessorMgrTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.ProcessorManagerApplication;
import de.dlr.proseo.procmgr.rest.model.ProcessorUtil;
import de.dlr.proseo.procmgr.rest.model.RestProcessor;

/**
 * Testing the service methods required to create, modify and delete processors
 * in the prosEO database, and to query the database about such processors
 *
 * @author Katharina Bassler
 */
@SpringBootTest(classes = ProcessorManagerApplication.class)
@WithMockUser(username = "UTM-testuser", roles = {})
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
public class ProcessorManagerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProcessorManagerTest.class);

	/** The processor manager under test */
	@Autowired
	ProcessorManager processorMgr;

	/** A REST template builder for this class */
	@MockitoBean
	RestTemplateBuilder rtb;

	// Test data
	private static String[] testMissionData =
			// code, name, processing_mode, file_class, product_file_template
			{ "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" };

	private static String[][] testProcessorData = {
			// name, version
			{ "KNMI L2", "01.03.02" }, { "DLR L2 (upas)", "01.01.07", } };

	/**
	 *
	 * Create a test mission, a test spacecraft and test orders in the database.
	 *
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	public void setUp() throws Exception {
		logger.trace(">>> Starting to create test data in the database");

		fillDatabase();

		logger.trace("<<< Finished creating test data in database");
	}

	/**
	 *
	 * Deleting test data from the database
	 *
	 * @throws java.lang.Exception
	 */
	@AfterAll
	public void tearDown() throws Exception {
		logger.debug(">>> Starting to delete test data in database");
		RepositoryService.getWorkflowRepository().deleteAll();
		RepositoryService.getOrderRepository().deleteAll();
		RepositoryService.getProductClassRepository().deleteAll();
		RepositoryService.getConfiguredProcessorRepository().deleteAll();
		RepositoryService.getConfigurationRepository().deleteAll();
		RepositoryService.getProcessorRepository().deleteAll();
		RepositoryService.getProcessorClassRepository().deleteAll();
		RepositoryService.getSpacecraftRepository().deleteAll();
		RepositoryService.getMissionRepository().deleteAll();
		logger.debug("<<< Finished deleting test data in database");
	}

	/**
	 * Filling the database with some initial data for testing purposes
	 *
	 * @param mission the mission to be referenced by the data filled in the
	 *                database
	 */
	private static void fillDatabase() {
		Mission testMission = new Mission();

		logger.trace("... creating mission {}", testMissionData[0]);

		// adding mission attributes
		testMission.setCode(testMissionData[0]);
		testMission.setName(testMissionData[1]);
		testMission.getProcessingModes().add(testMissionData[2]);
		testMission.getFileClasses().add(testMissionData[3]);
		testMission.setProductFileTemplate(testMissionData[4]);

		// saving mission in the database
		testMission = RepositoryService.getMissionRepository().save(testMission);

		logger.debug("... adding processor classes");
		ProcessorClass processorClass0 = new ProcessorClass();
		processorClass0.setMission(testMission);
		processorClass0.setProcessorName(testProcessorData[0][0]);
		processorClass0.setId(RepositoryService.getProcessorClassRepository().save(processorClass0).getId());

		ProcessorClass processorClass1 = new ProcessorClass();
		processorClass1.setMission(testMission);
		processorClass1.setProcessorName(testProcessorData[1][0]);
		processorClass1.setId(RepositoryService.getProcessorClassRepository().save(processorClass1).getId());

		logger.debug("... adding processors");
		Processor processor0 = new Processor();
		processor0.setProcessorClass(processorClass0);
		processor0.setProcessorVersion(testProcessorData[0][1]);
		processor0.setDockerImage("someDockerImage");
		processor0.setId(RepositoryService.getProcessorRepository().save(processor0).getId());

		Processor processor1 = new Processor();
		processor1.setProcessorClass(processorClass1);
		processor1.setProcessorVersion(testProcessorData[1][1]);
		processor1.setDockerImage("someOtherDockerImage");
		processor1.setId(RepositoryService.getProcessorRepository().save(processor1).getId());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ProcessorMgr#countProcessors(de.dlr.proseo.procmgr.rest.model.RestProcessor)}.
	 */
	@Test
	public final void testCountProcessors() {
		logger.trace(">>> testCountProcessors()");

		// Count processors and assert success.
		assertEquals("2", processorMgr.countProcessors(null, "UTM", null, null), "Wrong processor count.");
		assertEquals("1", processorMgr.countProcessors(null, "UTM", testProcessorData[0][0], null), "Wrong processor count.");
		assertEquals("1", processorMgr.countProcessors(null, "UTM", null, testProcessorData[0][1]), "Wrong processor count.");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ProcessorMgr#createProcessor(de.dlr.proseo.procmgr.rest.model.RestProcessor)}.
	 */
	@Test
	public final void testCreateProcessor() {
		logger.trace(">>> testCreateProcessor()");
		// TODO implement method
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ProcessorMgr#deleteProcessorById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteProcessorById() {
		logger.trace(">>> testDeleteProcessorById()");

		// Get a test processor to delete.
		Processor testProcessor = RepositoryService.getProcessorRepository().findAll().get(0);

		// Remove related configured processor to avoid inconsistencies
		testProcessor.getConfiguredProcessors().removeIf(cp -> true);

		// Delete processor and assert success.
		RestProcessor restProcessor = ProcessorUtil.toRestProcessor(testProcessor);
		processorMgr.deleteProcessorById(restProcessor.getId());
		assertTrue(
				RepositoryService.getProcessorRepository().findById(restProcessor.getId()).isEmpty(), "The processor was not deleted.");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ProcessorMgr#getProcessorById(java.lang.Long)}.
	 */
	@Test
	public final void testGetProcessorById() {
		logger.trace(">>> testGetProcessorById()");

		// Get a test processor to retrieve.
		RestProcessor testProcessor = ProcessorUtil
				.toRestProcessor(RepositoryService.getProcessorRepository().findAll().get(0));

		// Retrieve processor and assert success.
		assertNotNull(processorMgr.getProcessorById(testProcessor.getId()), "No processor was retrieved.");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ProcessorMgr#modifyProcessor(java.lang.Long, de.dlr.proseo.procmgr.rest.model.RestProcessor)}.
	 */
	@Test
	public final void testModifyProcessor() {
		logger.trace(">>> testModifyProcessor()");
		// TODO implement method
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ProcessorMgr#getProcessors(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testGetProcessors() {
		logger.trace(">>> testGetProcessors()");

		/*
		 * Using values from the test processor data which was used to initialize the
		 * processor repository means that the query must return at least one processor.
		 * If no mission was specified, it is acquired from the security service. Not
		 * specifying additional parameters returns all processors for the given
		 * mission.
		 */
		assertTrue(
				processorMgr.getProcessors(null, null, null, null, 0, 10, null).size() == 2, "More or less processors retrieved than expected.");
		assertTrue(
				processorMgr.getProcessors(null, testMissionData[0], null, null, 0, 100, null).size() == 2, "More or less processors retrieved than expected.");
		assertTrue(
				processorMgr.getProcessors(null, testMissionData[0], testProcessorData[0][0], null, null, null, null).size() == 1, "More or less processors retrieved than expected.");
		assertTrue(
				processorMgr.getProcessors(null, testMissionData[0], null, testProcessorData[0][1], null, null, null).size() == 1, "More or less processors retrieved than expected.");
	}

}
