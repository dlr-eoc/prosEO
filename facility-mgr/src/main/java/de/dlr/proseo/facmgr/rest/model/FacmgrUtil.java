package de.dlr.proseo.facmgr.rest.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.ProductFile.StorageType;


public class FacmgrUtil {
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(FacmgrUtil.class);
	
	/**
	 * Convert a prosEO model ProcessingFacility into a REST Facility
	 * 
	 * @param modelFacility the prosEO model ProcessingFacility
	 * @return an equivalent REST processingFacility or null, if no model ProcessingFacility was given
	 */

	public static RestProcessingFacility toRestFacility(ProcessingFacility modelFacility) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestFacility({})", (null == modelFacility ? "MISSING" : modelFacility.getId()));
		
		if (null == modelFacility)
			return null;
		
		RestProcessingFacility restFacility = new RestProcessingFacility();
		
		restFacility.setId(modelFacility.getId());

		restFacility.setVersion(Long.valueOf(modelFacility.getVersion()));
		logger.info("Coming here");
		if (null != modelFacility.getName()) {
			restFacility.setName(modelFacility.getName());

		}	
		if (null != modelFacility.getDescription()) {
			restFacility.setDescription(modelFacility.getDescription());

		}	
		if (null != modelFacility.getProcessingEngineUrl()) {
			restFacility.setProcessingEngineUrl(modelFacility.getProcessingEngineUrl());

		}	
		if (null != modelFacility.getStorageManagerUrl()) {
			restFacility.setStorageManagerUrl(modelFacility.getStorageManagerUrl());

		}	
		
		if (null != modelFacility.getDefaultStorageType()) {
			restFacility.setDefaultStorageType(modelFacility.getDefaultStorageType().toString());
		}
		
		logger.info("Coming here till the end");

		return restFacility;
		
	}
	
	/**
	 * Convert a REST ProcessingFacility into a prosEO model ProcessingFacility (scalar and embedded attributes only, no orbit references)
	 * 
	 * @param restFacility the REST Facility
	 * @return a (roughly) equivalent model Processing Facility
	 * @throws IllegalArgumentException if the REST facility violates syntax rules for date, enum or numeric values
	 */
	public static ProcessingFacility toModelFacility(RestProcessingFacility restFacility) {
		if (logger.isTraceEnabled()) logger.trace(">>> toModelFacility({})", (null == restFacility ? "MISSING" : restFacility.getId()));
		
		if (null == restFacility)
			return null;
		
		ProcessingFacility modelFacility = new ProcessingFacility();
		
		if (null != restFacility.getId() && 0 != restFacility.getId()) {
			modelFacility.setId(restFacility.getId());
			while (modelFacility.getVersion() < restFacility.getVersion()) {
				modelFacility.incrementVersion();
			} 
		}
		
		if (null != restFacility.getName()) {
			modelFacility.setName(restFacility.getName());

		}	
		if (null != restFacility.getDescription()) {
			modelFacility.setDescription(restFacility.getDescription());

		}	
		if (null != restFacility.getProcessingEngineUrl()) {
			modelFacility.setProcessingEngineUrl(restFacility.getProcessingEngineUrl());

		}	
		if (null != restFacility.getStorageManagerUrl()) {
			modelFacility.setStorageManagerUrl(restFacility.getStorageManagerUrl());

		}	
		
		if (null != restFacility.getDefaultStorageType()) {		
			modelFacility.setDefaultStorageType(StorageType.valueOf(restFacility.getDefaultStorageType()));
		}		
		
		return modelFacility;
		
		
	}


}
