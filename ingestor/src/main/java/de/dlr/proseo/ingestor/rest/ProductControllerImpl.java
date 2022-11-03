/**
 * ProductControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.util.ConcurrentModificationException;
import java.util.List;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.ingestor.IngestorSecurityConfig;
import de.dlr.proseo.ingestor.PlannerSemaphoreClient;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * Spring MVC controller for the prosEO Ingestor; implements the services required to ingest
 * products from pickup points into the prosEO database, and to query the database about such products
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class ProductControllerImpl implements ProductController {
		
	/** Client to request/release semaphores from Production Planner */
	@Autowired
	private PlannerSemaphoreClient semaphoreClient;

	/** Security configuration for Ingestor */
	@Autowired
	private IngestorSecurityConfig securityConfig;
			
	/** The product manager */
	@Autowired
	private ProductManager productManager;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(IngestControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.INGESTOR);
		
	/**
	 * Delete a product by ID
	 * 
	 * @param id the ID of the product to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the product did not exist, or
	 *         HTTP status "BAD_REQUEST", if the product still has files at some Processing Facility, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteProductById(Long id, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductById({})", id);
		
		try {
			productManager.deleteProductById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalStateException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * List of all products filtered by mission, product class, start time range; the output will be ordered by the columns
     * given in orderBy, and the resulting product list will only contain the records in the given range
	 * 
	 * @param mission the mission code
	 * @param productClass an array of product types
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo latest sensing start time
	 * @param recordFrom first record of filtered and ordered result to return
	 * @param recordTo last record of filtered and ordered result to return
	 * @param orderBy an array of strings containing a column name and an optional sort direction (ASC/DESC), separated by white space
	 * @return HTTP status "OK" and a list of products or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "NOT_FOUND" and an error message, if no products matching the search criteria were found
	 */
	@Override
	public ResponseEntity<List<RestProduct>> getProducts(String mission, String[] productClass, String mode, String fileClass, String quality, 
			String startTimeFrom, String startTimeTo, String genTimeFrom, String genTimeTo, 
			Long recordFrom, Long recordTo, Long jobStepId, String[] orderBy, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProducts({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})", mission, productClass,
				mode, fileClass, quality, startTimeFrom, startTimeTo, genTimeFrom, genTimeTo, recordFrom, recordTo, orderBy);
		
		try {
			return new ResponseEntity<>(
					productManager.getProducts(mission, productClass, mode, fileClass, quality, startTimeFrom, startTimeTo, genTimeFrom, genTimeTo, 
							recordFrom, recordTo, jobStepId, orderBy), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

    /**
     * Number of products available, possibly filtered by mission, product class and time range
     * 
	 * @param mission the mission code
	 * @param productClass an array of product types
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo latest sensing start time
	 * @return HTTP status "OK" and the number of products found (may be zero) or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted
     */
	@Override
    public ResponseEntity<?> countProducts(String mission, String[] productClass, String mode, String fileClass, String quality, 
    		String startTimeFrom, String startTimeTo, String genTimeFrom, String genTimeTo, Long jobStepId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> countProducts({}, {}, {}, {}, {}, {}, {}, {}, {})", mission, productClass,
				mode, fileClass, quality, startTimeFrom, startTimeTo, genTimeFrom, genTimeTo);
		
		try {
			return new ResponseEntity<>(
					productManager.countProducts(mission, productClass, mode, fileClass, quality, startTimeFrom, startTimeTo, genTimeFrom, genTimeTo, 
							jobStepId), HttpStatus.OK);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Create a product from the given Json object (does NOT create associated product files!)
	 * 
	 * @param product the Json object to create the product from
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the product after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestProduct> createProduct(de.dlr.proseo.ingestor.rest.model.@Valid RestProduct product, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> createProduct({})", (null == product ? "MISSING" : product.getProductClass()));
		
		try {
			return new ResponseEntity<>(productManager.createProduct(product), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Find the product with the given ID
	 * 
	 * @param id the ID to look for
	 * @return HTTP status "OK" and a Json object corresponding to the product found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no product ID was given, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product with the given ID exists
	 */
	@Override
	public ResponseEntity<RestProduct> getProductById(Long id, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductById({})", id);
		
		try {
			return new ResponseEntity<>(productManager.getProductById(id), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
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
	 *         HTTP status "NOT_MODIFIED" and the unchanged product, if no attributes were actually changed, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product with the given ID exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "CONFLICT"and an error message, if the product has been modified since retrieval by the client
	 */
	@Override
	public ResponseEntity<RestProduct> modifyProduct(Long id, RestProduct product, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProduct({})", id);
		
		// Get username and password from HTTP Authentication header for authentication with Production Planner
		String[] userPassword = securityConfig.parseAuthenticationHeader(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION));
		
		try {
			semaphoreClient.acquireSemaphore(userPassword[0], userPassword[1]);
			RestProduct changedProduct = productManager.modifyProduct(id, product);
			HttpStatus httpStatus = (product.getVersion() == changedProduct.getVersion() ? HttpStatus.NOT_MODIFIED : HttpStatus.OK);
			return new ResponseEntity<>(changedProduct, httpStatus);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} finally {
			semaphoreClient.releaseSemaphore(userPassword[0], userPassword[1]);
		}
	}

	/**
	 * Get a product by UUID
	 * 
	 * @param uuid the universally unique product identifier
	 * @return HTTP status "OK" and a Json object corresponding to the product found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no or an invalid product UUID was given, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product with the given UUID exists
	 */
	@Override
	public ResponseEntity<RestProduct> getProductByUuid(
			@Pattern(regexp = "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$") String uuid,
			HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductByUuid({})", uuid);
		
		try {
			return new ResponseEntity<>(productManager.getProductByUuid(uuid), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Get the primary data file (or ZIP file, if available) for the product as data stream (optionally range-restricted),
	 * returns a redirection link to the Storage Manager of a random Processing Facility
	 * 
	 * @param id the ID of the product to download
	 * @param fromByte the first byte of the data stream to download (optional, default is file start, i.e. byte 0)
	 * @param toByte the last byte of the data stream to download (optional, default is file end, i.e. file size - 1)
	 * @return HTTP status "TEMPORARY_REDIRECT" and a redirect URL in the HTTP Location header, or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no or an invalid product ID was given, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product with the given ID exists or if it does not have a data file
	 */
	@Override
	public ResponseEntity<?> downloadProductById(Long id, Long fromByte, Long toByte, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> downloadProductById({})", id);
		
		try {
			String uri = productManager.downloadProductById(id, fromByte, toByte);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.add(HttpHeaders.LOCATION, uri);
			return new ResponseEntity<>(responseHeaders, HttpStatus.TEMPORARY_REDIRECT);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Get a JSON Web Token for creating a download link to a Storage Manager
	 * 
	 * @param id the ID of the product to download
	 * @param fileName the name of the file to download (default primary data file or ZIP file, if available)
	 * @return HTTP status "OK" and the signed JSON Web Token (JWS) as per RFC 7515 and RFC 7519, or
	 *         HTTP status "BAD_REQUEST" and an error message, if no or an invalid product ID was given, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product with the given ID or no file with the given name exists
	 */
	@Override
	public ResponseEntity<?> getDownloadTokenById(Long id, String fileName, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getDownloadTokenById({})", id);
		
		try {
			return new ResponseEntity<>(productManager.getDownloadTokenById(id, fileName), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}
