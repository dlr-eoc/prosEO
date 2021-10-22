package de.dlr.proseo.monitor.product;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import de.dlr.proseo.model.MonProductProductionHour;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.monitor.MonitorConfiguration;

@Transactional
public class MonitorProducts extends Thread {
	private static Logger logger = LoggerFactory.getLogger(MonitorProducts.class);	

	/** Transaction manager for transaction control */

	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	private MonitorConfiguration config;

	public MonitorProducts(MonitorConfiguration config, PlatformTransactionManager txManager) {
		this.config = config;
		this.txManager = txManager;
	}
	
	@Transactional
	public void checkProducts() {
		Instant now = Instant.now();
		Instant timeFrom;
		Instant timeTo;
		Instant lastEntryDatetime = RepositoryService.getMonProductProductionHourRepository().findLastDatetime();
		if (lastEntryDatetime ==  null) {
			// no entry found, begin at now.
			timeFrom = now.truncatedTo(ChronoUnit.HOURS);
		} else {
			timeFrom = lastEntryDatetime.truncatedTo(ChronoUnit.HOURS);
		}
		Instant timeFromOrig = timeFrom;
		timeTo = timeFrom.plus(1, ChronoUnit.HOURS);
		// loop over missions and production types 
		for (Mission mission : RepositoryService.getMissionRepository().findAll()) {
			for (ProductionType mpt : ProductionType.values()) {
				// MonProductionType monProductionType = monProductionTypes.get(mpt);
				timeFrom = timeFromOrig;
				timeTo = timeFrom.plus(1, ChronoUnit.HOURS);
				// loop over missing entries
				while (timeFrom.isBefore(now)) {
					List<Product> products = RepositoryService.getProductRepository()
						.findByMissionCodeAndProductionTypeAndPublicatedAndPublicationTimeBetween(mission.getCode(), 
								mpt, 
								timeFrom,
								timeTo
								);
					MonProductProductionHour mppd = null;
					List<MonProductProductionHour> mppdList = RepositoryService.getMonProductProductionHourRepository().findByProductionTypeAndDatetime(mission.getId(), mpt.getValue(), timeFrom);
					if (mppdList.isEmpty()) {
						mppd = new MonProductProductionHour();						
					} else if (mppdList.size() > 1) {
						// should not be
						// create a warning
						logger.warn("Duplicate entries in MonProductProductionHour at {}", timeFrom);
						mppd = mppdList.get(0);
					} else {
						mppd = mppdList.get(0);
					}
					mppd.setMission(mission);
					mppd.setMonProductionType(mpt.getValue());
					mppd.setCount(0);
					mppd.setFileSize(0);
					mppd.setProductionLatencyAvg(0);
					mppd.setProductionLatencyMin(0);
					mppd.setProductionLatencyMax(0);
					mppd.setTotalLatencyAvg(0);
					mppd.setTotalLatencyMin(0);
					mppd.setTotalLatencyMax(0);
					mppd.setDatetime(timeFrom);
					int count = 0;
					int fileSize = 0;
					int productionLatencySum = 0;
					int productionLatencyAvg = 0;
					int productionLatencyMin = Integer.MAX_VALUE;
					int productionLatencyMax = 0;
					int totalLatencySum = 0;
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
						productionLatencySum += prodLatency;
						productionLatencyMin = Math.min(productionLatencyMin, prodLatency);
						productionLatencyMax = Math.max(productionLatencyMax, totalLatency);
						totalLatencySum += totalLatency;
						totalLatencyMin = Math.min(totalLatencyMin, prodLatency);
						totalLatencyMax = Math.max(totalLatencyMax, totalLatency);
						for (ProductFile f : product.getProductFile()) {
							fileSize += f.getFileSize();
						}
					}
					if (count > 0) {
						productionLatencyAvg = productionLatencySum / count;
						totalLatencyAvg = totalLatencySum / count;
						mppd.setCount(count);
						mppd.setFileSize(fileSize);
						mppd.setProductionLatencyAvg(productionLatencyAvg);
						mppd.setProductionLatencyMin(productionLatencyMin);
						mppd.setProductionLatencyMax(productionLatencyMax);
						mppd.setTotalLatencyAvg(totalLatencyAvg);
						mppd.setTotalLatencyMin(totalLatencyMin);
						mppd.setTotalLatencyMax(totalLatencyMax);
					}
					RepositoryService.getMonProductProductionHourRepository().save(mppd);					
					timeFrom = timeTo;
					timeTo = timeTo.plus(1, ChronoUnit.HOURS);
				}
			}
		}
		
	}
	
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

    		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

    		try {
    			// Transaction to check the delete preconditions
    			transactionTemplate.execute((status) -> {						
    				this.checkProducts();
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
