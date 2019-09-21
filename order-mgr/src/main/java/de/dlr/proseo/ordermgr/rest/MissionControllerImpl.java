/**
 * MissionControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import de.dlr.proseo.ordermgr.rest.model.Mission;
import de.dlr.proseo.ordermgr.rest.model.MissionUtil;
import de.dlr.proseo.model.service.RepositoryService;

/**
 * Spring MVC controller for the prosEO Order Manager; implements the services required to manage missions
 * 
 * @author Ranjitha Vignesh
 *
 */
@Component
public class MissionControllerImpl implements MissionController {

	/* Message ID constants */
	private static final int MSG_ID_MISSION_NOT_FOUND = 2001;
	private static final int MSG_ID_ENCLOSING_MISSION_NOT_FOUND = 2002;
	private static final int MSG_ID_COMPONENT_MISSION_NOT_FOUND = 2003;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 2004;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_MISSION_NOT_FOUND = "No mission found for ID %d (%d)";
	private static final String MSG_ENCLOSING_MISSION_NOT_FOUND = "Enclosing mission with ID %d not found (%d)";
	private static final String MSG_COMPONENT_MISSION_NOT_FOUND = "Component mission with ID %d not found (%d)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "Mission deletion unsuccessful for ID %d (%d)";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ingestor ";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(MissionControllerImpl.class);

	@Override
	public ResponseEntity<List<Mission>> getMissions() {
		if (logger.isTraceEnabled()) logger.trace(">>> getMissions");
		
		List<de.dlr.proseo.ordermgr.rest.model.Mission> result = new ArrayList<>();
		
		// Simple case: no search criteria set
		for (de.dlr.proseo.model.Mission  mission: RepositoryService.getMissionRepository().findAll()) {
			if (logger.isDebugEnabled()) logger.debug("Found mission with ID {}", mission.getId());
			Mission resultMission = MissionUtil.toRestMission(mission);
			if (logger.isDebugEnabled()) logger.debug("Created result mission with ID {}", resultMission.getId());
			result.add(resultMission);
		}
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Mission> createMission(@Valid Mission mission) {
		// TODO Auto-generated method stub

		String message = String.format(MSG_PREFIX + "POST not implemented (%d)", 2001);
		logger.error(message);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, message);
		return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		
	}

	@Override
	public ResponseEntity<Mission> getMissionById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getMissionById({})", id);
		
		Optional<de.dlr.proseo.model.Mission> modelMission = RepositoryService.getMissionRepository().findById(id);
		
		if (modelMission.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_MISSION_NOT_FOUND, id, MSG_ID_MISSION_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(MissionUtil.toRestMission(modelMission.get()), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<Mission> updateMission(Long id, @Valid Mission mission) {
		// TODO Auto-generated method stub
		return null;
	
		/*if (logger.isTraceEnabled()) logger.trace(">>> modifyMission({})", id);
		
		Optional<de.dlr.proseo.model.Mission> optModelMission = RepositoryService.getMissionRepository().findById(id);
		
		if (optModelMission.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_MISSION_NOT_FOUND, id, MSG_ID_MISSION_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		de.dlr.proseo.model.Mission modelMission = optModelMission.get();
		
		// Update modified attributes
		boolean missionChanged = false;
		de.dlr.proseo.model.Mission changedMission = MissionUtil.toModelMission(mission);
		
		if (!modelMission.getCode().equals(changedMission.getCode())) {
			missionChanged = true;
			modelMission.setCode(changedMission.getCode());;
		}
		
		if (!modelMission.getName().equals(changedMission.getName())) {
			missionChanged = true;
			modelMission.setName(changedMission.getName());;
		}
		// Save mission only if anything was actually changed
		if (missionChanged)	{
			modelMission.incrementVersion();
			modelMission = RepositoryService.getMissionRepository().save(modelMission);
		}
		
		return new ResponseEntity<>(MissionUtil.toRestMission(modelMission), HttpStatus.OK);*/
	
	}

	@Override
	public ResponseEntity<?> deleteMissionById(Long id) {
		// TODO Auto-generated method stub
		//return null;
		
		 if (logger.isTraceEnabled()) logger.trace(">>> deleteMissionById({})", id);
			
			// Test whether the mission id is valid
			Optional<de.dlr.proseo.model.Mission> modelMission = RepositoryService.getMissionRepository().findById(id);
			if (modelMission.isEmpty()) {
				String message = String.format(MSG_PREFIX + MSG_MISSION_NOT_FOUND, id, MSG_ID_MISSION_NOT_FOUND);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			}
			
			// Delete the mission
			RepositoryService.getMissionRepository().deleteById(id);

			// Test whether the deletion was successful
			modelMission = RepositoryService.getMissionRepository().findById(id);
			if (!modelMission.isEmpty()) {
				String message = String.format(MSG_PREFIX + MSG_DELETION_UNSUCCESSFUL, id, MSG_ID_DELETION_UNSUCCESSFUL);
				logger.error(message);
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.set(HTTP_HEADER_WARNING, message);
				return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
			}
			
			HttpHeaders responseHeaders = new HttpHeaders();
			return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
		}
		
	

}
