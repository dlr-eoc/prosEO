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

import de.dlr.proseo.storagemgr.cache.FileUtils;
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

	private S3DataAccessLayer s3DAL;
	private String basePath;

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(S3Storage.class);

	public S3Storage(String basePath, String s3AccessKey, String s3SecretAccessKey, String bucket) {
		s3DAL = new S3DataAccessLayer(s3AccessKey, s3SecretAccessKey, bucket);
		this.basePath = basePath;
	}

	public S3Storage(String basePath, String s3AccessKey, String s3SecretAccessKey, String s3Region,
			String s3EndPoint, String bucket) {
		s3DAL = new S3DataAccessLayer(s3AccessKey, s3SecretAccessKey, s3Region, s3EndPoint, bucket);
		this.basePath = basePath;
	}

	@Override
	public String getBasePath() {
		return basePath;
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

		String sourcePath = sourceFile.getFullPath();
		String targetPath = targetFileOrDir.getRelativePath();
		
		if (targetFileOrDir.isDirectory()) { 
			
			targetPath += sourceFile.getFileName();
		}
		
		s3DAL.setBucket(targetFileOrDir.getBucket());

		try {
			return s3DAL.uploadFile(sourcePath, targetPath);
		} catch (IOException e) {
			e.printStackTrace();
			if (logger.isTraceEnabled())
				logger.warn("Cannot upload file from " + sourcePath + " to " + targetPath + " ", e.getMessage());
			throw e;
		}
	}

	@Override
	public String downloadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException {

		String sourcePath =  new PathConverter(sourceFile.getRelativePath()).posixToS3Path().getPath();
		String targetPath = targetFileOrDir.getFullPath();

		s3DAL.setBucket(sourceFile.getBucket());

		try {
			return s3DAL.downloadFile(sourcePath, targetPath);
		} catch (IOException e) {
			e.printStackTrace();
			if (logger.isTraceEnabled())
				logger.warn("Cannot download file from " + sourcePath + " to " + targetPath + " ", e.getMessage());
			throw e;
		}
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
	public void deleteBucket(String bucketName) {
		s3DAL.deleteBucket(bucketName);
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
	public List<String> upload(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},())", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		String prefix = StorageType.S3.toString() + "|";
		List<String> uploadedFiles = new ArrayList<String>();

		if (isExistingPosixFile(sourceFileOrDir.getFullPath())) {

			uploadFile(sourceFileOrDir, targetFileOrDir);
			uploadedFiles.add(targetFileOrDir.getFullPath());
			return uploadedFiles;
		}

		StorageFile sourceDir = sourceFileOrDir;
		StorageFile targetDir = targetFileOrDir;
		File directory = new File(sourceDir.getFullPath());
		File[] files = directory.listFiles();
		Arrays.sort(files);

		for (File file : files) {

			if (file.isFile()) {

				StorageFile sourceFile = getAbsoluteStorageFile(file.getAbsolutePath());
				String uploadedFile = uploadFile(sourceFile, targetDir);

				uploadedFiles.add(prefix + uploadedFile);
			}
		}

		for (File file : files) {

			if (file.isDirectory()) {
				
				StorageFile sourceSubDir = getAbsoluteStorageFile(file.getAbsolutePath() + "/");
				
				String targetSubDirPath = targetDir.getRelativePath() + "/" + sourceSubDir.getFileName() + "/"; 

				targetSubDirPath = new PathConverter(targetSubDirPath).removeDoubleSlash().getPath();
				
				StorageFile targetSubDir = new S3StorageFile(targetDir);
				targetSubDir.setRelativePath(targetSubDirPath);
				
				List<String> subDirFiles = upload(sourceSubDir, targetSubDir);

				uploadedFiles.addAll(subDirFiles);
			}
		}

		return uploadedFiles;

	}
	
	
	private boolean isExistingPosixFile(String path) {
		
		File f = new File(path);
		return (f.exists() && !f.isDirectory()) ? true : false;
	}
	
	
	private String getRelativePath(String absolutePath) {

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

		s3DAL.deleteFile(storageFileOrDir.getRelativePath());

		// TODO: Change S3 DAL
		return null;
	}
	
	

	@Override
	public boolean isFile(StorageFile storageFileOrDir) {
		return s3DAL.fileExists(storageFileOrDir.getRelativePath());
	}

	@Override
	public boolean isDirectory(StorageFile storageFileOrDir) {
		return !isFile(storageFileOrDir);
	}

	@Override
	public List<String> download(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String deleteFile(StorageFile storageFileOrDir) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public StorageFile getAbsoluteStorageFile(String absolutePath) {
		
		String basePath = new PathConverter(absolutePath).getFirstFolder().getPath();
		String relativePath = new PathConverter(absolutePath).removeFirstFolder().getPath();

		return new PosixStorageFile(basePath, relativePath);
	}

	@Override
	public List<String> getFiles() {
		return s3DAL.getFiles();
	}
}
