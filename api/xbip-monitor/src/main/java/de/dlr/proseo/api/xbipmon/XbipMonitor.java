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
import java.nio.file.attribute.BasicFileAttributeView;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import de.dlr.proseo.api.basemon.BaseMonitor;
import de.dlr.proseo.api.basemon.TransferObject;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ApiMonitorMessage;

/**
 * Monitor for X-band Interface Points
 * 
 * X-band Interface Points are WebDAV volumes, so from within the Monitor they just look like plain POSIX volumes.
 * 
 * @author Dr. Thomas Bassler
 */
@Component
@Scope("singleton")
public class XbipMonitor extends BaseMonitor {
	
	/** The path to the XBIP directory (mounted WebDAV volume) */
	private Path xbipDirectory;
	
	/** The satellite identifier (e. g. "S1B") */
	private String satelliteIdentifier;
	
	/** Filter for directory */
	private static String SESSION_FILTER_FORMAT = "DCS_*_*_dat";
	
	/** The path to the target CADU directory (for L0 processing) */
	private Path caduDirectoryPath;
	
	/** The L0 processor command (a single command taking the CADU directory as argument) */
	private String l0ProcessorCommand;

	/** Maximum number of parallel file download threads within a download session (default 1 = no parallel downloads) */
	private int maxFileDownloadThreads = 1;
	
	/** Interval in millliseconds to check for completed file downloads (default 500 ms) */
	private int fileWaitInterval = 500;
	
	/** Maximum number of wait cycles for file download completion checks (default 3600 = total timeout of 30 min) */
	private int maxFileWaitCycles = 3600;
	
	/** The last copy performance in MiB/s (static, because it may be read and written from different threads) */
	private static Double lastCopyPerformance = 0.0;

	/** Indicator for parallel copying processes */
	/* package */ Map<String, Boolean> copySuccess = new ConcurrentHashMap<>();
	
	/** Total data size per session */
	private Map<String, Long> sessionDataSizes = new ConcurrentHashMap<>();

	/** The XBIP Monitor configuration to use */
	@Autowired
	private XbipMonitorConfiguration config;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(XbipMonitor.class);
	
	/**
	 * Class describing a download session
	 */
	public static class TransferSession implements TransferObject {
		
		/** The satellite identifier */
		private String satelliteIdentifier;
		
		/** The XBIP session identifier */
		private String sessionIdentifier;
		
		/** The path to the session data on the XBIP */
		private Path sessionPath;
		
		/** The DSIB files for the session */
		private List<Path> dsibFilePaths = new ArrayList<>();
		
		/** Reference time for this session */
		private Instant referenceTime;

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
		 * Gets the session reference time
		 * 
		 * @return the reference time
		 */
		@Override
		public Instant getReferenceTime() {
			return referenceTime;
		}

		/**
		 * Sets the session reference time
		 * 
		 * @param referenceTime the reference time to set
		 */
		public void setReferenceTime(Instant referenceTime) {
			this.referenceTime = referenceTime;
		}

		/**
		 * Gets the combined transfer object identifier: satellite|station unit|session ID
		 * 
		 * @see de.dlr.proseo.api.basemon.TransferObject#getIdentifier()
		 */
		@Override
		public String getIdentifier() {
			return String.format("%s_%s", satelliteIdentifier, sessionIdentifier);
		}
		
		/**
		 * Gets the DSIB file for a given channel
		 * 
		 * @param channel the channel identifier ("ch_x")
		 * @return the path to the DSIB file belonging to the given channel, or null, if no suitable file was found
		 */
		public Path getDsibFileForChannel(String channel) {
			String[] channelParts = channel.split("_");
			if (2 == channelParts.length) {
				for (Path dsibFilePath: dsibFilePaths) {
					if (dsibFilePath.toString().endsWith("ch" + channelParts[1] + "_DSIB.xml")) {
						return dsibFilePath;
					}
				}
			}
			return null;
		}
		
	}
	
	/**
	 * Class describing a download channel
	 */
	public static class DataSessionInformationBlock {
		public String session_id;
		public String time_start;
		public String time_stop;
		public String time_created;
		public String time_finished;
		public Long data_size;
		public List<String> dsdb_list;
	}
	
