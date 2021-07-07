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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private static Logger logger = LoggerFactory.getLogger(StorageManager.class);

	public static void main(String[] args) throws Exception {
		
		if (logger.isTraceEnabled()) logger.trace(">>> main({})", (Object[]) args);
		
		SpringApplication.run(StorageManager.class, args);
	}
	
}
