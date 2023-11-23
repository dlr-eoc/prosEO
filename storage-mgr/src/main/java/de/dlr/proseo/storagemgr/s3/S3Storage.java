/**
 * S3Storage.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.s3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.posix.PosixStorageFile;
import de.dlr.proseo.storagemgr.utils.FileUtils;
import de.dlr.proseo.storagemgr.utils.PathConverter;

/**
 * An implementation of the Storage interface for a S3-based file system,
 * providing functionality to interact with the storage system, that is
 * retrieving information about the storage, performing file operations, and
 * path conversion.
 *
 * @author Denys Chaykovskiy
 */
public class S3Storage implements Storage {

	/** S3 data access layer object */
	private S3DAL s3DAL;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(S3Storage.class);

	/**
	 * Constructor with the s3 configuration
	 *
	 * @param cfg s3 configuration
	 * @throws IOException if an I/O exception occurs
	 */
	public S3Storage(S3Configuration cfg) throws IOException {
		s3DAL = new S3DAL(cfg);

		new FileUtils(cfg.getSourcePath()).createDirectories();
	}

	/**
	 * Gets the storage type.
	 *
	 * @return the storage type
	 */
	@Override
	public StorageType getStorageType() {
		return StorageType.S3;
	}

	/**
	 * Gets the base path.
	 *
	 * @return the base path
	 */
	@Override
	public String getBasePath() {
		return s3DAL.getConfiguration().getBasePath();
	}

	/**
	 * Gets the absolute base path (fs prefix + bucket + base path), depends on fs.
	 *
	 * In other words, it is the absolute path without the relative path in this S3
	 * version. s3://bucket/
	 *
	 * @return the absolute base path
	 */
	@Override
	public String getAbsoluteBasePath() {
		return new PathConverter(s3DAL.getBucket()).addS3Prefix().getPath();
	}

	/**
	 * Gets the source path.
	 *
	 * @return the source path
	 */
	@Override
	public String getSourcePath() {
		return s3DAL.getConfiguration().getSourcePath();
	}

	/**
	 * Sets the bucket.
	 *
	 * @param bucket the bucket to set
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public void setBucket(String bucket) throws IOException {
		s3DAL.setBucket(bucket);
	}

	/**
	 * Gets the bucket.
	 *
	 * @return the current bucket
	 */
	@Override
	public String getBucket() {
		return s3DAL.getBucket();
	}

	/**
	 * Gets the buckets from storage.
	 *
	 * @return the list of buckets
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public List<String> getBuckets() throws IOException {
		return s3DAL.getBuckets();
	}

	/**
	 * Checks if the bucket exists.
	 *
	 * @param bucketName the name of the bucket
	 * @return true if the bucket exists
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public boolean bucketExists(String bucketName) throws IOException {
		return s3DAL.bucketExists(bucketName);
	}

	/**
	 * Deletes the bucket in storage.
	 *
	 * @param bucketName the bucket to delete
	 * @throws IOException if the bucket cannot be deleted
	 */
	@Override
	public void deleteBucket(String bucketName) throws IOException {
		s3DAL.deleteBucket(bucketName);
	}

	/**
	 * Gets the files from storage with the given prefix (folder).
	 *
	 * @param folder the prefix (folder) for search in storage
	 * @return the list of files with the given prefix
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public List<String> getRelativeFiles(String folder) throws IOException {
		return s3DAL.getFiles(folder);
	}

	/**
	 * Gets all files from storage.
	 *
	 * @return the list of all files from storage
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public List<String> getRelativeFiles() throws IOException {
		return s3DAL.getFiles();
	}

	/**
	 * Gets files (absolute paths) from storage with the given relative path.
	 *
	 * @param relativePath the relative path for search in storage
	 * @return the list of files with the given prefix
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public List<String> getAbsoluteFiles(String relativePath) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> getAbsoluteFiles({})", relativePath);

		String path = new PathConverter("", relativePath).getPath();

		return getAbsolutePath(s3DAL.getFiles(path));
	}

	/**
	 * Gets all files (absolute paths) from storage.
	 *
	 * @return the list of all files from storage
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public List<String> getAbsoluteFiles() throws IOException {
		return getAbsolutePath(s3DAL.getFiles());
	}

	/**
	 * Gets the relative path from the absolute path by removing the S3 prefix,
	 * bucket, and left slash.
	 *
	 * @param absolutePath the absolute path
	 * @return the relative path
	 */
	@Override
	public String getRelativePath(String absolutePath) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getRelativePath({})", absolutePath);

