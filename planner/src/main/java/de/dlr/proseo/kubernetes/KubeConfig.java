/**
 * 
 */
package de.dlr.proseo.kubernetes;


import com.google.common.io.ByteStreams;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Attach;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobBuilder;
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

	static ApiClient client;
	static CoreV1Api apiV1;
	static BatchV1Api batchApiV1;

	public static boolean connect() {

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
	
	public static boolean isConnected() {
	    if (apiV1 == null) {
	    	return false;
	    } else {
	    	return true;
	    }		
	}
	
	public static V1PodList getPodList() {
		V1PodList list = null;
		createPod();
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

	public static V1Pod createPod() {
		if (KubeConfig.isConnected()) {
			V1Pod pod =
					new V1PodBuilder()
					.withNewMetadata()
					.withName("test")
					.endMetadata()
					.withNewSpec()
					.addNewContainer()
					.withName("cont1")
					.withImage("centos/perl-524-centos7")
					.withCommand("perl")
					.withArgs("/testdata/test1.pl")
					
					.endContainer()
					.endSpec()
					.build();
			V1JobSpec jobSpec = new V1JobSpecBuilder()
				.withNewTemplate()
				.withNewMetadata()
				.withName("jobexample")
				.addToLabels("jobgroup", "jobexample")
				.endMetadata()
				.withNewSpec()
				.addNewContainer()
				.withName("cont1")
				.withImage("centos/perl-524-centos7")
				.withCommand("perl")
				.withArgs("/testdata/test1.pl")
				.addNewVolumeMount()
				.withName("input")
				.withMountPath("/testdata")
				.endVolumeMount()
				.endContainer()
				.withRestartPolicy("Never")
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
				.withName("testjob")
				.addToLabels("jobgroup", "jobexample")
				.endMetadata()
				.withSpec(jobSpec)
				.build();
			try {
				batchApiV1.createNamespacedJob ("default", job, null, null, null);
			} catch (ApiException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			/*
			 * try { pod = apiV1.createNamespacedPod("default", pod, null, null, null); }
			 * catch (ApiException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
			return pod;
		} else {
			return null;
		}
	}
}
