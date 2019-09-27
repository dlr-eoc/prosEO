/**
 * ProductClassControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest;

import java.util.List;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.prodclmgr.rest.model.RestProductClass;
import de.dlr.proseo.prodclmgr.rest.model.SelectionRuleString;
import de.dlr.proseo.prodclmgr.rest.model.SimpleSelectionRule;

/**
 * Spring MVC controller for the prosEO Ingestor; implements the services required to ingest
 * products from pickup points into the prosEO database, and to query the database about such products
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProductClassControllerImpl implements ProductclassController {
	
	/* Message ID constants */
	private static final int MSG_ID_PRODUCT_CLASS_MISSING = 2100;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_PRODUCT_CLASS_MISSING = "Product class not set (%d)";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-productclass-mgr ";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductClassControllerImpl.class);

    /**
     * Get product classes by mission, product type or mission type
     * 
     * @param mission the mission code
     * @param productType the prosEO product type (if set, missionType should not be set)
     * @param missionType the mission-defined product type (if set, productType should not be set)
     * @return a list of product classes conforming to the search criteria and HTTP status OK, or an empty list and HTTP status
     *         NOT_FOUND, if no such product classes exist
     */
	@Override
	public ResponseEntity<List<RestProductClass>> getRestProductClass(String mission, String productType, String missionType) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "GET for RestProductClass not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Create a new product class
     * 
     * @param productClass a Json object describing the new product class
     * @return a Json object describing the product class created (including ID and version) and HTTP status CREATED
     */
	@Override
	public ResponseEntity<RestProductClass> createRestProductClass(RestProductClass productClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> createRestProductClass({})", (null == productClass ? "MISSING" : productClass.getProductType()));
		
		if (null == productClass) {
			String message = String.format(MSG_PREFIX + MSG_PRODUCT_CLASS_MISSING, MSG_ID_PRODUCT_CLASS_MISSING);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
		}
		
		ProductClass modelProductClass = new ProductClass();
		
		
		// TODO Auto-generated method stub

		String message = String.format(MSG_PREFIX + "POST for RestProductClass not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Get a product class by ID
     * 
     * @param id the database ID of the product class
     * @return a Json object describing the product class and HTTP status OK
     */
	@Override
	public ResponseEntity<RestProductClass> getRestProductClassById(Long id) {
		// TODO Auto-generated method stub

		String message = String.format(MSG_PREFIX + "GET for RestProductClass with id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Update a product class by ID
     * 
     * @param id the database ID of the product class to update
     * @param productClass a Json object describing the product class to modify
     * @return a Json object describing the product class modified (including incremented version) and HTTP status OK,
     * 		   or HTTP status NOT_FOUND, if the requested product class does not exist
    */
	@Override
	public ResponseEntity<RestProductClass> modifyRestProductClass(Long id, RestProductClass productClass) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "PATCH for RestProductClass with id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Delete a product class by ID
     * 
     * @param id the database ID of the product class to delete
     * @return HTTP status NO_CONTENT
     */
	@Override
	public ResponseEntity<?> deleteProductclassById(Long id) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "DELETE for RestProductClass with id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Get the simple selection rules as formatted string, optionally selected by source class
     * 
     * @param id the database ID of the product class to get the selection rule from
     * @param sourceclass the prosEO product type of the source class, from which the product class can be generated (may be null)
     * @return a list of strings describing the selection rules for all configured processors
     */
	@Override
    public ResponseEntity<List<SelectionRuleString>> getSelectionRuleStrings(Long id, String sourceClass) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "GET for SelectionRuleString with RestProductClass id and source product type not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Create a selection rule
     * 
     * @param id the database ID of the product class
     * @param selectionRuleString a Json representation of a selection rule in Rule Language
     * @return a Json object representing the simple selection rule created and HTTP status CREATED
     */
	@Override
	public ResponseEntity<SimpleSelectionRule> createSelectionRuleString(Long id, @Valid List<SelectionRuleString> selectionRuleString) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "POST for SelectionRuleString with RestProductClass id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Get a selection rule by ID
     * 
     * @param ruleid the database ID of the simple selection rule to read
     * @param id the database ID of the product class
     * @return a Json object representing the simple selection rule in Rule Language and HTTP status OK,
     * 		   or HTTP status NOT_FOUND, if the rule ID is invalid or the rule does not belong to the given product class
     */
	@Override
	public ResponseEntity<SelectionRuleString> getSelectionRuleString(Long ruleid, Long id) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "GET for SelectionRuleString with RestProductClass id and SimpleSelectionRule id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Update a selection rule
     * 
     * @param ruleid the database ID of the simple selection rule to update
     * @param id the database ID of the product class
     * @param selectionRuleString a Json object representing the simple selection rule in Rule Language
     * @return a Json object representing the modified simple selection rule in Rule Language and HTTP status OK,
     * 		   or HTTP status NOT_FOUND, if the rule ID is invalid or the rule does not belong to the given product class
     */
	@Override
	public ResponseEntity<SelectionRuleString> modifySelectionRuleString(Long ruleid, Long id,
			SelectionRuleString selectionRuleString) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "PATCH for SelectionRuleString with RestProductClass id and SimpleSelectionRule id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Delete a selection rule
     * 
     * @param ruleid the database ID of the simple selection rule to delete
     * @param id the database ID of the product class
     * @return HTTP status NO_CONTENT
     */
	@Override
	public ResponseEntity<?> deleteSelectionrule(Long ruleid, Long id) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "DELETE for SelectionRuleString with RestProductClass id and SimpleSelectionRule id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Add the configured processor to the selection rule
     * 
     * @param configuredProcessor the name of the configured processor to add to the selection rule
     * @param ruleid the database ID of the simple selection rule
     * @param id the database ID of the product class
     * @return HTTP status NO_CONTENT
     */
	@Override
	public ResponseEntity<?> addProcessorToRule(String configuredProcessor, Long ruleid, Long id) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "PUT for SelectionRuleString with RestProductClass id, SimpleSelectionRule id and configured processor name not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Remove the configured processor from the selection rule (the selection rule will be disconnected from the configured processor)
     * 
     * @param configuredProcessor the name of the configured processor to remove from the selection rule
     * @param ruleid the database ID of the simple selection rule
     * @param id the database ID of the product class
     * @return HTTP status NO_CONTENT
     */
	@Override
	public ResponseEntity<?> removeProcessorFromRule(String configuredProcessor, Long ruleid, Long id) {
		// TODO Auto-generated method stub
		
		String message = String.format(MSG_PREFIX + "DELETE for SelectionRuleString with RestProductClass id, SimpleSelectionRule id and configured processor name not implemented (%d)", MSG_ID_NOT_IMPLEMENTED);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_IMPLEMENTED);
	}

}
