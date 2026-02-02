/**
 * OrderCreator.java
 * 
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordergen.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.OrderGenMessage;
import de.dlr.proseo.model.CalendarOrderTrigger;
import de.dlr.proseo.model.DataDrivenOrderTrigger;
import de.dlr.proseo.model.DatatakeOrderTrigger;
import de.dlr.proseo.model.OrbitOrderTrigger;
import de.dlr.proseo.model.OrderTrigger;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.TimeIntervalOrderTrigger;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.WorkflowOption;
import de.dlr.proseo.model.WorkflowOption.WorkflowOptionType;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.enums.TriggerType;
import de.dlr.proseo.model.rest.model.RestClassOutputParameter;
import de.dlr.proseo.model.rest.model.RestOrbitQuery;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.model.util.StringUtils;
import de.dlr.proseo.ordergen.OrderGenConfiguration;
import de.dlr.proseo.ordergen.service.ServiceConnection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Create and start a processing order.
 *
 * @author Ernst Melchinger
 *
 */
public class OrderCreator {


	/** A logger for this class */
	private ProseoLogger logger = new ProseoLogger(OrderCreator.class);
	
	private final String URI_PATH_ORDERS = "/orders";
	private final String ORDERS = "orders";

	private final String URI_PATH_ORDERS_APPROVE = "/orders/approve";
	private final String URI_PATH_ORDERS_PLAN = "/orders/plan";
	private final String URI_PATH_ORDERS_RESUME = "/orders/resume";
	

	private final String ORDER_SLICING_TYPE = "NONE";
	private final String ORDER_PRODUCTION_TYOE = "SYSTEMATIC";

	/** Transaction manager for transaction control */
	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	private OrderGenConfiguration config;

	private TriggerUtil triggerUtil;
	
	/** The connector service to the prosEO backend services */
	protected ServiceConnection serviceConnection;

	/**
	 * @param txManager the txManager to set
	 */
	public void setTxManager(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}

	/**
	 * @param config the config to set
	 */
	public void setConfig(OrderGenConfiguration config) {
		this.config = config;
	}

	/**
	 * @param triggerUtil the triggerUtil to set
	 */
	public void setTriggerUtil(TriggerUtil triggerUtil) {
		this.triggerUtil = triggerUtil;
	}

	/**
	 * @param serviceConnection the serviceConnection to set
	 */
	public void setServiceConnection(ServiceConnection serviceConnection) {
		this.serviceConnection = serviceConnection;
	}

	protected final DateTimeFormatter instantFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSSSSS")
		.withZone(ZoneId.of("UTC"));

	/**
	 * Sends an order to the production planner and releases it.
	 *
	 * @param order The order to be sent and released.
	 * @return The created order after sending and releasing.
	 * @throws Exception If an error occurs during the process of sending and releasing the order.
	 */
	public RestOrder createOrder(RestOrder order) throws Exception {
		RestOrder createdOrder = null;
		String message = null;
		if (order != null) {
			if (logger.isTraceEnabled())
				logger.trace(">>> createOrder({})", order.getIdentifier());
			// send order to order manager
			int i = 0;
			while (3 > i++) {
				try {
					createdOrder = serviceConnection.postToService(config.getOrderManagerUrl(), URI_PATH_ORDERS, order,
							RestOrder.class, order.getMissionCode() + "-" + config.getUser(),
							config.getPassword());
					break;
				} catch (RestClientResponseException e) {
					message = null;
					switch (e.getStatusCode().value()) {
					case org.apache.http.HttpStatus.SC_BAD_REQUEST:
						message = logger.log(OrderGenMessage.ORDER_DATA_INVALID, e.getStatusText());
					case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
					case org.apache.http.HttpStatus.SC_FORBIDDEN:
						message = (null == e.getStatusText()
								? ProseoLogger.format(OrderGenMessage.NOT_AUTHORIZED, config.getUser(), ORDERS,
										order.getMissionCode())
								: e.getStatusText());
					default:
						message = logger.log(OrderGenMessage.EXCEPTION, e.getMessage());
					}
					throw new Exception(message);
				} catch (RuntimeException e) {
					message = ProseoLogger.format(OrderGenMessage.EXCEPTION, e.getMessage());
					throw new Exception(message);
				}
			}
		}
		return createdOrder;
	}

