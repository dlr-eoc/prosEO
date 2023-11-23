/**
 * PosixStorage.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.posix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.StorageMgrMessage;
import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.utils.FileUtils;
import de.dlr.proseo.storagemgr.utils.PathConverter;

/**
 * An implementation of the Storage interface for a POSIX-based file system,
 * providing functionality to interact with the storage system, that is
 * retrieving information about the storage, performing file operations, and
 * path conversion.
 * 
 * This class assumes a no-bucket concept for POSIX storage.
 * 
 * @author Denys Chaykovskiy
 */
public class PosixStorage implements Storage {

	/** Bucket */
	private String bucket;
	
	/** POSIX data access layer object */
	private PosixDAL posixDAL;
	
	/** POSIX configuration */
	private PosixConfiguration cfg;

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(PosixStorage.class);

	/**
	 * No bucket constructor initializing the storage with a basePath and
	 * sourcePath. It creates the necessary directories if they don't exist.
	 * 
	 * @param cfg POSIX configuration
	 */
	public PosixStorage(PosixConfiguration cfg) {
		this.bucket = cfg.getBucket();  // StorageFile.NO_BUCKET is used in prosEO in POSIX Storage;

		new FileUtils(cfg.getBasePath()).createDirectories();
		new FileUtils(cfg.getSourcePath()).createDirectories();
		
		this.cfg = cfg; 
		posixDAL = new PosixDAL(cfg);
	}

	/**
	 * Gets the storage type.
	 * 
	 * @return the storage type
	 */
	@Override
	public StorageType getStorageType() {
		return StorageType.POSIX;
	}

	/**
	 * Gets the base path.
	 * 
	 * @return the base path
	 */
	@Override
	public String getBasePath() {
		return cfg.getBasePath();
	}

	/**
	 * Gets the absolute base path (file system prefix + bucket + base path).
	 * 
	 * In this no-bucket POSIX version, it returns the base path.
	 * 
	 * @return the absolute base path
	 */
	@Override
	public String getAbsoluteBasePath() {
		return new PathConverter(getBasePath()).getPath();
	}

	/**
	 * Gets the source path.
	 * 
	 * @return the source path
	 */
	@Override
	public String getSourcePath() {
		return cfg.getSourcePath();
	}

	/**
	 * Sets the bucket.
	 * 
	 * @param bucket the bucket to set
	 */
	@Override
	public void setBucket(String bucket) {
		this.bucket = bucket;

		String bucketPath = Paths.get(cfg.getBasePath(), bucket).toString();
		new FileUtils(bucketPath).createDirectories();
	}

	/**
	 * Gets the bucket.
	 * 
	 * @return the current bucket
	 */
	@Override
	public String getBucket() {
		return bucket;
	}

	/**
	 * Gets the buckets from the storage.
	 * 
	 * @return the list of buckets (one bucket)
	 */
	@Override
	public List<String> getBuckets() {
		return Arrays.asList(bucket);
	}

	/**
	 * Checks if the bucket exists.
	 * 
	 * In the one-bucket concept, it compares with the current bucket.
	 * 
	 * @param bucketName the name of the bucket
	 * @return true if the bucket exists
	 */
	@Override
	public boolean bucketExists(String bucketName) {
		return bucket.equals(bucketName);
	}

	/**
	 * Deletes the bucket from the storage.
	 * 
	 * @param bucket the bucket to delete
	 * @throws IOException if the bucket cannot be deleted
	 */
	@Override
	public void deleteBucket(String bucket) throws IOException {
		posixDAL.delete(getFullBucketPath());
	}

	/**
	 * Gets files from the storage with the given prefix (folder).
	 * 
	 * @param relativePath the prefix (folder) for searching in the storage
	 * @return the list of files with the given prefix
	 */
	@Override
	public List<String> getRelativeFiles(String relativePath) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles({})", relativePath);

		String path = new PathConverter(cfg.getBasePath(), relativePath).getPath();

