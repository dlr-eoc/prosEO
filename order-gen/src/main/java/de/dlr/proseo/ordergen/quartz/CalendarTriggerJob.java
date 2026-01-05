package de.dlr.proseo.ordergen.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.CalendarOrderTrigger;

public class CalendarTriggerJob implements Job {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(CalendarTriggerJob.class);

	private String mission = null;
	private String workflow = null;
	private String priority = null;
	private String executionDelay = null;
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		// TODO Auto-generated method stub

		if (logger.isTraceEnabled()) {
			Object object = context.getJobDetail().getJobDataMap().get("ordergenTrigger");
			String workflow = null;
			if (object instanceof CalendarOrderTrigger) {
				workflow = ((CalendarOrderTrigger) object).getWorkflow().getName();
			}
			logger.trace("--- trigger fired ({}, {})", context.getJobDetail().getKey(), workflow);
		}
	}

}
