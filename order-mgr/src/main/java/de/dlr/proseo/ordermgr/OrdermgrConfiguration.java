/**
 * OrdermgrConfiguration.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Order Manager component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class OrdermgrConfiguration {

	private static final int MSG_ID_INVALID_TIMEOUT = 2079;
	private static final String MSG_INVALID_TIMEOUT = "(I%d) Invalid timeout value %s found in configuration, using default %d";
	
	// Default connection timeout is 30 s
	private static final Long DEFAULT_TIMEOUT = 30000L;

	/** The URL of the prosEO Ingestor */
	@Value("${proseo.ingestor.url}")
	private String ingestorUrl;
	
	/** Connection timeout [ms] for Ingestor */
	@Value("${proseo.ingestor.timeout}")
	private String ingestorTimeout;
	private Long ingestorTimeoutLong = null;
	
	/** User for Ingestor connections (must have PRODUCT_GENERATOR role) */
	@Value("${proseo.ingestor.user}")
	private String ingestorUser;
	
	/** Password for Ingestor connections */
	@Value("${proseo.ingestor.password}")
	private String ingestorPassword;

	/** Log host for influxDB (default null means no monitor logging will take place) */
	@Value("${proseo.log.host:#{null}}")
	private String logHost;
		
	/** Log token for influxDB (default "EMPTY" will probably fail at application level, but avoids NullPointerException) */
	@Value("${proseo.log.token:EMPTY}")
	private String logToken;

	/** Log organization for influxDB (default "proseo") */
	@Value("${proseo.log.org:proseo}")
	private String logOrg;

	/** Log bucket for influxDB (default "production") */
	@Value("${proseo.log.bucket:production}")
	private String logBucket;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrdermgrConfiguration.class);
	
	/**
	 * Gets the URL of the prosEO Ingestor component
	 * 
	 * @return the Ingestor URL
	 */
	public String getIngestorUrl() {
		return ingestorUrl;
	}
	
	/**
	 * Gets the timeout in milliseconds for Ingestor connections
	 * 
	 * @return the ingestorTimeout
	 */
	public long getIngestorTimeout() {
		if (null == ingestorTimeoutLong) {
			if (null == ingestorTimeout) {
				ingestorTimeoutLong = DEFAULT_TIMEOUT;
			} else {
				try {
					ingestorTimeoutLong = Long.parseLong(ingestorTimeout);
				} catch (NumberFormatException e) {
					logger.warn(String.format(MSG_INVALID_TIMEOUT, MSG_ID_INVALID_TIMEOUT, ingestorTimeout, DEFAULT_TIMEOUT));
					ingestorTimeoutLong = DEFAULT_TIMEOUT;
				} 
			}
		}
		return ingestorTimeoutLong.longValue();
	}

	/**
	 * Gets the user for the prosEO Ingestor component
	 * 
	 * @return the Ingestor user
	 */
	public String getIngestorUser() {
		return ingestorUser;
	}

	/**
	 * Gets the password for the prosEO Ingestor component
	 * 
	 * @return the Ingestor password
	 */
	public String getIngestorPassword() {
		return ingestorPassword;
	}

	/**
	 * Gets the connection string (protocol, host and port) for the monitoring host
	 * 
	 * @return the logging host URL
	 */
	public String getLogHost() {
		return logHost;
	}

	/**
	 * Gets the authentication token for the monitoring host
	 * 
	 * @return the log authentication token
	 */
	public String getLogToken() {
		return logToken;
	}
	
	/**
	 * Gets the organization to use for monitoring calls
	 * 
	 * @return the logging organization
	 */
	public String getLogOrg() {
		return logOrg;
	}

	/**
	 * Gets the bucket in the monitoring database to use
	 * 
	 * @return the logging bucket
	 */
	public String getLogBucket() {
		return logBucket;
	}

}
