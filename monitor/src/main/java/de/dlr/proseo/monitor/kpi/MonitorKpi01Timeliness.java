package de.dlr.proseo.monitor.kpi;

import java.math.BigInteger;
import java.time.Duration;
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
import de.dlr.proseo.model.MonKpi01TimelinessQuarter;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.monitor.MonitorConfiguration;
import de.dlr.proseo.monitor.MonitorConfiguration.Timeliness;

/**
 * The thread monitoring KPI01 timeliness
 * 
 * @author Melchinger
 *
 */
@Transactional
public class MonitorKpi01Timeliness extends Thread {
	private static Logger logger = LoggerFactory.getLogger(MonitorKpi01Timeliness.class);	

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
	public MonitorKpi01Timeliness(MonitorConfiguration config, PlatformTransactionManager txManager, EntityManager em) {
		this.config = config;
		this.txManager = txManager;
		this.em = em;
		this.setName("MonitorKpi01");
	}

	/**
	 * Collect the monitoring information of raw data for day and month
	 */
	@Transactional
	public void checkKpi01Timeliness() {
		
		Instant now = Instant.now();
		Instant timeFrom;
		Instant timeTo;
		Instant lastEntryDatetime = RepositoryService.getMonKpi01TimelinessMonthRepository().findLastDatetime();
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
			int countAll = getTimelinessCount(timeFrom, timeTo, null, null);
			int countSuccess = 0;
			for (Timeliness timeliness : config.getTimeliness()) {
				countSuccess += getTimelinessCount(timeFrom, timeTo, timeliness.getMode(), Duration.ofMinutes(timeliness.getMinutes()));
			}
			MonKpi01TimelinessMonth tm;
			try {
				tm = RepositoryService.getMonKpi01TimelinessMonthRepository().findByDateTimeBetween(timeFrom, timeTo).get(0);
			} catch (IndexOutOfBoundsException ex) {
				tm = new MonKpi01TimelinessMonth();
			}
			tm.setCountSuccessful(countSuccess);
			tm.setCountAll(countAll);
			tm.setDatetime(timeFrom);
			RepositoryService.getMonKpi01TimelinessMonthRepository().save(tm);
			zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
			timeFrom = zdt.plusMonths(1).toInstant();
			zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
			timeTo = zdt.plusMonths(1).toInstant();
		}
		// quarter
		lastEntryDatetime = RepositoryService.getMonKpi01TimelinessQuarterRepository().findLastDatetime();
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
			int countAll = getTimelinessCount(timeFrom, timeTo, null, null);
			int countSuccess = 0;
			for (Timeliness timeliness : config.getTimeliness()) {
				countSuccess += getTimelinessCount(timeFrom, timeTo, timeliness.getMode(), Duration.ofMinutes(timeliness.getMinutes()));
			}
			MonKpi01TimelinessQuarter tq;
			try {
				tq = RepositoryService.getMonKpi01TimelinessQuarterRepository().findByDateTimeBetween(timeFrom, timeTo).get(0);
			} catch (IndexOutOfBoundsException ex) {
				tq = new MonKpi01TimelinessQuarter();
			}
			tq.setCountSuccessful(countSuccess);
			tq.setCountAll(countAll);
			tq.setDatetime(timeFrom);
			RepositoryService.getMonKpi01TimelinessQuarterRepository().save(tq);
			zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
			timeFrom = zdt.plusMonths(3).toInstant();
			zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
			timeTo = zdt.plusMonths(3).toInstant();
		}
		

	}
	
	private int getTimelinessCount(Instant from, Instant to, String mode, Duration timeliness) {
		if (logger.isTraceEnabled()) logger.trace(">>> getTimelinessCount({}, {}, {}, {})", from, to, mode, timeliness);
		int count = 0;
		String sqlString = null;
		sqlString = "SELECT count(*) FROM product p "
				+ "JOIN product_parameters pp ON (p.id = pp.product_id and pp.parameters_key = 'datatakeID') "
				+ "JOIN s1datatake dt ON CAST(pp.parameter_value AS BIGINT) = dt.datatake_id "
				+ "WHERE p.publication_time is not null and p.raw_data_availability_time is not null and "
				+ "p.publication_time >= '" + from.toString() + "' and p.publication_time < '" + to.toString() + "'";
		if (mode == null) {
			sqlString += " AND dt.timeliness is not null";
		} else {
			sqlString += " AND dt.timeliness = '" + mode + "' AND p.publication_time <= (p.raw_data_availability_time + interval '" + timeliness.toMinutes() + " mins')";
		}
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
    		if (config.getKpi01TimelinessCycle() != null) {
    			wait = config.getKpi01TimelinessCycle();
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
    				this.checkKpi01Timeliness();
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