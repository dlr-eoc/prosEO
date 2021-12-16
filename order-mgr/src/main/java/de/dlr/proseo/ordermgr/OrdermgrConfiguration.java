/**
 * OrdermgrConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	/** Wait time for cleanup */
	@Value("${proseo.orderManager.cleanupCycleTime}")
	private Integer cleanupCycleTime;

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
}
