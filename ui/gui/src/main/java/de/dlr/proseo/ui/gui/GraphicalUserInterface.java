/**
 * GraphicalUserInterface.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.ui.gui;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
/**
 * prosEO Graphical User Interface application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
//@SpringBootApplication
////@Configuration
////@EnableAutoConfiguration
////@EnableConfigurationProperties
//@ComponentScan(basePackages={"de.dlr.proseo"})
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan
public class GraphicalUserInterface  {

	public static void main(String[] args) throws Exception {
		
		SpringApplication.run(GraphicalUserInterface.class, args);
	}
	/**
	 * Für customlogin beide params error und logout erkennen lassen und durch das model 
	 * an das template weitergeben ?
	 * Dynamische Errorbildung, Mapping = /error done
	 * Beide Fälle wahrscheinlich über httpRequest (URI-Parameter, http-Status Code) 
	 * 
	 * Spring boot beispiel für Consume RestServices
	 * 1. Tutorial done
	 * 2. Tutorial/eigenen Code erweitern, done
	 * Aus .ftl file alle Produkte zurückgeben lassen (Rest Service -> Template -> Anzeigbar auf Website)
	 * (siehe Ingestor) done
	 * 2 Dockerfiles müssen parallel laufen, deswegen Docker-Port-Mapping beachten (nicht 2x 8080) -p parameter done
	 * Bei Fragen im Test für den Productcontroller schauen (productcontrollertest) done
	 * 
	 */
}
