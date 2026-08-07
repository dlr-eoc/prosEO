/**
 * ProcessorControllerTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.Task;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.ProcessorManagerApplication;
import de.dlr.proseo.procmgr.rest.model.ProcessorUtil;
import de.dlr.proseo.procmgr.rest.model.RestProcessor;
import de.dlr.proseo.procmgr.rest.model.RestTask;

/**
 * Testing ProcessorControllerImpl.class.
 *
 * TODO test invalid REST requests
 *
 * @author Katharina Bassler
 */
@SpringBootTest(classes = ProcessorManagerApplication.class)
@WithMockUser(username = "UTM-testuser", roles = {})
@Transactional
@TestInstance(Lifecycle.PER_CLASS)
public class ProcessorControllerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProcessorControllerTest.class);

	/** The ProcessorControllerImpl under test */
	@Autowired
	private ProcessorControllerImpl pci;

	/** A REST template builder for this class */
	@MockitoBean
	RestTemplateBuilder rtb;

	// Test data
	private static String[] testMissionData =
			// code, name, processing_mode, file_class, product_file_template
			{ "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" };

	private static String[][] testProcessorData = {
			// name, version
			{"KNMI L2", "01.03.02"},
			{"DLR L2 (upas)", "01.01.07",}};

	/** Database transaction manager */
	@Autowired
	private PlatformTransactionManager txManager;

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
		RepositoryService.getOrderRepository().deleteAll();
		RepositoryService.getProductClassRepository().deleteAll();
		RepositoryService.getConfiguredProcessorRepository().deleteAll();
		RepositoryService.getConfigurationRepository().deleteAll();
		RepositoryService.getProcessorRepository().deleteAll();
		RepositoryService.getProcessorClassRepository().deleteAll();
		RepositoryService.getWorkflowRepository().deleteAll();
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
		processorClass0 = RepositoryService.getProcessorClassRepository().save(processorClass0);

		ProcessorClass processorClass1 = new ProcessorClass();
		processorClass1.setMission(testMission);
		processorClass1.setProcessorName(testProcessorData[1][0]);
		processorClass1 = RepositoryService.getProcessorClassRepository().save(processorClass1);

		logger.debug("... adding processors");
		Processor processor0 = new Processor();
		processor0.setProcessorClass(processorClass0);
		processor0.setProcessorVersion(testProcessorData[0][1]);
		processor0.setDockerImage("someDockerImage");
		processor0 = RepositoryService.getProcessorRepository().save(processor0);
		Task task0 = new Task();
		task0.setTaskName("task0");
		task0.setTaskVersion("1");
		task0.setIsCritical(false);
		task0.setProcessor(processor0);
		task0 = RepositoryService.getTaskRepository().save(task0);
		processor0.getTasks().add(task0);
		processor0 = RepositoryService.getProcessorRepository().save(processor0);

		Processor processor1 = new Processor();
		processor1.setProcessorClass(processorClass1);
		processor1.setProcessorVersion(testProcessorData[1][1]);
		processor1.setDockerImage("someOtherDockerImage");
		processor1 = RepositoryService.getProcessorRepository().save(processor1);
		Task task1 = new Task();
		task1.setTaskName("task1");
		task1.setTaskVersion("1");
		task1.setIsCritical(false);
		task1.setProcessor(processor1);
		task1 = RepositoryService.getTaskRepository().save(task1);
		processor1.getTasks().add(task1);
		processor1 = RepositoryService.getProcessorRepository().save(processor1);
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processormgr.rest.ProcessorControllerImpl#createProcessor(de.dlr.proseo.model.rest.model.RestProcessor)}.
	 */
	@Test
	public final void testCreateProcessor() {
		logger.trace(">>> testCreateProcessor()");

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		transactionTemplate.execute(status -> {
			// retrieve and delete the test processor from the database
			RestProcessor toBeCreated = ProcessorUtil.toRestProcessor(RepositoryService.getProcessorRepository().findAll().get(0));
			RepositoryService.getProcessorRepository().deleteById(toBeCreated.getId());

			// testing processor creation with the processor controller
			toBeCreated.setId(null);
			for (RestTask task: toBeCreated.getTasks()) {
				task.setId(null);
			}
			ResponseEntity<RestProcessor> created = pci.createProcessor(toBeCreated);
			assertEquals(HttpStatus.CREATED, created.getStatusCode(), "Wrong HTTP status: ");
			assertEquals(toBeCreated.getProcessorName(), created.getBody().getProcessorName(), "Error during processor creation.");

			return true;
		});
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processormgr.rest.ProcessorControllerImpl#countProcessors(java.lang.String, java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
	 */
	@Test
	public final void testCountProcessors() {
		logger.trace(">>> testCountProcessors()");

		// count all processors from the database, as all were created with the
		// same mission
		List<Processor> expectedProcessors = RepositoryService.getProcessorRepository().findAll();

		// count all processors with the same mission as the test processors
		// from the database via the processor controller
		ResponseEntity<String> retrievedProcessors = pci.countProcessors(testMissionData[0], null, null, null);
		assertEquals(HttpStatus.OK, retrievedProcessors.getStatusCode(), "Wrong HTTP status: ");
		assertTrue(
				Integer.toUnsignedString(expectedProcessors.size()).equals(retrievedProcessors.getBody()), "Wrong number of processors retrieved.");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processormgr.rest.ProcessorControllerImpl#getProcessors(java.lang.String, java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
	 */
	@Test
	public final void testGetProcessors() {
		logger.trace(">>> testGetProcessors()");

		// retrieve all processors from the database, as all were created with the
		// same
		// mission
		List<Processor> expectedProcessors = RepositoryService.getProcessorRepository().findAll();

		// retrieve all processors with the same mission as the test processors
		// from the
		// database via the processor controller
		ResponseEntity<List<RestProcessor>> retrievedProcessors = pci.getProcessors(testMissionData[0],
				null, null, null, null, null, null);
		assertEquals(HttpStatus.OK, retrievedProcessors.getStatusCode(), "Wrong HTTP status: ");
		assertTrue(
				expectedProcessors.size() == retrievedProcessors.getBody().size(), "Wrong number of processors retrieved.");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processormgr.rest.ProcessorControllerImpl#getProcessorById(java.lang.Long)}.
	 */
	@Test
	public final void testGetProcessorById() {
		logger.trace(">>> testGetProcessorById()");

		// retrieve a test processor from the database
		Processor expectedProcessor = RepositoryService.getProcessorRepository().findAll().get(0);

		// retrieve a processor with the processor controller by using the id
		// from the
		// test processor
		ResponseEntity<RestProcessor> retrievedProcessor = pci
				.getProcessorById(expectedProcessor.getId());
		assertEquals(HttpStatus.OK, retrievedProcessor.getStatusCode(), "Wrong HTTP status: ");
		assertTrue(expectedProcessor.getProcessorClass().getProcessorName()
				.equals(retrievedProcessor.getBody().getProcessorName()), "Wrong processor retrieved.");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processormgr.rest.ProcessorControllerImpl#deleteProcessorById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteProcessorById() {
		logger.trace(">>> testDeleteProcessorById()");

		// chose one processor from the database for deletion
		Processor toBeDeleted = RepositoryService.getProcessorRepository().findAll().get(0);

		// remove related configured processor to avoid inconsistencies
		toBeDeleted.getConfiguredProcessors().removeIf(c -> true);

		// delete the chosen processor via the processor controller
		ResponseEntity<?> entity = pci.deleteProcessorById(toBeDeleted.getId());
		assertEquals(HttpStatus.NO_CONTENT, entity.getStatusCode(), "Wrong HTTP status: ");

		// assert that the processor was deleted
		assertTrue(
				RepositoryService.getProcessorRepository().findById(toBeDeleted.getId()).isEmpty(), "Processor not deleted.");
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processormgr.rest.ProcessorControllerImpl#modifyProcessor(java.lang.Long, de.dlr.proseo.model.rest.model.RestProcessor)}.
	 */
	@Test
	public final void testModifyProcessor() {
		logger.trace(">>> testModifyProcessor()");

		logger.trace("    1");
		Processor inRepository = RepositoryService.getProcessorRepository().findAll().get(0);
		logger.trace("    2");
		RestProcessor toBeModified = ProcessorUtil.toRestProcessor(inRepository);
		logger.trace("    3");
		String previousProcessorVersion = toBeModified.getProcessorVersion();
		logger.trace("    4");
		toBeModified.setProcessorVersion("10.1");
		logger.trace("    call modify");
		ResponseEntity<RestProcessor> entity = pci.modifyProcessor(toBeModified.getId(), toBeModified);
		logger.trace("    done modify");
		assertEquals(HttpStatus.OK, entity.getStatusCode(), "Wrong HTTP status: ");
		assertTrue(toBeModified.getVersion() + 1 == entity.getBody().getVersion(), "Modification unsuccessfull");
		assertNotEquals(previousProcessorVersion,
				entity.getBody().getProcessorVersion(), "Modification unsuccessfull");
	}

}
