package de.dlr.proseo.planner.util;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.ProcessingOrder.OrderState;
import de.dlr.proseo.model.Job.JobState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.dispatcher.OrderDispatcher;

@Component
@Transactional
public class OrderUtil {
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrderUtil.class);

    @Autowired
    private JobUtil jobUtil;
    @Autowired
    private JobStepUtil jobStepUtil;
    @Autowired
    private OrderDispatcher orderDispatcher;

	@Transactional
	public Boolean cancel(ProcessingOrder order) {
		Boolean answer = false;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:
				for (Job job : order.getJobs()) {
					jobUtil.cancel(job);
				}
				order.setOrderState(de.dlr.proseo.model.ProcessingOrder.OrderState.FAILED);
				RepositoryService.getOrderRepository().save(order);
				answer = true;
				break;				
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
			case COMPLETED:
			case FAILED:
			case CLOSED:
			default:
				break;
			}
		}
		return answer;
	}

	@Transactional
	public Boolean reset(ProcessingOrder order) {
		Boolean answer = false;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
				answer = true;
				break;
			case APPROVED:
				// jobs are in initial state, no change
				order.setOrderState(de.dlr.proseo.model.ProcessingOrder.OrderState.INITIAL);
				RepositoryService.getOrderRepository().save(order);
				answer = true;
				break;				
			case PLANNED:
				// remove jobs and jobsteps
				List<Job> toRemove = new ArrayList<Job>();
				for (Job job : order.getJobs()) {
					if (jobUtil.delete(job)) {
						toRemove.add(job);
					}
				}
				for (Job job : toRemove) {
					order.getJobs().remove(job);
				}
				order.setOrderState(de.dlr.proseo.model.ProcessingOrder.OrderState.INITIAL);
				RepositoryService.getOrderRepository().save(order);
				answer = true;
				break;				
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
			case COMPLETED:
			case FAILED:
			case CLOSED:
			default:
				break;
			}
		}
		return answer;
	}
	
	@Transactional
	public Boolean approve(ProcessingOrder order) {
		Boolean answer = false;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
				// jobs are in initial state, no change
				order.setOrderState(de.dlr.proseo.model.ProcessingOrder.OrderState.APPROVED);
				RepositoryService.getOrderRepository().save(order);
				answer = true;
				break;			
			case APPROVED:
			case PLANNED:	
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
			case COMPLETED:
			case FAILED:
			case CLOSED:
			default:
				break;
			}
		}
		return answer;
	}

	@Transactional
	public Boolean plan(ProcessingOrder order,  ProcessingFacility procFacility) {
		Boolean answer = false;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
				break;
			case APPROVED:
				if (orderDispatcher.publishOrder(order, procFacility)) {
					jobStepUtil.searchForJobStepsToRun(procFacility);
					order.setOrderState(de.dlr.proseo.model.ProcessingOrder.OrderState.PLANNED);
					RepositoryService.getOrderRepository().save(order);
					answer = true;
				}
				break;			
			case PLANNED:	
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
			case COMPLETED:
			case FAILED:
			case CLOSED:
			default:
				break;
			}
		}
		return answer;
	}

	@Transactional
	public Boolean resume(ProcessingOrder order) {
		Boolean answer = false;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:
				for (Job job : order.getJobs()) {
					jobUtil.resume(job);
				}
				order.setOrderState(de.dlr.proseo.model.ProcessingOrder.OrderState.RELEASED);
				RepositoryService.getOrderRepository().save(order);
				answer = true;
				break;				
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
			case COMPLETED:
			case FAILED:
			case CLOSED:
			default:
				break;
			}
		}
		return answer;
	}

	@Transactional
	public Boolean startOrder(ProcessingOrder order) {
		Boolean answer = false;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:
				break;				
			case RELEASED:
				order.setOrderState(de.dlr.proseo.model.ProcessingOrder.OrderState.RUNNING);
				RepositoryService.getOrderRepository().save(order);
				answer = true;
				break;				
			case RUNNING:
				answer = true;
				break;				
			case SUSPENDING:
			case COMPLETED:
			case FAILED:
			case CLOSED:
			default:
				break;
			}
		}
		return answer;
	}
	
	@Transactional
	public Boolean suspend(ProcessingOrder order) {
		Boolean answer = false;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
				break;
			case PLANNED:	
				answer = true;
				break;
			case RELEASED:
				for (Job job : order.getJobs()) {
					jobUtil.suspend(job);
				}
				order.setOrderState(de.dlr.proseo.model.ProcessingOrder.OrderState.PLANNED);
				RepositoryService.getOrderRepository().save(order);
				answer = true;
				break;			
			case RUNNING:
				Boolean oneNotSuspended = true;
				for (Job job : order.getJobs()) {
					oneNotSuspended = jobUtil.suspend(job) & oneNotSuspended;
				}
				if (oneNotSuspended) {
					order.setOrderState(de.dlr.proseo.model.ProcessingOrder.OrderState.SUSPENDING);
					RepositoryService.getOrderRepository().save(order);
					answer = true;
				} else {
					order.setOrderState(de.dlr.proseo.model.ProcessingOrder.OrderState.PLANNED);
					RepositoryService.getOrderRepository().save(order);
					answer = true;
				}
				break;			
			case SUSPENDING:
			case COMPLETED:
			case FAILED:
			case CLOSED:
			default:
				break;
			}
		}
		return answer;
	}
	
	@Transactional
	public Boolean close(ProcessingOrder order) {
		Boolean answer = false;
		if (order != null) {
			// INITIAL, APPROVED, PLANNED, RELEASED, RUNNING, SUSPENDING, COMPLETED, FAILED, CLOSED
			switch (order.getOrderState()) {
			case INITIAL:
			case APPROVED:
			case PLANNED:	
			case RELEASED:
			case RUNNING:
			case SUSPENDING:
				break;
			case COMPLETED:
			case FAILED:
				// job steps are completed/failed
				order.setOrderState(de.dlr.proseo.model.ProcessingOrder.OrderState.CLOSED);
				RepositoryService.getOrderRepository().save(order);
				answer = true;
				break;			
			case CLOSED:
				answer = true;
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
				break;
			case RUNNING:
				Boolean all = true;
				Boolean completed = true;
				for (Job job : order.getJobs()) {
					switch (job.getJobState()) {
					case COMPLETED:
						break;
					case FAILED:
						completed = false;
						break;
					default:
						all = false;
						break;
					}
				}
				if (all) {
					if (completed) {
						order.setOrderState(OrderState.COMPLETED);
					} else {
						order.setOrderState(OrderState.FAILED);
					}
					RepositoryService.getOrderRepository().save(order);
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

}
