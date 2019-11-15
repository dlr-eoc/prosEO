/**
 * ProductClassControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.NoResultException;
import javax.validation.Valid;
import javax.ws.rs.ServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.prodclmgr.rest.model.RestProductClass;
import de.dlr.proseo.prodclmgr.rest.model.SelectionRuleString;

/**
 * Spring MVC controller for the prosEO Ingestor; implements the services required to manage product classes and their selection rules
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProductClassControllerImpl implements ProductclassController {
	
	/* Message ID constants */
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-productclass-mgr ";

	/** The product class manager */
	@Autowired
	private ProductClassManager productClassManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductClassControllerImpl.class);

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
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + message);
		return responseHeaders;
	}
	
    /**
     * Get product classes by mission, product type or mission type
     * 
     * @param mission the mission code
     * @param productType the prosEO product type (if set, missionType should not be set)
     * @param missionType the mission-defined product type (if set, productType should not be set)
	 * @return HTTP status "OK" and a list of Json objects representing product classes satisfying the search criteria or
	 *         HTTP status "NOT_FOUND" and an error message, if no product classes matching the search criteria were found
     */
	@Override
	public ResponseEntity<List<RestProductClass>> getRestProductClass(String mission, String productType, String missionType) {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestProductClass({}, {}, {})", mission, productType, missionType);
		
		try {
			return new ResponseEntity<>(productClassManager.getRestProductClass(mission, productType, missionType), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		}
	}

    /**
     * Create a new product class
     * 
     * @param productClass a Json object describing the new product class
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the product class after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
     */
	@Override
	public ResponseEntity<RestProductClass> createRestProductClass(RestProductClass productClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> createRestProductClass({})", (null == productClass ? "MISSING" : productClass.getProductType()));
		
		try {
			return new ResponseEntity<>(productClassManager.createRestProductClass(productClass), HttpStatus.CREATED);
		} catch (ServerErrorException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.resolve(e.getResponse().getStatus()));
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

    /**
     * Get a product class by ID
     * 
     * @param id the database ID of the product class
	 * @return HTTP status "OK" and a Json object corresponding to the product class found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no product class ID was given, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product class with the given ID exists
     */
	@Override
	public ResponseEntity<RestProductClass> getRestProductClassById(Long id) {
		// TODO Auto-generated method stub

		return new ResponseEntity<>(
				errorHeaders(logError("GET for RestProductClass with id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Update a product class by ID
     * 
     * @param id the database ID of the product class to update
     * @param productClass a Json object describing the product class to modify
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the product class after modification
	 *             (with ID and version for all contained objects) or 
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product class with the given ID exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "CONFLICT"and an error message, if the product class has been modified since retrieval by the client
     */
	@Override
	public ResponseEntity<RestProductClass> modifyRestProductClass(Long id, RestProductClass productClass) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders(logError("PATCH for RestProductClass with id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Delete a product class by ID
     * 
     * @param id the database ID of the product class to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the product class did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
     */
	@Override
	public ResponseEntity<?> deleteProductclassById(Long id) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders(logError("DELETE for RestProductClass with id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Get the simple selection rules as formatted string, optionally selected by source class
     * 
     * @param id the database ID of the product class to get the selection rule from
     * @param sourceclass the prosEO product type of the source class, from which the product class can be generated (may be null)
	 * @return HTTP status "OK" and a list of strings describing the selection rules for all configured processors or
	 *         HTTP status "NOT_FOUND" and an error message, if no selection rules matching the search criteria were found
     */
	@Override
    public ResponseEntity<List<SelectionRuleString>> getSelectionRuleStrings(Long id, String sourceClass) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders(logError("GET for SelectionRuleString with RestProductClass id and source product type not implemented (%d)", MSG_ID_NOT_IMPLEMENTED)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Create a selection rule using Rule Language
     * 
     * @param id the database ID of the product class
     * @param selectionRuleString a Json representation of a selection rule in Rule Language
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the selection rule after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
     */
	@Override
	public ResponseEntity<RestProductClass> createSelectionRuleString(Long id, @Valid List<SelectionRuleString> selectionRuleStrings) {
		if (logger.isTraceEnabled()) logger.trace(">>> createSelectionRuleString(SelectionRuleString[{}])", (null == selectionRuleStrings ? "MISSING" : selectionRuleStrings.size()));

		try {
			return new ResponseEntity<>(productClassManager.createSelectionRuleString(id, selectionRuleStrings), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		}
	}

    /**
     * Get a selection rule by ID
     * 
     * @param ruleid the database ID of the simple selection rule to read
     * @param id the database ID of the product class
	 * @return HTTP status "OK" and a Json object corresponding to the simple selection rule in Rule Language or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no simple selection rule ID was given, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no simple selection rule with the given ID exists
     */
	@Override
	public ResponseEntity<SelectionRuleString> getSelectionRuleString(Long ruleid, Long id) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders(logError("GET for SelectionRuleString with RestProductClass id and SimpleSelectionRule id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Update a selection rule using Rule Language
     * 
     * @param ruleid the database ID of the simple selection rule to update
     * @param id the database ID of the product class
     * @param selectionRuleString a Json object representing the simple selection rule in Rule Language
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the modified simple selection rule in Rule Language 
	 *             (with ID and version for all contained objects) or 
	 * 		   HTTP status "NOT_FOUND" and an error message, if if the rule ID is invalid or the rule does not belong to the given product class, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "CONFLICT"and an error message, if the simple selection rule has been modified since retrieval by the client
     */
	@Override
	public ResponseEntity<SelectionRuleString> modifySelectionRuleString(Long ruleid, Long id,
			SelectionRuleString selectionRuleString) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders(logError("PATCH for SelectionRuleString with RestProductClass id and SimpleSelectionRule id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Delete a selection rule
     * 
     * @param ruleid the database ID of the simple selection rule to delete
     * @param id the database ID of the product class
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the simple selection rule did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful
     */
	@Override
	public ResponseEntity<?> deleteSelectionrule(Long ruleid, Long id) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders(logError("DELETE for SelectionRuleString with RestProductClass id and SimpleSelectionRule id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Add the configured processor to the selection rule
     * 
     * @param configuredProcessor the name of the configured processor to add to the selection rule
     * @param ruleid the database ID of the simple selection rule
     * @param id the database ID of the product class
	 * @return a response entity with HTTP status "NO_CONTENT", if the addition was successful, or
	 *         HTTP status "NOT_FOUND", if no configured processor with the given name or no selection rule or product class with the given ID exist
     */
	@Override
	public ResponseEntity<?> addProcessorToRule(String configuredProcessor, Long ruleid, Long id) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders(logError("PUT for SelectionRuleString with RestProductClass id, SimpleSelectionRule id and configured processor name not implemented (%d)", MSG_ID_NOT_IMPLEMENTED)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Remove the configured processor from the selection rule (the selection rule will be disconnected from the configured processor)
     * 
     * @param configuredProcessor the name of the configured processor to remove from the selection rule
     * @param ruleid the database ID of the simple selection rule
     * @param id the database ID of the product class
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if no configured processor with the given name or no selection rule or product class with the given ID exist
     */
	@Override
	public ResponseEntity<?> removeProcessorFromRule(String configuredProcessor, Long ruleid, Long id) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders(logError("DELETE for SelectionRuleString with RestProductClass id, SimpleSelectionRule id and configured processor name not implemented (%d)", MSG_ID_NOT_IMPLEMENTED)), 
				HttpStatus.NOT_IMPLEMENTED);
	}

}
