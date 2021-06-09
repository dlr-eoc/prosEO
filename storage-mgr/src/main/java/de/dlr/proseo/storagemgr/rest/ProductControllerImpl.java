/**
 * StorageControllerImpl.java
 * 
 * (C) 2019 DLR
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.rest;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.utils.StorageType;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.RestProductFS;
import de.dlr.proseo.storagemgr.utils.ProseoFile;

/**
 * Spring MVC controller for the prosEO Storage Manager; implements the services
 * required to manage any object storage, e. g. a storage based on the AWS S3
 * API
 * 
 * @author Hubert Asamer
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProductControllerImpl implements ProductController {

	/* Message IDs */
	private static final int MSG_ID_EXCEPTION_THROWN = 4001;
	private static final int MSG_ID_FILE_NOT_FOUND = 4002;
	private static final int MSG_ID_INVALID_PATH = 4003;

	/* Message strings */
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final String MSG_FILE_NOT_FOUND = "(E%d) File %s not found";
	private static final String MSG_INVALID_PATH = "(E%d) Invalid path %s";

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-storage-mgr ";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductControllerImpl.class);

	/** Storage Manager configuration */
	@Autowired
	private StorageManagerConfiguration cfg;

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
		
		if (logger.isTraceEnabled()) logger.trace(">>> errorHeaders({}, {}, {})", 
				messageFormat + " ", messageId + " ", "messageParameters");  
		
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.error(message);

		// Create an HTTP "Warning" header
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING, HTTP_MSG_PREFIX + message);
		return responseHeaders;
	}

	/**
	 * Copy a file from "ingest" file system to storage manager controlled prosEO cache.
	 * Source and target are defined in the restProductFS structure
	 * 
	 * @param restProductFS the ingest file information
	 * @return a response entity containing
	 *     HTTP status CREATED and the ingest file information updated with the file paths after ingestion on success, or
	 *     HTTP status INTERNAL_SERVER_ERROR and an error message
	 */
	@Override
	public ResponseEntity<RestProductFS> createRestProductFS(@Valid RestProductFS restProductFS) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> createRestProductFs({})", 
				(null == restProductFS ? "MISSING" : restProductFS.getProductId()));
		
		// get node name info...
		String hostName = "";
		try {
			InetAddress iAddress = InetAddress.getLocalHost();
			hostName = iAddress.getHostName();
		} catch (UnknownHostException e1) {

		}

		RestProductFS response = new RestProductFS();

		logger.info(restProductFS.toString());
		String pref = restProductFS.getProductId();
		if (!pref.endsWith("/")) {
			pref = pref + "/";
		}

		ArrayList<String> transferSum = new ArrayList<String>();

		ProseoFile targetFile = ProseoFile.fromType(StorageType.valueOf(restProductFS.getTargetStorageType()),
				pref, cfg);

		try {
			for (String fileOrDir : restProductFS.getSourceFilePaths()) {
				ProseoFile sourceFile = ProseoFile.fromTypeFullPath(
						StorageType.valueOf(restProductFS.getSourceStorageType()), fileOrDir, cfg);
				ArrayList<String> transfered = sourceFile.copyTo(targetFile, true);
				if (logger.isDebugEnabled()) logger.debug("Files transferred: {}", transfered);
				if (transfered != null) {
					transferSum.addAll(transfered);
				}
			}
			if (logger.isDebugEnabled()) logger.debug("Files registered: {}", transferSum);
			setRestProductFS(response, restProductFS, targetFile.getBasePath(), true, targetFile.getFullPath() + "/",
					transferSum, false, "registration executed on node " + hostName);
			return new ResponseEntity<>(response, HttpStatus.CREATED);
		} catch (Exception e) {
			return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
					e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * List the file/object contents of a repository.
	 * 
	 * @param storageType S2, POSIX or null
	 * @param prefix Path information
	 * @return a response entity containing
	 *     HTTP status OK or PARTIAL_CONTENT and list of file (object) paths on success, or
	 *     HTTP status INTERNAL_SERVER_ERROR and an error message
	 */
	@Override
	public ResponseEntity<List<String>> getProductFiles(String storageType, String prefix) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> getProductFiles({}, {})", 
				storageType + " ", prefix);
		
		List<StorageType> stl = new ArrayList<StorageType>();
		List<String> response = new ArrayList<String>();
		try {
			if (storageType == null) {
				stl.add(StorageType.S3);
				stl.add(StorageType.POSIX);
			} else {
				stl.add(StorageType.valueOf(storageType));
			}
			for (StorageType st : stl) {
				listProductFiles(st, prefix, response);
			}	
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
					e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
		}	
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	/**
	 * Set the members of RestProductFS response.
	 * 
	 * @param response the ingest info structure to update
	 * @param restProductFS the ingest info structure to copy product ID and source information from
	 * @param storageId the ID of the storage used
	 * @param registered true, if the requested files have been ingested, false otherwise
	 * @param registeredFilePath common path to the ingested files
	 * @param registeredFiles file names after ingestion
	 * @param deleted true, if the files were deleted, false otherwise
	 * @param msg a response message text
	 * @return the updated response object
	 */
	private RestProductFS setRestProductFS(RestProductFS response, RestProductFS restProductFS, String storageId,
			Boolean registered, String registeredFilePath, List<String> registeredFiles, Boolean deleted, String msg) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> setRestProductFS({}, {}, {}, {}, {}, {}, {}, {})", 
				(null == response ? "MISSING" : response.getProductId() + " "),
				(null == restProductFS ? "MISSING" : restProductFS.getProductId() + " "),
				storageId + " ", registered + " ", registeredFilePath + " ", 
				registeredFiles.size() + " ", deleted + " ", msg + " " );
		
		if (response != null && restProductFS != null) {
			response.setProductId(restProductFS.getProductId());
			response.setTargetStorageId(storageId);
			response.setRegistered(registered);
			response.setRegisteredFilePath(registeredFilePath);
			response.setSourceFilePaths(restProductFS.getSourceFilePaths());
			response.setSourceStorageType(restProductFS.getSourceStorageType());
			response.setTargetStorageType(restProductFS.getTargetStorageType());
			response.setRegisteredFilesCount(Long.valueOf(registeredFiles.size()));
			response.setRegisteredFilesList(registeredFiles);
			response.setDeleted(deleted);
			response.setMessage(msg);
		}
		if (logger.isDebugEnabled()) logger.debug("Response created: {}", response);
		return response;
	}

	/**
	 * Retrieve the byte stream for download of a file object in repository.
	 * 
	 * @param pathInfo Path to object
	 * @param fromByte Start byte, 0 if not set
	 * @param toByte End byte, end of object data if not set
	 * @return a response entity containing
	 *     HTTP status OK or PARTIAL_CONTENT and the byte stream on success, or
	 *     HTTP status NOT_FOUND and an error message, or
	 *     HTTP status INTERNAL_SERVER_ERROR and an error message
	 */
	@Override
	public ResponseEntity<?> getObject(String pathInfo, Long fromByte, Long toByte) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> getObject({}, {}, {})", 
				pathInfo + " ", fromByte + " ", toByte + " ");
		
		if (pathInfo != null) {
			try {
				ProseoFile sourceFile = ProseoFile.fromPathInfo(pathInfo, cfg);
				if (sourceFile == null) {
					return new ResponseEntity<>(errorHeaders(MSG_INVALID_PATH, MSG_ID_INVALID_PATH,
							pathInfo), HttpStatus.BAD_REQUEST);
				}
				InputStream stream = sourceFile.getDataAsInputStream();
				if (stream == null) {
					return new ResponseEntity<>(errorHeaders(MSG_FILE_NOT_FOUND, MSG_ID_FILE_NOT_FOUND,
							pathInfo), HttpStatus.NOT_FOUND);
				}
				Long len = sourceFile.getLength();
				HttpHeaders headers = new HttpHeaders();
				headers.setContentDispositionFormData("attachment", sourceFile.getFileName());
				long from = 0;
				long to = len - 1;
				HttpStatus status = HttpStatus.OK;
				if (fromByte != null || toByte != null) {
					List<HttpRange> ranges = new ArrayList<HttpRange>();
					if (fromByte != null) {
						from = fromByte;
						stream.skip(from);
					}
					if (toByte != null) {
						to = Math.min(toByte, len - 1);
					}
					len = to - from + 1;
					HttpRange range = HttpRange.createByteRange(from, to);
					ranges.add(range);
					headers.setRange(ranges);
					headers.setContentType(new MediaType("multipart", "byteranges"));
					status = HttpStatus.PARTIAL_CONTENT;
				} else {
					headers.setContentType(new MediaType("application", sourceFile.getExtension()));
				}
				headers.setContentLength(len);
				InputStreamResource fsr = new InputStreamResource(stream);
				if (fsr != null) {
					return new ResponseEntity<>(fsr, headers, status);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
						e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return new ResponseEntity<>(errorHeaders(MSG_FILE_NOT_FOUND, MSG_ID_FILE_NOT_FOUND,
				pathInfo), HttpStatus.NOT_FOUND);
	}

	/**
	 * Delete object(s) 
	 * 
	 * @param pathInfo path to the object or directory
	 * 
	 * @return a response entity containing
	 *     HTTP status OK and the full metadata of the deleted object, or
	 *     HTTP status NOT_FOUND and an error message, or
	 *     HTTP status INTERNAL_SERVER_ERROR and an error message
	 */
	@Override
	public ResponseEntity<RestProductFS> deleteProductByPathInfo(String pathInfo) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> deleteProductByPathInfo({})", pathInfo);
		
		RestProductFS response = new RestProductFS();
		if (pathInfo != null) {
			ProseoFile sourceFile = ProseoFile.fromPathInfo(pathInfo, cfg);
			try {
				ArrayList<String> deleted = sourceFile.delete();
				if (deleted != null && !deleted.isEmpty()) {
					response.setProductId("");
					response.setDeleted(true);
					response.setRegistered(false);
					response.setSourceFilePaths(deleted);
					response.setSourceStorageType(sourceFile.getFsType().toString());
					return new ResponseEntity<>(response, HttpStatus.OK);
				}
			} catch (Exception e) {
				return new ResponseEntity<>(errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN,
						e.getClass().toString() + ": " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return new ResponseEntity<>(errorHeaders(MSG_FILE_NOT_FOUND, MSG_ID_FILE_NOT_FOUND,
				pathInfo), HttpStatus.NOT_FOUND);
	}
	
	/**
	 * List file objects of repository. Collect result in response.
	 * 
	 * @param st the storage type
	 * @param prefix relative path to list
	 * @param response the ingest information response to fill
	 */
	private void listProductFiles(StorageType st, String prefix, List<String> response) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> listProductFiles({}, {})", 
				(null == st ? "MISSING" : st.toString() + " "),
				prefix + " ", response.size());
		
		ProseoFile path = null;
		if (prefix == null) {
			path = ProseoFile.fromType(st, "", cfg);
		} else {
			path = ProseoFile.fromType(st, prefix + "/", cfg);
		}
		List<ProseoFile> files = path.list();
		for (ProseoFile f : files) {
			String fs = f.getFsType().toString() + "|" + f.getFullPath();
			response.add(fs);
		}
	}

}
