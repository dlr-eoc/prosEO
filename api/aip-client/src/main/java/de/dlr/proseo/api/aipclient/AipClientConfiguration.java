/**
 * AipClientConfiguration.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.aipclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO AIP Client component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class AipClientConfiguration {
	
	/** The URL of the prosEO Ingestor */
	@Value("${proseo.ingestor.url}")
	private String ingestorUrl;
	
	/** Timeout for Ingestor connections in milliseconds */
	@Value("${proseo.ingestor.timeout}")
	private Long ingestorTimeout;
	
	/** Source directory for uploads by the prosEO Ingestor */
	@Value("${proseo.ingestor.sourcedir}")
	private String ingestorSourceDir;
	
	/** The Storage Manager mount point for product ingestion */
	@Value("${proseo.ingestor.mountpoint}")
	private String ingestorMountPoint;
	
	/** The interval between product order status checks in milliseconds */
	@Value("${proseo.order.check.interval}")
	private Long orderCheckInterval;
	
	/** Timeout for archive connections in milliseconds */
	@Value("${proseo.archive.timeout}")
	private Long archiveTimeout;
	
	
	/**
	 * Gets the URL of the prosEO Ingestor
	 * 
	 * @return the Ingestor URL
	 */
	public String getIngestorUrl() {
		return ingestorUrl;
	}

	/**
	 * Gets the timeout for Ingestor connections in milliseconds
	 * 
	 * @return the ingestor timeout
	 */
	public Long getIngestorTimeout() {
		return ingestorTimeout;
	}

	/**
	 * Gets the source directory for uploads by the prosEO Ingestor
	 * 
	 * @return the Ingestor source directory
	 */
	public String getIngestorSourceDir() {
		return ingestorSourceDir;
	}

	/**
	 * Gets the Storage Manager mount point for product ingestion
	 * 
	 * @return the ingestion mount point
	 */
	public String getIngestorMountPoint() {
		return ingestorMountPoint;
	}

	/**
	 * Gets the interval between product order checks
	 * 
	 * @return the product order check interval in ms
	 */
	public Long getOrderCheckInterval() {
		return orderCheckInterval;
	}

	/**
	 * Gets the timeout for archive connections in milliseconds
	 * 
	 * @return the archive timeout
	 */
	public Long getArchiveTimeout() {
		return archiveTimeout;
	}
}
