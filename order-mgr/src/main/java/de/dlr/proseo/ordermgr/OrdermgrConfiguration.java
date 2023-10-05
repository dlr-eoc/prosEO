/**
 * OrdermgrConfiguration.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Order Manager component
 *
 * @author Dr. Thomas Bassler
 */
@Configuration
@ConfigurationProperties(prefix = "proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class OrdermgrConfiguration {

	/** Wait time for cleanup */
	@Value("${proseo.orderManager.cleanupCycleTime}")
	private Integer cleanupCycleTime;

	/** The maximum number of results to be retrieved by REST requests */
	@Value("${spring.maxResults}")
	public Integer maxResults;

	/**
	 * Return the clea-up cycle time
	 *
	 * @return the cleanupCycleTime
	 */
	public Integer getCleanupCycleTime() {
		return cleanupCycleTime;
	}

	/**
	 * Set the clean-up cycle time
	 *
	 * @param cleanupCycleTime the cleanupCycleTime to set
	 */
	public void setCleanupCycleTime(Integer cleanupCycleTime) {
		this.cleanupCycleTime = cleanupCycleTime;
	}

	/**
	 * Return the maximum number of results to be retrieved by REST requests
	 *
	 * @return the maximum number of results to be retrieved by REST requests
	 */
	public Integer getMaxResults() {
		return maxResults;
	}

}