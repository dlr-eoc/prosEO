/**
 * IngestControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.ingestor.rest.model.Product;
import de.dlr.proseo.ingestor.rest.model.ProductFile;

/**
 * Spring MVC controller for the prosEO Ingestor; implements the services required to ingest
 * products from pickup points into the prosEO database, and to query the database about such products
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class IngestControllerImpl implements IngestController {

	/* Message ID constants */
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ingestor ";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(IngestControllerImpl.class);
	
    /**
     * Ingest all given products into the storage manager of the given processing facility. If the ID of a product to ingest
     * is 0 (zero), then the product will be created, otherwise a matching product will be looked up and updated
     * 
     * @param processingFacility the processing facility to ingest products to
     * @param ingestorProduct a list of product descriptions with product file locations
     * @return a Json list of the products updated and/or created including their product files and HTTP status "CREATED"
     */
	@Override
	public ResponseEntity<List<Product>> ingestProducts(String processingFacility, @Valid List<IngestorProduct> ingestorProduct) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "PUT for Ingestor Product not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
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
