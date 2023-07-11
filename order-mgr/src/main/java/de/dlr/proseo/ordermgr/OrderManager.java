/**
 * OrderManager.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.ordermgr.cleanup.CleanupOrdersThread;
import de.dlr.proseo.ordermgr.rest.ProcessingOrderMgr;

/**
 * prosEO Order Manager application
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages = { "de.dlr.proseo" })
@EnableJpaRepositories(basePackages = { "de.dlr.proseo.model.dao" })
public class OrderManager implements CommandLineRunner {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderManager.class);

	/** The Order Manager configuration */
	@Autowired
	OrdermgrConfiguration orderManagerConfig;

	/** The processing order manager */
	@Autowired
	private ProcessingOrderMgr procOrderManager;

	/** A thread to clean up deletable orders */
	private CleanupOrdersThread cleanupThread = null;

	/** The job name prefix */
	public static final String jobNamePrefix = "proseojob";

	/**
	 * Returns the processing order manager
	 * 
	 * @return the processing order manager
	 */
	public ProcessingOrderMgr getProcOrderManager() {
		return procOrderManager;
	}

	/**
	 * Sets the processing order manager
	 * 
	 * @param procOrderManager the processing order manager
	 */
	public void setProcOrderManager(ProcessingOrderMgr procOrderManager) {
		this.procOrderManager = procOrderManager;
	}

	/**
	 * Returns the order manager configuration
	 * 
	 * @return the order manager configuration
	 */
	public OrdermgrConfiguration getOrderManagerConfig() {
		return orderManagerConfig;
	}

	/**
	 * The entry point of the application. Initializes and runs the Order Manager.
	 *
	 * @param args The command-line arguments passed to the application.
	 * @throws Exception if an error occurs during the execution.
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication spa = new SpringApplication(OrderManager.class);
		spa.run(args);
	}

	/**
	 * Run the dispatcher of the clean-up thread
	 * 
	 * @param args The command-line arguments passed to the application.
	 */
	@Override
	public void run(String... args) throws Exception {
		this.startDispatcher();
	}

	/**
	 * Start the cleanup thread
	 */
	public void startDispatcher() {
		if (logger.isTraceEnabled())
			logger.trace(">>> start cleanup cycle");

		if (cleanupThread == null || !cleanupThread.isAlive()) {
			cleanupThread = new CleanupOrdersThread(this);
			cleanupThread.start();
		} else {
			if (cleanupThread.isInterrupted()) {
				// TODO What happens?
			}
		}
	}

}