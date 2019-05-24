/**
 * 
 */
package de.dlr.proseo.planner.kubernetes;


import java.util.HashMap;
import java.util.Map;

import com.google.common.io.ByteStreams;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Attach;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobBuilder;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1JobSpec;
import io.kubernetes.client.models.V1JobSpecBuilder;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodBuilder;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1Volume;
import io.kubernetes.client.models.V1VolumeBuilder;
import io.kubernetes.client.util.Config;

/**
 * @author melchinger
 *
 */
public class KubeConfig {

	private static HashMap<Integer, KubeJob> kubeJobList = null;
	
	private static ApiClient client;
	private static CoreV1Api apiV1;
	private static BatchV1Api batchApiV1;
	/**
	 * @return the client
	 */
	public static ApiClient getClient() {
		return client;
	}

	/**
	 * @return the apiV1
	 */
	public static CoreV1Api getApiV1() {
		return apiV1;
	}

	/**
	 * @return the batchApiV1
	 */
	public static BatchV1Api getBatchApiV1() {
		return batchApiV1;
	}


	
	public static boolean connect() {
		if (isConnected()) {
			return true;
		} else {
			kubeJobList = new HashMap<Integer, KubeJob>();

			client = Config.fromUrl("http://192.168.20.159:8080", false); 
			// Config.defaultClient();
			Configuration.setDefaultApiClient(client);

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
	
	public static boolean isConnected() {
	    if (apiV1 == null) {
	    	return false;
	    } else {
	    	return true;
	    }		
	}

	public static V1PodList getPodList() {
		V1PodList list = null;
		try {
			list = apiV1.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);
			for (V1Pod item : list.getItems()) {
				System.out.println(item.getMetadata().getName());
			}
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}

	public static V1JobList getJobList() {
		V1JobList list = null;
		try {
			list =  batchApiV1.listJobForAllNamespaces(null, null, null, null, null, null, null, null, null);
			for (V1Job item : list.getItems()) {
				System.out.println(item.getMetadata().getName());
			}
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}

	public static KubeJob createJob(String name) {
		int aKey = kubeJobList.size() + 1;
		KubeJob aJob = new KubeJob(aKey, name, "centos/perl-524-centos7", "/testdata/test1.pl", "perl");
		aJob = aJob.createJob();
		if (aJob != null) {
			kubeJobList.put(aJob.getJobId(), aJob);
		}
		return aJob;
	}
	public static KubeJob createJob() {
		return createJob(null);
	}

	public static boolean deleteJob(KubeJob aJob) {
		if (aJob != null) {
			return (KubeConfig.deleteJob(aJob.getJobName()));
		} else {
			return false;
		}
	}
	
	public static boolean deleteJob(String name) {
		V1DeleteOptions opt = new V1DeleteOptions();
		opt.setApiVersion("batchV1");
		opt.setPropagationPolicy("Foreground");
		try {
			batchApiV1.deleteNamespacedJob(name, "default", opt, null, null, 0, null, "Foreground");
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
}
