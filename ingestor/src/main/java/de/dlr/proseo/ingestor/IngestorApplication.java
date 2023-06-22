/**
 * Ingestor.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ingestor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.dlr.proseo.ingestor.cleanup.CleanupProductThread;
import de.dlr.proseo.ingestor.rest.ProductIngestor;
import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * prosEO Ingestor application
 *
 * @author Dr. Thomas Bassler
 *
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages = { "de.dlr.proseo" })
@EnableJpaRepositories(basePackages = { "de.dlr.proseo.model.dao" })
public class IngestorApplication implements CommandLineRunner {
	private static ProseoLogger logger = new ProseoLogger(IngestorApplication.class);

	/** Ingestor configuration */
	@Autowired
	IngestorConfiguration ingestorConfig;

	/** The product ingestor */
	@Autowired
	ProductIngestor productIngestor;

	/**
	 * Returns the product ingestor for the application
	 * 
	 * @return the product ingestor
	 */
	public ProductIngestor getProductIngestor() {
		return productIngestor;
	}

	/**
	 * Sets the product ingestor for the application
	 * 
	 * @param productIngestor the product ingestor to set
	 */
	public void setProductIngestor(ProductIngestor productIngestor) {
		this.productIngestor = productIngestor;
	}

	/**
	 * Returns the product ingestor configuration for the application
	 * 
	 * @return the ingestor configuration
	 */
	public IngestorConfiguration getIngestorConfig() {
		return ingestorConfig;
	}

	/**
	 * Sets the product ingestor configuration for the application
	 * 
	 * @param ingestorConfig the ingestor configuration to set
	 */
	public void setIngestorConfig(IngestorConfiguration ingestorConfig) {
		this.ingestorConfig = ingestorConfig;
	}

	/** A thread to delete old products and product files */
	private CleanupProductThread cleanupThread = null;

	public static void main(String[] args) throws Exception {
		SpringApplication spa = new SpringApplication(IngestorApplication.class);
		spa.run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		this.startDispatcher();
	}

	/**
	 * Starts the retention thread
	 */
	public void startDispatcher() {
		if (logger.isTraceEnabled())
			logger.trace(">>> start cleanup cycle");

		if (cleanupThread == null || !cleanupThread.isAlive()) {
			cleanupThread = new CleanupProductThread(this);
			cleanupThread.start();
		} else {
			if (cleanupThread.isInterrupted()) {
				//
			}
		}
	}
}
