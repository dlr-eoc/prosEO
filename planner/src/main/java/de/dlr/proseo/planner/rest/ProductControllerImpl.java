/**
 * ProductControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
/**
 * Spring MVC controller for the prosEO planner; implements the services required to handle products.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class ProductControllerImpl implements ProductController {

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_HEADER_SUCCESS = "Success";
	private static final String MSG_PREFIX = "199 proseo-planner ";
	
	private static Logger logger = LoggerFactory.getLogger(JobControllerImpl.class);
	
	/**
	 * Product created and available, sent by prosEO Ingestor
	 * 
	 */
	@Override
	public ResponseEntity<?> getObjectByProductid(String productid) {
		// todo 
		// look for product
		if (productid != null) {
			logger.info("GET product/" + productid);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_SUCCESS, "Simulated, to be implemented!");
			return new ResponseEntity<>("{\n" + 
					"  \"GET\": \"product/" + productid + "\"\n" + 
							"}", responseHeaders, HttpStatus.OK);
		} else {
			String message = String.format(MSG_PREFIX + "GET not implemented (%d)", 2001);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
	}

}
