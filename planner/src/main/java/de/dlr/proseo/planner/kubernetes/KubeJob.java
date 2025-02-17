/**
 * KubeJob.java
 *
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.kubernetes;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.JobStep.StdLogLevel;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.Task;
import de.dlr.proseo.model.enums.JobOrderVersion;
import de.dlr.proseo.model.enums.OrderSource;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.service.ProductQueryService;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.JobDispatcher;
import de.dlr.proseo.planner.util.UtilService;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.CoreV1EventList;
import io.kubernetes.client.openapi.models.V1EnvVarSource;
import io.kubernetes.client.openapi.models.V1EnvVarSourceBuilder;
import io.kubernetes.client.openapi.models.V1HostAlias;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobBuilder;
import io.kubernetes.client.openapi.models.V1JobCondition;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1JobSpecBuilder;
import io.kubernetes.client.openapi.models.V1LocalObjectReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodCondition;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;

/**
 * A KubeJob describes the complete information to run a Kubernetes job.
 *
 * @author Ernst Melchinger
 *
 */
@Component
public class KubeJob {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(KubeJob.class);
	
	/** The job's database id */
	private long jobId;

	/** The generated job name (job prefix + job ID) */
	private String jobName;

	/** The list of pod names associated with the job */
	private ArrayList<String> podNames;

	/** The generated container name (container prefix + job ID) */
	private String containerName;

	/** The processor image name */
	private String imageName;

	/** The job order file (JOF) */
	private String jobOrderFileName;

	/** Additional commands for the kube job (command arguments) */
	private ArrayList<String> args = new ArrayList<>();

	/** The processing order to which the job step is associated */
	private JobOrder jobOrder;

	/** The processing facility running the job step */
	private KubeConfig kubeConfig;
	
	/**
	 * Internal class to store data for Job Order creation
	 */
	private static class JobOrderData {
		public JobOrder jobOrder;
		public JobOrderVersion jobOrderVersion;
		
		public JobOrderData(JobOrder jobOrder, JobOrderVersion jobOrderVersion) {
			this.jobOrder = jobOrder;
			this.jobOrderVersion = jobOrderVersion;
		}
	}

	/**
	 * Returns the job ID.
	 *
	 * @return the jobId
	 */
	public long getJobId() {
		return jobId;
	}

	/**
	 * Returns the job name.
	 *
	 * @return the jobName
	 */
	public String getJobName() {
		return jobName;
	}

	/**
	 * Returns the list of pod names associated with the job. If the pod names are not already populated, a search for pods is
	 * performed.
	 *
	 * @return the podNames
	 */
	public ArrayList<String> getPodNames() {
		if (podNames == null || podNames.isEmpty()) {
			searchPod();
		}
		return podNames;
	}

	/**
	 * Returns the name of the container associated with the job.
	 *
	 * @return the containerName
	 */
	public String getContainerName() {
		return containerName;
	}

	/**
	 * Returns the name of the job order file (JOF).
	 *
	 * @return the jobOrderFileName
	 */
	public String getJobOrderFileName() {
		return jobOrderFileName;
	}

	/**
	 * Sets the name of the job order file (JOF).
	 *
	 * @param jobOrderFileName the jobOrderFileName to set
	 */
	public void setJobOrderFileName(String jobOrderFileName) {
		this.jobOrderFileName = jobOrderFileName;
	}

	/**
	 * Returns the processing order to which the job is associated.
	 *
	 * @return the jobOrder
	 */
	public JobOrder getJobOrder() {
		return jobOrder;
	}

	/**
	 * Sets the job order for the job.
	 *
	 * @param jobOrder the jobOrder to set
	 */
	public void setJobOrder(JobOrder jobOrder) {
		this.jobOrder = jobOrder;
	}

	/**
	 * Add an argument to the list of command arguments.
	 *
	 * @param arg The command argument to add
	 */
	public void addArg(String arg) {
		if (arg != null) {
			args.add(arg);
		}
	}

	/** Instantiate a kube job with a list of pod names */
	public KubeJob() {
		podNames = new ArrayList<>();
	}

	/**
	 * Instantiate a Kubernetes job with ID and Job Order file name
	 *
	 * @param jobId            the job ID
	 * @param jobOrderFileName the Job Order file name
	 */
	public KubeJob(Long jobId, String jobOrderFileName) {

		this.jobOrderFileName = jobOrderFileName;
		this.podNames = new ArrayList<>();

		this.jobId = jobId;
		this.jobName = ProductionPlanner.jobNamePrefix + jobId;
		this.containerName = ProductionPlanner.jobContainerPrefix + jobId;

	}

	/**
	 * Rebuilds the kube job entries of a processing facility after a restart of the planner.
	 *
	 * @param kubeConfig The processing facility's kube configuration
	 * @param job        The Kubernetes job to rebuild
	 * @return The created kube job or null if it is not a prosEO job
	 */
	public KubeJob rebuild(KubeConfig kubeConfig, V1Job job) {
		if (logger.isTraceEnabled())
			logger.trace(">>> rebuild({}, {})", (null == kubeConfig ? "null" : kubeConfig.getId()),
					(null == job ? "null" : job.getKind()));

		this.kubeConfig = kubeConfig;

		if (kubeConfig.isConnected() && job != null) {
			jobName = job.getMetadata().getName();

			// Check if it is a prosEO job by verifying the job name prefix
			if (jobName.startsWith(ProductionPlanner.jobNamePrefix)) {
				try {
					// Extract the job ID from the job name and set the container name
					jobId = Long.valueOf(jobName.substring(ProductionPlanner.jobNamePrefix.length()));
					containerName = ProductionPlanner.jobContainerPrefix + jobId;
				} catch (NumberFormatException e) {

					if (logger.isDebugEnabled()) {
						logger.debug("An exception occurred. Cause: ", e);
					}

					return null;
				}
			} else {
				// If it is not a prosEO job, return null
				return null;
			}

			// Search for associated pods
			searchPod();
		}

		return this;
	}

