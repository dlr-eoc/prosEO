/**
 * IngestorConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Ingestor component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class IngestorConfiguration {
	
	/** The URL of the prosEO Production Planner */
	@Value("${proseo.productionPlanner.url}")
	private String productionPlannerUrl;
	
	/** The user name to use for prosEO Production Planner logins */
	@Value("${proseo.productionPlanner.user}")
	private String productionPlannerUser;

	/** The password to use for prosEO Production Planner logins */
	@Value("${proseo.productionPlanner.password}")
	private String productionPlannerPassword;

	/** The user name to use for prosEO Storage Manager logins */
	@Value("${proseo.storageManager.user}")
	private String storageManagerUser;

	/** The password to use for prosEO Storage Manager logins */
	@Value("${proseo.storageManager.password}")
	private String storageManagerPassword;

	/** The default storage type of the prosEO Storage Manager */
	@Value("${proseo.storageManager.defaultStorageType}")
	private String defaultStorageType;

	/**
	 * Gets the URL of the prosEO Production Planner component
	 * 
	 * @return the productionPlannerUrl the URL of the Production Planner
	 */
	public String getProductionPlannerUrl() {
		return productionPlannerUrl;
	}

	/**
	 * Gets the user for production planner logins
	 * 
	 * @return the productionPlannerUser
	 */
	public String getProductionPlannerUser() {
		return productionPlannerUser;
	}

	/**
	 * Gets the password for production planner logins
	 * 
	 * @return the productionPlannerPassword
	 */
	public String getProductionPlannerPassword() {
		return productionPlannerPassword;
	}

	/**
	 * Gets the user for storage manager logins
	 * 
	 * @return the storage manager user
	 */
	public String getStorageManagerUser() {
		return storageManagerUser;
	}

	/**
	 * Gets the password for storage manager logins
	 * 
	 * @return the storage manager password
	 */
	public String getStorageManagerPassword() {
		return storageManagerPassword;
	}

	/**
	 * Gets the default storage type to use with the Storage Manager
	 * 
	 * @return the defaultStorageType
	 */
	public String getDefaultStorageType() {
		return defaultStorageType;
	}
	
	
}
