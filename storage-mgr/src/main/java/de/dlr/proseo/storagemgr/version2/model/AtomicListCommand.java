package de.dlr.proseo.storagemgr.version2.model;

import java.io.IOException;
import java.util.List;

/**
 * Atomic List Command interface for IO Operations with storages, which returns list<String>
 * 
 * @author Denys Chaykovskiy
 *
 */
public interface AtomicListCommand {
	
	/**
	 * Executes atomic command
	 * 
	 * @return list<string> with result of command execution
	 * @throws exception if atomic command was not successful 
	 */
	public List<String> execute() throws IOException;
	
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
}