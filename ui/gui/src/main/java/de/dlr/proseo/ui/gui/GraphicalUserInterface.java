/**
 * GraphicalUserInterface.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * prosEO Graphical User Interface application
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootApplication
//@Configuration
//@EnableAutoConfiguration
//@EnableConfigurationProperties
@ComponentScan(basePackages = { "de.dlr.proseo.ui" })
public class GraphicalUserInterface {

	/**
	 * The entry point of the Graphical User Interface application.
	 * 
	 * @param args the command-line arguments
	 * @throws Exception if an error occurs during the application execution
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication.run(GraphicalUserInterface.class, args);
	}

}