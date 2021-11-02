package de.dlr.proseo.monitor.rawdata;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.MonRawDataDownlinkDay;
import de.dlr.proseo.model.MonRawDataDownlinkMonth;
import de.dlr.proseo.model.MonRawDataDownload;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.monitor.MonitorConfiguration;

@Transactional
public class MonitorRawdatas extends Thread {
	private static Logger logger = LoggerFactory.getLogger(MonitorRawdatas.class);	

	/** Transaction manager for transaction control */

	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	private MonitorConfiguration config;

	public MonitorRawdatas(MonitorConfiguration config, PlatformTransactionManager txManager) {
		this.config = config;
		this.txManager = txManager;
		this.setName("MonitorRawdatas");
	}
	
	@Transactional
	public void checkRawDatas() {
		Instant now = Instant.now();
		Instant timeFrom;
		Instant timeTo;
		Instant lastEntryDatetime = RepositoryService.getMonRawDataDownlinkDayRepository().findLastDatetime();
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
		Instant timeFromOrig = timeFrom;
		timeTo = timeFrom.plus(1, ChronoUnit.DAYS);
		// loop over missions 
		for (Mission mission : RepositoryService.getMissionRepository().findAll()) {
			// Spacecrafts 
			for (Spacecraft sc : mission.getSpacecrafts()) {
				String spacecraftCode = sc.getCode();
				timeFrom = timeFromOrig;
				timeTo = timeFrom.plus(1, ChronoUnit.DAYS);
				// loop over missing entries
				while (timeFrom.isBefore(now)) {
					if (logger.isTraceEnabled()) logger.trace(">>> Aggregate raw data days for {}, {}", timeFrom, mission.getCode());
					List<MonRawDataDownload> rawDatas = RepositoryService.getMonRawDataDownloadRepository()
						.findByMissionCodeAndSpacecraftCodeAndDownloadTimeBetween(mission.getId(), spacecraftCode,
								timeFrom,
								timeTo
								);
					MonRawDataDownlinkDay downlinkDay = null;
					List<MonRawDataDownlinkDay> downlinkList = RepositoryService.getMonRawDataDownlinkDayRepository()
							.findByMissionIdAndSpacecraftCodeAndDatetime(mission.getId(), spacecraftCode, timeFrom);
					if (downlinkList.isEmpty()) {
						downlinkDay = new MonRawDataDownlinkDay();						
					} else if (downlinkList.size() > 1) {
						// should not be
						// create a warning
						logger.warn("Duplicate entries in MonRawDataDownlinkDay at {}", timeFrom);
						downlinkDay = downlinkList.get(0);
					} else {
						downlinkDay = downlinkList.get(0);
					}
					downlinkDay.setMission(mission);
					downlinkDay.setSpacecraftCode(spacecraftCode);
					downlinkDay.setCount(0);
					downlinkDay.setDownloadSize(0);
					downlinkDay.setDownloadLatencyAvg(0);
					downlinkDay.setDownloadLatencyMin(0);
					downlinkDay.setDownloadLatencyMax(0);
					downlinkDay.setTotalLatencyAvg(0);
					downlinkDay.setTotalLatencyMin(0);
					downlinkDay.setTotalLatencyMax(0);
					downlinkDay.setDatetime(timeFrom);
					int count = 0;
					int fileSize = 0;
					BigInteger downloadLatencySum = BigInteger.valueOf(0);
					int downloadLatencyAvg = 0;
					int downloadLatencyMin = Integer.MAX_VALUE;
					int downloadLatencyMax = 0;
					BigInteger totalLatencySum = BigInteger.valueOf(0);
					int totalLatencyAvg = 0;
					int totalLatencyMin = Integer.MAX_VALUE;
					int totalLatencyMax = 0;
					// now we have an entry
					// analyze products and collect aggregation
					for (MonRawDataDownload rawData : rawDatas) {
						count++;
						int totalLatency = (int)Duration.between(rawData.getDownloadTime(), rawData.getDataStopTime()).toSeconds();
						int  prodLatency;
						
						if (rawData.getRawDataAvailabilityTime() != null) {
							prodLatency = (int)Duration.between(rawData.getDownloadTime(), rawData.getRawDataAvailabilityTime()).toSeconds();
						} else {
							prodLatency = totalLatency;
						}
						downloadLatencySum = downloadLatencySum.add(BigInteger.valueOf(prodLatency));
						downloadLatencyMin = Math.min(downloadLatencyMin, prodLatency);
						downloadLatencyMax = Math.max(downloadLatencyMax, prodLatency);
						totalLatencySum = totalLatencySum.add(BigInteger.valueOf(totalLatency));
						totalLatencyMin = Math.min(totalLatencyMin, totalLatency);
						totalLatencyMax = Math.max(totalLatencyMax, totalLatency);
						fileSize += rawData.getDownloadSize();
					}
					if (count > 0) {
						downloadLatencyAvg = downloadLatencySum.divide(BigInteger.valueOf(count)).intValue();
						totalLatencyAvg = totalLatencySum.divide(BigInteger.valueOf(count)).intValue();
						downlinkDay.setCount(count);
						downlinkDay.setDownloadSize(fileSize);
						downlinkDay.setDownloadLatencyAvg(downloadLatencyAvg);
						downlinkDay.setDownloadLatencyMin(downloadLatencyMin==Integer.MAX_VALUE?0:downloadLatencyMin);
						downlinkDay.setDownloadLatencyMax(downloadLatencyMax);
						downlinkDay.setTotalLatencyAvg(totalLatencyAvg);
						downlinkDay.setTotalLatencyMin(totalLatencyMin==Integer.MAX_VALUE?0:totalLatencyMin);
						downlinkDay.setTotalLatencyMin(totalLatencyMin);
						downlinkDay.setTotalLatencyMax(totalLatencyMax);
					}
					RepositoryService.getMonRawDataDownlinkDayRepository().save(downlinkDay);					
					timeFrom = timeTo;
					timeTo = timeTo.plus(1, ChronoUnit.DAYS);
				}
			}
		}

		// now we have to summarize this into to month table
		lastEntryDatetime = RepositoryService.getMonRawDataDownlinkMonthRepository().findLastDatetime();
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
		ZonedDateTime zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
		int d = zdt.getDayOfMonth() - 1;
		zdt = zdt.minusDays(d);
		timeFrom = zdt.toInstant();
		timeFromOrig = timeFrom;
		timeTo = zdt.plusMonths(1).toInstant();

		// loop over missions and production types 
		for (Mission mission : RepositoryService.getMissionRepository().findAll()) {
			// Spacecrafts 
			for (Spacecraft sc : mission.getSpacecrafts()) {
				String spacecraftCode = sc.getCode();
				timeFrom = timeFromOrig;
				zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
				timeTo = zdt.plusMonths(1).toInstant();
				// loop over missing entries
				while (timeFrom.isBefore(now)) {
					//List<Product> products = RepositoryService.getProductRepository()
					//	.findByMissionCodeAndProductionTypeAndPublicatedAndPublicationTimeBetween(
					if (logger.isTraceEnabled()) logger.trace(">>> Aggregate raw data months for {}, {}, {}", timeFrom, mission.getCode(), spacecraftCode);
					List<MonRawDataDownlinkDay> dayList = RepositoryService.getMonRawDataDownlinkDayRepository()
							.findByMissionIdAndSpacecraftCodeAndDateTimeBetween(mission.getId(),
								spacecraftCode, 
								timeFrom,
								timeTo
								);
					MonRawDataDownlinkMonth rawDataMonth = null;
					List<MonRawDataDownlinkMonth> monthList = RepositoryService.getMonRawDataDownlinkMonthRepository()
							.findByMissionIdAndSpacecraftCodeAndDatetime(mission.getId(), spacecraftCode, timeFrom);
					if (monthList.isEmpty()) {
						rawDataMonth = new MonRawDataDownlinkMonth();						
					} else if (monthList.size() > 1) {
						// should not be
						// create a warning
						logger.warn("Duplicate entries in MonProductProductionDay at {}", timeFrom);
						rawDataMonth = monthList.get(0);
					} else {
						rawDataMonth = monthList.get(0);
					}
					rawDataMonth.setMission(mission);
					rawDataMonth.setSpacecraftCode(spacecraftCode);
					rawDataMonth.setCount(0);
					rawDataMonth.setDownloadSize(0);
					rawDataMonth.setDownloadLatencyAvg(0);
					rawDataMonth.setDownloadLatencyMin(0);
					rawDataMonth.setDownloadLatencyMax(0);
					rawDataMonth.setTotalLatencyAvg(0);
					rawDataMonth.setTotalLatencyMin(0);
					rawDataMonth.setTotalLatencyMax(0);
					rawDataMonth.setDatetime(timeFrom);
					int count = 0;
					int fileSize = 0;
					BigInteger downloadLatencySum = BigInteger.valueOf(0);
					int downloadLatencyAvg = 0;
					int downloadLatencyMin = Integer.MAX_VALUE;
					int downloadLatencyMax = 0;
					BigInteger totalLatencySum = BigInteger.valueOf(0);
					int totalLatencyAvg = 0;
					int totalLatencyMin = Integer.MAX_VALUE;
					int totalLatencyMax = 0;
					// now we have an entry
					// analyze products and collect aggregation
					for (MonRawDataDownlinkDay downlinkDay : dayList) {
						int prodLatency = downlinkDay.getDownloadLatencyAvg();
						int totalLatency = downlinkDay.getTotalLatencyAvg();
						count += downlinkDay.getCount();
						downloadLatencySum = downloadLatencySum.add(BigInteger.valueOf(prodLatency).multiply(BigInteger.valueOf(downlinkDay.getCount())));
						if (downlinkDay.getDownloadLatencyMin() > 0) {
							downloadLatencyMin = Math.min(downloadLatencyMin, downlinkDay.getDownloadLatencyMin());
						}
						downloadLatencyMax = Math.max(downloadLatencyMax, downlinkDay.getDownloadLatencyMax());
						
						totalLatencySum = totalLatencySum.add(BigInteger.valueOf(totalLatency).multiply(BigInteger.valueOf(downlinkDay.getCount())));
						if (downlinkDay.getTotalLatencyMin() > 0) {
							totalLatencyMin = Math.min(totalLatencyMin, downlinkDay.getTotalLatencyMin());
						}
						totalLatencyMax = Math.max(totalLatencyMax, downlinkDay.getTotalLatencyMax());
						fileSize += downlinkDay.getDownloadSize();
					}
					if (count > 0) {
						downloadLatencyAvg = downloadLatencySum.divide(BigInteger.valueOf(count)).intValue();
						totalLatencyAvg = totalLatencySum.divide(BigInteger.valueOf(count)).intValue();
						rawDataMonth.setCount(count);
						rawDataMonth.setDownloadSize(fileSize);
						rawDataMonth.setDownloadLatencyAvg(downloadLatencyAvg);
						rawDataMonth.setDownloadLatencyMin(downloadLatencyMin==Integer.MAX_VALUE?0:downloadLatencyMin);
						rawDataMonth.setDownloadLatencyMax(downloadLatencyMax);
						rawDataMonth.setTotalLatencyAvg(totalLatencyAvg);
						rawDataMonth.setTotalLatencyMin(totalLatencyMin==Integer.MAX_VALUE?0:totalLatencyMin);
						rawDataMonth.setTotalLatencyMax(totalLatencyMax);
					}
					RepositoryService.getMonRawDataDownlinkMonthRepository().save(rawDataMonth);					
					timeFrom = timeTo;
					zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
					timeTo = zdt.plusMonths(1).toInstant();
				}
			}
		}
		
		
		
	}
	
    public void run() {
    	Long wait = (long) 100000;
    	try {
    		if (config.getProductCycle() != null) {
    			wait = config.getRawdataCycle();
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
    				this.checkRawDatas();
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
