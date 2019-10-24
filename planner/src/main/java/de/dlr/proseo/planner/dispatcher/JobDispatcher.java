/**
 * JobDispatcher.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.dispatcher;

import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Set;

import org.codehaus.plexus.util.StringInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.joborder.Conf;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.storagemgr.rest.model.Joborder;
import de.dlr.proseo.storagemgr.rest.model.ProcFacility;

/**
 * Create Kubernetes jobs with all information needed like processor image, job order file, parameters.
 * 
 * @author melchinger
 *
 */
public class JobDispatcher {
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(JobDispatcher.class);

	private JobOrder jobOrder;
	
	
	/**
	 * 
	 */
	public JobDispatcher() {
		// TODO Auto-generated constructor stub
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
				String processorName;
				String version;
				String stdoutLogLevel;
				String stderrLogLevel;
				String isTest;
				String breakpointEnable;
				String processingStation;
				String acquisitionStation;


				Conf co = new Conf("Hugo", "0.0", "INFO", "WARNING", "false", "false", "PDGS-NP", "PDGS-GSN");
			} catch (Exception e) {

			}

		// read a job order file for test purposes
		if (jobOrder == null) {
			jobOrder = new JobOrder();
		}
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
		
/*		
	  <Ipf_Conf>
	    <Processor_Name>sample-processor</Processor_Name>
	    <Version>00.00.01</Version>
	    <Stdout_Log_Level>INFO</Stdout_Log_Level>
	    <Stderr_Log_Level>WARNING</Stderr_Log_Level>
	    <Test>false</Test>
	    <Breakpoint_Enable>true</Breakpoint_Enable>
	    <Processing_Station>PDGS-OP</Processing_Station>
	    <Acquisition_Station>PDGS-GSN</Acquisition_Station>
	    <Sensing_Time>
	      <Start>00000000_000000000000</Start>
	      <Stop>99999999_999999999999</Stop>
	    </Sensing_Time>
	    <Config_Files>
	      <Conf_File_Name>EMPTY</Conf_File_Name>
	    </Config_Files>
	    <Dynamic_Processing_Parameters>
	      <Processing_Parameter>
	        <Name>logging.root</Name>
	        <Value>notice</Value>
	      </Processing_Parameter>
	      <Processing_Parameter>
	        <Name>logging.dumplog</Name>
	        <Value>null</Value>
	      </Processing_Parameter>
	      <Processing_Parameter>
	        <Name>Threads</Name>
	        <Value>9</Value>
	      </Processing_Parameter>
	      <Processing_Parameter>
	        <Name>orbit number</Name>
	        <Value>01191</Value>
	      </Processing_Parameter>
	      <Processing_Parameter>
	        <Name>Processing_Mode</Name>
	        <Value>OFFL</Value>
	      </Processing_Parameter>
	      <Processing_Parameter>
	        <Name>Deadline_Time</Name>
	        <Value>20190425_161433000000</Value>
	      </Processing_Parameter>
	    </Dynamic_Processing_Parameters>
	  </Ipf_Conf>
	  */
		
		// build list of Ipf_Procs
		
		// find input files by using product queries
		
		// find output files using product
		
		
		}
		return jobOrder;
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
				String pfRestUrl = "/proseo/storage-mgr/v0.1/processingFacility";
				
				RestTemplate restTemplate = new RestTemplate();
				ResponseEntity<ProcFacility> pf = restTemplate.getForEntity(storageManagerUrl + pfRestUrl, ProcFacility.class);

				logger.info("... response is {}",pf.getBody().toString());

				String stMgrPf = pf.getBody().getName();
				pfRestUrl = "/proseo/storage-mgr/v0.1/" + stMgrPf + "/joborders";
				String b64String = jobOrder.buildBase64String(true);
				Joborder jo = new Joborder();
				jo.setJobOrderStringBase64(b64String);
				ResponseEntity<Joborder> response = restTemplate.postForEntity(storageManagerUrl + pfRestUrl, jo, Joborder.class);

				logger.info("... response is {}", response.getStatusCode());

				if (response.getBody().getUploaded()) {
					jobOrder.setFileName(response.getBody().getPathInfo());
					jobOrder.setFsType(response.getBody().getFsType().value());
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
