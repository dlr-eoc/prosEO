package de.dlr.proseo.planner.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.planner.ProductionPlanner;

public class OrderPlanThread extends Thread {

	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(OrderPlanThread.class);

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
	 * The expected job count 
	 */
	private int jobCount;
	
	/**
	 * The already planned jobs
	 */
	private int plannedJobs;
	
	/**
	 * @return the jobCount
	 */
	public int getJobCount() {
		return jobCount;
	}


	/**
	 * @return the plannedJobs
	 */
	public int getPlannedJobs() {
		return plannedJobs;
	}


	/**
	 * Create new thread
	 * 
	 * @param order The processing order to plan
	 * @param name The thread name
	 */
	public OrderPlanThread(ProductionPlanner productionPlanner, ProcessingOrder order,  ProcessingFacility procFacility, String name) {
		super(name);
		this.productionPlanner = productionPlanner;
		this.order = order;
		this.procFacility = procFacility;
	}
	

    public void run() {
		if (logger.isTraceEnabled()) logger.trace(">>> run({})", this.getName());

		if (order != null && productionPlanner != null) {
			plannedJobs = 0;
			// jobCount = getExpectedJobCount(order);
			jobCount = 10000;
			try {
				while (true) {
					plannedJobs++;
					sleep(1000);
				}
			}
			catch(InterruptedException e) {
			}
		}
    }
}
