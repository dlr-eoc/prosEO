package de.dlr.proseo.storagemgr.cache;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.StorageMgrMessage;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.utils.FileUtils;

/**
 * File cache for managing files in the cache storage. Features: 1) saves the
 * time stamp of the last access of the cache file; 2) contains the status of
 * the cache file, which can be changed; 3) uses the possibility to save in the
 * cache also "temporary" files (temporary file is a file, which has not yet
 * been copied to the cache. After copying it will be renamed to the cache file.
 * In case of unsuccessful copying a temporary file will be deleted)
 * 
 * Information about some public methods:
 * 
 * The method put(fullPath): - puts the file to the cache; - sets the file
 * status "READY"; - sets/updates the last accessed record; - cleans the cache
 * if there is not enough free space - deletes the file physically if it is a
 * temporary file
 * 
 * The method containsKey(fullPath): - checks if the path is in the cache. Only
 * cache paths with the status "READY" are visible - removes the cache path from
 * the cache if the physical file does not exist anymore - if the file is in the
 * cache, updates the time stamp of the last access calling put()
 * 
 * 
 * @author Denys Chaykovskiy
 *
 */
@Component
public class FileCache {

	/** Path to file cache storage */
	private String cachePath;

	/** Prefix for accessed files */
	private static final String ACCESSED_PREFIX = "accessed-";

	/** Prefix for temporary files */
	private static final String TEMPORARY_PREFIX = "temporary-";

	/** Prefix for status files */
	private static final String STATUS_PREFIX = "status-";

	/** Cache Map for storing file paths */
	private MapCache mapCache;

	@Autowired
	private StorageManagerConfiguration cfg;

	/** File Cache singleton */
	private static FileCache theFileCache;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(FileCache.class);

	/**
	 * Instance of file cache
	 * 
	 * @return file cache singleton
	 */
	public static FileCache getInstance() {

		return theFileCache;
	}

	/**
	 * Puts the new element to map. If element exists, it will be overwritten.
	 * Removes the file if it is temporary
	 * 
	 * @param pathKey the full cache file path as a key
	 */
	public void put(String pathKey) {

		if (logger.isTraceEnabled())
			logger.trace(">>> put({})", pathKey);

		// Ensure call is legal
		if (!new File(pathKey).exists()) {
			logger.log(StorageMgrMessage.CACHE_NO_FILE_FOR_PUTTING_TO_CACHE, pathKey);
			return;
		}

		// pathKey is the full path, beginning with the cache path
		if (!pathKey.startsWith(cachePath)) {
			if (logger.isTraceEnabled())
				logger.trace("... not adding {} to cache, because it is considered a backend file", pathKey);
			return;
		}

		if (isTemporaryPrefixFile(pathKey)) {

			deleteFile(pathKey);
			logger.log(StorageMgrMessage.CACHE_TEMPORARY_FILE_DELETED, pathKey);
		}

		if (!mapCache.containsKey(pathKey)) {

			deleteLRU();
		}

		rewriteStatusPrefixFile(pathKey, CacheFileStatus.READY);

		rewriteAccessedPrefixFile(pathKey);
		FileInfo fileInfo = new FileInfo(getFileAccessed(pathKey), getFileSize(pathKey));

		mapCache.put(pathKey, fileInfo);
	}

	/**
	 * Checks if the cache contains the path If not - returns false. If the physical
	 * file does not exits anymore - deletes the path from the cache. Returns true
	 * if the path is available in the cache and also updates the file record of the
	 * last access
	 * 
	 * @param pathKey File path as key
	 * @return true if pathkey is in file cache
	 */
	public boolean containsKey(String pathKey) {

		if (logger.isTraceEnabled())
			logger.trace(">>> containsKey({})", pathKey);

		if (!mapCache.containsKey(pathKey)) {

			return false;
		}

		File file = new File(pathKey);

		if (!file.isFile()) {

			remove(pathKey);
			return false;
		}

		put(pathKey);

		return true;
	}

	/**
	 * Gets temporary prefix of the file
	 * 
	 * @return temporary prefix of the file
	 */
	public static String getTemporaryPrefix() {
		return TEMPORARY_PREFIX;
	}

