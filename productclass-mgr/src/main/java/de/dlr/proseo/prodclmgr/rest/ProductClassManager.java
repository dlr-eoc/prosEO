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
import javax.persistence.Query;
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
import de.dlr.proseo.model.ProcessorClass;
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
	private static final int MSG_ID_PRODUCT_CLASS_NOT_FOUND_BY_TYPE = 2120;
	private static final int MSG_ID_PRODUCT_CLASS_NOT_FOUND_BY_SEARCH = 2121;
	private static final int MSG_ID_PRODUCT_CLASS_LIST_RETRIEVED = 2122;
	private static final int MSG_ID_PRODUCT_CLASS_ID_MISSING = 2123;
	private static final int MSG_ID_PRODUCT_CLASS_ID_NOT_FOUND = 2124;
	private static final int MSG_ID_PRODUCT_CLASS_RETRIEVED = 2125;
	private static final int MSG_ID_PRODUCT_CLASS_DATA_MISSING = 2126;
	private static final int MSG_ID_PRODUCT_CLASS_MODIFIED = 2127;
	private static final int MSG_ID_PRODUCT_CLASS_NOT_MODIFIED = 2128;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 2129;
	private static final int MSG_ID_PRODUCT_CLASS_DELETED = 2130;
	private static final int MSG_ID_DUPLICATE_RULE = 2131;
	private static final int MSG_ID_SELECTION_RULE_LIST_RETRIEVED = 2132;
	private static final int MSG_ID_SELECTION_RULE_ID_MISSING = 2133;
	private static final int MSG_ID_SELECTION_RULE_ID_NOT_FOUND = 2134;
	private static final int MSG_ID_SELECTION_RULE_RETRIEVED = 2135;
	private static final int MSG_ID_SELECTION_RULE_DATA_MISSING = 2136;
	private static final int MSG_ID_CONCURRENT_UPDATE = 2137;
	private static final int MSG_ID_CONCURRENT_RULE_UPDATE = 2138;
	private static final int MSG_ID_SELECTION_RULE_MODIFIED = 2139;
	private static final int MSG_ID_SELECTION_RULE_NOT_MODIFIED = 2140;
	private static final int MSG_ID_SELECTION_RULE_DELETED = 2141;
	private static final int MSG_ID_PROCESSOR_NAME_MISSING = 2142;
	private static final int MSG_ID_PROCESSOR_ADDED = 2143;
	private static final int MSG_ID_PROCESSOR_NOT_FOUND = 2144;
	private static final int MSG_ID_PROCESSOR_REMOVED = 2145;
