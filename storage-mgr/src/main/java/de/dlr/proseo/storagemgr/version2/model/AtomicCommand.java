package de.dlr.proseo.storagemgr.version2.model;

import java.io.IOException;

/**
 * Atomic Command interface for IO Operations with storages
 * 
 * @author Denys Chaykovskiy
 *
 */
public interface AtomicCommand <T> {
	
	/**
	 * Executes atomic command
	 * 
	 * @return string with result of command execution
	 * @throws exception if atomic command was not successful 
	 */
	public T execute() throws IOException;
	
	/**
	 * Gets Information about atomic command (mostly for logs)
	 * 
	 * @return Information about atomic command
	 */
	public String getInfo(); 
	
	/**
	 * Gets Information about completed atomic command (mostly for logs)
	 * 
	 * @return Information about completed atomic command
	 */
	public String getCompletedInfo(); 
	
	/**
	 * Gets Information about failed atomic command (mostly for logs)
	 * 
	 * @return Information about failed atomic command
	 */
	public String getFailedInfo(); 
}