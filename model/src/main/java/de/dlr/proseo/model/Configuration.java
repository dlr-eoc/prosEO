/**
 * Configuration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import de.dlr.proseo.model.enums.ProductQuality;

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
	
	/** Indicates the suitability for general use of products generated with this configuration */
	@Enumerated(EnumType.STRING)
	private ProductQuality productQuality = ProductQuality.NOMINAL;
	
	/** The configuration files for this configuration */
	@ElementCollection
	private Set<ConfigurationFile> configurationFiles = new HashSet<>();
	
	/**
	 * Additional input files for inclusion in generated Job Order Files
	 */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<ConfigurationInputFile> staticInputFiles = new HashSet<>();

	/** Specific parameter for "docker run" valid for this configuration */
	@ElementCollection
	private Map<String, String> dockerRunParameters = new HashMap<>();

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
	 * Gets the product quality expected with this configuration
	 * 
	 * @return the product quality
	 */
	public ProductQuality getProductQuality() {
		return productQuality;
	}

	/**
	 * Sets the product quality expected with this configuration
	 * 
	 * @param productQuality the product quality to set
	 */
	public void setProductQuality(ProductQuality productQuality) {
		this.productQuality = productQuality;
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

	/**
	 * Gets the static input files
	 * 
	 * @return the static input files
	 */
	public Set<ConfigurationInputFile> getStaticInputFiles() {
		return staticInputFiles;
	}

	/**
	 * Sets the static input files
	 * 
	 * @param staticInputFiles the static input files to set
	 */
	public void setStaticInputFiles(Set<ConfigurationInputFile> staticInputFiles) {
		this.staticInputFiles = staticInputFiles;
	}

	/**
	 * Gets the configuration-specific "docker run" parameters
	 * 
	 * @return the dockerRunParameters
	 */
	public Map<String, String> getDockerRunParameters() {
		return dockerRunParameters;
	}

	/**
	 * Sets the configuraiton-specific "docker run" parameters
	 * 
	 * @param dockerRunParameters the dockerRunParameters to set
	 */
	public void setDockerRunParameters(Map<String, String> dockerRunParameters) {
		this.dockerRunParameters = dockerRunParameters;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(configurationVersion, processorClass);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Configuration))
			return false;
		Configuration other = (Configuration) obj;
		return Objects.equals(configurationVersion, other.configurationVersion)
				&& Objects.equals(processorClass, other.processorClass);
	}

	@Override
	public String toString() {
		return "Configuration [processorClass=" + (null == processorClass ? "null" : processorClass.getProcessorName()) 
				+ ", configurationVersion=" + configurationVersion + ", dynProcParameters=" + dynProcParameters
				+ ", configurationFiles=" + configurationFiles + ", dockerRunParameters=" + dockerRunParameters + "]";
	}

}
