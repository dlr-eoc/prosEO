/**
 * MissionControllerImpl.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import de.dlr.proseo.model.rest.MissionController;
import de.dlr.proseo.model.rest.model.RestMission;
import de.dlr.proseo.model.rest.model.RestSpacecraft;
import de.dlr.proseo.ordermgr.OrdermgrConfiguration;
import de.dlr.proseo.ordermgr.rest.model.MissionUtil;
import de.dlr.proseo.model.Configuration;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProcessingOrder;
import de.dlr.proseo.model.Processor;
import de.dlr.proseo.model.ProcessorClass;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.ProductQuery;
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
	private static final int MSG_ID_DELETE_PRODUCTS_WITHOUT_FORCE = 1005;
	private static final int MSG_ID_PRODUCTS_EXIST = 1006;
	private static final int MSG_ID_PRODUCTCLASSES_EXIST = 1007;
	private static final int MSG_ID_PROCESSORCLASSES_EXIST = 1008;
	private static final int MSG_ID_DELETING_PRODUCT_FILES = 1009;

	/* Message string constants */
	private static final String MSG_NO_MISSIONS_FOUND = "(E%d) No missions found";
	private static final String MSG_MISSION_NOT_FOUND = "(E%d) No mission found for ID %d";
	private static final String MSG_DELETION_UNSUCCESSFUL = "(E%d) Mission deletion unsuccessful for ID %d";
	private static final String MSG_DELETE_PRODUCTS_WITHOUT_FORCE = "(E%d) Option 'delete-products' not valid without option 'force'";
	private static final String MSG_PRODUCTS_EXIST = "(E%d) Cannot delete mission %s due to existing products";
	private static final String MSG_PRODUCTCLASSES_EXIST = "(E%d) Cannot delete mission %s due to existing product classes";
	private static final String MSG_PROCESSORCLASSES_EXIST = "(E%d) Cannot delete mission %s due to existing processor classes";
	private static final String MSG_DELETING_PRODUCT_FILES = "(I%d) Deleting product files for product with database ID %d";
	
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String MSG_PREFIX = "199 proseo-ordermgr-missioncontroller ";
	
	private static final String URL_INGESTOR_FILE_DELETE = "/ingest/%s/%d";

	/** The Order Manager configuration */
	@Autowired
	OrdermgrConfiguration orderManagerConfig;

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
	 * @param id the ID of the mission to delete
	 * @param force flag whether to also delete all configured items (but not products)
	 * @param deleteProducts flag whether to also delete all stored products (also from all processing faciliy, requires "force")
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
			transactionTemplate.execute((status) -> {
				// Test whether the mission id is valid
				Optional<de.dlr.proseo.model.Mission> modelMission = RepositoryService.getMissionRepository().findById(id);
				if (modelMission.isEmpty()) {
					throw new NoResultException(String.format(MSG_MISSION_NOT_FOUND, MSG_ID_MISSION_NOT_FOUND, id));
				}
				
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
							throw new IllegalArgumentException(String.format(MSG_PRODUCTS_EXIST, MSG_ID_PRODUCTS_EXIST, modelMission.get().getCode()));
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
							throw new IllegalArgumentException(String.format(MSG_PRODUCTCLASSES_EXIST, MSG_ID_PRODUCTCLASSES_EXIST, modelMission.get().getCode()));
						}
					}
					// Check processor classes (processors, configurations etc. are configured with reference to processor classes)
					jpqlQuery = "select count(prc) from ProcessorClass prc where mission.id = :missionId";
					query = em.createQuery(jpqlQuery);
					query.setParameter("missionId", id);
					result = query.getSingleResult();
					if (result instanceof Long) {
						if (0 != ((Long) result)) {
							throw new IllegalArgumentException(String.format(MSG_PROCESSORCLASSES_EXIST, MSG_ID_PROCESSORCLASSES_EXIST,  modelMission.get().getCode()));
						}
					}
				}

				// Delete the mission
				if (force) {
					deleteMissionDependentObjects(modelMission.get());
				}
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

		// Delete all products and product files (by cascade; also remove product files from Storage Manager)
		deleteProductFiles(mission);
		
		String jpqlQuery = "select p from Product p where p.productClass.mission.id = " + missionId;
		Query query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Product) {
				Product product = (Product) resultObject;
				for (ProductQuery satisfiedQuery: product.getSatisfiedProductQueries()) {
					satisfiedQuery.getSatisfyingProducts().remove(product);
				}
				RepositoryService.getProductRepository().deleteById(((Product) resultObject).getId());
			}
		}
		
		// Delete all processing orders, jobs, job steps and product queries (by cascade)
		jpqlQuery = "select po from ProcessingOrder po where po.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof ProcessingOrder)
				RepositoryService.getOrderRepository().deleteById(((ProcessingOrder) resultObject).getId());
		}
		
		// Delete all product classes and selection rules (by cascade)
		jpqlQuery = "select pc from ProductClass pc where pc.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof ProductClass)
				RepositoryService.getProductClassRepository().deleteById(((ProductClass) resultObject).getId());
		}
		
		// Delete all configured processors
		jpqlQuery = "select cp from ConfiguredProcessor cp where cp.processor.processorClass.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof ConfiguredProcessor)
				RepositoryService.getConfiguredProcessorRepository().deleteById(((ConfiguredProcessor) resultObject).getId());
		}
		
		// Delete all processors and tasks (by cascade)
		jpqlQuery = "select p from Processor p where p.processorClass.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Processor)
				RepositoryService.getProcessorRepository().deleteById(((Processor) resultObject).getId());
		}
		
		// Delete all configurations and configuration input files (by cascade)
		jpqlQuery = "select c from Configuration c where c.processorClass.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Configuration)
				RepositoryService.getConfigurationRepository().deleteById(((Configuration) resultObject).getId());
		}
		
		// Delete all processor classes
		jpqlQuery = "select pc from ProcessorClass pc where pc.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof ProcessorClass)
				RepositoryService.getProcessorClassRepository().deleteById(((ProcessorClass) resultObject).getId());
		}
		
		// Delete all spacecrafts and orbits (by cascade)
		jpqlQuery = "select s from Spacecraft s where s.mission.id = " + missionId;
		query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Spacecraft)
				RepositoryService.getSpacecraftRepository().deleteById(((Spacecraft) resultObject).getId());
		}
		
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
		
		// Delete all users for this mission (bulk delete using Native SQL, since we have no implicit joins, and the User object is not mapped)
		sqlQuery = "delete from authorities where username like '" + mission.getCode() + "-%'";
		query = em.createNativeQuery(sqlQuery);
		query.executeUpdate();
		sqlQuery = "delete from users where username like '" + mission.getCode() + "-%'";
		query = em.createNativeQuery(sqlQuery);
		query.executeUpdate();
		
		// Delete all groups for this mission (bulk delete using Native SQL, since we have no implicit joins, and the Group object is not mapped)
		sqlQuery = "delete from group_authorities where group_id in (select id from groups where group_name like '" + mission.getCode() + "-%')";
		query = em.createNativeQuery(sqlQuery);
		query.executeUpdate();
		sqlQuery = "delete from groups where group_name like '" + mission.getCode() + "-%'";
		query = em.createNativeQuery(sqlQuery);
		query.executeUpdate();
		
	}

	/**
	 * Delete all product files for the given mission from their respective processing facilities
	 * Caution: This 
	 * @param mission the mission
	 */
	private void deleteProductFiles(Mission mission) {
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductFiles({})", mission.getCode());
		
		// Find all products for this mission with product files
		String jpqlQuery = "select p from Product p where p.productClass.mission.id = " + mission.getId() + " and p.productFile is not empty";
		Query query = em.createQuery(jpqlQuery);
		for (Object resultObject: query.getResultList()) {
			if (resultObject instanceof Product) {
				// Find all processing facilities, where this product is stored
				Product product = ((Product) resultObject);
				logger.info(String.format(MSG_DELETING_PRODUCT_FILES, MSG_ID_DELETING_PRODUCT_FILES, product.getId()));
				
				Set<String> processingFacilities = new HashSet<>();
				for (ProductFile productFile: ((Product) resultObject).getProductFile() ) {
					processingFacilities.add(productFile.getProcessingFacility().getName());
				}
				
				// Call Ingestor to remove product files at the found processing facilities from Storage Manager
				// Note: This action runs in an external transaction and cannot be rolled back!
				// Note 2: The external transaction modifies the list of product files for the product,
				//         therefore we have two loops to avoid accessing a potentially invalid object collection!
				for (String processingFacility: processingFacilities){
					URI ingestorUrl = URI.create(orderManagerConfig.getIngestorUrl() + String.format(URL_INGESTOR_FILE_DELETE,
							UriUtils.encodePathSegment(processingFacility, Charset.defaultCharset()), product.getId()));
					
					RestTemplate restTemplate = rtb
							.setConnectTimeout(Duration.ofMillis(orderManagerConfig.getIngestorTimeout()))
							.basicAuthentication(mission.getCode() + "-" + orderManagerConfig.getIngestorUser(), orderManagerConfig.getIngestorPassword())
							.build();
					restTemplate.delete(ingestorUrl);
					
				}
			}
		}

	}
}
