package de.dlr.proseo.storagemgr.version2;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class StorageProviderTest {

	@Autowired
	private TestUtils testUtils;
	
	@Autowired
	private StorageTestUtils storageTestUtils;
	
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
	}

	@Test
	public void testPosixPosixProvider() {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();

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
		
		assertTrue("File for upload has not been created: " + sourceFilePath, TestUtils.fileExists(sourceFilePath));

		// StorageProvider storageProvider = new StorageProvider();
		Storage storage = storageProvider.getStorage();
		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);

		// -------------------- upload ----------------------------

		StorageFile sourceFile = storageProvider.getPosixFile(sourcePath, testFileName);
		StorageFile destFile = storageProvider.getStorageFile(testFileName);

		try {
			storage.uploadFile(sourceFile, destFile);

		} catch (IOException e) {
			System.out.println("Cannot upload: " + e.getMessage());
		}

		assertTrue("File was not uploaded to storage: " + storageFilePath, TestUtils.fileExists(storageFilePath));

		StorageTestUtils.printStorageFileList("Storage Files (should be 1 file) ", storage.getStorageFiles());

		// ----------------------- download --------------------------

		sourceFile = storageProvider.getStorageFile(testFileName);
		destFile = storageProvider.getCacheFile(testFileName);
		
		try {
			storage.downloadFile(sourceFile, destFile);

		} catch (IOException e) {
			System.out.println("Cannot download: " + e.getMessage());
		}
				
		TestUtils.printDirectoryTree(cachePath);
		
		assertTrue("File was not downloaded from storage: " + cacheFilePath, TestUtils.fileExists(cacheFilePath));
		
		assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
		
		TestUtils.deleteStorageDirectories();
	}
	
	
	@Test 
	public void probaTest() throws Exception { 
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();
		
		String testFile = "probaTest.txt"; 
		
		storageTestUtils.createSourceFile(testFile);
		storageTestUtils.printSource();
		
		storageTestUtils.uploadToPosixStorage(testFile);
		storageTestUtils.printPosixStorage();
		
		storageTestUtils.downloadFromPosixStorage(testFile);
		storageTestUtils.printCache();
		

		TestUtils.deleteStorageDirectories();
	}

	@Test
	public void testDefaultS3PosixProvider() {

	}
}
