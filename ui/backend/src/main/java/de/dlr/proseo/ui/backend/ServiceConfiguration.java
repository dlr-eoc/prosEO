/**
 * ServiceConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO User Interface (both GUI and CLI)
 * 
 * @author Dr. Thomas Bassler
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class ServiceConfiguration {
	
	/** The user name to use for prosEO component logins */
	@Value("${proseo.user}")
	private String proseoUser;

	/** The password to use for prosEO component logins */
	@Value("${proseo.password}")
	private String proseoPassword;

	/** The URL of the prosEO User Manager */
	@Value("${proseo.userManager.url}")
	private String userManagerUrl;
	
	/** The URL of the prosEO Production Planner */
	@Value("${proseo.productionPlanner.url}")
	private String productionPlannerUrl;
	
	/** The URL of the prosEO Ingestor */
	@Value("${proseo.ingestor.url}")
	private String ingestorUrl;
	
	/** The URL of the prosEO Order Manager */
	@Value("${proseo.orderManager.url}")
	private String orderManagerUrl;
	
	/** The URL of the prosEO Processor Manager */
	@Value("${proseo.processorManager.url}")
	private String processorManagerUrl;
	
	/** The URL of the prosEO Product Class Manager */
	@Value("${proseo.productClassManager.url}")
	private String productClassManagerUrl;
	
	/**
	 * Gets the user for prosEO component logins
	 * 
	 * @return the prosEO user
	 */
	public String getProseoUser() {
		return proseoUser;
	}

	/**
	 * Gets the password for prosEO component logins
	 * 
	 * @return the prosEO password
	 */
	public String getProseoPassword() {
		return proseoPassword;
	}

	/**
	 * Gets the URL of the prosEO User Manager component
	 * 
	 * @return the userManagerUrl
	 */
	public String getUserManagerUrl() {
		return userManagerUrl;
	}

	/**
	 * Gets the URL of the prosEO Production Planner component
	 * 
	 * @return the productionPlannerUrl the URL of the Production Planner
	 */
	public String getProductionPlannerUrl() {
		return productionPlannerUrl;
	}

	/**
	 * Gets the URL of the prosEO Ingestor component
	 * 
	 * @return the ingestorUrl
	 */
	public String getIngestorUrl() {
		return ingestorUrl;
	}

	/**
	 * Gets the URL of the prosEO Order Manager component
	 * 
	 * @return the orderManagerUrl
	 */
	public String getOrderManagerUrl() {
		return orderManagerUrl;
	}

	/**
	 * Gets the URL of the prosEO Processor Manager component
	 * 
	 * @return the processorManagerUrl
	 */
	public String getProcessorManagerUrl() {
		return processorManagerUrl;
	}

	/**
	 * Gets the URL of the prosEO Product Class Manager component
	 * 
	 * @return the productClassManagerUrl
	 */
	public String getProductClassManagerUrl() {
		return productClassManagerUrl;
	}

}
