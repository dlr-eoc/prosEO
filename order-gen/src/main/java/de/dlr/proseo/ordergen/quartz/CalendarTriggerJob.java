/**
 * CalendarTriggerJob.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordergen.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.CalendarOrderTrigger;
import de.dlr.proseo.ordergen.OrderGenerator;

/**
 * Calendar (cron) quartz job implementation.
 *
 * @author Ernst Melchinger
 *
 */
public class CalendarTriggerJob implements Job {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(CalendarTriggerJob.class);
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		Object object = context.getJobDetail().getJobDataMap().get("orderTrigger");
		String workflowName = null;
		CalendarOrderTrigger trigger = null;
		if (object instanceof CalendarOrderTrigger) {
			trigger = ((CalendarOrderTrigger) object);
			workflowName = trigger.getWorkflow().getName();
		}
		if (logger.isTraceEnabled()) {
			logger.trace("--- trigger fired ({}, {})", context.getJobDetail().getKey(), workflowName);
		}

		OrderGenerator.orderCreator.createAndStartFromTrigger(trigger, context.getPreviousFireTime(), context.getFireTime(), context.getNextFireTime(), null);
	}

}
