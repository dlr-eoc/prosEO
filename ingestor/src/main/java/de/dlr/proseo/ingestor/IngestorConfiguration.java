/**
 * IngestorConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration class for the prosEO Ingestor component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
@ConfigurationProperties(prefix="proseo")
public class IngestorConfiguration {
	/** The database host (hostname or IP address) */
	private String dbhost;
	/** The database TCP port */
	private String dbport;
	/** The database user to connect with */
	private String dbuser;
	/** The database password to use for connections */
	private String dbpass;
	
	public String getDbhost() {
		return dbhost;
	}
	public void setDbhost(String dbhost) {
		this.dbhost = dbhost;
	}
	
	public String getDbport() {
		return dbport;
	}
	public void setDbport(String dbport) {
		this.dbport = dbport;
	}
	
	public String getDbuser() {
		return dbuser;
	}
	public void setDbuser(String dbuser) {
		this.dbuser = dbuser;
	}
	
	public String getDbpass() {
		return dbpass;
	}
	public void setDbpass(String dbpass) {
		this.dbpass = dbpass;
	}
}
