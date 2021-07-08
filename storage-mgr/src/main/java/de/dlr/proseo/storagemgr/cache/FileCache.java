package de.dlr.proseo.storagemgr.cache;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;

/**
 * @author Denys Chaykovskiy
 *
 */
@Component
public class FileCache {
	
	private String path;
	private static final String PREFIX = "accessed-";
	
	private MapCache mapCache;
	
	@Autowired
	private StorageManagerConfiguration cfg;

	
	private static FileCache theFileCache; 
	
	public static FileCache getInstance() {
		
		return theFileCache;
	}
	
	@PostConstruct
	private void init() {
		
		path = cfg.getPosixWorkerMountPoint();
		
		File directory = new File(path);

		mapCache = new MapCache();

		if (!directory.exists()) {

			if (!directory.mkdirs()) {

				throw new IllegalArgumentException("Cannot create directory for FileCache:" + path);
			}
		}

		putFilesToCache(path);
		
		theFileCache = this; 
	}
	

	/**
	 * Puts the new element to map. If element exists, it will be overwritten
	 * 
	 * @param pathKey
	 * @param fileInfo
	 */
	public void put(String pathKey) {

		FileInfo fileInfo;
		
		if (!mapCache.containsKey(pathKey)) {
			
			deleteLRU(pathKey);
		}
		
		rewriteFileAccessed(pathKey);
		fileInfo = new FileInfo(getFileAccessed(pathKey), getFileSize(pathKey));

		mapCache.put(pathKey, fileInfo);
	}
	
	/**
	 * Checks if key available in map and updates last access if available
	 * 
	 * @param pathKey
	 * @return
	 */
	public boolean containsKey(String pathKey) {

		boolean contains = mapCache.containsKey(pathKey);
		
		if (contains) {
			
			put(pathKey);	
		}
		
		return contains; 
	}
	
	/**
	 * @param newPath
	 */
	private void deleteLRU(String newPath) {
		
		int expectedUsage = Integer.valueOf(cfg.getExpectedCacheUsage());
    	double realUsage = getRealUsage();
    	
    	if (realUsage <= expectedUsage) {
    		return;
    	}
    	
    	mapCache.sortByAccessedAsc();
    	
    	List<Entry<String, FileInfo>> sortedPathes = mapCache.getSortedPathes();
    	Iterator<Entry<String, FileInfo>> cacheIterator = sortedPathes.iterator();
    	Entry<String, FileInfo> pathToDelete;
    	
    	while (realUsage > expectedUsage && cacheIterator.hasNext()) {
    		
    		pathToDelete = cacheIterator.next();
    		
    		remove(pathToDelete.getKey()); 
    		
        	realUsage = getRealUsage();
    	}
	}
	
	/**
	 * @return
	 */
	private double getRealUsage() {
		
		File file = new File(path); 
    	long totalBytes = file.getTotalSpace(); //total disk space in bytes
    	long freeBytes = file.getUsableSpace();
    	long usedBytes= totalBytes - freeBytes; 
    	
    	return 100.0 * usedBytes / totalBytes; 
	}

	/**
	 * @param pathKey
	 * @return
	 */
	/* package */ FileInfo get(String pathKey) {

		return mapCache.get(pathKey);
	}
	
	/**
	 * Removes cache element, file and accessed file
	 * 
	 * @param pathKey
	 */
	/* package */ void remove(String pathKey) {

		try {
			deleteFileAndAccessed(pathKey);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mapCache.remove(pathKey);
	}

	/**
	 * 
	 */
	/* package */ void clear() {

		Set<String> entries = mapCache.getCache().keySet();

		for (String pathKey : entries) {

			remove(pathKey);
		}
	}
	
	/**
	 * @return
	 */
	/* package */ int size() {
		
		return mapCache.size();
	}
	
	/**
	 * @return
	 */
	/* package */ Map<String, FileInfo> getMapCache() {

		return mapCache.getCache();
	}

	/**
	 * @return
	 */
	/* package */ static String getPrefix() {

		return PREFIX;
	}



	/**
	 * @param path
	 */
	/* package */ void putFilesToCache(String path) {

		File directory = new File(path);

		if (!directory.exists()) {

			throw new IllegalArgumentException("Wrong path: " + path);
		}

		File[] files = directory.listFiles();

		for (File file : files) {

			if (!isCacheFile(file.getName())) {

				continue;
			}

			if (file.isDirectory()) {

				putFilesToCache(file.getPath());
			} else if (file.isFile()) {

				if (!containsKey(file.getPath())) {

					put(file.getPath());
				}
			}
		}
	}

	/**
	 * @param path
	 * @param fileName
	 * @throws IOException
	 */
	/* package */ void deleteFileAndAccessed(String path) throws IOException {

		File file = new File(path);
		File accessedFile = new File(getAccessedPath(path));
		String directory = file.getParent();

		if (!file.delete()) {
			throw new IOException("File not deleted: " + path);
		}

		if (!accessedFile.delete()) {
			throw new IOException("Accessed File not deleted: " + path);
		}

		deleteEmptyDirectoriesToTop(directory);
	}

	/**
	 * @param directoryToDelete
	 */
	/* package */ void deleteEmptyDirectoriesToTop(String directoryToDelete) {

		if (null == directoryToDelete || directoryToDelete.equals(path)) {
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
	 * @param path
	 * @return
	 */
	/* package */ Long getFileSize(String path) {

		return new FileUtils(path).getFileSize();
	}

	/**
	 * @param path
	 * @param fileName
	 * @return
	 */
	/* package */ Instant getFileAccessed(String path) {

		String lastAccessed;
		FileUtils fileUtils = new FileUtils(getAccessedPath(path));

		if (!wasAccessed(path)) {
			rewriteFileAccessed(path);
		}

		lastAccessed = fileUtils.getFileContent();

		return Instant.parse(lastAccessed);
	}

	/**
	 * @param path
	 * @return
	 */
	/* package */ String getAccessedPath(String path) {

		File file = new File(path);
		String accessedPath = file.getParent() + "/" + PREFIX + file.getName();

		return accessedPath;

	}

	/**
	 * @param path
	 * @param fileName
	 * @return
	 */
	private boolean wasAccessed(String path) {

		File f = new File(getAccessedPath(path));

		return f.isFile();
	}

	/**
	 * @param fileName
	 * @return
	 */
	private boolean isCacheFile(String fileName) {

		if (isPrefixFile(fileName)) {

			return false;
		}

		if (isHiddenFile(fileName)) {

			return false;
		}

		return true;
	}

	/**
	 * @param fileName
	 * @return
	 */
	private boolean isPrefixFile(String fileName) {

		return fileName.startsWith(PREFIX) ? true : false;
	}

	/**
	 * @param fileName
	 * @return
	 */
	private boolean isHiddenFile(String fileName) {

		return fileName.startsWith(".") ? true : false;
	}

	/**
	 * @param path
	 * @param fileName
	 */
	private void rewriteFileAccessed(String path) {

		String accessedPath = getAccessedPath(path);
		String timeStamp = Instant.now().toString();
		FileUtils fileUtils = new FileUtils(accessedPath);

		fileUtils.createFile(timeStamp);
	}

}
