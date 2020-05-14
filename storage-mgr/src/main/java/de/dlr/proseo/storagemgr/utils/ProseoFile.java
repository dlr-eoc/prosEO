package de.dlr.proseo.storagemgr.utils;

import java.io.InputStream;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.FsType;

public abstract class ProseoFile {
	
	private static Logger logger = LoggerFactory.getLogger(ProseoFileS3.class);
	
	// original path
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

	// path below bucket
	protected String relPath;
	
	// bucket
	protected String basePath;

	protected StorageManagerConfiguration cfg;

	protected void buildFileName() {
		fileName = "";	
		if (relPath != null) {
			if (!relPath.endsWith("/")) {
				int i = relPath.lastIndexOf('/');
				if (i > 0) {
					fileName = relPath.substring(i + 1, relPath.length());
					relPath = relPath.substring(0, i);
				}
			}
		}
	}
	public String getRelPathAndFile() {
		if (relPath.endsWith("/")) {
			return relPath + fileName;
		} else {
			return relPath + "/" + fileName;
		}
	}
	
	public Boolean isDirectory() {
		return (fileName == null) || fileName.isEmpty();
	}
	
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
	
	public abstract FsType getFsType();
	
	public abstract String getFullPath();

	public abstract InputStream getDataAsInputStream();
	
	public abstract Boolean writeBytes(byte[] bytes) throws Exception;
	
	public abstract ArrayList<String> copyTo(ProseoFile proFile, Boolean recursive) throws Exception;
}
