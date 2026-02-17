/**
 * WorkflowRepositoryTest.java
 * 
 * (c) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.Assert.*;

import java.util.List;
import java.util.UUID;

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

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for OrderTemplateRepository
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class WorkflowRepositoryTest {

	private static final String TEST_MISSIONCODE = "xyz567";
	private static final String TEST_NAME = "$OrderTemplate 4711$";
	private static final String TEST_VERSION = "1.0";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(WorkflowRepositoryTest.class);
	
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
		Mission mission = new Mission();
		mission.setCode(TEST_MISSIONCODE);
		mission = RepositoryService.getMissionRepository().save(mission);
		
		Workflow workflow = new Workflow();
		workflow.setMission(mission);
		workflow.setUuid(UUID.randomUUID());
		workflow.setName(TEST_NAME);
		workflow.setWorkflowVersion(TEST_VERSION);
		workflow = RepositoryService.getWorkflowRepository().save(workflow);
		assertNotNull("Workflow save failed", workflow);
		
		logger.info("OK: Test for save completed");
		
		// Test findAll
		List<Workflow> workflows = RepositoryService.getWorkflowRepository().findAll();
		assertEquals("Find all failed for Workflow", 1, workflows.size());
		
		workflows.stream().forEach(wf -> 
			logger.debug("... found workflow {} / {} / {}", wf.getMission().getCode(), wf.getName(), wf.getWorkflowVersion()));
		
		logger.info("OK: Test for findAll completed");
		
		// Test findByMissionCode
		workflows = RepositoryService.getWorkflowRepository().findByMissionCode(TEST_MISSIONCODE);
		assertEquals("Find by mission code failed for Workflow", 1, workflows.size());
		
		logger.info("OK: Test for findByMissionCode completed");
		
		// Test findByMissionCodeAndName
		workflows = RepositoryService.getWorkflowRepository().findByMissionCodeAndName(TEST_MISSIONCODE, TEST_NAME);
		assertEquals("Find by mission code failed for Workflow", 1, workflows.size());
		
		logger.info("OK: Test for findByMissionCodeAndName completed");
		
		// Test findByMissionCodeAndNameAndVersion
		workflow = RepositoryService.getWorkflowRepository()
				.findByMissionCodeAndNameAndVersion(TEST_MISSIONCODE, TEST_NAME, TEST_VERSION);
		assertNotNull("Find by mission code and name failed for Workflow", workflow);
		
		logger.info("OK: Test for findByMissionCodeAndName completed");
		
	}

}
