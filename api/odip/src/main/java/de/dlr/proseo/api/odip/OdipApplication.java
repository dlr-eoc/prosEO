/**
 * OdipApplication.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.api.odip;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.dlr.proseo.api.odip.odata.OdipUtil;
import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * prosEO Processor Manager application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
@EnableJpaRepositories(basePackages = { "de.dlr.proseo.model.dao" })
public class OdipApplication extends OdipApplicationBase implements CommandLineRunner {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OdipApplication.class);

	@Autowired
	private OdipUtil odipUtil;
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(OdipApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> run({})", Arrays.asList(args));
		application = this;
		util = odipUtil;
	}
}
