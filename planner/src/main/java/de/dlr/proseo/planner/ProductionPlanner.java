/*
 * ProductionPlanner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.planner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import de.dlr.proseo.planner.kubernetes.KubeConfig;

/*
 * prosEO Ingestor application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan
public class ProductionPlanner {

	public static void main(String[] args) throws Exception {

		KubeConfig.connect();
		
		SpringApplication.run(ProductionPlanner.class, args);
	}

}
