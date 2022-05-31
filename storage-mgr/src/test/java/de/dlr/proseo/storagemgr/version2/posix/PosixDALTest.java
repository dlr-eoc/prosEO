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
import de.dlr.proseo.storagemgr.UniqueStorageTestPaths;
import de.dlr.proseo.storagemgr.version2.PathConverter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class PosixDALTest {

	@Rule
	public TestName testName = new TestName();
	
	@Autowired
	private StorageTestUtils storageTestUtils;

	@Test
	public void test() {

		TestUtils.printMethodName(this, testName);
		UniqueStorageTestPaths uniquePaths = new UniqueStorageTestPaths(this, testName); 

		// create unique source pathes
		List<String> pathes = new ArrayList<>();
		pathes.add(new PathConverter(uniquePaths.getUniqueTestFolder(), "file1.txt").getPath());
		pathes.add(new PathConverter(uniquePaths.getUniqueTestFolder(), "file2.txt").getPath());
		pathes.add(new PathConverter(uniquePaths.getUniqueTestFolder(), "dir/file3.txt").getPath());

		// create source files 
		List<String> sourcePathes = new ArrayList<>();
		for (String path : pathes) {

			String sourcePath = storageTestUtils.createSourceFile(path);
			sourcePathes.add(sourcePath);
		}

		String sourcePath = uniquePaths.getUniqueSourcePath();
		String storagePath = uniquePaths.getUniqueStoragePath();
		PosixDAL posixDAL = new PosixDAL();

		try {
			// print source files
			List<String> sourceFiles = posixDAL.getFiles(sourcePath);
			TestUtils.printList("Source Files: ", sourceFiles);
			assertTrue("Expected: 3, " + " Exists: " + sourceFiles.size(), sourceFiles.size() == 3);

			// upload files to storage
			List<String> uploadedFiles = posixDAL.upload(sourcePath, storagePath);
			TestUtils.printList("Uploaded Files: ", uploadedFiles);
			assertTrue("Expected: 3, " + " Exists: " + uploadedFiles.size(), uploadedFiles.size() == 3);

			// delete source files
			List<String> deletedFiles = posixDAL.delete(sourcePath);
			TestUtils.printList("Deleted Files: ", deletedFiles);
			assertTrue("Expected: 3, " + " Exists: " + deletedFiles.size(), deletedFiles.size() == 3);

			// download files from storage
			List<String> downloadedFiles = posixDAL.download(storagePath, sourcePath);
			TestUtils.printList("Downloaded Files: ", downloadedFiles);
			assertTrue("Expected: 3, " + " Exists: " + downloadedFiles.size(), downloadedFiles.size() == 3);

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		uniquePaths.deleteUniqueTestDirectories();
	}
}
