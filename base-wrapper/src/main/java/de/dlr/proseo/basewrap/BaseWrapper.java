/**
 * BaseWrapper.java
 *
 *  (C) 2019 Hubert Asamer, DLR
 *  (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.basewrap;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.basewrap.rest.HttpResponseInfo;
import de.dlr.proseo.basewrap.rest.RestOps;
import de.dlr.proseo.interfaces.rest.model.IngestorProduct;
import de.dlr.proseo.interfaces.rest.model.RestFileInfo;
import de.dlr.proseo.interfaces.rest.model.RestProduct;
import de.dlr.proseo.interfaces.rest.model.RestProductFile;
import de.dlr.proseo.model.enums.JobOrderVersion;
import de.dlr.proseo.model.enums.StorageType;
import de.dlr.proseo.model.joborder.InputOutput;
import de.dlr.proseo.model.joborder.IpfFileName;
import de.dlr.proseo.model.joborder.JobOrder;
import de.dlr.proseo.model.joborder.Proc;
import de.dlr.proseo.model.joborder.TimeInterval;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.model.util.ProseoUtil;

/**
 * prosEO Base Processor Wrapper - for processors conforming to ESA's "Generic
 * IPF Interface Specification" (MMFI-GSEG-EOPG-TN-07-0003, V.1.8)
 *
 * @author Hubert Asamer
 * @author Dr. Thomas Bassler *
 */
public class BaseWrapper {

	/** Exit code for successful completion */
	private static final int EXIT_CODE_OK = 0;
	/** Exit code for completion with warning */
	private static final int EXIT_CODE_WARNING = 127;
	/** Exit code for failure */
	private static final int EXIT_CODE_FAILURE = 255;
	/** Exit code explanation for successful completion */
	private static final String EXIT_TEXT_OK = "OK";
	/** Exit code explanation for failure */
	private static final String EXIT_TEXT_FAILURE = "FAILURE";
	/** CallBack-Message for failure */
	private static final String CALLBACK_STATUS_FAILURE = "FAILURE";
	/** CallBack-Message for success */
	private static final String CALLBACK_STATUS_SUCCESS = "SUCCESS";

	/** Current directory of this program is used as work-dir */
	private static final Path WORKING_DIR = Paths.get(System.getProperty("user.dir"));

	/**
	 * Order ID for job order file (according to Generic IPF Interface
	 * Specifications, sec. 4.2.2); limited to 31 bits (int), because some IPFs
	 * cannot handle larger order IDs (apparently they take the word "integer"
	 * literally)
	 */
	protected static final int JOB_ORDER_ID = (int) (Math.random() * Integer.MAX_VALUE);

	/** Current timestamp used for output-file prefixes */
	@Deprecated
	protected static final long WRAPPER_TIMESTAMP = JOB_ORDER_ID;

	/**
	 * Auto-created path/filename of JobOrderFile within container (according to
	 * Generic IPF Interface Specifications)
	 */
	protected String CONTAINER_JOF_PATH = WORKING_DIR.toString() + File.separator + "JobOrder." + JOB_ORDER_ID + ".xml";
	protected String REL_CONTAINER_JOF_PATH = CONTAINER_JOF_PATH;

	/**
	 * Directory prefix of produced output data (available for wrapper subclasses)
	 */
	protected static final String CONTAINER_OUTPUTS_PATH_PREFIX = "processor" + File.separator + String.valueOf(JOB_ORDER_ID);

	/* Message strings */
	private static final String MSG_CHECKING_ENVIRONMENT = "Checking {} environment variables:";
	private static final String MSG_DIFFERENT_FILE_PATHS_ASSIGNED = "Different file paths assigned by Storage Manager for files of same product ID {}";
	private static final String MSG_DIFFERENT_STORAGE_TYPES_ASSIGNED = "Different storage types assigned by Storage Manager for files of same product ID {}";
	private static final String MSG_DIRECTORY_NOT_EMPTY = "Output directory {} is not empty";
	private static final String MSG_ENVIRONMENT_CHECK_PASSED = "Check of environment variables passed";
	private static final String MSG_ENVIRONMENT_CHECK_FAILED = "Check of environment variables failed";
	private static final String MSG_ERROR_CALLING_PLANNER = "Error calling Production Planner, HTTP status code {} (cause: {})";
	private static final String MSG_ERROR_CONVERTING_INGESTOR_PRODUCT = "Error converting ingestor product with ID {} to JSON (cause: {})";
	private static final String MSG_ERROR_PUSHING_OUTPUT_FILE = "Error pushing output file {}, HTTP status code {} (cause: {})";
	private static final String MSG_ERROR_REGISTERING_PRODUCT = "Error registering product with ID {} with Ingestor, HTTP status code {} (cause: {})";
	private static final String MSG_ERROR_RETRIEVING_INPUT_FILE = "Error retrieving input file {}, HTTP status code {} (cause: {})";
	private static final String MSG_ERROR_RETRIEVING_JOB_ORDER = "Error retrieving Job Order File, HTTP status code {} (cause: {})";
	private static final String MSG_EXCEPTION_RETRIEVING_JOB_ORDER = "Exception encountered retrieving Job Order File (cause: {})";
	private static final String MSG_ERROR_PARSING_JOB_ORDER = "Error parsing Job Order File";
	private static final String MSG_ERROR_RUNNING_PROCESSOR = "Error running processor (cause: {})";
	private static final String MSG_ERROR_WRITING_JOF = "Error writing Job Order document to XML file";
	private static final String MSG_FETCHED_INPUT_FILES = "Fetched {} input files and prepared directories for {} outputs -- Ready for processing using Container-JOF {}";
	private static final String MSG_INVALID_VALUE_OF_ENVVAR = "Invalid value of EnvVar: {}";
	private static final String MSG_LEAVING_BASE_WRAPPER = "Leaving base-wrapper with exit code {} ({})";
	private static final String MSG_MALFORMED_RESPONSE_FROM_STORAGE_MANAGER = "Malformed response {} from Storage Manager when pushing {}";
	private static final String MSG_NOT_A_DIRECTORY = "Output path {} is not a directory";
	private static final String MSG_PLANNER_RESPONSE = "Production Planner response for callback is {} ({})";
	private static final String MSG_PREFIX_TIMESTAMP_FOR_NAMING = "Prefix timestamp used for JobOrderFile naming and results is {}";
	private static final String MSG_PROCESSING_FINISHED_OK = "Processing finished with return code {} (OK)";
	private static final String MSG_PROCESSING_FINISHED_WARNING = "Processing finished with return code {} (WARNING)";
	private static final String MSG_PROCESSING_FINISHED_ERROR = "Processing finished with return code {} (ERROR)";
	private static final String MSG_PRODUCT_ID_NOT_PARSEABLE = "Product ID {} not parseable as long integer";
	private static final String MSG_PRODUCTS_REGISTERED = "{} products registered with Ingestor";
	private static final String MSG_PRODUCTS_UPLOADED = "{} products with {} files uploaded to Storage Manager";
	private static final String MSG_REGISTERING_PRODUCTS = "Registering {} products with prosEO-Ingestor {}";
	private static final String MSG_STARTING_BASE_WRAPPER = "\n\n{\"prosEO\" : \"A Processing System for Earth Observation Data\"}\nStarting base-wrapper with JobOrder file {}";
	private static final String MSG_STARTING_PROCESSOR = "Starting Processing using command {} and local JobOrderFile: {}";
	private static final String MSG_UNABLE_TO_CREATE_DIRECTORY = "Unable to create directory path {} (cause: {} / {})";
	private static final String MSG_UNABLE_TO_ACCESS_FILE = "Unable to access file {} (cause: {} / {})";
	private static final String MSG_FILE_NOT_FETCHED = "Requested file {} not copied";
	private static final String MSG_UNABLE_TO_DELETE_DIRECTORY = "Unable to delete directory/file path {} (cause: {})";
	private static final String MSG_UPLOADING_RESULTS = "Uploading results to Storage Manager";
	private static final String MSG_CANNOT_CALCULATE_CHECKSUM = "Cannot calculate MD5 checksum for product {} (cause: {} / {})";
	private static final String MSG_MORE_THAN_ONE_ZIP_ARCHIVE = "More than one ZIP archive given for product {}";
	private static final String MSG_SKIPPING_INPUT_ENTRY = "Skipping input entry of type {} with filename type {}";
	private static final String MSG_WARNING_INPUT_FILENAME_MISSING = "Skipping input entry of type {} without filename";
	private static final String MSG_PROCESSOR_EXECUTION_INTERRUPTED = "Processor execution interrupted (cause: {})";
	private static final String MSG_ERROR_IN_PLANNER_CALLBACK = "Error calling back Production Planner at endpoint {} (cause: {})";
	private static final String MSG_CANNOT_DETERMINE_FILE_SIZE = "Cannot determine file size for path {} (cause: {} / {}";
	private static final String MSG_WRAPPER_CANNOT_BE_LAUNCHED = "Requested wrapper class {} cannot be launched (cause: {})";
	private static final String MSG_WRAPPER_NOT_SUBCLASS_OF_BASE_WRAPPER = "Requested wrapper class {} is not a subclass of BaseWrapper";
	private static final String MSG_WRAPPER_CLASS_NOT_FOUND = "Requested wrapper class {} not found";
	private static final String MSG_CANNOT_PARSE_FILE_INFO = "Cannot parse file info response {} (cause: {} / {})";
	private static final String MSG_PRODUCT_ID_MISSING = "Product ID missing in output of file type {}, product cannot be updated";
	private static final String MSG_ERROR_READING_PRODUCT = "Error reading product with ID {} from database (HTTP status code: {}, message: {})";
	private static final String MSG_ERROR_PARSING_PRODUCT = "Error converting HTTP response {} to REST product (cause: {})";
	private static final String MSG_ERROR_CONVERTING_PRODUCT = "Error converting REST product of class {} to JSON (cause: {})";
	private static final String MSG_ERROR_UPDATING_PRODUCT = "Error updating product with ID {} in database (HTTP status code: {}, message: {})";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(BaseWrapper.class);