	/**
	 * Creates a Kubernetes job on the processing facility based on the provided parameters.
	 * 
	 * TODO Add retry of database update after job has been sent to Kubernetes
	 *
	 * @param kubeConfig     The processing facility's kube configuration
	 * @param stdoutLogLevel The log level for stdout
	 * @param stderrLogLevel The log level for stderr
	 * @return The kube job
	 * @throws Exception if an error occurs during job creation
	 */
	//@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	public KubeJob createJob(KubeConfig kubeConfig, String stdoutLogLevel, String stderrLogLevel) throws Exception {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJob({}, {}, {})", kubeConfig, stdoutLogLevel, stderrLogLevel);
		
		// Ensure that the kube configuration is given
		this.kubeConfig = kubeConfig;
		if (!kubeConfig.isConnected()) {
			logger.log(PlannerMessage.KUBERNETES_NOT_CONNECTED, kubeConfig.getProcessingFacility().toString());
			return null;
		}

		TransactionTemplate transactionTemplate = new TransactionTemplate(this.kubeConfig.getProductionPlanner().getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		transactionTemplate.setReadOnly(false);
		final JobDispatcher jobDispatcher = new JobDispatcher();
		// Prepare for transaction retry, if "org.springframework.dao.CannotAcquireLockException" is thrown
		JobOrderData jobOrderData = null;
		Instant execTime = Instant.now();
		for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
			try {

				final Instant execTimeLoc = transactionTemplate.execute(status -> {

					// Find the job step in the database
					Optional<JobStep> jobStepOptional = RepositoryService.getJobStepRepository().findById(this.getJobId());
					if (jobStepOptional.isEmpty()) {
						logger.log(PlannerMessage.JOB_STEP_NOT_FOUND, this.getJobId());
						return null;
					}
					JobStep jobStep = jobStepOptional.get();

					// Find the execution time
					return jobStep.getJob().getProcessingOrder().getExecutionTime();				
				});   
				execTime = execTimeLoc;
				break;

			} catch (CannotAcquireLockException e) {
				if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

				if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
					ProseoUtil.dbWait();
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			}
		}
		if (execTime != null) {
			if (Instant.now().isBefore(execTime)) {
				if (logger.isTraceEnabled())
					logger.trace(">>> execution time of order is after now.");
				return null;
			} else {
				// reprocess the selection rules to get all "new" available input products
				reexecuteProductQueries();

			}
		}

