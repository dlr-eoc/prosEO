package de.dlr.proseo.storagemgr.utils;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;

/**
 * Abstract definition of proseo file objects
 * 
 * The file definition is based on:
 * File system type, e.g. S3, POSIX
 * Base path, e.g. S3 bucket, POSIX mount point
 * Relative path and file name
 * 
 * Example for full path:
 * S3: "s3://bucket/somewhere/file.name"
 * POSIX: "/mntPoint/somewhere/file.name" * 
 * 
 * Directories are represented by a "/" as last character
 * 
 * @author melchinger
 *
 */
public abstract class ProseoFile {
	
	/**
	 * Original path
	 */
	protected String pathInfo;
	
	/**
	 * File name 
	 */
	protected String fileName;
	
	/**
	 * File path relative to bucket
	 */
	protected String relPath;
	
	/**
	 * Bucket, in which the file resides
	 */
	protected String basePath;

	/**
	 * Storage manager configuration used to get easily access to default settings.
	 */
	protected StorageManagerConfiguration cfg;

	/**
	 * Logger for this class
	 */
	private static Logger logger = LoggerFactory.getLogger(ProseoFile.class);
	
	/**
	 * Gets the file path information
	 * @return the path information
	 */
	public String getPathInfo() {
		return pathInfo;
	}

	/**
	 * Sets the file path information
	 * @param pathInfo the pathInfo to set
	 */
	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	/**
	 * Gets the file name 
	 * @return the file name
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Sets the file name 
	 * @param fileName the file name to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Gets the file path relative to the bucket
	 * @return the relative path
	 */
	public String getRelPath() {
		return relPath;
	}

	/**
	 * Sets the file path relative to the bucket
	 * @param relPath the relative path to set
	 */
	public void setRelPath(String relPath) {
		this.relPath = relPath;
	}

	/**
	 * Gets the path to the bucket
	 * @return the base path
	 */
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Sets the path to the bucket
	 * @param basePath the base path to set
	 */
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	/**
	 * Set file name from relative path and remove it from the relative path
	 * (if the relative path does not end on '/')
	 */
	protected void buildFileName() {
		
		if (logger.isTraceEnabled()) logger.trace(">>> buildFileName()");
		
		fileName = "";	
		if (relPath != null) {
			if (!relPath.endsWith("/")) {
				int i = relPath.lastIndexOf('/');
				if (i > 0) {
					fileName = relPath.substring(i + 1, relPath.length());
					relPath = relPath.substring(0, i);
				} else {
					fileName = relPath;
					relPath = "";
				}
			}
		}
	}
	
	/**
	 * Gets the complete file path relative to the bucket
	 * @return Relative path + file name
	 */
	public String getRelPathAndFile() {
		
		if (logger.isTraceEnabled()) logger.trace(">>> getRelPathAndFile()");

		if (relPath.endsWith("/") || relPath.isEmpty()) {
			return relPath + fileName;
		} else {
			return relPath + "/" + fileName;
		}
	}

	/**
	 * Checks whether the object is a directory
	 * @return true if object represents a directory, false otherwise
	 */
	public Boolean isDirectory() {
		return (fileName == null) || fileName.isEmpty();
	}
	
	/**
	 * Gets the file name extension
	 * @return Extension of file name or an empty string, if the file name has no extension
	 */
	public String getExtension() {
		
		if (logger.isTraceEnabled()) logger.trace(">>> getExtension()");

		if ((fileName == null) || fileName.isEmpty()) {
			return "";
		} else {
			return FilenameUtils.getExtension(fileName);
		}
	}
	
	/**
	 * Create a file object out of full path info. 
	 * 
	 * @param pathInfo Full path
	 * @param cfg a pointer to the Storage Manager configuration
	 * @return The new file object or null, if the operation failed
	 */
	public static ProseoFile fromPathInfo(String pathInfo, StorageManagerConfiguration cfg) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> fromPathInfo({}, {})", pathInfo, cfg);

