/**
 * JobOrderControllerImpl.java
 * 
 * (C) 2019 DLR
 */
package de.dlr.proseo.storagemgr.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.utils.StorageType;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.StorageMgrMessage;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.RestJoborder;
import de.dlr.proseo.storagemgr.utils.ProseoFile;
import de.dlr.proseo.storagemgr.utils.StorageManagerUtils;

/**
 * Spring MVC controller for the prosEO Storage Manager; implements the services
 * required to manage Joborders
 * 
 * @author Hubert Asamer
 * @author Denys Chaykovskiy
 *
 */
@Component
public class JobOrderControllerImpl implements JoborderController {

	private static final Charset JOF_CHARSET = StandardCharsets.UTF_8;
	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "4000 proseo-storage-mgr ";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final int MSG_ID_EXCEPTION_THROWN = 9001;

	private static Logger loggerLegacy = LoggerFactory.getLogger(JoborderController.class);

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(JobOrderControllerImpl.class);

	@Autowired
	private StorageManagerConfiguration cfg;

	@Autowired
	private StorageProvider storageProvider;

	/**
	 * Create a job order file with generic name out of base64 string contained in
	 * RestJoborder. Store it into FS defined by fsType.
	 * 
	 * @param joborder Job order information
	 * @return ResponseEntity as RestJoborder
	 */
	@Override
	public ResponseEntity<RestJoborder> createRestJoborder(@Valid RestJoborder joborder) {

		if (logger.isTraceEnabled())
			logger.trace(">>> createRestJoborder({})", (null == joborder ? "MISSING" : joborder.getMessage()));

		if (storageProvider.isVersion2()) { // begin version 2 String -> StorageFile

			String jobOrder64 = joborder.getJobOrderStringBase64();

			if (!isStringBase64(jobOrder64)) {

				String msg = logger.log(StorageMgrMessage.STRING_NOT_BASE64_ENCODED);
				return new ResponseEntity<>(createBadResponse(msg, jobOrder64), HttpStatus.FORBIDDEN);
			}

			String relativePath = getJobOrderRelativePath(cfg.getJoborderPrefix());

			try {
				StorageFile targetFile = storageProvider.createStorageFile(relativePath, jobOrder64);
				logger.log(StorageMgrMessage.JOB_ORDER_FILE_UPLOADED, targetFile);
				return new ResponseEntity<>(createOkResponse(targetFile, jobOrder64), HttpStatus.CREATED);

			} catch (Exception e) {

				String msg = logger.log(StorageMgrMessage.JOB_ORDER_CREATION_ERROR, jobOrder64 + " " + e.getMessage());

				return new ResponseEntity<>(createBadResponse(msg, jobOrder64), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} // end version 2

		RestJoborder response = new RestJoborder();
		String separator = "/";
		try {
			// check if we have a Base64 encoded string & if we have valid XML
			if (!joborder.getJobOrderStringBase64()
					.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$")) {

				response.setUploaded(false);
				response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
				response.setPathInfo("n/a");
				response.setMessage("Attribute jobOrderStringBase64 is not Base64-encoded...");
				return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
			}

			DateTime tstamp = DateTime.now(DateTimeZone.UTC);
			String objKey = cfg.getJoborderPrefix() + separator + tstamp.getYear() + separator + tstamp.getMonthOfYear()
					+ separator + tstamp.getDayOfMonth() + separator + tstamp.getHourOfDay() + separator
					+ UUID.randomUUID().toString() + ".xml";

			String base64String = joborder.getJobOrderStringBase64();
			byte[] bytes = java.util.Base64.getDecoder().decode(base64String);
			if (!StorageManagerUtils
					.checkXml(StorageManagerUtils.inputStreamToString(new ByteArrayInputStream(bytes), JOF_CHARSET))) {
				response.setUploaded(false);
				response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
				response.setPathInfo("n/a");
				response.setMessage("XML Doc parsed from attribute jobOrderStringBase64 is not valid...");
				return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
			}
			// TODO - Change to ProseoFile.fromTypeAndBucket(..., cfg.getJoborderBucket(),
			// ...);
			ProseoFile proFile = ProseoFile.fromType(StorageType.valueOf(joborder.getFsType()), objKey, cfg);
			if (proFile != null) {

				if (proFile.writeBytes(bytes)) {
					response.setFsType(proFile.getFsType().toString());
					response.setPathInfo(proFile.getFullPath());
					response.setUploaded(true);
					response.setJobOrderStringBase64(joborder.getJobOrderStringBase64());
					loggerLegacy.info("Received & Uploaded joborder-file: {}", response.getPathInfo());
					return new ResponseEntity<>(response, HttpStatus.CREATED);
				}
			}
		} catch (Exception e) {
			return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
					e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
	}

	/**
	 * Retrieve contents of file as base64 string.
	 * 
	 * @param pathInfo the path to the file to retrieve
	 * @return Base64 coded String
	 */
	@Override
	public ResponseEntity<String> getObjectByPathInfo(String pathInfo) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getObjectByPathInfo({})", pathInfo);

		if (storageProvider.isVersion2()) { // begin version 2 StorageFile -> String

			if (null == pathInfo) {
				return new ResponseEntity<>(logger.log(StorageMgrMessage.PATH_IS_NULL), HttpStatus.NOT_FOUND);
			}

			if (pathInfo == "") {
				return new ResponseEntity<>(logger.log(StorageMgrMessage.INVALID_PATH, pathInfo), HttpStatus.NOT_FOUND);
			}

			try {

				String relativePath = storageProvider.getRelativePath(pathInfo);
				StorageFile storageFile = storageProvider.getStorageFile(relativePath);

				String response = storageProvider.getStorage().getFileContent(storageFile);

				logger.log(StorageMgrMessage.JOB_ORDER_FILE_GOT, pathInfo);

				return new ResponseEntity<>(response, HttpStatus.OK);

			} catch (Exception e) {

				String msg = logger.log(StorageMgrMessage.JOB_ORDER_FILE_CANNOT_BE_GOT, pathInfo, e.getMessage());
				return new ResponseEntity<>(msg, HttpStatus.INTERNAL_SERVER_ERROR);
			}

		} // end version 2

		String response = "";
		if (pathInfo != null) {
			ProseoFile proFile = ProseoFile.fromPathInfo(pathInfo, cfg);
			// Find storage type
			if (proFile == null || proFile.getFsType() == StorageType.ALLUXIO) {
				loggerLegacy.warn("Invalid storage type for path: {}", pathInfo);
				return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
			}

			InputStream jofStream = proFile.getDataAsInputStream();
			if (jofStream != null) {
				byte[] bytes = null;

				try {
					bytes = java.util.Base64.getEncoder().encode(jofStream.readAllBytes());
				} catch (IOException e) {
					loggerLegacy.error("Invalid job order stream");
					return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
							e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				response = new String(bytes);
				try {
					jofStream.close();
				} catch (IOException e) {
					loggerLegacy.warn("Failed to close input stream of " + pathInfo + " | " + e.getMessage());
				}
				return new ResponseEntity<>(response, HttpStatus.OK);
			}
		}
		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}
	
	/**
	 * Checks if string base64
	 * 
	 * @param stringBase64 string to check
	 * @return true if string base64
	 */
	private boolean isStringBase64(String stringBase64) {

		String stringBase64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$";

		return stringBase64.matches(stringBase64Pattern) ? true : false;
	}

	/**
	 * Gets job order relative path "prefix/YYYY/MM/DD/WEEKDAY/HH/RandomUUID.xml
	 * 
	 * @param joborderPrefix job order prefix
	 * @return job order relative path
	 */
	private String getJobOrderRelativePath(String joborderPrefix) {

		String separator = "/";
		DateTime timestamp = DateTime.now(DateTimeZone.UTC);

		return joborderPrefix + separator + timestamp.getYear() + separator + timestamp.getMonthOfYear() + separator
				+ timestamp.getDayOfMonth() + separator + timestamp.getHourOfDay() + separator
				+ UUID.randomUUID().toString() + ".xml";
	}

	/**
	 * Creates Ok RestJobOrder response (uploaded = true)
	 * 
	 * @param storageFile  storage file
	 * @param stringBase64 string base 64
	 * @return Ok RestJobOrder response
	 */
	private RestJoborder createOkResponse(StorageFile storageFile, String stringBase64) {

		RestJoborder response = new RestJoborder();

		response.setFsType(storageFile.getStorageType().toString());
		response.setPathInfo(storageFile.getFullPath());
		response.setUploaded(true);
		response.setJobOrderStringBase64(stringBase64);

		return response;
	}

	/**
	 * Creates Bad RestJobOrder response (uploaded = false)
	 * 
	 * @param message      message
	 * @param stringBase64 string base 64
	 * @return Bad RestJobOrder response
	 */
	private RestJoborder createBadResponse(String message, String stringBase64) {

		RestJoborder response = new RestJoborder();

		response.setUploaded(false);
		response.setJobOrderStringBase64(stringBase64);
		response.setPathInfo("n/a");
		response.setMessage(message);

		return response;
	}

	/**
	 * Log an error and return the corresponding HTTP message header
	 * 
	 * @param messageFormat     the message text with parameter placeholders in
	 *                          String.format() style
	 * @param messageId         a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the
	 *                          message format)
	 * @return an HttpHeaders object with a formatted error message
	 */
	private HttpHeaders errorHeaders(String messageFormat, int messageId, Object... messageParameters) {

		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		loggerLegacy.error(message);

		// Create an HTTP "Warning" header
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + message);
		return responseHeaders;
	}
}
