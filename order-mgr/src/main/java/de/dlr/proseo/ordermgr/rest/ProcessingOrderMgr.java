/**
 * ProcessingOrderMgr.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
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

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
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
import de.dlr.proseo.model.ProcessingOrder;
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
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.model.util.OrderUtil;

/**
 * Service methods required to create, modify and delete processing order in the prosEO database, and to query the database about
 * such orders
 *
 * @author Ranjitha Vignesh
 */
@Component
public class ProcessingOrderMgr {

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
	private static ProseoLogger logger = new ProseoLogger(ProcessingOrderMgr.class);

	/**
	 * Create an order from the given Json object
	 *
	 * @param order the Json object to create the order from
	 * @return a Json object corresponding to the order after persistence (with ID and version for all contained objects)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public RestOrder createOrder(RestOrder order) throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createOrder({})", (null == order ? "MISSING" : order.getIdentifier()));

		if (null == order) {
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.ORDER_MISSING));
		}

		// Ensure user is authorized for the order mission
		if (!securityService.isAuthorizedForMission(order.getMissionCode())) {
			throw new SecurityException(
					logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, order.getMissionCode(), securityService.getMission()));
		}

		// Ensure mandatory attributes are set
		if (null == order.getIdentifier() || order.getIdentifier().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "identifier", "order creation"));
		}
		if (null == order.getOrderState() || order.getOrderState().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "orderState", "order creation"));
		}
		if (null == order.getSlicingType() || order.getSlicingType().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "slicingType", "order creation"));
		}
		if (null == order.getRequestedProductClasses() || order.getRequestedProductClasses().isEmpty()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "requestedProductClasses", "order creation"));
		}
		if (null == order.getOutputFileClass() || order.getOutputFileClass().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "outputFileClass", "order creation"));
		}
		if (null == order.getProcessingMode() || order.getProcessingMode().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "processingMode", "order creation"));
		}

		// If list attributes were set to null explicitly, initialize with empty lists
		if (null == order.getOrbits()) {
			order.setOrbits(new ArrayList<RestOrbitQuery>());
		}
		if (null == order.getInputProductClasses()) {
			order.setInputProductClasses(new ArrayList<String>());
		}
		if (null == order.getInputFilters()) {
			order.setInputFilters(new ArrayList<RestInputFilter>());
		}
		if (null == order.getOutputParameters()) {
			order.setOutputParameters(new ArrayList<RestParameter>());
		}
		if (null == order.getClassOutputParameters()) {
			order.setClassOutputParameters(new ArrayList<RestClassOutputParameter>());
		}
		if (null == order.getConfiguredProcessors()) {
			order.setConfiguredProcessors(new ArrayList<String>());
		}
		if (null == order.getJobStepStates()) {
			order.setJobStepStates(new ArrayList<String>());
		}

		// Prepare the database order, but make sure ID and version are not copied if present
		order.setId(null);
		order.setVersion(null);

		ProcessingOrder modelOrder = OrderUtil.toModelOrder(order);

		// The mission must be set
		if (null == modelOrder.getMission()) {
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.MISSION_CODE_MISSING));
		}

		try {

			Mission mission = RepositoryService.getMissionRepository().findByCode(modelOrder.getMission().getCode());

			// Make sure order has a UUID
			if (null == modelOrder.getUuid() || modelOrder.getUuid().toString().isEmpty()) {
				modelOrder.setUuid(UUID.randomUUID());
			} else {
				// Test if given UUID is not yet in use
				if (null != RepositoryService.getOrderRepository().findByUuid(modelOrder.getUuid())) {
					throw new IllegalArgumentException(logger.log(OrderMgrMessage.DUPLICATE_ORDER_UUID, modelOrder.getUuid()));
				}
			}

			// Make sure order identifier is not yet in use
			if (null != RepositoryService.getOrderRepository()
				.findByMissionCodeAndIdentifier(order.getMissionCode(), modelOrder.getIdentifier())) {
				throw new IllegalArgumentException(
						logger.log(OrderMgrMessage.DUPLICATE_ORDER_IDENTIFIER, modelOrder.getIdentifier(), order.getMissionCode()));
			}

			// Orders must always be created in state INITIAL
			if (!OrderState.INITIAL.equals(modelOrder.getOrderState())) {
				throw new IllegalArgumentException(
						logger.log(OrderMgrMessage.ILLEGAL_CREATION_STATE, modelOrder.getOrderState().toString()));
			}

			// Identify the order time interval, either by orbit range queries if given, or by start and stop time
			if (order.getOrbits().isEmpty()) {
				if (null == modelOrder.getStartTime() || null == modelOrder.getStopTime()) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.ORDER_TIME_INTERVAL_MISSING, modelOrder.getIdentifier()));
				}
				// Ensure stop time is not before start time
				if (modelOrder.getStopTime().isBefore(modelOrder.getStartTime())) {
					throw new IllegalArgumentException(logger.log(OrderMgrMessage.NEGATIVE_DURATION, modelOrder.getIdentifier(),
							OrbitTimeFormatter.format(modelOrder.getStartTime()),
							OrbitTimeFormatter.format(modelOrder.getStopTime())));
				}
				// Ensure slice duration is given for slicing type TIME_SLICE
				if (OrderSlicingType.TIME_SLICE.equals(modelOrder.getSlicingType())
						&& (null == modelOrder.getSliceDuration() || modelOrder.getSliceDuration().isZero())) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.SLICE_DURATION_MISSING, modelOrder.getIdentifier()));
				}
				/*
				 * Setting a slice duration for slicing types other than TIME_SLICE or setting a slice overlap in case of slicing
				 * type NONE will prevented by the ProcessingOrder class.
				 */
			} else {
				// Find all requested orbit ranges
				modelOrder.getRequestedOrbits().clear();
				for (RestOrbitQuery orbitQuery : order.getOrbits()) {
					List<Orbit> orbit = RepositoryService.getOrbitRepository()
						.findByMissionCodeAndSpacecraftCodeAndOrbitNumberBetween(mission.getCode(), orbitQuery.getSpacecraftCode(),
								orbitQuery.getOrbitNumberFrom().intValue(), orbitQuery.getOrbitNumberTo().intValue());
					if (orbit.isEmpty()) {
						throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_ORBIT_RANGE,
								orbitQuery.getOrbitNumberFrom(), orbitQuery.getOrbitNumberTo(), orbitQuery.getSpacecraftCode()));
					}
					modelOrder.getRequestedOrbits().addAll(orbit);
				}
				// Set start and stop time from requested orbits
				Orbit minOrbit = Collections.min(modelOrder.getRequestedOrbits(), (o1, o2) -> {
					return o1.getStartTime().compareTo(o2.getStartTime());
				});
				Orbit maxOrbit = Collections.max(modelOrder.getRequestedOrbits(), (o1, o2) -> {
					return o1.getStopTime().compareTo(o2.getStopTime());
				});
				modelOrder.setStartTime(minOrbit.getStartTime());
				modelOrder.setStopTime(maxOrbit.getStopTime());
			}

			// Create input filters
			for (RestInputFilter restInputFilter : order.getInputFilters()) {
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
				modelOrder.getInputFilters().put(productClass, inputFilter);
			}

			// Create class output parameters
			for (RestClassOutputParameter restClassOutputParameter : order.getClassOutputParameters()) {
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
				modelOrder.getClassOutputParameters().put(productClass, classOutputParameter);
			}

			// Find requested product classes
			modelOrder.getRequestedProductClasses().clear();
			for (String productType : order.getRequestedProductClasses()) {
				ProductClass productClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(mission.getCode(), productType);
				if (null == productClass) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_REQUESTED_CLASS, productType, mission.getCode()));
				}
				modelOrder.getRequestedProductClasses().add(productClass);
			}

			// Find input product classes
			modelOrder.getInputProductClasses().clear();
			for (String productType : order.getInputProductClasses()) {
				ProductClass productClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(mission.getCode(), productType);
				if (null == productClass) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_INPUT_CLASS, productType, mission.getCode()));
				}
				modelOrder.getInputProductClasses().add(productClass);
			}

			// Retrieve workflow if specified and check consistency if over-specified
			if (null != order.getWorkflowUuid()) {
				Workflow workflow = RepositoryService.getWorkflowRepository().findByUuid(UUID.fromString(order.getWorkflowUuid()));
				if (null == workflow)
					throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_WORKFLOW_UUID, order.getWorkflowUuid()));
				if (null != order.getWorkflowName() && !workflow.getName().equals(order.getWorkflowName())) {
					throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_WORKFLOW_SPECIFICATION,
							order.getWorkflowName(), order.getWorkflowUuid()));
				}
				modelOrder.setWorkflow(workflow);
			}

			// Find requested configured processors
			modelOrder.getRequestedConfiguredProcessors().clear();
			for (String identifier : order.getConfiguredProcessors()) {
				ConfiguredProcessor configuredProcessor = RepositoryService.getConfiguredProcessorRepository()
					.findByMissionCodeAndIdentifier(order.getMissionCode(), identifier);
				if (null == configuredProcessor) {
					throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_CONFIGURED_PROCESSOR, identifier));
				}
				modelOrder.getRequestedConfiguredProcessors().add(configuredProcessor);
			}

			// Make sure processing mode and file class are OK
			if (!mission.getProcessingModes().contains(order.getProcessingMode())) {
				throw new IllegalArgumentException(
						logger.log(OrderMgrMessage.INVALID_PROCESSING_MODE, order.getProcessingMode(), mission.getCode()));
			}
			if (!mission.getFileClasses().contains(order.getOutputFileClass())) {
				throw new IllegalArgumentException(
						logger.log(OrderMgrMessage.INVALID_FILE_CLASS, order.getOutputFileClass(), mission.getCode()));
			}

			// Everything OK, store new order in database
			modelOrder = RepositoryService.getOrderRepository().save(modelOrder);
			logger.log(OrderMgrMessage.ORDER_CREATED, order.getIdentifier(), order.getMissionCode());

			return OrderUtil.toRestOrder(modelOrder);

		} catch (org.springframework.dao.DataIntegrityViolationException e) {

			if (null == RepositoryService.getMissionRepository().findByCode(modelOrder.getMission().getCode()))
				throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_MISSION_CODE, order.getMissionCode()));

			throw e;
		}
	}

	/**
	 * Delete the Job Order file for the given job step from the Storage Manager
	 *
	 * @param js the job step to delete the JOF from
	 * @return true on success, false otherwise
	 */
	private Boolean deleteJOF(JobStep js) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteJOF({})", (null == js ? "null" : js.getId()));

		if (js != null && js.getJobOrderFilename() != null) {
			ProcessingFacility facility = js.getJob().getProcessingFacility();
			String storageManagerUrl = facility.getStorageManagerUrl()
					+ String.format("/products?pathInfo=%s", js.getJobOrderFilename());

			RestTemplate restTemplate = rtb
				.basicAuthentication(facility.getStorageManagerUser(), facility.getStorageManagerPassword())
				.build();
			try {
				restTemplate.delete(storageManagerUrl);
				logger.log(OrderMgrMessage.JOF_DELETED, js.getJobOrderFilename(), facility.getName());
				return true;
			} catch (RestClientException e) {
				logger.log(OrderMgrMessage.JOF_DELETING_ERROR, js.getJobOrderFilename(), facility.getName(), e.getMessage());
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Prepare the order for deletion: remove dependencies to products and product queries.
	 *
	 * @param order the order to prepare
	 */
	private void prepareOrderToDelete(ProcessingOrder order) {
		if (logger.isTraceEnabled())
			logger.trace(">>> prepareOrderToDelete({})", order.getIdentifier());

		if (order != null) {
			for (Job j : order.getJobs()) {
				for (JobStep js : j.getJobSteps()) {

					deleteJOF(js);

					js.setJobOrderFilename(null);
					if (js.getOutputProduct() != null) {
						js.getOutputProduct().setJobStep(null);
					}

					for (ProductQuery pq : js.getInputProductQueries()) {
						for (Product p : pq.getSatisfyingProducts()) {
							p.getSatisfiedProductQueries().clear();
						}
						pq.getSatisfyingProducts().clear();
						RepositoryService.getProductQueryRepository().delete(pq);
					}
					js.getInputProductQueries().clear();
				}
			}
		}
	}

	/**
	 * Delete an order by entity
	 *
	 * @param order the order to delete
	 * @throws EntityNotFoundException if the order to delete does not exist in the database
	 * @throws RuntimeException        if the deletion was not performed as expected
	 */
	private void deleteOrder(ProcessingOrder order) throws EntityNotFoundException, RuntimeException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteOrder({})", order.getIdentifier());

		// Prepare the order to delete
		prepareOrderToDelete(order);
		// Delete the order
		long id = order.getId();
		RepositoryService.getOrderRepository().delete(order);
		// Test whether the deletion was successful
		Optional<ProcessingOrder> modelOrder = RepositoryService.getOrderRepository().findById(id);
		if (!modelOrder.isEmpty()) {
			throw new RuntimeException(logger.log(OrderMgrMessage.DELETION_UNSUCCESSFUL, id));
		}

		logger.log(OrderMgrMessage.ORDER_DELETED, id);
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
	public void deleteOrderById(Long id) throws EntityNotFoundException, SecurityException, RuntimeException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteOrderById({})", id);

		// Test whether the order id is valid
		Optional<ProcessingOrder> modelOrder = RepositoryService.getOrderRepository().findById(id);
		if (modelOrder.isEmpty()) {
			throw new EntityNotFoundException(logger.log(OrderMgrMessage.ORDER_NOT_FOUND, id));
		}

		// Ensure user is authorized for the order mission
		if (!securityService.isAuthorizedForMission(modelOrder.get().getMission().getCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					modelOrder.get().getMission().getCode(), securityService.getMission()));
		}
		deleteOrder(modelOrder.get());
	}

	/**
	 * Delete an expired order by ID without cross-mission access check
	 *
	 * @param id           the ID of the order to delete
	 * @param evictionTime the relevant cutoff time for the eviction of orders
	 * @throws EntityNotFoundException if the order to delete does not exist in the database
	 * @throws SecurityException       if a cross-mission data access was attempted
	 * @throws RuntimeException        if the deletion was not performed as expected
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void deleteExpiredOrderById(Long id, Instant evictionTime)
			throws EntityNotFoundException, SecurityException, RuntimeException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteExpiredOrderById({}, {})", id, evictionTime);

		// Test whether the order id is valid
		Optional<ProcessingOrder> modelOrder = RepositoryService.getOrderRepository().findById(id);
		if (modelOrder.isEmpty()) {
			throw new EntityNotFoundException(logger.log(OrderMgrMessage.ORDER_NOT_FOUND));
		}

		// Ensure order eviction time is actually before relevant cutoff time
		ProcessingOrder order = modelOrder.get();
		if (order.getEvictionTime().isBefore(evictionTime)) {
			deleteOrder(order);
		} else {
			logger.log(OrderMgrMessage.ORDER_NOT_EVICTABLE, order.getId(), order.getEvictionTime(), evictionTime);
		}
	}

	/**
	 * Find all orders of state CLOSED and eviction time less than t and delete them
	 *
	 * @param t the time to compare to
	 */
	@Deprecated
	public void deleteOrdersWithEvictionTimeLessThan(Instant t) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteOrdersWithEvictionTimeLessThan({})", t);
		List<ProcessingOrder> orders = RepositoryService.getOrderRepository()
			.findByOrderStateAndEvictionTimeLessThan(OrderState.CLOSED, t);
		long ordersDeleted = 0;
		for (ProcessingOrder po : orders) {
			try {
				deleteOrder(po);
			}
			// ignore known exceptions cause already logged
			catch (EntityNotFoundException e) {
				break;
			} catch (ProcessingException e) {
				break;
			} catch (IllegalArgumentException e) {
				break;
			} catch (RuntimeException e) {
				break;
			}
			ordersDeleted++;
		}
		logger.log(OrderMgrMessage.NUMBER_ORDERS_DELETED, ordersDeleted);
	}

	/**
	 * Find all orders of state CLOSED and eviction time less than t and return a list of their DB IDs
	 *
	 * @param evictionTime the time to compare to
	 * @return a list of database IDs for evictable orders
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public List<Long> findOrdersWithEvictionTimeLessThan(Instant evictionTime) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findOrdersWithEvictionTimeLessThan({})", evictionTime);

		return RepositoryService.getOrderRepository().findIdsByOrderStateAndEvictionTimeLessThan(OrderState.CLOSED, evictionTime);
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
	public RestOrder getOrderById(Long id) throws IllegalArgumentException, NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getOrderById({})", id);

		if (null == id) {
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.ORDER_MISSING, id));
		}
		if (id == 0) {
			// new order from "scratch", used at least if GUI
			// TODO Check if this should be moved to GUI (at least partially) or removed
			// altogether
			// Having id == 0 is contrary to the interface contract, which requires a valid
			// object database ID
			// Furthermore default values shall not deviate from the default values given in
			// the UML model
			RestOrder newOrder = new RestOrder();
			newOrder.setIdentifier("New");
			ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"));
			Calendar cal = GregorianCalendar.from(zdt);
			newOrder.setStartTime(OrbitTimeFormatter.format(cal.toInstant()));
			cal.add(Calendar.SECOND, 1);
			newOrder.setStopTime(OrbitTimeFormatter.format(cal.toInstant()));
			return newOrder;
		} else {
			Optional<ProcessingOrder> modelOrder = RepositoryService.getOrderRepository().findById(id);

			if (modelOrder.isEmpty()) {
				throw new NoResultException(logger.log(OrderMgrMessage.ORDER_NOT_FOUND, id));
			}

			// Ensure user is authorized for the order mission
			if (!securityService.isAuthorizedForMission(modelOrder.get().getMission().getCode())) {
				throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
						modelOrder.get().getMission().getCode(), securityService.getMission()));
			}

			logger.log(OrderMgrMessage.ORDER_RETRIEVED, id);

			return OrderUtil.toRestOrder(modelOrder.get());
		}
	}

	/**
	 * Update the order with the given ID with the attribute values of the given Json object. Orders may only be changed while they
	 * are in state "INITIAL". The only state modification allowed here is from INITIAL to APPROVED.
	 *
	 * @param id    the ID of the product to update
	 * @param order a Json object containing the modified (and unmodified) attributes
	 * @return a Json object corresponding to the product after modification (with ID and version for all contained objects)
	 * @throws EntityNotFoundException         if no product with the given ID exists
	 * @throws IllegalArgumentException        if any of the input data was invalid
	 * @throws SecurityException               if a cross-mission data access was attempted
	 * @throws ConcurrentModificationException if the order has been modified since retrieval by the client
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public RestOrder modifyOrder(Long id, RestOrder order)
			throws EntityNotFoundException, IllegalArgumentException, SecurityException, ConcurrentModificationException {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyOrder({})", id);

		if (null == id) {
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.ORDER_MISSING, id));
		}

		// Ensure user is authorized for the order mission
		if (!securityService.isAuthorizedForMission(order.getMissionCode())) {
			throw new SecurityException(
					logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, order.getMissionCode(), securityService.getMission()));
		}

		Optional<ProcessingOrder> optModelOrder = RepositoryService.getOrderRepository().findById(id);

		if (optModelOrder.isEmpty()) {
			throw new EntityNotFoundException(logger.log(OrderMgrMessage.ORDER_NOT_FOUND, id));
		}
		ProcessingOrder modelOrder = optModelOrder.get();
		Mission mission = modelOrder.getMission();
		logger.log(OrderMgrMessage.MODEL_ORDER_MISSIONCODE, mission.getCode());

		// Ensure mandatory attributes are set
		if (null == order.getIdentifier() || order.getIdentifier().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "identifier", "order modification"));
		}
		if (null == order.getOrderState() || order.getOrderState().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "orderState", "order modification"));
		}
		if (null == order.getSlicingType() || order.getSlicingType().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "slicingType", "order modification"));
		}
		if (null == order.getRequestedProductClasses() || order.getRequestedProductClasses().isEmpty()) {
			throw new IllegalArgumentException(
					logger.log(GeneralMessage.FIELD_NOT_SET, "requestedProductClasses", "order modification"));
		}
		if (null == order.getOutputFileClass() || order.getOutputFileClass().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "outputFileClass", "order modification"));
		}
		if (null == order.getProcessingMode() || order.getProcessingMode().isBlank()) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.FIELD_NOT_SET, "processingMode", "order modification"));
		}

		// If list attributes were set to null explicitly, initialize with empty lists
		if (null == order.getOrbits()) {
			order.setOrbits(new ArrayList<RestOrbitQuery>());
		}
		if (null == order.getInputProductClasses()) {
			order.setInputProductClasses(new ArrayList<String>());
		}
		if (null == order.getInputFilters()) {
			order.setInputFilters(new ArrayList<RestInputFilter>());
		}
		if (null == order.getOutputParameters()) {
			order.setOutputParameters(new ArrayList<RestParameter>());
		}
		if (null == order.getClassOutputParameters()) {
			order.setClassOutputParameters(new ArrayList<RestClassOutputParameter>());
		}
		if (null == order.getConfiguredProcessors()) {
			order.setConfiguredProcessors(new ArrayList<String>());
		}
		if (null == order.getJobStepStates()) {
			order.setJobStepStates(new ArrayList<String>());
		}

		// Make sure order is in INITIAL state
		if (!OrderState.INITIAL.equals(modelOrder.getOrderState())) {
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.ILLEGAL_ORDER_STATE));
		}

		// Update modified attributes
		boolean orderChanged = false;
		boolean stateChangeOnly = true;
		ProcessingOrder changedOrder = OrderUtil.toModelOrder(order);

		// Mission code and UUID cannot be changed
		if (!modelOrder.getMission().equals(changedOrder.getMission()))
			throw new IllegalArgumentException(
					logger.log(OrderMgrMessage.MODIFICATION_NOT_ALLOWED, "mission", modelOrder.getIdentifier()));
		if (!modelOrder.getUuid().equals(changedOrder.getUuid()))
			throw new IllegalArgumentException(
					logger.log(OrderMgrMessage.MODIFICATION_NOT_ALLOWED, "UUID", modelOrder.getIdentifier()));

		// ODIP: Workflow and input product reference may not be changed
		if (null != modelOrder.getInputProductReference()
				&& !modelOrder.getInputProductReference().equals(changedOrder.getInputProductReference()))
			throw new IllegalArgumentException(
					logger.log(OrderMgrMessage.MODIFICATION_NOT_ALLOWED, "input product reference", modelOrder.getIdentifier()));

		if (null == modelOrder.getWorkflow() && ((null != order.getWorkflowName()) || (null != order.getWorkflowUuid())))
			throw new IllegalArgumentException(
					logger.log(OrderMgrMessage.MODIFICATION_NOT_ALLOWED, "workflow", modelOrder.getIdentifier()));

		if (null != modelOrder.getWorkflow() && (order.getWorkflowName() != modelOrder.getWorkflow().getName()
				|| order.getWorkflowUuid() != modelOrder.getWorkflow().getUuid().toString()))
			throw new IllegalArgumentException(
					logger.log(OrderMgrMessage.MODIFICATION_NOT_ALLOWED, "workflow", modelOrder.getIdentifier()));

	
		if (!modelOrder.getIdentifier().equals(changedOrder.getIdentifier())) {
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setIdentifier(changedOrder.getIdentifier());
		}
		if (!modelOrder.getOrderSource().equals(changedOrder.getOrderSource())) {
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setOrderSource(changedOrder.getOrderSource());
		}
		if (!modelOrder.getOrderState().equals(changedOrder.getOrderState())) {
			orderChanged = true;

			// Check whether the requested state change (if any) is allowed and the user is authorized for it
			if (OrderState.APPROVED.equals(changedOrder.getOrderState()) && !securityService.hasRole(UserRole.ORDER_APPROVER)) {
				throw new SecurityException(logger.log(OrderMgrMessage.STATE_TRANSITION_FORBIDDEN,
						modelOrder.getOrderState().toString(), changedOrder.getOrderState().toString(), securityService.getUser()));
			}

			try {
				modelOrder.setOrderState(changedOrder.getOrderState());
			} catch (IllegalStateException e) {
				throw new IllegalArgumentException(logger.log(OrderMgrMessage.ILLEGAL_STATE_TRANSITION,
						modelOrder.getOrderState().toString(), changedOrder.getOrderState().toString()));
			}
		}
		if ((null == modelOrder.getExecutionTime() && null != changedOrder.getExecutionTime())
				|| null != modelOrder.getExecutionTime()
						&& !modelOrder.getExecutionTime().equals(changedOrder.getExecutionTime())) {
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setExecutionTime(changedOrder.getExecutionTime());
		}
		if ((null == modelOrder.getEvictionTime() && null != changedOrder.getEvictionTime())
				|| null != modelOrder.getEvictionTime() && !modelOrder.getEvictionTime().equals(changedOrder.getEvictionTime())) {
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setEvictionTime(changedOrder.getEvictionTime());
		}
		if (!changedOrder.getSlicingType().equals(OrderSlicingType.ORBIT)) {
			// use start/stop time only for time slicing. For Orbits it is set below.
			if (modelOrder.getStartTime() == null) {
				if (changedOrder.getStartTime() != null) {
					orderChanged = true;
					stateChangeOnly = false;
					modelOrder.setStartTime(changedOrder.getStartTime());
				}
			} else {
				if (!modelOrder.getStartTime().equals(changedOrder.getStartTime())) {
					orderChanged = true;
					stateChangeOnly = false;
					modelOrder.setStartTime(changedOrder.getStartTime());
				}
			}
			if (modelOrder.getStopTime() == null) {
				if (changedOrder.getStopTime() != null) {
					orderChanged = true;
					stateChangeOnly = false;
					modelOrder.setStopTime(changedOrder.getStopTime());
				}
			} else {
				if (!modelOrder.getStopTime().equals(changedOrder.getStopTime())) {
					orderChanged = true;
					stateChangeOnly = false;
					modelOrder.setStopTime(changedOrder.getStopTime());
				}
			}
			// Ensure stop time is not before start time
			if (modelOrder.getStopTime().isBefore(modelOrder.getStartTime())) {
				throw new IllegalArgumentException(logger.log(OrderMgrMessage.NEGATIVE_DURATION, modelOrder.getIdentifier(),
						OrbitTimeFormatter.format(modelOrder.getStartTime()), OrbitTimeFormatter.format(modelOrder.getStopTime())));
			}
		}
		if (!modelOrder.getSlicingType().equals(changedOrder.getSlicingType())) {
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setSlicingType(changedOrder.getSlicingType());
		}
		if (null == modelOrder.getSliceDuration() && null != changedOrder.getSliceDuration()
				|| null != modelOrder.getSliceDuration()
						&& !modelOrder.getSliceDuration().equals(changedOrder.getSliceDuration())) {

			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setSliceDuration(changedOrder.getSliceDuration());
		}
		if (!modelOrder.getSliceOverlap().equals(changedOrder.getSliceOverlap())) {
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setSliceOverlap(changedOrder.getSliceOverlap());
		}
		if (!modelOrder.getProductionType().equals(changedOrder.getProductionType())) {
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setProductionType(changedOrder.getProductionType());
		}
		if ((modelOrder.getProductRetentionPeriod() != null
				&& !modelOrder.getProductRetentionPeriod().equals(changedOrder.getProductRetentionPeriod()))
				|| (changedOrder.getProductRetentionPeriod() != null
						&& !changedOrder.getProductRetentionPeriod().equals(modelOrder.getProductRetentionPeriod()))) {
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setProductRetentionPeriod(changedOrder.getProductRetentionPeriod());
		}
		if (!modelOrder.getHasFailedJobSteps().equals(changedOrder.getHasFailedJobSteps())) {
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setHasFailedJobSteps(changedOrder.getHasFailedJobSteps());
		}

		// Check for changes in input filters
		Map<ProductClass, InputFilter> newInputFilters = new HashMap<>();
		if (null != order.getInputFilters()) {
			for (RestInputFilter restInputFilter : order.getInputFilters()) {
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
					if (inputFilter.equals(modelOrder.getInputFilters().get(productClass))) {
						newInputFilters.put(productClass, modelOrder.getInputFilters().get(productClass));
					} else {
						orderChanged = true;
						stateChangeOnly = false;
						newInputFilters.put(productClass, inputFilter);
					}
				}
			}
		}
		// Check for removed input filters
		for (ProductClass productClass : modelOrder.getInputFilters().keySet()) {
			if (null == newInputFilters.get(productClass)) {
				orderChanged = true;
				stateChangeOnly = false;
			}
		}

		// Check for changes in requested output products and their parameters
		Map<ProductClass, ClassOutputParameter> newClassOutputParameters = new HashMap<>();
		for (RestClassOutputParameter restClassOutputParameter : order.getClassOutputParameters()) {
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
				if (classOutputParameter.equals(modelOrder.getClassOutputParameters().get(productClass))) {
					newClassOutputParameters.put(productClass, modelOrder.getClassOutputParameters().get(productClass));
				} else {
					orderChanged = true;
					stateChangeOnly = false;
					newClassOutputParameters.put(productClass, classOutputParameter);
				}
			}
		}
		// Check for removed output parameters
		for (ProductClass productClass : modelOrder.getClassOutputParameters().keySet()) {
			if (null == newClassOutputParameters.get(productClass)) {
				orderChanged = true;
				stateChangeOnly = false;
			}
		}

		// Check for new requested product classes
		Set<ProductClass> newRequestedProductClasses = new HashSet<>();
		if (null != order.getRequestedProductClasses()) {
			REQUESTED_CLASSES: for (String requestedProductClass : order.getRequestedProductClasses()) {
				for (ProductClass modelRequestedClass : modelOrder.getRequestedProductClasses()) {
					if (modelRequestedClass.getProductType().equals(requestedProductClass)) {
						// Already present
						newRequestedProductClasses.add(modelRequestedClass);
						continue REQUESTED_CLASSES;
					}
				}
				// New component class
				orderChanged = true;
				stateChangeOnly = false;
				ProductClass newRequestedClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(order.getMissionCode(), requestedProductClass);
				if (null == newRequestedClass) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_REQUESTED_CLASS, requestedProductClass, order.getMissionCode()));
				}
				newRequestedProductClasses.add(newRequestedClass);
			}
		}
		if (!modelOrder.getOutputParameters().equals(changedOrder.getOutputParameters())) {
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setOutputParameters(changedOrder.getOutputParameters());
		}
		// Check for removed output products
		for (ProductClass productClass : modelOrder.getClassOutputParameters().keySet()) {
			if (null == newClassOutputParameters.get(productClass)) {
				orderChanged = true;
				stateChangeOnly = false;
			}
		} // Check for removed requested product classes
		for (ProductClass modelRequestedClass : modelOrder.getRequestedProductClasses()) {
			if (!newRequestedProductClasses.contains(modelRequestedClass)) {
				// Component class removed
				orderChanged = true;
				stateChangeOnly = false;
			}
		}

		// Check for new input product classes
		Set<ProductClass> newInputProductClasses = new HashSet<>();
		if (null != order.getInputProductClasses()) {
			INPUT_CLASSES: for (String inputProductClass : order.getInputProductClasses()) {
				for (ProductClass modelInputClass : modelOrder.getInputProductClasses()) {
					if (modelInputClass.getProductType().equals(inputProductClass)) {
						// Already present
						newInputProductClasses.add(modelInputClass);
						continue INPUT_CLASSES;
					}
				}
				// New component class
				orderChanged = true;
				stateChangeOnly = false;
				ProductClass newInputClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(order.getMissionCode(), inputProductClass);
				if (null == newInputClass) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_INPUT_CLASS, inputProductClass, order.getMissionCode()));
				}
				newInputProductClasses.add(newInputClass);
			}
		}
		// Check for removed input product classes
		for (ProductClass modelInputClass : modelOrder.getInputProductClasses()) {
			if (!newInputProductClasses.contains(modelInputClass)) {
				// Component class removed
				orderChanged = true;
				stateChangeOnly = false;
			}
		}

		if (!modelOrder.getOutputFileClass().equals(changedOrder.getOutputFileClass())) {
			if (!mission.getFileClasses().contains(changedOrder.getOutputFileClass())) {
				throw new IllegalArgumentException(
						logger.log(OrderMgrMessage.INVALID_FILE_CLASS, changedOrder.getOutputFileClass(), order.getMissionCode()));
			}
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setOutputFileClass(changedOrder.getOutputFileClass());
		}
		if (!modelOrder.getProcessingMode().equals(changedOrder.getProcessingMode())) {
			if (!mission.getProcessingModes().contains(changedOrder.getProcessingMode())) {
				throw new IllegalArgumentException(logger.log(OrderMgrMessage.INVALID_PROCESSING_MODE,
						changedOrder.getProcessingMode(), order.getMissionCode()));
			}
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setProcessingMode(changedOrder.getProcessingMode());
		}

		// Check for new configured processors
		Set<ConfiguredProcessor> newConfiguredProcessors = new HashSet<>();
		if (null != order.getConfiguredProcessors()) {
			CONFIGURED_PROCESSORS: for (String changedConfiguredProcessor : order.getConfiguredProcessors()) {
				for (ConfiguredProcessor modelConfiguredProcessor : modelOrder.getRequestedConfiguredProcessors()) {
					if (modelConfiguredProcessor.getIdentifier().equals(changedConfiguredProcessor)) {
						// Already present
						newConfiguredProcessors.add(modelConfiguredProcessor);
						continue CONFIGURED_PROCESSORS;
					}
				}
				// New component class
				orderChanged = true;
				stateChangeOnly = false;
				ConfiguredProcessor newConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository()
					.findByMissionCodeAndIdentifier(order.getMissionCode(), changedConfiguredProcessor);
				if (null == newConfiguredProcessor) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_CONFIGURED_PROCESSOR, changedConfiguredProcessor));
				}
				newConfiguredProcessors.add(newConfiguredProcessor);
			}
		}
		// Check for removed configured processors
		for (ConfiguredProcessor modelConfiguredProcessor : modelOrder.getRequestedConfiguredProcessors()) {
			if (!newConfiguredProcessors.contains(modelConfiguredProcessor)) {
				// Component class removed
				orderChanged = true;
				stateChangeOnly = false;
			}
		}

		// Check for new requested orbits
		List<Orbit> newRequestedOrbits = new ArrayList<>();
		if (null != order.getOrbits()) {
			for (RestOrbitQuery changedOrbitQuery : order.getOrbits()) {
				if (null == changedOrbitQuery.getSpacecraftCode() || null == changedOrbitQuery.getOrbitNumberFrom()
						|| null == changedOrbitQuery.getOrbitNumberTo())
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_ORBIT_RANGE, changedOrbitQuery.getOrbitNumberFrom(),
									changedOrbitQuery.getOrbitNumberTo(), changedOrbitQuery.getSpacecraftCode()));
				List<Orbit> changedRequestedOrbits = RepositoryService.getOrbitRepository()
					.findByMissionCodeAndSpacecraftCodeAndOrbitNumberBetween(mission.getCode(),
							changedOrbitQuery.getSpacecraftCode(), changedOrbitQuery.getOrbitNumberFrom().intValue(),
							changedOrbitQuery.getOrbitNumberTo().intValue());
				if (changedRequestedOrbits.isEmpty()) {
					throw new IllegalArgumentException(
							logger.log(OrderMgrMessage.INVALID_ORBIT_RANGE, changedOrbitQuery.getOrbitNumberFrom(),
									changedOrbitQuery.getOrbitNumberTo(), changedOrbitQuery.getSpacecraftCode()));
				}
				Instant startTime = null;
				Instant stopTime = null;
				for (Orbit changedRequestedOrbit : changedRequestedOrbits) {
					if (startTime == null) {
						startTime = changedRequestedOrbit.getStartTime();
					} else {
						if (startTime.isAfter(changedRequestedOrbit.getStartTime())) {
							startTime = changedRequestedOrbit.getStartTime();
						}
					}
					if (stopTime == null) {
						stopTime = changedRequestedOrbit.getStopTime();
					} else {
						if (stopTime.isBefore(changedRequestedOrbit.getStopTime())) {
							stopTime = changedRequestedOrbit.getStopTime();
						}
					}
					if (!modelOrder.getRequestedOrbits().contains(changedRequestedOrbit)) {
						// New orbit
						orderChanged = true;
						stateChangeOnly = false;
					}
					newRequestedOrbits.add(changedRequestedOrbit);
				}
				if (!startTime.equals(modelOrder.getStartTime())) {
					modelOrder.setStartTime(startTime);
					orderChanged = true;
					stateChangeOnly = false;
				}
				if (!stopTime.equals(modelOrder.getStopTime())) {
					modelOrder.setStopTime(stopTime);
					orderChanged = true;
					stateChangeOnly = false;
				}
			}
		}
		// Check for removed requested orbits
		for (Orbit modelRequestedOrbit : modelOrder.getRequestedOrbits()) {
			if (!newRequestedOrbits.contains(modelRequestedOrbit)) {
				// Orbit removed
				orderChanged = true;
				stateChangeOnly = false;
			}
		}

		// Check for changes in dynamicProcessingParameters
		if (!modelOrder.getDynamicProcessingParameters().equals(changedOrder.getDynamicProcessingParameters())) {
			orderChanged = true;
			stateChangeOnly = false;
			modelOrder.setDynamicProcessingParameters(changedOrder.getDynamicProcessingParameters());
		}

		// Check for changes in priority
		if (!modelOrder.getPriority().equals(changedOrder.getPriority())) {
			orderChanged = true;
			stateChangeOnly = false;
			changedOrder.setPriority(changedOrder.getPriority());
		}

		// Check for changes in notificationEndpoint
		if (null != modelOrder.getNotificationEndpoint()
				&& !modelOrder.getNotificationEndpoint().equals(changedOrder.getNotificationEndpoint())) {
			orderChanged = true;
			stateChangeOnly = false;
			changedOrder.setNotificationEndpoint(changedOrder.getNotificationEndpoint());
		}

		// Check for forbidden order data modifications
		if (orderChanged && !stateChangeOnly) {
			if (!securityService.hasRole(UserRole.ORDER_MGR)) {
				throw new SecurityException(logger.log(OrderMgrMessage.ORDER_MODIFICATION_FORBIDDEN, securityService.getUser()));
			}
		}

		// Save order only if anything was actually changed
		if (orderChanged) {
			modelOrder.incrementVersion();

			// Update the lists and sets
			modelOrder.getRequestedProductClasses().clear();
			modelOrder.getRequestedProductClasses().addAll(newRequestedProductClasses);
			modelOrder.getInputFilters().clear();
			modelOrder.getInputFilters().putAll(newInputFilters);
			modelOrder.getClassOutputParameters().clear();
			modelOrder.getClassOutputParameters().putAll(newClassOutputParameters);
			modelOrder.getInputProductClasses().clear();
			modelOrder.getInputProductClasses().addAll(newInputProductClasses);
			modelOrder.getRequestedConfiguredProcessors().clear();
			modelOrder.getRequestedConfiguredProcessors().addAll(newConfiguredProcessors);
			modelOrder.getRequestedOrbits().clear();
			modelOrder.getRequestedOrbits().addAll(newRequestedOrbits);

			// Persist the modified order
			modelOrder = RepositoryService.getOrderRepository().save(modelOrder);
			logger.log(OrderMgrMessage.ORDER_MODIFIED, id);
		} else {
			logger.log(OrderMgrMessage.ORDER_NOT_MODIFIED, id);
		}
		return OrderUtil.toRestOrder(modelOrder);

	}

	/**
	 * List of all orders filtered by mission, identifier, product class, execution time range; selection is restricted to the
	 * mission the current user is logged in to
	 *
	 * @param mission                 the mission code
	 * @param identifier              the order identifier
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
	public List<RestOrder> getOrders(String mission, String identifier, String[] requestedProductClasses,
			@DateTimeFormat Date startTimeFrom, @DateTimeFormat Date startTimeTo, @DateTimeFormat Date executionTimeFrom,
			@DateTimeFormat Date executionTimeTo) throws NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getOrders({}, {}, {}, {}, {})", mission, identifier, requestedProductClasses, startTimeFrom,
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

		List<RestOrder> result = new ArrayList<>();

		// Find using search parameters
		String jpqlQuery = "select p from ProcessingOrder p " + "join p.requestedProductClasses rpc "
				+ "where p.mission.code = :mission";
		if (null != identifier) {
			jpqlQuery += " and p.identifier = :identifier";
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
		if (null != identifier) {
			query.setParameter("identifier", identifier);
		}
		if (null != requestedProductClasses && 0 < requestedProductClasses.length) {
			for (int i = 0; i < requestedProductClasses.length; ++i) {
				query.setParameter("requestedProductClasses" + i, requestedProductClasses[i]);
			}
		}
		if (null != startTimeFrom) {
			query.setParameter("startTimeFrom", startTimeFrom.toInstant());
		}
		if (null != startTimeTo) {
			query.setParameter("startTimeTo", startTimeTo.toInstant());
		}
		if (null != executionTimeFrom) {
			query.setParameter("executionTimeFrom", executionTimeFrom.toInstant());
		}
		if (null != executionTimeTo) {
			query.setParameter("executionTimeTo", executionTimeTo.toInstant());
		}
		for (Object resultObject : query.getResultList()) {
			if (resultObject instanceof ProcessingOrder) {
				result.add(OrderUtil.toRestOrder((ProcessingOrder) resultObject));
			}
		}

		if (result.isEmpty()) {
			throw new NoResultException(logger.log(OrderMgrMessage.ORDER_LIST_EMPTY));

		}
		logger.log(OrderMgrMessage.ORDER_LIST_RETRIEVED, result.size(), mission, identifier, startTimeFrom, startTimeTo);
		return result;

	}

	/**
	 * Retrieve a list of orders satisfying the selection parameters. Mission code is mandatory.
	 *
	 * @param mission                 the mission code
	 * @param identifier              the order identifier pattern
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
	public List<RestOrder> getAndSelectOrders(String mission, String identifier, String[] state, String[] requestedProductClasses,
			String startTimeFrom, String startTimeTo, Long recordFrom, Long recordTo, String[] orderBy) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getAndSelectOrders({}, {}, {}, {}, {}, {}, {}, {}, {})", mission, identifier, state, 
					requestedProductClasses, startTimeFrom, startTimeTo, recordFrom, recordTo, orderBy);

		if (null == mission)
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.MISSION_CODE_MISSING));

		List<RestOrder> list = new ArrayList<>();
		Query query = createOrdersQuery(mission, identifier, state, startTimeFrom, startTimeTo, orderBy, false);

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
		long i = 0;
		for (Object resultObject : query.getResultList()) {
			if (i < recordFrom) {
				i++;
			} else {
				if (resultObject instanceof ProcessingOrder) {
					// Filter depending on product visibility and user authorization
					ProcessingOrder order = (ProcessingOrder) resultObject;
					if (productClasses != null) {
						for (ProductClass pc : order.getRequestedProductClasses()) {
							if (productClasses.contains(pc.getProductType())) {
								i++;
								list.add(de.dlr.proseo.model.util.OrderUtil.toRestOrder(order));
								break;
							}
						}
					} else {
						i++;
						list.add(de.dlr.proseo.model.util.OrderUtil.toRestOrder(order));
					}
				}
				if (i >= recordTo) {
					break;
				}
			}
		}

		logger.log(OrderMgrMessage.ORDER_LIST_RETRIEVED, list.size(), mission, identifier, startTimeFrom, startTimeTo);
		return list;
	}

	/**
	 * Calculate the amount of orders satisfying the selection parameters. Mission code is mandatory.
	 *
	 * @param mission                 the mission code
	 * @param identifier              the order identifier pattern
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
	public String countSelectOrders(String mission, String identifier, String[] state, String[] requestedProductClasses,
			String startTimeFrom, String startTimeTo, Long recordFrom, Long recordTo, String[] orderBy) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getAndSelectOrders({}, {}, {}, {}, {}, {}, {}, {}, {})", mission, identifier, state, 
					requestedProductClasses, startTimeFrom, startTimeTo, recordFrom, recordTo, orderBy);

		if (null == mission)
			throw new IllegalArgumentException(logger.log(OrderMgrMessage.MISSION_CODE_MISSING));

		Query query = createOrdersQuery(mission, identifier, state, startTimeFrom, startTimeTo, orderBy, false);

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
			if (resultObject instanceof ProcessingOrder) {
				// Filter depending on product visibility and user authorization
				if (productClasses != null) {
					ProcessingOrder order = (ProcessingOrder) resultObject;
					for (ProductClass pc : order.getRequestedProductClasses()) {
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

		logger.log(OrderMgrMessage.ORDER_LIST_RETRIEVED, i, mission, identifier, startTimeFrom, startTimeTo);
		return i.toString();
	}

	/**
	 * Create a JPQL query to retrieve the requested set of products
	 *
	 * @param mission       the mission code
	 * @param identifier    the order identifier pattern
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
	private Query createOrdersQuery(String mission, String identifier, String[] state, String startTimeFrom, String startTimeTo,
			String[] orderBy, Boolean count) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getAndSelectOrders({}, {}, {}, {}, {}, {}, {}, {}, {}, {})", mission, identifier, state,
					startTimeFrom, startTimeTo, orderBy, count);

		// Find using search parameters
		String jpqlQuery = null;
		String join = "";
		if (count) {
			jpqlQuery = "select count(p) from ProcessingOrder p " + join + " where p.mission.code = :missionCode";
		} else {
			jpqlQuery = "select p from ProcessingOrder p " + join + " where p.mission.code = :missionCode";
		}
		if (null != state && 0 < state.length) {
			jpqlQuery += " and p.orderState in (";
			for (int i = 0; i < state.length; ++i) {
				if (0 < i)
					jpqlQuery += ", ";
				jpqlQuery += ":orderState" + i;
			}
			jpqlQuery += ")";
		}
		if (null != identifier) {
			jpqlQuery += " and upper(p.identifier) like :identifier";
		}
		if (null != startTimeFrom) {
			jpqlQuery += " and p.startTime >= :startTime";
		}
		if (null != startTimeTo) {
			jpqlQuery += " and p.startTime <= :stopTime";
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
		if (null != state && 0 < state.length) {
			for (int i = 0; i < state.length; ++i) {
				query.setParameter("orderState" + i, OrderState.valueOf(state[i]));
			}
		}
		if (null != identifier) {
			query.setParameter("identifier", identifier.toUpperCase());
		}

		if (null != startTimeFrom) {
			query.setParameter("startTime", OrbitTimeFormatter.parseDateTime(startTimeFrom));
		}

		if (null != startTimeTo) {
			query.setParameter("stopTime", OrbitTimeFormatter.parseDateTime(startTimeTo));
		}
		return query;
	}

}