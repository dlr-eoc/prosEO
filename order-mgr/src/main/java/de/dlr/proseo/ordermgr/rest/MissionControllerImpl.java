/**
 * MissionControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.NoResultException;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.model.rest.MissionController;
import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.model.rest.model.RestSpacecraft;
import de.dlr.proseo.ordermgr.rest.model.MissionUtil;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Spacecraft;
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
	private static final int MSG_ID_NO_MISSIONS_FOUND = 1002;
	private static final int MSG_ID_DELETION_UNSUCCESSFUL = 1004;

	/* Message string constants */
	private static final String MSG_NO_MISSIONS_FOUND = "(E%d) No missions found";
	private static final String MSG_MISSION_NOT_FOUND = "(E%d) No mission found for ID %d";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Mission deletion unsuccessful for ID %d";
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ordermgr-missioncontroller ";


	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(MissionControllerImpl.class);

	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, MSG_PREFIX + message.replaceAll("\n", " "));
		return responseHeaders;
	}

	/**
	 * List of all missions with no search criteria

	 * @return a response entity with either a list of missions and HTTP status OK or an error message and an HTTP status indicating failure
	 */
	@Override
	public ResponseEntity<List<RestMission>> getMissions() {
		if (logger.isTraceEnabled()) logger.trace(">>> getMissions");

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		List<RestMission> result = null;
		try {
			result = transactionTemplate.execute((status) -> {
				List<RestMission> resultList = new ArrayList<>();

				List<Mission> modelMissions = RepositoryService.getMissionRepository().findAll();

				if(modelMissions.isEmpty()) {
					throw new NoResultException(String.format(MSG_NO_MISSIONS_FOUND, MSG_ID_NO_MISSIONS_FOUND));
				}

				// Simple case: no search criteria set
				for (Mission  mission: modelMissions) {
					if (logger.isDebugEnabled()) logger.debug("Found mission with ID {}", mission.getId());
					RestMission resultMission = MissionUtil.toRestMission(mission);
					if (logger.isDebugEnabled()) logger.debug("Created result mission with ID {}", resultMission.getId());
					resultList.add(resultMission);
				}
				
				return resultList;
			});
		} catch (NoResultException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
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

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		RestMission restMission = null;
		try {
			restMission = transactionTemplate.execute((status) -> {
				Mission modelMission = MissionUtil.toModelMission(mission);

				// The following does not make sense: ...getMission() cannot return the modelMission, as this is to be created!
				// Should any of the following succeed, it would mean that the mission already exists.
				//				modelMission.getProcessorClasses().clear();
				//				for (ProcessorClass procClass : RepositoryService.getProcessorClassRepository().findAll()) {			
				//					if(procClass.getMission().getCode().equals(modelMission.getCode())) {
				//						modelMission.getProcessorClasses().add(procClass);
				//					}		
				//				}
				//				
				//				modelMission.getProductClasses().clear();
				//				for (ProductClass prodClass : RepositoryService.getProductClassRepository().findByMissionCode(modelMission.getCode())) {
				//						modelMission.getProductClasses().add(prodClass);
				//				}
				// TODO throw exception, if mission exists

				modelMission = RepositoryService.getMissionRepository().save(modelMission);

				//Code to add spacecraft details
				modelMission.getSpacecrafts().clear();
				for (RestSpacecraft restSpacecraft : mission.getSpacecrafts()) {
					Spacecraft modelSpacecraft = new Spacecraft();
					if (null != RepositoryService.getSpacecraftRepository().findByCode(restSpacecraft.getCode())) {
						// does not make sense: if such a spacecraft would be found, it would already belong to a different mission!
						//						modelSpacecraft = RepositoryService.getSpacecraftRepository().findByCode(restSpacecraft.getCode());
						// TODO throw exception here
					}
					else {
						modelSpacecraft.setCode(restSpacecraft.getCode());
						modelSpacecraft.setName(restSpacecraft.getName());
						modelSpacecraft.setMission(modelMission);
						modelSpacecraft = RepositoryService.getSpacecraftRepository().save(modelSpacecraft);
					}

					modelMission.getSpacecrafts().add(modelSpacecraft);					
				}		
				modelMission = RepositoryService.getMissionRepository().save(modelMission);

				return MissionUtil.toRestMission(modelMission);
			});
		} catch (TransactionException e) {
			// TODO catch all exceptions created above
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}


		return new ResponseEntity<>(restMission, HttpStatus.CREATED);

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

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		RestMission restMission = null;
		try {
			restMission = transactionTemplate.execute((status) -> {
				Optional<Mission> modelMission = RepositoryService.getMissionRepository().findById(id);

				if (modelMission.isEmpty()) {
					throw new NoResultException(String.format(MSG_MISSION_NOT_FOUND, MSG_ID_MISSION_NOT_FOUND, id));
				}

				return MissionUtil.toRestMission(modelMission.get());
			});
		} catch (NoResultException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(restMission, HttpStatus.OK);
	}

	/**
	 * Update the mission with the given ID with the attribute values of the given Json object. 
	 * @param id the ID of the mission to update
	 * @param mission a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the mission after modification (with ID and version for all 
	 * 		   contained objects) and HTTP status "OK" or an error message and
	 * 		   HTTP status "NOT_FOUND", if no mission with the given ID exists
	 */
	@Override
	public ResponseEntity<RestMission> modifyMission(Long id, @Valid RestMission mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyMission({})", id);

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		RestMission restMission = null;

		try {
			restMission = transactionTemplate.execute((status) -> {
				Optional<Mission> optModelMission = RepositoryService.getMissionRepository().findById(id);

				if (optModelMission.isEmpty()) {
					throw new NoResultException(String.format(MSG_MISSION_NOT_FOUND, MSG_ID_MISSION_NOT_FOUND, id));
				}
				Mission modelMission = optModelMission.get();

				// Update modified attributes
				boolean missionChanged = false;

				Mission changedMission = MissionUtil.toModelMission(mission);

				// Never change the mission code

				if (!modelMission.getName().equals(changedMission.getName())) {
					missionChanged = true;
					modelMission.setName(changedMission.getName());
				}
				if (!modelMission.getProductFileTemplate().equals(changedMission.getProductFileTemplate())) {
					missionChanged = true;
					modelMission.setProductFileTemplate(changedMission.getProductFileTemplate());
				}
				if (!modelMission.getFileClasses().equals(changedMission.getFileClasses()))	{
					modelMission.getFileClasses().clear();
					modelMission.getFileClasses().addAll(changedMission.getFileClasses());
				}
				if (!modelMission.getProcessingModes().equals(changedMission.getProcessingModes())) {
					modelMission.getProcessingModes().clear();
					modelMission.getProcessingModes().addAll(changedMission.getProcessingModes());
				}

				/* Check for new and changed spacecrafts */
				Set<Spacecraft> newSpacecrafts = new HashSet<>();
				SPACECRAFT:
					for (RestSpacecraft restSpacecraft: mission.getSpacecrafts()) {
						// Look for corresponding spacecraft in modelMission
						for (Spacecraft modelSpacecraft: modelMission.getSpacecrafts()) {
							if (null != restSpacecraft.getId() && modelSpacecraft.getId() == restSpacecraft.getId().longValue() 
									|| modelSpacecraft.getCode().equals(restSpacecraft.getCode())) {
								// Spacecraft (possibly) changed)
								boolean spacecraftChanged = false;
								if (!modelSpacecraft.getCode().equals(restSpacecraft.getCode())) {
									spacecraftChanged = true;
									modelSpacecraft.setCode(restSpacecraft.getCode());
								}
								if (!modelSpacecraft.getName().equals(restSpacecraft.getName())) {
									spacecraftChanged = true;
									modelSpacecraft.setName(restSpacecraft.getName());
								}
								if (spacecraftChanged) {
									modelSpacecraft.incrementVersion();
									modelSpacecraft = RepositoryService.getSpacecraftRepository().save(modelSpacecraft);
								}
								newSpacecrafts.add(modelSpacecraft);
								continue SPACECRAFT; // check next restSpacecraft
							}
						}
						// New spacecraft
						Spacecraft modelSpacecraft = new Spacecraft();
						modelSpacecraft.setCode(restSpacecraft.getCode());
						modelSpacecraft.setName(restSpacecraft.getName());
						modelSpacecraft.setMission(modelMission);
						modelSpacecraft = RepositoryService.getSpacecraftRepository().save(modelSpacecraft);
						newSpacecrafts.add(modelSpacecraft);
						missionChanged = true;
					}
				/* Check for deleted spacecrafts */
				for (Spacecraft oldSpacecraft: modelMission.getSpacecrafts()) {
					if (!newSpacecrafts.contains(oldSpacecraft)) {
						// The spacecraft is not used any more for this mission
						missionChanged = true;
						RepositoryService.getSpacecraftRepository().delete(oldSpacecraft); // deletes all orbits by way of cascading
					}
				}
				modelMission.getSpacecrafts().clear();
				modelMission.getSpacecrafts().addAll(newSpacecrafts); // This may result in the same set of spacecrafts as before

				// Save mission only if anything was actually changed
				if (missionChanged)	{
					modelMission.incrementVersion();
					modelMission = RepositoryService.getMissionRepository().save(modelMission);
				}

				return MissionUtil.toRestMission(modelMission);
			});
		} catch (NoResultException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(restMission, HttpStatus.OK);

	}


	/**
	 * Delete a mission by ID
	 * 
	 * @param the ID of the mission to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, "NOT_FOUND", if the mission did not
	 *         exist, or "NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteMissionById(Long id) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteMissionById({})", id);

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		try {
			transactionTemplate.execute((status) -> {
				// Test whether the mission id is valid
				Optional<de.dlr.proseo.model.Mission> modelMission = RepositoryService.getMissionRepository().findById(id);
				if (modelMission.isEmpty()) {
					throw new NoResultException(String.format(MSG_MISSION_NOT_FOUND, MSG_ID_MISSION_NOT_FOUND, id));
				}

				// Delete the mission
				RepositoryService.getMissionRepository().deleteById(id);

				// Test whether the deletion was successful
				modelMission = RepositoryService.getMissionRepository().findById(id);
				if (!modelMission.isEmpty()) {
					throw new RuntimeException(String.format(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, id));
				}

				return null;
			});
		} catch (NoResultException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (RuntimeException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}

		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
	}
}
