/**
 * TaskRepositoryTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.Task;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for OrbitRepository
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class TaskRepositoryTest {

	private static final String TEST_VERSION = "$02.00.01$";
	private static final String TEST_NAME = "$KNMI L2$";
	private static final String TEST_TASK_NAME = "$TROPNLL2$";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(TaskRepositoryTest.class);
	
	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test the additional repository methods
	 */
	@Test
	public final void test() {
		ProcessorClass procClass = new ProcessorClass();
		procClass.setProcessorName(TEST_NAME);
		procClass = RepositoryService.getProcessorClassRepository().save(procClass);
		
		Processor proc = new Processor();
		proc.setProcessorClass(procClass);
		proc.setProcessorVersion(TEST_VERSION);
		proc = RepositoryService.getProcessorRepository().save(proc);

		procClass.getProcessors().add(proc);
		RepositoryService.getProcessorClassRepository().save(procClass);
		
		// Test save
		Task task = new Task();
		task.setTaskName(TEST_TASK_NAME);
		task.setProcessor(proc);
		task = RepositoryService.getTaskRepository().save(task);
		assertTrue("Database ID not set for task", 0 != task.getId());
		
		logger.info("OK: Test for save completed");
		
	}

}
