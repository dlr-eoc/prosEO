/**
 *  StorageFile.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.version2.model;

/**
 * Storage File Structure. The file can exist or it can be just an abstract
 * structure.
 * 
 * @author Denys Chaykovskiy
 */
public interface StorageFile {

	/** No bucket is an empty string */
	public final static String NO_BUCKET = "";

	/**
	 * Gets the full path
	 * 
	 * @return the full path
	 */
	public String getFullPath();

	/**
	 * Gets the base path
	 * 
	 * @return the base path
	 */
	public String getBasePath();

	/**
	 * Sets the base path
	 * 
	 * @param basePath base path
	 */
	public void setBasePath(String basePath);

	/**
	 * Gets the bucket
	 * 
	 * @return the bucket
	 */
	public String getBucket();

	/**
	 * Sets the bucket
	 * 
	 * @param bucket the bucket
	 */
	public void setBucket(String bucket);

	/**
	 * Gets relative path
	 * 
	 * @return relative path
	 */
	public String getRelativePath();

	/**
	 * Sets relative path
	 * 
	 * @param relativePath relative path
	 */
	public void setRelativePath(String relativePath);

	/**
	 * Gets file name
	 * 
	 * @return file name
	 */
	public String getFileName();

	/**
	 * Gets storage type
	 * 
	 * @return storage type
	 */
	public StorageType getStorageType();

	/**
	 * Gets extension from file name
	 * 
	 * @return extension
	 */
	public String getExtension();

	/**
	 * Checks if path is a directory path
	 * 
	 * @return true if directory
	 */
	public boolean isDirectory();
}