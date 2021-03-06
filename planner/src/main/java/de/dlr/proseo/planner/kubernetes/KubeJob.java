/**
 * KubeJob.java
 * 
 * © 2019 Prophos Informatik GmbH
 */

package de.dlr.proseo.planner.kubernetes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.JobStep.StdLogLevel;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.JobDispatcher;
import de.dlr.proseo.planner.rest.model.PodKube;
import de.dlr.proseo.planner.util.UtilService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1EnvVarSource;
import io.kubernetes.client.openapi.models.V1EnvVarSourceBuilder;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobBuilder;
import io.kubernetes.client.openapi.models.V1JobCondition;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1JobSpecBuilder;
import io.kubernetes.client.openapi.models.V1LocalObjectReference;
import io.kubernetes.client.openapi.models.V1Pod;
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
	 * @param aKubeConfig The processing facility
	 * @return The kube job
	 * @throws Exception 
	 */
	@Transactional
	public KubeJob createJob(KubeConfig aKubeConfig, String stdoutLogLevel, String stderrLogLevel) throws Exception {	
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
		setGenerationTime(jobStep.getOutputProduct(), genTime);
		JobDispatcher jd = new JobDispatcher();
		jobOrder = jd.createJobOrder(jobStep);
		if (jobOrder == null) {
			String errStr = String.format("Creation of job order for job step %d failed", jobStep.getId());
			logger.error(errStr);
			throw new Exception(errStr);
		}
		jobOrder = jd.sendJobOrderToStorageManager(kubeConfig, jobOrder, configuredProcessor.getProcessor().getJobOrderVersion());
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
		String mem = jobStep.getOutputProduct().getConfiguredProcessor().getConfiguration().getDockerRunParameters().getOrDefault("memory", "1500Mi");
		reqs.putRequestsItem("cpu", new Quantity(cpus))
			.putRequestsItem("memory", new Quantity(mem));
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
				logger.info("Creating job {}", job.toString());
			}
			job = aKubeConfig.getBatchApiV1().createNamespacedJob (aKubeConfig.getNamespace(), job, null, null, null);
			logger.info("Job {} created with status {}", job.getMetadata().getName(), job.getStatus().toString());
			searchPod();
			UtilService.getJobStepUtil().startJobStep(jobStep);
			Messages.KUBEJOB_CREATED.log(logger, kubeConfig.getId(), jobName);
		} catch (ApiException e1) {
			logger.error("Kubernetes API exception creating job for job step {}: {}", jobStep.getId(), e1.getMessage());
			throw e1;
		} catch (Exception e) {
			logger.error("General exception creating job for job step {}: {}", jobStep.getId(), e.getMessage());
			throw e;
		}
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
		if (kubeConfig != null && kubeConfig.isConnected()) {
			V1PodList pl;
			try {
				pl = kubeConfig.getApiV1().listNamespacedPod(kubeConfig.getNamespace(), null, null, null, 
						null, null, null, null, 30, null);
				podNames.clear();
				for (V1Pod p : pl.getItems()) {
					String pn = p.getMetadata().getName();
					if (pn.startsWith(jobName)) {
						podNames.add(pn);
					}
				}
			} catch (ApiException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Retrieve and save all necessary info before deletion of job
	 * 
	 * @param aKubeConfig The processing facility
	 * @param jobname The job name
	 */
	public void finish(KubeConfig aKubeConfig, String jobname) {
		if (aKubeConfig != null || kubeConfig != null) {
			if (kubeConfig == null) {
				kubeConfig = aKubeConfig;
			}
			searchPod();

			V1Job aJob = aKubeConfig.getV1Job(jobname);
			if (aJob != null) {
				PodKube aPlan = new PodKube(aJob);
				String cn = this.getContainerName();
				if (cn != null && !podNames.isEmpty()) {
					try {
						String log = kubeConfig.getApiV1().readNamespacedPodLog(podNames.get(podNames.size()-1), kubeConfig.getNamespace(), cn, null, null, null, null, null, null, null);
						aPlan.setLog(log);
					} catch (ApiException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Long jobStepId = this.getJobId();
				Optional<JobStep> js = RepositoryService.getJobStepRepository().findById(jobStepId);
				if (js.isPresent()) {
					try {
						if (aJob.getStatus() != null) {
							DateTime d;
							d = aJob.getStatus().getStartTime();
							if (d != null) {
								js.get().setProcessingStartTime(d.toDate().toInstant());
							}

							d = aJob.getStatus().getCompletionTime();
							if (d != null) {
								js.get().setProcessingCompletionTime(d.toDate().toInstant());
							}
							if (aJob.getStatus().getConditions() != null) {
								List<V1JobCondition> jobCondList = aJob.getStatus().getConditions();
								for (V1JobCondition jc : jobCondList) {
									if ((jc.getType().equalsIgnoreCase("complete") || jc.getType().equalsIgnoreCase("completed")) && jc.getStatus().equalsIgnoreCase("true")) {
										js.get().setJobStepState(JobStepState.COMPLETED);
										js.get().incrementVersion();	
									} else if (jc.getType().equalsIgnoreCase("failed") || jc.getType().equalsIgnoreCase("failure")) {
										js.get().setJobStepState(JobStepState.FAILED);	
										js.get().incrementVersion();
									}
								}
							}
						}
						if (aPlan.getLog() != null) {
							js.get().setProcessingStdOut(aPlan.getLog());
						}
					} catch (Exception e) {
						e.printStackTrace();						
					}
					RepositoryService.getJobStepRepository().save(js.get());
				}
			}
			KubeJobFinish toFini = new KubeJobFinish(this, jobname);
			toFini.start();
		}
	}	
	
	/**
	 * Get all the information of a Kubernetes job which is stored in job step
	 * 
	 * @param aJobName The Kubernetes job name
	 * @return true after success
	 */
	@Transactional
	public boolean getInfo(String aJobName) {
		boolean success = false;
		if (kubeConfig != null && kubeConfig.isConnected() && aJobName != null) {
			V1Job aJob = kubeConfig.getV1Job(aJobName);
			if (aJob == null) {
				// job not found, try to remove
				return true;
			}
			if (podNames.isEmpty()) {
				searchPod();
			}
			V1Pod aPod = kubeConfig.getV1Pod(podNames.get(podNames.size()-1));

			if (aPod != null) {
				PodKube aPlan = new PodKube(aJob);
				String cn = this.getContainerName();
				if (cn != null && !podNames.isEmpty()) {
					try {
						String log = kubeConfig.getApiV1().readNamespacedPodLog(podNames.get(podNames.size()-1), kubeConfig.getNamespace(), cn, null, null, null, null, null, null, null);
						aPlan.setLog(log);
					} catch (ApiException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Long jobStepId = this.getJobId();
				Optional<JobStep> js = RepositoryService.getJobStepRepository().findById(jobStepId);
				if (js.isPresent()) {
					try {
						if (aJob.getStatus() != null) {
							DateTime d;
							d = aJob.getStatus().getStartTime();
							if (d != null) {
								js.get().setProcessingStartTime(d.toDate().toInstant());
							}

							DateTime cd = aJob.getStatus().getCompletionTime();
							if (cd != null) {
								js.get().setProcessingCompletionTime(cd.toDate().toInstant());
							} else {
								// something wrong with job, try to get info from pod
								if (aPod != null) {
									// pod exists! 
								}
							}
							
							if (aJob.getStatus().getConditions() != null) {
								List<V1JobCondition> jobCondList = aJob.getStatus().getConditions();
								for (V1JobCondition jc : jobCondList) {
									if ((jc.getType().equalsIgnoreCase("complete") || jc.getType().equalsIgnoreCase("completed")) && jc.getStatus().equalsIgnoreCase("true")) {
										js.get().setJobStepState(JobStepState.COMPLETED);	
										js.get().incrementVersion();
										if (cd == null) {
											cd = jc.getLastProbeTime();
											js.get().setProcessingCompletionTime(cd.toDate().toInstant());
										}
										success = true;
									} else if ((jc.getType().equalsIgnoreCase("failed") || jc.getType().equalsIgnoreCase("failure")) && jc.getStatus().equalsIgnoreCase("true")) {
										js.get().setJobStepState(JobStepState.FAILED);	
										js.get().incrementVersion();	
										if (cd == null) {
											cd = jc.getLastProbeTime();
											js.get().setProcessingCompletionTime(cd.toDate().toInstant());
										}
										success = true;
									}
								}
							}
							
							// TODO check whether pod is in normal state or has an Error/Warning event
							// example
							// kubeConfig.getApiV1().listNamespacedEvent("default",null,false,null,"involvedObject.name=proseojob733-bwzf4",null,null,null,null,false);
							
						}
						if (aPlan.getLog() != null) {
							js.get().setProcessingStdOut(aPlan.getLog());
						}
					} catch (Exception e) {
						e.printStackTrace();						
					}
					RepositoryService.getJobStepRepository().save(js.get());	
				}
			}
		}
		return success;
	}

	
	/**
	 * A Kubernetes job has finished, get and store the info, delete the Kubernetes job.
	 * 
	 * @param aJobName The Kubernetes job name 
	 * @return true after success
	 */
	@Transactional
	public boolean getFinishInfo(String aJobName) {
		boolean success = false;
		success = getInfo(aJobName);
		if (success) {
			Long jobStepId = this.getJobId();
			Optional<JobStep> js = RepositoryService.getJobStepRepository().findById(jobStepId);
			if (js.isPresent()) {							
				UtilService.getJobStepUtil().checkFinish(js.get());
			}
		}
		if (success) {
			// delete kube job
			kubeConfig.deleteJob(aJobName);
			Messages.KUBEJOB_FINISHED.log(logger, kubeConfig.getId(), aJobName);
		}
		return success;
	}
	
	/**
	 * Set the generation time of created products to genTime
	 * 
	 * @param product The product
	 * @param genTime The generation time
	 */
	void setGenerationTime(Product product, Instant genTime) {
		if (product != null && genTime != null) {
			product.setGenerationTime(genTime);
			for (Product p : product.getComponentProducts()) {
				setGenerationTime(p, genTime);
			}
			RepositoryService.getProductRepository().save(product);
		}
	}
}
