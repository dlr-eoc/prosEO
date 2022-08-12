package de.dlr.proseo.storagemgr.version2.s3;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.model.AtomicCommand;

/**
 * S3 Default Retry Strategy for atomic operations 
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3DefaultRetryStrategy {
	
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3AtomicFileUploader.class);
	
	/** Atom command object */
	private AtomicCommand atomicCommand; 

	/** Max attempts */
	private int maxAttempts; 
	
	/** Wait time */
	private long waitTime; 
	
	/**
	 * Constructor 
	 * 
	 * @param atomicCommand atomic command
	 * @param maxAttempts maximal attempts
	 */
	public S3DefaultRetryStrategy(AtomicCommand atomicCommand, int maxAttempts, long waitTime) {
		
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
	public String execute() throws IOException {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> execute() - S3 Retry Strategy({},{})", atomicCommand.getInfo(), maxAttempts);
		
		IOException exception = null; 
		
		for (int i=1; i<=maxAttempts; i++) {
			
			try {
				return atomicCommand.execute();
			}
			catch (IOException e) {
				
				exception = e;
				logger.warn(">>>>> " + atomicCommand.getInfo() + ". Attempt " + i + " was not successful: " + e.getMessage());
				
				threadSleep();
			}	
		}
		
		if (exception == null) {
			logger.error(">>>>> Exception is null. Check max attempts: " + maxAttempts);
			throw new IOException("Exception is null");
			
		} 
		else { 
			logger.error("ERROR. " + atomicCommand.getInfo() + "All " + maxAttempts + " attempts were not successful: " + exception.getMessage());
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
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
}
