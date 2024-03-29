/**
 * UsermgrConfiguration.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO User Manager component
 *
 * @author Dr. Thomas Bassler *
 */
@Configuration
@ConfigurationProperties(prefix = "proseo")
@EntityScan(basePackages = { "de.dlr.proseo.model", "de.dlr.proseo.usermgr.model" })
public class UsermgrConfiguration {

	/** The default user to create in an empty user database */
	@Value("${proseo.defaultuser.name}")
	private String defaultUserName;

	/** The initial password of the default user */
	@Value("${proseo.defaultuser.password}")
	private String defaultUserPassword;

	/** The password expiration period in days */
	@Value("${proseo.password.expirationtime}")
	private String passwordExpirationTime;

	/** The maximum number of results to be retrieved by REST requests */
	@Value("${spring.maxResults}")
	public Integer maxResults;

	/**
	 * @return the maximum number of results to be retrieved by REST requests
	 */
	public Integer getMaxResults() {
		return maxResults;
	}

	/**
	 * Gets the name of the default user
	 *
	 * @return the name of the default user
	 */
	public String getDefaultUserName() {
		return defaultUserName;
	}

	/**
	 * Gets the password of the default user
	 *
	 * @return the password of the default user
	 */
	public String getDefaultUserPassword() {
		return defaultUserPassword;
	}

	/**
	 * Gets the password expiration time
	 *
	 * @return the password expiration time
	 */
	public String getPasswordExpirationTime() {
		return passwordExpirationTime;
	}

}