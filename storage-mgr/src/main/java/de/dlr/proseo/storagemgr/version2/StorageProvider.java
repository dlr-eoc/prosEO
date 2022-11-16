package de.dlr.proseo.storagemgr.version2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;
import de.dlr.proseo.storagemgr.version2.posix.PosixStorage;
import de.dlr.proseo.storagemgr.version2.posix.PosixStorageFile;
import de.dlr.proseo.storagemgr.version2.s3.S3Configuration;
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

		// init();
	}

	/**
	 * Initializes storage(s) from Application.yml
	 * 
	 * @throws IOException
	 */
	@PostConstruct
	private void init() throws IOException {

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
	 * Gets desired storage
	 * 
	 * @param storageType storage type
	 * @return storage storage
	 * @throws IOException
	 */
	public Storage getStorage(StorageType storageType) throws IOException {
		return createStorage(storageType, storagePath);
	}

	/**
	 * Gets storage according to absolute path
	 * 
	 * @param absolutePath absolute path
	 * @return storage
	 * @throws IOException
	 */
	public Storage getStorage(String absolutePath) throws IOException {

		if (new PathConverter(absolutePath).isS3Path()) {

			return getStorage(StorageType.S3);
		} else {

			return getStorage(StorageType.POSIX);
		}
	}

	/**
	 * Sets Storage
	 * 
	 * @param storageType storage
	 * @return storage, which was set
	 * @throws IOException
	 */
	public Storage setStorage(StorageType storageType) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> setStorage({})", storageType.toString());

		cfg.setDefaultStorageType(storageType.toString());
		storage = createStorage(storageType, storagePath);

		return storage;
	}

	/**
	 * Gets the list of base paths
	 * 
	 * @return the list of base paths
	 */
	public List<String> getBasePaths() {

		return basePaths;
	}

	// all ..version.. methods will be removed in release, for smooth integration
	// only

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

	/**
	 * Gets the cache file size
	 * 
	 * @param relativePath relative path of cache file
	 * @return file size in bytes
	 * @throws IOException if cannot get file size
	 */
	public long getCacheFileSize(String relativePath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getCacheFileSize({})", relativePath);

		return getPosixFileSize(getAbsoluteCachePath(relativePath));
	}

	/**
	 * Gets the source file size
	 * 
	 * @param relativePath relative source file path
	 * @return file size in bytes
	 * @throws IOException if cannot get file size
	 */
	public long getSourceFileSize(String relativePath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getSourceFileSize({})", relativePath);

		return getPosixFileSize(getAbsoluteSourcePath(relativePath));
	}

	/**
	 * Gets the posix file size
	 * 
	 * @param absolutePath absolute path
	 * @return file size in bytes
	 * @throws IOException if cannot get file size
	 */
	public long getPosixFileSize(String absolutePath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getPosixFileSize({})", absolutePath);

		Path path = Paths.get(absolutePath);
		return Files.size(path);
	}

	/**
	 * Creates storage under storage path.
	 * 
	 * @param storageType currently s3 or posix
	 * @param storagePath base path of posix storage, for s3 will be used for
	 *                    temporary files
	 * @return created storage
	 * @throws IOException
	 */
	private Storage createStorage(StorageType storageType, String storagePath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> createStorage({}, {})", storageType.toString(), storagePath);

		if (storageType == StorageType.POSIX) {
			return new PosixStorage(storagePath, sourcePath);

		} else if (storageType == StorageType.S3) {

			return new S3Storage(getS3ConfigurationFromFile());
		}

		throw new IllegalArgumentException("Storage Type " + storageType.toString() + " is wrong");
	}

	/**
	 * Gets cache file as StorageFile from relative path. Path can be virtual
	 * 
	 * @param relativePath
	 * @return StorageFile
	 */
	public StorageFile getCacheFile(String relativePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getCacheFile({})", relativePath);

		return new PosixStorageFile(cachePath, relativePath);
	}

	/**
	 * Gets source file as StorageFile from relative path. Path can be virtual
	 * 
	 * @param relativePath
	 * @return StorageFile
	 */
	public StorageFile getSourceFile(String relativePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getSourceFile({})", relativePath);

		return new PosixStorageFile(sourcePath, relativePath);
	}

	/**
	 * Gets posix file as StorageFile from relative path. Path can be virtual
	 * 
	 * @param relativePath
	 * @return StorageFile
	 */
	public StorageFile getPosixFile(String basePath, String relativePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getPosixFile({})", relativePath);

		return new PosixStorageFile(basePath, relativePath);
	}

	/**
	 * Gets storage file as StorageFile from relative path. Path can be virtual
	 * 
	 * @param relativePath
	 * @return StorageFile
	 */
	public StorageFile getStorageFile(String relativePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getStorageFile({})", relativePath);

		StorageType storageType = storage.getStorageType();

		if (storageType == StorageType.POSIX) {
			return new PosixStorageFile(storagePath, relativePath);

		} else if (storageType == StorageType.S3) {
			return new S3StorageFile(cfg.getS3DefaultBucket(), relativePath);
		}

		throw new IllegalArgumentException("Storage Type " + storageType.toString() + " is wrong");
	}

	/**
	 * Gets storage file size from StorageFile
	 * 
	 * @param storageFile Storage File
	 * @return file size
	 * @throws IOException
	 */
	public Long getFileSize(StorageFile storageFile) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFileSize({})", storageFile.getFullPath());

		if (new PathConverter(storageFile.getFullPath()).isS3Path()) {

			return getStorage(StorageType.S3).getFileSize(storageFile);
		} else {

			return getStorage(StorageType.POSIX).getFileSize(storageFile);
		}
	}

	/**
	 * Gets input stream from StorageFile
	 * 
	 * @param storageFile Storage File
	 * @return input stream
	 * @throws IOException
	 */
	public InputStream getInputStream(StorageFile storageFile) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getInputStream({})", storageFile.getFullPath());

		if (new PathConverter(storageFile.getFullPath()).isS3Path()) {

			return getStorage(StorageType.S3).getInputStream(storageFile);
		} else {

			return getStorage(StorageType.POSIX).getInputStream(storageFile);
		}
	}

	/**
	 * Gets a file as StorageFile from absolute path. Path to file can be virtual
	 * 
	 * @param relativePath
	 * @return StorageFile
	 */
	public StorageFile getAbsoluteFile(String absolutePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getAbsoluteFile({})", absolutePath);

		String basePath = new PathConverter(absolutePath, basePaths).getFirstFolder().getPath();
		String relativePath = new PathConverter(absolutePath, basePaths).removeFirstFolder().getPath();

		return new PosixStorageFile(basePath, relativePath);
	}

	/**
	 * Gets absolute source path from relative path
	 * 
	 * @param relativePath relative path to file
	 * @return absolute source path from relative path
	 */
	public String getAbsoluteSourcePath(String relativePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getAbsoluteSourcePath({})", relativePath);

		String path = Paths.get(sourcePath, relativePath).toString();
		return new PathConverter(path, basePaths).convertToSlash().getPath();
	}

	/**
	 * Gets absolute cache path from relative path
	 * 
	 * @param relativePath relative path to file
	 * @return absolute source path from relative path
	 */
	public String getAbsoluteCachePath(String relativePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getAbsoluteCachePath({})", relativePath);

		String path = Paths.get(cachePath, relativePath).toString();
		return new PathConverter(path, basePaths).convertToSlash().getPath();
	}

	/**
	 * Gets absolute posix storage path from relative path
	 * 
	 * @param relativePath relative path
	 * @return absolute source path from relative path
	 */
	public String getAbsolutePosixStoragePath(String relativePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getAbsolutePosixStoragePath({})", relativePath);

		String path = Paths.get(storagePath, relativePath).toString();
		return new PathConverter(path, basePaths).convertToSlash().getPath();
	}

	/**
	 * Gets relative path from absolute path using base path list
	 * 
	 * @param absolutePath absolute path
	 * @return relative path
	 * @throws IOException
	 */
	public String getRelativePath(String absolutePath) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> getRelativePath({})", absolutePath);

		if (new PathConverter(absolutePath).isS3Path()) {

			return getStorage(StorageType.S3).getRelativePath(absolutePath);
		} else {

			return getStorage(StorageType.POSIX).getRelativePath(absolutePath);
		}
	}

	/**
	 * Creates physically file in storage, based on relative path and content
	 * 
	 * @param relativePath relative path to file
	 * @param content      content of file
	 * @return StorageFile of physically created file
	 */
	public StorageFile createStorageFile(String relativePath, String content) {

		if (logger.isTraceEnabled())
			logger.trace(">>> createStorageFile({}, {})", relativePath, content.length());

		return storage.createStorageFile(relativePath, content);
	}

	/**
	 * Gets S3 Configuration from file
	 * 
	 * @return s3 configuration
	 */
	public S3Configuration getS3ConfigurationFromFile() {

		S3Configuration s3Configuration = new S3Configuration();

		s3Configuration.setS3AccessKey(cfg.getS3AccessKey());
		s3Configuration.setS3SecretAccessKey(cfg.getS3SecretAccessKey());
		s3Configuration.setS3Region(cfg.getS3Region());
		s3Configuration.setS3EndPoint(cfg.getS3EndPoint());

		s3Configuration.setBucket(cfg.getS3DefaultBucket());
		s3Configuration.setBasePath(cfg.getPosixBackendPath());
		s3Configuration.setSourcePath(cfg.getPosixSourcePath());

		s3Configuration.setMaxRequestAttempts(cfg.getMaxRequestAttempts());

		s3Configuration.setFileCheckWaitTime(cfg.getFileCheckWaitTime());

		s3Configuration.setDefaultEndPoint(Boolean.parseBoolean(cfg.getS3DefaultEndPoint()));

		return s3Configuration;
	}
}
