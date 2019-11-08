/**
 * ProductClassManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.validation.Valid;
import javax.ws.rs.ServerErrorException;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
 * Service methods required to manage product classes and their selection rules
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
@Transactional
public class ProductClassManager {
	
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

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductClassManager.class);

	/**
	 * Create and log a formatted informational message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private String logInfo(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);
		
		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.info(message);
		
		return message;
	}
	
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
     * Get product classes by mission, product type or mission type
     * 
     * @param mission the mission code
     * @param productType the prosEO product type (if set, missionType should not be set)
     * @param missionType the mission-defined product type (if set, productType should not be set)
     * @return a list of product classes conforming to the search criteria
	 * @throws NoResultException if no product classes matching the given search criteria could be found
     */
	public List<RestProductClass> getRestProductClass(String mission, String productType, String missionType) throws NoResultException {
		// TODO Auto-generated method stub
		
		throw new UnsupportedOperationException(logError("GET for RestProductClass not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

    /**
     * Create a new product class
     * 
     * @param productClass a Json object describing the new product class
     * @return a Json object describing the product class created (including ID and version) and HTTP status CREATED
 	 * @throws IllegalArgumentException if any of the input data was invalid
 	 * @throws ServerErrorException if saving the product class to the database fails
    */
	public RestProductClass createRestProductClass(RestProductClass productClass) throws
			IllegalArgumentException, ServerErrorException {
		if (logger.isTraceEnabled()) logger.trace(">>> createRestProductClass({})", (null == productClass ? "MISSING" : productClass.getProductType()));
		
		if (null == productClass) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_MISSING, MSG_ID_PRODUCT_CLASS_MISSING));
		}
		
		// Create product class object
		ProductClass modelProductClass = ProductClassUtil.toModelProductClass(productClass);
		Mission mission = RepositoryService.getMissionRepository().findByCode(productClass.getMissionCode());
		if (null == mission) {
			throw new IllegalArgumentException(logError(MSG_INVALID_MISSION_CODE, MSG_ID_INVALID_MISSION_CODE, productClass.getMissionCode()));
		}
		modelProductClass.setMission(mission);
		
		// Try to save the product class, make sure product class does not yet exist
		try {
			modelProductClass = RepositoryService.getProductClassRepository().save(modelProductClass);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_EXISTS, MSG_ID_PRODUCT_CLASS_EXISTS, productClass.getProductType(), productClass.getMissionCode()));
		} catch (DataAccessException e) {
			throw new ServerErrorException(logError(MSG_PRODUCT_CLASS_SAVE_FAILED, MSG_ID_PRODUCT_CLASS_SAVE_FAILED, 
							productClass.getProductType(), productClass.getMissionCode(), e.getClass().toString() + ": " + e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		
		// Add further attributes to product class
		if (null != productClass.getEnclosingClass()) {
			modelProductClass.setEnclosingClass(RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(mission.getCode(), productClass.getEnclosingClass()));
			if (null == modelProductClass.getEnclosingClass()) {
				throw new IllegalArgumentException(logError(MSG_INVALID_ENCLOSING_CLASS, MSG_ID_INVALID_ENCLOSING_CLASS,
						productClass.getEnclosingClass(), mission.getCode()));
			}
		}
		
		for (String componentClass: productClass.getComponentClasses()) {
			ProductClass modelComponentClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(mission.getCode(), componentClass);
			if (null == modelProductClass.getEnclosingClass()) {
				throw new IllegalArgumentException(logError(MSG_INVALID_COMPONENT_CLASS, MSG_ID_INVALID_COMPONENT_CLASS,
						componentClass, mission.getCode()));
			}
			modelProductClass.getComponentClasses().add(modelComponentClass);
		}
		
		if (null != productClass.getProcessorClass()) {
			modelProductClass.setProcessorClass(RepositoryService.getProcessorClassRepository()
					.findByMissionCodeAndProcessorName(mission.getCode(), productClass.getProcessorClass()));
			if (null == modelProductClass.getProcessorClass()) {
				throw new IllegalArgumentException(logError(MSG_INVALID_PROCESSOR_CLASS, MSG_ID_INVALID_PROCESSOR_CLASS,
						productClass.getProcessorClass(), mission.getCode()));
			}
		}
		
		for (RestSimpleSelectionRule rule: productClass.getSelectionRule()) {
			SimpleSelectionRule modelRule = new SimpleSelectionRule();
			if (mission.getProcessingModes().contains(rule.getMode())) {
				modelRule.setMode(rule.getMode());
			} else {
				throw new IllegalArgumentException(logError(MSG_INVALID_PROCESSING_MODE, MSG_ID_INVALID_PROCESSING_MODE,
						rule.getMode(), mission.getCode()));
			}
			modelRule.setIsMandatory(rule.getIsMandatory());
			modelRule.setTargetProductClass(modelProductClass);
			modelRule.setSourceProductClass(RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(mission.getCode(), rule.getSourceProductClass()));
			if (null == modelRule.getSourceProductClass()) {
				throw new IllegalArgumentException(logError(MSG_INVALID_SOURCE_CLASS, MSG_ID_INVALID_SOURCE_CLASS,
						rule.getSourceProductClass(), mission.getCode()));
			}

			for (RestParameter filterCondition: rule.getFilterConditions()) {
				if (null == filterCondition.getKey()) {
					throw new IllegalArgumentException(logError(MSG_INVALID_PARAMETER_KEY, MSG_ID_INVALID_PARAMETER_KEY, filterCondition.toString()));
				}
				try {
					Parameter modelParameter = new Parameter().init(ParameterType.valueOf(filterCondition.getParameterType()), filterCondition.getParameterValue());
					modelRule.getFilterConditions().put(filterCondition.getKey(), modelParameter);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException(logError(MSG_INVALID_PARAMETER_TYPE, MSG_ID_INVALID_PARAMETER_TYPE, filterCondition.getParameterType()));
				}
			}
			
			for (String configuredProcessor: rule.getApplicableConfiguredProcessors()) {
				ConfiguredProcessor modelProcessor = RepositoryService.getConfiguredProcessorRepository().findByIdentifier(configuredProcessor);
				if (null == modelProcessor) {
					throw new IllegalArgumentException(logError(MSG_INVALID_PROCESSOR, MSG_ID_INVALID_PROCESSOR, configuredProcessor));
				}
				modelRule.getApplicableConfiguredProcessors().add(modelProcessor);
			}
			
			for (RestSimplePolicy policy: rule.getSimplePolicies()) {
				SimplePolicy modelPolicy = new SimplePolicy();
				try {
					modelPolicy.setPolicyType(PolicyType.valueOf(policy.getPolicyType()));
				} catch (Exception e) {
					throw new IllegalArgumentException(logError(MSG_INVALID_POLICY_TYPE, MSG_ID_INVALID_POLICY_TYPE, policy.getPolicyType()));
				}
				try {
					modelPolicy.setDeltaTimeT0(new DeltaTime(policy.getDeltaTimeT0().getDuration(), TimeUnit.valueOf(policy.getDeltaTimeT0().getUnit())));
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException(logError(MSG_INVALID_TIME_UNIT, MSG_ID_INVALID_TIME_UNIT, policy.getDeltaTimeT0().getUnit()));
				}
				try {
					modelPolicy.setDeltaTimeT1(new DeltaTime(policy.getDeltaTimeT1().getDuration(), TimeUnit.valueOf(policy.getDeltaTimeT1().getUnit())));
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException(logError(MSG_INVALID_TIME_UNIT, MSG_ID_INVALID_TIME_UNIT, policy.getDeltaTimeT1().getUnit()));
				}
				modelRule.getSimplePolicies().add(modelPolicy);
			}
			
			modelProductClass.getRequiredSelectionRules().add(modelRule);
		}
		
		// Save the product class again with all sub-object references
		try {
			modelProductClass = RepositoryService.getProductClassRepository().save(modelProductClass);
		} catch (Exception e) {
			throw new ServerErrorException(logError(MSG_PRODUCT_CLASS_SAVE_FAILED, MSG_ID_PRODUCT_CLASS_SAVE_FAILED, 
							productClass.getProductType(), productClass.getMissionCode(), e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		
		logInfo(MSG_PRODUCT_CLASS_CREATED, MSG_ID_PRODUCT_CLASS_CREATED, modelProductClass.getProductType(), mission.getCode());
		
		return ProductClassUtil.toRestProductClass(modelProductClass);
	}

    /**
     * Get a product class by ID
     * 
     * @param id the database ID of the product class
     * @return a Json object describing the product class and HTTP status OK
	 * @throws IllegalArgumentException if no product class ID was given
	 * @throws NoResultException if no product class with the given ID exists
     */
	public RestProductClass getRestProductClassById(Long id) throws IllegalArgumentException, NoResultException {
		// TODO Auto-generated method stub

		throw new UnsupportedOperationException(logError("GET for RestProductClass with id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

    /**
     * Update a product class by ID
     * 
     * @param id the database ID of the product class to update
     * @param productClass a Json object describing the product class to modify
     * @return a Json object describing the product class modified (including incremented version)
	 * @throws EntityNotFoundException if no product class with the given ID exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws ConcurrentModificationException if the product class has been modified since retrieval by the client
     */
	public RestProductClass modifyRestProductClass(Long id, RestProductClass productClass) throws
			EntityNotFoundException, IllegalArgumentException, ConcurrentModificationException {
		// TODO Auto-generated method stub
		
		throw new UnsupportedOperationException(logError("PATCH for RestProductClass with id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

    /**
     * Delete a product class by ID
     * 
     * @param id the database ID of the product class to delete
	 * @throws EntityNotFoundException if the processor to delete does not exist in the database
	 * @throws RuntimeException if the deletion was not performed as expected
     */
	public void deleteProductclassById(Long id) throws EntityNotFoundException, RuntimeException {
		// TODO Auto-generated method stub
		
		throw new UnsupportedOperationException(logError("DELETE for RestProductClass with id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

    /**
     * Get the simple selection rules as formatted string, optionally selected by source class
     * 
     * @param id the database ID of the product class to get the selection rule from
     * @param sourceclass the prosEO product type of the source class, from which the product class can be generated (may be null)
     * @return a list of strings describing the selection rules for all configured processors
	 * @throws NoResultException if no simple selection rules matching the given search criteria could be found
     */
    public List<SelectionRuleString> getSelectionRuleStrings(Long id, String sourceClass) throws NoResultException {
		// TODO Auto-generated method stub
		
		throw new UnsupportedOperationException(logError("GET for SelectionRuleString with RestProductClass id and source product type not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

    /**
     * Create a selection rule using Rule Language
     * 
     * @param id the database ID of the product class
     * @param selectionRuleString a Json representation of a selection rule in Rule Language
     * @return a Json object representing the simple selection rule created and HTTP status CREATED
	 * @throws IllegalArgumentException if any of the input data was invalid
     */
	public RestProductClass createSelectionRuleString(Long id, @Valid List<SelectionRuleString> selectionRuleStrings) throws
			IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> createSelectionRuleString(SelectionRuleString[{}])", (null == selectionRuleStrings ? "MISSING" : selectionRuleStrings.size()));

		// Retrieve product class
		Optional<ProductClass> optProductClass = RepositoryService.getProductClassRepository().findById(id);
		if (optProductClass.isEmpty()) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_NOT_FOUND, MSG_ID_PRODUCT_CLASS_NOT_FOUND, id));
		}
		ProductClass productClass = optProductClass.get();
		
		// Process all selection rules
		for (SelectionRuleString restRuleString: selectionRuleStrings) {
			// Check the selection rule parameters
			String processingMode = restRuleString.getMode();
			if (null == processingMode || "".equals(processingMode)) {
				throw new IllegalArgumentException(logError(MSG_PROCESSING_MODE_MISSING, MSG_ID_PROCESSING_MODE_MISSING, restRuleString.toString()));
			}
			if (!productClass.getMission().getProcessingModes().contains(processingMode)) {
				throw new IllegalArgumentException(logError(MSG_INVALID_PROCESSING_MODE, MSG_ID_INVALID_PROCESSING_MODE,
						processingMode, productClass.getMission().getCode()));
			}
			
			Set<ConfiguredProcessor> configuredProcessors = new HashSet<>();
			for (String configuredProcessorIdentifier: restRuleString.getConfiguredProcessors()) {
				ConfiguredProcessor configuredProcessor = RepositoryService.getConfiguredProcessorRepository().findByIdentifier(configuredProcessorIdentifier);
				if (null == configuredProcessor) {
					throw new IllegalArgumentException(logError(MSG_INVALID_PROCESSOR, MSG_ID_INVALID_PROCESSOR, configuredProcessorIdentifier));
				}
				configuredProcessors.add(configuredProcessor);
			}
			
			// Parse the selection rule string
			SelectionRule selectionRule = null;
			try {
				selectionRule = SelectionRule.parseSelectionRule(productClass, restRuleString.getSelectionRule());
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(logError(MSG_RULE_STRING_MISSING, MSG_ID_RULE_STRING_MISSING, restRuleString));
			} catch (ParseException e) {
				throw new IllegalArgumentException(logError(MSG_INVALID_RULE_STRING, MSG_ID_INVALID_RULE_STRING,
						restRuleString.getSelectionRule(), e.getMessage()));
			}
			
			// Complete the simple selection rules and add them to the product class
			for (SimpleSelectionRule simpleSelectionRule: selectionRule.getSimpleRules()) {
				simpleSelectionRule.setMode(processingMode);
				simpleSelectionRule.getApplicableConfiguredProcessors().addAll(configuredProcessors);
				productClass.getRequiredSelectionRules().add(simpleSelectionRule);
			}
		}
		
		// Save the new selection rules in the product class
		productClass = RepositoryService.getProductClassRepository().save(productClass);

		// Return the modified product class
		logInfo(MSG_SELECTION_RULES_CREATED, MSG_ID_SELECTION_RULES_CREATED, selectionRuleStrings.size(), productClass.getProductType(), productClass.getMission().getCode());
		
		return ProductClassUtil.toRestProductClass(productClass);
	}

    /**
     * Get a selection rule by ID
     * 
     * @param ruleid the database ID of the simple selection rule to read
     * @param id the database ID of the product class
     * @return a Json object representing the simple selection rule in Rule Language
	 * @throws NoResultException if no selection rule or product class with the given ID exist
     */
	public SelectionRuleString getSelectionRuleString(Long ruleid, Long id) {
		// TODO Auto-generated method stub
		
		throw new UnsupportedOperationException(logError("GET for SelectionRuleString with RestProductClass id and SimpleSelectionRule id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

    /**
     * Update a selection rule using Rule Language
     * 
     * @param ruleid the database ID of the simple selection rule to update
     * @param id the database ID of the product class
     * @param selectionRuleString a Json object representing the simple selection rule in Rule Language
     * @return a Json object representing the modified simple selection rule in Rule Language
	 * @throws EntityNotFoundException if no selection rule or product class with the given ID exist
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws ConcurrentModificationException if the selection rule has been modified since retrieval by the client
     */
	public SelectionRuleString modifySelectionRuleString(Long ruleid, Long id,
			SelectionRuleString selectionRuleString) throws
			EntityNotFoundException, IllegalArgumentException, ConcurrentModificationException{
		// TODO Auto-generated method stub
		
		throw new UnsupportedOperationException(logError("PATCH for SelectionRuleString with RestProductClass id and SimpleSelectionRule id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

    /**
     * Delete a selection rule
     * 
     * @param ruleid the database ID of the simple selection rule to delete
     * @param id the database ID of the product class
	 * @throws EntityNotFoundException if the selection rule to delete does not exist in the database
	 * @throws RuntimeException if the deletion was not performed as expected
     */
	public void deleteSelectionrule(Long ruleid, Long id) throws EntityNotFoundException, RuntimeException {
		// TODO Auto-generated method stub
		
		throw new UnsupportedOperationException(logError("DELETE for SelectionRuleString with RestProductClass id and SimpleSelectionRule id not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

    /**
     * Add the configured processor to the selection rule
     * 
     * @param configuredProcessor the name of the configured processor to add to the selection rule
     * @param ruleid the database ID of the simple selection rule
     * @param id the database ID of the product class
	 * @throws EntityNotFoundException if no configured processor with the given name or no selection rule or product class with the given ID exist
     */
	public void addProcessorToRule(String configuredProcessor, Long ruleid, Long id) throws EntityNotFoundException {
		// TODO Auto-generated method stub
		
		throw new UnsupportedOperationException(logError("PUT for SelectionRuleString with RestProductClass id, SimpleSelectionRule id and configured processor name not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

    /**
     * Remove the configured processor from the selection rule (the selection rule will be disconnected from the configured processor)
     * 
     * @param configuredProcessor the name of the configured processor to remove from the selection rule
     * @param ruleid the database ID of the simple selection rule
     * @param id the database ID of the product class
	 * @throws EntityNotFoundException if no configured processor with the given name or no selection rule or product class with the given ID exist
     */
	public void removeProcessorFromRule(String configuredProcessor, Long ruleid, Long id) throws EntityNotFoundException {
		// TODO Auto-generated method stub
		
		throw new UnsupportedOperationException(logError("DELETE for SelectionRuleString with RestProductClass id, SimpleSelectionRule id and configured processor name not implemented (%d)", MSG_ID_NOT_IMPLEMENTED));
	}

}
