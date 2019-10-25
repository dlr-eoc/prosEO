/**
 * IngestControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.ingestor.IngestorConfiguration;
import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.ProductFile;
import de.dlr.proseo.ingestor.rest.model.ProductUtil;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile.StorageType;
import de.dlr.proseo.model.ProductQuery;
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
	private static final int MSG_ID_ERROR_STORING_PRODUCT = 2052;
	private static final int MSG_ID_NEW_PRODUCT_ADDED = 2053;
	private static final int MSG_ID_ERROR_NOTIFYING_PLANNER = 2054;
	private static final int MSG_ID_PRODUCT_INGESTION_FAILED = 2055;
	private static final int MSG_ID_UNEXPECTED_NUMBER_OF_FILE_PATHS = 2057;
	private static final int MSG_ID_PRODUCTS_INGESTED = 2058;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	private static final int MSG_ID_EXCEPTION_THROWN = 9001;
	
	/* Message string constants */
	private static final String MSG_INVALID_PROCESSING_FACILITY = "(E%d) Invalid processing facility %s for ingestion";
	private static final String MSG_ERROR_STORING_PRODUCT = "(E%d) Error storing product of class %s at processing facility %s";
	private static final String MSG_NEW_PRODUCT_ADDED = "(I%d) New product with ID %d and product type %s added to database";
	private static final String MSG_ERROR_NOTIFYING_PLANNER = "(E%d) Error notifying prosEO Production Planner of new product %d of type %s";
	private static final String MSG_PRODUCT_INGESTION_FAILED = "(E%d) Product ingestion failed (cause: %s)";
	private static final String MSG_UNEXPECTED_NUMBER_OF_FILE_PATHS = "(E%d) Unexpected number of file paths (%d, expected: %d) received from Storage Manager at %s";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
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
	
	/** Product controller */
	@Autowired
	ProductControllerImpl productController;
	
	/** single TransactionTemplate shared amongst all methods in this instance */
	private final TransactionTemplate transactionTemplate;

	/**
	 * Constructor using constructor-injection to supply the PlatformTransactionManager
	 * 
	 * @param transactionManager the platform transaction manager
	 */
	public IngestControllerImpl(PlatformTransactionManager transactionManager) {
		Assert.notNull(transactionManager, "The 'transactionManager' argument must not be null.");
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}
	
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
     * Ingest all given products into the storage manager of the given processing facility. If the ID of a product to ingest
     * is 0 (zero), then the product will be created, otherwise a matching product will be looked up and updated
     * 
     * @param processingFacility the processing facility to ingest products to
     * @param ingestorProducts a list of product descriptions with product file locations
     * @return a Json list of the products updated and/or created including their product files and HTTP status "CREATED",
     *   or HTTP status "BAD_REQUEST", if an invalid processing facility was given
     */
	@Override
	public ResponseEntity<List<RestProduct>> ingestProducts(String processingFacility, @Valid List<IngestorProduct> ingestorProducts) {
		if (logger.isTraceEnabled()) logger.trace(">>> ingestProducts({}, IngestorProduct[{}])", processingFacility, ingestorProducts.size());
		
		// Check whether the given processing facility is valid
		try {
			processingFacility = URLDecoder.decode(processingFacility, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new ResponseEntity<>(
					errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		final ProcessingFacility facility = RepositoryService.getFacilityRepository().findByName(processingFacility);
		if (null == facility) {
			return new ResponseEntity<>(
					errorHeaders(MSG_INVALID_PROCESSING_FACILITY, MSG_ID_INVALID_FACILITY, processingFacility), 
					HttpStatus.BAD_REQUEST);
		}
		
		List<RestProduct> result = new ArrayList<>();
		
		// Loop over all products to ingest
		for (IngestorProduct ingestorProduct: ingestorProducts) {
			ResponseEntity<RestProduct> transactionResponse = transactionTemplate.execute(new TransactionCallback<>() {

				@Override
				public ResponseEntity<RestProduct> doInTransaction(TransactionStatus txStatus) {
					// Create a new product in the metadata database
					ResponseEntity<RestProduct> createResponse = productController.createProduct(ingestorProduct);
					if (!HttpStatus.CREATED.equals(createResponse.getStatusCode())) {
						return new ResponseEntity<>(createResponse.getHeaders(), createResponse.getStatusCode());
					}
					RestProduct newProduct = createResponse.getBody();
					
					// Build post data for storage manager
					Map<String, Object> postData = new HashMap<>();
					postData.put("mountDirPath", ingestorProduct.getMountPoint());
					postData.put("productId", String.valueOf(newProduct.getId()));
					List<String> filePaths = new ArrayList<>();
					filePaths.add(ingestorProduct.getFilePath() + File.separator + ingestorProduct.getProductFileName());
					for (String auxFile: ingestorProduct.getAuxFileNames()) {
						filePaths.add(ingestorProduct.getFilePath() + File.separator + auxFile);
					}
					postData.put("exactFilePaths", filePaths);
					
					// Store the product in the storage manager for the given processing facility
					String storageManagerUrl = facility.getStorageManagerUrl() + "/store";
					RestTemplate restTemplate = rtb.basicAuthentication(
							ingestorConfig.getStorageManagerUser(), ingestorConfig.getStorageManagerPassword()).build();
					@SuppressWarnings("rawtypes")
					ResponseEntity<Map> responseEntity = restTemplate.postForEntity(storageManagerUrl, postData, Map.class);
					if (!HttpStatus.CREATED.equals(responseEntity.getStatusCode())) {
						return new ResponseEntity<>(
								errorHeaders(MSG_ERROR_STORING_PRODUCT, MSG_ID_ERROR_STORING_PRODUCT, ingestorProduct.getProductClass(), facility.getName()), 
								responseEntity.getStatusCode());
					}
					
					// Extract the product file paths from the response
					@SuppressWarnings("unchecked")
					Map<String, Object> responseBody = (Map<String, Object>) responseEntity.getBody();
					@SuppressWarnings("unchecked")
					List<String> responseFilePaths = (List<String>) responseBody.get("filePaths");
					if (null == responseFilePaths || responseFilePaths.size() != filePaths.size()) {
						return new ResponseEntity<>(
								errorHeaders(MSG_UNEXPECTED_NUMBER_OF_FILE_PATHS, MSG_ID_UNEXPECTED_NUMBER_OF_FILE_PATHS, responseFilePaths.size(), filePaths.size(), facility.getName()), 
								HttpStatus.INTERNAL_SERVER_ERROR);
					}
					de.dlr.proseo.model.ProductFile newProductFile = new de.dlr.proseo.model.ProductFile();
					newProductFile.setProcessingFacility(facility);
					newProductFile.setFilePath((new File(responseFilePaths.get(0))).getParent());
					newProductFile.setProductFileName(ingestorProduct.getProductFileName());
					for (String auxFile: ingestorProduct.getAuxFileNames()) {
						newProductFile.getAuxFileNames().add(auxFile);
					}
					if (responseFilePaths.get(0).startsWith("s3")) {
						newProductFile.setStorageType(StorageType.S3);
					} else if (responseFilePaths.get(0).startsWith("file")) {
						newProductFile.setStorageType(StorageType.POSIX);
					} else if (responseFilePaths.get(0).startsWith("alluxio")) {
						newProductFile.setStorageType(StorageType.ALLUXIO);
					} else {
						newProductFile.setStorageType(StorageType.OTHER);
					}
					Product newModelProduct = RepositoryService.getProductRepository().findById(newProduct.getId()).get();
					newProductFile.setProduct(newModelProduct);
					newProductFile = RepositoryService.getProductFileRepository().save(newProductFile);
					newModelProduct.getProductFile().add(newProductFile);
					newModelProduct = RepositoryService.getProductRepository().save(newModelProduct);
					
					// Check whether there are open product queries for this product type
					List<ProductQuery> productQueries = RepositoryService.getProductQueryRepository()
							.findUnsatisfiedByProductClass(newModelProduct.getProductClass().getId());
					if (!productQueries.isEmpty()) {
						// If so, inform the production planner of the new product
						String productionPlannerUrl = ingestorConfig.getProductionPlannerUrl() + "/product/" + String.valueOf(newProduct.getId());
						restTemplate = rtb.basicAuthentication(
								ingestorConfig.getProductionPlannerUser(), ingestorConfig.getProductionPlannerPassword()).build();
						ResponseEntity<?> response = restTemplate.getForObject(productionPlannerUrl, null, ResponseEntity.class);
						if (!HttpStatus.OK.equals(response.getStatusCode())) {
							return new ResponseEntity<>(
									errorHeaders(MSG_ERROR_NOTIFYING_PLANNER, MSG_ID_ERROR_NOTIFYING_PLANNER, newProduct.getId(), newProduct.getProductClass()), 
									response.getStatusCode());
						}
					}
					
					// Product ingestion successful
					return new ResponseEntity<>(ProductUtil.toRestProduct(newModelProduct), HttpStatus.OK);
				}
			});
			if (!HttpStatus.OK.equals(transactionResponse.getStatusCode())) {
				return new ResponseEntity<>(
						errorHeaders(MSG_PRODUCT_INGESTION_FAILED, MSG_ID_PRODUCT_INGESTION_FAILED, transactionResponse.getHeaders().get("Warning")), 
						transactionResponse.getStatusCode());
			}
			
			RestProduct restProduct = transactionResponse.getBody();
			
			logInfo(MSG_NEW_PRODUCT_ADDED, MSG_ID_NEW_PRODUCT_ADDED, restProduct.getId(), restProduct.getProductClass());
			
			// Prepare response product
			result.add(restProduct);
		}
		
		logInfo(MSG_PRODUCTS_INGESTED, MSG_ID_PRODUCTS_INGESTED, result.size(), processingFacility);

		return new ResponseEntity<>(result, HttpStatus.CREATED);
	}

    /**
     * Get the product file for a product at a given processing facility
     * 
     * @param productId the ID of the product to retrieve
     * @param processingFacility 
     */
	@Override
	public ResponseEntity<ProductFile> getProductFile(Long productId, String processingFacility) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders("GET for product file by processing facility not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Create a new product file for a product at a given processing facility
     * 
     */
	@Override
	public ResponseEntity<ProductFile> ingestProductFile(Long productId, String processingFacility,
	        @javax.validation.Valid
	        ProductFile productFile) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders("POST for product file at a processing facility not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Delete a product file for a product from a given processing facility
     * 
     */
	@Override
	public ResponseEntity<?> deleteProductFile(Long productId, String processingFacility) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders("DELETE for product file at a processing facility not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Update a product file for a product at a given processing facility
     * 
     */
	@Override
	public ResponseEntity<ProductFile> modifyProductFile(Long productId, String processingFacility, ProductFile productFile) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders("PATCH for product file at a processing facility not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
	}

}
