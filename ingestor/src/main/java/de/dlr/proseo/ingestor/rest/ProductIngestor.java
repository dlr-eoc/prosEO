/**
 * ProductIngestor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.ingestor.IngestorConfiguration;
import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.ProductFileUtil;
import de.dlr.proseo.ingestor.rest.model.ProductUtil;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.ingestor.rest.model.RestProductFile;
import de.dlr.proseo.interfaces.rest.model.RestProductFS;
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
@Transactional
public class ProductIngestor {

	/* Message ID constants */
	private static final int MSG_ID_PRODUCT_NOT_FOUND = 2001; // Same as in ProductManager
	private static final int MSG_ID_ERROR_STORING_PRODUCT = 2052;
	private static final int MSG_ID_NEW_PRODUCT_ADDED = 2053;
	private static final int MSG_ID_ERROR_NOTIFYING_PLANNER = 2054;
	private static final int MSG_ID_PRODUCT_INGESTION_FAILED = 2055;
	private static final int MSG_ID_UNEXPECTED_NUMBER_OF_FILE_PATHS = 2057;
	private static final int MSG_ID_PRODUCT_FILE_RETRIEVED = 2059;
	private static final int MSG_ID_NO_PRODUCT_FILES = 2060;
	private static final int MSG_ID_NO_PRODUCT_FILES_AT_FACILITY = 2061;
	private static final int MSG_ID_PRODUCT_FILE_EXISTS = 2062;
	private static final int MSG_ID_PRODUCT_FILE_INGESTED = 2063;
	private static final int MSG_ID_PRODUCT_FILE_NOT_FOUND = 2064;
	private static final int MSG_ID_CONCURRENT_UPDATE = 2065;
	private static final int MSG_ID_PRODUCT_FILE_MODIFIED = 2066;
	private static final int MSG_ID_PRODUCT_FILE_NOT_MODIFIED = 2067;
	private static final int MSG_ID_PRODUCT_FILE_DELETED = 2068;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 2069;
	private static final int MSG_ID_ERROR_DELETING_PRODUCT = 2070;
