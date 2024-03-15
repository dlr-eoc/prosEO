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
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.util.OrderUtil;
import de.dlr.proseo.planner.util.JobUtil;

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
    private JobUtil jobUtil;
    
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
	public void testApprovePlanReset() {
		logger.debug(">>> Starting testApprovePlanReset()");
		// stop dispatcher cycle first
		productionPlanner.stopDispatcher();
//	    List<Map<String, Object>> tableNames = jdbcTemplate.queryForList("SHOW TABLES");
//	    // Iterate over the table names
//	    for (Map<String, Object> tableName : tableNames) {
//	    	System.out.println(tableName.get("TABLE_NAME").toString());
//	    	List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + tableName.get("TABLE_NAME").toString());
//	    	// Print the results to the console
//	    	for (Map<String, Object> row : rows) {
//	    		System.out.println(row);
//	    	}
//	    }
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
		// check the actions according to the src/images/OrderStateStateMachineDiagram.png
		// order state is INITIAL, check illegal actions 
		// resMsg = orderUtil.approve(order);
		order = planOrder(transactionTemplate, orderId, OrderState.INITIAL);
		order = resumeOrder(transactionTemplate, orderId, OrderState.INITIAL);
		order = suspendOrder(transactionTemplate, orderId, OrderState.INITIAL);
		order = retryOrder(transactionTemplate, orderId, OrderState.INITIAL);
		order = cancelOrder(transactionTemplate, orderId, OrderState.INITIAL);
		order = resetOrder(transactionTemplate, orderId, OrderState.INITIAL);
		order = closeOrder(transactionTemplate, orderId, OrderState.INITIAL);

	
		// approve the order
		order = approveOrder(transactionTemplate, orderId, OrderState.APPROVED);
		logOrderState(transactionTemplate, orderId);
		
		// check illegal actions
		order = approveOrder(transactionTemplate, orderId, OrderState.APPROVED);
		// order = planOrder(transactionTemplate, orderId, OrderState.APPROVED);
		order = resumeOrder(transactionTemplate, orderId, OrderState.APPROVED);
		order = suspendOrder(transactionTemplate, orderId, OrderState.APPROVED);
		order = retryOrder(transactionTemplate, orderId, OrderState.APPROVED);
		order = cancelOrder(transactionTemplate, orderId, OrderState.APPROVED);
		// order = resetOrder(transactionTemplate, orderId, OrderState.APPROVED);
		order = closeOrder(transactionTemplate, orderId, OrderState.APPROVED);

		// reset order and approve again
		order = resetOrder(transactionTemplate, orderId, OrderState.INITIAL);
		order = approveOrder(transactionTemplate, orderId, OrderState.APPROVED);

		// plan the order, wait until planning is finished
		order = planOrder(transactionTemplate, orderId, OrderState.PLANNED, OrderState.PLANNING);
		logOrderState(transactionTemplate, orderId);
		
		// check illegal actions
		order = approveOrder(transactionTemplate, orderId, OrderState.PLANNED);
		order = planOrder(transactionTemplate, orderId, OrderState.PLANNED);
		// order = resumeOrder(transactionTemplate, orderId, OrderState.PLANNED);
		order = suspendOrder(transactionTemplate, orderId, OrderState.PLANNED);
		order = retryOrder(transactionTemplate, orderId, OrderState.PLANNED);
		// order = cancelOrder(transactionTemplate, orderId, OrderState.PLANNED);
		// order = resetOrder(transactionTemplate, orderId, OrderState.PLANNED);
		order = closeOrder(transactionTemplate, orderId, OrderState.PLANNED);
		
		// a planned order could be reset
		order = resetOrder(transactionTemplate, orderId, OrderState.INITIAL);
		
		
	}
	
	

	/**
	 * Test for Plan -> Release -> Suspend -> Release -> Cancel
	 */
	@Test
	@Sql("/ptm.sql")
	public void testPlanReleaseDelete() {
		logger.debug(">>> Starting testPlanReleaseDelete()");
		// stop dispatcher cycle first
		productionPlanner.stopDispatcher();
//	    List<Map<String, Object>> tableNames = jdbcTemplate.queryForList("SHOW TABLES");
//	    // Iterate over the table names
//	    for (Map<String, Object> tableName : tableNames) {
//	    	System.out.println(tableName.get("TABLE_NAME").toString());
//	    	List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + tableName.get("TABLE_NAME").toString());
//	    	// Print the results to the console
//	    	for (Map<String, Object> row : rows) {
//	    		System.out.println(row);
//	    	}
//	    }
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

		// release the order
		order = approveOrder(transactionTemplate, orderId, OrderState.APPROVED);
		order = planOrder(transactionTemplate, orderId, OrderState.PLANNED);
		order = resumeOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		logOrderState(transactionTemplate, orderId);

		// check illegal actions
		order = approveOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		order = planOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		order = resumeOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		// order = suspendOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		order = retryOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		order = cancelOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		order = resetOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		order = closeOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
			
		// suspend is possible
		order = suspendOrder(transactionTemplate, orderId, OrderState.PLANNED, OrderState.SUSPENDING);
		logOrderState(transactionTemplate, orderId);
		
		// Cancel the order
		order = cancelOrder(transactionTemplate, orderId, OrderState.FAILED);
		logOrderState(transactionTemplate, orderId);

		// check illegal actions
		order = approveOrder(transactionTemplate, orderId, OrderState.FAILED);
		order = planOrder(transactionTemplate, orderId, OrderState.FAILED);
		order = resumeOrder(transactionTemplate, orderId, OrderState.FAILED);
		order = suspendOrder(transactionTemplate, orderId, OrderState.FAILED);
		// order = retryOrder(transactionTemplate, orderId, OrderState.FAILED);
		order = cancelOrder(transactionTemplate, orderId, OrderState.FAILED);
		order = resetOrder(transactionTemplate, orderId, OrderState.FAILED);
		// order = closeOrder(transactionTemplate, orderId, OrderState.FAILED);
		
		// retry the order
		order = retryOrder(transactionTemplate, orderId, OrderState.PLANNED);
		logOrderState(transactionTemplate, orderId);

		// Cancel the order
		order = cancelOrder(transactionTemplate, orderId, OrderState.FAILED);
		logOrderState(transactionTemplate, orderId);
		
		// close the order		
		order = closeOrder(transactionTemplate, orderId, OrderState.CLOSED);

		 // check illegal actions
		 order = approveOrder(transactionTemplate, orderId, OrderState.CLOSED);
		 order = planOrder(transactionTemplate, orderId, OrderState.CLOSED);
		 order = resumeOrder(transactionTemplate, orderId, OrderState.CLOSED);
		 order = suspendOrder(transactionTemplate, orderId, OrderState.CLOSED);
		 order = retryOrder(transactionTemplate, orderId, OrderState.CLOSED);
		 order = cancelOrder(transactionTemplate, orderId, OrderState.CLOSED);
		 order = resetOrder(transactionTemplate, orderId, OrderState.CLOSED);
		 order = closeOrder(transactionTemplate, orderId, OrderState.CLOSED);		

		// delete the order
		
		resMsg = deleteOrder(transactionTemplate, orderId);
		
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


	/**
	 * Test for Plan -> Release -> Suspend -> Release -> Cancel
	 */
	@Test
	@Sql("/ptm.sql")
	public void testJob() {
		logger.debug(">>> Starting testPlanReleaseDelete()");
		// stop dispatcher cycle first
		productionPlanner.stopDispatcher();
//	    List<Map<String, Object>> tableNames = jdbcTemplate.queryForList("SHOW TABLES");
//	    // Iterate over the table names
//	    for (Map<String, Object> tableName : tableNames) {
//	    	System.out.println(tableName.get("TABLE_NAME").toString());
//	    	List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + tableName.get("TABLE_NAME").toString());
//	    	// Print the results to the console
//	    	for (Map<String, Object> row : rows) {
//	    		System.out.println(row);
//	    	}
//	    }
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
		order = approveOrder(transactionTemplate, orderId, OrderState.APPROVED);
		order = planOrder(transactionTemplate, orderId, OrderState.PLANNED);
		final Long jobId = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(MISSION_CODE, ORDER_L2);
			Job job = null;
			for (Job j : orderLoc.getJobs()) {
				job = j;
				break;
			}
			return job.getId();
		});
		
		// job is in state PLANNED
		// resume job
		resumeJob(transactionTemplate, jobId, JobState.RELEASED, JobState.STARTED);
		
		// check job actions
		resumeJob(transactionTemplate, jobId, JobState.RELEASED, JobState.STARTED);
	}
	private ProcessingOrder testResult(TransactionTemplate transactionTemplate, Long orderId, PlannerResultMessage resMsg, OrderState expectedState) {
		ProcessingOrder order = reloadOrder(transactionTemplate, orderId);
		return testResultPrim(order, resMsg, new OrderState[] {expectedState});
	};
	private ProcessingOrder testResultPrim(ProcessingOrder order, PlannerResultMessage resMsg, 
			 OrderState... expectedStates) {
		int i = 0;
		for (i = 0; i < expectedStates.length; i++) {
			if (order.getOrderState().equals(expectedStates[i])) {
				i = -1;
				break;
			}
		}
		if (i < 0) {
			
		} else {
			assertEquals("Order state error", expectedStates[0], order.getOrderState());
		}
		return order;
	};
	private Job testResultPrim(Job job, PlannerResultMessage resMsg, 
			 JobState... expectedStates) {
		int i = 0;
		for (i = 0; i < expectedStates.length; i++) {
			if (job.getJobState().equals(expectedStates[i])) {
				i = -1;
				break;
			}
		}
		if (i < 0) {
			
		} else {
			assertEquals("Order state error", expectedStates[0], job.getJobState());
		}
		return job;
	};
	private JobStep testResultPrim(JobStep jobStep, PlannerResultMessage resMsg, 
			 JobStepState... expectedStates) {
		int i = 0;
		for (i = 0; i < expectedStates.length; i++) {
			if (jobStep.getJobStepState().equals(expectedStates[i])) {
				i = -1;
				break;
			}
		}
		if (i < 0) {
			
		} else {
			assertEquals("Order state error", expectedStates[0], jobStep.getJobStepState());
		}
		return jobStep;
	};
	

	private ProcessingOrder approveOrder(TransactionTemplate transactionTemplate, Long orderId, OrderState... orderStates) {
		transactionTemplate.setReadOnly(false);
		final ProcessingOrder order = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = null;
			Optional<ProcessingOrder> optOrder = RepositoryService.getOrderRepository().findById(orderId);
			if (optOrder != null) {
				orderLoc = optOrder.get();
				PlannerResultMessage resMsg = orderUtil.approve(orderLoc);
				orderLoc = RepositoryService.getOrderRepository().findById(orderId).get();
				testResultPrim(orderLoc, resMsg, orderStates);
				return orderLoc;
			}
			return null;
		});		
		return order;
	}
	private ProcessingOrder cancelOrder(TransactionTemplate transactionTemplate, Long orderId, OrderState... orderStates) {
		transactionTemplate.setReadOnly(false);
		final ProcessingOrder order = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = null;
			Optional<ProcessingOrder> optOrder = RepositoryService.getOrderRepository().findById(orderId);
			if (optOrder != null) {
				orderLoc = optOrder.get();
				PlannerResultMessage resMsg = orderUtil.cancel(orderLoc);
				orderLoc = RepositoryService.getOrderRepository().findById(orderId).get();
				testResultPrim(orderLoc, resMsg, orderStates);
				return orderLoc;
			}
			return null;
		});		
		return order;
	}
	private ProcessingOrder closeOrder(TransactionTemplate transactionTemplate, Long orderId, OrderState... orderStates) {
		transactionTemplate.setReadOnly(false);
		PlannerResultMessage resMsg = orderUtil.close(orderId);
		ProcessingOrder order = reloadOrder(transactionTemplate, orderId);
		testResultPrim(order, resMsg, orderStates);
		return order;
	}
	private ProcessingOrder planOrder(TransactionTemplate transactionTemplate, Long orderId, OrderState... orderStates) {
		transactionTemplate.setReadOnly(false);
		final ProcessingFacility facility = transactionTemplate.execute((status) -> {
			return RepositoryService.getFacilityRepository().findByName(FACILITY_NAME);
		});	
		PlannerResultMessage resMsg = orderUtil.plan(orderId, facility, true);
		ProcessingOrder order = reloadOrder(transactionTemplate, orderId);
		testResultPrim(order, resMsg, orderStates);
		return order;
	}
	private PlannerResultMessage deleteOrder(TransactionTemplate transactionTemplate, Long orderId) {
		transactionTemplate.setReadOnly(false);
		final PlannerResultMessage msg = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = null;
			Optional<ProcessingOrder> optOrder = RepositoryService.getOrderRepository().findById(orderId);
			if (optOrder != null) {
				orderLoc = optOrder.get();
				PlannerResultMessage resMsg = orderUtil.delete(orderLoc);
				assertEquals("Delete order failed.", resMsg.getSuccess(), true);
				return resMsg;
			}
			return null;
		});		
		return msg;
	}
	private ProcessingOrder resetOrder(TransactionTemplate transactionTemplate, Long orderId, OrderState... orderStates) {
		transactionTemplate.setReadOnly(false);
		final ProcessingOrder order = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = null;
			Optional<ProcessingOrder> optOrder = RepositoryService.getOrderRepository().findById(orderId);
			if (optOrder != null) {
				orderLoc = optOrder.get();
				PlannerResultMessage resMsg = orderUtil.reset(orderLoc);
				orderLoc = RepositoryService.getOrderRepository().findById(orderId).get();
				testResultPrim(orderLoc, resMsg, orderStates);
				return orderLoc;
			}
			return null;
		});		
		return order;
	}
	private ProcessingOrder resumeOrder(TransactionTemplate transactionTemplate, Long orderId, OrderState... orderStates) {
		transactionTemplate.setReadOnly(false);
		final ProcessingOrder orderX = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = null;
			Optional<ProcessingOrder> optOrder = RepositoryService.getOrderRepository().findById(orderId);
			if (optOrder != null) {
				orderLoc = optOrder.get();
				return orderLoc;
			}
			return null;
		});	
		PlannerResultMessage resMsg = orderUtil.resume(orderX, true, null, null);
		ProcessingOrder order = reloadOrder(transactionTemplate, orderId);
		testResultPrim(order, resMsg, orderStates);
		return order;
	}
	private ProcessingOrder retryOrder(TransactionTemplate transactionTemplate, Long orderId, OrderState... orderStates) {
		transactionTemplate.setReadOnly(false);
		final ProcessingOrder order = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = null;
			Optional<ProcessingOrder> optOrder = RepositoryService.getOrderRepository().findById(orderId);
			if (optOrder != null) {
				orderLoc = optOrder.get();
				PlannerResultMessage resMsg = orderUtil.retry(orderLoc);
				orderLoc = RepositoryService.getOrderRepository().findById(orderId).get();
				testResultPrim(orderLoc, resMsg, orderStates);
				return orderLoc;
			}
			return null;
		});		
		return order;
	}
	private ProcessingOrder suspendOrder(TransactionTemplate transactionTemplate, Long orderId, OrderState... orderStates) {
		transactionTemplate.setReadOnly(false);
		PlannerResultMessage resMsg = orderUtil.suspend(orderId, true);
		ProcessingOrder order = reloadOrder(transactionTemplate, orderId);
		testResultPrim(order, resMsg, orderStates);
		return order;
	}
	

	private Job resumeJob(TransactionTemplate transactionTemplate, Long jobId, JobState... jobStates) {
		transactionTemplate.setReadOnly(false);
		final Job job = transactionTemplate.execute((status) -> {
			Job jobLoc = null;
			Optional<Job> optJob = RepositoryService.getJobRepository().findById(jobId);
			if (optJob != null) {
				jobLoc = optJob.get();
				PlannerResultMessage resMsg = jobUtil.resume(jobLoc);
				jobLoc = RepositoryService.getJobRepository().findById(jobId).get();
				testResultPrim(jobLoc, resMsg, jobStates);
				return jobLoc;
			}
			return null;
		});		
		return job;
	}
}
