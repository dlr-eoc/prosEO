/**
 * EdipMonitorApplication.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.api.edipmon;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.task.TaskExecutor;

/**
 * prosEO EDIP Monitor application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
public class EdipMonitorApplication implements CommandLineRunner {
	
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TaskExecutor taskExecutor;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(EdipMonitorApplication.class);
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(EdipMonitorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		if (logger.isTraceEnabled()) logger.trace(">>> run({})", Arrays.asList(args));
		
		Thread edipMonitor = (EdipMonitor) applicationContext.getBean(EdipMonitor.class);
		
		taskExecutor.execute(edipMonitor);
	}

}
