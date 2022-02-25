package de.dlr.proseo.planner.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Message;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.OrderDispatcher;
import de.dlr.proseo.planner.kubernetes.KubeConfig;

public class OrderReleaseThread extends Thread {

	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(OrderReleaseThread.class);

	@Autowired
	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
    
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
	 * @param order The processing order to plan
	 * @param name The thread name
	 */
	public OrderReleaseThread(ProductionPlanner productionPlanner, JobUtil jobUtil, ProcessingOrder order, String name) {
		super(name);
		this.productionPlanner = productionPlanner;
		this.jobUtil = jobUtil;
		this.order = order;
	}
	

    public void run() {
		if (logger.isTraceEnabled()) logger.trace(">>> run({})", this.getName());
		// TODO manage thread map in planner

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
					// ...
				}
				if (this.isInterrupted()) {
					answer = new Message(Messages.ORDER_RELEASING_INTERRUPTED);
					logger.warn(Messages.ORDER_RELEASING_INTERRUPTED.format(order.getIdentifier()));
					throw new InterruptedException();
				}
				// look for possible job steps to run
				String dummy = transactionTemplate.execute((status) -> {
					Optional<Job> jobOpt = RepositoryService.getJobRepository().findById(job.getId());
					if (jobOpt.get() != null) {
						KubeConfig kc = productionPlanner.getKubeConfig(jobOpt.get().getProcessingFacility().getName());
						UtilService.getJobStepUtil().checkJobToRun(kc, jobOpt.get());						
					}
					return null;
				});
				
			}
			final Messages finalAnswer = releaseAnswer;
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
						// TODO check whether order is already running
						lambdaOrder.setOrderState(OrderState.RELEASED);
						lambdaAnswer = new Message(Messages.ORDER_RELEASED);
					}
					lambdaAnswer.log(logger, lambdaOrder.getIdentifier());
					lambdaOrder.incrementVersion();
					lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
				} else {
					// stay in releasing failed, also if one job is already running
					lambdaOrder.setOrderState(OrderState.PLANNED);
					lambdaAnswer = new Message(Messages.ORDER_PLANNED);
					lambdaAnswer.log(logger, lambdaOrder.getIdentifier(), this.getName());
				}
				return lambdaAnswer;
			});
		}

		return answer;
	}
	
}
