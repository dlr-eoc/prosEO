package de.dlr.proseo.planner;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.util.OrderUtil;
import de.dlr.proseo.planner.util.JobStepUtil;
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
// @DirtiesContext
@WithMockUser(username = "PTM-proseo", roles = { "ORDER_APPROVER", "ORDER_MGR" })
@AutoConfigureTestEntityManager
// @EnableConfigurationProperties
@ComponentScan(basePackages = { "de.dlr.proseo" })
// @EnableJpaRepositories("de.dlr.proseo.model.dao")
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
	private JobStepUtil jobStepUtil;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

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
	
	// DUMMY to have a  valid test class
	@Test
	public void testDummy() {}

	// TEST DISABLED - FIX ptm.sql!
	//@Test
	//@Sql("/ptm.sql")
	public void testApprovePlanReset() {
		logger.debug(">>> Starting testApprovePlanReset()");
		// stop dispatcher cycle first
		productionPlanner.stopDispatcher();
		// List<Map<String, Object>> tableNames =
		// jdbcTemplate.queryForList("SHOW TABLES");
		// // Iterate over the table names
		// for (Map<String, Object> tableName : tableNames) {
		// System.out.println(tableName.get("TABLE_NAME").toString());
		// List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT *
		// FROM " + tableName.get("TABLE_NAME").toString());
		// // Print the results to the console
		// for (Map<String, Object> row : rows) {
		// System.out.println(row);
		// }
		// }
		productionPlanner.updateKubeConfigs();
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);
		// get the order id
		final Long orderId = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(MISSION_CODE,
					ORDER_L2);
			return orderLoc.getId();
		});
		reloadOrder(transactionTemplate, orderId);
		transactionTemplate.setReadOnly(false);
		// check the actions according to the
		// src/images/OrderStateStateMachineDiagram.png
		// order state is INITIAL, check illegal actions
		// resMsg = orderUtil.approve(order);
		planOrder(transactionTemplate, orderId, OrderState.INITIAL);
		resumeOrder(transactionTemplate, orderId, OrderState.INITIAL);
		suspendOrder(transactionTemplate, orderId, OrderState.INITIAL);
		retryOrder(transactionTemplate, orderId, OrderState.INITIAL);
		cancelOrder(transactionTemplate, orderId, OrderState.INITIAL);
		resetOrder(transactionTemplate, orderId, OrderState.INITIAL);
		closeOrder(transactionTemplate, orderId, OrderState.INITIAL);

		// approve the order
		approveOrder(transactionTemplate, orderId, OrderState.APPROVED);
		logOrderState(transactionTemplate, orderId);

		// check illegal actions
		approveOrder(transactionTemplate, orderId, OrderState.APPROVED);
		// planOrder(transactionTemplate, orderId, OrderState.APPROVED);
		resumeOrder(transactionTemplate, orderId, OrderState.APPROVED);
		suspendOrder(transactionTemplate, orderId, OrderState.APPROVED);
		retryOrder(transactionTemplate, orderId, OrderState.APPROVED);
		cancelOrder(transactionTemplate, orderId, OrderState.APPROVED);
		// resetOrder(transactionTemplate, orderId, OrderState.APPROVED);
		closeOrder(transactionTemplate, orderId, OrderState.APPROVED);

		// reset order and approve again
		resetOrder(transactionTemplate, orderId, OrderState.INITIAL);
		approveOrder(transactionTemplate, orderId, OrderState.APPROVED);

		// plan the order, wait until planning is finished
		planOrder(transactionTemplate, orderId, OrderState.PLANNED, OrderState.PLANNING);
		checkJobsAndSteps(transactionTemplate, orderId, OrderState.PLANNED, OrderState.PLANNING);
		logOrderState(transactionTemplate, orderId);

		// check illegal actions
		approveOrder(transactionTemplate, orderId, OrderState.PLANNED);
		planOrder(transactionTemplate, orderId, OrderState.PLANNED);
		// resumeOrder(transactionTemplate, orderId, OrderState.PLANNED);
		suspendOrder(transactionTemplate, orderId, OrderState.PLANNED);
		retryOrder(transactionTemplate, orderId, OrderState.PLANNED);
		// cancelOrder(transactionTemplate, orderId, OrderState.PLANNED);
		// resetOrder(transactionTemplate, orderId, OrderState.PLANNED);
		closeOrder(transactionTemplate, orderId, OrderState.PLANNED);

		// a planned order could be reset
		resetOrder(transactionTemplate, orderId, OrderState.INITIAL);

		// further actions in method testPlanReleaseDelete cause h2database
		// problem (runtime system with postgres not)

	}

	/**
	 * Test for Plan -> Release -> Suspend -> Release -> Cancel
	 */
	// TEST DISABLED - FIX ptm.sql!
	//@Test
	//@Sql("/ptm.sql")
	public void testPlanReleaseDelete() {
		logger.debug(">>> Starting testPlanReleaseDelete()");
		// stop dispatcher cycle first
		productionPlanner.stopDispatcher();
		// List<Map<String, Object>> tableNames =
		// jdbcTemplate.queryForList("SHOW TABLES");
		// // Iterate over the table names
		// for (Map<String, Object> tableName : tableNames) {
		// System.out.println(tableName.get("TABLE_NAME").toString());
		// List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT *
		// FROM " + tableName.get("TABLE_NAME").toString());
		// // Print the results to the console
		// for (Map<String, Object> row : rows) {
		// System.out.println(row);
		// }
		// }
		productionPlanner.updateKubeConfigs();
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);
		// get the order id
		final Long orderId = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(MISSION_CODE,
					ORDER_L2);
			return orderLoc.getId();
		});
		reloadOrder(transactionTemplate, orderId);
		transactionTemplate.setReadOnly(false);

		// release the order
		approveOrder(transactionTemplate, orderId, OrderState.APPROVED);
		planOrder(transactionTemplate, orderId, OrderState.PLANNED);
		checkJobsAndSteps(transactionTemplate, orderId, OrderState.PLANNED, OrderState.PLANNING);
		resumeOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		checkJobsAndSteps(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		logOrderState(transactionTemplate, orderId);

		// check illegal actions
		approveOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		planOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		resumeOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		// suspendOrder(transactionTemplate, orderId, OrderState.RELEASED,
		// OrderState.RELEASING, OrderState.RUNNING);
		retryOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		cancelOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		resetOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);
		closeOrder(transactionTemplate, orderId, OrderState.RELEASED, OrderState.RELEASING, OrderState.RUNNING);

		// suspend is possible
		suspendOrder(transactionTemplate, orderId, OrderState.PLANNED, OrderState.SUSPENDING);
		checkJobsAndSteps(transactionTemplate, orderId, OrderState.PLANNED, OrderState.SUSPENDING);
		logOrderState(transactionTemplate, orderId);

		// Cancel the order
		cancelOrder(transactionTemplate, orderId, OrderState.FAILED);
		checkJobsAndSteps(transactionTemplate, orderId, OrderState.FAILED);
		logOrderState(transactionTemplate, orderId);

		// check illegal actions
		approveOrder(transactionTemplate, orderId, OrderState.FAILED);
		planOrder(transactionTemplate, orderId, OrderState.FAILED);
		resumeOrder(transactionTemplate, orderId, OrderState.FAILED);
		suspendOrder(transactionTemplate, orderId, OrderState.FAILED);
		// retryOrder(transactionTemplate, orderId, OrderState.FAILED);
		cancelOrder(transactionTemplate, orderId, OrderState.FAILED);
		resetOrder(transactionTemplate, orderId, OrderState.FAILED);
		// closeOrder(transactionTemplate, orderId, OrderState.FAILED);

		// retry the order
		retryOrder(transactionTemplate, orderId, OrderState.PLANNED);
		checkJobsAndSteps(transactionTemplate, orderId, OrderState.PLANNED);
		logOrderState(transactionTemplate, orderId);

		// Cancel the order
		cancelOrder(transactionTemplate, orderId, OrderState.FAILED);
		checkJobsAndSteps(transactionTemplate, orderId, OrderState.FAILED);
		logOrderState(transactionTemplate, orderId);

		// close the order
		closeOrder(transactionTemplate, orderId, OrderState.CLOSED);
		checkJobsAndSteps(transactionTemplate, orderId, OrderState.CLOSED);

		// check illegal actions
		approveOrder(transactionTemplate, orderId, OrderState.CLOSED);
		planOrder(transactionTemplate, orderId, OrderState.CLOSED);
		resumeOrder(transactionTemplate, orderId, OrderState.CLOSED);
		suspendOrder(transactionTemplate, orderId, OrderState.CLOSED);
		retryOrder(transactionTemplate, orderId, OrderState.CLOSED);
		cancelOrder(transactionTemplate, orderId, OrderState.CLOSED);
		resetOrder(transactionTemplate, orderId, OrderState.CLOSED);
		closeOrder(transactionTemplate, orderId, OrderState.CLOSED);

		// delete the order

		deleteOrder(transactionTemplate, orderId);

		try {
			logger.debug(">>> run one cycle");
			productionPlanner.startDispatcher();
			// wait a bit to run one cycle
			Thread.sleep(100);
			productionPlanner.stopDispatcher();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Dispatcher run failed with exception: %s/%s".formatted(e.getClass().getName(), e.getMessage()));
		}
	}

	/**
	 * Test for Plan -> Release -> Suspend -> Release -> Cancel
	 */
	// TEST DISABLED - FIX ptm.sql!
	//@Test
	//@Sql("/ptm.sql")
	public void testJobAndStep() {
		logger.debug(">>> Starting testJobAndStep()");
		// stop dispatcher cycle first
		productionPlanner.stopDispatcher();
		// List<Map<String, Object>> tableNames =
		// jdbcTemplate.queryForList("SHOW TABLES");
		// // Iterate over the table names
		// for (Map<String, Object> tableName : tableNames) {
		// System.out.println(tableName.get("TABLE_NAME").toString());
		// List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT *
		// FROM " + tableName.get("TABLE_NAME").toString());
		// // Print the results to the console
		// for (Map<String, Object> row : rows) {
		// System.out.println(row);
		// }
		// }
		productionPlanner.updateKubeConfigs();
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);
		// get the order id
		final Long orderId = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(MISSION_CODE,
					ORDER_L2);
			return orderLoc.getId();
		});
		reloadOrder(transactionTemplate, orderId);
		transactionTemplate.setReadOnly(false);
		approveOrder(transactionTemplate, orderId, OrderState.APPROVED);
		planOrder(transactionTemplate, orderId, OrderState.PLANNED);
		final Long jobId = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(MISSION_CODE,
					ORDER_L2);
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
		// suspendJob(transactionTemplate, jobId, JobState.RELEASED,
		// JobState.STARTED);
		cancelJob(transactionTemplate, jobId, JobState.RELEASED, JobState.STARTED);
		retryJob(transactionTemplate, jobId, JobState.RELEASED, JobState.STARTED);

		// suspend
		suspendJob(transactionTemplate, jobId, JobState.RELEASED, JobState.PLANNED);

		// check job actions
		// resumeJob(transactionTemplate, jobId, JobState.PLANNED);
		suspendJob(transactionTemplate, jobId, JobState.PLANNED);
		// cancelJob(transactionTemplate, jobId, JobState.PLANNED);
		retryJob(transactionTemplate, jobId, JobState.PLANNED);

		// cancel
		cancelJob(transactionTemplate, jobId, JobState.FAILED);

		// check job actions
		resumeJob(transactionTemplate, jobId, JobState.FAILED);
		suspendJob(transactionTemplate, jobId, JobState.FAILED);
		cancelJob(transactionTemplate, jobId, JobState.FAILED);
		// retryJob(transactionTemplate, jobId, JobState.FAILED);

		// retry
		retryJob(transactionTemplate, jobId, JobState.PLANNED);

		// check job actions
		// resumeJob(transactionTemplate, jobId, JobState.PLANNED);
		suspendJob(transactionTemplate, jobId, JobState.PLANNED);
		// cancelJob(transactionTemplate, jobId, JobState.PLANNED);
		retryJob(transactionTemplate, jobId, JobState.PLANNED);

		// job steps
		final Long jobStepId = transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(MISSION_CODE,
					ORDER_L2);
			JobStep jobStep = null;
			for (Job j : orderLoc.getJobs()) {
				for (JobStep js : j.getJobSteps()) {
					jobStep = js;
					break;
				}
				if (jobStep != null) {
					break;
				}
			}
			return jobStep.getId();
		});

		// jobStep is in state PLANNED
		// resume jobStep
		resumeJobStep(transactionTemplate, jobStepId, JobStepState.READY, JobStepState.WAITING_INPUT, JobStepState.RUNNING);

		// check jobStep actions
		resumeJobStep(transactionTemplate, jobStepId, JobStepState.READY, JobStepState.WAITING_INPUT, JobStepState.RUNNING);
		// suspendJobStep(transactionTemplate, jobStepId, JobStepState.READY,
		// JobStepState.WAITING_INPUT, JobStepState.RUNNING);
		cancelJobStep(transactionTemplate, jobStepId, JobStepState.READY, JobStepState.WAITING_INPUT, JobStepState.RUNNING);
		retryJobStep(transactionTemplate, jobStepId, JobStepState.READY, JobStepState.WAITING_INPUT, JobStepState.RUNNING);

		// suspend
		suspendJobStep(transactionTemplate, jobStepId, JobStepState.PLANNED);

		// check jobStep actions
		// resumeJobStep(transactionTemplate, jobStepId, JobStepState.PLANNED);
		suspendJobStep(transactionTemplate, jobStepId, JobStepState.PLANNED);
		// cancelJobStep(transactionTemplate, jobStepId, JobStepState.PLANNED);
		retryJobStep(transactionTemplate, jobStepId, JobStepState.PLANNED);

		// cancel
		cancelJobStep(transactionTemplate, jobStepId, JobStepState.FAILED);

		// check jobStep actions
		resumeJobStep(transactionTemplate, jobStepId, JobStepState.FAILED);
		suspendJobStep(transactionTemplate, jobStepId, JobStepState.FAILED);
		cancelJobStep(transactionTemplate, jobStepId, JobStepState.FAILED);
		// retryJobStep(transactionTemplate, jobStepId, JobStepState.FAILED);

		// retry
		retryJobStep(transactionTemplate, jobStepId, JobStepState.PLANNED);

		// check jobStep actions
		// resumeJobStep(transactionTemplate, jobStepId, JobStepState.PLANNED);
		suspendJobStep(transactionTemplate, jobStepId, JobStepState.PLANNED);
		// cancelJobStep(transactionTemplate, jobStepId, JobStepState.PLANNED);
		retryJobStep(transactionTemplate, jobStepId, JobStepState.PLANNED);
	}

	private ProcessingOrder testResultPrim(ProcessingOrder order, PlannerResultMessage resMsg, OrderState... expectedStates) {
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

	private Job testResultPrim(Job job, PlannerResultMessage resMsg, JobState... expectedStates) {
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

	private JobStep testResultPrim(JobStep jobStep, PlannerResultMessage resMsg, JobStepState... expectedStates) {
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
		PlannerResultMessage resMsg = orderUtil.plan(orderId, facility.getId(), true);
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

	private Job suspendJob(TransactionTemplate transactionTemplate, Long jobId, JobState... jobStates) {
		transactionTemplate.setReadOnly(false);
		final Job job = transactionTemplate.execute((status) -> {
			Job jobLoc = null;
			Optional<Job> optJob = RepositoryService.getJobRepository().findById(jobId);
			if (optJob != null) {
				jobLoc = optJob.get();
				PlannerResultMessage resMsg = jobUtil.suspend(jobLoc, true);
				jobLoc = RepositoryService.getJobRepository().findById(jobId).get();
				testResultPrim(jobLoc, resMsg, jobStates);
				return jobLoc;
			}
			return null;
		});
		return job;
	}

	private Job cancelJob(TransactionTemplate transactionTemplate, Long jobId, JobState... jobStates) {
		transactionTemplate.setReadOnly(false);
		final Job job = transactionTemplate.execute((status) -> {
			Job jobLoc = null;
			Optional<Job> optJob = RepositoryService.getJobRepository().findById(jobId);
			if (optJob != null) {
				jobLoc = optJob.get();
				PlannerResultMessage resMsg = jobUtil.cancel(jobLoc);
				jobLoc = RepositoryService.getJobRepository().findById(jobId).get();
				testResultPrim(jobLoc, resMsg, jobStates);
				return jobLoc;
			}
			return null;
		});
		return job;
	}

	private Job retryJob(TransactionTemplate transactionTemplate, Long jobId, JobState... jobStates) {
		transactionTemplate.setReadOnly(false);
		final Job job = transactionTemplate.execute((status) -> {
			Job jobLoc = null;
			Optional<Job> optJob = RepositoryService.getJobRepository().findById(jobId);
			if (optJob != null) {
				jobLoc = optJob.get();
				PlannerResultMessage resMsg = jobUtil.retry(jobLoc);
				jobLoc = RepositoryService.getJobRepository().findById(jobId).get();
				testResultPrim(jobLoc, resMsg, jobStates);
				return jobLoc;
			}
			return null;
		});
		return job;
	}

	private JobStep resumeJobStep(TransactionTemplate transactionTemplate, Long jobStepId, JobStepState... jobStepStates) {
		transactionTemplate.setReadOnly(false);
		final JobStep jobStep = transactionTemplate.execute((status) -> {
			JobStep jobStepLoc = null;
			Optional<JobStep> optJobStep = RepositoryService.getJobStepRepository().findById(jobStepId);
			if (optJobStep != null) {
				jobStepLoc = optJobStep.get();
				PlannerResultMessage resMsg = jobStepUtil.resume(jobStepLoc, true);
				jobStepLoc = RepositoryService.getJobStepRepository().findById(jobStepId).get();
				testResultPrim(jobStepLoc, resMsg, jobStepStates);
				return jobStepLoc;
			}
			return null;
		});
		return jobStep;
	}

	private JobStep suspendJobStep(TransactionTemplate transactionTemplate, Long jobStepId, JobStepState... jobStepStates) {
		transactionTemplate.setReadOnly(false);
		final JobStep jobStep = transactionTemplate.execute((status) -> {
			JobStep jobStepLoc = null;
			Optional<JobStep> optJobStep = RepositoryService.getJobStepRepository().findById(jobStepId);
			if (optJobStep != null) {
				jobStepLoc = optJobStep.get();
				PlannerResultMessage resMsg = jobStepUtil.suspend(jobStepLoc, true);
				jobStepLoc = RepositoryService.getJobStepRepository().findById(jobStepId).get();
				testResultPrim(jobStepLoc, resMsg, jobStepStates);
				return jobStepLoc;
			}
			return null;
		});
		return jobStep;
	}

	private JobStep cancelJobStep(TransactionTemplate transactionTemplate, Long jobStepId, JobStepState... jobStepStates) {
		transactionTemplate.setReadOnly(false);
		final JobStep jobStep = transactionTemplate.execute((status) -> {
			JobStep jobStepLoc = null;
			Optional<JobStep> optJobStep = RepositoryService.getJobStepRepository().findById(jobStepId);
			if (optJobStep != null) {
				jobStepLoc = optJobStep.get();
				PlannerResultMessage resMsg = jobStepUtil.cancel(jobStepLoc);
				jobStepLoc = RepositoryService.getJobStepRepository().findById(jobStepId).get();
				testResultPrim(jobStepLoc, resMsg, jobStepStates);
				return jobStepLoc;
			}
			return null;
		});
		return jobStep;
	}

	private JobStep retryJobStep(TransactionTemplate transactionTemplate, Long jobStepId, JobStepState... jobStepStates) {
		transactionTemplate.setReadOnly(false);
		final JobStep jobStep = transactionTemplate.execute((status) -> {
			JobStep jobStepLoc = null;
			Optional<JobStep> optJobStep = RepositoryService.getJobStepRepository().findById(jobStepId);
			if (optJobStep != null) {
				jobStepLoc = optJobStep.get();
				PlannerResultMessage resMsg = jobStepUtil.retry(jobStepLoc);
				jobStepLoc = RepositoryService.getJobStepRepository().findById(jobStepId).get();
				testResultPrim(jobStepLoc, resMsg, jobStepStates);
				return jobStepLoc;
			}
			return null;
		});
		return jobStep;
	}

	private void checkJobsAndSteps(TransactionTemplate transactionTemplate, Long orderId, OrderState... orderStates) {
		transactionTemplate.execute((status) -> {
			ProcessingOrder orderLoc = null;
			Optional<ProcessingOrder> optOrder = RepositoryService.getOrderRepository().findById(orderId);
			if (optOrder != null) {
				orderLoc = optOrder.get();
				orderLoc = RepositoryService.getOrderRepository().findById(orderId).get();
				List<JobState> jobStates = new ArrayList<JobState>();
				for (OrderState orderState : orderStates) {
					switch (orderState) {
					case INITIAL:
						jobStates.add(JobState.INITIAL);
						break;
					case APPROVED:
						jobStates.add(JobState.INITIAL);
						break;
					case PLANNING:
						jobStates.add(JobState.INITIAL);
						break;
					case PLANNING_FAILED:
						jobStates.add(JobState.INITIAL);
						break;
					case PLANNED:
						jobStates.add(JobState.PLANNED);
						break;
					case RELEASING:
						jobStates.add(JobState.PLANNED);
						break;
					case RELEASED:
						jobStates.add(JobState.RELEASED);
						break;
					case RUNNING:
						jobStates.add(JobState.STARTED);
						break;
					case SUSPENDING:
						jobStates.add(JobState.STARTED);
						jobStates.add(JobState.PLANNED);
						break;
					case COMPLETED:
						jobStates.add(JobState.COMPLETED);
						break;
					case FAILED:
						jobStates.add(JobState.FAILED);
						break;
					case CLOSED:
						jobStates.add(JobState.CLOSED);
						break;
					default:
						break;
					}
				}
				List<JobStepState> jobStepStates = new ArrayList<JobStepState>();
				for (OrderState orderState : orderStates) {
					switch (orderState) {
					case INITIAL:
						jobStepStates.add(JobStepState.PLANNED);
						break;
					case APPROVED:
						jobStepStates.add(JobStepState.PLANNED);
						break;
					case PLANNING:
						jobStepStates.add(JobStepState.PLANNED);
						break;
					case PLANNING_FAILED:
						jobStepStates.add(JobStepState.PLANNED);
						break;
					case PLANNED:
						jobStepStates.add(JobStepState.PLANNED);
						break;
					case RELEASING:
						jobStepStates.add(JobStepState.PLANNED);
						break;
					case RELEASED:
						jobStepStates.add(JobStepState.READY);
						jobStepStates.add(JobStepState.WAITING_INPUT);
						break;
					case RUNNING:
						jobStepStates.add(JobStepState.RUNNING);
						break;
					case SUSPENDING:
						jobStepStates.add(JobStepState.RUNNING);
						jobStepStates.add(JobStepState.PLANNED);
						break;
					case COMPLETED:
						jobStepStates.add(JobStepState.COMPLETED);
						break;
					case FAILED:
						jobStepStates.add(JobStepState.FAILED);
						break;
					case CLOSED:
						jobStepStates.add(JobStepState.CLOSED);
						break;
					default:
						break;
					}
				}
				for (Job job : orderLoc.getJobs()) {
					if (!jobStates.contains(job.getJobState())) {
						assertEquals("Job state error: ", jobStates.get(0), job.getJobState());
//						logger.debug("Job state error: unexpected state {}", job.getJobState());
					}
					for (JobStep jobStep : job.getJobSteps()) {
						if (!jobStepStates.contains(jobStep.getJobStepState())) {
							assertEquals("Job step state error: ", jobStepStates.get(0), jobStep.getJobStepState());
//							logger.debug("Job step state error: unexpected state {}", jobStep.getJobStepState());
						}
					}
				}
			}
			return null;
		});
	}
}
