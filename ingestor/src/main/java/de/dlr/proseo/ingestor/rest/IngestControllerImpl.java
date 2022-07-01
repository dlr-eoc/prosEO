/**
 * IngestControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.ConcurrentModificationException;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.validation.Valid;
import javax.ws.rs.ProcessingException;

import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.ingestor.IngestorConfiguration;
import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.ingestor.rest.model.RestProductFile;
import de.dlr.proseo.model.ProcessingFacility;

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
	private static final int MSG_ID_AUTH_MISSING_OR_INVALID = 2056;
	private static final int MSG_ID_NOTIFICATION_FAILED = 2091;
	private static final int MSG_ID_ERROR_ACQUIRE_SEMAPHORE = 2092;
	private static final int MSG_ID_ERROR_RELEASE_SEMAPHORE = 2093;
	// private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	private static final int MSG_ID_EXCEPTION_THROWN = 9001;
	
	/* Message string constants */
	private static final String MSG_INVALID_PROCESSING_FACILITY = "(E%d) Invalid processing facility %s for ingestion";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final String MSG_AUTH_MISSING_OR_INVALID = "(E%d) Basic authentication missing or invalid: %s";
	private static final String MSG_NOTIFICATION_FAILED = "(E%d) Notification of Production Planner failed (cause: %s)";
	private static final String MSG_ERROR_ACQUIRE_SEMAPHORE = "(E%d) Error to acquire semaphore from prosEO Production Planner (cause: %s)";
	private static final String MSG_ERROR_RELEASE_SEMAPHORE = "(E%d) Error to release semaphore from prosEO Production Planner (cause: %s)";

	private static final String MSG_PRODUCTS_INGESTED = "(I%d) %d products ingested in processing facility %s";
	
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-ingestor ";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(IngestControllerImpl.class);
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/** Ingestor configuration */
	@Autowired
	IngestorConfiguration ingestorConfig;
	
	/** Product ingestor */
	@Autowired
	ProductIngestor productIngestor;
	
	/**
	 * Create and log a formatted message at the given level
	 * 
	 * @param level the logging level to use
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private String log(Level level, String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		if (Level.ERROR.equals(level)) {
			logger.error(message);
		} else if (Level.WARN.equals(level)) {
			logger.warn(message);
		} else {
			logger.info(message);
		}

		return message;
	}

	/**
	 * Create and log a formatted informational message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private String logInfo(String messageFormat, int messageId, Object... messageParameters) {
		return log(Level.INFO, messageFormat, messageId, messageParameters);
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
		return log(Level.ERROR, messageFormat, messageId, messageParameters);
	}
	
	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + (null == message ? "null" : message.replaceAll("\n", " ")));
		return responseHeaders;
	}
	
	/**
	 * Parse an HTTP authentication header into username and password
	 * @param authHeader the authentication header to parse
	 * @return a string array containing the username and the password
	 * @throws IllegalArgumentException if the authentication header cannot be parsed
	 */
	private String[] parseAuthenticationHeader(String authHeader) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> parseAuthenticationHeader({})", authHeader);

		if (null == authHeader) {
			String message = logError(MSG_AUTH_MISSING_OR_INVALID, MSG_ID_AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException (message);
		}
		String[] authParts = authHeader.split(" ");
		if (2 != authParts.length || !"Basic".equals(authParts[0])) {
			String message = logError(MSG_AUTH_MISSING_OR_INVALID, MSG_ID_AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException (message);
		}
		String[] userPassword = (new String(Base64.getDecoder().decode(authParts[1]))).split(":"); // guaranteed to work as per BasicAuth specification
		return userPassword;
	}

	/**
	 * Ask production planner for a slot to manipulate product(s)
	 * 
	 * @param user The user
	 * @param password The password
	 * @return true after semaphore was available 
	 */
	@SuppressWarnings("unused")
	private Boolean acquireSemaphore(String user, String password) {
		if (logger.isTraceEnabled()) logger.trace(">>> acquireSemaphore({}, PWD)", user);
		
		String url = ingestorConfig.getProductionPlannerUrl() + "/semaphore/acquire";
		RestTemplate restTemplate = rtb
				.setConnectTimeout(Duration.ofMillis(ingestorConfig.getProductionPlannerTimeout()))
				.basicAuthentication(user, password)
				.build();
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		if (!HttpStatus.OK.equals(response.getStatusCode())) {
			throw new ProcessingException(logError(MSG_ERROR_ACQUIRE_SEMAPHORE, MSG_ID_ERROR_ACQUIRE_SEMAPHORE,
					response.getStatusCode().toString()));
		}
		return true;
	}

	/**
	 * Release semaphore of production planner
	 * 
	 * @param user The user
	 * @param password The password
	 * @return true after semaphore was released 
	 */
	@SuppressWarnings("unused")
	private Boolean releaseSemaphore(String user, String password) {
		if (logger.isTraceEnabled()) logger.trace(">>> releaseSemaphore({}, PWD)", user);
		
		String url = ingestorConfig.getProductionPlannerUrl() + "/semaphore/release";
		RestTemplate restTemplate = rtb
				.setConnectTimeout(Duration.ofMillis(ingestorConfig.getProductionPlannerTimeout()))
				.basicAuthentication(user, password)
				.build();
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		if (!HttpStatus.OK.equals(response.getStatusCode())) {
			throw new ProcessingException(logError(MSG_ERROR_RELEASE_SEMAPHORE, MSG_ID_ERROR_RELEASE_SEMAPHORE,
					response.getStatusCode().toString()));
		}
		return true;
	}

	/**
     * Ingest all given products into the storage manager of the given processing facility. If the ID of a product to ingest
     * is null or 0 (zero), then the product will be created, otherwise a matching product will be looked up and updated.
     * 
     * The Production Planner will be notified of all ingested products. However, notification is optional, and if it fails,
     * the Ingestor still returns with HTTP status CREATED. We rely on a cyclical check by the Production Planner to pick up all newly
     * ingested products, should it not have been notified.
     * 
     * @param processingFacility the processing facility to ingest products to
     * @param copyFiles indicates, whether to copy the files to a different storage area
     *      (default "true"; only applicable if source and target storage type are the same)
     * @param ingestorProducts a list of product descriptions with product file locations
     * @param httpHeaders the HTTP request headers (injected)
     * @return HTTP status "CREATED" and a Json list of the products updated and/or created including their product files or
     *         HTTP status "BAD_REQUEST", if an invalid processing facility was given, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
     *         HTTP status "INTERNAL_SERVER_ERROR", if the communication to the Storage Manager or to the Production Planner failed
     */
	@Override
	public ResponseEntity<List<RestProduct>> ingestProducts(String processingFacility, Boolean copyFiles, @Valid List<IngestorProduct> ingestorProducts,
			HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> ingestProducts({}, IngestorProduct[{}])", processingFacility, ingestorProducts.size());
		
		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = productIngestor.getFacilityByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_INVALID_PROCESSING_FACILITY, MSG_ID_INVALID_FACILITY, processingFacility)), 
					HttpStatus.BAD_REQUEST);
		}
		
		// Get username and password from HTTP Authentication header for authentication with Production Planner
		String[] userPassword = parseAuthenticationHeader(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION));
		
		// Loop over all products to ingest
		List<RestProduct> result = new ArrayList<>();

		for (IngestorProduct ingestorProduct: ingestorProducts) {
			try {
				// TODO Thoroughly test semaphore acquisition/release --> blocks Planner unnecessarily
				// productIngestor.acquireSemaphore(userPassword[0], userPassword[1]);
				RestProduct restProduct = productIngestor.ingestProduct(facility, copyFiles, ingestorProduct, userPassword[0], userPassword[1]);
				result.add(restProduct);
				ingestorProduct.setId(restProduct.getId());
				if (logger.isTraceEnabled()) logger.trace("... product ingested, now notifying planner");
			} catch (ProcessingException e) {
				return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IllegalArgumentException e) {
				return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
			} catch (SecurityException e) {
				return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
			} finally {
				// productIngestor.releaseSemaphore(userPassword[0], userPassword[1]);
			}
			
			try {
				productIngestor.notifyPlanner(userPassword[0], userPassword[1], ingestorProduct, facility.getId());
				if (logger.isTraceEnabled()) logger.trace("... planner notification successful");
			} catch (Exception e) {
				// If notification fails, log warning, but otherwise ignore
				log(Level.WARN, MSG_NOTIFICATION_FAILED, MSG_ID_NOTIFICATION_FAILED, e.getMessage());
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
     * @param httpHeaders the HTTP request headers (injected)
     * @return HTTP status "OK" and the Json representation of the product file metadata, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
     * 	       HTTP status "NOT_FOUND", if no product file for the given product ID exists at the given processing facility 
     */
	@Override
	public ResponseEntity<RestProductFile> getProductFile(Long productId, String processingFacility, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductFile({}, {})", productId, processingFacility);
		
		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = productIngestor.getFacilityByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_INVALID_PROCESSING_FACILITY, MSG_ID_INVALID_FACILITY, processingFacility)), 
					HttpStatus.BAD_REQUEST);
		}
		
		try {
			return new ResponseEntity<>(productIngestor.getProductFile(productId, facility), HttpStatus.CREATED);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

    /**
     * Create the metadata of a new product file for a product at a given processing facility (it is assumed that the
     * files themselves are already pushed to the Storage Manager)
     * 
     * The Production Planner will be notified of the ingested product. However, notification is optional, and if it fails,
     * the Ingestor still returns with HTTP status CREATED. We rely on a cyclical check by the Production Planner to pick up all newly
     * ingested products, should it not have been notified.
     * 
     * @param productId the ID of the product to retrieve
     * @param processingFacility the name of the processing facility, in which the files have been stored
     * @param productFile the REST product file to store
     * @param httpHeaders the HTTP request headers (injected)
     * @return HTTP status "CREATED" and the updated REST product file (with ID and version) or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
     *         HTTP status "BAD_REQUEST", if the processing facility or the product cannot be found, or if the data for the
     *         product file is invalid (also, if a product file for the given processing facility already exists)
     */
	@Override
	public ResponseEntity<RestProductFile> ingestProductFile(Long productId, String processingFacility,
	        @javax.validation.Valid RestProductFile productFile, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> ingestProductFile({}, {}, {})", productId, processingFacility, productFile.getProductFileName());
		
		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = productIngestor.getFacilityByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_INVALID_PROCESSING_FACILITY, MSG_ID_INVALID_FACILITY, processingFacility)), 
					HttpStatus.BAD_REQUEST);
		}
		
		// Get username and password from HTTP Authentication header for authentication with Production Planner
		String[] userPassword = parseAuthenticationHeader(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION));
		
		RestProductFile restProductFile = null;
		try {
			// TODO Thoroughly test semaphore acquisition/release --> blocks Planner unnecessarily
			// productIngestor.acquireSemaphore(userPassword[0], userPassword[1]);
			restProductFile = productIngestor.ingestProductFile(
						productId, facility, productFile, userPassword[0], userPassword[1]);
		} catch (ProcessingException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (LockAcquisitionException e) {
			e.printStackTrace();
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			// productIngestor.releaseSemaphore(userPassword[0], userPassword[1]);
		}
		
		try {
			productIngestor.notifyPlanner(userPassword[0], userPassword[1], restProductFile, facility.getId());
		} catch (Exception e) {
			// If notification fails, log warning, but otherwise ignore
			log(Level.WARN, MSG_NOTIFICATION_FAILED, MSG_ID_NOTIFICATION_FAILED, e.getMessage());
		}

		return new ResponseEntity<>(restProductFile, HttpStatus.CREATED);
	}

    /**
     * Delete a product file for a product from a given processing facility (metadata and actual data file(s))
     * 
     * @param productId the ID of the product to retrieve
     * @param processingFacility the name of the processing facility, from which the files shall be deleted
     * @param eraseFiles erase the data file(s) from the storage area (default "true")
     * @param httpHeaders the HTTP request headers (injected)
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the processing facility, the product or the product file did not exist, or
	 *         HTTP status "BAD_REQUEST", if the product currently satisfies a product query for the given processing facility, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful, or
 	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
    *         HTTP status "INTERNAL_SERVER_ERROR", if the communication to the Storage Manager or to the Production Planner failed
     */
	@Override
	public ResponseEntity<?> deleteProductFile(Long productId, String processingFacility, Boolean eraseFiles, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductFile({}, {})", productId, processingFacility);

		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = productIngestor.getFacilityByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_INVALID_PROCESSING_FACILITY, MSG_ID_INVALID_FACILITY, processingFacility)), 
					HttpStatus.NOT_FOUND);
		}
		
		try {
			productIngestor.deleteProductFile(productId, facility, eraseFiles);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ProcessingException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
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
     * @param httpHeaders the HTTP request headers (injected)
     * @param productFile the REST product file to store
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product with the given ID or no processing facility with the given name exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "CONFLICT"and an error message, if the product file has been modified since retrieval by the client
     */
	@Override
	public ResponseEntity<RestProductFile> modifyProductFile(Long productId, String processingFacility, RestProductFile productFile,
			HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProductFile({}, {}, {})", productId, processingFacility, productFile.getProductFileName());

		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(
					errorHeaders(logError(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = productIngestor.getFacilityByName(processingFacility);
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
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}
