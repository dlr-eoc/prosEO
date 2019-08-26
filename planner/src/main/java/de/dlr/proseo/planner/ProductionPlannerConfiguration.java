/**
 * ProductionPlannerConfiguration.java
 * 
 */
package de.dlr.proseo.planner;


import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration class for the prosEO ProductionPlanner component
 * 
 * @author melchinger
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class ProductionPlannerConfiguration {
	
}
