/**
 * ProductControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.ProductUtil;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.Parameter.ParameterType;
import de.dlr.proseo.model.ProductFile.StorageType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingFacility;

/**
 * Spring MVC controller for the prosEO Ingestor; implements the services required to ingest
 * products from pickup points into the prosEO database, and to query the database about such products
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProductControllerImpl implements ProductController {
	
	/* Message ID constants */
	private static final int MSG_ID_PRODUCT_NOT_FOUND = 2001;
	private static final int MSG_ID_ENCLOSING_PRODUCT_NOT_FOUND = 2002;
	private static final int MSG_ID_COMPONENT_PRODUCT_NOT_FOUND = 2003;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 2004;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_PRODUCT_NOT_FOUND = "No product found for ID %d (%d)";
	private static final String MSG_ENCLOSING_PRODUCT_NOT_FOUND = "Enclosing product with ID %d not found (%d)";
	private static final String MSG_COMPONENT_PRODUCT_NOT_FOUND = "Component product with ID %d not found (%d)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "Product deletion unsuccessful for ID %d (%d)";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ingestor ";
	private static final String MSG_INGESTOR_PRODUCT_NOT_FOUND_BY_SENSING_START = MSG_PREFIX + "IngestorProduct with sensing start time %s not found (%d)";
	private static final String MSG_INGESTOR_PRODUCT_NOT_FOUND_BY_ID = MSG_PREFIX + "IngestorProduct with id %s not found (%d)";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductControllerImpl.class);
	
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
			String message = String.format(MSG_PREFIX + MSG_PRODUCT_NOT_FOUND, id, MSG_ID_PRODUCT_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		
		// Delete the product
		RepositoryService.getProductRepository().deleteById(id);

		// Test whether the deletion was successful
		modelProduct = RepositoryService.getProductRepository().findById(id);
		if (!modelProduct.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_DELETION_UNSUCCESSFUL, id, MSG_ID_DELETION_UNSUCCESSFUL);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		
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
	public ResponseEntity<List<de.dlr.proseo.ingestor.rest.model.Product>> getProducts(String mission, String[] productClass,
			Date startTimeFrom, Date startTimeTo) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProducts({}, {}, {}, {})", mission, productClass, startTimeFrom, startTimeTo);
		
		List<de.dlr.proseo.ingestor.rest.model.Product> result = new ArrayList<>();
		
		// Simple case: no search criteria set
		if (null == mission && (null == productClass || 0 == productClass.length) && null == startTimeFrom && null == startTimeTo) {
			for (Product product: RepositoryService.getProductRepository().findAll()) {
				if (logger.isDebugEnabled()) logger.debug("Found product with ID {}", product.getId());
				de.dlr.proseo.ingestor.rest.model.Product resultProduct = ProductUtil.toRestProduct(product);
				if (logger.isDebugEnabled()) logger.debug("Created result product with ID {}", resultProduct.getId());
				result.add(resultProduct);
			}
			return new ResponseEntity<>(result, HttpStatus.OK);
		}
		
		// Find using search parameters
		
		
		
		// TODO Auto-generated method stub
		String message = String.format(MSG_PREFIX + "GET with search parameters not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

	/**
	 * Create a product from the given Json object (does NOT create associated product files!)
	 * 
	 * @param product the Json object to create the product from
	 * @return a response containing a Json object corresponding to the product after persistence (with ID and version for all 
	 * 		   contained objects) and HTTP status "CREATED"
	 */
	@Override
	public ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> createProduct(
			de.dlr.proseo.ingestor.rest.model.@Valid Product product) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProduct({})", product.getProductClass());
		
		Product modelProduct = ProductUtil.toModelProduct(product);
		
		ProductClass modelProductClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(
				product.getMissionCode(), product.getProductClass());
		modelProduct.setProductClass(modelProductClass);
		
		for (Long componentProductId: product.getComponentProductIds()) {
			Optional<Product> componentProduct = RepositoryService.getProductRepository().findById(componentProductId);
			if (componentProduct.isEmpty()) {
				String message = String.format(MSG_PREFIX + MSG_COMPONENT_PRODUCT_NOT_FOUND,
						componentProductId, MSG_ID_COMPONENT_PRODUCT_NOT_FOUND);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			} else {
				modelProduct.getComponentProducts().add(componentProduct.get());
			}
		}
		
		Optional<Product> enclosingProduct = RepositoryService.getProductRepository().findById(product.getEnclosingProductId());
		if (enclosingProduct.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_ENCLOSING_PRODUCT_NOT_FOUND,
					product.getEnclosingProductId(), MSG_ID_ENCLOSING_PRODUCT_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		} else {
			modelProduct.setEnclosingProduct(enclosingProduct.get());
		}
		
		modelProduct = RepositoryService.getProductRepository().save(modelProduct);
		
		return new ResponseEntity<>(ProductUtil.toRestProduct(modelProduct), HttpStatus.CREATED);
	}

	/**
	 * Find the product with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a Json object corresponding to the found product and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no product with the given ID exists
	 */
	@Override
	public ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> getProductById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductById({})", id);
		
		Optional<Product> modelProduct = RepositoryService.getProductRepository().findById(id);
		
		if (modelProduct.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_PRODUCT_NOT_FOUND, id, MSG_ID_PRODUCT_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		
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
	public ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> modifyProduct(Long id,
			de.dlr.proseo.ingestor.rest.model.Product product) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProduct({})", id);
		
		Optional<Product> optModelProduct = RepositoryService.getProductRepository().findById(id);
		
		if (optModelProduct.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_PRODUCT_NOT_FOUND, id, MSG_ID_PRODUCT_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
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
		if (modelProduct.getEnclosingProduct().getId() != product.getEnclosingProductId().longValue()) {
			Optional<Product> enclosingProduct = RepositoryService.getProductRepository().findById(product.getEnclosingProductId());
			if (enclosingProduct.isEmpty()) {
				String message = String.format(MSG_PREFIX + MSG_ENCLOSING_PRODUCT_NOT_FOUND,
						product.getEnclosingProductId(), MSG_ID_ENCLOSING_PRODUCT_NOT_FOUND);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			} else {
				productChanged = true;
				modelProduct.setEnclosingProduct(enclosingProduct.get());
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
			Optional<Product> componentProduct = RepositoryService.getProductRepository().findById(componentProductId);
			if (componentProduct.isEmpty()) {
				String message = String.format(MSG_PREFIX + MSG_COMPONENT_PRODUCT_NOT_FOUND, componentProductId,
						MSG_ID_COMPONENT_PRODUCT_NOT_FOUND);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			} else {
				productChanged = true;
				modelProduct.getComponentProducts().add(componentProduct.get());
			} 
		}
		// Check for removed component products
		for (Product modelComponentProduct: modelProduct.getComponentProducts()) {
			if (product.getComponentProductIds().contains(modelComponentProduct.getId())) {
				continue;
			}
			productChanged = true;
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
		if (productChanged)	{
			modelProduct.incrementVersion();
			modelProduct = RepositoryService.getProductRepository().save(modelProduct);
		}
		
		return new ResponseEntity<>(ProductUtil.toRestProduct(modelProduct), HttpStatus.OK);
	}

}
