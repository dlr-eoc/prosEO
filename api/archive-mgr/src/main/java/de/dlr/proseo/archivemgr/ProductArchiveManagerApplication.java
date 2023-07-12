/**
 * ProductArchiveManagerApplication.java
 *
 * (C) 2023 DLR
 */
package de.dlr.proseo.archivemgr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * prosEO Product Archive Manager application
 *
 * @author Dr. Thomas Bassler
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages = { "de.dlr.proseo" })
@EnableJpaRepositories(basePackages = { "de.dlr.proseo.model.dao" })
public class ProductArchiveManagerApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ProductArchiveManagerApplication.class, args);
	}

}