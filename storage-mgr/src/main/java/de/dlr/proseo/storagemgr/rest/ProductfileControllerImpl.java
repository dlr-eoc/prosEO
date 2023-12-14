package de.dlr.proseo.storagemgr.rest;

import java.io.File;
import java.io.IOException;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.StorageMgrMessage;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.StorageProvider;
import de.dlr.proseo.storagemgr.Exceptions.FileLockedAfterMaxCyclesException;
import de.dlr.proseo.storagemgr.cache.FileCache;
import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;
import de.dlr.proseo.storagemgr.utils.StorageFileLocker;

/**
 * Spring MVC controller for the prosEO Storage Manager; implements the services
 * required to manage product files
 * 
 * @author Ernst Melchinger
 * @author Denys Chaykovskiy
 *
 */

@Component
public class ProductfileControllerImpl implements ProductfileController {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductControllerImpl.class);
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.STORAGE_MGR);

	@Autowired
	private StorageManagerConfiguration cfg;

	@Autowired
	private StorageProvider storageProvider;

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

		if (null == pathInfo) {
			String msg = logger.log(StorageMgrMessage.PATH_IS_NULL);
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.BAD_REQUEST);
		}

		if (pathInfo == "" || pathInfo.isBlank()) {
			String msg = logger.log(StorageMgrMessage.INVALID_PATH, pathInfo);
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.BAD_REQUEST);
		}

		// Storage Manager version 2: download Storage -> Cache
		// pathInfo is absolute path s3://bucket/.. or /storagePath/..

		String absoluteStoragePath = pathInfo;

		StorageFile cacheFile;
		try {

			String relativePath = storageProvider.getRelativePath(absoluteStoragePath);
			cacheFile = storageProvider.getCacheFile(relativePath);

		} catch (IOException e) {

			String msg = logger.log(StorageMgrMessage.PRODUCT_FILE_CANNOT_BE_DOWNLOADED, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.BAD_REQUEST);
		}

		StorageFileLocker fileLocker = new StorageFileLocker(cacheFile.getFullPath(), cfg.getFileCheckWaitTime(),
				cfg.getFileCheckMaxCycles());

		try {

			RestFileInfo restFileInfo = synchronizedDownloadFromStorageToCache(absoluteStoragePath, fileLocker);
			return new ResponseEntity<>(restFileInfo, HttpStatus.OK);

		} catch (FileLockedAfterMaxCyclesException e) {

			String time = String.valueOf(cfg.getFileCheckMaxCycles() * cfg.getFileCheckWaitTime() / 1000);
			String msg = logger.log(StorageMgrMessage.READ_TIME_OUT, absoluteStoragePath, time,
					e.getLocalizedMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.SERVICE_UNAVAILABLE);

		} catch (IOException e) {

			String msg = logger.log(StorageMgrMessage.PRODUCT_FILE_CANNOT_BE_DOWNLOADED, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.BAD_REQUEST);

		} catch (Exception e) {

			String msg = logger.log(StorageMgrMessage.INTERNAL_ERROR, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.INTERNAL_SERVER_ERROR);

		} finally {

			fileLocker.unlock();
			logger.debug("... unlocked the file: ", pathInfo);
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

		// uploads absolute external file -> cache -> storage
		// 1. download to cache absolute-file -> cache
		// 2. add to cache cache.put()
		// 3. upload to storage cache -> storage

		if (pathInfo == null) {
			return new ResponseEntity<RestFileInfo>(new RestFileInfo(), HttpStatus.BAD_REQUEST);
		}

		String absoluteExternalPath = pathInfo;

		String relativePath = getProductFolderWithFilename(absoluteExternalPath, productId);
		StorageFile cacheFile = storageProvider.getCacheFile(relativePath);

		StorageFileLocker fileLocker = new StorageFileLocker(cacheFile.getFullPath(), cfg.getFileCheckWaitTime(),
				cfg.getFileCheckMaxCycles());

		try {

			RestFileInfo restFileInfo = synchronizedDownloadFromAbsolutePathToCache(absoluteExternalPath, productId,
					fileSize, fileLocker);
			fileLocker.unlock();
			restFileInfo = synchronizedUploadFromCacheToStorage(relativePath, fileLocker);

			logger.log(StorageMgrMessage.PRODUCT_FILE_UPLOADED, absoluteExternalPath, productId);
			return new ResponseEntity<>(restFileInfo, HttpStatus.CREATED);

		} catch (Exception e) {

			String msg = logger.log(StorageMgrMessage.INTERNAL_ERROR, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.INTERNAL_SERVER_ERROR);
			
		} finally {

			fileLocker.unlock();
			logger.debug("... unlocked the file: ", absoluteExternalPath);
		}
	}

	/**
	 * Converts storage file to rest file info (generated model)
	 * 
	 * @param storageFile storage file
	 * @param fileSize    file size
	 * 
	 * @return rest file info
	 */
	private static RestFileInfo convertToRestFileInfo(StorageFile storageFile, long fileSize) {

		RestFileInfo restFileInfo = new RestFileInfo();

		restFileInfo.setStorageType(storageFile.getStorageType().toString());
		restFileInfo.setFilePath(storageFile.getFullPath());
		restFileInfo.setFileName(storageFile.getFileName());
		restFileInfo.setFileSize(fileSize);

		return restFileInfo;
	}

	private RestFileInfo synchronizedDownloadFromStorageToCache(String storageFilePath, StorageFileLocker fileLocker)
			throws FileLockedAfterMaxCyclesException, IOException, Exception {

		// relative path depends on path, not on actual storage
		String relativePath = storageProvider.getRelativePath(storageFilePath);

		StorageFile sourceFile = storageProvider.getStorageFile(relativePath);
		StorageFile targetFile = storageProvider.getCacheFile(sourceFile.getRelativePath());

		FileCache cache = FileCache.getInstance();

		if (!cache.containsKey(targetFile.getFullPath())) {

			fileLocker.lockOrWaitUntilUnlockedAndLock();

			if (!cache.containsKey(targetFile.getFullPath())) {

				// active thread - downloads the file and puts it to the cache
				storageProvider.getStorage().downloadFile(sourceFile, targetFile);
				logger.log(StorageMgrMessage.PRODUCT_FILE_DOWNLOADED, targetFile.getFullPath());

				cache.put(targetFile.getFullPath());

			} else {

				// passive thread - does nothing, waited for downloaded file and use it from cache
				logger.debug("... waiting-thread when the file downloaded and use it from cache: ",
						targetFile.getFullPath());
			}

		} else {
			logger.debug("... no download and no lock - the file is in cache: ", targetFile.getFullPath());
		}

		RestFileInfo restFileInfo = convertToRestFileInfo(targetFile,
				storageProvider.getCacheFileSize(sourceFile.getRelativePath()));

		return restFileInfo;
	}

	private RestFileInfo synchronizedDownloadFromAbsolutePathToCache(String absoluteExternalPath, Long productId,
			Long fileSize, StorageFileLocker fileLocker)
			throws FileLockedAfterMaxCyclesException, IOException, Exception {

		String productFolderWithFilename = getProductFolderWithFilename(absoluteExternalPath, productId);
		StorageFile targetCacheFile = storageProvider.getCacheFile(productFolderWithFilename);

		FileCache cache = FileCache.getInstance();

		if (!cache.containsKey(targetCacheFile.getFullPath())) {

			fileLocker.lockOrWaitUntilUnlockedAndLock();

			// After lock() the active thread starts to download the file and put it to the
			// cache
			// (see below)
			// After lock() the passive thread did nothing, but the file has been downloaded
			// and the cache has been updated - need to check if file contains in the cache
			// again

			if (!cache.containsKey(targetCacheFile.getFullPath())) {

				// I am active thread - downloads the file and puts it to the cache

				// TODO: WIP
				storageProvider.copyAbsoluteFilesToCache(absoluteExternalPath, productId);

				logger.log(StorageMgrMessage.PRODUCT_FILE_DOWNLOADED_FROM_EXTERNAL_TO_CACHE,
						targetCacheFile.getFullPath());

				cache.put(targetCacheFile.getFullPath());
			} else {

				// passive thread - did nothing, just waited until the file has been downloaded
				// and use it from cache
				logger.debug(
						"... waiting-thread when the file downloaded to cache from external storage and use it from cache: ",
						targetCacheFile.getFullPath());
			}

		} else {
			logger.debug("... no download and no lock - the file is in cache: ", targetCacheFile.getFullPath());
		}

		RestFileInfo restFileInfo = convertToRestFileInfo(targetCacheFile,
				storageProvider.getCacheFileSize(targetCacheFile.getRelativePath()));

		return restFileInfo;
	}

	private RestFileInfo synchronizedUploadFromCacheToStorage(String relativeCachePath, StorageFileLocker fileLocker)
			throws FileLockedAfterMaxCyclesException, IOException, Exception {

		Storage storage = storageProvider.getStorage();

		StorageFile sourceCacheFile = storageProvider.getCacheFile(relativeCachePath);
		StorageFile targetStorageFile = storageProvider.getStorageFile(relativeCachePath);

		FileCache cache = FileCache.getInstance();

		if (!cache.containsKey(sourceCacheFile.getFullPath())) {

			fileLocker.lockOrWaitUntilUnlockedAndLock();

			// After lock() the active thread starts to download the file and put it to the
			// cache
			// (see below)
			// After lock() the passive thread did nothing, but the file has been downloaded
			// and the cache has been updated - need to check if file contains in the cache
			// again

			if (!cache.containsKey(sourceCacheFile.getFullPath())) {

				// I am active thread - uploads the file and puts it to the cache

				storage.uploadFile(sourceCacheFile, targetStorageFile);

				logger.log(StorageMgrMessage.PRODUCT_FILE_DOWNLOADED_FROM_EXTERNAL_TO_CACHE,
						targetStorageFile.getFullPath());

				cache.put(targetStorageFile.getFullPath());
			} else {

				// passive thread - did nothing, just waited until the file has been uploaded
				// and use it from cache
				logger.debug("... waiting-thread when the file downloaded and use it from cache: ",
						targetStorageFile.getFullPath());
			}

		} else {
			logger.debug("... no download and no lock - the file is in cache: ", targetStorageFile.getFullPath());
		}

		RestFileInfo restFileInfo = convertToRestFileInfo(targetStorageFile,
				storageProvider.getCacheFileSize(sourceCacheFile.getRelativePath()));

		return restFileInfo;
	}

	private String getProductFolderWithFilename(String absoluteExternalPath, Long productId) {

		String fileName = new File(absoluteExternalPath).getName();
		return Paths.get(String.valueOf(productId), fileName).toString();
	}

}
