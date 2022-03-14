package de.dlr.proseo.storagemgr.version2;

import java.io.File;

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
 * Storage Provider for different storages and different profiles
 * 
 * @author Denys Chaykovskiy
 *
 */
@Component
public class StorageProvider {

	/** StorageProvider singleton */
	private static StorageProvider theStorageProvider;

	private Storage storage;

	private PathConverter pathConverter = new PathConverter();

	private boolean version2;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(StorageProvider.class);

	@Autowired
	private StorageManagerConfiguration cfg;

	/**
	 * Instance of file cache
	 * 
	 * @return file cache singleton
	 */
	public static StorageProvider getInstance() {

		return theStorageProvider;
	}

	public StorageProvider() {
	}

	/**
	 * Initializes storage(s) from Application.yml
	 */
	@PostConstruct
	private void init() {

		theStorageProvider = this;
		storage = createStorage(StorageType.valueOf(cfg.getDefaultStorageType()));

		version2 = cfg.getStorageManagerVersion2().equals("true") ? true : false;
		
		pathConverter.addBasePath(storage.getBasePath());
		pathConverter.addBasePath(cfg.getSourcePath());
		pathConverter.addBasePath(cfg.getPosixCachePath());
	}

	public Storage getStorage() {
		return storage;
	}

	public void loadStorage(StorageType storageType) {

		storage = createStorage(storageType);
	}

	// all ..version.. methods will be removed in release, for smooth integration only
	public void loadVersion1() {
		version2 = false;
	}

	public void loadVersion2() {
		version2 = true;
	}

	public boolean isVersion2() {
		return version2;
	}

	public StorageFile getPosixStorageFile(String basePath, String relativePath) {

		return new PosixStorageFile(basePath, relativePath);
	}

	public StorageFile getStorageFile(String relativePath) {

		StorageType storageType = storage.getStorageType();

		if (storageType == StorageType.POSIX) {
			return new PosixStorageFile(cfg.getPosixBackendPath(), relativePath);

		} else if (storageType == StorageType.S3) {
			return new S3StorageFile(cfg.getS3DefaultBucket(), relativePath);
		}

		throw new IllegalArgumentException("Storage Type " + storageType.toString() + " is wrong");
	}

	public StorageFile getCacheFile(String relativePath) {

		return new PosixStorageFile(cfg.getPosixCachePath(), relativePath);
	}

	private Storage createStorage(StorageType storageType) {

		if (storageType == StorageType.POSIX) {
			return new PosixStorage(cfg.getPosixBackendPath());

		} else if (storageType == StorageType.S3) {
			return new S3Storage(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey());
		}

		throw new IllegalArgumentException("Storage Type " + storageType.toString() + " is wrong");
	}

	public String getRelativePath(String absolutePath) {

		return pathConverter.getRelativePath(absolutePath);
	}
}
