package de.dlr.proseo.planner.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.model.rest.AcquireController;
import de.dlr.proseo.planner.ProductionPlanner;

@Component
public class AcquireControllerImpl implements AcquireController {

	/**
	 * Logger of this class
	 */
	private static ProseoLogger logger = new ProseoLogger(AcquireControllerImpl.class);

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;
    
	/**
	 * Ingestor asks for 'thread' semaphore to synchronize product changes
	 */
	@Override
	public ResponseEntity<?> acquireSemaphore(HttpHeaders httpHeaders) {
		if (logger.isTraceEnabled()) logger.trace(">>> acquireSemaphore()");
		try {
			productionPlanner.acquireThreadSemaphore("ingestorSemaphore");
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e);
			
			if (logger.isDebugEnabled()) logger.debug("... exception stack trace: ", e);

			return new ResponseEntity<>("acquireSemaphore failed", HttpStatus.NOT_ACCEPTABLE);
		}
		return new ResponseEntity<>("acquireSemaphore succeded", HttpStatus.OK);
	}

}
