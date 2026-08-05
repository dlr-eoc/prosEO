/**
 * TaskRepositoryTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
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
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
public class TaskRepositoryTest {

	private static final String TEST_VERSION = "$02.00.01$";
	private static final String TEST_NAME = "$KNMI L2$";
	private static final String TEST_TASK_NAME = "$TROPNLL2$";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(TaskRepositoryTest.class);
	
	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@BeforeEach
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception if an error occurs
	 */
	@AfterEach
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
		assertTrue(0 != task.getId(), "Database ID not set for task");
		
		logger.info("OK: Test for save completed");
		
	}

}
