/**
 * CadipMonitorApplication.java
 * 
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.api.cadipmon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.task.TaskExecutor;

/**
 * prosEO CADIP Monitor application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
public class CadipMonitorApplication implements CommandLineRunner {
	
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TaskExecutor taskExecutor;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(CadipMonitorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
		Thread cadipMonitor = (CadipMonitor) applicationContext.getBean("cadipMonitor");
		
		taskExecutor.execute(cadipMonitor);
	}

}
