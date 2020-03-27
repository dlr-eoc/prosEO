/**
 * JobDispatcher.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.dispatcher;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import de.dlr.proseo.interfaces.rest.model.RestJoborder;

/**
 * Create Kubernetes jobs with all information needed like processor image, job order file, parameters.
 * 
 * @author Ernst Melchinger
 *
 */

public class JobDispatcher {
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(JobDispatcher.class);
	private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd'_'HHmmssSSSSSS").withZone(ZoneId.of("UTC"));
	
	private JobOrder jobOrder;
	

	/**
	 * Create a job dispatcher.
	 */
	public JobDispatcher() {
	}

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
							for (ProductFile pf : p.getProductFile()) {
								InputOutput sio = new InputOutput(p.getProductClass().getProductType(), "Physical", "Input", String.valueOf(p.getId()));
								sio.getFileNames().add(new IpfFileName(pf.getProductFilePathName(), pf.getStorageType().name()));
								proc.getListOfInputs().add(sio);
							}
						}
					}
					Product p = jobStep.getOutputProduct();
					addIpfIOOutput(p, proc, jobStep); 
					jobOrder.getListOfProcs().add(proc);
				}

			} catch (Exception e) {
				e.printStackTrace();
				jobOrder = null;
			}

			// read a job order file for test purposes
			if (jobOrder != null) {
				jobOrder.writeXML("c:\\tmp\\jo" + jobStep.getId() + ".xml", true);
			}
		}
		return jobOrder;
	}
	
	public void addIpfIOOutput(Product p, Proc proc, JobStep jobStep) {
		String fnType = p.getComponentProducts().isEmpty() ? "Physical" : "Directory"; 
		InputOutput sio = new InputOutput(p.getProductClass().getProductType(), fnType, "Output", String.valueOf(p.getId()));
		if (p.getGenerationTime() != null) {
			sio.getFileNames().add(new IpfFileName(p.generateFilename(), "S3")); // p.getJobStep().getJob().getProcessingFacility().getDefaultFSType()));
//			sio.getFileNames().add(new IpfFileName("s3:/proseo-data-001/output/" + jobStep.getId() + "/" + p.generateFilename(), "S3")); // p.getJobStep().getJob().getProcessingFacility().getDefaultFSType()));
		} else {
			sio.getFileNames().add(new IpfFileName(p.getProductClass().getProductType(), "S3")); // p.getJobStep().getJob().getProcessingFacility().getDefaultFSType()));
		}
		proc.getListOfOutputs().add(sio);
		for (Product sp : p.getComponentProducts()) {
			addIpfIOOutput(sp, proc, jobStep);
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
		
		String storageManagerUrl = kubeConfig.getStorageManagerUrl();
		
		if (storageManagerUrl != null && jobOrder != null) {
			try {
				RestTemplate restTemplate = new RestTemplate();
				String restUrl = "/proseo/storage-mgr/v0.1/joborders";
				String b64String = jobOrder.buildBase64String(true);
				RestJoborder jo = new RestJoborder();
				jo.setJobOrderStringBase64(b64String);
				logger.info("HTTP Request: " + storageManagerUrl + restUrl);
				
				ResponseEntity<RestJoborder> response = restTemplate.postForEntity(storageManagerUrl + restUrl, jo, RestJoborder.class);

				logger.info("... response is {}", response.getStatusCode());

				if (response != null && response.getBody() != null && response.getBody().getUploaded()) {
					jobOrder.setFileName(response.getBody().getPathInfo());
					jobOrder.setFsType(response.getBody().getFsType().value());
				} else {
					return null;
				}		
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}			
		
		return jobOrder;
	}
}


/*
 
 
InputStream aStream;
try {
	aStream = new com.amazonaws.util.StringInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n" + 
			"<Ipf_Job_Order>\r\n" + 
			"  <Ipf_Conf>\r\n" + 
			"    <Processor_Name>CO____</Processor_Name>\r\n" + 
			"    <Version>01.03.01</Version>\r\n" + 
			"    <Stdout_Log_Level>INFO</Stdout_Log_Level>\r\n" + 
			"    <Stderr_Log_Level>WARNING</Stderr_Log_Level>\r\n" + 
			"    <Test>false</Test>\r\n" + 
			"    <Breakpoint_Enable>true</Breakpoint_Enable>\r\n" + 
			"    <Processing_Station>PDGS-OP</Processing_Station>\r\n" + 
			"    <Acquisition_Station>PDGS-GSN</Acquisition_Station>\r\n" + 
			"    <Sensing_Time>\r\n" + 
			"      <Start>20180105_075307000000</Start>\r\n" + 
			"      <Stop>20180105_093437000000</Stop>\r\n" + 
			"    </Sensing_Time>\r\n" + 
			"    <Config_Files/>\r\n" + 
			"    <Dynamic_Processing_Parameters>\r\n" + 
			"      <Processing_Parameter>\r\n" + 
			"        <Name>logging.root</Name>\r\n" + 
			"        <Value>notice</Value>\r\n" + 
			"      </Processing_Parameter>\r\n" + 
			"      <Processing_Parameter>\r\n" + 
			"        <Name>logging.dumplog</Name>\r\n" + 
			"        <Value>null</Value>\r\n" + 
			"      </Processing_Parameter>\r\n" + 
			"      <Processing_Parameter>\r\n" + 
			"        <Name>Threads</Name>\r\n" + 
			"        <Value>9</Value>\r\n" + 
			"      </Processing_Parameter>\r\n" + 
			"      <Processing_Parameter>\r\n" + 
			"        <Name>orbit number</Name>\r\n" + 
			"        <Value>01191</Value>\r\n" + 
			"      </Processing_Parameter>\r\n" + 
			"      <Processing_Parameter>\r\n" + 
			"        <Name>Processing_Mode</Name>\r\n" + 
			"        <Value>OFFL</Value>\r\n" + 
			"      </Processing_Parameter>\r\n" + 
			"      <Processing_Parameter>\r\n" + 
			"        <Name>Deadline_Time</Name>\r\n" + 
			"        <Value>20190425_163530000000</Value>\r\n" + 
			"      </Processing_Parameter>\r\n" + 
			"    </Dynamic_Processing_Parameters>\r\n" + 
			"  </Ipf_Conf>\r\n" + 
			"  <List_of_Ipf_Procs count=\"1\">\r\n" + 
			"    <Ipf_Proc>\r\n" + 
			"      <Task_Name>TROPNLL2DP</Task_Name>\r\n" + 
			"      <Task_Version>01.03.01</Task_Version>\r\n" + 
			"      <List_of_Inputs count=\"18\">\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>CFG_CO____</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"ALLUXIO\">/alluxio1/3244232/file01.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>CFG_CO___F</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"ALLUXIO\">/alluxio1/3244232/file02.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>REF_SOLAR_</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file03.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>REF_XS__CO</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file04.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>AUX_ISRF__</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file05.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>AUX_CTMCH4</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"3\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file06.txt</File_Name>\r\n" + 
			"            <File_Name FS_TYPE=\"ALLUXIO\">/alluxio1/3244232/file07.txt</File_Name>\r\n" + 
			"            <File_Name FS_TYPE=\"ALLUXIO\">/alluxio1/3244232/file08.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>L1B_IR_SIR</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file09.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>L1B_RA_BD1</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file10.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>L1B_RA_BD2</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file11.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>L1B_RA_BD3</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file12.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>L1B_RA_BD4</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file13.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>L1B_RA_BD5</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file14.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>L1B_RA_BD6</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file15.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>L1B_RA_BD7</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file16.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>L1B_RA_BD8</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file16.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>AUX_MET_TP</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file16.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>AUX_MET_2D</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file17.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"        <Input>\r\n" + 
			"          <File_Type>AUX_MET_QP</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">s3://s3test/3244233/file18.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Input>\r\n" + 
			"      </List_of_Inputs>\r\n" + 
			"      <List_of_Outputs count=\"3\">\r\n" + 
			"        <Output Product_ID=\"7397129831\">\r\n" + 
			"          <File_Type>L2__CO____</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"ALLUXIO\">results_A/result01.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Output>\r\n" + 
			"        <Output Product_ID=\"7397129832\">\r\n" + 
			"          <File_Type>L2__CO____</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"ALLUXIO\">results_B/result02.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Output>\r\n" + 
			"        <Output Product_ID=\"7397129833\">\r\n" + 
			"          <File_Type>L2__CO____</File_Type>\r\n" + 
			"          <File_Name_Type>PHYSICAL</File_Name_Type>\r\n" + 
			"          <List_of_File_Names count=\"1\">\r\n" + 
			"            <File_Name FS_TYPE=\"S3\">results_C/result03.txt</File_Name>\r\n" + 
			"          </List_of_File_Names>\r\n" + 
			"        </Output>\r\n" + 
			"      </List_of_Outputs>\r\n" + 
			"    </Ipf_Proc>\r\n" + 
			"  </List_of_Ipf_Procs>\r\n" + 
			"</Ipf_Job_Order>");
	jobOrder.readFromStream(aStream);
} catch (UnsupportedEncodingException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

*/