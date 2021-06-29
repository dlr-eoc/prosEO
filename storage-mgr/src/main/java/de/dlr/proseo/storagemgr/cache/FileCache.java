package de.dlr.proseo.storagemgr.cache;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * @author Denys Chaykovskiy
 *
 */
public class FileCache {

	private String path;
	private static final String PREFIX = "accessed-";

	private MapCache mapCache;

	/**
	 * @return
	 */
	public Map<String, FileInfo> getMapCache() {

		return mapCache.getCache();
	}

	/**
	 * @return
	 */
	public static String getPrefix() {

		return PREFIX;
	}

	/**
	 * @param pathKey
	 * @param fileInfo
	 */
	public void put(String pathKey) {

		FileInfo fileInfo = new FileInfo(getFileAccessed(pathKey), getFileSize(pathKey));

		mapCache.put(pathKey, fileInfo);
	}

	/**
	 * @param pathKey
	 * @return
	 */
	public FileInfo get(String pathKey) {

		return mapCache.get(pathKey);
	}

	/**
	 * Removes cache element, file and accessed file
	 * 
	 * @param pathKey
	 */
	public void remove(String pathKey) {

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
	public void clear() {

		Set<String> entries = mapCache.getCache().keySet();

		for (String pathKey : entries) {

			remove(pathKey);
		}
	}

	/**
	 * @param pathKey
	 * @return
	 */
	public boolean containsKey(String pathKey) {
		
		return mapCache.containsKey(pathKey);
	}

	/**
	 * @param path
	 */
	public FileCache(String path) {
		
		File directory = new File(path);

		this.path = path;

		mapCache = new MapCache();
		
		if (!directory.exists()) {
			
			if (!directory.mkdirs()) {
				
				throw new IllegalArgumentException("Cannot create directory for FileCache:" + path);
			}
		}
		
		putFilesToCache(path);
	}

	/**
	 * @return the path
	 */
	public String getPath() {

		return path;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {

		this.path = path;
	}

	/**
	 * @param path
	 */
	public void putFilesToCache(String path) {

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
			}
			else if (file.isFile()) {
				
				if (!containsKey(file.getPath())) {
					
					put(file.getPath());
				}
			} 
		}
	}

	/**
	 * @param path
	 * @param fileName
	 * @return
	 */
	public boolean wasAccessed(String path) {
		
		File f = new File(getAccessedPath(path));

		return f.isFile();
	}
	
	/**
	 * @param fileName
	 * @return
	 */
	public boolean isCacheFile(String fileName) {
		
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
	public boolean isPrefixFile(String fileName) {

		return fileName.startsWith(PREFIX) ? true : false;
	}

	/**
	 * @param fileName
	 * @return
	 */
	public boolean isHiddenFile(String fileName) {

		return fileName.startsWith(".") ? true : false;
	}

	/**
	 * @param path
	 * @param fileName
	 */
	public void rewriteFileAccessed(String path) {

		String accessedPath = getAccessedPath(path);
		String timeStamp = Instant.now().toString();
		FileUtils fileUtils = new FileUtils(accessedPath);

		fileUtils.createFile(timeStamp);
	}

	/**
	 * @param path
	 * @param fileName
	 * @throws IOException
	 */
	public void deleteFileAndAccessed(String path) throws IOException {

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
	public void deleteEmptyDirectoriesToTop(String directoryToDelete) {

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
	public Long getFileSize(String path) {
		
		return new FileUtils(path).getFileSize();
	}

	/**
	 * @param path
	 * @param fileName
	 * @return
	 */
	public Instant getFileAccessed(String path) {

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
	public String getAccessedPath(String path) {

		File file = new File(path);
		String accessedPath = file.getParent() + "/" + PREFIX + file.getName();

		return accessedPath;

	}
}
