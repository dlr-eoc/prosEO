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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.model.ConfigurationFile;
import de.dlr.proseo.model.ConfigurationInputFile;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
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
import de.dlr.proseo.model.joborder.TimeInterval;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.interfaces.rest.model.RestJoborder;
import de.dlr.proseo.model.enums.JobOrderVersion;
import de.dlr.proseo.model.enums.StorageType;

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
		if (logger.isTraceEnabled()) logger.trace(">>> createJobOrder({})", jobStep.getId());

		// create the job order structure for a jobStep

		jobOrder = null;		
		if (jobStep != null) {
			// Find DB elements needed
			try {
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
					// add static input files first
					for (ConfigurationInputFile scf : jobStep.getOutputProduct().getConfiguredProcessor().getConfiguration().getStaticInputFiles()) {
						InputOutput sio = new InputOutput(scf.getFileType(), scf.getFileNameType(), InputOutput.IO_TYPE_INPUT, null);
						for (String sioFName: scf.getFileNames()) {
							sio.getFileNames().add(new IpfFileName(sioFName));
						}
						proc.getListOfInputs().add(sio);
					}
					// dynamic input files calculated by input products
					// create IpfInput for each product class
					Map<ProductClass, List<Product>> productClasses = new HashMap<ProductClass, List<Product>>();
					for (ProductQuery pq : jobStep.getInputProductQueries()) {
						// Replaced "getNewestSatisfyingProducts" by "getSatisfyingProducts" --> older ones are allowed!
						// If this is a problem with some test cases, check test cases
						for (Product p : pq.getSatisfyingProducts()) {
							addProductToMap(p, productClasses);
						}
					}
					addIpfIOInput(productClasses, proc, jobStep, t.getProcessor().getUseInputFileTimeIntervals());
					Product p = jobStep.getOutputProduct();
					addIpfIOOutput(p, proc, jobStep, ""); 
					jobOrder.getListOfProcs().add(proc);
				}

			} catch (Exception e) {
				e.printStackTrace();
				jobOrder = null;
			}

			// write a job order file for test purposes
			if (jobOrder != null) {
				InetAddress ip;
				try {
					ip = InetAddress.getLocalHost();
					String hostname = ip.getHostName();
					if (hostname.equalsIgnoreCase("ME580")) {
						jobOrder.writeXML("c:\\tmp\\jo" + jobStep.getId() + ".xml", JobOrderVersion.MMFI_1_8, true);						
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
	 * @param useTimeIntervals if true, generates TimeInterval elements to the input definition
	 */
	public void addIpfIOInput(Product p, Proc proc, JobStep jobStep, Boolean useTimeintervals) {
		if (logger.isTraceEnabled()) logger.trace(">>> addIpfIOInput({}, {}, {}, {})", p.getId(), proc.getTaskName(), jobStep.getId(), useTimeintervals);

		if (p.getComponentProducts().isEmpty()) {
			for (ProductFile pf : p.getProductFile()) {
				InputOutput sio = new InputOutput(p.getProductClass().getProductType(), InputOutput.FN_TYPE_PHYSICAL, 
						InputOutput.IO_TYPE_INPUT, String.valueOf(p.getId()));
				String filePath = pf.getFilePath();
				String productFilePathAndName = (null == filePath || filePath.isBlank() ? "" : filePath + "/") + pf.getProductFileName();
				sio.getFileNames().add(new IpfFileName(productFilePathAndName, pf.getStorageType().name()));
				if (useTimeintervals) {
					TimeInterval ti = new TimeInterval(
						timeFormatter.format(p.getSensingStartTime()),
						timeFormatter.format(p.getSensingStopTime()),
						productFilePathAndName);
					sio.getTimeIntervals().add(ti);
				}
				proc.getListOfInputs().add(sio);
				if (logger.isTraceEnabled()) logger.trace("... added product {} to input files", p.getId());
			}
		} else {
			for (Product sp : p.getComponentProducts()) {
				addIpfIOInput(sp, proc, jobStep, useTimeintervals);
			}
		}
	}


	/**
	 * Add input file definition of products recursively.
	 * @param productClasses a map of products accessible by product class
	 * @param proc the Ipf_Proc element to add the input to
	 * @param jobStep the job step, for which the Job Order is generated
	 * @param useTimeIntervals if true, generates TimeInterval elements to the input definition
	 */
	public void addIpfIOInput(Map<ProductClass, List<Product>> productClasses, Proc proc, JobStep jobStep, Boolean useTimeintervals) {
		if (logger.isTraceEnabled()) logger.trace(">>> addIpfIOInput(<...>, {}, {}, {})", proc.getTaskName(), jobStep.getId(), useTimeintervals);

		for (ProductClass pc : productClasses.keySet()) {
			InputOutput sio = new InputOutput(pc.getProductType(), InputOutput.FN_TYPE_PHYSICAL, InputOutput.IO_TYPE_INPUT, null);
			for (Product p : productClasses.get(pc)) {
				if (p.getComponentProducts().isEmpty()) {
					for (ProductFile pf : p.getProductFile()) {
						String filePath = pf.getFilePath();
						String productFilePathAndName = (null == filePath || filePath.isBlank() ? "" : filePath + "/") + pf.getProductFileName();
						if (useTimeintervals) {
							TimeInterval ti = new TimeInterval(
									timeFormatter.format(p.getSensingStartTime()),
									timeFormatter.format(p.getSensingStopTime()),
									productFilePathAndName);
							sio.getTimeIntervals().add(ti);
						}
						sio.getFileNames().add(new IpfFileName(productFilePathAndName, pf.getStorageType().name()));
						if (logger.isTraceEnabled()) logger.trace("... added product {} to input files", p.getId());
					}
				} else {
					// not possible, because the product structure is flattened by addProductToMap
					if (logger.isTraceEnabled()) logger.trace("... in 'impossible' location for product {}", p.getId());
				}
			}
			proc.getListOfInputs().add(sio);
		}
	}
	
	/**
	 * Add the product to map productClasses. Collect all products of same class into one map element.
	 * Don't add products with components (these products are seen as directory and are not handled by wrapper/processor.
	 * 
	 * @param p The Product
	 * @param productClasses The map
	 */
	private void addProductToMap(Product p, Map<ProductClass, List<Product>> productClasses) {
		if (logger.isTraceEnabled()) logger.trace(">>> addProductToMap({}, [...])", (null == p ? "null" : p.getId()));

		if (p.getComponentProducts().isEmpty()) {
			if (productClasses.containsKey(p.getProductClass())) {
				productClasses.get(p.getProductClass()).add(p);
			} else {
				List<Product> plist = new ArrayList<Product>();
				plist.add(p);
				productClasses.put(p.getProductClass(), plist);
			}
			if (logger.isTraceEnabled()) logger.trace("... added!");
		} else {
			for (Product sp : p.getComponentProducts()) {
				addProductToMap(sp, productClasses);
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
		if (logger.isTraceEnabled()) logger.trace(">>> addIpfIOOutput({}, {}, {}, {})", p.getId(), proc.getTaskName(), jobStep.getId(), baseDir);

		String fnType = p.getComponentProducts().isEmpty() ? InputOutput.FN_TYPE_PHYSICAL : InputOutput.FN_TYPE_DIRECTORY; 
		InputOutput sio = new InputOutput(p.getProductClass().getProductType(), fnType, InputOutput.IO_TYPE_OUTPUT, String.valueOf(p.getId()));
		String storageType = jobStep.getJob().getProcessingFacility().getDefaultStorageType().toString();
		String fn = "";
		if (p.getGenerationTime() != null) {
			fn = p.generateFilename();
			if (fnType.equals(InputOutput.FN_TYPE_DIRECTORY)) {
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
	 * @param jobOrderVersion the Job Order file specification version to apply
	 * @return job order
	 */
	public JobOrder sendJobOrderToStorageManager(KubeConfig kubeConfig, JobOrder jobOrder, JobOrderVersion jobOrderVersion) {
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
			String b64String = jobOrder.buildBase64String(jobOrderVersion, true);
			RestJoborder jo = new RestJoborder();
			switch (kubeConfig.getStorageType()) {
			case S3:
				jo.setFsType(StorageType.S3.toString());
				break;
			case POSIX:
				// fall through intended
			default:
				jo.setFsType(StorageType.POSIX.toString());
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
