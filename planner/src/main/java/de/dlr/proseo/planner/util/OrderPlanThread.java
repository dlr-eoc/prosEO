/**
 * OrderPlanThread.java
 * 
 * @author Ernst Melchinger
 * © 2019 Prophos Informatik GmbH
 */

package de.dlr.proseo.planner.util;


import java.util.Optional;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.planner.PlannerResultMessage;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.OrderDispatcher;


/**
 * Thread to plan a processing order
 *
 */
public class OrderPlanThread extends Thread {

	/**
	 * Logger of this class
	 */
	private static ProseoLogger logger = new ProseoLogger(OrderPlanThread.class);
    
	/**
	 * The order dispatcher 
	 */
	private OrderDispatcher orderDispatcher;

	/** The Production Planner instance */
    private ProductionPlanner productionPlanner;
	
	/**
	 * The processing order to plan.
	 */
	private long orderId;
	
	/**
	 * The facility to process the order
	 */
	private ProcessingFacility procFacility;
	
	/**
	 * The result of the planning
	 */
	private PlannerResultMessage resultMessage;
	
	/**
	 * @return the resultMessage
	 */
	public PlannerResultMessage getResultMessage() {
		return resultMessage;
	}

	/**
	 * Create new thread
	 * 
	 * @param productionPlanner The production planner instance
	 * @param orderDispatcher Ther order dispatcher
	 * @param order The processing order to plan
	 * @param procFacility The processing facility to run the order
	 * @param name The thread name
	 */
	public OrderPlanThread(ProductionPlanner productionPlanner, OrderDispatcher orderDispatcher, long orderId,  ProcessingFacility procFacility, String name) {
		super(name);
		this.productionPlanner = productionPlanner;
		this.orderDispatcher = orderDispatcher;
		this.orderId = orderId;
		this.procFacility = procFacility;
	}	

