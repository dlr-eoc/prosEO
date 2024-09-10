/**
 * OrderUtil.java
 *
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.interfaces.rest.model.RestMessage;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.ProcessingOrderHistory;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.enums.OrderSource;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.planner.PlannerResultMessage;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.ProductionPlannerConfiguration;
import de.dlr.proseo.planner.dispatcher.OrderDispatcher;

/**
 * Handle processing orders
 *
 * @author Ernst Melchinger
 */
@Component
public class OrderUtil {

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderUtil.class);

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** Planner configuration */
	@Autowired
	ProductionPlannerConfiguration config;

	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/** Utility class for handling jobs */
	@Autowired
	private JobUtil jobUtil;

	/** The order dispatcher instance */
	@Autowired
	private OrderDispatcher orderDispatcher;

	/** The Production Planner instance */
	@Autowired
	private ProductionPlanner productionPlanner;

	/**
	 * Cancel the processing order and it jobs and job steps.
	 *
	 * @param orderX The processing order
	 * @return Result message
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public PlannerResultMessage cancel(ProcessingOrder orderX) {
		if (logger.isTraceEnabled())
			logger.trace(">>> cancel({})", (null == orderX ? "null" : orderX.getId()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		if (orderX != null) {
			ProcessingOrder order = RepositoryService.getOrderRepository().findById(orderX.getId()).get();
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
				answer.setMessage(PlannerMessage.ORDER_HASTOBE_PLANNED);
				break;
			case PLANNED:
				for (Job job : order.getJobs()) {
					jobUtil.cancel(job);
				}
				order.setOrderState(OrderState.FAILED);
				setStateMessage(order, ProductionPlanner.STATE_MESSAGE_CANCELLED);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				UtilService.getOrderUtil().setOrderHistory(order);
				answer.setMessage(PlannerMessage.ORDER_CANCELED);
				break;
			case RELEASED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RELEASED);
				break;
			case RELEASING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RELEASING);
				break;
			case RUNNING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RUNNING);
				break;
			case SUSPENDING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_SUSPENDING);
				break;
			case COMPLETED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_COMPLETED);
				break;
			case FAILED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_FAILED);
				break;
			case CLOSED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_CLOSED);
				break;
			default:
				break;
			}
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}
		return answer;
	}

	/**
	 * Reset the processing order and it jobs and job steps.
	 *
	 * @param order The processing order
	 * @return Result message
	 */
	public PlannerResultMessage reset(ProcessingOrder order) {
		if (logger.isTraceEnabled())
			logger.trace(">>> reset({})", (null == order ? "null" : order.getId()));
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer.setMessage(PlannerMessage.ORDER_RESET);
				break;
			case PLANNING:
				// look for plan thread and interrupt it
				OrderPlanThread pt = productionPlanner.getPlanThreads().get(ProductionPlanner.PLAN_THREAD_PREFIX + order.getId());
				if (pt != null) {
					pt.interrupt();
					int i = 0;
					while (pt.isAlive() && i < 1000) {
						i++;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							break;
						}
					}
				}
				try {
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							transactionTemplate.execute((status) -> {
								Optional<ProcessingOrder> opt = RepositoryService.getOrderRepository().findById(order.getId());
								if (opt.isPresent()) {
									ProcessingOrder orderx = opt.get();
									orderx.setOrderState(OrderState.APPROVED);
									setStateMessage(order, ProductionPlanner.STATE_MESSAGE_QUEUED);
									orderx.setHasFailedJobSteps(false);
									orderx.incrementVersion();
									RepositoryService.getOrderRepository().save(orderx);
									logOrderState(orderx);
								}
								return null;
							});
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled())
								logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled())
									logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}
				} catch (Exception e) {
					answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
					answer.setText(logger.log(answer.getMessage(), e.getMessage()));

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);
				}
				answer.setMessage(PlannerMessage.ORDER_RESET);
				break;
			case PLANNING_FAILED:
				// jobs are in initial state, no change
				try {
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							transactionTemplate.execute((status) -> {
								Optional<ProcessingOrder> opt = RepositoryService.getOrderRepository().findById(order.getId());
								if (opt.isPresent()) {
									ProcessingOrder orderx = opt.get();
									orderx.setOrderState(OrderState.APPROVED);
									setStateMessage(order, ProductionPlanner.STATE_MESSAGE_QUEUED);
									orderx.setHasFailedJobSteps(false);
									orderx.incrementVersion();
									RepositoryService.getOrderRepository().save(orderx);
									logOrderState(orderx);
								}
								return null;
							});
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled())
								logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled())
									logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}
				} catch (Exception e) {
					answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
					answer.setText(logger.log(answer.getMessage(), e.getMessage()));

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);
				}
				answer.setMessage(PlannerMessage.ORDER_RESET);
				break;
			case APPROVED:
			case RELEASED:
			case PLANNED:
				// remove jobs and job steps
				try {
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							transactionTemplate.execute((status) -> {
								Optional<ProcessingOrder> opt = RepositoryService.getOrderRepository().findById(order.getId());
								if (opt.isPresent()) {
									ProcessingOrder orderx = opt.get();
									HashMap<Long, Job> toRemove = new HashMap<Long, Job>();
									for (Job job : orderx.getJobs()) {
										if (jobUtil.delete(job)) {
											toRemove.put(job.getId(), job);
										}
									}
									List<Job> existingJobs = new ArrayList<Job>();
									existingJobs.addAll(orderx.getJobs());
									orderx.getJobs().clear();
									for (Job job : existingJobs) {
										if (toRemove.get(job.getId()) == null) {
											orderx.getJobs().add(job);
										} else {
											RepositoryService.getJobRepository().delete(job);
										}
									}
									orderx.setOrderState(OrderState.INITIAL);
									setStateMessage(order, ProductionPlanner.STATE_MESSAGE_QUEUED);
									orderx.setHasFailedJobSteps(false);
									orderx.incrementVersion();
									RepositoryService.getOrderRepository().save(orderx);
									logOrderState(orderx);
								}
								return null;
							});
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled())
								logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled())
									logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}
				} catch (Exception e) {
					answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
					answer.setText(logger.log(answer.getMessage(), e.getMessage()));

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);
				}
				answer.setMessage(PlannerMessage.ORDER_RESET);
				break;
			case RELEASING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RELEASING);
				break;
			case RUNNING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RUNNING);
				break;
			case SUSPENDING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_SUSPENDING);
				break;
			case COMPLETED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_COMPLETED);
				break;
			case FAILED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_FAILED);
				break;
			case CLOSED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_CLOSED);
				break;
			default:
				break;
			}
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}
		return answer;
	}

	/**
	 * Delete the processing order and it jobs and job steps.
	 *
	 * @param order The processing order
	 * @return Result message
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public PlannerResultMessage delete(ProcessingOrder order) {
		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", (null == order ? "null" : order.getId()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
				// jobs are in initial state, no change
				UtilService.getOrderUtil().setOrderHistoryOrderDeleted(order);
				RepositoryService.getOrderRepository().delete(order);
				answer.setMessage(PlannerMessage.ORDER_DELETED);
				break;
			case PLANNED:
			case COMPLETED:
			case FAILED:
			case CLOSED:
				// remove jobs and jobsteps
				HashMap<Long, Job> toRemove = new HashMap<>();
				for (Job job : order.getJobs()) {
					if (jobUtil.deleteForced(job)) {
						toRemove.put(job.getId(), job);
					}
				}
				List<Job> existingJobs = new ArrayList<>();
				existingJobs.addAll(order.getJobs());
				order.getJobs().clear();
				for (Job job : existingJobs) {
					if (toRemove.get(job.getId()) == null) {
						order.getJobs().add(job);
					} else {
						job.setProcessingOrder(null);
						RepositoryService.getJobRepository().delete(job);
					}
				}
				UtilService.getOrderUtil().setOrderHistoryOrderDeleted(order);
				RepositoryService.getOrderRepository().delete(order);
				answer.setMessage(PlannerMessage.ORDER_DELETED);
				break;
			case RELEASING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RELEASING);
				break;
			case RELEASED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RELEASED);
				break;
			case RUNNING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RUNNING);
				break;
			case SUSPENDING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_SUSPENDING);
				break;
			default:
				break;
			}
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}
		return answer;
	}

	/**
	 * Approve the processing order and it jobs and job steps.
	 *
	 * @param order The processing order
	 * @return Result message
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public PlannerResultMessage approve(ProcessingOrder order) {
		if (logger.isTraceEnabled())
			logger.trace(">>> approve({})", (null == order ? "null" : order.getId()));

		PlannerResultMessage answer = new PlannerResultMessage(PlannerMessage.ORDER_ALREADY_APPROVED);
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				// jobs are in initial state, no change
				order.setOrderState(OrderState.APPROVED);
				setStateMessage(order, ProductionPlanner.STATE_MESSAGE_QUEUED);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer.setMessage(PlannerMessage.ORDER_APPROVED);
				break;
			case APPROVED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_APPROVED);
				break;
			case PLANNED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_PLANNED);
				break;
			case RELEASING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RELEASING);
				break;
			case RELEASED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RELEASED);
				break;
			case RUNNING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RUNNING);
				break;
			case SUSPENDING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_SUSPENDING);
				break;
			case COMPLETED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_COMPLETED);
				break;
			case FAILED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_FAILED);
				break;
			case CLOSED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_CLOSED);
				break;
			default:
				break;
			}
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}
		return answer;
	}

	/**
	 * Plan the processing order and it jobs and job steps.
	 *
	 * @param id           The processing order ID
	 * @param facilityId   The database ID of the processing facility to run the order
	 * @param wait         indicates whether to wait for the order planning to complete
	 * @return Result message
	 */
	public PlannerResultMessage plan(long id, Long facilityId, Boolean wait) {
		if (logger.isTraceEnabled())
			logger.trace(">>> plan({}, {}, {})", id, facilityId, wait);

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		transactionTemplate.setReadOnly(true);
		final ProcessingOrder order = transactionTemplate.execute((status) -> {
			Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
			if (orderOpt.isPresent()) {
				return orderOpt.get();
			}
			return null;
		});

		if (null == order || null == facilityId) {
			PlannerResultMessage answer = new PlannerResultMessage(PlannerMessage.ORDER_NOT_EXIST);
			answer.setText(logger.log(answer.getMessage(), id));
			return answer;
		}

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);

		switch (order.getOrderState()) {
		case INITIAL:
			answer.setMessage(PlannerMessage.ORDER_HASTOBE_APPROVED);
			break;
		case APPROVED:
		case PLANNING_FAILED:
			// Set order state to PLANNING
			try {
				transactionTemplate.setReadOnly(false);
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						answer = transactionTemplate.execute((status) -> {
							Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
							if (orderOpt.isPresent()) {
								orderOpt.get().setOrderState(OrderState.PLANNING);
								orderOpt.get().incrementVersion();
								RepositoryService.getOrderRepository().save(orderOpt.get());

								return new PlannerResultMessage(PlannerMessage.ORDER_PLANNING);
							} else {
								return new PlannerResultMessage(PlannerMessage.ORDER_NOT_EXIST);
							}
						});

						break;
					} catch (CannotAcquireLockException e) {
						if (logger.isDebugEnabled())
							logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled())
								logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					}
				}
			} catch (Exception e) {
				answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
				answer.setText(logger.log(answer.getMessage(), e.getMessage()));

				if (logger.isDebugEnabled())
					logger.debug("... exception stack trace: ", e);
			}
			if (!answer.getSuccess()) {
				break;
			}

			// Fall through
		case PLANNING:
			// Moved here from the transaction above, because it does not affect the transaction
			// TODO Check whether "orderOpt.get()" should be in place of "order"? (and then it should be within the transaction!)
			// "order" is local to this method and is never updated in the database!
			order.setStateMessage(ProductionPlanner.STATE_MESSAGE_QUEUED);

			// Create a planning thread for this order, if required
			String threadName = ProductionPlanner.PLAN_THREAD_PREFIX + order.getId();
			if (!productionPlanner.getPlanThreads().containsKey(threadName)) {
				OrderPlanThread pt = new OrderPlanThread(productionPlanner, orderDispatcher, id, facilityId, threadName);
				productionPlanner.getPlanThreads().put(threadName, pt);
				pt.start();
				if (wait) {
					try {
						pt.join();
						answer = pt.getResultMessage();
					} catch (InterruptedException e) {
						e.printStackTrace();
						answer.setMessage(PlannerMessage.PLANNING_INTERRUPTED);
					}
				}
			}

			break;
		case PLANNED:
			answer.setMessage(PlannerMessage.ORDER_ALREADY_PLANNED);
			break;
		case RELEASING:
			answer.setMessage(PlannerMessage.ORDER_ALREADY_RELEASING);
			break;
		case RELEASED:
			answer.setMessage(PlannerMessage.ORDER_ALREADY_RELEASED);
			break;
		case RUNNING:
			answer.setMessage(PlannerMessage.ORDER_ALREADY_RUNNING);
			break;
		case SUSPENDING:
			answer.setMessage(PlannerMessage.ORDER_ALREADY_SUSPENDING);
			break;
		case COMPLETED:
			answer.setMessage(PlannerMessage.ORDER_ALREADY_COMPLETED);
			break;
		case FAILED:
			answer.setMessage(PlannerMessage.ORDER_ALREADY_FAILED);
			break;
		case CLOSED:
			answer.setMessage(PlannerMessage.ORDER_ALREADY_CLOSED);
			break;
		default:
			break;
		}
		answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));

		return answer;
	}

	/**
	 * Resume the processing order and it jobs and job steps.
	 *
	 * @param order The processing order
	 * @param wait  indicates whether to wait for the order releasing to complete
	 * @param user  the username for calling other prosEO services (e. g. AIP Client)
	 * @param pw    the password for calling other prosEO services (e. g. AIP Client)
	 * @return Result message
	 */
	public PlannerResultMessage resume(ProcessingOrder order, Boolean wait, String user, String pw) {
		if (logger.isTraceEnabled())
			logger.trace(">>> resume({})", (null == order ? "null" : order.getId()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer.setMessage(PlannerMessage.ORDER_HASTOBE_APPROVED);
				break;
			case APPROVED:
			case PLANNING:
				answer.setMessage(PlannerMessage.ORDER_HASTOBE_PLANNED);
				break;
			case PLANNED:
			case RELEASING:
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
				Boolean doIt = false;
				try {
					transactionTemplate.setReadOnly(false);
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							doIt = transactionTemplate.execute((status) -> {
								Optional<ProcessingOrder> opt = RepositoryService.getOrderRepository().findById(order.getId());
								if (opt.isPresent()) {
									ProcessingOrder orderx = opt.get();
									orderx.setOrderState(OrderState.RELEASING);
									// setStateMessage(order, ProductionPlanner.STATE_MESSAGE_RUNNING); 
									// moved out of transaction, see below
									orderx.incrementVersion();
									orderx = RepositoryService.getOrderRepository().save(orderx);
									return true;
								}
								return false;
							});
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled())
								logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled())
									logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}

				} catch (Exception e) {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);
				}
				if (doIt) {
					// Moved here from the transaction above, because it does not affect the transaction
					// TODO Check whether "orderOpt.get()" should be in place of "order"? (and then it should be within the
					// transaction!)
					// "order" is local to this method and is never updated in the database!
					order.setStateMessage(ProductionPlanner.STATE_MESSAGE_RUNNING);

					OrderReleaseThread releaseThread = null;
					try {
						transactionTemplate.setReadOnly(true);
						final ProcessingOrder ordery = transactionTemplate.execute((status) -> {
							Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(order.getId());
							if (orderOpt.isPresent()) {
								return orderOpt.get();
							}
							return null;
						});
						if (ordery != null) {
							String threadName = ProductionPlanner.RELEASE_THREAD_PREFIX + ordery.getId();
							if (!productionPlanner.getReleaseThreads().containsKey(threadName)) {
								releaseThread = new OrderReleaseThread(productionPlanner, em, jobUtil, ordery, threadName);
								productionPlanner.getReleaseThreads().put(threadName, releaseThread);
							}
							logOrderState(order);
							answer.setMessage(PlannerMessage.ORDER_RELEASING);
						}
					} catch (Exception e) {
						logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

						if (logger.isDebugEnabled())
							logger.debug("... exception stack trace: ", e);
					}
					if (releaseThread != null) {
						releaseThread.start();
						if (wait) {
							try {
								releaseThread.join();
								answer = releaseThread.getResultMessage();
							} catch (InterruptedException e) {
								answer.setMessage(PlannerMessage.ORDER_RELEASING_INTERRUPTED);
								answer.setText(logger.log(answer.getMessage(), releaseThread.getName(), order.getIdentifier()));
								e.printStackTrace();
							}
						}
					}
				}
				break;
			case RELEASED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RELEASED);
				break;
			case RUNNING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RUNNING);
				break;
			case SUSPENDING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_SUSPENDING);
				break;
			case COMPLETED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_COMPLETED);
				break;
			case FAILED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_FAILED);
				break;
			case CLOSED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_CLOSED);
				break;
			default:
				break;
			}
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}
		return answer;
	}

	/**
	 * Start the processing order and it jobs and job steps.
	 *
	 * @param order The processing order
	 * @return Result message
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public PlannerResultMessage startOrder(ProcessingOrder order) {
		if (logger.isTraceEnabled())
			logger.trace(">>> startOrder({})", (null == order ? "null" : order.getId()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer.setMessage(PlannerMessage.ORDER_HASTOBE_APPROVED);
				break;
			case APPROVED:
				answer.setMessage(PlannerMessage.ORDER_HASTOBE_PLANNED);
				break;
			case PLANNED:
				answer.setMessage(PlannerMessage.ORDER_HASTOBE_RELEASED);
				break;
			case RELEASING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_RELEASING);
				break;
			case RELEASED:
				order.setOrderState(OrderState.RUNNING);
				setStateMessage(order, ProductionPlanner.STATE_MESSAGE_RUNNING);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer.setMessage(PlannerMessage.ORDER_RUNNING);
				break;
			case RUNNING:
				answer.setMessage(PlannerMessage.ORDER_RUNNING);
				break;
			case SUSPENDING:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_SUSPENDING);
				break;
			case COMPLETED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_COMPLETED);
				break;
			case FAILED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_FAILED);
				break;
			case CLOSED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_CLOSED);
				break;
			default:
				break;
			}
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}
		return answer;
	}

	/**
	 * Suspend the processing order and its jobs and job steps.
	 *
	 * @param id The processing order ID
	 * @param force The flag to force kill of currently running job steps on processing facility
	 * @return Result message
	 */
	public PlannerResultMessage suspend(long id, Boolean force) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		final ProcessingOrder order = transactionTemplate.execute((status) -> {
			Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
			if (orderOpt.isPresent()) {
				return orderOpt.get();
			}
			return null;
		});
		if (logger.isTraceEnabled())
			logger.trace(">>> suspend({}, {})", (null == order ? "null" : order.getId()), force);
		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer.setMessage(PlannerMessage.ORDER_SUSPENDED);
				break;
			case APPROVED:
				answer.setMessage(PlannerMessage.ORDER_SUSPENDED);
				break;
			case PLANNED:
				answer.setMessage(PlannerMessage.ORDER_SUSPENDED);
				break;
			case RELEASING:
			case RUNNING:
			case SUSPENDING:
				// look for release thread and interrupt it
				OrderReleaseThread releaseThread = productionPlanner.getReleaseThreads()
					.get(ProductionPlanner.RELEASE_THREAD_PREFIX + order.getId());
				if (releaseThread != null) {
					releaseThread.interrupt();
					int i = 0;
					while (releaseThread.isAlive() && i < 1000) {
						i++;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							break;
						}
					}
				}
				try {
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							transactionTemplate.execute((status) -> {
								ProcessingOrder ordery = null;
								Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
								if (orderOpt.isPresent()) {
									ordery = orderOpt.get();
								}
								if (ordery == null) {
									return null;
								}
								if (ordery.getOrderState() == OrderState.RELEASING) {
									ordery.setOrderState(OrderState.RELEASED);
									ordery.setOrderState(OrderState.RUNNING);
									setStateMessage(order, ProductionPlanner.STATE_MESSAGE_RUNNING);
								}
								ordery.setOrderState(OrderState.SUSPENDING);
								setStateMessage(order, ProductionPlanner.STATE_MESSAGE_CANCELLED);
								RepositoryService.getOrderRepository().save(ordery);
								return ordery;
							});
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled())
								logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled())
									logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}

					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							answer = transactionTemplate.execute((status) -> {
								boolean suspending = false;
								boolean allFinished = true;
								ProcessingOrder orderz = null;
								Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
								if (orderOpt.isPresent()) {
									orderz = orderOpt.get();
								}
								if (orderz == null) {
									return new PlannerResultMessage(PlannerMessage.ORDER_NOT_EXIST);
								}
								for (Job job : orderz.getJobs()) {
									jobUtil.suspend(job, force);
									// check for state
									if (job.getJobState() == JobState.COMPLETED || job.getJobState() == JobState.RELEASED) {
										allFinished = allFinished & true;
									} else {
										allFinished = allFinished & false;
									}
									if (job.getJobState() == JobState.ON_HOLD || job.getJobState() == JobState.STARTED) {
										suspending = true;
									}
								}
								orderz.incrementVersion();
								if (orderz.getOrderState() == OrderState.RUNNING) {
									orderz.setOrderState(OrderState.SUSPENDING);
									setStateMessage(order, ProductionPlanner.STATE_MESSAGE_CANCELLED);
								}
								if (suspending) {
									// check whether some jobs are already finished
									orderz.setOrderState(OrderState.SUSPENDING);
									setStateMessage(order, ProductionPlanner.STATE_MESSAGE_CANCELLED);
									RepositoryService.getOrderRepository().save(orderz);
									logOrderState(orderz);
									return new PlannerResultMessage(PlannerMessage.ORDER_SUSPENDED);
								} else if (allFinished) {
									// check whether some jobs are already finished
									orderz.setOrderState(OrderState.COMPLETED);
									setTimes(orderz);
									setStateMessage(orderz, ProductionPlanner.STATE_MESSAGE_COMPLETED);
									checkAutoClose(orderz);
									RepositoryService.getOrderRepository().save(orderz);
									logOrderState(orderz);	
									UtilService.getOrderUtil().setOrderHistory(orderz);		
									return new PlannerResultMessage(PlannerMessage.ORDER_COMPLETED);
								} else {
									orderz.setOrderState(OrderState.PLANNED);
									setStateMessage(order, ProductionPlanner.STATE_MESSAGE_QUEUED);
									RepositoryService.getOrderRepository().save(orderz);
									logOrderState(orderz);
									return new PlannerResultMessage(PlannerMessage.ORDER_SUSPENDED);
								}
							});
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled())
								logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled())
									logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}

				} catch (Exception e) {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);
				} finally {
					productionPlanner.checkNextForRestart();
				}
				break;
			case RELEASED:
				try {
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							answer = transactionTemplate.execute((status) -> {
								ProcessingOrder orderz = null;
								Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
								if (orderOpt.isPresent()) {
									orderz = orderOpt.get();
								}
								if (orderz == null) {
									return new PlannerResultMessage(PlannerMessage.ORDER_NOT_EXIST);
								}
								for (Job job : orderz.getJobs()) {
									jobUtil.suspend(job, force);
								}
								if (orderz.getOrderState() == OrderState.RELEASED) {
									orderz.setOrderState(OrderState.SUSPENDING);
									setStateMessage(order, ProductionPlanner.STATE_MESSAGE_CANCELLED);
								}
								if (orderz.getOrderState() == OrderState.RUNNING) {
									orderz.setOrderState(OrderState.SUSPENDING);
									setStateMessage(order, ProductionPlanner.STATE_MESSAGE_CANCELLED);
								}
								orderz.setOrderState(OrderState.PLANNED);
								setStateMessage(order, ProductionPlanner.STATE_MESSAGE_QUEUED);
								orderz.incrementVersion();
								RepositoryService.getOrderRepository().save(orderz);
								logOrderState(orderz);
								return new PlannerResultMessage(PlannerMessage.ORDER_SUSPENDED);
							});
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled())
								logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled())
									logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}

				} catch (Exception e) {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);
				} finally {
					productionPlanner.checkNextForRestart();
				}
				break;
			case COMPLETED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_COMPLETED);
				break;
			case FAILED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_FAILED);
				break;
			case CLOSED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_CLOSED);
				break;
			default:
				break;
			}
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}
		return answer;
	}

	/**
	 * Prepare the suspend of the processing order. All jobs are set to state ON_HOLD first to avoid start of further job steps.
	 *
	 * @param id    The processing order ID
	 * @param force The flag to force kill of currently running job steps on processing facility
	 * @return Result message
	 */
	public PlannerResultMessage prepareSuspend(long id, Boolean force) {
		if (logger.isTraceEnabled())
			logger.trace(">>> prepareSuspend({}, {})", id, force);

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		transactionTemplate.setReadOnly(true);
		final ProcessingOrder order = transactionTemplate.execute((status) -> {
			Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
			if (orderOpt.isPresent()) {
				return orderOpt.get();
			}
			return null;
		});
		if (logger.isTraceEnabled())
			logger.trace(">>> prepareSuspend({}, {})", (null == order ? "null" : order.getId()), force);
		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer.setMessage(PlannerMessage.ORDER_SUSPENDED);
				break;
			case APPROVED:
				answer.setMessage(PlannerMessage.ORDER_SUSPENDED);
				break;
			case PLANNED:
				answer.setMessage(PlannerMessage.ORDER_SUSPENDED);
				break;
			case RELEASING:
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
				// look for release thread and interrupt it
				answer.setMessage(GeneralMessage.TRUE);
				OrderReleaseThread rt = productionPlanner.getReleaseThreads()
					.get(ProductionPlanner.RELEASE_THREAD_PREFIX + order.getId());
				if (rt != null) {
					rt.interrupt();
					int i = 0;
					while (rt.isAlive() && i < 1000) {
						i++;
						try {
							Thread.sleep(1000);
							rt.interrupt();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							break;
						}
					}
					if (rt.isAlive()) {
						answer.setMessage(PlannerMessage.ORDER_COULD_NOT_INTERRUPT);
					}
				}
				if (answer.getSuccess()) {
					try {
						transactionTemplate.setReadOnly(false);
						for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
							try {
								transactionTemplate.execute((status) -> {
									ProcessingOrder orderz = null;
									Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
									if (orderOpt.isPresent()) {
										orderz = orderOpt.get();
									}
									if (orderz.getOrderState() == OrderState.RELEASING) {
										orderz.setOrderState(OrderState.RELEASED);
										orderz.setOrderState(OrderState.RUNNING);
									}
									orderz.setOrderState(OrderState.SUSPENDING);
									setStateMessage(order, ProductionPlanner.STATE_MESSAGE_CANCELLED);
									RepositoryService.getOrderRepository().save(orderz);
									return null;
								});
								break;
							} catch (CannotAcquireLockException e) {
								if (logger.isDebugEnabled())
									logger.debug("... database concurrency issue detected: ", e);

								if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
									ProseoUtil.dbWait();
								} else {
									if (logger.isDebugEnabled())
										logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
									throw e;
								}
							}
						}

						for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
							try {
								transactionTemplate.execute((status) -> {
									ProcessingOrder orderz = null;
									Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
									if (orderOpt.isPresent()) {
										orderz = orderOpt.get();
									}
									for (Job job : orderz.getJobs()) {
										switch (job.getJobState()) {
										case INITIAL:
											job.setJobState(de.dlr.proseo.model.Job.JobState.RELEASED);
											// intentionally fall through
										case RELEASED:
											job.setJobState(de.dlr.proseo.model.Job.JobState.STARTED);
											// intentionally fall through
										case STARTED:
											job.setJobState(de.dlr.proseo.model.Job.JobState.ON_HOLD);
											RepositoryService.getJobRepository().save(job);
											break;
										default:
											break;
										}
									}
									return null;
								});
								break;
							} catch (CannotAcquireLockException e) {
								if (logger.isDebugEnabled())
									logger.debug("... database concurrency issue detected: ", e);

								if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
									ProseoUtil.dbWait();
								} else {
									if (logger.isDebugEnabled())
										logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
									throw e;
								}
							}
						}

						answer.setMessage(PlannerMessage.ORDER_SUSPEND_PREPARED);
					} catch (Exception e) {
						logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

						if (logger.isDebugEnabled())
							logger.debug("... exception stack trace: ", e);
					}
				}
				break;
			case COMPLETED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_COMPLETED);
				break;
			case FAILED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_FAILED);
				break;
			case CLOSED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_CLOSED);
				break;
			default:
				break;
			}
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}
		return answer;
	}

	/**
	 * Retry the processing order and it jobs and job steps.
	 *
	 * @param order The processing order
	 * @return Result message
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public PlannerResultMessage retry(ProcessingOrder order) {
		if (logger.isTraceEnabled())
			logger.trace(">>> retry({})", (null == order ? "null" : order.getId()));

		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:
			case RELEASING:
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
			case COMPLETED:
				answer.setMessage(PlannerMessage.ORDER_COULD_NOT_RETRY);
				break;
			case FAILED:
				boolean all = true;
				boolean allCompleted = true;
				order.setHasFailedJobSteps(false);
				for (Job job : order.getJobs()) {
					jobUtil.retry(job);
				}
				for (Job job : order.getJobs()) {
					if (!(job.getJobState() == JobState.PLANNED || job.getJobState() == JobState.COMPLETED)) {
						all = false;
					}
					if (job.getJobState() != JobState.COMPLETED) {
						allCompleted = false;
					}
					if (job.getHasFailedJobSteps()) {
						order.setHasFailedJobSteps(true);
					}
				}
				if (all) {
					if (order.getOrderState() == OrderState.COMPLETED) {
						answer.setMessage(PlannerMessage.ORDER_COMPLETED);
					} else if (allCompleted) {
						if (order.getOrderState() != OrderState.RUNNING) {
							order.setOrderState(OrderState.PLANNED);
							order.setOrderState(OrderState.RELEASED);
							order.setOrderState(OrderState.RUNNING);
						}
						order.setOrderState(OrderState.COMPLETED);
						checkAutoClose(order);
						setTimes(order);
						setStateMessage(order, ProductionPlanner.STATE_MESSAGE_COMPLETED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						answer.setMessage(PlannerMessage.ORDER_COMPLETED);
						UtilService.getOrderUtil().setOrderHistory(order);
					} else {
						if (order.getOrderState() == OrderState.RUNNING) {
							order.setOrderState(OrderState.SUSPENDING);
						}
						order.setOrderState(OrderState.PLANNED);
						setStateMessage(order, ProductionPlanner.STATE_MESSAGE_QUEUED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						answer.setMessage(PlannerMessage.ORDER_RETRIED);
					}
					logOrderState(order);
				} else {
					answer.setMessage(PlannerMessage.ORDER_COULD_NOT_RETRY);
				}
				break;
			case CLOSED:
				answer.setMessage(PlannerMessage.ORDER_ALREADY_CLOSED);
				break;
			default:
				break;
			}
			answer.setText(logger.log(answer.getMessage(), order.getIdentifier()));
		}
		return answer;
	}

	/**
	 * Close the processing order and it jobs and job steps.
	 *
	 * @param orderId The processing order ID
	 * @return Result message
	 */
	public PlannerResultMessage close(Long orderId) {
		if (logger.isTraceEnabled())
			logger.trace(">>> close({})", (null == orderId ? "null" : orderId));

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		List<Long> jobIds = new ArrayList<>();
		transactionTemplate.setReadOnly(true);
		final OrderState orderState = transactionTemplate.execute((status) -> {
			String sqlQuery = "select order_state from processing_order where id = " + orderId + ";";
			Query query = em.createNativeQuery(sqlQuery);
			Object o = query.getSingleResult();
			return OrderState.valueOf((String) o);
		});
		transactionTemplate.execute((status) -> {
			String sqlQuery = "select id from job where processing_order_id = " + orderId + ";";
			Query query = em.createNativeQuery(sqlQuery);
			List<?> ol = query.getResultList();
			for (Object o : ol) {
				if (o instanceof BigInteger) {
					jobIds.add(((BigInteger) o).longValue());
				}
			}
			return null;
		});
		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		if (orderId != null) {
			switch (orderState) {
			case INITIAL:
			case APPROVED:
			case PLANNED:
			case RELEASING:
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
				answer.setMessage(PlannerMessage.ORDER_HASTOBE_FINISHED);
				break;
			case COMPLETED:
			case FAILED:
				// job steps are completed/failed
				for (Long jobId : jobIds) {
					jobUtil.close(jobId);
				}
				transactionTemplate.setReadOnly(false);

				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						transactionTemplate.execute((status) -> {
							Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
							if (orderOpt.isPresent()) {
								ProcessingOrder locOrder = orderOpt.get();
								Duration retPeriod = locOrder.getMission().getOrderRetentionPeriod();
								if (retPeriod != null && locOrder.getProductionType() == ProductionType.SYSTEMATIC) {
									locOrder.setEvictionTime(Instant.now().plus(retPeriod));
								}
								if (locOrder.getOrderState() == OrderState.RUNNING) {
									locOrder.setOrderState(OrderState.FAILED);
								}
								locOrder.setOrderState(OrderState.CLOSED);
								if (locOrder.getHasFailedJobSteps()) {
									setStateMessage(locOrder, ProductionPlanner.STATE_MESSAGE_FAILED);
									UtilService.getOrderUtil().setOrderHistory(locOrder);
								} else {
									setStateMessage(locOrder, ProductionPlanner.STATE_MESSAGE_COMPLETED);
								}
								locOrder.incrementVersion();
								RepositoryService.getOrderRepository().save(locOrder);
								logOrderState(locOrder);
							}
							return null;
						});
						break;
					} catch (CannotAcquireLockException e) {
						if (logger.isDebugEnabled())
							logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled())
								logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					}
				}

				answer.setMessage(PlannerMessage.ORDER_CLOSED);
				break;
			case CLOSED:
				answer.setMessage(PlannerMessage.ORDER_CLOSED);
				break;
			default:
				break;
			}
			// don't log here, it is done by the caller
			// answer.setText(logger.log(answer, order.getIdentifier()));
		}
		return answer;
	}

	/**
	 * Check whether the processing order and it jobs and job steps are finished.
	 *
	 * @param orderId The processing order ID
	 * @return true after success
	 */
	public Boolean checkFinish(Long orderId) {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkFinish({})", orderId);

		boolean answer = false;
		boolean checkFurther = false;
		boolean hasChanged = false;
		ProcessingOrder order = null;
		Optional<ProcessingOrder> oOrder = RepositoryService.getOrderRepository().findById(orderId);
		if (oOrder.isPresent()) {
			order = oOrder.get();
		}
		// check current state for possibility to be suspended
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:
			case RELEASED:
			case SUSPENDING:
				boolean onHold = RepositoryService.getJobRepository().countJobOnHoldByProcessingOrderId(order.getId()) > 0;
				if (!onHold) {
					boolean all = RepositoryService.getJobRepository().countJobNotFinishedByProcessingOrderId(order.getId()) == 0;
					if (!all) {
						order.setOrderState(OrderState.PLANNED);
						setStateMessage(order, ProductionPlanner.STATE_MESSAGE_QUEUED);
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
						hasChanged = true;
						checkFurther = true;
						answer = true;
						break;
					}
				} else {
					break;
				}
			case RUNNING:
				boolean all = RepositoryService.getJobRepository().countJobNotFinishedByProcessingOrderId(order.getId()) == 0;
				if (all) {
					boolean completed = RepositoryService.getJobRepository().countJobFailedByProcessingOrderId(order.getId()) == 0;
					if (completed) {
						order.setOrderState(OrderState.COMPLETED);
						checkAutoClose(order);
						setTimes(order);
						setStateMessage(order, ProductionPlanner.STATE_MESSAGE_COMPLETED);
						UtilService.getOrderUtil().setOrderHistory(order);
					} else {
						order.setOrderState(OrderState.FAILED);
						setStateMessage(order, ProductionPlanner.STATE_MESSAGE_FAILED);
					}
					checkFurther = true;
					RepositoryService.getOrderRepository().save(order);
					UtilService.getOrderUtil().setOrderHistory(order);
					em.merge(order);
					hasChanged = true;
				}
				answer = true;
				break;
			case COMPLETED:
			case FAILED:
				checkFurther = true;
				answer = true;
				break;
			case CLOSED:
				checkAutoClose(order);
				answer = true;
				break;
			default:
				break;
			}
			if (checkFurther) {
				Boolean hasFailed = false;
				for (Job job : order.getJobs()) {
					if (job.getJobState() == JobState.FAILED) {
						hasFailed = true;
						break;
					}
				}
				if (order.getHasFailedJobSteps() != hasFailed) {
					order.setHasFailedJobSteps(hasFailed);
					RepositoryService.getOrderRepository().save(order);
					em.merge(order);
					hasChanged = true;
				}
				boolean allHasFinished = true;
				for (Job job : order.getJobs()) {
					if (job.getJobState() != JobState.FAILED && job.getJobState() != JobState.COMPLETED) {
						allHasFinished = false;
						break;
					}
				}
				if (allHasFinished) {
					if (hasFailed) {
						order.setOrderState(OrderState.FAILED);
						setStateMessage(order, ProductionPlanner.STATE_MESSAGE_FAILED);
						RepositoryService.getOrderRepository().save(order);
						UtilService.getOrderUtil().setOrderHistory(order);
						em.merge(order);
					} else {
						order.setOrderState(OrderState.COMPLETED);
						setTimes(order);
						setStateMessage(order, ProductionPlanner.STATE_MESSAGE_COMPLETED);
						checkAutoClose(order);
						UtilService.getOrderUtil().setOrderHistory(order);
						sendNotification(order);
					}
					hasChanged = true;
				}
			}
			if (hasChanged) {
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				em.merge(order);
				logOrderState(order);
			}
		}
		return answer;
	}

	/**
	 * If the order is in systematic processing and the mission has an order retention period, the order is automatically closed
	 * after completion and the eviction time is set.
	 *
	 * @param order The processing order
	 * @return true if closed
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public Boolean checkAutoClose(ProcessingOrder order) {
		Duration retPeriod = order.getMission().getOrderRetentionPeriod();
		if (retPeriod != null && order.getProductionType() == ProductionType.SYSTEMATIC) {
			order.setEvictionTime(Instant.now().plus(retPeriod));
			for (Job job : order.getJobs()) {
				if (job.getJobState() == JobState.COMPLETED) {
					jobUtil.close(job.getId());
				}
			}
			if (order.getOrderState() == OrderState.COMPLETED) {
				order.setOrderState(OrderState.CLOSED);
			}
			return true;
		}
		return false;
	}

	/**
	 * Set the actual completion time and the estimated completion time if it is null.
	 *
	 * @param order The processing order
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void setTimes(ProcessingOrder order) {
		if (order != null) {
			Instant timeNow = Instant.now();
			order.setActualCompletionTime(timeNow);
			if (order.getEstimatedCompletionTime() == null) {
				order.setEstimatedCompletionTime(timeNow);
			}
		}
	}

	/**
	 * Set the state message of the order.
	 *
	 * @param order The processing order
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void setStateMessage(ProcessingOrder order, String stateMessage) {
		if (order != null && stateMessage != null) {
			order.setStateMessage(stateMessage);
		}
	}

	/**
	 * Set the order state and time of an order history object.
	 * The time setting depends on the current order state.
	 *  
	 * @param order The processing order
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void setOrderHistory(ProcessingOrder order) {
		if (order != null) {
			ProcessingOrderHistory history = RepositoryService.getProcessingOrderHistoryRepository()
					.findByMissionCodeAndIdentifier(order.getMission().getCode(), order.getIdentifier());
			if (history != null) {
				history.setOrderState(order.getOrderState());
				switch (order.getOrderState()) {
				case RELEASED:
				case RUNNING:
					if (history.getReleaseTime() == null) {
						history.setReleaseTime(Instant.now());
					}
					break;
				case FAILED:
				case COMPLETED:
					if (history.getCompletionTime() == null) {
						history.setCompletionTime(Instant.now());
					}
					break;
				default:
					break;
				}
				RepositoryService.getProcessingOrderHistoryRepository().save(history);
			}
		}
	}

	/**
	 * Set the deletion time of an order history object.
	 *  
	 * @param order The processing order
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void setOrderHistoryOrderDeleted(ProcessingOrder order) {
		if (order != null) {
			ProcessingOrderHistory history = RepositoryService.getProcessingOrderHistoryRepository()
					.findByMissionCodeAndIdentifier(order.getMission().getCode(), order.getIdentifier());
			if (history != null) {
				history.setDeletionTime(Instant.now());
				RepositoryService.getProcessingOrderHistoryRepository().save(history);
			}
		}
	}
	
	/**
	 * Sends a notification for the given processing order.
	 *
	 * This method sends a notification to the specified endpoint regarding the processing order. It constructs a message containing
	 * relevant information such as product details, order ID, and notification date, and then sends it to the configured
	 * notification service endpoint.
	 *
	 * @param order The processing order for which the notification is to be sent.
	 * @return true if the notification is successfully sent; false otherwise.
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public Boolean sendNotification(ProcessingOrder order) {
		if (logger.isTraceEnabled())
			logger.trace(">>> sendNotification({})", order.getIdentifier());
		if (order.getNotificationEndpoint() != null) {
			switch (order.getOrderSource()) {
			case ODIP:
				if (logger.isTraceEnabled())
					logger.trace(">>> sendNotification({})", OrderSource.ODIP);
				// create the message content as String
				// first check whether product is generated,
				// use output product of first job step
				JobStep jobStep = null;
				if (!order.getJobs().isEmpty()) {
					Job firstJob = null;
					for (Job job : order.getJobs()) {
						firstJob = job;
						break;
					}
					if (!firstJob.getJobSteps().isEmpty()) {
						for (JobStep js : firstJob.getJobSteps()) {
							jobStep = js;
							break;
						}
					}
				}
				if (jobStep != null && jobStep.getOutputProduct() != null && jobStep.getOutputProduct().getProductFile() != null
						&& jobStep.getOutputProduct().getProductFile() != null) {
					String fileName = null;
					Product product = jobStep.getOutputProduct();
					fileName = getFirstFileName(product);
					if (fileName != null) {
						String message = String.join("\n", "POST", order.getNotificationEndpoint().getUri(), "{",
								"    \"@odata.context\": \"$metadata#Notification/$entity\",",
								"    \"ProductId\": \"" + product.getUuid() + "\",", "    \"ProductName\": \"" + fileName + "\",",
								"    \"ProductionOrderId\": \"" + order.getUuid() + "\",",
								"    \"NotificationDate\": \"" + Date.from(Instant.now()) + "\",", "}", "");

						// Skip if notification service is not configured
						if (config.getNotificationUrl().isBlank()) {
							// TODO log error
							return false;
						}
						RestMessage restMessage = new RestMessage();
						restMessage.setEndpoint(order.getNotificationEndpoint().getUri());
						restMessage.setUser(order.getNotificationEndpoint().getUsername());
						restMessage.setPassword(order.getNotificationEndpoint().getPassword());
						restMessage.setMessage(message);
						restMessage.setRaw(true);
						restMessage.setContentType(MediaType.APPLICATION_JSON);
						restMessage.setSender(order.getOrderSource().toString());
						String url = config.getNotificationUrl() + "/notify";
						RestTemplate restTemplate = new RestTemplate();
						ResponseEntity<String> response = null;
						try {
							if (logger.isTraceEnabled())
								logger.trace(">>> notify({}, {})", url, restMessage);
							response = restTemplate.postForEntity(url, restMessage, String.class);
						} catch (RestClientException rce) {
							logger.log(PlannerMessage.NOTIFY_FAILED, url, rce.getMessage());
							return false;
						} catch (Exception e) {
							String msg = logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getMessage());

							if (logger.isDebugEnabled())
								logger.debug("... exception stack trace: ", msg);

							return false;
						}
						if (!(HttpStatus.OK.equals(response.getStatusCode())
								|| (HttpStatus.CREATED.equals(response.getStatusCode())))) {
							logger.log(PlannerMessage.NOTIFY_FAILED, url, response.getStatusCode().toString());
							return false;
						}
					}
				}
				return true;
			default:
				// do nothing
				break;
			}
		}
		return false;
	}

	/**
	 * Get the processing facility(-ies) processing the order At the moment there is normally only one facility to do so.
	 *
	 * @param id The processing order ID
	 * @return List of processinig facilities
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public List<ProcessingFacility> getProcessingFacilities(long id) {
		ProcessingOrder order = null;
		Optional<ProcessingOrder> oOrder = RepositoryService.getOrderRepository().findById(id);
		if (oOrder.isPresent()) {
			order = oOrder.get();
		}
		if (logger.isTraceEnabled())
			logger.trace(">>> getProcessingFacilities({})", (null == order ? "null" : order.getId()));

		List<ProcessingFacility> processingFacilities = new ArrayList<>();
		if (order != null) {
			for (Job job : order.getJobs()) {
				if (!processingFacilities.contains(job.getProcessingFacility())) {
					processingFacilities.add(job.getProcessingFacility());
				}
			}
		}
		return processingFacilities;
	}

	/**
	 * Set the failed job steps flag in the order on failure
	 *
	 * @param order  the order to update
	 * @param failed indicates whether a job step has failed
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void setHasFailedJobSteps(ProcessingOrder order, Boolean failed) {
		if (logger.isTraceEnabled())
			logger.trace(">>> setHasFailedJobSteps({}, {})", (null == order ? "null" : order.getId()), failed);

		if (failed && !order.getHasFailedJobSteps()) {
			order.setHasFailedJobSteps(failed);
			order.incrementVersion();
			RepositoryService.getOrderRepository().save(order);
			em.merge(order);
		}
	}

	/**
	 * Log the order state (currently a dummy method)
	 * 
	 * @param order The order for which to log the state
	 */
	public void logOrderState(ProcessingOrder order) {
		// at the moment a dummy
	}

	/**
	 * Look for next order in state PLANNING, RELEASING and SUSPENDING to restart.
	 */
	public void checkNextForRestart() {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkNextForRestart()");
		long id = 0;
		if (productionPlanner.getSuspendingOrders().size() > 0) {
			id = productionPlanner.getSuspendingOrders().get(0);
			productionPlanner.getSuspendingOrders().remove(0);
			if (logger.isTraceEnabled())
				logger.trace(">>> suspend order ({})", id);
			restartSuspendingOrder(id);
			return;
		}
		if (productionPlanner.getReleasingOrders().size() > 0) {
			id = productionPlanner.getReleasingOrders().get(0);
			productionPlanner.getReleasingOrders().remove(0);
			if (logger.isTraceEnabled())
				logger.trace(">>> release order ({})", id);
			restartReleasingOrder(id);
			return;
		}
		if (productionPlanner.getPlanningOrders().size() > 0) {
			id = productionPlanner.getPlanningOrders().get(0);
			productionPlanner.getPlanningOrders().remove(0);
			if (logger.isTraceEnabled())
				logger.trace(">>> plan order ({})", id);
			restartPlanningOrder(id);
			return;
		}
	}

	/**
	 * Restart the thread corresponding to plan the order with the given ID.
	 * 
	 * @param order The order ID
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	private void restartPlanningOrder(long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> restartPlanningOrder({})", id);

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		// Find the processing facility
		final Long facilityId = transactionTemplate.execute((status) -> {
			Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
			if (orderOpt.isPresent()) {
				Set<Job> jobs = orderOpt.get().getJobs();
				for (Job job : jobs) {
					if (job.getProcessingFacility() != null) {
						return job.getProcessingFacility().getId();
					}
				}
			}
			return null;
		});
		if (facilityId != null) {
			UtilService.getOrderUtil().plan(id, facilityId, false);
		} else {
			// no job with processing facility exist, can't plan
			final ProcessingOrder order = transactionTemplate.execute((status) -> {
				Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
				if (orderOpt.isPresent()) {
					return orderOpt.get();
				}
				return null;
			});
			if (order != null) {
				UtilService.getOrderUtil().reset(order);
			}
		}
	}

	/**
	 * Restart the thread corresponding to release the order with the given ID.
	 * 
	 * @param id The order ID
	 */
	public void restartReleasingOrder(long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> restartReleasingOrder({})", id);

		Optional<ProcessingOrder> order = RepositoryService.getOrderRepository().findById(id);
		if (order.isPresent()) {
			// resume the order
			UtilService.getOrderUtil().resume(order.get(), false, "", "");
		}
	}

	/**
	 * Restart the thread corresponding to suspend the order with the given ID.
	 * 
	 * @param id The order ID
	 */
	public void restartSuspendingOrder(long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> restartSuspendingOrder({})", id);

		UtilService.getOrderUtil().suspend(id, true);
	}

	/**
	 * Retrieves the file name of the first product file in the given product's file list or in its component products recursively.
	 *
	 * @param product The product whose file name needs to be retrieved.
	 * @return The file name of the first product file found, or null if not found.
	 */
	private String getFirstFileName(Product product) {
		String fileName = null;
		for (ProductFile pf : product.getProductFile()) {
			fileName = pf.getProductFileName();
			if (fileName != null) {
				return fileName;
			}
		}
		for (Product p : product.getComponentProducts()) {
			fileName = getFirstFileName(p);
			if (fileName != null) {
				return fileName;
			}
		}
		return fileName;
	}
}
