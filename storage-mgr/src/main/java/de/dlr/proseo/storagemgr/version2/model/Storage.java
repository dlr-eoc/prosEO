/**
 * Storage.java
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.version2.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 *
 * A high-level interface for performing common operations on a storage system,
 * abstracting away the underlying implementation details. It provides methods
 * for retrieving information on the storage, managing buckets, and working with
 * files and directories, as well as utility methods to add a file system prefix
 * to a path and a list of paths.
 *
 * Implementations of this interface should provide the necessary logic to
 * interact with specific storage systems, such as POSIX or S3.
 *
 * @author Denys Chaykovskiy
 */
public interface Storage {

	/**
	 * Gets the storage type, e.g. POSIX or S3.
	 *
	 * @return the storage type
	 */
	public StorageType getStorageType();

	/**
	 * Gets the base path, which is the root directory of the storage system.
	 *
	 * @return the base path
	 */
	public String getBasePath();

	/**
	 * Gets the absolute base path (file system prefix + bucket + base path),
	 * depending on the file system.
	 *
	 * @return the absolute base path
	 */
	public String getAbsoluteBasePath();

	/**
	 * Gets the source path.
	 *
	 * @return the source path
	 */
	public String getSourcePath();

	/**
	 * Sets the bucket.
	 *
	 * @param bucket the bucket to set
	 * @throws IOException if an error occurs while setting the bucket
	 */
	public void setBucket(String bucket) throws IOException;

	/**
	 * Gets the current bucket.
	 *
	 * @return the current bucket
	 */
	public String getBucket();

	/**
	 * Gets the buckets from the storage.
	 *
	 * @return the list of buckets
	 * @throws IOException if an error occurs while getting the buckets
	 */
	public List<String> getBuckets() throws IOException;

	/**
	 * Checks if the bucket exists.
	 *
	 * @param bucketName the name of the bucket
	 * @return true if the bucket exists
	 * @throws IOException if an error occurs while checking the bucket existence
	 */
	public boolean bucketExists(String bucketName) throws IOException;

	/**
	 * Deletes a bucket in the storage.
	 *
	 * @param bucket the bucket to delete
	 * @throws IOException if an error occurs while deleting the bucket
	 */
	public void deleteBucket(String bucket) throws IOException;

	/**
	 * Gets files (relative paths) from the storage with the given prefix (folder).
	 *
	 * @param prefix the prefix (folder) to search in the storage
	 * @return the list of files starting with the given prefix
	 * @throws IOException if an error occurs while getting the relative files
	 */
	public List<String> getRelativeFiles(String prefix) throws IOException;

	/**
	 * Gets all files (relative paths) from the storage.
	 *
	 * @return the list of all files from the storage
	 * @throws IOException if an error occurs while getting the relative files
	 */
	public List<String> getRelativeFiles() throws IOException;

	/**
	 * Gets the files (absolute paths) from the storage with the given prefix
	 * (folder).
	 *
	 * @param prefix the prefix (folder) to search in the storage
	 * @return the list of files starting with the given prefix
	 * @throws IOException if an error occurs while getting the absolute files
	 */
	public List<String> getAbsoluteFiles(String prefix) throws IOException;

	/**
	 * Gets all files (absolute paths) from the storage.
	 *
	 * @return the list of all files from the storage
	 * @throws IOException if an error occurs while getting the absolute files
	 */
	public List<String> getAbsoluteFiles() throws IOException;

	/**
	 * Gets the relative path from the absolute path.
	 *
	 * @param absolutePath the absolute path
	 * @return the relative path
	 */
	public String getRelativePath(String absolutePath);

	/**
	 * Gets the relative paths from the absolute paths.
	 *
	 * @param absolutePaths the absolute paths
	 * @return the relative paths
	 */
	public List<String> getRelativePath(List<String> absolutePaths);

	/**
	 * Gets the absolute path depending on the storage file system.
	 *
	 * @param relativePath the relative path
	 * @return the absolute file depending on the storage file system
	 */
	public String getAbsolutePath(String relativePath);

	/**
	 * Gets the absolute paths depending on the storage file system.
	 *
	 * @param relativePaths the relative paths
	 * @return the absolute paths depending on the storage file system
	 */
	public List<String> getAbsolutePath(List<String> relativePaths);

	/**
	 * Gets the storage file.
	 *
	 * @param relativePath the relative path in the storage to the file
	 * @return the storage file object
	 */
	public StorageFile getStorageFile(String relativePath);

	/**
	 * Gets the storage files.
	 *
	 * @return the list of storage files
	 * @throws IOException if an error occurs while getting the storage files
	 */
	public List<StorageFile> getStorageFiles() throws IOException;

	/**
	 * Physically creates a storage file.
	 *
	 * @param relativePath the relative path of the file
	 * @param content      the content of the file
	 * @return the storage file object of the created file
	 * @throws IOException if an error occurs while creating the storage file
	 */
	public StorageFile createStorageFile(String relativePath, String content) throws IOException;

	/**
	 * Checks if a file exists.
	 *
	 * @param storageFile the storage file to check
	 * @return true if the file exists physically
	 * @throws IOException if an error occurs while checking the file existence
	 */
	public boolean fileExists(StorageFile storageFile) throws IOException;

