package de.dlr.proseo.geotools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * prosEO Geotools application for geographic operations (intersection/containment of footprints with named geographical areas)
 *
 * @author Ernst Melchinger
 *
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan
public class GeotoolsApplication {

	/**
	 * The entry point of the application.
	 *
	 * @param args The command line arguments.
	 * @throws Exception If an error occurs during application startup.
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication.run(GeotoolsApplication.class, args);
	}

}
