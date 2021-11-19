package de.dlr.proseo.geotools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * prosEO Geotools application
 * 
 * @author Ernst Melchinger
 * 
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan
public class GeotoolsApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(GeotoolsApplication.class, args);
	}

}
