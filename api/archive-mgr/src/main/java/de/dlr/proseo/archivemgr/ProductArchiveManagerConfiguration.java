/**
 * ProductArchiveManagerConfiguration.java
 *
 * (C) 2023 DLR
 */
package de.dlr.proseo.archivemgr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Product Archive Manager component
 *
 * @author Dr. Thomas Bassler
 */
@Configuration
@ConfigurationProperties(prefix = "proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class ProductArchiveManagerConfiguration {
	
	/**
	 * The maximum number of results to be retrieved by REST requests
	 */
	@Value("${spring.maxResults}")
	public Integer maxResults;

	/**
	 * @return the maximum number of results to be retrieved by REST requests
	 */
	public Integer getMaxResults() {
		return maxResults;
	}

	/**
	 * @param maxResults the maximum number of results to be retrieved by REST requests
	 */
	public void setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
	}

}