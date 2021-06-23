/**
 * OrderUtil.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Message;
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
    private OrderDispatcher orderDispatcher;

	/**
	 * Cancel the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Messages cancel(ProcessingOrder order) {
		Messages answer = Messages.FALSE;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:
			case RELEASED:
				for (Job job : order.getJobs()) {
					jobUtil.cancel(job);
				}
				order.setOrderState(OrderState.FAILED);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				answer = Messages.ORDER_CANCELED;
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
				order.setHasFailedJobSteps(false);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				answer = Messages.ORDER_RESET;
				break;		
			case RELEASED:		
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
				order.setHasFailedJobSteps(false);
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				answer = Messages.ORDER_RESET;
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
		Messages answer = Messages.ORDER_ALREADY_APPROVED;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
				// jobs are in initial state, no change
				order.setOrderState(OrderState.APPROVED);
				order.incrementVersion();
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
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Plan the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
	@Transactional
	public Message plan(ProcessingOrder order,  ProcessingFacility procFacility) {
		Message answer = new Message(Messages.FALSE);
		if (order != null && procFacility != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
				answer.setMessage(Messages.ORDER_HASTOBE_APPROVED);
				break;
			case APPROVED:
				answer = orderDispatcher.publishOrder(order, procFacility);
				if (answer.isTrue()) {
					if (order.getJobs().isEmpty()) {
						order.setOrderState(OrderState.COMPLETED);
						answer.setMessage(Messages.ORDER_PRODUCT_EXIST);
					} else {
						order.setOrderState(OrderState.PLANNED);
						answer.setMessage(Messages.ORDER_PLANNED);
					}
					order.incrementVersion();
					order = RepositoryService.getOrderRepository().save(order);
				}
				break;	
			case PLANNED:	
				answer.setMessage(Messages.ORDER_ALREADY_PLANNED);
				break;
			case RELEASED:
				answer.setMessage(Messages.ORDER_ALREADY_RELEASED);
				break;
			case RUNNING:
				answer.setMessage(Messages.ORDER_ALREADY_RUNNING);
				break;
			case SUSPENDING:
				answer.setMessage(Messages.ORDER_ALREADY_SUSPENDING);
				break;
			case COMPLETED:
				answer.setMessage(Messages.ORDER_ALREADY_COMPLETED);
				break;
			case FAILED:
				answer.setMessage(Messages.ORDER_ALREADY_FAILED);
				break;
			case CLOSED:
				answer.setMessage(Messages.ORDER_ALREADY_CLOSED);
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
				order.incrementVersion();
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
				order.incrementVersion();
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
			answer.log(logger, order.getIdentifier());
		}
		return answer;
	}

	/**
	 * Suspend the processing order and it jobs and job steps.
	 * 
	 * @param order The processing Order
	 * @return Result message
	 */
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
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				answer = Messages.ORDER_SUSPENDED;
				break;			
			case RUNNING:
			case SUSPENDING:
				Boolean suspending = false;
				Boolean allFinished = true;
				for (Job job : order.getJobs()) {
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
				order.incrementVersion();
				if (suspending) {
					// check whether some jobs are already finished
					order.setOrderState(OrderState.SUSPENDING);
					RepositoryService.getOrderRepository().save(order);
					answer = Messages.ORDER_SUSPENDED;
				} else if (allFinished) {
					// check whether some jobs are already finished
					order.setOrderState(OrderState.COMPLETED);
					RepositoryService.getOrderRepository().save(order);
					answer = Messages.ORDER_COMPLETED;
				} else {
					order.setOrderState(OrderState.SUSPENDING); // direct transition to PLANNED not allowed
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
				Boolean allCompleted = true;
				order.setHasFailedJobSteps(false);
				for (Job job : order.getJobs()) {
					jobUtil.retry(job);
				}
				for (Job job : order.getJobs()) {
					if (!(job.getJobState() == JobState.INITIAL || job.getJobState() == JobState.COMPLETED)) {
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
					if (allCompleted) {
						order.setOrderState(OrderState.COMPLETED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						answer = Messages.ORDER_COMPLETED;
					} else {
						order.setOrderState(OrderState.PLANNED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						answer = Messages.ORDER_RETRIED;
					}
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
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				answer = Messages.ORDER_CLOSED;
				break;			
			case CLOSED:
				answer = Messages.ORDER_CLOSED;
				break;
			default:
				break;
			}
			answer.log(logger, order.getIdentifier());
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
		Boolean answer = false;	
		Boolean checkFurther = false;
		Boolean hasChanged = false;
		ProcessingOrder order = null;
		Optional<ProcessingOrder> oOrder = RepositoryService.getOrderRepository().findById(orderId);
		if (oOrder.isPresent()) {
			order = oOrder.get();
		}
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
					} else {
						order.setOrderState(OrderState.COMPLETED);
					}
					RepositoryService.getOrderRepository().save(order);
					em.merge(order);
					hasChanged = true;
				}
			}
			if (hasChanged) {
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				em.merge(order);
			}
		}
 		return answer;
	}
		
	/**
	 * Get the processing facility(-ies) processing the order
	 * At the moment there is normally only one facility to do so.
	 * 
	 * @param order The processing order
	 * @return List of processinig facilities
	 */
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
	
	
	/**
	 * Update the order state depending on job state
	 * TODO
	 * 
	 * @param order The processing order
	 * @param jState The job state
	 */

	@Transactional
	public void updateState(ProcessingOrder orderOrig, JobState jState) {
		ProcessingOrder order = null;
		Optional<ProcessingOrder> oOrder = RepositoryService.getOrderRepository().findById(orderOrig.getId());
		if (oOrder.isPresent()) {
			order = oOrder.get();
		}
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
				// fall through intended
			case APPROVED:
				// no job and job step exists
				break;
			case PLANNED:
				if (jState == JobState.RELEASED) {
					order.setOrderState(OrderState.RELEASED);
					order.incrementVersion();
					RepositoryService.getOrderRepository().save(order);
					em.merge(order);
				} else if (jState == JobState.FAILED) {
					Boolean allState = true;
					this.setHasFailedJobSteps(order,  true);
					for (Job j : order.getJobs()) {
						if (j.getJobState() != JobState.FAILED && j.getJobState() != JobState.COMPLETED) {
							allState = false;
							break;
						}
					}
					if (allState) {
						order.setOrderState(OrderState.FAILED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
					}
				}	
				break;
			case RELEASED:
				if (jState == JobState.FAILED) {
					Boolean allState = true;
					this.setHasFailedJobSteps(order,  true);
					for (Job j : order.getJobs()) {
						if (j.getJobState() != JobState.FAILED && j.getJobState() != JobState.COMPLETED) {
							allState = false;
							break;
						}
					}
					if (allState) {
						order.setOrderState(OrderState.FAILED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
					}
				} else if (jState == JobState.STARTED) {
					order.setOrderState(OrderState.RUNNING);
					order.incrementVersion();
					RepositoryService.getOrderRepository().save(order);
					em.merge(order);
				}	
				break;
			case RUNNING:
				if (jState == JobState.FAILED) {
					Boolean allState = true;
					this.setHasFailedJobSteps(order,  true);
					for (Job j : order.getJobs()) {
						if (j.getJobState() != JobState.FAILED && j.getJobState() != JobState.COMPLETED) {
							allState = false;
							break;
						}
					}
					if (allState) {
						order.setOrderState(OrderState.FAILED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
					}
				} else if (jState == JobState.ON_HOLD) {
					Boolean allState = true;
					for (Job j : order.getJobs()) {
						if (j.getJobState() != JobState.ON_HOLD) {
							allState = false;
							break;
						}
					}
					if (allState) {
						order.setOrderState(OrderState.SUSPENDING);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
					}
				}	
				break;
			case SUSPENDING:
				if (jState == JobState.FAILED) {
					Boolean allState = true;
					this.setHasFailedJobSteps(order,  true);
					for (Job j : order.getJobs()) {
						if (j.getJobState() != JobState.FAILED && j.getJobState() != JobState.COMPLETED) {
							allState = false;
							break;
						}
					}
					if (allState) {
						order.setOrderState(OrderState.FAILED);
						order.incrementVersion();
						RepositoryService.getOrderRepository().save(order);
						em.merge(order);
					}
				}	
				break;
			case COMPLETED:
				break;
			case FAILED:
				if (jState == JobState.INITIAL) {
					order.setOrderState(OrderState.PLANNED);
					order.incrementVersion();
					RepositoryService.getOrderRepository().save(order);
					em.merge(order);
				}
				break;
			case CLOSED:
				break;
			default:
				break;
			}
			Boolean hasFailed = false;
			for (Job j : order.getJobs()) {
				if (j.getJobState() == JobState.FAILED || j.hasFailedJobSteps()) {
					hasFailed = true;
					break;
				}
			}
			order.setHasFailedJobSteps(hasFailed);
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
				} else {
					order.setOrderState(OrderState.COMPLETED);
				}
				order.incrementVersion();
				RepositoryService.getOrderRepository().save(order);
				em.merge(order);
			}
		}
	}
	

	/**
	 * Set the failed job steps flag in the order on failure
	 * 
	 * @param order the order to update
	 * @param failed indicates whether a job step has failed
	 */
	@Transactional
	public void setHasFailedJobSteps(ProcessingOrder order, Boolean failed) {
		if (failed && !order.getHasFailedJobSteps()) {
			order.setHasFailedJobSteps(failed);
			order.incrementVersion();
			RepositoryService.getOrderRepository().save(order);
			em.merge(order);
		}
	}


}
