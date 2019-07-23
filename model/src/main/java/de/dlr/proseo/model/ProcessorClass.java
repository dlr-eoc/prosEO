/**
 * ProcessorClass.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * A type of processor capable of generating products of a specific set of ProductClasses. There can be only one ProcessorClass
 * capable of generating products of any one ProductClass.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class ProcessorClass extends PersistentObject {
	
	/** The mission this processor class belongs to */
	@ManyToOne
	private Mission mission;
	
	/** User-defined unique processor class name (Processor_Name from Generic IPF Interface Specifications, sec. 4.1.3) */
	private String processorName;
	
	/** The product classes a processor of this class can generate */
	@OneToMany(mappedBy = "processorClass")
	private Set<ProductClass> productClasses;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(mission, processorName);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ProcessorClass))
			return false;
		ProcessorClass other = (ProcessorClass) obj;
		return Objects.equals(mission, other.mission) && Objects.equals(processorName, other.processorName);
	}

}
