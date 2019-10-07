package de.dlr.proseo.planner.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductControllerImpl implements ProductController {

	/**
	 * Product created and available, sent by prosEO Ingestor
	 * 
	 */
	@Override
	public ResponseEntity<?> modifyProduct(String productid) {
		// todo 
		// look for product

		// search dependent jobs/jobsteps

		// update ready products of jobs/jobsteps

		// run/activate jobsteps which are now ready to run
		return null;
	}

}
