package de.dlr.proseo.monitor.microservice;

import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.MonitorMessage;
import de.dlr.proseo.model.MonExtService;
import de.dlr.proseo.model.MonExtServiceStateOperationDay;
import de.dlr.proseo.model.MonExtServiceStateOperationMonth;
import de.dlr.proseo.model.MonService;
import de.dlr.proseo.model.MonServiceStateOperationDay;
import de.dlr.proseo.model.MonServiceStateOperationMonth;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.MonServiceStates;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.monitor.MonitorConfiguration;

/**
 * The thread monitoring KPI01 timeliness
 * 
 * @author Melchinger
 *
 */

public class MonServiceAggregation extends Thread {
	private static ProseoLogger logger = new ProseoLogger(MonServiceAggregation.class);	

	/** Transaction manager for transaction control */

	private PlatformTransactionManager txManager;
	
	/** JPA entity manager */
	private EntityManager em;

	/**
	 * The S1 monitor configuration additions (application.yml) 
	 */
	private MonitorConfiguration config;

	/**
	 * Instantiate the monitor services thread
	 * 
	 * @param config The monitor configuration
	 * @param config The S1 monitor configuration additions
	 * @param txManager The transaction manager
	 */
	public MonServiceAggregation(MonitorConfiguration config,
			PlatformTransactionManager txManager, EntityManager em) {
		this.config = config;
		this.txManager = txManager;
		this.em = em;
		this.setName("MonServiceAggregation");
	}

	/**
	 * Collect the monitoring information of services for day and month
	 */

	public void checkMonServiceAggregation() {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkMonServiceAggregation()");
		Instant now = Instant.now();
		Instant timeFrom;
		Instant timeTo;
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);
		ZonedDateTime zdt = transactionTemplate.execute((status) -> {			
			return calcBasicStartTime(now, RepositoryService.getMonServiceStateOperationDayRepository().findLastDatetime());
		});
		ZonedDateTime zdtOrig = zdt;

