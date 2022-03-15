package de.dlr.proseo.storagemgr;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import javax.annotation.PostConstruct;

import org.junit.Rule;
import org.junit.rules.TestName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;

/**
 * @author Denys Chaykovskiy
 *
 */
@Component
public class StorageTestUtils {

	@Autowired
	private TestUtils testUtils;

	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();

	String storagePath;
	String cachePath;
	String sourcePath;

	@PostConstruct
	private void init() {

		sourcePath = testUtils.getSourcePath();
		storagePath = testUtils.getStoragePath();
		cachePath = testUtils.getCachePath();

		theTestUtils = this;
	}

	private static StorageTestUtils theTestUtils;

	public static StorageTestUtils getInstance() {

		return theTestUtils;
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
	 * @param message
	 * @param arrayList
	 */
	public String createSourceFile(String relativePath) {

		String testFileContent = "some text inside file";
		String sourceFilePath = Paths.get(sourcePath, relativePath).toString();

		TestUtils.createFile(sourceFilePath, testFileContent);

		assertTrue("File for upload has not been created: " + sourceFilePath, TestUtils.fileExists(sourceFilePath));

		System.out.println("File " + relativePath + " successfully created in Source");
		
		return sourceFilePath; 
	}
	
	
	public String getAbsoluteSourcePath(String relativePath) { 
		
		return Paths.get(sourcePath, relativePath).toString();
	}

	public void uploadToPosixStorage(String relativePath) {

		Storage storage = storageProvider.getStorage();

		StorageFile sourceFile = storageProvider.getPosixFile(sourcePath, relativePath);
		StorageFile destFile = storageProvider.getStorageFile(relativePath);

		try {
			storage.uploadFile(sourceFile, destFile);

		} catch (IOException e) {
			System.out.println("Cannot upload: " + e.getMessage());
		}

		assertTrue("File was not uploaded to storage: " + relativePath, TestUtils.fileExists(destFile.getFullPath()));

		System.out.println("File " + relativePath + " successfully uploaded to Posix Storage");
	}

	public void downloadFromPosixStorage(String relativePath) {

		Storage storage = storageProvider.getStorage();

		StorageFile sourceFile = storageProvider.getStorageFile(relativePath);
		StorageFile destFile = storageProvider.getCacheFile(relativePath);

		try {
			storage.downloadFile(sourceFile, destFile);

		} catch (IOException e) {
			System.out.println("Cannot download: " + e.getMessage());
		}

		assertTrue("File was not downloaded from storage: " + relativePath,
				TestUtils.fileExists(destFile.getFullPath()));

		System.out.println("File " + relativePath + " successfully downloaded from Posix Storage to Cache");
	}

	public void printPosixStorage() {

		Storage storage = storageProvider.getStorage();
		StorageTestUtils.printStorageFileList("Storage Files: ", storage.getFiles());
	}

	public void printCache() {

		TestUtils.printDirectoryTree(cachePath);
	}

	public void printSource() {

		TestUtils.printDirectoryTree(sourcePath);
	}

}
