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

	private StorageProviderProfile profile;

	private Storage storage;

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

		loadProfile(StorageProviderProfile.DEFAULT);
		theStorageProvider = this;
	}

	public StorageProvider(StorageProviderProfile profile) {

		loadProfile(profile);
	}

	public void loadProfile(StorageProviderProfile profile) {

		// TODO: For debugging - remove later
		if (cfg == null)
			System.out.println("CFG in loadProfile IS NULL");

		this.profile = profile;

		if (profile == StorageProviderProfile.DEFAULT) {
			storage = createStorage(cfg.getDefaultStorageType());
		} else if (profile == StorageProviderProfile.STORAGE_POSIX) {
			storage = createStorage(StorageType.POSIX);
		} else if (profile == StorageProviderProfile.STORAGE_S3) {
			storage = createStorage(StorageType.S3);
		}
	}

	public StorageProviderProfile getProfile() {
		return profile;
	}

	public Storage getStorage() {
		return storage;
	}

	public Storage createStorage(StorageType storageType) {

		if (storageType == StorageType.POSIX) {
			return new PosixStorage(cfg.getPosixBackendPath());

		} else if (storageType == StorageType.S3) {
			return new S3Storage(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey());
		}

		throw new IllegalArgumentException("Storage Type " + storageType.toString() + " is wrong");
	}

	public Storage createStorage(String storageType) {
		return createStorage(StorageType.valueOf(storageType));
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

	/*
	public long getSize() {

		// TODO: Maybe use method from FileUtils
		return new File(getFullPath()).length();
	}
	*/
}
