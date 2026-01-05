package de.dlr.proseo.ordergen.quartz;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SchedulerMetaData;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.CalendarOrderTrigger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.OrderTrigger;
import de.dlr.proseo.model.enums.TriggerType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordergen.util.TriggerUtil;


public class OrdergenScheduler {
	

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrdergenScheduler.class);

	private Scheduler sched = null;
	
	private TriggerUtil triggerUtil;
	
	public void init(TriggerUtil util) throws SchedulerException {
		if (logger.isTraceEnabled())
			logger.trace(">>> buildCalendarTriggers()");
		triggerUtil = util;
		// First we must get a reference to a scheduler
		SchedulerFactory sf = new StdSchedulerFactory();
		sched = sf.getScheduler();
	}
	
	public void buildCalendarTriggers() {
		if (logger.isTraceEnabled())
			logger.trace(">>> buildCalendarTriggers()");
		for (Mission mission : RepositoryService.getMissionRepository().findAll()) {
			for (OrderTrigger orderTrigger : triggerUtil.findAllByMissionCodeAndTriggerNameAndType(mission.getCode(), null, TriggerType.Calendar.name())) {
				if (orderTrigger instanceof CalendarOrderTrigger) {
					CalendarOrderTrigger trigger = (CalendarOrderTrigger)orderTrigger;
					JobDetail qartzJob = newJob(CalendarTriggerJob.class).withIdentity(trigger.getName(), trigger.getMission().getCode() + "/" + TriggerType.Calendar.name()).build();
					CronTrigger qartzTrigger = newTrigger().withIdentity(trigger.getName(), trigger.getMission().getCode() + "/" + TriggerType.Calendar.name())
							.withSchedule(cronSchedule(trigger.getCronExpression()))
							.build();

					qartzJob.getJobDataMap().put("ordergenTrigger", trigger);

					try {
						Date ft = sched.scheduleJob(qartzJob, qartzTrigger);
					} catch (SchedulerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		if (logger.isTraceEnabled())
			logger.trace("--- start sched");
	    try {
			sched.start();
			
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    try {
	      // wait five minutes to show jobs
	      Thread.sleep(60L * 1000L);
	      // executing...
	    } catch (Exception e) {
	      //
	    }

		if (logger.isTraceEnabled())
			logger.trace("--- shutdown sched");
	    try {
			sched.shutdown(true);
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
