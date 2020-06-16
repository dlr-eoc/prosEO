/**
 * UtilService.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to retrieve the util objects
 * 
 * @author Ernst Melchinger
 *
 */
@Service
public class UtilService {
	
    private static UtilService theUtilService;
    
    /**
     * Autowired instance of job util
     */
    @Autowired
    private JobUtil jobUtil;
    
    /**
     * Autowired instance of job step util
     */
    @Autowired
    private JobStepUtil jobStepUtil;
    
    /**
     * Autowired instance of order util
     */
    @Autowired
    private OrderUtil orderUtil;
    

	/**
	 * Constructor to create the instance
	 */
	public UtilService() {
		super();
		theUtilService = this;
	}
	
	/**
	 * @return the job util
	 */
	public static JobUtil getJobUtil() {
		return theUtilService.jobUtil;
	}

	/**
	 * @return the job step util
	 */
	public static OrderUtil getOrderUtil() {
		return theUtilService.orderUtil;
	}

	/**
	 * @return the order util
	 */
	public static JobStepUtil getJobStepUtil() {
		return theUtilService.jobStepUtil;
	}

}
