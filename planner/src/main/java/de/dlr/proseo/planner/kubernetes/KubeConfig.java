/**
 * KubeConfig.java
 */
package de.dlr.proseo.planner.kubernetes;


import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.dlr.proseo.model.dao.JobStepRepository;
import de.dlr.proseo.planner.rest.model.PlannerPod;
import de.dlr.proseo.planner.rest.model.PodKube;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.util.Config;

/**
 * Represents the connection to a Kubernetes API 
 * 
 * @author melchinger
 *
 */
public class KubeConfig {

	private HashMap<Integer, KubeJob> kubeJobList = null;
	
	private ApiClient client;
	private CoreV1Api apiV1;
	private BatchV1Api batchApiV1;
	private String id;
	private String description;
	private String url;
	
	// no need to create own namespace, because only one "user" (prosEO) 
	private String namespace = "default";
	

	/**
	 * Instantiate a KuebConfig obejct
	 * 
	 * @param anId Name of ProcessingFacility
	 * @param aDescription Description of ProcessingFacility
	 * @param aUrl URL of ProcessingFacility
	 */
	public KubeConfig (String anId, String aDescription, String aUrl) {
		id = anId;
		description = aDescription;
		url = aUrl;
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
	 * Connect to the Kubernetes cluster
	 * 
	 * @return true if connected, otherwise false
	 */
	public boolean connect() {
		if (isConnected()) {
			return true;
		} else {
			kubeJobList = new HashMap<Integer, KubeJob>();

			client = Config.fromUrl(url, false); 
			// Config.defaultClient();
			Configuration.setDefaultApiClient(client);
			apiV1 = new CoreV1Api();
			batchApiV1 = new BatchV1Api();
			if (apiV1 == null || batchApiV1 == null) {
				apiV1 = null;
				batchApiV1 = null;
				return false;
			} else {
				V1PodList list = null;
				try {
					list = apiV1.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);
					// allow more response time and enhance time out 
					client.getHttpClient().setReadTimeout(100000, TimeUnit.MILLISECONDS);
					client.getHttpClient().setWriteTimeout(100000, TimeUnit.MILLISECONDS);
					client.getHttpClient().setConnectTimeout(100000, TimeUnit.MILLISECONDS);
				} catch (ApiException e) {
					// message handled in caller
					if (e.getCause() == null || e.getCause().getClass() != ConnectException.class) {
						e.printStackTrace();
					}
					apiV1 = null;
					batchApiV1 = null;
					return false;
				}
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
	public KubeJob createJob(String name) {
		int aKey = kubeJobList.size() + 1;
		KubeJob aJob = new KubeJob(aKey, null, "centos/perl-524-centos7", "/testdata/test1.pl", "perl");
		aJob = aJob.createJob(this);
		if (aJob != null) {
			kubeJobList.put(aJob.getJobId(), aJob);
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
		try {
			batchApiV1.deleteNamespacedJob(name, namespace, opt, null, null, 0, null, "Foreground");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (e instanceof IllegalStateException || e.getCause() instanceof IllegalStateException ) {
				// nothing to do 
				// cause there is a bug in Kubernetes API
			} else {
				e. printStackTrace();
				return false;
			}
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
			aV1Job = batchApiV1.readNamespacedJob(name, namespace, null, true, true);
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
}
