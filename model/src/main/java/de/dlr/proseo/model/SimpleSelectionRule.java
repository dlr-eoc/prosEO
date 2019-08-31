/**
 * SimpleSelectionRule.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * A rule defining the required input ProductTypes for a certain output ProductType using an ordered list of SelectionPolicys.
 * When selecting applicable Products for a JobStep the matching SelectionPolicy defines, whether a Product satisfies a ProductQuery.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class SimpleSelectionRule extends PersistentObject {
	
	/**
	 * Processing mode, for which this selection rule is valid (level 7 "Mode" from Generic IPF Interface Specifications, sec. 4.1.3);
	 * this is restricted by the processing modes defined for the mission, but the (self evident and default) special value "ALWAYS"
	 * is always valid.
	 */
	private String mode = "ALWAYS";
	
	/**
	 * Indicates whether the required source product is mandatory for the production of the target product
	 * (level 7 "Mandatory" from Generic IPF Interface Specifications, sec. 4.1.3)
	 */
	private Boolean isMandatory;
	
	/** 
	 * Parameter values to filter the selected products (triple of parameter key, parameter type and parameter value); checked
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
	
	/** The set of processor configurations, for which this rule is applicable */
	@ManyToMany
	private Set<ConfiguredProcessor> applicableConfiguredProcessors;
	
	/** The selection policies applied for selecting target products (the first applicable policy in the list holds) */
	@OneToMany
	private List<SimplePolicy> simplePolicies;

	/**
	 * Gets the applicable processing mode
	 * 
	 * @return the mode
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * Sets the applicable processing mode
	 * 
	 * @param mode the mode to set
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * Check whether the selected product is mandatory
	 * 
	 * @return the isMandatory
	 */
	public Boolean getIsMandatory() {
		return isMandatory;
	}

	/**
	 * Check whether the selected product is mandatory (convenience alias for getIsMandatory())
	 * 
	 * @return the isMandatory
	 */
	public Boolean isMandatory() {
		return this.getIsMandatory();
	}

	/**
	 * Indicate whether the selected product is mandatory
	 * 
	 * @param isMandatory the isMandatory to set
	 */
	public void setIsMandatory(Boolean isMandatory) {
		this.isMandatory = isMandatory;
	}

	/**
	 * Gets the additional filtering conditions
	 * 
	 * @return the filterConditions
	 */
	public Map<String, Parameter> getFilterConditions() {
		return filterConditions;
	}

	/**
	 * Sets the additional filtering conditions
	 * 
	 * @param filterConditions the filterConditions to set
	 */
	public void setFilterConditions(Map<String, Parameter> filterConditions) {
		this.filterConditions = filterConditions;
	}

	/**
	 * Gets the target product class (the one for which input products are selected by this rule)
	 * 
	 * @return the targetProductClass
	 */
	public ProductClass getTargetProductClass() {
		return targetProductClass;
	}

	/**
	 * Sets the target product class (the one for which input products are selected by this rule)
	 * 
	 * @param targetProductClass the targetProductClass to set
	 */
	public void setTargetProductClass(ProductClass targetProductClass) {
		this.targetProductClass = targetProductClass;
	}

	/**
	 * Gets the source product classes (the input products selected by this rule)
	 * 
	 * @return the sourceProductClass
	 */
	public ProductClass getSourceProductClass() {
		return sourceProductClass;
	}

	/**
	 * Sets the source product classes (the input products selected by this rule)
	 * 
	 * @param sourceProductClass the sourceProductClass to set
	 */
	public void setSourceProductClass(ProductClass sourceProductClass) {
		this.sourceProductClass = sourceProductClass;
	}

	/**
	 * Gets the applicable processor configurations
	 * 
	 * @return the applicableConfiguredProcessors
	 */
	public Set<ConfiguredProcessor> getApplicableConfiguredProcessors() {
		return applicableConfiguredProcessors;
	}

	/**
	 * Sets the applicable processor configurations
	 * 
	 * @param applicableConfiguredProcessors the applicableConfiguredProcessors to set
	 */
	public void setApplicableConfiguredProcessors(Set<ConfiguredProcessor> applicableConfiguredProcessors) {
		this.applicableConfiguredProcessors = applicableConfiguredProcessors;
	}

	/**
	 * Gets the simple selection policies
	 * 
	 * @return the simplePolicies
	 */
	public List<SimplePolicy> getSimplePolicies() {
		return simplePolicies;
	}

	/**
	 * Sets the simple selection policies
	 * 
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
