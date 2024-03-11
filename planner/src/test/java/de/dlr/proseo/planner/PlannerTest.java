package de.dlr.proseo.planner;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.util.OrderUtil;

/**
 * 
 */

/**
 * @author melchinger
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ProductionPlanner.class, webEnvironment = WebEnvironment.RANDOM_PORT)
//@DirtiesContext
@WithMockUser(username = "PTM-proseo", roles = { "ORDER_APPROVER", "ORDER_MGR" })
@AutoConfigureTestEntityManager
//@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
//@EnableJpaRepositories("de.dlr.proseo.model.dao")
public class PlannerTest {

	private static String ORDER_L2 = "L2_orbits_3000-3002";
	private static String MISSION_CODE = "PTM";
	private static String FACILITY_NAME = "localhost";
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(PlannerTest.class);

    @Autowired
    private ProductionPlanner productionPlanner;
    @Autowired
    private OrderUtil orderUtil;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;


	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	private OrderState logOrderState(TransactionTemplate transactionTemplate, Long orderId) {
		Boolean isReadOnly = transactionTemplate.isReadOnly();
		transactionTemplate.setReadOnly(true);
		final OrderState state = transactionTemplate.execute((status) -> {
			Optional<ProcessingOrder> optOrder = RepositoryService.getOrderRepository().findById(orderId);
			if (optOrder != null) {
				logger.debug("    New order state: {}", optOrder.get().getOrderState());
				return optOrder.get().getOrderState();
			}
			return null;
		});		
		transactionTemplate.setReadOnly(isReadOnly);
		return state;
	} 
	
	private ProcessingOrder reloadOrder(TransactionTemplate transactionTemplate, Long orderId) {
		Boolean isReadOnly = transactionTemplate.isReadOnly();
		transactionTemplate.setReadOnly(true);
		final ProcessingOrder order = transactionTemplate.execute((status) -> {
			Optional<ProcessingOrder> optOrder = RepositoryService.getOrderRepository().findById(orderId);
			if (optOrder != null) {
				return optOrder.get();
			}
			return null;
		});		
		transactionTemplate.setReadOnly(isReadOnly);
		return order;
	}
	
	@Test
	@Sql("/ptm.sql")
	public void test() {
		logger.debug(">>> Starting test()");
		productionPlanner.stopDispatcher();
	    List<Map<String, Object>> tableNames = jdbcTemplate.queryForList("SHOW TABLES");
	    // Iterate over the table names
	    for (Map<String, Object> tableName : tableNames) {
	        // Execute a SELECT * FROM <tableName> query
	    	// if (!"PRODUCT_PROCESSING_FACILITIES".equalsIgnoreCase(tableName.get("TABLE_NAME").toString())) {
	    		System.out.println(tableName.get("TABLE_NAME").toString());
	    		List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + tableName.get("TABLE_NAME").toString());
	    		// Print the results to the console
	    		for (Map<String, Object> row : rows) {
	    			System.out.println(row);
	    		}
	    	// }
	    }
	    productionPlanner.updateKubeConfigs();
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);
		// get the order id
		final Long orderId = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(MISSION_CODE, ORDER_L2);
			return orderLoc.getId();
		});
		ProcessingOrder order = reloadOrder(transactionTemplate, orderId);
		final ProcessingFacility facility = transactionTemplate.execute((status) -> {
			return RepositoryService.getFacilityRepository().findByName(FACILITY_NAME);
		});	

		PlannerResultMessage resMsg;
		transactionTemplate.setReadOnly(false);
		// check illegal actions 
		// resMsg = orderUtil.approve(order);
		resMsg = orderUtil.plan(orderId, facility, true);
		resMsg = orderUtil.resume(order, true, null, null);
		resMsg = orderUtil.suspend(orderId, true);
		resMsg = orderUtil.retry(order);
		resMsg = orderUtil.cancel(order);
		resMsg = orderUtil.reset(order);
		resMsg = orderUtil.close(orderId);
		logger.log(resMsg.getMessage(), order.getIdentifier()); // close doesn't log
		
		// approve the order
		resMsg = orderUtil.approve(order);
		order = reloadOrder(transactionTemplate, orderId);
		logOrderState(transactionTemplate, orderId);
		
		// check illegal actions
		resMsg = orderUtil.approve(order);
		// resMsg = orderUtil.plan(orderId, facility, true);
		resMsg = orderUtil.resume(order, true, null, null);
		resMsg = orderUtil.suspend(orderId, true);
		resMsg = orderUtil.retry(order);
		resMsg = orderUtil.cancel(order);
		// resMsg = orderUtil.reset(order);
		resMsg = orderUtil.close(orderId);
		logger.log(resMsg.getMessage(), order.getIdentifier()); // close doesn't log

		// plan the order
		orderUtil.plan(orderId, facility, true);
		order = reloadOrder(transactionTemplate, orderId);
		logOrderState(transactionTemplate, orderId);
		
		// check illegal actions
		resMsg = orderUtil.approve(order);
		resMsg = orderUtil.plan(orderId, facility, true);
		// resMsg = orderUtil.resume(order, true, null, null);
		resMsg = orderUtil.suspend(orderId, true);
		resMsg = orderUtil.retry(order);
		// resMsg = orderUtil.cancel(order);
		// resMsg = orderUtil.reset(order);
		resMsg = orderUtil.close(orderId);
		logger.log(resMsg.getMessage(), order.getIdentifier()); // close doesn't log

		
		try {
			logger.debug(">>> run one cycle");
			productionPlanner.startDispatcher();
			// wait a bit to run one cycle
			Thread.sleep(100);
			productionPlanner.stopDispatcher();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