	/**
	 * Sends an order to the production planner and releases it.
	 *
	 * @param order The order to be sent and released.
	 * @return The created order after sending and releasing.
	 * @throws Exception If an error occurs during the process of sending and releasing the order.
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public RestOrder planAndReleaseOrder(RestOrder order) throws Exception {
		RestOrder createdOrder = null;
		if (order != null) {
			if (logger.isTraceEnabled())
				logger.trace(">>> planAndReleaseOrder({})", order.getIdentifier());
			// send order to order manager
			int i = 0;
			// approve, plan, release the order
			while (3 > i++) {
				try {
					createdOrder = serviceConnection.patchToService(config.getProductionPlannerUrl(),
							URI_PATH_ORDERS_APPROVE + "/" + order.getId(), order, RestOrder.class,
							order.getMissionCode() + "-" + config.getUser(),
							config.getPassword());
					break;
				} catch (RestClientResponseException e) {
					String message = null;
					switch (e.getStatusCode().value()) {
					case org.apache.http.HttpStatus.SC_BAD_REQUEST:
						message = logger.log(OrderGenMessage.ORDER_DATA_INVALID, e.getStatusText());
						break;
					case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
					case org.apache.http.HttpStatus.SC_FORBIDDEN:
						message = (null == e.getStatusText()
								? ProseoLogger.format(OrderGenMessage.NOT_AUTHORIZED, config.getUser(), ORDERS,
										order.getMissionCode())
								: e.getStatusText());
						break;
					default:
						message = logger.log(OrderGenMessage.EXCEPTION, e.getMessage());
					}
					throw new Exception(message);
				} catch (RuntimeException e) {
					String message = ProseoLogger.format(OrderGenMessage.EXCEPTION, e.getMessage());
					throw new Exception(message);
				}
			}
			if (i > 2) {
				return createdOrder;
			}

			i = 0;
			while (3 > i++) {
				try {
					createdOrder = serviceConnection.putToService(config.getProductionPlannerUrl(),
							URI_PATH_ORDERS_PLAN + "/" + createdOrder.getId() + "?facility=" + config.getFacility() + "&wait=true",
							RestOrder.class, order.getMissionCode() + "-" + config.getUser(),
							config.getPassword());
					break;
				} catch (RestClientResponseException e) {
					String message = null;
					switch (e.getStatusCode().value()) {
					case org.apache.http.HttpStatus.SC_BAD_REQUEST:
						message = logger.log(OrderGenMessage.ORDER_DATA_INVALID, e.getStatusText());
						break;
					case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
					case org.apache.http.HttpStatus.SC_FORBIDDEN:
						message = (null == e.getStatusText()
								? ProseoLogger.format(OrderGenMessage.NOT_AUTHORIZED, config.getUser(), ORDERS,
										order.getMissionCode())
								: e.getStatusText());
						break;
					default:
						message = logger.log(OrderGenMessage.EXCEPTION, e.getMessage());
					}
					throw new Exception(message);
				} catch (RuntimeException e) {
					String message = ProseoLogger.format(OrderGenMessage.EXCEPTION, e.getMessage());
					throw new Exception(message);
				}
			}
			if (i > 2) {
				return createdOrder;
			}

			i = 0;
			while (3 > i++) {
				try {
					createdOrder = serviceConnection.patchToService(config.getProductionPlannerUrl(),
							URI_PATH_ORDERS_RESUME + "/" + createdOrder.getId() + "?wait=false", createdOrder, RestOrder.class,
							order.getMissionCode() + "-" + config.getUser(),
							config.getPassword());
					break;
				} catch (RestClientResponseException e) {
					String message = null;
					switch (e.getStatusCode().value()) {
					case org.apache.http.HttpStatus.SC_BAD_REQUEST:
						message = logger.log(OrderGenMessage.ORDER_DATA_INVALID, e.getStatusText());
						break;
					case org.apache.http.HttpStatus.SC_UNAUTHORIZED:
					case org.apache.http.HttpStatus.SC_FORBIDDEN:
						message = (null == e.getStatusText()
								? ProseoLogger.format(OrderGenMessage.NOT_AUTHORIZED, config.getUser(), ORDERS,
										order.getMissionCode())
								: e.getStatusText());
						break;
					default:
						message = logger.log(OrderGenMessage.EXCEPTION, e.getMessage());
					}
					System.err.println(message);
					throw new Exception(message);
				} catch (RuntimeException e) {
					String message = ProseoLogger.format(OrderGenMessage.EXCEPTION, e.getMessage());
					throw new Exception(message);
				}
			}
		}

		return createdOrder;
	}

	/**
	 * Build a RestOrder out of trigger information. Use time parameters or porductId to calculate the sensing start and stop times.
	 * 
	 * @param orderTrigger The trigger
	 * @param previousFireTime The previous fire time of a quartz job (Calendar or TimeInterval trigger)
	 * @param fireTime The current fire time of a quartz job (Calendar or TimeInterval trigger)
	 * @param nextFireTime The next fire time of a quartz job (Calendar or TimeInterval trigger)
	 * @param productId The product id of a DataDriven trigger
	 * @return
	 */
	public RestOrder createAndStartFromTrigger(OrderTrigger orderTrigger, Date previousFireTime, Date fireTime, Date nextFireTime, Long productId) {
		if (orderTrigger != null && orderTrigger instanceof OrderTrigger) {
			if (logger.isTraceEnabled())
				logger.trace(">>> createAndStartFromTrigger({})", orderTrigger.getName());
		} else {
			logger.log(OrderGenMessage.ORDER_DATA_INVALID, ">>> createAndStartFromTrigger(NULL)");
			return null;
		}
		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
		transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

		TriggerType typeTmp = null;
		if (orderTrigger instanceof CalendarOrderTrigger) {
			typeTmp = TriggerType.Calendar;
		} else if (orderTrigger instanceof TimeIntervalOrderTrigger) {
			typeTmp = TriggerType.TimeInterval;
		} else if (orderTrigger instanceof OrbitOrderTrigger) {
			typeTmp = TriggerType.Orbit;
		} else if (orderTrigger instanceof DatatakeOrderTrigger) {
			typeTmp = TriggerType.Datatake;
		} else if (orderTrigger instanceof DataDrivenOrderTrigger) {
			typeTmp = TriggerType.DataDriven;
		}
		final TriggerType type = typeTmp;
		Instant startTime = null;
		Instant stopTime = null;
		List<RestOrbitQuery> orbits = new ArrayList<>();
		String orderIdentifierSuffix = "";
		// calculate start and stop time
		switch (type) {
		case Calendar:
			startTime = fireTime.toInstant().truncatedTo(ChronoUnit.SECONDS);
			if (nextFireTime != null) {
				stopTime = nextFireTime.toInstant().truncatedTo(ChronoUnit.SECONDS);
			} else if (previousFireTime != null) {
				stopTime = startTime.plus(Duration.between(startTime, previousFireTime.toInstant().truncatedTo(ChronoUnit.SECONDS)));
			} else {
				// error
			}
			orderIdentifierSuffix = instantFormatter.format(startTime);
			break;
		case TimeInterval:
			TimeIntervalOrderTrigger tiTrigger = (TimeIntervalOrderTrigger)orderTrigger;
			startTime = fireTime.toInstant();
			stopTime = startTime.plus(tiTrigger.getTriggerInterval());
			tiTrigger.setNextTriggerTime(stopTime);
			transactionTemplate.setReadOnly(false);
			for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
				try {
					transactionTemplate.setReadOnly(false);
					transactionTemplate.execute((status) -> {
						triggerUtil.save(tiTrigger);
						return null;
					});

					break;
				} catch (CannotAcquireLockException e) {
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
			orderIdentifierSuffix = instantFormatter.format(startTime);
			break;
		case DataDriven:
			if (productId == null) {
				logger.log(OrderGenMessage.PRODUCT_NOT_FOUND, productId);
				return null;
			}
			transactionTemplate.setReadOnly(true);
			
			final Product inputProduct = transactionTemplate.execute((status) -> {
				Optional<Product> inputProductOpt = RepositoryService.getProductRepository().findById(productId);
				if (inputProductOpt.isEmpty()) {
					logger.log(OrderGenMessage.PRODUCT_NOT_FOUND, productId);
					return null;
				}
				return inputProductOpt.get();
			});
			if (inputProduct == null) {
				return null;
			}
			startTime = inputProduct.getSensingStartTime();
			stopTime = inputProduct.getSensingStopTime();
			orderIdentifierSuffix = instantFormatter.format(startTime);
			break;
		case Orbit:
			OrbitOrderTrigger orbitTrigger = (OrbitOrderTrigger)orderTrigger;
			RestOrbitQuery orbitQ = new RestOrbitQuery();
			orbitQ.setOrbitNumberFrom(Long.valueOf(orbitTrigger.getLastOrbit().getOrbitNumber()));
			orbitQ.setOrbitNumberTo(Long.valueOf(orbitTrigger.getLastOrbit().getOrbitNumber()));
			orbitQ.setSpacecraftCode(orbitTrigger.getSpacecraft().getCode());
			orbits.add(orbitQ);
			orderIdentifierSuffix = orbitTrigger.getLastOrbit().getOrbitNumber().toString();
			break;
		default:
			break;
		}

		final Instant finalStartTime = startTime;
		final Instant finalStopTime = stopTime;
		final List<RestOrbitQuery> finalOrbits = orbits; 
		final String finalOrderIdentifierSuffix = orderIdentifierSuffix;
		transactionTemplate.setReadOnly(true);
		
		final RestOrder order = transactionTemplate.execute((status) -> {
			Optional<Workflow> optWorkflow = RepositoryService.getWorkflowRepository().findById(orderTrigger.getWorkflow().getId());
			if (!optWorkflow.isPresent()) {
				logger.log(OrderGenMessage.WORKFLOW_NOT_FOUND, orderTrigger.getWorkflow().getId());
				return new RestOrder();
			}
			Workflow workflow  = optWorkflow.get(); 
			if (!workflow.getEnabled()) {
				// no workflow reference, return error
				logger.log(OrderGenMessage.WORKFLOW_NOT_ENABLED, workflow.getName());
				return new RestOrder();
			}

			Instant now = Instant.now();
			String orderIdentifier = workflow.getName() + "_" + orderTrigger.getName() + "_" + finalOrderIdentifierSuffix;
			if (RepositoryService.getOrderRepository().findByMissionCodeAndIdentifier(orderTrigger.getMission().getCode(), orderIdentifier) != null) {
				if (logger.isTraceEnabled())
					logger.trace("    order already exists ({})", orderIdentifier);
				return new RestOrder();
			}
			RestOrder restOrder = new RestOrder();
			restOrder.setMissionCode(workflow.getMission().getCode());
			restOrder.setIdentifier(orderIdentifier);
			restOrder.setWorkflowName(workflow.getName());
			restOrder.setWorkflowUuid(workflow.getUuid().toString());
			restOrder.setPriority(orderTrigger.getPriority());
			restOrder.setExecutionTime(Date.from(now.plus(orderTrigger.getExecutionDelay().toMillis(), ChronoUnit.MILLIS)));
			restOrder.setSubmissionTime(Date.from(now));
			
			
			if (workflow.getProcessingMode() != null && !workflow.getProcessingMode().isEmpty()) {
				restOrder.setProcessingMode(workflow.getProcessingMode());
			} else {
				restOrder.setProcessingMode(orderTrigger.getMission().getProcessingModes().toArray()[0].toString());
			}
			if (workflow.getOutputFileClass() != null && !workflow.getOutputFileClass().isEmpty()) {
				restOrder.setOutputFileClass(workflow.getOutputFileClass());
			} else {
				restOrder.setProcessingMode(orderTrigger.getMission().getFileClasses().toArray()[0].toString());
			}

			restOrder.setProductionType(ORDER_PRODUCTION_TYOE);
			if (workflow.getSlicingType() != null) {
				restOrder.setSlicingType(workflow.getSlicingType().toString());
			} else {
				restOrder.setSlicingType(ORDER_SLICING_TYPE);
			}
			restOrder.setOrbits(finalOrbits);
			// handle slicing type CALENDAR_DAY, CALENDAR_MONTH, CALENDAR_YEAR
			Instant startTimeLoc = finalStartTime;
			Instant stopTimeLoc = finalStopTime;
			if (startTimeLoc != null && stopTimeLoc != null) {
				if (restOrder.getSlicingType().equals(OrderSlicingType.CALENDAR_DAY.toString())) {
					startTimeLoc = finalStartTime.truncatedTo(ChronoUnit.DAYS);
					stopTimeLoc = finalStartTime.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS);
				} else if (restOrder.getSlicingType().equals(OrderSlicingType.CALENDAR_MONTH.toString())) {
					startTimeLoc = finalStartTime.truncatedTo(ChronoUnit.MONTHS);
					stopTimeLoc = finalStartTime.truncatedTo(ChronoUnit.MONTHS).plus(1, ChronoUnit.MONTHS);
				} else if (restOrder.getSlicingType().equals(OrderSlicingType.CALENDAR_YEAR.toString())) {
					startTimeLoc = finalStartTime.truncatedTo(ChronoUnit.YEARS);
					stopTimeLoc = finalStartTime.truncatedTo(ChronoUnit.YEARS).plus(1, ChronoUnit.YEARS);
				}
				restOrder.setStartTime(OrbitTimeFormatter.format(startTimeLoc));
				restOrder.setStopTime(OrbitTimeFormatter.format(stopTimeLoc));
			}
			if (workflow.getSliceDuration() != null) {
				restOrder.setSliceDuration(workflow.getSliceDuration().getSeconds());
			}
			if (workflow.getSliceOverlap() != null) {
				restOrder.setSliceOverlap(workflow.getSliceOverlap().getSeconds());
			}
			restOrder.getRequestedProductClasses().add(workflow.getOutputProductClass().getProductType());
			restOrder.getConfiguredProcessors().add(workflow.getConfiguredProcessor().getIdentifier());
			switch (type) {
			case DataDriven:
				DataDrivenOrderTrigger dtTrigger = (DataDrivenOrderTrigger)orderTrigger;
				if (!dtTrigger.getParametersToCopy().isEmpty()) {
					Optional<Product> inputProductOpt = RepositoryService.getProductRepository().findById(productId);
					Product product = inputProductOpt.get();
					Boolean copyAll = dtTrigger.getParametersToCopy().contains("*");
					for (String key : product.getParameters().keySet()) {
						if (copyAll || dtTrigger.getParametersToCopy().contains(key)) {
							restOrder.getOutputParameters()
							.add(new RestParameter(key, product.getParameters().get(key).getParameterType().toString(),
									product.getParameters().get(key).getParameterValue()));
						}
					}						
				}
				break;
			default:
				break;
			}
			if (workflow.getOutputParameters() != null) {
				for (String paramKey : workflow.getOutputParameters().keySet()) {
					restOrder.getOutputParameters()
					.add(new RestParameter(paramKey, workflow.getOutputParameters().get(paramKey).getParameterType().toString(),
							workflow.getOutputParameters().get(paramKey).getParameterValue()));
				}
			}
			if (workflow.getClassOutputParameters() != null) {
				for (ProductClass targetClass : workflow.getClassOutputParameters().keySet()) {
					RestClassOutputParameter restClassOutputParameter = new RestClassOutputParameter();
					restClassOutputParameter.setProductClass(targetClass.getProductType());
					Map<String, Parameter> outputParameters = workflow.getClassOutputParameters()
							.get(targetClass)
							.getOutputParameters();
					for (String paramKey : outputParameters.keySet()) {
						restClassOutputParameter.getOutputParameters()
						.add(new RestParameter(paramKey, outputParameters.get(paramKey).getParameterType().toString(),
								outputParameters.get(paramKey).getParameterValue()));
					}
					restOrder.getClassOutputParameters().add(restClassOutputParameter);
				}
			}

			for (WorkflowOption wo : workflow.getWorkflowOptions()) {

				RestParameter param = new RestParameter();
				param.setKey(wo.getName());
				if (wo.getType().equals(WorkflowOptionType.NUMBER)) {
					// check for number type
					try {
						Integer.parseInt(wo.getDefaultValue());
						param.setParameterType(ParameterType.INTEGER.toString());
					} catch (NumberFormatException e) {
						// try double
						try {
							Double.parseDouble(wo.getDefaultValue());
							param.setParameterType(ParameterType.DOUBLE.toString());
						} catch (NumberFormatException ex) {
							// error, value string is not a number
							logger.log(OrderGenMessage.MSG_WORKFLOW_OPTION_NO_TYPE_MATCH, wo.getName(),
									wo.getType(), wo.getDefaultValue());
							return new RestOrder();
						}
					}

				} else if (wo.getType().equals(WorkflowOptionType.DATENUMBER)) {
					/**
					 * Assumption is that this type means the day of year, i. e. it must be an integer number in the range
					 * 1..366
					 */
					try {
						Integer dn = Integer.parseInt(wo.getDefaultValue());
						if (dn < 1 || dn > 366) {
							logger.log(OrderGenMessage.MSG_WORKFLOW_OPTION_NO_TYPE_MATCH, wo.getName(),
									wo.getType(), wo.getDefaultValue());
							return new RestOrder();
						}
						param.setParameterType(ParameterType.INTEGER.toString());
					} catch (NumberFormatException e) {
						logger.log(OrderGenMessage.MSG_WORKFLOW_OPTION_NO_TYPE_MATCH, wo.getName(), wo.getType(),
								wo.getDefaultValue());
						return new RestOrder();
					}
				} else {
					// all others are strings
					param.setParameterType(ParameterType.STRING.toString());
				}
				param.setParameterValue(wo.getDefaultValue());
				restOrder.getDynamicProcessingParameters().add(param);
			}
			return restOrder;
		});

		if (StringUtils.isNullOrEmpty(order.getIdentifier())) {
			return null;
		}
		// REST order is built, go on
		RestOrder restOrder = (RestOrder) order;
		try {
			restOrder = createOrder(restOrder);
		} catch (Exception e) {
			logger.log(OrderGenMessage.CREATE_ORDER_FAILED, e.getMessage());
		}
		try {
			restOrder = planAndReleaseOrder(restOrder);
		} catch (Exception e) {
			logger.log(OrderGenMessage.PLAN_RELEASE_ORDER_FAILED, e.getMessage());
		}
		return restOrder;
	}
}