	/**
	 * Checks if a storage file or directory is a file (i.e., no slash at the end of
	 * the path).
	 *
	 * @param storageFileOrDir the storage file or directory
	 * @return true if the storage file is a file
	 * @throws IOException if an error occurs while checking if the storage file is
	 *                     a file
	 */
	public boolean isFile(StorageFile storageFileOrDir) throws IOException;

	/**
	 * Checks if the storage file or directory is a directory.
	 *
	 * @param storageFileOrDir the storage file or directory
	 * @return true if the storage file is a directory
	 * @throws IOException if an error occurs while checking if the storage file is
	 *                     a directory
	 */
	public boolean isDirectory(StorageFile storageFileOrDir) throws IOException;

	/**
	 * Gets the file size.
	 *
	 * @param storageFile the storage file
	 * @return the file size of the storage file
	 * @throws IOException if an error occurs while getting the file size
	 */
	public long getFileSize(StorageFile storageFile) throws IOException;

	/**
	 * Gets the content of the specified storage file.
	 *
	 * @param storageFile the storage file from which to retrieve the content
	 * @return the file content
	 * @throws IOException if an error occurs while getting the file content
	 */
	public String getFileContent(StorageFile storageFile) throws IOException;

	/**
	 * Uploads a file to the storage.
	 *
	 * @param sourceFile      the source file to upload
	 * @param targetFileOrDir the target file or directory in the storage
	 * @return the path of the uploaded file
	 * @throws IOException if an error occurs while uploading the file
	 */
	public String uploadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException;

	/**
	 * Uploads a file or directory recursively to the storage.
	 *
	 * @param sourceFileOrDir the source file or directory
	 * @param targetFileOrDir the target file or directory in the storage
	 * @return the list of uploaded files
	 * @throws IOException if an error occurs while uploading the file or directory
	 */
	public List<String> upload(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException;

	/**
	 * Uploads a file or directory recursively to the storage.
	 *
	 * @param sourceFileOrDir the source file or directory to upload
	 * @return the list of uploaded files
	 * @throws IOException if an error occurs while uploading the file or directory
	 */
	public List<String> upload(StorageFile sourceFileOrDir) throws IOException;

	/**
	 * Uploads a file to the storage.
	 *
	 * @param sourceFile the source file to upload
	 * @return the path of the uploaded file
	 * @throws IOException if an error occurs while uploading the file
	 */
	public String uploadFile(StorageFile sourceFile) throws IOException;

	/**
	 * Uploads a source file or directory recursively to the storage.
	 *
	 * @param relativeSourceFileOrDir the relative path to the source file or
	 *                                directory
	 * @return the list of uploaded source files to the storage
	 * @throws IOException if an error occurs while uploading the file
	 */
	public List<String> uploadSourceFileOrDir(String relativeSourceFileOrDir) throws IOException;

	/**
	 * Uploads a source file to the storage.
	 *
	 * @param relativeSourceFile the relative path to the source file
	 * @return the uploaded source file in the storage
	 * @throws IOException if an error occurs while uploading the file
	 */
	public String uploadSourceFile(String relativeSourceFile) throws IOException;

	/**
	 * Downloads a file or directory recursively from the storage.
	 *
	 * @param sourceFileOrDir the source file or directory in the storage
	 * @param targetFileOrDir the target file or directory
	 * @return the list of downloaded files
	 * @throws IOException if an error occurs while downloading the file or
	 *                     directory
	 */
	public List<String> download(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException;

	/**
	 * Downloads a file from the storage.
	 *
	 * @param sourceFile      the source file in the storage to download
	 * @param targetFileOrDir the target file or directory
	 * @return the downloaded storage file
	 * @throws IOException if an error occurs while downloading the file
	 */
	public String downloadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException;

	/**
	 * Deletes a file or directory recursively from the storage.
	 *
	 * @param storageFileOrDir the storage file or directory to delete
	 * @return the list of deleted files from the storage
	 * @throws IOException if an error occurs while deleting the file or directory
	 */
	public List<String> delete(StorageFile storageFileOrDir) throws IOException;

	/**
	 * Deletes a storage file from the storage.
	 *
	 * @param storageFileOrDir the storage file to delete
	 * @return the deleted storage file
	 * @throws IOException if an error occurs while deleting the file
	 */
	public String deleteFile(StorageFile storageFileOrDir) throws IOException;

	/**
	 * Deletes a file or directory recursively from the storage.
	 *
	 * @param relativeFileOrDir the relative path to the storage file or directory
	 *                          to delete
	 * @return the list of deleted files from the storage
	 * @throws IOException if an error occurs while deleting the file or directory
	 */
	public List<String> delete(String relativeFileOrDir) throws IOException;

	/**
	 * Adds a file system prefix to the path.
	 *
	 * @param path the path to extend
	 * @return the file system prefix + path
	 */
	public String addFSPrefix(String path);

	/**
	 * Adds a file system prefix and bucket to paths.
	 *
	 * @param paths the paths to extend
	 * @return the list of file system prefix + path
	 */
	public List<String> addFSPrefix(List<String> paths);

	/**
	 * Gets the input stream from a file.
	 *
	 * @param storageFile the storage file
	 * @return the input stream from the file
	 * @throws IOException if an error occurs while getting the input stream
	 */
	public InputStream getInputStream(StorageFile storageFile) throws IOException;
}