/**
 * ProductControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import de.dlr.proseo.ingestor.rest.model.RestProduct;

/**
 * Spring MVC controller for the prosEO Ingestor; implements the services required to ingest
 * products from pickup points into the prosEO database, and to query the database about such products
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class ProductControllerImpl implements ProductController {
	
	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-ingestor ";
	
	/** The product manager */
	@Autowired
	private ProductManager productManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductControllerImpl.class);
	
	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + message.replaceAll("\n", " "));
		return responseHeaders;
	}
	
	/**
	 * Delete a product by ID
	 * 
	 * @param the ID of the product to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the product did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteProductById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductById({})", id);
		
		try {
			productManager.deleteProductById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * List of all products filtered by mission, product class, start time range
	 * 
	 * @param mission the mission code
	 * @param productClass an array of product types
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo latest sensing start time
	 * @return HTTP status "OK" and a list of products or
	 *         HTTP status "NOT_FOUND" and an error message, if no products matching the search criteria were found
	 */
	@Override
	public ResponseEntity<List<RestProduct>> getProducts(String mission, String[] productClass,
			Date startTimeFrom, Date startTimeTo) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProducts({}, {}, {}, {})", mission, productClass, startTimeFrom, startTimeTo);
		
		try {
			return new ResponseEntity<>(
					productManager.getProducts(mission, productClass, startTimeFrom, startTimeTo), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Create a product from the given Json object (does NOT create associated product files!)
	 * 
	 * @param product the Json object to create the product from
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the product after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestProduct> createProduct(de.dlr.proseo.ingestor.rest.model.@Valid RestProduct product) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProduct({})", (null == product ? "MISSING" : product.getProductClass()));
		
		try {
			return new ResponseEntity<>(productManager.createProduct(product), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Find the product with the given ID
	 * 
	 * @param id the ID to look for
	 * @return HTTP status "OK" and a Json object corresponding to the product found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no product ID was given, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product with the given ID exists
	 */
	@Override
	public ResponseEntity<RestProduct> getProductById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductById({})", id);
		
		try {
			return new ResponseEntity<>(productManager.getProductById(id), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Update the product with the given ID with the attribute values of the given Json object. This method will NOT modify
	 * associated product files.
	 * 
	 * @param id the ID of the product to update
	 * @param product a Json object containing the modified (and unmodified) attributes
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the product after modification
	 *             (with ID and version for all contained objects) or 
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product with the given ID exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "CONFLICT"and an error message, if the product has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestProduct> modifyProduct(Long id, RestProduct product) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProduct({})", id);
		
		try {
			RestProduct changedProduct = productManager.modifyProduct(id, product);
			HttpStatus httpStatus = (product.getVersion() == changedProduct.getVersion() ? HttpStatus.NOT_MODIFIED : HttpStatus.OK);
			return new ResponseEntity<>(changedProduct, httpStatus);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		}
	}

	/**
	 * Get a product by UUID
	 * 
	 * @param uuid the universally unique product identifier
	 * @return HTTP status "OK" and a Json object corresponding to the product found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no or an invalid product UUID was given, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product with the given UUID exists
	 */
	@Override
	public ResponseEntity<RestProduct> getProductByUuid(
			@Pattern(regexp = "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$") String uuid) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductByUuid({})", uuid);
		
		try {
			return new ResponseEntity<>(productManager.getProductByUuid(uuid), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

}
