package de.dlr.proseo.storagemgr.version2.s3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.FileUtils;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;
import de.dlr.proseo.storagemgr.version2.posix.PosixStorageFile;

/**
 * S3 Storage
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3Storage implements Storage {

	/** s3 data access layer object */
	private S3DAL s3DAL;

	/** base path */
	private String basePath;

	/** source path */
	private String sourcePath;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3Storage.class);

	/**
	 * Constructor with bucket, access keys, region and end point
	 * 
	 * @param basePath          base path
	 * @param sourcePath        source path
	 * @param s3AccessKey       s3 access key
	 * @param s3SecretAccessKey s3 secret access key
	 * @param bucket            bucket
	 */
	public S3Storage(String basePath, String sourcePath, String s3AccessKey, String s3SecretAccessKey, String bucket) {
		s3DAL = new S3DAL(s3AccessKey, s3SecretAccessKey, bucket);
		this.basePath = basePath;
		this.sourcePath = sourcePath;
	}

	/**
	 * Constructor with bucket and access keys
	 * 
	 * @param basePath          base path
	 * @param sourcePath        source path
	 * @param s3AccessKey       s3 access key
	 * @param s3SecretAccessKey s3 secret access key
	 * @param s3Region          s3 region
	 * @param s3EndPoint        s3 end point
	 * @param bucket            bucket
	 */
	public S3Storage(String basePath, String sourcePath, String s3AccessKey, String s3SecretAccessKey, String s3Region,
			String s3EndPoint, String bucket) {
		s3DAL = new S3DAL(s3AccessKey, s3SecretAccessKey, s3Region, s3EndPoint, bucket);
		this.basePath = basePath;
		this.sourcePath = sourcePath;
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
	 * Gets base path
	 * 
	 * @return base path
	 */
	@Override
	public String getBasePath() {
		return basePath;
	}

	/**
	 * Gets source path
	 * 
	 * @return source path
	 */
	@Override
	public String getSourcePath() {
		return sourcePath;
	}

	/**
	 * Sets the bucket
	 * 
	 * @param bucket bucket to set
	 */
	@Override
	public void setBucket(String bucket) {
		s3DAL.setBucket(bucket);
	}

	/**
	 * Gets the bucket
	 * 
	 * @return current bucket
	 */
	@Override
	public String getBucket() {
		return s3DAL.getBucket();
	}

	/**
	 * Gets buckets from storage
	 * 
	 * @return list of buckets
	 */
	@Override
	public List<String> getBuckets() {
		return s3DAL.getBuckets();
	}

	/**
	 * Checks if the bucket exists
	 * 
	 * @param bucketName the name of the bucket
	 * @return true if the bucket exists
	 */
	@Override
	public boolean bucketExists(String bucketName) {
		return s3DAL.bucketExists(bucketName);
	}

	/**
	 * Deletes bucket in storage
	 * 
	 * @param bucket bucket to delete
	 * @throws IOException if bucket cannot be deleted
	 */
	@Override
	public void deleteBucket(String bucketName) {
		s3DAL.deleteBucket(bucketName);
	}

	/**
	 * Gets files from storage with given prefix (folder)
	 * 
	 * @param prefix prefix (folder) for search in storage
	 * @return list of files with given prefix
	 */
	@Override
	public List<String> getFiles(String folder) {

		return s3DAL.getFiles(folder);
	}

	/**
	 * Gets all files from storage
	 * 
	 * @return list of all files from storage
	 */
	@Override
	public List<String> getFiles() {
		return s3DAL.getFiles();
	}
	

	/**
	 * Gets the absolute path (s3://<bucket>/<relativePath>)
	 * 
	 * @param relativePath relative path
	 * @return the absolute file depending on storage file system
	 */
	public String getAbsolutePath(String relativePath) {
		
		return new PathConverter(s3DAL.getBucket(), relativePath).addS3Prefix().getPath();			
	}
	
	/**
	 * Gets absolute paths (s3://<bucket>/<relativePath>)
	 * 
	 * @param relativePath relative paths
	 * @return absolute paths depending on storage file system
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
	 * Gets Storage File
	 * 
	 * @param relativePath relative path in storage to the file
	 * @return the storage file object
	 */
	@Override
	public StorageFile getStorageFile(String relativePath) {
		return new S3StorageFile(s3DAL.getBucket(), relativePath);
	}

	/**
	 * Gets storage files
	 * 
	 * @return list of storage files
	 */
	@Override
	public List<StorageFile> getStorageFiles() {

		List<String> files = s3DAL.getFiles();
		List<StorageFile> storageFiles = new ArrayList<StorageFile>();

		for (String filePath : files) {

			StorageFile storageFile = new S3StorageFile(s3DAL.getBucket(), filePath);
			storageFiles.add(storageFile);
		}

		return storageFiles;
	}

	/**
	 * Creates physically storage file
	 * 
	 * @param relativePath relative path of the file
	 * @param content      content of the file
	 * @return storage file object of created file
	 */
	@Override
	public StorageFile createStorageFile(String relativePath, String content) {

		if (logger.isTraceEnabled())
			logger.trace(">>> createStorageFile({},{})", relativePath, content);

		String path = Paths.get(basePath, relativePath).toString();

		path = new PathConverter(path).fixAbsolutePath().getPath();

		FileUtils fileUtils = new FileUtils(path);
		fileUtils.createFile(content);

		StorageFile sourceFile = new PosixStorageFile(basePath, relativePath);
		StorageFile targetFile = new S3StorageFile(s3DAL.getBucket(), relativePath);

		try {
			uploadFile(sourceFile, targetFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		new File(path).delete();

		return targetFile;
	}

	/**
	 * Checks if file exists
	 * 
	 * @param storageFile Storage File to check
	 * @return true if file exists physically
	 */
	@Override
	public boolean fileExists(StorageFile storageFile) {
		return s3DAL.fileExists(storageFile.getRelativePath());
	}

	/**
	 * Checks if Storage file or directory is file (no slash at the end of the path)
	 * 
	 * @param storageFileOrDir storage file or directory
	 * @return true if storage file is file
	 */
	@Override
	public boolean isFile(StorageFile storageFileOrDir) {
		return s3DAL.fileExists(storageFileOrDir.getRelativePath());
	}

	/**
	 * Checks if storage file or directory is directory
	 * 
	 * @param storageFileOrDir storage file or directory
	 * @return true if storage file is file
	 */
	@Override
	public boolean isDirectory(StorageFile storageFileOrDir) {
		return !isFile(storageFileOrDir);
	}

	/**
	 * Gets the file size
	 * 
	 * @param storageFile Storage file
	 * @return the file size of the storage file
	 */
	@Override
	public long getFileSize(StorageFile storageFile) {
		return s3DAL.getFileSize(storageFile.getRelativePath());
	}

	/**
	 * Uploads file to storage
	 * 
	 * @param sourceFile      source file to upload
	 * @param targetFileOrDir target file or directory in the storage
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
	 * Uploads file or directory recursively to storage
	 * 
	 * @param sourceFileOrDir source file or directory
	 * @param targetFileOrDir target file or directory in the storage
	 * @return list of uploaded files
	 * @throws IOException if file or directory cannot be uploaded
	 */
	@Override
	public List<String> upload(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},{})", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return s3DAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getRelativePath());
	}

	/**
	 * Uploads file or directory recursively to the storage
	 * 
	 * @param sourceFileOrDir source file or dir to upload
	 * @return list of uploaded files
	 * @throws IOException if file or directory cannot be uploaded
	 */
	@Override
	public List<String> upload(StorageFile sourceFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({})", sourceFileOrDir.getFullPath());

		StorageFile targetFileOrDir = new S3StorageFile(s3DAL.getBucket(), sourceFileOrDir.getRelativePath());

		return s3DAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getRelativePath());
	}

	/**
	 * Uploads file to the storage
	 * 
	 * @param sourceFile source file to upload
	 * @return path of the uploaded file
	 * @throws IOException if file cannot be uploaded
	 */
	@Override
	public String uploadFile(StorageFile sourceFile) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({})", sourceFile.getFullPath());

		StorageFile targetFile = new S3StorageFile(s3DAL.getBucket(), sourceFile.getRelativePath());

		return s3DAL.uploadFile(sourceFile.getFullPath(), targetFile.getRelativePath());
	}

	/**
	 * Uploads source file or directory recursively to th storage
	 * 
	 * @param relativeSourceFileOrDir relative path to source file or directory
	 * @return list of uploaded to storage source files
	 * @throws IOException if file cannot be uploaded
	 */
	@Override
	public List<String> uploadSourceFileOrDir(String relativeSourceFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({})", relativeSourceFileOrDir);

		StorageFile sourceFileOrDir = new PosixStorageFile(sourcePath, relativeSourceFileOrDir);
		StorageFile targetFileOrDir = new S3StorageFile(s3DAL.getBucket(), sourceFileOrDir.getRelativePath());

		return s3DAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getRelativePath());
	}

	/**
	 * Uploads source file
	 * 
	 * @param relativeSourceFile relative path to source file
	 * @return uploaded to storage source file
	 * @throws IOException if file cannot be uploaded
	 */
	@Override
	public String uploadSourceFile(String relativeSourceFile) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({})", relativeSourceFile);

		StorageFile sourceFile = new PosixStorageFile(sourcePath, relativeSourceFile);
		// StorageFile targetFile = new S3StorageFile(s3DAL.getBucket(),
		// sourceFile.getRelativePath());

		return s3DAL.uploadFile(sourceFile.getFullPath(), sourceFile.getFullPath());
	}

	/**
	 * Downloads file or directory recursively
	 * 
	 * @param sourceFileOrDir source file or directory in the storage
	 * @param targetFileOrDir target file or directory
	 * @return list of downloaded files
	 * @throws IOException if file canot be downloaded
	 */
	@Override
	public List<String> download(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> download({},{})", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return s3DAL.download(sourceFileOrDir.getRelativePath(), targetFileOrDir.getFullPath());
	}

	/**
	 * Downloads file from storage
	 * 
	 * @param sourceFile      storage source file to download
	 * @param targetFileOrDir target file or directory
	 * @return Storage File object with downloaded file
	 * @throws IOException if the file cannot be downloaded
	 */
	@Override
	public String downloadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> downloadFile({},{})", sourceFile.getFullPath(), targetFileOrDir.getFullPath());

		return s3DAL.downloadFile(sourceFile.getRelativePath(), targetFileOrDir.getFullPath());
	}

	/**
	 * Deletes file or directory recursively from the storage
	 * 
	 * @param storageFileOrDir Storage file or directory to delete
	 * @return list of deleted files from storage
	 * @throws IOException if file or directory cannot be deleted
	 */
	@Override
	public List<String> delete(StorageFile storageFileOrDir) {

		return s3DAL.delete(storageFileOrDir.getRelativePath());
	}

	/**
	 * Deletes storage file from storage
	 * 
	 * @param storageFileOrDir storage file to delete
	 * @return deleted storage file
	 * @throws IOException if file cannot be deleted
	 */
	@Override
	public String deleteFile(StorageFile storageFile) throws IOException {
		return s3DAL.deleteFile(storageFile.getRelativePath());
	}

	/**
	 * Deletes file or directory recursively from the storage
	 * 
	 * @param relativeFileOrDir Storage file or directory to delete
	 * @return list of deleted files from storage
	 * @throws IOException if file or directory cannot be deleted
	 */
	@Override
	public List<String> delete(String relativeFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", relativeFileOrDir);

		StorageFile storageFileOrDir = getStorageFile(relativeFileOrDir);

		return s3DAL.delete(storageFileOrDir.getRelativePath());
	}

	/**
	 * Adds file system prefix to the path
	 * 
	 * @param path path to extend
	 * @return file system prefix + bucket + path
	 */
	@Override
	public String addFSPrefix(String path) {

		String prefix = StorageType.S3.toString() + "|";
		return new PathConverter(prefix, path).getPath();
	}

	/**
	 * Adds file system prefix to paths
	 * 
	 * @param paths paths to extend
	 * @return list of file system prefix + path
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
}