/**
 * StorageManager.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.storagemgr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * prosEO Storage Manager application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan
public class StorageManager {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(StorageManager.class);

	public static void main(String[] args) throws Exception {

		if (logger.isTraceEnabled())
			logger.trace(">>> main({})", (Object[]) args);

		SpringApplication.run(StorageManager.class, args);
	}
}
