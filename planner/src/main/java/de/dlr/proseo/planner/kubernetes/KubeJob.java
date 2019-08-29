/**
 * 
 */
package de.dlr.proseo.planner.kubernetes;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;


import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.dao.JobRepository;
import de.dlr.proseo.model.dao.JobStepRepository;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1JobBuilder;
import io.kubernetes.client.models.V1JobSpec;
import io.kubernetes.client.models.V1JobSpecBuilder;

/**
 * @author melchinger
 *
 */

@Transactional
@Component
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
	private KubeJob () {
		
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

	public KubeJob createJob(KubeConfig aKubeConfig) {	    
		if (aKubeConfig.isConnected()) {
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
				
				  JobStep js = new JobStep();
				  js.setProcessingMode(job.getMetadata().getName()); 
				  ProductionPlanner p = ProductionPlanner.getPlanner();
				  RepositoryService.getJobStepRepository().save(js);
				 
				aKubeConfig.getBatchApiV1().createNamespacedJob (aKubeConfig.getNamespace(), job, null, null, null);
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
}
