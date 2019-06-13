/**
 * ProductControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.ingestor.rest.model.IngestorProduct;
import de.dlr.proseo.model.Product;

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
	private static final String MSG_INGESTOR_PRODUCT_NOT_FOUND_BY_DESCRIPTOR = MSG_PREFIX + "IngestorProduct with descriptor %s not found (%d)";
	private static final String MSG_INGESTOR_PRODUCT_NOT_FOUND_BY_ID = MSG_PREFIX + "IngestorProduct with id %s not found (%d)";

	private static Logger logger = LoggerFactory.getLogger(ProductControllerImpl.class);

	/* (non-Javadoc)
	 * @see de.dlr.proseo.ingestor.rest.IngestorProductController#getProducts(java.lang.String, java.lang.String)
	 */
	@Override
	public ResponseEntity<List<IngestorProduct>> getIngestorProducts(String id, String descriptor) {
		logger.trace(">>> Entering getIngestorProducts");
		// TODO Auto-generated method stub
		
		// Dummy implementation
		Product product123 = new Product();
		product123.setPlUniqueId("123");
		product123.setArchiveLocation("DEF");
		product123.setCacheLocation("here");
		Product productABC = new Product();
		productABC.setPlUniqueId("456");
		productABC.setArchiveLocation("ABC");
		productABC.setCacheLocation("there");
		
		List<IngestorProduct> products = new ArrayList<>();
		IngestorProduct ip123 = new IngestorProduct(product123.getPlUniqueId(), product123.getArchiveLocation(), product123.getCacheLocation());
		IngestorProduct ipABC = new IngestorProduct(productABC.getPlUniqueId(), productABC.getArchiveLocation(), productABC.getCacheLocation());
		if (null == id && null == descriptor) {
			products.add(ip123);
			products.add(ipABC);
		} else if (null != id) {
			if ("123".equals(id)) {
				products.add(ip123);
			} else {
				String message = String.format(MSG_INGESTOR_PRODUCT_NOT_FOUND_BY_ID, id, 1001);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				logger.trace("<<< Leaving getIngestorProducts with error 'Invalid ID'");
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			}
		} else {
			if ("ABC".equals(descriptor)) {
				products.add(ipABC);
			} else {
				String message = String.format(MSG_INGESTOR_PRODUCT_NOT_FOUND_BY_DESCRIPTOR, descriptor, 1001);
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

	/* (non-Javadoc)
	 * @see de.dlr.proseo.ingestor.rest.IngestorProductController#getProductById(java.lang.String)
	 */
	@Override
	public ResponseEntity<IngestorProduct> getIngestorProductById(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.ingestor.rest.IngestorProductController#updateProduct(java.lang.String, de.dlr.proseo.ingestor.rest.model.IngestorProduct)
	 */
	@Override
	public ResponseEntity<IngestorProduct> updateIngestorProduct(String id, IngestorProduct product) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.ingestor.rest.IngestorProductController#deleteProductById(java.lang.String)
	 */
	@Override
	public ResponseEntity<?> deleteProductById(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.dlr.proseo.ingestor.rest.IngestorProductController#getProductByDescriptor(java.lang.String)
	 */
	@Override
	public ResponseEntity<IngestorProduct> getIngestorProductByDescriptor(String descriptor) {
		// TODO Auto-generated method stub
		return null;
	}

}
