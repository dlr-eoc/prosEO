/**
 * ProductQueryService.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.metamodel.Metamodel;

import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.BasicType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.ProcessingFacility;
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
	
	/* Used by the Production Planner for the creation of product queries */
	public static final String FACILITY_QUERY_SQL =
			" AND :facility_id IN (SELECT processing_facility_id FROM product_processing_facilities ppf WHERE ppf.product_id = p.id)";
	public static final String FACILITY_QUERY_SQL_SUBSELECT = FACILITY_QUERY_SQL.replace("ppf", "ppf2").replace("p.id", "p2.id");

	/** Mapping from Product attributes to SQL column names */
	// Used by the Production Planner for the creation of product queries
	private Map<String, String> productColumnMapping = new HashMap<>();
	
	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductQueryService.class);
	
	/**
	 * When creating the ProductQueryService, fill the mapping of Product attributes to SQL column names
	 */
	@PostConstruct
	private void init() {
		if (logger.isTraceEnabled()) logger.trace(">>> init()");
		
		Metamodel metamodel = em.getMetamodel();
		
		if (metamodel instanceof MetamodelImplementor) {
			EntityPersister persister = ((MetamodelImplementor) metamodel).entityPersister(Product.class.getName());
			if (persister instanceof AbstractEntityPersister) {
				AbstractEntityPersister aep = (AbstractEntityPersister) persister;
				String[] propertyNames = aep.getPropertyNames();
				if (logger.isTraceEnabled()) logger.trace("Found {} properties for class {}", propertyNames.length, Product.class.getName());
				for (int i = 0; i < propertyNames.length; ++i) {
					if (aep.getPropertyType(propertyNames[i]) instanceof BasicType) {
						String[] columnNames = aep.getPropertyColumnNames(propertyNames[i]);
						if (1 != columnNames.length) {
							logger.warn("Found {} columns for property {}", columnNames.length, propertyNames[i]);
						}
						if (logger.isTraceEnabled()) logger.trace("... mapping Product attribute {} to SQL column {}",
								propertyNames[i], columnNames[0]);
						productColumnMapping.put(propertyNames[i], columnNames[0]);
					} else {
						if (logger.isTraceEnabled()) logger.trace("Skipping non-basic property {}", propertyNames[i]);
					}
				}
			} else {
				logger.error("Cannot generate attribute/column map: 'persister' is not an AbstractEntityPersister");
			}
		} else {
			logger.error("Cannot generate attribute/column map: 'metamodel' is not a MetamodelImplementor");
		}
	}
	
	/**
	 * Provides a mapping of Product attributes to SQL column names
	 * 
	 * @return the attribute mapping
	 */
	public Map<String, String> getProductColumnMapping() {
		return productColumnMapping;
	}
	
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
	 * @param checkOnly if true, checks satisfaction, but does not store satisfying products, if false, will store satisfying products
	 *            for future reference
	 * @return true, if the query is satisfied (its list of satisfying products will then be set), false otherwise
	 * @throws IllegalArgumentException if the product query is incomplete
	 */
	public boolean executeQuery(ProductQuery productQuery, boolean checkOnly) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> executeQuery({}, {})", (null == productQuery ? null : productQuery.getId()), checkOnly);

		if (logger.isTraceEnabled()) logger.trace("Number of products in database: " + RepositoryService.getProductRepository().count());
		
		// Check arguments
		if (null == productQuery || null == productQuery.getGeneratingRule() || null == productQuery.getSqlQueryCondition()) {
			String message = String.format("Incomplete product query %s", null == productQuery ? "null" : productQuery.toString());
			logger.error(message);
			throw new IllegalArgumentException(message);
		}

		// Determine the requested processing facility
		Job job = productQuery.getJobStep().getJob();
		ProcessingFacility facility = job.getProcessingFacility();
		
		// Execute the query (native SQL due to use of recursive SQL view product_processing_facilities)
		String sqlQuery = productQuery.getSqlQueryCondition();
		if (logger.isDebugEnabled()) logger.debug("Executing SQL query: " + sqlQuery);
		
		Query query = em.createNativeQuery(sqlQuery, Product.class);
		query.setParameter("facility_id", facility.getId());
		
		Instant queryStart = Instant.now();
		List<?> queryResult = (List<?>) query.getResultList();
		if (logger.isTraceEnabled()) 
			logger.trace("... product selection query returned {} products in {} ms", queryResult.size(), Duration.between(queryStart, Instant.now()).toMillis());
		
		List<Product> products = new ArrayList<>();
		for (Object resultObject: queryResult) {
			if (resultObject instanceof Product) {
				products.add((Product) resultObject);
			}
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
		
		// Check if all conditions of the selection rule are met (this may reduce the output in some cases, where the 
		// SQL command deliberately returns a greater number of products than expected, and it may turn out that the
		// expected coverage of the time interval is not met)
		List<Object> selectedItems = null;
		try {
			selectedItems = productQuery.getGeneratingRule().selectItems(
					SelectionItem.asSelectionItems(products), job.getStartTime(), job.getStopTime());
			if (null == selectedItems) {
				// No items selected, but rule is optional
				if (logger.isTraceEnabled()) logger.trace("<<< executeQuery()");
				return testOptionalSatisfied(productQuery, checkOnly);
			}
		} catch (NoSuchElementException e) {
			// No items selected and rule is mandatory
			if (logger.isTraceEnabled()) logger.trace("selectItems() throws NoSuchElementException");
			if (logger.isTraceEnabled()) logger.trace("<<< executeQuery()");
			return false;
		}
		if (logger.isTraceEnabled()) logger.trace("Number of products after selection: " + selectedItems.size());
		
		// Set the query's list of satisfying products to the list of selected items (products), unless this was only a dry run
		if (!checkOnly) {
			productQuery.getSatisfyingProducts().clear();
			for (Object selectedItem: selectedItems) {
				if (selectedItem instanceof Product) {
					Product product = (Product) selectedItem;
					// we do not need to save the product in this transaction
					// only the satisfied product queries are updated but this relation is also saved by the product query
					productQuery.getSatisfyingProducts().add(product);
				}
			}
			productQuery.setIsSatisfied(true);
			if (logger.isTraceEnabled()) logger.trace("Number of products satisfying product query: " + productQuery.getSatisfyingProducts().size());
		}		
		if (logger.isTraceEnabled()) logger.trace("<<< executeQuery()");
		return true;
	}
	
}
