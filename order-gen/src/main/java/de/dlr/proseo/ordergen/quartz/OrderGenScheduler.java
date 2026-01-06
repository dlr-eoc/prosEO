/**
 * OrderGenScheduler.java
 *
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.CalendarOrderTrigger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.OrderTrigger;
import de.dlr.proseo.model.enums.TriggerType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordergen.util.TriggerUtil;


/**
 * OrderGenScheduler to hold and manage triggers.
 *
 * @author Ernst Melchinger
 *
 */
@Transactional(isolation = Isolation.REPEATABLE_READ)
public class OrderGenScheduler {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderGenScheduler.class);

	// The quartz scheduler
	private Scheduler sched = null;
	
	// Reference to the util instance
	private TriggerUtil triggerUtil;
	
	/**
	 * Initialize the scheduler
	 * 
	 * @param util The TriggerUtil instance
	 * @throws SchedulerException
	 */
	public void init(TriggerUtil util) throws SchedulerException {
		if (logger.isTraceEnabled())
			logger.trace(">>> initialize quartz scheduler()");
		triggerUtil = util;
		// First we must get a reference to a scheduler
		SchedulerFactory sf = new StdSchedulerFactory();
		sched = sf.getScheduler();
	}
	
	/**
	 * Load the Calendar trigger from database and add it to the scheduler
	 * 
	 * @throws SchedulerException
	 */
	public void buildCalendarTriggers() throws SchedulerException {
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

					Date ft = sched.scheduleJob(qartzJob, qartzTrigger);
				}
			}
		}
		if (logger.isTraceEnabled())
			logger.trace("<<< CalendarTriggers built");
	}

	/**
	 * Start the quartz scheduler
	 * 
	 * @throws SchedulerException
	 */
	public void start() throws SchedulerException {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> start quartz scheduler");
		}
		sched.start();
		if (logger.isTraceEnabled()) {
			logger.trace("<<< quartz scheduler started");
		}	
	}

	/**
	 * Shutdown the quartz scheduler
	 * 
	 * @throws SchedulerException
	 */
	public void shutdown() throws SchedulerException {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> shutdown quartz scheduler");
		}
		sched.shutdown(true);
		if (logger.isTraceEnabled()) {
			logger.trace("<<< quartz scheduler shutdown");
		}	
	}

	/**
	 * Shutdown, initialize, build triggers and start the quartz scheduler
	 * 
	 * @throws SchedulerException
	 */
	public void reload() throws SchedulerException {
		if (logger.isTraceEnabled()) {
			logger.trace(">>> reload triggers and restart quartz scheduler");
		}
		shutdown();
		init(triggerUtil);
		buildCalendarTriggers();
		start();
	}
}
