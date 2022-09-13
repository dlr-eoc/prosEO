package de.dlr.proseo.planner.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.rest.AcquireController;
import de.dlr.proseo.planner.ProductionPlanner;

@Component
public class AcquireControllerImpl implements AcquireController {

	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(AcquireControllerImpl.class);

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;
    
	/**
	 * Ingestor asks for 'thread' semaphore to synchronize product changes
	 */
	@Override
	public ResponseEntity<?> acquireSemaphore() {
		if (logger.isTraceEnabled()) logger.trace(">>> acquireSemaphore()");
		try {
			productionPlanner.acquireThreadSemaphore("ingestorSemaphore");
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>("acquireSemaphore failed", HttpStatus.NOT_ACCEPTABLE);
		}
		return new ResponseEntity<>("acquireSemaphore succeded", HttpStatus.OK);
	}

}
