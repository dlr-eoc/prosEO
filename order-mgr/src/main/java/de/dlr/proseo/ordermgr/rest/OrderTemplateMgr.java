/**
 * OrderTemplateMgr.java
 *
 * (C) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import javax.ws.rs.ProcessingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OrderMgrMessage;
import de.dlr.proseo.model.ClassOutputParameter;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.InputFilter;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.OrderTemplate;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductQuery;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.enums.UserRole;
import de.dlr.proseo.model.rest.model.RestClassOutputParameter;
import de.dlr.proseo.model.rest.model.RestInputFilter;
import de.dlr.proseo.model.rest.model.RestOrbitQuery;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestOrderTemplate;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.ordermgr.rest.model.OrderTemplateUtil;

/**
 * Service methods required to create, modify and delete order template in the prosEO database, and to query the database about
 * such order templates
 *
 * @author Ernst Melchinger
 */
@Component
public class OrderTemplateMgr {

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderTemplateMgr.class);

	/**
	 * Create an order from the given Json object
	 *
	 * @param order the Json object to create the order from
	 * @return a Json object corresponding to the order after persistence (with ID and version for all contained objects)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public RestOrderTemplate createOrderTemplate(RestOrderTemplate restOrderTemplate) throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createOrderTemplate({})", (null == restOrderTemplate ? "MISSING" : restOrderTemplate.getName()));

		if (null == restOrderTemplate) {
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.ORDERTEMPLATE_MISSING));
		}

		// Ensure user is authorized for the restOrderTemplate mission
		if (!securityService.isAuthorizedForMission(restOrderTemplate.getMissionCode())) {
			throw new SecurityException(
					logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, restOrderTemplate.getMissionCode(), securityService.getMission()));
		}

		// Ensure mandatory attributes are set
		if (null == restOrderTemplate.getName() || restOrderTemplate.getName().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "name", "order template creation"));
		}
		if (null == restOrderTemplate.getSlicingType() || restOrderTemplate.getSlicingType().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "slicingType", "order template creation"));
		}
		if (null == restOrderTemplate.getRequestedProductClasses() || restOrderTemplate.getRequestedProductClasses().isEmpty()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "requestedProductClasses", "order template creation"));
		}
		if (null == restOrderTemplate.getOutputFileClass() || restOrderTemplate.getOutputFileClass().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "outputFileClass", "order template creation"));
		}
		if (null == restOrderTemplate.getProcessingMode() || restOrderTemplate.getProcessingMode().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "processingMode", "order template creation"));
		}

		// If list attributes were set to null explicitly, initialize with empty lists
		if (null == restOrderTemplate.getInputProductClasses()) {
			restOrderTemplate.setInputProductClasses(new ArrayList<String>());
		}
		if (null == restOrderTemplate.getInputFilters()) {
			restOrderTemplate.setInputFilters(new ArrayList<RestInputFilter>());
		}
		if (null == restOrderTemplate.getOutputParameters()) {
			restOrderTemplate.setOutputParameters(new ArrayList<RestParameter>());
		}
		if (null == restOrderTemplate.getClassOutputParameters()) {
			restOrderTemplate.setClassOutputParameters(new ArrayList<RestClassOutputParameter>());
		}
		if (null == restOrderTemplate.getConfiguredProcessors()) {
			restOrderTemplate.setConfiguredProcessors(new ArrayList<String>());
		}
		
		// Prepare the database restOrderTemplate, but make sure ID and version are not copied if present
		restOrderTemplate.setId(null);
		restOrderTemplate.setVersion(null);

		OrderTemplate modelOrderTemplate = OrderTemplateUtil.toModelOrderTemplate(restOrderTemplate);

		// The mission must be set
		if (null == modelOrderTemplate.getMission()) {
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.MISSION_CODE_MISSING));
		}

		try {

			Mission mission = RepositoryService.getMissionRepository().findByCode(modelOrderTemplate.getMission().getCode());

			// Make sure order name is not yet in use
			if (null != RepositoryService.getOrderTemplateRepository()
				.findByMissionCodeAndName(restOrderTemplate.getMissionCode(), modelOrderTemplate.getName())) {
				throw new IllegalArgumentException(
						logger.log(OrderMgrMessage.DUPLICATE_ORDERTEMPLATE_NAME, modelOrderTemplate.getName(), restOrderTemplate.getMissionCode()));
			}



			// Create input filters
			for (RestInputFilter restInputFilter : restOrderTemplate.getInputFilters()) {
				InputFilter inputFilter = new InputFilter();
				inputFilter = RepositoryService.getInputFilterRepository().save(inputFilter);
				for (RestParameter restParam : restInputFilter.getFilterConditions()) {
					Parameter modelParam = new Parameter();
					modelParam.init(ParameterType.valueOf(restParam.getParameterType()), restParam.getParameterValue());
					inputFilter.getFilterConditions().put(restParam.getKey(), modelParam);
				}
				ProductClass productClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(mission.getCode(), restInputFilter.getProductClass());
				if (null == productClass) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_INPUT_CLASS, restInputFilter.getProductClass(), mission.getCode()));
				}
				modelOrderTemplate.getInputFilters().put(productClass, inputFilter);
			}

			// Create class output parameters
			for (RestClassOutputParameter restClassOutputParameter : restOrderTemplate.getClassOutputParameters()) {
				ClassOutputParameter classOutputParameter = new ClassOutputParameter();
				classOutputParameter = RepositoryService.getClassOutputParameterRepository().save(classOutputParameter);
				for (RestParameter restParam : restClassOutputParameter.getOutputParameters()) {
					Parameter modelParam = new Parameter();
					modelParam.init(ParameterType.valueOf(restParam.getParameterType()), restParam.getParameterValue());
					classOutputParameter.getOutputParameters().put(restParam.getKey(), modelParam);
				}
				ProductClass productClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(mission.getCode(), restClassOutputParameter.getProductClass());
				if (null == productClass) {
					throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_OUTPUT_CLASS,
							restClassOutputParameter.getProductClass(), mission.getCode()));
				}
				modelOrderTemplate.getClassOutputParameters().put(productClass, classOutputParameter);
			}

			// Find requested product classes
			modelOrderTemplate.getRequestedProductClasses().clear();
			for (String productType : restOrderTemplate.getRequestedProductClasses()) {
				ProductClass productClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(mission.getCode(), productType);
				if (null == productClass) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_REQUESTED_CLASS, productType, mission.getCode()));
				}
				modelOrderTemplate.getRequestedProductClasses().add(productClass);
			}

			// Find input product classes
			modelOrderTemplate.getInputProductClasses().clear();
			for (String productType : restOrderTemplate.getInputProductClasses()) {
				ProductClass productClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(mission.getCode(), productType);
				if (null == productClass) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_INPUT_CLASS, productType, mission.getCode()));
				}
				modelOrderTemplate.getInputProductClasses().add(productClass);
			}

			// Find requested configured processors
			modelOrderTemplate.getRequestedConfiguredProcessors().clear();
			for (String identifier : restOrderTemplate.getConfiguredProcessors()) {
				ConfiguredProcessor configuredProcessor = RepositoryService.getConfiguredProcessorRepository()
					.findByMissionCodeAndIdentifier(restOrderTemplate.getMissionCode(), identifier);
				if (null == configuredProcessor) {
					throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_CONFIGURED_PROCESSOR, identifier));
				}
				modelOrderTemplate.getRequestedConfiguredProcessors().add(configuredProcessor);
			}

			// Make sure processing mode and file class are OK
			if (!mission.getProcessingModes().contains(restOrderTemplate.getProcessingMode())) {
				throw new IllegalArgumentException(
						logger.log(OrderMgrMessage.INVALID_PROCESSING_MODE, restOrderTemplate.getProcessingMode(), mission.getCode()));
			}
			if (!mission.getFileClasses().contains(restOrderTemplate.getOutputFileClass())) {
				throw new IllegalArgumentException(
						logger.log(OrderMgrMessage.INVALID_FILE_CLASS, restOrderTemplate.getOutputFileClass(), mission.getCode()));
			}

			// Everything OK, store new order in database
			modelOrderTemplate = RepositoryService.getOrderTemplateRepository().save(modelOrderTemplate);
			logger.log(OrderMgrMessage.ORDERTEMPLATE_CREATED, restOrderTemplate.getName(), restOrderTemplate.getMissionCode());
			
			// Create and initialize the history element of the processing order.
			return OrderTemplateUtil.toRestOrderTemplate(modelOrderTemplate);

		} catch (org.springframework.dao.DataIntegrityViolationException e) {

			if (null == RepositoryService.getMissionRepository().findByCode(modelOrderTemplate.getMission().getCode()))
				throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_MISSION_CODE, restOrderTemplate.getMissionCode()));

			throw e;
		}
	}

	/**
	 * Delete an order by entity
	 *
	 * @param orderTemplate the order to delete
	 * @throws EntityNotFoundException if the order to delete does not exist in the database
	 * @throws RuntimeException        if the deletion was not performed as expected
	 */
	private void deleteOrderTemplate(OrderTemplate orderTemplate) throws EntityNotFoundException, RuntimeException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteOrderTemplate({})", orderTemplate.getName());

		// Delete the order
		long id = orderTemplate.getId();
		RepositoryService.getOrderTemplateRepository().delete(orderTemplate);
		// Test whether the deletion was successful
		Optional<OrderTemplate> modelOrderTemplate = RepositoryService.getOrderTemplateRepository().findById(id);
		if (!modelOrderTemplate.isEmpty()) {
			throw new RuntimeException(logger.log(OrderMgrMessage.DELETION_UNSUCCESSFUL, id));
		}

		logger.log(OrderMgrMessage.ORDERTEMPLATE_DELETED, id);
	}

	/**
	 * Delete an order by ID
	 *
	 * @param id the ID of the order to delete
	 * @throws EntityNotFoundException if the order to delete does not exist in the database
	 * @throws SecurityException       if a cross-mission data access was attempted
	 * @throws RuntimeException        if the deletion was not performed as expected
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void deleteOrderTemplateById(Long id) throws EntityNotFoundException, SecurityException, RuntimeException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteOrderTemplateById({})", id);

		// Test whether the order id is valid
		Optional<OrderTemplate> modelOrderTemplate = RepositoryService.getOrderTemplateRepository().findById(id);
		if (modelOrderTemplate.isEmpty()) {
			throw new EntityNotFoundException(logger.log(OrderMgrMessage.ORDERTEMPLATE_NOT_FOUND, id));
		}

		// Ensure user is authorized for the order mission
		if (!securityService.isAuthorizedForMission(modelOrderTemplate.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelOrderTemplate.get().getMission().getCode(), securityService.getMission()));
		}
		deleteOrderTemplate(modelOrderTemplate.get());
	}

	/**
	 * Find the oder with the given ID
	 *
	 * @param id the ID to look for
	 * @return a Json object corresponding to the order found
	 * @throws IllegalArgumentException if no order ID was given
	 * @throws NoResultException        if no order with the given ID exists
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public RestOrderTemplate getOrderTemplateById(Long id) throws IllegalArgumentException, NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getOrderTemplateById({})", id);

		if (null == id) {
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.ORDERTEMPLATE_MISSING, id));
		}
		if (id == 0) {
			// new order from "scratch", used at least if GUI
			// TODO Check if this should be moved to GUI (at least partially) or removed
			// altogether
			// Having id == 0 is contrary to the interface contract, which requires a valid
			// object database ID
			// Furthermore default values shall not deviate from the default values given in
			// the UML model
			RestOrderTemplate newOrderTemplate = new RestOrderTemplate();
			newOrderTemplate.setName("New");
			return newOrderTemplate;
		} else {
			Optional<OrderTemplate> modelOrderTemplate = RepositoryService.getOrderTemplateRepository().findById(id);

			if (modelOrderTemplate.isEmpty()) {
				throw new NoResultException(logger.log(OrderMgrMessage.ORDERTEMPLATE_NOT_FOUND, id));
			}

			// Ensure user is authorized for the order mission
			if (!securityService.isAuthorizedForMission(modelOrderTemplate.get().getMission().getCode())) {
				throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
						modelOrderTemplate.get().getMission().getCode(), securityService.getMission()));
			}

			logger.log(OrderMgrMessage.ORDERTEMPLATE_RETRIEVED, id);

			return OrderTemplateUtil.toRestOrderTemplate(modelOrderTemplate.get());
		}
	}

	/**
	 * Update the order with the given ID with the attribute values of the given Json object. Orders may only be changed while they
	 * are in state "INITIAL". The only state modification allowed here is from INITIAL to APPROVED.
	 *
	 * @param id    the ID of the product to update
	 * @param restOrderTemplate a Json object containing the modified (and unmodified) attributes
	 * @return a Json object corresponding to the product after modification (with ID and version for all contained objects)
	 * @throws EntityNotFoundException         if no product with the given ID exists
	 * @throws IllegalArgumentException        if any of the input data was invalid
	 * @throws SecurityException               if a cross-mission data access was attempted
	 * @throws ConcurrentModificationException if the order has been modified since retrieval by the client
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public RestOrderTemplate modifyOrderTemplate(Long id, RestOrderTemplate restOrderTemplate)
			throws EntityNotFoundException, IllegalArgumentException, SecurityException, ConcurrentModificationException {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyOrderTemplate({})", id);

		if (null == id) {
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.ORDERTEMPLATE_MISSING, id));
		}

		// Ensure user is authorized for the order mission
		if (!securityService.isAuthorizedForMission(restOrderTemplate.getMissionCode())) {
			throw new SecurityException(
					logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, restOrderTemplate.getMissionCode(), securityService.getMission()));
		}

		Optional<OrderTemplate> optModelOrderTemplate = RepositoryService.getOrderTemplateRepository().findById(id);

		if (optModelOrderTemplate.isEmpty()) {
			throw new EntityNotFoundException(logger.log(OrderMgrMessage.ORDERTEMPLATE_NOT_FOUND, id));
		}
		OrderTemplate modelOrderTemplate = optModelOrderTemplate.get();
		Mission mission = modelOrderTemplate.getMission();
		logger.log(OrderMgrMessage.MODEL_ORDER_MISSIONCODE, mission.getCode());

		// Ensure mandatory attributes are set
		if (null == restOrderTemplate.getName() || restOrderTemplate.getName().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "name", "order template modification"));
		}
		if (null == restOrderTemplate.getSlicingType() || restOrderTemplate.getSlicingType().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "slicingType", "order template modification"));
		}
		if (null == restOrderTemplate.getRequestedProductClasses() || restOrderTemplate.getRequestedProductClasses().isEmpty()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "requestedProductClasses", "order template modification"));
		}
		if (null == restOrderTemplate.getOutputFileClass() || restOrderTemplate.getOutputFileClass().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "outputFileClass", "order template modification"));
		}
		if (null == restOrderTemplate.getProcessingMode() || restOrderTemplate.getProcessingMode().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "processingMode", "order template modification"));
		}

		// If list attributes were set to null explicitly, initialize with empty lists
		if (null == restOrderTemplate.getInputProductClasses()) {
			restOrderTemplate.setInputProductClasses(new ArrayList<String>());
		}
		if (null == restOrderTemplate.getInputFilters()) {
			restOrderTemplate.setInputFilters(new ArrayList<RestInputFilter>());
		}
		if (null == restOrderTemplate.getOutputParameters()) {
			restOrderTemplate.setOutputParameters(new ArrayList<RestParameter>());
		}
		if (null == restOrderTemplate.getClassOutputParameters()) {
			restOrderTemplate.setClassOutputParameters(new ArrayList<RestClassOutputParameter>());
		}
		if (null == restOrderTemplate.getConfiguredProcessors()) {
			restOrderTemplate.setConfiguredProcessors(new ArrayList<String>());
		}


		// Update modified attributes
		boolean orderChanged = false;
		OrderTemplate changedOrderTemplate = OrderTemplateUtil.toModelOrderTemplate(restOrderTemplate);

		// Mission code and UUID may not be changed
		if (!modelOrderTemplate.getMission().equals(changedOrderTemplate.getMission()))
			throw new IllegalArgumentException(
					logger.log(OrderMgrMessage.MODIFICATION_NOT_ALLOWED, "mission", modelOrderTemplate.getName()));
		
		// Modify attributes
		if (!modelOrderTemplate.getName().equals(changedOrderTemplate.getName())) {
			orderChanged = true;
			modelOrderTemplate.setName(changedOrderTemplate.getName());
		}

		if (!modelOrderTemplate.getSlicingType().equals(changedOrderTemplate.getSlicingType())) {
			orderChanged = true;
			modelOrderTemplate.setSlicingType(changedOrderTemplate.getSlicingType());
		}
		if (null == modelOrderTemplate.getSliceDuration() && null != changedOrderTemplate.getSliceDuration()
				|| null != modelOrderTemplate.getSliceDuration()
						&& !modelOrderTemplate.getSliceDuration().equals(changedOrderTemplate.getSliceDuration())) {

			orderChanged = true;
			modelOrderTemplate.setSliceDuration(changedOrderTemplate.getSliceDuration());
		}
		if (!modelOrderTemplate.getSliceOverlap().equals(changedOrderTemplate.getSliceOverlap())) {
			orderChanged = true;
			modelOrderTemplate.setSliceOverlap(changedOrderTemplate.getSliceOverlap());
		}
		if ((modelOrderTemplate.getProductRetentionPeriod() != null
				&& !modelOrderTemplate.getProductRetentionPeriod().equals(changedOrderTemplate.getProductRetentionPeriod()))
				|| (changedOrderTemplate.getProductRetentionPeriod() != null
						&& !changedOrderTemplate.getProductRetentionPeriod().equals(modelOrderTemplate.getProductRetentionPeriod()))) {
			orderChanged = true;
			modelOrderTemplate.setProductRetentionPeriod(changedOrderTemplate.getProductRetentionPeriod());
		}


		if ((modelOrderTemplate.getInputDataTimeoutPeriod() != null
				&& !modelOrderTemplate.getInputDataTimeoutPeriod().equals(changedOrderTemplate.getInputDataTimeoutPeriod()))
				|| (changedOrderTemplate.getInputDataTimeoutPeriod() != null
						&& !changedOrderTemplate.getInputDataTimeoutPeriod().equals(modelOrderTemplate.getInputDataTimeoutPeriod()))) {
			orderChanged = true;
			modelOrderTemplate.setInputDataTimeoutPeriod(changedOrderTemplate.getInputDataTimeoutPeriod());
		}
		if (modelOrderTemplate.isOnInputDataTimeoutFail() != changedOrderTemplate.isOnInputDataTimeoutFail()) {
			modelOrderTemplate.setOnInputDataTimeoutFail(changedOrderTemplate.isOnInputDataTimeoutFail());
			orderChanged = true;
		}
		if (modelOrderTemplate.isAutoRelease() != changedOrderTemplate.isAutoRelease()) {
			modelOrderTemplate.setAutoRelease(changedOrderTemplate.isAutoRelease());
			orderChanged = true;
		}
		if (modelOrderTemplate.isAutoClose() != changedOrderTemplate.isAutoClose()) {
			modelOrderTemplate.setAutoClose(changedOrderTemplate.isAutoClose());
			orderChanged = true;
		}
		if (modelOrderTemplate.isEnabled() != changedOrderTemplate.isEnabled()) {
			modelOrderTemplate.setEnabled(changedOrderTemplate.isEnabled());
			orderChanged = true;
		}
		
		// Check for changes in input filters
		Map<ProductClass, InputFilter> newInputFilters = new HashMap<>();
		if (null != restOrderTemplate.getInputFilters()) {
			for (RestInputFilter restInputFilter : restOrderTemplate.getInputFilters()) {
				if (restInputFilter != null) {
					InputFilter inputFilter = new InputFilter();
					inputFilter = RepositoryService.getInputFilterRepository().save(inputFilter);
					for (RestParameter restParam : restInputFilter.getFilterConditions()) {
						if (restParam != null) {
							Parameter modelParam = new Parameter();
							modelParam.init(ParameterType.valueOf(restParam.getParameterType()), restParam.getParameterValue());
							inputFilter.getFilterConditions().put(restParam.getKey(), modelParam);
						}
					}
					ProductClass productClass = RepositoryService.getProductClassRepository()
						.findByMissionCodeAndProductType(mission.getCode(), restInputFilter.getProductClass());
					if (null == productClass) {
						throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_INPUT_CLASS,
								restInputFilter.getProductClass(), mission.getCode()));
					}
					if (inputFilter.equals(modelOrderTemplate.getInputFilters().get(productClass))) {
						newInputFilters.put(productClass, modelOrderTemplate.getInputFilters().get(productClass));
					} else {
						orderChanged = true;
						newInputFilters.put(productClass, inputFilter);
					}
				}
			}
		}
		// Check for removed input filters
		for (ProductClass productClass : modelOrderTemplate.getInputFilters().keySet()) {
			if (null == newInputFilters.get(productClass)) {
				orderChanged = true;
			}
		}

		// Check for changes in requested output products and their parameters
		Map<ProductClass, ClassOutputParameter> newClassOutputParameters = new HashMap<>();
		for (RestClassOutputParameter restClassOutputParameter : restOrderTemplate.getClassOutputParameters()) {
			if (restClassOutputParameter != null) {
				ClassOutputParameter classOutputParameter = new ClassOutputParameter();
				classOutputParameter = RepositoryService.getClassOutputParameterRepository().save(classOutputParameter);
				for (RestParameter restParam : restClassOutputParameter.getOutputParameters()) {
					if (restParam != null) {
						Parameter modelParam = new Parameter();
						modelParam.init(ParameterType.valueOf(restParam.getParameterType()), restParam.getParameterValue());
						classOutputParameter.getOutputParameters().put(restParam.getKey(), modelParam);
					}
				}
				ProductClass productClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(mission.getCode(), restClassOutputParameter.getProductClass());
				if (null == productClass) {
					throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_OUTPUT_CLASS,
							restClassOutputParameter.getProductClass(), mission.getCode()));
				}
				if (classOutputParameter.equals(modelOrderTemplate.getClassOutputParameters().get(productClass))) {
					newClassOutputParameters.put(productClass, modelOrderTemplate.getClassOutputParameters().get(productClass));
				} else {
					orderChanged = true;
					newClassOutputParameters.put(productClass, classOutputParameter);
				}
			}
		}
		// Check for removed output parameters
		for (ProductClass productClass : modelOrderTemplate.getClassOutputParameters().keySet()) {
			if (null == newClassOutputParameters.get(productClass)) {
				orderChanged = true;
			}
		}

		// Check for new requested product classes
		Set<ProductClass> newRequestedProductClasses = new HashSet<>();
		if (null != restOrderTemplate.getRequestedProductClasses()) {
			REQUESTED_CLASSES: for (String requestedProductClass : restOrderTemplate.getRequestedProductClasses()) {
				for (ProductClass modelRequestedClass : modelOrderTemplate.getRequestedProductClasses()) {
					if (modelRequestedClass.getProductType().equals(requestedProductClass)) {
						// Already present
						newRequestedProductClasses.add(modelRequestedClass);
						continue REQUESTED_CLASSES;
					}
				}
				// New component class
				orderChanged = true;
				ProductClass newRequestedClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(restOrderTemplate.getMissionCode(), requestedProductClass);
				if (null == newRequestedClass) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_REQUESTED_CLASS, requestedProductClass, restOrderTemplate.getMissionCode()));
				}
				newRequestedProductClasses.add(newRequestedClass);
			}
		}
		if (!modelOrderTemplate.getOutputParameters().equals(changedOrderTemplate.getOutputParameters())) {
			orderChanged = true;
			modelOrderTemplate.setOutputParameters(changedOrderTemplate.getOutputParameters());
		}
		// Check for removed output products
		for (ProductClass productClass : modelOrderTemplate.getClassOutputParameters().keySet()) {
			if (null == newClassOutputParameters.get(productClass)) {
				orderChanged = true;
			}
		} // Check for removed requested product classes
		for (ProductClass modelRequestedClass : modelOrderTemplate.getRequestedProductClasses()) {
			if (!newRequestedProductClasses.contains(modelRequestedClass)) {
				// Component class removed
				orderChanged = true;
			}
		}

		// Check for new input product classes
		Set<ProductClass> newInputProductClasses = new HashSet<>();
		if (null != restOrderTemplate.getInputProductClasses()) {
			INPUT_CLASSES: for (String inputProductClass : restOrderTemplate.getInputProductClasses()) {
				for (ProductClass modelInputClass : modelOrderTemplate.getInputProductClasses()) {
					if (modelInputClass.getProductType().equals(inputProductClass)) {
						// Already present
						newInputProductClasses.add(modelInputClass);
						continue INPUT_CLASSES;
					}
				}
				// New component class
				orderChanged = true;
				ProductClass newInputClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(restOrderTemplate.getMissionCode(), inputProductClass);
				if (null == newInputClass) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_INPUT_CLASS, inputProductClass, restOrderTemplate.getMissionCode()));
				}
				newInputProductClasses.add(newInputClass);
			}
		}
		// Check for removed input product classes
		for (ProductClass modelInputClass : modelOrderTemplate.getInputProductClasses()) {
			if (!newInputProductClasses.contains(modelInputClass)) {
				// Component class removed
				orderChanged = true;
			}
		}

		if (!modelOrderTemplate.getOutputFileClass().equals(changedOrderTemplate.getOutputFileClass())) {
			if (!mission.getFileClasses().contains(changedOrderTemplate.getOutputFileClass())) {
				throw new IllegalArgumentException(
						logger.log(OrderMgrMessage.INVALID_FILE_CLASS, changedOrderTemplate.getOutputFileClass(), restOrderTemplate.getMissionCode()));
			}
			orderChanged = true;
			modelOrderTemplate.setOutputFileClass(changedOrderTemplate.getOutputFileClass());
		}
		if (!modelOrderTemplate.getProcessingMode().equals(changedOrderTemplate.getProcessingMode())) {
			if (!mission.getProcessingModes().contains(changedOrderTemplate.getProcessingMode())) {
				throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_PROCESSING_MODE,
						changedOrderTemplate.getProcessingMode(), restOrderTemplate.getMissionCode()));
			}
			orderChanged = true;
			modelOrderTemplate.setProcessingMode(changedOrderTemplate.getProcessingMode());
		}

		// Check for new configured processors
		Set<ConfiguredProcessor> newConfiguredProcessors = new HashSet<>();
		if (null != restOrderTemplate.getConfiguredProcessors()) {
			CONFIGURED_PROCESSORS: for (String changedConfiguredProcessor : restOrderTemplate.getConfiguredProcessors()) {
				for (ConfiguredProcessor modelConfiguredProcessor : modelOrderTemplate.getRequestedConfiguredProcessors()) {
					if (modelConfiguredProcessor.getIdentifier().equals(changedConfiguredProcessor)) {
						// Already present
						newConfiguredProcessors.add(modelConfiguredProcessor);
						continue CONFIGURED_PROCESSORS;
					}
				}
				// New component class
				orderChanged = true;
				ConfiguredProcessor newConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository()
					.findByMissionCodeAndIdentifier(restOrderTemplate.getMissionCode(), changedConfiguredProcessor);
				if (null == newConfiguredProcessor) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_CONFIGURED_PROCESSOR, changedConfiguredProcessor));
				}
				newConfiguredProcessors.add(newConfiguredProcessor);
			}
		}
		// Check for removed configured processors
		for (ConfiguredProcessor modelConfiguredProcessor : modelOrderTemplate.getRequestedConfiguredProcessors()) {
			if (!newConfiguredProcessors.contains(modelConfiguredProcessor)) {
				// Component class removed
				orderChanged = true;
			}
		}

		// Check for changes in dynamicProcessingParameters
		if (!modelOrderTemplate.getDynamicProcessingParameters().equals(changedOrderTemplate.getDynamicProcessingParameters())) {
			orderChanged = true;
			modelOrderTemplate.setDynamicProcessingParameters(changedOrderTemplate.getDynamicProcessingParameters());
		}

		// Check for changes in priority
		if (!modelOrderTemplate.getPriority().equals(changedOrderTemplate.getPriority())) {
			orderChanged = true;
			modelOrderTemplate.setPriority(changedOrderTemplate.getPriority());
		}

		// Check for changes in notificationEndpoint
		if (null != changedOrderTemplate.getNotificationEndpoint()
				&& !changedOrderTemplate.getNotificationEndpoint().equals(modelOrderTemplate.getNotificationEndpoint())) {
			orderChanged = true;
			modelOrderTemplate.setNotificationEndpoint(changedOrderTemplate.getNotificationEndpoint());
		} else if (null != modelOrderTemplate.getNotificationEndpoint() && null == changedOrderTemplate.getNotificationEndpoint()) {
			orderChanged = true;
			modelOrderTemplate.setNotificationEndpoint(changedOrderTemplate.getNotificationEndpoint());
		}

		// Check for forbidden order data modifications
		if (orderChanged) {
			if (!securityService.hasRole(UserRole.ORDER_MGR)) {
				throw new SecurityException(logger.log(OrderMgrMessage.ORDERTEMPLATE_MODIFICATION_FORBIDDEN, securityService.getUser()));
			}
		}

		// Save order only if anything was actually changed
		if (orderChanged) {
			modelOrderTemplate.incrementVersion();

			// Update the lists and sets
			modelOrderTemplate.getRequestedProductClasses().clear();
			modelOrderTemplate.getRequestedProductClasses().addAll(newRequestedProductClasses);
			modelOrderTemplate.getInputFilters().clear();
			modelOrderTemplate.getInputFilters().putAll(newInputFilters);
			modelOrderTemplate.getClassOutputParameters().clear();
			modelOrderTemplate.getClassOutputParameters().putAll(newClassOutputParameters);
			modelOrderTemplate.getInputProductClasses().clear();
			modelOrderTemplate.getInputProductClasses().addAll(newInputProductClasses);
			modelOrderTemplate.getRequestedConfiguredProcessors().clear();
			modelOrderTemplate.getRequestedConfiguredProcessors().addAll(newConfiguredProcessors);

			// Persist the modified order
			modelOrderTemplate = RepositoryService.getOrderTemplateRepository().save(modelOrderTemplate);
			logger.log(OrderMgrMessage.ORDERTEMPLATE_MODIFIED, id);
		} else {
			logger.log(OrderMgrMessage.ORDERTEMPLATE_NOT_MODIFIED, id);
		}
		return OrderTemplateUtil.toRestOrderTemplate(modelOrderTemplate);

	}

	/**
	 * List of all orders filtered by mission, name, product class, execution time range; selection is restricted to the
	 * mission the current user is logged in to
	 *
	 * @param mission                 the mission code
	 * @param name              the order name
	 * @param requestedProductClasses an array of product types
	 * @param startTimeFrom           earliest sensing start time
	 * @param startTimeTo             latest sensing start time
	 * @param executionTimeFrom       earliest order execution time
	 * @param executionTimeTo         latest order execution time
	 * @return a list of orders
	 * @throws NoResultException if no orders matching the given search criteria could be found
	 * @throws SecurityException if a cross-mission data access was attempted
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public List<RestOrderTemplate> getOrderTemplatesToDel(String mission, String name, String[] requestedProductClasses,
			String startTimeFrom, String startTimeTo, String executionTimeFrom,
			String executionTimeTo) throws NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getOrderTemplates({}, {}, {}, {}, {})", mission, name, requestedProductClasses, startTimeFrom,
					startTimeTo, executionTimeFrom, executionTimeTo);

		if (null == mission) {
			mission = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(mission)) {
				throw new SecurityException(
						logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, mission, securityService.getMission()));
			}
		}

		List<RestOrderTemplate> result = new ArrayList<>();

		// Find using search parameters
		String jpqlQuery = "select p from OrderTemplate p " + "join p.requestedProductClasses rpc "
				+ "where p.mission.code = :mission";
		if (null != name) {
			jpqlQuery += " and p.name = :name";
		}
		if (null != requestedProductClasses && 0 < requestedProductClasses.length) {
			jpqlQuery += " and rpc.productType in (";
			for (int i = 0; i < requestedProductClasses.length; ++i) {
				if (0 < i)
					jpqlQuery += ", ";
				jpqlQuery += ":requestedProductClasses" + i;
			}
			jpqlQuery += ")";
		}
		if (null != startTimeFrom) {
			jpqlQuery += " and p.startTime >= :startTimeFrom";
		}
		if (null != startTimeTo) {
			jpqlQuery += " and p.startTime <= :startTimeTo";
		}
		if (null != executionTimeFrom) {
			jpqlQuery += " and p.executionTime >= :executionTimeFrom";
		}
		if (null != executionTimeTo) {
			jpqlQuery += " and p.executionTime <= :executionTimeTo";
		}
		Query query = em.createQuery(jpqlQuery);
		query.setParameter("mission", mission);
		if (null != name) {
			query.setParameter("name", name);
		}
		if (null != requestedProductClasses && 0 < requestedProductClasses.length) {
			for (int i = 0; i < requestedProductClasses.length; ++i) {
				query.setParameter("requestedProductClasses" + i, requestedProductClasses[i]);
			}
		}
		for (Object resultObject : query.getResultList()) {
			if (resultObject instanceof OrderTemplate) {
				result.add(OrderTemplateUtil.toRestOrderTemplate((OrderTemplate) resultObject));
			}
		}

		if (result.isEmpty()) {
			throw new NoResultException(logger.log(OrderMgrMessage.ORDERTEMPLATE_LIST_EMPTY));

		}
		logger.log(OrderMgrMessage.ORDERTEMPLATE_LIST_RETRIEVED, result.size(), mission, name, startTimeFrom, startTimeTo);
		return result;

	}

	/**
	 * Retrieve a list of orders satisfying the selection parameters. Mission code is mandatory.
	 *
	 * @param mission                 the mission code
	 * @param name              the order name pattern
	 * @param state                   an array of states
	 * @param requestedProductClasses an array of product types
	 * @param startTimeFrom           earliest sensing start time
	 * @param startTimeTo             latest sensing start time
	 * @param recordFrom              first record of filtered and ordered result to return
	 * @param recordTo                last record of filtered and ordered result to return
	 * @param orderBy                 an array of strings containing a column name and an optional sort direction (ASC/DESC),
	 *                                separated by white space
	 *
	 * @return The result list
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public List<RestOrderTemplate> getOrderTemplates(String mission, String name, String[] requestedProductClasses,
			Long recordFrom, Long recordTo, String[] orderBy) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getOrderTemplates({}, {}, {}, {}, {}, {})", mission, name, 
					requestedProductClasses, recordFrom, recordTo, orderBy);

		if (null == mission)
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.MISSION_CODE_MISSING));

		Query query = createOrderTemplatesQuery(mission, name, requestedProductClasses, recordFrom, recordTo, orderBy, false);

		List<RestOrderTemplate> result = new ArrayList<>();
		for (Object resultObject : query.getResultList()) {
			if (resultObject instanceof OrderTemplate) {
				result.add(OrderTemplateUtil.toRestOrderTemplate((OrderTemplate) resultObject));
			}
		}

		logger.log(OrderMgrMessage.ORDERTEMPLATE_LIST_RETRIEVED, result.size(), mission, name);
		return result;
	}

	/**
	 * Calculate the amount of orders satisfying the selection parameters. Mission code is mandatory.
	 *
	 * @param mission                 the mission code
	 * @param name              the order name pattern
	 * @param state                   an array of states
	 * @param requestedProductClasses an array of product types
	 * @param startTimeFrom           earliest sensing start time
	 * @param startTimeTo             latest sensing start time
	 * @param recordFrom              first record of filtered and ordered result to return
	 * @param recordTo                last record of filtered and ordered result to return
	 * @param orderBy                 an array of strings containing a column name and an optional sort direction (ASC/DESC),
	 *
	 * @return The order count
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public String countOrderTemplates(String mission, String name, String[] requestedProductClasses,
			Long recordFrom, Long recordTo, String[] orderBy) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countOrderTemplates({}, {}, {}, {}, {}, {})", mission, name, 
					requestedProductClasses, recordFrom, recordTo, orderBy);

		if (null == mission)
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.MISSION_CODE_MISSING));

		Query query = createOrderTemplatesQuery(mission, name, requestedProductClasses, recordFrom, recordTo, orderBy, false);

		List<String> productClasses = null;
		if (requestedProductClasses != null && requestedProductClasses.length > 0) {
			productClasses = new ArrayList<>();
			for (String s : requestedProductClasses) {
				productClasses.add(s);
			}
		}
		if (recordFrom == null) {
			recordFrom = (long) 0;
		}
		if (recordTo == null) {
			recordTo = Long.MAX_VALUE;
		}
		Long i = (long) 0;
		for (Object resultObject : query.getResultList()) {
			if (resultObject instanceof OrderTemplate) {
				// Filter depending on product visibility and user authorization
				if (productClasses != null) {
					OrderTemplate orderTemplate = (OrderTemplate) resultObject;
					for (ProductClass pc : orderTemplate.getRequestedProductClasses()) {
						if (productClasses.contains(pc.getProductType())) {
							i++;
							break;
						}
					}
				} else {
					i++;
				}
			}
			if (i >= recordTo) {
				break;
			}
		}

		logger.log(OrderMgrMessage.ORDERTEMPLATE_LIST_RETRIEVED, i, mission, name);
		return i.toString();
	}

	/**
	 * Create a JPQL query to retrieve the requested set of products
	 *
	 * @param mission       the mission code
	 * @param name    the order name pattern
	 * @param state         an array of states
	 * @param productClass  an array of product types
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo   latest sensing start time
	 * @param recordFrom    first record of filtered and ordered result to return
	 * @param recordTo      last record of filtered and ordered result to return
	 * @param orderBy       an array of strings containing a column name and an optional sort direction (ASC/DESC), separated by
	 *                      white space
	 * @param count         if true, do count, otherwise retrieve
	 *
	 * @return JPQL Query
	 */
	private Query createOrderTemplatesQuery(String mission, String name, String[] requestedProductClasses,
			Long recordFrom, Long recordTo, String[] orderBy, Boolean count) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createOrderTemplatesQuery({}, {}, {}, {}, {}, {}, {})", mission, name, requestedProductClasses,
					recordFrom, recordTo, orderBy, count);

		// Find using search parameters
		String jpqlQuery = null;
		String join = "";
		if (null != requestedProductClasses && 0 < requestedProductClasses.length) {
			join = "join p.requestedProductClasses rpc ";
		}
		if (count) {
			jpqlQuery = "select count(p) from OrderTemplate p " + join + " where p.mission.code = :missionCode";
		} else {
			jpqlQuery = "select p from OrderTemplate p " + join + " where p.mission.code = :missionCode";
		}
		if (null != name) {
			jpqlQuery += " and upper(p.name) like :name";
		}
		if (null != requestedProductClasses && 0 < requestedProductClasses.length) {
			jpqlQuery += " and rpc.productType in (";
			for (int i = 0; i < requestedProductClasses.length; ++i) {
				if (0 < i)
					jpqlQuery += ", ";
				jpqlQuery += ":requestedProductClasses" + i;
			}
			jpqlQuery += ")";
		}
		// order by
		if (null != orderBy && 0 < orderBy.length) {
			jpqlQuery += " order by ";
			for (int i = 0; i < orderBy.length; ++i) {
				if (0 < i)
					jpqlQuery += ", ";
				String[] orderb = orderBy[i].split(" ");
				jpqlQuery += "p.";
				jpqlQuery += orderb[0];
				if (orderb.length > 1) {
					jpqlQuery += " ";
					jpqlQuery += orderb[1];
				}
			}
		}

		Query query = em.createQuery(jpqlQuery);
		if (null != mission) {
			query.setParameter("missionCode", mission);
		}
		if (null != name) {
			query.setParameter("name", name.toUpperCase());
		}
		if (null != requestedProductClasses && 0 < requestedProductClasses.length) {
			for (int i = 0; i < requestedProductClasses.length; ++i) {
				query.setParameter("requestedProductClasses" + i, requestedProductClasses[i]);
			}
		}

		Integer from = 0;
		if (recordFrom != null) {
			from = recordFrom.intValue();
		}
		Integer to = Integer.MAX_VALUE;
		if (recordTo != null) {
			to = recordTo.intValue();
		}
		query.setFirstResult(from);
		query.setMaxResults(to - from);

		return query;
	}

}
