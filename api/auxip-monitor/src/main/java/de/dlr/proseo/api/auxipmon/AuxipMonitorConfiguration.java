/**
 * AuxipMonitorConfiguration.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.auxipmon;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO AUXIP Monitor component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
public class AuxipMonitorConfiguration {
	
	/** The AUXIP Monitor identifier */
	@Value("${proseo.auxip.id}")
	private String auxipId;
	
	/** The base URI of the AUXIP (protocol, host name, port; no terminating slash) */
	@Value("${proseo.auxip.baseuri}")
	private String auxipBaseUri;
	
	/** The AUXIP context, if any (any URI component(s) before "odata"; no starting or terminating slash) */
	@Value("${proseo.auxip.context}")
	private String auxipContext;
	
	/** Flag indicating whether token-based authentication shall be used */
	@Value("${proseo.auxip.usetoken:false}")
	private Boolean auxipUseToken;
	
	/** The URI for requesting a bearer token (full URL) */
	@Value("${proseo.auxip.tokenuri}")
	private String auxipTokenUri;
	
	/** The AUXIP username */
	@Value("${proseo.auxip.user}")
	private String auxipUser;
	
	/** The AUXIP password */
	@Value("${proseo.auxip.password}")
	private String auxipPassword;
	
	/** The AUXIP client ID (optional, only for OpenID-based token requests) */
	@Value("${proseo.auxip.client.id:#{null}}")
	private String auxipClientId;
	
	/** The AUXIP client secret (only for OpenID-based token requests; mandatory if client ID is set) */
	@Value("${proseo.auxip.client.secret:#{null}}")
	private String auxipClientSecret;
	
	/** Flag whether to send cliend ID and secret in body (only for OpenID-based token requests; mandatory if client ID is set) */
	@Value("${proseo.auxip.client.sendinbody:#{null}}")
	private Boolean auxipClientSendInBody;
	
	/** The product types to select */
	@Value("${proseo.auxip.producttypes}")
	private String auxipProductTypes;
	
	/** The interval between pickup point checks in milliseconds */
	@Value("${proseo.auxip.check.interval}")
	private Long auxipCheckInterval;
	
	/** The interval between individual chunk retrievals in milliseconds */
	@Value("${proseo.auxip.chunk.interval}")
	private Long auxipChunkInterval;
	
	/** The path to the file for storing transfer history */
	@Value("${proseo.auxip.history.file}")
	private String auxipHistoryPath;
	
	/** The period to retain transfer history entries for, in milliseconds */
	@Value("${proseo.auxip.history.retention}")
	private Long auxipHistoryRetention;
	
	/** The interval to truncate transfer history file in milliseconds */
	@Value("${proseo.auxip.history.truncate.interval}")
	private Long auxipTruncateInterval;
	
	/** Maximum number of parallel transfer threads */
	@Value("${proseo.auxip.thread.max:1}")
	private Integer maxDownloadThreads;
	
	/** Interval in millliseconds to check for completed transfers */
	@Value("${proseo.auxip.thread.wait:500}")
	private Integer taskWaitInterval;
	
	/** Maximum number of wait cycles for transfer completion checks */
	@Value("${proseo.auxip.thread.cycles:3600}")
	private Integer maxWaitCycles;
	
	/** The minimum size in bytes of a file to be used for performance measurements */
	@Value("${proseo.auxip.performance.minsize}")
	private Long auxipPerformanceMinSize;
	
	/** The path to the target AUX file directory (for ingestion) */
	@Value("${proseo.auxip.directory}")
	private String auxipDirectoryPath;
	
	/**
	 * Gets the AUXIP Monitor identifier
	 * 
	 * @return the AUXIP Monitor identifier
	 */
	public String getAuxipId() {
		return auxipId;
	}

	/**
	 * Gets the base URI of the AUXIP
	 * 
	 * @return the AUXIP base URI (without trailing slash)
	 */
	public String getAuxipBaseUri() {
		while (auxipBaseUri.endsWith("/")) {
			auxipBaseUri = auxipBaseUri.substring(0, auxipBaseUri.length() - 1);
		}
		return auxipBaseUri;
	}

	/**
	 * Gets the AUXIP context
	 * 
	 * @return the AUXIP context (without starting or terminating slashes)
	 */
	public String getAuxipContext() {
		while (auxipContext.startsWith("/")) {
			auxipContext = auxipContext.substring(1);
		}
		while (auxipContext.endsWith("/")) {
			auxipContext = auxipContext.substring(0, auxipContext.length() - 1);
		}
		return auxipContext;
	}
	
	/**
	 * Indicates whether token-based authentication shall be used
	 * 
	 * @return true, if token-based authentication shall be used, false otherwise
	 */
	public Boolean getAuxipUseToken() {
		return auxipUseToken;
	}

	/**
	 * Gets the URI for requesting a bearer token
	 * 
	 * @return the bearer token URI (without starting or terminating slashes)
	 */
	public String getAuxipTokenUri() {
		while (auxipTokenUri.startsWith("/")) {
			auxipTokenUri = auxipTokenUri.substring(1);
		}
		while (auxipTokenUri.endsWith("/")) {
			auxipTokenUri = auxipTokenUri.substring(0, auxipTokenUri.length() - 1);
		}
		return auxipTokenUri;
	}

	/**
	 * Gets the AUXIP username
	 * 
	 * @return the username
	 */
	public String getAuxipUser() {
		return auxipUser;
	}

	/**
	 * Gets the AUXIP password
	 * 
	 * @return the password
	 */
	public String getAuxipPassword() {
		return auxipPassword;
	}

	/**
	 * Gets the client ID for OpenID token requests
	 * 
	 * @return the OpenID client ID
	 */
	public String getAuxipClientId() {
		return auxipClientId;
	}

	/**
	 * Gets the client secret for OpenID token requests
	 * 
	 * @return the OpenID client secret
	 */
	public String getAuxipClientSecret() {
		return auxipClientSecret;
	}

	/**
	 * Indicates whether to send the client ID and secret in the message body for OpenID token requests
	 * 
	 * @return true, if client ID and secret are to be sent in the body, false otherwise
	 */
	public Boolean getAuxipClientSendInBody() {
		return auxipClientSendInBody;
	}

	/**
	 * Gets the list of product types to retrieve from AUXIP
	 * 
	 * @return the a list of product types
	 */
	public List<String> getAuxipProductTypes() {
		return Arrays.asList(auxipProductTypes.split(","));
	}

	/**
	 * Gets the path to the file for storing transfer history
	 * 
	 * @return the AUXIP transfer history file path
	 */
	public String getAuxipHistoryPath() {
		return auxipHistoryPath;
	}

	/**
	 * Gets the interval between pickup point checks
	 * 
	 * @return the AUXIP check interval in ms
	 */
	public Long getAuxipCheckInterval() {
		return auxipCheckInterval;
	}

	/**
	 * Gets the interval between individual chunk retrievals
	 * 
	 * @return the interval between chunk retrievals in ms
	 */
	public Long getAuxipChunkInterval() {
		return auxipChunkInterval;
	}

	/**
	 * Gets the interval to truncate transfer history file
	 * 
	 * @return the AUXIP history truncation interval in ms
	 */
	public Long getAuxipTruncateInterval() {
		return auxipTruncateInterval;
	}

	/**
	 * Gets the period to retain transfer history entries for
	 * 
	 * @return the AUXIP history retention period in ms
	 */
	public Long getAuxipHistoryRetention() {
		return auxipHistoryRetention;
	}

	/**
	 * Gets the maximum number of parallel transfer threads
	 * 
	 * @return the maximum number of threads
	 */
	public Integer getMaxDownloadThreads() {
		return maxDownloadThreads;
	}

	/**
	 * Gets the interval in millliseconds to check for completed transfers
	 * 
	 * @return the download wait interval
	 */
	public Integer getTaskWaitInterval() {
		return taskWaitInterval;
	}

	/**
	 * Gets the maximum number of wait cycles for transfer completion checks
	 * 
	 * @return the number of wait cycles
	 */
	public Integer getMaxWaitCycles() {
		return maxWaitCycles;
	}

	/**
	 * Gets the minimum size for files used in performance measurements
	 * 
	 * @return the minimum file size in bytes
	 */
	public Long getAuxipPerformanceMinSize() {
		return auxipPerformanceMinSize;
	}

	/**
	 * Gets the target path to store AUX files for ingestion
	 * 
	 * @return the path to the target directory
	 */
	public String getAuxipDirectoryPath() {
		return auxipDirectoryPath;
	}

}