	/**
	 * Enumeration with valid environment variable names (available for wrapper
	 * subclasses).
	 *
	 * At runtime-start the BaseWrapper checks the presence and values of each
	 * variable.
	 * <ul>
	 * <li>{@link #JOBORDER_FILE} URI of valid JobOrder-File</li>
	 * <li>{@link #STORAGE_ENDPOINT} public API endpoint URL of prosEO Storage
	 * Manager</li>
	 * <li>{@link #STORAGE_USER} username for connection to Storage Manager</li>
	 * <li>{@link #STORAGE_PASSWORD} password for connection to Storage Manager</li>
	 * <li>{@link #INGESTOR_ENDPOINT} public API endpoint URL of
	 * prosEO-Ingestor</li>
	 * <li>{@link #STATE_CALLBACK_ENDPOINT} public API endpoint URL of
	 * prosEO-Planner for submitting the final state of the wrapper-run.</li>
	 * <li>{@link #PROCESSOR_SHELL_COMMAND} the processor shell command to be
	 * invoked by the wrapper</li>
	 * <li>{@link #PROCESSING_FACILITY_NAME} name of the processing-facility this
	 * wrapper runs in.</li>
	 * <li>{@link #PROSEO_USER} username for connection to Planner and Ingestor</li>
	 * <li>{@link #PROSEO_PW} password for connection to Planner and Ingestor</li>
	 * <li>{@link #LOCAL_FS_MOUNT} the mount point within the container, where the
	 * shared storage is mounted</li>
	 * </ul>
	 *
	 */
	protected enum ENV_VARS {
		JOBORDER_FILE, JOBORDER_VERSION, STORAGE_ENDPOINT, STORAGE_USER, STORAGE_PASSWORD, INGESTOR_ENDPOINT,
		STATE_CALLBACK_ENDPOINT, PROCESSOR_SHELL_COMMAND, PROCESSING_FACILITY_NAME, PROSEO_USER, PROSEO_PW, LOCAL_FS_MOUNT,
		FILECHECK_MAX_CYCLES, FILECHECK_WAIT_TIME
	}

	// Environment Variables from Container (set via run-invocation or directly from
	// docker-image)

	// Variables to be provided by Production Planner during invocation
	/** Path to Job Order File, format according to file system type */
	private String ENV_JOBORDER_FILE = System.getenv(ENV_VARS.JOBORDER_FILE.toString());
	/** Path to Job Order File, format according to file system type */
	protected String ENV_JOBORDER_VERSION = System.getenv(ENV_VARS.JOBORDER_VERSION.toString());
	/** HTTP endpoint for local Storage Manager */
	private String ENV_STORAGE_ENDPOINT = System.getenv(ENV_VARS.STORAGE_ENDPOINT.toString());
	/** User name for local Storage Manager */
	private String ENV_STORAGE_USER = System.getenv(ENV_VARS.STORAGE_USER.toString());
	/** Password for local Storage Manager */
	private String ENV_STORAGE_PASSWORD = System.getenv(ENV_VARS.STORAGE_PASSWORD.toString());
	/**
	 * Mount point of shared local file system (available for wrapper subclasses)
	 */
	protected String ENV_LOCAL_FS_MOUNT = System.getenv(ENV_VARS.LOCAL_FS_MOUNT.toString());
	/** Directory for temporary/output files created by this wrapper */
	protected String wrapperDataDirectory = ENV_LOCAL_FS_MOUNT + File.separator + CONTAINER_OUTPUTS_PATH_PREFIX;
	/** User name for prosEO Control Instance (available for wrapper subclasses) */
	protected String ENV_PROSEO_USER = System.getenv(ENV_VARS.PROSEO_USER.toString());
	/** Password for prosEO Control Instance (available for wrapper subclasses) */
	protected String ENV_PROSEO_PW = System.getenv(ENV_VARS.PROSEO_PW.toString());
	/**
	 * Variables to control max cycles and wait time to check file size of fetched
	 * input files
	 */
	protected String ENV_FILECHECK_MAX_CYCLES = System.getenv(ENV_VARS.FILECHECK_MAX_CYCLES.toString());
	protected String ENV_FILECHECK_WAIT_TIME = System.getenv(ENV_VARS.FILECHECK_WAIT_TIME.toString());
	/**
	 * Callback address for prosEO Production Planner, format is:
	 * <planner-URL>/processingfacilities/<procFacilityName>/finish/<podName>
	 */
	private String ENV_STATE_CALLBACK_ENDPOINT = System.getenv(ENV_VARS.STATE_CALLBACK_ENDPOINT.toString());
	/**
	 * Name of the Processing Facility this wrapper is running in (available for
	 * wrapper subclasses)
	 */
	protected String ENV_PROCESSING_FACILITY_NAME = System.getenv(ENV_VARS.PROCESSING_FACILITY_NAME.toString());
	/** HTTP endpoint for Ingestor callback (available for wrapper subclasses) */
	protected String ENV_INGESTOR_ENDPOINT = System.getenv(ENV_VARS.INGESTOR_ENDPOINT.toString());