	/**
	 * Initialize global parameters
	 */
	@PostConstruct
	private void init() {
		xbipDirectory = Paths.get(config.getXbipDirectoryPath());
		satelliteIdentifier = config.getXbipSatellite();
		xbipDirectory = xbipDirectory.resolve(satelliteIdentifier);
		
		this.setTransferHistoryFile(Paths.get(config.getXbipHistoryPath()));
		this.setCheckInterval(config.getXbipCheckInterval());
		this.setTruncateInterval(config.getXbipTruncateInterval());
		this.setHistoryRetentionDuration(Duration.ofMillis(config.getXbipHistoryRetention()));
		
		// Multi-threading control
		this.setMaxDownloadThreads(config.getMaxDownloadThreads());
		this.setTaskWaitInterval(config.getTaskWaitInterval());
		this.setMaxWaitCycles(config.getMaxWaitCycles());
		this.setMaxFileDownloadThreads(config.getMaxFileDownloadThreads());
		this.setFileWaitInterval(config.getFileWaitInterval());
		this.setMaxFileWaitCycles(config.getMaxFileWaitCycles());
		
		caduDirectoryPath = Paths.get(config.getL0CaduDirectoryPath());
		l0ProcessorCommand = config.getL0Command();
		
		logger.log(ApiMonitorMessage.XBIP_START_MESSAGE, xbipDirectory, satelliteIdentifier, getTransferHistoryFile(),
				getCheckInterval(), getTruncateInterval(), getHistoryRetentionDuration(), caduDirectoryPath,
				l0ProcessorCommand, getMaxDownloadThreads(), getTaskWaitInterval(), getMaxWaitCycles(),
				getMaxFileDownloadThreads(), getFileWaitInterval(), getMaxFileWaitCycles());
	}
	
	/**
	 * Gets the maximum number of parallel file download threads within a download session
	 * 
	 * @return the maximum number of parallel file download threads
	 */
	public int getMaxFileDownloadThreads() {
		return maxFileDownloadThreads;
	}

	/**
	 * Sets the maximum number of parallel file download threads within a download session
	 * 
	 * @param maxFileDownloadThreads the maximum number of parallel file download threads to set
	 */
	public void setMaxFileDownloadThreads(int maxFileDownloadThreads) {
		this.maxFileDownloadThreads = maxFileDownloadThreads;
	}

	/**
	 * Gets the interval to check for completed file downloads
	 * 
	 * @return the check interval in millliseconds
	 */
	public int getFileWaitInterval() {
		return fileWaitInterval;
	}

	/**
	 * Sets the interval to check for completed file downloads
	 * 
	 * @param fileWaitInterval the check interval in millliseconds to set
	 */
	public void setFileWaitInterval(int fileWaitInterval) {
		this.fileWaitInterval = fileWaitInterval;
	}

	/**
	 * Gets the maximum number of wait cycles for file download completion checks
	 * 
	 * @return the maximum number of wait cycles
	 */
	public int getMaxFileWaitCycles() {
		return maxFileWaitCycles;
	}

	/**
	 * Sets the maximum number of wait cycles for file download completion checks
	 * 
	 * @param maxFileWaitCycles the maximum number of wait cycles to set
	 */
	public void setMaxFileWaitCycles(int maxFileWaitCycles) {
		this.maxFileWaitCycles = maxFileWaitCycles;
	}

	/**
	 * Gets the last copy performance for monitoring purposes
	 * 
	 * @return the last copy performance in MiB/s
	 */
	synchronized public Double getLastCopyPerformance() {
		return lastCopyPerformance;
	}

	/**
	 * Records the last copy performance for monitoring purposes
	 * 
	 * @param copyPerformance the copy performance in MiB/s
	 */
	synchronized /* package */ void setLastCopyPerformance(Double copyPerformance) {
		lastCopyPerformance = copyPerformance;
	}
	
	/**
	 * Thread-safe method to calculate total session download size
	 * 
	 * @param caduSize the size of the CADU chunk to add to the session download size
	 */
	synchronized private void addToSessionDataSize(String sessionId, long caduSize) {
		if (null == sessionDataSizes.get(sessionId)) {
			sessionDataSizes.put(sessionId, caduSize);
		} else {
			sessionDataSizes.put(sessionId, sessionDataSizes.get(sessionId) + caduSize);
		}
	}
	
	/**
	 * Check the configured XBIP satellite directory for sessions (without filtering);
	 * note that the passed reference time stamp is ignored, as on the XBIP there is no reliable value to compare it against
	 */
	@Override
	protected TransferControl checkAvailableDownloads(Instant referenceTimeStamp) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkAvailableDownloads({})", referenceTimeStamp);

