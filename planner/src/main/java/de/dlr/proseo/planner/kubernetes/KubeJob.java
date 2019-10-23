/**
 * 
 */
package de.dlr.proseo.planner.kubernetes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.JobDispatcher;
import de.dlr.proseo.planner.rest.model.PodKube;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobBuilder;
import io.kubernetes.client.models.V1JobCondition;
import io.kubernetes.client.models.V1JobSpec;
import io.kubernetes.client.models.V1JobSpecBuilder;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;

/**
 * A KubeJob describes the complete information to run a Kubernetes job.
 * 
 * @author melchinger
 *
 */

//@Transactional
@Component
public class KubeJob {

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
	private String podName;
	/**
	 * The container name generated (container prefix + jobId)
	 */
	private String containerName;
	/**
	 * The processor image name
	 */
	private String imageName;
	/**
	 * The command to call in image
	 */
	private String command;
	/**
	 * The job order file
	 */
	private String jobOrderFileName;
	/**
	 * Job Order content
	 */
	private String jobOrderString;
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
	public String getPodName() {
		if (podName == null) {
			searchPod();
		}
		return podName;
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
	 * Instanciate a kube job
	 */
	public KubeJob () {
		
	}
	
	/**
	 * Instanciate a kube job with parameters
	 * @param id The DB id
	 * @param name The name prefix, if set
	 * @param processor The processor image 
	 * @param jobOrderFN The job order file name
	 * @param cmd The command call for image
	 * @param args Arguments for call
	 */
	public KubeJob (int id, String name, String processor, String jobOrderFN, String cmd, ArrayList<String> args) {
		
		imageName = processor;
		command = cmd;
		jobOrderFileName = jobOrderFN;
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
	 * Rebuild kube job entries of processing facility after restart of planner
	 * 
	 * @param aKubeConfig Ther processing facility
	 * @param aJob The kubernetes job
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
	 * Create the kubernetes job on processing facility (based on constructor parameters)
	 * @param aKubeConfig The processing facility
	 * @return The kube job
	 */
	public KubeJob createJob(KubeConfig aKubeConfig) {	
		kubeConfig = aKubeConfig;
		JobOrder jobOrder = null;
		if (aKubeConfig.isConnected()) {
			Optional<JobStep> js = RepositoryService.getJobStepRepository().findById(this.getJobId());
			if (!js.isEmpty()) {
				JobDispatcher jd = new JobDispatcher();
				jobOrder = jd.createJobOrder(js.get());
				if (jobOrder == null) {
					// todo Exception
					return null;
				}
				jobOrder = jd.sendJobOrderToStorageManager(kubeConfig, jobOrder);
				if (jobOrder == null) {
					// todo Exception
					return null;
				}
			}
			V1JobSpec jobSpec = new V1JobSpecBuilder()
				.withNewTemplate()
				.withNewMetadata()
				.withName(jobName + "spec")
				.addToLabels("jobgroup", jobName + "spec")
				.endMetadata()
				.withNewSpec()
				.addNewContainer()
				.withName(containerName)
				.withImage(imageName)
				.withImagePullPolicy("Never")
//				.withCommand(command)
//			    .withArgs(jobOrderFileName)
				.addNewEnv()
				.withName("JOBORDER_FILE")
				.withValue(jobOrder.getFileName())
				.endEnv()
				.addNewEnv()
				.withName("JOBORDER_FS_TYPE")
				.withValue(jobOrder.getFsType())
				.endEnv()
				.addNewEnv()
				.withName("INGESTOR_ENDPOINT")
				.withValue("")
				.endEnv()
				.addNewEnv()
				.withName("STATE_CALLBACK_ENDPOINT")
//				.withValue("http://" + ProductionPlanner.hostName + ":" + ProductionPlanner.port)
				.withValue("http://" + "192.168.20.155" + ":" + ProductionPlanner.port 
						+ "/proseo/planner/v0.1/processingfacilities/" + kubeConfig.getId() + "/finish/" + jobName)
				.endEnv()
				.addNewEnv()
				.withName("S3_ENDPOINT")
				.withValue("http://192.168.20.159:9000")
				.endEnv()
				.addNewEnv()
				.withName("S3_ACCESS_KEY")
				.withValue("short_access_key")
				.endEnv()
				.addNewEnv()
				.withName("S3_SECRET_ACCESS_KEY")
				.withValue("short_secret_key")
				.endEnv()
				.addNewEnv()
				.withName("S3_STORAGE_ID_OUTPUTS")
				.withValue("s3test")
				.endEnv()
				.addNewEnv()
				.withName("ALLUXIO_STORAGE_ID_OUTPUTS")
				.withValue("alluxio1")
				.endEnv()
				.addNewEnv()
				.withName("INGESTOR_ENDPOINT")
				.withValue("http://192.168.20.159:8082")
				.endEnv()
				.addNewEnv()
				.withName("PROCESSING_FACILITY_NAME")
				.withValue(kubeConfig.getId())
				.endEnv()
				.addNewVolumeMount()
				.withName("input")
				.withMountPath("/testdata")
				.endVolumeMount()
				.endContainer()
				.withRestartPolicy("Never")
				.withHostNetwork(true)
				.withDnsPolicy("ClusterFirstWithHostNet")
				.addNewVolume()
				.withName("input")
				.withNewHostPath()
				.withPath("/root")
				.endHostPath()
				.endVolume()
				.endSpec()
				.endTemplate()
				.withBackoffLimit(1)
				.build();			
			V1Job job = new V1JobBuilder()
				.withNewMetadata()
				.withName(jobName)
				.addToLabels("jobgroup", jobName + "spec")
				.endMetadata()
				.withSpec(jobSpec)
				.build();
			try {
				
				if (!js.isEmpty()) {
					aKubeConfig.getBatchApiV1().createNamespacedJob (aKubeConfig.getNamespace(), job, null, null, null);
					searchPod();

					js.get().setJobStepState(JobStepState.READY);	
					RepositoryService.getJobStepRepository().save(js.get());
				}
			} catch (ApiException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return null;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			/*
			 * try { pod = apiV1.createNamespacedPod(aKubeConfig.getNamespace(), pod, null, null, null); }
			 * catch (ApiException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
			return this;
		} else {
			return null;
		}
	}	
	
	/**
	 * Search pod for job and set podName
	 */
	public void searchPod() {
		if (kubeConfig != null && kubeConfig.isConnected()) {
			V1PodList pl;
			try {
				pl = kubeConfig.getApiV1().listNamespacedPod(kubeConfig.getNamespace(), true, null, null, null, 
						null, null, null, 30, null);
				for (V1Pod p : pl.getItems()) {
					String pn = p.getMetadata().getName();
					if (pn.startsWith(jobName)) {
						podName = pn;
						break;
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
			String pn = this.getPodName();
			if (pn == null || pn.isEmpty()) {
				searchPod();
				pn = this.getPodName();
			}

			V1Job aJob = aKubeConfig.getV1Job(jobname);
			if (aJob != null) {
				PodKube aPlan = new PodKube(aJob);
				String cn = this.getContainerName();
				if (cn != null && pn != null) {
					try {
						String log = kubeConfig.getApiV1().readNamespacedPodLog(pn, kubeConfig.getNamespace(), cn, null, null, null, null, null, null, null);
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
									} else if (jc.getType().equalsIgnoreCase("failed") || jc.getType().equalsIgnoreCase("failure")) {
										js.get().setJobStepState(JobStepState.FAILED);	
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
					Optional<JobStep> jsa = RepositoryService.getJobStepRepository().findById(jobStepId);
					if (jsa.isPresent()) {
						jsa.get();
					}
				}
			}
		}
	}	
}
