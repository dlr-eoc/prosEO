package de.dlr.proseo.storagemgr.version2.s3;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.model.AtomicListCommand;

/**
 * S3 List Retry Strategy for atomic operations 
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3ListRetryStrategy {
	
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicFileUploader.class);
	
	/** Atom command object */
	private AtomicListCommand atomicListCommand; 

	/** Max attempts */
	private int maxAttempts; 

	/**
	 * Constructor 
	 * 
	 * @param atomicListCommand atomic list command
	 * @param maxAttempts maximal attempts
	 */
	public S3ListRetryStrategy(AtomicListCommand atomicListCommand, int maxAttempts) {
		
		this.atomicListCommand = atomicListCommand; 
		this.maxAttempts = maxAttempts; 
	}
	
	/**
	 * Executes strategy of retrying atom command 
	 * 
	 * @return List<String> with result of strategy execution
	 * @throws exception if atomic list command was not successful maxAttempts times 
	 */
	public List<String> execute() throws IOException {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - S3 List Retry Strategy({},{})", atomicListCommand.getInfo(), maxAttempts);
		
		IOException exception = null; 
		
		for (int i=1; i<=maxAttempts; i++) {
			
			try {
				return atomicListCommand.execute();
			}
			catch (IOException e) {
				
				exception = e;
				logger.warn(">>>>> " + atomicListCommand.getInfo() + ". Attempt " + i + " was not successful: " + e.getMessage());
			}	
		}
		
		if (exception == null) {
			logger.error(">>>>> Exception is null. Check max attempts: " + maxAttempts);
			throw new IOException("Exception is null");
		} 
		else { 
			logger.error("ERROR. " + atomicListCommand.getInfo() + "All " + maxAttempts + " attempts were not successful: " + exception.getMessage());
			exception.printStackTrace();
			throw exception; 
		}
	}
}
