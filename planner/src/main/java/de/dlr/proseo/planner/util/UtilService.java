/**
 * UtilService.java
 *
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to retrieve the utility classes
 *
 * @author Ernst Melchinger
 */
@Service
public class UtilService {

	/** The utility class retrieval service */
	private static UtilService theUtilService;

	/** Utility class for managing jobs */
	@Autowired
	private JobUtil jobUtil;

	/** Utility class for managing job steps */
	@Autowired
	private JobStepUtil jobStepUtil;

	/** Utility class for managing orders */
	@Autowired
	private OrderUtil orderUtil;

	/** Constructor to create the instance */
	public UtilService() {
		super();
		theUtilService = this;
	}

	/**
	 * Gets the utility class for managing jobs.
	 * 
	 * @return the utility class for managing jobs
	 */
	public static JobUtil getJobUtil() {
		return theUtilService.jobUtil;
	}

	/**
	 * Gets the utility class for managing job orders.
	 * 
	 * @return the utility class for managing job orders
	 */
	public static OrderUtil getOrderUtil() {
		return theUtilService.orderUtil;
	}

	/**
	 * Gets the utility class for managing job steps.
	 * 
	 * @return the utility class for managing job steps
	 */
	public static JobStepUtil getJobStepUtil() {
		return theUtilService.jobStepUtil;
	}

}
