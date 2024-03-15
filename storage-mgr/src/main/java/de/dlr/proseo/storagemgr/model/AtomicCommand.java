/**
 * AtomicCommand.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.model;

import java.io.IOException;

/**
 * An interface for atomic operations or commands related to IO operations with
 * storage systems. It provides a set of methods to execute, retrieve
 * information, and handle the results of these atomic commands.
 * 
 * @author Denys Chaykovskiy
 */
public interface AtomicCommand<T> {

	/**
	 * Executes atomic command
	 * 
	 * @return string with result of command execution
	 * @throws IOException if atomic command was not successful
	 */
	public T execute() throws IOException;

	/**
	 * Gets information about atomic command (mostly for logs)
	 * 
	 * @return information about atomic command
	 */
	public String getInfo();

	/**
	 * Gets information about completed atomic command (mostly for logs)
	 * 
	 * @return information about completed atomic command
	 */
	public String getCompletedInfo();

	/**
	 * Gets information about failed atomic command (mostly for logs)
	 * 
	 * @return information about failed atomic command
	 */
	public String getFailedInfo();
}