/**
 * ProductClassManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.prodclmgr.rest;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProductClassMgrMessage;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.SimpleSelectionRule;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.enums.ProductVisibility;
import de.dlr.proseo.model.enums.ProcessingLevel;
import de.dlr.proseo.model.SimplePolicy;
import de.dlr.proseo.model.SimplePolicy.DeltaTime;
import de.dlr.proseo.model.SimplePolicy.PolicyType;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.model.util.SelectionRule;
import de.dlr.proseo.prodclmgr.ProductClassConfiguration;
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
	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** The product class configuration */
	ProductClassConfiguration config;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductClassManager.class);
	
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
	 * Set the enclosing class of the given product class to the class with the given mission code and product type;
	 * check for possible cycles
	 * 
	 * @param productClass the product class to update (must not be transient)
	 * @param missionCode the mission code of the enclosing product class
	 * @param productType the product type of the enclosing product class
	 * @throws IllegalArgumentException if a product class with the given mission code and product type does not exist,
	 *     or if the addition of the enclosing class would create a class cycle
	 */
	private void setEnclosingClass(ProductClass productClass, String missionCode, String productType) throws IllegalArgumentException {
		ProductClass enclosingClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(
				missionCode, productType);
		if (null == enclosingClass) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_ENCLOSING_CLASS,
					productType, missionCode));
		}
		// Check for class cycles
		if (hasEnclosingClassCycle(enclosingClass, productClass)) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.ENCLOSING_CLASS_CYCLE,
					productType, productClass.getProductType(), missionCode));
		}
		enclosingClass.getComponentClasses().add(productClass);
		enclosingClass = RepositoryService.getProductClassRepository().save(enclosingClass);
		productClass.setEnclosingClass(enclosingClass);
	}

	/**
	 * Check for cycles in the product class tree (upward direction)
	 * 
	 * @param enclosingClass the enclosing class to check
	 * @param productClass the product class to check against
	 * @return true, if adding the enclosing class would create a product class cycle, false otherwise
	 */
	private boolean hasEnclosingClassCycle(ProductClass enclosingClass, ProductClass productClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> hasEnclosingClassCycle({}, {})", enclosingClass.getProductType(),
				productClass.getProductType());

		if (enclosingClass.getProductType().equals(productClass.getProductType())) {
			return true;
		}
		if (null == enclosingClass.getEnclosingClass()) {
			return false;
		}
		return hasEnclosingClassCycle(enclosingClass.getEnclosingClass(), productClass);
	}

	/**
	 * Check for cycles in the product class tree (downward direction)
	 * 
	 * @param componentClass the component class to check
	 * @param productClass the product class to check against
	 * @return true, if adding the component class would create a product class cycle, false otherwise
	 */
	private boolean hasComponentClassCycle(ProductClass componentClass, ProductClass productClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> hasEnclosingClassCycle({}, {})", componentClass.getProductType(),
				productClass.getProductType());

		if (componentClass.getProductType().equals(productClass.getProductType())) {
			return true;
		}
		for (ProductClass componentComponentClass: componentClass.getComponentClasses()) {
			if (hasComponentClassCycle(componentComponentClass, productClass)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove the processor class from the given product class (and the product class from the processor class)
	 * 
	 * @param productClass
	 */
	private void removeProcessorClass(ProductClass productClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> removeProcessorClass({})", productClass.getProductType());

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
		if (logger.isTraceEnabled()) logger.trace(">>> setProcessorClass({}, {}, {})",
				modelProductClass.getProductType(), missionCode, processorName);

		ProcessorClass processorClass = RepositoryService.getProcessorClassRepository()
				.findByMissionCodeAndProcessorName(missionCode, processorName);
		if (null == processorClass) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_PROCESSOR_CLASS,
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
     * @param productType list of product types
     * @param processorClass list of processor class names
     * @param the processing level
     * @param the visibility
	 * @param recordFrom           first record of filtered and ordered result to return
	 * @param recordTo             last record of filtered and ordered result to return
     * @return a list of product classes conforming to the search criteria
	 * @throws NoResultException if no product classes matching the given search criteria could be found
     * @throws SecurityException if a cross-mission data access was attempted
     * @throws HttpClientErrorException.TooManyRequests if the result list would exceed a configured maximum
     */
	public List<RestProductClass> getRestProductClass(String mission, String[] productType, String[] processorClass, 
			String level, String visibility, String[] orderBy, Integer recordFrom, Integer recordTo) throws NoResultException, SecurityException, HttpClientErrorException.TooManyRequests {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestProductClass({}, {}, {}, {}, {})", mission, productType,
				recordFrom, recordTo, orderBy);
		
		if (null == mission) {
			mission = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(mission)) {
				throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
						mission, securityService.getMission()));
			} 
		}
		
		if (recordFrom == null) {
			recordFrom = 0;
		}
		if (recordTo == null) {
			recordTo = Integer.MAX_VALUE;
		}

		Long numberOfResults = Long.parseLong(this.countProductClasses(mission, productType, processorClass, level, visibility));
		Integer maxResults = config.getMaxResults();
		if (numberOfResults > maxResults && (recordTo - recordFrom) > maxResults
				&& (numberOfResults - recordFrom) > maxResults) {
			throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS,
					logger.log(GeneralMessage.TOO_MANY_RESULTS, "product classes", numberOfResults, config.getMaxResults()));
		}
		
		List<RestProductClass> result = new ArrayList<>();
		
		Query query = createProductClassesQuery(mission, productType, processorClass, level, visibility, recordFrom, recordTo, orderBy, false);
		query.setFirstResult(recordFrom);
		query.setMaxResults(recordTo - recordFrom);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof de.dlr.proseo.model.ProductClass) {
				result.add(ProductClassUtil.toRestProductClass((de.dlr.proseo.model.ProductClass) resultObject));
			}
		}
		if (result.isEmpty()) {
			throw new NoResultException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_NOT_FOUND_BY_SEARCH, 
					mission, productType));
		}

		logger.log(ProductClassMgrMessage.PRODUCT_CLASS_LIST_RETRIEVED, mission, productType);
		
		return result;
	}
	
    /**
     * Count product classes, optionally filtered by mission and/or product type
     * 
     * @param mission the mission code
     * @param productType the prosEO product type
     * @param productType list of product types
     * @param processorClass list of processor class names
     * @param the processing level
     * @param the visibility
     * @return a list of product classes conforming to the search criteria
	 * @throws NoResultException if no product classes matching the given search criteria could be found
     * @throws SecurityException if a cross-mission data access was attempted
     */
	public String countProductClasses(String mission, String[] productType, String[] processorClass, String level, String visibility) throws NoResultException, SecurityException {

		if (logger.isTraceEnabled()) logger.trace(">>> countProductClasses({})", mission);
		
		if (null == mission) {
			mission = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(mission)) {
				throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
						mission, securityService.getMission()));
			} 
		}
		Query query = createProductClassesQuery(mission, productType, processorClass, level, visibility, null, null, null, true);
		Object resultObject = query.getSingleResult();
		if (resultObject instanceof Long) {
			return ((Long)resultObject).toString();
		}
		if (resultObject instanceof String) {
			return (String) resultObject;
		}
		return "0";
	}

    /**
     * Count product classes, optionally filtered by mission and/or product type
     * 
     * @param mission the mission code
     * @param productType the prosEO product type
     * @return a list of product classes conforming to the search criteria
	 * @throws NoResultException if no product classes matching the given search criteria could be found
     * @throws SecurityException if a cross-mission data access was attempted
     */
    public List<String> getProductClassNames(java.lang.String mission, java.lang.String productType) throws NoResultException, SecurityException {

		if (logger.isTraceEnabled()) logger.trace(">>> countProductClasses({})", mission);
		
		if (null == mission) {
			mission = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(mission)) {
				throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
						mission, securityService.getMission()));
			} 
		}
		String jpqlQuery = null;
		String join = "";
		jpqlQuery = "select distinct p.productType from ProductClass p " + join + " where p.mission.code = :missionCode";

		if (null != productType) {
			jpqlQuery += " and productType = :productType";
		}
				
		Query query = em.createQuery(jpqlQuery);
		query.setParameter("missionCode", mission);

		if (productType != null) {
			query.setParameter("productType", productType);
		}
		ArrayList<String> result = new ArrayList<String>();
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof String) {
				result.add((String)resultObject);
			}
		}
		if (result.isEmpty()) {
			throw new NoResultException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_NOT_FOUND_BY_SEARCH, 
					mission, productType));
		}

		logger.log(ProductClassMgrMessage.PRODUCT_CLASS_LIST_RETRIEVED, mission, productType);
		
		return result;
	}

	/*
	 * @param mission the mission code (will be set to logged in mission, if not given; otherwise must match logged in mission)
     * @param productType list of product types
     * @param processorClass list of processor class names
     * @param the processing level
     * @param the visibility
	 * @param orderBy an array of strings containing a column name and an optional sort direction (ASC/DESC), separated by white space
	 * @return JPQL Query
	 */
	private Query createProductClassesQuery(String mission, String[] productType, String[] processorClass, String level, String visibility,
			Integer recordFrom, Integer recordTo, String[] orderBy, Boolean count) {

		if (logger.isTraceEnabled()) logger.trace(">>> createProductClassesQuery({}, {}, {}, {}, {}, {})", mission, productType, recordFrom, recordTo, orderBy, count);
		
		// Find using search parameters
		String jpqlQuery = null;
		String join = "";
		if (count) {
			jpqlQuery = "select count(p) from ProductClass p " + join + " where p.mission.code = :missionCode";
		} else {
			jpqlQuery = "select p from ProductClass p " + join + " where p.mission.code = :missionCode";
		}

		if (null != productType && 0 < productType.length) {
			jpqlQuery += " and productType in (";
			for (int i = 0; i < productType.length; ++i) {
				if (0 < i) jpqlQuery += ", ";
				jpqlQuery += ":productType" + i;
			}
			jpqlQuery += ")";
		}
		if (null != processorClass && 0 < processorClass.length) {
			jpqlQuery += " and processorClass.processorName in (";
			for (int i = 0; i < processorClass.length; ++i) {
				if (0 < i) jpqlQuery += ", ";
				jpqlQuery += ":processorClass" + i;
			}
			jpqlQuery += ")";
		}
		if (null != level) {
			jpqlQuery += " and p.processingLevel = :level";
		}
		if (null != visibility) {
			jpqlQuery += " and p.visibility = :visibility";
		}
		// order by
		if (null != orderBy && 0 < orderBy.length) {
			jpqlQuery += " order by ";
			for (int i = 0; i < orderBy.length; ++i) {
				if (0 < i) jpqlQuery += ", ";
				jpqlQuery += "p.";
				jpqlQuery += orderBy[i];
			}
		}

		Query query = em.createQuery(jpqlQuery);
		query.setParameter("missionCode", mission);

		if (null != productType && 0 < productType.length) {
			for (int i = 0; i < productType.length; ++i) {
				query.setParameter("productType" + i, productType[i]);
			}
		}
		if (null != processorClass && 0 < processorClass.length) {
			for (int i = 0; i < processorClass.length; ++i) {
				query.setParameter("processorClass" + i, processorClass[i]);
			}
		}
		if (null != level) {
			query.setParameter("level", ProcessingLevel.valueOf(level));
		}
		if (null != visibility) {
			query.setParameter("visibility", ProductVisibility.valueOf(visibility));
		}

		// length of record list
		if (recordFrom != null && recordFrom >= 0) {
			query.setFirstResult(recordFrom.intValue());
		}
		if (recordTo != null && recordTo >= 0) {
			query.setMaxResults(recordTo.intValue() - recordFrom.intValue());
		}
		return query;
	}
	
    /**
     * Create a new product class
     * 
     * @param productClass a Json object describing the new product class
     * @return a Json object describing the product class created (including ID and version) and HTTP status CREATED
 	 * @throws IllegalArgumentException if any of the input data was invalid
 	 * @throws ServerErrorException if saving the product class to the database fails
     * @throws SecurityException if a cross-mission data access was attempted
    */
	public RestProductClass createRestProductClass(RestProductClass productClass) throws
			IllegalArgumentException, ServerErrorException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> createRestProductClass({})", (null == productClass ? "MISSING" : productClass.getProductType()));
		
		if (null == productClass) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_MISSING));
		}
		
		// Ensure user is authorized for the mission of the product class
		if (!securityService.isAuthorizedForMission(productClass.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					productClass.getMissionCode(), securityService.getMission()));			
		}
		
		// Ensure mandatory attributes are set
		if (null == productClass.getProductType() || productClass.getProductType().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "productType", "productClass creation"));
		}
		if (null == productClass.getVisibility() || productClass.getVisibility().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "visibility", "productClass creation"));
		}
		
		// If list attributes were explicitly set to null, initialize with empty list
		if (null == productClass.getComponentClasses()) {
			productClass.setComponentClasses(new ArrayList<String>());
		}
		if (null == productClass.getSelectionRule()) {
			productClass.setSelectionRule(new ArrayList<RestSimpleSelectionRule>());
		}
		
		// Create product class object
		ProductClass modelProductClass = ProductClassUtil.toModelProductClass(productClass);
		Mission mission = RepositoryService.getMissionRepository().findByCode(productClass.getMissionCode());
		if (null == mission) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_MISSION_CODE, productClass.getMissionCode()));
		}
		modelProductClass.setMission(mission);
		
		// Make sure a product class with the same product type does not yet exist for the mission
		if (null != RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(
				productClass.getMissionCode(), productClass.getProductType())) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_EXISTS,
					productClass.getProductType(), productClass.getMissionCode()));
		}

		// Create a persistent product class for reference in other objects
		modelProductClass = RepositoryService.getProductClassRepository().save(modelProductClass);
		
		// Add further attributes to product class
		if (null != productClass.getEnclosingClass()) {
			setEnclosingClass(modelProductClass, mission.getCode(), productClass.getEnclosingClass());
		}
		
		for (String componentClass: productClass.getComponentClasses()) {
			ProductClass modelComponentClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(mission.getCode(), componentClass);
			if (null == modelComponentClass) {
				throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_COMPONENT_CLASS,
						componentClass, mission.getCode()));
			}
			if (hasComponentClassCycle(modelComponentClass, modelProductClass)) {
				throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.COMPONENT_CLASS_CYCLE,
						componentClass, productClass.getProductType(), mission.getCode()));
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
				throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_PROCESSING_MODE,
						rule.getMode(), mission.getCode()));
			}
			modelRule.setIsMandatory(rule.getIsMandatory());
			modelRule.setTargetProductClass(modelProductClass);
			modelRule.setSourceProductClass(RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(mission.getCode(), rule.getSourceProductClass()));
			if (null == modelRule.getSourceProductClass()) {
				throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_SOURCE_CLASS,
						rule.getSourceProductClass(), mission.getCode()));
			}

			for (RestParameter filterCondition: rule.getFilterConditions()) {
				if (null == filterCondition.getKey()) {
					throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_PARAMETER_KEY, filterCondition.toString()));
				}
				try {
					Parameter modelParameter = new Parameter().init(ParameterType.valueOf(filterCondition.getParameterType()), filterCondition.getParameterValue());
					modelRule.getFilterConditions().put(filterCondition.getKey(), modelParameter);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_PARAMETER_TYPE, filterCondition.getParameterType()));
				}
			}
			
			for (String configuredProcessor: rule.getConfiguredProcessors()) {
				ConfiguredProcessor modelProcessor = RepositoryService.getConfiguredProcessorRepository()
						.findByMissionCodeAndIdentifier(productClass.getMissionCode(), configuredProcessor);
				if (null == modelProcessor) {
					throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_PROCESSOR, configuredProcessor));
				}
				modelRule.getConfiguredProcessors().add(modelProcessor);
			}
			
			for (RestSimplePolicy policy: rule.getSimplePolicies()) {
				SimplePolicy modelPolicy = new SimplePolicy();
				try {
					modelPolicy.setPolicyType(PolicyType.valueOf(policy.getPolicyType()));
				} catch (Exception e) {
					throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_POLICY_TYPE, policy.getPolicyType()));
				}
				try {
					modelPolicy.setDeltaTimeT0(new DeltaTime(policy.getDeltaTimeT0().getDuration(), TimeUnit.valueOf(policy.getDeltaTimeT0().getUnit())));
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_TIME_UNIT, policy.getDeltaTimeT0().getUnit()));
				}
				try {
					modelPolicy.setDeltaTimeT1(new DeltaTime(policy.getDeltaTimeT1().getDuration(), TimeUnit.valueOf(policy.getDeltaTimeT1().getUnit())));
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_TIME_UNIT, policy.getDeltaTimeT1().getUnit()));
				}
				modelRule.getSimplePolicies().add(modelPolicy);
			}
			
			modelProductClass.getRequiredSelectionRules().add(modelRule);
		}
		
		// Save the product class again with all sub-object references
		try {
			modelProductClass = RepositoryService.getProductClassRepository().save(modelProductClass);
		} catch (Exception e) {
			throw new ServerErrorException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_SAVE_FAILED, 
							productClass.getProductType(), productClass.getMissionCode(), e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		
		logger.log(ProductClassMgrMessage.PRODUCT_CLASS_CREATED, modelProductClass.getProductType(), mission.getCode());
		
		return ProductClassUtil.toRestProductClass(modelProductClass);
	}

    /**
     * Get a product class by ID
     * 
     * @param id the database ID of the product class
     * @return a Json object describing the product class and HTTP status OK
	 * @throws IllegalArgumentException if no product class ID was given
	 * @throws NoResultException if no product class with the given ID exists
     * @throws SecurityException if a cross-mission data access was attempted
     */
	public RestProductClass getRestProductClassById(Long id) throws IllegalArgumentException, NoResultException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getRestProductClassById({})", id);
		
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new NoResultException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		// Ensure user is authorized for the mission of the configuration
		if (!securityService.isAuthorizedForMission(modelProductClass.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProductClass.get().getMission().getCode(), securityService.getMission()));			
		}
		
	logger.log(ProductClassMgrMessage.PRODUCT_CLASS_RETRIEVED, id);

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
     * @throws SecurityException if a cross-mission data access was attempted
	 * @throws ConcurrentModificationException if the product class has been modified since retrieval by the client
     */
	public RestProductClass modifyRestProductClass(Long id, RestProductClass productClass) throws
			EntityNotFoundException, IllegalArgumentException, SecurityException, ConcurrentModificationException {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyRestProductClass({}, {})", id, (null == productClass ? "MISSING" : productClass.getProductType()));
		
		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_MISSING));
		}
		if (null == productClass) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_DATA_MISSING));
		}
		
		// Ensure user is authorized for the mission of the product class
		if (!securityService.isAuthorizedForMission(productClass.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					productClass.getMissionCode(), securityService.getMission()));			
		}
		
		Optional<ProductClass> optProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (optProductClass.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		ProductClass modelProductClass = optProductClass.get();
		
		// Make sure we are allowed to change the product class(no intermediate update)
		if (modelProductClass.getVersion() != productClass.getVersion().intValue()) {
			throw new ConcurrentModificationException(logger.log(ProductClassMgrMessage.CONCURRENT_UPDATE, id));
		}
		
		// Ensure mandatory attributes are set
		if (null == productClass.getProductType() || productClass.getProductType().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "productType", "productClass modification"));
		}
		if (null == productClass.getVisibility() || productClass.getVisibility().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "visibility", "productClass modification"));
		}
		
		// If list attributes were explicitly set to null, initialize with empty list
		if (null == productClass.getComponentClasses()) {
			productClass.setComponentClasses(new ArrayList<String>());
		}
		if (null == productClass.getSelectionRule()) {
			productClass.setSelectionRule(new ArrayList<RestSimpleSelectionRule>());
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
		if (null == modelProductClass.getProcessingLevel() && null != changedProductClass.getProcessingLevel()
				|| null != modelProductClass.getProcessingLevel() && !modelProductClass.getProcessingLevel().equals(changedProductClass.getProcessingLevel())) {
			productClassChanged = true;
			modelProductClass.setProcessingLevel(changedProductClass.getProcessingLevel());
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

		if (logger.isTraceEnabled()) logger.trace("... scalar attributes for product class have changed: " + productClassChanged);
		
		// Update product file template, if different from mission template (uses REST product class for comparison!)
		if (modelProductClass.getMission().getProductFileTemplate().equals(modelProductClass.getProductFileTemplate())) {
			// Currently no template set --> set template, if a new template different from the mission's template was given
			if (!modelProductClass.getMission().getProductFileTemplate().equals(productClass.getProductFileTemplate())) {
				if (logger.isTraceEnabled()) logger.trace("... new product file template for product class set");
				productClassChanged = true;
				modelProductClass.setProductFileTemplate(productClass.getProductFileTemplate());
			}
		} else if (null == productClass.getProductFileTemplate() 
				|| modelProductClass.getMission().getProductFileTemplate().equals(productClass.getProductFileTemplate())) {
			// Currently template is set, but new value is null or same as mission template --> unset template
			if (logger.isTraceEnabled()) logger.trace("... product file template for product class removed");
			productClassChanged = true;
			modelProductClass.setProductFileTemplate(null);
		} else if (!modelProductClass.getProductFileTemplate().equals(productClass.getProductFileTemplate())) {
			// Currently template is set, but a different value was given, which does not correspond to the mission template
			if (logger.isTraceEnabled()) logger.trace(String.format("... product file template for product changed from [%s] to [%s]",
					modelProductClass.getProductFileTemplate(), productClass.getProductFileTemplate()));
			productClassChanged = true;
			modelProductClass.setProductFileTemplate(productClass.getProductFileTemplate());
		}
		
		if (logger.isTraceEnabled()) logger.trace("... product file template for product class has changed: " + productClassChanged);
		
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
		
		if (logger.isTraceEnabled()) logger.trace("... processor class for product class has changed: " + productClassChanged);
		
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
					throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_COMPONENT_CLASS,
							changedComponentClass, productClass.getMissionCode()));
				}
				if (hasComponentClassCycle(newComponentClass, modelProductClass)) {
					throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.COMPONENT_CLASS_CYCLE,
							changedComponentClass, productClass.getProductType(),productClass.getMissionCode()));
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
		
		if (logger.isTraceEnabled()) logger.trace("... component classes for product class have changed: " + productClassChanged);
		
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
		
		if (logger.isTraceEnabled()) logger.trace("... enclosing class for product class has changed: " + productClassChanged);
		
		// Save product class only if anything was actually changed
		if (productClassChanged) {
			modelProductClass.incrementVersion();
			modelProductClass.setComponentClasses(newComponentClasses);
			modelProductClass = RepositoryService.getProductClassRepository().save(modelProductClass);
			logger.log(ProductClassMgrMessage.PRODUCT_CLASS_MODIFIED, id);
		} else {
			logger.log(ProductClassMgrMessage.PRODUCT_CLASS_NOT_MODIFIED, id);
		}
		
		return ProductClassUtil.toRestProductClass(modelProductClass);
	}

    /**
     * Delete a product class by ID (with all its selection rules)
     * 
     * @param id the database ID of the product class to delete
     * @throws EntityNotFoundException if the processor to delete does not exist in the database
     * @throws RuntimeException if the deletion was not performed as expected
     * @throws IllegalArgumentException if the product class ID was not given, or if dependent objects exist
     * @throws SecurityException if a cross-mission data access was attempted
     */
	public void deleteProductclassById(Long id)
			throws EntityNotFoundException, RuntimeException, IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductclassById({})", id);
		
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_MISSING));
		}
		
		// Test whether the product id is valid
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		if (modelProductClass.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_NOT_FOUND, id));
		}
		
		// Ensure user is authorized for the mission of the product class
		if (!securityService.isAuthorizedForMission(modelProductClass.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProductClass.get().getMission().getCode(), securityService.getMission()));			
		}
		
		// Test whether processor classes reference this product class
		if (null != modelProductClass.get().getProcessorClass()) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_HAS_PROCESSOR,
					modelProductClass.get().getMission().getCode(),
					modelProductClass.get().getProductType()));
		}
		// Test whether there are products for this class
		String jpqlQuery = "select p from Product p where productClass.mission.code = :missionCode"
				+ " and productClass.productType = :productType";
		Query query = em.createQuery(jpqlQuery);
		query.setParameter("missionCode", modelProductClass.get().getMission().getCode());
		query.setParameter("productType", modelProductClass.get().getProductType());
		
		if (!query.getResultList().isEmpty()) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_HAS_PRODUCTS,
					modelProductClass.get().getMission().getCode(),
					modelProductClass.get().getProductType()));
		}
		
		// Test whether there are selection rules having this class as source product class
		jpqlQuery = "select count(s) from SimpleSelectionRule s where sourceProductClass = :sourceProductClass";
		query = em.createQuery(jpqlQuery);
		query.setParameter("sourceProductClass", modelProductClass.get());
		Object result = query.getSingleResult();
		if (!(result instanceof Number) || 0 != ((Number) result).intValue()) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_HAS_SELECTION_RULES,
					modelProductClass.get().getProductType(), result));
		}
		
		// Delete the processor class
		RepositoryService.getProductClassRepository().deleteById(id);

		// Test whether the deletion was successful
		modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		if (!modelProductClass.isEmpty()) {
			throw new RuntimeException(logger.log(ProductClassMgrMessage.DELETION_UNSUCCESSFUL, id));
		}
		
		logger.log(ProductClassMgrMessage.PRODUCT_CLASS_DELETED, id);
	}

    /**
     * Get the simple selection rules as formatted string, optionally selected by source class
     * 
     * @param id the database ID of the product class to get the selection rule from
     * @param sourceClass the prosEO product type of the source class, from which the product class can be generated (may be null)
     * @return a list of strings describing the selection rules for all configured processors
	 * @throws NoResultException if no simple selection rules matching the given search criteria could be found
     * @throws SecurityException if a cross-mission data access was attempted
     */
    public List<SelectionRuleString> getSelectionRuleStrings(Long id, String sourceClass) throws NoResultException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getSelectionRuleStrings({}, {})", id, sourceClass);

		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new NoResultException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_NOT_FOUND, id));
		}

		// Ensure user is authorized for the mission of the product class
		if (!securityService.isAuthorizedForMission(modelProductClass.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProductClass.get().getMission().getCode(), securityService.getMission()));			
		}
		
		if (modelProductClass.get().getRequiredSelectionRules().isEmpty()) {
			throw new NoResultException(logger.log(ProductClassMgrMessage.NO_RULES_FOUND,
					modelProductClass.get().getProductType()));
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
				for (ConfiguredProcessor modelProcessor: modelRule.getConfiguredProcessors()) {
					restRule.getConfiguredProcessors().add(modelProcessor.getIdentifier());
				}
				result.add(restRule);
			}
		}
		if (result.isEmpty()) {
			throw new NoResultException(logger.log(ProductClassMgrMessage.NO_RULES_FOUND_FOR_SOURCE,
					modelProductClass.get().getProductType(), sourceClass));
		}
		
		logger.log(ProductClassMgrMessage.SELECTION_RULE_LIST_RETRIEVED, modelProductClass.get().getProductType(), sourceClass);
		
		return result;
	}

    /**
     * Create a selection rule using Rule Language
     * 
     * @param id the database ID of the product class
     * @param selectionRuleStrings a Json representation of a selection rule in Rule Language
     * @return a Json object representing the simple selection rule created and HTTP status CREATED
	 * @throws IllegalArgumentException if any of the input data was invalid
     * @throws SecurityException if a cross-mission data access was attempted
     */
	public RestProductClass createSelectionRuleString(Long id, @Valid List<SelectionRuleString> selectionRuleStrings) throws
			IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> createSelectionRuleString(SelectionRuleString[{}])", (null == selectionRuleStrings ? "MISSING" : selectionRuleStrings.size()));

		// Retrieve product class
		Optional<ProductClass> optProductClass = RepositoryService.getProductClassRepository().findById(id);
		if (optProductClass.isEmpty()) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_NOT_FOUND, id));
		}

		// Ensure user is authorized for the mission of the product class
		if (!securityService.isAuthorizedForMission(optProductClass.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					optProductClass.get().getMission().getCode(), securityService.getMission()));			
		}
		
		ProductClass productClass = optProductClass.get();
		
		// Process all selection rules
		for (SelectionRuleString restRuleString: selectionRuleStrings) {
			// Check the selection rule parameters
			String processingMode = restRuleString.getMode();
			if (null != processingMode) {
				if (processingMode.isBlank()) {
					processingMode = null;
				} else if (!productClass.getMission().getProcessingModes().contains(processingMode)) {
					throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_PROCESSING_MODE,
							processingMode, productClass.getMission().getCode()));
				}
			}
			
			Set<ConfiguredProcessor> configuredProcessors = new HashSet<>();
			for (String configuredProcessorIdentifier: restRuleString.getConfiguredProcessors()) {
				ConfiguredProcessor configuredProcessor = RepositoryService.getConfiguredProcessorRepository()
						.findByMissionCodeAndIdentifier(optProductClass.get().getMission().getCode(), configuredProcessorIdentifier);
				if (null == configuredProcessor) {
					throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_PROCESSOR, configuredProcessorIdentifier));
				}
				configuredProcessors.add(configuredProcessor);
			}
			
			// Parse the selection rule string
			SelectionRule selectionRule = null;
			try {
				selectionRule = SelectionRule.parseSelectionRule(productClass, restRuleString.getSelectionRule());
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.RULE_STRING_MISSING, restRuleString));
			} catch (ParseException e) {
				throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_RULE_STRING,
						restRuleString.getSelectionRule(), e.getMessage()));
			}
			
			// Complete the simple selection rules and add them to the product class, if no rule with equivalent key values exists
			for (SimpleSelectionRule simpleSelectionRule: selectionRule.getSimpleRules()) {
				// Set remaining attributes
				simpleSelectionRule.setMode(processingMode);
				simpleSelectionRule.getConfiguredProcessors().addAll(configuredProcessors);
				
				// Check for duplicates
				for (SimpleSelectionRule existingRule: productClass.getRequiredSelectionRules()) {
					if (existingRule.getSourceProductClass().equals(simpleSelectionRule.getSourceProductClass())
							&& Objects.equals(existingRule.getMode(), simpleSelectionRule.getMode())) {
						// Duplicate candidate - check applicable configured processors
						if (existingRule.getConfiguredProcessors().isEmpty() || simpleSelectionRule.getConfiguredProcessors().isEmpty()) {
							// At least one of the rules is applicable for all configured processors, so this is a duplicate
							throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.DUPLICATE_RULE,
									productClass.getProductType(), existingRule.getSourceProductClass().getProductType(),
									existingRule.getMode(), "(all)"));
						}
						for (ConfiguredProcessor existingProcessor: existingRule.getConfiguredProcessors()) {
							if (simpleSelectionRule.getConfiguredProcessors().contains(existingProcessor)) {
								// Overlapping set of configured processors, so this is a duplicate
								throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.DUPLICATE_RULE,
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
		logger.log(ProductClassMgrMessage.SELECTION_RULES_CREATED, selectionRuleStrings.size(), productClass.getProductType(), productClass.getMission().getCode());
		
		return ProductClassUtil.toRestProductClass(productClass);
	}

    /**
     * Get a selection rule by ID
     * 
     * @param ruleid the database ID of the simple selection rule to read
     * @param id the database ID of the product class
     * @return a Json object representing the simple selection rule in Rule Language
	 * @throws NoResultException if no selection rule or product class with the given ID exist
     * @throws SecurityException if a cross-mission data access was attempted
     */
	public SelectionRuleString getSelectionRuleString(Long ruleid, Long id) throws NoResultException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> getSelectionRuleString({}, {})", ruleid, id);

		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_MISSING));
		}
		if (null == ruleid || 0 == ruleid) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.SELECTION_RULE_ID_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new NoResultException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		// Ensure user is authorized for the mission of the product class
		if (!securityService.isAuthorizedForMission(modelProductClass.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProductClass.get().getMission().getCode(), securityService.getMission()));			
		}
		
		// Find requested simple selection rule
		for (SimpleSelectionRule modelRule: modelProductClass.get().getRequiredSelectionRules()) {
			if (modelRule.getId() == id.longValue()) {
				SelectionRuleString restRule = new SelectionRuleString();
				restRule.setId(modelRule.getId());
				restRule.setVersion(Long.valueOf(modelRule.getVersion()));
				restRule.setMode(modelRule.getMode());
				restRule.setSelectionRule(modelRule.toString());
				for (ConfiguredProcessor modelProcessor: modelRule.getConfiguredProcessors()) {
					restRule.getConfiguredProcessors().add(modelProcessor.getIdentifier());
				}
				logger.log(ProductClassMgrMessage.SELECTION_RULE_RETRIEVED, ruleid, id);
				return restRule;
			}
		}
		
		// Selection rule not found
		throw new NoResultException(logger.log(ProductClassMgrMessage.SELECTION_RULE_ID_NOT_FOUND, ruleid, id));
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
     * @throws SecurityException if a cross-mission data access was attempted
	 * @throws ConcurrentModificationException if the selection rule has been modified since retrieval by the client
     */
	public SelectionRuleString modifySelectionRuleString(Long ruleid, Long id,
			SelectionRuleString selectionRuleString) throws
			EntityNotFoundException, IllegalArgumentException, SecurityException, ConcurrentModificationException{
		if (logger.isTraceEnabled()) logger.trace(">>> modifySelectionRuleString({}, {}, {})", ruleid, id, selectionRuleString);

		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_MISSING));
		}
		if (null == ruleid || 0 == ruleid) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.SELECTION_RULE_ID_MISSING));
		}
		if (null == selectionRuleString || null == selectionRuleString.getSelectionRule() || 0 == selectionRuleString.getSelectionRule().length()) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.SELECTION_RULE_DATA_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new NoResultException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		// Ensure user is authorized for the mission of the product class
		if (!securityService.isAuthorizedForMission(modelProductClass.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProductClass.get().getMission().getCode(), securityService.getMission()));			
		}
		
		// Find requested simple selection rule
		for (SimpleSelectionRule modelRule: modelProductClass.get().getRequiredSelectionRules()) {
			if (modelRule.getId() == ruleid.longValue()) {
				// Make sure we are allowed to change the selection rule (no intermediate update)
				if (modelRule.getVersion() != selectionRuleString.getVersion().intValue()) {
					throw new ConcurrentModificationException(logger.log(ProductClassMgrMessage.CONCURRENT_RULE_UPDATE, ruleid));
				}
				
				// Parse the selection rule string
				SelectionRule changedRule = null;
				try {
					changedRule = SelectionRule.parseSelectionRule(modelProductClass.get(), selectionRuleString.getSelectionRule());
				} catch (ParseException e) {
					throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_RULE_STRING, selectionRuleString, e.getMessage()));
				}
				
				boolean ruleChanged = false;
				// Check whether the (normalized) selection rule string was changed
				List<SimpleSelectionRule> changedSimpleRules = changedRule.getSimpleRules();
				if (1 != changedSimpleRules.size()) {
					throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_RULE_STRING, selectionRuleString, ProductClassMgrMessage.EXACTLY_ONE_SELECTION_RULE_EXPECTED));
				}
				SimpleSelectionRule changedSimpleRule = changedSimpleRules.get(0);
				if (!modelRule.toString().equals(changedRule.toString())) {
					ruleChanged = true;
					modelRule.getFilterConditions().clear();
					modelRule.getFilterConditions().putAll(changedSimpleRule.getFilterConditions());
					modelRule.setFilteredSourceProductType(changedSimpleRule.getFilteredSourceProductType());
					modelRule.setIsMandatory(changedSimpleRule.getIsMandatory());
					modelRule.getSimplePolicies().clear();
					modelRule.getSimplePolicies().addAll(changedSimpleRule.getSimplePolicies());
					modelRule.setSourceProductClass(changedSimpleRule.getSourceProductClass());
					modelRule.setMinimumCoverage(changedSimpleRule.getMinimumCoverage());
					RepositoryService.getProductClassRepository().save(modelProductClass.get());
				}
				// Check mode change (including from/to null)
				if (!Objects.equals(modelRule.getMode(), selectionRuleString.getMode())) {
					ruleChanged = true;
					modelRule.setMode(selectionRuleString.getMode());
				}
				// Check for new configured processors
				Set<ConfiguredProcessor> newConfiguredProcessors = new HashSet<>();
				for (String changedProcessorName: selectionRuleString.getConfiguredProcessors()) {
					ConfiguredProcessor changedProcessor = RepositoryService.getConfiguredProcessorRepository()
							.findByMissionCodeAndIdentifier(modelProductClass.get().getMission().getCode(), changedProcessorName);
					if (null == changedProcessor) {
						throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.INVALID_PROCESSOR, changedProcessorName));
					}
					if (!modelRule.getConfiguredProcessors().contains(changedProcessor)) {
						ruleChanged = true;
					}
					newConfiguredProcessors.add(changedProcessor);
				}
				// Check for removed configured processors
				for (ConfiguredProcessor oldProcessor: modelRule.getConfiguredProcessors()) {
					if (!newConfiguredProcessors.contains(oldProcessor)) {
						ruleChanged = true;
					}
				}
				
				// Save simple selection rule only if anything was actually changed
				if (ruleChanged) {
					modelRule.incrementVersion();
					modelRule.setConfiguredProcessors(newConfiguredProcessors);
					RepositoryService.getProductClassRepository().save(modelProductClass.get());
				}
				
				SelectionRuleString restRule = new SelectionRuleString();
				restRule.setId(modelRule.getId());
				restRule.setVersion(Long.valueOf(modelRule.getVersion()));
				restRule.setMode(modelRule.getMode());
				restRule.setSelectionRule(modelRule.toString());
				for (ConfiguredProcessor modelProcessor: modelRule.getConfiguredProcessors()) {
					restRule.getConfiguredProcessors().add(modelProcessor.getIdentifier());
				}
				if (ruleChanged) {
					logger.log(ProductClassMgrMessage.SELECTION_RULE_MODIFIED, ruleid, id);
				} else {
					logger.log(ProductClassMgrMessage.SELECTION_RULE_NOT_MODIFIED, ruleid, id);
				}
				return restRule;
			}
		}
		
		// Selection rule not found
		throw new EntityNotFoundException(logger.log(ProductClassMgrMessage.SELECTION_RULE_ID_NOT_FOUND, ruleid, id));
	}

    /**
     * Delete a selection rule
     * 
     * @param ruleid the database ID of the simple selection rule to delete
     * @param id the database ID of the product class
     * @throws EntityNotFoundException if the selection rule to delete or the product class do not exist in the database
     * @throws IllegalArgumentException if the ID of the product class or the selection rule was not given, or the rule
     *             cannot be deleted due to existing product queries
     * @throws SecurityException if a cross-mission data access was attempted
     */
	public void deleteSelectionrule(Long ruleid, Long id) throws EntityNotFoundException, IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteSelectionrule({}, {})", ruleid, id);
		
		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_MISSING));
		}
		if (null == ruleid || 0 == ruleid) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.SELECTION_RULE_ID_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		// Ensure user is authorized for the mission of the product class
		if (!securityService.isAuthorizedForMission(modelProductClass.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProductClass.get().getMission().getCode(), securityService.getMission()));			
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
					logger.log(ProductClassMgrMessage.SELECTION_RULE_ID_NOT_FOUND, ruleid, id));
		}
		
		// Make sure rule is not used in any product query
		Query query = em.createQuery("select pq from ProductQuery pq where pq.generatingRule = :rule")
				.setParameter("rule", modelRuleToDelete);
		if (!query.getResultList().isEmpty()) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_QUERIES_EXIST,
					modelRuleToDelete.toString(), modelProductClass.get().getProductType()));
		}
		
		modelProductClass.get().getRequiredSelectionRules().remove(modelRuleToDelete);
		RepositoryService.getProductClassRepository().save(modelProductClass.get());

		logger.log(ProductClassMgrMessage.SELECTION_RULE_DELETED, ruleid, id);
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
     * @throws SecurityException if a cross-mission data access was attempted
     */
	public SelectionRuleString addProcessorToRule(String configuredProcessor, Long ruleid, Long id)
			throws EntityNotFoundException, IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> addProcessorToRule({}, {}, {})", configuredProcessor, ruleid, id);
		
		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_MISSING));
		}
		if (null == ruleid || 0 == ruleid) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.SELECTION_RULE_ID_MISSING));
		}
		if (null == configuredProcessor || 0 == configuredProcessor.length()) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PROCESSOR_NAME_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		// Ensure user is authorized for the mission of the product class
		if (!securityService.isAuthorizedForMission(modelProductClass.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProductClass.get().getMission().getCode(), securityService.getMission()));			
		}
		
		// Find requested simple selection rule
		for (SimpleSelectionRule modelRule: modelProductClass.get().getRequiredSelectionRules()) {
			if (modelRule.getId() == id.longValue()) {
				// Retrieve the processor
				ConfiguredProcessor newProcessor = RepositoryService.getConfiguredProcessorRepository()
						.findByMissionCodeAndIdentifier(modelProductClass.get().getMission().getCode(), configuredProcessor);
				if (null == newProcessor) {
					throw new EntityNotFoundException(logger.log(ProductClassMgrMessage.INVALID_PROCESSOR, configuredProcessor));
				}
				// Add the processor, if is not yet added
				if (!modelRule.getConfiguredProcessors().contains(newProcessor)) {
					modelRule.getConfiguredProcessors().add(newProcessor);
					RepositoryService.getProductClassRepository().save(modelProductClass.get());
					logger.log(ProductClassMgrMessage.PROCESSOR_ADDED, configuredProcessor, ruleid, id);
				} else {
					logger.log(ProductClassMgrMessage.SELECTION_RULE_NOT_MODIFIED, ruleid, id);
				}
				
				// Return the updated selection rule
				SelectionRuleString restRule = new SelectionRuleString();
				restRule.setId(modelRule.getId());
				restRule.setVersion(Long.valueOf(modelRule.getVersion()));
				restRule.setMode(modelRule.getMode());
				restRule.setSelectionRule(modelRule.toString());
				for (ConfiguredProcessor modelProcessor: modelRule.getConfiguredProcessors()) {
					restRule.getConfiguredProcessors().add(modelProcessor.getIdentifier());
				}
				return restRule;
			}
		}
		
		// Selection rule not found
		throw new EntityNotFoundException(logger.log(ProductClassMgrMessage.SELECTION_RULE_ID_NOT_FOUND, ruleid, id));
	}

    /**
     * Remove the configured processor from the selection rule (the selection rule will be disconnected from the configured processor)
     * 
     * @param configuredProcessor the name of the configured processor to remove from the selection rule
     * @param ruleid the database ID of the simple selection rule
     * @param id the database ID of the product class
     * @return the Json representation of the modified selection rule
     * @throws EntityNotFoundException if no configured processor with the given name or no selection rule or product class with the given ID exist
     * @throws IllegalArgumentException if the product class ID, the selection rule ID or the name of the configured processor were not given
     * @throws SecurityException if a cross-mission data access was attempted
     */
	public SelectionRuleString removeProcessorFromRule(String configuredProcessor, Long ruleid, Long id)
			throws EntityNotFoundException, IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled()) logger.trace(">>> removeProcessorFromRule({}, {}, {})", configuredProcessor, ruleid, id);
		
		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_MISSING));
		}
		if (null == ruleid || 0 == ruleid) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.SELECTION_RULE_ID_MISSING));
		}
		if (null == configuredProcessor || 0 == configuredProcessor.length()) {
			throw new IllegalArgumentException(logger.log(ProductClassMgrMessage.PROCESSOR_NAME_MISSING));
		}
		
		Optional<ProductClass> modelProductClass = RepositoryService.getProductClassRepository().findById(id);
		
		if (modelProductClass.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProductClassMgrMessage.PRODUCT_CLASS_ID_NOT_FOUND, id));
		}
		
		// Ensure user is authorized for the mission of the product class
		if (!securityService.isAuthorizedForMission(modelProductClass.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelProductClass.get().getMission().getCode(), securityService.getMission()));			
		}
		
		// Find requested simple selection rule
		for (SimpleSelectionRule modelRule: modelProductClass.get().getRequiredSelectionRules()) {
			if (modelRule.getId() == id.longValue()) {
				// Retrieve the processor
				ConfiguredProcessor newProcessor = RepositoryService.getConfiguredProcessorRepository()
						.findByMissionCodeAndIdentifier(modelProductClass.get().getMission().getCode(), configuredProcessor);
				if (null == newProcessor) {
					throw new EntityNotFoundException(logger.log(ProductClassMgrMessage.INVALID_PROCESSOR, configuredProcessor));
				}
				// Add the processor, if is not yet added
				if (modelRule.getConfiguredProcessors().contains(newProcessor)) {
					modelRule.getConfiguredProcessors().remove(newProcessor);
					RepositoryService.getProductClassRepository().save(modelProductClass.get());
					logger.log(ProductClassMgrMessage.PROCESSOR_REMOVED, configuredProcessor, ruleid, id);
				} else {
					throw new EntityNotFoundException(logger.log(ProductClassMgrMessage.PROCESSOR_NOT_FOUND, configuredProcessor, ruleid, id));
				}
				
				// Return the updated selection rule
				SelectionRuleString restRule = new SelectionRuleString();
				restRule.setId(modelRule.getId());
				restRule.setVersion(Long.valueOf(modelRule.getVersion()));
				restRule.setMode(modelRule.getMode());
				restRule.setSelectionRule(modelRule.toString());
				for (ConfiguredProcessor modelProcessor: modelRule.getConfiguredProcessors()) {
					restRule.getConfiguredProcessors().add(modelProcessor.getIdentifier());
				}
				return restRule;
			}
		}
		
		// Selection rule not found
		throw new EntityNotFoundException(logger.log(ProductClassMgrMessage.SELECTION_RULE_ID_NOT_FOUND, ruleid, id));
	}

}
