/**
 * IngestorConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Ingestor component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class IngestorConfiguration {
	
	private static final int MSG_ID_INVALID_TIMEOUT = 2079;
	private static final String MSG_INVALID_TIMEOUT = "(I%d) Invalid timeout value %s found in configuration, using default %d";
	
	// Default connection timeout is 30 s
	private static final Long DEFAULT_TIMEOUT = 30000L;
	
	/** The URL of the prosEO Production Planner */
	@Value("${proseo.productionPlanner.url}")
	private String productionPlannerUrl;
	
	/** Connection timeout [ms] for Production Planner */
	@Value("${proseo.productionPlanner.timeout}")
	private String productionPlannerTimeout;
	private Long productionPlannerTimeoutLong = null;
	
	/** Connection timeout [ms] for Storage Manager */
	@Value("${proseo.storageManager.timeout}")
	private String storageManagerTimeout;
	private Long storageManagerTimeoutLong = null;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(IngestorConfiguration.class);
	
	/**
	 * Gets the URL of the prosEO Production Planner component
	 * 
	 * @return the productionPlannerUrl the URL of the Production Planner
	 */
	public String getProductionPlannerUrl() {
		return productionPlannerUrl;
	}

	/**
	 * Gets the timeout in milliseconds for Production Planner connections
	 * 
	 * @return the productionPlannerTimeout
	 */
	public long getProductionPlannerTimeout() {
		if (null == productionPlannerTimeoutLong) {
			if (null == productionPlannerTimeout) {
				productionPlannerTimeoutLong = DEFAULT_TIMEOUT;
			} else {
				try {
					productionPlannerTimeoutLong = Long.parseLong(productionPlannerTimeout);
				} catch (NumberFormatException e) {
					logger.warn(String.format(MSG_INVALID_TIMEOUT, MSG_ID_INVALID_TIMEOUT, productionPlannerTimeout, DEFAULT_TIMEOUT));
					productionPlannerTimeoutLong = DEFAULT_TIMEOUT;
				} 
			}
		}
		return productionPlannerTimeoutLong.longValue();
	}

	/**
	 * Gets the timeout in milliseconds for Storage Manager connections
	 * 
	 * @return the Storage Manager timeout
	 */
	public long getStorageManagerTimeout() {
		if (null == storageManagerTimeoutLong) {
			if (null == storageManagerTimeout) {
				storageManagerTimeoutLong = DEFAULT_TIMEOUT;
			} else {
				try {
					storageManagerTimeoutLong = Long.parseLong(storageManagerTimeout);
				} catch (NumberFormatException e) {
					logger.warn(String.format(MSG_INVALID_TIMEOUT, MSG_ID_INVALID_TIMEOUT, storageManagerTimeout, DEFAULT_TIMEOUT));
					storageManagerTimeoutLong = DEFAULT_TIMEOUT;
				} 
			}
		}
		return storageManagerTimeoutLong.longValue();
	}

}
