package de.dlr.proseo.ordermgr.rest.model;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import de.dlr.proseo.model.Spacecraft;

public class MissionUtil {
	
	/**
	 * Convert a prosEO model Mission into a REST Mission
	 * 
	 * @param modelMission the prosEO model mission
	 * @return an equivalent REST mission or null, if no model mission was given
	 */
	public static Mission toRestMission(de.dlr.proseo.model.Mission modelMission) {
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
		
		if (null != modelMission.getProcessingModes()) {
			for (String processingModes : modelMission.getProcessingModes()) {
				restMission.getProcessingModes().add(processingModes);
			}
		}
		
		if (null != modelMission.getSpacecrafts()) {
			List <de.dlr.proseo.ordermgr.rest.model.Spacecraft> restSpacecrafts = new ArrayList<de.dlr.proseo.ordermgr.rest.model.Spacecraft>();
			
			for(Spacecraft modelSpacecraft : modelMission.getSpacecrafts()) {			
				de.dlr.proseo.ordermgr.rest.model.Spacecraft restFinal = new de.dlr.proseo.ordermgr.rest.model.Spacecraft (modelSpacecraft.getCode(),modelSpacecraft.getName());			
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
		// TODO Auto-generated method stub
		//return null;
		
		de.dlr.proseo.model.Mission modelMission = new de.dlr.proseo.model.Mission();
		
		modelMission.setId(restMission.getId());
		while (modelMission.getVersion() < restMission.getVersion()) {
			modelMission.incrementVersion();
		}
		modelMission.setCode(restMission.getCode());
		modelMission.setName(restMission.getName());
		
		return modelMission;
	}

}
