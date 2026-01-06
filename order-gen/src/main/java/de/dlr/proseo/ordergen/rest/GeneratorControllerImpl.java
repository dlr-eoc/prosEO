/**
 * GeneratorControllerImpl.java
 *
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordergen.rest;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.rest.GeneratorController;
import de.dlr.proseo.model.rest.model.RestOrder;

/**
 * Controller for the generation of processing orders
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class GeneratorControllerImpl implements GeneratorController {
	private static ProseoLogger logger = new ProseoLogger(GeneratorControllerImpl.class);
	
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.ORDERGEN);
	
	@Autowired
	private OrderManager orderManager;

	/**
	 * Fire a data-driven trigger to generate a processing order from the given input product
	 * 
	 * @param productId Database ID of the input product to use for order generation
	 * @return HTTP status "CREATED" and a response containing a Json object
	 *         corresponding to the generated processing order after persistence 
	 *         (with ID and version for all contained objects)
	 *         or HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted,
	 *         or HTTP status "NOT_FOUND", if no product with the given database ID exists
	 */
	@Override
	public ResponseEntity<List<RestOrder>> generateForProduct(Long productId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> generateForProduct({})", productId);

		try {
			return new ResponseEntity<>(orderManager.generateForProduct(productId), HttpStatus.CREATED);
		} catch (NoSuchElementException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}
