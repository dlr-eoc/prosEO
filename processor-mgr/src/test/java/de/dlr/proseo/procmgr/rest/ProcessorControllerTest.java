/**
 * ProcessorControllerTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

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
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.ProcessorManagerApplication;
import de.dlr.proseo.procmgr.rest.model.ProcessorUtil;
import de.dlr.proseo.procmgr.rest.model.RestProcessor;

/**
 * Testing ProcessorControllerImpl.class.
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
public class ProcessorControllerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProcessorControllerTest.class);

	/** The ProcessorControllerImpl under test */
	@Autowired
	private ProcessorControllerImpl pci;

	/** A REST template builder for this class */
	@MockBean
	RestTemplateBuilder rtb;

	// Test data
	private static String[] testMissionData =
			// code, name, processing_mode, file_class, product_file_template
			{ "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" };

	private static String[][] testProcessorData = {
			// name, version
			{"KNMI L2", "01.03.02"},
			{"DLR L2 (upas)", "01.01.07",}};

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
		RepositoryService.getProcessorRepository().deleteAll();
		RepositoryService.getProcessorClassRepository().deleteAll();
		RepositoryService.getMissionRepository().deleteAll();
		logger.trace("<<< Finished deleting test data in database");
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
	 * {@link de.dlr.proseo.processormgr.rest.ProcessorControllerImpl#createProcessor(de.dlr.proseo.model.rest.model.RestProcessor)}.
	 */
	@Test
	public final void testCreateProcessor() {
		logger.trace(">>> testCreateProcessor()");

		// retrieve and delete the test processor from the database
		RestProcessor toBeCreated = ProcessorUtil
				.toRestProcessor(RepositoryService.getProcessorRepository().findAll().get(0));
		RepositoryService.getProcessorRepository().deleteById(toBeCreated.getId());

		// testing processor creation with the processor controller
		ResponseEntity<RestProcessor> created = pci.createProcessor(toBeCreated);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, created.getStatusCode());
		assertEquals("Error during processor creation.", toBeCreated.getProcessorName(),
				created.getBody().getProcessorName());
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
		ResponseEntity<String> retrievedProcessors = pci.countProcessors(testMissionData[0], null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedProcessors.getStatusCode());
		assertTrue("Wrong number of processors retrieved.",
				Integer.toUnsignedString(expectedProcessors.size()).equals(retrievedProcessors.getBody()));
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
				null, null, null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedProcessors.getStatusCode());
		assertTrue("Wrong number of processors retrieved.",
				expectedProcessors.size() == retrievedProcessors.getBody().size());
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
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedProcessor.getStatusCode());
		assertTrue("Wrong processor retrieved.", expectedProcessor.getProcessorClass().getProcessorName()
				.equals(retrievedProcessor.getBody().getProcessorName()));
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
		assertEquals("Wrong HTTP status: ", HttpStatus.NO_CONTENT, entity.getStatusCode());

		// assert that the processor was deleted
		assertTrue("Processor not deleted.",
				RepositoryService.getProcessorRepository().findById(toBeDeleted.getId()).isEmpty());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processormgr.rest.ProcessorControllerImpl#modifyProcessor(java.lang.Long, de.dlr.proseo.model.rest.model.RestProcessor)}.
	 */
	@Test
	public final void testModifyProcessor() {
		logger.trace(">>> testModifyProcessor()");

		Processor inRepository = RepositoryService.getProcessorRepository().findAll().get(0);
		RestProcessor toBeModified = ProcessorUtil.toRestProcessor(inRepository);
		String previousProcessorVersion = toBeModified.getProcessorVersion();
		toBeModified.setProcessorVersion("10.1");

		ResponseEntity<RestProcessor> entity = pci.modifyProcessor(toBeModified.getId(), toBeModified);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());
		assertTrue("Modification unsuccessfull", toBeModified.getVersion() + 1 == entity.getBody().getVersion());
		assertNotEquals("Modification unsuccessfull", previousProcessorVersion,
				entity.getBody().getProcessorVersion());
	}

}
