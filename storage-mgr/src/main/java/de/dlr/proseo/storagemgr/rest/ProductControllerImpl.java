/**
 * StorageControllerImpl.java
 * 
 * (C) 2019 DLR
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.rest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.FsType;
import de.dlr.proseo.storagemgr.rest.model.RestProductFS;
import de.dlr.proseo.storagemgr.rest.model.StorageType;
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

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-storage-mgr ";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final int MSG_ID_EXCEPTION_THROWN = 4001;
	private static Logger logger = LoggerFactory.getLogger(ProductControllerImpl.class);

	@Autowired
	private StorageManagerConfiguration cfg;

	/**
	 * Log an error and return the corresponding HTTP message header
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return an HttpHeaders object with a formatted error message
	 */
	private HttpHeaders errorHeaders(String messageFormat, int messageId, Object... messageParameters) {
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

	@Override
	public ResponseEntity<RestProductFS> createRestProductFS(@Valid RestProductFS restProductFS) {
		// get node name info...
		String hostName ="";
		try {
			InetAddress iAddress = InetAddress.getLocalHost();
			hostName = iAddress.getHostName();
		} catch (UnknownHostException e1) {

		}

		RestProductFS response = new RestProductFS();
		
		logger.info(restProductFS.toString());
		String pref = restProductFS.getProductId();
		
		ArrayList<String> transferSum = new ArrayList<String>();

		ProseoFile targetFile = ProseoFile.fromType(FsType.fromValue(restProductFS.getTargetStorageType().toString()), pref, cfg);

		try {
			for (String fileOrDir : restProductFS.getSourceFilePaths()) {
				ProseoFile sourceFile = ProseoFile.fromTypeFullPath(FsType.fromValue(restProductFS.getSourceStorageType().toString()), fileOrDir, cfg);
				ArrayList<String> transfered = sourceFile.copyTo(targetFile, true);
				if (logger.isDebugEnabled()) logger.debug("Files transferred: {}", transfered);
				if (transfered != null) {
					transferSum.addAll(transfered);
				}
			}
			if (logger.isDebugEnabled()) logger.debug("Files registered: {}", transferSum);
			setRestProductFS(response, restProductFS, cfg.getS3DefaultBucket(), true, targetFile.getFullPath() + "/",
							 transferSum, false, "registration executed on node "+hostName);
			return new ResponseEntity<>(response, HttpStatus.CREATED);
		} catch (Exception e) {
			return new ResponseEntity<>(
					errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<List<RestProductFS>> getRestProductFs(StorageType storageType, String storageId, Long id) {
		List<RestProductFS> response = new ArrayList<RestProductFS>();
		return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);
	}
	
	private RestProductFS setRestProductFS(RestProductFS response,
			RestProductFS restProductFS,
			String storageId,
			Boolean registered,
			String registeredFilePath,
			List<String> registeredFiles,
			Boolean deleted,
			String msg
			) {
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

	@Override
	public ResponseEntity<?> getObject(String pathInfo, Boolean zip, Long fromByte, Long toByte) {
		List<RestProductFS> response = new ArrayList<RestProductFS>();
		return new ResponseEntity<>(response, HttpStatus.NOT_IMPLEMENTED);		
	}

	@Override
	public ResponseEntity<RestProductFS> deleteProductByPathInfo(String pathInfo) {
		// TODO Auto-generated method stub
		return null;
	}

}
