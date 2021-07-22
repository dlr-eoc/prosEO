/**
 * BaseMonitor.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.basemon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for implementing monitors on pickup points ("interface points" in the ESA Ground Segment Framework architecture).
 * The monitor is intended to run "forever" (i. e. until it is shut down by an interrupt), so it does not terminate on errors
 * during transfer or follow-up actions. It does terminate if it cannot correctly write its transfer history.
 * 
 * @author Dr. Thomas Bassler
 */
public abstract class BaseMonitor {
	
	/** Interval in millliseconds to check for completed subtasks */
	private static final long TASK_WAIT_INTERVAL = 500;

	/** Interval between pickup point checks in milliseconds, default is one second */
	private long checkInterval = 1000;
	
	/** Interval to truncate transfer history file, default is one day */
	private long truncateInterval = 24 * 60 * 60 * 1000;
	
	/** Period to retain transfer history entries for, default is seven days */
	private Duration historyRetentionDuration = Duration.ofDays(7);
	
	/** Time of next planned truncation of transfer history file */
	private Instant nextHistoryTruncation = Instant.EPOCH;
	
	/** File for storing transfer history, default file name "transfer.history" in the current directory */
	private Path transferHistoryFile = Paths.get("transfer.history");
	
	/** Transfer history (set of transfer object identifiers) */
	private Set<String> transferHistory = new HashSet<>();

	/** Reference time stamp for retrieving objects from pickup point */
	private Instant referenceTimeStamp = Instant.EPOCH;
	
	/* Message ID constants */
	private static final int MSG_ID_INTERRUPTED = 5200;
	private static final int MSG_ID_TRANSFER_FAILED = 5201;
	private static final int MSG_ID_FOLLOW_ON_ACTION_FAILED = 5202;
	private static final int MSG_ID_HISTORY_READ_FAILED = 5203;
	private static final int MSG_ID_HISTORY_WRITE_FAILED = 5204;
	private static final int MSG_ID_ABORTING_MONITOR = 5205;
	private static final int MSG_ID_ILLEGAL_HISTORY_ENTRY_FORMAT = 5206;
	private static final int MSG_ID_ILLEGAL_HISTORY_ENTRY_DATE = 5207;
	private static final int MSG_ID_HISTORY_ENTRIES_READ = 5208;
	private static final int MSG_ID_HISTORY_ENTRIES_TRUNCATED = 5209;
	private static final int MSG_ID_TASK_WAIT_INTERRUPTED = 5209;
	
	/* Message string constants */
	private static final String MSG_INTERRUPTED = "(I%d) Interrupt received while waiting for next check of pickup point";
	private static final String MSG_TRANSFER_FAILED = "(E%d) Transfer of object %s failed";
	private static final String MSG_FOLLOW_ON_ACTION_FAILED = "(E%d) Follow-on action for object %s failed";
	private static final String MSG_HISTORY_READ_FAILED = "(E%d) Failed to read transfer history file %s (cause: %s)";
	private static final String MSG_HISTORY_WRITE_FAILED = "(E%d) Failed to write transfer history file %s (cause: %s)";
	private static final String MSG_ABORTING_MONITOR = "(E%d) Aborting monitor due to IOException (cause: %s)";
	private static final String MSG_ILLEGAL_HISTORY_ENTRY_FORMAT = "(E%d) Transfer history entry '%s' has illegal format";
	private static final String MSG_ILLEGAL_HISTORY_ENTRY_DATE = "(E%d) Transfer history entry date '%s' has illegal format";
	private static final String MSG_HISTORY_ENTRIES_READ = "(I%d) %d history entries read from history file %s, reference time for next pickup point lookup is %s";
	private static final String MSG_HISTORY_ENTRIES_TRUNCATED = "(I%d) %d entries truncated from transfer history file %s";
	private static final String MSG_TASK_WAIT_INTERRUPTED = "(I%d) Wait for task completion interrupted, continuing wait";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(BaseMonitor.class);
	
	/**
	 * Gets the interval between pickup point checks
	 * 
	 * @return the checkInterval the interval in milliseconds
	 */
	public long getCheckInterval() {
		return checkInterval;
	}

	/**
	 * Sets the interval between pickup point checks
	 * 
	 * @param checkInterval the check interval to set in milliseconds
	 */
	public void setCheckInterval(long checkInterval) {
		this.checkInterval = checkInterval;
	}

	/**
	 * Gets the file path of the transfer history file
	 * 
	 * @return the path to the transfer history file
	 */
	public Path getTransferHistoryFile() {
		return transferHistoryFile;
	}

	/**
	 * Gets the file path of the transfer history file
	 * 
	 * @param transferHistoryFile the path to the transfer history file to set
	 */
	public void setTransferHistoryFile(Path transferHistoryFile) {
		this.transferHistoryFile = transferHistoryFile;
	}