//	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_PRODUCT_NOT_FOUND = "(E%d) No product found for ID %d";
	private static final String MSG_ERROR_STORING_PRODUCT = "(E%d) Error storing product of class %s at processing facility %s (Storage Manager cause: %s)";
	private static final String MSG_PRODUCT_FILE_EXISTS = "(E%d) Product file for processing facility %s exists";
	private static final String MSG_ERROR_NOTIFYING_PLANNER = "(E%d) Error notifying prosEO Production Planner of new product %d of type %s (Production Planner cause: %s)";
	private static final String MSG_PRODUCT_INGESTION_FAILED = "(E%d) Product ingestion failed (cause: %s)";
	private static final String MSG_UNEXPECTED_NUMBER_OF_FILE_PATHS = "(E%d) Unexpected number of file paths (%d, expected: %d) received from Storage Manager at %s";
	private static final String MSG_NO_PRODUCT_FILES = "(E%d) No product files found for product ID %d";
	private static final String MSG_NO_PRODUCT_FILES_AT_FACILITY = "(E%d) No product file found for product ID %d at processing facility %s";
	private static final String MSG_PRODUCT_FILE_NOT_FOUND = "(E%d) Product file for processing facility %s not found";
	private static final String MSG_CONCURRENT_UPDATE = "(E%d) The product file for product ID %d and processing facility %s has been modified since retrieval by the client";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Deletion unsuccessful for product file %s in product with ID %d";
	private static final String MSG_ERROR_DELETING_PRODUCT = "(E%d) Error deleting product with ID %d from processing facility %s (cause: %s)";

	private static final String MSG_NEW_PRODUCT_ADDED = "(I%d) New product with ID %d and product type %s added to database";
	private static final String MSG_PRODUCT_FILE_RETRIEVED = "(I%d) Product file retrieved for product ID %d at processing facility %s";
	private static final String MSG_PRODUCT_FILE_INGESTED = "(I%d) Product file %s ingested for product ID %d at processing facility %s";
	private static final String MSG_PRODUCT_FILE_MODIFIED = "(I%d) Product file %s for product with id %d modified";
	private static final String MSG_PRODUCT_FILE_NOT_MODIFIED = "(I%d) Product file %s for product with id %d not modified (no changes)";
	private static final String MSG_PRODUCT_FILE_DELETED = "(I%d) Product file %s for product with id %d deleted";

	/* URLs for Storage Manager and Production Planner */
	private static final String URL_PLANNER_NOTIFY = "/product/%d";
	private static final String URL_STORAGE_MANAGER_REGISTER = "/storage/products/register";
	private static final String URL_STORAGE_MANAGER_DELETE = "/storage/products/%d";
	private static final String HTTP_HEADER_WARNING = "Warning";
	
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
		postData.put("productId", String.valueOf(newProduct.getId()));
		List<String> filePaths = new ArrayList<>();
		filePaths.add(ingestorProduct.getMountPoint() + File.separator + ingestorProduct.getFilePath() + File.separator + ingestorProduct.getProductFileName());
		for (String auxFile: ingestorProduct.getAuxFileNames()) {
			filePaths.add(ingestorProduct.getMountPoint() + File.separator + ingestorProduct.getFilePath() + File.separator + auxFile);
		}
		postData.put("sourceFilePaths", filePaths);
		postData.put("sourceStorageType", ingestorProduct.getSourceStorageType());
		postData.put("targetStorageType", ingestorConfig.getDefaultStorageType());
		
		// Store the product in the storage manager for the given processing facility
		String storageManagerUrl = facility.getStorageManagerUrl() + URL_STORAGE_MANAGER_REGISTER;
		if (logger.isDebugEnabled()) logger.debug("Calling Storage Manager with URL " + storageManagerUrl + " and data " + postData);
		RestTemplate restTemplate = rtb.basicAuthentication(
				ingestorConfig.getStorageManagerUser(), ingestorConfig.getStorageManagerPassword()).build();
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> responseEntity = null;
		try {
			responseEntity = restTemplate.postForEntity(storageManagerUrl, postData, Map.class);
		} catch (RestClientException e) {
			throw new ProcessingException(logError(MSG_ERROR_STORING_PRODUCT, MSG_ID_ERROR_STORING_PRODUCT,
					ingestorProduct.getProductClass(), facility.getName(),
					responseEntity.getStatusCode().toString() + ": " + responseEntity.getHeaders().getFirst(HTTP_HEADER_WARNING)));
		}
		if (!HttpStatus.CREATED.equals(responseEntity.getStatusCode())) {
			throw new ProcessingException(logError(MSG_ERROR_STORING_PRODUCT, MSG_ID_ERROR_STORING_PRODUCT,
					ingestorProduct.getProductClass(), facility.getName(), responseEntity.getStatusCode().toString()));
		}
		
		// Extract the product file paths from the response
		ObjectMapper mapper = new ObjectMapper();
		RestProductFS restProductFs = mapper.convertValue(responseEntity.getBody(), RestProductFS.class);
		List<String> responseFilePaths = restProductFs.getRegisteredFilesList();
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
		switch (restProductFs.getTargetStorageType()) {
		case S_3:
			newProductFile.setStorageType(StorageType.S3);
			break;
		case POSIX:
			newProductFile.setStorageType(StorageType.POSIX);
			break;
		case ALLUXIO:
			newProductFile.setStorageType(StorageType.ALLUXIO);
			break;
		default:
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
			String productionPlannerUrl = ingestorConfig.getProductionPlannerUrl() + String.format(URL_PLANNER_NOTIFY, newProduct.getId());
			restTemplate = rtb.basicAuthentication(
					ingestorConfig.getProductionPlannerUser(), ingestorConfig.getProductionPlannerPassword()).build();
			ResponseEntity<String> response = restTemplate.getForEntity(productionPlannerUrl, String.class);
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
     * Create the metadata of a new product file for a product at a given processing facility (it is assumed that the
     * files themselves are already pushed to the Storage Manager)
     * 
     * @param productId the ID of the product to retrieve
     * @param facility the processing facility, in which the files have been stored
     * @param productFile the REST product file to store
     * @return the updated REST product file (with ID and version)
     * @throws IllegalArgumentException if the product cannot be found, or if the data for the
     *         product file is invalid (also, if a product file for the given processing facility already exists)
     */
	public RestProductFile ingestProductFile(Long productId, ProcessingFacility facility, RestProductFile productFile) throws
			IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> ingestProductFile({}, {}, {})", productId, facility, productFile.getProductFileName());

		// Find the product with the given ID
		Optional<Product> product = RepositoryService.getProductRepository().findById(productId);
		if (product.isEmpty()) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_NOT_FOUND, MSG_ID_PRODUCT_NOT_FOUND, productId));
		}
		Product modelProduct = product.get();
		
		// Error, if a database product file for the given facility exists already
		for (ProductFile modelProductFile: modelProduct.getProductFile()) {
			if (facility.equals(modelProductFile.getProcessingFacility())) {
				throw new IllegalArgumentException(logError(MSG_PRODUCT_FILE_EXISTS, MSG_ID_PRODUCT_FILE_EXISTS, facility));
			}
		}
		// OK, not found!
		
		// Create the database product file
		ProductFile modelProductFile = ProductFileUtil.toModelProductFile(productFile);
		modelProductFile.setProcessingFacility(facility);
		modelProductFile.setProduct(product.get());
		modelProductFile = RepositoryService.getProductFileRepository().save(modelProductFile);
		
		modelProduct.getProductFile().add(modelProductFile);  // Autosave with commit
		
		// Check whether there are open product queries for this product type
		List<ProductQuery> productQueries = RepositoryService.getProductQueryRepository()
				.findUnsatisfiedByProductClass(modelProduct.getProductClass().getId());
		if (!productQueries.isEmpty()) {
			// If so, inform the production planner of the new product
			String productionPlannerUrl = ingestorConfig.getProductionPlannerUrl() + String.format(URL_PLANNER_NOTIFY, modelProduct.getId());
			RestTemplate restTemplate = rtb.basicAuthentication(
					ingestorConfig.getProductionPlannerUser(), ingestorConfig.getProductionPlannerPassword()).build();
			ResponseEntity<String> response = restTemplate.getForEntity(productionPlannerUrl, String.class);
			if (!HttpStatus.OK.equals(response.getStatusCode())) {
				throw new ProcessingException(logError(MSG_ERROR_NOTIFYING_PLANNER, MSG_ID_ERROR_NOTIFYING_PLANNER,
						modelProduct.getId(), modelProduct.getProductClass().getProductType(), response.getStatusCode().toString()));
			}
		}
		
		// Return the updated REST product file
		logInfo(MSG_PRODUCT_FILE_INGESTED, MSG_ID_PRODUCT_FILE_INGESTED, productFile.getProductFileName(), productId, facility.getName());

		return ProductFileUtil.toRestProductFile(modelProductFile);
	}

    /**
     * Delete a product file for a product from a given processing facility (metadata and actual data file(s))
     * 
     * @param productId the ID of the product to retrieve
     * @param facility the processing facility, from which the files shall be deleted
     * @throws EntityNotFoundException if the product or the product file could not be found
     * @throws RuntimeException if the deletion failed
 	 * @throws ProcessingException if the communication with the Storage Manager fails
    */
	public void deleteProductFile(Long productId, ProcessingFacility facility) throws 
			EntityNotFoundException, RuntimeException, ProcessingException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductFile({}, {})", productId, facility);

		// Find the product with the given ID
		Optional<Product> product = RepositoryService.getProductRepository().findById(productId);
		if (product.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_PRODUCT_NOT_FOUND, MSG_ID_PRODUCT_NOT_FOUND, productId));
		}
		
		// Error, if a database product file for the given facility does not yet exist
		ProductFile modelProductFile = null;
		for (ProductFile aProductFile: product.get().getProductFile()) {
			if (facility.equals(aProductFile.getProcessingFacility())) {
				modelProductFile = aProductFile;
			}
		}
		if (null == modelProductFile) {
			throw new EntityNotFoundException(logError(MSG_PRODUCT_FILE_NOT_FOUND, MSG_ID_PRODUCT_FILE_NOT_FOUND, facility));
		}
		
		// Remove the product from the processing facility storage
		String storageManagerUrl = facility.getStorageManagerUrl() + String.format(URL_STORAGE_MANAGER_DELETE, product.get().getId());
		RestTemplate restTemplate = rtb.basicAuthentication(
				ingestorConfig.getStorageManagerUser(), ingestorConfig.getStorageManagerPassword()).build();
		try {
			restTemplate.delete(storageManagerUrl);
		} catch (RestClientException e) {
			throw new ProcessingException(logError(MSG_ERROR_DELETING_PRODUCT, MSG_ID_ERROR_DELETING_PRODUCT,
					product.get().getId(), facility.getName(), e.getMessage()));
		}

		// Delete the product
		RepositoryService.getProductFileRepository().deleteById(modelProductFile.getId());

		// Test whether the deletion was successful
		if (!RepositoryService.getProductFileRepository().findById(modelProductFile.getId()).isEmpty()) {
			throw new RuntimeException(logError(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, modelProductFile.getProductFileName(), productId));
		}
		
		logInfo(MSG_PRODUCT_FILE_DELETED, MSG_ID_PRODUCT_FILE_DELETED, modelProductFile.getProductFileName(), productId);
	}

    /**
     * Update the product file metadata for a product at a given processing facility
     * 
     * @param productId the ID of the product to retrieve
     * @param facility the processing facility, in which the files have been stored
     * @param productFile the REST product file to store
     * @return the updated REST product file (with ID and version)
     * @throws IllegalArgumentException if the product cannot be found, or if the data for the
     *         product file is invalid (also, if a product file for the given processing facility already exists)
     * @throws ConcurrentModificationException if the product file was modified since its retrieval by the client
     */
	public RestProductFile modifyProductFile(Long productId, ProcessingFacility facility, RestProductFile productFile) throws
	EntityNotFoundException, IllegalArgumentException, ConcurrentModificationException {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProductFile({}, {}, {})", productId, facility, productFile.getProductFileName());

		// Find the product with the given ID
		Optional<Product> product = RepositoryService.getProductRepository().findById(productId);
		if (product.isEmpty()) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_NOT_FOUND, MSG_ID_PRODUCT_NOT_FOUND, productId));
		}
		
		// Error, if a database product file for the given facility does not yet exist
		ProductFile modelProductFile = null;
		for (ProductFile aProductFile: product.get().getProductFile()) {
			if (facility.equals(aProductFile.getProcessingFacility())) {
				modelProductFile = aProductFile;
			}
		}
		if (null == modelProductFile) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_FILE_NOT_FOUND, MSG_ID_PRODUCT_FILE_NOT_FOUND, facility));
		}

		// Make sure we are allowed to change the product file (no intermediate update)
		if (modelProductFile.getVersion() != productFile.getVersion().intValue()) {
			throw new ConcurrentModificationException(logError(MSG_CONCURRENT_UPDATE, MSG_ID_CONCURRENT_UPDATE, productId, facility.getName()));
		}
		
		// Add object links (these cannot have changed, since they were the search criteria)
		modelProductFile.setProduct(product.get());
		modelProductFile.setProcessingFacility(facility);
		
		// Update the database product file replacing all attributes by the values in the given REST product file
		boolean productFileChanged = false;
		ProductFile changedProductFile = ProductFileUtil.toModelProductFile(productFile);
		if (!modelProductFile.getProductFileName().equals(changedProductFile.getProductFileName())) {
			productFileChanged = true;
			modelProductFile.setProductFileName(changedProductFile.getProductFileName());
		}
		if (!modelProductFile.getFilePath().equals(changedProductFile.getFilePath())) {
			productFileChanged = true;
			modelProductFile.setFilePath(changedProductFile.getFilePath());
		}
		if (!modelProductFile.getStorageType().equals(changedProductFile.getStorageType())) {
			productFileChanged = true;
			modelProductFile.setStorageType(changedProductFile.getStorageType());
		}
		
		// The set of aux file names gets replaced completely, if not equal
		if (!modelProductFile.getAuxFileNames().equals(changedProductFile.getAuxFileNames())) {
			productFileChanged = true;
			modelProductFile.getAuxFileNames().clear();
			modelProductFile.getAuxFileNames().addAll(changedProductFile.getAuxFileNames());
		}
		
		if (productFileChanged) {
			modelProductFile.incrementVersion();
			modelProductFile = RepositoryService.getProductFileRepository().save(modelProductFile);
			logInfo(MSG_PRODUCT_FILE_MODIFIED, MSG_ID_PRODUCT_FILE_MODIFIED, modelProductFile.getProductFileName(), productId);
		} else {
			logInfo(MSG_PRODUCT_FILE_NOT_MODIFIED, MSG_ID_PRODUCT_FILE_NOT_MODIFIED, modelProductFile.getProductFileName(), productId);
		}
		
		// Return the updated REST product file
		return ProductFileUtil.toRestProductFile(modelProductFile);
	}

}
