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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;

/**
 * File cache for managing files in cache storage.
 * 
 * @author Denys Chaykovskiy
 *
 */
@Component
public class FileCache {

	/** Path to file cache storage */
	private String cachePath;

	/** Prefix for accessed files */
	private static final String PREFIX = "accessed-";

	/** Cache Map for storing file pathes */
	private MapCache mapCache;

	@Autowired
	private StorageManagerConfiguration cfg;

	/** File Cache singleton */
	private static FileCache theFileCache;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(FileCache.class);

	/**
	 * Instance of file cache
	 * 
	 * @return file cache singleton
	 */
	public static FileCache getInstance() {

		return theFileCache;
	}

	/**
	 * Puts the new element to map. If element exists, it will be overwritten
	 * 
	 * @param pathKey File path as a key
	 */
	public void put(String pathKey) {

		if (logger.isTraceEnabled())
			logger.trace(">>> put({})", pathKey);

		// Ensure call is legal
		if (!new File(pathKey).exists()) {
			logger.error("> File can't be put to cache, it does not exist: " + pathKey);
			return;
		}
		if (!pathKey.startsWith(cachePath)) {
			if (logger.isTraceEnabled()) logger.trace("... not adding {} to cache, because it is considered a backend file", pathKey);
			return;
		}

		if (!mapCache.containsKey(pathKey)) {

			deleteLRU();
		}

		rewriteFileAccessed(pathKey);
		FileInfo fileInfo = new FileInfo(getFileAccessed(pathKey), getFileSize(pathKey));

		mapCache.put(pathKey, fileInfo);
	}

	/**
	 * Checks if key available in map and updates the record in file cache if
	 * available
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

		if (!file.exists() || !file.isFile()) {

			remove(pathKey);
			return false;
		}

		put(pathKey);

		return true;
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
	 * Cleanup cache by Least Recently Used strategy, if disk usage is higher than maximum usage configured
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
					
					// Once we get here, the cache may already have been cleared by a concurrent thread
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
						logger.trace("... deleteLRU.duration of sorting({} ms, {} ns, Cache size - {} records)", duration / 1000000,
								duration, size());
					
					long entryCount = 0;
					long bytesToDelete =
							(long) ((getRealUsage() - cfg.getExpectedCacheUsage()) * (new File(cachePath)).getTotalSpace() / 100.0);
					
					// We do not rely on getRealUsage() during the deletion, because the file system information may not be
					// updated as fast as we are deleting files (this has been observed in practice)
					while (0 < bytesToDelete && cacheIterator.hasNext()) {
						if (logger.isTraceEnabled()) logger.trace("... to delete: {} --> deleting next entry", bytesToDelete);
						
						Entry<String, FileInfo> entry = cacheIterator.next();
						bytesToDelete -= entry.getValue().getSize();
						
						remove(entry.getKey());
						
						++entryCount;
					}
					logger.info("Cache cleanup removed {} entries from file cache in {} ms", entryCount,
							(System.nanoTime() - startTime) / 1000000);
					
					// We have a serious problem, if we still do not have enough cache space
					if (getRealUsage() >= cfg.getMaximumCacheUsage()) {
						logger.error("Disk usage {} exceeds maximum usage {} after emptying cache", getRealUsage(),
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
	 * Clears the cache only (without deleting of files), sets the cache path and puts
	 * files in cache
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
	 * @param pathKey Path to file
	 * @return
	 */
	/* package */ FileInfo get(String pathKey) {

		return mapCache.get(pathKey);
	}

