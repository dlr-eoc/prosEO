package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.rest.model.OrderUtil;
import de.dlr.proseo.ordermgr.rest.model.RestOrbitQuery;
import de.dlr.proseo.ordermgr.rest.model.RestOrder;


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
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	private static final int MSG_ID_ORDER_MISSING = 1008;
	private static final int MSG_ID_ORDER_DELETED = 1009;
	private static final int MSG_ID_ORDER_RETRIEVED = 1010;
	private static final int MSG_ID_ORDER_MODIFIED = 1011;
	private static final int MSG_ID_ORDER_NOT_MODIFIED = 1012;
	private static final int MSG_ID_ORDER_CREATED = 1013;


	/* Message string constants */
	private static final String MSG_ORDER_NOT_FOUND = "No order found for ID %d (%d)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "Order deletion unsuccessful for ID %d (%d)";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ordermgr-ordercontroller ";
	private static final String MSG_ORDER_MISSING = "(E%d) Order not set";
	private static final String MSG_ORDER_DELETED = "(I%d) Order with id %d deleted";
	private static final String MSG_ORDER_ID_MISSING = "(E%d) Order ID not set";
	private static final String MSG_ORDER_RETRIEVED = "(I%d) Order with ID %s retrieved";
	private static final String MSG_ORDER_NOT_MODIFIED = "(I%d) Order with id %d not modified (no changes)";
	private static final String MSG_ORDER_MODIFIED = "(I%d) Order with id %d modified";
	private static final String MSG_ORDER_CREATED = "(I%d) Order with identifier %s created for mission %s";

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
		
		//Find the  mission for the mission code given in the rest Order
		de.dlr.proseo.model.Mission mission = RepositoryService.getMissionRepository().findByCode(order.getMissionCode());
		modelOrder.setMission(mission);	
		
		modelOrder.getRequestedOrbits().clear();
		for(RestOrbitQuery orbitQuery : order.getOrbits()) {
			List<Orbit> orbit = RepositoryService.getOrbitRepository().
									findBySpacecraftCodeAndOrbitNumberBetween(orbitQuery.getSpacecraftCode(), orbitQuery.getOrbitNumberFrom().intValue(), orbitQuery.getOrbitNumberTo().intValue());
			modelOrder.getRequestedOrbits().addAll(orbit);
		}
		
		modelOrder.getRequestedProductClasses().clear();
		for (String prodClass : order.getRequestedProductClasses()) {
			for(ProductClass product : RepositoryService.getProductClassRepository().findByProductType(prodClass)) {
				modelOrder.getRequestedProductClasses().add(product);
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

		// To be verified
		@SuppressWarnings("rawtypes")
		Set jobs = new HashSet();
		for(Job job : RepositoryService.getJobRepository().findAll()) {			
			if(job.getProcessingOrder().getId() == order.getId()) {
				jobs.add(job);				
			}
		}
		
		modelOrder.setJobs(jobs);
		
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
	 * Update the product with the given ID with the attribute values of the given Json object. This method will NOT modify
	 * associated product files.
	 * 
	 * @param id the ID of the product to update
	 * @param product a Json object containing the modified (and unmodified) attributes
	 * @return a Json object corresponding to the product after modification (with ID and version for all contained objects)
	 * @throws EntityNotFoundException if no product with the given ID exists
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws ConcurrentModificationException if the product has been modified since retrieval by the client
	 */
	public RestOrder modifyOrder(Long id, RestOrder order) throws
	EntityNotFoundException, IllegalArgumentException, ConcurrentModificationException {
		
		Optional<ProcessingOrder> optModelOrder = RepositoryService.getOrderRepository().findById(id);
		
		if (optModelOrder.isEmpty()) {
			throw new EntityNotFoundException(logError(MSG_ORDER_NOT_FOUND, MSG_ID_ORDER_NOT_FOUND, id));
		}
		ProcessingOrder modelOrder = optModelOrder.get();
		
		// Update modified attributes
		boolean orderChanged = false;
		ProcessingOrder changedOrder = OrderUtil.toModelOrder(order);
		
		if (!modelOrder.getMission().equals(changedOrder.getMission())) {
			orderChanged = true;
			modelOrder.setMission(changedOrder.getMission());
		}
		
		if (!modelOrder.getIdentifier().equals(changedOrder.getIdentifier())) {
			orderChanged = true;
			modelOrder.setIdentifier(changedOrder.getIdentifier());
		}
		if (!modelOrder.getOrderState().equals(changedOrder.getOrderState())) {
			orderChanged = true;
			modelOrder.setOrderState(changedOrder.getOrderState());
		}
		if (!modelOrder.getFilterConditions().equals(changedOrder.getFilterConditions())) {
			orderChanged = true;
			modelOrder.setFilterConditions(changedOrder.getFilterConditions());
		}
		if (!modelOrder.getOutputParameters().equals(changedOrder.getOutputParameters())) {
			orderChanged = true;
			modelOrder.setOutputParameters(changedOrder.getOutputParameters());
		}
		if (!modelOrder.getRequestedProductClasses().equals(changedOrder.getRequestedProductClasses())) {
			orderChanged = true;
			modelOrder.setRequestedProductClasses(changedOrder.getRequestedProductClasses());
		}

		if (!modelOrder.getProcessingMode().equals(changedOrder.getProcessingMode())) {
			orderChanged = true;
			modelOrder.setProcessingMode(changedOrder.getProcessingMode());
		}		
		
		// Save order only if anything was actually changed
		if (orderChanged)	{
			modelOrder.incrementVersion();
			modelOrder = RepositoryService.getOrderRepository().save(modelOrder);
			logInfo(MSG_ORDER_MODIFIED, MSG_ID_ORDER_MODIFIED, id);
		} else {
			logInfo(MSG_ORDER_NOT_MODIFIED, MSG_ID_ORDER_NOT_MODIFIED, id);
		}
		
		return OrderUtil.toRestOrder(modelOrder);

	}
	
	
	public List<RestOrder> getOrders(String mission, String identifier, String[] productclasses, @DateTimeFormat Date starttimefrom,
			@DateTimeFormat Date starttimeto) {
		// TODO Auto-generated method stub
		if (logger.isTraceEnabled()) logger.trace(">>> getOrders({}, {}, {}, {}, {})", mission, identifier, productclasses, starttimefrom, starttimeto);
		List<RestOrder> result = new ArrayList<>();
		
		if (null == mission && null == identifier && (null == productclasses || 0 == productclasses.length) && null == starttimefrom && null == starttimeto) {
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
			if (null != starttimefrom) {
				jpqlQuery += " and p.startTime >= :startTimeFrom";
			}
			if (null != starttimeto) {
				jpqlQuery += " and p.stopTime <= :startTimeTo";
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
			if (null != starttimefrom) {
				query.setParameter("startTimeFrom", starttimefrom);
			}
			if (null != starttimeto) {
				query.setParameter("startTimeTo", starttimeto);
			}
			for (Object resultObject: query.getResultList()) {
				if (resultObject instanceof ProcessingOrder) {
					result.add(OrderUtil.toRestOrder((ProcessingOrder) resultObject));
				}
			}

		}
		return result;

	}

}
