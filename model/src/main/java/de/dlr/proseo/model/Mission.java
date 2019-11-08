/**
 * Mission.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * An Earth Observation mission.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = @Index(unique = true, columnList = "code"))
public class Mission extends PersistentObject {

	/** The mission code (e. g. S5P) */
	private String code;
	
	/** The mission name (e. g. Sentinel-5 Precursor) */
	private String name;
	
	/**
	 * Allowed file classes for this mission (Ground Segment File Format Standard, sec. 4.1.2)
	 */
	@ElementCollection
	private Set<String> fileClasses = new HashSet<>();
	
	/** 
	 * Processing mode tags as agreed for this mission (level 7 "Mode" from Generic IPF Interface Specifications, sec. 4.1.3).
     * <br>
     * The tag names can be chosen according to the processing operational needs and configuration. Examples of name operationally 
     * used in existing Processing Facilities are herewith provided:
     * <ul>
     * <li>NRT: near real time orders</li>
     * <li>SYSTEMATIC: routine production orders</li>
     * <li>REPROCESSING: Batch reprocessing (with new auxiliary files) of previously executed orders</li>
     * <li>SUBS: “slice” orders, that is, orders dedicated to extract a sub- portion from a previously generate product</li>
     * </ul>
     * Example: For Sentinel-5P, the tags are "NRTI" (near real time), "OFFL" (systematic ["offline"]), "RPRO" (reprocessing)
	 */
	@ElementCollection
	private Set<String> processingModes = new HashSet<>();
	
	/**
	 * Template for the generation of product files, indicating variable parts using Spring Expression Language, e. g.
	 * 			"S5P_${fileClass}_${productClass.missionType}_" +
	 *			"${T(java.time.format.DateTimeFormatter).ofPattern(\"uuuuMMdd'T'HHmmss\").withZone(T(java.time.ZoneId).of(\"UTC\")).format(sensingStartTime)}_" +
	 *			"${T(java.time.format.DateTimeFormatter).ofPattern(\"uuuuMMdd'T'HHmmss\").withZone(T(java.time.ZoneId).of(\"UTC\")).format(sensingStopTime)}_" +
	 *			"${(new java.text.DecimalFormat(\"00000\")).format(orbit.orbitNumber)}_" +
	 *			"${parameters.get(\"copernicusCollection\").getParameterValue()}_" +
	 *			"${configuredProcessor.processor.processorVersion.replaceAll(\"\\.\", \"\")}_" +
	 *			"${T(java.time.format.DateTimeFormatter).ofPattern(\"uuuuMMdd'T'HHmmss\").withZone(T(java.time.ZoneId).of(\"UTC\")).format(generationTime)}.nc";
	 * (based on the Sentinel-5P file naming convention)
	 */
	@org.hibernate.annotations.Type(type = "materialized_clob")
	private String productFileTemplate;
	
	/** The spacecrafts this mission owns */
	@OneToMany(mappedBy = "mission")
	private Set<Spacecraft> spacecrafts = new HashSet<>();
	
	/** The product classes this mission produces or uses */
	@OneToMany(mappedBy = "mission")
	private Set<ProductClass> productClasses = new HashSet<>();
	
	/** The processor classes this mission uses */
	@OneToMany(mappedBy = "mission")
	private Set<ProcessorClass> processorClasses = new HashSet<>();
	
	/** The processing orders issued for this mission */
	@OneToMany(mappedBy = "mission")
	private Set<ProcessingOrder> processingOrders = new HashSet<>();
	
	/**
	 * Gets the mission code
	 * 
	 * @return the code
	 */
	public String getCode() {
		return code;
	}
	
	/**
	 * Sets the mission code
	 * 
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}
	
	/**
	 * Gets the mission name
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the mission name
	 * 
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the allowed file classes
	 * 
	 * @return the fileClasses
	 */
	public Set<String> getFileClasses() {
		return fileClasses;
	}

	/**
	 * Sets the allowed file classes
	 * 
	 * @param fileClasses the fileClasses to set
	 */
	public void setFileClasses(Set<String> fileClasses) {
		this.fileClasses = fileClasses;
	}

	/**
	 * Gets the processing mode tags defined for the mission
	 * 
	 * @return the processingModes
	 */
	public Set<String> getProcessingModes() {
		return processingModes;
	}

	/**
	 * Sets the processing mode tags defined for the mission
	 * 
	 * @param processingModes the processingModes to set
	 */
	public void setProcessingModes(Set<String> processingModes) {
		this.processingModes = processingModes;
	}

	/**
	 * Gets the product filename template
	 * 
	 * @return the productFileTemplate
	 */
	public String getProductFileTemplate() {
		return productFileTemplate;
	}

	/**
	 * Sets the product filename template
	 * 
	 * @param productFileTemplate the product filename template to set
	 */
	public void setProductFileTemplate(String productFileTemplate) {
		this.productFileTemplate = productFileTemplate;
	}

	/**
	 * Gets the set of spacecrafts
	 * 
	 * @return the spacecrafts
	 */
	public Set<Spacecraft> getSpacecrafts() {
		return spacecrafts;
	}

	/**
	 * Sets the set of spacecrafts
	 * 
	 * @param spacecrafts the spacecrafts to set
	 */
	public void setSpacecrafts(Set<Spacecraft> spacecrafts) {
		this.spacecrafts = spacecrafts;
	}

	/**
	 * Gets the product classes defined for this mission
	 * 
	 * @return the productClasses
	 */
	public Set<ProductClass> getProductClasses() {
		return productClasses;
	}

	/**
	 * Sets the product classes defined for this mission
	 * 
	 * @param productClasses the productClasses to set
	 */
	public void setProductClasses(Set<ProductClass> productClasses) {
		this.productClasses = productClasses;
	}

	/**
	 * Gets the processor classes defined for this mission
	 * 
	 * @return the processorClasses
	 */
	public Set<ProcessorClass> getProcessorClasses() {
		return processorClasses;
	}

	/**
	 * Sets the processor classes defined for this mission
	 * 
	 * @param processorClasses the processorClasses to set
	 */
	public void setProcessorClasses(Set<ProcessorClass> processorClasses) {
		this.processorClasses = processorClasses;
	}

	/**
	 * Gets the processing orders issued for this mission
	 * 
	 * @return the processingOrders
	 */
	public Set<ProcessingOrder> getProcessingOrders() {
		return processingOrders;
	}

	/**
	 * Sets the processing orders issued for this mission
	 * 
	 * @param processingOrders the processingOrders to set
	 */
	public void setProcessingOrders(Set<ProcessingOrder> processingOrders) {
		this.processingOrders = processingOrders;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Mission))
			return false;
		Mission other = (Mission) obj;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Mission [code=" + code + ", name=" + name + ", processingModes=" + processingModes + "]";
	}
}
