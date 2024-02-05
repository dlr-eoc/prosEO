package de.dlr.proseo.storagemgr.rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

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
import de.dlr.proseo.storagemgr.cache.CacheFileStatus;
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

	/** The file permission with read-only permissions for all users */
	private static String READ_ONLY_FOR_ALL_USERS = "r--r--r--";

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

		// 1. copy Storage -> Cache
		// 2. add to cache cache.put(cache file)
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

			RestFileInfo restFileInfo = copyFileStorageToCache(absoluteStoragePath, fileLocker);
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

		// copies absolute external file -> cache file -> storage file
		// 1. copy external -> cache
		// 2. add to cache cache.put(cache file)
		// 3. copy cache -> storage
		// pathInfo is absolute external path

		if (pathInfo == null) {
			return new ResponseEntity<RestFileInfo>(new RestFileInfo(), HttpStatus.BAD_REQUEST);
		}

		String externalPath = pathInfo;

		String relativePath = getProductFolderWithFilename(externalPath, productId);
		StorageFile cacheFile = storageProvider.getCacheFile(relativePath);

		StorageFileLocker fileLocker = new StorageFileLocker(cacheFile.getFullPath(), cfg.getFileCheckWaitTime(),
				cfg.getFileCheckMaxCycles());

		try {

			RestFileInfo restFileInfo = copyFileExternalToCache(externalPath, productId, fileSize, fileLocker);
			fileLocker.unlock();
			restFileInfo = copyFileCacheToStorage(relativePath, fileLocker);

			logger.log(StorageMgrMessage.PRODUCT_FILE_UPLOADED_TO_STORAGE, externalPath, productId);
			return new ResponseEntity<>(restFileInfo, HttpStatus.CREATED);

		} catch (Exception e) {

			String msg = logger.log(StorageMgrMessage.INTERNAL_ERROR, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.INTERNAL_SERVER_ERROR);

		} finally {

			fileLocker.unlock();
			logger.debug("... unlocked the file: ", externalPath);
		}
	}

	/**
	 * Copies the file from the storage to the cache using synchronization. During
	 * the copying to the cache, the status of the file will be "not exists", after
	 * the completion the status will be set to "ready". Sets the file permission
	 * 444.
	 * 
	 * @param storageFilePath the file path in the storage
	 * @param fileLocker      file locker is used for synchronization
	 * @return RestFileInfo
	 * @throws FileLockedAfterMaxCyclesException
	 * @throws IOException
	 * @throws Exception
	 */
	private RestFileInfo copyFileStorageToCache(String storageFilePath, StorageFileLocker fileLocker)
			throws FileLockedAfterMaxCyclesException, IOException, Exception {

		// x-to-cache-copy method, status "not exists" is used

		// relative path depends on path, not on actual storage
		String relativePath = storageProvider.getRelativePath(storageFilePath);

		StorageFile storageFile = storageProvider.getStorageFile(relativePath);
		StorageFile cacheFile = storageProvider.getCacheFile(storageFile.getRelativePath());

		FileCache cache = FileCache.getInstance();

		if (!cache.containsKey(cacheFile.getFullPath())) {

			fileLocker.lockOrWaitUntilUnlockedAndLock();

			// check again, the file could be copied to cache from another thread after lock
			if (!cache.containsKey(cacheFile.getFullPath())) {

				// active thread - copies the file to the cache storage and puts it to the cache

				cache.setCacheFileStatus(cacheFile.getFullPath(), CacheFileStatus.INCOMPLETE);

				storageProvider.getStorage().downloadFile(storageFile, cacheFile);

				logger.log(StorageMgrMessage.PRODUCT_FILE_DOWNLOADED_FROM_STORAGE, cacheFile.getFullPath());

				Files.setPosixFilePermissions(Paths.get(cacheFile.getFullPath()),
						PosixFilePermissions.fromString(READ_ONLY_FOR_ALL_USERS));

				cache.put(cacheFile.getFullPath()); // cache file status = READY

			} else {

				// passive thread - did nothing, waited for copied file and use it from cache
				logger.debug("... waiting-thread when the file downloaded and use it from cache: ",
						cacheFile.getFullPath());
			}

		} else {

			logger.debug("... no download and no lock - the file is in cache: ", cacheFile.getFullPath());
		}

		RestFileInfo restFileInfo = convertToRestFileInfo(cacheFile,
				storageProvider.getCacheFileSize(storageFile.getRelativePath()));

		return restFileInfo;
	}

	/**
	 * Copies the file from the external source to the cache using synchronization.
	 * During the copying to the cache, the status of the file will be "not exists",
	 * after the completion the status will be set to "ready". Sets the file
	 * permission 444.
	 * 
	 * @param externalPath external path of the file, which will be copied to the
	 *                     cache
	 * @param productId    product id is used as a directory to store copied file in
	 *                     cache
	 * @param fileSize     file size
	 * @param fileLocker   file locker is used for synchronization
	 * @return Rest File Info
	 * @throws FileLockedAfterMaxCyclesException
	 * @throws IOException
	 * @throws Exception
	 */
	private RestFileInfo copyFileExternalToCache(String externalPath, Long productId, Long fileSize,
			StorageFileLocker fileLocker) throws FileLockedAfterMaxCyclesException, IOException, Exception {

		// x-to-cache-copy method, status "not exists" is used

		String productFolderWithFilename = getProductFolderWithFilename(externalPath, productId);
		StorageFile cacheFile = storageProvider.getCacheFile(productFolderWithFilename);

		FileCache cache = FileCache.getInstance();

		if (!cache.containsKey(cacheFile.getFullPath())) {

			fileLocker.lockOrWaitUntilUnlockedAndLock();

			// check again, the file could be copied to cache from another thread after lock
			if (!cache.containsKey(cacheFile.getFullPath())) {

				// active thread - copies the file to the cache storage and puts it to the cache

				cache.setCacheFileStatus(cacheFile.getFullPath(), CacheFileStatus.INCOMPLETE);

				storageProvider.copyAbsoluteFilesToCache(externalPath, productId);

				logger.log(StorageMgrMessage.PRODUCT_FILE_DOWNLOADED_FROM_EXTERNAL_TO_CACHE, cacheFile.getFullPath());

				Files.setPosixFilePermissions(Paths.get(cacheFile.getFullPath()),
						PosixFilePermissions.fromString(READ_ONLY_FOR_ALL_USERS));

				cache.put(cacheFile.getFullPath()); // cache file status = READY

			} else {

				// passive thread - did nothing, waited for copied file and use it from cache
				logger.debug(
						"... waiting-thread when the file downloaded to cache from external storage and use it from cache: ",
						cacheFile.getFullPath());
			}

		} else {

			logger.debug("... no download and no lock - the file is in cache: ", cacheFile.getFullPath());
		}

		RestFileInfo restFileInfo = convertToRestFileInfo(cacheFile,
				storageProvider.getCacheFileSize(cacheFile.getRelativePath()));

		return restFileInfo;
	}

	// TODO: WIP Special use case for cache recovery - file in cache, but not in
	// storage
	// TODO: WIP Special use case for cache state - not uploaded to storage

	/**
	 * Copies the file from the cache to the storage using synchronization. During
	 * the copying to the cache, the status of the file will be "not exists", after
	 * the completion the status will be set to "ready"
	 * 
	 * @param relativeCachePath relative cache path
	 * @param fileLocker        is used for synchronization
	 * @return RestFileInfo
	 * @throws FileLockedAfterMaxCyclesException
	 * @throws IOException
	 * @throws Exception
	 */
	private RestFileInfo copyFileCacheToStorage(String relativeCachePath, StorageFileLocker fileLocker)
			throws FileLockedAfterMaxCyclesException, IOException, Exception {

		Storage storage = storageProvider.getStorage();

		StorageFile cacheFile = storageProvider.getCacheFile(relativeCachePath);
		StorageFile storageFile = storageProvider.getStorageFile(relativeCachePath);

		FileCache cache = FileCache.getInstance();

		if (!cache.containsKey(cacheFile.getFullPath())) {

			fileLocker.lockOrWaitUntilUnlockedAndLock();

			// check again, the file could be copied to storage from another thread after
			// lock
			if (!cache.containsKey(cacheFile.getFullPath())) {

				// active thread - copies the file to the storage and checks it as OK (WIP)
				storage.uploadFile(cacheFile, storageFile);

				logger.log(StorageMgrMessage.PRODUCT_FILE_DOWNLOADED_FROM_EXTERNAL_TO_CACHE, storageFile.getFullPath());

				cache.put(storageFile.getFullPath());

			} else {

				// passive thread - did nothing, waited for copied file and use it from cache
				logger.debug("... waiting-thread when the file downloaded and use it from cache: ",
						storageFile.getFullPath());
			}

		} else {

			logger.debug("... no download and no lock - the file is in cache: ", storageFile.getFullPath());
		}

		RestFileInfo restFileInfo = convertToRestFileInfo(storageFile,
				storageProvider.getCacheFileSize(cacheFile.getRelativePath()));

		return restFileInfo;
	}

	/**
	 * Converts storage file to rest file info (generated model)
	 * 
	 * @param storageFile storage file
	 * @param fileSize    file size
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

	/**
	 * Gets a product folder with the file name from the given external path using
	 * product id
	 * 
	 * @param externalPath absolute external path
	 * @param productId    product id
	 * @return product folder with the file name
	 */
	private String getProductFolderWithFilename(String externalPath, Long productId) {

		String fileName = new File(externalPath).getName();
		return Paths.get(String.valueOf(productId), fileName).toString();
	}
}