		for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
			try {

				final JobOrderData jobOrderDataLoc = transactionTemplate.execute(status -> {
					// Find the job step in the database
					Optional<JobStep> jobStepOptional = RepositoryService.getJobStepRepository().findById(this.getJobId());
					if (jobStepOptional.isEmpty()) {
						logger.log(PlannerMessage.JOB_STEP_NOT_FOUND, this.getJobId());
						return null;
					}
					JobStep jobStep = jobStepOptional.get();

					// Retrieve the configured processor
					ConfiguredProcessor configuredProcessor = jobStep.getOutputProduct().getConfiguredProcessor();
					if (null == configuredProcessor || !configuredProcessor.getEnabled()) {
						logger.log(PlannerMessage.CONFIG_PROC_DISABLED, jobStep.getOutputProduct().getConfiguredProcessor().getIdentifier());
						return null;
					}

					// Set the log levels if provided, otherwise use default values
					if (stdoutLogLevel != null && !stdoutLogLevel.isEmpty()) {
						jobStep.setStdoutLogLevel(JobStep.StdLogLevel.valueOf(stdoutLogLevel));
					} else if (jobStep.getStdoutLogLevel() == null) {
						jobStep.setStdoutLogLevel(StdLogLevel.INFO);
					}
					if (stderrLogLevel != null && !stderrLogLevel.isEmpty()) {
						jobStep.setStderrLogLevel(JobStep.StdLogLevel.valueOf(stderrLogLevel));
					} else if (jobStep.getStdoutLogLevel() == null) {
						jobStep.setStderrLogLevel(StdLogLevel.INFO);
					}
					// Set the generation time
					Instant generationTime = Instant.now();
					Duration retentionPeriod = jobStep.getJob().getProcessingOrder().getProductRetentionPeriod();
					if (retentionPeriod == null) {
						retentionPeriod = jobStep.getJob().getProcessingOrder().getMission().getProductRetentionPeriod();
					}
					setGenerationTime(jobStep.getOutputProduct(), generationTime, retentionPeriod);

					// Attempt to create the job order for the job step
					JobOrder jobOrder = null;

					try {
						jobOrder = jobDispatcher.createJobOrder(jobStep);
					} catch (Exception e) {
						logger.log(PlannerMessage.JOB_STEP_CREATION_FAILED_EXCEPTION, jobStep.getId(), e.getMessage());

						if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

						jobStep.setProcessingStartTime(generationTime);
						jobStep.setProcessingCompletionTime(generationTime);
						jobStep.setProcessingStdOut(
								String.format("Exception: creation of job order for job step %d failed", jobStep.getId())
								+ "\n" + e.getClass().getName() + " / " + e.getMessage());
						jobStep.setJobStepState(JobStepState.RUNNING);
						jobStep.setJobStepState(JobStepState.FAILED);
						RepositoryService.getJobStepRepository().save(jobStep);
						return null;
					}
					if (jobOrder == null) {
						return null;
					}

					return new JobOrderData(jobOrder, configuredProcessor.getProcessor().getJobOrderVersion());
				});   
				jobOrderData = jobOrderDataLoc;
				break;
			} catch (CannotAcquireLockException e) {
				if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

				if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
					ProseoUtil.dbWait();
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			}
		}
		if (null == jobOrderData || null == jobOrderData.jobOrder) {
			throw new Exception(logger.log(PlannerMessage.JOB_STEP_CREATION_FAILED, this.getJobId()));
		}
		

		// Send the job order to the storage manager (outside of database transaction)
		jobOrderData.jobOrder = jobDispatcher.sendJobOrderToStorageManager(this.kubeConfig, jobOrderData.jobOrder, jobOrderData.jobOrderVersion);
		if (jobOrderData.jobOrder == null) {
			throw new Exception(logger.log(PlannerMessage.SENDING_JOB_STEP_FAILED, this.getJobId()));
		}
		final JobOrderData jobOrderDataLoc = jobOrderData;
		V1Job job = null;
		// Prepare for transaction retry, if "org.springframework.dao.CannotAcquireLockException" is thrown
		for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
			try {

				V1Job jobLoc = transactionTemplate.execute(status -> {
					// Find the job step in the database
					Optional<JobStep> jobStepOptional = RepositoryService.getJobStepRepository().findById(this.getJobId());
					if (jobStepOptional.isEmpty()) {
						logger.log(PlannerMessage.JOB_STEP_NOT_FOUND, this.getJobId());
						return null;
					}
					JobStep jobStep = jobStepOptional.get();

					jobStep.setJobOrderFilename(jobOrderDataLoc.jobOrder.getFileName());

					// Construct the environment variables and configuration for the Kubernetes job
					String missionCode = jobStep.getJob().getProcessingOrder().getMission().getCode();
					String wrapUser = missionCode + "-" + ProductionPlanner.config.getWrapperUser();
					imageName = jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getDockerImage();
					String localMountPoint = ProductionPlanner.config.getPosixWorkerMountPoint();

					// Configure the compute resource requirements
					V1ResourceRequirements requirements = new V1ResourceRequirements();
					String cpus = jobStep.getOutputProduct()
							.getConfiguredProcessor()
							.getConfiguration()
							.getDockerRunParameters()
							.getOrDefault("cpu", "1");
					String mem = jobStep.getOutputProduct()
							.getConfiguredProcessor()
							.getConfiguration()
							.getDockerRunParameters()
							.getOrDefault("memory", "1");
					String minDiskSpace = jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getMinDiskSpace().toString()
							+ "Mi";
					try {
						cpus = getCPUs(jobStep.getOutputProduct().getConfiguredProcessor().getProcessor(), Integer.parseInt(cpus)).toString();
					} catch (NumberFormatException ex) {
						cpus = getCPUs(jobStep.getOutputProduct().getConfiguredProcessor().getProcessor(), 1).toString();
					}
					try {
						mem = getMinMemory(jobStep.getOutputProduct().getConfiguredProcessor().getProcessor(), Integer.parseInt(mem)).toString()
								+ "Gi";
					} catch (NumberFormatException ex) {
						mem = getMinMemory(jobStep.getOutputProduct().getConfiguredProcessor().getProcessor(), 1).toString() + "Gi";
					}
					requirements.putRequestsItem("cpu", new Quantity(cpus))
					.putRequestsItem("memory", new Quantity(mem))
					.putRequestsItem("ephemeral-storage", new Quantity(minDiskSpace));
					V1EnvVarSource es = new V1EnvVarSourceBuilder().withNewFieldRef().withFieldPath("status.hostIP").endFieldRef().build();
					String localStorageManagerUrl = this.kubeConfig.getLocalStorageManagerUrl();

					// Create a host alias, if given in the config file, for use in the Planner and Ingestor URLs
					List<V1HostAlias> hostAliases = new ArrayList<>();
					if (null != ProductionPlanner.config.getHostAlias()) {
						String[] hostAliasParts = ProductionPlanner.config.getHostAlias().split(":");
						if (2 != hostAliasParts.length) {
							logger.log(PlannerMessage.MALFORMED_HOST_ALIAS, ProductionPlanner.config.getHostAlias());
						} else {
							V1HostAlias hostAlias = new V1HostAlias().ip(hostAliasParts[0])
									.hostnames(Arrays.asList(hostAliasParts[1].split(",")));
							hostAliases.add(hostAlias);
						}
					}

					// Build the job specification
					V1JobSpec jobSpec = new V1JobSpecBuilder().withNewTemplate()
							.withNewMetadata()
							.withName(jobName + "spec")
							.addToLabels("jobgroup", jobName + "spec")
							.endMetadata()
							.withNewSpec()
							.addToImagePullSecrets(new V1LocalObjectReference().name("proseo-regcred"))
							.addAllToHostAliases(hostAliases)
							.addNewContainer()
							.withName(containerName)
							.withImage(imageName)
							.withImagePullPolicy("Always")
							.addNewEnv()
							.withName("NODE_IP")
							.withValueFrom(es)
							.endEnv()
							.addNewEnv()
							.withName("JOBORDER_FILE")
							.withValue(jobOrderDataLoc.jobOrder.getFileName())
							.endEnv()
							.addNewEnv()
							.withName("JOBORDER_VERSION")
							.withValue(jobOrderDataLoc.jobOrderVersion.toString())
							.endEnv()
							.addNewEnv()
							.withName("STORAGE_ENDPOINT")
							.withValue(localStorageManagerUrl)
							.endEnv()
							.addNewEnv()
							.withName("STORAGE_USER")
							.withValue(wrapUser)
							.endEnv()
							.addNewEnv()
							.withName("STORAGE_PASSWORD")
							.withValue(ProductionPlanner.config.getWrapperPassword())
							.endEnv()
							.addNewEnv()
							.withName("STATE_CALLBACK_ENDPOINT")
							.withValue(ProductionPlanner.config.getProductionPlannerUrl() + "/processingfacilities/" + this.kubeConfig.getId()
							+ "/finish/" + jobName)
							.endEnv()
							.addNewEnv()
							.withName("PROCESSING_FACILITY_NAME")
							.withValue(this.kubeConfig.getId())
							.endEnv()
							.addNewEnv()
							.withName("INGESTOR_ENDPOINT")
							.withValue(ProductionPlanner.config.getIngestorUrl())
							.endEnv()
							.addNewEnv()
							.withName("PROSEO_USER")
							.withValue(wrapUser)
							.endEnv()
							.addNewEnv()
							.withName("PROSEO_PW")
							.withValue(ProductionPlanner.config.getWrapperPassword())
							.endEnv()
							.addNewEnv()
							.withName("LOCAL_FS_MOUNT")
							.withValue(localMountPoint)
							.endEnv()
							.addNewEnv()
							.withName("FILECHECK_MAX_CYCLES")
							.withValue(ProductionPlanner.config.getProductionPlannerFileCheckMaxCycles().toString())
							.endEnv()
							.addNewEnv()
							.withName("FILECHECK_WAIT_TIME")
							.withValue(ProductionPlanner.config.getProductionPlannerFileCheckWaitTime().toString())
							.endEnv()
							.addNewVolumeMount()
							.withName("proseo-mnt")
							.withMountPath(localMountPoint)
							.endVolumeMount()
							.withResources(requirements)
							.endContainer()
							.addNewVolume()
							.withName("proseo-mnt")
							.withNewPersistentVolumeClaim()
							.withClaimName("proseo-nfs")
							.endPersistentVolumeClaim()
							.endVolume()
							.withRestartPolicy("Never")
							.withHostNetwork(true)
							.withDnsPolicy("ClusterFirstWithHostNet")
							.endSpec()
							.endTemplate()
							.withBackoffLimit(0)
							.build();

					// Build the job object
					return new V1JobBuilder().withNewMetadata()
							.withName(jobName)
							.addToLabels("jobgroup", jobName + "spec")
							.endMetadata()
							.withSpec(jobSpec)
							.build();

				});
				job = jobLoc;
				break;
			} catch (CannotAcquireLockException e) {
				if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

				if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
					ProseoUtil.dbWait();
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Creating job {}", job.toString());
		}

		// Create the Kubernetes job
		Integer cycle = ProductionPlanner.config.getProductionPlannerJobCreatedWaitTime();
		for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
			try {
				job = kubeConfig.getBatchApiV1().createNamespacedJob(kubeConfig.getNamespace(), job, null, null, null);
				break;
			} catch (ApiException e) {
				// look whether job was created or the exception was "real"
				if (logger.isTraceEnabled()) 
					logger.trace("    createNamespacedJob: ApiException, retry {} of {}", i, jobName);
				if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
					Thread.sleep(500);
					// search job/pod
					searchPod();
					if (podNames.get(podNames.size() - 1).startsWith(jobName)) {
						// job was created
						job = kubeConfig.getBatchApiV1().readNamespacedJob(jobName, kubeConfig.getNamespace(), null, false, false);
						if (logger.isTraceEnabled()) 
							logger.trace("    createNamespacedJob: retry {} of {} successful", i, jobName);
						break;
					}
				} else {
					throw e;
				}
			}
		}
		logger.log(PlannerMessage.JOB_CREATED, job.getMetadata().getName(), job.getStatus().toString());
		// Prepare for transaction retry, if "org.springframework.dao.CannotAcquireLockException" is thrown
		for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
			try {


				transactionTemplate.execute(status -> {
					// Find the job step in the database
					Optional<JobStep> jobStepOptional = RepositoryService.getJobStepRepository().findById(this.getJobId());
					if (jobStepOptional.isEmpty()) {
						logger.log(PlannerMessage.JOB_STEP_NOT_FOUND, this.getJobId());
						return null;
					}
					JobStep jobStep = jobStepOptional.get();

					// Start the job step
					jobStep.setProcessingStartTime(Instant.now());
					UtilService.getJobStepUtil().startJobStep(jobStep);
					logger.log(PlannerMessage.KUBEJOB_CREATED, this.kubeConfig.getId(), jobName);

					return null;

				}); 
				break;
			} catch (CannotAcquireLockException e) {
				if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

				if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
					ProseoUtil.dbWait();
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			} catch (Exception e) {
				logger.log(PlannerMessage.JOB_STEP_CREATION_EXCEPTION, this.getJobId(), e.getMessage());

				if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

				throw e;
			}
		}
		// Wait for the time span configured for K8 job creation
		if (cycle == null) {
			cycle = 2000;
		}
		Thread.sleep(cycle);
		for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
			try {

				transactionTemplate.execute(status -> {
					// Find the job step in the database
					Optional<JobStep> jobStepOptional = RepositoryService.getJobStepRepository().findById(this.getJobId());
					if (jobStepOptional.isEmpty()) {
						logger.log(PlannerMessage.JOB_STEP_NOT_FOUND, this.getJobId());
						return null;
					}
					JobStep jobStep = jobStepOptional.get();

					// Search for pods associated with the kube job, update the job log, and save the job step to the repository.
					searchPod();
					updateJobLog(jobStep);
					RepositoryService.getJobStepRepository().save(jobStep);

					return null;

				});
				break;
			} catch (CannotAcquireLockException e) {
				if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

				if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
					ProseoUtil.dbWait();
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			} catch (Exception e) {
				logger.log(PlannerMessage.JOB_STEP_CREATION_EXCEPTION, this.getJobId(), e.getMessage());

				if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

				throw e;
			}

		}
		if (logger.isTraceEnabled())
			logger.trace("<<< createJob finished successfully");

		return this;
	}

	/**
	 * Gets the kube configuration
	 *
	 * @return the kubeConfig
	 */
	public KubeConfig getKubeConfig() {
		return kubeConfig;
	}

	/** Searches for pods associated with the kube job and populates the podNames list with the names of the found pods. */
	public void searchPod() {
		if (logger.isTraceEnabled())
			logger.trace(">>> searchPod()");

		if (kubeConfig != null && kubeConfig.isConnected()) {
			V1PodList podList;

			for (int retryNumber = 0; retryNumber < ProseoUtil.K8S_MAX_RETRY; ) {
				++retryNumber;
				
				try {
					// Retrieve the pod list for the namespace
					podList = kubeConfig.getApiV1().listNamespacedPod(kubeConfig.getNamespace(), null, null, null, null, null, null,
							null, null, 30, null);
					podNames.clear();

					for (V1Pod pod : podList.getItems()) {
						String podName = pod.getMetadata().getName();

						// Filter pods based on the job name prefix
						if (podName.startsWith(jobName)) {
							podNames.add(podName);
							if (logger.isTraceEnabled())
								logger.trace("     Pod found: {}", podName);
						}
					}
					break;
				} catch (ApiException e) {
					logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);
				}
				
				if (retryNumber < ProseoUtil.K8S_MAX_RETRY) {
					ProseoUtil.kubeWait(retryNumber);
				}
			}
		}

		if (logger.isTraceEnabled())
			logger.trace("<<< searchPod()");
	}

	/**
	 * If the kube configuration is available, search for pods associated with the job, retrieve the job information, and update the
	 * job step accordingly. Then, start a KubeJobFinish thread to monitor the completion of the kube job.
	 *
	 * @param kubeConfig The kube configuration associated with the processing facility
	 * @param jobName    The name of the kube job
	 */
	public void finish(KubeConfig kubeConfig, String jobName) {
		if (logger.isTraceEnabled())
			logger.trace(">>> finish({}, {})", (null == kubeConfig ? "null" : kubeConfig.getId()), jobName);

		// Set kubeConfig if not already provided
		if (kubeConfig != null || this.kubeConfig != null) {
			if (this.kubeConfig == null) {
				this.kubeConfig = kubeConfig;
			}
		} else {
			// TODO Maybe log?
			return;
		}

		List<JobStep> jobSteps = new ArrayList<>(); // TODO Is this list actually needed?

		// Search for pods associated with the job
		searchPod();

		TransactionTemplate transactionTemplate = new TransactionTemplate(kubeConfig.getProductionPlanner().getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
			try {
				transactionTemplate.execute((status) -> {

					// Retrieve the kube job information
					V1Job job = kubeConfig.getV1Job(jobName);
					if (job == null) {
						return null;
					}

					// Find the job step in the database
					Long jobStepId = this.getJobId();
					Optional<JobStep> jobStep = RepositoryService.getJobStepRepository().findById(jobStepId);
					if (jobStep.isEmpty()) {
						return null;
					}

					// Update the job log
					updateJobLog(jobStep.get());

					if (job.getStatus() != null) {
						// Set the processing start time
						OffsetDateTime startTime = job.getStatus().getStartTime();
						if (startTime != null) {
							jobStep.get().setProcessingStartTime(startTime.toInstant());
						}

						// Set the processing completion time
						OffsetDateTime completionTime = job.getStatus().getCompletionTime();
						if (completionTime != null) {
							jobStep.get().setProcessingCompletionTime(completionTime.toInstant());
						}

						// Set the job conditions
						if (job.getStatus().getConditions() != null) {
							List<V1JobCondition> jobConditions = job.getStatus().getConditions();

							for (V1JobCondition jobCondition : jobConditions) {
								if ((jobCondition.getType().equalsIgnoreCase("complete")
										|| jobCondition.getType().equalsIgnoreCase("completed"))
										&& jobCondition.getStatus().equalsIgnoreCase("true")) {
									jobStep.get().setJobStepState(JobStepState.COMPLETED);
									UtilService.getJobStepUtil().checkCreatedProducts(jobStep.get());
									jobStep.get().incrementVersion();
								} else if (jobCondition.getType().equalsIgnoreCase("failed")
										|| jobCondition.getType().equalsIgnoreCase("failure")) {
									jobStep.get().setJobStepState(JobStepState.FAILED);
									jobStep.get().incrementVersion();
								}
							}
						}
					}

					// Save the updated job step in the repository
					RepositoryService.getJobStepRepository().save(jobStep.get());

					// Log the order state for the job step's processing order
					UtilService.getOrderUtil().logOrderState(jobStep.get().getJob().getProcessingOrder());

					jobSteps.add(jobStep.get());
					return jobStep.get();
				});
				break;
			} catch (CannotAcquireLockException e) {
				if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

				if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
					ProseoUtil.dbWait();
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			} catch (Exception e) {
				logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());

				if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
			}
		} 

		// Configure and start a KubeJobFinish object to monitor the completion of the kube job
		KubeJobFinish jobMonitor = new KubeJobFinish(this, kubeConfig.getProductionPlanner(), jobName);
		kubeConfig.getProductionPlanner().getFinishThreads().put(jobName, jobMonitor);
		jobMonitor.start();

	}

	/*
	 * TODO Analyze pod. The phase of a Pod is a simple, high-level summary of where the Pod is in its lifecycle. The conditions
	 * array, the reason and message fields, and the individual container status arrays contain more detail about the pod's status.
	 * There are five possible phase values:
	 * 
	 * Pending: The pod has been accepted by the Kubernetes system, but one or more of the container images has not been created.
	 * This includes time before being scheduled as well as time spent downloading images over the network, which could take a
	 * while.
	 * 
	 * Running: The pod has been bound to a node, and all of the containers have been created. At least one container is still
	 * running, or is in the process of starting or restarting.
	 * 
	 * Succeeded: All containers in the pod have terminated in success, and will not be restarted.
	 * 
	 * Failed: All containers in the pod have terminated, and at least one container has terminated in failure. The container either
	 * exited with non-zero status or was terminated by the system. *
	 * 
	 * Unknown: For some reason the state of the pod could not be obtained, typically due to an error in communicating with the host
	 * of the pod. More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#pod-phase
	 *
	 * Conditions: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-conditions
	 */

	/**
	 * Update all the information of a Kubernetes job which is stored in the job step.
	 *
	 * This method retrieves the information of a Kubernetes job with the provided name and updates the corresponding job step
	 * accordingly. It also performs additional checks and updates based on the job and pod status. The method returns the success
	 * status of the update operation.
	 *
	 * @param jobName The name of the Kubernetes job
	 * @return The result of the update operation: TRUE if the update was successful, FALSE if an error occurred, and CHANGED if the
	 *         job step was modified by others during the update process
	 */
	public Boolean updateInfo(String jobName) {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateInfo({})", jobName);

		// Ensure that the kube configuration is present and connected, and that a job name was provided
		if (kubeConfig == null || !kubeConfig.isConnected() || jobName == null) {
			if (logger.isTraceEnabled())
				logger.trace("<<< updateInfo({})", jobName);

			return Boolean.FALSE;
		}

		// Check whether a job can be found under the provided name
		V1Job job = kubeConfig.getV1Job(jobName);
		if (job == null) {
			if (logger.isTraceEnabled())
				logger.trace("    updateInfo: job not found");

			// TODO Why are we returning true?
			return Boolean.TRUE;
		}

		// If not already known, search for associated pods
		if (podNames.isEmpty()) {
			searchPod();
		}
		if (podNames.isEmpty()) {
			if (logger.isTraceEnabled())
				logger.trace("    updateInfo: pod not found");

			return Boolean.FALSE;
		}

		// Retrieve the latest associated pod
		V1Pod pod = kubeConfig.getV1Pod(podNames.get(podNames.size() - 1));
		if (pod == null) {
			// No pod found for the stored name
			if (logger.isTraceEnabled())
				logger.trace("    updateInfo: pod with name {} not found", podNames.get(podNames.size() - 1));

			return Boolean.FALSE;
		}

		TransactionTemplate transactionTemplate = new TransactionTemplate(this.kubeConfig.getProductionPlanner().getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
			try {
				return transactionTemplate.execute((status) -> {

					Boolean success = Boolean.FALSE;
					Long jobStepId = this.getJobId();
					Optional<JobStep> jobStep = RepositoryService.getJobStepRepository().findById(jobStepId);

					if (!jobStep.isPresent()) {
						// No job step present, everything up to date
						if (logger.isTraceEnabled())
							logger.trace("<<< updateInfo({})", jobName);

						return success;
					}

					try {
						if (job.getStatus() != null) {
							if (logger.isTraceEnabled())
								logger.trace("    updateInfo: analyze job state");

							// Set start time
							OffsetDateTime startTime = job.getStatus().getStartTime();
							if (startTime != null) {
								jobStep.get().setProcessingStartTime(startTime.toInstant());
							}

							// Set completion time
							OffsetDateTime completionTime = job.getStatus().getCompletionTime();
							if (completionTime != null) {
								jobStep.get().setProcessingCompletionTime(completionTime.toInstant());
							} else {
								// TODO Something is wrong with the job, try to get info from pod
							}

							// Set job conditions
							if (job.getStatus().getConditions() != null) {
								List<V1JobCondition> jobConditions = job.getStatus().getConditions();

								for (V1JobCondition jobCondition : jobConditions) {
									if ((jobCondition.getType().equalsIgnoreCase("complete")
											|| jobCondition.getType().equalsIgnoreCase("completed"))
											&& jobCondition.getStatus().equalsIgnoreCase("true")) {

										// Plan failed job steps
										if (jobStep.get().getJobStepState() == de.dlr.proseo.model.JobStep.JobStepState.FAILED) {
											jobStep.get().setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.PLANNED);
										}

										// Run ready job steps
										if (JobStepState.READY.equals(jobStep.get().getJobStepState())) {
											// Sometimes we don't get the state transition to RUNNING
											if (logger.isTraceEnabled())
												logger.trace("... fixing state {} of job step {} to RUNNING",
														jobStep.get().getJobStepState(), jobStep.get().getId());

											jobStep.get().setJobStepState(JobStepState.RUNNING);
										} else if (JobStepState.PLANNED.equals(jobStep.get().getJobStepState())
												|| JobStepState.WAITING_INPUT.equals(jobStep.get().getJobStepState())) {
											// Sometimes we don't even get the state transition to READY
											if (logger.isTraceEnabled())
												logger.trace("... fixing state {} of job step {} to RUNNING",
														jobStep.get().getJobStepState(), jobStep.get().getId());

											jobStep.get().setJobStepState(JobStepState.READY);
											jobStep.get().setJobStepState(JobStepState.RUNNING);
										}

										// Mark closed job steps as completed
										if (!JobStepState.CLOSED.equals(jobStep.get().getJobStepState())) {
											jobStep.get().setJobStepState(JobStepState.COMPLETED);
										}

										// Removing unnecessary component products and delete the main product if it has no more components
										// or files
										UtilService.getJobStepUtil().checkCreatedProducts(jobStep.get());

										// Set completion time if necessary
										if (completionTime == null) {
											completionTime = jobCondition.getLastProbeTime();
											jobStep.get().setProcessingCompletionTime(completionTime.toInstant());
										}

										success = Boolean.TRUE;
									} else if ((jobCondition.getType().equalsIgnoreCase("failed")
											|| jobCondition.getType().equalsIgnoreCase("failure"))
											&& jobCondition.getStatus().equalsIgnoreCase("true")) {

										// Run ready job steps
										if (JobStepState.READY.equals(jobStep.get().getJobStepState())) {
											jobStep.get().setJobStepState(JobStepState.RUNNING);
										} else if (JobStepState.PLANNED.equals(jobStep.get().getJobStepState())) {
											// TODO Why do we only look state PLANNED if the state is not READY?
											jobStep.get().setJobStepState(JobStepState.READY);
											jobStep.get().setJobStepState(JobStepState.RUNNING);
										}

										// Mark job steps that are neither completed nor closed as failed
										if (!JobStepState.COMPLETED.equals(jobStep.get().getJobStepState())
												&& !JobStepState.CLOSED.equals(jobStep.get().getJobStepState())) {
											jobStep.get().setJobStepState(JobStepState.FAILED);
										}

										if (completionTime == null) {
											completionTime = jobCondition.getLastProbeTime();
											jobStep.get().setProcessingCompletionTime(completionTime.toInstant());
										}

										success = Boolean.TRUE;
									}
								}

							}

							// TODO Cancel pod and job, write reasons into job step log.
							// TODO Check the pod for errors and warnings.
							// kubeConfig.getApiV1().listNamespacedEvent("default",null,false,null,"involvedObject.name=proseojob733-bwzf4",null,null,null,null,false);
						} else {
							if (logger.isTraceEnabled())
								logger.trace("    updateInfo: status not found");
						}
					} catch (Exception e) {
						logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());

						if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

						// Re-throw to roll back the transaction
						throw e;
					}

					// Update the log and save the modified job step
					updateJobLog(jobStep.get());
					if (success == Boolean.TRUE) {
						jobStep.get().incrementVersion();
						RepositoryService.getJobStepRepository().save(jobStep.get());
					}

					if (logger.isTraceEnabled())
						logger.trace("<<< updateInfo({}, {})", jobName, success);

					return success;
				});
			} catch (CannotAcquireLockException e) {
				if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

				if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
					ProseoUtil.dbWait();
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			}
		}
		return Boolean.FALSE;

	}

	/**
	 * Retrieves and store the completion information of the Kubernetes job specified by the given job name. Additionally, perform
	 * cleanup operations and delete the Kubernetes job.
	 *
	 * @param jobName The name of the Kubernetes job
	 * @return True if the update and deletion were successful, false otherwise
	 */
	public boolean updateFinishInfoAndDelete(String jobName) {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateFinishInfoAndDelete({})", jobName);

		Boolean success = updateInfo(jobName);

		// If the updateInfo was successful, perform additional operations
		if (success) {
			TransactionTemplate transactionTemplate = new TransactionTemplate(
					this.kubeConfig.getProductionPlanner().getTxManager());
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);		
			for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
				try {

					transactionTemplate.execute((status) -> {
						Long jobStepId = this.getJobId();
						Optional<JobStep> jobStep = RepositoryService.getJobStepRepository().findById(jobStepId);

						// Perform cleanup operations on the job step
						if (jobStep.isPresent()) {
							UtilService.getJobStepUtil().checkFinish(jobStep.get());
						}

						return null;
					});
					break;
				} catch (CannotAcquireLockException e) {
					if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

					if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
						ProseoUtil.dbWait();
					} else {
						if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
						throw e;
					}
				}
			}

			// Delete the Kubernetes job
			kubeConfig.deleteJob(jobName);
			logger.log(PlannerMessage.KUBEJOB_FINISHED, kubeConfig.getId(), jobName);
		}

		return success;
	}

	/**
	 * Sets the generation time of the given product to the provided time, and recursively sets the generation time for all its
	 * component products. If a retention period is specified, it also sets the eviction time of the product. The updated product is
	 * then saved in the repository.
	 *
	 * @param product         The product to set the generation time for
	 * @param genTime         The generation time to set
	 * @param retentionPeriod The retention period for the product
	 */
	private void setGenerationTime(Product product, Instant genTime, Duration retentionPeriod) {
		if (logger.isTraceEnabled())
			logger.trace(">>> setGenerationTime({}, {}, {})", (null == product ? "null" : product.getId()), genTime,
					retentionPeriod);

		if (product != null && genTime != null) {
			product.setGenerationTime(genTime);

			if (retentionPeriod != null) {
				product.setEvictionTime(genTime.plus(retentionPeriod));
			}

			// Recursively set the generation time for component products
			for (Product p : product.getComponentProducts()) {
				setGenerationTime(p, genTime, retentionPeriod);
			}

			// Save the updated product in the repository
			RepositoryService.getProductRepository().save(product);
		}
	}

	/**
	 * Locks a product and its component products for write access.
	 *
	 * @param product The product to lock
	 * @param em      The EntityManager used for locking
	 */