	/**
	 * Removes cache element, file and accessed file
	 * 
	 * @param pathKey Path to file
	 */
	/* package */ void remove(String pathKey) {

		if (logger.isTraceEnabled())
			logger.trace(">>> remove({}) - last accessed {}", pathKey, mapCache.get(pathKey).getAccessed());

		deleteFileAndAccessed(pathKey);

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
	/* package */ static String getPrefix() {

		return PREFIX;
	}

	/* package */ void putFilesToCache(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> putFilesToCache({})", path);

		File directory = new File(path);

		if (!directory.exists()) {

			if (!directory.mkdirs()) { // try to create

				throw new IllegalArgumentException("Wrong path for FileCache: " + path);
			}
		}
	
		File[] files = directory.listFiles();
		

		for (File file : files) {
			
			if (mapCache.containsKey(file.getPath())) {

				continue;
			}
			
			if (file.isDirectory()) {
				
				if (new FileUtils(file.getPath()).isEmptyDirectory()) {
			
					deleteEmptyDirectoriesToTop(file.getPath());	
				}
				else {
			
					putFilesToCache(file.getPath());
				}
				
				continue;
			}

			if (isPrefixFile(file.getPath()) && 
					!Files.exists(Paths.get(getPathFromAccessed(file.getPath())))) {

				file.delete();
				
				if (new FileUtils(path).isEmptyDirectory()) {
				
					deleteEmptyDirectoriesToTop(path);	
				
					return;
				}
				
				continue; 
			}
			
			else if (file.isFile() && isCacheFile(file.getPath())) {

				putNotUpdateAccessed(file.getPath());
			}
		}
	}

	/**
	 * Puts the element to map without update accessed-info. If element exists, it
	 * will be overwritten.
	 *
	 * @param pathKey File path as a key
	 */
	public void putNotUpdateAccessed(String pathKey) {

		if (logger.isTraceEnabled())
			logger.trace(">>> putNotUpdateAccessed({})", pathKey);
		
		if (!isCacheFile(pathKey)) {
			
			return; 
		}

		// Ensure call is legal
		if (!new File(pathKey).exists()) {
			logger.error("> File can't be put to cache, it does not exist: " + pathKey);
			return;
		}
		if (!pathKey.startsWith(cachePath)) {
			if (logger.isTraceEnabled()) logger.trace("... not adding {} to cache, because it is considered a backend file", pathKey);
			return;
		}

		FileInfo fileInfo = new FileInfo(getFileAccessed(pathKey), getFileSize(pathKey));

		mapCache.put(pathKey, fileInfo);
	}

	/**
	 * Deletes if exist file and logically connected accessed file from the disk
	 * 
	 * @param path full path to the file
	 */
	/* package */ void deleteFileAndAccessed(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteFileAndAccessed({})", path);

		String accessedPath = getAccessedPath(path);
		File file = new File(path);
		File accessedFile = new File(accessedPath);
		String directory = file.getParent();

		if (!file.exists()) {
			if (logger.isTraceEnabled())
				logger.warn("> File does not exist, but exists in cache({})", path);
		} else if (!file.delete()) { // delete file
			if (logger.isTraceEnabled())
				logger.warn("> File was not deleted({})", path);
		}

		if (!accessedFile.exists()) {
			if (logger.isTraceEnabled())
				logger.warn("> File was not deleted({})", accessedPath);
		} else if (!accessedFile.delete()) { // delete accessed file
			if (logger.isTraceEnabled())
				logger.warn("> Accessed File was not deleted({})", accessedPath);
		}

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
			rewriteFileAccessed(path);
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
		String accessedPath = file.getParent() + "/" + PREFIX + file.getName();

		return accessedPath;

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
		String path = file.getParent() + "/" + file.getName().replace(PREFIX, "");

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
	 * Returns true if the file is the cache file (not accessed and not hidden file)
	 * 
	 * @param path the full path to the file
	 * @return true if the file is the cache file
	 */
	private boolean isCacheFile(String path) {

		if (isPrefixFile(path)) {

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
	private boolean isPrefixFile(String path) {
		
		String fileName = new File(path).getName();

		return fileName.startsWith(PREFIX) ? true : false;
	}

	/**
	 * Returns true if the file is a hidden file
	 * 
	 * @param path the full path to the file
	 * @return true if the file is hidden
	 */
	private boolean isHiddenFile(String path) {
		
		String fileName = new File(path).getName();

		return fileName.startsWith(".") ? true : false;
	}

	/**
	 * Rewrites accessed file with the current time stamp
	 * 
	 * @param path The full path to file
	 */
	private void rewriteFileAccessed(String path) {

		if (logger.isTraceEnabled())
			logger.trace(">>> rewriteFileAccessed({})", path);

		String accessedPath = getAccessedPath(path);
		String timeStamp = Instant.now().toString();
		FileUtils fileUtils = new FileUtils(accessedPath);

		fileUtils.createFile(timeStamp);
	}

}
