/**
 * S3Configuration.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.version2.s3;

/**
 * Holds the configuration settings required to connect to and interact with an
 * S3 storage system. It allows for the customization of various aspects such as
 * access credentials, region, bucket, paths, and behavior-related settings like
 * request attempts and file transfer management.
 *
 * @author Denys Chaykovskiy
 */
public class S3Configuration {

	/** s3 access key */
	private String s3AccessKey;

	/** s3 secret access key */
	private String s3SecretAccessKey;

	/** s3 region */
	private String s3Region;

	/** s3 end point */
	private String s3EndPoint;

	/** Bucket */
	private String bucket;

	/** base path */
	private String basePath;

	/** source path */
	private String sourcePath;

	/** max request attempts */
	private int maxRequestAttempts;

	/** wait time */
	private long fileCheckWaitTime;

	/** default region */
	private boolean defaultEndPoint;

	public boolean isDefaultEndPoint() {
		return defaultEndPoint;
	}

	public void setDefaultEndPoint(boolean defaultEndPoint) {
		this.defaultEndPoint = defaultEndPoint;
	}

	public boolean isFileTransferManager() {
		return fileTransferManager;
	}

	public void setFileTransferManager(boolean fileTransferManager) {
		this.fileTransferManager = fileTransferManager;
	}

	public void setFileCheckWaitTime(long fileCheckWaitTime) {
		this.fileCheckWaitTime = fileCheckWaitTime;
	}

	/** File transferManager */
	private boolean fileTransferManager;

	public String getS3AccessKey() {
		return s3AccessKey;
	}

	public void setS3AccessKey(String s3AccessKey) {
		this.s3AccessKey = s3AccessKey;
	}

	public String getS3SecretAccessKey() {
		return s3SecretAccessKey;
	}

	public void setS3SecretAccessKey(String s3SecretAccessKey) {
		this.s3SecretAccessKey = s3SecretAccessKey;
	}

	public String getS3Region() {
		return s3Region;
	}

	public void setS3Region(String s3Region) {
		this.s3Region = s3Region;
	}

	public String getS3EndPoint() {
		return s3EndPoint;
	}

	public void setS3EndPoint(String s3EndPoint) {
		this.s3EndPoint = s3EndPoint;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public int getMaxRequestAttempts() {
		return maxRequestAttempts;
	}

	public void setMaxRequestAttempts(int maxRequestAttempts) {
		this.maxRequestAttempts = maxRequestAttempts;
	}

	public long getFileCheckWaitTime() {
		return fileCheckWaitTime;
	}

	public void setFileCheckWaitTime(Long fileCheckWaitTime) {
		this.fileCheckWaitTime = fileCheckWaitTime;
	}
}