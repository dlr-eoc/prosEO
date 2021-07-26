/**
 * XbipMonitor.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.xbipmon;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.api.basemon.BaseMonitor;
import de.dlr.proseo.api.basemon.TransferObject;

/**
 * Monitor for X-band Interface Points
 * 
 * X-band Interface Points are WebDAV volumes, so from within the Monitor they just look like plain POSIX volumes.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
public class XbipMonitor extends BaseMonitor {
	
	/** The path to the XBIP directory (mounted WebDAV volume) */
	private Path xbipDirectory;
	
	/** The satellite identifier (e. g. "S1B") */
	private String satelliteIdentifier;
	
	/** The X-band station unit ID */
	private String stationUnitIdentifier;

	/** Filter for directory based on station unit ID */
	private String sessionFilter;
	private static String SESSION_FILTER_FORMAT = "DCS_%s_*_dat";
	
	/** The path to the target CADU directory (for L0 processing) */
	private Path caduDirectoryPath;
	
	/** The L0 processor command (a single command taking the CADU directory as argument) */
	private String l0ProcessorCommand;

	/** The last copy performance in MiB/s */
	private Double lastCopyPerformance = 0.0;

	/** Indicator for parallel copying processes
	/* package */ Map<String, Boolean> copySuccess = new HashMap<>();

	/** The XBIP Monitor configuration to use */
	@Autowired
	private XbipMonitorConfiguration config;

	/* Message ID constants */
	private static final int MSG_ID_XBIP_NOT_READABLE = 5300;
	private static final int MSG_ID_XBIP_ENTRY_MALFORMED = 5301;
	private static final int MSG_ID_AVAILABLE_DOWNLOADS_FOUND = 5302;
	private static final int MSG_ID_TRANSFER_OBJECT_IS_NULL = 5303;
	private static final int MSG_ID_INVALID_TRANSFER_OBJECT_TYPE = 5304;
	private static final int MSG_ID_CANNOT_CREATE_TARGET_DIR = 5305;
	private static final int MSG_ID_COPY_FAILED = 5306;
	/* package */ static final int MSG_ID_COPY_FILE_FAILED = 5307;
	private static final int MSG_ID_COPY_INTERRUPTED = 5307;
	private static final int MSG_ID_COMMAND_START_FAILED = 5308;
	private static final int MSG_ID_SESSION_TRANSFER_INCOMPLETE = 5309;

	/* Message string constants */
	private static final String MSG_XBIP_NOT_READABLE = "(E%d) XBIP directory %s not readable (cause: %s)";
	private static final String MSG_XBIP_ENTRY_MALFORMED = "(E%d) Malformed XBIP directory entry %s found - skipped";
	private static final String MSG_TRANSFER_OBJECT_IS_NULL = "(E%d) Transfer object is null - skipped";
	private static final String MSG_INVALID_TRANSFER_OBJECT_TYPE = "(E%d) Transfer object %s of invalid type found - skipped";
	private static final String MSG_CANNOT_CREATE_TARGET_DIR = "(E%d) Cannot create channel directory in target directory %s - skipped";
	private static final String MSG_COPY_FAILED = "(E%d) Copying of session directory %s failed (cause: %s)";
	/* package */ static final String MSG_COPY_FILE_FAILED = "(E%d) Copying of session data file %s failed (cause: %s)";
	private static final String MSG_COPY_INTERRUPTED = "(E%d) Copying of session directory %s failed due to interrupt";
	private static final String MSG_COMMAND_START_FAILED = "(E%d) Start of L0 processing command '%s' failed (cause: %s)";

	private static final String MSG_AVAILABLE_DOWNLOADS_FOUND = "(I%d) %d session entries found for download (unfiltered)";
	private static final String MSG_SESSION_TRANSFER_INCOMPLETE = "(I%d) Transfer for session %s still incomplete - skipped";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(XbipMonitorConfiguration.class);
	
	protected static class TransferSession implements TransferObject {
		
		/** The satellite identifier */
		private String satelliteIdentifier;
		
		/** The X-band station unit ID */
		private String stationUnitIdentifier;
		
		/** The XBIP session identifier */
		private String sessionIdentifier;
		
		/** The path to the session data on the XBIP */
		private Path sessionPath;

		/**
		 * Gets the session identifier
		 * 
		 * @return the session identifier
		 */
		public String getSessionIdentifier() {
			return sessionIdentifier;
		}

		/**
		 * Sets the session identifier
		 * 
		 * @param sessionIdentifier the session identifier to set
		 */
		public void setSessionIdentifier(String sessionIdentifier) {
			this.sessionIdentifier = sessionIdentifier;
		}

		/**
		 * Gets the path to the session data
		 * 
		 * @return the session data path
		 */
		public Path getSessionPath() {
			return sessionPath;
		}

		/**
		 * Sets the path to the session data
		 * 
		 * @param sessionPath the session data path to set
		 */
		public void setSessionPath(Path sessionPath) {
			this.sessionPath = sessionPath;
		}

		/**
		 * Gets the combined transfer object identifier: satellite|station unit|session ID
		 * 
		 * @see de.dlr.proseo.api.basemon.TransferObject#getIdentifier()
		 */
		@Override
		public String getIdentifier() {
			return String.format("%s|%s|%s", satelliteIdentifier, stationUnitIdentifier, sessionIdentifier);
		}
		
	}
	
	@PostConstruct
	private void init() {
		xbipDirectory = Paths.get(config.getXbipDirectoryPath());
		satelliteIdentifier = config.getXbipSatellite();
		stationUnitIdentifier = config.getXbipStationUnit();
		xbipDirectory = xbipDirectory.resolve(satelliteIdentifier);
		sessionFilter = String.format(SESSION_FILTER_FORMAT, stationUnitIdentifier);
		
		this.setTransferHistoryFile(Paths.get(config.getXbipHistoryPath()));
		this.setCheckInterval(config.getXbipCheckInterval());
		this.setTruncateInterval(config.getXbipTruncateInterval());
		this.setHistoryRetentionDuration(Duration.ofMillis(config.getXbipHistoryRetention()));
		
		caduDirectoryPath = Paths.get(config.getL0CaduDirectoryPath());
		l0ProcessorCommand = config.getL0Command();
		
		logger.info("------  Starting XBIP Monitor  ------");
		logger.info("XBIP directory . . . . . . : " + xbipDirectory);
		logger.info("Satellite  . . . . . . . . : " + satelliteIdentifier);
		logger.info("X-band station unit  . . . : " + stationUnitIdentifier);
		logger.info("Transfer history file  . . : " + this.getTransferHistoryFile());
		logger.info("XBIP check interval  . . . : " + this.getCheckInterval());
		logger.info("History truncation interval: " + this.getTruncateInterval());
		logger.info("History retention period . : " + this.getHistoryRetentionDuration());
		logger.info("CADU target directory  . . : " + caduDirectoryPath);
		logger.info("L0 processor command . . . : " + l0ProcessorCommand);
		
		Thread monitorThread = new Thread() {
			
			@Override
			public void run() {
				this.run();
			}
		};
		
		monitorThread.start();
	}
	
	/**
	 * Gets the last copy performance for monitoring purposes
	 * 
	 * @return the last copy performance in MiB/s
	 */
	public Double getLastCopyPerformance() {
		return lastCopyPerformance;
	}

	/**
	 * Records the last copy performance for monitoring purposes
	 * 
	 * @param copyPerformance the copy performance in MiB/s
	 */
	/* package */ synchronized void setLastCopyPerformance(Double copyPerformance) {
		lastCopyPerformance = copyPerformance;
	}

	/**
	 * Check the configured XBIP satellite directory for sessions (without filtering);
	 * note that the passed reference time stamp is ignored, as on the XBIP there is no reliable value to compare it against
	 */
	@Override
	protected List<TransferObject> checkAvailableDownloads(Instant referenceTimeStamp) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkAvailableDownloads({})", referenceTimeStamp);

		List<TransferObject> objectsToTransfer = new ArrayList<>();
		
		if (Files.isDirectory(xbipDirectory) && Files.isReadable(xbipDirectory)) {
			try (DirectoryStream<Path> xbipList = Files.newDirectoryStream(xbipDirectory, sessionFilter)) {
				xbipList.forEach(sessionEntry -> {
					
					String[] sessionEntryParts = sessionEntry.getFileName().toString().split("_");
					
					if (5 != sessionEntryParts.length) {
						logger.warn(String.format(MSG_XBIP_ENTRY_MALFORMED, MSG_ID_XBIP_ENTRY_MALFORMED, sessionEntry.getFileName().toString()));
						return;
					}
						
					// Check availability of DSIB files in each channel directory
					String[] channelEntries = sessionEntry.toFile().list();
					if (0 == channelEntries.length) {
						logger.info(String.format(MSG_SESSION_TRANSFER_INCOMPLETE, MSG_ID_SESSION_TRANSFER_INCOMPLETE,
								sessionEntry.getFileName().toString()));
						return;
					}
					for (String channelEntry: channelEntries) {
						String[] channelEntryParts = channelEntry.split("_");
						if (2 != channelEntryParts.length) {
							logger.warn(String.format(MSG_XBIP_ENTRY_MALFORMED, MSG_ID_XBIP_ENTRY_MALFORMED, channelEntry));
							return;
						}
						String dsibFileName = sessionEntry.getFileName().toString()
								.replace("dat", "ch_" + channelEntryParts[1] + "_DSIB.xml");
						if (logger.isTraceEnabled()) logger.trace("... checking existence of DSIB file " + dsibFileName);
						if (!Files.exists(sessionEntry.resolve(channelEntry).resolve(dsibFileName))) {
							// Session transfer incomplete, skip
							logger.info(String.format(MSG_SESSION_TRANSFER_INCOMPLETE, MSG_ID_SESSION_TRANSFER_INCOMPLETE,
									sessionEntry.getFileName().toString()));
							return;
						}
					}
					
					// Session transfer is complete, create transfer object
					TransferSession transferSession = new TransferSession();
					transferSession.satelliteIdentifier = satelliteIdentifier;
					transferSession.stationUnitIdentifier = stationUnitIdentifier;
					transferSession.sessionIdentifier = sessionEntryParts[2];
					transferSession.sessionPath = sessionEntry;
					objectsToTransfer.add(transferSession);
					
				});
			} catch (IOException e) {
				logger.error(String.format(MSG_XBIP_NOT_READABLE, MSG_ID_XBIP_NOT_READABLE, xbipDirectory.toString(), e.getMessage()));
			}
		} else {
			logger.error(String.format(MSG_XBIP_NOT_READABLE, MSG_ID_XBIP_NOT_READABLE, xbipDirectory.toString(), "Not a readable directory"));
		}
		
		logger.info(String.format(MSG_AVAILABLE_DOWNLOADS_FOUND, MSG_ID_AVAILABLE_DOWNLOADS_FOUND, objectsToTransfer.size()));
		
		return objectsToTransfer;
	}

	/**
	 * Transfer the data in the session data directory to the configured CADU target directory for L0 processing
	 */
	@Override
	protected boolean transferToTargetDir(TransferObject object) {
		if (logger.isTraceEnabled()) logger.trace(">>> transferToTargetDir({})", null == object ? "null" : object.getIdentifier());
		
		if (null == object) {
			logger.error(String.format(MSG_TRANSFER_OBJECT_IS_NULL, MSG_ID_TRANSFER_OBJECT_IS_NULL));
			return false;
		}
		
		if (object instanceof TransferSession) {
			
			TransferSession transferSession = (TransferSession) object;
			
			// Optimistically we assume success (actually: it's an AND condition)
			copySuccess.put(transferSession.getIdentifier(), true);

			List<Thread> copyTasks = new ArrayList<>();
			
			// Determine the correct target directory
			Path sessionDirectory = transferSession.sessionPath.getFileName();
			Path caduDirectory = caduDirectoryPath.resolve(sessionDirectory);
			
			// Check all channel directories
			for (String channel: Arrays.asList("ch_1", "ch_2")) {
				Path sessionChannelDirectory = transferSession.sessionPath.resolve(channel);
				
				// If a channel does not exist, skip it
				if (!Files.exists(sessionChannelDirectory)) {
					if (logger.isTraceEnabled()) logger.trace("... skipping non-existing channel directory " + sessionChannelDirectory);
					continue;
				}
				
				// Create target directory for channel data
				Path caduChannelDirectory = caduDirectory.resolve(channel);
				try {
					Files.createDirectories(caduChannelDirectory);
				} catch (IOException e) {
					logger.error(
							String.format(MSG_CANNOT_CREATE_TARGET_DIR, MSG_ID_CANNOT_CREATE_TARGET_DIR, caduDirectory.toString()));
					return false;
				}
				
				// Copy each file in the session channel directory to the target directory in a separate, parallel thread,
				// recording the transfer performance
				try (DirectoryStream<Path> sessionChannelStream = Files.newDirectoryStream(sessionChannelDirectory)) {
					sessionChannelStream.forEach(sessionChannelFile -> {
						
						// Prepare the copying task
						Thread copyTask = new Thread() {

							@Override
							public void run() {
								try {
									if (logger.isTraceEnabled())
										logger.trace("... Copying file {} to directory {}", sessionChannelFile, caduChannelDirectory);

									Instant copyStart = Instant.now();

									Files.copy(sessionChannelFile, caduChannelDirectory, StandardCopyOption.REPLACE_EXISTING,
											StandardCopyOption.COPY_ATTRIBUTES);

									Duration copyDuration = Duration.between(copyStart, Instant.now());
									Double copyPerformance = sessionChannelFile.toFile().length() / // Bytes
									(copyDuration.toNanos() / 1000000000.0) // seconds (with fraction)
											/ (1024 * 1024); // --> MiB/s
									
									// Record the performance for files of sufficient size (currently 20 MiB) - TODO make threshold configurable
									if (20 * 1024 * 1024 < sessionChannelFile.toFile().length()) {
										setLastCopyPerformance(copyPerformance);
									}
									
									if (logger.isTraceEnabled())
										logger.trace("... Copying of file {} complete, duration {}, speed {} MiB/s",
												sessionChannelFile, copyDuration, copyPerformance);
								} catch (IOException e) {
									logger.error(String.format(MSG_COPY_FILE_FAILED, MSG_ID_COPY_FILE_FAILED,
											sessionChannelFile.toString(), e.getMessage()));
									copySuccess.put(transferSession.getIdentifier(), false);
								}
							}

						};
						copyTasks.add(copyTask);
						
						// Start the copying task asynchronically
						copyTask.start();
					});
				} catch (IOException e) {
					logger.error(String.format(MSG_COPY_FAILED, MSG_ID_COPY_FAILED, sessionChannelDirectory.toString()));
					return false;
				}
			}
			
			// Join all copying subtasks
			if (logger.isTraceEnabled()) logger.trace("... waiting for all subtasks to complete");
			for (Thread copyTask: copyTasks) {
				try {
					copyTask.join();
				} catch (InterruptedException e) {
					logger.error(String.format(MSG_COPY_INTERRUPTED, MSG_ID_COPY_INTERRUPTED, transferSession.sessionPath.toString()));
					return false;
				}
			}
			
			// Check whether any copy action failed
			Boolean myCopySuccess = copySuccess.get(transferSession.getIdentifier());
			copySuccess.remove(transferSession.getIdentifier());
			
			if (logger.isTraceEnabled()) logger.trace("<<< transferToTargetDir: " + (myCopySuccess ? "SUCCESS" : "FAILURE"));
			
			return myCopySuccess;

		} else {
			logger.error(String.format(MSG_INVALID_TRANSFER_OBJECT_TYPE, MSG_ID_INVALID_TRANSFER_OBJECT_TYPE, object.getIdentifier()));
			return false;
		}
	}

	/**
	 * Trigger L0 processing on the given downlink session transfer object (spawns a separate process taking just
	 * the path to the session CADU data as parameter)
	 */
	@Override
	protected boolean triggerFollowOnAction(TransferObject transferObject) {
		if (logger.isTraceEnabled()) logger.trace(">>> triggerFollowOnAction({})",
				null == transferObject ? "null" : transferObject.getIdentifier());

		if (null == transferObject) {
			logger.error(String.format(MSG_TRANSFER_OBJECT_IS_NULL, MSG_ID_TRANSFER_OBJECT_IS_NULL));
			return false;
		}
		
		if (transferObject instanceof TransferSession) {
			TransferSession transferSession = (TransferSession) transferObject;
			
			// Determine the correct target directory
			Path sessionDirectory = transferSession.sessionPath.getFileName();
			Path caduDirectory = caduDirectoryPath.resolve(sessionDirectory);

			// Call external process
			ProcessBuilder processBuilder = new ProcessBuilder();

			String command = config.getL0Command() + " " + caduDirectory.toString();
			processBuilder.command(command.split(" "));
			processBuilder.redirectErrorStream(true);
			processBuilder.redirectOutput(Redirect.INHERIT);
			
			try {
				
				processBuilder.start();
				
			} catch (IOException e) {
				logger.error(String.format(MSG_COMMAND_START_FAILED, MSG_ID_COMMAND_START_FAILED, command, e.getMessage()));
				return false;
			}

		} else {
			logger.error(String.format(MSG_INVALID_TRANSFER_OBJECT_TYPE, MSG_ID_INVALID_TRANSFER_OBJECT_TYPE, transferObject.getIdentifier()));
			return false;
		}

		return true;
	}

}