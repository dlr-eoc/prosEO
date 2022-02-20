package de.dlr.proseo.storagemgr.version2;


import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.cache.FileCache;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageType;
import de.dlr.proseo.storagemgr.version2.posix.PosixStorage;
import de.dlr.proseo.storagemgr.version2.s3.S3Storage;
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

	private Storage internalStorage;
	private Storage externalStorage;
	
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
		
		// loadProfile(StorageProviderProfile.INTERNAL_POSIX_EXTERNAL_POSIX);
		
		// loadProfile(StorageProviderProfile.DEFAULT);	
	}
	
	/**
	 * Initializes file cache with directory from Application.yml
	 */
	@PostConstruct
	private void init() { 
		
		loadProfile(StorageProviderProfile.INTERNAL_POSIX_EXTERNAL_POSIX);
		
		theStorageProvider = this;
		// loadProfile(StorageProviderProfile.DEFAULT);	
	}
	

	public StorageProvider(StorageProviderProfile profile) { 
		
		loadProfile(profile);
	}
	
	public void loadProfile(StorageProviderProfile profile) {
		
		this.profile = profile; 
		
		if (profile == StorageProviderProfile.DEFAULT) {
			
			externalStorage = createStorage(StorageType.S3);
			internalStorage = createStorage(cfg.getDefaultStorageType()); 
		}		
		else if (profile == StorageProviderProfile.INTERNAL_POSIX_EXTERNAL_POSIX) {
			
			if (cfg == null) System.out.println("CFG IS NULL");
			
			externalStorage = new PosixStorage(cfg.getPosixBackendPath() + "/exernal"); // temp. for testing
			internalStorage = createStorage(StorageType.POSIX);  
		}
	}
	
	public Storage getInternalStorage() {
		
		return internalStorage; 
	}
	
	public Storage getExternalStorage() {
		
		return externalStorage; 
	}
	
	public Storage createStorage(StorageType storageType) { 
		if (storageType == StorageType.POSIX) { 
			return new PosixStorage(cfg.getPosixBackendPath()); 
		}
		else if (storageType == StorageType.S3) { 
			return new S3Storage(cfg.getS3AccessKey(), cfg.getS3SecretAccessKey());
		}
		
		throw new IllegalArgumentException("Storage Type " + storageType.toString() + " is wrong");
	}
	
	public Storage createStorage(String storageType) { 
		return createStorage(StorageType.valueOf(storageType)); 
	}
}
