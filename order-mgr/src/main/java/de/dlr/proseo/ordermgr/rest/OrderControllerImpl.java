/**
 * OrderControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.ordermgr.rest.model.OrbitUtil;
import de.dlr.proseo.ordermgr.rest.model.OrderUtil;
import de.dlr.proseo.ordermgr.rest.model.RestOrbit;
import de.dlr.proseo.ordermgr.rest.model.RestOrbitQuery;
import de.dlr.proseo.ordermgr.rest.model.RestOrder;

/**
 * Spring MVC controller for the prosEO Order Manager; implements the services required to manage processing orders
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class OrderControllerImpl implements OrderController {
	
	/* Message ID constants */
	private static final int MSG_ID_ORDER_NOT_FOUND = 1007;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 1004;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	private static final int MSG_ID_ORDER_MISSING = 1008;


	/* Message string constants */
	private static final String MSG_ORDER_NOT_FOUND = "No order found for ID %d (%d)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "Order deletion unsuccessful for ID %d (%d)";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ordermgr-ordercontroller ";
	private static final String MSG_ORDER_MISSING = "(E%d) Order not set";

	private static Logger logger = LoggerFactory.getLogger(OrderControllerImpl.class);

	@SuppressWarnings("unchecked")
	@Override
	public ResponseEntity<RestOrder> createOrder(RestOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> createOrder({})", order.getClass());
		
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
		
		modelOrder = RepositoryService.getOrderRepository().save(modelOrder);
		
		return new ResponseEntity<>(OrderUtil.toRestOrder(modelOrder), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<List<RestOrder>> getOrders(String mission, String identifier, String[] productclasses, Date starttimefrom,
			Date starttimeto) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> getOrder{}");
		List<RestOrder> result = new ArrayList<>();		
		// Find using search parameters
		if("" != mission) {
			
			
		}
		//Find all with no search criteria
		else{
			for(ProcessingOrder procOrder : RepositoryService.getOrderRepository().findAll()) {
				if (logger.isDebugEnabled()) logger.debug("Found order with ID {}", procOrder.getId());
				RestOrder resultOrder = OrderUtil.toRestOrder(procOrder);
				if (logger.isDebugEnabled()) logger.debug("Created result order with ID {}", resultOrder.getId());
				result.add(resultOrder);		
			
			}
		}
			
		return new ResponseEntity<>(result, HttpStatus.OK);								

	

	}

	@Override
	public ResponseEntity<RestOrder> getOrderById(Long id) {
		// TODO Auto-generated method stub
		if (logger.isTraceEnabled()) logger.trace(">>> getOrderById({})", id);
		
		Optional<de.dlr.proseo.model.ProcessingOrder> modelOrder = RepositoryService.getOrderRepository().findById(id);
		
		if (modelOrder.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_ORDER_NOT_FOUND, id, MSG_ID_ORDER_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(OrderUtil.toRestOrder(modelOrder.get()), HttpStatus.OK);
	}

	// To be Tested
	@Override
	public ResponseEntity<RestOrder> modifyOrder(Long id, @Valid RestOrder order) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyOrder({})", id);
		
		Optional<ProcessingOrder> optModelOrder = RepositoryService.getOrderRepository().findById(id);
		
		if (optModelOrder.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_ORDER_NOT_FOUND, id, MSG_ID_ORDER_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
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
		if (!modelOrder.getProcessingMode().equals(changedOrder.getProcessingMode())) {
			orderChanged = true;
			modelOrder.setProcessingMode(changedOrder.getProcessingMode());
		}
		
		// Save order only if anything was actually changed
		if (orderChanged)	{
			modelOrder.incrementVersion();
			modelOrder = RepositoryService.getOrderRepository().save(modelOrder);
		}
		
		return new ResponseEntity<>(OrderUtil.toRestOrder(modelOrder), HttpStatus.OK);
	
	}
	@Override
	public ResponseEntity<?> deleteOrderById(Long id) {
		 if (logger.isTraceEnabled()) logger.trace(">>> deleteOrderById({})", id);
			
			// Test whether the order id is valid
			Optional<ProcessingOrder> modelOrder = RepositoryService.getOrderRepository().findById(id);
			if (modelOrder.isEmpty()) {
				String message = String.format(MSG_PREFIX + MSG_ORDER_NOT_FOUND, id, MSG_ID_ORDER_NOT_FOUND);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			}
			
			// Delete the order
			RepositoryService.getOrderRepository().deleteById(id);

			// Test whether the deletion was successful
			modelOrder = RepositoryService.getOrderRepository().findById(id);
			if (!modelOrder.isEmpty()) {
				String message = String.format(MSG_PREFIX + MSG_DELETION_UNSUCCESSFUL, id, MSG_ID_DELETION_UNSUCCESSFUL);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			}
			
			HttpHeaders responseHeaders = new HttpHeaders();
			return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);		
	}

}
