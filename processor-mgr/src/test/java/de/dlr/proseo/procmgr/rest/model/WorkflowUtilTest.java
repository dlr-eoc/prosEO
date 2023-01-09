/**
 * WorkflowUtilTest.java
 * 
 * (c) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.procmgr.rest.model;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Test;

import de.dlr.proseo.logging.logger.ProseoLogger;

/**
 * Testing utility methods for workflows
 * 
 * @author Katharina Bassler
 *
 */
public class WorkflowUtilTest {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(WorkflowUtilTest.class);

	// Test data
	private static String[] testWorkflowData =
			// name, uuid, workflowVersion, inputProductClass, outputProductClass,
			// configuredProcessor
			{ "CLOUD_to_O3", UUID.randomUUID().toString(), "1.0", "L2__CLOUD_", "L2__O3____",
					"UPAS 02.04.01 2022-11-02" };
	private static String[] testWorkflowOption =
			// workflowName, name, optionType
			{ "CLOUD_to_O3", "Threads", "NUMBER" };

	/**
	 * Create a local test workflow
	 *
	 * @param workflowData The data from which to create the workflow
	 * @returns the created workflow
	 */
	private static RestWorkflow createLocalWorkflow(String[] workflowData, String[] workflowOptionData) {
		logger.trace("... creating local test workflow");

		RestWorkflow workflow = new RestWorkflow();

		// set workflow parameters
		workflow.setName(workflowData[0]);
		workflow.setUuid(workflowData[1]);
		workflow.setWorkflowVersion(workflowData[2]);
		workflow.setInputProductClass(workflowData[3]);
		workflow.setOutputProductClass(workflowData[4]);
		workflow.setConfiguredProcessor(workflowData[5]);

		// set workflow options
		RestWorkflowOption workflowOption = new RestWorkflowOption();
		workflowOption.setWorkflowName(workflowOptionData[0]);
		workflowOption.setName(workflowOptionData[1]);
		workflowOption.setOptionType(workflowOptionData[2]);
		workflowOption.setValueRange(null);
		workflow.getWorkflowOptions().add(workflowOption);

		return workflow;
	}

	/**
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.model.WorkflowUtil#toRestWorkflow(de.dlr.proseo.model.Workflow)}.
	 * Test method for
	 * {@link de.dlr.proseo.procmgr.rest.model.WorkflowUtil#toModelWorkflow(de.dlr.proseo.procmgr.rest.model.RestWorkflow)}.
	 */
	@Test
	public final void testWorkflowUtil() {
		RestWorkflow initialWorkflow = createLocalWorkflow(testWorkflowData, testWorkflowOption);
		RestWorkflow convertedWorkflow = WorkflowUtil.toRestWorkflow(WorkflowUtil.toModelWorkflow(initialWorkflow));

		assertEquals(initialWorkflow.getName(), convertedWorkflow.getName());
		assertEquals(initialWorkflow.getId(), convertedWorkflow.getId());
		assertEquals(initialWorkflow.getVersion(), convertedWorkflow.getVersion());
		assertEquals(1, initialWorkflow.getWorkflowOptions().size());
		assertEquals(1, convertedWorkflow.getWorkflowOptions().size());

		RestWorkflowOption initialOption = initialWorkflow.getWorkflowOptions().get(0);
		RestWorkflowOption convertedOption = initialWorkflow.getWorkflowOptions().get(0);
		assertEquals(initialOption.getName(), convertedOption.getName());
		assertEquals(initialOption.getOptionType(), convertedOption.getOptionType());
		assertEquals(initialOption.getValueRange(), convertedOption.getValueRange());
	}

}
