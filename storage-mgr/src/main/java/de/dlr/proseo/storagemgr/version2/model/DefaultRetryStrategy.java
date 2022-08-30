package de.dlr.proseo.storagemgr.version2.model;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Retry Strategy for atomic operations
 * 
 * @author Denys Chaykovskiy
 *
 */
public class DefaultRetryStrategy<T> {

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(DefaultRetryStrategy.class);

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
					logger.trace("Attempt " + i + " was not successful: " + atomicCommand.getInfo() + e.getMessage());

				threadSleep();
			}
		}

		if (exception == null) {
			logger.error(">>>>> Exception is null. Check max attempts: " + maxAttempts);
			throw new IOException("Exception is null");

		} else {
			logger.error("All " + maxAttempts + " attempts were not successful: " + atomicCommand.getInfo()
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
