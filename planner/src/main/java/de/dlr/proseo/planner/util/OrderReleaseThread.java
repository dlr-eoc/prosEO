/**
 * OrderReleaseThread.java
 * 
 * @author Ernst Melchinger
 * © 2019 Prophos Informatik GmbH
 */

package de.dlr.proseo.planner.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Message;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;

/**
 * The thread to release a processing order
 *
 */
public class OrderReleaseThread extends Thread {

	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(OrderReleaseThread.class);
    
	/**
	 * The job utility instance 
	 */
	private JobUtil jobUtil;

	/** The Production Planner instance */
    private ProductionPlanner productionPlanner;
	
	/**
	 * The processing order to plan.
	 */
	private ProcessingOrder order;

	/**
	 * Create new thread
	 * 
	 * @param productionPlanner The production planner instance
	 * @param jobUtil The job utility instance
	 * @param order The processing order to plan
	 * @param name The thread name
	 */
	public OrderReleaseThread(ProductionPlanner productionPlanner, JobUtil jobUtil, ProcessingOrder order, String name) {
		super(name);
		this.productionPlanner = productionPlanner;
		this.jobUtil = jobUtil;
		this.order = order;
	}
	
    /**
     * Start and initialize the release thread
     */
    public void run() {
		if (logger.isTraceEnabled()) logger.trace(">>> run({})", this.getName());

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		
		Message answer = new Message(Messages.FALSE);
		if (order != null && productionPlanner != null && jobUtil != null) {
			answer = new Message(Messages.TRUE);
			final long orderId = order.getId();
			try {
				if (answer.isTrue()) {
					answer = release(order.getId());
				}
				if (!answer.isTrue()) {
					// nothing to do here, order state already set in release
				}
			}
			catch(InterruptedException e) {
				Messages.ORDER_RELEASING_INTERRUPTED.format(this.getName(), order.getIdentifier());
			} 
			catch(Exception e) {
				Messages.ORDER_RELEASING_EXCEPTION.format(this.getName(), order.getIdentifier());
				logger.error(e.getMessage());
				@SuppressWarnings("unused")
				Object dummy = transactionTemplate.execute((status) -> {
					ProcessingOrder lambdaOrder = null;
					Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
					if (orderOpt.isPresent()) {
						lambdaOrder = orderOpt.get();
					}
					lambdaOrder.setOrderState(OrderState.PLANNED);
					lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
					return null;
				});
			}
		}
		productionPlanner.getReleaseThreads().remove(this.getName());
		if (logger.isTraceEnabled()) logger.trace("<<< run({})", this.getName());
    }
    
    
    /**
     * Release the processing order 
     * 
     * @param orderId The id of the order
     * @return The result message
     * @throws InterruptedException
     */
    public Message release(long orderId) throws InterruptedException {
		ProcessingOrder order = null;

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		final List<Job> jobList = new ArrayList<Job>();
		order = transactionTemplate.execute((status) -> {
			Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
			if (orderOpt.isPresent()) {
				jobList.addAll(orderOpt.get().getJobs());
				return orderOpt.get();
			}
			return null;
		});
		if (logger.isTraceEnabled()) logger.trace(">>> release({})", (null == order ? "null" : order.getId()));
		Message answer = new Message(Messages.FALSE);
		Messages releaseAnswer = Messages.FALSE;
		if (order != null && 
				(order.getOrderState() == OrderState.RELEASING || order.getOrderState() == OrderState.PLANNED)) {
			for (Job job : jobList) {
				if (this.isInterrupted()) {
					answer = new Message(Messages.ORDER_RELEASING_INTERRUPTED);
					logger.warn(Messages.ORDER_RELEASING_INTERRUPTED.format(order.getIdentifier()));
					throw new InterruptedException();
				}
				releaseAnswer = jobUtil.resume(job.getId());
				if (!releaseAnswer.isTrue()) {
					// nothing to do here
				}
				if (this.isInterrupted()) {
					answer = new Message(Messages.ORDER_RELEASING_INTERRUPTED);
					logger.warn(Messages.ORDER_RELEASING_INTERRUPTED.format(order.getIdentifier()));
					throw new InterruptedException();
				}
				// look for possible job steps to run
				final KubeConfig kc = transactionTemplate.execute((status) -> {
					Optional<Job> jobOpt = RepositoryService.getJobRepository().findById(job.getId());
					if (jobOpt.get() != null) {
						return productionPlanner.getKubeConfig(jobOpt.get().getProcessingFacility().getName());
					}
					return null;
				});
				UtilService.getJobStepUtil().checkJobToRun(kc, job.getId());						
				
			}
			final Messages finalAnswer = releaseAnswer;
			answer = transactionTemplate.execute((status) -> {
				ProcessingOrder lambdaOrder = null;
				Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
				if (orderOpt.isPresent()) {
					lambdaOrder = orderOpt.get();
				}
				Message lambdaAnswer = new Message(Messages.FALSE);
				if (finalAnswer.isTrue() || finalAnswer.getCode() == Messages.JOB_ALREADY_COMPLETED.getCode()) {
					if (lambdaOrder.getJobs().isEmpty()) {
						lambdaOrder.setOrderState(OrderState.COMPLETED);
						lambdaAnswer = new Message(Messages.ORDER_PRODUCT_EXIST);
					} else {
						// check whether order is already running
						Boolean running = false;
						for (Job j : lambdaOrder.getJobs()) {
							if (RepositoryService.getJobStepRepository().countJobStepRunningByJobId(j.getId()) > 0) {
								running = true;
								break;
							}
						}
						lambdaOrder.setOrderState(OrderState.RELEASED);
						if (running) {
							lambdaOrder.setOrderState(OrderState.RUNNING);
						} 
						lambdaAnswer = new Message(Messages.ORDER_RELEASED);
					}
					lambdaAnswer.log(logger, lambdaOrder.getIdentifier());
					lambdaOrder.incrementVersion();
					lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
				} else {
					// the order is also released
					Boolean running = false;
					for (Job j : lambdaOrder.getJobs()) {
						if (RepositoryService.getJobStepRepository().countJobStepRunningByJobId(j.getId()) > 0) {
							running = true;
							break;
						}
					}
					lambdaOrder.setOrderState(OrderState.RELEASED);
					if (running) {
						lambdaOrder.setOrderState(OrderState.RUNNING);
					}
					lambdaAnswer = new Message(Messages.ORDER_RELEASED);
					lambdaAnswer.log(logger, lambdaOrder.getIdentifier(), this.getName());
					lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
				}
				return lambdaAnswer;
			});
		}

		return answer;
	}
	
}
