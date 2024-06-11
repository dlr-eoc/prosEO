/**
 * RepositoryConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.service;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the Repository test application
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@EntityScan(basePackages = "de.dlr.proseo.model")
public class RepositoryConfiguration {
	
}
