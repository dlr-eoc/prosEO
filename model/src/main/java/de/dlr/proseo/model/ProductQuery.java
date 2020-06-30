/**
 * ProductQuery.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A ProductQuery models the need of a JobStep to use a Product of a certain ProductClass for a specific time period.
 * The time period can be defined by conditions, and a Product is said to satisfy a ProductQuery, if it matches the given
 * conditions. A JobStep can be executed as soon as all its ProductQuerys are satisfied.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
@Table(indexes = { 
		@Index(unique = true, columnList = "job_step_id, requested_product_class_id"), 
		@Index(columnList = "requested_product_class_id") } )
public class ProductQuery extends PersistentObject {

	/* Static message strings */
	private static final String MSG_CANNOT_ACCESS_PRODUCT_FIELD = "Cannot access product field %s (cause: %s)";

	/** Job step issuing this query */
	@ManyToOne
	private JobStep jobStep;
	
	/**
	 * The selection rule, from which this query was derived
	 */
	@ManyToOne
	private SimpleSelectionRule generatingRule;
	
	/**
	 * The product class requested by the selection rule
	 */
	@ManyToOne
	private ProductClass requestedProductClass;
	
	/**
	 * The product query as a JPQL (Java Persistence Query Language) query condition (if set, sqlQueryCondition must not be set)
	 * Note: The annotated SQL datatype "TEXT" is not a standard SQL datatype (defined for MySQL and PostgreSQL)
	 */
	@Column(columnDefinition = "TEXT")
	private String jpqlQueryCondition;
	
	/**
	 * The product query as a native SQL query condition (if set, jpqlQueryCondition must not be set)
	 * Note: The annotated SQL datatype "TEXT" is not a standard SQL datatype (defined for MySQL and PostgreSQL)
	 */
	@Column(columnDefinition = "TEXT")
	private String sqlQueryCondition;
	
	/**
	 * Additional filter conditions to apply to the selected products.
	 */
	@ElementCollection
	private Map<String, Parameter> filterConditions = new HashMap<>();
	
	/**
	 * Minimum percentage of coverage of the desired validity period for fulfilment of this query
	 */
	private Short minimumCoverage = 0;
	
	/** Indicates whether this query is fully satisfied by the satisfying products. */
	private Boolean isSatisfied = false;
	
	/**
	 * Products satisfying this query condition
	 */
	@ManyToMany(mappedBy = "satisfiedProductQueries")
	private Set<Product> satisfyingProducts = new HashSet<>();

	/**
	 * Create a product query from a simple selection rule for a given job step
	 * 
	 * @param selectionRule the selection rule to create the product query from
	 * @param jobStep the job step to generate the product query for
	 * @return a product query object
	 */
	public static ProductQuery fromSimpleSelectionRule(SimpleSelectionRule selectionRule, JobStep jobStep) {
		ProductQuery productQuery = new ProductQuery();
		productQuery.generatingRule = selectionRule;
		productQuery.jobStep = jobStep;
		productQuery.requestedProductClass = selectionRule.getSourceProductClass();
		productQuery.jpqlQueryCondition = selectionRule.asJpqlQuery(jobStep.getJob().getStartTime(), jobStep.getJob().getStopTime());
		productQuery.sqlQueryCondition = selectionRule.asSqlQuery(jobStep.getJob().getStartTime(), jobStep.getJob().getStopTime());
		productQuery.filterConditions.putAll(
				jobStep.getJob().getProcessingOrder().getInputFilters().get(selectionRule.getSourceProductClass()).getFilterConditions());
		
		return productQuery;
	}
	
	/**
	 * Gets the job step issuing the query
	 * 
	 * @return the jobStep
	 */
	public JobStep getJobStep() {
		return jobStep;
	}

	/**
	 * Sets the job step issuing the query
	 * 
	 * @param jobStep the jobStep to set
	 */
	public void setJobStep(JobStep jobStep) {
		this.jobStep = jobStep;
	}

	/**
	 * Gets the selection rule that generated this product query
	 * 
	 * @return the generatingRule
	 */
	public SimpleSelectionRule getGeneratingRule() {
		return generatingRule;
	}

	/**
	 * Sets the selection rule that generated this product query
	 * 
	 * @param generatingRule the generatingRule to set
	 */
	public void setGeneratingRule(SimpleSelectionRule generatingRule) {
		this.generatingRule = generatingRule;
	}

	/**
	 * Gets the requested product class
	 * 
	 * @return the requestedProductClass
	 */
	public ProductClass getRequestedProductClass() {
		return requestedProductClass;
	}

	/**
	 * Sets the requested product class
	 * 
	 * @param requestedProductClass the requestedProductClass to set
	 */
	public void setRequestedProductClass(ProductClass requestedProductClass) {
		this.requestedProductClass = requestedProductClass;
	}

	/**
	 * Gets the JPQL query condition
	 * 
	 * @return the jpqlQueryCondition
	 */
	public String getJpqlQueryCondition() {
		return jpqlQueryCondition;
	}

	/**
	 * Sets the JPQL query condition
	 * 
	 * @param jpqlQueryCondition the jpqlQueryCondition to set
	 */
	public void setJpqlQueryCondition(String jpqlQueryCondition) {
		this.jpqlQueryCondition = jpqlQueryCondition;
	}

	/**
	 * Gets the SQL query condition
	 * 
	 * @return the sqlQueryCondition
	 */
	public String getSqlQueryCondition() {
		return sqlQueryCondition;
	}

	/**
	 * Sets the SQL query condition
	 * 
	 * @param sqlQueryCondition the sqlQueryCondition to set
	 */
	public void setSqlQueryCondition(String sqlQueryCondition) {
		this.sqlQueryCondition = sqlQueryCondition;
	}

	/**
	 * Gets the additional filtering conditions
	 * 
	 * @return the outputParameters
	 */
	public Map<String, Parameter> getFilterConditions() {
		return filterConditions;
	}

	/**
	 * Sets the additional filtering conditions
	 * 
	 * @param outputParameters the outputParameters to set
	 */
	public void setFilterConditions(Map<String, Parameter> filterConditions) {
		this.filterConditions = filterConditions;
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
	 * Check whether this product query is satisfied
	 * 
	 * @return the isSatisfied
	 */
	public Boolean getIsSatisfied() {
		return isSatisfied;
	}

	/**
	 * Check whether this product query is satisfied (convenience alias for getIsSatisfied())
	 * 
	 * @return the isSatisfied
	 */
	public Boolean isSatisfied() {
		return this.getIsSatisfied();
	}

	/**
	 * Indicate that this product query is satisfied
	 * 
	 * @param isSatisfied the isSatisfied to set
	 */
	public void setIsSatisfied(Boolean isSatisfied) {
		this.isSatisfied = isSatisfied;
	}

	/**
	 * Get the products satisfying this query
	 * 
	 * @return the satisfyingProducts
	 */
	public Set<Product> getSatisfyingProducts() {
		return satisfyingProducts;
	}

	/**
	 * Get the newest products satisfying this query
	 * 
	 * @return the newestProduct
	 */
	public List<Product> getNewestSatisfyingProducts() {
		HashMap<Instant, HashMap<Instant, Product>> newestProducts = new HashMap<Instant, HashMap<Instant, Product>>();
		for (Product p : satisfyingProducts) {
			if ((p.getProductFile() != null && !p.getProductFile().isEmpty())
					|| !p.getComponentProducts().isEmpty()) {
				if (newestProducts.get(p.getSensingStartTime()) == null) {
					HashMap<Instant, Product> stopMap = new HashMap<Instant, Product>();
					stopMap.put(p.getSensingStopTime(), p);
					newestProducts.put(p.getSensingStartTime(), stopMap);
				} else {
					Product product = newestProducts.get(p.getSensingStartTime()).get(p.getSensingStopTime());
					if (   product != null) {
						if (product.getGenerationTime() != null 	
								&& p.getGenerationTime() != null 
								&& p.getGenerationTime().isAfter(product.getGenerationTime())) {
							newestProducts.get(p.getSensingStartTime()).replace(p.getSensingStartTime(), p);
						}
					} else {
						newestProducts.get(p.getSensingStartTime()).put(p.getSensingStopTime(), p);
					}
				}
			}
		}
		List<Product> retProducts = new ArrayList<Product>();
		for (HashMap<Instant, Product> map : newestProducts.values()) {
			retProducts.addAll(map.values());
		}
		return retProducts;
	}

	/**
	 * Set the products satisfying this query
	 * 
	 * @param satisfyingProducts the satisfyingProducts to set
	 */
	public void setSatisfyingProducts(Set<Product> satisfyingProducts) {
		this.satisfyingProducts = satisfyingProducts;
	}
	
	/**
	 * Test whether the given product satisfies the filter conditions of this query
	 * 
	 * @param product the product to test
	 * @return true, if all filter conditions are met, false otherwise
	 */
	public boolean testFilterConditions(Product product) {
		boolean success = true;
		
		for (String filterKey: filterConditions.keySet()) {
			try {
				Field filterField = Product.class.getDeclaredField(filterKey);
				Object productField = filterField.get(product);
				success = success && filterConditions.get(filterKey).getParameterValue().equals(productField.toString());
			} catch (NoSuchFieldException e) {
				success = success &&  filterConditions.get(filterKey).equals(product.getParameters().get(filterKey));
			} catch (SecurityException | IllegalAccessException e) {
				throw new RuntimeException(String.format(MSG_CANNOT_ACCESS_PRODUCT_FIELD, filterKey, e.getMessage()), e);
			}
		}
		
		return success;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(jobStep, requestedProductClass);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ProductQuery))
			return false;
		ProductQuery other = (ProductQuery) obj;
		return Objects.equals(jobStep, other.jobStep) && Objects.equals(requestedProductClass, other.requestedProductClass);
	}

	@Override
	public String toString() {
		return "ProductQuery [jobStep=" + jobStep + ", generatingRule=" + generatingRule + ", requestedProductClass="
				+ requestedProductClass + ", jpqlQueryCondition=" + jpqlQueryCondition + ", sqlQueryCondition=" + sqlQueryCondition
				+ ", outputParameters=" + filterConditions + ", minimumCoverage=" + minimumCoverage + ", isSatisfied=" + isSatisfied
				+ ", satisfyingProducts=" + satisfyingProducts + "]";
	}
}
