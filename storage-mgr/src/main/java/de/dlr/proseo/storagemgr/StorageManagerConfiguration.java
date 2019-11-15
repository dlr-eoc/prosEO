package de.dlr.proseo.storagemgr;

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
	
	@Value("${proseo.procFacility.name}")
	private String procFacilityName;
	
	@Value("${proseo.procFacility.url}")
	private String procFacilityUrl;
	
	@Value("${proseo.procFacility.descr}")
	private String procFacilityDescr;
	
	@Value("${proseo.s3.s3AccessKey}")
	private String s3AccessKey;
	
	@Value("${proseo.s3.s3SecretAccessKey}")
	private String s3SecretAccessKey;
	
	@Value("${proseo.s3.s3EndPoint}")
	private String s3EndPoint;
	
	@Value("${proseo.alluxio.alluxioUnderFsS3Bucket}")
	private String alluxioUnderFsS3Bucket;
	
	@Value("${proseo.alluxio.alluxioUnderFsS3BucketEndPoint}")
	private String alluxioUnderFsS3BucketEndPoint;
	
	@Value("${proseo.alluxio.alluxioUnderFsS3BucketPrefix}")
	private String alluxioUnderFsS3BucketPrefix;
	
	@Value("${proseo.joborder.bucket}")
	private String joborderBucket;
	
	@Value("${proseo.joborder.prefix}")
	private String joborderPrefix;
	
	@Value("${proseo.alluxio.alluxioUnderFsMaxPrefixes}")
	private int alluxioUnderFsMaxPrefixes;
	
	@Value("${proseo.s3.s3MaxNumberOfBuckets}")
	private int s3MaxNumberOfBuckets;
	
	@Value("${proseo.mountpoints.alluxio.k8sNode.alluxioCache}")
	private String alluxioK8sMountPointCache;
	
	@Value("${proseo.mountpoints.alluxio.k8sNode.alluxioFuse}")
	private String alluxioK8sMountPointFuse;
	
	@Value("${proseo.mountpoints.unregisteredProducts.k8sNode.unregisteredProducts}")
	private String unregisteredProductsK8sMountPoint;

	@Value("${proseo.s3.s3Region}")
	private String s3Region;
	
	@Value("${proseo.global.storageIdPrefix}")
	private String storageIdPrefix;
	
	@Value("${proseo.s3.s3DefaultBucket}")
	private String s3DefaultBucket;
	
	@Value("${proseo.alluxio.alluxioUnderFsDefaultPrefix}")
	private String alluxioUnderFsDefaultPrefix;
	
	
	
	
	
	/**
	 * @return the alluxioUnderFsDefaultPrefix
	 */
	public String getAlluxioUnderFsDefaultPrefix() {
		return alluxioUnderFsDefaultPrefix;
	}

	/**
	 * @param alluxioUnderFsDefaultPrefix the alluxioUnderFsDefaultPrefix to set
	 */
	public void setAlluxioUnderFsDefaultPrefix(String alluxioUnderFsDefaultPrefix) {
		this.alluxioUnderFsDefaultPrefix = alluxioUnderFsDefaultPrefix;
	}

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
	 * @return the storageIdPrefix
	 */
	public String getStorageIdPrefix() {
		return storageIdPrefix;
	}

	/**
	 * @param storageIdPrefix the storageIdPrefix to set
	 */
	public void setStorageIdPrefix(String storageIdPrefix) {
		this.storageIdPrefix = storageIdPrefix;
	}

	/**
	 * @return the s3Region
	 */
	public String getS3Region() {
		return s3Region;
	}

	/**
	 * @param s3Region the s3Region to set
	 */
	public void setS3Region(String s3Region) {
		this.s3Region = s3Region;
	}

	public String getAlluxioK8sMountPointCache() {
		return alluxioK8sMountPointCache;
	}

	public void setAlluxioK8sMountPointCache(String alluxioK8sMountPointCache) {
		this.alluxioK8sMountPointCache = alluxioK8sMountPointCache;
	}

	public String getAlluxioK8sMountPointFuse() {
		return alluxioK8sMountPointFuse;
	}

	public void setAlluxioK8sMountPointFuse(String alluxioK8sMountPointFuse) {
		this.alluxioK8sMountPointFuse = alluxioK8sMountPointFuse;
	}

	public String getUnregisteredProductsK8sMountPoint() {
		return unregisteredProductsK8sMountPoint;
	}

	public void setUnregisteredProductsK8sMountPoint(String unregisteredProductsK8sMountPoint) {
		this.unregisteredProductsK8sMountPoint = unregisteredProductsK8sMountPoint;
	}

	public String getProcFacilityDescr() {
		return procFacilityDescr;
	}

	public void setProcFacilityDescr(String procFacilityDescr) {
		this.procFacilityDescr = procFacilityDescr;
	}

	public String getProcFacilityName() {
		return procFacilityName;
	}

	public void setProcFacilityName(String procFacilityName) {
		this.procFacilityName = procFacilityName;
	}

	public String getProcFacilityUrl() {
		return procFacilityUrl;
	}

	public void setProcFacilityUrl(String procFacilityUrl) {
		this.procFacilityUrl = procFacilityUrl;
	}

	public String getJoborderPrefix() {
		return joborderPrefix;
	}

	public void setJoborderPrefix(String joborderPrefix) {
		this.joborderPrefix = joborderPrefix;
	}

	public String getJoborderBucket() {
		return joborderBucket;
	}

	public void setJoborderBucket(String joborderBucket) {
		this.joborderBucket = joborderBucket;
	}

	/**
	 * @return the alluxioUnderFsMaxPrefixes
	 */
	public int getAlluxioUnderFsMaxPrefixes() {
		return alluxioUnderFsMaxPrefixes;
	}

	/**
	 * @param alluxioUnderFsMaxPrefixes the alluxioUnderFsMaxPrefixes to set
	 */
	public void setAlluxioUnderFsMaxPrefixes(int alluxioUnderFsMaxPrefixes) {
		this.alluxioUnderFsMaxPrefixes = alluxioUnderFsMaxPrefixes;
	}

	/**
	 * @return the alluxioUnderFsS3Bucket
	 */
	public String getAlluxioUnderFsS3Bucket() {
		return alluxioUnderFsS3Bucket;
	}

	/**
	 * @param alluxioUnderFsS3Bucket the alluxioUnderFsS3Bucket to set
	 */
	public void setAlluxioUnderFsS3Bucket(String alluxioUnderFsS3Bucket) {
		this.alluxioUnderFsS3Bucket = alluxioUnderFsS3Bucket;
	}

	/**
	 * @return the alluxioUnderFsS3BucketEndPoint
	 */
	public String getAlluxioUnderFsS3BucketEndPoint() {
		return alluxioUnderFsS3BucketEndPoint;
	}

	/**
	 * @param alluxioUnderFsS3BucketEndPoint the alluxioUnderFsS3BucketEndPoint to set
	 */
	public void setAlluxioUnderFsS3BucketEndPoint(String alluxioUnderFsS3BucketEndPoint) {
		this.alluxioUnderFsS3BucketEndPoint = alluxioUnderFsS3BucketEndPoint;
	}

	/**
	 * @return the alluxioUnderFsS3BucketPrefix
	 */
	public String getAlluxioUnderFsS3BucketPrefix() {
		return alluxioUnderFsS3BucketPrefix;
	}

	/**
	 * @param alluxioUnderFsS3BucketPrefix the alluxioUnderFsS3BucketPrefix to set
	 */
	public void setAlluxioUnderFsS3BucketPrefix(String alluxioUnderFsS3BucketPrefix) {
		this.alluxioUnderFsS3BucketPrefix = alluxioUnderFsS3BucketPrefix;
	}

	/**
	 * @return the s3MaxNumberOfBuckets
	 */
	public int getS3MaxNumberOfBuckets() {
		return s3MaxNumberOfBuckets;
	}

	/**
	 * @param s3MaxNumberOfBuckets the s3MaxNumberOfBuckets to set
	 */
	public void setS3MaxNumberOfBuckets(int s3MaxNumberOfBuckets) {
		this.s3MaxNumberOfBuckets = s3MaxNumberOfBuckets;
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

	/**
	 * @return the s3EndPoint
	 */
	public String getS3EndPoint() {
		return s3EndPoint;
	}

	/**
	 * @param s3EndPoint the s3EndPoint to set
	 */
	public void setS3EndPoint(String s3EndPoint) {
		this.s3EndPoint = s3EndPoint;
	}

	
	
}
