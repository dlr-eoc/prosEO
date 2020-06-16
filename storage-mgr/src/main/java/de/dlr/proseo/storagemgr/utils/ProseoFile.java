package de.dlr.proseo.storagemgr.utils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.FsType;

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
	 * Logger for this class
	 */
	private static Logger logger = LoggerFactory.getLogger(ProseoFileS3.class);
	
	/**
	 * original path
	 */
	protected String pathInfo;
	
	/**
	 * @return the pathInfo
	 */
	public String getPathInfo() {
		return pathInfo;
	}

	/**
	 * @return the relPath
	 */
	public String getRelPath() {
		return relPath;
	}

	/**
	 * @return the basePath
	 */
	public String getBasePath() {
		return basePath;
	}

	/**
	 * @param pathInfo the pathInfo to set
	 */
	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	/**
	 * @param relPath the relPath to set
	 */
	public void setRelPath(String relPath) {
		this.relPath = relPath;
	}

	/**
	 * @param basePath the basePath to set
	 */
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	// file name
	protected String fileName;
	
	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * path below bucket
	 */
	protected String relPath;
	
	/**
	 * Bucket
	 */
	protected String basePath;

	/**
	 * Storage manager configuration used to get easily access to default settings.
	 */
	protected StorageManagerConfiguration cfg;

	/**
	 * Extract file name from relPath
	 */
	protected void buildFileName() {
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
	 * @return Relative path + file name
	 */
	public String getRelPathAndFile() {
		if (relPath.endsWith("/") || relPath.isEmpty()) {
			return relPath + fileName;
		} else {
			return relPath + "/" + fileName;
		}
	}

	/**
	 * @return true if object represents a directory
	 */
	public Boolean isDirectory() {
		return (fileName == null) || fileName.isEmpty();
	}
	
	/**
	 * @return Extension of file name 
	 */
	public String getExtension() {
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
	 * @param cfg
	 * @return The new file object
	 */
	public static ProseoFile fromPathInfo(String pathInfo, StorageManagerConfiguration cfg) {
		if (pathInfo != null) {
			String aPath = pathInfo.trim();
			// Find storage type
			if (aPath.startsWith("s3:") || aPath.startsWith("S3:")) {
				return new ProseoFileS3(aPath, true, cfg);
			} else if (aPath.startsWith("alluxio:")) {
				return new ProseoFileAlluxio(aPath, true, cfg);
			} else if (aPath.startsWith("/")) {
				return new ProseoFilePosix(aPath, true, cfg);
			}
			logger.warn("Unknown FS type in path: {}", pathInfo);
		}
		logger.warn("pathInfo not set");
		return null;
	}
	
	/**
	 * Create file object of aType with relative path pathInfo
	 * 
	 * @param aType FsType
	 * @param pathInfo Relative path with bucket
	 * @param cfg
	 * @return The new file object
	 */
	public static ProseoFile fromType(FsType aType, String pathInfo, StorageManagerConfiguration cfg) {
		if (pathInfo != null) {
			String aPath = pathInfo.trim();
			// Find storage type
			switch (aType) {
			case S_3:
				return new ProseoFileS3(aPath, false, cfg);
			case ALLUXIO:
				return new ProseoFileAlluxio(aPath, false, cfg);
			case POSIX:
				return new ProseoFilePosix(aPath, false, cfg);
			}
			logger.warn("Unknown FS type in path: {}", pathInfo);
		}
		logger.warn("pathInfo not set");
		return null;
	}

	/**
	 * Create file object of aType with full path pathInfo
	 * 
	 * @param aType FsType
	 * @param pathInfo Full path (with type info)
	 * @param cfg
	 * @return The new file object
	 */
	public static ProseoFile fromTypeFullPath(FsType aType, String pathInfo, StorageManagerConfiguration cfg) {
		if (pathInfo != null) {
			String aPath = pathInfo.trim();
			// Find storage type
			switch (aType) {
			case S_3:
				return new ProseoFileS3(aPath, true, cfg);
			case ALLUXIO:
				return new ProseoFileAlluxio(aPath, true, cfg);
			case POSIX:
				return new ProseoFilePosix(aPath, true, cfg);
			}
			logger.warn("Unknown FS type in path: {}", pathInfo);
		}
		logger.warn("pathInfo not set");
		return null;
	}
	
	/**
	 * Create file object of aType with bucket and relative path.
	 * 
	 * @param aType FsType
	 * @param bucket Bucket 
	 * @param pathInfo Relative path
	 * @param cfg
	 * @return
	 */
	public static ProseoFile fromTypeAndBucket(FsType aType, String bucket, String pathInfo, StorageManagerConfiguration cfg) {
		if (pathInfo != null) {
			String aPath = pathInfo.trim();
			// Find storage type
			switch (aType) {
			case S_3:
				return new ProseoFileS3(bucket, aPath, cfg);
			case ALLUXIO:
				return new ProseoFileAlluxio(bucket, aPath, cfg);
			case POSIX:
				return new ProseoFilePosix(bucket, aPath, cfg);
			}
			logger.warn("Unknown FS type in path: {}", pathInfo);
		}
		logger.warn("pathInfo not set");
		return null;
	}

	/**
	 * @return The file system type
	 */
	public abstract FsType getFsType();
	
	/**
	 * @return Get the file system resource definition
	 */
	public abstract FileSystemResource getFileSystemResource();
	
	/**
	 * Delete file object recursively.
	 * 
	 * @return String list of deleted object paths.
	 */
	public abstract ArrayList<String> delete();
	
	/**
	 * List objects recursively.
	 * 
	 * @return List of file objects
	 */
	public abstract ArrayList<ProseoFile> list();
	
	/**
	 * @return The full path including type
	 */
	public abstract String getFullPath();

	/**
	 * @return Input stream on file object
	 */
	public abstract InputStream getDataAsInputStream();
	
	/**
	 * Write binary byte array to file object.
	 * 
	 * @param bytes Byte array
	 * @return true after success
	 */
	public abstract Boolean writeBytes(byte[] bytes) throws Exception;
	
	/**
	 * Copy this object to target proFile
	 * 
	 * @param proFile Target file object
	 * @param recursive Copy recursively if true
	 * @return List of copied target file names
	 */
	public abstract ArrayList<String> copyTo(ProseoFile proFile, Boolean recursive) throws Exception;
	
	/**
	 * @return Length of file object
	 */
	public abstract long getLength();
}
