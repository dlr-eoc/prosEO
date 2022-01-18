/**
 * Ingestor.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ingestor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.dlr.proseo.ingestor.cleanup.CleanupProductThread;
import de.dlr.proseo.ingestor.rest.ProductIngestor;

/**
 * prosEO Ingestor application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
@EnableJpaRepositories(basePackages = { "de.dlr.proseo.model.dao" })
public class IngestorApplication  implements CommandLineRunner {
	private static Logger logger = LoggerFactory.getLogger(IngestorApplication.class);

	/** Ingestor configuration */
	@Autowired
	IngestorConfiguration ingestorConfig;
	
	/** The product ingestor */
	@Autowired
	ProductIngestor productIngestor;
	
	/**
	 * @return the productIngestor
	 */
	public ProductIngestor getProductIngestor() {
		return productIngestor;
	}

	/**
	 * @param productIngestor the productIngestor to set
	 */
	public void setProductIngestor(ProductIngestor productIngestor) {
		this.productIngestor = productIngestor;
	}

	/**
	 * @return the ingestorConfig
	 */
	public IngestorConfiguration getIngestorConfig() {
		return ingestorConfig;
	}

	/**
	 * @param ingestorConfig the ingestorConfig to set
	 */
	public void setIngestorConfig(IngestorConfiguration ingestorConfig) {
		this.ingestorConfig = ingestorConfig;
	}

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
	 * Start the retention thread
	 */
	public void startDispatcher() {
		if (logger.isTraceEnabled()) logger.trace(">>> start cleanup cycle");
		
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
