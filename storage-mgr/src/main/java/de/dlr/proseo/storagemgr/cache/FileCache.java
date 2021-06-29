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

		FileInfo fileInfo = new FileInfo(getLastAccessed(pathKey), getFileSize(pathKey));

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

		mapCache.clear();
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

		this.path = path;

		mapCache = new MapCache();

		createAccessedForNotAccessed(path);
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
	public void createAccessedForNotAccessed(String path) {

		File directory = new File(path);

		if (!directory.exists()) {

			throw new IllegalArgumentException("Wrong path: " + path);
		}

		File[] files = directory.listFiles();

		for (File file : files) {

			if (isPrefixFile(file.getName()) || isHiddenFile(file.getName())) {
				continue;
			}

			if (file.isFile()) {

				if (!hasAccessed(file.getAbsolutePath())) {

					rewriteFileAccessed(file.getAbsolutePath());
				}
			}

			else if (file.isDirectory()) {

				createAccessedForNotAccessed(file.getPath());
			}
		}
	}

	/**
	 * @param path
	 * @param fileName
	 * @return
	 */
	public boolean hasAccessed(String path) {
		File f = new File(getAccessedPath(path));

		return f.isFile();
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

		System.out.println("Created/Updated Access File: " + accessedPath);

		FileUtils.createFile(accessedPath, timeStamp);

		put(path);
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

		System.out.println("File deleted successfully:" + path);

		if (!accessedFile.delete()) {
			throw new IOException("Accessed File not deleted: " + path);
		}

		System.out.println("Accessed File deleted successfully" + path);

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

		return FileUtils.getFileSize(path);
	}

	/**
	 * @param path
	 * @param fileName
	 * @return
	 */
	public Instant getLastAccessed(String path) {

		String lastAccessed;

		if (!hasAccessed(path)) {
			rewriteFileAccessed(path);
		}

		lastAccessed = FileUtils.getFileContent(getAccessedPath(path));

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
