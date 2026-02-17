/**
 * OrbitTriggerJob.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordergen.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.OrderGenMessage;
import de.dlr.proseo.model.OrbitOrderTrigger;
import de.dlr.proseo.ordergen.OrderGenerator;

/**
 * Orbit quartz job implementation.
 *
 * @author Ernst Melchinger
 *
 */
public class OrbitTriggerJob implements Job {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrbitTriggerJob.class);
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		Object object = context.getJobDetail().getJobDataMap().get("orderTrigger");
		String orderTemplateName = null;
		OrbitOrderTrigger trigger = null;
		if (object instanceof OrbitOrderTrigger) {
			trigger = ((OrbitOrderTrigger) object);
			orderTemplateName = trigger.getOrderTemplate().getName();
		}
		if (logger.isTraceEnabled()) {
			logger.trace("--- trigger fired ({}, {})", context.getJobDetail().getKey(), orderTemplateName);
		}

		OrderGenerator.orderCreator.createAndStartFromTrigger(trigger, context.getPreviousFireTime(), 
				context.getFireTime(), context.getNextFireTime(), null);
		try {
			OrderGenerator.scheduler.buildNextOrbitTriggerFor(trigger);
		} catch (SchedulerException e) {
			logger.log(OrderGenMessage.CREATE_ORBIT_TRIGGER_FAILED, e.getMessage());
		} catch (Exception e) {
			logger.log(OrderGenMessage.EXCEPTION, e.getMessage());
		}
	}

}
