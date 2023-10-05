/**
 * Conf.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.joborder;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.dlr.proseo.model.enums.JobOrderVersion;

/**
 * Representation of the Ipf_Conf Job Order element
 *
 * For details see Generic IPF Interface Specification issue 1 revision 8 -
 * 03/08/2009 MMFI-GSEG-EOPG-TN-07-0003
 *
 * @author Ernst Melchinger
 */
public class Conf {

	/** Job Order element Processor_Name */
	private String processorName;

	/** Job Order element Version */
	private String version;

	/** Job Order element Stdout_Log_Level */
	private String stdoutLogLevel;

	/** Job Order element Stderr_Log_Level */
	private String stderrLogLevel;

	/** Job Order element Test */
	private String test;

	/** Job Order element Breakpoint_Enable */
	private String breakpointEnable;

	/** Job Order element Acquisition_Station */
	private String acquisitionStation;

	/** Job Order element Processing_Station */
	private String processingStation;

	/** Job Order elements Config_Files / Config_File_Name */
	private List<String> configFileNames = new ArrayList<>();

	/** Job Order element Sensing_Time */
	private SensingTime sensingTime;

	/** Job Order elements Dynamic_Processing_Parameters / Processing_Parameter */
	private List<ProcessingParameter> dynamicProcessingParameters = new ArrayList<>();

	/**
	 * Thrown when a request to a unique named processing parameter is executed and
	 * there is more than one parameter with the given name.
	 *
	 * The exception class name is borrowed from
	 * javax.persistence.NonUniqueResultException.
	 */
	public static class NonUniqueResultException extends RuntimeException {
		private static final long serialVersionUID = -1717642966866290514L;

		public NonUniqueResultException() {
			super();
		}

		public NonUniqueResultException(String message) {
			super(message);
		}

		public NonUniqueResultException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/**
	 * Gets the Processor_Name element
	 *
	 * @return the processor name
	 */
	public String getProcessorName() {
		return processorName;
	}

	/**
	 * Sets the Processor_Name element
	 *
	 * @param processorName the processor name to set
	 */
	public void setProcessorName(String processorName) {
		this.processorName = processorName;
	}

	/**
	 * Gets the Version element
	 *
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the Version element
	 *
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Gets the Stdout_Log_Level element
	 *
	 * @return the log level for stdout
	 */
	public String getStdoutLogLevel() {
		return stdoutLogLevel;
	}

	/**
	 * Sets the Stdout_Log_Level element
	 *
	 * @param stdoutLogLevel the log level for stdout to set
	 */
	public void setStdoutLogLevel(String stdoutLogLevel) {
		this.stdoutLogLevel = stdoutLogLevel;
	}

	/**
	 * Gets the Stderr_Log_Level element
	 *
	 * @return the log level for stderr
	 */
	public String getStderrLogLevel() {
		return stderrLogLevel;
	}

	/**
	 * Sets the Stderr_Log_Level element
	 *
	 * @param stderrLogLevel the log level for stderr to set
	 */
	public void setStderrLogLevel(String stderrLogLevel) {
		this.stderrLogLevel = stderrLogLevel;
	}

	/**
	 * Gets the Test element
	 *
	 * @return the test flag
	 */
	public String getTest() {
		return test;
	}

	/**
	 * Sets the Test element
	 *
	 * @param test the test flag to set ("true" or "false")
	 */
	public void setTest(String test) {
		this.test = test;
	}

	/**
	 * Gets the Breakpoint_Enable element
	 *
	 * @return the breakpoint enablement flag
	 */
	public String getBreakpointEnable() {
		return breakpointEnable;
	}

	/**
	 * Sets the Breakpoint_Enable element
	 *
	 * @param breakpointEnable the breakpoint enablement flag to set ("true" or
	 *                         "false")
	 */
	public void setBreakpointEnable(String breakpointEnable) {
		this.breakpointEnable = breakpointEnable;
	}

	/**
	 * Gets the Processing_Station element
	 *
	 * @return the processing station
	 */
	public String getProcessingStation() {
		return processingStation;
	}

	/**
	 * Sets the Processing_Station element
	 *
	 * @param processingStation the processing station to set
	 */
	public void setProcessingStation(String processingStation) {
		this.processingStation = processingStation;
	}

