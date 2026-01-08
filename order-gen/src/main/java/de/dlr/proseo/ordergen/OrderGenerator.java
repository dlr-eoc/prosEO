/**
 * OrderGenerator.java
 *
 * (C) 2025 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordergen;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.ordergen.quartz.OrderGenScheduler;
import de.dlr.proseo.ordergen.service.ServiceConnection;
import de.dlr.proseo.ordergen.util.TriggerUtil;
import de.dlr.proseo.ordergen.util.OrderCreator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * prosEO Order Generator application
 *
 * @author Ernst Melchinger
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages = { "de.dlr.proseo" })
@EnableJpaRepositories(basePackages = { "de.dlr.proseo.model.dao" })
public class OrderGenerator implements CommandLineRunner {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderGenerator.class);
	

	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;
	
	@Autowired
	private OrderGenConfiguration config;

	@Autowired
	private TriggerUtil triggerUtil;

	/** The connector service to the prosEO backend services */
	@Autowired
	protected ServiceConnection serviceConnection;
	
	public static OrderGenScheduler scheduler;

	public static OrderCreator orderCreator;


	/**
	 * Gets the transaction manger.
	 *
	 * @return The PlatformTransactionManager instance.
	 */
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}

	
	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub		
		
		scheduler = new OrderGenScheduler();
		scheduler.init(triggerUtil);
		orderCreator = new OrderCreator();
		orderCreator.setTxManager(txManager);
		orderCreator.setTriggerUtil(triggerUtil);
		orderCreator.setServiceConnection(serviceConnection);
		orderCreator.setConfig(config);
		scheduler.buildCalendarTriggers();
		scheduler.buildImeIntervalTriggers();
		scheduler.start();
		
	}

	public static void main(String[] args) throws Exception {
		SpringApplication spa = new SpringApplication(OrderGenerator.class);
		spa.run(args);
	}

}
