/**
 * AipClientTestConfiguration.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.aipclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for the prosEO AIP Client component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@Import(AipClientConfiguration.class)
@ConfigurationProperties(prefix="proseo")
public class AipClientTestConfiguration {
	
	/** A test mission */
	@Value("${proseo.test.mission}")
	private String testMission;
	
	/** A test user */
	@Value("${proseo.test.username}")
	private String testUsername;
	
	/** The test user's password */
	@Value("${proseo.test.password}")
	private String testPassword;
	
	/**
	 * Gets the test mission
	 * 
	 * @return the test mission
	 */
	public String getTestMission() {
		return testMission;
	}

	/**
	 * Gets the test user name
	 * 
	 * @return the test user name
	 */
	public String getTestUsername() {
		return testUsername;
	}

	/**
	 * Gets the test user password
	 * 
	 * @return the test user password
	 */
	public String getTestPassword() {
		return testPassword;
	}
}
