package de.dlr.proseo.storagemgr;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.posix.PosixDAL;
import de.dlr.proseo.storagemgr.posix.PosixStorageFile;
import de.dlr.proseo.storagemgr.version2.PathConverter;

/**
 * @author Denys Chaykovskiy
 *
 */
public class BaseStorageTestUtils {

	protected String sourcePath;
	protected String storagePath;
	protected String cachePath;

	public StorageFile getSourceFile(String relativePath) {

		return new PosixStorageFile(sourcePath, relativePath);
	}

	public StorageFile getStorageFile(String relativePath) {

		return new PosixStorageFile(storagePath, relativePath);
	}

	public StorageFile getCacheFile(String relativePath) {

		return new PosixStorageFile(cachePath, relativePath);
	}

	/**
	 * @param message
	 * @param arrayList
	 */
	public static void printStorageFileList(String message, List<StorageFile> list) {

		System.out.println();
		System.out.println(message + " SIZE: " + list.size());
		for (StorageFile element : list) {

			System.out.println(" - " + element.getRelativePath());
		}
		System.out.println();
	}

	/**
	 * Creates file in source and returns absolute path of created file
	 * 
	 * @param relativePath
	 * @return absolute path of created file
	 */
	public String createSourceFile(String relativePath) {
		
		String testFileContent = "some text inside file";		
		return createSourceFile(relativePath, testFileContent);
	}
	
	/**
	 * Creates file in source and returns absolute path of created file
	 * 
	 * @param relativePath
	 * @param fileContent
	 * @return absolute path of created file
	 */
	public String createSourceFile(String relativePath, String fileContent) {

		String testFileContent = fileContent;
		String path = Paths.get(sourcePath, relativePath).toString();
		String sourceFilePath = new PathConverter(path).convertToSlash().getPath();

		TestUtils.createFile(sourceFilePath, testFileContent);

		assertTrue("File for upload in Source has not been created: " + sourceFilePath, TestUtils.fileExists(sourceFilePath));

		System.out.println("File " + relativePath + " successfully created in Source");

		return sourceFilePath;
	}
	
	public String getAbsoluteSourcePath(String relativePath) {

		String path = Paths.get(sourcePath, relativePath).toString();
		return new PathConverter(path).convertToSlash().getPath();
	}

	public void uploadToPosixStorage(String relativePath) {

		PosixDAL posixDAL = new PosixDAL();

		StorageFile sourceFile = getSourceFile(relativePath);
		StorageFile destFile = getStorageFile(relativePath);

		try {
			posixDAL.uploadFile(sourceFile.getFullPath(), destFile.getFullPath());

		} catch (IOException e) {
			System.out.println("Cannot upload: " + e.getMessage());
		}

		assertTrue("File was not uploaded to storage: " + relativePath, TestUtils.fileExists(destFile.getFullPath()));

		System.out.println("File " + relativePath + " successfully uploaded to Posix Storage");
	}

	public void downloadFromPosixStorage(String relativePath) {

		PosixDAL posixDAL = new PosixDAL();

		StorageFile sourceFile = getStorageFile(relativePath);
		StorageFile destFile = getCacheFile(relativePath);

		try {
			posixDAL.downloadFile(sourceFile.getFullPath(), destFile.getFullPath());

		} catch (IOException e) {
			System.out.println("Cannot download: " + e.getMessage());
		}

		assertTrue("File was not downloaded from storage: " + relativePath,
				TestUtils.fileExists(destFile.getFullPath()));

		System.out.println("File " + relativePath + " successfully downloaded from Posix Storage to Cache");
	}

	public void printSource() {

		TestUtils.printDirectoryTree(sourcePath);
	}

	public void printPosixStorage() {

		TestUtils.printDirectoryTree(storagePath);
	}

	public void printCache() {

		TestUtils.printDirectoryTree(cachePath);
	}

	public String getStoragePath() {
		return storagePath;
	}

	public String getCachePath() {
		return cachePath;
	}

	public String getSourcePath() {
		return sourcePath;
	}
}
