/**
 * ProductArchiveManagerConfiguration.java
 * 
 * (C) 2023 DLR
 */
package de.dlr.proseo.archivemgr;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Product Archive Manager component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class ProductArchiveManagerConfiguration {

}
