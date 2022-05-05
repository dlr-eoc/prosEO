package de.dlr.proseo.storagemgr.version2.posix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.storagemgr.cache.FileUtils;
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

	private String basePath;
	private String bucket;
	
	private static String PREFIX = StorageType.POSIX.toString() + "|";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(PosixStorage.class);

	public PosixStorage() {
	}

	public PosixStorage(String basePath) {
		this.basePath = basePath;
		this.bucket = StorageFile.NO_BUCKET;

		createDirectories(basePath);
	}

	@Override
	public String getBasePath() {
		return basePath;
	}

	@Override
	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	@Override
	public String getBucket() {
		return bucket;
	}

	@Override
	public boolean fileExists(StorageFile storageFile) {

		return new File(storageFile.getFullPath()).isFile();
	}

	// TODO: maybe make recursive
	@Override
	public List<StorageFile> getStorageFiles() {

		File folder = new File(getFullBucketPath());
		File[] listOfFiles = folder.listFiles();
		List<StorageFile> files = new ArrayList<StorageFile>();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {

				String fullBucketPath = getFullBucketPath();
				String name = listOfFiles[i].getName();

				// String relativePath = relativizePath(getFullBucketPath(),
				// listOfFiles[i].getName());

				String relativePath = listOfFiles[i].getName();

				files.add(new PosixStorageFile(getFullBucketPath(), relativePath));
				// System.out.println("File " + listOfFiles[i].getName());
			}
		}
		return files;
	}

	@Override
	public String uploadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException {

		createParentDirectories(targetFileOrDir.getFullPath());

		String sourcePath = sourceFile.getFullPath();
		String targetPath = targetFileOrDir.getFullPath();
	
		Path sourceFilePath = new File(sourcePath).toPath();
		Path targetFilePath = new File(targetPath).toPath();

		try {
			Path copiedPath = Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
			return PREFIX + copiedPath.toString();
			
		} catch (IOException e) {
			if (logger.isTraceEnabled())
				logger.warn("Cannot upload file from " + sourcePath + " to " + targetPath + " ", e.getMessage());
			throw e;
		}
	}

	@Override
	public String downloadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException {

		createParentDirectories(targetFileOrDir.getFullPath());

		String sourcePath = sourceFile.getFullPath();
		String targetPath = targetFileOrDir.getFullPath();
		
		if (targetFileOrDir.isDirectory()) {			
			targetPath = Paths.get(targetPath, sourceFile.getFileName()).toString();
			createParentDirectories(targetPath);
		}
		
		Path sourceFilePath = new File(sourcePath).toPath();
		Path targetFilePath = new File(targetPath).toPath();

		try {
			Path copiedPath = Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
			return PREFIX + copiedPath.toString();
			
		} catch (Exception e) {
			e.printStackTrace();
			if (logger.isTraceEnabled())
				logger.warn("Cannot download file/folder from " + sourcePath + " to " + targetPath + " ",
						e.getMessage());
			throw e;
		}
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.POSIX;
	}

	private String getRelativePath(String absolutePath) {

		Path pathAbsolute = Paths.get(absolutePath);
		Path pathBase = Paths.get(basePath);
		Path pathRelative = pathBase.relativize(pathAbsolute);
		System.out.println("RelativePath: " + pathRelative + " from AbsolutePath: " + pathAbsolute);

		return pathRelative.toString();
	}

	private String getFullBucketPath() {
		return bucket.equals(StorageFile.NO_BUCKET) ? basePath : Paths.get(basePath, bucket).toString();
	}
	
	
	private String getAbsolutePath(String relativePath) {
		return  new PathConverter().convertToSlash(Paths.get(getFullBucketPath(), relativePath).toString());
	}

	private void createParentDirectories(String fullPath) {

		File targetFile = new File(fullPath);
		File parent = targetFile.getParentFile();

		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new IllegalStateException("Couldn't create dir: " + parent);
		}
	}

	public void createDirectories(String path) {
		File file = new File(path);

		if (!file.exists()) {
			file.mkdirs();
		}
	}

	@Override
	public long getFileSize(StorageFile storageFile) {

		FileUtils fileUtils = new FileUtils(storageFile.getFullPath());
		return fileUtils.getFileSize();
	}

	@Override
	public StorageFile getStorageFile(String relativePath) {

		return new PosixStorageFile(basePath, relativePath);
	}

	@Override
	public StorageFile createStorageFile(String relativePath, String content) {

		StorageFile storageFile = getStorageFile(relativePath);

		FileUtils fileUtils = new FileUtils(storageFile.getFullPath());
		fileUtils.createFile(content);

		return storageFile;
	}

	@Override
	public List<String> upload(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {
		
		if (logger.isTraceEnabled()) logger.trace(">>> upload({},())", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());
		
		String prefix = StorageType.POSIX.toString() + "|";
		List<String> uploadedFiles = new ArrayList<String>();
		
		if (isFile(sourceFileOrDir)) {
			
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
				
				StorageFile sourceFile = getStorageFile(getRelativePath(file.getAbsolutePath()));
				String uploadedFile = uploadFile(sourceFile, targetDir);
			
				uploadedFiles.add(prefix + uploadedFile);
			}
		}

		for (File file : files) {
			
			if (file.isDirectory()) {
				
				StorageFile sourceSubDir = getStorageFile(getRelativePath(file.getAbsolutePath()));
				List<String> subDirFiles = upload(sourceSubDir, targetDir);
				
				uploadedFiles.addAll(subDirFiles);
			}
		}
		
		return uploadedFiles;
	}
	
	
	@Override
	public List<String> download(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {
		
		if (logger.isTraceEnabled()) logger.trace(">>> upload({},())", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());
		
		String prefix = StorageType.POSIX.toString() + "|";
		List<String> downloadedFiles = new ArrayList<String>();
		
		if (isFile(sourceFileOrDir)) {
			
			downloadFile(sourceFileOrDir, targetFileOrDir);
			downloadedFiles.add(targetFileOrDir.getFullPath());
			return downloadedFiles;
		}

		StorageFile sourceDir = sourceFileOrDir; 
		StorageFile targetDir = targetFileOrDir; 
		File directory = new File(sourceDir.getFullPath());
		File[] files = directory.listFiles();
		Arrays.sort(files);

		for (File file : files) {
			
			if (file.isFile()) {
				
				StorageFile sourceFile = getStorageFile(getRelativePath(file.getAbsolutePath()));
				String downloadedFile = downloadFile(sourceFile, targetDir);
			
				downloadedFiles.add(prefix + downloadedFile);
			}
		}

		for (File file : files) {
			
			if (file.isDirectory()) {
				
				StorageFile sourceSubDir = getStorageFile(getRelativePath(file.getAbsolutePath()));
				StorageFile targetSubDir = new PosixStorageFile(targetDir);
				String targetSubDirPath = Paths.get(targetDir.getRelativePath(), file.getName()).toString();
				targetSubDir.setRelativePath(new PathConverter().addSlashAtEnd(targetSubDirPath));
				
				List<String> subDirFiles = download(sourceSubDir, targetSubDir);
				
				downloadedFiles.addAll(subDirFiles);
			}
		}
		
		return downloadedFiles;
	}

	

	// list of POSIX|absolutPath
	@Override
	public List<String> getFiles(String relativePath) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> getFiles({})", relativePath);

		String prefix = StorageType.POSIX.toString() + "|";
		List<String> returnFiles = new ArrayList<String>();
		
		String absolutePath = getAbsolutePath(relativePath);
		File directory = new File(absolutePath);

		if (directory.isFile()) {
			returnFiles.add(prefix + absolutePath);
		}

		File[] files = directory.listFiles();
		Arrays.sort(files);

		for (File file : files) {
			
			if (file.isFile()) {
				returnFiles.add(prefix + file.getAbsolutePath());
			}
		}

		for (File file : files) {
			
			if (file.isDirectory()) {
				String relativeDir =  new PathConverter().convertToSlash(Paths.get(relativePath, file.getName()).toString());
				List<String> dirFiles = getFiles(relativeDir);
				returnFiles.addAll(dirFiles);
			}
		}

		return returnFiles;
	}
	
	
	public String deleteFile(StorageFile storageFile) throws IOException  {
		
		boolean fileDeleted = new File(storageFile.getFullPath()).delete();
		
		if (fileDeleted) { 
			return storageFile.getFullPath(); 
		}
		
		throw new IOException("Cannot delete file: " + storageFile.getFullPath());  
	}

	@Override
	public List<String> delete(StorageFile sourceFileOrDir) throws IOException {
	
	if (logger.isTraceEnabled()) logger.trace(">>> delete({})", sourceFileOrDir.getFullPath());
		
		String prefix = StorageType.POSIX.toString() + "|";
		List<String> deletedFiles = new ArrayList<String>();
		
		if (isFile(sourceFileOrDir)) {
			
			String deletedFile = deleteFile(sourceFileOrDir);
			deletedFiles.add(deletedFile);
			return deletedFiles;
		}

		StorageFile sourceDir = sourceFileOrDir; 
	
		File directory = new File(sourceDir.getFullPath());
		File[] files = directory.listFiles();
		Arrays.sort(files);

		for (File file : files) {
			
			if (file.isFile()) {
				
				StorageFile sourceFile = getStorageFile(getRelativePath(file.getAbsolutePath()));
				String deletedFile = deleteFile(sourceFile);
				deletedFiles.add(deletedFile);
			}
		}

		for (File file : files) {
			
			if (file.isDirectory()) {
				
				StorageFile sourceSubDir = getStorageFile(getRelativePath(file.getAbsolutePath()));
				List<String> subDirFilesDeleted = delete(sourceSubDir);
				
				deletedFiles.addAll(subDirFilesDeleted);
			}
		}
		
		return deletedFiles;
	}
	
	@Override
	public boolean isFile(StorageFile storageFileOrDir) {
	     return new File(storageFileOrDir.getFullPath()).isFile();
	}

	@Override
	public boolean isDirectory(StorageFile storageFileOrDir) {
	     return new File(storageFileOrDir.getFullPath()).isDirectory();
	}
	
	
}
