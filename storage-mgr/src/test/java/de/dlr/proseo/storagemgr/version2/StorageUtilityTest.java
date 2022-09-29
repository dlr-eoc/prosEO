package de.dlr.proseo.storagemgr.version2;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import de.dlr.proseo.storagemgr.version2.model.StorageType;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;

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
		
		TestUtils.createEmptyStorageDirectories();
		
		// change type to show another storage
		StorageType storageType = StorageType.S3; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);
		
		Storage storage = storageProvider.getStorage();
		Storage posixStorage = storageProvider.getStorage(StorageType.POSIX);
		Storage s3Storage = storageProvider.getStorage(StorageType.S3);
		
		// show s3 buckets
		TestUtils.printList("S3 Buckets:", s3Storage.getBuckets());
		
		// show default storage files 
		StorageTestUtils.printStorageFiles("Default Storage Before Action", storage);
		
		// show s3 storage files 
		StorageTestUtils.printStorageFiles("S3 Before Action", s3Storage);
		
		// show posix storage files 
		StorageTestUtils.printStorageFiles("POSIX Before Action", posixStorage);
		
		// action 
		// storage.delete("/Users");
		
		// show storage files 
		// StorageTestUtils.printStorageFiles("After Action", storage);		
	}
}
