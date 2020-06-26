/**
 * JobDispatcher.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.dispatcher;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.model.ConfigurationFile;
import de.dlr.proseo.model.ConfigurationInputFile;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.Task;
import de.dlr.proseo.model.joborder.Conf;
import de.dlr.proseo.model.joborder.InputOutput;
import de.dlr.proseo.model.joborder.IpfFileName;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.joborder.Proc;
import de.dlr.proseo.model.joborder.ProcessingParameter;
import de.dlr.proseo.model.joborder.SensingTime;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.interfaces.rest.model.FsType;
import de.dlr.proseo.interfaces.rest.model.RestJoborder;

/**
 * Create Kubernetes jobs with all information needed like processor image, job order file, parameters.
 * 
 * @author Ernst Melchinger
 *
 */

/**
 * @author melchinger
 *
 */
public class JobDispatcher {
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(JobDispatcher.class);
	private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd'_'HHmmssSSSSSS").withZone(ZoneId.of("UTC"));
	
	/**
	 * The job order structure 
	 */
	private JobOrder jobOrder;
	

	/**
	 * Create a job dispatcher.
	 */
	public JobDispatcher() {
	}

	/**
	 * Create the job order of a job step
	 * 
	 * @param jobStep
	 * @return The new job order
	 */
	public JobOrder createJobOrder(JobStep jobStep) {
		// create the job order structure for a jobStep

		jobOrder = null;		
		if (jobStep != null) {
			// Find DB elements needed
			try {
				Job job = jobStep.getJob();
				if (job == null) {
					// throw new RuntimeException("Element not found");
				}
				ProcessingOrder order = job.getProcessingOrder();
				if (order == null) {
					// throw new RuntimeException("Element not found");
				}
				Set<ConfiguredProcessor> configuredProcessors = order.getRequestedConfiguredProcessors();

				ConfiguredProcessor cp;

				Product product = RepositoryService.getProductRepository().findByJobStep(jobStep);

				if (product == null) {
					// throw new RuntimeException("Element not found");
				}

				Set<ProductQuery> productQueries = jobStep.getInputProductQueries();

				jobOrder = new JobOrder();
				String processorName = jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getProcessorClass().getProcessorName();
				String version = jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getProcessorVersion();
				String stdoutLogLevel = jobStep.getStdoutLogLevel().name(); 
				String stderrLogLevel = jobStep.getStderrLogLevel().name(); 
				String isTest = jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getIsTest() == true ? "true" : "false";
				String breakpointEnable = "true";
				String processingStation = jobStep.getJob().getProcessingOrder().getMission().getName() + " " + jobStep.getJob().getProcessingFacility().getName();
				// String acquisitionStation = ""; // unknown, not to set


				Conf co = new Conf(processorName,
						version,
						stdoutLogLevel,
						stderrLogLevel,
						isTest,
						breakpointEnable,
						processingStation,
						null);
				String start = timeFormatter.format(jobStep.getJob().getStartTime());
				String stop =  timeFormatter.format(jobStep.getJob().getStopTime());
				co.setSensingTime(new SensingTime(start, stop));

				// config files 
				for (ConfigurationFile cf : jobStep.getOutputProduct().getConfiguredProcessor().getConfiguration().getConfigurationFiles()) {
					co.getConfigFileNames().add(cf.getFileName());					
				}
				// dynamic parameter
				Map<String,Parameter> dpp = jobStep.getOutputProduct().getConfiguredProcessor().getConfiguration().getDynProcParameters();
				for (String dppn : jobStep.getOutputProduct().getConfiguredProcessor().getConfiguration().getDynProcParameters().keySet()) {
					co.getDynamicProcessingParameters().add((new ProcessingParameter(dppn, dpp.get(dppn).getParameterValue())));
				}
				jobOrder.setConf(co);
				// list of ipf procs
				for (Task t : jobStep.getOutputProduct().getConfiguredProcessor().getProcessor().getTasks()) {
					Proc proc = new Proc(t.getTaskName(), t.getTaskVersion());
					// add config files
					for (ConfigurationFile cf : jobStep.getOutputProduct().getConfiguredProcessor().getConfiguration().getConfigurationFiles()) {
						InputOutput sio = new InputOutput("processing_configuration", "Physical", "Input", null);
						sio.getFileNames().add(new IpfFileName(cf.getFileName()));
						proc.getListOfInputs().add(sio);				
					}
					// add static config files first
					for (ConfigurationInputFile scf : jobStep.getOutputProduct().getConfiguredProcessor().getConfiguration().getStaticInputFiles()) {
						InputOutput sio = new InputOutput(scf.getFileType(), scf.getFileNameType(), "Input", null);
						for (String sioFName: scf.getFileNames()) {
							sio.getFileNames().add(new IpfFileName(sioFName));
						}
						proc.getListOfInputs().add(sio);
					}
					// dynamic input files calculated by input products
					for (ProductQuery pq : jobStep.getOutputProduct().getSatisfiedProductQueries()) {
						for (Product p : pq.getNewestSatisfyingProducts()) {
							addIpfIOInput(p, proc, jobStep);
						}
					}
					Product p = jobStep.getOutputProduct();
					addIpfIOOutput(p, proc, jobStep, ""); 
					jobOrder.getListOfProcs().add(proc);
				}

			} catch (Exception e) {
				e.printStackTrace();
				jobOrder = null;
			}

			// read a job order file for test purposes
			if (jobOrder != null) {
				InetAddress ip;
				try {
					ip = InetAddress.getLocalHost();
					String hostname = ip.getHostName();
					if (hostname.equalsIgnoreCase("ME580")) {
						jobOrder.writeXML("c:\\tmp\\jo" + jobStep.getId() + ".xml", true);						
					}
				} catch (UnknownHostException e) {
					// do nothing		
				}
			}
		}
		return jobOrder;
	}
	
