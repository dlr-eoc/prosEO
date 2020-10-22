/**
 * ProductClassControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest;

import java.util.ConcurrentModificationException;
import java.util.List;

import javax.persistence.EntityNotFoundException;
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
	//private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-productclass-mgr ";

	/** The product class manager */
	@Autowired
	private ProductClassManager productClassManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductClassControllerImpl.class);

	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + message.replaceAll("\n", " "));
		return responseHeaders;
	}
	
    /**
     * Get product classes, optionally filtered by mission and/or product type
     * 
     * @param mission the mission code
     * @param productType the prosEO product type (if set, missionType should not be set)
     * @return HTTP status "OK" and a list of Json objects representing product classes satisfying the search criteria or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "NOT_FOUND" and an error message, if no product classes matching the search criteria were found
     */
	@Override
	public ResponseEntity<List<RestProductClass>> getRestProductClass(String mission, String productType) {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestProductClass({}, {}, {})", mission, productType);
		
		try {
			return new ResponseEntity<>(productClassManager.getRestProductClass(mission, productType), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

    /**
     * Create a new product class
     * 
     * @param productClass a Json object describing the new product class
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the product class after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
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
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

    /**
     * Get a product class by ID
     * 
     * @param id the database ID of the product class
	 * @return HTTP status "OK" and a Json object corresponding to the product class found or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no product class ID was given, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product class with the given ID exists
     */
	@Override
	public ResponseEntity<RestProductClass> getRestProductClassById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestProductClassById({})", id);

		try {
			return new ResponseEntity<>(productClassManager.getRestProductClassById(id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

    /**
     * Update a product class by ID (does not update its selection rules)
     * 
     * @param id the database ID of the product class to update
     * @param productClass a Json object describing the product class to modify
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the product class after modification
	 *             (with ID and version for all contained objects) or 
	 * 		   HTTP status "NOT_FOUND" and an error message, if no product class with the given ID exists, or
	 *         HTTP status "BAD_REQUEST" and an error message, if any of the input data was invalid, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "CONFLICT"and an error message, if the product class has been modified since retrieval by the client
     */
	@Override
	public ResponseEntity<RestProductClass> modifyRestProductClass(Long id, RestProductClass productClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyRestProductClass({}, {})", id, (null == productClass ? "MISSING" : productClass.getProductType()));
		
		try {
			return new ResponseEntity<>(productClassManager.modifyRestProductClass(id, productClass), HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

    /**
     * Delete a product class by ID (with all its selection rules)
     * 
     * @param id the database ID of the product class to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the product class did not exist, or
	 *         HTTP status "NOT_MODIFIED", if the deletion was unsuccessful, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "BAD_REQUEST", if the product class ID was not given, or if dependent objects exist
     */
	@Override
	public ResponseEntity<?> deleteProductclassById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductclassById({})", id);
		
		try {
			productClassManager.deleteProductclassById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

    /**
     * Get the simple selection rules as formatted string, optionally selected by source class
     * 
     * @param id the database ID of the product class to get the selection rule from
     * @param sourceClass the prosEO product type of the source class, from which the product class can be generated (may be null)
	 * @return HTTP status "OK" and a list of strings describing the selection rules for all configured processors or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "NOT_FOUND" and an error message, if no selection rules matching the search criteria were found
     */
	@Override
    public ResponseEntity<List<SelectionRuleString>> getSelectionRuleStrings(Long id, String sourceClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> getSelectionRuleStrings({}, {})", id, sourceClass);
		
		try {
			return new ResponseEntity<>(productClassManager.getSelectionRuleStrings(id, sourceClass), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

    /**
     * Create a selection rule using Rule Language
     * 
     * @param id the database ID of the product class
     * @param selectionRuleStrings a Json representation of a selection rule in Rule Language
	 * @return HTTP status "CREATED" and a response containing a Json object corresponding to the selection rule after persistence
	 *             (with ID and version for all contained objects) or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "BAD_REQUEST", if any of the input data was invalid
     */
	@Override
	public ResponseEntity<RestProductClass> createSelectionRuleString(Long id, @Valid List<SelectionRuleString> selectionRuleStrings) {
		if (logger.isTraceEnabled()) logger.trace(">>> createSelectionRuleString(SelectionRuleString[{}])", (null == selectionRuleStrings ? "MISSING" : selectionRuleStrings.size()));

		try {
			return new ResponseEntity<>(productClassManager.createSelectionRuleString(id, selectionRuleStrings), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

    /**
     * Get a selection rule by ID
     * 
     * @param ruleid the database ID of the simple selection rule to read
     * @param id the database ID of the product class
	 * @return HTTP status "OK" and a Json object corresponding to the simple selection rule in Rule Language or 
	 *         HTTP status "BAD_REQUEST" and an error message, if no simple selection rule ID was given, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no simple selection rule with the given ID exists
     */
	@Override
	public ResponseEntity<SelectionRuleString> getSelectionRuleString(Long ruleid, Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getSelectionRuleString({}, {})", ruleid, id);
		
		try {
			return new ResponseEntity<>(productClassManager.getSelectionRuleString(ruleid, id), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
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
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "CONFLICT"and an error message, if the simple selection rule has been modified since retrieval by the client
     */
	@Override
	public ResponseEntity<SelectionRuleString> modifySelectionRuleString(Long ruleid, Long id,
			SelectionRuleString selectionRuleString) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifySelectionRuleString({}, {}, {})", ruleid, id, selectionRuleString);
		
		try {
			return new ResponseEntity<>(productClassManager.modifySelectionRuleString(ruleid, id, selectionRuleString), HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		}
	}

    /**
     * Delete a selection rule
     * 
     * @param ruleid the database ID of the simple selection rule to delete
     * @param id the database ID of the product class
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or
	 *         HTTP status "NOT_FOUND", if the if the selection rule to delete or the product class do not exist in the database, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "BAD_REQUEST", if the ID of the product class or the selection rule was not given, or the rule
     *             cannot be deleted due to existing product queries
     */
	@Override
	public ResponseEntity<?> deleteSelectionrule(Long ruleid, Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteSelectionrule({}, {})", ruleid, id);
		
		try {
			productClassManager.deleteSelectionrule(ruleid, id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

    /**
     * Add the configured processor to the selection rule (if it is not already part of the selection rule)
     * 
     * @param configuredProcessor the name of the configured processor to add to the selection rule
     * @param ruleid the database ID of the simple selection rule
     * @param id the database ID of the product class
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the modified simple selection rule in 
	 *             Rule Language, if the addition was successful, or
	 *         HTTP status "NOT_FOUND", if no configured processor with the given name or no selection rule or product class 
	 *             with the given ID exist, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "BAD_REQUEST", if the product class ID, the selection rule ID or the name of the configured processor were not given
     */
	@Override
	public ResponseEntity<SelectionRuleString> addProcessorToRule(String configuredProcessor, Long ruleid, Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> addProcessorToRule({}, {}, {})", configuredProcessor, ruleid, id);
		
		try {
			return new ResponseEntity<>(productClassManager.addProcessorToRule(configuredProcessor, ruleid, id), HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

    /**
     * Remove the configured processor from the selection rule (the selection rule will be disconnected from the configured processor)
     * 
     * @param configuredProcessor the name of the configured processor to remove from the selection rule
     * @param ruleid the database ID of the simple selection rule
     * @param id the database ID of the product class
	 * @return HTTP status "OK" and a response containing a Json object corresponding to the modified simple selection rule in
	 *             Rule Language, if the removal was successful, or
	 *         HTTP status "NOT_FOUND", if no configured processor with the given name or no selection rule or product class
	 *             with the given ID exist, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 *         HTTP status "BAD_REQUEST", if the product class ID, the selection rule ID or the name of the configured processor were not given
     */
	@Override
	public ResponseEntity<SelectionRuleString> removeProcessorFromRule(String configuredProcessor, Long ruleid, Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> removeProcessorFromRule({}, {}, {})", configuredProcessor, ruleid, id);
		
		try {
			return new ResponseEntity<>(productClassManager.removeProcessorFromRule(configuredProcessor, ruleid, id), HttpStatus.OK);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}
