/**
 * DefaultRetryStrategy.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.version2.model;

import java.io.IOException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.StorageMgrMessage;

/**
 * A retry strategy for executing atomic operations. The class provides a
 * mechanism to retry a specified command multiple times until it succeeds or
 * reaches the configured maximum number of attempts.
 *
 * @author Denys Chaykovskiy
 */
public class DefaultRetryStrategy<T> {

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(DefaultRetryStrategy.class);

	/** Atom command object */
	private AtomicCommand<T> atomicCommand;

	/** Maximum number of retry attempts */
	private int maxAttempts;

	/** Wait time between retries */
	private long waitTime;

	/**
	 * The constructor initializes the retry strategy with the atomic command to be
	 * executed, the maximum number of attempts, and the wait time between each
	 * attempt.
	 *
	 * @param atomicCommand atomic command
	 * @param maxAttempts   maximal attempts
	 * @param waitTime 		wait time between retries
	 */
	public DefaultRetryStrategy(AtomicCommand<T> atomicCommand, int maxAttempts, long waitTime) {

		this.atomicCommand = atomicCommand;
		this.maxAttempts = maxAttempts;
		this.waitTime = waitTime;
	}

	/**
	 * Tries to execute the atomic command and returns its result if successful. If
	 * the atomic command throws an IOException, the retry strategy catches and
	 * records it. It repeats the execution of the atomic command for a specified
	 * number of attempts, waiting for a certain time interval between each attempt.
	 * If the maximum number of attempts is reached without a successful execution,
	 * the retry strategy throws the recorded exception.
	 *
	 * @return string with result of strategy execution
	 * @throws IOException if atom command was not successful maxAttempts times
	 */
	public T execute() throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - Default Retry Strategy({},{})", atomicCommand.getInfo(), maxAttempts);

		IOException exception = null;

		for (int i = 1; i <= maxAttempts; i++) {

			try {
				// Execute the command and return its result if successful.
				return atomicCommand.execute();
			} catch (IOException e) {
				// Catch and record thrown IOException
				exception = e;

				if (logger.isTraceEnabled())
					logger.trace("Attempt " + i + " was not successful: " + atomicCommand.getFailedInfo() + e.getMessage());

				// Wait before the next try.
				threadSleep();
			}
		}

		if (exception == null) {
			logger.log(StorageMgrMessage.EXCEPTION_IS_NULL);
			throw new IOException("Exception is null");

		} else {
			// If the maximum attempts are reached without a success, throw recorded
			// exception.

			logger.log(StorageMgrMessage.ATTEMPTS_WERE_NOT_SUCCESSFUL, maxAttempts,
					atomicCommand.getInfo() + exception.getMessage());
			exception.printStackTrace();
			throw exception;
		}
	}

	/**
	 * Cause the current thread to sleep for the specified wait time between each
	 * attempt at command execution
	 */
	private void threadSleep() {
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("An exception occurred. Cause: ", e);
			}
		}
	}
}