/**
 * DataCache.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.util.List;

/**
 * prosEO data cache for authenticated user
 *
 * @author Ernst Melchinger
 */
public class DataCache {

	/** List with cached configured processors */
	private List<String> configuredProcessors = null;

	/** List with cached processing facilities */
	private List<String> facilities = null;

	/** List with cached file classes */
	private List<String> fileClasses = null;

	/** List with cached processing modes */
	private List<String> processingModes = null;

	/** List with cached processor classes */
	private List<String> processorclasses = null;

	/** List with cached product classes */
	private List<String> productclasses = null;

	/** List with cached spacecrafts */
	private List<String> spaceCrafts = null;

	/** List with cached workflows */
	private List<String> workflows = null;

	/**
	 * Clears the data cache
	 */
	public void clear() {
		productclasses = null;
		configuredProcessors = null;
		facilities = null;
		fileClasses = null;
		processingModes = null;
		spaceCrafts = null;
	}

	/**
	 * Get the configured processors
	 *
	 * @return the configuredProcessors
	 */
	public List<String> getConfiguredProcessors() {
		return configuredProcessors;
	}

	/**
	 * Get the facilities
	 *
	 * @return the facilities
	 */
	public List<String> getFacilities() {
		return facilities;
	}

	/**
	 * Get the file classes
	 *
	 * @return the fileClasses
	 */
	public List<String> getFileClasses() {
		return fileClasses;
	}

	/**
	 * Get the processing modes
	 * 
	 * @return the processingModes
	 */
	public List<String> getProcessingModes() {
		return processingModes;
	}

	/**
	 * Get the processor classes
	 *
	 * @return the processor classes
	 */
	public List<String> getProcessorclasses() {
		return processorclasses;
	}

	/**
	 * Get the product classes
	 *
	 * @return the product classes
	 */
	public List<String> getProductclasses() {
		return productclasses;
	}

	/**
	 * Get the spacecrafts
	 * 
	 * @return the spaceCrafts
	 */
	public List<String> getSpaceCrafts() {
		return spaceCrafts;
	}

	/**
	 * Get the workflows
	 *
	 * @return the workflows
	 */
	public List<String> getWorkflows() {
		return workflows;
	}

	/**
	 * Set the configured processors
	 *
	 * @param configuredProcessors the configuredProcessors to set
	 */
	public void setConfiguredProcessors(List<String> configuredProcessors) {
		this.configuredProcessors = configuredProcessors;
	}

	/**
	 * Set the facilities
	 * 
	 * @param facilities the facilities to set
	 */
	public void setFacilities(List<String> facilities) {
		this.facilities = facilities;
	}

	/**
	 * Set the file classes
	 * 
	 * @param fileClasses the fileClasses to set
	 */
	public void setFileClasses(List<String> fileClasses) {
		this.fileClasses = fileClasses;
	}

	/**
	 * Set the processing modes
	 * 
	 * @param processingModes the processingModes to set
	 */
	public void setProcessingModes(List<String> processingModes) {
		this.processingModes = processingModes;
	}

	/**
	 * Set the processor classes
	 *
	 * @param processorclasses the processor classes to set
	 */
	public void setProcessorclasses(List<String> processorclasses) {
		this.processorclasses = processorclasses;
	}

	/**
	 * Set the product classes
	 *
	 * @param productclasses the product classes to set
	 */
	public void setProductclasses(List<String> productclasses) {
		this.productclasses = productclasses;
	}

	/**
	 * Set the spacecrafts
	 * 
	 * @param spaceCrafts the spaceCrafts to set
	 */
	public void setSpaceCrafts(List<String> spaceCrafts) {
		this.spaceCrafts = spaceCrafts;
	}

	/**
	 * Set the workflows
	 *
	 * @param workflows the workflows to set
	 */
	public void setWorkflows(List<String> workflows) {
		this.workflows = workflows;
	}

}