		if (pathInfo == null) {
			logger.warn("pathInfo not set");
		} else {
			String aPath = pathInfo.trim();
			// Find storage type
			if (aPath.startsWith("s3:") || aPath.startsWith("S3:")) {
				return new ProseoFileS3(aPath, true, cfg);
			} else if (aPath.startsWith("alluxio:")) {
				return new ProseoFileAlluxio(aPath, true, cfg);
			} else {
				if (!aPath.startsWith("/")) {
					aPath = Paths.get(aPath).toAbsolutePath().toString();
				}
				return new ProseoFilePosix(aPath, true, cfg);
			}
			//logger.warn("Unknown FS type in path: {}", pathInfo);
		}
		return null;
	}
	
	/**
	 * Create file object of aType with relative path pathInfo
	 * 
	 * @param aType StorageType
	 * @param pathInfo Relative path with bucket
	 * @param cfg a pointer to the Storage Manager configuration
	 * @return The new file object or null, if the operation failed
	 */
	public static ProseoFile fromType(StorageType aType, String pathInfo, StorageManagerConfiguration cfg) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> fromType({}, {}, {})", aType, pathInfo, cfg);
		
		if (pathInfo != null) {
			String aPath = pathInfo.trim();
			// Find storage type
			switch (aType) {
			case S3:
				return new ProseoFileS3(aPath, false, cfg);
			case ALLUXIO:
				return new ProseoFileAlluxio(aPath, false, cfg);
			case POSIX:
				return new ProseoFilePosix(aPath, false, cfg);
			case OTHER:
			default:
				logger.warn("Unknown FS type in path: {}", pathInfo);
				return null;
			}
		}
		logger.warn("pathInfo not set");
		return null;
	}

	/**
	 * Create file object of aType with full path pathInfo
	 * 
	 * @param aType StorageType
	 * @param pathInfo Full path (with type info)
	 * @param cfg a pointer to the Storage Manager configuration
	 * @return The new file object or null, if the operation failed
	 */
	public static ProseoFile fromTypeFullPath(StorageType aType, String pathInfo, StorageManagerConfiguration cfg) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> fromTypeFullPath({}, {}, {})", aType, pathInfo, cfg);
		
		if (pathInfo != null) {
			String aPath = pathInfo.trim();
			// Find storage type
			switch (aType) {
			case S3:
				return new ProseoFileS3(aPath, true, cfg);
			case ALLUXIO:
				return new ProseoFileAlluxio(aPath, true, cfg);
			case POSIX:
				return new ProseoFilePosix(aPath, true, cfg);
			case OTHER:
			default:
				logger.warn("Unknown FS type in path: {}", pathInfo);
				return null;
			}
		}
		logger.warn("pathInfo not set");
		return null;
	}
	
	/**
	 * Create file object of aType with bucket and relative path.
	 * 
	 * @param aType StorageType
	 * @param bucket Bucket 
	 * @param pathInfo Relative path
	 * @param cfg a pointer to the Storage Manager configuration
	 * @return the new file object or null, if the operation failed
	 */
	public static ProseoFile fromTypeAndBucket(StorageType aType, String bucket, String pathInfo, StorageManagerConfiguration cfg) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> fromTypeAndBucket({}, {}, {})", aType, bucket, pathInfo, cfg);
		
		if (pathInfo != null) {
			String aPath = pathInfo.trim();
			// Find storage type
			switch (aType) {
			case S3:
				return new ProseoFileS3(bucket, aPath, cfg);
			case ALLUXIO:
				return new ProseoFileAlluxio(bucket, aPath, cfg);
			case POSIX:
				return new ProseoFilePosix(bucket, aPath, cfg);
			case OTHER:
			default:
				logger.warn("Unknown FS type in path: {}", pathInfo);
				return null;
			}
		}
		logger.warn("pathInfo not set");
		return null;
	}

	// The methods below must be implemented by the derived prosEO file classes
	
	/**
	 * Gets the file system type
	 * @return The file system type
	 */
	public abstract StorageType getFsType();
	
	/**
	 * Gets the file system resource definition
	 * @return the file system resource definition
	 */
	public abstract FileSystemResource getFileSystemResource();
	
	/**
	 * Delete file object recursively.
	 * 
	 * @return list of deleted object paths.
	 */
	public abstract ArrayList<String> delete();
	
	/**
	 * List objects recursively.
	 * 
	 * @return list of file objects
	 */
	public abstract ArrayList<ProseoFile> list();
	
	/**
	 * Gets the full file path
	 * @return The full path including type
	 */
	public abstract String getFullPath();

	/**
	 * Gets the file content as input stream
	 * @return the file content (data)
	 */
	public abstract InputStream getDataAsInputStream();
	
	/**
	 * Write binary byte array to file object.
	 * 
	 * @param bytes Byte array
	 * @return true after success
	 * @throws Exception if an error occurs in any lower-level library
	 */
	public abstract Boolean writeBytes(byte[] bytes) throws Exception;
	
	/**
	 * Copy this object to target proFile
	 * 
	 * @param proFile Target file object
	 * @param recursive Copy recursively if true
	 * @return List of copied target file names
	 * @throws Exception if an error occurs in any lower-level library
	 */
	public abstract ArrayList<String> copyTo(ProseoFile proFile, Boolean recursive) throws Exception;
	
	/**
	 * Gets the object size
	 * @return Length of file object
	 */
	public abstract long getLength();
}
