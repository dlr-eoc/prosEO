/**
 * Configuration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * A specific processor configuration, tied to a specific ConfiguredProcessor object. It mainly consists of a set of configuration
 * files and template information for creating Job Order files for the associated processor.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class Configuration extends PersistentObject {
	
	/** The processor class this configuration version belongs to */
	@ManyToOne
	private ProcessorClass processorClass;

	/** The configured processors, for which this configuration is valid */
	@OneToMany(mappedBy = "configuration")
	private Set<ConfiguredProcessor> configuredProcessors = new HashSet<>();
	
	/**
	 * Version identification of the configuration environment (level 1 "Version" from Generic IPF Interface Specifications,
	 * sec. 4.1.3; but may be different from Processor version)
	 */
	private String configurationVersion;
	
	/**
	 * Dynamic processing parameters, the values denoting default values, which may be changed by the Job Order File generator
	 * ("List_of_Dyn_ProcParam" from Generic IPF Interface Specifications, sec. 4.1.3).
	 */
	@ElementCollection
	private Map<String, Parameter> dynProcParameters = new HashMap<>();
	
	/** The configuration files for this configuration */
	@ElementCollection
	private Set<ConfigurationFile> configurationFiles = new HashSet<>();

	/**
	 * Gets the associated processor class
	 * 
	 * @return the configuredProcessor
	 */
	public ProcessorClass getProcessorClass() {
		return processorClass;
	}

	/**
	 * Sets the associated processor class
	 * 
	 * @param processorClass the processorClass to set
	 */
	public void setProcessorClass(ProcessorClass processorClass) {
		this.processorClass = processorClass;
	}

	/**
	 * Gets the associated configured processors
	 * 
	 * @return the configuredProcessor
	 */
	public Set<ConfiguredProcessor> getConfiguredProcessors() {
		return configuredProcessors;
	}

	/**
	 * Sets the associated configured processors
	 * 
	 * @param configuredProcessors the configuredProcessors to set
	 */
	public void setConfiguredProcessors(Set<ConfiguredProcessor> configuredProcessors) {
		this.configuredProcessors = configuredProcessors;
	}

	/**
	 * Gets the configuration version
	 * 
	 * @return the configurationVersion
	 */
	public String getConfigurationVersion() {
		return configurationVersion;
	}

	/**
	 * Sets the configuration version
	 * 
	 * @param configurationVersion the configurationVersion to set
	 */
	public void setConfigurationVersion(String configurationVersion) {
		this.configurationVersion = configurationVersion;
	}

	/**
	 * Gets the dynamic processing parameters
	 * 
	 * @return the dynProcParameters
	 */
	public Map<String, Parameter> getDynProcParameters() {
		return dynProcParameters;
	}

	/**
	 * Sets the dynamic processing parameters
	 * 
	 * @param dynProcParameters the dynProcParameters to set
	 */
	public void setDynProcParameters(Map<String, Parameter> dynProcParameters) {
		this.dynProcParameters = dynProcParameters;
	}

	/**
	 * Gets the configuration files
	 * 
	 * @return the configurationFiles
	 */
	public Set<ConfigurationFile> getConfigurationFiles() {
		return configurationFiles;
	}

	/**
	 * Sets the configuration files
	 * 
	 * @param configurationFiles the configurationFiles to set
	 */
	public void setConfigurationFiles(Set<ConfigurationFile> configurationFiles) {
		this.configurationFiles = configurationFiles;
	}

}
