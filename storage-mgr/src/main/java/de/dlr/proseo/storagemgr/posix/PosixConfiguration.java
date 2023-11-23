/**
 * PosixConfiguration.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.posix;

/**
 * Holds the configuration settings required to connect to and interact with an
 * POSIX storage system. It allows for the customization of various aspects such as
 * access credentials, bucket, paths, and behavior-related settings like
 * request attempts and file transfer management.
 *
 * @author Denys Chaykovskiy
 */
public class PosixConfiguration {

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
	
	public void setFileCheckWaitTime(long fileCheckWaitTime) {
		this.fileCheckWaitTime = fileCheckWaitTime;
	}

}