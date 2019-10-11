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
import javax.persistence.TypedQuery;

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
		if (logger.isTraceEnabled()) logger.trace(">>> executeQuery(useNativeSQL = " + useNativeSQL + ")");

		if (logger.isTraceEnabled()) logger.trace("Number of products in database: " + RepositoryService.getProductRepository().count());
		
		// Check arguments
		if (null == productQuery || null == productQuery.getGeneratingRule() || null == productQuery.getJpqlQueryCondition()) {
			String message = String.format("Incomplete product query %s", productQuery.toString());
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		
		// Execute the query
		List<Product> products = null;
		if (useNativeSQL) {
			String sqlQuery = productQuery.getSqlQueryCondition();
			if (logger.isDebugEnabled()) logger.debug("Executing SQL query: " + sqlQuery);
			Query query = em.createNativeQuery(sqlQuery, Product.class);
			@SuppressWarnings("unchecked")
			List<Product> uncheckedProducts = (List<Product>) query.getResultList(); // Only to restrict scope of @SuppressWarnings("unchecked")
			products = uncheckedProducts;
		} else {
			String jpqlQuery = productQuery.getJpqlQueryCondition();
			if (logger.isDebugEnabled()) logger.debug("Executing JPQL query: " + jpqlQuery);
			TypedQuery<Product> query = em.createQuery(jpqlQuery, Product.class);
			query.setLockMode(LockModeType.READ);
			products = query.getResultList();
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Number of products found: " + products.size());
			for (Product product: products) {
				logger.trace("Found product {} with start time = {}, stop time = {} and revision = {}", 
						product.getId(), product.getSensingStartTime().toString(), product.getSensingStopTime().toString(),
						product.getParameters().get("revision").getParameterValue().toString());
			}
		}
		
		// Check if there is any result at all
		if (products.isEmpty()) {
			if (logger.isTraceEnabled()) logger.trace("<<< executeQuery()");
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
			if (logger.isTraceEnabled()) logger.trace("selectItems() throws NoSuchElementException");
			if (logger.isTraceEnabled()) logger.trace("<<< executeQuery()");
			return testOptionalSatisfied(productQuery);
		}
		if (logger.isTraceEnabled()) logger.trace("Number of products after selection: " + selectedItems.size());
		
		// Set the query's list of satisfying products to the list of selected items (products)
		for (Object selectedItem: selectedItems) {
			if (selectedItem instanceof Product) {
				productQuery.getSatisfyingProducts().add((Product) selectedItem);
			}
		}
		productQuery.setIsSatisfied(true);
		
		if (logger.isTraceEnabled()) logger.trace("<<< executeQuery()");
		return true;
	}
	
	/**
	 * Execute the JPQL query of the given product query and check additional conditions (e. g. selection time interval coverage)
	 * 
	 * @param productQuery the product query to execute
	 * @return true, if the query is satisfied (its list of satisfying products will then be set), false otherwise
	 * @throws IllegalArgumentException if the product query is incomplete
	 */
	public boolean executeQuery(ProductQuery productQuery) throws IllegalArgumentException {
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
