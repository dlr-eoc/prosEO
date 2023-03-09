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
