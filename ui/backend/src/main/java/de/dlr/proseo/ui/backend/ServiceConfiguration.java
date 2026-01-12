/**
 * ServiceConfiguration.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO User Interface (both GUI and CLI)
 *
 * @author Dr. Thomas Bassler
 */
@Configuration
@ConfigurationProperties(prefix = "proseo")
public class ServiceConfiguration {

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

	/** The URL of the prosEO Facility Manager */
	@Value("${proseo.facilityManager.url}")
	private String facilityManagerUrl;

	/** The URL of the prosEO Archive Manager */
	@Value("${proseo.archiveManager.url}")
	private String archiveManagerUrl;

	/** The URL of the prosEO Order Generator */
	@Value("${proseo.orderGenerator.url}")
	private String orderGenUrl;

	/** Timeout for HTTP connections */
	@Value("${proseo.http.timeout}")
	private Long httpTimeout;

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

	/**
	 * Gets the URL of the prosEO Facility Manager component
	 *
	 * @return the facilityManagerUrl
	 */
	public String getFacilityManagerUrl() {
		return facilityManagerUrl;
	}

	/**
	 * Gets the URL of the prosEO Archive Manager component
	 *
	 * @return the archiveManagerUrl
	 */
	public String getArchiveManagerUrl() {
		return archiveManagerUrl;
	}

	/**
	 * Gets the URL of the prosEO Order Generator component
	 *
	 * @return the archiveManagerUrl
	 */
	public String getOrderGenUrl() {
		return orderGenUrl;
	}

	/**
	 * Gets the default timeout for HTTP connections
	 *
	 * @return the httpTimeout
	 */
	public Long getHttpTimeout() {
		return httpTimeout;
	}
}
