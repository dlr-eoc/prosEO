/**
 * ProductQuery.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;


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
	@ManyToOne(fetch = FetchType.LAZY)
	private JobStep jobStep;
	
	/**
	 * The selection rule, from which this query was derived
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	private SimpleSelectionRule generatingRule;
	
	/**
	 * The product class requested by the selection rule
	 */
	@ManyToOne(fetch = FetchType.LAZY)
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
	 * 
	 * Deprecated, use getGeneratingRule().getMinimumCoverage() instead
	 */
	@Deprecated
	private Short minimumCoverage = 0;
	
	/** Indicates whether this query is fully satisfied by the satisfying products. */
	private Boolean isSatisfied = false;

	/** Indicates whether possible satisfying products are in download. */
	private Boolean inDownload = false;
	
	/**
	 * Products satisfying this query condition
	 */
	@ManyToMany
	private Set<Product> satisfyingProducts = new HashSet<>();

	/**
	 * The hashCode() method is often called (during add to a HashSet) and the calculation needs a lot of effort.
	 * Thus the hash code is cached and only new calculated after calling a setter method.
	 */
	@Transient
	private int hashCache = 0;

	/**
	 * Create a product query from a simple selection rule for a given job step
	 * 
	 * @param selectionRule the selection rule to create the product query from
	 * @param jobStep the job step to generate the product query for
	 * @param productColumnMapping a mapping from attribute names of the Product class to the corresponding SQL column names
	 * @param facilityQuerySql an SQL selection string to add to the selection rule SQL query
	 * @param facilityQuerySqlSubselect an SQL selection string to add to sub-SELECTs in selection policy SQL query conditions
	 * @return a product query object
	 */
	public static ProductQuery fromSimpleSelectionRule(SimpleSelectionRule selectionRule, JobStep jobStep,
			Map<String, String> productColumnMapping, String facilityQuerySql, String facilityQuerySqlSubselect) {
		
		ProductQuery productQuery = new ProductQuery();
		productQuery.generatingRule = selectionRule;
		productQuery.jobStep = jobStep;
		productQuery.requestedProductClass = selectionRule.getSourceProductClass();
		InputFilter inputFilter = jobStep.getJob().getProcessingOrder().getInputFilters().get(selectionRule.getSourceProductClass());
		if (null != inputFilter) {
			productQuery.filterConditions.putAll(inputFilter.getFilterConditions());
		}
		productQuery.jpqlQueryCondition = selectionRule.asJpqlQuery(
				jobStep.getJob().getStartTime(), jobStep.getJob().getStopTime(), productQuery.filterConditions);
		productQuery.sqlQueryCondition = selectionRule.asSqlQuery(
				jobStep.getJob().getStartTime(), jobStep.getJob().getStopTime(), productQuery.filterConditions,
				productColumnMapping, facilityQuerySql, facilityQuerySqlSubselect);
		productQuery.calcHash();
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
		calcHash();
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
		calcHash();
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
		calcHash();
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
		calcHash();
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
		calcHash();
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
		calcHash();
	}

	/**
	 * Gets the minimum percentage of coverage of the desired validity period
	 * 
	 * Deprecated, use getGeneratingRule().getMinimumCoverage() instead
	 * 
	 * @return the minimumCoverage
	 */
	@Deprecated
	public Short getMinimumCoverage() {
		return minimumCoverage;
	}

	/**
	 * Sets the minimum percentage of coverage of the desired validity period
	 * 
	 * Deprecated, use getGeneratingRule().setMinimumCoverage() instead
	 * 
	 * @param minimumCoverage the minimumCoverage to set
	 */
	@Deprecated
	public void setMinimumCoverage(Short minimumCoverage) {
		this.minimumCoverage = minimumCoverage;
		calcHash();
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
	 * @return the inDownload
	 */
	public Boolean getInDownload() {
		return inDownload;
	}

	/**
	 * @param inDownload the inDownload to set
	 */
	public void setInDownload(Boolean inDownload) {
		this.inDownload = inDownload;
	}

	/**
	 * Indicate that this product query is satisfied
	 * 
	 * @param isSatisfied the isSatisfied to set
	 */
	public void setIsSatisfied(Boolean isSatisfied) {
		this.isSatisfied = isSatisfied;
		calcHash();
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
	 * This should not be necessary. If "newest" is intended, then the correct selection method (e. g. LatestValidity) should
	 * be used!
	 * 
	 * @return the newestProduct
	 */
//	public List<Product> getNewestSatisfyingProducts() {
//		HashMap<Instant, HashMap<Instant, Product>> newestProducts = new HashMap<Instant, HashMap<Instant, Product>>();
//		for (Product p : satisfyingProducts) {
//			if ((p.getProductFile() != null && !p.getProductFile().isEmpty())
//					|| !p.getComponentProducts().isEmpty()) {
//				if (newestProducts.get(p.getSensingStartTime()) == null) {
//					HashMap<Instant, Product> stopMap = new HashMap<Instant, Product>();
//					stopMap.put(p.getSensingStopTime(), p);
//					newestProducts.put(p.getSensingStartTime(), stopMap);
//				} else {
//					Product product = newestProducts.get(p.getSensingStartTime()).get(p.getSensingStopTime());
//					if (   product != null) {
//						if (product.getGenerationTime() != null 	
//								&& p.getGenerationTime() != null 
//								&& p.getGenerationTime().isAfter(product.getGenerationTime())) {
//							newestProducts.get(p.getSensingStartTime()).replace(p.getSensingStartTime(), p);
//						}
//					} else {
//						newestProducts.get(p.getSensingStartTime()).put(p.getSensingStopTime(), p);
//					}
//				}
//			}
//		}
//		List<Product> retProducts = new ArrayList<Product>();
//		for (HashMap<Instant, Product> map : newestProducts.values()) {
//			retProducts.addAll(map.values());
//		}
//		return retProducts;
//	}

	/**
	 * Set the products satisfying this query
	 * 
	 * @param satisfyingProducts the satisfyingProducts to set
	 */
	public void setSatisfyingProducts(Set<Product> satisfyingProducts) {
		this.satisfyingProducts = satisfyingProducts;
		calcHash();
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
				if (!filterField.canAccess(product)) {
					filterField.setAccessible(true);
				}
				Object productField = filterField.get(product);
				if (null == productField) {
					// A "null" field never matches
					success = false;
					break;
				}
				success = success && filterConditions.get(filterKey).getParameterValue().equals(productField.toString());
			} catch (NoSuchFieldException e) {
				success = success &&  filterConditions.get(filterKey).equals(product.getParameters().get(filterKey));
			} catch (SecurityException | IllegalAccessException e) {
				throw new RuntimeException(String.format(MSG_CANNOT_ACCESS_PRODUCT_FIELD, filterKey, e.getMessage()), e);
			}
		}
		
		return success;
	}
	
	/**
	 * Calculate the hashCode and store it in the hashCache
	 */
	private void calcHash() {
		hashCache = Objects.hash(jobStep, requestedProductClass);
	}

	@Override
	public int hashCode() {
		if (hashCache == 0) {
			// this is usually called after the object is read from DB.
			calcHash();
		}
		return hashCache;
	}

	@Override
	public boolean equals(Object obj) {
		// Object identity
		if (this == obj)
			return true;
		
		// Same database object
		if (super.equals(obj))
			return true;
		
		if (!(obj instanceof ProductQuery))
			return false;
		ProductQuery other = (ProductQuery) obj;
		return Objects.equals(jobStep, other.getJobStep()) && Objects.equals(requestedProductClass, other.getRequestedProductClass());
	}

	@Override
	public String toString() {
		return "ProductQuery [jobStep=" + jobStep + ", generatingRule=" + generatingRule + ", requestedProductClass="
				+ requestedProductClass + ", jpqlQueryCondition=" + jpqlQueryCondition + ", sqlQueryCondition=" + sqlQueryCondition
				+ ", outputParameters=" + filterConditions + ", minimumCoverage=" + minimumCoverage + ", isSatisfied=" + isSatisfied
				+ ", satisfyingProducts=" + satisfyingProducts + "]";
	}
}
