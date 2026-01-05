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
	
	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub		
		
	}

	public static void main(String[] args) throws Exception {
		SpringApplication spa = new SpringApplication(OrderGenerator.class);
		spa.run(args);
	}

}
