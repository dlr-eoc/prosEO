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
 * 
 */
public class DataCache {

	/**
	 * List with cached data
	 */
	private List<String> productclasses = null;
	/**
	 * List with cached data
	 */
	private List<String> configuredProcessors = null;
	/**
	 * List with cached data
	 */
	private List<String> facilities = null;

	/**
	 * List with cached data
	 */
	private List<String> fileClasses = null;
	/**
	 * List with cached data
	 */
	private List<String> processingModes = null;
	/**
	 * List with cached data
	 */
	private List<String> spaceCrafts = null;

	/**
	 * @return the configuredProcessors
	 */
	public List<String> getConfiguredProcessors() {
		return configuredProcessors;
	}

	/**
	 * @param configuredProcessors the configuredProcessors to set
	 */
	public void setConfiguredProcessors(List<String> configuredProcessors) {
		this.configuredProcessors = configuredProcessors;
	}

	/**
	 * @return the productclasses
	 */
	public List<String> getProductclasses() {
		return productclasses;
	}

	/**
	 * @param productclasses the productclasses to set
	 */
	public void setProductclasses(List<String> productclasses) {
		this.productclasses = productclasses;
	}

	/**
	 * @return the facilities
	 */
	public List<String> getFacilities() {
		return facilities;
	}

	/**
	 * @return the fileClasses
	 */
	public List<String> getFileClasses() {
		return fileClasses;
	}

	/**
	 * @return the processingModes
	 */
	public List<String> getProcessingModes() {
		return processingModes;
	}

	/**
	 * @return the spaceCrafts
	 */
	public List<String> getSpaceCrafts() {
		return spaceCrafts;
	}

	/**
	 * @param facilities the facilities to set
	 */
	public void setFacilities(List<String> facilities) {
		this.facilities = facilities;
	}

	/**
	 * @param fileClasses the fileClasses to set
	 */
	public void setFileClasses(List<String> fileClasses) {
		this.fileClasses = fileClasses;
	}

	/**
	 * @param processingModes the processingModes to set
	 */
	public void setProcessingModes(List<String> processingModes) {
		this.processingModes = processingModes;
	}

	/**
	 * @param spaceCrafts the spaceCrafts to set
	 */
	public void setSpaceCrafts(List<String> spaceCrafts) {
		this.spaceCrafts = spaceCrafts;
	}

	
	public void clear() {
		productclasses = null;
		configuredProcessors = null;
		facilities = null;
		fileClasses = null;
		processingModes = null;
		spaceCrafts = null;
	}
}
