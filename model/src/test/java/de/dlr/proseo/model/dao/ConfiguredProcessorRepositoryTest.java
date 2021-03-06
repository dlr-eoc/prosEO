/**
 * ConfiguredProcessorRepositoryTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.Assert.*;

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

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Unit test cases for ConfiguredProcessorRepository
 *
 * @author Dr. Thomas Bassler
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
@AutoConfigureTestEntityManager
public class ConfiguredProcessorRepositoryTest {

	private static final String TEST_IDENTIFIER = "myConfProc";
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ConfiguredProcessorRepositoryTest.class);
	
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
		ConfiguredProcessor confProc = new ConfiguredProcessor();
		confProc.setIdentifier(TEST_IDENTIFIER);
		confProc.setUuid(UUID.randomUUID());
		RepositoryService.getConfiguredProcessorRepository().save(confProc);
		
		// Test findByIdentifier
		confProc = RepositoryService.getConfiguredProcessorRepository().findByIdentifier(TEST_IDENTIFIER);
		assertNotNull("Find by identifier failed for ConfiguredProcessor", confProc);
		
		logger.info("OK: Test for findByIdentifier completed");
		
		// Test findByUuid
		confProc = RepositoryService.getConfiguredProcessorRepository().findByUuid(confProc.getUuid());
		assertNotNull("Find by UUID failed for ConfiguredProcessor", confProc);
		
		logger.info("OK: Test for findByUuid completed");
		
	}

}
