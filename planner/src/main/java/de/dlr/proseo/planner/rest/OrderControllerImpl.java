/**
 * OrderControllerImpl.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.planner.PlannerResultMessage;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.Job;
import de.dlr.proseo.model.JobStep;
import de.dlr.proseo.model.JobStep.JobStepState;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.enums.OrderSource;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.planner.ProductionPlanner;
import de.dlr.proseo.planner.ProductionPlannerSecurityConfig;
import de.dlr.proseo.planner.kubernetes.KubeConfig;
import de.dlr.proseo.model.rest.OrderController;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.planner.rest.model.RestUtil;
import de.dlr.proseo.planner.util.OrderUtil;

import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring MVC controller for the prosEO planner; implements the services required to plan and handle processing orders.
 * 
 * @author Ernst Melchinger
 */
@Component
public class OrderControllerImpl implements OrderController {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(OrderControllerImpl.class);

	/** Utility class for handling HTTP headers */
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PLANNER);

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** The production planner instance */
	@Autowired
	private ProductionPlanner productionPlanner;

	/** The production planner security configuration */
	@Autowired
	ProductionPlannerSecurityConfig securityConfig;

	/** Utility class for handling processing orders */
	@Autowired
	private OrderUtil orderUtil;

	/**
	 * Get all processing orders (deprecated).
	 * 
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return ResponseEntity containing a list of RestOrder and HTTP status
	 */
	@Override
	@Deprecated
	public ResponseEntity<List<RestOrder>> getOrders(HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getOrders()");

		try {
			List<RestOrder> list = new ArrayList<RestOrder>();
			try {
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
				transactionTemplate.setReadOnly(true);
				transactionTemplate.execute((status) -> {
					Iterable<ProcessingOrder> orders = RepositoryService.getOrderRepository().findAll();
					for (ProcessingOrder processingOrder : orders) {
						RestOrder restOrder = getRestOrder(processingOrder.getId());
						list.add(restOrder);
					}
					return null;
				});
			} catch (Exception e) {
				String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

				if (logger.isDebugEnabled())
					logger.debug("... exception stack trace: ", e);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			logger.log(PlannerMessage.ORDERS_RETRIEVED);

			return new ResponseEntity<>(list, HttpStatus.OK);
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Get processing order by ID (deprecated).
	 * 
	 * @param orderId     the ID of the processing order
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return ResponseEntity containing a RestOrder and HTTP status
	 */
	@Override
	@Deprecated
	public ResponseEntity<RestOrder> getOrder(String orderId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getOrder({})", orderId);

		try {
			ProcessingOrder order = this.findOrder(orderId);

			if (null == order) {
				String message = logger.log(PlannerMessage.ORDER_NOT_EXIST, orderId);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			} else {
				RestOrder restOrder = getRestOrder(order.getId());

				logger.log(PlannerMessage.ORDER_RETRIEVED, orderId);

				return new ResponseEntity<>(restOrder, HttpStatus.OK);
			}
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Approve processing order by ID (deprecated).
	 * 
	 * @param orderId     the ID of the processing order
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return ResponseEntity containing a RestOrder and HTTP status
	 */
	@Override
	@Deprecated
	public ResponseEntity<RestOrder> approveOrder(String orderId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> approveOrder({})", orderId);

		ProcessingOrder order = this.findOrder(orderId);
		if (null == order) {
			String message = logger.log(PlannerMessage.ORDER_NOT_EXIST, orderId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		} else {
			PlannerResultMessage msg = new PlannerResultMessage(null);
			try {
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
				msg = transactionTemplate.execute((status) -> {
					ProcessingOrder orderx = this.findOrderPrim(orderId);
					return orderUtil.approve(orderx);
				});
			} catch (Exception e) {
				String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

				if (logger.isDebugEnabled())
					logger.debug("... exception stack trace: ", e);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			if (msg.getSuccess()) {
				// approved
				RestOrder restOrder = getRestOrder(order.getId());

				return new ResponseEntity<>(restOrder, HttpStatus.OK);
			} else {
				// already running or at end, could not approve
				return new ResponseEntity<>(http.errorHeaders(msg.getText()), HttpStatus.BAD_REQUEST);
			}
		}
	}

	/**
	 * Reset processing order by ID.
	 * 
	 * @param orderId     the ID of the processing order
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return ResponseEntity containing a RestOrder and HTTP status
	 */
	@Override
	public ResponseEntity<RestOrder> resetOrder(String orderId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> resetOrder({})", orderId);

		try {
			// Find the processing order by its ID
			ProcessingOrder order = this.findOrder(orderId);

			if (null == order) {
				// Log and return a NOT_FOUND response if the order doesn't exist
				String message = logger.log(PlannerMessage.ORDER_NOT_EXIST, orderId);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			} else {
				// Attempt to reset the order
				PlannerResultMessage msg = orderUtil.reset(order);
				// Any exceptions in the reset process are already logged inside orderUtil.reset()

				if (msg.getSuccess()) {
					// If the order is successfully reset, retrieve the updated RestOrder
					RestOrder restOrder = getRestOrder(order.getId());

					return new ResponseEntity<>(restOrder, HttpStatus.OK);
				} else {
					// Resetting the order failed due to an illegal state, return a BAD_REQUEST response
					return new ResponseEntity<>(http.errorHeaders(msg.getText()), HttpStatus.BAD_REQUEST);
				}
			}
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Delete processing order by ID.
	 * 
	 * @param orderId     the ID of the processing order
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return ResponseEntity containing a RestOrder and HTTP status
	 */
	@Override
	public ResponseEntity<RestOrder> deleteOrder(String orderId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteOrder({})", orderId);

		try {
			// Find the processing order by its ID
			ProcessingOrder order = this.findOrder(orderId);

			// Check if the order exists
			if (null == order) {
				String message = logger.log(PlannerMessage.ORDER_NOT_EXIST, orderId);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			} else {
				PlannerResultMessage msg = new PlannerResultMessage(null);
				try {
					// Execute the deletion within a transaction to ensure consistency
					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							msg = transactionTemplate.execute((status) -> {
								ProcessingOrder orderx = this.findOrderPrim(orderId);
								// Attempt to delete the order
								return orderUtil.delete(orderx);
							});
							break;
						} catch (CannotAcquireLockException e) {
							// Handle database concurrency issues by retrying
							if (logger.isDebugEnabled())
								logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled())
									logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}
					}
				} catch (Exception e) {
					String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

					if (logger.isDebugEnabled())
						logger.debug("... exception stack trace: ", e);

					return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
				}

				if (msg.getSuccess()) {
					// If the deletion is successful, retrieve and return the updated
					RestOrder restOrder = getRestOrder(order.getId());

					return new ResponseEntity<>(restOrder, HttpStatus.OK);
				} else {
					// Deletion failed due to an illegal state, return a BAD_REQUEST response
					return new ResponseEntity<>(http.errorHeaders(msg.getText()), HttpStatus.BAD_REQUEST);
				}
			}
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Plan processing order of id on processing facility
	 * 
	 * @param releaseId   the processing order database ID
	 * @param facility    the processing facility name
	 * @param wait        indicates whether to wait for the order planning to complete
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return HTTP status "CREATED" and the updated REST order, if the planning succeeded, or HTTP status "NOT MODIFIED", an error
	 *         message and the original REST order, if a warning was issued, or HTTP status "NOT FOUND" and an error message, if
	 *         either the order or the facility cannot be found, or HTTP status "BAD REQUEST" and an error message, if the planning
	 *         failed for some reason, or HTTP status "INTERNAL SERVER ERROR" and an error message, if any unforeseen error occurred
	 */
	@Override
	public ResponseEntity<RestOrder> planOrder(String releaseId, String facility, Boolean wait, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> planOrder({}, {})", releaseId, facility);

		// Ensure releaseId and facility are not null
		if (null == releaseId || null == facility) {
			String message = null;
			if (releaseId == null && facility == null) {
				message = logger.log(PlannerMessage.PARAM_ID_FACILITY_NOT_SET);
			} else if (releaseId == null) {
				message = logger.log(PlannerMessage.PARAM_ID_NOT_SET);
			} else if (facility == null) {
				message = logger.log(PlannerMessage.PARAM_FACILITY_NOT_SET);
			}
			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		}

		try {
			// Find the processing order by its releaseId
			ProcessingOrder order = findOrder(releaseId);

			// Ensure order exists
			if (null == order) {
				String message = logger.log(PlannerMessage.ORDER_NOT_EXIST, releaseId);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			}

			// Check the status of the requested processing facility
			KubeConfig kubeConfig = productionPlanner.updateKubeConfig(facility);
			if (null == kubeConfig) {
				String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, facility);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			}

			ProcessingFacility processingFacility = kubeConfig.getProcessingFacility();
			if (processingFacility.getFacilityState() == FacilityState.DISABLED) {
				String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, facility,
						processingFacility.getFacilityState().toString());

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
			}

			if (wait == null) {
				wait = false;
			}

			// Plan the order on the processing facility
			PlannerResultMessage msg = orderUtil.plan(order.getId(), processingFacility, wait);
			if (msg.getSuccess()) {
				// If planning is successful, retrieve and return the updated order
				RestOrder restOrder = getRestOrder(order.getId());

				return new ResponseEntity<>(restOrder, HttpStatus.CREATED);
			} else if (msg.getCode() == PlannerMessage.ORDER_PRODUCT_EXIST.getCode()) {
				// If the order product already exists, return the RestOrder with CREATED status
				RestOrder restOrder = getRestOrder(order.getId());

				return new ResponseEntity<>(restOrder, HttpStatus.CREATED);
			} else if (msg.getLevel() == Level.WARN) {
				// If a warning is issued during planning, return the original RestOrder with NOT_MODIFIED status
				RestOrder restOrder = getRestOrder(order.getId());
				return new ResponseEntity<>(restOrder, http.errorHeaders(msg.getText()), HttpStatus.NOT_MODIFIED);
			} else {
				// If planning failed for some reason, return a BAD_REQUEST response with an error message
				return new ResponseEntity<>(http.errorHeaders(msg.getText()), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Release processing order of id (at the moment the same functionality as resumeOrder)
	 * 
	 * @param orderId     the processing order database ID
	 * @param wait        indicates whether to wait for the order releasing to complete
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return HTTP status "OK" and the updated REST order, if the releasing succeeded, or HTTP status "NOT FOUND" and an error
	 *         message, if the order cannot be found, or HTTP status "BAD REQUEST" and an error message, if the releasing failed for
	 *         some reason, or HTTP status "INTERNAL SERVER ERROR" and an error message, if any unforeseen error occurred
	 */
	@Override
	public ResponseEntity<RestOrder> releaseOrder(String orderId, Boolean wait, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> releaseOrder({})", orderId);

		String[] userPassword = securityConfig.parseAuthenticationHeader(httpHeaders.getFirst(HttpHeaders.AUTHORIZATION));
		try {
			ProcessingOrder order = this.findOrder(orderId);

			if (null == order) {
				String message = logger.log(PlannerMessage.ORDER_NOT_EXIST, orderId);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			}
			// Check the status of the requested processing facility
			for (ProcessingFacility processingFacilityX : orderUtil.getProcessingFacilities(order.getId())) {
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
				final ResponseEntity<RestOrder> response = transactionTemplate.execute((status) -> {
					Optional<ProcessingFacility> processingFacilityOpt = RepositoryService.getFacilityRepository()
						.findById(processingFacilityX.getId());
					if (processingFacilityOpt.isPresent()) {
						ProcessingFacility processingFacility = processingFacilityOpt.get();
						KubeConfig kubeConfig = productionPlanner.updateKubeConfig(processingFacility.getName());
						if (null == kubeConfig) {
							String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, processingFacility.getName());

							return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
						}
						if (processingFacility.getFacilityState() != FacilityState.RUNNING
								&& processingFacility.getFacilityState() != FacilityState.STARTING) {
							String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, processingFacility.getName(),
									processingFacility.getFacilityState().toString());
							if (processingFacility.getFacilityState() == FacilityState.DISABLED) {
								return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
							} else {
								return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.SERVICE_UNAVAILABLE);
							}
						}
					}
					return null;
				});
				if (response != null) {
					return response;
				}
			}

			if (wait == null) {
				wait = false;
			}

			// Release the order (same as resume)
			PlannerResultMessage msg = orderUtil.resume(order, wait, userPassword[0], userPassword[1]);

			// Check whether the release triggers any job steps
			// This is already done during RELEASING

			if (msg.getSuccess()) {
				// resumed
				RestOrder restOrder = getRestOrder(order.getId());
				// handle special ODIP case
				if (restOrder.getOrderSource().equals(OrderSource.ODIP.toString())) {
					// read database object and check state of job steps.
					// They have to be released and not waiting input

					// TODO Check: Since the code below (from "if (orderName != null)") has been commented out, the following
					// lines have no meaning any more - or have they?

					TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

					// used to identify the order and missing input (if not null) as well
					try {
						transactionTemplate.setReadOnly(true);
						transactionTemplate.execute((status) -> {
							Optional<ProcessingOrder> opt = RepositoryService.getOrderRepository().findById(order.getId());
							if (opt.isPresent()) {
								ProcessingOrder orderx = opt.get();
								for (Job job : orderx.getJobs()) {
									for (JobStep jobStep : job.getJobSteps()) {
										if (jobStep.getJobStepState() == JobStepState.WAITING_INPUT) {
											return orderx.getIdentifier();
										}
									}
								}
								return null;
							}
							return null;
						});
					} catch (Exception e) {
						String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

						if (logger.isDebugEnabled())
							logger.debug("... exception stack trace: ", e);

						return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
					}
				}

				return new ResponseEntity<>(restOrder, HttpStatus.OK);
			} else {
				// Resuming failed due to an illegal state, return a BAD_REQUEST response
				return new ResponseEntity<>(http.errorHeaders(msg.getText()), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Resume processing order (currently an alias for releaseOrder).
	 * 
	 * @param orderId     the processing order database ID
	 * @param wait        indicates whether to wait for the order releasing to complete
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return ResponseEntity containing a RestOrder and HTTP status
	 */
	@Override
	public ResponseEntity<RestOrder> resumeOrder(String orderId, Boolean wait, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> resumeOrder({})", orderId);

		if (wait == null) {
			wait = false;
		}
		return releaseOrder(orderId, wait, httpHeaders);
	}

	/**
	 * Cancel processing order by ID.
	 * 
	 * @param orderId     the ID of the processing order
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return ResponseEntity containing a RestOrder and HTTP status
	 */
	@Override
	public ResponseEntity<RestOrder> cancelOrder(String orderId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> cancelOrder({})", orderId);

		// Find the processing order by its orderId
		ProcessingOrder order = this.findOrder(orderId);

		// Ensure the order exists
		if (null == order) {
			String message = logger.log(PlannerMessage.ORDER_NOT_EXIST, orderId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		}

		PlannerResultMessage msg = new PlannerResultMessage(null);
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
				try {
					// Execute the cancellation operation within a transaction
					transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
					msg = transactionTemplate.execute((status) -> {
						ProcessingOrder orderx = this.findOrderPrim(orderId);
						return orderUtil.cancel(orderx);
					});
					break;
				} catch (CannotAcquireLockException e) {
					// Handle concurrency issues by retrying the transaction
					if (logger.isDebugEnabled())
						logger.debug("... database concurrency issue detected: ", e);

					if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
						ProseoUtil.dbWait();
					} else {
						if (logger.isDebugEnabled())
							logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
						throw e;
					}
				}
			}

		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (msg.getSuccess()) {
			// If cancellation is successful, retrieve and return the canceled order
			RestOrder restOrder = getRestOrder(order.getId());

			return new ResponseEntity<>(restOrder, HttpStatus.OK);
		} else {
			// Cancellation failed due to an illegal state, return a BAD_REQUEST response
			return new ResponseEntity<>(http.errorHeaders(msg.getText()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Close processing order by ID.
	 * 
	 * @param orderId     the ID of the processing order
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return ResponseEntity containing a RestOrder and HTTP status
	 */
	@Override
	public ResponseEntity<RestOrder> closeOrder(String orderId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> closeOrder({})", orderId);

		// Find the processing order by its orderId
		ProcessingOrder order = this.findOrder(orderId);

		// Ensure the order exists
		if (null == order) {
			String message = logger.log(PlannerMessage.ORDER_NOT_EXIST, orderId);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
		}

		PlannerResultMessage msg = new PlannerResultMessage(null);
		try {
			// Attempt to close the order
			msg = orderUtil.close(order.getId());
			logger.log(msg.getMessage(), order.getIdentifier());
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (msg.getSuccess()) {
			// If closing is successful, retrieve and return the closed order
			RestOrder restOrder = getRestOrder(order.getId());

			return new ResponseEntity<>(restOrder, HttpStatus.OK);
		} else {
			// Closing failed due to an illegal state, return a BAD_REQUEST response
			return new ResponseEntity<>(http.errorHeaders(msg.getText()), HttpStatus.BAD_REQUEST);
		}
	}

	/**
	 * Suspend processing order by ID.
	 * 
	 * @param orderId     the ID of the processing order
	 * @param force       indicates whether to force suspend the order
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return ResponseEntity containing a RestOrder and HTTP status
	 */
	@Override
	public ResponseEntity<RestOrder> suspendOrder(String orderId, Boolean force, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> suspendOrder({}, force: {})", orderId, force);

		try {
			// Find the processing order by its orderId
			ProcessingOrder order = this.findOrder(orderId);

			// Ensure order exists
			if (null == order) {
				String message = logger.log(PlannerMessage.ORDER_NOT_EXIST, orderId);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			}

			if (null == force) {
				force = false;
			}

			// Check the status of the requested processing facility
			for (ProcessingFacility processingFacilityX : orderUtil.getProcessingFacilities(order.getId())) {
				TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
				final ResponseEntity<RestOrder> response = transactionTemplate.execute((status) -> {
					Optional<ProcessingFacility> processingFacilityOpt = RepositoryService.getFacilityRepository()
						.findById(processingFacilityX.getId());
					if (processingFacilityOpt.isPresent()) {
						ProcessingFacility processingFacility = processingFacilityOpt.get();
						KubeConfig kubeConfig = productionPlanner.updateKubeConfig(processingFacility.getName());
						if (null == kubeConfig) {
							String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, processingFacility.getName());

							return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
						}
						if (processingFacility.getFacilityState() != FacilityState.RUNNING
								&& processingFacility.getFacilityState() != FacilityState.STARTING) {
							String message = logger.log(GeneralMessage.FACILITY_NOT_AVAILABLE, processingFacility.getName(),
									processingFacility.getFacilityState().toString());
							if (processingFacility.getFacilityState() == FacilityState.DISABLED) {
								return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.BAD_REQUEST);
							} else {
								return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.SERVICE_UNAVAILABLE);
							}
						}
					}
					return null;
				});
				if (response != null) {
					return response;
				}
			}

			PlannerResultMessage msg = orderUtil.prepareSuspend(order.getId(), force);
			if (msg.getSuccess()) {
				msg = orderUtil.suspend(order.getId(), force);
			}
			// Any exceptions in the suspension process are already logged inside orderUtil.reset()

			if (msg.getSuccess()) {
				// If suspension is successful, retrieve and return the suspended order
				RestOrder restOrder = getRestOrder(order.getId());

				return new ResponseEntity<>(restOrder, HttpStatus.OK);
			} else {
				// Suspension failed due to an illegal state, return a BAD_REQUEST response
				return new ResponseEntity<>(http.errorHeaders(msg.getText()), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Retry processing order by ID.
	 * 
	 * @param orderId     the ID of the processing order
	 * @param httpHeaders the HTTP request headers (injected)
	 * @return ResponseEntity containing a RestOrder and HTTP status
	 */
	@Override
	public ResponseEntity<RestOrder> retryOrder(String orderId, HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> retryOrder({})", orderId);

		try {
			// Find the processing order by its orderId
			ProcessingOrder order = this.findOrder(orderId);

			// Ensure order exists
			if (null == order) {
				String message = logger.log(PlannerMessage.ORDER_NOT_EXIST, orderId);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
			}

			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
			transactionTemplate.setReadOnly(true);
//			ProcessingFacility pf = transactionTemplate.execute((status) -> {
//				for (Job j : this.findOrder(orderId).getJobs()) {
//					return j.getProcessingFacility();
//				}
//				return null;
//			});
			// Check the status of the requested processing facility
//			KubeConfig kc = productionPlanner.updateKubeConfig(pf.getName());
//			if (null == kc) {
//				String message = logger.log(PlannerMessage.FACILITY_NOT_EXIST, pf.getName());
//
//		    	return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.NOT_FOUND);
//			}

			PlannerResultMessage msg = new PlannerResultMessage(null);
			try {
				transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
				transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
				for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
					try {
						msg = transactionTemplate.execute((status) -> {
							ProcessingOrder orderx = this.findOrderPrim(orderId);
							// Attempt order retry
							return orderUtil.retry(orderx);
						});
						break;
					} catch (CannotAcquireLockException e) {
						// Handle database concurrency issues by retrying
						if (logger.isDebugEnabled())
							logger.debug("... database concurrency issue detected: ", e);

						if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
							ProseoUtil.dbWait();
						} else {
							if (logger.isDebugEnabled())
								logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
							throw e;
						}
					}
				}
			} catch (Exception e) {
				String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

				if (logger.isDebugEnabled())
					logger.debug("... exception stack trace: ", e);

				return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			if (msg.getSuccess()) {
				// If reset is successful, retrieve and return the order (now in approved status)
				RestOrder restOrder = getRestOrder(order.getId());

				return new ResponseEntity<>(restOrder, HttpStatus.OK);
			} else {
				// Retry failed due to an illegal state, return a BAD_REQUEST response
				return new ResponseEntity<>(http.errorHeaders(msg.getText()), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			String message = logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>(http.errorHeaders(message), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Find a processing order by DB id or identifier.
	 * 
	 * @param orderId DB id or identifier
	 * @return Order found
	 */
	private ProcessingOrder findOrderPrim(String orderId) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findOrderPrim({})", orderId);

		String missionCode = securityService.getMission();

		ProcessingOrder order = null;
		TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		transactionTemplate.setReadOnly(true);
		order = transactionTemplate.execute((status) -> {
			ProcessingOrder orderx = null;
			try {
				// Attempt to parse orderId as a Long to retrieve the order by DB id
				Long id = Long.valueOf(orderId);
				Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
				if (orderOpt.isPresent()) {
					orderx = orderOpt.get();
				}
			} catch (NumberFormatException nfe) {
				// If parsing fails, use orderId as an identifier to retrieve the order
			}
			if (orderx == null) {
				// If order not found by DB id, try to find it by identifier and mission code
				orderx = RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(missionCode, orderId);
			}

			if (null == orderx) {
				// If order still not found, return null
				return null;
			}

			// Ensure user is authorized for the mission of the order
			if (!missionCode.equals(orderx.getMission().getCode())) {
				logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, orderx.getMission().getCode(), missionCode);
				return null;
			}
			return orderx;
		});
		return order;
	}

	/**
	 * Find a processing order by ID.
	 * 
	 * @param orderId the ID of the processing order
	 * @return ProcessingOrder found
	 */
	private ProcessingOrder findOrder(String orderId) {
		if (logger.isTraceEnabled())
			logger.trace(">>> findOrder({})", orderId);
		ProcessingOrder order = null;
		try {
			order = this.findOrderPrim(orderId);
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);
		}
		return order;
	}

	/**
	 * Get a REST order by ID.
	 * 
	 * @param id the ID of the processing order
	 * @return RestOrder found
	 */
	private RestOrder getRestOrder(long id) {
		RestOrder answer = null;
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(productionPlanner.getTxManager());
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
			transactionTemplate.setReadOnly(true);
			answer = transactionTemplate.execute((status) -> {
				RestOrder restOrder = null;
				ProcessingOrder order = null;
				Optional<ProcessingOrder> orderOpt = RepositoryService.getOrderRepository().findById(id);
				if (orderOpt.isPresent()) {
					order = orderOpt.get();
				}
				restOrder = RestUtil.createRestOrder(order);
				return restOrder;
			});
		} catch (Exception e) {
			logger.log(GeneralMessage.RUNTIME_EXCEPTION_ENCOUNTERED, e.getMessage());

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);
		}
		return answer;
	}

}