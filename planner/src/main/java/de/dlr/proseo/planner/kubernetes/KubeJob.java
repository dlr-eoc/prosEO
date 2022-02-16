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
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.JobStep.StdLogLevel;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.Task;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.enums.JobOrderVersion;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.JobDispatcher;
import de.dlr.proseo.planner.rest.model.PodKube;
import de.dlr.proseo.planner.util.UtilService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.CoreV1EventList;
import io.kubernetes.client.openapi.models.V1EnvVarSource;
import io.kubernetes.client.openapi.models.V1EnvVarSourceBuilder;
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

//@Transactional
@Component
public class KubeJob {
	
	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(KubeJob.class);
	
	/**
	 * The job id of DB
	 */
	private long jobId;
	/**
	 * The generated job name (job prefix + jobId)
	 */
	private String jobName;
	/**
	 * The pod name found
	 */
	private ArrayList<String> podNames;
	/**
	 * The container name generated (container prefix + jobId)
	 */
	private String containerName;
	/**
	 * The processor image name
	 */
	private String imageName;
	/**
	 * The job order file
	 */
	private String jobOrderFileName;
	/**
	 * Arguments of command
	 */
	private ArrayList<String> args = new ArrayList<String>();
	/**
	 * The order of job step
	 */
	private JobOrder jobOrder;
	/**
	 * The processing facility running job step
	 */
	private KubeConfig kubeConfig;
	
	private enum UpdateInfoResult {
		TRUE, FALSE, CHANGED;
	}
	
	/**
	 * @return the jobId
	 */
	public long getJobId() {
		return jobId;
	}

	/**
	 * @return the jobName
	 */
	public String getJobName() {
		return jobName;
	}

	/**
	 * @return the podName
	 */
	public ArrayList<String> getPodNames() {
		if (podNames == null || podNames.isEmpty()) {
			searchPod();
		}
		return podNames;
	}
	
	/**
	 * @return the containerName
	 */
	public String getContainerName() {
		return containerName;
	}

	/**
	 * @return the jobOrderFileName
	 */
	public String getJobOrderFileName() {
		return jobOrderFileName;
	}

	/**
	 * @param jobOrderFileName the jobOrderFileName to set
	 */
	public void setJobOrderFileName(String jobOrderFileName) {
		this.jobOrderFileName = jobOrderFileName;
	}

	/**
	 * @return the jobOrder
	 */
	public JobOrder getJobOrder() {
		return jobOrder;
	}

	/**
	 * @param jobOrder the jobOrder to set
	 */
	public void setJobOrder(JobOrder jobOrder) {
		this.jobOrder = jobOrder;
	}
	
	/**
	 * Add argument to argument list
	 * @param arg Argument to add
	 */
	public void addArg(String arg) {
		if (arg != null) {
			args.add(arg);
		}
	}
	
	/**
	 * Instantiate a kube job
	 */
	public KubeJob () {
		podNames = new ArrayList<String>();		
	}
	
	/**
	 * Instantiate a kube job with parameters
	 * @param id The DB id
	 * @param name The name prefix, if set
	 * @param processor The processor image 
	 * @param jobOrderFN The job order file name
	 * @param args Arguments for call
	 */
	public KubeJob (int id, String name, String processor, String jobOrderFN, ArrayList<String> args) {
		
		imageName = processor;
		jobOrderFileName = jobOrderFN;
		podNames = new ArrayList<String>();

		if (args != null) {
			this.args.addAll(args);
		}
		JobStep js = new JobStep();
		js = RepositoryService.getJobStepRepository().save(js);
		jobId = js.getId();
		if (name != null) {
			jobName = name + jobId;			
		}else {
			jobName = ProductionPlanner.jobNamePrefix + jobId;
		}
		containerName = ProductionPlanner.jobContainerPrefix + jobId;
		js.setProcessingMode(jobName); 
		RepositoryService.getJobStepRepository().save(js);
		
		
	}
	/**
	 * Instantiate a Kubernetes job with ID and Job Order file name
	 * @param jsId the job ID
	 * @param jobOrderFN the Job Order file name
	 */
	public KubeJob (Long jsId, String jobOrderFN) {
		
		jobOrderFileName = jobOrderFN;
		podNames = new ArrayList<String>();

		jobId = jsId;
		jobName = ProductionPlanner.jobNamePrefix + jobId;
		containerName = ProductionPlanner.jobContainerPrefix + jobId;
		
	}
	
