/**
 * ProcessorClassRepositoryTest.java
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

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for ProcessorClassRepository
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
public class ProcessorClassRepositoryTest {

	private static final String TEST_CODE = "$xyz$";
	private static final String TEST_NAME = "$KNMI L2$";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorClassRepositoryTest.class);
	
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
		Mission mission = new Mission();
		mission.setCode(TEST_CODE);
		mission = RepositoryService.getMissionRepository().save(mission);
		
		ProcessorClass procClass = new ProcessorClass();
		procClass.setMission(mission);
		procClass.setProcessorName(TEST_NAME);
		RepositoryService.getProcessorClassRepository().save(procClass);
		
		// Test findByProcessorName
		procClass = RepositoryService.getProcessorClassRepository().findByMissionCodeAndProcessorName(TEST_CODE, TEST_NAME);
		assertNotNull(procClass, "Find by processor name failed for ProcessorClass");
		
		logger.info("OK: Test for findByProcessorName completed");
		
	}

}
