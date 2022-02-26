package de.dlr.proseo.storagemgr.version2.posix;

import static org.junit.Assert.*;

import java.io.IOException;

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
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class PosixStorageTest {
	
	@Autowired
	private TestUtils testUtils;
	
	@Autowired
	private StorageManagerConfiguration cfg;


	@Rule
	public TestName testName = new TestName();
	

	@Test
	public void testPosixPosixUploadFile() {
		
		String internalStoragePath = "E:\\test\\internalStorage\\";
		String externalStoragePath = "E:\\test\\externalStorage\\";
		String fileName = "source.txt"; 
		String testFileContent = "some text inside file";
		
		StorageFile sourceFile = new PosixStorageFile(internalStoragePath, fileName);
		StorageFile destFile = new PosixStorageFile(externalStoragePath, fileName);
		
		Storage externalStorage = new PosixStorage(internalStoragePath);
		Storage internalStorage = new PosixStorage(externalStoragePath); 
		
		//TestUtils.createFile(sourceFile.getFullPath(), testFileContent);
		
		TestUtils.printDirectoryTree(internalStoragePath);
		TestUtils.printDirectoryTree(externalStoragePath);

		assertTrue("File for upload has not been created: " + sourceFile.getFullPath(), TestUtils.fileExists(sourceFile.getFullPath()));
		
		try {
			externalStorage.uploadFile(sourceFile, destFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		TestUtils.printDirectoryTree(internalStoragePath);
		TestUtils.printDirectoryTree(externalStoragePath);
	
	}

}