		TransferControl transferControl = new TransferControl();
		transferControl.referenceTime = Instant.now();
		
		if (Files.isDirectory(xbipDirectory) && Files.isReadable(xbipDirectory)) {
			
			if (logger.isTraceEnabled()) logger.trace("... checking directory {} with session filter {}", xbipDirectory, SESSION_FILTER_FORMAT);
			
			try (DirectoryStream<Path> xbipList = Files.newDirectoryStream(xbipDirectory, SESSION_FILTER_FORMAT)) {
				
				if (logger.isTraceEnabled()) logger.trace("... xbipList created " + xbipList);
				
				xbipList.forEach(sessionEntry -> {

					if (logger.isTraceEnabled()) logger.trace("... checking sessionEntry " + sessionEntry.getFileName());
					
					String[] sessionEntryParts = sessionEntry.getFileName().toString().split("_");
					
					if (5 != sessionEntryParts.length) {
						logger.log(ApiMonitorMessage.XBIP_ENTRY_MALFORMED, sessionEntry.getFileName().toString());
						return;
					}
						
					// Check availability of DSIB files in each channel directory
					List<Path> dsibFilePaths = new ArrayList<>();
					
					String[] channelEntries = sessionEntry.toFile().list();
					Instant latestDsibTimestamp = Instant.MIN;
					if (null == channelEntries) {
						logger.log(ApiMonitorMessage.SKIPPING_SESSION_DIRECTORY, sessionEntry.getFileName().toString());
						return;
					}
					if (0 == channelEntries.length) {
						logger.log(ApiMonitorMessage.SESSION_TRANSFER_INCOMPLETE, sessionEntry.getFileName().toString());
						return;
					}
					for (String channelEntry: channelEntries) {

						if (logger.isTraceEnabled()) logger.trace("... checking channelEntry " + channelEntry);
						
						String[] channelEntryParts = channelEntry.split("_");
						if (2 != channelEntryParts.length) {
							logger.log(ApiMonitorMessage.XBIP_ENTRY_MALFORMED, channelEntry);
							return;
						}
						String dsibFileName = sessionEntry.getFileName().toString()
								.replace("dat", "ch" + channelEntryParts[1] + "_DSIB.xml");
						Path dsibFilePath = sessionEntry.resolve(channelEntry).resolve(dsibFileName);
						if (logger.isTraceEnabled()) logger.trace("... checking existence of DSIB file " + dsibFilePath);
						if (!Files.exists(dsibFilePath)) {
							// Session transfer incomplete, skip
							logger.log(ApiMonitorMessage.SESSION_TRANSFER_INCOMPLETE, sessionEntry.getFileName().toString());
							return;
						}
						
						// Check DSIB file timestamp
						try {
							Instant dsibTimestamp = Files.getFileAttributeView(dsibFilePath, BasicFileAttributeView.class)
									.readAttributes().lastModifiedTime().toInstant();
							if (latestDsibTimestamp.isBefore(dsibTimestamp)) {
								latestDsibTimestamp = dsibTimestamp;
							}
						} catch (IOException e) {
							logger.log(ApiMonitorMessage.CANNOT_READ_DSIB_FILE, dsibFilePath, e.toString());
							return;
						}
						
						dsibFilePaths.add(dsibFilePath);
					}
					
					// Check configured retrieval delay
					if (Instant.now().isBefore(latestDsibTimestamp.plusMillis(config.getXbipRetrievalDelay()))) {
						logger.log(ApiMonitorMessage.SKIPPING_SESSION_FOR_DELAY, sessionEntry.getFileName().toString());
						return;
					}

					if (logger.isTraceEnabled()) logger.trace("... downloadable session found!");
					
					// Session transfer is complete, create transfer object
					TransferSession transferSession = new TransferSession();
					transferSession.satelliteIdentifier = satelliteIdentifier;
					transferSession.sessionIdentifier = sessionEntryParts[3];
					transferSession.sessionPath = sessionEntry;
					transferSession.dsibFilePaths = dsibFilePaths;
					transferSession.referenceTime = Instant.now();
					transferControl.transferObjects.add(transferSession);
					
				});
			} catch (IOException e) {
				logger.log(ApiMonitorMessage.XBIP_NOT_READABLE, xbipDirectory.toString(), e.getMessage());
			}
		} else {
			logger.log(ApiMonitorMessage.XBIP_NOT_READABLE, xbipDirectory.toString(), "Not a readable directory");
		}
		
