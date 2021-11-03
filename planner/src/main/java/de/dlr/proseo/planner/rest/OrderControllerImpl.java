/**
 * OrderControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.Query;

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
import de.dlr.proseo.planner.util.UtilService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
	@Transactional
	public ResponseEntity<List<RestOrder>> getOrders() {
		if (logger.isTraceEnabled()) logger.trace(">>> getOrders()");
		
		try {
			Iterable<ProcessingOrder> orders = RepositoryService.getOrderRepository().findAll();
			List<RestOrder> list = new ArrayList<RestOrder>();

			for (ProcessingOrder po : orders) {
				RestOrder ro = RestUtil.createRestOrder(po);
				list.add(ro);			
			}
			
			Messages.ORDERS_RETRIEVED.log(logger);

			return new ResponseEntity<>(list, HttpStatus.OK);
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ResponseEntity<List<RestOrder>> getAndSelectOrders(String mission, String identifier, String[] state, 
			String[] productClass, String startTime, String stopTime, Long recordFrom, Long recordTo, String[] orderBy) {
		if (logger.isTraceEnabled()) logger.trace(">>> getAndSelectOrders({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})", mission, identifier, state, 
				productClass, startTime, stopTime, recordFrom, recordTo, orderBy);
		String missionCode = securityService.getMission(); 
		if (null == mission) {
			mission = missionCode;
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(mission)) {
				throw new SecurityException(Messages.ILLEGAL_CROSS_MISSION_ACCESS.log(logger,
						mission, missionCode));
			} 
		}
		
		try {
			List<RestOrder> list = orderUtil.getAndSelectOrders(missionCode, identifier, state, productClass, startTime, stopTime, recordFrom, recordTo, orderBy);
						
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
	@Transactional
	public ResponseEntity<RestOrder> getOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> getOrder({})", orderId);
		
		try {
			ProcessingOrder order = this.findOrder(orderId);
			
			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			} else {
				RestOrder ro = RestUtil.createRestOrder(order);

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
	@Transactional
	public ResponseEntity<RestOrder> approveOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> approveOrder({})", orderId);
		
		try {
			ProcessingOrder order = this.findOrder(orderId);
			
			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			} else {
				Messages msg = orderUtil.approve(order);
				// Already logged
				
				if (msg.isTrue()) {
					// approved
					RestOrder ro = RestUtil.createRestOrder(order);

					return new ResponseEntity<>(ro, HttpStatus.OK);
				} else {
					// already running or at end, could not approve
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
	 * Reset processing order of id
	 * 
	 */
	@Override
	@Transactional
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
					RestOrder ro = RestUtil.createRestOrder(order);

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
	@Transactional
	public ResponseEntity<RestOrder> deleteOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteOrder({})", orderId);
		
		try {
			ProcessingOrder order = this.findOrder(orderId);
			
			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			} else {
				Messages msg = orderUtil.delete(order);
				// Already logged
				
				if (msg.isTrue()) {
					// deleted
					RestOrder ro = RestUtil.createRestOrder(order);

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
	@Transactional
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
			
			Messages msg = orderUtil.plan(order, pf);
			if (msg.isTrue()) {
				RestOrder ro = RestUtil.createRestOrder(order);

				return new ResponseEntity<>(ro, HttpStatus.CREATED);
			} else if (msg.getCode() == Messages.ORDER_PRODUCT_EXIST.getCode()) {
				RestOrder ro = RestUtil.createRestOrder(order);

				return new ResponseEntity<>(ro, HttpStatus.CREATED);
			} else if (msg.getType() == Messages.MessageType.W) {
				RestOrder ro = RestUtil.createRestOrder(order);
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
	@Transactional
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
			boolean found = false;
			for (ProcessingFacility pf : orderUtil.getProcessingFacilities(order)) {
				if (pf.getFacilityState() != FacilityState.RUNNING) {
					continue;
				}

				KubeConfig kc = productionPlanner.getKubeConfig(pf.getName());
				if (kc != null) {
					found = true;
					UtilService.getJobStepUtil().checkOrderToRun(kc, order);
				}
			}
			if (!found) {
				UtilService.getJobStepUtil().checkForJobStepsToRun();
			}
			
			// Already logged
			
			if (msg.isTrue()) {
				// resumed
				RestOrder ro = RestUtil.createRestOrder(order);

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
	@Transactional
	public ResponseEntity<RestOrder> resumeOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> resumeOrder({})", orderId);
		
		return releaseOrder(orderId);
	}

	/**
	 * Cancel processing order of id
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<RestOrder> cancelOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> cancelOrder({})", orderId);
		
		try {
			ProcessingOrder order = this.findOrder(orderId);

			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			}
			
			Messages msg = orderUtil.cancel(order);
			// Already logged
			
			if (msg.isTrue()) {
				// cancelled
				RestOrder ro = RestUtil.createRestOrder(order);

				return new ResponseEntity<>(ro, HttpStatus.OK);
			} else {
				// illegal state for cancel
				String message = msg.format(orderId);

				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Close processing order of id
	 * 
	 */
	@Override
	@Transactional
	public ResponseEntity<RestOrder> closeOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> closeOrder({})", orderId);
		
		try {
			ProcessingOrder order = this.findOrder(orderId);

			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			}
			
			Messages msg = orderUtil.close(order);
			// Already logged
			
			if (msg.isTrue()) {
				// closed
				RestOrder ro = RestUtil.createRestOrder(order);

				return new ResponseEntity<>(ro, HttpStatus.OK);
			} else {
				// illegal state for close
				String message = msg.format(orderId);

				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			String message = Messages.RUNTIME_EXCEPTION.log(logger, e.getMessage());
			
			return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Suspend processing order of id
	 * 
	 */
	@Override
	@Transactional
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
				for (ProcessingFacility pf : orderUtil.getProcessingFacilities(order)) {
					if (pf.getFacilityState() != FacilityState.RUNNING) {
						String message = Messages.FACILITY_NOT_AVAILABLE.log(logger, pf.getName(),
								pf.getFacilityState().toString());

						return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.BAD_REQUEST);
					}
				} 
			}
			
			Messages msg = orderUtil.suspend(order, force);
			// Already logged
			
			if (msg.isTrue()) {
				// suspended
				RestOrder ro = RestUtil.createRestOrder(order);

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
	@Transactional
	public ResponseEntity<RestOrder> retryOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> retryOrder({})", orderId);
		
		try {
			ProcessingOrder order = this.findOrder(orderId);

			if (null == order) {
				String message = Messages.ORDER_NOT_EXIST.log(logger, orderId);
				
				return new ResponseEntity<>(Messages.errorHeaders(message), HttpStatus.NOT_FOUND);
			}
			
			Messages msg = orderUtil.retry(order);
			// Already logged
			
			if (msg.isTrue()) {
				// retrying
				RestOrder ro = RestUtil.createRestOrder(order);

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
	@Transactional
	private ProcessingOrder findOrder(String orderId) {
		if (logger.isTraceEnabled()) logger.trace(">>> findOrder({})", orderId);
				
		String missionCode = securityService.getMission();

		ProcessingOrder order = null;
		try {
			Long id = Long.valueOf(orderId);
			Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
			if (orderOpt.isPresent()) {
				order = orderOpt.get();
			}
		} catch (NumberFormatException nfe) {
			// use orderId as identifier
		}
		if (order == null) {
			order = RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(missionCode, orderId);
		}
		
		if (null == order) {
			return null;
		}
		
		// Ensure user is authorized for the mission of the order
		if (!missionCode.equals(order.getMission().getCode())) {
			Messages.ILLEGAL_CROSS_MISSION_ACCESS.log(logger, order.getMission().getCode(), missionCode);
			return null;			
		}
		
		return order;
	}

	
}
