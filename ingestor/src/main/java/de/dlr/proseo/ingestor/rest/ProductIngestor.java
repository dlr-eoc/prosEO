/**
 * ProductIngestor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.ingestor.IngestorConfiguration;
import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.ProductFileUtil;
import de.dlr.proseo.ingestor.rest.model.ProductUtil;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.ingestor.rest.model.RestProductFile;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.ProductFile.StorageType;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Services required to ingest products from pickup points into the prosEO database, and to create, read, updated and delete
 * product file metadata
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class ProductIngestor {


	/* Message ID constants */
	private static final int MSG_ID_ERROR_STORING_PRODUCT = 2052;
	private static final int MSG_ID_NEW_PRODUCT_ADDED = 2053;
	private static final int MSG_ID_ERROR_NOTIFYING_PLANNER = 2054;
	private static final int MSG_ID_PRODUCT_INGESTION_FAILED = 2055;
	private static final int MSG_ID_UNEXPECTED_NUMBER_OF_FILE_PATHS = 2057;
	private static final int MSG_ID_PRODUCT_FILE_RETRIEVED = 2059;
	private static final int MSG_ID_NO_PRODUCT_FILES = 2060;
	private static final int MSG_ID_NO_PRODUCT_FILES_AT_FACILITY = 2061;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	
	/* Message string constants */
	private static final String MSG_ERROR_STORING_PRODUCT = "(E%d) Error storing product of class %s at processing facility %s (Storage Manager cause: %s)";
	private static final String MSG_NEW_PRODUCT_ADDED = "(I%d) New product with ID %d and product type %s added to database";
	private static final String MSG_ERROR_NOTIFYING_PLANNER = "(E%d) Error notifying prosEO Production Planner of new product %d of type %s (Production Planner cause: %s)";
	private static final String MSG_PRODUCT_INGESTION_FAILED = "(E%d) Product ingestion failed (cause: %s)";
	private static final String MSG_UNEXPECTED_NUMBER_OF_FILE_PATHS = "(E%d) Unexpected number of file paths (%d, expected: %d) received from Storage Manager at %s";
	private static final String MSG_NO_PRODUCT_FILES = "(E%d) No product files found for product ID %d";
	private static final String MSG_NO_PRODUCT_FILES_AT_FACILITY = "(E%d) No product file found for product ID %d at processing facility %s";
	private static final String MSG_PRODUCT_FILE_RETRIEVED = "(I%d) Product file retrieved for product ID %d at processing facility %s";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductIngestor.class);
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/** Ingestor configuration */
	@Autowired
	IngestorConfiguration ingestorConfig;
	
	/** Product Manager */
	@Autowired
	ProductManager productManager;
	
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
     * Ingest all given products into the storage manager of the given processing facility. If the ID of a product to ingest
     * is null or 0 (zero), then the product will be created, otherwise a matching product will be looked up and updated
     * 
     * @param facility the processing facility to ingest products to
     * @param ingestorProduct a product description with product file locations
     * @return a Json representation of the product updated and/or created including their product files
	 * @throws IllegalArgumentException if the product ingestion failed (typically due to an error in the Json input)
	 * @throws ProcessingException if the communication with the Storage Manager or the Production Planner fails
	 */
	public RestProduct ingestProduct(ProcessingFacility facility, IngestorProduct ingestorProduct)
			throws IllegalArgumentException, ProcessingException {
		if (logger.isTraceEnabled()) logger.trace(">>> ingestProduct({}, {})", facility.getName(), ingestorProduct.getProductClass());
		
		// Create a new product in the metadata database
		RestProduct newProduct;
		try {
			if (null == ingestorProduct.getId() || 0 == ingestorProduct.getId()) {
				newProduct = productManager.createProduct(ingestorProduct);
			} else {
				newProduct = productManager.getProductById(ingestorProduct.getId());
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_INGESTION_FAILED, MSG_ID_PRODUCT_INGESTION_FAILED, e.getMessage()));
		}
		
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
			throw new ProcessingException(logError(MSG_ERROR_STORING_PRODUCT, MSG_ID_ERROR_STORING_PRODUCT,
					ingestorProduct.getProductClass(), facility.getName(), responseEntity.getStatusCode().toString()));
		}
		
		// Extract the product file paths from the response
		@SuppressWarnings("unchecked")
		Map<String, Object> responseBody = (Map<String, Object>) responseEntity.getBody();
		@SuppressWarnings("unchecked")
		List<String> responseFilePaths = (List<String>) responseBody.get("filePaths");
		if (null == responseFilePaths || responseFilePaths.size() != filePaths.size()) {
			throw new ProcessingException(logError(MSG_UNEXPECTED_NUMBER_OF_FILE_PATHS, MSG_ID_UNEXPECTED_NUMBER_OF_FILE_PATHS,
					responseFilePaths.size(), filePaths.size(), facility.getName()));
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
				throw new ProcessingException(logError(MSG_ERROR_NOTIFYING_PLANNER, MSG_ID_ERROR_NOTIFYING_PLANNER,
						newProduct.getId(), newProduct.getProductClass(), response.getStatusCode().toString()));
			}
		}
		
		// Product ingestion successful
		logInfo(MSG_NEW_PRODUCT_ADDED, MSG_ID_NEW_PRODUCT_ADDED, newModelProduct.getId(), newModelProduct.getProductClass().getProductType());

		return ProductUtil.toRestProduct(newModelProduct);
	}

    /**
     * Get the product file metadata for a product at a given processing facility
     * 
     * @param productId the ID of the product to retrieve
     * @param facility the processing facility to retrieve the product file metadata for
     * @return the Json representation of the product file metadata
     * @throws NoResultException if no product file for the given product ID exists at the given processing facility
     */
	public RestProductFile getProductFile(Long productId, ProcessingFacility facility) throws NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductFile({}, {})", productId, facility.getName());
		
		// Find the product files for the given product ID
		List<ProductFile> productFiles = RepositoryService.getProductFileRepository().findByProductId(productId);
		if (productFiles.isEmpty()) {
			throw new NoResultException(logError(MSG_NO_PRODUCT_FILES, MSG_ID_NO_PRODUCT_FILES, productId));
		}
		
		// Find the correct product file for the processing facility
		ProductFile productFile = null;
		for (ProductFile productFileCandidate: productFiles) {
			if (facility.equals(productFileCandidate.getProcessingFacility())) {
				// By definition there is at most one product file per product and processing facility
				productFile = productFileCandidate;
				break;
			}
		}
		if (null == productFile) {
			throw new NoResultException(logError(MSG_NO_PRODUCT_FILES_AT_FACILITY, MSG_ID_NO_PRODUCT_FILES_AT_FACILITY, 
					productId, facility));
		}
		
		logInfo(MSG_PRODUCT_FILE_RETRIEVED, MSG_ID_PRODUCT_FILE_RETRIEVED, productId, facility.getName());

		return ProductFileUtil.toRestProductFile(productFile);
	}

    /**
     * Create the metadata of a new product file for a product at a given processing facility
     * 
     * @param productId the ID of the product to retrieve
     * @param processingFacility 
     */
	public ResponseEntity<ProductFile> ingestProductFile(Long productId, String processingFacility, ProductFile productFile) {
		// TODO Auto-generated method stub
		
		throw new UnsupportedOperationException(logError("POST for product file at a processing facility not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

    /**
     * Delete a product file for a product from a given processing facility (metadata and actual data file(s))
     * 
     */
	public ResponseEntity<?> deleteProductFile(Long productId, String processingFacility) {
		// TODO Auto-generated method stub
		
		throw new UnsupportedOperationException(logError("DELETE for product file at a processing facility not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

    /**
     * Update the product file metadata for a product at a given processing facility
     * 
     */
	public ResponseEntity<ProductFile> modifyProductFile(Long productId, String processingFacility, ProductFile productFile) {
		// TODO Auto-generated method stub
		
		throw new UnsupportedOperationException(logError("PATCH for product file at a processing facility not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

}
