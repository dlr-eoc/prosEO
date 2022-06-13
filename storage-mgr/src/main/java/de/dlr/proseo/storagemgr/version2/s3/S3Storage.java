package de.dlr.proseo.storagemgr.version2.s3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.version2.FileUtils;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.model.BucketsStorage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;
import de.dlr.proseo.storagemgr.version2.posix.PosixStorageFile;

/**
 * S3 Storage
 * 
 * @author Denys Chaykovskiy
 *
 */
public class S3Storage implements BucketsStorage {

	private S3DAL s3DAL;
	private String basePath;
	private String sourcePath; 

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3Storage.class);

	public S3Storage(String basePath, String sourcePath, String s3AccessKey, String s3SecretAccessKey, String bucket) {
		s3DAL = new S3DAL(s3AccessKey, s3SecretAccessKey, bucket);
		this.basePath = basePath;
		this.sourcePath = sourcePath; 
	}

	public S3Storage(String basePath, String sourcePath, String s3AccessKey, String s3SecretAccessKey, String s3Region,
			String s3EndPoint, String bucket) {
		s3DAL = new S3DAL(s3AccessKey, s3SecretAccessKey, s3Region, s3EndPoint, bucket);
		this.basePath = basePath;
		this.sourcePath = sourcePath; 
	}

	@Override
	public String getBasePath() {
		return basePath;
	}
	
	@Override
	public String getSourcePath() {
		return sourcePath;
	}

	@Override
	public void setBucket(String bucket) {
		s3DAL.setBucket(bucket);
	}

	@Override
	public String getBucket() {
		return s3DAL.getBucket();
	}

	@Override
	public boolean fileExists(StorageFile storageFile) {
		return s3DAL.fileExists(storageFile.getRelativePath());
	}

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

	@Override
	public String uploadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({},{})", sourceFile.getFullPath(), targetFileOrDir.getFullPath());

		return s3DAL.uploadFile(sourceFile.getFullPath(), targetFileOrDir.getRelativePath());
	}
	
	@Override
	public List<String> upload(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},{})", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return s3DAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getRelativePath());
	}
	
	@Override
	public List<String> upload(StorageFile sourceFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({})", sourceFileOrDir.getFullPath());
		
		StorageFile targetFileOrDir = new S3StorageFile(s3DAL.getBucket(), sourceFileOrDir.getRelativePath());

		return s3DAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getRelativePath());
	}
	
	@Override
	public String uploadFile(StorageFile sourceFile) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({})", sourceFile.getFullPath());
		
		StorageFile targetFile = new S3StorageFile(s3DAL.getBucket(), sourceFile.getRelativePath());

		return s3DAL.uploadFile(sourceFile.getFullPath(), targetFile.getRelativePath());
	} 
	
	@Override
	public List<String> uploadSourceFileOrDir(String relativeSourceFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({})", relativeSourceFileOrDir);
		
		StorageFile sourceFileOrDir = new PosixStorageFile(sourcePath, relativeSourceFileOrDir);
		StorageFile targetFileOrDir = new S3StorageFile(s3DAL.getBucket(), sourceFileOrDir.getRelativePath());

		return s3DAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getRelativePath());
	}
	
	@Override
	public String uploadSourceFile(String relativeSourceFile) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({})", relativeSourceFile);
		
		StorageFile sourceFile = new PosixStorageFile(sourcePath, relativeSourceFile);		
		StorageFile targetFile = new S3StorageFile(s3DAL.getBucket(), sourceFile.getRelativePath());

		return s3DAL.uploadFile(sourceFile.getFullPath(), sourceFile.getFullPath());
	} 
	
	@Override
	public String downloadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadFile({},{})", sourceFile.getFullPath(), targetFileOrDir.getFullPath());

		return s3DAL.downloadFile(sourceFile.getRelativePath(), targetFileOrDir.getFullPath());
	}
	
	@Override
	public List<String> download(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> download({},{})", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return s3DAL.download(sourceFileOrDir.getRelativePath(), targetFileOrDir.getFullPath());
	}

	@Override
	public String deleteFile(StorageFile storageFile) throws IOException {
		return s3DAL.deleteFile(storageFile.getRelativePath());
	}
	
	public StorageFile getAbsoluteStorageFile(String absolutePath) {
		
		String basePath = new PathConverter(absolutePath).getFirstFolder().addSlashAtBegin().getPath();
		String relativePath = new PathConverter(absolutePath).removeFirstFolder().getPath();

		return new PosixStorageFile(basePath, relativePath);
	}

	@Override
	public List<String> getFiles() {
		return s3DAL.getFiles();
	}

	@Override
	public void deleteBucket(String bucketName) {
		s3DAL.deleteBucket(bucketName);
	}

	@Override
	public List<String> getBuckets() {
		return s3DAL.getBuckets();
	}

	@Override
	public boolean bucketExists(String bucketName) {
		return s3DAL.bucketExists(bucketName);
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.S3;
	}

	@Override
	public long getFileSize(StorageFile storageFile) {
		return s3DAL.getFileSize(storageFile.getRelativePath());
	}

	@Override
	public StorageFile getStorageFile(String relativePath) {
		return new S3StorageFile(s3DAL.getBucket(), relativePath);
	}

	@Override
	public StorageFile createStorageFile(String relativePath, String content) {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> createStorageFile({},{})", relativePath, content);

		String path = Paths.get(basePath, relativePath).toString();

		path = new PathConverter(path).verifyAbsolutePath().getPath();

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
	
	@Override
	public String addFSPrefix(String path) {

		String prefix = StorageType.S3.toString() + "|";
		return prefix + path;
	}

	@Override
	public List<String> addFSPrefix(List<String> paths) {

		List<String> pathsWithPrefix = new ArrayList<>();

		for (String path : paths) {
			String pathWithPrefix = addFSPrefix(path);
			pathsWithPrefix.add(pathWithPrefix);
		}

		return pathsWithPrefix;
	}

	
	
	private boolean isExistingPosixFile(String path) {
		
		File f = new File(path);
		return (f.exists() && !f.isDirectory()) ? true : false;
	}
	
	
	private String getRelativePath(String absolutePath) {
		
		if (logger.isTraceEnabled())
			logger.trace(">>> getRelativePath({})", absolutePath);

		Path pathAbsolute = Paths.get(absolutePath);
		Path pathBase = Paths.get(basePath);
		Path pathRelative = pathBase.relativize(pathAbsolute);
		System.out.println("RelativePath: " + pathRelative + " from AbsolutePath: " + pathAbsolute);

		return pathRelative.toString();
	}

	@Override
	public List<String> getFiles(String folder) {

		return s3DAL.getFiles(folder);
	}

	@Override
	public List<String> delete(StorageFile storageFileOrDir) {

		return s3DAL.delete(storageFileOrDir.getRelativePath());
	}
	
	

	@Override
	public boolean isFile(StorageFile storageFileOrDir) {
		return s3DAL.fileExists(storageFileOrDir.getRelativePath());
	}

	@Override
	public boolean isDirectory(StorageFile storageFileOrDir) {
		return !isFile(storageFileOrDir);
	}

	
}
