/**
 * StorageProvider.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;

import de.dlr.proseo.storagemgr.posix.PosixStorage;
import de.dlr.proseo.storagemgr.posix.PosixStorageFile;
import de.dlr.proseo.storagemgr.posix.PosixConfiguration;
import de.dlr.proseo.storagemgr.posix.PosixDAL;

import de.dlr.proseo.storagemgr.s3.S3Configuration;
import de.dlr.proseo.storagemgr.s3.S3Storage;
import de.dlr.proseo.storagemgr.s3.S3StorageFile;

import de.dlr.proseo.storagemgr.utils.PathConverter;

/**
 * A central manager for different types of storage in the application. It
 * handles the creation, retrieval, and management of storage instances,
 * provides file-related operations and paths, and offers various utility
 * methods related to storage management.
 *
 * This class acts as a singleton and manages the storage configuration, source
 * paths, and storage operations.
 *
 * Author: Denys Chaykovskiy
 */
@Component
public class StorageProvider {

	/** StorageProvider singleton */
	private static StorageProvider theStorageProvider;

	/** The default storage as configured for the application */
	private Storage defaultStorage;

	/** Base Paths are used to get relative path from absolute path */
	List<String> basePaths = new ArrayList<>();

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(StorageProvider.class);

	/** Storage Manager Configuration */
	@Autowired
	private StorageManagerConfiguration cfg;

	/** Source path is used as a central place to upload files to defaultStorage */
	private String sourcePath;

	/** Storage path is the configured default file path for POSIX storage */
	private String defaultPosixPath;

	/** Cache path is used for downloaded files from storage */
	private String cachePath;

	/**
	 * Returns the instance of the storage provider (singleton).
	 *
	 * @return the storage provider singleton instance
	 */
	public static StorageProvider getInstance() {
		return theStorageProvider;
	}

	/**
	 * Simple default constructor
	 */
	public StorageProvider() {
		// init();
	}

	/**
	 * Load default source, storage, and cache paths
	 */
	public void loadDefaultPaths() {
		sourcePath = cfg.getDefaultSourcePath();
		defaultPosixPath = cfg.getPosixBackendPath();
		cachePath = cfg.getPosixCachePath();
	}

	/**
	 * Sets the source path.
	 *
	 * @param sourcePath the source path to set
	 */
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	/**
	 * Sets the default POSIX storage path.
	 *
	 * @param defaultPosixPath the storage path to set
	 */
//	public void setDefaultPosixPath(String defaultPosixPath) {
//		this.defaultPosixPath = defaultPosixPath;
//	}

	/**
	 * Sets the cache path.
	 *
	 * @param cachePath the cache path to set
	 */
	public void setCachePath(String cachePath) {
		this.cachePath = cachePath;
	}

	/**
	 * Gets the source path.
	 *
	 * @return the source path
	 */
	public String getSourcePath() {
		return sourcePath;
	}

	/**
	 * Gets the default POSIX storage path.
	 *
	 * @return the POSIX storage path
	 */
	public String getDefaultStoragePath() {
		return defaultPosixPath;
	}

	/**
	 * Gets the cache path.
	 *
	 * @return the cache path
	 */
	public String getCachePath() {
		return cachePath;
	}

	/**
	 * Gets the current storage instance.
	 *
	 * @return the current defaultStorage instance
	 */
	public Storage getStorage() {
		return defaultStorage;
	}

	/**
	 * Gets the default storage instance based on the specified storage type.
	 *
	 * @param storageType the storage type
	 * @return the storage instance
	 * @throws IOException if an error occurs during storage creation
	 */
	public Storage getDefaultStorage(StorageType storageType) throws IOException {
		return getStorage(storageType, 
				StorageType.POSIX.equals(storageType) ? defaultPosixPath : cfg.getS3DefaultBucket());
	}

	/**
	 * Gets the desired storage instance based on the specified storage type.
	 *
	 * @param storageType the storage type
	 * @param storagePath the base path for a POSIX storage and the bucket for an S3 storage 
	 * @return the storage instance
	 * @throws IOException if an error occurs during storage creation
	 */
	public Storage getStorage(StorageType storageType, String storagePath) throws IOException {
		return createStorage(storageType, storagePath);
	}