	// Variables to be provided by the processor or wrapper image
	/**
	 * Shell command to run the processor (with path to Job Order File as sole
	 * parameter)
	 */
	protected String ENV_PROCESSOR_SHELL_COMMAND = System.getenv(ENV_VARS.PROCESSOR_SHELL_COMMAND.toString());

	/**
	 * Class for raising wrapper-generated runtime exceptions
	 */
	@SuppressWarnings("serial")
	public static class WrapperException extends RuntimeException {
	}

	/**
	 * Extracts the prosEO-compliant message from the "Warning" header, if any
	 *
	 * @param responseInfo the response info object received from
	 *                     RestOps::restApiCall
	 * @return the prosEO-compliant message, if there is one, or the unchanged
	 *         warning header, if there is one, or null otherwise
	 */
	protected String extractProseoMessage(HttpResponseInfo responseInfo) {
		if (logger.isTraceEnabled())
			logger.trace(">>> extractProseoMessage({})", (null == responseInfo ? "null" : responseInfo.getHttpWarning()));

		if (null == responseInfo || null == responseInfo.getHttpWarning()) {
			return null;
		}

		return ProseoUtil.extractProseoMessage(responseInfo.getHttpWarning());
	}

	/**
	 * Get the product metadata for a given output element
	 *
	 * @param output the output element from the Job Order file to get the metadata
	 *               for
	 * @return a REST interface product
	 * @throws WrapperException if an error occurs in the communication to the
	 *                          Ingestor, or the output element does not contain a
	 *                          product ID, or the product cannot be found
	 */
	protected RestProduct retrieveProductMetadata(InputOutput output) throws WrapperException {
		if (logger.isTraceEnabled())
			logger.trace(">>> retrieveProductMetadata({})", (null == output ? "null" : output.getProductID()));

		if (null == output.getProductID()) {
			logger.error(MSG_PRODUCT_ID_MISSING, output.getFileType());
			throw new WrapperException();
		}

		// Retrieve product metadata from Ingestor
		String ingestorRestUrl = "/products/" + output.getProductID();

		HttpResponseInfo responseInfo = RestOps.restApiCall(ENV_PROSEO_USER, ENV_PROSEO_PW, ENV_INGESTOR_ENDPOINT, ingestorRestUrl,
				null, null, RestOps.HttpMethod.GET);

		if (null == responseInfo || 200 != responseInfo.gethttpCode()) {
			logger.error(MSG_ERROR_READING_PRODUCT, output.getProductID(),
					(null == responseInfo ? 500 : responseInfo.gethttpCode()), extractProseoMessage(responseInfo));
			throw new WrapperException();
		}

		// Convert JSON string to RestProduct
		RestProduct restProduct;
		try {
			restProduct = new ObjectMapper().readValue(responseInfo.gethttpResponse(), RestProduct.class);
		} catch (Exception e) {
			logger.error(MSG_ERROR_PARSING_PRODUCT, responseInfo.gethttpResponse(), e.getMessage());
			throw new WrapperException();
		}

		return restProduct;
	}

	/**
	 * Update the metadata for the product denoted in the given output element
	 *
	 * @param output          the output element from the Job Order file to update
	 *                        the metadata for
	 * @param productMetadata a REST interface product specifying the updated
	 *                        metadata
	 * @throws WrapperException if an error occurs in the communication to the
	 *                          Ingestor, or the output element does not contain a
	 *                          product ID
	 */
	protected void updateProductMetadata(InputOutput output, RestProduct productMetadata) throws WrapperException {
		if (logger.isTraceEnabled())
			logger.trace(">>> updateProductMetadata({}, RestProduct)", (null == output ? "null" : output.getProductID()));

		if (null == output.getProductID()) {
			logger.error(MSG_PRODUCT_ID_MISSING, output.getFileType());
			throw new WrapperException();
		}

		// Update product metadata using Ingestor
		String ingestorRestUrl = "/products/" + output.getProductID();

		String jsonRequest = null;
		try {
			jsonRequest = new ObjectMapper().writeValueAsString(productMetadata);
		} catch (JsonProcessingException e) {
			logger.error(MSG_ERROR_CONVERTING_PRODUCT, productMetadata.getProductClass(), e.getMessage());
			throw new WrapperException();
		}

		HttpResponseInfo responseInfo = RestOps.restApiCall(ENV_PROSEO_USER, ENV_PROSEO_PW, ENV_INGESTOR_ENDPOINT, ingestorRestUrl,
				jsonRequest, null, RestOps.HttpMethod.PATCH);

		if (null == responseInfo || (200 != responseInfo.gethttpCode() && 304 != responseInfo.gethttpCode())) { // 304 Not modified
																												// in case of
																												// re-runs possible
			logger.error(MSG_ERROR_UPDATING_PRODUCT, output.getProductID(),
					(null == responseInfo ? 500 : responseInfo.gethttpCode()), extractProseoMessage(responseInfo));
			throw new WrapperException();
		}
	}

	/**
	 * Create a REST interface product for product ingestion from the given output
	 * file path; sets attributes mountPoint, filePath, productFileName, fileSize,
	 * checksum and checksumTime
	 *
	 * @param outputFilePath path to the output file
	 * @return a REST interface product for ingestion with ingestion information set
	 */
	protected IngestorProduct createIngestorProduct(Path outputFilePath) {
		if (logger.isTraceEnabled())
			logger.trace(">>> createIngestorProduct({})", outputFilePath);

		IngestorProduct product = new IngestorProduct();

		product.setMountPoint(ENV_LOCAL_FS_MOUNT);
		product.setFilePath(Path.of(ENV_LOCAL_FS_MOUNT).relativize(outputFilePath.getParent()).toString());
		product.setProductFileName(outputFilePath.getFileName().toString());
		product.setSourceStorageType(StorageType.POSIX.toString());
		product.setFileSize(outputFilePath.toFile().length());

		try {
			product.setChecksum(MD5Util.md5Digest(outputFilePath.toFile()));
		} catch (IOException e) {
			// Rather unlikely, but if we fail here, we probably also fail fatally later
			// during ingestion
			logger.warn(MSG_CANNOT_CALCULATE_CHECKSUM, outputFilePath, e.getClass().getName(), e.getMessage());
			product.setChecksum("N/A");
		}

		product.setChecksumTime(OrbitTimeFormatter.format(Instant.now()));

		return product;
	}

