/**
 * ProductQuery.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

/**
 * A ProductQuery models the need of a JobStep to use a Product of a certain ProductClass for a specific time period.
 * The time period can be defined by conditions, and a Product is said to satisfy a ProductQuery, if it matches the given
 * conditions. A JobStep can be executed as soon as all its ProductQuerys are satisfied.
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class ProductQuery extends PersistentObject {

	/** Job step issuing this query */
	@ManyToOne
	private JobStep jobStep;
	
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
	
	/** Indicates whether this query is fully satisfied by the satisfying products. */
	private Boolean isSatisfied;
	
	/**
	 * Products satisfying this query condition
	 */
	@ManyToMany(mappedBy = "satisfiedProductQueries")
	private Set<Product> satisfyingProducts = new HashSet<>();

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
	 * Set the products satisfying this query
	 * 
	 * @param satisfyingProducts the satisfyingProducts to set
	 */
	public void setSatisfyingProducts(Set<Product> satisfyingProducts) {
		this.satisfyingProducts = satisfyingProducts;
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
}