	/**
	 * Gets the storage instance based on the specified absolute path.
	 *
	 * @param absolutePath the absolute path
	 * @return the storage instance
	 * @throws IOException if an error occurs during storage retrieval
	 */
	public Storage getStorage(String absolutePath) throws IOException {
		PathConverter pcv = new PathConverter(absolutePath);
		StorageType storageType = pcv.getStorageType();
		return getStorage(storageType, 
				StorageType.POSIX.equals(storageType) ? pcv.getPath() : pcv.removeFsPrefix().getFirstFolder().getPath());
	}

	/**
	 * Sets the default storage type and creates the corresponding storage instance.
	 * 
	 * NOT INTENDED FOR USE OUTSIDE UNIT TEST CASES!
	 *
	 * @param storageType the storage type to set
	 * @return the created storage instance
	 * @throws IOException if an error occurs during storage creation
	 */
	public Storage setDefaultStorage(StorageType storageType) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> setStorage({})", storageType.toString());

		cfg.setDefaultStorageType(storageType.toString());
		defaultStorage = createStorage(storageType, 
				StorageType.POSIX.equals(storageType) ? defaultPosixPath : cfg.getS3DefaultBucket());

		return defaultStorage;
	}

	/**
	 * Gets the list of base paths.
	 *
	 * @return the list of base paths
	 */
	public List<String> getBasePaths() {
		return basePaths;
	}

	/**
	 * Gets the file size of the cache file specified by the relative path.
	 *
	 * @param relativePath the relative path of the cache file
	 * @return the file size in bytes
	 * @throws IOException if an error occurs while getting the file size
	 */
	public long getCacheFileSize(String relativePath) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getCacheFileSize({})", relativePath);

		return getPosixFileSize(getAbsoluteCachePath(relativePath));
	}

	/**
	 * Gets the file size of the source file specified by the relative path.
	 *
	 * @param relativePath the relative path of the source file
	 * @return the file size in bytes
	 * @throws IOException if an error occurs while getting the file size
	 */
	public long getSourceFileSize(String relativePath) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getSourceFileSize({})", relativePath);

		return getPosixFileSize(getAbsoluteSourcePath(relativePath));
	}

	/**
	 * Gets the file size of the posix file specified by the absolute path.
	 *
	 * @param absolutePath the absolute path of the posix file
	 * @return the file size in bytes
	 * @throws IOException if an error occurs while getting the file size
	 */
	public long getPosixFileSize(String absolutePath) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getPosixFileSize({})", absolutePath);

		Path path = Paths.get(absolutePath);
		return Files.size(path);
	}

	/**
	 * Gets the cache file as a StorageFile from the relative path. The path can be
	 * virtual.
	 *
	 * @param relativePath the relative path of the cache file
	 * @return the StorageFile representing the cache file
	 */
	public StorageFile getCacheFile(String relativePath) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getCacheFile({})", relativePath);

		return new PosixStorageFile(cachePath, relativePath);
	}

	/**
	 * Gets the source file as a StorageFile from the relative path. The path can be
	 * virtual.
	 *
	 * @param relativePath the relative path of the source file
	 * @return the StorageFile representing the source file
	 */
	public StorageFile getSourceFile(String relativePath) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getSourceFile({})", relativePath);

		return new PosixStorageFile(sourcePath, relativePath);
	}

	/**
	 * Gets the posix file as a StorageFile from the relative path. The path can be
	 * virtual.
	 *
	 * @param basePath     the base path of the posix defaultStorage
	 * @param relativePath the relative path of the file
	 * @return the StorageFile representing the posix file
	 */
	public StorageFile getPosixFile(String basePath, String relativePath) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getPosixFile({})", relativePath);

		return new PosixStorageFile(basePath, relativePath);
	}

	/**
	 * Gets the storage file as a StorageFile from the relative path. The path can
	 * be virtual.
	 * @param storage the storage to get the file from
	 * @param relativePath the relative path of the file
	 *
	 * @return the StorageFile representing the storage file
	 */
	public StorageFile getStorageFile(Storage storage, String relativePath) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getStorageFile({}, {})",
					null == storage ? "NULL" : 
						(StorageType.POSIX.equals(storage.getStorageType()) ? storage.getBasePath() : storage.getBucket()),
					relativePath);

		StorageType storageType = storage.getStorageType();

		if (StorageType.POSIX.equals(storageType)) {
			return new PosixStorageFile(storage.getBasePath(), relativePath);
		} else if (StorageType.S3.equals(storageType)) {
			return new S3StorageFile(storage.getBucket(), relativePath);
		}

		throw new IllegalArgumentException("Storage Type " + storageType.toString() + " is wrong");
	}
	
	/**
	 * Gets the storage file as a StorageFile from the relative path. The path can
	 * be virtual using the default storage
	 * 
	 * @param relativePath the relative path of the file
	 *
	 * @return the StorageFile representing the storage file
	 */
	public StorageFile getStorageFileFromDefaultStorage(String relativePath) {
		return getStorageFile(defaultStorage, relativePath);
	}

	/**
	 * Gets the file size of the storage file specified by the StorageFile instance.
	 * 
	 * NOT USED AND NOT TESTED
	 *
	 * @param storageFile the StorageFile instance
	 * @return the file size in bytes
	 * @throws IOException if an error occurs while getting the file size
	 */