	/**
	 * Gets the interval to truncate transfer history file
	 * 
	 * @return the truncation interval
	 */
	public long getTruncateInterval() {
		return truncateInterval;
	}

	/**
	 * Sets the interval to truncate transfer history file
	 * 
	 * @param truncateInterval the truncation interval to set
	 */
	public void setTruncateInterval(long truncateInterval) {
		this.truncateInterval = truncateInterval;
	}

	/**
	 * Gets the period to retain transfer history entries for
	 * 
	 * @return the history retention duration
	 */
	public Duration getHistoryRetentionDuration() {
		return historyRetentionDuration;
	}

	/**
	 * Sets the period to retain transfer history entries for
	 * 
	 * @param historyRetentionDuration the history retention duration to set
	 */
	public void setHistoryRetentionDuration(Duration historyRetentionDuration) {
		this.historyRetentionDuration = historyRetentionDuration;
	}

	/**
	 * Read identifiers of transfer objects from history file
	 * 
	 * @return a set of transfer object identifiers
	 * @throws IOException if an I/O error occurs opening or reading the file
	 */
	private Set<String> readTransferHistory() throws IOException {
		if (logger.isTraceEnabled()) logger.trace(">>> readTransferHistory()");
		
		Set<String> transferHistory = new HashSet<>();
		
		// Read transfer history from file (if the file exists)
		if (Files.exists(transferHistoryFile)) {
			
			try (BufferedReader historyFile = Files.newBufferedReader(transferHistoryFile)) {
				
				while (historyFile.ready()) {
					
					// Analyse entry: Consists of <check date>;<transfer object id>
					String[] historyEntryParts = historyFile.readLine().split(";", 2);
					if (2 != historyEntryParts.length) {
						logger.error(String.format(MSG_ILLEGAL_HISTORY_ENTRY_FORMAT, MSG_ID_ILLEGAL_HISTORY_ENTRY_FORMAT, historyEntryParts.toString()));
						throw new IllegalArgumentException();
					}
					
					// Highest entry date becomes reference time stamp for next pickup point lookup
					try {
						Instant entryDate = Instant.parse(historyEntryParts[0]);
						if (referenceTimeStamp.isBefore(entryDate)) {
							referenceTimeStamp = entryDate;
						}
					} catch (DateTimeParseException e) {
						logger.error(String.format(MSG_ILLEGAL_HISTORY_ENTRY_DATE, MSG_ID_ILLEGAL_HISTORY_ENTRY_DATE, historyEntryParts[0]));
						throw new IllegalArgumentException();
					}
					
					// Add second part of entry to transfer history
					transferHistory.add(historyEntryParts[1]);
					
				}
				
			} catch (IOException e) {
				String message = String.format(MSG_HISTORY_READ_FAILED, MSG_ID_HISTORY_READ_FAILED, transferHistoryFile.toString(),
						e.getMessage());
				logger.error(message);
				throw new IOException(message, e);
			} catch (IllegalArgumentException e) {
				// Do nothing: Already logged, and entry can be ignored; might result in double transfer of object
			}
			
		}
		
		logger.info(String.format(MSG_HISTORY_ENTRIES_READ, MSG_ID_HISTORY_ENTRIES_READ,
				transferHistory.size(), transferHistoryFile, referenceTimeStamp.toString()));
		
		return transferHistory;
	}

	/**
	 * Adds the identifier of a transfer object to history file
	 * 
	 * @param transferObjectIdentifier a transfer object identifier
	 * @param checkTimeStamp the timestamp of the pickup point check
	 * 
	 * @throws IOException if an I/O error occurs opening or writing the file
	 */
	private void writeTransferHistory(String transferObjectIdentifier, Instant checkTimeStamp) throws IOException {
		if (logger.isTraceEnabled()) logger.trace(">>> writeTransferHistory({})", transferObjectIdentifier);
		
		// Prepare file creation, if necessary
		OpenOption openOption =  StandardOpenOption.APPEND;
		if (!Files.exists(transferHistoryFile)) {
			openOption = StandardOpenOption.CREATE;
			if (null != transferHistoryFile.getParent()) {
				Files.createDirectories(transferHistoryFile.getParent());
			}
		}
		
		// Write transfer object identifier to file
		try  (BufferedWriter historyFile = Files.newBufferedWriter(transferHistoryFile, openOption)) {
			historyFile.write(checkTimeStamp.toString());
			historyFile.write(';');
			historyFile.write(transferObjectIdentifier);
			historyFile.newLine();
		} catch (IOException e) {
			String message = String.format(MSG_HISTORY_WRITE_FAILED, MSG_ID_HISTORY_WRITE_FAILED,
					transferHistoryFile.toString(), e.getMessage());
			logger.error(message);
			throw new IOException(message, e);
		}
	}
	
