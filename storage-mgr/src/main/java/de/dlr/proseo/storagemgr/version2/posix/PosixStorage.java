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

	private String basePath;
	private String bucket;

	private static String PREFIX = StorageType.POSIX.toString() + "|";

	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(PosixStorage.class);

	private PosixDAL posixDAL = new PosixDAL();

	public PosixStorage() {
	}

	public PosixStorage(String basePath) {
		this.basePath = basePath;
		this.bucket = StorageFile.NO_BUCKET;

		new FileUtils(basePath).createDirectories();
	}

	@Override
	public String getBasePath() {
		return basePath;
	}

	@Override
	public void setBucket(String bucket) {
		this.bucket = bucket;

		String bucketPath = Paths.get(basePath, bucket).toString();
		new FileUtils(bucketPath).createDirectories();
	}

	@Override
	public String getBucket() {
		return bucket;
	}

	@Override
	public boolean fileExists(StorageFile storageFile) {

		return new File(storageFile.getFullPath()).isFile();
	}

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

	@Override
	public String uploadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> uploadFile({},{})", sourceFile.getFullPath(), targetFileOrDir.getFullPath());

		return posixDAL.uploadFile(sourceFile.getFullPath(), targetFileOrDir.getFullPath());
	}

	@Override
	public String downloadFile(StorageFile sourceFile, StorageFile targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> downloadFile({},{})", sourceFile.getFullPath(), targetFileOrDir.getFullPath());

		return posixDAL.downloadFile(sourceFile.getFullPath(), targetFileOrDir.getFullPath());
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.POSIX;
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

		if (logger.isTraceEnabled())
			logger.trace(">>> upload({},())", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return posixDAL.upload(sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());
	}

	@Override
	public String addFSPrefix(String path) {

		String prefix = StorageType.POSIX.toString() + "|";
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

	@Override
	public List<String> download(StorageFile sourceFileOrDir, StorageFile targetFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> download({},{})", sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());

		return posixDAL.download(sourceFileOrDir.getFullPath(), targetFileOrDir.getFullPath());
	}

	@Override
	public List<String> getFiles(String relativePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getFiles({})", relativePath);
		
		String path = new PathConverter(basePath, relativePath).getPath(); 

		return posixDAL.getFiles(path);
	}

	public String deleteFile(StorageFile storageFile) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> deleteFile({})", storageFile.getFullPath());

		return posixDAL.deleteFile(storageFile.getFullPath());
	}

	@Override
	public List<String> delete(StorageFile storageFileOrDir) throws IOException {

		if (logger.isTraceEnabled())
			logger.trace(">>> delete({})", storageFileOrDir.getFullPath());

		return posixDAL.delete(storageFileOrDir.getFullPath());
	}

	@Override
	public boolean isFile(StorageFile storageFileOrDir) {
		return new File(storageFileOrDir.getFullPath()).isFile();
	}

	@Override
	public boolean isDirectory(StorageFile storageFileOrDir) {
		return new File(storageFileOrDir.getFullPath()).isDirectory();
	}

	@Override
	public List<String> getFiles() {
		return posixDAL.getFiles(basePath);
	}

	@Override
	public void deleteBucket(String bucket) throws IOException  {
		
		posixDAL.delete(getFullBucketPath());
	}

	private String getAbsolutePath(String relativePath) {

		if (logger.isTraceEnabled())
			logger.trace(">>> getAbsolutePath({})", relativePath);

		String path = Paths.get(getFullBucketPath(), relativePath).toString();
		return new PathConverter(path).convertToSlash().getPath();
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

	private String getFullBucketPath() {
		return bucket.equals(StorageFile.NO_BUCKET) ? basePath : Paths.get(basePath, bucket).toString();
	}
}
