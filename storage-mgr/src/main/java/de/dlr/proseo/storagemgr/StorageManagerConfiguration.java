package de.dlr.proseo.storagemgr;

import java.io.File;
import java.util.Arrays;

import javax.annotation.PostConstruct;

/**
 * StorageManagerConfiguration.java
 * 
 * (C) 2019 DLR
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Ingestor component
 * 
 * @author Hubert Asamer
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class StorageManagerConfiguration {
	
	@Value("${proseo.global.storageIdPrefix}")
	private String storageIdPrefix;
	
	@Value("${proseo.s3.s3AccessKey}")
	private String s3AccessKey;
	
	@Value("${proseo.s3.s3SecretAccessKey}")
	private String s3SecretAccessKey;
	
	@Value("${proseo.s3.s3EndPoint}")
	private String s3EndPoint;
	
	@Value("${proseo.s3.s3Region}")
	private String s3Region;
	
	@Value("${proseo.s3.s3MaxNumberOfBuckets}")
	private int s3MaxNumberOfBuckets;

	@Value("${proseo.s3.s3DefaultBucket}")
	private String s3DefaultBucket;

	// Obsolete
	@Value("${proseo.alluxio.alluxioUnderFsS3Bucket}")
	private String alluxioUnderFsS3Bucket;
	
	// Obsolete
	@Value("${proseo.alluxio.alluxioUnderFsS3BucketPrefix}")
	private String alluxioUnderFsS3BucketPrefix;
	
	/** Mount point for backend storage (must be different from cachePath) */
	@Value("${proseo.posix.backendPath}")
	private String posixBackendPath;

	/** Mount point for file cache */
	@Value("${proseo.posix.cachePath}")
	private String posixCachePath;
	
	@Value("${proseo.joborder.bucket}")
	private String joborderBucket;
	
	@Value("${proseo.joborder.prefix}")
	private String joborderPrefix;
	
	/** Default type for backend storage */
	@Value("${proseo.storageManager.defaultStorageType}")
	private String defaultStorageType;

	/** Maximum cycles for file size check */
	@Value("${proseo.storageManager.filecheck.maxcycles}")
	private Long fileCheckMaxCycles;
	
	/** Wait time for file size check cycle in milliseconds */
	@Value("${proseo.storageManager.filecheck.waittime}")
	private Long fileCheckWaitTime;
	
	/** Shared secret for Storage Manager download tokens */
	@Value("${proseo.storageManager.secret}")
	private String storageManagerSecret;
	
	/** Recommended minimum cache usage for efficient operation (percentage of file system size) */
	@Value("${proseo.storageManager.cache.expectedUsage}")
	private Integer expectedCacheUsage;
	
	/** Maximum cache usage (percentage of file system size) */
	@Value("${proseo.storageManager.cache.maximumUsage}")
	private Integer maximumCacheUsage;

	/** Singleton object */
	private static StorageManagerConfiguration theConfiguration = null;
	
	/**
	 * Sets the singleton object for this class
	 */
	@PostConstruct
	private void init() {
		theConfiguration = this;
	}
	
	/**
	 * Gets the singleton object for this class
	 * 
	 * @return the singleton StorageManagerConfiguration
	 */
	public static StorageManagerConfiguration getConfiguration() {
		return theConfiguration;
	}
	
	
	/**
	 * @return the defaultStorageType
	 */
	public String getDefaultStorageType() {
		return defaultStorageType;
	}

	/**
	 * Gets the shared secret for generating Storage Manager download tokens as 256-bit byte array
	 * 
	 * @return the Storage Manager secret
	 */
	public byte[] getStorageManagerSecret() {
		byte[] sharedSecret = Arrays.copyOf(
				(storageManagerSecret + "                ").getBytes(),
				32);
		return sharedSecret;
	}

    /**
	 * @return the fileCheckMaxCycles
	 */
	public Long getFileCheckMaxCycles() {
		return fileCheckMaxCycles;
	}

	/**
	 * @return the fileCheckWaitTime
	 */
	public Long getFileCheckWaitTime() {
		return fileCheckWaitTime;
	}

	/**
	 * Gets the absolute path to the POSIX file cache
	 * 
	 * @return the POSIX cache path
	 */
	public String getPosixCachePath() {
		return new File(posixCachePath).getAbsolutePath();
	}

	/**
	 * Gets the absolute path to the POSIX backend storage (if used)
	 * 
	 * @return the POSIX backend storage path
	 */
	public String getPosixBackendPath() {
		return new File(posixBackendPath).getAbsolutePath();
	}

	/**
	 * @return the alluxioUnderFsDefaultPrefix
	 */
//	public String getAlluxioUnderFsDefaultPrefix() {
//		return alluxioUnderFsDefaultPrefix;
//	}

	/**
	 * @return the s3DefaultBucket
	 */
	public String getS3DefaultBucket() {
		return s3DefaultBucket;
	}

	/**
	 * @return the storageIdPrefix
	 */
	public String getStorageIdPrefix() {
		return storageIdPrefix;
	}

	/**
	 * @return the s3Region
	 */
	public String getS3Region() {
		return s3Region;
	}

//	public String getAlluxioK8sMountPointCache() {
//		return alluxioK8sMountPointCache;
//	}
//
//	public String getAlluxioK8sMountPointFuse() {
//		return alluxioK8sMountPointFuse;
//	}

	/**
	 * @return the joborderPrefix
	 */
	public String getJoborderPrefix() {
		return joborderPrefix;
	}
	
	
	/**
	 * @return the joborderBucket
	 */
	public String getJoborderBucket() {
		return joborderBucket;
	}

	/**
	 * @return the alluxioUnderFsMaxPrefixes
	 */
//	public int getAlluxioUnderFsMaxPrefixes() {
//		return alluxioUnderFsMaxPrefixes;
//	}

	/**
	 * @return the alluxioUnderFsS3Bucket
	 */
	public String getAlluxioUnderFsS3Bucket() {
		return alluxioUnderFsS3Bucket;
	}

	/**
	 * @return the alluxioUnderFsS3BucketEndPoint
	 */
//	public String getAlluxioUnderFsS3BucketEndPoint() {
//		return alluxioUnderFsS3BucketEndPoint;
//	}

	/**
	 * @return the alluxioUnderFsS3BucketPrefix
	 */
	public String getAlluxioUnderFsS3BucketPrefix() {
		return alluxioUnderFsS3BucketPrefix;
	}

	/**
	 * @return the s3MaxNumberOfBuckets
	 */
	public int getS3MaxNumberOfBuckets() {
		return s3MaxNumberOfBuckets;
	}

	/**
	 * @return the s3AccessKey
	 */
	public String getS3AccessKey() {
		return s3AccessKey;
	}

	/**
	 * @return the s3SecretAccessKey
	 */
	public String getS3SecretAccessKey() {
		return s3SecretAccessKey;
	}

	/**
	 * @return the s3EndPoint
	 */
	public String getS3EndPoint() {
		return s3EndPoint;
	}
	
	/**
	 * @return the expected cache usage
	 */
	public Integer getExpectedCacheUsage() {
		return expectedCacheUsage;
	}

	/**
	 * @return the maximum cache usage
	 */
	public Integer getMaximumCacheUsage() {
		return maximumCacheUsage;
	}

}
