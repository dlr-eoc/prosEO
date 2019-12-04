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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.JobStep.StdLogLevel;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.ProductionPlannerConfiguration;
import de.dlr.proseo.planner.dispatcher.JobDispatcher;
import de.dlr.proseo.planner.rest.JobControllerImpl;
import de.dlr.proseo.planner.rest.model.PodKube;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Copy;
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
	public ArrayList<String> getPodNames() {
		if (podNames == null) {
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
	 * Instanciate a kube job
	 */
	public KubeJob () {
		podNames = new ArrayList<String>();		
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
		podNames = new ArrayList<String>();
//		try {
//            jobOrderString = ""; // Files.readString(Paths.get("C:\\usr\\prosEO\\workspace-proseo\\prosEO\\sample-wrapper\\src\\test\\resources\\JobOrder.608109247_KNMI-L2_CO.xml"));
//        } catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
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
	public KubeJob createJob(KubeConfig aKubeConfig, String stdoutLogLevel, String stderrLogLevel) {	
		kubeConfig = aKubeConfig;
		JobOrder jobOrder = null;
		if (aKubeConfig.isConnected()) {
			Optional<JobStep> js = RepositoryService.getJobStepRepository().findById(this.getJobId());
			if (!js.isEmpty()) {
				JobStep jobStep = js.get();
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
				JobDispatcher jd = new JobDispatcher();
				jobOrder = jd.createJobOrder(jobStep);
				if (jobOrder == null) {
					// todo Exception
					return null;
				}
				jobOrder = jd.sendJobOrderToStorageManager(kubeConfig, jobOrder);
				if (jobOrder == null) {
					// todo Exception
					return null;
				}
				imageName = jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getDockerImage();
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
						.withValue(ProductionPlanner.config.getProductionPlannerUrl() +"/v0.1/processingfacilities/" + kubeConfig.getId() + "/finish/" + jobName)
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
						.withName("ramdisk")
						.withMountPath("/mnt/ramdisk")
						.endVolumeMount()
						.endContainer()
						.addNewVolume()
						.withName("ramdisk")
						.withNewHostPath()
						.withPath("/tmp")
						.endHostPath()
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


					if (!js.isEmpty()) {
						aKubeConfig.getBatchApiV1().createNamespacedJob (aKubeConfig.getNamespace(), job, null, null, null);
						searchPod();

						jobStep.setJobStepState(JobStepState.READY);	
						RepositoryService.getJobStepRepository().save(jobStep);
						logger.info("Job " + kubeConfig.getId() + "/" + jobName + " created");
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
			KubeJobFinish toFini = new KubeJobFinish(this, jobname);
			toFini.start();
		}
	}	
	public boolean getFinishInfo(String aJobName) {
		boolean success = false;
		if (kubeConfig != null && kubeConfig.isConnected() && aJobName != null) {
			V1Job aJob = kubeConfig.getV1Job(aJobName);
			if (podNames.isEmpty()) {
				searchPod();
			}
			V1Pod aPod = kubeConfig.getV1Pod(podNames.get(podNames.size()-1));
			
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
										if (cd == null) {
											cd = jc.getLastProbeTime();
											js.get().setProcessingCompletionTime(cd.toDate().toInstant());
										}
										success = true;
									} else if ((jc.getType().equalsIgnoreCase("failed") || jc.getType().equalsIgnoreCase("failure")) && jc.getStatus().equalsIgnoreCase("true")) {
										js.get().setJobStepState(JobStepState.FAILED);		
										if (cd == null) {
											cd = jc.getLastProbeTime();
											js.get().setProcessingCompletionTime(cd.toDate().toInstant());
										}
										success = true;
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
		if (success) {
			// delete kube job
			kubeConfig.deleteJob(aJobName);
			logger.info("Job " + kubeConfig.getId() + "/" + aJobName + " finished");
		}
		return success;
	}
}
