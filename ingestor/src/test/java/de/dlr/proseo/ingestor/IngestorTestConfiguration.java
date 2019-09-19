/**
 * IngestorTestConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for the prosEO Ingestor component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@Import(IngestorConfiguration.class)
@ConfigurationProperties(prefix="proseo")
public class IngestorTestConfiguration {
	
	/** A user name for test */
	@Value("${proseo.user.name}")
	private String userName;
	
	/** A password for test */
	@Value("${proseo.user.password}")
	private String userPassword;
	
	/** The URL of the prosEO Storage Manager mockup (from the database in the real world) */
	@Value("${proseo.productionPlanner.url}")
	private String storageManagerUrl;

	/**
	 * Gets the test API user name
	 * 
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	
	/**
	 * Gets the test API password
	 * 
	 * @return the userPassword
	 */
	public String getUserPassword() {
		return userPassword;
	}
	
	/**
	 * Gets the URL of the Storage Manager mockup
	 * 
	 * @return the storageManagerUrl
	 */
	public String getStorageManagerUrl() {
		return storageManagerUrl;
	}
}
