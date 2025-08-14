package de.dlr.proseo.monitor.product;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.MonitorMessage;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.MonProductProductionDay;
import de.dlr.proseo.model.MonProductProductionHour;
import de.dlr.proseo.model.MonProductProductionMonth;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.monitor.MonitorConfiguration;

/**
 * The thread monitoring the products
 * 
 * @author Melchinger
 *
 */
public class MonitorProducts extends Thread {
	private static ProseoLogger logger = new ProseoLogger(MonitorProducts.class);	

	/** Transaction manager for transaction control */

	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/**
	 * The monitor configuration (application.yml) 
	 */
	private MonitorConfiguration config;

	/**
	 * Instantiate the monitor products thread
	 * 
	 * @param config The monitor configuration
	 * @param txManager The transaction manager
	 */
	public MonitorProducts(MonitorConfiguration config, PlatformTransactionManager txManager) {
		this.config = config;
		this.txManager = txManager;
		this.setName("MonitorProducts");
	}

	/**
	 * Collect the monitoring information of production for hour, day and month
	 */
	public void checkProducts() {
		Instant now = Instant.now();
		Instant timeFrom;
		Instant timeTo;
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);
		Instant lastEntryDatetime = transactionTemplate.execute((status) -> {			
			return RepositoryService.getMonProductProductionHourRepository().findLastDatetime();
		});
		if (lastEntryDatetime ==  null) {
			// no entry found, begin at now.
			timeFrom = now.truncatedTo(ChronoUnit.HOURS);
			if (config.getAggregationStart() != null) {
				try {
					timeFrom = Instant.parse(config.getAggregationStart()).truncatedTo(ChronoUnit.HOURS);
				} catch (DateTimeParseException ex) {
					logger.log(MonitorMessage.ILLEGAL_CONFIG_VALUE, config.getAggregationStart());
				}
			} 
		} else {
			timeFrom = lastEntryDatetime.truncatedTo(ChronoUnit.HOURS);
		}
		Instant timeFromOrig = timeFrom;
		timeTo = timeFrom.plus(1, ChronoUnit.HOURS);
		List<Mission> missions = transactionTemplate.execute((status) -> {			
			return RepositoryService.getMissionRepository().findAll();
		});
		// loop over missions and production types 
		for (Mission mission : missions) {
			for (ProductionType mpt : ProductionType.values()) {
				// ProductionType productionType = monProductionTypes.get(mpt);
				timeFrom = timeFromOrig;
				timeTo = timeFrom.plus(1, ChronoUnit.HOURS);
				// loop over missing entries
				transactionTemplate.setReadOnly(false);
				while (timeFrom.isBefore(now)) {
					if (logger.isTraceEnabled()) logger.trace(">>> Aggregate Hours for {}, {}, {}", timeFrom, mission.getCode(), mpt.getValue());
					transactionTemplate.setReadOnly(false);
					final Instant timeFromX = timeFrom;
					final Instant timeToX = timeTo;
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							transactionTemplate.execute((status) -> {			
								List<Product> products = RepositoryService.getProductRepository()
										.findByMissionCodeAndProductionTypeAndPublicatedAndPublicationTimeBetween(mission.getCode(), 
												mpt, 
												timeFromX,
												timeToX
												);
								MonProductProductionHour mppd = null;
								List<MonProductProductionHour> mppdList = RepositoryService.getMonProductProductionHourRepository()
										.findByProductionTypeAndDatetime(mission.getId(), mpt.getValue(), timeFromX);
								if (mppdList.isEmpty()) {
									mppd = new MonProductProductionHour();						
								} else if (mppdList.size() > 1) {
									// should not be
									// create a warning
									logger.log(MonitorMessage.DUPLICATE_ENTRIES, "MonProductProductionHour", timeFromX);
									mppd = mppdList.get(0);
								} else {
									mppd = mppdList.get(0);
								}
								mppd.setMission(mission);
								mppd.setProductionType(mpt.getValue());
								mppd.setCount(0);
								mppd.setFileSize(0);
								mppd.setProductionLatencyAvg(0);
								mppd.setProductionLatencyMin(0);
								mppd.setProductionLatencyMax(0);
								mppd.setTotalLatencyAvg(0);
								mppd.setTotalLatencyMin(0);
								mppd.setTotalLatencyMax(0);
								mppd.setDatetime(timeFromX);
								int count = 0;
								BigInteger fileSize = BigInteger.valueOf(0);
								BigInteger productionLatencySum = BigInteger.valueOf(0);
								int productionLatencyAvg = 0;
								int productionLatencyMin = Integer.MAX_VALUE;
								int productionLatencyMax = 0;
								BigInteger totalLatencySum = BigInteger.valueOf(0);
								int totalLatencyAvg = 0;
								int totalLatencyMin = Integer.MAX_VALUE;
								int totalLatencyMax = 0;
								// now we have an entry
								// analyze products and collect aggregation
								for (Product product : products) {
									count++;
									int totalLatency = (int)Duration.between(product.getSensingStopTime(), product.getPublicationTime()).toSeconds();
									int  prodLatency;

									if (product.getRawDataAvailabilityTime() != null) {
										prodLatency = (int)Duration.between(product.getRawDataAvailabilityTime(), product.getPublicationTime()).toSeconds();
									} else {
										prodLatency = totalLatency;
									}
									productionLatencySum = productionLatencySum.add(BigInteger.valueOf(prodLatency));
									productionLatencyMin = Math.min(productionLatencyMin, prodLatency);
									productionLatencyMax = Math.max(productionLatencyMax, prodLatency);
									totalLatencySum = totalLatencySum.add(BigInteger.valueOf(totalLatency));
									totalLatencyMin = Math.min(totalLatencyMin, totalLatency);
									totalLatencyMax = Math.max(totalLatencyMax, totalLatency);
									for (ProductFile f : product.getProductFile()) {
										fileSize = fileSize.add(BigInteger.valueOf(f.getFileSize()));
									}
								}
								if (count > 0) {
									BigInteger bix = productionLatencySum.divide(BigInteger.valueOf(count));
									if (bix.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) >= 0) {
										logger.log(MonitorMessage.WRONG_PRODUCTION_LATENCY, bix.toString());
									}
									productionLatencyAvg = bix.intValue();
									bix = totalLatencySum.divide(BigInteger.valueOf(count));
									if (bix.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) >= 0) {
										logger.log(MonitorMessage.WRONG_TOTAL_LATENCY, bix.toString());
									}
									totalLatencyAvg = bix.intValue();
									if (fileSize.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) >= 0) {
										logger.log(MonitorMessage.WRONG_DOWNLOAD_SIZE, fileSize.toString());
									}
									mppd.setCount(count);
									mppd.setFileSize(fileSize.longValue());
									mppd.setProductionLatencyAvg(productionLatencyAvg);
									mppd.setProductionLatencyMin(productionLatencyMin==Integer.MAX_VALUE?0:productionLatencyMin);
									mppd.setProductionLatencyMax(productionLatencyMax);
									mppd.setTotalLatencyAvg(totalLatencyAvg);
									mppd.setTotalLatencyMin(totalLatencyMin==Integer.MAX_VALUE?0:totalLatencyMin);
									mppd.setTotalLatencyMin(totalLatencyMin);
									mppd.setTotalLatencyMax(totalLatencyMax);
								}
								RepositoryService.getMonProductProductionHourRepository().save(mppd);	
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
					timeTo = timeTo.plus(1, ChronoUnit.HOURS);
				}
			}
		}
		// now we have to summarize this into to day table
		transactionTemplate.setReadOnly(true);
		lastEntryDatetime = transactionTemplate.execute((status) -> {			
			return RepositoryService.getMonProductProductionDayRepository().findLastDatetime();
		});
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
		timeFromOrig = timeFrom;
		timeTo = timeFrom.plus(1, ChronoUnit.DAYS);

		// loop over missions and production types 
		transactionTemplate.setReadOnly(false);
		for (Mission mission : missions) {
			for (ProductionType mpt : ProductionType.values()) {
				// ProductionType productionType = monProductionTypes.get(mpt);
				timeFrom = timeFromOrig;
				timeTo = timeFrom.plus(1, ChronoUnit.DAYS);
				// loop over missing entries
				while (timeFrom.isBefore(now)) {
					//List<Product> products = RepositoryService.getProductRepository()
					//	.findByMissionCodeAndProductionTypeAndPublicatedAndPublicationTimeBetween(
					if (logger.isTraceEnabled()) logger.trace(">>> Aggregate Days for {}, {}, {}", timeFrom, mission.getCode(), mpt.getValue());
					transactionTemplate.setReadOnly(false);
					final Instant timeFromX = timeFrom;
					final Instant timeToX = timeTo;
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							transactionTemplate.execute((status) -> {			
								List<MonProductProductionHour> hourList = RepositoryService.getMonProductProductionHourRepository()
										.findByMissionCodeAndProductionTypeAndDateTimeBetween(mission.getId(),
												mpt.getValue(), 
												timeFromX,
												timeToX
												);
								MonProductProductionDay mppd = null;
								List<MonProductProductionDay> mppdList = RepositoryService.getMonProductProductionDayRepository()
										.findByProductionTypeAndDatetime(mission.getId(), mpt.getValue(), timeFromX);
								if (mppdList.isEmpty()) {
									mppd = new MonProductProductionDay();						
								} else if (mppdList.size() > 1) {
									// should not be
									// create a warning
									logger.log(MonitorMessage.DUPLICATE_ENTRIES, "MonProductProductionDay", timeFromX);
									mppd = mppdList.get(0);
								} else {
									mppd = mppdList.get(0);
								}
								mppd.setMission(mission);
								mppd.setProductionType(mpt.getValue());
								mppd.setCount(0);
								mppd.setFileSize(0);
								mppd.setProductionLatencyAvg(0);
								mppd.setProductionLatencyMin(0);
								mppd.setProductionLatencyMax(0);
								mppd.setTotalLatencyAvg(0);
								mppd.setTotalLatencyMin(0);
								mppd.setTotalLatencyMax(0);
								mppd.setDatetime(timeFromX);
								int count = 0;
								BigInteger fileSize = BigInteger.valueOf(0);
								BigInteger productionLatencySum = BigInteger.valueOf(0);
								int productionLatencyAvg = 0;
								int productionLatencyMin = Integer.MAX_VALUE;
								int productionLatencyMax = 0;
								BigInteger totalLatencySum = BigInteger.valueOf(0);
								int totalLatencyAvg = 0;
								int totalLatencyMin = Integer.MAX_VALUE;
								int totalLatencyMax = 0;
								// now we have an entry
								// analyze products and collect aggregation
								for (MonProductProductionHour productProduction : hourList) {
									int prodLatency = productProduction.getProductionLatencyAvg();
									int totalLatency = productProduction.getTotalLatencyAvg();
									count += productProduction.getCount();
									productionLatencySum = productionLatencySum.add(BigInteger.valueOf(prodLatency).multiply(BigInteger.valueOf(productProduction.getCount())));
									if (productProduction.getProductionLatencyMin() > 0) {
										productionLatencyMin = Math.min(productionLatencyMin, productProduction.getProductionLatencyMin());
									}
									productionLatencyMax = Math.max(productionLatencyMax, productProduction.getProductionLatencyMax());

									totalLatencySum = totalLatencySum.add(BigInteger.valueOf(totalLatency).multiply(BigInteger.valueOf(productProduction.getCount())));
									if (productProduction.getTotalLatencyMin() > 0) {
										totalLatencyMin = Math.min(totalLatencyMin, productProduction.getTotalLatencyMin());
									}
									totalLatencyMax = Math.max(totalLatencyMax, productProduction.getTotalLatencyMax());
									fileSize = fileSize.add(BigInteger.valueOf(productProduction.getFileSize()));
								}
								if (count > 0) {
									BigInteger bix = productionLatencySum.divide(BigInteger.valueOf(count));
									if (bix.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) >= 0) {
										logger.log(MonitorMessage.WRONG_PRODUCTION_LATENCY, bix.toString());
									}
									productionLatencyAvg = bix.intValue();
									bix = totalLatencySum.divide(BigInteger.valueOf(count));
									if (bix.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) >= 0) {
										logger.log(MonitorMessage.WRONG_TOTAL_LATENCY, bix.toString());
									}
									totalLatencyAvg = bix.intValue();
									if (fileSize.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) >= 0) {
										logger.log(MonitorMessage.WRONG_DOWNLOAD_SIZE, fileSize.toString());
									}
									mppd.setCount(count);
									mppd.setFileSize(fileSize.longValue());
									mppd.setProductionLatencyAvg(productionLatencyAvg);
									mppd.setProductionLatencyMin(productionLatencyMin==Integer.MAX_VALUE?0:productionLatencyMin);
									mppd.setProductionLatencyMax(productionLatencyMax);
									mppd.setTotalLatencyAvg(totalLatencyAvg);
									mppd.setTotalLatencyMin(totalLatencyMin==Integer.MAX_VALUE?0:totalLatencyMin);
									mppd.setTotalLatencyMax(totalLatencyMax);
								}
								RepositoryService.getMonProductProductionDayRepository().save(mppd);		
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
		}

		// now we have to summarize this into to month table
		transactionTemplate.setReadOnly(true);
		lastEntryDatetime = transactionTemplate.execute((status) -> {			
			return RepositoryService.getMonProductProductionMonthRepository().findLastDatetime();
		});
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
		int d = zdt.getDayOfMonth() - 1;
		zdt = zdt.minusDays(d);
		timeFrom = zdt.toInstant();
		timeFromOrig = timeFrom;
		timeTo = zdt.plusMonths(1).toInstant();

		// loop over missions and production types 
		transactionTemplate.setReadOnly(false);
		for (Mission mission : missions) {
			for (ProductionType mpt : ProductionType.values()) {
				// ProductionType productionType = monProductionTypes.get(mpt);
				timeFrom = timeFromOrig;
				zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
				timeTo = zdt.plusMonths(1).toInstant();
				// loop over missing entries
				while (timeFrom.isBefore(now)) {
					//List<Product> products = RepositoryService.getProductRepository()
					//	.findByMissionCodeAndProductionTypeAndPublicatedAndPublicationTimeBetween(
					if (logger.isTraceEnabled()) logger.trace(">>> Aggregate Months for {}, {}, {}", timeFrom, mission.getCode(), mpt.getValue());
					final Instant timeFromX = timeFrom;
					final Instant timeToX = timeTo;
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							transactionTemplate.execute((status) -> {			
								List<MonProductProductionDay> dayList = RepositoryService.getMonProductProductionDayRepository()
										.findByMissionCodeAndProductionTypeAndDateTimeBetween(mission.getId(),
												mpt.getValue(), 
												timeFromX,
												timeToX
												);
								MonProductProductionMonth mppd = null;
								List<MonProductProductionMonth> mppdList = RepositoryService.getMonProductProductionMonthRepository()
										.findByProductionTypeAndDatetime(mission.getId(), mpt.getValue(), timeFromX);
								if (mppdList.isEmpty()) {
									mppd = new MonProductProductionMonth();						
								} else if (mppdList.size() > 1) {
									// should not be
									// create a warning
									logger.log(MonitorMessage.DUPLICATE_ENTRIES, "MonProductProductionDay", timeFromX);
									mppd = mppdList.get(0);
								} else {
									mppd = mppdList.get(0);
								}
								mppd.setMission(mission);
								mppd.setProductionType(mpt.getValue());
								mppd.setCount(0);
								mppd.setFileSize(0);
								mppd.setProductionLatencyAvg(0);
								mppd.setProductionLatencyMin(0);
								mppd.setProductionLatencyMax(0);
								mppd.setTotalLatencyAvg(0);
								mppd.setTotalLatencyMin(0);
								mppd.setTotalLatencyMax(0);
								mppd.setDatetime(timeFromX);
								int count = 0;
								BigInteger fileSize = BigInteger.valueOf(0);
								BigInteger productionLatencySum = BigInteger.valueOf(0);
								int productionLatencyAvg = 0;
								int productionLatencyMin = Integer.MAX_VALUE;
								int productionLatencyMax = 0;
								BigInteger totalLatencySum = BigInteger.valueOf(0);
								int totalLatencyAvg = 0;
								int totalLatencyMin = Integer.MAX_VALUE;
								int totalLatencyMax = 0;
								// now we have an entry
								// analyze products and collect aggregation
								for (MonProductProductionDay productProduction : dayList) {
									int prodLatency = productProduction.getProductionLatencyAvg();
									int totalLatency = productProduction.getTotalLatencyAvg();
									count += productProduction.getCount();
									productionLatencySum = productionLatencySum.add(BigInteger.valueOf(prodLatency).multiply(BigInteger.valueOf(productProduction.getCount())));
									if (productProduction.getProductionLatencyMin() > 0) {
										productionLatencyMin = Math.min(productionLatencyMin, productProduction.getProductionLatencyMin());
									}
									productionLatencyMax = Math.max(productionLatencyMax, productProduction.getProductionLatencyMax());

									totalLatencySum = totalLatencySum.add(BigInteger.valueOf(totalLatency).multiply(BigInteger.valueOf(productProduction.getCount())));
									if (productProduction.getTotalLatencyMin() > 0) {
										totalLatencyMin = Math.min(totalLatencyMin, productProduction.getTotalLatencyMin());
									}
									totalLatencyMax = Math.max(totalLatencyMax, productProduction.getTotalLatencyMax());
									fileSize = fileSize.add(BigInteger.valueOf(productProduction.getFileSize()));
								}
								if (count > 0) {
									BigInteger bix = productionLatencySum.divide(BigInteger.valueOf(count));
									if (bix.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) >= 0) {
										logger.log(MonitorMessage.WRONG_PRODUCTION_LATENCY, bix.toString());
									}
									productionLatencyAvg = bix.intValue();
									bix = totalLatencySum.divide(BigInteger.valueOf(count));
									if (bix.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) >= 0) {
										logger.log(MonitorMessage.WRONG_TOTAL_LATENCY, bix.toString());
									}
									totalLatencyAvg = bix.intValue();
									if (fileSize.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) >= 0) {
										logger.log(MonitorMessage.WRONG_DOWNLOAD_SIZE, fileSize.toString());
									}
									mppd.setCount(count);
									mppd.setFileSize(fileSize.longValue());
									mppd.setProductionLatencyAvg(productionLatencyAvg);
									mppd.setProductionLatencyMin(productionLatencyMin==Integer.MAX_VALUE?0:productionLatencyMin);
									mppd.setProductionLatencyMax(productionLatencyMax);
									mppd.setTotalLatencyAvg(totalLatencyAvg);
									mppd.setTotalLatencyMin(totalLatencyMin==Integer.MAX_VALUE?0:totalLatencyMin);
									mppd.setTotalLatencyMax(totalLatencyMax);
								}
								RepositoryService.getMonProductProductionMonthRepository().save(mppd);			
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
					zdt = ZonedDateTime.ofInstant(timeFrom, ZoneId.of("UTC"));
					timeTo = zdt.plusMonths(1).toInstant();
				}
			}
		}
	}
	

    /**
     * Start the monitor thread
     */
    public void run() {
    	Long wait = (long) 100000;
    	try {
    		if (config.getProductCycle() != null) {
    			wait = config.getProductCycle();
    		} else {
    			wait = config.getCycle();
    		}
    	} catch (NumberFormatException e) {
    		wait = (long) 100000;
    	}
    	while (!this.isInterrupted()) {
    		// look for job steps to run

    		try {
    			// Transaction to check the delete preconditions			
    			this.checkProducts();
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