	/**
	 * Gets the Acquisition_Station element
	 *
	 * @return the acquisition station
	 */
	public String getAcquisitionStation() {
		return acquisitionStation;
	}

	/**
	 * Sets the Acquisition_Station element
	 *
	 * @param acquisitionStation the acquisition station to set
	 */
	public void setAcquisitionStation(String acquisitionStation) {
		this.acquisitionStation = acquisitionStation;
	}

	/**
	 * Gets the Sensing_Time element
	 *
	 * @return the sensing time
	 */
	public SensingTime getSensingTime() {
		return sensingTime;
	}

	/**
	 * Sets the Sensing_Time element
	 *
	 * @param sensingTime the sensing time to set
	 */
	public void setSensingTime(SensingTime sensingTime) {
		this.sensingTime = sensingTime;
	}

	/**
	 * Gets the Config_File_Name values for the Config_Files element
	 *
	 * @return the configuration file names
	 */
	public List<String> getConfigFileNames() {
		return configFileNames;
	}

	/**
	 * Sets the Config_File_Name values for the Config_Files element
	 *
	 * @param configFileNames the configuration file names to set
	 */
	public void setConfigFileNames(List<String> configFileNames) {
		this.configFileNames = configFileNames;
	}

	/**
	 * Gets the Processing_Parameter values for the Dynamic_Processing_Parameters
	 * element
	 *
	 * @return the dynamic processing parameters
	 */
	public List<ProcessingParameter> getDynamicProcessingParameters() {
		return dynamicProcessingParameters;
	}
	
	/**
	 * Sets the Processing_Parameter values for the Dynamic_Processing_Parameters
	 * element
	 *
	 * @param dynamicProcessingParameters the dynamic processing parameters to set
	 */
	public void setDynamicProcessingParameters(List<ProcessingParameter> dynamicProcessingParameters) {
		this.dynamicProcessingParameters = dynamicProcessingParameters;
	}

	/**
	 * Gets the Processing_Parameter values for the given key (ignoring case)
	 *
	 * @param name the key to search for
	 * @return a list of processing parameters with the given key
	 */
	public List<ProcessingParameter> getProcessingParametersByName(String name) {
		List<ProcessingParameter> result = new ArrayList<>();

		for (ProcessingParameter param : dynamicProcessingParameters) {
			if (param.getName().equalsIgnoreCase(name)) {
				result.add(param);
			}
		}

		return result;
	}

	/**
	 * Gets the unique Processing_parameter for the given key (ignoring case)
	 *
	 * @param name the key to search for
	 * @return the single processing parameter matching the key, or null if no such
	 *         parameter exists
	 * @throws NonUniqueResultException if more than one parameter with the given
	 *                                  name exists
	 */
	public ProcessingParameter getUniqueProcessingParameterByName(String name) throws NonUniqueResultException {
		List<ProcessingParameter> result = getProcessingParametersByName(name);

		if (result.isEmpty()) {
			return null;
		} else if (1 == result.size()) {
			return result.get(0);
		} else {
			throw new NonUniqueResultException("More than one processing parameter with name " + name);
		}
	}

	/**
	 * Sets the unique Processing_parameter for the given key (ignoring case) to the
	 * given value (parameter will be created, if necessary)
	 *
	 * @param name  the key of the parameter to set
	 * @param value the new parameter value
	 * @throws NonUniqueResultException if more than one parameter with the given
	 *                                  name exists
	 */
	public void setProcessingParameterByName(String name, String value) throws NonUniqueResultException {
		ProcessingParameter param = getUniqueProcessingParameterByName(name);

		if (null == param) {
			param = new ProcessingParameter(name, value);
			dynamicProcessingParameters.add(param);
		} else {
			param.setValue(value);
		}
	}

	/**
	 * No-argument constructor, initializes the configuration file names and the
	 * dynamic processing parameters
	 */
	public Conf() {
	}

