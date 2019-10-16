/**
 * ProductClass.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * A class of products pertaining to a specific Mission, e. g. the L2_O3 products of Sentinel-5P. A ProductClass can describe
 * final (deliverable) products as well as intermediate products. For a ProductClass its dependency on base products can be
 * described using SelectionRules. Alternatively a ProductClass may be composed of other product classes (e. g. the S5P NPP
 * products consist of three separate single-band NPP sub-products).
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = { 
		@Index(unique = true, columnList = "mission_id, product_type"), 
		@Index(unique = true, columnList = "mission_id, mission_type") })
public class ProductClass extends PersistentObject {

	/** The mission this product class belongs to */
	@ManyToOne
	private Mission mission;
	
	/** The product type, as it shall be known in the processing system (product class identifier; e. g. CLOUD);
	 * unique within a mission */
	@Column(name = "product_type")
	private String productType;
	
	/** The product type as it is agreed in the mission specification documents (e. g. L2_CLOUD___); unique within a mission */
	@Column(name = "mission_type")
	private String missionType;
	
	/** A short description of the product type to display as informational text on the user interface */
	private String description;
	
	/** The selection rules describing the required input files to generate this product class */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "targetProductClass")
	private Set<SimpleSelectionRule> requiredSelectionRules = new HashSet<>();
	
	/** The selection rules, for which this class provides the requested input files */
	@OneToMany(mappedBy = "sourceProductClass")
	private Set<SimpleSelectionRule> supportedSelectionRules = new HashSet<>();
	
	/** Set of component product classes */
	@OneToMany(mappedBy = "enclosingClass")
	private Set<ProductClass> componentClasses = new HashSet<>();
	
	/** Product class for which this product class is a component */
	@ManyToOne
	private ProductClass enclosingClass;
	
	/** Processor class capable of generating products of this class */
	@ManyToOne
	private ProcessorClass processorClass;
	
	
	/**
	 * Gets the mission this product class belongs to
	 * 
	 * @return the mission
	 */
	public Mission getMission() {
		return mission;
	}
	
	/**
	 * Sets the mission this product class belongs to
	 * 
	 * @param mission the mission to set
	 */
	public void setMission(Mission mission) {
		this.mission = mission;
	}
	
	/**
	 * Gets the prosEO-internal product type (product class identifier)
	 * 
	 * @return the product type
	 */
	public String getProductType() {
		return productType;
	}
	
	/**
	 * Sets the prosEO-internal product type (product class identifier)
	 * 
	 * @param productType the product type to set
	 */
	public void setProductType(String productType) {
		this.productType = productType;
	}
	
	/**
	 * Gets the product class type as defined by the mission
	 * 
	 * @return the missionType
	 */
	public String getMissionType() {
		return missionType;
	}
	
	/**
	 * Sets the product class type as defined by the mission
	 * 
	 * @param missionType the missionType to set
	 */
	public void setMissionType(String missionType) {
		this.missionType = missionType;
	}
	
	/**
	 * Gets the product class description
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Sets the product class description
	 * 
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Gets the set of required selection rules
	 * 
	 * @return the requiredSelectionRules
	 */
	public Set<SimpleSelectionRule> getRequiredSelectionRules() {
		return requiredSelectionRules;
	}

	/**
	 * Gets the set of required selection rules
	 * 
	 * @param requiredSelectionRules the requiredSelectionRules to set
	 */
	public void setRequiredSelectionRules(Set<SimpleSelectionRule> requiredSelectionRules) {
		this.requiredSelectionRules = requiredSelectionRules;
	}

	/**
	 * Gets the set of supported selection rules
	 * 
	 * @return the supportedSelectionRules
	 */
	public Set<SimpleSelectionRule> getSupportedSelectionRules() {
		return supportedSelectionRules;
	}

	/**
	 * Sets the set of supported selection rules
	 * 
	 * @param supportedSelectionRules the supportedSelectionRules to set
	 */
	public void setSupportedSelectionRules(Set<SimpleSelectionRule> supportedSelectionRules) {
		this.supportedSelectionRules = supportedSelectionRules;
	}

	/**
	 * Gets the product classes of component products
	 * 
	 * @return the componentClasses
	 */
	public Set<ProductClass> getComponentClasses() {
		return componentClasses;
	}
	
	/**
	 * Sets the product classes for component products
	 * 
	 * @param componentClasses the componentClasses to set
	 */
	public void setComponentClasses(Set<ProductClass> componentClasses) {
		this.componentClasses = componentClasses;
	}
	
	/**
	 * Gets the product classes of an enclosing product
	 * 
	 * @return the enclosingClass
	 */
	public ProductClass getEnclosingClass() {
		return enclosingClass;
	}
	
	/**
	 * Sets the product classes for an enclosing product
	 * 
	 * @param enclosingClass the enclosingClass to set
	 */
	public void setEnclosingClass(ProductClass enclosingClass) {
		this.enclosingClass = enclosingClass;
	}
	
	/**
	 * Gets the processor class capable of generating products of this class
	 * 
	 * @return the processorClass
	 */
	public ProcessorClass getProcessorClass() {
		return processorClass;
	}

	/**
	 * Gets the processor class capable of generating products of this class
	 * 
	 * @param processorClass the processorClass to set
	 */
	public void setProcessorClass(ProcessorClass processorClass) {
		this.processorClass = processorClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((mission == null) ? 0 : mission.hashCode());
		result = prime * result + ((productType == null) ? 0 : productType.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ProductClass))
			return false;
		ProductClass other = (ProductClass) obj;
		if (mission == null) {
			if (other.mission != null)
				return false;
		} else if (!mission.equals(other.mission))
			return false;
		if (productType == null) {
			if (other.productType != null)
				return false;
		} else if (!productType.equals(other.productType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ProductClass [mission=" + (null == mission ? "null" : mission.getCode()) 
				+ ", productType=" + productType + ", missionType=" + missionType + ", description=" + description 
				+ ", processorClass=" + (null == processorClass ? "null" : processorClass.getProcessorName()) + "]";
	}
	
	
}
