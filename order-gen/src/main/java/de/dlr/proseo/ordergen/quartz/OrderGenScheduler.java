/**
 * OrderGenScheduler.java
 *
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordergen.quartz;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import java.time.Instant;
import java.util.Date;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.SimpleTrigger;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.CalendarOrderTrigger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.OrbitOrderTrigger;
import de.dlr.proseo.model.OrderTrigger;
import de.dlr.proseo.model.TimeIntervalOrderTrigger;
import de.dlr.proseo.model.enums.TriggerType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.ordergen.util.TriggerUtil;

/**
 * OrderGenScheduler to hold and manage triggers.
 *
 * @author Ernst Melchinger
 *
 */
public class OrderGenScheduler {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderGenScheduler.class);

	// The quartz scheduler
	private Scheduler sched = null;
	
	// Reference to the util instance
	private TriggerUtil triggerUtil;
	
	private PlatformTransactionManager txManager;
	
	/**
	 * @param txManager the txManager to set
	 */
	public void setTxManager(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}

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
	 * Load the Calendar triggers from database and add them to the scheduler
	 * 
	 * @throws SchedulerException
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void buildCalendarTriggers() throws SchedulerException {
		if (logger.isTraceEnabled())
			logger.trace(">>> buildCalendarTriggers()");
		for (Mission mission : RepositoryService.getMissionRepository().findAll()) {
			for (OrderTrigger orderTrigger : triggerUtil.findAllByMissionCodeAndTriggerNameAndType(mission.getCode(), null, TriggerType.Calendar.name())) {
				if (orderTrigger instanceof CalendarOrderTrigger) {
					CalendarOrderTrigger trigger = (CalendarOrderTrigger)orderTrigger;
					JobDetail quartzJob = newJob(CalendarTriggerJob.class).withIdentity(trigger.getName(), trigger.getMission().getCode() + "/" + TriggerType.Calendar.name()).build();
					CronTrigger quartzTrigger = newTrigger().withIdentity(trigger.getName(), trigger.getMission().getCode() + "/" + TriggerType.Calendar.name())
							.withSchedule(cronSchedule(trigger.getCronExpression()))
							.build();
					
					quartzJob.getJobDataMap().put("orderTrigger", trigger);
					sched.scheduleJob(quartzJob, quartzTrigger);
				}
			}
		}
		if (logger.isTraceEnabled())
			logger.trace("<<< CalendarTriggers built");
	}

	/**
	 * Load the TimeIntervall triggers from database and add them to the scheduler
	 * 
	 * @throws SchedulerException
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void buildImeIntervalTriggers() throws SchedulerException {
		if (logger.isTraceEnabled())
			logger.trace(">>> buildImeIntervalTriggers()");
		for (Mission mission : RepositoryService.getMissionRepository().findAll()) {
			for (OrderTrigger orderTrigger : triggerUtil.findAllByMissionCodeAndTriggerNameAndType(mission.getCode(), null, TriggerType.TimeInterval.name())) {
				if (orderTrigger instanceof TimeIntervalOrderTrigger) {
					TimeIntervalOrderTrigger trigger = (TimeIntervalOrderTrigger)orderTrigger;
					Instant startTime = Instant.now();
					if (trigger.getNextTriggerTime() != null) {
						startTime = trigger.getNextTriggerTime();
					}
					JobDetail quartzJob = newJob(TimeIntervalTriggerJob.class).withIdentity(trigger.getName(), trigger.getMission().getCode() + "/" + TriggerType.TimeInterval.name()).build();
					SimpleTrigger quartzTrigger = newTrigger().withIdentity(trigger.getName(), trigger.getMission().getCode() + "/" + TriggerType.TimeInterval.name())
							.startAt(Date.from(startTime))
					        .withSchedule(simpleSchedule().withIntervalInMilliseconds(trigger.getTriggerInterval().toMillis()).repeatForever())							
							.build();
					
					quartzJob.getJobDataMap().put("orderTrigger", trigger);
					sched.scheduleJob(quartzJob, quartzTrigger);
				}
			}
		}
		if (logger.isTraceEnabled())
			logger.trace("<<< ImeIntervalTriggers built");
	}

	/**
	 * Load the Orbit triggers from database and add them to the scheduler
	 * 
	 * @throws SchedulerException
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void buildOrbitTriggers() throws SchedulerException {
		if (logger.isTraceEnabled())
			logger.trace(">>> buildOrbitTriggers()");
		for (Mission mission : RepositoryService.getMissionRepository().findAll()) {
			for (OrderTrigger orderTrigger : triggerUtil.findAllByMissionCodeAndTriggerNameAndType(mission.getCode(), null, TriggerType.Orbit.name())) {
				if (orderTrigger instanceof OrbitOrderTrigger) {
					OrbitOrderTrigger trigger = (OrbitOrderTrigger)orderTrigger;
					Orbit orbit = trigger.getLastOrbit();
					if (orbit != null) {
					// calculate start time using orbit
					Instant startTime = orbit.getStopTime().plus(trigger.getDeltaTime());
					JobDetail quartzJob = newJob(OrbitTriggerJob.class).withIdentity(trigger.getName(), trigger.getMission().getCode() + "/" + TriggerType.Orbit.name()).build();
					SimpleTrigger quartzTrigger = (SimpleTrigger)newTrigger().withIdentity(trigger.getName(), trigger.getMission().getCode() + "/" + TriggerType.Orbit.name())
							.startAt(Date.from(startTime))						
							.build();
					
					quartzJob.getJobDataMap().put("orderTrigger", trigger);
					sched.scheduleJob(quartzJob, quartzTrigger);
					} else {
						// Error
					}
				}
			}
		}
		if (logger.isTraceEnabled())
			logger.trace("<<< ImeIntervalTriggers built");
	}

	/**
	 * Build and start a trigger for the next orbit.
	 * @param orderTrigger
	 * @throws SchedulerException
	 */
	public void buildNextOrbitTriggerFor(OrderTrigger orderTrigger) throws SchedulerException {
		if (logger.isTraceEnabled())
			logger.trace(">>> builNextdOrbitTriggerFor({})", orderTrigger.getName());
				if (orderTrigger instanceof OrbitOrderTrigger) {
					TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							transactionTemplate.setReadOnly(false);
							Object o = transactionTemplate.execute((status) -> {
								OrbitOrderTrigger trigger = RepositoryService.getOrbitOrderTriggerRepository().findByMissionCodeAndName(orderTrigger.getMission().getCode(), orderTrigger.getName());
								Orbit orbit = trigger.getLastOrbit();
								if (orbit != null) {
									// calculate start time using orbit
									Integer orbitNr = RepositoryService.getOrbitRepository()
											.findNextByMissionCodeAndSpacecraftCodeAndOrbitNumber(trigger.getMission().getCode(), 
													trigger.getSpacecraft().getCode(), orbit.getOrbitNumber());
									Orbit nextOrbit = RepositoryService.getOrbitRepository()
											.findByMissionCodeAndSpacecraftCodeAndOrbitNumber(trigger.getMission().getCode(), 
													trigger.getSpacecraft().getCode(), orbitNr);
									if (nextOrbit != null) {
										trigger.setLastOrbit(nextOrbit);
										triggerUtil.save(trigger);
										Instant startTime = nextOrbit.getStopTime().plus(trigger.getDeltaTime());
										JobDetail quartzJob = newJob(OrbitTriggerJob.class).withIdentity(trigger.getName(), trigger.getMission().getCode() + "/" + TriggerType.Orbit.name()).build();
										SimpleTrigger quartzTrigger = (SimpleTrigger)newTrigger().withIdentity(trigger.getName(), trigger.getMission().getCode() + "/" + TriggerType.Orbit.name())
												.startAt(Date.from(startTime))						
												.build();

										quartzJob.getJobDataMap().put("orderTrigger", trigger);
										try {
											sched.deleteJob(quartzJob.getKey());
											sched.scheduleJob(quartzJob, quartzTrigger);
										} catch (SchedulerException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									} else {
										// Error
									}
								} else {
									// Error
								}
								return null;
							});

							break;
						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled())
								logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled())
									logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}
				}
		if (logger.isTraceEnabled())
			logger.trace("<<< OrbitTrigger built");
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
		buildImeIntervalTriggers();
		buildOrbitTriggers();
		start();
	}
}
