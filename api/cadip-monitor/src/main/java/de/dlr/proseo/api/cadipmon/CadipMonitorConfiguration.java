/**
 * CadipMonitorConfiguration.java
 * 
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.cadipmon;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO CADIP Monitor component
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
public class CadipMonitorConfiguration {
	
	/** The CADIP Monitor identifier */
	@Value("${proseo.cadip.id}")
	private String cadipId;
	
	/** The satellite identifier (e. g. "S1B") */
	@Value("${proseo.cadip.satellite}")
	private String cadipSatellite;
	
	/** The base URI of the CADIP (protocol, host name, port; no terminating slash) */
	@Value("${proseo.cadip.baseuri}")
	private String cadipBaseUri;
	
	/** The CADIP context, if any (any URI component(s) before "odata"; no starting or terminating slash) */
	@Value("${proseo.cadip.context}")
	private String cadipContext;
	
	/** Flag indicating whether token-based authentication shall be used */
	@Value("${proseo.cadip.usetoken:false}")
	private Boolean cadipUseToken;
	
	/** The URI for requesting a bearer token (full URL) */
	@Value("${proseo.cadip.tokenuri}")
	private String cadipTokenUri;
	
	/** The CADIP username */
	@Value("${proseo.cadip.user}")
	private String cadipUser;
	
	/** The CADIP password */
	@Value("${proseo.cadip.password}")
	private String cadipPassword;
	
	/** The CADIP client ID (optional, only for OpenID-based token requests) */
	@Value("${proseo.cadip.client.id:#{null}}")
	private String cadipClientId;
	
	/** The CADIP client secret (only for OpenID-based token requests; mandatory if client ID is set) */
	@Value("${proseo.cadip.client.secret:#{null}}")
	private String cadipClientSecret;
	
	/** Flag whether to send cliend ID and secret in body (only for OpenID-based token requests; mandatory if client ID is set) */
	@Value("${proseo.cadip.client.sendinbody:#{null}}")
	private Boolean cadipClientSendInBody;
	
	/** The interval between pickup point checks in milliseconds */
	@Value("${proseo.cadip.check.interval}")
	private Long cadipCheckInterval;
	
	/** The interval between checks for available session files in milliseconds */
	@Value("${proseo.cadip.session.interval}")
	private Long cadipSessionInterval;
	
	/** The retrieval delay in milliseconds (to avoid concurrent CADIP access by multiple PDGSs, default 0) */
	@Value("${proseo.cadip.retrieval.delay:0}")
	private Long cadipRetrievalDelay;
	
	/** The maximum allowed duration for download of a single session in milliseconds */
	@Value("${proseo.cadip.retrieval.timeout}")
	private Long cadipRetrievalTimeout;
	
	/** The path to the file for storing transfer history */
	@Value("${proseo.cadip.history.file}")
	private String cadipHistoryPath;
	
	/** The period to retain transfer history entries for, in milliseconds */
	@Value("${proseo.cadip.history.retention}")
	private Long cadipHistoryRetention;
	
	/** The interval to truncate transfer history file in milliseconds */
	@Value("${proseo.cadip.history.truncate.interval}")
	private Long cadipTruncateInterval;
	
	/** Maximum number of parallel transfer sessions */
	@Value("${proseo.cadip.thread.max:1}")
	private Integer maxDownloadThreads;
	
	/** Interval in millliseconds to check for completed transfer sessions */
	@Value("${proseo.cadip.thread.wait:500}")
	private Integer taskWaitInterval;
	
	/** Maximum number of wait cycles for transfer session completion checks */
	@Value("${proseo.cadip.thread.cycles:3600}")
	private Integer maxWaitCycles;
	
	/** Maximum number of parallel file download threads within a download session */
	@Value("${proseo.cadip.file.maxthreads:1}")
	private Integer maxFileDownloadThreads;
	
	/** Interval in millliseconds to check for completed file downloads */
	@Value("${proseo.cadip.file.wait:500}")
	private Integer fileWaitInterval;
	
	/** Maximum number of wait cycles for file download completion checks */
	@Value("${proseo.cadip.file.maxcycles:3600}")
	private Integer maxFileWaitCycles;
	
	/** The minimum size in bytes of a file to be used for performance measurements */
	@Value("${proseo.cadip.performance.minsize}")
	private Long cadipPerformanceMinSize;
	
	/** The path to the target CADU directory (for L0 processing) */
	@Value("${proseo.l0.directory.cadu}")
	private String l0CaduDirectoryPath;
	
	/**
	 * Gets the CADIP Monitor identifier
	 * 
	 * @return the CADIP Monitor identifier
	 */
	public String getCadipId() {
		return cadipId;
	}

	/**
	 * Gets the satellite identifier
	 * 
	 * @return the satellite identifier
	 */
	public String getCadipSatellite() {
		return cadipSatellite;
	}

	/**
	 * Gets the base URI of the CADIP
	 * 
	 * @return the CADIP base URI (without trailing slash)
	 */
	public String getCadipBaseUri() {
		while (cadipBaseUri.endsWith("/")) {
			cadipBaseUri = cadipBaseUri.substring(0, cadipBaseUri.length() - 1);
		}
		return cadipBaseUri;
	}

	/**
	 * Gets the CADIP context
	 * 
	 * @return the CADIP context (without starting or terminating slashes)
	 */
	public String getCadipContext() {
		while (cadipContext.startsWith("/")) {
			cadipContext = cadipContext.substring(1);
		}
		while (cadipContext.endsWith("/")) {
			cadipContext = cadipContext.substring(0, cadipContext.length() - 1);
		}
		return cadipContext;
	}
	
	/**
	 * Indicates whether token-based authentication shall be used
	 * 
	 * @return true, if token-based authentication shall be used, false otherwise
	 */
	public Boolean getCadipUseToken() {
		return cadipUseToken;
	}

	/**
	 * Gets the URI for requesting a bearer token
	 * 
	 * @return the bearer token URI (without starting or terminating slashes)
	 */
	public String getCadipTokenUri() {
		while (cadipTokenUri.startsWith("/")) {
			cadipTokenUri = cadipTokenUri.substring(1);
		}
		while (cadipTokenUri.endsWith("/")) {
			cadipTokenUri = cadipTokenUri.substring(0, cadipTokenUri.length() - 1);
		}
		return cadipTokenUri;
	}

	/**
	 * Gets the CADIP username
	 * 
	 * @return the username
	 */
	public String getCadipUser() {
		return cadipUser;
	}

	/**
	 * Gets the CADIP password
	 * 
	 * @return the password
	 */
	public String getCadipPassword() {
		return cadipPassword;
	}

	/**
	 * Gets the client ID for OpenID token requests
	 * 
	 * @return the OpenID client ID
	 */
	public String getCadipClientId() {
		return cadipClientId;
	}

	/**
	 * Gets the client secret for OpenID token requests
	 * 
	 * @return the OpenID client secret
	 */
	public String getCadipClientSecret() {
		return cadipClientSecret;
	}

	/**
	 * Indicates whether to send the client ID and secret in the message body for OpenID token requests
	 * 
	 * @return true, if client ID and secret are to be sent in the body, false otherwise
	 */
	public Boolean getCadipClientSendInBody() {
		return cadipClientSendInBody;
	}

	/**
	 * Gets the path to the file for storing transfer history
	 * 
	 * @return the CADIP transfer history file path
	 */
	public String getCadipHistoryPath() {
		return cadipHistoryPath;
	}

	/**
	 * Gets the interval between pickup point checks
	 * 
	 * @return the CADIP check interval in ms
	 */
	public Long getCadipCheckInterval() {
		return cadipCheckInterval;
	}

	/**
	 * Gets the interval between checks for available session files
	 * 
	 * @return the interval between session checks in ms
	 */
	public Long getCadipSessionInterval() {
		return cadipSessionInterval;
	}

	/**
	 * Gets the retrieval delay for the pickup point
	 * 
	 * @return the CADIP retrieval delay in ms
	 */
	public Long getCadipRetrievalDelay() {
		return cadipRetrievalDelay;
	}

	/**
	 * Gets the maximum allowed duration for download of a single session
	 * 
	 * @return the retrieval timeout in ms
	 */
	public Long getCadipRetrievalTimeout() {
		return cadipRetrievalTimeout;
	}

	/**
	 * Gets the interval to truncate transfer history file
	 * 
	 * @return the CADIP history truncation interval in ms
	 */
	public Long getCadipTruncateInterval() {
		return cadipTruncateInterval;
	}

	/**
	 * Gets the period to retain transfer history entries for
	 * 
	 * @return the CADIP history retention period in ms
	 */
	public Long getCadipHistoryRetention() {
		return cadipHistoryRetention;
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
	 * Gets the maximum number of parallel file download threads within a download session
	 * 
	 * @return the maximum number of parallel file download threads
	 */
	public Integer getMaxFileDownloadThreads() {
		return maxFileDownloadThreads;
	}

	/**
	 * Gets the interval to check for completed file downloads
	 * 
	 * @return the check interval in millliseconds
	 */
	public Integer getFileWaitInterval() {
		return fileWaitInterval;
	}

	/**
	 * Gets the maximum number of wait cycles for file download completion checks
	 * 
	 * @return the maximum number of wait cycles
	 */
	public Integer getMaxFileWaitCycles() {
		return maxFileWaitCycles;
	}

	/**
	 * Gets the minimum size for files used in performance measurements
	 * 
	 * @return the minimum file size in bytes
	 */
	public Long getCadipPerformanceMinSize() {
		return cadipPerformanceMinSize;
	}

	/**
	 * Gets the path to the target CADU directory
	 * 
	 * @return the CADU directory path
	 */
	public String getL0CaduDirectoryPath() {
		return l0CaduDirectoryPath;
	}

}
