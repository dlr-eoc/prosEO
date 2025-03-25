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
 * Configuration class for the prosEO ProductionPlanner component.
 *
 * @author Ernst Melchinger
 */
@Configuration
@ConfigurationProperties(prefix = "proseo")
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

	/** The URL of the prosEO notification service */
	@Value("${proseo.notification.url}")
	private String notificationUrl;

	/** A host alias to forward to the pods for use in the Planner and Ingestor URLs */
	@Value("${proseo.wrapper.hostalias:}")
	private String hostAlias;

	/** Wait time for K8s job finish cycle in milliseconds */
	@Value("${proseo.productionPlanner.cyclewaittime}")
	private Integer productionPlannerCycleWaitTime;

	/** Wait time for K8s pod created in milliseconds */
	@Value("${proseo.productionPlanner.jobcreatedwaittime}")
	private Integer productionPlannerJobCreatedWaitTime;

	/** Kubernetes configuration file name */
	@Value("${proseo.productionPlanner.kubeconfig}")
	private String productionPlannerKubeConfig;

	/** Maximum cycle for K8s job finish */
	@Value("${proseo.productionPlanner.maxcycles}")
	private Integer productionPlannerMaxCycles;

	/** Wait time for K8s job finish cycle in milliseconds */
	@Value("${proseo.productionPlanner.dispatcherwaittime}")
	private Integer productionPlannerDispatcherWaitTime;

	/** Maximum cycles for file size check */
	@Value("${proseo.productionPlanner.filecheckmaxcycles}")
	private Integer productionPlannerFileCheckMaxCycles;

	/** Wait time for file size check cycle in milliseconds */
	@Value("${proseo.productionPlanner.filecheckwaittime}")
	private Integer productionPlannerFileCheckWaitTime;

	/** Check for further job steps after one had finished */
	@Value("${proseo.productionPlanner.checkForFurtherJobStepsToRun}")
	private Boolean checkForFurtherJobStepsToRun;

	/** Mount point for wrapper */
	@Value("${proseo.posix.workerMountPoint}")
	private String posixWorkerMountPoint;

	/** TODO Check: Wait time for file size check cycle in milliseconds */
	@Value("${proseo.productionPlanner.planningbatchsize}")
	private Integer planningBatchSize;

	/** Sort parameter for starting order of job steps */
	@Value("${proseo.productionPlanner.jobStepSort}")
	private JobStepSort jobStepSort;
	
	/** Enable (default)/disable automatic derivation of job steps to generate required input data */
	@Value("${proseo.productionPlanner.autogenerate}")
	private Boolean autogenerate = true;
	

	/** Timeout for HTTP requests in milliseconds */
	@Value("${proseo.http.timeout}")
	private Integer httpTimeout;

	/** The URI of the AIP client (protocol, host name, port, context; no terminating slash) */
	@Value("${proseo.aip.url}")
	private String aipUrl;

	/** The user of the AIP client */
	@Value("${proseo.aip.user}")
	private String aipUser;

	/** The password of the AIP client */
	@Value("${proseo.aip.password}")
	private String aipPassword;

	/**
	 * Gets the job step sorting order.
	 * 
	 * @return the jobStepSort
	 */
	public JobStepSort getJobStepSort() {
		return jobStepSort;
	}

	/**
	 * Gets the AIP user.
	 *
	 * @return the aipUser
	 */
	public String getAipUser() {
		return aipUser;
	}

	/**
	 * Gets the AIP password.
	 *
	 * @return the aipPassword
	 */
	public String getAipPassword() {
		return aipPassword;
	}

	/**
	 * Gets the AIP URL.
	 *
	 * @return the aipUrl
	 */
	public String getAipUrl() {
		return aipUrl;
	}

	/**
	 * Get the HTTP timeout value.
	 *
	 * @return The HTTP timeout value in milliseconds.
	 */
	public Integer getHttpTimeout() {
		return httpTimeout;
	}

	/**
	 * Set the HTTP timeout value.
	 *
	 * @param httpTimeout The HTTP timeout value to set in milliseconds.
	 */
	public void setHttpTimeout(Integer httpTimeout) {
		this.httpTimeout = httpTimeout;
	}

	/**
	 * Get the URL of the prosEO notification service.
	 *
	 * @return The URL of the notification service.
	 */
	public String getNotificationUrl() {
		return notificationUrl;
	}

	/**
	 * Set the URL of the prosEO notification service.
	 *
	 * @param notificationUrl The URL of the notification service to set.
	 */
	public void setNotificationUrl(String notificationUrl) {
		this.notificationUrl = notificationUrl;
	}

	/**
	 * Get the flag indicating whether to check for further job steps to run.
	 *
	 * @return True if further job steps need to be checked, false otherwise.
	 */
	public Boolean getCheckForFurtherJobStepsToRun() {
		if (checkForFurtherJobStepsToRun == null) {
			checkForFurtherJobStepsToRun = true;
		}
		return checkForFurtherJobStepsToRun;
	}

	/**
	 * Set the flag indicating whether to check for further job steps to run.
	 *
	 * @param checkForFurtherJobStepsToRun The flag value to set.
	 */
	public void setCheckForFurtherJobStepsToRun(Boolean checkForFurtherJobStepsToRun) {
		this.checkForFurtherJobStepsToRun = checkForFurtherJobStepsToRun;
	}

	/**
	 * Get the planning batch size.
	 *
	 * @return The planning batch size.
	 */
	public Integer getPlanningBatchSize() {
		if (planningBatchSize == null || planningBatchSize < 1) {
			planningBatchSize = 1;
		}
		return planningBatchSize;
	}

	/**
	 * Set the planning batch size.
	 *
	 * @param planningBatchSize The planning batch size to set.
	 */
	public void setPlanningBatchSize(Integer planningBatchSize) {
		this.planningBatchSize = planningBatchSize;
	}

	/**
	 * Get the wait time for K8s job finish cycle.
	 *
	 * @return The wait time for K8s job finish cycle in milliseconds.
	 */
	public Integer getProductionPlannerJobCreatedWaitTime() {
		return productionPlannerJobCreatedWaitTime;
	}

	/**
	 * Set the wait time for K8s job finish cycle.
	 *
	 * @param productionPlannerJobCreatedWaitTime The wait time for K8s job finish cycle to set in milliseconds.
	 */
	public void setProductionPlannerJobCreatedWaitTime(Integer productionPlannerJobCreatedWaitTime) {
		this.productionPlannerJobCreatedWaitTime = productionPlannerJobCreatedWaitTime;
	}

	/**
	 * Get the maximum cycles for file size check.
	 *
	 * @return The maximum cycles for file size check.
	 */
	public Integer getProductionPlannerFileCheckMaxCycles() {
		return productionPlannerFileCheckMaxCycles;
	}

	/**
	 * Get the wait time for file size check cycle.
	 *
	 * @return The wait time for file size check cycle in milliseconds.
	 */
	public Integer getProductionPlannerFileCheckWaitTime() {
		return productionPlannerFileCheckWaitTime;
	}

	/**
	 * Get the Kubernetes configuration file name.
	 *
	 * @return The Kubernetes configuration file name.
	 */
	public String getProductionPlannerKubeConfig() {
		return productionPlannerKubeConfig;
	}

	/**
	 * Get the mount point for the wrapper.
	 *
	 * @return The mount point for the wrapper.
	 */
	public String getPosixWorkerMountPoint() {
		return posixWorkerMountPoint;
	}

	/**
	 * Get the wait time for K8s job finish cycle.
	 *
	 * @return The wait time for K8s job finish cycle in milliseconds.
	 */
	public Integer getProductionPlannerCycleWaitTime() {
		return productionPlannerCycleWaitTime;
	}

	/**
	 * Get the wait time for the dispatcher in K8s job finish cycle.
	 *
	 * @return The wait time for the dispatcher in K8s job finish cycle in milliseconds.
	 */
	public Integer getProductionPlannerDispatcherWaitTime() {
		return productionPlannerDispatcherWaitTime;
	}

	/**
	 * Get the maximum cycles for K8s job finish.
	 *
	 * @return The maximum cycles for K8s job finish.
	 */
	public Integer getProductionPlannerMaxCycles() {
		return productionPlannerMaxCycles;
	}

	/**
	 * Get the URL of the prosEO Production Planner.
	 *
	 * @return The URL of the Production Planner.
	 */
	public String getProductionPlannerUrl() {
		return productionPlannerUrl;
	}

	/**
	 * Get the user name used for Production Planner/Ingestor logins by the wrapper.
	 *
	 * @return The user name used for logins.
	 */
	public String getWrapperUser() {
		return wrapperUser;
	}

	/**
	 * Get the password used for Production Planner/Ingestor logins by the wrapper.
	 *
	 * @return The password used for logins.
	 */
	public String getWrapperPassword() {
		return wrapperPassword;
	}

	/**
	 * Get the URL of the prosEO Ingestor.
	 *
	 * @return The URL of the Ingestor.
	 */
	public String getIngestorUrl() {
		return ingestorUrl;
	}

	/**
	 * Get the host alias entry.
	 *
	 * @return The host alias entry or null if no entry was given and the default has been set.
	 */
	public String getHostAlias() {
		if (hostAlias.isBlank()) {
			return null;
		} else {
			return hostAlias;
		}
	}

	/**
	 * Get the autogenerate flag
	 * @return the autogenerate
	 */
	public Boolean getAutogenerate() {
		return autogenerate;
	}
	

}