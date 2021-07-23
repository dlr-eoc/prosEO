/**
 * XbipMonitorConfiguration.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.xbipmon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@ConfigurationProperties(prefix="proseo")
public class XbipMonitorConfiguration {
	
	/** The path to the XBIP directory (mounted WebDAV volume) */
	@Value("${xbip.directory}")
	private String xbipDirectoryPath;
	
	/** The satellite identifier (e. g. "S1B") */
	@Value("${xbip.satellite}")
	private String xbipSatellite;
	
	/** The X-band station unit ID (default "00") */
	@Value("${xbip.station.unit:00}")
	private String xbipStationUnit;
	
	/** The interval between pickup point checks in milliseconds */
	@Value("${xbip.check.interval}")
	private Long xbipCheckInterval;
	
	/** The path to the file for storing transfer history */
	@Value("${xbip.history.file}")
	private String xbipHistoryPath;
	
	/** The period to retain transfer history entries for, in milliseconds */
	@Value("${xbip.history.retention}")
	private Long xbipHistoryRetention;
	
	/** The interval to truncate transfer history file in milliseconds */
	@Value("${xbip.history.truncate.interval}")
	private Long xbipTruncateInterval;
	
	/** The path to the target CADU directory (for L0 processing) */
	@Value("${l0.directory.cadu}")
	private String l0CaduDirectoryPath;
	
	/** The L0 processor command (a single command taking the CADU directory as argument) */
	@Value("${l0.command}")
	private String l0Command;
	
	/** A logger for this class */
//	private static Logger logger = LoggerFactory.getLogger(XbipMonitorConfiguration.class);
	
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
	 * Gets the X-band station unit ID
	 * 
	 * @return the 2-digit station unit ID
	 */
	public String getXbipStationUnit() {
		return xbipStationUnit;
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
