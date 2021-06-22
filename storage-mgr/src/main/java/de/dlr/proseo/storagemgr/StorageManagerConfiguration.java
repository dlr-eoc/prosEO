package de.dlr.proseo.storagemgr;

import java.util.Arrays;

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
	
	@Value("${proseo.s3.s3AccessKey}")
	private String s3AccessKey;
	
	@Value("${proseo.s3.s3SecretAccessKey}")
	private String s3SecretAccessKey;
	
	@Value("${proseo.s3.s3EndPoint}")
	private String s3EndPoint;
	
	@Value("${proseo.alluxio.alluxioUnderFsS3Bucket}")
	private String alluxioUnderFsS3Bucket;
	
	@Value("${proseo.alluxio.alluxioUnderFsS3BucketPrefix}")
	private String alluxioUnderFsS3BucketPrefix;
	
	@Value("${proseo.joborder.bucket}")
	private String joborderBucket;
	
	@Value("${proseo.joborder.prefix}")
	private String joborderPrefix;
	
	@Value("${proseo.s3.s3MaxNumberOfBuckets}")
	private int s3MaxNumberOfBuckets;

	@Value("${proseo.s3.s3Region}")
	private String s3Region;
	
	@Value("${proseo.global.storageIdPrefix}")
	private String storageIdPrefix;
	
	@Value("${proseo.s3.s3DefaultBucket}")
	private String s3DefaultBucket;

	@Value("${proseo.posix.mountPoint}")
	private String posixMountPoint;

	@Value("${proseo.posix.workerMountPoint}")
	private String posixWorkerMountPoint;
	
	@Value("${proseo.storageManager.defaultStorageType}")
	private String defaultStorageType;
	
	/** Shared secret for Storage Manager download tokens */
	@Value("${proseo.storageManager.secret}")
	private String storageManagerSecret;
	
	
	// Alluxio configuration values currently not in use
	
//	@Value("${proseo.alluxio.alluxioUnderFsDefaultPrefix}")
//	private String alluxioUnderFsDefaultPrefix;
//	
//	@Value("${proseo.alluxio.alluxioUnderFsMaxPrefixes}")
//	private int alluxioUnderFsMaxPrefixes;
//	
//	@Value("${proseo.alluxio.alluxioUnderFsS3BucketEndPoint}")
//	private String alluxioUnderFsS3BucketEndPoint;
//	
//	@Value("${proseo.mountpoints.alluxio.k8sNode.alluxioCache}")
//	private String alluxioK8sMountPointCache;
//	
//	@Value("${proseo.mountpoints.alluxio.k8sNode.alluxioFuse}")
//	private String alluxioK8sMountPointFuse;
	
	
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
	 * @return the posixWorkerMountPoint
	 */
	public String getPosixWorkerMountPoint() {
		return posixWorkerMountPoint;
	}

	/**
	 * @return the posixMountPoint
	 */
	public String getPosixMountPoint() {
		return posixMountPoint;
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

}
