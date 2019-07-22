/**
 * SimpleSelectionRule.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import de.dlr.proseo.model.Product.Parameter;

/**
 * A rule defining the required input ProductTypes for a certain output ProductType using an ordered list of SelectionPolicys.
 * When selecting applicable Products for a JobStep the matching SelectionPolicy defines, whether a Product satisfies a ProductQuery.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class SimpleSelectionRule extends PersistentObject {
	
	/** Indicates whether the required source product is mandatory for the production of the target product */
	private Boolean isMandatory;
	
	/** Parameter values to filter the selected products (triple of parameter key, parameter type and parameter value); checked
	 * against product parameter values by equality.
	 */
	@ElementCollection
	private Map<String, Parameter> filterConditions;
	
	/** The product class which uses the selection rule */
	@ManyToOne
	private ProductClass targetProductClass;
	
	/** The product class which is selected by this rule */
	@ManyToOne
	private ProductClass sourceProductClass;
	
	/** The selection policies applied for selecting target products (the first applicable policy in the list holds) */
	@OneToMany
	private List<SimplePolicy> simplePolicies;

	/**
	 * @return the isMandatory
	 */
	public Boolean getIsMandatory() {
		return isMandatory;
	}

	/**
	 * @param isMandatory the isMandatory to set
	 */
	public void setIsMandatory(Boolean isMandatory) {
		this.isMandatory = isMandatory;
	}

	/**
	 * @return the filterConditions
	 */
	public Map<String, Parameter> getFilterConditions() {
		return filterConditions;
	}

	/**
	 * @param filterConditions the filterConditions to set
	 */
	public void setFilterConditions(Map<String, Parameter> filterConditions) {
		this.filterConditions = filterConditions;
	}

	/**
	 * @return the targetProductClass
	 */
	public ProductClass getTargetProductClass() {
		return targetProductClass;
	}

	/**
	 * @param targetProductClass the targetProductClass to set
	 */
	public void setTargetProductClass(ProductClass targetProductClass) {
		this.targetProductClass = targetProductClass;
	}

	/**
	 * @return the sourceProductClass
	 */
	public ProductClass getSourceProductClass() {
		return sourceProductClass;
	}

	/**
	 * @param sourceProductClass the sourceProductClass to set
	 */
	public void setSourceProductClass(ProductClass sourceProductClass) {
		this.sourceProductClass = sourceProductClass;
	}

	/**
	 * @return the simplePolicies
	 */
	public List<SimplePolicy> getSimplePolicies() {
		return simplePolicies;
	}

	/**
	 * @param simplePolicies the simplePolicies to set
	 */
	public void setSimplePolicies(List<SimplePolicy> simplePolicies) {
		this.simplePolicies = simplePolicies;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(sourceProductClass, targetProductClass);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof SimpleSelectionRule))
			return false;
		SimpleSelectionRule other = (SimpleSelectionRule) obj;
		return Objects.equals(sourceProductClass, other.sourceProductClass)
				&& Objects.equals(targetProductClass, other.targetProductClass);
	}

}
