/**
 * ProcessorMgrMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the processor manager.
 *
 * @author Katharina Bassler
 */
public enum ProcessorMgrMessage implements ProseoMessage {

	CONCURRENT_UPDATE								(4501, Level.ERROR, false, "The configuration with ID {0} has been modified since retrieval by the client", ""),
	CONFIGURATION_CREATED							(4502, Level.INFO, true, "Configuration for processor {0} with version {1} created for mission {2}", ""),
	CONFIGURATION_DATA_MISSING						(4503, Level.ERROR, false, "Configuration data not set", ""),
	CONFIGURATION_DELETED							(4504, Level.INFO, true, "Configuration with id {0} deleted", ""),
	CONFIGURATION_HAS_PROC							(4505, Level.ERROR, false, "Configuration for mission {0} with processor name {1} and configuration version {2} cannot be deleted, because it has configured processors", ""),
	CONFIGURATION_ID_MISSING						(4506, Level.ERROR, false, "Configuration ID not set", ""),
	CONFIGURATION_ID_NOT_FOUND						(4507, Level.ERROR, false, "No Configuration found with ID {0}", ""),
	CONFIGURATION_INVALID							(4508, Level.ERROR, false, "Configuration {0} with version {1} invalid for mission {2}", ""),
	CONFIGURATION_LIST_RETRIEVED					(4509, Level.INFO, true, "Configuration(s) for mission {0}, processor name {1} and configuration version {2} retrieved", ""),
	CONFIGURATION_MISSING							(4510, Level.ERROR, false, "Configuration not set", ""),
	CONFIGURATION_MODIFIED							(4511, Level.INFO, true, "Configuration with id {0} modified", ""),
	CONFIGURATION_NOT_FOUND							(4512, Level.ERROR, false, "No configuration found for mission {0}, processor name {1} and configuration version {2}", ""),
	CONFIGURATION_NOT_MODIFIED						(4513, Level.INFO, true, "Configuration with id {0} not modified (no changes)", ""),
	CONFIGURATION_RETRIEVED							(4514, Level.INFO, true, "Configuration with ID {0} retrieved", ""),
	CONFIGURED_PROCESSOR_CREATED					(4515, Level.INFO, true, "Configuration for processor {0} with version {1} created for mission {2}", ""),
	CONFIGURED_PROCESSOR_DATA_MISSING				(4516, Level.ERROR, false, "Data for configured processor not set", ""),
	CONFIGURED_PROCESSOR_DELETED					(4517, Level.INFO, true, "Configured processor with id {0} deleted", ""),
	CONFIGURED_PROCESSOR_HAS_PRODUCTS				(4518, Level.ERROR, false, "Cannot delete configured processor {0}, because it is referenced by {1} products", ""),
	CONFIGURED_PROCESSOR_HAS_SELECTION_RULES		(4519, Level.ERROR, false, "Cannot delete configured processor {0}, because it is referenced by {1} selection rules", ""),
	CONFIGURED_PROCESSOR_ID_MISSING					(4520, Level.ERROR, false, "Configuration ID not set", ""),
	CONFIGURED_PROCESSOR_ID_NOT_FOUND				(4521, Level.ERROR, false, "No Configuration found with ID {0}", ""),
	CONFIGURED_PROCESSOR_LIST_RETRIEVED				(4522, Level.INFO, true, "Configuration(s) for mission {0}, identifier {1}, processor name {2}, processor version {3} and configuration version {4} retrieved", ""),
	CONFIGURED_PROCESSOR_MISSING					(4523, Level.ERROR, false, "Configuration not set", ""),
	CONFIGURED_PROCESSOR_MODIFIED					(4524, Level.INFO, true, "Configured processor with id {0} modified", ""),
	CONFIGURED_PROCESSOR_NOT_FOUND					(4525, Level.ERROR, false, "No configured processors found for mission {0}, identifier {1}, processor name {2}, processor version {3} and configuration version {4}", ""),
	CONFIGURED_PROCESSOR_NOT_MODIFIED				(4526, Level.INFO, true, "Configured processor with id {0} not modified (no changes)", ""),
	CONFIGURED_PROCESSOR_RETRIEVED					(4527, Level.INFO, true, "Configuration with ID {0} retrieved", ""),
	CRITICALITY_LEVEL_MISSING						(4528, Level.ERROR, false, "Task {0} defined as critical, but no criticality level given", ""),
	DELETE_FAILURE									(4529, Level.ERROR, false, "Processor deletion failed for ID {0} (cause: {1})", ""),
	DELETION_UNSUCCESSFUL							(4530, Level.ERROR, false, "Configuration deletion unsuccessful for ID {0}", ""),
	DUPLICATE_CONFIGURATION							(4531, Level.ERROR, false, "Duplicate configuration for mission {0} with processor name {1} and configuration version {2}", ""),
	DUPLICATE_CONFPROC_ID							(4532, Level.ERROR, false, "Duplicate configured processor identifier {0}", ""),
	DUPLICATE_CONFPROC_UUID							(4533, Level.ERROR, false, "Duplicate configured processor UUID {0}", ""),
	DUPLICATE_PROCESSOR_CLASS						(4534, Level.ERROR, false, "Duplicate processor class for mission {0} and processor name {1}", ""),
	DUPLICATE_PROCESSOR								(4535, Level.ERROR, false, "Duplicate processor for mission {0}, processor name {1} and processor version {2}", ""),
	FILENAME_TYPE_INVALID							(4536, Level.ERROR, false, "Input filename type {0} invalid", ""),
	INPUT_FILE_ID_NOT_FOUND							(4537, Level.ERROR, false, "No static input file found with ID {0}", ""),
	MISSION_CODE_INVALID							(4538, Level.ERROR, false, "Mission code {0} invalid", ""),
	PROCESSOR_CLASS_CREATED							(4539, Level.INFO, true, "Processor class {0} created for mission {1}", ""),
	PROCESSOR_CLASS_DATA_MISSING					(4540, Level.ERROR, false, "Processor class data not set", ""),
	PROCESSOR_CLASS_DELETED							(4541, Level.INFO, true, "Processor class with id {0} deleted", ""),
	PROCESSOR_CLASS_HAS_CONF						(4542, Level.ERROR, false, "Processor class for mission {0} and processor name {1} cannot be deleted, because it has configurations", ""),
	PROCESSOR_CLASS_HAS_PROC						(4543, Level.ERROR, false, "Processor class for mission {0} and processor name {1} cannot be deleted, because it has processor versions", ""),
	PROCESSOR_CLASS_ID_MISSING						(4544, Level.ERROR, false, "Processor class ID not set", ""),
	PROCESSOR_CLASS_ID_NOT_FOUND					(4545, Level.ERROR, false, "No processor class found with ID {0}", ""),
	PROCESSOR_CLASS_INVALID							(4546, Level.ERROR, false, "Processor class {0} invalid for mission {1}", ""),
	PROCESSOR_CLASS_LIST_RETRIEVED					(4547, Level.INFO, true, "Processor class(es) for mission {0} and processor name {1} retrieved", ""),
	PROCESSOR_CLASS_MISSING							(4548, Level.ERROR, false, "Processor class not set", ""),
	PROCESSOR_CLASS_MODIFIED						(4549, Level.INFO, true, "Processor class with id {0} modified", ""),
	PROCESSOR_CLASS_NOT_FOUND						(4550, Level.ERROR, false, "No processor class found for mission {0} and processor name {1}", ""),
	PROCESSOR_CLASS_NOT_MODIFIED					(4551, Level.INFO, true, "Processor class with id {0} not modified (no changes)", ""),
	PROCESSOR_CLASS_RETRIEVED						(4552, Level.INFO, true, "Processor class with ID {0} retrieved", ""),
	PROCESSOR_CREATED								(4553, Level.INFO, true, "Processor {0}, version {0} created for mission {1}", ""),
	PROCESSOR_DATA_MISSING							(4554, Level.ERROR, false, "Processor data not set", ""),
	PROCESSOR_DELETED								(4555, Level.INFO, true, "Processor with id {0} deleted", ""),
	PROCESSOR_HAS_CONFIG							(4556, Level.ERROR, false, "Processor for mission {0} with processor name {1} and processor version {2} cannot be deleted, because it has configured processors", ""),
	PROCESSOR_ID_MISSING							(4557, Level.ERROR, false, "Processor ID not set", ""),
	PROCESSOR_ID_NOT_FOUND							(4558, Level.ERROR, false, "No processor found with ID {0}", ""),
	PROCESSOR_INVALID								(4559, Level.ERROR, false, "Processor {0} with version {1} invalid for mission {2}", ""),
	PROCESSOR_LIST_RETRIEVED						(4560, Level.INFO, true, "Processor(s) for mission {0}, processor name {1} and processor version {2} retrieved", ""),
	PROCESSOR_MISSING								(4561, Level.ERROR, false, "Processor not set", ""),
	PROCESSOR_MODIFIED								(4562, Level.INFO, true, "Processor with id {0} modified", ""),
	PROCESSOR_NAME_MISSING							(4563, Level.ERROR, false, "Processor name not set", ""),
	PROCESSOR_NOT_FOUND								(4564, Level.ERROR, false, "No processor found for mission {0}, processor name {1} and processor version {2}", ""),
	PROCESSOR_NOT_MODIFIED							(4565, Level.INFO, true, "Processor with id {0} not modified (no changes)", ""),
	PROCESSOR_RETRIEVED								(4566, Level.INFO, true, "Processor with ID {0} retrieved", ""),
	PRODUCT_CLASS_INVALID							(4567, Level.ERROR, false, "Product type {0} invalid for mission {1}", ""),
	WORKFLOW_MISSING								(4568, Level.ERROR, false, "Workflow not set", ""),
	WORKFLOW_LIST_EMPTY								(4569, Level.ERROR, false, "No workflow found for search criteria", ""),
	WORKFLOW_LIST_RETRIEVED							(4570, Level.INFO, true, "Workflow list of size {0} retrieved for mission {1}, workflow name {2}, workflow version {3}, output product class {3}, configured processor {4}, and enabled = {6}", ""),
	MISSION_CODE_MISSING							(4571, Level.ERROR, false, "No mission code given", ""),
	WORKFLOW_ID_MISSING								(4572, Level.ERROR, false, "Workflow ID not set", ""),
	WORKFLOW_ID_NOT_FOUND							(4573, Level.ERROR, false, "No workflow found with ID {0}", ""),
	WORKFLOW_DELETED								(4574, Level.INFO, true, "Workflow with id {0} deleted", ""),
	WORKFLOW_DELETION_UNSUCCESSFUL					(4575, Level.ERROR, false, "Workflow deletion unsuccessful for ID {0}", ""),
	WORKFLOW_RETRIEVED								(4576, Level.INFO, true, "Workflow with ID {0} retrieved", ""),
	NO_WORKFLOW_FOUND								(4577, Level.ERROR, false, "No workflow found for mission {0}, name {1}, version {2}, output product class {3}, configured processor {4}, and enabled = {5}", ""),
	WORKFLOW_NAME_MISSING							(4578, Level.ERROR, false, "Workflow name not set", ""),
	DUPLICATE_WORKFLOW								(4579, Level.ERROR, false, "Duplicate workflow for mission {0}, workflow name {1}, workflow version {2}, uuid {3}", ""),
	WORKFLOW_CREATED								(4580, Level.INFO, true, "Workflow {0}, version {1} created for mission {2}", ""),
	FIELD_NOT_SET									(4581, Level.ERROR, false, "{0} is/are mandatory", ""),
	PROCESSOR_PRODUCT_MISMATCH						(4582, Level.ERROR, false, "The specified processor class must be able to produce the specified output product class", ""),
	WORKFLOW_OPTION_MISMATCH						(4583, Level.ERROR, false, "Workflow reference {0}/{1} in workflow option does not match workflow {2}/{3}", ""),
	FIELD_MISSSPECIFIED								(4584, Level.ERROR, false, "No {0} found for mission code {1} and {0} \"{2}\"", ""),
	WORKFLOW_MODIFIED								(4585, Level.INFO, true, "Workflow with id {0} modified", ""),
	WORKFLOW_NOT_MODIFIED							(4586, Level.INFO, true, "Workflow with id {0} not modified (no changes)", ""),
	WORKFLOW_DATA_MISSING							(4587, Level.ERROR, false, "Workflow data not set", ""),
	CONCURRENT_WORKFLOW_UPDATE						(4588, Level.ERROR, false, "The workflow with ID {0} has been modified since retrieval by the client", ""),
	RANGE_MUST_CONTAIN_DEFAULT						(4589, Level.ERROR, false, "The default value \"{0}\" of workflow option {1} is not contained in the value range", ""),
	WORKFLOWS_COUNTED								(4590, Level.INFO, true, "{0} workflow(s) found for mission {1}, workflow name {2}, workflow version {3}, output product class {4}, configured processor {5}, and enabled = {6}", ""),
	CONFIGURATIONS_COUNTED							(4591, Level.INFO, true, "{0} configuration(s) found for mission {1}, processor name {2}, configuration version {3}", ""),
	CONFIGURED_PROCESSORS_COUNTED					(4592, Level.INFO, true, "{0} configured processor(s) found for mission {1}, processor name {2}, processor version {3}, configuration version {4}", ""),
	PROCESSORS_COUNTED								(4593, Level.INFO, true, "{0} processor(s) found for mission {1}, processor name {2}, processor version {3}", ""),
	PROCESSOR_CLASSES_COUNTED						(4594, Level.INFO, true, "{0} processor classe(s) found for mission {1}, processor name {2}", ""),
	ILLEGAL_OPTION_TYPE								(4595, Level.ERROR, false, "{0} is not a legal WorkflowOptionType (must be STRING, NUMBER, or DATENUMBER)", ""),
	PRODUCT_CLASS_NOT_FOUND							(4596, Level.ERROR, false, "Product class {0} was not found during input filter creation", "")
	
	;

	private final int code;
	private final Level level;
	private final boolean success;
	private final String message;
	private final String description;

	private ProcessorMgrMessage(int code, Level level, boolean success, String message, String description) {
		this.level = level;
		this.code = code;
		this.success = success;
		this.message = message;
		this.description = description;
	}

	/**
	 * Get the message's code.
	 *
	 * @return The message code.
	 */
	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get the message's level.
	 *
	 * @return The message level.
	 */
	@Override
	public Level getLevel() {
		return level;
	}

	/**
	 * Get the message.
	 *
	 * @return The message.
	 */
	@Override
	public String getMessage() {
		return message;
	}

	/**
	 * Get a more detailed description of the message's purpose.
	 *
	 * @return A description of the message.
	 */
	@Override
	public String getDescription() {
		return description;
	}
	
	/**
	 * Get the message's success.
	 * 
	 * @return The message's success.
	 */
	public boolean getSuccess() {
		return success;
	}
}
