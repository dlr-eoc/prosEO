/**
 * OrderRepositoryTest.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
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

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.service.RepositoryApplication;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Unit test cases for ProductRepository
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootTest(classes = RepositoryApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Transactional
public class OrderRepositoryTest {

	private static final String TEST_MISSIONCODE = "xyz567";
	private static final String TEST_IDENTIFIER = "$Order 4711$";
	private static final Instant TEST_EXECUTION_TIME = Instant.from(OrbitTimeFormatter.parse("2018-06-13T09:23:45.000000")); // Timestamp without fraction of seconds!

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderRepositoryTest.class);
	
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
		
		ProcessingOrder order = new ProcessingOrder();
		order.setMission(mission);
		order.setIdentifier(TEST_IDENTIFIER);
		order.setUuid(UUID.randomUUID());
		order.setExecutionTime(TEST_EXECUTION_TIME);
		order = RepositoryService.getOrderRepository().save(order);
		mission.getProcessingOrders().add(order);
		
		// Test findByIdentifier
		order = RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(TEST_MISSIONCODE, TEST_IDENTIFIER);
		assertNotNull(order, "Find by identifier failed for ProcessingOrder");
		
		logger.info("OK: Test for findByIdentifier completed");
		
		// Test findByUuid
		order = RepositoryService.getOrderRepository().findByUuid(order.getUuid());
		assertNotNull(order, "Find by UUID failed for ProcessingOrder");
		
		logger.info("OK: Test for findByUuid completed");
		
		// Test findByExecutionTimeBetween
		List<ProcessingOrder> orders = RepositoryService.getOrderRepository().findByExecutionTimeBetween(
				TEST_EXECUTION_TIME, TEST_EXECUTION_TIME.plusSeconds(600));
		assertFalse(orders.isEmpty(), "Find by execution time between failed for ProcessingOrder");
		
		logger.info("OK: Test for findByExecutionTimeBetween completed");
		
	}

}
