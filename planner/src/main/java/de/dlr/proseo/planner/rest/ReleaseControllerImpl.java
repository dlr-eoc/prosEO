/**
 * ReleaseControllerImpl.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.planner.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.model.rest.ReleaseController;
import de.dlr.proseo.planner.ProductionPlanner;

/**
 * A controller to request semaphore release for synchronizing database changes.
 * 
 * @author Ernst Melchinger
 */
@Component
public class ReleaseControllerImpl implements ReleaseController {

	/** Logger of this class*/
	private static ProseoLogger logger = new ProseoLogger(ReleaseControllerImpl.class);

	/** The Production Planner instance */
	@Autowired
	private ProductionPlanner productionPlanner;

	/**
	 * Ingestor releases 'thread' semaphore to synchronize database changes.
	 * 
	 * @param httpHeaders HttpHeaders object containing HTTP headers from the request
	 * @return ResponseEntity representing the result of the acquireSemaphore operation
	 */
	@Override
	public ResponseEntity<?> releaseSemaphore(HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled())
			logger.trace(">>> releaseSemaphore()");
		
		try {
			productionPlanner.releaseThreadSemaphore("ingestorSemaphore");
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);

			if (logger.isDebugEnabled())
				logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>("releaseSemaphore failed", HttpStatus.NOT_ACCEPTABLE);
		}
		
		return new ResponseEntity<>("releaseSemaphore succeded", HttpStatus.OK);
	}

}