	/**
	 * Add input file definition of product p recursively.
	 * @param p Product
	 * @param proc The Ipf_Proc
	 * @param jobStep Job step
	 */
	public void addIpfIOInput(Product p, Proc proc, JobStep jobStep) {
		if (p.getComponentProducts().isEmpty()) {
			for (ProductFile pf : p.getProductFile()) {
				InputOutput sio = new InputOutput(p.getProductClass().getProductType(), "Physical", "Input", String.valueOf(p.getId()));
				String filePath = pf.getFilePath();
				String productFilePathAndName = (null == filePath || filePath.isBlank() ? "" : filePath + "/") + pf.getProductFileName();
				sio.getFileNames().add(new IpfFileName(productFilePathAndName, pf.getStorageType().name()));
				proc.getListOfInputs().add(sio);
			}
		} else {
			for (Product sp : p.getComponentProducts()) {
				addIpfIOInput(sp, proc, jobStep);
			}
		}
	}


	/**
	 * Add output file definition of product p recursively.
	 * @param p Product
	 * @param proc The Ipf_Proc
	 * @param jobStep Job step
	 * @param baseDir Base directory path of output data in facility/storage manager
	 */
	public void addIpfIOOutput(Product p, Proc proc, JobStep jobStep, String baseDir) {
		String fnType = p.getComponentProducts().isEmpty() ? "Physical" : "Directory"; 
		InputOutput sio = new InputOutput(p.getProductClass().getProductType(), fnType, "Output", String.valueOf(p.getId()));
		String storageType = jobStep.getJob().getProcessingFacility().getDefaultStorageType().toString();
		String fn = "";
		if (p.getGenerationTime() != null) {
			fn = p.generateFilename();
			if (fnType.equals("Directory")) {
				int i = fn.lastIndexOf('.');
				if (i > 0) {
					fn = fn.substring(0, i);
				}
			}
			fn = baseDir + fn;
			sio.getFileNames().add(new IpfFileName(fn, storageType)); 
			
		} else {
			fn = baseDir + p.getProductClass().getProductType();
			sio.getFileNames().add(new IpfFileName(fn, storageType)); 
		}
		proc.getListOfOutputs().add(sio);
		for (Product sp : p.getComponentProducts()) {
			addIpfIOOutput(sp, proc, jobStep, fn + "/");
		}
	}

	/**
	 * Send the job order as Base64 string to storage manager
	 * 
	 * @param kubeConfig The processing facility used 
	 * @param jobOrder The job order file
	 * @return job order
	 */
	public JobOrder sendJobOrderToStorageManager(KubeConfig kubeConfig, JobOrder jobOrder) {
		if (logger.isTraceEnabled()) logger.trace(">>> sendJobOrderToStorageManager({}, {})", kubeConfig, jobOrder);
		
		String storageManagerUrl = kubeConfig.getStorageManagerUrl();
		
		if (null == storageManagerUrl || null == jobOrder) {
			logger.error("Insufficient data for sending job order to Storage Manager");
			return null;
		}
		
		try {
			RestTemplateBuilder rtb = new RestTemplateBuilder();
			RestTemplate restTemplate = rtb.setConnectTimeout(Duration.ofMillis(5000))
					.basicAuthentication(kubeConfig.getStorageManagerUser(), kubeConfig.getStorageManagerPassword()).build();
			String restUrl = "/joborders";
			String b64String = jobOrder.buildBase64String(true);
			RestJoborder jo = new RestJoborder();
			switch (kubeConfig.getStorageType()) {
			case S3:
				jo.setFsType(FsType.S_3);
				break;
			case POSIX:
				// fall through intended
			default:
				jo.setFsType(FsType.POSIX);
				break;					
			}
			
			jo.setJobOrderStringBase64(b64String);
			logger.info("HTTP Request: " + storageManagerUrl + restUrl);
			
			ResponseEntity<RestJoborder> response = restTemplate.postForEntity(storageManagerUrl + restUrl, jo, RestJoborder.class);

			logger.info("... response is {}", response.getStatusCode());

			if (response != null && response.getBody() != null && response.getBody().getUploaded()) {
				jobOrder.setFileName(response.getBody().getPathInfo());
			} else {
				return null;
			}		
		} catch (Exception e) {
			logger.error("Exception sending job order to Storage Manager: {}", e.getMessage());
			e.printStackTrace();
			return null;
		}
		
		return jobOrder;
	}
}
