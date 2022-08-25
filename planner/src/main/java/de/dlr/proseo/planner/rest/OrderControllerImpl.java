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
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.planner.Messages;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.model.rest.OrderController;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.planner.rest.model.RestUtil;
import de.dlr.proseo.planner.util.OrderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

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
	
	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;

    @Autowired
    private OrderUtil orderUtil;


	/**
	 * Get all processing orders
	 * 
	 */
	@Override
	public ResponseEntity<List<RestOrder>> getOrders() {
		if (logger.isTraceEnabled()) logger.trace(">>> getOrders()");
		
		try {
			List<RestOrder> list = new ArrayList<RestOrder>();
			try {
				productionPlanner.acquireThreadSemaphore("getOrders");
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				transactionTemplate.execute((status) -> {
					Iterable<ProcessingOrder> orders = RepositoryService.getOrderRepository().findAll();
					for (ProcessingOrder po : orders) {
						RestOrder ro = getRestOrder(po.getId());
						list.add(ro);			
					}
					return null;
				});
				productionPlanner.releaseThreadSemaphore("getOrders");	
			} catch (Exception e) {
				productionPlanner.releaseThreadSemaphore("getOrders");	
				String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());			
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
			}			
			Messages.ORDERS_RETRIEVED.log(logger);

			return new ResponseEntity<>(list, HttpStatus.OK);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Get processing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> getOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> getOrder({})", orderId);
		
		try {
			ProcessingOrder order = this.findOrder(orderId);
			
			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			} else {
				RestOrder ro = getRestOrder(order.getId());

				Messages.ORDER_RETRIEVED.log(logger, orderId);

				return new ResponseEntity<>(ro, HttpStatus.OK);
			}
		} catch (Exception e) {	
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	/**
	 * Approve processing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> approveOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> approveOrder({})", orderId);

		ProcessingOrder order = this.findOrder(orderId);
		if (null == order) {
			String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		} else {
			Messages msg = null;
			try {
				productionPlanner.acquireThreadSemaphore("approveOrder");
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				msg = transactionTemplate.execute((status) -> {
					ProcessingOrder orderx = this.findOrderPrim(orderId);
					return orderUtil.approve(orderx);
				});
				productionPlanner.releaseThreadSemaphore("approveOrder");	
			} catch (Exception e) {
				productionPlanner.releaseThreadSemaphore("approveOrder");	
				String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());			
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			if (msg.isTrue()) {
				// approved
				RestOrder ro = getRestOrder(order.getId());

				return new ResponseEntity<>(ro, HttpStatus.OK);
			} else {
				// already running or at end, could not approve
				String message = msg.format(orderId);

				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
			}
		}
	}

	
	/**
	 * Reset processing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> resetOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> resetOrder({})", orderId);
		
		try {
			ProcessingOrder order = this.findOrder(orderId);
			
			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			} else {
				Messages msg = orderUtil.reset(order);
				// Already logged
				
				if (msg.isTrue()) {
					// reset
					RestOrder ro = getRestOrder(order.getId());

					return new ResponseEntity<>(ro, HttpStatus.OK);
				} else {
					// illegal state for reset
					String message = msg.format(orderId);

					return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	
	/**
	 * Delete processing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> deleteOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteOrder({})", orderId);
		
		try {
			ProcessingOrder order = this.findOrder(orderId);
			
			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			} else {
				Messages msg = null;
				try {
					productionPlanner.acquireThreadSemaphore("deleteOrder");
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					msg = transactionTemplate.execute((status) -> {
						ProcessingOrder orderx = this.findOrderPrim(orderId);
						return orderUtil.delete(orderx);
					});
					productionPlanner.releaseThreadSemaphore("deleteOrder");	
				} catch (Exception e) {
					productionPlanner.releaseThreadSemaphore("deleteOrder");	
					String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());			
					return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}

				if (msg.isTrue()) {
					// cancelled
					RestOrder ro = getRestOrder(order.getId());

					return new ResponseEntity<>(ro, HttpStatus.OK);
				} else {
					// illegal state for delete
					String message = msg.format(orderId);

					return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
				}
			}
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Plan processing order of id on processing facility
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> planOrder(String releaseId, String facility) {
		if (logger.isTraceEnabled()) logger.trace(">>> planOrder({}, {})", releaseId, facility);
		
		if (null == releaseId || null == facility) {
			String message = null;
			if (releaseId == null && facility == null) {
				message = Messages.PARAM_ID_FACILITY_NOT_SET.log(logger);
			} else if (releaseId == null) {
				message = Messages.PARAM_ID_NOT_SET.log(logger);
			} else if (facility == null) {
				message = Messages.PARAM_FACILITY_NOT_SET.log(logger);
			}
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		}
		
		try {
			ProcessingOrder order = findOrder(releaseId);

			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, releaseId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			}
			
			// Check the status of the requested processing facility
			KubeConfig kc = productionPlanner.updateKubeConfig(facility);
			if (null == kc) {
				String message = Messages.FACILITY_NOT_EXIST.log(logger, facility);

		    	return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			}
			
			ProcessingFacility pf = kc.getProcessingFacility();
			if (pf.getFacilityState() != FacilityState.RUNNING && pf.getFacilityState() != FacilityState.STARTING) {
				String message = Messages.FACILITY_NOT_AVAILABLE.log(logger, facility, pf.getFacilityState().toString());

		    	return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
			}
			
			Messages msg = orderUtil.plan(order.getId(), pf);
			if (msg.isTrue()) {
				RestOrder ro = getRestOrder(order.getId());

				return new ResponseEntity<>(ro, HttpStatus.CREATED);
			} else if (msg.getCode() == Messages.ORDER_PRODUCT_EXIST.getCode()) {
				RestOrder ro = getRestOrder(order.getId());

				return new ResponseEntity<>(ro, HttpStatus.CREATED);
			} else if (msg.getType() == Messages.MessageType.W) {
				RestOrder ro = getRestOrder(order.getId());
				String message = msg.formatWithPrefix();					

				return new ResponseEntity<>(ro, Messages.errorHeaders(message), HttpStatus.NOT_MODIFIED);
			} else {
				String message = msg.formatWithPrefix();					

				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Release processing order of id (at the moment the same functionality as resumeOrder)
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> releaseOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> releaseOrder({})", orderId);
		
		try {
			ProcessingOrder order = this.findOrder(orderId);
			
			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			}
			
			Messages msg = orderUtil.resume(order);
			
			// Check whether the release triggers any job steps
			// This is already done during RELEASING
						
			if (msg.isTrue()) {
				// resumed
				RestOrder ro = getRestOrder(order.getId());

				return new ResponseEntity<>(ro, HttpStatus.OK);
			} else {
				// illegal state for resume
				String message = msg.format(orderId);

				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Resume processing order of id (currently an alias for releaseOrder)
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> resumeOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> resumeOrder({})", orderId);
		
		return releaseOrder(orderId);
	}

	/**
	 * Cancel processing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> cancelOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancelOrder({})", orderId);

		ProcessingOrder order = this.findOrder(orderId);

		if (null == order) {
			String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		}

		Messages msg = null;
		try {
			productionPlanner.acquireThreadSemaphore("cancelOrder");
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			msg = transactionTemplate.execute((status) -> {
				ProcessingOrder orderx = this.findOrderPrim(orderId);
				return orderUtil.cancel(orderx);
			});
			productionPlanner.releaseThreadSemaphore("cancelOrder");	
		} catch (Exception e) {
			productionPlanner.releaseThreadSemaphore("cancelOrder");	
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (msg.isTrue()) {
			// cancelled
			RestOrder ro = getRestOrder(order.getId());

			return new ResponseEntity<>(ro, HttpStatus.OK);
		} else {
			// illegal state for cancel
			String message = msg.format(orderId);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Close processing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> closeOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> closeOrder({})", orderId);

		ProcessingOrder order = this.findOrder(orderId);

		if (null == order) {
			String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
		}
		Messages msg = null;
		try {
			productionPlanner.acquireThreadSemaphore("closeOrder");
			msg = orderUtil.close(order.getId());
			msg.log(logger, order.getIdentifier());
			productionPlanner.releaseThreadSemaphore("closeOrder");	
		} catch (Exception e) {
			productionPlanner.releaseThreadSemaphore("closeOrder");	
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (msg.isTrue()) {
			// cancelled
			RestOrder ro = getRestOrder(order.getId());

			return new ResponseEntity<>(ro, HttpStatus.OK);
		} else {
			// illegal state for close
			String message = msg.format(orderId);

			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Suspend processing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> suspendOrder(String orderId, Boolean force) {
		if (logger.isTraceEnabled()) logger.trace(">>> suspendOrder({}, force: {})", orderId, force);
		
		try {
			ProcessingOrder order = this.findOrder(orderId);

			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			}
			if (null == force) {
				force = false;
			}
			
			if (force) {
				// "Suspend force" is only allowed, if the processing facilities are available
				for (ProcessingFacility pf : orderUtil.getProcessingFacilities(order.getId())) {
					if (pf.getFacilityState() != FacilityState.RUNNING) {
						String message = Messages.FACILITY_NOT_AVAILABLE.log(logger, pf.getName(),
								pf.getFacilityState().toString());

						return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
					}
				} 
			}

			Messages msg = orderUtil.prepareSuspend(order.getId(), force);
			if (msg.isTrue()) {
				msg = orderUtil.suspend(order.getId(), force);
			}
			// Already logged
			
			if (msg.isTrue()) {
				// suspended
				
				RestOrder ro = getRestOrder(order.getId());

				return new ResponseEntity<>(ro, HttpStatus.OK);
			} else {
				// illegal state for suspend
				String message = msg.format(orderId);

				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Retry processing order of id
	 * 
	 */
	@Override
	public ResponseEntity<RestOrder> retryOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> retryOrder({})", orderId);
		
		try {
			ProcessingOrder order = this.findOrder(orderId);

			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			}
			
			Messages msg = null;
			try {
				productionPlanner.acquireThreadSemaphore("retryOrder");
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				msg = transactionTemplate.execute((status) -> {
					ProcessingOrder orderx = this.findOrderPrim(orderId);
					return orderUtil.retry(orderx);
				});
				productionPlanner.releaseThreadSemaphore("retryOrder");	
			} catch (Exception e) {
				productionPlanner.releaseThreadSemaphore("retryOrder");	
				String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());			
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			if (msg.isTrue()) {
				// approved
				RestOrder ro = getRestOrder(order.getId());

				return new ResponseEntity<>(ro, HttpStatus.OK);
			} else {
				// illegal state for retry
				String message = msg.format(orderId);

				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	/**
	 * Find a processing order by DB id or identifier.
	 * 
	 * @param orderId DB id or identifier
	 * @return Order found
	 */
	private ProcessingOrder findOrderPrim(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> findOrderPrim({})", orderId);
				
		String missionCode = securityService.getMission();

		ProcessingOrder order = null;
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		order = transactionTemplate.execute((status) -> {
			ProcessingOrder orderx = null;
			try {
				Long id = Long.valueOf(orderId);
				Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
				if (orderOpt.isPresent()) {
					orderx = orderOpt.get();
				}
			} catch (NumberFormatException nfe) {
				// use orderId as identifier
			}
			if (orderx == null) {
				orderx = RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(missionCode, orderId);
			}

			if (null == orderx) {
				return null;
			}

			// Ensure user is authorized for the mission of the order
			if (!missionCode.equals(orderx.getMission().getCode())) {
				Messages.ILLEGAL_CROSS_MISSION_ACCESS.log(logger, orderx.getMission().getCode(), missionCode);
				return null;			
			}
			return orderx;
		});
		return order;
	}

	private ProcessingOrder findOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> findOrder({})", orderId);
		ProcessingOrder order = null;
		try {
			productionPlanner.acquireThreadSemaphore("approveOrder");
			order = this.findOrderPrim(orderId);
			productionPlanner.releaseThreadSemaphore("approveOrder");	
		} catch (Exception e) {
			productionPlanner.releaseThreadSemaphore("approveOrder");	
			Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());	
		}
		return order;
	}
	
	private RestOrder getRestOrder(long id) {
		productionPlanner.acquireThreadSemaphore("getRestOrder");
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		RestOrder answer = transactionTemplate.execute((status) -> {
			RestOrder ro = null;
			ProcessingOrder order = null;
			Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
			if (orderOpt.isPresent()) {
				order = orderOpt.get();
			}
			ro = RestUtil.createRestOrder(order);
			return ro;
		});
		productionPlanner.releaseThreadSemaphore("getRestOrder");
		return answer;
	}

	
}
