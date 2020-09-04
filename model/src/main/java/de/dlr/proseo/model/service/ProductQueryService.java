/**
 * ProductQueryService.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
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
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductQueryService.class);
	
	/**
	 * Check whether the product query is optional, and set it to satisfied, if so
	 * 
	 * @param productQuery the product query to check
	 * @return true, if the product query is optional and therefore satisfied, false otherwise
	 */
	private boolean testOptionalSatisfied(ProductQuery productQuery, boolean checkOnly) {
		if (productQuery.getGeneratingRule().isMandatory()) {
			productQuery.setIsSatisfied(false);
			return false;
		} else {
			productQuery.setIsSatisfied(true);
			return true;
		}
	}
	
	/**
	 * Execute the query of the given product query and check additional conditions (e. g. selection time interval coverage).
	 * If successful, the query and its satisfying products are updated (these updates must be persisted by the calling method).
	 * 
	 * @param productQuery the product query to execute
	 * @param useNativeSQL set to true, if native SQL is to be used, and to false for JPQL
	 * @param checkOnly if true, checks satisfaction, but does not store satisfying products, if false, will store satisfying products
	 *            for future reference
	 * @return true, if the query is satisfied (its list of satisfying products will then be set), false otherwise
	 * @throws IllegalArgumentException if the product query is incomplete
	 */
	public boolean executeQuery(ProductQuery productQuery, boolean useNativeSQL, boolean checkOnly) throws IllegalArgumentException {
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
			products = query.getResultList();
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Number of products found: " + products.size());
			for (Product product: products) {
				logger.trace("Found product {} with start time = {}, stop time = {} and revision = {}", 
						product.getId(), product.getSensingStartTime().toString(), product.getSensingStopTime().toString(),
						product.getParameters().get("revision") != null 
							? product.getParameters().get("revision").getParameterValue().toString()
							: "");
			}
		}
		
		// Check if there is any result at all
		if (products.isEmpty()) {
			if (logger.isTraceEnabled()) logger.trace("<<< executeQuery()");
			return testOptionalSatisfied(productQuery, checkOnly);
		}
		
		// Filter products available at the requested processing facility
		ProcessingFacility facility = productQuery.getJobStep().getJob().getProcessingFacility();
		List<Product> productsAtFacility = new ArrayList<>();
		getProductsAtFacility(productsAtFacility, products, facility, false);
		products = productsAtFacility;
		
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
			return testOptionalSatisfied(productQuery, checkOnly);
		}
		if (logger.isTraceEnabled()) logger.trace("Number of products after selection: " + selectedItems.size());
		
		// Check if the additional filter conditions of the job step are met
		Set<Product> selectedProducts = new HashSet<>();
		for (Object selectedItem: selectedItems) {
			if (selectedItem instanceof Product) {
				Product product = (Product) selectedItem;
				if ((product.getProductFile() != null && !product.getProductFile().isEmpty())
						|| !product.getComponentProducts().isEmpty()) {
					if (productQuery.testFilterConditions(product)) {
						selectedProducts.add(product);
					} else {
						if (logger.isTraceEnabled()) logger.trace(product.toString() + " does not meet filter conditions");
					}
				} else {
					logger.info(product.toString() + ": product files are empty");
				}
			}
		}
		if (selectedProducts.isEmpty()) {
			if (logger.isTraceEnabled()) logger.trace("<<< executeQuery()");
			return testOptionalSatisfied(productQuery, checkOnly);
		}
		if (logger.isTraceEnabled()) logger.trace("Number of products after testing filter conditions: " + selectedProducts.size());

		if (!checkOnly) {
			// Set the query's list of satisfying products to the list of selected items (products)
			productQuery.getSatisfyingProducts().clear();
			for (Product product: selectedProducts) {
				product.getSatisfiedProductQueries().add(productQuery);
				productQuery.getSatisfyingProducts().add(product);
			}
			productQuery.setIsSatisfied(true);
		}		
		if (logger.isTraceEnabled()) logger.trace("<<< executeQuery()");
		return true;
	}
	
	private boolean getProductsAtFacility(List<Product> productsAtFacility, List<Product> products, ProcessingFacility facility, Boolean inComponent) {
		Boolean answer = true; 
		for (Product product: products) {
			if (product.getComponentProducts().isEmpty()) { 
				Boolean found = false;
				for (ProductFile productFile: product.getProductFile()) {
					if (facility.equals(productFile.getProcessingFacility())) {
						if (!inComponent) {
							productsAtFacility.add(product);
						}
						found = true;
						break;
					}
				}
				answer &= found; 
			} else {
				List<Product> componentProducts = new ArrayList<Product>();
				componentProducts.addAll(product.getComponentProducts());
				answer &= getProductsAtFacility(productsAtFacility, componentProducts, facility, true);
				if (answer) {
					productsAtFacility.add(product);
				}
			}
		}
		return answer;
	}
	
	/**
	 * Execute the JPQL query of the given product query and check additional conditions (e. g. selection time interval coverage)
	 * 
	 * @param productQuery the product query to execute
	 * @param checkOnly if true, checks satisfaction, but does not store satisfying products, if false, will store satisfying products
	 *            for future reference
	 * @return true, if the query is satisfied (its list of satisfying products will then be set), false otherwise
	 * @throws IllegalArgumentException if the product query is incomplete
	 */
	public boolean executeQuery(ProductQuery productQuery, boolean checkOnly) throws IllegalArgumentException {
		return executeQuery(productQuery, false, checkOnly);
	}

	/**
	 * Execute the native SQL query of the given product query and check additional conditions (e. g. selection time interval coverage)
	 * 
	 * @param productQuery the product query to execute
	 * @param checkOnly if true, checks satisfaction, but does not store satisfying products, if false, will store satisfying products
	 *            for future reference
	 * @return true, if the query is satisfied (its list of satisfying products will then be set), false otherwise
	 * @throws IllegalArgumentException if the product query is incomplete
	 */
	public boolean executeSqlQuery(ProductQuery productQuery, boolean checkOnly) throws IllegalArgumentException {
		return executeQuery(productQuery, true, checkOnly);
	}

}
