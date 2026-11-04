/**
 * DatatakeManagerApplication.java
 * 
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.dtmgr;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * prosEO Mission Planning Monitor application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan(basePackages = { "de.dlr.proseo" })
@EnableJpaRepositories(basePackages = { "de.dlr.proseo.model.dao" })
public class DatatakeManagerApplication implements CommandLineRunner {
	
	/**
	 * Main method to start Spring application
	 * 
	 * @param args command line arguments (not used)
	 * @throws Exception if any unhandled exception occurs
	 */
	@Override
	public void run(String... args) throws Exception {
		SpringApplication.run(DatatakeManagerApplication.class, args);
		
	}

}
