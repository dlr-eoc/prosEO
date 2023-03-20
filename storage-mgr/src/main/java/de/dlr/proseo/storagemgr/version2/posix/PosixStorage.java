package de.dlr.proseo.storagemgr.version2.posix;

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
import de.dlr.proseo.storagemgr.version2.FileUtils;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

/**
 * Posix Storage
 * 
 * @author Denys Chaykovskiy
 *
 */
public class PosixStorage implements Storage {

	/** base path */
	private String basePath;

	/** source path */
	private String sourcePath;

	/** bucket */
	private String bucket;

	/** Logger for this class */
	private static ProseoLogger logger = new ProseoLogger(PosixStorage.class);

	/** posix data access layer object */
	private PosixDAL posixDAL = new PosixDAL();

	/**
	 * Default Constructor
	 * 
	 */
	public PosixStorage() {
	}

	/**
	 * No bucket constructor
	 * 
	 * @param basePath   base path
	 * @param sourcePath source path
	 */
	public PosixStorage(String basePath, String sourcePath) {
		this.basePath = basePath;
		this.sourcePath = sourcePath;
		this.bucket = StorageFile.NO_BUCKET;

		new FileUtils(basePath).createDirectories();
		new FileUtils(sourcePath).createDirectories();
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
	 * Gets base path
	 * 
	 * @return base path
	 */
	@Override
	public String getBasePath() {
		return basePath;
	}
	
	/**
	 * Gets absolute base path (fs prefix + bucket + base path), depends on fs
	 * 
	 * in other words absolute path without relative path
	 * in this no-bucket posix version returns base path
	 * 
	 * @return absolute base path
	 */
	@Override
	public String getAbsoluteBasePath() {
			
		return new PathConverter(getBasePath()).getPath();
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
		this.bucket = bucket;

		String bucketPath = Paths.get(basePath, bucket).toString();
		new FileUtils(bucketPath).createDirectories();
	}

	/**
	 * Gets the bucket
	 * 
	 * @return current bucket
	 */
	@Override
	public String getBucket() {
		return bucket;
	}

	/**
	 * Gets buckets from storage (one-bucket concept for posix)
	 * 
	 * @return list of buckets (one bucket)
	 */
	@Override
	public List<String> getBuckets() {
		return Arrays.asList(bucket);
	}

	/**
	 * Checks if the bucket exists (in one-bucket concept compares with current
	 * bucket)
	 * 
	 * @param bucketName the name of the bucket
	 * @return true if the bucket exists
	 */
	@Override
	public boolean bucketExists(String bucketName) {
		return bucket.equals(bucketName);
	}

	/**
	 * Deletes bucket in storage
	 * 
	 * @param bucket bucket to delete
	 * @throws IOException if bucket cannot be deleted
	 */
	@Override
	public void deleteBucket(String bucket) throws IOException {

		posixDAL.delete(getFullBucketPath());
	}

	/**
	 * Gets files from storage with given prefix (folder)
	 * 
	 * @param prefix prefix (folder) for search in storage
	 * @return list of files with given prefix
	 */
	@Override
	public List<String> getRelativeFiles(String relativePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles({})", relativePath);

		String path = new PathConverter(basePath, relativePath).getPath();

		return getRelativePath(posixDAL.getFiles(path));
	}

	/**
	 * Gets all files from storage
	 * 
	 * @return list of all files from storage
	 */
	@Override
	public List<String> getRelativeFiles() {
		return getRelativePath(posixDAL.getFiles(basePath));
	}
	
	/**
	 * Gets files (absolute paths) from storage with given prefix (folder)
	 * 
	 * @param prefix prefix (relative path) for search in storage
	 * @return list of files with given prefix
	 */
	public List<String> getAbsoluteFiles(String relativePath) {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles({})", relativePath);

		String path = new PathConverter(basePath, relativePath).getPath();

		return posixDAL.getFiles(path);
	}

	/**
	 * Gets all files (absolute paths) from storage
	 * 
	 * @return list of all files from storage
	 */
	public List<String> getAbsoluteFiles() {
		return posixDAL.getFiles(basePath);	
	}

	/**
	 * Gets relative path from absolute path using base path list
	 * 
	 * @param absolutePath absolute path
	 * @return relative path
	 */
	@Override
	public String getRelativePath(String absolutePath) {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> getRelativePath({})", absolutePath);
		
		List<String> basePaths = new ArrayList<>();
		basePaths.add(basePath);
		basePaths.add(sourcePath);

		return new PathConverter(absolutePath, basePaths).getRelativePath().getPath();
	}
	
	/**
	 * Gets relative paths from absolute paths using base path list
	 * 
	 * @param absolutePaths absolute paths
	 * @return relative paths
	 */
	@Override
	public List<String> getRelativePath(List<String> absolutePaths) {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> getRelativePath({})", absolutePaths);
		
		logger.trace("... basePath = {}, sourcePath = {}", basePath, sourcePath);
		List<String> basePaths = new ArrayList<>();
		basePaths.add(basePath);
		basePaths.add(sourcePath);
		
		List<String> relativePaths = new ArrayList<>();
		
		for (String absolutePath :absolutePaths) {
			
			String relativePath = new PathConverter(absolutePath, basePaths).getRelativePath().getPath();
			relativePaths.add(relativePath);
		}
		
		logger.trace("... resulting relativePaths = {}");
		
		return relativePaths; 
	}
	
	/**
	 * Gets the absolute path (posix: /<storagePath>/<relativePath>)
	 * 
	 * @param relativePath relative path
	 * @return the absolute file depending on storage file system
	 */
	public String getAbsolutePath(String relativePath) {
		
		if (new PathConverter(relativePath).startsWithSlash()) return relativePath;
		
		return new PathConverter(getBasePath(), relativePath).getPath();				
	}
	
	/**
	 * Gets absolute paths (posix: /<storagePath>/<relativePath>)
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

		return new PosixStorageFile(basePath, relativePath);
	}

	/**
	 * Gets storage files
	 * 
	 * @return list of storage files
	 */
	@Override
	public List<StorageFile> getStorageFiles() {

		List<String> pathes = posixDAL.getFiles(basePath);
		List<StorageFile> storageFiles = new ArrayList<>();

		for (String path : pathes) {
			StorageFile storageFile = getStorageFile(getRelativePath(path));
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
	 * @throws IOException if file cannot be created
	 */
	@Override
	public StorageFile createStorageFile(String relativePath, String content) throws IOException {

		StorageFile storageFile = getStorageFile(relativePath);

		FileUtils fileUtils = new FileUtils(storageFile.getFullPath());
		if (!fileUtils.createFile(content)) throw new IOException("Cannot create file in POSIX Storage: " + relativePath);

		return storageFile;
	}

	/**
	 * Checks if file exists
	 * 
	 * @param storageFile Storage File to check
	 * @return true if file exists physically
	 */
	@Override
	public boolean fileExists(StorageFile storageFile) {

		return new File(storageFile.getFullPath()).isFile();
	}

	/**
	 * Checks if Storage file or directory is file (no slash at the end of the path)
	 * 
	 * @param storageFileOrDir storage file or directory
	 * @return true if storage file is file
	 */
	@Override
	public boolean isFile(StorageFile storageFileOrDir) {
		return new File(storageFileOrDir.getFullPath()).isFile();
	}

	/**
	 * Checks if storage file or directory is directory
	 * 
	 * @param storageFileOrDir storage file or directory
	 * @return true if storage file is file
	 */
	@Override
	public boolean isDirectory(StorageFile storageFileOrDir) {
		return new File(storageFileOrDir.getFullPath()).isDirectory();
	}

	/**
	 * Gets the file size
	 * 
	 * @param storageFile Storage file
	 * @return the file size of the storage file
	 */
	@Override
	public long getFileSize(StorageFile storageFile) {

		FileUtils fileUtils = new FileUtils(storageFile.getFullPath());
		return fileUtils.getFileSize();
	}
	
	/**
	 * Gets file content
	 * 
	 * @param storageFile storage file
	 * @return file content
	 */
	@Override
	public String getFileContent(StorageFile storageFile) throws IOException {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> getFileContent({})", storageFile.getFullPath());

		return posixDAL.getFileContent(storageFile.getFullPath());
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
		
		String uploadedAbsoluteFile = posixDAL.uploadFile(sourceFile.getFullPath(), targetFileOrDir.getFullPath());

		return getRelativePath(uploadedAbsoluteFile);
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
		
		List<String> uploadedAbsolutePaths = posixDAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return getRelativePath(uploadedAbsolutePaths);
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

		StorageFile targetFileOrDir = new PosixStorageFile(basePath, sourceFileOrDir.getRelativePath());
		
		List<String> uploadedAbsolutePaths = posixDAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return getRelativePath(uploadedAbsolutePaths);
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

		StorageFile targetFile = new PosixStorageFile(basePath, sourceFile.getRelativePath());
		
		String uploadedFile = posixDAL.uploadFile(sourceFile.getFullPath(), targetFile.getFullPath());

		return getRelativePath(uploadedFile);
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
		StorageFile targetFileOrDir = new PosixStorageFile(basePath, relativeSourceFileOrDir);
		
		List<String> uploadedAbsolutePaths = posixDAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return getRelativePath(uploadedAbsolutePaths);
	}

	/**
	 * @param relativeSourceFile relative path to source file
	 * @return uploaded to storage source file
	 * @throws IOException if file cannot be uploaded
	 */
	@Override
	public String uploadSourceFile(String relativeSourceFile) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({})", relativeSourceFile);

		StorageFile sourceFile = new PosixStorageFile(sourcePath, relativeSourceFile);
		StorageFile targetFile = new PosixStorageFile(basePath, relativeSourceFile);
		
		String uploadedFile = posixDAL.uploadFile(sourceFile.getFullPath(), targetFile.getFullPath());

		return getRelativePath(uploadedFile);
	}

	/**
	 * @param sourceFileOrDir source file or directory in the storage
	 * @param targetFileOrDir target file or directory
	 * @return list of downloaded files
	 * @throws IOException if file cannot be downloaded
	 */
	@Override
	public List<String> download(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> download({},{})", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return posixDAL.download(sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());
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

		return posixDAL.downloadFile(sourceFile.getFullPath(), targetFileOrDir.getFullPath());
	}

	/**
	 * Deletes file or directory recursively from the storage
	 * 
	 * @param storageFileOrDir Storage file or directory to delete
	 * @return list of deleted files from storage
	 * @throws IOException if file or directory cannot be deleted
	 */
	@Override
	public List<String> delete(StorageFile storageFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", storageFileOrDir.getFullPath());

		return posixDAL.delete(storageFileOrDir.getFullPath());
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

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteFile({})", storageFile.getFullPath());

		return posixDAL.deleteFile(storageFile.getFullPath());
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

		return posixDAL.delete(storageFileOrDir.getFullPath());
	}

	/**
	 * Adds file system prefix to the path
	 * 
	 * @param path path to extend
	 * @return file system prefix + "|" + path
	 */
	@Override
	public String addFSPrefix(String path) {

		return StorageType.POSIX.toString() + "|" + path; 
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
	
	/**
	 * Gets input stream from file
	 * 
	 * @param storageFile storage file
	 * @return input stream from file
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
	 * Gets relative path from absolute path
	 * 
	 * @param absolutePath absolute path
	 * @return relative path
	 */
	
	/*
	private String getRelativePath(String absolutePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getRelativePath({})", absolutePath);

		Path pathAbsolute = Paths.get(absolutePath);
		Path pathBase = Paths.get(basePath);
		Path pathRelative = pathBase.relativize(pathAbsolute);
		System.out.println("RelativePath: " + pathRelative + " from AbsolutePath: " + pathAbsolute);

		return pathRelative.toString();
	}
	*/

	/**
	 * Gets full bucket path
	 * 
	 * @return full bucket path
	 */
	private String getFullBucketPath() {
		return bucket.equals(StorageFile.NO_BUCKET) ? basePath : Paths.get(basePath, bucket).toString();
	}

}