	/**
	 * Rebuild kube job entries of processing facility after restart of planner
	 * 
	 * @param aKubeConfig The processing facility
	 * @param aJob The Kubernetes job
	 * @return The created kube job or null for not proseo jobs
	 */
	public KubeJob rebuild(KubeConfig aKubeConfig, V1Job aJob) {
		if (logger.isTraceEnabled()) logger.trace(">>> rebuild({}, {}, {})",
				(null == aKubeConfig ? "null" : aKubeConfig.getId()),
				(null == aJob ? "null" : aJob.getKind()));
		
		kubeConfig = aKubeConfig;
		if (aKubeConfig.isConnected() && aJob != null) {
			jobName = aJob.getMetadata().getName();
			if (jobName.startsWith(ProductionPlanner.jobNamePrefix)) {
				try {
					jobId = Long.valueOf(jobName.substring(ProductionPlanner.jobNamePrefix.length()));
					containerName = ProductionPlanner.jobContainerPrefix + jobId;
				} catch (NumberFormatException e) {
					e.printStackTrace();
					return null;
				}
			} else {
				return null;
			}
			searchPod();
		}
		return this;
	}

	/**
	 * Create the Kubernetes job on processing facility (based on constructor parameters)
	 * 
	 * Method is synchronized to avoid different threads (background dispatching and event-triggered dispatching) to
	 * interfere with each other.
	 * 
	 * @param aKubeConfig The processing facility
	 * @return The kube job
	 * @throws Exception 
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	synchronized public KubeJob createJob(KubeConfig aKubeConfig, String stdoutLogLevel, String stderrLogLevel) throws Exception {	
		if (logger.isTraceEnabled()) logger.trace(">>> createJob({}, {}, {})", aKubeConfig, stdoutLogLevel, stderrLogLevel);
		
		kubeConfig = aKubeConfig;
		JobOrder jobOrder = null;
		if (!aKubeConfig.isConnected()) {
			Messages.KUBERNETES_NOT_CONNECTED.log(logger, aKubeConfig.getProcessingFacility().toString());
			return null;
		}
		
		Optional<JobStep> js = RepositoryService.getJobStepRepository().findById(this.getJobId());
		if (js.isEmpty()) {
			Messages.JOB_STEP_NOT_FOUND.log(logger, this.getJobId());
			return null;
		}
		
		JobStep jobStep = js.get();
		ConfiguredProcessor configuredProcessor = jobStep.getOutputProduct().getConfiguredProcessor();
		if (null == configuredProcessor || !configuredProcessor.getEnabled()) {
			Messages.CONFIG_PROC_DISABLED.log(logger, jobStep.getOutputProduct().getConfiguredProcessor().getIdentifier());
			return null;
		}
		Instant execTime = jobStep.getJob().getProcessingOrder().getExecutionTime();
		if (execTime != null) { 
			if (Instant.now().isBefore(execTime)) {
				if (logger.isTraceEnabled()) logger.trace(">>> execution time of order is after now.");
				return null;
			}
		}
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
		Instant genTime = Instant.now();
		Duration retentionPeriod = jobStep.getJob().getProcessingOrder().getProductRetentionPeriod();
		if (retentionPeriod == null) {
			retentionPeriod = jobStep.getJob().getProcessingOrder().getMission().getProductRetentionPeriod();
		}
		setGenerationTime(jobStep.getOutputProduct(), genTime, retentionPeriod);
		JobDispatcher jd = new JobDispatcher();
		jobOrder = jd.createJobOrder(jobStep);
		if (jobOrder == null) {
			String errStr = String.format("Creation of job order for job step %d failed", jobStep.getId());
			logger.error(errStr);
			throw new Exception(errStr);
		}
		JobOrderVersion joVersion = configuredProcessor.getProcessor().getJobOrderVersion();
		jobOrder = jd.sendJobOrderToStorageManager(kubeConfig, jobOrder, joVersion);
		if (jobOrder == null) {
			String errStr = String.format("Sending of job order to Storage Manager failed for job step %d", jobStep.getId());
			logger.error(errStr);
			throw new Exception(errStr);
		}
		jobStep.setJobOrderFilename(jobOrder.getFileName());
		// wrapper user and PW
		String missionCode = jobStep.getJob().getProcessingOrder().getMission().getCode();
		String wrapUser = missionCode + "-" + ProductionPlanner.config.getWrapperUser();
		
		imageName = jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getDockerImage();
		
		String localMountPoint = ProductionPlanner.config.getPosixWorkerMountPoint();
		
		// Use Java style Map (as opposed to Scala's Map class)
		
		// Build a ResourceRequirements object
		V1ResourceRequirements reqs = new V1ResourceRequirements();
		String cpus = jobStep.getOutputProduct().getConfiguredProcessor().getConfiguration().getDockerRunParameters().getOrDefault("cpu", "1");
		String mem = jobStep.getOutputProduct().getConfiguredProcessor().getConfiguration().getDockerRunParameters().getOrDefault("memory", "1");
		String minDiskSpace = jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getMinDiskSpace().toString() + "Mi";
		try {
			cpus = getCPUs(jobStep.getOutputProduct().getConfiguredProcessor().getProcessor(), Integer.parseInt(cpus)).toString();
		} catch (NumberFormatException ex) {
			cpus = getCPUs(jobStep.getOutputProduct().getConfiguredProcessor().getProcessor(), 1).toString();
		}
		try {
			mem = getMinMemory(jobStep.getOutputProduct().getConfiguredProcessor().getProcessor(), Integer.parseInt(mem)).toString() + "Gi";
		} catch (NumberFormatException ex) {
			mem = getMinMemory(jobStep.getOutputProduct().getConfiguredProcessor().getProcessor(), 1).toString() + "Gi";
		}
		reqs.putRequestsItem("cpu", new Quantity(cpus))
			.putRequestsItem("memory", new Quantity(mem))
			.putRequestsItem("ephemeral-storage", new Quantity(minDiskSpace));
		V1EnvVarSource es = new V1EnvVarSourceBuilder().withNewFieldRef().withFieldPath("status.hostIP").endFieldRef().build();
		String localStorageManagerUrl = kubeConfig.getLocalStorageManagerUrl();
				
		V1JobSpec jobSpec = new V1JobSpecBuilder()
				.withNewTemplate()
				.withNewMetadata()
				.withName(jobName + "spec")
				.addToLabels("jobgroup", jobName + "spec")
				.endMetadata()
				.withNewSpec()
				.addToImagePullSecrets(new V1LocalObjectReference().name("proseo-regcred"))
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
				.withValue(jobOrder.getFileName())
				.endEnv()
				.addNewEnv()
				.withName("JOBORDER_VERSION")
				.withValue(joVersion.toString())
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
				.withValue(ProductionPlanner.config.getProductionPlannerUrl() +"/processingfacilities/" + kubeConfig.getId() + "/finish/" + jobName)
				.endEnv()
				.addNewEnv()
				.withName("PROCESSING_FACILITY_NAME")
				.withValue(kubeConfig.getId())
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
				.withResources(reqs)
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
		V1Job job = new V1JobBuilder()
				.withNewMetadata()
				.withName(jobName)
				.addToLabels("jobgroup", jobName + "spec")
				.endMetadata()
				.withSpec(jobSpec)
				.build();
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Creating job {}", job.toString());
			}
			job = aKubeConfig.getBatchApiV1().createNamespacedJob (aKubeConfig.getNamespace(), job, null, null, null);
			logger.info("Job {} created with status {}", job.getMetadata().getName(), job.getStatus().toString());
			UtilService.getJobStepUtil().startJobStep(jobStep);
			Messages.KUBEJOB_CREATED.log(logger, kubeConfig.getId(), jobName);
			Integer cycle = ProductionPlanner.config.getProductionPlannerJobCreatedWaitTime();
			if (cycle == null) {
				cycle = 2000;
			}
			Thread.sleep(cycle);
			searchPod();
			updateJobLog(jobStep);
			RepositoryService.getJobStepRepository().save(jobStep);
		} catch (ApiException e1) {
			logger.error("Kubernetes API exception creating job for job step {}: {}", jobStep.getId(), e1.getMessage());
			throw e1;
		} catch (Exception e) {
			logger.error("General exception creating job for job step {}: {}", jobStep.getId(), e.getMessage());
			throw e;
		}
		if (logger.isTraceEnabled()) logger.trace("<<< createJob finished successful");
		return this;
	}	
	
	/**
	 * @return the kubeConfig
	 */
	public KubeConfig getKubeConfig() {
		return kubeConfig;
	}

