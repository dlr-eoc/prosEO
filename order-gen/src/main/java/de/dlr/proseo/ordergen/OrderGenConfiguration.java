/**
 * OrderGenConfiguration.java
 *
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordergen;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * Configuration class for the prosEO OrderGenerator component
 *
 * @author Ernst Melchinger
 *
 */
@Configuration
@ConfigurationProperties(prefix = "proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class OrderGenConfiguration {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderGenConfiguration.class);

	/** The maximum number of results to be retrieved by REST requests */
	@Value("${spring.maxResults}")
	public Integer maxResults;

	/** Timeout for HTTP connections */
	@Value("${proseo.http.timeout}")
	private Long httpTimeout;

	/** The URL of the prosEO Production Planner */
	@Value("${proseo.productionPlanner.url}")
	private String productionPlannerUrl;

	/** The URL of the prosEO Order Manager */
	@Value("${proseo.orderManager.url}")
	private String orderManagerUrl;

	/** User */
	@Value("${proseo.orderGenerator.user}")
	private String user;

	/** Password */
	@Value("${proseo.orderGenerator.password}")
	private String password;

	/** Facility to use */
	@Value("${proseo.orderGenerator.facility}")
	private String facility;
	
	/**
	 * Gets the production planner URL
	 * 
	 * @return the productionPlannerUrl
	 */
	public String getProductionPlannerUrl() {
		return productionPlannerUrl;
	}

	/**
	 * Gets the order manager URL
	 * 
	 * @return the orderManagerUrl
	 */
	public String getOrderManagerUrl() {
		return orderManagerUrl;
	}

	/**
	 * Gets the timeout for HTTP connections
	 * 
	 * @return the httpTimeout
	 */
	public Long getHttpTimeout() {
		return httpTimeout;
	}

	/**
	 * @return the maximum number of results to be retrieved by REST requests
	 */
	public Integer getMaxResults() {
		return maxResults;
	}
	
	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @return the facility
	 */
	public String getFacility() {
		return facility;
	}

}
