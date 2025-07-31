/**
 * ProcessorClassControllerTest.java
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
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.ProcessorManagerApplication;
import de.dlr.proseo.procmgr.rest.model.ProcessorClassUtil;
import de.dlr.proseo.procmgr.rest.model.RestProcessorClass;

/**
 * Testing ProcessorClassControllerImpl.class.
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
public class ProcessorClassControllerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProcessorClassControllerTest.class);

	/** The ProcessorClassControllerImpl under test */
	@Autowired
	private ProcessorClassControllerImpl pci;

	/** A REST template builder for this class */
	@MockBean
	RestTemplateBuilder rtb;

	// Test data
	private static String[] testMissionData =
			// code, name, processing_mode, file_class, product_file_template
			{ "UTM", "ABCD Testing", "NRTI", "OPER", "test_file_temp" };
	private static String[] testProcessorClassData =
			// name, version
			{ "KNMI L2", "DLR L2 (upas)" };

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

		logger.debug("... adding processorClass classes");
		ProcessorClass processorClass0 = new ProcessorClass();
		processorClass0.setMission(testMission);
		processorClass0.setProcessorName(testProcessorClassData[0]);
		processorClass0.setId(RepositoryService.getProcessorClassRepository().save(processorClass0).getId());

		ProcessorClass processorClass1 = new ProcessorClass();
		processorClass1.setMission(testMission);
		processorClass1.setProcessorName(testProcessorClassData[1]);
		processorClass1.setId(RepositoryService.getProcessorClassRepository().save(processorClass1).getId());

	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processorClassmgr.rest.ProcessorClassControllerImpl#createProcessorClass(de.dlr.proseo.model.rest.model.RestProcessorClass)}.
	 */
	@Test
	public final void testCreateProcessorClass() {
		logger.trace(">>> testCreateProcessorClass()");

		// retrieve and delete the test processorClass from the database
		RestProcessorClass toBeCreated = ProcessorClassUtil
				.toRestProcessorClass(RepositoryService.getProcessorClassRepository().findAll().get(0));
		RepositoryService.getProcessorClassRepository().deleteById(toBeCreated.getId());

		// testing processorClass creation with the processorClass controller
		ResponseEntity<RestProcessorClass> created = pci.createProcessorClass(toBeCreated);
		assertEquals("Wrong HTTP status: ", HttpStatus.CREATED, created.getStatusCode());
		assertEquals("Error during processorClass creation.", toBeCreated.getProcessorName(),
				created.getBody().getProcessorName());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processorClassmgr.rest.ProcessorClassControllerImpl#countProcessorClasss(java.lang.String, java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
	 */
	@Test
	public final void testCountProcessorClasss() {
		logger.trace(">>> testCountProcessorClasss()");

		// count all processorClasss from the database, as all were created with the
		// same mission
		List<ProcessorClass> expectedProcessorClasss = RepositoryService.getProcessorClassRepository().findAll();

		// count all processorClasss with the same mission as the test processorClasss
		// from the database via the processorClass controller
		ResponseEntity<String> retrievedProcessorClasss = pci.countProcessorClasses(testMissionData[0], null, null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedProcessorClasss.getStatusCode());
		assertTrue("Wrong number of processorClasss retrieved.",
				Integer.toUnsignedString(expectedProcessorClasss.size()).equals(retrievedProcessorClasss.getBody()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processorClassmgr.rest.ProcessorClassControllerImpl#getProcessorClasss(java.lang.String, java.lang.String, java.lang.String[], java.util.Date, java.util.Date, java.util.Date, java.util.Date)}.
	 */
	@Test
	public final void testGetProcessorClasss() {
		logger.trace(">>> testGetProcessorClasss()");

		// retrieve all processorClasss from the database, as all were created with the
		// same
		// mission
		List<ProcessorClass> expectedProcessorClasss = RepositoryService.getProcessorClassRepository().findAll();

		// retrieve all processorClasss with the same mission as the test
		// processorClasss
		// from the
		// database via the processorClass controller
		ResponseEntity<List<RestProcessorClass>> retrievedProcessorClasss = pci.getProcessorClasses(testMissionData[0],
				null, null, null, null, null, null);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedProcessorClasss.getStatusCode());
		assertTrue("Wrong number of processorClasss retrieved.",
				expectedProcessorClasss.size() == retrievedProcessorClasss.getBody().size());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processorClassmgr.rest.ProcessorClassControllerImpl#getProcessorClassById(java.lang.Long)}.
	 */
	@Test
	public final void testGetProcessorClassById() {
		logger.trace(">>> testGetProcessorClassById()");

		// retrieve a test processorClass from the database
		ProcessorClass expectedProcessorClass = RepositoryService.getProcessorClassRepository().findAll().get(0);

		// retrieve a processorClass with the processorClass controller by using the id
		// from the
		// test processorClass
		ResponseEntity<RestProcessorClass> retrievedProcessorClass = pci
				.getProcessorClassById(expectedProcessorClass.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, retrievedProcessorClass.getStatusCode());
		assertTrue("Wrong processorClass retrieved.",
				expectedProcessorClass.getProcessorName().equals(retrievedProcessorClass.getBody().getProcessorName()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processorClassmgr.rest.ProcessorClassControllerImpl#deleteProcessorClassById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteProcessorClassById() {
		logger.trace(">>> testDeleteProcessorClassById()");

		// chose one processor class from the database for deletion
		ProcessorClass toBeDeleted = RepositoryService.getProcessorClassRepository().findAll().get(0);

		// delete the chosen processor class via the processor class controller
		ResponseEntity<?> entity = pci.deleteProcessorClassById(toBeDeleted.getId());
		assertEquals("Wrong HTTP status: ", HttpStatus.NO_CONTENT, entity.getStatusCode());

		// assert that the processor class was deleted
		assertTrue("ProcessorClass not deleted.",
				RepositoryService.getProcessorClassRepository().findById(toBeDeleted.getId()).isEmpty());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.processorClassmgr.rest.ProcessorClassControllerImpl#modifyProcessorClass(java.lang.Long, de.dlr.proseo.model.rest.model.RestProcessorClass)}.
	 */
	@Test
	public final void testModifyProcessorClass() {
		logger.trace(">>> testModifyProcessorClass()");

		ProcessorClass inRepository = RepositoryService.getProcessorClassRepository().findAll().get(0);
		RestProcessorClass toBeModified = ProcessorClassUtil.toRestProcessorClass(inRepository);
		String oldName = toBeModified.getProcessorName();
		toBeModified.setProcessorName("newName");

		ResponseEntity<RestProcessorClass> entity = pci.modifyProcessorClass(toBeModified.getId(), toBeModified);
		assertEquals("Wrong HTTP status: ", HttpStatus.OK, entity.getStatusCode());
		assertTrue("Modification unsuccessfull", toBeModified.getVersion() + 1 == entity.getBody().getVersion());
		assertNotEquals("Modification unsuccessfull", oldName, entity.getBody().getProcessorName());
	}

}
