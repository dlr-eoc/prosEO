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
import de.dlr.proseo.ordermgr.rest.model.RestMission;
import de.dlr.proseo.ordermgr.rest.model.MissionUtil;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.ProductClass;
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
	private static final int MSG_ID_MISSION_NOT_FOUND = 1001;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 1004;
	private static final int MSG_ID_NOT_IMPLEMENTED = 9000;
	
	/* Message string constants */
	private static final String MSG_MISSION_NOT_FOUND = "No mission found for ID %d (%d)";
	private static final String MSG_DELETION_UNSUCCESSFUL = "Mission deletion unsuccessful for ID %d (%d)";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ordermgr-missioncontroller ";
	private static final String MSG_MISSIONS_NOT_FOUND = "No missions found";


	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(MissionControllerImpl.class);

	
	/**
	 * List of all missions with no search criteria

	 * @return a response entity with either a list of missions and HTTP status OK or an error message and an HTTP status indicating failure
	 */
	@Override
	public ResponseEntity<List<RestMission>> getMissions() {
		if (logger.isTraceEnabled()) logger.trace(">>> getMissions");
		
		List<RestMission> result = new ArrayList<>();
		String message = String.format(HTTP_HEADER_WARNING +": " +  MSG_MISSIONS_NOT_FOUND +" "+  MSG_ID_MISSION_NOT_FOUND);
		
		Iterable<Mission> modelMission = RepositoryService.getMissionRepository().findAll();

		if(modelMission.spliterator().getExactSizeIfKnown() == 0) {
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			logger.info(message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);			
		}
		
		// Simple case: no search criteria set
		for (Mission  mission: RepositoryService.getMissionRepository().findAll()) {
			if (logger.isDebugEnabled()) logger.debug("Found mission with ID {}", mission.getId());
			RestMission resultMission = MissionUtil.toRestMission(mission);
			if (logger.isDebugEnabled()) logger.debug("Created result mission with ID {}", resultMission.getId());
			result.add(resultMission);
		}		
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	/**
	 * Create a mission from the given Json object 
	 * 
	 * @param mission the Json object to create the mission from
	 * @return a response containing a Json object corresponding to the mission after persistence (with ID and version for all 
	 * 		   contained objects) and HTTP status "CREATED"
	 */
	@Override
	public ResponseEntity<RestMission> createMission(@Valid RestMission mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> createMission({})", mission.getClass());
		
		Mission modelMission = MissionUtil.toModelMission(mission);
		
		modelMission.getProcessorClasses().clear();
		for (ProcessorClass procClass : RepositoryService.getProcessorClassRepository().findAll()) {			
			if(procClass.getMission().getCode().equals(modelMission.getCode())) {
				modelMission.getProcessorClasses().add(procClass);
			}		
		}
		
		modelMission.getProductClasses().clear();
		for (ProductClass prodClass : RepositoryService.getProductClassRepository().findByMissionCode(modelMission.getCode())) {
				modelMission.getProductClasses().add(prodClass);
		}
		
		modelMission = RepositoryService.getMissionRepository().save(modelMission);
		
		return new ResponseEntity<>(MissionUtil.toRestMission(modelMission), HttpStatus.CREATED);
	
		
	}

	/**
	 * Find the mission with the given ID
	 * 
	 * @param id the ID to look for
	 * @return a response entity corresponding to the found mission and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no mission with the given ID exists
	 */
	@Override
	public ResponseEntity<RestMission> getMissionById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> getMissionById({})", id);
		
		Optional<Mission> modelMission = RepositoryService.getMissionRepository().findById(id);
		
		if (modelMission.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_MISSION_NOT_FOUND, id, MSG_ID_MISSION_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(MissionUtil.toRestMission(modelMission.get()), HttpStatus.OK);
	}

	/**
	 * Update the mission with the given ID with the attribute values of the given Json object. 
	 * @param id the ID of the product to update
	 * @param product a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the mission after modification (with ID and version for all 
	 * 		   contained objects) and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no mission with the given ID exists
	 */
	@Override
	public ResponseEntity<RestMission> modifyMission(Long id, @Valid RestMission mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyMission({})", id);
		
		Optional<Mission> optModelMission = RepositoryService.getMissionRepository().findById(id);
		
		if (optModelMission.isEmpty()) {
			String message = String.format(MSG_PREFIX + MSG_MISSION_NOT_FOUND, id, MSG_ID_MISSION_NOT_FOUND);
			logger.error(message);
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.set(HTTP_HEADER_WARNING, message);
			return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
		}
		Mission modelMission = optModelMission.get();
		
		// Update modified attributes
		boolean missionChanged = false;
		Mission changedMission = MissionUtil.toModelMission(mission);
		
		if (!modelMission.getCode().equals(changedMission.getCode())) {
			missionChanged = true;
			modelMission.setCode(changedMission.getCode());
		}
		
		if (!modelMission.getName().equals(changedMission.getName())) {
			missionChanged = true;
			modelMission.setName(changedMission.getName());
		}
		// Save mission only if anything was actually changed
		if (missionChanged)	{
			modelMission.incrementVersion();
			modelMission = RepositoryService.getMissionRepository().save(modelMission);
		}
		
		return new ResponseEntity<>(MissionUtil.toRestMission(modelMission), HttpStatus.OK);
	
	}

	
	/**
	 * Delete a mission by ID
	 * 
	 * @param the ID of the mission to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, "NOT_FOUND", if the product did not
	 *         exist, or "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteMissionById(Long id) {
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
