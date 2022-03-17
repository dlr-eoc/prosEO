/**
 * OrderPlanThread.java
 * 
 * @author Ernst Melchinger
 * © 2019 Prophos Informatik GmbH
 */

package de.dlr.proseo.planner.util;


import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Message;
import de.dlr.proseo.planner.Messages;
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
	private static Logger logger = LoggerFactory.getLogger(OrderPlanThread.class);
    
	/**
	 * The order dispatcher 
	 */
	private OrderDispatcher orderDispatcher;

	/** The Production Planner instance */
    private ProductionPlanner productionPlanner;
	
	/**
	 * The processing order to plan.
	 */
	private ProcessingOrder order;
	
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
	public OrderPlanThread(ProductionPlanner productionPlanner, OrderDispatcher orderDispatcher, ProcessingOrder order,  ProcessingFacility procFacility, String name) {
		super(name);
		this.productionPlanner = productionPlanner;
		this.orderDispatcher = orderDispatcher;
		this.order = order;
		this.procFacility = procFacility;
	}	

    /**
     * Start and initialize the thread
     */
    public void run() {
		if (logger.isTraceEnabled()) logger.trace(">>> run({})", this.getName());
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		
		Message answer = new Message(Messages.FALSE);
		if (order != null && productionPlanner != null && orderDispatcher != null) {
			final long orderId = order.getId();
			try {
				answer = orderDispatcher.prepareExpectedJobs(orderId, procFacility, this);
				if (answer.isTrue()) {
					answer = plan(order.getId());
				}
				if (!answer.isTrue()) {
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
				}
			}
			catch(InterruptedException e) {
				// do nothing, message already logged
			} 
			catch(Exception e) {
				Messages.ORDER_PLANNING_EXCEPTION.format(this.getName(), order.getIdentifier());
				logger.error(e.getMessage());
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
			}
		}
		productionPlanner.getPlanThreads().remove(this.getName());
		if (logger.isTraceEnabled()) logger.trace("<<< run({})", this.getName());
    }
    
    
    /**
     * Plan the order 
     * 
     * @param orderId The id of the order
     * @return The result message
     * @throws InterruptedException
     */
    public Message plan(long orderId) throws InterruptedException {
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
		Message answer = new Message(Messages.FALSE);
		Message publishAnswer = new Message(Messages.FALSE);
		if (order != null && procFacility != null && 
				(order.getOrderState() == OrderState.PLANNING || order.getOrderState() == OrderState.APPROVED)) {
			try {
				publishAnswer = orderDispatcher.createJobSteps(order.getId(), procFacility, productionPlanner, this);
			} catch (InterruptedException e) {
				throw e;
			} finally {
				final Message finalAnswer = publishAnswer;
				answer = transactionTemplate.execute((status) -> {
					ProcessingOrder lambdaOrder = null;
					Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
					if (orderOpt.isPresent()) {
						lambdaOrder = orderOpt.get();
					}
					Message lambdaAnswer = new Message(Messages.FALSE);
					if (finalAnswer.isTrue()) {
						if (lambdaOrder.getJobs().isEmpty()) {
							lambdaOrder.setOrderState(OrderState.COMPLETED);
							lambdaAnswer = new Message(Messages.ORDER_PRODUCT_EXIST);
						} else {
							lambdaOrder.setOrderState(OrderState.PLANNED);
							lambdaAnswer = new Message(Messages.ORDER_PLANNED);
						}
						lambdaAnswer.log(logger, lambdaOrder.getIdentifier());
						lambdaOrder.incrementVersion();
						lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
					} else {
						lambdaOrder.setOrderState(OrderState.PLANNING_FAILED);
						lambdaAnswer = new Message(Messages.ORDER_PLANNING_FAILED);
						lambdaAnswer.log(logger, lambdaOrder.getIdentifier(), this.getName());
					}
					return lambdaAnswer;
				});
			}
		}
		
		return answer;
	}
}