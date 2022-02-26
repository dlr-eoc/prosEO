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
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
import de.dlr.proseo.storagemgr.version2.posix.PosixStorageFile;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class StorageProviderTest {
	

	@Autowired
	private TestUtils testUtils;
	
	@Autowired
	private StorageManagerConfiguration cfg;
	
	@Autowired
	private StorageProvider storageProvider;


	@Rule
	public TestName testName = new TestName();

	String testStoragePath; 
	String testCachePath;
	String testSourcePath; 
	String cachePath;
	
	String testRelativeStoragePath; 
	String testRelativeCachePath;
	String testRelativeSourcePath; 

	@PostConstruct
	private void init() {
		testRelativeSourcePath = testUtils.getRelativeTestSourcePath(); 
		testRelativeStoragePath = testUtils.getRelativeTestStoragePath(); 
		testRelativeCachePath = testUtils.getRelativeTestCachePath();
		
		testSourcePath = testUtils.getTestSourcePath(); 
		testStoragePath = testUtils.getTestStoragePath(); 
		testCachePath = testUtils.getTestCachePath();
		
		cachePath = testUtils.getCachePath();
	}
	
	
	@Test
	public void testPosixPosixProvider() {
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectories();

		String testFileName = "testfile.txt";
		String testFileContent = "some text inside file";
		
		String relativeSourceFilePath = Paths.get(testRelativeSourcePath, testFileName).toString();
		String relativeStorageFilePath = Paths.get(testRelativeStoragePath, testFileName).toString();
		String relativeCacheFilePath = Paths.get(testRelativeCachePath, testFileName).toString();

		String sourceFilePath = Paths.get(testSourcePath, testFileName).toString();
		String storageFilePath = Paths.get(testStoragePath, testFileName).toString();
		String cacheFilePath = Paths.get(testCachePath, testFileName).toString();
		
		System.out.println("Relative Source Path: " + relativeSourceFilePath); 
		System.out.println("Relative Storage Path: " + relativeStorageFilePath); 
		System.out.println("Relative Cache Path: " + relativeCacheFilePath); 
		
		
		TestUtils.createDirectory(testSourcePath);
		TestUtils.createDirectory(testStoragePath);
		TestUtils.createDirectory(testCachePath);

		TestUtils.createFile(sourceFilePath, testFileContent);

		TestUtils.printDirectoryTree(testSourcePath);

		assertTrue("File for upload has not been created: " + sourceFilePath, TestUtils.fileExists(sourceFilePath));
		
		
		Storage storage = storageProvider.getStorage();
				
		
		StorageFile sourceFile = storageProvider.getPosixStorageFile(testSourcePath, relativeSourceFilePath);
			
		StorageFile destFile = storageProvider.getStorageFile(relativeStorageFilePath);
		
		StorageTestUtils.printStorageFileList("Storage Files ", storage.getFiles());
		
		// TODO: add cache storage 
		// StorageTestUtils.printStorageFileList("Cache Files", cache.getFiles());
				
		try {
			storage.uploadFile(sourceFile, destFile);
			
		} catch (IOException e) {
			e.printStackTrace();
		} 

		assertTrue("File was not uploaded to storage: " + destFile.getFullPath(), storage.fileExists(destFile));

		StorageTestUtils.printStorageFileList("Storage Files ", storage.getFiles());
		
		// StorageTestUtils.printStorageFileList("Cache Files", cache.getFiles());
	}
	
	
	@Test
	public void testDefaultS3PosixProvider() {
		
	}
}