	/**
	 * Search pod for job and set podName
	 */
	public void searchPod() {
		if (logger.isTraceEnabled()) logger.trace(">>> searchPod()");
		
		if (kubeConfig != null && kubeConfig.isConnected()) {
			V1PodList pl;
			try {
				pl = kubeConfig.getApiV1().listNamespacedPod(kubeConfig.getNamespace(), null, null, null, 
						null, null, null, null, null, 30, null);
				podNames.clear();
				for (V1Pod p : pl.getItems()) {
					String pn = p.getMetadata().getName();
					if (pn.startsWith(jobName)) {
						podNames.add(pn);
						if (logger.isTraceEnabled()) logger.trace("     Pod found: {}", pn);
					}
				}
			} catch (ApiException e) {
				logger.error(Messages.RUNTIME_EXCEPTION.format(e.getMessage()), e);
			}
		}
		if (logger.isTraceEnabled()) logger.trace("<<< searchPod()");
	}
	
	/**
	 * Retrieve and save all necessary info before deletion of job
	 * 
	 * @param aKubeConfig The processing facility
	 * @param jobname The job name
	 */
	public void finish(KubeConfig aKubeConfig, String jobname) {
		if (logger.isTraceEnabled()) logger.trace(">>> finish({}, {})", (null == aKubeConfig ? "null" : aKubeConfig.getId()), jobname);
		
		if (aKubeConfig != null || kubeConfig != null) {
			if (kubeConfig == null) {
				kubeConfig = aKubeConfig;
			}
			searchPod();

			V1Job aJob = aKubeConfig.getV1Job(jobname);
			if (aJob != null) {
				Long jobStepId = this.getJobId();
				Optional<JobStep> js = RepositoryService.getJobStepRepository().findById(jobStepId);
				if (js.isPresent()) {
					updateJobLog(js.get());
					try {
						if (aJob.getStatus() != null) {
							OffsetDateTime d;
							d = aJob.getStatus().getStartTime();
							if (d != null) {
								js.get().setProcessingStartTime(d.toInstant());
							}

							d = aJob.getStatus().getCompletionTime();
							if (d != null) {
								js.get().setProcessingCompletionTime(d.toInstant());
							}
							if (aJob.getStatus().getConditions() != null) {
								List<V1JobCondition> jobCondList = aJob.getStatus().getConditions();
								for (V1JobCondition jc : jobCondList) {
									if ((jc.getType().equalsIgnoreCase("complete") || jc.getType().equalsIgnoreCase("completed")) && jc.getStatus().equalsIgnoreCase("true")) {
										js.get().setJobStepState(JobStepState.COMPLETED);
										UtilService.getJobStepUtil().checkCreatedProducts(js.get());
										js.get().incrementVersion();	
									} else if (jc.getType().equalsIgnoreCase("failed") || jc.getType().equalsIgnoreCase("failure")) {
										js.get().setJobStepState(JobStepState.FAILED);	
										js.get().incrementVersion();
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();						
					}
					RepositoryService.getJobStepRepository().save(js.get());
					UtilService.getJobUtil().updateState(js.get().getJob(), js.get().getJobStepState());
					UtilService.getOrderUtil().logOrderState(js.get().getJob().getProcessingOrder());
				}
			}
			KubeJobFinish toFini = new KubeJobFinish(this, jobname);
			toFini.start();
		}
	}	
	
	/* TODO analyse pod 
	 * 
	 * phase
    	string	The phase of a Pod is a simple, high-level summary of where the Pod is in its lifecycle. 
    	The conditions array, the reason and message fields, and the individual container status arrays 
    	contain more detail about the pod's status. There are five possible phase values: 
    	Pending: The pod has been accepted by the Kubernetes system, but one or more of the container 
    		images has not been created. This includes time before being scheduled as well as time spent 
    		downloading images over the network, which could take a while. 
    	Running: The pod has been bound to a node, and all of the containers have been created. 
    		At least one container is still running, or is in the process of starting or restarting. 
    	Succeeded: All containers in the pod have terminated in success, and will not be restarted. 
    	Failed: All containers in the pod have terminated, and at least one container has terminated in failure. 
    		The container either exited with non-zero status or was terminated by the system. 
    	Unknown: For some reason the state of the pod could not be obtained, typically due to an error 
    		in communicating with the host of the pod. 
    	More info: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle#pod-phase
	 * 
	 * conditions:
	     https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-conditions
	 * 
	 */
	
	/**
	 * Update all the information of a Kubernetes job which is stored in job step
	 * 
	 * @param aJobName The Kubernetes job name
	 * @return true after success
	 */
	@Transactional
	public UpdateInfoResult updateInfo(String aJobName) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateInfo({})", aJobName);
		
		UpdateInfoResult success = UpdateInfoResult.FALSE;
		if (kubeConfig != null && kubeConfig.isConnected() && aJobName != null) {
			V1Job aJob = kubeConfig.getV1Job(aJobName);
			if (aJob == null) {
				// job not found, try to remove
				if (logger.isTraceEnabled()) logger.trace("    updateInfo: job not found");
				return UpdateInfoResult.TRUE;
			}
			if (podNames.isEmpty()) {
				searchPod();
			}
			if (podNames.isEmpty()) {
				if (logger.isTraceEnabled()) logger.trace("    updateInfo: pod not found");
				return UpdateInfoResult.FALSE;
			}
			V1Pod aPod = kubeConfig.getV1Pod(podNames.get(podNames.size()-1));

			if (aPod != null) {
				String podMessages = "";
				Long jobStepId = this.getJobId();
				Optional<JobStep> js = RepositoryService.getJobStepRepository().findById(jobStepId);
				if (js.isPresent()) {
					int oldVersion = js.get().getVersion();
					try {
						if (aJob.getStatus() != null) {
							if (logger.isTraceEnabled()) logger.trace("    updateInfo: analyze job state");
							OffsetDateTime d;
							d = aJob.getStatus().getStartTime();
							if (d != null) {
								js.get().setProcessingStartTime(d.toInstant());
							}

							OffsetDateTime cd = aJob.getStatus().getCompletionTime();
							if (cd != null) {
								js.get().setProcessingCompletionTime(cd.toInstant());
							} else {
								// something wrong with job, try to get info from pod
								if (aPod != null) {
									// pod exists! 
								}
							}
							if (aJob.getStatus().getConditions() != null) {
								List<V1JobCondition> jobCondList = aJob.getStatus().getConditions();

								if (jobCondList != null) {
									for (V1JobCondition jc : jobCondList) {
										if ((jc.getType().equalsIgnoreCase("complete") || jc.getType().equalsIgnoreCase("completed")) && jc.getStatus().equalsIgnoreCase("true")) {
											if (js.get().getJobStepState() == de.dlr.proseo.model.JobStep.JobStepState.FAILED) {
												js.get().setJobStepState(de.dlr.proseo.model.JobStep.JobStepState.INITIAL);
											}
											if (JobStepState.READY.equals(js.get().getJobStepState())) {
												// Sometimes we don't get the state transition to RUNNING
												js.get().setJobStepState(JobStepState.RUNNING); // otherwise we cannot set it to COMPLETED
											} else if (JobStepState.INITIAL.equals(js.get().getJobStepState())) {
												js.get().setJobStepState(JobStepState.READY);
												js.get().setJobStepState(JobStepState.RUNNING);
											}
										    js.get().setJobStepState(JobStepState.COMPLETED);
											UtilService.getJobStepUtil().checkCreatedProducts(js.get());
											
											if (cd == null) {
												cd = jc.getLastProbeTime();
												js.get().setProcessingCompletionTime(cd.toInstant());
											}
											success = UpdateInfoResult.TRUE;
										} else if ((jc.getType().equalsIgnoreCase("failed") || jc.getType().equalsIgnoreCase("failure")) && jc.getStatus().equalsIgnoreCase("true")) {
											if (JobStepState.READY.equals(js.get().getJobStepState())) {
												// Sometimes we don't get the state transition to RUNNING
												js.get().setJobStepState(JobStepState.RUNNING); // otherwise we cannot set it to COMPLETED
											} else if (JobStepState.INITIAL.equals(js.get().getJobStepState())) {
												js.get().setJobStepState(JobStepState.READY);
												js.get().setJobStepState(JobStepState.RUNNING);
											}
											js.get().setJobStepState(JobStepState.FAILED);	
											if (cd == null) {
												cd = jc.getLastProbeTime();
												js.get().setProcessingCompletionTime(cd.toInstant());
											}
											success = UpdateInfoResult.TRUE;
										}
									}
								}
							}
							// cancel pod and job, write reasons into job step log


							// TODO check whether pod is in normal state or has an Error/Warning event
							// example
							// kubeConfig.getApiV1().listNamespacedEvent("default",null,false,null,"involvedObject.name=proseojob733-bwzf4",null,null,null,null,false);


						} else {
							if (logger.isTraceEnabled()) logger.trace("    updateInfo: status not found");
						}
					} catch (Exception e) {
						e.printStackTrace();						
					}
					updateJobLog(js.get());
					RepositoryService.getJobStepRepository().save(js.get());
					Optional<JobStep> jsn = RepositoryService.getJobStepRepository().findById(jobStepId);
					if (jsn.isPresent()) {
						if (oldVersion == jsn.get().getVersion()) {
							if (success == UpdateInfoResult.TRUE) {
								js.get().incrementVersion();
								RepositoryService.getJobStepRepository().save(js.get());
								UtilService.getJobUtil().updateState(js.get().getJob(), js.get().getJobStepState());
							}
						} else {
							if (logger.isTraceEnabled()) logger.trace("    updateInfo: job step changed by others");
							success = UpdateInfoResult.CHANGED;
						}
					}						
				}
			}
		}
		if (logger.isTraceEnabled()) logger.trace("<<< updateInfo({})", aJobName);
		return success;
	}

	
	/**
	 * A Kubernetes job has finished, get and store the info, delete the Kubernetes job.
	 * 
	 * @param aJobName The Kubernetes job name 
	 * @return true after success
	 */
	@Transactional
	public boolean updateFinishInfoAndDelete(String aJobName) {
		if (logger.isTraceEnabled()) logger.trace(">>> updateFinishInfoAndDelete({})", aJobName);
		
		UpdateInfoResult success = UpdateInfoResult.FALSE;
		success = updateInfo(aJobName);
		if (success.equals(UpdateInfoResult.TRUE)) {
			Long jobStepId = this.getJobId();
			Optional<JobStep> js = RepositoryService.getJobStepRepository().findById(jobStepId);
			if (js.isPresent()) {							
				UtilService.getJobStepUtil().checkFinish(js.get());
			}
		}
		if (!success.equals(UpdateInfoResult.FALSE)) {
			// delete kube job
			kubeConfig.deleteJob(aJobName);
			Messages.KUBEJOB_FINISHED.log(logger, kubeConfig.getId(), aJobName);
		}
		return !success.equals(UpdateInfoResult.FALSE);
	}
	
	/**
	 * Set the generation time of created products to genTime
	 * 
	 * @param product The product
	 * @param genTime The generation time
	 */
	void setGenerationTime(Product product, Instant genTime, Duration retentionPeriod) {
		if (logger.isTraceEnabled()) logger.trace(">>> setGenerationTime({}, {}, {})",
				(null == product ? "null" : product.getId()), genTime, retentionPeriod);
		
		if (product != null && genTime != null) {			
			product.setGenerationTime(genTime);
			if (retentionPeriod != null) {
				product.setEvictionTime(genTime.plus(retentionPeriod));
			}
			for (Product p : product.getComponentProducts()) {
				setGenerationTime(p, genTime, retentionPeriod);
			}
			RepositoryService.getProductRepository().save(product);
		}
	}
	
	/**
	 * Get the maximum setting of cpus of processor tasks
	 * @param proc The Processor
	 * @param cpus The default cpus value
	 * @return The requested cpus maximum
	 */
	private Integer getCPUs(Processor proc, Integer cpus) {
		if (proc != null) {
			for (Task t : proc.getTasks()) {
				if (t.getNumberOfCpus() != null) {
					if (t.getNumberOfCpus() > cpus) {
						cpus = t.getNumberOfCpus();
					}
				}
			}
		}
		return cpus;
	}
	
	/**
	 * Get the requested memory size of processor tasks
	 * @param proc The Processor
	 * @param minMem The minimum memory size
	 * @return The requested memory size
	 */
	private Integer getMinMemory(Processor proc, Integer minMem) {
		if (proc != null) {
			for (Task t : proc.getTasks()) {
				if (t.getMinMemory() != null) {
					if (t.getMinMemory() > minMem) {
						minMem = t.getMinMemory();
					}
				}
			}
		}
		return minMem;
	}
	
	private void updateJobLog(JobStep js) {
		if (js != null && !podNames.isEmpty()) {
			// Check conditions
			String podMessages = "";
			V1Pod aPod = kubeConfig.getV1Pod(podNames.get(podNames.size()-1));
			if (aPod != null && aPod.getStatus().getConditions() != null) {
				podMessages += "Job Step Conditions (Type - Status):\n";
				List<V1PodCondition> pobCondList = aPod.getStatus().getConditions();
				for (V1PodCondition pc : pobCondList) {
					podMessages += "  " + pc.getType() + " - " + pc.getStatus() + "\n";
				}
				String fieldSelector = "involvedObject.name==" + aPod.getMetadata().getName();
				CoreV1EventList el = null;
				try {
					el = kubeConfig.getApiV1().listEventForAllNamespaces(false, null, fieldSelector, null, 30, null, null, null, null, null);
					if (el != null) {
						podMessages += "Job Step Events (Type - Reason - Count - Message):\n";
						for (CoreV1Event ev : el.getItems()) {
							podMessages += "  " + ev.getType() + " - " + ev.getReason() + " - " + ev.getCount() + " - " + ev.getMessage()  + "\n";
						}
						podMessages += "\n\n";
					}
				} catch (ApiException e) {
					logger.error(Messages.RUNTIME_EXCEPTION.format(e.getMessage()), e);
				}
			}

			String cn = this.getContainerName();
			String log = "";
			if (cn != null) {
				try {
					log = kubeConfig.getApiV1().readNamespacedPodLog(podNames.get(podNames.size()-1), kubeConfig.getNamespace(), cn, null, null, null, null, null, null, null, null);
				} catch (ApiException e1) {
					// ignore, normally the pod has no log
					if (logger.isTraceEnabled()) logger.trace("    updateInfo: ApiException ignore, normally the pod has no log");
				} catch (Exception e) {
					logger.error(Messages.RUNTIME_EXCEPTION.format(e.getMessage()), e);
				}
			} else {
				if (logger.isTraceEnabled()) logger.trace("    updateJobLog: container not found");
			}

			if (log != null && !log.isBlank()) {
				js.setProcessingStdOut(podMessages + log);
			} else if (js.getProcessingStdOut() == null || js.getProcessingStdOut().isBlank()) {
				js.setProcessingStdOut(podMessages);
			}
		}
	}
}
