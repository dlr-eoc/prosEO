/**
 * OrdermgrConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Order Manager component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class OrdermgrConfiguration {

	private static final int MSG_ID_INVALID_TIMEOUT = 2079;
	private static final String MSG_INVALID_TIMEOUT = "(I%d) Invalid timeout value %s found in configuration, using default %d";
	
	// Default connection timeout is 30 s
	private static final Long DEFAULT_TIMEOUT = 30000L;
	

	/** Wait time for cleanup */
	private Integer cleanupCycleTime;

	/** The URL of the prosEO Ingestor */
	@Value("${proseo.ingestor.url}")
	private String ingestorUrl;
	
	/** Connection timeout [ms] for Ingestor */
	@Value("${proseo.ingestor.timeout}")
	private String ingestorTimeout;
	private Long ingestorTimeoutLong = null;
	
	/** User for Ingestor connections (must have PRODUCT_GENERATOR role) */
	@Value("${proseo.ingestor.user}")
	private String ingestorUser;
	
	/** Password for Ingestor connections */
	@Value("${proseo.ingestor.password}")
	private String ingestorPassword;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrdermgrConfiguration.class);
	
	
	/**
	 * @return the cleanupCycleTime
	 */
	public Integer getCleanupCycleTime() {
		return cleanupCycleTime;
	}

	/**
	 * @param cleanupCycleTime the cleanupCycleTime to set
	 */
	public void setCleanupCycleTime(Integer cleanupCycleTime) {
		this.cleanupCycleTime = cleanupCycleTime;
	}

	/**
	 * Gets the URL of the prosEO Ingestor component
	 * 
	 * @return the Ingestor URL
	 */
	public String getIngestorUrl() {
		return ingestorUrl;
	}
	
	/**
	 * Gets the timeout in milliseconds for Ingestor connections
	 * 
	 * @return the ingestorTimeout
	 */
	public long getIngestorTimeout() {
		if (null == ingestorTimeoutLong) {
			if (null == ingestorTimeout) {
				ingestorTimeoutLong = DEFAULT_TIMEOUT;
			} else {
				try {
					ingestorTimeoutLong = Long.parseLong(ingestorTimeout);
				} catch (NumberFormatException e) {
					logger.warn(String.format(MSG_INVALID_TIMEOUT, MSG_ID_INVALID_TIMEOUT, ingestorTimeout, DEFAULT_TIMEOUT));
					ingestorTimeoutLong = DEFAULT_TIMEOUT;
				} 
			}
		}
		return ingestorTimeoutLong.longValue();
	}

	/**
	 * Gets the user for the prosEO Ingestor component
	 * 
	 * @return the Ingestor user
	 */
	public String getIngestorUser() {
		return ingestorUser;
	}

	/**
	 * Gets the password for the prosEO Ingestor component
	 * 
	 * @return the Ingestor password
	 */
	public String getIngestorPassword() {
		return ingestorPassword;
	}

}