	/**
	 * Remove entries older than the configured truncation period from the transfer history to avoid its becoming too long
	 * 
	 * @throws IOException if an I/O error occurs opening, reading or writing the file
	 */
	private void truncateTransferHistory() throws IOException {
		if (logger.isTraceEnabled()) logger.trace(">>> truncateTransferHistory()");
		
		if (Files.exists(transferHistoryFile)) {
			
			// Move history file to temporary file
			Path tempFilePath = Paths.get(transferHistoryFile.toString() + ".tmp");
			Files.move(transferHistoryFile, tempFilePath);
			
			// Copy entries newer than history retention duration to new history file
			Instant truncationTime = Instant.now().minus(historyRetentionDuration);
			long truncatedCount = 0;
			
			try (BufferedReader oldHistoryFile = Files.newBufferedReader(tempFilePath);
					BufferedWriter newHistoryFile = Files.newBufferedWriter(transferHistoryFile)) {
				
				while (oldHistoryFile.ready()) {
					
					String oldHistoryEntry = oldHistoryFile.readLine();
					
					// Analyse entry: Consists of <check date>;<transfer object id>
					String[] historyEntryParts = oldHistoryEntry.split(";", 2);
					if (2 != historyEntryParts.length) {
						logger.error(String.format(MSG_ILLEGAL_HISTORY_ENTRY_FORMAT, MSG_ID_ILLEGAL_HISTORY_ENTRY_FORMAT, historyEntryParts.toString()));
						throw new IllegalArgumentException();
					}
					
					// Copy entries newer than truncation time
					try {
						Instant entryDate = Instant.parse(historyEntryParts[0]);
						if (truncationTime.isBefore(entryDate)) {
							newHistoryFile.write(oldHistoryEntry);
							newHistoryFile.newLine();
						} else {
							++truncatedCount;
						}
					} catch (DateTimeParseException e) {
						logger.error(String.format(MSG_ILLEGAL_HISTORY_ENTRY_DATE, MSG_ID_ILLEGAL_HISTORY_ENTRY_DATE, historyEntryParts[0]));
						throw new IllegalArgumentException();
					}
					
				}
				
			}
			
			Files.delete(tempFilePath);
			
			logger.info(String.format(MSG_HISTORY_ENTRIES_TRUNCATED, MSG_ID_HISTORY_ENTRIES_TRUNCATED,
					truncatedCount, transferHistoryFile.toString()));
			
		}
		
		nextHistoryTruncation = Instant.now().plus(historyRetentionDuration);
	}

	/**
	 * Check the pickup point for available objects (unfiltered)
	 * 
	 * @param referenceTimeStamp the reference timestamp to apply for pickup point lookups
	 * @return a list of available transfer objects
	 */
	protected abstract List<TransferObject> checkAvailableDownloads(Instant referenceTimeStamp);
	
	/**
	 * Check the given list of objects against the transfer history and return a new list containing only the objects
	 * not yet transferred (based on the object identifiers)
	 * 
	 * @param transferableObjects a list of objects available on the pickup point for transfer
	 * @return a new (possibly empty) list of objects, which have not yet been transferred
	 */
	private List<TransferObject> filterTransferableObjects(List<TransferObject> transferableObjects) {
		if (logger.isTraceEnabled()) logger.trace(">>> filterTransferableObjects(TransferObject[{}])", 
				null == transferableObjects ? "null" : transferableObjects.size());

		List<TransferObject> objectsToTransfer = new ArrayList<>();
		
		// Apply filter
		if (logger.isTraceEnabled()) logger.trace("... filtering against transfer history of size " + transferHistory.size());
		for (TransferObject objectToTransfer: transferableObjects) {
			if (!transferHistory.contains(objectToTransfer.getIdentifier())) {
				objectsToTransfer.add(objectToTransfer);
			}
		}
		
		return objectsToTransfer;
	}
	
	/**
	 * Download the given transfer object from the pickup point and copy them to the configured target directory
	 * 
	 * @param object the transfer object to download
	 * @return true, if the download was successful, false otherwise
	 */
	protected abstract boolean transferToTargetDir(TransferObject object);
	
	/**
	 * Add the given object to the transfer history
	 * 
	 * @param transferredObject the transferred object
	 * @param checkTimeStamp the timestamp of the last pickup point lookup to record for the transferred object
	 * @throws IOException if an I/O error occurs opening or writing the transfer history file
	 */
	synchronized private void recordTransfer(TransferObject transferredObject, Instant checkTimeStamp) throws IOException {
		if (logger.isTraceEnabled()) logger.trace(">>> recordTransfer({})",
				null == transferredObject ? "null" : transferredObject.getIdentifier());
		
		// Add identifier of transferred object to history
		String transferredObjectIdentifier = transferredObject.getIdentifier();
		
		transferHistory.add(transferredObjectIdentifier);
		
		writeTransferHistory(transferredObjectIdentifier, checkTimeStamp);
		
	}