	/**
	 * Ingest the given product into the prosEO metadata database and into the
	 * backend storage
	 *
	 * @param product a REST interface product for product ingestion
	 * @throws WrapperException if an error occurs in the communication to the
	 *                          Ingestor
	 */
	protected void ingestProduct(IngestorProduct product) {
		if (logger.isTraceEnabled())
			logger.trace(">>> ingestProduct({})", (null == product ? "null" : product.getId()));

		// Upload product via Ingestor
		String ingestorRestUrl = "/ingest/" + ENV_PROCESSING_FACILITY_NAME;

		String jsonRequest = null;
		try {
			jsonRequest = new ObjectMapper().writeValueAsString(Arrays.asList(product)); // Ingestion expects list of products
		} catch (JsonProcessingException e) {
			logger.error(MSG_ERROR_CONVERTING_PRODUCT, product.getProductClass(), e.getMessage());
			throw new WrapperException();
		}

		HttpResponseInfo responseInfo = RestOps.restApiCall(ENV_PROSEO_USER, ENV_PROSEO_PW, ENV_INGESTOR_ENDPOINT, ingestorRestUrl,
				jsonRequest, null, RestOps.HttpMethod.POST);

		if (null == responseInfo || 201 != responseInfo.gethttpCode()) {
			logger.error(MSG_ERROR_REGISTERING_PRODUCT, product.getProductClass(),
					(null == responseInfo ? 500 : responseInfo.gethttpCode()), extractProseoMessage(responseInfo));
			throw new WrapperException();
		}
	}

	/**
	 * Remove protocol information, leading and trailing slashes from given file
	 * name
	 *
	 * @param fileName String
	 * @return normalized file name string or an empty string, if fileName was null
	 *         or blank
	 */
	private String normalizeFileName(final String fileName) {
		if (logger.isTraceEnabled())
			logger.trace(">>> normalizeFileName({})", fileName);

		if (null == fileName || fileName.isBlank()) {
			return "";
		}

		String workFileName = fileName;

		// Step 1: Remove protocol
		String[] fileNameParts = workFileName.split(":", 2);
		if (2 == fileNameParts.length) {
			workFileName = fileNameParts[1];
		}

		// Step 2: Remove any leading and trailing slashes
		workFileName.replace("^/+", "").replace("/+$", "");

		return workFileName;
	}

