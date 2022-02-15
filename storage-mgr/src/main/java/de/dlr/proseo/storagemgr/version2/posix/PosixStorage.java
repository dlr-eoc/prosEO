package de.dlr.proseo.storagemgr.version2.posix;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;

/**
 * Posix Storage
 * 
 * @author Denys Chaykovskiy
 *
 */
public class PosixStorage implements Storage {

	private String basePath;
	private String bucket;

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
	public boolean uploadFile(StorageFile sourceFile, StorageFile storageFile) {

		createParentDirectories(storageFile.getFullPath());

		Path sourcePath = new File(sourceFile.getFullPath()).toPath();
		Path storagePath = new File(storageFile.getFullPath()).toPath();

		try {
			Files.copy(sourcePath, storagePath, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			// TODO: add logger
			System.out
					.println("Cannot upload file FROM " + sourceFile.getFullPath() + " TO " + storageFile.getFullPath());
			return false;
		}

		return true;
	}

	@Override
	public boolean downloadFile(StorageFile storageFile, StorageFile targetFile) {

		createParentDirectories(targetFile.getFullPath());

		Path storagePath = new File(storageFile.getFullPath()).toPath();
		Path targetPath = new File(targetFile.getFullPath()).toPath();
		
		try {
			Files.copy(storagePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			// TODO: add logger
			System.out
					.println("Cannot download file FROM " + storageFile.getFullPath() + " TO " + targetFile.getFullPath());
			return false;
		}

		return true;
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

}
