/**
 * SlurmJob.java
 *
 * © 2026 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.engine.slurm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.JobStep.StdLogLevel;
import de.dlr.proseo.model.enums.JobOrderVersion;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.ProductionPlannerConfiguration;
import de.dlr.proseo.planner.dispatcher.JobDispatcher;
import de.dlr.proseo.planner.engine.EngineConfig;
import de.dlr.proseo.planner.engine.EngineJob;
import de.dlr.proseo.planner.util.UtilService;
import de.dlr.proseo.planner.service.ServiceConnection.SlurmException;
import tools.jackson.databind.ObjectMapper;

/**
 * A SlurmJob describes the complete information to run a slurm job.
 *
 * @author Ernst Melchinger
 *
 */
public class SlurmJob extends EngineJob {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(SlurmJob.class);

	private long slurmJobId;
	
	private SlurmConfig  slurmConfig;
		
	private ProductionPlannerConfiguration config;
	
	/**
	 * @return the slurmJobId
	 */
	public long getSlurmJobId() {
		return slurmJobId;
	}
	
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
	 * @param slurmJobId the slurmJobId to set
	 */
	public void setSlurmJobId(long slurmJobId) {
		this.slurmJobId = slurmJobId;
	}

	public SlurmJob(long jobId, String jobOrderFileName) {
		super(jobId, jobOrderFileName);
		config = ProductionPlanner.config;
	}

