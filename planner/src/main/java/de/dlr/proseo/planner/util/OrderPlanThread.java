/**
 * OrderPlanThread.java
 * 
 * @author Ernst Melchinger
 * © 2019 Prophos Informatik GmbH
 */

package de.dlr.proseo.planner.util;


import java.util.Optional;

import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.logging.messages.ProseoMessage;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
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
		if (logger.isTraceEnabled()) logger.trace(">>> run({})", this.getName());
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		
		ProseoMessage answer = GeneralMessage.FALSE;
		if (orderId != 0 && productionPlanner != null && orderDispatcher != null) {
			try {
				productionPlanner.acquireThreadSemaphore("OrderPlanThread.run");
				answer = orderDispatcher.prepareExpectedJobs(orderId, procFacility, this);
				productionPlanner.releaseThreadSemaphore("OrderPlanThread.run");
			} catch(InterruptedException e) {
				productionPlanner.releaseThreadSemaphore("OrderPlanThread.run");
			} 
			try {
				if (answer.getSuccess()) {
					answer = plan(orderId);
				}
			} catch(InterruptedException e) {
			} catch(Exception e) {
				@SuppressWarnings("unused")
				Object dummy = transactionTemplate.execute((status) -> {
					ProcessingOrder lambdaOrder = null;
					Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
					if (orderOpt.isPresent()) {
						lambdaOrder = orderOpt.get();
					}
					logger.log(PlannerMessage.ORDER_PLANNING_EXCEPTION, this.getName(), lambdaOrder.getIdentifier());
					logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getMessage());
					lambdaOrder.setOrderState(OrderState.PLANNING_FAILED);
					lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
					return null;
				});
			}
			try {
				if (!answer.getSuccess()) {
					productionPlanner.acquireThreadSemaphore("OrderPlanThread.run");
					@SuppressWarnings("unused")
					Object dummy = transactionTemplate.execute((status) -> {
						ProcessingOrder lambdaOrder = null;
						Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
						if (orderOpt.isPresent()) {
							lambdaOrder = orderOpt.get();
						}
						lambdaOrder.setOrderState(OrderState.PLANNING_FAILED);
						lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
						return null;
					});
					productionPlanner.releaseThreadSemaphore("OrderPlanThread.run");
				}
			}
			catch(Exception e) {
				productionPlanner.releaseThreadSemaphore("OrderPlanThread.run");
				@SuppressWarnings("unused")
				Object dummy = transactionTemplate.execute((status) -> {
					ProcessingOrder lambdaOrder = null;
					Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
					if (orderOpt.isPresent()) {
						lambdaOrder = orderOpt.get();
					}
					logger.log(PlannerMessage.ORDER_PLANNING_EXCEPTION, this.getName(), lambdaOrder.getIdentifier());
					logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getMessage());
					lambdaOrder.setOrderState(OrderState.PLANNING_FAILED);
					lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
					return null;
				});
			}
		}
		productionPlanner.getPlanThreads().remove(this.getName());
		if (logger.isTraceEnabled()) logger.trace("<<< run({})", this.getName());
		productionPlanner.checkNextForRestart();				
    }
    
    
    /**
     * Plan the order 
     * 
     * @param orderId The id of the order
     * @return The result message
     * @throws InterruptedException
     */
    public ProseoMessage plan(long orderId) throws InterruptedException {
		ProcessingOrder order = null;

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());

		order = transactionTemplate.execute((status) -> {
			Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
			if (orderOpt.isPresent()) {
				return orderOpt.get();
			}
			return null;
		});
		if (logger.isTraceEnabled()) logger.trace(">>> plan({}, {})", (null == order ? "null" : order.getId()),
				(null == procFacility ? "null" : procFacility.getName()));
		ProseoMessage answer = GeneralMessage.FALSE;
		ProseoMessage publishAnswer = GeneralMessage.FALSE;
		if (order != null && procFacility != null && 
				(order.getOrderState() == OrderState.PLANNING || order.getOrderState() == OrderState.APPROVED)) {
			try {
				publishAnswer = orderDispatcher.createJobSteps(order.getId(), procFacility, productionPlanner, this);
			} catch (InterruptedException e) {
				throw e;
			}
			try {
				productionPlanner.acquireThreadSemaphore("OrderPlanThread.plan");
				final ProseoMessage finalAnswer = publishAnswer;
				answer = transactionTemplate.execute((status) -> {
					ProcessingOrder lambdaOrder = null;
					Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
					if (orderOpt.isPresent()) {
						lambdaOrder = orderOpt.get();
					}
					ProseoMessage lambdaAnswer = GeneralMessage.FALSE;
					if (finalAnswer.getSuccess()) {
						if (lambdaOrder.getJobs().isEmpty()) {
							lambdaOrder.setOrderState(OrderState.COMPLETED);
							UtilService.getOrderUtil().checkAutoClose(lambdaOrder);
							lambdaAnswer = PlannerMessage.ORDER_PRODUCT_EXIST;
						} else {
							lambdaOrder.setOrderState(OrderState.PLANNED);
							lambdaAnswer = PlannerMessage.ORDER_PLANNED;
						}
						logger.log(lambdaAnswer, lambdaOrder.getIdentifier());
						lambdaOrder.incrementVersion();
						lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
					} else {
						lambdaOrder.setOrderState(OrderState.PLANNING_FAILED);
						lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
						lambdaAnswer = PlannerMessage.ORDER_PLANNING_FAILED;
						logger.log(lambdaAnswer, lambdaOrder.getIdentifier(), this.getName());
					}
					return lambdaAnswer;
				});
			} catch (Exception e) {
				answer = GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED;
				logger.log(answer,  e.getMessage());
			} finally {
				productionPlanner.releaseThreadSemaphore("OrderPlanThread.plan");
			}
		}
		
		return answer;
	}
}
