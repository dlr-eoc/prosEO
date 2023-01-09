/**
 * WorkflowMgr.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProcessorMgrMessage;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.WorkflowOption;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.procmgr.rest.model.RestWorkflow;
import de.dlr.proseo.procmgr.rest.model.WorkflowUtil;

/**
 * Service methods required to create, modify and delete workflows in the prosEO
 * database, and to query the database about such workflows
 *
 * @author Katharina Bassler
 */
@Component
@Transactional
public class WorkflowMgr {

	/** Utility class for user authorizations */
	@Autowired
	private SecurityService securityService;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/** REST template builder */
	@Autowired
	RestTemplateBuilder rtb;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(WorkflowMgr.class);

	/**
	 * Create a new workflow
	 * 
	 * @param workflow a Json representation of the new workflow
	 * @return a Json representation of the workflow after creation (with ID and
	 *         version number)
	 * @throws IllegalArgumentException if any of the input data was invalid
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public RestWorkflow createWorkflow(RestWorkflow restWorkflow) throws IllegalArgumentException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createWorkflow({})", (null == restWorkflow ? "MISSING" : restWorkflow.getName()));

		if (null == restWorkflow) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.WORKFLOW_MISSING));
		}

		// Ensure the workflow name is set
		if (null == restWorkflow.getName() || restWorkflow.getName().isBlank()) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.WORKFLOW_NAME_MISSING));
		}

		// Ensure the mission is set and the user is authorized for the mission
		if (null == restWorkflow.getMissionCode()) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.MISSION_CODE_MISSING));
		} else if (!securityService.isAuthorizedForMission(restWorkflow.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					restWorkflow.getMissionCode(), securityService.getMission()));
		}

		// Ensure a workflow with the same mission, name and version, or the same
		// UUID, does not yet exist
		if (null != RepositoryService.getWorkflowRepository().findByMissionCodeAndWorkflowNameAndWorkflowVersion(
				restWorkflow.getMissionCode(), restWorkflow.getName(), restWorkflow.getWorkflowVersion())
				|| null != RepositoryService.getWorkflowRepository()
						.findByUuid(UUID.fromString(restWorkflow.getUuid()))) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.DUPLICATE_WORKFLOW, restWorkflow.getMissionCode(),
							restWorkflow.getName(), restWorkflow.getWorkflowVersion(), restWorkflow.getUuid()));
		}

		Workflow modelWorkflow = WorkflowUtil.toModelWorkflow(restWorkflow);

		// If no UUID was given, a random one is assigned
		if (null == restWorkflow.getUuid()) {
			modelWorkflow.setUuid(UUID.randomUUID());
		}

		// Workflow version is mandatory.
		if (null == restWorkflow.getWorkflowVersion()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow creation, workflow version"));
		}

		// The configured processor is mandatory and must exist in the repository.
		if (null == restWorkflow.getConfiguredProcessor()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow creation, configuredProcessor"));
		} else {
			modelWorkflow.setConfiguredProcessor(
					RepositoryService.getConfiguredProcessorRepository().findByMissionCodeAndIdentifier(
							restWorkflow.getMissionCode(), restWorkflow.getConfiguredProcessor()));
			if (null == modelWorkflow.getConfiguredProcessor()) {
				throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_MISSSPECIFIED,
						"configured processor", restWorkflow.getMissionCode(), restWorkflow.getConfiguredProcessor()));
			}
		}

		// The input product class is mandatory and must exist in the repository.
		if (null == restWorkflow.getInputProductClass()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow creation, inputProductClass"));
		} else {
			modelWorkflow
					.setInputProductClass(RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(
							restWorkflow.getMissionCode(), restWorkflow.getInputProductClass()));
			if (null == modelWorkflow.getInputProductClass()) {
				throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_MISSSPECIFIED,
						"input product class", restWorkflow.getMissionCode(), restWorkflow.getInputProductClass()));
			}
		}

		// The output product class is mandatory and must exist in the repository. The
		// specified configured processor must be able to produce the specified output
		// class.
		if (null == restWorkflow.getOutputProductClass()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow creation, outputProductClass"));
		} else if (!modelWorkflow.getConfiguredProcessor().getProcessor().getProcessorClass().getProductClasses()
				.contains(RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(
						restWorkflow.getMissionCode(), restWorkflow.getOutputProductClass()))) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_PRODUCT_MISMATCH));
		} else {
			modelWorkflow.setOutputProductClass(
					RepositoryService.getProductClassRepository().findByMissionCodeAndProductType(
							restWorkflow.getMissionCode(), restWorkflow.getOutputProductClass()));
			if (null == modelWorkflow.getOutputProductClass()) {
				throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_MISSSPECIFIED,
						"output product class", restWorkflow.getMissionCode(), restWorkflow.getOutputProductClass()));
			}
		}

		// If provided, workflowOptions must have the mandatory fields set.
		if (!modelWorkflow.getWorkflowOptions().isEmpty()) {
			for (WorkflowOption option : modelWorkflow.getWorkflowOptions()) {
				if (null == option.getName()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflowOption creation, option name"));
				}
				if (null == option.getType()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflowOption creation, option type"));
				}
				if (option.getValueRange().isEmpty()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflowOption creation, value range"));
				}
				if (null != option.getDefaultValue() && !option.getValueRange().contains(option.getDefaultValue())) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.RANGE_MUST_CONTAIN_DEFAULT,
							option.getDefaultValue(), option.getId()));
				}
			}
		}

		// The new workflow is saved to the repository.
		modelWorkflow = RepositoryService.getWorkflowRepository().save(modelWorkflow);

		// The workflow options are saved to the repository.
		if (!modelWorkflow.getWorkflowOptions().isEmpty()) {
			for (WorkflowOption option : modelWorkflow.getWorkflowOptions()) {
				RepositoryService.getWorkflowOptionRepository().save(option);
			}
		}

		logger.log(ProcessorMgrMessage.WORKFLOW_CREATED, modelWorkflow.getName(), modelWorkflow.getWorkflowVersion(),
				modelWorkflow.getConfiguredProcessor().getProcessor().getProcessorClass().getMission().getCode());

		return WorkflowUtil.toRestWorkflow(modelWorkflow);
	}

	/**
	 * Delete a workflow by ID
	 *
	 * @param id the ID of the workflow to delete
	 * @throws EntityNotFoundException  if the workflow to delete does not exist in
	 *                                  the database
	 * @throws RuntimeException         if the deletion was not performed as
	 *                                  expected
	 * @throws IllegalArgumentException if the ID of the workflow to delete was not
	 *                                  given, or if dependent objects exist
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public void deleteWorkflowById(Long id)
			throws EntityNotFoundException, SecurityException, IllegalArgumentException, RuntimeException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteWorkflowById({})", id);

		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.WORKFLOW_ID_MISSING));
		}

		// Test whether the workflow id is valid
		Optional<Workflow> modelWorkflow = RepositoryService.getWorkflowRepository().findById(id);
		if (modelWorkflow.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProcessorMgrMessage.WORKFLOW_ID_NOT_FOUND, id));
		}

		// Ensure user is authorized for the mission of the workflow
		if (!securityService.isAuthorizedForMission(modelWorkflow.get().getConfiguredProcessor().getProcessor()
				.getProcessorClass().getMission().getCode())) {
			throw new SecurityException(
					logger.log(
							GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, modelWorkflow.get().getConfiguredProcessor()
									.getProcessor().getProcessorClass().getMission().getCode(),
							securityService.getMission()));
		}

		// Delete workflow options depending on this workflow
		if (!modelWorkflow.get().getWorkflowOptions().isEmpty()) {
			for (WorkflowOption option : modelWorkflow.get().getWorkflowOptions()) {
				RepositoryService.getWorkflowOptionRepository().deleteById(option.getId());
			}
		}

		// Delete the workflow
		try {
			RepositoryService.getWorkflowRepository().deleteById(id);
		} catch (Exception e) {
			throw new RuntimeException(logger.log(ProcessorMgrMessage.DELETE_FAILURE, id, e.getMessage()));
		}

		// Test whether the deletion was successful
		modelWorkflow = RepositoryService.getWorkflowRepository().findById(id);
		if (!modelWorkflow.isEmpty()) {
			throw new RuntimeException(logger.log(ProcessorMgrMessage.DELETION_UNSUCCESSFUL, id));
		}

		logger.log(ProcessorMgrMessage.WORKFLOW_DELETED, id);
	}

	/**
	 * Get a workflow by ID
	 *
	 * @param id the workflow ID
	 * @return a Json object corresponding to the workflow found
	 * @throws IllegalArgumentException if no workflow ID was given
	 * @throws NoResultException        if no workflow with the given ID exists
	 * @throws SecurityException        if a cross-mission data access was attempted
	 */
	public RestWorkflow getWorkflowById(Long id) throws IllegalArgumentException, NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getWorkflowById({})", id);

		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.WORKFLOW_MISSING, id));
		}

		Optional<Workflow> modelWorkflow = RepositoryService.getWorkflowRepository().findById(id);

		if (modelWorkflow.isEmpty()) {
			throw new NoResultException(logger.log(ProcessorMgrMessage.WORKFLOW_ID_NOT_FOUND, id));
		}

		// Ensure user is authorized for the mission of the processor
		if (!securityService.isAuthorizedForMission(modelWorkflow.get().getConfiguredProcessor().getProcessor()
				.getProcessorClass().getMission().getCode())) {
			throw new SecurityException(
					logger.log(
							GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, modelWorkflow.get().getConfiguredProcessor()
									.getProcessor().getProcessorClass().getMission().getCode(),
							securityService.getMission()));
		}

		logger.log(ProcessorMgrMessage.WORKFLOW_RETRIEVED, id);

		return WorkflowUtil.toRestWorkflow(modelWorkflow.get());
	}

	/**
	 * Update a workflow by ID
	 * 
	 * @param id       the ID of the workflow to update
	 * @param workflow a Json object containing the modified (and unmodified)
	 *                 attributes
	 * @return a response containing a Json object corresponding to the workflow
	 *         after modification (with ID and version for all contained objects)
	 * @throws EntityNotFoundException         if no workflow with the given ID
	 *                                         exists
	 * @throws IllegalArgumentException        if any of the input data was invalid
	 * @throws SecurityException               if a cross-mission data access was
	 *                                         attempted
	 * @throws ConcurrentModificationException if the workflow has been modified
	 *                                         since retrieval by the client
	 */
	public RestWorkflow modifyWorkflow(Long id, RestWorkflow restWorkflow) throws EntityNotFoundException,
			IllegalArgumentException, SecurityException, ConcurrentModificationException {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyWorkflow({})", id);

		// Check arguments
		if (null == id || 0 == id) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.WORKFLOW_ID_MISSING));
		}
		if (null == restWorkflow) {
			throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.WORKFLOW_DATA_MISSING));
		}

		// Ensure user is authorized for the mission of the workflow
		if (!securityService.isAuthorizedForMission(restWorkflow.getMissionCode())) {
			throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS,
					restWorkflow.getMissionCode(), securityService.getMission()));
		}

		// Ensure workflow to be modified exists
		Optional<Workflow> optWorkflow = RepositoryService.getWorkflowRepository().findById(id);
		if (optWorkflow.isEmpty()) {
			throw new EntityNotFoundException(logger.log(ProcessorMgrMessage.WORKFLOW_ID_NOT_FOUND, id));
		}
		Workflow modelWorkflow = optWorkflow.get();

		// Ensure user is allowed to change the workflow (no intermediate update)
		if (modelWorkflow.getVersion() != restWorkflow.getVersion().intValue()) {
			throw new ConcurrentModificationException(logger.log(ProcessorMgrMessage.CONCURRENT_WORKFLOW_UPDATE, id));
		}

		boolean workflowChanged = false;
		boolean workflowOptionsChanged = false;

		String missionCode = modelWorkflow.getConfiguredProcessor().getProcessor().getProcessorClass().getMission()
				.getCode();

		// Name may not be null
		if (null == restWorkflow.getName()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow modification, name"));
		} else if (!modelWorkflow.getName().equals(restWorkflow.getName())) {
			workflowChanged = true;
			modelWorkflow.setName(restWorkflow.getName());
		}

		// Workflow version may not be null
		if (null == restWorkflow.getWorkflowVersion()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow modification, workflow version"));
		} else if (!modelWorkflow.getWorkflowVersion().equals(restWorkflow.getWorkflowVersion())) {
			workflowChanged = true;
			modelWorkflow.setWorkflowVersion(restWorkflow.getWorkflowVersion());
		}

		// Configured processor may not be null and must exist
		if (null == restWorkflow.getConfiguredProcessor()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow modification, configuredProcessor"));
		} else if (null != restWorkflow.getConfiguredProcessor()
				&& modelWorkflow.getConfiguredProcessor().getIdentifier() != restWorkflow.getConfiguredProcessor()) {
			workflowChanged = true;

			ConfiguredProcessor newConfiguredProcessor = RepositoryService.getConfiguredProcessorRepository()
					.findByMissionCodeAndIdentifier(restWorkflow.getMissionCode(),
							restWorkflow.getConfiguredProcessor());

			if (null == newConfiguredProcessor) {
				throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_MISSSPECIFIED,
						"configured processor", restWorkflow.getMissionCode(), restWorkflow.getConfiguredProcessor()));
			}
			modelWorkflow.setConfiguredProcessor(newConfiguredProcessor);
		}

		// Input product class may not be null and must exist
		if (null == restWorkflow.getInputProductClass()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow modification, inputProductClass"));
		} else if (null != restWorkflow.getInputProductClass()
				&& !modelWorkflow.getInputProductClass().getProductType().equals(restWorkflow.getInputProductClass())) {
			workflowChanged = true;

			ProductClass newInputProductClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(missionCode, restWorkflow.getInputProductClass());

			if (null == newInputProductClass) {
				throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_MISSSPECIFIED,
						"input product class", restWorkflow.getMissionCode(), restWorkflow.getInputProductClass()));
			}
			modelWorkflow.setInputProductClass(newInputProductClass);
		}

		// Output product class may not be null, must exist, and must be produced by the
		// configured processor
		if (null == restWorkflow.getOutputProductClass()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow creation, outputProductClass"));
		} else if (null != restWorkflow.getOutputProductClass() && !modelWorkflow.getOutputProductClass()
				.getProductType().equals(restWorkflow.getOutputProductClass())) {
			workflowChanged = true;
			ProductClass newOutputProductClass = RepositoryService.getProductClassRepository()
					.findByMissionCodeAndProductType(missionCode, restWorkflow.getOutputProductClass());

			if (null == newOutputProductClass) {
				throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_MISSSPECIFIED,
						"output product class", restWorkflow.getMissionCode(), restWorkflow.getOutputProductClass()));
			}
			if (!modelWorkflow.getConfiguredProcessor().getProcessor().getProcessorClass().getProductClasses()
					.contains(newOutputProductClass)) {
				throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PROCESSOR_PRODUCT_MISMATCH));
			}
			modelWorkflow.setOutputProductClass(newOutputProductClass);
		}

		if (logger.isTraceEnabled())
			logger.trace("... scalar attributes for workflow have changed: " + workflowChanged);

		// Check whether options were deleted entirely
		if (restWorkflow.getWorkflowOptions().isEmpty()) {
			workflowOptionsChanged = true;
			modelWorkflow.getWorkflowOptions().clear();
		}

		// If options were provided, replace old options
		if (!restWorkflow.getWorkflowOptions().isEmpty()) {
			workflowOptionsChanged = true;
			modelWorkflow.getWorkflowOptions().clear();

			for (WorkflowOption newOption : WorkflowUtil.toModelWorkflow(restWorkflow).getWorkflowOptions()) {
				RepositoryService.getWorkflowOptionRepository().save(newOption);

				// If new options are provided, their mandatory fields must be set.
				if (null == newOption.getName()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
							"For workflowOption modification, option name"));
				}
				if (null == newOption.getType()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
							"For workflowOption modification, option type"));
				}
				if (newOption.getValueRange().isEmpty()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
							"For workflowOption modification, value range"));
				}
				if (null != newOption.getDefaultValue() && !newOption.getValueRange().contains(newOption.getDefaultValue())) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.RANGE_MUST_CONTAIN_DEFAULT,
							newOption.getDefaultValue(), newOption.getId()));
				}
				modelWorkflow.getWorkflowOptions().add(newOption);
			}
		}

		if (logger.isTraceEnabled())
			logger.trace("... workflow options have changed: " + workflowOptionsChanged);

		// Save workflow only if anything was actually changed
		if (workflowChanged || workflowOptionsChanged) {
			modelWorkflow.incrementVersion();
			modelWorkflow = RepositoryService.getWorkflowRepository().save(modelWorkflow);
			logger.log(ProcessorMgrMessage.WORKFLOW_MODIFIED, id);
		} else {
			logger.log(ProcessorMgrMessage.WORKFLOW_NOT_MODIFIED, id);
		}

		return WorkflowUtil.toRestWorkflow(modelWorkflow);

	}

	/**
	 * Get workflows by mission, name and version (user-defined version, not
	 * database version)
	 *
	 * @param missionCode     the mission code
	 * @param workflowName    the name of the workflow (class)
	 * @param workflowVersion the workflow version
	 * @return a list of Json objects representing workflows satisfying the search
	 *         criteria
	 * @throws NoResultException if no workflows matching the given search criteria
	 *                           could be found
	 * @throws SecurityException if a cross-mission data access was attempted
	 */
	public List<RestWorkflow> getWorkflows(String missionCode, String workflowName, String workflowVersion,
			String outputProductClass, String configuredProcessor) throws NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getWorkflows({}, {}, {}, {}, {})", missionCode, workflowName, workflowVersion,
					outputProductClass, configuredProcessor);

		if (null == missionCode) {
			missionCode = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(missionCode)) {
				throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, missionCode,
						securityService.getMission()));
			}
		}

		List<RestWorkflow> result = new ArrayList<>();

		String jpqlQuery = "select w from Workflow w where configuredProcessor.processor.processorClass.mission.code = :missionCode";
		if (null != workflowName) {
			jpqlQuery += " and name = :workflowName";
		}
		if (null != workflowVersion) {
			jpqlQuery += " and workflowVersion = :workflowVersion";
		}
		if (null != outputProductClass) {
			jpqlQuery += " and outputProductClass.productType = :outputProductClass";
		}
		if (null != configuredProcessor) {
			jpqlQuery += " and configuredProcessor.identifier = :configuredProcessor";
		}

		Query query = em.createQuery(jpqlQuery);
		query.setParameter("missionCode", missionCode);
		if (null != workflowName) {
			query.setParameter("workflowName", workflowName);
		}
		if (null != workflowVersion) {
			query.setParameter("workflowVersion", workflowVersion);
		}
		if (null != outputProductClass) {
			query.setParameter("outputProductClass", outputProductClass);
		}
		if (null != configuredProcessor) {
			query.setParameter("configuredProcessor", configuredProcessor);
		}

		for (Object resultObject : query.getResultList()) {
			if (resultObject instanceof Workflow) {
				result.add(WorkflowUtil.toRestWorkflow((Workflow) resultObject));
			}
		}
		if (result.isEmpty()) {
			throw new NoResultException(logger.log(ProcessorMgrMessage.NO_WORKFLOW_FOUND, missionCode, workflowName,
					workflowVersion, outputProductClass, configuredProcessor));
		}

		logger.log(ProcessorMgrMessage.WORKFLOW_LIST_RETRIEVED, result.size(), missionCode, workflowName,
				workflowVersion, outputProductClass, configuredProcessor);

		return result;
	}

}
