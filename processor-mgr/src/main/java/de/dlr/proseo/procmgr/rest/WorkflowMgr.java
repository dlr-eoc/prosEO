/**
 * WorkflowMgr.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.ProcessorMgrMessage;
import de.dlr.proseo.model.ClassOutputParameter;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.InputFilter;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.Workflow;
import de.dlr.proseo.model.WorkflowOption;
import de.dlr.proseo.model.WorkflowOption.WorkflowOptionType;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.service.SecurityService;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.procmgr.ProcessorManagerConfiguration;
import de.dlr.proseo.procmgr.rest.model.RestClassOutputParameter;
import de.dlr.proseo.procmgr.rest.model.RestInputFilter;
import de.dlr.proseo.procmgr.rest.model.RestParameter;
import de.dlr.proseo.procmgr.rest.model.RestWorkflow;
import de.dlr.proseo.procmgr.rest.model.RestWorkflowOption;
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

	/** The processor manager configuration */
	@Autowired
	ProcessorManagerConfiguration config; 
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(WorkflowMgr.class);

	/**
	 * Count the workflows matching the specified workflowName, workflowVersion,
	 * outputProductClass, or configured processor.
	 * 
	 * @param missionCode         the mission code
	 * @param workflowName        the workflow name
	 * @param workflowVersion     the workflow version
	 * @param outputProductClass  the output product class
	 * @param configuredProcessor the configured processor
	 * @return the number of workflows found as string
	 * @throws SecurityException if a cross-mission data access was attempted
	 */
	public String countWorkflows(String missionCode, String workflowName, String workflowVersion,
			String outputProductClass, String configuredProcessor, Boolean enabled) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countWorkflows({}, {}, {}, {}, {}, {})", missionCode, workflowName, workflowVersion,
					outputProductClass, configuredProcessor, enabled);

		if (null == missionCode) {
			missionCode = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(missionCode)) {
				throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, missionCode,
						securityService.getMission()));
			}
		}

		// build query
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = cb.createQuery(Long.class);
		Root<Workflow> rootWorkflow = query.from(Workflow.class);

		List<Predicate> predicates = new ArrayList<>();

		predicates.add(cb.equal(rootWorkflow.get("configuredProcessor").get("processor").get("processorClass")
				.get("mission").get("code"), missionCode));
		if (workflowName != null)
			predicates.add(cb.equal(rootWorkflow.get("name"), workflowName));
		if (workflowVersion != null)
			predicates.add(cb.equal(rootWorkflow.get("workflowVersion"), workflowVersion));
		if (outputProductClass != null)
			predicates.add(cb.equal(rootWorkflow.get("outputProductClass").get("productType"), outputProductClass));
		if (configuredProcessor != null)
			predicates.add(cb.equal(rootWorkflow.get("configuredProcessor").get("identifier"), configuredProcessor));
		if (enabled != null)
			predicates.add(cb.equal(rootWorkflow.get("enabled"), enabled));
		query.select(cb.count(rootWorkflow)).where(predicates.toArray(new Predicate[predicates.size()]));

		Long result = em.createQuery(query).getSingleResult();

		logger.log(ProcessorMgrMessage.WORKFLOWS_COUNTED, result, missionCode, workflowName, workflowVersion,
				outputProductClass, configuredProcessor, enabled);

		return result.toString();
	}
	
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
		if ((null != RepositoryService.getWorkflowRepository().findByMissionCodeAndWorkflowNameAndWorkflowVersion(
				restWorkflow.getMissionCode(), restWorkflow.getName(), restWorkflow.getWorkflowVersion())
				|| (null != restWorkflow.getUuid() && null != RepositoryService.getWorkflowRepository()
						.findByUuid(UUID.fromString(restWorkflow.getUuid()))))) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.DUPLICATE_WORKFLOW, restWorkflow.getMissionCode(),
							restWorkflow.getName(), restWorkflow.getWorkflowVersion(), restWorkflow.getUuid()));
		}

		// Prepare the database order, but make sure ID and version are not copied if present
		restWorkflow.setId(null);
		restWorkflow.setVersion(null);
		if (null != restWorkflow.getWorkflowOptions() && !restWorkflow.getWorkflowOptions().isEmpty()) {
			for (RestWorkflowOption option : restWorkflow.getWorkflowOptions()) {				
				option.setId(null);
				option.setVersion(null);
			}
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

		// Enabled status is mandatory.
		if (null == restWorkflow.getEnabled()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow creation, enabled field")); 
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
		
		// Output file class is mandatory.
		if (null == restWorkflow.getOutputFileClass()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow creation, outputFileClass"));
		}
		
		// Processing mode is mandatory.
		if (null == restWorkflow.getProcessingMode()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow creation, processingMode"));
		}
		
		// Slicing type is mandatory and duration and overlap must be specified accordingly.
		if (null == restWorkflow.getSlicingType()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow creation, slicingType"));
		}
		
		if (restWorkflow.getSlicingType().equals(OrderSlicingType.TIME_SLICE.toString()) && restWorkflow.getSliceDuration().equals(Long.valueOf(0l))) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow creation and slicingType TIME_SLICE, slicingDuration"));
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
				if (null == option.getValueRange()) {
					// Quietly restore value range as empty list
					option.setValueRange(new ArrayList<>());
				}
			}
		}

		// Check for completeness of non-mandatory attributes

		// If provided, inputFilters must have the mandatory fields set.
		if (!modelWorkflow.getInputFilters().isEmpty()) {
			modelWorkflow.getInputFilters().forEach((productClass, filterConditions) -> {
				if (productClass == null) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For inputFilter creation, productClass"));
				}
				if (filterConditions.getFilterConditions().isEmpty()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
							"For inputFilter creation, filterConditions"));
					// TODO possibly check whether filter conditions have mandatory fields set
				}
			});
		}

		// If provided, classOutputParameters must have the mandatory fields set.
		if (!modelWorkflow.getClassOutputParameters().isEmpty()) {
			modelWorkflow.getClassOutputParameters().forEach((productClass, classOutputParameters) -> {
				if (productClass == null) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
							"For classOutputParameter creation, productClass"));
				}
				if (classOutputParameters.getOutputParameters().isEmpty()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
							"For classOutputParameter creation, classOutputParameters"));
					// TODO possibly check whether output parameters have mandatory fields set
				}
			});
		}

		// If provided, outputParameters must have the mandatory fields set.
		if (!modelWorkflow.getOutputParameters().isEmpty()) {
			modelWorkflow.getOutputParameters().forEach((key, parameter) -> {
				if (key == null) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For outputParameter creation, product"));
				}
				if (null == parameter.getParameterType()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
							"For outputParameter creation, parameter type"));
				}
				if (null == parameter.getParameterValue()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
							"For outputParameter creation, parameter value"));
				}
			});
		}
		
		// The new workflow is saved to the repository.
		RepositoryService.getWorkflowRepository().save(modelWorkflow);
		modelWorkflow = RepositoryService.getWorkflowRepository().findByUuid(modelWorkflow.getUuid());
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

		// Enabled status may not be null.
		if (null == restWorkflow.getEnabled()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow modification, enabled field"));
		} else if (!modelWorkflow.getEnabled().equals(restWorkflow.getEnabled())) {
			workflowChanged = true;
			modelWorkflow.setEnabled(restWorkflow.getEnabled());
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
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow modification, outputProductClass"));
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
		
		// Output file class is mandatory.
		if (null == restWorkflow.getOutputFileClass()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow modification, outputFileClass"));
		} else if (!modelWorkflow.getOutputFileClass().equals(restWorkflow.getOutputFileClass())) {
			workflowChanged = true;
			modelWorkflow.setOutputFileClass(restWorkflow.getOutputFileClass());
		}
		
		// Processing mode is mandatory.
		if (null == restWorkflow.getProcessingMode()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow modification, processingMode"));
		} else if (!modelWorkflow.getProcessingMode().equals(restWorkflow.getProcessingMode())) {
			workflowChanged = true;
			modelWorkflow.setProcessingMode(restWorkflow.getProcessingMode());
		}

		// Check that slice parameters are still consistent
		if (null == restWorkflow.getSlicingType()) {
			throw new IllegalArgumentException(
					logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "For workflow modification, slicingType"));
		}

		if (!restWorkflow.getSlicingType().equals(modelWorkflow.getSlicingType().toString())) {
			workflowChanged = true;
			if (restWorkflow.getSlicingType().equals(OrderSlicingType.TIME_SLICE.toString())
					&& restWorkflow.getSliceDuration().equals(Long.valueOf(0l))) {
				throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
						"For workflow modification and slicingType TIME_SLICE, slicingDuration"));
			}
		}

		// Check for changes in non-mandatory attributes

		// If input filters where provided, update old filters
		// Remember old values
		Map<ProductClass, InputFilter> oldFilters = new HashMap<>();
		modelWorkflow.getInputFilters()
				.forEach((productClass, inputFilter) -> oldFilters.put(productClass, inputFilter));

		if (!restWorkflow.getInputFilters().isEmpty()) {

			for (RestInputFilter newFilter : restWorkflow.getInputFilters()) {
				InputFilter modelFilter = null;
				Boolean isNew = false;

				// Check for missing mandatory values in new filter
				if (null == newFilter.getProductClass()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "In input filter: productClass"));
				}
				if (newFilter.getFilterConditions().isEmpty() || null == newFilter.getFilterConditions()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "In input filter: filterConditions"));
				}
				for (RestParameter param : newFilter.getFilterConditions()) {
					if (null == param.getKey()) {
						throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
								"In inputFilter/filterConditions, parameter key"));
					}
					if (null == param.getParameterType()) {
						throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
								"In inputFilter/filterConditions, parameter type"));
					}
					if (null == param.getParameterValue()) {
						throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
								"In inputFilter/filterConditions, parameter value"));
					}
				}

				// Ensure that the new filter specifies a valid product class
				ProductClass productClass = RepositoryService.getProductClassRepository()
						.findByMissionCodeAndProductType(missionCode, newFilter.getProductClass());
				if (null == productClass) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.PRODUCT_CLASS_NOT_FOUND, newFilter.getProductClass()));
				}

				// Check whether the new filter is in fact an old filter
				if (modelWorkflow.getInputFilters().containsKey(productClass)) {
					modelFilter = modelWorkflow.getInputFilters().get(productClass);
				} else {
					isNew = true;
				}

				// Potentially override old values
				// TODO truly override instead of replace
				modelFilter.getFilterConditions().clear();
				for (RestParameter param : newFilter.getFilterConditions()) {
					Parameter newParam = new Parameter();
					newParam.setParameterType(ParameterType.valueOf(param.getParameterType()));
					switch (newParam.getParameterType()) {
					case BOOLEAN:
						newParam.setBooleanValue(Boolean.valueOf(param.getParameterValue()));
						newParam.setParameterValue(Boolean.valueOf(param.getParameterValue()));
						break;
					case DOUBLE:
						newParam.setDoubleValue(Double.valueOf(param.getParameterValue()));
						newParam.setParameterValue(Double.valueOf(param.getParameterValue()));
						break;
					case INSTANT:
						// TODO verify
						newParam.setInstantValue(OrbitTimeFormatter.parseDateTime(param.getParameterValue()));
						newParam.setParameterValue(OrbitTimeFormatter.parseDateTime(param.getParameterValue()));
						break;
					case INTEGER:
						newParam.setIntegerValue(Integer.valueOf(param.getParameterValue()));
						newParam.setParameterValue(Integer.valueOf(param.getParameterValue()));
						break;
					case STRING:
						newParam.setStringValue(param.getParameterValue());
						newParam.setParameterValue(param.getParameterValue());
						break;
					default:
						break;
					}
					modelFilter.getFilterConditions().put(param.getKey(), newParam);
				}

				// Add new filters, update map of old values to contain only deleted filters
				if (isNew) {
					modelWorkflow.getInputFilters().put(productClass, modelFilter);
				} else {
					oldFilters.remove(productClass);
				}
			}

			// Remove deleted filters
			for (ProductClass prodClass : oldFilters.keySet()) {
				if (modelWorkflow.getInputFilters().containsKey(prodClass)) {
					modelWorkflow.getInputFilters().remove(prodClass);
				}
			}
		}

		// If classOutputParameters where provided, update old classOutputParameters
		// Remember old values
		Map<ProductClass, ClassOutputParameter> oldClassOutputParameters = new HashMap<>();
		modelWorkflow.getClassOutputParameters().forEach((productClass,
				classOutputParameter) -> oldClassOutputParameters.put(productClass, classOutputParameter));

		if (!restWorkflow.getClassOutputParameters().isEmpty()) {

			for (RestClassOutputParameter newClassOutputParameter : restWorkflow.getClassOutputParameters()) {
				ClassOutputParameter modelClassOutputParameter = null;
				Boolean isNew = false;

				// Check for missing mandatory values in new classOutputParameter
				if (null == newClassOutputParameter.getProductClass()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
							"In input classOutputParameter: productClass"));
				}
				if (newClassOutputParameter.getOutputParameters().isEmpty()
						|| null == newClassOutputParameter.getOutputParameters()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
							"In input classOutputParameter: outputParameters"));
				}
				for (RestParameter param : newClassOutputParameter.getOutputParameters()) {
					if (null == param.getKey()) {
						throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
								"In classOutputParameter/outputParameters, parameter key"));
					}
					if (null == param.getParameterType()) {
						throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
								"In classOutputParameter/outputParameters, parameter type"));
					}
					if (null == param.getParameterValue()) {
						throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
								"In classOutputParameter/outputParameters, parameter value"));
					}
				}

				// Ensure that the new classOutputParameter specifies a valid product class
				ProductClass productClass = RepositoryService.getProductClassRepository()
						.findByMissionCodeAndProductType(missionCode, newClassOutputParameter.getProductClass());
				if (null == productClass) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.PRODUCT_CLASS_NOT_FOUND,
							newClassOutputParameter.getProductClass()));
				}

				// Check whether the new classOutputParameter is in fact an old
				// classOutputParameter
				if (modelWorkflow.getClassOutputParameters().containsKey(productClass)) {
					modelClassOutputParameter = modelWorkflow.getClassOutputParameters().get(productClass);
				} else {
					isNew = true;
				}

				// Potentially override old values
				// TODO truly override instead of replace
				modelClassOutputParameter.getOutputParameters().clear();
				for (RestParameter param : newClassOutputParameter.getOutputParameters()) {
					Parameter newParam = new Parameter();
					newParam.setParameterType(ParameterType.valueOf(param.getParameterType()));
					switch (newParam.getParameterType()) {
					case BOOLEAN:
						newParam.setBooleanValue(Boolean.valueOf(param.getParameterValue()));
						newParam.setParameterValue(Boolean.valueOf(param.getParameterValue()));
						break;
					case DOUBLE:
						newParam.setDoubleValue(Double.valueOf(param.getParameterValue()));
						newParam.setParameterValue(Double.valueOf(param.getParameterValue()));
						break;
					case INSTANT:
						// TODO verify
						newParam.setInstantValue(OrbitTimeFormatter.parseDateTime(param.getParameterValue()));
						newParam.setParameterValue(OrbitTimeFormatter.parseDateTime(param.getParameterValue()));
						break;
					case INTEGER:
						newParam.setIntegerValue(Integer.valueOf(param.getParameterValue()));
						newParam.setParameterValue(Integer.valueOf(param.getParameterValue()));
						break;
					case STRING:
						newParam.setStringValue(param.getParameterValue());
						newParam.setParameterValue(param.getParameterValue());
						break;
					default:
						break;
					}
					modelClassOutputParameter.getOutputParameters().put(param.getKey(), newParam);
				}

				// Add new classOutputParameters, update map of old values to contain only
				// deleted classOutputParameters
				if (isNew) {
					modelWorkflow.getClassOutputParameters().put(productClass, modelClassOutputParameter);
				} else {
					oldClassOutputParameters.remove(productClass);
				}
			}

			// Remove deleted classOutputParameters
			for (ProductClass prodClass : oldClassOutputParameters.keySet()) {
				if (modelWorkflow.getClassOutputParameters().containsKey(prodClass)) {
					modelWorkflow.getClassOutputParameters().remove(prodClass);
				}
			}
		}

		// If outputParameters where provided, update old outputParameters
		// Remember old values
		Map<String, Parameter> oldOutputParameters = new HashMap<>();
		modelWorkflow.getOutputParameters()
				.forEach((productClass, outputParameter) -> oldOutputParameters.put(productClass, outputParameter));

		if (!restWorkflow.getOutputParameters().isEmpty()) {

			for (RestParameter newOutputParameter : restWorkflow.getOutputParameters()) {
				Parameter modelOutputParameter = null;
				Boolean isNew = false;

				// Check for missing mandatory values in new outputParameter
				if (null == newOutputParameter.getKey()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "In outputParameter, parameter key"));
				}
				if (null == newOutputParameter.getParameterType()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "In outputParameter, parameter type"));
				}
				if (null == newOutputParameter.getParameterValue()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "In outputParameter, parameter value"));
				}

				// Check whether the new outputParameter is in fact an old
				// outputParameter
				if (modelWorkflow.getOutputParameters().containsKey(newOutputParameter.getKey())) {
					modelOutputParameter = modelWorkflow.getOutputParameters().get(newOutputParameter.getKey());
				} else {
					isNew = true;
				}

				// Potentially override old values
				modelOutputParameter.setParameterType(ParameterType.valueOf(newOutputParameter.getParameterType()));
				switch (modelOutputParameter.getParameterType()) {
				case BOOLEAN:
					modelOutputParameter.setBooleanValue(Boolean.valueOf(newOutputParameter.getParameterValue()));
					modelOutputParameter.setParameterValue(Boolean.valueOf(newOutputParameter.getParameterValue()));
					break;
				case DOUBLE:
					modelOutputParameter.setDoubleValue(Double.valueOf(newOutputParameter.getParameterValue()));
					modelOutputParameter.setParameterValue(Double.valueOf(newOutputParameter.getParameterValue()));
					break;
				case INSTANT:
					// TODO verify
					modelOutputParameter
							.setInstantValue(OrbitTimeFormatter.parseDateTime(newOutputParameter.getParameterValue()));
					modelOutputParameter.setParameterValue(
							OrbitTimeFormatter.parseDateTime(newOutputParameter.getParameterValue()));
					break;
				case INTEGER:
					modelOutputParameter.setIntegerValue(Integer.valueOf(newOutputParameter.getParameterValue()));
					modelOutputParameter.setParameterValue(Integer.valueOf(newOutputParameter.getParameterValue()));
					break;
				case STRING:
					modelOutputParameter.setStringValue(newOutputParameter.getParameterValue());
					modelOutputParameter.setParameterValue(newOutputParameter.getParameterValue());
					break;
				default:
					break;
				}
				;

				// Add new outputParameters, update map of old values to contain only
				// deleted outputParameters
				if (isNew) {
					modelWorkflow.getOutputParameters().put(newOutputParameter.getKey(), modelOutputParameter);
				} else {
					oldOutputParameters.remove(newOutputParameter.getKey());
				}
			}

			// Remove deleted outputParameters
			for (String key : oldOutputParameters.keySet()) {
				if (modelWorkflow.getOutputParameters().containsKey(key)) {
					modelWorkflow.getOutputParameters().remove(key);
				}
			}
		}

		// UUID may not be modified
		if (restWorkflow.getUuid() != null && !modelWorkflow.getUuid().toString().equals(restWorkflow.getUuid())) {
			throw new IllegalArgumentException(logger.log(GeneralMessage.NO_UUID_MODIFICATION));
		}

		if (logger.isTraceEnabled())
			logger.trace("... scalar attributes for workflow have changed: " + workflowChanged);

		// TODO change modification logic regarding workflow options, or version will be
		// incremented regardless of actual changes
		// Check whether options were deleted entirely
		if (restWorkflow.getWorkflowOptions().isEmpty()) {
			workflowOptionsChanged = true;
			modelWorkflow.getWorkflowOptions().clear();
		}

		// If options were provided, update old options (and don't create new one)

		// TODO remember names to remove "deleted" options
		// Remember old values
		Map<String, String> optNames = new HashMap<String, String>();
		for (WorkflowOption opt : modelWorkflow.getWorkflowOptions()) {
			optNames.put(opt.getName(), opt.getName());
		}
		if (!restWorkflow.getWorkflowOptions().isEmpty()) {
			workflowOptionsChanged = true;

			for (RestWorkflowOption newOption : restWorkflow.getWorkflowOptions()) {
				WorkflowOption modelOption = null;
				Boolean isNew = false;

				// Check for missing mandatory values in new option
				if (null == newOption.getMissionCode()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "In workflow option: missionCode"));
				}
				if (null == newOption.getWorkflowName()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "In workflow option: workflowName"));
				}
				if (null == newOption.getName()) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.FIELD_NOT_SET, "In workflow option: name"));
				}
				if (null == newOption.getOptionType()) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.FIELD_NOT_SET,
							"For workflowOption modification, option type"));
				}
				if (null == newOption.getValueRange()) {
					// Quietly restore value range as empty list
					newOption.setValueRange(new ArrayList<>());
				}

				// Assert that mission code and workflow name match
				if ((null != newOption.getMissionCode()
						&& !newOption.getMissionCode().equals(restWorkflow.getMissionCode()))
						|| (null != newOption.getWorkflowName()
								&& !newOption.getWorkflowName().equals(restWorkflow.getName()))) {
					throw new IllegalArgumentException(logger.log(ProcessorMgrMessage.WORKFLOW_OPTION_MISMATCH,
							newOption.getMissionCode(), newOption.getWorkflowName(), restWorkflow.getMissionCode(),
							restWorkflow.getName()));
				}

				// Check whether the new option is in fact an old option
				for (WorkflowOption opt : modelWorkflow.getWorkflowOptions()) {
					if (newOption.getName().equals(opt.getName())) {
						modelOption = opt;
						break;
					}
				}

				// If the option is indeed new, initialize it
				if (modelOption == null) {
					modelOption = new WorkflowOption();
					modelOption.setWorkflow(modelWorkflow);
					modelOption.setName(newOption.getName());
					isNew = true;
				}

				// Potentially override old values
				if (null == WorkflowOptionType.get(newOption.getOptionType().toLowerCase())) {
					throw new IllegalArgumentException(
							logger.log(ProcessorMgrMessage.ILLEGAL_OPTION_TYPE, newOption.getOptionType()));
				}
				modelOption.setType(WorkflowOptionType.get(newOption.getOptionType().toLowerCase()));
				modelOption.setValueRange(newOption.getValueRange());
				if (null == modelOption.getValueRange()) {
					// Quietly restore value range as empty list
					modelOption.setValueRange(new ArrayList<>());
				}
				modelOption.setDefaultValue(newOption.getDefaultValue());

				// Add new options, update map of old values to contain only deleted options
				if (isNew) {
					modelWorkflow.getWorkflowOptions().add(modelOption);
				} else {
					optNames.remove(modelOption.getName());
				}
			}

			// Remove deleted options
			for (String name : optNames.keySet()) {
				WorkflowOption toRemove = null;
				for (WorkflowOption opt : modelWorkflow.getWorkflowOptions()) {
					if (opt.getName().equals(name)) {
						toRemove = opt;
						break;
					}
				}
				if (toRemove != null) {
					modelWorkflow.getWorkflowOptions().remove(toRemove);
				}
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
	 * @param recordFrom      first record of filtered and ordered result to return
	 * @param recordTo        last record of filtered and ordered result to return
	 * @return a list of Json objects representing workflows satisfying the search
	 *         criteria
	 * @throws NoResultException if no workflows matching the given search criteria
	 *                           could be found
	 * @throws SecurityException if a cross-mission data access was attempted
	 */
	public List<RestWorkflow> getWorkflows(String missionCode, String workflowName, String workflowVersion,
			String outputProductClass, String configuredProcessor, Boolean enabled, Integer recordFrom, Integer recordTo) throws NoResultException, SecurityException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getWorkflows({}, {}, {}, {}, {}, {})", missionCode, workflowName, workflowVersion,
					outputProductClass, configuredProcessor, enabled);

		if (null == missionCode) {
			missionCode = securityService.getMission();
		} else {
			// Ensure user is authorized for the requested mission
			if (!securityService.isAuthorizedForMission(missionCode)) {
				throw new SecurityException(logger.log(GeneralMessage.ILLEGAL_CROSS_MISSION_ACCESS, missionCode,
						securityService.getMission()));
			}		
		}
		
		if (recordFrom == null) {
			recordFrom = 0;
		}
		if (recordTo == null) {
			recordTo = Integer.MAX_VALUE;
		}

		Long numberOfResults = Long.parseLong(this.countWorkflows(missionCode, workflowName, workflowVersion, outputProductClass, configuredProcessor, enabled));
		Integer maxResults = config.getMaxResults();
		if (numberOfResults > maxResults && (recordTo - recordFrom) > maxResults && (numberOfResults - recordFrom) > maxResults) {
			throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, logger.log(GeneralMessage.TOO_MANY_RESULTS, "workflows", numberOfResults, config.getMaxResults()));
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
		if (null != enabled) {
			jpqlQuery += " and enabled = :enabled";
		}
		jpqlQuery += " ORDER BY w.id";

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
		if (null != enabled) {
			query.setParameter("enabled", enabled);
		}
		query.setFirstResult(recordFrom);
		query.setMaxResults(recordTo - recordFrom);

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
