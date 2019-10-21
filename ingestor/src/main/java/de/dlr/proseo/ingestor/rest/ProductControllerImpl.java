/**
 * ProductControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.ingestor.rest.model.ProductUtil;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.Parameter;

/**
 * Spring MVC controller for the prosEO Ingestor; implements the services required to ingest
 * products from pickup points into the prosEO database, and to query the database about such products
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class ProductControllerImpl implements ProductController {
	
	/* Message ID constants */
	private static final int MSG_ID_PRODUCT_MISSING = 2000;
	private static final int MSG_ID_PRODUCT_NOT_FOUND = 2001;
	private static final int MSG_ID_ENCLOSING_PRODUCT_NOT_FOUND = 2002;
	private static final int MSG_ID_COMPONENT_PRODUCT_NOT_FOUND = 2003;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 2004;
	private static final int MSG_ID_PRODUCT_DELETED = 2005;
	private static final int MSG_ID_PRODUCT_LIST_RETRIEVED = 2006;
	private static final int MSG_ID_PRODUCT_CREATED = 2007;
	private static final int MSG_ID_PRODUCT_RETRIEVED = 2008;
	private static final int MSG_ID_PRODUCT_MODIFIED = 2009;
	private static final int MSG_ID_PRODUCT_NOT_MODIFIED = 2010;
	private static final int MSG_ID_PRODUCT_LIST_EMPTY = 2011;
	private static final int MSG_ID_MISSION_OR_PRODUCT_CLASS_INVALID = 2012;
	private static final int MSG_ID_PRODUCT_ID_MISSING = 2013;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_PRODUCT_MISSING = "(E%d) Product not set";
	private static final String MSG_PRODUCT_ID_MISSING = "(E%d) Product ID not set";
	private static final String MSG_PRODUCT_NOT_FOUND = "(E%d) No product found for ID %d";
	private static final String MSG_PRODUCT_LIST_EMPTY = "(E%d) No products found for search criteria";
	private static final String MSG_MISSION_OR_PRODUCT_CLASS_INVALID = "(E%d) Mission code %s and/or product type %s invalid";
	private static final String MSG_ENCLOSING_PRODUCT_NOT_FOUND = "(E%d) Enclosing product with ID %d not found";
	private static final String MSG_COMPONENT_PRODUCT_NOT_FOUND = "(E%d) Component product with ID %d not found";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Product deletion unsuccessful for ID %d";
	private static final String MSG_PRODUCT_DELETED = "(I%d) Product with id %d deleted";
	private static final String MSG_PRODUCT_LIST_RETRIEVED = "(I%d) Product list of size %d retrieved for mission '%s', product classes '%s', start time '%s', stop time '%s'";
	private static final String MSG_PRODUCT_CREATED = "(I%d) Product of type %s created for mission %s";
	private static final String MSG_PRODUCT_RETRIEVED = "(I%d) Product with id %d retrieved";
	private static final String MSG_PRODUCT_MODIFIED = "(I%d) Product with id %d modified";
	private static final String MSG_PRODUCT_NOT_MODIFIED = "(I%d) Product with id %d not modified (no changes)";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-ingestor ";
	
	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductControllerImpl.class);
	
	/**
	 * Log an informational message with the prosEO message prefix
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 */
	private void logInfo(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		logger.info(String.format(messageFormat, messageParamList.toArray()));
	}
	
	/**
	 * Log an error and return the corresponding HTTP message header
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return an HttpHeaders object with a formatted error message
	 */
	private HttpHeaders errorHeaders(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.error(message);
		
		// Create an HTTP "Warning" header
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + message);
		return responseHeaders;
	}
	
	/**
	 * Delete a product by ID
	 * 
	 * @param the ID of the product to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, "NOT_FOUND", if the product did not
	 *         exist, or "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteProductById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductById({})", id);
		
		// Test whether the product id is valid
		Optional<Product> modelProduct = RepositoryService.getProductRepository().findById(id);
		if (modelProduct.isEmpty()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PRODUCT_NOT_FOUND, MSG_ID_PRODUCT_NOT_FOUND), HttpStatus.NOT_FOUND);
		}
		
		// Delete the product
		RepositoryService.getProductRepository().deleteById(id);

		// Test whether the deletion was successful
		modelProduct = RepositoryService.getProductRepository().findById(id);
		if (!modelProduct.isEmpty()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, id), HttpStatus.NOT_MODIFIED);
		}
		
		logInfo(MSG_PRODUCT_DELETED, MSG_ID_PRODUCT_DELETED, id);
		
		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
	}

	/**
	 * List of all products filtered by mission, product class, start time range
	 * 
	 * @param mission the mission code
	 * @param productClass an array of product types
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo latest sensing start time
	 * @return a response entity with either a list of products and HTTP status OK or an error message and an HTTP status indicating failure
	 */
	@Override
	public ResponseEntity<List<RestProduct>> getProducts(String mission, String[] productClass,
			Date startTimeFrom, Date startTimeTo) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProducts({}, {}, {}, {})", mission, productClass, startTimeFrom, startTimeTo);
		
		List<RestProduct> result = new ArrayList<>();
		
		if (null == mission && (null == productClass || 0 == productClass.length) && null == startTimeFrom && null == startTimeTo) {
			// Simple case: no search criteria set
			for (Product product: RepositoryService.getProductRepository().findAll()) {
				if (logger.isDebugEnabled()) logger.debug("Found product with ID {}", product.getId());
				RestProduct resultProduct = ProductUtil.toRestProduct(product);
				if (logger.isDebugEnabled()) logger.debug("Created result product with ID {}", resultProduct.getId());

				result.add(resultProduct);
			}
		} else {
			// Find using search parameters
			String jpqlQuery = "select p from Product where 1 = 1";
			if (null != mission) {
				jpqlQuery += " and p.productClass.mission.code = :missionCode";
			}
			if (null != productClass && 0 < productClass.length) {
				jpqlQuery += " and p.productClass.productType in (";
				for (int i = 0; i < productClass.length; ++i) {
					if (0 < i) jpqlQuery += ", ";
					jpqlQuery += ":productClass" + i;
				}
				jpqlQuery += ")";
			}
			if (null != startTimeFrom) {
				jpqlQuery += " and p.sensingStartTime >= :startTimeFrom";
			}
			if (null != startTimeTo) {
				jpqlQuery += " and p.sensingStartTime <= :startTimeTo";
			}
			Query query = em.createQuery(jpqlQuery);
			if (null != mission) {
				query.setParameter("missionCode", mission);
			}
			if (null != productClass && 0 < productClass.length) {
				for (int i = 0; i < productClass.length; ++i) {
					query.setParameter("productClass" + i, productClass[i]);
				}
			}
			if (null != startTimeFrom) {
				query.setParameter("startTimeFrom", startTimeFrom);
			}
			if (null != startTimeTo) {
				query.setParameter("startTimeTo", startTimeTo);
			}
			for (Object resultObject: query.getResultList()) {
				if (resultObject instanceof Product) {
					result.add(ProductUtil.toRestProduct((Product) resultObject));
				}
			}
		}
		
		if (result.isEmpty()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PRODUCT_LIST_EMPTY, MSG_ID_PRODUCT_LIST_EMPTY), 
					HttpStatus.NOT_FOUND);
		}
		
		logInfo(MSG_PRODUCT_LIST_RETRIEVED, MSG_ID_PRODUCT_LIST_RETRIEVED, result.size(), "null", "null", "null", "null");
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	/**
	 * Create a product from the given Json object (does NOT create associated product files!)
	 * 
	 * @param product the Json object to create the product from
	 * @return a response containing a Json object corresponding to the product after persistence (with ID and version for all 
	 * 		   contained objects) and HTTP status "CREATED"
	 */
	@Override
	public ResponseEntity<RestProduct> createProduct(
			de.dlr.proseo.ingestor.rest.model.@Valid RestProduct product) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProduct({})", (null == product ? "MISSING" : product.getProductClass()));
		
		if (null == product) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PRODUCT_MISSING, MSG_ID_PRODUCT_MISSING), HttpStatus.BAD_REQUEST);
		}

		Product modelProduct = ProductUtil.toModelProduct(product);
		
		ProductClass modelProductClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(
				product.getMissionCode(), product.getProductClass());
		if (null == modelProductClass) {
			return new ResponseEntity<>(
					errorHeaders(MSG_MISSION_OR_PRODUCT_CLASS_INVALID, MSG_ID_MISSION_OR_PRODUCT_CLASS_INVALID, 
							product.getMissionCode(), product.getProductClass()),
					HttpStatus.BAD_REQUEST);
		}
		modelProduct.setProductClass(modelProductClass);
		
		for (Long componentProductId: product.getComponentProductIds()) {
			Optional<Product> componentProduct = RepositoryService.getProductRepository().findById(componentProductId);
			if (componentProduct.isEmpty()) {
				return new ResponseEntity<>(
						errorHeaders(MSG_COMPONENT_PRODUCT_NOT_FOUND, MSG_ID_COMPONENT_PRODUCT_NOT_FOUND, componentProductId), 
						HttpStatus.NOT_FOUND);
			} else {
				modelProduct.getComponentProducts().add(componentProduct.get());
			}
		}
		
		if (null != product.getEnclosingProductId()) {
			Optional<Product> enclosingProduct = RepositoryService.getProductRepository().findById(product.getEnclosingProductId());
			if (enclosingProduct.isEmpty()) {
				return new ResponseEntity<>(
						errorHeaders(MSG_ENCLOSING_PRODUCT_NOT_FOUND, MSG_ID_ENCLOSING_PRODUCT_NOT_FOUND, product.getEnclosingProductId()), 
						HttpStatus.NOT_FOUND);
			} else {
				modelProduct.setEnclosingProduct(enclosingProduct.get());
			} 
		}
		modelProduct = RepositoryService.getProductRepository().save(modelProduct);
		
		logInfo(MSG_PRODUCT_CREATED, MSG_ID_PRODUCT_CREATED, product.getProductClass(), product.getMissionCode());
		
		return new ResponseEntity<>(ProductUtil.toRestProduct(modelProduct), HttpStatus.CREATED);
	}

	/**
	 * Find the product with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a Json object corresponding to the product found and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no product with the given ID exists
	 */
	@Override
	public ResponseEntity<RestProduct> getProductById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductById({})", id);
		
		if (null == id) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PRODUCT_ID_MISSING, MSG_ID_PRODUCT_ID_MISSING, id), 
					HttpStatus.BAD_REQUEST);
		}
		
		Optional<Product> modelProduct = RepositoryService.getProductRepository().findById(id);
		
		if (modelProduct.isEmpty()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PRODUCT_NOT_FOUND, MSG_ID_PRODUCT_NOT_FOUND, id), 
					HttpStatus.NOT_FOUND);
		}
		
		logInfo(MSG_PRODUCT_RETRIEVED, MSG_ID_PRODUCT_RETRIEVED, id);
		
		return new ResponseEntity<>(ProductUtil.toRestProduct(modelProduct.get()), HttpStatus.OK);
	}

	/**
	 * Update the product with the given ID with the attribute values of the given Json object. This method will NOT modify
	 * associated product files.
	 * 
	 * @param id the ID of the product to update
	 * @param product a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the product after modification (with ID and version for all 
	 * 		   contained objects) and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no product with the given ID exists
	 */
	@Override
	public ResponseEntity<RestProduct> modifyProduct(Long id,
			RestProduct product) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProduct({})", id);
		
		Optional<Product> optModelProduct = RepositoryService.getProductRepository().findById(id);
		
		if (optModelProduct.isEmpty()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PRODUCT_NOT_FOUND, MSG_ID_PRODUCT_NOT_FOUND, id), 
					HttpStatus.NOT_FOUND);
		}
		Product modelProduct = optModelProduct.get();
		
		// Update modified attributes
		boolean productChanged = false;
		Product changedProduct = ProductUtil.toModelProduct(product);
		
		if (!modelProduct.getProductClass().getMission().getCode().equals(product.getMissionCode())
			|| !modelProduct.getProductClass().getProductType().equals(product.getProductClass())) {
			productChanged = true;
			ProductClass modelProductClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(
					product.getMissionCode(), product.getProductClass());
			modelProduct.setProductClass(modelProductClass);
		}
		if (!modelProduct.getMode().equals(changedProduct.getMode())) {
			productChanged = true;
			if (logger.isDebugEnabled()) logger.debug("Changing mode from {} to {}", modelProduct.getMode(), changedProduct.getMode());
			modelProduct.setMode(changedProduct.getMode());
		}
		if (!modelProduct.getSensingStartTime().equals(changedProduct.getSensingStartTime())) {
			productChanged = true;
			modelProduct.setSensingStartTime(changedProduct.getSensingStartTime());
		}
		if (!modelProduct.getSensingStopTime().equals(changedProduct.getSensingStopTime())) {
			productChanged = true;
			modelProduct.setSensingStopTime(changedProduct.getSensingStopTime());
		}
		
		// Update relationship to enclosing product
		if (null == modelProduct.getEnclosingProduct() && null == product.getEnclosingProductId()) {
			// OK - no enclosing product on both sides
		} else if (null == product.getEnclosingProductId()) {
			// Enclosing product was set, but is no more
			productChanged = true;
			Product modelEnclosingProduct = modelProduct.getEnclosingProduct();
			modelEnclosingProduct.getComponentProducts().remove(modelProduct);
			RepositoryService.getProductRepository().save(modelEnclosingProduct);
			modelProduct.setEnclosingProduct(null);
		} else {
			// Enclosing product shall be set, check whether it has been changed
			if (null == modelProduct.getEnclosingProduct() /* new */
					|| modelProduct.getEnclosingProduct().getId() != product.getEnclosingProductId().longValue() /* changed */) {
				Optional<Product> enclosingProduct = RepositoryService.getProductRepository().findById(product.getEnclosingProductId());
				if (enclosingProduct.isEmpty()) {
					return new ResponseEntity<>(
							errorHeaders(MSG_ENCLOSING_PRODUCT_NOT_FOUND, MSG_ID_ENCLOSING_PRODUCT_NOT_FOUND, product.getEnclosingProductId()), 
							HttpStatus.NOT_FOUND);
				} else {
					productChanged = true;
					if (null != modelProduct.getEnclosingProduct()) {
						// Enclosing product has changed, remove this product from old enclosing product
						Product modelEnclosingProduct = modelProduct.getEnclosingProduct();
						modelEnclosingProduct.getComponentProducts().remove(modelProduct);
						RepositoryService.getProductRepository().save(modelEnclosingProduct);
					}
					// Add this product to new enclosing product
					enclosingProduct.get().getComponentProducts().add(modelProduct);
					RepositoryService.getProductRepository().save(enclosingProduct.get());
					modelProduct.setEnclosingProduct(enclosingProduct.get());
				}
			}
		}
		
		// Check for added component products
		ADDED_PRODUCTS:
		for (Long componentProductId: product.getComponentProductIds()) {
			for (Product modelComponentProduct: modelProduct.getComponentProducts()) {
				if (modelComponentProduct.getId() == componentProductId.longValue()) {
					continue ADDED_PRODUCTS;
				}
			}
			// Fall through, so there is a new component product
			Optional<Product> componentProduct = RepositoryService.getProductRepository().findById(componentProductId);
			if (componentProduct.isEmpty()) {
				return new ResponseEntity<>(
						errorHeaders(MSG_COMPONENT_PRODUCT_NOT_FOUND, MSG_ID_COMPONENT_PRODUCT_NOT_FOUND, componentProductId), 
						HttpStatus.NOT_FOUND);
			} else {
				productChanged = true;
				// Set enclosing product for new component product
				componentProduct.get().setEnclosingProduct(modelProduct);
				RepositoryService.getProductRepository().save(componentProduct.get());
				modelProduct.getComponentProducts().add(componentProduct.get());
			} 
		}
		// Check for removed component products
		for (Product modelComponentProduct: modelProduct.getComponentProducts()) {
			if (product.getComponentProductIds().contains(modelComponentProduct.getId())) {
				continue;
			}
			productChanged = true;
			// Remove enclosing product from component product
			modelComponentProduct.setEnclosingProduct(null);
			RepositoryService.getProductRepository().save(modelComponentProduct);
			modelProduct.getComponentProducts().remove(modelComponentProduct);
		}
		
		// Check for added or changed parameters
		for (String changedParamKey: changedProduct.getParameters().keySet()) {
			Parameter changedParam = changedProduct.getParameters().get(changedParamKey);
			if (modelProduct.getParameters().containsKey(changedParamKey)) {
				Parameter modelParam = modelProduct.getParameters().get(changedParamKey);
				if (modelParam.equals(changedParam)) {
					continue;
				}
			}
			productChanged = true;
			modelProduct.getParameters().put(changedParamKey, changedParam);
		}
		// Check for removed parameters
		for (String modelParamKey: modelProduct.getParameters().keySet()) {
			if (changedProduct.getParameters().containsKey(modelParamKey)) {
				// If found, must be equal after checking for added/changed parameters
				continue;
			}
			productChanged = true;
			modelProduct.getParameters().remove(modelParamKey);
		}
		
		// Save product only if anything was actually changed
		HttpStatus httpStatus = null;
		if (productChanged)	{
			modelProduct.incrementVersion();
			modelProduct = RepositoryService.getProductRepository().save(modelProduct);
			httpStatus = HttpStatus.OK;
			logInfo(MSG_PRODUCT_MODIFIED, MSG_ID_PRODUCT_MODIFIED, id);
		} else {
			httpStatus = HttpStatus.NOT_MODIFIED;
			logInfo(MSG_PRODUCT_NOT_MODIFIED, MSG_ID_PRODUCT_NOT_MODIFIED, id);
		}
		
		return new ResponseEntity<>(ProductUtil.toRestProduct(modelProduct), httpStatus);
	}

}
