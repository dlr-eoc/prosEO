/**
 * ConfigurationRepositoryTest.java
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

import de.dlr.proseo.model.Configuration;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for ConfigurationRepository
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class ConfigurationRepositoryTest {

	private static final String TEST_CODE = "$xyz$";
	private static final String TEST_VERSION = "$02.00.01$";
	private static final String TEST_NAME = "$KNMI L2$";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ConfigurationRepositoryTest.class);
	
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
		mission.setCode(TEST_CODE);
		mission = RepositoryService.getMissionRepository().save(mission);
		
		ProcessorClass procClass = new ProcessorClass();
		procClass.setMission(mission);
		procClass.setProcessorName(TEST_NAME);
		procClass = RepositoryService.getProcessorClassRepository().save(procClass);
				
		Configuration conf = new Configuration();
		conf.setProcessorClass(procClass);
		conf.setConfigurationVersion(TEST_VERSION);
		conf = RepositoryService.getConfigurationRepository().save(conf);

		// Test findByMissionCodeAndProcessorNameAndConfigurationVersion
		conf = RepositoryService.getConfigurationRepository().findByMissionCodeAndProcessorNameAndConfigurationVersion(TEST_CODE, TEST_NAME, TEST_VERSION);
		assertNotNull("Find by processor name and configuration version failed for Processor", conf);
		
		logger.info("OK: Test for findByMissionCodeAndProcessorNameAndConfigurationVersion completed");
		
	}

}
