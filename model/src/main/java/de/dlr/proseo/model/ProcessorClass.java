/**
 * ProcessorClass.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * A type of processor capable of generating products of a specific set of ProductClasses. There can be only one ProcessorClass
 * capable of generating products of any one ProductClass.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = @Index(unique = true, columnList = "mission_id, processor_name"))
public class ProcessorClass extends PersistentObject {
	
	/** The mission this processor class belongs to */
	@ManyToOne
	private Mission mission;
	
	/** User-defined processor class name (unique within a mission; Processor_Name from Generic IPF Interface Specifications, sec. 4.1.3) */
	@Column(name = "processor_name")
	private String processorName;
	
	/** The product classes a processor of this class can generate */
	@OneToMany(mappedBy = "processorClass")
	private Set<ProductClass> productClasses = new HashSet<>();
	
	/** The processor versions for this class */
	@OneToMany(mappedBy = "processorClass")
	private Set<Processor> processors = new HashSet<>();

	/**
	 * Gets the mission this processor class belongs to
	 * 
	 * @return the mission
	 */
	public Mission getMission() {
		return mission;
	}

	/**
	 * Sets the mission this processor class belongs to
	 * 
	 * @param mission the mission to set
	 */
	public void setMission(Mission mission) {
		this.mission = mission;
	}

	/**
	 * Gets the processor (class) name
	 * 
	 * @return the processorName
	 */
	public String getProcessorName() {
		return processorName;
	}

	/**
	 * Sets the processor (class) name
	 * 
	 * @param processorName the processorName to set
	 */
	public void setProcessorName(String processorName) {
		this.processorName = processorName;
	}

	/**
	 * Gets the product classes processors of this class can generate
	 * 
	 * @return the productClasses
	 */
	public Set<ProductClass> getProductClasses() {
		return productClasses;
	}

	/**
	 * Sets the product classes processors of this class can generate
	 * 
	 * @param productClasses the productClasses to set
	 */
	public void setProductClasses(Set<ProductClass> productClasses) {
		this.productClasses = productClasses;
	}

	/**
	 * Gets the processor versions for this class
	 * 
	 * @return the processors
	 */
	public Set<Processor> getProcessors() {
		return processors;
	}

	/**
	 * Sets the processor versions for this class
	 * 
	 * @param processors the processors to set
	 */
	public void setProcessors(Set<Processor> processors) {
		this.processors = processors;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(mission, processorName);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		// Object identity
		if (this == obj)
			return true;
		
		// Same database object
		if (super.equals(obj))
			return true;
		
		if (!(obj instanceof ProcessorClass))
			return false;
		ProcessorClass other = (ProcessorClass) obj;
		return Objects.equals(processorName, other.getProcessorName()) && Objects.equals(mission, other.getMission());
	}

	@Override
	public String toString() {
		return "ProcessorClass [mission=" + (null == mission ? "null" : mission.getCode()) + ", processorName=" + processorName + "]";
	}

}
