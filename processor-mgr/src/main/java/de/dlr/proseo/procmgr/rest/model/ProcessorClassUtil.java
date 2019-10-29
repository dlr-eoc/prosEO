/**
 * ProcessorClassUtil.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.ProductClass;

/**
 * Utility methods for processor classes, e. g. for conversion between prosEO model and REST model
 * 
 * @author Dr. Thomas Bassler
 */
public class ProcessorClassUtil {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProcessorClassUtil.class);
	
	/**
	 * Convert a prosEO model processor class into a REST processor class
	 * 
	 * @param modelProcessorClass the prosEO model product
	 * @return an equivalent REST product or null, if no model product was given
	 */
	public static RestProcessorClass toRestProcessorClass(ProcessorClass modelProcessorClass) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestProcessorClass({})", (null == modelProcessorClass ? "MISSING" : modelProcessorClass.getId()));

		if (null == modelProcessorClass)
			return null;
		
		RestProcessorClass restProcessorClass = new RestProcessorClass();
		
		restProcessorClass.setId(modelProcessorClass.getId());
		restProcessorClass.setVersion(Long.valueOf(modelProcessorClass.getVersion()));
		restProcessorClass.setMissionCode(modelProcessorClass.getMission().getCode());
		restProcessorClass.setProcessorName(modelProcessorClass.getProcessorName());
		
		for (ProductClass productClass: modelProcessorClass.getProductClasses()) {
			restProcessorClass.getProductClasses().add(productClass.getProductType());
		}
		
		return restProcessorClass;
	}
	
	/**
	 * Convert a REST processor class into a prosEO model processor class (scalar and embedded attributes only, no object references)
	 * 
	 * @param restProcessorClass the REST product
	 * @return a (roughly) equivalent model product or null, if no REST product was given
	 * @throws IllegalArgumentException if the REST product violates syntax rules for date, enum or numeric values
	 */
	public static ProcessorClass toModelProcessorClass(RestProcessorClass restProcessorClass) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> toModelProcessorClass({})", (null == restProcessorClass ? "MISSING" : restProcessorClass.getProcessorName()));

		if (null == restProcessorClass)
			return null;
		
		ProcessorClass modelProcessorClass = new ProcessorClass();
		
		if (null != restProcessorClass.getId() && 0 != restProcessorClass.getId()) {
			modelProcessorClass.setId(restProcessorClass.getId());
			while (modelProcessorClass.getVersion() < restProcessorClass.getVersion()) {
				modelProcessorClass.incrementVersion();
			} 
		}
		modelProcessorClass.setProcessorName(restProcessorClass.getProcessorName());
		
		return modelProcessorClass;
	}
}
