package de.dlr.proseo.storagemgr.version2.model;

import java.io.IOException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.StorageMgrMessage;

/**
 * Default Retry Strategy for atomic operations
 * 
 * @author Denys Chaykovskiy
 *
 */
public class DefaultRetryStrategy<T> {

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(DefaultRetryStrategy.class);

	/** Atom command object */
	private AtomicCommand<T> atomicCommand;

	/** Max attempts */
	private int maxAttempts;

	/** Wait time */
	private long waitTime;

	/**
	 * Constructor
	 * 
	 * @param atomicCommand atomic command
	 * @param maxAttempts   maximal attempts
	 */
	public DefaultRetryStrategy(AtomicCommand<T> atomicCommand, int maxAttempts, long waitTime) {

		this.atomicCommand = atomicCommand;
		this.maxAttempts = maxAttempts;
		this.waitTime = waitTime;
	}

	/**
	 * Executes strategy of retrying atom command
	 * 
	 * @return string with result of strategy execution
	 * @throws exception if atom command was not successful maxAttempts times
	 */
	public T execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - Default Retry Strategy({},{})", atomicCommand.getInfo(), maxAttempts);

		IOException exception = null;

		for (int i = 1; i <= maxAttempts; i++) {

			try {
				return atomicCommand.execute();
			} catch (IOException e) {

				exception = e;

				if (logger.isTraceEnabled())
					logger.trace("Attempt " + i + " was not successful: " + atomicCommand.getFailedInfo() + e.getMessage());

				threadSleep();
			}
		}

		if (exception == null) {
			logger.log(StorageMgrMessage.EXCEPTION_IS_NULL);
			throw new IOException("Exception is null");

		} else {
			logger.log(StorageMgrMessage.ATTEMPTS_WERE_NOT_SUCCESSFUL, maxAttempts, atomicCommand.getInfo()
					+ exception.getMessage());
			exception.printStackTrace();
			throw exception;
		}
	}

	/**
	 * Thread sleep
	 * 
	 */
	private void threadSleep() {

		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