		return getRelativePath(posixDAL.getFiles(path));
	}

	/**
	 * Gets all files from the storage.
	 * 
	 * @return the list of all files from the storage
	 */
	@Override
	public List<String> getRelativeFiles() {
		return getRelativePath(posixDAL.getFiles(cfg.getBasePath()));
	}

	/**
	 * Gets files (absolute paths) from the storage with the given prefix (folder).
	 * 
	 * @param relativePath the prefix (relative path) for searching in the storage
	 * @return the list of files with the given prefix
	 */
	public List<String> getAbsoluteFiles(String relativePath) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles({})", relativePath);

		String path = new PathConverter(cfg.getBasePath(), relativePath).getPath();

		return posixDAL.getFiles(path);
	}

	/**
	 * Gets all files (absolute paths) from the storage.
	 * 
	 * @return the list of all files from the storage
	 */
	public List<String> getAbsoluteFiles() {
		return posixDAL.getFiles(cfg.getBasePath());
	}

	/**
	 * Gets the relative path from the absolute path using the base path list.
	 * 
	 * @param absolutePath the absolute path
	 * @return the relative path
	 */
	@Override
	public String getRelativePath(String absolutePath) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getRelativePath({})", absolutePath);

		List<String> basePaths = new ArrayList<>();
		basePaths.add(cfg.getBasePath());
		basePaths.add(cfg.getSourcePath());

		return new PathConverter(absolutePath, basePaths).getRelativePath().getPath();
	}

	/**
	 * Gets the relative paths from the absolute paths using the base path list.
	 * 
	 * @param absolutePaths the absolute paths
	 * @return the relative paths
	 */
	@Override
	public List<String> getRelativePath(List<String> absolutePaths) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getRelativePath({})", absolutePaths);

		logger.trace("... basePath = {}, sourcePath = {}", cfg.getBasePath(), cfg.getSourcePath());
		List<String> basePaths = new ArrayList<>();
		basePaths.add(cfg.getBasePath());
		basePaths.add(cfg.getSourcePath());

		List<String> relativePaths = new ArrayList<>();

		for (String absolutePath : absolutePaths) {
			String relativePath = new PathConverter(absolutePath, basePaths).getRelativePath().getPath();
			relativePaths.add(relativePath);
		}

		logger.trace("... resulting relativePaths = {}");

		return relativePaths;
	}

	/**
	 * Gets the absolute path (POSIX: /&lt;storagePath&gt;/&lt;relativePath&gt;).
	 * 
	 * @param relativePath the relative path
	 * @return the absolute file depending on the storage file system
	 */
	public String getAbsolutePath(String relativePath) {
		if (new PathConverter(relativePath).startsWithSlash())
			return relativePath;

		return new PathConverter(getBasePath(), relativePath).getPath();
	}

	/**
	 * Gets the absolute paths (POSIX: /&lt;storagePath&gt;/&lt;relativePath&gt;).
	 * 
	 * @param relativePaths the relative paths
	 * @return the absolute paths depending on the storage file system
	 */
	public List<String> getAbsolutePath(List<String> relativePaths) {
		List<String> absolutePaths = new ArrayList<>();

		for (String relativePath : relativePaths) {
			String absolutePath = getAbsolutePath(relativePath);
			absolutePaths.add(absolutePath);
		}

		return absolutePaths;
	}

	/**
	 * Gets the storage file.
	 * 
	 * @param relativePath the relative path in storage to the file
	 * @return the storage file object
	 */
	@Override
	public StorageFile getStorageFile(String relativePath) {
		return new PosixStorageFile(cfg.getBasePath(), relativePath);
	}

	/**
	 * Gets the storage files.
	 * 
	 * @return the list of storage files
	 */
	@Override
	public List<StorageFile> getStorageFiles() {
		List<String> paths = posixDAL.getFiles(cfg.getBasePath());
		List<StorageFile> storageFiles = new ArrayList<>();

		for (String path : paths) {
			StorageFile storageFile = getStorageFile(getRelativePath(path));
			storageFiles.add(storageFile);
		}

		return storageFiles;
	}

	/**
	 * Creates a physical storage file.
	 * 
	 * @param relativePath the relative path of the file
	 * @param content      the content of the file
	 * @return the storage file object of the created file
	 * @throws IOException if the file cannot be created
	 */
	@Override
	public StorageFile createStorageFile(String relativePath, String content) throws IOException {
		StorageFile storageFile = getStorageFile(relativePath);

		FileUtils fileUtils = new FileUtils(storageFile.getFullPath());
		if (!fileUtils.createFile(content))
			throw new IOException("Cannot create file in POSIX Storage: " + relativePath);

		return storageFile;
	}

	/**
	 * Checks if the file exists.
	 * 
	 * @param storageFile the storage file to check
	 * @return true if the file exists physically
	 */
	@Override
	public boolean fileExists(StorageFile storageFile) {
		return new File(storageFile.getFullPath()).isFile();
	}

	/**
	 * Checks if the storage file or directory is a file (no slash at the end of the
	 * path).
	 * 
	 * @param storageFileOrDir the storage file or directory
	 * @return true if the storage file is a file
	 */
	@Override
	public boolean isFile(StorageFile storageFileOrDir) {
		return new File(storageFileOrDir.getFullPath()).isFile();
	}

	/**
	 * Checks if the storage file or directory is a directory.
	 * 
	 * @param storageFileOrDir the storage file or directory
	 * @return true if the storage file is a file
	 */
	@Override
	public boolean isDirectory(StorageFile storageFileOrDir) {
		return new File(storageFileOrDir.getFullPath()).isDirectory();
	}

	/**
	 * Gets the file size.
	 * 
	 * @param storageFile the storage file
	 * @return the file size of the storage file
	 */
	@Override
	public long getFileSize(StorageFile storageFile) {
		FileUtils fileUtils = new FileUtils(storageFile.getFullPath());
		return fileUtils.getFileSize();
	}

	/**
	 * Gets the file content.
	 * 
	 * @param storageFile the storage file
	 * @return the file content
	 */
	@Override
	public String getFileContent(StorageFile storageFile) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getFileContent({})", storageFile.getFullPath());

		return posixDAL.getFileContent(storageFile.getFullPath());
	}

	/**
	 * Uploads the file to the storage.
	 * 
	 * @param sourceFile      the source file to upload
	 * @param targetFileOrDir the target file or directory in the storage
	 * @return the uploaded storage file
	 * @throws IOException if the file cannot be uploaded
	 */
	@Override
	public String uploadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({},{})", sourceFile.getFullPath(), targetFileOrDir.getFullPath());

		String uploadedAbsoluteFile = posixDAL.uploadFile(sourceFile.getFullPath(), targetFileOrDir.getFullPath());

		return getRelativePath(uploadedAbsoluteFile);
	}

	/**
	 * Uploads the file or directory recursively to the storage.
	 * 
	 * @param sourceFileOrDir the source file or directory
	 * @param targetFileOrDir the target file or directory in the storage
	 * @return the list of uploaded files
	 * @throws IOException if the file or directory cannot be uploaded
	 */
	@Override
	public List<String> upload(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},{})", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		List<String> uploadedAbsolutePaths = posixDAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return getRelativePath(uploadedAbsolutePaths);
	}

	/**
	 * Uploads the file or directory recursively to the storage.
	 * 
	 * @param sourceFileOrDir the source file or dir to upload
	 * @return the list of uploaded files
	 * @throws IOException if the file or directory cannot be uploaded
	 */
	@Override
	public List<String> upload(StorageFile sourceFileOrDir) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> upload({})", sourceFileOrDir.getFullPath());

		StorageFile targetFileOrDir = new PosixStorageFile(cfg.getBasePath(), sourceFileOrDir.getRelativePath());

		List<String> uploadedAbsolutePaths = posixDAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return getRelativePath(uploadedAbsolutePaths);
	}

	/**
	 * Uploads the file to the storage.
	 * 
	 * @param sourceFile the source file to upload
	 * @return the path of the uploaded file
	 * @throws IOException if the file cannot be uploaded
	 */
	@Override
	public String uploadFile(StorageFile sourceFile) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({})", sourceFile.getFullPath());

		StorageFile targetFile = new PosixStorageFile(cfg.getBasePath(), sourceFile.getRelativePath());

		String uploadedFile = posixDAL.uploadFile(sourceFile.getFullPath(), targetFile.getFullPath());

		return getRelativePath(uploadedFile);
	}

	/**
	 * Uploads the source file or directory recursively to the storage.
	 * 
	 * @param relativeSourceFileOrDir the relative path to source file or directory
	 * @return the list of uploaded files to the storage
	 * @throws IOException if the file or directory cannot be uploaded
	 */
	@Override
	public List<String> uploadSourceFileOrDir(String relativeSourceFileOrDir) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> upload({})", relativeSourceFileOrDir);

		StorageFile sourceFileOrDir = new PosixStorageFile(cfg.getSourcePath(), relativeSourceFileOrDir);
		StorageFile targetFileOrDir = new PosixStorageFile(cfg.getBasePath(), relativeSourceFileOrDir);

		List<String> uploadedAbsolutePaths = posixDAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return getRelativePath(uploadedAbsolutePaths);
	}

	/**
	 * Uploads the source file to the storage.
	 * 
	 * @param relativeSourceFile the relative path to the source file
	 * @return the uploaded source file
	 * @throws IOException if the file cannot be uploaded
	 */
	@Override
	public String uploadSourceFile(String relativeSourceFile) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({})", relativeSourceFile);

		StorageFile sourceFile = new PosixStorageFile(cfg.getSourcePath(), relativeSourceFile);
		StorageFile targetFile = new PosixStorageFile(cfg.getBasePath(), relativeSourceFile);

		String uploadedFile = posixDAL.uploadFile(sourceFile.getFullPath(), targetFile.getFullPath());

		return getRelativePath(uploadedFile);
	}

	/**
	 * Downloads the file or directory from the storage.
	 * 
	 * @param sourceFileOrDir the storage source file or directory to download
	 * @param targetFileOrDir the target file or directory
	 * @return the list of downloaded files
	 * @throws IOException if the file cannot be downloaded
	 */
	@Override
	public List<String> download(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> download({},{})", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return posixDAL.download(sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());
	}

	/**
	 * Downloads the file from the storage.
	 * 
	 * @param sourceFile      the storage source file to download
	 * @param targetFileOrDir the target file or directory
	 * @return the downloaded storage file
	 * @throws IOException if the file cannot be downloaded
	 */
	@Override
	public String downloadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadFile({},{})", sourceFile.getFullPath(), targetFileOrDir.getFullPath());

		return posixDAL.downloadFile(sourceFile.getFullPath(), targetFileOrDir.getFullPath());
	}

	/**
	 * Deletes the file or directory recursively from the storage.
	 * 
	 * @param storageFileOrDir the storage file or directory to delete
	 * @return the list of deleted files from the storage
	 * @throws IOException if the file or directory cannot be deleted
	 */
	@Override
	public List<String> delete(StorageFile storageFileOrDir) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", storageFileOrDir.getFullPath());

		return posixDAL.delete(storageFileOrDir.getFullPath());
	}

	/**
	 * Deletes the storage file from the storage.
	 * 
	 * @param storageFile the storage file to delete
	 * @return the deleted storage file
	 * @throws IOException if the file cannot be deleted
	 */
	@Override
	public String deleteFile(StorageFile storageFile) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> deleteFile({})", storageFile.getFullPath());

		return posixDAL.deleteFile(storageFile.getFullPath());
	}

	/**
	 * Deletes the file or directory recursively from the storage.
	 * 
	 * @param relativeFileOrDir the relative path to the storage file or directory
	 *                          to delete
	 * @return the list of deleted files from the storage
	 * @throws IOException if the file or directory cannot be deleted
	 */
	@Override
	public List<String> delete(String relativeFileOrDir) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", relativeFileOrDir);

		StorageFile storageFileOrDir = getStorageFile(relativeFileOrDir);

		return posixDAL.delete(storageFileOrDir.getFullPath());
	}

	/**
	 * Adds the file system prefix to the path.
	 * 
	 * @param path the path to extend
	 * @return the file system prefix + "|" + path
	 */
	@Override
	public String addFSPrefix(String path) {
		return StorageType.POSIX.toString() + "|" + path;
	}

	/**
	 * Adds the file system prefix to the paths.
	 * 
	 * @param paths the paths to extend
	 * @return the list of file system prefix + path
	 */
	@Override
	public List<String> addFSPrefix(List<String> paths) {
		List<String> pathsWithPrefix = new ArrayList<>();

		for (String path : paths) {
			String pathWithPrefix = addFSPrefix(path);
			pathsWithPrefix.add(pathWithPrefix);
		}

		return pathsWithPrefix;
	}

	/**
	 * Gets the input stream from the file.
	 * 
	 * @param storageFile the storage file
	 * @return the input stream from the file
	 */
	@Override
	public InputStream getInputStream(StorageFile storageFile) throws IOException {
		String fullpath = storageFile.getFullPath();

		try {
			return new FileInputStream(fullpath);
		} catch (FileNotFoundException e) {
			logger.log(StorageMgrMessage.FILE_NOT_FOUND, fullpath);
			throw e;
		}
	}

	/**
	 * Gets the relative path from the absolute path.
	 * 
	 * @param absolutePath the absolute path
	 * @return the relative path
	 */
	/*
	 * private String getRelativePath(String absolutePath) {
	 * 
	 * if (logger.isTraceEnabled()) logger.trace(">>> getRelativePath({})",
	 * absolutePath);
	 * 
	 * Path pathAbsolute = Paths.get(absolutePath); Path pathBase =
	 * Paths.get(basePath); Path pathRelative = pathBase.relativize(pathAbsolute);
	 * System.out.println("RelativePath: " + pathRelative + " from AbsolutePath: " +
	 * pathAbsolute);
	 * 
	 * return pathRelative.toString(); }
	 */

	/**
	 * Gets the full bucket path.
	 * 
	 * @return the full bucket path
	 */
	private String getFullBucketPath() {
		return bucket.equals(StorageFile.NO_BUCKET) ? cfg.getBasePath() : Paths.get(cfg.getBasePath(), bucket).toString();
	}
}