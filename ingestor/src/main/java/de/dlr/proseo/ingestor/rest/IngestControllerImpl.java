/**
 * IngestControllerImpl.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ConcurrentModificationException;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import javax.validation.Valid;
import javax.ws.rs.ProcessingException;

import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.ingestor.IngestorSecurityConfig;
import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.ingestor.rest.model.RestProductFile;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.IngestorMessage;
import de.dlr.proseo.model.ProcessingFacility;

/**
 * Spring MVC controller for the prosEO Ingestor; implements the services
 * required to ingest products from pickup points into the prosEO database, and
 * to query the database about such products
 *
 * @author Dr. Thomas Bassler
 */
@Component
public class IngestControllerImpl implements IngestController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(IngestControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.INGESTOR);

	/** Product ingestor */
	@Autowired
	private ProductIngestor productIngestor;

	/** Security configuration for Ingestor */
	@Autowired
	private IngestorSecurityConfig securityConfig;

	/**
	 * Ingest all given products into the storage manager of the given processing
	 * facility. If the ID of a product to ingest is null or 0 (zero), then the
	 * product will be created, otherwise a matching product will be looked up and
	 * updated.
	 *
	 * The Production Planner will be notified of all ingested products. However,
	 * notification is optional, and if it fails, the Ingestor still returns with
	 * HTTP status CREATED. We rely on a cyclical check by the Production Planner to
	 * pick up all newly ingested products, should it not have been notified.
	 *
	 * @param processingFacility the processing facility to ingest products to
	 * @param copyFiles          indicates, whether to copy the files to a different
	 *                           storage area (default "true"; only applicable if
	 *                           source and target storage type are the same)
	 * @param ingestorProducts   a list of product descriptions with product file
	 *                           locations
	 * @param httpHeaders        the HTTP request headers (injected)
	 * @return HTTP status "CREATED" and a Json list of the products updated and/or
	 *         created including their product files or HTTP status "BAD_REQUEST",
	 *         if an invalid processing facility was given, or HTTP status
	 *         "FORBIDDEN" and an error message, if a cross-mission data access was
	 *         attempted, or HTTP status "INTERNAL_SERVER_ERROR", if the
	 *         communication to the Storage Manager or to the Production Planner
	 *         failed
	 */
	@Override
	public ResponseEntity<List<RestProduct>> ingestProducts(String processingFacility, Boolean copyFiles,
			@Valid List<IngestorProduct> ingestorProducts, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> ingestProducts({}, IngestorProduct[{}])", processingFacility, ingestorProducts.size());

		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e)),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		final ProcessingFacility facility = productIngestor.getFacilityByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(IngestorMessage.INVALID_FACILITY, processingFacility)),
					HttpStatus.BAD_REQUEST);
		}

		// Default is to copy files, if query parameter is not set
		if (null == copyFiles) {
			copyFiles = true;
		}

		// Get username and password from HTTP Authentication header for authentication
		// with Production Planner
		String[] userPassword = securityConfig.parseAuthenticationHeader(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION));

		// Perform product ingestion
		try {
			return new ResponseEntity<>(productIngestor.ingestProducts(facility, copyFiles, ingestorProducts, userPassword[0],
					userPassword[1]), HttpStatus.CREATED);
		} catch (ProcessingException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Get the product file metadata for a product at a given processing facility
	 *
	 * @param productId          the ID of the product to retrieve
	 * @param processingFacility the processing facility to retrieve the product
	 *                           file metadata for
	 * @param httpHeaders        the HTTP request headers (injected)
	 * @return HTTP status "OK" and the Json representation of the product file
	 *         metadata, or HTTP status "FORBIDDEN" and an error message, if a
	 *         cross-mission data access was attempted, or HTTP status "NOT_FOUND",
	 *         if no product file for the given product ID exists at the given
	 *         processing facility
	 */
	@Override
	public ResponseEntity<RestProductFile> getProductFile(Long productId, String processingFacility, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getProductFile({}, {})", productId, processingFacility);

		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e)),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = productIngestor.getFacilityByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(IngestorMessage.INVALID_FACILITY, processingFacility)),
					HttpStatus.BAD_REQUEST);
		}

		try {
			return new ResponseEntity<>(productIngestor.getProductFile(productId, facility), HttpStatus.CREATED);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Create the metadata of a new product file for a product at a given processing
	 * facility (it is assumed that the files themselves are already pushed to the
	 * Storage Manager)
	 *
	 * The Production Planner will be notified of the ingested product. However,
	 * notification is optional, and if it fails, the Ingestor still returns with
	 * HTTP status CREATED. We rely on a cyclical check by the Production Planner to
	 * pick up all newly ingested products, should it not have been notified.
	 *
	 * @param productId          the ID of the product to retrieve
	 * @param processingFacility the name of the processing facility, in which the
	 *                           files have been stored
	 * @param productFile        the REST product file to store
	 * @param httpHeaders        the HTTP request headers (injected)
	 * @return HTTP status "CREATED" and the updated REST product file (with ID and
	 *         version) or HTTP status "FORBIDDEN" and an error message, if a
	 *         cross-mission data access was attempted, or HTTP status
	 *         "BAD_REQUEST", if the processing facility or the product cannot be
	 *         found, or if the data for the product file is invalid (also, if a
	 *         product file for the given processing facility already exists)
	 */
	@Override
	public ResponseEntity<RestProductFile> ingestProductFile(Long productId, String processingFacility,
			@javax.validation.Valid RestProductFile productFile, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> ingestProductFile({}, {}, {})", productId, processingFacility, productFile.getProductFileName());

		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e)),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = productIngestor.getFacilityByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(IngestorMessage.INVALID_FACILITY, processingFacility)),
					HttpStatus.BAD_REQUEST);
		}

		// Get username and password from HTTP Authentication header for authentication
		// with Production Planner
		String[] userPassword = securityConfig.parseAuthenticationHeader(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION));

		try {
			return new ResponseEntity<>(
					productIngestor.ingestProductFile(productId, facility, productFile, userPassword[0], userPassword[1]),
					HttpStatus.CREATED);
		} catch (ProcessingException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (LockAcquisitionException e) {
			if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Delete a product file for a product from a given processing facility
	 * (metadata and actual data file(s))
	 *
	 * @param productId          the ID of the product to retrieve
	 * @param processingFacility the name of the processing facility, from which the
	 *                           files shall be deleted
	 * @param eraseFiles         erase the data file(s) from the storage area
	 *                           (default "true")
	 * @param httpHeaders        the HTTP request headers (injected)
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was
	 *         successful, or HTTP status "NOT_FOUND", if the processing facility,
	 *         the product or the product file did not exist, or HTTP status
	 *         "BAD_REQUEST", if the product currently satisfies a product query for
	 *         the given processing facility, or HTTP status "NOT_MODIFIED", if the
	 *         deletion was unsuccessful, or HTTP status "FORBIDDEN" and an error
	 *         message, if a cross-mission data access was attempted, or HTTP status
	 *         "INTERNAL_SERVER_ERROR", if the communication to the Storage Manager
	 *         or to the Production Planner failed
	 */
	@Override
	public ResponseEntity<?> deleteProductFile(Long productId, String processingFacility, Boolean eraseFiles,
			HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteProductFile({}, {})", productId, processingFacility);

		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e)),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = productIngestor.getFacilityByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(IngestorMessage.INVALID_FACILITY, processingFacility)),
					HttpStatus.NOT_FOUND);
		}

		try {
			productIngestor.deleteProductFile(productId, facility, eraseFiles);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ProcessingException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * Update the product file metadata for a product at a given processing facility
	 * (it is assumed that any new or changed files themselves are already pushed to
	 * the Storage Manager)
	 *
	 * @param productId          the ID of the product to retrieve
	 * @param processingFacility the name of the processing facility, in which the
	 *                           files have been stored
	 * @param httpHeaders        the HTTP request headers (injected)
	 * @param productFile        the REST product file to store
	 * @return HTTP status OK and the updated REST product file (with ID and
	 *         version) or HTTP status "NOT_FOUND" and an error message, if no
	 *         product with the given ID or no processing facility with the given
	 *         name exists, or HTTP status "BAD_REQUEST" and an error message, if
	 *         any of the input data was invalid, or HTTP status "FORBIDDEN" and an
	 *         error message, if a cross-mission data access was attempted, or HTTP
	 *         status "CONFLICT"and an error message, if the product file has been
	 *         modified since retrieval by the client
	 * 
	 */
	@Override
	public ResponseEntity<RestProductFile> modifyProductFile(Long productId, String processingFacility, RestProductFile productFile,
			HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyProductFile({}, {}, {})", productId, processingFacility, productFile.getProductFileName());

		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e)),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = productIngestor.getFacilityByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(http.errorHeaders(logger.log(IngestorMessage.INVALID_FACILITY, processingFacility)),
					HttpStatus.BAD_REQUEST);
		}

		try {
			return new ResponseEntity<>(productIngestor.modifyProductFile(productId, facility, productFile), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}
