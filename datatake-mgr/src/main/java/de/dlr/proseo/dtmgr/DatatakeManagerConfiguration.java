/**
 * DatatakeManagerConfiguration.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.dtmgr;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Datatake Manager component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix = "proseo.dtmgr")
public class DatatakeManagerConfiguration {
	
	/** A list of spacecraft codes */
	private List<String> spacecrafts;
	
	/**
	 * The interval between datatake checks in milliseconds
	 * (default once per hour; 0 disables processing of incomplete datatakes)
	 */
	@Value("${proseo.dtmgr.check.interval:3600000}")
	private Long checkInterval;
	
	/** The datatake timeout period in seconds (default 200 min, roughly corresponding to two orbits in LEO) */
	@Value("${proseo.dtmgr.timeout:12000}")
	private Long timeout;
	
	/**
	 * Gets the list of spacecraft codes
	 * 
	 * @return a list of spacecraft codes
	 */
	public List<String> getSpacecrafts() {
		return spacecrafts;
	}

	/**
	 * Sets the list of spacecraft codes
	 * 
	 * @param spacecrafts the spacecrafts to set
	 */
	public void setSpacecrafts(List<String> spacecrafts) {
		this.spacecrafts = spacecrafts;
	}

	/**
	 * Gets the interval between datatake checks
	 * 
	 * @return the datatake check interval in ms
	 */
	public Long getCheckInterval() {
		return checkInterval;
	}

	/**
	 * Sets the interval between datatake checks
	 * 
	 * @param checkInterval the checkInterval to set
	 */
	public void setCheckInterval(Long checkInterval) {
		this.checkInterval = checkInterval;
	}

	/**
	 * Gets the timeout for incomplete datatakes
	 * 
	 * @return the datatake timeout period in seconds
	 */
	public Long getTimeout() {
		return timeout;
	}

	/**
	 * Sets the timeout for incomplete datatakes
	 * 
	 * @param timeout the timeout to set
	 */
	public void setTimeout(Long timeout) {
		this.timeout = timeout;
	}

}
