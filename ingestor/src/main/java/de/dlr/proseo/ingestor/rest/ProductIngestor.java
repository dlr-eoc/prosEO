/**
 * ProductIngestor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.ws.rs.ProcessingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;

import de.dlr.proseo.ingestor.IngestorConfiguration;
import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.ProductFileUtil;
import de.dlr.proseo.ingestor.rest.model.ProductUtil;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.ingestor.rest.model.RestProductFile;
import de.dlr.proseo.interfaces.rest.model.RestProductFS;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.IngestorMessage;
import de.dlr.proseo.model.DownloadHistory;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.enums.StorageType;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Services required to ingest products from pickup points into the prosEO database, and to create, read, updated and delete
 * product file metadata
 * 
 * @author Dr. Thomas Bassler
 */
@Component
@Transactional
public class ProductIngestor {

	/* URLs for Storage Manager and Production Planner */
	private static final String URL_PLANNER_NOTIFY = "/product/%d";
	private static final String URL_STORAGE_MANAGER_REGISTER = "/products";
	private static final String URL_STORAGE_MANAGER_DELETE = "/products?pathInfo=%s";
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductIngestor.class);
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/** Ingestor configuration */
	@Autowired
	IngestorConfiguration ingestorConfig;
	
	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** Product Manager */
	@Autowired
	ProductManager productManager;
	
	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	/**
	
	/**
	 * Find a processing facility by name (transaction wrapper for repository method)
	 * 
	 * @param facilityName the name of the facility to retrieve
	 * @return the processing facility found or null, if no such processing facility exists
	 */
	public ProcessingFacility getFacilityByName(String facilityName) {
		return RepositoryService.getFacilityRepository().findByName(facilityName);
	}
	
    /**
     * Ingest all given products into the storage manager of the given processing facility. If the ID of a product to ingest
     * is null or 0 (zero), then the product will be created, otherwise a matching product will be looked up and updated
     * 
     * @param facility the processing facility to ingest products to
     * @param copyFiles indicates, whether to copy the files to a different storage area
     *      (default "true"; only applicable if source and target storage type are the same)
     * @param ingestorProduct a product description with product file locations
     * @param user the username to pass on to the Production Planner
     * @param password the password to pass on to the Production Planner
     * @return a Json representation of the product updated and/or created including their product files
     * @throws IllegalArgumentException if the product ingestion failed (typically due to an error in the Json input)
     * @throws ProcessingException if the communication with the Storage Manager fails
     * @throws SecurityException if a cross-mission data access was attempted
	 */

	@Transactional
	public RestProduct ingestProduct(ProcessingFacility facility, Boolean copyFiles, IngestorProduct ingestorProduct, String user, String password)
			throws IllegalArgumentException, ProcessingException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> ingestProduct({}, {}, {}, PWD)", facility.getName(), ingestorProduct.getProductClass(), user);
		
		// Ensure user is authorized for the product's mission
		if (!securityService.isAuthorizedForMission(ingestorProduct.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					ingestorProduct.getMissionCode(), securityService.getMission()));			
		}
		
		// Default is to copy files, if query parameter is not set
		if (null == copyFiles) {
			copyFiles = true;
		}
		// If the list of auxiliary file names has been set to null, we assume an empty list
		if (null == ingestorProduct.getAuxFileNames()) {
			ingestorProduct.setAuxFileNames(new ArrayList<>());
		}
		
		// Create a new product or find an existing product in the metadata database
		RestProduct newProduct;
		try {
			if (null == ingestorProduct.getId() || 0 == ingestorProduct.getId()) {
				newProduct = productManager.createProduct(ingestorProduct);
			} else {
				newProduct = productManager.getProductById(ingestorProduct.getId());
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_INGESTION_FAILED, e.getMessage()));
		}
		
		// Create product file object in database for the stored files
		de.dlr.proseo.model.ProductFile newProductFile = new de.dlr.proseo.model.ProductFile();
		newProductFile.setProcessingFacility(facility);

		if (copyFiles || !ingestorProduct.getSourceStorageType().equals(facility.getDefaultStorageType().toString())) {
			// Ingest product file and auxiliary files to Storage Manager
			String targetFilePath = ingestToStorageManager(facility, ingestorProduct, newProduct, copyFiles);

			newProductFile.setFilePath(targetFilePath);
		} else {
			// No ingestion required, the files will be used as provided
			newProductFile.setFilePath(ingestorProduct.getMountPoint() + "/" + ingestorProduct.getFilePath());
		}
		
		newProductFile.setProductFileName(ingestorProduct.getProductFileName());
		for (String auxFile: ingestorProduct.getAuxFileNames()) {
			newProductFile.getAuxFileNames().add(auxFile);
		}
		try {
			newProductFile.setStorageType(StorageType.valueOf(facility.getDefaultStorageType().toString()));
		} catch (Exception e) {
			newProductFile.setStorageType(StorageType.OTHER);
		}
		newProductFile.setFileSize(ingestorProduct.getFileSize());
		newProductFile.setChecksum(ingestorProduct.getChecksum());
		newProductFile.setChecksumTime(Instant.from(OrbitTimeFormatter.parse(ingestorProduct.getChecksumTime())));
		Product newModelProduct = RepositoryService.getProductRepository().findById(newProduct.getId()).get();
		newProductFile.setProduct(newModelProduct);
		newProductFile = RepositoryService.getProductFileRepository().save(newProductFile);

		newModelProduct.getProductFile().add(newProductFile);
		// Check for first time ingestion (defines publication time)
		if (null == newModelProduct.getPublicationTime()) {
			newModelProduct.setPublicationTime(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		}
		newModelProduct = RepositoryService.getProductRepository().save(newModelProduct);
		
		// Product ingestion successful
		logger.log(IngestorMessage.NEW_PRODUCT_ADDED, newModelProduct.getId(), newModelProduct.getProductClass().getProductType());

		return ProductUtil.toRestProduct(newModelProduct);
	}

	/**
	 * Store the given model product with the location information from the ingestor product at the given processing facility
	 * 
	 * @param facility the processing facility to store to
	 * @param ingestorProduct product description including file paths for upload
	 * @param modelProduct product model from metadata database
	 * @param copyFiles indicates, whether to copy the files to a different storage area
	 * @return path to the ingested product in the processing facility
	 * @throws ProcessingException if an exception or an error occurred during uploading
	 * @throws IllegalArgumentException if the result object from the Storage Manager cannot be mapped to the return class
	 */
	private String ingestToStorageManager(ProcessingFacility facility, IngestorProduct ingestorProduct,
			RestProduct modelProduct, Boolean copyFiles) throws ProcessingException, IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> ingestToStorageManager({}, {}, {})", facility.getName(), ingestorProduct.getProductClass(), modelProduct.getId());
		
		// Build post data for storage manager
		Map<String, Object> postData = new HashMap<>();
		postData.put("productId", String.valueOf(modelProduct.getId()));
		List<String> filePaths = new ArrayList<>();
		filePaths.add(ingestorProduct.getMountPoint() + "/" + ingestorProduct.getFilePath() + "/" + ingestorProduct.getProductFileName());
		for (String auxFile : ingestorProduct.getAuxFileNames()) {
			filePaths.add(ingestorProduct.getMountPoint() + "/" + ingestorProduct.getFilePath() + "/" + auxFile);
		}
		postData.put("sourceFilePaths", filePaths);
		postData.put("sourceStorageType", ingestorProduct.getSourceStorageType());
		postData.put("targetStorageType", facility.getDefaultStorageType());
		
		// Store the product in the storage manager for the given processing facility
		String storageManagerUrl = facility.getStorageManagerUrl() + URL_STORAGE_MANAGER_REGISTER;
		if (logger.isDebugEnabled()) logger.debug("Calling Storage Manager with URL " + storageManagerUrl + " and data " + postData);
		
		RestTemplate restTemplate = rtb
				.setConnectTimeout(Duration.ofMillis(ingestorConfig.getStorageManagerTimeout()))
				.basicAuthentication(facility.getStorageManagerUser(), facility.getStorageManagerPassword())
				.build();
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> responseEntity = null;
		try {
			responseEntity = restTemplate.postForEntity(storageManagerUrl, postData, Map.class);
		} catch (RestClientException e) {
			String message = (null == responseEntity ? e.getMessage() : 
				responseEntity.getStatusCode().toString() + ": " + responseEntity.getHeaders().getFirst(HttpHeaders.WARNING));
			throw new ProcessingException(logger.log(IngestorMessage.ERROR_STORING_PRODUCT,
					ingestorProduct.getProductClass(), facility.getName(),
					message));
		}
		if (!HttpStatus.CREATED.equals(responseEntity.getStatusCode())) {
			throw new ProcessingException(logger.log(IngestorMessage.ERROR_STORING_PRODUCT,
					ingestorProduct.getProductClass(), facility.getName(), responseEntity.getStatusCode().toString()));
		}
		if (logger.isTraceEnabled()) logger.trace("... Call to Storage Manager successful");
		
		// Extract the product file paths from the response
		ObjectMapper mapper = new ObjectMapper();
		RestProductFS restProductFs = mapper.convertValue(responseEntity.getBody(), RestProductFS.class);

		List<String> responseFilePaths = restProductFs.getRegisteredFilesList();
		if (null == responseFilePaths || responseFilePaths.size() != filePaths.size()) {
			throw new ProcessingException(logger.log(IngestorMessage.UNEXPECTED_NUMBER_OF_FILE_PATHS,
					(null == responseFilePaths ? 0 : responseFilePaths.size()), filePaths.size(), facility.getName()));
		}
		String s = responseFilePaths.get(0);
		int last = s.lastIndexOf('/');
		if (last > 0) {
			s = s.substring(0, last);
		}
		return s;
	}

    /**
     * Get the product file metadata for a product at a given processing facility
     * 
     * @param productId the ID of the product to retrieve
     * @param facility the processing facility to retrieve the product file metadata for
     * @return the Json representation of the product file metadata
     * @throws NoResultException if no product file for the given product ID exists at the given processing facility
     * @throws SecurityException if a cross-mission data access was attempted
     */
	public RestProductFile getProductFile(Long productId, ProcessingFacility facility) throws NoResultException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getProductFile({}, {})", productId, facility.getName());
		
		// Find the product files for the given product ID
		List<ProductFile> productFiles = RepositoryService.getProductFileRepository().findByProductId(productId);
		if (productFiles.isEmpty()) {
			throw new NoResultException(logger.log(IngestorMessage.NO_PRODUCT_FILES, productId));
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
			throw new NoResultException(logger.log(IngestorMessage.NO_PRODUCT_FILES_AT_FACILITY, 
					productId, facility));
		}
		
		// Ensure user is authorized for the product file's mission
		if (!securityService.isAuthorizedForMission(productFile.getProduct().getProductClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					productFile.getProduct().getProductClass().getMission().getCode(), securityService.getMission()));			
		}
		
		logger.log(IngestorMessage.PRODUCT_FILE_RETRIEVED, productId, facility.getName());

		return ProductFileUtil.toRestProductFile(productFile);
	}

    /**
     * Create the metadata of a new product file for a product at a given processing facility (it is assumed that the
     * files themselves are already pushed to the Storage Manager)
     * 
     * @param productId the ID of the product to retrieve
     * @param facility the processing facility, in which the files have been stored
     * @param productFile the REST product file to store
     * @param user the username to pass on to the Production Planner
     * @param password the password to pass on to the Production Planner
     * @return the updated REST product file (with ID and version)
     * @throws IllegalArgumentException if the product cannot be found, or if the data for the
     *         product file is invalid (also, if a product file for the given processing facility already exists)
     * @throws SecurityException if a cross-mission data access was attempted
     */
	public RestProductFile ingestProductFile(Long productId, ProcessingFacility facility, RestProductFile productFile, String user, String password)
			throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> ingestProductFile({}, {}, {}, {}, PWD)", productId, facility, productFile.getProductFileName(), user);

		// Find the product with the given ID
		Optional<Product> product = RepositoryService.getProductRepository().findById(productId);
		
		if (product.isEmpty()) {
			throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_NOT_FOUND, productId));
		}
		Product modelProduct = product.get();

		// Ensure user is authorized for the product's mission
		if (!securityService.isAuthorizedForMission(modelProduct.getProductClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProduct.getProductClass().getMission().getCode(), securityService.getMission()));			
		}
		
		// Error, if a database product file for the given facility exists already
		for (ProductFile modelProductFile: modelProduct.getProductFile()) {
			if (facility.equals(modelProductFile.getProcessingFacility())) {
				throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_FILE_EXISTS, facility));
			}
		}
		// OK, not found!
		
		// Create the database product file
		ProductFile modelProductFile = ProductFileUtil.toModelProductFile(productFile);
		modelProductFile.setProcessingFacility(facility);
		modelProductFile.setProduct(product.get());
		modelProductFile = RepositoryService.getProductFileRepository().save(modelProductFile);
		
		// Check for first time ingestion (defines publication time)
		if (null == modelProduct.getPublicationTime()) {
			modelProduct.setPublicationTime(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		}
		modelProduct.getProductFile().add(modelProductFile);  // Autosave with commit

		// Return the updated REST product file
		logger.log(IngestorMessage.PRODUCT_FILE_INGESTED, productFile.getProductFileName(), productId, facility.getName());

		return ProductFileUtil.toRestProductFile(modelProductFile);
	}

    /**
     * Delete a product file for a product from a given processing facility (metadata and actual data file(s))
     * 
     * @param productId the ID of the product to retrieve
     * @param facility the processing facility, from which the files shall be deleted
     * @param eraseFiles erase the data file(s) from the storage area (default "true")
     * @throws EntityNotFoundException if the product or the product file could not be found
     * @throws RuntimeException if the deletion failed
 	 * @throws ProcessingException if the communication with the Storage Manager fails
 	 * @throws IllegalArgumentException if the product currently satisfies a product query for the given processing facility
     * @throws SecurityException if a cross-mission data access was attempted
     */
	public void deleteProductFile(Long productId, ProcessingFacility facility, Boolean eraseFiles) throws 
			EntityNotFoundException, RuntimeException, ProcessingException, IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductFile({}, {}, {})", productId, facility.getName(), eraseFiles);
		
		// Default is to erase data files from storage area
		if (null == eraseFiles) {
			eraseFiles = true;
		}

		// Find the product with the given ID
		Optional<Product> product = RepositoryService.getProductRepository().findById(productId);
		if (product.isEmpty()) {
			throw new EntityNotFoundException(logger.log(IngestorMessage.PRODUCT_NOT_FOUND, productId));
		}
		
		// Ensure user is authorized for the product's mission
		if (!securityService.isAuthorizedForMission(product.get().getProductClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					product.get().getProductClass().getMission().getCode(), securityService.getMission()));			
		}
		deleteProductFile(product.get(), facility, eraseFiles);
	}
	
    /**
     * Delete a product file for a product from a given processing facility (metadata and actual data file(s))
     * 
     * @param product the product
     * @param facility the processing facility, from which the files shall be deleted
     * @param eraseFiles erase the data file(s) from the storage area (default "true")
     * @throws EntityNotFoundException if the product or the product file could not be found
     * @throws RuntimeException if the deletion failed
 	 * @throws ProcessingException if the communication with the Storage Manager fails
 	 * @throws IllegalArgumentException if the product currently satisfies a product query for the given processing facility
     */
	private void deleteProductFile(Product product, ProcessingFacility facility, Boolean eraseFiles) throws 
			EntityNotFoundException, RuntimeException, ProcessingException, IllegalArgumentException {
		// no logging cause already logged by 
		// deleteProductFile(Long productId, ProcessingFacility facility, Boolean eraseFiles)
		// if (logger.isTraceEnabled()) logger.trace(">>> deleteProductFile({}, {}, {})", product.getId(), facility.getName(), eraseFiles);

		// Error, if a database product file for the given facility does not yet exist
		ProductFile modelProductFile = null;
		for (ProductFile aProductFile: product.getProductFile()) {
			if (facility.equals(aProductFile.getProcessingFacility())) {
				modelProductFile = aProductFile;
			}
		}
		if (null == modelProductFile) {
			throw new EntityNotFoundException(logger.log(IngestorMessage.PRODUCT_FILE_NOT_FOUND, facility.getName()));
		}
		
		// Do not delete product file, if the product is currently satisfying some product query for the same processing facility
		for (ProductQuery productQuery: product.getSatisfiedProductQueries()) {
			if (productQuery.getJobStep().getJob().getProcessingFacility().equals(facility)) {
				throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_QUERY_EXISTS,
						product.getId(), facility.getName()));
			}
		}
		
		if (eraseFiles) {
			// Remove the product file from the processing facility storage: Delete all files individually by path name
			List<String> allFiles = new ArrayList<>(modelProductFile.getAuxFileNames());
			allFiles.add(modelProductFile.getProductFileName());
			if (null != modelProductFile.getZipFileName()) {
				allFiles.add(modelProductFile.getZipFileName());
			}
			for (String fileName : allFiles) {
				String storageManagerUrl = facility.getStorageManagerUrl()
						+ String.format(URL_STORAGE_MANAGER_DELETE, modelProductFile.getFilePath() + "/" + fileName); // file separator is always '/' in Storage Manager

				RestTemplate restTemplate = rtb
						.basicAuthentication(facility.getStorageManagerUser(), facility.getStorageManagerPassword()).build();
				try {
					restTemplate.delete(storageManagerUrl);
				} catch (RestClientException e) {
					throw new ProcessingException(logger.log(IngestorMessage.ERROR_DELETING_PRODUCT,
							product.getId(), facility.getName(), e.getMessage()));
				}
			} 
		}
		
		// Remove links to product file from product download history
		for (DownloadHistory downloadHistory: product.getDownloadHistory()) {
			if (modelProductFile.equals(downloadHistory.getProductFile())) {
				// Link is optional, and download history shall persist even if file is deleted
				downloadHistory.setProductFile(null);
			}
		}
		
		// Remove the product file from the product
		product.getProductFile().remove(modelProductFile);
		
		// Delete the product file metadata
		RepositoryService.getProductFileRepository().delete(modelProductFile);

		// Test whether the deletion was successful
		if (!RepositoryService.getProductFileRepository().findById(modelProductFile.getId()).isEmpty()) {
			throw new RuntimeException(logger.log(IngestorMessage.DELETION_UNSUCCESSFUL, modelProductFile.getProductFileName(), product.getId()));
		}
		
		logger.log(IngestorMessage.PRODUCT_FILE_DELETED, modelProductFile.getProductFileName(), product.getId());
	}

	/**
	 * Delete all product (files) with eviction time older than t.
	 *  
	 * @param t The Instant for eviction time
	 */
	public void deleteProductFilesOlderThan(Instant t) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductFilesOlderThan({})", t);		
		List<Product> products = RepositoryService.getProductRepository().findByEvictionTimeLessThan(t);
		long productFilesDeleted = 0;
		for (Product product : products) {
			for (ProductFile aProductFile: product.getProductFile()) {
				try {
					deleteProductFile(product, aProductFile.getProcessingFacility(), true);
				} 
				// ignore known exceptions cause already logged
				catch (EntityNotFoundException e) {break;}
				catch (ProcessingException e) {break;}
				catch (IllegalArgumentException e) {break;}
				catch (RuntimeException e) {break;};
				productFilesDeleted++;
			}
		}
		logger.log(IngestorMessage.NUMBER_PRODUCT_FILES_DELETED, productFilesDeleted);
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
     * @throws SecurityException if a cross-mission data access was attempted
     */
	public RestProductFile modifyProductFile(Long productId, ProcessingFacility facility, RestProductFile productFile) throws
	EntityNotFoundException, IllegalArgumentException, ConcurrentModificationException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyProductFile({}, {}, {})", productId, facility, productFile.getProductFileName());

		// Find the product with the given ID
		Optional<Product> product = RepositoryService.getProductRepository().findById(productId);
		if (product.isEmpty()) {
			throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_NOT_FOUND, productId));
		}
		
		// Ensure user is authorized for the product's mission
		if (!securityService.isAuthorizedForMission(product.get().getProductClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					product.get().getProductClass().getMission().getCode(), securityService.getMission()));			
		}
		
		// Error, if a database product file for the given facility does not yet exist
		ProductFile modelProductFile = null;
		for (ProductFile aProductFile: product.get().getProductFile()) {
			if (facility.equals(aProductFile.getProcessingFacility())) {
				modelProductFile = aProductFile;
			}
		}
		if (null == modelProductFile) {
			throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_FILE_NOT_FOUND, facility));
		}

		// Make sure we are allowed to change the product file (no intermediate update)
		if (modelProductFile.getVersion() != productFile.getVersion().intValue()) {
			throw new ConcurrentModificationException(logger.log(IngestorMessage.CONCURRENT_UPDATE, productId, facility.getName()));
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
		if (!modelProductFile.getFileSize().equals(changedProductFile.getFileSize())) {
			productFileChanged = true;
			modelProductFile.setFileSize(changedProductFile.getFileSize());
		}
		if (!modelProductFile.getChecksum().equals(changedProductFile.getChecksum())) {
			productFileChanged = true;
			modelProductFile.setChecksum(changedProductFile.getChecksum());
		}
		if (!modelProductFile.getChecksumTime().equals(changedProductFile.getChecksumTime())) {
			productFileChanged = true;
			modelProductFile.setChecksumTime(changedProductFile.getChecksumTime());
		}
		if (null == modelProductFile.getZipFileName()) {
			if (null == changedProductFile.getZipFileName()) {
				// OK, nothing changed
			} else {
				// ZIP archive added
				productFileChanged = true;
				
				// All ZIP archive-related attributes must be set
				modelProductFile.setZipFileName(changedProductFile.getZipFileName());
				modelProductFile.setZipFileSize(changedProductFile.getZipFileSize());
				modelProductFile.setZipChecksum(changedProductFile.getZipChecksum());
				modelProductFile.setZipChecksumTime(changedProductFile.getZipChecksumTime());
			}
		} else {
			if (!modelProductFile.getZipFileName().equals(changedProductFile.getZipFileName())) {
				productFileChanged = true;
				modelProductFile.setZipFileName(changedProductFile.getZipFileName());
			}
			if (!modelProductFile.getZipFileSize().equals(changedProductFile.getZipFileSize())) {
				productFileChanged = true;
				modelProductFile.setZipFileSize(changedProductFile.getZipFileSize());
			}
			if (!modelProductFile.getZipChecksum().equals(changedProductFile.getZipChecksum())) {
				productFileChanged = true;
				modelProductFile.setZipChecksum(changedProductFile.getZipChecksum());
			}
			if (!modelProductFile.getZipChecksumTime().equals(changedProductFile.getZipChecksumTime())) {
				productFileChanged = true;
				modelProductFile.setZipChecksumTime(changedProductFile.getZipChecksumTime());
			} 
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
			logger.log(IngestorMessage.PRODUCT_FILE_MODIFIED, modelProductFile.getProductFileName(), productId);
		} else {
			logger.log(IngestorMessage.PRODUCT_FILE_NOT_MODIFIED, modelProductFile.getProductFileName(), productId);
		}
		
		// Return the updated REST product file
		return ProductFileUtil.toRestProductFile(modelProductFile);
	}

	/**
	 * Notify the Production Planner component of newly ingested products
	 * 
     * @param user the username to pass on to the Production Planner
     * @param password the password to pass on to the Production Planner
     * @param ingestorProduct a product description with product file locations
	 * @throws IllegalArgumentException if the mission code and/or the product type are invalid
	 * @throws RestClientException if an error in the REST API occurs
	 * @throws ProcessingException if the communication with the Production Planner fails
     * @throws SecurityException if a cross-mission data access was attempted
	 */
	public void notifyPlanner(String user, String password, IngestorProduct ingestorProduct, long facilityId)
			throws IllegalArgumentException, RestClientException, ProcessingException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> notifyPlanner({}, PWD, {})", user, ingestorProduct.getProductClass());
		
		// Check whether Planner notification is desirable at all
		if (!ingestorConfig.getNotifyPlanner()) {
			if (logger.isDebugEnabled()) logger.debug("... skipping Planner notification due to configuration setting");
			return;
		}

		// Ensure user is authorized for the product's mission
		if (!securityService.isAuthorizedForMission(ingestorProduct.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					ingestorProduct.getMissionCode(), securityService.getMission()));			
		}
		
		// Retrieve the product class from the database
		ProductClass modelProductClass = RepositoryService.getProductClassRepository()
				.findByMissionCodeAndProductType(ingestorProduct.getMissionCode(), ingestorProduct.getProductClass());
		if (null == modelProductClass) {
			throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_CLASS_INVALID, 
					ingestorProduct.getProductClass()));
		}
		
		// Check whether there are open product queries for this product type
		List<ProductQuery> productQueries = RepositoryService.getProductQueryRepository()
				.findUnsatisfiedByProductClass(modelProductClass.getId());
		if (!productQueries.isEmpty()) {
			// If so, inform the production planner of the new product
			String productionPlannerUrl = ingestorConfig.getProductionPlannerUrl() + String.format(URL_PLANNER_NOTIFY, ingestorProduct.getId());
			productionPlannerUrl += "?facility=" + facilityId;
			
			RestTemplate restTemplate = rtb
					.setConnectTimeout(Duration.ofMillis(ingestorConfig.getProductionPlannerTimeout()))
					.basicAuthentication(user, password)
					.build();
			ResponseEntity<String> response = restTemplate.getForEntity(productionPlannerUrl, String.class);
			if (!HttpStatus.OK.equals(response.getStatusCode())) {
				throw new ProcessingException(logger.log(IngestorMessage.ERROR_NOTIFYING_PLANNER,
						ingestorProduct.getId(), ingestorProduct.getProductClass(), response.getStatusCode().toString()));
			}
		}
	}

	/**
	 * Notify the Production Planner component of newly ingested products
	 * 
     * @param user the username to pass on to the Production Planner
     * @param password the password to pass on to the Production Planner
	 * @param restProductFile the product file that has been ingested
	 * @throws IllegalArgumentException if the mission code and/or the product type are invalid
	 * @throws RestClientException if an error in the REST API occurs
	 * @throws ProcessingException if the communication with the Production Planner fails
     * @throws SecurityException if a cross-mission data access was attempted
	 */
	public void notifyPlanner(String user, String password, RestProductFile restProductFile, long facilityId)
			throws IllegalArgumentException, RestClientException, ProcessingException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> notifyPlanner({}, PWD, {})", user, restProductFile.getProductFileName());

		// Retrieve the product for the given product file
		Optional<Product> modelProduct = RepositoryService.getProductRepository().findById(restProductFile.getProductId());
		if (modelProduct.isEmpty()) {
			throw new IllegalArgumentException(logger.log(IngestorMessage.PRODUCT_NOT_FOUND, restProductFile.getProductId()));
		}
		
		// Ensure user is authorized for the product's mission
		if (!securityService.isAuthorizedForMission(modelProduct.get().getProductClass().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProduct.get().getProductClass().getMission().getCode(), securityService.getMission()));			
		}
		
		// Copy the relevant attributes to an IngestorProduct
		IngestorProduct ingestorProduct = new IngestorProduct();
		ingestorProduct.setId(modelProduct.get().getId());
		ingestorProduct.setMissionCode(modelProduct.get().getProductClass().getMission().getCode());
		ingestorProduct.setProductClass(modelProduct.get().getProductClass().getProductType());
		
		// Notify planner
		notifyPlanner(user, password, ingestorProduct, facilityId);
	}

}
