/**
 * StorageProviderTest.java
 * 
 * (C) 2024 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Paths;

import jakarta.annotation.PostConstruct;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageProvider;
import de.dlr.proseo.storagemgr.BaseStorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;

/**
 * Test class for StorageProvider
 * 
 * @author Denys Chaykovskiy
 */
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class StorageProviderTest {

	@Autowired
	private TestUtils testUtils;
	
	@Autowired
	private BaseStorageTestUtils storageTestUtils;
	
	@Autowired
	private StorageProvider storageProvider;

	private String storagePath;
	private String cachePath;
	private String sourcePath;

	@PostConstruct
	private void init() {

		sourcePath = testUtils.getSourcePath();
		storagePath = testUtils.getStoragePath();
		cachePath = testUtils.getCachePath();
	}

	@Test
	public void testPosixPosixProvider(TestInfo testInfo) throws IOException {

		TestUtils.printMethodName(this, testInfo);
		testUtils.createEmptyStorageDirectories();

		String testFileName = "testfile.txt";
		String testFileContent = "some text inside file";

		String sourceFilePath = Paths.get(sourcePath, testFileName).toString();
		String storageFilePath = Paths.get(storagePath, testFileName).toString();
		String cacheFilePath = Paths.get(cachePath, testFileName).toString();

		System.out.println("Source Path:  " + sourceFilePath);
		System.out.println("Storage Path: " + storageFilePath);
		System.out.println("Cache Path:   " + cacheFilePath);

		System.out.println();

		TestUtils.createFile(sourceFilePath, testFileContent);
		
		TestUtils.printDirectoryTree(sourcePath);
		
		assertTrue(TestUtils.fileExists(sourceFilePath), "File for upload has not been created: " + sourceFilePath);

		StorageType storageType = StorageType.POSIX; 
		storageProvider.setDefaultStorage(storageType);
		Storage storage = storageProvider.getStorage();

		// -------------------- upload ----------------------------

		StorageFile sourceFile = storageProvider.getPosixFile(sourcePath, testFileName);
		StorageFile destFile = storageProvider.getStorageFile(storage, testFileName);

		try {
			storage.uploadFile(sourceFile, destFile);

		} catch (IOException e) {
			System.out.println("Cannot upload: " + e.getMessage());
		}

		assertTrue(TestUtils.fileExists(storageFilePath), "File was not uploaded to storage: " + storageFilePath);

		BaseStorageTestUtils.printStorageFileList("Storage Files (should be 1 file) ", storage.getStorageFiles());

		// ----------------------- download --------------------------

		sourceFile = storageProvider.getStorageFile(storage, testFileName);
		destFile = storageProvider.getCacheFile(testFileName);
		
		try {
			storage.downloadFile(sourceFile, destFile);

		} catch (IOException e) {
			System.out.println("Cannot download: " + e.getMessage());
		}
				
		TestUtils.printDirectoryTree(cachePath);
		
		assertTrue(TestUtils.fileExists(cacheFilePath), "File was not downloaded from storage: " + cacheFilePath);
		
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertEquals(storageType, realStorageType, "Expected: SM POSIX, " + " Exists: " + realStorageType);
		
		testUtils.deleteStorageDirectories();
	}
	
	
	@Test 
	public void probaTest(TestInfo testInfo) throws Exception { 
		
		TestUtils.printMethodName(this, testInfo);
		testUtils.createEmptyStorageDirectories();
		
		String testFile = "probaTest.txt"; 
		
		storageTestUtils.createSourceFile(testFile);
		storageTestUtils.printSource();
		
		storageTestUtils.uploadToPosixStorage(testFile);
		storageTestUtils.printPosixStorage();
		
		storageTestUtils.downloadFromPosixStorage(testFile);
		storageTestUtils.printCache();
		

		testUtils.deleteStorageDirectories();
	}
}
