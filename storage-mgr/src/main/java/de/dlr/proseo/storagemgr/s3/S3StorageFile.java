/**
 * S3StorageFile.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.s3;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.utils.PathConverter;

/**
 * Represents a file within a S3-based storage system. It implements the
 * StorageFile interface and provides functionality related to the file's
 * properties and operations.
 *
 * Full path: "s3:/" + bucket + relativePath (with fileName)
 *
 * @author Denys Chaykovskiy
 */
public class S3StorageFile implements StorageFile {

	/** s3 prefix */
	private static final String S3PREFIX = "s3:/";

	/** slash */
	private static final String SLASH = "/";

	/** bucket */
	private String bucket;

	/** relative path */
	private String relativePath;

	/**
	 * Constructor
	 *
	 * @param bucket       bucket
	 * @param relativePath relative path
	 */
	public S3StorageFile(String bucket, String relativePath) {

		this.bucket = bucket;
		this.relativePath = relativePath;
	}

	/**
	 * Copy Constructor
	 *
	 * @param storageFile Storage file
	 */
	public S3StorageFile(StorageFile storageFile) {
		this(storageFile.getBucket(), storageFile.getRelativePath());
	}

	/**
	 * Gets the full path
	 *
	 * @return the full path
	 */
	@Override
	public String getFullPath() {
		String path = Paths.get(bucket, relativePath).toString();
		return addS3Prefix(new PathConverter(path).convertToSlash().getPath());
	}

	/**
	 * Gets the base path
	 *
	 * @return the base path
	 */
	@Override
	public String getBasePath() {
		return ""; // no base path in s3
	}

	/**
	 * Sets the base path
	 *
	 * @param basePath base path
	 */
	@Override
	public void setBasePath(String basePath) {
	}

	/**
	 * Gets the bucket
	 *
	 * @return the bucket
	 */
	@Override
	public String getBucket() {
		return bucket;
	}

	/**
	 * Sets the bucket
	 *
	 * @param bucket the bucket
	 */
	@Override
	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	/**
	 * Gets relative path
	 *
	 * @return relative path
	 */
	@Override
	public String getRelativePath() {
		return relativePath;
	}

	/**
	 * Sets relative path
	 *
	 * @param relativePath relative path
	 */
	@Override
	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	/**
	 * Gets file name
	 *
	 * @return file name
	 */
	@Override
	public String getFileName() {
		return new File(relativePath).getName();
	}

	/**
	 * Gets storage type
	 *
	 * @return storage type
	 */
	@Override
	public StorageType getStorageType() {
		return StorageType.S3;
	}

	/**
	 * Gets extension from file name
	 *
	 * @return extension
	 */
	@Override
	public String getExtension() {
		return FilenameUtils.getExtension(relativePath);
	}

	/**
	 * Checks if path is a directory path
	 *
	 * @return true if directory
	 */
	@Override
	public boolean isDirectory() {

		return relativePath.endsWith(SLASH) ? true : false;
	}

	/**
	 * Adds s3 prefix to the path
	 *
	 * @param path path
	 * @return s3 prefix + path
	 */
	private String addS3Prefix(String path) {
		return path.startsWith(SLASH) ? S3PREFIX + path : S3PREFIX + SLASH + path;
	}

	@Override
	public String toString() {
		return "S3StorageFile [bucket=" + bucket + ", relativePath=" + relativePath + "]";
	}
}