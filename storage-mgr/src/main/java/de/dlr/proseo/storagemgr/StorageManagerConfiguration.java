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
	
	@Value("${proseo.s3.s3AccessKey}")
	private String s3AccessKey;
	
	@Value("${proseo.s3.s3SecretAccessKey}")
	private String s3SecretAccessKey;
	
	@Value("${proseo.s3.s3EndPoint}")
	private String s3EndPoint;
	
	@Value("${proseo.s3.s3JoborderPrefixKey}")
	private String s3JoborderPrefixKey;
	
	@Value("${proseo.alluxio.alluxioUnderFsS3Bucket}")
	private String alluxioUnderFsS3Bucket;
	
	@Value("${proseo.alluxio.alluxioUnderFsS3BucketEndPoint}")
	private String alluxioUnderFsS3BucketEndPoint;
	
	@Value("${proseo.alluxio.alluxioUnderFsS3BucketPrefix}")
	private String alluxioUnderFsS3BucketPrefix;
	
	@Value("${proseo.alluxio.alluxioUnderFsMaxPrefixes}")
	private int alluxioUnderFsMaxPrefixes;
	
	@Value("${proseo.s3.s3MaxNumberOfBuckets}")
	private int s3MaxNumberOfBuckets;
	
	


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
	 * @return the s3JoborderPrefixKey
	 */
	public String getS3JoborderPrefixKey() {
		return s3JoborderPrefixKey;
	}

	/**
	 * @param s3JoborderPrefixKey the s3JoborderPrefixKey to set
	 */
	public void setS3JoborderPrefixKey(String s3JoborderPrefixKey) {
		this.s3JoborderPrefixKey = s3JoborderPrefixKey;
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
