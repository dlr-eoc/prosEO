/**
 * EdipMonitorConfiguration.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.edipmon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO EDIP Monitor component
 *
 * @author Dr. Thomas Bassler
 *
 */
@Configuration
@ConfigurationProperties(prefix = "proseo")
public class EdipMonitorConfiguration {

	/** The EDIP Monitor identifier */
	@Value("${proseo.edip.id}")
	private String edipId;

	/** The path to the EDIP directory (mounted WebDAV volume) */
	@Value("${proseo.edip.directory}")
	private String edipDirectoryPath;

	/** The satellite identifier (e. g. "S1B") */
	@Value("${proseo.edip.satellite}")
	private String edipSatellite;

	/** The interval between pickup point checks in milliseconds */
	@Value("${proseo.edip.check.interval}")
	private Long edipCheckInterval;

	/** The path to the file for storing transfer history */
	@Value("${proseo.edip.history.file}")
	private String edipHistoryPath;

	/** The period to retain transfer history entries for, in milliseconds */
	@Value("${proseo.edip.history.retention}")
	private Long edipHistoryRetention;

	/** The interval to truncate transfer history file in milliseconds */
	@Value("${proseo.edip.history.truncate.interval}")
	private Long edipTruncateInterval;

	/** The minimum size in bytes of a file to be used for performance measurements */
	@Value("${proseo.edip.performance.minsize}")
	private Long edipPerformanceMinSize;

	/** The path to the target CADU directory (for L0 processing) */
	@Value("${proseo.l0.directory.cadu}")
	private String l0CaduDirectoryPath;

	/** The L0 processor command (a single command taking the CADU directory as argument) */
	@Value("${proseo.l0.command}")
	private String l0Command;

	/** Maximum number of parallel transfer sessions */
	@Value("${proseo.edip.session.maxthreads:1}")
	private Integer maxDownloadThreads;

	/** Interval in millliseconds to check for completed transfer sessions */
	@Value("${proseo.edip.session.wait:500}")
	private Integer taskWaitInterval;

	/** Maximum number of wait cycles for transfer session completion checks */
	@Value("${proseo.edip.session.maxcycles:3600}")
	private Integer maxWaitCycles;

	/** Maximum number of parallel file download threads within a download session */
	@Value("${proseo.edip.file.maxthreads:1}")
	private Integer maxFileDownloadThreads;

	/** Interval in millliseconds to check for completed file downloads */
	@Value("${proseo.edip.file.wait:500}")
	private Integer fileWaitInterval;

	/** Maximum number of wait cycles for file download completion checks */
	@Value("${proseo.edip.file.maxcycles:3600}")
	private Integer maxFileWaitCycles;

	/**
	 * Gets the EDIP Monitor identifier
	 *
	 * @return the EDIP Monitor identifier
	 */
	public String getEdipId() {
		return edipId;
	}

	/**
	 * Gets the path to the EDIP directory
	 *
	 * @return the EDIP directory path
	 */
	public String getEdipDirectoryPath() {
		return edipDirectoryPath;
	}

	/**
	 * Gets the satellite identifier
	 *
	 * @return the 3-character satellite identifier
	 */
	public String getEdipSatellite() {
		return edipSatellite;
	}

	/**
	 * Gets the path to the file for storing transfer history
	 *
	 * @return the EDIP transfer history file path
	 */
	public String getEdipHistoryPath() {
		return edipHistoryPath;
	}

	/**
	 * Gets the interval between pickup point checks
	 *
	 * @return the EDIP check interval in ms
	 */
	public Long getEdipCheckInterval() {
		return edipCheckInterval;
	}

	/**
	 * Gets the interval to truncate transfer history file
	 *
	 * @return the EDIP history truncation interval in ms
	 */
	public Long getEdipTruncateInterval() {
		return edipTruncateInterval;
	}

	/**
	 * Gets the period to retain transfer history entries for
	 *
	 * @return the EDIP history retention period in ms
	 */
	public Long getEdipHistoryRetention() {
		return edipHistoryRetention;
	}

	/**
	 * Gets the minimum size for files used in performance measurements
	 *
	 * @return the minimum file size in bytes
	 */
	public Long getEdipPerformanceMinSize() {
		return edipPerformanceMinSize;
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