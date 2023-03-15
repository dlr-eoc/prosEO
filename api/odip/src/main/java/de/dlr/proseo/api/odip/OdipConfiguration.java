/**
 * OdipConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Processor Manager component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class OdipConfiguration {
	
	/** The URL of the prosEO User Manager */
	@Value("${proseo.userManager.url}")
	private String userMgrUrl;
	
	/** The URL of the prosEO Ingestor */
	@Value("${proseo.ingestor.url}")
	private String ingestorUrl;

	/** The URL of the prosEO Production Planner */
	@Value("${proseo.productionPlanner.url}")
	private String productionPlannerUrl;

	/** The URL of the prosEO Order Manager */
	@Value("${proseo.orderManager.url}")
	private String orderManagerUrl;
	
	/** Validity period for OAuth2 tokens */
	@Value("${proseo.token.expiration}")
	private Long tokenExpirationPeriod;
	
	/** Maximum number of objects to retrieve in a single query */
	@Value("${proseo.quota}")
	private Long quota;

	/** Timeout for HTTP connections */
	@Value("${proseo.http.timeout}")
	private Long httpTimeout;

	/** Facility to use */
	@Value("${proseo.odip.facility}")
	private String facility;

	/** The base URI of the AUXIP (protocol, host name, port; no terminating slash) */
	@Value("${proseo.auxip.baseuri}")
	private String auxipBaseUri;
	
	/**
	 * @return the auxipBaseUri
	 */
	public String getAuxipBaseUri() {
		return auxipBaseUri;
	}

	/**
	 * @return the facility
	 */
	public String getFacility() {
		return facility;
	}

	/**
	 * @return the httpTimeout
	 */
	public Long getHttpTimeout() {
		return httpTimeout;
	}

	/**
	 * @return the productionPlannerUrl
	 */
	public String getProductionPlannerUrl() {
		return productionPlannerUrl;
	}

	/**
	 * @return the orderManagerUrl
	 */
	public String getOrderManagerUrl() {
		return orderManagerUrl;
	}

	/**
	 * Gets the URL of the prosEO User Manager component
	 * 
	 * @return the User Manager URL
	 */
	public String getUserMgrUrl() {
		return userMgrUrl;
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
	 * Gets the token validity period
	 * 
	 * @return the token expiration period
	 */
	public Long getTokenExpirationPeriod() {
		return tokenExpirationPeriod;
	}

	/**
	 * Gets the maximum number of objects to retrieve in a single query
	 * 
	 * @return the quota
	 */
	public Long getQuota() {
		return quota;
	}

}