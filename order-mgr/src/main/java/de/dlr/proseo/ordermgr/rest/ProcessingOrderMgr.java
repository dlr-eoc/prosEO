package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrderUtil;
import de.dlr.proseo.model.rest.model.RestOrbitQuery;
import de.dlr.proseo.model.rest.model.RestOrder;


/**
 * Service methods required to create, modify and delete processing order in the prosEO database,
 * and to query the database about such orders
 * 
 * @author Ranjitha Vignesh
 */
@Component
@Transactional
public class ProcessingOrderMgr {
	
	/* Message ID constants */
	private static final int MSG_ID_ORDER_NOT_FOUND = 1007;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 1004;
	//private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	private static final int MSG_ID_ORDER_MISSING = 1008;
	private static final int MSG_ID_ORDER_DELETED = 1009;
	private static final int MSG_ID_ORDER_RETRIEVED = 1010;
	private static final int MSG_ID_ORDER_MODIFIED = 1011;
	private static final int MSG_ID_ORDER_NOT_MODIFIED = 1012;
	private static final int MSG_ID_ORDER_CREATED = 1013;
	private static final int MSG_ID_DUPLICATE_ORDER_UUID = 1014;
	private static final int MSG_ID_INVALID_REQUESTED_CLASS = 1015;
	private static final int MSG_ID_INVALID_INPUT_CLASS = 1016;
	private static final int MSG_ID_INVALID_FILE_CLASS = 1017;
	private static final int MSG_ID_INVALID_PROCESSING_MODE = 1018;
	private static final int MSG_ID_INVALID_CONFIGURED_PROCESSOR = 1019;
	private static final int MSG_ID_INVALID_ORBIT_RANGE = 1020;
	private static final int MSG_ID_ORDER_IDENTIFIER_MISSING = 1021;
	private static final int MSG_ID_DUPLICATE_ORDER_IDENTIFIER = 1022;
	private static final int MSG_ID_ORDER_TIME_INTERVAL_MISSING = 1023;
	private static final int MSG_ID_REQUESTED_PRODUCTCLASSES_MISSING = 1024;
	private static final int MSG_ID_ORDER_LIST_EMPTY = 1025;
	private static final int MSG_ID_ORDER_LIST_RETRIEVED = 1026;
	

	/* Message string constants */
	private static final String MSG_ORDER_NOT_FOUND = "(E%d) No order found for ID %d";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Order deletion unsuccessful for ID %d";
	private static final String MSG_ORDER_MISSING = "(E%d) Order not set";
	private static final String MSG_ORDER_DELETED = "(I%d) Order with id %d deleted";
	private static final String MSG_ORDER_ID_MISSING = "(E%d) Order ID not set";
	private static final String MSG_ORDER_RETRIEVED = "(I%d) Order with ID %s retrieved";
	private static final String MSG_ORDER_NOT_MODIFIED = "(I%d) Order with id %d not modified (no changes)";
	private static final String MSG_ORDER_MODIFIED = "(I%d) Order with id %d modified";
	private static final String MSG_ORDER_CREATED = "(I%d) Order with identifier %s created for mission %s";
	private static final String MSG_DUPLICATE_ORDER_UUID = "(E%d) Order UUID %s already exists";
	private static final String MSG_INVALID_REQUESTED_CLASS = "(E%d) Requested product class %s is not defined for mission %s";
	private static final String MSG_INVALID_INPUT_CLASS = "(E%d) Input product class %s is not defined for mission %s";
	private static final String MSG_INVALID_FILE_CLASS = "(E%d) Output file class %s is not defined for mission %s";
	private static final String MSG_INVALID_PROCESSING_MODE = "(E%d) Processing mode %s is not defined for mission %s";
	private static final String MSG_INVALID_CONFIGURED_PROCESSOR = "(E%d) Configured processor %s not found";
	private static final String MSG_INVALID_ORBIT_RANGE = "(E%d) No orbits defined between orbit number %d and %d for spacecraft %s";
	private static final String MSG_ORDER_IDENTIFIER_MISSING = "(E%d) Order identifier not set";
	private static final String MSG_DUPLICATE_ORDER_IDENTIFIER = "(E%d) Order identifier %s already exists";
	private static final String MSG_ORDER_TIME_INTERVAL_MISSING = "(E%d) Time interval (orbit or time range) missing for order %s";
	private static final String MSG_REQUESTED_PRODUCTCLASSES_MISSING = "(E%d) Requested product classes missing for order %s";
	private static final String MSG_ORDER_LIST_RETRIEVED = "(I%d) Order list of size %d retrieved for mission '%s', order '%s', start time '%s', stop time '%s'";

