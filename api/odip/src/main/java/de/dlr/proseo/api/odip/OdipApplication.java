/**
 * OdipApplication.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.api.odip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * prosEO Processor Manager application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
@EnableJpaRepositories(basePackages = { "de.dlr.proseo.model.dao" })
public class OdipApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(OdipApplication.class, args);
	}

}
