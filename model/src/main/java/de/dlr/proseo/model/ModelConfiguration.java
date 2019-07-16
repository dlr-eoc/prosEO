/**
 * ModelConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Dummy class to enforce database schema configuration
 * 
 * @author Thomas Bassler
 *
 */
@EnableAutoConfiguration
@ComponentScan
public class ModelConfiguration {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ModelConfiguration.class, args);
	}
}
