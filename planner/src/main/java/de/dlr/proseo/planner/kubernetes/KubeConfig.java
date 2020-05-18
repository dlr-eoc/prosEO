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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProductFile.StorageType;
import de.dlr.proseo.model.rest.model.PlannerPod;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.rest.model.PodKube;
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

	private static Logger logger = LoggerFactory.getLogger(KubeConfig.class);
	
	private HashMap<String, KubeJob> kubeJobList = null;
	
	private V1NodeList kubeNodes = null;
	
	private int workerCnt = 0;
	
	private ApiClient client;
	private CoreV1Api apiV1;
	private BatchV1Api batchApiV1;
	private String id;
	private long longId;
	private String description;
	private String url;
	private String storageManagerUrl;
	private StorageType storageType;
	/** User name for connecting to this facility's processing engine (Kubernetes instance) */
	private String processingEngineUser;
	
	/** Password for connecting to this facility's processing engine (Kubernetes instance) */
	private String processingEnginePassword;

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
	 * @return the processingEngineUser
	 */
	public String getProcessingEngineUser() {
		return processingEngineUser;
	}

	/**
	 * @return the processingEnginePassword
	 */
	public String getProcessingEnginePassword() {
		return processingEnginePassword;
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
	 * @param processingEngineUser the processingEngineUser to set
	 */
	public void setProcessingEngineUser(String processingEngineUser) {
		this.processingEngineUser = processingEngineUser;
	}

	/**
	 * @param processingEnginePassword the processingEnginePassword to set
	 */
	public void setProcessingEnginePassword(String processingEnginePassword) {
		this.processingEnginePassword = processingEnginePassword;
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
	
	public KubeJob getKubeJob(String name) {
		return kubeJobList.get(name);
	}
	
	/**
	 * Instantiate a KuebConfig object
	 * 
	 * @param pf the ProcessingFacility
	 */

	public KubeConfig () {
	}

	public KubeConfig (ProcessingFacility pf) {
		id = pf.getName();
		longId = pf.getId();
		description = pf.getDescription();
		url = pf.getProcessingEngineUrl();
		storageManagerUrl = pf.getStorageManagerUrl();
		storageType = pf.getDefaultStorageType();
		localStorageManagerUrl = pf.getLocalStorageManagerUrl();
		processingEngineUser = pf.getProcessingEngineUser();
		processingEnginePassword = pf.getProcessingEnginePassword();
		storageManagerUser = pf.getStorageManagerUser();
		storageManagerPassword = pf.getStorageManagerPassword();
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
		if (isConnected()) {
			return true;
		} else {
			kubeJobList = new HashMap<String, KubeJob>();

			if (id.equalsIgnoreCase("OTC")) {
//				try {
//					client = Config.fromConfig("I:\\usr\\prosEO\\kubernetes\\auth\\config");
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//					return false;
//				}
				client = Config.fromUserPassword(url, 
						 "user1", 
						 "c36ff53775d69aa6bdbfa1486d8908a2fc9c38e712e6b0b69584a4f0cd9e8006", 
						 false);
			} else 	if (id.equalsIgnoreCase("Lerchenhof")) {
				client = Config.fromUrl(url, false);
			} else {
                try {
                	// beschreibt Kubernetes in Docker
                    client = Config.fromConfig("kube_config");
                } catch (IOException e) {
                    logger.info("Cannot access Kubernetes Configuration file: " + e.getMessage());
                }
                if (client == null) {
                    client = Config.fromUrl(url, false);
                }
			}
			client.setDebugging(true);
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
				getNodeInfo();
				sync();
			}
			return true;
		}
	}
	
	/**
	 * @return true if connected, otherwise false
	 */
	public boolean isConnected() {
	    if (apiV1 == null) {
	    	return false;
	    } else {
	    	return true;
	    }		
	}

	public boolean couldJobRun() {
		return (kubeJobList.size() < (getWorkerCnt() + nodesDelta));
	}

	public void sync() {
		// rebuild runtime data 
		V1JobList k8sJobList = null;
		try {
			k8sJobList = batchApiV1.listJobForAllNamespaces(null, null, null, null, null, null, null, null, null);
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		// Look wheather Kubernetes job is running or has already finished without message event to Planner
		// Walk through Kubernetes job list (to manipulate KubeJob list)
		for (V1Job aJob : k8sJobList.getItems()) {
			String kName = aJob.getMetadata().getName();
			KubeJob kj = kubeJobList.get(kName);
			if (kj != null) {
				if (kj.getFinishInfo(kName)) {
					
				}
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
			list = apiV1.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			list =  batchApiV1.listJobForAllNamespaces(null, null, null, null, null, null, null, null, null);
			
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		int aKey = kubeJobList.size() + 1;
		// KubeJob aJob = new KubeJob(aKey, null, "centos/perl-524-centos7", "/testdata/test3.pl", "perl", null);
		KubeJob aJob = new KubeJob(Long.parseLong(name), "/testdata/test1.pl");
		aJob = aJob.createJob(this, stdoutLogLevel, stderrLogLevel);
		if (aJob != null) {
			kubeJobList.put(aJob.getJobName(), aJob);
		}
		return aJob;
	}

	/**
	 * Create a new job on cluster
	 * 
	 * @param name of new job
	 * @return new job or null
	 */
	@Transactional
	public KubeJob createJob(long id, String stdoutLogLevel, String stderrLogLevel) {
		int aKey = kubeJobList.size() + 1;
		// KubeJob aJob = new KubeJob(aKey, null, "centos/perl-524-centos7", "/testdata/test3.pl", "perl", null);
		KubeJob aJob = new KubeJob(id, "/testdata/test1.pl");
		aJob = aJob.createJob(this, stdoutLogLevel, stderrLogLevel);
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
		V1Job aV1Job = null;
		try {
			aV1Job = batchApiV1.readNamespacedJob(name, namespace, null, null, null);
		} catch (ApiException e) {
			if (e.getCode() == 404) {
				// do nothing
				Messages.KUBECONFIG_JOB_NOT_FOUND.log(logger, name);
				return null;			
			} else {
				e. printStackTrace();
				return null;				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (e instanceof IllegalStateException || e.getCause() instanceof IllegalStateException ) {
				// nothing to do 
				// cause there is a bug in Kubernetes API
			} else {
				e. printStackTrace();
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
		V1Pod aV1Pod = null;
		try {
			aV1Pod = apiV1.readNamespacedPod(name, namespace, null, null, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (e instanceof IllegalStateException || e.getCause() instanceof IllegalStateException ) {
				// nothing to do 
				// cause there is a bug in Kubernetes API
			} else {
				e. printStackTrace();
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
	 * Delete a job
	 * 
	 * @param name of pod/job
	 * @return true after success, otherwise false
	 */
	public boolean deletePodNamed(String name) {
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
		if (this.isConnected()) {		 
			V1JobList list = this.getJobList();
			List<PlannerPod> jobList = new ArrayList<PlannerPod>();
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
	
	public boolean getNodeInfo() {
		kubeNodes = null;
		workerCnt = 0;
		try {
			kubeNodes = apiV1.listNode(null, null, null, null, null, null, null, null, null);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return workerCnt > 0;
	}
}
