/**
 * ProductionInterfaceConfiguration.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Processor Manager component
 *
 * @author Dr. Thomas Bassler
 */
@Configuration
@ConfigurationProperties(prefix = "proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class ProductionInterfaceConfiguration {

	/** The URL of the prosEO User Manager */
	@Value("${proseo.userManager.url}")
	private String userMgrUrl;

	/** The URL of the prosEO Ingestor */
	@Value("${proseo.ingestor.url}")
	private String ingestorUrl;

	/** Validity period for OAuth2 tokens */
	@Value("${proseo.token.expiration}")
	private Long tokenExpirationPeriod;

	/** Maximum number of products to retrieve in a single query */
	@Value("${proseo.quota}")
	private Long quota;

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
	 * Gets the maximum number of products to retrieve in a single query
	 *
	 * @return the quota
	 */
	public Long getQuota() {
		return quota;
	}

}