	/**
	 * Returns a status of the cache file
	 * 
	 * @param path The full path to the cache file
	 * @return a status of the cache file
	 */
	public CacheFileStatus getCacheFileStatus(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getCacheFileStatus({})", path);

		String status;
		FileUtils fileUtils = new FileUtils(getStatusPath(path));

		if (!hasStatus(path)) {
			rewriteStatusPrefixFile(path, CacheFileStatus.READY);
		}

		status = fileUtils.getFileContent();

		return CacheFileStatus.valueOf(status);
	}

	/**
	 * Sets the status of a cache file. The pathKey must be an existing cache file, 
	 * the exception is for "not exists" cache file status.
	 * 
	 * @param pathKey File path as key
	 * @return a status of the cache file
	 */
	public void setCacheFileStatus(String pathKey, CacheFileStatus status) {

		if (logger.isTraceEnabled())
			logger.trace(">>> setCacheFileStatus({}, {})", pathKey, status);

		// There is no cache file for the status == not exists
		if (status != CacheFileStatus.NOT_EXISTS) {
			if (!mapCache.containsKey(pathKey)) {
				if (!new File(pathKey).exists()) {
					logger.log(StorageMgrMessage.CACHE_NO_FILE_FOR_PUTTING_TO_CACHE, pathKey);
				}
				return;
			}
		}

		rewriteStatusPrefixFile(pathKey, status);
	}

	/**
	 * Initializes file cache with directory from Application.yml
	 */
	@PostConstruct
	private void init() {

		if (logger.isTraceEnabled())
			logger.trace(">>> init()");

		setPath(cfg.getPosixCachePath());
	}

	/**
	 * Cleanup cache by Least Recently Used strategy, if disk usage is higher than
	 * maximum usage configured
	 */
	private void deleteLRU() {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteLRU()");

		// Check whether cleanup is needed
		if (getRealUsage() < cfg.getMaximumCacheUsage()) {
			return;
		}

		// Run cache cleanup in background
		Thread deleteLRUTask = new Thread() {

			@Override
			public void run() {

				// Only one cleanup task at the same time allowed
				synchronized (theFileCache) {

					// Once we get here, the cache may already have been cleared by a concurrent
					// thread
					if (getRealUsage() < cfg.getMaximumCacheUsage()) {
						return;
					}

					long startTime = System.nanoTime();

					mapCache.sortByAccessedAsc();

					List<Entry<String, FileInfo>> sortedPathes = mapCache.getSortedPathes();
					Iterator<Entry<String, FileInfo>> cacheIterator = sortedPathes.iterator();

					long endTime = System.nanoTime();
					long duration = endTime - startTime;

					if (logger.isTraceEnabled())
						logger.trace("... deleteLRU.duration of sorting({} ms, {} ns, Cache size - {} records)",
								duration / 1000000, duration, size());

					long entryCount = 0;
					long bytesToDelete = (long) ((getRealUsage() - cfg.getExpectedCacheUsage())
							* (new File(cachePath)).getTotalSpace() / 100.0);

					// We do not rely on getRealUsage() during the deletion, because the file system
					// information may not be
					// updated as fast as we are deleting files (this has been observed in practice)
					while (0 < bytesToDelete && cacheIterator.hasNext()) {
						if (logger.isTraceEnabled())
							logger.trace("... to delete: {} --> deleting next entry", bytesToDelete);

						Entry<String, FileInfo> entry = cacheIterator.next();
						bytesToDelete -= entry.getValue().getSize();

						remove(entry.getKey());

						++entryCount;
					}
					logger.log(StorageMgrMessage.CACHE_CLEANUP_REPORT, entryCount,
							(System.nanoTime() - startTime) / 1000000);

					// We have a serious problem, if we still do not have enough cache space
					if (getRealUsage() >= cfg.getMaximumCacheUsage()) {
						logger.log(StorageMgrMessage.CACHE_NOT_ENOUGH_SPACE_AFTER_CLEANING, getRealUsage(),
								cfg.getMaximumCacheUsage());
					}
				}
			}

		};
		deleteLRUTask.start();
	}

	/**
	 * Calculates the real disk usage in percent 0..100
	 * 
	 * @return real disk usage in percent
	 */
	private double getRealUsage() {

		if (logger.isTraceEnabled())
			logger.trace(">>> getRealUsage()");

		File file = new File(cachePath);
		long totalBytes = file.getTotalSpace(); // total disk space in bytes
		long freeBytes = file.getUsableSpace();
		long usedBytes = totalBytes - freeBytes;

		return 100.0 * usedBytes / totalBytes;
	}

