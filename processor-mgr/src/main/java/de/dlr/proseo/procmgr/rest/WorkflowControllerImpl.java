/**
 * WorkflowControllerImpl.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest;

import java.util.ConcurrentModificationException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.procmgr.rest.model.RestWorkflow;

/**
 * Spring MVC controller for the prosEO Workflow Manager; implements the
 * services required to manage workflows.
 * 
 * @author Katharina Bassler
 */
@Component
public class WorkflowControllerImpl implements WorkflowController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(WorkflowControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.PROCESSOR_MGR);

	/** The workflow manager */
	@Autowired
	private WorkflowMgr workflowManager;

	/** JPA entity manager */
	@PersistenceContext
	private EntityManager em;

	/**
	 * Count the workflows matching the specified workflowName, workflowVersion,
	 * outputProductClass, or configured processor.
	 * 
	 * @param missionCode         the mission code
	 * @param workflowName        the workflow name
	 * @param workflowVersion     the workflow version
	 * @param outputProductClass  the output product class
	 * @param configuredProcessor the configured processor
	 * @return the number of matching workflows as a String (may be zero) or HTTP
	 *         status "FORBIDDEN" and an error message, if a cross-mission data
	 *         access was attempted
	 */
	@Override
	public ResponseEntity<?> countWorkflows(String missionCode, String workflowName, String workflowVersion,
			String outputProductClass, String configuredProcessor) {
		if (logger.isTraceEnabled())
			logger.trace(">>> countWorkflows({}, {}, {}, {}, {})", missionCode, workflowName, workflowVersion,
					outputProductClass, configuredProcessor);

		try {
			return new ResponseEntity<>(workflowManager.countWorkflows(missionCode, workflowName, workflowVersion,
					outputProductClass, configuredProcessor), HttpStatus.OK);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}
	
	/**
	 * Get a list of all workflows with the specified mission, workflow name,
	 * workflow version, output product class and configured processor
	 *
	 * @param missionCode         the mission code
	 * @param workflowName        the workflow name
	 * @param workflowVersion     the workflow version
	 * @param outputProductClass  the output product class
	 * @param configuredProcessor the configured processor
	 * @return HTTP status "OK" and a list of workflows or HTTP status "NOT_FOUND"
	 *         and an error message, if no workflows match the search criteria, or
	 *         HTTP status "FORBIDDEN" and an error message, if a cross-mission data
	 *         access was attempted, or HTTP status "TOO MANY REQUESTS" if the result
	 *         list exceeds a configured maximum
	 */
	@Override
	public ResponseEntity<List<RestWorkflow>> getWorkflows(String missionCode, String workflowName,
			String workflowVersion, String outputProductClass, String configuredProcessor, Integer recordFrom, Integer recordTo) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getWorkflows({}, {}, {}, {}, {})", missionCode, workflowName, workflowVersion,
					outputProductClass, configuredProcessor);

		try {
			return new ResponseEntity<>(workflowManager.getWorkflows(missionCode, workflowName, workflowVersion,
					outputProductClass, configuredProcessor, recordFrom, recordTo), HttpStatus.OK);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (HttpClientErrorException.TooManyRequests e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.TOO_MANY_REQUESTS);
		}
	}

	/**
	 * Create a workflow from the given Json object
	 *
	 * @param workflow the Json object from which to create the workflow
	 * @return HTTP status "CREATED" and a response containing a Json object
	 *         corresponding to the workflow after persistence (with ID and version
	 *         for all contained objects) or HTTP status "FORBIDDEN" and an error
	 *         message, if a cross-mission data access was attempted, or HTTP status
	 *         "BAD_REQUEST", if any of the input data was invalid
	 */
	@Override
	public ResponseEntity<RestWorkflow> createWorkflow(@Valid RestWorkflow workflow) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createWorkflow({})", (null == workflow ? "MISSING" : workflow.getName()));

		try {
			return new ResponseEntity<>(workflowManager.createWorkflow(workflow), HttpStatus.CREATED);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Find the workflow with the given ID
	 *
	 * @param id the ID to look for
	 * @return HTTP status "OK" and a Json object corresponding to the found order
	 *         or HTTP status "FORBIDDEN" and an error message, if a cross-mission
	 *         data access was attempted, or HTTP status "NOT_FOUND", if no workflow
	 *         with the given ID exists
	 */
	@Override
	public ResponseEntity<RestWorkflow> getWorkflowById(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getWorkflowById({})", id);

		try {
			return new ResponseEntity<>(workflowManager.getWorkflowById(id), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (NoResultException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Delete a workflow by ID
	 *
	 * @param id the ID of the workflow to delete
	 * @return a response entity with HTTP status "NO_CONTENT", if the deletion was
	 *         successful, or HTTP status "NOT_FOUND" and an error message, if the
	 *         workflow did not exist, or HTTP status "FORBIDDEN" and an error
	 *         message, if a cross-mission data access was attempted, or HTTP status
	 *         "NOT_MODIFIED" and an error message, if the deletion was unsuccessful
	 */
	@Override
	public ResponseEntity<?> deleteWorkflowById(Long id) {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteWorkflowById({})", id);

		try {
			workflowManager.deleteWorkflowById(id);
			return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		} catch (RuntimeException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
		}
	}

	/**
	 * Update the workflow with the given ID with the attribute values of the given
	 * Json object.
	 *
	 * @param id       the ID of the workflow to update
	 * @param workflow a Json object containing the modified (and unmodified)
	 *                 attributes
	 * @return a response containing HTTP status "OK" and a Json object
	 *         corresponding to the workflow after modification (with ID and version
	 *         for all contained objects) or HTTP status "NOT_MODIFIED" and the
	 *         unchanged workflow, if no attributes were actually changed, or HTTP
	 *         status "NOT_FOUND" and an error message, if no workflow with the
	 *         given ID exists, or HTTP status "FORBIDDEN" and an error message, if
	 *         a cross-mission data access was attempted
	 */
	@Override
	public ResponseEntity<RestWorkflow> modifyWorkflow(Long id, RestWorkflow workflow) {
		if (logger.isTraceEnabled())
			logger.trace(">>> modifyWorkflow({})", id);

		try {
			RestWorkflow changedWorkflow = workflowManager.modifyWorkflow(id, workflow);
			HttpStatus httpStatus = (workflow.getVersion() == changedWorkflow.getVersion() ? HttpStatus.NOT_MODIFIED
					: HttpStatus.OK);
			return new ResponseEntity<>(changedWorkflow, httpStatus);
		} catch (EntityNotFoundException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (ConcurrentModificationException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
		} catch (SecurityException e) {
			return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
		}
	}

}