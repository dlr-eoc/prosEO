/**
 * OrderControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.model.rest.OrderController;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.planner.rest.model.RestUtil;
import de.dlr.proseo.planner.util.OrderUtil;
import de.dlr.proseo.planner.util.UtilService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring MVC controller for the prosEO planner; implements the services required to plan
 * and handle processing orders.
 * 
 * @author Ernst Melchinger
 *
 */
@Component
public class OrderControllerImpl implements OrderController {
	
	/**
	 * Logger of this class.
	 */
	private static Logger logger = LoggerFactory.getLogger(OrderControllerImpl.class);
	
	
    @Autowired
    private ProductionPlanner productionPlanner;

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
		responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), Messages.OK.getDescription());
		return new ResponseEntity<>(list, responseHeaders, HttpStatus.OK);
	}

	/**
	 * Get processing order of id
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<RestOrder> getOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			RestOrder ro = RestUtil.createRestOrder(order);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), Messages.OK.getDescription());
			return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
		}
		String message = Messages.ORDER_NOT_EXIST.formatWithPrefix(orderId);
		Messages.ORDER_NOT_EXIST.log(logger, orderId);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	/**
	 * Approve processing order of id
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<RestOrder> approveOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			Messages msg = orderUtil.approve(order);
			if (msg.isTrue()) {
				// canceled
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(orderId));
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not approve
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(orderId));
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		String message =  Messages.ORDER_NOT_EXIST.formatWithPrefix(orderId);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	
	/**
	 * Reset processing order of id
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<RestOrder> resetOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			Messages msg = orderUtil.reset(order);
			if (msg.isTrue()) {
				// canceled
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(orderId));
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(orderId));
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		String message = Messages.ORDER_NOT_EXIST.formatWithPrefix(orderId);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	
	/**
	 * Delete processing order of id
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<RestOrder> deleteOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			Messages msg = orderUtil.delete(order);
			if (msg.isTrue()) {
				// deleted
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(orderId));
				return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
			} else {
				// already running or at end, could not suspend
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(orderId));
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		String message = Messages.ORDER_NOT_EXIST.formatWithPrefix(orderId);
		logger.error(message);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	/**
	 * Plan processing order of id on processing facility
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<RestOrder> planOrder(String releaseId, String facility) {
		if (releaseId != null && facility != null) {
			productionPlanner.updateKubeConfig(facility);
			ProcessingOrder order = findOrder(releaseId);
			ProcessingFacility pf = null;
			KubeConfig kc = productionPlanner.getKubeConfig(facility);
			if (kc != null) {
				pf = kc.getProcessingFacility();
			}
			if (order != null && pf != null) {
				Messages msg = orderUtil.plan(order, pf);
				if (msg.isTrue()) {
					RestOrder ro = RestUtil.createRestOrder(order);
					String message = msg.formatWithPrefix(order.getIdentifier());
					HttpHeaders responseHeaders = new HttpHeaders();
					responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), message);
					return new ResponseEntity<>(ro, responseHeaders, HttpStatus.CREATED);
				} else {
					RestOrder ro = RestUtil.createRestOrder(order);
					String message = msg.formatWithPrefix(order.getIdentifier());
					HttpHeaders responseHeaders = new HttpHeaders();
					responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
					return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
				}
			} else {
				String message = "";
				if (order == null && pf == null) {
					message = Messages.ORDER_FACILITY_NOT_EXIST.formatWithPrefix(releaseId, facility);
					Messages.ORDER_FACILITY_NOT_EXIST.log(logger, releaseId, facility);
				} else {
					if (order == null) {
						message = Messages.ORDER_NOT_EXIST.formatWithPrefix(releaseId);
						Messages.ORDER_NOT_EXIST.log(logger, releaseId);
					}
					if (pf == null) {
						message = Messages.FACILITY_NOT_EXIST.formatWithPrefix(facility);
						Messages.FACILITY_NOT_EXIST.log(logger, facility);
					}
				}
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			}
		} else {
			String message = "";
			if (releaseId == null && facility == null) {
				message = Messages.PARAM_ID_FACILITY_NOT_SET.formatWithPrefix();
				Messages.PARAM_ID_FACILITY_NOT_SET.log(logger);
			} else {
				if (releaseId == null) {
					message = Messages.PARAM_ID_NOT_SET.formatWithPrefix();
					Messages.PARAM_ID_NOT_SET.log(logger);
				}
				if (facility == null) {
					message = Messages.PARAM_FACILITY_NOT_SET.formatWithPrefix();
					Messages.PARAM_FACILITY_NOT_SET.log(logger);
				}
			}
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * Release processing order of id
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<RestOrder> releaseOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			Messages msg = orderUtil.resume(order);
			boolean found = false;
			for (ProcessingFacility pf : orderUtil.getProcessingFacilities(order)) {
				KubeConfig kc = productionPlanner.getKubeConfig(pf.getName());
				if (kc != null) {
					found = true;
					UtilService.getJobStepUtil().checkOrderToRun(kc, order);
				}
			}
			if (!found) {
				UtilService.getJobStepUtil().checkForJobStepsToRun();
			}
			if (msg.isTrue()) {				
				// canceled
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(orderId));
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(orderId));
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		Messages.ORDER_NOT_EXIST.log(logger, orderId);
		String message = Messages.ORDER_NOT_EXIST.formatWithPrefix(orderId);
    	HttpHeaders responseHeaders = new HttpHeaders();
    	responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	/**
	 * Cancel processing order of id
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<RestOrder> cancelOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			Messages msg = orderUtil.cancel(order);
			if (msg.isTrue()) {
				// canceled
				String message = msg.formatWithPrefix(orderId);
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), message);
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not cancel
				String message = msg.formatWithPrefix(orderId);
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		Messages.ORDER_NOT_EXIST.log(logger, orderId);
		String message = Messages.ORDER_NOT_EXIST.formatWithPrefix(orderId);
    	HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	/**
	 * Close processing order of id
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<RestOrder> closeOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			Messages msg = orderUtil.close(order);
			if (msg.isTrue()) {
				// canceled
				String message = msg.formatWithPrefix(orderId);
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), message);
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not cancel
				String message = msg.formatWithPrefix(orderId);
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		Messages.ORDER_NOT_EXIST.log(logger, orderId);
		String message = Messages.ORDER_NOT_EXIST.formatWithPrefix(orderId);
    	HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}

	/**
	 * Suspend processing order of id
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<RestOrder> suspendOrder(String orderId, Boolean force) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			Messages msg = orderUtil.suspend(order, force);
			if (msg.isTrue()) {
				// canceled
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(orderId));
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(orderId));
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		Messages.ORDER_NOT_EXIST.log(logger, orderId);
		String message = Messages.ORDER_NOT_EXIST.formatWithPrefix(orderId);
    	HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}
	
	/**
	 * Retry processing order of id
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<RestOrder> retryOrder(String orderId) {
		ProcessingOrder order = this.findOrder(orderId);
		if (order != null) {
			Messages msg = orderUtil.retry(order);
			if (msg.isTrue()) {
				// canceled
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_SUCCESS.getDescription(), msg.formatWithPrefix(orderId));
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.OK);
			} else {
				// already running or at end, could not suspend
				RestOrder ro = RestUtil.createRestOrder(order);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), msg.formatWithPrefix(orderId));
				return new ResponseEntity<>(ro, responseHeaders, HttpStatus.NOT_MODIFIED);
			}
		}
		Messages.ORDER_NOT_EXIST.log(logger, orderId);
		String message = Messages.ORDER_NOT_EXIST.formatWithPrefix(orderId);
    	HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(Messages.HTTP_HEADER_WARNING.getDescription(), message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
	}


	/**
	 * Find a processing order by DB id or identifier.
	 * 
	 * @param orderId DB id or identifier
	 * @return Order found
	 */
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
