/**
 * ProductClassControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.ws.rs.ServerErrorException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.Parameter.ParameterType;
import de.dlr.proseo.model.SimplePolicy;
import de.dlr.proseo.model.SimplePolicy.DeltaTime;
import de.dlr.proseo.model.SimplePolicy.PolicyType;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.SelectionRule;
import de.dlr.proseo.prodclmgr.rest.model.ProductClassUtil;
import de.dlr.proseo.prodclmgr.rest.model.RestParameter;
import de.dlr.proseo.prodclmgr.rest.model.RestProductClass;
import de.dlr.proseo.prodclmgr.rest.model.RestSimplePolicy;
import de.dlr.proseo.prodclmgr.rest.model.RestSimpleSelectionRule;
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
	private static final int MSG_ID_PRODUCT_CLASS_MISSING = 2100;
	private static final int MSG_ID_INVALID_MISSION_CODE = 2101;
	private static final int MSG_ID_INVALID_PROCESSING_MODE = 2102;
	private static final int MSG_ID_INVALID_ENCLOSING_CLASS = 2103;
	private static final int MSG_ID_INVALID_COMPONENT_CLASS = 2104;
	private static final int MSG_ID_INVALID_PROCESSOR_CLASS = 2105;
	private static final int MSG_ID_INVALID_SOURCE_CLASS = 2106;
	private static final int MSG_ID_INVALID_PARAMETER_KEY = 2107;
	private static final int MSG_ID_INVALID_PARAMETER_TYPE = 2108;
	private static final int MSG_ID_INVALID_PROCESSOR = 2109;
	private static final int MSG_ID_INVALID_POLICY_TYPE = 2110;
	private static final int MSG_ID_INVALID_TIME_UNIT = 2111;
	private static final int MSG_ID_PRODUCT_CLASS_EXISTS = 2112;
	private static final int MSG_ID_PRODUCT_CLASS_SAVE_FAILED = 2113;
	private static final int MSG_ID_PRODUCT_CLASS_CREATED = 2114;
	private static final int MSG_ID_PRODUCT_CLASS_NOT_FOUND = 2115;
	private static final int MSG_ID_PROCESSING_MODE_MISSING = 2116;
	private static final int MSG_ID_RULE_STRING_MISSING = 2117;
	private static final int MSG_ID_INVALID_RULE_STRING = 2118;
	private static final int MSG_ID_SELECTION_RULES_CREATED = 2119;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_PRODUCT_CLASS_MISSING = "(E%d) Product class not set";
	private static final String MSG_PRODUCT_CLASS_NOT_FOUND = "(E%d) Product class with id %d not found";
	private static final String MSG_PROCESSING_MODE_MISSING = "(E%d) Processing mode missing in selection rule string %s";
	private static final String MSG_INVALID_MISSION_CODE = "(E%d) Invalid mission code %s";
	private static final String MSG_RULE_STRING_MISSING = "(E%d) Selection rule missing in selection rule string %s";
	private static final String MSG_INVALID_RULE_STRING = "(E%d) Syntax error in selection rule %s: %s";
	private static final String MSG_PRODUCT_CLASS_EXISTS = "(E%d) Product class %s already exists for mission %s";
	private static final String MSG_PRODUCT_CLASS_SAVE_FAILED = "(E%d) Save failed for product class %s in mission %s (cause: %s)";
	private static final String MSG_INVALID_PROCESSING_MODE = "(E%d) Processing mode %s not defined for mission %s";
	private static final String MSG_INVALID_ENCLOSING_CLASS = "(E%d) Enclosing product class %s is not defined for mission %s";
	private static final String MSG_INVALID_COMPONENT_CLASS = "(E%d) Component product class %s is not defined for mission %s";
	private static final String MSG_INVALID_PROCESSOR_CLASS = "(E%d) Processor class %s is not defined for mission %s";
	private static final String MSG_INVALID_SOURCE_CLASS = "(E%d) Source product class %s is not defined for mission %s";
	private static final String MSG_INVALID_PARAMETER_KEY = "(E%d) Parameter key missing in filter condition %s";
	private static final String MSG_INVALID_PARAMETER_TYPE = "(E%d) Invalid parameter type %s in filter condition, one of {STRING, INTEGER, BOOLEAN, DOUBLE} expected";
	private static final String MSG_INVALID_PROCESSOR = "(E%d) Configured processor %s is not defined";
	private static final String MSG_INVALID_POLICY_TYPE = "(E%d) Invalid policy type %s in selection rule, see Generic IPF Interface Specifications for valid values";
	private static final String MSG_INVALID_TIME_UNIT = "(E%d) Invalid time unit %s in selection rule, one of {DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS} expected";
	private static final String MSG_PRODUCT_CLASS_CREATED = "(I%d) Product class of type %s created for mission %s";
	private static final String MSG_SELECTION_RULES_CREATED = "(I%d) %d selection rules added to product class of type %s in mission %s";
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
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders(logError("GET for RestProductClass not implemented (%d)", MSG_ID_NOT_IMPLEMENTED)), 
				HttpStatus.NOT_IMPLEMENTED);
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