		logger.log(ApiMonitorMessage.AVAILABLE_DOWNLOADS_FOUND, transferControl.transferObjects.size());
		
		return transferControl;
	}

	/**
	 * Transfer the data in the session data directory to the configured CADU target directory for L0 processing
	 */
	@Override
	protected boolean transferToTargetDir(TransferObject object) {
		if (logger.isTraceEnabled()) logger.trace(">>> transferToTargetDir({})", null == object ? "null" : object.getIdentifier());
		
		if (null == object) {
			logger.log(ApiMonitorMessage.TRANSFER_OBJECT_IS_NULL);
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
			long expectedSessionDataSize = 0L;
			sessionDataSizes.put(transferSession.getIdentifier(), 0L);
			
			// Check all channel directories
			Semaphore semaphore = new Semaphore(maxFileDownloadThreads);
			
			for (String channel: Arrays.asList("ch_1", "ch_2")) {
				Path sessionChannelDirectory = transferSession.sessionPath.resolve(channel);
				
				// If a channel does not exist, skip it
				if (!Files.exists(sessionChannelDirectory)) {
					if (logger.isTraceEnabled()) logger.trace("... skipping non-existing channel directory " + sessionChannelDirectory);
					continue;
				}
				
				// Parse DSIB file
				Path dsibFilePath = transferSession.getDsibFileForChannel(channel);
				DataSessionInformationBlock dsib = null;
				try {
					dsib = (new XmlMapper()).readValue(dsibFilePath.toFile(), DataSessionInformationBlock.class);
				} catch (IOException e) {
					logger.log(ApiMonitorMessage.CANNOT_READ_DSIB_FILE, dsibFilePath.toString(), e.getMessage());
					return false;
				}
				if (logger.isDebugEnabled())
					logger.debug("DSIB: data size: {}, # of CADU files: {}", dsib.data_size, dsib.dsdb_list.size());
				expectedSessionDataSize += dsib.data_size;
				
				// Create target directory for channel data
				Path caduChannelDirectory = caduDirectory.resolve(channel);
				try {
					Files.createDirectories(caduChannelDirectory);
				} catch (IOException e) {
					logger.log(ApiMonitorMessage.CANNOT_CREATE_TARGET_DIR, caduDirectory.toString());
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
								// Check whether parallel execution is allowed
								try {
									semaphore.acquire();
									if (logger.isDebugEnabled())
										logger.debug("... file download semaphore acquired, {} permits remaining",
												semaphore.availablePermits());
								} catch (InterruptedException e) {
									logger.log(ApiMonitorMessage.ABORTING_TASK, e.toString());
									return;
								}
								
								try {
									if (logger.isTraceEnabled())
										logger.trace("... Copying file {} to directory {}", sessionChannelFile, caduChannelDirectory);

									Instant copyStart = Instant.now();

									Files.copy(
											sessionChannelFile,
											caduChannelDirectory.resolve(sessionChannelFile.getFileName()),
											StandardCopyOption.REPLACE_EXISTING,
											StandardCopyOption.COPY_ATTRIBUTES);

									Duration copyDuration = Duration.between(copyStart, Instant.now());
									Double copyPerformance = sessionChannelFile.toFile().length() / // Bytes
											(copyDuration.toNanos() / 1000000000.0) // seconds (with fraction)
											/ (1024 * 1024); // --> MiB/s
									
									// Record the performance for files of sufficient size
									if (config.getXbipPerformanceMinSize() < sessionChannelFile.toFile().length()) {
										setLastCopyPerformance(copyPerformance);
									}
									
									if (sessionChannelFile.toString().toLowerCase().endsWith("raw")) {
										// Calculate total download size
										addToSessionDataSize(transferSession.getIdentifier(), sessionChannelFile.toFile().length());
									}
									if (logger.isTraceEnabled())
										logger.trace("... Copying of file {} complete, duration {}, speed {} MiB/s",
												sessionChannelFile, copyDuration, copyPerformance);
								} catch (IOException e) {
									logger.log(ApiMonitorMessage.COPY_FILE_FAILED, sessionChannelFile.toString(), e.getMessage());
									copySuccess.put(transferSession.getIdentifier(), false);
								}

								// Release parallel thread
								semaphore.release();
								if (logger.isDebugEnabled())
									logger.debug("... file download semaphore released, {} permits now available",
											semaphore.availablePermits());
							}

						};
						copyTasks.add(copyTask);
						
						// Start the copying task asynchronically
						copyTask.start();
					});
					
				} catch (IOException e) {
					logger.log(ApiMonitorMessage.COPY_FAILED, sessionChannelDirectory.toString());
					return false;
				}
			}
			
			// Wait for all copying subtasks
			if (logger.isTraceEnabled()) logger.trace("... waiting for all subtasks to complete");
			for (Thread copyTask: copyTasks) {
				int k = 0;
				while (copyTask.isAlive() && k < maxFileWaitCycles) {
					try {
						Thread.sleep(fileWaitInterval);
					} catch (InterruptedException e) {
						logger.log(ApiMonitorMessage.COPY_INTERRUPTED, transferSession.sessionPath.toString());
						return false;
					}
					++k;
				}
				if (k == maxFileWaitCycles) {
					// Timeout reached --> kill download and report error
					copyTask.interrupt();
					logger.log(ApiMonitorMessage.COPY_TIMEOUT, (maxFileWaitCycles * fileWaitInterval) / 1000,
							transferSession.sessionPath.toString());
				}
			}
			
			// Check the total session data size
			if (expectedSessionDataSize != sessionDataSizes.get(transferSession.getIdentifier())) {
				logger.log(ApiMonitorMessage.DATA_SIZE_MISMATCH, transferSession.sessionPath.toString(), expectedSessionDataSize,
						sessionDataSizes.get(transferSession.getIdentifier()));
				copySuccess.put(transferSession.getIdentifier(), false);
			} else {
				if (logger.isTraceEnabled()) logger.trace("... total session data size is as expected: " + expectedSessionDataSize);
			}
			sessionDataSizes.remove(transferSession.getIdentifier());
			
			// Check whether any copy action failed
			Boolean myCopySuccess = copySuccess.get(transferSession.getIdentifier());
			copySuccess.remove(transferSession.getIdentifier());
			
			logger.log(ApiMonitorMessage.SESSION_TRANSFER_COMPLETED, transferSession.getIdentifier(),
					(myCopySuccess ? "SUCCESS" : "FAILURE"));
			
			return myCopySuccess;

		} else {
			logger.log(ApiMonitorMessage.INVALID_TRANSFER_OBJECT_TYPE, object.getIdentifier());
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
			logger.log(ApiMonitorMessage.TRANSFER_OBJECT_IS_NULL);
			return false;
		}
		
		if (transferObject instanceof TransferSession) {
			TransferSession transferSession = (TransferSession) transferObject;
			
			// Determine the correct target directory
			Path sessionDirectory = transferSession.sessionPath.getFileName();
			Path caduDirectory = caduDirectoryPath.resolve(sessionDirectory);

			// Parse DSIB file of channel 1 for time stamps
			Path dsibFilePath = transferSession.getDsibFileForChannel("ch_1");
			DataSessionInformationBlock dsib = null;
			try {
				dsib = (new XmlMapper()).readValue(dsibFilePath.toFile(), DataSessionInformationBlock.class);
			} catch (IOException e) {
				logger.log(ApiMonitorMessage.CANNOT_READ_DSIB_FILE, dsibFilePath.toString(), e.getMessage());
				return false;
			}

			// Call external process
			ProcessBuilder processBuilder = new ProcessBuilder();

			String command = config.getL0Command() + " " + caduDirectory.toString()
				+ " " + dsib.time_start.replace("Z", "") + " " + dsib.time_finished.replace("Z", "");
			processBuilder.command(command.split(" "));
			processBuilder.redirectErrorStream(true);
			processBuilder.redirectOutput(Redirect.DISCARD);
			
			try {
				
				processBuilder.start();
				
				logger.log(ApiMonitorMessage.FOLLOW_ON_ACTION_STARTED, transferSession.getIdentifier(), command);
				
			} catch (IOException e) {
				logger.log(ApiMonitorMessage.COMMAND_START_FAILED, command, e.getMessage());
				return false;
			}

		} else {
			logger.log(ApiMonitorMessage.INVALID_TRANSFER_OBJECT_TYPE, transferObject.getIdentifier());
			return false;
		}

		return true;
	}

}