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

	INVALID_FACILITY					(2051, Level.INFO, "Invalid processing facility {0} for ingestion", ""),
	PRODUCTS_INGESTED					(2058, Level.INFO, "{0} products ingested in processing facility {1}", ""),
	AUTH_MISSING_OR_INVALID				(2056, Level.ERROR, "Basic authentication missing or invalid: {0}", ""),
	NOTIFICATION_FAILED					(2091, Level.WARN, "Notification of Production Planner failed (cause: {0})", ""),
	ERROR_ACQUIRE_SEMAPHORE				(2092, Level.ERROR,	"Error to acquire semaphore from prosEO Production Planner (cause: {0})", ""),
	ERROR_RELEASE_SEMAPHORE				(2093, Level.ERROR,	"Error to release semaphore from prosEO Production Planner (cause: {0})", ""),
	PRODUCT_UUID_INVALID				(2021, Level.ERROR, "Product UUID {0} invalid", ""),
	INVALID_PARAMETER_VALUE				(2049, Level.ERROR, "Invalid parameter value {0} for type {1}", ""),
	INVALID_PARAMETER_TYPE				(2048, Level.ERROR, "Invalid parameter type {0}", ""),
	INVALID_PRODUCT_GENERATION_TIME		(2047, Level.ERROR, "Invalid product generation time {0}", ""),
	INVALID_SENSING_STOP_TIME			(2046, Level.ERROR, "Invalid sensing stop time {0}", ""),
	INVALID_SENSING_START_TIME			(2045, Level.ERROR, "Invalid sensing start time {0}", ""),
	INVALID_PRODUCTION_TYPE				(2050, Level.ERROR, "Invalid production type {0}", ""),
	INVALID_RAW_DATA_AVAILABILITY_TIME	(2072, Level.ERROR, "Invalid raw data availability time {0}", ""),
	INVALID_PUBLICATION_TIME			(2073, Level.ERROR, "Invalid publication time {0}", ""),
	INVALID_EVICTION_TIME				(2074, Level.ERROR, "Invalid eviction time {0}", ""),
	PRODUCT_NOT_FOUND					(2001, Level.ERROR, "No product found for ID {0}", ""),
	ERROR_STORING_PRODUCT				(2052, Level.ERROR, "Error storing product of class {0} at processing facility {1} (Storage Manager cause: {2})", ""),
	PRODUCT_FILE_EXISTS					(2062, Level.ERROR, "Product file for processing facility {0} exists", ""),
	ERROR_NOTIFYING_PLANNER				(2054, Level.ERROR, "Error notifying prosEO Production Planner of new product {0} of type {1} (Production Planner cause: {2})", ""),
	PRODUCT_INGESTION_FAILED			(2055, Level.ERROR, "Product ingestion failed (cause: {0})", ""),
	UNEXPECTED_NUMBER_OF_FILE_PATHS		(2057, Level.ERROR, "Unexpected number of file paths ({0}, expected: {0}) received from Storage Manager at {1}", ""),
	NO_PRODUCT_FILES					(2060, Level.ERROR, "No product files found for product ID {0}", ""),
	NO_PRODUCT_FILES_AT_FACILITY		(2061, Level.ERROR, "No product file found for product ID {0} at processing facility {1}", ""),
	PRODUCT_FILE_NOT_FOUND				(2064, Level.ERROR, "Product file for processing facility {0} not found", ""),
	CONCURRENT_UPDATE					(2065, Level.ERROR, "The product file for product ID {0} and processing facility {1} has been modified since retrieval by the client", ""),
	DELETION_UNSUCCESSFUL				(2069, Level.ERROR, "Deletion unsuccessful for product file {0} in product with ID {1}", ""),	
	ERROR_DELETING_PRODUCT				(2070, Level.ERROR, "Error deleting product with ID {0} from processing facility {1} (cause: {2})"	, ""),
	PRODUCT_QUERY_EXISTS				(2071, Level.ERROR, "Product with ID {0} is required for at least one job step on processing facility {1}", ""),
	PRODUCT_CLASS_INVALID				(2012, Level.ERROR, "Product type {0} invalid", ""),
	NEW_PRODUCT_ADDED					(2053, Level.INFO, "New product with ID {0} and product type {1} added to database", ""),
	PRODUCT_FILE_RETRIEVED				(2059, Level.INFO, "Product file retrieved for product ID {0} at processing facility {1}", ""),
	PRODUCT_FILE_INGESTED				(2063, Level.INFO, "Product file {0} ingested for product ID {1} at processing facility {2}", ""),
	PRODUCT_FILE_MODIFIED				(2066, Level.INFO, "Product file {0} for product with id {1} modified", ""),
	PRODUCT_FILE_NOT_MODIFIED			(2067, Level.INFO, "Product file {0} for product with id {1} not modified (no changes)", ""),
	PRODUCT_FILE_DELETED				(2068, Level.INFO, "Product file {0} for product with id {1} deleted", ""),
	NUMBER_PRODUCT_FILES_DELETED		(2072, Level.INFO, "{0} product files deleted", ""),
	PRODUCT_MISSING						(2000, Level.ERROR, "Product not set", ""),
	PRODUCT_ID_MISSING					(2013, Level.ERROR, "Product ID not set", ""),
	PRODUCT_LIST_EMPTY					(2011, Level.ERROR, "No products found for search criteria", ""),
	ENCLOSING_PRODUCT_NOT_FOUND			(2002, Level.ERROR, "Enclosing product with ID {0} not found", ""),
	COMPONENT_PRODUCT_NOT_FOUND			(2003, Level.ERROR, "Component product with ID {0} not found", ""),
	ORBIT_NOT_FOUND						(2019, Level.ERROR, "Orbit {0} for spacecraft {1} not found", ""),
	FILE_CLASS_INVALID					(2014, Level.ERROR, "File class {0} invalid for mission {1}", ""),
	MODE_INVALID						(2015, Level.ERROR, "Processing mode {0} invalid for mission {1}", ""),
	COMPONENT_PRODUCT_CLASS_INVALID		(2017, Level.ERROR, "Component product class {0} invalid for product class {1} in mission {2}", ""),
	ENCLOSING_PRODUCT_CLASS_INVALID		(2018, Level.ERROR, "Enclosing product class {0} invalid for product class {1} in mission {2}", ""),
	PRODUCT_UUID_MISSING				(2020, Level.ERROR, "Product UUID not set", ""),
	PRODUCT_NOT_FOUND_BY_UUID			(2022, Level.ERROR, "No product found for UUID {0}", ""),
	DUPLICATE_PRODUCT_UUID				(2024, Level.ERROR, "Duplicate product UUID {0}", ""),
	CONFIGURED_PROCESSOR_NOT_FOUND		(2025, Level.ERROR, "Configured processor {0} not found", ""),
	PRODUCT_HAS_FILES					(2026, Level.ERROR, "Product with ID {0} (or some component product) has existing files and cannot be deleted", ""),
	PRODUCT_EXISTS						(2027, Level.ERROR, "Product with equal characteristics already exists with ID {0}", ""),
	VISIBILITY_VIOLATION				(2029, Level.ERROR, "User not authorized for read access to product class {0}", ""),
	PRODUCT_NOT_AVAILABLE				(2030, Level.ERROR, "Product with ID {0} not available on any Processing Facility", ""),
	PRODUCTFILE_NOT_AVAILABLE			(2034, Level.ERROR, "Product with ID {0} has no file named {1}", ""),
	PRODUCT_DELETED						(2005, Level.INFO, " Product with id {0} deleted", ""),
	PRODUCT_LIST_RETRIEVED				(2006, Level.INFO, " Product list of size {0} retrieved for mission {1}, product classes {2}, start time {3}, stop time {4}", ""),
	PRODUCT_CREATED						(2007, Level.INFO, " Product of type {0} created for mission {1}", ""),
	PRODUCT_RETRIEVED_BY_UUID			(2023, Level.INFO, " Product with UUID {0} retrieved", ""),
	PRODUCT_MODIFIED					(2009, Level.INFO, " Product with id {0} modified", ""),
	PRODUCT_NOT_MODIFIED				(2010, Level.INFO, " Product with id {0} not modified (no changes)", ""),
	PRODUCT_RETRIEVED					(2008, Level.INFO, " Product with ID {0} retrieved", ""),
	PRODUCT_DOWNLOAD_REQUESTED			(2031, Level.INFO, " Download link for product with ID {0} provided", ""),
	PRODUCT_DOWNLOAD_TOKEN_REQUESTED	(2032, Level.INFO, " Download token for product with ID {0} and file name {1} provided", ""),
	INVALID_TIMEOUT						(2079, Level.WARN, "Invalid timeout value {0} found in configuration, using default {1}",""),
	INVALID_VALIDITY					(2080, Level.WARN, "Invalid token validity value {0} found in configuration, using default {1}", ""),
	INITIALIZE_AUTHENTICATION			(2081, Level.INFO, "Initializing authentication from user details service",""),
	INITIALIZE_USER_INFO				(2081, Level.INFO, "Initializing user details service from datasource {0}",""),
	
	;

	private final int code;
	private final Level level;
	private final String message;
	private final String description;

	private IngestorMessage(int code, Level level, String message, String description) {
		this.level = level;
		this.code = code;
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
	 * Get a more detailed description of the message's purpose.
	 * 
	 * @return A description of the message.
	 */
	public String getDescription() {
		return description;
	}

}
