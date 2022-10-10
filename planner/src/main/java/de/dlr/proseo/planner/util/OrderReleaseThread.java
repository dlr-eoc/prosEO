/**
 * OrderReleaseThread.java
 * 
 * @author Ernst Melchinger
 * © 2019 Prophos Informatik GmbH
 */

package de.dlr.proseo.planner.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.logging.messages.ProseoMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.SimplePolicy;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
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
	private static ProseoLogger logger = new ProseoLogger(OrderReleaseThread.class);
    
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
		
		ProseoMessage answer = GeneralMessage.FALSE;
		if (order != null && productionPlanner != null && jobUtil != null) {
			answer = GeneralMessage.TRUE;
			final long orderId = order.getId();
			try {
				if (answer.getSuccess()) {
					answer = release(order.getId());
				}
				if (!answer.getSuccess()) {
					// nothing to do here, order state already set in release
				}
			}
			catch(InterruptedException e) {
				logger.log(PlannerMessage.ORDER_RELEASING_INTERRUPTED, this.getName(), order.getIdentifier());
			} 
			catch(IllegalStateException e) {
				final ProcessingOrder order = transactionTemplate.execute((status) -> {
					Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
					if (orderOpt.isPresent()) {
						return orderOpt.get();
					}
					return null;
				});
				if (order.getOrderState().equals(OrderState.RUNNING) || order.getOrderState().equals(OrderState.RELEASED)) {
					// order already set to RELEASED or RUNNING, nothing to do					
				} else {
					logger.log(PlannerMessage.ORDER_RELEASING_EXCEPTION, this.getName(), order.getIdentifier());
					logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getMessage());
					productionPlanner.acquireThreadSemaphore("runRelease1");	
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

					productionPlanner.releaseThreadSemaphore("runRelease1");
				}
			}
			catch(Exception e) {
				logger.log(PlannerMessage.ORDER_RELEASING_EXCEPTION, this.getName(), order.getIdentifier());
				logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getMessage());
				productionPlanner.acquireThreadSemaphore("runRelease2");
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
				productionPlanner.releaseThreadSemaphore("runRelease2");
			}
		}
		productionPlanner.getReleaseThreads().remove(this.getName());
		if (logger.isTraceEnabled()) logger.trace("<<< run({})", this.getName());
		productionPlanner.checkNextForRestart();				
    }
    
    
    /**
     * Release the processing order 
     * 
     * @param orderId The id of the order
     * @return The result message
     * @throws InterruptedException
     */
    public ProseoMessage release(long orderId) throws InterruptedException {
		ProcessingOrder order = null;

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		final List<Job> jobList = new ArrayList<Job>();
		try {
			productionPlanner.acquireThreadSemaphore("release1");	
			order = transactionTemplate.execute((status) -> {
				Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
				if (orderOpt.isPresent()) {
					jobList.addAll(orderOpt.get().getJobs());

					jobList.sort(new Comparator<Job>() {
						@Override
						public int compare(Job o1, Job o2) {
							return o1.getStartTime().compareTo(o2.getStartTime());
						}});
					
					return orderOpt.get();
				}
				return null;
			});
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());
		} finally {
			productionPlanner.releaseThreadSemaphore("release1");					
		}
		if (logger.isTraceEnabled()) logger.trace(">>> release({})", (null == order ? "null" : order.getId()));
		ProseoMessage answer = GeneralMessage.FALSE;
		ProseoMessage releaseAnswer = GeneralMessage.FALSE;
		if (order != null && 
				(order.getOrderState() == OrderState.RELEASING || order.getOrderState() == OrderState.PLANNED)) {
			

			int packetSize = ProductionPlanner.config.getPlanningBatchSize();
			final Long jCount = (long) jobList.size();
			List<Integer> curJList = new ArrayList<Integer>();
			curJList.add(0);
			List<Integer> curJSList = new ArrayList<Integer>();
			curJSList.add(0);
			try {
				while (curJList.get(0) < jCount) {
					try {
						productionPlanner.acquireThreadSemaphore("releaseOrder");	
						if (logger.isTraceEnabled()) logger.trace(">>> releaseJobBlock({})", curJList.get(0));
						Object answer1 = transactionTemplate.execute((status) -> {
							curJSList.set(0, 0);
							ProseoMessage locAnswer = GeneralMessage.TRUE;
							while (curJList.get(0) < jCount && curJSList.get(0) < packetSize) {
								if (this.isInterrupted()) {
									return PlannerMessage.ORDER_RELEASING_INTERRUPTED;
								}
								if (jobList.get(curJList.get(0)).getJobState() == JobState.PLANNED) {
									Job locJob = RepositoryService.getJobRepository().getOne(jobList.get(curJList.get(0)).getId());
									locAnswer = jobUtil.resume(locJob);
									curJSList.set(0, curJSList.get(0) + locJob.getJobSteps().size());
								}
								curJList.set(0, curJList.get(0) + 1);		
							}
							return locAnswer;					
						});
						if(answer1 instanceof ProseoMessage) {
							answer = (ProseoMessage) answer1;
							if (answer.getCode() == PlannerMessage.ORDER_RELEASING_INTERRUPTED.getCode()) {
								break;
							}
						}
					} catch (Exception e) {	
						throw e;
					} finally {
						productionPlanner.releaseThreadSemaphore("releaseOrder");
					}
					try {
						productionPlanner.acquireThreadSemaphore("releaseOrder2");	
						@SuppressWarnings("unused")
						Object answer2 = transactionTemplate.execute((status) -> {
							List<JobStep> jsToRelease = RepositoryService.getJobStepRepository()
									.findAllByJobStepStateAndOrderIdByDate(JobStepState.READY, orderId);
							for (JobStep js : jsToRelease) {
								try {
									Job locJob = RepositoryService.getJobRepository().getOne(js.getJob().getId());
									KubeConfig kc = productionPlanner.getKubeConfig(locJob.getProcessingFacility().getName());
									if (!kc.couldJobRun()) {
										break;
									}
									UtilService.getJobStepUtil().checkJobStepToRun(kc, js.getId());
								} catch (Exception e) {
									e.printStackTrace();
								} 
							}
							return null;					
						});
					} catch (Exception e) {	
						throw e;
					} finally {
						productionPlanner.releaseThreadSemaphore("releaseOrder2");
					}
				}
			} catch (Exception e) {	
				throw e;
			}
			if (this.isInterrupted()) {
				answer = PlannerMessage.ORDER_RELEASING_INTERRUPTED;
				logger.log(answer, this.getName(), order.getIdentifier());
				throw new InterruptedException();
			}
			final ProseoMessage finalAnswer = releaseAnswer;
			try {
				productionPlanner.acquireThreadSemaphore("releaseOrder3");	
				answer = transactionTemplate.execute((status) -> {
					ProcessingOrder lambdaOrder = null;
					Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(orderId);
					if (orderOpt.isPresent()) {
						lambdaOrder = orderOpt.get();
					}
					ProseoMessage lambdaAnswer = GeneralMessage.FALSE;
					if (finalAnswer.getSuccess() || finalAnswer.getCode() == PlannerMessage.JOB_ALREADY_COMPLETED.getCode()) {
						if (lambdaOrder.getJobs().isEmpty()) {
							lambdaOrder.setOrderState(OrderState.COMPLETED);
							UtilService.getOrderUtil().checkAutoClose(lambdaOrder);
							lambdaAnswer = PlannerMessage.ORDER_PRODUCT_EXIST;
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
							lambdaAnswer = PlannerMessage.ORDER_RELEASED;
						}
						logger.log(lambdaAnswer, lambdaOrder.getIdentifier());
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
						lambdaAnswer = PlannerMessage.ORDER_RELEASED;
						logger.log(lambdaAnswer, lambdaOrder.getIdentifier(), this.getName());
						lambdaOrder = RepositoryService.getOrderRepository().save(lambdaOrder);
					}
					return lambdaAnswer;
				});

			} catch (Exception e) {	
				throw e;
			} finally {
				productionPlanner.releaseThreadSemaphore("releaseOrder3");
			}
		}

		return answer;
	}
	
}
