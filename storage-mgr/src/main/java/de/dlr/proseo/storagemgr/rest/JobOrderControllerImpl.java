/**
 * JobOrderControllerImpl.java
 * 
 * (C) 2019 DLR
 */
package de.dlr.proseo.storagemgr.rest;

import java.util.UUID;

import javax.validation.Valid;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.StorageMgrMessage;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.rest.model.RestJoborder;

/**
 * Spring MVC controller for the prosEO Storage Manager; implements the services
 * required to manage Joborders
 * 
 * @author Denys Chaykovskiy
 * @author Hubert Asamer
 *
 */
@Component
public class JobOrderControllerImpl implements JoborderController {

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

		// Storage Manager version 2: String -> StorageFile

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

		// Storage Manager version 2: StorageFile -> String

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

}
