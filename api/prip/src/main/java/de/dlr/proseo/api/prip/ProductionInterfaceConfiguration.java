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
	
	/**
	 * Gets the URL of the prosEO User Manager component
	 * 
	 * @return the User Manager URL
	 */
	public String getUserMgrUrl() {
		return userMgrUrl;
	}

}
