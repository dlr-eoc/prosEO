package de.dlr.proseo.planner.joborder;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import io.kubernetes.client.models.V1Pod;

/**
 * @author melchinger
 *
 * IPF_Conf information
 * 
 * For details see 
 * Generic IPF Interface Specification
 * issue 1 revision 8 - 03/08/2009
 * MMFI-GSEG-EOPG-TN-07-0003
 *  
 */
public class Conf {
	/**
	 * 
	 */
	private String processorName;
	/**
	 * 
	 */
	private String version;
	/**
	 * 
	 */
	private String stdoutLogLevel;
	/**
	 * 
	 */
	private String stderrLogLevel;
	/**
	 * 
	 */
	private String test;
	/**
	 * 
	 */
	private String breakpointEnable;
	/**
	 * 
	 */
	private String processingStation;
	/**
	 * 
	 */
	private String acquisitionStation;
	/**
	 * 
	 */
	private SensingTime sensingTime;
	/**
	 * 
	 */
	private List<String> configFileNames; 
	/**
	 * 
	 */
	private List<ProcessingParameter> dynamicProcessingParameters;
	/**
	 * @return the processorName
	 */
	public String getProcessorName() {
		return processorName;
	}
	/**
	 * @param processorName the processorName to set
	 */
	public void setProcessorName(String processorName) {
		this.processorName = processorName;
	}
	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}
	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}
	/**
	 * @return the stdoutLogLevel
	 */
	public String getStdoutLogLevel() {
		return stdoutLogLevel;
	}
	/**
	 * @param stdoutLogLevel the stdoutLogLevel to set
	 */
	public void setStdoutLogLevel(String stdoutLogLevel) {
		this.stdoutLogLevel = stdoutLogLevel;
	}
	/**
	 * @return the stderrLogLevel
	 */
	public String getStderrLogLevel() {
		return stderrLogLevel;
	}
	/**
	 * @param stderrLogLevel the stderrLogLevel to set
	 */
	public void setStderrLogLevel(String stderrLogLevel) {
		this.stderrLogLevel = stderrLogLevel;
	}
	/**
	 * @return the test
	 */
	public String getTest() {
		return test;
	}
	/**
	 * @param test the test to set
	 */
	public void setTest(String test) {
		this.test = test;
	}
	/**
	 * @return the breakpointEnable
	 */
	public String getBreakpointEnable() {
		return breakpointEnable;
	}
	/**
	 * @param breakpointEnable the breakpointEnable to set
	 */
	public void setBreakpointEnable(String breakpointEnable) {
		this.breakpointEnable = breakpointEnable;
	}
	/**
	 * @return the processingStation
	 */
	public String getProcessingStation() {
		return processingStation;
	}
	/**
	 * @param processingStation the processingStation to set
	 */
	public void setProcessingStation(String processingStation) {
		this.processingStation = processingStation;
	}
	/**
	 * @return the acquisitionStation
	 */
	public String getAcquisitionStation() {
		return acquisitionStation;
	}
	/**
	 * @param acquisitionStation the acquisitionStation to set
	 */
	public void setAcquisitionStation(String acquisitionStation) {
		this.acquisitionStation = acquisitionStation;
	}
	/**
	 * @return the sensingTime
	 */
	public SensingTime getSensingTime() {
		return sensingTime;
	}
	/**
	 * @param sensingTime the sensingTime to set
	 */
	public void setSensingTime(SensingTime sensingTime) {
		this.sensingTime = sensingTime;
	}
	/**
	 * @return the configFileNames
	 */
	public List<String> getConfigFileNames() {
		return configFileNames;
	}
	/**
	 * @param configFileNames the configFileNames to set
	 */
	public void setConfigFileNames(List<String> configFileNames) {
		this.configFileNames = configFileNames;
	}
	/**
	 * @return the dynamicProcessingParameters
	 */
	public List<ProcessingParameter> getDynamicProcessingParameters() {
		return dynamicProcessingParameters;
	}
	
	public Conf() {
		this.configFileNames = new ArrayList<String>();
		this.dynamicProcessingParameters = new ArrayList<ProcessingParameter>();
	}
	/**
	 * @param processorName
	 * @param version
	 * @param stdoutLogLevel
	 * @param stderrLogLevel
	 * @param test
	 * @param breakpointEnable
	 * @param processingStation
	 * @param acquisitionStation
	 */
	public Conf(String processorName, String version, String stdoutLogLevel, String stderrLogLevel, String test,
			String breakpointEnable, String processingStation, String acquisitionStation) {
		this.processorName = processorName;
		this.version = version;
		this.stdoutLogLevel = stdoutLogLevel;
		this.stderrLogLevel = stderrLogLevel;
		this.test = test;
		this.breakpointEnable = breakpointEnable;
		this.processingStation = processingStation;
		this.acquisitionStation = acquisitionStation;
		this.configFileNames = new ArrayList<String>();
		this.dynamicProcessingParameters = new ArrayList<ProcessingParameter>();
		this.sensingTime = null;
	}
	
	/**
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 * @param doc The Document
	 * @param parentElement The node to add this as child
	 * @prosEOAttributes if true, write attributes of prosEO specific data
	 */
	public void buildXML(Document doc, Element parentElement, Boolean prosEOAttributes) {

	    Element configEle = doc.createElement("Ipf_Conf");
	    parentElement.appendChild(configEle);
	    
        Element processorNameEle = doc.createElement("Processor_Name");
        processorNameEle.appendChild(doc.createTextNode(processorName));
        configEle.appendChild(processorNameEle);
        
        Element versionEle = doc.createElement("Version");
        versionEle.appendChild(doc.createTextNode(version));
        configEle.appendChild(versionEle);
        
        Element stdoutLogLevelEle = doc.createElement("Stdout_Log_Level");
        stdoutLogLevelEle.appendChild(doc.createTextNode(stdoutLogLevel));
        configEle.appendChild(stdoutLogLevelEle);
        
        Element stderrLogLevelEle = doc.createElement("Stderr_Log_Level");
        stderrLogLevelEle.appendChild(doc.createTextNode(stderrLogLevel));
        configEle.appendChild(stderrLogLevelEle);
        
        Element testEle = doc.createElement("Test");
        testEle.appendChild(doc.createTextNode(test));
        configEle.appendChild(testEle);
        
        Element breakpointEnableEle = doc.createElement("Breakpoint_Enable");
        breakpointEnableEle.appendChild(doc.createTextNode(breakpointEnable));
        configEle.appendChild(breakpointEnableEle);
        
        Element processingStationEle = doc.createElement("Processing_Station");
        processingStationEle.appendChild(doc.createTextNode(processingStation));
        configEle.appendChild(processingStationEle);
        
        Element acquisitionStationEle = doc.createElement("Acquisition_Station");
        acquisitionStationEle.appendChild(doc.createTextNode(acquisitionStation));
        configEle.appendChild(acquisitionStationEle);
	    
        if (sensingTime != null) {
        	sensingTime.buildXML(doc, configEle, prosEOAttributes);
        }

        Element configFilesEle = doc.createElement("Config_Files");
        configEle.appendChild(configFilesEle);
        
        Element configFileNameEle = null;
        for (String item : configFileNames) {
        	configFileNameEle =  doc.createElement("Conf_File_Name");
        	configFileNameEle.appendChild(doc.createTextNode(item));
        	configFilesEle.appendChild(configFileNameEle);
        }
        Element dynProcParamsEle = doc.createElement("Dynamic_Processing_Parameters");
        configEle.appendChild(dynProcParamsEle);
        
        for (ProcessingParameter item : dynamicProcessingParameters) {
        	item.buildXML(doc, dynProcParamsEle, prosEOAttributes);
        }
	}
	
	/**
	 * Read info from XML sub tree
	 * @param thisNode XML node containing information
	 */
	public void read(Node thisNode) {
		if (thisNode != null) {
			Node child = thisNode.getFirstChild();
			while (child != null) {
				switch (child.getNodeName().toLowerCase()) {
				case "processor_name" : 
					this.setProcessorName(child.getTextContent().strip());
					break;
				case "version" : 
					this.setVersion(child.getTextContent().strip());
					break;
				case "stdout_log_level" : 
					this.setStdoutLogLevel(child.getTextContent().strip());
					break;
				case "stderr_log_level" : 
					this.setStderrLogLevel(child.getTextContent().strip());
					break;
				case "test" : 
					this.setTest(child.getTextContent().strip());
					break;
				case "breakpoint_enable" : 
					this.setBreakpointEnable(child.getTextContent().strip());
					break;
				case "processing_station" : 
					this.setProcessingStation(child.getTextContent().strip());
					break;
				case "acquisition_station" : 
					this.setAcquisitionStation(child.getTextContent().strip());
					break;
				case "sensing_time" : 
					this.setSensingTime(new SensingTime());
					this.getSensingTime().read(child);
					break;
				case "config_files" : 
					Node cfn = child.getFirstChild();
					while (cfn != null) {
						this.getConfigFileNames().add(cfn.getTextContent().strip());
						cfn = cfn.getNextSibling();
					}						
					break;
				case "dynamic_processing_parameters" : 
					Node dpp = child.getFirstChild();
					while (dpp != null) {
						ProcessingParameter pp = new ProcessingParameter();
						pp.read(dpp);
						this.getDynamicProcessingParameters().add(pp);
						dpp = dpp.getNextSibling();
					}						
					break;
				}
				child = child.getNextSibling();
			}
		}		
	}
}
