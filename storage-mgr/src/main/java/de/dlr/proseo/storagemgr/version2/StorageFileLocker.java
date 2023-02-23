package de.dlr.proseo.storagemgr.version2;

import java.util.concurrent.ConcurrentSkipListSet;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.storagemgr.version2.Exceptions.FileLockedAfterMaxCyclesException;

/**
 * Storage File Locker Locks files currently being downloaded from external
 * storage
 * 
 * @author Denys Chaykovskiy
 *
 */
/**
 * @author dchaykovskiy
 *
 */
public class StorageFileLocker {

	/** List of currently being downloaded files, which are locked */
	private static ConcurrentSkipListSet<String> productLockSet = new ConcurrentSkipListSet<>();

	/** Logger */
	private static ProseoLogger logger = new ProseoLogger(StorageFileLocker.class);

	/** File check max cycles */
	private long fileCheckMaxCycles;

	/** Wait time */
	private long waitTime;

	/** file path to lock */
	private String path;

	/**
	 * Constructor
	 * 
	 * @param path               path of the file
	 * @param fileCheckMaxCycles file check max cycles
	 */
	public StorageFileLocker(String path, long waitTime, long fileCheckMaxCycles) {

		this.path = path;
		this.fileCheckMaxCycles = fileCheckMaxCycles;
		this.waitTime = waitTime;
	}

	/**
	 * Locks the file
	 * 
	 * @throws FileLockedAfterMaxCyclesException
	 * @throws InterruptedException
	 * 
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
	 * Unlocks the file
	 * 
	 */
	public void unlock() {
		productLockSet.remove(path);
	}
}
