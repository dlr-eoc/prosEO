/**
 * AipClientApplication.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.api.aipclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * prosEO AIP Client application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
@EnableJpaRepositories(basePackages = { "de.dlr.proseo.model.dao" })
public class AipClientApplication {
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(AipClientApplication.class, args);
	}

}