		List<MonService> services = transactionTemplate.execute((status) -> {	
			return RepositoryService.getMonServiceRepository().findAll();
		});
		List<MonExtService> extServices = transactionTemplate.execute((status) -> {	
			return RepositoryService.getMonExtServiceRepository().findAll();
		});
		// loop over missing entries
		transactionTemplate.setReadOnly(false);
		for (MonService m : services) {
			timeFrom = zdtOrig.toInstant();
			timeTo = timeFrom.plus(1, ChronoUnit.DAYS);
			if (logger.isTraceEnabled())
				logger.trace("  aggregate days for service '{}'", m.getName());
			while (timeFrom.isBefore(now)) {
				final Instant timeFromX = timeFrom;
				final Instant timeToX = timeTo;
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						transactionTemplate.execute((status) -> {			
							int countUp = getServiceStateCount(timeFromX, timeToX, m.getId(), true);
							int countDown = getServiceStateCount(timeFromX, timeToX, m.getId(), false);
							double upPercent = (countUp + countDown) == 0 ? 0.0 : 100.0 * countUp / (countUp + countDown);

							MonServiceStateOperationDay tm;
							try {
								tm = RepositoryService.getMonServiceStateOperationDayRepository().findByDateTimeBetween(timeFromX, timeToX, m.getId()).get(0);
							} catch (IndexOutOfBoundsException ex) {
								tm = new MonServiceStateOperationDay();
							}
							tm.setMonServiceId(m.getId());
							tm.setUpTime(upPercent);
							tm.setDatetime(timeFromX);
							RepositoryService.getMonServiceStateOperationDayRepository().save(tm);			
							return null;
						});
						break;
					} catch (CannotAcquireLockException e) {
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					}
				}											
				timeFrom = timeTo;
				timeTo = timeTo.plus(1, ChronoUnit.DAYS);
			}
		}
		transactionTemplate.setReadOnly(true);
		zdt = transactionTemplate.execute((status) -> {			
			return calcBasicStartTime(now, RepositoryService.getMonExtServiceStateOperationDayRepository().findLastDatetime());
		});
		zdtOrig = zdt;
		// loop over missing entries
		transactionTemplate.setReadOnly(false);
		for (MonExtService m : extServices) {
			timeFrom = zdtOrig.toInstant();
			timeTo = timeFrom.plus(1, ChronoUnit.DAYS);
			if (logger.isTraceEnabled())
				logger.trace("  aggregate days for external service '{}'", m.getName());
			while (timeFrom.isBefore(now)) {
				final Instant timeFromX = timeFrom;
				final Instant timeToX = timeTo;
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						transactionTemplate.execute((status) -> {	
							int countUp = getExtServiceStateCount(timeFromX, timeToX, m.getId(), true);
							int countDown = getExtServiceStateCount(timeFromX, timeToX, m.getId(), false);
							double upPercent = (countUp + countDown) == 0 ? 0.0 : 100.0 * countUp / (countUp + countDown);

							MonExtServiceStateOperationDay tm;
							try {
								tm = RepositoryService.getMonExtServiceStateOperationDayRepository().findByDateTimeBetween(timeFromX, timeToX, m.getId()).get(0);
							} catch (IndexOutOfBoundsException ex) {
								tm = new MonExtServiceStateOperationDay();
							}
							tm.setMonExtServiceId(m.getId());
							tm.setUpTime(upPercent);
							tm.setDatetime(timeFromX);
							RepositoryService.getMonExtServiceStateOperationDayRepository().save(tm);			
							return null;
						});
						break;
					} catch (CannotAcquireLockException e) {
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					}
				}													
				timeFrom = timeTo;
				timeTo = timeTo.plus(1, ChronoUnit.DAYS);
			}
		}

		transactionTemplate.setReadOnly(true);
		zdt = transactionTemplate.execute((status) -> {			
			return calcBasicStartTime(now, RepositoryService.getMonServiceStateOperationMonthRepository().findLastDatetime());
		});
		int d = zdt.getDayOfMonth() - 1;
		zdtOrig = zdt.minusDays(d);
		// loop over missing entries
		transactionTemplate.setReadOnly(false);
		for (MonService m : services) {
			timeFrom = zdtOrig.toInstant();
			timeTo = zdtOrig.plusMonths(1).toInstant();
			if (logger.isTraceEnabled())
				logger.trace("  aggregate months for service '{}'", m.getName());
			while (timeFrom.isBefore(now)) {
				final Instant timeFromX = timeFrom;
				final Instant timeToX = timeTo;
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						transactionTemplate.execute((status) -> {	
							int countUp = getServiceStateCount(timeFromX, timeToX, m.getId(), true);
							int countDown = getServiceStateCount(timeFromX, timeToX, m.getId(), false);
							double upPercent = (countUp + countDown) == 0 ? 0.0 : 100.0 * countUp / (countUp + countDown);

							MonServiceStateOperationMonth tm;
							try {
								tm = RepositoryService.getMonServiceStateOperationMonthRepository().findByDateTimeBetween(timeFromX, timeToX, m.getId()).get(0);
							} catch (IndexOutOfBoundsException ex) {
								tm = new MonServiceStateOperationMonth();
							}
							tm.setMonServiceId(m.getId());
							tm.setUpTime(upPercent);
							tm.setDatetime(timeFromX);
							RepositoryService.getMonServiceStateOperationMonthRepository().save(tm);		
							return null;
						});
						break;
					} catch (CannotAcquireLockException e) {
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					}
				}													
				zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
				timeFrom = zdt.plusMonths(1).toInstant();
				zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
				timeTo = zdt.plusMonths(1).toInstant();
			}
		}
		transactionTemplate.setReadOnly(true);
		zdt = transactionTemplate.execute((status) -> {			
		return calcBasicStartTime(now, RepositoryService.getMonExtServiceStateOperationMonthRepository().findLastDatetime());
		});
		d = zdt.getDayOfMonth() - 1;
		zdtOrig = zdt.minusDays(d);
		// loop over missing entries
		transactionTemplate.setReadOnly(false);
		for (MonExtService m : extServices) {
			timeFrom = zdtOrig.toInstant();
			timeTo = zdtOrig.plusMonths(1).toInstant();
			if (logger.isTraceEnabled())
				logger.trace("  aggregate months for external service '{}'", m.getName());
			while (timeFrom.isBefore(now)) {
				final Instant timeFromX = timeFrom;
				final Instant timeToX = timeTo;
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						transactionTemplate.execute((status) -> {	
							int countUp = getExtServiceStateCount(timeFromX, timeToX, m.getId(), true);
							int countDown = getExtServiceStateCount(timeFromX, timeToX, m.getId(), false);
							double upPercent = (countUp + countDown) == 0 ? 0.0 : 100.0 * countUp / (countUp + countDown);

							MonExtServiceStateOperationMonth tm;
							try {
								tm = RepositoryService.getMonExtServiceStateOperationMonthRepository().findByDateTimeBetween(timeFromX, timeToX, m.getId()).get(0);
							} catch (IndexOutOfBoundsException ex) {
								tm = new MonExtServiceStateOperationMonth();
							}
							tm.setMonExtServiceId(m.getId());
							tm.setUpTime(upPercent);
							tm.setDatetime(timeFromX);
							RepositoryService.getMonExtServiceStateOperationMonthRepository().save(tm);	
							return null;
						});
						break;
					} catch (CannotAcquireLockException e) {
						if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					}
				}													
				zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
				timeFrom = zdt.plusMonths(1).toInstant();
				zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
				timeTo = zdt.plusMonths(1).toInstant();
			}
		}
	}

	private int getServiceStateCount(Instant from, Instant to, long serviceId, Boolean up) {
		int count = 0;
		String sqlString = null;
		sqlString = "SELECT count(*) FROM mon_service_state_operation d WHERE "
				+ "d.mon_service_id = " + serviceId + " and ";
		if (up) {
			sqlString += "d.mon_service_state_id = " + MonServiceStates.RUNNING_ID + " and ";
		} else {
			sqlString += "d.mon_service_state_id <> " + MonServiceStates.RUNNING_ID + " and ";
		}
		sqlString += "d.datetime >= '" + from.toString() + "' and d.datetime < '" + to.toString() + "'";
		final Query query = em.createNativeQuery(sqlString);
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);
		Object result = transactionTemplate.execute((status) -> {			
			return query.getSingleResult();
		});
		if (result != null && result instanceof BigInteger) {
			count = ((BigInteger)result).intValue();
		}
		return count;
	}

	private int getExtServiceStateCount(Instant from, Instant to, long serviceId, Boolean up) {
		int count = 0;
		String sqlString = null;
		sqlString = "SELECT count(*) FROM mon_ext_service_state_operation d WHERE "
				+ "d.mon_ext_service_id = " + serviceId + " and ";
		if (up) {
			sqlString += "d.mon_service_state_id = " + MonServiceStates.RUNNING_ID + " and ";
		} else {
			sqlString += "d.mon_service_state_id <> " + MonServiceStates.RUNNING_ID + " and ";
		}
		sqlString += "d.datetime >= '" + from.toString() + "' and d.datetime < '" + to.toString() + "'";
		Query query = em.createNativeQuery(sqlString);
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);
		Object result = transactionTemplate.execute((status) -> {			
			return query.getSingleResult();
		});
		if (result != null && result instanceof BigInteger) {
			count = ((BigInteger)result).intValue();
		}
		return count;
	}

	private ZonedDateTime calcBasicStartTime(Instant now, Instant lastEntryDatetime) {
		Instant timeFrom = null;	
		if (lastEntryDatetime ==  null) {
			// no entry found, begin at now.
			timeFrom = now.truncatedTo(ChronoUnit.DAYS);
			if (config.getAggregationStart() != null) {
				try {
					timeFrom = Instant.parse(config.getAggregationStart()).truncatedTo(ChronoUnit.DAYS);
				} catch (DateTimeParseException ex) {
					logger.log(MonitorMessage.ILLEGAL_CONFIG_VALUE, config.getAggregationStart());
				}
			} 
		} else {
			timeFrom = lastEntryDatetime.truncatedTo(ChronoUnit.DAYS);
		}
		ZonedDateTime zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
		return zdt;
	}

    /**
     * Start the monitor thread
     */	
    public void run() {
    	Long wait = (long) 100000;
    	try {
    		if (config.getServiceAggregationCycle() != null) {
    			wait = config.getServiceAggregationCycle();
    		}
    	} catch (NumberFormatException e) {
    		wait = (long) 100000;
    	}
    	while (!this.isInterrupted()) {
    		// look for job steps to run
    		try {
    			this.checkMonServiceAggregation();
    		} catch (NoResultException e) {
    			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED,e);
    		} catch (IllegalArgumentException e) {
    			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED,e);
    		} catch (TransactionException e) {
    			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED,e);
    		} catch (RuntimeException e) {
    			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED,e);
    		}
    		try {
    			sleep(wait);
    		}
    		catch(InterruptedException e) {
    			this.interrupt();
    		}
    	}
    }   
}