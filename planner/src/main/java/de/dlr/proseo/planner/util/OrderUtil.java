/**
 * OrderUtil.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.dispatcher.OrderDispatcher;

/**
 * Handle processing orders
 * 
 * @author Ernst Melchinger
 *
 */
@Component
@Transactional
public class OrderUtil {
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderUtil.class);

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
    @Autowired
    private JobUtil jobUtil;
    @Autowired
    private JobStepUtil jobStepUtil;
    @Autowired
    private OrderDispatcher orderDispatcher;

	@Transactional
	public Messages cancel(ProcessingOrder order) {
		Messages answer = Messages.FALSE;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:
				for (Job job : order.getJobs()) {
					jobUtil.cancel(job);
				}
				order.setOrderState(OrderState.FAILED);
				RepositoryService.getOrderRepository().save(order);
				answer = Messages.ORDER_CANCELED;
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
		}
		return answer;
	}

	@Transactional
	public Messages reset(ProcessingOrder order) {
		Messages answer = Messages.FALSE;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_RESET;
				break;
			case APPROVED:
				// jobs are in initial state, no change
				order.setOrderState(OrderState.INITIAL);
				RepositoryService.getOrderRepository().save(order);
				answer = Messages.ORDER_RESET;
				break;				
			case PLANNED:
				// remove jobs and jobsteps
				HashMap<Long,Job> toRemove = new HashMap<Long,Job>();
				for (Job job : order.getJobs()) {
					if (jobUtil.delete(job)) {
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
				order.setOrderState(OrderState.INITIAL);
				RepositoryService.getOrderRepository().save(order);
				answer = Messages.ORDER_RESET;
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
		}
		return answer;
	}
	
	@Transactional
	public Messages delete(ProcessingOrder order) {
		Messages answer = Messages.FALSE;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
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
		}
		return answer;
	}
	
	@Transactional
	public Messages approve(ProcessingOrder order) {
		Messages answer = Messages.ORDER_ALREADY_APPROVED;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
				// jobs are in initial state, no change
				order.setOrderState(OrderState.APPROVED);
				RepositoryService.getOrderRepository().save(order);
				answer = Messages.ORDER_APPROVED;
				break;			
			case APPROVED:
				answer = Messages.ORDER_ALREADY_APPROVED;
				break;
			case PLANNED:	
				answer = Messages.ORDER_ALREADY_PLANNED;
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
		}
		return answer;
	}

	@Transactional
	public Messages plan(ProcessingOrder order,  ProcessingFacility procFacility) {
		Messages answer = Messages.FALSE;
		if (order != null && procFacility != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_HASTOBE_APPROVED;
				break;
			case APPROVED:
				if (orderDispatcher.publishOrder(order, procFacility)) {
					if (order.getJobs().isEmpty()) {
						order.setOrderState(OrderState.COMPLETED);
						answer = Messages.ORDER_COMPLETED;
					} else {
						// jobStepUtil.searchForJobStepsToRun(procFacility);
						order.setOrderState(OrderState.PLANNED);
						answer = Messages.ORDER_PLANNED;
					}
					order = RepositoryService.getOrderRepository().save(order);
				}
				break;	
			case PLANNED:	
				answer = Messages.ORDER_ALREADY_PLANNED;
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
		}
		return answer;
	}

	@Transactional
	public Messages resume(ProcessingOrder order) {
		Messages answer = Messages.FALSE;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
				answer = Messages.ORDER_HASTOBE_APPROVED;
				break;
			case APPROVED:
				answer = Messages.ORDER_HASTOBE_PLANNED;
				break;
			case PLANNED:
				for (Job job : order.getJobs()) {
					jobUtil.resume(job);
				}
				if (order.getJobs().isEmpty()) {
					order.setOrderState(OrderState.COMPLETED);
					answer = Messages.ORDER_COMPLETED;
				} else {
					order.setOrderState(OrderState.RELEASED);
					answer = Messages.ORDER_RELEASED;
				}
				RepositoryService.getOrderRepository().save(order);
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
		}
		return answer;
	}

	@Transactional
	public Messages startOrder(ProcessingOrder order) {
		Messages answer = Messages.FALSE;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
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
			case RELEASED:
				order.setOrderState(OrderState.RUNNING);
				RepositoryService.getOrderRepository().save(order);
				answer = Messages.ORDER_RELEASED;
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
		}
		return answer;
	}
	
	@Transactional
	public Messages suspend(ProcessingOrder order, Boolean force) {
		Messages answer = Messages.FALSE;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
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
			case RELEASED:
				for (Job job : order.getJobs()) {
					jobUtil.suspend(job, force);
				}
				order.setOrderState(OrderState.PLANNED);
				RepositoryService.getOrderRepository().save(order);
				answer = Messages.ORDER_SUSPENDED;
				break;			
			case RUNNING:
			case SUSPENDING:
				Boolean allSuspended = true;
				for (Job job : order.getJobs()) {
					allSuspended = jobUtil.suspend(job, force).isTrue() & allSuspended;
				}
				if (!allSuspended) {
					order.setOrderState(OrderState.SUSPENDING);
					RepositoryService.getOrderRepository().save(order);
					answer = Messages.ORDER_SUSPENDED;
				} else {
					order.setOrderState(OrderState.PLANNED);
					RepositoryService.getOrderRepository().save(order);
					answer = Messages.ORDER_SUSPENDED;
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
		}
		return answer;
	}

	@Transactional
	public Messages retry(ProcessingOrder order) {
		Messages answer = Messages.FALSE;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:	
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
			case COMPLETED:
				answer = Messages.ORDER_COULD_NOT_RETRY;
				break;
			case FAILED:
				Boolean all = true;
				for (Job job : order.getJobs()) {
					jobUtil.retry(job);
				}
				for (Job job : order.getJobs()) {
					if (!(job.getJobState() == JobState.INITIAL || job.getJobState() == JobState.COMPLETED)) {
						all = false;
						break;
					}
				}
				if (all) {
					order.setOrderState(OrderState.PLANNED);
					RepositoryService.getOrderRepository().save(order);
					answer = Messages.ORDER_RETRIED;
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
		}
		return answer;
	}
	
	@Transactional
	public Messages close(ProcessingOrder order) {
		Messages answer = Messages.FALSE;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:	
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
				answer = Messages.ORDER_HASTOBE_FINISHED;
				break;
			case COMPLETED:
			case FAILED:
				// job steps are completed/failed
				order.setOrderState(OrderState.CLOSED);
				RepositoryService.getOrderRepository().save(order);
				answer = Messages.ORDER_CLOSED;
				break;			
			case CLOSED:
				answer = Messages.ORDER_CLOSED;
				break;
			default:
				break;
			}
		}
		return answer;
	}
	
	@Transactional
	public Boolean checkFinish(ProcessingOrder order) {
		Boolean answer = false;	
		// check current state for possibility to be suspended
		// INITIAL, RELEASED, STARTED, ON_HOLD, COMPLETED, FAILED
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
					} else {
						order.setOrderState(OrderState.FAILED);
					}
					RepositoryService.getOrderRepository().save(order);
					em.merge(order);
				}
				answer = true;
				break;
			case COMPLETED:
			case FAILED:
				answer = true;
				break;
			default:
				break;
			}	
		}
 		return answer;
	}
	
	public List<ProcessingFacility> getProcessingFacilities(ProcessingOrder order) {
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

}
