package de.dlr.proseo.storagemgr.version2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.cache.FileCache;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;
import de.dlr.proseo.storagemgr.version2.posix.PosixStorage;
import de.dlr.proseo.storagemgr.version2.posix.PosixStorageFile;
import de.dlr.proseo.storagemgr.version2.s3.S3Storage;
import de.dlr.proseo.storagemgr.version2.s3.S3StorageFile;

/**
 * Storage Provider for different storages
 * 
 * @author Denys Chaykovskiy
 *
 */
@Component
public class StorageProvider {

	/** StorageProvider singleton */
	private static StorageProvider theStorageProvider;

	/** Storage */
	private Storage storage;

	/** Base Paths are used to get relative path from absolute path */
	List<String> basePaths = new ArrayList<>();

	/** For smooth integration only, will be removed */
	private boolean version2;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(StorageProvider.class);

	/** Storage Manager Configuration */
	@Autowired
	private StorageManagerConfiguration cfg;

	/** Source path is used as a central place to upload files to storage */
	private String sourcePath;
	
	/** Storage path is used for posix storage */
	private String storagePath;
	
	/** Cache path is used for downloaded files from storage */
	private String cachePath;

	/**
	 * Instance of storage provider
	 * 
	 * @return storage provider singleton
	 */
	public static StorageProvider getInstance() {

		return theStorageProvider;
	}

	/**
	 * Simple default Constructor
	 */
	public StorageProvider() {

		//init();
	}

	/**
	 * Initializes storage(s) from Application.yml
	 */
	@PostConstruct
	private void init() {

		theStorageProvider = this;
		storage = createStorage(StorageType.valueOf(cfg.getDefaultStorageType()), cfg.getPosixBackendPath());

		version2 = cfg.getStorageManagerVersion2().equals("true") ? true : false;

		basePaths.add(storage.getBasePath());
		basePaths.add(cfg.getPosixSourcePath());
		basePaths.add(cfg.getPosixCachePath());

		loadDefaultPaths();
	}
	
	/**
	 * Load default source, storage and cache paths
	 */
	public void loadDefaultPaths() {

		sourcePath = cfg.getPosixSourcePath();
		storagePath = cfg.getPosixBackendPath();
		cachePath = cfg.getPosixCachePath();
	}
	
	/**
	 * Sets source path
	 * 
	 * @param sourcePath 
	 */
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	/**
	 * Sets storage path
	 * 
	 * @param storagePath
	 */
	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}

	/**
	 * Sets cache path
	 * 
	 * @param cachePath
	 */
	public void setCachePath(String cachePath) {
		this.cachePath = cachePath;
	}
	
	/**
	 * Gets source path
	 * 
	 * @return source path
	 */
	public String getSourcePath() {
		return sourcePath; 
	}
	
	/**
	 * Gets storage path
	 * 
	 * @return storage path
	 */
	public String getStoragePath() {
		return storagePath; 
	}
	
	/**
	 * Gets cache path
	 * 
	 * @return cache path
	 */
	public String getCachePath() {
		return cachePath; 
	}
	
	/**
	 * Gets storage
	 * 
	 * @return storage
	 */
	public Storage getStorage() {
		return storage;
	}
	
	/**
	 * Sets Storage
	 * 
	 * @param storageType
	 * @return storage, which was set
	 */
	public Storage setStorage(StorageType storageType) {
		
		cfg.setDefaultStorageType(storageType.toString());
		storage = createStorage(storageType, storagePath);
		
		return storage;
	}
	
	
	/**
	 * Gets the list of base paths
	 * 
	 * @return  the list of base paths
	 */
	public List<String> getBasePaths() {
		
		return basePaths; 
	}

	// all ..version.. methods will be removed in release, for integration only
	
	/**
	 * Loads version 1 of storage manager (currently used) 
	 * 
	 */
	public void loadVersion1() {
		version2 = false;
	}

	/**
	 * Loads version 2 of storage manager (new source code) 
	 * 
	 */
	public void loadVersion2() {
		version2 = true;
	}

	/**
	 * Checks if version 2 of storage manager is currently used 
	 * 
	 */
	public boolean isVersion2() {
		return version2;
	}
	
	/**
	 * Checks if version 1 of storage manager is currently used 
	 * 
	 */	
	public boolean isVersion1() {
		return !version2;
	}

	public long getCacheFileSize(String relativePath) throws IOException {

		return getPosixFileSize(getAbsoluteCachePath(relativePath));
	}

	public long getSourceFileSize(String relativePath) throws IOException {

		return getPosixFileSize(getAbsoluteSourcePath(relativePath));
	}

	public long getPosixFileSize(String absolutePath) throws IOException {

		Path path = Paths.get(absolutePath);
		return Files.size(path);
	}

	private Storage createStorage(StorageType storageType, String storagePath) {

		if (storageType == StorageType.POSIX) {
			return new PosixStorage(storagePath, sourcePath);

		} else if (storageType == StorageType.S3) {
			return new S3Storage(storagePath, sourcePath, cfg.getS3AccessKey(), cfg.getS3SecretAccessKey(),
					cfg.getS3DefaultBucket());
		}

		throw new IllegalArgumentException("Storage Type " + storageType.toString() + " is wrong");
	}

	public StorageFile getCacheFile(String relativePath) {

		return new PosixStorageFile(cachePath, relativePath);
	}

	public StorageFile getSourceFile(String relativePath) {

		return new PosixStorageFile(sourcePath, relativePath);
	}

	public StorageFile getPosixFile(String basePath, String relativePath) {

		return new PosixStorageFile(basePath, relativePath);
	}

	public StorageFile getStorageFile(String relativePath) {

		StorageType storageType = storage.getStorageType();

		if (storageType == StorageType.POSIX) {
			return new PosixStorageFile(storagePath, relativePath);

		} else if (storageType == StorageType.S3) {
			return new S3StorageFile(cfg.getS3DefaultBucket(), relativePath);
		}

		throw new IllegalArgumentException("Storage Type " + storageType.toString() + " is wrong");
	}

	// if file only, return source path
	public StorageFile getAbsoluteFile(String absolutePath) {

		String basePath = new PathConverter(absolutePath, basePaths).getFirstFolder().getPath();
		String relativePath = new PathConverter(absolutePath, basePaths).removeFirstFolder().getPath();

		return new PosixStorageFile(basePath, relativePath);
	}

	public String getAbsoluteSourcePath(String relativePath) {

		String path = Paths.get(sourcePath, relativePath).toString();
		return new PathConverter(path, basePaths).convertToSlash().getPath();
	}

	public String getAbsoluteCachePath(String relativePath) {

		String path = Paths.get(cachePath, relativePath).toString();
		return new PathConverter(path, basePaths).convertToSlash().getPath();
	}

	public String getAbsoluteStoragePath(String relativePath) {

		String path = Paths.get(storagePath, relativePath).toString();

		return new PathConverter(path, basePaths).convertToSlash().getPath();
	}

	public String getRelativePath(String absolutePath) {

		return new PathConverter(absolutePath, basePaths).getRelativePath().getPath();
	}

	public StorageFile createStorageFile(String relativePath, String content) {

		return storage.createStorageFile(relativePath, content);
	}
}
