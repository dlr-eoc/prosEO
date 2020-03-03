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
    
    @Autowired
    private JobUtil jobUtil;
    @Autowired
    private JobStepUtil jobStepUtil;
    @Autowired
    private OrderUtil orderUtil;
    

	public UtilService() {
		super();
		theUtilService = this;
	}
	
	public static JobUtil getJobUtil() {
		return theUtilService.jobUtil;
	}
	public static OrderUtil getOrderUtil() {
		return theUtilService.orderUtil;
	}
	public static JobStepUtil getJobStepUtil() {
		return theUtilService.jobStepUtil;
	}

}