	/**
	 * Clears the cache only (without deleting of files), sets the cache path and
	 * puts files in cache
	 * 
	 * @param pathKey The Cache Path
	 */
	/* package */ void setPath(String pathKey) {

		if (logger.isTraceEnabled())
			logger.trace(">>> setPath({})", pathKey);

		theFileCache = this;
		cachePath = pathKey;

		mapCache = new MapCache();

		File directory = new File(cachePath);

		if (!directory.exists()) {

			if (!directory.mkdirs()) {

				throw new IllegalArgumentException("Cannot create directory for FileCache:" + cachePath);
			}
		}

		putFilesToCache(cachePath);
	}

	/**
	 * Gets the path key from file cache
	 * 
	 * @param pathKey Path to the cache file
	 * @return
	 */
	/* package */ FileInfo get(String pathKey) {

		return mapCache.get(pathKey);
	}

	/**
	 * Removes cache element, cache file and auxiliary files
	 * 
	 * @param pathKey Path to file
	 */
	/* package */ void remove(String pathKey) {

		if (logger.isTraceEnabled())
			logger.trace(">>> remove({}) - last accessed {}", pathKey, mapCache.get(pathKey).getAccessed());

		deleteCacheFileAndAuxPrefixFiles(pathKey);

		mapCache.remove(pathKey);
	}

	/**
	 * Clears all cache elements only (files remain on disk)
	 * 
	 */
	/* package */ void clear() {

		if (logger.isTraceEnabled())
			logger.trace(">>> clear()");

		mapCache.clear();
	}

	/**
	 * Removes all cache elements and their connected files and accessed files from
	 * disk
	 * 
	 */
	/* package */ void removeAll() {

		if (logger.isTraceEnabled())
			logger.trace(">>> removeAll()");

		Set<String> entries = mapCache.getCache().keySet();

		for (String pathKey : entries) {

			remove(pathKey);
		}
	}

	/**
	 * Gives the number of elements in cache
	 * 
	 * @return number of elements in cache
	 */
	/* package */ int size() {

		return mapCache.size();
	}

	/**
	 * Gets the map cache
	 * 
	 * @return map cache
	 */
	/* package */ Map<String, FileInfo> getMapCache() {

		return mapCache.getCache();
	}

	/**
	 * Gets the file prefix for accessed files
	 * 
	 * @return the file prefix
	 */
	/* package */ static String getAccessedPrefix() {

		return ACCESSED_PREFIX;
	}

	/**
	 * Puts files to cache, removes accessed prefix files without files, removes
	 * temporary prefix files
	 * 
	 * @param path Path to files
	 */
	/* package */ void putFilesToCache(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> putFilesToCache({})", path);

		File directory = new File(path);

		if (!directory.exists()) {

			if (!directory.mkdirs()) { // try to create
				throw new IllegalArgumentException("Cannot create directory for FileCache: " + path);
			}
		}

		File[] files = directory.listFiles();

		for (File file : files) {

			// check if already in cache
			if (mapCache.containsKey(file.getPath())) {
				continue;
			}

			// recursive processing if directory
			if (file.isDirectory()) {
				if (new FileUtils(file.getPath()).isEmptyDirectory()) {

					deleteEmptyDirectoriesToTop(file.getPath());
				} else {

					putFilesToCache(file.getPath());
				}
				continue;
			}

			// delete if temporary file
			if (isTemporaryPrefixFile(file.getPath())) {
				deleteFile(file.getPath());
			}

			// delete if accessed file alone without file
			if (isAccessedPrefixFile(file.getPath()) && !Files.exists(Paths.get(getPathFromAccessed(file.getPath())))) {

				file.delete();

				if (new FileUtils(path).isEmptyDirectory()) {

					deleteEmptyDirectoriesToTop(path);
					return;
				}
				continue;
			}

			// if cache file, adds to cache without update accessed prefix file
			else if (file.isFile() && isCacheFile(file.getPath())) {

				putWithoutUpdateAccessedPrefixFile(file.getPath());
			}
		}
	}

