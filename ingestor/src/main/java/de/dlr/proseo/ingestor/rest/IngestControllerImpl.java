/**
 * IngestControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.validation.Valid;
import javax.ws.rs.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import de.dlr.proseo.ingestor.IngestorConfiguration;
import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.ingestor.rest.model.RestProductFile;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Spring MVC controller for the prosEO Ingestor; implements the services required to ingest
 * products from pickup points into the prosEO database, and to query the database about such products
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class IngestControllerImpl implements IngestController {


	/* Message ID constants */
	private static final int MSG_ID_INVALID_FACILITY = 2051;
	private static final int MSG_ID_PRODUCTS_INGESTED = 2058;
	// private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	private static final int MSG_ID_EXCEPTION_THROWN = 9001;
	
	/* Message string constants */
	private static final String MSG_INVALID_PROCESSING_FACILITY = "(E%d) Invalid processing facility %s for ingestion";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final String MSG_PRODUCTS_INGESTED = "(I%d) %d products ingested in processing facility %s";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-ingestor ";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(IngestControllerImpl.class);
	
	/** Ingestor configuration */
	@Autowired
	IngestorConfiguration ingestorConfig;
	
	/** Product ingestor */
	@Autowired
	ProductIngestor productIngestor;
	
	/**
	 * Create and log a formatted informational message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private String logInfo(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.info(message);
		
		return message;
	}
	
	/**
	 * Create and log a formatted error message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted error message
	 */
	private String logError(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.error(message);
		
		return message;
	}
	
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
     * Ingest all given products into the storage manager of the given processing facility. If the ID of a product to ingest
     * is null or 0 (zero), then the product will be created, otherwise a matching product will be looked up and updated
     * 
     * @param processingFacility the processing facility to ingest products to
     * @param ingestorProducts a list of product descriptions with product file locations
     * @return HTTP status "CREATED" and a Json list of the products updated and/or created including their product files or
     *         HTTP status "BAD_REQUEST", if an invalid processing facility was given, or
     *         HTTP status "INTERNAL_SERVER_ERROR", if the communication to the Storage Manager or to the Production Planner failed
     */
	@Override
	public ResponseEntity<List<RestProduct>> ingestProducts(String processingFacility, @Valid List<IngestorProduct> ingestorProducts) {
		if (logger.isTraceEnabled()) logger.trace(">>> ingestProducts({}, IngestorProduct[{}])", processingFacility, ingestorProducts.size());
		
		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = RepositoryService.getFacilityRepository().findByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_INVALID_PROCESSING_FACILITY, MSG_ID_INVALID_FACILITY, processingFacility)), 
					HttpStatus.BAD_REQUEST);
		}
		
		List<RestProduct> result = new ArrayList<>();
		
		// Loop over all products to ingest
		for (IngestorProduct ingestorProduct: ingestorProducts) {
			try {
				result.add(productIngestor.ingestProduct(facility, ingestorProduct));
			} catch (ProcessingException e) {
				return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IllegalArgumentException e) {
				return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
			}
		}
		
		logInfo(MSG_PRODUCTS_INGESTED, MSG_ID_PRODUCTS_INGESTED, result.size(), processingFacility);

		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

    /**
     * Get the product file metadata for a product at a given processing facility
     * 
     * @param productId the ID of the product to retrieve
     * @param processingFacility the processing facility to retrieve the product file metadata for
     * @return HTTP status "OK" and the Json representation of the product file metadata, or
     * 	       HTTP status "NOT_FOUND", if no product file for the given product ID exists at the given processing facility 
     */
	@Override
	public ResponseEntity<RestProductFile> getProductFile(Long productId, String processingFacility) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductFile({}, {})", productId, processingFacility);
		
		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = RepositoryService.getFacilityRepository().findByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_INVALID_PROCESSING_FACILITY, MSG_ID_INVALID_FACILITY, processingFacility)), 
					HttpStatus.BAD_REQUEST);
		}
		
		try {
			return new ResponseEntity<>(productIngestor.getProductFile(productId, facility), HttpStatus.CREATED);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

    /**
     * Create the metadata of a new product file for a product at a given processing facility (it is assumed that the
     * files themselves are already pushed to the Storage Manager)
     * 
     * @param productId the ID of the product to retrieve
     * @param processingFacility the name of the processing facility, in which the files have been stored
     * @param productFile the REST product file to store
     * @return HTTP status "CREATED" and the updated REST product file (with ID and version) or
     *         HTTP status "BAD_REQUEST", if the processing facility or the product cannot be found, or if the data for the
     *         product file is invalid (also, if a product file for the given processing facility already exists)
     */
	@Override
	public ResponseEntity<RestProductFile> ingestProductFile(Long productId, String processingFacility,
	        @javax.validation.Valid
	        RestProductFile productFile) {
		if (logger.isTraceEnabled()) logger.trace(">>> ingestProductFile({}, {}, {})", productId, processingFacility, productFile.getProductFileName());
		
		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = RepositoryService.getFacilityRepository().findByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_INVALID_PROCESSING_FACILITY, MSG_ID_INVALID_FACILITY, processingFacility)), 
					HttpStatus.BAD_REQUEST);
		}
		
		try {
			return new ResponseEntity<>(productIngestor.ingestProductFile(productId, facility, productFile), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

    /**
     * Delete a product file for a product from a given processing facility (metadata and actual data file(s))
     * 
     * @param productId the ID of the product to retrieve
     * @param processingFacility the name of the processing facility, from which the files shall be deleted
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the processing facility, the product or the product file did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful, or
     *         HTTP status "INTERNAL_SERVER_ERROR", if the communication to the Storage Manager or to the Production Planner failed
     */
	@Override
	public ResponseEntity<?> deleteProductFile(Long productId, String processingFacility) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductFile({}, {})", productId, processingFacility);

		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = RepositoryService.getFacilityRepository().findByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_INVALID_PROCESSING_FACILITY, MSG_ID_INVALID_FACILITY, processingFacility)), 
					HttpStatus.NOT_FOUND);
		}
		
		try {
			productIngestor.deleteProductFile(productId, facility);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (ProcessingException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

    /**
     * Update the product file metadata for a product at a given processing facility (it is assumed that any new or changed
     * files themselves are already pushed to the Storage Manager)
     * 
     * @param productId the ID of the product to retrieve
     * @param processingFacility the name of the processing facility, in which the files have been stored
     * @param productFile the REST product file to store
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product with the given ID or no processing facility with the given name exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "CONFLICT"and an error message, if the product file has been modified since retrieval by the client
     */
	@Override
	public ResponseEntity<RestProductFile> modifyProductFile(Long productId, String processingFacility, RestProductFile productFile) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProductFile({}, {}, {})", productId, processingFacility, productFile.getProductFileName());

		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = RepositoryService.getFacilityRepository().findByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_INVALID_PROCESSING_FACILITY, MSG_ID_INVALID_FACILITY, processingFacility)), 
					HttpStatus.BAD_REQUEST);
		}
		
		try {
			return new ResponseEntity<>(productIngestor.modifyProductFile(productId, facility, productFile), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		}
	}

}
