package de.dlr.proseo.storagemgr.version2;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.utils.StorageProvider;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class StorageUtilityTest {

	@Autowired
	private TestUtils testUtils;

	@Autowired
	private StorageTestUtils storageTestUtils;
	
	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();

	String testCachePath;
	String cachePath;
	String testSourcePath;

	@PostConstruct
	private void init() {
		testCachePath = testUtils.getTestCachePath();
		cachePath = testUtils.getCachePath();

		testSourcePath = testUtils.getTestSourcePath();
	}

	@Test
	public void testUtility() throws IOException {

		TestUtils.printMethodName(this, testName);
				
		// change type to show another storage
		StorageType storageType = StorageType.POSIX; 
		storageProvider.setStorage(storageType);
		
		Storage storage = storageProvider.getStorage();
		Storage posixStorage = storageProvider.getStorage(StorageType.POSIX);
		Storage s3Storage = storageProvider.getStorage(StorageType.S3);
		
		// show s3 buckets
		TestUtils.printList("S3 Buckets:", s3Storage.getBuckets());
		
		// show s3 storage files 
		StorageTestUtils.printStorageFiles("S3 before Action", s3Storage);
		
		// show posix storage files 
		StorageTestUtils.printStorageFiles("POSIX before Action", posixStorage);
		
		// show default storage files 
		StorageTestUtils.printStorageFiles("Default Storage BEFORE Action", storage);

		
		// ACTIONS // 		
		String path = "test-utility-POSIX-upload/dir/ut2-upload.txt";

		// UPLOAD 
		storageTestUtils.createSourceFile(path);
		storage.uploadSourceFile(path);
		
		// DOWNLOAD 
		// StorageFile sourceFile = storageProvider.getStorageFile(path);
		// StorageFile destFile = storageProvider.getCacheFile(path);
	    // storage.downloadFile(sourceFile, destFile);

		// show storage files 
		// StorageTestUtils.printStorageFiles("After Action", storage);		
		
		// DELETE
		// storage.delete(path);
		
		// show default storage files 
		StorageTestUtils.printStorageFiles("Default Storage AFTER Action", storage);
	}
} 
