package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import de.dlr.proseo.model.Orbit;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.dispatcher.OrderDispatcher;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.planner.rest.model.RestJobStep;
import de.dlr.proseo.planner.rest.model.RestOrbitQuery;
import de.dlr.proseo.planner.rest.model.RestOrder;
import de.dlr.proseo.planner.rest.model.RestParameter;
import de.dlr.proseo.planner.rest.model.RestUtil;
import de.dlr.proseo.planner.util.JobStepUtil;
import de.dlr.proseo.planner.util.JobUtil;
import de.dlr.proseo.planner.util.OrderUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderControllerImpl implements OrderController {
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_HEADER_SUCCESS = "Success";
	private static final String MSG_PREFIX = "199 proseo-planner ";
	
	private static Logger logger = LoggerFactory.getLogger(OrderControllerImpl.class);
	
	
    @Autowired
    private ProductionPlanner productionPlanner;

    @Autowired
    private OrderDispatcher orderDispatcher;

    @Autowired
    private JobStepUtil jobStepUtil;
    @Autowired
    private JobUtil jobUtil;
    @Autowired
    private OrderUtil orderUtil;


	/**
	 * Get all processing orders
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<List<RestOrder>> getOrders() {
		
		Iterable<ProcessingOrder> orders = RepositoryService.getOrderRepository().findAll();
		List<RestOrder> list = new ArrayList<RestOrder>();

		for (ProcessingOrder po : orders) {
			RestOrder ro = RestUtil.createRestOrder(po);
			list.add(ro);			
		}

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_SUCCESS, "");
		return new ResponseEntity<>(list, responseHeaders, HttpStatus.OK);
	}
	
	/**
	 * Approve prcessing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> approveOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			if (orderUtil.approve(order)) {
				// canceled
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Processing order approved");
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Processing order could not be approved");
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		String message = String.format(MSG_PREFIX + "Processing order with id or identifier %s does not exist (%d)", orderId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	
	/**
	 * Approve prcessing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> resetOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			if (orderUtil.reset(order)) {
				// canceled
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Processing order reset");
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Processing order could not be reset");
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		String message = String.format(MSG_PREFIX + "Processing order with id or identifier %s does not exist (%d)", orderId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	/**
	 * Plan processing order of id on processing facility
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> planOrder(String releaseId, String facility) {
		if (releaseId != null && facility != null) {
			ProcessingOrder order = findOrder(releaseId);
			ProcessingFacility pf = null;
			KubeConfig kc = productionPlanner.getKubeConfig(facility);
			if (kc != null) {
				pf = kc.getProcessingFacility();
			}
			if (order != null && pf != null) {
				if (orderUtil.plan(order, pf)) {
					RestOrder ro = RestUtil.createRestOrder(order);
					String message = String.format(MSG_PREFIX + "Jobs for processing order '%s' planned (%d)", order.getIdentifier(), 2000);
					logger.error(message);
					HttpHeaders responseHeaders = new HttpHeaders();
					responseHeaders.set(HTTP_HEADER_WARNING, message);
					return new ResponseEntity<>(ro, responseHeaders, HttpStatus.CREATED);
				} else {
					RestOrder ro = RestUtil.createRestOrder(order);
					String message = String.format(MSG_PREFIX + "Jobs for processing order '%s' could not be planned (%d)", order.getIdentifier(), 2000);
					logger.error(message);
					HttpHeaders responseHeaders = new HttpHeaders();
					responseHeaders.set(HTTP_HEADER_WARNING, message);
					return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
				}
			} else {
				String message = String.format(MSG_PREFIX);
				if (order == null && pf == null) {
					message += String.format("Processing order '%s' and processing facility '%s' not found (%d)", releaseId, facility, 2000);
				} else {
					if (order == null) {
						message += String.format("Processing order '%s' not found (%d)", releaseId, 2000);
					}
					if (pf == null) {
						message += String.format("Processing facility '%s' not found (%d)", facility, 2000);
					}
				}
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			}
		} else {
			String message = String.format(MSG_PREFIX);
			if (releaseId == null && facility == null) {
				message += String.format("Parameter id and facility are not set (%d)", 2000);
			} else {
				if (releaseId == null) {
					message += String.format("Parameter id not set  (%d)", 2000);
				}
				if (facility == null) {
					message += String.format("Parameter facility not set (%d)", 2000);
				}
			}
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Release prcessing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> releaseOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			if (orderUtil.resume(order)) {
				// canceled
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Processing order resumed/released");
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Processing order could not be resumed/released");
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		String message = String.format(MSG_PREFIX + "Processing order with id or identifier %s does not exist (%d)", orderId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	/**
	 * Cancel prcessing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> cancelOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			if (orderUtil.cancel(order)) {
				// canceled
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Processing order canceled");
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Processing order could not be canceled");
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		String message = String.format(MSG_PREFIX + "Processing order with id or identifier %s does not exist (%d)", orderId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	/**
	 * Suspend prcessing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> suspendOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			if (orderUtil.suspend(order)) {
				// canceled
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Processing order suspended");
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_SUCCESS, "Processing order could not be suspended");
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		String message = String.format(MSG_PREFIX + "Processing order with id or identifier %s does not exist (%d)", orderId, 2001);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	@Transactional
	private ProcessingOrder findOrder(String orderId) {
		Optional<ProcessingOrder> orderOpt = null;
		ProcessingOrder order = null;
		try {
			Long id = Long.valueOf(orderId);
			orderOpt = RepositoryService.getOrderRepository().findById(id);
			if (orderOpt.isPresent()) {
				order = orderOpt.get();
			}
		} catch (NumberFormatException nfe) {
			// use name as identifier
		}
		if (order == null) {
			order = RepositoryService.getOrderRepository().findByIdentifier(orderId);
		}
		return order;
	}

	
}