//	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_PRODUCT_CLASS_MISSING = "(E%d) Product class not set";
	private static final String MSG_PRODUCT_CLASS_NOT_FOUND = "(E%d) Product class with ID %d not found";
	private static final String MSG_PRODUCT_CLASS_NOT_FOUND_BY_TYPE = "(E%d) Product class %s not found for mission %s";
	private static final String MSG_PRODUCT_CLASS_NOT_FOUND_BY_SEARCH = "(E%d) No product classes found for mission %s and product type %s";
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
	private static final String MSG_PRODUCT_CLASS_ID_MISSING = "(E%d) Product class ID not set";
	private static final String MSG_PRODUCT_CLASS_ID_NOT_FOUND = "(E%d) No product class found with ID %d";
	private static final String MSG_PRODUCT_CLASS_DATA_MISSING = "(E%d) Product class data not set";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Product class deletion unsuccessful for ID %d";
	private static final String MSG_SELECTION_RULE_ID_MISSING = "(E%d) Selection rule ID not set";
	private static final String MSG_SELECTION_RULE_ID_NOT_FOUND = "(E%d) Selection rule with ID %d not found for product class with ID %d";
	private static final String MSG_SELECTION_RULE_DATA_MISSING = "(E%d) Selection rule data not set";
	private static final Object MSG_EXACTLY_ONE_SELECTION_RULE_EXPECTED = "Exactly one simple selection rule expected"; // Sub-message for MSG_INVALID_RULE_STRING
	private static final String MSG_CONCURRENT_UPDATE = "(E%d) The product class with ID %d has been modified since retrieval by the client";
	private static final String MSG_CONCURRENT_RULE_UPDATE = "(E%d) The selection rule with ID %d has been modified since retrieval by the client";
	private static final String MSG_PROCESSOR_NAME_MISSING = "(E%d) Name of configured processor not set";
	private static final String MSG_PROCESSOR_NOT_FOUND = "(E%d) Configured processor %s not found in selection rule with ID %d for product class with ID %d";
	private static final String MSG_DUPLICATE_RULE = "(E%d) Product class %s already contains selection rule for source class %s, mode %s and configured processor %s";

	private static final String MSG_PRODUCT_CLASS_LIST_RETRIEVED = "(I%d) Product class(es) for mission %s and product type %s retrieved";
	private static final String MSG_PRODUCT_CLASS_CREATED = "(I%d) Product class of type %s created for mission %s";
	private static final String MSG_SELECTION_RULES_CREATED = "(I%d) %d selection rules added to product class of type %s in mission %s";
	private static final String MSG_PRODUCT_CLASS_RETRIEVED = "(I%d) Product class with ID %d retrieved";
	private static final String MSG_PRODUCT_CLASS_MODIFIED = "(I%d) Product class with ID %d modified";
	private static final String MSG_PRODUCT_CLASS_NOT_MODIFIED = "(I%d) Product class with ID %d not modified (no changes)";
	private static final String MSG_PRODUCT_CLASS_DELETED = "(I%d) Product class with ID %d deleted";
	private static final String MSG_SELECTION_RULE_LIST_RETRIEVED = "(I%d) Selection rules for target product type %s and source product type %s retrieved";
	private static final String MSG_SELECTION_RULE_RETRIEVED = "(I%d) Selection rule with ID %d for product class with ID %d retrieved";
	private static final String MSG_SELECTION_RULE_MODIFIED = "(I%d) Selection rule with ID %d modified";
	private static final String MSG_SELECTION_RULE_NOT_MODIFIED = "(I%d) Selection rule with ID %d not modified (no changes)";
	private static final String MSG_SELECTION_RULE_DELETED = "(I%d) Selection rule with ID %d for product class with ID %d deleted";
	private static final String MSG_PROCESSOR_ADDED = "(I%d) Configured processor %s added to selection rule with ID %d for product class with ID %d";
	private static final String MSG_PROCESSOR_REMOVED = "(I%d) Configured processor %s removed from selection rule with ID %d for product class with ID %d";

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
	 * Remove the enclosing product class from the given product class (and the product class from the enclosing class)
	 * 
	 * @param productClass the product class to remove the enclosing class from (must not be transient)
	 */
	private void removeEnclosingClass(ProductClass productClass) {
		ProductClass enclosingClass = productClass.getEnclosingClass();
		enclosingClass.getComponentClasses().remove(productClass);
		RepositoryService.getProductClassRepository().save(enclosingClass);
		productClass.setEnclosingClass(null);
	}

	/**
	 * Set the enclosing class of the given product class to the class with the given mission code and product type
	 * 
	 * @param productClass the product class to update (must not be transient)
	 * @param missionCode the mission code of the enclosing product class
	 * @param productType the product type of the enclosing product class
	 * @throws IllegalArgumentException if a product class with the given mission code and product type does not exist
	 */
	private void setEnclosingClass(ProductClass productClass, String missionCode, String productType) throws IllegalArgumentException {
		ProductClass enclosingClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(
				missionCode, productType);
		if (null == enclosingClass) {
			throw new IllegalArgumentException(logError(MSG_INVALID_ENCLOSING_CLASS, MSG_ID_INVALID_ENCLOSING_CLASS,
					productType, missionCode));
		}
		enclosingClass.getComponentClasses().add(productClass);
		enclosingClass = RepositoryService.getProductClassRepository().save(enclosingClass);
		productClass.setEnclosingClass(enclosingClass);
	}

	/**
	 * Remove the processor class from the given product class (and the product class from the processor class)
	 * 
	 * @param productClass
	 */
	private void removeProcessorClass(ProductClass productClass) {
		ProcessorClass processorClass = productClass.getProcessorClass();
		processorClass.getProductClasses().remove(productClass);
		RepositoryService.getProcessorClassRepository().save(processorClass);
		productClass.setProcessorClass(null);
	}

	/**
	 * Set the processor class, which can generate products of the given product class
	 * 
	 * @param modelProductClass the product class to update (must not be transient)
	 * @param missionCode the mission code of the processor class
	 * @param processorName the processor (class) name
	 * @throws IllegalArgumentException if a processor class with the given mission code and processor name does not exist
	 */
	private void setProcessorClass(ProductClass modelProductClass, String missionCode, String processorName)
			throws IllegalArgumentException {
		ProcessorClass processorClass = RepositoryService.getProcessorClassRepository()
				.findByMissionCodeAndProcessorName(missionCode, processorName);
		if (null == processorClass) {
			throw new IllegalArgumentException(logError(MSG_INVALID_PROCESSOR_CLASS, MSG_ID_INVALID_PROCESSOR_CLASS,
					processorName, missionCode));
		}
		processorClass.getProductClasses().add(modelProductClass);
		processorClass = RepositoryService.getProcessorClassRepository().save(processorClass);
		modelProductClass.setProcessorClass(processorClass);
	}

    /**
     * Get product classes, optionally filtered by mission and/or product type
     * 
     * @param mission the mission code
     * @param productType the prosEO product type
     * @return a list of product classes conforming to the search criteria
	 * @throws NoResultException if no product classes matching the given search criteria could be found
     */
	public List<RestProductClass> getRestProductClass(String mission, String productType) throws NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestProductClass({}, {}, {})", mission, productType);
		
		List<RestProductClass> result = new ArrayList<>();
		
		if (null != mission && null != productType) {
			ProductClass productClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(mission, productType);
			if (null == productClass) {
				throw new NoResultException(logError(MSG_PRODUCT_CLASS_NOT_FOUND_BY_TYPE, MSG_ID_PRODUCT_CLASS_NOT_FOUND_BY_TYPE, 
						productType, mission));
			}
			result.add(ProductClassUtil.toRestProductClass(productClass));
		} else {
			String jpqlQuery = "select pc from ProductClass pc where 1 = 1";
			if (null != mission) {
				jpqlQuery += " and mission.code = :missionCode";
			}
			if (null != productType) {
				jpqlQuery += " and productType = :productType";
			}
			Query query = em.createQuery(jpqlQuery);
			if (null != mission) {
				query.setParameter("missionCode", mission);
			}
			if (null != productType) {
				query.setParameter("productType", productType);
			}
			for (Object resultObject: query.getResultList()) {
				if (resultObject instanceof de.dlr.proseo.model.ProductClass) {
					result.add(ProductClassUtil.toRestProductClass((de.dlr.proseo.model.ProductClass) resultObject));
				}
			}
			if (result.isEmpty()) {
				throw new NoResultException(logError(MSG_PRODUCT_CLASS_NOT_FOUND_BY_SEARCH, MSG_ID_PRODUCT_CLASS_NOT_FOUND_BY_SEARCH, 
						mission, productType));
			}
		}
		logInfo(MSG_PRODUCT_CLASS_LIST_RETRIEVED, MSG_ID_PRODUCT_CLASS_LIST_RETRIEVED, mission, productType);
		
		return result;
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
			setEnclosingClass(modelProductClass, mission.getCode(), productClass.getEnclosingClass());
		}
		
		for (String componentClass: productClass.getComponentClasses()) {
			ProductClass modelComponentClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(mission.getCode(), componentClass);
			if (null == modelProductClass.getEnclosingClass()) {
				throw new IllegalArgumentException(logError(MSG_INVALID_COMPONENT_CLASS, MSG_ID_INVALID_COMPONENT_CLASS,
						componentClass, mission.getCode()));
			}
			modelComponentClass.setEnclosingClass(modelProductClass);
			modelComponentClass = RepositoryService.getProductClassRepository().save(modelComponentClass);
			modelProductClass.getComponentClasses().add(modelComponentClass);
		}
		
		if (null != productClass.getProcessorClass()) {
			setProcessorClass(modelProductClass, mission.getCode(), productClass.getProcessorClass());
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
		if (logger.isTraceEnabled()) logger.trace(">>> getRestProductClassById({})", id);
		
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_ID_MISSING, MSG_ID_PRODUCT_CLASS_ID_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new NoResultException(logError(MSG_PRODUCT_CLASS_ID_NOT_FOUND, MSG_ID_PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		logInfo(MSG_PRODUCT_CLASS_RETRIEVED, MSG_ID_PRODUCT_CLASS_RETRIEVED, id);

		return ProductClassUtil.toRestProductClass(modelProductClass.get());
	}

    /**
     * Update a product class by ID (does not update its selection rules)
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
		if (logger.isTraceEnabled()) logger.trace(">>> modifyRestProductClass({}, {})", id, (null == productClass ? "MISSING" : productClass.getProductType()));
		
		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_ID_MISSING, MSG_ID_PRODUCT_CLASS_ID_MISSING));
		}
		if (null == productClass) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_DATA_MISSING, MSG_ID_PRODUCT_CLASS_DATA_MISSING));
		}
		
		Optional<ProductClass> optProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (optProductClass.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_PRODUCT_CLASS_ID_NOT_FOUND, MSG_ID_PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		ProductClass modelProductClass = optProductClass.get();
		
		// Make sure we are allowed to change the product class(no intermediate update)
		if (modelProductClass.getVersion() != productClass.getVersion().intValue()) {
			throw new ConcurrentModificationException(logError(MSG_CONCURRENT_UPDATE, MSG_ID_CONCURRENT_UPDATE, id));
		}
		
		// Apply changed attributes
		ProductClass changedProductClass = ProductClassUtil.toModelProductClass(productClass);
		
		boolean productClassChanged = false;
		if (!modelProductClass.getProductType().equals(changedProductClass.getProductType())) {
			productClassChanged = true;
			modelProductClass.setProductType(changedProductClass.getProductType());
		}
		if (null == modelProductClass.getDescription() && null != changedProductClass.getDescription()
				|| null != modelProductClass.getDescription() && !modelProductClass.getDescription().equals(changedProductClass.getDescription())) {
			productClassChanged = true;
			modelProductClass.setDescription(changedProductClass.getDescription());
		}
		if (null == modelProductClass.getVisibility() && null != changedProductClass.getVisibility()
				|| null != modelProductClass.getVisibility() && !modelProductClass.getVisibility().equals(changedProductClass.getVisibility())) {
			productClassChanged = true;
			modelProductClass.setVisibility(changedProductClass.getVisibility());
		}
		if (null == modelProductClass.getDefaultSlicingType() && null != changedProductClass.getDefaultSlicingType()
				|| null != modelProductClass.getDefaultSlicingType() && !modelProductClass.getDefaultSlicingType().equals(changedProductClass.getDefaultSlicingType())) {
			productClassChanged = true;
			modelProductClass.setDefaultSlicingType(changedProductClass.getDefaultSlicingType());
		}
		if (null == modelProductClass.getDefaultSliceDuration() && null != changedProductClass.getDefaultSliceDuration()
				|| null != modelProductClass.getDefaultSliceDuration() && !modelProductClass.getDefaultSliceDuration().equals(changedProductClass.getDefaultSliceDuration())) {
			productClassChanged = true;
			modelProductClass.setDefaultSliceDuration(changedProductClass.getDefaultSliceDuration());
		}
		
		// Check the processor class
		if (null == productClass.getProcessorClass() || 0 == productClass.getProcessorClass().length()) {
			if (null != modelProductClass.getProcessorClass()) {
				// Associated processor class removed
				productClassChanged = true;
				removeProcessorClass(modelProductClass);
			}
		} else if (null == modelProductClass.getProcessorClass()) {
			// New associated processor class added
			productClassChanged = true;
			setProcessorClass(modelProductClass, productClass.getMissionCode(), productClass.getProcessorClass());
		} else if (!modelProductClass.getProcessorClass().getProcessorName().equals(productClass.getProcessorClass())) {
			// Associated processor class changed - remove product class from old processor class
			productClassChanged = true;
			removeProcessorClass(modelProductClass);
			// Add new associated processor class
			setProcessorClass(modelProductClass, productClass.getMissionCode(), productClass.getProcessorClass());
		}
		
		// Check for new component product classes
		Set<ProductClass> newComponentClasses = new HashSet<>();
		if (null != productClass.getComponentClasses()) {
			COMPONENT_CLASSES:
			for (String changedComponentClass: productClass.getComponentClasses()) {
				for (ProductClass modelComponentClass: modelProductClass.getComponentClasses()) {
					if (modelComponentClass.getProductType().equals(changedComponentClass)) {
						// Already present
						newComponentClasses.add(modelComponentClass);
						continue COMPONENT_CLASSES;
					}
				}
				// New component class
				productClassChanged = true;
				ProductClass newComponentClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(productClass.getMissionCode(), changedComponentClass);
				if (null == newComponentClass) {
					throw new IllegalArgumentException(logError(MSG_INVALID_COMPONENT_CLASS, MSG_ID_INVALID_COMPONENT_CLASS,
							changedComponentClass, productClass.getMissionCode()));
				}
				newComponentClass.setEnclosingClass(modelProductClass);
				newComponentClass = RepositoryService.getProductClassRepository().save(newComponentClass);
				newComponentClasses.add(newComponentClass);
			}
		}
		// Check for removed component product classes
		for (ProductClass modelComponentClass: modelProductClass.getComponentClasses()) {
			if (!newComponentClasses.contains(modelComponentClass)) {
				// Component class removed
				productClassChanged = true;
				modelComponentClass.setEnclosingClass(null);
				RepositoryService.getProductClassRepository().save(modelComponentClass);
			}
		}
		
		// Check the enclosing class
		if (null == productClass.getEnclosingClass() || 0 == productClass.getEnclosingClass().length()) {
			if (null != modelProductClass.getEnclosingClass()) {
				// Enclosing class removed
				productClassChanged = true;
				removeEnclosingClass(modelProductClass);
			}
		} else if (null == modelProductClass.getEnclosingClass()) {
			// Enclosing class added
			productClassChanged = true;
			setEnclosingClass(modelProductClass, productClass.getMissionCode(), productClass.getEnclosingClass());
		} else if (!modelProductClass.getEnclosingClass().getProductType().equals(productClass.getEnclosingClass())) {
			// Enclosing class changed
			productClassChanged = true;
			// Remove the product class from the old enclosing class
			removeEnclosingClass(modelProductClass);
			// Add the product class to the new enclosing class
			setEnclosingClass(modelProductClass, productClass.getMissionCode(), productClass.getEnclosingClass());
		}
		
		// Save product class only if anything was actually changed
		if (productClassChanged) {
			modelProductClass.incrementVersion();
			modelProductClass.setComponentClasses(newComponentClasses);
			modelProductClass = RepositoryService.getProductClassRepository().save(modelProductClass);
			logInfo(MSG_PRODUCT_CLASS_MODIFIED, MSG_ID_PRODUCT_CLASS_MODIFIED, id);
		} else {
			logInfo(MSG_PRODUCT_CLASS_NOT_MODIFIED, MSG_ID_PRODUCT_CLASS_NOT_MODIFIED, id);
		}
		
		return ProductClassUtil.toRestProductClass(modelProductClass);
	}

    /**
     * Delete a product class by ID (with all its selection rules)
     * 
     * @param id the database ID of the product class to delete
     * @throws EntityNotFoundException if the processor to delete does not exist in the database
     * @throws RuntimeException if the deletion was not performed as expected
     * @throws IllegalArgumentException if the product class ID was not given
     */
	public void deleteProductclassById(Long id) throws EntityNotFoundException, RuntimeException, IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductclassById({})", id);
		
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_ID_MISSING, MSG_ID_PRODUCT_CLASS_ID_MISSING));
		}
		
		// Test whether the product id is valid
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		if (modelProductClass.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_PRODUCT_CLASS_NOT_FOUND, MSG_ID_PRODUCT_CLASS_NOT_FOUND, id));
		}
		
		// Delete the processor class
		RepositoryService.getProductClassRepository().deleteById(id);

		// Test whether the deletion was successful
		modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		if (!modelProductClass.isEmpty()) {
			throw new RuntimeException(logError(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, id));
		}
		
		logInfo(MSG_PRODUCT_CLASS_DELETED, MSG_ID_PRODUCT_CLASS_DELETED, id);
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
		if (logger.isTraceEnabled()) logger.trace(">>> getSelectionRuleStrings({}, {})", id, sourceClass);

		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_ID_MISSING, MSG_ID_PRODUCT_CLASS_ID_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new NoResultException(logError(MSG_PRODUCT_CLASS_ID_NOT_FOUND, MSG_ID_PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		// Find the correct simple selection rules
		List<SelectionRuleString> result = new ArrayList<>();
		for (SimpleSelectionRule modelRule: modelProductClass.get().getRequiredSelectionRules()) {
			// Add selection rule to result, if no source class was specified, or if the source class matches
			if (null == sourceClass || 0 == sourceClass.length() || modelRule.getSourceProductClass().getProductType().equals(sourceClass)) {
				SelectionRuleString restRule = new SelectionRuleString();
				restRule.setId(modelRule.getId());
				restRule.setVersion(Long.valueOf(modelRule.getVersion()));
				restRule.setMode(modelRule.getMode());
				restRule.setSelectionRule(modelRule.toString());
				for (ConfiguredProcessor modelProcessor: modelRule.getApplicableConfiguredProcessors()) {
					restRule.getConfiguredProcessors().add(modelProcessor.getIdentifier());
				}
				result.add(restRule);
			}
		}
		
		logInfo(MSG_SELECTION_RULE_LIST_RETRIEVED, MSG_ID_SELECTION_RULE_LIST_RETRIEVED, modelProductClass.get().getProductType(), sourceClass);
		
		return result;
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
			if (!SimpleSelectionRule.PROCESSING_MODE_ALWAYS.equals(processingMode) 
					&& !productClass.getMission().getProcessingModes().contains(processingMode)) {
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
			
			// Complete the simple selection rules and add them to the product class, if no rule with equivalent key values exists
			for (SimpleSelectionRule simpleSelectionRule: selectionRule.getSimpleRules()) {
				// Set remaining attributes
				simpleSelectionRule.setMode(processingMode);
				simpleSelectionRule.getApplicableConfiguredProcessors().addAll(configuredProcessors);
				
				// Check for duplicates
				for (SimpleSelectionRule existingRule: productClass.getRequiredSelectionRules()) {
					if (existingRule.getSourceProductClass().equals(simpleSelectionRule.getSourceProductClass())
							|| existingRule.getMode().equals(simpleSelectionRule.getMode())) {
						// Duplicate candidate - check applicable configured processors
						if (existingRule.getApplicableConfiguredProcessors().isEmpty() || simpleSelectionRule.getApplicableConfiguredProcessors().isEmpty()) {
							// At least one of the rules is applicable for all configured processors, so this is a duplicate
							throw new IllegalArgumentException(logError(MSG_DUPLICATE_RULE, MSG_ID_DUPLICATE_RULE,
									productClass.getProductType(), existingRule.getSourceProductClass().getProductType(),
									existingRule.getMode(), "(all)"));
						}
						for (ConfiguredProcessor existingProcessor: existingRule.getApplicableConfiguredProcessors()) {
							if (simpleSelectionRule.getApplicableConfiguredProcessors().contains(existingProcessor)) {
								// Overlapping set of configured processors, so this is a duplicate
								throw new IllegalArgumentException(logError(MSG_DUPLICATE_RULE, MSG_ID_DUPLICATE_RULE,
										productClass.getProductType(), existingRule.getSourceProductClass().getProductType(),
										existingRule.getMode(), existingProcessor.getIdentifier()));
							}
						}
					}
				}
				
				// Add new selection rule to product class
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
	public SelectionRuleString getSelectionRuleString(Long ruleid, Long id) throws NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getSelectionRuleString({}, {})", ruleid, id);

		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_ID_MISSING, MSG_ID_PRODUCT_CLASS_ID_MISSING));
		}
		if (null == ruleid || 0 == ruleid) {
			throw new IllegalArgumentException(logError(MSG_SELECTION_RULE_ID_MISSING, MSG_ID_SELECTION_RULE_ID_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new NoResultException(logError(MSG_PRODUCT_CLASS_ID_NOT_FOUND, MSG_ID_PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		// Find requested simple selection rule
		for (SimpleSelectionRule modelRule: modelProductClass.get().getRequiredSelectionRules()) {
			if (modelRule.getId() == id.longValue()) {
				SelectionRuleString restRule = new SelectionRuleString();
				restRule.setId(modelRule.getId());
				restRule.setVersion(Long.valueOf(modelRule.getVersion()));
				restRule.setMode(modelRule.getMode());
				restRule.setSelectionRule(modelRule.toString());
				for (ConfiguredProcessor modelProcessor: modelRule.getApplicableConfiguredProcessors()) {
					restRule.getConfiguredProcessors().add(modelProcessor.getIdentifier());
				}
				logInfo(MSG_SELECTION_RULE_RETRIEVED, MSG_ID_SELECTION_RULE_RETRIEVED, ruleid, id);
				return restRule;
			}
		}
		
		// Selection rule not found
		throw new NoResultException(logError(MSG_SELECTION_RULE_ID_NOT_FOUND, MSG_ID_SELECTION_RULE_ID_NOT_FOUND, ruleid, id));
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
		if (logger.isTraceEnabled()) logger.trace(">>> modifySelectionRuleString({}, {}, {})", ruleid, id, selectionRuleString);

		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_ID_MISSING, MSG_ID_PRODUCT_CLASS_ID_MISSING));
		}
		if (null == ruleid || 0 == ruleid) {
			throw new IllegalArgumentException(logError(MSG_SELECTION_RULE_ID_MISSING, MSG_ID_SELECTION_RULE_ID_MISSING));
		}
		if (null == selectionRuleString || null == selectionRuleString.getSelectionRule() || 0 == selectionRuleString.getSelectionRule().length()) {
			throw new IllegalArgumentException(logError(MSG_SELECTION_RULE_DATA_MISSING, MSG_ID_SELECTION_RULE_DATA_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new NoResultException(logError(MSG_PRODUCT_CLASS_ID_NOT_FOUND, MSG_ID_PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		// Find requested simple selection rule
		for (SimpleSelectionRule modelRule: modelProductClass.get().getRequiredSelectionRules()) {
			if (modelRule.getId() == ruleid.longValue()) {
				// Make sure we are allowed to change the selection rule (no intermediate update)
				if (modelRule.getVersion() != selectionRuleString.getVersion().intValue()) {
					throw new ConcurrentModificationException(logError(MSG_CONCURRENT_RULE_UPDATE, MSG_ID_CONCURRENT_RULE_UPDATE, ruleid));
				}
				
				// Parse the selection rule string
				SelectionRule changedRule = null;
				try {
					changedRule = SelectionRule.parseSelectionRule(modelProductClass.get(), selectionRuleString.getSelectionRule());
				} catch (ParseException e) {
					throw new IllegalArgumentException(logError(MSG_INVALID_RULE_STRING, MSG_ID_INVALID_RULE_STRING, selectionRuleString, e.getMessage()));
				}
				
				boolean ruleChanged = false;
				// Check whether the (normalized) selection rule string was changed
				List<SimpleSelectionRule> changedSimpleRules = changedRule.getSimpleRules();
				if (1 != changedSimpleRules.size()) {
					throw new IllegalArgumentException(logError(MSG_INVALID_RULE_STRING, MSG_ID_INVALID_RULE_STRING, selectionRuleString, MSG_EXACTLY_ONE_SELECTION_RULE_EXPECTED));
				}
				SimpleSelectionRule changedSimpleRule = changedSimpleRules.get(0);
				if (!modelRule.toString().equals(changedRule.toString())) {
					ruleChanged = true;
					modelRule.setFilterConditions(changedSimpleRule.getFilterConditions());
					modelRule.setFilteredSourceProductType(changedSimpleRule.getFilteredSourceProductType());
					modelRule.setIsMandatory(changedSimpleRule.getIsMandatory());
					modelRule.setSimplePolicies(changedSimpleRule.getSimplePolicies());
					modelRule.setSourceProductClass(changedSimpleRule.getSourceProductClass());
				}
				// Check mode change
				if (!modelRule.getMode().equals(selectionRuleString.getMode())) {
					ruleChanged = true;
					modelRule.setMode(selectionRuleString.getMode());
				}
				// Check for new configured processors
				Set<ConfiguredProcessor> newConfiguredProcessors = new HashSet<>();
				for (String changedProcessorName: selectionRuleString.getConfiguredProcessors()) {
					ConfiguredProcessor changedProcessor = RepositoryService.getConfiguredProcessorRepository().findByIdentifier(changedProcessorName);
					if (null == changedProcessor) {
						throw new IllegalArgumentException(logError(MSG_INVALID_PROCESSOR, MSG_ID_INVALID_PROCESSOR, changedProcessorName));
					}
					if (!modelRule.getApplicableConfiguredProcessors().contains(changedProcessor)) {
						ruleChanged = true;
					}
					newConfiguredProcessors.add(changedProcessor);
				}
				// Check for removed configured processors
				for (ConfiguredProcessor oldProcessor: modelRule.getApplicableConfiguredProcessors()) {
					if (!newConfiguredProcessors.contains(oldProcessor)) {
						ruleChanged = true;
					}
				}
				
				// Save simple selection rule only if anything was actually changed
				if (ruleChanged) {
					modelRule.incrementVersion();
					modelRule.setApplicableConfiguredProcessors(newConfiguredProcessors);
					RepositoryService.getProductClassRepository().save(modelProductClass.get());
				}
				
				SelectionRuleString restRule = new SelectionRuleString();
				restRule.setId(modelRule.getId());
				restRule.setVersion(Long.valueOf(modelRule.getVersion()));
				restRule.setMode(modelRule.getMode());
				restRule.setSelectionRule(modelRule.toString());
				for (ConfiguredProcessor modelProcessor: modelRule.getApplicableConfiguredProcessors()) {
					restRule.getConfiguredProcessors().add(modelProcessor.getIdentifier());
				}
				if (ruleChanged) {
					logInfo(MSG_SELECTION_RULE_MODIFIED, MSG_ID_SELECTION_RULE_MODIFIED, ruleid, id);
				} else {
					logInfo(MSG_SELECTION_RULE_NOT_MODIFIED, MSG_ID_SELECTION_RULE_NOT_MODIFIED, ruleid, id);
				}
				return restRule;
			}
		}
		
		// Selection rule not found
		throw new EntityNotFoundException(logError(MSG_SELECTION_RULE_ID_NOT_FOUND, MSG_ID_SELECTION_RULE_ID_NOT_FOUND, ruleid, id));
	}

    /**
     * Delete a selection rule
     * 
     * @param ruleid the database ID of the simple selection rule to delete
     * @param id the database ID of the product class
     * @throws EntityNotFoundException if the selection rule to delete or the product class do not exist in the database
     * @throws IllegalArgumentException if the ID of the product class or the selection rule was not given
     */
	public void deleteSelectionrule(Long ruleid, Long id) throws EntityNotFoundException, IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteSelectionrule({}, {})", ruleid, id);
		
		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_ID_MISSING, MSG_ID_PRODUCT_CLASS_ID_MISSING));
		}
		if (null == ruleid || 0 == ruleid) {
			throw new IllegalArgumentException(logError(MSG_SELECTION_RULE_ID_MISSING, MSG_ID_SELECTION_RULE_ID_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_PRODUCT_CLASS_ID_NOT_FOUND, MSG_ID_PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		// Find requested simple selection rule
		SimpleSelectionRule modelRuleToDelete = null;
		for (SimpleSelectionRule modelRule: modelProductClass.get().getRequiredSelectionRules()) {
			if (modelRule.getId() == ruleid.longValue()) {
				modelRuleToDelete = modelRule;
				break;
			}
		}
		
		// Selection rule not found
		if (null == modelRuleToDelete) {
			throw new EntityNotFoundException(
					logError(MSG_SELECTION_RULE_ID_NOT_FOUND, MSG_ID_SELECTION_RULE_ID_NOT_FOUND, ruleid, id));
		}
		
		modelProductClass.get().getRequiredSelectionRules().remove(modelRuleToDelete);
		RepositoryService.getProductClassRepository().save(modelProductClass.get());

		logInfo(MSG_SELECTION_RULE_DELETED, MSG_ID_SELECTION_RULE_DELETED, ruleid, id);
	}

    /**
     * Add the configured processor to the selection rule (if it is not already part of the selection rule)
     * 
     * @param configuredProcessor the name of the configured processor to add to the selection rule
     * @param ruleid the database ID of the simple selection rule
     * @param id the database ID of the product class
     * @return the modified selection rule in Rule Language
     * @throws EntityNotFoundException if no configured processor with the given name or no selection rule or product class with the given ID exist
     * @throws IllegalArgumentException if the product class ID, the selection rule ID or the name of the configured processor were not given
     */
	public SelectionRuleString addProcessorToRule(String configuredProcessor, Long ruleid, Long id) throws EntityNotFoundException, IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> addProcessorToRule({}, {}, {})", configuredProcessor, ruleid, id);
		
		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_ID_MISSING, MSG_ID_PRODUCT_CLASS_ID_MISSING));
		}
		if (null == ruleid || 0 == ruleid) {
			throw new IllegalArgumentException(logError(MSG_SELECTION_RULE_ID_MISSING, MSG_ID_SELECTION_RULE_ID_MISSING));
		}
		if (null == configuredProcessor || 0 == configuredProcessor.length()) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_NAME_MISSING, MSG_ID_PROCESSOR_NAME_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_PRODUCT_CLASS_ID_NOT_FOUND, MSG_ID_PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		// Find requested simple selection rule
		for (SimpleSelectionRule modelRule: modelProductClass.get().getRequiredSelectionRules()) {
			if (modelRule.getId() == id.longValue()) {
				// Retrieve the processor
				ConfiguredProcessor newProcessor = RepositoryService.getConfiguredProcessorRepository().findByIdentifier(configuredProcessor);
				if (null == newProcessor) {
					throw new EntityNotFoundException(logError(MSG_INVALID_PROCESSOR, MSG_ID_INVALID_PROCESSOR, configuredProcessor));
				}
				// Add the processor, if is not yet added
				if (!modelRule.getApplicableConfiguredProcessors().contains(newProcessor)) {
					modelRule.getApplicableConfiguredProcessors().add(newProcessor);
					RepositoryService.getProductClassRepository().save(modelProductClass.get());
					logInfo(MSG_PROCESSOR_ADDED, MSG_ID_PROCESSOR_ADDED, configuredProcessor, ruleid, id);
				} else {
					logInfo(MSG_SELECTION_RULE_NOT_MODIFIED, MSG_ID_SELECTION_RULE_NOT_MODIFIED, ruleid, id);
				}
				
				// Return the updated selection rule
				SelectionRuleString restRule = new SelectionRuleString();
				restRule.setId(modelRule.getId());
				restRule.setVersion(Long.valueOf(modelRule.getVersion()));
				restRule.setMode(modelRule.getMode());
				restRule.setSelectionRule(modelRule.toString());
				for (ConfiguredProcessor modelProcessor: modelRule.getApplicableConfiguredProcessors()) {
					restRule.getConfiguredProcessors().add(modelProcessor.getIdentifier());
				}
				return restRule;
			}
		}
		
		// Selection rule not found
		throw new EntityNotFoundException(logError(MSG_SELECTION_RULE_ID_NOT_FOUND, MSG_ID_SELECTION_RULE_ID_NOT_FOUND, ruleid, id));
	}

    /**
     * Remove the configured processor from the selection rule (the selection rule will be disconnected from the configured processor)
     * 
     * @param configuredProcessor the name of the configured processor to remove from the selection rule
     * @param ruleid the database ID of the simple selection rule
     * @param id the database ID of the product class
     * @throws EntityNotFoundException if no configured processor with the given name or no selection rule or product class with the given ID exist
     * @throws IllegalArgumentException if the product class ID, the selection rule ID or the name of the configured processor were not given
     */
	public SelectionRuleString removeProcessorFromRule(String configuredProcessor, Long ruleid, Long id) throws EntityNotFoundException, IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> removeProcessorFromRule({}, {}, {})", configuredProcessor, ruleid, id);
		
		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logError(MSG_PRODUCT_CLASS_ID_MISSING, MSG_ID_PRODUCT_CLASS_ID_MISSING));
		}
		if (null == ruleid || 0 == ruleid) {
			throw new IllegalArgumentException(logError(MSG_SELECTION_RULE_ID_MISSING, MSG_ID_SELECTION_RULE_ID_MISSING));
		}
		if (null == configuredProcessor || 0 == configuredProcessor.length()) {
			throw new IllegalArgumentException(logError(MSG_PROCESSOR_NAME_MISSING, MSG_ID_PROCESSOR_NAME_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_PRODUCT_CLASS_ID_NOT_FOUND, MSG_ID_PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		// Find requested simple selection rule
		for (SimpleSelectionRule modelRule: modelProductClass.get().getRequiredSelectionRules()) {
			if (modelRule.getId() == id.longValue()) {
				// Retrieve the processor
				ConfiguredProcessor newProcessor = RepositoryService.getConfiguredProcessorRepository().findByIdentifier(configuredProcessor);
				if (null == newProcessor) {
					throw new EntityNotFoundException(logError(MSG_INVALID_PROCESSOR, MSG_ID_INVALID_PROCESSOR, configuredProcessor));
				}
				// Add the processor, if is not yet added
				if (modelRule.getApplicableConfiguredProcessors().contains(newProcessor)) {
					modelRule.getApplicableConfiguredProcessors().remove(newProcessor);
					RepositoryService.getProductClassRepository().save(modelProductClass.get());
					logInfo(MSG_PROCESSOR_REMOVED, MSG_ID_PROCESSOR_REMOVED, configuredProcessor, ruleid, id);
				} else {
					throw new EntityNotFoundException(logError(MSG_PROCESSOR_NOT_FOUND, MSG_ID_PROCESSOR_NOT_FOUND, configuredProcessor, ruleid, id));
				}
				
				// Return the updated selection rule
				SelectionRuleString restRule = new SelectionRuleString();
				restRule.setId(modelRule.getId());
				restRule.setVersion(Long.valueOf(modelRule.getVersion()));
				restRule.setMode(modelRule.getMode());
				restRule.setSelectionRule(modelRule.toString());
				for (ConfiguredProcessor modelProcessor: modelRule.getApplicableConfiguredProcessors()) {
					restRule.getConfiguredProcessors().add(modelProcessor.getIdentifier());
				}
				return restRule;
			}
		}
		
		// Selection rule not found
		throw new EntityNotFoundException(logError(MSG_SELECTION_RULE_ID_NOT_FOUND, MSG_ID_SELECTION_RULE_ID_NOT_FOUND, ruleid, id));
	}

}
