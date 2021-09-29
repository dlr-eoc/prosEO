/**
 * KubeConfig.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.kubernetes;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.enums.StorageType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.rest.model.PodKube;
import de.dlr.proseo.planner.util.UtilService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Taint;
import io.kubernetes.client.util.Config;

/**
 * Represents the connection to a Kubernetes API 
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class KubeConfig {

	/** Dummy Job Order file name (only used in development environment) **/
	private static final String DUMMY_JOF_FILENAME = "/testdata/test1.pl";

	/**
	 * Logger of this class 
	 */
	private static Logger logger = LoggerFactory.getLogger(KubeConfig.class);
	
	/**
	 * Map containing the created Kubernetes jobs
	 */
	private HashMap<String, KubeJob> kubeJobList = null;
	
	/**
	 * List of Kubernetes nodes
	 */
	private V1NodeList kubeNodes = null;
	
	/**
	 * Number of ready Kubernetes worker nodes
	 */
	private int workerCnt = 0;
	
	/**
	 * The Kubernetes API client
	 */
	private ApiClient client;
	
	/**
	 * The Kubernetes core API V1 
	 */
	private CoreV1Api apiV1;
	
	/**
	 * The Kubernetes batch API V1 
	 */
	private BatchV1Api batchApiV1;
	
	/**
	 * The name of the facility
	 */
	private String id;

	/**
	 * The id of the facility
	 */
	private long longId;

	/**
	 * The version of the facility
	 */
	private long version;
	
	/**
	 * The facility description 
	 */
	private String description;
	
	/**
	 * The run state the facility currently is in 
	 */
	private FacilityState facilityState;
	
	/**
	 * The maximum of jobs per node
	 */
	private Integer maxJobsPerNode;
	
	/**
	 * The url of the facility 
	 */
	private String url;
	
	/**
	 * The storage manager url
	 */
	private String storageManagerUrl;
	
	/**
	 * The default storage type 
	 */
	private StorageType storageType;
	
	/** Authentication token for connecting to this facility's processing engine (Kubernetes instance) */
	private String processingEngineToken;
	
	/**
	 * URL of the locally accessible Storage Manager instance on a specific processing node (to be used by the Processing Engine).
	 * This URL shall contain the string "%NODE_IP%", which will be replaced by the actual node IP of the Kubernetes worker node.
	 */
	private String localStorageManagerUrl;
	
	/** User name for connecting to the Storage Manager (locally and from external services) */
	private String storageManagerUser;
	
	/** Password for connecting to the Storage Manager (locally and from external services) */
	private String storageManagerPassword;

	/**
	 * @return the maxJobsPerNode
	 */
	public Integer getMaxJobsPerNode() {
		return maxJobsPerNode;
	}

	/**
	 * @param maxJobsPerNode the maxJobsPerNode to set
	 */
	public void setMaxJobsPerNode(Integer maxJobsPerNode) {
		this.maxJobsPerNode = maxJobsPerNode;
	}

	/**
	 * @return the storageManagerPassword
	 */
	public String getStorageManagerPassword() {
		return storageManagerPassword;
	}

	/**
	 * @param storageManagerPassword the storageManagerPassword to set
	 */
	public void setStorageManagerPassword(String storageManagerPassword) {
		this.storageManagerPassword = storageManagerPassword;
	}

	/**
	 * @return the processingEngineToken
	 */
	public String getProcessingEngineToken() {
		return processingEngineToken;
	}

	/**
	 * @return the localStorageManagerUrl
	 */
	public String getLocalStorageManagerUrl() {
		return localStorageManagerUrl;
	}

	/**
	 * @return the storageManagerUser
	 */
	public String getStorageManagerUser() {
		return storageManagerUser;
	}

	/**
	 * @param storageManagerUrl the storageManagerUrl to set
	 */
	public void setStorageManagerUrl(String storageManagerUrl) {
		this.storageManagerUrl = storageManagerUrl;
	}

	/**
	 * @param processingEngineToken the processingEngineToken to set
	 */
	public void setProcessingEngineToken(String processingEngineToken) {
		this.processingEngineToken = processingEngineToken;
	}

	/**
	 * @param localStorageManagerUrl the localStorageManagerUrl to set
	 */
	public void setLocalStorageManagerUrl(String localStorageManagerUrl) {
		this.localStorageManagerUrl = localStorageManagerUrl;
	}

	/**
	 * @param storageManagerUser the storageManagerUser to set
	 */
	public void setStorageManagerUser(String storageManagerUser) {
		this.storageManagerUser = storageManagerUser;
	}

	/**
	 * @return the storageType
	 */
	public StorageType getStorageType() {
		return storageType;
	}

	/**
	 * @param storageType the storageType to set
	 */
	public void setStorageType(StorageType storageType) {
		this.storageType = storageType;
	}

	private ProcessingFacility processingFacility;
	private int nodesDelta = 0;
	
	/**
	 * @return the processingFacility
	 */
	public ProcessingFacility getProcessingFacility() {
		return processingFacility;
	}

	/**
	 * @param processingFacility the processingFacility to set
	 */
	public void setProcessingFacility(ProcessingFacility processingFacility) {
		this.processingFacility = processingFacility;
	}

	// no need to create own namespace, because only one "user" (prosEO) 
	private String namespace = "default";
	
	/**
	 * Look for the kube job of name.
	 * 
	 * @param name The name of a kube job
	 * @return The kube job found or null
	 */
	public KubeJob getKubeJob(String name) {
		return kubeJobList.get(name);
	}
	
	/**
	 * Instantiate a KubeConfig object without arguments
	 */

	public KubeConfig() {
	}

	/**
	 * Instantiate a KubeConfig object with a processing facility
	 * @param pf the processing facility to set
	 */
	public KubeConfig(ProcessingFacility pf) {
		setFacility(pf);
	}
	
	/**
	 * Set all locally stored variables
	 * 
	 * @param pf The processing facility
	 */
	public void setFacility(ProcessingFacility pf) {
		id = pf.getName();
		longId = pf.getId();
		version = pf.getVersion();
		description = pf.getDescription();
		facilityState = pf.getFacilityState();
		url = pf.getProcessingEngineUrl();
		storageManagerUrl = pf.getStorageManagerUrl();
		storageType = pf.getDefaultStorageType();
		localStorageManagerUrl = pf.getLocalStorageManagerUrl();
		processingEngineToken = pf.getProcessingEngineToken();
		storageManagerUser = pf.getStorageManagerUser();
		storageManagerPassword = pf.getStorageManagerPassword();
		maxJobsPerNode = pf.getMaxJobsPerNode();
		processingFacility = pf;
	}
	
	/**
	 * @return the storage manager URL
	 */
	public String getStorageManagerUrl() {
		return storageManagerUrl;
	}

	/**
	 * @return the ApiClient
	 */
	public ApiClient getClient() {
		return client;
	}

	/**
	 * @return the CoreV1Api
	 */
	public CoreV1Api getApiV1() {
		return apiV1;
	}

	/**
	 * @return the BatchV1Api
	 */
	public BatchV1Api getBatchApiV1() {
		return batchApiV1;
	}
	
	/**
	 * @return the workerCnt
	 */
	public int getWorkerCnt() {
		return workerCnt;
	}

	/**
	 * Connect to the Kubernetes cluster
	 * 
	 * @return true if connected, otherwise false
	 */
	public boolean connect() {
		if (logger.isTraceEnabled()) logger.trace(">>> connect()");
		
		if (getFacilityState() == FacilityState.DISABLED || getFacilityState() == FacilityState.STOPPED)
			// nothing to do
			return false;
		
		if (isConnected()) {
			return true;
		} else {
			kubeJobList = new HashMap<String, KubeJob>();
			if (processingEngineToken != null && !processingEngineToken.isEmpty()) {
				try {
					client = null;
					client = Config.fromToken(url, 
						 processingEngineToken, 
						 false);
				} catch (Exception ex) {
					logger.info(ex.getMessage());
				}
			}
			if (client == null) {
                try {
                	// describes Kubernetes in Docker
                	String kconf = ProductionPlanner.config.getProductionPlannerKubeConfig();
                	if (kconf == null || kconf.isEmpty()) {
                		kconf = "kube_config";
                	}
                    client = Config.fromConfig(kconf);
                } catch (IOException e) {
                    logger.info("Cannot access Kubernetes Configuration file: " + e.getMessage());
                }
                if (client == null) {
                    client = Config.fromUrl(url, false);
                }
			}
			if (client == null) {
				logger.error("Could not connect with facility " + url);
			}
			if (logger.isTraceEnabled()) {
				client.setDebugging(true);
			}
			Configuration.setDefaultApiClient(client);
			apiV1 = new CoreV1Api();
			batchApiV1 = new BatchV1Api();
			if (apiV1 == null || batchApiV1 == null) {
				apiV1 = null;
				batchApiV1 = null;
				return false;
			} else {								
				// allow more response time and enhance time out 
				client.setReadTimeout(100000);
				client.setWriteTimeout(100000);
				client.setConnectTimeout(100000);

				// get node info
				sync();
			}
			return true;
		}
	}
	
	/**
	 * @return true if connected, otherwise false
	 */
	public boolean isConnected() {
		if (getFacilityState() == FacilityState.DISABLED || apiV1 == null) {
	    	return false;
	    } else {
	    	return true;
	    }		
	}

	/**
	 * Check whether a new Kubernetes job could run.
	 * TODO At the moment simple check against worker count. Could possibly be improved
	 * @return
	 */
	public boolean couldJobRun() {
		if (logger.isTraceEnabled()) logger.trace(">>> couldJobRun()");
		
		if (getFacilityState() == FacilityState.DISABLED || getFacilityState() == FacilityState.STOPPED 
				|| getFacilityState() == FacilityState.STOPPING || getFacilityState() == FacilityState.STARTING) {
			// not available for jobs
			return false;
		}
		Integer mJPN = 1;
		if (getMaxJobsPerNode() != null) {
			mJPN = getMaxJobsPerNode();
		}
		return (kubeJobList.size() < ((getWorkerCnt() * mJPN) + nodesDelta));
	}

	/**
	 * Synchronize Kubernetes cluster and planner 
	 */
	public void sync() {
		if (logger.isTraceEnabled()) logger.trace(">>> sync()");
		
		if (getFacilityState() == FacilityState.DISABLED || getFacilityState() == FacilityState.STOPPED || getFacilityState() == FacilityState.STARTING)
			// nothing to do
			return;
		
		// rebuild runtime data 
		V1JobList k8sJobList = null;
		getNodeInfo();
		try {
			k8sJobList = batchApiV1.listJobForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
		} catch (ApiException e) {
			logger.error(Messages.RUNTIME_EXCEPTION.format(e.getMessage()), e);
			return;
		}
		// update kubeJob list
		Map<String, V1Job> kJobs = new HashMap<String, V1Job>();
		for (V1Job aJob : k8sJobList.getItems()) {
			String kName = aJob.getMetadata().getName();
			kJobs.put(kName, aJob);
			if (getKubeJob(kName) == null) {
				if (!kubeJobList.containsKey(aJob.getMetadata().getName())) {
					KubeJob kj = new KubeJob();
					kj = kj.rebuild(this, aJob);
					if (kj != null) {
						kubeJobList.put(kj.getJobName(), kj);
					}
				}
			}
		}
		// now are all existing Kubernetes jobs synchronized with KubeJob list.
		// walk through KubeJob list and remove elements without existing Kubernetes job
		List<KubeJob> kjList = new ArrayList<KubeJob>();
		kjList.addAll(kubeJobList.values());
		for (KubeJob kj : kjList) {
			if (!kJobs.containsKey(kj.getJobName())) {
				kubeJobList.remove(kj.getJobName());
			}
		}
		// Look whether Kubernetes job is running or has already finished without message event to Planner
		// Walk through Kubernetes job list (to manipulate KubeJob list)
		for (V1Job aJob : k8sJobList.getItems()) {
			String kName = aJob.getMetadata().getName();
			KubeJob kj = kubeJobList.get(kName);
			if (kj != null) {
				if (kj.updateFinishInfoAndDelete(kName)) {
					
				}
			}			
		}
		// get all job steps of DB with states RUNNING
		List<de.dlr.proseo.model.JobStep.JobStepState> jobStepStates = new ArrayList<>();
		jobStepStates.add(de.dlr.proseo.model.JobStep.JobStepState.RUNNING);
		List<JobStep> runningJobSteps = RepositoryService.getJobStepRepository().findAllByProcessingFacilityAndJobStepStateIn(processingFacility.getId(), jobStepStates);
		// These job steps has to be in Kubernetes job list. If not, there was a problem. Set it to failed.
		for (JobStep js : runningJobSteps) {
			String aName = ProductionPlanner.jobNamePrefix + js.getId();
			// If no job with this name is running, change state to FAILED and set info in log
			if (!kJobs.containsKey(aName)) {
				js.setJobStepState(JobStepState.FAILED);	
				js.incrementVersion();
				String stdout = js.getProcessingStdOut();
				if (stdout == null) {
					stdout = "";
				}
				js.setProcessingStdOut("Job on Processing Facility was deleted/canceled by others (e.g. operator)\n\n" + stdout);
				js = RepositoryService.getJobStepRepository().save(js);
				UtilService.getJobStepUtil().checkFinish(js);
			}			
		}		
	}
	/**
	 * Retrieve all pods of cluster
	 * 
	 * @return List of pods
	 */
	public V1PodList getPodList() {
		V1PodList list = null;
		try {
			list = apiV1.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
		} catch (ApiException e) {
			logger.error(Messages.RUNTIME_EXCEPTION.format(e.getMessage()), e);
			return null;
		}
		return list;
	}

	/**
	 * Retrieve all jobs of cluster
	 * 
	 * @return List of jobs
	 */
	public V1JobList getJobList() {
		V1JobList list = null;
		try {
			list =  batchApiV1.listJobForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
			
		} catch (ApiException e) {
			logger.error(Messages.RUNTIME_EXCEPTION.format(e.getMessage()), e);
			return null;
		}
		return list;
	}

	/**
	 * Create a new job on cluster
	 * 
	 * @param name of new job
	 * @return new job or null
	 */
	@Transactional
	public KubeJob createJob(String name, String stdoutLogLevel, String stderrLogLevel) {
		if (logger.isTraceEnabled()) logger.trace(">>> createJob({}, {}, {})", name, stdoutLogLevel, stderrLogLevel);
		
		KubeJob aJob = new KubeJob(Long.parseLong(name), DUMMY_JOF_FILENAME);
		try {
			aJob = aJob.createJob(this, stdoutLogLevel, stderrLogLevel);
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				// We did not throw this one!
				logger.error("Job creation failed with exception: " + e.getMessage(), e);
			}
			aJob = null;
		}
		if (aJob != null) {
			kubeJobList.put(aJob.getJobName(), aJob);
		}
		return aJob;
	}

	/**
	 * Create a new job on cluster
	 * 
	 * @param id the job ID
	 * @param stdoutLogLevel the log level to set for stdout
	 * @param stderrLogLevel the log level to set for stderr
	 * @return
	 */
	@Transactional
	public KubeJob createJob(long id, String stdoutLogLevel, String stderrLogLevel) {
		if (logger.isTraceEnabled()) logger.trace(">>> createJob({}, {}, {})", id, stdoutLogLevel, stderrLogLevel);
		
		KubeJob aJob = new KubeJob(id, DUMMY_JOF_FILENAME);
		try {
			aJob = aJob.createJob(this, stdoutLogLevel, stderrLogLevel);
		} catch (Exception e) {
			aJob = null;
		}
		if (aJob != null) {
			kubeJobList.put(aJob.getJobName(), aJob);
		}
		return aJob;
	}

	/**
	 * Delete a job from cluster
	 * 
	 * @param aJob to delete
	 * @return true after success, otherwise false
	 */
	public boolean deleteJob(KubeJob aJob) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteJob({})", (null == aJob ? "null" : aJob.getJobId()));
		
		if (aJob != null) {
			return (deleteJob(aJob.getJobName()));
		} else {
			return false;
		}
	}

	/**
	 * Delete a named job from cluster
	 * 
	 * @param name of job to delete
	 * @return true after success, otherwise false
	 */
	public boolean deleteJob(String name) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteJob({})", name);
		
		V1DeleteOptions opt = new V1DeleteOptions();
		opt.setApiVersion("batchV1");
		opt.setPropagationPolicy("Foreground");
		opt.setGracePeriodSeconds((long) 0);
		try {
			batchApiV1.deleteNamespacedJob(name, namespace, "false", null, 0, null, "Foreground", opt);
		} catch (Exception e) {
			if (e instanceof IllegalStateException || e.getCause() instanceof IllegalStateException ) {
				// nothing to do 
				// cause there is a bug in Kubernetes API
			} else if (e instanceof ApiException) {
				if (((ApiException) e).getCode() == HttpStatus.NOT_FOUND.value()) {
					// Already gone (for whatever reason, maybe Kubernetes breakdown, maybe manual intervention)
					return true;
				} else {
					e.printStackTrace();
					return false;
				}
			} else {
				e. printStackTrace();
				return false;
			}
		} finally {
			kubeJobList.remove(name);
		}
		return true;
	}
	
	/**
	 * Retrieve a Kubernetes job
	 * 
	 * @param name of job
	 * @return job found or null
	 */
	public V1Job getV1Job(String name) {
		if (logger.isTraceEnabled()) logger.trace(">>> getV1Job({})", name);
		
		V1Job aV1Job = null;
		try {
			aV1Job = batchApiV1.readNamespacedJob(name, namespace, null, null, null);
		} catch (ApiException e) {
			if (e.getCode() == 404) {
				// do nothing
				Messages.KUBECONFIG_JOB_NOT_FOUND.log(logger, name);
				return null;			
			} else {
				logger.error(Messages.RUNTIME_EXCEPTION.format(e.getMessage()), e);
				return null;				
			}
		} catch (Exception e) {
			if (e instanceof IllegalStateException || e.getCause() instanceof IllegalStateException ) {
				// nothing to do 
				// cause there is a bug in Kubernetes API
			} else {
				logger.error(Messages.RUNTIME_EXCEPTION.format(e.getMessage()), e);
				return null;
			}
		}
		return aV1Job;
	}
	/**
	 * Retrieve a Kubernetes Pod
	 * 
	 * @param name of Pod
	 * @return pod found or null
	 */
	public V1Pod getV1Pod(String name) {
		if (logger.isTraceEnabled()) logger.trace(">>> getV1Pod({})", name);
		
		V1Pod aV1Pod = null;
		try {
			aV1Pod = apiV1.readNamespacedPod(name, namespace, null, null, null);
		} catch (Exception e) {
			if (e instanceof IllegalStateException || e.getCause() instanceof IllegalStateException ) {
				// nothing to do 
				// cause there is a bug in Kubernetes API
			} else if (e instanceof ApiException && ((ApiException) e).getCode() == 404) {
				// pod not found
				return null;
			} else {
				logger.error(Messages.RUNTIME_EXCEPTION.format(e.getMessage()), e);
				return null;
			}
		}
		return aV1Pod;
	}

	/**
	 * @return namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * @return id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return longId
	 */
	public long getLongId() {
		return longId;
	}

	/**
	 * @return version
	 */
	public long getVersion() {
		return version;
	}
	
	/**
	 * @return url
	 */
	public String getUrl() {
		return url;
	}
	
	/**
	 * @return url
	 */
	public String getProcessingEngineUrl() {
		return url;
	}
	
	/**
	 * @return description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * @return the facility state
	 */
	public FacilityState getFacilityState() {
		return facilityState;
	}

	/**
	 * @param facilityState the facility state to set
	 */
	public void setFacilityState(FacilityState facilityState) {
		this.facilityState = facilityState;
	}

	/**
	 * Delete a job
	 * 
	 * @param name of pod/job
	 * @return true after success, otherwise false
	 */
	public boolean deletePodNamed(String name) {
		if (logger.isTraceEnabled()) logger.trace(">>> deletePodNamed({})", name);
		
		if (this.isConnected()) {
			return this.deleteJob(name);
		}
		return false;
	}
	
	/**
	 * Delete all pods of status
	 * 
	 * @param status of pods to delete
	 * @return true after success, otherwise false
	 */
	public boolean deletePodsStatus(String status) {
		if (logger.isTraceEnabled()) logger.trace(">>> deletePodsStatus({})", status);
		
		if (this.isConnected()) {		 
			V1JobList list = this.getJobList();
			if (list != null) {
				for (V1Job item : list.getItems()) {
					PodKube pk = new PodKube(item);
					if (pk != null && pk.hasStatus(status)) {
						this.deleteJob(pk.getName());
					}
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Search ready worker nodes on Kubernetes cluster
	 * 
	 * @return true if at least one worker node is ready
	 */
	public boolean getNodeInfo() {
		if (logger.isTraceEnabled()) logger.trace(">>> getNodeInfo()");
		
		kubeNodes = null;
		workerCnt = 0;
		try {
			kubeNodes = apiV1.listNode(null, null, null, null, null, null, null, null, null, null);
			if (kubeNodes != null) {
				for (V1Node node : kubeNodes.getItems()) {
					if (node.getSpec() != null) {
						if (node.getSpec().getTaints() != null) {
							for (V1Taint taint : node.getSpec().getTaints()) {
								if (taint.getEffect() != null) {
									if (!taint.getEffect().equalsIgnoreCase("NoSchedule") && !taint.getEffect().equalsIgnoreCase("NoExecute")) {
										workerCnt++;
									}
								}
							}
						} else {
							workerCnt++;
						}
					} else {
						workerCnt++;
					}
				}						
			}
		} catch (ApiException e) {
			logger.error(Messages.RUNTIME_EXCEPTION.format(e.getMessage()), e);
			return false;
		}
		
		return workerCnt > 0;
	}
}
