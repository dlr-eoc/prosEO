package de.dlr.proseo.storagemgr.version2.posix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	@Override
	public List<StorageFile> getFiles() {

		File folder = new File(getFullBucketPath());
		File[] listOfFiles = folder.listFiles();
		List<StorageFile> files = new ArrayList<StorageFile>();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				String relativePath = relativizePath(getFullBucketPath(), listOfFiles[i].getName());

				files.add(new PosixStorageFile(getFullBucketPath(), relativePath));
				System.out.println("File " + listOfFiles[i].getName());
			}
		}
		return files;
	}

	@Override
	public void uploadFile(StorageFile sourceFile, StorageFile targetFile) throws IOException {

		createParentDirectories(targetFile.getFullPath());

		String sourcePath = sourceFile.getFullPath();
		String targetPath = targetFile.getFullPath();

		Path sourceFilePath = new File(sourcePath).toPath();
		Path targetFilePath = new File(targetPath).toPath();

		try {
			Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);

		} catch (IOException e) {
			if (logger.isTraceEnabled())
				logger.warn("Cannot upload file from " + sourcePath + " to " + targetPath + " ", e.getMessage());
			throw e;
		}
	}

	@Override
	public void downloadFile(StorageFile sourceFile, StorageFile targetFile) throws IOException {

		createParentDirectories(targetFile.getFullPath());

		String sourcePath = sourceFile.getFullPath();
		String targetPath = targetFile.getFullPath();

		Path sourceFilePath = new File(sourcePath).toPath();
		Path targetFilePath = new File(targetPath).toPath();

		try {
			Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			if (logger.isTraceEnabled())
				logger.warn("Cannot upload file from " + sourcePath + " to " + targetPath + " ", e.getMessage());
			throw e;
		}
	}

	@Override
	public StorageType getStorageType() {
		return StorageType.POSIX;
	}

	private String relativizePath(String absolutePath, String basePath) {

		Path pathAbsolute = Paths.get(absolutePath);
		Path pathBase = Paths.get(basePath);
		Path pathRelative = pathBase.relativize(pathAbsolute);
		System.out.println(pathRelative);

		return pathRelative.toString();
	}

	private String getFullBucketPath() {
		return bucket.equals(StorageFile.NO_BUCKET) ? basePath : Paths.get(basePath, bucket).toString();
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public StorageFile getFile(String relativePath) {
		
		return new PosixStorageFile(basePath, relativePath);
	}

}
