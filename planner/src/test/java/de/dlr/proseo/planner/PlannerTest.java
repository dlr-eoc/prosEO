package de.dlr.proseo.planner;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
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
	
	@Test
	@Sql("/ptm.sql")
	public void test() {
		logger.debug(">>> Starting test()");
	    List<Map<String, Object>> tableNames = jdbcTemplate.queryForList("SHOW TABLES");
	    // Iterate over the table names
	    for (Map<String, Object> tableName : tableNames) {
	        // Execute a SELECT * FROM <tableName> query
	    	if (!"PRODUCT_PROCESSING_FACILITIES".equalsIgnoreCase(tableName.get("TABLE_NAME").toString())) {
	    		System.out.println(tableName.get("TABLE_NAME").toString());
	    		List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + tableName.get("TABLE_NAME").toString());
	    		// Print the results to the console
	    		for (Map<String, Object> row : rows) {
	    			System.out.println(row);
	    		}
	    	}
	    }

		List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM product where product_class_id = 40");
		List<Product> ps = jdbcTemplate.queryForList("SELECT * FROM product where product_class_id = 40", Product.class);

		logger.debug(">>> Approve order {}", ORDER_L2);
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(false);
		// approve saves the order
		final ProcessingOrder order = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(MISSION_CODE, ORDER_L2);
			orderUtil.approve(orderLoc);
			return orderLoc;
		});
		final Long orderId = order.getId();
		logOrderState(transactionTemplate, orderId);
		transactionTemplate.setReadOnly(true);
		final ProcessingFacility facility = transactionTemplate.execute((status) -> {
			return RepositoryService.getFacilityRepository().findByName(FACILITY_NAME);
		});	
		transactionTemplate.setReadOnly(false);
		// planning can't be interrupted, use the wait feature
		orderUtil.plan(orderId, facility, true);
		logOrderState(transactionTemplate, orderId);
		// now release the order in an own thread and try to suspend it
		// test only standard implementation (without notification endpoint)
		orderUtil.resume(order, false, null, null);
		logOrderState(transactionTemplate, orderId);
		orderUtil.suspend(orderId, true);
		logOrderState(transactionTemplate, orderId);
		// now release with wait
		orderUtil.resume(order, true, null, null);
		logOrderState(transactionTemplate, orderId);
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
