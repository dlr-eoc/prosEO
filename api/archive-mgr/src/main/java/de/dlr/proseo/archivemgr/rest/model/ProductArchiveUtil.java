/**
 * ProductArchiveUtil.java
 * 
 * (C) 2023 DLR
 */

package de.dlr.proseo.archivemgr.rest.model;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.ProcessingFacility;
import de.dlr.proseo.model.enums.FacilityState;
import de.dlr.proseo.model.enums.StorageType;


public class ProductArchiveUtil {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductArchiveUtil.class);
	
	/**
	 * Convert a prosEO model ProcessingFacility into a REST Facility
	 * 
	 * @param modelFacility the prosEO model ProcessingFacility
	 * @return an equivalent REST processingFacility or null, if no model ProcessingFacility was given
	 */
	
//	public static RestProcessingFacility toRestFacility(ProcessingFacility modelFacility) {
//		if (logger.isTraceEnabled()) logger.trace(">>> toRestFacility({})", (null == modelFacility ? "MISSING" : modelFacility.getId()));
//		
//		if (null == modelFacility)
//			return null;
//		
//		RestProcessingFacility restFacility = new RestProcessingFacility();
//		
//		restFacility.setId(modelFacility.getId());
//
//		restFacility.setVersion(Long.valueOf(modelFacility.getVersion()));
//		if (null != modelFacility.getName()) {
//			restFacility.setName(modelFacility.getName());
//
//		}	
//		if (null != modelFacility.getDescription()) {
//			restFacility.setDescription(modelFacility.getDescription());
//
//		}	
//		if (null != modelFacility.getFacilityState()) {
//			restFacility.setFacilityState(modelFacility.getFacilityState().toString());
//
//		}
//		if (null != modelFacility.getProcessingEngineUrl()) {
//			restFacility.setProcessingEngineUrl(modelFacility.getProcessingEngineUrl());
//
//		}	
//		if (null != modelFacility.getProcessingEngineToken()) {
//			restFacility.setProcessingEngineToken(modelFacility.getProcessingEngineToken());
//
//		}
//		if (null != modelFacility.getMaxJobsPerNode()) {
//			restFacility.setMaxJobsPerNode(modelFacility.getMaxJobsPerNode().longValue());
//
//		}	
//		if (null != modelFacility.getStorageManagerUrl()) {
//			restFacility.setStorageManagerUrl(modelFacility.getStorageManagerUrl());
//
//		}	
//		if (null != modelFacility.getExternalStorageManagerUrl()) {
//			restFacility.setExternalStorageManagerUrl(modelFacility.getExternalStorageManagerUrl());
//
//		}	
//		if (null != modelFacility.getLocalStorageManagerUrl()) {
//			restFacility.setLocalStorageManagerUrl(modelFacility.getLocalStorageManagerUrl());
//
//		}	
//		if (null != modelFacility.getStorageManagerUser()) {
//			restFacility.setStorageManagerUser(modelFacility.getStorageManagerUser());
//
//		}
//		if (null != modelFacility.getStorageManagerPassword()) {
//			restFacility.setStorageManagerPassword(modelFacility.getStorageManagerPassword());
//
//		}
//		if (null != modelFacility.getDefaultStorageType()) {
//			restFacility.setDefaultStorageType(modelFacility.getDefaultStorageType().toString());
//		}
//		
//
//		return restFacility;
//		
//	}
	
	/**
	 * Convert a REST ProcessingFacility into a prosEO model ProcessingFacility (scalar and embedded attributes only, no orbit references)
	 * 
	 * @param restFacility the REST Facility
	 * @return a (roughly) equivalent model Processing Facility
	 * @throws IllegalArgumentException if the REST facility violates syntax rules for date, enum or numeric values
	 */
//	public static ProcessingFacility toModelFacility(RestProcessingFacility restFacility) {
//		if (logger.isTraceEnabled()) logger.trace(">>> toModelFacility({})", (null == restFacility ? "MISSING" : restFacility.getId()));
//		
//		if (null == restFacility)
//			return null;
//		
//		ProcessingFacility modelFacility = new ProcessingFacility();
//		
//		if (null != restFacility.getId() && 0 != restFacility.getId()) {
//			modelFacility.setId(restFacility.getId());
//			while (modelFacility.getVersion() < restFacility.getVersion()) {
//				modelFacility.incrementVersion();
//			} 
//		}
//
//		if (null != restFacility.getName()) {
//			modelFacility.setName(restFacility.getName());
//
//		}	
//		if (null != restFacility.getDescription()) {
//			modelFacility.setDescription(restFacility.getDescription());
//
//		}	
//		if (null != restFacility.getFacilityState()) {
//			modelFacility.setFacilityState(FacilityState.valueOf(restFacility.getFacilityState()));
//
//		}	
//		if (null != restFacility.getProcessingEngineUrl()) {
//			modelFacility.setProcessingEngineUrl(restFacility.getProcessingEngineUrl());
//
//		}	
//		if (null != restFacility.getProcessingEngineToken()) {
//			modelFacility.setProcessingEngineToken(restFacility.getProcessingEngineToken());
//
//		}	
//		if (null != restFacility.getMaxJobsPerNode()) {
//			modelFacility.setMaxJobsPerNode(restFacility.getMaxJobsPerNode().intValue());
//
//		}	
//		if (null != restFacility.getStorageManagerUrl()) {
//			modelFacility.setStorageManagerUrl(restFacility.getStorageManagerUrl());
//
//		}	
//		if (null != restFacility.getExternalStorageManagerUrl()) {
//			modelFacility.setExternalStorageManagerUrl(restFacility.getExternalStorageManagerUrl());
//
//		}	
//		if (null != restFacility.getLocalStorageManagerUrl()) {
//			modelFacility.setLocalStorageManagerUrl(restFacility.getLocalStorageManagerUrl());
//
//		}	
//		if (null != restFacility.getStorageManagerUser()) {
//			modelFacility.setStorageManagerUser(restFacility.getStorageManagerUser());
//
//		}	
//		if (null != restFacility.getStorageManagerPassword()) {
//			modelFacility.setStorageManagerPassword(restFacility.getStorageManagerPassword());
//
//		}	
//		if (null != restFacility.getDefaultStorageType()) {		
//			modelFacility.setDefaultStorageType(StorageType.valueOf(restFacility.getDefaultStorageType()));
//		}		
//
//		return modelFacility;
//		
//		
//	}


}
