package de.dlr.proseo.storagemgr.rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ApiMonitorMessage;
import de.dlr.proseo.logging.messages.StorageMgrMessage;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.StorageProvider;
import de.dlr.proseo.storagemgr.Exceptions.FileLockedAfterMaxCyclesException;
import de.dlr.proseo.storagemgr.cache.CacheFileStatus;
import de.dlr.proseo.storagemgr.cache.FileCache;
import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;
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
	private static final String READ_ONLY_FOR_ALL_USERS = "r--r--r--";

	/** Semaphore to limit number of parallel download requests to archive */
	private static Semaphore downloadSemaphore = null;

	/** Semaphore to limit number of parallel upload requests to archive */
	private static Semaphore uploadSemaphore = null;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductfileControllerImpl.class);
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

		// 1. copy Storage -> Cache (synchronized)
		// 2. add to cache cache.put(cache file)
		// Info: pathInfo is absolute path s3://bucket/.. or /storagePath/..

		String absoluteStoragePath = pathInfo;

		try {

			RestFileInfo restFileInfo = copyStorageFileToCache(absoluteStoragePath); // synchronized
			return new ResponseEntity<>(restFileInfo, HttpStatus.OK);

		} catch (FileLockedAfterMaxCyclesException e) {

			String time = String.valueOf(cfg.getFileCheckMaxCycles() * cfg.getFileCheckWaitTime() / 1000);
			String msg = logger.log(StorageMgrMessage.READ_TIME_OUT, absoluteStoragePath, time,
					e.getLocalizedMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.SERVICE_UNAVAILABLE);

		} catch (IOException e) {

			String msg = logger.log(StorageMgrMessage.PRODUCT_FILE_CANNOT_BE_DOWNLOADED, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.INTERNAL_SERVER_ERROR);

		} catch (Exception e) {

			String msg = logger.log(StorageMgrMessage.INTERNAL_ERROR, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Copy local file named pathInfo to storage manager. The target file path is:
	 * default mount point + productId + relative source file path
	 * 
	 * @param pathInfo  Source file name
	 * @param productId Product id
	 * @param fileSize  File Size
	 * @return RestFileInfo Rest File Info
	 * @throws Exception Internal Server Error
	 */
	@Override
	public ResponseEntity<RestFileInfo> updateProductfiles(String pathInfo, Long productId, Long fileSize) {

		if (logger.isTraceEnabled())
			logger.trace(">>> updateProductfiles({}, {}, {})", pathInfo, productId, fileSize);

		// copies absolute external file -> cache file -> storage file
		// 1. copy external -> cache (synchronized)
		// 2. add to cache cache.put(cache file)
		// 3. copy cache -> storage
		// pathInfo is absolute external path

		if (pathInfo == null) {
			return new ResponseEntity<RestFileInfo>(new RestFileInfo(), HttpStatus.BAD_REQUEST);
		}

		String externalPath = pathInfo;

		String targetPath = getProductFolderWithFilename(externalPath, productId);

		try {

			RestFileInfo restFileInfo = copyExternalFileToCache(externalPath, targetPath, productId); // synchronized

			restFileInfo = copyCacheFileToStorage(targetPath);

			logger.log(StorageMgrMessage.PRODUCT_FILE_UPLOADED_TO_STORAGE, externalPath, productId);
			return new ResponseEntity<>(restFileInfo, HttpStatus.CREATED);

		} catch (Exception e) {

			String msg = logger.log(StorageMgrMessage.INTERNAL_ERROR, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Checks if the file is in the cache. If yes (in the cache), returns the file
	 * from the cache, no copy needed. If no, copies the file from the storage to
	 * the cache using synchronization. During the copying to the cache, the status
	 * of the file will be "not exists", after the completion the status will be set
	 * to "ready". Sets the file permission 444.
	 * 
	 * @param absoluteStorageFilePath the file path in the storage
	 * @return RestFileInfo
	 * @throws FileLockedAfterMaxCyclesException
	 * @throws IOException
	 * @throws Exception
	 */
	private RestFileInfo copyStorageFileToCache(String absoluteStorageFilePath)
			throws FileLockedAfterMaxCyclesException, IOException, Exception {

		if (logger.isTraceEnabled())
			logger.trace(">>> copyFileStorageToCache({})", absoluteStorageFilePath);

		// relative path depends on path, not on actual storage
		Storage storage = storageProvider.getStorage(absoluteStorageFilePath);
		String relativePath = storage.getRelativePath(absoluteStorageFilePath);

		StorageFile storageFile = storageProvider.getStorageFile(storage, relativePath);
		StorageFile cacheFile = storageProvider.getCacheFile(storageFile.getRelativePath());

		FileCache cache = FileCache.getInstance();

		if (!cache.containsKey(cacheFile.getFullPath())) {

			synchroCopyStorageFileToCache(storageFile, cacheFile); // synchronized

		} else {

			logger.debug("... no download and no lock - the file is in cache: {}", cacheFile.getFullPath());
		}

		RestFileInfo restFileInfo = convertToRestFileInfo(cacheFile,
				storageProvider.getCacheFileSize(storageFile.getRelativePath()));

		return restFileInfo;
	}

	/**
	 * Copies the file from the storage to the cache using synchronization. During
	 * the copying to the cache, the status of the file will be "not exists", after
	 * the completion the status will be set to "ready". Sets the file permission
	 * 444.
	 * 
	 * @param srcStorageFile Source Storage File
	 * @param destCacheFile  Destination Cache File
	 * @throws FileLockedAfterMaxCyclesException
	 * @throws IOException
	 * @throws Exception
	 */
	private void synchroCopyStorageFileToCache(StorageFile srcStorageFile, StorageFile destCacheFile)
			throws FileLockedAfterMaxCyclesException, IOException, Exception {

		if (logger.isTraceEnabled())
			logger.trace(">>> synchroCopyFileStorageToCache({}, {})", srcStorageFile, destCacheFile);

		// synchronized x-to-cache-copy method, status "not exists" is used

		StorageFileLocker fileLocker = new StorageFileLocker(destCacheFile.getFullPath(), cfg.getFileCheckWaitTime(),
				cfg.getFileCheckMaxCycles());

		FileCache cache = FileCache.getInstance();

		fileLocker.lockOrWaitUntilUnlockedAndLock();

		// check again, the file could be copied to cache from another thread after lock
		if (!cache.containsKey(destCacheFile.getFullPath())) {

			try {
				
				// Restrict number of parallel downloads
				if (null == downloadSemaphore) {
					downloadSemaphore = new Semaphore(cfg.getMaxDownloadThreads(), true);
					if (logger.isDebugEnabled())
						logger.debug("... file download semaphore {} created", downloadSemaphore);
				}
				
				downloadSemaphore.acquire();
				if (logger.isDebugEnabled())
					logger.debug("... file download semaphore {} acquired, {} permits remaining",
							downloadSemaphore, downloadSemaphore.availablePermits());

				// active thread - copies the file to the cache storage and puts it to the cache
				logger.debug("... active-thread: copies the file to the cache storage and puts it to the cache: {}",
						destCacheFile.getFullPath());

				cache.setCacheFileStatus(destCacheFile.getFullPath(), CacheFileStatus.INCOMPLETE);

				if (StorageType.POSIX.equals(srcStorageFile.getStorageType())) {
					storageProvider.getStorage().downloadFile(srcStorageFile, destCacheFile);
				} else {
					storageProvider
						.getStorage(srcStorageFile.getStorageType(), srcStorageFile.getBucket())
						.downloadFile(srcStorageFile, destCacheFile);
				}

				logger.log(StorageMgrMessage.PRODUCT_FILE_DOWNLOADED_FROM_STORAGE, destCacheFile.getFullPath());

				Files.setPosixFilePermissions(Paths.get(destCacheFile.getFullPath()),
						PosixFilePermissions.fromString(READ_ONLY_FOR_ALL_USERS));

				cache.put(destCacheFile.getFullPath()); // cache file status = READY

			} catch (InterruptedException e) {
				throw new IOException(logger.log(ApiMonitorMessage.ABORTING_TASK, e.toString()));
				
			} catch (Exception e) {
				throw e;

			} finally {

				// Release parallel thread
				downloadSemaphore.release();
				if (logger.isDebugEnabled())
					logger.debug("... file download semaphore {} released, {} permits now available",
							downloadSemaphore, downloadSemaphore.availablePermits());
				
				fileLocker.unlock();
			}

		} else {

			// passive thread - did nothing, waited for copied file and use it from cache.
			// In case of FileLockedAfterMaxCyclesException from this waiting thread the
			// file is locked not by this thread and thats's why will be not unlocked.

			logger.debug(
					"... waiting-thread: waited until the file was downloaded to cache from external storage and use it from cache: {}",
					destCacheFile.getFullPath());

			fileLocker.unlock();
		}
	}

	/**
	 * Checks if the file is in the cache. If yes (in the cache), returns the file
	 * from the cache, no copy needed. If no, copies the file from the external
	 * source to the cache using synchronization. During the copying to the cache,
	 * the status of the file will be "not exists", after the completion the status
	 * will be set to "ready". Sets the file permission 444.
	 * 
	 * @param srcExternalPath external absolute path of the file, which will be copied to the
	 *                     cache
	 * @param targetPath relative path to the target location of the file
	 * @param productId    product id is used as a directory to store copied file in
	 *                     cache
	 * @return Rest File Info
	 * @throws FileLockedAfterMaxCyclesException
	 * @throws IOException
	 * @throws Exception
	 */
	private RestFileInfo copyExternalFileToCache(String srcExternalPath, String targetPath, Long productId)
			throws FileLockedAfterMaxCyclesException, IOException, Exception {

		if (logger.isTraceEnabled())
			logger.trace(">>> copyFileExternalToCache({}, {}, {}, {})", srcExternalPath, productId);

		StorageFile destCacheFile = storageProvider.getCacheFile(targetPath);

		FileCache cache = FileCache.getInstance();

		if (!cache.containsKey(destCacheFile.getFullPath())) {

			synchroCopyExternalFileToCache(srcExternalPath, productId, destCacheFile); // synchronized

		} else {

			logger.debug("... no download and no lock - the file is in cache: {}", destCacheFile.getFullPath());
		}

		RestFileInfo restFileInfo = convertToRestFileInfo(destCacheFile,
				storageProvider.getCacheFileSize(destCacheFile.getRelativePath()));

		return restFileInfo;
	}

	/**
	 * Copies the file from the external source to the cache using synchronization.
	 * During the copying to the cache, the status of the file will be "not exists",
	 * after the completion the status will be set to "ready". Sets the file
	 * permission 444.
	 * 
	 * @param externalPath  external path of the file, which will be copied to the
	 *                      cache
	 * @param productId     product id is used as a directory to store copied file
	 *                      in cache
	 * @param destCacheFile Information about destination cache file
	 * @throws FileLockedAfterMaxCyclesException
	 * @throws IOException
	 * @throws Exception
	 */
	private void synchroCopyExternalFileToCache(String srcExternalPath, Long productId, StorageFile destCacheFile)
			throws FileLockedAfterMaxCyclesException, IOException, Exception {

		if (logger.isTraceEnabled())
			logger.trace(">>> synchroCopyFileExternalToCache({}, {}, {})", srcExternalPath, productId, 
					(null == destCacheFile ? "null" : destCacheFile.getFullPath()));

		// synchronized x-to-cache-copy method, status "not exists" is used

		FileCache cache = FileCache.getInstance();

		StorageFileLocker fileLocker = new StorageFileLocker(destCacheFile.getFullPath(), cfg.getFileCheckWaitTime(),
				cfg.getFileCheckMaxCycles());

		fileLocker.lockOrWaitUntilUnlockedAndLock();

		// check again, the file could be copied to cache from another thread after lock
		if (!cache.containsKey(destCacheFile.getFullPath())) {

			try {

				// active thread - copies the file to the cache storage and puts it to the cache
				logger.debug("... active-thread: copies the file to the cache storage and puts it to the cache: {}",
						destCacheFile.getFullPath());

				cache.setCacheFileStatus(destCacheFile.getFullPath(), CacheFileStatus.INCOMPLETE);

				storageProvider.copyAbsoluteFilesToCache(srcExternalPath, destCacheFile);

				logger.log(StorageMgrMessage.PRODUCT_FILE_DOWNLOADED_FROM_EXTERNAL_TO_CACHE,
						destCacheFile.getFullPath());

				Files.setPosixFilePermissions(Paths.get(destCacheFile.getFullPath()),
						PosixFilePermissions.fromString(READ_ONLY_FOR_ALL_USERS));

				cache.put(destCacheFile.getFullPath()); // cache file status = READY

			} catch (Exception e) {

				throw e;

			} finally {

				fileLocker.unlock();
			}

		} else {

			// passive thread - did nothing, waited for copied file and use it from cache.
			// In case of FileLockedAfterMaxCyclesException from this waiting thread the
			// file is locked not by this thread and thats's why will be not unlocked.

			logger.debug(
					"... waiting-thread: waited until the file was downloaded to cache from external storage and use it from cache: {}",
					destCacheFile.getFullPath());

			fileLocker.unlock();
		}
	}

	/**
	 * Copies the file from the cache to the backend storage.
	 * 
	 * @param relativeCachePath relative cache path
	 * @return RestFileInfo
	 * @throws IOException
	 * @throws Exception
	 */
	private RestFileInfo copyCacheFileToStorage(String relativeCachePath)
			throws FileLockedAfterMaxCyclesException, IOException, Exception {

		if (logger.isTraceEnabled())
			logger.trace(">>> copyFileCacheToStorage({})", relativeCachePath);

		Storage storage = storageProvider.getStorage();

		StorageFile cacheFile = storageProvider.getCacheFile(relativeCachePath);
		StorageFile storageFile = storageProvider.getStorageFile(storage, relativeCachePath);
		
		try {
			// Restrict number of parallel downloads
			if (null == uploadSemaphore) {
				uploadSemaphore = new Semaphore(cfg.getMaxDownloadThreads(), true);
				if (logger.isDebugEnabled())
					logger.debug("... file upload semaphore {} created", uploadSemaphore);
			}
			
			uploadSemaphore.acquire();
			if (logger.isDebugEnabled())
				logger.debug("... file upload semaphore {} acquired, {} permits remaining",
						uploadSemaphore, uploadSemaphore.availablePermits());

			// Upload file
			storage.uploadFile(cacheFile, storageFile);
		
		} catch (InterruptedException e) {
			throw new IOException(logger.log(ApiMonitorMessage.ABORTING_TASK, e.toString()));
		} finally {
			// Release parallel thread
			uploadSemaphore.release();
			if (logger.isDebugEnabled())
				logger.debug("... file upload semaphore {} released, {} permits now available",
						uploadSemaphore, uploadSemaphore.availablePermits());
			
		}

		logger.log(StorageMgrMessage.PRODUCT_FILE_UPLOADED_FROM_CACHE_TO_STORAGE, storageFile.getFullPath());

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

		if (logger.isTraceEnabled())
			logger.trace(">>> convertToRestFileInfo({}, {})", storageFile, fileSize);

		RestFileInfo restFileInfo = new RestFileInfo();

		restFileInfo.setStorageType(storageFile.getStorageType().toString());
		restFileInfo.setFilePath(storageFile.getFullPath());
		restFileInfo.setFileName(storageFile.getFileName());
		restFileInfo.setFileSize(fileSize);

		return restFileInfo;
	}

	/**
	 * Gets a product folder with the file name from the given external path using
	 * product id and current timestamp (to avoid huge directories and make the backend storage navigable)
	 * 
	 * @param externalPath absolute external path
	 * @param productId    product id
	 * @return product folder with the file name
	 */
	private String getProductFolderWithFilename(String externalPath, Long productId) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getProductFolderWithFilename({}, {})", externalPath, productId);

		String fileName = new File(externalPath).getName();
		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
		return Paths.get(
				String.valueOf(now.get(ChronoField.YEAR)),
				String.valueOf(now.get(ChronoField.MONTH_OF_YEAR)),
				String.valueOf(now.get(ChronoField.DAY_OF_MONTH)),
				String.valueOf(now.get(ChronoField.HOUR_OF_DAY)),
				String.valueOf(productId),
				fileName).toString();
	}
}
