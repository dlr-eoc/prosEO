package de.dlr.proseo.storagemgr.version2;

import org.springframework.beans.factory.annotation.Autowired;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

public class StorageProfile {
	
	private String posixCachePath; 
	private String posixStoragePath;
	private StorageType storageType; 
	private String s3AccessKey; 
	private String s3DefaultBucket;
	/**
	 * @return the s3DefaultBucket
	 */
	public String getS3DefaultBucket() {
		return s3DefaultBucket;
	}

	/**
	 * @param s3DefaultBucket the s3DefaultBucket to set
	 */
	public void setS3DefaultBucket(String s3DefaultBucket) {
		this.s3DefaultBucket = s3DefaultBucket;
	}

	/**
	 * @return the s3AccessKey
	 */
	public String getS3AccessKey() {
		return s3AccessKey;
	}

	/**
	 * @param s3AccessKey the s3AccessKey to set
	 */
	public void setS3AccessKey(String s3AccessKey) {
		this.s3AccessKey = s3AccessKey;
	}

	/**
	 * @return the s3SecretAccessKey
	 */
	public String getS3SecretAccessKey() {
		return s3SecretAccessKey;
	}

	/**
	 * @param s3SecretAccessKey the s3SecretAccessKey to set
	 */
	public void setS3SecretAccessKey(String s3SecretAccessKey) {
		this.s3SecretAccessKey = s3SecretAccessKey;
	}
	private String s3SecretAccessKey; 
	
	@Autowired
	private static StorageManagerConfiguration cfg;
	
	
	public static StorageProfile getDefaultConfiguration() { 
		
		StorageProfile storageProfile = new StorageProfile(); 
		
		storageProfile.setPosixCachePath(cfg.getPosixCachePath()); 
		storageProfile.setPosixStoragePath(cfg.getPosixBackendPath()); 
		storageProfile.setStorageType(StorageType.valueOf(cfg.getDefaultStorageType()));
	
		storageProfile.setS3AccessKey(cfg.getS3AccessKey()); 
		storageProfile.setS3SecretAccessKey(cfg.getS3SecretAccessKey());  
		storageProfile.setS3DefaultBucket(cfg.getS3DefaultBucket());
	
		return storageProfile; 	
	}
	
	public static StorageProfile getTestPosixConfiguration(String posixCachePath, String posixStoragePath) { 
		
		StorageProfile storageProfile = new StorageProfile(); 
		
		storageProfile.setPosixCachePath(posixCachePath); 
		storageProfile.setPosixStoragePath(posixStoragePath); 
		storageProfile.setStorageType(StorageType.valueOf(cfg.getDefaultStorageType()));
		
		storageProfile.setS3AccessKey(cfg.getS3AccessKey()); 
		storageProfile.setS3SecretAccessKey(cfg.getS3SecretAccessKey());  
		storageProfile.setS3DefaultBucket(cfg.getS3DefaultBucket());
	
		return storageProfile; 	
	}
	
	
	public StorageProfile() {		
	}
	
	/**
	 * @return the cachePath
	 */
	public String getPosixCachePath() {
		return posixCachePath;
	}
	/**
	 * @param cachePath the cachePath to set
	 */
	public void setPosixCachePath(String posixCachePath) {
		this.posixCachePath = posixCachePath;
	}
	/**
	 * @return the storageType
	 */
	public StorageType getStorageType() {
		return storageType;
	}
	/**
	 * @param storageType the storageType to set
	 */
	public void setStorageType(StorageType storageType) {
		this.storageType = storageType;
	}
	/**
	 * @return the posixStoragePath
	 */
	public String getPosixStoragePath() {
		return posixStoragePath;
	}
	/**
	 * @param posixStoragePath the posixStoragePath to set
	 */
	public void setPosixStoragePath(String posixStoragePath) {
		this.posixStoragePath = posixStoragePath;
	}  
	

}