//	public Long getFileSize(StorageFile storageFile) throws IOException {
//		if (logger.isTraceEnabled())
//			logger.trace(">>> getFileSize({})", storageFile.getFullPath());
//
//		StorageType storageType = new PathConverter(storageFile.getFullPath()).getStorageType();
//		
//		if (StorageType.POSIX.equals(storageType)) {
//			return getStorage(storageType, storageFile.getBasePath()).getFileSize(storageFile);
//		} else {
//			return getStorage(storageType, storageFile.getBucket()).getFileSize(storageFile);
//		}
//	}

	/**
	 * Gets an input stream from the storage file specified by the StorageFile
	 * instance.
	 * 
	 * NOT USED AND NOT TESTED
	 *
	 * @param storageFile the StorageFile instance
	 * @return the input stream
	 * @throws IOException if an error occurs while getting the input stream
	 */
//	public InputStream getInputStream(StorageFile storageFile) throws IOException {
//		if (logger.isTraceEnabled())
//			logger.trace(">>> getInputStream({})", storageFile.getFullPath());
//
//		StorageType storageType = new PathConverter(storageFile.getFullPath()).getStorageType();
//
//		if (StorageType.POSIX.equals(storageType)) {
//			return getStorage(storageType, storageFile.getBasePath()).getInputStream(storageFile);
//		} else {
//			return getStorage(storageType, storageFile.getBucket()).getInputStream(storageFile);
//		}
//	}

	/**
	 * Gets a file as a POSIX StorageFile from the absolute path. The path to the file can
	 * be virtual.
	 *
	 * @param absolutePath the absolute path to the file
	 * @return the StorageFile representing the file
	 */
	public StorageFile getAbsoluteFile(String absolutePath) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getAbsoluteFile({})", absolutePath);

		String basePath = new PathConverter(absolutePath, basePaths).getFirstFolder().getPath();
		String relativePath = new PathConverter(absolutePath, basePaths).removeFirstFolder().getPath();

		return new PosixStorageFile(basePath, relativePath);
	}

	/**
	 * Gets the absolute source path from the relative path.
	 *
	 * @param relativePath the relative path to the file
	 * @return the absolute source path from the relative path
	 */
	public String getAbsoluteSourcePath(String relativePath) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getAbsoluteSourcePath({})", relativePath);

		String path = Paths.get(sourcePath, relativePath).toString();
		return new PathConverter(path, basePaths).convertToSlash().getPath();
	}

	/**
	 * Gets the absolute cache path from the relative path.
	 *
	 * @param relativePath the relative path to the file
	 * @return the absolute cache path from the relative path
	 */
	public String getAbsoluteCachePath(String relativePath) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getAbsoluteCachePath({})", relativePath);

		String path = Paths.get(cachePath, relativePath).toString();
		return new PathConverter(path, basePaths).convertToSlash().getPath();
	}

	/**
	 * Gets the relative path from the absolute path using the base path list.
	 *
	 * @param absolutePath the absolute path
	 * @return the relative path
	 * @throws IOException if an error occurs while getting the relative path
	 */
	public String getRelativePath(String absolutePath) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getRelativePath({})", absolutePath);

		PathConverter pcv = new PathConverter(absolutePath);
		StorageType storageType = pcv.getStorageType();
		return getStorage(storageType, 
				StorageType.POSIX.equals(storageType) ? pcv.getPath() : pcv.removeFsPrefix().getFirstFolder().getPath())
			.getRelativePath(absolutePath);
	}

	/**
	 * Creates a physical file in the storage based on the relative path and
	 * content.
	 *
	 * @param relativePath the relative path to the file
	 * @param content      the content of the file
	 * @return the StorageFile representing the physically created file
	 * @throws IOException if an error occurs while creating the storage file
	 */
	public StorageFile createStorageFile(String relativePath, String content) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createStorageFile({}, {})", relativePath, content.length());

		return defaultStorage.createStorageFile(relativePath, content);
	}
	
	/**
	 * Gets the POSIX configuration from the file.
	 *
	 * @return the POSIX configuration
	 */
	public PosixConfiguration getPosixConfigurationFromFile() {
		PosixConfiguration posixConfiguration = new PosixConfiguration();
		
		// FYI: No bucket configuration is used in prosEO for POSIX Storage
		posixConfiguration.setBucket(StorageFile.NO_BUCKET);
		
		posixConfiguration.setBasePath(cfg.getPosixBackendPath());
		posixConfiguration.setSourcePath(cfg.getDefaultSourcePath());

		posixConfiguration.setMaxRequestAttempts(cfg.getMaxRequestAttempts());

		posixConfiguration.setFileCheckWaitTime(cfg.getFileCheckWaitTime());

		return posixConfiguration;
	}

	/**
	 * Gets the S3 configuration from the file.
	 *
	 * @return the S3 configuration
	 */
	public S3Configuration getS3ConfigurationFromFile() {
		S3Configuration s3Configuration = new S3Configuration();

		s3Configuration.setS3AccessKey(cfg.getS3AccessKey());
		s3Configuration.setS3SecretAccessKey(cfg.getS3SecretAccessKey());
		s3Configuration.setS3Region(cfg.getS3Region());
		s3Configuration.setS3EndPoint(cfg.getS3EndPoint());

		s3Configuration.setBucket(cfg.getS3DefaultBucket());
		s3Configuration.setBasePath(cfg.getPosixBackendPath());
		s3Configuration.setSourcePath(cfg.getDefaultSourcePath());

		s3Configuration.setMaxRequestAttempts(cfg.getMaxRequestAttempts());

		s3Configuration.setFileCheckWaitTime(cfg.getFileCheckWaitTime());

		s3Configuration.setDefaultEndPoint(Boolean.parseBoolean(cfg.getS3DefaultEndPoint()));

		return s3Configuration;
	}
	
	/**
	 * Copies files from an absolute source path to an absolute path in the cache
	 * 
	 * @param sourceFile the file path to copy from
	 * @param destCacheFile the file path to copy to
	 * @return a path list of copied files
	 * @throws IOException 
	 */
	public List<String> copyAbsoluteFilesToCache(String sourceFile, StorageFile destCacheFile) throws IOException {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> copyAbsoluteFilesToCache({}, {})", sourceFile, 
					(null == destCacheFile ? "null" : destCacheFile.getFullPath()));
				
		PosixDAL posixDAL = new PosixDAL(getPosixConfigurationFromFile());
		
		return posixDAL.copy(sourceFile, destCacheFile.getFullPath());

	}
	
	/**
	 * Initializes storage(s) from Application.yml
	 *
	 * @throws IOException if an error occurs during initialization
	 */
	@PostConstruct
	private void init() throws IOException {
		theStorageProvider = this;
		StorageType storageType = StorageType.valueOf(cfg.getDefaultStorageType());
		defaultStorage = createStorage(storageType, 
				StorageType.POSIX.equals(storageType) ? cfg.getPosixBackendPath() : cfg.getS3DefaultBucket());

		basePaths.add(defaultStorage.getBasePath());
		basePaths.add(cfg.getDefaultSourcePath());
		basePaths.add(cfg.getPosixCachePath());

		loadDefaultPaths();
	}
	
	/**
	 * Creates a storage instance based on the specified storage type and storage
	 * path.
	 *
	 * @param storageType the storage type (S3 or POSIX)
	 * @param storagePath the base path for a POSIX storage and the bucket for an S3 storage
	 * @return the created storage instance
	 * @throws IOException if an error occurs during storage creation
	 */
	private Storage createStorage(StorageType storageType, String storagePath) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createStorage({}, {})", storageType.toString(), storagePath);

		sourcePath = cfg.getDefaultSourcePath();

		if (StorageType.POSIX.equals(storageType)) {
			PosixConfiguration posixConfig = getPosixConfigurationFromFile();
			posixConfig.setBasePath(storagePath);
			return new PosixStorage(posixConfig);
		} else if (StorageType.S3.equals(storageType)) {
			S3Configuration s3Config = getS3ConfigurationFromFile();
			s3Config.setBucket(storagePath);
			return new S3Storage(s3Config);
		}

		throw new IllegalArgumentException("Storage Type " + storageType.toString() + " is wrong");
	}
}