/**
 * XbipMonitorApplication.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.api.xbipmon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * prosEO XBIP Monitor application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
public class XbipMonitorApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(XbipMonitorApplication.class, args);
	}

}
