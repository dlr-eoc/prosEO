package de.dlr.proseo.storagemgr.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;
import de.dlr.proseo.storagemgr.utils.StorageType;
import de.dlr.proseo.storagemgr.utils.ProseoFile;

/**
 * Spring MVC controller for the prosEO Storage Manager; implements the services
 * required to manage product files
 * 
 * @author Ernst Melchinger
 *
 */

@Component
public class ProductfileControllerImpl implements ProductfileController {

	private static final String HTTP_HEADER_WARNING = "Warning";
	private static final String HTTP_MSG_PREFIX = "199 proseo-storage-mgr ";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown: %s";
	private static final int MSG_ID_EXCEPTION_THROWN = 4001;
	private static final String MSG_FILE_NOT_FETCHED = "Requested file {} not copied";
		
	private static Logger logger = LoggerFactory.getLogger(ProductfileControllerImpl.class);
	
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

	/**
	 * Copy source file named pathInfo to local storage used by workers.
	 * The local file name is: posixWorkerMountPoint + relative source file path
	 * @param pathInfo Source file name
	 * @return Local file name
	 */
	@Override
	public ResponseEntity<RestFileInfo> getRestFileInfoByPathInfo(String pathInfo) {
		RestFileInfo response = new RestFileInfo();
		if (pathInfo != null) {
			ProseoFile sourceFile = ProseoFile.fromPathInfo(pathInfo, cfg);
			ProseoFile targetFile = ProseoFile.fromPathInfo(cfg.getPosixWorkerMountPoint() + "/" + sourceFile.getRelPathAndFile(), cfg);
			

			try {
					ArrayList<String> transfered = sourceFile.copyTo(targetFile, false);
					if (transfered != null && !transfered.isEmpty()) {
						response.setStorageType(targetFile.getFsType().toString());
						response.setFilePath(targetFile.getFullPath());
						response.setFileName(targetFile.getFileName());
						response.setFileSize(targetFile.getLength());
						return new ResponseEntity<>(response, HttpStatus.OK);
					}
			} catch (Exception e) {
				return new ResponseEntity<>(
						errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return new ResponseEntity<RestFileInfo>(response, HttpStatus.NOT_FOUND);
	}

	/** 
	 * Copy local file named pathInfo to storage manager.
	 * The target file path is: default mount point + productId + relative source file path
	 * 
	 * @param pathInfo Source file name
	 * @param productId Product id
	 * @return Target file name
	 */
	@Override
	public ResponseEntity<RestFileInfo> updateProductfiles(String pathInfo, Long productId, Long fileSize) {
		RestFileInfo response = new RestFileInfo();
		if (pathInfo != null) {
			ProseoFile sourceFile = ProseoFile.fromPathInfo(pathInfo, cfg);
			String targetRelPath = String.valueOf(productId);
			String aPath = sourceFile.getRelPathAndFile();
			while (aPath.startsWith("/")) {
				aPath = aPath.substring(1);			
			}
			String relPath = "";
			int pos = aPath.indexOf('/');
			if (pos >= 0) {
				relPath = aPath.substring(pos + 1);
			} else {
				relPath = aPath;
			}
			// replace top relPath directory
			ProseoFile targetFile = ProseoFile.fromType(StorageType.valueOf(cfg.getDefaultStorageType()), targetRelPath + "/" + relPath, cfg);
			try {
				// wait until source file is really copied
				if (sourceFile.getFsType() == StorageType.POSIX) {
					int i = 0;
					Path fp = Path.of(sourceFile.getFullPath());
					if (fp.toFile().isFile()) {
						Integer wait = Integer.valueOf(cfg.getFileCheckWaitTime());
						Integer max = Integer.valueOf(cfg.getFileCheckMaxCycles());
						try {
							while (Files.size(fp) < fileSize && i < max) {
								if (logger.isDebugEnabled()) {
									logger.debug("Wait for fully copied file {}", sourceFile.getFullPath());
								}
								i++;
								try {
									this.wait(wait);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						} catch (IOException e) {
							logger.error("Unable to access file {}", sourceFile.getFullPath());
							return new ResponseEntity<>(
									errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
									HttpStatus.INTERNAL_SERVER_ERROR);
						}
						if (i >= max) {
							logger.error(MSG_FILE_NOT_FETCHED, aPath);
						}
					}
				}
				ArrayList<String> transfered = sourceFile.copyTo(targetFile, false);
				
				if (transfered != null && !transfered.isEmpty()) {
					response.setStorageType(targetFile.getFsType().toString());
					response.setFilePath(targetFile.getFullPath());
					response.setFileName(targetFile.getFileName());
					response.setFileSize(targetFile.getLength());
					return new ResponseEntity<>(response, HttpStatus.CREATED);
				}
			} catch (Exception e) {
				return new ResponseEntity<>(
						errorHeaders(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage()), 
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return new ResponseEntity<RestFileInfo>(response, HttpStatus.NOT_FOUND);
	}
}
