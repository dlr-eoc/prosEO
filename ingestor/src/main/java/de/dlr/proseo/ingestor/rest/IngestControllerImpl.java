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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.ingestor.IngestorConfiguration;
import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.ProductFile;
import de.dlr.proseo.ingestor.rest.model.ProductUtil;
import de.dlr.proseo.ingestor.rest.model.RestProduct;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
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
	private static final int MSG_ID_INVALID_PRODUCT_TYPE = 2055;
	private static final int MSG_ID_ORBIT_NOT_FOUND = 2056;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	private static final int MSG_ID_EXCEPTION_THROWN = 9001;
	
	/* Message string constants */
	private static final String MSG_INVALID_PROCESSING_FACILITY = "Invalid processing facility %s for ingestion (%d)";
	private static final String MSG_ERROR_STORING_PRODUCT = "Error storing product of class %s at processing facility %s (%d)";
	private static final String MSG_NEW_PRODUCT_ADDED = "New product with ID %d and product type %s added to database (%d)";
	private static final String MSG_ERROR_NOTIFYING_PLANNER = "Error notifying prosEO Production Planner of new product %d of type %s (%d)";
	private static final String MSG_INVALID_PRODUCT_TYPE = "Invalid product type %s for ingestor product (%d)";
	private static final String MSG_ORBIT_NOT_FOUND = "Orbit %d for spacecraft %s not found (%d)";
	private static final String MSG_EXCEPTION_THROWN = "Exception thrown: %s (%d)";

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ingestor ";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(IngestControllerImpl.class);
	
	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/** Ingestor configuration */
	@Autowired
	IngestorConfiguration ingestorConfig;
	
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
			String message = String.format(MSG_PREFIX + MSG_EXCEPTION_THROWN, e.getMessage(), MSG_ID_EXCEPTION_THROWN);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		ProcessingFacility facility = RepositoryService.getFacilityRepository().findByName(processingFacility);
		if (null == facility) {
			String message = String.format(MSG_PREFIX + MSG_INVALID_PROCESSING_FACILITY, processingFacility, MSG_ID_INVALID_FACILITY);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.BAD_REQUEST);
		}
		
		List<RestProduct> result = new ArrayList<>();
		
		// Loop over all products to ingest
		for (IngestorProduct ingestorProduct: ingestorProducts) {
			// Create a new product in the metadata database
			Product newProduct = ProductUtil.toModelProduct(ingestorProduct);
			
			ProductClass productClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(ingestorProduct.getMissionCode(), ingestorProduct.getProductClass());
			if (null == productClass) {
				String message = String.format(MSG_PREFIX + MSG_INVALID_PRODUCT_TYPE, ingestorProduct.getProductClass(), MSG_ID_INVALID_PRODUCT_TYPE);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				return new ResponseEntity<>(responseHeaders, HttpStatus.BAD_REQUEST);
			}
			newProduct.setProductClass(productClass);
			
			Orbit orbit = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumber(
					ingestorProduct.getOrbit().getSpacecraftCode(), 
					ingestorProduct.getOrbit().getOrbitNumber().intValue());
			if (null == orbit) {
				String message = String.format(MSG_PREFIX + MSG_ORBIT_NOT_FOUND, 
						ingestorProduct.getOrbit().getOrbitNumber().intValue(), ingestorProduct.getOrbit().getSpacecraftCode(), MSG_ID_ORBIT_NOT_FOUND);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				return new ResponseEntity<>(responseHeaders, HttpStatus.BAD_REQUEST);
			}
			
			newProduct = RepositoryService.getProductRepository().save(newProduct);
			logger.info(String.format(MSG_NEW_PRODUCT_ADDED, newProduct.getId(), newProduct.getProductClass().getProductType(),
					MSG_ID_NEW_PRODUCT_ADDED));
			
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
			String storageManagerUrl = facility.getStorageManagerUrl();
			RestTemplate restTemplate = rtb.basicAuthentication(
					ingestorConfig.getStorageManagerUser(), ingestorConfig.getStorageManagerPassword()).build();
			@SuppressWarnings("rawtypes")
			ResponseEntity<Map> responseEntity = restTemplate.postForEntity(storageManagerUrl, postData, Map.class);
			if (!HttpStatus.CREATED.equals(responseEntity.getStatusCode())) {
				String message = String.format(MSG_PREFIX + MSG_ERROR_STORING_PRODUCT, 
						ingestorProduct.getProductClass(), processingFacility, MSG_ID_ERROR_STORING_PRODUCT);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				return new ResponseEntity<>(responseHeaders, responseEntity.getStatusCode());
			}
			
			// Check whether there are open product queries for this product type
			List<ProductQuery> productQueries = RepositoryService.getProductQueryRepository()
					.findUnsatisfiedByProductClass(newProduct.getProductClass().getId());
			if (!productQueries.isEmpty()) {
				// If so, inform the production planner of the new product
				String productionPlannerUrl = ingestorConfig.getProductionPlannerUrl() + "/product/" + String.valueOf(newProduct.getId());
				restTemplate = rtb.basicAuthentication(
						ingestorConfig.getProductionPlannerUser(), ingestorConfig.getProductionPlannerPassword()).build();
				ResponseEntity<?> response = restTemplate.patchForObject(productionPlannerUrl, null, ResponseEntity.class);
				if (!HttpStatus.OK.equals(response.getStatusCode())) {
					String message = String.format(MSG_PREFIX + MSG_ERROR_NOTIFYING_PLANNER, 
							newProduct.getId(), newProduct.getProductClass().getProductType(), MSG_ID_ERROR_NOTIFYING_PLANNER);
					logger.error(message);
					HttpHeaders responseHeaders = new HttpHeaders();
					responseHeaders.set(HTTP_HEADER_WARNING, message);
					return new ResponseEntity<>(responseHeaders, response.getStatusCode());
				}
			}
			
			// Prepare response product
			RestProduct restProduct = ProductUtil.toRestProduct(newProduct);
			result.add(restProduct);
		}
		
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
		
		String message = String.format(MSG_PREFIX + "GET for product file by processing facility not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
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
		
		String message = String.format(MSG_PREFIX + "POST for product file at a processing facility not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Delete a product file for a product from a given processing facility
     * 
     */
	@Override
	public ResponseEntity<?> deleteProductFile(Long productId, String processingFacility) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "DELETE for product file at a processing facility not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Update a product file for a product at a given processing facility
     * 
     */
	@Override
	public ResponseEntity<ProductFile> modifyProductFile(Long productId, String processingFacility, ProductFile productFile) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "PATCH for product file at a processing facility not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

}
