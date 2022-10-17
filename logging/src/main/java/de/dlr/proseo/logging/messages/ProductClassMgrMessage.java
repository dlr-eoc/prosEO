/**
 * ProductClassMgrMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the product class manager.
 *
 * @author Katharina Bassler
 */
public enum ProductClassMgrMessage implements ProseoMessage {

	COMPONENT_CLASS_CYCLE				(5001, Level.ERROR, false, "Component product class {0} for product class {1} would create a product class cycle for mission {2}", ""),
	CONCURRENT_RULE_UPDATE				(5002, Level.ERROR, false, "The selection rule with ID {0} has been modified since retrieval by the client", ""),
	CONCURRENT_UPDATE					(5003, Level.ERROR, false, "The product class with ID {0} has been modified since retrieval by the client", ""),
	DELETION_UNSUCCESSFUL				(5004, Level.ERROR, false, "Product class deletion unsuccessful for ID {0}", ""),
	DUPLICATE_RULE						(5005, Level.ERROR, false, "Product class {0} already contains selection rule for source class {1}, mode {2} and configured processor {3}", ""),
	ENCLOSING_CLASS_CYCLE				(5006, Level.ERROR, false, "Enclosing product class {0} for product class {1} would create a product class cycle for mission {2}", ""),
	EXACTLY_ONE_SELECTION_RULE_EXPECTED (5007, Level.ERROR, false, "Exactly one simple selection rule expected", "Sub-message for INVALID_RULE_STRING"),
	INVALID_COMPONENT_CLASS				(5008, Level.ERROR, false, "Component product class {0} is not defined for mission {1}", ""),
	INVALID_ENCLOSING_CLASS				(5009, Level.ERROR, false, "Enclosing product class {0} is not defined for mission {1}", ""),
	INVALID_MISSION_CODE				(5010, Level.ERROR, false, "Invalid mission code {0}", ""),
	INVALID_PARAMETER_KEY				(5011, Level.ERROR, false, "Parameter key missing in filter condition {0}", ""),
	INVALID_PARAMETER_TYPE				(5012, Level.ERROR, false, "Invalid parameter type {0} in filter condition, one of {STRING, INTEGER, BOOLEAN, DOUBLE} expected", ""),
	INVALID_POLICY_TYPE					(5013, Level.ERROR, false, "Invalid policy type {0} in selection rule, see Generic IPF Interface Specifications for valid values", ""),
	INVALID_PROCESSING_MODE				(5014, Level.ERROR, false, "Processing mode {0} not defined for mission {1}", ""),
	INVALID_PROCESSOR					(5015, Level.ERROR, false, "Configured processor {0} is not defined", ""),
	INVALID_PROCESSOR_CLASS				(5016, Level.ERROR, false, "Processor class {0} is not defined for mission {1}", ""),
	INVALID_RULE_STRING					(5017, Level.ERROR, false, "Syntax error in selection rule {0}: {1}", ""),
	INVALID_SOURCE_CLASS				(5018, Level.ERROR, false, "Source product class {0} is not defined for mission {1}", ""),
	INVALID_TIME_UNIT					(5019, Level.ERROR, false, "Invalid time unit {0} in selection rule, one of {DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS} expected", ""),
	NO_RULES_FOUND						(5020, Level.ERROR, false, "No selection rules found for product class {0}", ""),
	NO_RULES_FOUND_FOR_SOURCE			(5021, Level.ERROR, false, "No selection rules found for target product class {1} and source product class {2}", ""),
	PROCESSOR_ADDED						(5022, Level.INFO, true, "Configured processor {0} added to selection rule with ID {1} for product class with ID {2}", ""),
	PROCESSOR_NAME_MISSING				(5023, Level.ERROR, false, "Name of configured processor not set", ""),
	PROCESSOR_NOT_FOUND					(5024, Level.ERROR, false, "Configured processor {0} not found in selection rule with ID {1} for product class with ID {2}", ""),
	PROCESSOR_REMOVED					(5025, Level.INFO, true, "Configured processor {0} removed from selection rule with ID {1} for product class with ID {2}", ""),
	PRODUCT_CLASS_CREATED				(5026, Level.INFO, true, "Product class of type {0} created for mission {1}", ""),
	PRODUCT_CLASS_DATA_MISSING			(5027, Level.ERROR, false, "Product class data not set", ""),
	PRODUCT_CLASS_DELETED				(5028, Level.INFO, true, "Product class with ID {0} deleted", ""),
	PRODUCT_CLASS_EXISTS				(5029, Level.ERROR, false, "Product class {0} already exists for mission {1}", ""),
	PRODUCT_CLASS_HAS_PROCESSOR			(5030, Level.ERROR, false, "Product class for mission {0} with product type {1} cannot be deleted, because it is referenced by a processor class", ""),
	PRODUCT_CLASS_HAS_PRODUCTS			(5031, Level.ERROR, false, "Product class for mission {0} with product type {1} cannot be deleted, because it has products", ""),
	PRODUCT_CLASS_HAS_SELECTION_RULES	(5032, Level.ERROR, false, "Cannot delete product class {0}, because it is referenced by {1} selection rules", ""),
	PRODUCT_CLASS_ID_MISSING			(5033, Level.ERROR, false, "Product class ID not set", ""),
	PRODUCT_CLASS_ID_NOT_FOUND			(5034, Level.ERROR, false, "No product class found with ID {0}", ""),
	PRODUCT_CLASS_LIST_RETRIEVED		(5035, Level.INFO, true, "Product class(es) for mission {0} and product type {1} retrieved", ""),
	PRODUCT_CLASS_MISSING				(5036, Level.ERROR, false, "Product class not set", ""),
	PRODUCT_CLASS_MODIFIED				(5037, Level.INFO, true, "Product class with ID {0} modified", ""),
	PRODUCT_CLASS_NOT_FOUND				(5038, Level.ERROR, false, "Product class with ID {0} not found", ""),
	PRODUCT_CLASS_NOT_FOUND_BY_SEARCH	(5039, Level.ERROR, false, "No product classes found for mission {0} and product type {1}", ""),
	PRODUCT_CLASS_NOT_MODIFIED			(5040, Level.INFO, true, "Product class with ID {0} not modified (no changes)", ""),
	PRODUCT_CLASS_RETRIEVED				(5041, Level.INFO, true, "Product class with ID {0} retrieved", ""),
	PRODUCT_CLASS_SAVE_FAILED			(5042, Level.ERROR, false, "Save failed for product class {0} in mission {1} (cause: {2})", ""),
	PRODUCT_QUERIES_EXIST				(5043, Level.ERROR, false, "Rule '{0}' for product class {1} cannot be deleted, because it is used in product queries", ""),
	RULE_STRING_MISSING					(5044, Level.ERROR, false, "Selection rule missing in selection rule string {0}", ""),
	SELECTION_RULE_DATA_MISSING			(5045, Level.ERROR, false, "Selection rule data not set", ""),
	SELECTION_RULE_DELETED				(5046, Level.INFO, true, "Selection rule with ID {0} for product class with ID {1} deleted", ""),
	SELECTION_RULE_ID_MISSING			(5047, Level.ERROR, false, "Selection rule ID not set", ""),
	SELECTION_RULE_ID_NOT_FOUND			(5048, Level.ERROR, false, "Selection rule with ID {0} not found for product class with ID {1}", ""),
	SELECTION_RULE_LIST_RETRIEVED		(5049, Level.INFO, true, "Selection rules for target product type {0} and source product type {1} retrieved", ""),
	SELECTION_RULE_MODIFIED				(5050, Level.INFO, true, "Selection rule with ID {0} modified", ""),
	SELECTION_RULE_NOT_MODIFIED			(5051, Level.INFO, true, "Selection rule with ID {0} not modified (no changes)", ""),
	SELECTION_RULE_RETRIEVED			(5052, Level.INFO, true, "Selection rule with ID {0} for product class with ID {1} retrieved", ""),
	SELECTION_RULES_CREATED				(5053, Level.INFO, true, "{0} selection rules added to product class of type {1} in mission {2}", ""),

	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private ProductClassMgrMessage(int code, Level level, boolean success, String message, String description) {
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
	 * Get a more detailed description of the message's purpose.
	 *
	 * @return A description of the message.
	 */
	@Override
	public String getDescription() {
		return description;
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
	 * Get the message's success.
	 * 
	 * @return The message's success.
	 */
	public boolean getSuccess() {
		return success;
	}
}
