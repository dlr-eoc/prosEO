/**
 * ProductControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.Orbit;

/**
 * Spring MVC controller for the prosEO Ingestor; implements the services required to ingest
 * products from pickup points into the prosEO database, and to query the database about such products
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProductControllerImpl implements ProductController {
	
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ingestor ";
	private static final String MSG_INGESTOR_PRODUCT_NOT_FOUND_BY_SENSING_START = MSG_PREFIX + "IngestorProduct with sensing start time %s not found (%d)";
	private static final String MSG_INGESTOR_PRODUCT_NOT_FOUND_BY_ID = MSG_PREFIX + "IngestorProduct with id %s not found (%d)";
	
	private static Logger logger = LoggerFactory.getLogger(ProductControllerImpl.class);

	@Override
	public ResponseEntity<?> deleteProductById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<List<de.dlr.proseo.ingestor.rest.model.Product>> getProducts(String mission, String[] productClass,
			Date startTimeFrom, Date startTimeTo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> createProduct(
			de.dlr.proseo.ingestor.rest.model.@Valid Product product) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> getProductById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<de.dlr.proseo.ingestor.rest.model.Product> updateProduct(Long id,
			de.dlr.proseo.ingestor.rest.model.@Valid Product product) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> updateIngestorProduct(String processingFacility, @Valid List<IngestorProduct> ingestorProduct) {
		// TODO Auto-generated method stub
		return null;
	}

}
