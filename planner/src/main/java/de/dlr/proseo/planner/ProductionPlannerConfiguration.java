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

	/** The URL of the prosEO Ingestor */
	@Value("${proseo.ingestor.url}")
	private String ingestorUrl;
	
	/** The user name to use for prosEO Ingestor logins */
	@Value("${proseo.ingestor.user}")
	private String ingestorUser;

	/** The password to use for prosEO Ingestor logins */
	@Value("${proseo.ingestor.password}")
	private String ingestorPassword;
	
	/** The URL of the prosEO S3 */
	@Value("${proseo.s3.s3EndPoint}")
	private String s3EndPoint;

	/** The user name to use for prosEO S3 logins */
	@Value("${proseo.s3.s3SecretAccessKey}")
	private String s3SecretAccessKey;

	/** The password to use for prosEO S3 logins */
	@Value("${proseo.s3.s3AccessKey}")
	private String s3AccessKey;
	
	/** The bucket to use for prosEO S3 logins */
	@Value("${proseo.s3.s3DefaultBucket}")
	private String s3DefaultBucket;
	
	/** Wait time for K8s job finish cycle in milliseconds */
	@Value("${proseo.productionPlanner.cyclewaittime}")
	private String productionPlannerCycleWaitTime;

	/** Maximum cycle for K8s job finish */
	@Value("${proseo.productionPlanner.maxcycles}")
	private String productionPlannerMaxCycles;
	
	/** Wait time for K8s job finish cycle in milliseconds */
	@Value("${proseo.productionPlanner.dispatcherwaittime}")
	private String productionPlannerDispatcherWaitTime;


	/**
	 * @return the s3Url
	 */
	public String getS3EndPoint() {
		return s3EndPoint;
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
	 * Gets the user for S3 logins
	 * 
	 * @return the S3 user
	 */
	public String getS3AccessKey() {
		return s3AccessKey;
	}

	/**
	 * Gets the password for S3 logins
	 * 
	 * @return the S3 password
	 */
	public String getS3SecretAccessKey() {
		return s3SecretAccessKey;
	}


	public String getS3DefaultBucket() {
		return s3DefaultBucket;
	}
	/**
	 * @return the ingestorUrl
	 */
	public String getIngestorUrl() {
		return ingestorUrl;
	}

	/**
	 * @return the ingestorUser
	 */
	public String getIngestorUser() {
		return ingestorUser;
	}

	/**
	 * @return the ingestorPassword
	 */
	public String getIngestorPassword() {
		return ingestorPassword;
	}
	
}
