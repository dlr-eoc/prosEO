package de.dlr.proseo.storagemgr.version2;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
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
import de.dlr.proseo.storagemgr.version2.posix.PosixStorageFile;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class StorageProviderTest {
	
	
	@Test
	public void testPosixPosixProvider() {
		
		StorageProvider storageProvider = StorageProvider.getInstance(); //(StorageProviderProfile.INTERNAL_POSIX_EXTERNAL_POSIX); 
		
		Storage storage = storageProvider.getStorage();
		
		String internalStoragePath = "E:\\test\\internalStorage\\";
		String externalStoragePath = "E:\\test\\externalStorage\\";
		String fileName = "source.txt"; 
		String testFileContent = "some text inside file";
		
		StorageFile sourceFile = new PosixStorageFile(internalStoragePath, fileName);
		StorageFile destFile = new PosixStorageFile(externalStoragePath, fileName);
		
		StorageTestUtils.printStorageFileList("Storage Files ", storage.getFiles());
		
		// TODO: add cache storage 
		// StorageTestUtils.printStorageFileList("Cache Files", cache.getFiles());
		
		assertTrue("File for upload has not been created: " + sourceFile.getFullPath(), TestUtils.fileExists(sourceFile.getFullPath()));
		
		try {
			storage.uploadFile(sourceFile, destFile);
			
		} catch (IOException e) {
			e.printStackTrace();
		} 

		assertTrue("File was not uploaded to external storage: " + destFile.getFullPath(), TestUtils.fileExists(destFile.getFullPath()));

		StorageTestUtils.printStorageFileList("Storage Files ", storage.getFiles());
		// StorageTestUtils.printStorageFileList("Cache Files", cache.getFiles());
	}
	
	
	@Test
	public void testDefaultS3PosixProvider() {
		
	}
}
