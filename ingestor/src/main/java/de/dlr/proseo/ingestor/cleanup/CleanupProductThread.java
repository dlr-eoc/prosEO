package de.dlr.proseo.ingestor.cleanup;

import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;
import de.dlr.proseo.ingestor.IngestorApplication;
import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * Thread to look for deletable product files 
 * 
 * @author Ernst Melchinger
 *
 */
public class CleanupProductThread extends Thread {

	/**
	 * Logger for this class
	 */
	private static ProseoLogger logger = new ProseoLogger(CleanupProductThread.class);
	
	/**
	 * The ingestor instance
	 */
	private IngestorApplication ingestor;
	
	/**
	 * Create new CleanupProductThread for ingestor
	 * 
	 * @param ingestor the ingestor application
	 */
	public CleanupProductThread(IngestorApplication ingestor) {
		super("CleanupProducts");
		this.setDaemon(true);
		this.ingestor = ingestor;
	}
	

	/**
	 * Start the cleanup cycle thread to look for deletable products and its files every cleanupCycleTime hours
	 * 
	 * @see java.lang.Thread#run()
	 */
    public void run() {
		if (logger.isTraceEnabled()) logger.trace(">>> run()");
		// default wait is one day 24h * 60' * 60'' * 1000 millis
		long wait = 24;
		if (this.ingestor.getIngestorConfig().getCleanupCycleTime() != null) {
			wait = this.ingestor.getIngestorConfig().getCleanupCycleTime();
		};
		wait = wait * 60 * 60 * 1000;
		
		while (!this.isInterrupted()) {
			if (logger.isTraceEnabled()) logger.trace(">>> cleanup products cycle()");
			Instant t = Instant.now();
			// search for products to delete
			ingestor.getProductIngestor().deleteProductFilesOlderThan(t);
			try {
				sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
