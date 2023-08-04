/**
 * UserManagerApplication.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * prosEO User Manager application
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootApplication
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan(basePackages = { "de.dlr.proseo" })
@EnableJpaRepositories(basePackages = { "de.dlr.proseo.model.dao", "de.dlr.proseo.usermgr.dao" })
public class UserManagerApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(UserManagerApplication.class, args);
	}

}