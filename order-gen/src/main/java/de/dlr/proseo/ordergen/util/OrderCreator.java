package de.dlr.proseo.ordergen.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.OrderGenMessage;
import de.dlr.proseo.model.rest.model.RestOrder;
import de.dlr.proseo.ordergen.OrderGenConfiguration;
import de.dlr.proseo.ordergen.service.ServiceConnection;

public class OrderCreator {


	/** A logger for this class */
	private ProseoLogger logger = new ProseoLogger(OrderCreator.class);
	
	private final String URI_PATH_ORDERS = "/orders";
	private final String ORDERS = "orders";

	private final String URI_PATH_ORDERS_APPROVE = "/orders/approve";
	private final String URI_PATH_ORDERS_PLAN = "/orders/plan";
	private final String URI_PATH_ORDERS_RESUME = "/orders/resume";

	/** MonitorServices configuration */
	@Autowired
	protected OrderGenConfiguration config;

	/** The connector service to the prosEO backend services */
	@Autowired
	protected ServiceConnection serviceConnection;

	/**
	 * Sends an order to the production planner and releases it.
	 *
	 * @param order The order to be sent and released.
	 * @return The created order after sending and releasing.
	 * @throws OdipException If an error occurs during the process of sending and releasing the order.
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
	 * @throws OdipException If an error occurs during the process of sending and releasing the order.
	 */
	@Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
	public RestOrder planAndReleaseOrder(RestOrder order, String mission, String user, String password) throws Exception {
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
							mission + "-" + user, password);
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
							RestOrder.class, mission + "-" + user, password);
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
								? ProseoLogger.format(OrderGenMessage.NOT_AUTHORIZED, user, ORDERS,
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
							URI_PATH_ORDERS_RESUME + "/" + createdOrder.getId() + "?wait=true", createdOrder, RestOrder.class,
							mission + "-" + user, password);
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
								? ProseoLogger.format(OrderGenMessage.NOT_AUTHORIZED, user, ORDERS,
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
	
}
