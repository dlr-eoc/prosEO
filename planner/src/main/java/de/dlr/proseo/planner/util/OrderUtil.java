/**
 * OrderUtil.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.OrderDispatcher;

/**
 * Handle processing orders
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class OrderUtil {
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderUtil.class);

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
    /**
     * The job utility instance
     */
    @Autowired
    private JobUtil jobUtil;

    /**
     * The order dispatcher instance
     */
    @Autowired
    private OrderDispatcher orderDispatcher;

    /** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;


	/**
	 * Cancel the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages cancel(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancel({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
				answer = Messages.ORDER_HASTOBE_PLANNED;
				break;
			case PLANNED:
				for (Job job : order.getJobs()) {
					jobUtil.cancel(job);
				}
				order.setOrderState(OrderState.FAILED);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer = Messages.ORDER_CANCELED;
				break;	
			case RELEASED:
				answer = Messages.ORDER_ALREADY_RELEASED;
				break;
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;
				break;
			case RUNNING:
				answer = Messages.ORDER_ALREADY_RUNNING;
				break;
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Reset the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	public Messages reset(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> reset({})", (null == order ? "null" : order.getId()));
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_RESET;
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

				productionPlanner.acquireThreadSemaphore("OrderUtil.reset");
				transactionTemplate.execute((status) -> {
					Optional<ProcessingOrder> opt = RepositoryService.getOrderRepository().findById(order.getId());
					if (opt.isPresent()) {
						ProcessingOrder orderx = opt.get();
						orderx.setOrderState(OrderState.APPROVED);
						orderx.setHasFailedJobSteps(false);
						orderx.incrementVersion();
						RepositoryService.getOrderRepository().save(orderx);
						logOrderState(orderx);
					}
					return null;
				});
				productionPlanner.releaseThreadSemaphore("OrderUtil.reset");
				answer = Messages.ORDER_RESET;
				break;	
			case PLANNING_FAILED:
				// jobs are in initial state, no change
				productionPlanner.acquireThreadSemaphore("OrderUtil.reset");
				transactionTemplate.execute((status) -> {
					Optional<ProcessingOrder> opt = RepositoryService.getOrderRepository().findById(order.getId());
					if (opt.isPresent()) {
						ProcessingOrder orderx = opt.get();
						orderx.setOrderState(OrderState.APPROVED);
						orderx.setHasFailedJobSteps(false);
						orderx.incrementVersion();
						RepositoryService.getOrderRepository().save(orderx);
						logOrderState(orderx);
					}
					return null;
				});
				productionPlanner.releaseThreadSemaphore("OrderUtil.reset");
				answer = Messages.ORDER_RESET;
				break;			
			case APPROVED:		
			case RELEASED:		
			case PLANNED:
				// remove jobs and job steps
				productionPlanner.acquireThreadSemaphore("OrderUtil.reset");
				try {
				transactionTemplate.execute((status) -> {
					Optional<ProcessingOrder> opt = RepositoryService.getOrderRepository().findById(order.getId());
					if (opt.isPresent()) {
						ProcessingOrder orderx = opt.get();
						HashMap<Long,Job> toRemove = new HashMap<Long,Job>();
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
						orderx.setHasFailedJobSteps(false);
						orderx.incrementVersion();
						RepositoryService.getOrderRepository().save(orderx);
						logOrderState(orderx);
					}
					return null;
				});
				} catch (Exception e) {
					Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
				}
				productionPlanner.releaseThreadSemaphore("OrderUtil.reset");
				answer = Messages.ORDER_RESET;
				break;	
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;
				break;
			case RUNNING:
				answer = Messages.ORDER_ALREADY_RUNNING;
				break;
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Delete the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages delete(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> delete({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
				// jobs are in initial state, no change
				RepositoryService.getOrderRepository().delete(order);
				answer = Messages.ORDER_DELETED;
				break;				
			case PLANNED:
			case COMPLETED:
			case FAILED:
			case CLOSED:
				// remove jobs and jobsteps
				HashMap<Long,Job> toRemove = new HashMap<Long,Job>();
				for (Job job : order.getJobs()) {
					if (jobUtil.deleteForced(job)) {
						toRemove.put(job.getId(), job);
					}
				}
				List<Job> existingJobs = new ArrayList<Job>();
				existingJobs.addAll(order.getJobs());
				order.getJobs().clear();
				for (Job job : existingJobs) {
					if (toRemove.get(job.getId()) == null) {
						order.getJobs().add(job);
					} else {
						RepositoryService.getJobRepository().delete(job);
					}
				}
				RepositoryService.getOrderRepository().delete(order);
				answer = Messages.ORDER_DELETED;
				break;
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;
				break;	
			case RELEASED:
				answer = Messages.ORDER_ALREADY_RELEASED;
				break;
			case RUNNING:
				answer = Messages.ORDER_ALREADY_RUNNING;
				break;
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Approve the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages approve(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> approve({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.ORDER_ALREADY_APPROVED;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				// jobs are in initial state, no change
				order.setOrderState(OrderState.APPROVED);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer = Messages.ORDER_APPROVED;
				break;			
			case APPROVED:
				answer = Messages.ORDER_ALREADY_APPROVED;
				break;
			case PLANNED:	
				answer = Messages.ORDER_ALREADY_PLANNED;
				break;
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;
				break;
			case RELEASED:
				answer = Messages.ORDER_ALREADY_RELEASED;
				break;
			case RUNNING:
				answer = Messages.ORDER_ALREADY_RUNNING;
				break;
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Plan the processing order and it jobs and job steps.
	 * 
	 * @param order The processing order
	 * @param procFacility The processing facility to run the order
	 * @return Result message
	 */
	public Messages plan(long id,  ProcessingFacility procFacility) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());

		final ProcessingOrder order = transactionTemplate.execute((status) -> {
			Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
			if (orderOpt.isPresent()) {
				return orderOpt.get();
			}
			return null;
		});
		if (logger.isTraceEnabled()) logger.trace(">>> plan({}, {})", (null == order ? "null" : order.getId()),
				(null == procFacility ? "null" : procFacility.getName()));
		
		Messages answer = Messages.FALSE;
		if (order != null && procFacility != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_HASTOBE_APPROVED;
				break;
			case APPROVED:
			case PLANNING:
			case PLANNING_FAILED:
				productionPlanner.acquireThreadSemaphore("plan");
				answer = transactionTemplate.execute((status) -> {
					Messages answerx = Messages.FALSE;
					Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
					if (orderOpt.isPresent()) {
						orderOpt.get().setOrderState(OrderState.PLANNING);
						orderOpt.get().incrementVersion();
						RepositoryService.getOrderRepository().save(orderOpt.get());
						answerx = Messages.ORDER_PLANNING;
					} else {
						answerx = Messages.ORDER_PLANNING_FAILED;
					}
					return answerx;
				});
				productionPlanner.releaseThreadSemaphore("plan");
				if (answer.isTrue()) {
					String threadName = ProductionPlanner.PLAN_THREAD_PREFIX + order.getId();
					if (!productionPlanner.getPlanThreads().containsKey(threadName)) {
						OrderPlanThread pt = new OrderPlanThread(productionPlanner, orderDispatcher, id, procFacility, threadName);
						productionPlanner.getPlanThreads().put(threadName, pt);
						pt.start();
					}
				}
				break;	
			case PLANNED:	
				answer = Messages.ORDER_ALREADY_PLANNED;
				break;
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;
				break;
			case RELEASED:
				answer = Messages.ORDER_ALREADY_RELEASED;
				break;
			case RUNNING:
				answer = Messages.ORDER_ALREADY_RUNNING;
				break;
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Resume the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	public Messages resume(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> resume({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_HASTOBE_APPROVED;
				break;
			case APPROVED:
			case PLANNING:
				answer = Messages.ORDER_HASTOBE_PLANNED;
				break;
			case PLANNED:
			case RELEASING:
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				Boolean doIt = false;
				try {
					productionPlanner.acquireThreadSemaphore("resume");	
					doIt = transactionTemplate.execute((status) -> {
						Optional<ProcessingOrder> opt = RepositoryService.getOrderRepository().findById(order.getId());
						if (opt.isPresent()) {
							ProcessingOrder orderx = opt.get();
							orderx.setOrderState(OrderState.RELEASING);
							orderx.incrementVersion();
							orderx = RepositoryService.getOrderRepository().save(orderx);
							return true;
						}
						return false;
					});
				} catch (Exception e) {
					Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
				} finally {
					productionPlanner.releaseThreadSemaphore("resume");					
				}
				if (doIt) {
					try {
						productionPlanner.acquireThreadSemaphore("resume");	
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
								OrderReleaseThread rt = new OrderReleaseThread(productionPlanner, jobUtil, ordery, threadName);
								productionPlanner.getReleaseThreads().put(threadName, rt);
								rt.start();
							}
							logOrderState(order);
							answer = Messages.ORDER_RELEASING;
						}
					} catch (Exception e) {
						Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
					} finally {
						productionPlanner.releaseThreadSemaphore("resume");					
					}
				}
				break;	
			case RELEASED:
				answer = Messages.ORDER_ALREADY_RELEASED;
				break;
			case RUNNING:
				answer = Messages.ORDER_ALREADY_RUNNING;
				break;
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Start the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages startOrder(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> startOrder({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_HASTOBE_APPROVED;
				break;
			case APPROVED:
				answer = Messages.ORDER_HASTOBE_PLANNED;
				break;
			case PLANNED:
				answer = Messages.ORDER_HASTOBE_RELEASED;
				break;	
			case RELEASING:
				answer = Messages.ORDER_ALREADY_RELEASING;	
				break;		
			case RELEASED:
				order.setOrderState(OrderState.RUNNING);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer = Messages.ORDER_RUNNING;
				break;				
			case RUNNING:
				answer = Messages.ORDER_RUNNING;
				break;	
			case SUSPENDING:
				answer = Messages.ORDER_ALREADY_SUSPENDING;
				break;
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Suspend the processing order and its jobs and job steps.
	 * 
	 * @param order The processing order id
	 * @param force The flag to force kill of currently running job steps on processing facility
	 * @return Result message
	 */
	public Messages suspend(long id, Boolean force) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());

		final ProcessingOrder order = transactionTemplate.execute((status) -> {
			Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
			if (orderOpt.isPresent()) {
				return orderOpt.get();
			}
			return null;
		});
		if (logger.isTraceEnabled()) logger.trace(">>> suspend({}, {})", (null == order ? "null" : order.getId()), force);
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_SUSPENDED;
				break;
			case APPROVED:
				answer = Messages.ORDER_SUSPENDED;
				break;
			case PLANNED:	
				answer = Messages.ORDER_SUSPENDED;
				break;
			case RELEASING:
			case RUNNING:
			case SUSPENDING:
				// look for plan thread and interrupt it
				OrderReleaseThread rt = productionPlanner.getReleaseThreads().get(ProductionPlanner.RELEASE_THREAD_PREFIX + order.getId());
				if (rt != null) {
					rt.interrupt();
					int i = 0;
					while (rt.isAlive() && i < 1000) {
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
					productionPlanner.acquireThreadSemaphore("suspend");	
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
						}
						ordery.setOrderState(OrderState.SUSPENDING);
						RepositoryService.getOrderRepository().save(ordery);
						return ordery;
					});
					answer = transactionTemplate.execute((status) -> {
						Boolean suspending = false;
						Boolean allFinished = true;
						ProcessingOrder orderz = null;
						Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
						if (orderOpt.isPresent()) {
							orderz = orderOpt.get();
						}
						if (orderz == null) {
							return Messages.ORDER_NOT_EXIST;
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
						}
						if (suspending) {
							// check whether some jobs are already finished
							orderz.setOrderState(OrderState.SUSPENDING);
							RepositoryService.getOrderRepository().save(orderz);
							logOrderState(orderz);		
							return Messages.ORDER_SUSPENDED;
						} else if (allFinished) {
							// check whether some jobs are already finished
							orderz.setOrderState(OrderState.COMPLETED);
							checkAutoClose(order);
							RepositoryService.getOrderRepository().save(orderz);
							logOrderState(orderz);			
							return  Messages.ORDER_COMPLETED;
						} else {
							orderz.setOrderState(OrderState.PLANNED);
							RepositoryService.getOrderRepository().save(orderz);
							logOrderState(orderz);			
							return  Messages.ORDER_SUSPENDED;
						}
					});
				} catch (Exception e) {
					Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
				} finally {
					productionPlanner.releaseThreadSemaphore("suspend");
					productionPlanner.checkNextForRestart();					
				}
				break;	
			case RELEASED:
				try {
					productionPlanner.acquireThreadSemaphore("suspend");	
					answer = transactionTemplate.execute((status) -> {
						ProcessingOrder orderz = null;
						Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
						if (orderOpt.isPresent()) {
							orderz = orderOpt.get();
						}
						if (orderz == null) {
							return Messages.ORDER_NOT_EXIST;
						}
						for (Job job : orderz.getJobs()) {
							jobUtil.suspend(job, force);
						}
						if (orderz.getOrderState() == OrderState.RELEASED) {
							orderz.setOrderState(OrderState.SUSPENDING);
						}
						if (orderz.getOrderState() == OrderState.RUNNING) {
							orderz.setOrderState(OrderState.SUSPENDING);
						}
						orderz.setOrderState(OrderState.PLANNED);
						orderz.incrementVersion();
						RepositoryService.getOrderRepository().save(orderz);
						logOrderState(orderz);
						return Messages.ORDER_SUSPENDED;
					});
				} catch (Exception e) {
					Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
				} finally {
					productionPlanner.releaseThreadSemaphore("suspend");	
					productionPlanner.checkNextForRestart();				
				}
				break;			
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Prepare the suspend of the processing order.
	 * All jobs are set to state ON_HOLD first to avoid start of further job steps.
	 * 
	 * @param order The processing order id
	 * @param force The flag to force kill of currently running job steps on processing facility
	 * @return Result message
	 */
	public Messages prepareSuspend(long id, Boolean force) {
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		final ProcessingOrder order = transactionTemplate.execute((status) -> {
			Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
			if (orderOpt.isPresent()) {
				return orderOpt.get();
			}
			return null;
		});
		if (logger.isTraceEnabled()) logger.trace(">>> prepareSuspend({}, {})", (null == order ? "null" : order.getId()), force);
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_SUSPENDED;
				break;
			case APPROVED:
				answer = Messages.ORDER_SUSPENDED;
				break;
			case PLANNED:	
				answer = Messages.ORDER_SUSPENDED;
				break;
			case RELEASING:
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
				// look for plan thread and interrupt it
				answer = Messages.TRUE;
				OrderReleaseThread rt = productionPlanner.getReleaseThreads().get(ProductionPlanner.RELEASE_THREAD_PREFIX + order.getId());
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
						answer = Messages.ORDER_COULD_NOT_INTERRUPT;
					}
				}
				if (answer.isTrue()) {
					try {
						productionPlanner.acquireThreadSemaphore("prepareSuspend");	
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
							RepositoryService.getOrderRepository().save(orderz);
							return null;
						});
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
						answer = Messages.ORDER_SUSPEND_PREPARED;
					} catch (Exception e) {
						Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
					} finally {
						productionPlanner.releaseThreadSemaphore("prepareSuspend");					
					}
				}
				break;	
			case COMPLETED:
				answer = Messages.ORDER_ALREADY_COMPLETED;
				break;
			case FAILED:
				answer = Messages.ORDER_ALREADY_FAILED;
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Retry the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages retry(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> retry({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
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
				answer = Messages.ORDER_COULD_NOT_RETRY;
				break;
			case FAILED:
				Boolean all = true;
				Boolean allCompleted = true;
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
						answer = Messages.ORDER_COMPLETED;
					} else if (allCompleted) {
						if (order.getOrderState() != OrderState.RUNNING) {
							order.setOrderState(OrderState.PLANNED);
							order.setOrderState(OrderState.RELEASED);
							order.setOrderState(OrderState.RUNNING);
						}
						order.setOrderState(OrderState.COMPLETED);
						checkAutoClose(order);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						answer = Messages.ORDER_COMPLETED;
					} else {
						if (order.getOrderState() == OrderState.RUNNING) {
							order.setOrderState(OrderState.SUSPENDING);
						}
						order.setOrderState(OrderState.PLANNED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						answer = Messages.ORDER_RETRIED;
					}
					logOrderState(order);
				} else {
					answer = Messages.ORDER_COULD_NOT_RETRY;
				}
				break;
			case CLOSED:
				answer = Messages.ORDER_ALREADY_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Close the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages close(ProcessingOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> close({})", (null == order ? "null" : order.getId()));
		
		Messages answer = Messages.FALSE;
		if (order != null) {
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:	
			case RELEASING:
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
				answer = Messages.ORDER_HASTOBE_FINISHED;
				break;
			case COMPLETED:
			case FAILED:
				// job steps are completed/failed
				Duration retPeriod = order.getMission().getOrderRetentionPeriod();
				if (retPeriod != null && order.getProductionType() == ProductionType.SYSTEMATIC) {
					order.setEvictionTime(Instant.now().plus(retPeriod));
				}
				for (Job job : order.getJobs()) {
					jobUtil.close(job);
				}
				order.setOrderState(OrderState.CLOSED);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				logOrderState(order);
				answer = Messages.ORDER_CLOSED;
				break;			
			case CLOSED:
				answer = Messages.ORDER_CLOSED;
				break;
			default:
				break;
			}
			// don't log here, it is done by the caller
			// answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Check whether the processing order and it jobs and job steps are finished.
	 * 
	 * @param order The processing Order
	 * @return true after success
	 */
	@Transactional
	public Boolean checkFinish(Long orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkFinish({})", orderId);
		
		Boolean answer = false;	
		Boolean checkFurther = false;
		Boolean hasChanged = false;
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
				Boolean onHold = RepositoryService.getJobRepository().countJobOnHoldByProcessingOrderId(order.getId()) > 0;
				if (!onHold) {
					Boolean all = RepositoryService.getJobRepository().countJobNotFinishedByProcessingOrderId(order.getId()) == 0;
					if (!all) {
						order.setOrderState(OrderState.PLANNED);
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
				Boolean all = RepositoryService.getJobRepository().countJobNotFinishedByProcessingOrderId(order.getId()) == 0;
				if (all) {
					Boolean completed = RepositoryService.getJobRepository().countJobFailedByProcessingOrderId(order.getId()) == 0;
					if (completed) {
						order.setOrderState(OrderState.COMPLETED);
						checkAutoClose(order);
					} else {
						order.setOrderState(OrderState.FAILED);
					}
					checkFurther = true;
					RepositoryService.getOrderRepository().save(order);
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
				for (Job j : order.getJobs()) {
					if (j.getJobState() == JobState.FAILED) {
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
				Boolean allHasFinished = true;
				for (Job j : order.getJobs()) {
					if (j.getJobState() != JobState.FAILED && j.getJobState() != JobState.COMPLETED) {
						allHasFinished = false;
						break;
					}
				}
				if (allHasFinished) {
					if (hasFailed) {
						order.setOrderState(OrderState.FAILED);
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
					} else {
						order.setOrderState(OrderState.COMPLETED);
						checkAutoClose(order);
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
	 * If the order is in systematic processing and the mission has an order retention period,
	 * the order is automatically closed after completion and the eviction time is set.
	 * 
	 * @param order The processing order
	 * @return true if closed
	 */
	@Transactional
	public Boolean checkAutoClose(ProcessingOrder order) {
		Duration retPeriod = order.getMission().getOrderRetentionPeriod();
		if (retPeriod != null && order.getProductionType() == ProductionType.SYSTEMATIC) {
			order.setEvictionTime(Instant.now().plus(retPeriod));
			for (Job job : order.getJobs()) {
				if (job.getJobState() == JobState.COMPLETED) {
					jobUtil.close(job);
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
	 * Get the processing facility(-ies) processing the order
	 * At the moment there is normally only one facility to do so.
	 * 
	 * @param order The processing order
	 * @return List of processinig facilities
	 */
	@Transactional
	public List<ProcessingFacility> getProcessingFacilities(long id) {
		ProcessingOrder order = null;
		Optional<ProcessingOrder> oOrder = RepositoryService.getOrderRepository().findById(id);
		if (oOrder.isPresent()) {
			order = oOrder.get();
		}
		if (logger.isTraceEnabled()) logger.trace(">>> getProcessingFacilities({})", (null == order ? "null" : order.getId()));
		
		List<ProcessingFacility> pfList = new ArrayList<ProcessingFacility>();
		if (order != null) {
			for (Job j : order.getJobs()) {
				if (!pfList.contains(j.getProcessingFacility())) {
					pfList.add(j.getProcessingFacility());
				}
			}
		}
		return pfList;
	}
	
	/**
	 * Set the failed job steps flag in the order on failure
	 * 
	 * @param order the order to update
	 * @param failed indicates whether a job step has failed
	 */
	@Transactional
	public void setHasFailedJobSteps(ProcessingOrder order, Boolean failed) {
		if (logger.isTraceEnabled()) logger.trace(">>> setHasFailedJobSteps({}, {})", (null == order ? "null" : order.getId()), failed);
		
		if (failed && !order.getHasFailedJobSteps()) {
			order.setHasFailedJobSteps(failed);
			order.incrementVersion();
			RepositoryService.getOrderRepository().save(order);
			em.merge(order);
		}
	}

	public void logOrderState(ProcessingOrder order) {
		// at the moment a dummy		
	}


	/**
	 * Look for next order in state PLANNING, RELEASING and SUSPENDING to restart.
	 */
	public void checkNextForRestart() {
		if (logger.isTraceEnabled()) logger.trace(">>> checkNextForRestart()");
		long id = 0;
		if (productionPlanner.getSuspendingOrders().size() > 0) {
			id = (long)productionPlanner.getSuspendingOrders().get(0);
			productionPlanner.getSuspendingOrders().remove(0);
			restartSuspendingOrder(id);
			return;
		}
		if (productionPlanner.getReleasingOrders().size() > 0) {
			id = (long)productionPlanner.getReleasingOrders().get(0);
			productionPlanner.getReleasingOrders().remove(0);
			restartReleasingOrder(id);
			return;
		}
		if (productionPlanner.getPlanningOrders().size() > 0) {
			id = (long)productionPlanner.getPlanningOrders().get(0);
			productionPlanner.getPlanningOrders().remove(0);
			restartPlanningOrder(id);
			return;
		}
	}

	/**
	 * restart the corresponding thread.
	 */
	@Transactional
	private void restartPlanningOrder(long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> restartPlanningOrder({})", id);

		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());

			// Find the processing facility
			final ProcessingFacility pf = transactionTemplate.execute((status) -> {
				Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
				if (orderOpt.isPresent()) {
					Set<Job> jobs = orderOpt.get().getJobs();
					for (Job job : jobs) {
						if (job.getProcessingFacility() != null) {
							return job.getProcessingFacility();
						}
					}
				}
				return null;
			});
			if (pf != null) {
				UtilService.getOrderUtil().plan(id, pf);
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
	 * restart the corresponding thread.
	 */
	public void restartReleasingOrder(long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> restartReleasingOrder({})", id);
			
		Optional<ProcessingOrder> order = RepositoryService.getOrderRepository().findById(id);
		if (order.isPresent()) {
			// resume the order
			UtilService.getOrderUtil().resume(order.get());
		}
	}

	/**
	 * restart the corresponding thread.
	 */
	public void restartSuspendingOrder(long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> restartSuspendingOrder({})", id);
		
		UtilService.getOrderUtil().suspend(id, true);
	}
}
