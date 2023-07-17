/**
 * OrderManager.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.cleanup;

import java.time.Instant;
import java.util.List;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OrderMgrMessage;
import de.dlr.proseo.ordermgr.OrderManager;

/**
 * Thread to look for deletable processing orders
 *
 * @author Ernst Melchinger
 */
public class CleanupOrdersThread extends Thread {

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(CleanupOrdersThread.class);

	/** The order manager instance */
	private OrderManager orderMgr;

	/**
	 * Create new CleanupOrdersThread for the order manager
	 *
	 * @param orderMgr the order manager application
	 */
	public CleanupOrdersThread(OrderManager orderMgr) {
		super("CleanupOrders");
		this.setDaemon(true);
		this.orderMgr = orderMgr;
	}

	/**
	 * Start the cleanup cycle thread to look for deletable orders every cleanupCycleTime hours
	 *
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		if (logger.isTraceEnabled())
			logger.trace(">>> run()");

		// default wait is one day 24h * 60' * 60'' * 1000 millis
		long wait = 24;
		if (this.orderMgr.getOrderManagerConfig().getCleanupCycleTime() != null) {
			wait = this.orderMgr.getOrderManagerConfig().getCleanupCycleTime();
		}
		wait = wait * 60 * 60 * 1000;

		while (!this.isInterrupted()) {
			try {
				logger.log(OrderMgrMessage.ORDER_CLEANUP_CYCLE);

				Instant evictionTime = Instant.now();
				List<Long> orderIdsToDelete = orderMgr.getProcOrderManager().findOrdersWithEvictionTimeLessThan(evictionTime);

				for (Long orderId : orderIdsToDelete) {
					// One transaction per delete operation
					orderMgr.getProcOrderManager().deleteExpiredOrderById(orderId, evictionTime);
				}

				logger.log(OrderMgrMessage.ORDER_CLEANUP_SLEEP, wait);
				sleep(wait);
			} catch (InterruptedException e) {
				logger.log(OrderMgrMessage.ORDER_CLEANUP_TERMINATE);
				break;
			} catch (Exception e) {
				logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass() + " / " + e.getMessage());
				// continue loop anyway
			}
		}
	}

}