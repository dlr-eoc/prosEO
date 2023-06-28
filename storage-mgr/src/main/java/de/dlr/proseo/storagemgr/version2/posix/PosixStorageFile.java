/**
 * PosixStorageFile.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.version2.posix;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

/**
 * Represents a file within a POSIX-based storage system. It implements the
 * StorageFile interface and provides functionality related to the file's
 * properties and operations.
 * 
 * Full path: "/" + basePath + bucket (optional) + relativePath (with fileName)
 * 
 * @author Denys Chaykovskiy
 */
public class PosixStorageFile implements StorageFile {

	/** Base path */
	private String basePath;

	/** Bucket */
	private String bucket;

	/** Relative path */
	private String relativePath;

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(PosixStorageFile.class);

	/**
	 * No Bucket Constructor
	 * 
	 * @param basePath     base path
	 * @param relativePath relative path
	 */
	public PosixStorageFile(String basePath, String relativePath) {
		this(basePath, StorageFile.NO_BUCKET, relativePath);
	}

	/**
	 * Constructor with bucket
	 * 
	 * @param basePath     base path
	 * @param bucket       bucket
	 * @param relativePath relative path
	 */
	public PosixStorageFile(String basePath, String bucket, String relativePath) {

		this.basePath = basePath;
		this.bucket = bucket;
		this.relativePath = relativePath;
	}

	/**
	 * Copy Constructor
	 * 
	 * @param storageFile Storage file
	 */
	public PosixStorageFile(StorageFile storageFile) {
		this(storageFile.getBasePath(), storageFile.getBucket(), storageFile.getRelativePath());
	}

	/**
	 * Gets the full path
	 * 
	 * @return the full path
	 */
	@Override
	public String getFullPath() {

		try {
			return new PathConverter(basePath, bucket, relativePath).fixAbsolutePath().getPath();
		} catch (Exception e) {

			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Gets the base path
	 * 
	 * @return the base path
	 */
	@Override
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Sets the base path
	 * 
	 * @param basePath base path
	 */
	@Override
	public void setBasePath(String basePath) {
		this.basePath = basePath;
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
		return StorageType.POSIX;
	}

	/**
	 * Gets extension from file name
	 * 
	 * @return extension
	 */
	@Override
	public String getExtension() {

		if (logger.isTraceEnabled())
			logger.trace(">>> getExtension()");

		return FilenameUtils.getExtension(relativePath);
	}

	/**
	 * Checks if path is a directory path
	 * 
	 * @return true if directory
	 */
	@Override
	public boolean isDirectory() {

		return (relativePath.endsWith("/") || relativePath.endsWith("\\")) ? true : false;
	}
}