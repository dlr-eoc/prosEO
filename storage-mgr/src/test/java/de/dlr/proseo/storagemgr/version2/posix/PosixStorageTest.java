package de.dlr.proseo.storagemgr.version2.posix;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class PosixStorageTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private TestUtils testUtils;
	
	@Autowired
	private StorageTestUtils storageTestUtils;
	
	@Autowired
	private StorageProvider storageProvider;

	
	@Rule
	public TestName testName = new TestName();
	

	@Test
	public void testPosixPosixUpload() throws IOException {
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();
		
		String prefix = "files/"; 
		
		List<String> pathes = new ArrayList<>();
		pathes.add(prefix + "file1.txt");
		pathes.add(prefix + "file2.txt");
		pathes.add(prefix + "dir/file3.txt");
		
		for (String path : pathes) {
			
			storageTestUtils.createSourceFile(path);
		}
				
		storageTestUtils.printPosixStorage();
		
		StorageFile sourceDir = storageProvider.getSourceFile(prefix);
		StorageFile targetDir = storageProvider.getStorageFile(prefix); 
		
		List<String> uploadedPathes = storageProvider.getStorage().upload(sourceDir, targetDir);
				
		for (String uploadedPath : uploadedPathes) { 
			
			System.out.println("Uploaded: " + uploadedPath);
		}
	}
	
	
	@Test
	public void testPosixPosixDownload() throws IOException {
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();
		
		String prefix = "files/"; 
		
		List<String> pathes = new ArrayList<>();
		pathes.add(prefix + "file1.txt");
		pathes.add(prefix + "file2.txt");
		pathes.add(prefix + "dir/file3.txt");
		
		for (String path : pathes) {
			
			storageTestUtils.createSourceFile(path);
			storageTestUtils.uploadToPosixStorage(path);
		}
				
		storageTestUtils.printPosixStorage();
		
		StorageFile sourceDir = storageProvider.getStorageFile(prefix);
		StorageFile targetDir = storageProvider.getCacheFile(prefix); 
		
		List<String> downloadedPathes = storageProvider.getStorage().download(sourceDir, targetDir);
				
		for (String downloadedPath : downloadedPathes) { 
			
			System.out.println("Downloaded: " + downloadedPath);
		}
	}
	
	
/*	
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
*/

}
