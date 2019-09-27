/**
 * SimpleSelectionRule.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import de.dlr.proseo.model.util.SelectionRule.SelectionItem;

/**
 * A rule defining the required input ProductTypes for a certain output ProductType using an ordered list of SelectionPolicys.
 * When selecting applicable Products for a JobStep the matching SelectionPolicy defines, whether a Product satisfies a ProductQuery.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class SimpleSelectionRule extends PersistentObject {
	
	/* Error messages */
	private static final String MSG_NO_ITEM_FOUND = "No item found for selection rule '%s' and time interval (%s, %s)";
	private static final String MSG_INVALID_ITEM_TYPE = "Item with different item type found";
	
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
	
	/** The product class which uses the selection rule */
	@ManyToOne
	private ProductClass targetProductClass;
	
	/** The product class which is selected by this rule */
	@ManyToOne
	private ProductClass sourceProductClass;
	
	/** 
	 * Parameter values to filter the selected products (triple of parameter key, parameter type and parameter value); checked
	 * against product parameter values by equality.
	 */
	@ElementCollection
	private Map<String, Parameter> filterConditions = new HashMap<>();
	
	/**
	 * Combined filter consisting of source product class and filter conditions in the form
	 * &lt;product type&gt;[/&lt;filter key&gt;:&lt;filter value&gt;[,&lt;filter key&gt;:&lt;filter value&gt;]...]
	 */
	private String filteredSourceProductType;
	
	/** 
	 * The selection policies applied for selecting target products (chained by logical 'short-circuit' OR, 
	 * i. e. the first applicable policy in the list holds) 
	 */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SimplePolicy> simplePolicies = new ArrayList<>();

	/** The set of processor configurations, for which this rule is applicable */
	@ManyToMany
	private Set<ConfiguredProcessor> applicableConfiguredProcessors = new HashSet<>();
	
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
	 * Gets the source product type with filter conditions
	 * 
	 * @return the filteredSourceProductType
	 */
	public String getFilteredSourceProductType() {
		return filteredSourceProductType;
	}

	/**
	 * Sets the source product type with filter conditions
	 * 
	 * @param filteredSourceProductType the filteredSourceProductType to set
	 */
	public void setFilteredSourceProductType(String filteredSourceProductType) {
		this.filteredSourceProductType = filteredSourceProductType;
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

	/**
	 * Merge two simple rules creating a new simple rule
	 * <p>
	 * For selection rules containing the policy 'LatestValidityClosest' a merge is possible, if and only if all occurrences
	 * of this policy in both rules have the same delta times (this policy actually refers to a point
	 * in time and not to a time interval, therefore a merge can only be done between policies referring to the same point in time).
	 * 
	 * @param anotherSimpleSelectionRule the simple rule to merge this rule with
	 * @return a new SimpleRule object whose policies reflect the merged validity periods
	 * @throws IllegalArgumentException if a merge of simple rules with different product types or with different
	 *   delta times for a 'LatestValidityClosest' policy is attempted
	 */
	public SimpleSelectionRule merge(SimpleSelectionRule anotherSimpleSelectionRule) throws IllegalArgumentException {
		if (!targetProductClass.equals(anotherSimpleSelectionRule.targetProductClass)) {
			throw new IllegalArgumentException("Cannot merge simple rules for different product types!");
		}
		SimpleSelectionRule newSimpleSelectionRule = new SimpleSelectionRule();
		newSimpleSelectionRule.targetProductClass = targetProductClass;
		newSimpleSelectionRule.sourceProductClass = sourceProductClass;
		newSimpleSelectionRule.filterConditions = filterConditions;
		newSimpleSelectionRule.filteredSourceProductType = filteredSourceProductType;
		// Merge all policies of this rule with the corresponding policies of the other rule
		for (SimplePolicy simplePolicy: simplePolicies) {
			boolean found = false;
			for (SimplePolicy anotherSimplePolicy: anotherSimpleSelectionRule.simplePolicies) {
				if (simplePolicy.getPolicyType().equals(anotherSimplePolicy.getPolicyType())) {
					newSimpleSelectionRule.simplePolicies.add(simplePolicy.merge(anotherSimplePolicy));
					found = true;
					break;
				}
			}
			if (!found) {
				newSimpleSelectionRule.simplePolicies.add(simplePolicy);
			}
		}
		// Add all policies of the other rule, which have not yet been merged
		for (SimplePolicy anotherSimplePolicy: anotherSimpleSelectionRule.simplePolicies) {
			boolean found = false;
			for (SimplePolicy simplePolicy: newSimpleSelectionRule.simplePolicies) {
				if (simplePolicy.getPolicyType().equals(anotherSimplePolicy.getPolicyType())) {
					found = true; // Already merged
					break;
				} 
			}
			if (!found) {
				newSimpleSelectionRule.simplePolicies.add(anotherSimplePolicy);
			}
		}
		newSimpleSelectionRule.isMandatory = isMandatory || anotherSimpleSelectionRule.isMandatory;
		
		return newSimpleSelectionRule;
	}
	
	/**
	 * Select all items from the given collection that fulfil this rule for the given time interval.
	 * For all items the item type must match the targetProductClass of the rule.
	 * 
	 * @param items the collection of items to be searched
	 * @param startTime the start time of the time interval to check against
	 * @param stopTime the end time of the time interval to check against
	 * @return a list of all item objects fulfilling the selection rule, or null, if no such qualifying item
	 * 		   exists and the selection rule is marked as 'OPTIONAL'
	 * @throws NoSuchElementException if no item fulfils the selection rule, and the selection rule is marked as 'MANDATORY'
	 * @throws IllegalArgumentException if any of the items is not of the correct type
	 */
	public List<Object> selectItems(Collection<SelectionItem> items, Instant startTime, Instant stopTime)
			throws NoSuchElementException, IllegalArgumentException {
		List<Object> itemObjectList = new ArrayList<Object>();
		
		// Check that all items conform to the product type of this rule
		for (SelectionItem item: items) {
			if (!item.itemType.equals(targetProductClass.getProductType())) {
				throw new IllegalArgumentException(MSG_INVALID_ITEM_TYPE + item.itemType);
			}
		}
		
		// Iterate over all policies and test them against the item collection
		for (SimplePolicy policy: simplePolicies) {
			itemObjectList.addAll(policy.selectItems(items, startTime, stopTime));
			if (!itemObjectList.isEmpty()) {
				// Short-circuited OR: first match(es) apply
				return itemObjectList;
			}
		}
		// No matching items found
		if (isMandatory) {
			throw new NoSuchElementException(
					String.format(MSG_NO_ITEM_FOUND, this.toString(), startTime.toString(), stopTime.toString()));
		}
		return null;
	}
	
	/**
	 * Format this rule as an OQL query
	 * <p>
	 * Limitation: For LatestValidityClosest the query may return two products, one to each side of the centre of the
	 * given time interval. It is up to the calling program to select the applicable product.
	 * 
	 * @param startTime the start time to use in the database query
	 * @param stopTime the stop time to use in the database query
	 * @return an OQL string representing this rule
	 */
	public String asPlQueryCondition(final Instant startTime, final Instant stopTime) {
		// Generate query projection
		StringBuilder simpleRuleQuery = new StringBuilder("select startTime, stopTime from ");
		simpleRuleQuery.append(sourceProductClass.getProductType()).append(" where ");
		
		// Ensure canonical ordering of policies
		simplePolicies.sort(new Comparator<SimplePolicy>() {
			@Override
			public int compare(SimplePolicy o1, SimplePolicy o2) {
				return o1.getPolicyType().compareTo(o2.getPolicyType());
			}});
		
		// Generate query condition
		if (0 < filterConditions.size()) {
			// Wrap everything in parentheses for later addition of filter conditions
			simpleRuleQuery.append("(");
		}
		
		// Format policies
		if (1 < simplePolicies.size()) {
			// Wrap multiple policies in parentheses
			simpleRuleQuery.append("(");
		}
		boolean first = true;
		for (SimplePolicy simplePolicy: simplePolicies) {
			if (first)
				first = false;
			else
				simpleRuleQuery.append(" or ");
			simpleRuleQuery.append(simplePolicy.asPlQueryCondition(sourceProductClass.getProductType(), startTime, stopTime));
		}
		if (1 < simplePolicies.size()) {
			// Close parentheses for multiple policies
			simpleRuleQuery.append(")");
		}
		
		// Format filter conditions
		if (0 < filterConditions.size()) {
			for (String filterKey: filterConditions.keySet()) {
				simpleRuleQuery.append(String.format(" and %s = '%s'", filterKey, filterConditions.get(filterKey).getStringValue()));
			}
			// Close parentheses of query string with filter conditions
			simpleRuleQuery.append(")");
		}
		return simpleRuleQuery.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder simpleRuleString = new StringBuilder("FOR ");
		simpleRuleString.append(sourceProductClass.getProductType());
		if (0 < filterConditions.size()) {
			simpleRuleString.append('/');
			boolean first = true;
			for (String filterKey: filterConditions.keySet()) {
				if (first)
					first = false;
				else
					simpleRuleString.append(',');
				simpleRuleString.append(filterKey).append(':').append(filterConditions.get(filterKey).getStringValue());
			}
		}
		simpleRuleString.append(" SELECT ");
		boolean first = true;
		for (SimplePolicy simplePolicy: simplePolicies) {
			if (first)
				first = false;
			else
				simpleRuleString.append(" OR ");
			simpleRuleString.append(simplePolicy.toString());
		}
		if (isMandatory)
			simpleRuleString.append(" MANDATORY");
		else
			simpleRuleString.append(" OPTIONAL");
		return simpleRuleString.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(sourceProductClass, targetProductClass);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
