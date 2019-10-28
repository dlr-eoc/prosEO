package de.dlr.proseo.ordermgr.rest.model;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Spacecraft;

public class MissionUtil {
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(MissionUtil.class);

	/**
	 * Convert a prosEO model Mission into a REST Mission
	 * 
	 * @param modelMission the prosEO model mission
	 * @return an equivalent REST mission or null, if no model mission was given
	 */
	public static Mission toRestMission(de.dlr.proseo.model.Mission modelMission) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestMission({})", (null == modelMission ? "MISSING" : modelMission.getId()));

		if (null == modelMission)
			return null;
		
		Mission restMission = new Mission();
		
		restMission.setId(modelMission.getId());
		restMission.setVersion(Long.valueOf(modelMission.getVersion()));
		
		if (null != modelMission.getCode()) {
			restMission.setCode(modelMission.getCode());
		}
		
		if (null != modelMission.getName()) {
			restMission.setName(modelMission.getName());
		}
		
		if(null != modelMission.getFileClasses()) {
			
			for (String fileClass : modelMission.getFileClasses()) {
				restMission.getFileClasses().add(fileClass);
			}
			
		}
		
		if (null != modelMission.getProcessingModes()) {
			for (String processingModes : modelMission.getProcessingModes()) {
				restMission.getProcessingModes().add(processingModes);
			}
		}
		
		if(null != modelMission.getProductFileTemplate()) {
			restMission.setProductFileTemplate(modelMission.getProductFileTemplate());
		}
		

		
		if (null != modelMission.getSpacecrafts()) {
			List <de.dlr.proseo.ordermgr.rest.model.Spacecraft> restSpacecrafts = new ArrayList<de.dlr.proseo.ordermgr.rest.model.Spacecraft>();
			
			for(Spacecraft modelSpacecraft : modelMission.getSpacecrafts()) {			
				de.dlr.proseo.ordermgr.rest.model.Spacecraft restFinal = new de.dlr.proseo.ordermgr.rest.model.Spacecraft(
						modelSpacecraft.getId(), Long.valueOf(modelSpacecraft.getVersion()), modelSpacecraft.getCode(), modelSpacecraft.getName());			
				restSpacecrafts.add(restFinal);						
			}
			restMission.setSpacecrafts(restSpacecrafts);
		}
		
		return restMission;
	}
	
	/**
	 * Convert a REST mission into a prosEO model mission (scalar and embedded attributes only, no mission references)
	 * 
	 * @param restMission the REST mission
	 * @return a (roughly) equivalent model mission
	 * @throws IllegalArgumentException if the REST mission violates syntax rules for date, enum or numeric values
	 */

	public static de.dlr.proseo.model.Mission toModelMission(Mission restMission) throws IllegalArgumentException{
		
		if (logger.isTraceEnabled()) logger.trace(">>> toModelMission({})", (null == restMission ? "MISSING" : restMission.getId()));
		
		de.dlr.proseo.model.Mission modelMission = new de.dlr.proseo.model.Mission();
		
		modelMission.setId(restMission.getId());
		while (modelMission.getVersion() < restMission.getVersion()) {
			modelMission.incrementVersion();
		}
		modelMission.setCode(restMission.getCode());
		modelMission.setName(restMission.getName());
		
		modelMission.getFileClasses().clear();
		for(String fileClass : restMission.getFileClasses()) {
			modelMission.getFileClasses().add(fileClass);
		}
		
		modelMission.setProductFileTemplate(restMission.getProductFileTemplate());
		
		modelMission.getProcessingModes().clear();
		for(String mode : restMission.getProcessingModes()) {
			modelMission.getProcessingModes().add(mode);
		}
		
		return modelMission;
	}

}
