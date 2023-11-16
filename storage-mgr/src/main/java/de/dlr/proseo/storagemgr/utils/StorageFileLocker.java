/**
 * StorageFileLocker.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.utils;

import java.util.concurrent.ConcurrentSkipListSet;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.storagemgr.Exceptions.FileLockedAfterMaxCyclesException;

/**
 * A mechanism to lock and unlock files during the download process. It helps
 * manage concurrent access to files by ensuring that only one process or thread
 * can access a file at a time.
 *
 * @author Denys Chaykovskiy
 */
public class StorageFileLocker {

	/**
	 * Stores the paths of files that are currently being downloaded and thus are
	 * locked
	 */
	private static ConcurrentSkipListSet<String> productLockSet = new ConcurrentSkipListSet<>();

	/** Logger */
	private static ProseoLogger logger = new ProseoLogger(StorageFileLocker.class);

	/** The maximum number of cycles for checking if a file is locked */
	private long fileCheckMaxCycles;

	/** The wait time between each cycle of checking the file lock status */
	private long waitTime;

	/** The path of the file that needs to be locked */
	private String path;

	/**
	 * Constructor
	 *
	 * @param path               path of the file
	 * @param waitTime           the wait time between each cycle of checking the
	 *                           file lock status
	 * @param fileCheckMaxCycles file check max cycles
	 */
	public StorageFileLocker(String path, long waitTime, long fileCheckMaxCycles) {

		this.path = path;
		this.fileCheckMaxCycles = fileCheckMaxCycles;
		this.waitTime = waitTime;
	}

	/**
	 * Acquires a lock on the file. The method checks if the file is already locked
	 * by checking if it exists in the productLockSet. If it's not locked, it adds
	 * the file path to the set and breaks out of the loop. Otherwise, it waits for
	 * the specified waitTime and checks again. This process continues for a maximum
	 * of fileCheckMaxCycles cycles.
	 *
	 * @throws FileLockedAfterMaxCyclesException if the file is still locked after a
	 *                                           maximum number of checks
	 * @throws InterruptedException              if the thread is interrupted while
	 *                                           waiting for the concurrent access
	 *                                           to terminate
	 */
	public void lock() throws FileLockedAfterMaxCyclesException, InterruptedException {

		long i = 0;

		for (; i < fileCheckMaxCycles; ++i) {

			synchronized (productLockSet) {
				if (!productLockSet.contains(path)) {
					productLockSet.add(path);
					break;
				}
			}

			if (logger.isDebugEnabled())
				logger.debug("... waiting for concurrent access to {} to terminate", path);
			Thread.sleep(waitTime);
		}

		if (i == fileCheckMaxCycles) {
			throw new FileLockedAfterMaxCyclesException("File Path: " + path);
		}
	}

	/**
	 * Releases the lock on the file by removing the file path from the
	 * productLockSet.
	 */
	public void unlock() {
		productLockSet.remove(path);
	}
}