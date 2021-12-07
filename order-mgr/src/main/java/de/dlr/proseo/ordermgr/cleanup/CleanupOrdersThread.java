package de.dlr.proseo.ordermgr.cleanup;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import de.dlr.proseo.ordermgr.OrderManager;

/**
 * Thread to look for deletable processing orders
 * 
 * @author Ernst Melchinger
 *
 */
public class CleanupOrdersThread  extends Thread {

	/**
	 * Logger for this class
	 */
	private static Logger logger = LoggerFactory.getLogger(CleanupOrdersThread.class);
	
	/**
	 * The order manager instance
	 */
	private OrderManager orderMgr;
	
	/**
	 * Create new CleanupOrdersThread for for order manager
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
	@Transactional
    public void run() {
		if (logger.isTraceEnabled()) logger.trace(">>> run()");
		// default wait is one day 24h * 60' * 60'' * 1000 millis
		long wait = 24;
		if (this.orderMgr.getOrderManagerConfig().getCleanupCycleTime() != null) {
			wait = this.orderMgr.getOrderManagerConfig().getCleanupCycleTime();
		};
		wait = wait * 60 * 60 * 1000;
		
		while (!this.isInterrupted()) {
			if (logger.isTraceEnabled()) logger.trace(">>> cleanup cycle()");
			Instant t = Instant.now();
			this.orderMgr.getProcOrderManager().deleteOrdersWithEvictionTimeLessThan(t);
			try {
				sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
