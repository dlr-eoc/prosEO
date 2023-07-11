/**
 * XbipMonitorConfiguration.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.xbipmon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO XBIP Monitor component
 *
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix = "proseo")
public class XbipMonitorConfiguration {

	/** The XBIP Monitor identifier */
	@Value("${proseo.xbip.id}")
	private String xbipId;

	/** The path to the XBIP directory (mounted WebDAV volume) */
	@Value("${proseo.xbip.directory}")
	private String xbipDirectoryPath;

	/** The satellite identifier (e. g. "S1B") */
	@Value("${proseo.xbip.satellite}")
	private String xbipSatellite;

	/** The interval between pickup point checks in milliseconds */
	@Value("${proseo.xbip.check.interval}")
	private Long xbipCheckInterval;

	/** The Retrieval delay in milliseconds (to avoid concurrent XBIP access by multiple PDGSs, default 0) */
	@Value("${proseo.xbip.retrieval.delay:0}")
	private Long xbipRetrievalDelay;

	/** The path to the file for storing transfer history */
	@Value("${proseo.xbip.history.file}")
	private String xbipHistoryPath;

	/** The period to retain transfer history entries for, in milliseconds */
	@Value("${proseo.xbip.history.retention}")
	private Long xbipHistoryRetention;

	/** The interval to truncate transfer history file in milliseconds */
	@Value("${proseo.xbip.history.truncate.interval}")
	private Long xbipTruncateInterval;

	/** The minimum size in bytes of a file to be used for performance measurements */
	@Value("${proseo.xbip.performance.minsize}")
	private Long xbipPerformanceMinSize;

	/** The path to the target CADU directory (for L0 processing) */
	@Value("${proseo.l0.directory.cadu}")
	private String l0CaduDirectoryPath;

	/** The L0 processor command (a single command taking the CADU directory as argument) */
	@Value("${proseo.l0.command}")
	private String l0Command;

	/** Maximum number of parallel transfer sessions */
	@Value("${proseo.xbip.session.maxthreads:1}")
	private Integer maxDownloadThreads;

	/** Interval in millliseconds to check for completed transfer sessions */
	@Value("${proseo.xbip.session.wait:500}")
	private Integer taskWaitInterval;

	/** Maximum number of wait cycles for transfer session completion checks */
	@Value("${proseo.xbip.session.maxcycles:3600}")
	private Integer maxWaitCycles;

	/** Maximum number of parallel file download threads within a download session */
	@Value("${proseo.xbip.file.maxthreads:1}")
	private Integer maxFileDownloadThreads;

	/** Interval in millliseconds to check for completed file downloads */
	@Value("${proseo.xbip.file.wait:500}")
	private Integer fileWaitInterval;

	/** Maximum number of wait cycles for file download completion checks */
	@Value("${proseo.xbip.file.maxcycles:3600}")
	private Integer maxFileWaitCycles;

	/**
	 * Gets the XBIP Monitor identifier
	 *
	 * @return the XBIP Monitor identifier
	 */
	public String getXbipId() {
		return xbipId;
	}

	/**
	 * Gets the path to the XBIP directory
	 *
	 * @return the XBIP directory path
	 */
	public String getXbipDirectoryPath() {
		return xbipDirectoryPath;
	}

	/**
	 * Gets the satellite identifier
	 *
	 * @return the 3-character satellite identifier
	 */
	public String getXbipSatellite() {
		return xbipSatellite;
	}

	/**
	 * Gets the path to the file for storing transfer history
	 *
	 * @return the XBIP transfer history file path
	 */
	public String getXbipHistoryPath() {
		return xbipHistoryPath;
	}

	/**
	 * Gets the interval between pickup point checks
	 *
	 * @return the XBIP check interval in ms
	 */
	public Long getXbipCheckInterval() {
		return xbipCheckInterval;
	}

	/**
	 * Gets the retrieval delay for the pickup point
	 *
	 * @return the XBIP retrieval delay in ms
	 */
	public Long getXbipRetrievalDelay() {
		return xbipRetrievalDelay;
	}

	/**
	 * Gets the interval to truncate transfer history file
	 *
	 * @return the XBIP history truncation interval in ms
	 */
	public Long getXbipTruncateInterval() {
		return xbipTruncateInterval;
	}

	/**
	 * Gets the period to retain transfer history entries for
	 *
	 * @return the XBIP history retention period in ms
	 */
	public Long getXbipHistoryRetention() {
		return xbipHistoryRetention;
	}

	/**
	 * Gets the minimum size for files used in performance measurements
	 *
	 * @return the minimum file size in bytes
	 */
	public Long getXbipPerformanceMinSize() {
		return xbipPerformanceMinSize;
	}

	/**
	 * Gets the path to the target CADU directory
	 *
	 * @return the CADU directory path
	 */
	public String getL0CaduDirectoryPath() {
		return l0CaduDirectoryPath;
	}

	/**
	 * Gets the L0 processor command
	 *
	 * @return the L0 processor command
	 */
	public String getL0Command() {
		return l0Command;
	}

	/**
	 * Gets the maximum number of parallel transfer session threads
	 *
	 * @return the maximum number of transfer session threads
	 */
	public Integer getMaxDownloadThreads() {
		return maxDownloadThreads;
	}

	/**
	 * Gets the interval to check for completed transfer sessions
	 *
	 * @return the transfer session wait interval in millliseconds
	 */
	public Integer getTaskWaitInterval() {
		return taskWaitInterval;
	}

	/**
	 * Gets the maximum number of wait cycles for transfer session completion checks
	 *
	 * @return the maximum number of wait cycles
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

}