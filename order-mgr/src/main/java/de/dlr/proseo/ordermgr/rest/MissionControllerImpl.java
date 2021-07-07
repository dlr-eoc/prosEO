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

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.Valid;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import de.dlr.proseo.model.rest.MissionController;
import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.model.rest.model.RestPayload;
import de.dlr.proseo.model.rest.model.RestSpacecraft;
import de.dlr.proseo.ordermgr.OrdermgrConfiguration;
import de.dlr.proseo.ordermgr.rest.model.MissionUtil;
import de.dlr.proseo.model.Configuration;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Payload;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Spacecraft;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;

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
	private static final int MSG_ID_DELETE_PRODUCTS_WITHOUT_FORCE = 1005;
	private static final int MSG_ID_PRODUCTS_EXIST = 1006;
	private static final int MSG_ID_PRODUCTCLASSES_EXIST = 1007;
	private static final int MSG_ID_PROCESSORCLASSES_EXIST = 1008;
	private static final int MSG_ID_MISSION_DELETED = 1010;
	private static final int MSG_ID_MISSION_UPDATED = 1011;
	private static final int MSG_ID_MISSION_RETRIEVED = 1012;
	private static final int MSG_ID_MISSION_CREATED = 1013;
	private static final int MSG_ID_MISSIONS_RETRIEVED = 1014;
	private static final int MSG_ID_MISSION_EXISTS = 1015;
	private static final int MSG_ID_SPACECRAFT_EXISTS = 1016;
	private static final int MSG_ID_MISSION_CODE_MISSING = 1017;
	private static final int MSG_ID_MISSION_NOT_MODIFIED = 1018;

	// Same as in other services
	private static final int MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS = 2028;

	/* Message string constants */
	private static final String MSG_NO_MISSIONS_FOUND = "(E%d) No missions found";
	private static final String MSG_MISSION_NOT_FOUND = "(E%d) No mission found for ID %d";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Mission deletion unsuccessful for ID %d";
	private static final String MSG_DELETE_PRODUCTS_WITHOUT_FORCE = "(E%d) Option 'delete-products' not valid without option 'force'";
	private static final String MSG_PRODUCTS_EXIST = "(E%d) Cannot delete mission %s due to existing products";
	private static final String MSG_PRODUCTCLASSES_EXIST = "(E%d) Cannot delete mission %s due to existing product classes";
	private static final String MSG_PROCESSORCLASSES_EXIST = "(E%d) Cannot delete mission %s due to existing processor classes";
	private static final String MSG_MISSION_EXISTS = "(E%d) Mission with mission code %s already exists";
	private static final String MSG_SPACECRAFT_EXISTS = "(E%d) Spacecraft with spacecraft code %s already exists for mission %s";
	private static final String MSG_MISSION_CODE_MISSING = "(E%d) No mission code given";

	private static final String MSG_MISSION_DELETED = "(I%d) Mission with database ID %d deleted";
	private static final String MSG_MISSION_UPDATED = "(I%d) Mission %s updated";
	private static final String MSG_MISSION_RETRIEVED = "(I%d) Mission %s retrieved";
	private static final String MSG_MISSION_CREATED = "(I%d) Mission %s created";
	private static final String MSG_MISSIONS_RETRIEVED = "(I%d) All missions retrieved";
	private static final String MSG_MISSION_NOT_MODIFIED = "(I%d) Mission with id %d not modified (no changes)";

	// Same as in other services
	private static final String MSG_ILLEGAL_CROSS_MISSION_ACCESS = "(E%d) Illegal cross-mission access to mission %s (logged in to %s)";
	
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ordermgr-missioncontroller ";
	
	/** The Order Manager configuration */
	@Autowired
	OrdermgrConfiguration orderManagerConfig;

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

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
	public ResponseEntity<List<RestMission>> getMissions(String code) {
		if (logger.isTraceEnabled()) logger.trace(">>> getMissions");

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		List<RestMission> result = null;
		try {
			result = transactionTemplate.execute((status) -> {
				List<RestMission> resultList = new ArrayList<>();
				if (code != null) {
					Mission mission = RepositoryService.getMissionRepository().findByCode(code);
					if (mission == null) {
						throw new NoResultException(String.format(MSG_MISSION_NOT_FOUND, MSG_ID_MISSION_NOT_FOUND, code));
					}

					if (logger.isDebugEnabled()) logger.debug("Found mission with ID {}", mission.getId());
					RestMission resultMission = MissionUtil.toRestMission(mission);
					if (logger.isDebugEnabled()) logger.debug("Created result mission with ID {}", resultMission.getId());
					resultList.add(resultMission);					
				} else {
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

		logger.info(String.format(MSG_MISSIONS_RETRIEVED, MSG_ID_MISSIONS_RETRIEVED));
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	/**
	 * Create a mission from the given Json object 
	 * 
	 * @param mission the Json object to create the mission from
	 * @return a response containing a Json object corresponding to the mission after persistence (with ID and version for all 
	 * 		   contained objects) and HTTP status "CREATED"
	 * @throws IllegalArgumentException if any of the input data is invalid
	 */
	@Override
	public ResponseEntity<RestMission> createMission(@Valid RestMission mission) throws IllegalArgumentException {
		if (logger.isTraceEnabled()) logger.trace(">>> createMission({})", mission.getCode());
		
		// Check valid mission code
		if (null == mission.getCode() || mission.getCode().isBlank()) {
			throw new IllegalArgumentException(String.format(MSG_MISSION_CODE_MISSING, MSG_ID_MISSION_CODE_MISSING, mission.getCode()));
		}

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		RestMission restMission = null;
		try {
			restMission = transactionTemplate.execute((status) -> {
				Mission modelMission = MissionUtil.toModelMission(mission);

				// Check, whether mission exists already
				if (null != RepositoryService.getMissionRepository().findByCode(mission.getCode())) {
					throw new IllegalArgumentException(String.format(MSG_MISSION_EXISTS, MSG_ID_MISSION_EXISTS, mission.getCode()));
				}

				modelMission = RepositoryService.getMissionRepository().save(modelMission);

				// Add spacecraft details
				modelMission.getSpacecrafts().clear();
				for (RestSpacecraft restSpacecraft : mission.getSpacecrafts()) {
					Spacecraft modelSpacecraft = new Spacecraft();
					if (null != RepositoryService.getSpacecraftRepository().findByMissionAndCode(mission.getCode(), restSpacecraft.getCode())) {
						throw new IllegalArgumentException(String.format(MSG_SPACECRAFT_EXISTS, MSG_ID_SPACECRAFT_EXISTS, 
								restSpacecraft.getCode(), mission.getCode()));
					}

					modelSpacecraft.setCode(restSpacecraft.getCode());
					modelSpacecraft.setName(restSpacecraft.getName());
					modelSpacecraft.setMission(modelMission);
					if (null != restSpacecraft.getPayloads()) {
						for (RestPayload restPayload : restSpacecraft.getPayloads()) {
							Payload modelPayload = new Payload();
							modelPayload.setName(restPayload.getName());
							modelPayload.setDescription(restPayload.getDescription());
							modelSpacecraft.getPayloads().add(modelPayload);
						}
					}
					
					modelSpacecraft = RepositoryService.getSpacecraftRepository().save(modelSpacecraft);
					modelMission.getSpacecrafts().add(modelSpacecraft);					
				}		
				modelMission = RepositoryService.getMissionRepository().save(modelMission);

				return MissionUtil.toRestMission(modelMission);
			});
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		logger.info(String.format(MSG_MISSION_CREATED, MSG_ID_MISSION_CREATED, restMission.getCode()));
		
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

		logger.info(String.format(MSG_MISSION_RETRIEVED, MSG_ID_MISSION_RETRIEVED, restMission.getCode()));
		
		return new ResponseEntity<>(restMission, HttpStatus.OK);
	}


	/**
	 * Update the mission with the given ID with the attribute values of the given Json object. 
	 * @param id the ID of the mission to update
	 * @param mission a Json object containing the modified (and unmodified) attributes
	 * @return a response containing a Json object corresponding to the mission after modification (with ID and version for all 
	 * 		   contained objects) and 
	 *         HTTP status "OK" or 
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was attempted, or
	 * 		   HTTP status "NOT_FOUND" and an error message, if no mission with the given ID exists
	 */
	@Override
	public ResponseEntity<RestMission> modifyMission(Long id, @Valid RestMission mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> modifyMission({})", id);

		// Ensure user is authorized for the mission to update
		if (!securityService.isAuthorizedForMission(mission.getCode())) {
			String message = String.format(MSG_ILLEGAL_CROSS_MISSION_ACCESS, MSG_ID_ILLEGAL_CROSS_MISSION_ACCESS,
					mission.getCode(), securityService.getMission());			
			logger.error(message);
			return new ResponseEntity<>(errorHeaders(message), HttpStatus.FORBIDDEN);
		}
		
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
					missionChanged = true;
					modelMission.getFileClasses().clear();
					modelMission.getFileClasses().addAll(changedMission.getFileClasses());
				}
				if (!modelMission.getProcessingModes().equals(changedMission.getProcessingModes())) {
					missionChanged = true;
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
								spacecraftChanged = updatePayloads(modelSpacecraft.getPayloads(), restSpacecraft.getPayloads());
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
						
						if (null != restSpacecraft.getPayloads()) {
							for (RestPayload restPayload : restSpacecraft.getPayloads()) {
								Payload modelPayload = new Payload();
								modelPayload.setName(restPayload.getName());
								modelPayload.setDescription(restPayload.getDescription());
								modelSpacecraft.getPayloads().add(modelPayload);
							}
						}
						
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

		HttpStatus httpStatus = HttpStatus.OK;
		if (mission.getVersion() == restMission.getVersion()) {
			httpStatus = HttpStatus.NOT_MODIFIED;
			logger.info(String.format(MSG_MISSION_NOT_MODIFIED, MSG_ID_MISSION_NOT_MODIFIED, id));
		} else {
			logger.info(String.format(MSG_MISSION_UPDATED, MSG_ID_MISSION_UPDATED, restMission.getCode()));
		}
		
		return new ResponseEntity<>(restMission, httpStatus);

	}


	/**
	 * Update a spacecraft's list of payloads from the REST spacecraft's list of payloads
	 * 
	 * @param modelPayloads the spacecraft's list of payloads
	 * @param restPayloads the REST spacecraft's list of payloads
	 * @return true, if any of the payloads was changed, false otherwise
	 */
	private boolean updatePayloads(List<Payload> modelPayloads, @Valid List<RestPayload> restPayloads) {
		boolean spacecraftChanged = false;
		if (null == restPayloads) {
			if (!modelPayloads.isEmpty()) {
				spacecraftChanged = true;
				modelPayloads.clear();
			}
		} else {
			List<Payload> newPayloads = new ArrayList<>();
			for (RestPayload restPayload : restPayloads) {
				Payload modelPayload = new Payload();
				modelPayload.setName(restPayload.getName());
				modelPayload.setDescription(restPayload.getDescription());
				newPayloads.add(modelPayload);
			}
			if (!modelPayloads.equals(newPayloads)) {
				spacecraftChanged = true;
				modelPayloads.clear();
				modelPayloads.addAll(newPayloads);
			}
		}
		return spacecraftChanged;
	}

	/**
	 * Delete a mission by ID
	 * 
	 * @param id the ID of the mission to delete
	 * @param force flag whether to also delete all configured items (but not products)
	 * @param deleteProducts flag whether to also delete all stored products (also from all processing facilities, requires "force")
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful,
	 *      "BAD_REQUEST", if "deleteProducts" was specified without "force" or if dependent objects exist for the mission,
	 * 		"NOT_FOUND", if the mission did not exist, or
	 * 		"NOT_MODIFIED", if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteMissionById(Long id, Boolean force, Boolean deleteProducts) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteMissionById({})", id);

		TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);

		try {
			// Transaction to check the delete preconditions
			transactionTemplate.execute((status) -> {
				// Test whether the mission id is valid
				Optional<de.dlr.proseo.model.Mission> modelMission = RepositoryService.getMissionRepository().findById(id);
				if (modelMission.isEmpty()) {
					throw new NoResultException(String.format(MSG_MISSION_NOT_FOUND, MSG_ID_MISSION_NOT_FOUND, id));
				}
				Mission mission = modelMission.get();
				
				// Check execution options
				if (deleteProducts && !force) {
					throw new IllegalArgumentException(String.format(MSG_DELETE_PRODUCTS_WITHOUT_FORCE, MSG_ID_DELETE_PRODUCTS_WITHOUT_FORCE));
				}
				
				// Do not delete missions with products without "deleteProducts"
				if (!deleteProducts) {
					String jpqlQuery = "select count(p) from Product p where productClass.mission.id = :missionId";
					Query query = em.createQuery(jpqlQuery);
					query.setParameter("missionId", id);
					Object result = query.getSingleResult();
					if (result instanceof Long) {
						if (0 != ((Long) result)) {
							throw new IllegalArgumentException(String.format(MSG_PRODUCTS_EXIST, MSG_ID_PRODUCTS_EXIST, mission.getCode()));
						}
					}
				}
				
				// Do not delete missions with dependent configured elements without "force")
				if (!force) {
					// Check product classes (selection rules etc. are configured with reference to product classes)
					String jpqlQuery = "select count(pdc) from ProductClass pdc where mission.id = :missionId";
					Query query = em.createQuery(jpqlQuery);
					query.setParameter("missionId", id);
					Object result = query.getSingleResult();
					if (result instanceof Long) {
						if (0 != ((Long) result)) {
							throw new IllegalArgumentException(String.format(MSG_PRODUCTCLASSES_EXIST, MSG_ID_PRODUCTCLASSES_EXIST, mission.getCode()));
						}
					}
					// Check processor classes (processors, configurations etc. are configured with reference to processor classes)
					jpqlQuery = "select count(prc) from ProcessorClass prc where mission.id = :missionId";
					query = em.createQuery(jpqlQuery);
					query.setParameter("missionId", id);
					result = query.getSingleResult();
					if (result instanceof Long) {
						if (0 != ((Long) result)) {
							throw new IllegalArgumentException(String.format(MSG_PROCESSORCLASSES_EXIST, MSG_ID_PROCESSORCLASSES_EXIST,  mission.getCode()));
						}
					}
				}

				// Delete the mission
				if (force) {
					deleteMissionDependentObjects(mission);
				}
				RepositoryService.getMissionRepository().deleteById(id);

				// Test whether the deletion was successful
				if (!RepositoryService.getMissionRepository().findById(id).isEmpty()) {
					throw new RuntimeException(String.format(MSG_DELETION_UNSUCCESSFUL, MSG_ID_DELETION_UNSUCCESSFUL, id));
				}

				return null;
			});
		} catch (NoResultException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (TransactionException e) {
			logger.error(e.getMessage());
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (RuntimeException e) {
			logger.error(e.getMessage(), e);
			return new ResponseEntity<>(errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}

		logger.info(String.format(MSG_MISSION_DELETED, MSG_ID_MISSION_DELETED, id));
		
		HttpHeaders responseHeaders = new HttpHeaders();
		return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
	}

	/**
	 * Delete a mission and all (!) its dependent elements.
	 * Note that most deletes are performed in a loop, since JPA discourages bulk deletes (JPA Spec 2.0, sec. 4.10) and Hibernate
	 * does not handle implicit joins in delete statements correctly (although syntactically correct).
	 * 
	 * @param missionId the database ID of the mission
	 */
	private void deleteMissionDependentObjects(Mission mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteMissionDependentObjects({})", mission.getCode());
		
		long missionId = mission.getId();
		
		// Delete all products
		String jpqlQuery = "select p from Product p where p.productClass.mission.id = " + missionId;
		Query query = em.createQuery(jpqlQuery);
		List<Long> deletedProductIds = new ArrayList<>();
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Product) {
				deleteProduct((Product) resultObject, deletedProductIds);
			}
		}
		if (logger.isDebugEnabled()) logger.debug("... products deleted");
		
		// Delete all processing orders, jobs, job steps and product queries (by cascade)
		jpqlQuery = "select po from ProcessingOrder po where po.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof ProcessingOrder)
				RepositoryService.getOrderRepository().deleteById(((ProcessingOrder) resultObject).getId());
		}
		if (logger.isDebugEnabled()) logger.debug("... processing orders deleted");
		
		// Delete all product classes and selection rules (by cascade)
		jpqlQuery = "select pc from ProductClass pc where pc.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof ProductClass)
				RepositoryService.getProductClassRepository().deleteById(((ProductClass) resultObject).getId());
		}
		if (logger.isDebugEnabled()) logger.debug("... product classes deleted");
		
		// Delete all configured processors
		jpqlQuery = "select cp from ConfiguredProcessor cp where cp.processor.processorClass.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof ConfiguredProcessor)
				RepositoryService.getConfiguredProcessorRepository().deleteById(((ConfiguredProcessor) resultObject).getId());
		}
		if (logger.isDebugEnabled()) logger.debug("... configured processors deleted");
		
		// Delete all processors and tasks (by cascade)
		jpqlQuery = "select p from Processor p where p.processorClass.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Processor)
				RepositoryService.getProcessorRepository().deleteById(((Processor) resultObject).getId());
		}
		if (logger.isDebugEnabled()) logger.debug("... processors deleted");
		
		// Delete all configurations and configuration input files (by cascade)
		jpqlQuery = "select c from Configuration c where c.processorClass.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Configuration)
				RepositoryService.getConfigurationRepository().deleteById(((Configuration) resultObject).getId());
		}
		if (logger.isDebugEnabled()) logger.debug("... configurations deleted");
		
		// Delete all processor classes
		jpqlQuery = "select pc from ProcessorClass pc where pc.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof ProcessorClass)
				RepositoryService.getProcessorClassRepository().deleteById(((ProcessorClass) resultObject).getId());
		}
		if (logger.isDebugEnabled()) logger.debug("... processor classes deleted");
		
		// Delete all spacecrafts and orbits (by cascade)
		jpqlQuery = "select s from Spacecraft s where s.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Spacecraft)
				RepositoryService.getSpacecraftRepository().deleteById(((Spacecraft) resultObject).getId());
		}
		if (logger.isDebugEnabled()) logger.debug("... spacecrafts deleted");
		
		// Delete all user-group associations for this mission (bulk delete using Native SQL, since we have no implicit joins, and the User object is not mapped)
		String sqlQuery = "delete from users_group_memberships where users_username like '" + mission.getCode() + "-%'";
		query = em.createNativeQuery(sqlQuery);
		query.executeUpdate();
		sqlQuery = "delete from groups_group_members where groups_id in (select id from groups where group_name like '" + mission.getCode() + "-%')";
		query = em.createNativeQuery(sqlQuery);
		query.executeUpdate();
		sqlQuery = "delete from group_members where username like '" + mission.getCode() + "-%'";
		query = em.createNativeQuery(sqlQuery);
		query.executeUpdate();
		if (logger.isDebugEnabled()) logger.debug("... group memberships deleted");
		
		// Delete all groups for this mission (bulk delete using Native SQL, since we have no implicit joins, and the Group object is not mapped)
		sqlQuery = "delete from group_authorities where group_id in (select id from groups where group_name like '" + mission.getCode() + "-%')";
		query = em.createNativeQuery(sqlQuery);
		query.executeUpdate();
		sqlQuery = "delete from groups where group_name like '" + mission.getCode() + "-%'";
		query = em.createNativeQuery(sqlQuery);
		query.executeUpdate();
		if (logger.isDebugEnabled()) logger.debug("... groups deleted");
		
		// Delete all users for this mission (bulk delete using Native SQL, since we have no implicit joins, and the User object is not mapped)
		sqlQuery = "delete from authorities where username like '" + mission.getCode() + "-%'";
		query = em.createNativeQuery(sqlQuery);
		query.executeUpdate();
		sqlQuery = "delete from users where username like '" + mission.getCode() + "-%'";
		query = em.createNativeQuery(sqlQuery);
		query.executeUpdate();
		if (logger.isDebugEnabled()) logger.debug("... users deleted");
		
	}

	/**
	 * Delete product, after all component products have been deleted (this would have happened through the CASCADE annotation
	 * anyway, but in the bulk delete this is error-prone, therefore we control it programmatically); deletes all product files
	 * for this product (CASCADE)
	 * 
	 * @param product the product to delete
	 * @param deletedProductIds the products deleted so far (may contain the current product!!)
	 */
	private void deleteProduct(Product product, List<Long> deletedProductIds) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProduct({}, [...])", product.getId());
		
		if (deletedProductIds.contains(product.getId())) {
			return;
		}
		
		for (Product componentProduct: product.getComponentProducts()) {
			deleteProduct(componentProduct, deletedProductIds);
		}
		
		deletedProductIds.add(product.getId());
		
		RepositoryService.getProductRepository().deleteById(product.getId());
	}
}
