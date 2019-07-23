/**
 * ProductClass.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

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
public class ProductClass extends PersistentObject {

	/** The mission this product class belongs to */
	@ManyToOne
	private Mission mission;
	/** The product type, as it shall be known in the processing system (product class identifier; e. g. CLOUD);
	 * unique within a mission */
	private String productType;
	/** The product type as it is agreed in the mission specification documents (e. g. L2_CLOUD___); unique within a mission */
	private String missionType;
	/** A short description of the product type to display as informational text on the user interface */
	private String description;
	
	/** Set of component product classes */
	@OneToMany(mappedBy = "enclosingClass")
	private Set<ProductClass> componentClasses;
	/** Product class for which this product class is a component */
	@ManyToOne
	private ProductClass enclosingClass;
	
	
	/**
	 * Gets the mission this product class belongs to
	 * @return the mission
	 */
	public Mission getMission() {
		return mission;
	}
	
	/**
	 * Sets the mission this product class belongs to
	 * @param mission the mission to set
	 */
	public void setMission(Mission mission) {
		this.mission = mission;
	}
	
	/**
	 * Gets the prosEO-internal product type (product class identifier)
	 * @return the product type
	 */
	public String getProductType() {
		return productType;
	}
	
	/**
	 * Sets the prosEO-internal product type (product class identifier)
	 * @param productType the product type to set
	 */
	public void setProductType(String productType) {
		this.productType = productType;
	}
	
	/**
	 * Gets the product class type as defined by the mission
	 * @return the missionType
	 */
	public String getMissionType() {
		return missionType;
	}
	
	/**
	 * Sets the product class type as defined by the mission
	 * @param missionType the missionType to set
	 */
	public void setMissionType(String missionType) {
		this.missionType = missionType;
	}
	
	/**
	 * Gets the product class description
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Sets the product class description
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Gets the product classes of component products
	 * @return the componentClasses
	 */
	public Set<ProductClass> getComponentClasses() {
		return componentClasses;
	}
	
	/**
	 * Sets the product classes for component products
	 * @param componentClasses the componentClasses to set
	 */
	public void setComponentClasses(Set<ProductClass> componentClasses) {
		this.componentClasses = componentClasses;
	}
	
	/**
	 * Gets the product classes of an enclosing product
	 * @return the enclosingClass
	 */
	public ProductClass getEnclosingClass() {
		return enclosingClass;
	}
	
	/**
	 * Sets the product classes for an enclosing product
	 * @param enclosingClass the enclosingClass to set
	 */
	public void setEnclosingClass(ProductClass enclosingClass) {
		this.enclosingClass = enclosingClass;
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
	
	
}