	/**
	 * Puts the file to map without update accessed prefix file. If element exists,
	 * it will be overwritten.
	 *
	 * @param pathKey File path as a key
	 */
	/* package */ void putWithoutUpdateAccessedPrefixFile(String pathKey) {

		if (logger.isTraceEnabled())
			logger.trace(">>> putWithoutUpdateAccessedPrefixFile({})", pathKey);

		if (!isCacheFile(pathKey)) {
			return;
		}

		// Ensure call is legal
		if (!new File(pathKey).exists()) {
			logger.log(StorageMgrMessage.CACHE_NO_FILE_FOR_PUTTING_TO_CACHE, pathKey);
			return;
		}
		if (!pathKey.startsWith(cachePath)) {
			if (logger.isTraceEnabled())
				logger.trace("... not adding {} to cache, because it is considered a backend file", pathKey);
			return;
		}

		FileInfo fileInfo = new FileInfo(getFileAccessed(pathKey), getFileSize(pathKey));

		mapCache.put(pathKey, fileInfo);
	}

	/**
	 * Deletes a cache file and logically connected auxiliary files from the disk
	 * 
	 * @param path full path to the cache file
	 */
	/* package */ void deleteCacheFileAndAuxPrefixFiles(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteFileAndAuxPrefixFiles({})", path);

		String directory = new File(path).getParent();

		deleteFile(path);

		deleteFile(getAccessedPath(path));
		deleteFile(getStatusPath(path));
		deleteFile(getTemporaryPath(path));

		deleteEmptyDirectoriesToTop(directory);
	}

	/**
	 * Deletes empty directories recursively in the direction of root
	 * 
	 * @param directoryToDelete the path to the directory
	 */
	/* package */ void deleteEmptyDirectoriesToTop(String directoryToDelete) {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteEmptyDirectoriesToTop({})", directoryToDelete);

		if (null == directoryToDelete || directoryToDelete.equals(cachePath)) {
			return;
		}

		File directory = new File(directoryToDelete);

		if (!directory.isDirectory()) {
			return;
		}

		File[] allFiles = directory.listFiles();
		String parent = directory.getParent();

		if (allFiles.length == 0) {

			directory.delete();

			deleteEmptyDirectoriesToTop(parent);
		}
	}

	/**
	 * Returns the file size in bytes
	 * 
	 * @param path The full path to file
	 * @return the file size
	 */
	/* package */ Long getFileSize(String path) {

		return new FileUtils(path).getFileSize();
	}

	/**
	 * Returns the last accessed time stamp of the file
	 * 
	 * @param path The full path to the file
	 * @return time stamp of last accessed
	 */
	/* package */ Instant getFileAccessed(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFileAccessed({})", path);

		String lastAccessed;
		FileUtils fileUtils = new FileUtils(getAccessedPath(path));

		if (!wasAccessed(path)) {
			rewriteAccessedPrefixFile(path);
		}

		lastAccessed = fileUtils.getFileContent();

		return Instant.parse(lastAccessed);
	}

	/**
	 * Gets the accessed path of the file
	 * 
	 * @param path The full path to the file
	 * @return accessed path
	 */
	/* package */ String getAccessedPath(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getAccessedPath({})", path);

		File file = new File(path);
		String accessedPath = file.getParent() + "/" + ACCESSED_PREFIX + file.getName();

		return accessedPath;
	}

	/**
	 * Gets the path of the status file
	 * 
	 * @param path The full path to the cache file
	 * @return the path to the status file of the cache file
	 */
	/* package */ String getStatusPath(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getStatusPath({})", path);

		File file = new File(path);
		String statusPath = file.getParent() + "/" + STATUS_PREFIX + file.getName();

		return statusPath;
	}

	/**
	 * Gets the path of the temporary file
	 * 
	 * @param path The full path to the cache file
	 * @return the path to the temporary file of the cache file
	 */
	/* package */ String getTemporaryPath(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getTemporaryPath({})", path);

		File file = new File(path);
		String temporaryPath = file.getParent() + "/" + TEMPORARY_PREFIX + file.getName();

		return temporaryPath;
	}

