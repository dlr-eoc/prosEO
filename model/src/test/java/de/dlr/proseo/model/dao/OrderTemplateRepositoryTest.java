/**
 * OrderTemplateRepositoryTest.java
 * 
 * (c) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.Assert.*;

import java.util.List;

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
import de.dlr.proseo.model.OrderTemplate;
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
public class OrderTemplateRepositoryTest {

	private static final String TEST_MISSIONCODE = "xyz567";
	private static final String TEST_NAME = "$OrderTemplate 4711$";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderTemplateRepositoryTest.class);
	
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
		
		OrderTemplate orderTemplate = new OrderTemplate();
		orderTemplate.setMission(mission);
		orderTemplate.setName(TEST_NAME);
		orderTemplate = RepositoryService.getOrderTemplateRepository().save(orderTemplate);
		
		// Test findByMissionCode
		List<OrderTemplate> orderTemplates = RepositoryService.getOrderTemplateRepository().findByMissionCode(TEST_MISSIONCODE);
		assertEquals("Find by mission code failed for OrderTemplate", 1, orderTemplates.size());
		
		logger.info("OK: Test for findByMissionCode completed");
		
		// Test findByMissionCodeAndName
		orderTemplate = RepositoryService.getOrderTemplateRepository().findByMissionCodeAndName(TEST_MISSIONCODE, TEST_NAME);
		assertNotNull("Find by mission code and name failed for OrderTemplate", orderTemplate);
		
		logger.info("OK: Test for findByMissionCodeAndName completed");
		
	}

}
