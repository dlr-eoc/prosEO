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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ApiMonitorMessage;

/**
 * Base class for implementing monitors on pickup points ("interface points" in the ESA Ground Segment Framework architecture).
 * The monitor is intended to run "forever" (i. e. until it is shut down by an interrupt), so it does not terminate on errors
 * during transfer or follow-up actions. It does terminate if it cannot correctly write its transfer history.
 * 
 * @author Dr. Thomas Bassler
 */
public abstract class BaseMonitor extends Thread {
	
	/** Interval between pickup point checks in milliseconds, default is one second */
	private long checkInterval = 1000;
	
	/** Interval to truncate transfer history file in milliseconds, default is one day */
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
	
	/** Maximum number of parallel download threads (default 1 = no parallel downloads) */
	private int maxDownloadThreads = 1;
	
	/** Interval in millliseconds to check for completed subtasks (default 500 ms) */
	private int taskWaitInterval = 500;
	
	/** Maximum number of wait cycles for task completion checks (default 3600 = total timeout of 30 min) */
	private int maxWaitCycles = 3600;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(BaseMonitor.class);
	
	/**
	 * Structure for controlling the transfer process
	 */
	public static class TransferControl {
		/** Latest reference time of all transfer objects */
		public Instant referenceTime = null;
		/** Objects to transfer */
		public List<TransferObject> transferObjects = new ArrayList<>();
	}
	
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
	 * Gets the maximum number of parallel download threads
	 * 
	 * @return the maximum number of download threads
	 */
	public int getMaxDownloadThreads() {
		return maxDownloadThreads;
	}

	/**
	 * Sets the maximum number of parallel download threads
	 * 
	 * @param maxDownloadThreads the maximum number of download threads to set
	 */
	public void setMaxDownloadThreads(int maxDownloadThreads) {
		this.maxDownloadThreads = maxDownloadThreads;
	}

	/**
	 * Gets the interval to check for completed subtasks
	 * 
	 * @return the task wait interval in millliseconds
	 */
	public int getTaskWaitInterval() {
		return taskWaitInterval;
	}

	/**
	 * Sets the interval to check for completed subtasks
	 * 
	 * @param taskWaitInterval the task wait interval in millliseconds to set
	 */
	public void setTaskWaitInterval(int taskWaitInterval) {
		this.taskWaitInterval = taskWaitInterval;
	}

	/**
	 * Gets the maximum number of wait cycles for task completion checks
	 * 
	 * @return the maximum number of wait cycles
	 */
	public int getMaxWaitCycles() {
		return maxWaitCycles;
	}

	/**
	 * Sets the maximum number of wait cycles for task completion checks
	 * 
	 * @param maxWaitCycles the maximum number of wait cycles to set
	 */
	public void setMaxWaitCycles(int maxWaitCycles) {
		this.maxWaitCycles = maxWaitCycles;
	}

	/**
	 * Read identifiers of transfer objects from history file, sets the reference time stamp as a side effect
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
					
					// Parse entry: Consists of <check date>;<transfer object id>
					String[] historyEntryParts = historyFile.readLine().split(";", 2);
					if (2 != historyEntryParts.length) {
						logger.log(ApiMonitorMessage.ILLEGAL_HISTORY_ENTRY_FORMAT, Arrays.toString(historyEntryParts));
						throw new IllegalArgumentException();
					}
					
					// Highest entry date becomes reference time stamp for next pickup point lookup
					try {
						Instant entryDate = Instant.parse(historyEntryParts[0]);
						if (referenceTimeStamp.isBefore(entryDate)) {
							referenceTimeStamp = entryDate;
						}
					} catch (DateTimeParseException e) {
						logger.log(ApiMonitorMessage.ILLEGAL_HISTORY_ENTRY_DATE, historyEntryParts[0]);
						throw new IllegalArgumentException();
					}
					
					// Add second part of entry to transfer history
					transferHistory.add(historyEntryParts[1]);
					
				}
				
			} catch (IOException e) {
				String message = logger.log(ApiMonitorMessage.HISTORY_READ_FAILED, transferHistoryFile.toString(), e.getMessage());
				throw new IOException(message, e);
			} catch (IllegalArgumentException e) {
				// Do nothing: Already logged, and entry can be ignored; might result in double transfer of object
			}
			
		}
		
		logger.log(
			ApiMonitorMessage.HISTORY_ENTRIES_READ,transferHistory.size(), transferHistoryFile, referenceTimeStamp.toString());
		
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
			String message = logger.log(ApiMonitorMessage.HISTORY_WRITE_FAILED, transferHistoryFile.toString(), e.getMessage());
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
						logger.log(ApiMonitorMessage.ILLEGAL_HISTORY_ENTRY_FORMAT, Arrays.toString(historyEntryParts));
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
						logger.log(ApiMonitorMessage.ILLEGAL_HISTORY_ENTRY_DATE, historyEntryParts[0]);
						throw new IllegalArgumentException();
					}
					
				}
				
			}
			
			Files.delete(tempFilePath);
			
			logger.log(ApiMonitorMessage.HISTORY_ENTRIES_TRUNCATED, truncatedCount, transferHistoryFile.toString());
			
		}
		
		nextHistoryTruncation = Instant.now().plus(historyRetentionDuration);
	}

	/**
	 * Check the pickup point for available objects (unfiltered)
	 * 
	 * @param referenceTimeStamp the reference timestamp to apply for pickup point lookups
	 * @return a transfer control object containing the latest reference time and a list of available transfer objects
	 */
	protected abstract TransferControl checkAvailableDownloads(Instant referenceTimeStamp);
	
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
		
