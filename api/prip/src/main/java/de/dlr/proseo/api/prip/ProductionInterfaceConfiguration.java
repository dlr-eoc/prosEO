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
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class ProductionInterfaceConfiguration {
	
	/** The URL of the prosEO User Manager */
	@Value("${proseo.userManager.url}")
	private String userMgrUrl;
	
	/** The URL of the prosEO Ingestor */
	@Value("${proseo.ingestor.url}")
	private String ingestorUrl;
	
	/** The URL of the prosEO Processor Manager */
	@Value("${proseo.processorManager.url}")
	private String processorManagerUrl;
	
	/** The URL of the prosEO Facility Manager */
	@Value("${proseo.facilityManager.url}")
	private String facilityManagerUrl;
	
	/** The user name for the prosEO Facility Manager */
	@Value("${proseo.facilityManager.user}")
	private String facilityManagerUser;
	
	/** The password for the prosEO Facility Manager */
	@Value("${proseo.facilityManager.password}")
	private String facilityManagerPassword;
	
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
	 * Gets the URL of the prosEO Processor Manager component
	 * 
	 * @return the Processor Manager URL
	 */
	public String getProcessorManagerUrl() {
		return processorManagerUrl;
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
	 * Gets the user name for the prosEO Facility Manager component
	 * 
	 * @return the facilityManagerUser
	 */
	public String getFacilityManagerUser() {
		return facilityManagerUser;
	}

	/**
	 * Gets the password for the prosEO Facility Manager component
	 * 
	 * @return the facilityManagerPassword
	 */
	public String getFacilityManagerPassword() {
		return facilityManagerPassword;
	}

}
