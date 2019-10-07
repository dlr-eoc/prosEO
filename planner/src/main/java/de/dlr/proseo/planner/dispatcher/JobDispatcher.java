/**
 * JobDispatcher.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.dispatcher;

import java.util.List;
import java.util.Set;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.dao.ProductRepository;
import de.dlr.proseo.model.joborder.Conf;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Create Kubernetes jobs with all information needed like processor image, job order file, parameters.
 * 
 * @author melchinger
 *
 */
public class JobDispatcher {

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
			throw new RuntimeException("Element not found");
		}
		ProcessingOrder order = job.getProcessingOrder();
		if (order == null) {
			throw new RuntimeException("Element not found");
		}
		Set<ConfiguredProcessor> configuredProcessors = order.getRequestedConfiguredProcessors();
		
		ConfiguredProcessor cp;
		
		Product product = RepositoryService.getProductRepository().findByJobStep(jobStep);

		if (product == null) {
			throw new RuntimeException("Element not found");
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
}