//	private void lockProduct(Product product, EntityManager em) {
//		// Set the lock timeout to 10,000 milliseconds
//		Map<String, Object> properties = new HashMap<>();
//		properties.put("javax.persistence.lock.timeout", 10000L);
//
//		if (product != null) {
//			if (logger.isTraceEnabled())
//				logger.trace("  lock product {}", product.getId());
//
//			try {
//				// Acquire a pessimistic write lock on the product
//				em.lock(product, LockModeType.PESSIMISTIC_WRITE, properties);
//			} catch (Exception e) {
//				logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
//				
//				if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
//			}
//
//			// Recursively lock the component products
//			for (Product p : product.getComponentProducts()) {
//				lockProduct(p, em);
//			}
//		}
//	}

	/**
	 * Retrieves the maximum value of CPUs required by the processor tasks.
	 *
	 * @param processor The Processor object
	 * @param cpus      The default value of CPUs
	 * @return The maximum value of CPUs required
	 */
	private Integer getCPUs(Processor processor, Integer cpus) {
		if (processor != null) {
			// Iterate over the tasks of the processor
			for (Task t : processor.getTasks()) {
				// Update 'cpus' if the task requires more CPUs than the current maximum
				if (t.getNumberOfCpus() != null && t.getNumberOfCpus() > cpus) {
					cpus = t.getNumberOfCpus();
				}
			}
		}

		return cpus;
	}

	/**
	 * Gets the minimum memory requirement among the tasks of a processor.
	 *
	 * @param processor        The Processor
	 * @param defaultMemoryMin The default minimum memory value
	 * @return The minimum memory requirement among the tasks
	 */
	private Integer getMinMemory(Processor processor, Integer defaultMemoryMin) {
		if (processor != null) {
			for (Task task : processor.getTasks()) {
				if (task.getMinMemory() != null && task.getMinMemory() > defaultMemoryMin) {
					defaultMemoryMin = task.getMinMemory();
				}
			}
		}

		return defaultMemoryMin;
	}

	/**
	 * Updates the job step log based on the latest pod's log.
	 *
	 * @param jobStep The JobStep to update the log for
	 */
	private void updateJobLog(JobStep jobStep) {
		if (jobStep != null && !podNames.isEmpty()) {
			V1Pod pod = kubeConfig.getV1Pod(podNames.get(podNames.size() - 1));
			String log = getJobStepLogPrim(pod);

			jobStep.setProcessingStdOut(log);
		}
	}

	/**
	 * Retrieves the log information for a job step from the associated pod.
	 *
	 * @param pod The V1Pod object representing the associated pod
	 * @return The log information for the job step
	 */
	public String getJobStepLogPrim(V1Pod pod) {
		StringBuilder podMessages = new StringBuilder("");

		// Retrieve pod conditions and construct a message string for each condition
		if (pod != null && pod.getStatus().getConditions() != null) {
			podMessages.append("Job Step Conditions (Type - Status):\n");

			List<V1PodCondition> podConditions = pod.getStatus().getConditions();
			for (V1PodCondition podCondition : podConditions) {
				podMessages.append("  " + podCondition.getType() + " - " + podCondition.getStatus() + "\n");
			}

			// Retrieve events related to the pod using field selector and append them to the message string
			String fieldSelector = "involvedObject.name==" + pod.getMetadata().getName();
			CoreV1EventList events = null;
			try {
				events = kubeConfig.getApiV1()
					.listEventForAllNamespaces(false, null, fieldSelector, null, 30, null, null, null, null, null);

				if (events != null) {
					podMessages.append("Job Step Events (Type - Reason - Count - Message):\n");

					for (CoreV1Event event : events.getItems()) {
						podMessages.append("  " + event.getType() + " - " + event.getReason() + " - " + event.getCount() + " - "
								+ event.getMessage() + "\n");
					}

					podMessages.append("\n\n");
				}
			} catch (ApiException e) {
				logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
				
				if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
			}
		}

		String containerName = this.getContainerName();
		String log = "";

		// Read the pod log for the specified container name
		if (containerName != null) {
			try {
				log = kubeConfig.getApiV1()
					.readNamespacedPodLog(podNames.get(podNames.size() - 1), kubeConfig.getNamespace(), containerName, null, null,
							null, null, null, null, null, null);
			} catch (ApiException e1) {
				if (logger.isTraceEnabled())
					logger.trace("    updateInfo: ApiException ignore, normally the pod has no log");
			} catch (Exception e) {
				logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getClass() + " - " + e.getMessage());
				
				if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);
			}
		} else {
			if (logger.isTraceEnabled())
				logger.trace("    updateJobLog: container not found");
		}

		// Combine pod messages and log, or return only pod messages if log is blank or null
		if (log != null && !log.isBlank()) {
			return podMessages.append(log).toString();
		} else {
			return podMessages.toString();
		}
	}

	private void reexecuteProductQueries() {

		TransactionTemplate transactionTemplate = new TransactionTemplate(this.kubeConfig.getProductionPlanner().getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		transactionTemplate.setReadOnly(false);
		transactionTemplate.execute(status -> {

			// Find the job step in the database
			Optional<JobStep> jobStepOptional = RepositoryService.getJobStepRepository().findById(this.getJobId());
			if (jobStepOptional.isEmpty()) {
				logger.log(PlannerMessage.JOB_STEP_NOT_FOUND, this.getJobId());
				return null;
			}
			JobStep js = jobStepOptional.get();

			// Iterate over the input product queries of the job step
			for (ProductQuery productQuery : js.getInputProductQueries()) {
				// Execute the product query always
				ProductQueryService productQueryService = UtilService.getJobStepUtil().getProductQueryService();
				if (productQueryService.executeQuery(productQuery, false)) {
					// If the query is successfully executed, update its state and save
					RepositoryService.getProductQueryRepository().save(productQuery);
				}
			}	
			return null;
		});  
	}
}