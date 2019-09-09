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
	
	@Value("${proseo.user.name}")
	private String userName;
	
	@Value("${proseo.user.password}")
	private String userPassword;
	
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	
	/**
	 * @return the userPassword
	 */
	public String getUserPassword() {
		return userPassword;
	}
	
	/**
	 * @param userPassword the userPassword to set
	 */
	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}
}