	/**
	 * Gets the path to file from accessed path
	 * 
	 * @param accessed Path accessed Path to the file
	 * @return the full path to file
	 */
	/* package */ String getPathFromAccessed(String accessedPath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getPathFromAccessed({})", accessedPath);

		File file = new File(accessedPath);
		String path = file.getParent() + "/" + file.getName().replace(ACCESSED_PREFIX, "");

		return path;
	}

	/**
	 * Returns true if the file was accessed
	 * 
	 * @param path the full path to the file
	 * @return true if the file was accessed
	 */
	private boolean wasAccessed(String path) {

		File f = new File(getAccessedPath(path));

		return f.isFile();
	}

	/**
	 * Returns true if the cache file has a status
	 * 
	 * @param path the full path to the cache file
	 * @return true if the file was accessed
	 */
	private boolean hasStatus(String path) {

		File f = new File(getStatusPath(path));

		return f.isFile();
	}

	/**
	 * Returns true if the file is the cache file (starts with the cache path, not
	 * accessed, not temporary, not hidden and not status file)
	 * 
	 * @param path the full path to the file
	 * @return true if the file is the cache file
	 */
	private boolean isCacheFile(String path) {

		if (!path.startsWith(cachePath)) {
			return false;
		}

		if (isAccessedPrefixFile(path)) {
			return false;
		}

		if (isTemporaryPrefixFile(path)) {
			return false;
		}

		if (isStatusPrefixFile(path)) {
			return false;
		}

		if (isHiddenFile(path)) {
			return false;
		}

		return true;
	}

	/**
	 * Returns true if the file has the access prefix
	 * 
	 * @param path the full path to the file
	 * @return true if the file has the prefix
	 */
	private boolean isAccessedPrefixFile(String path) {

		return hasFilePrefix(path, ACCESSED_PREFIX);
	}

	/**
	 * Returns true if the file has the temporary prefix
	 * 
	 * @param path the full path to the file
	 * @return true if the file has the prefix
	 */
	private boolean isTemporaryPrefixFile(String path) {

		return hasFilePrefix(path, TEMPORARY_PREFIX);
	}

	/**
	 * Returns true if the file has the status prefix
	 * 
	 * @param path the full path to the cache file
	 * @return true if the cache file path has the status prefix
	 */
	private boolean isStatusPrefixFile(String path) {

		return hasFilePrefix(path, STATUS_PREFIX);
	}

	/**
	 * Returns true if the file is a hidden file
	 * 
	 * @param path the full path to the file
	 * @return true if the file is hidden
	 */
	private boolean isHiddenFile(String path) {

		return hasFilePrefix(path, ".");
	}

	/**
	 * Returns true if the file has the "x" prefix
	 * 
	 * @param path the full path to the file
	 * @return true if the file has the prefix
	 */
	private boolean hasFilePrefix(String path, String prefix) {

		String fileName = new File(path).getName();

		return fileName.startsWith(prefix) ? true : false;
	}

	/**
	 * Rewrites accessed prefix file with the current time stamp
	 * 
	 * @param path The full path to file
	 */
	private void rewriteAccessedPrefixFile(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> rewriteFileAccessed({})", path);

		String accessedPath = getAccessedPath(path);
		String timeStamp = Instant.now().toString();
		FileUtils fileUtils = new FileUtils(accessedPath);

		fileUtils.synchroCreateFile(timeStamp, cfg.getFileCheckWaitTime(), cfg.getFileCheckMaxCycles());
	}

	/**
	 * Rewrites status prefix file with the current status
	 * 
	 * @param path   The full path to the cache file
	 * @param status The
	 */
	private void rewriteStatusPrefixFile(String path, CacheFileStatus status) {

		if (logger.isTraceEnabled())
			logger.trace(">>> rewriteStatusPrefixFile({}, {})", path, status);

		String statusPath = getStatusPath(path);
		FileUtils fileUtils = new FileUtils(statusPath);

		fileUtils.synchroCreateFile(status.toString(), cfg.getFileCheckWaitTime(), cfg.getFileCheckMaxCycles());
	}

	/**
	 * Deletes file
	 * 
	 * @param path The full path to file
	 */
	private void deleteFile(String path) {

		File file = new File(path);

		if (file.exists()) {
			if (!file.delete()) { // delete file
				if (logger.isTraceEnabled())
					logger.log(StorageMgrMessage.CACHE_FILE_NOT_DELETED, path);
			}
		}
	}
}