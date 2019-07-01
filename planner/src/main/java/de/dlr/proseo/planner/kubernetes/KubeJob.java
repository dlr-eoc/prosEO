/**
 * 
 */
package de.dlr.proseo.planner.kubernetes;

import de.dlr.proseo.model.joborder.JobOrder;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobBuilder;
import io.kubernetes.client.models.V1JobSpec;
import io.kubernetes.client.models.V1JobSpecBuilder;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodBuilder;

/**
 * @author melchinger
 *
 */
public class KubeJob {
	private int jobId;
	private String jobName;
	private String containerName;
	private String imageName;
	private String command;
	private String jobOrderFileName;
	private JobOrder jobOrder;
	/**
	 * @return the jobId
	 */
	public int getJobId() {
		return jobId;
	}

	/**
	 * @return the jobName
	 */
	public String getJobName() {
		return jobName;
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

	public KubeJob (int id, String name, String processor, String jobOrderFN, String cmd) {
		jobId = id;
		if (name != null) {
			jobName = name + jobId;			
		}else {
			jobName = "proseojob" + jobId;
		}
		containerName = "proseocont" + jobId;
		imageName = processor;
		command = cmd;
		jobOrderFileName = jobOrderFN;
		
	}

	public KubeJob createJob() {
		if (KubeConfig.isConnected()) {
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
				.withCommand(command)
				.withArgs(jobOrderFileName)
				.addNewEnv()
				.withName("JOBORDER_FILE")
				.withValue(jobOrderFileName)
				.endEnv()
				.addNewEnv()
				.withName("FS_TYPE")
				.withValue("posix")
				.endEnv()
				.addNewEnv()
				.withName("LOGFILE_TARGET")
				.withValue("")
				.endEnv()
				.addNewEnv()
				.withName("STATE_CALLBACK_ENDPOINT")
				.withValue("http://")
				.endEnv()
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
				.withName(jobName)
				.addToLabels("jobgroup", jobName + "spec")
				.endMetadata()
				.withSpec(jobSpec)
				.build();
			try {
				KubeConfig.getBatchApiV1().createNamespacedJob ("default", job, null, null, null);
			} catch (ApiException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return null;
			}
			/*
			 * try { pod = apiV1.createNamespacedPod("default", pod, null, null, null); }
			 * catch (ApiException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
			return this;
		} else {
			return null;
		}
	}
}
