package de.dlr.proseo.notification;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * The NotificationService application
 * 
 * @author Ernst Melchinger
 *
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages = { "de.dlr.proseo" })
public class NotificationService implements CommandLineRunner {

	private static ProseoLogger logger = new ProseoLogger(NotificationService.class);

	/**
	 * Initialize and run application
	 * 
	 * @param args command line arguments
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication spa = new SpringApplication(NotificationService.class);
		spa.run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
