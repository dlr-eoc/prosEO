package de.dlr.proseo.storagemgr.rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.cache.FileCache;
import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;
import de.dlr.proseo.storagemgr.utils.StorageType;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.posix.PosixStorageFile;
import de.dlr.proseo.storagemgr.utils.ProseoFile;
import de.dlr.proseo.storagemgr.utils.StorageLogger;

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
	private static final String MSG_FILE_NOT_FOUND = "(E%d) File %s not found";
	private static final String MSG_FILE_COPIED = "(I%d) Requested object %s copied to target path %s";
	private static final String MSG_TARGET_PATH_MISSING = "(E%d) No target path given";
	private static final String MSG_FILES_UPDATED = "(I%d) Product file %s uploaded for product ID %d";
	private static final String MSG_READ_TIMEOUT = "(E%d) Read for file %s timed out after %s seconds";

	private static final int MSG_ID_EXCEPTION_THROWN = 4051;
	private static final int MSG_ID_FILE_COPIED = 4052;
	private static final int MSG_ID_FILE_NOT_FOUND = 4053;
	private static final int MSG_ID_FILES_UPDATED = 4054;
	private static final int MSG_ID_READ_TIMEOUT = 4055;

	// Same as in ProseFileS3
	private static final int MSG_ID_TARGET_PATH_MISSING = 4100;

	private static final String MSG_FILE_NOT_FETCHED = "Requested file {} not copied";

	// Lock table for products currently being downloaded from backend storage
	private static ConcurrentSkipListSet<String> productLockSet = new ConcurrentSkipListSet<>();

	private static Logger logger = LoggerFactory.getLogger(ProductfileControllerImpl.class);

	@Autowired
	private StorageManagerConfiguration cfg;

	StorageProvider storageProvider = new StorageProvider();

	/**
	 * Create an HTTP "Warning" header with the given text message
	 * 
	 * @param message the message text
	 * @return an HttpHeaders object with a warning message
	 */
	private HttpHeaders errorHeaders(String message) {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HTTP_HEADER_WARNING,
				HTTP_MSG_PREFIX + (null == message ? "null" : message.replaceAll("\n", " ")));
		return responseHeaders;
	}



	/**
	 * Copy source file named pathInfo to file cache used by processors. The local
	 * file name is: posixWorkerMountPoint + relative source file path
	 * 
	 * @param pathInfo Source file name
	 * @return Local file name
	 */
	@Override
	public ResponseEntity<RestFileInfo> getRestFileInfoByPathInfo(String pathInfo) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getRestFileInfoByPathInfo({})", pathInfo);
		// StorageLogger.trace() - can avoid if logger.isTraceEnabled()

		
		// pathInfo is absolute path s3://.. or /..   DOWNLOAD Storage -> Cache
		if (storageProvider.isVersion2()) {

			try {
				String storagePath = new PathConverter(pathInfo).removeFsPrefix().removeBucket().getPath();
				
				
				Storage storage = storageProvider.getStorage();
				String relativePath = storageProvider.getRelativePath(pathInfo);
				FileCache cache = FileCache.getInstance();

				//StorageFile sourceFile = storageProvider.getStorageFile(relativePath); 
				StorageFile sourceFile = storageProvider.getAbsoluteFile(pathInfo);
				StorageFile targetFile = storageProvider.getCacheFile(relativePath);
				
				if (!cache.containsKey(targetFile.getFullPath())) {
					
					storage.downloadFile(sourceFile, targetFile);
					cache.put(targetFile.getFullPath()); 
				}

				RestFileInfo restFileInfo = ControllerUtils.convertToRestFileInfo(targetFile,
						storageProvider.getCacheFileSize(relativePath));
				return HttpResponses.createOk(restFileInfo);

			} catch (Exception e) {
				return HttpResponses.createError("Cannot download file", e);
			}
			
		}

	
		if (null == pathInfo || pathInfo.isBlank())

		{
			return new ResponseEntity<>(
					errorHeaders(StorageLogger.logError(logger, MSG_TARGET_PATH_MISSING, MSG_ID_TARGET_PATH_MISSING)),
					HttpStatus.BAD_REQUEST);
		}

		ProseoFile sourceFile = ProseoFile.fromPathInfo(pathInfo, cfg);
		ProseoFile targetFile = ProseoFile.fromPathInfo(cfg.getPosixCachePath() + "/" + sourceFile.getRelPathAndFile(),
				cfg);

		// Acquire lock on requested product file
		try {
			int i = 0;
			for (; i < cfg.getFileCheckMaxCycles(); ++i) {
				synchronized (productLockSet) {
					if (!productLockSet.contains(sourceFile.getFileName())) {
						productLockSet.add(sourceFile.getFileName());
						break;
					}
				}
				if (logger.isDebugEnabled())
					logger.debug("... waiting for concurrent access to {} to terminate", sourceFile.getFileName());
				Thread.sleep(500);
			}
			;
			if (i == cfg.getFileCheckMaxCycles()) {
				return new ResponseEntity<>(
						errorHeaders(StorageLogger.logError(logger, MSG_READ_TIMEOUT, MSG_ID_READ_TIMEOUT,
								sourceFile.getFileName(),
								cfg.getFileCheckMaxCycles() * cfg.getFileCheckWaitTime() / 1000)),
						HttpStatus.SERVICE_UNAVAILABLE);
			}
		} catch (InterruptedException e) {
			return new ResponseEntity<>(errorHeaders(StorageLogger.logError(logger, MSG_EXCEPTION_THROWN,
					MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		try {
			ArrayList<String> transferredFiles = sourceFile.copyTo(targetFile, false);
			if (transferredFiles != null && !transferredFiles.isEmpty()) {
				RestFileInfo response = new RestFileInfo();
				response.setStorageType(targetFile.getFsType().toString());
				response.setFilePath(targetFile.getFullPath());
				response.setFileName(targetFile.getFileName());
				response.setFileSize(targetFile.getLength());

				StorageLogger.logInfo(logger, MSG_FILE_COPIED, MSG_ID_FILE_COPIED, sourceFile.getFullPath(),
						targetFile.getFullPath());

				return new ResponseEntity<>(response, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(
						errorHeaders(
								StorageLogger.logError(logger, MSG_FILE_NOT_FOUND, MSG_ID_FILE_NOT_FOUND, pathInfo)),
						HttpStatus.NOT_FOUND);
			}
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<RestFileInfo>(errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			return new ResponseEntity<>(errorHeaders(StorageLogger.logError(logger, MSG_EXCEPTION_THROWN,
					MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())),
					HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			productLockSet.remove(sourceFile.getFileName());
		}
	}

	/**
	 * Copy local file named pathInfo to storage manager. The target file path is:
	 * default mount point + productId + relative source file path
	 * 
	 * @param pathInfo  Source file name
	 * @param productId Product id
	 * @return Target file name
	 */
	@Override
	public ResponseEntity<RestFileInfo> updateProductfiles(String pathInfo, Long productId, Long fileSize) {

		if (logger.isTraceEnabled())
			logger.trace(">>> updateProductfiles({}, {})", pathInfo, productId);

		
		// pathInfo absolute path, UPLOAD absolute file -> storage 
		if (storageProvider.isVersion2()) {

			try {
				Storage storage = storageProvider.getStorage();
				String absolutePath = pathInfo;
				// String relativePath = storageProvider.getRelativePath(absolutePath);
				String fileName = new File(pathInfo).getName();
				String productFolderWithFilename = Paths.get(String.valueOf(productId), fileName).toString();

				StorageFile sourceFile = storageProvider.getAbsoluteFile(absolutePath);
				StorageFile targetFile = storageProvider.getStorageFile(productFolderWithFilename);

				storage.uploadFile(sourceFile, targetFile);

				RestFileInfo restFileInfo = ControllerUtils.convertToRestFileInfo(targetFile,
						storage.getFileSize(targetFile));
				return HttpResponses.createCreated(restFileInfo);

			} catch (Exception e) {
				return HttpResponses.createError("Cannot upload file", e);
			}
		}
		
		

		RestFileInfo response = new RestFileInfo();
		if (pathInfo != null) {
			ProseoFile sourceFile = ProseoFile.fromPathInfo(pathInfo, cfg);
			ProseoFile targetFile = ProseoFile.fromType(StorageType.valueOf(cfg.getDefaultStorageType()),
					String.valueOf(productId) + "/" + sourceFile.getFileName(), cfg);
			try {
				// wait until source file is really copied
				if (sourceFile.getFsType() == StorageType.POSIX) {
					int i = 0;
					Path fp = Path.of(sourceFile.getFullPath());
					if (fp.toFile().isFile()) {
						Long wait = cfg.getFileCheckWaitTime();
						Long max = cfg.getFileCheckMaxCycles();
						try {
							while (Files.size(fp) < fileSize && i < max) {
								if (logger.isDebugEnabled()) {
									logger.debug("Wait for fully copied file {}", sourceFile.getFullPath());
								}
								i++;
								try {
									Thread.sleep(wait);
								} catch (InterruptedException e) {
									return new ResponseEntity<>(
											errorHeaders(StorageLogger.logError(logger, MSG_READ_TIMEOUT,
													MSG_ID_READ_TIMEOUT, sourceFile.getFileName(),
													cfg.getFileCheckMaxCycles() * cfg.getFileCheckWaitTime() / 1000)),
											HttpStatus.SERVICE_UNAVAILABLE);
								}
							}
						} catch (IOException e) {
							logger.error("Unable to access file {}", sourceFile.getFullPath());
							return new ResponseEntity<>(
									errorHeaders(StorageLogger.logError(logger, MSG_EXCEPTION_THROWN,
											MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())),
									HttpStatus.INTERNAL_SERVER_ERROR);
						}
						if (i >= max) {
							logger.error(MSG_FILE_NOT_FETCHED, sourceFile.getFullPath());
						}
					}
				}
				ArrayList<String> transfered = sourceFile.copyTo(targetFile, false);

				if (transfered != null && !transfered.isEmpty()) {
					response.setStorageType(targetFile.getFsType().toString());
					response.setFilePath(targetFile.getFullPath());
					response.setFileName(targetFile.getFileName());
					response.setFileSize(targetFile.getLength());

					StorageLogger.logInfo(logger, MSG_FILES_UPDATED, MSG_ID_FILES_UPDATED, pathInfo, productId);

					return new ResponseEntity<>(response, HttpStatus.CREATED);
				}
			} catch (Exception e) {
				return new ResponseEntity<>(errorHeaders(StorageLogger.logError(logger, MSG_EXCEPTION_THROWN,
						MSG_ID_EXCEPTION_THROWN, e.getClass().toString() + ": " + e.getMessage())),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		return new ResponseEntity<RestFileInfo>(response, HttpStatus.NOT_FOUND);
	}
}