	/**
	 * Trigger any necessary follow-on action on the transfer object (processing, order generation etc.)
	 * 
	 * @param object the transfer object to start the action on
	 * @return true, if starting the action succeeded (not necessarily the action itself), false otherwise
	 */
	protected abstract boolean triggerFollowOnAction(TransferObject object);
	
	/**
	 * Monitoring loop:
	 * <ol>
	 *   <li>Check data availability on pickup point</li>
	 *   <li>Filter objects not yet processed from transfer history</li>
	 *   <li>For all objects not yet processed:
	 *     <ol>
	 *       <li>Transfer object to local target directory</li>
	 *       <li>Record transfer in history</li>
	 *       <li>Trigger follow-on action</li>
	 *     </ol>
	 *   </li>
	 * </ol>
	 * 
	 * @param maxCycles maximum number of loop cycles (if null, run forever)
	 */
	public void run(Integer maxCycles) {
		if (logger.isTraceEnabled()) logger.trace(">>> run()");
		
		// Initialize transfer history
		try {
			transferHistory = readTransferHistory();
		} catch (IOException e) {
			logger.error(String.format(MSG_ABORTING_MONITOR, MSG_ID_ABORTING_MONITOR, e.getMessage()));
			return;
		}

		int i = 0;
		while (null == maxCycles || i < maxCycles) {
			
			if (logger.isTraceEnabled()) logger.trace("... entering check loop # " + i);
			
			// Truncate transfer history, if scheduled time has arrived
			if (Instant.now().isAfter(nextHistoryTruncation)) {
				try {
					truncateTransferHistory();
				} catch (IOException e) {
					logger.error(String.format(MSG_ABORTING_MONITOR, MSG_ID_ABORTING_MONITOR, e.getMessage()));
					return;
				}
			}
			
			// Check data availability on pickup point
			Instant checkTimeStamp = Instant.now();
			List<TransferObject> transferableObjects = checkAvailableDownloads(referenceTimeStamp);
			referenceTimeStamp = checkTimeStamp;
			
			// Filter objects not yet processed from transfer history
			List<TransferObject> objectsToTransfer = filterTransferableObjects(transferableObjects);
			List<Thread> transferTasks = new ArrayList<>();
			
			// Transfer all objects not yet processed
			for (TransferObject objectToTransfer: objectsToTransfer) {
				
				// Setup parallel transfers
				Thread transferTask = new Thread() {

					@Override
					public void run() {
						// Transfer object to local target directory
						if (transferToTargetDir(objectToTransfer)) {
							
							// Record transfer in history
							try {
								recordTransfer(objectToTransfer, checkTimeStamp);
							} catch (IOException e) {
								logger.error(String.format(MSG_ABORTING_MONITOR, MSG_ID_ABORTING_MONITOR, e.getMessage()));
								return;
							}
							
							// Trigger follow-on action
							if (!triggerFollowOnAction(objectToTransfer)) {
								logger.error(String.format(MSG_FOLLOW_ON_ACTION_FAILED, MSG_ID_FOLLOW_ON_ACTION_FAILED, objectToTransfer.getIdentifier()));
							}
							
						} else {
							logger.error(String.format(MSG_TRANSFER_FAILED, MSG_ID_TRANSFER_FAILED, objectToTransfer.getIdentifier()));
						}
					}
				};
				transferTasks.add(transferTask);
				transferTask.start();
				
			}
			
			// Wait for all tasks to complete
			if (logger.isTraceEnabled()) logger.trace("... waiting for all subtasks to complete");
			for (Thread transferTask: transferTasks) {
				while (transferTask.isAlive()) {
					try {
						Thread.sleep(TASK_WAIT_INTERVAL);
					} catch (InterruptedException e) {
						logger.warn(String.format(MSG_TASK_WAIT_INTERRUPTED, MSG_ID_TASK_WAIT_INTERRUPTED));
					}
				}
			}
						
			// Wait for next check interval
			try {
				if (logger.isTraceEnabled()) logger.trace("... sleeping for {} seconds", checkInterval / 1000.0);
				Thread.sleep(checkInterval);
			} catch (InterruptedException e) {
				// On interrupt leave monitoring loop
				logger.info(String.format(MSG_INTERRUPTED, MSG_ID_INTERRUPTED));
				break;
			}
			
			// Count cycles only if maximum of cycles is set
			if (logger.isTraceEnabled()) logger.trace("... leaving check loop # " + i);
			if (null != maxCycles) ++i;
		}
	}
	
	/**
	 * Run forever (convenience function for calling {@link #run(Integer)} with a <code>null</code> argument)
	 */
	public void run() {
		run(null);
	}

}
