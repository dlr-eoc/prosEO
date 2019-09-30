/**
 * ProductClassControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

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
import de.dlr.proseo.prodclmgr.rest.model.ProductClassUtil;
import de.dlr.proseo.prodclmgr.rest.model.RestProductClass;
import de.dlr.proseo.prodclmgr.rest.model.SelectionRuleString;

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
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_PRODUCT_CLASS_MISSING = "(E%d) Product class not set";
	private static final String MSG_INVALID_MISSION_CODE = "(E%d) Invalid mission code %s";
	private static final String MSG_PRODUCT_CLASS_EXISTS = "(E%d) Product class %s already exists for mission %s";
	private static final String MSG_PRODUCT_CLASS_SAVE_FAILED = "(E%d) Save failed for product class %s in mission %s (cause: %s)";
	private static final String MSG_INVALID_PROCESSING_MODE = "(E%d) Processing mode %s not defined for mission %s";
	private static final String MSG_INVALID_ENCLOSING_CLASS = "(E%d) Enclosing product class %s is not defined for mission %s";
	private static final String MSG_INVALID_COMPONENT_CLASS = "(E%d) Component product class %s is not defined for mission %s";
	private static final String MSG_INVALID_PROCESSOR_CLASS = "(E%d) Processor class %s is not defined";
	private static final String MSG_INVALID_SOURCE_CLASS = "(E%d) Source product class %s is not defined for mission %s";
	private static final String MSG_INVALID_PARAMETER_KEY = "(E%d) Parameter key missing in filter condition %s";
	private static final String MSG_INVALID_PARAMETER_TYPE = "(E%d) Invalid parameter type %s in filter condition, one of {STRING, INTEGER, BOOLEAN, DOUBLE} expected";
	private static final String MSG_INVALID_PROCESSOR = "(E%d) Configured processor %s is not defined";
	private static final String MSG_INVALID_POLICY_TYPE = "(E%d) Invalid policy type %s in selection rule, see Generic IPF Interface Specifications for valid values";
	private static final String MSG_INVALID_TIME_UNIT = "(E%d) Invalid time unit %s in selection rule, one of {DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS} expected";
	private static final String MSG_PRODUCT_CLASS_CREATED = "(I%d) Product class of type %s created for mission %s";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-productclass-mgr ";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductClassControllerImpl.class);

	/**
	 * Log an error and return the corresponding HTTP message header
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return an HttpHeaders object with a formatted error message
	 */
	private HttpHeaders errorHeaders(String messageFormat, int messageId, Object... messageParameters) {
		String message = String.format(MSG_PREFIX + messageFormat, messageId, messageParameters);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return responseHeaders;
	}
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
		
		return new ResponseEntity<>(
				errorHeaders("GET for RestProductClass not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
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
			return new ResponseEntity<>(errorHeaders(MSG_PRODUCT_CLASS_MISSING, MSG_ID_PRODUCT_CLASS_MISSING), HttpStatus.BAD_REQUEST);
		}
		
		// Create product class object
		ProductClass modelProductClass = ProductClassUtil.toModelProductClass(productClass);
		Mission mission = RepositoryService.getMissionRepository().findByCode(productClass.getMissionCode());
		if (null == modelProductClass.getMission()) {
			return new ResponseEntity<>(
					errorHeaders(MSG_INVALID_MISSION_CODE, MSG_ID_INVALID_MISSION_CODE, productClass.getMissionCode()), HttpStatus.BAD_REQUEST);
		}
		modelProductClass.setMission(mission);
		
		// Try to save the product class, make sure product class does not yet exist
		try {
			modelProductClass = RepositoryService.getProductClassRepository().save(modelProductClass);
		} catch (DuplicateKeyException e) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PRODUCT_CLASS_EXISTS, MSG_ID_PRODUCT_CLASS_EXISTS, productClass.getProductType(), productClass.getMissionCode()), HttpStatus.BAD_REQUEST);
		} catch (DataAccessException e) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PRODUCT_CLASS_SAVE_FAILED, MSG_ID_PRODUCT_CLASS_SAVE_FAILED, 
							productClass.getProductType(), productClass.getMissionCode(), e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		// Add further attributes to product class
		if (null != productClass.getEnclosingClass()) {
			modelProductClass.setEnclosingClass(RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(mission.getCode(), productClass.getEnclosingClass()));
			if (null == modelProductClass.getEnclosingClass()) {
				return new ResponseEntity<>(
						errorHeaders(MSG_INVALID_ENCLOSING_CLASS, MSG_ID_INVALID_ENCLOSING_CLASS, productClass.getEnclosingClass(), mission.getCode()), HttpStatus.BAD_REQUEST);
			}
		}
		
		for (String componentClass: productClass.getComponentClasses()) {
			ProductClass modelComponentClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(mission.getCode(), componentClass);
			if (null == modelProductClass.getEnclosingClass()) {
				return new ResponseEntity<>(
						errorHeaders(MSG_INVALID_COMPONENT_CLASS, MSG_ID_INVALID_COMPONENT_CLASS, componentClass, mission.getCode()), HttpStatus.BAD_REQUEST);
			}
			modelProductClass.getComponentClasses().add(modelComponentClass);
		}
		
		if (null != productClass.getProcessorClass()) {
			modelProductClass.setProcessorClass(RepositoryService.getProcessorClassRepository().findByProcessorName(productClass.getProcessorClass()));
			if (null == modelProductClass.getEnclosingClass()) {
				return new ResponseEntity<>(
						errorHeaders(MSG_INVALID_PROCESSOR_CLASS, MSG_ID_INVALID_PROCESSOR_CLASS, productClass.getProcessorClass()), HttpStatus.BAD_REQUEST);
			}
		}
		
		for (de.dlr.proseo.prodclmgr.rest.model.SimpleSelectionRule rule: productClass.getSelectionRule()) {
			SimpleSelectionRule modelRule = new SimpleSelectionRule();
			if (mission.getProcessingModes().contains(rule.getMode())) {
				modelRule.setMode(rule.getMode());
			} else {
				return new ResponseEntity<>(
						errorHeaders(MSG_INVALID_PROCESSING_MODE, MSG_ID_INVALID_PROCESSING_MODE, rule.getMode(), mission.getCode()), HttpStatus.BAD_REQUEST);
			}
			modelRule.setIsMandatory(rule.getIsMandatory());
			modelRule.setTargetProductClass(modelProductClass);
			modelRule.setSourceProductClass(RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(mission.getCode(), rule.getSourceProductClass()));
			if (null == modelRule.getSourceProductClass()) {
				return new ResponseEntity<>(
						errorHeaders(MSG_INVALID_SOURCE_CLASS, MSG_ID_INVALID_SOURCE_CLASS, rule.getSourceProductClass(), mission.getCode()), HttpStatus.BAD_REQUEST);
			}

			for (de.dlr.proseo.prodclmgr.rest.model.Parameter filterCondition: rule.getFilterConditions()) {
				if (null == filterCondition.getKey()) {
					return new ResponseEntity<>(
							errorHeaders(MSG_INVALID_PARAMETER_KEY, MSG_ID_INVALID_PARAMETER_KEY, filterCondition.toString()), HttpStatus.BAD_REQUEST);
				}
				try {
					Parameter modelParameter = new Parameter().init(ParameterType.valueOf(filterCondition.getParameterType()), filterCondition.getParameterValue());
					modelRule.getFilterConditions().put(filterCondition.getKey(), modelParameter);
				} catch (IllegalArgumentException e) {
					return new ResponseEntity<>(
							errorHeaders(MSG_INVALID_PARAMETER_TYPE, MSG_ID_INVALID_PARAMETER_TYPE, filterCondition.getParameterType()), HttpStatus.BAD_REQUEST);
				}
			}
			
			for (String configuredProcessor: rule.getApplicableConfiguredProcessors()) {
				ConfiguredProcessor modelProcessor = RepositoryService.getConfiguredProcessorRepository().findByIdentifier(configuredProcessor);
				if (null == modelProcessor) {
					return new ResponseEntity<>(
							errorHeaders(MSG_INVALID_PROCESSOR, MSG_ID_INVALID_PROCESSOR, configuredProcessor), HttpStatus.BAD_REQUEST);
				}
				modelRule.getApplicableConfiguredProcessors().add(modelProcessor);
			}
			
			for (de.dlr.proseo.prodclmgr.rest.model.SimplePolicy policy: rule.getSimplePolicies()) {
				SimplePolicy modelPolicy = new SimplePolicy();
				try {
					modelPolicy.setPolicyType(PolicyType.valueOf(policy.getPolicyType()));
				} catch (Exception e) {
					return new ResponseEntity<>(
							errorHeaders(MSG_INVALID_POLICY_TYPE, MSG_ID_INVALID_POLICY_TYPE, policy.getPolicyType()), HttpStatus.BAD_REQUEST);
				}
				try {
					modelPolicy.setDeltaTimeT0(new DeltaTime(policy.getDeltaTimeT0().getDuration(), TimeUnit.valueOf(policy.getDeltaTimeT0().getUnit())));
				} catch (IllegalArgumentException e) {
					return new ResponseEntity<>(
							errorHeaders(MSG_INVALID_TIME_UNIT, MSG_ID_INVALID_TIME_UNIT, policy.getDeltaTimeT0().getUnit()), HttpStatus.BAD_REQUEST);
				}
				try {
					modelPolicy.setDeltaTimeT1(new DeltaTime(policy.getDeltaTimeT1().getDuration(), TimeUnit.valueOf(policy.getDeltaTimeT1().getUnit())));
				} catch (IllegalArgumentException e) {
					return new ResponseEntity<>(
							errorHeaders(MSG_INVALID_TIME_UNIT, MSG_ID_INVALID_TIME_UNIT, policy.getDeltaTimeT1().getUnit()), HttpStatus.BAD_REQUEST);
				}
				modelRule.getSimplePolicies().add(modelPolicy);
			}
			
			modelProductClass.getRequiredSelectionRules().add(modelRule);
		}
		
		// Save the product class again with all sub-object references
		try {
			modelProductClass = RepositoryService.getProductClassRepository().save(modelProductClass);
		} catch (Exception e) {
			return new ResponseEntity<>(
					errorHeaders(MSG_PRODUCT_CLASS_SAVE_FAILED, MSG_ID_PRODUCT_CLASS_SAVE_FAILED, 
							productClass.getProductType(), productClass.getMissionCode(), e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		logger.info(String.format(MSG_PREFIX + MSG_PRODUCT_CLASS_CREATED, MSG_ID_PRODUCT_CLASS_CREATED, modelProductClass.getProductType(), mission.getCode()));
		
		return new ResponseEntity<>(ProductClassUtil.toRestProductClass(null), HttpStatus.CREATED);
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

		return new ResponseEntity<>(
				errorHeaders("GET for RestProductClass with id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
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
		
		return new ResponseEntity<>(
				errorHeaders("PATCH for RestProductClass with id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
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
		
		return new ResponseEntity<>(
				errorHeaders("DELETE for RestProductClass with id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
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
		
		return new ResponseEntity<>(
				errorHeaders("GET for SelectionRuleString with RestProductClass id and source product type not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
	}

    /**
     * Create a selection rule
     * 
     * @param id the database ID of the product class
     * @param selectionRuleString a Json representation of a selection rule in Rule Language
     * @return a Json object representing the simple selection rule created and HTTP status CREATED
     */
	@Override
	public ResponseEntity<de.dlr.proseo.prodclmgr.rest.model.SimpleSelectionRule> createSelectionRuleString(Long id, @Valid List<SelectionRuleString> selectionRuleString) {
		// TODO Auto-generated method stub
		
		return new ResponseEntity<>(
				errorHeaders("POST for SelectionRuleString with RestProductClass id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
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
		
		return new ResponseEntity<>(
				errorHeaders("GET for SelectionRuleString with RestProductClass id and SimpleSelectionRule id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
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
		
		return new ResponseEntity<>(
				errorHeaders("PATCH for SelectionRuleString with RestProductClass id and SimpleSelectionRule id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
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
		
		return new ResponseEntity<>(
				errorHeaders("DELETE for SelectionRuleString with RestProductClass id and SimpleSelectionRule id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
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
		
		return new ResponseEntity<>(
				errorHeaders("PUT for SelectionRuleString with RestProductClass id, SimpleSelectionRule id and configured processor name not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
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
		
		return new ResponseEntity<>(
				errorHeaders("DELETE for SelectionRuleString with RestProductClass id, SimpleSelectionRule id and configured processor name not implemented (%d)", MSG_ID_NOT_IMPLEMENTED), 
				HttpStatus.NOT_IMPLEMENTED);
	}

}
