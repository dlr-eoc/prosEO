/**
 * GraphicalUserInterface.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ui.gui;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
/**
 * prosEO Graphical User Interface application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@SpringBootApplication
////@Configuration
////@EnableAutoConfiguration
////@EnableConfigurationProperties
//@ComponentScan(basePackages={"de.dlr.proseo"})
//@Configuration
//@EnableAutoConfiguration
@EnableConfigurationProperties
//@ComponentScan
public class GraphicalUserInterface  {

	public static void main(String[] args) throws Exception {
		
		SpringApplication.run(GraphicalUserInterface.class, args);
	}
	
}
