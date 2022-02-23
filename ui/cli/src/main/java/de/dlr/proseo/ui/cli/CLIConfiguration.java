/**
 * CLIConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.cli;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Ingestor component
 * 
 * @author Dr. Thomas Bassler
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
public class CLIConfiguration {
	
	/** The file path for the prosEO CLI syntax file */
	@Value("${proseo.cli.syntaxfile}")
	private String cliSyntaxFile;
	
	/** Check whether CLI should actually be started (default true) */
	@Value("${proseo.cli.start:true}")
	private Boolean cliStart;
	
	/**
	 * Gets the file path to the CLI syntax file
	 * 
	 * @return the cliSyntaxFile
	 */
	public String getCliSyntaxFile() {
		return cliSyntaxFile;
	}
	
	/**
	 * Indicate whether the CLI prompt should be started
	 * 
	 * @return the cli start flag
	 */
	public Boolean getCliStart() {
		return cliStart;
	}

}
