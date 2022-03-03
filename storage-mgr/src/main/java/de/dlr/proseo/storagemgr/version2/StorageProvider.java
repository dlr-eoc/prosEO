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

	//private StorageProfile profile;

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

		// loadProfile(StorageProfile.getDefaultConfiguration());
		theStorageProvider = this;
		
		// create cache from profile.getCache()
		storage = createStorage(StorageType.valueOf(cfg.getDefaultStorageType())); 
	}

	/*
	public StorageProvider(StorageProfile profile) {

		loadProfile(profile);
	}
	*/

	/*
	public void loadProfile(StorageProfile profile) {

		// TODO: For debugging - remove later
		//if (cfg == null)
		//	System.out.println("CFG in loadProfile IS NULL");

		this.profile = profile;
		
		
		// create cache from profile.getCache()
		createStorage(profile.getStorageType()); 
	}

	public StorageProfile getProfile() {
		return profile;
	}
	*/

	public Storage getStorage() {
		return storage;
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
	
	// no bucket for s3 and posix
	public String getRelativePath(String absolutePath) {
		
		String path = absolutePath;
		
		if (path.startsWith("s3:/") || path.startsWith("S3:/")) {
			path = path.substring(4);
		}
		
		while (path.startsWith("/")) {
			path = path.substring(1);
		}
		
		return path;
	}
	
	


	/*
	public long getSize() {

		// TODO: Maybe use method from FileUtils
		return new File(getFullPath()).length();
	}
	*/
}
