/**
 * SimpleSelectionRule.java
 * 
 * (C) 2016 - 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Duration;
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
import de.dlr.proseo.model.util.SelectionItem;

/**
 * A rule defining the required input ProductTypes for a certain output ProductType using an ordered list of SelectionPolicys.
 * When selecting applicable Products for a JobStep the matching SelectionPolicy defines, whether a Product satisfies a ProductQuery.
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
public class SimpleSelectionRule extends PersistentObject {
	
	/* Error messages */
	private static final String MSG_NO_ITEM_FOUND = "No item found or not enough time coverage for selection rule '%s' and time interval (%s, %s)";
	private static final String MSG_INVALID_ITEM_TYPE = "Item with different item type found ";
	private static final String MSG_CANNOT_CREATE_QUERY = "Cannot create query (cause: %s)";
	
	/**
	 * Processing mode, for which this selection rule is valid (level 7 "Mode" from Generic IPF Interface Specifications, sec. 4.1.3);
	 * the attribute is optional, its values are restricted by the processing modes defined for the mission.
	 */
	private String mode = null;
	
	/**
	 * Indicates whether the required source product is mandatory for the production of the target product
	 * (level 7 "Mandatory" from Generic IPF Interface Specifications, sec. 4.1.3)
	 */
	private Boolean isMandatory;
	
	/**
	 * Minimum percentage of coverage of the desired validity period for fulfilment of this rule (default 0)
	 */
	private Short minimumCoverage = 0;
	
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
	 * Gets the minimum percentage of coverage of the desired validity period
	 * 
	 * @return the minimumCoverage
	 */
	public Short getMinimumCoverage() {
		return minimumCoverage;
	}

	/**
	 * Sets the minimum percentage of coverage of the desired validity period
	 * 
	 * @param minimumCoverage the minimumCoverage to set
	 */
	public void setMinimumCoverage(Short minimumCoverage) {
		this.minimumCoverage = minimumCoverage;
	}

	/**
	 * Gets the additional filtering conditions
	 * 
	 * @return the filter conditions
	 */
	public Map<String, Parameter> getFilterConditions() {
		return filterConditions;
	}

	/**
	 * Sets the additional filtering conditions
	 * 
	 * @param filterConditions the filter conditions to set
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
		if (null == filteredSourceProductType) {
			StringBuilder fsptBuilder = new StringBuilder(sourceProductClass.getProductType());
			boolean first = true;
			for (String filterConditionKey: filterConditions.keySet()) {
				if (first) {
					first = false;
					fsptBuilder.append('/');
				} else {
					fsptBuilder.append(',');
				}
				fsptBuilder.append(filterConditionKey).append(':').append(filterConditions.get(filterConditionKey).toString());
			}
			filteredSourceProductType = fsptBuilder.toString();
		}
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
	 * Inner class for calculating time interval coverage
	 */
	private static class TimeInterval {
		private Instant start, stop;
		public TimeInterval(Instant start, Instant stop) { this.start = start; this.stop = stop; }
		public List<TimeInterval> cutOut(TimeInterval other) {
			List<TimeInterval> result = new ArrayList<>();
			if (!other.start.isBefore(this.stop) || !other.stop.isAfter(this.start)) {
				// No overlap
				result.add(this);
			} else if (other.start.isAfter(this.start)) {
				// No overlap at start
				result.add(new TimeInterval(this.start, other.start));
				if (other.stop.isBefore(this.stop)) {
					// Other interval cuts this one in two (no overlap at end)
					result.add(new TimeInterval(other.stop, this.stop));
				}
			} else {
				if (other.stop.isBefore(this.stop)) {
					// overlapping from start
					result.add(new TimeInterval(other.stop, this.stop));
				} // else: total eclipse
			}
			return result;
		}
	}
	
	/**
	 * Check whether the required minimum coverage of the requested time interval is reached with the given item objects.
	 * 
	 * @param itemObjectList the item objects to evaluate
	 * @param startTime the beginning of the requested time interval
	 * @param stopTime the end of the requested time interval
	 * @return true, if the minimum coverage percentage is reached, false otherwise
	 */
	/* For a possibly very efficient solution check https://stackoverflow.com/questions/1982409/data-structure-for-handling-intervals */
	private boolean hasSufficientCoverage(final Set<SelectionItem> items, final Instant startTime, final Instant stopTime) {
		// Fulfilled, if no coverage is required
		if (0 == minimumCoverage) {
			return true;
		}
		
		// We start the list with the requested time interval
		List<TimeInterval> residualIntervals = new ArrayList<>();
		residualIntervals.add(new TimeInterval(startTime, stopTime));
		
		// Cut out the time intervals of all items
		for (SelectionItem item: items) {
			List<TimeInterval> newResiduals = new ArrayList<>();
			for (TimeInterval oldResidual: residualIntervals) {
				newResiduals.addAll(oldResidual.cutOut(new TimeInterval(item.startTime, item.stopTime)));
			}
			if (newResiduals.isEmpty()) {
				// 100 % coverage
				return true;
			}
			residualIntervals = newResiduals;
		}
		
		// Sum up duration of residual intervals and compare to initial interval
		long initialDuration = Duration.between(startTime, stopTime).getSeconds();
		long residualDuration = 0;
		for (TimeInterval residualInterval: residualIntervals) {
			residualDuration += Duration.between(residualInterval.start, residualInterval.stop).getSeconds();
		}
		
		// Check required coverage
		return minimumCoverage <= ((initialDuration - residualDuration) * 100) / initialDuration;
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
	public List<Object> selectItems(final Collection<SelectionItem> items, final Instant startTime, final Instant stopTime)
			throws NoSuchElementException, IllegalArgumentException {
		Set<SelectionItem> selectedItems = new HashSet<>();
		
		// Check that all items conform to the product type of this rule
		for (SelectionItem item: items) {
			if (!item.itemType.equals(sourceProductClass.getProductType())) {
				throw new IllegalArgumentException(MSG_INVALID_ITEM_TYPE + item.itemType);
			}
		}
		
		// Iterate over all policies and test them against the item collection
		for (SimplePolicy policy: simplePolicies) {
			selectedItems.addAll(policy.selectItems(items, startTime, stopTime));
			if (!selectedItems.isEmpty() && hasSufficientCoverage(selectedItems, startTime, stopTime)) {
				// Short-circuited OR: first match(es) apply
				List<Object> itemObjectList = new ArrayList<>();
				for (SelectionItem item: selectedItems) itemObjectList.add(item.itemObject);
				return itemObjectList;
			}
		}
		// No or not enough matching items found
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
	
	
	/**
	 * Format this rule as an JPQL (Jave Persistence Query Language) query
	 * <p>
	 * Limitation: For LatestValidityClosest the query may return two products, one to each side of the centre of the
	 * given time interval. It is up to the calling program to select the applicable product.
	 * 
	 * @param startTime the start time to use in the database query
	 * @param stopTime the stop time to use in the database query
	 * @return an OQL string representing this rule
	 */
	public String asJpqlQuery(final Instant startTime, final Instant stopTime) {
		// Generate query projection
		StringBuilder simpleRuleQuery = new StringBuilder("select p from Product p ");

		// Join with as many instances of the product_parameters table as there are filter conditions
		for (int i = 0; i < filterConditions.size(); ++i) {
			simpleRuleQuery.append(String.format("join p.parameters pp%d ", i, i));
		}
		
		simpleRuleQuery.append("where p.productClass.id = ").append(sourceProductClass.getId()).append(" and ");
		
		// Ensure canonical ordering of policies
		simplePolicies.sort(new Comparator<SimplePolicy>() {
			@Override
			public int compare(SimplePolicy o1, SimplePolicy o2) {
				return o1.getPolicyType().compareTo(o2.getPolicyType());
			}});
		
		// Generate query condition
		
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
			simpleRuleQuery.append(simplePolicy.asJpqlQueryCondition(sourceProductClass, startTime, stopTime));
		}
		if (1 < simplePolicies.size()) {
			// Close parentheses for multiple policies
			simpleRuleQuery.append(")");
		}
		
		// Format filter conditions
		int i = 0;
		for (String filterKey: filterConditions.keySet()) {
			// If the key points to a class attribute, query the attribute value, otherwise query a parameter with this key
			try {
				Product.class.getDeclaredField(filterKey);
				simpleRuleQuery.append(
						String.format(" and p.%s = '%s'", filterKey, filterConditions.get(filterKey).getStringValue()));
			} catch (NoSuchFieldException e) {
				simpleRuleQuery.append(String.format(" and key(pp%d) = '%s' and pp%d.parameterValue = '%s'", 
						i, filterKey, i, filterConditions.get(filterKey).getStringValue()));
			} catch (SecurityException e) {
				throw new RuntimeException(String.format(MSG_CANNOT_CREATE_QUERY, e.getMessage()), e);
			}
			++i;
		}
		return simpleRuleQuery.toString();
	}
	
	/**
	 * Format this rule as a native SQL query
	 * <p>
	 * Limitation: For LatestValidityClosest the query may return two products, one to each side of the centre of the
	 * given time interval. It is up to the calling program to select the applicable product.
	 * 
	 * @param startTime the start time to use in the database query
	 * @param stopTime the stop time to use in the database query
	 * @return an OQL string representing this rule
	 */
	public String asSqlQuery(final Instant startTime, final Instant stopTime) {
		// Generate query projection
		StringBuilder simpleRuleQuery = new StringBuilder("SELECT * FROM product p ");
		
		// Join with as many instances of the product_parameters table as there are filter conditions
		for (int i = 0; i < filterConditions.size(); ++i) {
			simpleRuleQuery.append(String.format("JOIN product_parameters pp%d ON p.id = pp%d.product_id ", i, i));
		}
		
		// Select correct product class		
		simpleRuleQuery.append("WHERE p.product_class_id = ").append(sourceProductClass.getId()).append(" AND ");
		
		// Ensure canonical ordering of policies
		simplePolicies.sort(new Comparator<SimplePolicy>() {
			@Override
			public int compare(SimplePolicy o1, SimplePolicy o2) {
				return o1.getPolicyType().compareTo(o2.getPolicyType());
			}});
		
		// Generate query condition
		
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
				simpleRuleQuery.append(" OR ");
			simpleRuleQuery.append(simplePolicy.asSqlQueryCondition(sourceProductClass, startTime, stopTime));
		}
		if (1 < simplePolicies.size()) {
			// Close parentheses for multiple policies
			simpleRuleQuery.append(")");
		}
		
		// Format filter conditions
		int i = 0;
		for (String filterKey: filterConditions.keySet()) {
			// If the key points to a class attribute, query the attribute value, otherwise query a parameter with this key
			try {
				Product.class.getDeclaredField(filterKey);
				simpleRuleQuery.append(
						String.format(" AND p.%s = '%s'", filterKey, filterConditions.get(filterKey).getStringValue()));
			} catch (NoSuchFieldException e) {
				simpleRuleQuery.append(
						String.format(" AND pp%d.parameters_key = '%s' AND pp%d.parameter_value = '%s'", 
								i, filterKey, i, filterConditions.get(filterKey).getStringValue()));
			} catch (SecurityException e) {
				throw new RuntimeException(String.format(MSG_CANNOT_CREATE_QUERY, e.getMessage()), e);
			}
			++i;
		}

		return simpleRuleQuery.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder simpleRuleString = new StringBuilder("FOR ");
		simpleRuleString.append(this.getFilteredSourceProductType());
		simpleRuleString.append(" SELECT ");
		boolean first = true;
		for (SimplePolicy simplePolicy: simplePolicies) {
			if (first)
				first = false;
			else
				simpleRuleString.append(" OR ");
			simpleRuleString.append(simplePolicy.toString());
		}
		if (isMandatory) {
			if (0 < minimumCoverage) {
				simpleRuleString.append(" MINCOVER(" + minimumCoverage + ")");
			} else {
				simpleRuleString.append(" MANDATORY");
			}
		} else {
			simpleRuleString.append(" OPTIONAL");
		}
		return simpleRuleString.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(sourceProductClass, targetProductClass, mode);
		return result;
	}

	/**
	 * Test equality of selection rules based on source and target product classes and processing mode
	 * (i. e. there must be only one selection rule for the same source and target classes and the same processing mode).
	 * 
	 * @param obj the reference object with which to compare
	 * @return true if this object is the same as the obj argument according to the definition above; false otherwise
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
				&& Objects.equals(targetProductClass, other.targetProductClass)
				&& Objects.equals(mode, other.mode);
	}

}
