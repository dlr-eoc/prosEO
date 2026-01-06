/**
 * IngestorMessage.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */package de.dlr.proseo.logging.messages;

import org.slf4j.event.Level;

/**
 * A collection of messages needed by the ingestor.
 *
 * @author Katharina Bassler
 */
public enum IngestorMessage implements ProseoMessage {

	AUTH_MISSING_OR_INVALID				(2000, Level.ERROR, false, "Basic authentication missing or invalid: {0}", ""),
	COMPONENT_PRODUCT_CLASS_INVALID		(2001, Level.ERROR, false, "Component product class {0} invalid for product class {1} in mission {2}", ""),
	COMPONENT_PRODUCT_NOT_FOUND			(2002, Level.ERROR, false, "Component product with ID {0} not found", ""),
	CONCURRENT_UPDATE					(2003, Level.ERROR, false, "The product file for product ID {0} and processing facility {1} has been modified since retrieval by the client", ""),
	CONFIGURED_PROCESSOR_NOT_FOUND		(2004, Level.ERROR, false, "Configured processor {0} not found", ""),
	DELETION_UNSUCCESSFUL				(2005, Level.ERROR, false, "Deletion unsuccessful for product file {0} in product with ID {1}", ""),
	DUPLICATE_PRODUCT_UUID				(2006, Level.ERROR, false, "Duplicate product UUID {0}", ""),
	ENCLOSING_PRODUCT_CLASS_INVALID		(2007, Level.ERROR, false, "Enclosing product class {0} invalid for product class {1} in mission {2}", ""),
	ENCLOSING_PRODUCT_NOT_FOUND			(2008, Level.ERROR, false, "Enclosing product with ID {0} not found", ""),
	ERROR_DELETING_PRODUCT				(2010, Level.ERROR, false, "Error deleting product with ID {0} from processing facility {1} (cause: {2})"	, ""),
	ERROR_NOTIFYING_PLANNER				(2011, Level.ERROR, false, "Error notifying prosEO Production Planner of new product {0} of type {1} (Production Planner cause: {2})", ""),
	ERROR_STORING_PRODUCT				(2013, Level.ERROR, false, "Error storing product of class {0} at processing facility {1} (Storage Manager cause: {2})", ""),
	FILE_CLASS_INVALID					(2014, Level.ERROR, false, "File class {0} invalid for mission {1}", ""),
	INITIALIZE_AUTHENTICATION			(2015, Level.INFO, true, "Initializing authentication from user details service",""),
	INITIALIZE_USER_INFO				(2016, Level.INFO, true, "Initializing user details service from datasource {0}",""),
	INVALID_EVICTION_TIME				(2017, Level.ERROR, false, "Invalid eviction time {0}", ""),
	INVALID_FACILITY					(2018, Level.ERROR, false, "Invalid processing facility {0} for ingestion", ""),
	INVALID_PARAMETER_TYPE				(2019, Level.ERROR, false, "Invalid parameter type {0}", ""),
	INVALID_PARAMETER_VALUE				(2020, Level.ERROR, false, "Invalid parameter value {0} for type {1}", ""),
	INVALID_PRODUCT_GENERATION_TIME		(2021, Level.ERROR, false, "Invalid product generation time {0}", ""),
	INVALID_PRODUCTION_TYPE				(2022, Level.ERROR, false, "Invalid production type {0}", ""),
	INVALID_PUBLICATION_TIME			(2023, Level.ERROR, false, "Invalid publication time {0}", ""),
	INVALID_RAW_DATA_AVAILABILITY_TIME	(2024, Level.ERROR, false, "Invalid raw data availability time {0}", ""),
	INVALID_SENSING_START_TIME			(2025, Level.ERROR, false, "Invalid sensing start time {0}", ""),
	INVALID_SENSING_STOP_TIME			(2026, Level.ERROR, false, "Invalid sensing stop time {0}", ""),	
	INVALID_TIMEOUT						(2027, Level.WARN, true, "Invalid timeout value {0} found in configuration, using default {1}",""),
	INVALID_VALIDITY					(2028, Level.WARN, true, "Invalid token validity value {0} found in configuration, using default {1}", ""),
	MODE_INVALID						(2029, Level.ERROR, false, "Processing mode {0} invalid for mission {1}", ""),
	NEW_PRODUCT_ADDED					(2030, Level.INFO, true, "New product with ID {0} and product type {1} added to database", ""),
	NO_PRODUCT_FILES					(2031, Level.ERROR, false, "No product files found for product ID {0}", ""),
	NO_PRODUCT_FILES_AT_FACILITY		(2032, Level.ERROR, false, "No product file found for product ID {0} at processing facility {1}", ""),
	PLANNER_NOTIFICATION_FAILED			(2034, Level.WARN, true, "Notification of Production Planner failed (cause: {0})", ""),
	NUMBER_PRODUCT_FILES_DELETED		(2035, Level.INFO, true, "{0} product files deleted", ""),
	ORBIT_NOT_FOUND						(2036, Level.ERROR, false, "Orbit {0} for spacecraft {1} not found", ""),
	PRODUCT_CLASS_INVALID				(2037, Level.ERROR, false, "Product type {0} invalid", ""),
	PRODUCT_CREATED						(2038, Level.INFO, true, " Product of type {0} created for mission {1}", ""),
	PRODUCT_DELETED						(2039, Level.INFO, true, " Product with id {0} deleted", ""),
	PRODUCT_DOWNLOAD_REQUESTED			(2040, Level.INFO, true, " Download link for product with ID {0} provided", ""),
	PRODUCT_DOWNLOAD_TOKEN_REQUESTED	(2041, Level.INFO, true, " Download token for product with ID {0} and file name {1} provided", ""),
	PRODUCT_EXISTS						(2042, Level.ERROR, false, "Product with equal characteristics already exists with ID {0}", ""),
	PRODUCT_FILE_DELETED				(2043, Level.INFO, true, "Product file {0} for product with id {1} deleted", ""),
	PRODUCT_FILE_EXISTS					(2044, Level.ERROR, false, "Product file {0} exists at processing facility {0}", ""),
	PRODUCT_FILE_INGESTED				(2045, Level.INFO, true, "Product file {0} ingested for product ID {1} at processing facility {2}", ""),
	PRODUCT_FILE_MODIFIED				(2046, Level.INFO, true, "Product file {0} for product with id {1} modified", ""),
	PRODUCT_FILE_NOT_FOUND				(2047, Level.ERROR, false, "Product file for processing facility {0} not found", ""),
	PRODUCT_FILE_NOT_MODIFIED			(2048, Level.INFO, true, "Product file {0} for product with id {1} not modified (no changes)", ""),
	PRODUCT_FILE_RETRIEVED				(2049, Level.INFO, true, "Product file retrieved for product ID {0} at processing facility {1}", ""),
	PRODUCT_HAS_FILES					(2050, Level.ERROR, false, "Product with ID {0} (or some component product) has existing files and cannot be deleted", ""),
	PRODUCT_ID_MISSING					(2051, Level.ERROR, false, "Product ID not set", ""),
	PRODUCT_INGESTION_FAILED			(2052, Level.ERROR, false, "Product ingestion failed (cause: {0})", ""),
	PRODUCT_LIST_EMPTY					(2053, Level.ERROR, false, "No products found for search criteria", ""),
	PRODUCT_LIST_RETRIEVED				(2054, Level.INFO, true, " Product list of size {0} retrieved for mission {1}, product classes {2}, start time {3}, stop time {4}", ""),
	PRODUCT_MISSING						(2055, Level.ERROR, false, "Product not set", ""),
	PRODUCT_MODIFIED					(2056, Level.INFO, true, " Product with id {0} modified", ""),
	PRODUCT_NOT_AVAILABLE				(2057, Level.ERROR, false, "Product with ID {0} not available on any Processing Facility", ""),
	PRODUCT_NOT_FOUND					(2058, Level.ERROR, false, "No product found for ID {0}", ""),
	PRODUCT_NOT_FOUND_BY_UUID			(2059, Level.ERROR, false, "No product found for UUID {0}", ""),
	PRODUCT_NOT_MODIFIED				(2060, Level.INFO, true, " Product with id {0} not modified (no changes)", ""),
	PRODUCT_QUERY_EXISTS				(2061, Level.ERROR, false, "Product with ID {0} is required for at least one job step on processing facility {1}", ""),
	PRODUCT_RETRIEVED					(2062, Level.INFO, true, " Product with ID {0} retrieved", ""),
	PRODUCT_RETRIEVED_BY_UUID			(2063, Level.INFO, true, " Product with UUID {0} retrieved", ""),
	PRODUCT_UUID_INVALID				(2064, Level.ERROR, false, "Product UUID {0} invalid", ""),
	PRODUCT_UUID_MISSING				(2065, Level.ERROR, false, "Product UUID not set", ""),
	PRODUCTFILE_NOT_AVAILABLE			(2066, Level.ERROR, false, "Product with ID {0} has no file named {1}", ""),
	PRODUCTS_INGESTED					(2067, Level.INFO, true, "{0} products ingested in processing facility {1}", ""),
	UNEXPECTED_NUMBER_OF_FILE_PATHS		(2068, Level.ERROR, false, "Unexpected number of file paths ({0}, expected: {1}) received from Storage Manager at {2}", ""),
	VISIBILITY_VIOLATION				(2069, Level.ERROR, false, "User not authorized for read access to product class {0}", ""),
	PRODUCT_LIST_MISSING				(2070, Level.ERROR, false, "No product list given for ingestion", ""),
	ORDERGEN_NOTIFICATION_FAILED		(2071, Level.WARN, true, "Notification of Order Generator failed (cause: {0})", ""),
	ERROR_NOTIFYING_ORDERGEN			(2072, Level.ERROR, false, "Error notifying prosEO Order Generator of new product {0} of type {1} (Production Planner cause: {2})", ""),
	
	;

	private final int code;
	private final String description;
	private final Level level;
	private final String message;
	private final boolean success;

	private IngestorMessage(int code, Level level, boolean success, String message, String description) {
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
	public int getCode() {
		return code;
	}

	/**
	 * Get a more detailed description of the message's purpose.
	 * 
	 * @return A description of the message.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get the message's level.
	 * 
	 * @return The message level.
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * Get the message.
	 * 
	 * @return The message.
	 */
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
