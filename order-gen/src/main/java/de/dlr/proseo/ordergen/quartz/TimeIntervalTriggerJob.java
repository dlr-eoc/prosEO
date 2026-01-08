package de.dlr.proseo.ordergen.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.TimeIntervalOrderTrigger;
import de.dlr.proseo.ordergen.OrderGenerator;

public class TimeIntervalTriggerJob implements Job {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(TimeIntervalTriggerJob.class);
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub

		Object object = context.getJobDetail().getJobDataMap().get("orderTrigger");
		String workflowName = null;
		TimeIntervalOrderTrigger trigger = null;
		if (object instanceof TimeIntervalOrderTrigger) {
			trigger = ((TimeIntervalOrderTrigger) object);
			workflowName = trigger.getWorkflow().getName();
		}
		if (logger.isTraceEnabled()) {
			logger.trace("--- trigger fired ({}, {})", context.getJobDetail().getKey(), workflowName);
		}

		OrderGenerator.orderCreator.createAndStartFromTrigger(trigger, context.getPreviousFireTime(), context.getFireTime(), context.getNextFireTime(), null);
	}

}