		if (logger.isTraceEnabled()) logger.trace("... objects to transfer after filtering: " + objectsToTransfer.size());
		return objectsToTransfer;
	}
	
	/**
	 * Download the given transfer object from the pickup point and copy it to the configured target directory
	 * 
	 * @param object the transfer object to download
	 * @return true, if the download was successful, false otherwise
	 */
	protected abstract boolean transferToTargetDir(TransferObject object);
	
	/**
	 * Add the given object to the transfer history
	 * 
	 * @param transferredObject the transferred object
	 * @throws IOException if an I/O error occurs opening or writing the transfer history file
	 */
	synchronized private void recordTransfer(TransferObject transferredObject) throws IOException {
		if (logger.isTraceEnabled()) logger.trace(">>> recordTransfer({})",
				null == transferredObject ? "null" : transferredObject.getIdentifier());
		
		// Add identifier of transferred object to history
		String transferredObjectIdentifier = transferredObject.getIdentifier();
		
		transferHistory.add(transferredObjectIdentifier);
		
		writeTransferHistory(transferredObjectIdentifier, transferredObject.getReferenceTime());
		
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
			transferHistory = readTransferHistory(); // Sets the reference time stamp as a side effect
		} catch (IOException e) {
			logger.log(ApiMonitorMessage.ABORTING_MONITOR, e.getMessage());
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
					logger.log(ApiMonitorMessage.ABORTING_MONITOR, e.getMessage());
					return;
				}
			}
			
			// Check data availability on pickup point
			TransferControl transferControl;
			try {
				transferControl = checkAvailableDownloads(referenceTimeStamp);
			} catch (Exception e) {
				logger.log(ApiMonitorMessage.EXCEPTION_CHECKING_DOWNLOADS, e.getClass().getName(), e.getMessage());
				logger.debug("Exception Stack Trace:", e);
				continue;
			}
			
			// Filter objects not yet processed from transfer history
			List<TransferObject> objectsToTransfer = filterTransferableObjects(transferControl.transferObjects);
			
			// Transfer all objects not yet processed
			List<Thread> transferTasks = new ArrayList<>();
			Semaphore semaphore = new Semaphore(maxDownloadThreads);
			
			for (TransferObject objectToTransfer: objectsToTransfer) {
				
				// Setup parallel transfers
				Thread transferTask = new Thread() {

					@Override
					public void run() {
						// Check whether parallel execution is allowed
						try {
							semaphore.acquire();
							if (logger.isDebugEnabled()) logger.debug("... task semaphore acquired, {} permits remaining", semaphore.availablePermits());
						} catch (InterruptedException e) {
							logger.log(ApiMonitorMessage.ABORTING_TASK, e.toString());
							return;
						}
						
						try {
							// Transfer object to local target directory
							if (transferToTargetDir(objectToTransfer)) {
								
								// Record transfer in history
								try {
									recordTransfer(objectToTransfer);
								} catch (IOException e) {
									logger.log(ApiMonitorMessage.ABORTING_TASK, e.toString());
									return;
								}
								
								// Trigger follow-on action
								if (!triggerFollowOnAction(objectToTransfer)) {
									logger.log(ApiMonitorMessage.FOLLOW_ON_ACTION_FAILED, objectToTransfer.getIdentifier());
								}
								
							} else {
								logger.log(ApiMonitorMessage.TRANSFER_FAILED, objectToTransfer.getIdentifier());
							}
						} catch (Exception e) {
							logger.log(ApiMonitorMessage.EXCEPTION_IN_TRANSFER_OR_ACTION, e.getClass().getName(), e.getMessage());
							logger.debug("Exception Stack Trace:", e);
							// continue, releasing semaphore
						}

						// Release parallel thread
						semaphore.release();
						if (logger.isDebugEnabled()) logger.debug("... task semaphore released, {} permits now available", semaphore.availablePermits());
					}
				};
				transferTasks.add(transferTask);
				transferTask.start();
				
			}
			
			// Wait for all tasks to complete
			if (logger.isTraceEnabled()) logger.trace("... waiting for all subtasks to complete");
			for (Thread transferTask: transferTasks) {
				int k = 0;
				while (transferTask.isAlive() && k < maxWaitCycles) {
					try {
						Thread.sleep(taskWaitInterval);
					} catch (InterruptedException e) {
						logger.log(ApiMonitorMessage.TASK_WAIT_INTERRUPTED);
						return;
					}
					++k;
				}
				if (k == maxWaitCycles) {
					// Timeout reached --> kill task and report error
					transferTask.interrupt();
					logger.log(ApiMonitorMessage.SUBTASK_TIMEOUT, (maxWaitCycles * taskWaitInterval) / 1000);
				}
			}
						
			// Wait for next check interval
			try {
				if (logger.isTraceEnabled()) logger.trace("... sleeping for {} seconds", checkInterval / 1000.0);
				Thread.sleep(checkInterval);
			} catch (InterruptedException e) {
				// On interrupt leave monitoring loop
				logger.log(ApiMonitorMessage.INTERRUPTED);
				break;
			}
			
			// Count cycles only if maximum of cycles is set
			if (logger.isTraceEnabled()) logger.trace("... leaving check loop # " + i);
			if (null != maxCycles) ++i;
			
			// Update transfer history
			try {
				transferHistory = readTransferHistory(); // Sets the reference time stamp as a side effect
			} catch (IOException e) {
				logger.log(ApiMonitorMessage.ABORTING_MONITOR, e.getMessage());
				return;
			}
		}
	}
	
	/**
	 * Run forever (as required by Thread superclass, at the same time convenience function for calling {@link #run(Integer)} 
	 * with a <code>null</code> argument)
	 */
	@Override
	public void run() {
		run(null);
	}

}
