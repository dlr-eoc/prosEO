/**
 * ProductionPlannerConfiguration.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO ProductionPlanner component
 * 
 * @author Ernst Melchinger
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class ProductionPlannerConfiguration {

	/** The URL of the prosEO Production Planner */
	@Value("${proseo.productionPlanner.url}")
	private String productionPlannerUrl;
	
	/** The user name to use for Production Planner/Ingestor logins by wrapper */
	@Value("${proseo.wrapper.user}")
	private String wrapperUser;

	/** The password to use for prosEO Production Planner/Ingestor logins by wrapper */
	@Value("${proseo.wrapper.password}")
	private String wrapperPassword;

	/** The URL of the prosEO Ingestor */
	@Value("${proseo.ingestor.url}")
	private String ingestorUrl;
	
	/** Wait time for K8s job finish cycle in milliseconds */
	@Value("${proseo.productionPlanner.cyclewaittime}")
	private String productionPlannerCycleWaitTime;
	
	/** Kubernetes configuration file name */
	@Value("${proseo.productionPlanner.kubeconfig}")
	private String productionPlannerKubeConfig;

	/** Maximum cycle for K8s job finish */
	@Value("${proseo.productionPlanner.maxcycles}")
	private String productionPlannerMaxCycles;
	
	/** Wait time for K8s job finish cycle in milliseconds */
	@Value("${proseo.productionPlanner.dispatcherwaittime}")
	private String productionPlannerDispatcherWaitTime;

	/** Maximum cycles for file size check */
	@Value("${proseo.productionPlanner.filecheckmaxcycles}")
	private String productionPlannerFileCheckMaxCycles;
	
	/** Wait time for file size check cycle in milliseconds */
	@Value("${proseo.productionPlanner.filecheckwaittime}")
	private String productionPlannerFileCheckWaitTime;

	/** Mount point for wrapper */
	@Value("${proseo.posix.workerMountPoint}")
	private String posixWorkerMountPoint;
	
	/** Log organization for influxDB */
	@Value("${proseo.log.org}")
	private String logOrg;

	/** Log token for influxDB */
	@Value("${proseo.log.token}")
	private String logToken;

	/** Log host for influxDB */
	@Value("${proseo.log.host}")
	private String logHost;
	
	/**
	 * @return the logHost
	 */
	public String getLogHost() {
		return logHost;
	}

	/**
	 * @return the logOrg
	 */
	public String getLogOrg() {
		return logOrg;
	}

	/**
	 * @return the logToken
	 */
	public String getLogToken() {
		return logToken;
	}

	/**
	 * @return the productionPlannerFileCheckMaxCycles
	 */
	public String getProductionPlannerFileCheckMaxCycles() {
		return productionPlannerFileCheckMaxCycles;
	}

	/**
	 * @return the productionPlannerFileCheckWaitTime
	 */
	public String getProductionPlannerFileCheckWaitTime() {
		return productionPlannerFileCheckWaitTime;
	}

	/**
	 * @return the productionPlannerKubeConfig
	 */
	public String getProductionPlannerKubeConfig() {
		return productionPlannerKubeConfig;
	}

	/**
	 * @return the posixWorkerMountPoint
	 */
	public String getPosixWorkerMountPoint() {
		return posixWorkerMountPoint;
	}

	/**
	 * @return the productionPlannerCycleWaitTime
	 */
	public String getProductionPlannerCycleWaitTime() {
		return productionPlannerCycleWaitTime;
	}

	/**
	 * @return the productionPlannerDispatcherWaitTime
	 */
	public String getProductionPlannerDispatcherWaitTime() {
		return productionPlannerDispatcherWaitTime;
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
	 * @return the wrapperUser
	 */
	public String getWrapperUser() {
		return wrapperUser;
	}

	/**
	 * Gets the password for production planner logins
	 * 
	 * @return the productionPlannerPassword
	 */
	public String getWrapperPassword() {
		return wrapperPassword;
	}

	/**
	 * @return the ingestorUrl
	 */
	public String getIngestorUrl() {
		return ingestorUrl;
	}

}
