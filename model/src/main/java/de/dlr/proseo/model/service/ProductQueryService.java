/**
 * ProductQueryService.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.service;

import java.util.List;
import java.util.NoSuchElementException;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.util.SelectionItem;

/**
 * Service class to execute a product query on the prosEO database
 * 
 * @author Dr. Thomas Bassler
 */
@Service
public class ProductQueryService {
	
	/** JPA entity manager */
	@PersistenceContext
	EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductQueryService.class);
	
	/**
	 * Check whether the product query is optional, and set it to satisfied, if so
	 * 
	 * @param productQuery the product query to check
	 * @return true, if the product query is optional and therefore satisfied, false otherwise
	 */
	private boolean testOptionalSatisfied(ProductQuery productQuery) {
		if (productQuery.getGeneratingRule().isMandatory()) {
			productQuery.setIsSatisfied(false);
			return false;
		} else {
			productQuery.setIsSatisfied(true);
			return true;
		}
	}
	
	/**
	 * Execute the query of the given product query and check additional conditions (e. g. selection time interval coverage)
	 * 
	 * @param productQuery the product query to execute
	 * @return true, if the query is satisfied (its list of satisfying products will then be set), false otherwise
	 * @throws IllegalArgumentException if the product query is incomplete
	 */
	public boolean executeQuery(ProductQuery productQuery, boolean useNativeSQL) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> executeJpqlQuery()");
		
		// Check arguments
		if (null == productQuery || null == productQuery.getGeneratingRule() || null == productQuery.getJpqlQueryCondition()) {
			String message = String.format("Incomplete product query %s", productQuery.toString());
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		
		// Execute the query
		Query query = null;
		if (useNativeSQL) {
			String sqlQuery = productQuery.getJpqlQueryCondition();
			if (logger.isDebugEnabled()) logger.debug("Executing SQL query: " + sqlQuery);
			query = em.createNativeQuery(sqlQuery, Product.class);
		} else {
			String jpqlQuery = productQuery.getJpqlQueryCondition();
			if (logger.isDebugEnabled()) logger.debug("Executing JPQL query: " + jpqlQuery);
			query = em.createQuery(jpqlQuery, Product.class);
		}
		query.setLockMode(LockModeType.READ);
		@SuppressWarnings("unchecked")
		List<Product> products = query.getResultList();
		
		// Check if there is any result at all
		if (products.isEmpty()) {
			if (logger.isTraceEnabled()) logger.trace("<<< executeJpqlQuery()");
			return testOptionalSatisfied(productQuery);
		}
		
		// Check if all conditions of the selection rule are met
		List<Object> selectedItems = null;
		try {
			selectedItems = productQuery.getGeneratingRule().selectItems(
					SelectionItem.asSelectionItems(products), 
					productQuery.getJobStep().getJob().getStartTime(),
					productQuery.getJobStep().getJob().getStopTime());
		} catch (NoSuchElementException e) {
			if (logger.isTraceEnabled()) logger.trace("<<< executeJpqlQuery()");
			return testOptionalSatisfied(productQuery);
		}
		
		// Set the query's list of satisfying products to the list of selected items (products)
		for (Object selectedItem: selectedItems) {
			if (selectedItem instanceof Product) {
				productQuery.getSatisfyingProducts().add((Product) selectedItem);
			}
		}
		productQuery.setIsSatisfied(true);
		
		if (logger.isTraceEnabled()) logger.trace("<<< executeJpqlQuery()");
		return true;
	}
	
	/**
	 * Execute the JPQL query of the given product query and check additional conditions (e. g. selection time interval coverage)
	 * 
	 * @param productQuery the product query to execute
	 * @return true, if the query is satisfied (its list of satisfying products will then be set), false otherwise
	 * @throws IllegalArgumentException if the product query is incomplete
	 */
	public boolean executeJpqlQuery(ProductQuery productQuery) throws IllegalArgumentException {
		return executeQuery(productQuery, false);
	}

	/**
	 * Execute the native SQL query of the given product query and check additional conditions (e. g. selection time interval coverage)
	 * 
	 * @param productQuery the product query to execute
	 * @return true, if the query is satisfied (its list of satisfying products will then be set), false otherwise
	 * @throws IllegalArgumentException if the product query is incomplete
	 */
	public boolean executeSqlQuery(ProductQuery productQuery) throws IllegalArgumentException {
		return executeQuery(productQuery, true);
	}

}
