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

	/* (non-Javadoc)
	 * @see de.dlr.proseo.ingestor.rest.IngestorProductController#getProducts(java.lang.String, java.lang.String)
	 */
	@Override
	public ResponseEntity<List<IngestorProduct>> getIngestorProducts(Long id, String sensingStart) {
		logger.trace(">>> Entering getIngestorProducts");
		// TODO Auto-generated method stub
		
		// Dummy implementation
		Product product123 = new Product();
		product123.setId(123L);
		product123.setSensingStartTime(Orbit.orbitTimeFormatter.parse("2019-07-22T12:27:38.654321", Instant::from));
		product123.setSensingStopTime(Orbit.orbitTimeFormatter.parse("2019-07-22T13:57:38.654321", Instant::from));
		Product productABC = new Product();
		productABC.setId(456L);
		productABC.setSensingStartTime(Orbit.orbitTimeFormatter.parse("2019-07-22T13:57:38.654321", Instant::from));
		productABC.setSensingStopTime(Orbit.orbitTimeFormatter.parse("2019-07-22T15:27:38.654321", Instant::from));
		
		List<IngestorProduct> products = new ArrayList<>();
		IngestorProduct ip123 = new IngestorProduct(product123.getId(),
				Orbit.orbitTimeFormatter.format(product123.getSensingStartTime()),
				Orbit.orbitTimeFormatter.format(product123.getSensingStopTime()));
		IngestorProduct ipABC = new IngestorProduct(productABC.getId(),
				Orbit.orbitTimeFormatter.format(productABC.getSensingStartTime()),
				Orbit.orbitTimeFormatter.format(productABC.getSensingStopTime()));
		if (null == id && null == sensingStart) {
			products.add(ip123);
			products.add(ipABC);
		} else if (null != id) {
			if (123L == id) {
				products.add(ip123);
			} else if (456L == id) {
				products.add(ipABC);
			} else {
				String message = String.format(MSG_INGESTOR_PRODUCT_NOT_FOUND_BY_ID, id, 1001);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				logger.trace("<<< Leaving getIngestorProducts with error 'Invalid ID'");
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			}
		} else {
			if ("2019-07-22T13:57:38.654321".equals(sensingStart)) {
				products.add(ipABC);
			} else if ("2019-07-22T12:27:38.654321".equals(sensingStart)) {
				products.add(ip123);
			} else {
				String message = String.format(MSG_INGESTOR_PRODUCT_NOT_FOUND_BY_SENSING_START, sensingStart, 1001);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				logger.trace("<<< Leaving getIngestorProducts with error 'Invalid descriptor'");
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			}
		}
		logger.debug("Returning " + products);
		logger.trace("<<< Leaving getIngestorProducts with products " + products);
		return new ResponseEntity<>(products, HttpStatus.OK);
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.ingestor.rest.IngestorProductController#createProduct(de.dlr.proseo.ingestor.rest.model.IngestorProduct)
	 */
	@Override
	public ResponseEntity<IngestorProduct> createIngestorProduct(IngestorProduct product) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<IngestorProduct> getIngestorProductById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<IngestorProduct> updateIngestorProduct(Long id, @Valid IngestorProduct ingestorProduct) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResponseEntity<?> deleteProductById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

}
