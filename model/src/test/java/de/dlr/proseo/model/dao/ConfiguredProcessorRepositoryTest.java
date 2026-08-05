/**
 * ConfiguredProcessorRepositoryTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

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

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for ConfiguredProcessorRepository
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
public class ConfiguredProcessorRepositoryTest {

	private static final String TEST_PROCESSOR_CLASS = "myproc";
	private static final String TEST_MISSIONCODE = "ABC123";
	private static final String TEST_IDENTIFIER = "myConfProc";
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ConfiguredProcessorRepositoryTest.class);
	
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
		mission.setCode(TEST_MISSIONCODE);
		mission = RepositoryService.getMissionRepository().save(mission);
		
		ProcessorClass pc = new ProcessorClass();
		pc.setMission(mission);
		pc.setProcessorName(TEST_PROCESSOR_CLASS);
		pc = RepositoryService.getProcessorClassRepository().save(pc);
		mission.getProcessorClasses().add(pc);
		
		Processor p = new Processor();
		p.setProcessorClass(pc);
		p.setProcessorVersion("1.0");
		p = RepositoryService.getProcessorRepository().save(p);
		pc.getProcessors().add(p);
		
		ConfiguredProcessor confProc = new ConfiguredProcessor();
		confProc.setIdentifier(TEST_IDENTIFIER);
		confProc.setUuid(UUID.randomUUID());
		logger.debug("Created UUID " + confProc.getUuid());
		confProc.setProcessor(p);
		confProc = RepositoryService.getConfiguredProcessorRepository().save(confProc);
		p.getConfiguredProcessors().add(confProc);
		
		// Test findByIdentifier
		confProc = RepositoryService.getConfiguredProcessorRepository().findByMissionCodeAndIdentifier(TEST_MISSIONCODE, TEST_IDENTIFIER);
		assertNotNull(confProc, "Find by identifier failed for ConfiguredProcessor");
		
		logger.info("OK: Test for findByIdentifier completed");
		
		// Test findAll
		List<ConfiguredProcessor> listOfConfProcs = RepositoryService.getConfiguredProcessorRepository().findAll();
		assertTrue(0 < listOfConfProcs.size(), "List is empty");
		confProc = listOfConfProcs.get(0);
		
		logger.info("OK: Test for findAll completed");
		
		// Test findByUuid
		logger.debug("Looking for configured processor with UUID " + confProc.getUuid());
		confProc = RepositoryService.getConfiguredProcessorRepository().findByUuid(confProc.getUuid());
		assertNotNull(confProc, "Find by UUID failed for ConfiguredProcessor");
		
		logger.info("OK: Test for findByUuid completed");
		
	}

}
