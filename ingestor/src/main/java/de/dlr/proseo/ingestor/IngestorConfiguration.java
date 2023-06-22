/**
 * IngestorConfiguration.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.IngestorMessage;

/**
 * Configuration class for the prosEO Ingestor component
 *
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix = "proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class IngestorConfiguration {

	// Default connection timeout is 30 s
	private static final Long DEFAULT_TIMEOUT = 30000L;
	// Default validity period for Storage Manager download tokens is 60 s
	private static final Long DEFAULT_VALIDITY = 60000L;

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

	/** Shared secret for Storage Manager download tokens */
	@Value("${proseo.storageManager.secret}")
	private String storageManagerSecret;

	/** Validity period for Storage Manager download tokens */
	@Value("${proseo.storageManager.validity}")
	private String storageManagerValidity;
	private Long storageManagerValidityLong = null;

	/** Wait time for cleanup */
	@Value("${proseo.ingestor.cleanupCycleTime}")
	private Integer cleanupCycleTime;

	/** Notify Production Planner upon product ingestion */
	@Value("${proseo.ingestor.notifyPlanner:true}")
	private Boolean notifyPlanner;

	/** The maximum number of results to be retrieved by REST requests */
	@Value("${spring.maxResults}")
	public Integer maxResults;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(IngestorConfiguration.class);

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
					logger.log(IngestorMessage.INVALID_TIMEOUT, productionPlannerTimeout, DEFAULT_TIMEOUT);
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
					logger.log(IngestorMessage.INVALID_TIMEOUT, storageManagerTimeout, DEFAULT_TIMEOUT);
					storageManagerTimeoutLong = DEFAULT_TIMEOUT;
				}
			}
		}
		return storageManagerTimeoutLong.longValue();
	}

	/**
	 * Gets the shared secret for generating Storage Manager download tokens as
	 * 256-bit byte array
	 *
	 * @return the Storage Manager secret
	 */
	public byte[] getStorageManagerSecret() {
		byte[] sharedSecret = Arrays.copyOf((storageManagerSecret + "                ").getBytes(), 32);
		return sharedSecret;
	}

	/**
	 * Gets the validity period in milliseconds for Storage Manager download tokens
	 *
	 * @return the Storage Manager token validity
	 */
	public long getStorageManagerTokenValidity() {
		if (null == storageManagerValidityLong) {
			if (null == storageManagerValidity) {
				storageManagerValidityLong = DEFAULT_VALIDITY;
			} else {
				try {
					storageManagerValidityLong = Long.parseLong(storageManagerValidity);
				} catch (NumberFormatException e) {
					logger.log(IngestorMessage.INVALID_VALIDITY, storageManagerValidity, DEFAULT_VALIDITY);
					storageManagerValidityLong = DEFAULT_VALIDITY;
				}
			}
		}
		return storageManagerValidityLong.longValue();
	}

	/**
	 * Gets the order cleanup cycle
	 *
	 * @return the order cleanup cycle (in days)
	 */
	public Integer getCleanupCycleTime() {
		return cleanupCycleTime;
	}

	/**
	 * Indicates whether the Production Planner should be notified about new product
	 * ingestions
	 *
	 * @return true, if Planner notification is requested, false otherwise
	 */
	public Boolean getNotifyPlanner() {
		return notifyPlanner;
	}

	/**
	 * Gets the maximum number of results to be retrieved by REST requests
	 * 
	 * @return the maximum number of results to be retrieved by REST requests
	 */
	public Integer getMaxResults() {
		return maxResults;
	}

}
