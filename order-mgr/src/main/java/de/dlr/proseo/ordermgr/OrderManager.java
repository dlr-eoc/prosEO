/**
 * OrderManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ordermgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.dlr.proseo.ordermgr.cleanup.CleanupOrdersThread;
import de.dlr.proseo.ordermgr.rest.ProcessingOrderMgr;

/**
 * prosEO Order Manager application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
@EnableJpaRepositories(basePackages = { "de.dlr.proseo.model.dao" })
public class OrderManager implements CommandLineRunner{
	
	private static Logger logger = LoggerFactory.getLogger(OrderManager.class);
	
	/** The Order Manager configuration */
	@Autowired
	OrdermgrConfiguration orderManagerConfig;

	/** The processing order manager */
	@Autowired
	private ProcessingOrderMgr procOrderManager;
	
	private CleanupOrdersThread cleanupThread = null;

	/**
	 * @return the procOrderManager
	 */
	public ProcessingOrderMgr getProcOrderManager() {
		return procOrderManager;
	}

	/**
	 * @param procOrderManager the procOrderManager to set
	 */
	public void setProcOrderManager(ProcessingOrderMgr procOrderManager) {
		this.procOrderManager = procOrderManager;
	}

	/**
	 * @return the orderManagerConfig
	 */
	public OrdermgrConfiguration getOrderManagerConfig() {
		return orderManagerConfig;
	}

	public static void main(String[] args) throws Exception {
		SpringApplication spa = new SpringApplication(OrderManager.class);
		spa.run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		this.startDispatcher();		
	}


	/**
	 * Start the cleanup thread
	 */
	public void startDispatcher() {
		if (logger.isTraceEnabled()) logger.trace(">>> start cleanup cycle");
		
		if (cleanupThread == null || !cleanupThread.isAlive()) {
			cleanupThread = new CleanupOrdersThread(this);
			cleanupThread.start();
		} else {
			if (cleanupThread.isInterrupted()) {
				// r
			}
		}
	}
}