	private static final String MSG_ORDER_LIST_EMPTY = "(E%d) No processing order found for search criteria";


	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessingOrderMgr.class);
	
	/**
	 * Create and log a formatted informational message
	 * 
	 * @param messageFormat the message text with parameter placeholder in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info message
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
	 * Create an order from the given Json object 
	 * 
	 * @param order the Json object to create the order from
	 * @return a Json object corresponding to the order after persistence (with ID and version for all contained objects)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 */
	public RestOrder createOrder(RestOrder order) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> createOrder({})", (null == order ? "MISSING" : order.getIdentifier()));
		
		if (null == order) {
			throw new IllegalArgumentException(logError(MSG_ORDER_MISSING, MSG_ID_ORDER_MISSING));
		}
		
		ProcessingOrder modelOrder = OrderUtil.toModelOrder(order);
		// Make sure order has a UUID
		if (null == modelOrder.getUuid()) {
			modelOrder.setUuid(UUID.randomUUID());
		} else {
			// Test if given UUID is not yet in use
			if (null != RepositoryService.getOrderRepository().findByUuid(modelOrder.getUuid())) {
				throw new IllegalArgumentException(logError(MSG_DUPLICATE_ORDER_UUID, MSG_ID_DUPLICATE_ORDER_UUID, 
						modelOrder.getUuid()));
			}
		}
		
		// Make sure order has a non-blank identifier, which is not yet in use
		if (null == modelOrder.getIdentifier() || modelOrder.getIdentifier().isBlank()) {
			throw new IllegalArgumentException(logError(MSG_ORDER_IDENTIFIER_MISSING, MSG_ID_ORDER_IDENTIFIER_MISSING));
		}
		if (null != RepositoryService.getOrderRepository().findByIdentifier(modelOrder.getIdentifier())) {
			throw new IllegalArgumentException(logError(MSG_DUPLICATE_ORDER_IDENTIFIER, MSG_ID_DUPLICATE_ORDER_IDENTIFIER, modelOrder.getIdentifier()));
		}
		
		// No matter what the given order state was, orders are always created in state INITIAL
		modelOrder.setOrderState(OrderState.INITIAL);
		
		//Find the  mission for the mission code given in the rest Order
		de.dlr.proseo.model.Mission mission = RepositoryService.getMissionRepository().findByCode(order.getMissionCode());
		modelOrder.setMission(mission);	
		
		// Identify the order time interval, either by orbit range queries if given, or by start and stop time
		if (order.getOrbits().isEmpty()) {
			if (null == modelOrder.getStartTime() || null == modelOrder.getStopTime()) {
				throw new IllegalArgumentException(logError(MSG_ORDER_TIME_INTERVAL_MISSING, MSG_ID_ORDER_TIME_INTERVAL_MISSING, modelOrder.getIdentifier()));
			}
		} else {
			// Find all requested orbit ranges
			modelOrder.getRequestedOrbits().clear();
			for (RestOrbitQuery orbitQuery : order.getOrbits()) {
				List<Orbit> orbit = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumberBetween(
						orbitQuery.getSpacecraftCode(),
						orbitQuery.getOrbitNumberFrom().intValue(),
						orbitQuery.getOrbitNumberTo().intValue());
				if (orbit.isEmpty()) {
					throw new IllegalArgumentException(logError(MSG_INVALID_ORBIT_RANGE, MSG_ID_INVALID_ORBIT_RANGE,
							orbitQuery.getOrbitNumberFrom(),
							orbitQuery.getOrbitNumberTo(),
							orbitQuery.getSpacecraftCode()));
				}
				modelOrder.getRequestedOrbits().addAll(orbit);
			}
			// Set start and stop time from requested orbits
			Orbit minOrbit = Collections.min(modelOrder.getRequestedOrbits(),
					(o1, o2) -> { return o1.getStartTime().compareTo(o2.getStartTime()); });
			Orbit maxOrbit = Collections.max(modelOrder.getRequestedOrbits(),
					(o1, o2) -> { return o1.getStopTime().compareTo(o2.getStopTime()); });
			modelOrder.setStartTime(minOrbit.getStartTime());
			modelOrder.setStopTime(maxOrbit.getStopTime());
		}
		
		// Make sure requested product classes are set (mandatory)
		if (order.getRequestedProductClasses().isEmpty()) {
			throw new IllegalArgumentException(logError(MSG_REQUESTED_PRODUCTCLASSES_MISSING, MSG_ID_REQUESTED_PRODUCTCLASSES_MISSING, modelOrder.getIdentifier()));
		} else {
			modelOrder.getRequestedProductClasses().clear();
			for (String prodClass : order.getRequestedProductClasses()) {
				for (ProductClass product : RepositoryService.getProductClassRepository().findByProductType(prodClass)) {
					modelOrder.getRequestedProductClasses().add(product);
				}
			} 
		}
		
		modelOrder.getInputProductClasses().clear();
		for (String prodClass : order.getInputProductClasses()) {
			for(ProductClass product : RepositoryService.getProductClassRepository().findByProductType(prodClass)) {
				modelOrder.getInputProductClasses().add(product);
			}
		}		
		modelOrder.getRequestedConfiguredProcessors().clear();
		for (String identifier : order.getConfiguredProcessors()) {
			modelOrder.getRequestedConfiguredProcessors().add(RepositoryService.getConfiguredProcessorRepository().findByIdentifier(identifier));
		}
	
		// Everything OK, store new order in database
		modelOrder = RepositoryService.getOrderRepository().save(modelOrder);
		
		logInfo(MSG_ORDER_CREATED, MSG_ID_ORDER_CREATED, order.getIdentifier(), order.getMissionCode());

		return OrderUtil.toRestOrder(modelOrder);

		
	}

	
	/**
	 * Delete an order by ID
	 * 
	 * @param the ID of the order to delete
	 * @throws EntityNotFoundException if the order to delete does not exist in the database
	 * @throws RuntimeException if the deletion was not performed as expected
	 */
	public void deleteOrderById(Long id) throws EntityNotFoundException, RuntimeException {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteOrderById({})", id);

		
		// Test whether the order id is valid
		Optional<ProcessingOrder> modelOrder = RepositoryService.getOrderRepository().findById(id);
		if (modelOrder.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND));
		}
		// Delete the order
		RepositoryService.getOrderRepository().deleteById(id);
		// Test whether the deletion was successful
		modelOrder = RepositoryService.getOrderRepository().findById(id);
		if (!modelOrder.isEmpty()) {
			throw new RuntimeException(logError(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, id));
		}
		
		logInfo(MSG_ORDER_DELETED, MSG_ID_ORDER_DELETED, id);
	}
	
	/**
	 * Find the oder with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a Json object corresponding to the order found
	 * @throws IllegalArgumentException if no order ID was given
	 * @throws NoResultException if no order with the given ID exists
	 */
	public RestOrder getOrderById(Long id) throws IllegalArgumentException, NoResultException {
		if (logger.isTraceEnabled()) logger.trace(">>> getOrderById({})", id);
		
		if (null == id) {
			throw new IllegalArgumentException(logError(MSG_ORDER_ID_MISSING, MSG_ID_ORDER_MISSING, id));
		}
		
		Optional<ProcessingOrder> modelOrder = RepositoryService.getOrderRepository().findById(id);
		
		if (modelOrder.isEmpty()) {
			throw new NoResultException(logError(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, id));
		}
		
		logInfo(MSG_ORDER_RETRIEVED, MSG_ID_ORDER_RETRIEVED, id);
		
		return OrderUtil.toRestOrder(modelOrder.get());
	}
	
	/**
	 * Update the order with the given ID with the attribute values of the given Json object. 	 * 
	 * @param id the ID of the product to update
	 * @param order a Json object containing the modified (and unmodified) attributes
	 * @return a Json object corresponding to the product after modification (with ID and version for all contained objects)
	 * @throws EntityNotFoundException if no product with the given ID exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws ConcurrentModificationException if the order has been modified since retrieval by the client
	 */
	public RestOrder modifyOrder(Long id, RestOrder order) throws
	EntityNotFoundException, IllegalArgumentException, ConcurrentModificationException {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyOrder({})", id);
		
		if (null == id) {
			throw new IllegalArgumentException(logError(MSG_ORDER_ID_MISSING, MSG_ID_ORDER_MISSING, id));
		}
		
		Optional<ProcessingOrder> optModelOrder = RepositoryService.getOrderRepository().findById(id);
				
		if (optModelOrder.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, id));
		}
		ProcessingOrder modelOrder = optModelOrder.get();
		
		// Update modified attributes
		boolean orderChanged = false;
		ProcessingOrder changedOrder = OrderUtil.toModelOrder(order);
		
		logger.info("Model order missioncode: "+modelOrder.getMission().getCode());
		
		// Mission code and UUID cannot be changed
		
		if (!modelOrder.getIdentifier().equals(changedOrder.getIdentifier())) {
			orderChanged = true;
			modelOrder.setIdentifier(changedOrder.getIdentifier());
		}
		if (!modelOrder.getOrderState().equals(changedOrder.getOrderState())) {
			orderChanged = true;
			modelOrder.setOrderState(changedOrder.getOrderState());
		}
		if (null == modelOrder.getExecutionTime() && null != changedOrder.getExecutionTime()
				|| null != modelOrder.getExecutionTime() && !modelOrder.getOrderState().equals(changedOrder.getOrderState())) {
			orderChanged = true;
			modelOrder.setExecutionTime(changedOrder.getExecutionTime());
		}
		if (!modelOrder.getStartTime().equals(changedOrder.getStartTime())) {
			orderChanged = true;
			modelOrder.setStartTime(changedOrder.getStartTime());
		}
		if (!modelOrder.getStopTime().equals(changedOrder.getStopTime())) {
			orderChanged = true;
			modelOrder.setStopTime(changedOrder.getStopTime());
		}
		if (!modelOrder.getSlicingType().equals(changedOrder.getSlicingType())) {
			orderChanged = true;
			modelOrder.setSlicingType(changedOrder.getSlicingType());
		}
		if (null == modelOrder.getSliceDuration() && null != changedOrder.getSliceDuration()
				|| null != modelOrder.getSliceDuration() && !modelOrder.getSliceDuration().equals(changedOrder.getSliceDuration())) {
			orderChanged = true;
			modelOrder.setSliceDuration(changedOrder.getSliceDuration());
		}
		if (!modelOrder.getSliceOverlap().equals(changedOrder.getSliceOverlap())) {
			orderChanged = true;
			modelOrder.setSliceOverlap(changedOrder.getSliceOverlap());
		}
		if (!modelOrder.getPropagateSlicing().equals(changedOrder.getPropagateSlicing())) {
			orderChanged = true;
			modelOrder.setPropagateSlicing(changedOrder.getPropagateSlicing());
		}
		if (!modelOrder.getFilterConditions().equals(changedOrder.getFilterConditions())) {
			orderChanged = true;
			modelOrder.setFilterConditions(changedOrder.getFilterConditions());
		}
		if (!modelOrder.getOutputParameters().equals(changedOrder.getOutputParameters())) {
			orderChanged = true;
			modelOrder.setOutputParameters(changedOrder.getOutputParameters());
		}
		
		// Check for new requested product classes
		Set<ProductClass> newRequestedProductClasses = new HashSet<>();
		if (null != order.getRequestedProductClasses()) {
			REQUESTED_CLASSES:
			for (String requestedProductClass: order.getRequestedProductClasses()) {
				for (ProductClass modelRequestedClass: modelOrder.getRequestedProductClasses()) {
					if (modelRequestedClass.getProductType().equals(requestedProductClass)) {
						// Already present
						newRequestedProductClasses.add(modelRequestedClass);
						continue REQUESTED_CLASSES;
					}
				}
				// New component class
				orderChanged = true;
				ProductClass newRequestedClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(order.getMissionCode(), requestedProductClass);
				if (null == newRequestedClass) {
					throw new IllegalArgumentException(logError(MSG_INVALID_REQUESTED_CLASS, MSG_ID_INVALID_REQUESTED_CLASS,
							requestedProductClass, order.getMissionCode()));
				}
				newRequestedProductClasses.add(newRequestedClass);
			}
		}
		// Check for removed requested product classes
		for (ProductClass modelRequestedClass: modelOrder.getRequestedProductClasses()) {
			if (!newRequestedProductClasses.contains(modelRequestedClass)) {
				// Component class removed
				orderChanged = true;
			}
		}
		
		// Check for new input product classes
		Set<ProductClass> newInputProductClasses = new HashSet<>();
		if (null != order.getInputProductClasses()) {
			INPUT_CLASSES:
			for (String inputProductClass: order.getInputProductClasses()) {
				for (ProductClass modelInputClass: modelOrder.getInputProductClasses()) {
					if (modelInputClass.getProductType().equals(inputProductClass)) {
						// Already present
						newInputProductClasses.add(modelInputClass);
						continue INPUT_CLASSES;
					}
				}
				// New component class
				orderChanged = true;
				ProductClass newInputClass = RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(order.getMissionCode(), inputProductClass);
				if (null == newInputClass) {
					throw new IllegalArgumentException(logError(MSG_INVALID_INPUT_CLASS, MSG_ID_INVALID_INPUT_CLASS,
							inputProductClass, order.getMissionCode()));
				}
				newInputProductClasses.add(newInputClass);
			}
		}
		// Check for removed requested product classes
		for (ProductClass modelInputClass: modelOrder.getInputProductClasses()) {
			if (!newInputProductClasses.contains(modelInputClass)) {
				// Component class removed
				orderChanged = true;
			}
		}

		if (!modelOrder.getOutputFileClass().equals(changedOrder.getOutputFileClass())) {
			if (!modelOrder.getMission().getFileClasses().contains(changedOrder.getOutputFileClass())) {
				throw new IllegalArgumentException(logError(MSG_INVALID_FILE_CLASS, MSG_ID_INVALID_FILE_CLASS,
						changedOrder.getOutputFileClass(), order.getMissionCode()));
			}
			orderChanged = true;
			modelOrder.setOutputFileClass(changedOrder.getOutputFileClass());
		}		
		if (!modelOrder.getProcessingMode().equals(changedOrder.getProcessingMode())) {
			if (!modelOrder.getMission().getProcessingModes().contains(changedOrder.getProcessingMode())) {
				throw new IllegalArgumentException(logError(MSG_INVALID_PROCESSING_MODE, MSG_ID_INVALID_PROCESSING_MODE,
						changedOrder.getProcessingMode(), order.getMissionCode()));
			}
			orderChanged = true;
			modelOrder.setProcessingMode(changedOrder.getProcessingMode());
		}
		
		// Check for new configured processors
		Set<ConfiguredProcessor> newConfiguredProcessors = new HashSet<>();
		if (null != order.getConfiguredProcessors()) {
			CONFIGURED_PROCESSORS:
			for (String changedConfiguredProcessor: order.getConfiguredProcessors()) {
				for (ConfiguredProcessor modelConfiguredProcessor: modelOrder.getRequestedConfiguredProcessors()) {
					if (modelConfiguredProcessor.getIdentifier().equals(changedConfiguredProcessor)) {
						// Already present
						newConfiguredProcessors.add(modelConfiguredProcessor);
						continue CONFIGURED_PROCESSORS;
					}
				}
				// New component class
				orderChanged = true;
				ConfiguredProcessor newConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository().findByIdentifier(changedConfiguredProcessor);
				if (null == newConfiguredProcessor) {
					throw new IllegalArgumentException(logError(MSG_INVALID_CONFIGURED_PROCESSOR, MSG_ID_INVALID_CONFIGURED_PROCESSOR,
							changedConfiguredProcessor));
				}
				newConfiguredProcessors.add(newConfiguredProcessor);
			}
		}
		// Check for removed configured processors
		for (ConfiguredProcessor modelConfiguredProcessor: modelOrder.getRequestedConfiguredProcessors()) {
			if (!newConfiguredProcessors.contains(modelConfiguredProcessor)) {
				// Component class removed
				orderChanged = true;
			}
		}
		
		// Check for new requested orbits
		List<Orbit> newRequestedOrbits = new ArrayList<>();
		if (null != order.getOrbits()) {
			for (RestOrbitQuery changedOrbitQuery: order.getOrbits()) {
				List<Orbit> changedRequestedOrbits = RepositoryService.getOrbitRepository().findBySpacecraftCodeAndOrbitNumberBetween(
						changedOrbitQuery.getSpacecraftCode(),
						changedOrbitQuery.getOrbitNumberFrom().intValue(),
						changedOrbitQuery.getOrbitNumberTo().intValue());
				if (changedRequestedOrbits.isEmpty()) {
					throw new IllegalArgumentException(logError(MSG_INVALID_ORBIT_RANGE, MSG_ID_INVALID_ORBIT_RANGE,
							changedOrbitQuery.getOrbitNumberFrom(),
							changedOrbitQuery.getOrbitNumberTo(),
							changedOrbitQuery.getSpacecraftCode()));
				}
				for (Orbit changedRequestedOrbit: changedRequestedOrbits) {
					if (!modelOrder.getRequestedOrbits().contains(changedRequestedOrbit)) {
						// New orbit
						orderChanged = true;
					}
					newRequestedOrbits.add(changedRequestedOrbit);
				}
			}
		}
		// Check for removed requested orbits
		for (Orbit modelRequestedOrbit: modelOrder.getRequestedOrbits()) {
			if (!newRequestedOrbits.contains(modelRequestedOrbit)) {
				// Orbit removed
				orderChanged = true;
			}
		}
		
		// Save order only if anything was actually changed
		if (orderChanged)	{
			modelOrder.incrementVersion();
			
			// Update the lists and sets
			modelOrder.getRequestedProductClasses().clear();
			modelOrder.getRequestedProductClasses().addAll(newRequestedProductClasses);
			modelOrder.getInputProductClasses().clear();
			modelOrder.getInputProductClasses().addAll(newInputProductClasses);
			modelOrder.getRequestedConfiguredProcessors().clear();
			modelOrder.getRequestedConfiguredProcessors().addAll(newConfiguredProcessors);
			modelOrder.getRequestedOrbits().clear();
			modelOrder.getRequestedOrbits().addAll(newRequestedOrbits);
			
			// Persist the modified order
			modelOrder = RepositoryService.getOrderRepository().save(modelOrder);
			logInfo(MSG_ORDER_MODIFIED, MSG_ID_ORDER_MODIFIED, id);
		} else {
			logInfo(MSG_ORDER_NOT_MODIFIED, MSG_ID_ORDER_NOT_MODIFIED, id);
		}
		return OrderUtil.toRestOrder(modelOrder);

	}
	/**
	 * List of all orders filtered by mission,identifier, product class, execution time range
	 * 
	 * @param mission the mission code
	 * @param productClass an array of product types
	 * @param startTimeFrom earliest sensing start time
	 * @param startTimeTo latest sensing start time
	 * @param executionTimeFrom earliest order execution time
	 * @param executionTimeTo latest order execution time
	 * @return a list of orders
	 * @throws NoResultException if no orders matching the given search criteria could be found
	 */
	
	public List<RestOrder> getOrders(String mission, String identifier, String[] productclasses, @DateTimeFormat Date startTimeFrom,
			@DateTimeFormat Date startTimeTo, @DateTimeFormat Date executionTimeFrom,
			@DateTimeFormat Date executionTimeTo) {
		if (logger.isTraceEnabled()) logger.trace(">>> getOrders({}, {}, {}, {}, {})", mission, identifier, productclasses, startTimeFrom, startTimeTo, executionTimeFrom, executionTimeTo);
		List<RestOrder> result = new ArrayList<>();
		
		if (null == mission && null == identifier && (null == productclasses || 0 == productclasses.length) && null == executionTimeFrom && null == executionTimeTo) {
			// Simple case: no search criteria set
			for (ProcessingOrder order: RepositoryService.getOrderRepository().findAll()) {
				if (logger.isDebugEnabled()) logger.debug("Found order with ID {}", order.getId());
				RestOrder resultOrder = OrderUtil.toRestOrder(order);
				if (logger.isDebugEnabled()) logger.debug("Created result order with ID {}", resultOrder.getId());

				result.add(resultOrder);
			}
		}else {
			// Find using search parameters
			String jpqlQuery = "select p from ProcessingOrder p where 1 = 1";
			if (null != mission) {
				jpqlQuery += " and p.mission.code = :mission";
			}
			if (null != identifier) {
				jpqlQuery += " and p.identifier = :identifier";
			}
			if (null != productclasses && 0 < productclasses.length) {
				jpqlQuery += " and p.productClass.productType in (";
				for (int i = 0; i < productclasses.length; ++i) {
					if (0 < i) jpqlQuery += ", ";
					jpqlQuery += ":productClass" + i;
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
			if (null != mission) {
				query.setParameter("mission", mission);
			}
			if (null != identifier) {
				query.setParameter("identifier", identifier);
			}
			if (null != productclasses && 0 < productclasses.length) {
				for (int i = 0; i < productclasses.length; ++i) {
					query.setParameter("productClass" + i, productclasses[i]);
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
			for (Object resultObject: query.getResultList()) {
				if (resultObject instanceof ProcessingOrder) {
					result.add(OrderUtil.toRestOrder((ProcessingOrder) resultObject));
				}
			}

		}
		if (result.isEmpty()) {
			throw new NoResultException(logError(MSG_ORDER_LIST_EMPTY, MSG_ID_ORDER_LIST_EMPTY));
			
		}
		logInfo(MSG_ORDER_LIST_RETRIEVED, MSG_ID_ORDER_LIST_RETRIEVED, result.size(), result.size(), mission, identifier, startTimeFrom, startTimeTo);
		return result;

	}

}
