/**
 * ProductionPlannerConfiguration.java
 * 
 */
package de.dlr.proseo.planner;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
	
	/** The URL of the prosEO Production Planner */
	@Value("${proseo.productionPlanner.url}")
	private String productionPlannerUrl;
	
	/** The user name to use for prosEO Production Planner logins */
	@Value("${proseo.productionPlanner.user}")
	private String productionPlannerUser;

	/** The password to use for prosEO Production Planner logins */
	@Value("${proseo.productionPlanner.password}")
	private String productionPlannerPassword;
	
	/** The URL of the prosEO Storage Manager */
	@Value("${proseo.storageManager.url}")
	private String storageManagerUrl;

	/** The user name to use for prosEO Storage Manager logins */
	@Value("${proseo.storageManager.user}")
	private String storageManagerUser;

	/** The password to use for prosEO Storage Manager logins */
	@Value("${proseo.storageManager.password}")
	private String storageManagerPassword;

	/** Wait time for K8s job finish cycle in milliseconds */
	@Value("${proseo.productionPlanner.cyclewaittime}")
	private String productionPlannerCycleWaitTime;

	/** Maximum cycle for K8s job finish */
	@Value("${proseo.productionPlanner.maxcycles}")
	private String productionPlannerMaxCycles;

	/**
	 * @return the storageManagerUrl
	 */
	public String getStorageManagerUrl() {
		return storageManagerUrl;
	}

	/**
	 * @return the productionPlannerCycleWaitTime
	 */
	public String getProductionPlannerCycleWaitTime() {
		return productionPlannerCycleWaitTime;
	}

	/**
	 * @return the productionPlannerMaxCycles
	 */
	public String getProductionPlannerMaxCycles() {
		return productionPlannerMaxCycles;
	}

	/**
	 * Gets the URL of the prosEO Production Planner component
	 * 
	 * @return the productionPlannerUrl the URL of the Production Planner
	 */
	public String getProductionPlannerUrl() {
		return productionPlannerUrl;
	}

	/**
	 * Gets the user for production planner logins
	 * 
	 * @return the productionPlannerUser
	 */
	public String getProductionPlannerUser() {
		return productionPlannerUser;
	}

	/**
	 * Gets the password for production planner logins
	 * 
	 * @return the productionPlannerPassword
	 */
	public String getProductionPlannerPassword() {
		return productionPlannerPassword;
	}

	/**
	 * Gets the user for storage manager logins
	 * 
	 * @return the storage manager user
	 */
	public String getStorageManagerUser() {
		return storageManagerUser;
	}

	/**
	 * Gets the password for storage manager logins
	 * 
	 * @return the storage manager password
	 */
	public String getStorageManagerPassword() {
		return storageManagerPassword;
	}
	
}
