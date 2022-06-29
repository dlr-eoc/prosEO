package de.dlr.proseo.storagemgr.version2.model;

import java.io.IOException;
import java.util.List;

/**
 * Storage Interface
 * 
 * concept of default bucket
 * 1. No Bucket (now in POSIX) 2. Default bucket 3. Buckets 
 * 
 * @author Denys Chaykovskiy
 *
 */
public interface Storage {
		
	/**
	 * Gets storage type
	 * 
	 * @return storage type
	 */
	public StorageType getStorageType();

	/**
	 * Gets base path
	 * 
	 * @return base path
	 */
	public String getBasePath();

	/**
	 * Gets source path
	 * 
	 * @return source path
	 */
	public String getSourcePath();

	/**
	 * Sets the bucket
	 * 
	 * @param bucket bucket to set
	 */
	public void setBucket(String bucket);

	/**
	 * Gets the current bucket
	 * 
	 * @return current bucket
	 */
	public String getBucket();
	
	/**
	 * Gets buckets from storage
	 * 
	 * @return list of buckets
	 */
	public List<String> getBuckets();
	
	/**
	 * Checks if the bucket exists
	 * 
	 * @param bucketName the name of the bucket
	 * @return true if the bucket exists
	 */
	public boolean bucketExists(String bucketName);

	/**
	 * Deletes bucket in storage
	 * 
	 * @param bucket bucket to delete
	 * @throws IOException if bucket cannot be deleted
	 */
	public void deleteBucket(String bucket) throws IOException;
	
	/**
	 * Gets files from storage with given prefix (folder)
	 * 
	 * @param prefix prefix (folder) for search in storage
	 * @return list of files with given prefix
	 */
	public List<String> getFiles(String prefix);

	/**
	 * Gets all files from storage
	 * 
	 * @return list of all files from storage
	 */
	public List<String> getFiles();
	
	/**
	 * Gets Storage File
	 * 
	 * @param relativePath relative path in storage to the file
	 * @return the storage file object
	 */
	public StorageFile getStorageFile(String relativePath);

	/**
	 * Gets storage files
	 * 
	 * @return list of storage files
	 */
	public List<StorageFile> getStorageFiles();
	
	/**
	 * Creates physically storage file
	 * 
	 * @param relativePath relative path of the file
	 * @param content      content of the file
	 * @return storage file object of created file
	 */
	public StorageFile createStorageFile(String relativePath, String content);
	
	/**
	 * Checks if file exists
	 * 
	 * @param storageFile Storage File to check
	 * @return true if file exists physically
	 */
	public boolean fileExists(StorageFile storageFile);
	
	/**
	 * Checks if Storage file or directory is file (no slash at the end of the path)
	 * 
	 * @param storageFileOrDir storage file or directory
	 * @return true if storage file is file
	 */
	public boolean isFile(StorageFile storageFileOrDir);

	/**
	 * Checks if storage file or directory is directory
	 * 
	 * @param storageFileOrDir storage file or directory
	 * @return true if storage file is file
	 */
	public boolean isDirectory(StorageFile storageFileOrDir);

	/**
	 * Gets the file size
	 * 
	 * @param storageFile Storage file
	 * @return the file size of the storage file
	 */
	public long getFileSize(StorageFile storageFile);

	/**
	 * Uploads file to storage
	 * 
	 * @param sourceFile      source file to upload
	 * @param targetFileOrDir target file or directory in the storage
	 * @return the uploaded storage file
	 * @throws IOException if the file cannot be uploaded
	 */
	public String uploadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException;

	/**
	 * Uploads file or directory recursively to storage
	 * 
	 * @param sourceFileOrDir source file or directory
	 * @param targetFileOrDir target file or directory in the storage
	 * @return list of uploaded files
	 * @throws IOException if file or directory cannot be uploaded
	 */
	public List<String> upload(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException;

	/**
	 * Uploads file or directory recursively to the storage
	 * 
	 * @param sourceFileOrDir source file or dir to upload
	 * @return list of uploaded files
	 * @throws IOException if file or directory cannot be uploaded
	 */
	public List<String> upload(StorageFile sourceFileOrDir) throws IOException;

	/**
	 * Uploads file to the storage
	 * 
	 * @param sourceFile source file to upload
	 * @return path of the uploaded file
	 * @throws IOException if file cannot be uploaded
	 */
	public String uploadFile(StorageFile sourceFile) throws IOException;

	/**
	 * Uploads source file or directory recursively to th storage
	 * 
	 * @param relativeSourceFileOrDir relative path to source file or directory
	 * @return list of uploaded to storage source files
	 * @throws IOException if file cannot be uploaded
	 */
	public List<String> uploadSourceFileOrDir(String relativeSourceFileOrDir) throws IOException;

	/**
 	 * Uploads source file to the storage
 	 * 
	 * @param relativeSourceFile relative path to source file
	 * @return uploaded to storage source file
	 * @throws IOException if file cannot be uploaded
	 */
	public String uploadSourceFile(String relativeSourceFile) throws IOException;

	/**
	 * Downloads file or directory recursively
	 *   
	 * @param sourceFileOrDir source file or directory in the storage
	 * @param targetFileOrDir target file or directory
	 * @return list of downloaded files
	 * @throws IOException if file canot be downloaded
	 */
	public List<String> download(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException;

	/**
	 * Downloads file from storage
	 * 
	 * @param sourceFile      storage source file to download
	 * @param targetFileOrDir target file or directory
	 * @return Storage File object with downloaded file
	 * @throws IOException if the file cannot be downloaded
	 */
	public String downloadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException;

	/**
	 * Deletes file or directory recursively from the storage
	 * 
	 * @param storageFileOrDir Storage file or directory to delete
	 * @return list of deleted files from storage
	 * @throws IOException if file or directory cannot be deleted
	 */
	public List<String> delete(StorageFile storageFileOrDir) throws IOException;

	/**
	 * Deletes storage file from storage
	 * 
	 * @param storageFileOrDir storage file to delete
	 * @return deleted storage file
	 * @throws IOException if file cannot be deleted
	 */
	public String deleteFile(StorageFile storageFileOrDir) throws IOException;

	/**
	 * Adds file system prefix to the path
	 * 
	 * @param path path to extend
	 * @return file system prefix + path
	 */
	public String addFSPrefix(String path);

	/**
	 * Adds file system prefix to paths
	 * 
	 * @param paths paths to extend
	 * @return list of file system prefix + path
	 */
	public List<String> addFSPrefix(List<String> paths);
}
