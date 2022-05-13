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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class PosixDALTest {

	@Rule
	public TestName testName = new TestName();
	
	@Autowired
	private TestUtils testUtils;
	
	@Autowired
	private StorageTestUtils storageTestUtils;
	
	@Test
	public void test() {
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();
		
		String prefix = "files/"; 
		
		List<String> pathes = new ArrayList<>();
		pathes.add(prefix + "file1.txt");
		pathes.add(prefix + "file2.txt");
		pathes.add(prefix + "dir/file3.txt");
		
		List<String> sourcePathes = new ArrayList<>();
		
		for (String path : pathes) {
			
			String sourcePath = storageTestUtils.createSourceFile(path);
			sourcePathes.add(sourcePath); 
			
		}
		
		String sourcePath = testUtils.getSourcePath();
		String storagePath = testUtils.getStoragePath();
		
		PosixDAL posixDAL = new PosixDAL(); 
		
		
		try {
			List<String> sourceFiles = 	posixDAL.getFiles(sourcePath);
			TestUtils.printList("Source Files: ", sourceFiles);
			
			List<String> uploadedFiles = posixDAL.upload(sourcePath, storagePath);
			TestUtils.printList("Uploaded Files: ", uploadedFiles);
		
			List<String> deletedFiles = posixDAL.delete(sourcePath);
			TestUtils.printList("Deleted Files: ", deletedFiles);

			List<String> downloadedFiles = posixDAL.download(storagePath, sourcePath);
			TestUtils.printList("Downloaded Files: ", downloadedFiles);
	
		} catch (IOException e) {			
			e.printStackTrace();
		}
		
	}

}