		return new PathConverter(absolutePath).removeFsPrefix().removeBucket().removeLeftSlash().getPath();
	}

	/**
	 * Gets the relative paths from the absolute paths by removing the S3 prefix,
	 * bucket, and left slash.
	 *
	 * @param absolutePaths the absolute paths
	 * @return the relative paths
	 */
	@Override
	public List<String> getRelativePath(List<String> absolutePaths) {
		if (logger.isTraceEnabled())
			logger.trace(">>> getRelativePath({})", absolutePaths.size());

		List<String> relativePaths = new ArrayList<>();

		for (String absolutePath : absolutePaths) {
			String relativePath = new PathConverter(absolutePath).removeFsPrefix().removeBucket().removeLeftSlash().getPath();
			relativePaths.add(relativePath);
		}

		return relativePaths;
	}

	/**
	 * Gets the absolute path (s3://&lt;bucket&gt;/&lt;relativePath&gt;).
	 *
	 * @param relativePath the relative path
	 * @return the absolute file depending on the storage file system
	 */
	@Override
	public String getAbsolutePath(String relativePath) {
		return new PathConverter(s3DAL.getBucket(), relativePath).addS3Prefix().getPath();
	}

	/**
	 * Gets the absolute paths (s3://&lt;bucket&gt;/&lt;relativePath&gt;).
	 *
	 * @param relativePaths the relative paths
	 * @return the absolute paths depending on the storage file system
	 */
	@Override
	public List<String> getAbsolutePath(List<String> relativePaths) {
		List<String> absolutePaths = new ArrayList<>();

		for (String relativePath : relativePaths) {
			String absolutePath = getAbsolutePath(relativePath);
			absolutePaths.add(absolutePath);
		}

		return absolutePaths;
	}

	/**
	 * Gets the Storage File.
	 *
	 * @param relativePath the relative path in storage to the file
	 * @return the storage file object
	 */
	@Override
	public StorageFile getStorageFile(String relativePath) {
		return new S3StorageFile(s3DAL.getBucket(), relativePath);
	}

	/**
	 * Gets the storage files.
	 *
	 * @return the list of storage files
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public List<StorageFile> getStorageFiles() throws IOException {
		List<String> files = s3DAL.getFiles();
		List<StorageFile> storageFiles = new ArrayList<>();

		for (String filePath : files) {
			StorageFile storageFile = new S3StorageFile(s3DAL.getBucket(), filePath);
			storageFiles.add(storageFile);
		}

		return storageFiles;
	}

	/**
	 * Creates the physical storage file.
	 *
	 * @param relativePath the relative path of the file
	 * @param content      the content of the file
	 * @return the storage file object of the created file
	 * @throws IOException if the file cannot be created
	 */
	@Override
	public StorageFile createStorageFile(String relativePath, String content) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> createStorageFile({},{})", relativePath, content);

		String path = Paths.get(s3DAL.getConfiguration().getBasePath(), relativePath).toString();
		path = new PathConverter(path).fixAbsolutePath().getPath();

		FileUtils fileUtils = new FileUtils(path);
		fileUtils.createFile(content);

		StorageFile sourceFile = new PosixStorageFile(s3DAL.getConfiguration().getBasePath(), relativePath);
		StorageFile targetFile = new S3StorageFile(s3DAL.getBucket(), relativePath);

		try {
			uploadFile(sourceFile, targetFile);
		} catch (IOException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("An exception occurred. Cause: ", e);
			}
			throw e;
		} finally {
			new File(path).delete();
		}

		return targetFile;
	}

	/**
	 * Checks if the file exists.
	 *
	 * @param storageFile the storage file to check
	 * @return true if the file exists physically
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public boolean fileExists(StorageFile storageFile) throws IOException {
		return s3DAL.fileExists(storageFile.getRelativePath());
	}

	/**
	 * Checks if the storage file or directory is a file (no slash at the end of the
	 * path).
	 *
	 * @param storageFileOrDir the storage file or directory
	 * @return true if the storage file is a file
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public boolean isFile(StorageFile storageFileOrDir) throws IOException {
		return s3DAL.fileExists(storageFileOrDir.getRelativePath());
	}

	/**
	 * Checks if the storage file or directory is a directory.
	 *
	 * @param storageFileOrDir the storage file or directory
	 * @return true if the storage file is a file
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public boolean isDirectory(StorageFile storageFileOrDir) throws IOException {
		return !isFile(storageFileOrDir);
	}

	/**
	 * Gets the file size.
	 *
	 * @param storageFile the storage file
	 * @return the file size of the storage file
	 * @throws IOException if an I/O exception occurs
	 */
	@Override
	public long getFileSize(StorageFile storageFile) throws IOException {
		return s3DAL.getFileSize(storageFile.getRelativePath());
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

		return s3DAL.getFileContent(storageFile.getRelativePath());
	}

	/**
	 * Uploads the file to storage.
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

		return s3DAL.uploadFile(sourceFile.getFullPath(), targetFileOrDir.getRelativePath());
	}

	/**
	 * Uploads the file or directory recursively to storage.
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

		return s3DAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getRelativePath());
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

		StorageFile targetFileOrDir = new S3StorageFile(s3DAL.getBucket(), sourceFileOrDir.getRelativePath());

		return s3DAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getRelativePath());
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

		StorageFile targetFile = new S3StorageFile(s3DAL.getBucket(), sourceFile.getRelativePath());

		return s3DAL.uploadFile(sourceFile.getFullPath(), targetFile.getRelativePath());
	}

	/**
	 * Uploads the source file or directory recursively to the storage.
	 *
	 * @param relativeSourceFileOrDir the relative path to source file or directory
	 * @return the list of uploaded to storage source files
	 * @throws IOException if the file cannot be uploaded
	 */
	@Override
	public List<String> uploadSourceFileOrDir(String relativeSourceFileOrDir) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> upload({})", relativeSourceFileOrDir);

		StorageFile sourceFileOrDir = new PosixStorageFile(s3DAL.getConfiguration().getSourcePath(), relativeSourceFileOrDir);
		StorageFile targetFileOrDir = new S3StorageFile(s3DAL.getBucket(), sourceFileOrDir.getRelativePath());

		return s3DAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getRelativePath());
	}

	/**
	 * Uploads the source file.
	 *
	 * @param relativeSourceFile the relative path to source file
	 * @return the uploaded to storage source file
	 * @throws IOException if the file cannot be uploaded
	 */
	@Override
	public String uploadSourceFile(String relativeSourceFile) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({})", relativeSourceFile);

		StorageFile sourceFile = new PosixStorageFile(s3DAL.getConfiguration().getSourcePath(), relativeSourceFile);
		// StorageFile targetFile = new S3StorageFile(s3DAL.getBucket(),
		// sourceFile.getRelativePath());

		return s3DAL.uploadFile(sourceFile.getFullPath(), sourceFile.getRelativePath());
	}

	/**
	 * Downloads the file or directory recursively.
	 *
	 * @param sourceFileOrDir the source file or directory in the storage
	 * @param targetFileOrDir the target file or directory
	 * @return the list of downloaded files
	 * @throws IOException if the file cannot be downloaded
	 */
	@Override
	public List<String> download(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> download({},{})", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return s3DAL.download(sourceFileOrDir.getRelativePath(), targetFileOrDir.getFullPath());
	}

	/**
	 * Downloads the file from storage.
	 *
	 * @param sourceFile      the storage source file to download
	 * @param targetFileOrDir the target file or directory
	 * @return the Storage File object with the downloaded file
	 * @throws IOException if the file cannot be downloaded
	 */
	@Override
	public String downloadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadFile({},{})", sourceFile.getFullPath(), targetFileOrDir.getFullPath());

		return s3DAL.downloadFile(sourceFile.getRelativePath(), targetFileOrDir.getFullPath());
	}

	/**
	 * Deletes the file or directory recursively from the storage.
	 *
	 * @param storageFileOrDir the Storage file or directory to delete
	 * @return the list of deleted files from storage
	 * @throws IOException if the file or directory cannot be deleted
	 */
	@Override
	public List<String> delete(StorageFile storageFileOrDir) throws IOException {
		return s3DAL.delete(storageFileOrDir.getRelativePath());
	}

	/**
	 * Deletes the storage file from storage.
	 *
	 * @param storageFile the storage file to delete
	 * @return the deleted storage file
	 * @throws IOException if the file cannot be deleted
	 */
	@Override
	public String deleteFile(StorageFile storageFile) throws IOException {
		return s3DAL.deleteFile(storageFile.getRelativePath());
	}

	/**
	 * Deletes the file or directory recursively from the storage.
	 *
	 * @param relativeFileOrDir the Storage file or directory to delete
	 * @return the list of deleted files from storage
	 * @throws IOException if the file or directory cannot be deleted
	 */
	@Override
	public List<String> delete(String relativeFileOrDir) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", relativeFileOrDir);

		StorageFile storageFileOrDir = getStorageFile(relativeFileOrDir);

		return s3DAL.delete(storageFileOrDir.getRelativePath());
	}

	/**
	 * Adds the file system prefix to the path.
	 *
	 * @param path the path to extend
	 * @return the file system prefix + "|" + path
	 */
	@Override
	public String addFSPrefix(String path) {
		return StorageType.S3.toString() + "|" + path;
	}

	/**
	 * Adds the file system prefix to paths.
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
		if (logger.isTraceEnabled())
			logger.trace(">>> getInputStream({})", storageFile.getFullPath());

		return s3DAL.getInputStream(storageFile.getRelativePath());
	}
}