	@Override
	public String getJobStepLog(EngineConfig engineConfig) {
		String fName = config.getWorkingDirectoryLocal() + "/" + jobName + "-" + slurmJobId + ".out";
		Path path = Path.of(fName);		
		File aFile = path.toFile();
		String log = "";
		if (aFile.exists() && aFile.canRead()) {
			try {
				log = Files.readString(path);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return log;
	}
	
	private List<String> addEnvVar(List<String> environment, String key, String value) {
		if (environment == null) {
			environment = new ArrayList<String>();
		}
		environment.add(key + "=" + value);
		return environment;
	}

	@Override
	public SlurmJob createJob(EngineConfig engineConfigVar, String stdoutLogLevel, String stderrLogLevel)
			throws Exception {
		if (logger.isTraceEnabled())
			logger.trace(">>> createJob({}, {}, {})", engineConfigVar, stdoutLogLevel, stderrLogLevel);

		// Ensure that the slurm configuration is given
		this.engineConfig = engineConfigVar;
		if (engineConfigVar instanceof SlurmConfig) {
			this.slurmConfig = (SlurmConfig)engineConfigVar;
		} else {
			return null;
		}
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(this.engineConfig.getProductionPlanner().getTxManager());
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
		jobOrderData.jobOrder = jobDispatcher.sendJobOrderToStorageManager(this.slurmConfig, jobOrderData.jobOrder, jobOrderData.jobOrderVersion);
		if (jobOrderData.jobOrder == null) {
			throw new Exception(logger.log(PlannerMessage.SENDING_JOB_STEP_FAILED, this.getJobId()));
		}
		final JobOrderData jobOrderDataLoc = jobOrderData;
		Map<String, Object> job = null;
		// Prepare for transaction retry, if "org.springframework.dao.CannotAcquireLockException" is thrown
		for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
			try {

				Map<String, Object> jobLoc = transactionTemplate.execute(status -> {
					// Find the job step in the database
					Optional<JobStep> jobStepOptional = RepositoryService.getJobStepRepository().findById(this.getJobId());
					if (jobStepOptional.isEmpty()) {
						logger.log(PlannerMessage.JOB_STEP_NOT_FOUND, this.getJobId());
						return null;
					}
					JobStep jobStep = jobStepOptional.get();

					jobStep.setJobOrderFilename(jobOrderDataLoc.jobOrder.getFileName());

					// Construct the environment variables and configuration for the slurm job
					String missionCode = jobStep.getJob().getProcessingOrder().getMission().getCode();
					String wrapUser = missionCode + "-" + ProductionPlanner.config.getWrapperUser();
					imageName = jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getDockerImage();
					String localMountPoint = ProductionPlanner.config.getPosixWorkerMountPoint();
					String volumeMount = ProductionPlanner.config.getSlurmVolumeMount();
					// Configure the compute resource requirements
					
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
//					requirements.putRequestsItem("cpu", new Quantity(cpus))
//					.putRequestsItem("memory", new Quantity(mem))
//					.putRequestsItem("ephemeral-storage", new Quantity(minDiskSpace));
//					V1EnvVarSource es = new V1EnvVarSourceBuilder().withNewFieldRef().withFieldPath("status.hostIP").endFieldRef().build();
					String localStorageManagerUrl = this.engineConfig.getLocalStorageManagerUrl();

					// Create a host alias, if given in the config file, for use in the Planner and Ingestor URLs
//					List<V1HostAlias> hostAliases = new ArrayList<>();
//					if (null != ProductionPlanner.config.getHostAlias()) {
//						String[] hostAliasParts = ProductionPlanner.config.getHostAlias().split(":");
//						if (2 != hostAliasParts.length) {
//							logger.log(PlannerMessage.MALFORMED_HOST_ALIAS, ProductionPlanner.config.getHostAlias());
//						} else {
//							V1HostAlias hostAlias = new V1HostAlias().ip(hostAliasParts[0])
//									.hostnames(Arrays.asList(hostAliasParts[1].split(",")));
//							hostAliases.add(hostAlias);
//						}
//					}

					// Build the job specification
					Map<String, Object> jobSpec = new HashMap<>();
					jobSpec.put("name", jobName);

					// TODO			.withImage(imageName)

					List<String> environment = new ArrayList<String>();
					addEnvVar(environment, "JOBORDER_FILE", jobOrderDataLoc.jobOrder.getFileName());
					addEnvVar(environment, "JOBORDER_VERSION", jobOrderDataLoc.jobOrderVersion.toString());
					addEnvVar(environment, "STORAGE_ENDPOINT", localStorageManagerUrl);
					addEnvVar(environment, "STORAGE_USER", wrapUser);
					addEnvVar(environment, "STORAGE_PASSWORD", ProductionPlanner.config.getWrapperPassword());
					addEnvVar(environment, "STATE_CALLBACK_ENDPOINT", ProductionPlanner.config.getProductionPlannerUrl() + "/processingfacilities/" + this.engineConfig.getId()
					+ "/finish/" + jobName);
					addEnvVar(environment, "PROCESSING_FACILITY_NAME", this.engineConfig.getId());
					addEnvVar(environment, "INGESTOR_ENDPOINT", ProductionPlanner.config.getIngestorUrl());
					addEnvVar(environment, "PROSEO_USER", wrapUser);
					addEnvVar(environment, "PROSEO_PW", ProductionPlanner.config.getWrapperPassword());
					addEnvVar(environment, "LOCAL_FS_MOUNT", localMountPoint);
					addEnvVar(environment, "FILECHECK_MAX_CYCLES", ProductionPlanner.config.getProductionPlannerFileCheckMaxCycles().toString());
					addEnvVar(environment, "FILECHECK_WAIT_TIME", ProductionPlanner.config.getProductionPlannerFileCheckWaitTime().toString());
					addEnvVar(environment, "NODE_IP", "TODO");

					jobSpec.put("environment", environment);
					jobSpec.put("standard_output", jobName + "-%j.out");
					jobSpec.put("standard_error", jobName + "-%j.err");
					jobSpec.put("account", "root");
					jobSpec.put("current_working_directory", "/data");
					// Build the job object
					Map<String, Object> jobSubmitReq = new HashMap<>();
					String dockerParams = "";
					Map<String, String> params =jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getDockerRunParameters();
					for (String key : jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getDockerRunParameters().keySet()) {
						dockerParams += " " + params.get(key);
					}
					String scr = "#!/bin/bash\ndocker run --rm"
							+ dockerParams
							+ " --name=" + jobName
							+ " -e JOBORDER_FILE=$JOBORDER_FILE"
							+ " -e JOBORDER_VERSION=$JOBORDER_VERSION"
							+ " -e STORAGE_ENDPOINT=$STORAGE_ENDPOINT"
							+ " -e STORAGE_USER=$STORAGE_USER"
							+ " -e STORAGE_PASSWORD=$STORAGE_PASSWORD"
							+ " -e STATE_CALLBACK_ENDPOINT=$STATE_CALLBACK_ENDPOINT"
							+ " -e PROCESSING_FACILITY_NAME=$PROCESSING_FACILITY_NAME"
							+ " -e INGESTOR_ENDPOINT=$INGESTOR_ENDPOINT"
							+ " -e PROSEO_USER=$PROSEO_USER"
							+ " -e PROSEO_PW=$PROSEO_PW"
							+ " -e LOCAL_FS_MOUNT=$LOCAL_FS_MOUNT"
							+ " -e FILECHECK_MAX_CYCLES=$FILECHECK_MAX_CYCLES"
							+ " -e FILECHECK_WAIT_TIME=$FILECHECK_WAIT_TIME"
							+ " --cpus " + cpus
							+ " --mount '" + volumeMount + "'"
							+ " " + imageName;

					jobSubmitReq.put("script", scr);
					jobSubmitReq.put("job", jobSpec);
					// jobSubmitReq.
					return jobSubmitReq;

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

		if (logger.isDebugEnabled()) {
			ObjectMapper mapper = new ObjectMapper();
			String s = mapper.writeValueAsString(job);
			logger.trace("Creating job {}", s);
		}
		Integer cycle = ProductionPlanner.config.getProductionPlannerJobCreatedWaitTime();
		
		ObjectMapper mapper = new ObjectMapper();
		String s = mapper.writeValueAsString(job);
		String result = "";
		for (int ii = 0; ii < ProseoUtil.DB_MAX_RETRY; ii++) {
			try {
				result = slurmConfig.getProductionPlanner().getServiceConnection().postToSlurm(slurmConfig.getProcessingFacility().getProcessingEngineUrl(), 
						SlurmConfig.CALL_SLURM_0045 + "/job/submit", s, String.class, slurmConfig.getProcessingFacility().getProcessingEngineToken());
				break;
			} catch (SlurmException e) {
				// Get current token and retry
				if ((ii + 1) < ProseoUtil.DB_MAX_RETRY) {
					// get new token
					try {
						slurmConfig.refreshToken();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			}
		}
		Map<?,?> m = mapper.readValue(result, Map.class);
		if (m.get("job_id") != null && m.get("job_id") instanceof Integer) {
			this.slurmJobId = (Integer)(m.get("job_id"));
			List<?> a = (List<?>) m.get("errors");
			for (Object o : a) {
				if (o instanceof Map) {
					if (((Map<?,?>)o).get("error_number").equals(1007)) {
						// Protocol authentication error
						// get new token
					}
				}
			}
		}
		logger.log(PlannerMessage.JOB_CREATED, (String)job.get("name"), "TODO");
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
					logger.log(PlannerMessage.SLURMJOB_CREATED, this.engineConfig.getId(), jobName);

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

					// Search for pods associated with the slurm job, update the job log, and save the job step to the repository.
					// searchPod();
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

	@Override
	public void finish(EngineConfig engineConfigVar, String jobName) {
		if (logger.isTraceEnabled())
			logger.trace(">>> finish({}, {})", (null == engineConfigVar ? "null" : engineConfigVar.getId()), jobName);

		// Set engineConfig if not already provided
		if ((engineConfigVar != null && engineConfigVar instanceof SlurmConfig) 
				|| this.engineConfig != null && this.engineConfig instanceof SlurmConfig) {
			if (this.engineConfig == null) {
				this.slurmConfig = (SlurmConfig)engineConfigVar;
				this.engineConfig = (SlurmConfig)engineConfigVar;
			} else {
				this.slurmConfig = (SlurmConfig)engineConfig;
			}
		} else {
			// TODO Maybe log?
			return;
		}

		List<JobStep> jobSteps = new ArrayList<>(); // TODO Is this list actually needed?

		TransactionTemplate transactionTemplate = new TransactionTemplate(engineConfig.getProductionPlanner().getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
			try {
				transactionTemplate.execute((status) -> {

					// Find the job step in the database
					Long jobStepId = this.getJobId();
					Optional<JobStep> jobStep = RepositoryService.getJobStepRepository().findById(jobStepId);
					if (jobStep.isEmpty()) {
						return null;
					}
					updateInfo(jobName);
					// Update the job log
					updateJobLog(jobStep.get());

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

		// Configure and start a SlurmJobFinish object to monitor the completion of the slurm job
		SlurmJobFinish jobMonitor = new SlurmJobFinish(this, engineConfig.getProductionPlanner(), jobName);
		engineConfig.getProductionPlanner().getFinishThreads().put(jobName, jobMonitor);
		jobMonitor.start();

		
	}

	@Override
	public Boolean updateInfo(String jobName) {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateInfo({})", jobName);

		// Ensure that the Slurm configuration is present and connected, and that a job name was provided
		if (engineConfig == null || !engineConfig.isConnected() || jobName == null) {
			if (logger.isTraceEnabled())
				logger.trace("<<< updateInfo({})", jobName);

			return Boolean.FALSE;
		}


		ObjectMapper mapper = new ObjectMapper();
		String result = "";
		for (int ii = 0; ii < ProseoUtil.DB_MAX_RETRY; ii++) {
			try {
				result = slurmConfig.getProductionPlanner().getServiceConnection().getFromSlurm(slurmConfig.getProcessingFacility().getProcessingEngineUrl(), 
						SlurmConfig.CALL_SLURM_0045 + "/job/" + slurmJobId, String.class, slurmConfig.getProcessingFacility().getProcessingEngineToken());
				break;
			} catch (SlurmException e) {
				// Get current token and retry
				if ((ii + 1) < ProseoUtil.DB_MAX_RETRY) {
					// get new token
					try {
						slurmConfig.refreshToken();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else {
					if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
					throw e;
				}
			}
		}
		Map<?,?> m = mapper.readValue(result, Map.class);
		// jobs
		if (m.get("jobs") != null && m.get("jobs") instanceof List) {
			if (((List<?>)(m.get("jobs"))).size() <= 0) {
				return false;
			}
		} else {
			return false;
		}
		TransactionTemplate transactionTemplate = new TransactionTemplate(this.engineConfig.getProductionPlanner().getTxManager());
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
						Instant startTime = null;
						Instant endTime = null;
						JobStepState jobState = null;
						if (m.get("jobs") != null && m.get("jobs") instanceof List) {
							for (Object lo : (List<?>)(m.get("jobs"))) {
								if (lo instanceof Map) {								
									Map<?,?> jo = (Map<?,?>)lo;
									if (jo.get("job_state") != null && jo.get("job_state") instanceof List) {
										for (Object state : (List<?>)(jo.get("job_state"))) {
											if (state instanceof String && "COMPLETED".equalsIgnoreCase((String)state)) {
												jobState = JobStepState.COMPLETED;
											} else if (state instanceof String && "RUNNING".equalsIgnoreCase((String)state)) {
												jobState = JobStepState.RUNNING;
												success = true;
											} else if (state instanceof String && "PENDING".equalsIgnoreCase((String)state)) {
												jobState = JobStepState.RUNNING;
												success = true;
											} else if (state instanceof String && "FAILED".equalsIgnoreCase((String)state)) {
												jobState = JobStepState.FAILED;
											} else {
												jobState = JobStepState.FAILED;
											}
										}
									}
									if (jo.get("time") != null && jo.get("time") instanceof Map) {
										Map<?,?> time = (Map<?,?>)jo.get("time");
										if (time.get("start") != null && time.get("start") instanceof Integer) {
											startTime = Instant.ofEpochSecond(((Integer)(time.get("start"))).longValue());
										}
										if (time.get("end") != null && time.get("end") instanceof Integer) {
											endTime = Instant.ofEpochSecond(((Integer)(time.get("end"))).longValue());
										}

									}
									if (jo.get("start_time") != null && jo.get("start_time") instanceof Map) {
										Map<?,?> time = (Map<?,?>)jo.get("start_time");
										if (time.get("set") != null && time.get("set") instanceof Boolean && ((Boolean)(time.get("set")))) {
											if (time.get("number") != null && time.get("number") instanceof Integer) {
												startTime = Instant.ofEpochSecond(((Integer)(time.get("number"))).longValue());
											}}
									}
									if (jo.get("end_time") != null && jo.get("end_time") instanceof Map) {
										Map<?,?> time = (Map<?,?>)jo.get("end_time");
										if (time.get("set") != null && time.get("set") instanceof Boolean && ((Boolean)(time.get("set")))) {
											if (time.get("number") != null && time.get("number") instanceof Integer) {
												endTime = Instant.ofEpochSecond(((Integer)(time.get("number"))).longValue());
											}
										}
									}
								}

							}
						}

						if (startTime != null) {
							jobStep.get().setProcessingStartTime(startTime);
						}
						if (endTime != null) {
							jobStep.get().setProcessingCompletionTime(endTime);
						}

						if (jobState.equals(JobStepState.COMPLETED)) {
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
							success = Boolean.TRUE;
						} else if (jobState.equals(JobStepState.FAILED)) {

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

							success = Boolean.TRUE;
						}

							// Mark closed job steps as completed
							if (!JobStepState.CLOSED.equals(jobStep.get().getJobStepState())) {
								jobStep.get().setJobStepState(JobStepState.COMPLETED);
							}

							// Removing unnecessary component products and delete the main product if it has no more components
							// or files
							UtilService.getJobStepUtil().checkCreatedProducts(jobStep.get());

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

	@Override
	public boolean updateFinishInfoAndDelete(String jobName) {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateFinishInfoAndDelete({})", jobName);

		Boolean success = updateInfo(jobName);

		// If the updateInfo was successful, perform additional operations
		if (success) {
			TransactionTemplate transactionTemplate = new TransactionTemplate(
					this.engineConfig.getProductionPlanner().getTxManager());
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

			// Delete the slurm job
			engineConfig.deleteJob(jobName);
			logger.log(PlannerMessage.SLURMJOB_FINISHED, engineConfig.getId(), jobName);
		}

		return success;
	}

	@Override
	protected void updateJobLog(JobStep jobStep) {
		// TODO Auto-generated method stub
		String fName = config.getWorkingDirectoryLocal() + "/" + jobName + "-" + slurmJobId + ".out";
		Path path = Path.of(fName);		
		File aFile = path.toFile();
		String log = "";
		if (aFile.exists() && aFile.canRead()) {
			try {
				log = Files.readString(path);
				jobStep.setProcessingStdOut(log);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		fName = config.getWorkingDirectoryLocal() + "/" + jobName + "-" + slurmJobId + ".err";
		path = Path.of(fName);		
		aFile = path.toFile();
		String err = "";
		if (aFile.exists() && aFile.canRead() && aFile.length() > 0) {
			try {
				err = Files.readString(path);
				jobStep.setProcessingStdErr(err);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
