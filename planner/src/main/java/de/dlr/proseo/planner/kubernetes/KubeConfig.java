/**
 * 
 */
package de.dlr.proseo.planner.kubernetes;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
	

	public KubeConfig (String anId, String aDescription, String aUrl) {
		id = anId;
		description = aDescription;
		url = aUrl;
	}
	/**
	 * @return the client
	 */
	public ApiClient getClient() {
		return client;
	}

	/**
	 * @return the apiV1
	 */
	public CoreV1Api getApiV1() {
		return apiV1;
	}

	/**
	 * @return the batchApiV1
	 */
	public BatchV1Api getBatchApiV1() {
		return batchApiV1;
	}

	/**
	 * @return the namespace
	 */
	public String getNamespace() {
		return namespace;
	}
	/**
	 * @return the namespace
	 */
	public void setNamespace(String aNamespace) {
		namespace = aNamespace;
	}


	
	public boolean connect() {
		if (isConnected()) {
			return true;
		} else {
			kubeJobList = new HashMap<Integer, KubeJob>();

			client = Config.fromUrl(url, false); 
			// Config.defaultClient();
			Configuration.setDefaultApiClient(client);
			client.getHttpClient().setReadTimeout(100000, TimeUnit.MILLISECONDS);
			client.getHttpClient().setWriteTimeout(100000, TimeUnit.MILLISECONDS);
			client.getHttpClient().setConnectTimeout(100000, TimeUnit.MILLISECONDS);
			apiV1 = new CoreV1Api();
			batchApiV1 = new BatchV1Api();
			if (apiV1 == null || batchApiV1 == null) {
				apiV1 = null;
				batchApiV1 = null;
				return false;
			} else {
				if (getPodList() == null) {
					apiV1 = null;
					batchApiV1 = null;
					return false;
				} else {
					return true;
				}
			}
		}
	}
	
	public boolean isConnected() {
	    if (apiV1 == null) {
	    	return false;
	    } else {
	    	return true;
	    }		
	}

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

	public KubeJob createJob(String name) {
		int aKey = kubeJobList.size() + 1;
		KubeJob aJob = new KubeJob(aKey, name, "centos/perl-524-centos7", "/testdata/test1.pl", "perl");
		aJob = aJob.createJob(this);
		if (aJob != null) {
			kubeJobList.put(aJob.getJobId(), aJob);
		}
		return aJob;
	}
	public KubeJob createJob() {
		return createJob(null);
	}

	public boolean deleteJob(KubeJob aJob) {
		if (aJob != null) {
			return (deleteJob(aJob.getJobName()));
		} else {
			return false;
		}
	}

	public boolean deleteJob(String name) {
		V1DeleteOptions opt = new V1DeleteOptions();
		opt.setApiVersion("batchV1");
		opt.setPropagationPolicy("Foreground");
		try {
			batchApiV1.deleteNamespacedJob(name, getNamespace(), opt, null, null, 0, null, "Foreground");
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
	public KubeJob getJob(String name) {
		V1Job aV1Job = null;
		KubeJob aJob = null;
		try {
			aV1Job = batchApiV1.readNamespacedJob(name, getNamespace(), null, true, true);
			if (aV1Job != null) {
				// todo
				// aJob = new KubeJob(0, name, name, name, name);
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
		return aJob;
	}
	public String getId() {
		return id;
	}
	public String getUrl() {
		return url;
	}
	public String getDescription() {
		return description;
	}
	public boolean deletePodNamed(String name) {
		if (this.isConnected()) {
			return this.deleteJob(name);
		}
		return false;
	}
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
