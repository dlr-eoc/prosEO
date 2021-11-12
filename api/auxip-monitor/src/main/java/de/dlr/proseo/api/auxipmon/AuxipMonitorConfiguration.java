/**
 * AuxipMonitorConfiguration.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.auxipmon;

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
	
	/** The path to the AUXIP directory (mounted WebDAV volume) */
	@Value("${proseo.auxip.directory}")
	private String auxipDirectoryPath;
	
	/** The satellite identifier (e. g. "S1B") */
	@Value("${proseo.auxip.satellite}")
	private String auxipSatellite;
	
	/** The X-band station unit ID (default "00") */
	@Value("${proseo.auxip.station.unit:00}")
	private String auxipStationUnit;
	
	/** The interval between pickup point checks in milliseconds */
	@Value("${proseo.auxip.check.interval}")
	private Long auxipCheckInterval;
	
	/** The path to the file for storing transfer history */
	@Value("${proseo.auxip.history.file}")
	private String auxipHistoryPath;
	
	/** The period to retain transfer history entries for, in milliseconds */
	@Value("${proseo.auxip.history.retention}")
	private Long auxipHistoryRetention;
	
	/** The interval to truncate transfer history file in milliseconds */
	@Value("${proseo.auxip.history.truncate.interval}")
	private Long auxipTruncateInterval;
	
	/** The minimum size in bytes of a file to be used for performance measurements */
	@Value("${proseo.auxip.performance.minsize}")
	private Long auxipPerformanceMinSize;
	
	/** The path to the target CADU directory (for L0 processing) */
	@Value("${proseo.l0.directory.cadu}")
	private String l0CaduDirectoryPath;
	
	/** The L0 processor command (a single command taking the CADU directory as argument) */
	@Value("${proseo.l0.command}")
	private String l0Command;
	
	/**
	 * Gets the AUXIP Monitor identifier
	 * 
	 * @return the AUXIP Monitor identifier
	 */
	public String getAuxipId() {
		return auxipId;
	}

	/**
	 * Gets the path to the AUXIP directory
	 * 
	 * @return the AUXIP directory path
	 */
	public String getAuxipDirectoryPath() {
		return auxipDirectoryPath;
	}

	/**
	 * Gets the satellite identifier
	 * 
	 * @return the 3-character satellite identifier
	 */
	public String getAuxipSatellite() {
		return auxipSatellite;
	}

	/**
	 * Gets the X-band station unit ID
	 * 
	 * @return the 2-digit station unit ID
	 */
	public String getAuxipStationUnit() {
		return auxipStationUnit;
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
	 * Gets the minimum size for files used in performance measurements
	 * 
	 * @return the minimum file size in bytes
	 */
	public Long getAuxipPerformanceMinSize() {
		return auxipPerformanceMinSize;
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

}
