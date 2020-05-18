/**
 * IngestorConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Ingestor component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class IngestorConfiguration {
	
	/** The URL of the prosEO Production Planner */
	@Value("${proseo.productionPlanner.url}")
	private String productionPlannerUrl;
	
	/**
	 * Gets the URL of the prosEO Production Planner component
	 * 
	 * @return the productionPlannerUrl the URL of the Production Planner
	 */
	public String getProductionPlannerUrl() {
		return productionPlannerUrl;
	}

}
