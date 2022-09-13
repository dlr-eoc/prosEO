package de.dlr.proseo.planner.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.rest.ReleaseController;
import de.dlr.proseo.planner.ProductionPlanner;

@Component
public class ReleaseControllerImpl implements ReleaseController {

	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(ReleaseControllerImpl.class);

	/** The Production Planner instance */
    @Autowired
    private ProductionPlanner productionPlanner;

	/**
	 * Ingestor releases 'thread' semaphore
	 */
	@Override
	public ResponseEntity<?> releaseSemaphore() {
		if (logger.isTraceEnabled()) logger.trace(">>> releaseSemaphore()");
		try {
			productionPlanner.releaseThreadSemaphore("ingestorSemaphore");
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>("releaseSemaphore failed", HttpStatus.NOT_ACCEPTABLE);
		}
		return new ResponseEntity<>("releaseSemaphore succeded", HttpStatus.OK);
	}

}
