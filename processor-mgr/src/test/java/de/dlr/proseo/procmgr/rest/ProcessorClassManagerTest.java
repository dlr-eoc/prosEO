/**
 * ProcessorClassMgrTest.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.procmgr.ProcessorManagerApplication;
import de.dlr.proseo.procmgr.rest.model.ProcessorClassUtil;
import de.dlr.proseo.procmgr.rest.model.RestProcessorClass;

/**
 * Testing the service methods required to create, modify and delete processors
 * in the prosEO database, and to query the database about such processors
 *
 * @author Katharina Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProcessorManagerApplication.class)
@WithMockUser(username = "UTM-testuser", roles = {})
@AutoConfigureTestEntityManager
@Transactional
public class ProcessorClassManagerTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProcessorClassManagerTest.class);

	/** The processor manager under test */
	@Autowired
	ProcessorClassManager processorMgr;

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
	 * {@link de.dlr.proseo.procmgr.rest.ProcessorClassMgr#countProcessorClasses(de.dlr.proseo.procmgr.rest.model.RestProcessorClass)}.
	 */
	@Test
	public final void testCountProcessorClasses() {
		logger.trace(">>> testCountProcessorClasses()");

		// Count processors and assert success.
		assertEquals("Wrong processor count.", "2", processorMgr.countProcessorClasses("UTM", null));
		assertEquals("Wrong processor count.", "1",
				processorMgr.countProcessorClasses("UTM", testProcessorClassData[0]));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ProcessorClassMgr#createProcessorClass(de.dlr.proseo.procmgr.rest.model.RestProcessorClass)}.
	 */
	@Test
	public final void testCreateProcessorClass() {
		logger.trace(">>> testCreateProcessorClass()");
		// TODO implement method
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ProcessorClassMgr#deleteProcessorClassById(java.lang.Long)}.
	 */
	@Test
	public final void testDeleteProcessorClassById() {
		logger.trace(">>> testDeleteProcessorClassById()");

		// Get a test processor to delete.
		ProcessorClass testProcessorClass = RepositoryService.getProcessorClassRepository().findAll().get(0);

		// Delete processor and assert success.
		RestProcessorClass restProcessorClass = ProcessorClassUtil.toRestProcessorClass(testProcessorClass);
		processorMgr.deleteProcessorClassById(restProcessorClass.getId());
		assertTrue("The processor was not deleted.",
				RepositoryService.getProcessorClassRepository().findById(restProcessorClass.getId()).isEmpty());
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ProcessorClassMgr#getProcessorClassById(java.lang.Long)}.
	 */
	@Test
	public final void testGetProcessorClassById() {
		logger.trace(">>> testGetProcessorClassById()");

		// Get a test processor to retrieve.
		RestProcessorClass testProcessorClass = ProcessorClassUtil
				.toRestProcessorClass(RepositoryService.getProcessorClassRepository().findAll().get(0));

		// Retrieve processor and assert success.
		assertNotNull("No processor was retrieved.", processorMgr.getProcessorClassById(testProcessorClass.getId()));
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ProcessorClassMgr#modifyProcessorClass(java.lang.Long, de.dlr.proseo.procmgr.rest.model.RestProcessorClass)}.
	 */
	@Test
	public final void testModifyProcessorClass() {
		logger.trace(">>> testModifyProcessorClass()");
		// TODO implement method
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.ProcessorClassMgr#getProcessorClasses(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testGetProcessorClasses() {
		logger.trace(">>> testGetProcessorClasses()");

		/*
		 * Using values from the test processor data which was used to initialize the
		 * processor repository means that the query must return at least one processor.
		 * If no mission was specified, it is acquired from the security service. Not
		 * specifying additional parameters returns all processors for the given
		 * mission.
		 */
		assertTrue("More or less processors retrieved than expected.",
				processorMgr.getProcessorClasses(null, null, 0, 10).size() == 2);
		assertTrue("More or less processors retrieved than expected.",
				processorMgr.getProcessorClasses(testMissionData[0], null, 0, 100).size() == 2);
		assertTrue("More or less processors retrieved than expected.", processorMgr
				.getProcessorClasses(testMissionData[0], testProcessorClassData[0], null, null).size() == 1);
	}

}