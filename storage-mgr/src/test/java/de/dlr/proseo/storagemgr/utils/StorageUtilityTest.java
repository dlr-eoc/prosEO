/**
 * StorageUtilityTest.java
 * 
 * (C) 2024 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.utils;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import de.dlr.proseo.storagemgr.StorageProvider;
import de.dlr.proseo.storagemgr.BaseStorageTestUtils;
import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageType;

/**
 * Test class for Storage
 * 
 * @author Denys Chaykovskiy
 */
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class StorageUtilityTest {

	@Autowired
	private BaseStorageTestUtils storageTestUtils;
	
	@Autowired
	private StorageProvider storageProvider;

	@Test
	public void testUtility(TestInfo testInfo) throws IOException {

		TestUtils.printMethodName(this, testInfo);
				
		// change type to show another storage
		StorageType storageType = StorageType.POSIX; 
		storageProvider.setDefaultStorage(storageType);
		
		Storage storage = storageProvider.getStorage();
		Storage posixStorage = storageProvider.getDefaultStorage(StorageType.POSIX);
		Storage s3Storage = storageProvider.getDefaultStorage(StorageType.S3);
		
		// show s3 buckets
		TestUtils.printList("S3 Buckets:", s3Storage.getBuckets());
		
		// show s3 storage files 
		BaseStorageTestUtils.printStorageFiles("S3 before Action", s3Storage);
		
		// show posix storage files 
		BaseStorageTestUtils.printStorageFiles("POSIX before Action", posixStorage);
		
		// show default storage files 
		BaseStorageTestUtils.printStorageFiles("Default Storage BEFORE Action", storage);

		
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
		BaseStorageTestUtils.printStorageFiles("Default Storage AFTER Action", storage);
	}
} 
