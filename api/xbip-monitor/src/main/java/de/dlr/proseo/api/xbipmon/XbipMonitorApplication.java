/**
 * XbipMonitorApplication.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.api.xbipmon;

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
 * prosEO XBIP Monitor application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
public class XbipMonitorApplication implements CommandLineRunner {
	
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TaskExecutor taskExecutor;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(XbipMonitorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
		Thread xbipMonitor = (XbipMonitor) applicationContext.getBean(XbipMonitor.class);
		
		taskExecutor.execute(xbipMonitor);
	}

}
