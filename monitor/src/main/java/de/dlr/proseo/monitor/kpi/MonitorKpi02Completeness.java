package de.dlr.proseo.monitor.kpi;

import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.MonKpi01TimelinessMonth;
import de.dlr.proseo.model.MonKpi02CompletenessMonth;
import de.dlr.proseo.model.MonKpi02CompletenessQuarter;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.monitor.MonitorConfiguration;

/**
 * The thread monitoring KPI01 timeliness
 * 
 * @author Melchinger
 *
 */
@Transactional
public class MonitorKpi02Completeness extends Thread {
	private static Logger logger = LoggerFactory.getLogger(MonitorKpi02Completeness.class);	

	/** Transaction manager for transaction control */

	private PlatformTransactionManager txManager;
	
	/** JPA entity manager */
	private EntityManager em;

	/**
	 * The monitor configuration (application.yml) 
	 */
	private MonitorConfiguration config;

	/**
	 * Instantiate the monitor raw data thread
	 * 
	 * @param config The monitor configuration
	 * @param txManager The transaction manager
	 */
	public MonitorKpi02Completeness(MonitorConfiguration config, PlatformTransactionManager txManager, EntityManager em) {
		this.config = config;
		this.txManager = txManager;
		this.em = em;
		this.setName("MonitorKpi02");
	}

	/**
	 * Collect the monitoring information of raw data for day and month
	 */
	@Transactional
	public void checkKpi02Completeness() {
		Instant now = Instant.now();
		Instant timeFrom;
		Instant timeTo;
		Instant lastEntryDatetime = RepositoryService.getMonKpi02CompletenessMonthRepository().findLastDatetime();
		if (lastEntryDatetime ==  null) {
			// no entry found, begin at now.
			timeFrom = now.truncatedTo(ChronoUnit.DAYS);
			if (config.getAggregationStart() != null) {
				try {
					timeFrom = Instant.parse(config.getAggregationStart()).truncatedTo(ChronoUnit.DAYS);
				} catch (DateTimeParseException ex) {
					logger.warn("Illegal config value productAggregationStart; {}", config.getAggregationStart());
				}
			} 
		} else {
			timeFrom = lastEntryDatetime.truncatedTo(ChronoUnit.DAYS);
		}
		timeTo = timeFrom.plus(1, ChronoUnit.DAYS);
		// month

		ZonedDateTime zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
		int d = zdt.getDayOfMonth() - 1;
		zdt = zdt.minusDays(d);
		timeFrom = zdt.toInstant();
		timeTo = zdt.plusMonths(1).toInstant();
		// loop over missing entries
		while (timeFrom.isBefore(now)) {
			int countAll = getCompletenessCount(timeFrom, timeTo, true);
			int countSuccess = getCompletenessCount(timeFrom, timeTo, false);
			MonKpi02CompletenessMonth tm;
			try {
				tm = RepositoryService.getMonKpi02CompletenessMonthRepository().findByDateTimeBetween(timeFrom, timeTo).get(0);
			} catch (IndexOutOfBoundsException ex) {
				tm = new MonKpi02CompletenessMonth();
			}
			tm.setCountCompleted(countSuccess);
			tm.setCountAll(countAll);
			tm.setDatetime(timeFrom);
			RepositoryService.getMonKpi02CompletenessMonthRepository().save(tm);
			zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
			timeFrom = zdt.plusMonths(1).toInstant();
			zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
			timeTo = zdt.plusMonths(1).toInstant();
		}
		// quarter
		lastEntryDatetime = RepositoryService.getMonKpi02CompletenessQuarterRepository().findLastDatetime();
		if (lastEntryDatetime ==  null) {
			// no entry found, begin at now.
			timeFrom = now.truncatedTo(ChronoUnit.DAYS);
			if (config.getAggregationStart() != null) {
				try {
					timeFrom = Instant.parse(config.getAggregationStart()).truncatedTo(ChronoUnit.DAYS);
				} catch (DateTimeParseException ex) {
					logger.warn("Illegal config value productAggregationStart; {}", config.getAggregationStart());
				}
			} 
		} else {
			timeFrom = lastEntryDatetime.truncatedTo(ChronoUnit.DAYS);
		}
		zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
		d = zdt.getDayOfMonth() - 1;
		zdt = zdt.minusDays(d);
		int m = zdt.getMonthValue();
		int md = (m - 1) % 3;
		zdt = zdt.minusMonths(md);
		timeFrom = zdt.toInstant();
		timeTo = zdt.plusMonths(3).toInstant();
		while (timeFrom.isBefore(now)) {
			int countAll = getCompletenessCount(timeFrom, timeTo, true);
			int countSuccess = getCompletenessCount(timeFrom, timeTo, false);
			MonKpi02CompletenessQuarter tq;
			try {
				tq = RepositoryService.getMonKpi02CompletenessQuarterRepository().findByDateTimeBetween(timeFrom, timeTo).get(0);
			} catch (IndexOutOfBoundsException ex) {
				tq = new MonKpi02CompletenessQuarter();
			}
			tq.setCountCompleted(countSuccess);
			tq.setCountAll(countAll);
			tq.setDatetime(timeFrom);
			RepositoryService.getMonKpi02CompletenessQuarterRepository().save(tq);
			zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
			timeFrom = zdt.plusMonths(3).toInstant();
			zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
			timeTo = zdt.plusMonths(3).toInstant();
		}
		

	}
	
	private int getCompletenessCount(Instant from, Instant to, Boolean all) {
		if (logger.isTraceEnabled()) logger.trace(">>> getCompletenessCount({}, {}, {})", from, to, all);
		int count = 0;
		String sqlString = null;
		sqlString = "SELECT count(*) FROM s1datatake WHERE ";
		if (!all) {
			sqlString += "processing_time IS NOT NULL AND ";
		}
		sqlString += "sensing_start_time >= '" + from.toString() + "' and sensing_start_time < '" + to.toString() + "'";
		Query query = em.createNativeQuery(sqlString);
		Object result = query.getSingleResult();
		if (result != null && result instanceof BigInteger) {
			count = ((BigInteger)result).intValue();
		}
		return count;
	}


    /**
     * Start the monitor thread
     */	
    public void run() {
    	Long wait = (long) 100000;
    	try {
    		if (config.getKpi02CompletenessCycle() != null) {
    			wait = config.getKpi02CompletenessCycle();
    		} else {
    			wait = config.getCycle();
    		}
    	} catch (NumberFormatException e) {
    		wait = (long) 100000;
    	}
    	while (!this.isInterrupted()) {
    		// look for job steps to run

    		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

    		try {
    			// Transaction to check the delete preconditions
    			transactionTemplate.execute((status) -> {						
    				this.checkKpi02Completeness();
    				return null;
    			});
    		} catch (NoResultException e) {
    			logger.error(e.getMessage());
    		} catch (IllegalArgumentException e) {
    			logger.error(e.getMessage());
    		} catch (TransactionException e) {
    			logger.error(e.getMessage());
    		} catch (RuntimeException e) {
    			logger.error(e.getMessage(), e);
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