	/**
	 * Constructor with arguments for all scalar attributes
	 *
	 * @param processorName      the processor name to set
	 * @param version            the version to set
	 * @param stdoutLogLevel     the log level for stdout to set
	 * @param stderrLogLevel     the log level for stderr to set
	 * @param test               the test flag to set ("true" or "false")
	 * @param breakpointEnable   the breakpoint enablement flag to set ("true" or
	 *                           "false")
	 * @param processingStation  the processing station to set
	 * @param acquisitionStation the acquisition station to set
	 */
	public Conf(String processorName, String version, String stdoutLogLevel, String stderrLogLevel, String test,
			String breakpointEnable, String processingStation, String acquisitionStation) {
		this.processorName = processorName;
		this.version = version;
		this.stdoutLogLevel = stdoutLogLevel;
		this.stderrLogLevel = stderrLogLevel;
		this.test = test;
		this.breakpointEnable = breakpointEnable;
		this.acquisitionStation = acquisitionStation;
		this.processingStation = processingStation;
		this.configFileNames = new ArrayList<>();
		this.sensingTime = null;
		this.dynamicProcessingParameters = new ArrayList<>();
	}

	/**
	 * Add contents of this to XML node parentElement. Use doc to create elements
	 *
	 * @param doc              the Document to use
	 * @param parentElement    the node to add this as child to
	 * @param jobOrderVersion  the Job Order file specification version to apply
	 * @param prosEOAttributes if true, write attributes of prosEO specific data
	 */
	public void buildXML(Document doc, Element parentElement, JobOrderVersion jobOrderVersion, Boolean prosEOAttributes) {

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

		if (acquisitionStation != null && !acquisitionStation.isBlank()) {
			Element acquisitionStationEle = doc.createElement("Acquisition_Station");
			acquisitionStationEle.appendChild(doc.createTextNode(acquisitionStation));
			configEle.appendChild(acquisitionStationEle);
		}

		Element processingStationEle = doc.createElement("Processing_Station");
		processingStationEle.appendChild(doc.createTextNode(processingStation));
		configEle.appendChild(processingStationEle);

		Element configFilesEle = doc
			.createElement(jobOrderVersion == JobOrderVersion.MMFI_1_8 ? "Config_Files" : "List_of_Config_Files");
		configEle.appendChild(configFilesEle);

		Element configFileNameEle = null;
		for (String item : configFileNames) {
			configFileNameEle = doc.createElement(jobOrderVersion == JobOrderVersion.MMFI_1_8 ? "Conf_File_Name" : "Config_File");
			configFileNameEle.appendChild(doc.createTextNode(item));
			configFilesEle.appendChild(configFileNameEle);
		}

		if (sensingTime != null) {
			sensingTime.buildXML(doc, configEle, prosEOAttributes);
		}

		Element dynProcParamsEle = doc.createElement(jobOrderVersion == JobOrderVersion.MMFI_1_8 ? "Dynamic_Processing_Parameters"
				: "List_of_Dyn_Processing_Parameters");
		configEle.appendChild(dynProcParamsEle);

		for (ProcessingParameter item : dynamicProcessingParameters) {
			item.buildXML(doc, dynProcParamsEle, jobOrderVersion, prosEOAttributes);
		}
	}

	/**
	 * Read info from XML sub tree
	 *
	 * @param thisNode XML node containing information
	 */
	public void read(Node thisNode) {
		if (thisNode != null) {
			Node child = thisNode.getFirstChild();
			while (child != null) {
				switch (child.getNodeName().toLowerCase()) {
				case "processor_name":
					this.setProcessorName(child.getTextContent().strip());
					break;
				case "version":
					this.setVersion(child.getTextContent().strip());
					break;
				case "stdout_log_level":
					this.setStdoutLogLevel(child.getTextContent().strip());
					break;
				case "stderr_log_level":
					this.setStderrLogLevel(child.getTextContent().strip());
					break;
				case "test":
					this.setTest(child.getTextContent().strip());
					break;
				case "breakpoint_enable":
					this.setBreakpointEnable(child.getTextContent().strip());
					break;
				case "processing_station":
					this.setProcessingStation(child.getTextContent().strip());
					break;
				case "acquisition_station":
					this.setAcquisitionStation(child.getTextContent().strip());
					break;
				case "sensing_time":
					this.setSensingTime(new SensingTime());
					this.getSensingTime().read(child);
					break;
				case "config_files":
				case "list_of_config_files":
					Node cfn = child.getFirstChild();
					while (cfn != null) {
						this.getConfigFileNames().add(cfn.getTextContent().strip());
						cfn = cfn.getNextSibling();
					}
					break;
				case "dynamic_processing_parameters":
				case "list_of_dyn_processing_parameters":
				case "list_of_dynamic_processing_parameters":
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