	/**
	 * Check presence and values of all required Environment Variables
	 *
	 * @throws WrapperException if the check does not pass for any reason
	 */
	private void checkEnvironment() {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkEnv()");

		logger.info(MSG_CHECKING_ENVIRONMENT, ENV_VARS.values().length);
		for (ENV_VARS e : ENV_VARS.values()) {
			if (ENV_VARS.PROSEO_PW == e || ENV_VARS.STORAGE_PASSWORD == e) {
				logger.info("... {} = {}", e, "(not disclosed)");
			} else {
				logger.info("... {} = {}", e, System.getenv(e.toString()));
			}
		}

		boolean envOK = true;
		if (ENV_JOBORDER_FILE == null || ENV_JOBORDER_FILE.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.JOBORDER_FILE);
			envOK = false;
		}
		if (ENV_JOBORDER_VERSION == null || ENV_JOBORDER_VERSION.isEmpty()) {
			ENV_JOBORDER_VERSION = JobOrderVersion.MMFI_1_8.toString();
		}
		if (ENV_STORAGE_ENDPOINT == null || ENV_STORAGE_ENDPOINT.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.STORAGE_ENDPOINT);
			envOK = false;
		}
		if (ENV_STORAGE_USER == null || ENV_STORAGE_USER.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.STORAGE_USER);
			envOK = false;
		}
		if (ENV_STORAGE_PASSWORD == null || ENV_STORAGE_PASSWORD.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.STORAGE_PASSWORD);
			envOK = false;
		}
		if (ENV_STATE_CALLBACK_ENDPOINT == null || ENV_STATE_CALLBACK_ENDPOINT.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.STATE_CALLBACK_ENDPOINT);
			envOK = false;
		}
		if (ENV_PROCESSOR_SHELL_COMMAND == null || ENV_PROCESSOR_SHELL_COMMAND.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.PROCESSOR_SHELL_COMMAND);
			envOK = false;
		}
		if (ENV_PROCESSING_FACILITY_NAME == null || ENV_PROCESSING_FACILITY_NAME.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.PROCESSING_FACILITY_NAME);
			envOK = false;
		}
		if (ENV_INGESTOR_ENDPOINT == null || ENV_INGESTOR_ENDPOINT.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.INGESTOR_ENDPOINT);
			envOK = false;
		}
		if (ENV_PROSEO_USER == null || ENV_PROSEO_USER.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.PROSEO_USER);
			envOK = false;
		}
		if (ENV_PROSEO_PW == null || ENV_PROSEO_PW.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.PROSEO_PW);
			envOK = false;
		}
		if (ENV_LOCAL_FS_MOUNT == null || ENV_LOCAL_FS_MOUNT.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.LOCAL_FS_MOUNT);
			envOK = false;
		}
		if (ENV_FILECHECK_WAIT_TIME == null || ENV_FILECHECK_WAIT_TIME.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.FILECHECK_WAIT_TIME);
			envOK = false;
		}
		try {
			int i = Integer.parseInt(ENV_FILECHECK_WAIT_TIME);
			if (i <= 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException ex) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.FILECHECK_WAIT_TIME);
			envOK = false;
		}
		if (ENV_FILECHECK_MAX_CYCLES == null || ENV_FILECHECK_MAX_CYCLES.isEmpty()) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.FILECHECK_MAX_CYCLES);
			envOK = false;
		}

		try {
			int i = Integer.parseInt(ENV_FILECHECK_MAX_CYCLES);
			if (i <= 0) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException ex) {
			logger.error(MSG_INVALID_VALUE_OF_ENVVAR, ENV_VARS.FILECHECK_MAX_CYCLES);
			envOK = false;
		}

		if (envOK) {
			logger.info(MSG_ENVIRONMENT_CHECK_PASSED);
			logger.info(MSG_PREFIX_TIMESTAMP_FOR_NAMING, JOB_ORDER_ID);
		} else {
			logger.error(MSG_ENVIRONMENT_CHECK_FAILED);
			throw new WrapperException();
		}
	}

	/**
	 * Fetch Job Order file from Storage Manager
	 *
	 * @return the Job Order file as String
	 * @throws WrapperException if the Job Order file cannot be read
	 */
	private String provideInitialJOF() throws WrapperException {
		if (logger.isTraceEnabled())
			logger.trace(">>> provideInitialJOF()");

		// Call Storage Manager to retrieve Job Order File as Base64-encoded string
		try {
			Map<String, String> params = new HashMap<>();
			params.put("pathInfo", ENV_JOBORDER_FILE);
			HttpResponseInfo responseInfo = RestOps.restApiCall(ENV_STORAGE_USER, ENV_STORAGE_PASSWORD, ENV_STORAGE_ENDPOINT,
					"/joborders", null, params, RestOps.HttpMethod.GET);

			if (200 == responseInfo.gethttpCode()) {
				return new String(Base64.getDecoder().decode(responseInfo.gethttpResponse()));
			} else {
				logger.error(MSG_ERROR_RETRIEVING_JOB_ORDER, responseInfo.gethttpCode(), extractProseoMessage(responseInfo));
				throw new WrapperException();
			}
		} catch (Exception e) {
			logger.error(MSG_EXCEPTION_RETRIEVING_JOB_ORDER, e.getMessage());
			throw new WrapperException();
		}
	}

	/**
	 * Parse the given JobOrder XML file
	 *
	 * @param jobOrderFile the XML file to parse
	 * @return JobOrder Object
	 * @throws WrapperException if the Job Order string cannot be parsed into a Job
	 *                          Order document
	 */
	private JobOrder parseJobOrderFile(String jobOrderFile) throws WrapperException {
		if (logger.isTraceEnabled())
			logger.trace(">>> parseJobOrderFile(JOF)");

		JobOrder jobOrderDoc = null;
		jobOrderDoc = new JobOrder();
		jobOrderDoc = jobOrderDoc.read(jobOrderFile);
		if (null == jobOrderDoc) {
			logger.error(MSG_ERROR_PARSING_JOB_ORDER);
			throw new WrapperException();
		}

		return jobOrderDoc;
	}

	/**
	 * Hook for mission-specific modifications to the job order document before
	 * fetching input data Intended for override by mission-specific wrapper
	 * classes, NO-OP in BaseWrapper.
	 *
	 * @param jobOrderDoc the job order document to modify
	 * @throws WrapperException if some error occurred which forces wrapper
	 *                          termination
	 */
	protected void preFetchInputHook(JobOrder jobOrderDoc) throws WrapperException {
		// No operation
	}

	/**
	 * Fetch remote input-data to container-workdir and return valid JobOrder object
	 * for container-runtime-context. (=remapped file-pathes)
	 *
	 * @param jo the JobOrder file to parse
	 * @return JobOrder object valid for container-context
	 * @throws WrapperException if input data cannot be found or output directories
	 *                          cannot be created
	 */
	private JobOrder fetchInputData(JobOrder jo) throws WrapperException {
		if (logger.isTraceEnabled())
			logger.trace(">>> fetchInputData(JOF)");

		int numberOfInputs = 0, numberOfOutputs = 0;

		// Loop all procs -> mainly only one is present
		for (Proc item : jo.getListOfProcs()) {
			// Loop all Input
			for (InputOutput io : item.getListOfInputs()) {
				if (!InputOutput.FN_TYPE_PHYSICAL.equals(io.getFileNameType())) {
					// Only download "Physical" files
					logger.info(MSG_SKIPPING_INPUT_ENTRY, io.getFileType(), io.getFileNameType());
					continue;
				}
				// Loop List_of_File_Names
				for (IpfFileName fn : io.getFileNames()) {
					// Ensure file name exists and is not blank
					if (null == fn.getFileName() || fn.getFileName().isBlank()) {
						logger.warn(MSG_WARNING_INPUT_FILENAME_MISSING, io.getFileType());
						continue;
					}

					// Fill original filename with current val of `File_Name` --> for later use...
					fn.setOriginalFileName(fn.getFileName());

					// Test local availability of input file
					File f = new File(fn.getFileName());
					if (f.exists()) {
						// nothing to do
						continue;
					}

					// Request input file from Storage Manager
					Map<String, String> params = new HashMap<>();
					params.put("pathInfo", fn.getFileName() + (io.getFileNameType().equalsIgnoreCase("Directory") ? "/" : ""));
					HttpResponseInfo responseInfo = RestOps.restApiCall(ENV_STORAGE_USER, ENV_STORAGE_PASSWORD,
							ENV_STORAGE_ENDPOINT, "/productfiles", null, params, RestOps.HttpMethod.GET);

					if (200 != responseInfo.gethttpCode()) {
						logger.error(MSG_ERROR_RETRIEVING_INPUT_FILE, fn.getFileName(), responseInfo.gethttpCode(),
								extractProseoMessage(responseInfo));
						throw new WrapperException();
					}

					// Update file name to new file name on POSIX file system
					String fileInfo = responseInfo.gethttpResponse();
					ObjectMapper objectMapper = new ObjectMapper();
					RestFileInfo rfi = null;
					try {
						rfi = objectMapper.readValue(fileInfo, RestFileInfo.class);
					} catch (Exception ex) {
						logger.error(MSG_CANNOT_PARSE_FILE_INFO, fileInfo, ex.getClass().getName(), ex.getMessage());
						continue;
					}
					String inputFileName = rfi.getFilePath();

					// wait for copy completion due to NFS caching of clients
					Path fp = Path.of(inputFileName);
					if (fp.toFile().isFile()) {
						Integer wait = Integer.valueOf(ENV_FILECHECK_WAIT_TIME);
						Integer max = Integer.valueOf(ENV_FILECHECK_MAX_CYCLES);
						try {
							if (logger.isDebugEnabled())
								logger.debug(
										"... Testing wait for download of {}, wait interval {}, max cycles {}; size is {}, expected {}",
										inputFileName, wait, max, Files.size(fp), rfi.getFileSize());
							synchronized (this) {
								int i = 0;
								while ((Files.size(fp) < rfi.getFileSize()) && (i < max)) {
									if (logger.isDebugEnabled()) {
										logger.debug("Wait for fully copied file {}", inputFileName);
									}
									i++;
									try {
										this.wait(wait);
									} catch (InterruptedException e) {
										// Do nothing (except for debug logging), we just stay in the while loop
										if (logger.isDebugEnabled())
											logger.debug("... wait interrupted, cause: " + e.getMessage());
									}
								}
								if (i >= max) {
									logger.error(MSG_FILE_NOT_FETCHED, inputFileName);
									throw new WrapperException();
								}
							}
						} catch (IOException e) {
							logger.error(MSG_UNABLE_TO_ACCESS_FILE, inputFileName, e.getClass().getName(), e.getMessage());
							throw new WrapperException();
						}
					} else {
						logger.info("Skipping wait for {}, because it's not a file", inputFileName);
					}
					fn.setFileName(inputFileName);
					// Check for time intervals for this file and update their file names, too
					for (TimeInterval ti : io.getTimeIntervals()) {
						if (ti.getFileName().equals(fn.getOriginalFileName())) {
							ti.setFileName(inputFileName);
						}
					}

					++numberOfInputs;
				}
			}

			// Loop all Output and prepare directories
			for (InputOutput io : item.getListOfOutputs()) {

				// Loop List_of_File_Names
				for (IpfFileName fn : io.getFileNames()) {

					// Fill original filename with current val of `File_Name` --> for later use -->
					// push results step
					fn.setOriginalFileName(fn.getFileName());

					// Set output file_name to local work-dir path
					fn.setFileName(wrapperDataDirectory + File.separator + normalizeFileName(fn.getFileName()));

					// Handle directories and regular files differently
					Path filePath = Paths.get(fn.getFileName());
					if (logger.isTraceEnabled())
						logger.trace("... creating directory for {}, output file name type {}", fn.getFileName(),
								io.getFileNameType());
					if (InputOutput.FN_TYPE_DIRECTORY.equals(io.getFileNameType())) {
						if (Files.exists(filePath)) {
							if (!Files.isDirectory(filePath)) {
								logger.error(MSG_NOT_A_DIRECTORY, fn.getFileName());
								throw new WrapperException();
							}
							if (0 != filePath.toFile().list().length) {
								logger.error(MSG_DIRECTORY_NOT_EMPTY, fn.getFileName());
								throw new WrapperException();
							}
						}
						try {
							if (logger.isTraceEnabled())
								logger.trace("... in 'Directory' branch: calling create directories for {}", filePath);
							Files.createDirectories(filePath);
						} catch (IOException | SecurityException e) {
							logger.error(MSG_UNABLE_TO_CREATE_DIRECTORY, filePath, e.getClass().getName(), e.getMessage());
							if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
							throw new WrapperException();
						}
					} else {
						try {
							if (logger.isTraceEnabled())
								logger.trace("... in 'Physical' branch: deleting file {}", filePath);
							Files.deleteIfExists(filePath);
							if (logger.isTraceEnabled())
								logger.trace("... in 'Physical' branch: calling create directories for {}", filePath.getParent());
							Files.createDirectories(filePath.getParent());
						} catch (IOException | SecurityException e) {
							logger.error(MSG_UNABLE_TO_CREATE_DIRECTORY, filePath.getParent(), e.getClass().getName(),
									e.getMessage());
							if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
							throw new WrapperException();
						}
					}

					++numberOfOutputs;
				}
			}
		}

		logger.info(MSG_FETCHED_INPUT_FILES, numberOfInputs, numberOfOutputs, CONTAINER_JOF_PATH);
		return jo;
	}

	/**
	 * Hook for mission-specific modifications to the job order document after
	 * fetching input data Intended for override by mission-specific wrapper
	 * classes, NO-OP in BaseWrapper.
	 *
	 * @param jobOrderDoc the job order document to modify
	 * @throws WrapperException if some error occurred which forces wrapper
	 *                          termination
	 */
	protected void postFetchInputHook(JobOrder jobOrderDoc) throws WrapperException {
		// No operation
	}

	/**
	 * Creates valid container-context JobOrderFile under given path
	 *
	 * @param jo   JobOrder remapped JobOrder object
	 * @param path file path of newly created JOF
	 * @throws WrapperException if the Job Order document cannot be written to an
	 *                          XML file
	 */
	private void provideContainerJOF(JobOrder jo, String path) throws WrapperException {
		if (logger.isTraceEnabled())
			logger.trace(">>> provideContainerJOF(JOF, {})", path);

		boolean ok = jo.writeXML(path, JobOrderVersion.valueOf(ENV_JOBORDER_VERSION), false);
		if (!ok) {
			logger.error(MSG_ERROR_WRITING_JOF);
			throw new WrapperException();
		}
	}

	/**
	 * Executes the processor
	 *
	 * @param jofPath path of re-mapped JobOrder file valid in container context
	 * @throws WrapperException if the process was interrupted or some other
	 *                          exception occurred during processing
	 */
	private void runProcessor(String jofPath) throws WrapperException {
		if (logger.isTraceEnabled())
			logger.trace(">>> runProcessor({}, {})", jofPath);

		logger.info(MSG_STARTING_PROCESSOR, ENV_PROCESSOR_SHELL_COMMAND, jofPath);

		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.redirectErrorStream(true);

		processBuilder.command((ENV_PROCESSOR_SHELL_COMMAND + " " + jofPath).split(" "));
		processBuilder.redirectErrorStream(true);
		processBuilder.redirectOutput(Redirect.INHERIT);

		int exitCode = EXIT_CODE_FAILURE; // Failure
		try {
			exitCode = processBuilder.start().waitFor();

			if (EXIT_CODE_OK == exitCode) {
				logger.info(MSG_PROCESSING_FINISHED_OK, exitCode);
			} else if (EXIT_CODE_WARNING >= exitCode) {
				logger.warn(MSG_PROCESSING_FINISHED_WARNING, exitCode);
			} else {
				logger.error(MSG_PROCESSING_FINISHED_ERROR, exitCode);
				throw new WrapperException();
			}
		} catch (InterruptedException e) {
			logger.error(MSG_PROCESSOR_EXECUTION_INTERRUPTED, e.getMessage());
			throw new WrapperException();
		} catch (IOException e) {
			logger.error(MSG_ERROR_RUNNING_PROCESSOR, e.getMessage());
			throw new WrapperException();
		}
	}

	/**
	 * Hook for mission-specific modifications to the final job order document after
	 * execution of the processor (before push of results). Intended for
	 * <ol>
	 * <li>Adding additional output files to the output list as desired (e. g. log
	 * files, job order file)</li>
	 * <li>Packaging multiple files into a single ZIP file for delivery via the PRIP
	 * if desired (add an output file with File_Name_Type "Archive", using Java
	 * constant InputOutput.FN_TYPE_ARCHIVE)</li>
	 * </ol>
	 * Note: The first (non-archive) output file is taken as the (main) product
	 * file, subsequent files are interpreted as auxiliary files.
	 *
	 * Intended for override by mission-specific wrapper classes, NO-OP in
	 * BaseWrapper.
	 *
	 * @param joWork the job order document to modify
	 * @throws WrapperException if some error occurred which forces wrapper
	 *                          termination
	 */
	protected void postProcessingHook(JobOrder joWork) throws WrapperException {
		// No operation
	}

	/**
	 * Pushes processing results to prosEO storage
	 *
	 * @param jo jobOrder JobOrder-Object (valid in container context)
	 * @return ArrayList all infos of pushed products
	 * @throws WrapperException if the product ID cannot be retrieved or the product
	 *                          upload failed
	 */
	private ArrayList<RestProductFile> pushResults(JobOrder jo) throws WrapperException {
		if (logger.isTraceEnabled())
			logger.trace(">>> pushResults(JOF)");

		logger.info(MSG_UPLOADING_RESULTS);

		int numOutputs = 0;

		ArrayList<RestProductFile> pushedOutputs = new ArrayList<>();

		for (Proc item : jo.getListOfProcs()) {
			// Loop all Outputs
			for (InputOutput io : item.getListOfOutputs()) {
				// Ignore directories (cannot be pushed)
				if (InputOutput.FN_TYPE_DIRECTORY.equals(io.getFileNameType())) {
					continue;
				}

				// Prepare product file for Ingestor REST API
				RestProductFile productFile = new RestProductFile();
				try {
					productFile.setProductId(Long.parseLong((io.getProductID())));
				} catch (NumberFormatException e) {
					logger.error(MSG_PRODUCT_ID_NOT_PARSEABLE, io.getProductID());
					throw new WrapperException();
				}
				productFile.setProcessingFacilityName(ENV_PROCESSING_FACILITY_NAME);
				pushedOutputs.add(productFile);

				// Loop all output files
				for (IpfFileName fn : io.getFileNames()) {
					// Push output file to Storage Manager
					Map<String, String> params = new HashMap<>();
					params.put("pathInfo", fn.getFileName());
					params.put("productId", io.getProductID());
					Path fp = Path.of(fn.getFileName());
					try {
						params.put("fileSize", String.valueOf(Files.size(fp)));
					} catch (IOException e1) {
						logger.warn(MSG_CANNOT_DETERMINE_FILE_SIZE, fp, e1.getClass().getName(), e1.getMessage());
						params.put("fileSize", "0");
					}
					HttpResponseInfo responseInfo = RestOps.restApiCall(ENV_STORAGE_USER, ENV_STORAGE_PASSWORD,
							ENV_STORAGE_ENDPOINT, "/productfiles", null, params, RestOps.HttpMethod.PUT);

					if (201 != responseInfo.gethttpCode()) {
						logger.error(MSG_ERROR_PUSHING_OUTPUT_FILE, fn.getFileName(), responseInfo.gethttpCode(),
								extractProseoMessage(responseInfo));
						throw new WrapperException();
					}

					String fileInfo = responseInfo.gethttpResponse();
					ObjectMapper objectMapper = new ObjectMapper();
					RestFileInfo rfi = null;
					try {
						rfi = objectMapper.readValue(fileInfo, RestFileInfo.class);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					if (rfi == null) {
						logger.error(MSG_MALFORMED_RESPONSE_FROM_STORAGE_MANAGER, responseInfo.gethttpResponse(), fn.getFileName());
						throw new WrapperException();
					}

					// Make sure all product files have the same storage tpye
					if (null == productFile.getStorageType()) {
						productFile.setStorageType(rfi.getStorageType());
					} else if (!productFile.getStorageType().equals(rfi.getStorageType())) {
						logger.error(MSG_DIFFERENT_STORAGE_TYPES_ASSIGNED, productFile.getProductId());
						throw new WrapperException();
					}

					// Separate the file path into a directory path and a file name
					String filePath = rfi.getFilePath(); // This is not a file path in the local (POSIX) file system; its separator
															// is always "/"
					int lastSeparatorIndex = filePath.lastIndexOf('/');
					String parentPath = filePath.substring(0, lastSeparatorIndex);
					String fileName = filePath.substring(lastSeparatorIndex + 1);

					// Make sure all product files are stored in the same path
					if (null == productFile.getFilePath()) {
						productFile.setFilePath(parentPath);
					} else if (!productFile.getFilePath().equals(parentPath)) {
						logger.error(MSG_DIFFERENT_FILE_PATHS_ASSIGNED, productFile.getProductId());
						throw new WrapperException();
					}

					// Create metadata for this file
					if (InputOutput.FN_TYPE_ARCHIVE.equals(io.getFileNameType())) {
						// Extension to JOF specification, only to be used in "postProcessingHook()" to
						// identify ZIP archives,
						// must only be used once
						if (null != productFile.getZipFileName()) {
							logger.error(MSG_MORE_THAN_ONE_ZIP_ARCHIVE, productFile.getProductId());
							throw new WrapperException();
						}
						productFile.setZipFileName(fileName);
						File primaryProductFile = new File(fn.getFileName()); // The full path to the file in the local file system
						productFile.setZipFileSize(primaryProductFile.length());
						try {
							productFile.setZipChecksum(MD5Util.md5Digest(primaryProductFile));
							productFile.setZipChecksumTime(OrbitTimeFormatter.format(Instant.now()));
						} catch (IOException e) {
							logger.error(MSG_CANNOT_CALCULATE_CHECKSUM, productFile.getProductId(), e.getClass().getName(),
									e.getMessage());
							throw new WrapperException();
						}
					} else if (null == productFile.getProductFileName()) {
						// The first (non-archive) file is taken as the main product file
						productFile.setProductFileName(fileName);
						File primaryProductFile = new File(fn.getFileName()); // The full path to the file in the local file system
						productFile.setFileSize(primaryProductFile.length());
						try {
							productFile.setChecksum(MD5Util.md5Digest(primaryProductFile));
							productFile.setChecksumTime(OrbitTimeFormatter.format(Instant.now()));
						} catch (IOException e) {
							logger.error(MSG_CANNOT_CALCULATE_CHECKSUM, productFile.getProductId(), e.getClass().getName(),
									e.getMessage());
							throw new WrapperException();
						}
					} else {
						// Subsequent (non-archive) files are auxiliary files
						productFile.getAuxFileNames().add(fileName);
					}

					++numOutputs;
				}

			}
		}

		logger.info(MSG_PRODUCTS_UPLOADED, pushedOutputs.size(), numOutputs);

		if (logger.isDebugEnabled()) {
			logger.debug("Upload summary: listing {} outputs of type 'RestProductFile'", pushedOutputs.size());
			for (RestProductFile p : pushedOutputs) {
				logger.debug("PRODUCT_ID = {}, FS_TYPE = {}, PATH = {}, PRODUCT FILE = {}", p.getProductId(), p.getStorageType(),
						p.getFilePath(), p.getProductFileName());
			}
		}
		return pushedOutputs;
	}

	/**
	 * Register pushed Products using prosEO-Ingestor REST API
	 *
	 * @param pushedProducts ArrayList
	 * @throws WrapperException if the product cannot be registered with the
	 *                          Ingestor
	 */
	private void ingestPushedOutputs(ArrayList<RestProductFile> pushedProducts) {
		if (logger.isTraceEnabled())
			logger.trace(">>> ingestPushedOutputs(RestProductFile[{}])", pushedProducts.size());

		logger.info(MSG_REGISTERING_PRODUCTS, pushedProducts.size(), ENV_INGESTOR_ENDPOINT);

		// loop pushed products and send HTTP-POST to prosEO-Ingestor in order to
		// register the products
		for (RestProductFile productFile : pushedProducts) {
			String ingestorRestUrl = "/ingest/" + ENV_PROCESSING_FACILITY_NAME + "/" + productFile.getProductId();

			ObjectMapper obj = new ObjectMapper();
			String jsonRequest = null;
			try {
				jsonRequest = obj.writeValueAsString(productFile);
			} catch (JsonProcessingException e) {
				logger.error(MSG_ERROR_CONVERTING_INGESTOR_PRODUCT, productFile.getProductId(), e.getMessage());
				throw new WrapperException();
			}
			HttpResponseInfo responseInfo = RestOps.restApiCall(ENV_PROSEO_USER, ENV_PROSEO_PW, ENV_INGESTOR_ENDPOINT,
					ingestorRestUrl, jsonRequest, null, RestOps.HttpMethod.POST);

			if (null == responseInfo || 201 != responseInfo.gethttpCode()) {
				logger.error(MSG_ERROR_REGISTERING_PRODUCT, productFile.getProductId(),
						(null == responseInfo ? 500 : responseInfo.gethttpCode()), extractProseoMessage(responseInfo));
				throw new WrapperException();
			}
		}

		logger.info(MSG_PRODUCTS_REGISTERED, pushedProducts.size());
	}

	/**
	 * Cleanup (delete) directories created by this BaseWrapper. It removes as many
	 * subdirectories and directories as it can beginning from the wrapper data
	 * directory root.
	 *
	 * This method must not throw any exceptions!
	 */
	private void cleanup() {
		if (logger.isTraceEnabled())
			logger.trace(">>> cleanup()");

		Path wrapperDataPath = Path.of(wrapperDataDirectory);
		if (!Files.exists(wrapperDataPath)) {
			// The data directory was never created, so it does not need to be cleaned up
			return;
		}
		try {
			Files.walkFileTree(wrapperDataPath, new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					// Nothing to do
					if (logger.isTraceEnabled())
						logger.trace("... before visiting directory " + dir);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						if (logger.isTraceEnabled())
							logger.trace("... deleting file " + file);
						Files.delete(file);
					} catch (IOException e) {
						logger.error(MSG_UNABLE_TO_DELETE_DIRECTORY, file.toString(),
								e.getClass().getName() + " / " + e.getMessage());
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					logger.error(MSG_UNABLE_TO_DELETE_DIRECTORY, file.toString(), "Call to visitFileFailed");
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					try {
						if (logger.isTraceEnabled())
							logger.trace("... deleting directory " + dir);
						Files.delete(dir);
					} catch (IOException e) {
						logger.error(MSG_UNABLE_TO_DELETE_DIRECTORY, dir.toString(),
								e.getClass().getName() + " / " + e.getMessage());
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
			logger.error(MSG_UNABLE_TO_DELETE_DIRECTORY, wrapperDataDirectory, e.getClass().getName() + " / " + e.getMessage());
		}
	}

	/**
	 * Triggers a callback to ENV_STATE_CALLBACK_ENDPOINT (prosEO Production
	 * Planner)
	 *
	 * @param msg the callback message
	 */
	private void callBack(String msg) {
		if (logger.isTraceEnabled())
			logger.trace(">>> callBack({})", msg);

		try {
			Map<String, String> params = new HashMap<>();
			params.put("status", msg);
			// queryParam status is set by wrapper
			HttpResponseInfo responseInfo = RestOps.restApiCall(ENV_PROSEO_USER, ENV_PROSEO_PW, ENV_STATE_CALLBACK_ENDPOINT, "",
					null, params, RestOps.HttpMethod.POST);

			if (null == responseInfo || 200 != responseInfo.gethttpCode()) {
				logger.error(MSG_ERROR_CALLING_PLANNER, (null == responseInfo ? 500 : responseInfo.gethttpCode()),
						extractProseoMessage(responseInfo));
			}

			logger.info(MSG_PLANNER_RESPONSE, responseInfo.gethttpResponse(), responseInfo.gethttpCode());
		} catch (Exception e) {
			logger.error(MSG_ERROR_IN_PLANNER_CALLBACK, ENV_STATE_CALLBACK_ENDPOINT, e.getMessage());
			return;
		}

	}

	/**
	 * Perform processing: check parameters, parse JobOrder file, fetch input files,
	 * process, push output files
	 *
	 * @return the program exit code (OK or FAILURE)
	 */
	final public int run() {
		if (logger.isTraceEnabled())
			logger.trace(">>> run()");

		/* ProcessorWrapperFlow */
		/* ==================== */

		logger.info(MSG_STARTING_BASE_WRAPPER, ENV_JOBORDER_FILE);

		try {
			/* STEP [1] Check environment variables (acting as invocation parameters) */

			checkEnvironment();

			/* STEP [2] Fetch the JobOrder file from the Storage Manager */

			String jobOrderFile = provideInitialJOF();

			/* STEP [3] Create a Job Order document from the JobOrder file */

			JobOrder jobOrderDoc = parseJobOrderFile(jobOrderFile);

			/* STEP [4 - Optional] Modify Job Order document for processor operation */

			/*
			 * Hook for additional mission-specific pre-fetch operations on the job order
			 * document
			 */
			preFetchInputHook(jobOrderDoc);

			/* STEP [5] Fetch input files and remap file names in Job Order */

			JobOrder joWork = fetchInputData(jobOrderDoc);

			/*
			 * STEP [6 - Optional] Modify fetched data and Job Order document for processor
			 * operation
			 */

			/*
			 * Hook for additional mission-specific post-fetch operations on the job order
			 * document
			 */
			postFetchInputHook(joWork);

			/* STEP [7] Create Job Order File in file system for container context */

			provideContainerJOF(joWork, CONTAINER_JOF_PATH);

			/* STEP [8] Execute Processor */

			runProcessor(REL_CONTAINER_JOF_PATH);

			/* STEP [9] Perform processor-specific updates to the Job Order document */

			/* Hook for additional post-processing operations on the job order document */
			postProcessingHook(joWork);

			/* STEP [10] Push Processing Results to prosEO Storage, if any */

			ArrayList<RestProductFile> pushedProducts = pushResults(joWork);
			if (null == pushedProducts) {
				callBack(CALLBACK_STATUS_FAILURE);
				logger.info(MSG_LEAVING_BASE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
				return EXIT_CODE_FAILURE;
			}

			/* STEP [11] Register pushed products using with prosEO Ingestor */
			ingestPushedOutputs(pushedProducts);

		} catch (WrapperException e) {
			/* STEP [12B] Report failure to Production Planner */
			callBack(CALLBACK_STATUS_FAILURE);
			logger.info(MSG_LEAVING_BASE_WRAPPER, EXIT_CODE_FAILURE, EXIT_TEXT_FAILURE);
			return EXIT_CODE_FAILURE;
		} finally {
			/*
			 * Step [12A] Cleanup temporary files/directories; note that this is executed
			 * after catching an exception, too
			 */
			cleanup();
		}

		/* STEP [12B] Report success to Production Planner */

		callBack(CALLBACK_STATUS_SUCCESS);

		logger.info(MSG_LEAVING_BASE_WRAPPER, EXIT_CODE_OK, EXIT_TEXT_OK);
		return EXIT_CODE_OK;
	}

	/**
	 * Main routine: Run BaseWrapper
	 *
	 * @param args first string argument is class name of actual Wrapper class (must
	 *             be a subclass of BaseWrapper or BaseWrapper itself; default is to
	 *             use BaseWrapper)
	 */
	public static void main(String[] args) {
		Class<?> clazz = null;

		try {
			clazz = (0 == args.length ? BaseWrapper.class : ClassLoader.getSystemClassLoader().loadClass(args[0]));
		} catch (ClassNotFoundException e) {
			logger.error(MSG_WRAPPER_CLASS_NOT_FOUND, args[0]);
			System.exit(EXIT_CODE_FAILURE);
		}
		if (!BaseWrapper.class.isAssignableFrom(clazz)) {
			logger.error(MSG_WRAPPER_NOT_SUBCLASS_OF_BASE_WRAPPER, clazz.getName());
			System.exit(EXIT_CODE_FAILURE);
		}
		try {
			System.exit(((BaseWrapper) clazz.getDeclaredConstructor().newInstance()).run());
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
					logger.debug("An exception occurred. Cause: ", e);
				}
			logger.error(MSG_WRAPPER_CANNOT_BE_LAUNCHED, clazz.getName(), e.getMessage());
			System.exit(EXIT_CODE_FAILURE);
		}
	}

}