    /**
     * Start and initialize the thread
     */
    public void run() {
		if (logger.isTraceEnabled()) logger.trace(">>> run() for thread {}", this.getName());
		
		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		
		if (0 == orderId || null == productionPlanner || null == orderDispatcher) {
			logger.log(answer.getMessage());
		} else {
			// Create the required jobs for the order (independent transaction)
			try {
				answer = orderDispatcher.prepareExpectedJobs(orderId, procFacility, this);
			} catch(InterruptedException e) {
				answer.setMessage(PlannerMessage.PLANNING_INTERRUPTED);
				answer.setText(logger.log(answer.getMessage(), orderId));
			}
			
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
			
			try {
				if (answer.getSuccess()) {
					answer = plan(orderId);
				}
			} catch(InterruptedException e) {
				answer.setMessage(PlannerMessage.PLANNING_INTERRUPTED);
				answer.setText(logger.log(answer.getMessage(), orderId));
			} catch(Exception e) {
				// Set order state to failed		
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						transactionTemplate.execute((status) -> {
							Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
							if (orderOpt.isPresent()) {
								ProcessingOrder lambdaOrder = orderOpt.get();
								lambdaOrder.setOrderState(OrderState.PLANNING_FAILED);
								lambdaOrder.setStateMessage(ProductionPlanner.STATE_MESSAGE_FAILED);
								lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
							}
							return null;
						});
						break;
					} catch (CannotAcquireLockException e1) {
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e1);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait(i + 1);
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e1;
						}
					}
				}

				answer.setText(logger.log(PlannerMessage.ORDER_PLANNING_EXCEPTION, this.getName(), orderId));
				answer.setMessage(GeneralMessage.EXCEPTION_ENCOUNTERED);
				answer.setText(logger.log(answer.getMessage(), e.getMessage()));
				
				if (logger.isDebugEnabled()) logger.debug("Exception stack trace: ", e);
			}
			try {
				if (!answer.getSuccess()) {
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							transactionTemplate.execute((status) -> {
								ProcessingOrder lambdaOrder = null;
								Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
								if (orderOpt.isPresent()) {
									lambdaOrder = orderOpt.get();
								}
								lambdaOrder.setOrderState(OrderState.PLANNING_FAILED);
								UtilService.getOrderUtil().setStateMessage(lambdaOrder, ProductionPlanner.STATE_MESSAGE_FAILED);
								lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
								return null;
							});
							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait(i + 1);
							} else {
								if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}
				}
			}
			catch(Exception e) {
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						transactionTemplate.execute((status) -> {
							ProcessingOrder lambdaOrder = null;
							Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
							if (orderOpt.isPresent()) {
								lambdaOrder = orderOpt.get();
							}
							lambdaOrder.setOrderState(OrderState.PLANNING_FAILED);
							UtilService.getOrderUtil().setStateMessage(lambdaOrder, ProductionPlanner.STATE_MESSAGE_FAILED);
							lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
							return null;
						});
						break;
					} catch (CannotAcquireLockException e1) {
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e1);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait(i + 1);
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e1;
						}
					}
				}

				answer.setText(logger.log(PlannerMessage.ORDER_PLANNING_EXCEPTION, this.getName(), orderId));
				answer.setMessage(GeneralMessage.EXCEPTION_ENCOUNTERED);
				answer.setText(logger.log(answer.getMessage(), e.getMessage()));
			}
		}
		this.resultMessage = answer;
		productionPlanner.getPlanThreads().remove(this.getName());
		
		if (logger.isTraceEnabled()) logger.trace("<<< run() for thread {}", this.getName());
		
		productionPlanner.checkNextForRestart();				
    }
    
    
    /**
     * Plan the order 
     * 
     * @param orderId The id of the order
     * @return The result message
     * @throws InterruptedException
     */
    public PlannerResultMessage plan(long orderId) throws InterruptedException {
		if (logger.isTraceEnabled()) logger.trace(">>> plan({})", orderId);
		
		ProcessingOrder order = null;

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		order = transactionTemplate.execute((status) -> {
			Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
			if (orderOpt.isPresent()) {
				return orderOpt.get();
			}
			return null;
		});
		
		if (logger.isTraceEnabled()) logger.trace("... planning job steps for order {} on facility {})",
				(null == order ? "null" : order.getId()),
				(null == procFacility ? "null" : procFacility.getName()));
		
		PlannerResultMessage answer = new PlannerResultMessage(GeneralMessage.FALSE);
		PlannerResultMessage publishAnswer = new PlannerResultMessage(GeneralMessage.FALSE);
		
		if (order != null && procFacility != null && 
				(order.getOrderState() == OrderState.PLANNING || order.getOrderState() == OrderState.APPROVED)) {
			try {
				publishAnswer = orderDispatcher.createJobSteps(order.getId(), procFacility, productionPlanner, this);
			} catch (InterruptedException e) {
				throw e;
			}
			
			try {
				final PlannerResultMessage finalAnswer = new PlannerResultMessage(publishAnswer.getMessage());
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						answer = transactionTemplate.execute((status) -> {
							ProcessingOrder lambdaOrder = null;
							Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
							if (orderOpt.isPresent()) {
								lambdaOrder = orderOpt.get();
							}
							PlannerResultMessage lambdaAnswer = new PlannerResultMessage(GeneralMessage.FALSE);
							if (finalAnswer.getSuccess()) {
								if (lambdaOrder.getJobs().isEmpty()) {
									lambdaOrder.setOrderState(OrderState.COMPLETED);
									UtilService.getOrderUtil().checkAutoClose(lambdaOrder);
									UtilService.getOrderUtil().setTimes(lambdaOrder);
									UtilService.getOrderUtil().setStateMessage(lambdaOrder, ProductionPlanner.STATE_MESSAGE_COMPLETED);
									lambdaAnswer.setMessage(PlannerMessage.ORDER_PRODUCT_EXIST);
								} else {
									lambdaOrder.setOrderState(OrderState.PLANNED);
									UtilService.getOrderUtil().setStateMessage(lambdaOrder, ProductionPlanner.STATE_MESSAGE_QUEUED);
									lambdaAnswer.setMessage(PlannerMessage.ORDER_PLANNED);
								}
								lambdaAnswer.setText(logger.log(lambdaAnswer.getMessage(), lambdaOrder.getIdentifier()));
								lambdaOrder.incrementVersion();
								lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
							} else {
								lambdaOrder.setOrderState(OrderState.PLANNING_FAILED);
								UtilService.getOrderUtil().setStateMessage(lambdaOrder, ProductionPlanner.STATE_MESSAGE_FAILED);
								lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
								lambdaAnswer.setMessage(PlannerMessage.ORDER_PLANNING_FAILED);
								lambdaAnswer.setText(logger.log(lambdaAnswer.getMessage(), lambdaOrder.getIdentifier(), this.getName()));
							}
							return lambdaAnswer;
						});
						break;
					} catch (CannotAcquireLockException e) {
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait(i + 1);
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					}
				}

			} catch (Exception e) {
				answer.setMessage(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED);
				answer.setText(logger.log(answer.getMessage(),  e.getMessage()));
				
				if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
			}
		}
		
		return answer;
	}
}
