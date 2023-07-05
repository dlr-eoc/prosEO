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
	
	/** Path to the directory to download files to (must be readable for Storage Manager, see below) */
	@Value("${proseo.aipclient.targetdir}")
	private String clientTargetDir;
	
	/** The URL of the prosEO Ingestor */
	@Value("${proseo.ingestor.url}")
	private String ingestorUrl;
	
	/** Timeout for Ingestor connections in milliseconds */
	@Value("${proseo.ingestor.timeout}")
	private Long ingestorTimeout;
	
	/** The Storage Manager mount point for product ingestion */
	@Value("${proseo.storagemgr.mountpoint}")
	private String storageMgrMountPoint;
	
	/** Source directory for uploads by the prosEO Storage Manager (Storage Manager perspective on proseo.aipclient.targetdir) */
	@Value("${proseo.storagemgr.sourcedir}")
	private String storageMgrSourceDir;
	
	/** The interval between product order status checks in milliseconds */
	@Value("${proseo.order.check.interval}")
	private Long orderCheckInterval;
	
	/** Timeout for archive connections in milliseconds */
	@Value("${proseo.archive.timeout}")
	private Long archiveTimeout;
	
	
	/**
	 * Gets the path to the directory to download files to
	 * 
	 * @return the download target directory
	 */
	public String getClientTargetDir() {
		return clientTargetDir;
	}

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
	 * Gets the Storage Manager mount point for product ingestion
	 * 
	 * @return the ingestion mount point
	 */
	public String getStorageMgrMountPoint() {
		return storageMgrMountPoint;
	}

	/**
	 * Gets the source directory for uploads by the prosEO Storage Manager
	 * 
	 * @return the Ingestor source directory
	 */
	public String getStorageMgrSourceDir() {
		return storageMgrSourceDir;
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
